package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.RecommendationItem
import com.iwedia.cltv.platform.model.RecommendationRow

interface RecommendationInterface {

    fun getRecommendationRows(callback: IAsyncDataCallback<ArrayList<RecommendationRow>>)

}