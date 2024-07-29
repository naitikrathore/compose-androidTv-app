package com.example.tvscratch

import androidx.lifecycle.ViewModel
import com.example.tvscratch.data.Movie
import com.example.tvscratch.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(private val repo: Repository):ViewModel() {
    val latest_data = repo.getLiveData()
    val latest_fav = repo.getFavData()
    val latest_recent = repo.getRecData()

    fun insertData(entry: Movie) {
        repo.insertDataRepo(entry)
    }

    fun deleteData(entryId: Long) {
        repo.deleteDataRepo(entryId)
    }

    fun updateData(entry: Movie) {
        repo.updateData(entry)
    }
}