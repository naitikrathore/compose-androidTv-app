package tv.anoki.ondemand.data.remote.dto.series

data class SeasonDto(
    val episodes: List<EpisodeDto>?,
    val season: String
)