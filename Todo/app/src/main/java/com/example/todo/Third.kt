package com.example.todo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todo.databinding.ActivityMainBinding
import com.example.todo.databinding.ActivityThirdBinding

class Third : AppCompatActivity() {
    private lateinit var binding:ActivityThirdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThirdBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_third)
        binding.btnsum.setOnClickListener {
            var a = binding.firstnum.text.toString().toInt()
            var b = binding.secnum.text.toString().toInt()
            var res = a + b
            binding.result.text = res.toString()


        }
    }
}