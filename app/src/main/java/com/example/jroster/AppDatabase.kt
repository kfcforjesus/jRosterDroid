package com.example.jroster

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DbData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dbDataDao(): DbDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Only one instance of the database is created at a time
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roster_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
