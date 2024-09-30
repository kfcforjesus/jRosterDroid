package com.example.jroster

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val friendCode: String, // Primary key
    val name: String,
    val userID: String
)
