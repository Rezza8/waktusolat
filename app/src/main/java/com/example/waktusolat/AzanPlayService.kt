package com.example.waktusolat

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlin.concurrent.thread
import androidx.core.app.NotificationCompat

class AzanPlayService : Service() {
    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var currentPrayerName: String = ""

    override fun onCreate() {
        super.onCreate()
        PrayerNotificationHelper.createNotificationChannel(this)
        createAzanChannels()
    }

    private fun createAzanChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm.createNotificationChannel(android.app.NotificationChannel(
                AZAN_CHANNEL_ID, "Azan", android.app.NotificationManager.IMPORTANCE_NONE
            ).apply {
                description = "Pemberitahuan azan semasa dimainkan"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            })
            nm.createNotificationChannel(android.app.NotificationChannel(
                AZAN_VISIBLE_CHANNEL_ID, "Azan", android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Pemberitahuan azan semasa dimainkan"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentPrayerName = intent?.getStringExtra(PrayerNotificationHelper.EXTRA_PRAYER_NAME) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_STOP_AZAN) {
            stopAzan()
            return START_NOT_STICKY
        }

        val notification = createNotification(currentPrayerName)
        startForeground(PrayerNotificationHelper.NOTIFICATION_ID_AZAN_SERVICE, notification)

        thread { playAzan() }

        return START_NOT_STICKY
    }

    private fun createNotification(prayerName: String): Notification {
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val showNotif = prefs.getBoolean(PREFS_SHOW_AZAN_NOTIF, true)
        val channelId = if (showNotif) AZAN_VISIBLE_CHANNEL_ID else AZAN_CHANNEL_ID

        val stopIntent = Intent(this, AzanPlayService::class.java).apply {
            action = ACTION_STOP_AZAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_azan_notification)
            .setContentTitle("Waktu Solat")
            .setContentText("$prayerName - Azan sedang dimainkan")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_azan_notification, "Tutup", stopPendingIntent)
            .build()
    }

    private fun playAzan() {
        var mp: MediaPlayer? = null
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.requestAudioFocus(
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build()
                )
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN)
            }

            val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
            
            // Check per-prayer mute
            val muteKey = PrayerNotificationHelper.getMuteKey(currentPrayerName)
            if (prefs.getBoolean(muteKey, false)) {
                stopSelf()
                return
            }

            val azanTypeKey = PrayerNotificationHelper.getAzanTypeKey(currentPrayerName)
            val azanType = prefs.getString(azanTypeKey, "long") ?: "long"
            val customUriKey = PrayerNotificationHelper.getCustomUriKey(currentPrayerName)
            val customUriStr = prefs.getString(customUriKey, null)

            if (azanType == "custom" && !customUriStr.isNullOrBlank()) {
                val uri = Uri.parse(customUriStr)
                mp = MediaPlayer.create(this, uri)
                mp?.apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setOnCompletionListener { stopSelf() }
                    setOnErrorListener { _, _, _ -> stopSelf(); true }
                    start()
                } ?: stopSelf()
                return
            }

            val file = PrayerNotificationHelper.getAzanFile(this, PrayerNotificationHelper.AZAN_PANJANG_FILENAME)
            if (file != null && file.exists()) {
                mp = MediaPlayer().apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setDataSource(file.absolutePath)
                    setOnPreparedListener { start() }
                    setOnCompletionListener { stopSelf() }
                    setOnErrorListener { _, _, _ -> stopSelf(); true }
                    prepareAsync()
                }
                return
            }

            val resId = resources.getIdentifier("azan_makkah", "raw", packageName)
            if (resId != 0) {
                mp = MediaPlayer.create(this, resId).apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setOnCompletionListener { stopSelf() }
                    setOnErrorListener { _, _, _ -> stopSelf(); true }
                    start()
                }
                return
            }

            mp = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).apply {
                setAudioStreamType(AudioManager.STREAM_ALARM)
                setOnCompletionListener { stopSelf() }
                setOnErrorListener { _, _, _ -> stopSelf(); true }
                start()
            }
        } catch (e: Exception) {
            mp?.release()
            stopSelf()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        stopSelf()
    }

    private fun stopAzan() {
        stopPlayback()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP_AZAN = "com.example.waktusolat.STOP_AZAN"
        const val PREFS_SHOW_AZAN_NOTIF = "show_azan_playback_notif"
        private const val AZAN_CHANNEL_ID = "azan_playback"
        private const val AZAN_VISIBLE_CHANNEL_ID = "azan_playback_visible"
    }
}
