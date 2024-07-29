package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todo.databinding.ActivityMainBinding
import com.example.todo.databinding.ActivitySecondBinding

class Second : AppCompatActivity() {
    private lateinit var binding:ActivitySecondBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val fname=intent.getStringExtra("EFName")
        val lname=intent.getStringExtra("ELName")
        val birthdate=intent.getIntExtra("EBD",0)
        val contr=intent.getStringExtra("EC")
        val ans="Name is ${fname} ${lname} born on ${birthdate} from ${contr}"
        binding.out.text=ans
        binding.btnback.setOnClickListener{

//            finish() //it destroy the activity
            Intent(this,MainActivity::class.java).also {
                startActivity(it)
            }
        }
        binding.yt.setOnClickListener{
            Intent(Intent.ACTION_MAIN).also {
                it.`package`="com.google.android.youtube"
                startActivity(it)
            }
        }



//        setContentView(R.layout.activity_second)
//
//        riva te lateinit var binding: ActivityMainBinding
//        override fun onCreate(savedInstanceState: Bundle?) {
//            super.onCreate(savedInstanceState)
//            binding= ActivityMainBinding.inflate(layoutInflater)
//            setContentView(binding.root)

        }
}