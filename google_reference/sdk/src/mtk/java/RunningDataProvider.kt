package com.iwedia.cltv

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.ReferenceSdk.context
import com.iwedia.cltv.sdk.TifDataProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.handlers.ReferenceTvHandler
import com.mediatek.twoworlds.tv.common.MtkTvChCommonBase
import core_entities.Error
import listeners.AsyncReceiver
import org.json.JSONObject
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.io.UnsupportedEncodingException
import java.sql.Blob

/**
 * Running-Status Data provider
 *
 * @author Rahul Singh Rawat
 */


class RunningDataProvider {

    class RunningData {
        var isRunning = 0
        var replacementService = ""
    }

    companion object {

        private const val TAG = "RunningDataProvider"


        fun initChannelObserver(tvChannel: ReferenceTvChannel) {}

        /* Try old approach */

        @SuppressLint("Range")
        fun isVisible(providerString: String): Boolean
        {
            val providerArray: Array<String> = providerString.split(",").toTypedArray()
            if (!(providerArray.size == 10 || providerArray.size == 6))
            {
                Log.i(TAG, "isVisible: data length is not 6 or 10")
                return true
            }
            var length = providerArray.size
            val providerData = LongArray(length)
            val mSvlId: Long = providerArray[1].toLong()
            val mSvlRecId: Long = providerArray[2].toLong()
            val channelId: Long = providerArray[3].toLong()
            providerData[0] = mSvlId
            providerData[1] = mSvlRecId
            providerData[2] = channelId
            // v[3] = mHashcode;
            providerData[4] = (mSvlId shl 16) + mSvlRecId
            for (i in 5 until providerArray.size)
            {
                providerData[i] = providerArray[i].toInt().toLong()
                Log.i(TAG, "isVisible: providerData[$i] ${providerData[i]}")
            }

            return if (providerData[6] and MtkTvChCommonBase.SB_VNET_EPG.toLong() > 0)   //providerData[6] is the nwMask
            {
                Log.i(TAG, "isVisible: SERVICE VISIBLE")
                true
            }
            else
            {
                Log.i(TAG, "isVisible: SERVICE INVISIBLE")
                false
            }
        }

        fun checkRunningStatus(activeTvChannel: ReferenceTvChannel?) {}
    }
}