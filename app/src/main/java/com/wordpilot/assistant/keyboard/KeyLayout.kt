package com.wordpilot.assistant.keyboard

sealed class KeyAction {
    data class Character(
        val primary: String,
        val shifted: String = primary.uppercase(),
        val hint: String? = null
    ) : KeyAction()

    object Shift : KeyAction()
    object Backspace : KeyAction()
    object DeleteWord : KeyAction()
    object Enter : KeyAction()
    object Space : KeyAction()
    object SwitchMode : KeyAction()
    object SwitchToLetters : KeyAction()
    object SwitchToSymbols : KeyAction()
    object Emoji : KeyAction()
    object TextEditing : KeyAction()
    object Clipboard : KeyAction()
    object EditKeyboard : KeyAction()
    object ResizeKeyboard : KeyAction()
    object WordPilotAssistant : KeyAction()
}

data class KeyModel(
    val action: KeyAction,
    val weight: Float = 1f,
    val isSpecial: Boolean = false,
    val isAction: Boolean = false
)

object KeyLayout {

    // Number Row (1 2 3 4 5 6 7 8 9 0)
    val numberRow = listOf(
        KeyModel(KeyAction.Character("1")),
        KeyModel(KeyAction.Character("2")),
        KeyModel(KeyAction.Character("3")),
        KeyModel(KeyAction.Character("4")),
        KeyModel(KeyAction.Character("5")),
        KeyModel(KeyAction.Character("6")),
        KeyModel(KeyAction.Character("7")),
        KeyModel(KeyAction.Character("8")),
        KeyModel(KeyAction.Character("9")),
        KeyModel(KeyAction.Character("0"))
    )

    // QWERTY Row 1 (q w e r t y u i o p) with Gboard hints
    val qwertyRow1 = listOf(
        KeyModel(KeyAction.Character("q", hint = "1")),
        KeyModel(KeyAction.Character("w", hint = "2")),
        KeyModel(KeyAction.Character("e", hint = "3")),
        KeyModel(KeyAction.Character("r", hint = "4")),
        KeyModel(KeyAction.Character("t", hint = "5")),
        KeyModel(KeyAction.Character("y", hint = "6")),
        KeyModel(KeyAction.Character("u", hint = "7")),
        KeyModel(KeyAction.Character("i", hint = "8")),
        KeyModel(KeyAction.Character("o", hint = "9")),
        KeyModel(KeyAction.Character("p", hint = "0"))
    )

    // QWERTY Row 2 (a s d f g h j k l) with Gboard hints
    val qwertyRow2 = listOf(
        KeyModel(KeyAction.Character("a", hint = "@")),
        KeyModel(KeyAction.Character("s", hint = "#")),
        KeyModel(KeyAction.Character("d", hint = "$")),
        KeyModel(KeyAction.Character("f", hint = "_")),
        KeyModel(KeyAction.Character("g", hint = "&")),
        KeyModel(KeyAction.Character("h", hint = "-")),
        KeyModel(KeyAction.Character("j", hint = "+")),
        KeyModel(KeyAction.Character("k", hint = "(")),
        KeyModel(KeyAction.Character("l", hint = ")"))
    )

    // QWERTY Row 3 (Shift, z x c v b n m, Backspace)
    val qwertyRow3 = listOf(
        KeyModel(KeyAction.Shift, weight = 1.4f, isSpecial = true),
        KeyModel(KeyAction.Character("z", hint = "*")),
        KeyModel(KeyAction.Character("x", hint = "\"")),
        KeyModel(KeyAction.Character("c", hint = "'")),
        KeyModel(KeyAction.Character("v", hint = ":")),
        KeyModel(KeyAction.Character("b", hint = ";")),
        KeyModel(KeyAction.Character("n", hint = "!")),
        KeyModel(KeyAction.Character("m", hint = "?")),
        KeyModel(KeyAction.Backspace, weight = 1.4f, isSpecial = true)
    )

    // QWERTY Bottom Row (?123, Emoji, Comma, Space [English], Period, Enter)
    val qwertyBottomRow = listOf(
        KeyModel(KeyAction.SwitchMode, weight = 1.35f, isSpecial = true),
        KeyModel(KeyAction.Emoji, weight = 1.0f, isSpecial = true),
        KeyModel(KeyAction.Character(",", hint = "⚙"), weight = 0.95f),
        KeyModel(KeyAction.Space, weight = 4.2f),
        KeyModel(KeyAction.Character(".", hint = "…"), weight = 0.95f),
        KeyModel(KeyAction.Enter, weight = 1.55f, isSpecial = true, isAction = true)
    )

    // Numbers Layout Row 1
    val numbersRow1 = listOf(
        KeyModel(KeyAction.Character("1")),
        KeyModel(KeyAction.Character("2")),
        KeyModel(KeyAction.Character("3")),
        KeyModel(KeyAction.Character("4")),
        KeyModel(KeyAction.Character("5")),
        KeyModel(KeyAction.Character("6")),
        KeyModel(KeyAction.Character("7")),
        KeyModel(KeyAction.Character("8")),
        KeyModel(KeyAction.Character("9")),
        KeyModel(KeyAction.Character("0"))
    )

    // Numbers Layout Row 2
    val numbersRow2 = listOf(
        KeyModel(KeyAction.Character("@")),
        KeyModel(KeyAction.Character("#")),
        KeyModel(KeyAction.Character("$")),
        KeyModel(KeyAction.Character("_")),
        KeyModel(KeyAction.Character("&")),
        KeyModel(KeyAction.Character("-")),
        KeyModel(KeyAction.Character("+")),
        KeyModel(KeyAction.Character("(")),
        KeyModel(KeyAction.Character(")"))
    )

    // Numbers Layout Row 3
    val numbersRow3 = listOf(
        KeyModel(KeyAction.SwitchToSymbols, weight = 1.4f, isSpecial = true), // =\<
        KeyModel(KeyAction.Character("*")),
        KeyModel(KeyAction.Character("\"")),
        KeyModel(KeyAction.Character("'")),
        KeyModel(KeyAction.Character(":")),
        KeyModel(KeyAction.Character(";")),
        KeyModel(KeyAction.Character("!")),
        KeyModel(KeyAction.Character("?")),
        KeyModel(KeyAction.Backspace, weight = 1.4f, isSpecial = true)
    )

    // Numbers Bottom Row
    val numbersBottomRow = listOf(
        KeyModel(KeyAction.SwitchToLetters, weight = 1.35f, isSpecial = true),
        KeyModel(KeyAction.Emoji, weight = 1.0f, isSpecial = true),
        KeyModel(KeyAction.Character("/")),
        KeyModel(KeyAction.Space, weight = 4.2f),
        KeyModel(KeyAction.Character(".")),
        KeyModel(KeyAction.Enter, weight = 1.55f, isSpecial = true, isAction = true)
    )

    // Symbols Layout Row 1
    val symbolsRow1 = listOf(
        KeyModel(KeyAction.Character("~")),
        KeyModel(KeyAction.Character("`")),
        KeyModel(KeyAction.Character("|")),
        KeyModel(KeyAction.Character("•")),
        KeyModel(KeyAction.Character("√")),
        KeyModel(KeyAction.Character("π")),
        KeyModel(KeyAction.Character("÷")),
        KeyModel(KeyAction.Character("×")),
        KeyModel(KeyAction.Character("¶")),
        KeyModel(KeyAction.Character("∆"))
    )

    // Symbols Layout Row 2
    val symbolsRow2 = listOf(
        KeyModel(KeyAction.Character("£")),
        KeyModel(KeyAction.Character("€")),
        KeyModel(KeyAction.Character("¥")),
        KeyModel(KeyAction.Character("¢")),
        KeyModel(KeyAction.Character("^")),
        KeyModel(KeyAction.Character("°")),
        KeyModel(KeyAction.Character("=")),
        KeyModel(KeyAction.Character("{")),
        KeyModel(KeyAction.Character("}"))
    )

    // Symbols Layout Row 3
    val symbolsRow3 = listOf(
        KeyModel(KeyAction.SwitchMode, weight = 1.4f, isSpecial = true), // ?123
        KeyModel(KeyAction.Character("\\")),
        KeyModel(KeyAction.Character("%")),
        KeyModel(KeyAction.Character("©")),
        KeyModel(KeyAction.Character("®")),
        KeyModel(KeyAction.Character("™")),
        KeyModel(KeyAction.Character("[")),
        KeyModel(KeyAction.Character("]")),
        KeyModel(KeyAction.Backspace, weight = 1.4f, isSpecial = true)
    )

    // Symbols Bottom Row
    val symbolsBottomRow = listOf(
        KeyModel(KeyAction.SwitchToLetters, weight = 1.35f, isSpecial = true),
        KeyModel(KeyAction.Emoji, weight = 1.0f, isSpecial = true),
        KeyModel(KeyAction.Character("<")),
        KeyModel(KeyAction.Space, weight = 4.2f),
        KeyModel(KeyAction.Character(">")),
        KeyModel(KeyAction.Enter, weight = 1.55f, isSpecial = true, isAction = true)
    )
}
