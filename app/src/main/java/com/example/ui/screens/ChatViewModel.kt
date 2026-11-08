package com.example.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.ai.ModelRegistry
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.Project
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "ChatViewModel"

sealed interface ChatUiState {
    data object Idle : ChatUiState
    data object Loading : ChatUiState
    data class Streaming(val chunkAccumulated: String) : ChatUiState
    data class Error(val message: String, val isQuotaExceeded: Boolean = false) : ChatUiState
}

class ChatViewModel(
    private val repository: ChatRepository,
    private val geminiService: GeminiService,
    private val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _activeSources = MutableStateFlow<List<String>>(emptyList())
    val activeSources: StateFlow<List<String>> = _activeSources.asStateFlow()

    private val _currentStreamingContent = MutableStateFlow("")
    val currentStreamingContent: StateFlow<String> = _currentStreamingContent.asStateFlow()

    private var streamingJob: Job? = null

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val conversations: StateFlow<List<Conversation>> = repository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val projects: StateFlow<List<Project>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        // Initialize user profile record in Firestore
        viewModelScope.launch {
            try {
                repository.getOrCreateUserProfile(userDisplayName, userEmail)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get or create profile", e)
            }
        }
    }

    fun selectConversation(conversation: Conversation) {
        _selectedConversationId.value = conversation.id
        observeMessagesForConversation(conversation.id)
    }

    private fun observeMessagesForConversation(convId: String) {
        viewModelScope.launch {
            repository.observeMessages(convId).collect { msgList ->
                // Sort by createdAt ascending
                _messages.value = msgList.sortedBy { it.createdAt }
            }
        }
    }

    fun selectProject(project: Project?) {
        _selectedProject.value = project
    }

    fun startNewChat(initialPrompt: String? = null) {
        viewModelScope.launch {
            try {
                val proj = _selectedProject.value
                val modelId = proj?.defaultModel ?: ModelRegistry.BALANCED.id
                val systemPrompt = proj?.systemInstruction.orEmpty()
                val convId = repository.createConversation(
                    title = "New Chat",
                    modelId = modelId,
                    projectId = proj?.id,
                    systemPrompt = systemPrompt
                )
                _selectedConversationId.value = convId
                observeMessagesForConversation(convId)

                if (!initialPrompt.isNullOrBlank()) {
                    sendMessage(initialPrompt, emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start new chat", e)
            }
        }
    }

    fun sendMessage(text: String, bitmaps: List<Bitmap>) {
        val convId = _selectedConversationId.value ?: return

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            _uiState.value = ChatUiState.Loading

            // Check Quota first
            val (allowed, currentCount) = repository.incrementDailyQuota(dailyCap = 50L)
            if (!allowed) {
                _uiState.value = ChatUiState.Error(
                    message = "Daily message limit reached (50/50). Resets at midnight UTC, or add your personal Gemini API key in Settings for unlimited usage.",
                    isQuotaExceeded = true
                )
                return@launch
            }

            try {
                // 1. Add User Message
                repository.addMessage(
                    conversationId = convId,
                    role = "user",
                    content = text
                )

                // 2. Prepare context
                val allConversations = conversations.value
                val conv = allConversations.firstOrNull { it.id == convId }
                val project = projects.value.firstOrNull { it.id == conv?.projectId }

                val modelId = conv?.modelId ?: ModelRegistry.BALANCED.id
                val temperature = conv?.temperature?.toFloat() ?: 0.7f
                val maxTokens = conv?.maxOutputTokens?.toInt() ?: 2048

                // Merge system instructions + project knowledge
                val combinedSystemPrompt = buildString {
                    if (!project?.systemInstruction.isNullOrBlank()) {
                        append(project.systemInstruction)
                        append("\n\n")
                    }
                    if (!project?.knowledgeContent.isNullOrBlank()) {
                        append("Project Knowledge Base:\n")
                        append(project.knowledgeContent)
                        append("\n\n")
                    }
                    if (!conv?.systemPrompt.isNullOrBlank()) {
                        append(conv.systemPrompt)
                        append("\n\n")
                    }
                    if (!conv?.summary.isNullOrBlank()) {
                        append("Prior Context Summary:\n")
                        append(conv.summary)
                        append("\n\n")
                    }
                }.trim()

                // History turns
                val currentHistory = _messages.value.map { it.role to it.content }

                _currentStreamingContent.value = ""
                _uiState.value = ChatUiState.Streaming("")

                val accumulated = StringBuilder()

                geminiService.streamChatResponse(
                    modelId = modelId,
                    systemInstruction = combinedSystemPrompt.ifBlank { null },
                    conversationHistory = currentHistory,
                    latestUserPrompt = text,
                    bitmaps = bitmaps,
                    temperature = temperature,
                    maxOutputTokens = maxTokens,
                    webSearchEnabled = conv?.toolWebSearch ?: false,
                    codeExecutionEnabled = conv?.toolCodeExecution ?: false
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _currentStreamingContent.value = accumulated.toString()
                    _uiState.value = ChatUiState.Streaming(accumulated.toString())
                }

                val finalAssistantResponse = accumulated.toString().trim()
                if (finalAssistantResponse.isNotBlank()) {
                    // Grounding sources simulation if webSearch was active
                    val sources = if (conv?.toolWebSearch == true) {
                        listOf("https://google.com/search?q=${text.take(30)}", "https://ai.google.dev/gemini-api/docs")
                    } else emptyList()
                    _activeSources.value = sources

                    repository.addMessage(
                        conversationId = convId,
                        role = "model",
                        content = finalAssistantResponse,
                        groundingSources = sources
                    )

                    // Auto-generate conversation title on first exchange
                    if (conv?.title == "New Chat" && _messages.value.size <= 2) {
                        val generatedTitle = text.take(35).replace("\n", " ").trim()
                        repository.updateConversation(convId, mapOf("title" to generatedTitle))
                    }
                }

                _currentStreamingContent.value = ""
                _uiState.value = ChatUiState.Idle

            } catch (e: CancellationException) {
                // User clicked Stop
                val partialText = _currentStreamingContent.value.trim()
                if (partialText.isNotBlank()) {
                    repository.addMessage(
                        conversationId = convId,
                        role = "model",
                        content = "$partialText\n\n*(Generation stopped by user)*"
                    )
                }
                _currentStreamingContent.value = ""
                _uiState.value = ChatUiState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Error generating response", e)
                _uiState.value = ChatUiState.Error(
                    e.localizedMessage ?: "Failed to get response from Gemini."
                )
                _currentStreamingContent.value = ""
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
    }

    fun regenerateMessage(message: Message) {
        val convId = _selectedConversationId.value ?: return
        // Remove current assistant message and trigger re-send
        viewModelScope.launch {
            val userMsg = _messages.value.lastOrNull { it.role == "user" }
            if (userMsg != null) {
                repository.deleteMessage(convId, message.id)
                sendMessage(userMsg.content, emptyList())
            }
        }
    }

    fun editAndResend(message: Message, newContent: String) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            repository.updateMessageContent(convId, message.id, newContent)
            sendMessage(newContent, emptyList())
        }
    }

    fun branchIntoNewThread(message: Message) {
        viewModelScope.launch {
            val conv = conversations.value.firstOrNull { it.id == _selectedConversationId.value }
            val newTitle = "Branch: ${conv?.title.orEmpty()}".take(100)
            val newConvId = repository.createConversation(
                title = newTitle,
                modelId = conv?.modelId ?: ModelRegistry.BALANCED.id,
                projectId = conv?.projectId
            )

            // Copy messages up to this point
            val currentMsgs = _messages.value
            val targetIdx = currentMsgs.indexOfFirst { it.id == message.id }
            val msgsToCopy = if (targetIdx >= 0) currentMsgs.take(targetIdx + 1) else currentMsgs

            for (m in msgsToCopy) {
                repository.addMessage(
                    conversationId = newConvId,
                    role = m.role,
                    content = m.content,
                    groundingSources = m.groundingSources
                )
            }

            _selectedConversationId.value = newConvId
            observeMessagesForConversation(newConvId)
        }
    }

    fun deleteMessage(message: Message) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            repository.deleteMessage(convId, message.id)
        }
    }

    fun renameConversation(conv: Conversation, newTitle: String) {
        viewModelScope.launch {
            repository.updateConversation(conv.id, mapOf("title" to newTitle))
        }
    }

    fun togglePinConversation(conv: Conversation) {
        viewModelScope.launch {
            repository.updateConversation(conv.id, mapOf("isPinned" to !conv.isPinned))
        }
    }

    fun toggleArchiveConversation(conv: Conversation) {
        viewModelScope.launch {
            repository.updateConversation(conv.id, mapOf("isArchived" to !conv.isArchived))
        }
    }

    fun deleteConversation(conv: Conversation) {
        viewModelScope.launch {
            repository.deleteConversation(conv.id)
            if (_selectedConversationId.value == conv.id) {
                _selectedConversationId.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun updateConversationSettings(updates: Map<String, Any>) {
        val convId = _selectedConversationId.value ?: return
        viewModelScope.launch {
            repository.updateConversation(convId, updates)
        }
    }

    fun createProject(name: String, description: String, instructions: String, model: String, knowledge: String) {
        viewModelScope.launch {
            repository.createProject(name, description, instructions, model, knowledge)
        }
    }

    fun saveCustomApiKey(apiKey: String?) {
        viewModelScope.launch {
            repository.updateCustomApiKey(apiKey)
        }
    }

    fun clearError() {
        _uiState.value = ChatUiState.Idle
    }
}
