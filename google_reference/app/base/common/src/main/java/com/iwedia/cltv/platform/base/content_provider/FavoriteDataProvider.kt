package com.iwedia.cltv.platform.base.content_provider

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.FavoriteDataProviderInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.favorite.FavoriteItem


/**
 * Favorite data provider
 *
 */
open class FavoriteDataProvider constructor(
    var context: Context,
    var channelDataProvider: ChannelDataProviderInterface
) : FavoriteDataProviderInterface {
    private val TAG = javaClass.simpleName

    init {
        getFavoriteItems(object : IAsyncDataCallback<List<FavoriteItem>> {
            override fun onReceive(data: List<FavoriteItem>) {
            }

            override fun onFailed(error: Error) {
            }
        })
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun getFavorites(callback: IAsyncDataCallback<ArrayList<FavoriteItem>>) {
        val contentResolver: ContentResolver = context.contentResolver
        val retList = ArrayList<FavoriteItem>()
        val cursor = contentResolver.query(
            ContentProvider.FAVORITES_URI, null, null, null, null
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val item = fromFavoriteCursor(cursor, channelDataProvider.getChannelList())
                if (item != null) {
                    retList.add(item)
                }
            }
            val list = ArrayList<FavoriteItem>()
            for (favItem in retList) {
                list.add(favItem)
            }
            callback.onReceive(list)
            cursor.close()
        } else {
            callback.onFailed(Error("Favorite cursor is null!"))
        }
    }

    override fun addToFavorites(
        favoriteItem: FavoriteItem,
        callback: IAsyncCallback
    ) {
        CoroutineHelper.runCoroutine({
            val contentResolver: ContentResolver = context.contentResolver
            val contentValues = toFavContentValues(favoriteItem)
            val selection =
                "${Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${Contract.Favorites.SERVICE_ID_COLUMN} == ?"
            val selectionArgs = arrayOf(
                favoriteItem.tvChannel.onId.toString(),
                favoriteItem.tvChannel.tsId.toString(),
                favoriteItem.tvChannel.serviceId.toString()
            )
            var cursor = contentResolver.query(
                ContentProvider.FAVORITES_URI,
                null,
                selection,
                selectionArgs,
                null
            )
            try {
                if (cursor!!.count > 0) {
                    contentResolver.update(
                        ContentProvider.FAVORITES_URI,
                        contentValues,
                        selection,
                        selectionArgs
                    )
                    if (favoriteItem.tvChannel != null) {
                        favoriteItem.tvChannel.favListIds.clear()
                        favoriteItem.tvChannel.favListIds.addAll(favoriteItem.favListIds)
                    }
                    callback.onSuccess()
                } else {
                    var res = contentResolver.insert(
                        ContentProvider.FAVORITES_URI,
                        contentValues
                    )
                    if (res != null) {
                        if (favoriteItem.tvChannel != null) {
                            favoriteItem.tvChannel.favListIds.clear()
                            favoriteItem.tvChannel.favListIds.addAll(favoriteItem.favListIds)
                        }
                        callback.onSuccess()
                    } else {
                        callback.onFailed(Error("Failed to add item to the favorites"))
                    }
                }
                cursor.close()
            } catch (e: java.lang.Exception) {
                callback.onFailed(Error("Failed to add item to the favorites"))
                e.printStackTrace()
            }
        })
    }

    override fun deleteFromFavorites(
        favoriteItem: FavoriteItem,
        callback: IAsyncCallback
    ) {
        CoroutineHelper.runCoroutine({
            val contentResolver: ContentResolver = context.contentResolver
            val selection =
                "${Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${Contract.Favorites.SERVICE_ID_COLUMN} == ?"
            val selectionArgs = arrayOf(
                favoriteItem.tvChannel.onId.toString(),
                favoriteItem.tvChannel.tsId.toString(),
                favoriteItem.tvChannel.serviceId.toString()
            )
            val res = contentResolver.delete(
                ContentProvider.FAVORITES_URI,
                selection,
                selectionArgs
            )
            if (res == 1) {
                favoriteItem.tvChannel.favListIds.clear()
                callback.onSuccess()
            } else {
                callback.onFailed(Error("Failed to remove item from the favorites"))
            }
        })
    }

    override fun removeFavoriteCategory(category: String, callback: IAsyncCallback) {
        val contentResolver: ContentResolver = context.contentResolver
        val selection =
            "${Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${Contract.Favorites.SERVICE_ID_COLUMN} == ?"
        getFavoriteItems(object : IAsyncDataCallback<List<FavoriteItem>> {
            override fun onReceive(data: List<FavoriteItem>) {
                data.forEach { favoriteItem ->
                    if (favoriteItem.favListIds.contains(category)) {
                        val selectionArgs = arrayOf(
                            favoriteItem.tvChannel.onId.toString(),
                            favoriteItem.tvChannel.tsId.toString(),
                            favoriteItem.tvChannel.serviceId.toString()
                        )
                        var cursor = contentResolver.query(
                            ContentProvider.FAVORITES_URI,
                            null,
                            selection,
                            selectionArgs,
                            null
                        )
                        if (cursor!!.count > 0) {
                            favoriteItem.favListIds.remove(category)
                            if (favoriteItem.favListIds.isEmpty()) {
                                //remove the entry, as no favorite category exist for this channel
                                val res = contentResolver.delete(
                                    ContentProvider.FAVORITES_URI,
                                    selection,
                                    selectionArgs
                                )
                            } else {
                                val contentValues = toFavContentValues(favoriteItem)
                                contentResolver.update(
                                    ContentProvider.FAVORITES_URI,
                                    contentValues,
                                    selection,
                                    selectionArgs
                                )
                            }
                        }
                    }
                }
                callback.onSuccess()
            }

            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }
        })
    }

    fun getFavoriteItems(callback: IAsyncDataCallback<List<FavoriteItem>>) {
        CoroutineHelper.runCoroutine({
            val contentResolver: ContentResolver = context.contentResolver
            var retList = java.util.ArrayList<FavoriteItem>()

            var cursor = contentResolver.query(
                ContentProvider.FAVORITES_URI,
                null,
                null,
                null,
                null
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    var item = fromFavoriteCursor(cursor, channelDataProvider.getChannelList())
                    if (item != null) {
                        retList.add(item)
                    }
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ favorites cursor size " + cursor.count)
                callback.onReceive(retList)
                cursor.close()
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "favorites cursor is null")
                callback.onFailed(Error("Favorite cursor is null!"))
            }
        })
    }

    override fun renameFavoriteCategory(
        newName: String,
        oldName: String,
        callback: IAsyncCallback
    ) {
        val contentResolver: ContentResolver = context.contentResolver
        val selection =
            "${Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${Contract.Favorites.SERVICE_ID_COLUMN} == ?"
        getFavoriteItems(object : IAsyncDataCallback<List<FavoriteItem>> {
            override fun onReceive(data: List<FavoriteItem>) {
                data.forEach { favoriteItem ->
                    if (favoriteItem.favListIds.contains(oldName)) {
                        val selectionArgs = arrayOf(
                            favoriteItem.tvChannel.onId.toString(),
                            favoriteItem.tvChannel.tsId.toString(),
                            favoriteItem.tvChannel.serviceId.toString()
                        )
                        var cursor = contentResolver.query(
                            ContentProvider.FAVORITES_URI,
                            null,
                            selection,
                            selectionArgs,
                            null
                        )
                        if (cursor!!.count > 0) {
                            val index = favoriteItem.favListIds.indexOf(oldName)
                            favoriteItem.favListIds[index] = newName
                            val contentValues = toFavContentValues(favoriteItem)
                            contentResolver.update(
                                ContentProvider.FAVORITES_URI,
                                contentValues,
                                selection,
                                selectionArgs
                            )
                        }
                    }
                }
                callback.onSuccess()
            }

            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }
        })
    }

    fun updateFavChannelData(tvChannel: TvChannel) {
        CoroutineHelper.runCoroutine({
            val contentResolver: ContentResolver = context.contentResolver
            val contentValues = ContentValues()
            contentValues.put(
                Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN,
                tvChannel.onId
            )
            contentValues.put(
                Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN,
                tvChannel.tsId
            )
            contentValues.put(Contract.Favorites.SERVICE_ID_COLUMN, tvChannel.serviceId)
//            contentValues.put(ReferenceContract.Favorites.COLUMN_TYPE, FavoriteItemType.TV_CHANNEL)
            var listIds = ""
            for (index in 0 until tvChannel.favListIds.size) {
                val listId = tvChannel.favListIds[index]
                listIds += listId
                if (index != (tvChannel.favListIds.size - 1)) {
                    listIds += ","
                }
            }
            contentValues.put(Contract.Favorites.COLUMN_LIST_IDS, listIds)
            val selection =
                "${Contract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${Contract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${Contract.Favorites.SERVICE_ID_COLUMN} == ?"
            val selectionArgs = arrayOf(
                tvChannel.onId.toString(),
                tvChannel.tsId.toString(),
                tvChannel.serviceId.toString()
            )
            var cursor = contentResolver.query(
                ContentProvider.FAVORITES_URI,
                null,
                selection,
                selectionArgs,
                null
            )
            try {
                if (cursor!!.count > 0) {
                    contentResolver.update(
                        ContentProvider.FAVORITES_URI,
                        contentValues,
                        selection,
                        selectionArgs
                    )
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        })
    }

    override fun getFavChannelList(): ArrayList<TvChannel> {
        return channelDataProvider.getChannelList()
    }

    override fun clearFavourites(){
        CoroutineHelper.runCoroutine({
                context.contentResolver.delete(
                ContentProvider.FAVORITES_URI,
                null,
                null
            )
        })
    }
}