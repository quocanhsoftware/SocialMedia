package com.quocanh.socialmedia.controller

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quocanh.socialmedia.data.PostRepository
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.model.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostController : ViewModel() {
    private val repository = PostRepository()

    private val _posts = mutableStateOf<List<Post>>(emptyList())
    val posts: State<List<Post>> = _posts

    private val _searchedUsers = mutableStateOf<List<User>>(emptyList())
    val searchedUsers: State<List<User>> = _searchedUsers

    private val _userPosts = mutableStateOf<List<Post>>(emptyList())
    val userPosts: State<List<Post>> = _userPosts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    private var allPosts = emptyList<Post>()
    private var currentFeedType = "explore" // "explore" or "following"

    private val postColors = listOf(
        0xFFE8F5E9.toInt(), 0xFFE3F2FD.toInt(), 0xFFFFF3E0.toInt(),
        0xFFF3E5F5.toInt(), 0xFFFCE4EC.toInt(), 0xFFEFEBE9.toInt()
    )

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            fetchData()
            _isLoading.value = false
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchData()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchData() {
        if (currentFeedType == "explore") {
            allPosts = repository.getPosts()
            _posts.value = allPosts
        } else {
            val currentUserId = FirebaseManager.getCurrentUser()?.uid
            if (currentUserId != null) {
                try {
                    val userDoc = FirebaseManager.firestore.collection("users").document(currentUserId).get().await()
                    val following = userDoc.get("following") as? List<String> ?: emptyList()
                    _posts.value = repository.getFollowingPosts(following)
                } catch (e: Exception) {
                    _posts.value = emptyList()
                }
            }
        }
    }

    fun loadAllPosts() {
        currentFeedType = "explore"
        loadPosts()
    }

    fun loadFollowingPosts() {
        currentFeedType = "following"
        loadPosts()
    }

    fun searchPosts(query: String) {
        if (query.isEmpty()) {
            _posts.value = allPosts
            _searchedUsers.value = emptyList()
        } else {
            // Tìm kiếm bài viết từ dữ liệu local (hoặc fetch thêm nếu cần)
            _posts.value = allPosts.filter { 
                it.content.contains(query, ignoreCase = true) || 
                it.username.contains(query, ignoreCase = true) 
            }
            // Tìm kiếm người dùng từ Firebase
            FirebaseManager.searchUsers(query) { users ->
                _searchedUsers.value = users
            }
        }
    }

    fun loadPostsByUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _userPosts.value = repository.getPostsByUser(userId)
            _isLoading.value = false
        }
    }

    fun createPost(content: String, imageUrls: List<String> = emptyList()) {
        val currentUser = FirebaseManager.getCurrentUser() ?: return
        val randomColor = postColors.random()

        FirebaseManager.firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val usernameFromDb = document.getString("username") ?: "Người dùng"
                val userAvatar = document.getString("avatar") ?: ""
                val newPost = Post(
                    userId = currentUser.uid,
                    username = usernameFromDb,
                    userAvatar = userAvatar,
                    content = content,
                    imageUrls = imageUrls,
                    backgroundColor = randomColor
                )
                viewModelScope.launch {
                    if (repository.createPost(newPost)) loadPosts()
                }
            }
    }

    fun updatePost(postId: String, content: String, imageUrls: List<String>) {
        viewModelScope.launch {
            if (repository.updatePost(postId, content, imageUrls)) {
                loadPosts()
                val currentUserId = FirebaseManager.getCurrentUser()?.uid
                if (currentUserId != null) loadPostsByUser(currentUserId)
            }
        }
    }

    fun deletePost(postId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.deletePost(postId)
            if (success) {
                loadPosts()
                val currentUserId = FirebaseManager.getCurrentUser()?.uid
                if (currentUserId != null) loadPostsByUser(currentUserId)
            }
            onResult(success)
        }
    }

    fun toggleLike(postId: String) {
        val currentUser = FirebaseManager.getCurrentUser() ?: return
        viewModelScope.launch {
            if (repository.toggleLike(postId, currentUser.uid)) {
                loadPosts()
            }
        }
    }
}
