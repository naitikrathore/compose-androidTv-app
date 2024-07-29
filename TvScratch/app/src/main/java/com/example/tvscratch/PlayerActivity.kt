package com.example.tvscratch

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity :FragmentActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById<PlayerView>(R.id.pvExo)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        val videoURL = intent.getStringExtra("VIDEO_URL") ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        if(videoURL!=null){
            Log.e("nn","${videoURL}")
            val videoUri = Uri.parse(videoURL)
            val mediaItem = MediaItem.fromUri(videoUri)

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    Log.e("PlayerActivity", "Error: ${error.message}", error)
                }
            })
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
