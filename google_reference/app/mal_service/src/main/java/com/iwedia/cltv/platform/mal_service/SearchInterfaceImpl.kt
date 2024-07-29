package com.iwedia.cltv.platform.mal_service

import androidx.core.text.isDigitsOnly
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.SearchInterface
import com.iwedia.cltv.platform.mal_service.epg.EpgDataProvider
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.model.vod.Vod
import java.util.ArrayList
import java.util.Locale

class SearchInterfaceImpl(private val epgDataProvider: EpgDataProvider, private val serviceImpl: IServiceAPI) : SearchInterface {
    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun searchForChannels(query: String, callback: IAsyncDataCallback<List<TvChannel>>) {
        CoroutineHelper.runCoroutine({
            var resultList = arrayListOf<TvChannel>()
            serviceImpl.searchForChannels(query).forEach { tvChannel ->
                resultList.add(fromServiceChannel(tvChannel))
            }
            callback.onReceive(resultList)
        })
    }

    override fun searchForEvents(query: String, callback: IAsyncDataCallback<List<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            val result = ArrayList<TvEvent>()
            var eventList = epgDataProvider.getEventList()
            eventList.forEach {
                if (it.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                    || (query.trim().isDigitsOnly() && it.startTime >= query.trim().toLong())
                    || (it.tvChannel.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault())))
                )
                    result.add(it)
            }
            callback.onReceive(result)
        })
    }

    override fun searchForRecordings(
        query: String,
        callback: IAsyncDataCallback<List<Recording>>?
    ) {
        callback?.onReceive(arrayListOf())
    }

    override fun searchForScheduledRecordings(
        query: String,
        callback: IAsyncDataCallback<List<ScheduledRecording>>
    ) {
        TODO("Not yet implemented")
    }

    override fun searchForScheduledReminders(
        query: String,
        callback: IAsyncDataCallback<List<ScheduledReminder>>?
    ) {
        TODO("Not yet implemented")
    }

    override fun searchForVod(query: String, callback: IAsyncDataCallback<List<Vod>>?) {
        TODO("Not yet implemented")
    }

    override fun getSimilarChannel(channelQuery: String): TvChannel? {
        return fromServiceChannel(serviceImpl.getSimilarChannel(channelQuery))
    }
}