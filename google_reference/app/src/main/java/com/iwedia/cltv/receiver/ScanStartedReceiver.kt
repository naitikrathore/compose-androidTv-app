package com.iwedia.cltv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.ModuleProvider
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.Constants

class ScanStartedReceiver: BroadcastReceiver() {

    companion object {
        val SCAN_STARTED_INTENT_ACTION = "com.iwedia.SETTINGS_SCAN_STARTED"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(Constants.LogTag.CLTV_TAG + "ScanStartedReceiver ", "intent received")
        val moduleProvider = ModuleProvider(ReferenceApplication.get())
        moduleProvider.getPlayerModule().stop()
    }
}