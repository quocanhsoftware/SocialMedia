package com.quocanh.socialmedia.adapter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Comment

object CommentAdapter {

    @Composable
    fun CommentSheet(
        isOpen: Boolean,
        comments: List<Comment>,
        onDismiss: () -> Unit,
        onSendComment: (String) -> Unit,
        onUpdateComment: (String, String) -> Unit = { _, _ -> },
        onDeleteComment: (String) -> Unit = {}
    ) {
        var editingCommentId by remember { mutableStateOf<String?>(null) }
        var editingText by remember { mutableStateOf("") }

        if (isOpen) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                // Dimmed background area to allow clicking outside to dismiss
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Main Sheet Content
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f) // Take 85% of screen height to be "lowered"
                            .clickable(enabled = false) { }, // Prevent clicks from dismissing when touching inside
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                                .imePadding()
                        ) {
                            // Drag Handle Indicator
                            Box(
                                modifier = Modifier
                                    .size(40.dp, 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                    .align(Alignment.CenterHorizontally)
                            )

                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                                Text(
                                    text = "Bình luận",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (comments.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${comments.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                            }

                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                            // Comments List
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (comments.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillParentMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = MaterialTheme.colorScheme.outlineVariant
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    "Chưa có bình luận nào",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(comments, key = { it.id }) { comment ->
                                        CommentItem(
                                            comment = comment,
                                            onEdit = {
                                                editingCommentId = comment.id
                                                editingText = comment.content
                                            },
                                            onDelete = { onDeleteComment(comment.id) }
                                        )
                                    }
                                }
                            }

                            // Input area
                            Surface(
                                tonalElevation = 3.dp,
                                shadowElevation = 10.dp
                            ) {
                                Column(modifier = Modifier.navigationBarsPadding()) {
                                    if (editingCommentId != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sửa bình luận", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(onClick = { editingCommentId = null }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Hủy", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    var commentText by remember { mutableStateOf("") }
                                    val textToDisplay = if (editingCommentId != null) editingText else commentText

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 29.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        OutlinedTextField(
                                            value = textToDisplay,
                                            onValueChange = {
                                                if (editingCommentId != null) editingText = it else commentText = it
                                            },
                                            placeholder = { Text("Viết bình luận...") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(24.dp),
                                            maxLines = 4,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))

                                        FloatingActionButton(
                                            onClick = {
                                                if (editingCommentId != null) {
                                                    if (editingText.isNotBlank()) {
                                                        onUpdateComment(editingCommentId!!, editingText)
                                                        editingCommentId = null
                                                    }
                                                } else {
                                                    if (commentText.isNotBlank()) {
                                                        onSendComment(commentText)
                                                        commentText = ""
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = "Gửi", modifier = Modifier.size(20.dp))
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
    private fun CommentItem(
        comment: Comment,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val currentUserId = FirebaseManager.getCurrentUser()?.uid
        val isOwnComment = currentUserId == comment.userId

        var userAvatar by remember { mutableStateOf(comment.userAvatar) }
        LaunchedEffect(comment.userId) {
            FirebaseManager.getUserInfo(comment.userId) { user ->
                user?.let { userAvatar = it.avatar }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatar.isNotEmpty()) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = comment.username, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = comment.content, 
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(comment.timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    
                    if (isOwnComment) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Sửa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onEdit() }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Xóa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onDelete() }
                        )
                    }
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "Vừa xong"
        }
    }
}
