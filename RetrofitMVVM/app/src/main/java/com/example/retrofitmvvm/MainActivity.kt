package com.example.retrofitmvvm

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.retrofit.QuotesAPI
import com.example.retrofitmvvm.api.RetrofitHelper
import com.example.retrofitmvvm.databinding.ActivityMainBinding
import com.example.retrofitmvvm.viewModel.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val quoteSer=RetrofitHelper.getInstance().create(QuotesAPI::class.java)

        setContentView(binding.root)


    }
}