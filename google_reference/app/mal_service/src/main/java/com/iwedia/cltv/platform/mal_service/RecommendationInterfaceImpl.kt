package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.RecommendationInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.RecommendationItem
import com.iwedia.cltv.platform.model.RecommendationRow

class RecommendationInterfaceImpl(private val serviceImpl: IServiceAPI) : RecommendationInterface {
    override fun getRecommendationRows(callback: IAsyncDataCallback<ArrayList<RecommendationRow>>) {
        var result = arrayListOf<RecommendationRow>()
        serviceImpl.recommendationRows.forEach { row ->
            var list = arrayListOf<RecommendationItem>()
            row.items.forEach {
                if (it.genre == null) it.genre = ""
                list.add(fromServiceRecommendationItem(it))
            }
            result.add(RecommendationRow(row.name, list))
        }
        callback.onReceive(result)
    }

}