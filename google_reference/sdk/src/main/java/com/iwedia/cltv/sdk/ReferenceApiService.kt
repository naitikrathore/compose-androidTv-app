package com.iwedia.cltv.sdk

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Reference api service
 *
 * @author Dragan Krnjaic
 */
interface ReferenceApiService {

    @GET
    fun downloadFile(@Url fileUrl: String) : Call<ResponseBody>


    companion object {

        fun create(baseUrl: String): ReferenceApiService {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .build()

            return retrofit.create(ReferenceApiService::class.java);
        }
    }
}