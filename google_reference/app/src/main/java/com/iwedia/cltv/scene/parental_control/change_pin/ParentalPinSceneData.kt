package com.iwedia.cltv.scene.parental_control.change_pin

import world.SceneData

/**
 * Parental pin scene data
 *
 * @author Dejan Nadj
 */
class ParentalPinSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    companion object {
        const val DEFAULT_CHANGE_PIN_SCENE_TYPE = 0
        const val CA_CHANGE_PIN_SCENE_TYPE = 1
    }

    var sceneType = DEFAULT_CHANGE_PIN_SCENE_TYPE
}