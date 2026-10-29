# WordPilot — System-Wide Android Writing Assistant

WordPilot is a production-oriented, local-first Android writing assistant providing a Grammarly-style writing experience across third-party applications.

## Key Features

1. **System-Wide Floating Grammar Assistant**
   - Renders a compact correction card directly above the keyboard layout without requiring invasive overlay permissions.
   - Circular color-coded error indicator (Spelling, Grammar, Punctuation, Capitalization, Style).
   - Prominent replacement text with 1-tap "Apply" action.
   - Non-intrusive dismiss button and expandable drawer for explanations, personal dictionary addition, and rule silencing.

2. **Full-Featured Custom Android Keyboard**
   - Built on `InputMethodService` using Jetpack Compose.
   - QWERTY, numeric, and symbol layouts.
   - Real-time spelling prediction and grammar analysis.
   - Safe text replacement via Android `InputConnection` with boundary and context verification.

3. **Offline Spelling & Grammar Engine**
   - **TrieSpellEngine**: Trie-based prefix and word dictionary (~10,000 core English words) with dynamic Levenshtein distance $\le 2$ suggestion generation.
   - **RuleBasedGrammarEngine**: Comprehensive modular rule engine covering:
     - Repeated consecutive words (`REPEATED_WORD`)
     - Subject-verb agreement (`SUBJECT_VERB_AGREEMENT`)
     - Indefinite article usage (`ARTICLE_A_AN`: "a" vs "an" before vowel sounds)
     - Commonly confused words (`CONFUSED_WORDS`: their/they're, your/you're, its/it's, then/than, would have)
     - Punctuation spacing & formatting (`PUNCTUATION_SPACING`: missing spaces after punctuation, extra spaces before punctuation, repeated punctuation)
     - Capitalization (`CAPITALIZATION`: sentence starts and the pronoun "I")
   - Zero internet requirement — 100% functional in airplane mode.

4. **Standalone Writing Editor**
   - Rich text editing canvas with word and character counters.
   - Undo/Redo stack (up to 50 states).
   - "Check Writing" inspection with categorized issues list.
   - Document save, rename, and local history management.

5. **Privacy & Sensitive Input Protection**
   - `SensitiveInputGuard` automatically disables analysis on:
     - Password fields (plain, web, visible)
     - Numeric PIN fields
     - OTP and two-factor authentication inputs
     - Inputs with `TYPE_TEXT_FLAG_NO_SUGGESTIONS`
   - Strict local-first architecture: no analytics, no network telemetry, no external API calls for grammar or spelling.

6. **Pluggable On-Device AI Architecture**
   - `LocalAIEngine` abstraction ready for on-device GGUF/ONNX small language models (0.5B–1.5B parameters).
   - Styles supported: Fix Grammar, Improve Clarity, Make Professional, Make Casual, Make Polite, Make Concise, Improve Naturalness, Custom prompt.

---

## Technical Specifications

- **Target Device**: Motorola Edge 70 Pro
- **Platform**: Android 16 (API 35), backward-compatible to Android 8.0 (API 26)
- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose + Material Design 3
- **Local Persistence**: Room Database 2.6.1 + DataStore Preferences
- **Build System**: Gradle 8.9 / AGP 8.5.2 / KSP 1.9.24-1.0.20

---

## Keyboard Setup Instructions

To enable WordPilot as your input method on your device:

1. Open **Settings** on your Android device.
2. Navigate to **System** → **Languages & input** → **On-screen keyboard** (or **Manage on-screen keyboards**).
3. Toggle on **WordPilot Keyboard**.
4. When typing in any messaging app, browser, or note app, tap the **keyboard switcher icon** on the navigation bar and select **WordPilot Keyboard**.

---

## License & Attribution

- Built exclusively with open-source, permissive dependencies (Apache 2.0 / MIT).
- Curated wordlist derived from public domain frequency lists.
