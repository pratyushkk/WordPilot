package com.wordpilot.assistant.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WordPilotAccessibilityService : AccessibilityService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    companion object {
        private const val TAG = "WordPilotA11y"
        var instance: WordPilotAccessibilityService? = null
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val debouncer = Debouncer(serviceScope, 250L)
    private val safeReplacementHelper = SafeReplacementHelper()

    private lateinit var floatingController: FloatingAssistantController
    private var lastAnalyzedText: String = ""
    private var hideJob: Job? = null
    private var lastOriginalTextBeforeUndo: String? = null
    private var lastActiveSentenceSpan: SentenceSpan? = null
    private var isAutoPauseBankingEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "onCreate: WordPilotAccessibilityService started")
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        serviceScope.launch {
            try {
                WordPilotApp.instance.userPreferences.autoPauseBankingEnabled.collect {
                    isAutoPauseBankingEnabled = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting autoPauseBankingEnabled", e)
            }
        }

        floatingController = FloatingAssistantController(
            context = this,
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "onServiceConnected: Configuring AccessibilityServiceInfo")
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
            AccessibilityEvent.TYPE_VIEW_FOCUSED or
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
            AccessibilityEvent.TYPE_WINDOWS_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 50
        serviceInfo = info
    }

    private fun isExcludedPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.lowercase()

        // Allow WordPilot itself and System UI / IME
        if (lower.startsWith("com.wordpilot.") || lower == "android" || lower.startsWith("com.android.systemui")) {
            return false
        }

        // 1. Known Banking, Payment & UPI Apps
        if (lower.startsWith("com.google.android.apps.nbu.paisa.user") || // GPay
            lower.startsWith("com.phonepe.") || // PhonePe
            lower.startsWith("net.one97.paytm") || // Paytm
            lower.startsWith("in.org.npci.upiapp") || // BHIM
            lower.startsWith("com.dreamplug.androidapp") || // CRED
            lower.startsWith("com.sbi.") || // SBI / YONO
            lower.startsWith("com.snapwork.sbi") ||
            lower.startsWith("com.hdfcbank.") || // HDFC
            lower.startsWith("com.hdfc.payzapp") ||
            lower.startsWith("com.csam.icici.bank.imobile") || // ICICI
            lower.startsWith("com.axis.mobile") || // Axis
            lower.startsWith("com.kotak.") || // Kotak
            lower.startsWith("com.bankofbaroda.") || // BoB
            lower.startsWith("com.pnb.pnbone") || // PNB
            lower.startsWith("com.canarabank.") || // Canara
            lower.startsWith("com.unionbank.") || // Union Bank
            lower.startsWith("com.idfcfirstbank.") || // IDFC First
            lower.startsWith("com.indusind.") || // IndusInd
            lower.startsWith("com.rblbank.") || // RBL
            lower.startsWith("com.federalbank.") || // Federal
            lower.startsWith("com.yesbank") || // Yes Bank
            lower.startsWith("com.freecharge.") || // Freecharge
            lower.startsWith("com.mobikwik_new") || // Mobikwik
            lower.startsWith("com.fi.money") || // Fi Money
            lower.startsWith("money.jupiter") || // Jupiter
            lower.startsWith("com.zerodha.kite3") || // Zerodha
            lower.startsWith("com.nextbillion.groww") || // Groww
            lower.startsWith("com.upstox.pro") || // Upstox
            lower.startsWith("com.angelbroking.") || // Angel One
            lower.startsWith("com.chase.sig.android") || // Chase
            lower.startsWith("com.infonow.bofa") || // Bank of America
            lower.startsWith("com.wf.wellsfargomobile") || // Wells Fargo
            lower.startsWith("com.citi.citimobile") || // Citi
            lower.startsWith("com.barclays.") || // Barclays
            lower.startsWith("uk.co.hsbc.") || // HSBC
            lower.startsWith("com.paypal.") || // PayPal
            lower.startsWith("com.revolut.") || // Revolut
            lower.startsWith("com.squareup.cash") // Cash App
        ) {
            return true
        }

        // 2. Password Managers, 2FA Authenticators & Wallets
        if (lower.startsWith("com.google.android.apps.authenticator2") ||
            lower.startsWith("com.azure.authenticator") ||
            lower.startsWith("com.x8bit.bitwarden") ||
            lower.startsWith("com.onepassword.") ||
            lower.startsWith("com.lastpass.") ||
            lower.startsWith("com.dashlane") ||
            lower.startsWith("com.authy.authy")
        ) {
            return true
        }

        // 3. Keyword heuristics for unlisted banking and payment apps
        if (lower.contains("banking") ||
            lower.contains(".bank") ||
            lower.contains("yono") ||
            lower.contains("paytm") ||
            lower.contains("phonepe") ||
            lower.contains("authenticator") ||
            lower.contains("password")
        ) {
            return true
        }

        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString()
        if (isExcludedPackage(packageName)) {
            hideJob?.cancel()
            floatingController.hideAssistant()
            floatingController.clearCorrection()

            if (isAutoPauseBankingEnabled) {
                Toast.makeText(this, "WordPilot paused for banking app. Tap Quick Tile to re-enable.", Toast.LENGTH_LONG).show()
                disableSelf()
            }
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleTextInputEvent(event)
            }
        }
    }

    private fun handleTextInputEvent(event: AccessibilityEvent) {
        // STRICT: Bubble must ONLY appear when keyboard is actually open
        if (!isKeyboardVisible()) {
            floatingController.hideAssistant()
            return
        }

        val node = findActiveInputNode(event)

        if (node != null && !node.isPassword) {
            // Cancel any pending dismissal
            hideJob?.cancel()

            // Keep assistant visible on screen while keyboard is open
            floatingController.showAssistant()

            val text = extractNodeText(node)
            if (text.isBlank()) {
                floatingController.clearCorrection()
                return
            }

            val app = WordPilotApp.instance
            val sentence = app.sentenceAnalyzer.findCurrentSentence(text, text.length)
                ?: SentenceSpan(0, text.length, text)
            lastActiveSentenceSpan = sentence

            floatingController.updateTextContext(
                fullText = text,
                sentenceText = sentence.text,
                onApplySentence = { newSentence ->
                    applySentenceReplacementToNode(node, text, sentence, newSentence)
                },
                onUndo = {
                    undoLastReplacement(node)
                },
                onInsert = { textToInsert ->
                    insertTextToNode(node, text, textToInsert)
                }
            )

            if (text != lastAnalyzedText) {
                Log.d(TAG, "Text changed in ${event.packageName}: \"$text\"")
                debouncer.debounce {
                    analyzeText(text, node)
                }
            }
        } else {
            floatingController.hideAssistant()
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        if (isExcludedPackage(packageName)) {
            hideJob?.cancel()
            floatingController.hideAssistant()
            floatingController.clearCorrection()

            if (isAutoPauseBankingEnabled) {
                Toast.makeText(this, "WordPilot paused for banking app. Tap Quick Tile to re-enable.", Toast.LENGTH_LONG).show()
                disableSelf()
            }
            return
        }

        if (!isKeyboardVisible()) {
            // Keyboard is not visible: hide immediately with brief debounce
            hideJob?.cancel()
            hideJob = serviceScope.launch {
                delay(120L)
                if (!isKeyboardVisible()) {
                    floatingController.hideAssistant()
                }
            }
        } else {
            val node = findActiveInputNodeAcrossAllWindows()
            if (node != null && !node.isPassword) {
                hideJob?.cancel()
                floatingController.showAssistant()
            } else {
                floatingController.hideAssistant()
            }
        }
    }

    private fun isKeyboardVisible(): Boolean {
        if (com.wordpilot.assistant.utils.KeyboardVisibilityTracker.isWordPilotImeVisible) {
            return true
        }
        return try {
            val winList = windows
            if (winList.isNullOrEmpty()) return false
            winList.any { win ->
                if (win.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    val bounds = android.graphics.Rect()
                    win.getBoundsInScreen(bounds)
                    bounds.height() > 150 && bounds.width() > 200
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun findActiveInputNode(event: AccessibilityEvent): AccessibilityNodeInfo? {
        // Priority 1: Direct event source if editable
        val source = event.source
        if (source != null && isEditableNode(source)) {
            return source
        }

        // Priority 2: Check active window / interactive windows
        return findActiveInputNodeAcrossAllWindows()
    }

    private fun findActiveInputNodeAcrossAllWindows(): AccessibilityNodeInfo? {
        // 1. Search rootInActiveWindow
        try {
            rootInActiveWindow?.let { root ->
                if (isExcludedPackage(root.packageName?.toString())) return null
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { if (isEditableNode(it)) return it }
                findFocusedEditable(root)?.let { return it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking rootInActiveWindow", e)
        }

        // 2. Search all interactive non-IME windows
        try {
            for (window in windows) {
                if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue
                val root = window.root ?: continue
                if (isExcludedPackage(root.packageName?.toString())) continue
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { if (isEditableNode(it)) return it }
                findFocusedEditable(root)?.let { return it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching windows", e)
        }

        return null
    }

    private fun isEditableNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return false
        return node.isEditable ||
            node.className?.contains("EditText", ignoreCase = true) == true ||
            node.className?.contains("AutoCompleteTextView", ignoreCase = true) == true ||
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

    private fun extractNodeText(node: AccessibilityNodeInfo): String {
        node.text?.let { if (it.isNotEmpty()) return it.toString() }
        if (!node.contentDescription.isNullOrEmpty()) {
            return node.contentDescription.toString()
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val childText = child.text?.toString()
            if (!childText.isNullOrEmpty()) {
                return childText
            }
        }
        return ""
    }

    private suspend fun analyzeText(text: String, targetNode: AccessibilityNodeInfo) = withContext(Dispatchers.Default) {
        val app = WordPilotApp.instance
        lastAnalyzedText = text

        try {
            withContext(Dispatchers.Main) {
                floatingController.setChecking(true)
            }

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
                floatingController.setChecking(false)
                if (!isKeyboardVisible()) {
                    floatingController.hideAssistant()
                    return@withContext
                }
                if (topCorrection != null) {
                    Log.d(TAG, "Showing correction: ${topCorrection.originalText} -> ${topCorrection.suggestedReplacement} (total: ${validCorrections.size})")
                    floatingController.showCorrection(
                        correction = topCorrection,
                        totalErrorCount = validCorrections.size,
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
                    floatingController.clearCorrection()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing text", e)
            withContext(Dispatchers.Main) {
                floatingController.setChecking(false)
                floatingController.clearCorrection()
            }
        }
    }

    private fun applyCorrectionToNode(
        targetNode: AccessibilityNodeInfo,
        originalFullText: String,
        correction: Correction
    ) {
        try {
            // Find the most fresh node instance across windows
            val node = findActiveInputNodeAcrossAllWindows() ?: targetNode
            val currentText = extractNodeText(node).ifBlank { originalFullText }
            val updatedText = safeReplacementHelper.applyCorrection(currentText, correction) ?: return

            Log.d(TAG, "Applying correction: \"$currentText\" -> \"$updatedText\"")
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "ACTION_SET_TEXT result: $success")

            if (success) {
                // Keep cursor at the end of the replaced word
                val replacementLength = correction.suggestedReplacement?.length ?: 0
                val newCursor = (correction.startOffset + replacementLength).coerceIn(0, updatedText.length)
                val cursorArgs = Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursor)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursor)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
                lastAnalyzedText = updatedText
            }

            // Reset card back to "✓ Clear" monitoring state
            floatingController.clearCorrection()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying correction", e)
        }
    }

    private fun applySentenceReplacementToNode(
        targetNode: AccessibilityNodeInfo,
        originalFullText: String,
        sentence: SentenceSpan,
        newSentence: String
    ) {
        try {
            val node = findActiveInputNodeAcrossAllWindows() ?: targetNode
            val currentText = extractNodeText(node).ifBlank { originalFullText }
            lastOriginalTextBeforeUndo = currentText

            val updatedText = if (sentence.start <= currentText.length && sentence.end <= currentText.length) {
                currentText.substring(0, sentence.start) + newSentence + currentText.substring(sentence.end)
            } else {
                newSentence
            }

            Log.d(TAG, "Applying sentence replacement: \"$currentText\" -> \"$updatedText\"")
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "ACTION_SET_TEXT result: $success")

            if (success) {
                val newCursor = (sentence.start + newSentence.length).coerceIn(0, updatedText.length)
                val cursorArgs = Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursor)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursor)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
                lastAnalyzedText = updatedText
            } else {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("WordPilot Text", newSentence))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying sentence replacement", e)
        }
    }

    private fun undoLastReplacement(targetNode: AccessibilityNodeInfo) {
        try {
            val prevText = lastOriginalTextBeforeUndo ?: return
            val node = findActiveInputNodeAcrossAllWindows() ?: targetNode
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, prevText)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            lastAnalyzedText = prevText
            lastOriginalTextBeforeUndo = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in undoLastReplacement", e)
        }
    }

    private fun insertTextToNode(
        targetNode: AccessibilityNodeInfo,
        currentFullText: String,
        textToInsert: String
    ) {
        try {
            val node = findActiveInputNodeAcrossAllWindows() ?: targetNode
            val currentText = extractNodeText(node).ifBlank { currentFullText }
            lastOriginalTextBeforeUndo = currentText
            val updatedText = if (currentText.isNotEmpty() && !currentText.endsWith(" ")) {
                "$currentText $textToInsert"
            } else {
                "$currentText$textToInsert"
            }
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updatedText)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            lastAnalyzedText = updatedText
        } catch (e: Exception) {
            Log.e(TAG, "Error in insertTextToNode", e)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt: service interrupted")
        floatingController.hideAssistant()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "onDestroy: service destroyed")
        floatingController.destroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
    }
}
