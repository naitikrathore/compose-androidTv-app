package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PromotionItem

interface PromotionInterface {

    fun getPromotionList(callback: IAsyncDataCallback<ArrayList<PromotionItem>>)

}