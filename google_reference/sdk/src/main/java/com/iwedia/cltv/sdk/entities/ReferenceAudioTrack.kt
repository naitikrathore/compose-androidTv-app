package com.iwedia.cltv.sdk.entities

import android.media.tv.TvTrackInfo

class ReferenceAudioTrack : ReferenceTrackObject {
    constructor(trackInfo: TvTrackInfo) : super(Type.AUDIO, trackInfo)
}