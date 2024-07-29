package com.iwedia.cltv.sdk.handlers

import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.ReferenceRecording
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.entities.ReferenceTvEvent
import core_entities.Error
import data_type.GList
import data_type.GLong
import handlers.DataProvider
import handlers.PvrHandler
import listeners.AsyncDataReceiver
import java.util.concurrent.atomic.AtomicInteger

/**
 * Reference recording handler
 *
 * @author Dragan Krnjaic
 */
class ReferenceRecordingsHandler(dataProvider: DataProvider<*>) :
    PvrHandler<ReferenceTvChannel, ReferenceTvEvent, ReferenceRecording>(dataProvider) {

    fun getRecordings(callback: AsyncDataReceiver<GList<ReferenceRecording>>) {

        var recordingList: GList<ReferenceRecording>? = GList()
        var counter = AtomicInteger(0)

        for (i in 0 until ReferenceSdk.tvHandler!!.getChannelList().size()) {
            ReferenceSdk.epgHandler!!.getCurrentEvent(
                ReferenceSdk.tvHandler!!.getChannelList().get(i)!!,
                object : AsyncDataReceiver<ReferenceTvEvent> {
                    override fun onFailed(error: Error?) {
                        if (counter.incrementAndGet() == ReferenceSdk.tvHandler!!.getChannelList()
                                .size()
                        ) {
                            callback.onReceive(recordingList!!)
                        }
                    }

                    override fun onReceive(data: ReferenceTvEvent) {
                        recordingList!!.add(
                            ReferenceRecording(
                                i,
                                "Recording " + i,
                                GLong(""),
                                GLong(""),
                                ReferenceSdk.tvHandler!!.getChannelList()
                                    .get(i)!!.logoImagePath as String,
                                "",
                                ReferenceSdk.tvHandler!!.getChannelList().get(i)!!,
                                data
                            )
                        )

                        if (counter.incrementAndGet() == ReferenceSdk.tvHandler!!.getChannelList()
                                .size()
                        ) {
                            callback.onReceive(recordingList)
                        }
                    }
                })
        }
    }
}