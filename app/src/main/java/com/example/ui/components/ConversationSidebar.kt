package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.model.Project

@Composable
fun ConversationSidebar(
    conversations: List<Conversation>,
    selectedConversationId: String?,
    onSelectConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    projects: List<Project>,
    onSelectProject: (Project?) -> Unit,
    selectedProject: Project?,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    onRenameConversation: (Conversation, String) -> Unit,
    onPinConversation: (Conversation) -> Unit,
    onArchiveConversation: (Conversation) -> Unit,
    onDeleteConversation: (Conversation) -> Unit,
    onCreateProject: (String, String, String, String, String) -> Unit,
    userDisplayName: String,
    userEmail: String,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .testTag("conversation_sidebar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            // New Chat button
            Button(
                onClick = onNewChat,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_chat_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search history filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("search_history_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Projects selector section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "PROJECTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { showNewProjectDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Project", modifier = Modifier.size(16.dp))
                }
            }

            // Project pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedProject == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onSelectProject(null) }
                ) {
                    Text(
                        text = "All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedProject == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                projects.take(3).forEach { proj ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedProject?.id == proj.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onSelectProject(proj) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = proj.name,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "CONVERSATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Filtered conversation list
            val filteredConversations = conversations
                .filter { !it.isArchived }
                .filter { selectedProject == null || it.projectId == selectedProject.id }
                .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.updatedAt })

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                items(filteredConversations, key = { it.id }) { conv ->
                    val isSelected = conv.id == selectedConversationId
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectConversation(conv) }
                            .testTag("conversation_item_${conv.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (conv.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (conv.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = conv.title,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            // Quick item actions
                            IconButton(
                                onClick = { onPinConversation(conv) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pin conversation",
                                    tint = if (conv.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    conversationToRename = conv
                                    renameInput = conv.title
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteConversation(conv) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // User Info & Bottom Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userDisplayName.ifBlank { "Google User" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = userEmail,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("settings_button")) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = onSignOut, modifier = Modifier.testTag("signout_button")) {
                    Icon(Icons.Default.Logout, contentDescription = "Sign Out", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    // Rename dialog
    if (conversationToRename != null) {
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("Conversation Title") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = conversationToRename
                        if (target != null && renameInput.isNotBlank()) {
                            onRenameConversation(target, renameInput.trim())
                        }
                        conversationToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        var projName by remember { mutableStateOf("") }
        var projDesc by remember { mutableStateOf("") }
        var projInstructions by remember { mutableStateOf("") }
        var projKnowledge by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create New Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = projName,
                        onValueChange = { projName = it },
                        label = { Text("Project Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = projDesc,
                        onValueChange = { projDesc = it },
                        label = { Text("Description") },
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = projInstructions,
                        onValueChange = { projInstructions = it },
                        label = { Text("Custom System Instructions") },
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = projKnowledge,
                        onValueChange = { projKnowledge = it },
                        label = { Text("Project Knowledge Context") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (projName.isNotBlank()) {
                            onCreateProject(
                                projName.trim(),
                                projDesc.trim(),
                                projInstructions.trim(),
                                "gemini-3.8-flash",
                                projKnowledge.trim()
                            )
                            showNewProjectDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
