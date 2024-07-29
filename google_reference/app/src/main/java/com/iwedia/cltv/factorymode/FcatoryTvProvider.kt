package com.iwedia.cltv.factorymode

import android.content.ContentProvider
import android.content.ContentValues
import android.content.SharedPreferences
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import com.iwedia.cltv.platform.model.Constants


class FactoryTvProvider : ContentProvider() {
    companion object {
        private const val DEBUG = true
        private var firstTime = true
        private const val TAG = "FactoryTvProvider"
        private const val AUTHORITY = "com.iwedia.cltv.factorymode.FactoryTvProvider"
        const val KEY_FACTORY_TV_FLAG = "factory_tv_flag"
        private const val MATCH_FACTORY_TV_FLAG = 1
        const val FACTORY_TV_FLAG_OFF = 0
        const val FACTORY_TV_FLAG_ON = 1
        private var sUriMatcher: UriMatcher? = null
        init {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher!!.addURI(AUTHORITY, KEY_FACTORY_TV_FLAG, MATCH_FACTORY_TV_FLAG)
        }
    }
    private var mSpref: SharedPreferences? = null
    override fun onCreate(): Boolean {
        mSpref = PreferenceManager.getDefaultSharedPreferences(context)
        if (firstTime) {
            val all: Map<*, *> = mSpref!!.all
            if (!all.containsKey(KEY_FACTORY_TV_FLAG)) {
                mSpref!!.edit().putInt(KEY_FACTORY_TV_FLAG, FACTORY_TV_FLAG_OFF).apply()
            }
            firstTime = false
        }
        return true
    }
    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        throw UnsupportedOperationException()
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor {
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "query : uri = $uri matched uri ${sUriMatcher?.match(uri)}")
        return when (sUriMatcher?.match(uri)) {
            MATCH_FACTORY_TV_FLAG -> {
                val cursor = MatrixCursor(
                    arrayOf(
                        "key", "value"
                    )
                )
                val all: Map<*, *> = mSpref!!.all
                var value: Any?
                for (key in projection!!) {
                    if (all.containsKey(key)) {
                        value = all[key]
                        cursor.addRow(
                            arrayOf(
                                key, value
                            )
                        )
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "added row :$key $value")
                    }
                }
                cursor
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"update : uri = $uri" )
        when (sUriMatcher?.match(uri)) {
            MATCH_FACTORY_TV_FLAG -> {
                if (values!!.containsKey(KEY_FACTORY_TV_FLAG)) {
                    val flag = values.getAsInteger(KEY_FACTORY_TV_FLAG)
                    if (mSpref != null) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "set factoryTv flag : $flag"
                        )
                        mSpref!!.edit().putInt(KEY_FACTORY_TV_FLAG, flag).apply()
                    }
                }
                context!!.contentResolver.notifyChange(uri, null)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        return 0
    }
    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException()
    }
}
