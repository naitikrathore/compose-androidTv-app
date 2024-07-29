package com.iwedia.cltv.scene.infoBanner

import android.media.tv.TvTrackInfo
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
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import listeners.AsyncDataReceiver
import world.SceneListener

/**
 * Info banner scene listener
 *
 * @author Aleksandar Lazic
 */
interface InfoBannerSceneListener : SceneListener, TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
    fun getIsCC(type: Int): Boolean
    fun getIsAudioDescription(type: Int): Boolean
    fun getTeleText(type: Int): Boolean
    fun getIsDolby(type: Int): Boolean
    fun isHOH(type: Int): Boolean
    fun getIsInReclist(tvEvent: TvEvent): Boolean
    fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>)
    fun getActiveChannel(): TvChannel
    fun getRecordingInProgressTvChannel(): TvChannel
    fun recordingInProgress(): Boolean
    fun showDetailsScene(tvEvent: TvEvent)
    fun onChannelDownPressed()
    fun onChannelUpPressed()
    fun playCurrentEvent(tvChannel: TvChannel)
    fun onKeyboardClicked()
    fun onRecordButtonClicked(tvEvent: TvEvent, callback: IAsyncCallback)
    fun onAudioTrackClicked(audioTrack: IAudioTrack)
    fun onSubtitleTrackClicked(subtitleTrack: ISubtitle)
    fun isAudioSubtitleButtonPressed(): Boolean
    fun requestCurrentSubtitleTrack(callback: AsyncDataReceiver<ISubtitle>)
    fun addToWatchlist(tvEvent: TvEvent, callback: IAsyncCallback)
    fun removeFromWatchlist(tvEvent: TvEvent, callback: IAsyncCallback)
    fun onWatchlistClicked(tvEvent: TvEvent, callback: IAsyncCallback)
    fun hasScheduledReminder(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>)
    fun hasScheduledRecording(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>)
    fun getCurrentAudioTrack(): IAudioTrack?
    fun getCurrentSubtitleTrack(): ISubtitle?
    fun getAvailableAudioTracks(): MutableList<IAudioTrack>?
    fun getAvailableSubtitleTracks(): MutableList<ISubtitle>?
    fun setSubtitles(isActive: Boolean)
    fun getClosedCaptionSubtitlesState(): Boolean?
    fun isSubtitlesEnabled(): Boolean
    fun isClosedCaptionEnabled(): Boolean?
    fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int)
    fun getClosedCaption(): String?
    fun setClosedCaption(): Int?
    fun isCCTrackAvailable(): Boolean?
    fun isInWatchlist(tvEvent: TvEvent): Boolean
    fun getChannelSourceType(tvChannel: TvChannel): String
    fun isParentalOn() : Boolean
    fun getAudioChannelInfo(type: Int): String
    fun refreshData(tvChannel: TvChannel)
    fun getLanguageMapper(): LanguageMapperInterface
    fun getVideoResolution(): String
    fun isParentalControlsEnabled(): Boolean
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    fun getCurrentTime(tvChannel: TvChannel): Long
    fun isCurrentEvent(tvEvent: TvEvent) : Boolean
    fun getDateTimeFormat(): DateTimeFormat
    fun isPvrPathSet(): Boolean
    fun isUsbFreeSpaceAvailable(): Boolean
    fun isUsbStorageAvailable(): Boolean
    fun isEventLocked(tvEvent: TvEvent?): Boolean
    fun isUsbWritableReadable(): Boolean
    fun getConfigInfo(nameOfInfo: String): Boolean
    fun getPlatformName(): String
    fun getAudioFormatInfo(): String
    fun isScrambled(): Boolean
    fun defaultAudioClicked()

}