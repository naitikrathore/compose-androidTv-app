package com.iwedia.cltv.scene.intro_scene

import com.iwedia.cltv.platform.`interface`.ToastInterface
import world.SceneListener

/**
 * Intro scene listener
 *
 * @author Aleksandar Lazic
 */
interface IntroSceneListener : SceneListener, ToastInterface {
    fun onAppInitialized()
    fun exitApplication()
    fun isRegionSupported(): Boolean
}