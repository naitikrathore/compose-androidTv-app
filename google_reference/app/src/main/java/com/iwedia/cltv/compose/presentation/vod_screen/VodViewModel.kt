package com.iwedia.cltv.compose.presentation.vod_screen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.foryou.RailItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "FastVodDataProvider"

class VodViewModel : ViewModel() {

    private val _state = MutableStateFlow(VodState())
    val state = _state.asStateFlow()

    fun isVodEnabled(): Boolean = false // Set this value to the true in order to have VOD tab
    fun releaseFocus() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isFocusRequested = false
                )
            }
        }
    }

    fun requestFocus() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isFocusRequested = true
                )
            }
        }
    }

    fun onClick() {
        fetchAllEvents()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun fetchAllEvents() {
        viewModelScope.launch {
            FastLiveTabDataProvider.forYouModule.getForYouRails(
                object : IAsyncDataCallback<ArrayList<RailItem>> {
                    override fun onFailed(error: Error) {

                    }

                    override fun onReceive(data: ArrayList<RailItem>) {
                        ReferenceApplication.runOnUiThread {
                            _state.update {
                                it.copy(
                                    items = data
                                )
                            }
                        }
                    }
                })
        }
    }

    fun rememberFocusedItemIndex(index: Int) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    focusedItemIndex = index
                )
            }
        }
    }
}