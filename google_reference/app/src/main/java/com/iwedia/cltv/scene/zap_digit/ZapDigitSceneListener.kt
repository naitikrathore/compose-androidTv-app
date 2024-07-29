package com.iwedia.cltv.scene.zap_digit

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.TvChannel
import world.SceneListener

/**
 * Zap banner scene listener
 *
 * @author Dejan Nadj
 */
interface ZapDigitSceneListener : SceneListener, ToastInterface, TTSSetterInterface {

    /**
     * On digit pressed
     * @param digit Digit
     */
    fun onDigitPressed(digit: Int)

    /**
     * Zap on digit
     * @param displayNumber displayNumber
     */
    fun zapOnDigit(itemId: Int)

    /**
     * On period pressed
     */
    fun onPeriodPressed()

    /**
     * On backspace pressed
     */
    fun onBackspacePressed()

    /**
     * On timer end zap
     * @param displayNumber displayNumber
     * @param itemId itemId
     */
    fun onTimerEndZap(itemId: Int)

    /**
     * On timer end zap
     * @param displayNumber displayNumber
     */
    fun onTimerEnd()

    /**
     * Channel Type info
     * @param tvChannel tvChannel
     */
    fun getChannelSourceType(tvChannel: TvChannel) : String
}