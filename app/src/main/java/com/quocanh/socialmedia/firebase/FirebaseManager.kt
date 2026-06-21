package com.quocanh.socialmedia.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quocanh.socialmedia.model.Message
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.model.User

object FirebaseManager {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    fun sendMessage(receiverId: String, content: String, onResult: (Boolean) -> Unit) {
        val senderId = auth.currentUser?.uid ?: return onResult(false)
        if (content.isEmpty()) return onResult(false)

        val conversationId = getConversationId(senderId, receiverId)
        val conversationDocRef = firestore.collection("conversations").document(conversationId)
        val messageId = conversationDocRef.collection("messages").document().id
        val timestamp = System.currentTimeMillis()

        val message = Message(
            id = messageId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = timestamp
        )

        val batch = firestore.batch()
        batch.set(conversationDocRef.collection("messages").document(messageId), message)
        batch.set(
            conversationDocRef,
            mapOf(
                "participants" to listOf(senderId, receiverId),
                "lastMessage" to content,
                "lastTimestamp" to timestamp,
                "lastSenderId" to senderId
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )

        batch.commit()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun getMessages(conversationId: String, onResult: (List<Message>) -> Unit) {
        firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val messages = value?.toObjects(Message::class.java) ?: emptyList()
                onResult(messages)
            }
    }

    fun getConversations(onResult: (List<Map<String, Any?>>) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onResult(emptyList())

        firestore.collection("conversations")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    // Log the error for debugging
                    println("Firebase Error getting conversations: ${error.localizedMessage}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val conversations = value?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val participants = data["participants"] as? List<String> ?: emptyList()
                    val otherUserId = participants.find { it != currentUid } ?: return@mapNotNull null
                    mapOf(
                        "otherUserId" to otherUserId,
                        "lastMessage" to data["lastMessage"],
                        "lastTimestamp" to data["lastTimestamp"],
                        "lastSenderId" to data["lastSenderId"]
                    )
                } ?: emptyList()
                // Sort manually for now if needed
                onResult(conversations.sortedByDescending { it["lastTimestamp"] as? Long ?: 0L })
            }
    }

    fun checkMutualFollow(uid1: String, uid2: String, onResult: (Boolean) -> Unit) {
        getUserInfo(uid1) { user1 ->
            getUserInfo(uid2) { user2 ->
                val isMutual = user1?.following?.contains(uid2) == true && 
                               user2?.following?.contains(uid1) == true
                onResult(isMutual)
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

    fun getUsers(uids: List<String>, onlyActive: Boolean = true, onResult: (List<User>) -> Unit) {
        if (uids.isEmpty()) {
            onResult(emptyList())
            return
        }
        
        // Firestore whereIn limit is 10. Chunk the list to support more users.
        val chunks = uids.chunked(10)
        val allUsers = mutableListOf<User>()
        var processedChunks = 0

        for (chunk in chunks) {
            var query = firestore.collection("users").whereIn("uid", chunk)
            if (onlyActive) {
                query = query.whereEqualTo("isDisabled", false)
            }

            query.get()
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

    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        if (query.isBlank()) {
            onResult(emptyList())
            return
        }
        firestore.collection("users").get()
            .addOnSuccessListener { documents ->
                val users = documents.toObjects(User::class.java)
                val filtered = users.filter { 
                    it.username.contains(query, ignoreCase = true) && !it.isDisabled
                }
                onResult(filtered)
            }
            .addOnFailureListener {
                onResult(emptyList())
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
