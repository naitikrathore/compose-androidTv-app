package com.iwedia.cltv.scene.reminder_conflict_scene

import com.iwedia.cltv.platform.model.TvEvent
import world.SceneData

class ReminderSceneData constructor(
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

    //listener to give callback when any event is clicked in list or when reminder conflict overlay is destroyed.
    var eventSelectedClickListener: EventSelectedClickListener?= null

    //list to store conflicted watchlist events.
    var listOfConflictedTvEvents : MutableList<TvEvent>? = null
}