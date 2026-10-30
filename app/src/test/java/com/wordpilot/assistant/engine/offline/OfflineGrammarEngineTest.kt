package com.wordpilot.assistant.engine.offline

import com.wordpilot.assistant.domain.models.CorrectionCategory
import com.wordpilot.assistant.engine.offline.applier.CorrectionApplier
import com.wordpilot.assistant.engine.offline.dictionary.EnglishDictionary
import com.wordpilot.assistant.engine.offline.morphology.MorphologyAnalyzer
import com.wordpilot.assistant.engine.offline.normalizer.TextNormalizer
import com.wordpilot.assistant.engine.offline.rules.Rule
import com.wordpilot.assistant.engine.offline.segmenter.SentenceSegmenter
import com.wordpilot.assistant.engine.offline.spelling.SpellingChecker
import com.wordpilot.assistant.engine.offline.test.GrammarTestRunner
import com.wordpilot.assistant.engine.offline.tokenizer.Tokenizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OfflineGrammarEngineTest {

    private lateinit var facade: OfflineGrammarEngineFacade

    @Before
    fun setUp() {
        facade = OfflineGrammarEngineFacade(null)
    }

    // 1. TEXT NORMALIZER TESTS
    @Test
    fun testTextNormalizerCurlyQuotesAndApostrophes() {
        val normalizer = TextNormalizer()
        val raw = "“It’s a ‘wonderful’ day—indeed.”"
        val result = normalizer.normalize(raw)

        assertEquals("\"It's a 'wonderful' day-indeed.\"", result.normalizedText)
        assertEquals(0, result.toOriginalOffset(0))
        assertEquals(raw.length, result.toOriginalOffset(result.normalizedText.length))
    }

    // 2. SENTENCE SEGMENTER TESTS
    @Test
    fun testSentenceSegmenterWithAbbreviationsAndDecimals() {
        val segmenter = SentenceSegmenter()
        val text = "Dr. Smith bought 3.14 lbs of apples at 10 a.m. He was very happy. Did you see him?"
        val sentences = segmenter.segment(text)

        assertEquals(3, sentences.size)
        assertTrue(sentences[0].text.startsWith("Dr. Smith"))
        assertEquals("He was very happy.", sentences[1].text)
        assertEquals("Did you see him?", sentences[2].text)
    }

    // 3. TOKENIZER TESTS
    @Test
    fun testTokenizerContractionsAndPunctuation() {
        val tokenizer = Tokenizer()
        val text = "Don't touch the cat's tail, okay?"
        val tokens = tokenizer.tokenize(text)

        val words = tokens.filter { it.isWord }
        assertEquals(6, words.size)
        assertEquals("don't", words[0].cleanText)
        assertEquals("cat's", words[3].cleanText)
    }

    // 4. MORPHOLOGY TESTS
    @Test
    fun testMorphologyIrregularVerbsAndPlurals() {
        val morphology = MorphologyAnalyzer()

        assertEquals("went", morphology.getPastTense("go"))
        assertEquals("gone", morphology.getPastParticiple("go"))
        assertEquals("written", morphology.getPastParticiple("write"))
        assertEquals("barks", morphology.getThirdPersonSingularVerb("bark"))
        assertEquals("watches", morphology.getThirdPersonSingularVerb("watch"))

        assertEquals("children", morphology.getPluralNoun("child"))
        assertEquals("people", morphology.getPluralNoun("person"))
        assertEquals("child", morphology.getSingularNoun("children"))
        assertTrue(morphology.isUncountable("information"))
        assertTrue(morphology.isUncountable("advice"))
    }

    // 5. SPELLING CHECKER TESTS
    @Test
    fun testSpellingCommonTyposAndSplitWords() = runBlocking {
        val text = "I recieved alot of gifts yesterday."
        val corrections = facade.checkText(text)

        val typoCorrection = corrections.find { it.originalText == "recieved" }
        assertNotNull("Should detect 'recieved'", typoCorrection)
        assertEquals("received", typoCorrection?.suggestedReplacement)

        val splitCorrection = corrections.find { it.originalText == "alot" }
        assertNotNull("Should detect 'alot'", splitCorrection)
        assertEquals("a lot", splitCorrection?.suggestedReplacement)
    }

    // 6. SUBJECT-VERB AGREEMENT TESTS
    @Test
    fun testSubjectVerbAgreement() = runBlocking {
        val text1 = "She have two brothers."
        val corrections1 = facade.checkText(text1)
        val sv1 = corrections1.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should flag 'She have'", sv1)
        assertEquals("has", sv1?.suggestedReplacement)

        val text2 = "The dogs barks loudly."
        val corrections2 = facade.checkText(text2)
        val sv2 = corrections2.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should flag 'dogs barks'", sv2)
        assertEquals("bark", sv2?.suggestedReplacement)

        val text3 = "There is many people outside."
        val corrections3 = facade.checkText(text3)
        val sv3 = corrections3.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should flag 'There is many'", sv3)
        assertEquals("are", sv3?.suggestedReplacement)

        val text4 = "Everyone are ready to start."
        val corrections4 = facade.checkText(text4)
        val sv4 = corrections4.find { it.ruleId == "SUBJECT_VERB_AGREEMENT" }
        assertNotNull("Should flag 'Everyone are'", sv4)
        assertEquals("is", sv4?.suggestedReplacement)
    }

    // 7. ARTICLE TESTS
    @Test
    fun testArticleRules() = runBlocking {
        val text1 = "He waited for a hour."
        val corrections1 = facade.checkText(text1)
        val art1 = corrections1.find { it.ruleId == "ARTICLE_A_AN" }
        assertNotNull("Should flag 'a hour'", art1)
        assertEquals("an", art1?.suggestedReplacement)

        val text2 = "She wants an university degree."
        val corrections2 = facade.checkText(text2)
        val art2 = corrections2.find { it.ruleId == "ARTICLE_A_AN" }
        assertNotNull("Should flag 'an university'", art2)
        assertEquals("a", art2?.suggestedReplacement)
    }

    // 8. VERB FORMS TESTS
    @Test
    fun testVerbForms() = runBlocking {
        val text1 = "They have went to the station."
        val corrections1 = facade.checkText(text1)
        val vf1 = corrections1.find { it.ruleId == "VERB_FORM_MISUSE" }
        assertNotNull("Should flag 'have went'", vf1)
        assertEquals("gone", vf1?.suggestedReplacement)

        val text2 = "I didn't saw anything."
        val corrections2 = facade.checkText(text2)
        val vf2 = corrections2.find { it.ruleId == "VERB_FORM_MISUSE" }
        assertNotNull("Should flag 'didn't saw'", vf2)
        assertEquals("see", vf2?.suggestedReplacement)

        val text3 = "You must to go now."
        val corrections3 = facade.checkText(text3)
        val vf3 = corrections3.find { it.ruleId == "MODAL_INFINITIVE_TO" }
        assertNotNull("Should flag 'must to'", vf3)
    }

    // 9. PRONOUN TESTS
    @Test
    fun testPronounsAndContractions() = runBlocking {
        val text1 = "Him and I went home."
        val corrections1 = facade.checkText(text1)
        val pr1 = corrections1.find { it.ruleId == "PRONOUN_CASE_AND_CONTRACTION" }
        assertNotNull("Should flag 'Him and I'", pr1)
        assertEquals("He", pr1?.suggestedReplacement)

        val text2 = "This is between you and I."
        val corrections2 = facade.checkText(text2)
        val pr2 = corrections2.find { it.ruleId == "PRONOUN_CASE_AND_CONTRACTION" }
        assertNotNull("Should flag 'between you and I'", pr2)
        assertEquals("me", pr2?.suggestedReplacement)

        val text3 = "Your welcome to join."
        val corrections3 = facade.checkText(text3)
        val pr3 = corrections3.find { it.ruleId == "PRONOUN_CASE_AND_CONTRACTION" }
        assertNotNull("Should flag 'Your welcome'", pr3)
        assertEquals("You're", pr3?.suggestedReplacement)

        val text4 = "Its raining heavily."
        val corrections4 = facade.checkText(text4)
        val pr4 = corrections4.find { it.ruleId == "PRONOUN_CASE_AND_CONTRACTION" }
        assertNotNull("Should flag 'Its raining'", pr4)
        assertEquals("It's", pr4?.suggestedReplacement)
    }

    // 10. DETERMINERS AND QUANTIFIERS TESTS
    @Test
    fun testDeterminersAndQuantifiers() = runBlocking {
        val text1 = "I read these book yesterday."
        val corrections1 = facade.checkText(text1)
        val det1 = corrections1.find { it.ruleId == "DETERMINER_QUANTIFIER_AGREEMENT" }
        assertNotNull("Should flag 'these book'", det1)
        assertEquals("books", det1?.suggestedReplacement)

        val text2 = "There are much problems with this approach."
        val corrections2 = facade.checkText(text2)
        val det2 = corrections2.find { it.ruleId == "DETERMINER_QUANTIFIER_AGREEMENT" }
        assertNotNull("Should flag 'much problems'", det2)
        assertEquals("many", det2?.suggestedReplacement)

        val text3 = "Every students must attend."
        val corrections3 = facade.checkText(text3)
        val det3 = corrections3.find { it.ruleId == "DETERMINER_QUANTIFIER_AGREEMENT" }
        assertNotNull("Should flag 'Every students'", det3)
        assertEquals("student", det3?.suggestedReplacement)
    }

    // 11. CONFUSED WORDS TESTS
    @Test
    fun testConfusedWordsAndCollocations() = runBlocking {
        val text1 = "We should of known the answer."
        val corrections1 = facade.checkText(text1)
        val cw1 = corrections1.find { it.ruleId == "CONFUSED_WORDS_AND_COLLOCATIONS" }
        assertNotNull("Should flag 'should of'", cw1)
        assertEquals("have", cw1?.suggestedReplacement)

        val text2 = "This phone is better then that one."
        val corrections2 = facade.checkText(text2)
        val cw2 = corrections2.find { it.ruleId == "CONFUSED_WORDS_AND_COLLOCATIONS" }
        assertNotNull("Should flag 'better then'", cw2)
        assertEquals("than", cw2?.suggestedReplacement)

        val text3 = "It depends of the situation."
        val corrections3 = facade.checkText(text3)
        val cw3 = corrections3.find { it.ruleId == "CONFUSED_WORDS_AND_COLLOCATIONS" }
        assertNotNull("Should flag 'depends of'", cw3)
        assertEquals("on", cw3?.suggestedReplacement)
    }

    // 12. DOUBLE COMPARATIVES AND ADVERBS TESTS
    @Test
    fun testDoubleComparativesAndAdverbs() = runBlocking {
        val text1 = "This idea is more better than before."
        val corrections1 = facade.checkText(text1)
        val comp1 = corrections1.find { it.ruleId == "DOUBLE_COMPARATIVE" }
        assertNotNull("Should flag 'more better'", comp1)

        val text2 = "He speaks English good."
        val corrections2 = facade.checkText(text2)
        val adv1 = corrections2.find { it.ruleId == "ADJECTIVE_ADVERB_MISUSE" }
        assertNotNull("Should flag 'speaks English good'", adv1)
        assertEquals("well", adv1?.suggestedReplacement)
    }

    // 13. NEGATION AND REDUNDANCY TESTS
    @Test
    fun testNegationAndRedundancy() = runBlocking {
        val text1 = "I don't know nothing about it."
        val corrections1 = facade.checkText(text1)
        val neg1 = corrections1.find { it.ruleId == "DOUBLE_NEGATIVE" }
        assertNotNull("Should flag 'don't know nothing'", neg1)
        assertEquals("anything", neg1?.suggestedReplacement)

        val text2 = "We arrived in the the afternoon."
        val corrections2 = facade.checkText(text2)
        val red1 = corrections2.find { it.ruleId == "REPEATED_WORD" }
        assertNotNull("Should flag repeated 'the the'", red1)
    }

    // 14. CORRECTION APPLIER TESTS
    @Test
    fun testCorrectionApplierReverseOrder() = runBlocking {
        val original = "She have a apple."
        val corrections = facade.checkText(original)
        assertTrue("Should detect at least 2 errors", corrections.size >= 2)

        val applier = CorrectionApplier()
        val corrected = applier.applyMultipleCorrections(original, corrections)
        assertEquals("She has an apple.", corrected)
    }

    // 15. AUTOMATED REGRESSION RUNNER
    @Test
    fun testGrammarTestRunnerSuite() = runBlocking {
        val runner = GrammarTestRunner(facade)
        val report = runner.runAllTests()

        println("=== GRAMMAR TEST RUNNER REPORT ===")
        println("Rules Passed: ${report.rulesPassed} / ${report.totalRules}")
        println("Test Cases Passed: ${report.totalPassedTestCases} / ${report.totalTestCases}")
        println("Overall Accuracy: ${(report.overallAccuracy * 100).toInt()}%")

        for (r in report.results) {
            if (!r.isPassed) {
                println("Rule ${r.ruleId} had failures:")
                r.failures.forEach { println("  - $it") }
            }
        }

        assertTrue("Expected accuracy above 90%", report.overallAccuracy >= 0.90f)
    }
}
