package com.iwedia.cltv.platform.base.content_provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList

/**
 * Tif channel data provider
 *
 * Provides channel list from tv.db
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.S)
open class TifChannelDataProvider constructor(var context: Context) : ChannelDataProviderInterface {
    private val TAG = javaClass.simpleName
    private var channelList = arrayListOf<TvChannel>()
    private val CHANNEL_UPDATE_TIMEOUT = 3000L
    private var channelUpdateTimer: CountDownTimer? = null
    private lateinit var channelListObserver: ContentObserver
    //used to prevent loading of channel list again when only channel locked status changes in db
    private var channelLockStatusUpdated = false

    init {
        channelListObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Channel data updated")
                if (channelLockStatusUpdated) {
                    channelLockStatusUpdated = false
                    return
                }
                startChannelUpdateTimer()
            }
        }

        context.contentResolver.registerContentObserver(
            TvContract.Channels.CONTENT_URI,
            true,
            channelListObserver
        )
    }

    override fun dispose() {
        stopChannelUpdateTimer()
        if (channelListObserver != null) {
            context.contentResolver.unregisterContentObserver(channelListObserver)
        }
    }

    override fun enableLcn(enableLcn: Boolean) {
        TODO("Not yet implemented")
    }

    override fun isLcnEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPlatformData(cursor: Cursor): Any? {
        return null
    }

    protected fun clearChannelList() {
        channelList.clear()
    }

    protected fun addChannel(tvChannel: TvChannel) {
        if (!channelList.contains(tvChannel)) {
                channelList.add(tvChannel)
        }
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    open fun loadChannels() {
        CoroutineHelper.runCoroutine({
            var lChannelList = arrayListOf<TvChannel>()
            lChannelList.clear()
            val contentResolver: ContentResolver = context.contentResolver
            var inputList = getInputIds(context)
            if (inputList!!.isNotEmpty()) {
                for (input in inputList) {
                    //in case other hardware works with other inputs that contain different name, those inputs need to be added here so that the channels are added in cltv app
                    if (input.contains("com.google.android.tv.dtvinput") || input.contains("mediatek") || input.contains("iwedia") ||
                        input.contains("realtek")) {
                        var cursor = contentResolver.query(
                            TvContract.buildChannelsUriForInput(input),
                            null,
                            null,
                            null,
                            null
                        )

                        if (cursor!!.count > 0) {
                            cursor.moveToFirst()
                            do {
                                try {
                                    var tvChannel =
                                        createChannelFromCursor(context, cursor, lChannelList.size)
                                    tvChannel.platformSpecific = getPlatformData(cursor)
                                    if (tvChannel.inputId.contains("anoki", ignoreCase = true)) {
                                        tvChannel.ordinalNumber =
                                            cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4))
                                    }
                                    lChannelList.add(tvChannel)
                                } catch(e : Exception) {
                                    e.printStackTrace()
                                }
                            } while (cursor.moveToNext())
                        }
                        cursor!!.close()
                    }
                }
                updateChannelList(lChannelList)
                InformationBus.informationBusEventListener.submitEvent(Events.CHANNELS_LOADED)
            }
        })
    }

    @Synchronized
    fun updateChannelList(updatedList : ArrayList<TvChannel>) {
        channelList.clear()
        channelList.addAll(updatedList)
    }

    @Synchronized
    override fun getChannelList(): ArrayList<TvChannel> {
        var list = arrayListOf<TvChannel>()
        if (channelList.isNotEmpty()) {
            list.addAll(getSortedChannelList(channelList))
        }
        return list
    }

    override fun getSortedChannelList(oldChannelList: MutableList<TvChannel>): java.util.ArrayList<TvChannel> {
        val sortedChannelList = CopyOnWriteArrayList(oldChannelList)
        try {
            sortedChannelList.sortBy { it.displayNumber }
            val isNumeric = isNumeric(ArrayList(sortedChannelList))

            /*
             * Fast channels are sorted by 'ordinalNumber'.
             * Broadcast channels are sorted by 'displayNumber'.
             * */
            return ArrayList(sortedChannelList.sortedWith(compareBy<TvChannel> {
                if (it.isFastChannel()) it.ordinalNumber
                else -1
            }.thenBy {
                if (isNumeric) {
                    if (!it.isFastChannel()) it.displayNumber.toInt()
                    else -1
                } else {
                    if (!it.isFastChannel()) it.displayNumber
                    else ""
                }
            }))
        } catch (ex: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onReceive: display number missing")
        }
        return ArrayList(sortedChannelList)
    }

    override fun deleteChannel(tvChannel: TvChannel): Boolean {
        val contentResolver: ContentResolver = context.contentResolver

        var uri = TvContract.buildChannelUri(tvChannel.channelId)
        return try {
            var ret =
                contentResolver.delete(
                    uri,
                    null,
                    null
                )
            if (ret > 0) {
                tvChannel.isBrowsable = false
                true
            }  else false
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun lockUnlockChannel(tvChannel: TvChannel, lock: Boolean): Boolean {
        println("getLockedChannelList "+ "lock or unlock channel ${tvChannel.name} | ${lock}")
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        var uri = TvContract.buildChannelUri(tvChannel.channelId)
        var locked = if (lock) 1 else 0
        contentValues.put(TvContract.Channels.COLUMN_LOCKED, locked)

        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
            println("getLockedChannelList " + "contentResolver update $ret")
            if (ret > 0) {
                channelLockStatusUpdated = true
                tvChannel.isLocked = lock
                println("getLockedChannelList " + "contentResolver update tvChannel ${tvChannel.name}")
                true
            }  else false
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
        return true
    }

    override fun skipUnskipChannel(tvChannel: TvChannel, skip: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Stop channel data update timer if it is already started
     */
    private fun stopChannelUpdateTimer() {
        if (channelUpdateTimer != null) {
            channelUpdateTimer!!.cancel()
            channelUpdateTimer = null
        }
    }

    /**
     * Start channel data update timer
     */
    private fun startChannelUpdateTimer() {
        //Cancel timer if it's already started
        stopChannelUpdateTimer()

        //Start new count down timer
        channelUpdateTimer = object :
            CountDownTimer(
                CHANNEL_UPDATE_TIMEOUT,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                // Wait for event loading
                var eventListener = ChannelLoadedEventListener(object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                    }

                    override fun onSuccess() {
                        InformationBus.informationBusEventListener.submitEvent(Events.CHANNEL_LIST_UPDATED)
                    }
                })
                loadChannels()
            }
        }
        channelUpdateTimer!!.start()
    }

    inner class ChannelLoadedEventListener(var callback: IAsyncCallback?){

        private var eventListener: Any?= null
        init {
            InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.CHANNELS_LOADED), callback = {
                eventListener = it
            }, onEventReceived = {
                callback?.onSuccess()
                InformationBus.informationBusEventListener.unregisterEventListener(eventListener!!)
            })
        }
    }
}