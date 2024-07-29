package com.iwedia.cltv.platform.base.content_provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import android.util.Log
import com.iwedia.cltv.platform.`interface`.SchedulerDataProviderInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

class SchedulerDataProvider(var context: Context): SchedulerDataProviderInterface {

    val TAG = javaClass.simpleName
    override fun storeScheduledReminder(scheduledReminder: ScheduledReminder, callback: IAsyncCallback) {
        val contentResolver: ContentResolver = context.contentResolver
        var cv = ContentValues()

        cv.put(Contract.ScheduledReminders.NAME_COLUMN, scheduledReminder.name)
        cv.put(Contract.ScheduledReminders.CHANNEL_ID_COLUMN, scheduledReminder.tvChannelId)
        cv.put(Contract.ScheduledReminders.EVENT_ID_COLUMN, scheduledReminder.tvEventId)
        cv.put(Contract.ScheduledReminders.START_TIME_COLUMN, scheduledReminder.startTime)
        try {
            contentResolver.insert(ContentProvider.SCHEDULED_REMINDERS_URI, cv)
            callback.onSuccess()
        } catch (e: Exception) {
            callback.onFailed(Error("Insert cannot be done"))
        }
    }

    override fun removeScheduledReminder(scheduledReminder: ScheduledReminder, callback: IAsyncCallback) {
        val contentResolver: ContentResolver = context.contentResolver
        val uri = ContentProvider.SCHEDULED_REMINDERS_URI
        val selection =
            Contract.ScheduledReminders.CHANNEL_ID_COLUMN + " = ? and " + Contract.ScheduledReminders.START_TIME_COLUMN + " = ?"

        try {
            var ret =
                contentResolver.delete(
                    uri,
                    selection,
                    arrayOf(scheduledReminder.tvChannelId.toString(),scheduledReminder.startTime.toString())
                )
            callback.onSuccess()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            callback.onFailed(Error("Delete cannot be done"))
        }
    }

    @SuppressLint("Range")
    override fun getScheduledRemindersData(): ArrayList<ScheduledReminder> {
        val retList = arrayListOf<ScheduledReminder>()
        val contentResolver: ContentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContentProvider.SCHEDULED_REMINDERS_URI,
            null,
            null,
            null,
            null
        )

        if (cursor!!.count > 0) {
            cursor.moveToFirst()
            do {
                var name = ""
                val id: Int = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))
                if (cursor.getString(cursor.getColumnIndex(Contract.ScheduledReminders.NAME_COLUMN)) != null) {
                    name =
                        cursor.getString(cursor.getColumnIndex(Contract.ScheduledReminders.NAME_COLUMN))
                }
                val channelId: Int = cursor.getInt(cursor.getColumnIndex(Contract.ScheduledReminders.CHANNEL_ID_COLUMN))
                val eventId: Int = cursor.getInt(cursor.getColumnIndex(Contract.ScheduledReminders.EVENT_ID_COLUMN))
                Log.d(
                    com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData: ${cursor.getLong(
                    cursor.getColumnIndex(
                        Contract.ScheduledReminders.START_TIME_COLUMN
                    )
                )}")
                val startTime: Long = cursor.getLong(cursor.getColumnIndex(Contract.ScheduledReminders.START_TIME_COLUMN))
                val reminder = ScheduledReminder(id, name, null,null,startTime = startTime, tvChannelId = channelId, tvEventId = eventId)
                retList.add(reminder)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return retList
    }

    override fun clearReminderList() {
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.delete(ContentProvider.SCHEDULED_REMINDERS_URI, null, null)
    }

    override fun storeScheduledRecording(scheduledRecording: ScheduledRecording, callback: IAsyncCallback) {
        val contentResolver: ContentResolver = context.contentResolver
        var cv = ContentValues()

        cv.put(Contract.ScheduledRecordings.NAME_COLUMN, scheduledRecording.name)
        cv.put(Contract.ScheduledRecordings.CHANNEL_ID_COLUMN, scheduledRecording.tvChannelId)
        cv.put(Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN, scheduledRecording.tvEventId)
        cv.put(Contract.ScheduledRecordings.START_TIME_COLUMN, scheduledRecording.scheduledDateStart)
        cv.put(Contract.ScheduledRecordings.END_TIME_COLUMN, scheduledRecording.scheduledDateEnd)
        cv.put(Contract.ScheduledRecordings.DATA_COLUMN, scheduledRecording.repeatFreq.name)
        try {
            contentResolver.insert(ContentProvider.SCHEDULED_RECORDINGS_URI, cv)
            callback.onSuccess()
        } catch (e:Exception) {
            callback.onFailed(Error("Insert cannot be done"))
        }
    }

    @SuppressLint("Range")
    override fun getRecodingId(recordedData: ScheduledRecording, callback: IAsyncDataCallback<Int>){
        val contentResolver: ContentResolver = context.contentResolver
        var selection =
            Contract.ScheduledRecordings.NAME_COLUMN + " = ? and " + Contract.ScheduledRecordings.START_TIME_COLUMN + " = ? and " + Contract.ScheduledRecordings.END_TIME_COLUMN + " = ?"
        var cursor = contentResolver.query(
            ContentProvider.SCHEDULED_RECORDINGS_URI,
            null,
            selection,
            arrayOf(recordedData.name, recordedData.scheduledDateStart.toString(), recordedData.scheduledDateEnd.toString()),
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            callback.onReceive(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)))
        } else {
            callback.onFailed(Error("Error getting Recording ID"))
        }
    }

    override fun removeScheduledRecording(scheduledRecording: ScheduledRecording, callback: IAsyncCallback) {
        val contentResolver: ContentResolver = context.contentResolver
        val uri = ContentProvider.SCHEDULED_RECORDINGS_URI
        var selection = Contract.ScheduledRecordings.CHANNEL_ID_COLUMN + " = ? and " + Contract.ScheduledRecordings.START_TIME_COLUMN + " = ?"

        try {
            var ret =
                contentResolver.delete(
                    uri,
                    selection,
                    arrayOf(scheduledRecording.tvChannelId.toString(), scheduledRecording.scheduledDateStart.toString())
                )
            callback.onSuccess()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            callback.onFailed(Error("Delete cannot be done"))
        }
    }

    @SuppressLint("Range")
    override fun getScheduledRecordingData(): ArrayList<ScheduledRecording> {
        var retList = arrayListOf<ScheduledRecording>()

        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            ContentProvider.SCHEDULED_RECORDINGS_URI,
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
                if (cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN)) != null) {
                    name = cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.NAME_COLUMN))
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.ScheduledRecordings.CHANNEL_ID_COLUMN)) != null) {
                    channelId = cursor.getInt(cursor.getColumnIndex(Contract.ScheduledRecordings.CHANNEL_ID_COLUMN)).toInt()
                }
                if (cursor.getInt(cursor.getColumnIndex(Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN)) != null) {
                    tvEventId = cursor.getInt(cursor.getColumnIndex(Contract.ScheduledRecordings.TV_EVENT_ID_COLUMN)).toInt()
                }
                if ( cursor.getColumnIndex(Contract.ScheduledRecordings.START_TIME_COLUMN) >= 0
                    && cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.START_TIME_COLUMN)) != null) {
                    startTime = cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.START_TIME_COLUMN)).toLong()
                }
                if (cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.END_TIME_COLUMN)) != null) {
                    endTime = cursor.getLong(cursor.getColumnIndex(Contract.ScheduledRecordings.END_TIME_COLUMN)).toLong()
                }
                if (cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.DATA_COLUMN)) != null) {
                    data = cursor.getString(cursor.getColumnIndex(Contract.ScheduledRecordings.DATA_COLUMN))
                }
                var recordingData = ScheduledRecording(id, name, startTime, endTime, channelId, tvEventId,  RepeatFlag.valueOf(data))
                retList.add(recordingData)
            } while (cursor.moveToNext())
        }
        cursor.close()

        return retList
    }

    override fun clearRecordingList() {
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.delete(ContentProvider.SCHEDULED_RECORDINGS_URI, null, null)
    }
}
