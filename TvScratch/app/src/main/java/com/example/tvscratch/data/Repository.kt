package com.example.tvscratch.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject


class Repository @Inject constructor(private val dbHelper: DatabaseDB) {
    private val latest_data = MutableLiveData<List<Movie>>()
    private val favLiveData = MutableLiveData<List<Movie>>()
    private val recLiveData = MutableLiveData<List<Movie>>()

    fun getLiveData() = latest_data as LiveData<List<Movie>>
    fun getFavData() = favLiveData as LiveData<List<Movie>>
    fun getRecData() = recLiveData as LiveData<List<Movie>>

    init {
        loadDataRepo()
    }

    private fun loadDataRepo() {
        val data = dbHelper.getData()
        latest_data.postValue(data)
        val fav = data.filter { it.isFav == 1 }
        favLiveData.postValue(fav)
        val recent = data.filter { it.isRecent == 1 }
        recLiveData.postValue(recent)
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