package com.iwedia.cltv.scene.oad

import world.SceneData

class OadSceneData(
    previousSceneId: Int,
    previousSceneInstance: Int,
    var title: String?,
    var version: String,
    var description: String?,
    var progress: String?,
    var sceneState: eSceneState,
    var topButtonEnabled: Boolean,
    var bottomButtonEnabled: Boolean,
    var topButtonText: String,
    var bottomButtonText: String,
) : SceneData(
    previousSceneId,
    previousSceneInstance,
    title,
    version,
    description,
    progress,
    sceneState,
    topButtonEnabled,
    bottomButtonEnabled,
    topButtonText,
    bottomButtonText,
) {
    enum class eSceneState {
        OAD_SCENE_SCAN,
        OAD_SCENE_SCAN_FAIL,
        OAD_SCENE_SCAN_SUCCESS,
        OAD_SCENE_DOWNLOAD,
        OAD_SCENE_DOWNLOAD_FAIL,
        OAD_SCENE_RESTART_CONFIRM,
        OAD_UP_TO_DATE
    }
}