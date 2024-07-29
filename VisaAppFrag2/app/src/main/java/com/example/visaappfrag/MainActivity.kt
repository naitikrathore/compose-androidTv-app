package com.example.visaappfrag

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.visaapp.MyData
import com.example.visaappfrag.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), DataPassListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var aplfragment:MainFragment
//    private lateinit var shwfragment:EntriesFragment
    var entryList= mutableListOf<MyData>()
    //instance of each of them is created

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //initialize fragments
         aplfragment=MainFragment()
//        shwfragment=EntriesFragment()


//        //intial fragment to show
//
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frg, aplfragment)
        fragmentTransaction.commit()

        binding.btnApl.setOnClickListener{
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.frg, aplfragment)
            fragmentTransaction.addToBackStack("frg1")
            fragmentTransaction.commit()

        }

        binding.btnshw.setOnClickListener{

            val fragmentTransaction = fragmentManager.beginTransaction()
//            fragmentTransaction.replace(R.id.frg, shwfragment)
            fragmentTransaction.replace(R.id.frg, EntriesFragment.newInstance(entryList))
            fragmentTransaction.addToBackStack("frg1")
            fragmentTransaction.commit()
        }



    }

    override fun onDataPassed(entries: List<MyData>) {
        Log.e("nai","$entries")

        entryList.addAll(entries)

    }




}