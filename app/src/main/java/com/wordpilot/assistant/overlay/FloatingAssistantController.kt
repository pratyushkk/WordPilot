package com.wordpilot.assistant.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
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
import kotlin.math.abs

class FloatingAssistantController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val savedStateRegistryOwner: SavedStateRegistryOwner
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isViewAttached = false

    private val _activeCorrection = MutableStateFlow<Correction?>(null)
    val activeCorrection: StateFlow<Correction?> = _activeCorrection.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private var onAcceptCallback: ((Correction) -> Unit)? = null
    private var onAddToDictionaryCallback: ((Correction) -> Unit)? = null
    private var onIgnoreRuleCallback: ((Correction) -> Unit)? = null

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
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

    fun dismiss() {
        _activeCorrection.value = null
        _isExpanded.value = false
        detachOverlayView()
    }

    fun toggleExpand() {
        _isExpanded.value = !_isExpanded.value
    }

    private fun attachOverlayView() {
        if (isViewAttached) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
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
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 860
        }

        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@FloatingAssistantController.lifecycleOwner)
            setViewTreeViewModelStoreOwner(this@FloatingAssistantController.viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(this@FloatingAssistantController.savedStateRegistryOwner)

            setContent {
                val currentCorrection by activeCorrection.collectAsState()
                val expanded by isExpanded.collectAsState()

                currentCorrection?.let { correction ->
                    WordPilotTheme {
                        FloatingSuggestionCard(
                            correction = correction,
                            isExpanded = expanded,
                            onAccept = {
                                onAcceptCallback?.invoke(correction)
                                dismiss()
                            },
                            onDismiss = { dismiss() },
                            onToggleExpand = { toggleExpand() },
                            onAddToDictionary = {
                                onAddToDictionaryCallback?.invoke(correction)
                                dismiss()
                            },
                            onIgnoreRule = {
                                onIgnoreRuleCallback?.invoke(correction)
                                dismiss()
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
        } catch (e: Exception) {
            isViewAttached = false
        }
    }

    private fun detachOverlayView() {
        if (!isViewAttached || overlayView == null) return
        try {
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            // View might not be attached
        } finally {
            overlayView = null
            isViewAttached = false
        }
    }

    fun destroy() {
        dismiss()
    }
}
