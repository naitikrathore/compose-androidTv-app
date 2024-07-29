package com.iwedia.cltv.platform.refplus5

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.base.SubtitleInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants


class SubtitleInterfaceImpl(private var context: Context, utilsInterface: UtilsInterface, playerInterface: PlayerInterface):
        SubtitleInterfaceBaseImpl(context, utilsInterface, playerInterface) {
        private val TAG = javaClass.simpleName
        private val AUTHORITY = Constants.AUTHORITY
        private val GENERAL = "general"
        private val subtitleTable = String.format("content://%s/%s", AUTHORITY, GENERAL)

        @RequiresApi(Build.VERSION_CODES.R)
        override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo): Boolean {
                var hasHoHKey= false
                try {
                        if (tvTrackInfo.isHardOfHearing) {
                                hasHoHKey = true
                        }
                } catch (e: java.lang.Exception) { }
                return hasHoHKey
        }

        override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
                if(updateSwitch){
                        saveTISSettingsIntValue(context, "subtitle_display", "com.mediatek.tis.settings.analog", "common", 1)
                }else{
                        saveTISSettingsIntValue(context, "subtitle_display", "com.mediatek.tis.settings.analog", "common", 0)
                }
        }

        override fun getSubtitlesType(): Int {
                return readTISSettingsIntValues(context, "subtitle_display", "com.mediatek.tis.settings.analog", "common", 0)
        }

        @SuppressLint("Range")
        override fun getSubtitlesState(): Boolean {
                var subtitleValue = getSubtitlesType()

                if (subtitleValue == 1) {
                        return true
                }
                return false
        }

        override fun enableSubtitles(enable: Boolean) {
        }

        override fun setAnalogSubtitlesType(value: String) {
        }

        override fun getAnalogSubtitlesType(): String? {
                return ""
        }

        private fun saveTISSettingsStringValue(
                context: Context, id: String?, AUTHORITY: String?, general: String?, value: String?): Boolean {
                val uri = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))

                try {
                        val values = ContentValues()
                        values.put(id, value)
                        context.contentResolver.update(uri, values, null, null)
                } catch (ex: java.lang.Exception) {
                        return false
                }
                return true
        }

        @SuppressLint("Range")
        private fun readTISSettingsStringValues(
                context: Context, id: String, AUTHORITY: String?, general: String?, def: String): String? {
                var def = def
                var cursor: Cursor? = null
                val uri = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))

                try {
                        cursor = context.contentResolver.query(uri, arrayOf(id), null, null, null)
                        if (cursor == null) {
                                return def
                        }
                        if (cursor.moveToNext()) {
                                def = cursor.getString(cursor.getColumnIndex(id))
                        }
                } catch (ex: java.lang.Exception) {
                }
                try {
                        cursor!!.close()
                } catch (ex: java.lang.Exception) {
                        return def
                }
                return def
        }

        private fun saveTISSettingsIntValue(context: Context, id: String?, AUTHORITY: String?, general: String?, value: Int): Boolean {
                val uri = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))

                try {
                        val values = ContentValues()
                        values.put(id, value)
                        context.contentResolver.update(uri, values, null, null)
                } catch (ex: java.lang.Exception) {
                        return false
                }
                return true
        }

        @SuppressLint("Range")
        private fun readTISSettingsIntValues(context: Context, id: String, AUTHORITY: String?, general: String?, def: Int): Int {
                var cursor: Cursor? = null
                val URI = Uri.parse(java.lang.String.format("content://%s/%s", AUTHORITY, general))
                var retDef = def
                try {
                        cursor = context.contentResolver.query(URI, arrayOf(id), null, null, null)
                        if (cursor == null) {
                                return def
                        }
                        if (cursor.moveToNext()) {
                                retDef = cursor.getInt(cursor.getColumnIndex(id))
                                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "readTISSettingsStringValues return:$def")
                        }
                } catch (ex: Exception) {
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, Log.getStackTraceString(ex))
                }

                try {
                        cursor!!.close()
                } catch (ex: Exception) {
                        return retDef
                }

                return retDef

        }
}