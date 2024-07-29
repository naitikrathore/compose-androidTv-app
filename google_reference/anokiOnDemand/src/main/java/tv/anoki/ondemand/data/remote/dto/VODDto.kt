package tv.anoki.ondemand.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class VodItemDto(
    open val contentId: String,
    open val title: String,
    open val description: String,
    open val thumbnail: String,
    open val rating: String,
    open val origRating: String,
    open val year: String,
    open val genre: String,
    open val language: String,
    open val type: String,
    open val cast: String?,
    open val vodPlaybackUrl: String?,
    open val licenseServerUrl: String? = null,
    open val trailerUrl: String?,
    open val runtime: String?,
    open val numSeasons: Int?,
    open val resumeFromSec: Int = 0, //TODO: remove hard coded value once we get it from the server
    open val progress: Float = 0F //TODO: remove hard coded value once we get it from the server
)

@JsonClass(generateAdapter = true)
data class VodItemsDto(
    val items: List<VodItemDto> = emptyList(),
    val name: String
)

typealias VodListDto = List<VodItemsDto>
