package com.iwedia.cltv.scene.parental_control.locked_channel
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import world.SceneListener

/**
 * Parental channel lock scene listener
 */
interface ParentalChannelLockSceneListener : SceneListener, TTSSetterInterface {
    fun onUnlockPressed()
    fun checkPin(pin : String)
    fun requestActiveChannel()
    fun isAccessibilityEnabled(): Boolean
}