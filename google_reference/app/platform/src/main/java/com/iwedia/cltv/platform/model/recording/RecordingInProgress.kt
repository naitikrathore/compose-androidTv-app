package com.iwedia.cltv.platform.model.recording

import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent

data class RecordingInProgress(
    var id: Int,
    var recordingStart: Long,
    var recordingEnd: Long?,
    var tvChannel: TvChannel,
    var tvEvent: TvEvent,

    ) {
    var currentRecordedDuration : Long = 0
    val maxRecordedDuration  get() = recordingEnd?.minus(recordingStart)
}
