package com.example.waktusolat

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.waktusolat.api.PrayerTimeV2
import com.google.gson.reflect.TypeToken

class KeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            reschedulePrayerAlarms()
            handler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler.post(heartbeatRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Waktu Solat")
            .setSmallIcon(R.drawable.ic_azan_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) { }

        scheduleHeartbeat()

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(heartbeatRunnable)
        cancelHeartbeat()
        super.onDestroy()
    }

    private fun scheduleHeartbeat() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, KeepAliveService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, HEARTBEAT_INTENT_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + HEARTBEAT_INTERVAL_MS,
            pendingIntent
        )
    }

    private fun cancelHeartbeat() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, KeepAliveService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, HEARTBEAT_INTENT_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun reschedulePrayerAlarms() {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(SharedViewModel.PREFS_CACHED_PRAYERS, null) ?: return

        try {
            val type = object : TypeToken<List<PrayerTimeV2>>() {}.type
            val prayers: List<PrayerTimeV2> = com.google.gson.Gson().fromJson(cachedJson, type)
            val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            PrayerNotificationHelper.scheduleAlarms(this, prayers, today)
        } catch (_: Exception) { }
    }

    companion object {
        private const val CHANNEL_ID = "keep_alive_channel"
        private const val NOTIFICATION_ID = 2002
        private const val HEARTBEAT_INTERVAL_MS = 15 * 60 * 1000L
        private const val HEARTBEAT_INTENT_CODE = 3003

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    CHANNEL_ID,
                    "Perkhidmatan Latar",
                    NotificationManager.IMPORTANCE_NONE
                ).apply {
                    description = "Memastikan aplikasi terus berjalan"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                val nm = context.getSystemService(NotificationManager::class.java)
                nm.createNotificationChannel(channel)
            }
        }

        fun start(context: Context) {
            createChannel(context)
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
