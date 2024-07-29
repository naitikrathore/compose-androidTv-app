package com.example.lazymultiselectcompose.viewModels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lazymultiselectcompose.models.TweetListItem
import com.example.lazymultiselectcompose.repository.TweetRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(private val repository: TweetRepo, private val savedStateHandle: SavedStateHandle) :ViewModel() {

    val tweets : StateFlow<List<TweetListItem>>
        get() = repository.tweets

    init {
        viewModelScope.launch {
            val category=savedStateHandle.get<String>("category")?:"motivation"
            repository.getTweets(category)
        }
    }
}