package com.iwedia.cltv.scene.parental_control.change_pin

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import world.SceneListener

/**
 * Parental pin scene listener
 *
 * @author Aleksandar Lazic
 */
interface ParentalPinSceneListener : SceneListener, TTSSetterInterface,
    ToastInterface {
    fun onCurrentPinEntered(pin: String)
    fun onNewPinEntered(pin: String)
    fun onConfirmNewPinEntered(pin: String)
    fun isAccessibilityEnabled(): Boolean
}