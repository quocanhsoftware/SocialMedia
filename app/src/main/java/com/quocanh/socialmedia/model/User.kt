package com.quocanh.socialmedia.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val avatar: String = "",
    val gender: String = "",
    val birthday: String = "",
    val hometown: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val role: String = "user", // "user" or "admin"
    
    @get:PropertyName("isDisabled")
    @set:PropertyName("isDisabled")
    var isDisabled: Boolean = false
)