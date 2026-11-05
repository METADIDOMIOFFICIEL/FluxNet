package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_quality_records")
data class NetworkQualityRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val operatorKey: String,
    val connectionType: String,
    val downloadSpeedMbps: Double,
    val uploadSpeedMbps: Double,
    val latencyMs: Long,
    val jitterMs: Long,
    val timestampMillis: Long,
    val networkName: String
)
