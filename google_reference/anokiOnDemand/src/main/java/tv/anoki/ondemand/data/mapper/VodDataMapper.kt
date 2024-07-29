package tv.anoki.ondemand.data.mapper

import tv.anoki.framework.ui.decryptLicenceUrl
import tv.anoki.ondemand.data.remote.dto.VodItemDto
import tv.anoki.ondemand.data.remote.dto.VodItemsDto
import tv.anoki.ondemand.data.remote.dto.series.EpisodeDto
import tv.anoki.ondemand.data.remote.dto.series.SeasonDto
import tv.anoki.ondemand.data.remote.dto.series.SeriesMetadataDto
import tv.anoki.ondemand.data.remote.dto.single_work.SingleWorkDto
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODItems
import tv.anoki.ondemand.domain.model.VODType
import tv.anoki.ondemand.domain.model.series.Episode
import tv.anoki.ondemand.domain.model.series.Season
import tv.anoki.ondemand.domain.model.series.SeriesMetadata
import tv.anoki.ondemand.domain.model.single_work.SingleWork

/**
 * The function to convert string type to VODType enum
 *
 * @param type the string value
 */
private fun convertToVODType(type: String): VODType {
    return if (type == "series") VODType.SERIES else VODType.SINGLE_WORK
}

/**
 * The function to convert remote data(VodItemDto) to UI data(VODItem)
 */
fun VodItemDto.toVodItem(): VODItem {
    return VODItem(
        contentId = contentId,
        title = title,
        description = description,
        thumbnail = thumbnail,
        rating = rating,
        origRating = origRating,
        year = year,
        genre = genre,
        language = language,
        type = convertToVODType(type),
        runtime = runtime ?: "",
        vodPlaybackUrl = vodPlaybackUrl ?: "",
        licenseServerUrl = decryptLicenceUrl(licenseServerUrl),
        numSeasons = numSeasons ?: 1,
        resumeFromSec = resumeFromSec,
        trailerUrl = trailerUrl ?: "",
        progress = progress,
        cast = cast
    )
}

/**
 * The function to convert remote data(VodItemsDto) to UI data(VODItems)
 */
fun VodItemsDto.toVodItems(): VODItems {
    return VODItems(
        name = name,
        items = items.let { data ->
            data.map { it.toVodItem() }
        })
}

/**
 * The function to convert remote data(SingleWorkDto) to UI data(SingleWork)
 */
fun SingleWorkDto.toSingleWork(): SingleWork {
    return SingleWork(
        contentId = contentId,
        title = title,
        description = description,
        thumbnail = thumbnail,
        rating = rating,
        origRating = origRating,
        year = year,
        genre = genre,
        language = language,
        type = convertToVODType(type),
        runtime = runtime,
        vodPlaybackUrl = vodPlaybackUrl,
        licenseServerUrl = decryptLicenceUrl(licenseServerUrl),
        trailerUrl = trailerUrl,
        cast = cast,
        director = director,
        resumeFromSec = resumeFromSec,
        durationSec = durationSec,
        progress = ((resumeFromSec.toFloat()).div(durationSec.toFloat()))
    )
}

/**
 * The function to convert remote data(SeriesMetadataDto) to UI data(SeriesMetadata)
 */
fun SeriesMetadataDto.toSeriesMetadata(): SeriesMetadata {
    return SeriesMetadata(
        contentId = contentId,
        title = title,
        description = description,
        thumbnail = thumbnail,
        rating = rating,
        origRating = origRating,
        year = year,
        genre = genre,
        language = language,
        type = convertToVODType(type),
        cast = cast,
        runtime = runtime ?: "",
        vodPlaybackUrl = vodPlaybackUrl ?: "",
        licenseServerUrl = decryptLicenceUrl(licenseServerUrl),
        trailerUrl = trailerUrl,
        numSeasons = numSeasons ?: 1,
        resumeFromSeason = resumeFromSeason,
        resumeFromEpisode = resumeFromEpisode,
        episodeDurationSec = episodeDurationSec ?: 0,
        episodePlaybackUrl = episodePlaybackUrl,
        resumeFromSec = resumeFromSec,
        progress = resumeFromSec.toFloat().div(episodeDurationSec?.toFloat() ?: 1f),
    )
}

/**
 * The function to convert remote data(EpisodeDto) to UI data(Episode)
 */
fun EpisodeDto.toEpisode(): Episode {
    return Episode(
        contentId = contentId,
        title = title,
        description = description,
        thumbnail = thumbnail,
        rating = rating,
        origRating = origRating,
        year = year,
        genre = genre,
        language = language,
        type = convertToVODType(type),
        runtime = runtime ?: "",
        vodPlaybackUrl = vodPlaybackUrl,
        licenseServerUrl = decryptLicenceUrl(licenseServerUrl),
        trailerUrl = trailerUrl,
        director = director,
        resumeFromSec = resumeFromSec,
        durationSec = durationSec ?: 0,
        episode = episode,
        season = season,
        cast = cast,
        progress = ((resumeFromSec.toFloat()).div(durationSec?.toFloat() ?: 0F)),
        numSeasons = numSeasons ?: 1
    )
}

/**
 * The function to convert remote data(SeasonDto) to UI data(Season)
 */
fun SeasonDto.toSeasons(): Season {
    return Season(
        season = season,
        episodes = episodes?.let { episodes ->
            episodes.map { episodeDto -> episodeDto.toEpisode() }
        } ?: arrayListOf()
    )
}


