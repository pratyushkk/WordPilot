package com.wordpilot.assistant.data.repository

import com.wordpilot.assistant.data.database.DocumentEntity
import com.wordpilot.assistant.data.database.HistoryDao
import com.wordpilot.assistant.domain.models.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(private val dao: HistoryDao) {

    fun getAllDocuments(): Flow<List<Document>> {
        return dao.getAllDocuments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getDocument(id: Int): Document? {
        return dao.getDocument(id)?.toDomain()
    }

    suspend fun saveDocument(document: Document): Long {
        return dao.insert(document.toEntity())
    }

    suspend fun updateDocument(document: Document) {
        dao.update(document.toEntity().copy(modifiedTimestamp = System.currentTimeMillis()))
    }

    suspend fun deleteDocument(id: Int) {
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun getDocumentCount(): Int {
        return dao.getDocumentCount()
    }

    private fun DocumentEntity.toDomain() = Document(
        id = id,
        title = title,
        content = content,
        createdTimestamp = createdTimestamp,
        modifiedTimestamp = modifiedTimestamp
    )

    private fun Document.toEntity() = DocumentEntity(
        id = id,
        title = title,
        content = content,
        createdTimestamp = createdTimestamp,
        modifiedTimestamp = modifiedTimestamp
    )
}
