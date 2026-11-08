package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.ModelRegistry
import com.example.data.model.Message
import com.example.ui.components.ChatComposer
import com.example.ui.components.ConversationSidebar
import com.example.ui.components.EmptyChatState
import com.example.ui.components.MessageBubble
import com.example.ui.components.ModelAndParamsSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SourcesAndKnowledgePanel
import com.example.ui.components.ToolChipsBar
import com.example.ui.screens.signOutUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    viewModel: ChatViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val selectedConvId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()
    val activeSources by viewModel.activeSources.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val streamingContent by viewModel.currentStreamingContent.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showModelParamsSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRightPanel by remember { mutableStateOf(false) }

    val activeConversation = remember(conversations, selectedConvId) {
        conversations.firstOrNull { it.id == selectedConvId }
    }

    val isStreaming = uiState is ChatUiState.Streaming

    val listState = rememberLazyListState()

    // Auto-scroll on new message or streaming token
    LaunchedEffect(messages.size, streamingContent.length) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            listState.animateScrollToItem((messages.size).coerceAtLeast(0))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedLayout = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Expanded desktop/tablet 3-pane layout: Permanent left sidebar
            if (isExpandedLayout) {
                ConversationSidebar(
                    conversations = conversations,
                    selectedConversationId = selectedConvId,
                    onSelectConversation = { conv ->
                        viewModel.selectConversation(conv)
                    },
                    onNewChat = { viewModel.startNewChat() },
                    projects = projects,
                    onSelectProject = { viewModel.selectProject(it) },
                    selectedProject = selectedProject,
                    onOpenSettings = { showSettingsDialog = true },
                    onSignOut = {
                        signOutUser(context, coroutineScope, onSignOut)
                    },
                    onRenameConversation = { conv, title ->
                        viewModel.renameConversation(conv, title)
                    },
                    onPinConversation = { conv ->
                        viewModel.togglePinConversation(conv)
                    },
                    onArchiveConversation = { conv ->
                        viewModel.toggleArchiveConversation(conv)
                    },
                    onDeleteConversation = { conv ->
                        viewModel.deleteConversation(conv)
                    },
                    onCreateProject = { name, desc, inst, model, know ->
                        viewModel.createProject(name, desc, inst, model, know)
                    },
                    userDisplayName = viewModel.userDisplayName,
                    userEmail = viewModel.userEmail
                )
                HorizontalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
            }

            // Center Chat Pane
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = !isExpandedLayout,
                drawerContent = {
                    if (!isExpandedLayout) {
                        ConversationSidebar(
                            conversations = conversations,
                            selectedConversationId = selectedConvId,
                            onSelectConversation = { conv ->
                                viewModel.selectConversation(conv)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onNewChat = {
                                viewModel.startNewChat()
                                coroutineScope.launch { drawerState.close() }
                            },
                            projects = projects,
                            onSelectProject = { viewModel.selectProject(it) },
                            selectedProject = selectedProject,
                            onOpenSettings = {
                                showSettingsDialog = true
                                coroutineScope.launch { drawerState.close() }
                            },
                            onSignOut = {
                                signOutUser(context, coroutineScope, onSignOut)
                            },
                            onRenameConversation = { conv, title ->
                                viewModel.renameConversation(conv, title)
                            },
                            onPinConversation = { conv ->
                                viewModel.togglePinConversation(conv)
                            },
                            onArchiveConversation = { conv ->
                                viewModel.toggleArchiveConversation(conv)
                            },
                            onDeleteConversation = { conv ->
                                viewModel.deleteConversation(conv)
                            },
                            onCreateProject = { name, desc, inst, model, know ->
                                viewModel.createProject(name, desc, inst, model, know)
                            },
                            userDisplayName = viewModel.userDisplayName,
                            userEmail = viewModel.userEmail
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = activeConversation?.title ?: "Gemini Chat",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val currentModel = ModelRegistry.getById(activeConversation?.modelId.orEmpty())
                                    Text(
                                        text = "${currentModel.friendlyName} • ${currentModel.tier}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            navigationIcon = {
                                if (!isExpandedLayout) {
                                    IconButton(
                                        onClick = { coroutineScope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("drawer_menu_button")
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            },
                            actions = {
                                // Quick theme toggle button in top bar
                                IconButton(
                                    onClick = {
                                        val nextMode = when (com.example.ui.theme.ThemeManager.themeMode) {
                                            com.example.ui.theme.ThemeMode.DARK -> com.example.ui.theme.ThemeMode.LIGHT
                                            com.example.ui.theme.ThemeMode.LIGHT -> com.example.ui.theme.ThemeMode.DARK
                                            com.example.ui.theme.ThemeMode.SYSTEM -> com.example.ui.theme.ThemeMode.DARK
                                        }
                                        com.example.ui.theme.ThemeManager.setTheme(context, nextMode)
                                    },
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (com.example.ui.theme.ThemeManager.themeMode == com.example.ui.theme.ThemeMode.DARK)
                                            androidx.compose.material.icons.Icons.Default.DarkMode
                                        else androidx.compose.material.icons.Icons.Default.LightMode,
                                        contentDescription = "Toggle Theme"
                                    )
                                }

                                if (activeConversation != null) {
                                    IconButton(
                                        onClick = { showModelParamsSheet = true },
                                        modifier = Modifier.testTag("model_params_button")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = "Model Parameters")
                                    }
                                }
                                IconButton(
                                    onClick = { showRightPanel = !showRightPanel },
                                    modifier = Modifier.testTag("toggle_sources_panel_button")
                                ) {
                                    Icon(Icons.Default.Source, contentDescription = "Toggle Sources")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        if (activeConversation != null) {
                            Column {
                                // Tools and Grounding Chips
                                ToolChipsBar(
                                    webSearchEnabled = activeConversation.toolWebSearch,
                                    onToggleWebSearch = {
                                        viewModel.updateConversationSettings(mapOf("toolWebSearch" to it))
                                    },
                                    codeExecEnabled = activeConversation.toolCodeExecution,
                                    onToggleCodeExec = {
                                        viewModel.updateConversationSettings(mapOf("toolCodeExecution" to it))
                                    },
                                    imageGenEnabled = activeConversation.toolImageGeneration,
                                    onToggleImageGen = {
                                        viewModel.updateConversationSettings(mapOf("toolImageGeneration" to it))
                                    }
                                )

                                ChatComposer(
                                    onSendMessage = { prompt, bitmaps ->
                                        viewModel.sendMessage(prompt, bitmaps)
                                    },
                                    isStreaming = isStreaming,
                                    onStopStreaming = { viewModel.stopStreaming() }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (activeConversation == null) {
                            // Empty state
                            EmptyChatState(
                                onSelectPrompt = { prompt ->
                                    viewModel.startNewChat(prompt)
                                }
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Error / Quota Banner
                                if (uiState is ChatUiState.Error) {
                                    val err = uiState as ChatUiState.Error
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                            .testTag("error_banner")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = err.message,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { viewModel.clearError() }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Dismiss",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                // Messages List
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                ) {
                                    items(messages, key = { it.id }) { msg ->
                                        MessageBubble(
                                            message = msg,
                                            onEdit = { edited ->
                                                viewModel.editAndResend(edited, edited.content)
                                            },
                                            onRegenerate = { regen ->
                                                viewModel.regenerateMessage(regen)
                                            },
                                            onBranch = { branchMsg ->
                                                viewModel.branchIntoNewThread(branchMsg)
                                            },
                                            onDelete = { delMsg ->
                                                viewModel.deleteMessage(delMsg)
                                            }
                                        )
                                    }

                                    // Realtime streaming bubble placeholder
                                    if (isStreaming && streamingContent.isNotEmpty()) {
                                        item {
                                            MessageBubble(
                                                message = Message(
                                                    id = "streaming_current",
                                                    role = "model",
                                                    content = streamingContent,
                                                    characterCount = streamingContent.length.toLong(),
                                                    tokenCount = (streamingContent.length / 4L).coerceAtLeast(1L)
                                                ),
                                                isStreaming = true
                                            )
                                        }
                                    }

                                    // Loading indicator if waiting for first token
                                    if (uiState is ChatUiState.Loading) {
                                        item {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Gemini is thinking...",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right pane: Sources & Knowledge
            AnimatedVisibility(
                visible = showRightPanel,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                HorizontalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
                SourcesAndKnowledgePanel(
                    activeSources = activeSources,
                    activeProject = selectedProject,
                    onClose = { showRightPanel = false }
                )
            }
        }
    }

    // Model and reasoning parameter sheet
    if (showModelParamsSheet && activeConversation != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showModelParamsSheet = false }
        ) {
            ModelAndParamsSheet(
                conversation = activeConversation,
                onUpdateConversation = { updates ->
                    viewModel.updateConversationSettings(updates)
                },
                onDismiss = { showModelParamsSheet = false }
            )
        }
    }

    // Settings & Quotas Dialog
    if (showSettingsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSettingsDialog = false }
        ) {
            SettingsDialog(
                userProfile = userProfile,
                onSaveApiKey = { apiKey ->
                    viewModel.saveCustomApiKey(apiKey)
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
