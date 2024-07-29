package tv.anoki.ondemand.presentation.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.domain.model.VODItems
import tv.anoki.ondemand.domain.use_case.GetVodListUseCase
import javax.inject.Inject

@HiltViewModel
class VodListDataViewModel @Inject constructor(
    private val getVodListUseCase: GetVodListUseCase
) : ViewModel() {

    val vodItemsFlow =
        MutableStateFlow<VodListDataUiState<List<VODItems>>>(VodListDataUiState.Loading())

    init {
        getListingData()
    }

    fun refreshListingData() {
        getListingData()
    }

    /**
     * The function api call to get vod lists data
     */
    private fun getListingData() {
        viewModelScope.launch(Dispatchers.IO) {
            getVodListUseCase.invoke().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val list = result.data.filter { it.items.isNotEmpty() }
                        vodItemsFlow.value = VodListDataUiState.Ready(list)
                    }

                    is Result.Error -> {
                        vodItemsFlow.value = VodListDataUiState.Error(result.exception)
                    }

                    else -> {
                        vodItemsFlow.value = VodListDataUiState.Loading()
                    }
                }
            }
        }
    }
}

sealed interface VodListDataUiState<T> {
    data class Loading<T>(val loadingData: Unit = Unit) : VodListDataUiState<T>
    data class Error<T>(val exception: Throwable? = null) : VodListDataUiState<T>
    data class Ready<T>(val data: T) : VodListDataUiState<T>
}
