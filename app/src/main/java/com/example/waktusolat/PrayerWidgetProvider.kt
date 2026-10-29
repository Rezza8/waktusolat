package com.example.waktusolat

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.waktusolat.api.PrayerApiService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            appWidgetManager.updateAppWidget(id, views)
        }

        Thread {
            try {
                val api = PrayerApiService.create()
                val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                val zoneCode = prefs.getString("zone_code", "SGR01") ?: "SGR01"

                val call = api.getPrayerTimesV2Sync(zoneCode)
                val response = call.execute().body() ?: return@Thread

                val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                val todayPrayer = response.prayers.find { it.day == today } ?: return@Thread

                val tz = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

                val timeFmt = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
                    timeZone = tz
                }
                val fmt: (Long) -> String = { timeFmt.format(Date(it * 1000)) }

                val dateFmt = SimpleDateFormat("d MMM yyyy", Locale("ms", "MY")).apply {
                    timeZone = tz
                }
                val gregDate = dateFmt.format(Date())

                val hijriParts = todayPrayer.hijri.split("-")
                val hijriDate = if (hijriParts.size == 3) {
                    val day = hijriParts[2].trimStart('0')
                    val month = getHijriMonth(hijriParts[1].toIntOrNull() ?: 1)
                    val year = hijriParts[0]
                    "$day $month ${year}H"
                } else null

                val dateText = gregDate
                val hijriText = if (hijriDate != null) " · $hijriDate" else ""

                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openPending = PendingIntent.getActivity(
                        context, id, openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openPending)

                    views.setTextViewText(R.id.widget_tv_subuh, fmt(todayPrayer.fajr))
                    views.setTextViewText(R.id.widget_tv_syuruk, fmt(todayPrayer.syuruk))
                    views.setTextViewText(R.id.widget_tv_zuhur, fmt(todayPrayer.dhuhr))
                    views.setTextViewText(R.id.widget_tv_asar, fmt(todayPrayer.asr))
                    views.setTextViewText(R.id.widget_tv_maghrib, fmt(todayPrayer.maghrib))
                    views.setTextViewText(R.id.widget_tv_isyak, fmt(todayPrayer.isha))
                    val zoneCode = prefs.getString("zone_code", "SGR01") ?: "SGR01"
                    val zoneName = prefs.getString("zone_display", "") ?: ""
                    val zoneDisplay = if (zoneName.isNotEmpty()) "$zoneName ($zoneCode)" else zoneCode
                    views.setTextViewText(R.id.widget_tv_zone, zoneDisplay)
                    views.setTextViewText(R.id.widget_tv_date, dateText)
                    views.setTextViewText(R.id.widget_tv_hijri, hijriText)

                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun getHijriMonth(month: Int): String {
        return when (month) {
            1 -> "Muharam"
            2 -> "Safar"
            3 -> "Rabiulawal"
            4 -> "Rabiulakhir"
            5 -> "Jamadilawal"
            6 -> "Jamadilakhir"
            7 -> "Rejab"
            8 -> "Syaaban"
            9 -> "Ramadan"
            10 -> "Syawal"
            11 -> "Zulkaedah"
            12 -> "Zulhijjah"
            else -> ""
        }
    }
}
