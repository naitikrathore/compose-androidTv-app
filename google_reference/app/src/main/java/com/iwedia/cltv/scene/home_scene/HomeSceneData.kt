package com.iwedia.cltv.scene.home_scene

import world.SceneData

/**
 * Home scene data
 *
 * @author Dejan Nadj
 */
class HomeSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    /**
     * Home scene initial filter position
     */
    var initialFilterPosition = 0

    /**
     * Move focus to the current event in guide grid
     */
    var focusToCurrentEvent: Boolean = true

    /**
     * Open edit channel preference deeplink
     */
    var openEditChannel: Boolean = false

    /**
     * Open device info preference deeplink
     */
    var openDeviceInfo: Boolean = false

}