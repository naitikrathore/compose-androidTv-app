package tv.anoki.ondemand.presentation.details

sealed interface UiState<T> {
    data class Loading<T>(val loadingData: Unit = Unit) : UiState<T>
    data class Error<T>(val exception: Throwable? = null) : UiState<T>
    data class Ready<T>(val data: T) : UiState<T>
}