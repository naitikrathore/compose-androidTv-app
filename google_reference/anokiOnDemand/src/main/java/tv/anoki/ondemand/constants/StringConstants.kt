package tv.anoki.ondemand.constants

object StringConstants {

    const val ROUTE_VOD_LISTING = "vod/listing"
    const val ROUTE_VOD_SINGLE_WORK_DETAILS = "vod/single-work/{contentId}/details"
    const val ROUTE_VOD_SERIES_DETAILS = "vod/series/{contentId}/details"
    const val ROUTE_VOD_SEASONS_AND_EPISODES =
        "vod/series/{contentId}/{title}/{resumeFromSeason}/{resumeFromEpisode}/seasons/"
    const val ROUTE_PARAM_CONTENT_ID = "{contentId}"
    const val ROUTE_PARAM_CONTENT_TITLE = "{title}"
    const val ROUTE_PARAM_CONTENT_RESUME_FROM_SEASON = "{resumeFromSeason}"
    const val ROUTE_PARAM_CONTENT_RESUME_FROM_EPISODE = "{resumeFromEpisode}"

    const val BUNDLE_VOD_PLAYER_URL = "url_key"
    const val ACTION_VOD_PLAYER_PLAYBACK = "fast_vod_playback"
    const val ACTION_VOD_PLAYER_START_TIMER = "fast_vod_start_timer"
    const val ACTION_VOD_PLAYER_STOP_TIMER = "fast_vod_stop_timer"
    const val ACTION_VOD_PLAYER_STOP_PLAYER = "fast_vod_stop_player"
    const val ACTION_VOD_PLAYER_RELEASE_PLAYER = "fast_vod_release_player"
    const val ACTION_VOD_PLAYER_PAUSE_PLAYER = "fast_vod_pause_player"
    const val ACTION_VOD_PLAYER_PLAY_PLAYER = "fast_vod_play_player"
    const val ACTION_VOD_PLAYER_SEEK_TO = "fast_vod_seek_to_player"
    const val BUNDLE_VOD_PLAYER_RESUME_FROM = "player_resume_from"
    const val BUNDLE_VOD_PLAYER_IS_PLAY = "player_play_pause"
    const val BUNDLE_VOD_PLAYER_SEEK_TO = "player_seek_to"
    const val ACTION_VOD_BACKWARD_PLAYER = "fast_vod_backward_player"
    const val ACTION_VOD_FORWARD_PLAYER = "fast_vod_forward_player"
    const val BUNDLE_VOD_PLAYER_LICENCE_URL = "player_play_licence_url"
}
