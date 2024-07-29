package com.iwedia.cltv.platform.model

object Constants {

    const val PARENTAL_CONTROL_INTENT_ACTION = "com.google.android.tv.inputplayer.PARENTAL_CONTROL"
    const val CONST_CHANGE_INPUT_TYPE_FOCUS_DELAY: Int = 1000

    object LogTag {
        const val CLTV_TAG = "[CLTV] "
    }

    object ChannelConstants {
        const val ANDROID_SETTINGS_CHANNEL_SETTINGS = "android.settings.CHANNELS_SETTINGS"
        const val ANDROID_SETTINGS__SETTINGS = "android.settings.SETTINGS"
        const val COM_ANDROID_TV_SETTINGS = "com.android.tv.settings"
        const val MEDIATEK_TV_SETTINGS_CHANNEL_ACTIVITY = "com.mediatek.tv.settings.channelsetting.ChannelActivity"
        const val MEDIATEK_TV_SETTINGS_CHANNEL_ACTIVITY_T56 = "com.android.tv.settings.partnercustomizer.vendor.channels.ChannelsActivity"
    }

    object SharedPrefsConstants {
        //Shared preferences tags
        const val PARENTAL_CONTROLS_ENABLED_TAG = "ANOKI_PARENTAL_CONTROLS_ENABLED"
        const val ANOKI_PARENTAL_CONTROLS_LEVEL_TAG = "ANOKI_PARENTAL_CONTROLS_LEVEL"
        const val ANOKI_PARENTAL_UNLOCKED_TAG = "ANOKI_PARENTAL_UNLOCKED"
        const val ANOKI_TEMPORARY_RATING_ENABLED_TAG = "ANOKI_TEMP_PARENTAL_CONTROLS_ENABLED"
        const val ANOKI_TEMPORARY_RATING_LEVEL_TAG = "ANOKI_TEMP_PARENTAL_CONTROLS_LEVEL"
        const val PREFS_KEY_ADVERTISING_ID = "TV_DEVICE_ADVERTISING_ID"
        const val PREFS_KEY_IP_ADDRESS = "TV_DEVICE_IP_ADDRESS"
        const val STARTED_CHANNEL_FROM_LIVE_TAB_TAG = "STARTED_APP_FROM_LIVE_TAB"
        const val ANOKI_SCAN_TAG = "ANOKI_SCAN"
        const val ANOKI_EPG_TAG = "ANOKI_EPG"
        const val PREFS_KEY_CURRENT_COUNTRY_ALPHA3 = "CURRENT_COUNTRY_ALPHA3"
        const val ANOKI_RECOMMENDATION_TAG = "ANOKI_RECOMMENDATIONS"
        const val ANOKI_GENRE_TAG = "ANOKI_GENRE"
        const val ANOKI_PROMOTION_TAG = "ANOKI_PROMOTION"
        const val PREFS_KEY_LOCKED_CHANNEL_IDS = "LOCKED_CHANNEL_IDS"
    }

    object AnokiParentalConstants {
        const val USE_ANOKI_RATING_SYSTEM = true
    }

    /**
     * @author Maksim
     *
     * This will be implemented through whole app
     * so there aren't any String literals but we should use constants
     * (as well as for all other constants in the future that i will be adding)
     */
    object FlavorConstants {
        const val BASE = "base"
        const val GRETZKY = "gretzky"
        const val MTK = "mtk"
        const val RTK = "rtk"
        const val ATSC = "atsc"
        const val REFPLUS5 = "refplus5"
    }

    object VodTypeConstants {
        const val SINGLE_WORK = "single-work"
        const val SERIES = "series"
    }
}