package com.quocanh.socialmedia.controller

import com.quocanh.socialmedia.firebase.FirebaseManager
import com.quocanh.socialmedia.model.User

class AuthController {
    fun registerUser(
        username: String,

        email: String,

        password: String,
        callback: (Boolean, String)-> Unit
    ){
        FirebaseManager.auth
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val uid = FirebaseManager.auth.currentUser!!.uid
                    val user = User(
                        uid = uid,
                        email = email,
                        avatar = "",
                        username = username
                    )
                    saveUsertoFirestore(user, callback)
                } else{
                        callback(false, task.exception?.message?:"Thêm user không thành công")
                }
            }
    }
    fun saveUsertoFirestore(
        user:User,
        callback: (Boolean, String) -> Unit
    ){
        FirebaseManager.firestore
            .collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                callback(true, "Register successly")
            }
            .addOnFailureListener {
                callback(false, it.message?:"Register error on firebase")
            }
    }
}