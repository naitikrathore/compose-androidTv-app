package tv.anoki.ondemand.domain.model.series

import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType
import javax.annotation.concurrent.Immutable

@Immutable
data class Episode(
    override val contentId: String,
    override val title: String,
    override val description: String,
    override val thumbnail: String,
    override val rating: String,
    override val origRating: String,
    override val year: String,
    override val genre: String,
    override val runtime: String,
    override val language: String,
    override val type: VODType,
    override val cast: String?,
    override val vodPlaybackUrl: String,
    override val licenseServerUrl: String? = null,
    override val trailerUrl: String?,
    override val numSeasons: Int,
    override val resumeFromSec: Int,
    override val progress: Float,

    val director: String,
    val season: String,
    val episode: String,
    val durationSec: Int
) : VODItem(
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
    runtime = runtime,
    vodPlaybackUrl = vodPlaybackUrl,
    licenseServerUrl = licenseServerUrl,
    trailerUrl = trailerUrl,
    numSeasons = 1,
    resumeFromSec = resumeFromSec,
    progress = progress
) {

    /**
     * Checks if the episode has been partially watched.
     *
     * This method determines whether the episode playback has started but not completed
     * based on the progress made in watching the episode.
     *
     * @return `true` if the episode has been partially watched, `false` otherwise.
     */
    fun isEpisodePartiallyWatched() = progress > 0f && progress < 1f

}

@Immutable
data class EpisodeList(val items: List<Episode>)