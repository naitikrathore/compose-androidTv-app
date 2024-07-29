package com.iwedia.cltv.platform.base

import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.model.vod.Vod
import java.util.*

/**
 * Search interface base implementation
 *
 * @author Dejan Nadj
 */
open class SearchInterfaceBaseImpl(
    private val channelProvider: ChannelDataProviderInterface,
    private val epgDataProvider: EpgDataProviderInterface,
    private val pvrInterface: PvrInterface,
    private val scheduledInterface : ScheduledInterface,
    private val utilsModule: UtilsInterface,
    private val networkModule: NetworkInterface
) : SearchInterface {

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun setup() {
    }

    override fun searchForChannels(query: String, callback: IAsyncDataCallback<List<TvChannel>>) {
        CoroutineHelper.runCoroutine({
            var channelList = channelProvider.getChannelList()
            val result = mutableListOf<TvChannel>()

            channelList.forEach {
                if (it.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault())))
                {
                    if (utilsModule.isThirdPartyChannel(it)) {
                        result.add(it)
                    } else if (it.isBrowsable) {
                        result.add(it)
                    }
                }
            }
            callback.onReceive(result)
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
        pvrInterface.getRecordingList(object : IAsyncDataCallback<List<Recording>> {
            override fun onFailed(error: Error) {
                callback?.onFailed(error)
            }

            override fun onReceive(data: List<Recording>) {
                val result = mutableListOf<Recording>()
                data.forEach {
                    if (it.name.lowercase(Locale.getDefault())
                            .contains(query.lowercase(Locale.getDefault()))
                        || (query.trim().isDigitsOnly() && query.trim().toLong() > it.duration)
                        || (it.videoUrl.lowercase(Locale.getDefault())).contains(query.lowercase(Locale.getDefault()))
                    )
                        result.add(it)
                }
                callback?.onReceive(result)
            }
        })
    }

    override fun searchForScheduledRecordings(
        query: String,
        callback: IAsyncDataCallback<List<ScheduledRecording>>
    ) {
        scheduledInterface.getScheduledRecordingsList(object : IAsyncDataCallback<List<ScheduledRecording>> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: List<ScheduledRecording>) {
                val result = mutableListOf<ScheduledRecording>()
                data.forEach {
                    if (it.name.lowercase(Locale.getDefault())
                            .contains(query.lowercase(Locale.getDefault()))
                        || (query.trim().isDigitsOnly() && query.trim().toInt() > it.scheduledDateStart)
                        || (it.tvChannel?.name?.lowercase(Locale.getDefault())
                            ?.contains(query.lowercase(Locale.getDefault())) == true)
                    )
                        result.add(it)
                }
                callback.onReceive(result)
            }
        })
    }

    override fun searchForScheduledReminders(
        query: String,
        callback: IAsyncDataCallback<List<ScheduledReminder>>?
    ) {
        scheduledInterface.getScheduledRemindersList(object : IAsyncDataCallback<List<ScheduledReminder>> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: List<ScheduledReminder>) {
                val result = mutableListOf<ScheduledReminder>()
                data.forEach {
                    if (it.name.lowercase(Locale.getDefault())
                            .contains(query.lowercase(Locale.getDefault()))
                        || (it.tvChannel?.name?.lowercase(Locale.getDefault())
                            ?.contains(query.lowercase(Locale.getDefault())) == true)
                    )
                        result.add(it)
                }
                callback?.onReceive(result)
            }
        })
    }

    override fun searchForVod(query: String, callback: IAsyncDataCallback<List<Vod>>?) {
        TODO("Not yet implemented")
    }

    override fun getSimilarChannel(channelQuery: String): TvChannel? {
        //getting channel by channel number
        var tvChannel : TvChannel? = null
        tvChannel = getChannelByDisplayNumber(channelQuery)
        //getting channel by name
        if (tvChannel == null){
            var channelName = channelQuery.filterNot { it.isWhitespace() }
            var similarity = 0.0
            var maxSimilarity = 0.0
            //get the maximum similarity channel using name
            channelProvider.getChannelList().forEach { existingChannel ->
                similarity = utilsModule.similarity(existingChannel.name, channelName)
                if (similarity>maxSimilarity && similarity>=0.5f){
                    maxSimilarity = similarity
                    tvChannel = existingChannel
                }
            }
        }
        return tvChannel
    }
    private fun getChannelByDisplayNumber(displayNumber: String) :TvChannel?{
        var channels = channelProvider.getChannelList()
        channels.forEach { channel ->
            if (channel.displayNumber == displayNumber) {
                return channel
            }
        }
        return null
    }

}
