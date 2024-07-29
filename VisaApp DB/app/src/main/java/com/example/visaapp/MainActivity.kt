package com.example.visaapp

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.visaapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
//    private lateinit var dbHelper: HelperDB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        dbHelper=HelperDB(this)
        Log.d("LifeCycle", "MainActivity: OnCreate")

        binding.btappply.setOnClickListener {
            adddata(it)
        }

        binding.btnshow.setOnClickListener {
            val intent = Intent(this, EntiesActivity::class.java)
                startActivity(intent)
            }
    }
    private fun adddata(view: View) {
        var firstName = binding.etFname.text.toString()
        var lastName = binding.etLname.text.toString()
        var country = binding.etCn.text.toString()
        if (firstName.isEmpty() || lastName.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        } else {
            val values = ContentValues().apply {
                put(VisaContract.VisaEntry.COLUMN_FIRST_NAME, firstName)
                put(VisaContract.VisaEntry.COLUMN_LAST_NAME, lastName)
                put(VisaContract.VisaEntry.COLUMN_COUNTRY, country)
            }
            val uri = contentResolver.insert(VisaContract.VisaEntry.CONTENT_URI, values)
            if (uri != null) {
                Toast.makeText(this, "Data added successfully", Toast.LENGTH_SHORT).show()
                binding.etFname.text.clear()
                binding.etLname.text.clear()
                binding.etCn.text.clear()
            } else {
                Toast.makeText(this, "Failed to add data", Toast.LENGTH_SHORT).show()
            }

        }
    }

    companion object {
        const val YOUR_REQUEST_CODE = 1 // Define a request code constant
    }
}