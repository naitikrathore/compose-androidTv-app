package com.example.visaapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.example.visaapp.MyDataEntry.TABLE_NAME


object MyDataEntry : BaseColumns {
    const val TABLE_NAME = "visadata"
    const val COLUMN_FIRST_NAME = "first_name"
    const val COLUMN_LAST_NAME = "last_name"
    const val COLUMN_COUNTRY = "country"
}

class HelperDB(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "VisaData.db"
        const val DATABASE_VERSION = 1

    }

    override fun onCreate(db: SQLiteDatabase?) {
        val sql_create_entries = """
          CREATE TABLE ${MyDataEntry.TABLE_NAME}(
               ${BaseColumns._ID} INTEGER PRIMARY KEY,
               ${MyDataEntry.COLUMN_FIRST_NAME} TEXT,
                ${MyDataEntry.COLUMN_LAST_NAME} TEXT,
                ${MyDataEntry.COLUMN_COUNTRY} TEXT
          )
      """.trimIndent()
        db?.execSQL(sql_create_entries)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(firstName: String, lastName: String, country: String): Long {
        val values = ContentValues().apply {
            put(MyDataEntry.COLUMN_FIRST_NAME, firstName)
            put(MyDataEntry.COLUMN_LAST_NAME, lastName)
            put(MyDataEntry.COLUMN_COUNTRY, country)
        }
        val db = writableDatabase
        val id = db.insert(MyDataEntry.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getAllData(): List<MyData> {
        val dataList = mutableListOf<MyData>()
        val db = readableDatabase
        val projection = arrayOf(
            BaseColumns._ID,
            MyDataEntry.COLUMN_FIRST_NAME,
            MyDataEntry.COLUMN_LAST_NAME,
            MyDataEntry.COLUMN_COUNTRY
        )
        val cursor: Cursor = db.query(
            MyDataEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            null
        )
        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
                val firstName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_FIRST_NAME))
                val lastName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_LAST_NAME))
                val country =
                    cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_COUNTRY))
                dataList.add(MyData(id, firstName, lastName, country))
            }
        }
        db.close()
        return dataList
    }

    fun deleteData(id: Long): Int {
        val db = this.writableDatabase
        val result = db.delete(
            MyDataEntry.TABLE_NAME,
            "${BaseColumns._ID}=?",
            arrayOf(id.toString())
        )
        db.close()
        return result
    }
}


//
//
//
//
//
//
//import android.content.ContentValues
//import android.content.Context
//import android.database.sqlite.SQLiteDatabase
//import android.database.sqlite.SQLiteOpenHelper
//import com.example.visaapp.MyData
//
//class DatabaseHelper(context: Context) :
//    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
//
//    companion object {
//        private const val DATABASE_VERSION = 1
//        private const val DATABASE_NAME = "VisaAppDB"
//        private const val TABLE_NAME = "VisaEntries"
//        private const val KEY_ID = "id"
//        private const val KEY_FIRST_NAME = "first_name"
//        private const val KEY_LAST_NAME = "last_name"
//        private const val KEY_COUNTRY = "country"
//    }
//
//    override fun onCreate(db: SQLiteDatabase?) {
//        val createTable = ("CREATE TABLE $TABLE_NAME ($KEY_ID INTEGER PRIMARY KEY, " +
//                "$KEY_FIRST_NAME TEXT, $KEY_LAST_NAME TEXT, $KEY_COUNTRY TEXT)")
//        db?.execSQL(createTable)
//    }
//
//    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
//        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
//        onCreate(db)
//    }
//
//    fun insertData(data: MyData): Long {
//        val db = this.writableDatabase
//        val contentValues = ContentValues().apply {
//            put(KEY_FIRST_NAME, data.FirstName)
//            put(KEY_LAST_NAME, data.LastName)
//            put(KEY_COUNTRY, data.Country)
//        }
//        val id = db.insert(TABLE_NAME, null, contentValues)
//        db.close()
//        return id
//    }
//
//    fun getAllData(): List<MyData> {
//        val dataList = mutableListOf<MyData>()
//        val db = this.readableDatabase
//        val query = "SELECT * FROM $TABLE_NAME"
//        val cursor = db.rawQuery(query, null)
//        cursor.use {
//            while (it.moveToNext()) {
//                val id = it.getInt(it.getColumnIndex(KEY_ID))
//                val firstName = it.getString(it.getColumnIndex(KEY_FIRST_NAME))
//                val lastName = it.getString(it.getColumnIndex(KEY_LAST_NAME))
//                val country = it.getString(it.getColumnIndex(KEY_COUNTRY))
//                dataList.add(MyData(firstName, lastName, country, id))
//            }
//        }
//        db.close()
//        return dataList
//    }
//
//    fun deleteData(id: Int): Int {
//        val db = this.writableDatabase
//        val result = db.delete(TABLE_NAME, "$KEY_ID=?", arrayOf(id.toString()))
//        db.close()
//        return result
//    }
//}
