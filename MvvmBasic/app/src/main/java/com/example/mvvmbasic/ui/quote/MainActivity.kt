package com.example.mvvmbasic.ui.quote

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.mvvmbasic.data.Quote
import com.example.mvvmbasic.databinding.ActivityMainBinding
import com.example.mvvmbasic.utilities.InjectorUtils

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUi()
    }
    private fun initializeUi(){
        val factory =InjectorUtils.provideQuotesViewModelFactory()
        val viewModel = ViewModelProvider(this, factory).get(QuotesViewModel::class.java)

        viewModel.getQuotes().observe(this, Observer { quotes ->
            val quotesText = quotes.joinToString("\n\n") { "${it.quoteText}\n- ${it.author}" }
            binding.textViewQuotes.text = quotesText
        })

       binding.buttonAddQuote.setOnClickListener{
           val quote =Quote(binding.editTextQuote.text.toString(),binding.editTextAuthor.text.toString())
           viewModel.addQuotes(quote)
           binding.editTextQuote.text.clear()
           binding.editTextAuthor.text.clear()
       }

    }
}