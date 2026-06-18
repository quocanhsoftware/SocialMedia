package com.quocanh.socialmedia.controller

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quocanh.socialmedia.data.AdminRepository
import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.model.User
import kotlinx.coroutines.launch

class AdminController : ViewModel() {
    private val repository = AdminRepository()

    private val _users = mutableStateOf<List<User>>(emptyList())
    val users: State<List<User>> = _users

    private val _posts = mutableStateOf<List<Post>>(emptyList())
    val posts: State<List<Post>> = _posts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val currentAdminUid: String?
        get() = FirebaseManager.getCurrentUser()?.uid

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _users.value = repository.getAllUsers(currentAdminUid)
            _isLoading.value = false
        }
    }

    fun loadAllPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _posts.value = repository.getAllPosts()
            _isLoading.value = false
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadAllUsers()
            } else {
                _isLoading.value = true
                _users.value = repository.searchUsers(query, currentAdminUid)
                _isLoading.value = false
            }
        }
    }

    fun searchPosts(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadAllPosts()
            } else {
                _isLoading.value = true
                _posts.value = repository.searchPosts(query)
                _isLoading.value = false
            }
        }
    }

    fun toggleUserDisabled(uid: String, isDisabled: Boolean) {
        viewModelScope.launch {
            if (repository.toggleUserDisabled(uid, isDisabled)) {
                loadAllUsers()
                loadAllPosts() // Cập nhật lại bài viết nếu người dùng bị ẩn nội dung
            }
        }
    }

    fun deleteUser(uid: String) {
        viewModelScope.launch {
            if (repository.deleteUser(uid)) {
                loadAllUsers()
                loadAllPosts() // Cập nhật lại bài viết
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            if (repository.deletePost(postId)) {
                loadAllPosts()
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            if (repository.deleteComment(postId, commentId)) {
                loadAllPosts()
            }
        }
    }
}
