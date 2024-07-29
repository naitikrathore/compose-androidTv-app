package com.iwedia.cltv.sdk.entities

data class ScheduledReminderData(
    var id: Int,
    var name: String,
    var channelId: Int,
    var eventId: Int,
    var startTime: Long
)
