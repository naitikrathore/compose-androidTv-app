package com.iwedia.cltv.sdk.media

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.handlers.ReferencePlayerHandler

import com.iwedia.cltv.sdk.media.MediaSessionControl.MediaSessionCallback
import com.iwedia.ui.neon.media.IPlayerStateListener


/**
 * @class MediaSessionControl
 * @description Interface component for communication with Media Session part of Google Assistant
 * In order to properly execute playback related voice commands (e.g PLAY, PAUSE, FAST FORWARD...)
 * The component has two interfaces:
 * - [MediaSessionCallback] - interface for sending commands from Media Session to player.
 * - [SamplePlayerStateListener] - interface for sending current state from player to Media Session
 *
 */
@SuppressLint("StaticFieldLeak")
object MediaSessionControl {
    private const val TAG = "MediaSessionControl"

    private lateinit var mMediaSession: MediaSessionCompat
    private var mPlaybackPositionSeconds = 0L
    private lateinit var mActivity: Activity

    private val SPEED_FF_2X = 2
    private val SPEED_FF_4X = 4
    private val SPEED_FF_8X = 8
    private val SPEED_FR_2X = -2
    private val SPEED_FR_4X = -4
    private val SPEED_FR_8X = -8
    val SPEED_FF_1X = 1

    val changeSpeedArr = listOf(SPEED_FR_8X, SPEED_FR_4X , SPEED_FR_2X , SPEED_FF_1X, SPEED_FF_2X , SPEED_FF_4X, SPEED_FF_8X)
    var currentSpeedValue = SPEED_FF_1X
    private var REGULAR_SPEED = SPEED_FF_1X.toFloat()

    /**
     * When interacting with media session we can define which commands we want to react to.
     */
    private val MEDIA_ACTIONS_TIMESHIFT = (
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_SEEK_TO
                    or PlaybackStateCompat.ACTION_FAST_FORWARD
                    or PlaybackStateCompat.ACTION_REWIND)

    private val MEDIA_ACTIONS_LIVE = (
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)


    /**
     * Private method that sends the current playback status to Media Session.
     * The information that need to be included as part of an update:
     * - state - current playback state as one of the values from [PlaybackStateCompat.State]
     * - position - current playback position in milli seconds
     * - speed - current playback speed as a float number. (REGULAR = 1f, FF_x2 = 2f...)
     */
    private fun setPlaybackStatusToMediaSession(
        @PlaybackStateCompat.State state: Int, speed: Float
    ) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setPlaybackStatusToMediaSession: $state")
        var isTimeshiftActive =
            (ReferenceSdk.playerHandler as ReferencePlayerHandler).isTimeShiftActive

        if (isTimeshiftActive) {
            val builder = PlaybackStateCompat.Builder()
                .setActions(MEDIA_ACTIONS_TIMESHIFT)
                .setState(
                    state, mPlaybackPositionSeconds * 1000 // Needs to be in milli seconds
                    , speed
                )
            mMediaSession.setPlaybackState(builder.build())
        } else {
            val builder = PlaybackStateCompat.Builder()
                .setActions(MEDIA_ACTIONS_LIVE)
                .setState(
                    state, mPlaybackPositionSeconds * 1000 // Needs to be in milli seconds
                    , speed
                )
            mMediaSession.setPlaybackState(builder.build())
        }
    }

    private fun injectKeyEvent(code: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "injectKeyEvent: ############ MEDIA SESSION injectKeyEvent")
        var keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, code)
        mActivity.dispatchKeyEvent(keyEvent)

        keyEvent = KeyEvent(KeyEvent.ACTION_UP, code)
        mActivity.dispatchKeyEvent(keyEvent)


    }

    fun initialize(activity: Activity, logTag: String) {


        // Create a MediaSessionCompat
        mActivity = activity

        if (this::mMediaSession.isInitialized) {
            mMediaSession.release()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "MEDIA SESSION release invoked here...")
        }


        mMediaSession = MediaSessionCompat(activity, logTag).apply {


            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                // These flags are deprecated from Android O and by default
                // every player needs to handle these actions.
                setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            }

            // Do not let MediaButtons restart the player when the app is not visible
            setMediaButtonReceiver(null)

            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(MEDIA_ACTIONS_LIVE)

            setPlaybackState(stateBuilder.build())

            // MySessionCallback has methods that handle callbacks from a media controller
            setCallback(MediaSessionCallback())
        }


        // Create a MediaControllerCompat
        MediaControllerCompat(activity, mMediaSession).also { mediaController ->
            MediaControllerCompat.setMediaController(activity, mediaController)
        }

        if (ReferenceSdk.playerHandler != null) {
            (ReferenceSdk.playerHandler as ReferencePlayerHandler).mediaSessionPlayerStateListener =
                MediaSessionPlayerStateListener()
        }
    }

    fun deinitialize() {
//    DummyPlayer.unregisterListener()
    }

    fun updatePlaybackSpeed(speed: Float){
       REGULAR_SPEED = speed
    }

    fun getPlaybackSpeed() : Int{
        return REGULAR_SPEED.toInt()
    }

    /**
     * @class MediaSessionCallback
     * @description Media Session interface to send playback commands to the application.
     * Every callback should execute appropriate command over the player instance.
     *
     */
    private class MediaSessionCallback : MediaSessionCompat.Callback() {
        private val TAG_MS = "MediaSessionControl"

        override fun onPlay() {
            super.onPlay()
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onPlay")
            // In the Google Assistant implementation in Q4 2019 along side of calling 'onPlay' callback
            // Google Assistant inject to the application KEYCODE_MEDIA_PLAY key.
            // Because of that, all the interaction with the player should be done by handing
            // the KEYCODE_MEDIA_PLAY key.
        }

        override fun onPause() {
            super.onPause()
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onPause ${mMediaSession.controller.playbackState.state}") 
            //  Google Assistant changed its logic from version 3.10.0 and introduced:
            //  - sending PAUSE event when GA popup is shown
            //  - sending PLAY key event when GA popup is dismissed
            // We need to handle onPause() event and pause the playback on key ASSIST press.
            if (mMediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                setPlaybackStatusToMediaSession(PlaybackStateCompat.STATE_PAUSED, REGULAR_SPEED)
            }
        }

        override fun onSkipToNext() {
            // Triggered with voice command 'NEXT'
            super.onSkipToNext()
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onSkipToNext")
            // The proposed behavior is that this events should be handled the same way application
            // handles KEYCODE_MEDIA_NEXT.
            // It should trigger jump forward 30 second
            injectKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
        }

        override fun onSkipToPrevious() {
            // Triggered with voice command 'PREVIOUS'
            super.onSkipToPrevious()
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onSkipToPrevious")
            // The proposed behavior is that this events should be handled the same way application
            // handles KEYCODE_MEDIA_PREVIOUS
            // This should trigger jump back 10 seconds
            injectKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onSeekTo(positionMillis: Long) {
            // positionMillis is given as an absolute value to which the player should seek to
            // It will be executed this way even if the voice command has relative value like:
            // - 'FAST FORWARD 10 SECONDS'
            // Media Session will automatically calculate absolute position to seek to
            super.onSeekTo(positionMillis)
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onSeekTo $positionMillis")
            val positionSeconds = positionMillis
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "seek to position $positionSeconds")

            //IF STATE  TIMESHIFT
            //TODO CHECK STATE
            (ReferenceSdk.playerHandler as ReferencePlayerHandler).timeShiftSeekTo(
                mPlaybackPositionSeconds * 1000 + positionSeconds
            )
        }

        override fun onFastForward() {
            // Not triggered by any query
            // Media Control command fast-forward is not yet supported  by the Google Assistant in Q4 2019
            super.onFastForward()
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onFastForward")
        }

        override fun onRewind() {
            // Not triggered by any query
            // Media Control command rewind is not yet supported  by the Google Assistant in Q4 2019
            super.onRewind()
            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onRewind")
        }

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            val bundle = mediaButtonEvent.extras

            Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onMediaButtonEvent()")
            // This logic is added to avoid toggle command being executed by MediaSession
            if (bundle != null) {
                val keyEvent = bundle!!.get("android.intent.extra.KEY_EVENT") as KeyEvent
                Log.d(Constants.LogTag.CLTV_TAG + TAG_MS, "onMediaButtonEvent " + keyEvent!!.keyCode)
                if (keyEvent!!.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    return true
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    /**
     * @class SamplePlayerStateListener
     * @description Callback to indicate the state of the player back to MediaSession
     * This callback is triggered from the player when playback state is changed,
     * independent of the cause (MediaSession interaction, RCU interaction...)
     * We need to keep MediaSession informed with any change of playback status in order to get
     * seek commands triggered with valid values.
     */
    class MediaSessionPlayerStateListener : IPlayerStateListener {
        companion object {
            private const val STATE_STOPPED = "STOPPED"
            private const val STATE_PLAYING = "PLAYING"
            private const val STATE_PAUSED = "PAUSED"
        }

        private var mCurrentStatus = STATE_STOPPED
        private var isPlaying = false

        fun onRelease() {
            // We need to reset MediaSession by invoking release.
            mMediaSession.release()
        }

        override fun onSeekCompleted() {
            // When every seek command is executed (e.g. -10 or +30 jumps)
            // We need to inform MediaSession that the playback position is changed.
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Seek completed")

            if (isPlaying) {
                mCurrentStatus = STATE_PLAYING
                setPlaybackStatusToMediaSession(PlaybackStateCompat.STATE_PLAYING, REGULAR_SPEED)
            } else {
                mCurrentStatus = STATE_PAUSED
                setPlaybackStatusToMediaSession(PlaybackStateCompat.STATE_PAUSED, REGULAR_SPEED)
            }
        }

        override fun onPlaybackStart() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Playback started")
            // Update state to media session
            isPlaying = true
            mMediaSession.isActive = true
            setPlaybackStatusToMediaSession(PlaybackStateCompat.STATE_PLAYING, REGULAR_SPEED)

            // Update UI
            mCurrentStatus = STATE_PLAYING
        }

        override fun onPlaybackPause() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Playback paused")

            // Update state to media session
            isPlaying = false
            setPlaybackStatusToMediaSession(PlaybackStateCompat.STATE_PAUSED, REGULAR_SPEED)

            // Update UI
            mCurrentStatus = STATE_PAUSED
        }

        override fun onPlaybackStop() {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Playback stopped")
            // Update state to media session
            isPlaying = false
            setPlaybackStatusToMediaSession(PlaybackStateCompat.STATE_STOPPED, REGULAR_SPEED)
            mMediaSession.isActive = false

            // Update UI
            mCurrentStatus = STATE_STOPPED
        }

        override fun onPlaybackPosition(position: Long) {

            mPlaybackPositionSeconds = position / 1000
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Playback position $mPlaybackPositionSeconds")
            // This callback is added only for debug UI update, to track the current playback position.
            //
            // We do not need to update regularly current position of the playback to Media Session.
            // Media Session internally calculates the current playback position based on the last
            // set state, position and playback speed. We should just send information if position
            // is changed due to seek command executed.

            // Update UI
        }
    }

    fun checkRequiredLimit(endPosition: Long, currPosition: Int): Long {
        var needPosition: Long = 1
        var speed = REGULAR_SPEED.toInt()
        if(speed <1){
            var reqPosition =  when (speed){
                SPEED_FF_1X -> SPEED_FF_1X + 1
                SPEED_FF_2X -> SPEED_FF_2X + 2
                SPEED_FF_4X -> SPEED_FF_4X + 8
                SPEED_FF_8X -> SPEED_FF_8X +  40
                else -> {SPEED_FF_1X}
            }
            needPosition = (currPosition + reqPosition).toLong()
        }else{
            var reqPosition =  when (speed){
                SPEED_FF_1X -> SPEED_FF_1X + 1
                SPEED_FF_2X -> SPEED_FF_2X + 2
                SPEED_FF_4X -> SPEED_FF_4X + 8
                SPEED_FF_8X -> SPEED_FF_8X +  40
                else -> {SPEED_FF_1X}
            }
            needPosition = (endPosition - currPosition) + reqPosition
        }
        return needPosition
    }

    val SPEED_FACTORS_MAPPING = mapOf(
        SPEED_FR_8X to 48,
        SPEED_FR_4X to 12 ,
        SPEED_FR_2X to 4,
        SPEED_FF_1X to 2,
        SPEED_FF_2X to 4,
        SPEED_FF_4X to 12,
        SPEED_FF_8X to 48)
}