package com.tanasi.streamflix.activities.main

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanasi.streamflix.utils.GitHub
import com.tanasi.streamflix.utils.InAppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow<State>(State.CheckingUpdate)
    val state: Flow<State> = _state

    sealed class State {
        data object CheckingUpdate : State()
        data class SuccessCheckingUpdate(val release: GitHub.Release?) : State()

        data object DownloadingUpdate : State()
        data class SuccessDownloadingUpdate(val apk: File) : State()

        data object InstallingUpdate : State()

        data class FailedUpdate(val error: Exception) : State()
    }


    fun checkUpdate() = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.CheckingUpdate)

        try {
            val release = InAppUpdater.getReleaseUpdate()

            _state.emit(State.SuccessCheckingUpdate(release))
        } catch (e: Exception) {
            Log.e("MainViewModel", "checkUpdate: ", e)
            _state.emit(State.FailedUpdate(e))
        }
    }

    fun downloadUpdate(
        context: Context,
        asset: GitHub.Release.Asset,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.DownloadingUpdate)

        try {
            val apk = InAppUpdater.downloadApk(context, asset)

            _state.emit(State.SuccessDownloadingUpdate(apk))
        } catch (e: Exception) {
            Log.e("MainViewModel", "downloadUpdate: ", e)
            _state.emit(State.FailedUpdate(e))
        }
    }

    fun installUpdate(
        context: Context,
        apk: File,
    ) = viewModelScope.launch(Dispatchers.IO) {
        _state.emit(State.InstallingUpdate)

        try {
            InAppUpdater.installApk(context, Uri.fromFile(apk))
        } catch (e: Exception) {
            Log.e("MainViewModel", "installUpdate: ", e)
            _state.emit(State.FailedUpdate(e))
        }
    }
}