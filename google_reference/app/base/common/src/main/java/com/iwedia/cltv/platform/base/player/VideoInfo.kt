package com.iwedia.cltv.platform.base.player

import com.iwedia.cltv.platform.base.util.VideoDefinition

data class VideoInfo(
    var width: Int = 0,
    var height: Int = 0,
    var format: VideoDefinition = VideoDefinition.UNKNOWN_LEVEL,
    var frameRate: Float = 0f,
    var aspectRation: Float = 0f
)
