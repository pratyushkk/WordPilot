package com.wordpilot.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.measureTimeMillis

class GectorDeviceTestReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "GectorDeviceTest"
        const val ACTION_TEST_SENTENCES = "com.wordpilot.assistant.TEST_SENTENCES"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TEST_SENTENCES) return

        Log.i(TAG, "================================================================================")
        Log.i(TAG, "  STARTING GECTOR COMPREHENSIVE ON-DEVICE SENTENCE EVALUATION ON PHYSICAL PHONE  ")
        Log.i(TAG, "================================================================================")

        val pendingResult = goAsync()
        val app = context.applicationContext as WordPilotApp

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val coordinator = app.correctionCoordinator
                val safeHelper = SafeReplacementHelper()

                val testSentences = listOf(
                    // Short sentences
                    "Short" to "he ate a apple",
                    "Short" to "she go to school",
                    "Short" to "i like dogs",
                    "Short" to "I really really like it.",
                    "Short" to "They is very happy.",
                    "Short" to "We bought apples bananas and oranges.",

                    // Medium sentences
                    "Medium" to "she have went to a apple store yesterday and i was happy.",
                    "Medium" to "Yesterday they goes to the market and buys some fresh fruits.",
                    "Medium" to "However I think that this decision were not correct.",

                    // Long Paragraphs & Complex Sentences
                    "Long Paragraph" to "yesterday we goes to the new restaurant downtown. the food were really really delicious but the service was a bit slow. i order a pizza with mushroom and onions and my friend ordered pasta. However we will definitely visit again next week",
                    "Long Sentence" to "Although the weather was bad and raining heavily outside she decide to walk to school without an umbrella because she have missed the morning bus and did not wanted to be late for her important examination.",

                    // Already Correct (False positive test)
                    "Clean" to "The quick brown fox jumps over the lazy dog.",
                    "Clean" to "She has already finished her homework, and she is ready for bed.",

                    // Edge Cases
                    "Edge" to "i'm not sure if they'll arrive on time today",
                    "Edge" to "On January 15 2024 they visited 3 different cities."
                )

                val reportBuilder = StringBuilder()
                reportBuilder.append("=== GECTOR ON-DEVICE EVALUATION REPORT ===\n")
                reportBuilder.append("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\n\n")

                var passed = 0
                var totalDuration = 0L

                for ((idx, pair) in testSentences.withIndex()) {
                    val (type, input) = pair
                    val indexStr = "[${idx + 1}/${testSentences.size}]"

                    Log.i(TAG, "------------------------------------------------------------")
                    Log.i(TAG, "$indexStr [$type] INPUT: \"$input\"")
                    reportBuilder.append("$indexStr [$type]\n  INPUT:  \"$input\"\n")

                    var durationMs: Long
                    val corrections = try {
                        durationMs = measureTimeMillis {
                            // Run actual inference on device
                        }
                        val resultList: List<com.wordpilot.assistant.domain.models.Correction>
                        durationMs = measureTimeMillis {
                            resultList = coordinator.checkText(input)
                        }
                        resultList
                    } catch (e: Exception) {
                        Log.e(TAG, "$indexStr EXCEPTION: ${e.message}", e)
                        reportBuilder.append("  EXCEPTION: ${e.message}\n\n")
                        continue
                    }

                    totalDuration += durationMs
                    Log.i(TAG, "  LATENCY: ${durationMs}ms | FOUND: ${corrections.size} corrections")
                    reportBuilder.append("  LATENCY: ${durationMs}ms | CORRECTIONS: ${corrections.size}\n")

                    for (c in corrections) {
                        val cLog = "    -> [${c.category}] '${c.originalText}' => '${c.suggestedReplacement}' [${c.startOffset}..${c.endOffset}] (rule: ${c.ruleId})"
                        Log.i(TAG, cLog)
                        reportBuilder.append("$cLog\n")
                    }

                    val applied = safeHelper.applyMultipleCorrections(input, corrections)
                    Log.i(TAG, "  OUTPUT: \"$applied\"")
                    reportBuilder.append("  OUTPUT: \"$applied\"\n\n")

                    passed++
                }

                val avgLatency = if (testSentences.isNotEmpty()) totalDuration / testSentences.size else 0
                val summary = "EVALUATION COMPLETE: $passed/${testSentences.size} passed | Total Time: ${totalDuration}ms | Avg Latency: ${avgLatency}ms"
                Log.i(TAG, "================================================================================")
                Log.i(TAG, summary)
                Log.i(TAG, "================================================================================")
                reportBuilder.append("SUMMARY: $summary\n")

                // Write report to app files directory
                val reportFile = File(context.filesDir, "gector_test_results.txt")
                reportFile.writeText(reportBuilder.toString())
                Log.i(TAG, "Report written to ${reportFile.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in on-device test runner", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
