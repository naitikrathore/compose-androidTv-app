package com.iwedia.cltv.anoki_fast.vod.player

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.reference_scene.ReferenceSceneManager
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.series.SeriesMetadata
import utils.information_bus.Event
import utils.information_bus.InformationBus

private const val TAG = "VodBannerSceneManager"

/**
 * Manager class for handling the VOD (Video On Demand) banner scene.
 *
 * @param context The context of the main activity.
 * @param worldHandler The handler for the reference world.
 * @param playerModule The interface for player operations.
 * @param tvModule The interface for TV operations.
 */
class VodBannerSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    val playerModule: PlayerInterface,
    val tvModule: TvInterface,
    networkInterface: NetworkInterface
) : ReferenceSceneManager(
    context, worldHandler, ReferenceWorldHandler.SceneId.VOD_BANNER_SCENE, networkInterface
), VodBannerSceneListener {

    private var durationPosition = 0L
    private var currentPosition = 0L
    private var vodBannerSceneData: VodBannerSceneData? = null
    private var isPlaying = true
    private var isException = true

    init {
        registerGenericEventListener(Events.VOD_DURATION_POSITION)
        registerGenericEventListener(Events.VOD_CURRENT_POSITION)
        registerGenericEventListener(Events.VOD_PLAY_BACK_ERROR)
        registerGenericEventListener(Events.VOD_PLAY_STATE)
        registerGenericEventListener(Events.VOD_PAUSE_STATE)
        registerGenericEventListener(Events.VOD_STOP_STATE)
        registerGenericEventListener(Events.VOD_ON_START)
    }

    /**
     * Handles received events and updates the scene accordingly.
     *
     * @param event The received event.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onEventReceived(event: Event?) {
        when (event?.type) {
            Events.VOD_DURATION_POSITION -> {
                durationPosition = (event.data?.get(0) as Long)
                (scene as VodBannerScene).setContentDuration(durationPosition)
            }

            Events.VOD_CURRENT_POSITION -> {
                currentPosition = (event.data?.get(0) as Long)
                (scene as VodBannerScene).setContentCurrentPosition(currentPosition)
                checkVideoCompleted()
            }

            Events.VOD_PLAY_BACK_ERROR -> {
                (scene as VodBannerScene).setState(isPlaying = false, isError = true)
                handleTimer(isPlaying = false)
                isException = true
            }

            Events.VOD_PLAY_STATE -> {
                (scene as VodBannerScene).setState(isPlaying = true, isError = false)
                managePlayPause(isPlaying = (event.data?.get(0) as Boolean))
            }

            Events.VOD_PAUSE_STATE -> {
                managePlayPause(isPlaying = (event.data?.get(0) as Boolean))
            }

            Events.VOD_STOP_STATE -> {
                handleTimer(isPlaying = false)
            }

            Events.VOD_ON_START -> {
                onPreparePlay()
            }
        }
    }

    /**
     * Manages the play/pause state of the video player.
     *
     * @param isPlaying Whether the video is currently playing.
     */
    private fun managePlayPause(isPlaying: Boolean) {
        (scene as VodBannerScene).setPlaying(isPlaying)
        handleTimer(isPlaying)
    }

    /**
     * Checks if the video has completed and handles the back click event.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkVideoCompleted() {
        if (currentPosition > 0L && currentPosition >= durationPosition) {
            onBackClicked()
        }
    }

    /**
     * Handles the timer based on the play state of the video.
     *
     * @param isPlaying Whether the video is currently playing.
     */
    private fun handleTimer(isPlaying: Boolean) {
        if (isPlaying) {
            VodPlayerHelper.startTimer()
        } else {
            VodPlayerHelper.stopTimer()
        }
    }

    /**
     * Creates the VOD banner scene.
     */
    override fun createScene() {
        scene = VodBannerScene(context!!, this)
    }

    /**
     * Prepares the video player for playback.
     */
    private fun onPreparePlay() {
        isException = false

        VodPlayerHelper.stopTimer()

        var playBackUrl: String

        vodBannerSceneData?.let {
            playBackUrl = when (it.vodItem) {
                is SeriesMetadata -> it.vodItem.episodePlaybackUrl
                else -> (it.vodItem as VODItem).vodPlaybackUrl.toString()
            }

            val resumeFrom = if (currentPosition == 0L) {
                (it.vodItem as VODItem).resumeFromSec
            } else {
                currentPosition.div(1000).toInt()
            }

            val drmLicenseUrl = (it.vodItem as VODItem).licenseServerUrl ?: ""

            VodPlayerHelper.preparePlay(playBackUrl, resumeFrom, isPlaying, drmLicenseUrl)
            playerModule.unmute()

            InformationBus.submitEvent(Event(Events.VOD_CONTENT_IS_WATCHED, true))
        }
    }

    /**
     * Handles the back click event.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBackClicked() {
        vodBannerSceneData?.let {
            VodPlayerHelper.pause()
            VodPlayerHelper.stopTimer()
            playerModule.mute()
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {}
                override fun onReceive(data: TvChannel) {
                    tvModule.changeChannel(data, object : IAsyncCallback {
                        override fun onFailed(error: Error) {}

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onSuccess() {
                            worldHandler!!.triggerActionWithInstanceId(
                                it.previousSceneId, it.previousSceneInstance, Action.SHOW
                            )
                            ReferenceApplication.worldHandler?.triggerAction(
                                ReferenceWorldHandler.SceneId.VOD_BANNER_SCENE, Action.DESTROY
                            )
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                        }
                    })
                }
            })
        }
    }

    /**
     * Called when the scene is initialized.
     */
    override fun onSceneInitialized() {
        if (data != null) {
            vodBannerSceneData = data as VodBannerSceneData
            scene!!.refresh(vodBannerSceneData?.vodItem)
            InformationBus.submitEvent(Event(Events.VOD_PLAYBACK_STARTED))
            ReferenceApplication.worldHandler!!.playbackState =
                ReferenceWorldHandler.PlaybackState.VOD
            onPreparePlay()
        }
    }

    /**
     * Handles the pause click event.
     */
    override fun onPauseClicked() {
        isPlaying = false
        VodPlayerHelper.pause()
    }

    /**
     * Handles the play click event.
     */
    override fun onPlayClicked() {
        isPlaying = true
        VodPlayerHelper.play()
    }

    /**
     * Handles the seek event.
     *
     * @param progress The progress to seek to, as a fraction of the total duration.
     */
    override fun onSeek(progress: Float) {
        VodPlayerHelper.seekTo(durationPosition.times(progress).toLong())
    }

    /**
     * Handles the back press event.
     *
     * @return true if the back press was handled, false otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBackPressed(): Boolean {
        onBackClicked()
        return true
    }

    /**
     * Handles the resume event.
     */
    override fun onResume() {
        super.onResume()
        vodBannerSceneData?.let {
            if (isException) {
                (scene as VodBannerScene).setState(isPlaying = false, isError = false)
                onPreparePlay()
            } else {
                if (isPlaying) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        VodPlayerHelper.play()
                    }, 100)
                } else {
                    VodPlayerHelper.pause()
                }
            }
        }
    }

    /**
     * Handles the pause event.
     */
    override fun onPause() {
        super.onPause()
        VodPlayerHelper.pause()
        VodPlayerHelper.stopTimer()
    }

    /**
     * Handles the destroy event.
     */
    override fun onDestroy() {
        super.onDestroy()
        VodPlayerHelper.pause()
        VodPlayerHelper.stopTimer()
        resumeLiveContent()
    }

    private fun resumeLiveContent() {
        val applicationMode =
            if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {}

            override fun onReceive(data: TvChannel) {
                tvModule.changeChannel(data, object : IAsyncCallback {
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {
                        ReferenceApplication.worldHandler!!.playbackState =
                            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                    }
                }, applicationMode = applicationMode)
            }
        }, applicationMode = applicationMode)
    }

}