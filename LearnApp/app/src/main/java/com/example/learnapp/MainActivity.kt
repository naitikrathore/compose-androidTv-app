package com.example.learnapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var todoList= mutableListOf(
            Todo("Naitik",false),
            Todo("Naitik",false),
            Todo("Naitik",false),
            Todo("Naitik",false),
            Todo("Naitik",false),
            Todo("ssf",true),
            Todo("Naitik",false),
            Todo("Aks",false),
            Todo("Naitik",false),
            Todo("ssf",false),
            Todo("Naitik",true),
            Todo("fs",false),
            Todo("Naitik",false),
            Todo("dsdf",false),
            Todo("Naitik",false),
            Todo("sdff",true),
            Todo("Naitik",true),
            Todo("ds",false),
            Todo("Naitik",true),
            Todo("ddd",false),
            Todo("Naitik",false),
        )
        val adapter=TodoAdapter(todoList)
        binding.rvTodo.adapter=adapter
        binding.rvTodo.layoutManager=LinearLayoutManager(this)
        binding.btntodo.setOnClickListener{
            var title=binding.etTodo.text.toString()
            val todo=Todo(title,false)
            todoList.add(todo)
            adapter.notifyItemInserted(todoList.size-1)
        }
        }
}