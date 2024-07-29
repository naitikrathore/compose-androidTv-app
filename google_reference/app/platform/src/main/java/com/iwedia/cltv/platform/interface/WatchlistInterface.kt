package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.ArrayList

interface WatchlistInterface {
    fun scheduleReminder(
        reminder: ScheduledReminder,
        callback: IAsyncCallback?
    )

    fun clearWatchList()

    fun removeScheduledReminder(
        reminder: ScheduledReminder,
        callback: IAsyncCallback?
    )

    fun getWatchList(callback: IAsyncDataCallback<MutableList<ScheduledReminder>>)

    fun getWatchListCount(callback: IAsyncDataCallback<Int>)

    fun getWatchlistItem(
        position: Int,
        callback: IAsyncDataCallback<ScheduledReminder>
    )

    fun hasScheduledReminder(tvEvent: TvEvent?, callback: IAsyncDataCallback<Boolean>)

    fun removeWatchlistEventsForDeletedChannels()

    fun loadScheduledReminders()

    fun isInWatchlist(referenceTvEvent: TvEvent): Boolean
    fun checkReminderConflict(channelId: Int, startTime: Long): Boolean
}