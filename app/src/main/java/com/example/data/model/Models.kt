package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val dailyMessageCount: Long = 0L,
    val quotaResetEpochDay: Long = 0L,
    val customApiKey: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Project(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val systemInstruction: String = "",
    val defaultModel: String = "gemini-3.8-flash",
    val knowledgeContent: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String? = null,
    val title: String = "New Chat",
    val modelId: String = "gemini-3.8-flash",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Message(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "user", // "user", "model", "system"
    val content: String = "",
    val attachments: List<String> = emptyList(),
    val groundingSources: List<String> = emptyList(),
    val codeExecOutput: String = "",
    val generatedImageUrl: String = "",
    val tokenCount: Long = 0L,
    val characterCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
