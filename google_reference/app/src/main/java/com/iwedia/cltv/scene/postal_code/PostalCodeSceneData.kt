package com.iwedia.cltv.scene.postal_code

import world.SceneData

/**
 * Postal scene data
 *
 * @author Tanvi Raut
 */
class PostalCodeSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    companion object {
        const val DEFAULT_POSTAL_CODE_SCENE_TYPE = 0
    }

    interface SubmitListener {
        fun onSubmit(postalCode: String)
    }
    var submitListener: SubmitListener? = null
    var sceneType = DEFAULT_POSTAL_CODE_SCENE_TYPE
}