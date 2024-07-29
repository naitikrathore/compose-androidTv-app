package com.example.videoplayer.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.videoplayer.databinding.ActivityVideoBinding

class VideoActivity : AppCompatActivity() {
    private lateinit var binding:ActivityVideoBinding
    private lateinit var player:ExoPlayer
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0
    private var playWhenReady: Boolean = true
//    private lateinit var mediaSession: MediaSession

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("actlife", "onCreateVideo")
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            0
        )


        var link=intent.getStringExtra("videoUrl")
        var curlink="http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        player=ExoPlayer.Builder(this).build()
        binding.pvExo.player=player

        if (savedInstanceState != null) {
            playbackPosition = savedInstanceState.getLong("playbackPosition", 0)
            currentWindow = savedInstanceState.getInt("currentWindow", 0)
            playWhenReady = savedInstanceState.getBoolean("playWhenReady", true)
        }

        if(link!=null){
            val serviceIntent = Intent(this, PlayerService::class.java).apply {
                putExtra("videoUrl", curlink)
                action = PlayerService.Actions.START.toString()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            }else{
                startService(serviceIntent)
            }
//            Log.e("actlife", link)
            val videoUri = Uri.parse(curlink)
            Log.e("nait","${videoUri}")

            val mediaItem = MediaItem.fromUri(videoUri)
            player.setMediaItem(mediaItem)
            player.prepare()

            player.seekTo(currentWindow, playbackPosition)
            player.playWhenReady = playWhenReady

//            player.playWhenReady=true
            player.addListener(object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (!playWhenReady) {
                        val pauseIntent = Intent(this@VideoActivity, PlayerService::class.java).apply {
                            action = PlayerService.Actions.PAUSE.toString()
                        }
                        startService(pauseIntent)
                    }
                }
            })
        }else{
            finish()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save player state to restore later
        outState.putLong("playbackPosition", player.currentPosition)
        outState.putInt("currentWindow", player.currentWindowIndex)
        outState.putBoolean("playWhenReady", player.playWhenReady)
        Log.d("actlife", "onSaveInstanceState: Saving state")
    }
}