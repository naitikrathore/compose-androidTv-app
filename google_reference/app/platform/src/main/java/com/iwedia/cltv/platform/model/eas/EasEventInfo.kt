package com.iwedia.cltv.platform.model.eas

data class EasEventInfo(
    var alertText: String? = "",
    var activationText: String? = "",
    var isAtsc3: Boolean = false,
    var channelChangeUri: String? = "",
    var isChannelChange : Boolean? = false
)
