package com.wordpilot.assistant.overlay

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
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
import com.wordpilot.assistant.domain.models.Correction
import com.wordpilot.assistant.ui.theme.WordPilotTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private var overlayView: View? = null
    private var isViewAttached = false
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private val _activeCorrection = MutableStateFlow<Correction?>(null)
    val activeCorrection: StateFlow<Correction?> = _activeCorrection.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    private val _customWidthDp = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_WIDTH_DP, DEFAULT_WIDTH_DP))
    val customWidthDp: StateFlow<Float> = _customWidthDp.asStateFlow()

    private val _customHeightDp = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_HEIGHT_DP, DEFAULT_HEIGHT_DP))
    val customHeightDp: StateFlow<Float> = _customHeightDp.asStateFlow()

    private val _customTextSizeSp = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_TEXT_SIZE_SP, DEFAULT_TEXT_SIZE_SP))
    val customTextSizeSp: StateFlow<Float> = _customTextSizeSp.asStateFlow()

    private val _customCornerRadiusDp = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_CORNER_RADIUS_DP, DEFAULT_CORNER_RADIUS_DP))
    val customCornerRadiusDp: StateFlow<Float> = _customCornerRadiusDp.asStateFlow()

    private val _customBubbleSizeDp = MutableStateFlow(prefs.getFloat(KEY_CUSTOM_BUBBLE_SIZE_DP, DEFAULT_BUBBLE_SIZE_DP))
    val customBubbleSizeDp: StateFlow<Float> = _customBubbleSizeDp.asStateFlow()

    private val _isCustomizing = MutableStateFlow(false)
    val isCustomizing: StateFlow<Boolean> = _isCustomizing.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when (key) {
            KEY_CUSTOM_WIDTH_DP -> _customWidthDp.value = sp.getFloat(KEY_CUSTOM_WIDTH_DP, DEFAULT_WIDTH_DP)
            KEY_CUSTOM_HEIGHT_DP -> _customHeightDp.value = sp.getFloat(KEY_CUSTOM_HEIGHT_DP, DEFAULT_HEIGHT_DP)
            KEY_CUSTOM_TEXT_SIZE_SP -> _customTextSizeSp.value = sp.getFloat(KEY_CUSTOM_TEXT_SIZE_SP, DEFAULT_TEXT_SIZE_SP)
            KEY_CUSTOM_CORNER_RADIUS_DP -> _customCornerRadiusDp.value = sp.getFloat(KEY_CUSTOM_CORNER_RADIUS_DP, DEFAULT_CORNER_RADIUS_DP)
            KEY_CUSTOM_BUBBLE_SIZE_DP -> _customBubbleSizeDp.value = sp.getFloat(KEY_CUSTOM_BUBBLE_SIZE_DP, DEFAULT_BUBBLE_SIZE_DP)
            KEY_MINIMIZED -> _isMinimized.value = sp.getBoolean(KEY_MINIMIZED, false)
        }
    }

    private var onAcceptCallback: ((Correction) -> Unit)? = null
    private var onAddToDictionaryCallback: ((Correction) -> Unit)? = null
    private var onIgnoreRuleCallback: ((Correction) -> Unit)? = null

    init {
        _isMinimized.value = prefs.getBoolean(KEY_MINIMIZED, false)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    fun updateDimensions(w: Float, h: Float, ts: Float, cr: Float, bs: Float) {
        _customWidthDp.value = w
        _customHeightDp.value = h
        _customTextSizeSp.value = ts
        _customCornerRadiusDp.value = cr
        _customBubbleSizeDp.value = bs
        prefs.edit()
            .putFloat(KEY_CUSTOM_WIDTH_DP, w)
            .putFloat(KEY_CUSTOM_HEIGHT_DP, h)
            .putFloat(KEY_CUSTOM_TEXT_SIZE_SP, ts)
            .putFloat(KEY_CUSTOM_CORNER_RADIUS_DP, cr)
            .putFloat(KEY_CUSTOM_BUBBLE_SIZE_DP, bs)
            .apply()
    }

    fun toggleCustomizing() {
        _isCustomizing.value = !_isCustomizing.value
    }

    fun canDrawOverlays(): Boolean {
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

    fun setChecking(checking: Boolean) {
        _isChecking.value = checking
    }

    fun showCorrection(
        correction: Correction,
        onAccept: (Correction) -> Unit,
        onAddToDictionary: ((Correction) -> Unit)? = null,
        onIgnoreRule: ((Correction) -> Unit)? = null
    ) {
        if (!canDrawOverlays()) return

        _activeCorrection.value = correction
        _isExpanded.value = false
        this.onAcceptCallback = onAccept
        this.onAddToDictionaryCallback = onAddToDictionary
        this.onIgnoreRuleCallback = onIgnoreRule

        if (!isViewAttached) {
            attachOverlayView()
        }
    }

    fun clearCorrection() {
        _activeCorrection.value = null
        _isExpanded.value = false
    }

    fun dismiss() {
        clearCorrection()
    }

    fun hideAssistant() {
        _activeCorrection.value = null
        _isExpanded.value = false
        detachOverlayView()
    }

    fun toggleExpand() {
        _isExpanded.value = !_isExpanded.value
    }

    fun toggleMinimize() {
        val willMinimize = !_isMinimized.value
        _isMinimized.value = willMinimize
        prefs.edit().putBoolean(KEY_MINIMIZED, willMinimize).apply()

        val view = overlayView ?: return
        val lp = windowLayoutParams ?: return
        val screenWidth = context.resources.displayMetrics.widthPixels
        val density = context.resources.displayMetrics.density

        if (willMinimize) {
            // Snap to the extreme closest edge (0 gap on left, or flush on right)
            lp.x = if (lp.x < screenWidth / 2) {
                0 // Extreme left edge
            } else {
                screenWidth - (48 * density).toInt() // Extreme right edge
            }
        } else {
            // Restore comfortable offset or keep at extreme edge
            if (lp.x > screenWidth / 2) {
                lp.x = (screenWidth - (310 * density).toInt()).coerceAtLeast(0)
            } else {
                lp.x = 0 // Extreme left
            }
        }

        try {
            windowManager.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating minimize layout", e)
        }
    }

    /**
     * Move popup anywhere on screen (Free X & Y 2D dragging).
     * Can go all the way to the extreme 4 corners of the display.
     */
    fun onDrag(deltaX: Float, deltaY: Float) {
        val view = overlayView ?: return
        val lp = windowLayoutParams ?: return
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        val density = context.resources.displayMetrics.density

        val estimatedWidth = if (_isMinimized.value) (48 * density).toInt() else (310 * density).toInt()
        val estimatedHeight = if (_isMinimized.value) (48 * density).toInt() else (56 * density).toInt()

        // Full 2D range: reaches extreme 0 up to screen borders
        val newX = (lp.x + deltaX.toInt()).coerceIn(0, (screenWidth - estimatedWidth).coerceAtLeast(0))
        // Gravity includes Gravity.BOTTOM: positive deltaY means dragging down, which decreases y from bottom
        val newY = (lp.y - deltaY.toInt()).coerceIn(0, (screenHeight - estimatedHeight).coerceAtLeast(0))

        lp.x = newX
        lp.y = newY
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
        val defaultMarginX = (12 * density).toInt()
        val defaultY = (340 * density).toInt()

        val savedX = prefs.getInt(KEY_X_PX, defaultMarginX)
        val savedY = prefs.getInt(KEY_Y_PX, defaultY)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
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
                val currentCorrection by activeCorrection.collectAsState()
                val checking by isChecking.collectAsState()
                val expanded by isExpanded.collectAsState()
                val minimized by isMinimized.collectAsState()
                val customWidth by customWidthDp.collectAsState()
                val customHeight by customHeightDp.collectAsState()
                val customTextSize by customTextSizeSp.collectAsState()
                val customCornerRadius by customCornerRadiusDp.collectAsState()
                val customBubbleSize by customBubbleSizeDp.collectAsState()
                val customizing by isCustomizing.collectAsState()

                WordPilotTheme {
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(
                                start = if (minimized) 0.dp else 4.dp,
                                end = if (minimized) 0.dp else 4.dp,
                                top = 4.dp,
                                bottom = 4.dp
                            ),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        FloatingSuggestionCard(
                            correction = currentCorrection,
                            isChecking = checking,
                            isExpanded = expanded,
                            isMinimized = minimized,
                            customWidthDp = customWidth,
                            customHeightDp = customHeight,
                            customTextSizeSp = customTextSize,
                            customCornerRadiusDp = customCornerRadius,
                            customBubbleSizeDp = customBubbleSize,
                            isCustomizing = customizing,
                            onToggleCustomize = { toggleCustomizing() },
                            onUpdateDimensions = { w, h, ts, cr, bs ->
                                updateDimensions(w, h, ts, cr, bs)
                            },
                            onAccept = {
                                currentCorrection?.let { corr ->
                                    onAcceptCallback?.invoke(corr)
                                    clearCorrection()
                                }
                            },
                            onDismiss = { clearCorrection() },
                            onCloseAssistant = { hideAssistant() },
                            onToggleMinimize = { toggleMinimize() },
                            onToggleExpand = { toggleExpand() },
                            onAddToDictionary = {
                                currentCorrection?.let { corr ->
                                    onAddToDictionaryCallback?.invoke(corr)
                                    clearCorrection()
                                }
                            },
                            onIgnoreRule = {
                                currentCorrection?.let { corr ->
                                    onIgnoreRuleCallback?.invoke(corr)
                                    clearCorrection()
                                }
                            },
                            onDrag = { dx, dy ->
                                onDrag(dx, dy)
                            }
                        )
                    }
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            isViewAttached = true
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
    }
}
