package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.GeminiService
import com.example.data.repository.ChatRepository
import com.example.ui.screens.ChatViewModel
import com.example.ui.screens.MainChatScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * ChatActivity: Dedicated activity hosting the full-featured Chat UI
 * with message list, multimodal input area, and conversation state management.
 */
class ChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

        fun start(context: Context, conversationId: String? = null) {
            val intent = Intent(context, ChatActivity::class.java).apply {
                if (conversationId != null) {
                    putExtra(EXTRA_CONVERSATION_ID, conversationId)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            // If user session is absent, return to MainActivity auth gate
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        val uid = currentUser.uid
        val displayName = currentUser.displayName ?: "User"
        val email = currentUser.email ?: ""
        val targetConversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    key(uid) {
                        val chatViewModel: ChatViewModel = viewModel(
                            key = uid,
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                                    val dbId = app.getString(R.string.firestore_database_id)
                                    val firestore = FirebaseFirestore.getInstance(dbId)
                                    val repo = ChatRepository(firestore)
                                    val gemini = GeminiService()
                                    ChatViewModel(
                                        repository = repo,
                                        geminiService = gemini,
                                        currentUserId = uid,
                                        userDisplayName = displayName,
                                        userEmail = email
                                    )
                                }
                            }
                        )

                        // If a conversation ID was passed in Intent, select it
                        if (targetConversationId != null && chatViewModel.selectedConversationId.value == null) {
                            val active = chatViewModel.conversations.value.firstOrNull { it.id == targetConversationId }
                            if (active != null) {
                                chatViewModel.selectConversation(active)
                            }
                        }

                        MainChatScreen(
                            viewModel = chatViewModel,
                            onSignOut = {
                                val mainIntent = Intent(this@ChatActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(mainIntent)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
