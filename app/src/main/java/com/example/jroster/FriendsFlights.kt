package com.example.jroster

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends_flights")
data class FriendsFlights(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activity: String,
    val ata: String?,
    val atd: String,
    val base: String?,
    val checkIn: String?,
    val checkOut: String?,
    val date: String,
    val dd: String?,
    val dest: String,
    val friendCode: String,
    val orig: String
)
