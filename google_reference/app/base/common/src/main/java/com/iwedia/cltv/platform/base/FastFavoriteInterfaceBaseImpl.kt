package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.FastFavoriteInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback

class FastFavoriteInterfaceBaseImpl constructor(private var fastDataProviderInterface: FastDataProviderInterface) :
    FastFavoriteInterface {
    override fun getFavorites(callback: IAsyncDataCallback<ArrayList<String>>) {
        var favoriteList  = fastDataProviderInterface.getFastFavoriteList()
        if (favoriteList.isNotEmpty()) {
            callback.onReceive(favoriteList)
        } else {
            callback.onFailed(Error("Favorite List  not found."))
        }
    }

    override fun updateFavorites(channelId: String, addFavorite: Boolean, callback: IAsyncCallback) {
        fastDataProviderInterface.updateFavoriteList(channelId,addFavorite,object :IAsyncCallback{
            override fun onFailed(error: Error) { callback.onFailed(error) }
            override fun onSuccess() { callback.onSuccess() }
        })
    }
}