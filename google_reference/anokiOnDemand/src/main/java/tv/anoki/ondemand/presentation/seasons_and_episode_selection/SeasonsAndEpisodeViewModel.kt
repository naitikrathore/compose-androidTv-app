package tv.anoki.ondemand.presentation.seasons_and_episode_selection

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tv.anoki.framework.data.Result
import tv.anoki.ondemand.domain.model.series.Episode
import tv.anoki.ondemand.domain.model.series.Season
import tv.anoki.ondemand.domain.use_case.GetSeriesWithIdUseCase
import tv.anoki.ondemand.presentation.details.BaseViewModel
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.FocusState.EPISODES
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.FocusState.NONE
import tv.anoki.ondemand.presentation.seasons_and_episode_selection.FocusState.SEASONS
import javax.annotation.concurrent.Immutable
import javax.inject.Inject

private const val TAG = "SeasonsAndEpisodeViewModel"

@HiltViewModel
class SeasonsAndEpisodeViewModel @Inject constructor(
    private val getSeriesWithIdUseCase: GetSeriesWithIdUseCase
) : BaseViewModel() {

    /**
     * Channel used for sending and receiving events.
     * Events can be emitted into this channel to notify observers about state changes or other occurrences.
     */
    private val eventChannel = Channel<Event>()

    /**
     * Flow representing the stream of events.
     * Observers can collect from this flow to receive notifications about state changes or other occurrences.
     */
    val events = eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(SeasonsAndEpisodesUiState())
    val uiState = _uiState.asStateFlow()

    private val _seasonsListUiState = MutableStateFlow(ListUiState())
    val seasonsListUiState = _seasonsListUiState.asStateFlow()

    private val _episodesListUiState = MutableStateFlow(ListUiState())
    val episodesListUiState = _episodesListUiState.asStateFlow()

    private var lastFocusedState = NONE

    // TODO BORIS: seasonAndEpisode scrolling issue - refactor this in the future
    private var initialSelectedSeason: Int? = null
    private var initialSelectedEpisode: Int? = null
    private var areIndexInitialised: Boolean = false

    /**
     * Gets the index of the first episode in the selected season.
     *
     * This method searches for the first episode in the episode list that belongs to the season
     * currently selected by the user. It updates the episode list UI state with the
     * position of the first episode in the selected season and returns the index of this episode.
     *
     * @return the index of the first episode in the selected season. If no episode is found,
     *         the returned index will be 0.
     */
    fun getFirstEpisodeIndexOfSelectedSeason(): Int {

        var episodeIndex = 0

        val selectedSeasonIndex = _seasonsListUiState.value.selectedPosition
        val episodeList = _uiState.value.episodesList

        episodeList.indexOfFirst {
            it.season.toInt() == selectedSeasonIndex + 1
        }.let {
            if (it > 0) {
                episodeIndex = it
            }
        }

        _episodesListUiState.update {
            it.copy(
                selectedPosition = episodeIndex
            )
        }

        return episodeIndex
    }

    /**
     * Retrieves the index of the first episode within the list of episodes that
     * belongs to the selected season
     */
    fun calculateLastWatchedEpisodeIndex() {
        val selectedSeasonIndex = _seasonsListUiState.value.selectedPosition
        val selectedEpisodeIndex = _episodesListUiState.value.selectedPosition

        /**
         *  Doing minus because from server we are getting season & episode value starting from 1 and not 0
         */
        val episode = _uiState.value.episodesList.find { ((it.season.toInt().minus(1)) == selectedSeasonIndex && (it.episode.toInt().minus(1)) == selectedEpisodeIndex) }

        var index1 = 0
        if (episode != null) {
            val index = _uiState.value.episodesList.indexOf(episode)
            index1 = index
        }

        _episodesListUiState.update {
            it.copy(
                selectedPosition = index1
            )
        }
    }

    fun onEvent(event: SeasonsEpisodesScreenEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is SeasonsEpisodesScreenEvent.FetchSeries -> {
                    fetchSeries(event.id)
                }

                is SeasonsEpisodesScreenEvent.OnResume -> {
                    fetchSeries(event.id)
                    setFocusState(focusState = lastFocusedState, isDelayRequired = true)
                }

            }
        }
    }

    // TODO BORIS: seasonAndEpisode scrolling issue - refactor this in the future
    fun setupInitialSeasonAndEpisodeIndex(seasonIndex: Int, episodeIndex: Int) {
        if (areIndexInitialised) return

        initialSelectedSeason = seasonIndex - 1
        initialSelectedEpisode = episodeIndex - 1

        areIndexInitialised = true
    }

    fun onEpisodePressed() {
        _uiState.update { 
            it.copy(
                isLoading = true
            )
        }
    }

    /**
     * Requests the focus to be set on the list of episodes.
     */
    fun requestFocusOnEpisodeList() {
        viewModelScope.launch {
            setFocusState(EPISODES)
        }
    }

    /**
     * Requests the focus to be set on the list of seasons.
     */
    fun requestFocusOnSeasonsList() {
        viewModelScope.launch {
            setFocusState(SEASONS)
        }
    }

    // TODO BORIS: seasonAndEpisode scrolling issue - refactor this in the future
    private fun updateLastWatchedSeasonAndEpisode() {
        lastFocusedState = EPISODES

        val episodeList = _uiState.value.episodesList

        if (initialSelectedEpisode == null || initialSelectedSeason == null) return

        if (initialSelectedSeason!! > 0) {

            initialSelectedEpisode = episodeList.indexOfFirst {
                it.season.toInt().minus(1) == initialSelectedSeason
                        && it.episode.toInt().minus(1) == initialSelectedEpisode
            }

        }

        _seasonsListUiState.update {
            it.copy(
                selectedPosition = initialSelectedSeason!!
            )
        }
        _episodesListUiState.update {
            it.copy(
                selectedPosition = initialSelectedEpisode!!
            )
        }

        initialSelectedEpisode = null
        initialSelectedSeason = null

    }


    /**
     * Fetches series data asynchronously with the specified ID.
     * This method updates the UI state to indicate loading state while fetching data.
     * @param id The ID of the series to fetch.
     */
    private suspend fun fetchSeries(id: String) {
        _uiState.update {
            it.copy(
                isLoading = true
            )
        }
        getSeriesWithIdUseCase(id).collect { result ->
            when (result) {
                is Result.Success -> {
                    val list = result.data.filter { it.episodes.isNotEmpty() }

                    val episodeList = arrayListOf<Episode>()

                    list.forEach {
                        episodeList.addAll(it.episodes)
                    }

                    _uiState.update {
                        it.copy(
                            seasonList = list,
                            errorMessage = "",
                            episodesList = episodeList
                        )
                    }

                    updateLastWatchedSeasonAndEpisode()

                    requestFocusOnEpisodeList()

                    _uiState.update {
                        it.copy(
                            isLoading = false
                        )
                    }

                }

                is Result.Error -> {
                    result.exception?.localizedMessage?.let { errorMessage ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = errorMessage
                            )
                        }
                    }
                }

                Result.Loading -> {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            errorMessage = ""
                        )
                    }
                }
            }
        }
    }

    /**
     * Updates the UI state when an episode is selected.
     * @param selectedEpisodeIndex The index of the selected episode within the episodes list.
     */
    fun onEpisodeSelected(selectedEpisodeIndex: Int) {
        _episodesListUiState.update {
            it.copy(
                selectedPosition = selectedEpisodeIndex
            )
        }

        val episode = _uiState.value.episodesList[selectedEpisodeIndex]

        // Update the selected season index in the seasons list UI state
        _seasonsListUiState.update {
            it.copy(
                selectedPosition = episode.season.toInt() - 1
            )
        }
        _uiState.update {
            it.copy(title = episode.season)
        }
    }

    /**
     * Handles the event when navigating back from the Season List.
     *
     * This function updates the `lastFocusedState` to indicate that the
     * focus should be on the Episodes screen. It ensures that when the user
     * returns from the previous screen, the application knows to focus on the
     * Episodes section.
     */
    fun onBackFromSeason() {
        lastFocusedState = EPISODES
    }

    /**
     * Updates the UI states when a season is selected.
     * @param selectedEpisodeIndex The index of the selected episode within the episodes list.
     * @param selectedSeasonIndex The index of the selected season within the seasons list.
     */
    fun onSeasonSelected(selectedSeasonIndex: Int, selectedEpisodeIndex: Int = 0) {

        // Retrieve the selected season from the overall UI state
        val item = _uiState.value.seasonList[selectedSeasonIndex]

        // Update the selected episode index in the episodes list UI state
        _episodesListUiState.update {
            it.copy(
                selectedPosition = selectedEpisodeIndex
            )
        }

        // Update the selected season index in the seasons list UI state
        _seasonsListUiState.update {
            it.copy(
                selectedPosition = selectedSeasonIndex
            )
        }

        // Retrieve the selected season from the overall UI state
        _uiState.update {
            it.copy(
                selectedSeason = item,
            )
        }
        _uiState.update {
            it.copy(title = item.season)
        }

        onSeasonChanged()

    }

    /**
     * Resets the focus state for both the episodes and seasons lists UI.
     *
     * @param lastFocusedState The last focused state to set, defaults to [FocusState.NONE].
     *                         This parameter specifies the state to be set after resetting focus.
     *                         By default, it sets the focus state to none.
     *
     * This method updates the UI states of both episodes and seasons lists to indicate that
     * focus is not requested, and optionally sets the last focused state to a specified value.
     */
    private fun resetFocusState(lastFocusedState: FocusState = NONE) {
        this.lastFocusedState = lastFocusedState

        // List of UI states to update for resetting the focus state
        val statesToUpdate = listOf(_episodesListUiState, _seasonsListUiState)

        // Update each UI state to indicate that focus is not requested
        statesToUpdate.forEach { listUiState ->
            listUiState.update {
                it.copy(isFocusRequested = false)
            }
        }
    }

    /**
     * Sets the focus state for either the episodes list or the seasons list UI, or none.
     *
     * @param focusState The focus state to set, defaults to [FocusState.NONE].
     * @param isDelayRequired Specifies whether a delay is required before focusing, defaults to false.
     *                       This delay can be useful for ensuring proper focusing, especially in "onResume method".
     */
    private suspend fun setFocusState(
        focusState: FocusState = NONE,
        isDelayRequired: Boolean = false
    ) {

        // Reset the focus state for both lists
        resetFocusState()

        // If a delay is required before focusing, add a delay of 300 milliseconds.
        // This delay is crucial for handling the onResume state. Without it, when the user
        // moves the app to the background by pressing the Home button on the Remote Control Unit (RCU)
        // and navigates back to the app, the focus may become misaligned.
        // TODO This should be optimised in the future.
        if (isDelayRequired) {
            delay(300)
        }

        // Update the UI state based on the specified focus state
        when (focusState) {

            SEASONS -> {

                lastFocusedState = SEASONS

                _seasonsListUiState.update {
                    it.copy(
                        isFocusRequested = true
                    )
                }

                _episodesListUiState.update {
                    it.copy(
                        isFocusRequested = false
                    )
                }

            }

            EPISODES -> {

                lastFocusedState = EPISODES

                _episodesListUiState.update {
                    it.copy(
                        isFocusRequested = true
                    )
                }

                _seasonsListUiState.update {
                    it.copy(
                        isFocusRequested = false
                    )
                }

            }

            NONE -> return // No focus state specified, return without updating
        }
    }

    /**
     * Notifies the UI layer when the selected season changes.
     *
     * In an `EventObserver`, the UI can respond to a `SeasonSelected` event
     * by animating the scroll to the first episode of the newly selected season.
     */
    private fun onSeasonChanged() {
        viewModelScope.launch {
            eventChannel.send(Event.SeasonSelected)
        }
    }
}

/**
 * Enumeration representing the focus state of the UI.
 * - [SEASONS]: Indicates that the focus is on the seasons list.
 * - [EPISODES]: Indicates that the focus is on the episodes list.
 * - [NONE]: Indicates that the focus is neither on the seasons nor episodes, typically
 *   when the top bar or another component unrelated to seasons or episodes is focused.
 */
enum class FocusState {
    SEASONS,
    EPISODES,
    NONE
}

@Immutable
data class SeasonsAndEpisodesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String = "",
    val seasonList: List<Season> = emptyList(),
    val episodesList: List<Episode> = emptyList(),
    val selectedSeason: Season? = null,
    val title: String = ""
)

@Immutable
data class ListUiState(
    val selectedPosition: Int = -1,
    val isFocusRequested: Boolean = false,
)