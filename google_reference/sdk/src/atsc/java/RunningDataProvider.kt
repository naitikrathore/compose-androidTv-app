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
import com.google.firebase.database.snapshot.BooleanNode
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.ReferenceSdk.context
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider.Companion.CHANNELS_URI
import com.iwedia.cltv.sdk.TifDataProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.handlers.ReferenceTvHandler
import core_entities.Error
import listeners.AsyncReceiver
import org.json.JSONObject
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.io.UnsupportedEncodingException

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

        private var channelObserver: ContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                Log.i(TAG, "onChange: #######ONCHANGE entry")
                var activeTvChannel = (ReferenceSdk.tvHandler!! as ReferenceTvHandler).activeChannel
                checkRunningStatus(activeTvChannel)
            }
        }

        private const val TAG = "RunningDataProvider"

        
        fun initChannelObserver(tvChannel: ReferenceTvChannel) {
            if (channelObserver != null){
                context.contentResolver.unregisterContentObserver(channelObserver)
                Log.i(TAG, "initChannelObserver: channelobserver != null, unregister")
            }

            Log.i(TAG, "initChannelObserver: Register new observer")
            context.contentResolver.registerContentObserver(
                ReferenceContract.buildChannelsUri(tvChannel.id.toLong()),
                true,
                channelObserver
            )
        }

        /* Try old approach */

        fun isVisible(providerString: String): Boolean
        {
            return true
        }

        private fun serviceRunning(){
            InformationBus.submitEvent(
                Event(
                    ReferenceEvents.ACTIVE_SERVICE_RUNNING
                )
            )
        }

        private fun notRunningBanner(){
            Log.i(TAG, "notRunningBanner: Sending event ${ReferenceEvents.ACTIVE_SERVICE_NOT_RUNNING}")
            InformationBus.submitEvent(
                Event(
                    ReferenceEvents.ACTIVE_SERVICE_NOT_RUNNING
                )
            )
        }

        private fun replacementBanner()
        {
            Log.i(TAG, "notRunningBanner: Sending event ${ReferenceEvents.ACTIVE_SERVICE_REPLACEABLE}")
            InformationBus.submitEvent(
                Event(
                    ReferenceEvents.ACTIVE_SERVICE_REPLACEABLE
                )
            )
        }

        @RequiresApi(Build.VERSION_CODES.N)
        private fun replaceService(replaceService: String, lastChannel: String)
        {
            val prefix = "0x"

            val tripletStr: String? = replaceService.substringAfter("dvb://")

            val tripletIDs = tripletStr!!.split(".").toTypedArray()
            Log.i(TAG, "replaceService: ONID = ${tripletIDs[0]}, TSID = ${tripletIDs[1]}, ServiceID = ${tripletIDs[2]}")

            var replaceONId = Integer.decode(prefix.plus(tripletIDs[0]))
            var replaceTSId = Integer.decode(prefix.plus(tripletIDs[1]))
            var replaceServiceId = Integer.decode(prefix.plus(tripletIDs[2]))

            var replaceChannel = (ReferenceSdk.dataProvider as TifDataProvider).searchChannelByTriplet(replaceONId, replaceTSId, replaceServiceId)
            if (replaceChannel != null) {
                replacementBanner()

                (ReferenceSdk.tvHandler as ReferenceTvHandler).changeChannel(
                    replaceChannel,
                    object : AsyncReceiver {
                        override fun onFailed(error: Error?) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onFailed: No Replacement Channel")
                        }

                        override fun onSuccess() {
                            Log.i(TAG, "onSuccess: Replacement Channel Success")
                            Toast.makeText(context, "Service $lastChannel not running, switching to ${replaceChannel.name}", Toast.LENGTH_SHORT).show()
                            checkRunningStatus(replaceChannel)
                        }
                    })

            }
            else {
                notRunningBanner()
                Log.i(TAG, "replaceService: Replacement Channel Not Found!!")
            }
        }

        @RequiresApi(Build.VERSION_CODES.N)
        fun checkRunningStatus(activeTvChannel: ReferenceTvChannel?) {
            Log.i(TAG, "checkRunningStatus: #######ENTRY")
            //Check running status and replacement service for active channel

            Log.i(TAG, "getRunningStatus: LOG4 ########## Values of active = $activeTvChannel")
            if (activeTvChannel != null)
            {
                Log.i(TAG, "checkRunningStatus: ACTIVE CHANNEL ID = ${activeTvChannel!!.id}")
                val systemData = getRunningStatus(activeTvChannel!!.onId, activeTvChannel.tsId, activeTvChannel.serviceId)
                if(systemData == null) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Unable to get system data")
                    return
                }

                var rnng_stts = systemData.isRunning
                var rplc_srvc = systemData.replacementService
                if (rnng_stts == 1)
                {
                    if (rplc_srvc == "not_running_banner")
                    {
                        Log.i(TAG, "checkRunningStatus: Displaying banner")
                        notRunningBanner()
                    }
                    else
                    {
                        Log.i(TAG, "checkRunningStatus: replace service found as $rplc_srvc")
                        var actChannel = activeTvChannel.name
                        replaceService(rplc_srvc, actChannel)
                    }
                }
                else
                {
                    serviceRunning()
                }
            }
        }

        @SuppressLint("Range")
        private fun getRunningStatus(onId: Int,  tsId: Int, serviceId: Int) : RunningData?
        {
            var providerData = ""
            var obj: JSONObject? = null
            var retRunning = RunningData()
            var selection = ReferenceContract.Channels.ORIGINAL_NETWORK_ID_COLUMN + " = ? and " + ReferenceContract.Channels.TRANSPORT_STREAM_ID_COLUMN + " = ? and " + ReferenceContract.Channels.SERVICE_ID_COLUMN + " = ?"
            var cursor = context.contentResolver.query(
                CHANNELS_URI,
                null,
                selection,
                arrayOf(onId.toString(), tsId.toString(), serviceId.toString()),
                null
            )
            Log.i(TAG, "getRunningStatus: cursor = $cursor and cursor.count = ${cursor!!.count}")
            if (cursor != null && cursor!!.count > 0) {
                cursor.moveToFirst()

                if (cursor?.getBlob(cursor.getColumnIndex(ReferenceContract.Channels.INTERNAL_PROVIDER_DATA_COLUMN)) != null) {
                    var providerDataBlob = cursor.getBlob(cursor.getColumnIndex(ReferenceContract.Channels.INTERNAL_PROVIDER_DATA_COLUMN))
                    try {
                        providerData = String(providerDataBlob, Charsets.UTF_8)
                        Log.i(TAG, "getRunningStatus: ProviderData = $providerData")
                    }
                    catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getRunningStatus: Cannot convert")
                        return null
                    }


                    try {
                        obj = JSONObject(providerData)
                    }catch (e : Exception){
                        Log.i(TAG, "getRunningStatus: Cannot Convert String to JSONObject")
                        return null
                    }


                    try {
                        retRunning.isRunning = obj.getInt("running-status")
                        Log.i(TAG, "getRunningStatus: RUNNING STATUS = ${retRunning.isRunning}")
                        retRunning.replacementService = obj.getString("replace-service")
                        Log.i(TAG, "getRunningStatus: REPLACEMENT SERVICE = ${retRunning.replacementService}")
                    }
                    catch (e : Exception) {
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getRunningStatus: JSONException --------- $e")
                    }

                    if (retRunning.isRunning == 1) {
                        try {
                            retRunning.replacementService = obj.getString("replace-service")
                            if (retRunning.replacementService == "")
                            {
                                retRunning.replacementService = "not_running_banner"
                            }
                        }
                        catch (e : Exception) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getRunningStatus: JSONException --------- $e")
                            retRunning.replacementService = "not_running_banner"
                        }
                    }
                }
                else {
                    Log.i(TAG, "getSystemInfoData: cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)) == null")
                }
            }
            cursor!!.close()
            return retRunning
        }

    }
}