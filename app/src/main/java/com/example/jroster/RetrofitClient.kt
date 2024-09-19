package com.example.jroster

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.Date

object RetrofitClient {
    private const val BASE_URL = "http://flightschoolms.com/JRoster/"

    // Remove the DateDeserializer reference and use default Gson
    private val gson = GsonBuilder()
        .create()  // No need to register any custom deserializer


    private val okHttpClient = OkHttpClient.Builder().build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())  // No custom deserializer needed
            .build()
    }

    val rosterApiService: RosterApiService by lazy {
        retrofit.create(RosterApiService::class.java)
    }
}
