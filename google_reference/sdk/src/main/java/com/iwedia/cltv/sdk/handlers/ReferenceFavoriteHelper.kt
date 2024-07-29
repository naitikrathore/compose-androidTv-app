package com.iwedia.cltv.sdk.handlers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.util.Log

import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.ReferenceFavoriteItem
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import core_entities.Error
import core_entities.FavoriteItem
import data_type.GList
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver

/**
 * Reference favorite helper
 * Executes database operations
 *
 * @author Dejan Nadj
 */
class ReferenceFavoriteHelper constructor(
    var channels: MutableList<ReferenceTvChannel> = mutableListOf()
) {

    private val TAG = "ReferenceFavoriteHelper"
    init {
        //Init fav list ids for channels
        getFavoriteItems(object: AsyncDataReceiver<List<ReferenceFavoriteItem>> {
            override fun onReceive(data: List<ReferenceFavoriteItem>) {
            }

            override fun onFailed(error: Error?) {
            }
        })
    }

    /**
     * Add favorite item
     *
     * @param favoriteItem item to be added
     * @param callback callback instance
     */
    fun addToFavorites(favoriteItem: ReferenceFavoriteItem, callback: AsyncReceiver) {
        CoroutineHelper.runCoroutine( {
            val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
            val contentValues = toContentValues(favoriteItem)
            val selection = "${ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.SERVICE_ID_COLUMN} == ?"
            val selectionArgs = arrayOf(
                favoriteItem.tvChannel.onId.toString(),
                favoriteItem.tvChannel.tsId.toString(),
                favoriteItem.tvChannel.serviceId.toString()
            )
            var cursor = contentResolver.query(
                ReferenceContentProvider.FAVORITES_URI,
                null,
                selection,
                selectionArgs,
                null
            )
            try {
                if (cursor!!.count > 0) {
                    contentResolver.update(
                        ReferenceContentProvider.FAVORITES_URI,
                        contentValues,
                        selection,
                        selectionArgs
                    )
                    callback.onSuccess()
                } else {
                    var res = contentResolver.insert(ReferenceContentProvider.FAVORITES_URI, contentValues)
                    if (res != null) {
                        callback.onSuccess()
                    } else {
                        callback.onFailed(Error(100, "Failed to add item to the favorites"))
                    }
                }
            } catch (e: java.lang.Exception) {
                callback.onFailed(Error(100, "Failed to add item to the favorites"))
                e.printStackTrace()
            }
        })
    }

    /**
     * Updates existing tv channel fav list data
     *
     * @param tvChannel reference tv channel to be updated
     */
    fun updateFavChannelData(tvChannel: ReferenceTvChannel) {
        CoroutineHelper.runCoroutine( {
            val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
            val contentValues = ContentValues()
            contentValues.put(ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN, tvChannel.onId)
            contentValues.put(ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN, tvChannel.tsId)
            contentValues.put(ReferenceContract.Favorites.SERVICE_ID_COLUMN, tvChannel.serviceId)
            contentValues.put(ReferenceContract.Favorites.COLUMN_TYPE, FavoriteItem.FavoriteItemType.TV_CHANNEL)
            var listIds = ""
            for (index in 0 until tvChannel.favListIds.size) {
                val listId = tvChannel.favListIds[index]
                listIds += listId
                if (index != (tvChannel.favListIds.size - 1)) {
                    listIds += ","
                }
            }
            contentValues.put(ReferenceContract.Favorites.COLUMN_LIST_IDS, listIds)
            val selection = "${ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.SERVICE_ID_COLUMN} == ?"
            val selectionArgs = arrayOf(
                tvChannel.onId.toString(),
                tvChannel.tsId.toString(),
                tvChannel.serviceId.toString()
            )
            var cursor = contentResolver.query(
                ReferenceContentProvider.FAVORITES_URI,
                null,
                selection,
                selectionArgs,
                null
            )
            try {
                if (cursor!!.count > 0) {
                    contentResolver.update(
                        ReferenceContentProvider.FAVORITES_URI,
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

    /**
     * Remove favorite item
     *
     * @param favoriteItem item to be removed
     * @param callback callback instance
     */
    fun removeFromFavorites(favoriteItem: ReferenceFavoriteItem, callback: AsyncReceiver) {
        CoroutineHelper.runCoroutine( {
            val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
            val selection = "${ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.SERVICE_ID_COLUMN} == ?"
            val selectionArgs = arrayOf(
                favoriteItem.tvChannel.onId.toString(),
                favoriteItem.tvChannel.tsId.toString(),
                favoriteItem.tvChannel.serviceId.toString()
            )
            val res = contentResolver.delete(ReferenceContentProvider.FAVORITES_URI, selection, selectionArgs)
            if (res == 1) {
                callback.onSuccess()
            } else {
                callback.onFailed(Error(100, "Failed to remove item from the favorites"))
            }
        })
    }

    /**
     * Get favorite items list
     *
     * @param callback callback instance
     */
    fun getFavoriteItems(callback: AsyncDataReceiver<List<ReferenceFavoriteItem>>)  {
        CoroutineHelper.runCoroutine({
            val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
            var retList = java.util.ArrayList<ReferenceFavoriteItem>()

            var cursor = contentResolver.query(
                ReferenceContentProvider.FAVORITES_URI,
                null,
                null,
                null,
                null
            )
            if( cursor != null) {
                while (cursor.moveToNext()) {
                    var item = fromCursor(cursor)
                    if (item != null) {
                        retList.add(item)
                    }
                }
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ favorites cursor size " + cursor.count)
                callback.onReceive(retList)
                cursor.close()
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "favorites cursor is null")
                callback.onFailed(Error(100, "Favorite cursor is null!"))
            }
        })
    }

    /**
     * Clear all favorite items
     */
    fun clearAll() {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        contentResolver.delete(ReferenceContentProvider.FAVORITES_URI, null, null)
    }

    /**
     * Remove favorite category
     *
     * @param favoriteCategory category name
     * @param receiver callback
     */
    fun removeFavoriteCategory(favoriteCategory: String, receiver: AsyncReceiver) {
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        val selection = "${ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.SERVICE_ID_COLUMN} == ?"
        getFavoriteItems(object: AsyncDataReceiver<List<ReferenceFavoriteItem>> {
            override fun onReceive(data: List<ReferenceFavoriteItem>) {
                data.forEach { favoriteItem ->
                    if (favoriteItem.favListIds.contains(favoriteCategory)) {
                        val selectionArgs = arrayOf(
                            favoriteItem.tvChannel.onId.toString(),
                            favoriteItem.tvChannel.tsId.toString(),
                            favoriteItem.tvChannel.serviceId.toString()
                        )
                        var cursor = contentResolver.query(
                            ReferenceContentProvider.FAVORITES_URI,
                            null,
                            selection,
                            selectionArgs,
                            null
                        )
                        if (cursor!!.count > 0) {
                            favoriteItem.favListIds.remove(favoriteCategory)
                            if(favoriteItem.favListIds.isEmpty()) {
                                //remove the entry, as no favorite category exist for this channel
                                val res = contentResolver.delete(ReferenceContentProvider.FAVORITES_URI, selection, selectionArgs)
                            } else {
                                val contentValues = toContentValues(favoriteItem)
                                contentResolver.update(
                                    ReferenceContentProvider.FAVORITES_URI,
                                    contentValues,
                                    selection,
                                    selectionArgs
                                )
                            }
                        }
                    }
                }
                receiver.onSuccess()
            }

            override fun onFailed(error: Error?) {
                receiver.onFailed(error)
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
        val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
        val selection = "${ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN} == ? AND ${ReferenceContract.Favorites.SERVICE_ID_COLUMN} == ?"
        getFavoriteItems(object: AsyncDataReceiver<List<ReferenceFavoriteItem>> {
            override fun onReceive(data: List<ReferenceFavoriteItem>) {
                data.forEach { favoriteItem ->
                    if (favoriteItem.favListIds.contains(oldName)) {
                        val selectionArgs = arrayOf(
                            favoriteItem.tvChannel.onId.toString(),
                            favoriteItem.tvChannel.tsId.toString(),
                            favoriteItem.tvChannel.serviceId.toString()
                        )
                        var cursor = contentResolver.query(
                            ReferenceContentProvider.FAVORITES_URI,
                            null,
                            selection,
                            selectionArgs,
                            null
                        )
                        if (cursor!!.count > 0) {
                            val index = favoriteItem.favListIds.indexOf(oldName)
                            favoriteItem.favListIds[index] = newName
                            val contentValues = toContentValues(favoriteItem)
                            contentResolver.update(
                                ReferenceContentProvider.FAVORITES_URI,
                                contentValues,
                                selection,
                                selectionArgs
                            )
                        }
                    }
                }
                receiver.onSuccess()
            }

            override fun onFailed(error: Error?) {
                receiver.onSuccess()
            }
        })
    }

    private fun toContentValues(referenceFavoriteItem: ReferenceFavoriteItem): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN, referenceFavoriteItem.tvChannel.onId)
        contentValues.put(ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN, referenceFavoriteItem.tvChannel.tsId)
        contentValues.put(ReferenceContract.Favorites.SERVICE_ID_COLUMN, referenceFavoriteItem.tvChannel.serviceId)
        contentValues.put(ReferenceContract.Favorites.COLUMN_TYPE, FavoriteItem.FavoriteItemType.TV_CHANNEL)
        var listIds = ""
        for (index in 0 until referenceFavoriteItem.favListIds.size) {
            val listId = referenceFavoriteItem.favListIds[index]
            listIds += listId
            if (index != (referenceFavoriteItem.favListIds.size - 1)) {
                listIds += ","
            }
        }
        contentValues.put(ReferenceContract.Favorites.COLUMN_LIST_IDS, listIds)
        return contentValues
    }

    @SuppressLint("Range")
    private fun fromCursor(cursor: Cursor): ReferenceFavoriteItem? {
        val onid = cursor.getInt(cursor.getColumnIndex(ReferenceContract.Favorites.ORIGINAL_NETWORK_ID_COLUMN))
        val tsid = cursor.getInt(cursor.getColumnIndex(ReferenceContract.Favorites.TRANSPORT_STREAM_ID_COLUMN))
        val sid = cursor.getInt(cursor.getColumnIndex(ReferenceContract.Favorites.SERVICE_ID_COLUMN))
        val type = cursor.getInt(cursor.getColumnIndex(ReferenceContract.Favorites.COLUMN_TYPE))
        val listIds = cursor.getString(cursor.getColumnIndex(ReferenceContract.Favorites.COLUMN_LIST_IDS))
        var favListIds = ArrayList<String>()
        val tempList = listIds.split(",")
        favListIds.addAll(tempList)
        channels.forEach { tvChannel ->
            if (tvChannel.onId == onid && tvChannel.tsId == tsid && tvChannel.serviceId == sid) {
                tvChannel.favListIds.clear()
                tvChannel.favListIds.addAll(favListIds)
                return ReferenceFavoriteItem(tvChannel, tvChannel.favListIds)
            }
        }
        return null
    }

    /**
     * Update channel list favorite data after language change
     */
    fun updateChannelFavList(fav: String, channels: GList<ReferenceTvChannel>) {
        channels.value.forEach { tvChannel ->
            var newFavList = arrayListOf<String>()
            tvChannel.favListIds.forEach{favItem ->
                if (favItem == "$fav 1") {
                    newFavList.add(ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 1")
                } else if (favItem == "$fav 2") {
                    newFavList.add(ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 2")
                } else if (favItem == "$fav 3") {
                    newFavList.add(ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 3")
                } else if (favItem == "$fav 4") {
                    newFavList.add(ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 4")
                } else if (favItem == "$fav 5") {
                    newFavList.add(ReferenceSdk.sdkListener!!.getFavoriteStringTranslationResource() + " 5")
                } else if (favItem.isNotEmpty()){
                    newFavList.add(favItem)
                }
            }
            tvChannel.favListIds.clear()
            tvChannel.favListIds.addAll(newFavList)
        }
    }
}