package com.example.jroster

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DbData::class], version = 2) // Increment the version number
abstract class AppDatabase : RoomDatabase() {
    abstract fun dbDataDao(): DbDataDao

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
                    .fallbackToDestructiveMigration() // This line will delete old data and recreate the database with the new schema.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
