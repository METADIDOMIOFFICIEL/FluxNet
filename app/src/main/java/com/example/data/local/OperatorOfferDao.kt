package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OperatorOfferDao {
    @Query("SELECT * FROM operator_offers")
    fun getAllOffers(): Flow<List<OperatorOfferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOffer(offer: OperatorOfferEntity)
}
