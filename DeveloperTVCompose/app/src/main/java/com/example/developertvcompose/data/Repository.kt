package com.example.developertvcompose.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class Repository @Inject constructor(private val dbHelper: DatabaseDB) {
    private val _latestData = MutableStateFlow<List<Movie>>(emptyList())
    val getlatestData: StateFlow<List<Movie>> = _latestData

    private val _favLiveData = MutableStateFlow<List<Movie>>(emptyList())
    val getfavLiveData: StateFlow<List<Movie>> = _favLiveData

    private val _recLiveData = MutableStateFlow<List<Movie>>(emptyList())
    val getrecLiveData: StateFlow<List<Movie>> = _recLiveData

    init {
        loadDataRepo()
    }

    private fun loadDataRepo() {
        val data = dbHelper.getData()
        _latestData.value = data
        _favLiveData.value = data.filter { it.isFav == 1 }
        _recLiveData.value = data.filter { it.isRecent == 1 }
    }

    fun insertDataRepo(entry: Movie) {
        val id = dbHelper.insertData(entry)
        if (id != -1L) {
            loadDataRepo()
        }
    }

    fun deleteDataRepo(entryId: Long) {
        val result = dbHelper.deleteData(entryId)
        if (result > 0) {
            loadDataRepo()
        }
    }

    fun updateData(entry: Movie) {
        dbHelper.updateData(entry)
        loadDataRepo()
    }
}
