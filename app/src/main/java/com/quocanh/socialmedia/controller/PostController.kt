package com.quocanh.socialmedia.controller

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quocanh.socialmedia.data.PostRepository
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Post
import kotlinx.coroutines.launch

class PostController : ViewModel() {
    private val repository = PostRepository()

    private val _posts = mutableStateOf<List<Post>>(emptyList())
    val posts: State<List<Post>> = _posts

    private val _userPosts = mutableStateOf<List<Post>>(emptyList())
    val userPosts: State<List<Post>> = _userPosts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

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
        if (currentFeedType == "explore") {
            loadAllPosts()
        } else {
            loadFollowingPosts()
        }
    }

    fun loadAllPosts() {
        currentFeedType = "explore"
        viewModelScope.launch {
            _isLoading.value = true
            allPosts = repository.getPosts()
            _posts.value = allPosts
            _isLoading.value = false
        }
    }

    fun loadFollowingPosts() {
        currentFeedType = "following"
        val currentUserId = FirebaseManager.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            FirebaseManager.getUserInfo(currentUserId) { user ->
                val following = user?.following ?: emptyList()
                viewModelScope.launch {
                    _posts.value = repository.getFollowingPosts(following)
                    _isLoading.value = false
                }
            }
        }
    }

    fun searchPosts(query: String) {
        if (query.isEmpty()) {
            _posts.value = allPosts
        } else {
            _posts.value = allPosts.filter { 
                it.content.contains(query, ignoreCase = true) || 
                it.username.contains(query, ignoreCase = true) 
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

    fun createPost(content: String, imageUrl: String = "") {
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
                    imageUrl = imageUrl,
                    backgroundColor = randomColor
                )
                viewModelScope.launch {
                    if (repository.createPost(newPost)) loadPosts()
                }
            }
    }

    fun updatePost(postId: String, content: String, imageUrl: String) {
        viewModelScope.launch {
            if (repository.updatePost(postId, content, imageUrl)) {
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
