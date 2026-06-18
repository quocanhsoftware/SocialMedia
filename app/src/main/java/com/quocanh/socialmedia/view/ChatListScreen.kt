package com.quocanh.socialmedia.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    var conversations by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var users by remember { mutableStateOf<Map<String, User>>(emptyMap()) }

    LaunchedEffect(Unit) {
        FirebaseManager.getConversations { convList ->
            conversations = convList
            val userIds = convList.mapNotNull { it["otherUserId"] as? String }
            FirebaseManager.getUsers(userIds) { userList ->
                users = userList.associateBy { it.uid }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tin nhắn",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
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
        ) {
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có cuộc trò chuyện nào",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations, key = { it["otherUserId"] as? String ?: "" }) { conv ->
                        val otherUserId = conv["otherUserId"] as? String ?: return@items
                        val user = users[otherUserId]
                        val lastMessage = conv["lastMessage"] as? String ?: ""

                        // Fallback if user not found yet, or just show a placeholder
                        if (user != null) {
                            ConversationItem(
                                user = user,
                                lastMessage = lastMessage,
                                onClick = { onNavigateToChat(otherUserId) }
                            )
                        } else {
                            // Show a placeholder while user data is loading
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Loading...", style = MaterialTheme.typography.titleMedium)
                                    Text(lastMessage, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Divider()
                            // Try fetching this user individually!
                            LaunchedEffect(otherUserId) {
                                FirebaseManager.getUserInfo(otherUserId) { fetchedUser ->
                                    fetchedUser?.let {
                                        users = users + (it.uid to it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    user: User,
    lastMessage: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = user.avatar),
            contentDescription = user.username,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
    Divider()
}
