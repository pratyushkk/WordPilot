package com.wordpilot.assistant

import android.app.Application
import com.wordpilot.assistant.data.database.WordPilotDatabase
import com.wordpilot.assistant.data.preferences.UserPreferences
import com.wordpilot.assistant.data.repository.DictionaryRepositoryImpl
import com.wordpilot.assistant.data.repository.HistoryRepositoryImpl
import com.wordpilot.assistant.data.repository.ModelRepositoryImpl
import com.wordpilot.assistant.domain.correction.CorrectionUseCase
import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.models.AIEngineType
import com.wordpilot.assistant.domain.rewriting.RewriteUseCase
import com.wordpilot.assistant.engine.CorrectionCoordinator
import com.wordpilot.assistant.engine.RewriteCoordinator
import com.wordpilot.assistant.engine.SentenceAnalyzer
import com.wordpilot.assistant.engine.LocalAIEngine
import com.wordpilot.assistant.engine.impl.GeminiAIEngine
import com.wordpilot.assistant.engine.impl.LocalAIEngineImpl
import com.wordpilot.assistant.engine.impl.RuleBasedGrammarEngine
import com.wordpilot.assistant.engine.impl.RuleBasedRewriteEngine
import com.wordpilot.assistant.engine.impl.TrieSpellEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

class WordPilotApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Database
    lateinit var database: WordPilotDatabase
        private set

    // Preferences
    lateinit var userPreferences: UserPreferences
        private set

    // Repositories
    lateinit var dictionaryRepository: DictionaryRepositoryImpl
        private set
    lateinit var historyRepository: HistoryRepositoryImpl
        private set
    lateinit var modelRepository: ModelRepositoryImpl
        private set

    // Engines
    lateinit var offlineGrammarFacade: com.wordpilot.assistant.engine.offline.OfflineGrammarEngineFacade
        private set
    lateinit var spellEngine: TrieSpellEngine
        private set
    lateinit var grammarEngine: RuleBasedGrammarEngine
        private set
    lateinit var aiEngine: LocalAIEngine
        private set
    lateinit var geminiEngine: GeminiAIEngine
        private set
    lateinit var ruleBasedRewriteEngine: RuleBasedRewriteEngine
        private set
    lateinit var rewriteCoordinator: RewriteCoordinator
        private set
    lateinit var correctionCoordinator: CorrectionCoordinator
        private set
    lateinit var sentenceAnalyzer: SentenceAnalyzer
        private set

    // Use Cases
    lateinit var correctionUseCase: CorrectionUseCase
        private set
    lateinit var rewriteUseCase: RewriteUseCase
        private set

    var cachedGeminiApiKey: String? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        initDependencies()
    }

    private fun initDependencies() {
        // Database
        database = WordPilotDatabase.getInstance(this)

        // Preferences
        userPreferences = UserPreferences(this)

        // Repositories
        dictionaryRepository = DictionaryRepositoryImpl(database.dictionaryDao())
        historyRepository = HistoryRepositoryImpl(database.historyDao())
        modelRepository = ModelRepositoryImpl(File(filesDir, "models"))

        // Engines
        offlineGrammarFacade = com.wordpilot.assistant.engine.offline.OfflineGrammarEngineFacade(this)
        spellEngine = TrieSpellEngine(this, offlineGrammarFacade)
        grammarEngine = RuleBasedGrammarEngine(this, offlineGrammarFacade)
        correctionCoordinator = CorrectionCoordinator(spellEngine, grammarEngine)
        sentenceAnalyzer = SentenceAnalyzer()

        val safeReplacementHelper = SafeReplacementHelper()
        ruleBasedRewriteEngine = RuleBasedRewriteEngine(correctionCoordinator, safeReplacementHelper)
        aiEngine = LocalAIEngineImpl(modelRepository, ruleBasedRewriteEngine)

        geminiEngine = GeminiAIEngine { cachedGeminiApiKey }

        rewriteCoordinator = RewriteCoordinator(
            geminiEngine = geminiEngine,
            localEngine = aiEngine,
            ruleEngine = ruleBasedRewriteEngine,
            initialEngineType = AIEngineType.GEMINI_FLASH
        )

        // Use Cases
        correctionUseCase = CorrectionUseCase(correctionCoordinator, sentenceAnalyzer, safeReplacementHelper)
        rewriteUseCase = RewriteUseCase(rewriteCoordinator)

        // Preload API key and preferred engine from preferences
        applicationScope.launch {
            try {
                val words = dictionaryRepository.getAllWords()
                words.forEach { offlineGrammarFacade.addCustomWord(it) }
            } catch (_: Exception) {}
        }

        applicationScope.launch {
            userPreferences.geminiApiKey.collect { key ->
                cachedGeminiApiKey = key
            }
        }

        applicationScope.launch {
            userPreferences.selectedEngine.collect { engineId ->
                val type = AIEngineType.fromId(engineId)
                rewriteCoordinator.setEngineType(type)
            }
        }
    }

    fun updateGeminiApiKey(key: String) {
        cachedGeminiApiKey = key
        applicationScope.launch {
            userPreferences.setGeminiApiKey(key)
        }
    }

    companion object {
        lateinit var instance: WordPilotApp
            private set
    }
}
