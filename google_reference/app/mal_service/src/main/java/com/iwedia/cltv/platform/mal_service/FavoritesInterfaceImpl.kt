package com.iwedia.cltv.platform.mal_service

import android.util.Log
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.cltv.mal.model.async.IAsyncListener
import com.iwedia.cltv.platform.`interface`.UtilsInterface


class FavoritesInterfaceImpl(private val serviceImpl: IServiceAPI, utilsInterface: UtilsInterface) : FavoritesInterface {
    override fun setup() {
    }

    override fun dispose() {
    }

    override fun updateFavoriteItem(item: FavoriteItem, callback: IAsyncCallback) {
        val favoriteItem = toServiceFavoriteItem(item)
        serviceImpl.updateFavoriteItem(
            favoriteItem,
            object : IAsyncListener.Stub() {
                override fun onSuccess() {
                    callback.onSuccess()
                }

                override fun onFailed(error: String) {
                    callback.onFailed(Error(error))
                }
            })
    }

    override fun isInFavorites(favoriteItem: FavoriteItem, callback: IAsyncDataCallback<Boolean>) {
        CoroutineHelper.runCoroutine({
            val favoriteItem = toServiceFavoriteItem(favoriteItem)
            callback.onReceive(serviceImpl.isInFavorites(favoriteItem))
        })
    }

    override fun getFavoriteItems(callback: IAsyncDataCallback<List<FavoriteItem>>) {
        CoroutineHelper.runCoroutine({
            val result = arrayListOf<FavoriteItem>()
            serviceImpl.favoriteItems.forEach { favoriteItem ->
                result.add(fromServiceFavoriteItem(favoriteItem))
            }
            callback.onReceive(result)
        })
    }

    override fun getFavoriteListByType(
        type: Int,
        callback: IAsyncDataCallback<List<FavoriteItem>>
    ) {
        CoroutineHelper.runCoroutine({
            val result = arrayListOf<FavoriteItem>()
            serviceImpl.getFavoriteListByType(type).forEach { favoriteItem ->
                result.add(fromServiceFavoriteItem(favoriteItem))
            }
            callback.onReceive(result)
        })
    }

    override fun geFavoriteCategories(): ArrayList<String> {
        return serviceImpl.geFavoriteCategories().toCollection(ArrayList())
    }

    override fun addFavoriteCategory(category: String, receiver: IAsyncCallback) {
        CoroutineHelper.runCoroutine({
            serviceImpl.addFavoriteCategory(category)
            receiver.onSuccess()
        })
    }

    override fun removeFavoriteCategory(category: String, receiver: IAsyncCallback) {
        CoroutineHelper.runCoroutine({
            serviceImpl.removeFavoriteCategory(category)
            receiver.onSuccess()
        })
    }

    override fun renameFavoriteCategory(
        newName: String,
        oldName: String,
        receiver: IAsyncCallback
    ) {
        CoroutineHelper.runCoroutine({
            serviceImpl.renameFavoriteCategory(newName, oldName)
            receiver.onSuccess()
        })
    }

    override fun getAvailableCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
        CoroutineHelper.runCoroutine({
            callback.onReceive(
                serviceImpl.availableCategories.toCollection(
                    ArrayList()
                )
            )
        })
    }

    override fun getFavoritesForCategory(
        category: String,
        callback: IAsyncDataCallback<ArrayList<FavoriteItem>>
    ) {
        CoroutineHelper.runCoroutine({
            val result = arrayListOf<FavoriteItem>()
            serviceImpl.getFavoritesForCategory(category)
                .forEach { favoriteItem ->
                    result.add(fromServiceFavoriteItem(favoriteItem))
                }
            callback.onReceive(result)
        })
    }

    override fun getChannelList(): ArrayList<TvChannel> {
        val result = arrayListOf<TvChannel>()
        serviceImpl.channelListFavoritesInterface.forEach { tvChannel ->
            result.add(fromServiceChannel(tvChannel))
        }
        return result
    }

    override fun addFavoriteInfoToChannels() {
        serviceImpl.addFavoriteInfoToChannels()
    }

    override fun clearFavourites() {
        serviceImpl.clearFavourites()

    }

}