package com.example.jroster

import retrofit2.Call
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Field

interface PasscodeApiService {
    @FormUrlEncoded
    @POST("getFriendCode.php")
    fun getFriendCode(@Field("userID") userID: String, @Field("passcode") passcode: String): Call<List<Map<String, String>>>
}
