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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.domain.models.EditorSnapshot
import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.domain.models.TextOperationResult
import com.wordpilot.assistant.overlay.OverlayGeometry
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
    private var onApplySentenceCallback: ((String) -> TextOperationResult)? = null
    private var onInsertCallback: ((String) -> TextOperationResult)? = null
    private var onUndoCallback: (() -> TextOperationResult)? = null
    private var onAcceptSingleCorrectionCallback: ((Correction) -> Unit)? = null
    private var onAddToDictionaryCallback: ((Correction) -> Unit)? = null
    private var onIgnoreRuleCallback: ((Correction) -> Unit)? = null

    private var currentRequestId: Long = 0L
    private var bubbleRestingX: Int = 0
    private var bubbleRestingY: Int = 0

    private val appearancePrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _appearanceSettings.value = AppearanceSettings.load(context)
        updateWindowLayoutForState(_uiState.value)
    }

    init {
        context.getSharedPreferences(AppearanceSettings.PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(appearancePrefsListener)
    }

    fun setUiState(targetState: FloatingUiState) {
        if (targetState == FloatingUiState.COLLAPSED) {
            _uiState.value = FloatingUiState.COLLAPSED
            // The native window stays at expanded bounds while Compose animates.
            // onCollapseTransitionCompleted() resizes the WindowManager bounds once Compose finishes!
        } else {
            updateWindowLayoutForState(targetState)
            _uiState.value = targetState
        }
    }

    fun onCollapseTransitionCompleted() {
        if (_uiState.value == FloatingUiState.COLLAPSED) {
            updateWindowLayoutForState(FloatingUiState.COLLAPSED)
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
        val collapsedSizePx = OverlayGeometry.getCollapsedSizePx(density, s.bubbleSizeDp)

        when (state) {
            FloatingUiState.COLLAPSED -> {
                lp.width = collapsedSizePx
                lp.height = collapsedSizePx
                lp.x = bubbleRestingX
                lp.y = bubbleRestingY
            }
            FloatingUiState.EXPANDED -> {
                val expandedWidthPx = OverlayGeometry.getExpandedWidthPx(density, screenWidthPx, s.toolbarMaxWidthDp)
                val expandedHeightPx = OverlayGeometry.getExpandedHeightPx(density, s.toolbarHeightDp, s.bubbleSizeDp)

                lp.width = expandedWidthPx
                lp.height = expandedHeightPx
                lp.x = OverlayGeometry.calculateEdgeAnchoredX(
                    isNearRightEdge = _isNearRightEdge.value,
                    bubbleRestingX = bubbleRestingX,
                    collapsedWidthPx = collapsedSizePx,
                    targetWidthPx = expandedWidthPx,
                    screenWidthPx = screenWidthPx,
                    density = density
                )
                lp.y = bubbleRestingY
            }
            FloatingUiState.MORE_OPTIONS -> {
                val panelWidthPx = OverlayGeometry.getMoreOptionsWidthPx(density, screenWidthPx)
                val panelHeightPx = OverlayGeometry.getMoreOptionsHeightPx(density)

                lp.width = panelWidthPx
                lp.height = panelHeightPx
                lp.x = OverlayGeometry.calculateEdgeAnchoredX(
                    isNearRightEdge = _isNearRightEdge.value,
                    bubbleRestingX = bubbleRestingX,
                    collapsedWidthPx = collapsedSizePx,
                    targetWidthPx = panelWidthPx,
                    screenWidthPx = screenWidthPx,
                    density = density
                )
                lp.y = OverlayGeometry.clampY(bubbleRestingY, panelHeightPx, screenHeightPx, density)
            }
            FloatingUiState.PREVIEW -> {
                val previewWidthPx = OverlayGeometry.getPreviewWidthPx(density, screenWidthPx)
                val previewHeightPx = OverlayGeometry.getPreviewHeightPx(density)

                lp.width = previewWidthPx
                lp.height = previewHeightPx
                lp.x = OverlayGeometry.calculateEdgeAnchoredX(
                    isNearRightEdge = _isNearRightEdge.value,
                    bubbleRestingX = bubbleRestingX,
                    collapsedWidthPx = collapsedSizePx,
                    targetWidthPx = previewWidthPx,
                    screenWidthPx = screenWidthPx,
                    density = density
                )
                lp.y = OverlayGeometry.clampY(bubbleRestingY, previewHeightPx, screenHeightPx, density)
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
        snapshot: EditorSnapshot,
        onApplySentence: (String) -> TextOperationResult,
        onUndo: (() -> TextOperationResult)? = null,
        onInsert: ((String) -> TextOperationResult)? = null
    ) {
        _currentFullText.value = snapshot.fullText
        _currentSentence.value = snapshot.sentenceText
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
        this.onAddToDictionaryCallback = onAddToDictionary
        this.onIgnoreRuleCallback = onIgnoreRule

        if (!isViewAttached) {
            attachOverlayView()
        }
    }

    fun clearCorrection() {
        _activeCorrection.value = null
        _errorCount.value = 0
        cachedTextCorrections = null
    }

    fun dismiss() {
        clearCorrection()
        setUiState(FloatingUiState.COLLAPSED)
    }

    // Cached analysis results for instant preview
    private var cachedTextCorrections: Pair<String, List<Correction>>? = null

    fun setCachedCorrections(text: String, corrections: List<Correction>) {
        cachedTextCorrections = Pair(text, corrections)
    }

    // =========================================================================
    // ACTION DISPATCHERS
    // =========================================================================

    fun onActionSelected(actionId: String) {
        when (actionId) {
            "fix_grammar", "punctuation" -> {
                executeFixGrammar()
            }
            "settings" -> {
                openAppearanceSettings()
            }
            "more" -> {
                setUiState(FloatingUiState.MORE_OPTIONS)
            }
            "back_to_expanded" -> {
                setUiState(FloatingUiState.COLLAPSED)
            }
            else -> {
                executeFixGrammar()
            }
        }
    }

    fun executeFixGrammar() {
        val targetText = getTargetText()
        if (targetText.isBlank()) {
            Toast.makeText(context, "No text detected to fix", Toast.LENGTH_SHORT).show()
            return
        }

        // Fast-path: If accessibility service already analyzed this text, show result instantly
        val cached = cachedTextCorrections
        if (cached != null && cached.first == targetText && cached.second.isNotEmpty()) {
            val fixed = safeReplacementHelper.applyMultipleCorrections(targetText, cached.second)
            if (fixed.isNotBlank()) {
                _previewOriginalText.value = targetText
                _previewImprovedText.value = fixed
                _previewErrorMessage.value = null
                _isPreviewLoading.value = false
                setUiState(FloatingUiState.PREVIEW)
                return
            }
        }

        val requestId = ++currentRequestId
        _previewOriginalText.value = targetText
        _previewImprovedText.value = ""
        _previewErrorMessage.value = null
        _isPreviewLoading.value = true
        setUiState(FloatingUiState.PREVIEW)

        controllerScope.launch {
            try {
                val app = WordPilotApp.instance
                val corrections = withContext(Dispatchers.Default) {
                    app.correctionCoordinator.checkText(targetText)
                }

                if (requestId != currentRequestId) return@launch

                var fixed = targetText
                if (corrections.isNotEmpty()) {
                    val replaced = safeReplacementHelper.applyMultipleCorrections(targetText, corrections)
                    if (replaced.isNotBlank()) {
                        fixed = replaced
                    }
                }

                if (requestId != currentRequestId) return@launch
                _previewImprovedText.value = fixed
                _isPreviewLoading.value = false
            } catch (e: Exception) {
                if (requestId != currentRequestId) return@launch
                Log.e(TAG, "Error in executeFixGrammar", e)
                _previewErrorMessage.value = "Failed to fix grammar: ${e.localizedMessage}"
                _isPreviewLoading.value = false
            }
        }
    }

    private fun executeRewrite(@Suppress("UNUSED_PARAMETER") style: RewriteStyle, @Suppress("UNUSED_PARAMETER") toneName: String, @Suppress("UNUSED_PARAMETER") customPrompt: String? = null) {
        executeFixGrammar()
    }

    fun onCustomPromptSubmit(@Suppress("UNUSED_PARAMETER") prompt: String) {
        executeFixGrammar()
    }

    fun onToneSelected(@Suppress("UNUSED_PARAMETER") tone: String) {
        executeFixGrammar()
    }

    fun onSelectEngine(@Suppress("UNUSED_PARAMETER") engine: AIEngineType) {
        val app = WordPilotApp.instance
        app.rewriteCoordinator.setEngineType(AIEngineType.GECTOR)
        controllerScope.launch {
            app.userPreferences.setSelectedEngine(AIEngineType.GECTOR.id)
        }
    }

    fun applyPreviewReplacement() {
        val improved = _previewImprovedText.value
        if (improved.isBlank()) return

        val callback = onApplySentenceCallback
        if (callback != null) {
            val result = callback(improved)
            when (result) {
                is TextOperationResult.Success -> {
                    _canUndo.value = true
                    setUiState(FloatingUiState.COLLAPSED)
                    clearCorrection()
                    Toast.makeText(context, "Text updated", Toast.LENGTH_SHORT).show()
                }
                is TextOperationResult.StaleSnapshot -> {
                    Toast.makeText(context, "Editor text was modified. Please review.", Toast.LENGTH_LONG).show()
                }
                is TextOperationResult.EditorModified -> {
                    Toast.makeText(context, "Editor was modified", Toast.LENGTH_SHORT).show()
                }
                is TextOperationResult.UnsupportedEditor -> {
                    copyPreviewText()
                    Toast.makeText(context, "Direct edit unsupported. Text copied to clipboard.", Toast.LENGTH_LONG).show()
                }
                is TextOperationResult.Failure -> {
                    Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            copyPreviewText()
        }
    }

    fun insertPreviewText() {
        val textToInsert = _previewImprovedText.value
        if (textToInsert.isBlank()) return

        val callback = onInsertCallback
        if (callback != null) {
            val result = callback(textToInsert)
            when (result) {
                is TextOperationResult.Success -> {
                    _canUndo.value = true
                    setUiState(FloatingUiState.COLLAPSED)
                    Toast.makeText(context, "Text inserted", Toast.LENGTH_SHORT).show()
                }
                is TextOperationResult.Failure -> {
                    Toast.makeText(context, result.reason, Toast.LENGTH_LONG).show()
                }
                else -> {
                    copyPreviewText()
                    Toast.makeText(context, "Direct insert unsupported. Copied to clipboard.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            copyPreviewText()
        }
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
        val callback = onUndoCallback
        if (callback != null) {
            val result = callback()
            when (result) {
                is TextOperationResult.Success -> {
                    _canUndo.value = false
                    Toast.makeText(context, "Change undone", Toast.LENGTH_SHORT).show()
                }
                is TextOperationResult.EditorModified -> {
                    _canUndo.value = false
                    Toast.makeText(context, "Cannot undo: editor text has changed", Toast.LENGTH_LONG).show()
                }
                is TextOperationResult.Failure -> {
                    Toast.makeText(context, result.reason, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    _canUndo.value = false
                }
            }
        }
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
        return _currentFullText.value.ifBlank { _currentSentence.value }
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
        val collapsedSizePx = OverlayGeometry.getCollapsedSizePx(density, s.bubbleSizeDp)

        val overlayType = if (context is AccessibilityService) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            collapsedSizePx,
            collapsedSizePx,
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
                val activeEngine by WordPilotApp.instance.rewriteCoordinator.selectedEngine.collectAsState()

                WordPilotTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentUiState) {
                                if (currentUiState != FloatingUiState.COLLAPSED) {
                                    detectTapGestures {
                                        setUiState(FloatingUiState.COLLAPSED)
                                    }
                                }
                            }
                            .padding(horizontal = OverlayGeometry.CONTENT_PADDING_DP.dp, vertical = OverlayGeometry.CONTENT_PADDING_DP.dp),
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
                                    executeFixGrammar()
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
                            onCancelPreview = { setUiState(FloatingUiState.COLLAPSED) },
                            onUndo = { undoLastChange() },
                            onOpenSettings = { openAppearanceSettings() },
                            onCollapse = { setUiState(FloatingUiState.COLLAPSED) },
                            onCollapseTransitionCompleted = { onCollapseTransitionCompleted() },
                            selectedEngine = activeEngine,
                            onSelectEngine = { onSelectEngine(it) }
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
