package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val wifiAllowed: Boolean = true,
    val mobileAllowed: Boolean = true,
    val blockBackground: Boolean = false,
    val autoWatch: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
