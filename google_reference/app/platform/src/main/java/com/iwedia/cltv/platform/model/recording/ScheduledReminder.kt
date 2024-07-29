package com.iwedia.cltv.platform.model.recording

import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent

data class ScheduledReminder(
    var id: Int,
    val name: String,
    //todo these two objects need to be removed
    val tvChannel: TvChannel? = null,
    val tvEvent: TvEvent? = null,
    //////
    val startTime: Long? = null,
    val tvChannelId: Int? = null,
    val tvEventId: Int? = null
)
