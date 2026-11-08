package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.GeminiService
import com.example.data.repository.ChatRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatViewModel
import com.example.ui.screens.MainChatScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Mandatory App Check configuration with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)
        com.example.ui.theme.ThemeManager.init(this)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val user = currentUser!!
        val uid = user.uid
        val displayName = user.displayName ?: "User"
        val email = user.email ?: ""

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

            MainChatScreen(
                viewModel = chatViewModel,
                onSignOut = {
                    currentUser = null
                }
            )
        }
    }
}
