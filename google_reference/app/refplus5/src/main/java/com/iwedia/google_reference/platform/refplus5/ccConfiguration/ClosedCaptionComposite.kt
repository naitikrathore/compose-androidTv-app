package com.iwedia.google_reference.platform.refplus5.ccConfiguration

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import java.util.Locale
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Columns.CLOSED_CAPTION_DISPLAY
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Columns.CLOSED_CAPTION_SERVICE
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_DEFAULT
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_MUTE
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_OFF
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_ON
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC1
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC2
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC3
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC4
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_DEFAULT
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT1
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT2
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT3
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT4

class ClosedCaptionComposite {
    companion object {
        private val TAG = javaClass.simpleName
        private const val GENERAL = "common"
        private val table = "content://" + Constants.AUTHORITY + "/" + GENERAL

        private fun saveCompositeSettingsValue(context: Context, id: String, value: String): Boolean {
            val URI = Uri.parse(table)
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "saveATSCSettingsValue id: $id value:$value")
            val values = ContentValues()
            values.put(id.toLowerCase(Locale.ROOT), value)
            try {
                var count = context.contentResolver.update(URI, values, null, null)
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "update count = $count")
            } catch (e: Exception) {
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "e: Exception:" + e.printStackTrace())
                return false
            }
            return true
        }

        fun saveUserSelectedCCOptions(context: Context, ccOptions: String, value: Int) : Boolean {
            var saveValue = 0
            var compositeSetting = ""
            when (ccOptions) {
                "display_cc" ->
                {
                    compositeSetting = CLOSED_CAPTION_DISPLAY
                    saveValue = when (value) {
                        0 -> CLOSED_CAPTION_DISPLAY_OFF
                        1 -> CLOSED_CAPTION_DISPLAY_ON
                        else -> return false
                    }
                }
                "caption_services" ->
                {
                    compositeSetting = CLOSED_CAPTION_SERVICE
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
            saveCompositeSettingsValue(context, compositeSetting, saveValue.toString())
            return true
        }
    }
}