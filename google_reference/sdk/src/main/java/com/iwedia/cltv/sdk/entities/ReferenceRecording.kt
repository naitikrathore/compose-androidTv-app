package com.iwedia.cltv.sdk.entities

import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.handlers.ReferenceTvHandler
import core_entities.Recording
import data_type.GLong
import java.util.*


class ReferenceRecording(
    id: Int = -1,
    name: String = "",
    duration: GLong = GLong("0"),
    recordingDate: GLong = GLong("0"),
    image: String = "",
    videoUrl: String = "",
    tvChannel: ReferenceTvChannel ?= null,
    tvEvent: ReferenceTvEvent ? = null
) : Recording<ReferenceTvChannel, ReferenceTvEvent, Any>(
    id,
    name,
    duration,
    recordingDate,
    image,
    videoUrl,
    tvChannel!!,
    tvEvent!!
) {
    var recordedEvent: ReferenceTvEvent ?= null
    var shortDescription: String = ""
    var contentRating : String = ""
    var recordingEndTime: GLong? = GLong("0")

    override fun toString(): String {
        return "ReferenceRecording = [id = $id, name = $name," +
                "duration = ${duration.value}, startTime = ${Date(recordingDate.value.toLong())}" +
                ", endTime = ${Date(recordingEndTime!!.value.toLong())}, imagePath = $image," +
                " uri = $videoUrl, tv channel = ${tvChannel?.name}, event = ${tvEvent?.name}"
    }

    fun isInProgress(): Boolean {
        return duration.value.toLong() == 0L
    }

    companion object {

        /**
         * Creates tv event time info (HH:mm - HH:mm)
         *
         * @param recordingDate recording item date
         * @param recordingEndTime recording item end time
         * @return tv event time info
         */
        fun createRecordingTimeInfo(recordingDate: GLong, recordingEndTime: GLong): String? {
            return if (recordingDate != null && recordingEndTime != null) {
                var startTime = Date(recordingDate.value.toLong())
                var endTime = Date(recordingEndTime!!.value.toLong())

                if (recordingDate != null) {
                    startTime = Date(recordingDate!!.value.toLong())
                }
                if (recordingEndTime!! != null) {
                    endTime = Date(recordingEndTime!!.value.toLong())
                }
                var totalSeconds = (endTime.time - startTime.time) / 1000
                var hours = totalSeconds / 3600 % 24
                var minutes = totalSeconds / 60 % 60
                var seconds = totalSeconds % 60
                if (totalSeconds < 0) {
                    hours = 0
                    minutes = 0
                    seconds = 0
                }
                return if (hours > 0) {
                    (if (hours < 10) "0$hours h" else "$hours h").toString() + " " + (if (minutes < 10) "0$minutes min" else "$minutes min") + " " + if (seconds < 10) "0$seconds sec" else "$seconds sec"
                } else if (minutes > 0) {
                    (if (minutes < 10) "0$minutes min" else "$minutes min").toString() + " " + if (seconds < 10) "0$seconds sec" else "$seconds sec"
                } else {
                    if (seconds < 10) "0$seconds sec" else "$seconds sec"
                }
            } else {
                "No start time defined"
            }
        }

        /**
         * Calculate progress bar value
         *
         * @param currentTimeData current time
         * @param startTime item start time
         * @param endTime item end time
         * @return progress percentage
         */
        fun calculateCurrentProgress(currentTimeData: GLong, startTime: GLong, endTime: GLong): Int {
            if (startTime == null || endTime == null) {
                return 0
            }
            val currentTime = currentTimeData.value.toLong()
            val currentMinute = (currentTime - startTime.value.toLong())
            val duration = (endTime!!.value.toLong() - startTime.value.toLong())

            var progress = 0
            if (duration > 0) {
                progress = (currentMinute * 100 / duration).toInt()
            }

            //Progress value
            return progress.toInt()
        }
    }

    fun getChannelDisplayNumber(): String {
        tvChannel.let {
            (ReferenceSdk.tvHandler as ReferenceTvHandler).getChannelList().value.forEach { referenceTvChannel ->
                if (referenceTvChannel.id == it.id) {
                    return referenceTvChannel.displayNumber
                }
            }
            return it.displayNumber
        }
        return "0"
    }
}
