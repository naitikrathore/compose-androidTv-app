package com.example.videoplayer.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.videoplayer.data.Entry
import com.example.videoplayer.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(private val repo: Repository) : ViewModel() {
    val entries = repo.getLive()
    val getfav= repo.getFavoritesLive()
    val loginResult:LiveData<Boolean> =repo.loginResult
    val signUpResult:LiveData<Long> =repo.signupResult

    fun insertData(entry: Entry) {
        repo.insertDataRepo(entry)
    }

    fun deleteEntry(entryId: Long) {
        repo.deleteDataRepo(entryId)
    }

    fun login(username: String, password: String) {
        repo.login(username, password)
    }

    fun signup(username: String, password: String) {
        repo.signup(username, password)
    }
    fun updaterepo(entry: Entry){
        repo.updatedata(entry)
    }


}