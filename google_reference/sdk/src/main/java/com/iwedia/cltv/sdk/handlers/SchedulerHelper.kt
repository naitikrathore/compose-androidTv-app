package com.iwedia.cltv.sdk.handlers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.provider.BaseColumns
import android.util.Log
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.ScheduledRecordingData
import com.iwedia.cltv.sdk.entities.ScheduledReminderData

class SchedulerHelper {

    val TAG = javaClass.simpleName
    fun storeScheduledReminder(scheduledReminder: ScheduledReminderData): Boolean {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var cv = ContentValues()

        cv.put(ReferenceContract.ScheduledReminders.NAME_COLUMN, scheduledReminder.name)
        cv.put(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN, scheduledReminder.channelId)
        cv.put(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN, scheduledReminder.eventId)
        cv.put(ReferenceContract.ScheduledReminders.START_TIME_COLUMN, scheduledReminder.startTime)
        return try {
            contentResolver.insert(ReferenceContentProvider.SCHEDULED_REMINDERS_URI, cv)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun removeScheduledReminder(scheduledReminder: ScheduledReminderData): Boolean {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        val uri = ReferenceContentProvider.SCHEDULED_REMINDERS_URI
        var selection =
            ReferenceContract.ScheduledReminders.NAME_COLUMN + " = ? and " + ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN + " = ?"

        return try {
            var ret =
                contentResolver.delete(
                    uri,
                    selection,
                    arrayOf(scheduledReminder.name, scheduledReminder.channelId.toString())
                )
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("Range")
    fun getScheduledRemindersData(): ArrayList<ScheduledReminderData> {
        var retList = arrayListOf<ScheduledReminderData>()
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var cursor = contentResolver.query(
            ReferenceContentProvider.SCHEDULED_REMINDERS_URI,
            null,
            null,
            null,
            null
        )

        if (cursor!!.count > 0) {


            cursor.moveToFirst()
            do {
                var id = -1
                var name = ""
                var channelId = 0
                var eventId = 0
                var startTime = 0L
                if (cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) != null) {
                    id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)).toInt()
                }
                if (cursor.getString(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.NAME_COLUMN)) != null) {
                    name =
                        cursor.getString(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.NAME_COLUMN))
                }
                if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN)) != null) {
                    channelId =
                        cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.CHANNEL_ID_COLUMN))
                            .toInt()
                }
                if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN)) != null) {
                    eventId =
                        cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.EVENT_ID_COLUMN))
                            .toInt()
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData: ${cursor.getLong(
                    cursor.getColumnIndex(
                        ReferenceContract.ScheduledReminders.START_TIME_COLUMN
                    )
                )}")
                if (cursor.getLong(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.START_TIME_COLUMN)) != null) {
                    startTime =
                        cursor.getLong(cursor.getColumnIndex(ReferenceContract.ScheduledReminders.START_TIME_COLUMN))
                            .toLong()
                }
                var reminder = ScheduledReminderData(id, name, channelId, eventId, startTime)
                retList.add(reminder)
            } while (cursor.moveToNext())
        }
        return retList
    }

    fun clearReminderList() {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        contentResolver.delete(ReferenceContentProvider.SCHEDULED_REMINDERS_URI, null, null)
    }

    fun storeScheduledRecording(scheduledRecording: ScheduledRecordingData): Boolean {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var cv = ContentValues()

        cv.put(ReferenceContract.ScheduledRecordings.NAME_COLUMN, scheduledRecording.name)
        cv.put(
            ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN,
            scheduledRecording.tvChannelId
        )

        cv.put(
            ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN,
            scheduledRecording.tvEventId
        )

        cv.put(
            ReferenceContract.ScheduledRecordings.START_TIME_COLUMN,
            scheduledRecording.scheduledDateStart
        )
        cv.put(
            ReferenceContract.ScheduledRecordings.END_TIME_COLUMN,
            scheduledRecording.scheduledDateEnd
        )
        cv.put(ReferenceContract.ScheduledRecordings.DATA_COLUMN, scheduledRecording.data)
        return try {
            contentResolver.insert(ReferenceContentProvider.SCHEDULED_RECORDINGS_URI, cv)
            true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("Range")
    fun getRecodingId(recordedData: ScheduledRecordingData): Int{
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var selection =
            ReferenceContract.ScheduledRecordings.NAME_COLUMN + " = ? and " + ReferenceContract.ScheduledRecordings.START_TIME_COLUMN + " = ? and " + ReferenceContract.ScheduledRecordings.END_TIME_COLUMN + " = ?"
        var cursor = contentResolver.query(
            ReferenceContentProvider.SCHEDULED_RECORDINGS_URI,
            null,
            selection,
            arrayOf(recordedData.name, recordedData.scheduledDateStart.toString(), recordedData.scheduledDateEnd.toString()),
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            return cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
        }
        return -1
    }

    fun removeScheduledRecording(scheduledRecording: ScheduledRecordingData): Boolean {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        val uri = ReferenceContentProvider.SCHEDULED_RECORDINGS_URI
        var selection =
            BaseColumns._ID + " = ? and " + ReferenceContract.ScheduledRecordings.START_TIME_COLUMN + " = ?"

        var id = (ReferenceSdk.pvrSchedulerHandler as ReferencePvrSchedulerHandler).getId(
            scheduledRecording
        )

        return try {
            var ret =
                contentResolver.delete(
                    uri,
                    selection,
                    arrayOf(
                        id.toString(),
                        scheduledRecording.scheduledDateStart.toString()
                    )
                )
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    @SuppressLint("Range")
    fun getScheduledRecordingData(): ArrayList<ScheduledRecordingData> {
        var retList = arrayListOf<ScheduledRecordingData>()

        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        var cursor = contentResolver.query(
            ReferenceContentProvider.SCHEDULED_RECORDINGS_URI,
            null,
            null,
            null,
            null
        )
        if (cursor!!.count > 0) {
            cursor.moveToFirst()
            do {
                var id = -1
                var name = ""
                var channelId = 0
                var tvEventId = -1
                var startTime = 0L
                var endTime = 0L
                var data = ""
                if (cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)) != null) {
                    id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)).toInt()
                }
                if (cursor.getString(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.NAME_COLUMN)) != null) {
                    name =
                        cursor.getString(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.NAME_COLUMN))
                }
                if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN)) != null) {
                    channelId =
                        cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.CHANNEL_ID_COLUMN))
                            .toInt()
                }

                if (cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN)) != null) {
                    tvEventId =
                        cursor.getInt(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.TV_EVENT_ID_COLUMN))
                            .toInt()
                }
                if (cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.START_TIME_COLUMN) >= 0
                    && cursor.getLong(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.START_TIME_COLUMN)) != null
                ) {
                    startTime =
                        cursor.getLong(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.START_TIME_COLUMN))
                            .toLong()
                }
                if (cursor.getLong(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.END_TIME_COLUMN)) != null) {
                    endTime =
                        cursor.getLong(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.END_TIME_COLUMN))
                            .toLong()
                }
                if (cursor.getString(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.DATA_COLUMN)) != null) {
                    data =
                        cursor.getString(cursor.getColumnIndex(ReferenceContract.ScheduledRecordings.DATA_COLUMN))
                }
                var recordingData =
                    ScheduledRecordingData(id, name, startTime, endTime, channelId, tvEventId, data)
                retList.add(recordingData)
            } while (cursor.moveToNext())
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData: ############## LOAD SCHEDULED RECORDINGS FROM DB ${retList.size}")
        return retList
    }

    fun clearRecordingList() {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        contentResolver.delete(ReferenceContentProvider.SCHEDULED_RECORDINGS_URI, null, null)
    }
}
