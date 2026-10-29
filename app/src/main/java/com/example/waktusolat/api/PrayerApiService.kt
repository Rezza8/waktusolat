package com.example.waktusolat.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface PrayerApiService {

    @GET("zones")
    suspend fun getZones(): List<Zone>

    @GET("v2/solat/{zone}")
    suspend fun getPrayerTimesV2(
        @Path("zone") zone: String,
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): PrayerResponseV2

    @GET("v2/solat/{zone}")
    fun getPrayerTimesV2Sync(
        @Path("zone") zone: String,
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): retrofit2.Call<PrayerResponseV2>

    @GET("solat/{zone}")
    suspend fun getPrayerTimesV1(
        @Path("zone") zone: String,
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): PrayerResponseV1

    companion object {
        private const val BASE_URL = "https://api.waktusolat.app/"

        fun create(): PrayerApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PrayerApiService::class.java)
        }
    }
}
