package com.iwedia.cltv.platform.refplus5.provider

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvContract.buildChannelUri
import android.net.Uri
import android.util.Log
import com.iwedia.cltv.platform.base.content_provider.TifSystemInfoProvider
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.mediatek.dtv.tvinput.client.tunerinfo.TunerInfoManager
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBC_BANDWIDTH_5MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBC_BANDWIDTH_6MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBC_BANDWIDTH_7MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBC_BANDWIDTH_8MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBT_BANDWIDTH_10MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBT_BANDWIDTH_1_7MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBT_BANDWIDTH_5MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBT_BANDWIDTH_6MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBT_BANDWIDTH_7MHZ
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants.DVBT_BANDWIDTH_8MHZ
import java.util.Locale
import com.mediatek.dtv.tvinput.dvbtuner.chdb.Constants as const

/**
 * System Information Data provider
 *
 * @author Rishi Raj
 */

class SystemInfoProvider(var context: Context, tunerMode: Int?) : TifSystemInfoProvider() {
    val TAG = javaClass.simpleName
    private var tunerInfo: TunerInfoManager? = null
    private val bandWidthMap = getBandWidthMap(tunerMode!!)

    override fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>){
        CoroutineHelper.runCoroutine({
            val retVal = SystemInfoData()
            val info = tunerInfo?.getTunerInfo("")
            tunerInfo = TunerInfoManager(context, "com.mediatek.dtv.tvinput.dvbtuner/.DvbTvInputService/HW0")


            if (info != null) {
                retVal.signalStrength = info.getInt(Constants.KEY_SIGNAL_STRENGTH_IN_PERCENT)
                retVal.signalQuality = info.getInt(Constants.KEY_SIGNAL_QUALITY_IN_PERCENT)
                retVal.signalUEC = info.getInt(Constants.KEY_UEC)
                retVal.signalAGC = info.getInt(Constants.KEY_AGC)
                retVal.bandwidth = bandWidthMap.getOrDefault(info.getInt(Constants.KEY_BANDWIDTH), "BW_UNKNOWN")
                retVal.signalBer  = info.getInt(Constants.KEY_SIGNAL_BER)
            }
            retVal.frequency = getFrequency(tvChannel.channelId, context)
            retVal.networkName = getNetworkName(tvChannel.channelId,context)!!
            retVal.postViterbi = String.format(Locale.ENGLISH, "%3.2e", retVal.signalBer.toDouble() / (100 * 1000))
            retVal.attr5s = String.format(Locale.ENGLISH, "%3.2e", retVal.signalBer.toDouble() / (100 * 1000))
            callback.onReceive(retVal)
        })
    }

    fun getFrequency(mId: Long, context: Context?): Int {
        return getDbIntValueforTv(context!!, buildChannelUri(mId), const.COLUMN_FREQUENCY)
    }

    fun getNetworkName(mId: Long, context: Context?): String? {
        return getDbStringValueforTv(context!!, buildChannelUri(mId), const.COLUMN_NETWORK_NAME)
    }

    @SuppressLint("Range")
    fun getchNumber(cursor: Cursor): String{
            return cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER))
    }
    @SuppressLint("Range")
    fun getDbIntValueforTv(context: Context, uri: Uri?, columnValue: String): Int {
        if (uri == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getDbIntValueforTv  uri == null")
            return 0
        }
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getDbValueforTv :$uri|columnValue :$columnValue")
        var dbvalue = 0
        val cr = context.contentResolver
        val c = cr.query(uri, null, null, null, "")
        if (c == null) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getDbValueforTv Cursor == null !!")
            return 0
        }
        try {
            while (c.moveToNext()) {
                dbvalue = c.getInt(c.getColumnIndex(columnValue))
            }
        } catch (e: Exception) {
            e.printStackTrace()

        }
        c.close()
        return dbvalue
    }

    @SuppressLint("Range")
    fun getDbStringValueforTv(context: Context, uri: Uri?, columnValue: String): String? {
        if (uri == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getDbIntValueforTv  uri == null")
            return ""
        }
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getDbValueforTv :$uri|columnValue :$columnValue")
        var dbvalue = ""
        val cr = context.contentResolver
        val c = cr.query(uri, null, null, null, "")
        if (c == null) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getDbValueforTv Cursor == null !!")
            return ""
        }
        try {
            while (c.moveToNext()) {
                dbvalue = c.getString(c.getColumnIndex(columnValue))
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        c.close()
        return dbvalue
    }

    private fun getBandWidthMap(tuneMode: Int): MutableMap<Int, String> {
        val map = hashMapOf<Int, String>()
        // hashMap based on tuneMode value
        // DVB-T/C
        return when (tuneMode) {
            0 -> {
                map[DVBT_BANDWIDTH_5MHZ] = "BW_5_MHz"
                map[DVBT_BANDWIDTH_6MHZ] = "BW_6_MHz"
                map[DVBT_BANDWIDTH_7MHZ] = "BW_7_MHz"
                map[DVBT_BANDWIDTH_8MHZ] = "BW_8_MHz"
                map[DVBT_BANDWIDTH_10MHZ] = "BW_10_MHz"
                map[DVBT_BANDWIDTH_1_7MHZ] = "BW_1_712_MHz"
                return map
            }
            1 -> {
                map[DVBC_BANDWIDTH_5MHZ] = "BW_5_MHz"
                map[DVBC_BANDWIDTH_6MHZ] = "BW_6_MHz"
                map[DVBC_BANDWIDTH_7MHZ] = "BW_6_MHz"
                map[DVBC_BANDWIDTH_8MHZ] = "BW_8_MHz"
                return map
            }
            else -> {
                hashMapOf()
            }
        }
    }
}