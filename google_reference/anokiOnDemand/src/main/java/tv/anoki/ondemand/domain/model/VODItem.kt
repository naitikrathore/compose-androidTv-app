package tv.anoki.ondemand.domain.model

import java.util.UUID
import javax.annotation.concurrent.Immutable

enum class VODType {
    SINGLE_WORK,
    SERIES
}

@Immutable
open class VODItem(
    open val contentId: String,
    open val title: String,
    open val description: String,
    open val thumbnail: String,
    open val rating: String,
    open val origRating: String,
    open val year: String,
    open val genre: String,
    open val runtime: String,
    open val language: String,
    open val type: VODType,
    open val cast: String?,
    open val vodPlaybackUrl: String?,
    open val licenseServerUrl: String? = null,
    open val trailerUrl: String?,
    open val numSeasons: Int,
    open val duration: Int = 0,
    open val resumeFromSec: Int,
    open val progress: Float = 0F, //TODO remove hard coded value once we get it from the server

    open val _id: String = UUID.randomUUID().toString()
)

@Immutable
data class VODItems(
    val items: List<VODItem>,
    val name: String,
    val _id: String = UUID.randomUUID().toString()
)

@Immutable
data class VideoOnDemandsItemsList(val items: List<VODItems>)
