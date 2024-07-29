package com.iwedia.cltv.platform.model.vod

data class VodSeries(
    var id: Int,
    var title: String,
    var shortDescription: String,
    var extendedDescription: String,
    var imagePath: String,
    var backgroundImagePath: String,
    var durationSec: Int,
    var releaseYear: Int,
    var trailers: ArrayList<String>,
    var videoUrl: String?,
    var rating: Float,
    var parentalRating: Float,
    var currency: String,
    var price: Float,
    var actors: ArrayList<String>,
    var directors: ArrayList<String>,
    var producers: ArrayList<String>,
    var copyright: String,
    var categoryIds: ArrayList<Int>?,
    var seasons: ArrayList<VodSeason>,
    var data: Any?
)
