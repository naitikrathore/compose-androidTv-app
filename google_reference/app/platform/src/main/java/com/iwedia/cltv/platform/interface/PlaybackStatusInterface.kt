package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.player.PlaybackStatus

interface PlaybackStatusInterface {

    var isNetworkAvailable :Boolean
    var isSignalAvailable :Boolean
    var isWaitingChannel :Boolean
    var isChannelsAvailable  :Boolean
    var isPlayerTimeout :Boolean
    var appJustStarted :Boolean
    var isPvrPlaybackActive :Boolean
    var isLockedOverlay: Boolean
    fun onPlaybackStatusChanged(playbackStatus: PlaybackStatus)
}