package com.example.contactapp

import ContactAdapter
import android.Manifest
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    //    declares the MainActivity class, which extends AppCompatActivity. MainActivity is the entry point of your app and extends AppCompatActivity
    private lateinit var binding: ActivityMainBinding

    //    This line declares a private variable binding of type ActivityMainBinding. This variable will hold the binding object for the activity's layout, allowing you to access views defined in the layout XML file.
    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            Log.e("Naitik", "on changed callleddd")
            loadContacts()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//    creates a mutable list of DataContact objects named contact. This list will store the contacts fetched from the device.
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 200)

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        contentResolver.registerContentObserver(uri, true, contentObserver)
        loadContacts()
    }


    private fun loadContacts() {
        val contact = mutableListOf<DataContact>()
//        val projection = arrayOf(
//            ContactsContract.CommonDataKinds.Phone._ID,
//            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
//            ContactsContract.CommonDataKinds.Phone.NUMBER
//        )

        //contentobserver
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
//            cursor.moveToFirst()
            Log.e("Naitik", "cursor size: " + cursor.count)
            val idcol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val namecol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numcol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idcol)
                val name = cursor.getString(namecol)
                val num = cursor.getString(numcol)
                contact.add(DataContact(id, name, num))
            }

        }
        binding.rvContact.apply {
            adapter = ContactAdapter()
            layoutManager = LinearLayoutManager(this@MainActivity)
            (binding.rvContact.adapter as ContactAdapter).setContacts(contact)
        }
    }

    override fun onDestroy() {
        // Unregister ContentObserver
        contentResolver.unregisterContentObserver(contentObserver)
        super.onDestroy()
    }

}