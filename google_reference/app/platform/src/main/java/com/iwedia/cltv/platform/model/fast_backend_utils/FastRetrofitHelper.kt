package com.iwedia.cltv.platform.model.fast_backend_utils

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors

object FastRetrofitHelper {
    private var retrofitInstance: FastBackendAPI?= null
    private var baseUrl: String?= null
    fun getFastBackendApi(url:String): FastBackendAPI {
        if (retrofitInstance != null && url == baseUrl) {
            return retrofitInstance!!
        } else {
            //to intercept http apis
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.NONE
            val client = HttpOkClientHelper.instance.newBuilder()
                .addInterceptor(ErrorInterceptor())
                .addInterceptor(logging)
                .addInterceptor(KeyApiInterceptor())
                .addInterceptor(CompressionInterceptor())
            // to parse input data using Gson
            val gson = GsonBuilder()
                .setLenient()
                .create()

            val retrofit: Retrofit by lazy {
                Retrofit.Builder()
                    .baseUrl(url)
                    .callbackExecutor(Executors.newSingleThreadExecutor())
                    .addConverterFactory(GsonConverterFactory.create(gson)) // Use Gson for JSON parsing
                    .client(client.build())
                    .build()
            }

            val retrofitInstance by lazy {
                retrofit.create(FastBackendAPI::class.java)
            }
            baseUrl = url
            this.retrofitInstance = retrofitInstance
            return retrofitInstance!!
        }

    }
}