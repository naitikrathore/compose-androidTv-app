package com.iwedia.cltv.anoki_fast.vod.player

import android.os.Handler
import android.os.Looper
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.platform.model.information_bus.events.Events
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import tv.anoki.ondemand.domain.model.VODItem
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 * Helper class for managing VOD trailer playback with debouncing.
 */
object VodTrailerHelper {

    private var handler: Handler? = null
    private const val DELAY = 800L
    private var vodItem: VODItem? = null
    private lateinit var debounceLast: DebounceLast
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var stopTrailerCallback: () -> Unit

    fun initialize() {
        debounceLast = DebounceLast(800L)
        coroutineScope = CoroutineScope(Dispatchers.Main)
    }
    /**
     * ChangeTrailer the VOD trailer playback process with debouncing.
     *
     * @param vodItem The VOD item containing the trailer URL and other metadata.
     * @param stopTrailerCallback The callback to invoke when the VOD trailer player is stopped.
     */
    fun changeTrailer(vodItem: VODItem?, stopTrailerCallback: () -> Unit) {
        this.vodItem = vodItem
        this.stopTrailerCallback = stopTrailerCallback
        debounceLast.debounceLast(coroutineScope) {
            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            if (this.vodItem?.trailerUrl.isNullOrEmpty()) {
                stopVodTrailerPlayer(stopTrailerCallback)
            } else {
                handler?.removeCallbacks(runnable)
                handler?.postDelayed(runnable, DELAY)
            }
        }
    }

    /**
     * Stops the VOD trailer player and resets the state.
     */
    fun stopVodTrailerPlayer(stopTrailerCallback: () -> Unit) {
        removeHandler()
        VodPlayerHelper.pause()
        ReferenceApplication.worldHandler!!.playbackState =
            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
        stopTrailerCallback.invoke()
    }

    /**
     * Removes any pending handler callbacks and nullifies the handler.
     */
    private fun removeHandler() {
        handler?.removeCallbacks(runnable)
        handler = null
    }

    private val runnable = Runnable {
        if (vodItem?.trailerUrl != null) {
            ReferenceApplication.worldHandler!!.playbackState =
                ReferenceWorldHandler.PlaybackState.VOD_TRAILER
            VodPlayerHelper.preparePlay(
                playBackUrl = vodItem?.trailerUrl!!,
                resumeFrom = 0,
                drmLicenseUrl = vodItem?.licenseServerUrl ?: "",
                isPlaying = true
            )
            removeHandler()
            InformationBus.submitEvent(Event(Events.VOD_PLAYBACK_STARTED))
        } else {
            stopVodTrailerPlayer(stopTrailerCallback)
        }
    }

    fun cancel(){
        if(::coroutineScope.isInitialized) {
            coroutineScope.cancel()
        }
    }
}