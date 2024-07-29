package com.iwedia.cltv.platform.model.fast_backend_utils

import android.content.Context
import com.iwedia.cltv.platform.`interface`.UtilsInterface

object FastUrlHelper {
    /**
     * Test URLs for FAST Backend
     */
    private val BASE_URL_TEST = "https://stream-dev.aistrm.net:5100"
    private val ANOKI_UID_URL_TEST = "https://config-dev.aistrm.net:5104"

    /**
     * Staging URLs for FAST Backend
     */
    private val BASE_URL_STAGE = "https://stream-stage.aistrm.net:5200"
    private val ANOKI_UID_URL_STAGE = "https://config-stage.aistrm.net:5204"

    /**
     * Production URLs for FAST Backend
     */
    private val BASE_URL_PROD = "https://stream.aistrm.net"
    private val ANOKI_UID_URL_PROD = "https://config.aistrm.net"

    var BASE_URL = BASE_URL_PROD
    var ANOKI_UID_URL = ANOKI_UID_URL_PROD

    private const val PREFS_SELECTED_URL= "SELECTED_URL"

    fun getServerList(): Map<Int, String> {
        return mapOf(Pair(0, "Production"), Pair(1, "Stage"), Pair(2, "Test"))
    }

    fun getSelectedUrlIndex(context: Context): Int {
        return context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getInt(PREFS_SELECTED_URL, 0)
    }

    fun saveSelectedUrlIndex(context: Context, x: Int){
        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().putInt(
            PREFS_SELECTED_URL, x).apply()
    }

    fun setSelectedUrl(x: Int){
        when(x){
            0 -> {
                BASE_URL = BASE_URL_PROD
                ANOKI_UID_URL = ANOKI_UID_URL_PROD
            }
            1 -> {
                BASE_URL = BASE_URL_STAGE
                ANOKI_UID_URL = ANOKI_UID_URL_STAGE
            }
            2 -> {
                BASE_URL = BASE_URL_TEST
                ANOKI_UID_URL = ANOKI_UID_URL_TEST
            }
            else -> {
                BASE_URL = BASE_URL_PROD
                ANOKI_UID_URL = ANOKI_UID_URL_PROD
            }
        }
    }
}