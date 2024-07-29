package com.iwedia.cltv.platform.refplus5.screenMode

import android.net.Uri

class Constants {
    companion object {
        val GLOBAL_PROVIDER_URI_URI = Uri.parse("content://com.mediatek.tv.internal.data/global_value")
        const val PQ_SETTINGS_AUTHORITY = "com.mediatek.tv.settingspqdb"

        const val CFG_FISION_PIC_PICTURE_FORMAT = "pictrue_format"
        const val VIDEO_UPDATED_KEYS = "VIDEO_UPDATED_KEYS"
        const val CURRENT_SOURCE = "M_CURRENT_SOURCE"

        private const val CFG_GRP_BS_PREFIX = "g_bs__"
        const val CFG_BS_BS_SRC = CFG_GRP_BS_PREFIX + "bs_src"
        const val CFG_BS_BS_USER_SRC = CFG_GRP_BS_PREFIX + "bs_user_src"

        const val WIDTH: String = "WIDTH"
        const val HEIGHT: String = "HEIGHT"
        const val FRAME_RATE: String = "FRAME_RATE"

        private const val RESOLUTION_CATEGORY: String = "resolution"
        private const val SOURCE_CATEGORY: String = "source"
        private const val SETTING_CATEGORY_GENERAL: String = "general"
        private const val RESET_CATEGORY: String = "reset"

        val RESOLUTION_URI: Uri = Uri.parse("content://$PQ_SETTINGS_AUTHORITY/$RESOLUTION_CATEGORY")
        val SOURCE_URI_URI: Uri = Uri.parse("content://$PQ_SETTINGS_AUTHORITY/$SOURCE_CATEGORY")
        val SETTINGS_URI_URI: Uri = Uri.parse("content://$PQ_SETTINGS_AUTHORITY/$SETTING_CATEGORY_GENERAL")
        val RESET_URI_URI: Uri = Uri.parse("content://$PQ_SETTINGS_AUTHORITY/$RESET_CATEGORY")
    }
}