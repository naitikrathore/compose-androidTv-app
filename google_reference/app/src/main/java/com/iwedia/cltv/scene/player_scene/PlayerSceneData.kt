package com.iwedia.cltv.scene.player_scene

import com.iwedia.cltv.platform.model.TvEvent
import world.SceneData

/**
 * Player scene data
 *
 * @author Dejan Nadj
 */
class PlayerSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {

    var playerType: Int = PLAYER_TYPE_DEFAULT
    var tvEvent: Any?= null
    var isPauseClicked : Boolean = false
    var recordedContent: Any?= null

//    var newRecordedContent: Recording?= null
    var newTvEvent: TvEvent?= null

    companion object {
        const val PLAYER_TYPE_DEFAULT = 100
        const val PLAYER_TYPE_TIME_SHIFT = 200
        const val PLAYER_TYPE_PVR = 300

        var isOnlyPlayback: Boolean = false
    }
}