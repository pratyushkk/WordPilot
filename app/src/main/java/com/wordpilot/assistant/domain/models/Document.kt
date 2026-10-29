package com.wordpilot.assistant.domain.models

data class Document(
    val id: Int = 0,
    val title: String,
    val content: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val modifiedTimestamp: Long = System.currentTimeMillis()
)
