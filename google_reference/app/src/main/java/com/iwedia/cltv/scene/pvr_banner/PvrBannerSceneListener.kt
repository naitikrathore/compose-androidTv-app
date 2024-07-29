package com.iwedia.cltv.scene.pvr_banner

import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.TvChannel
import world.SceneListener

/**
 * Pvr banner scene listener
 *
 * @author Dragan Krnjaic
 */
interface PvrBannerSceneListener: SceneListener, ToastInterface {

    /**
     * Show stop recording dialog
     *
     * @param channelChange UP - channel up, DOWN - channel down, NULL - active channel
     */
    fun showStopRecordingDialog(channelChange: String?)
    fun setRecIndication(boolean: Boolean)
    fun getChannelById(id: Int): TvChannel
}