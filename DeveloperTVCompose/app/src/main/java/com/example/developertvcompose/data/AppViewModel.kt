package com.example.developertvcompose.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(private val repo: Repository) : ViewModel() {
    private val _latestData = MutableStateFlow<List<Movie>>(emptyList())
    val latestData: StateFlow<List<Movie>> = _latestData

    private val _latestFav = MutableStateFlow<List<Movie>>(emptyList())
    val latestFav: StateFlow<List<Movie>> = _latestFav

    private val _latestRecent = MutableStateFlow<List<Movie>>(emptyList())
    val latestRecent: StateFlow<List<Movie>> = _latestRecent

    private val _currentMovieDetails = MutableStateFlow<Movie?>(null)
    val currentMovieDetails: StateFlow<Movie?> = _currentMovieDetails

    init {
        // Load initial data
        viewModelScope.launch {
            _latestData.value = repo.getlatestData.value
            _latestFav.value = repo.getfavLiveData.value
            _latestRecent.value = repo.getrecLiveData.value
        }
    }

    fun insertData(entry: Movie) {
        viewModelScope.launch {
            repo.insertDataRepo(entry)
            _latestData.value = repo.getlatestData.value// Update the list after insertion
        }
    }

    fun deleteData(entryId: Long) {
        viewModelScope.launch {
            repo.deleteDataRepo(entryId)
            _latestData.value = repo.getlatestData.value // Update the list after deletion
        }
    }

    fun updateData(entry: Movie) {
        viewModelScope.launch {
            repo.updateData(entry)
            _latestData.value = repo.getlatestData.value// Update the list after updating
        }
    }

    fun loadMovieDetails(movieId: Long) {
        viewModelScope.launch {
            val movie = _latestData.value.find { it.id == movieId }
            _currentMovieDetails.value = movie
        }
    }

}