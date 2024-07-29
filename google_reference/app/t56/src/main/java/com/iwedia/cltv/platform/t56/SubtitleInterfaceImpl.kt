package com.iwedia.cltv.platform.t56

import android.content.Context
import android.media.tv.TvTrackInfo
import android.util.Log
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.SubtitleInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.SubtitleInterfaceBaseImpl
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvConfigType

class SubtitleInterfaceImpl(context: Context, private var utilsInterface: UtilsInterface, playerInterface: PlayerInterface):
    SubtitleInterfaceBaseImpl(context, utilsInterface, playerInterface) {

    private val TAG = javaClass.simpleName
    private var primarySubtitlesLang: String = ""
    private var secondarySubtitlesLang: String = ""

    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo): Boolean {
        var hasHoHKey= false
        try {
            if (tvTrackInfo.extra.getString("key_HearingImpaired")?.toBoolean() == true) {
                hasHoHKey = true
            }
        } catch (e: java.lang.Exception) { }
        return hasHoHKey
    }

    override fun enableSubtitles(enable: Boolean) {
        if (enable) {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX, 1)
        } else {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX, 0)
        }
    }

    override fun getSubtitlesState(): Boolean {
        return MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX) != 0
    }

    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
        if (position == 0) {
            enableHardOfHearing(false)
        }
        if (position == 1) {
            enableHardOfHearing(true)
        }
    }

    override fun enableHardOfHearing(enable: Boolean) {
        if (enable) {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR, 1)
        } else {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR, 0)
        }
    }

    override fun setPrimarySubtitleLanguage(language: String) {
        MtkTvConfig.getInstance()
            .setLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG, language)
        primarySubtitlesLang = language
    }

    override fun setSecondarySubtitleLanguage(language: String) {
        MtkTvConfig.getInstance()
            .setLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG_2ND, language)
        secondarySubtitlesLang = language
    }

    override fun getSubtitlesType(): Int {
        return if (getHardOfHearingState()) {
            1
        } else {
            0
        }
    }

    override fun getHardOfHearingState(): Boolean {
        if(MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR) == 1) {
            return true
        }
        return false
    }

    override fun getPrimarySubtitleLanguage(): String? {
        var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage: $language")
        if(language == null) {
            language = utilsInterface.getLanguageMapper()?.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage replaced with: $language")
        }
        return language
    }

    override fun getSecondarySubtitleLanguage(): String? {
        var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG_2ND)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSecondarySubtitleLanguage: $language")
        if(language == null) {
            language = "eng"
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage replaced with: $language")
        }
        return language
    }
}