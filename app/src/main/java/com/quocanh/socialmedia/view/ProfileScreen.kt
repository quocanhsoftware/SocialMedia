package com.quocanh.socialmedia.view

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Transgender
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.quocanh.socialmedia.adapter.CommentAdapter
import com.quocanh.socialmedia.adapter.PostAdapter
import com.quocanh.socialmedia.adapter.UserAdapter
import com.quocanh.socialmedia.controller.CommentController
import com.quocanh.socialmedia.controller.PostController
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.model.User
import com.quocanh.socialmedia.utils.CloudinaryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    postController: PostController = viewModel(),
    commentController: CommentController = viewModel(),
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val userPosts by postController.userPosts
    val isLoading by postController.isLoading
    
    val comments by commentController.comments
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Post?>(null) }

    var showUserListSheet by remember { mutableStateOf(false) }
    var userListTitle by remember { mutableStateOf("") }
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    val currentUid = FirebaseManager.getCurrentUser()?.uid

    fun refreshData() {
        FirebaseManager.getUserInfo(userId) { fetchedUser ->
            user = fetchedUser
        }
        currentUid?.let {
            FirebaseManager.getUserInfo(it) { fetchedCurrentUser ->
                currentUser = fetchedCurrentUser
            }
        }
        postController.loadPostsByUser(userId)
    }

    LaunchedEffect(userId) {
        refreshData()
    }

    val isFollowing = currentUser?.following?.contains(userId) == true
    val isMe = currentUid == userId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.username ?: "Trang cá nhân", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Header Background (Gradients)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)
                                )
                            )
                    )
                    
                    // Avatar (shifted up)
                    Box(
                        modifier = Modifier
                            .offset(y = (-50).dp)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user?.avatar?.isNotEmpty() == true) {
                            AsyncImage(
                                model = user?.avatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .offset(y = (-40).dp)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user?.username ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isMe) {
                            Button(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.fillMaxWidth(0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chỉnh sửa")
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Button(
                                    onClick = {
                                        if (isFollowing) {
                                            FirebaseManager.unfollowUser(userId) { success -> if (success) refreshData() }
                                        } else {
                                            FirebaseManager.followUser(userId) { success -> if (success) refreshData() }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(if (isFollowing) "Đang theo dõi" else "Theo dõi")
                                }
                                Button(
                                    onClick = { onNavigateToChat(userId) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nhắn tin")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "Đang theo dõi",
                                count = user?.following?.size ?: 0,
                                onClick = {
                                    user?.following?.let { uids ->
                                        userListTitle = "Đang theo dõi"
                                        FirebaseManager.getUsers(uids) { users ->
                                            userList = users
                                            showUserListSheet = true
                                        }
                                    }
                                }
                            )
                            StatItem(
                                label = "Người theo dõi",
                                count = user?.followers?.size ?: 0,
                                onClick = {
                                    user?.followers?.let { uids ->
                                        userListTitle = "Người theo dõi"
                                        FirebaseManager.getUsers(uids) { users ->
                                            userList = users
                                            showUserListSheet = true
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // User Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                InfoRow(icon = Icons.Default.Transgender, label = "Giới tính", value = user?.gender)
                                InfoRow(icon = Icons.Default.Cake, label = "Ngày sinh", value = user?.birthday)
                                InfoRow(icon = Icons.Default.Home, label = "Đến từ", value = user?.hometown)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Bài viết",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isLoading && userPosts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (userPosts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có bài đăng nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(userPosts) { post ->
                    PostAdapter.PostItem(
                        post = post,
                        onLikeClick = { postController.toggleLike(post.id) },
                        onCommentClick = {
                            selectedPostId = post.id
                            commentController.loadComments(post.id)
                            showCommentSheet = true
                        },
                        onUserClick = { clickedUserId ->
                            if (clickedUserId != userId) {
                                onNavigateToProfile(clickedUserId)
                            }
                        },
                        onEditClick = if (isMe) { {
                            editingPost = post
                            showCreatePostDialog = true
                        } } else null,
                        onDeleteClick = if (isMe) { { postController.deletePost(post.id) } } else null
                    )
                }
            }
        }

        if (showCreatePostDialog) {
            PostActionDialog(
                post = editingPost,
                onDismiss = {
                    showCreatePostDialog = false
                    editingPost = null
                },
                onConfirm = { content, imageUrl ->
                    if (editingPost != null) {
                        postController.updatePost(editingPost!!.id, content, imageUrl)
                    }
                    showCreatePostDialog = false
                    editingPost = null
                }
            )
        }

        if (showEditProfileDialog && user != null) {
            EditProfileDialog(
                user = user!!,
                onDismiss = { showEditProfileDialog = false },
                onConfirm = { updatedUser ->
                    FirebaseManager.updateUserInfo(updatedUser) { success, message ->
                        if (success) {
                            refreshData()
                            showEditProfileDialog = false
                        }
                    }
                }
            )
        }

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
                        postController.loadPostsByUser(userId)
                    }
                }
            },
            onUpdateComment = { commentId, newContent ->
                selectedPostId?.let { postId ->
                    commentController.updateComment(postId, commentId, newContent)
                }
            },
            onDeleteComment = { commentId ->
                selectedPostId?.let { postId ->
                    commentController.deleteComment(postId, commentId) {
                        postController.loadPostsByUser(userId)
                    }
                }
            }
        )

        UserAdapter.UserListSheet(
            isOpen = showUserListSheet,
            title = userListTitle,
            users = userList,
            onDismiss = { showUserListSheet = false },
            onUserClick = onNavigateToProfile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (User) -> Unit
) {
    var username by remember { mutableStateOf(user.username) }
    var avatar by remember { mutableStateOf(user.avatar) }
    var gender by remember { mutableStateOf(user.gender) }
    var birthday by remember { mutableStateOf(user.birthday) }
    var hometown by remember { mutableStateOf(user.hometown) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            CloudinaryManager.uploadImage(it, 
                onSuccess = { url ->
                    avatar = url
                    isUploading = false
                },
                onError = {
                    isUploading = false
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa thông tin") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .align(Alignment.CenterHorizontally)
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar.isNotEmpty()) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape).padding(4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Tên người dùng") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text("Giới tính") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text("Ngày sinh") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hometown,
                    onValueChange = { hometown = it },
                    label = { Text("Đến từ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(user.copy(
                        username = username,
                        avatar = avatar,
                        gender = gender,
                        birthday = birthday,
                        hometown = hometown
                    ))
                },
                enabled = !isUploading
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun StatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(text = "$count", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "$label: ", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
