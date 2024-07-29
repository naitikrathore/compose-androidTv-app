package com.iwedia.cltv.platform.base.content_provider

import com.iwedia.cltv.platform.model.EntityType
import com.iwedia.cltv.platform.model.IAsyncDataCallback

interface DataProvider {
     fun <T> getDataAsync(type: EntityType, callback: IAsyncDataCallback<List<T>>) {}
     fun <T> getData(type: EntityType): List<T> {
        return mutableListOf()
     }
}