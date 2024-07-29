package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback

interface FastFavoriteInterface {
    fun getFavorites(callback: IAsyncDataCallback<ArrayList<String>>)

    /**
     * addFavorite - true for adding and false for removing from list
     */
    fun updateFavorites(channelId: String, addFavorite:Boolean ,callback: IAsyncCallback)
}