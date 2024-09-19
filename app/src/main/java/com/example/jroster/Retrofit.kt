package com.example.jroster

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RosterApiService {

    // Define the POST request for fetching roster data from the PHP script
    @FormUrlEncoded
    @POST("fetchRoster.php")
    fun getRosterData(
        @Field("userID") userID: String,
        @Field("passCode") passCode: String
    ): Call<List<DbData>>
}
