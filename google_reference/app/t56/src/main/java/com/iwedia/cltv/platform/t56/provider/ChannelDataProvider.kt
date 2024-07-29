package com.iwedia.cltv.platform.t56.provider

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider
import com.iwedia.cltv.platform.model.TvChannel
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Gretzky channel data provider implementation
 *
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.S)
class ChannelDataProvider constructor(context: Context) : TifChannelDataProvider(context) {
    @SuppressLint("Range")
    override fun getPlatformData(cursor: Cursor): Any? {
        var tunerType = ""
        var platformSpecificData = PlatformSpecificData()
        try {
            if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE)) != null) {
                tunerType = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE))
            }
            platformSpecificData.tifTunerType = tunerType
            //because OTHER type has different INTERNAL_PROVIDER_DATA_COLUMN blob format, thanks MTK
            if(tunerType != TvContract.Channels.TYPE_OTHER) {
                var index =
                    cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)
                if (index != -1) {
                    var blob = cursor.getBlob(index)
                    if (blob != null) {
                            var providerDataValues = String(blob, Charsets.UTF_8).split(",")
                            platformSpecificData.internalServiceListID = providerDataValues[1].toInt()
                            platformSpecificData.internalServiceIndex = providerDataValues[2].toInt()
                            platformSpecificData.internalServiceChannelId = providerDataValues[3].toInt()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return platformSpecificData
    }

    override fun getSortedChannelList(oldChannelList: MutableList<TvChannel>): ArrayList<TvChannel> {
        val channelList = CopyOnWriteArrayList(oldChannelList)
        var newChannelList : List<TvChannel> = mutableListOf()

        channelList.forEach { tvChannel ->
            if(tvChannel != null) {
                if (tvChannel.displayNumber.length == 4) {
                    tvChannel.displayNumber = tvChannel.displayNumber.replace("-", ".0")
                    (newChannelList as MutableList).add(tvChannel)
                } else {
                    tvChannel.displayNumber = tvChannel.displayNumber.replace("-", ".")
                    (newChannelList as MutableList).add(tvChannel)
                }
            }
        }

        /*
         * Fast channels are sorted by 'ordinalNumber'.
         * Broadcast channels are sorted by 'displayNumber'.
         * */
        try {
            newChannelList = newChannelList.sortedWith(compareBy<TvChannel> {
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

        val sortedChannelList = mutableListOf<TvChannel>()
        newChannelList.forEach { tvChannel ->
            tvChannel.displayNumber = tvChannel.displayNumber.replace(".0","-")
            tvChannel.displayNumber = tvChannel.displayNumber.replace(".","-")
            sortedChannelList.add(tvChannel)
        }

        return ArrayList(sortedChannelList)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getChannelList(): ArrayList<TvChannel> {
        val channelList = super.getChannelList().toMutableList()
        return getSortedChannelList(channelList)
    }

    override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
        return tvChannel.isBrowsable
    }
}