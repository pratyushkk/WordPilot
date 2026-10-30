package com.wordpilot.assistant.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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

    companion object {
        private var activeInstance: WordPilotInputMethodService? = null

        fun requestOpenExpandedAssistant(): Boolean {
            val instance = activeInstance ?: return false
            instance.suggestionController.openExpandedAssistant()
            return true
        }

        fun isImeActive(): Boolean = activeInstance != null
    }

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
        activeInstance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        suggestionController = KeyboardSuggestionController(serviceScope)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

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
                        onCommitCharacter = { char ->
                            currentInputConnection?.commitText(char, 1)
                        },
                        onAcceptCorrection = { correction ->
                            suggestionController.acceptCorrection(correction)
                        },
                        onDismissCorrection = {
                            suggestionController.dismissCorrection()
                        },
                        onToggleExpandCard = {
                            suggestionController.openExpandedAssistant()
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
                            suggestionController.openExpandedAssistant()
                        },
                        onCloseAssistant = {
                            suggestionController.closeExpandedAssistant()
                        },
                        onApplyAssistantResult = { text ->
                            suggestionController.applyAssistantResult(text)
                        },
                        onRefreshAssistantText = {
                            suggestionController.refreshAssistantText()
                        },
                        onUpdateAssistantSourceText = { text ->
                            suggestionController.updateAssistantSourceText(text)
                        },
                        onSelectAssistantStyle = { style ->
                            suggestionController.selectAssistantStyle(style)
                        },
                        onCustomInstructionChanged = { inst ->
                            suggestionController.updateAssistantCustomInstruction(inst)
                        },
                        onSelectAssistantEngine = { engine ->
                            suggestionController.selectAssistantEngine(engine)
                        },
                        onRetryAssistantRewrite = {
                            suggestionController.executeAssistantRewrite()
                        },
                        onResizeClicked = {
                            suggestionController.setMode(KeyboardMode.RESIZE)
                        },
                        onOpenResizeMode = {
                            suggestionController.setMode(KeyboardMode.RESIZE)
                        },
                        onSaveResizeDimensions = { height, bottom, side ->
                            suggestionController.setKeyboardDimensions(height, bottom, side)
                        },
                        onResetResizeDimensions = {
                            suggestionController.resetKeyboardDimensions()
                        },
                        onChangeBottomPadding = {
                            suggestionController.setBottomPadding(it)
                        },
                        onChangeSidePadding = {
                            suggestionController.setSidePadding(it)
                        },
                        onTextEditingClicked = {
                            suggestionController.setMode(KeyboardMode.TEXT_EDITING)
                        },
                        onClipboardClicked = {
                            suggestionController.setMode(KeyboardMode.CLIPBOARD)
                        },
                        onSettingsClicked = {
                            suggestionController.setMode(KeyboardMode.EDIT_SETTINGS)
                        },
                        onHideKeyboard = {
                            requestHideSelf(0)
                        },
                        onMoveCursor = { dir ->
                            suggestionController.moveCursor(dir)
                        },
                        onJumpToStart = {
                            suggestionController.jumpToStart()
                        },
                        onJumpToEnd = {
                            suggestionController.jumpToEnd()
                        },
                        onSelectAll = {
                            suggestionController.selectAll()
                        },
                        onToggleSelect = {
                            suggestionController.toggleSelectionMode()
                        },
                        isSelecting = suggestionController.isSelecting,
                        onCut = {
                            suggestionController.cutText()
                        },
                        onCopy = {
                            suggestionController.copyText()
                        },
                        onPaste = {
                            suggestionController.pasteText()
                        },
                        onToggleNumberRow = {
                            suggestionController.toggleNumberRow(it)
                        },
                        onToggleKeyHints = {
                            suggestionController.toggleKeyHints(it)
                        },
                        onChangeKeyHeight = {
                            suggestionController.setKeyHeight(it)
                        },
                        onToggleHaptic = {
                            suggestionController.toggleHaptic(it)
                        },
                        onPasteClip = {
                            suggestionController.pasteClipboardText(it)
                        },
                        onClearClips = {
                            suggestionController.clearClipboard()
                        },
                        onEmojiClicked = { emoji ->
                            currentInputConnection?.commitText(emoji, 1)
                        },
                        onClosePanel = {
                            suggestionController.setMode(KeyboardMode.QWERTY)
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

        if (!restarting) {
            val currentMode = suggestionController.uiState.value.mode
            if (currentMode != KeyboardMode.EXPANDED_ASSISTANT) {
                suggestionController.setMode(KeyboardMode.QWERTY)
            }
        }

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
            is KeyAction.DeleteWord -> {
                deleteWord(ic)
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
            is KeyAction.Emoji -> {
                suggestionController.setMode(KeyboardMode.EMOJI)
            }
            is KeyAction.TextEditing -> {
                suggestionController.setMode(KeyboardMode.TEXT_EDITING)
            }
            is KeyAction.Clipboard -> {
                suggestionController.setMode(KeyboardMode.CLIPBOARD)
            }
            is KeyAction.EditKeyboard -> {
                suggestionController.setMode(KeyboardMode.EDIT_SETTINGS)
            }
            is KeyAction.ResizeKeyboard -> {
                suggestionController.setMode(KeyboardMode.RESIZE)
            }
            is KeyAction.WordPilotAssistant -> {
                suggestionController.openExpandedAssistant()
            }
        }
    }

    private fun deleteWord(ic: InputConnection) {
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }

        val textBefore = ic.getTextBeforeCursor(100, 0)?.toString() ?: ""
        if (textBefore.isEmpty()) {
            ic.deleteSurroundingText(1, 0)
            return
        }

        var i = textBefore.length - 1
        while (i >= 0 && textBefore[i].isWhitespace()) {
            i--
        }

        if (i >= 0 && !textBefore[i].isLetterOrDigit()) {
            while (i >= 0 && !textBefore[i].isLetterOrDigit() && !textBefore[i].isWhitespace()) {
                i--
            }
        } else {
            while (i >= 0 && (textBefore[i].isLetterOrDigit() || textBefore[i] == '_')) {
                i--
            }
        }

        val count = (textBefore.length - (i + 1)).coerceAtLeast(1)
        ic.deleteSurroundingText(count, 0)
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

    override fun onWindowShown() {
        super.onWindowShown()
        com.wordpilot.assistant.utils.KeyboardVisibilityTracker.isWordPilotImeVisible = true
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        com.wordpilot.assistant.utils.KeyboardVisibilityTracker.isWordPilotImeVisible = false
    }

    override fun onDestroy() {
        com.wordpilot.assistant.utils.KeyboardVisibilityTracker.isWordPilotImeVisible = false
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
    }
}
