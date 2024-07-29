package com.example.retrofitmvvm.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.retrofit.QuotesAPI
import com.example.retrofitmvvm.model.QuoteList

class QuoteRepo(private val quoteService: QuotesAPI) {
    private var getlatestQuote = MutableLiveData<QuoteList>()

    val latestquote: LiveData<QuoteList> get() = getlatestQuote

    suspend fun getQuote(page: Int) {
        val result = quoteService.getQuotes(page)
        if (result?.body() != null) {
            getlatestQuote.postValue(result.body())
        }
    }

}