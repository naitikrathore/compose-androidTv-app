package com.iwedia.cltv.platform.model.fast_backend_utils

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpOkClientHelper {
    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}