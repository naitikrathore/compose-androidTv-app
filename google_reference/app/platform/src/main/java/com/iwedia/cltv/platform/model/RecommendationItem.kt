package com.iwedia.cltv.platform.model

data class RecommendationItem(
    val type: String,
    val title: String,
    val thumbnail: String,
    val description: String = "",
    val playbackUrl: String = "",
    val channelId: String = "",
    val startTimeEpoch: Long = 0L,
    val durationSec: Long = 0L,
    val rating: String = "",
    val genre: String = "",
    val language: String= "",
    val previewUrl: String = "",
    val previewUrlSkipSec: Long = 0L,
    val contentId: String,
) {
    override fun toString(): String {
        return "RecommendationItem (type=$type, title=$title, thumbnail='$thumbnail')"
    }
}
