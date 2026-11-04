package com.wordpilot.assistant.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_entries")
data class DictionaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String,
    val category: String = "custom",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ignored_rules")
data class IgnoredRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ruleId: String,
    val word: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)
