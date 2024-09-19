package com.example.jroster

sealed class RosterSection {
    data class Header(val dateText: String) : RosterSection()
    data class Duty(
        val route: String,
        val time: String,
        val checkIn: String,
        val atd: String
    ) : RosterSection()
}