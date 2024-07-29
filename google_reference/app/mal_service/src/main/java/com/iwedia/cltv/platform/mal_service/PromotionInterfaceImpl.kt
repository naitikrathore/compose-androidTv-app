package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.PromotionInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus

class PromotionInterfaceImpl(private val serviceImpl: IServiceAPI) : PromotionInterface {
    private var promotions = ArrayList<PromotionItem>()
    init {
        InformationBus.informationBusEventListener.registerEventListener(
            arrayListOf(Events.FAST_DATA_UPDATED),
            {},
            {
                promotions.clear()
            })
    }

    override fun getPromotionList(callback: IAsyncDataCallback<ArrayList<PromotionItem>>) {
        if (promotions.isNotEmpty()) {
            callback.onReceive(promotions)
        } else {
            var result = arrayListOf<PromotionItem>()
            serviceImpl.promotionList.forEach {
                result.add(fromServicePromotionItem(it))
            }
            promotions.clear()
            promotions.addAll(result)
            callback.onReceive(result)
        }
    }
}