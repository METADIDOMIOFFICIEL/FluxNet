package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operator_offers")
data class OperatorOfferEntity(
    @PrimaryKey val operatorKey: String,
    val planType: String = "Mobile 4G/5G",
    val priceFcfa: Double = 2000.0,
    val dataMb: Double = 0.0,
    val callMinutes: Int = 0,
    val smsCount: Int = 0,
    val validityDays: Int = 7
)
