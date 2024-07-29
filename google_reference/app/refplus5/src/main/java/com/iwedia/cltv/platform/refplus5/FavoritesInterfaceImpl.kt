package com.iwedia.cltv.platform.refplus5

import android.content.Context
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.base.FavoritesInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.favorite.FavoriteItem


internal class FavoritesInterfaceImpl(
    applicationContext: Context,
    channelDataProvider: ChannelDataProviderInterface,
    utilsInterface: UtilsInterface
) :
    FavoritesInterfaceBaseImpl(applicationContext, channelDataProvider, utilsInterface) {

    override fun getFavoriteItems(callback: IAsyncDataCallback<List<FavoriteItem>>) {
        dataProvider.getFavorites(
            object : IAsyncDataCallback<ArrayList<FavoriteItem>> {
                override fun onReceive(data: ArrayList<FavoriteItem>) {
                    var retList = ArrayList<FavoriteItem>()
                    data.forEach { item ->
                        retList.add(item)
                    }
                    callback.onReceive(retList)
                }

                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }
            })
    }

    override fun addFavoriteInfoToChannels() {
        var allChannelList = channelDataProvider.getChannelList()

        getFavoriteItems(object : IAsyncDataCallback<List<FavoriteItem>> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: List<FavoriteItem>) {
                data.forEach { item ->
                    allChannelList.forEach { tvChannel ->
                        if (tvChannel.id == item.id) {
                            tvChannel.favListIds = item.tvChannel.favListIds
                            return@forEach
                        }
                    }
                }
            }
        })
    }
}