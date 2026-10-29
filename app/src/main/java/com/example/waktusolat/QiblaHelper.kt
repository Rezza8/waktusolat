package com.example.waktusolat

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaHelper {

    private const val MECCA_LAT = 21.4225
    private const val MECCA_LON = 39.8262

    fun calculateBearing(lat: Double, lon: Double): Float {
        val dLon = Math.toRadians(MECCA_LON - lon)
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(MECCA_LAT)
        val x = sin(dLon) * cos(lat2)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(x, y))
        return ((bearing + 360) % 360).toFloat()
    }

    fun calculateDistance(lat: Double, lon: Double): Double {
        val R = 6371.0
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(MECCA_LAT)
        val dLat = Math.toRadians(MECCA_LAT - lat)
        val dLon = Math.toRadians(MECCA_LON - lon)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun getLastLocation(context: Context): Location? {
        if (!PrayerLocationHelper.hasLocationPermission(context)) return null
        return try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            var loc = Tasks.await(fused.lastLocation, 3, TimeUnit.SECONDS)
            if (loc != null) return loc
            loc = Tasks.await(
                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
                10, TimeUnit.SECONDS
            )
            loc
        } catch (_: Exception) {
            null
        }
    }
}
