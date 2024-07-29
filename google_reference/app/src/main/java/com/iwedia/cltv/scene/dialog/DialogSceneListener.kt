package com.iwedia.cltv.scene.dialog

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.TTSStopperInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import world.SceneListener

/**
 * Dialog scene listener
 *
 * @author Aleksandar Lazic
 */
interface DialogSceneListener
    : SceneListener,
    TTSSetterInterface,
    ToastInterface,
    TTSStopperInterface {
    fun onPositiveButtonClicked()
    fun onNegativeButtonClicked()
    fun exitApplication()
}