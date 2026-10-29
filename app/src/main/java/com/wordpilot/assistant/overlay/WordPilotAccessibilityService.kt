package com.wordpilot.assistant.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.SentenceSpan
import com.wordpilot.assistant.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordPilotAccessibilityService : AccessibilityService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    companion object {
        private const val TAG = "WordPilotA11y"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val debouncer = Debouncer(serviceScope, 300L)
    private val safeReplacementHelper = SafeReplacementHelper()

    private lateinit var floatingController: FloatingAssistantController
    private var lastAnalyzedText: String = ""

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: WordPilotAccessibilityService started")
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        floatingController = FloatingAssistantController(
            context = this,
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected: Configuring AccessibilityServiceInfo")
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
            AccessibilityEvent.TYPE_VIEW_FOCUSED or
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 50
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleTextEvent(event)
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // If window changed completely to non-text window, check if input still focused
                val activeNode = findActiveInputNode(event)
                if (activeNode == null) {
                    floatingController.dismiss()
                }
            }
        }
    }

    private fun handleTextEvent(event: AccessibilityEvent) {
        val node = findActiveInputNode(event) ?: return

        // Skip password or non-editable nodes
        if (node.isPassword) {
            Log.d(TAG, "Skipping password field")
            floatingController.dismiss()
            return
        }

        val text = node.text?.toString() ?: ""
        if (text.isBlank()) {
            floatingController.dismiss()
            return
        }

        if (text != lastAnalyzedText) {
            Log.d(TAG, "Text changed in ${event.packageName}: \"$text\"")
            debouncer.debounce {
                analyzeText(text, node)
            }
        }
    }

    private fun findActiveInputNode(event: AccessibilityEvent): AccessibilityNodeInfo? {
        // Priority 1: Direct event source if editable
        val source = event.source
        if (source != null && isEditableNode(source)) {
            return source
        }

        // Priority 2: Input focused node in active window
        val root = rootInActiveWindow
        if (root != null) {
            val inputFocused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (inputFocused != null && isEditableNode(inputFocused)) {
                return inputFocused
            }

            // Priority 3: Search recursively for focused editable node
            val focusedChild = findFocusedEditable(root)
            if (focusedChild != null) {
                return focusedChild
            }
        }

        return source?.takeIf { isEditableNode(it) }
    }

    private fun isEditableNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return false
        return node.isEditable ||
            node.className?.contains("EditText", ignoreCase = true) == true ||
            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && isEditableNode(node)) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFocusedEditable(child)
            if (found != null) return found
        }
        return null
    }

    private suspend fun analyzeText(text: String, targetNode: AccessibilityNodeInfo) = withContext(Dispatchers.Default) {
        val app = WordPilotApp.instance
        lastAnalyzedText = text

        try {
            val sentence = app.sentenceAnalyzer.findCurrentSentence(text, text.length)
                ?: SentenceSpan(0, text.length, text)

            val corrections = app.correctionCoordinator.checkSentence(
                text,
                sentence.start,
                sentence.end
            )

            val validCorrections = corrections.filter { correction ->
                val isIgnored = app.dictionaryRepository.isRuleIgnored(correction.ruleId, correction.originalText)
                val inDict = if (correction.category == CorrectionCategory.SPELLING) {
                    app.dictionaryRepository.wordExists(correction.originalText.lowercase())
                } else false
                !isIgnored && !inDict
            }

            val topCorrection = validCorrections.firstOrNull()

            withContext(Dispatchers.Main) {
                if (topCorrection != null) {
                    Log.d(TAG, "Showing correction: ${topCorrection.originalText} -> ${topCorrection.suggestedReplacement}")
                    floatingController.showCorrection(
                        correction = topCorrection,
                        onAccept = { correction ->
                            applyCorrectionToNode(targetNode, text, correction)
                        },
                        onAddToDictionary = { correction ->
                            serviceScope.launch {
                                app.dictionaryRepository.addWord(correction.originalText.lowercase())
                                app.spellEngine.addWord(correction.originalText.lowercase())
                            }
                        },
                        onIgnoreRule = { correction ->
                            serviceScope.launch {
                                app.dictionaryRepository.ignoreRule(correction.ruleId, correction.originalText)
                            }
                        }
                    )
                } else {
                    floatingController.dismiss()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing text", e)
            withContext(Dispatchers.Main) {
                floatingController.dismiss()
            }
        }
    }

    private fun applyCorrectionToNode(
        node: AccessibilityNodeInfo,
        originalFullText: String,
        correction: Correction
    ) {
        try {
            val currentText = node.text?.toString() ?: originalFullText
            val updatedText = safeReplacementHelper.applyCorrection(currentText, correction) ?: return

            Log.d(TAG, "Applying correction: \"$currentText\" -> \"$updatedText\"")
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "ACTION_SET_TEXT result: $success")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying correction", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt: service interrupted")
        floatingController.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: service destroyed")
        floatingController.destroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
    }
}
