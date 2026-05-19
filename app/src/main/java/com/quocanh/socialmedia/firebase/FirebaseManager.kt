package com.quocanh.socialmedia.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {

        if (email.isEmpty() || password.isEmpty()) {
            onResult(false, "Vui lòng nhập đầy đủ email và mật khẩu")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
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
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }
}