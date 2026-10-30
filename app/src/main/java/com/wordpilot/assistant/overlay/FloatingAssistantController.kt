package com.wordpilot.assistant.overlay

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.wordpilot.assistant.MainActivity
import com.wordpilot.assistant.WordPilotApp
import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.ui.floating.FloatingAssistantView
import com.wordpilot.assistant.ui.floating.FloatingUiState
import com.wordpilot.assistant.ui.navigation.Routes
import com.wordpilot.assistant.ui.settings.AppearanceSettings
import com.wordpilot.assistant.ui.theme.WordPilotTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingAssistantController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner
) {
    companion object {
        const val TAG = "FloatingController"
        const val PREFS_NAME = "wordpilot_overlay_prefs"
        const val KEY_X_PX = "overlay_x_px"
        const val KEY_Y_PX = "overlay_y_px"
        const val KEY_MINIMIZED = "overlay_minimized"
        const val KEY_CUSTOM_WIDTH_DP = "overlay_width_dp"
        const val KEY_CUSTOM_HEIGHT_DP = "overlay_height_dp"
        const val KEY_CUSTOM_TEXT_SIZE_SP = "overlay_text_size_sp"
        const val KEY_CUSTOM_CORNER_RADIUS_DP = "overlay_corner_radius_dp"
        const val KEY_CUSTOM_BUBBLE_SIZE_DP = "overlay_bubble_size_dp"

        const val DEFAULT_WIDTH_DP = 300f
        const val DEFAULT_HEIGHT_DP = 56f
        const val DEFAULT_TEXT_SIZE_SP = 16.5f
        const val DEFAULT_CORNER_RADIUS_DP = 34f
        const val DEFAULT_BUBBLE_SIZE_DP = 48f
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val safeReplacementHelper = SafeReplacementHelper()
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var isViewAttached = false
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    // Appearance settings loaded persistently
    private val _appearanceSettings = MutableStateFlow(AppearanceSettings.load(context))
    val appearanceSettings: StateFlow<AppearanceSettings> = _appearanceSettings.asStateFlow()

    // Floating UI state machine
    private val _uiState = MutableStateFlow(FloatingUiState.COLLAPSED)
    val uiState: StateFlow<FloatingUiState> = _uiState.asStateFlow()

    private val _isNearRightEdge = MutableStateFlow(false)
    val isNearRightEdge: StateFlow<Boolean> = _isNearRightEdge.asStateFlow()

    // Context & Text detection state
    private val _currentFullText = MutableStateFlow("")
    private val _currentSentence = MutableStateFlow("")
    private val _activeCorrection = MutableStateFlow<Correction?>(null)
    val activeCorrection: StateFlow<Correction?> = _activeCorrection.asStateFlow()

    private val _errorCount = MutableStateFlow(0)
    val errorCount: StateFlow<Int> = _errorCount.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    // Suggestion Preview State
    private val _previewOriginalText = MutableStateFlow("")
    private val _previewImprovedText = MutableStateFlow("")
    private val _isPreviewLoading = MutableStateFlow(false)
    private val _previewErrorMessage = MutableStateFlow<String?>(null)
    private val _canUndo = MutableStateFlow(false)
    private val _currentTone = MutableStateFlow("Default")

    // Callbacks provided by WordPilotAccessibilityService
    private var onApplySentenceCallback: ((String) -> Unit)? = null
    private var onInsertCallback: ((String) -> Unit)? = null
    private var onUndoCallback: (() -> Unit)? = null
    private var onAcceptSingleCorrectionCallback: ((Correction) -> Unit)? = null

    private var collapseResizeJob: Job? = null
    private var bubbleRestingX: Int = 0
    private var bubbleRestingY: Int = 0

    private val appearancePrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        _appearanceSettings.value = AppearanceSettings.load(context)
        updateWindowLayoutForState(_uiState.value)
    }

    init {
        context.getSharedPreferences(AppearanceSettings.PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(appearancePrefsListener)
    }

    fun setUiState(targetState: FloatingUiState) {
        collapseResizeJob?.cancel()
        if (targetState == FloatingUiState.COLLAPSED) {
            _uiState.value = FloatingUiState.COLLAPSED
            collapseResizeJob = controllerScope.launch {
                delay(260L)
                updateWindowLayoutForState(FloatingUiState.COLLAPSED)
            }
        } else {
            updateWindowLayoutForState(targetState)
            _uiState.value = targetState
        }
    }

    private fun updateWindowLayoutForState(state: FloatingUiState) {
        val view = overlayView ?: return
        val lp = windowLayoutParams ?: return
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val screenWidthPx = displayMetrics.widthPixels
        val screenHeightPx = displayMetrics.heightPixels

        val s = appearanceSettings.value
        val bubbleSizePx = (s.bubbleSizeDp * density).toInt()
        val paddingPx = (14 * density).toInt()

        val collapsedWidthPx = bubbleSizePx + paddingPx * 2
        val collapsedHeightPx = bubbleSizePx + paddingPx * 2

        when (state) {
            FloatingUiState.COLLAPSED -> {
                lp.width = collapsedWidthPx
                lp.height = collapsedHeightPx
                lp.x = bubbleRestingX
                lp.y = bubbleRestingY
            }
            FloatingUiState.EXPANDED -> {
                val maxAllowedWidthDp = (screenWidthPx / density) - 24f
                val expandedWidthDp = s.toolbarMaxWidthDp.coerceAtMost(maxAllowedWidthDp)
                val expandedWidthPx = ((expandedWidthDp + 16f) * density).toInt()
                val expandedHeightDp = maxOf(s.toolbarHeightDp, s.bubbleSizeDp) + 16f
                val expandedHeightPx = (expandedHeightDp * density).toInt()

                lp.width = expandedWidthPx
                lp.height = expandedHeightPx

                if (_isNearRightEdge.value) {
                    val bubbleRightEdgePx = bubbleRestingX + collapsedWidthPx
                    lp.x = (bubbleRightEdgePx - expandedWidthPx).coerceAtLeast((8 * density).toInt())
                } else {
                    lp.x = bubbleRestingX.coerceAtMost(screenWidthPx - expandedWidthPx - (8 * density).toInt())
                }
                lp.y = bubbleRestingY
            }
            FloatingUiState.MORE_OPTIONS -> {
                val maxAllowedWidthDp = (screenWidthPx / density) - 24f
                val panelWidthDp = 340f.coerceAtMost(maxAllowedWidthDp)
                val panelWidthPx = (panelWidthDp * density).toInt()
                val panelHeightDp = 360f
                val panelHeightPx = (panelHeightDp * density).toInt()

                lp.width = panelWidthPx
                lp.height = panelHeightPx

                if (_isNearRightEdge.value) {
                    val bubbleRightEdgePx = bubbleRestingX + collapsedWidthPx
                    lp.x = (bubbleRightEdgePx - panelWidthPx).coerceAtLeast((8 * density).toInt())
                } else {
                    lp.x = bubbleRestingX.coerceAtMost(screenWidthPx - panelWidthPx - (8 * density).toInt())
                }

                val maxAvailableY = screenHeightPx - panelHeightPx - (48 * density).toInt()
                lp.y = bubbleRestingY.coerceAtMost(maxAvailableY).coerceAtLeast((16 * density).toInt())
            }
            FloatingUiState.PREVIEW -> {
                val maxAllowedWidthDp = (screenWidthPx / density) - 24f
                val previewWidthDp = 340f.coerceAtMost(maxAllowedWidthDp)
                val previewWidthPx = (previewWidthDp * density).toInt()
                val previewHeightDp = 420f
                val previewHeightPx = (previewHeightDp * density).toInt()

                lp.width = previewWidthPx
                lp.height = previewHeightPx

                if (_isNearRightEdge.value) {
                    val bubbleRightEdgePx = bubbleRestingX + collapsedWidthPx
                    lp.x = (bubbleRightEdgePx - previewWidthPx).coerceAtLeast((8 * density).toInt())
                } else {
                    lp.x = bubbleRestingX.coerceAtMost(screenWidthPx - previewWidthPx - (8 * density).toInt())
                }

                val maxAvailableY = screenHeightPx - previewHeightPx - (48 * density).toInt()
                lp.y = bubbleRestingY.coerceAtMost(maxAvailableY).coerceAtLeast((16 * density).toInt())
            }
        }

        try {
            windowManager.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window layout for state $state", e)
        }
    }

    fun canDrawOverlays(): Boolean {
        if (context is AccessibilityService) return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun showAssistant() {
        if (!canDrawOverlays()) return
        if (!isViewAttached) {
            attachOverlayView()
        }
    }

    fun hideAssistant() {
        _activeCorrection.value = null
        _errorCount.value = 0
        _uiState.value = FloatingUiState.COLLAPSED
        detachOverlayView()
    }

    fun setChecking(checking: Boolean) {
        _isChecking.value = checking
    }

    fun updateTextContext(
        fullText: String,
        sentenceText: String,
        onApplySentence: (String) -> Unit,
        onUndo: (() -> Unit)? = null,
        onInsert: ((String) -> Unit)? = null
    ) {
        _currentFullText.value = fullText
        _currentSentence.value = sentenceText
        this.onApplySentenceCallback = onApplySentence
        this.onUndoCallback = onUndo
        this.onInsertCallback = onInsert
    }

    fun showCorrection(
        correction: Correction,
        totalErrorCount: Int = 1,
        onAccept: (Correction) -> Unit,
        onAddToDictionary: ((Correction) -> Unit)? = null,
        onIgnoreRule: ((Correction) -> Unit)? = null
    ) {
        if (!canDrawOverlays()) return

        _activeCorrection.value = correction
        _errorCount.value = totalErrorCount.coerceAtLeast(1)
        this.onAcceptSingleCorrectionCallback = onAccept

        if (!isViewAttached) {
            attachOverlayView()
        }
    }

    fun clearCorrection() {
        _activeCorrection.value = null
        _errorCount.value = 0
    }

    fun dismiss() {
        clearCorrection()
        setUiState(FloatingUiState.COLLAPSED)
    }

    // =========================================================================
    // ACTION DISPATCHERS
    // =========================================================================

    fun onActionSelected(actionId: String) {
        when (actionId) {
            "more" -> {
                setUiState(FloatingUiState.MORE_OPTIONS)
            }
            "back_to_expanded" -> {
                setUiState(FloatingUiState.EXPANDED)
            }
            "fix_grammar" -> {
                executeFixGrammar()
            }
            "rephrase" -> {
                executeRewrite(RewriteStyle.REWRITE, "Default")
            }
            "professional" -> {
                executeRewrite(RewriteStyle.MAKE_PROFESSIONAL, "Professional")
            }
            "shorten" -> {
                executeRewrite(RewriteStyle.MAKE_CONCISE, "Concise")
            }
            "expand" -> {
                executeRewrite(RewriteStyle.REWRITE, "Expanded", "Elaborate and expand the following text with richer details:")
            }
            "casual" -> {
                executeRewrite(RewriteStyle.MAKE_CASUAL, "Casual")
            }
            "formal" -> {
                executeRewrite(RewriteStyle.MAKE_PROFESSIONAL, "Formal")
            }
            "friendly" -> {
                executeRewrite(RewriteStyle.MAKE_POLITE, "Friendly")
            }
            "summarize" -> {
                executeRewrite(RewriteStyle.MAKE_CONCISE, "Summary")
            }
            "translate" -> {
                executeRewrite(RewriteStyle.CUSTOM, "Translate", "Translate the following text to natural English:")
            }
            "clarity" -> {
                executeRewrite(RewriteStyle.IMPROVE_CLARITY, "Clarity")
            }
            "punctuation" -> {
                executeFixGrammar()
            }
            else -> {
                executeRewrite(RewriteStyle.REWRITE, "Default")
            }
        }
    }

    private fun executeFixGrammar() {
        val targetText = getTargetText()
        if (targetText.isBlank()) {
            Toast.makeText(context, "No text detected to fix", Toast.LENGTH_SHORT).show()
            return
        }

        _previewOriginalText.value = targetText
        _previewImprovedText.value = ""
        _previewErrorMessage.value = null
        _isPreviewLoading.value = true
        setUiState(FloatingUiState.PREVIEW)

        controllerScope.launch {
            try {
                val app = WordPilotApp.instance
                val corrections = withContext(Dispatchers.Default) {
                    app.correctionCoordinator.checkSentence(targetText, 0, targetText.length)
                }

                var fixed = targetText
                if (corrections.isNotEmpty()) {
                    val replaced = safeReplacementHelper.applyMultipleCorrections(targetText, corrections)
                    if (replaced.isNotBlank()) {
                        fixed = replaced
                    }
                }

                // Sentence mechanics cleanup
                fixed = cleanSentencePunctuation(fixed)

                _previewImprovedText.value = fixed
                _isPreviewLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error in executeFixGrammar", e)
                _previewErrorMessage.value = "Failed to fix grammar: ${e.localizedMessage}"
                _isPreviewLoading.value = false
            }
        }
    }

    private fun executeRewrite(style: RewriteStyle, toneName: String, customPrompt: String? = null) {
        val targetText = getTargetText()
        if (targetText.isBlank()) {
            Toast.makeText(context, "No text detected to rephrase", Toast.LENGTH_SHORT).show()
            return
        }

        _previewOriginalText.value = targetText
        _previewImprovedText.value = ""
        _previewErrorMessage.value = null
        _isPreviewLoading.value = true
        _currentTone.value = toneName
        setUiState(FloatingUiState.PREVIEW)

        controllerScope.launch {
            try {
                val app = WordPilotApp.instance
                val result = withContext(Dispatchers.Default) {
                    app.rewriteCoordinator.rewrite(
                        text = targetText,
                        style = style,
                        customInstruction = customPrompt
                    )
                }

                if (result.isSuccess && result.rewrittenText.isNotBlank()) {
                    _previewImprovedText.value = result.rewrittenText
                    _previewErrorMessage.value = null
                } else {
                    _previewImprovedText.value = result.rewrittenText.ifBlank { targetText }
                    _previewErrorMessage.value = result.errorMessage
                }
                _isPreviewLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error rewriting text", e)
                _previewErrorMessage.value = "Rewriting failed: ${e.localizedMessage}"
                _isPreviewLoading.value = false
            }
        }
    }

    fun onCustomPromptSubmit(prompt: String) {
        executeRewrite(RewriteStyle.CUSTOM, "Custom", prompt)
    }

    fun onToneSelected(tone: String) {
        when (tone.lowercase()) {
            "professional", "formal" -> executeRewrite(RewriteStyle.MAKE_PROFESSIONAL, tone)
            "casual" -> executeRewrite(RewriteStyle.MAKE_CASUAL, tone)
            "friendly" -> executeRewrite(RewriteStyle.MAKE_POLITE, tone)
            else -> executeRewrite(RewriteStyle.REWRITE, "Default")
        }
    }

    fun applyPreviewReplacement() {
        val improved = _previewImprovedText.value
        if (improved.isBlank()) return

        onApplySentenceCallback?.invoke(improved)
        _canUndo.value = true
        setUiState(FloatingUiState.COLLAPSED)
        clearCorrection()
        Toast.makeText(context, "Text updated", Toast.LENGTH_SHORT).show()
    }

    fun insertPreviewText() {
        val textToInsert = _previewImprovedText.value
        if (textToInsert.isBlank()) return

        onInsertCallback?.invoke(textToInsert)
        setUiState(FloatingUiState.COLLAPSED)
        Toast.makeText(context, "Text inserted", Toast.LENGTH_SHORT).show()
    }

    fun copyPreviewText() {
        val textToCopy = _previewImprovedText.value.ifBlank { _previewOriginalText.value }
        if (textToCopy.isNotBlank()) {
            val clip = ClipData.newPlainText("WordPilot Text", textToCopy)
            clipboardManager.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun undoLastChange() {
        onUndoCallback?.invoke()
        _canUndo.value = false
        Toast.makeText(context, "Change undone", Toast.LENGTH_SHORT).show()
    }

    fun openAppearanceSettings() {
        setUiState(FloatingUiState.COLLAPSED)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", Routes.BUBBLE_CUSTOMIZATION)
        }
        context.startActivity(intent)
    }

    private fun getTargetText(): String {
        return _currentSentence.value.ifBlank { _currentFullText.value }
    }

    private fun cleanSentencePunctuation(text: String): String {
        var res = text.trim()
        if (res.isNotEmpty() && !res.endsWith('.') && !res.endsWith('!') && !res.endsWith('?')) {
            res += "."
        }
        if (res.isNotEmpty() && res[0].isLowerCase()) {
            res = res.replaceFirstChar { it.uppercase() }
        }
        return res
    }

    // =========================================================================
    // WINDOW & OVERLAY MANAGEMENT
    // =========================================================================

    fun onDrag(deltaX: Float, deltaY: Float) {
        val view = overlayView ?: return
        val lp = windowLayoutParams ?: return
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        val maxX = (screenWidth - lp.width).coerceAtLeast(0)
        val maxY = (screenHeight - lp.height).coerceAtLeast(0)

        val newX = (lp.x + deltaX.toInt()).coerceIn(0, maxX)
        val newY = (lp.y - deltaY.toInt()).coerceIn(0, maxY)

        lp.x = newX
        lp.y = newY
        bubbleRestingX = newX
        bubbleRestingY = newY
        _isNearRightEdge.value = newX > screenWidth / 2

        prefs.edit().putInt(KEY_X_PX, newX).putInt(KEY_Y_PX, newY).apply()

        try {
            windowManager.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating drag position", e)
        }
    }

    private fun attachOverlayView() {
        if (isViewAttached) return

        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val defaultMarginX = (16 * density).toInt()
        val defaultY = (340 * density).toInt()

        val savedX = prefs.getInt(KEY_X_PX, defaultMarginX)
        val savedY = prefs.getInt(KEY_Y_PX, defaultY)
        bubbleRestingX = savedX
        bubbleRestingY = savedY

        _isNearRightEdge.value = savedX > displayMetrics.widthPixels / 2

        val s = appearanceSettings.value
        val bubbleSizePx = (s.bubbleSizeDp * density).toInt()
        val paddingPx = (8 * density).toInt()
        val collapsedWidthPx = bubbleSizePx + paddingPx * 2
        val collapsedHeightPx = bubbleSizePx + paddingPx * 2

        val overlayType = if (context is AccessibilityService) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            collapsedWidthPx,
            collapsedHeightPx,
            overlayType,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
            x = savedX
            y = savedY
        }
        windowLayoutParams = params

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@FloatingAssistantController.lifecycleOwner)
            setViewTreeViewModelStoreOwner(this@FloatingAssistantController.viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(this@FloatingAssistantController.savedStateRegistryOwner)

            setContent {
                val settings by appearanceSettings.collectAsState()
                val currentUiState by uiState.collectAsState()
                val nearRight by isNearRightEdge.collectAsState()
                val errCount by errorCount.collectAsState()
                val checking by isChecking.collectAsState()
                val activeCorr by activeCorrection.collectAsState()

                val origText by _previewOriginalText.collectAsState()
                val imprText by _previewImprovedText.collectAsState()
                val prevLoading by _isPreviewLoading.collectAsState()
                val prevError by _previewErrorMessage.collectAsState()
                val undoAvailable by _canUndo.collectAsState()
                val tone by _currentTone.collectAsState()

                WordPilotTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = if (nearRight) Alignment.BottomEnd else Alignment.BottomStart
                    ) {
                        FloatingAssistantView(
                            settings = settings,
                            uiState = currentUiState,
                            isNearRightEdge = nearRight,
                            errorCount = errCount,
                            isChecking = checking,
                            activeCorrection = activeCorr,
                            previewOriginalText = origText,
                            previewImprovedText = imprText,
                            isPreviewLoading = prevLoading,
                            previewErrorMessage = prevError,
                            canUndo = undoAvailable,
                            currentTone = tone,
                            onBubbleClick = {
                                if (currentUiState == FloatingUiState.COLLAPSED) {
                                    setUiState(FloatingUiState.EXPANDED)
                                } else {
                                    setUiState(FloatingUiState.COLLAPSED)
                                }
                            },
                            onDrag = { dx, dy -> onDrag(dx, dy) },
                            onActionClick = { actionId -> onActionSelected(actionId) },
                            onCustomPromptSubmit = { prompt -> onCustomPromptSubmit(prompt) },
                            onSelectTone = { selectedTone -> onToneSelected(selectedTone) },
                            onReplace = { applyPreviewReplacement() },
                            onInsert = { insertPreviewText() },
                            onCopy = { copyPreviewText() },
                            onCancelPreview = { setUiState(FloatingUiState.EXPANDED) },
                            onUndo = { undoLastChange() },
                            onOpenSettings = { openAppearanceSettings() },
                            onCollapse = { setUiState(FloatingUiState.COLLAPSED) }
                        )
                    }
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            isViewAttached = true
            updateWindowLayoutForState(_uiState.value)
            Log.d(TAG, "Overlay attached on screen at x=$savedX, y=$savedY")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay", e)
            isViewAttached = false
        }
    }

    private fun detachOverlayView() {
        collapseResizeJob?.cancel()
        if (!isViewAttached || overlayView == null) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            // View might already be detached
        } finally {
            overlayView = null
            isViewAttached = false
        }
    }

    fun destroy() {
        hideAssistant()
        controllerScope.cancel()
        context.getSharedPreferences(AppearanceSettings.PREFS_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(appearancePrefsListener)
    }
}
