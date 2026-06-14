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

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

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
            _posts.value = repository.getPosts()
            _isLoading.value = false
        }
    }

    fun createPost(content: String, imageUrl: String = "") {
        val currentUser = FirebaseManager.getCurrentUser() ?: return
        val randomColor = postColors.random()

        FirebaseManager.firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val usernameFromDb = document.getString("username") ?: "Người dùng"
                val newPost = Post(
                    userId = currentUser.uid,
                    username = usernameFromDb,
                    content = content,
                    imageUrl = imageUrl,
                    backgroundColor = randomColor
                )
                viewModelScope.launch {
                    if (repository.createPost(newPost)) loadPosts()
                }
            }
    }

    fun toggleLike(postId: String) {
        val currentUser = FirebaseManager.getCurrentUser() ?: return
        viewModelScope.launch {
            if (repository.toggleLike(postId, currentUser.uid)) loadPosts()
        }
    }
}
