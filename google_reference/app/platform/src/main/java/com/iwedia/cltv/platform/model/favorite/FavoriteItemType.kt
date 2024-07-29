package com.iwedia.cltv.platform.model.favorite

import com.iwedia.cltv.platform.model.channel.FilterItemType

enum class FavoriteItemType {
    TV_CHANNEL(100),
    TV_PVR(200),
    TV_CATCH_UP(300),
    VOD(400),
    CONTINUE_WATCHING(500),
    TRAILER(600),
    TV_START_OVER(700);

    private var id: Int = -1

    constructor(id: Int) {
        this.id = id
    }

    fun getFilterId(): Int = id

    companion object {
        fun getFilterTypeById(id: Int): FavoriteItemType {
            return when (id) {
                100 -> FavoriteItemType.TV_CHANNEL
                200 -> FavoriteItemType.TV_PVR
                300 -> FavoriteItemType.TV_CATCH_UP
                400 -> FavoriteItemType.VOD
                500 -> FavoriteItemType.CONTINUE_WATCHING
                600 -> FavoriteItemType.TRAILER
                700 -> FavoriteItemType.TV_START_OVER
                else -> FavoriteItemType.TV_CHANNEL
            }
        }
    }
}

