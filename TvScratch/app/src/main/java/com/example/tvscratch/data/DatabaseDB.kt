package com.example.tvscratch.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.example.tvscratch.data.MyDataEntry.TABLE_NAME

object MyDataEntry : BaseColumns {
    const val TABLE_NAME = "myAppData"
    const val COLUMN_TITLE = "title"
    const val COLUMN_POSTER_PATH = "posterPath"
    const val COLUMN_DESCRIPTION = "description"
    const val COLUMN_ISFAV = "isFav"
    const val COLUMN_IS_RECENT="isRecent"
}


class DatabaseDB (context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABSE_VERSION) {
    companion object {
        const val DATABASE_NAME = "TvAppData"
        const val DATABSE_VERSION = 3
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_ENTRY_TABLE = """
           CREATE TABLE ${MyDataEntry.TABLE_NAME}(
           ${BaseColumns._ID} INTEGER PRIMARY KEY,
           ${MyDataEntry.COLUMN_TITLE} TEXT,
           ${MyDataEntry.COLUMN_POSTER_PATH} INT,
           ${MyDataEntry.COLUMN_DESCRIPTION} TEXT,
           ${MyDataEntry.COLUMN_ISFAV} INT,
           ${MyDataEntry.COLUMN_IS_RECENT} INT
           )
       """.trimIndent()
        db?.execSQL(CREATE_ENTRY_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${MyDataEntry.TABLE_NAME}")
        onCreate(db)
    }

    fun insertData(entry: Movie): Long {
        val values = ContentValues().apply {
            put(MyDataEntry.COLUMN_TITLE, entry.title)
            put(MyDataEntry.COLUMN_POSTER_PATH, entry.img)
            put(MyDataEntry.COLUMN_DESCRIPTION, entry.description)
            put(MyDataEntry.COLUMN_ISFAV, entry.isFav)
            put(MyDataEntry.COLUMN_IS_RECENT,entry.isRecent)
        }
        val db = writableDatabase
        val id = db.insert(MyDataEntry.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getData(): List<Movie> {
        val entries = mutableListOf<Movie>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            val title =
                cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_TITLE))
            val desc =
                cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_DESCRIPTION))
            val poster =
                cursor.getInt(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_POSTER_PATH))
            val isFav =
                cursor.getInt(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_ISFAV))
            val isRecent =
                cursor.getInt(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_IS_RECENT))
            entries.add(Movie(id, title, desc, poster, isFav, isRecent))
        }
        cursor.close()
        db.close()
        return entries
    }

    fun updateData(entry: Movie): Int {
        val values = ContentValues().apply {
            put(MyDataEntry.COLUMN_TITLE, entry.title)
            put(MyDataEntry.COLUMN_DESCRIPTION, entry.description)
            put(MyDataEntry.COLUMN_POSTER_PATH, entry.img)
            put(MyDataEntry.COLUMN_ISFAV, entry.isFav)
            put(MyDataEntry.COLUMN_IS_RECENT,entry.isRecent)
        }

        val db = writableDatabase
        val result = db.update(
            MyDataEntry.TABLE_NAME,
            values,
            "${BaseColumns._ID}=?",
            arrayOf(entry.id.toString())
        )
        db.close()
        return result
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