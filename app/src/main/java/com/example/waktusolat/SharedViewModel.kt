package com.example.waktusolat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.waktusolat.api.PrayerApiService
import com.example.waktusolat.api.PrayerTimeV2
import com.example.waktusolat.api.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val api = PrayerApiService.create()

    private val _zones = MutableLiveData<List<Zone>>()
    val zones: LiveData<List<Zone>> = _zones

    private val _selectedZone = MutableLiveData<Zone?>()
    val selectedZone: LiveData<Zone?> = _selectedZone

    private val _todayPrayer = MutableLiveData<PrayerTimeV2?>()
    val todayPrayer: LiveData<PrayerTimeV2?> = _todayPrayer

    private val _currentThemeIndex = MutableLiveData<Int>()
    val currentThemeIndex: LiveData<Int> = _currentThemeIndex

    private val _quranVerse = MutableLiveData<QuranVerse?>()
    val quranVerse: LiveData<QuranVerse?> = _quranVerse

    private var cachedVerseDay = -1

    init {
        val prefs = application.getSharedPreferences(ThemeConfig.PREFS_KEY, android.content.Context.MODE_PRIVATE)
        _currentThemeIndex.value = prefs.getInt(ThemeConfig.PREFS_THEME_INDEX, 0)
        loadZones()
    }

    fun loadZones() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { api.getZones() }
                _zones.value = result
                restoreSavedZone(result)
            } catch (_: Exception) { }
        }
    }

    fun setThemeIndex(index: Int) {
        _currentThemeIndex.value = index
    }

    fun loadPrayerTimes(zoneCode: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { api.getPrayerTimesV2(zoneCode) }
                val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
                _todayPrayer.value = response.prayers.find { it.day == today }
                PrayerNotificationHelper.scheduleAlarms(getApplication(), response.prayers, today)
                KeepAliveService.start(getApplication())

                // Cache prayer times so KeepAliveService can re-schedule alarms on heartbeat
                getApplication<Application>().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREFS_CACHED_PRAYERS, com.google.gson.Gson().toJson(response.prayers))
                    .putInt(PREFS_CACHED_TODAY, today)
                    .putString(PREFS_CACHED_ZONE, zoneCode)
                    .apply()
            } catch (_: Exception) { }
        }
    }

    fun setSelectedZone(zone: Zone) {
        _selectedZone.value = zone
        getApplication<Application>().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("zone_code", zone.jakimCode)
            .putString("zone_display", zone.negeri)
            .apply()
        loadPrayerTimes(zone.jakimCode)
    }

    fun restoreSavedZone(allZones: List<Zone>) {
        val prefs = getApplication<Application>().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("zone_code", null)
        var zone: Zone? = null
        if (saved != null) {
            zone = allZones.find { it.jakimCode == saved }
        }
        if (zone == null) {
            zone = allZones.find { it.jakimCode == "SGR01" }
        }
        if (zone != null) {
            setSelectedZone(zone)
        }
        _selectedZone.value?.let { loadPrayerTimes(it.jakimCode) }
    }

    fun getCountryName(): String {
        val locale = Locale.getDefault()
        val country = locale.displayCountry
        return if (country.isNotBlank()) country else "Sabah"
    }

    fun loadQuranVerse() {
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        if (today == cachedVerseDay && _quranVerse.value != null) return
        cachedVerseDay = today

        val juzAmmaSurahs = listOf(
            78 to 40, 79 to 46, 80 to 42, 81 to 29, 82 to 19, 83 to 36, 84 to 25, 85 to 22,
            86 to 17, 87 to 19, 88 to 26, 89 to 30, 90 to 20, 91 to 15, 92 to 21, 93 to 11,
            94 to 8, 95 to 8, 96 to 19, 97 to 5, 98 to 8, 99 to 8, 100 to 11, 101 to 11,
            102 to 8, 103 to 3, 104 to 9, 105 to 5, 106 to 4, 107 to 7, 108 to 3, 109 to 6,
            110 to 3, 111 to 5, 112 to 4, 113 to 5, 114 to 6
        )
        val idx = today % juzAmmaSurahs.size
        val (surah, ayahCount) = juzAmmaSurahs[idx]
        val ayah = (today % ayahCount) + 1

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    com.example.waktusolat.api.QuranApiService.create().getAyah(surah, ayah, "ms.basmeih")
                }
                _quranVerse.value = QuranVerse(
                    text = result.data?.text ?: "",
                    surah = surah,
                    ayah = ayah,
                    surahName = result.data?.surah?.name ?: ""
                )
            } catch (_: Exception) { }
        }
    }

    data class QuranVerse(val text: String, val surah: Int, val ayah: Int, val surahName: String)

    companion object {
        const val PREFS_CACHED_PRAYERS = "cached_prayers_json"
        const val PREFS_CACHED_TODAY = "cached_today"
        const val PREFS_CACHED_ZONE = "cached_zone_code"
    }
}
