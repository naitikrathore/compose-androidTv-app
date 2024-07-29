package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.RecommendationInterface
import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.RecommendationRow

open class RecommendationInterfaceBaseImpl constructor(var fastDataProviderInterface: FastDataProviderInterface): RecommendationInterface{

    open val TAG = "RecommendationInterfaceBaseImpl"

    override fun getRecommendationRows(callback: IAsyncDataCallback<ArrayList<RecommendationRow>>) {
        var recommendationList  = fastDataProviderInterface.getRecommendationRows()
        if (recommendationList.size > 0) {
            callback.onReceive(recommendationList)
        } else {
            callback.onFailed(Error("Recommendations not found."))
        }
    }
}