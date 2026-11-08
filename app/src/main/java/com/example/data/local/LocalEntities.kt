package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_conversations")
data class LocalConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val projectId: String? = null,
    val title: String,
    val modelId: String,
    val systemPrompt: String = "",
    val temperature: Double = 0.7,
    val maxOutputTokens: Long = 2048L,
    val thinkingBudget: Long = 0L,
    val toolWebSearch: Boolean = false,
    val toolCodeExecution: Boolean = false,
    val toolImageGeneration: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val summary: String = "",
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_messages")
data class LocalMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val userId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val attachmentsRaw: String = "", // serialized list or comma-separated
    val groundingSourcesRaw: String = "",
    val codeExecOutput: String = "",
    val generatedImageUrl: String = "",
    val tokenCount: Long = 0L,
    val characterCount: Long = 0L,
    val createdAtEpoch: Long = System.currentTimeMillis()
)
