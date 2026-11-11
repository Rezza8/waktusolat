package com.example.waktusolat

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.waktusolat.api.PrayerApiService
import com.example.waktusolat.api.PrayerTimeV2
import com.example.waktusolat.api.Zone
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val api = PrayerApiService.create()
    private val appPrefs = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val tz: TimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

    private var prayerJob: Job? = null
    private var requestedZone: String? = null
    private var lastLoadedZone: String? = null

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
        val prefs = application.getSharedPreferences(ThemeConfig.PREFS_KEY, Context.MODE_PRIVATE)
        _currentThemeIndex.value = prefs.getInt(ThemeConfig.PREFS_THEME_INDEX, 0)

        restoreCachedZones()
        restoreCachedPrayerTimes()

        // Don't wait for the zones call before fetching today's times.
        if (_selectedZone.value == null) {
            appPrefs.getString("zone_code", null)?.let { loadPrayerTimes(it) }
        }
        loadZones()
    }

    fun loadZones() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { api.getZones() }
                if (result.isNotEmpty()) {
                    appPrefs.edit().putString(PREFS_CACHED_ZONES, gson.toJson(result)).apply()
                    _zones.value = result
                    restoreSavedZone(result)
                }
            } catch (_: Exception) { }
        }
    }

    fun setThemeIndex(index: Int) {
        _currentThemeIndex.value = index
    }

    fun loadPrayerTimes(zoneCode: String, force: Boolean = false) {
        if (!force) {
            if (prayerJob?.isActive == true && requestedZone == zoneCode) return

            if (zoneCode == lastLoadedZone && _todayPrayer.value != null) {
                requestedZone = zoneCode
                return
            }
        }

        // A request for another zone (or a forced refresh) supersedes the in-flight one.
        if (prayerJob?.isActive == true) {
            prayerJob?.cancel()
        }

        requestedZone = zoneCode
        prayerJob = viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { api.getPrayerTimesV2(zoneCode) }
                if (requestedZone != zoneCode) return@launch

                val today = Calendar.getInstance(tz).get(Calendar.DAY_OF_MONTH)
                _todayPrayer.value = response.prayers.find { it.day == today }
                PrayerNotificationHelper.scheduleAlarms(getApplication(), response.prayers, today)
                KeepAliveService.start(getApplication())

                // Cache the monthly schedule so the next launch can render instantly.
                appPrefs.edit()
                    .putString(PREFS_CACHED_PRAYERS, gson.toJson(response.prayers))
                    .putInt(PREFS_CACHED_TODAY, today)
                    .putString(PREFS_CACHED_ZONE, zoneCode)
                    .apply()

                lastLoadedZone = zoneCode
                PrayerWidgetProvider.requestUpdate(getApplication())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }

    fun setSelectedZone(zone: Zone) {
        _selectedZone.value = zone
        appPrefs.edit()
            .putString("zone_code", zone.jakimCode)
            .putString("zone_display", zone.negeri)
            .apply()
        loadPrayerTimes(zone.jakimCode)
    }

    fun restoreSavedZone(allZones: List<Zone>) {
        if (_selectedZone.value != null) return

        val saved = appPrefs.getString("zone_code", null)
        val zone = (if (saved != null) allZones.find { it.jakimCode == saved } else null)
            ?: allZones.find { it.jakimCode == DEFAULT_ZONE }
            ?: return
        setSelectedZone(zone)
    }

    fun getCountryName(): String {
        val locale = Locale.getDefault()
        val country = locale.displayCountry
        return if (country.isNotBlank()) country else "Sabah"
    }

    fun loadQuranVerse() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
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

    private fun restoreCachedZones() {
        val cached = appPrefs.getString(PREFS_CACHED_ZONES, null) ?: return
        try {
            val zones = gson.fromJson(cached, Array<Zone>::class.java)?.toList() ?: return
            if (zones.isEmpty()) return
            _zones.value = zones
            restoreSavedZone(zones)
        } catch (_: Exception) { }
    }

    private fun restoreCachedPrayerTimes() {
        val savedZone = appPrefs.getString("zone_code", null) ?: return
        val cachedZone = appPrefs.getString(PREFS_CACHED_ZONE, null) ?: return
        if (savedZone != cachedZone) return

        val cached = appPrefs.getString(PREFS_CACHED_PRAYERS, null) ?: return
        try {
            val prayers = gson.fromJson(cached, Array<PrayerTimeV2>::class.java)?.toList() ?: return
            val now = Calendar.getInstance(tz)
            val today = prayers.firstOrNull { sameDay(it.fajr, now) } ?: return
            _todayPrayer.value = today
        } catch (_: Exception) { }
    }

    private fun sameDay(epochSeconds: Long, reference: Calendar): Boolean {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = epochSeconds * 1000L }
        return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == reference.get(Calendar.DAY_OF_YEAR)
    }

    data class QuranVerse(val text: String, val surah: Int, val ayah: Int, val surahName: String)

    companion object {
        private const val DEFAULT_ZONE = "SGR01"

        const val PREFS_CACHED_PRAYERS = "cached_prayers_json"
        const val PREFS_CACHED_TODAY = "cached_today"
        const val PREFS_CACHED_ZONE = "cached_zone_code"
        const val PREFS_CACHED_ZONES = "cached_zones_json"
    }
}
