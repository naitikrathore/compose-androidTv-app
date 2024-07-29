package com.iwedia.cltv.scene.preferences_info_scene

import world.SceneData

/**
 * Preferences info scene data
 *
 * @author Dejan Nadj
 */
class PreferencesInfoSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    var title: String = ""
    var items: ArrayList<InfoData> = ArrayList()
    var type: Int = INFO_ABOUT_TYPE

    inner class InfoData constructor(
        var title:String = "",
        var content:String =""
    )

    companion object {
        const val INFO_MESSAGES_TYPE = 0
        const val INFO_ABOUT_TYPE = 1
    }
}