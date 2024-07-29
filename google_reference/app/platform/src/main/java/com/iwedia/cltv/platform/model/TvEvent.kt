package com.iwedia.cltv.platform.model

import java.util.*

data class TvEvent(
    val id: Int,
    val tvChannel: TvChannel,
    val name: String,
    val shortDescription: String?,
    val longDescription: String?,
    val imagePath: String?,
    var startTime: Long,
    var endTime: Long,
    val categories: List<Int>?,
    val parentalRate: Int,
    val rating: Int,
    val tag: Any? = null,
    var parentalRating:String?,
    var isProgramSame: Boolean,
    var isInitialChannel: Boolean,
    var providerFlag: Int?,
    var genre: String? = null,
    var subGenre: String? = null,
    var isSchedulable: Boolean = true,
    var tvEventId : Int? = -1,

    //RRT5 parental data
    var rrt5Rating: String = "",
){
    //creates new duplicate object of existing tvEvent
    fun clone() : TvEvent {
        return TvEvent(
            id,
            tvChannel,
            name,
            shortDescription,
            longDescription,
            imagePath,
            startTime,
            endTime,
            categories,
            parentalRate,
            rating,
            tag,
            parentalRating,
            isProgramSame,
            isInitialChannel,
            providerFlag,
            genre,
            subGenre,
            isSchedulable,
            tvEventId
        )
    }

    fun isGuideCardEvent(): Boolean = tag != null && tag is String && tag == "guide"

    fun isVodContent(): Boolean = subGenre == Constants.VodTypeConstants.SERIES || subGenre == Constants.VodTypeConstants.SINGLE_WORK

    var data: Any?= null

    companion object{
        const val DUMMY_EVENT_ID = -555

        /**
         * Make dummy createNoInformationEvent object
         */
        fun createNoInformationEvent(tvChannel: TvChannel, currentTimeData: Long ): TvEvent {
            var calendar = Calendar.getInstance()
            calendar.time.time = currentTimeData
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            var startDate = calendar.time
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 50)
            calendar.set(Calendar.SECOND, 59)
            var endDate = calendar.time

            var noInformation = "No Information"
            val recordedTvEvent: TvEvent = TvEvent(
                -1,
                tvChannel,
                noInformation,
                noInformation,
                noInformation,
                "",
                startDate.time,
                endDate.time,
                null,
                0,
                0,
                null,
                "",
                false,
                false,
                null,
                null,
                null
            )

            return recordedTvEvent
        }

        fun createRecordingEvent(
            tvChannel: TvChannel, currentTimeData: Long, parentalRating: String,
            recordingName: String, shortDescription: String, longDescription: String,
            startTime: Long, endTime: Long
        ): TvEvent {

            return TvEvent(
                -1, tvChannel, recordingName, shortDescription, longDescription, "",
                startTime, endTime, null, 0, 0, null, parentalRating,
                false, false, null, null, null
            )
        }

        /**
         * Creates no information tv event for the tv channel
         *
         * @param tvChannel tv channel
         * @return created no information tv event
         */
        fun createNoInformationEvent(
            tvChannel: TvChannel,
            startTime: Long,
            endTime: Long, currentTimeData: Long
        ): TvEvent {

            val event = createNoInformationEvent(tvChannel, currentTimeData)

            event.startTime = startTime
            event.endTime = endTime

            return event
        }

        /**
         * Creates Tune to {channel name} for more information tv event
         *
         * @param tvChannel tv channel
         * @return created tv event
         */
        fun createTuneToChannelForMoreInformationEvent(
            tvChannel: TvChannel,
            startTime: Long,
            endTime: Long, currentTimeData: Long,
            text: String
        ): TvEvent {

            var calendar = Calendar.getInstance()
            calendar.time.time = currentTimeData
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            var startDate = calendar.time
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 50)
            calendar.set(Calendar.SECOND, 59)
            var endDate = calendar.time

            val noInformation = if (text.startsWith("Tune to"))
                "Tune to ${tvChannel.name} channel for more information"
            else
                text
            val event: TvEvent = TvEvent(
                -1,
                tvChannel,
                noInformation,
                "",
                "",
                "",
                startDate.time,
                endDate.time,
                null,
                0,
                0,
                null,
                "",
                false,
                false,
                null,
                null,
                null
            )

            event.startTime = startTime
            event.endTime = endTime

            return event
        }

        /**
         * Set progress bar value
         *
         * @param currentTimeData current time
         * @param tvEvent current event
         * @return progress percentage
         */
        fun calculateCurrentProgress(currentTimeData: Long, tvEvent: TvEvent): Int {
            //Current time, event start time, event end time
            val currentTime = currentTimeData
            val startTime = tvEvent.startTime
            val endTime = tvEvent.endTime
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

        fun createAppLinkCardEvent(tvChannel: TvChannel): TvEvent {
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
            return TvEvent(
                tvChannel.id,
                tvChannel,
                appLinkText,
                "",
                "",
                appLinkPosterUri,
                startDate.time.toString().toLong(),
                endDate.time.toString().toLong(),
                null,
                0,
                0,
                null,
                "",
                false,
                false,
                null,
                null,
                null
            )
        }
    }
}
