package com.example.appfrag

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appfrag.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val firstfrag=Firstfrag()
        val secondfrag=Secondfrag()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flfragment,firstfrag)
            addToBackStack("f1")
            commit()
        }
        binding.btnchat.setOnClickListener{
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flfragment,firstfrag)
                addToBackStack("f1")
                commit()
            }
        }
        binding.btncalls.setOnClickListener{
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flfragment,secondfrag)
                addToBackStack("f2")
                commit()
            }
        }



    }
}