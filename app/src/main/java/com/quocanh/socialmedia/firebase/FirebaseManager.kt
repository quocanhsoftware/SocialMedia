package com.quocanh.socialmedia.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.model.User

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

    fun updateUserInfo(user: User, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(false, "Người dùng chưa đăng nhập")
        
        firestore.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                onResult(true, "Cập nhật thông tin thành công")
            }
            .addOnFailureListener {
                onResult(false, it.localizedMessage ?: "Cập nhật thất bại")
            }
    }

    fun getUserInfo(uid: String, onResult: (User?) -> Unit) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                onResult(user)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun getUsers(uids: List<String>, onResult: (List<User>) -> Unit) {
        if (uids.isEmpty()) {
            onResult(emptyList())
            return
        }
        
        // Firestore whereIn limit is 10. Chunk the list to support more users.
        val chunks = uids.chunked(10)
        val allUsers = mutableListOf<User>()
        var processedChunks = 0

        for (chunk in chunks) {
            firestore.collection("users")
                .whereIn("uid", chunk)
                .get()
                .addOnSuccessListener { documents ->
                    allUsers.addAll(documents.toObjects(User::class.java))
                    processedChunks++
                    if (processedChunks == chunks.size) {
                        onResult(allUsers)
                    }
                }
                .addOnFailureListener {
                    processedChunks++
                    if (processedChunks == chunks.size) {
                        onResult(allUsers)
                    }
                }
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }

    fun followUser(targetUid: String, onResult: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onResult(false)
        if (currentUid == targetUid) return onResult(false)

        val batch = firestore.batch()
        
        val currentUserRef = firestore.collection("users").document(currentUid)
        val targetUserRef = firestore.collection("users").document(targetUid)

        batch.update(currentUserRef, "following", FieldValue.arrayUnion(targetUid))
        batch.update(targetUserRef, "followers", FieldValue.arrayUnion(currentUid))

        batch.commit()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun unfollowUser(targetUid: String, onResult: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onResult(false)
        
        val batch = firestore.batch()
        
        val currentUserRef = firestore.collection("users").document(currentUid)
        val targetUserRef = firestore.collection("users").document(targetUid)

        batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetUid))
        batch.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUid))

        batch.commit()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getAllPosts(onResult: (List<Post>) -> Unit) {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val posts = value?.toObjects(Post::class.java) ?: emptyList()
                onResult(posts)
            }
    }

    fun getFollowingPosts(onResult: (List<Post>) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onResult(emptyList())
        
        getUserInfo(currentUid) { user ->
            val following = user?.following ?: emptyList()
            if (following.isEmpty()) {
                onResult(emptyList())
                return@getUserInfo
            }

            firestore.collection("posts")
                .whereIn("userId", following)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        onResult(emptyList())
                        return@addSnapshotListener
                    }
                    val posts = value?.toObjects(Post::class.java) ?: emptyList()
                    onResult(posts)
                }
        }
    }
}
