package com.iwedia.cltv.platform.model

import com.google.gson.annotations.SerializedName

/**
 * Model class to handle EPG response data
 *
 * @author Abhilash M R
 */
data class ProgramListModel(
    @SerializedName("channelId")
    val channelId: String,
    @SerializedName("items")
    val programList: List<Program>,
)

data class Program(
    @SerializedName("startTime")
    val startTime: String,
    @SerializedName("startTimeEpoch")
    val startTimeEpoch: Long,
    @SerializedName("durationSec")
    val durationSec: Long,
    @SerializedName("contentId")
    val contentId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("thumbnail")
    val thumbnail: String,
    @SerializedName("image")
    val image: String,
    @SerializedName("rating")
    val rating: String,
    @SerializedName("year")
    val year: String,
    @SerializedName("genre")
    val genre: String,
    @SerializedName("director")
    val director: String,
    @SerializedName("cast")
    val cast: String,
    @SerializedName("runtime")
    val runtime: String,
    @SerializedName("language")
    val language: String,
)
