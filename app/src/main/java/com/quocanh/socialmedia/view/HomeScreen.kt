package com.quocanh.socialmedia.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quocanh.socialmedia.adapter.CommentAdapter
import com.quocanh.socialmedia.adapter.PostAdapter
import com.quocanh.socialmedia.controller.CommentController
import com.quocanh.socialmedia.controller.PostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    postController: PostController = viewModel(),
    commentController: CommentController = viewModel(),
    onLogout: () -> Unit
) {
    val posts by postController.posts
    val comments by commentController.comments
    val isLoading by postController.isLoading
    
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    // Dùng Box làm root để CommentSheet có thể đè lên toàn màn hình bao gồm cả TopBar
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SocialMedia", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5))
                ) {
                    item {
                        PostAdapter.CreatePostBar(onClick = { showCreatePostDialog = true })
                    }

                    if (isLoading && posts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    } else {
                        items(posts) { post ->
                            PostAdapter.PostItem(
                                post = post,
                                onLikeClick = { postController.toggleLike(post.id) },
                                onCommentClick = {
                                    selectedPostId = post.id
                                    commentController.loadComments(post.id)
                                    showCommentSheet = true
                                }
                            )
                        }
                    }
                }

                // Dialog tạo bài viết
                if (showCreatePostDialog) {
                    CreatePostDialog(
                        onDismiss = { showCreatePostDialog = false },
                        onPostCreated = { content ->
                            postController.createPost(content)
                            showCreatePostDialog = false
                        }
                    )
                }
            }
        }

        // CommentSheet đặt ở đây để đè lên toàn bộ Scaffold
        // Điều này giúp imePadding() hoạt động hiệu quả nhất
        CommentAdapter.CommentSheet(
            isOpen = showCommentSheet && selectedPostId != null,
            comments = comments,
            onDismiss = { 
                showCommentSheet = false
                selectedPostId = null
            },
            onSendComment = { content ->
                selectedPostId?.let { postId ->
                    commentController.addComment(postId, content) {
                        postController.loadPosts()
                    }
                }
            }
        )
    }
}

@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPostCreated: (String) -> Unit
) {
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo bài viết") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Bạn đang nghĩ gì?") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onPostCreated(content) },
                enabled = content.isNotBlank()
            ) {
                Text("Đăng")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
