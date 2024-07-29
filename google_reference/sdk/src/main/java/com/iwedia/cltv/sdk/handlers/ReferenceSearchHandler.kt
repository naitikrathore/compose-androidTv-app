package com.iwedia.cltv.sdk.handlers

import android.text.TextUtils
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.entities.ReferenceTvEvent
import core_entities.*
import data_type.GList
import handlers.DataProvider
import handlers.SearchHandler
import listeners.AsyncDataReceiver
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ReferenceSearchHandler(dataProvider: DataProvider<*>) : SearchHandler<ReferenceTvChannel,ReferenceTvEvent, Vod,Recording<ReferenceTvChannel,ReferenceTvEvent,Any>,ScheduledRecording<ReferenceTvChannel,ReferenceTvEvent>,
        ScheduledReminder<ReferenceTvChannel,ReferenceTvEvent>>(dataProvider) {

    override fun searchForChannels(
        searchQuery: String?,
        callback: AsyncDataReceiver<GList<ReferenceTvChannel>>?
    ) {
        CoroutineHelper.runCoroutine({
            if (searchQuery != null) {
                if(TextUtils.isEmpty(searchQuery.trim())){
                    callback!!.onFailed(null)
                    return@runCoroutine
                }
            }

            var channelList = ReferenceSdk.tvHandler!!.getChannelList()
            val result = GList<ReferenceTvChannel>()

            channelList.value.forEach {
                if ((it.inputId.lowercase(Locale.getDefault()).contains("com.google.android.tv.dtvinput"))
                    || (it.inputId.lowercase(Locale.getDefault()).contains("com.mediatek.tvinput"))) {
                    if ((it.name.lowercase(Locale.getDefault()).contains(searchQuery!!.lowercase(Locale.getDefault())))
                        || (searchQuery.trim().isDigitsOnly() && it.lcn == searchQuery.trim().toInt())
                        || (searchQuery.trim().isDigitsOnly() && it.displayNumber == searchQuery.trim())) {
                        result.add(it)
                    }
                } else {
                    if (it.inputId.lowercase(Locale.getDefault()).contains(searchQuery!!.lowercase(Locale.getDefault()))
                        || (it.name.lowercase(Locale.getDefault()).contains(searchQuery.lowercase(Locale.getDefault())))
                        || (searchQuery.trim().isDigitsOnly() && it.lcn == searchQuery.trim().toInt())
                        || (searchQuery.trim().isDigitsOnly() && it.displayNumber == searchQuery.trim())) {
                        result.add(it)
                    }
                }
            }
            callback!!.onReceive(result)
        })
    }

    override fun searchForEvents(
        searchQuery: String?,
        callback: AsyncDataReceiver<GList<ReferenceTvEvent>>?
    ) {
        val result = GList<ReferenceTvEvent>()
        var channelList = ReferenceSdk.tvHandler!!.getChannelList()
        var counter = AtomicInteger(0)

        channelList.value.forEach { it ->
            dataProvider!!.getDataAsync(DataProvider.DataType.TV_EVENT,object : AsyncDataReceiver<GList<ReferenceTvEvent>>{
                override fun onFailed(error: Error?) {
                    if (counter.incrementAndGet() == channelList.size()) {
                        callback!!.onReceive(result)
                    }
                }

                override fun onReceive(data: GList<ReferenceTvEvent>) {


                    data.value.forEach {
                        if (it.name.lowercase(Locale.getDefault()).contains(searchQuery!!.lowercase(
                                Locale.getDefault()
                            )) ){
                            result.add(it)
                        }
                    }
                    if (counter.incrementAndGet() == channelList.size()) {
                        callback!!.onReceive(result)
                    }
                }
            }, it)
        }
    }
}