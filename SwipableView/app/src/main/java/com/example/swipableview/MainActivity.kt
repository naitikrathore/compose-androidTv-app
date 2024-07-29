package com.example.swipableview

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.swipableview.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val images= listOf(
            R.drawable.img,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4
        )
        val adapter=ViewPageAdapter(images)
//        remeber whenever need some view or id from any XML file you need to use binding you have make multiple time this mistake
        binding.viewpager.adapter=adapter
//        binding.viewpager.orientation=ViewPager2.ORIENTATION_VERTICAL


        //now we will connect tablayout to view pager
//
        TabLayoutMediator(binding.tablt,binding.viewpager){ tab,position ->
            tab.text="Tab ${position+1}"
        }.attach()

        binding.tablt.addOnTabSelectedListener(object:OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                Toast.makeText(this@MainActivity, "Selceted ${tab?.text}", Toast.LENGTH_SHORT).show()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

                Toast.makeText(this@MainActivity, "Unselected ${tab?.text}", Toast.LENGTH_SHORT).show()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Toast.makeText(this@MainActivity, "Re Selected ${tab?.text}", Toast.LENGTH_SHORT).show()
            }
        })


    }
}