package com.iwedia.cltv

import android.annotation.SuppressLint
import android.content.Context
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.net.Uri
import android.util.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * System Information Data provider
 *
 * @author
 */
class SystemInfoProvider {

    class SystemInfoData {
        var displayNumber = 0L
        var displayName = ""
        var providerData = ""
        var logoImagePath = ""
        var isRadioChannel = false
        var isSkipped = false
        var isLocked = false;
        var tunerType = -1
        var ordinalNumber = 0
        var frequency = 0
        var tsId = 0
        var onId = 0
        var serviceId = 0
        var bandwidth = 0
        var networkId = 0           // Unsupported
        var networkName = ""        // Unsupported
        var postViterbi = 0         // Unsupported
        var attr5s = 0              // Unsupported
        var signalQuality = 0
        var signalStrength = 0
        var signalBer = 0
        var signalAGC = 0     // Unsupported
        var signalUEC = 0     // Unsupported
    }

    companion object {
        val TAG = javaClass.simpleName
        lateinit var mycontext: Context

        fun setContext(con: Context) {
                mycontext=con
            }

        val AUTHORITY = "com.google.android.tv.dtvprovider"
        val SIGNAL_STATUS_PATH = "streamers"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$SIGNAL_STATUS_PATH");

        val projection: Array<String> = arrayOf(
                "id",
                "type",
                "state",
                "signal",
                "stats",
                "parameters"
            )

        val terSelectionClause = "type=? OR type=?"
        val terSelectionArgs : Array<String> = arrayOf("TERRESTRIAL", "TERRESTRIAL_2")

        @SuppressLint("Range")
        fun getSystemInfoData(onId: Int, tsId: Int, serviceId : Int) :  SystemInfoData? {
            var retVal = SystemInfoData()
            var serviceFound = false

            for (input in (mycontext.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
                var cursor = mycontext.contentResolver.query(
                    TvContract.buildChannelsUriForInput(input.id),
                    null,
                    null,
                    null,
                    null
                )

                if (cursor != null) {
                    if (cursor!!.count > 0) {
                        cursor.moveToFirst()
                        do {
                            var lOnId = 0
                            var lTsId = 0
                            var lServiceId = 0

                            /* ON ID */
                            if (cursor?.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID)) != null) {
                                lOnId =
                                    cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID))
                            }

                            /* TS ID */
                            if (cursor?.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID)) != null) {
                                lTsId =
                                    cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID))
                            }

                            /* Service ID */
                            if (cursor?.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID)) != null) {
                                lServiceId =
                                    cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID))
                            }

                            if ((lOnId == onId) && (lTsId == tsId) && (lServiceId == serviceId)) {
                                retVal.onId = lOnId
                                retVal.tsId = lTsId
                                retVal.serviceId = serviceId
                                /* Display Number */
                                if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)) != null) {
                                    retVal.displayNumber =
                                        cursor.getLong(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER))
                                }

                                /* Frequency & Bandwidth */
                                if (cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)) != null) {
                                    var blob = cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA))

                                    try {
                                        // on jap dtv error since database is different
                                        // org.json.JSONException: Value entertainment of type java.lang.String cannot be converted to JSONObject
                                        if (blob.isNotEmpty()) {
                                            var providerData = String(blob, Charsets.UTF_8)
                                            val obj = JSONObject(providerData)
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSystemInfoData: ${providerData}")
                                            if(obj !=null && obj.has("transport") ) {
                                                val jsonTransportObject = obj.getJSONObject("transport")
                                                if (jsonTransportObject.has("frequency"))
                                                    retVal.frequency = jsonTransportObject.getInt("frequency")
                                                if (jsonTransportObject.has("bandwidth"))
                                                    retVal.bandwidth = jsonTransportObject.getInt("bandwidth")
                                                if (jsonTransportObject.has("network-id"))
                                                    retVal.networkId = jsonTransportObject.getInt("network-id")
                                                if (retVal.bandwidth > 1000000) {
                                                    retVal.bandwidth = retVal.bandwidth / 1000000
                                                }
                                            }
                                        }

                                    } catch (ex: JSONException){
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSystemInfoData: $ex")
                                    }

                                }
                                serviceFound = true
                            }
                        } while (cursor.moveToNext() && !serviceFound)

                        if(serviceFound) {
                            cursor?.close()
                            break
                        }
                    }
                }
                cursor?.close()
            }

            if(!serviceFound) {
                return null
            }

            val channelsCursor = mycontext.contentResolver.query(
                CONTENT_URI,
                projection,
                terSelectionClause,
                terSelectionArgs,
                null);

            /* Signal Strength, Signal Quality & Ber */
            when (channelsCursor?.count) {
                null -> {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Null data in the Streamers Content Provider")
                }
                0 -> {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "No data in the Streamers Content Provider")
                }
                else -> {
                    channelsCursor.apply {
                        // Determine the column indexes
                        val idIndex: Int = getColumnIndex("id")
                        val typeIndex: Int = getColumnIndex("type")
                        val stateIndex: Int = getColumnIndex("state")
                        val signalIndex: Int = getColumnIndex("signal")
                        val statsIndex: Int = getColumnIndex("stats")
                        val parametersIndex: Int = getColumnIndex("parameters")
                        var tempFrequency : Int = 0

                        while (moveToNext()) {
                            // Gets the values from the columns
                            var id = getInt(idIndex)
                            var type = getString(typeIndex)

                            // Handling TER, CABLE and SAT, NOT IP
                            if (type == "TERRESTRIAL" || type == "TERRESTRIAL_2" || type == "CABLE" || type == "SATELLITE)") {
                                var state = getString(stateIndex)
                                var signal = getString(signalIndex).toBoolean()
                                var stats = getString(statsIndex)
                                var parameters = getString(parametersIndex)

                                var jsonParameters = JSONObject(parameters)
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSystemInfoData: JSONObject jsonParameters has valueeee: ${jsonParameters.has("frequency")}")
                                if (jsonParameters.has("frequency")) {
                                    tempFrequency = jsonParameters.getInt("frequency")
                                    if (signal) {
                                        if (tempFrequency == retVal.frequency) {
                                            try {
                                                // Same for all tuners
                                                var jsonStats = JSONObject(stats)

                                                var ber = jsonStats.getInt("ber")
                                                var snr = jsonStats.getInt("snr")
                                                var rssi = jsonStats.getInt("rssi")

                                                retVal.signalStrength = rssi
                                                retVal.signalQuality = snr
                                                retVal.signalBer = ber
                                            } catch (e: JSONException) {
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Exception e: $e")
                                                e.printStackTrace()
                                            }
                                        } else {
                                            continue
                                        }
                                    }
                                }
                                else {
                                    continue
                                }
                            } else {
                                continue
                            }
                        }
                    }
                }
            }
            channelsCursor?.close()
            return retVal
        }
    }
}