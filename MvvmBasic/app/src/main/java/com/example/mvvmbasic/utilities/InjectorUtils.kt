package com.example.mvvmbasic.utilities

import com.example.mvvmbasic.data.FakeDatabase
import com.example.mvvmbasic.data.QuoteRepository
import com.example.mvvmbasic.ui.quote.QuotesViewModel
import com.example.mvvmbasic.ui.quote.QuotesViewModelFactory

object InjectorUtils {
    fun provideQuotesViewModelFactory():QuotesViewModelFactory{
        val quoteRepository=QuoteRepository.getInstance(FakeDatabase.getInstance().quoteDao)
        return QuotesViewModelFactory(quoteRepository)
    }
}