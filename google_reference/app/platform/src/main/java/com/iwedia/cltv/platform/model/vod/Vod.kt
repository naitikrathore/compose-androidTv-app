package com.iwedia.cltv.platform.model.vod

data class Vod(
    var id: Int,
    val title: String,
    var shortDescription: String,
    var extendedDescription: String,
    var imagePath: String,
    var backgroundImagePath: String,
    var durationSec: Int,
    var releaseYear: Int,
    var trailers: ArrayList<String>,
    var videoUrl: String?,
    var rating: Float,
    var currency: String,
    var price: Float,
    var actors: ArrayList<String>,
    var directors: ArrayList<String>,
    var categoryIds: ArrayList<Int>?,
    var data: Any?
)
