package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // --- Conversations ---

    @Query("SELECT * FROM local_conversations WHERE userId = :userId ORDER BY isPinned DESC, updatedAtEpoch DESC")
    fun getConversationsForUser(userId: String): Flow<List<LocalConversationEntity>>

    @Query("SELECT * FROM local_conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): LocalConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConversation(conversation: LocalConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<LocalConversationEntity>)

    @Query("UPDATE local_conversations SET title = :newTitle, updatedAtEpoch = :updatedAt WHERE id = :conversationId")
    suspend fun updateConversationTitle(conversationId: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE local_conversations SET isPinned = :isPinned WHERE id = :conversationId")
    suspend fun updateConversationPinned(conversationId: String, isPinned: Boolean)

    @Query("UPDATE local_conversations SET isArchived = :isArchived WHERE id = :conversationId")
    suspend fun updateConversationArchived(conversationId: String, isArchived: Boolean)

    @Query("DELETE FROM local_conversations WHERE id = :conversationId")
    suspend fun deleteConversationById(conversationId: String)

    // --- Messages ---

    @Query("SELECT * FROM local_messages WHERE conversationId = :conversationId ORDER BY createdAtEpoch ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<LocalMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMessage(message: LocalMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalMessageEntity>)

    @Query("UPDATE local_messages SET content = :newContent, tokenCount = :tokenCount, characterCount = :charCount WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String, tokenCount: Long, charCount: Long)

    @Query("DELETE FROM local_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM local_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Transaction
    suspend fun deleteConversationAndMessages(conversationId: String) {
        deleteMessagesForConversation(conversationId)
        deleteConversationById(conversationId)
    }
}
