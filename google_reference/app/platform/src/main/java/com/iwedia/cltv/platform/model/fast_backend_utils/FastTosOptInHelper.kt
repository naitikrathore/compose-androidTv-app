package com.iwedia.cltv.platform.model.fast_backend_utils

import android.content.Context
import android.util.Log
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.FastInfoItem
import com.iwedia.cltv.platform.model.FastTosOptInItem
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

object FastTosOptInHelper {

    private const val TAG = "FastTosOptInHelper"
    private const val ANOKI_TOS_OPT_IN_TAG = "ANOKI_TOS_OPT_IN"
    private const val ANOKI_TOS_SPLASH_IMAGE_URL_TAG = "ANOKI_TOS_SPLASH_IMAGE_URL"
    private const val ANOKI_TOS_URL_TAG = "ANOKI_TOS_URL"
    private const val ANOKI_PRIVACY_POLICY_URL_TAG = "ANOKI_PRIVACY_POLICY_URL"

    private var tosOptIn = -1
    private var splashImageUrl = ""
    private var tosUrl = ""
    private var privacyPolicyUrl = ""
    private var fastInfoItem: FastInfoItem? = null
    private var versionName = ""

    fun fetchTosOptInFromServer(context: Context, callback: (tos: Int) -> Unit) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch TOS Opt in")
        val tosSharedPref = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)
        tosOptIn = tosSharedPref.getInt(ANOKI_TOS_OPT_IN_TAG, -1)
        splashImageUrl = tosSharedPref.getString(ANOKI_TOS_SPLASH_IMAGE_URL_TAG, "").toString()
        tosUrl = tosSharedPref.getString(ANOKI_TOS_URL_TAG, "").toString()
        versionName = tosSharedPref.getString("version_name", "").toString()
        privacyPolicyUrl = tosSharedPref.getString(ANOKI_PRIVACY_POLICY_URL_TAG, "").toString()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchTosOptInFromServer: tosOptIn $tosOptIn, splashImageUrl $splashImageUrl, tosUrl $tosUrl, privacyPolicyUrl $privacyPolicyUrl")
        if (tosOptIn != -1) {
            callback.invoke(tosOptIn)
        } else {
            try {

                Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchTosOptInFromServer: locale === ${LocaleHelper.getCurrentLocale()}")
                FastAnokiUidHelper.fetchAnokiUID(
                    context,
                    AdvertisingIdHelper.fetchAdvertisingId(context)
                ) {
                    val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.ANOKI_UID_URL)
                        .getFastInfo(it, LocaleHelper.getCurrentLocale(),versionName)
                    call.enqueue(object : Callback<FastInfoItem> {
                        override fun onResponse(
                            call: Call<FastInfoItem>,
                            response: Response<FastInfoItem>
                        ) {
                            if (response.isSuccessful && response.body() != null) {
                                fastInfoItem = response.body()!!
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: fetched fast info == $fastInfoItem")
                                if (fastInfoItem != null) {
                                    tosOptIn = fastInfoItem!!.tosOptIn
                                    splashImageUrl = fastInfoItem!!.splashImageUrl!!
                                    tosUrl = fastInfoItem!!.tosUrl!!
                                    privacyPolicyUrl = fastInfoItem!!.privacyPolicyUrl!!
                                    tosSharedPref.edit().putInt(ANOKI_TOS_OPT_IN_TAG, tosOptIn).apply()
                                    tosSharedPref.edit().putString(ANOKI_TOS_SPLASH_IMAGE_URL_TAG, splashImageUrl).apply()
                                    tosSharedPref.edit().putString(ANOKI_TOS_URL_TAG, tosUrl).apply()
                                    tosSharedPref.edit().putString(ANOKI_PRIVACY_POLICY_URL_TAG, privacyPolicyUrl).apply()
                                }
                                callback.invoke(tosOptIn)
                            }
                        }

                        override fun onFailure(call: Call<FastInfoItem>, t: Throwable) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch fast info ${t.message}")
                            callback.invoke(tosOptIn)
                        }

                    })
                }
            } catch (e: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch fast info ${e.message}")
                callback.invoke(tosOptIn)
            }
        }
    }

    fun getTosSplashImageUrl(): String {
        return splashImageUrl
    }

    fun getTosUrl(): String {
        return tosUrl
    }

    fun getPrivacyPolicyUrl(): String {
        return privacyPolicyUrl
    }

    fun putTosOptInServer(context: Context, value: Int, callback: (tos: Boolean) -> Unit) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateTosOptInServer: ")
        try {
            FastAnokiUidHelper.fetchAnokiUID(
                context,
                AdvertisingIdHelper.fetchAdvertisingId(context)
            ) {
                val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.ANOKI_UID_URL)
                    .putTosOptIn(FastTosOptInItem(it, SystemPropertyHelper.getPropertiesAsString(),value))
                call.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                            .putInt(ANOKI_TOS_OPT_IN_TAG, value).apply()
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: update tos opt in Server ${response.message()}")
                        callback.invoke(true)
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailure: Failed to update tosOptIn in server ${t.message}")
                        callback.invoke(false)
                    }

                })
            }
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateTosOptInServer: Failed to update tosOptIn in server ${e.message}")
            callback.invoke(false)
        }
    }
}