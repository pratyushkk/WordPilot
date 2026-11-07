package com.wordpilot.assistant.ui

import com.wordpilot.assistant.domain.models.RewriteStyle
import com.wordpilot.assistant.ui.floating.ALL_TONE_DEFINITIONS
import com.wordpilot.assistant.ui.settings.AiWindowSettings
import org.junit.Assert.*
import org.junit.Test

class ToneConfigurationTest {

    @Test
    fun testDefaultTones_areStrictlyTheFourUserRequestedTones() {
        val defaults = AiWindowSettings.DEFAULT_TONE_STYLES
        assertEquals(4, defaults.size)
        assertEquals("FIX_GRAMMAR", defaults[0])
        assertEquals("MAKE_CONCISE", defaults[1])
        assertEquals("REWRITE", defaults[2])
        assertEquals("MAKE_POLITE", defaults[3])
    }

    @Test
    fun testAllToneDefinitions_containsAllEightOptionsWithTitles() {
        assertEquals(8, ALL_TONE_DEFINITIONS.size)

        val idMap = ALL_TONE_DEFINITIONS.associateBy { it.id }

        // Top 4 default options
        assertTrue(idMap.containsKey("FIX_GRAMMAR"))
        assertEquals("Polish", idMap["FIX_GRAMMAR"]?.title)
        assertEquals(RewriteStyle.FIX_GRAMMAR, idMap["FIX_GRAMMAR"]?.style)

        assertTrue(idMap.containsKey("MAKE_CONCISE"))
        assertEquals("Short", idMap["MAKE_CONCISE"]?.title)
        assertEquals(RewriteStyle.MAKE_CONCISE, idMap["MAKE_CONCISE"]?.style)

        assertTrue(idMap.containsKey("REWRITE"))
        assertEquals("Rewrite", idMap["REWRITE"]?.title)
        assertEquals(RewriteStyle.REWRITE, idMap["REWRITE"]?.style)

        assertTrue(idMap.containsKey("MAKE_POLITE"))
        assertEquals("Polite", idMap["MAKE_POLITE"]?.title)
        assertEquals(RewriteStyle.MAKE_POLITE, idMap["MAKE_POLITE"]?.style)

        // Additional selectable options in settings
        assertTrue(idMap.containsKey("MAKE_PROFESSIONAL"))
        assertEquals("Formal", idMap["MAKE_PROFESSIONAL"]?.title)

        assertTrue(idMap.containsKey("MAKE_CASUAL"))
        assertEquals("Casual", idMap["MAKE_CASUAL"]?.title)

        assertTrue(idMap.containsKey("IMPROVE_CLARITY"))
        assertEquals("Clarity", idMap["IMPROVE_CLARITY"]?.title)

        assertTrue(idMap.containsKey("IMPROVE_NATURAL"))
        assertEquals("Natural", idMap["IMPROVE_NATURAL"]?.title)
    }

    @Test
    fun testFilteringBySettings_producesExactMatchingTones() {
        val defaultTones = ALL_TONE_DEFINITIONS.filter { it.id in AiWindowSettings.DEFAULT_TONE_STYLES }
        assertEquals(4, defaultTones.size)
        val titles = defaultTones.map { it.title }
        assertEquals(listOf("Polish", "Short", "Rewrite", "Polite"), titles)

        // When user enables additional tone from settings (e.g. Formal and Clarity)
        val customSelection = listOf("FIX_GRAMMAR", "MAKE_CONCISE", "MAKE_PROFESSIONAL", "IMPROVE_CLARITY")
        val customTones = ALL_TONE_DEFINITIONS.filter { it.id in customSelection }
        assertEquals(4, customTones.size)
        assertTrue(customTones.any { it.title == "Formal" })
        assertTrue(customTones.any { it.title == "Clarity" })
    }
}
