package com.iwedia.cltv.platform.mal_service.player

import android.media.tv.TvContentRating
import android.os.Bundle
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface.PlayerListener
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

abstract class Player<T: TrackManager<*, *>>(
    protected var trackManager: T
): PlayerInterface {

    protected val mListeners: MutableSet<PlayerListener> =
        Collections.newSetFromMap(ConcurrentHashMap(1))

    override lateinit var activePlayableItem: Any
    override var playerState: PlayerState = PlayerState.IDLE
    override var isTimeShiftAvailable = false

    override fun getSubtitleTracks(applicationMode: ApplicationMode) = trackManager.getSubtitleTracks()
    override fun getAudioTracks() = trackManager.getAudioTracks()
    override fun getActiveSubtitle() = trackManager.currentSubtitleTrack
    override fun getActiveAudioTrack() = trackManager.currentAudioTrack

    override fun registerListener(listener: PlayerListener) { mListeners.add(listener) }
    override fun unregisterListener(listener: PlayerListener) { mListeners.remove(listener) }
}

fun MutableSet<PlayerListener>.onNoPlayback() = forEach {
    it.onNoPlayback()
}

fun MutableSet<PlayerListener>.onPlaybackStarted() = forEach {
    it.onPlaybackStarted()
}

fun MutableSet<PlayerListener>.onAudioTrackUpdated(audioTracks: List<IAudioTrack>) = forEach {
    it.onAudioTrackUpdated(audioTracks)
}

fun MutableSet<PlayerListener>.onSubtitleTrackUpdated(subtitleTracks: List<ISubtitle>) = forEach {
    it.onSubtitleTrackUpdated(subtitleTracks)
}

fun MutableSet<PlayerListener>.onVideoAvailable(inputId: String) = forEach {
    it.onVideoAvailable(inputId)
}
fun MutableSet<PlayerListener>.onVideoUnAvailable(reason: Int, inputId: String) = forEach {
    it.onVideoUnAvailable(reason,inputId)
}

fun MutableSet<PlayerListener>.onContentAvailable() = forEach {
    it.onContentAvailable()
}

fun MutableSet<PlayerListener>.onContentBlocked(rating: TvContentRating) = forEach {
    it.onContentBlocked(rating)
}
fun MutableSet<PlayerListener>.onTimeShiftStatusChanged(inputId: String, status: Boolean) = forEach {
    it.onTimeShiftStatusChanged(inputId, status)
}

fun MutableSet<PlayerListener>.onEvent(inputId: String, eventType: String, eventArgs: Bundle) = forEach {
    it.onEvent(inputId, eventType, eventArgs)
}
fun MutableSet<PlayerListener>.onTrackSelected(inputId: String, eventType: Int, trackId: String?) = forEach {
    it.onTrackSelected(inputId, eventType, trackId)
}