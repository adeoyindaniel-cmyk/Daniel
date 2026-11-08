package com.example.data.repository

import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.Project
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- User Profile & Quotas ---

    suspend fun getOrCreateUserProfile(displayName: String, email: String): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java) ?: UserProfile(id = uid, userId = uid, displayName = displayName, email = email)
        }

        val epochDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to displayName.take(100),
            "email" to email.take(200),
            "dailyMessageCount" to 0L,
            "quotaResetEpochDay" to epochDay,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return UserProfile(
                id = uid,
                userId = uid,
                displayName = displayName,
                email = email,
                dailyMessageCount = 0L,
                quotaResetEpochDay = epochDay,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCustomApiKey(apiKey: String?) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "customApiKey" to apiKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun incrementDailyQuota(dailyCap: Long = 50L): Pair<Boolean, Long> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()
        val profile = snapshot.toObject(UserProfile::class.java)

        val currentEpochDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val profileEpochDay = profile?.quotaResetEpochDay ?: 0L
        val currentCount = if (profileEpochDay < currentEpochDay) 0L else (profile?.dailyMessageCount ?: 0L)

        // If custom API key is present, unlimited usage
        if (!profile?.customApiKey.isNullOrBlank()) {
            val newCount = currentCount + 1
            docRef.update(
                mapOf(
                    "dailyMessageCount" to newCount,
                    "quotaResetEpochDay" to currentEpochDay,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return Pair(true, newCount)
        }

        if (currentCount >= dailyCap) {
            return Pair(false, currentCount)
        }

        val newCount = currentCount + 1
        docRef.update(
            mapOf(
                "dailyMessageCount" to newCount,
                "quotaResetEpochDay" to currentEpochDay,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return Pair(true, newCount)
    }

    fun observeUserProfile(): Flow<UserProfile?> {
        val uid = requireUserId()
        return db.collection("users").document(uid).snapshots().map { it.toObject(UserProfile::class.java) }
    }

    // --- Projects ---

    fun observeProjects(): Flow<List<Project>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("projects")
            .snapshots()
            .map { it.toObjects(Project::class.java) }
    }

    suspend fun createProject(name: String, description: String, instructions: String, model: String, knowledge: String): String {
        val uid = requireUserId()
        val docId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("projects").document(docId)
        val payload = mapOf(
            "userId" to uid,
            "name" to name.take(100),
            "description" to description.take(500),
            "systemInstruction" to instructions.take(5000),
            "defaultModel" to model.take(100),
            "knowledgeContent" to knowledge.take(20000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProject(projectId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Conversations ---

    fun observeConversations(): Flow<List<Conversation>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("conversations")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java)
            }
    }

    suspend fun createConversation(
        title: String,
        modelId: String,
        projectId: String? = null,
        systemPrompt: String = "",
        temperature: Double = 0.7,
        maxTokens: Long = 2048L,
        thinkingBudget: Long = 0L,
        toolWebSearch: Boolean = false,
        toolCodeExecution: Boolean = false,
        toolImageGeneration: Boolean = false
    ): String {
        val uid = requireUserId()
        val docId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("conversations").document(docId)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to title.take(200),
            "modelId" to modelId.take(100),
            "isPinned" to false,
            "isArchived" to false,
            "systemPrompt" to systemPrompt.take(5000),
            "temperature" to temperature,
            "maxOutputTokens" to maxTokens,
            "thinkingBudget" to thinkingBudget,
            "toolWebSearch" to toolWebSearch,
            "toolCodeExecution" to toolCodeExecution,
            "toolImageGeneration" to toolImageGeneration,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!projectId.isNullOrBlank()) {
            payload["projectId"] = projectId
        }
        try {
            docRef.set(payload).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateConversation(conversationId: String, updates: Map<String, Any>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
        val fullUpdates = updates.toMutableMap()
        fullUpdates["updatedAt"] = FieldValue.serverTimestamp()
        try {
            docRef.update(fullUpdates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Messages ---

    fun observeMessages(conversationId: String): Flow<List<Message>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("conversations").document(conversationId)
            .collection("messages")
            .snapshots()
            .map { it.toObjects(Message::class.java) }
    }

    suspend fun addMessage(
        conversationId: String,
        role: String,
        content: String,
        attachments: List<String> = emptyList(),
        groundingSources: List<String> = emptyList(),
        codeExecOutput: String = "",
        generatedImageUrl: String = ""
    ): String {
        val uid = requireUserId()
        val docId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
            .collection("messages").document(docId)

        val tokenEstimate = (content.length / 4L).coerceAtLeast(1L)
        val payload = mapOf(
            "userId" to uid,
            "conversationId" to conversationId,
            "role" to role,
            "content" to content.take(100000),
            "attachments" to attachments,
            "groundingSources" to groundingSources,
            "codeExecOutput" to codeExecOutput.take(50000),
            "generatedImageUrl" to generatedImageUrl.take(2000),
            "tokenCount" to tokenEstimate,
            "characterCount" to content.length.toLong(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            // Also bump conversation updatedAt
            db.collection("users").document(uid).collection("conversations").document(conversationId)
                .update("updatedAt", FieldValue.serverTimestamp()).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateMessageContent(conversationId: String, messageId: String, newContent: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        try {
            val tokenEstimate = (newContent.length / 4L).coerceAtLeast(1L)
            docRef.update(
                mapOf(
                    "content" to newContent.take(100000),
                    "tokenCount" to tokenEstimate,
                    "characterCount" to newContent.length.toLong()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
