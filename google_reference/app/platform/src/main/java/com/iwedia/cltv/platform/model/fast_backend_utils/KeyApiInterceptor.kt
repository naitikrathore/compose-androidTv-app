package com.iwedia.cltv.platform.model.fast_backend_utils

import okhttp3.Interceptor
import okhttp3.Response

class KeyApiInterceptor : Interceptor {

    private val TEST_KEY = "aXdlZGlhLXRlc3QtYXBw.xDcF3hP2vF7R6kRW"
    private val PRODUCTION_KEY = "aXdlZGlhLXByb2QtYXBw.DRjWmgdF8dNYwyEQ"
    private val KEY_HEADER = "X-API-KEY"

    override fun intercept (chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = originalRequest.newBuilder()
            .header(KEY_HEADER,PRODUCTION_KEY)
            .build()

        return chain.proceed(response)
    }

    fun getProductionKey(): String{
        return PRODUCTION_KEY
    }
}