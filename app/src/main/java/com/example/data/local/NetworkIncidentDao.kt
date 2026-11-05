package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkIncidentDao {
    @Query("SELECT * FROM network_incidents ORDER BY timestampMillis DESC")
    fun getAllIncidents(): Flow<List<NetworkIncidentEntity>>

    @Query("SELECT * FROM network_incidents WHERE operatorKey = :operatorKey ORDER BY timestampMillis DESC")
    fun getIncidentsByOperator(operatorKey: String): Flow<List<NetworkIncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: NetworkIncidentEntity): Long

    @Update
    suspend fun updateIncident(incident: NetworkIncidentEntity)

    @Delete
    suspend fun deleteIncident(incident: NetworkIncidentEntity)

    @Query("DELETE FROM network_incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: Long)
}
