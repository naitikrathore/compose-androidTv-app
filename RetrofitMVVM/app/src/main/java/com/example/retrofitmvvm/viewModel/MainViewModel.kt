package com.example.retrofitmvvm.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.retrofitmvvm.model.QuoteList
import com.example.retrofitmvvm.repository.QuoteRepo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class MainViewModel(private val repo: QuoteRepo) :ViewModel(){
    init{
        GlobalScope.launch(Dispatchers.IO) {
            repo.getQuote(1)
        }
    }
    val quote:LiveData<QuoteList> get()=repo.latestquote
}