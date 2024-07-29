package com.iwedia.cltv.scene.PIN

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import world.SceneListener

/**
 * PIN Scene Listener
 *
 * @author Nishant Bansal
 */
interface PinSceneListener : SceneListener, TTSSetterInterface {
    fun checkPin(pin : String)

    /**
     * Checks whether the user has set a custom PIN, or if it remains at its default value.
     *
     * @return `true` if the PIN has been changed from its default value, `false` otherwise.
     */
    fun isParentalPinChanged(): Boolean
    fun getParentalPin(): String
    fun isAccessibilityEnabled(): Boolean
    fun isDefaultPinNotificationRequired(): Boolean
}