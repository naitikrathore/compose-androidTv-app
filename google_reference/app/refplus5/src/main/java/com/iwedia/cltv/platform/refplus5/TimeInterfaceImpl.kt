package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.media.tv.TvContract
import android.util.Log
import com.iwedia.cltv.platform.base.TimeInterfaceBaseImpl
import com.iwedia.cltv.platform.model.TvChannel
import com.mediatek.dtv.tvinput.client.broadcastclock.TvBroadcastClock
import com.mediatek.dtv.tvinput.client.scan.Constants

class TimeInterfaceImpl(private val context: Context): TimeInterfaceBaseImpl() {
    private val TAG = javaClass.simpleName
    private val mBroadcastTimeMap = mutableMapOf<String, TvBroadcastClock>()
    private val ERROR_NOT_OBTAINED = com.mediatek.dtv.tvinput.framework.tifextapi.common.broadcastclock.Constants.ERROR_NOT_OBTAINED
    private var time: Long = ERROR_NOT_OBTAINED
    private var lastObtainedTime: Long = 0L
    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return if ((tvChannel.inputId.lowercase().contains("iwedia"))) {
            super.getCurrentTime()
        } else {
            try {
                getTvBroadcastClock(context, tvChannel.type)?.let {
                    if (lastObtainedTime + 1000 < System.currentTimeMillis()) {
                        lastObtainedTime = System.currentTimeMillis()
                        time = it.getUtcTime() ?: ERROR_NOT_OBTAINED
                    }
                }
                if (time == ERROR_NOT_OBTAINED) {
                    time = super.getCurrentTime()
                }
            } catch (ex: Exception) {
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getCurrentTime: ex : ${ex.message}")
            }

            return time
        }
    }

    private fun getTvBroadcastClock(context: Context, broadcastType: String?): TvBroadcastClock? {
        val type = getBroadcastType(broadcastType)

        if (type >= 0) {
            if (!mBroadcastTimeMap.containsKey(broadcastType!!)) {
                val tvBroadcastClock = TvBroadcastClock(context.applicationContext, type)
                mBroadcastTimeMap[broadcastType] = tvBroadcastClock
                return tvBroadcastClock
            }
            return mBroadcastTimeMap[broadcastType]
        }
        return null
    }

    private fun getBroadcastType(broadcastType: String?): Int {
        return when (broadcastType) {
            TvContract.Channels.TYPE_DVB_T,
            TvContract.Channels.TYPE_DVB_T2 -> { Constants.TYPE_DVB_T }
            TvContract.Channels.TYPE_DVB_C,
            TvContract.Channels.TYPE_DVB_C2 -> { Constants.TYPE_DVB_C }
            TvContract.Channels.TYPE_DVB_S,
            TvContract.Channels.TYPE_DVB_S2 -> { Constants.TYPE_DVB_S }
            TvContract.Channels.TYPE_DTMB -> { Constants.TYPE_DTMB }
            TvContract.Channels.TYPE_ATSC_T -> { Constants.TYPE_ATSC }
            TvContract.Channels.TYPE_ISDB_T -> { Constants.TYPE_ISDB_T }
            TvContract.Channels.TYPE_ISDB_TB -> { Constants.TYPE_ISDB_TB }
            TvContract.Channels.TYPE_ATSC_C -> { Constants.TYPE_CQAM }
            else -> -1
        }
    }
}