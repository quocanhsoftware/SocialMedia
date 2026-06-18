package com.quocanh.socialmedia.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quocanh.socialmedia.adapter.CommentAdapter
import com.quocanh.socialmedia.adapter.PostAdapter
import com.quocanh.socialmedia.controller.CommentController
import com.quocanh.socialmedia.controller.PostController
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Post

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userRole: String = "user",
    onLogout: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToChatList: () -> Unit,
    postController: PostController = viewModel(),
    commentController: CommentController = viewModel()
) {
    val posts by postController.posts
    val comments by commentController.comments
    val isLoading by postController.isLoading
    val isRefreshing by postController.isRefreshing
    
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var currentUserAvatar by remember { mutableStateOf("") }
    val currentUserId = FirebaseManager.getCurrentUser()?.uid

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            FirebaseManager.getUserInfo(uid) { user ->
                user?.let { currentUserAvatar = it.avatar }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                postController.searchPosts(it)
                            },
                            placeholder = { Text("Tìm kiếm bài viết...", fontSize = 16.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    } else {
                        Text(
                            "SocialMedia",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                },
                actions = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                            postController.searchPosts("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                        }
                        IconButton(onClick = onNavigateToChatList) {
                            Icon(Icons.Default.ChatBubble, contentDescription = "Tin nhắn")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (userRole == "admin") {
                                    DropdownMenuItem(
                                        text = { Text("Quản trị hệ thống") },
                                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToAdmin()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Đăng xuất") },
                                    onClick = {
                                        showMenu = false
                                        FirebaseManager.logout()
                                        onLogout()
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { postController.refreshPosts() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    item {
                        PostAdapter.CreatePostBar(
                            avatarUrl = currentUserAvatar,
                            onAvatarClick = { currentUserId?.let { onNavigateToProfile(it) } },
                            onClick = { 
                                editingPost = null
                                showCreatePostDialog = true 
                            }
                        )
                    }

                    item {
                        SecondaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { 
                                    selectedTab = 0
                                    postController.loadAllPosts()
                                },
                                text = { 
                                    Text(
                                        "Khám phá", 
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { 
                                    selectedTab = 1
                                    postController.loadFollowingPosts()
                                },
                                text = { 
                                    Text(
                                        "Đang theo dõi", 
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                }
                            )
                        }
                    }

                    if (isLoading && posts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
                            }
                        }
                    } else if (posts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    if (selectedTab == 1) "Bạn chưa theo dõi ai hoặc họ chưa có bài đăng nào" 
                                    else "Không tìm thấy kết quả nào", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            PostAdapter.PostItem(
                                post = post,
                                onLikeClick = { postController.toggleLike(post.id) },
                                onCommentClick = {
                                    selectedPostId = post.id
                                    commentController.loadComments(post.id)
                                    showCommentSheet = true
                                },
                                onUserClick = { userId -> onNavigateToProfile(userId) },
                                onEditClick = {
                                    editingPost = post
                                    showCreatePostDialog = true
                                },
                                onDeleteClick = { postController.deletePost(post.id) }
                            )
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
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
                        } else {
                            postController.createPost(content, imageUrl)
                        }
                        showCreatePostDialog = false
                        editingPost = null
                    }
                )
            }
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
                        postController.loadPosts()
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
                        postController.loadPosts()
                    }
                }
            }
        )
    }
}
