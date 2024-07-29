package com.iwedia.cltv.platform.model

enum class PlatformType {
    BASE,
    FAST,
    GRETZKY,
    MTK,
    RTK,
    REF_PLUS_5,
    IW_ATSC_3;

    companion object {
        fun getPlatformName(type: PlatformType): String {
            return when(type) {
                BASE -> "BASE"
                FAST -> "FAST"
                GRETZKY -> "GRETZKY"
                MTK -> "MediaTeK"
                RTK -> "RealTek"
                REF_PLUS_5 -> "RefPlus_5.0"
                IW_ATSC_3 -> "ATSC_3.0"
            }
        }
    }
}