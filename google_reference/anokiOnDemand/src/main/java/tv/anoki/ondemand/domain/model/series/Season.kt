package tv.anoki.ondemand.domain.model.series

import java.util.UUID
import javax.annotation.concurrent.Immutable

@Immutable
data class Season(
    val episodes: List<Episode>,
    val season: String,
    val _id: String = UUID.randomUUID().toString()
)

@Immutable
data class SeasonList(val items: List<Season>)