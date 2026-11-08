package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.ImagePickerHelper

@Composable
fun ChatComposer(
    onSendMessage: (String, List<Bitmap>) -> Unit,
    isStreaming: Boolean,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val attachedBitmaps = remember { mutableStateListOf<Bitmap>() }
    val context = LocalContext.current

    // Photo picker launcher (0-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        for (uri in uris) {
            val bmp = ImagePickerHelper.loadBitmapFromUri(context, uri)
            if (bmp != null) {
                attachedBitmaps.add(bmp)
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            attachedBitmaps.add(bmp)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Thumbnail preview row with remove buttons
        AnimatedVisibility(visible = attachedBitmaps.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                itemsIndexed(attachedBitmaps) { index, bmp ->
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(64.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attachment preview",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                        ) {
                            IconButton(
                                onClick = { attachedBitmaps.removeAt(index) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove attachment",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Multimodal Action buttons
            IconButton(
                onClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("attach_media_button")
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = "Attach photos or files",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { cameraLauncher.launch() },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("camera_capture_button")
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = "Camera capture",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Main input field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Gemini anything...", fontSize = 14.sp) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_textfield")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send or Stop button
            if (isStreaming) {
                IconButton(
                    onClick = onStopStreaming,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("stop_streaming_button")
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop streaming response",
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        val trimmed = textInput.trim()
                        if (trimmed.isNotBlank() || attachedBitmaps.isNotEmpty()) {
                            onSendMessage(trimmed, attachedBitmaps.toList())
                            textInput = ""
                            attachedBitmaps.clear()
                        }
                    },
                    enabled = textInput.isNotBlank() || attachedBitmaps.isNotEmpty(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (textInput.isNotBlank() || attachedBitmaps.isNotEmpty())
                            MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
