package com.iwedia.google_reference.platform.refplus5.ccConfiguration

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Columns.CLOSED_CAPTION_DISPLAY
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Columns.CLOSED_CAPTION_SERVICE
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_DEFAULT
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_MUTE
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_OFF
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_ON
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC1
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC2
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC3
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC4
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_DEFAULT
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT1
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT2
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT3
import com.mediatek.dtv.tvinput.framework.tifextapi.analog.analogtuner.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT4
import java.util.Locale

class ClosedCaptionAnalog {
    companion object {
        val TAG = javaClass.simpleName
        val GENERAL = "common"

        val table = "content://" + Constants.AUTHORITY.toString() + "/" + GENERAL

        private fun saveAnalogSettingsValue(context: Context, id: String, value: String): Boolean {
            val URI = Uri.parse(table)
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "saveAnalogSettingsValue:$id----$value")
            val values = ContentValues()
            values.put(id.toLowerCase(Locale.ROOT), value)
            try {
                var num = context.contentResolver.update(URI, values, null, null)
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "num = $num")
            } catch (e: Exception) {
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "e: Exception:" + e.printStackTrace())
                return false
            }
            return true
        }

        fun saveUserSelectedCCOptions(context: Context, ccOptions: String, value: Int) : Boolean {

            var saveValue = 0
            var analogSetting = ""
            when (ccOptions) {
                "display_cc" ->
                {
                    analogSetting = CLOSED_CAPTION_DISPLAY
                    saveValue = when (value) {
                        0 -> CLOSED_CAPTION_DISPLAY_OFF
                        1 -> CLOSED_CAPTION_DISPLAY_ON
                        else -> return false
                    }
                }
                "caption_services" ->
                {
                    analogSetting = CLOSED_CAPTION_SERVICE
                    saveValue = when(value) {
                        0 -> CLOSED_CAPTION_SERVICE_CC1
                        1 -> CLOSED_CAPTION_SERVICE_CC2
                        2 -> CLOSED_CAPTION_SERVICE_CC3
                        3 -> CLOSED_CAPTION_SERVICE_CC4
                        4 -> CLOSED_CAPTION_SERVICE_TEXT1
                        5 -> CLOSED_CAPTION_SERVICE_TEXT2
                        6 -> CLOSED_CAPTION_SERVICE_TEXT3
                        7 -> CLOSED_CAPTION_SERVICE_TEXT4
                        else -> return false
                    }
                }
                else -> return false

            }
            saveAnalogSettingsValue(context, analogSetting, saveValue.toString())
            return true
        }
    }
}