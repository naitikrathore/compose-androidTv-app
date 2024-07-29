package com.iwedia.cltv.platform.model.content_provider

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.Log
import com.iwedia.cltv.platform.model.Constants
import java.lang.IllegalArgumentException

class PromoProvider : ContentProvider() {

    private var db : SQLiteDatabase? = null

    companion object {
        private const val TAG: String = "PromoProvider"
        private const val AUTHORITY = "com.iwedia.cltv.platform.model.content_provider.PromoProvider"
        private const val PROMO_TABLE = "promotions"
        val PROMO_URI : Uri = Uri.parse("content://$AUTHORITY/$PROMO_TABLE")
        val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private const val PROMO = 1
        private const val PROMO_ID = 2
        const val DATABASE_NAME = "promotion.db"
        const val DATABASE_VERSION = 1
        var PROMO_PROJECTION_MAP: HashMap<String, String> = HashMap()
    }
    init {
        sUriMatcher.addURI(AUTHORITY, PROMO_TABLE, PROMO)
        sUriMatcher.addURI(AUTHORITY, "$PROMO_TABLE/#", PROMO_ID)
        PROMO_PROJECTION_MAP[BaseColumns._ID] = PROMO_TABLE + "." + BaseColumns._ID
        PROMO_PROJECTION_MAP[Contract.PromoBanner.SUPPORTED_COUNTRY_COLUMN] = PROMO_TABLE + "." + Contract.PromoBanner.SUPPORTED_COUNTRY_COLUMN
    }

    private class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        @SuppressLint("LongLogTag")
        override fun onCreate(p0: SQLiteDatabase?) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Path of DB = ${p0!!.path}")
            p0.execSQL("CREATE TABLE $PROMO_TABLE ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + Contract.PromoBanner.SUPPORTED_COUNTRY_COLUMN + " TEXT NOT NULL,"
                    + "UNIQUE(${BaseColumns._ID})"
                    +");")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Created promo banner table.")
        }
        @SuppressLint("LongLogTag")
        override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
            Log.i(TAG, "Upgrading from version $p1 to $p2, data will be lost")
            p0!!.execSQL("DROP TABLE IF EXISTS $PROMO_TABLE")
            onCreate(p0)
        }
    }

    private lateinit var mOpenHelper : DBHelper

    @SuppressLint("LongLogTag")
    override fun onCreate(): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate - create new DB")
        mOpenHelper = DBHelper(context!!)
        db = mOpenHelper.writableDatabase
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val queryBuilder = SQLiteQueryBuilder()
        when (sUriMatcher.match(uri)) {
            PROMO -> {
                queryBuilder.tables = PROMO_TABLE
                queryBuilder.projectionMap = PROMO_PROJECTION_MAP
            }
            PROMO_ID -> {
                queryBuilder.tables = PROMO_TABLE
                queryBuilder.appendWhere("${BaseColumns._ID}=${uri.pathSegments[1]}")
            }
        }
        val sortOrderTemp : String = if (sortOrder == null || sortOrder == "") {
            BaseColumns._ID
        } else {
            sortOrder
        }
        db = mOpenHelper.readableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs,null,null, sortOrderTemp)
        cursor.setNotificationUri(context!!.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String {
        return when (sUriMatcher.match(uri)) {
            PROMO -> Contract.PromoBanner.CONTENT_TYPE
            PROMO_ID -> Contract.PromoBanner.CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("Unknown URI + $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        when(sUriMatcher.match(uri)){
            PROMO -> {
                return insertPromoBanners(uri, values!!)
            }
            else -> throw IllegalArgumentException("Unknown uri $uri")
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        db = mOpenHelper.writableDatabase
        val count: Int = when (sUriMatcher.match(uri)) {
            PROMO ->  {
                db!!.delete(PROMO_TABLE, selection, selectionArgs)
            }
            PROMO_ID -> {
                val id = uri.pathSegments[1]
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                db!!.delete(
                    PROMO_TABLE, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs
                )
            }
            else -> throw IllegalArgumentException("Unknown URI + $uri")
        }

        return count
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        db = mOpenHelper.writableDatabase
        val count: Int = when (sUriMatcher.match(uri)) {
            PROMO -> {
                db!!.update(PROMO_TABLE, values, selection, selectionArgs)
            }
            PROMO_ID -> {
                val id = uri.pathSegments[1]
                val tempSelection = if (!TextUtils.isEmpty(selection)) " AND ($selection)" else ""
                db!!.update(PROMO_TABLE, values, "${BaseColumns._ID} = $id" + tempSelection, selectionArgs)
            }
            else -> throw IllegalArgumentException("Unknown URI + $uri")
        }
        return count
    }

    private fun insertPromoBanners(uri: Uri, values: ContentValues) : Uri {
        db = mOpenHelper.writableDatabase
        val rowId : Long = db!!.insert(PROMO_TABLE, null, values)
        if (rowId > 0) {
            return Contract.buildPromoUri(rowId)
        }
        throw  SQLException("Failed to insert row into $uri")
    }
}