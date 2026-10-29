package com.example.waktusolat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AzanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(PrayerNotificationHelper.EXTRA_PRAYER_NAME) ?: return

        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PrayerNotificationHelper.PREFS_NOTIFICATION, true)) return

        val notifKey = "notif_${prayerName.lowercase()}"
        if (!prefs.getBoolean(notifKey, true)) return

        val muteKey = PrayerNotificationHelper.getMuteKey(prayerName)
        if (prefs.getBoolean(muteKey, false)) return

        val azanTypeKey = PrayerNotificationHelper.getAzanTypeKey(prayerName)
        val azanType = prefs.getString(azanTypeKey, "short") ?: "short"

        PrayerNotificationHelper.showPrayerNotification(context, prayerName, azanType)

        if (azanType == "long" || azanType == "custom") {
            val serviceIntent = Intent(context, AzanPlayService::class.java).apply {
                putExtra(PrayerNotificationHelper.EXTRA_PRAYER_NAME, prayerName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
