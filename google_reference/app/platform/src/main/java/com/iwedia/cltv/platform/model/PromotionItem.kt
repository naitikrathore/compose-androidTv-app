package com.iwedia.cltv.platform.model

data class PromotionItem(
    val type: String,
    val banner: String?,
    val logo: String?,
    val callToAction: String?,
    val clickUrl: String?,
    val channelid: String?,
    val contentId: String?
) {

    override fun toString(): String {
        return "PromotionItem (type=$type, banner=$banner, logo='$logo', callToAction='$callToAction', clickUrl='$clickUrl', channelid='$channelid', contentId='$contentId')"
    }

    fun isVodContent(): Boolean = type == Constants.VodTypeConstants.SERIES || type == Constants.VodTypeConstants.SINGLE_WORK
}