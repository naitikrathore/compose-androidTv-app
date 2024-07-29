package com.iwedia.cltv.platform.rtk.provider

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.media.tv.TvContract
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.base.content_provider.getInputIds
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.rtk.util.ChannelsLoadedCallback
import java.util.*


/**
 * Gretzky channel data provider implementation
 *
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.S)
class ChannelDataProvider constructor(context: Context) : TifChannelDataProvider(context) {
    private var receiver: BroadcastReceiver
    private val RTK_SCAN_START = "com.realtek.common.RtkTvCommon.SCAN_STARTED"
    private val RTK_SCAN_FINISH = "com.realtek.common.RtkTvCommon.SCAN_FINISHED"
    private val RTK_SCAN_RESULT = "com.realtek.common.RtkTvCommon.SCAN_RESULT" //with boolean extra->"SCAN_RESULT", true: success, false: failed
    private val RTK_COLUMN_SI_BROWSABLE = "si_browsable"

    private var channelsLoadedCallback: ChannelsLoadedCallback? = null

    init {
        val intentFilter = IntentFilter(RTK_SCAN_FINISH)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (p1?.action == RTK_SCAN_FINISH) {
                    InformationBus.informationBusEventListener.submitEvent(Events.EXIT_APPLICATION_ON_SCAN)
                }
            }
        }
        context.registerReceiver(receiver, intentFilter)
    }

    @SuppressLint("Range")
    override fun getPlatformData(cursor: Cursor): Any? {
        var isSiBrowsable = false
        var platformSpecificData = PlatformSpecificData()
        try {
            if (cursor.getString(cursor.getColumnIndex(RTK_COLUMN_SI_BROWSABLE)) != null) {
                isSiBrowsable = (cursor.getInt(cursor.getColumnIndex(RTK_COLUMN_SI_BROWSABLE)) and 0x01 != 0)
            }
            platformSpecificData.isSiBrowsable = isSiBrowsable
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return platformSpecificData
    }

    override fun getSortedChannelList(oldChannelList: MutableList<TvChannel>): ArrayList<TvChannel> {
        var newChannelList : List<TvChannel> = mutableListOf()

        /*
         * Fast channels are sorted by 'ordinalNumber'.
         * Broadcast channels are sorted by 'displayNumber'.
         * */
        try {
            newChannelList = oldChannelList.sortedWith(compareBy<TvChannel> {
                if (it.isFastChannel()) it.ordinalNumber
                else -1
            }.thenBy {
                if (!it.isFastChannel()) it.displayNumber.replace("-",".").toDouble()
                else -1.0
            })
        }catch (E: IllegalArgumentException){
            println(E)
        }catch (E: Exception){
            println(E)
        }

        return ArrayList(newChannelList)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getChannelList(): ArrayList<TvChannel> {
        val channelList = super.getChannelList().toMutableList()
        return getSortedChannelList(channelList)
    }

    override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
        return tvChannel.isBrowsable
    }

    override fun enableLcn(enableLcn: Boolean) {
    }

    override fun isLcnEnabled(): Boolean {
        return false
    }

    override fun deleteChannel(tvChannel: TvChannel): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues()
        contentValues.put(TvContract.Channels.COLUMN_BROWSABLE, 0)

        var uri = TvContract.buildChannelUri(tvChannel.channelId)
        return try {
            var ret =
                contentResolver.update(
                    uri,
                    contentValues,
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

    override fun dispose() {
        if (receiver != null) context.unregisterReceiver(receiver)
        super.dispose()
    }

    @SuppressLint("Range")
    override fun loadChannels() {
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
                                        com.iwedia.cltv.platform.base.content_provider.createChannelFromCursor(
                                            context,
                                            cursor,
                                            lChannelList.size
                                        )
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

                channelsLoadedCallback?.onChannelsLoaded()
            }
        })
    }

    fun setChannelsLoadedCallback(callback: ChannelsLoadedCallback) {
        this.channelsLoadedCallback = callback
    }
}