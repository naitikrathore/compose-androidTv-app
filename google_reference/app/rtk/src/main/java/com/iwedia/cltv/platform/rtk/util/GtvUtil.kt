package com.iwedia.cltv.platform.rtk.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.provider.Settings
import com.iwedia.cltv.platform.model.Constants

class GtvUtil {
    companion object {

        private val TAG = GtvUtil::class.java.simpleName
        private const val PERMISSION_WRITE_EPG_DATA =
            "com.android.providers.tv.permission.WRITE_EPG_DATA"
        private const val ACTION_INPUT_SELECTED = "android.apps.tv.launcherx.INPUT_SELECTED"
        private const val EXTRA_INPUT_ID = "extra_input_id"
        private const val LAUNCHERX_PACKAGE_NAME = "com.google.android.apps.tv.launcherx"
        private const val TV_CURRENT_INPUT_ID = "com.android.tv_current_input_id"
        fun getCurrentInputID(context: Context): String {
            val curInputID: String? = Settings.Global.getString(context.contentResolver, TV_CURRENT_INPUT_ID)
            return curInputID ?: ""
        }

        fun broadcastInputId(context: Context, inputId: String?) {
            var inputIdLocal: String? = ""
            inputIdLocal = if (inputId.equals(""))
                getCurrentInputID(context)
            else
                inputId

            if (inputIdLocal != "") {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "broadcastInputId: $inputIdLocal")
                val intent = Intent(ACTION_INPUT_SELECTED)
                intent.putExtra(EXTRA_INPUT_ID, inputIdLocal)
                intent.setPackage(LAUNCHERX_PACKAGE_NAME)
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                context.sendBroadcast(
                    intent,
                    PERMISSION_WRITE_EPG_DATA
                )
            }
        }
    }
}