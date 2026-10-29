package com.example.waktusolat

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.waktusolat.api.PrayerTimeV2
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread

object PrayerNotificationHelper {
    const val CHANNEL_ID = "prayer_times"
    const val ACTION_AZAN_ALARM = "com.example.waktusolat.AZAN_ALARM"
    const val EXTRA_PRAYER_NAME = "prayer_name"
    const val EXTRA_PRAYER_TIME = "prayer_time"
    const val PREFS_NOTIFICATION = "notification_enabled"
    const val PREFS_AZAN_TYPE = "azan_type"
    const val PREFS_CUSTOM_AZAN_URI = "custom_azan_uri"
    const val NOTIFICATION_ID_AZAN_SERVICE = 1001
    const val AZAN_MEKAH_URL = "https://www.islamcan.com/audio/adhan/azan-makkah.mp3"
    const val AZAN_DIR = "waktu solat"
    const val AZAN_PENDEK_FILENAME = "azan mekah short.mp3"
    const val AZAN_PANJANG_FILENAME = "azan mekah long.mp3"

    val PRAYER_NAMES = listOf("Imsak", "Subuh", "Syuruk", "Zuhur", "Asar", "Maghrib", "Isyak")

    fun getAzanTypeKey(prayerName: String): String =
        "azan_type_${prayerName.lowercase()}"

    fun getCustomUriKey(prayerName: String): String =
        "custom_uri_${prayerName.lowercase()}"

    fun getMuteKey(prayerName: String): String =
        "mute_${prayerName.lowercase()}"

    fun migrateOldSettings(prefs: android.content.SharedPreferences) {
        if (prefs.contains(PREFS_AZAN_TYPE)) {
            val oldType = prefs.getString(PREFS_AZAN_TYPE, "short") ?: "short"
            val oldUri = prefs.getString(PREFS_CUSTOM_AZAN_URI, null)
            prefs.edit().apply {
                for (name in PRAYER_NAMES) {
                    putString(getAzanTypeKey(name), oldType)
                    if (oldUri != null) {
                        putString(getCustomUriKey(name), oldUri)
                    }
                }
                remove(PREFS_AZAN_TYPE)
                remove(PREFS_CUSTOM_AZAN_URI)
                apply()
            }
        }
    }

    fun getAzanFile(context: Context, filename: String): File? {
        val externalDir = File(context.getExternalFilesDir(null), AZAN_DIR)
        val externalFile = File(externalDir, filename)
        if (externalFile.exists()) return externalFile

        val internalDir = File(context.filesDir, AZAN_DIR)
        val internalFile = File(internalDir, filename)
        if (internalFile.exists()) return internalFile

        val rawName = filename.removeSuffix(".mp3")
        val safeName = rawName.replace(" ", "_")
        var resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        if (resId == 0) {
            resId = context.resources.getIdentifier(safeName, "raw", context.packageName)
        }
        if (resId != 0) {
            if (!internalDir.exists()) internalDir.mkdirs()
            try {
                context.resources.openRawResource(resId).use { input ->
                    FileOutputStream(internalFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (internalFile.exists()) return internalFile
            } catch (_: Exception) { }
        }

        return null
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Waktu Solat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan waktu solat dengan azan"
                enableVibration(true)
                setShowBadge(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleAlarms(context: Context, prayers: List<PrayerTimeV2>, today: Int) {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREFS_NOTIFICATION, true)) return

        cancelAlarms(context)

        val todayPrayer = prayers.find { it.day == today } ?: return

        val times = listOf(
            "Subuh" to todayPrayer.fajr,
            "Syuruk" to todayPrayer.syuruk,
            "Zuhur" to todayPrayer.dhuhr,
            "Asar" to todayPrayer.asr,
            "Maghrib" to todayPrayer.maghrib,
            "Isyak" to todayPrayer.isha
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis() / 1000

        for ((name, epoch) in times) {
            if (epoch <= now) continue

            val intent = Intent(context, AzanAlarmReceiver::class.java).apply {
                action = ACTION_AZAN_ALARM
                putExtra(EXTRA_PRAYER_NAME, name)
                putExtra(EXTRA_PRAYER_TIME, epoch)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmInfo = AlarmManager.AlarmClockInfo(epoch * 1000, null)
            alarmManager.setAlarmClock(alarmInfo, pendingIntent)
        }
    }

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        for (name in PRAYER_NAMES) {
            val intent = Intent(context, AzanAlarmReceiver::class.java).apply {
                action = ACTION_AZAN_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showPrayerNotification(context: Context, prayerName: String, azanType: String) {
        createNotificationChannel(context)

        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(PREFS_NOTIFICATION, true)) return

        // Check per-prayer mute — no notification at all
        val muteKey = getMuteKey(prayerName)
        if (prefs.getBoolean(muteKey, false)) {
            return
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_azan_notification)
            .setContentTitle("Waktu Solat")
            .setContentText("Sudah masuk waktu $prayerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        if (prayerName == "Imsak") {
            builder.setVibrate(longArrayOf(0, 200, 100, 200))
        } else if (azanType == "short") {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
            playShortAzan(context)
        }

        val notification = builder.build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(prayerName.hashCode(), notification)
    }

    private fun playShortAzan(context: Context) {
        thread {
            var mp: MediaPlayer? = null
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioManager.requestAudioFocus(
                        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                }

                val file = getAzanFile(context, AZAN_PENDEK_FILENAME)
                mp = if (file != null && file.exists()) {
                    MediaPlayer().apply {
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                        setDataSource(file.absolutePath)
                        prepare()
                    }
                } else {
                    val resId = context.resources.getIdentifier("azan_mekah_short", "raw", context.packageName)
                    if (resId != 0) MediaPlayer.create(context, resId)
                    else MediaPlayer.create(context, Settings.System.DEFAULT_ALARM_ALERT_URI)
                }
                
                mp?.apply {
                    setOnCompletionListener { 
                        release()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            audioManager.abandonAudioFocusRequest(
                                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build()
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            audioManager.abandonAudioFocus(null)
                        }
                    }
                    setOnErrorListener { _, _, _ -> release(); true }
                    start()
                }
            } catch (e: Exception) {
                mp?.release()
            }
        }
    }

    fun downloadAzanIfNeeded(context: Context): File? {
        val localPanjang = getAzanFile(context, AZAN_PANJANG_FILENAME)
        if (localPanjang != null && localPanjang.exists()) return localPanjang

        val file = File(context.cacheDir, "azan_makkah.mp3")
        if (file.exists() && file.length() > 0) return file

        return try {
            val url = URL(AZAN_MEKAH_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            if (file.exists() && file.length() > 0) file else null
        } catch (_: Exception) {
            null
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
