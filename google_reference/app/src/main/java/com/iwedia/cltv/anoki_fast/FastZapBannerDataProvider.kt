package com.iwedia.cltv.anoki_fast

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle

object FastZapBannerDataProvider: TTSSetterInterface {

    val FAST_SHOW_GUIDE_INTENT = "FAST_SHOW_GUIDE"
    val FAST_SHOW_ZAP_BANNER_INTENT = "FAST_SHOW_ZAP_BANNER"
    val FAST_HIDE_ZAP_BANNER_INTENT = "FAST_HIDE_ZAP_BANNER"

    var tvModule: TvInterface?= null
    var epgModule: EpgInterface?= null
    var playerModule: PlayerInterface?= null
    private lateinit var textToSpeechModule: TTSInterface
    var utilsModule: UtilsInterface?= null
    var categoryModule: CategoryInterface?= null
    var parentalControlSettingsModule: ParentalControlSettingsInterface?= null

    fun init(
        tvInterface: TvInterface,
        epgInterface: EpgInterface,
        playerInterface: PlayerInterface,
        utilsInterface: UtilsInterface,
        categoryInterface: CategoryInterface,
        parentalControlSettingsModule: ParentalControlSettingsInterface,
        textToSpeechModule: TTSInterface
    ) {
        this.tvModule = tvInterface
        this.epgModule = epgInterface
        this.playerModule = playerInterface
        this.utilsModule = utilsInterface
        this.categoryModule = categoryInterface
        this.parentalControlSettingsModule = parentalControlSettingsModule
        this.textToSpeechModule = textToSpeechModule
    }

    fun getCurrentChannel(callback : (tvChanel: TvChannel?)-> Unit) {
        tvModule?.getActiveChannel(callback = object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                error.printStackTrace()
                callback(null)
            }

            override fun onReceive(tvChannel: TvChannel) {
                callback(tvChannel)
            }
        }, ApplicationMode.FAST_ONLY)
    }

    fun getCurrentEvent(tvChannel: TvChannel, callback : (tvEvent: TvEvent?)-> Unit) {
        epgModule?.getCurrentEvent(tvChannel = tvChannel, object : IAsyncDataCallback<TvEvent> {
            override fun onFailed(error: Error) {
                error.printStackTrace()
                callback(null)
            }

            override fun onReceive(tvEvent: TvEvent) {
                callback(tvEvent)
            }
        })
    }

    fun channelUp(callback: (success: Boolean)->Unit) {
        filterChannelListByActiveGenre { channelList->
            tvModule?.playNextIndex(channelList, object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    error.printStackTrace()
                    callback(false)
                }

                override fun onSuccess() {
                    callback(true)
                }
            }, ApplicationMode.FAST_ONLY)
        }
    }

    fun channelDown(callback: (success: Boolean)->Unit) {
        filterChannelListByActiveGenre { channelList->
            tvModule?.playPrevIndex(channelList, object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    error.printStackTrace()
                    callback(false)
                }

                override fun onSuccess() {
                    callback(true)
                }
            }, ApplicationMode.FAST_ONLY)
        }
    }

    private fun filterChannelListByActiveGenre(callback: (list: ArrayList<TvChannel>)->Unit) {
        FastLiveTabDataProvider.getActiveGenre { genre->
            if (genre == "Favorites") {
                callback.invoke(FastLiveTabDataProvider.getFavoriteChannels())
            } else {
                categoryModule?.getAvailableFilters(object :IAsyncDataCallback<ArrayList<Category>>{
                    override fun onFailed(error: Error) {}

                    override fun onReceive(data: ArrayList<Category>) {

                        data.forEach { category ->
                            if (category.name == genre) {
                                categoryModule?.filterChannels(category,   object : IAsyncDataCallback<ArrayList<TvChannel>> {
                                    override fun onFailed(error: Error) {}
                                    @RequiresApi(Build.VERSION_CODES.R)
                                    override fun onReceive(data: ArrayList<TvChannel>) {

                                        var list : ArrayList<TvChannel> = arrayListOf()
                                        list.addAll(data)
                                        callback.invoke(list)
                                    }
                                }, ApplicationMode.FAST_ONLY)
                            }
                        }
                    }
                }, ApplicationMode.FAST_ONLY)
            }

        }
    }

    fun guideClick(context: Context) {
        val intent = Intent(FAST_SHOW_GUIDE_INTENT)
        context.sendBroadcast(intent)
    }

    fun getAvailableAudioTracks(): MutableList<IAudioTrack>? {
        return playerModule?.getAudioTracks() as MutableList<IAudioTrack>?
    }

    fun getCurrentAudioTrack() : IAudioTrack? {
        return playerModule?.getActiveAudioTrack()
    }

    fun audioTrackSelected(audioTrack: IAudioTrack) {
        playerModule?.selectAudioTrack(audioTrack)
    }

    fun getCurrentSubtitleTrack() : ISubtitle? {
        return playerModule?.getActiveSubtitle()
    }

    fun getAvailableSubtitleTracks(): MutableList<ISubtitle>? {
        return playerModule?.getSubtitleTracks(ApplicationMode.FAST_ONLY) as MutableList<ISubtitle>?
    }

    fun isSubtitlesEnabled(): Boolean {
        return utilsModule?.getSubtitlesState(PrefType.BASE)!!
    }

    fun enableSubtitles(enable: Boolean) {
        if (!enable) {
            playerModule?.selectSubtitle(null)
        } else {
            playerModule?.selectSubtitle(getCurrentSubtitleTrack())
        }
        utilsModule?.enableSubtitles(enable, PrefType.BASE)!!
    }

    fun subtitleTrackSelected(subtitleTrack: ISubtitle) {
        utilsModule?.enableSubtitles(true, PrefType.BASE)
        playerModule?.selectSubtitle(subtitleTrack)
    }

    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        return tvModule!!.getParentalRatingDisplayName(parentalRating, ApplicationMode.FAST_ONLY, tvEvent)
    }
    fun getVideoResolution(): String {
        return playerModule!!.getVideoResolution()
    }

    fun isDolby(trackType: Int): Boolean {
        return playerModule!!.getIsDolby(trackType)
    }

    fun getDateTimeFromat(): DateTimeFormat {
        return utilsModule!!.getDateTimeFormat()
    }

    fun isEventLocked(tvEvent: TvEvent?) = parentalControlSettingsModule!!.isEventLocked(tvEvent)
    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }
}