package com.example.appbuttomnavi

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.appbuttomnavi.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val firfrag=Frag1()
        val secfrag=Frag2()
        val thifrag=Frag3()

        setCurrentFragment(firfrag)

        bottomNavigationView.setOnNavigationItemSelected{
            when(it.itemid)
        }











    }
    private fun setCurrentFragment(fragment: Fragment)=
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flfragment,fragment)
            addToBackStack("f1")
            commit()
        }
}
