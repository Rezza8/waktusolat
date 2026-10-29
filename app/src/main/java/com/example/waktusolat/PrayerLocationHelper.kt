package com.example.waktusolat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.waktusolat.api.Zone
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

object PrayerLocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun getAddress(context: Context, lat: Double, lng: Double): Address? {
        return try {
            val geocoder = Geocoder(context, Locale("ms", "MY"))
            geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun matchZone(zones: List<Zone>, state: String?, city: String?): Zone? {
        if (zones.isEmpty()) return null

        val stateNorm = state?.trim()?.lowercase(Locale.ROOT) ?: ""
        val cityNorm = city?.trim()?.lowercase(Locale.ROOT) ?: ""

        fun Zone.exact(s: String, d: String) =
            negeri.trim().lowercase(Locale.ROOT) == s &&
            daerah.trim().lowercase(Locale.ROOT) == d

        val exact = zones.find { it.exact(stateNorm, cityNorm) }
        if (exact != null) return exact

        val byState = zones.filter { it.negeri.trim().lowercase(Locale.ROOT) == stateNorm }
        if (byState.size == 1) return byState.first()
        if (byState.size > 1 && cityNorm.isNotEmpty()) {
            val byCity = byState.find { cityNorm.contains(it.daerah.trim().lowercase(Locale.ROOT)) }
            if (byCity != null) return byCity
        }

        val cityInState = zones.find {
            it.negeri.trim().lowercase(Locale.ROOT) == stateNorm &&
            it.daerah.trim().lowercase(Locale.ROOT).contains(cityNorm)
        }
        if (cityInState != null) return cityInState

        val stateInCity = zones.find {
            cityNorm.contains(it.negeri.trim().lowercase(Locale.ROOT))
        }
        if (stateInCity != null) return stateInCity

        val cityInName = zones.find {
            cityNorm.contains(it.daerah.trim().lowercase(Locale.ROOT))
        }
        if (cityInName != null) return cityInName

        return null
    }

    private fun getLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            var loc = Tasks.await(fused.lastLocation, 3, TimeUnit.SECONDS)
            if (loc != null) return loc
            loc = Tasks.await(
                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
                10, TimeUnit.SECONDS
            )
            if (loc != null) return loc
        } catch (_: Exception) { }

        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { return it }
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let { return it }
        } catch (_: Exception) { }

        return null
    }

    private fun detectZoneByIP(zones: List<Zone>): Zone? {
        return try {
            val url = URL("http://ip-api.com/json/")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val json = JSONObject(reader.readText())
            reader.close()

            if (json.optString("status") == "success") {
                val state = json.optString("region", "")
                val city = json.optString("city", "")
                matchZone(zones, state, city)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun detectZone(context: Context, zones: List<Zone>): Zone? {
        val location = getLocation(context)
        if (location != null) {
            val address = getAddress(context, location.latitude, location.longitude)
            if (address != null) {
                val state = address.adminArea ?: address.subAdminArea ?: ""
                val city = address.locality ?: address.subLocality ?: ""
                val zone = matchZone(zones, state, city)
                if (zone != null) return zone
            }
        }
        return detectZoneByIP(zones)
    }
}
