package com.iwedia.cltv.anoki_fast.vod.details

import world.SceneListener

interface DetailsSceneListener: SceneListener {
    fun onVodItemClicked(vodItem: Any)
}