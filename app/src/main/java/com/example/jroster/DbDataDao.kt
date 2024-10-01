package com.example.jroster

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DbDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(rosterEntries: List<DbData>)

    @Query("DELETE FROM DbData")
    fun deleteAll()

    @Query("SELECT * FROM DbData")
    fun getAll(): List<DbData>

    @Query("DELETE FROM DbData")
    suspend fun clearAll()

    @Query("SELECT * FROM dbdata WHERE activity IN (:daysOffCodes)")
    suspend fun getUserDaysOff(daysOffCodes: List<String>): List<DbData>

    @Query("SELECT * FROM dbdata WHERE activity IN (:activities) AND date >= :fromDate")
    suspend fun getUserDaysOffFromDate(activities: List<String>, fromDate: String): List<DbData>
}