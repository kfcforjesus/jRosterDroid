package com.example.jroster

import retrofit2.Call
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Field

interface FriendApiService {
    @FormUrlEncoded
    @POST("getFriendData.php")
    fun getFriendData(@Field("friendCode") friendCode: String): Call<List<Friend>>
}
