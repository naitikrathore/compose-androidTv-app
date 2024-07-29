package com.iwedia.cltv.platform.model.recording

import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent

data class ScheduledRecording(
    var id : Int,
    val name: String,
    val scheduledDateStart: Long,
    val scheduledDateEnd: Long,
    val tvChannelId: Int,
    val tvEventId: Int?,
    var repeatFreq : RepeatFlag,
    //todo these two objects need to be removed
    var tvChannel: TvChannel? = null,
    var tvEvent: TvEvent? = null
)
