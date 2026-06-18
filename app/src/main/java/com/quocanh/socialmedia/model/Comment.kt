package com.quocanh.socialmedia.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @field:JvmField
    val isUserDisabled: Boolean = false
)