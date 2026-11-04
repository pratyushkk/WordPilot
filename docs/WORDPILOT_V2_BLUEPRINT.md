# WordPilot V2.0 Master Blueprint & Implementation Specification

> **Target Version:** WordPilot v2.0-PRO  
> **Platforms:** Android 8.0+ (API 26–34) & Web Simulator (Next.js 14)  
> **Primary AI Engine:** Google Gemini 1.5 Flash (`generativeai:0.9.0`)  
> **Failover Engines:** Sapling AI → LanguageTool Public/Self-hosted → Offline Rule Engine  
> **Target Package:** `com.wordpilot.assistant`  
> **Status:** Production Architecture Blueprint for Immediate AI/Developer Handoff  

---

## 1. Executive Summary & V2 Objectives

WordPilot V1.0 delivered a production-grade grammar assistant featuring 30% edge-tucking, direct error-count badges, 1-tap correction replacement, privacy banking shields, and dual-provider failover.

**WordPilot V2.0** elevates the app from a grammar checker into a **full-suite generative writing companion** operating system-wide across third-party apps (WhatsApp, Instagram, Gmail, Slack, Twitter/X, Notes).

```
                     ┌────────────────────────────────────────┐
                     │     WordPilotAccessibilityService      │
                     │  (Reads text fields, cursor, context)  │
                     └───────────────────┬────────────────────┘
                                         │
                                         ▼
                     ┌────────────────────────────────────────┐
                     │      FloatingAssistantController       │
                     │       (State Machine & Brain)          │
                     └──────┬────────────┬────────────┬───────┘
                            │            │            │
            ┌───────────────┘            │            └───────────────┐
            ▼                            ▼                            ▼
  ┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
  │   FloatingView   │         │ Rewrite/AIEngine │         │ AiWindowSettings │
  │ (Compose Overlay)│         │ (Gemini 1.5 Pro) │         │ (Studio Config)  │
  └──────────────────┘         └──────────────────┘         └──────────────────┘
```

### Core V2 Additions
1. **Full Gemini 1.5 Flash AI Wiring**: Remove stubs for `executeRewrite()`, `onCustomPromptSubmit()`, and `onToneSelected()`.
2. **5 New AI Modalities**:
   - **AI Chat / Q&A Window**: Floating assistant conversation panel above any app.
   - **Summarize Selection**: Bullet-point and TL;DR summarization of selected text.
   - **Translate Selection**: 10-language instant translation with 1-tap insert.
   - **Explain & Define**: Contextual clarity, jargon definition, and grammar error rationale.
   - **Smart Reply**: 3 contextual 1-tap quick replies based on incoming chat messages.
3. **Dedicated AI Window Customization Studio**:
   - Distinct from the bubble appearance studio.
   - Configures AI window dimensions, glassmorphism blur/opacity, screen docking position, quick-action chips order, streaming typewriter speed, and keyboard auto-resize.
4. **WindowManager Overlay Focus Hardening**:
   - Dynamically toggles `FLAG_NOT_FOCUSABLE` so users can type inside the AI Chat and Custom Prompt fields without dismissing keyboard or freezing the background app.
5. **Web Simulator V2 Parity**:
   - Updates the Instagram DM simulator (`Tanya` ↔ `Jamie @ja.baxt.er`) with V2 features (AI Chat drawer, tone picker, and AI Window studio).

---

## 2. Architecture & Floating UI State Machine

### 2.1 Extended State Enum: `FloatingUiState`

**File:** `app/src/main/java/com/wordpilot/assistant/ui/floating/FloatingAssistantView.kt`

```kotlin
package com.wordpilot.assistant.ui.floating

enum class FloatingUiState {
    COLLAPSED,          // Minimized bubble (tucked 30% into edge)
    EXPANDED,           // Horizontal morphing pill with error chips & AI logo
    MORE_OPTIONS,       // Grid of AI features (Rewrite, Chat, Summarize, etc.)
    PREVIEW,            // Diff view showing Original vs Improved + Replace/Insert/Copy
    AI_CHAT,            // Floating conversational chat window with Gemini
    SUMMARIZE_RESULT,   // Clean summary card with Copy/Replace buttons
    SMART_REPLY,        // 3 contextual quick-reply chips
    TRANSLATE_RESULT,   // Translated text card with language selector
    EXPLAIN_RESULT,     // Jargon/grammar explanation modal
    CUSTOM_PROMPT_INPUT // Full input field for user's ad-hoc prompt
}
```

### 2.2 WindowManager Overlay Focus & Resizing Protocol

**File:** `app/src/main/java/com/wordpilot/assistant/overlay/FloatingAssistantController.kt`

A major overlay pitfall in Android is that text input inside an accessibility overlay requires removing `FLAG_NOT_FOCUSABLE`. If `FLAG_NOT_FOCUSABLE` is cleared while collapsed, the user cannot touch or type into background apps.

#### The Dynamic Focus Matrix:
| State | `FLAG_NOT_FOCUSABLE` | Window Width | Window Height | Soft Input Mode |
|---|---|---|---|---|
| `COLLAPSED` | **Present** (Non-focusable) | Bubble Size (48dp) | Bubble Size (48dp) | `SOFT_INPUT_ADJUST_NOTHING` |
| `EXPANDED` | **Present** (Non-focusable) | Toolbar Width (320dp) | Toolbar Height (50dp)| `SOFT_INPUT_ADJUST_NOTHING` |
| `PREVIEW` | **Present** (Non-focusable) | Wrap Content (max 340dp)| Wrap Content | `SOFT_INPUT_ADJUST_NOTHING` |
| `MORE_OPTIONS` | **Present** (Non-focusable) | Panel Width (340dp) | Wrap Content (max 380dp)| `SOFT_INPUT_ADJUST_NOTHING` |
| `AI_CHAT` | **REMOVED** (Focusable) | Match Parent / 92% Screen | 420dp–520dp | `SOFT_INPUT_ADJUST_RESIZE` |
| `CUSTOM_PROMPT`| **REMOVED** (Focusable) | Panel Width (340dp) | 220dp | `SOFT_INPUT_ADJUST_RESIZE` |

#### Controller Focus Switcher:
```kotlin
fun updateWindowLayoutForState(newState: FloatingUiState) {
    val lp = windowLayoutParams ?: return
    val view = overlayView ?: return
    val density = context.resources.displayMetrics.density
    val s = appearanceSettings.value
    val aiWin = aiWindowSettings.value

    when (newState) {
        FloatingUiState.COLLAPSED -> {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            lp.width = (s.bubbleSizeDp * density).toInt()
            lp.height = (s.bubbleSizeDp * density).toInt()
        }
        FloatingUiState.EXPANDED -> {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            lp.width = (s.toolbarMaxWidthDp * density).toInt()
            lp.height = (s.toolbarHeightDp * density).toInt()
        }
        FloatingUiState.AI_CHAT, FloatingUiState.CUSTOM_PROMPT_INPUT -> {
            // Must remove FLAG_NOT_FOCUSABLE so Android software keyboard can pop up
            lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            lp.width = (aiWin.windowWidthDp * density).toInt()
            lp.height = (aiWin.windowHeightDp * density).toInt()
        }
        FloatingUiState.MORE_OPTIONS, FloatingUiState.PREVIEW, 
        FloatingUiState.SUMMARIZE_RESULT, FloatingUiState.SMART_REPLY,
        FloatingUiState.TRANSLATE_RESULT, FloatingUiState.EXPLAIN_RESULT -> {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            lp.width = (aiWin.windowWidthDp * density).toInt()
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
    }
    windowManager.updateViewLayout(view, lp)
}
```

---

## 3. Dedicated AI Window Customization Studio

To fulfill the user requirement for a **separate AI window customization feature**, create a dedicated configuration model and studio screen.

### 3.1 Data Model: `AiWindowSettings`

**New File:** `app/src/main/java/com/wordpilot/assistant/ui/settings/AiWindowSettings.kt`

```kotlin
package com.wordpilot.assistant.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.wordpilot.assistant.domain.models.RewriteStyle

enum class WindowAnchorPosition(val displayName: String) {
    SNAP_TO_BUBBLE("Follow Floating Bubble"),
    ABOVE_KEYBOARD("Anchor Above Keyboard"),
    CENTER_DIALOG("Center Screen Modal"),
    BOTTOM_SHEET("Bottom Sheet Dock")
}

enum class WindowGlassPreset(val displayName: String, val opacity: Float, val blurDp: Float) {
    FROSTED_GLASS("Frosted Glass (Default)", 0.88f, 18f),
    DEEP_OBSIDIAN("Deep Obsidian", 0.96f, 24f),
    TRANSLUCENT("Subtle Minimal", 0.72f, 12f),
    PURE_OLED("Pitch Black OLED", 1.0f, 0f)
}

enum class StreamingSpeed(val displayName: String, val delayMs: Long) {
    INSTANT("Instant (No animation)", 0L),
    TYPEWRITER_FAST("Fast Typewriter (15ms)", 15L),
    TYPEWRITER_NATURAL("Natural Pace (30ms)", 30L)
}

data class AiWindowSettings(
    // Dimensions
    val windowWidthDp: Float = 340f,
    val windowHeightDp: Float = 460f,
    val cornerRadiusDp: Float = 26f,
    val borderWidthDp: Float = 1.2f,

    // Glass & Styling
    val glassPreset: WindowGlassPreset = WindowGlassPreset.FROSTED_GLASS,
    val backgroundTintArgb: Int = 0xFF12141A.toInt(),
    val accentBorderArgb: Int = 0xFF10B981.toInt(), // Emerald green
    val borderGlowIntensity: Float = 0.45f,
    val backgroundOpacity: Float = 0.90f,

    // Position & Behavior
    val anchorPosition: WindowAnchorPosition = WindowAnchorPosition.SNAP_TO_BUBBLE,
    val closeOnOutsideTouch: Boolean = true,
    val autoClearChatOnClose: Boolean = false,
    val streamingSpeed: StreamingSpeed = StreamingSpeed.TYPEWRITER_FAST,

    // Enabled Features Grid (User configurable)
    val showChatTab: Boolean = true,
    val showSummarizeTab: Boolean = true,
    val showSmartReplyTab: Boolean = true,
    val showTranslateTab: Boolean = true,
    val showExplainTab: Boolean = true,
    val enabledRewriteStyles: List<String> = listOf(
        "MAKE_PROFESSIONAL",
        "MAKE_CASUAL",
        "MAKE_CONCISE",
        "MAKE_ELABORATE",
        "FIX_GRAMMAR",
        "IMPROVE_CLARITY"
    ),

    // Quick Tone Chips Order
    val defaultToneChips: List<String> = listOf(
        "Professional", "Casual", "Assertive", "Friendly", "Urgent", "Persuasive"
    ),

    // Custom Quick Prompts
    val customPrompts: List<String> = listOf(
        "Turn into bullet points",
        "Make this sound confident",
        "Check for passive voice",
        "Reply politely saying no"
    )
) {
    companion object {
        const val PREFS_NAME = "wordpilot_ai_window_prefs"

        fun load(context: Context): AiWindowSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return AiWindowSettings(
                windowWidthDp = prefs.getFloat("ai_win_width", 340f),
                windowHeightDp = prefs.getFloat("ai_win_height", 460f),
                cornerRadiusDp = prefs.getFloat("ai_win_radius", 26f),
                backgroundOpacity = prefs.getFloat("ai_win_opacity", 0.90f),
                anchorPosition = WindowAnchorPosition.valueOf(
                    prefs.getString("ai_win_anchor", WindowAnchorPosition.SNAP_TO_BUBBLE.name)!!
                ),
                glassPreset = WindowGlassPreset.valueOf(
                    prefs.getString("ai_win_preset", WindowGlassPreset.FROSTED_GLASS.name)!!
                ),
                streamingSpeed = StreamingSpeed.valueOf(
                    prefs.getString("ai_win_speed", StreamingSpeed.TYPEWRITER_FAST.name)!!
                ),
                showChatTab = prefs.getBoolean("ai_win_show_chat", true),
                showSummarizeTab = prefs.getBoolean("ai_win_show_sum", true),
                showSmartReplyTab = prefs.getBoolean("ai_win_show_smart", true),
                showTranslateTab = prefs.getBoolean("ai_win_show_trans", true),
                showExplainTab = prefs.getBoolean("ai_win_show_exp", true)
            )
        }

        fun save(context: Context, settings: AiWindowSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putFloat("ai_win_width", settings.windowWidthDp)
                putFloat("ai_win_height", settings.windowHeightDp)
                putFloat("ai_win_radius", settings.cornerRadiusDp)
                putFloat("ai_win_opacity", settings.backgroundOpacity)
                putString("ai_win_anchor", settings.anchorPosition.name)
                putString("ai_win_preset", settings.glassPreset.name)
                putString("ai_win_speed", settings.streamingSpeed.name)
                putBoolean("ai_win_show_chat", settings.showChatTab)
                putBoolean("ai_win_show_sum", settings.showSummarizeTab)
                putBoolean("ai_win_show_smart", settings.showSmartReplyTab)
                putBoolean("ai_win_show_trans", settings.showTranslateTab)
                putBoolean("ai_win_show_exp", settings.showExplainTab)
                apply()
            }
        }
    }
}
```

### 3.2 UI Screen: `AiWindowCustomizationScreen.kt`

**New File:** `app/src/main/java/com/wordpilot/assistant/ui/settings/AiWindowCustomizationScreen.kt`

This screen allows the user to:
1. **Interactive Live Preview**: Shows an active miniature mockup of the AI Window that reacts instantly as sliders/toggles change.
2. **Dimension Sliders**:
   - Width: `280dp` to `380dp`
   - Height: `360dp` to `560dp`
   - Corner Radius: `12dp` to `36dp`
3. **Glassmorphism Selector**: Frosted Glass, Deep Obsidian, Translucent, Pure OLED.
4. **Anchor Position Radios**: Follow Floating Bubble, Above Keyboard, Center Dialog.
5. **Feature Matrix Toggles**: Enable/disable Chat, Summarize, Smart Reply, Translate, Explain.
6. **Custom Prompts Editor**: Add new custom prompt chips and reorder existing prompts.
7. **Reset to Defaults Button**.

---

## 4. Full AI Engine Implementation (`GeminiAIEngine.kt`)

**File:** `app/src/main/java/com/wordpilot/assistant/engine/impl/GeminiAIEngine.kt`

### 4.1 Chat / Q&A Streaming Implementation
```kotlin
data class ChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

suspend fun chatStream(
    message: String,
    history: List<ChatMessage>,
    activeContextText: String?,
    onChunkReceived: (String) -> Unit,
    onComplete: (String) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    val apiKey = apiKeyProvider()?.trim()
    if (apiKey.isNullOrBlank()) {
        onError("Gemini API key is not configured. Please add it in Settings.")
        return@withContext
    }

    try {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent?alt=sse&key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
            doOutput = true
            connectTimeout = 12000
            readTimeout = 30000
        }

        // Build Multi-turn Chat Payload
        val contentsArray = JSONArray()
        
        // System instruction context injection
        val systemInstruction = "You are WordPilot, an elite floating AI writing assistant on Android. " +
            "The user is currently typing in an app. Context of active text: '${activeContextText ?: ""}'. " +
            "Be direct, concise, insightful, and helpful. Format in markdown."

        for (msg in history.takeLast(6)) {
            val role = if (msg.role == "user") "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
            })
        }
        // Current user message
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", message)))
        })

        val requestBody = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 1024)
            })
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(requestBody.toString()) }

        val fullResponse = StringBuilder()
        BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("data: ")) {
                    val jsonStr = line!!.substring(6).trim()
                    if (jsonStr.isNotEmpty()) {
                        try {
                            val candidate = JSONObject(jsonStr)
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                            fullResponse.append(candidate)
                            withContext(Dispatchers.Main) {
                                onChunkReceived(candidate)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        withContext(Dispatchers.Main) {
            onComplete(fullResponse.toString())
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError("Gemini Chat failed: ${e.localizedMessage}")
        }
    }
}
```

### 4.2 Summarization Implementation
```kotlin
suspend fun summarize(
    text: String,
    mode: String = "BULLETS" // "BULLETS" or "KEY_TAKEAWAYS" or "ONE_SENTENCE"
): Result<String> = withContext(Dispatchers.IO) {
    val prompt = when (mode) {
        "ONE_SENTENCE" -> "Summarize the following text in exactly one clear, impactful sentence:\n\n$text"
        "KEY_TAKEAWAYS" -> "Extract the top 3 key takeaways from this text with bullet points:\n\n$text"
        else -> "Provide a concise bulleted summary of the following text, highlighting core facts:\n\n$text"
    }
    executeDirectPrompt(prompt, temperature = 0.2f)
}
```

### 4.3 Smart Reply Suggestions
```kotlin
suspend fun generateSmartReplies(
    incomingContext: String
): Result<List<String>> = withContext(Dispatchers.IO) {
    val prompt = """
    The user received the following message in a chat app:
    "$incomingContext"

    Generate exactly 3 smart, natural, contextual replies:
    1: Short agreement/affirmative
    2: Friendly elaboration or follow-up question
    3: Polite decline or alternative proposal

    Output ONLY a valid JSON array of 3 strings, e.g. ["Yes, sounds great!", "I'd love to, what time?", "Sorry, I'm tied up today."]. Do not include markdown or extra text.
    """.trimIndent()

    val rawResult = executeDirectPrompt(prompt, temperature = 0.4f)
    rawResult.map { jsonString ->
        try {
            val cleaned = jsonString.replace("```json", "").replace("```", "").trim()
            val arr = JSONArray(cleaned)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list
        } catch (e: Exception) {
            listOf("Sounds good!", "Thanks for letting me know.", "Let me get back to you soon.")
        }
    }
}
```

### 4.4 Translation Implementation
```kotlin
suspend fun translate(
    text: String,
    targetLanguage: String
): Result<String> = withContext(Dispatchers.IO) {
    val prompt = "Translate the following text accurately into $targetLanguage. Output ONLY the translated text without commentary:\n\n$text"
    executeDirectPrompt(prompt, temperature = 0.1f)
}
```

### 4.5 Explain & Define Implementation
```kotlin
suspend fun explain(
    text: String
): Result<String> = withContext(Dispatchers.IO) {
    val prompt = "Explain the meaning, context, and key terms in the following text simply and clearly in 2-3 sentences:\n\n$text"
    executeDirectPrompt(prompt, temperature = 0.3f)
}
```

---

## 5. UI Panels & Compose Hierarchy

### 5.1 Main Overlay Hierarchy
```
FloatingAssistantView (ComposeView root)
 │
 ├── State: COLLAPSED
 │    └── FloatingBubble (minimized circle, error badge, 30% edge tuck)
 │
 ├── State: EXPANDED
 │    └── ExpandedToolbar (AI logo + horizontal scrollable error chips)
 │
 ├── State: MORE_OPTIONS
 │    └── MoreOptionsPanelV2 (grid of 9 rewrite styles, Chat, Summarize, Smart Reply buttons)
 │
 ├── State: PREVIEW
 │    └── SuggestionPreview (Original red diff vs Improved green diff + Replace/Insert/Copy)
 │
 ├── State: AI_CHAT (NEW)
 │    └── FloatingAiChatPanel (conversation scroll, streaming bubble, input box, send button)
 │
 ├── State: SUMMARIZE_RESULT (NEW)
 │    └── SummarizeResultPanel (mode selector, bullet points list, 1-tap Replace/Insert/Copy)
 │
 ├── State: SMART_REPLY (NEW)
 │    └── SmartReplyPanel (3 pill buttons that immediately type into the active field)
 │
 ├── State: TRANSLATE_RESULT (NEW)
 │    └── TranslateResultPanel (language picker dropdown, translated text preview, 1-tap Replace)
 │
 └── State: CUSTOM_PROMPT_INPUT (NEW)
      └── CustomPromptPanel (text area for custom user prompt + preset suggestion chips)
```

### 5.2 `FloatingAiChatPanel.kt` (New Compose Component)

**File:** `app/src/main/java/com/wordpilot/assistant/ui/floating/FloatingAiChatPanel.kt`

Features:
- Glassmorphic header with Close, Clear Chat, and Minimize buttons.
- Chat message bubble list:
  - User message: Right-aligned blue/indigo gradient bubble.
  - Gemini response: Left-aligned dark glass bubble with Markdown rendering + copy icon.
- Typing animation with 3 bouncing dots while waiting for first chunk.
- Text input box with Send button. When focused, `FloatingAssistantController` clears `FLAG_NOT_FOCUSABLE`.
- 1-Tap **"Insert Answer into Text Field"** button on each AI response.

### 5.3 `MoreOptionsPanelV2.kt` (Enhanced Grid)

**File:** `app/src/main/java/com/wordpilot/assistant/ui/floating/MoreOptionsPanel.kt`

Replaces the current placeholder (which only has 2 chips) with a rich categorized grid:
1. **Section 1: AI Tools**:
   - `AI Chat` (Launches `FloatingUiState.AI_CHAT`)
   - `Summarize` (Launches `FloatingUiState.SUMMARIZE_RESULT`)
   - `Smart Reply` (Launches `FloatingUiState.SMART_REPLY`)
   - `Translate` (Launches `FloatingUiState.TRANSLATE_RESULT`)
   - `Explain` (Launches `FloatingUiState.EXPLAIN_RESULT`)
2. **Section 2: Rewrite Tones & Styles (9 Chips)**:
   - Professional, Casual, Concise, Elaborate, Fix Grammar, Improve Clarity, Friendly, Assertive, Simplify.
3. **Section 3: Custom Instruction Input**:
   - Quick text field with "Submit" button triggering `onCustomPromptSubmit(prompt)`.

---

## 6. Android Backend Hardening & Bug Fixes

### 6.1 Fixing Unwired Controller Stubs
Update `FloatingAssistantController.kt`:
```kotlin
fun executeRewrite(style: RewriteStyle) {
    val targetText = getTargetText()
    if (targetText.isBlank()) {
        Toast.makeText(context, "No text detected to rewrite", Toast.LENGTH_SHORT).show()
        return
    }

    _previewOriginalText.value = targetText
    _previewImprovedText.value = ""
    _previewErrorMessage.value = null
    _isPreviewLoading.value = true
    setUiState(FloatingUiState.PREVIEW)

    controllerScope.launch {
        try {
            val result = WordPilotApp.instance.rewriteCoordinator.rewrite(
                text = targetText,
                style = style
            )
            if (result.isSuccess) {
                _previewImprovedText.value = result.rewrittenText
            } else {
                _previewErrorMessage.value = result.errorMessage ?: "Rewrite failed"
            }
        } catch (e: Exception) {
            _previewErrorMessage.value = "Error: ${e.localizedMessage}"
        } finally {
            _isPreviewLoading.value = false
        }
    }
}

fun onCustomPromptSubmit(prompt: String) {
    val targetText = getTargetText()
    if (targetText.isBlank()) return

    _previewOriginalText.value = targetText
    _previewImprovedText.value = ""
    _isPreviewLoading.value = true
    setUiState(FloatingUiState.PREVIEW)

    controllerScope.launch {
        val result = WordPilotApp.instance.rewriteCoordinator.rewrite(
            text = targetText,
            style = RewriteStyle.IMPROVE_CLARITY,
            customInstruction = prompt
        )
        _previewImprovedText.value = result.rewrittenText
        _isPreviewLoading.value = false
    }
}

fun onToneSelected(tone: String) {
    val style = when (tone.lowercase()) {
        "professional" -> RewriteStyle.MAKE_PROFESSIONAL
        "casual" -> RewriteStyle.MAKE_CASUAL
        "concise" -> RewriteStyle.MAKE_CONCISE
        "friendly" -> RewriteStyle.MAKE_FRIENDLY
        "assertive" -> RewriteStyle.MAKE_ASSERTIVE
        else -> RewriteStyle.IMPROVE_CLARITY
    }
    executeRewrite(style)
}
```

### 6.2 Safe Text Replacement Guarantee
Ensure `applyPreviewReplacement()` handles cursor offsets without causing index crashes:
- Validates `AccessibilityNodeInfo.isEditable`.
- Dispatches `ACTION_SET_TEXT` with `Bundle().apply { putCharSequence(ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText) }`.
- Sets cursor to end using `ACTION_SET_SELECTION`.

---

## 7. Web Simulator V2 Upgrades (`WordPilot-Web`)

The web simulator at `c:\Users\PRATYUSH\OneDrive\Desktop\WordPilot-Web` must match the Android app's V2 features:

1. **AI Chat Drawer in Instagram Simulator**:
   - A floating button on the WordPilot overlay that opens a sliding chat drawer.
   - Allows Tanya to ask Gemini questions, summarize Jamie's message, or generate 3 smart replies.
2. **AI Window Customization Tab**:
   - Add a 5th tab to `WordPilot-Web`: `AI Window Studio`.
   - Live controls for window size, glassmorphism presets, positioning, typewriter streaming speed, and custom prompt builder.
3. **Fix Existing Minor Issues**:
   - Clean up unused imports (`ExternalLink`).
   - Align timestamp header (`12:44 PM` matching Jamie's initial message).
   - Add user-friendly toast on API error instead of silent console warning.

---

## 8. Implementation Roadmap & AI Agent Handoff Tasks

Any AI agent or developer can pick up and execute these modular tasks in order:

```
[Phase 1: Foundation]
  Task 1.1: AiWindowSettings.kt & Preferences Storage
  Task 1.2: FloatingUiState enum expansion

[Phase 2: Backend AI Engine]
  Task 2.1: GeminiAIEngine chatStream, summarize, smartReply, translate methods
  Task 2.2: RewriteCoordinator & LocalAIEngine interface updates
  Task 2.3: Wire FloatingAssistantController stubs (executeRewrite, customPrompt, tone)

[Phase 3: Floating UI Panels]
  Task 3.1: MoreOptionsPanelV2 (Categorized Grid with 9 styles & AI tools)
  Task 3.2: FloatingAiChatPanel (Full chat overlay with soft-keyboard focus toggle)
  Task 3.3: SummarizeResultPanel & SmartReplyPanel
  Task 3.4: CustomPromptPanel & ToneSelector

[Phase 4: AI Window Studio UI]
  Task 4.1: AiWindowCustomizationScreen with interactive live preview
  Task 4.2: Navigation entry in SettingsActivity / NavGraph

[Phase 5: Web Simulator V2]
  Task 5.1: Web AI Chat Drawer & Smart Reply chips
  Task 5.2: Web AI Window Studio tab & bug fixes

[Phase 6: Verification & Release]
  Task 6.1: Gradle assembleRelease & APK verification
  Task 6.2: Web npm run build verification
  Task 6.3: Push to GitHub & deploy WordPilot v2.0
```

---

## 9. Verification & Quality Acceptance Criteria

1. **Android Compilation**: `gradlew assembleDebug` and `gradlew assembleRelease` pass with zero errors.
2. **Keyboard Focus Test**: Tapping text input inside `FloatingAiChatPanel` summons the keyboard without crashing or dismissing the overlay.
3. **Streaming Text Test**: Gemini responses stream smoothly into the UI according to the configured `StreamingSpeed`.
4. **1-Tap Text Injection**: Tapping "Replace" or "Insert" in `SuggestionPreview` or `SmartReplyPanel` writes directly into the background app's active text field.
5. **No Broken Stubs**: Zero `executeFixGrammar()` fallbacks when tapping rewrite styles or custom prompts.
6. **Web Simulator Integrity**: `npm run build` succeeds with zero lint or TypeScript errors.
