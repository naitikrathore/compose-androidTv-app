package com.iwedia.cltv.platform.refplus5.audio

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import java.util.*

/** Constants class. */
class Constants {
    /** Companion in Constants class. */
    companion object {

        private val TAG = javaClass.simpleName

        const val AUTHORITY = "com.mediatek.tv.agent.sounddb"
        const val SETTING_CATEGORY_GENERAL: String = "general"
        @JvmField
        val SOUND_SETTINGS_URI = Uri.parse("content://" + AUTHORITY + "/" + SETTING_CATEGORY_GENERAL)

        // ID start
        @JvmField val CFG_AUD_SOUND_MODE = "AQ_SoundStyle"
        @JvmField val CFG_AUD_SPDIF_DELAY = "AQ_SPDIF_Delay"
        @JvmField val CFG_AUD_DOLBY_SOUND_MODE = "AQ_DolbySoundMode"
        // ID end

        @JvmField
        val SOUND_ITEM_LIST_BY_SOURCE =
            ArrayList(Arrays.asList(CFG_AUD_SOUND_MODE, CFG_AUD_SPDIF_DELAY, CFG_AUD_DOLBY_SOUND_MODE))

        fun setValue(cr: ContentResolver, id: String, value: String): Int {
            var result = 0
            val values = ContentValues()
            val SELECTION: String = SoundDBHelper.ITEM + "=?"
            val SELECTIONARGS = arrayOf(id)
            try {
                values.put(SoundDBHelper.Source.COMMON, value)
                result =
                    cr.update(SoundDBHelper.INPUT_SOURCE_CONTENT_URI, values, SELECTION, SELECTIONARGS)
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, ex.message ?: "null")
            }
            cr.notifyChange(Uri.parse(SOUND_SETTINGS_URI.toString() + "/" + id), null)
            return result
        }


        @SuppressLint("Range")
        fun getStringValue(cr: ContentResolver, id: String): String {
            var value: String = ""
            var cursor: Cursor? = null
            val SELECTION: String = SoundDBHelper.ITEM + "=?"
            val SELECTIONARGS = arrayOf(id)

            try {
                cursor =
                    cr.query(SoundDBHelper.INPUT_SOURCE_CONTENT_URI, null, SELECTION, SELECTIONARGS, null)
                if (cursor != null && cursor.moveToNext()) {
                    if (SOUND_ITEM_LIST_BY_SOURCE.contains(id)) {
                        Log.i(
                            TAG,
                            "getStringValue id=" + id + ", is contains SOUND_ITEM_LIST_BY_SOURCE")
                        var currentInputSourceItem: String = getSourceValue(cr)
                        if (currentInputSourceItem != null && currentInputSourceItem.length > 0) {
                            value = cursor.getString(cursor.getColumnIndex(currentInputSourceItem))
                        }
                    } else {
                        value = cursor.getString(cursor.getColumnIndex(SoundDBHelper.Source.COMMON))
                    }
                }
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, ex.message?:"null")
            }

            try {
                cursor?.close()
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, ex.message?:"null")
            }

            return value
        }

        fun getIntValue(cr: ContentResolver, id: String, def: Int): Int {
            try {
                return getStringValue(cr, id).toInt()
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "getIntValue " + ex.message)
            }
            return def
        }

        @SuppressLint("Range")
        fun getSourceValue(cr: ContentResolver): String {
            var value: String = ""
            var cursor: Cursor? = null
            val SELECTION: String = SoundDBHelper.ITEM + "=?"
            val SELECTIONARGS = arrayOf(SoundDBHelper.SOURCE_ID)

            try {
                cursor =
                    cr.query(SoundDBHelper.INPUT_SOURCE_CONTENT_URI, null, SELECTION, SELECTIONARGS, null)
                if (cursor != null && cursor.moveToNext()) {
                    value = cursor.getString(cursor.getColumnIndex(SoundDBHelper.Source.COMMON))
                }
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "${ex.printStackTrace()}")
            }

            try {
                cursor?.close()
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "${ex.printStackTrace()}")
            }

            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getSourceValue,value: " + value)
            return value
        }
    }
}
