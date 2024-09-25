package com.example.jroster

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DbDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)  // Replace if there's a conflict
    fun insertAll(data: List<DbData>)

    @Query("SELECT * FROM DbData")
    suspend fun getAll(): List<DbData>

    @Query("DELETE FROM DbData")
    fun deleteAll()

    @Query("DELETE FROM DbData")
    suspend fun clearAll()
}
