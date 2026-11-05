package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkQualityRecordDao {
    @Query("SELECT * FROM network_quality_records ORDER BY timestampMillis DESC")
    fun getAllRecords(): Flow<List<NetworkQualityRecordEntity>>

    @Query("SELECT * FROM network_quality_records WHERE operatorKey = :operatorKey ORDER BY timestampMillis DESC")
    fun getRecordsByOperator(operatorKey: String): Flow<List<NetworkQualityRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: NetworkQualityRecordEntity): Long

    @Query("DELETE FROM network_quality_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)
}
