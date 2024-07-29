package com.iwedia.cltv.scene.recording_watchlist_conflict_scene

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import world.SceneListener

/**
 *  Recording Watchlist Conflict Scene Listener
 *
 *  @author Shubham Kumar
 */


interface RecordingWatchlistConflictSceneListener : SceneListener, TTSSetterInterface,
    ToastInterface {
    fun onEventSelected()
    fun isTimeShiftActive(): Boolean
    fun timeShiftStop(callback: IAsyncCallback)
    fun changeChannel(tvChannel: TvChannel, callback: IAsyncCallback)
    fun playChannel(scheduledChannel: TvChannel)
    fun startRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback)
    fun getCurrentTime(tvChannel: TvChannel): Long
}