package com.iwedia.cltv.scene.recording_watchlist_conflict_scene

import com.iwedia.cltv.platform.model.TvEvent
import world.SceneData

/**
 *  Recording Watchlist Conflict Scene Data
 *
 *  @author Shubham Kumar
 */


class RecordingWatchlistConflictSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    interface EventSelectedClickListener{
        fun onEventSelected()
    }

    var eventSelectedClickListener: EventSelectedClickListener?= null

    var listOfConflictedTvEvents : MutableList<TvEvent>? = null
}