package com.example.videoplayer.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.example.videoplayer.data.MyDataEntry.TABLE_NAME
import javax.inject.Inject

object MyDataEntry : BaseColumns {
    const val TABLE_NAME = "youtubedata"
    const val COLUMN_NAME = "name"
    const val COLUMN_LINK = "link"
    const val COLUMN_UPLOADER_NAME = "channelName"
    const val COLUMN_THUMBNAIL_PATH="thumbnailPath"
    const val COLUMN_PROFILE_PIC_PATH="profilepicPath"
    const val COLUMN_ISFAV="isfav"
}

object MyUserEntry : BaseColumns {
    const val TABLE_NAME = "userdata"
    const val COLUMN_USERNAME="username"
    const val COLUMN_PASSWORD="password"
}

class DatabaseDB (context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "YouTubeDB"
        const val DATABASE_VERSION = 6
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_ENTRIES_TABLE = """
            CREATE TABLE ${MyDataEntry.TABLE_NAME}(
               ${BaseColumns._ID} INTEGER PRIMARY KEY,
               ${MyDataEntry.COLUMN_NAME} TEXT,
                ${MyDataEntry.COLUMN_LINK} TEXT,
                ${MyDataEntry.COLUMN_UPLOADER_NAME} TEXT,
                ${MyDataEntry.COLUMN_THUMBNAIL_PATH}  TEXT,
                ${MyDataEntry.COLUMN_PROFILE_PIC_PATH} TEXT,
                ${MyDataEntry.COLUMN_ISFAV} INT
          )
        """.trimIndent()


        val CREATE_USER_TABLE = """
        CREATE TABLE ${MyUserEntry.TABLE_NAME}(
            ${BaseColumns._ID} INTEGER PRIMARY KEY,
            ${MyUserEntry.COLUMN_USERNAME} TEXT UNIQUE,
            ${MyUserEntry.COLUMN_PASSWORD} TEXT
        )
          """.trimIndent()


         db?.execSQL(CREATE_ENTRIES_TABLE)
         db?.execSQL(CREATE_USER_TABLE)

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${MyDataEntry.TABLE_NAME}")
        db?.execSQL("DROP TABLE IF EXISTS ${MyUserEntry.TABLE_NAME}")

        onCreate(db)
    }

    fun insertData(entry: Entry): Long {
        val values = ContentValues().apply {
            put(MyDataEntry.COLUMN_NAME, entry.name)
            put(MyDataEntry.COLUMN_LINK, entry.link)
            put(MyDataEntry.COLUMN_UPLOADER_NAME, entry.uploader)
            put(MyDataEntry.COLUMN_THUMBNAIL_PATH,entry.thumbnailPath)
            put(MyDataEntry.COLUMN_PROFILE_PIC_PATH,entry.profilePicture)
            put(MyDataEntry.COLUMN_ISFAV,entry.isFav)
        }
        val db = writableDatabase
        val id = db.insert(MyDataEntry.TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun insertUser(username:String, password:String):Long{
        val values=ContentValues().apply {
            put(MyUserEntry.COLUMN_USERNAME,username)
            put(MyUserEntry.COLUMN_PASSWORD,password)
        }
        val db=writableDatabase
        val id=db.insert(MyUserEntry.TABLE_NAME,null,values)
        db.close()
        return id
    }
    fun verifyUser(username: String,password: String):Boolean{
        val db=readableDatabase
        val cursor=db.query(
            MyUserEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID),
            "${MyUserEntry.COLUMN_USERNAME} =? AND ${MyUserEntry.COLUMN_PASSWORD}=?",
            arrayOf(username,password),
            null,null,null
        )
        val exists=cursor.count>0
        cursor.close()
        db.close()
        return exists
    }

    fun getData(): List<Entry> {
        val entries = mutableListOf<Entry>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            val name =
                cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_NAME))
            val link =
                cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_LINK))
            val uploader =
                cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_UPLOADER_NAME))
            val thumbnailPath =
                cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_THUMBNAIL_PATH))
            val profilePicturePath =
                cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_PROFILE_PIC_PATH))
            val isFav =
                cursor.getInt(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_ISFAV))
            entries.add(Entry(id, name, link, uploader, thumbnailPath, profilePicturePath, isFav ))
        }
        cursor.close()
        db.close()
        return entries
    }
    fun updateData(entry: Entry): Int {
        val values = ContentValues().apply {
            put(MyDataEntry.COLUMN_NAME, entry.name)
            put(MyDataEntry.COLUMN_LINK, entry.link)
            put(MyDataEntry.COLUMN_UPLOADER_NAME, entry.uploader)
            put(MyDataEntry.COLUMN_THUMBNAIL_PATH, entry.thumbnailPath)
            put(MyDataEntry.COLUMN_PROFILE_PIC_PATH, entry.profilePicture)
            put(MyDataEntry.COLUMN_ISFAV, entry.isFav)
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
