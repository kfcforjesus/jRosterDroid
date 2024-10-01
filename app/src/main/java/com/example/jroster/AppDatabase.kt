package com.example.jroster

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DbData::class, Friend::class, FriendsFlights::class],  // Add new entities
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dbDataDao(): DbDataDao
    abstract fun friendDao(): FriendDao
    abstract fun friendsFlightsDao(): FriendsFlightsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roster_database"
                )
                    .fallbackToDestructiveMigration()  // This line will delete old data and recreate the database with the new schema.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
