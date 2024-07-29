package com.iwedia.cltv.scene.preferences_status

import world.SceneData

/**
 * Preferences Cam Info status scene data
 *
 * @author Dejan Nadj
 */
class PreferencesStatusSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    var title: String = ""
    var items: ArrayList<StatusItem> = ArrayList()

    open inner class StatusItem constructor(
        var mainItem: Boolean = false,
        var item1: String = "",
        var item2: String = "",
        var item3: String = "",
        var item4: String = ""
    )
}