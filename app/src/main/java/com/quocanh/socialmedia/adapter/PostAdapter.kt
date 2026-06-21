package com.quocanh.socialmedia.adapter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Post

object PostAdapter {

    @Composable
    fun CreatePostBar(
        avatarUrl: String,
        onAvatarClick: () -> Unit,
        onClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { onClick() },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Bạn đang nghĩ gì?",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun PostItem(
        post: Post,
        onLikeClick: () -> Unit,
        onCommentClick: () -> Unit,
        onUserClick: (String) -> Unit,
        onEditClick: (() -> Unit)? = null,
        onDeleteClick: (() -> Unit)? = null
    ) {
        val currentUserId = FirebaseManager.getCurrentUser()?.uid
        val isLiked = currentUserId?.let { post.likes.contains(it) } ?: false
        val isOwnPost = currentUserId == post.userId
        
        var userAvatar by remember { mutableStateOf("") }
        var showMenu by remember { mutableStateOf(false) }

        LaunchedEffect(post.userId) {
            FirebaseManager.getUserInfo(post.userId) { user ->
                user?.let { userAvatar = it.avatar }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.5.dp
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onUserClick(post.userId) },
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
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).clickable { onUserClick(post.userId) }) {
                        Text(
                            text = post.username, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatTimestamp(post.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isOwnPost) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (onEditClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Chỉnh sửa bài viết") },
                                        onClick = {
                                            showMenu = false
                                            onEditClick()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                }
                                if (onDeleteClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Xóa bài viết", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenu = false
                                            onDeleteClick()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Content
                if (post.content.isNotEmpty()) {
                    val isTextOnly = post.imageUrls.isEmpty()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (isTextOnly) 150.dp else 0.dp)
                            .background(if (isTextOnly) Color(post.backgroundColor).copy(alpha = 0.9f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = if (isTextOnly) 24.dp else 8.dp),
                        contentAlignment = if (isTextOnly) Alignment.Center else Alignment.TopStart
                    ) {
                        Text(
                            text = post.content,
                            fontSize = if (isTextOnly) 20.sp else 15.sp,
                            fontWeight = if (isTextOnly) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTextOnly) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = if (isTextOnly) TextAlign.Center else TextAlign.Start,
                            lineHeight = if (isTextOnly) 28.sp else 20.sp
                        )
                    }
                }

                // Images
                if (post.imageUrls.isNotEmpty()) {
                    if (post.imageUrls.size == 1) {
                        AsyncImage(
                            model = post.imageUrls[0],
                            contentDescription = "Post image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 500.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(post.imageUrls) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Post image",
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(300.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Stats
                if (post.likes.isNotEmpty() || post.commentCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (post.likes.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.ThumbUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${post.likes.size}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (post.commentCount > 0) {
                            Text(
                                text = "${post.commentCount} bình luận",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ActionButton(
                        icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        text = "Thích",
                        color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onLikeClick
                    )
                    ActionButton(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        text = "Bình luận",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onCommentClick
                    )
                }
            }
        }
    }

    @Composable
    private fun RowScope.ActionButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        text: String,
        color: Color,
        onClick: () -> Unit
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(contentColor = color)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
            days > 0 -> "$days ngày trước"
            hours > 0 -> "$hours giờ trước"
            minutes > 0 -> "$minutes phút trước"
            seconds > 30 -> "$seconds giây trước"
            else -> "Vừa xong"
        }
    }
}
