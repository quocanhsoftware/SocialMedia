package com.quocanh.socialmedia.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Message
import com.quocanh.socialmedia.model.User
import com.quocanh.socialmedia.utils.CloudinaryManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    otherUserId: String,
    onBack: () -> Unit
) {
    val currentUserId = FirebaseManager.getCurrentUser()?.uid ?: return
    val conversationId = FirebaseManager.getConversationId(currentUserId, otherUserId)
    val context = LocalContext.current

    var otherUser by remember { mutableStateOf<User?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageInput by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isUploading = true
            CloudinaryManager.uploadMultipleImages(
                uris = uris,
                onAllSuccess = { urls ->
                    FirebaseManager.sendMediaMessage(
                        receiverId = otherUserId,
                        content = messageInput,
                        imageUrls = urls,
                        onResult = {
                            messageInput = ""
                            isUploading = false
                        }
                    )
                },
                onError = {
                    isUploading = false
                }
            )
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            val fileName = "file_${System.currentTimeMillis()}"
            CloudinaryManager.uploadFile(
                uri = it,
                fileName = fileName,
                onSuccess = { url, name ->
                    FirebaseManager.sendMediaMessage(
                        receiverId = otherUserId,
                        content = messageInput,
                        fileUrl = url,
                        fileName = name,
                        onResult = {
                            messageInput = ""
                            isUploading = false
                        }
                    )
                },
                onError = {
                    isUploading = false
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        FirebaseManager.getUserInfo(otherUserId) { user ->
            otherUser = user
        }
        FirebaseManager.getMessages(conversationId) { msgList ->
            messages = msgList
            if (msgList.isNotEmpty()) {
                coroutineScope.launch {
                    listState.animateScrollToItem(msgList.size - 1)
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = rememberAsyncImagePainter(model = otherUser?.avatar),
                            contentDescription = otherUser?.username,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = otherUser?.username ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image button
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isUploading,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Gửi ảnh")
                }
                Spacer(modifier = Modifier.width(4.dp))
                // File button
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    enabled = !isUploading,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Gửi file")
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                TextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Nhập tin nhắn...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    enabled = !isUploading
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                FirebaseManager.sendMessage(otherUserId, messageInput) {
                                    messageInput = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(if (message.imageUrls.isNotEmpty() || message.fileUrl != null) 4.dp else 12.dp)
            ) {
                // Show images if present
                if (message.imageUrls.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(message.imageUrls) { url ->
                            Image(
                                painter = rememberAsyncImagePainter(model = url),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                // Show file if present
                if (message.fileUrl != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(message.fileUrl))
                                context.startActivity(intent)
                            }
                    ) {
                        Icon(
                            Icons.Default.InsertDriveFile, 
                            contentDescription = null, 
                            tint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message.fileName ?: "File",
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Show text content if present
                if (message.content.isNotEmpty()) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(
                            start = if (message.imageUrls.isNotEmpty() || message.fileUrl != null) 4.dp else 0.dp,
                            top = if (message.imageUrls.isNotEmpty() || message.fileUrl != null) 8.dp else 0.dp,
                            end = if (message.imageUrls.isNotEmpty() || message.fileUrl != null) 4.dp else 0.dp,
                            bottom = if (message.imageUrls.isNotEmpty() || message.fileUrl != null) 0.dp else 0.dp
                        ),
                        color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
