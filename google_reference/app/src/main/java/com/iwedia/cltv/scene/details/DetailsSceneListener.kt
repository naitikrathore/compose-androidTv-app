package com.iwedia.cltv.scene.details

import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import listeners.AsyncDataReceiver
import world.SceneListener

/**
 * DetailsSceneListener
 *
 * @author Aleksandar Milojevic
 */
interface DetailsSceneListener : SceneListener, TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
    /**
     * On button clicked
     * @param name button text
     * @param obj  button object
     */
    fun onButtonClick(buttonType: ButtonType, obj: Any, callback: IAsyncCallback)

    fun onWatchlistAddClicked(tvEvent: TvEvent, callback: IAsyncCallback)

    fun onRecordButtonClicked(tvEvent: TvEvent, callback: IAsyncCallback)

    /**
     * On favorite button pressed
     *
     * @param tvChannel tv channel
     * @param favListIds favorites list ids that tv channel should be added to
     */
    fun onFavoriteButtonPressed(tvChannel: TvChannel, favListIds: ArrayList<String>)

    fun onAudioTrackClicked(audioTrack: IAudioTrack)

    fun onSubtitleTrackClicked(subtitle: ISubtitle)

    /**
     * Request current subtitle track
     */
    fun requestCurrentSubtitleTrack(callback: AsyncDataReceiver<ISubtitle>)

    /**
     * Get available favorites categories
     */
    fun getFavoritesCategories(callback: IAsyncDataCallback<ArrayList<String>>)

    fun isRecordingInProgress(): Boolean

    fun getRecordingInProgressTvChannel(): TvChannel

    fun isSchedulerForRecording(tvEvent: TvEvent): Boolean

    fun isInWatchlist(tvEvent: TvEvent): Boolean

    fun isInRecList(tvEvent: TvEvent): Boolean

    fun isDolby(type: Int): Boolean

    fun isCC(type: Int): Boolean

    fun isAudioDescription(type: Int): Boolean

    fun getAvailableAudioTracks(): MutableList<IAudioTrack>

    fun getAvailableSubtitleTracks(): MutableList<ISubtitle>

    fun getPlaybackPositionPercent(recording: Recording): Double
    fun getRecordingPlaybackPosition(recordingId: Int): Long

    fun getRecordingChannelDisplayNumber(id: Int): String

    fun getFavoriteItemList(tvChannel: TvChannel): ArrayList<String>
    fun setSubtitles(isActive: Boolean)
    fun getCurrentAudioTrack(): IAudioTrack?
    fun getCurrentSubtitleTrack(): ISubtitle?

    fun getPreviousSceneId(): Int
    fun getClosedCaptionSubtitlesState(): Boolean
    fun isSubtitlesEnabled(): Boolean
    fun getIsCC(type: Int): Boolean
    fun getIsAudioDescription(type: Int): Boolean
    fun getIsDolby(type: Int): Boolean
    fun isTeleText(type: Int): Boolean

    fun isClosedCaptionEnabled(): Boolean?
    fun setCCInfo()
    fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int)
    fun getClosedCaption(): String?
    fun setClosedCaption(): Int?
    fun isCCTrackAvailable(): Boolean
    fun getActiveChannel(): TvChannel

    fun getAudioChannelInfo(type: Int): String

    fun isParentalEnabled(): Boolean

    fun getLanguageMapper(): LanguageMapperInterface

    fun getVideoResolution(): String
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    fun getCurrentTime(tvChannel: TvChannel): Long
    fun onWatchlistRemoveClicked(tvEvent: TvEvent, callback: IAsyncCallback)
    fun isCurrentEvent(tvEvent: TvEvent) : Boolean

    fun isGtvMode(): Boolean
    fun getDateTimeFormat(): DateTimeFormat
    fun getConfigInfo(nameOfInfo: String): Boolean
    fun getVideoResolutionForRecoding(recording: Recording): String
    fun getTTX(type: Int): Boolean
    fun isHOH(type: Int): Boolean

    fun getPlatformName(): String
    fun getAudioFormatInfo(): String
}