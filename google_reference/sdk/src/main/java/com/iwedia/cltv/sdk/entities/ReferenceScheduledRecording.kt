package com.iwedia.cltv.sdk.entities

import core_entities.ScheduledRecording
import data_type.GLong

open class ReferenceScheduledRecording(
    id: Int,
    name: String,
    scheduledDateStart: GLong,
    scheduledDateEnd: GLong,
    tvChannel: ReferenceTvChannel,
    tvEvent: ReferenceTvEvent?,
    repeatFreq: Int
) :
    ScheduledRecording<ReferenceTvChannel, ReferenceTvEvent>(
        id, name, scheduledDateStart, scheduledDateEnd, tvChannel, tvEvent
    ) {
    var repeatFreq: Int = repeatFreq
    var scheduledRecordingsListIds: ArrayList<String> = ArrayList()

    object REPEAT_FLAG {
        var NONE = 0
        var DAILY = 1
        var WEEKLY = 2
    }

    fun updateId(id: Int) {
        this.id = id
    }

    override fun toString(): String {
        return "Scheduled recording : [id = ${id}, name = ${name}, scheduled date start = ${scheduledDateStart}, scheduled date end = ${scheduledDateEnd}, tvChannel =${tvChannel}, tvEvent = ${tvEvent}, repeat frequency = ${repeatFreq} "
    }
}

