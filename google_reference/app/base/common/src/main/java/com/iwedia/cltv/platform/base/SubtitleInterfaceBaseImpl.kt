package com.iwedia.cltv.platform.base

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.media.tv.TvTrackInfo
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.SubtitleInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.content_provider.Contract

open class SubtitleInterfaceBaseImpl(private var context: Context, private val utilsInterface: UtilsInterface, playerInterface: PlayerInterface) :
    SubtitleInterface {

    val KEY_SUBTITLE_ENABLED = "media.subtitles.enabled"
    val KEY_SUBTITLE_TYPE = "media.subtitles.preferredtype"
    val KEY_PREFERRED_SUBTITLE_LANGUAGE = "preferred_subtitle_language"
    val KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE = "preferred_second_subtitle_language"

    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo): Boolean {
        return false
    }

    override fun enableHardOfHearing(enable: Boolean) {
    }

    override fun enableSubtitles(enable: Boolean) {
        utilsInterface.setPrefsValue(KEY_SUBTITLE_ENABLED, enable)
    }

    override fun getSubtitlesState(): Boolean {
        return utilsInterface.getPrefsValue(KEY_SUBTITLE_ENABLED, false) as Boolean
    }

    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
    }

    override fun setPrimarySubtitleLanguage(language: String) {
        utilsInterface.setPrefsValue(KEY_PREFERRED_SUBTITLE_LANGUAGE, language)
    }

    override fun setSecondarySubtitleLanguage(language: String) {
        utilsInterface.setPrefsValue(KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE, language)
    }

    override fun getHardOfHearingState(): Boolean {
        return false
    }

    override fun getSubtitlesType(): Int {
        return 0
    }

    override fun getPrimarySubtitleLanguage(): String? {
        return utilsInterface.getPrefsValue(KEY_PREFERRED_SUBTITLE_LANGUAGE, "") as String
    }

    override fun getSecondarySubtitleLanguage(): String? {
        return utilsInterface.getPrefsValue(KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE, "") as String
    }

    override fun updateSubtitleTracks() {
    }

    override fun setAnalogSubtitlesType(value: String) {
    }

    override fun getAnalogSubtitlesType(): String? {
        return ""
    }

    @SuppressLint("Range")
    fun readInternalProviderData(key: String): String? {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            Contract.buildConfigUri(1),
            null,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            if (cursor.getString(cursor.getColumnIndex(Contract.Config.CURRENT_COUNTRY_COLUMN)) != null) {
                val country = cursor.getString(cursor.getColumnIndex(Contract.Config.CURRENT_COUNTRY_COLUMN)).toString()
                return country
            }
        }
        return null
    }

}
