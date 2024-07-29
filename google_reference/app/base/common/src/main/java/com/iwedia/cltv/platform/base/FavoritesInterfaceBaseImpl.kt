package com.iwedia.cltv.platform.base

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.base.content_provider.FavoriteDataProvider
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus


open class FavoritesInterfaceBaseImpl constructor(
    val context: Context,
    val channelDataProvider: ChannelDataProviderInterface,
    val utilsInterface: UtilsInterface
) :
    FavoritesInterface {
    private val categoriesList = ArrayList<String>()
    val FAVORITES_PREFS_TAG = "favorites"
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(FAVORITES_PREFS_TAG, Context.MODE_PRIVATE)
    var favorites: Any
    private var favoritesOriginal: Any
    val TAG = javaClass.simpleName
    //todo need to put in strings
//    var favData1 = "Favorite"
    var favData1 = utilsInterface.getStringValue("favorite_list")
    var FAVORITE_1 = "$favData1 1"
    var FAVORITE_2 = "$favData1 2"
    var FAVORITE_3 = "$favData1 3"
    var FAVORITE_4 = "$favData1 4"
    var FAVORITE_5 = "$favData1 5"
    val dataProvider = FavoriteDataProvider(context, channelDataProvider)

    init {
        favorites = sharedPreferences.getString(FAVORITES_PREFS_TAG, "")!!
        favoritesOriginal = "$FAVORITE_1 ,$FAVORITE_2,$FAVORITE_3,$FAVORITE_4,$FAVORITE_5"
        if (favorites == "") {
            favorites = "$FAVORITE_1,$FAVORITE_2,$FAVORITE_3,$FAVORITE_4,$FAVORITE_5"
            sharedPreferences.edit().putString(FAVORITES_PREFS_TAG, favorites.toString()).apply()
        }
        val temp = favorites.toString().split(",")
        temp.forEach { item ->
            if (item != " " && item != "")
                categoriesList.add(item)
        }
    }

    override fun setup() {
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun updateFavoriteItem(item: FavoriteItem, callback: IAsyncCallback) {
        if (item.favListIds.isEmpty()) {
            dataProvider.deleteFromFavorites(
                item,
                object : IAsyncCallback {
                    override fun onSuccess() {
                        callback.onSuccess()
                        InformationBus.informationBusEventListener.submitEvent(Events.FAVORITE_LIST_UPDATED, arrayListOf(item.tvChannel.channelId,
                            item.favListIds))
                    }

                    override fun onFailed(error: Error) {
                        callback.onFailed(error)
                    }

                })
        } else {
            dataProvider.addToFavorites(
                item,
                object : IAsyncCallback {
                    override fun onSuccess() {
                        callback.onSuccess()
                        InformationBus.informationBusEventListener.submitEvent(Events.FAVORITE_LIST_UPDATED,
                            arrayListOf(item.tvChannel.channelId,
                            item.favListIds))
                    }

                    override fun onFailed(error: Error) {
                        callback.onFailed(error)
                    }

                })
        }
    }

    override fun isInFavorites(favoriteItem: FavoriteItem, callback: IAsyncDataCallback<Boolean>) {
        dataProvider.getFavorites(
            object : IAsyncDataCallback<ArrayList<FavoriteItem>> {
                override fun onReceive(data: ArrayList<FavoriteItem>) {
                    var isInFavorites = false
                    data.forEach { item ->
                        if (item.id == favoriteItem.id) {
                            isInFavorites = true
                        }
                    }
                    callback.onReceive(isInFavorites)
                }

                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }
            })
    }

    override fun getFavoriteItems(callback: IAsyncDataCallback<List<FavoriteItem>>) {
        TODO("Not yet implemented")
    }

    override fun getFavoriteListByType(
        type: Int,
        callback: IAsyncDataCallback<List<FavoriteItem>>
    ) {
        TODO("Not yet implemented")
    }

    override fun geFavoriteCategories(): ArrayList<String> {
        return categoriesList
    }

    override fun addFavoriteCategory(category: String, receiver: IAsyncCallback) {
        CoroutineHelper.runCoroutine({
            if (categoriesList.contains(category)) {
                receiver.onFailed(Error("100"))
            } else {

                var favorites = sharedPreferences.getString(FAVORITES_PREFS_TAG, "")
                val newFavorites = favorites.toString() + "," + category
                sharedPreferences.edit().putString(FAVORITES_PREFS_TAG, newFavorites).apply()
                categoriesList.add(category)
                InformationBus.informationBusEventListener?.submitEvent(Events.FAVORITE_LIST_CATEGORY_UPDATED)
                receiver.onSuccess()
            }
        })
    }

    override fun removeFavoriteCategory(category: String, receiver: IAsyncCallback) {
        CoroutineHelper.runCoroutine({
            if (categoriesList.contains(category)) {
                dataProvider.removeFavoriteCategory(
                    category,
                    object : IAsyncCallback {

                        override fun onSuccess() {
                            var favorites = ""
                            categoriesList.remove(category)
                            if (categoriesList.isNotEmpty()) {
                                favorites = categoriesList[0]
                                for (index in 1 until categoriesList.size) {
                                    favorites += "," + categoriesList[index]
                                }
                            }
                            sharedPreferences.edit().putString(FAVORITES_PREFS_TAG, favorites)
                                .apply()
                            receiver.onSuccess()
                            InformationBus.informationBusEventListener?.submitEvent(Events.FAVORITE_LIST_CATEGORY_UPDATED)
                        }

                        override fun onFailed(error: Error) {
                            receiver.onFailed(error)
                        }
                    })
            } else {
                receiver.onFailed(Error("Favorite category $category does not exist!"))
            }
        })
    }

    override fun renameFavoriteCategory(
        newName: String,
        oldName: String,
        receiver: IAsyncCallback
    ) {
        CoroutineHelper.runCoroutine({
            if (categoriesList.contains(newName)) {
                receiver.onFailed(Error("Favorite category $newName already exist!"))
                return@runCoroutine
            } else if (categoriesList.contains(oldName)) {
                dataProvider.renameFavoriteCategory(
                    newName,
                    oldName,
                    object : IAsyncCallback {
                        override fun onSuccess() {
                            val index = categoriesList.indexOf(oldName)
                            categoriesList[index] = newName
                            var favorites = ""
                            favorites = categoriesList[0].toString()
                            for (index in 1 until categoriesList.size) {
                                favorites += "," + categoriesList[index]
                            }
                            sharedPreferences.edit().putString(FAVORITES_PREFS_TAG, favorites)
                                .apply()
                            InformationBus.informationBusEventListener?.submitEvent(Events.FAVORITE_LIST_CATEGORY_UPDATED)
                            receiver.onSuccess()
                        }

                        override fun onFailed(error: Error) {
                            receiver.onFailed(error)
                        }
                    })
            } else {
                receiver.onFailed(Error("Favorite category with name $oldName does not exist!"))
            }
        })
    }

    override fun getAvailableCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
        CoroutineHelper.runCoroutine({
            val list = ArrayList<String>()
            list.addAll(categoriesList)
            callback.onReceive(list)
        })
    }

    override fun getFavoritesForCategory(
        category: String,
        callback: IAsyncDataCallback<ArrayList<FavoriteItem>>
    ) {
        dataProvider.getFavorites(
            object : IAsyncDataCallback<ArrayList<FavoriteItem>> {
                override fun onReceive(data: ArrayList<FavoriteItem>) {
                    var retList = ArrayList<FavoriteItem>()
                    data.forEach { item ->
                        if (item.tvChannel.isBrowsable && item.tvChannel.favListIds.contains(category)) {
                            retList.add(item)
                        }
                    }
                    callback.onReceive(retList)
                }


                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }
            })
    }

    override fun getChannelList(): ArrayList<TvChannel> {
        var channelList: java.util.ArrayList<TvChannel> = arrayListOf()
        var oldChannelList = dataProvider.getFavChannelList()
        try {
            oldChannelList.forEach { channel ->
                if(channel.isBrowsable || channel.inputId.contains("iwedia") || channel.inputId.contains("sampletvinput")){
                    channelList.add(channel)
                }
            }
        }catch (E: java.util.ConcurrentModificationException){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelList: ${E.printStackTrace()}")
        }
      return  channelList
    }

    override fun addFavoriteInfoToChannels() {
    }

    override fun clearFavourites() {
        dataProvider.clearFavourites()
    }

}