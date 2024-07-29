package com.iwedia.cltv.platform.model.recording

import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.PlayableItem
import java.util.*

class Recording (
    var id : Int = -1,
    var name : String = "",
    var duration : Long = 0,
    var recordingDate: Long = 0,
    var image : String,
    var videoUrl : String,
    var tvChannel: TvChannel?= null,
    var tvEvent: TvEvent? = null,
    var recordingStartTime : Long,
    var recordingEndTime : Long,
    var shortDescription : String,
    var longDescription : String = "",
    var contentRating: String,
    var isEventLocked : Boolean = false
) : PlayableItem
{
    var recordedEvent: TvEvent ?= null
    var data: Any?= null
    override fun toString(): String {
        return "Recording = [id = $id, name = $name," +
                "duration = ${duration}, startTime = ${Date(recordingStartTime)}" +
                ", endTime = ${Date(recordingEndTime)}, imagePath = $image," +
                " uri = $videoUrl, tv channel = ${tvChannel?.name}, event = ${tvEvent?.name}"
    }

    fun isInProgress(): Boolean {
        return duration == 0L
    }

    companion object {

        /**
         * Creates tv event time info (HH:mm - HH:mm)
         *
         * @param recordingDate recording item date
         * @param recordingEndTime recording item end time
         * @return tv event time info
         */
        fun createRecordingTimeInfo(recordingDate: Long, recordingEndTime: Long): String? {
            return if (recordingDate != null && recordingEndTime != null) {
                var startTime = Date(recordingDate)
                var endTime = Date(recordingEndTime)

                if (recordingDate != null) {
                    startTime = Date(recordingDate)
                }
                if (recordingEndTime!! != null) {
                    endTime = Date(recordingEndTime)
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
        fun calculateCurrentProgress(currentTimeData: Long, startTime: Long, endTime: Long): Int {
            if (startTime == null || endTime == null) {
                return 0
            }
            val currentTime = currentTimeData
            val currentMinute = (currentTime - startTime)
            val duration = (endTime!! - startTime)

            var progress = 0
            if (duration > 0) {
                progress = (currentMinute * 100 / duration).toInt()
            }

            //Progress value
            return progress.toInt()
        }
    }
}
