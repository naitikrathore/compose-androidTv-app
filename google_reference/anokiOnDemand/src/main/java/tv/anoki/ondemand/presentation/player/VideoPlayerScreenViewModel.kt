package tv.anoki.ondemand.presentation.player

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import tv.anoki.ondemand.domain.model.Videos
import javax.annotation.concurrent.Immutable
import javax.inject.Inject

/**
 * ViewModel for the Video Player Screen.
 *
 * @constructor Creates an instance of VideoPlayerScreenViewModel.
 */
@HiltViewModel
class VideoPlayerScreenViewModel @Inject constructor() : ViewModel() {

    private val _videoState = MutableStateFlow<VODPlayerUiState<Videos>>(VODPlayerUiState.Loading(""))
    val videoState = _videoState.asStateFlow()

    private val _playerUiState = MutableStateFlow(PlayerUiState())
    val playerUiState = _playerUiState.asStateFlow()

    /**
     * Sets the duration of the content.
     *
     * @param contentDuration The duration of the content in milliseconds.
     */
    fun setContentDuration(contentDuration: Long) {
        _playerUiState.update {
            it.copy(contentDuration = contentDuration)
        }
    }

    /**
     * Sets the current position of the content.
     *
     * @param contentCurrentPosition The current position of the content in milliseconds.
     */
    fun setContentCurrentPosition(contentCurrentPosition: Long) {
        _playerUiState.update {
            it.copy(contentCurrentPosition = contentCurrentPosition)
        }
    }

    /**
     * Sets the data for the video.
     *
     * @param videoTitle The title of the video.
     * @param videoDescription The description of the video.
     * @param origRating The original rating of the video.
     */
    fun setData(videoTitle: String, videoDescription: String, origRating: String) {
        _playerUiState.update {
            it.copy(title = videoTitle, description = videoDescription, origRating = origRating)
        }
    }

    /**
     * Sets the state of the player.
     *
     * @param isPlaying Whether the player is playing.
     * @param isError Whether there is an error in the player.
     */
    fun setState(isPlaying: Boolean, isError: Boolean) {
        _videoState.value = if (isError) {
            VODPlayerUiState.Error(_playerUiState.value.title)
        } else if (isPlaying) {
            VODPlayerUiState.Ready(Videos())
        } else {
            VODPlayerUiState.Loading(_playerUiState.value.title)
        }
        _playerUiState.update {
            it.copy(
                isPlaying = isPlaying,
                playerError = if (isError) " " else "",
                isLoading = !isPlaying && !isError
            )
        }
    }

    /**
     * Sets the playing state of the player.
     *
     * @param isPlaying Whether the player is playing.
     */
    fun setPlaying(isPlaying: Boolean) {
        _playerUiState.update {
            it.copy(isPlaying = isPlaying)
        }
    }
}

/**
 * Sealed interface representing the UI state of the VOD player.
 */
sealed interface VODPlayerUiState<T> {
    /**
     * Represents the loading state of the VOD player.
     *
     * @param loadingData The data being loaded.
     */
    data class Loading<T>(val loadingData: String) : VODPlayerUiState<T>

    /**
     * Represents the error state of the VOD player.
     *
     * @param loadingData The data being loaded.
     * @param exception The exception that occurred.
     */
    data class Error<T>(val loadingData: String, val exception: Throwable? = null) : VODPlayerUiState<T>

    /**
     * Represents the ready state of the VOD player.
     *
     * @param data The data of the VOD player.
     */
    data class Ready<T>(val data: T) : VODPlayerUiState<T>
}

/**
 * Data class representing the UI state of the player.
 *
 * @property contentCurrentPosition The current position of the content in milliseconds.
 * @property contentDuration The duration of the content in milliseconds.
 * @property isLoading Whether the player is loading.
 * @property playerError The error message of the player.
 * @property title The title of the video.
 * @property description The description of the video.
 * @property origRating The original rating of the video.
 * @property isPlaying Whether the player is playing.
 * @property isSeekSelected Whether the seek option is selected.
 */
@Immutable
data class PlayerUiState(
    val contentCurrentPosition: Long = 0,
    val contentDuration: Long = 0,
    val isLoading: Boolean = false,
    val playerError: String = "",
    val title: String = "",
    val description: String = "",
    val origRating: String = "",
    val isPlaying: Boolean = false,
    val isSeekSelected: Boolean = false
)