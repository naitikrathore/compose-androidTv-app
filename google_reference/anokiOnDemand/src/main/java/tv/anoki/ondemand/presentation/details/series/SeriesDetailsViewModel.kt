package tv.anoki.ondemand.presentation.details.series

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.domain.model.series.SeriesMetadata
import tv.anoki.ondemand.domain.use_case.GetSeriesMetadataWithIdUseCase
import tv.anoki.ondemand.presentation.details.BaseViewModel
import tv.anoki.ondemand.presentation.details.UiState
import javax.inject.Inject

@HiltViewModel
class SeriesDetailsViewModel @Inject constructor(
    private val getSeriesMetadataWithIdUseCase: GetSeriesMetadataWithIdUseCase
) : BaseViewModel() {

    val seriesMetadataFlow =
        MutableStateFlow<UiState<SeriesMetadata>>(UiState.Loading())

    /**
     * The function to make api call to get series metadata
     *
     * @param contentId the unique id to get details
     */
    fun getSeriesMetaData(contentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getSeriesMetadataWithIdUseCase(contentId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        seriesMetadataFlow.value = UiState.Ready(result.data)
                    }

                    is Result.Error -> {
                        seriesMetadataFlow.value = UiState.Error(result.exception)
                    }

                    Result.Loading -> {
                        seriesMetadataFlow.value = UiState.Loading()
                    }
                }
            }
        }
    }
}