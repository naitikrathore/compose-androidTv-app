package com.iwedia.cltv.platform.model

data class FastInfoItem(
    val auid: String,
    val tosOptIn: Int,
    val splashImageUrl: String?,
    val tosUrl: String?,
    val privacyPolicyUrl: String?,
){
    override fun toString(): String {
        return "FastInfoItem (auid=$auid, tosOptIn=$tosOptIn, splashImageUrl='$splashImageUrl', tosUrl='$tosUrl', privacyPolicyUrl='$privacyPolicyUrl')"
    }
}
