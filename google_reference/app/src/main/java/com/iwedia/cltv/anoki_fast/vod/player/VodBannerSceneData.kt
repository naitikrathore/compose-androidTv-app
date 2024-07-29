package com.iwedia.cltv.anoki_fast.vod.player

import world.SceneData

/**
 * Data class for VOD (Video On Demand) banner scene.
 *
 * @param previousSceneId The ID of the previous scene.
 * @param previousSceneInstance The instance of the previous scene.
 * @param vodItem The VOD item to be displayed in the banner scene.
 */
class VodBannerSceneData(
    previousSceneId: Int,
    previousSceneInstance: Int,
    val vodItem: Any
) : SceneData(
    previousSceneId,
    previousSceneInstance,
    vodItem
)