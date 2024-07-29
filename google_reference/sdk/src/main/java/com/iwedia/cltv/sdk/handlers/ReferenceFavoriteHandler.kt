package com.iwedia.cltv.sdk.handlers

import android.util.Log
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.TifDataProvider
import com.iwedia.cltv.sdk.entities.ReferenceFavoriteItem
import core_entities.Error
import data_type.GList
import handlers.DataProvider
import handlers.FavoritesHandler
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus

/**
 * Reference favorite handler
 *
 * @author Dejan Nadj
 */
class ReferenceFavoriteHandler(dataProvider: DataProvider<*>): FavoritesHandler<ReferenceFavoriteItem>(dataProvider) {

    /**
     * Favorites categories list
     */
    private val categoriesList = ArrayList<String>()

    /**
     * Update favorite prefs data if it is needed after language change
     */
    private fun updateFavoritePrefs() {
        var favString = ReferenceSdk.prefsHandler?.getValue(FAVORITE_STRING_TAG, "")!!

        if (favString != "" && favString != ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource()) {
            favorites = ReferenceSdk.prefsHandler?.getValue(FAVORITES_PREFS_TAG, "")!!
            val temp = favorites.toString().split(",")
            var favList = ""
            temp.forEach { item->
                var item = item
                if (item.equals("$favString 1")) {
                    item = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 1"
                    favList+= "$item"
                } else if (item.equals("$favString 2")) {
                    item = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 2"
                    favList+= ",$item"
                } else if (item.equals("$favString 3")) {
                    item = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 3"
                    favList+= ",$item"
                } else if (item.equals("$favString 4")) {
                    item = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 4"
                    favList+= ",$item"
                } else if (item.equals("$favString 5")) {
                    item = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 5"
                    favList+= ",$item"
                } else if (item.isNotEmpty()){
                    favList+= ",$item"
                }
            }
            ReferenceSdk.prefsHandler?.storeValue(FAVORITES_PREFS_TAG, favList)
        }
    }

    init {
        // Load favorites categories from prefs
        updateFavoritePrefs()
        favorites = ReferenceSdk.prefsHandler?.getValue(FAVORITES_PREFS_TAG, "")!!
        favoritesOriginal = "$FAVORITE_1,$FAVORITE_2,$FAVORITE_3,$FAVORITE_4,$FAVORITE_5"
        if (favorites == "") {
            favorites = "$FAVORITE_1,$FAVORITE_2,$FAVORITE_3,$FAVORITE_4,$FAVORITE_5"
            ReferenceSdk.prefsHandler?.storeValue(FAVORITES_PREFS_TAG, favorites)
        }
        val temp = favorites.toString().split(",")
        temp.forEach { item ->
            if (item != " " && item != "")
                categoriesList.add(item)
        }
    }

    /**
     * Get favorites categories
     */
    fun getCategories(): ArrayList<String> {
        return categoriesList
    }

    /**
     * Add favorites category
     *
     * @param favoriteCategory category name
     * @param receiver  callback
     */
    fun addFavoriteCategory(favoriteCategory: String, receiver: AsyncReceiver) {
        CoroutineHelper.runCoroutine({
            if (categoriesList.contains(favoriteCategory)) {

                receiver.onFailed(Error(100, "Favorite category $favoriteCategory already exist!"))
            } else {
                var favorites = ReferenceSdk.prefsHandler?.getValue(FAVORITES_PREFS_TAG, "")
                val newFavorites = favorites.toString() + "," + favoriteCategory
                ReferenceSdk.prefsHandler?.storeValue(FAVORITES_PREFS_TAG, newFavorites)
                categoriesList.add(favoriteCategory)
                Log.d(Constants.LogTag.CLTV_TAG + "UPDATE", "addFavoriteCategory: ")
                InformationBus.submitEvent(Event(ReferenceEvents.FAVORITE_LIST_CATEGORY_UPDATED))
                receiver.onSuccess()
            }
        })
    }

    /**
     * Remove favorite category
     *
     * @param favoriteCategory category name
     * @param receiver callback
     */
    fun removeFavoriteCategory(favoriteCategory: String, receiver: AsyncReceiver) {
        CoroutineHelper.runCoroutine({
            if (categoriesList.contains(favoriteCategory)) {
                (dataProvider as TifDataProvider).removeFavoriteCategory(favoriteCategory, object : AsyncReceiver{
                    override fun onSuccess() {
                        var favorites = ""
                        categoriesList.remove(favoriteCategory)
                        if (categoriesList.isNotEmpty()) {
                            favorites = categoriesList[0]
                            for(index in 1 until categoriesList.size) {
                                favorites += "," + categoriesList[index]
                            }
                        }
                        ReferenceSdk.prefsHandler?.storeValue(FAVORITES_PREFS_TAG, favorites)
                        receiver.onSuccess()
                        InformationBus.submitEvent(Event(ReferenceEvents.FAVORITE_LIST_CATEGORY_UPDATED))
                    }

                    override fun onFailed(error: Error?) {
                        receiver.onFailed(error)
                    }
                })
            } else {
                receiver.onFailed(Error(100, "Favorite category $favoriteCategory does not exist!"))
            }
        })
    }

    /**
     * Rename favorite category
     *
     * @param newName   new category name
     * @param oldName   old category name
     * @param receiver callback
     */
    fun renameFavoriteCategory(newName: String, oldName: String, receiver: AsyncReceiver) {
        CoroutineHelper.runCoroutine({
            if (categoriesList.contains(newName)) {
                receiver.onFailed(Error(100, "Favorite category $newName already exist!"))
                return@runCoroutine
            } else if (categoriesList.contains(oldName)) {
                (dataProvider as TifDataProvider).renameFavoriteCategory(newName, oldName, object : AsyncReceiver{
                    override fun onSuccess() {
                        val index = categoriesList.indexOf(oldName)
                        categoriesList[index] = newName
                        var favorites = ""
                        favorites = categoriesList[0]
                        for(index in 1 until categoriesList.size) {
                            favorites += "," + categoriesList[index]
                        }
                        ReferenceSdk.prefsHandler?.storeValue(FAVORITES_PREFS_TAG, favorites)
                        InformationBus.submitEvent(Event(ReferenceEvents.FAVORITE_LIST_CATEGORY_UPDATED))
                        receiver.onSuccess()
                    }

                    override fun onFailed(error: Error?) {
                        receiver.onFailed(error)
                    }
                })
            } else {
                receiver.onFailed(Error(100, "Favorite category with name $oldName does not exist!"))
            }
        })
    }
    override fun isInFavorites(
        favoriteItem: ReferenceFavoriteItem,
        callback: AsyncDataReceiver<Boolean?>?
    ) {
        dataProvider!!.getDataAsync(DataProvider.DataType.FAVORITE, object : AsyncDataReceiver<GList<ReferenceFavoriteItem>> {
            override fun onReceive(data: GList<ReferenceFavoriteItem>) {
                var isInFavorites = false
                data.value.forEach { item ->
                    if (item.id == favoriteItem.id) {
                        isInFavorites = true
                    }
                }
                callback?.onReceive(isInFavorites)
            }

            override fun onFailed(error: Error?) {
                callback?.onFailed(error)
            }
        })
    }

    /**
     * Update favorite item
     *
     * @param favoriteItem favorite item to be updated
     * @param callback callback instance
     */
    fun updateFavoriteItem(favoriteItem: ReferenceFavoriteItem, callback: AsyncReceiver) {
        if (favoriteItem.favListIds.isEmpty()) {
            dataProvider!!.removeDataItemAsync(DataProvider.DataType.FAVORITE, favoriteItem, object : AsyncReceiver {
                override fun onSuccess() {
                    callback?.onSuccess()
                }

                override fun onFailed(error: Error?) {
                    callback?.onFailed(error)
                }
            })
        } else {
            dataProvider!!.addDataItemAsync(
                DataProvider.DataType.FAVORITE,
                favoriteItem,
                object : AsyncReceiver {
                    override fun onSuccess() {
                        callback?.onSuccess()
                    }

                    override fun onFailed(error: Error?) {
                        callback?.onFailed(error)
                    }
                })
        }
    }

    /**
     * Get available favorite categories
     *
     * @param callback callback instance
     */
    fun getAvailableCategories(callback: AsyncDataReceiver<ArrayList<String>>) {
        CoroutineHelper.runCoroutine({
            var list = ArrayList<String>()
            list.addAll(categoriesList)
            callback.onReceive(list)
        })
    }

    /**
     * Get favorite items by category
     *
     * @param category category
     * @param callback callback instance
     */
    fun getFavoritesForCategory(category: String, callback: AsyncDataReceiver<ArrayList<ReferenceFavoriteItem>>) {
        dataProvider!!.getDataAsync(DataProvider.DataType.FAVORITE, object : AsyncDataReceiver<GList<ReferenceFavoriteItem>> {
            override fun onReceive(data: GList<ReferenceFavoriteItem>) {
                var retList = ArrayList<ReferenceFavoriteItem>()
                data.value.forEach { item ->
                    if (item.tvChannel.favListIds.contains(category)) {
                        retList.add(item)
                    }
                }
                callback?.onReceive(retList)
            }

            override fun onFailed(error: Error?) {
                callback?.onFailed(error)
            }
        })

    }

    //todo
    companion object {
        lateinit var favorites: Any
        lateinit var favoritesOriginal: Any
        const val FAVORITES_PREFS_TAG = "favorites"
        const val FAVORITE_STRING_TAG =  "favorite"
        var FAVORITE_1 = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 1"
        var FAVORITE_2 = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 2"
        var FAVORITE_3 = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 3"
        var FAVORITE_4 = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 4"
        var FAVORITE_5 = ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 5"
    }
}