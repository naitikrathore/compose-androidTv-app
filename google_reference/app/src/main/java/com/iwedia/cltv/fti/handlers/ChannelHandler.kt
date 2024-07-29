package com.iwedia.cltv.fti.handlers

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvInputManager
import android.net.Uri
import android.util.Log
import com.iwedia.cltv.fti.data.Channel
import com.iwedia.cltv.fti.data.ChannelsContract
import com.iwedia.cltv.platform.model.Constants
import org.json.JSONException
import org.json.JSONObject


class ChannelHandler(mContext: Context) {

    private var instance: ChannelHandler? = null
    private val TAG = "ChannelHandler"
    private val CHANNELS_URI: Uri =
        Uri.parse("content://com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider/channels")

    var mChannels: ArrayList<Channel>? = null

    private var mContext: Context? = null
    private var mContentResolver: ContentResolver? = null
    private var mTvInputManager: TvInputManager? = null
    private val mDuplicateChannels: List<Channel> = ArrayList<Channel>()
    val channelsContract: ChannelsContract = ChannelsContract()


    init {
        mTvInputManager = mContext!!.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager?
        mContentResolver = mContext!!.getContentResolver()
        mChannels = getListOfChannelsInternal()
    }

    fun getListOfChannels(): List<Channel> {
        return mChannels!!
    }

    fun getMapOfDuplicateChannels(channels: List<Channel>): HashMap<Int, MutableList<Channel>>? {
        val duplicatesMap: HashMap<Int, MutableList<Channel>> = HashMap()
        for (c in channels) {
            val jsonConfig = c.getInternalProviderData()
            try {
                val jobj = JSONObject(jsonConfig)
                if (jobj.has("original-lcn")) {
                    val initialLcn = jobj.getInt("original-lcn")
                    if(initialLcn != 0){
                        if (!duplicatesMap.containsKey(initialLcn)) {
                            duplicatesMap[initialLcn] = ArrayList()
                        }
                        duplicatesMap[initialLcn]!!.add(c)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        val keys: MutableList<Int> = ArrayList()

        duplicatesMap.entries.forEach { e ->
            if(e.value.size == 1){
                keys.add(e.key)
            }
        }
        for (key in keys) {
            duplicatesMap.remove(key)
        }
        return duplicatesMap
    }

    /**
     * Returns the current list of channels your app provides.
     *
     * @return List of channels.
     */
    private fun getListOfChannelsInternal(): ArrayList<Channel> {
        val channels: MutableList<Channel> = ArrayList()
        // TvProvider returns programs in chronological order by default.
        var cursor: Cursor? = null
        val channel: Channel = Channel()
        try {
            cursor = mContentResolver!!.query(CHANNELS_URI, channel.PROJECTION, null, null, null)
            if (cursor == null || cursor.getCount() === 0) {
//                val a: ArrayList<Channel>
                return ArrayList(channels)
            }
            while (cursor.moveToNext()) {
                val channel: Channel? = channel.fromCursor(cursor)
                if (channel!!.isBrowsable()) {
                    channels.add(channel)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get channels", e)
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
        return ArrayList(channels)
    }

    fun updateChannelList() {
        mChannels = getListOfChannelsInternal()
    }

    fun updateSkipStatus(channelId: Long, status: Int) {
        val values = ContentValues()
        val selection: String = channelsContract.ID.toString() + "=" + channelId
        values.put(channelsContract.SKIP_COLUMN, status)
        mContentResolver!!.update(CHANNELS_URI, values, selection, null)
        for (i in 0 until mChannels!!.size) {
            if (channelId == mChannels!![i].getId()) {
                var skipped = false
                skipped = if (status == 1) {
                    true
                } else {
                    false
                }
                mChannels!![i].setSkip(skipped)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "New skip value: $status")
                Log.d(Constants.LogTag.CLTV_TAG + TAG, mChannels!![i].toString())
                break
            }
        }
    }

    fun deleteChannel(channel: Channel, status: Int): Int {
        val values = ContentValues()
        val selection: String = channelsContract.ID.toString() + "=" + channel.getId()
        values.put(channelsContract.BROWSABLE_COLUMN, status)
        val result = mContentResolver!!.update(CHANNELS_URI, values, selection, null)
        for (i in 0 until mChannels!!.size) {
            if (channel.getId() === mChannels!![i].getId()) {
                var browsable = false
                browsable = if (status == 1) {
                    true
                } else {
                    false
                }
                mChannels!![i].setBrowsable(browsable)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "New browsable value: $status")
                Log.d(Constants.LogTag.CLTV_TAG + TAG, mChannels!![i].toString())
                mChannels!!.removeAt(i)
                break
            }
        }
        return result
    }

    fun deleteAllChannels() {
        val values = ContentValues()
        values.put(channelsContract.BROWSABLE_COLUMN, 0)
        val where: String = channelsContract.BROWSABLE_COLUMN.toString() + " =?"
        val args = arrayOf(
            "1"
        )
        val result = mContentResolver!!.update(CHANNELS_URI, values, where, args)
        mChannels!!.removeAll(mChannels!!)
    }

    fun renameChannel(channelId: Long, newName: String?) {
        val values = ContentValues()
        val selection: String = channelsContract.ID.toString() + "=" + channelId
        values.put(channelsContract.REFERENCE_NAME_COLUMN, newName)
        mContentResolver!!.update(CHANNELS_URI, values, selection, null)
        for (i in 0 until mChannels!!.size) {
            if (channelId == mChannels!![i].getId()) {
                mChannels!![i].setDisplayName(newName)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, mChannels!![i].toString())
                break
            }
        }
    }

}