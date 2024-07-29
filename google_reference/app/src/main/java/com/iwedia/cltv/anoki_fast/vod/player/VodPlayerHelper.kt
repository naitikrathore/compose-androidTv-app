package com.iwedia.cltv.anoki_fast.vod.player

import android.media.tv.TvView
import android.net.Uri
import android.os.Bundle
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.tis.ui.SetupActivity
import tv.anoki.ondemand.constants.StringConstants

/**
 * Helper class for managing VOD (Video On Demand) player actions.
 */
object VodPlayerHelper {

    /**
     * Creates a Bundle from a map of parameters.
     *
     * @param params The map of parameters to include in the Bundle.
     * @return A Bundle containing the parameters.
     * @throws IllegalArgumentException if the type of any value in the map is unsupported.
     */
    private fun createBundle(params: Map<String, Any>): Bundle {
        val bundle = Bundle()
        for ((key, value) in params) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                // Add other types as needed
                else -> throw IllegalArgumentException("Unsupported type for Bundle: ${value::class.java}")
            }
        }
        return bundle
    }

    /**
     * Sends an empty tune command to reset the live TV view.
     */
    private fun sendEmptyTune() {
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).tune(
            SetupActivity.INPUT_ID,
            Uri.EMPTY
        )
    }

    /**
     * Prepares the VOD player for playback.
     *
     * @param playBackUrl The URL of the playback content.
     * @param resumeFrom The position from which to resume playback.
     * @param isPlaying Whether the content should start playing immediately.
     * @param drmLicenseUrl The URL of the DRM license.
     */
    fun preparePlay(
        playBackUrl: String,
        resumeFrom: Int,
        isPlaying: Boolean,
        drmLicenseUrl: String
    ) {
        sendEmptyTune()
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_PLAYBACK, createBundle(
                mapOf(
                    StringConstants.BUNDLE_VOD_PLAYER_URL to playBackUrl,
                    StringConstants.BUNDLE_VOD_PLAYER_RESUME_FROM to resumeFrom,
                    StringConstants.BUNDLE_VOD_PLAYER_IS_PLAY to isPlaying,
                    StringConstants.BUNDLE_VOD_PLAYER_LICENCE_URL to drmLicenseUrl
                )
            )
        )
    }

    /**
     * Seeks to the specified duration in the VOD player.
     *
     * @param duration The duration to seek to.
     */
    fun seekTo(duration: Long) {
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_SEEK_TO, createBundle(
                mapOf(
                    StringConstants.BUNDLE_VOD_PLAYER_SEEK_TO to duration
                )
            )
        )
    }

    /**
     * Starts playback in the VOD player.
     */
    fun play() {
        sendEmptyTune()
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_PLAY_PLAYER, createBundle(mapOf())
        )
    }

    /**
     * Pauses playback in the VOD player.
     */
    fun pause() {
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_PAUSE_PLAYER, createBundle(mapOf())
        )
    }

    /**
     * Stops playback in the VOD player.
     */
    fun stop() {
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_STOP_PLAYER, createBundle(mapOf())
        )
    }

    /**
     * Releases the resources of the VOD player.
     */
    fun release() {
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_RELEASE_PLAYER, createBundle(mapOf())
        )
    }

    /**
     * Starts the timer for the VOD player.
     */
    fun startTimer() {
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_START_TIMER, createBundle(mapOf())
        )
    }

    /**
     * Stops the timer for the VOD player.
     */
    fun stopTimer() {
        ((ReferenceApplication.getActivity() as MainActivity).liveTvView as TvView).sendAppPrivateCommand(
            StringConstants.ACTION_VOD_PLAYER_STOP_TIMER, createBundle(mapOf())
        )
    }
}