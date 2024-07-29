package com.example.mvvmapp.ui.shopping

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mvvmapp.data.db.ShoppingDatabase
import com.example.mvvmapp.data.repositories.ShoppingRepo
import com.example.mvvmapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val database= ShoppingDatabase(this)
        val repos = ShoppingRepo(database)
        val factory =ShoppingViewModelFactory(repos)
        val viewModel=ViewModelProvider(this,factory).get(ShoppingViewModel::class.java)
//        val factory = ShoppingViewModelFactory(repo)
//        viewModel = ViewModelProvider(this, factory).get(ShoppingViewModel::class.java)
    }

}