package com.iwedia.cltv.tis.model

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContentRating
import android.media.tv.TvContract
import android.net.Uri
import android.util.Log
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvEvent
import java.util.Date

/**
 * Data class for EPG events data
 *
 * @author Abhilash M R
 */
class ProgramDescriptor (
    var mChId: Long = 0,
    var mStartTime: String = "",
    var mTitle: String = "",
    var mDescription: String = "",
    var mThumbNail: String = "",
    var mIcon: String = "",
    var mRating: String = "",
    var mGenre: String = "",
    var mRuntime: String = "",
    var mLanguage: String = "",
) {


    fun getContentValues(inputId: String?): ContentValues? {
        val ret = ContentValues()
        ret.put(TvContract.Programs.COLUMN_AUDIO_LANGUAGE, mLanguage)
        ret.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, mRuntime)
        ret.put(TvContract.Programs.COLUMN_BROADCAST_GENRE, mGenre)
        ret.put(TvContract.Programs.COLUMN_CONTENT_RATING, mRating)
        ret.put(TvContract.Programs.COLUMN_POSTER_ART_URI, mIcon)
        ret.put(TvContract.Programs.COLUMN_THUMBNAIL_URI, mThumbNail)
        ret.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, mDescription)
        ret.put(TvContract.Programs.COLUMN_TITLE, mTitle)
        ret.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, mStartTime)
        ret.put(TvContract.Programs.COLUMN_CHANNEL_ID, mChId)
        return ret
    }

    companion object {
        fun getProjection(): Array<String?>? {
            return arrayOf(
                TvContract.Programs.COLUMN_CHANNEL_ID,
                TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS,
                TvContract.Programs.COLUMN_TITLE,
                TvContract.Programs.COLUMN_SHORT_DESCRIPTION,
                TvContract.Programs.COLUMN_THUMBNAIL_URI,
                TvContract.Programs.COLUMN_POSTER_ART_URI,
                TvContract.Programs.COLUMN_CONTENT_RATING,
                TvContract.Programs.COLUMN_BROADCAST_GENRE,
                TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS,
                TvContract.Programs.COLUMN_AUDIO_LANGUAGE
            )
        }

        fun getProgramRating(): TvContentRating {
            return TvContentRating.createRating("com.android.tv","US_TV", "US_TV_14", "US_TV_S")
        }

        @SuppressLint("Range")
        fun getProgramRating(context: Context, channelId: Long): TvContentRating? {
            val uri: Uri = TvContract.buildProgramsUriForChannel(channelId)

            //Create query
            var epgCursor: Cursor? = null
            epgCursor = context.contentResolver
                .query(uri, null, null, null, null)
            var currentTime = System.currentTimeMillis()
            var contentRating: TvContentRating ?= null
            Log.d(Constants.LogTag.CLTV_TAG + "Anoki", "current time ${Date(currentTime)}")
            if (epgCursor != null) {
                epgCursor.moveToFirst()
                var startTime = 0L
                var endTime = 0L
                loop@ while (!epgCursor.isAfterLast) {
                    if (epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS)) != null) {
                        startTime =
                            epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS))
                    }
                    if (epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS)) != null) {
                        endTime =
                            epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS))
                    }
                    if (startTime <= currentTime && endTime >= currentTime) {
                        Log.d(Constants.LogTag.CLTV_TAG + "Anoki", "event time filter")
                        var rating = epgCursor.getString(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_CONTENT_RATING))
                        Log.d(Constants.LogTag.CLTV_TAG + "Anoki", "content rating ${rating}")
                        if (rating == null) {
                            epgCursor.close()
                            return null
                        }
                        contentRating = TvContentRating.unflattenFromString(rating)
                    }
                    if (contentRating != null) {
                        break@loop
                    }
                    epgCursor.moveToNext()
                }
            }
            epgCursor?.close()
            return contentRating
        }

        @SuppressLint("Range")
        fun getCurrentEventEndTime(context: Context, channelId: Long): Long {
            val uri: Uri = TvContract.buildProgramsUriForChannel(channelId)

            //Create query
            var epgCursor: Cursor? = null
            epgCursor = context.contentResolver
                .query(uri, null, null, null, null)
            var currentTime = System.currentTimeMillis()
            var contentRating: TvContentRating ?= null
            Log.d(Constants.LogTag.CLTV_TAG + "Anoki", "current time ${Date(currentTime)}")
            if (epgCursor != null) {
                epgCursor.moveToFirst()
                var startTime = 0L
                var endTime = 0L
                loop@ while (!epgCursor.isAfterLast) {
                    if (epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS)) != null) {
                        startTime =
                            epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS))
                    }
                    if (epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS)) != null) {
                        endTime =
                            epgCursor.getLong(epgCursor.getColumnIndex(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS))
                    }
                    if (startTime <= currentTime && endTime >= currentTime) {
                        epgCursor.close()
                        return endTime
                    }
                    epgCursor.moveToNext()
                }
            }
            epgCursor?.close()
            return 0
        }
    }
}