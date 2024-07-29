package com.iwedia.cltv.assistant

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.model.Constants


/**
 * Bootup receiver
 */
class BootupReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onReceive(context: Context, arg1: Intent?) {
        Log.d(Constants.LogTag.CLTV_TAG + "BootupReceiver", "onReceive: ")
        try {
            val serviceIntent = Intent(context, ContentAggregatorService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}