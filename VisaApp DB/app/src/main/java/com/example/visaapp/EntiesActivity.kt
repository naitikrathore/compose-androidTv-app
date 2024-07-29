package com.example.visaapp

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.BaseColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visaapp.databinding.ActivityEntiesBinding
import com.example.visaapp.databinding.ActivityMainBinding
import androidx.recyclerview.widget.RecyclerView

class EntiesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEntiesBinding
    private lateinit var adapter: VisaAdapter

    //    private lateinit var dbHelper: HelperDB
    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            Log.e("Naitik", "on changed callleddd")
            loadEntries()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        dbHelper = HelperDB(this)
        adapter = VisaAdapter(null)
        binding.rvVisa.adapter = adapter
        binding.rvVisa.layoutManager = LinearLayoutManager(this)

        val uri = VisaContract.VisaEntry.CONTENT_URI
        contentResolver.registerContentObserver(uri, true, contentObserver)
        loadEntries()

        binding.btnDel.setOnClickListener {
//            adapter.deleteselected()
            if (adapter.isAnyItemSelected()) {
                deleteSelectedItemsFromDb()
//                loadEntries()
            } else {
                Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show()
            }
//            val dataFromDb = dbHelper.getAllData()
//            adapter.updateData(dataFromDb.toMutableList())
        }


    }

    private fun deleteSelectedItemsFromDb() {
        val positionsToRemove = mutableListOf<Int>()
        for (i in adapter.selected.size() - 1 downTo 0) {
            val position = adapter.selected.keyAt(i)
            val cursor = adapter.cursor ?: return // Get current cursor from adapter
            if (cursor.moveToPosition(position)) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
                val uri = ContentUris.withAppendedId(VisaContract.VisaEntry.CONTENT_URI, id)
                contentResolver.delete(uri, null, null)
                positionsToRemove.add(position)
            }
        }
        positionsToRemove.forEach {
            adapter.notifyItemRemoved(it)
        }
        adapter.selected.clear()
    }

    private fun loadEntries() {
        val cursor = contentResolver.query(
            VisaContract.VisaEntry.CONTENT_URI,
            null,
            null,
            null,
            null,
            null
        )
        adapter.updateData(cursor)
//        val data=dbHelper.getAllData()
//        adapter.updateData(data.toMutableList())
    }


    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "SecondActivity: onStart()")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("Lifecycle", "MainActivity: onRestart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "SecondActivity: onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d("Lifecycle", "SecondActivity: onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Lifecycle", "SecondActivity: onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(contentObserver)
        adapter.closeCursor()
        Log.d("Lifecycle", "SecondActivity: onDestroy()")
    }


}