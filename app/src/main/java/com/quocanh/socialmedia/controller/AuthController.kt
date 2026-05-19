package com.quocanh.socialmedia.controller

import com.google.firebase.auth.FirebaseUser
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

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        if (email.isEmpty() || password.isEmpty()) {
            onResult(false, "Vui lòng nhập đầy đủ email và mật khẩu")
            return
        }

        FirebaseManager.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    onResult(true, "Đăng nhập thành công")
                } else {
                    onResult(
                        false,
                        task.exception?.localizedMessage ?: "Đăng nhập thất bại"
                    )
                }
            }
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseManager.auth.currentUser
    }

    fun logout() {
        FirebaseManager.auth.signOut()
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