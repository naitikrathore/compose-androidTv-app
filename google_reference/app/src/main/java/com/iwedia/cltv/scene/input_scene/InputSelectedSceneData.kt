package com.iwedia.cltv.scene.input_scene

import world.SceneData

/**
 * Input selected scene data
 *
 * @author Dejan Nadj
 */
class InputSelectedSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
) : SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {

    /**
     * Selected input source type
     */
    var inputType: String = ""

    /**
     * Input resolution
     */
    var inputIcon: String = ""

    var inputPixelValue: String = ""


    var inputHdrValue: String = ""

    /**
     * Input source name
     */
    var inputSourceName: String = ""
}