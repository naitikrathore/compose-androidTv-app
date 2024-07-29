package com.example.cppractise

import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cppractise.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var miliYes: Long = 0

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange)
            loadimage()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri =MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        contentResolver.registerContentObserver(uri, true, contentObserver)

        miliYes = calculatemili()
        loadimage()



    }

    private fun calculatemili(): Long {
        val milisYes = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        return milisYes
    }

    private fun loadimage() {
        val imagelist= mutableListOf<DataImage>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
        )

        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
//        the condition for question mark is added on selectionArgs
        val selectionArgs = arrayOf(miliYes.toString())
        val sortorder="${MediaStore.Images.Media.DATE_ADDED} DESC"
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortorder
        )?. use{cursor ->
//            whoch colm you need
            val idcolm=cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val namecolm=cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

//            now for each colm entered go to its all rows
              while(cursor.moveToNext()){
                  var id=cursor.getLong(idcolm)
                  val name=cursor.getString(namecolm)
//                  uri inside while loop in loadimage(): Created using withAppendedId() to uniquely identify each image retrieved from the media store.
                  val uri=ContentUris.withAppendedId(
                      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                      id
                  )
                  imagelist.add(DataImage(id,name,uri))

              }
        }

    }


    override fun onDestroy() {
        // Unregister ContentObserver
        contentResolver.unregisterContentObserver(contentObserver)
        super.onDestroy()
    }
}