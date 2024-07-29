/**
 * Interface representing a TV player.
 */
package com.iwedia.cltv.platform.`interface`

import android.media.tv.TvContentRating
import android.media.tv.TvTrackInfo
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import kotlin.reflect.KCallable

interface PlayerInterface {

    /**
     * Interface for listening to player events.
     */
    interface PlayerListener {
        /**
         * Called when there is no playback.
         */
        fun onNoPlayback()

        /**
         * Called when playback has started.
         */
        fun onPlaybackStarted()

        /**
         * Called when the list of audio tracks is updated.
         */
        fun onAudioTrackUpdated(audioTracks: List<IAudioTrack>)

        /**
         * Called when the list of subtitle tracks is updated.
         */
        fun onSubtitleTrackUpdated(subtitleTracks: List<ISubtitle>)

        /**
         * Called when video becomes available with a specific input ID.
         */
        fun onVideoAvailable(inputId: String)

        /**
         * Called when video becomes unavailable with a specific reason and input ID.
         */
        fun onVideoUnAvailable(reason: Int, inputId: String)

        /**
         * Called when content becomes available.
         */
        fun onContentAvailable()

        /**
         * Called when content is blocked due to a content rating.
         */
        fun onContentBlocked(rating: TvContentRating)

        /**
         * Called when the time shift status changes for a specific input ID.
         */
        fun onTimeShiftStatusChanged(inputId: String, status: Boolean)

        /**
         * Called when an event with specific arguments occurs for a specific input ID.
         */
        fun onEvent(inputId: String, eventType: String, eventArgs: Bundle)

        /**
         * Called when the track is selected.
         */
        fun onTrackSelected(inputId: String, type: Int, trackId: String?)
    }

    /**
     * The currently active playable item in the player.
     */
    var activePlayableItem: Any

    var liveTabChannel: TvChannel?

    /**
     * The current state of the player.
     */
    var playerState: PlayerState

    /**
     * Indicates whether time shifting is available for the content being played.
     */
    var isTimeShiftAvailable: Boolean

    /**
     * Indicates whether the content being played was scrambled.
     */
    var wasScramble : Boolean

    /**
     * LiveData holding the current playback status of the player.
     */
    var playbackStatus : MutableLiveData<PlaybackStatus>

    /**
     * Indicates whether parental control is active.
     */
    var isParentalActive: Boolean

    /**
     * The content rating that is blocked.
     */
    var blockedRating: TvContentRating?

    /**
     * Indicates whether the channel is unlocked.
     */
    var isChannelUnlocked : Boolean

    /**
     * Indicates whether the player is on the lock screen.
     */
    var isOnLockScreen : Boolean
    /**
     * Indicates whether the parental control is active or not.
     */
    fun getIsParentalControlActive():Boolean
    /**
     * Reference to the service callable.
     */
    var mServiceRef: KCallable<*>?

    // Player control methods

    /**
     * Set the caption (subtitle) display state.
     */
    fun setCaptionEnabled(enabled: Boolean)

    /**
     * Select a specific subtitle track.
     */
    fun selectSubtitle(subtitle: ISubtitle?)

    /**
     * Select a specific audio track.
     */
    fun selectAudioTrack(audioTrack: IAudioTrack)

    /**
     * Get a list of available subtitle tracks.
     */
    fun getSubtitleTracks(applicationMode: ApplicationMode = ApplicationMode.DEFAULT): List<ISubtitle>

    /**
     * Get a list of available audio tracks.
     */
    fun getAudioTracks(): List<IAudioTrack>

    /**
     * Get the currently active subtitle track.
     */
    fun getActiveSubtitle(): ISubtitle?

    /**
     * Get the currently active audio track.
     */
    fun getActiveAudioTrack(): IAudioTrack?

    /**
     * Set the view where playback should be rendered.
     */
    fun setPlaybackView(playbackView: ViewGroup)

    /**
     * Pause playback.
     */
    fun pause()

    /**
     * Start playback with the specified playable item.
     */
    fun play(playableItem: Any)

    /**
     * Reset the player.
     */
    fun reset()

    /**
     * Resume playback.
     */
    fun resume()

    /**
     * Stop playback.
     */
    fun stop()

    /**
     * Mute audio.
     */
    fun mute()

    /**
     * Unmute audio.
     */
    fun unmute()

    /* Time shift api */
    fun seek(positionMs: Long, isRelative: Boolean)

    /**
     * Set the playback speed.
     *
     * @param speed The playback speed to set.
     */
    fun setSpeed(speed: Int)

    /**
     * Get the current playback speed.
     *
     * @return The current playback speed.
     */
    fun getSpeed()

    /**
     * Slow down playback speed.
     */
    fun slowDown()

    /**
     * Speed up playback speed.
     */
    fun speedUp()

    /**
     * Get the duration of the content being played.
     *
     * @return The duration in milliseconds.
     */
    fun getDuration(): Long

    /**
     * Get the current playback position.
     *
     * @return The current position in milliseconds.
     */
    fun getPosition(): Long

    // Listener methods

    /**
     * Register a listener to receive player events.
     */
    fun registerListener(listener: PlayerListener)

    /**
     * Unregister a previously registered player listener.
     */
    fun unregisterListener(listener: PlayerListener)

    // Content unblocking

    /**
     * Request to unblock content with an asynchronous callback.
     *
     * @param callback The callback to be invoked when content is unblocked.
     */
    fun requestUnblockContent(callback: IAsyncCallback)

//    fun timeShiftSeekForward(timeMs: Long)
//    fun timeShiftSeekBackward(timeMs: Long)
//    fun timeShiftSeekTo(timeMs: Long)
//    fun setTimeShiftSpeed(speed: Int)

    /**
     * Get a list of playback tracks for a specified type.
     *
     * @param type The type of playback tracks to retrieve.
     * @return A list of TvTrackInfo objects representing the tracks.
     */
    fun getPlaybackTracks(type : Int) : List<TvTrackInfo>

    /**
     * Check if a track is in Dolby format for a specified type.
     *
     * @param type The type of track.
     * @return `true` if the track is in Dolby format, `false` otherwise.
     */
    fun getIsDolby(type: Int): Boolean

    /**
     * Get the Dolby type for a specific track.
     *
     * @param type The type of track.
     * @param trackId The ID of the track.
     * @return The Dolby type of the track.
     */
    fun getDolbyType(type: Int, trackId: String): String

    /**
     * Check if closed captions (CC) are available for a specified type.
     *
     * @param type The type of content.
     * @return `true` if closed captions are available, `false` otherwise.
     */
    fun getIsCC(type: Int): Boolean

    /**
     * Check if audio description (AD) is available for a specified type.
     *
     * @param type The type of content.
     * @return `true` if audio description is available, `false` otherwise.
     */
    fun getIsAudioDescription(type: Int): Boolean

    /**
     * Check if a TvTrackInfo object represents an audio description track.
     *
     * @param tvTrackInfo The TvTrackInfo object to check.
     * @return `true` if the track is an audio description track, `false` otherwise.
     */
    fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean

    /**
     * Check if teletext is available for a specified type.
     *
     * @param type The type of content.
     * @return `true` if teletext is available, `false` otherwise.
     */
    fun getTeleText(type: Int): Boolean

    /**
     * Get the video resolution of the content being played.
     *
     * @return The video resolution as a string.
     */
    fun getVideoResolution(): String
    /**
     * Get the audio channel index for a specified type.
     *
     * @param type The type of content.
     * @return The audio channel index.
     */
    fun getAudioChannelIndex(type: Int) : Int

    fun getAudioFormat():String
    fun unlockChannel() : Boolean

    fun switchAudioTrack() :String?

    fun setSubtitleSurface(holder: SurfaceHolder?)
    fun setQuietTuneEnabled(enabled : Boolean)
    fun getQuietTuneEnabled() : Boolean

    fun setTeletextSurface(holder: SurfaceHolder?)

    fun performBackgroundTuning(tvChannel: TvChannel)
    fun initTTML(ttmlViewContainer: RelativeLayout)
    fun refreshTTMLStatus()
    fun setTTMLVisibility(isVisible: Boolean)

    fun setDefaultAudioTrack()

    // Track options
    /*fun setSubtitles(view: Boolean)
    fun updateSubtitleCheckStatus(status: Boolean)
    fun getAvailableSubtitleTracks(): MutableList<ReferenceSubtitleTrack>
    fun getAvailableAudioTracks(): MutableList<ReferenceAudioTrack>
    fun resetVideoUnavailable()
    fun updateSubtitleLanguage(language: String)

    fun requestUnblockContent(callback: AsyncReceiver)
   fun getIsAudioDescription(type:Int): Boolean
   fun getIsHOH(type: Int): Boolean
   fun getDolbyType(type: Int, trackId: String): String
   fun getTeleText(type: Int): Boolean  */
    // Display options
    /*  fun isDisplayModeAvailable(displayMode: Int): Boolean
      fun getVideoDisplayAspectRatio(): Float
      fun getDisplayModeFromPref(): Int
      fun getDisplayMode(): Int
      fun storeDisplayMode(displayMode: Int)
      fun setDisplayMode(displayMode: Int, animate: Boolean, storeInPreference: Boolean): Int
      fun getScaleType(): Int
  */

//    inner class ReferenceTimeShiftPositionCallback {...}

}