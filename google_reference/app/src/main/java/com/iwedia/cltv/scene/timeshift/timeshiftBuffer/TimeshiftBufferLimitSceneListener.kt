package com.iwedia.cltv.scene.timeshift.timeshiftBuffer

import world.SceneListener

interface TimeshiftBufferLimitSceneListener : SceneListener {
    fun onTimeShiftWatchClicked()
    fun onReturnToLiveClicked()
}