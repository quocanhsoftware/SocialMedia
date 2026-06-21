package com.quocanh.socialmedia.model

import com.google.firebase.firestore.PropertyName

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0,
    
    @get:PropertyName("isUserDisabled")
    @set:PropertyName("isUserDisabled")
    var isUserDisabled: Boolean = false
)
