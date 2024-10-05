package com.example.jroster

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object RetrofitClient {
    private const val BASE_URL = "http://flightschoolms.com/JRoster/"

    private val okHttpClient = OkHttpClient.Builder().build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    val rosterApiService: RosterApiService by lazy {
        retrofit.create(RosterApiService::class.java)
    }

    val passcodeApiService: PasscodeApiService by lazy {
        retrofit.create(PasscodeApiService::class.java)
    }

    // Friend API Service
    val friendApiService: FriendApiService by lazy {
        retrofit.create(FriendApiService::class.java)
    }
}
