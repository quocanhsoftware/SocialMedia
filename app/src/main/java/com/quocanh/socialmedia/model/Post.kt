package com.quocanh.socialmedia.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val backgroundColor: Int = 0xFFFFFFFF.toInt(), // Thêm màu nền cho bài viết
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0
)
