package com.wordpilot.assistant.data.repository

import com.wordpilot.assistant.data.database.DictionaryDao
import com.wordpilot.assistant.data.database.DictionaryEntity
import com.wordpilot.assistant.data.database.IgnoredRuleEntity
import com.wordpilot.assistant.domain.models.DictionaryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DictionaryRepositoryImpl(private val dao: DictionaryDao) {

    fun getAllEntries(): Flow<List<DictionaryEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addWord(word: String, category: String = "custom") {
        dao.insert(DictionaryEntity(word = word.lowercase().trim(), category = category))
    }

    suspend fun removeWord(word: String) {
        dao.deleteByWord(word.lowercase().trim())
    }

    suspend fun wordExists(word: String): Boolean {
        return dao.wordExists(word.lowercase().trim())
    }

    suspend fun getAllWords(): Set<String> {
        return dao.getAllWords().toSet()
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    // Ignored rules
    fun getAllIgnoredRules(): Flow<List<IgnoredRuleEntity>> {
        return dao.getAllIgnoredRules()
    }

    suspend fun ignoreRule(ruleId: String, word: String? = null) {
        dao.insertIgnoredRule(IgnoredRuleEntity(ruleId = ruleId, word = word))
    }

    suspend fun restoreRule(rule: IgnoredRuleEntity) {
        dao.deleteIgnoredRule(rule)
    }

    suspend fun isRuleIgnored(ruleId: String, word: String? = null): Boolean {
        return dao.isRuleIgnored(ruleId, word)
    }

    suspend fun clearAllIgnoredRules() {
        dao.deleteAllIgnoredRules()
    }

    private fun DictionaryEntity.toDomain() = DictionaryEntry(
        id = id,
        word = word,
        category = category,
        addedTimestamp = addedTimestamp
    )
}
