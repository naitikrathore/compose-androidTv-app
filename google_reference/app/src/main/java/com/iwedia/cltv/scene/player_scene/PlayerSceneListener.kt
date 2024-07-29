package com.iwedia.cltv.scene.player_scene

import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import world.SceneListener

/**
 * PlayerSceneListener
 *
 * @author Aleksandar Milojevic
 */
interface PlayerSceneListener : SceneListener, TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
    /**
     * On play pause button clicked
     */
    fun onPlayPauseClicked()

    /**
     * On previous button clicked
     */
    fun onPreviousClicked(isLongPress : Boolean)

    /**
     * On next button clicked
     */
    fun onNextClicked(isLongPress : Boolean)

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
    fun isPlayerActiveScene(): Boolean

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
     * Exit recordings on App Pause
     */
    fun exitRecordings()

    /**
     * Request Home scene
     */
    fun showHomeScene()
    fun getAvailableSubtitleTracks(): MutableList<ISubtitle>
    fun getAvailableAudioTracks(): MutableList<IAudioTrack>
    fun setSubtitles(isActive: Boolean)
    fun getCurrentAudioTrack() : IAudioTrack?
    fun getCurrentSubtitleTrack(): ISubtitle?

    fun getIsCC(type: Int): Boolean
    fun getIsAudioDescription(type: Int): Boolean
    fun getTeleText(type: Int): Boolean
    fun getIsDolby(type: Int): Boolean
    fun getLanguageMapper(): LanguageMapperInterface
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent?): String
    fun showPvrPlaybackExitDialog(channelUp: Boolean?)

    fun pvrPlaybackExit(channelUp: Boolean?)
    fun onPauseClicked()

    fun getVideoResolution(): String

    fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>)
    fun getConfigInfo(nameOfInfo: String): Boolean
}