package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btent.setOnClickListener{
            var firstN=binding.etfn.text.toString()
            var secondN=binding.etln.text.toString()
            var country=binding.etcn.text.toString()
            if(firstN.isEmpty() || secondN.isEmpty() || country.isEmpty()){
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }else{
                val bundle=Bundle().apply {
                    putString("EFName",firstN)
                    putString("ELName",secondN)
                    putString("EC",country)
                }
                Intent(this,entry::class.java).apply {
                    putExtras(bundle)
                    startActivity(this)
                }
                Toast.makeText(this, "Entry Created", Toast.LENGTH_SHORT).show()
            }
        }

    }
}