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




}
