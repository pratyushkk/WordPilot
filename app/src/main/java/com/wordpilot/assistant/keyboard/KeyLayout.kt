package com.wordpilot.assistant.keyboard

sealed class KeyAction {
    data class Character(val primary: String, val shifted: String = primary.uppercase()) : KeyAction()
    object Shift : KeyAction()
    object Backspace : KeyAction()
    object Enter : KeyAction()
    object Space : KeyAction()
    object SwitchMode : KeyAction()
    object SwitchToLetters : KeyAction()
    object SwitchToSymbols : KeyAction()
    object WordPilotAssistant : KeyAction()
}

data class KeyModel(
    val action: KeyAction,
    val weight: Float = 1f,
    val isSpecial: Boolean = false
)

object KeyLayout {

    val qwertyRow1 = listOf(
        KeyModel(KeyAction.Character("q")),
        KeyModel(KeyAction.Character("w")),
        KeyModel(KeyAction.Character("e")),
        KeyModel(KeyAction.Character("r")),
        KeyModel(KeyAction.Character("t")),
        KeyModel(KeyAction.Character("y")),
        KeyModel(KeyAction.Character("u")),
        KeyModel(KeyAction.Character("i")),
        KeyModel(KeyAction.Character("o")),
        KeyModel(KeyAction.Character("p"))
    )

    val qwertyRow2 = listOf(
        KeyModel(KeyAction.Character("a")),
        KeyModel(KeyAction.Character("s")),
        KeyModel(KeyAction.Character("d")),
        KeyModel(KeyAction.Character("f")),
        KeyModel(KeyAction.Character("g")),
        KeyModel(KeyAction.Character("h")),
        KeyModel(KeyAction.Character("j")),
        KeyModel(KeyAction.Character("k")),
        KeyModel(KeyAction.Character("l"))
    )

    val qwertyRow3 = listOf(
        KeyModel(KeyAction.Shift, weight = 1.5f, isSpecial = true),
        KeyModel(KeyAction.Character("z")),
        KeyModel(KeyAction.Character("x")),
        KeyModel(KeyAction.Character("c")),
        KeyModel(KeyAction.Character("v")),
        KeyModel(KeyAction.Character("b")),
        KeyModel(KeyAction.Character("n")),
        KeyModel(KeyAction.Character("m")),
        KeyModel(KeyAction.Backspace, weight = 1.5f, isSpecial = true)
    )

    val qwertyBottomRow = listOf(
        KeyModel(KeyAction.SwitchMode, weight = 1.5f, isSpecial = true), // ?123
        KeyModel(KeyAction.Character(","), weight = 1f),
        KeyModel(KeyAction.WordPilotAssistant, weight = 1f, isSpecial = true),
        KeyModel(KeyAction.Space, weight = 4.5f),
        KeyModel(KeyAction.Character("."), weight = 1f),
        KeyModel(KeyAction.Enter, weight = 1.8f, isSpecial = true)
    )

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

    val numbersRow2 = listOf(
        KeyModel(KeyAction.Character("@")),
        KeyModel(KeyAction.Character("#")),
        KeyModel(KeyAction.Character("$")),
        KeyModel(KeyAction.Character("%")),
        KeyModel(KeyAction.Character("&")),
        KeyModel(KeyAction.Character("-")),
        KeyModel(KeyAction.Character("+")),
        KeyModel(KeyAction.Character("(")),
        KeyModel(KeyAction.Character(")"))
    )

    val numbersRow3 = listOf(
        KeyModel(KeyAction.SwitchToSymbols, weight = 1.5f, isSpecial = true), // =<
        KeyModel(KeyAction.Character("*")),
        KeyModel(KeyAction.Character("\"")),
        KeyModel(KeyAction.Character("'")),
        KeyModel(KeyAction.Character(":")),
        KeyModel(KeyAction.Character(";")),
        KeyModel(KeyAction.Character("!")),
        KeyModel(KeyAction.Character("?")),
        KeyModel(KeyAction.Backspace, weight = 1.5f, isSpecial = true)
    )

    val numbersBottomRow = listOf(
        KeyModel(KeyAction.SwitchToLetters, weight = 1.5f, isSpecial = true), // ABC
        KeyModel(KeyAction.Character("/")),
        KeyModel(KeyAction.Character(",")),
        KeyModel(KeyAction.Space, weight = 4f),
        KeyModel(KeyAction.Character(".")),
        KeyModel(KeyAction.Enter, weight = 1.8f, isSpecial = true)
    )
}
