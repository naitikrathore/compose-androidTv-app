package com.iwedia.cltv.sdk.entities

import TvConfigurationHelper.Companion.CODEC_AUDIO_AC3
import TvConfigurationHelper.Companion.CODEC_AUDIO_AC3_ATSC
import TvConfigurationHelper.Companion.CODEC_AUDIO_DTS
import TvConfigurationHelper.Companion.CODEC_AUDIO_EAC3
import TvConfigurationHelper.Companion.CODEC_AUDIO_EAC3_ATSC
import android.media.MediaPlayer
import android.media.tv.TvTrackInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.handlers.ReferencePlayerHandler

open class ReferenceTrackObject {

    enum class Type {
        AUDIO,
        SUBTITLE
    }

    var type: Type
    var trackInfo: TvTrackInfo

    constructor(type: Type, trackInfo: TvTrackInfo) {
        this.type = type
        this.trackInfo = trackInfo
    }

    fun getName(): String {

        if (trackInfo!!.language.length <= 1)
            return "Undefined"

        if (trackInfo!!.language.length == 2) {
            if (LanguageCodeMapper.getLanguageName(trackInfo!!.language) == null) {
                return "Undefined"
            }
            return LanguageCodeMapper.getLanguageName(trackInfo!!.language)!!
        } else {
            if (LanguageCodeMapper.getLanguageName(trackInfo!!.language.substring(0, 3)) == null) {
                return "Undefined"
            }

            return LanguageCodeMapper.getLanguageName(trackInfo!!.language.substring(0, 3))!!
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getAudioSpecificInfo(language : String, adIndication : String): String {
        var languageInfo  = language
        val DOLBY_AUDIO_EAC3_NAME_TYPE = " - Dolby D+" //" [EAC-3]"
        val DOLBY_AUDIO_AC3_NAME_TYPE = " - Dolby D"   // " [AC-3]"
        val AUDIO_DTS_TYPE = " [DTS]"

        if (TvConfigurationHelper.hasAudioDescription(trackInfo )) {
            languageInfo += adIndication
        }
        val result = (ReferenceSdk.playerHandler as ReferencePlayerHandler).getDolbyType(TvTrackInfo.TYPE_AUDIO, trackInfo.id)


        if(!result.isNullOrEmpty()){
            languageInfo = when (result) {
                CODEC_AUDIO_EAC3, CODEC_AUDIO_EAC3_ATSC -> languageInfo +  DOLBY_AUDIO_EAC3_NAME_TYPE
                CODEC_AUDIO_AC3, CODEC_AUDIO_AC3_ATSC -> languageInfo + DOLBY_AUDIO_AC3_NAME_TYPE
                CODEC_AUDIO_DTS -> languageInfo + AUDIO_DTS_TYPE
                else -> languageInfo
            }
        }
        return languageInfo
    }

}