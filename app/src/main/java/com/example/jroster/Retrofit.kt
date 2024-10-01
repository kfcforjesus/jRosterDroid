package com.example.jroster

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RosterApiService {

    // Fetch user's own roster, returns List<DbData>
    @FormUrlEncoded
    @POST("fetchRoster.php")
    fun getRosterData(
        @Field("userID") userID: String,
        @Field("passCode") passCode: String
    ): Call<List<DbData>>

    // Fetch friend's roster, returns List<FriendsFlights>
    @FormUrlEncoded
    @POST("fetchFriendRoster.php")
    fun getFriendRosterData(
        @Field("userID") friendUserID: String,
        @Field("friendCode") friendCode: String
    ): Call<List<FriendsFlights>>
}
