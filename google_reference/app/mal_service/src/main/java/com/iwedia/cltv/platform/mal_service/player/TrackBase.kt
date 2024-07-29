package com.iwedia.cltv.platform.mal_service.player

import android.media.tv.TvTrackInfo
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.player.track.ITrack


sealed class TrackBase(val trackInfo: TvTrackInfo): ITrack {

    protected  val undefinedString = "undefined"
    protected  val undefinedLangCode = "und"

    override val trackId: String = trackInfo.id

    //todo remove utilsInterface calls and replace functions from utils to player
    class AudioTrack(val track: TvTrackInfo, val utilsInterface: UtilsInterface,
                     override var isAnalogTrack: Boolean,
                     override var analogName: String?,
    ): TrackBase(track), IAudioTrack {
        override var languageName: String = getTrackLanguageName()
        override var trackName: String = getAudioTrackName()
        override var isAd: Boolean = getIsAd()
        override var isDolby: Boolean = getIsDolby()
        override val languageCode: String = languageCode()
        override var isHohAudio : Boolean = getHohAudio()

        private fun getAudioTrackName(): String {
            var trackName = getLanguageCodeMapper(languageName, utilsInterface)

            if (trackName == undefinedLangCode) {

                if (isAnalogTrack) {
                    trackName = analogName ?: trackName
                }
            }
            return trackName
        }

        private fun getIsAd(): Boolean {
            return (utilsInterface.hasAudioDescription(track))
        }

        private fun getIsDolby(): Boolean {
            return utilsInterface.getCodecDolbySpecificAudioInfo(track).isNotEmpty()
        }
        private fun getTrackLanguageName(): String {
            return if (track.language == null || trackInfo.language?.length!! <= 1 ) {
                undefinedLangCode
            } else {
                track.language
            }
        }
        private fun languageCode() : String {
            return utilsInterface.getLanguageMapper()!!.getLanguageCode(languageName)
        }

        private fun getHohAudio() : Boolean {
            return utilsInterface.hasHohAudio(track)
        }
    }

    class SubtitleTrack(val track: TvTrackInfo, val utilsInterface: UtilsInterface): TrackBase(track), ISubtitle {
        override var languageName: String = getTrackLanguageName()
        override val trackName: String = getSubtitleTrackName()
        override var isHoh: Boolean = getIsHoh()
        override var isTxtBased: Boolean = getIsTxtBased()
        override val languageCode: String = languageCode()
        private fun getTrackLanguageName(): String {
            return if (track.language == null || trackInfo.language?.length!! <= 1) {
                undefinedLangCode
            } else {
                track.language
            }
        }

        private fun getSubtitleTrackName(): String {
            var trackName = getLanguageCodeMapper(languageName, utilsInterface)
            return trackName
        }

        private fun getIsHoh(): Boolean {
            return utilsInterface.hasHardOfHearingSubtitleInfo(track)
        }

        private fun getIsTxtBased(): Boolean {
            return utilsInterface.isTeletextBasedSubtitle(track)
        }

        private fun languageCode() : String {
            return utilsInterface.getLanguageMapper()!!.getLanguageCode(languageName)
        }

    }

    fun getLanguageCodeMapper(language: String, utilsInterface: UtilsInterface) : String{
        return utilsInterface.getLanguageMapper()!!.getLanguageName(language) ?: undefinedString
    }
}