package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile

@Composable
fun SettingsDialog(
    userProfile: UserProfile?,
    onSaveApiKey: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember(userProfile?.customApiKey) {
        mutableStateOf(userProfile?.customApiKey.orEmpty())
    }
    var apiKeySavedFeedback by remember { mutableStateOf(false) }

    val dailyCap = 50L
    val currentUsage = userProfile?.dailyMessageCount ?: 0L
    val usageRatio = (currentUsage.toFloat() / dailyCap).coerceIn(0f, 1f)

    // Calculate approximate reset time (midnight UTC)
    val nowMillis = System.currentTimeMillis()
    val millisInDay = 24 * 60 * 60 * 1000L
    val remainingMillis = millisInDay - (nowMillis % millisInDay)
    val remainingHours = remainingMillis / (60 * 60 * 1000L)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Settings & Preferences",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Switcher & Material 3 Dynamic Color
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("theme_switcher_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "APPEARANCE & THEME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.theme.ThemeMode.values().forEach { mode ->
                            val isSelected = com.example.ui.theme.ThemeManager.themeMode == mode
                            androidx.compose.material3.FilterChip(
                                selected = isSelected,
                                onClick = { com.example.ui.theme.ThemeManager.setTheme(context, mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            com.example.ui.theme.ThemeMode.SYSTEM -> "System"
                                            com.example.ui.theme.ThemeMode.LIGHT -> "Light"
                                            com.example.ui.theme.ThemeMode.DARK -> "Dark"
                                        },
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        when (mode) {
                                            com.example.ui.theme.ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                            com.example.ui.theme.ThemeMode.LIGHT -> Icons.Default.LightMode
                                            com.example.ui.theme.ThemeMode.DARK -> Icons.Default.DarkMode
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Material 3 Dynamic Color Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Material 3 Dynamic Color",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Derive vibrant theme tones from system wallpaper (Android 12+)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = com.example.ui.theme.ThemeManager.dynamicColorEnabled,
                            onCheckedChange = { com.example.ui.theme.ThemeManager.toggleDynamicColor(context, it) },
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Free Tier Quota Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Message Quota (Free Tier)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!userProfile?.customApiKey.isNullOrBlank()) {
                        Text(
                            text = "Unlimited Usage Enabled via Custom API Key",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { usageRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (usageRatio > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currentUsage / $dailyCap messages used",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Resets in ~${remainingHours}h",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // "Use My Own API Key" Section
            Text(
                text = "BRING YOUR OWN KEY (UNLIMITED USAGE)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Store your personal Gemini API key server-side in Firestore to bypass daily rate limits and bill calls directly to your account.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    apiKeySavedFeedback = false
                },
                placeholder = { Text("Paste AIzaSy... Gemini API Key") },
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_api_key_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val trimmed = apiKeyInput.trim()
                        onSaveApiKey(trimmed.ifBlank { null })
                        apiKeySavedFeedback = true
                    },
                    modifier = Modifier.testTag("save_api_key_button")
                ) {
                    Text("Save Key")
                }
                if (userProfile?.customApiKey != null) {
                    OutlinedButton(
                        onClick = {
                            apiKeyInput = ""
                            onSaveApiKey(null)
                            apiKeySavedFeedback = false
                        }
                    ) {
                        Text("Remove Key")
                    }
                }
                if (apiKeySavedFeedback) {
                    Text(
                        text = "Saved!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Google Workspace Connectors Status
            Text(
                text = "WORKSPACE CONNECTORS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Integrate with Google Workspace to read calendar events, emails, documents, and sheets.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            WorkspaceConnectorRow(
                name = "Google Calendar",
                icon = Icons.Default.CalendarMonth,
                status = "Connected via Google Sign-In"
            )
            WorkspaceConnectorRow(
                name = "Google Drive & Docs",
                icon = Icons.Default.Description,
                status = "Connected via Google Sign-In"
            )
            WorkspaceConnectorRow(
                name = "Gmail",
                icon = Icons.Default.Mail,
                status = "Connected via Google Sign-In"
            )
            WorkspaceConnectorRow(
                name = "Google Sheets",
                icon = Icons.Default.TableChart,
                status = "Connected via Google Sign-In"
            )
        }
    }
}

@Composable
fun WorkspaceConnectorRow(
    name: String,
    icon: ImageVector,
    status: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(text = status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = "Active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
