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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
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
    lateinit var providerOrchestrator: com.wordpilot.assistant.engine.orchestration.ProviderOrchestrator
        private set
    lateinit var saplingProvider: com.wordpilot.assistant.engine.providers.SaplingProvider
        private set
    lateinit var languageToolProvider: com.wordpilot.assistant.engine.providers.LanguageToolProvider
        private set
    lateinit var grammarBotProvider: com.wordpilot.assistant.engine.providers.GrammarBotProvider
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

        sentenceAnalyzer = SentenceAnalyzer()
        saplingProvider = com.wordpilot.assistant.engine.providers.SaplingProvider()
        languageToolProvider = com.wordpilot.assistant.engine.providers.LanguageToolProvider()
        grammarBotProvider = com.wordpilot.assistant.engine.providers.GrammarBotProvider()

        val providersMap = mapOf(
            "sapling" to saplingProvider,
            "languagetool" to languageToolProvider,
            "grammarbot" to grammarBotProvider
        )
        providerOrchestrator = com.wordpilot.assistant.engine.orchestration.ProviderOrchestrator(providersMap)

        correctionCoordinator = CorrectionCoordinator(
            providerOrchestrator = providerOrchestrator,
            spellEngine = spellEngine,
            grammarEngine = grammarEngine,
            sentenceAnalyzer = sentenceAnalyzer
        )

        val safeReplacementHelper = SafeReplacementHelper()
        ruleBasedRewriteEngine = RuleBasedRewriteEngine(correctionCoordinator, safeReplacementHelper)
        aiEngine = LocalAIEngineImpl(modelRepository, ruleBasedRewriteEngine)

        geminiEngine = GeminiAIEngine { cachedGeminiApiKey }

        rewriteCoordinator = RewriteCoordinator(
            geminiEngine = geminiEngine,
            localEngine = aiEngine,
            ruleEngine = ruleBasedRewriteEngine,
            safeReplacementHelper = safeReplacementHelper,
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
            userPreferences.blockCloudAi.collect { blocked ->
                rewriteCoordinator.setBlockCloudAi(blocked)
            }
        }

        applicationScope.launch {
            userPreferences.selectedEngine.collect { engineId ->
                val type = AIEngineType.fromId(engineId)
                rewriteCoordinator.setEngineType(type)
            }
        }

        // Observe provider configuration from UserPreferences
        applicationScope.launch {
            combine(
                userPreferences.saplingEnabled,
                userPreferences.saplingApiKey
            ) { enabled, apiKey ->
                saplingProvider.configure(
                    com.wordpilot.assistant.engine.providers.ProviderConfig(
                        id = "sapling",
                        enabled = enabled,
                        apiKey = apiKey,
                        baseUrl = null,
                        priority = 1
                    )
                )
            }.collect()
        }

        applicationScope.launch {
            combine(
                userPreferences.languageToolEnabled,
                userPreferences.languageToolBaseUrl
            ) { enabled, baseUrl ->
                languageToolProvider.configure(
                    com.wordpilot.assistant.engine.providers.ProviderConfig(
                        id = "languagetool",
                        enabled = enabled,
                        apiKey = null,
                        baseUrl = baseUrl,
                        priority = 2
                    )
                )
            }.collect()
        }

        applicationScope.launch {
            combine(
                userPreferences.grammarBotEnabled,
                userPreferences.grammarBotApiKey
            ) { enabled, apiKey ->
                grammarBotProvider.configure(
                    com.wordpilot.assistant.engine.providers.ProviderConfig(
                        id = "grammarbot",
                        enabled = enabled,
                        apiKey = apiKey,
                        baseUrl = null,
                        priority = 3
                    )
                )
            }.collect()
        }


        applicationScope.launch {
            userPreferences.grammarProviderPriority.collect { raw ->
                val list = raw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                if (list.isNotEmpty()) {
                    providerOrchestrator.updatePriority(list)
                }
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
