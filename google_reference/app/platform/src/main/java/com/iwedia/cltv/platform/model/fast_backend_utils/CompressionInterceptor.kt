package com.iwedia.cltv.platform.model.fast_backend_utils

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.Request
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class CompressionInterceptor : Interceptor {
    override fun intercept (chain: Interceptor.Chain): Response {
        val request = chain.request ()
        val compressedRequest = request.newBuilder()
            .header("Accept-Encoding", "gzip")
            .build()
        val originalResponse = chain.proceed(compressedRequest)

        return if (originalResponse.isSuccessful && isGzipped(originalResponse.headers())) {
            // If the response is successful and the content is gzipped, decompress it
            val uncompressedBody = decodeGzip(originalResponse.body()!!)
            originalResponse.newBuilder().body(uncompressedBody).build()
        } else {
            originalResponse
        }
    }

    private fun isGzipped(headers: okhttp3.Headers): Boolean {
        return headers["Content-Encoding"]?.equals("gzip", ignoreCase = true) ?: false
    }

    private fun decodeGzip(responseBody: ResponseBody): ResponseBody {
        val inputStream = GZIPInputStream(responseBody.byteStream())
        val content = inputStream.bufferedReader().use { it.readText() }
        return ResponseBody.create(responseBody.contentType(), content)
    }
}