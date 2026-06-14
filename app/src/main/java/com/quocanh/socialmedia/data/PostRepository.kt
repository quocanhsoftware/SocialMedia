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
            postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
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
