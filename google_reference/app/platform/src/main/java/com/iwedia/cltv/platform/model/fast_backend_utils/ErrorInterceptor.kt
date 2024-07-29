package com.iwedia.cltv.platform.model.fast_backend_utils

import android.util.Log
import com.google.gson.Gson
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import okhttp3.Interceptor
import okhttp3.Response

class ErrorInterceptor : Interceptor {
    override fun intercept (chain: Interceptor.Chain): Response {
        val request = chain.request ()
        Log.d(Constants.LogTag.CLTV_TAG + "AnokiServerErrorMessage", "request ${request.body().toString()}")
        val response = chain.proceed (request)
        if (!response.isSuccessful) {
            val errorBody = response.body ()?.string ()
            Log.d(Constants.LogTag.CLTV_TAG + "AnokiServerErrorMessage","errorBody $errorBody")
            var error: ApiError = ApiError(400,"Error in Anoki response","")
            try {
                error = Gson().fromJson(errorBody, ApiError::class.java)
                throw ApiException (error)
            } catch (e: Exception) {
                if (e.message == "Service is not available in the current location") {
                    InformationBus.informationBusEventListener.submitEvent(Events.ANOKI_REGION_NOT_SUPPORTED)
                }
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"AnokiServerErrorMessage","Error received: msg ${e.message}, status code ${error.statusCode}")
                e.printStackTrace()
                //throw ApiException (ApiError(100, e.message!!, ""))
            }
        }
        return response
    }
}