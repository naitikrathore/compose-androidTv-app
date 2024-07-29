package com.iwedia.cltv.platform.model

data class FastFavoriteItem(
    val auid: String,
    val platformId: String,
    val profileId: String,
    val channelId: String?,
    val favourite:Int?,
){
    override fun toString(): String {
        return "{profileId:$profileId, channelId:$channelId, favourite:'$favourite'}"
    }
}
