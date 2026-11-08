package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class ModelOption(
    val id: String,
    val friendlyName: String,
    val tier: String,
    val description: String,
    val latencyHint: String,
    val costHint: String,
    val requiresBlaze: Boolean = false
)

object ModelRegistry {
    val FAST = ModelOption(
        id = "gemini-3.5-flash-lite",
        friendlyName = "Fast",
        tier = "Flash-Lite class",
        description = "Ultra fast, high-volume workhorse model for quick responses and summarization.",
        latencyHint = "~0.5s TTFT",
        costHint = "Free Tier Included",
        requiresBlaze = false
    )

    val BALANCED = ModelOption(
        id = "gemini-3.8-flash",
        friendlyName = "Balanced",
        tier = "Flash class (Default)",
        description = "Frontier-class intelligence, versatile multimodal understanding, code & reasoning.",
        latencyHint = "~1.2s TTFT",
        costHint = "Free Tier Included",
        requiresBlaze = false
    )

    val DEEP = ModelOption(
        id = "gemini-4-argon",
        friendlyName = "Deep",
        tier = "Pro class reasoning",
        description = "Advanced multi-step reasoning, complex STEM logic, code architecture, and math.",
        latencyHint = "~2.8s TTFT",
        costHint = "Blaze Plan",
        requiresBlaze = true
    )

    val ALL_MODELS = listOf(FAST, BALANCED, DEEP)

    fun getById(id: String): ModelOption {
        return ALL_MODELS.firstOrNull { it.id == id } ?: BALANCED
    }
}

class GeminiService {

    companion object {
        private const val TAG = "GeminiService"
        private const val TIMEOUT_MILLIS = 60_000L
    }

    /**
     * Streams response chunks from Gemini via Firebase AI Logic.
     */
    fun streamChatResponse(
        modelId: String,
        systemInstruction: String?,
        conversationHistory: List<Pair<String, String>>, // role to content
        latestUserPrompt: String,
        bitmaps: List<Bitmap> = emptyList(),
        temperature: Float = 0.7f,
        maxOutputTokens: Int? = 2048,
        thinkingBudget: Int? = null,
        webSearchEnabled: Boolean = false,
        codeExecutionEnabled: Boolean = false
    ): Flow<String> = flow {
        val modelName = if (modelId.isNotBlank()) modelId else ModelRegistry.BALANCED.id

        val config = generationConfig {
            this.temperature = temperature
            if (maxOutputTokens != null && maxOutputTokens > 0) {
                this.maxOutputTokens = maxOutputTokens
            }
        }

        val generativeModel = Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = config,
            systemInstruction = if (!systemInstruction.isNullOrBlank()) {
                content { text(systemInstruction) }
            } else null
        )

        // Prepare prior turns for multi-turn chat
        val chatHistory = conversationHistory.map { (role, msgContent) ->
            val firebaseRole = if (role == "user") "user" else "model"
            content(firebaseRole) { text(msgContent) }
        }

        val chat = generativeModel.startChat(chatHistory)

        withTimeout(TIMEOUT_MILLIS) {
            val userContent = content("user") {
                for (bmp in bitmaps) {
                    image(bmp)
                }
                text(latestUserPrompt)
            }

            chat.sendMessageStream(userContent).collect { chunk ->
                val chunkText = chunk.text
                if (!chunkText.isNullOrEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }

    /**
     * Unary content generation (for summarization or title generation).
     */
    suspend fun generateUnary(
        modelId: String = ModelRegistry.FAST.id,
        prompt: String,
        systemInstruction: String? = null
    ): String {
        return try {
            val generativeModel = Firebase.ai.generativeModel(
                modelName = modelId,
                systemInstruction = if (!systemInstruction.isNullOrBlank()) {
                    content { text(systemInstruction) }
                } else null
            )
            withTimeout(30_000L) {
                val response = generativeModel.generateContent(prompt)
                response.text.orEmpty().trim()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unary generation failed", e)
            throw e
        }
    }
}
