package com.iwedia.cltv.scene.timeshift

import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import world.SceneListener

/**
 * Timeshift scene listener
 *
 * @author Dejan Nadj
 */
interface TimeshiftSceneListener: SceneListener, TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
    /**
     * On play pause button clicked
     */
    fun onPlayPauseClicked()

    /**
     * On previous button clicked
     */
    fun onPreviousClicked(isLongPress : Boolean, repeatCount: Int)

    /**
     * On next button clicked
     */
    fun onNextClicked(isLongPress : Boolean, repeatCount: Int)

    /**
     * On seek
     *
     * @param progress
     */
    fun onSeek(progress: Int)

    /**
     * On channel up pressed
     */
    fun onChannelUp()

    /**
     * On channel down pressed
     */
    fun onChannelDown()

    /**
     * On stop button clicked
     */
    fun onStopClicked()

    /**
     * Is close dialog shown
     */
    fun isCloseDialogShown(): Boolean

    /**
     * On audio track clicked
     */
    fun onAudioTrackClicked(audioTrack: IAudioTrack)

    /**
     * On subtitle track clicked
     */
    fun onSubtitleTrackClicked(subtitle: ISubtitle)

    /**
     * On left key pressed
     */
    fun onLeftKeyPressed()

    /**
     * On right key pressed
     */
    fun onRightKeyPressed()

    /**
     * Checks if PlayerScene is current Active Scene
     */
    fun isActiveScene(): Boolean

    /**
     * Request fast-forward time-shift with the given speed.
     */
    fun onFastForward(speed: Int)

    /**
     * Request rewind time-shift with the given speed.
     */
    fun onRewind(speed: Int)

    /**
     * Forced update current program speed to SPEED_FF_1X.
     */
    fun changeSpeed()

    /**
     * Request current subtitle track
     */
    fun requestCurrentSubtitleTrack() : ISubtitle?

    /**
     * Request current audio track
     */
    fun requestCurrentAudioTrack(): IAudioTrack?

    /**
     * Request Home scene
     */
    fun showHomeScene()

    /**
     * Request available subtitle tracks
     */
    fun getAvailableSubtitleTracks(): MutableList<ISubtitle>

    /**
     * Request available audio tracks
     */
    fun getAvailableAudioTracks(): MutableList<IAudioTrack>

    /**
     * Set active subtitle track
     */
    fun setSubtitles(isActive: Boolean)

    /**
     * Request active audio track
     */
    fun getCurrentAudioTrack() : IAudioTrack?

    /**
     * Request active subtitle track
     */
    fun getCurrentSubtitleTrack(): ISubtitle?

    /**
     * Check is cc available
     */
    fun getIsCC(type: Int): Boolean

    /**
     * Check is audio description available
     */
    fun getIsAudioDescription(type: Int): Boolean

    /**
     * Check is teletext available
     */
    fun getTeleText(type: Int): Boolean

    /**
     * Check is dolby available
     */
    fun getIsDolby(type: Int): Boolean

    /**
     * Get language mapper
     */
    fun getLanguageMapper(): LanguageMapperInterface

    /**
     * Set timeshift indicator visibility
     */
    fun setIndicator(show: Boolean)

    /**
     * Check is timeshift started
     */
    fun isTimeshiftStarted(): Boolean

    /**
     * Check is subtitles enabled
     */
    fun isSubtitleEnabled(): Boolean

    /**
     * Check is seeking in progress
     */
    fun isSeeking(): Boolean

    fun isTimeShiftAvailable(): Boolean

    fun stopTimeshift()

    fun isDialogSceneOpenUsb(): Boolean

    fun getVideoResolution(): String

    fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>)
    fun getConfigInfo(nameOfInfo: String): Boolean
}