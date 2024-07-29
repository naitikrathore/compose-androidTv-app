package com.example.lazymultiselectcompose.repository

import androidx.compose.runtime.State
import com.example.lazymultiselectcompose.api.TweetsyAPI
import com.example.lazymultiselectcompose.models.TweetListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class TweetRepo @Inject constructor(private val tweetsyAPI: TweetsyAPI) {
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    private val _tweets = MutableStateFlow<List<TweetListItem>>(emptyList())

    //    val categories: StateFlow<List<String>> = _categories
    val categories: StateFlow<List<String>>
        get() = _categories


    val tweets: StateFlow<List<TweetListItem>>
        get() = _tweets

    suspend fun getCategories() {
        val response = tweetsyAPI.getCategories()
        if (response.isSuccessful && response.body() != null) {
            _categories.emit(response.body()!!)
        }
    }

    suspend fun getTweets(category: String) {
        val response = tweetsyAPI.getTweets("tweets[?(@.category==\"$category\")]")
        if (response.isSuccessful && response.body() != null) {
            _tweets.emit(response.body()!!)
        }
    }
}