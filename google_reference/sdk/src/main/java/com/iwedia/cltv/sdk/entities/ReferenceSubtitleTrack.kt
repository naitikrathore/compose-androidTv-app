package com.iwedia.cltv.sdk.entities

import android.media.tv.TvTrackInfo
import com.iwedia.cltv.sdk.handlers.ReferencePlayerHandler
import core_entities.AudioTrack
import core_entities.SubtitleTrack

class ReferenceSubtitleTrack : ReferenceTrackObject {

    constructor(trackInfo: TvTrackInfo) : super(Type.SUBTITLE, trackInfo)
}