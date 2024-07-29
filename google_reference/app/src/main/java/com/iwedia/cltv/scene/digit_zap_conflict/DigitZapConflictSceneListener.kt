package com.iwedia.cltv.scene.digit_zap_conflict

import com.iwedia.cltv.platform.model.TvChannel
import world.SceneListener

/**
 * Digit zap conflict scene listener
 *
 * @author Dejan Nadj
 */
interface DigitZapConflictSceneListener : SceneListener {

    fun onChannelClicked(tvChannel: TvChannel)

    fun getChannelsInConflict(): MutableList<TvChannel>
}