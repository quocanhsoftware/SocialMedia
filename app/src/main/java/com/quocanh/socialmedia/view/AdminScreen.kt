package com.quocanh.socialmedia.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.quocanh.socialmedia.controller.AdminController
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    adminController: AdminController = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        adminController.loadAllUsers()
        adminController.loadAllPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản trị hệ thống", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (selectedTab == 0) adminController.searchUsers(it)
                    else adminController.searchPosts(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(if (selectedTab == 0) "Tìm người dùng..." else "Tìm bài viết...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Người dùng") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bài viết") }
                )
            }

            if (adminController.isLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> UserManagementList(
                        users = adminController.users.value,
                        onToggleDisable = { uid, isDis -> adminController.toggleUserDisabled(uid, isDis) },
                        onDelete = { uid -> adminController.deleteUser(uid) }
                    )
                    1 -> PostManagementList(
                        posts = adminController.posts.value,
                        onDeletePost = { postId -> adminController.deletePost(postId) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserManagementList(
    users: List<User>,
    onToggleDisable: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(users) { user ->
            UserAdminItem(user, onToggleDisable, onDelete)
        }
    }
}

@Composable
fun UserAdminItem(
    user: User,
    onToggleDisable: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatar.ifEmpty { "https://via.placeholder.com/150" },
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
                Text("Role: ${user.role}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            
            Row {
                IconButton(onClick = { onToggleDisable(user.uid, !user.isDisabled) }) {
                    Icon(
                        imageVector = if (user.isDisabled) Icons.Default.LockOpen else Icons.Default.Block,
                        contentDescription = "Vô hiệu hóa",
                        tint = if (user.isDisabled) Color.Green else Color(0xFFFFA500)
                    )
                }
                IconButton(onClick = { onDelete(user.uid) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun PostManagementList(
    posts: List<Post>,
    onDeletePost: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(posts) { post ->
            PostAdminItem(post, onDeletePost)
        }
    }
}

@Composable
fun PostAdminItem(
    post: Post,
    onDeletePost: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = post.userAvatar,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(post.username, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onDeletePost(post.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa bài viết", tint = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(post.content)
            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = post.imageUrls[0],
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                if (post.imageUrls.size > 1) {
                    Text(
                        text = "+${post.imageUrls.size - 1} ảnh khác",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
