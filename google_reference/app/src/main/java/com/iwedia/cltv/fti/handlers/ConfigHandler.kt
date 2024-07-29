package com.iwedia.cltv.fti.handlers

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import java.io.File
import java.io.FileOutputStream


class ConfigHandler {

    companion object {
        private var instance: ConfigHandler? = null
        private val TAG = "ConfigHandler"
        private val CONFIG_URI: Uri =
            Uri.parse("content://com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider/config")

        private var mContext: Context? = null
        private var mContentResolver: ContentResolver? = null


        @SuppressLint("Range")
        fun getLcnValue(): String? {
            val cursor: Cursor = mContext!!.getContentResolver().query(CONFIG_URI, null, null, null, null)!!
            return if (cursor != null) {
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndex("lcn"))
                } else {
                    null
                }
            } else null
        }

        @SuppressLint("Range")
        fun getPinValue(): String? {
            val cursor: Cursor = mContext!!.getContentResolver().query(CONFIG_URI, null, null, null, null)!!
            return if (cursor != null) {
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndex("pin"))
                } else {
                    null
                }
            } else null
        }

        @SuppressLint("Range")
        fun getCountryValue(): String? {
            val cursor: Cursor = mContext!!.getContentResolver().query(CONFIG_URI, null, null, null, null)!!
            return if (cursor != null) {
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndex("current_country"))
                } else {
                    null
                }
            } else null
        }

        fun setLcnValue(lcnEnabled: Boolean) {
            val enabled = if (lcnEnabled) "true" else "false"
            val cursor: Cursor =
                mContext!!.getContentResolver().query(CONFIG_URI, null, null, null, null)!!
            if (cursor.moveToFirst()) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Update existing row with new pin")
                val uri: Uri = Uri.withAppendedPath(CONFIG_URI, "/1")
                val cv = ContentValues()
                cv.put("lcn", enabled)
                val where = "_id" + " =?"
                val args = arrayOf(
                    "1"
                )
                val result: Int = mContext!!.getContentResolver().update(uri, cv, where, args)
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Insert new element in config table")
                val cv = ContentValues()
                cv.put("lcn", enabled)
                val uri: Uri = mContext!!.getContentResolver().insert(CONFIG_URI, cv)!!
            }
            cursor.close()
        }

        fun setPinValue(newPin: String?) {
            val cursor: Cursor = mContext!!.getContentResolver().query(CONFIG_URI, null, null, null, null)!!
            if (cursor.moveToFirst()) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Update existing row with new pin")
                val uri: Uri = Uri.withAppendedPath(CONFIG_URI, "/1")
                val cv = ContentValues()
                cv.put("pin", newPin)
                val where = "_id" + " =?"
                val args = arrayOf(
                    "1"
                )
                val result: Int = mContext!!.getContentResolver().update(uri, cv, where, args)
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Insert new element in config table")
                val cv = ContentValues()
                cv.put("pin", newPin)
                cv.put("lcn", "false")
                val uri: Uri = mContext!!.getContentResolver().insert(CONFIG_URI, cv)!!
            }
            cursor.close()
        }

        fun setCountryValue(tag: String?) {
            val configUri: Uri = CONFIG_URI
            val cursor: Cursor = ReferenceApplication.applicationContext().contentResolver.query(configUri, null, null, null, null)!!
            if (cursor.moveToFirst()){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Update existing row with new country tag")
                val uri: Uri = Uri.withAppendedPath(configUri, "/1")
                val cv = ContentValues()
                cv.put("current_country", tag)
                val where = "_id" + " =?"
                val args = arrayOf("1")
                val result: Int = ReferenceApplication.applicationContext().contentResolver.update(uri, cv, where, args)
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Inserting new element in config table")
                val cv = ContentValues()
                cv.put("current_country", tag)
                val uri: Uri = ReferenceApplication.applicationContext().contentResolver.insert(configUri,cv)!!
            }
            cursor.close()
        }

        @SuppressLint("Range")
        fun getCompanyLogo(): String {
            val contentResolver: ContentResolver = ReferenceApplication.applicationContext().contentResolver
            var companyLogo = ""
            var cursor = contentResolver.query(
                ContentProvider.OEM_CUSTOMIZATION_URI,
                null,
                null,
                null,
                null
            )
            if (cursor!!.count > 0) {
                cursor.moveToFirst()

                if (cursor.getBlob(cursor.getColumnIndex(Contract.OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN)) != null) {
                    var blob = cursor.getBlob(cursor.getColumnIndex(
                        Contract.OemCustomization.BRANDING_CHANNEL_LOGO_COLUMN))

                    var temp = String(blob)
                    if (temp.startsWith("//")) {
                        companyLogo = "https:$temp"
                    } else  if (blob != null){
                        try {
                            val bitmap: Bitmap =
                                BitmapFactory.decodeByteArray(blob, 0, blob.size)

                            var file = File(ReferenceApplication.applicationContext().filesDir, "companyLogo");
                            var fos = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            fos.flush()
                            fos.close()
                            companyLogo = file.path
                        } catch (e: Exception) {
                            companyLogo = "no_image"
                        }
                    } else {
                        companyLogo = "no_image"
                    }
                }
            }
            cursor.close()
            return companyLogo
        }
    }
}