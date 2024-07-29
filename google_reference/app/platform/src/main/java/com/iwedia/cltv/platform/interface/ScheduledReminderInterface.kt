package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.recording.ScheduledReminder

interface ScheduledReminderInterface {
    fun getScheduledReminders(): ArrayList<ScheduledReminder>
    fun loadScheduledReminders()
}