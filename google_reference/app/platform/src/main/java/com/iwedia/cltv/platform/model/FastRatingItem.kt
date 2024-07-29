package com.iwedia.cltv.platform.model

data class FastRatingItem(
    val auid: String?,
    val platformId: String?,
    val rating: String?
){
    override fun toString(): String {
        return "{auid:$auid, rating:$rating}"
    }
}
