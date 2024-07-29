package com.iwedia.cltv.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.tis.helper.ScanHelper
import com.iwedia.cltv.utils.Utils

class LanguageChangeReceiver: BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onReceive(context: Context?, p1: Intent?) {
        //Restart app
        //Utils.restartApp()
        ScanHelper.deleteChannelsAndAllTimeStampsAndRestartApp(context!!)
    }

}