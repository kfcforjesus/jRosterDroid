package com.example.jroster

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RosterApiService {

    // Define the POST request for fetching roster data for the current user
    @FormUrlEncoded
    @POST("fetchRoster.php")
    fun getRosterData(
        @Field("userID") userID: String,
        @Field("passCode") passCode: String
    ): Call<List<DbData>>

    // Define the POST request for fetching friend's roster data
    @FormUrlEncoded
    @POST("fetchFriendRoster.php")
    fun getFriendRosterData(
        @Field("userID") userID: String,
        @Field("friendCode") friendCode: String
    ): Call<List<DbData>>
}
