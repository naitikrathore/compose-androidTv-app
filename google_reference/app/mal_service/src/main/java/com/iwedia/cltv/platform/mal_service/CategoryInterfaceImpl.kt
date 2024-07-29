package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncCategoryListener
import com.cltv.mal.model.async.IAsyncTvChannelListener
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus

class CategoryInterfaceImpl(private val serviceImpl: IServiceAPI) : CategoryInterface {
    //Fast data caching
    private var activeCategoryChannelList = HashMap<String, ArrayList<TvChannel>>()
    private var availableFilters = ArrayList<Category>()
    private var filteredChannelList = HashMap<String, ArrayList<TvChannel>>()
    //Broadcast data caching
    private var allBroadcastChannels = ArrayList<TvChannel>()


    init {
        InformationBus.informationBusEventListener.registerEventListener(arrayListOf(
            Events.FAST_DATA_UPDATED,
            Events.ANOKI_RATING_LEVEL_SET,
            Events.CHANNEL_LIST_UPDATED,
            Events.EPG_DATA_UPDATED,
            Events.CHANNEL_LIST_UPDATED,
            Events.FAVORITE_LIST_UPDATED
        ), {}, {
            activeCategoryChannelList.clear()
            availableFilters.clear()
            filteredChannelList.clear()
            if (it == Events.CHANNEL_LIST_UPDATED || it == Events.FAVORITE_LIST_UPDATED) {
                allBroadcastChannels.clear()
            }
        })
    }

    override fun getActiveCategory(applicationMode: ApplicationMode): String {
        return serviceImpl.getActiveCategory(applicationMode.ordinal)
    }

    override fun setActiveCategory(activeCategory: String, applicationMode: ApplicationMode) {
        serviceImpl.setActiveCategory(activeCategory, applicationMode.ordinal)
    }

    override fun getActiveEpgFilter(applicationMode: ApplicationMode): Int {
        return serviceImpl.getActiveEpgFilter(applicationMode.ordinal)
    }

    override fun setActiveEpgFilter(filterId: Int, applicationMode: ApplicationMode) {
        serviceImpl.setActiveEpgFilter(filterId, applicationMode.ordinal)
    }

    override fun getActiveCategoryChannelList(
        activeCategoryId: Int,
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode
    ) {
        CoroutineHelper.runCoroutine({
            if (applicationMode == ApplicationMode.FAST_ONLY) {
                var activeCategoryName = serviceImpl.getActiveCategory(applicationMode.ordinal)
                if (activeCategoryChannelList.containsKey(activeCategoryName) && activeCategoryChannelList[activeCategoryName]!!.isNotEmpty()) {
                    callback.onReceive(activeCategoryChannelList[activeCategoryName]!!)
                    return@runCoroutine
                }
            }
            if (activeCategoryId == 0 && allBroadcastChannels.isNotEmpty()) {
                var response = arrayListOf<TvChannel>()
                response.addAll(allBroadcastChannels)
                callback.onReceive(response)
                return@runCoroutine
            }

            serviceImpl.getActiveCategoryChannelList(
                activeCategoryId,
                applicationMode.ordinal,
                object : IAsyncTvChannelListener.Stub() {
                    override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvChannel>?) {
                        var result = arrayListOf<TvChannel>()
                        response?.forEach { channel ->
                            result.add(
                                fromServiceChannel(channel)
                            )
                        }
                        if (applicationMode == ApplicationMode.FAST_ONLY) {
                            var activeCategoryName =
                                serviceImpl.getActiveCategory(applicationMode.ordinal)
                            activeCategoryChannelList[activeCategoryName] = result
                        }
                        callback.onReceive(result)
                    }
                })
        })

    }

    override fun getAvailableFilters(
        callback: IAsyncDataCallback<ArrayList<Category>>,
        applicationMode: ApplicationMode
    ) {

        CoroutineHelper.runCoroutine({
            if (applicationMode == ApplicationMode.FAST_ONLY && availableFilters.isNotEmpty()) {
                callback.onReceive(availableFilters)
                return@runCoroutine
            }
            serviceImpl.getAvailableFilters(
                applicationMode.ordinal,
                object : IAsyncCategoryListener.Stub() {
                    override fun onResponse(response: Array<out com.cltv.mal.model.entities.Category>?) {
                        var result = arrayListOf<Category>()
                        response?.forEach { category ->
                            result.add(fromServiceCategory(category))
                        }
                        if (applicationMode == ApplicationMode.FAST_ONLY) {
                            if (availableFilters.isEmpty())
                                availableFilters.addAll(result)
                        }
                        callback.onReceive(result)
                    }
                })
        })

    }

    override fun filterChannels(
        category: Category,
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode
    ) {
        CoroutineHelper.runCoroutine({

            if (applicationMode == ApplicationMode.FAST_ONLY && filteredChannelList.containsKey(
                    category.name!!
                ) && filteredChannelList.get(category.name!!)!!.isNotEmpty()
            ) {
                callback.onReceive(filteredChannelList[category.name]!!)
                return@runCoroutine
            }

            if (applicationMode == ApplicationMode.DEFAULT && category.id == 0 && allBroadcastChannels.isNotEmpty()) {
                var response = arrayListOf<TvChannel>()
                response.addAll(allBroadcastChannels)
                callback.onReceive(response)
                return@runCoroutine
            }
            var cat = com.cltv.mal.model.entities.Category(
                category.id,
                category.name,
                category.isSelected,
                category.priority
            )
            serviceImpl.filterChannels(
                cat,
                applicationMode.ordinal,
                object : IAsyncTvChannelListener.Stub() {
                    override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvChannel>?) {
                        var result = arrayListOf<TvChannel>()
                        response?.forEach { tvChannel ->
                            result.add(fromServiceChannel(tvChannel))
                        }
                        if (applicationMode == ApplicationMode.FAST_ONLY) {
                            filteredChannelList[category.name!!] = result
                        }
                        if (applicationMode == ApplicationMode.DEFAULT && category.id == 0) {
                            allBroadcastChannels.clear()
                            allBroadcastChannels.addAll(result)
                        }
                        callback.onReceive(result)
                    }
                })
        })

    }
}