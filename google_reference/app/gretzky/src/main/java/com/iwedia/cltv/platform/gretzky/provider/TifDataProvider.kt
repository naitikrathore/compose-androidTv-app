package com.iwedia.cltv.platform.gretzky.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.util.Log
import com.iwedia.cltv.platform.base.content_provider.DataProvider
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import java.lang.Exception

class TifDataProvider(val contentResolver: ContentResolver): DataProvider {

    private val TAG = "TifDataProvider"
    private var channels = ArrayList<TvChannel>()

    fun insertChannel (tvChannel: TvChannel): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ insert channel ${tvChannel.internalId}")

        return try {
            var ret =
               contentResolver.insertChannel(tvChannel)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteChannel(tvChannel: TvChannel): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ delete channel ${tvChannel.internalId}")

        val contentValues = ContentValues()
        val uri = Contract.buildChannelsUri(tvChannel.internalId)
        contentValues.put(Contract.Channels.BROWSABLE_COLUMN, 0)
        contentValues.put(Contract.Channels.DELETED_COLUMN, 1)

        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            tvChannel.isBrowsable = false
            var removedChannel: TvChannel? = null
            /*channels.value.forEach {
                if (compare(it, tvChannel)) {
                    removedChannel = it
                }

            }
            if (removedChannel != null) {
                channels.remove(removedChannel!!)
                getChannelList().remove(removedChannel!!)
            }*/

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun lockChannel(tvChannel: TvChannel, lock: Boolean): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ lock channel ${tvChannel.internalId}")
        // TODO temporary solution for the manifest parental control permissions issue
        /*channels.value.forEach { channel ->
            if (channel.channelId == tvChannelId) {
                channel.isLocked = lock
                return@forEach
            }
        }
        return true*/

        val contentValues = ContentValues()
        val uri = Contract.buildChannelsUri(tvChannel.internalId)
        var locked = if (lock) 1 else 0
        contentValues.put(Contract.Channels.LOCKED_COLUMN, locked)

        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    fun skipChannel(tvChannel: TvChannel, skip: Boolean): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ skip channel ${tvChannel.internalId}")
        val contentValues = ContentValues()
        var isSkipped = if (skip) 1 else 0
        contentValues.put(Contract.Channels.SKIP_COLUMN, isSkipped)

        val uri = Contract.buildChannelsUri(tvChannel.internalId)

        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearBrowsable() {
        val contentValues = ContentValues()

        channels.forEach { channel ->
            contentValues.put(Contract.Channels.BROWSABLE_COLUMN, 1)
            val uri = Contract.buildChannelsUri(channel.internalId)

            try {
                var ret =
                    contentResolver.update(
                        uri,
                        contentValues,
                        null,
                        null
                    )
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchChannelByTriplet(onId: Int, tsId: Int, serviceId: Int): TvChannel? {
        val contentResolver: ContentResolver = contentResolver
        var selection =
            Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN + " = ? and " + Contract.Channels.TRANSPORT_STREAM_ID_COLUMN + " = ? and " + Contract.Channels.SERVICE_ID_COLUMN + " = ?"
        var cursor = contentResolver.query(
            ContentProvider.CHANNELS_URI,
            null,
            selection,
            arrayOf(onId.toString(), tsId.toString(), serviceId.toString()),
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            return createChannelFromCursor(cursor)
        }
        return null
    }

}