package com.iwedia.cltv.platform.model.player.track

interface IAudioTrack : ITrack {
    var isAnalogTrack: Boolean
    var analogName: String?
    var isAd: Boolean
    var isDolby: Boolean
    var isHohAudio: Boolean
    var isSps: Boolean
    var isAdSps: Boolean
    var apdText : String
}