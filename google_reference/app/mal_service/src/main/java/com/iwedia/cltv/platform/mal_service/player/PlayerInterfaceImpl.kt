package com.iwedia.cltv.platform.mal_service.player

import android.content.Context
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvTrackInfo
import android.media.tv.TvView
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncListener
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.mal_service.toServiceContentRating
import com.iwedia.cltv.platform.mal_service.toServiceSubtitleTrack
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import java.lang.reflect.InvocationTargetException
import kotlin.random.Random


internal class PlayerInterfaceImpl(var context: Context, private val serviceImpl: IServiceAPI, utilsInterface: UtilsInterface, parentalControlSettingsInterface: ParentalControlSettingsInterface) : PlayerBaseImpl(utilsInterface,parentalControlSettingsInterface) {

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
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeShiftStatusChanged, inputId $inputId $status");
            }

            override fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {
                try {
                    serviceImpl.onEvent(inputId, eventType, eventArgs)
                    serviceImpl.setTvInputId(inputId)
                } catch (e: Exception) {}
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
                serviceImpl.selectSubtitle(null, true)
            }
        } else {
            setCaptionEnabled(true)
            playbackView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, subtitle.trackId)
            if(isAnalogService()){
                serviceImpl.selectSubtitle(toServiceSubtitleTrack(subtitle), true)
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
            Log.d(Constants.LogTag.CLTV_TAG + "PlayerInterfaceImpl", "isDolbyAudioType tracks==null or tracks is empty!")
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
                    Log.d(Constants.LogTag.CLTV_TAG + "PlayerInterfaceImpl/getIsDolby", "TRACK IS DOLBY")
                    return true
                }
                else{
                    return false
                }
            }
        }
        Log.d(Constants.LogTag.CLTV_TAG + "PlayerInterfaceImpl", "isDolbyAudioType false")
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getTeleText(type: Int): Boolean {
        return utilsInterface.isTeletextAvailable()
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
    /*override fun getVideoResolution(): String {
        try {
            if ((activePlayableItem is TvChannel)) {
                if (!(activePlayableItem as TvChannel).isFastChannel()) {

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
    }*/

    override fun play(playableItem: Any) {
        mQuietTuneEnabled = false
        (trackManager as TrackManagerImpl).setPlayableItem(playableItem)
        continuePlay(playableItem)
    }

    private fun continuePlay(playableItem: Any) {
        activePlayableItem = playableItem
        when (playableItem) {
            is TvChannel -> {
                if (playbackStatus.value == PlaybackStatus.PARENTAL_PIN_SHOW) {
                    playbackStatus.value = PlaybackStatus.PARENTAL_PIN_HIDE
                }
                isParentalActive = false

                lastPlayedService = playableItem
                val channelUri = TvContract.buildChannelUri(playableItem.id.toLong())
                trackManager.clear()
                playbackView.tune(playableItem.inputId, channelUri)
                if (isMuted) mute()
                InformationBus.informationBusEventListener.submitEvent(
                    Events.CHANNEL_CHANGED,
                    arrayListOf(playableItem as TvChannel)
                )
                isChannelUnlocked = false
                serviceImpl.setChannelUnlocked(isChannelUnlocked)
                setActiveChannelLockedStatus()
            }

            is Recording -> {
                val recordingUri = TvContract.buildRecordedProgramUri(playableItem.id.toLong())
                playbackView.reset()
                mListeners.onVideoAvailable(playableItem.tvChannel!!.inputId)
                Log.d(Constants.LogTag.CLTV_TAG + "PVR TEST","Recording ref 5")
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

    override fun requestUnblockContent(callback: IAsyncCallback) {
        if (((lastPlayedService is TvChannel) && (lastPlayedService as TvChannel).isFastChannel()) ||
            ((lastPlayedService !is TvChannel) && Constants.AnokiParentalConstants.USE_ANOKI_RATING_SYSTEM)) {
            val sharedPreferences = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_UNLOCKED_TAG, Random.nextInt())
            editor.apply()
            callback.onSuccess()
            return
        }
        super.requestUnblockContent(callback)
        if (blockedRating != null) {
            var rating = toServiceContentRating(blockedRating!!)
            serviceImpl.requestUnblockContent(rating, object : IAsyncListener.Stub() {
                override fun onFailed(data: String?) {}

                override fun onSuccess() {}
            })
        }
    }

    override fun getSubtitleTracks(applicationMode: ApplicationMode): List<ISubtitle> {
        if (applicationMode == ApplicationMode.FAST_ONLY)
            return super.getSubtitleTracks(applicationMode)
        else {
            if (serviceImpl.isSubtitleRegion()) {
                return trackManager.getSubtitleTracks()
            }
            return ArrayList()
        }
    }

    fun getCCSubtitleTracks(): List<ISubtitle>{
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
                //playbackView.tune(playableItem.inputId, channelUri, bundle)
                if (isMuted) mute()
                InformationBus.informationBusEventListener.submitEvent(Events.CHANNEL_CHANGED, arrayListOf(playableItem as TvChannel))
                isChannelUnlocked = false
                serviceImpl.setChannelUnlocked(isChannelUnlocked)
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
        selectAudioTrack(nextAudioTrack)
        return nextAudioTrack.trackName
    }

    override fun unlockChannel(): Boolean {
        if(channelLockRating == null) {
            return false
        }

        serviceImpl.setChannelUnlocked(true)

        CoroutineHelper.runCoroutine({
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
                return@runCoroutine
            } catch (invocationTargetException: InvocationTargetException) {
                invocationTargetException.cause?.printStackTrace()
                return@runCoroutine
            }
        })

        return true
    }



    override fun setQuietTuneEnabled(enabled: Boolean) {
        mQuietTuneEnabled = enabled
    }

    override fun getQuietTuneEnabled(): Boolean {
        return mQuietTuneEnabled
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
    }
}