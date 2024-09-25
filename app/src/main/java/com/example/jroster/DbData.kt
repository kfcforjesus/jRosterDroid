package com.example.jroster

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DbData")
data class DbData(
    @PrimaryKey val date: String, // Marking date as the primary key
    val dd: String?,
    val activity: String,
    val checkIn: String,
    val orig: String,
    val atd: String,
    val dest: String,
    val ata: String,
    val checkOut: String,
    val ac: String
)

