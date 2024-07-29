package tv.anoki.ondemand.presentation.listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.anoki.ondemand.domain.model.VODItem
import javax.inject.Inject

@HiltViewModel
class VodListViewModel @Inject constructor() : ViewModel() {

    private var lastFocusedItem: Pair<String, Int>? = null

    private val _focusedRowIndex = MutableStateFlow(-1)

    private val _selectedBackgroundImage =
        MutableStateFlow<Triple<String, Boolean, VODItem?>>(Triple("", false, null))
    val selectedBackgroundImage: StateFlow<Triple<String, Boolean, VODItem?>> =
        _selectedBackgroundImage.asStateFlow()

    /**
     * The flag to remember if list was focused or not before putting app in background. So that after resuming app
     * we can request focus on list item again using focusRequesterMap and focusRowItemIndexMap
     */
    private var _isListFocused = false

    /**
     * The flag to remember if trailer is playing or not
     */
    private val _isTrailerPlaying = MutableStateFlow(false)
    val isTrailerPlaying: StateFlow<Boolean> = _isTrailerPlaying.asStateFlow()

    private val _isScreenActive = MutableStateFlow(false)

    private val _isFocusRequested = MutableStateFlow(false)
    val isFocusRequested: StateFlow<Boolean> = _isFocusRequested.asStateFlow()

    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded.asStateFlow()

    private val _isVideoReadyToPlay = MutableStateFlow(false)
    val isVideoReadyToPlay: StateFlow<Boolean> = _isVideoReadyToPlay.asStateFlow()

    private val _isContentWatched = MutableStateFlow(false)
    var isContentWatched: StateFlow<Boolean> = _isContentWatched.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(false)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    fun setScreenActive(isScreenActive: Boolean) {
        _isScreenActive.value = isScreenActive
    }

    fun setFocusRequested(isFocusRequested: Boolean) {
        _isFocusRequested.value = isFocusRequested
    }

    /**
     * The function returns true if data is loaded. Default value is false
     */
    fun isDataLoaded(): Boolean {
        return _isDataLoaded.value
    }

    fun setDataLoaded(isDataLoaded: Boolean) {
        _isDataLoaded.value = isDataLoaded
    }

    /**
     * The function updates flag isVideoReadyToPlay when content is ready to play
     */
    fun setVideoReadyToPlay(isVideoReadyToPlay: Boolean) {
        _isVideoReadyToPlay.value = isVideoReadyToPlay
    }

    fun setContentIsWatched(isContentWatched: Boolean) {
        _isContentWatched.value = isContentWatched
        //Log.d("Level-B", "setContentIsWatched: $isContentWatched")
        if(isContentWatched) {
            lastFocusedItem = null
        }
    }

    /**
     * The function to remember the last focused item by id and index
     */
    fun setLastFocusedItem(name: String, index: Int) {
        lastFocusedItem = Pair(name, index)
    }

    /**
     * The function to remember the selected item thumbnail and render as a background
     */
    fun setSelectedBackgroundImage(videoOnDemands: VODItem, url: String, isFullscreen: Boolean) {
        _selectedBackgroundImage.value =
            Triple(first = url, second = isFullscreen, third = videoOnDemands)
    }

    /**
     * The function to remember if list has focus or not
     */
    fun setListFocused(isFocused: Boolean) {
        _isListFocused = isFocused
    }

    /**
     * The function to remember the trailer is playing or not
     */
    fun setTrailerPlaying(isPlaying: Boolean) {
        _isTrailerPlaying.value = isPlaying
    }

    /**
     * The function to remember current focused row index
     */
    fun setFocusedRowIndex(rIndex: Int) {
        _focusedRowIndex.value = rIndex
    }

    /**
     * The function to create StateFlow for row with row index rIndex which will emit state whenever
     * row with row index rIndex is focused or unfocused.
     */
    fun isRowFocused(rIndex: Int): StateFlow<Boolean> {
        val isRowFocused = MutableStateFlow(false)
        viewModelScope.launch(Dispatchers.Default) {
            _focusedRowIndex.collect {
                isRowFocused.value = it == rIndex
            }
        }
        return isRowFocused.asStateFlow()
    }

    /**
     * The function to create StateFlow for Card with itemID and itemIndex which will emit state to
     * restore focus after coming back from different screen.
     */
    fun shouldFocusCard(itemID: String, rowIndex: Int, itemIndex: Int): StateFlow<Boolean> {
        val shouldFocusCard = MutableStateFlow(false)

        viewModelScope.launch(Dispatchers.Default) {
            _isScreenActive.collect {
                shouldFocusCard.value = (it && _isListFocused && lastFocusedItem?.first == itemID && lastFocusedItem?.second == itemIndex)
                //Log.d("Level-C", "_isScreenActive: ${_isScreenActive.value} $rowIndex:$itemIndex ${shouldFocusCard.value}")
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            _isDataLoaded.collect {
                shouldFocusCard.value = (it && _isScreenActive.value && _isContentWatched.value && rowIndex == 0 && itemIndex == 0)
                //Log.d("Level-C", "_isDataLoaded: ${_isDataLoaded.value} $rowIndex:$itemIndex ${shouldFocusCard.value}")
            }
        }

        return shouldFocusCard.asStateFlow()
    }

    fun shouldReloadListData(): StateFlow<Boolean> {
        val shouldReload = MutableStateFlow(false)
        viewModelScope.launch(Dispatchers.Default) {
            _isScreenActive.collect {
                shouldReload.value = it && _isContentWatched.value
            }
        }
        if(shouldReload.value) {
            setDataLoaded(false)
        }

        return shouldReload.asStateFlow()
    }

    /**
     * The function updates flag isFocusRequested when focus request on item
     */
    fun requestFocus() {
        _isFocusRequested.value = true
    }

    /**
     * The function updates flag isFocusRequested when focus release on item
     */
    fun releaseFocus() {
        _isFocusRequested.value = false
    }

    /**
     * The function sets flag isInternetAvailable based on internet connection
     */
    fun setInternet(value: Boolean) {
        _isInternetAvailable.value = value
    }
}
