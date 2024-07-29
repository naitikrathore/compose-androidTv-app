package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.FastFavoriteInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback

class FastFavoriteInterfaceImpl(private val serviceImpl: IServiceAPI) : FastFavoriteInterface {
    override fun getFavorites(callback: IAsyncDataCallback<ArrayList<String>>) {
        var result = arrayListOf<String>()
        serviceImpl.favorites.forEach {
            result.add(it)
        }
        callback.onReceive(result)
    }

    override fun updateFavorites(
        channelId: String,
        addFavorite: Boolean,
        callback: IAsyncCallback
    ) {
        serviceImpl.updateFavorites(channelId, addFavorite)
        callback.onSuccess()
    }

}