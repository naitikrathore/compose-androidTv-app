package com.example.visaapp

import android.app.Activity
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

var entryList = mutableListOf<MyData>()

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("LifeCycle", "MainActivity: OnCreate")

        binding.btappply.setOnClickListener {
            adddata(it)
        }

        binding.btnshow.setOnClickListener {
            val intent = Intent(this, EntiesActivity::class.java).apply {
                putExtra("entryList", ArrayList(entryList))
//                startActivity(this)
            }
            startActivityForResult(intent, 1)
        }
    }

    private fun adddata(view: View) {
        var firstName = binding.etFname.text.toString()
        var lastName = binding.etLname.text.toString()
        var country = binding.etCn.text.toString()
        if (firstName.isEmpty() || lastName.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        } else {
            entryList.add(MyData(firstName, lastName, country))
            binding.etFname.text.clear()
            binding.etLname.text.clear()
            binding.etCn.text.clear()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == YOUR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val updatedList = data?.getSerializableExtra("updatedList") as? ArrayList<MyData>

            Log.e("Naitik", updatedList.toString())
            if (updatedList != null) {
                entryList.clear()
                entryList.addAll(updatedList)
                // Update your UI or perform any other action with the updated list
            }
        }
    }

    companion object {
        const val YOUR_REQUEST_CODE = 1 // Define a request code constant
    }

    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "MainActivity: onStart()")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("Lifecycle", "MainActivity: onRestart()")
    }

    override fun onResume() {
        super.onResume()
        Log.d("Lifecycle", "MainActivity: onResume()")
    }

    override fun onPause() {
        super.onPause()
        Log.d("Lifecycle", "MainActivity: onPause()")
    }

    override fun onStop() {
        super.onStop()
        Log.d("Lifecycle", "MainActivity: onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Lifecycle", "MainActivity: onDestroy()")
    }

}