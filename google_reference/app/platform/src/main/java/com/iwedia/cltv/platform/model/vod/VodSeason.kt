package com.iwedia.cltv.platform.model.vod

data class VodSeason(
    var id: Int,
    var seriesId: Int,
    var seasonPoster: String?,
    var year: Int,
    var seasonPrice: Float,
    var episodes: ArrayList<VodEpisode>
)