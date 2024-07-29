package com.iwedia.cltv.scene.search


import com.iwedia.cltv.platform.`interface`.TTSStopperInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType
import world.SceneListener

/**
 * Search scene listener
 *
 * @author Aleksandar Lazic
 */
interface SearchSceneListener : SceneListener, TTSStopperInterface, TTSSetterInterface,
    ToastInterface {
    fun onSearchQuery(query: String)
    fun onTvEventClicked(event: Any)
    fun isChannelLocked(channelId: Int): Boolean
    fun isParentalControlsEnabled(): Boolean
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    fun getCurrent(tvChannel: TvChannel):Long
    fun isInWatchlist(tvEvent: TvEvent): Boolean
    fun isInRecList(tvEvent: TvEvent): Boolean
    fun getDateTimeFormat(): DateTimeFormat
    fun isEventLocked(tvEvent: TvEvent?): Boolean

    fun onVodItemClicked(type: VODType, contentId: String)
}