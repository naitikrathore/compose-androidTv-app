package com.iwedia.cltv.platform.model

data class FastUserSettingsItem(
    val auid: String?,
    val platformId: String?,
    val dnt: Int?
){
    override fun toString(): String {
        return "{dnt:$dnt, auid:$auid}"
    }
}