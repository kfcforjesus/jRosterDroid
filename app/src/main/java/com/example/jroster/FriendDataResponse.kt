package com.example.jroster

data class FriendDataResponse(
    val friendCode: String,
    val name: String,
    val userID: String
)

data class FriendFlight(
    val activity: String,
    val ata: String,
    val atd: String,
    val base: String,
    val checkIn: String,
    val checkOut: String,
    val date: String,
    val dd: String,
    val dest: String,
    val friendCode: String,
    val orig: String
)
