package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.media.AudioManager
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvTrackInfo
import android.media.tv.TvView
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.base.player.PlayerBaseImpl
import com.iwedia.cltv.platform.base.player.TrackBase
import com.iwedia.cltv.platform.base.player.onVideoAvailable
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.refplus5.SaveValue.Companion.saveTISSettingsIntValue
import com.iwedia.cltv.platform.refplus5.audio.TvProviderAudioTrackBase
import com.mediatek.dtv.tvinput.client.videosignalinfo.VideoSignalInfo
import com.mediatek.dtv.tvinput.framework.tifextapi.common.audio.Constants
import kotlinx.coroutines.Dispatchers
import java.lang.reflect.InvocationTargetException


internal class PlayerInterfaceImpl(var context: Context, utilsInterface: UtilsInterface, parentalControlSettingsInterface: ParentalControlSettingsInterface) : PlayerBaseImpl(utilsInterface,parentalControlSettingsInterface) {

    override var playerState: PlayerState = PlayerState.IDLE
    override var isTimeShiftAvailable = true

    var channelLockRating : TvContentRating? = null

    private var mQuietTuneEnabled = false

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
                wasScramble = false
                if (isParentalActive)
                    playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
                else
                    playbackStatus.value = PlaybackStatus.PLAYBACK_INIT

                //Hide black overlay after 1sec
                CoroutineHelper.runCoroutineWithDelay({
                    if (!isMuted) {
                        unmute()
                    }
                    InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_HIDE_BLACK_OVERLAY)
                }, 1000)
            }

            override fun onVideoUnAvailable(reason: Int, inputId: String) {
                if(reason == TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY){
                    playbackStatus.value = PlaybackStatus.AUDIO_ONLY
                }
                else if (reason == 256 || reason == 18 || reason == 15) {
                    wasScramble = true
                    playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
                } else if (reason == 1) {
                    playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                    noPlayback = false
                } else if (reason == 2) {
                    playbackStatus.value = PlaybackStatus.NO_PLAYBACK
                    noPlayback = true
                } else {
                    playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                    noPlayback = false
                }
            }

            override fun onContentAvailable() {
                playbackStatus.value = PlaybackStatus.PARENTAL_PIN_HIDE
                isParentalActive = false
                if (!wasScramble && !noPlayback) {
                    playbackStatus.value = PlaybackStatus.PLAYBACK_STARTED
                }
                else if (noPlayback) {
                    playbackStatus.value = PlaybackStatus.NO_PLAYBACK
                }
                else {
                    playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
                }
            }

            override fun onContentBlocked(rating: TvContentRating) {
                blockedRating = rating
                playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
                if(rating.ratingSystem == "channel_lock") {
                    channelLockRating = rating
                    isParentalActive = false
                } else {
                    isParentalActive = true
                }
            }

            override fun onTimeShiftStatusChanged(inputId: String, status: Boolean) {
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "onTimeShiftStatusChanged, inputId $inputId $status");
            }

            override fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {
                (utilsInterface as UtilsInterfaceImpl).onEvent(inputId, eventType, eventArgs)
                utilsInterface.setTvInputId(inputId)
            }

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
            }
        })


    }



    override fun selectSubtitle(subtitle: ISubtitle?) {
        if (subtitle == null) {
            setCaptionEnabled(false)
            playbackView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
            if(isAnalogService()){
                saveTISSettingsIntValue(context, "subtitle_display", "com.mediatek.tis.settings.analog", "common", 0)
            }
        } else {
            setCaptionEnabled(true)
            playbackView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, subtitle.trackId)
            if(isAnalogService()){
                saveTISSettingsIntValue(context, "subtitle_display", "com.mediatek.tis.settings.analog", "common", 1)
            }
        }
    }

    private fun isAnalogService(): Boolean{
        if(activePlayableItem is TvChannel){
            if(((activePlayableItem as TvChannel).type == TvContract.Channels.TYPE_NTSC) ||
                ((activePlayableItem as TvChannel).type == TvContract.Channels.TYPE_PAL) ||
                ((activePlayableItem as TvChannel).type == TvContract.Channels.TYPE_SECAM)) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsDolby(type: Int): Boolean {
        var tracks = getPlaybackTracks(type)
        var trackId = getPlaybackSelectedTrack(type)
        if (tracks == null || tracks.isEmpty()) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "PlayerInterfaceImpl", "isDolbyAudioType tracks==null or tracks is empty!")
            return false
        }
        for (i in tracks.indices) {
            if (tracks[i].id == trackId) {
                var trackAudioEncodeType: String = ""
                when (tracks[i].encoding.toString()) {
                    "audio/ac3" -> trackAudioEncodeType = CODEC_AUDIO_AC3
                    "audio/eac3" -> trackAudioEncodeType = CODEC_AUDIO_EAC3
                    "audio/dts" -> trackAudioEncodeType = CODEC_AUDIO_DTS
                }
                if (trackAudioEncodeType == CODEC_AUDIO_AC3 || trackAudioEncodeType == CODEC_AUDIO_EAC3 || trackAudioEncodeType == CODEC_AUDIO_EAC3_ATSC || trackAudioEncodeType == CODEC_AUDIO_AC3_ATSC ||trackAudioEncodeType == CODEC_AUDIO_DTS) {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "PlayerInterfaceImpl/getIsDolby", "TRACK IS DOLBY")
                    return true
                }
                else{
                    return false
                }
            }
        }
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "PlayerInterfaceImpl", "isDolbyAudioType false")
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getTeleText(type: Int): Boolean {
        return (utilsInterface as UtilsInterfaceImpl).isTeletextAvailable()
    }

    override fun mute() {
        isMuted = true
        playbackView.setStreamVolume(0.0f)
    }

    override fun unmute() {
        isMuted = false
        //if you add here setStreamVolume(0f) sound will not work on Analog services
        //coz MTK SDK does not like to be abused
        playbackView.setStreamVolume(1.0f)
    }

    private fun getVideoFormatType(videoHeight: Int): String {
        return when (videoHeight) {
            2160 -> VID_UHD // VIDEO_HEIGHT_2160
            1080 -> VID_FHD // VIDEO_HEIGHT_1080
            720 -> VID_HD // VIDEO_HEIGHT_720
            576,480 -> VID_SD // VIDEO_HEIGHT_480
            else -> {
                if (videoHeight >= 720) VID_HD else VID_SD
            }
        }
    }
    override fun getVideoResolution(): String {
        try {
            if ((activePlayableItem is TvChannel)) {
                if (!(activePlayableItem as TvChannel).isFastChannel()) {

                    if ((activePlayableItem as TvChannel).isRadioChannel){
                        return ""
                    }
                    val key = "KEY_VIDEO_HEIGHT"
                    var inputId =  if (utilsInterface.getRegion()== Region.US) {
                        "com.mediatek.dtv.tvinput.atsctuner/.AtscTunerInputService/HW0"
                    } else {
                        "com.mediatek.dtv.tvinput.dvbtuner/.DvbTvInputService/HW0"
                    }

                    val mVideo = VideoSignalInfo(context, inputId)
                    val videoHeight = mVideo.getVideoSignalInfo("")!!.getInt(key)

                    return getVideoFormatType(videoHeight)

                } else {
                    return super.getVideoResolution()
                }
            }
        }catch (E: Exception){
            println(E)
        }
        return super.getVideoResolution()
    }

    override fun play(playableItem: Any) {

        if (playableItem is TvChannel) {
            AudioInterfaceImpl.mAudioTrackId = -1
            AudioInterfaceImpl.mAudioFormatId = -1
            AudioInterfaceImpl.addCallback(context.applicationContext, playableItem.inputId)
        }
        if (liveTabChannel == null) {
            mQuietTuneEnabled = false
            (trackManager as TrackManagerImpl).setPlayableItem(playableItem)
            continuePlay(playableItem)
        } else {
            continuePlay(liveTabChannel as TvChannel)
            liveTabChannel = null
        }
    }

    private fun setTunerMode(channel : TvChannel) {
        var modeSet = false
        var tunerMode = BS_SRC_AIR
        when(channel.tunerType) {
            TunerType.TERRESTRIAL_TUNER_TYPE -> {
                tunerMode = BS_SRC_AIR
                modeSet = true
            }
            TunerType.CABLE_TUNER_TYPE -> {
                tunerMode = BS_SRC_CABLE
                modeSet = true
            }
            TunerType.SATELLITE_TUNER_TYPE -> {
                tunerMode = BS_SRC_SAT
                modeSet = true
            }
            else -> {}
        }

        if(modeSet) {
            val currentTunerMode =  (utilsInterface as UtilsInterfaceImpl).readMtkInternalGlobalIntValue(context, com.iwedia.cltv.platform.refplus5.screenMode.Constants.CFG_BS_BS_SRC,-1)
                if(currentTunerMode != tunerMode) {

                utilsInterface.saveMtkInternalGlobalValue(
                    context,
                    com.iwedia.cltv.platform.refplus5.screenMode.Constants.CFG_BS_BS_SRC,
                    tunerMode.toString(),
                    true
                );
                utilsInterface.saveMtkInternalGlobalValue(
                    context,
                    com.iwedia.cltv.platform.refplus5.screenMode.Constants.CFG_BS_BS_USER_SRC,
                    tunerMode.toString(),
                    true
                );
            }
        }
    }

    private fun continuePlay(playableItem: Any) {
        activePlayableItem = playableItem
        when (playableItem) {
            is TvChannel -> {
                setTunerMode(playableItem)
                if (playbackStatus.value == PlaybackStatus.PARENTAL_PIN_SHOW) {
                    playbackStatus.value = PlaybackStatus.PARENTAL_PIN_HIDE
                }
                isParentalActive = false

                lastPlayedService = playableItem
                val channelUri = TvContract.buildChannelUri(playableItem.id.toLong())
                trackManager.clear()
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + PlayerBaseImpl.TAG, "continuePlay playableItem: ${playableItem.name} channenlUri: $channelUri")
                playbackView.tune(playableItem.inputId, channelUri)
                if (playableItem.isLocked || isMuted){
                    mute()
                }
                InformationBus.informationBusEventListener.submitEvent(
                    Events.CHANNEL_CHANGED,
                    arrayListOf(playableItem as TvChannel)
                )
                isChannelUnlocked = false
                (utilsInterface as UtilsInterfaceImpl).setChannelUnlocked(isChannelUnlocked)
                setActiveChannelLockedStatus()
            }

            is Recording -> {
                val recordingUri = TvContract.buildRecordedProgramUri(playableItem.id.toLong())
                playbackView.reset()
                mListeners.onVideoAvailable(playableItem.tvChannel!!.inputId)
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "PVR TEST","Recording ref 5")
                playbackView.timeShiftPlay(playableItem.tvChannel!!.inputId, recordingUri)

                InformationBus.informationBusEventListener.registerEventListener(
                    arrayListOf(Events.PLAYBACK_HIDE_BLACK_OVERLAY),
                    {
                        listener = it
                    },
                    {
                        playbackView.timeShiftResume()
                        listener?.let {
                            InformationBus.informationBusEventListener.unregisterEventListener(
                                it
                            )
                        }
                        listener = null
                    })

            }
        }
    }

    override fun getSubtitleTracks(applicationMode: ApplicationMode): List<ISubtitle> {
        if (applicationMode == ApplicationMode.FAST_ONLY)
            return super.getSubtitleTracks(applicationMode)
        else {
            if ((utilsInterface as UtilsInterfaceImpl).isSubtitleRegion()) {
                return trackManager.getSubtitleTracks()
            }
            return ArrayList()
        }
    }



    fun getCCSubtitleTracks() : List<ISubtitle> {
        return trackManager.getSubtitleTracks()
    }

    fun play(playableItem: Any, bundle: Bundle) {
        activePlayableItem = playableItem
        when (playableItem) {
            is TvChannel -> {
                if(playbackStatus.value == PlaybackStatus.PARENTAL_PIN_SHOW) {
                    playbackStatus.value = PlaybackStatus.PARENTAL_PIN_HIDE
                }
                isParentalActive = false

                val channelUri = TvContract.buildChannelUri(playableItem.id.toLong())
                trackManager.clear()
                //(playbackView as TvSurfaceView).buildBundleData(bundle, input)
                println("PLAY input ${playableItem.inputId} $channelUri $bundle")
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + PlayerBaseImpl.TAG, "play playableItem: ${playableItem.name} channenlUri: $channelUri")
                playbackView.tune(playableItem.inputId, channelUri, bundle)
                if (isMuted) mute()
                InformationBus.informationBusEventListener.submitEvent(Events.CHANNEL_CHANGED, arrayListOf(playableItem as TvChannel))
                isChannelUnlocked = false
                (utilsInterface as UtilsInterfaceImpl).setChannelUnlocked(isChannelUnlocked)
                setActiveChannelLockedStatus()
            }
        }
    }

    override fun switchAudioTrack(): String {
        val audioTracks = getAudioTracks()
        if (audioTracks.isEmpty()) return ""
        val activeAudioTrack =getActiveAudioTrack()
        val activeIndex = audioTracks.indexOf(activeAudioTrack)
        val nextIndex = (activeIndex + 1) % audioTracks.size
        val nextAudioTrack = audioTracks[nextIndex]
        playbackView.selectTrack(TvTrackInfo.TYPE_AUDIO, nextAudioTrack.trackId)
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
            TAG,
            "selectAudioTrack: MainActivity: select audio ${
                playbackView.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
            }"
        )
        return nextAudioTrack.trackName
    }

    override fun unlockChannel(): Boolean {
        if(channelLockRating == null) {
            return false
        }

        (utilsInterface as UtilsInterfaceImpl).setChannelUnlocked(true)

        CoroutineHelper.runCoroutineForSuspend({
            if (mServiceRef == null) {
                mServiceRef = TvView::class.members.single {
                    it.name == "requestUnblockContent"
                }
            }

            try {
                    mServiceRef!!.call(playbackView, channelLockRating)
                    channelLockRating = null
            } catch (e: Exception) {
                e.printStackTrace()
                return@runCoroutineForSuspend
            } catch (invocationTargetException: InvocationTargetException) {
                invocationTargetException.cause?.printStackTrace()
                return@runCoroutineForSuspend
            }
        },Dispatchers.IO)

        return true
    }



    override fun setQuietTuneEnabled(enabled: Boolean) {
        mQuietTuneEnabled = enabled
    }

    override fun getQuietTuneEnabled(): Boolean {
        return mQuietTuneEnabled
    }

    override fun getAudioChannelIndex(type: Int): Int {
        return AudioInterfaceImpl.mAudioTrackId
    }

    override fun getAudioFormat(): String {
        val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val dtsVersion = mAudioManager.getParameters("AQ_DtsVersion")
        Log.e("DHANYA","AudioInterfaceImpl.mAudioFormatId ${AudioInterfaceImpl.mAudioFormatId}")
        return when (AudioInterfaceImpl.mAudioFormatId) {
            Constants.AudioCodec.AUDIO_CODEC_MPEG_L2 -> "MPEG1-L2"
            Constants.AudioCodec.AUDIO_CODEC_RAW -> "PCM"
            Constants.AudioCodec.AUDIO_CODEC_AAC -> "AAC"
            Constants.AudioCodec.AUDIO_CODEC_HE_AAC -> "HE_AAC"
            Constants.AudioCodec.AUDIO_CODEC_HE_AAC_V2 -> "HE_AACv2"
            Constants.AudioCodec.AUDIO_CODEC_MPEG4_AAC_LC,
            Constants.AudioCodec.AUDIO_CODEC_MPEG2_AAC_LC -> "AAC_LC"

            Constants.AudioCodec.AUDIO_CODEC_DTS -> "DTS"
            Constants.AudioCodec.AUDIO_CODEC_DTS_HD ->
                if (dtsVersion.equals("AQ_DtsVersion=DTS:X")) "DTS-HD"
                else "DTS_HD Master Audio" // DTS-HD  |  DTS_HD Master Audio
            Constants.AudioCodec.AUDIO_CODEC_DTS_HD_LBR ->
                if (dtsVersion.equals("AQ_DtsVersion=DTS:X")) "DTS-HD"
                else "DTS_EXPRESS" // DTS-HD  |  DTS Express
            Constants.AudioCodec.AUDIO_CODEC_DTS_HD_HR,
            Constants.AudioCodec.AUDIO_CODEC_DTS_HD_MA -> "DTS-HD"

            Constants.AudioCodec.AUDIO_CODEC_DTS_X_P1,
            Constants.AudioCodec.AUDIO_CODEC_DTS_X_P2 -> "DTS:X"

            Constants.AudioCodec.AUDIO_CODEC_DTS_UHD_P2 -> "DTS:X"
            Constants.AudioCodec.AUDIO_CODEC_MPEGH_MHA1,
            Constants.AudioCodec.AUDIO_CODEC_MPEGH_MHM1,
            Constants.AudioCodec.AUDIO_CODEC_MPEGH_BL_L3,
            Constants.AudioCodec.AUDIO_CODEC_MPEGH_BL_L4,
            Constants.AudioCodec.AUDIO_CODEC_MPEGH_LC_L3,
            Constants.AudioCodec.AUDIO_CODEC_MPEGH_LC_L4 -> "MPEG-H"

            Constants.AudioCodec.AUDIO_CODEC_DRA -> "DRA"
            else -> ""
        }
    }

    companion object {
        const val TAG = "PlayerInterfaceImpl"
        private const val CODEC_AUDIO_AC3 = "ac3"
        private const val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
        private const val CODEC_AUDIO_EAC3 = "eac3"
        private const val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
        private const val CODEC_AUDIO_DTS = "dts"
        const val VID_UHD = "UHD"
        const val VID_FHD = "FHD"
        const val VID_HD = "HD"
        const val VID_SD = "SD"
        val BS_SRC_AIR = 0
        val BS_SRC_CABLE = 1
        val BS_SRC_SAT = 2
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsCC(type: Int): Boolean {
        if (type == TvTrackInfo.TYPE_AUDIO) {
            var tracks = trackManager.getAudioTracks()
            for (track in tracks) {
                if (track.isHohAudio) return true
            }
            return false
        } else {
            var tracks = playbackView!!.getTracks(type)
            if (tracks != null)
                for (track in tracks) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (track!!.isHardOfHearing) return true
                    } else {
                        return false
                    }
                }
        }
        return false
    }

    override fun setDefaultAudioTrack() {
        Log.e("BHANYA", "settings default audio track as null")
        var firstAudLang = utilsInterface.getPrefsValue("key_first_audio_lang", "")
        var secondAudioLang = utilsInterface.getPrefsValue("key_second_first_lang", "")
        Log.e("BHANYA", "firstAudLang $firstAudLang , secondAudioLang $secondAudioLang")
        if (firstAudLang == "") {
            firstAudLang = utilsInterface.getPrimaryAudioLanguage()
            utilsInterface.setPrefsValue("key_first_audio_lang", firstAudLang ?: "")
            Log.e("BHANYA", "firstAudLang  value from db $firstAudLang")

        }
        if (secondAudioLang == "") {
            secondAudioLang = utilsInterface.getSecondaryAudioLanguage()
            utilsInterface.setPrefsValue("key_second_first_lang", secondAudioLang ?: "")
            Log.e("BHANYA", "secondAudioLang value from db $secondAudioLang")
        }
        val audioTracks = getAudioTracks()
        audioTracks.forEach {
            //TODO check about AD
            if (it.isAd) {
                Log.e("BHANYA", "play AD track ${it.trackName}")
                selectAudioTrack(it)
            } else if (it.languageName == firstAudLang) {
                selectAudioTrack(it)
            } else if (it.languageName == secondAudioLang) {
                selectAudioTrack(it)
            }
        }
    }

}