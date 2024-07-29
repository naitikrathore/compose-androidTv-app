package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private lateinit var horizontalRV: RecyclerView
    private lateinit var verticalRV: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("nait","ss")
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        horizontalRV=binding.horizontalRecyclerView
        verticalRV=binding.verticalRecyclerView

        setupHorizontalRecyclerView()
        setupVerticalRecyclerView()
    }

    private fun setupHorizontalRecyclerView() {
        val horizontalAdapter = HorizontalAdapter(createHorizontalList())
        horizontalRV.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = horizontalAdapter
        }
    }

    private fun setupVerticalRecyclerView() {
        val verticalAdapter = VerticalAdapter(createVerticalList())
        verticalRV.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = verticalAdapter
        }
    }

    private fun createHorizontalList(): List<String> {
        return listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
    }

    private fun createVerticalList(): List<String> {
        return listOf("Row 1", "Row 2", "Row 3", "Row 4", "Row 5")
    }

    }