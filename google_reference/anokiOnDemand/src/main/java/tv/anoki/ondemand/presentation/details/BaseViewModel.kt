package tv.anoki.ondemand.presentation.details

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tv.anoki.ondemand.constants.UIConstants
import javax.annotation.concurrent.Immutable
import javax.inject.Inject

@HiltViewModel
open class BaseViewModel @Inject constructor() : ViewModel() {

    private val _baseUiState = MutableStateFlow(BaseViewUiState())
    val baseUiState: StateFlow<BaseViewUiState> = _baseUiState.asStateFlow()

    /**
     * The function to handle focus on last focused action button
     */
    fun setLastFocusedButtonIndex(index: Int) {
        _baseUiState.update {
            it.copy(
                lastFocusedButtonIndex = index
            )
        }
    }

    /**
     * The function to update resume state of the screen
     */
    fun setResumeCalled(isResume: Boolean) {
        _baseUiState.update {
            it.copy(
                isResumeCalled = isResume
            )
        }
    }
}

@Immutable
data class BaseViewUiState(
    val lastFocusedButtonIndex: Int = UIConstants.ACTION_BUTTON_PLAY_RESUME_INDEX,
    val isResumeCalled: Boolean = false,
)
