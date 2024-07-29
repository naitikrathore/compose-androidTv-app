package com.iwedia.cltv.platform.refplus5

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.ArrayMap
import android.util.Log
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.refplus5.eas.EasControl
import com.iwedia.cltv.platform.refplus5.screenMode.Constants.Companion.GLOBAL_PROVIDER_URI_URI


class SaveValue {

    companion object{

        private val TAG = javaClass.simpleName

        fun saveTISSettingsStringValue(
            context: Context, id: String, AUTHORITY: String, general: String, value: String
        ): Boolean {
            val URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "saveTISSettingsStringValue : $URI $id $value")
            try {
                val values = ContentValues()
                values.put(id, value)
                var ret = context.contentResolver.update(URI, values, null, null)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "saveTISSettingsStringValue: $ret")
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, Log.getStackTraceString(ex))
                return false
            }
            return true
        }

        @SuppressLint("Range")
        fun readTISSettingsStringValues(
            context: Context, id: String, AUTHORITY: String, general: String, def: String
        ): String {
            var def = def
            var cursor: Cursor? = null
            val URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "readTISSettingsStringValues : $URI $id $def")
            try {
                cursor = context.contentResolver.query(URI, arrayOf(id), null, null, null)
                if (cursor == null) {
                    return def
                }
                if (cursor.moveToNext()) {
                    def = cursor.getString(cursor.getColumnIndex(id))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "readTISSettingsStringValues return:$def")
                }
            } catch (ex: java.lang.Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, Log.getStackTraceString(ex))
            }
            try {
                cursor!!.close()
            } catch (ex: java.lang.Exception) {
                return def
            }
            return def
        }

        @SuppressLint("Range")
        fun readTISSettingsIntValues(
            context: Context, id: String, AUTHORITY: String?, general: String?, def: Int
        ): Int {
            var def = def
            var cursor: Cursor? = null
            val URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "readTISSettingsIntValues: %s %s %d $URI.toString(), $id, $def")
            try {
                cursor = context.contentResolver.query(URI, arrayOf(id), null, null, null)
                if (cursor == null) {
                    return def
                }
                if (cursor.moveToNext()) {
                    def = cursor.getInt(cursor.getColumnIndex(id))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "readTISSettingsIntValues return: %d $def")
                }
            } catch (ex: java.lang.Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, Log.getStackTraceString(ex))
            }
            try {
                cursor!!.close()
            } catch (ex: java.lang.Exception) {
                return def
            }
            return def
        }

        fun saveTISSettingsIntValue(
            context: Context, id: String?, AUTHORITY: String?, general: String?, value: Int
        ): Boolean {
            val URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "saveTISSettingsIntValue : %s %s %d $URI $id, $value")
            try {
                val values = ContentValues()
                values.put(id, value)
                context.contentResolver.update(URI, values, null, null)
            } catch (ex: java.lang.Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, Log.getStackTraceString(ex))
                return false
            }
            return true
        }

        fun saveWorldValue(context: Context, id: String?, value: String?, isStored: Boolean): Boolean {
            val values = ContentValues()
            values.put(EasControl.GLOBAL_VALUE_KEY, id)
            values.put(EasControl.GLOBAL_VALUE_VALUE, value)
            values.put(
                EasControl.GLOBAL_VALUE_STORED,
                isStored
            )

            try {
                context.contentResolver.insert(
                    EasControl.GLOBAL_PROVIDER_URI,
                    values
                )
                return true
            } catch (ex: java.lang.Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "saveWorldValue: ex : ${ex.message}")
            }
            return false
        }

        fun readWorldStringValue(context: Context, id: String?): String? {
            var cursor: Cursor? = null
            var value = ""
            val uri = Uri.withAppendedPath(GLOBAL_PROVIDER_URI_URI, id)
            try {
                cursor = context.contentResolver.query(uri, null, null, null, null)
                if (cursor == null) {
                    return value
                }
                if (cursor.moveToNext()) {
                    value = cursor.getString(1)
                }
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, ""+ex)
            }
            try {
                cursor!!.close()
            } catch (ex: Exception) {
                return value
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "readWorldStringValue :$uri||$value")
            return value
        }
    }
}