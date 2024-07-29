package com.iwedia.cltv.platform.model.vod

data class VodEpisode(
    var id: Int,
    var seasonId: Int,
    var seriesId: Int,
    var title: String,
    var shortDescription: String,
    var extendedDescription: String,
    var imagePath: String,
    var backgroundImagePath: String,
    var durationSec: Int,
    var releaseYear: Int,
    var date: String,
    var trailers: ArrayList<String>,
    var videoUrl: String,
    var rating: Float,
    var parentalRating: Int,
    var currency: String,
    var price: Float,
    var actors: ArrayList<String>,
    var directors: ArrayList<String>,
    var producers: ArrayList<String>,
    var copyright: String,
    var data: Any?
)
