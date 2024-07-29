package com.iwedia.cltv

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.utils.Utils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * Custom TextView for Time
 *
 * @author Nishant Bansal
 */

class TimeTextView : androidx.appcompat.widget.AppCompatTextView {

    private lateinit var dateTimeFormat : DateTimeFormat
    var time : Any? = null

    @RequiresApi(Build.VERSION_CODES.R)
    set(value) {
        if (value is TvEvent) {
            this.text = createTvEventTimeInfo(value)
        } else if (value is String) {
            this.text = if (DateFormat.is24HourFormat(context)) {
                value
            } else {
                Utils.convert12Hr(value)
            }
        } else if (value is TvChannel) {
            this.text = value.name
        }
        field = value
    }

    constructor(context: Context?) : super(context!!) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
    }

    fun setDateTimeFormat(dateTimeFormat : DateTimeFormat){
        this.dateTimeFormat = dateTimeFormat
    }

    /**
     * Creates tv event time info (HH:mm - HH:mm)
     *
     * @param tvEvent tv event
     * @return tv event time info
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createTvEventTimeInfo(tvEvent: TvEvent): String {
        // Determine the date and time format based on whether the event is today or not
        val dateTimePattern = if (DateUtils.isToday(tvEvent.startTime) && DateUtils.isToday(tvEvent.endTime)) {
            dateTimeFormat.timePattern
        } else {
            dateTimeFormat.dateTimePattern
        }

        val dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern)

        val startTime = dateTimeFormatter.format(
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(tvEvent.startTime),
                ZoneId.systemDefault()
            )
        )
        val endTime = dateTimeFormatter.format(
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(tvEvent.endTime),
                ZoneId.systemDefault()
            )
        )

        return "$startTime - $endTime"
    }

}