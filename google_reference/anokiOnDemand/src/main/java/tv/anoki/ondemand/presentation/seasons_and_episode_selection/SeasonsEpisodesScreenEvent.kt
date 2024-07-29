package tv.anoki.ondemand.presentation.seasons_and_episode_selection

sealed interface SeasonsEpisodesScreenEvent {
    data class FetchSeries(val id: String) : SeasonsEpisodesScreenEvent
    data class OnResume(val id: String): SeasonsEpisodesScreenEvent
}