package com.iwedia.cltv.scene.recording_conflict

import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import world.SceneData

class RecordingConflictSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {
    interface ItemClicked{
        fun onFirstItemClicked()
        fun onSecondItemClicked()
    }

    enum class RecordingConflictType(){
        RECORDING_RECORDING, RECORDING_SCHEDULE, SCHEDULE_RECORDING, SCHEDULE_SCHEDULE
    }

    var type: RecordingConflictType? = null

    var title : String? = null

    var firstSchedule : MutableList<ScheduledRecording>? = null
    var secondSchedule : ScheduledRecording? = null

    var firstRecording : TvEvent? = null
    var secondRecording : TvEvent? = null

    var conflictMessage : String? = null

    var firstItemText : String? = null
    var secondItemText : String? = null

    var onItemClicked: ItemClicked? = null
}