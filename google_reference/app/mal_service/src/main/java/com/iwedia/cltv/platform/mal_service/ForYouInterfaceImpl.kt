package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncRailItemListener
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.foryou.RailItem

class ForYouInterfaceImpl(private val serviceImpl: IServiceAPI) : ForYouInterface {
    override fun setup() {}

    override fun dispose() {}

    override fun getAvailableRailSize(): Int {
        return serviceImpl.availableRailSize
    }

    override fun getForYouRails(callback: IAsyncDataCallback<ArrayList<RailItem>>) {
        serviceImpl.getForYouRails(object : IAsyncRailItemListener.Stub() {
            override fun onResponse(response: Array<out com.cltv.mal.model.entities.RailItem>?) {
                var result = arrayListOf<RailItem>()
                response?.forEach { railItem ->
                    result.add(fromServiceRailItem(railItem))
                }
                callback.onReceive(result)
            }
        })
    }

    override fun updateRailData() {
        serviceImpl.updateRailData()
    }

    override fun setPvrEnabled(pvrEnabled: Boolean) {
        serviceImpl.setPvrEnabled(pvrEnabled)
    }
}