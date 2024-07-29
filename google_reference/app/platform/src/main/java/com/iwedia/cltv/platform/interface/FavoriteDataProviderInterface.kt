package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.*
import com.iwedia.cltv.platform.model.favorite.FavoriteItem

/**
 * favorite data provider interface
 *
 */
interface FavoriteDataProviderInterface {

    fun getFavorites(callback: IAsyncDataCallback<ArrayList<FavoriteItem>>)
    fun deleteFromFavorites(dataItem: FavoriteItem, callback: IAsyncCallback)
    fun addToFavorites(dataItem: FavoriteItem, callback: IAsyncCallback)
    fun removeFavoriteCategory(category: String, callback: IAsyncCallback)
    fun renameFavoriteCategory(
        newName: String,
        oldName: String,
        callback: IAsyncCallback
    )
    fun dispose()
    fun getFavChannelList() : ArrayList<TvChannel>
    fun clearFavourites()
}