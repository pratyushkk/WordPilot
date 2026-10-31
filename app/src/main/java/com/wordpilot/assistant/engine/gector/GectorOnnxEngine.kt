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

class GectorOnnxEngine(
    private val modelFile: File? = null,
    private val vocabFile: File? = null,
    private var tagVocab: List<String> = emptyList()
) {
    companion object {
        private const val TAG = "GectorOnnxEngine"
        const val MODEL_FILENAME = "gector.onnx"
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokenizer: GectorTokenizer = GectorTokenizer()
    private val tagDecoder = GectorTagDecoder()

    @Volatile
    private var isInitialized = false

    fun isAvailable(): Boolean {
        return isInitialized && ortSession != null && modelFile != null && modelFile.exists()
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (modelFile == null || !modelFile.exists() || modelFile.length() < 1024 * 1024) {
            isInitialized = false
            return@withContext false
        }

        try {
            // Load label vocabulary if available
            val labelFile = vocabFile ?: (modelFile.parentFile?.let { File(it, "labels.json") })
            if (labelFile != null && labelFile.exists()) {
                try {
                    val jsonStr = labelFile.readText()
                    val json = org.json.JSONObject(jsonStr)
                    if (json.has("labels")) {
                        val arr = json.getJSONArray("labels")
                        val list = ArrayList<String>(arr.length())
                        for (i in 0 until arr.length()) {
                            list.add(arr.getString(i))
                        }
                        tagVocab = list
                        Log.d(TAG, "Loaded ${tagVocab.size} GECToR tag labels from ${labelFile.name}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse labels.json", e)
                }
            }

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

    suspend fun check(text: String, maxIterations: Int = 2): List<Correction> = withContext(Dispatchers.Default) {
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
                val outputTensor = results[0].value as? Array<Array<FloatArray>>
                    ?: return@withContext emptyList()

                val predictedTags = mutableListOf<String>()
                // Extract argmax tag for each token excluding [CLS] and [SEP]
                val batchOutput = outputTensor[0]
                for (t in 1 until batchOutput.size - 1) {
                    val logits = batchOutput[t]
                    val maxIdx = logits.indices.maxByOrNull { logits[it] } ?: 0
                    val tagName = tagVocab.getOrElse(maxIdx) { GectorTagDecoder.TAG_KEEP }
                    predictedTags.add(tagName)
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
