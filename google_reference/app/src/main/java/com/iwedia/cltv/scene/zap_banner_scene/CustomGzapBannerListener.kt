package com.iwedia.cltv.scene.zap_banner_scene

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import world.widget.custom.zap_banner.GZapBannerListener

/**
 * @author Gaurav Jain
 */
interface CustomGzapBannerListener: GZapBannerListener, TTSSetterInterface {
    fun onBackClicked()
    /** zap to channel after long press + */
    fun onNextChannelZapHold()
    /** zap to channel after long press - */
    fun onPreviousZapHold()
    /** show zap banner after channel long press + */
    fun onNextChannelInfoBannerHold()
    /** show zap banner after channel long press - */
    fun onPreviousChannelInfoBannerHold()

    fun getIsCC(type: Int): Boolean
    fun getIsAudioDescription(type: Int): Boolean
    fun getTeleText(type: Int): Boolean
    fun isHOH(type: Int): Boolean
    fun getIsDolby(type: Int): Boolean
    fun isClosedCaptionEnabled(): Boolean?
    fun getClosedCaption(): String?
    fun isCCTrackAvailable(): Boolean?
    fun getAudioChannelInfo(type: Int): String
    fun getAudioFormatInfo():String
    fun getChannelSourceType(tvChannel: TvChannel): String
    fun getVideoResolution(): String
    fun isParentalEnabled() : Boolean
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    fun getCurrentTime(tvChannel: TvChannel): Long
    fun showVirtualKeyboard()
    fun getAvailableAudioTracks(): List<IAudioTrack>
    fun getCurrentAudioTrack(): IAudioTrack?
    fun showInfoBanner()
    fun showHomeScene()
    fun showChannelList()
    fun onLastActiveChannelClicked()
    fun getDateTimeFormat(): DateTimeFormat
    fun isEventLocked(tvEvent: TvEvent?): Boolean
    fun getAvailableSubtitleTracks(): List<ISubtitle>
    fun getCurrentSubtitleTrack(): ISubtitle?
    fun isSubtitleEnabled(): Boolean
    fun getNextEvent(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>)
    fun isScrambled(): Boolean

    }