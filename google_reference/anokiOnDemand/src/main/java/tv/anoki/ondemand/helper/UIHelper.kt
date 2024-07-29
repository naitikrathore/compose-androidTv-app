package tv.anoki.ondemand.helper

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.anoki.ondemand.R
import tv.anoki.ondemand.constants.StringConstants

/**
 * The function to build the seasons and episodes screen route
 *
 * @param route the seasons and episodes route string
 * @param contentId the unique id of series type content
 * @param title the title string of series type content
 * @param resumeFromSeason the int value of previously watched season
 * @param resumeFromEpisode the int value of previously watched episode
 */
fun buildSeasonsEpisodesScreenRoute(
    route: String,
    contentId: String,
    title: String,
    resumeFromSeason: Int,
    resumeFromEpisode: Int
): String {
    return route
        .replace(StringConstants.ROUTE_PARAM_CONTENT_ID, contentId)
        .replace(StringConstants.ROUTE_PARAM_CONTENT_TITLE, title)
        .replace(StringConstants.ROUTE_PARAM_CONTENT_RESUME_FROM_SEASON, "$resumeFromSeason")
        .replace(StringConstants.ROUTE_PARAM_CONTENT_RESUME_FROM_EPISODE, "$resumeFromEpisode")
}

/**
 * The function to build the strings to show numbers of seasons
 */
@Composable
fun buildNumSeasonsString(numSeasons: Int): String {
    return stringResource(
        id = R.string.text_number_of_seasons,
        numSeasons,
        if (numSeasons > 1) stringResource(
            id = R.string.text_s
        ) else ""
    )
}