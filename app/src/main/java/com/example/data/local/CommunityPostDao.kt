package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityPostDao {
    @Query("SELECT * FROM community_posts ORDER BY timestampMillis DESC")
    fun getAllPosts(): Flow<List<CommunityPostEntity>>

    @Query("SELECT * FROM community_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: Long): CommunityPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CommunityPostEntity): Long

    @Update
    suspend fun updatePost(post: CommunityPostEntity)

    @Query("UPDATE community_posts SET likeCount = :newCount, isLikedByMe = :isLiked WHERE id = :postId")
    suspend fun updateLike(postId: Long, isLiked: Boolean, newCount: Int)

    @Query("UPDATE community_posts SET commentCount = commentCount + 1 WHERE id = :postId")
    suspend fun incrementCommentCount(postId: Long)

    @Query("DELETE FROM community_posts WHERE id = :postId")
    suspend fun deletePostById(postId: Long)
}
