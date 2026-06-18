package com.quocanh.socialmedia.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quocanh.socialmedia.model.Post
import kotlinx.coroutines.tasks.await

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")

    suspend fun getPosts(): List<Post> {
        return try {
            val allPosts = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
            
            // Lấy danh sách ID của những người dùng bị vô hiệu hóa
            val disabledUsers = db.collection("users").whereEqualTo("isDisabled", true).get().await()
            val disabledUserIds = disabledUsers.documents.map { it.id }.toSet()
            
            // Chỉ lọc bỏ những bài viết của người dùng bị disable
            allPosts.filter { it.userId !in disabledUserIds }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowingPosts(followingIds: List<String>): List<Post> {
        if (followingIds.isEmpty()) return emptyList()
        return try {
            val allPosts = postsCollection
                .whereIn("userId", followingIds)
                .get()
                .await()
                .toObjects(Post::class.java)
            
            val disabledUsers = db.collection("users").whereEqualTo("isDisabled", true).get().await()
            val disabledUserIds = disabledUsers.documents.map { it.id }.toSet()
            
            allPosts.filter { it.userId !in disabledUserIds }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPostsByUser(userId: String): List<Post> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            val isDisabled = userDoc.getBoolean("isDisabled") ?: false
            if (isDisabled) return emptyList()

            postsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(Post::class.java)
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPost(post: Post): Boolean {
        return try {
            val docRef = postsCollection.document()
            val postWithId = post.copy(id = docRef.id)
            docRef.set(postWithId).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updatePost(postId: String, content: String, imageUrl: String): Boolean {
        return try {
            postsCollection.document(postId).update(
                mapOf(
                    "content" to content,
                    "imageUrl" to imageUrl
                )
            ).await()
            true
        } catch (e: Exception) {
            false
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

    suspend fun toggleLike(postId: String, userId: String): Boolean {
        return try {
            val postRef = postsCollection.document(postId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val post = snapshot.toObject(Post::class.java)
                if (post != null) {
                    val newLikes = post.likes.toMutableList()
                    if (newLikes.contains(userId)) {
                        newLikes.remove(userId)
                    } else {
                        newLikes.add(userId)
                    }
                    transaction.update(postRef, "likes", newLikes)
                }
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
