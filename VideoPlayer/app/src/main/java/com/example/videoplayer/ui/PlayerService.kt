package com.example.videoplayer.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.videoplayer.R

class PlayerService : Service() {

    private lateinit var player: ExoPlayer
    private var notificationId = 1 // Unique ID for the notification
    private val channelId = "media_playback_channel"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.toString() -> start(intent.getStringExtra("videoUrl"))
            Actions.PAUSE.toString() -> pause()
        }
        return START_NOT_STICKY
    }

    enum class Actions {
        START, PAUSE
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        createNotificationChannel()
    }

    private fun start(videoUrl: String?) {
        videoUrl?.let {
            val mediaItem = MediaItem.fromUri(it)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }
        startForeground(notificationId, createNotification())
    }

    private fun pause() {
        if (player.isPlaying) {
            player.playWhenReady = false
            stopForeground(false)
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ExoPlayer",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, VideoActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_play)
            .setContentTitle("Playing video")
            .setContentText("Bunny Video")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
