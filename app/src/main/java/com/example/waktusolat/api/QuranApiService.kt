package com.example.waktusolat.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface QuranApiService {

    @GET("v1/surah/{number}")
    suspend fun getSurahArabic(@Path("number") number: Int): QuranResponse

    @GET("v1/surah/{number}/ms.basmeih")
    suspend fun getSurahMalay(@Path("number") number: Int): QuranResponse

    @GET("v1/surah/{number}/en.transliteration")
    suspend fun getSurahTransliteration(@Path("number") number: Int): QuranResponse

    @GET("v1/ayah/{surah}:{ayah}/{edition}")
    suspend fun getAyah(
        @Path("surah") surah: Int,
        @Path("ayah") ayah: Int,
        @Path("edition") edition: String
    ): AyahResponse

    companion object {
        private const val BASE_URL = "https://api.alquran.cloud/"

        fun create(): QuranApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QuranApiService::class.java)
        }
    }
}
