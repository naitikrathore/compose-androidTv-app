package com.iwedia.cltv.platform.model

data class FastTosOptInItem(
    val auid: String?,
    val platformId: String?,
    val tosOptIn: Int?
){
    override fun toString(): String {
        return "{auid:$auid, tosOptIn:$tosOptIn}"
    }
}
