package com.wordpilot.assistant.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DictionaryEntity::class,
        IgnoredRuleEntity::class,
        DocumentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WordPilotDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: WordPilotDatabase? = null

        fun getInstance(context: Context): WordPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WordPilotDatabase::class.java,
                    "wordpilot.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
