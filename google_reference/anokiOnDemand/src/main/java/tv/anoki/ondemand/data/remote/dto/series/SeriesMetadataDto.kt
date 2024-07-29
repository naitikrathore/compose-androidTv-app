package tv.anoki.ondemand.data.remote.dto.series

import com.squareup.moshi.JsonClass
import tv.anoki.ondemand.data.remote.dto.VodItemDto

@JsonClass(generateAdapter = true)
data class SeriesMetadataDto(
    override val contentId: String,
    override val title: String,
    override val description: String,
    override val thumbnail: String,
    override val rating: String,
    override val origRating: String,
    override val year: String,
    override val genre: String,
    override val language: String,
    override val type: String,
    override val cast: String?,
    override val runtime: String?,
    override val vodPlaybackUrl: String?,
    override val licenseServerUrl: String? = null,
    override val trailerUrl: String,
    override val numSeasons: Int?,
    override val resumeFromSec: Int = 0, //TODO: remove hard coded value once we get it from the server

    val resumeFromSeason: Int,
    val resumeFromEpisode: Int,
    val episodeDurationSec: Int?, // TODO Make it mandatory once filed is available on stage
    val episodePlaybackUrl: String,
    val durationSec: Int?// duration of the episode // TODO: this should be changed to not nullable value on the API - Leena agreed with this
) : VodItemDto(
    contentId = contentId,
    title = title,
    description = description,
    thumbnail = thumbnail,
    rating = rating,
    origRating = origRating,
    year = year,
    genre = genre,
    language = language,
    type = type,
    cast = cast,
    vodPlaybackUrl = vodPlaybackUrl,
    licenseServerUrl = licenseServerUrl,
    trailerUrl = trailerUrl,
    runtime = runtime,
    numSeasons = numSeasons,
    resumeFromSec = resumeFromSec
)
