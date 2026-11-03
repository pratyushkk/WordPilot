package com.wordpilot.assistant.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.engine.providers.ProviderResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Diagnostic broadcast receiver to verify live multi-provider API execution on device.
 * Trigger via: adb shell am broadcast -a com.wordpilot.assistant.TEST_GRAMMAR_APIS -n com.wordpilot.assistant.debug/com.wordpilot.assistant.receiver.GrammarApiDeviceTestReceiver
 */
class GrammarApiDeviceTestReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GrammarApiTestReceiver"
        const val ACTION_TEST_GRAMMAR_APIS = "com.wordpilot.assistant.TEST_GRAMMAR_APIS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TEST_GRAMMAR_APIS) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = WordPilotApp.instance
                val lt = app.languageToolProvider
                val reportBuilder = StringBuilder()

                reportBuilder.append("=== LANGUAGETOOL ON-DEVICE VERIFICATION REPORT ===\n")
                reportBuilder.append("Timestamp: ${java.time.Instant.now()}\n")
                reportBuilder.append("Configured: ${lt.isConfigured()}\n")
                reportBuilder.append("Enabled: ${lt.isEnabled()}\n\n")

                val testSentences = listOf(
                    "he ate a apple",
                    "she go to school",
                    "Their going to there house",
                    "i have went to the store yesterday",
                    "We was happy with the results"
                )

                for ((idx, sentence) in testSentences.withIndex()) {
                    val start = System.currentTimeMillis()
                    val result = lt.check(sentence)
                    val latency = System.currentTimeMillis() - start

                    reportBuilder.append("[$idx/5] INPUT: \"$sentence\"\n")
                    when (result) {
                        is ProviderResult.Success -> {
                            reportBuilder.append("   - Status: HTTP 200 OK (${latency}ms)\n")
                            reportBuilder.append("   - Corrections Found: ${result.corrections.size}\n")
                            for (c in result.corrections) {
                                reportBuilder.append("     * [${c.category}] '${c.originalText}' => '${c.suggestedReplacement}' [${c.startOffset}..${c.endOffset}] (${c.explanation})\n")
                            }
                        }
                        is ProviderResult.Failure -> {
                            reportBuilder.append("   - FAILED (${latency}ms): ${result::class.simpleName} - ${result.message}\n")
                        }
                    }
                    reportBuilder.append("\n")
                }

                val fullReport = reportBuilder.toString()
                Log.i(TAG, fullReport)

                val reportFile = File(context.filesDir, "languagetool_test_results.txt")
                reportFile.writeText(fullReport)
                Log.i(TAG, "Report written to ${reportFile.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Error in test receiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
