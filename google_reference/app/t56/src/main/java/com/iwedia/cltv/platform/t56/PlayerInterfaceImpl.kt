package com.iwedia.cltv.platform.t56

import android.content.Context
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvTrackInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.player.PlayerBaseImpl
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.t56.provider.PlatformSpecificData
import com.mediatek.twoworlds.tv.MtkTvAppTVBase
import com.mediatek.twoworlds.tv.MtkTvBanner
import com.mediatek.twoworlds.tv.MtkTvChannelList
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.twoworlds.tv.common.MtkTvTISMsgBase
import com.mediatek.twoworlds.tv.model.MtkTvDvbChannelInfo
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase


internal class PlayerInterfaceImpl(var context: Context, utilsInterface: UtilsInterface, val epgInterface: EpgInterface, parentalControlSettingsInterface: ParentalControlSettingsInterface) : PlayerBaseImpl(
    utilsInterface, parentalControlSettingsInterface
) {
    override var playerState: PlayerState = PlayerState.IDLE
    override var isTimeShiftAvailable = false
    private var channelForBackgroundTuning: TvChannel? = null
    private var isBroadcastChannelTuned = false

    init {
        trackManager = TrackManagerImpl(utilsInterface)

        super.mListeners.add(object : PlayerInterface.PlayerListener {
            override fun onNoPlayback() {
            }

            override fun onPlaybackStarted() {
            }

            override fun onAudioTrackUpdated(audioTracks: List<IAudioTrack>) {
            }

            override fun onSubtitleTrackUpdated(subtitleTracks: List<ISubtitle>) {
            }

            override fun onVideoAvailable(inputId: String) {
                InformationBus.informationBusEventListener.submitEvent(Events.VIDEO_RESOLUTION_UNAVAILABLE, arrayListOf(0))
                if(inputId.contains("Tuner")) {
                    wasScramble = false
                    if (isParentalActive)
                        playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
                    else
                        playbackStatus.value = PlaybackStatus.PLAYBACK_INIT

                    epgInterface.updateEpgData()
                    //Hide black overlay after 1sec
                    CoroutineHelper.runCoroutineWithDelay({
                        if (!isMuted) {
                            unmute()
                        }
                        InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_HIDE_BLACK_OVERLAY)
                    }, 1000)
                }
            }

            override fun onVideoUnAvailable(reason: Int, inputId: String) {
                if(inputId.contains("Tuner")) {
                    if (reason == 18) {
                        wasScramble = true
                        playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
                    } else if (reason == 1) {
                        wasScramble = false
                        playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                    } else if (reason == 2) {
                        playbackStatus.value = PlaybackStatus.NO_PLAYBACK
                    } else {
                        playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                    }
                } else {
                    InformationBus.informationBusEventListener.submitEvent(Events.VIDEO_RESOLUTION_UNAVAILABLE, arrayListOf(1))
                }
            }

            override fun onContentAvailable() {
                playbackStatus.value = PlaybackStatus.PARENTAL_PIN_HIDE
                isParentalActive = false
                if (!wasScramble) {
                    playbackStatus.value = PlaybackStatus.PLAYBACK_STARTED
                } else {
                    playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
                }
            }

            override fun onContentBlocked(rating: TvContentRating) {
                println("getLockedChannelList " + "onContentBlocked $rating")
                blockedRating = rating
                isParentalActive = true
                playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
            }

            override fun onTimeShiftStatusChanged(inputId: String, status: Boolean) {
            }

            override fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {
            }

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
            }
        })
    }

    override fun requestUnblockContent(callback: IAsyncCallback) {
        if((activePlayableItem as TvChannel).isFastChannel()){
            super.requestUnblockContent(callback)
        }else{
            CoroutineHelper.runCoroutine({
                MtkTvAppTVBase().unblockSvc("main", true);
                callback.onSuccess()
            })
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsDolby(type: Int): Boolean {
        var tracks = getPlaybackTracks(type)
        if (tracks.isNotEmpty())
            for (track in tracks) {
                try {
                    var trackAudioEncodeType: String = ""
                    when (track.extra.get("key_AudioEncodeType") as String) {
                        "1" -> trackAudioEncodeType = CODEC_AUDIO_AC3
                        "12" -> trackAudioEncodeType = CODEC_AUDIO_EAC3
                        "26" -> trackAudioEncodeType = CODEC_AUDIO_DTS
                    }
                    if (trackAudioEncodeType == CODEC_AUDIO_AC3 || trackAudioEncodeType == CODEC_AUDIO_EAC3 || trackAudioEncodeType == CODEC_AUDIO_EAC3_ATSC || trackAudioEncodeType == CODEC_AUDIO_AC3_ATSC ||trackAudioEncodeType == CODEC_AUDIO_DTS) {
                        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "TRASCK IS DOLBY")
                        return true
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getDolbyType(type: Int, trackId: String): String {
        var tracks = getPlaybackTracks(type)
        if (tracks.isNotEmpty())
            for (track in tracks) {
                try {
                    if(track.id == trackId) {
                        try {
                            var trackAudioEncodeType: String = ""
                            when (track.extra.get("key_AudioEncodeType") as String) {
                                "1" -> trackAudioEncodeType = CODEC_AUDIO_AC3
                                "12" -> trackAudioEncodeType = CODEC_AUDIO_EAC3
                                "26" -> trackAudioEncodeType = CODEC_AUDIO_DTS
                            }
                            return trackAudioEncodeType
                        } catch (e: Exception) {
                            continue
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        return ""
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        return hasAudioDescriptionCheck(tvTrackInfo)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsAudioDescription(type: Int): Boolean {
        var tracks = getPlaybackTracks(type)
        if (tracks.isNotEmpty())
            for (track in tracks) {
                try {
                    if (hasAudioDescriptionCheck(track)) {
                        return true
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        return false
    }

    fun hasAudioDescriptionCheck(tvTrackInfo: TvTrackInfo): Boolean{
        var audioDescriptionEnable : Boolean = false

        var type: String =  ""
        var mixtype: String = ""
        var eClass: String = ""
        if (tvTrackInfo.extra != null) {
            if(tvTrackInfo.extra.get("key_AudioType") != null){
                type = tvTrackInfo.extra.get("key_AudioType") as String
            }
            if(tvTrackInfo.extra.get("key_AudioMixType") != null){
                mixtype = tvTrackInfo.extra.get("key_AudioMixType") as String
            }
            if(tvTrackInfo.extra.get("key_AudioMixType") != null){
                eClass = tvTrackInfo.extra.get("key_AudioEditorialClass") as String
            }

            Log.i(TAG, "hasAudioDescription extra values -> mixtype:$mixtype eClass:$eClass type:$type ")

            if (!eClass.isNullOrEmpty() && !mixtype.isNullOrEmpty()) {
                if (TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_HEARING_IMPAIRED_CLEAN.toString() == eClass) {
                    if (!eClass.isNullOrEmpty() && TvProviderAudioTrackBase.AUD_TYPE_HEARING_IMPAIRED == Integer.parseInt(type)) {
                        audioDescriptionEnable = true
                    }
                }
                else if (TvProviderAudioTrackBase.AUD_MIX_TYPE_INDEPENDENT.toString() == mixtype) {
                    if (TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_AD.toString() == eClass || TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE.toString() == eClass || TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_RESERVED.toString() == eClass && TvProviderAudioTrackBase.AUD_TYPE_VISUAL_IMPAIRED.toString() == type) {
                        audioDescriptionEnable = true;
                    }
                }
                else if (TvProviderAudioTrackBase.AUD_MIX_TYPE_SUPPLEMENTARY.toString() == mixtype) {
                    if (TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_AD.toString() == eClass || TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_RESERVED.toString() == eClass && TvProviderAudioTrackBase.AUD_TYPE_VISUAL_IMPAIRED.toString() == type) {
                        audioDescriptionEnable = true
                    }
                } else if (TvProviderAudioTrackBase.AUD_MIX_TYPE_INDEPENDENT.toString() != mixtype && TvProviderAudioTrackBase.AUD_TYPE_VISUAL_IMPAIRED.toString() == type) {
                    audioDescriptionEnable = true
                }
            }
        }

        Log.i(TAG, "hasAudioDescription : $audioDescriptionEnable ")
        return audioDescriptionEnable
    }

    override fun getAudioChannelIndex(type: Int) : Int {
        var audioLangIdx = -1
        var tracks = getPlaybackTracks(type)
        var currentTrack = getActiveAudioTrack()

        tracks.forEachIndexed { i, track ->
            if (track.id == currentTrack?.trackId) {
                if(currentTrack!!.isAnalogTrack) {
                    audioLangIdx = when(currentTrack.analogName) {
                        "Unknown" -> 0
                        "Mono" -> 1
                        "Stereo" -> 4
                        "SAP" -> 3
                        else -> 4
                    }
                } else {
                    audioLangIdx = tracks[i].audioChannelCount
                }
            }
        }

        return audioLangIdx
    }

    override fun getVideoResolution(): String {
        try {
            if ((activePlayableItem is TvChannel)) {
                if (!(activePlayableItem as TvChannel).isFastChannel()) {
                    return if ((MtkTvBanner.getInstance() != null) && (MtkTvBanner.getInstance().iptsRslt != null)
                    ) {
                        MtkTvBanner.getInstance().iptsRslt
                    } else {
                        super.getVideoResolution()
                    }
                }
                return super.getVideoResolution()
            }
        }catch (E: Exception){
            println(E)
        }
        return super.getVideoResolution()
    }

    override fun play(playableItem: Any) {
        if(playableItem is TvChannel) {
            var serviceListId = (playableItem.platformSpecific as PlatformSpecificData).internalServiceListID
            when(serviceListId) {
                1 -> {
                    MtkTvConfig.getInstance().setConfigValue(
                        MtkTvConfigType.CFG_BS_BS_SRC,
                        MtkTvConfigType.APP_CFG_BS_SRC_AIR,
                        MtkTvConfigType.CFGF_SET_VALUE
                    )
                }
                2 -> {
                    MtkTvConfig.getInstance().setConfigValue(
                        MtkTvConfigType.CFG_BS_BS_SRC,
                        MtkTvConfigType.APP_CFG_BS_SRC_CABLE,
                        MtkTvConfigType.CFGF_SET_VALUE
                    )
                }
            }
        }

        if(!isBroadcastChannelTuned) super.play(playableItem)
    }

    override fun performBackgroundTuning(tvChannel: TvChannel) {
        if(channelForBackgroundTuning != null && TvChannel.compare(channelForBackgroundTuning!!, tvChannel)){
            return
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "performBackgroundTuning: tvChannel name === ${tvChannel.name}")
        val bundle = Bundle()
        if(isBarkChannel(tvChannel)){
            bundle.putByte(MtkTvTISMsgBase.MSG_CHANNEL_IS_BARKER_CHANNEL, MtkTvTISMsgBase.MTK_TIS_VALUE_TRUE)
        } else {
            bundle.putByte(MtkTvTISMsgBase.MSG_CHANNEL_IS_BARKER_CHANNEL, MtkTvTISMsgBase.MTK_TIS_VALUE_FALSE)
        }
        val channelUri = TvContract.buildChannelUri(tvChannel.id.toLong())
        trackManager.clear()
        playbackView.sendAppPrivateCommand(MtkTvTISMsgBase.MTK_TIS_MSG_CHANNEL, bundle)
        playbackView.tune(tvChannel.inputId, channelUri)
        if(tvChannel.isFastChannel()) {
            resume()
            isBroadcastChannelTuned = false
        } else {
            stop()
            isBroadcastChannelTuned = true
        }
        channelForBackgroundTuning = tvChannel
    }

    private fun isBarkChannel(tvChannel: TvChannel): Boolean {
        if(tvChannel.isFastChannel() || utilsInterface.getRegion() != Region.EU) return false
        val mtkService = MtkTvChannelList.getInstance().getChannelInfoBySvlRecId(
            tvChannel.providerFlag1!!, tvChannel.providerFlag2!!)
        if(mtkService != null){
            val dvbChannelInfo : MtkTvDvbChannelInfo = mtkService as MtkTvDvbChannelInfo
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "isBarkChannel: providerFlag1 === ${tvChannel.providerFlag1!!}, providerFlag2 === ${tvChannel.providerFlag2!!}, barkerMask === ${dvbChannelInfo.barkerMask}")
            return dvbChannelInfo.barkerMask == 1
        }
        return false
    }

    companion object {
        private const val CODEC_AUDIO_AC3 = "ac3"
        private const val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
        private const val CODEC_AUDIO_EAC3 = "eac3"
        private const val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
        private const val CODEC_AUDIO_DTS = "dts"
    }

}