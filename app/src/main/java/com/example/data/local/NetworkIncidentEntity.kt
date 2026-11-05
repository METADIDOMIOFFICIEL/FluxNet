package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_incidents")
data class NetworkIncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val operatorKey: String,
    val planType: String = "Mobile 4G/5G",
    val problemLevel: String = "Modéré",
    val durationMinutes: Int = 30,
    val incidentDate: String,
    val incidentTime: String,
    val dayOfWeek: String,
    val timestampMillis: Long,
    val isResolved: Boolean = true,
    val description: String = ""
)
