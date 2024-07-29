package com.iwedia.cltv.scene.channel_list

import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import world.SceneListener

/**
 * ChannelListSceneListener
 *
 * @author Aleksandar Milojevic
 */
interface ChannelListSceneListener : SceneListener, TTSSetterInterface,
    ToastInterface, TTSSetterForSelectableViewInterface {
    fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>)
    fun onCategoryChannelClicked(position: Int)

    fun sortChannelList(channelList: MutableList<ChannelListItem>): MutableList<ChannelListItem>
    fun onSearchClicked()
    fun digitPressed(digit: Int)
    fun onChannelItemClicked(tvChannel: TvChannel)
    fun onAddFavoritesClicked(tvChannel: TvChannel, favListIds : ArrayList<String>)
    fun saveSelectedSortListPosition(position: Int)
    fun getSelectedSortListPosition() : Int
    fun onRecordButtonPressed(tvEvent: TvEvent)
    fun onChannelListEmpty()
    fun getActiveChannel(): TvChannel

    //Replaced with playerInterface change
    fun getAvailableAudioTracks() : List<IAudioTrack>
    fun getAvailableSubtitleTracks() : List<ISubtitle>

    fun addDeletedChannel(tvChannel: TvChannel)
    fun deleteChannel(tvChannel: TvChannel): Boolean
    fun lockUnlockChannel(tvChannel: TvChannel, lockUnlock: Boolean, callback: IAsyncCallback)
    fun skipUnskipChannel(tvChannel: TvChannel, skipUnskip: Boolean): Boolean
    fun getFavoritesCategory(callback: IAsyncDataCallback<ArrayList<String>>)
    // Track options
    fun getIsAudioDescription(type:Int): Boolean
    fun getIsDolby(type: Int): Boolean
    fun getIsHOH(type: Int): Boolean
    fun getDolbyType(type: Int, trackId: String): String
    fun getTeleText(type: Int): Boolean
    fun isParentalEnabled(): Boolean

    fun getChannelList(): ArrayList<TvChannel>

    fun getWatchlist(): MutableList<ScheduledReminder>?

    fun removeScheduledReminder(reminder: ScheduledReminder)

    fun onActiveChannelDeleted()

    fun isClosedCaptionEnabled(): Boolean?
    fun getClosedCaption(): String?
    fun setClosedCaption(): Int?
    fun getAudioChannelInfo(type: Int): String
    fun getAudioFormatInfo():String
    fun getVideoResolution(): String
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    fun getCurrentTime(tvChannel: TvChannel): Long
    fun getChannelSourceType(tvChannel: TvChannel): String
    fun getCurrentAudioTrack(): IAudioTrack?
    fun isCCTrackAvailable(): Boolean
    fun getInstalledRegion(): Region
    fun getActiveCategory(): String
    fun getDateTimeFormat(): DateTimeFormat
    fun isPvrPathSet(): Boolean
    fun isUsbFreeSpaceAvailable(): Boolean
    fun isUsbStorageAvailable(): Boolean
    fun isEventLocked(tvEvent: TvEvent?): Boolean
    fun isUsbWritableReadable(): Boolean
    fun onClickEditChannel()
    fun getConfigInfo(nameOfInfo: String): Boolean
    fun showRecordingStopPopUp(callback: IAsyncCallback)
    fun getCurrentSubtitleTrack() : ISubtitle?
    fun isSubtitlesEnabled() : Boolean
    fun stopRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback)
    fun isScrambled(): Boolean
}