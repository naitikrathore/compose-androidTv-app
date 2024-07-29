package com.iwedia.cltv.sdk.entities

import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.R
import com.iwedia.cltv.sdk.ReferenceSdk
import core_entities.TvEvent
import data_type.GList
import data_type.GLong
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Reference tv event
 *
 * @author Dejan Nadj
 */
open class ReferenceTvEvent(
    id: Int,
    tvChannel: ReferenceTvChannel,
    name: String,
    shortDescription: String?,
    longDescription: String?,
    imagePath: String?,
    startDate: GLong,
    endDate: GLong,
    categories: GList<Int>? = GList<Int>(),
    parentalRate: Int = 0,
    rating: Int = 0,
    tag: Any? = null,
    parentalRating: String? = null,
    var isProgramSame: Boolean = false,
    var isInitialChannel: Boolean = false,
    var providerFlag: Int? = null

) : TvEvent<ReferenceTvChannel>(
    id,
    tvChannel,
    name,
    shortDescription,
    longDescription,
    imagePath,
    startDate,
    endDate,
    categories!!,
    parentalRate,
    rating,
    tag
) {
    var startTimeInfo: GLong? = null
    var endTimeInfo: GLong? = null
    var parentalRating = parentalRating

    companion object {
        const val DUMMY_EVENT_ID = -555

        /**
         * Creates tv event time info (HH:mm - HH:mm)
         *
         * @param tvEvent tv event
         * @return tv event time info
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun createTvEventTimeInfo24hour(tvEvent: ReferenceTvEvent): String? {
            return if (tvEvent.startDate != null) {
                val sdf: DateTimeFormatter
                var startTime = Date(tvEvent.startDate.value.toLong())
                var endTime = Date(tvEvent.endDate.value.toLong())

                if (tvEvent.startTimeInfo != null) {
                    startTime = Date(tvEvent.startTimeInfo!!.value.toLong())
                }
                if (tvEvent.endTimeInfo != null) {
                    endTime = Date(tvEvent.endTimeInfo!!.value.toLong())
                }
                sdf = if (startTime.day == endTime.day && DateUtils.isToday(tvEvent.startDate.value.toLong())) {
                     DateTimeFormatter.ofPattern("HH:mm")
                } else {
                    DateTimeFormatter.ofPattern("dd/MM HH:mm" )
                }
                val startTimeString = sdf.format( LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startTime.time),
                    ZoneId.systemDefault()
                ))
                val endTimeString = sdf.format( LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(endTime.time),
                    ZoneId.systemDefault()
                ))
                "$startTimeString - $endTimeString"
            } else {
                "No start time defined"
            }
        }


        fun formatDateAndTime(tvEvent: ReferenceTvEvent, clock: Int): String? {
            return if (tvEvent.startDate != null) {

                var startTime = Date(tvEvent.startDate.value.toLong())
                var endTime = Date(tvEvent.endDate.value.toLong())
                val sdfStartDate: SimpleDateFormat
                val sdfEndDate: SimpleDateFormat
                if (tvEvent.startTimeInfo != null) {
                    startTime = Date(tvEvent.startTimeInfo!!.value.toLong())
                }
                if (tvEvent.endTimeInfo != null) {
                    endTime = Date(tvEvent.endTimeInfo!!.value.toLong())
                }

                if (DateUtils.isToday(startTime.time)) {
                    if (clock == 12) {
                        //hh is used for the 12 clock format
                        sdfStartDate = SimpleDateFormat("hh:mm a")
                        sdfEndDate = SimpleDateFormat("hh:mm a")
                    } else {
                        //HH is used for the 24 clock format
                        sdfStartDate = SimpleDateFormat("HH:mm")
                        sdfEndDate = SimpleDateFormat("HH:mm")
                    }
                } else {
                    if (clock == 12) {
                        sdfStartDate = SimpleDateFormat("dd MMM hh:mm a")
                        sdfEndDate = SimpleDateFormat("hh:mm a")
                    } else {
                        sdfStartDate = SimpleDateFormat("dd MMM HH:mm")
                        sdfEndDate = SimpleDateFormat("HH:mm")
                    }
                }
                val startTimeString = sdfStartDate.format(startTime)
                val endTimeString = sdfEndDate.format(endTime)
                "$startTimeString - $endTimeString"
            } else {
                "No start time defined"
            }
        }

        /**
         * Creates tv event time info (HH:mm - HH:mm)
         *
         * @param tvEvent tv event
         * @return tv event time info
         */
        @RequiresApi(Build.VERSION_CODES.O)
        fun createTvEventTimeInfo12hour(tvEvent: ReferenceTvEvent): String? {
            return if (tvEvent.startDate != null) {
                val sdf: DateTimeFormatter
                var startTime = Date(tvEvent.startDate.value.toLong())
                var endTime = Date(tvEvent.endDate.value.toLong())

                if (tvEvent.startTimeInfo != null) {
                    startTime = Date(tvEvent.startTimeInfo!!.value.toLong())
                }
                if (tvEvent.endTimeInfo != null) {
                    endTime = Date(tvEvent.endTimeInfo!!.value.toLong())
                }
                sdf = if (startTime.day == endTime.day && DateUtils.isToday(tvEvent.startDate.value.toLong())) {
                    DateTimeFormatter.ofPattern("h:mma")
                } else {
                    DateTimeFormatter.ofPattern("dd/MM h:mma" )
                }
                val startTimeString = sdf.format( LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startTime.time),
                    ZoneId.systemDefault()
                ))
                val endTimeString = sdf.format( LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(endTime.time),
                    ZoneId.systemDefault()
                ))
                "$startTimeString - $endTimeString"
            } else {
                "No start time defined"
            }
        }

        /**
         * Set progress bar value
         *
         * @param currentTimeData current time
         * @param tvEvent current event
         * @return progress percentage
         */
        fun calculateCurrentProgress(currentTimeData: GLong, tvEvent: ReferenceTvEvent): Int {
            //Current time, event start time, event end time
            val currentTime = currentTimeData.value.toLong()
            val startTime = tvEvent.startDate.value.toLong()
            val endTime = tvEvent.endDate.value.toLong()
            val minutes = (1000 * 60).toLong() //Minutes converter
            //Current event minute, event duration
            val currentMinute = (currentTime - startTime) / minutes
            val duration = (endTime - startTime) / minutes

            var progress = 0
            if (duration > 0) {
                progress = (currentMinute * 100 / duration).toInt()
            }

            //Progress value
            return progress.toInt()
        }


        /**
         * Creates no information tv event for the tv channel
         *
         * @param tvChannel tv channel
         * @return created no information tv event
         */
        fun createNoInformationEvent(tvChannel: ReferenceTvChannel): ReferenceTvEvent {
            var calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            var startDate = calendar.time
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 50)
            calendar.set(Calendar.SECOND, 59)
            var endDate = calendar.time
            var noInformation =
                ReferenceSdk.sdkListener!!.getNoInformationStringResource(R.string.no_information)
            return ReferenceTvEvent(
                -1,
                tvChannel,
                noInformation,
                noInformation,
                noInformation,
                "",
                GLong(startDate.time.toString()),
                GLong(endDate.time.toString())
            )
        }

        /**
         * Creates no information tv event for the tv channel
         *
         * @param tvChannel tv channel
         * @return created no information tv event
         */
        fun createNoInformationEvent(
            tvChannel: ReferenceTvChannel,
            startDate: Date,
            endDate: Date
        ): ReferenceTvEvent {
            var noInformation =
                ReferenceSdk.sdkListener!!.getNoInformationStringResource(R.string.no_information)
            return ReferenceTvEvent(
                -1,
                tvChannel,
                noInformation,
                noInformation,
                noInformation,
                "",
                GLong(startDate.time.toString()),
                GLong(endDate.time.toString())
            )
        }

        /**
         * Creates appLinkCard tv event for the tv channel .
         *
         * @param tvChannel tv channel
         * @return created no information tv app link event
         */
        fun createAppLinkCardEvent(tvChannel: ReferenceTvChannel): ReferenceTvEvent {
            var calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            var startDate = calendar.time
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 50)
            calendar.set(Calendar.SECOND, 59)
            var endDate = calendar.time

            var appLinkText = tvChannel.appLinkText
            var appLinkPosterUri = tvChannel.appLinkPosterUri
            return ReferenceTvEvent(
                tvChannel.id,
                tvChannel,
                appLinkText,
                "",
                "",
                appLinkPosterUri,
                GLong(startDate.time.toString()),
                GLong(endDate.time.toString())
            )
        }
    }

    override fun toString(): String {
        return "ReferenceTvEvent = [id = $id, tv channel = ${tvChannel.name}, name = $name," +
                "start date = ${Date(startDate.value.toLong())}, end date = ${Date(endDate.value.toLong())}"
    }
}