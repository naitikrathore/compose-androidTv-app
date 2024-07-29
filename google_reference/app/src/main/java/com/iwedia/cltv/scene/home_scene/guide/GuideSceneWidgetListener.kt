package com.iwedia.cltv.scene.home_scene.guide

import com.iwedia.cltv.platform.`interface`.TTSStopperInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import world.widget.GWidgetListener

/**
 * Guide scene widget listener
 *
 * @author Dejan Nadj
 */
interface GuideSceneWidgetListener : GWidgetListener, TTSSetterInterface,
    TTSStopperInterface {

    /**
     * On guide filter selected
     *
     * @param filterId guide filter id
     */
    fun onFilterSelected(filterId: Int)

    /**
     * On catch up button pressed
     *
     * @param tvEvent pressed tv event
     */
    fun onCatchUpButtonPressed(tvEvent: TvEvent)

    /**
     * On watch current event button pressed
     *
     * @param tvChannel tv channel
     */
    fun onWatchButtonPressed(tvChannel: TvChannel)

    /**
     * On record button pressed
     *
     * @param tvEvent pressed tv event
     */
    fun onRecordButtonPressed(tvEvent: TvEvent, callback: IAsyncCallback)

    /**
     * On watchlist button pressed
     *
     * @param tvEvent pressed tv event
     */
    fun onWatchlistButtonPressed(tvEvent: TvEvent) : Boolean

    /**
     * On favorite button pressed
     *
     * @param tvChannel tv channel
     * @param favListIds favorites list ids that tv channel should be added to
     */
    fun onFavoriteButtonPressed(tvChannel: TvChannel, favListIds: ArrayList<String>)

    /**
     * On more info button pressed
     *
     * @param tvEvent tv event
     */
    fun onMoreInfoButtonPressed(tvEvent: TvEvent)

    /**
     * Request focus on home scene top menu
     */
    fun requestFocusOnTopMenu()

    /**
     * set broadcast as active filter.
     */
    fun setGuideButtonAsActiveFilter()

    /**
     * Get content for day with offset
     */
    fun getDayWithOffset(additionalDayCount: Int, isExtend: Boolean,channelList:MutableList<TvChannel>)

    /**
     * On digit button pressed
     * @param digit pressed digit
     * @param epgActiveFilter shows which is the active filter currently
     * @param filterMetadata shows extra info like if its fav then which favorite - fav1, fav 2 etc
     */
    fun onDigitPressed(digit: Int, epgActiveFilter: FilterItemType? = null, filterMetadata: String?=null)

    /**
     * set active filter after guide scene loading complete
     */
    fun setActiveFilterOnGuide()
    fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>)
    /**
     *remove focus form main menu item on digit zap in TV Guide
     */
    fun clearFocusFromMainMenu()
    fun getChannelsOfSelectedFilter(): MutableList<TvChannel>
    fun loadNextChannels(anchorChannel: TvChannel, callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>, currentDayOffset: Int, nextdayoffset: Int, extend: Boolean)
    fun loadPreviousChannels(anchorChannel: TvChannel, callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>, currentDayOffset: Int, nextdayoffset: Int, extend: Boolean)
    fun getEventsForChannels(anchorChannel: TvChannel, epgActiveFilter: Int, callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>, currentDayOffset: Int, nextdayoffset: Int, extend: Boolean)
    fun getRecordingInProgressTvChannel(): TvChannel?
    fun isInWatchList(tvEvent: TvEvent): Boolean
    fun isInRecordingList(tvEvent: TvEvent): Boolean
    fun isInFavoriteList(tvChannel: TvChannel): Boolean
    fun getFavoriteCategories(callback: IAsyncDataCallback<ArrayList<String>>)
    fun getFavoriteSelectedItems(tvChannel: TvChannel): ArrayList<String>

    fun isRecordingInProgress(): Boolean
    fun isParentalEnabled() : Boolean
    fun isClosedCaptionEnabled(): Boolean
    fun isCCTrackAvailable(): Boolean?
    fun getIsAudioDescription(type: Int): Boolean
    fun getIsDolby(type: Int): Boolean
    fun isHOH(type:Int): Boolean
    fun isTeleText(type:Int): Boolean
    fun getAudioChannelInfo(type: Int): String
    fun getAudioFormatInfo():String
    fun getVideoResolution(): String

    fun onInitialized()
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    suspend fun getCurrentTime(tvChannel: TvChannel): Long
    fun isCurrentEvent(tvEvent: TvEvent) : Boolean
    fun getAvailableAudioTracks(): List<IAudioTrack>
    fun getAvailableSubtitleTracks(): List<ISubtitle>
    fun getClosedCaption(): String?
    fun getCurrentAudioTrack(): IAudioTrack?
    fun getActiveEpgFilter(): Int
    fun isAccessibilityEnabled(): Boolean
    fun changeChannelFromTalkback(tvChannel: TvChannel)
    fun fetchEventForChannelList(callback: IAsyncDataCallback<java.util.LinkedHashMap<Int, MutableList<TvEvent>>>, channelList: MutableList<TvChannel>, dayOffset: Int, additionalDayOffset: Int, isExtend: Boolean)

    fun onOptionsClicked()
    fun getDateTimeFormat() : DateTimeFormat
    fun isEventLocked(tvEvent: TvEvent?): Boolean
    fun getCountryPreferences(preference: UtilsInterface.CountryPreference, defaultValue: Any?): Any?
    fun getConfigInfo(nameOfInfo: String): Boolean
    fun getCurrentSubtitleTrack() : ISubtitle?
    fun isSubtitleEnabled() : Boolean
    fun tuneToFocusedChannel(tvChannel: TvChannel)
    fun getActiveFastChannel(callback: IAsyncDataCallback<TvChannel>)
    fun setActiveWindow(tvChannelList: MutableList<TvChannel>, startTime: Long)
    fun getStartTimeForActiveWindow(): Long
    fun getChannelSourceType(tvChannel: TvChannel):String
    fun isScrambled(): Boolean
}