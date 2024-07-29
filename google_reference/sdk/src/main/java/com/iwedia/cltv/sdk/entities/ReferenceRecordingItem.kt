package com.iwedia.cltv.sdk.entities

import default_sdk.entities.DefaultScheduledRecording
import default_sdk.entities.DefaultTvChannel
import default_sdk.entities.DefaultTvEvent

class ReferenceRecordingItem constructor(
    referenceScheduledRecording: ReferenceScheduledRecording,
    var recListIds: ArrayList<String> = arrayListOf(),

): DefaultScheduledRecording(
    referenceScheduledRecording.id,
    referenceScheduledRecording.name ,
    referenceScheduledRecording.scheduledDateStart,
    referenceScheduledRecording.scheduledDateEnd,
    referenceScheduledRecording.tvChannel as DefaultTvChannel,
    referenceScheduledRecording.tvEvent as DefaultTvEvent) {
}