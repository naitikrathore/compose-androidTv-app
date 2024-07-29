package com.iwedia.cltv.scene.zap_banner_scene

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.scene.ReferenceSceneListener

/**
 * Zap banner scene listener
 *
 * @author Dejan Nadj
 */
interface ZapBannerSceneListener : ReferenceSceneListener, TTSSetterInterface {

    /**
     * Channel up
     */
    fun channelUp();

    /**
     * Channel down
     */
    fun channelDown();

    /**
     * Last active channel
     */
    fun lastActiveChannel()

    /**
     * show info Banner
     */
    fun showInfoBanner()

    /**
     * show channel List
     */
    fun showChannelList()

    fun onBackClicked()

    fun isClosedCaptionEnabled(): Boolean?

    fun getClosedCaption(): String?

    fun isCCTrackAvailable(): Boolean?
    fun getChannelSourceType(tvChannel: TvChannel): String

    /**
     * show home scene
     */
    fun onOKPressed()

    /**
     * Show home scene
     */
    fun showHomeScene()

    /**
     * show player
     */
    fun showPlayer()

    /**
     * zap to channel after long press channel +
     */
    fun channelUpAfterLongPress()

    /**
     * show zap banner on channel +
     */
    fun channelUpZapBanner()

    /**
     * zap to channel after long press channel -
     */
    fun channelDownAfterLongPress()

    /**
     * show zap banner on channel -
     */
    fun channelDownZapBanner()

    /**
     * check is cc available
     */
    fun getIsCC(type: Int): Boolean

    /**
     * check is audio description available
     */
    fun getIsAudioDescription(type: Int): Boolean

    /**
     * check is teletext available
     */
    fun getTeleText(type: Int): Boolean

    /**
     * check is dolby available
     */
    fun getIsDolby(type: Int): Boolean

    fun isHOH(type:Int): Boolean

    /**
     * get audio channel info
     */
    fun getAudioChannelInfo(type: Int): String

    /**
     * check is channel unlocked
     */
    fun isChannelUnlocked(): Boolean

    /**
     * get video resolution string
     */
    fun getVideoResolution(): String

    /**
     * get parental rating display name
     */
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String

    /**
     * is parental rating enabled
     */

    fun isParentalEnabled(): Boolean

    /**
     * Get current time epoch time in ms
     */
    fun getCurrentTime(tvChannel: TvChannel): Long

    /**
     * Show virtual keyboard
     */
    fun showVirtualKeyboard()
    fun getCurrentAudioTrack(): IAudioTrack?
    fun getAvailableAudioTracks(): List<IAudioTrack>
    fun getDateTimeFormat(): DateTimeFormat
    fun isEventLocked(tvEvent: TvEvent?): Boolean
    fun getAvailableSubtitleTracks(): List<ISubtitle>
    fun getCurrentSubtitleTrack(): ISubtitle?
    fun isSubtitleEnabled(): Boolean
    fun getNextEvent(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>)

    fun getAudioFormatInfo(): String
    fun isScrambled():Boolean
}