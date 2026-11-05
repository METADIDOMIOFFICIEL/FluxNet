package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules")
    fun getAllRules(): Flow<List<AppRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: AppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<AppRuleEntity>)
}
