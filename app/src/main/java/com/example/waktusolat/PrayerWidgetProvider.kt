package com.example.waktusolat

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import com.example.waktusolat.api.PrayerApiService
import com.example.waktusolat.api.PrayerTimeV2
import com.google.gson.Gson
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
        refresh(context.applicationContext, appWidgetManager, appWidgetIds.copyOf())
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val app = context.applicationContext
                val manager = AppWidgetManager.getInstance(app)
                val ids = manager.getAppWidgetIds(ComponentName(app, PrayerWidgetProvider::class.java))
                refresh(app, manager, ids)
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            REFRESH_REQUEST,
            Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    companion object {
        const val ACTION_REFRESH = "com.example.waktusolat.WIDGET_REFRESH"

        private const val REFRESH_REQUEST = 4201
        private const val DEFAULT_ZONE = "SGR01"
        private const val SECONDS_PER_DAY = 24L * 60L * 60L
        private const val CLOCK_FORMAT = "hh:mm a"

        private val TZ: TimeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

        private class Slot(
            val columnId: Int,
            val labelId: Int,
            val timeId: Int,
            val isAzan: Boolean,
            val time: (PrayerTimeV2) -> Long
        )

        private class DaySchedule(val today: PrayerTimeV2, val tomorrowFajr: Long)

        private val SLOTS = listOf(
            Slot(R.id.widget_col_subuh, R.id.widget_lbl_subuh, R.id.widget_tv_subuh, true) { it.fajr },
            Slot(R.id.widget_col_syuruk, R.id.widget_lbl_syuruk, R.id.widget_tv_syuruk, false) { it.syuruk },
            Slot(R.id.widget_col_zuhur, R.id.widget_lbl_zuhur, R.id.widget_tv_zuhur, true) { it.dhuhr },
            Slot(R.id.widget_col_asar, R.id.widget_lbl_asar, R.id.widget_tv_asar, true) { it.asr },
            Slot(R.id.widget_col_maghrib, R.id.widget_lbl_maghrib, R.id.widget_tv_maghrib, true) { it.maghrib },
            Slot(R.id.widget_col_isyak, R.id.widget_lbl_isyak, R.id.widget_tv_isyak, true) { it.isha }
        )

        // Imsak is not shown and Syuruk is not an azan, so the countdown only targets azan times.
        private val AZAN_SLOTS = SLOTS.filter { it.isAzan }

        /** Redraws every placed widget; used after times are cached or the theme changes. */
        fun requestUpdate(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, PrayerWidgetProvider::class.java))
            refresh(app, manager, ids)
        }

        private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            Thread {
                try {
                    val schedule = loadSchedule(context) ?: return@Thread
                    render(context, manager, ids, schedule)
                } catch (_: Exception) {
                    // Keep the last rendered widget rather than blanking it out.
                }
            }.start()
        }

        /** Cached month first; the network is only touched when the cache has no entry for today. */
        private fun loadSchedule(context: Context): DaySchedule? {
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val gson = Gson()

            prefs.getString(SharedViewModel.PREFS_CACHED_PRAYERS, null)?.let { cached ->
                try {
                    val prayers = gson.fromJson(cached, Array<PrayerTimeV2>::class.java)?.toList()
                    if (prayers != null) {
                        todaySchedule(prayers)?.let { return it }
                    }
                } catch (_: Exception) {
                }
            }

            val zoneCode = prefs.getString("zone_code", DEFAULT_ZONE) ?: DEFAULT_ZONE
            val response = PrayerApiService.create()
                .getPrayerTimesV2Sync(zoneCode)
                .execute()
                .body() ?: return null
            val schedule = todaySchedule(response.prayers) ?: return null

            prefs.edit()
                .putString(SharedViewModel.PREFS_CACHED_PRAYERS, gson.toJson(response.prayers))
                .putInt(SharedViewModel.PREFS_CACHED_TODAY, schedule.today.day)
                .putString(SharedViewModel.PREFS_CACHED_ZONE, zoneCode)
                .apply()

            return schedule
        }

        private fun todaySchedule(prayers: List<PrayerTimeV2>): DaySchedule? {
            val now = Calendar.getInstance(TZ)
            val today = prayers.firstOrNull { sameDay(it.fajr, now) } ?: return null

            val tomorrow = Calendar.getInstance(TZ).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowFajr = prayers.firstOrNull { sameDay(it.fajr, tomorrow) }?.fajr
                ?: (today.fajr + SECONDS_PER_DAY)

            return DaySchedule(today, tomorrowFajr)
        }

        private fun sameDay(epochSeconds: Long, reference: Calendar): Boolean {
            val cal = Calendar.getInstance(TZ).apply { timeInMillis = epochSeconds * 1000L }
            return cal.get(Calendar.YEAR) == reference.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == reference.get(Calendar.DAY_OF_YEAR)
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            schedule: DaySchedule
        ) {
            val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            val zoneCode = prefs.getString("zone_code", DEFAULT_ZONE) ?: DEFAULT_ZONE
            val accent = accentColor(context)
            val prayers = schedule.today

            val nowSeconds = System.currentTimeMillis() / 1000L
            val nextSlot = AZAN_SLOTS.firstOrNull { it.time(prayers) > nowSeconds }
            // After Isyak the countdown rolls over to tomorrow's Subuh.
            val targetMillis = (if (nextSlot != null) nextSlot.time(prayers) else schedule.tomorrowFajr) * 1000L

            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("ms", "MY"))
                .apply { timeZone = TZ }
            val clockFormat = SimpleDateFormat(CLOCK_FORMAT, Locale.ENGLISH)
                .apply { timeZone = TZ }

            val dateText = dateFormat.format(Date())
            val hijriText = hijriText(prayers.hijri)?.let { " · $it" } ?: ""

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)
                views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context, id))

                views.setTextViewText(R.id.widget_tv_date, dateText)
                views.setTextViewText(R.id.widget_tv_hijri, hijriText)
                views.setTextViewText(R.id.widget_tv_zone, zoneCode)

                views.setTextViewText(R.id.widget_tv_subuh, formatClock(clockFormat, prayers.fajr))
                views.setTextViewText(R.id.widget_tv_syuruk, formatClock(clockFormat, prayers.syuruk))
                views.setTextViewText(R.id.widget_tv_zuhur, formatClock(clockFormat, prayers.dhuhr))
                views.setTextViewText(R.id.widget_tv_asar, formatClock(clockFormat, prayers.asr))
                views.setTextViewText(R.id.widget_tv_maghrib, formatClock(clockFormat, prayers.maghrib))
                views.setTextViewText(R.id.widget_tv_isyak, formatClock(clockFormat, prayers.isha))

                for (slot in SLOTS) {
                    views.setInt(slot.columnId, "setBackgroundColor", Color.TRANSPARENT)
                    views.setTextColor(slot.labelId, Color.WHITE)
                    views.setTextColor(slot.timeId, Color.WHITE)
                }
                if (nextSlot != null) {
                    views.setInt(nextSlot.columnId, "setBackgroundResource", R.drawable.bg_widget_next)
                    views.setTextColor(nextSlot.labelId, accent)
                    views.setTextColor(nextSlot.timeId, accent)
                }

                val remaining = (targetMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                views.setChronometerCountDown(R.id.widget_tv_next_timer, true)
                views.setChronometer(
                    R.id.widget_tv_next_timer,
                    SystemClock.elapsedRealtime() + remaining,
                    "%s",
                    true
                )
                views.setTextColor(R.id.widget_tv_next_timer, accent)

                manager.updateAppWidget(id, views)
            }

            scheduleNextRefresh(context, targetMillis)
        }

        private fun formatClock(format: SimpleDateFormat, epochSeconds: Long): String =
            format.format(Date(epochSeconds * 1000L))

        private fun hijriText(hijri: String): String? {
            val parts = hijri.split("-")
            if (parts.size != 3) return null
            val month = getHijriMonth(parts[1].toIntOrNull() ?: return null)
            val day = parts[2].trimStart('0').ifEmpty { "0" }
            return "$day $month ${parts[0]}H"
        }

        private fun accentColor(context: Context): Int {
            val prefs = context.getSharedPreferences(ThemeConfig.PREFS_KEY, Context.MODE_PRIVATE)
            val index = prefs.getInt(ThemeConfig.PREFS_THEME_INDEX, 0)
                .coerceIn(0, ThemeConfig.themes.size - 1)
            return ThemeConfig.themes[index].accent
        }

        private fun openAppIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_REFRESH)
            return PendingIntent.getBroadcast(
                context,
                REFRESH_REQUEST,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Wakes the widget just after the next azan so the countdown and highlight roll over. */
        private fun scheduleNextRefresh(context: Context, targetMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pending = refreshPendingIntent(context)
            val triggerAt = targetMillis + 2_000L

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                }
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
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
}
