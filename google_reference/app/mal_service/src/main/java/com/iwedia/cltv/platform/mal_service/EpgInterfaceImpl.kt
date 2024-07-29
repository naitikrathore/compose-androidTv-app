package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncTvEventListener
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent

class EpgInterfaceImpl(private val serviceImpl: IServiceAPI) : EpgInterface {
    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun getCurrentEvent(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            var channel = toServiceChannel(tvChannel)
            val currentEvent = serviceImpl.getCurrentEvent(channel)
            callback.onReceive(fromServiceTvEvent(currentEvent))
        })
    }

    override fun updateEpgData(applicationMode: ApplicationMode) {
        serviceImpl.updateEpgData(applicationMode.ordinal)
    }

    override fun getEvent(index: Int, callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            val event = serviceImpl.getTvEvent(index)
            callback.onReceive(fromServiceTvEvent(event))
        })
    }

    override fun getEventById(id: Int, callback: IAsyncDataCallback<TvEvent>) {
        CoroutineHelper.runCoroutine({
            val event = serviceImpl.getTvEventById(id)
            callback.onReceive(fromServiceTvEvent(event))
        })
    }

    override fun getEventByNameAndStartTime(
        name: String,
        startTime: Long?,
        channelId: Int,
        callback: IAsyncDataCallback<TvEvent>
    ) {
        CoroutineHelper.runCoroutine({
            val event = serviceImpl.getTvEventByNameAndStartTime(name, startTime!!)
            callback.onReceive(fromServiceTvEvent(event))
        })
    }

    override fun getEventList(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            serviceImpl.getEventList(object : IAsyncTvEventListener.Stub() {
                override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvEvent>?) {
                    var result = arrayListOf<TvEvent>()
                    response?.forEach { tvEvent ->
                        result.add(fromServiceTvEvent(tvEvent))
                    }
                    callback.onReceive(result)
                }
            })
        })

    }

    private val eventListByChannelCache = HashMap<Long, ArrayList<TvEvent>>()

    override fun getEventListByChannel(
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<ArrayList<TvEvent>>
    ) {
        CoroutineHelper.runCoroutine({
            if (eventListByChannelCache.containsKey(tvChannel.channelId)) {
                callback.onReceive(eventListByChannelCache.get(tvChannel.channelId)!!)
            } else {
                var channel = toServiceChannel(tvChannel)
                serviceImpl.getEventListByChannel(channel,
                    object : IAsyncTvEventListener.Stub() {
                        override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvEvent>?) {
                            var result = arrayListOf<TvEvent>()

                            response?.forEach { tvEvent ->
                                result.add(fromServiceTvEvent(tvEvent))
                            }
                            eventListByChannelCache.put(tvChannel.channelId, result)
                            callback.onReceive(result)
                        }
                    })
            }

        })

    }

    override fun getEventListByChannelAndTime(
        tvChannel: TvChannel,
        startTime: Long,
        endTime: Long,
        callback: IAsyncDataCallback<ArrayList<TvEvent>>
    ) {
        CoroutineHelper.runCoroutine({
            getEventListByChannel(tvChannel, object : IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onReceive(data: ArrayList<TvEvent>) {
                    var channelEventListByTime = arrayListOf<TvEvent>()
                    data.forEach { tvEvent ->
                        if ((startTime > tvEvent.startTime && startTime > tvEvent.endTime) //  Event completely left side of selected date
                            || (endTime < tvEvent.startTime && endTime < tvEvent.endTime) //  Event completely right side of selected date
                        ) {
                            return@forEach
                        } else {
                            channelEventListByTime.add(tvEvent)
                        }
                    }
                    callback.onReceive(channelEventListByTime)
                }

                override fun onFailed(error: Error) {
                }
            })


            /*var channel = toServiceChannel(tvChannel)
            serviceImpl.getEventListByChannelAndTime(
                channel,
                startTime,
                endTime,
                object : IAsyncTvEventListener.Stub() {
                    override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvEvent>?) {
                        var result = arrayListOf<TvEvent>()

                        response?.forEach { tvEvent ->
                            result.add(fromServiceTvEvent(tvEvent))
                        }
                        callback.onReceive(result)
                    }
                })*/

        })

    }

    override fun getAllCurrentEvent(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            serviceImpl.getAllCurrentEvent(object : IAsyncTvEventListener.Stub() {
                override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvEvent>?) {
                    var result = arrayListOf<TvEvent>()
                    response?.forEach { tvEvent ->
                        result.add(fromServiceTvEvent(tvEvent))
                    }
                    callback.onReceive(result)
                }
            })
        })

    }

    override fun getAllNextEvents(callback: IAsyncDataCallback<ArrayList<TvEvent>>) {
        CoroutineHelper.runCoroutine({
            serviceImpl.getAllNextEvents(object : IAsyncTvEventListener.Stub() {
                override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvEvent>?) {
                    var result = arrayListOf<TvEvent>()
                    response?.forEach { tvEvent ->
                        result.add(fromServiceTvEvent(tvEvent))
                    }
                    callback.onReceive(result)
                }
            })
        })

    }

    override fun getNextEventByChannel(
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<TvEvent>
    ) {
    }

    override fun setActiveWindow(tvChannelList: MutableList<TvChannel>, startTime: Long) {}

    override fun clearActiveWindow() {}

    override fun getStartTimeForActiveWindow(): Long {
        return 0
    }
}