package com.quocanh.socialmedia.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.quocanh.socialmedia.model.Comment
import com.quocanh.socialmedia.model.Post
import com.quocanh.socialmedia.model.User
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val postsCollection = db.collection("posts")

    suspend fun getAllUsers(currentAdminUid: String? = null): List<User> {
        return try {
            val users = usersCollection.get().await().toObjects(User::class.java)
            if (currentAdminUid != null) {
                users.filter { it.uid != currentAdminUid }
            } else {
                users
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchUsers(query: String, currentAdminUid: String? = null): List<User> {
        return try {
            val allUsers = usersCollection.get().await().toObjects(User::class.java)
            allUsers.filter { 
                (it.username.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true)) &&
                it.uid != currentAdminUid
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleUserDisabled(uid: String, isDisabled: Boolean): Boolean {
        return try {
            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: return false

            // 1. Cập nhật trạng thái isDisabled của tài khoản đó
            usersCollection.document(uid).update("isDisabled", isDisabled).await()

            // 2. Cập nhật danh sách follow của những người liên quan để trừ/tăng số lượng hiển thị
            val following = user.following
            val followers = user.followers

            if (isDisabled) {
                // Khi vô hiệu hóa: Xóa uid này khỏi followers của những người họ đang follow
                following.forEach { targetUid ->
                    usersCollection.document(targetUid).update("followers", FieldValue.arrayRemove(uid)).await()
                }
                // Xóa uid này khỏi following của những người đang follow họ
                followers.forEach { followerUid ->
                    usersCollection.document(followerUid).update("following", FieldValue.arrayRemove(uid)).await()
                }
            } else {
                // Khi mở lại: Thêm uid này lại vào followers của những người họ đang follow
                following.forEach { targetUid ->
                    usersCollection.document(targetUid).update("followers", FieldValue.arrayUnion(uid)).await()
                }
                // Thêm uid này lại vào following của những người đang follow họ
                followers.forEach { followerUid ->
                    usersCollection.document(followerUid).update("following", FieldValue.arrayUnion(uid)).await()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteUser(uid: String): Boolean {
        return try {
            // Khi thực sự xóa tài khoản, mới xóa vĩnh viễn mọi dữ liệu liên quan bài viết, comment, like, follow
            cleanupUserData(uid)
            usersCollection.document(uid).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun cleanupUserData(uid: String) {
        // 1. Xóa tất cả bài viết của người dùng này
        val userPosts = postsCollection.whereEqualTo("userId", uid).get().await()
        for (doc in userPosts.documents) {
            doc.reference.delete().await()
        }

        // 2. Xóa người dùng khỏi danh sách like của tất cả bài viết khác
        val likedPosts = postsCollection.whereArrayContains("likes", uid).get().await()
        for (doc in likedPosts.documents) {
            doc.reference.update("likes", FieldValue.arrayRemove(uid)).await()
        }

        // 3. Xóa người dùng khỏi danh sách followers và following của tất cả người dùng khác
        val followers = usersCollection.whereArrayContains("followers", uid).get().await()
        for (doc in followers.documents) {
            doc.reference.update("followers", FieldValue.arrayRemove(uid)).await()
        }
        val following = usersCollection.whereArrayContains("following", uid).get().await()
        for (doc in following.documents) {
            doc.reference.update("following", FieldValue.arrayRemove(uid)).await()
        }

        // 4. Xóa tất cả bình luận của người dùng này trong tất cả các bài viết
        try {
            val userComments = db.collectionGroup("comments").whereEqualTo("userId", uid).get().await()
            for (doc in userComments.documents) {
                val commentRef = doc.reference
                val postRef = commentRef.parent.parent
                
                db.runTransaction { transaction ->
                    transaction.delete(commentRef)
                    if (postRef != null) {
                        transaction.update(postRef, "commentCount", FieldValue.increment(-1))
                    }
                }.await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAllPosts(): List<Post> {
        return try {
            postsCollection.get().await().toObjects(Post::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchPosts(query: String): List<Post> {
        return try {
            val allPosts = postsCollection.get().await().toObjects(Post::class.java)
            allPosts.filter { it.content.contains(query, ignoreCase = true) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deletePost(postId: String): Boolean {
        return try {
            postsCollection.document(postId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Boolean {
        return try {
            db.runBatch { batch ->
                batch.delete(postsCollection.document(postId).collection("comments").document(commentId))
                batch.update(postsCollection.document(postId), "commentCount", FieldValue.increment(-1))
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
