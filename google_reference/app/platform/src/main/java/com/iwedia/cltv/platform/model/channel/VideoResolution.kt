package com.iwedia.cltv.platform.model.channel

enum class VideoResolution {
    VIDEO_RESOLUTION_ED,
    VIDEO_RESOLUTION_FHD,
    VIDEO_RESOLUTION_SD,
    VIDEO_RESOLUTION_HD,
    VIDEO_RESOLUTION_UHD;

    companion object {
        fun getVideoResolutionById(id: Int): VideoResolution {
            return when (id) {
                0 -> {
                    VIDEO_RESOLUTION_ED
                }
                1 -> {
                    VIDEO_RESOLUTION_FHD
                }
                2 -> {
                    VIDEO_RESOLUTION_SD
                }
                3 -> {
                    VIDEO_RESOLUTION_HD
                }
                else -> VIDEO_RESOLUTION_UHD
            }
        }
    }
}