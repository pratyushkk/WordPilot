package com.wordpilot.assistant.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary_entries ORDER BY word ASC")
    fun getAllEntries(): Flow<List<DictionaryEntity>>

    @Query("SELECT * FROM dictionary_entries WHERE word = :word LIMIT 1")
    suspend fun findWord(word: String): DictionaryEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM dictionary_entries WHERE word = :word)")
    suspend fun wordExists(word: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DictionaryEntity)

    @Delete
    suspend fun delete(entry: DictionaryEntity)

    @Query("DELETE FROM dictionary_entries WHERE word = :word")
    suspend fun deleteByWord(word: String)

    @Query("DELETE FROM dictionary_entries")
    suspend fun deleteAll()

    @Query("SELECT word FROM dictionary_entries")
    suspend fun getAllWords(): List<String>

    // Ignored rules
    @Query("SELECT * FROM ignored_rules ORDER BY addedTimestamp DESC")
    fun getAllIgnoredRules(): Flow<List<IgnoredRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIgnoredRule(rule: IgnoredRuleEntity)

    @Delete
    suspend fun deleteIgnoredRule(rule: IgnoredRuleEntity)

    @Query("DELETE FROM ignored_rules")
    suspend fun deleteAllIgnoredRules()

    @Query("SELECT EXISTS(SELECT 1 FROM ignored_rules WHERE ruleId = :ruleId AND (word IS NULL OR word = :word))")
    suspend fun isRuleIgnored(ruleId: String, word: String? = null): Boolean
}
