package com.example.mvvmbasic.ui.quote


import androidx.lifecycle.ViewModel
import com.example.mvvmbasic.data.Quote
import com.example.mvvmbasic.data.QuoteRepository

class QuotesViewModel (private val quoteRepository: QuoteRepository)
    :ViewModel() {

        fun getQuotes()= quoteRepository.getQuote()
        fun addQuotes(quote: Quote) = quoteRepository.addQuote(quote)
}