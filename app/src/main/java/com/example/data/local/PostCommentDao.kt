package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostCommentDao {
    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY timestampMillis ASC")
    fun getCommentsForPost(postId: Long): Flow<List<PostCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: PostCommentEntity): Long

    @Query("UPDATE post_comments SET likeCount = :newCount, isLikedByMe = :isLiked WHERE id = :commentId")
    suspend fun updateCommentLike(commentId: Long, isLiked: Boolean, newCount: Int)

    @Query("DELETE FROM post_comments WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: Long)
}
