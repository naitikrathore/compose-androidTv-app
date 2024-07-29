package com.iwedia.cltv.scene.walkthrough_scene

import com.iwedia.cltv.platform.`interface`.ToastInterface
import world.SceneListener

/**
 * Intro scene listener
 *
 * @author Aleksandar Lazic
 */
interface WalkthroughSceneListener : SceneListener, ToastInterface {
    fun onAppInitialized()

    fun onEnd()

    fun exitApplication()
    fun isAccessibilityEnabled(): Boolean
    fun getConfigInfo(nameOfInfo: String): Boolean
}