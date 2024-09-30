package com.example.jroster

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FriendsFlightsApiService {
    @GET("fetchFriendRoster.php")
    fun getFriendFlights(@Query("friendCode") friendCode: String): Call<List<FriendsFlights>>
}