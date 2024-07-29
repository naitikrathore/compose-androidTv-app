package com.iwedia.cltv.platform.model.fast_backend_utils

import android.content.Context
import android.util.Log
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object FastAnokiUidHelper {

    private const val TAG = "FastAnokiUidHelper"
    const val ANOKI_UID_TAG = "ANOKI_UID"
    private var versionName: String? = null

    fun fetchAnokiUID(context: Context, deviceId: String, callback: (auid: String)->Unit){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Fetch anoki UID")
        var anokiUid = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(ANOKI_UID_TAG, "").toString()
        versionName = context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString("version_name", "").toString()
        try{
            if(anokiUid.isNotEmpty()){
                callback.invoke(anokiUid)
                return
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchAnokiUID: deviceId $deviceId")
            val call = FastRetrofitHelper.getFastBackendApi(FastUrlHelper.ANOKI_UID_URL).getAnokiUID(deviceId, SystemPropertyHelper.getPropertiesAsString(),versionName!!)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        anokiUid = response.body()!!.string()
                        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
                            .putString(ANOKI_UID_TAG, anokiUid).apply()
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: fetchAnokiUID anoki UID ==== $anokiUid")
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResponse: invoke call that anoki uid is fetched")
                        callback.invoke(anokiUid)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch anoki UID $t")
                    callback.invoke(anokiUid)
                }
            })
        } catch(e : Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Failed to fetch anoki UID ${e.message}")
            callback.invoke(anokiUid)
        }
    }
}