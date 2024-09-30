package com.example.jroster

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FriendsFlightsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flightEntries: List<FriendsFlights>)

    @Query("SELECT * FROM friends_flights WHERE friendCode = :friendCode")
    suspend fun getFlightsByFriendCode(friendCode: String): List<FriendsFlights>

    @Query("DELETE FROM friends_flights WHERE friendCode = :friendCode")
    suspend fun deleteFlightsForFriend(friendCode: String)
}
