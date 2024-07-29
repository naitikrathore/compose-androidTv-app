package com.iwedia.cltv.platform.model.fast_backend_utils

import android.content.Context
import android.util.Log
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.network.NetworkData
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

object IpAddressHelper {

    var publicIpAddress: String = ""

    private var networkModule: NetworkInterface? = null

    fun setNetworkModule (networkInterface: NetworkInterface) {
        networkModule = networkInterface
    }

    fun fetchPublicIpAddress(context: Context): String {
        val client = HttpOkClientHelper.instance.newBuilder().build()
        val request = Request.Builder()
            .url("https://api64.ipify.org?format=json")
            .build()
        if (networkModule == null || networkModule!!.networkStatus.value == NetworkData.NoConnection) {
            return ""
        }
        try {

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    var jsonIp = responseBody?.string()
                    try {
                        Log.d(Constants.LogTag.CLTV_TAG + "IpAddressHelper", "jsonIP address = $jsonIp")
                        val jsonObject = JSONObject(jsonIp)
                        publicIpAddress = jsonObject.getString("ip")

                        /**
                         * Save IP address in sharedPrefs so that it will be available for anokiOnDemand module
                         */
                        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                            .putString(Constants.SharedPrefsConstants.PREFS_KEY_IP_ADDRESS, publicIpAddress)
                            .apply()
                        Log.d(Constants.LogTag.CLTV_TAG + "IpAddressHelper", "Public IP address = $publicIpAddress")
                        return publicIpAddress
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return responseBody!!.string()
                }
            }
        } catch (e: IOException) {
            Log.d(Constants.LogTag.CLTV_TAG + "IpAddressHelper", "Exception!!! ${e.message} ${e.cause} ${e.toString()}")
            e.printStackTrace()
            return ""
        }
        return ""
    }

    fun getIpAddress(): String {
        return publicIpAddress
    }
}