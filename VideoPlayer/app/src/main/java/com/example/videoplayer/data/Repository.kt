package com.example.videoplayer.data

import android.content.Context
import android.support.v4.os.IResultReceiver._Parcel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(private val dbHelper: DatabaseDB) {
    private val latest_data = MutableLiveData<List<Entry>>()
    private val _loginResult = MutableLiveData<Boolean>()
    private val _signupResult = MutableLiveData<Long>()
    private val favLiveData=MutableLiveData<List<Entry>>()

    fun getLive() = latest_data as LiveData<List<Entry>>
    fun getFavoritesLive()= favLiveData as LiveData<List<Entry>>

    val loginResult: LiveData<Boolean> get() = _loginResult
    val signupResult: LiveData<Long> get() = _signupResult

    init {
        loadDataRepo()
    }
    private fun loadDataRepo() {
        val data = dbHelper.getData()
        latest_data.postValue(data)
        val fav =data.filter { it.isFav==1 }
        favLiveData.postValue(fav)
    }

    fun insertDataRepo(entry: Entry) {
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

    fun login(username: String, password: String) {
        val success = dbHelper.verifyUser(username, password)
        _loginResult.postValue(success)
    }

    fun signup(username: String, password: String) {
        val id = dbHelper.insertUser(username, password)
        _signupResult.postValue(id)

    }
    fun updatedata(entry: Entry){
        dbHelper.updateData(entry)
        loadDataRepo()
    }

}
