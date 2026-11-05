package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val authorName: String,
    val authorAvatarColor: Long = 0xFF0284C7,
    val postType: String, // "INCIDENT" or "PLAN_OFFER"
    val operatorKey: String,
    val planType: String = "Mobile 4G/5G",
    val location: String = "National",
    val title: String,
    val content: String,
    val timestampMillis: Long,
    val dateFormatted: String,
    val timeFormatted: String,
    // Incident specifics
    val problemLevel: String = "", // "Critique", "Majeur", "Modéré", "Mineur"
    val durationMinutes: Int = 0,
    val isResolved: Boolean = true,
    // Plan offer specifics
    val priceFcfa: Double = 0.0,
    val dataMb: Double = 0.0,
    val callMinutes: Int = 0,
    val smsCount: Int = 0,
    val validityDays: Int = 0,
    // Social counters
    val likeCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentCount: Int = 0
)
