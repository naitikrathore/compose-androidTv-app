package com.iwedia.ui.neon.media

/**
 * @interface IPlayerStateListener
 * @description Example interface for sending playback status callbacks from
 * the [DummyPlayer] back to MediaSession
 */
interface IPlayerStateListener {
  fun onPlaybackStart()
  fun onPlaybackPause()
  fun onPlaybackStop()
  fun onSeekCompleted()
  fun onPlaybackPosition(position: Long)
}