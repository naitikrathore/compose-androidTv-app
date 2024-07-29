package tv.anoki.ondemand.presentation.details.single_work

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.constants.UIConstants
import tv.anoki.ondemand.domain.model.single_work.SingleWork
import tv.anoki.ondemand.domain.use_case.GetSingleWorkWithIdUseCase
import tv.anoki.ondemand.presentation.details.BaseViewModel
import tv.anoki.ondemand.presentation.details.UiState
import javax.inject.Inject

private const val TAG = "VODDetailsViewModel"

@HiltViewModel
class SingleWorkDetailsViewModel @Inject constructor(
    private val getSingleWorkWithIdUseCase: GetSingleWorkWithIdUseCase
) : BaseViewModel() {

    val uiState =
        MutableStateFlow<UiState<SingleWork>>(UiState.Loading())

    init {
        setLastFocusedButtonIndex(UIConstants.ACTION_BUTTON_PLAY_RESUME_INDEX)
    }

    /**
     * The function to make api call to get single work data
     *
     * @param contentId the unique id to get details
     */
    fun getSingleWorkData(contentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getSingleWorkWithIdUseCase(contentId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        uiState.value = UiState.Ready(result.data)
                    }

                    is Result.Error -> {
                        uiState.value = UiState.Error(result.exception)
                    }

                    else -> {
                        uiState.value = UiState.Loading()
                    }
                }
            }
        }
    }
}

