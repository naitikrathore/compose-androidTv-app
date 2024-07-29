package com.iwedia.cltv.tis.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Point
import android.media.tv.TvContentRating
import android.media.tv.TvInputManager
import android.media.tv.TvInputService
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.view.accessibility.CaptioningManager
import androidx.annotation.RequiresApi
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.core.view.DisplayCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.text.CueGroup
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.SubtitleView
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.AdvertisingIdHelper
import com.iwedia.cltv.platform.model.fast_backend_utils.IpAddressHelper
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.fast_backend_utils.KeyApiInterceptor
import com.iwedia.cltv.tis.helper.ChannelListHelper
import com.iwedia.cltv.tis.helper.ScanHelper
import com.iwedia.cltv.tis.model.ProgramDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.anoki.framework.BuildConfig
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_BACKWARD_PLAYER
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_FORWARD_PLAYER
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_PAUSE_PLAYER
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_PLAYBACK
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_PLAY_PLAYER
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_RELEASE_PLAYER
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_SEEK_TO
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_START_TIMER
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_STOP_PLAYER
import tv.anoki.ondemand.constants.StringConstants.ACTION_VOD_PLAYER_STOP_TIMER
import tv.anoki.ondemand.constants.StringConstants.BUNDLE_VOD_PLAYER_IS_PLAY
import tv.anoki.ondemand.constants.StringConstants.BUNDLE_VOD_PLAYER_LICENCE_URL
import tv.anoki.ondemand.constants.StringConstants.BUNDLE_VOD_PLAYER_RESUME_FROM
import tv.anoki.ondemand.constants.StringConstants.BUNDLE_VOD_PLAYER_SEEK_TO
import tv.anoki.ondemand.constants.StringConstants.BUNDLE_VOD_PLAYER_URL
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


/**
 * Custom TvInputService for ANOKI Channel Integration.
 * Uses ExoPlayer for playback.
 *
 * @author Abhilash M R
 */
class AnokiTvInputService : TvInputService() {
    private val TAG: String = this.toString()
    private lateinit var captionManager: CaptioningManager
    private lateinit var session: AnokiTvInputSessionImpl
    private var isParentalEnabled = false
    private var parentalControlsBroadcastReceiver: ParentalControlsBroadcastReceiver?= null

    /**
     * Parental controls broadcast receiver
     * Detects when parental controls is enabled/disabled
     */
    inner class ParentalControlsBroadcastReceiver: BroadcastReceiver() {

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onReceive(context: Context?, intent: Intent?) {
            checkParentalControls(context!!)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private var spListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG ||
            key == Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG ||
            key == Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_LEVEL_TAG) {

            if((key == Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_LEVEL_TAG) &&
                (sharedPreferences.getInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 0) == 0)) {
                return@OnSharedPreferenceChangeListener
            }

            isParentalEnabled = sharedPreferences.getBoolean(Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG, false)
            session.contentUnblocked = false
            if (isParentalEnabled) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Shared prefs parental controls enabled")
                val level = sharedPreferences.getInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, 0)
                var blockedRating = session.checkBlockedRating()
                if (!blockedRating) {
                    session.onUnblockContent(null)
                }
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Shared prefs parental controls disabled")
                session.onUnblockContent(null)
            }
        } else if (key == Constants.SharedPrefsConstants.ANOKI_PARENTAL_UNLOCKED_TAG) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Shared prefs parental controls unlocked")
            session.onUnblockContent(null)
        }
    }
    @RequiresApi(Build.VERSION_CODES.P)
    private fun checkParentalControls(context: Context) {
        try {
            session.contentUnblocked = false
            isParentalEnabled = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
                .getBoolean(Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG, false)
            if (!isParentalEnabled) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Parental controls disabled")
                session.onUnblockContent(null)
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Parental controls enabled")
                var blockedRating = session.checkBlockedRating()
                if (!blockedRating) {
                    session.onUnblockContent(null)
                }
            }
        } catch (ex : Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Parental controls failed")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Thread{IpAddressHelper.fetchPublicIpAddress(applicationContext)}.start()
        captionManager = getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
        var intentFilter = IntentFilter()
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED)
        intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED)
        parentalControlsBroadcastReceiver = ParentalControlsBroadcastReceiver()
        registerReceiver(parentalControlsBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        if (parentalControlsBroadcastReceiver != null) {
            unregisterReceiver(parentalControlsBroadcastReceiver)
        }
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateSession(inputId: String): Session? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateSession: $inputId")
        CoroutineHelper.runCoroutine({
            ChannelListHelper.initData(this)
        })
        val rendererFactory = CustomRenderersFactory(applicationContext)
        val mPlayer: ExoPlayer = ExoPlayer.Builder(applicationContext!!, rendererFactory).build()
        session = AnokiTvInputSessionImpl(applicationContext, captionManager, spListener, mPlayer)
        session.setOverlayViewEnabled(true)
        return session
    }

    class AnokiTvInputSessionImpl(
        private var context: Context?,
        private var captionManager: CaptioningManager,
        private val spListener: SharedPreferences.OnSharedPreferenceChangeListener,
        private var mPlayer: ExoPlayer
    ) :
        TvInputService.Session(context), Player.Listener {
        private val TAG = javaClass.simpleName
        private var isPlayingLive = true
        //Preferred audio/subtitle tracks shared prefs data
        private val PREFS_TAG = "LiveTVPrefs"
        val KEY_PREFERRED_AUDIO_LANGUAGE = "preferred_audio_language"
        val KEY_PREFERRED_SUBTITLE_LANGUAGE = "preferred_subtitle_language"
        val KEY_PREFERRED_SECOND_AUDIO_LANGUAGE = "preferred_second_audio_language"
        val KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE = "preferred_second_subtitle_language"
        val KEY_ADS_TARGETING = "ads_targeting"
        private val licenseServerUrlKey = "tjsOgiSfmEAcBOIp"

        private var playerHeight = 0
        private var playerWidth = 0
        var preferredAudioTrack = ""
        var preferredSecondAudioTrack = ""
        var preferredSubtitleTrack = ""
        var preferredSecondSubtitleTrack = ""

        private lateinit var mSurface: Surface
        private var subtitleView: SubtitleView ?= null
        private var captionEnabled = false

        private var trackMap = HashMap<Int, TvTrackInfo>()
        private var audioTracks = HashMap<Int, Tracks.Group>()
        private var subtitleTracks = HashMap<Int, Tracks.Group>()
        private var selectedAudioTrack = -1
        private var selectedSubtitleTrack = -1
        private var channelUri: Uri ?= null
        var contentUnblocked = false
        var parentalNextEventTimer: CountDownTimer?= null
        private var playbackJob: Job?= null

        init {
            captionEnabled = captionManager.isEnabled
            getDisplayMetrics()
            ReferenceApplication.applicationContext()?.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)?.registerOnSharedPreferenceChangeListener(spListener)
        }

        override fun onSetCaptionEnabled(enabled: Boolean) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSetCaptionEnabled $captionEnabled")
            captionEnabled = enabled
            selectSubtitleTrack(captionEnabled)
        }

        override fun onCreateOverlayView(): View {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreateOverlayView")
            val inflater = context?.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            subtitleView = inflater.inflate(com.iwedia.cltv.R.layout.subtitleview, null) as SubtitleView
            var captionStyle: CaptionStyleCompat = CaptionStyleCompat.createFromCaptionStyle(captionManager.userStyle)
            var captionTextSize = getCaptionFontSize()
            subtitleView?.setStyle(captionStyle)
            subtitleView?.setFixedTextSize(0, captionTextSize)
            subtitleView?.visibility = View.VISIBLE
            return subtitleView!!
        }

        private fun getCaptionFontSize(): Float {
            val display = (context!!.getSystemService(WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
            val displaySize = Point()
            display.getSize(displaySize)
            return Math.max(
                context!!.resources.getDimension(com.iwedia.cltv.R.dimen.font_13),
                0.0533f * Math.min(
                    displaySize.x,
                    displaySize.y
                )
            )
        }

        override fun onCues(cueGroup: CueGroup) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCues:")
            if (subtitleView != null) {
                subtitleView?.setCues(cueGroup.cues)
            }
        }

        override fun onRelease() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onRelease:")
            //Stop playback and release the Player.
            mPlayer.stop()
            mPlayer.release()
            ReferenceApplication.applicationContext()?.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)?.unregisterOnSharedPreferenceChangeListener(spListener)
        }

        override fun onSetSurface(surface: Surface?): Boolean {
            if (mPlayer != null) {
                if (surface != null) {
                    mSurface = surface
                    mPlayer.setVideoSurface(mSurface)
                }
            }
            return true
        }

        override fun onSetStreamVolume(volume: Float) {
            mPlayer.volume = volume
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onTune(channelUri: Uri): Boolean {
            if (channelUri == Uri.EMPTY) {
                return true
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTune: $channelUri")
            if (this.channelUri != null) {
                if (this.channelUri != channelUri) {
                    contentUnblocked = false
                    stopNextEventParentalTimer()
                }
            }
            this.channelUri = channelUri

            selectedAudioTrack = -1
            selectedSubtitleTrack = -1
            trackMap.clear()
            audioTracks.clear()
            subtitleTracks.clear()

            val dnt = if (context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(KEY_ADS_TARGETING, true)) 0 else 1
            val country  = context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getString(ScanHelper.PREFS_KEY_CURRENT_COUNTRY_ALPHA3, "USA").toString()
            var auid = context!!.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(
                ScanHelper.PREFS_KEY_AUID, "")
                .toString()
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING)
            if (!channelUri.toString().isNullOrEmpty()) {
                var temp = channelUri.toString().split("/")
                var id = temp[temp.size - 1]
                val channel = ChannelListHelper.findChannelById(id.toLong())
                var playbackUrl = channel?.mChPlaybackUrl
                if (!playbackUrl.isNullOrEmpty()) {
                    playbackJob?.cancel()
                    playbackJob = CoroutineScope(Dispatchers.IO).launch {
                        playbackUrl =
                            playbackUrl!!
                                .replace("{{IP}}", IpAddressHelper.getIpAddress())
                                .replace("{{DNT}}",dnt.toString())
                                .replace("{{APP_BUNDLE_ID}}",context!!.packageName)
                                .replace("{{COUNTRY}}",country)
                                .replace("{{DID}}",AdvertisingIdHelper.getAdvertisingId(context!!))
                                .replace("{{PLAYER_WIDTH}}",playerWidth.toString())
                                .replace("{{PLAYER_HEIGHT}}",playerHeight.toString())
                                .replace("{{AUID}}",auid)
                    }

                }
                //var playbackUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
                //var playbackUrl = "https://media.axprod.net/TestVectors/v7-Clear/Manifest_1080p.mpd"

                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTune: playbackUrl = $playbackUrl")
                var drmUrl = getDecryptedDrmUrl(channel!!.mLicenseServerUrl)
                if (!playbackUrl.isNullOrEmpty()) {
                    // Create a player instance if its not exists.
                    val rendererFactory = CustomRenderersFactory(context)
                    if (mPlayer == null) mPlayer = ExoPlayer.Builder(context!!, rendererFactory).build()
                    mPlayer!!.stop()
                    if(drmUrl.isNotEmpty()){
                        drmUrl = drmUrl.replace("{{AUID}}", auid).replace("{{KEY}}", KeyApiInterceptor().getProductionKey())
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTune: drmUrl === $drmUrl")
                        mPlayer!!.setMediaItem(MediaItem.Builder()
                            .setUri(playbackUrl)
                            .setDrmConfiguration(
                                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                                    .setLicenseUri(drmUrl).build()
                            ).build())
                    } else {
                        mPlayer!!.setMediaItem(MediaItem.Builder().setUri(playbackUrl).build())
                    }
                    // Prepare the player.
                    mPlayer!!.prepare()
                    // to allow content playback after parental check.

                    var blockedRating = checkBlockedRating()
                    if(!blockedRating) {
                        notifyContentAllowed()
                    }
                    mPlayer.addListener(this)
                    // Start Playback.
                    mPlayer!!.play()

                    isPlayingLive = true
                }

            }
            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE)

            return true
        }

        private fun getDecryptedDrmUrl(cipherText: String): String {
            if(cipherText.isEmpty() || cipherText==null || cipherText == "NULL") return ""
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getDecryptedDrmUrl: cipherText === $cipherText, licenseServerUrlKey === $licenseServerUrlKey")
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            val secretKey = SecretKeySpec(licenseServerUrlKey.toByteArray(), "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText))
            return String(decryptedBytes)
        }

        @RequiresApi(Build.VERSION_CODES.P)
        fun checkBlockedRating(): Boolean{
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkBlockedRating:")
            if (Constants.AnokiParentalConstants.USE_ANOKI_RATING_SYSTEM) {
                var level = ReferenceApplication.applicationContext().getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getInt(Constants.SharedPrefsConstants.ANOKI_PARENTAL_CONTROLS_LEVEL_TAG, 0)
                if(ReferenceApplication.applicationContext().getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_ENABLED_TAG, 0) == 1) {
                    level = ReferenceApplication.applicationContext().getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getInt(Constants.SharedPrefsConstants.ANOKI_TEMPORARY_RATING_LEVEL_TAG, 0)
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "anoki level rating $level")

                var temp = channelUri.toString().split("/")

                //for Vod content
                if(channelUri == null) {
                    return false
                }

                var channelId = temp[temp.size - 1]
                var playbackUrl = ChannelListHelper.findChannelById(channelId.toLong())?.mChPlaybackUrl
                if (playbackUrl!= null && !contentUnblocked) {

                    var contentRating: TvContentRating? =
                        ProgramDescriptor.getProgramRating(context!!, channelId.toLong())
                            ?: return false
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Blocked content rating main rating system ${contentRating!!.mainRating}")
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Content rating rating system ${contentRating!!.ratingSystem}")
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Content rating rating flattenToString ${contentRating!!.flattenToString()}")
                    var contentBlocked = false

                    val ratingLevel = contentRating.mainRating.split("_")
                    if (ratingLevel.isNotEmpty() && ratingLevel.size == 3) {
                        //Anoki rating level starts with value 1 so here we decrease it by 1
                        contentBlocked = level < ratingLevel[2].toInt() - 1
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "is content blocked $contentBlocked")

                        var isParentalEnabled = context?.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)?.getBoolean(Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG, false) ?: false
                        if (contentBlocked && isParentalEnabled) {
                            mPlayer.stop()
                            notifyContentBlocked(contentRating)
                            return true
                        }
                    }
                    var endTime = ProgramDescriptor.getCurrentEventEndTime(context!!, channelId.toLong())
                    startNextEventParentalTimer(endTime)
                }
                return false
            }
            var temp = channelUri.toString().split("/")
            var channelId = temp[temp.size - 1]
            var playbackUrl = ChannelListHelper.findChannelById(channelId.toLong())?.mChPlaybackUrl

            //TODO only for testing should be removed once when the parental rating will be added on Anoki server
            var tvInputManager: TvInputManager = context!!.getSystemService(TV_INPUT_SERVICE) as TvInputManager
            Log.d(Constants.LogTag.CLTV_TAG + TAG,"checkBlockedRating isParentalEnabled = ${tvInputManager.isParentalControlsEnabled}")
            if (playbackUrl!= null && !contentUnblocked && tvInputManager.isParentalControlsEnabled) {

                var contentRating: TvContentRating? =
                    ProgramDescriptor.getProgramRating(context!!, channelId.toLong())
                        ?: return false
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Blocked content rating main rating system ${contentRating!!.mainRating}")
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Content rating rating system ${contentRating!!.ratingSystem}")
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Content rating rating flattenToString ${contentRating!!.flattenToString()}")
                var contentBlocked = false
                tvInputManager.blockedRatings.forEach { tvContentRating->
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Blocked content rating main rating system ${tvContentRating.mainRating}")
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Blocked content rating rating system ${tvContentRating.ratingSystem}")
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Blocked content rating rating flattenToString ${tvContentRating.flattenToString()}")
                }
                contentBlocked = tvInputManager.isRatingBlocked(contentRating)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "is content blocked $contentBlocked")
                var endTime = ProgramDescriptor.getCurrentEventEndTime(context!!, channelId.toLong())
                startNextEventParentalTimer(endTime)
                if (contentBlocked) {
                    mPlayer.stop()
                    notifyContentBlocked(contentRating)
                    return true
                }
            }
            return false
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPlayerError:")

            if(!isPlayingLive){
                mPlayer.stop()
                InformationBus.submitEvent(Event(Events.VOD_PLAY_BACK_ERROR, error.message))
            }else{
                mPlayer.stop()
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN)
                //Retune on playback error
                onTune(channelUri!!)
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onRenderedFirstFrame:")
            var blockedRating = checkBlockedRating()
            if (!blockedRating) {
                notifyVideoAvailable()
            }
        }

        override fun notifyTrackSelected(type: Int, trackId: String?) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "notifyTrackSelected: ")
            super.notifyTrackSelected(type, trackId)
        }

        override fun notifyTracksChanged(tracks: MutableList<TvTrackInfo>?) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "notifyTracksChanged: ")
            super.notifyTracksChanged(tracks)
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onSelectTrack(type: Int, trackId: String?): Boolean {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSelectTrack:")
            if (trackId == null) return true
            if (type == TvTrackInfo.TYPE_AUDIO) {
                selectedAudioTrack = trackId.toInt()
                selectAudioTrack()
            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                selectedSubtitleTrack = trackId.toInt()
                selectSubtitleTrack(captionEnabled)
            }

            notifyTrackSelected(type, trackId)
            return true
        }

        override fun onTracksChanged(tracks: Tracks) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTracksChanged: ")
            super.onTracksChanged(tracks)
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPlaybackStateChanged: $playbackState & isPlayingLive $isPlayingLive")
            when (playbackState) {
                ExoPlayer.STATE_IDLE -> {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPlaybackStateChanged: STATE_IDLE ")
                }

                ExoPlayer.STATE_READY -> {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPlaybackStateChanged: STATE_READY ")
                    if(!isPlayingLive) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "VOD_DURATION_POSITION: ")
                        InformationBus.submitEvent(Event(Events.VOD_DURATION_POSITION, mPlayer.duration))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "VOD_PLAY_STATE: ")
                        InformationBus.submitEvent(Event(Events.VOD_PLAY_STATE, mPlayer.isPlaying))
                    }else{
                        var blockedRating = checkBlockedRating()
                        if (!blockedRating) {
                            notifyTracksChanged(getAllTracks())
                            notifyVideoAvailable()
                            getPreferredTracks()
                            notifyTrackSelected()
                        }
                    }
                }

                ExoPlayer.STATE_BUFFERING -> {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPlaybackStateChanged: STATE_BUFFERING")
                    if(isPlayingLive) {
                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING)
                    }
                }
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "VOD_PLAYER_STATE: ")
            InformationBus.submitEvent(Event(Events.VOD_PLAYER_STATE, playbackState))
        }

        private fun getAllTracks(): MutableList<TvTrackInfo> {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllTracks:")
            trackMap.clear()
            audioTracks.clear()
            subtitleTracks.clear()
            val tracks = mutableListOf<TvTrackInfo>()
            val count = mPlayer.currentTracks.groups.size
            if (count > 0) {
                for (i in 0 until count) {
                    val format = mPlayer.currentTracks.groups[i].getTrackFormat(0)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAllTracks: format : $format ")
                    val trackId = tracks.size.toString()
                    format.sampleMimeType?.let {
                        var trackType = if (it.contains("audio")) TvTrackInfo.TYPE_AUDIO
                        else if (it.contains("video")) TvTrackInfo.TYPE_VIDEO
                        else if (it.contains("text") || it.contains("ttml") || it.contains("cea")) TvTrackInfo.TYPE_SUBTITLE
                        else TvTrackInfo.TYPE_VIDEO
                        val builder = trackId?.let { TvTrackInfo.Builder(trackType, trackId) }
                        if (trackType == TvTrackInfo.TYPE_VIDEO) {
                            builder?.setVideoWidth(format.width)
                            builder?.setVideoHeight(format.height)
                        } else if (trackType == TvTrackInfo.TYPE_AUDIO) {
                            builder?.setAudioChannelCount(format.channelCount)
                            builder?.setAudioSampleRate(format.sampleRate)
                            if (format.language != null) {
                                builder?.setLanguage(format.language!!)
                            }
                            if (selectedAudioTrack == -1) {
                                selectedAudioTrack = tracks.size
                            }
                        } else if (trackType == TvTrackInfo.TYPE_SUBTITLE) {
                            if (format.language != null) {
                                builder?.setLanguage(format.language!!)
                            }
                            if (selectedSubtitleTrack == -1) {
                                selectedSubtitleTrack = tracks.size
                            }
                        }
                        if (builder != null) {
                            var track = builder.build()

                            if (mPlayer.currentTracks.groups[i].type == C.TRACK_TYPE_AUDIO) {
                                audioTracks[tracks.size] = mPlayer.currentTracks.groups[i]
                            }
                            if (mPlayer.currentTracks.groups[i].type == C.TRACK_TYPE_TEXT) {
                                subtitleTracks[tracks.size] = mPlayer.currentTracks.groups[i]
                            }
                            trackMap[tracks.size] = track
                            tracks.add(track)
                        }
                    }
                }
            }
            return tracks
        }

        /**
         * Select audio track
         */
        @RequiresApi(Build.VERSION_CODES.P)
        private fun selectAudioTrack() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectAudioTrack:")
            var blockedRating = checkBlockedRating()
            if (audioTracks.contains(selectedAudioTrack) && !blockedRating) {
                mPlayer.trackSelectionParameters = mPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setOverrideForType(TrackSelectionOverride(audioTracks[selectedAudioTrack]!!.mediaTrackGroup, 0))
                    .build()
            }
        }

        /**
         * Select subtitle track
         */
        private fun selectSubtitleTrack(enabled: Boolean) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectSubtitleTrack $enabled")
            if (enabled) {
                if (subtitleTracks.contains(selectedSubtitleTrack)) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectSubtitleTrack ${subtitleTracks[selectedSubtitleTrack]?.mediaTrackGroup?.getFormat(0)?.language}")
                    mPlayer.trackSelectionParameters = mPlayer.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setOverrideForType(TrackSelectionOverride(subtitleTracks[selectedSubtitleTrack]!!.mediaTrackGroup, 0))
                        .build()
                }
            } else {
                mPlayer.trackSelectionParameters = mPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .build()
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun notifyTrackSelected() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "notifyTrackSelected")
            if (selectedAudioTrack != -1 && trackMap.contains(selectedAudioTrack)) {
                selectAudioTrack()
                notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, selectedAudioTrack.toString())
            }

            if (selectedSubtitleTrack != -1 && trackMap.contains(selectedSubtitleTrack)) {
                selectSubtitleTrack(captionEnabled)
                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, selectedSubtitleTrack.toString())
            }
        }

        private fun selectPreferredTracks() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Preferred audio track $preferredAudioTrack")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Preferred subtitle track $preferredSubtitleTrack")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Preferred second audio track $preferredSecondAudioTrack")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Preferred second subtitle track $preferredSecondSubtitleTrack")
            var preferredAudioTrackSet = false
            var preferredSubtitleTrackSet = false
            if (preferredAudioTrack.isNotEmpty()) {
                var preferredLanguage = Locale(preferredAudioTrack)
                audioTracks.forEach {
                    if (it.value.mediaTrackGroup.getFormat(0).language != null) {
                        var audioLang = Locale(it.value.mediaTrackGroup.getFormat(0).language!!)
                        if (audioLang.displayLanguage == preferredLanguage.displayLanguage) {
                            selectedAudioTrack = it.key
                            preferredAudioTrackSet = true
                            mPlayer.trackSelectionParameters = mPlayer.trackSelectionParameters
                                .buildUpon()
                                .setPreferredTextLanguage(preferredAudioTrack)
                                .build()
                        }
                    }
                }
            }

            if (preferredSecondAudioTrack.isNotEmpty() && !preferredAudioTrackSet) {
                var preferredLanguage = Locale(preferredSecondAudioTrack)
                audioTracks.forEach {
                    if (it.value.mediaTrackGroup.getFormat(0).language != null) {
                        var audioLang = Locale(it.value.mediaTrackGroup.getFormat(0).language!!)
                        if (audioLang.displayLanguage == preferredLanguage.displayLanguage) {
                            selectedAudioTrack = it.key
                            preferredAudioTrackSet = true
                            mPlayer.trackSelectionParameters = mPlayer.trackSelectionParameters
                                .buildUpon()
                                .setPreferredTextLanguage(preferredAudioTrack)
                                .build()
                        }
                    }
                }
            }

            if (preferredSubtitleTrack.isNotEmpty()) {
                var preferredLanguage = Locale(preferredSubtitleTrack)
                subtitleTracks.forEach {
                    if (it.value.mediaTrackGroup.getFormat(0).language != null) {
                        var subtitleLang = Locale(it.value.mediaTrackGroup.getFormat(0).language!!)
                        if (subtitleLang.displayLanguage == preferredLanguage.displayLanguage) {
                            selectedSubtitleTrack = it.key
                            preferredSubtitleTrackSet = true
                            mPlayer.trackSelectionParameters = mPlayer.trackSelectionParameters
                                .buildUpon()
                                .setPreferredTextLanguage(preferredSubtitleTrack)
                                .build()
                        }
                    }
                }
            }
            if (preferredSecondSubtitleTrack.isNotEmpty() && !preferredSubtitleTrackSet) {
                var preferredLanguage = Locale(preferredSecondSubtitleTrack)
                subtitleTracks.forEach {
                    if (it.value.mediaTrackGroup.getFormat(0).language != null) {
                        var subtitleLang = Locale(it.value.mediaTrackGroup.getFormat(0).language!!)
                        if (subtitleLang.displayLanguage == preferredLanguage.displayLanguage) {
                            selectedSubtitleTrack = it.key
                            mPlayer.trackSelectionParameters = mPlayer.trackSelectionParameters
                                .buildUpon()
                                .setPreferredTextLanguage(preferredSubtitleTrack)
                                .build()
                        }
                    }
                }
            }
        }

        private fun getPreferredTracks() {
            preferredAudioTrack = context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getString(KEY_PREFERRED_AUDIO_LANGUAGE, "") as String
            preferredSubtitleTrack = context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getString(KEY_PREFERRED_SUBTITLE_LANGUAGE, "") as String
            preferredSecondAudioTrack = context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getString(KEY_PREFERRED_SECOND_AUDIO_LANGUAGE, "") as String
            preferredSecondSubtitleTrack = context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getString(KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE, "") as String
            selectPreferredTracks()
        }

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onUnblockContent(unblockedRating: TvContentRating?) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onUnblockContent: ")
            super.onUnblockContent(unblockedRating)
            contentUnblocked = true
            onTune(channelUri!!)
        }

        override fun notifyContentBlocked(rating: TvContentRating) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "notifyContentBlocked: ")
            super.notifyContentBlocked(rating)
        }

        override fun notifyContentAllowed() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "notifyContentAllowed: ")
            super.notifyContentAllowed()
        }

        override fun onTimeShiftGetStartPosition(): Long {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeShiftGetStartPosition: ")
            return super.onTimeShiftGetStartPosition()
        }

        private fun getDisplayMetrics() {
            val displayManager = DisplayManagerCompat.getInstance(context!!)
            val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

            defaultDisplay?.let { display ->
                var temp = DisplayCompat.getSupportedModes(context!!, display)
                playerHeight = temp[0].physicalHeight
                playerWidth = temp[0].physicalWidth
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getDisplayMetrics $playerWidth x $playerHeight")
            }
        }

        private fun stopNextEventParentalTimer() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopNextEventParentalTimer:")
            if (parentalNextEventTimer != null) {
                parentalNextEventTimer?.cancel()
                parentalNextEventTimer = null
            }
        }
        private fun startNextEventParentalTimer(endTime: Long) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "startNextEventParentalTimer:")
            stopNextEventParentalTimer()
            var timerEndTime = endTime - System.currentTimeMillis()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "startNextEventParentalTimer  ${Date(endTime)} $timerEndTime")
            parentalNextEventTimer = object : CountDownTimer(timerEndTime, 1000) {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onFinish() {
                    checkBlockedRating()
                }

                override fun onTick(millisUntilFinished: Long) {
                }

            }.start()
        }

        /**
         * This method overrides the onAppPrivateCommand method from a superclass.
         *
         * @param action The action string.
         * @param data The Bundle containing additional data.
         */
        override fun onAppPrivateCommand(action: String, data: Bundle?) {
            when(action){
                ACTION_VOD_PLAYER_PLAYBACK -> {
                    val playbackUrl = data!!.getString(BUNDLE_VOD_PLAYER_URL)
                    val resumeFromSec = data.getInt(BUNDLE_VOD_PLAYER_RESUME_FROM)
                    val isPlaying = data.getBoolean(BUNDLE_VOD_PLAYER_IS_PLAY, true)
                    val licenceUrl = data.getString(BUNDLE_VOD_PLAYER_LICENCE_URL, "")
                    prepareAndPlay(playbackUrl, resumeFromSec, isPlaying, licenceUrl)
                }

                ACTION_VOD_PLAYER_PLAY_PLAYER -> {
                    if (!mPlayer.isPlaying) mPlayer.play()
                    InformationBus.submitEvent(Event(Events.VOD_PLAY_STATE, mPlayer.isPlaying))
                }

                ACTION_VOD_PLAYER_PAUSE_PLAYER -> {
                    if (mPlayer.isPlaying) mPlayer.pause()
                    InformationBus.submitEvent(Event(Events.VOD_PAUSE_STATE, mPlayer.isPlaying))
                }

                ACTION_VOD_PLAYER_SEEK_TO -> {
                    val seekTo = data!!.getLong(BUNDLE_VOD_PLAYER_SEEK_TO)
                    if(!mPlayer.isPlaying) mPlayer.play()
                    mPlayer.seekTo(seekTo)
                    InformationBus.submitEvent(Event(Events.VOD_SEEK_TO_STATE))
                }

                ACTION_VOD_BACKWARD_PLAYER -> {
                    val seekTo = data!!.getLong(BUNDLE_VOD_PLAYER_SEEK_TO)
                    val newPosition = mPlayer.currentPosition.minus(seekTo)
                    if(!mPlayer.isPlaying) mPlayer.play()
                    mPlayer.seekTo(newPosition)
                    InformationBus.submitEvent(Event(Events.VOD_BACKWARD_STATE))
                }

                ACTION_VOD_FORWARD_PLAYER -> {
                    val seekTo = data!!.getLong(BUNDLE_VOD_PLAYER_SEEK_TO)
                    val newPosition = mPlayer.currentPosition.plus(seekTo)
                    if(!mPlayer.isPlaying) mPlayer.play()
                    mPlayer.seekTo(newPosition)
                    InformationBus.submitEvent(Event(Events.VOD_FORWARD_STATE))
                }

                ACTION_VOD_PLAYER_STOP_TIMER -> {
                    stopTimerHandler()
                    InformationBus.submitEvent(Event(Events.VOD_STOP_TIMER))
                }

                ACTION_VOD_PLAYER_START_TIMER -> {
                    startTimerHandler()
                    InformationBus.submitEvent(Event(Events.VOD_START_TIMER))
                }

                ACTION_VOD_PLAYER_STOP_PLAYER -> {
                    mPlayer.stop()
                    InformationBus.submitEvent(Event(Events.VOD_STOP_STATE))
                }

                ACTION_VOD_PLAYER_RELEASE_PLAYER -> {
                    mPlayer.release()
                    InformationBus.submitEvent(Event(Events.VOD_RELEASE_STATE))
                }

            }
            super.onAppPrivateCommand(action, data)
        }

        /**
         * Prepares and plays the media.
         *
         * @param url The URL of the media.
         * @param resumeFromSec The position to resume playback from.
         * @param isPlaying A boolean indicating whether to start playing immediately.
         * @param licenceUrl The URL for license if DRM protected.
         */
        private fun prepareAndPlay(url: String?, resumeFromSec: Int, isPlaying: Boolean, licenceUrl: String) {
//            Log.d(Constants.LogTag.CLTV_TAG + TAG, "prepareAndPlay: ")
            val dnt = if (context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(KEY_ADS_TARGETING, true)) 0 else 1
            val country = context!!.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE)
                .getString(ScanHelper.PREFS_KEY_CURRENT_COUNTRY_ALPHA3, "USA").toString()
            val auid =
                context!!.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
                    .getString(
                        ScanHelper.PREFS_KEY_AUID, ""
                    ).toString()
            var playbackUrl = url
            if (!playbackUrl.isNullOrEmpty()) {
                playbackJob?.cancel()
                playbackJob = CoroutineScope(Dispatchers.IO).launch {
                    playbackUrl = playbackUrl!!.replace("{{IP}}", IpAddressHelper.getIpAddress())
                        .replace("{{DNT}}", dnt.toString())
                        .replace("{{APP_BUNDLE_ID}}", context!!.packageName)
                        .replace("{{COUNTRY}}", country)
                        .replace("{{DID}}", AdvertisingIdHelper.getAdvertisingId(context!!))
                        .replace("{{PLAYER_WIDTH}}", playerWidth.toString())
                        .replace("{{PLAYER_HEIGHT}}", playerHeight.toString())
                        .replace("{{AUID}}", auid)
                }
            }
            mPlayer.stop()
            mPlayer.clearMediaItems()
//            Log.d(Constants.LogTag.CLTV_TAG + TAG, "prepareAndPlay: playbackUrl : $playbackUrl")
//            Log.d(Constants.LogTag.CLTV_TAG + TAG, "prepareAndPlay: resumeFromSec : $resumeFromSec")
            val mi: MediaItem
            if (licenceUrl.isEmpty()) {
                mi = MediaItem.Builder().setUri(playbackUrl).build()
            } else {
                mi = MediaItem.Builder()
                    .setUri(playbackUrl).setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setMultiSession(true)
                            .setLicenseUri(licenceUrl.replace("{{AUID}}", auid).replace("{{KEY}}", BuildConfig.X_API_KEY))
                            .build()
                    ).build()
            }
            mPlayer.setMediaItem(mi);
            mPlayer.prepare()

            if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.VOD_TRAILER) {
                mPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }else{
                mPlayer.repeatMode = Player.REPEAT_MODE_OFF
            }

            mPlayer.seekTo(resumeFromSec.times(1000L))
            mPlayer.addListener(this)
            isPlayingLive = false
            if(isPlaying) {
                mPlayer.play()
            }else{
                mPlayer.pause()
            }
        }

        var handler: Handler? = null
        val delay = 1000L

        /**
         * Starts the timer handler.
         */
        private fun startTimerHandler() {
            stopTimerHandler()
            if(handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            repeatDelayed()
        }

        /**
         * Stops the timer handler.
         */
        private fun stopTimerHandler(){
            if (handler != null) {
                handler?.removeCallbacks(runnable)
                handler = null
            }
        }

        /**
         * Repeats the delayed action.
         */
        private fun repeatDelayed() {
            handler?.let {
                it.removeCallbacks (runnable)
                it.postDelayed(runnable,1000)
            }
        }

        /**
         * The runnable task to execute repeatedly.
         */
        private val runnable = Runnable {
            InformationBus.submitEvent(Event(Events.VOD_CURRENT_POSITION, mPlayer.currentPosition))
            repeatDelayed()
        }
    }
}