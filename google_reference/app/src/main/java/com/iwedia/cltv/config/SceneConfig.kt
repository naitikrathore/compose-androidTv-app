package com.iwedia.cltv.config

import com.iwedia.cltv.config.entities.ConfigParam

/**
 * Runtime configuration item for single scene
 * @author Veljko Ilkic
 */
class SceneConfig {

    var sceneId: Int = -1
    var name: String = ""
    var title: String = ""
    var value: MutableList<ConfigParam> = mutableListOf()

    constructor(
        sceneId: Int,
        sceneName: String,
        sceneTitle: String,
        values: MutableList<ConfigParam>
    ) {
        this.sceneId = sceneId
        this.name = sceneName
        this.title = sceneTitle
        this.value.addAll(values)
    }

}