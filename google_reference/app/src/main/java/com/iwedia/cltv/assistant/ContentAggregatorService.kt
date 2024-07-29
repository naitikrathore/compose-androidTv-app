package com.iwedia.cltv.assistant

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.receiver.GlobalAppReceiver


class ContentAggregatorService : Service() {
    @RequiresApi(Build.VERSION_CODES.R)
    private val globalAppReceiver = GlobalAppReceiver()
    val TAG = javaClass.simpleName

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate: creating ContentAggregatorService")
        registerGlobalAppReceiver()
        super.onCreate()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onDestroy() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onDestroy: destroying ContentAggregatorService")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun registerGlobalAppReceiver(){
        val intentFilter = IntentFilter(GlobalAppReceiver.GLOBAL_KEY_INTENT_ACTION).apply {
            addAction(GlobalAppReceiver.INTENT_INPUT_SOURCE)
            addAction(GlobalAppReceiver.SCAN_COMPLETED_INTENT_ACTION)
            addAction(GlobalAppReceiver.ANDROID_GLOBAL_BUTTON_ACTION)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(GlobalAppReceiver.SETTINGS_OPENED_INTENT_ACTION)
            addAction(GlobalAppReceiver.SETTINGS_STOPPED_INTENT_ACTION)
            addAction(GlobalAppReceiver.PVR_STATUS_CHANGED_INTENT_ACTION)
            addAction(GlobalAppReceiver.STREAM_MUTE_CHANGED_ACTION)
            addAction(GlobalAppReceiver.INPUT_CHANGE_ACTION)
            addAction(GlobalAppReceiver.TV_KEYCODE_INTENT_ACTION)
        }
        try {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate: registering globalAppReceiver")
            applicationContext.registerReceiver(globalAppReceiver, intentFilter)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "can not register global app receiver", e)
        }
    }
}