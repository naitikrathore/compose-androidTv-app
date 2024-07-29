package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.model.vod.Vod

interface SearchInterface {
    fun dispose()

    fun setup()

    fun searchForChannels(query:String, callback: IAsyncDataCallback<List<TvChannel>>)

    fun searchForEvents(query:String, callback: IAsyncDataCallback<List<TvEvent>>)

    fun searchForRecordings(query:String, callback: IAsyncDataCallback<List<Recording>>?)

    fun searchForScheduledRecordings(query:String, callback: IAsyncDataCallback<List<ScheduledRecording>>)

    fun searchForScheduledReminders(query:String, callback: IAsyncDataCallback<List<ScheduledReminder>>?)

    fun searchForVod(query:String, callback: IAsyncDataCallback<List<Vod>>?)
    /*
        get the channel using query: it could be either name or channel display number
     */
    fun getSimilarChannel(channelQuery : String) : TvChannel?
}