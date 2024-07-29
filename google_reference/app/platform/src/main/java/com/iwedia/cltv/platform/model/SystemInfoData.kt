package com.iwedia.cltv.platform.model

data class SystemInfoData (
    var displayNumber :Long = 0L,
    var displayName :String ?="",
    var providerData :String ?= "",
    var logoImagePath :String ?= "",
    var isRadioChannel :Boolean = false,
    var isSkipped :Boolean = false,
    var isLocked :Boolean = false,
    var tunerType :Int = -1,
    var ordinalNumber:Int = 0,
    var frequency :Int = 0,
    var tsId:Int = 0,
    var onId:Int = 0,
    var serviceId:Int = 0,
    var bandwidth :String ?= "",
    var networkId :Int = 0,          // Unsupported
    var networkName :String ?= "",      // Unsupported
    var postViterbi :String ?= "",    // Unsupported
    var attr5s :String ?= "",           // Unsupported
    var signalQuality :Int =0,
    var signalStrength :Int =0 ,
    var signalBer :Int=0,
    var signalAGC:Int =0,    // Unsupported
    var signalUEC :Int=0,     // Unsupported
)