package com.wordpilot.assistant

import android.app.Application
import com.wordpilot.assistant.data.database.WordPilotDatabase
import com.wordpilot.assistant.data.preferences.UserPreferences
import com.wordpilot.assistant.data.repository.DictionaryRepositoryImpl
import com.wordpilot.assistant.data.repository.HistoryRepositoryImpl
import com.wordpilot.assistant.data.repository.ModelRepositoryImpl
import com.wordpilot.assistant.domain.correction.CorrectionUseCase
import com.wordpilot.assistant.domain.correction.SafeReplacementHelper
import com.wordpilot.assistant.domain.rewriting.RewriteUseCase
import com.wordpilot.assistant.engine.CorrectionCoordinator
import com.wordpilot.assistant.engine.SentenceAnalyzer
import com.wordpilot.assistant.engine.impl.RuleBasedGrammarEngine
import com.wordpilot.assistant.engine.impl.StubAIEngine
import com.wordpilot.assistant.engine.impl.TrieSpellEngine
import java.io.File

class WordPilotApp : Application() {

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
    lateinit var spellEngine: TrieSpellEngine
        private set
    lateinit var grammarEngine: RuleBasedGrammarEngine
        private set
    lateinit var aiEngine: StubAIEngine
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
        spellEngine = TrieSpellEngine(this)
        grammarEngine = RuleBasedGrammarEngine()
        aiEngine = StubAIEngine()
        correctionCoordinator = CorrectionCoordinator(spellEngine, grammarEngine)
        sentenceAnalyzer = SentenceAnalyzer()

        // Use Cases
        val safeReplacementHelper = SafeReplacementHelper()
        correctionUseCase = CorrectionUseCase(correctionCoordinator, sentenceAnalyzer, safeReplacementHelper)
        rewriteUseCase = RewriteUseCase(aiEngine)
    }

    companion object {
        lateinit var instance: WordPilotApp
            private set
    }
}
