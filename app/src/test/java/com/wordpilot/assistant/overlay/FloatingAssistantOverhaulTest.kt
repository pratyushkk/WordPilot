package com.wordpilot.assistant.overlay

import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.EditorSnapshot
import com.wordpilot.assistant.domain.models.TextOperationResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FloatingAssistantOverhaulTest {

    private lateinit var helper: SafeReplacementHelper

    @Before
    fun setUp() {
        helper = SafeReplacementHelper()
    }

    // =========================================================================
    // OVERLAY GEOMETRY TESTS
    // =========================================================================

    @Test
    fun testCollapsedSizeCalculation() {
        val density = 2.5f
        val bubbleSizeDp = 48f
        val collapsedPx = OverlayGeometry.getCollapsedSizePx(density, bubbleSizeDp)
        // (48 + 8 * 2) * 2.5 = 64 * 2.5 = 160 px
        assertEquals(160, collapsedPx)
    }

    @Test
    fun testExpandedWidthCalculationCappedByScreen() {
        val density = 3.0f
        val screenWidthPx = 1080 // 360 dp
        val requestedToolbarWidthDp = 400f // Larger than screen

        val expandedWidthPx = OverlayGeometry.getExpandedWidthPx(density, screenWidthPx, requestedToolbarWidthDp)
        val expandedWidthDp = expandedWidthPx / density

        // Max allowed width is screenWidthDp - 24 = 336 dp + 16 dp padding = 352 dp
        assertTrue(expandedWidthDp <= (screenWidthPx / density))
    }

    @Test
    fun testCalculateEdgeAnchoredXNearRightEdge() {
        val density = 2.0f
        val screenWidthPx = 800
        val bubbleRestingX = 700
        val collapsedWidthPx = 100
        val targetWidthPx = 300

        val anchoredX = OverlayGeometry.calculateEdgeAnchoredX(
            isNearRightEdge = true,
            bubbleRestingX = bubbleRestingX,
            collapsedWidthPx = collapsedWidthPx,
            targetWidthPx = targetWidthPx,
            screenWidthPx = screenWidthPx,
            density = density
        )

        // bubbleRightEdge = 700 + 100 = 800
        // targetX = 800 - 300 = 500
        assertEquals(500, anchoredX)
    }

    @Test
    fun testCalculateEdgeAnchoredXNearLeftEdge() {
        val density = 2.0f
        val screenWidthPx = 800
        val bubbleRestingX = 20
        val collapsedWidthPx = 100
        val targetWidthPx = 300

        val anchoredX = OverlayGeometry.calculateEdgeAnchoredX(
            isNearRightEdge = false,
            bubbleRestingX = bubbleRestingX,
            collapsedWidthPx = collapsedWidthPx,
            targetWidthPx = targetWidthPx,
            screenWidthPx = screenWidthPx,
            density = density
        )

        assertEquals(20, anchoredX)
    }

    @Test
    fun testClampYKeepsPanelWithinScreenMargins() {
        val density = 2.0f
        val screenHeightPx = 1600
        val panelHeightPx = 600

        // Attempting to place too close to top
        val clampedTop = OverlayGeometry.clampY(0, panelHeightPx, screenHeightPx, density)
        val minTopPx = (OverlayGeometry.TOP_BAR_SAFETY_MARGIN_DP * density).toInt()
        assertEquals(minTopPx, clampedTop)

        // Attempting to place too close to bottom
        val clampedBottom = OverlayGeometry.clampY(1500, panelHeightPx, screenHeightPx, density)
        val maxAllowedY = screenHeightPx - panelHeightPx - (OverlayGeometry.BOTTOM_BAR_SAFETY_MARGIN_DP * density).toInt()
        assertEquals(maxAllowedY, clampedBottom)
    }

    // =========================================================================
    // EDITOR SNAPSHOT & SAFE REPLACEMENT TESTS
    // =========================================================================

    @Test
    fun testEditorSnapshotIntactVerification() {
        val text = "Hello world. This is a sentence. And more."
        val sentenceText = "This is a sentence."
        val start = text.indexOf(sentenceText)
        val end = start + sentenceText.length

        val snapshot = EditorSnapshot(
            packageName = "com.test.app",
            fieldIdentifier = "input_text",
            fullText = text,
            selectionStart = start + 5,
            selectionEnd = start + 5,
            sentenceStart = start,
            sentenceEnd = end,
            sentenceText = sentenceText
        )

        assertTrue(snapshot.isSentenceIntact(text))

        // Modify within sentence
        val modifiedText = "Hello world. This was a sentence. And more."
        assertFalse(snapshot.isSentenceIntact(modifiedText))

        // Truncate text
        val truncatedText = "Hello world."
        assertFalse(snapshot.isSentenceIntact(truncatedText))
    }

    @Test
    fun testReplaceSentenceWithSnapshotSuccess() {
        val text = "First line. Old erroneous sentence. Last line."
        val oldSentence = "Old erroneous sentence."
        val newSentence = "Polished sentence with proper words."
        val start = text.indexOf(oldSentence)
        val end = start + oldSentence.length

        val snapshot = EditorSnapshot(
            packageName = "com.test.app",
            fieldIdentifier = "editor",
            fullText = text,
            selectionStart = start,
            selectionEnd = end,
            sentenceStart = start,
            sentenceEnd = end,
            sentenceText = oldSentence
        )

        val replaced = helper.replaceSentenceWithSnapshot(text, snapshot, newSentence)
        assertNotNull(replaced)
        assertEquals("First line. Polished sentence with proper words. Last line.", replaced)
    }

    @Test
    fun testReplaceSentenceWithSnapshotRejectsStaleText() {
        val originalText = "Original sentence before change."
        val snapshot = EditorSnapshot(
            packageName = "com.test.app",
            fieldIdentifier = "editor",
            fullText = originalText,
            selectionStart = 0,
            selectionEnd = originalText.length,
            sentenceStart = 0,
            sentenceEnd = originalText.length,
            sentenceText = originalText
        )

        val userModifiedText = "Original sentence was changed by user."
        val replaced = helper.replaceSentenceWithSnapshot(userModifiedText, snapshot, "Rewritten sentence.")
        assertNull("Should reject replacement when editor snapshot is stale", replaced)
    }

    // =========================================================================
    // INSERT AT CURSOR NON-DESTRUCTIVE TESTS
    // =========================================================================

    @Test
    fun testInsertAtCursorMiddleWithAutomaticSpacing() {
        val currentText = "Hello world"
        val cursor = 5 // Right after "Hello"
        val textToInsert = "wonderful"

        val (newText, newCursor) = helper.insertAtCursor(currentText, textToInsert, cursor, cursor)
        assertEquals("Hello wonderful world", newText)
        assertEquals("Hello wonderful".length, newCursor)
    }

    @Test
    fun testInsertAtCursorEndWithoutDoubleSpacing() {
        val currentText = "Here is my thought "
        val cursor = currentText.length
        val textToInsert = "and conclusion."

        val (newText, newCursor) = helper.insertAtCursor(currentText, textToInsert, cursor, cursor)
        assertEquals("Here is my thought and conclusion.", newText)
        assertEquals(newText.length, newCursor)
    }

    @Test
    fun testInsertAtCursorReplacingSelection() {
        val currentText = "This is bad text here"
        val selStart = currentText.indexOf("bad")
        val selEnd = selStart + "bad".length
        val textToInsert = "excellent"

        val (newText, newCursor) = helper.insertAtCursor(currentText, textToInsert, selStart, selEnd)
        assertEquals("This is excellent text here", newText)
        assertEquals("This is excellent".length, newCursor)
    }

    // =========================================================================
    // TEXT OPERATION RESULT TESTS
    // =========================================================================

    @Test
    fun testTextOperationResultHandling() {
        val results: List<TextOperationResult> = listOf(
            TextOperationResult.Success,
            TextOperationResult.StaleSnapshot,
            TextOperationResult.EditorModified,
            TextOperationResult.UnsupportedEditor,
            TextOperationResult.Failure("Custom error")
        )

        val successCount = results.count { it is TextOperationResult.Success }
        assertEquals(1, successCount)

        val failure = results.filterIsInstance<TextOperationResult.Failure>().first()
        assertEquals("Custom error", failure.reason)
    }

    // =========================================================================
    // GECTOR & BLOCK CLOUD AI TESTS
    // =========================================================================

    @Test
    fun testAIEngineTypeGectorRegistration() {
        assertEquals(com.wordpilot.assistant.domain.models.AIEngineType.GECTOR, com.wordpilot.assistant.domain.models.AIEngineType.fromId("gector"))
        assertEquals("GECToR", com.wordpilot.assistant.domain.models.AIEngineType.GECTOR.shortName)
    }

    @Test
    fun testRewriteCoordinatorBlocksCloudAiWhenPolicyEnabled() = kotlinx.coroutines.runBlocking {
        val geminiEngine = com.wordpilot.assistant.engine.impl.GeminiAIEngine { "test_key" }
        val dummyRepo = com.wordpilot.assistant.data.repository.ModelRepositoryImpl(java.io.File("dummy"))
        val localEngine = com.wordpilot.assistant.engine.impl.LocalAIEngineImpl(dummyRepo)
        val spellEngine = object : com.wordpilot.assistant.engine.SpellEngine {
            override suspend fun check(text: String) = emptyList<com.wordpilot.assistant.domain.models.Correction>()
            override fun getSuggestions(word: String, maxResults: Int) = emptyList<String>()
            override fun addWord(word: String) {}
            override fun removeWord(word: String) {}
            override fun isKnownWord(word: String) = true
            override fun isAvailable() = true
        }
        val grammarEngine = object : com.wordpilot.assistant.engine.GrammarEngine {
            override suspend fun check(text: String) = emptyList<com.wordpilot.assistant.domain.models.Correction>()
            override fun isAvailable() = true
            override fun getSupportedRuleIds() = emptyList<String>()
            override fun setDisabledRules(ruleIds: Set<String>) {}
        }
        val corrCoordinator = com.wordpilot.assistant.engine.CorrectionCoordinator(spellEngine, grammarEngine)
        val ruleEngine = com.wordpilot.assistant.engine.impl.RuleBasedRewriteEngine(corrCoordinator, helper)

        val coordinator = com.wordpilot.assistant.engine.RewriteCoordinator(
            geminiEngine = geminiEngine,
            localEngine = localEngine,
            ruleEngine = ruleEngine,
            initialEngineType = com.wordpilot.assistant.domain.models.AIEngineType.GEMINI_FLASH
        )

        assertEquals(com.wordpilot.assistant.domain.models.AIEngineType.GEMINI_FLASH, coordinator.selectedEngine.value)

        // Block Cloud AI
        coordinator.setBlockCloudAi(true)
        assertTrue(coordinator.isBlockCloudAi)

        // Verifies selected engine automatically switches to GECTOR
        assertEquals(com.wordpilot.assistant.domain.models.AIEngineType.GECTOR, coordinator.selectedEngine.value)

        // Attempting to select GEMINI_FLASH while blocked is rejected
        coordinator.setEngineType(com.wordpilot.assistant.domain.models.AIEngineType.GEMINI_FLASH)
        assertEquals(com.wordpilot.assistant.domain.models.AIEngineType.GECTOR, coordinator.selectedEngine.value)

        // Verification of rewrite interception: does not call Gemini
        val result = coordinator.rewrite("This is a clean sentence.", com.wordpilot.assistant.domain.models.RewriteStyle.REWRITE, forcedEngine = com.wordpilot.assistant.domain.models.AIEngineType.GEMINI_FLASH)
        assertTrue(result.isSuccess)
    }
}
