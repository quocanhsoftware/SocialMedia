package com.quocanh.socialmedia.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quocanh.socialmedia.model.Comment
import kotlinx.coroutines.tasks.await

class CommentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")

    suspend fun addComment(postId: String, comment: Comment): Boolean {
        return try {
            val commentRef = postsCollection.document(postId).collection("comments").document()
            val commentWithId = comment.copy(id = commentRef.id, postId = postId)
            
            db.runBatch { batch ->
                batch.set(commentRef, commentWithId)
                batch.update(postsCollection.document(postId), "commentCount", com.google.firebase.firestore.FieldValue.increment(1))
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateComment(postId: String, commentId: String, newContent: String): Boolean {
        return try {
            postsCollection.document(postId).collection("comments").document(commentId)
                .update("content", newContent)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Boolean {
        return try {
            db.runBatch { batch ->
                batch.delete(postsCollection.document(postId).collection("comments").document(commentId))
                batch.update(postsCollection.document(postId), "commentCount", com.google.firebase.firestore.FieldValue.increment(-1))
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getComments(postId: String): List<Comment> {
        return try {
            val comments = postsCollection.document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Comment::class.java)

            // Lấy danh sách ID của những người dùng bị vô hiệu hóa (isDisabled = true)
            val disabledUsers = db.collection("users").whereEqualTo("isDisabled", true).get().await()
            val disabledUserIds = disabledUsers.documents.map { it.id }.toSet()

            // Lọc bỏ bình luận của những người dùng có ID nằm trong danh sách bị khóa
            comments.filter { it.userId !in disabledUserIds }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
