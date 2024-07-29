package com.iwedia.cltv.anoki_fast.vod.player

import world.SceneListener

/**
 * Listener interface for VOD (Video On Demand) banner scene events.
 */
interface VodBannerSceneListener : SceneListener {

    /**
     * Called when the back button is clicked.
     */
    fun onBackClicked()

    /**
     * Called when the play button is clicked.
     */
    fun onPlayClicked()

    /**
     * Called when the pause button is clicked.
     */
    fun onPauseClicked()

    /**
     * Called when a seek operation is performed.
     *
     * @param progress The progress to seek to, as a fraction of the total duration.
     */
    fun onSeek(progress: Float)
}