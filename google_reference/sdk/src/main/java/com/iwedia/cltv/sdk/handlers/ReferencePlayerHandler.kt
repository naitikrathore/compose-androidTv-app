package com.iwedia.cltv.sdk.handlers

import TvConfigurationHelper
import android.animation.*
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Intent
import android.media.PlaybackParams
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.media.tv.TvInputManager.TIME_SHIFT_STATUS_AVAILABLE
import android.media.tv.TvTrackInfo
import android.media.tv.TvTrackInfo.TYPE_SUBTITLE
import android.media.tv.TvView
import android.media.tv.TvView.*
import android.net.Uri
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.util.Property
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.sdk.BuildConfig
import com.iwedia.cltv.sdk.DisplayMode
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.ReferenceSdk.context
import com.iwedia.cltv.sdk.entities.*
import com.iwedia.cltv.sdk.media.MediaSessionControl
import core_entities.*
import data_type.GLong
import handlers.PlayerHandler
import kotlinx.coroutines.Job
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import utils.information_bus.events.HideLoadingEvent
import utils.information_bus.events.ShowLoadingEvent
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.math.abs
import kotlin.reflect.KCallable


/**
 * Reference player player
 *
 * @author Dejan Nadj
 */
class ReferencePlayerHandler : PlayerHandler<
        PlayableItem,
        PlaybackSurface<View>?,
        PlaybackSurfaceContainer<ViewGroup>?,
        AudioTrack, SubtitleTrack>() {

    private val TAG = "ReferencePlayerHandler"

    val UPDATE_EPG_GUIDE_EVENT = "com.google.android.tv.dtvepg.UPDATE_EPG_GUIDE"

    private var liveTvView: TvView? = null
    private var pvrPlaybackTvView: TvView? = null
    private var mServiceRef: KCallable<*>? = null
    //private var livePlaybackView: RelativeLayout? = null

    /**
     * Show time shift indication mutable live data
     */
    var showTimeShiftIndication: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    /**
     * List of audio tracks that are currently active.
     */
    private var mAudioTracks: MutableList<ReferenceAudioTrack> =
        mutableListOf<ReferenceAudioTrack>()

    /**
     * List of subtitle tracks that are currently active.
     */
    private var mSubtitleTracks: MutableList<ReferenceSubtitleTrack> =
        mutableListOf<ReferenceSubtitleTrack>()

    /**
     * Index of the currently active audio track.
     */
    private var mCurrentAudioTrack: ReferenceAudioTrack? = null

    /**
     * Index of the currently active subtitle track.
     */
    private var mCurrentSubtitleTrack: ReferenceSubtitleTrack? = null

    /**
     * Player timeout
     */
    private val PLAYER_TIMEOUT: Long = 10000

    /**
     * Pre loading time out
     */
    private val LOADING_PRE_TIME: Long = 3000

    /**
     * Time to wait for on content blocked callback
     */
    private val OVERLAY_TIME: Long = 3000

    /**
     * Player timeout timer
     */
    var timeOutTimer: Timer? = null

    /**
     * Overlay timer
     */
    var overlayTimer: CountDownTimer? = null

    /**
     * Player pre loading timer
     */
    var preLoadingTimer: Timer? = null

    /**
     * Flag if we need to show overlay for locked channels
     */
    var isZapTimeExtended = false

    /**
     * Flag if correct pin is entered
     */
    var isChannelUnlocked = false

    /**
     * Flag if correct pin is entered
     */
    var isOnLockScreen = false


    /**
     * Is time shift available
     */
    var isTimeShiftAvailable = false

    /**
     * Enum player status
     */
    object PlayerStatus {
        const val STATUS_PLAY = 0
        const val STATUS_STOP = 1
        const val STATUS_PAUSE = 2
        const val STATUS_IDLE = 3
    }

    /**
     * Player status
     */
    var playerStatus: Int = PlayerStatus.STATUS_IDLE
    var playableItem: PlayableItem? = null

    var mediaSessionPlayerStateListener: MediaSessionControl.MediaSessionPlayerStateListener? = null

    /**
     * Pvr playback callback
     */
    var pvrPlaybackCallback: PvrPlaybackCallback? = null

    /**
     * Pvr playback start position
     */
    var pvrPlaybackStartPosition = 0L

    /**
     * Video unavailable reason value
     */
    private var onVideoUnavailableReason = -1

    /**
     * Scramble value
     */
    var mWasScramble = false

    /**
     * Is parental blocking active
     */
    private var isParentalActive = false


    /**
     *  Following variable are used for Aspect ratio (DisplayMode selection)
     *
     * mVideoWidth and height is collected in onTrackSelected from track
     */
    var mVideoWidth: Int = 0
    var mVideoHeight: Int = 0

    private var mVideoFrameRate = 0f
    private val DISPLAY_ASPECT_RATIO_EPSILON = 0.01f
    private var mVideoDisplayAspectRatio: Float = 0.0f
    private var mDisplayMode = 0
    private var lastSubtitleTrack: ReferenceSubtitleTrack? = null

    /**
     * PREF_DISPLAY_MODE used for pref. storage key
     * */
    companion object{
        const val PREF_DISPLAY_MODE: String = "display_mode"
    }

    /**
     * mWindowWidth and height is display(monitor) params
     * */
    private var mWindowWidth: Int? = null
    private var mWindowHeight: Int? = null

    private var mAppliedDisplayedMode: Int = DisplayMode.MODE_NOT_DEFINED
    private var mAppliedTvViewStartMargin = 0
    private var mAppliedTvViewEndMargin = 0
    private var mAppliedVideoDisplayAspectRatio = 0f
    private val mTvViewStartMargin = 0
    private val mTvViewEndMargin = 0

    /**
     * AspectRatioHelper class has few helper methods for AspectRatio calculations
     * */
    val aspectRation = AspectRatioHelper()

    private var mVideoFormat: Int = aspectRation.VIDEO_DEFINITION_LEVEL_UNKNOWN
    var selectedMode = ReferenceSdk.prefsHandler!!.getValue(PREF_DISPLAY_MODE, 1) as Int
    var value: Int? = null

    /**
     * UI elements for Display Mode
     **/
    private var mTvViewAnimator: ObjectAnimator? = null
    private var mTvViewLayoutParams: RelativeLayout.LayoutParams? = null
    private var mTvViewFrame: RelativeLayout.LayoutParams? = null
    private var mLastAnimatedTvViewFrame: RelativeLayout.LayoutParams? = null
    private var mOldTvViewFrame: RelativeLayout.LayoutParams? = null


    private val mLinearOutSlowIn: TimeInterpolator = AnimationUtils.loadInterpolator(
        context, android.R.interpolator.linear_out_slow_in
    );
    private val mFastOutLinearIn: TimeInterpolator = AnimationUtils.loadInterpolator(
        context, android.R.interpolator.fast_out_linear_in
    );

    /**
     * Blocked rating
     */
    private var blockedRating: TvContentRating? = null

    /**
     * Pvr playback callback
     */
    interface PvrPlaybackCallback {
        fun onPlaybackPositionChanged(position: Long)
    }

    /**
     * Set time shift indication flag
     */
    fun setTimeShiftIndication(show: Boolean) {
        showTimeShiftIndication.value = show
    }

    private var isSubtitleChecked = false
    private var lastSelectedSubtitleTrackId: String? = null

    /**
     * TV view callback
     */
    private val tvViewCallback: TvView.TvInputCallback = object : TvView.TvInputCallback() {
        override fun onConnectionFailed(inputId: String) {
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "onConnectionFailed $inputId"
            )
            InformationBus.submitEvent(Event(ReferenceEvents.NO_PLAYBACK))
            playerStatus = PlayerStatus.STATUS_IDLE
        }

        override fun onDisconnected(inputId: String) {
            Log.d(Constants.LogTag.CLTV_TAG +
                "Reference Player",
                "onDisconnected $inputId"
            )
            InformationBus.submitEvent(Event(ReferenceEvents.NO_PLAYBACK))
            playerStatus = PlayerStatus.STATUS_IDLE
        }

        override fun onChannelRetuned(inputId: String, channelUri: Uri) {
            Log.d(Constants.LogTag.CLTV_TAG +
                "Reference Player",
                "onChannelRetuned $inputId"
            )
            InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_STARTED))
            InformationBus.submitEvent(HideLoadingEvent())
            cancelTimers()
            playerStatus = PlayerStatus.STATUS_PLAY
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onTracksChanged(inputId: String, tracks: List<TvTrackInfo>) {

            Log.i(
                TAG,
                "onTracksChanged $inputId"
            )

            var activeAudioTrackId = liveTvView!!.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
            var activeSubtitleTrackId = liveTvView!!.getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE)

            mAudioTracks.clear()
            mSubtitleTracks.clear()

            for (track in tracks) {
                if (track.type == TvTrackInfo.TYPE_AUDIO) {
                    mAudioTracks.add(ReferenceAudioTrack(track))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTracksChanged:AdDesc ${track.isAudioDescription}")
                    Log.d(Constants.LogTag.CLTV_TAG +
                        "Reference Player",
                        "New audio track: id=" + track.id + ", lang=" + track.language
                    )
                    if (activeAudioTrackId != null) {
                        if (track.id == activeAudioTrackId) {
                            try {
                                mCurrentAudioTrack = ReferenceAudioTrack(track)
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "onTracksChanged - activeAudio = " + mCurrentAudioTrack!!.trackInfo.language
                                )
                            } catch (nfe: NumberFormatException) {
                                // not a valid int
                            }
                        }
                    }
                } else if (track.type == TvTrackInfo.TYPE_SUBTITLE) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTracksChanged: ${track.encoding.toString()}")
                    if (track.encoding != "teletext-full-page")
                        mSubtitleTracks.add(ReferenceSubtitleTrack(track))
                    Log.i(
                        TAG,
                        "New subtitle track: id=" + track.id + ", lang=" + track.language
                    )
                    if (activeSubtitleTrackId != null)
                        if (track.id == activeSubtitleTrackId) {
                            try {
                                mCurrentSubtitleTrack = ReferenceSubtitleTrack(track)
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    " Reference Player Handler onTracksChanged - activeSubtitle =  " + mCurrentSubtitleTrack!!.trackInfo.language
                                )
                            } catch (nfe: NumberFormatException) {
                                // not a valid int
                            }

                        }
                }
            }

            if (mAudioTracks.isNotEmpty()) {
                InformationBus.submitAsyncEvent(
                    Event(
                        ReferenceEvents.AUDIO_TRACKS_UPDATED,
                        playableItem!!.playableObject,
                        mAudioTracks
                    )
                )
            }
            if (mSubtitleTracks.isNotEmpty()) {
                InformationBus.submitAsyncEvent(
                    Event(
                        ReferenceEvents.SUBTITLE_TRACKS_UPDATED,
                        playableItem!!.playableObject,
                        mSubtitleTracks
                    )
                )
            }
        }

        @SuppressLint("LongLogTag")
        override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
            Log.i(
                TAG,
                "onTrackSelected: mCurrentAudioTrackIndex =${
                    liveTvView!!.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
                }"
            )
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "onTrackSelected $inputId $type $trackId"
            )
            InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_STARTED))
            InformationBus.submitEvent(HideLoadingEvent())
            cancelTimers()
            playerStatus = PlayerStatus.STATUS_PLAY
            if (mediaSessionPlayerStateListener != null) {
                mediaSessionPlayerStateListener!!.onPlaybackStart()
            }

            if (type == TvTrackInfo.TYPE_AUDIO) {
                synchronized(mAudioTracks) {

                    if (trackId == null) {
                        Log.d(Constants.LogTag.CLTV_TAG + "onTrackSelected", "trackId == null")
                        mCurrentAudioTrack = null
                    } else {
                        for (track in mAudioTracks) {
                            if (track.trackInfo!!.id == trackId) {
                                mCurrentAudioTrack = track
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "onTrackSelected: activeAudio = " + mCurrentAudioTrack!!.trackInfo.language
                                )

                            }
                        }
                    }
                }
            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                synchronized(mSubtitleTracks) {
                    if (trackId == null || trackId.isEmpty()) {
                        mCurrentSubtitleTrack = null
                    } else {
                        for (track in mSubtitleTracks) {
                            if (track.trackInfo!!.id == trackId) {
                                mCurrentSubtitleTrack = track
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "onTrackSelected: activeSubtitle = " + mCurrentSubtitleTrack!!.trackInfo.language
                                )
                            }
                        }
                    }
                }
            }
            if (type == TvTrackInfo.TYPE_VIDEO) {

                try {
                    if (trackId == null) {
                        mVideoWidth = 0;
                        mVideoHeight = 0;
                        mVideoFormat = aspectRation.VIDEO_DEFINITION_LEVEL_UNKNOWN;
                        mVideoFrameRate = 0f;
                        mVideoDisplayAspectRatio = 0f;
                    } else {
                        var tracks: List<TvTrackInfo> = liveTvView!!.getTracks(type)
                        var trackFound: Boolean = false

                        if (tracks != null) {
                            for (track in tracks) {
                                if (track.id.equals(trackId)) {
                                    mVideoWidth = track.videoWidth
                                    mVideoHeight = track.videoHeight
                                    mVideoFormat =
                                        aspectRation.getVideoDefinitionLevelFromSize(
                                            mVideoWidth,
                                            mVideoHeight
                                        )
                                    mVideoFrameRate = track.videoFrameRate
                                    if (mVideoWidth <= 0 || mVideoHeight <= 0) {
                                        mVideoDisplayAspectRatio = 0.0f
                                    } else {
                                        var videoPixelAspectRatio: Float =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                track.videoPixelAspectRatio
                                            } else {
                                                0.0f
                                            }
                                        mVideoDisplayAspectRatio =
                                            mVideoWidth.toFloat() / mVideoHeight
                                        mVideoDisplayAspectRatio *= if (videoPixelAspectRatio > 0) videoPixelAspectRatio else 1.toFloat()
                                    }
                                    trackFound = true
                                    break
                                }
                            }
                            if (!trackFound) {
                                Log.w(TAG, "Invalid track ID: " + trackId);
                            }
                        }

                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

                setDisplayModeOnInit()

            }

            if (mCurrentAudioTrack != null) {
                Log.i(
                    TAG,
                    "active audio track ${mCurrentAudioTrack!!.trackInfo.id}"
                )
            } else {
                Log.i(
                    TAG,
                    "active audio track NULL"
                )
            }

            if (mCurrentSubtitleTrack != null) {
                Log.i(
                    TAG,
                    "active subtitle track ${mCurrentSubtitleTrack!!.trackInfo.id}"
                )
            } else {
                Log.i(
                    TAG,
                    "active subtitle track NULL"
                )
            }
        }

        override fun onVideoSizeChanged(inputId: String, width: Int, height: Int) {
            Log.d(Constants.LogTag.CLTV_TAG +
                "Reference Player",
                "onVideoSizeChanged $inputId"
            )
        }

        override fun onVideoAvailable(inputId: String) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onVideoAvailable: ######################### onVideoAvailable $inputId")
            mWasScramble = false
            //startRunningStatusCheck()
            //Trigger playback started to hide all overlays
            if(isParentalActive)
                InformationBus.submitEvent(Event(ReferenceEvents.PARENTAL_PIN_SHOW))
            else
                InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_INIT))
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "onVideoAvailable $inputId"
            )

            val updateEpgIntent =
                Intent(UPDATE_EPG_GUIDE_EVENT)
            updateEpgIntent.setPackage("com.iwedia.cltv")

            // Broadcast epg intent
            context.sendBroadcast(
                updateEpgIntent
            )

            //TODO HACK for Iwedia input (onVideoAvailable/onVideoUnavailable bug)
            if (onVideoUnavailableReason == 1) {
                onVideoUnavailableReason = -1
                return
            }

            InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_STARTED))
            InformationBus.submitEvent(HideLoadingEvent())
            cancelTimers()
            playerStatus = PlayerStatus.STATUS_PLAY
            if (mediaSessionPlayerStateListener != null) {
                mediaSessionPlayerStateListener!!.onPlaybackStart()
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onVideoUnavailable(inputId: String, reason: Int) {
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "onVideoUnavailable $inputId $reason"
            )
            if (playableItem != null && playableItem!!.itemType == PlayableItemType.TV_CHANNEL) {

                if (pvrPlaybackTvView != null) {
                    pvrPlaybackTvView!!.reset()
                }

                val playableObject: ReferenceTvChannel =
                    playableItem!!.playableObject as ReferenceTvChannel
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onVideoUnavailable: ######## PLAYBACK STATUS HELPER IN PLAYER HANDLER ${playableObject.name}")
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onVideoUnavailable: ######## PLAYBACK STATUS HELPER IN PLAYER HANDLER onVideoUnavailable $reason")
            if (BuildConfig.FLAVOR.contains("mtk")) {
                if (reason == 0) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onVideoUnavailable: ######################### onVideoUnavailable ReferenceEvents.SCRAMBLED_CHANNEL")
                    mWasScramble = true
                    InformationBus.submitEvent(Event(ReferenceEvents.SCRAMBLED_CHANNEL))
                }
            } else {
                if (reason == 256 || reason == 18) {
                    mWasScramble = true
                    InformationBus.submitEvent(Event(ReferenceEvents.SCRAMBLED_CHANNEL))
                }
            }

            if (reason == 1) {
                InformationBus.submitEvent(Event(ReferenceEvents.WAITING_FOR_CHANNEL))
            } else if (reason == 2) {
                mWasScramble = isActiveChannelScrambled()
                InformationBus.submitEvent(Event(ReferenceEvents.NO_PLAYBACK))
            } else {
                InformationBus.submitEvent(Event(ReferenceEvents.WAITING_FOR_CHANNEL))
            }
            playerStatus = PlayerStatus.STATUS_IDLE
            onVideoUnavailableReason = reason

            startVideoAvailabilityCheck()
        }

        var lastEvent : ReferenceTvEvent? = null
        override fun onContentAllowed(inputId: String) {
            InformationBus.submitEvent(Event(ReferenceEvents.PARENTAL_PIN_HIDE))
            lastEvent = null

            isParentalActive = false
            Log.d(Constants.LogTag.CLTV_TAG +
                "Reference Player",
                "onContentAllowed $inputId wasScrambled = $mWasScramble"
            )
            if (!mWasScramble) {
                InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_STARTED))
                InformationBus.submitEvent(HideLoadingEvent())
                cancelTimers()
                playerStatus = PlayerStatus.STATUS_PLAY
                if (mediaSessionPlayerStateListener != null) {
                    mediaSessionPlayerStateListener!!.onPlaybackStart()
                }
            } else {
                InformationBus.submitEvent(Event(ReferenceEvents.SCRAMBLED_CHANNEL))
            }

        }

        override fun onContentBlocked(inputId: String, rating: TvContentRating) {
            blockedRating = rating
            isParentalActive = true
            Log.d(Constants.LogTag.CLTV_TAG +
                "Reference Player",
                "onContentBlocked $inputId"
            )

            val cv = ConditionVariable()
            var isPinShowSubmitted = false

            ReferenceSdk.epgHandler!!.getCurrentEvent((activePlayableItem!!.playableObject as ReferenceTvChannel), object : AsyncDataReceiver<ReferenceTvEvent>{
                override fun onFailed(error: Error?) {
                    InformationBus.submitEvent(Event(ReferenceEvents.PARENTAL_PIN_SHOW))
                    isPinShowSubmitted = true
                    cv.open()
                }

                override fun onReceive(data: ReferenceTvEvent) {
                    if((lastEvent == null )|| (lastEvent!!.id != data.id)){
                        if (!isChannelUnlocked) {
                            InformationBus.submitEvent(Event(ReferenceEvents.PARENTAL_PIN_SHOW))
                        } else {
                            InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_HIDE_BLACK_OVERLAY))
                        }
                    }
                    lastEvent = data
                    isPinShowSubmitted = true
                    cv.open()
                }
            })

            cv.block(500)
            if (!isPinShowSubmitted) {
                InformationBus.submitEvent(Event(ReferenceEvents.PARENTAL_PIN_SHOW))
            }
        }


        override fun onTimeShiftStatusChanged(inputId: String, status: Int) {
            if (inputId.contains("mediatek")) {
                isTimeShiftAvailable = true
            } else {
                isTimeShiftAvailable = status == TIME_SHIFT_STATUS_AVAILABLE
            }
            Log.d(Constants.LogTag.CLTV_TAG +
                "Reference Player",
                "onTimeShiftStatusChanged $inputId $status"
            )
        }
    }



    /**
     * TV view callback
     */
    private val tvViewCallbackPvr: TvView.TvInputCallback = object : TvView.TvInputCallback() {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onTracksChanged(inputId: String, tracks: List<TvTrackInfo>) {

            Log.i(
                "Reference Player",
                "onTracksChanged $inputId"
            )

            var activeAudioTrackId = pvrPlaybackTvView!!.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
            var activeSubtitleTrackId = pvrPlaybackTvView!!.getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE)

            mAudioTracks.clear()
            mSubtitleTracks.clear()

            for (track in tracks) {
                if (track.type == TvTrackInfo.TYPE_AUDIO) {
                    mAudioTracks.add(
                        ReferenceAudioTrack(track)
                    )
                    Log.d(Constants.LogTag.CLTV_TAG +
                        "Reference Player",
                        "New audio track: id=" + track.id + ", lang=" + track.language
                    )
                    if (activeAudioTrackId != null) {
                        if (track.id == activeAudioTrackId) {
                            try {
                                mCurrentAudioTrack = ReferenceAudioTrack(track)
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "onTracksChanged - activeAudio = " + mCurrentAudioTrack!!.trackInfo.language
                                )
                            } catch (nfe: NumberFormatException) {
                                // not a valid int
                            }
                        }
                    }
                } else if (track.type == TvTrackInfo.TYPE_SUBTITLE) {
                    mSubtitleTracks.add(ReferenceSubtitleTrack(track))
                    if (activeSubtitleTrackId != null)
                        if (track.id == activeSubtitleTrackId) {
                            try {
                                mCurrentSubtitleTrack = ReferenceSubtitleTrack(track)
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "onTracksChanged - activeSubtitle =  " + mCurrentSubtitleTrack!!.trackInfo.language
                                )
                            } catch (nfe: NumberFormatException) {
                                // not a valid int
                            }

                        }
                }
            }

            if (mAudioTracks.isNotEmpty()) {
                InformationBus.submitAsyncEvent(
                    Event(
                        ReferenceEvents.AUDIO_TRACKS_UPDATED,
                        playableItem!!.playableObject,
                        mAudioTracks
                    )
                )
            }
            if (mSubtitleTracks.isNotEmpty()) {
                InformationBus.submitAsyncEvent(
                    Event(
                        ReferenceEvents.SUBTITLE_TRACKS_UPDATED,
                        playableItem!!.playableObject,
                        mSubtitleTracks
                    )
                )
            }
        }

        @SuppressLint("LongLogTag")
        override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
            Log.i(
                TAG,
                "onTrackSelected: mCurrentAudioTrackIndex =${
                    pvrPlaybackTvView!!.getSelectedTrack(TvTrackInfo.TYPE_AUDIO)
                }"
            )
            Log.d(Constants.LogTag.CLTV_TAG +
                "Reference Player",
                "onTrackSelected $inputId $type $trackId"
            )

            if (type == TvTrackInfo.TYPE_AUDIO) {
                synchronized(mAudioTracks) {

                    if (trackId == null) {
                        Log.d(Constants.LogTag.CLTV_TAG + "onTrackSelected", "trackId == null")
                        mCurrentAudioTrack = null
                    } else {
                        for (track in mAudioTracks) {
                            if (track.trackInfo!!.id == trackId) {
                                mCurrentAudioTrack = track
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "onTrackSelected: activeAudio = " + mCurrentAudioTrack!!.trackInfo.language
                                )

                            }
                        }
                    }
                }
            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                synchronized(mSubtitleTracks) {
                    if (trackId == null || trackId.isEmpty()) {
                        mCurrentSubtitleTrack = null
                    } else {
                        for (track in mSubtitleTracks) {
                            if (track.trackInfo!!.id == trackId) {
                                mCurrentSubtitleTrack = track
                                Log.d(Constants.LogTag.CLTV_TAG +
                                    TAG,
                                    "onTrackSelected: activeSubtitle = " + mCurrentSubtitleTrack!!.trackInfo.language
                                )
                            }
                        }
                    }
                }
            }
            if (type == TvTrackInfo.TYPE_VIDEO) {

                try {
                    if (trackId == null) {
                        mVideoWidth = 0;
                        mVideoHeight = 0;
                        mVideoFormat = aspectRation.VIDEO_DEFINITION_LEVEL_UNKNOWN;
                        mVideoFrameRate = 0f;
                        mVideoDisplayAspectRatio = 0f;
                    } else {
                        var tracks: List<TvTrackInfo> = pvrPlaybackTvView!!.getTracks(type)
                        var trackFound: Boolean = false

                        if (tracks != null) {
                            for (track in tracks) {
                                if (track.id.equals(trackId)) {
                                    mVideoWidth = track.videoWidth
                                    mVideoHeight = track.videoHeight
                                    mVideoFormat =
                                        aspectRation.getVideoDefinitionLevelFromSize(
                                            mVideoWidth,
                                            mVideoHeight
                                        )
                                    mVideoFrameRate = track.videoFrameRate
                                    if (mVideoWidth <= 0 || mVideoHeight <= 0) {
                                        mVideoDisplayAspectRatio = 0.0f
                                    } else {
                                        var videoPixelAspectRatio: Float =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                track.videoPixelAspectRatio
                                            } else {
                                                0.0f
                                            }
                                        mVideoDisplayAspectRatio =
                                            mVideoWidth.toFloat() / mVideoHeight
                                        mVideoDisplayAspectRatio *= if (videoPixelAspectRatio > 0) videoPixelAspectRatio else 1.toFloat()
                                    }
                                    trackFound = true
                                    break
                                }
                            }
                            if (!trackFound) {
                                Log.w(TAG, "Invalid track ID: " + trackId);
                            }
                        }

                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

                setDisplayModeOnInit()

            }

            if (mCurrentAudioTrack != null) {
                Log.i(
                    TAG,
                    "active audio track ${mCurrentAudioTrack!!.trackInfo.id}"
                )
            } else {
                Log.i(
                    TAG,
                    "active audio track NULL"
                )
            }

            if (mCurrentSubtitleTrack != null) {
                Log.i(
                    TAG,
                    "active subtitle track ${mCurrentSubtitleTrack!!.trackInfo.id}"
                )
            } else {
                Log.i(
                    TAG,
                    "active subtitle track NULL"
                )
            }
        }
    }

    /**
     * Check video availability task
     * Needed to recover scrambled channel info after antenna plug/unplug switching
     */
    private var videoAvailabilityJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startVideoAvailabilityCheck() {
        videoAvailabilityJob?.cancel()
        videoAvailabilityJob = CoroutineHelper.runCoroutineWithDelay({
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "startVideoAvailabilityCheck: ########## STATUS onVideoUnavailable RESTART")
            if (mWasScramble) {
                // Retry playback after 5sec if onVideoUnavailableReason is not tuning or waiting
                if (onVideoUnavailableReason != -1 && onVideoUnavailableReason != 1 && onVideoUnavailableReason != 3) {
                    if (!ReferenceSdk.sdkListener!!.isSettingsOpened()) {
                        play(playableItem!!, object : AsyncReceiver {
                            override fun onSuccess() {}
                            override fun onFailed(error: Error?) {}
                        })
                    }
                }
            }
        }, 5000)
    }

    /**
     * Start channel list update timer
     */
    private fun startTimer() {
        //Cancel timer if it's already started
        stopTimer()
        if (activePlayableItem != null && activePlayableItem?.itemType == PlayableItemType.TV_CHANNEL) {
            if ((activePlayableItem!!.playableObject as ReferenceTvChannel).isLocked || mWasScramble) {
                endTimer()
                return
            }

            overlayTimer = object : CountDownTimer(OVERLAY_TIME, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                }

                override fun onFinish() {
                    endTimer()
                }
            }

            InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_SHOW_BLACK_OVERLAY))
            (liveTvView as TvView).setStreamVolume(0f)
        }
    }

    private fun endTimer() {
        stopTimer()
        if (!isMuted) {
            (liveTvView as TvView).setStreamVolume(1f)
        }
        InformationBus.submitEvent(Event(ReferenceEvents.PLAYBACK_HIDE_BLACK_OVERLAY))
    }

    /**
     * Stop channel list udpate timer if it is already started
     */
    private fun stopTimer() {
        if (overlayTimer != null) {
            overlayTimer!!.cancel()
            overlayTimer = null
        }
    }

    fun isActiveChannelScrambled(): Boolean {
        if (BuildConfig.FLAVOR.contains("mtk")) {
            return onVideoUnavailableReason == 0
        } else {
            return onVideoUnavailableReason == 18 || onVideoUnavailableReason == 256
        }
    }

    fun resetVideoUnavailable() {
        onVideoUnavailableReason = -1
    }

    fun updateSubtitleCheckStatus(status: Boolean) {
        isSubtitleChecked = status
    }

    fun setCurrentSubtitleTrack(track: ReferenceSubtitleTrack, callback: AsyncReceiver) {
        if (!isSubtitleChecked) {
            isSubtitleChecked = true
        }
        Log.i(
            TAG,
            "Select subtitle track with id " + track.trackInfo!!.id
        )

        synchronized(mSubtitleTracks) {

            mCurrentSubtitleTrack = track

            if (liveTvView != null && liveTvView!!.visibility==View.VISIBLE) {
                if (liveTvView!!.getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE) == track.trackInfo!!.id) {
                    isSubtitleChecked = false
                }

                if (isSubtitleChecked) {
                    (liveTvView as TvView).setCaptionEnabled(true)
                    lastSubtitleTrack = track
                    (liveTvView as TvView).selectTrack(
                        TvTrackInfo.TYPE_SUBTITLE,
                        track.trackInfo!!.id
                    )
                } else {
                    (liveTvView as TvView).setCaptionEnabled(false)
                    (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
                }

                callback.onSuccess()
            } else {
                callback.onFailed(Error(404, "Track not found."))
            }

            if (pvrPlaybackTvView != null && liveTvView!!.visibility == GONE)  {

                if (pvrPlaybackTvView!!.getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE) == track.trackInfo!!.id) {
                    isSubtitleChecked = false
                }

                if (isSubtitleChecked) {
                    (pvrPlaybackTvView as TvView).setCaptionEnabled(true)
                    lastSubtitleTrack = track
                    (pvrPlaybackTvView as TvView).selectTrack(
                        TvTrackInfo.TYPE_SUBTITLE,
                        track.trackInfo!!.id
                    )
                } else {
                    (pvrPlaybackTvView as TvView).setCaptionEnabled(false)
                    (pvrPlaybackTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
                }

                callback.onSuccess()
            } else {
                //callback.onFailed(Error(404, "Track not found."))
            }
        }
    }

    @Deprecated("You should use setCurrentSubtitleTrack method")
    override fun setActiveSubtitleTrack(track: SubtitleTrack, callback: AsyncReceiver) {
        callback.onFailed(Error(404, "Not implemented"))
    }

    fun updateSubtitleLanguage(language: String) {
        if (TvConfigurationHelper.getSubtitlesEnabled()) {
            var found = false
            mSubtitleTracks.forEach {
                if (it.trackInfo!!.language == language) {
                    found = true
                    (liveTvView as TvView).setCaptionEnabled(true)
                    (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, it.trackInfo!!.id)
                }
            }
            if (!found) {
                (liveTvView as TvView).setCaptionEnabled(false)
                (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
            }
        } else {
            Toast.makeText(context, "Subtitles are OFF!!!", Toast.LENGTH_SHORT).show()
        }
    }



    fun getCurrentSubtitleTrack(callback: AsyncDataReceiver<ReferenceSubtitleTrack>) {
        var retVal: ReferenceSubtitleTrack? = null

        synchronized(mSubtitleTracks) {
            if (mCurrentSubtitleTrack != null) {
                try {
                    for (track in mSubtitleTracks) {

                        val tvView :TvView =if( liveTvView!!.visibility == VISIBLE ) liveTvView!! else pvrPlaybackTvView!!

                        if (track.trackInfo!!.id != null && TextUtils.isDigitsOnly(track.trackInfo!!.id) && tvView!!.getSelectedTrack(
                                TvTrackInfo.TYPE_SUBTITLE
                            ) != null
                        ) {
                            if (Integer.parseInt(track.trackInfo!!.id) == Integer.parseInt(
                                    tvView!!.getSelectedTrack(
                                        TvTrackInfo.TYPE_SUBTITLE
                                    )
                                )
                            ) {
                                try {
                                    retVal = track
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "getActiveSubtitleTrack: track.id = " + track!!.trackInfo.id
                                    )
                                } catch (nfe: NumberFormatException) {
                                    // not a valid int
                                }
                            }
                        }

                    }
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                        "Ref+",
                        "getActiveSubtitleTrack: Invalid index ${mCurrentSubtitleTrack!!.trackInfo!!.id}"
                    )
                } catch (e: NullPointerException) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                        "Ref+",
                        "getActiveSubtitleTrack: Invalid index ${mCurrentSubtitleTrack!!.trackInfo!!.id}"
                    )
                }
            }
        }

        if (retVal != null) {
            Log.i(
                "Ref+",
                "Active subtitle track: " + retVal!!.trackInfo!!.id
            )
            callback.onReceive(retVal!!)
        } else {
            Log.i(
                "Ref+",
                "Active subtitle track: -1"
            )
            callback.onFailed(Error(404, "Active subtitle track: -1"))
        }
    }


    @Deprecated("Use getCurrentSubtitleTrack method")
    override fun getActiveSubtitleTrack(callback: AsyncDataReceiver<SubtitleTrack>) {
        callback.onFailed(Error(404, "Not implemented"))
    }

    fun getAvailableSubtitleTracks(): MutableList<ReferenceSubtitleTrack> {
        return mSubtitleTracks
    }


    override fun setPlaybackView(vararg views: PlaybackSurface<View>?) {
        if(isSubtitleChecked) {
            (liveTvView as TvView).setCaptionEnabled(true)
        }
        if (views[0]!!.type == PlaybackSurface.PlaybackType.CHANNEL) {
            liveTvView = views[0]!!.view as TvView
            //(liveTvView as TvView).setCallback(tvViewCallback)
        }
        if (views[1]!!.type == PlaybackSurface.PlaybackType.PVR) {
            pvrPlaybackTvView = views[1]!!.view as TvView
            (pvrPlaybackTvView as TvView).setCallback(tvViewCallbackPvr)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun play(playableItem: PlayableItem, callback: AsyncReceiver) {

        setDisplayModeOnInit()

        this.playableItem = playableItem

        if (playableItem.itemType == PlayableItemType.TV_CHANNEL) {

            if (pvrPlaybackTvView != null) {
                pvrPlaybackTvView!!.reset()
            }
            ReferenceSdk.recentlyHandler?.addRecentlyWatched(playableItem)

            val playableObject: ReferenceTvChannel =
                playableItem.playableObject as ReferenceTvChannel
            val channelUri = TvContract.buildChannelUri(playableObject.id.toLong())

            if (liveTvView == null) {
                callback.onFailed(Error(404, "Live surface view is null"))
                InformationBus.submitEvent(Event(ReferenceEvents.NO_PLAYBACK))
                return
            }

            liveTvView!!.visibility = View.VISIBLE

            cancelTimers()

            if (preLoadingTimer == null) {
                preLoadingTimer = Timer()
            }

            preLoadingTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    if (playerStatus != PlayerStatus.STATUS_PLAY) {
                        InformationBus.submitEvent(ShowLoadingEvent())
                    }
                    preLoadingTimer = null
                }
            }, LOADING_PRE_TIME)

            if (timeOutTimer == null) {
                timeOutTimer = Timer()
            }

            timeOutTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    if (playerStatus != PlayerStatus.STATUS_PLAY) {
                        playerStatus = PlayerStatus.STATUS_IDLE
                        InformationBus.submitEvent(HideLoadingEvent())
                        InformationBus.submitEvent(Event(ReferenceEvents.PLAYER_TIMEOUT))
                        callback.onFailed(Error(100, "TvInput timeout"))
                        timeOutTimer = null
                    }
                }
            }, PLAYER_TIMEOUT)

            liveTvView!!.visibility = View.VISIBLE
            ReferenceSdk.sdkListener!!.runOnUiThread {
                liveTvView!!.tune(playableObject.inputId, channelUri)
            }

            activePlayableItem = playableItem
            // Reset time shift available flag on track changed
            isTimeShiftAvailable = false

            //todo
            if (isZapTimeExtended) {
                startTimer()
            }
            isChannelUnlocked = false
            callback.onSuccess()
        } else if (playableItem.itemType == PlayableItemType.TV_PVR) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "play: ######### PLAY RECORDING")
            val recording: ReferenceRecording = playableItem.playableObject as ReferenceRecording
            var inputId: String = recording.tvChannel.inputId
            val recordingUri = TvContract.buildRecordedProgramUri(recording.id.toLong())
            if (liveTvView != null) {
                liveTvView!!.visibility = View.GONE
                liveTvView!!.reset()
            }
            timeShiftPosition = 0
            pvrPlaybackStartPosition = 0
            var initialPlaybackPosition = 0L

            pvrPlaybackTvView!!.setTimeShiftPositionCallback(@RequiresApi(Build.VERSION_CODES.M)
            object : TimeShiftPositionCallback() {
                override fun onTimeShiftStartPositionChanged(inputId: String, timeMs: Long) {
                    super.onTimeShiftStartPositionChanged(inputId, timeMs)
                    if (timeMs < 0) return
                    if (!isRichTvInput(inputId)) {
                        if (pvrPlaybackStartPosition == 0L) {
                            pvrPlaybackStartPosition = timeMs
                            callback.onSuccess()
                        }
                    }
                }

                override fun onTimeShiftCurrentPositionChanged(inputId: String, timeMs: Long) {
                    super.onTimeShiftCurrentPositionChanged(inputId, timeMs)
                    if (timeMs < 0) return
                    if (initialPlaybackPosition == 0L) {
                        initialPlaybackPosition = timeMs
                    }
                    if (isRichTvInput(inputId) && pvrPlaybackStartPosition == 0L && timeMs > 0) {
                        if (timeMs - initialPlaybackPosition > 1500) {
                            pvrPlaybackStartPosition = timeMs
                            callback.onSuccess()
                        } else {
                            initialPlaybackPosition = timeMs
                        }
                    }
                    if (isRichTvInput(inputId)) {
                        pvrPlaybackCallback?.onPlaybackPositionChanged(timeMs - pvrPlaybackStartPosition - 1500)
                    } else {
                        pvrPlaybackCallback?.onPlaybackPositionChanged(timeMs - pvrPlaybackStartPosition)
                    }

                }
            })

            val recordedProgramUri =
                if (BuildConfig.FLAVOR.contains("mtk")) ContentUris.withAppendedId(
                    recordingUri,
                    0.toLong()
                )
                else recordingUri

            pvrPlaybackTvView!!.postDelayed(Runnable {
                pvrPlaybackTvView!!.visibility = View.VISIBLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    pvrPlaybackTvView!!.timeShiftPlay(inputId, recordedProgramUri)
                }
            }, 1000)

            activePlayableItem = playableItem
        }
    }

    fun requestUnblockContent(callback: AsyncReceiver) {
        CoroutineHelper.runCoroutine({
            if (mServiceRef == null) {
                mServiceRef = TvView::class.members.single {
                    it.name == "requestUnblockContent"
                }
            }
            val test = liveTvView as TvView
            try {
                mServiceRef!!.call(test, blockedRating)
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onFailed(null)
                return@runCoroutine
            } catch (invocationTargetException: InvocationTargetException) {
                invocationTargetException.cause?.printStackTrace()
                callback.onFailed(null)
                return@runCoroutine
            }
            callback.onSuccess()
        })
    }

    private fun isRichTvInput(inputId: String?): Boolean {
        return inputId == "com.example.android.sampletvinput/.rich.RichTvInputService"
    }

    override fun setup() {
        activePlayableItem = null
        CoroutineHelper.runCoroutine({
            mServiceRef = TvView::class.members.single {
                it.name == "requestUnblockContent"
            }
        })
    }

    fun setCurrentAudioTrack(track: ReferenceAudioTrack, callback: AsyncReceiver) {
        synchronized(mAudioTracks) {

            try {
                mCurrentAudioTrack = track
            } catch (e: IndexOutOfBoundsException) {
                Log.i(
                    "Ref+",
                    "Invalid index!"
                )
            }
            if (liveTvView != null  && liveTvView!!.visibility == VISIBLE) {
                (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_AUDIO, track.trackInfo!!.id)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setActiveAudioTrack: ")
                callback.onSuccess()
            } else {
                callback.onFailed(Error(404, "Error changing audio track."))
            }
            if (pvrPlaybackTvView != null && liveTvView!!.visibility == GONE) {
                (pvrPlaybackTvView as TvView).selectTrack(TvTrackInfo.TYPE_AUDIO, track.trackInfo!!.id)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setActiveAudioTrack: ")
                callback.onSuccess()
            } else {
                //callback.onFailed(Error(404, "Error changing audio track."))
            }
        }
    }

    @Deprecated("setCurrentAudioTrack")
    override fun setActiveAudioTrack(track: AudioTrack, callback: AsyncReceiver) {
        callback.onFailed(Error(404, "Not implemented"))
    }

    fun getCurrentAudioTrack(): ReferenceAudioTrack {
        return mCurrentAudioTrack!!
    }

    fun getCurrentAudioTrack(callback: AsyncDataReceiver<ReferenceAudioTrack>) {
        var retVal: ReferenceAudioTrack? = null
        synchronized(mAudioTracks) {
            if (mCurrentAudioTrack != null) {
                try {
                    for (track in mAudioTracks) {

                        val tvView :TvView =if( liveTvView!!.visibility == VISIBLE ) liveTvView!! else pvrPlaybackTvView!!

                        if (track.trackInfo!!.id != null && TextUtils.isDigitsOnly(track.trackInfo!!.id) && tvView!!.getSelectedTrack(
                                TvTrackInfo.TYPE_AUDIO
                            ) != null
                        ) {
                            if (Integer.parseInt(track.trackInfo!!.id) == Integer.parseInt(
                                    tvView!!.getSelectedTrack(
                                        TvTrackInfo.TYPE_AUDIO
                                    )
                                )
                            ){
                                try {
                                    retVal = track
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "getActiveAudioTrack: track.id = " + track.trackInfo!!.id
                                    )
                                } catch (nfe: NumberFormatException) {
                                    // not a valid int
                                }
                            }
                        }
                    }
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                        "Ref+",
                        "getActiveAudioTrack: Invalid index ${mCurrentAudioTrack!!.trackInfo!!.id}"
                    )
                } catch (e: NullPointerException) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                        "Ref+",
                        "getActiveAudioTrack: Invalid index ${mCurrentAudioTrack!!.trackInfo!!.id}"
                    )
                } catch (e: NumberFormatException) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                        "Ref+",
                        "getActiveAudioTrack: NumberFormatException ${mCurrentAudioTrack!!.trackInfo!!.id}"
                    )
                }
            }

            if (retVal != null) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "track.Id=  " + retVal!!.trackInfo!!.id + " language= " + retVal!!.trackInfo!!.language
                )
                callback.onReceive(retVal!!)
            } else {
                callback.onFailed(Error(404, "Track not found"))
            }
        }
    }

    @Deprecated("Use getCurrentAudioTrack")
    override fun getActiveAudioTrack(callback: AsyncDataReceiver<AudioTrack>) {
        callback.onFailed(Error(404, "Not implemented"))
    }

    fun getAvailableAudioTracks(): MutableList<ReferenceAudioTrack> {
        return mAudioTracks
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun resume(callback: AsyncReceiver) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "resume: ############ PLAYER RESUME")
        if (activePlayableItem?.itemType == PlayableItemType.TV_CHANNEL) {
            ReferenceSdk.sdkListener!!.runOnUiThread {
                if (activePlayableItem != null) {
                    play(activePlayableItem!!, object : AsyncReceiver {
                        override fun onFailed(error: Error?) {
                            callback.onFailed(error)
                        }

                        override fun onSuccess() {
                            callback.onSuccess()
                        }
                    })
                } else {
                    callback.onFailed(Error(404, "Nothing to resume."))
                }
            }
        } else if (activePlayableItem?.itemType == PlayableItemType.TV_PVR) {
            pvrPlaybackTvView!!.timeShiftResume()
            callback.onSuccess()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun pause(callback: AsyncReceiver) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "pause: ############ PLAYER PAUSE")
        if (activePlayableItem?.itemType == PlayableItemType.TV_CHANNEL) {
            ReferenceSdk.sdkListener!!.runOnUiThread {
                if (activePlayableItem != null) {

                    if (activePlayableItem != null) {
                        (liveTvView as TvView).reset()
                    }
                    callback.onSuccess()
                } else {
                    callback.onFailed(Error(404, "Nothing to pause."))
                }
            }
        } else if (activePlayableItem?.itemType == PlayableItemType.TV_PVR) {
            pvrPlaybackTvView!!.timeShiftPause()
            callback.onSuccess()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun stop(callback: AsyncReceiver) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "stop: ############ PLAYER STOP")
        if (activePlayableItem?.itemType == PlayableItemType.TV_CHANNEL) {
            ReferenceSdk.sdkListener!!.runOnUiThread {
                if (activePlayableItem != null) {
                    (liveTvView as TvView).reset()
                }
                playerStatus = PlayerStatus.STATUS_STOP
                callback.onSuccess()
            }
        } else if (activePlayableItem?.itemType == PlayableItemType.TV_PVR) {
            pvrPlaybackCallback = null
            pvrPlaybackTvView!!.setTimeShiftPositionCallback(null)
            pvrPlaybackStartPosition = 0
            var playableItem = PlayableItem(
                PlayableItemType.TV_CHANNEL,
                (ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel!!
            )
            (pvrPlaybackTvView as TvView).reset()
            play(playableItem, callback)
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun seek(positionMs: GLong, isRelative: Boolean, callback: AsyncReceiver) {
        if (activePlayableItem?.itemType == PlayableItemType.TV_PVR) {
            pvrPlaybackTvView!!.timeShiftSeekTo(pvrPlaybackStartPosition + positionMs.value.toLong())
            callback.onSuccess()
        } else {
            liveTvView!!.timeShiftSeekTo(pvrPlaybackStartPosition + positionMs.value.toLong())
            callback.onSuccess()
            if (mediaSessionPlayerStateListener != null) {
                mediaSessionPlayerStateListener!!.onSeekCompleted()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun reset(callback: AsyncReceiver) {
        (liveTvView as TvView).reset()

        pvrPlaybackCallback = null
        liveTvView!!.setTimeShiftPositionCallback(null)
        pvrPlaybackTvView!!.setTimeShiftPositionCallback(null)
        pvrPlaybackStartPosition = 0
        callback.onSuccess()
    }

    override fun getState(callback: AsyncDataReceiver<Int>) {
        callback.onReceive(playerStatus)
    }

    override fun dispose() {
        super.dispose()
        activePlayableItem = null

        if (mediaSessionPlayerStateListener != null) {
            mediaSessionPlayerStateListener!!.onPlaybackStop()
            mediaSessionPlayerStateListener!!.onRelease()
        }

        mediaSessionPlayerStateListener = null
        cancelTimers()
    }

    /**
     * Cancel timers
     */
    protected fun cancelTimers() {
        if (timeOutTimer != null) {
            timeOutTimer?.cancel()
            timeOutTimer = null
        }

        if (preLoadingTimer != null) {
            preLoadingTimer?.cancel()
            preLoadingTimer = null
        }
    }

    private var isMuted = false
    fun mute() {
        isMuted = true
        (liveTvView as TvView).setStreamVolume(0f)
    }

    fun unmute() {
        isMuted = false
        (liveTvView as TvView).setStreamVolume(1f)
    }

    fun isParentalBlockingActive(): Boolean {
        return isParentalActive
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getIsAudioDescription(type: Int): Boolean {
        liveTvView?.let {
            var tracks = it.getTracks(type)
            if (tracks != null)
                if (tracks.size != 0)
                    for (track in tracks) {
                        try {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getIsAudioDescription: ${track.isAudioDescription}")
                            if (track!!.isAudioDescription) {
                                return true
                            }
                        } catch (e: java.lang.Exception) {
                        }
                    }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getIsDolby(type: Int): Boolean {
        if (liveTvView != null) {
            var tracks = liveTvView!!.getTracks(type)
            if (tracks != null)
                if (tracks.size != 0)
                    for (track in tracks) {
                        try {
                            if (TvConfigurationHelper.isDolby(track)) {
                                Log.d(Constants.LogTag.CLTV_TAG + "TAG", "TRASCK IS DOLBY")
                                return true
                            }
                        } catch (e: java.lang.Exception) {
                            continue
                        }
                    }
        }

        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getIsCC(type: Int): Boolean {
        //TODO CHECK IMPLEMENTATION
        liveTvView?.let {
            var tracks = it.getTracks(type)
            if (tracks != null)
                for (track in tracks) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        return track!!.isHardOfHearing
                    } else {
                        return false
                    }

                }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getDolbyType(type: Int, trackId: String): String {
        val tracks = liveTvView!!.getTracks(type)
        if (tracks != null) {
            for (track in tracks) {
                if (BuildConfig.FLAVOR.contains("mtk")) {
                    when (track.extra.get("key_AudioEncodeType") as String) {
                        "1" -> return "ac3"
                        "12" -> return "eac3"
                        "26" -> return "dts"
                    }
                    return ""
                }
                if (track.id.equals(trackId)) {
                    //Match track by ID in order to distinguish if track is dolby or not
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Track: ${track!!.description} ${track!!.encoding} ${track!!.id} ${track!!.language}")
                    return if (track.encoding != null)
                        track.encoding!!
                    else ""
                }
            }
        }
        return ""
    }

    /**
     * Is time shift active flag
     */
    var isTimeShiftActive = false

    /**
     * Time shift start position
     */
    var timeShiftStartPosition: Long = -1

    /**
     * Time shift position
     */
    var timeShiftPosition = 0L

    /**
     * Is time shift paused flag
     */
    var isTimeShiftPaused = false

    /**
     * Time shift position callback
     */
    var timeShiftPositionCallback: ReferenceTimeShiftPositionCallback? = null

    /**
     * Time shift pause/resume
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun timeShiftPause() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftPause: ######## TIMESHIFT PUASE IN PLAYERHANDLER $isTimeShiftPaused")

        if (isTimeShiftPaused) {

            liveTvView!!.timeShiftResume()
            isTimeShiftPaused = false

            if (mediaSessionPlayerStateListener != null) {
                mediaSessionPlayerStateListener!!.onPlaybackStart()
            }

        } else {
            liveTvView!!.timeShiftPause()

            if (timeShiftPositionCallback == null) {
                timeShiftPositionCallback = ReferenceTimeShiftPositionCallback()
                liveTvView!!.setTimeShiftPositionCallback(timeShiftPositionCallback)
            }
            isTimeShiftPaused = true


            if (mediaSessionPlayerStateListener != null) {
                mediaSessionPlayerStateListener!!.onPlaybackPause()
            }
        }
        isTimeShiftActive = true
    }

    /**
     * Time shift in background
     * This timer is used to calculate time when the time shift is running in the background (app is paused)
     */
    var timeShiftInBackgroundTimer: Timer? = null
    var timeShiftInBackgroundTimerTask: TimerTask? = null
    var timeShiftInBackgroundDuration = 0L
    private fun startTimeShiftInBackgroundTimer() {
        if (timeShiftInBackgroundTimer == null) {
            timeShiftInBackgroundTimer = Timer()
        }
        timeShiftInBackgroundDuration = 0L
        timeShiftInBackgroundTimerTask = object : TimerTask() {
            override fun run() {
                timeShiftInBackgroundDuration += 1000
            }
        }
        timeShiftInBackgroundTimer?.scheduleAtFixedRate(timeShiftInBackgroundTimerTask, 1000, 1000)
    }

    private fun stopTimeShiftInBackgroundTimer() {
        if (timeShiftInBackgroundTimer != null) {
            timeShiftInBackgroundTimer?.cancel()
            timeShiftInBackgroundTimer?.purge()
            timeShiftInBackgroundTimerTask?.cancel()
            timeShiftInBackgroundTimer = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun pauseTimeShift() {
        if (isTimeShiftActive) {
            if (!isTimeShiftPaused) {
                startTimeShiftInBackgroundTimer()
            }
            liveTvView!!.timeShiftPause()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun resumeTimeShift() {
        stopTimeShiftInBackgroundTimer()
        if (isTimeShiftActive) {
            liveTvView!!.timeShiftResume()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                liveTvView!!.timeShiftSeekTo(timeShiftPosition + timeShiftInBackgroundDuration)
                InformationBus.submitEvent(Event(ReferenceEvents.SHOW_PLAYER_BANNER_EVENT))
                timeShiftInBackgroundDuration = 0L
                if (isTimeShiftPaused) {
                    liveTvView!!.timeShiftPause()
                } else {
                    liveTvView!!.timeShiftResume()
                }
            }, 1000)
        }
    }

    /**
     * Time shift stop
     * Stop time shift and resume live playback
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun timeShiftStop() {
        isTimeShiftPaused = false
        timeShiftPositionCallback = null
        timeShiftPosition = 0
        timeShiftStartPosition = -1L
        isTimeShiftActive = false
        liveTvView!!.setTimeShiftPositionCallback(null)

        if (mediaSessionPlayerStateListener != null) {
            mediaSessionPlayerStateListener!!.onPlaybackStop()
        }
        if (activePlayableItem != null) {
            play(activePlayableItem!!, object : AsyncReceiver {
                override fun onFailed(error: Error?) {
                }

                override fun onSuccess() {
                }
            })

            if (mediaSessionPlayerStateListener != null) {
                mediaSessionPlayerStateListener!!.onPlaybackStart()
            }
        }
    }

    /**
     * Time shift seek forward for a specified time in ms
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun timeShiftSeekForward(timeMs: Long) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftSeekForward: ########### TIMESHIFT SEEK FORWARD $timeMs")
        liveTvView!!.timeShiftSeekTo(timeShiftPosition + timeMs)

        if (mediaSessionPlayerStateListener != null) {
            mediaSessionPlayerStateListener!!.onSeekCompleted()
        }
    }

    /**
     * Time shift seek backward for a specified time in ms
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun timeShiftSeekBackward(timeMs: Long) {
        liveTvView!!.timeShiftSeekTo(timeShiftPosition - timeMs)

        if (mediaSessionPlayerStateListener != null) {
            mediaSessionPlayerStateListener!!.onSeekCompleted()
        }
    }

    /**
     * Time shift seek to a specified time in ms
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun timeShiftSeekTo(timeMs: Long) {
        liveTvView!!.timeShiftSeekTo(timeShiftStartPosition + timeMs)

        if (mediaSessionPlayerStateListener != null) {
            mediaSessionPlayerStateListener!!.onSeekCompleted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun setTimeShiftSpeed(speed: Int) {
        if (isTimeShiftActive) {
            val playbackParams = PlaybackParams()
            playbackParams.speed = speed.toFloat()
            liveTvView!!.timeShiftSetPlaybackParams(playbackParams)
        }else {
            val playbackParams = PlaybackParams()
            playbackParams.speed = speed.toFloat()
            pvrPlaybackTvView!!!!.timeShiftSetPlaybackParams(playbackParams)
        }
    }

    /**
     * Reference time shift position callback
     */
    @RequiresApi(Build.VERSION_CODES.M)
    inner class ReferenceTimeShiftPositionCallback : TvView.TimeShiftPositionCallback() {
        //Duration in seconds time shift position - start position
        private var duration: Long = 0

        override fun onTimeShiftCurrentPositionChanged(inputId: String?, timeMs: Long) {
            if (timeShiftStartPosition == -1L) {
                timeShiftStartPosition = timeMs
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Init Time shift start position ${Date(timeShiftStartPosition)}")
            }
            timeShiftPosition = timeMs
            duration = (timeShiftPosition - timeShiftStartPosition) / 1000
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Time shift current position ${Date(timeMs)}")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Time shift duration ${duration}")
            InformationBus.submitEvent(Event(ReferenceEvents.TIME_SHIFT_POSITION_EVENT, duration))
        }

        override fun onTimeShiftStartPositionChanged(inputId: String?, timeMs: Long) {
            super.onTimeShiftStartPositionChanged(inputId, timeMs)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Time shift start position ${Date(timeMs)}")
        }
    }

    fun isDisplayModeAvailable(displayMode: Int): Boolean {
        return aspectRation.isDisplayModeAvailable(
            displayMode,
            getVideoDisplayAspectRatio(),
            liveTvView!!
        )
    }

    fun getVideoDisplayAspectRatio(): Float {
        return mVideoDisplayAspectRatio
    }

    fun getDisplayMode(): Int {
        return (ReferenceSdk.prefsHandler!!.getValue(PREF_DISPLAY_MODE, 1) as Int)
    }

    fun getScaleType(): Int {
        return aspectRation.checkScaleType
    }
//
//    fun isSubtitleChecked(): Boolean {
//        return isSubtitleChecked
//    }

    fun setSubtitles(view: Boolean) {
        if (!view) {
            if(liveTvView != null && liveTvView!!.visibility == VISIBLE){
                lastSelectedSubtitleTrackId = (liveTvView as TvView).getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE)
                (liveTvView as TvView).setCaptionEnabled(false)
                (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
            }
            if(pvrPlaybackTvView != null && liveTvView!!.visibility == GONE){
                lastSelectedSubtitleTrackId = (pvrPlaybackTvView as TvView).getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE)
                (pvrPlaybackTvView as TvView).setCaptionEnabled(false)
                (pvrPlaybackTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
            }

        } else {
            if(liveTvView != null&& liveTvView!!.visibility == VISIBLE){
                liveTvView?.setCaptionEnabled(true)
                (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, lastSelectedSubtitleTrackId)
            }
            if(pvrPlaybackTvView != null && liveTvView!!.visibility == GONE){
                (pvrPlaybackTvView as TvView).setCaptionEnabled(true)
                (pvrPlaybackTvView as TvView).selectTrack(
                    TvTrackInfo.TYPE_SUBTITLE,
                    lastSelectedSubtitleTrackId
                )
            }

        }
    }

    fun setDisplayMode(displayMode: Int, animate: Boolean, storeInPreference: Boolean): Int {
        val prev = mDisplayMode
        mDisplayMode = displayMode
        if (storeInPreference) {
            ReferenceSdk.prefsHandler!!.storeValue(PREF_DISPLAY_MODE, displayMode)
        }
        val size = aspectRation.getDisplaySize()
        mWindowWidth = size.x
        mWindowHeight = size.y
        mTvViewFrame = aspectRation.createMarginLayoutParams(
            0, 0, 0, 0,
            mWindowWidth!!, mWindowHeight!!
        )
        applyDisplayMode(getVideoDisplayAspectRatio(), animate, false)
        return prev
    }

    private fun applyDisplayMode(
        videoDisplayAspectRatio: Float,
        animate: Boolean,
        forceUpdate: Boolean
    ) {

        var videoDisplayAspectRatio = videoDisplayAspectRatio
        if (videoDisplayAspectRatio <= 0f) {
            videoDisplayAspectRatio = mWindowWidth!!.toFloat() / mWindowHeight!!
        }
        if (mAppliedDisplayedMode === mDisplayMode && mAppliedTvViewStartMargin === mTvViewStartMargin
            && mAppliedTvViewEndMargin === mTvViewEndMargin && (abs(
                mAppliedVideoDisplayAspectRatio - videoDisplayAspectRatio
            ) < DISPLAY_ASPECT_RATIO_EPSILON)
        ) {
            if (!forceUpdate) {

                return
            }
        } else {
            mAppliedDisplayedMode = mDisplayMode
            mAppliedTvViewStartMargin = mTvViewStartMargin
            mAppliedTvViewEndMargin = mTvViewEndMargin
            mAppliedVideoDisplayAspectRatio = videoDisplayAspectRatio
        }
        val availableAreaWidth: Int = mWindowWidth!! - mTvViewStartMargin - mTvViewEndMargin
        val availableAreaHeight: Int = availableAreaWidth * mWindowHeight!! / mWindowWidth!!
        var displayMode = mDisplayMode
        var availableAreaRatio = 0f
        if (availableAreaWidth <= 0 || availableAreaHeight <= 0) {
            displayMode = DisplayMode.MODE_FULL
        } else {
            availableAreaRatio = availableAreaWidth.toFloat() / availableAreaHeight
        }
        val layoutParams: RelativeLayout.LayoutParams = aspectRation.getLayoutParamsForDisplayMode(
            displayMode,
            videoDisplayAspectRatio,
            availableAreaRatio,
            availableAreaWidth,
            availableAreaHeight
        )
        val tvViewFrameTop = (mWindowHeight!! - availableAreaHeight) / 2
        val tvViewFrame: RelativeLayout.LayoutParams = aspectRation.createMarginLayoutParams(
            mTvViewStartMargin,
            mTvViewEndMargin,
            tvViewFrameTop,
            tvViewFrameTop,
            mWindowWidth!!,
            mWindowHeight!!
        )
        setTvViewPosition(
            layoutParams,
            tvViewFrame!!,
            animate
        )
    }

    fun setSubtitle(view: Boolean, data: ReferenceSubtitleTrack) {
        if (!view) {
            if(liveTvView != null && liveTvView!!.visibility == VISIBLE){
                lastSelectedSubtitleTrackId = (liveTvView as TvView).getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE)
                (liveTvView as TvView).setCaptionEnabled(false)
                (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
            }

            if(pvrPlaybackTvView != null && liveTvView!!.visibility == GONE){
                lastSelectedSubtitleTrackId = (pvrPlaybackTvView as TvView).getSelectedTrack(TvTrackInfo.TYPE_SUBTITLE)
                (pvrPlaybackTvView as TvView).setCaptionEnabled(false)
                (pvrPlaybackTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, null)
            }

        } else {
            if(liveTvView != null&& liveTvView!!.visibility == VISIBLE){
                liveTvView?.setCaptionEnabled(true)
                (liveTvView as TvView).selectTrack(TvTrackInfo.TYPE_SUBTITLE, data?.trackInfo?.id)
            }
            if(pvrPlaybackTvView != null && liveTvView!!.visibility == GONE){
                (pvrPlaybackTvView as TvView).setCaptionEnabled(true)
                (pvrPlaybackTvView as TvView).selectTrack(
                    TvTrackInfo.TYPE_SUBTITLE,
                    lastSelectedSubtitleTrackId
                )
            }

        }
    }

    private fun setTvViewPosition(
        layoutParams: RelativeLayout.LayoutParams,
        tvViewFrame: RelativeLayout.LayoutParams,
        animate: Boolean
    ) {
        val oldTvViewFrame: RelativeLayout.LayoutParams = mTvViewFrame!!
        mTvViewLayoutParams = layoutParams
        mTvViewFrame = tvViewFrame

        if (animate) {
            initTvAnimatorIfNeeded()
            if (mTvViewAnimator!!.isStarted) {
                mTvViewAnimator!!.cancel()
                mOldTvViewFrame = RelativeLayout.LayoutParams(mLastAnimatedTvViewFrame)
            } else {
                mOldTvViewFrame = RelativeLayout.LayoutParams(oldTvViewFrame)
            }
            mTvViewAnimator!!.setObjectValues(liveTvView!!.layoutParams, layoutParams)
            mTvViewAnimator!!.setEvaluator(
                object : TypeEvaluator<RelativeLayout.LayoutParams?> {
                    var lp: RelativeLayout.LayoutParams? = null
                    override fun evaluate(
                        p0: Float,
                        p1: RelativeLayout.LayoutParams?,
                        p2: RelativeLayout.LayoutParams?
                    ): RelativeLayout.LayoutParams? {
                        if (lp == null) {
                            lp = RelativeLayout.LayoutParams(0, 0)
                        }
                        aspectRation.interpolateMargins(lp!!, p1!!, p2!!, p0)
                        return lp
                    }
                })
            mTvViewAnimator!!.interpolator =
                if (isTvViewFullScreen()) mFastOutLinearIn else mLinearOutSlowIn
            mTvViewAnimator!!.start()
        } else {
            if (mTvViewAnimator != null && mTvViewAnimator!!.isStarted) {
                return
            }
            setLayoutParamsOnTvView(layoutParams)
        }
    }

    private fun isTvViewFullScreen(): Boolean {
        return mTvViewStartMargin == 0 && mTvViewEndMargin == 0
    }

    private fun setLayoutParamsOnTvView(layoutParams: RelativeLayout.LayoutParams) {
        InformationBus.submitEvent(Event(ReferenceEvents.VIDEO_ASPECT_RATION, layoutParams))
    }

    private fun initTvAnimatorIfNeeded() {
        if (mTvViewAnimator != null) {
            return
        }
        mTvViewAnimator = ObjectAnimator()
        mTvViewAnimator!!.target = liveTvView!!
        mTvViewAnimator!!.setProperty(
            Property.of(
                RelativeLayout::class.java,
                ViewGroup.LayoutParams::class.java,
                "layoutParams"
            )
        )
        mTvViewAnimator!!.duration = 250
        mTvViewAnimator!!.addListener(
            object : AnimatorListenerAdapter() {
                private var mCanceled = false

                override fun onAnimationCancel(animation: Animator) {
                    mCanceled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (mCanceled) {
                        mCanceled = false
                        return
                    }
                    liveTvView!!.post {
                        setTvViewPosition(
                            mTvViewLayoutParams!!,
                            mTvViewFrame!!,
                            false
                        )
                    }
                }
            })
        mTvViewAnimator!!.addUpdateListener { animator ->
            val fraction: Float = animator.animatedFraction
            mLastAnimatedTvViewFrame = liveTvView!!.layoutParams as RelativeLayout.LayoutParams
            aspectRation.interpolateMargins(
                mLastAnimatedTvViewFrame!!, mOldTvViewFrame!!, mTvViewFrame!!, fraction
            )
            liveTvView!!.layoutParams = mLastAnimatedTvViewFrame
        }
    }

    private fun setDisplayModeOnInit() {
        selectedMode = ReferenceSdk.prefsHandler!!.getValue(PREF_DISPLAY_MODE, 1) as Int

        val isAvailable = (ReferenceSdk.playerHandler as ReferencePlayerHandler).isDisplayModeAvailable(selectedMode)

        if (isAvailable) {
            //We are setting store param 'false' as we will not save display mode until it is set from Pref. menu
            setDisplayMode(
                selectedMode,
                false,
                false
            )
        } else {
            //If video does not support any display mode changes we need to keep it 1(FULL) as default.
            setDisplayMode(
                1,
                false,
                false
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getTeleText(type: Int): Boolean {
        val tracks = liveTvView?.getTracks(type)
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


}
