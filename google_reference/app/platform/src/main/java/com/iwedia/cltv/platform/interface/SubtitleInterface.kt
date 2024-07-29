package com.iwedia.cltv.platform.`interface`

import android.media.AudioManager
import android.media.tv.TvTrackInfo
import com.iwedia.cltv.platform.model.player.track.ISubtitle

interface SubtitleInterface {
    fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo) : Boolean
    fun enableHardOfHearing(enable: Boolean)
    fun enableSubtitles(enable: Boolean)
    fun setSubtitlesType(position: Int, updateSwitch: Boolean)
    fun setPrimarySubtitleLanguage(language: String)
    fun setSecondarySubtitleLanguage(language: String)
    fun getHardOfHearingState(): Boolean
    fun getSubtitlesState(): Boolean
    fun getSubtitlesType(): Int
    fun getPrimarySubtitleLanguage(): String?
    fun getSecondarySubtitleLanguage(): String?
    fun updateSubtitleTracks()
    fun setAnalogSubtitlesType(value: String)
    fun getAnalogSubtitlesType(): String?
}