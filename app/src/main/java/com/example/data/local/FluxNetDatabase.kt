package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppRuleEntity::class,
        OperatorOfferEntity::class,
        NetworkIncidentEntity::class,
        NetworkQualityRecordEntity::class,
        CommunityPostEntity::class,
        PostCommentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class FluxNetDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao
    abstract fun operatorOfferDao(): OperatorOfferDao
    abstract fun networkIncidentDao(): NetworkIncidentDao
    abstract fun networkQualityRecordDao(): NetworkQualityRecordDao
    abstract fun communityPostDao(): CommunityPostDao
    abstract fun postCommentDao(): PostCommentDao

    companion object {
        @Volatile
        private var INSTANCE: FluxNetDatabase? = null

        fun getInstance(context: Context): FluxNetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FluxNetDatabase::class.java,
                    "fluxnet_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
