package com.example.jroster

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dbdata")
data class DbData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Auto-generates a unique ID for each duty
    val dd: String,
    val date: String,
    val activity: String,
    val checkIn: String,
    val atd: String,
    val dest: String,
    val orig: String,
    val ata: String,
    val checkOut: String,
    val ac: String?
)