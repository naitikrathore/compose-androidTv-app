package com.iwedia.cltv.platform.rtk

import android.content.ContentValues
import android.content.Context
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.player.PlayerBaseImpl
import com.iwedia.cltv.platform.base.player.TrackBase
import com.iwedia.cltv.platform.`interface`.*
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
import com.iwedia.cltv.platform.rtk.provider.ChannelDataProvider
import com.iwedia.cltv.platform.rtk.util.ChannelsLoadedCallback
import com.iwedia.cltv.platform.rtk.util.MtsUtil
import com.iwedia.cltv.platform.rtk.util.RTK_INPUT_REGEX
import com.realtek.system.RtkProjectConfigs
import com.realtek.tv.ChannelsExConstants
import com.realtek.tv.RtkSettingConstants
import com.realtek.tv.TVMediaTypeConstants
import com.realtek.tv.Tv
import com.realtek.tv.ttml.SubtitleView
import com.realtek.tv.ttml.TtmlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.xmlpull.v1.XmlPullParserException

@RequiresApi(Build.VERSION_CODES.S)
internal class PlayerInterfaceImpl(var context: Context, utilsInterface: UtilsInterface, val epgInterface: EpgInterface, parentalControlSettingsInterface: ParentalControlSettingsInterface, val timeInterface: TimeInterface, val hbbTvInterface: HbbTvInterfaceImpl, val channelDataProvider: ChannelDataProvider) : PlayerBaseImpl(
    utilsInterface, parentalControlSettingsInterface
) , ChannelsLoadedCallback{
    private var ttmlManager: TtmlManager? = null
    private var ttmlViewContainer: RelativeLayout? = null
    override var playerState: PlayerState = PlayerState.IDLE
    override var isTimeShiftAvailable = false
    override var noPlayback = false
    private val teleTextTracks = arrayListOf<TvTrackInfo>()
    val TAG = "TifPlayer RTK"
    private var channelListLoaded = false
    private var directTuneCalled = false

    @RequiresApi(Build.VERSION_CODES.P)
    var tv: Tv? = null

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
                refreshTTMLStatus()
            }

            override fun onVideoAvailable(inputId: String) {
                InformationBus.informationBusEventListener.submitEvent(Events.VIDEO_RESOLUTION_UNAVAILABLE, arrayListOf(0))
                    wasScramble = false
                    noPlayback = false
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
                    }, 500)
                if(inputId.contains(RTK_INPUT_REGEX))
                    utilsInterface.setGoogleLauncherBehaviour(context, inputId)
            }

            override fun onVideoUnAvailable(reason: Int, inputId: String) {
                when (reason) {
                    TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY -> {
                        //To unmute audio since player was reset before channel tune for radio channel
                        if (!isMuted) unmute()
                        CoroutineHelper.runCoroutine({
                            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_HIDE_BLACK_OVERLAY)

                        }, Dispatchers.Main)
                        wasScramble = false
                        playbackStatus.value = PlaybackStatus.AUDIO_ONLY
                    }
                    18,
                    TVMediaTypeConstants.VIDEO_UNAVAILABLE_REASON_SCRAMBLED -> {
                        wasScramble = true
                        noPlayback = false
                        playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
                    }
                    1 -> {
                        wasScramble = false
                        noPlayback = false
                        playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                    }
                    2 -> {
                        noPlayback = true
                        playbackStatus.value = PlaybackStatus.NO_PLAYBACK
                    }
                    else -> {
                        noPlayback = false
                        playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                    }
                }
                refreshTTMLStatus()
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

                hbbTvInterface.onContentAllowed()
            }

            override fun onContentBlocked(rating: TvContentRating) {
                println("getLockedChannelList " + "onContentBlocked $rating")
                blockedRating = rating
                isParentalActive = true
                playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW

                hbbTvInterface.onContentBlocked()

                CoroutineHelper.runCoroutine({
                    InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_HIDE_BLACK_OVERLAY)

                }, Dispatchers.Main)
            }

            override fun onTimeShiftStatusChanged(inputId: String, status: Boolean) {
            }

            override fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {
                if(eventType == "dtv_network_service_change_update_now") {
                    val updateType = eventArgs.getInt("updateType", -1)
                    (utilsInterface as UtilsInterfaceImpl).getTvSetting().updateDtvNetworkServiceWithType(0, updateType)
                }
                if(eventType == "session_direct_tune_result"){
                    updateDirectTuneResult(inputId, eventArgs)
                }
                if (TVMediaTypeConstants.SESSION_CALLBACK_STRING_STREAM_TIME_UPDATED == eventType) {
                    var mDtvStreamUtcTimeMs = eventArgs.getInt("dtv_stream_utc_time_seconds",0)
                    if(mDtvStreamUtcTimeMs > 0L) {
                        (timeInterface as TimeInterfaceImpl).setStreamTime(mDtvStreamUtcTimeMs * 1000L)
                        val currentOffset: Int = eventArgs.getInt("dtv_stream_current_offset", 0) * 1000
                        val dstChangeTime = eventArgs.getInt("dtv_stream_dst_change_time", 0)
                        val nextOffset: Int = eventArgs.getInt("dtv_stream_next_offset_after_dst", 0) * 1000
                        (timeInterface as TimeInterfaceImpl).setStreamTimeOffset(currentOffset, dstChangeTime * 1000L, nextOffset)
                    }
                } else if ("TeleText_Track_List_Notify" == eventType) {
                    teleTextTracks.clear()
                    val ttIndexList = eventArgs.getStringArrayList("ttIndexList")
                    val ttLanguageList = eventArgs.getStringArrayList("ttLanguageList")
                    val hardHearingList = eventArgs.getIntegerArrayList("hardHearingList")

                    if (ttIndexList != null && ttLanguageList != null && hardHearingList != null && ttLanguageList.size == ttIndexList.size && hardHearingList.size == ttIndexList.size) {
                        for (i in ttIndexList.indices) {
                            val builder = TvTrackInfo.Builder(
                                TvTrackInfo.TYPE_SUBTITLE, ttIndexList[i]
                            )
                            builder.setLanguage(ttLanguageList[i])
                            val args = Bundle()
                            args.putInt("hardHearing", hardHearingList[i])
                            builder.setExtra(args)
                            teleTextTracks.add(builder.build())
                        }
                    }
                } else if (TVMediaTypeConstants.STRING_PLAY_FIRST_DTV_CHANNEL == eventType) {
                    val changeChannelId = eventArgs.getLong("currentChannelId", -1)
                    directTuneCalled = true
                    CoroutineHelper.runCoroutineForSuspend({
                        waitForChannelListLoadedEvent()
                        if (changeChannelId > -1) {
                            InformationBus.informationBusEventListener.submitEvent(
                                Events.DIRECT_TUNE_FORCE_PLAYBACK,
                                arrayListOf(changeChannelId)
                            )
                        }
                        channelListLoaded = false
                        directTuneCalled = false
                    }, Dispatchers.IO)
                }
            }

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
                if (type == TvTrackInfo.TYPE_VIDEO)
                    InformationBus.informationBusEventListener.submitEvent(Events.VIDEO_RESOLUTION_AVAILABLE, arrayListOf(0))

                refreshTTMLStatus()
            }
        })

        channelDataProvider.setChannelsLoadedCallback(this)
    }

    override fun onChannelsLoaded() {
        setChannelListLoaded(true)
    }

    private suspend fun waitForChannelListLoadedEvent() {
       withTimeoutOrNull(8000) { async {
           while (!channelListLoaded) {
           delay(100)
       } }.await() }
    }

    fun setChannelListLoaded(status: Boolean) {
        if(status && directTuneCalled) {
            channelListLoaded = true
        }
    }

    private fun updateDirectTuneResult(inputId: String, eventArgs: Bundle) {
        if (eventArgs == null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " eventArgs is null, no result;")
            return
        }
        directTuneCalled = true
        val channelId: Long = getDirectTuneChannelId(inputId, eventArgs)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, " updateDirectTuneResult channelId:$channelId")
        CoroutineHelper.runCoroutineForSuspend({
            waitForChannelListLoadedEvent()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onEvent: ChannelAdded = $channelListLoaded")
            if (channelId > -1) {
                InformationBus.informationBusEventListener.submitEvent(Events.DIRECT_TUNE_FORCE_PLAYBACK, arrayListOf(channelId))
            }
            channelListLoaded = false
            directTuneCalled = false
        }, Dispatchers.IO)
    }

    private fun getDirectTuneChannelId(inputId: String, args: Bundle): Long {
        val result = args.getBoolean("direct_tune_result", false)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getDirectTuneChannelId: $result")
        val channelId =  if (result) {
                args.getInt("direct_tune_channel_id", -1).toLong()
        } else {
            (lastPlayedService as TvChannel).channelId
        }
        return channelId
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun mute() {
        isMuted = true
        val tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
        val tvSrc = tv.getActivatedTvSource(0)

        when (tvSrc.src) {
            TVMediaTypeConstants.TV_SOURCE_DTV -> {
                //To mute for DVB/ATSC channels
                tv.disconnectTvAudio(TVMediaTypeConstants.TV_SOURCE_DTV, 0)
            }
            TVMediaTypeConstants.TV_SOURCE_ATV -> {
                //To mute for NTSC channels
                tv.disconnectTvAudio(TVMediaTypeConstants.TV_SOURCE_ATV, 0)
            }
            else -> {
                //To mute for live channels
                playbackView.setStreamVolume(0.0f)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun unmute() {
        isMuted = false
        val tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
        val tvSrc = tv.getActivatedTvSource(0)

        when (tvSrc.src) {
            TVMediaTypeConstants.TV_SOURCE_DTV -> {
                //To unmute for DVB/ATSC channels
                tv.connectTvAudio(TVMediaTypeConstants.TV_SOURCE_DTV, 0)
            }
            TVMediaTypeConstants.TV_SOURCE_ATV -> {
                //To unmute for NTSC channels
                tv.connectTvAudio(TVMediaTypeConstants.TV_SOURCE_ATV, 0)
            }
            else -> {
                //To unmute for live channels
                playbackView.setStreamVolume(1.0f)
            }
        }
    }

    override fun requestUnblockContent(callback: IAsyncCallback) {
        if((activePlayableItem as TvChannel).isFastChannel()){
            super.requestUnblockContent(callback)
        }else{
            //TODO
            super.requestUnblockContent(callback)
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsDolby(type: Int): Boolean {
        var tracks = getPlaybackTracks(type)
        if (tracks.isNotEmpty())
            for (track in tracks) {
                try {
                    val codecType = (utilsInterface as UtilsInterfaceImpl).getTvSetting()?.curDtvAudioSrcType

                    if (codecType != null) {
                        return if (codecType < 0 || codecType >= AUDIO_SRC_TYPE_ARRAY.size) {
                            false
                        } else {
                            val audioFormat = AUDIO_SRC_TYPE_ARRAY[codecType]
                            (audioFormat == TYPE_DOLBY_DIGITAL || audioFormat == TYPE_DOLBY_DIGITAL_PLUS
                                    || audioFormat == TYPE_AC4 || audioFormat == TYPE_MAT
                                    || audioFormat == TYPE_AC4_ATMOS || audioFormat == TYPE_DOLBY_DIGITAL_PLUS_ATMOS
                                    || audioFormat == TYPE_MAT_ATMOS)
                        }
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
                    val isAD = track.extra?.getInt("isAD", 0)
                    if (isAD != null) {
                        if (isAD>0) {
                            return true
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getIsCC(type: Int): Boolean {
        var tracks = playbackView!!.getTracks(type)

        if (tracks != null && tracks.isNotEmpty()) {
            for (track in tracks) {
                if ((utilsInterface as UtilsInterfaceImpl).hasHardOfHearingSubtitleInfo(track))
                    return true
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

            //TODO
        }

        Log.i(TAG, "hasAudioDescription : $audioDescriptionEnable ")
        return audioDescriptionEnable
    }
    @RequiresApi(Build.VERSION_CODES.P)
    fun getAtvMTSAudioListType(): Int {
        try {
            return (utilsInterface as UtilsInterfaceImpl).getTvSetting().atvMTSAudioListType
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return -1
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getAtvMTSAudioListIndex(): Int {
        try {
            return (utilsInterface as UtilsInterfaceImpl).getTvSetting().atvMTSAudioListIndex
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        return -1
    }
    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateVolumeTypeView() : String{
        val mtsListType: Int = getAtvMTSAudioListType()
        val mtsListIndex: Int = getAtvMTSAudioListIndex()
        var mtsAudioArr: Array<String> = when (mtsListType) {
            1 -> UtilsInterfaceImpl.stereo.toTypedArray()
            2 -> UtilsInterfaceImpl.dual.toTypedArray()
            3 -> UtilsInterfaceImpl.nicam_dual.toTypedArray()
            4 -> UtilsInterfaceImpl.sap_mono.toTypedArray()
            5 -> UtilsInterfaceImpl.sap_stereo.toTypedArray()
            6 -> UtilsInterfaceImpl.nicam_mono.toTypedArray()
            0 -> UtilsInterfaceImpl.mono.toTypedArray()
            else -> UtilsInterfaceImpl.mono.toTypedArray()
        }

        if (mtsListIndex >= 0 && mtsListIndex < mtsAudioArr.size) {
            return mtsAudioArr[mtsListIndex]
        }
        return ""
    }
    @RequiresApi(Build.VERSION_CODES.P)
    override fun getAudioChannelIndex(type: Int) : Int {
        var audioLangIdx = -1
        var tracks = getPlaybackTracks(type)
        var currentTrack = getActiveAudioTrack()
        tracks.forEachIndexed { i, track ->
            if (track.id == currentTrack?.trackId) {
                if(currentTrack!!.isAnalogTrack) {
                    audioLangIdx =getIndexForAnalogousTrack(currentTrack.analogName)
                }
            }else{
                val tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
                val tvSrc = tv.getActivatedTvSource(0)
                if (tvSrc.src == TVMediaTypeConstants.TV_SOURCE_ATV) {
                    val audioTrack = updateVolumeTypeView()
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAudioChannelIndex: ${audioTrack}")
                    audioLangIdx = getIndexForAnalogousTrack(audioTrack)
                }
            }
        }
        return audioLangIdx
    }
    private fun getIndexForAnalogousTrack(trackName:String?): Int {
        return when(trackName) {
            "Unknown" -> 0
            UtilsInterfaceImpl.mono_label -> 1
            UtilsInterfaceImpl.stereo_label -> 2
            UtilsInterfaceImpl.dual_1 -> 16
            UtilsInterfaceImpl.dual_2 -> 17
            UtilsInterfaceImpl.dual_12 -> 18
            UtilsInterfaceImpl.sap_mono_label -> 19
            UtilsInterfaceImpl.sap_stereo_label -> 20
            UtilsInterfaceImpl.nicam_mono_label -> 7
            else -> 2
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    override fun selectAudioTrack(audioTrack: IAudioTrack) {
        if(getTunerSource() == TVMediaTypeConstants.TV_SOURCE_ATV ){
            if(audioTrack.isAnalogTrack){
                val tv: Tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
                val listType: Int = tv.atvMTSAudioListType
                val selectedTrackName = audioTrack.analogName
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectAudioTrack: selectedTrackName=$selectedTrackName listType=$listType")
                if(!selectedTrackName.isNullOrEmpty()) {
                    var index = 0
                    index = try {
                        audioTrack.trackId.toInt()
                    }catch (e:Exception){
                        0
                    }
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectAudioTrack: index=$index listType=$listType")
                    val listIndex = MtsUtil.findMtsTypeIndex(listType, index)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectAudioTrack: listType=$listType listIndex=$listIndex")
                    tv.setAtvMTSAudioListIndex(index)
                    updateMtsTypeToDB(listIndex)
                }
            }
        }else {
            utilsInterface.setPrefsValue("AUDIO_FIRST_LANGUAGE", audioTrack.languageName)
            utilsInterface.setPrefsValue("AUDIO_FIRST_LANGUAGE_AD", audioTrack.isAd)
            utilsInterface.setPrefsValue("AUDIO_FIRST_TRACK_ID", audioTrack.trackId)
            playbackView.selectTrack(TvTrackInfo.TYPE_AUDIO, audioTrack.trackId)
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "selectAudioTrack: MainActivity: select audio ${
                    playbackView.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
                }"
            )
        }
        hbbTvInterface.updateConfigurations()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getTunerSource(): Int {
        val tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
        val tvSrc = tv.getActivatedTvSource(0)
        return tvSrc.src
    }

    private fun updateMtsTypeToDB(mtsTypeIndex: Int) {
        val curChInputID: String = getCurrentChannelInputID()
        val curChID: String = getCurrentChannelID(curChInputID)
        val values = ContentValues()
        values.put(ChannelsExConstants.COLUMN_MTS_TYPE, mtsTypeIndex)
        context.contentResolver.update(
            TvContract.Channels.CONTENT_URI,
            values,
            "_id = ? and input_id = ?",
            arrayOf<String>(curChID + "", curChInputID)
        )
    }
    private fun getCurrentChannelInputID(): String {
        val curChInputID = Settings.Global.getString(
            context.contentResolver,
            RtkSettingConstants.TV_CURRENT_INPUT_ID
        )
        return curChInputID ?: ""
    }
    private val SETTINGS_CURRENT_CHANNEL_ID_PREFIX = "channelidforinput_"
    private val INVALID_ID: Long = -1

    private fun getCurrentChannelID(curChInputID: String?): String {
        if (curChInputID == null || curChInputID == "") {
            return ""
        }
        val curChID = Settings.Global.getLong(
            context.getContentResolver(),
            SETTINGS_CURRENT_CHANNEL_ID_PREFIX + curChInputID,
            INVALID_ID
        )
        return if (curChID != INVALID_ID) curChID.toString() else ""
    }
    override fun getVideoResolution(): String {
        val VIDEO_SD_WIDTH = 704
        val VIDEO_SD_HEIGHT = 480
        val VIDEO_HD_WIDTH = 1280
        val VIDEO_HD_HEIGHT = 720
        val VIDEO_FULL_HD_WIDTH = 1920
        val VIDEO_FULL_HD_HEIGHT = 1080
        val VIDEO_ULTRA_HD_WIDTH = 2048
        val VIDEO_ULTRA_HD_HEIGHT = 1536

        try {
            if ((activePlayableItem is TvChannel)) {
                if (!(activePlayableItem as TvChannel).isFastChannel()) {
                    val tracks = playbackView.getTracks(TvTrackInfo.TYPE_VIDEO)
                    val trackId = playbackView.getSelectedTrack(TvTrackInfo.TYPE_VIDEO)

                    for (track in tracks) {
                        if (track.id.equals(trackId)) {
                            val width = track.videoWidth
                            val height = track.videoHeight

                            return if (width >= VIDEO_ULTRA_HD_WIDTH && height >= VIDEO_ULTRA_HD_HEIGHT) {
                                VID_UHD
                            } else if (width >= VIDEO_FULL_HD_WIDTH && height >= VIDEO_FULL_HD_HEIGHT) {
                                VID_FHD
                            } else if (width >= VIDEO_HD_WIDTH && height >= VIDEO_HD_HEIGHT) {
                                VID_HD
                            } else if (width >= VIDEO_SD_WIDTH && height >= VIDEO_SD_HEIGHT) {
                                VID_SD
                            } else {
                                if (height >= 720) VID_HD else VID_SD
                            }
                        }
                    }
                    return ""
                }
                return super.getVideoResolution()
            }
        }catch (E: Exception){
            println(E)
        }
        return super.getVideoResolution()
    }

    override fun getTeleText(type: Int): Boolean {
        if (teleTextTracks.isNotEmpty()) return true
        return false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun play(playableItem: Any) {
        // Show black overlay during channel switching
        if (playableItem is TvChannel) {
            if (!(playableItem as TvChannel).isLocked && !wasScramble) {
                CoroutineHelper.runCoroutine({
                    InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_SHOW_BLACK_OVERLAY)
                    if (isMuted) {
                        mute()
                    }
                }, Dispatchers.Main)
            }
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "play: ")
        if (playableItem is TvChannel && (playableItem as TvChannel).platformSpecific == "directTuneChannel") {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "play: ${(playableItem as TvChannel).inputId}")
            val bundle = Bundle()
            val channelUri = Uri.parse("content://android.media.tv/channel/-1")
            bundle.putBoolean("isDirectTune", true)
            bundle.putString("DirectTuneNumber", (playableItem as TvChannel).displayNumber)
            playbackView.tune(playableItem.inputId, channelUri, bundle)
        } else {
            //Resetting player if tuning channel is radio. This is to avoid displaying last frame of video channel.
            if (playableItem is TvChannel) {
                if (playableItem.isRadioChannel) {
                    stop()
                }
            }
            super.play(playableItem)
            refreshTTMLStatus()
        }
    }

    override fun setCaptionEnabled(enabled: Boolean) {
        super.setCaptionEnabled(enabled)
        refreshTTMLStatus()
    }

    override fun selectSubtitle(subtitle: ISubtitle?) {
        super.selectSubtitle(subtitle)

        if (subtitle != null) {
            if (subtitle.isHoh) {
                utilsInterface.setSubtitlesType((utilsInterface as UtilsInterfaceImpl).SUBTITLE_SETTING_TYPE_HEARING_IMPAIRED)
            } else {
                utilsInterface.setSubtitlesType((utilsInterface as UtilsInterfaceImpl).SUBTITLE_SETTING_TYPE_BASIC)
            }
        }

        refreshTTMLStatus()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getActiveSubtitle() : ISubtitle? {
        val tunerType = getTunerSource()

        if (tunerType == TVMediaTypeConstants.TV_SOURCE_DTV) {
            val activeSubtitle = trackManager.currentSubtitleTrack
            val selectedSubtitleTrackId =
                "s" + (utilsInterface as UtilsInterfaceImpl).getTvSetting()
                    .getDtvCurChSubtitleIndex(
                        TVMediaTypeConstants.TV_SOURCE_DTV
                    ).toString()

            if (activeSubtitle?.track?.id != selectedSubtitleTrackId) {
                trackManager.selectSubtitleTrack(selectedSubtitleTrackId)
            }
            return trackManager.currentSubtitleTrack
        }
        return super.getActiveSubtitle()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setSubtitleSurface(holder: SurfaceHolder?) {
        (utilsInterface as UtilsInterfaceImpl).getTvSetting().setDisplay(holder, TVMediaTypeConstants.OWNERSHIP_DTV_SUBTITLE)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setTeletextSurface(holder: SurfaceHolder?) {
        (utilsInterface as UtilsInterfaceImpl).getTvSetting().setDisplay(holder, TVMediaTypeConstants.OWNERSHIP_TELETEXT)
    }

    override fun initTTML(ttmlViewContainer: RelativeLayout) {
        if (isTTMLSupported()) {
            this.ttmlViewContainer = ttmlViewContainer
            setTTMLVisibility(true)
            try {
                val ttmlView = SubtitleView(context)
                ttmlViewContainer.addView(ttmlView)
                ttmlManager = TtmlManager((utilsInterface as UtilsInterfaceImpl).getTvSetting(), ttmlView)
            } catch (ex: XmlPullParserException) {
                Log.e(TAG, "Failed to create TtmlManager, exception: $ex")
            }
        }
    }

    override fun refreshTTMLStatus() {
        if (isTTMLSupported() && ttmlManager != null) {
            ttmlManager?.refreshStatus()
        }
    }

    override fun setTTMLVisibility(isVisible: Boolean) {
        if (isTTMLSupported() && ttmlViewContainer != null) {
            ttmlViewContainer?.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun isTTMLSupported(): Boolean {
        val boolString = RtkProjectConfigs.getInstance().getConfig(
            LIVETV_CATEGORY,
            LIVETV_CONFIG_TTML
        )
        return (boolString != null && boolString.isNotEmpty() && boolString.toBoolean())
    }

    companion object {
        const val TYPE_EMPTY = ""
        const val TYPE_PCM = "PCM"
        const val TYPE_DOLBY_DIGITAL = "DOLBY_DIGITAL"
        const val TYPE_DOLBY_DIGITAL_PLUS = "DOLBY_DIGITAL_PLUS"
        const val TYPE_MPEG = "MPEG"
        const val TYPE_AAC = "AAC"
        const val TYPE_HEAAC = "HEAAC"
        const val TYPE_DRA = "DRA"
        const val TYPE_MP3 = "MP3"
        const val TYPE_DTS = "DTS"
        const val TYPE_SIF = "SIF"
        const val TYPE_SIF_BTSC = "SIF_BTSC"
        const val TYPE_SIF_A2 = "SIF_A2"
        const val TYPE_DEFAULT = "DEFAULT"
        const val TYPE_NONE = "NONE"
        const val TYPE_DTS_HD_MA = "DTS_HD_MA"
        const val TYPE_DTS_EXPRESS = "DTS_EXPRESS"
        const val TYPE_DTS_CD = "DTS_CD"
        const val TYPE_DOLBY_DIGITAL_PLUS_ATMOS = "DOLBY_DIGITAL_PLUS_ATMOS"
        const val TYPE_AC4 = "AC4"
        const val TYPE_AC4_ATMOS = "AC4_ATMOS"
        const val TYPE_MPEG_H = "MPEG_H"
        const val TYPE_MAT = "MAT"
        const val TYPE_MAT_ATMOS = "MAT_ATMOS"
        const val TYPE_OPUS = "OPUS"
        const val TYPE_VOBIS = "VOBIS"
        const val TYPE_TRUEHD = "TRUEHD"
        const val TYPE_FLAC = "FLAC"
        const val TYPE_RA = "RA"
        const val TYPE_MPEG1 = "MPEG1"
        const val TYPE_MPEG2 = "MPEG2"
        const val TYPE_HDMV_LPCM = "HDMV_LPCM"
        const val TYPE_HDMV_DTS = "HDMV_DTS"
        const val TYPE_HDMV_DOLBY_LOSELESS = "HDMV_DOLBY_LOSELESS"
        const val TYPE_HDMV_DOLBY_DIGITAL_PLUS = "HDMV_DOLBY_DIGITAL_PLUS"
        const val TYPE_HDMV_DTS_HD_NO_XLL = "HDMV_DTS_HD_NO_XLL"
        const val TYPE_HDMV_DTS_HD_XLL = "HDMV_DTS_HD_XLL"
        const val TYPE_HDMV_DOLBY_DIGITAL_PLUS_SEC = "HDMV_DOLBY_DIGITAL_PLUS_SEC"
        const val TYPE_HDMV_DTS_HD_LBR = "HDMV_DTS_HD_LBR"
        const val TYPE_MPEG4_AAC = "MPEG4_AAC"
        const val TYPE_MPEG4_HEAAC = "MPEG4_HEAAC"

        val AUDIO_SRC_TYPE_ARRAY = arrayOf(
            TYPE_EMPTY,
            TYPE_PCM,
            TYPE_DOLBY_DIGITAL,
            TYPE_DOLBY_DIGITAL_PLUS,
            TYPE_MPEG,
            TYPE_AAC,
            TYPE_HEAAC,
            TYPE_DRA,
            TYPE_MP3,
            TYPE_DTS,
            TYPE_SIF,
            TYPE_SIF_BTSC,
            TYPE_SIF_A2,
            TYPE_DEFAULT,
            TYPE_NONE,
            TYPE_DTS_HD_MA,
            TYPE_DTS_EXPRESS,
            TYPE_DTS_CD,
            TYPE_DOLBY_DIGITAL_PLUS_ATMOS,
            TYPE_AC4,
            TYPE_AC4_ATMOS,
            TYPE_MPEG_H,
            TYPE_MAT,
            TYPE_MAT_ATMOS,
            TYPE_OPUS,
            TYPE_VOBIS,
            TYPE_TRUEHD,
            TYPE_FLAC,
            TYPE_RA
        )

        val CODEC_TYPE_ARRAY = arrayOf<String>(
            TYPE_EMPTY,
            TYPE_MPEG1,
            TYPE_MPEG2,
            TYPE_AAC,
            TYPE_DOLBY_DIGITAL,
            TYPE_DTS,
            TYPE_HDMV_LPCM,
            TYPE_HDMV_DTS,
            TYPE_HDMV_DOLBY_LOSELESS,
            TYPE_HDMV_DOLBY_DIGITAL_PLUS,
            TYPE_HDMV_DTS_HD_NO_XLL,
            TYPE_HDMV_DTS_HD_XLL,
            TYPE_HDMV_DOLBY_DIGITAL_PLUS_SEC,
            TYPE_HDMV_DTS_HD_LBR,
            TYPE_DRA,
            TYPE_MPEG4_AAC,
            TYPE_MPEG4_HEAAC,
            TYPE_DOLBY_DIGITAL_PLUS,
            TYPE_AC4
        )

        private const val CODEC_AUDIO_AC3 = "ac3"
        private const val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
        private const val CODEC_AUDIO_EAC3 = "eac3"
        private const val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
        private const val CODEC_AUDIO_DTS = "dts"
        const val VID_UHD = "UHD"
        const val VID_FHD = "FHD"
        const val VID_HD = "HD"
        const val VID_SD = "SD"

        private const val LIVETV_CATEGORY = "[LiveTV]"
        private const val LIVETV_CONFIG_TTML = "TTML"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getAudioTracks(): List<IAudioTrack> {
        val tunerType = getTunerSource()
        val audioTracks = mutableListOf<IAudioTrack>()
        val tv: Tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAudioTracks: ")
        if(tunerType == TVMediaTypeConstants.TV_SOURCE_ATV){
            val listType:Int = tv.getAtvMTSAudioListType()
            val atvTracksNames = (utilsInterface as UtilsInterfaceImpl).handleAnalogousAudioTracks(listType)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAudioTracks: ${atvTracksNames.toString()}")
            atvTracksNames.forEachIndexed { index, analogName ->
                val builder = TvTrackInfo.Builder(
                    TvTrackInfo.TYPE_AUDIO,
                    index.toString()
                    )
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAudioTracks: $analogName")
                val args = Bundle()
                args.putInt("hardHearing", 0)
                args.putInt("index", index)
                builder.setExtra(args)
                audioTracks.add(TrackBase.AudioTrack(builder.build(), utilsInterface, true, analogName))
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAudioTracks: ${audioTracks?.size}")
            return audioTracks
        }
        return super.getAudioTracks()
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun getActiveAudioTrack(): IAudioTrack? {
        val tunerType = getTunerSource()
        val tv: Tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
        if(tunerType == TVMediaTypeConstants.TV_SOURCE_ATV) {
            val listIndex: Int = tv.getAtvMTSAudioListIndex()
            val listType = tv.getAtvMTSAudioListType()
            val atvTracksNames = (utilsInterface as UtilsInterfaceImpl).handleAnalogousAudioTracks(listType)
            if(atvTracksNames.size > listIndex && listIndex >= 0) {
                val track = atvTracksNames[listIndex]
                val builder = TvTrackInfo.Builder(
                    TvTrackInfo.TYPE_AUDIO,
                    listIndex.toString()
                )
                val args = Bundle()
                args.putInt("hardHearing", 0)
                args.putInt("index", listIndex)
                builder.setExtra(args)
                return TrackBase.AudioTrack(builder.build(), utilsInterface, true, track)
            }
        } else if (tunerType == TVMediaTypeConstants.TV_SOURCE_DTV) {
            val activeAudio = trackManager.currentAudioTrack
            val selectedAudioTrackId = "a" + (utilsInterface as UtilsInterfaceImpl).getTvSetting()
                .getCurChAudioIndex(
                    TVMediaTypeConstants.TV_SOURCE_DTV).toString()

            if (activeAudio?.track?.id != selectedAudioTrackId) {
                trackManager.selectAudioTrack(selectedAudioTrackId)
            }
            return trackManager.currentAudioTrack
        }
        return super.getActiveAudioTrack()
    }
}
