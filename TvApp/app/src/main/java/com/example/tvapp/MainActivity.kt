package com.example.tvapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.tvapp.databinding.ActivityMainBinding

class MainActivity : FragmentActivity() {
   private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) { getSupportFragmentManager().beginTransaction().replace(R.id.frag_container, HomeFragment())
                .commitNow()
        }
    }


    fun changeFragment(fragment: Fragment){
          supportFragmentManager.beginTransaction().add(R.id.frag_container,fragment)
              .commitNow()
    }
}