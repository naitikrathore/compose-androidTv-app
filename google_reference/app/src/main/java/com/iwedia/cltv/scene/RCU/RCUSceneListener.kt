package com.iwedia.cltv.scene.RCU

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import world.SceneListener

interface RCUSceneListener:SceneListener, TTSSetterInterface {
    fun digitPressed(digit: Int)
    fun onKey()
    fun okPressed()
    fun getCurrentTime(): Long
    fun timerEnd()
    fun ttxPressed()
    fun redPressed()
    fun greenPressed()
    fun bluePressed()
    fun yellowPressed()
    fun backPressed()
    fun ttxState(): Boolean
}