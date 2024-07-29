package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.ActivityEntryBinding
import com.example.myapplication.databinding.ActivityMainBinding

class entry : AppCompatActivity() {
    private lateinit var binding: ActivityEntryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btback.setOnClickListener{
          finish()
        }
        var ddata= mutableListOf(
            Showdata("Naitik","Sec","na"),
            Showdata("Naitik","Sec","na"),
            Showdata("Naitik","Sec","na"),
            Showdata("Naitik","Sec","na"),
            Showdata("Naitik","Sec","na"),
            Showdata("Naitik","Sec","na"),
            Showdata("Naitik","Sec","na"),
                    Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
                Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na"),
        Showdata("Naitik","Sec","na")

        )
        val adapter=EntryaAdapter(ddata)
        binding.recyclerView.adapter=adapter
        binding.recyclerView.layoutManager=LinearLayoutManager(this)


    }
}