package com.iwedia.cltv.platform.base.player

import android.annotation.SuppressLint
import android.media.PlaybackParams
import android.media.tv.AitInfo
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvTrackInfo
import android.media.tv.TvView
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.SurfaceHolder
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.base.util.VideoDisplayUtil
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import java.lang.reflect.InvocationTargetException
import kotlin.random.Random
import kotlin.reflect.KCallable


open class PlayerBaseImpl(val utilsInterface: UtilsInterface,
                          val parentalControlSettingsInterface: ParentalControlSettingsInterface) :
    Player<TrackBaseManager>(TrackBaseManager(utilsInterface)) {

    protected lateinit var playbackView: TvView
    override lateinit var activePlayableItem: Any
    override var liveTabChannel: TvChannel? = null
    private var videoInfo = VideoInfo()
    private var displayMode = 0
    override var playbackStatus = MutableLiveData<PlaybackStatus>()
    override var wasScramble = false
    override var blockedRating: TvContentRating? = null
    override var isParentalActive = false
    override var isChannelUnlocked = false
    override var isOnLockScreen = false
    override var mServiceRef: KCallable<*>?= null
    var lastPlayedService: PlayableItem? = null
    private var pvrPlaybackStartPosition = 0L
    var listener: Any? = null
    open var noPlayback = false

    init {
        CoroutineHelper.runCoroutine({
            mServiceRef = TvView::class.members.single {
                it.name == "requestUnblockContent"
            }
        })

    }

    private val tvViewCallback: TvView.TvInputCallback = object : TvView.TvInputCallback() {
        override fun onConnectionFailed(inputId: String) {
            Log.w(TAG, "onConnectionFailed $inputId")
            mListeners.onNoPlayback()
            playerState = PlayerState.IDLE
            playbackStatus.value = PlaybackStatus.NO_PLAYBACK
        }

        override fun onDisconnected(inputId: String) {
            Log.w(TAG, "onDisconnected $inputId")
            mListeners.onNoPlayback()
            playerState = PlayerState.IDLE
            playbackStatus.value = PlaybackStatus.NO_PLAYBACK
        }

        override fun onChannelRetuned(inputId: String, channelUri: Uri) {
            Log.i(TAG, "onChannelReTuned $inputId $channelUri")
            mListeners.onPlaybackStarted()
            playerState = PlayerState.PLAYING
            playbackStatus.value = PlaybackStatus.PLAYBACK_STARTED
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onTracksChanged(inputId: String, tracks: List<TvTrackInfo>) {
            Log.i("BHANYA", "onTracksChanged $inputId")
            var activeAudioTrackId: String? = ""
            val selectedAudioLanguage = utilsInterface.getPrefsValue("AUDIO_FIRST_LANGUAGE", "") as String
            val selectedAudioLanguageAD = utilsInterface.getPrefsValue("AUDIO_FIRST_LANGUAGE_AD", false) as Boolean
            val selectedAudioTrackId = utilsInterface.getPrefsValue("AUDIO_FIRST_TRACK_ID", "") as String
            Log.e("BHANYA", "AselectedAudioLanguage = ${selectedAudioLanguage} , activeAudioTrackId :$activeAudioTrackId")
            if (selectedAudioLanguage.isNotEmpty()) {
                tracks.forEach { track ->
                   if((track.language == selectedAudioLanguage) && (track.id != playbackView.getSelectedTrack(TvTrackInfo.TYPE_AUDIO))
                       && track.type == TvTrackInfo.TYPE_AUDIO && utilsInterface.hasAudioDescription(track) == selectedAudioLanguageAD && track.id == selectedAudioTrackId) {
                       Log.e("BHANYA", "inside track name  = ${track} , activeAudioTrackId :$activeAudioTrackId")
                       activeAudioTrackId = track.id
                       selectAudioTrack(TrackBase.AudioTrack(track, utilsInterface,false,""))
                   }
                }
                if (activeAudioTrackId!!.isEmpty()) {
                    activeAudioTrackId = playbackView.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
                }
            } else {

                activeAudioTrackId = playbackView.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
                Log.e("BHANYA", "else case activeAudioTrackId = ${selectedAudioLanguage} , activeAudioTrackId :$activeAudioTrackId")
            }

            val activeSubtitleTrackId = playbackView.getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE)

            trackManager.updateTracks(activeAudioTrackId, activeSubtitleTrackId, tracks)

            Log.d("BHANYA", "Active Audio = ${trackManager.currentAudioTrack?.trackName} $activeAudioTrackId")
//            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Active Subtitle = ${trackManager.currentSubtitleTrack?.trackName} $activeSubtitleTrackId")


            mListeners.onAudioTrackUpdated(trackManager.getAudioTracks())
            mListeners.onSubtitleTrackUpdated(trackManager.getSubtitleTracks())
            InformationBus.informationBusEventListener?.submitEvent(Events.AUDIO_TRACKS_UPDATED)
        }

        @SuppressLint("LongLogTag")
        override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
            Log.d("BHANYA", "onTrackSelected $inputId, $type, $trackId ")

            val selectedAudioLanguage = utilsInterface.getPrefsValue("AUDIO_FIRST_LANGUAGE", "") as String
            val selectedAudioLanguageAD = utilsInterface.getPrefsValue("AUDIO_FIRST_LANGUAGE_AD", false) as Boolean
            val selectedAudioTrackId = utilsInterface.getPrefsValue("AUDIO_FIRST_TRACK_ID", "") as String

            mListeners.onTrackSelected(inputId, type, trackId)
//            mListeners.onPlaybackStarted()
//            playerState = PlayerState.PLAYING
//            cancelTimers()

//            if (mediaSessionPlayerStateListener != null) {
//                mediaSessionPlayerStateListener!!.onPlaybackStart()
//            }

            when (type) {
                TvTrackInfo.TYPE_AUDIO -> {
                    if (selectedAudioLanguage.isNotEmpty()) {
                        trackManager.getAudioTracks().forEach { track ->
                            if(track.track.language == selectedAudioLanguage
                                && utilsInterface.hasAudioDescription(track.trackInfo) == selectedAudioLanguageAD && track.track.id == selectedAudioTrackId) {
                                trackManager.selectAudioTrack(track.trackId)
                            }
                            if(track.isAnalogTrack && (track.trackId == trackId)) {
                                trackManager.selectAudioTrack(trackId)
                            }
                        }
                    } else {
                        trackManager.selectAudioTrack(trackId)
                    }
                }
                TvTrackInfo.TYPE_SUBTITLE -> {
                    trackManager.selectSubtitleTrack(trackId)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTrackSelected: On subtitle track selected!!!")
                    InformationBus.informationBusEventListener.submitEvent(Events.SUBTITLE_TRACK_SELECTED)
                }
                else -> {
                    if (trackId == null) {
                        videoInfo = VideoInfo()
                    } else {
                        val tracks = playbackView.getTracks(type)
                        val track = tracks?.find { it.id == trackId }

                        track?.let {
                            videoInfo.width = it.videoWidth
                            videoInfo.height = it.videoHeight
                            videoInfo.format = VideoDisplayUtil.resolveVideoDefinition(
                                videoInfo.width,
                                videoInfo.height
                            )
                            videoInfo.frameRate = it.videoFrameRate
                            videoInfo.aspectRation = VideoDisplayUtil.calculateAspectRatio(
                                videoInfo.width, videoInfo.height, it
                            )
                        }
                    }

                    setDisplayModeOnInit()
                }
            }
            if (isMuted) {
                mute()
            }
        }

        override fun onVideoSizeChanged(inputId: String, width: Int, height: Int) {
            Log.i(TAG, "onVideoSizeChanged: $width, $height")
        }

        override fun onVideoAvailable(inputId: String) {
            Log.i(TAG, "onVideoAvailable: $inputId")
            //used to set video resolution for the zap banner when video is available
            InformationBus.informationBusEventListener?.submitEvent(Events.VIDEO_RESOLUTION_AVAILABLE)

            noPlayback = false
            mListeners.onVideoAvailable(inputId)
            if(inputId.contains("Tuner")) {
                wasScramble = false
                if (isParentalActive) {
                    playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
                } else {
                    playbackStatus.value = PlaybackStatus.PLAYBACK_INIT
                    mListeners.onPlaybackStarted()
                    playbackStatus.value = PlaybackStatus.PLAYBACK_STARTED
                    playerState = PlayerState.PLAYING
                }

                // since playback comes after locking channel and there background sound from lock channel can come so muting background sound again
                if (isMuted) mute()
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onVideoUnavailable(inputId: String, reason: Int) {
            Log.w(TAG, "onVideoUnavailable: $inputId")
            mListeners.onVideoUnAvailable(reason, inputId)
            if(inputId.contains("Tuner")) {

                playerState = PlayerState.IDLE

                if (reason == 1) {
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

            // startVideoAvailabilityCheck()
        }

        override fun onContentAllowed(inputId: String) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onContentAllowed: $inputId")
            mListeners.onContentAvailable()
            playbackStatus.value = PlaybackStatus.PARENTAL_PIN_HIDE
            isParentalActive = false
            if (!wasScramble && !noPlayback) {
                playbackStatus.value = PlaybackStatus.PLAYBACK_STARTED
            }
            if (wasScramble){
                playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
            }
            else if (noPlayback) {
                playbackStatus.value = PlaybackStatus.NO_PLAYBACK
            }
        }

        override fun onContentBlocked(inputId: String, rating: TvContentRating) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onContentBlocked: $inputId")
            blockedRating = rating
            isParentalActive = true
            playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
            mListeners.onContentBlocked(rating)
        }

        override fun onTimeShiftStatusChanged(inputId: String, status: Int) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeShiftStatusChanged: ${TvInputManager.TIME_SHIFT_STATUS_AVAILABLE}")
            //todo check this later
            isTimeShiftAvailable = status == TvInputManager.TIME_SHIFT_STATUS_AVAILABLE
            InformationBus.informationBusEventListener?.submitEvent(Events.TIME_CHANGED_IN_TIMESHIFT)
            mListeners.onTimeShiftStatusChanged(inputId, isTimeShiftAvailable)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeShiftStatusChanged: $inputId [$status]")
        }

        override fun onAitInfoUpdated(inputId: String, aitInfo: AitInfo) {
            super.onAitInfoUpdated(inputId, aitInfo)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onAitInfoUpdated: $inputId")
        }

        override fun onSignalStrengthUpdated(inputId: String, strength: Int) {
            super.onSignalStrengthUpdated(inputId, strength)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSignalStrengthUpdated: $inputId")
        }

        override fun onTuned(inputId: String, channelUri: Uri) {
            super.onTuned(inputId, channelUri)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTuned: $inputId channelUri: $channelUri")
        }

        fun onEvent(inputId: String?, eventType: String?, eventArgs: Bundle?) {
            if((inputId != null) && (eventType != null) && (eventArgs != null)) {
                mListeners.onEvent(inputId, eventType, eventArgs)
            }
        }
    }


    override fun getIsParentalControlActive():Boolean{
        return if ((activePlayableItem as TvChannel).isFastChannel()) {
            parentalControlSettingsInterface.isAnokiParentalControlsEnabled()
        }else{
            parentalControlSettingsInterface.isParentalControlsEnabled()
        }
    }
    fun setActiveChannelLockedStatus(){
        if ((activePlayableItem as TvChannel).isFastChannel()) {
            if ((activePlayableItem as TvChannel).isLocked && !isChannelUnlocked && parentalControlSettingsInterface.isAnokiParentalControlsEnabled() ){
                playbackStatus.value = PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT
            }else{
                playbackStatus.value = PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT
            }
        } else {
            if ((activePlayableItem as TvChannel).isLocked && !isChannelUnlocked && parentalControlSettingsInterface.isParentalControlsEnabled() ){
                playbackStatus.value = PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT
            }else{
                playbackStatus.value = PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT
            }
        }
    }
    private fun setDisplayModeOnInit() {
        val selectedMode = 1    // Get it from shared pref ...

        val isAvailable = VideoDisplayUtil.isDisplayModeAvailable(selectedMode, videoInfo)

        if (isAvailable) {
            //We are setting store param 'false' as we will not save display mode until it is set from Pref. menu
            setDisplayMode(selectedMode, false)
        } else {
            //If video does not support any display mode changes we need to keep it 1(FULL) as default.
            setDisplayMode(1, false)
        }
    }

    fun setDisplayMode(mode: Int, animate: Boolean) {

    }

    fun setDisplaySize(width: Int, height: Int) {
        val params = VideoDisplayUtil.createLayoutParams(Pair(width, height))
        playbackView.layoutParams = params
        playbackView.invalidate()
        playbackView.requestFocus()
    }

    override fun getPlaybackTracks(type : Int) : List<TvTrackInfo> {
       var tracks : List<TvTrackInfo> = emptyList()
        try {
            if (playbackView != null) {
                tracks = playbackView.getTracks(type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tracks
    }

    fun getPlaybackSelectedTrack(type : Int) : String {
        var trackId = ""
        try {
            if (playbackView != null) {
                trackId = playbackView.getSelectedTrack(type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return trackId
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun isDolby(tvTrackInfo: TvTrackInfo): Boolean{
        Log.i("TVConfigurationDolby", "isDolby:${tvTrackInfo.encoding} ")
        return tvTrackInfo.encoding == Companion.CODEC_AUDIO_AC3 || tvTrackInfo.encoding == Companion.CODEC_AUDIO_AC3_ATSC ||tvTrackInfo.encoding == Companion.CODEC_AUDIO_EAC3 ||tvTrackInfo.encoding == Companion.CODEC_AUDIO_EAC3_ATSC ||tvTrackInfo.encoding == Companion.CODEC_AUDIO_DTS
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsDolby(type: Int): Boolean {
        var tracks = getPlaybackTracks(type)
        if (tracks.isNotEmpty())
            for (track in tracks) {
                try {
                    if (isDolby(track)) {
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
                        if (isDolby(track)) {
                            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "TRASCK IS DOLBY")
                            return track.encoding!!
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        return ""
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsCC(type: Int): Boolean {
        var tracks = playbackView!!.getTracks(type)
        if (tracks != null)
            for (track in tracks) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    return track!!.isHardOfHearing
                } else {
                    return false
                }

            }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        return tvTrackInfo!!.isAudioDescription
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsAudioDescription(type: Int): Boolean {
        var tracks = playbackView!!.getTracks(type)
        if (tracks != null)
            if (tracks.size != 0)
                for (track in tracks) {
                    try {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getIsAudioDescription: ${track.isAudioDescription}")
                        if (track!!.isAudioDescription) {
                            return true
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getTeleText(type: Int): Boolean {
        val tracks = playbackView?.getTracks(type)
        if (!tracks.isNullOrEmpty()) {
            for (track in tracks) {
                try {
                    if (track?.encoding == "teletext-full-page")
                        return true
                } catch (e : Exception) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getTeleText: ", e)
                }
            }
        }
        return false
    }

    override fun getVideoResolution(): String {
        return ""
    }

    override fun getAudioChannelIndex(type: Int): Int {
        return -1
    }

    override fun getAudioFormat(): String {
        return ""
    }

    override fun unlockChannel(): Boolean {
        return false
    }

    override fun switchAudioTrack():String? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "PlayerBaseImpl: switchAudioTrack")
        return ""
    }

    override fun setSubtitleSurface(holder: SurfaceHolder?) {}
    override fun setTeletextSurface(holder: SurfaceHolder?) {}
    override fun performBackgroundTuning(tvChannel: TvChannel) {}
    override fun initTTML(ttmlViewContainer: RelativeLayout) {}
    override fun refreshTTMLStatus() {}
    override fun setTTMLVisibility(isVisible: Boolean) {}

    override fun setCaptionEnabled(enabled: Boolean) {
        playbackView.setCaptionEnabled(enabled)
    }

    override fun selectSubtitle(subtitle: ISubtitle?) {
        if (subtitle == null) {
            setCaptionEnabled(false)
            playbackView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
        } else {
            setCaptionEnabled(true)
            playbackView.selectTrack(TvTrackInfo.TYPE_SUBTITLE, subtitle.trackId)
        }
    }

    override fun selectAudioTrack(audioTrack: IAudioTrack) {
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

    override fun setPlaybackView(playbackView: ViewGroup) {
        if (playbackView is TvView) {
            if (this::playbackView.isInitialized) {
                stop()
            }
            this.playbackView = playbackView
            this.playbackView.setCallback(tvViewCallback)
            playbackView.setCaptionEnabled(utilsInterface.getSubtitlesState())
        } else throw IllegalArgumentException("You should set TvView as playback view for player")
    }

    override fun play(playableItem: Any) {
        InformationBus.informationBusEventListener.submitEvent(
            Events.PVR_RECORDING_SHOULD_STOP,
            arrayListOf({
                continuePlay(playableItem)
            })
        )
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
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "continuePlay playableItem: ${playableItem.name} channenlUri: $channelUri")
                playbackView.tune(playableItem.inputId, channelUri)
                if (isMuted) mute()
                InformationBus.informationBusEventListener.submitEvent(
                    Events.CHANNEL_CHANGED,
                    arrayListOf(playableItem as TvChannel)
                )
                isChannelUnlocked = false
                setActiveChannelLockedStatus()
            }

            is Recording -> {
                val recordingUri = TvContract.buildRecordedProgramUri(playableItem.id.toLong())
                //mListeners.onVideoAvailable(playableItem.tvChannel!!.inputId)
                playbackView.reset()
                Log.d(Constants.LogTag.CLTV_TAG + "PVR TEST","Recording start")
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

    override fun pause() {
        if (playerState == PlayerState.PLAYING) {
            playbackView.reset()
            playerState = PlayerState.PAUSED
        }
    }

    override fun resume() {
        if(utilsInterface.getBlueMuteState() && noPlayback){
            return
        }
        else if ((playerState == PlayerState.PAUSED) || (playerState == PlayerState.STOPPED)) {
            play(activePlayableItem)
            playerState = PlayerState.PLAYING
        }
    }

    override fun stop() {
        if ( (!utilsInterface.getBlueMuteState()) || (utilsInterface.getBlueMuteState() && !noPlayback) ){
            try {
                if (activePlayableItem != null) {
                    when (activePlayableItem) {
                        is TvChannel -> {
                            playbackView.reset()
                            playerState = PlayerState.STOPPED
                            InformationBus.informationBusEventListener.submitEvent(Events.PLAYBACK_STOPPED)
                        }
                        is Recording -> {
                            playbackView.reset()
                            lastPlayedService?.let { play(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected var isMuted = false
    override fun mute() {
        isMuted = true
        (playbackView as TvView).setStreamVolume(1f)
        (playbackView as TvView).setStreamVolume(0f)
    }

    override fun unmute() {
        isMuted = false
        (playbackView as TvView).setStreamVolume(0f)
        (playbackView as TvView).setStreamVolume(1f)
    }

    override fun reset() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "reset: PlayerBaseImpl reset")
    }

    override fun seek(positionMs: Long, isRelative: Boolean) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "seek: PlayerBaseImpl seek")
    }

    override fun setSpeed(speed: Int) {
        val playbackParams = PlaybackParams()
        playbackParams.speed = speed.toFloat()
        playbackParams.pitch = 1.0f
        playbackView.timeShiftSetPlaybackParams(playbackParams)
    }

    override fun getSpeed() {
        TODO("Not yet implemented")
    }

    override fun slowDown() {
        TODO("Not yet implemented")
    }

    override fun speedUp() {
        TODO("Not yet implemented")
    }

    override fun getDuration(): Long {
        TODO("Not yet implemented")
    }

    override fun getPosition(): Long {
        TODO("Not yet implemented")
    }

    override fun requestUnblockContent(callback: IAsyncCallback) {
        if (((lastPlayedService is TvChannel) && (lastPlayedService as TvChannel).isFastChannel()) ||
            ((lastPlayedService !is TvChannel) && Constants.AnokiParentalConstants.USE_ANOKI_RATING_SYSTEM)) {
            utilsInterface.setPrefsValue(Constants.SharedPrefsConstants.ANOKI_PARENTAL_UNLOCKED_TAG, Random.nextInt())
            callback.onSuccess()
            return
        }
        var isSuccess = true
        CoroutineHelper.runCoroutine({
            if (mServiceRef == null) {
                mServiceRef = TvView::class.members.single {
                    it.name == "requestUnblockContent"
                }
            }
            val test = playbackView as TvView
            try {
                mServiceRef!!.call(test, blockedRating)
            } catch (e: Exception) {
                e.printStackTrace()
                isSuccess = false
                callback.onFailed(Error("Request unblock content failed!"))
                return@runCoroutine
            } catch (invocationTargetException: InvocationTargetException) {
                invocationTargetException.cause?.printStackTrace()
                isSuccess = false
                callback.onFailed(Error("Request unblock content failed!"))
                return@runCoroutine
            }
        })

        //Delay callback onSuccess triggering in order to receive onContentAllowed mw callback
        if (isSuccess) {
            Handler().postDelayed({
                callback.onSuccess()
            }, 1000)
        }
    }

    override fun setQuietTuneEnabled(enabled: Boolean) {
    }

    override fun getQuietTuneEnabled(): Boolean {
        return false
    }

    companion object {
        const val PLAYER_TYPE_DEFAULT = 100
        const val PLAYER_TYPE_TIME_SHIFT = 200
        const val PLAYER_TYPE_PVR = 300

        const val TAG = "TifPlayer"
        private const val CODEC_AUDIO_AC3 = "ac3"
        private const val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
        private const val CODEC_AUDIO_EAC3 = "eac3"
        private const val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
        private const val CODEC_AUDIO_DTS = "dts"
    }

    override fun setDefaultAudioTrack() {

    }
}