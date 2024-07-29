package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.PromotionInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PromotionItem

open class PromotionInterfaceBaseImpl constructor(var fastDataProviderInterface: FastDataProviderInterface): PromotionInterface {
    open val TAG = "PromotionInterfaceBaseImpl"
    override fun getPromotionList(callback: IAsyncDataCallback<ArrayList<PromotionItem>>) {
        var promotionList  = fastDataProviderInterface.getPromotionList()

        if (promotionList.size > 0) {
            callback.onReceive(promotionList)
        } else {
            callback.onFailed(Error("Recommendations not found."))
        }
    }
}