package com.quocanh.socialmedia.controller

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quocanh.socialmedia.data.CommentRepository
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Comment
import kotlinx.coroutines.launch

class CommentController : ViewModel() {
    private val repository = CommentRepository()

    private val _comments = mutableStateOf<List<Comment>>(emptyList())
    val comments: State<List<Comment>> = _comments

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _comments.value = repository.getComments(postId)
            _isLoading.value = false
        }
    }

    fun addComment(postId: String, content: String, onComplete: () -> Unit) {
        val currentUser = FirebaseManager.getCurrentUser() ?: return
        
        FirebaseManager.firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val usernameFromDb = document.getString("username") ?: "Người dùng"
                val comment = Comment(
                    postId = postId,
                    userId = currentUser.uid,
                    username = usernameFromDb,
                    content = content
                )
                viewModelScope.launch {
                    if (repository.addComment(postId, comment)) {
                        loadComments(postId)
                        onComplete()
                    }
                }
            }
    }
}
