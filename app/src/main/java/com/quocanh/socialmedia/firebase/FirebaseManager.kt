package com.quocanh.socialmedia.firebase

import com.google.firebase.auth.FirebaseAuth
<<<<<<< HEAD
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
}
=======
import com.google.firebase.auth.FirebaseUser

class FirebaseManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            onResult(false, "Vui lòng nhập đầy đủ email và mật khẩu")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Đăng nhập thành công")
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Đăng nhập thất bại")
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
>>>>>>> c438a43ed867e2bb7af8b5c60108d2f6d6945140
