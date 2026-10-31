package com.wordpilot.assistant.engine.gector

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import com.wordpilot.assistant.domain.models.Correction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer
import org.json.JSONObject

class GectorOnnxEngine(
    private val modelFile: File? = null,
    private val vocabFile: File? = null,
    private var tagVocab: List<String> = emptyList()
) {
    companion object {
        private const val TAG = "GectorOnnxEngine"
        // Standard GECToR error detection threshold (Omelianchuk et al., 2020)
        private const val DEFAULT_MIN_ERROR_PROB = 0.50f
        private const val DEFAULT_ADDITIONAL_CONFIDENCE = 0.20f
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false
    private var keepTagIndex: Int = 1

    private val conjugator = GectorVerbConjugator()
    private var tokenizer: GectorTokenizer = GectorTokenizer()
    private var tagDecoder = GectorTagDecoder(conjugator)

    init {
        if (tagVocab.isNotEmpty()) {
            keepTagIndex = tagVocab.indexOf(GectorTagDecoder.TAG_KEEP).takeIf { it >= 0 } ?: 1
        }
    }

    fun isAvailable(): Boolean = isInitialized && ortSession != null

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (modelFile == null || !modelFile.exists() || modelFile.length() < 1024 * 1024) {
            isInitialized = false
            return@withContext false
        }

        try {
            val modelDir = modelFile.parentFile

            // 1. Load label vocabulary
            val labelFile = vocabFile ?: (modelDir?.let { File(it, "labels.json") })
            if (labelFile != null && labelFile.exists()) {
                try {
                    val jsonStr = labelFile.readText()
                    val json = JSONObject(jsonStr)
                    if (json.has("labels")) {
                        val arr = json.getJSONArray("labels")
                        val list = ArrayList<String>(arr.length())
                        for (i in 0 until arr.length()) {
                            list.add(arr.getString(i))
                        }
                        tagVocab = list
                        keepTagIndex = tagVocab.indexOf(GectorTagDecoder.TAG_KEEP).takeIf { it >= 0 } ?: 1
                        Log.d(TAG, "Loaded ${tagVocab.size} GECToR tag labels (keepTagIndex=$keepTagIndex) from ${labelFile.name}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse labels.json", e)
                }
            }

            // 2. Load RoBERTa BPE tokenizer vocabulary
            val tokFile = modelDir?.let { File(it, "tokenizer.json") }
            if (tokFile != null && tokFile.exists()) {
                val tok = GectorTokenizer()
                if (tok.loadFromTokenizerJson(tokFile)) {
                    tokenizer = tok
                    Log.d(TAG, "RoBERTa tokenizer initialized successfully")
                }
            }

            // 3. Load verb conjugation vocabulary
            val verbFile = modelDir?.let { File(it, "verb-form-vocab.txt") }
            if (verbFile != null && verbFile.exists()) {
                if (conjugator.loadFromFile(verbFile)) {
                    tagDecoder = GectorTagDecoder(conjugator)
                    Log.d(TAG, "Verb conjugator initialized successfully")
                }
            }

            // 4. Initialize ONNX Runtime session
            val env = OrtEnvironment.getEnvironment()
            ortEnv = env
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            ortSession = env.createSession(modelFile.absolutePath, sessionOptions)
            isInitialized = true
            Log.d(TAG, "GECToR ONNX session initialized successfully from ${modelFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GECToR ONNX session", e)
            isInitialized = false
            false
        }
    }

    fun getTokenizer(): GectorTokenizer = tokenizer

    suspend fun check(
        text: String,
        maxIterations: Int = 2,
        minErrorProbability: Float = DEFAULT_MIN_ERROR_PROB,
        additionalConfidence: Float = DEFAULT_ADDITIONAL_CONFIDENCE
    ): List<Correction> = withContext(Dispatchers.Default) {
        if (!isAvailable() || text.isBlank()) {
            return@withContext emptyList()
        }

        try {
            val session = ortSession ?: return@withContext emptyList()
            val env = ortEnv ?: return@withContext emptyList()

            val tokenized = tokenizer.tokenize(text)
            if (tokenized.tokens.isEmpty()) return@withContext emptyList()

            val seqLen = tokenized.inputIds.size.toLong()
            val inputShape = longArrayOf(1, seqLen)

            val inputIdsBuffer = LongBuffer.wrap(tokenized.inputIds)
            val maskBuffer = LongBuffer.wrap(tokenized.attentionMask)

            val inputIdsTensor = OnnxTensor.createTensor(env, inputIdsBuffer, inputShape)
            val maskTensor = OnnxTensor.createTensor(env, maskBuffer, inputShape)

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to maskTensor
            )

            session.run(inputs).use { results ->
                val labelProbsVal = if (results.size() > 0) results["label_probs"].orElse(null)?.value else null
                val errorProbsVal = if (results.size() > 1) results["error_probs"].orElse(null)?.value else null

                @Suppress("UNCHECKED_CAST")
                val labelProbs = (labelProbsVal ?: results[0].value) as? Array<Array<FloatArray>> ?: return@withContext emptyList()
                @Suppress("UNCHECKED_CAST")
                val errorProbs = (errorProbsVal ?: if (results.size() > 1) results[1].value else null) as? Array<FloatArray>

                val predictedTags = mutableListOf<String>()
                val batchLabels = labelProbs[0]
                val batchErrors = errorProbs?.get(0)

                // Loop over tokens (excluding <s> at 0 and </s> at size-1)
                for (t in 1 until batchLabels.size - 1) {
                    val probs = batchLabels[t].clone()
                    val errProb = batchErrors?.getOrNull(t) ?: 0f

                    // Official GECToR inference tweak: add additional_confidence bias to $KEEP probability
                    if (keepTagIndex in probs.indices) {
                        probs[keepTagIndex] += additionalConfidence
                    }

                    val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: keepTagIndex
                    val predictedTag = if (maxIdx != keepTagIndex && errProb >= minErrorProbability) {
                        val chosen = tagVocab.getOrElse(maxIdx) { GectorTagDecoder.TAG_KEEP }
                        if (chosen != "<OOV>") chosen else GectorTagDecoder.TAG_KEEP
                    } else {
                        GectorTagDecoder.TAG_KEEP
                    }

                    predictedTags.add(predictedTag)
                }

                tagDecoder.decodeTags(tokenized.tokens, predictedTags, text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running GECToR ONNX inference", e)
            emptyList()
        }
    }

    fun close() {
        try {
            ortSession?.close()
            ortSession = null
            ortEnv?.close()
            ortEnv = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GECToR session", e)
        }
    }
}
