package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "post_comments")
data class PostCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val postId: Long,
    val parentCommentId: Long? = null, // null for root comment, or id of parent comment for replies
    val replyToAuthor: String? = null,
    val authorName: String,
    val authorAvatarColor: Long = 0xFF10B981,
    val commentText: String,
    val timestampMillis: Long,
    val dateFormatted: String,
    val timeFormatted: String,
    val likeCount: Int = 0,
    val isLikedByMe: Boolean = false
)
