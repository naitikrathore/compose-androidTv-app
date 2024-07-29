package tv.anoki.ondemand.presentation.seasons_and_episode_selection

sealed interface Event {
    data object SeasonSelected: Event
}