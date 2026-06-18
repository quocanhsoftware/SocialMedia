package com.quocanh.socialmedia.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val avatar: String = "",
    val gender: String = "",
    val birthday: String = "",
    val hometown: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList()
)