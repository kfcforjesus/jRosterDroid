package com.example.jroster

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient

object RetrofitClient {
    private const val BASE_URL = "http://flightschoolms.com/JRoster/"

    private val okHttpClient = OkHttpClient.Builder().build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())  // Use default Gson converter
            .client(okHttpClient)  // Add OkHttpClient
            .build()
    }

    val rosterApiService: RosterApiService by lazy {
        retrofit.create(RosterApiService::class.java)
    }

    // Friend API Service
    val friendApiService: FriendApiService by lazy {
        retrofit.create(FriendApiService::class.java)
    }
}
