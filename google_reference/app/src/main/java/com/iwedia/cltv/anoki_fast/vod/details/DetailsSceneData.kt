package com.iwedia.cltv.anoki_fast.vod.details

import tv.anoki.ondemand.domain.model.VODItem
import world.SceneData

class DetailsSceneData(
    previousSceneId: Int,
    previousSceneInstance: Int,
    val contentId: String
) : SceneData(
    previousSceneId,
    previousSceneInstance,
    contentId
)