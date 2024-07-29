package com.iwedia.cltv.platform.mal_service

import android.media.tv.TvTrackInfo
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.SubtitleInterface

class SubtitleInterfaceImpl(private val serviceImpl: IServiceAPI) : SubtitleInterface {
    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo): Boolean {
        return serviceImpl.hasHardOfHearingSubtitleInfo(tvTrackInfo)
    }

    override fun enableHardOfHearing(enable: Boolean) {
        serviceImpl.enableHardOfHearing(enable)
    }

    override fun enableSubtitles(enable: Boolean) {
        serviceImpl.enableSubtitles(enable)
    }

    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
        serviceImpl.setSubtitlesType(position, updateSwitch)
    }

    override fun setPrimarySubtitleLanguage(language: String) {
        serviceImpl.primarySubtitleLanguage = language
    }

    override fun setSecondarySubtitleLanguage(language: String) {
        serviceImpl.secondarySubtitleLanguage = language
    }

    override fun getHardOfHearingState(): Boolean {
        return serviceImpl.hardOfHearingState
    }

    override fun getSubtitlesState(): Boolean {
        return serviceImpl.subtitlesState
    }

    override fun getSubtitlesType(): Int {
        return serviceImpl.subtitlesType
    }

    override fun getPrimarySubtitleLanguage(): String? {
        return serviceImpl.primarySubtitleLanguage
    }

    override fun getSecondarySubtitleLanguage(): String? {
        return serviceImpl.secondarySubtitleLanguage
    }

    override fun updateSubtitleTracks() {
        serviceImpl.updateSubtitleTracks()
    }

    override fun setAnalogSubtitlesType(value: String) {
        serviceImpl.analogSubtitlesType = value
    }

    override fun getAnalogSubtitlesType(): String? {
        return serviceImpl.analogSubtitlesType
    }

}