package com.iwedia.cltv.platform.model.fast_backend_utils

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants

object AdvertisingIdHelper {

    private const val defaultAdId = "00000000-0000-0000-0000-000000000000"
    private var advertisingId = ""

    fun fetchAdvertisingId(context: Context): String {
        return try {
            val idInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            advertisingId = idInfo.id

            /**
             * Save advertisingId in sharedPrefs so that it will be available for anokiOnDemand module
             */
            context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                .putString(Constants.SharedPrefsConstants.PREFS_KEY_ADVERTISING_ID, advertisingId)
                .apply()
            Log.d(Constants.LogTag.CLTV_TAG + "AdvertisingIdHelper", "fetchAdvertisingId: AD_ID $advertisingId")
            advertisingId
        } catch (e: Exception) {
            e.printStackTrace()
            defaultAdId
        }
    }

    fun getAdvertisingId(context: Context): String {
        val adId =  context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(
            Constants.SharedPrefsConstants.PREFS_KEY_ADVERTISING_ID, null)
        if (adId.isNullOrEmpty()) {
            fetchAdvertisingId(context)
        }
        return advertisingId
    }
}