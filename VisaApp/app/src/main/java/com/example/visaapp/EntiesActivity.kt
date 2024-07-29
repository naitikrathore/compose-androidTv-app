package com.example.visaapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val entryList = intent.getSerializableExtra("entryList") as ArrayList<MyData>
        adapter = VisaAdapter(entryList)
        binding.rvVisa.adapter = adapter
        binding.rvVisa.layoutManager = LinearLayoutManager(this)
        Log.d("Lifecycle", "SecondActivity: onCreate()")

        binding.btnDel.setOnClickListener {
            adapter.deleteselected()
        }
    }

    override fun onBackPressed() {
        val resultIntent = Intent().apply {
            putExtra(
                "updatedList",
                ArrayList(adapter.allitem)
            ) // Assuming entryList is your data list
        }
        setResult(Activity.RESULT_OK, resultIntent)
        super.onBackPressed()
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
        Log.d("Lifecycle", "SecondActivity: onDestroy()")
    }

}