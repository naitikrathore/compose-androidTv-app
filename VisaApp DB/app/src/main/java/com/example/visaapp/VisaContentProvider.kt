package com.example.visaapp

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns

class VisaContentProvider : ContentProvider() {
    companion object {
        const val VISAS = 100
        const val VISA_ID = 101
        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(VisaContract.CONTENT_AUTHORITY, VisaContract.VisaEntry.TABLE_NAME, VISAS)
            addURI(
                VisaContract.CONTENT_AUTHORITY,
                "${VisaContract.VisaEntry.TABLE_NAME}/#",
                VISA_ID
            )
        }

    }

    private lateinit var db: HelperDB

    override fun onCreate(): Boolean {
        db = HelperDB(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val dbH = db.readableDatabase
        val cursor: Cursor = when (sUriMatcher.match(uri)) {
            VISAS -> dbH.query(
                VisaContract.VisaEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
            )

            VISA_ID -> {
                val id = ContentUris.parseId(uri)
                dbH.query(
                    VisaContract.VisaEntry.TABLE_NAME,
                    projection,
                    "${BaseColumns._ID}=?",
                    arrayOf(id.toString()),
                    null,
                    null,
                    sortOrder
                )
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (sUriMatcher.match(uri)) {
            VISAS -> "vnd.android.cursor.dir/${VisaContract.CONTENT_AUTHORITY}.${VisaContract.VisaEntry.TABLE_NAME}"
            VISA_ID -> "vnd.android.cursor.item/${VisaContract.CONTENT_AUTHORITY}.${VisaContract.VisaEntry.TABLE_NAME}"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = db.writableDatabase
        val id: Long = when (sUriMatcher.match(uri)) {
            VISAS -> db.insert(VisaContract.VisaEntry.TABLE_NAME, null, values)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        context?.contentResolver?.notifyChange(uri, null)
        return ContentUris.withAppendedId(uri, id)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = db.writableDatabase
        val rowsDeleted: Int = when (sUriMatcher.match(uri)) {
            VISAS -> db.delete(VisaContract.VisaEntry.TABLE_NAME, selection, selectionArgs)
            VISA_ID -> {
                val id = ContentUris.parseId(uri)
                db.delete(VisaContract.VisaEntry.TABLE_NAME, "${BaseColumns._ID}=?", arrayOf(id.toString()))
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        if (rowsDeleted != 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return rowsDeleted
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = db.writableDatabase
        val rowsUpdated: Int = when (sUriMatcher.match(uri)) {
            VISAS -> db.update(VisaContract.VisaEntry.TABLE_NAME, values, selection, selectionArgs)
            VISA_ID -> {
                val id = ContentUris.parseId(uri)
                db.update(VisaContract.VisaEntry.TABLE_NAME, values, "${BaseColumns._ID}=?", arrayOf(id.toString()))
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        if (rowsUpdated != 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }
        return rowsUpdated
    }
}