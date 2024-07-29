package com.iwedia.cltv.sdk.entities

data class ScheduledRecordingData(
    var id: Int,
    var name: String,
    var scheduledDateStart: Long,
    var scheduledDateEnd: Long,
    var tvChannelId: Int,
    var tvEventId: Int,
    var data: String
)