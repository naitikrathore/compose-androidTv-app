package com.iwedia.cltv.platform.model

import com.google.gson.annotations.SerializedName

/**
 * Model class to handle channel list response data
 *
 * @author Abhilash M R
 */
data class ChannelListModel(
    @SerializedName("channelId")
    val channelId: String,
    @SerializedName("logo")
    val logo: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("playbackUrl")
    val playbackUrl: String,
    @SerializedName("rating")
    val rating: String,
    @SerializedName("genre")
    val genre: List<String>,
    @SerializedName("resolution")
    val resolution: List<String>,
    @SerializedName("licenseServerUrl")
    val licenseServerUrl: String
)