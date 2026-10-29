package com.example.waktusolat.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Zone(
    val jakimCode: String,
    val negeri: String,
    val daerah: String
)

data class PrayerTimeV2(
    val day: Int,
    val hijri: String,
    val fajr: Long,
    val syuruk: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long
)

data class PrayerResponseV2(
    val zone: String,
    val year: Int,
    val month: String,
    @SerializedName("month_number")
    val monthNumber: Int,
    @SerializedName("last_updated")
    val lastUpdated: String?,
    val prayers: List<PrayerTimeV2>
)

data class PrayerTimeV1(
    val hijri: String,
    val date: String,
    val day: String,
    val fajr: String,
    val syuruk: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

data class PrayerResponseV1(
    val prayerTime: List<PrayerTimeV1>,
    val status: String,
    val serverTime: String,
    val periodType: String,
    val lang: String,
    val zone: String,
    val bearing: String
)

data class DailyPrayer(
    val name: String,
    val nameMs: String,
    val time: String,
    val isCurrent: Boolean = false
)

data class JuzAmmaSurah(
    val number: Int,
    val nameArabic: String,
    val nameLatin: String,
    val nameMs: String,
    val verses: Int,
    val meaning: String
) : Serializable

data class QuranAyah(
    val number: Int,
    val numberInSurah: Int,
    val text: String
)

data class QuranEdition(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val format: String,
    val type: String,
    val direction: String
)

data class SurahDetail(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val ayahs: List<QuranAyah>,
    val edition: QuranEdition
)

data class QuranResponse(
    val code: Int,
    val status: String,
    val data: SurahDetail
)

data class AyahSurahInfo(
    val number: Int,
    val name: String,
    val englishName: String
)

data class AyahData(
    val number: Int,
    val numberInSurah: Int,
    val text: String,
    val surah: AyahSurahInfo
)

data class AyahResponse(
    val code: Int,
    val status: String,
    val data: AyahData
)
