package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todo.databinding.ActivityMainBinding



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnApply.setOnClickListener {
            var firstname = binding.etfn.text.toString()
            var lastname =binding.etln.text.toString()
            var birthdate = binding.etBD.text.toString()
            var country= binding.etcn.toString()
            Toast.makeText(this, "Entries saved", Toast.LENGTH_SHORT).show()
            binding.etfn.text.clear()
            binding.etln.text.clear()
            binding.etBD.text.clear()
            binding.etcn.text.clear()
        }


        binding.btnsubmit.setOnClickListener{
            val firstname=binding.etfn.text.toString()
            val lastname=binding.etln.text.toString()
            val birthdate=binding.etBD.text.toString().toInt()
            val country=binding.etcn.text.toString()
            val bundle=Bundle().apply {
                putString("EFName",firstname)
                putString("ELName",lastname)
                putInt("EBD",birthdate)
                putString("EC",country)
            }
            Intent(this,Second::class.java).apply {
                putExtras(bundle)
                startActivity(this)
            }
        }


//        binding.btnsubmit.setOnClickListener{
//            val firstname=binding.etfn.text.toString()
//            val lastname=binding.etln.text.toString()
//            val birthdate=binding.etBD.text.toString().toInt()
//            val country=binding.etcn.text.toString()
//            Intent(this,Second::class.java).also {
//                it.putExtra("EFName",firstname )
//                it.putExtra("ELName",lastname)
//                it.putExtra("EBD",birthdate)
//                it.putExtra("EC",country)
//                startActivity(it)
//            }
//        }

        }

    override fun onStart() {
        super.onStart()
        Log.e("","on start")
    }
}