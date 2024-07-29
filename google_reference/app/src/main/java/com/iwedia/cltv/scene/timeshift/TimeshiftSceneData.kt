package com.iwedia.cltv.scene.timeshift

import com.iwedia.cltv.platform.model.TvEvent
import world.SceneData

/**
 * Timeshift scene data
 *
 * @author Dejan Nadj
 */
class TimeshiftSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    var tvEvent: TvEvent? = null
    var isPauseClicked: Boolean = false
}