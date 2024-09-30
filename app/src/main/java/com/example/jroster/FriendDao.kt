package com.example.jroster

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend)

    @Query("SELECT * FROM friends")
    suspend fun getAllFriends(): List<Friend>

    @Query("DELETE FROM friends WHERE friendCode = :friendCode")
    suspend fun deleteFriend(friendCode: String)
}