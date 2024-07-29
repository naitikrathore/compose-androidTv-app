package com.iwedia.cltv.scene.PIN

import com.iwedia.cltv.platform.`interface`.ToastInterface
import world.SceneData

/**
 * PIN sceneData
 *
 * @author Nishant Bansal
 */
class PinSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    var text: Any?,
    var listener: PinSceneDataListener
) : SceneData(
    previousSceneId,
    previousSceneInstance,
    text,
    listener
) {
    interface PinSceneDataListener: ToastInterface {
        fun onPinSuccess()
    }
}