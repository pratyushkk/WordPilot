package com.wordpilot.assistant.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.wordpilot.assistant.MainActivity
import com.wordpilot.assistant.privacy.SensitiveInputGuard
import com.wordpilot.assistant.ui.theme.WordPilotTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class WordPilotInputMethodService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var suggestionController: KeyboardSuggestionController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        suggestionController = KeyboardSuggestionController(serviceScope)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@WordPilotInputMethodService)
            setViewTreeViewModelStoreOwner(this@WordPilotInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@WordPilotInputMethodService)

            setContent {
                val uiState by suggestionController.uiState.collectAsState()

                WordPilotTheme {
                    WordPilotKeyboardView(
                        uiState = uiState,
                        onKeyClicked = { action -> handleKeyAction(action, uiState) },
                        onAcceptCorrection = { correction ->
                            suggestionController.acceptCorrection(correction)
                        },
                        onDismissCorrection = {
                            suggestionController.dismissCorrection()
                        },
                        onToggleExpandCard = {
                            suggestionController.toggleCardExpanded()
                        },
                        onAddToDictionary = { correction ->
                            suggestionController.addToDictionary(correction)
                        },
                        onIgnoreRule = { correction ->
                            suggestionController.ignoreRule(correction)
                        },
                        onWordSuggestionClicked = { word ->
                            applyWordSuggestion(word)
                        },
                        onAssistantClicked = {
                            val intent = Intent(this@WordPilotInputMethodService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }

        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        suggestionController.setInputConnection(currentInputConnection)

        val isSensitive = SensitiveInputGuard.isSensitiveField(info)
        suggestionController.setSensitiveField(isSensitive)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        suggestionController.setInputConnection(null)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(200, 0)
        val textAfter = ic.getTextAfterCursor(100, 0)
        suggestionController.onTextChanged(textBefore, textAfter)
    }

    private fun handleKeyAction(action: KeyAction, uiState: KeyboardUiState) {
        val ic = currentInputConnection ?: return

        when (action) {
            is KeyAction.Character -> {
                val isShifted = uiState.shiftState != ShiftState.OFF
                val charToInsert = if (isShifted) action.shifted else action.primary
                ic.commitText(charToInsert, 1)
                suggestionController.resetShiftAfterCharacter()
            }
            is KeyAction.Space -> {
                ic.commitText(" ", 1)
            }
            is KeyAction.Backspace -> {
                val selected = ic.getSelectedText(0)
                if (selected.isNullOrEmpty()) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
            }
            is KeyAction.Enter -> {
                val editorInfo = currentInputEditorInfo
                val imeOptions = editorInfo?.imeOptions ?: 0
                val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
                if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    ic.commitText("\n", 1)
                }
            }
            is KeyAction.Shift -> {
                suggestionController.toggleShift()
            }
            is KeyAction.SwitchMode -> {
                suggestionController.setMode(KeyboardMode.NUMBERS)
            }
            is KeyAction.SwitchToLetters -> {
                suggestionController.setMode(KeyboardMode.QWERTY)
            }
            is KeyAction.SwitchToSymbols -> {
                suggestionController.setMode(KeyboardMode.SYMBOLS)
            }
            is KeyAction.WordPilotAssistant -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }

    private fun applyWordSuggestion(suggestion: String) {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(50, 0)?.toString() ?: ""
        val lastWord = textBefore.split("\\s+".toRegex()).lastOrNull() ?: ""

        if (lastWord.isNotEmpty()) {
            ic.deleteSurroundingText(lastWord.length, 0)
        }
        ic.commitText("$suggestion ", 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
    }
}
