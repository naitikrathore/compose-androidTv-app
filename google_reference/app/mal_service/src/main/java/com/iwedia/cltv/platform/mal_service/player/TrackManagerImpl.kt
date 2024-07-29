package com.iwedia.cltv.platform.mal_service.player

import android.media.tv.TvTrackInfo
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.channel.TunerType

class TrackManagerImpl (utilsInterface: UtilsInterface) : TrackBaseManager(utilsInterface) {

    private var activePlayableItem: Any? = null

    private val analogAudioLanguage = arrayListOf(
        "Unknown",
        "Mono",
        "Stereo",
        "SAP",
        "Dual1",
        "Dual2",
        "Dual I+II",
        "NICAM mono",
        "NICAM stereo",
        "NICAM Dual I",
        "NICAM Dual II",
        "NICAM Dual I+II",
        "4#CH",
        "5.1#CH",
        "7.1#CH",
        "Dual Mono"
    )

    private fun isCurrentServiceAnalog() : Boolean {
        if (activePlayableItem != null) {
            if (activePlayableItem is TvChannel) {
                var currentChannel = (activePlayableItem as TvChannel)
                if (currentChannel.tunerType == TunerType.ANALOG_TUNER_TYPE) {
                    return true
                }
                return false
            }
        }
        return false
    }

    override fun updateTracks(
        activeAudioTrackId: String?,
        activeSubtitleTrackId: String?,
        tracks: List<TvTrackInfo>
    ) {
        clear()
        synchronized(lock) {
            for (track in tracks) {
                if (track.type == TvTrackInfo.TYPE_AUDIO) {
                    if (isCurrentServiceAnalog()) {
                        var analogIndex = track.extra.getInt("audio.atv-sound-mode")
                        var analogName = analogAudioLanguage[analogIndex]
                        val audioTrack = TrackBase.AudioTrack(track, utilsInterface, true, analogName)
                        mAudioTracks.add(audioTrack)
                        if (activeAudioTrackId != null && track.id == activeAudioTrackId) {
                            currentAudioTrack = audioTrack
                        }
                    } else {
                        val audioTrack = TrackBase.AudioTrack(track, utilsInterface, false, "")
                        mAudioTracks.add(audioTrack)
                        if (activeAudioTrackId != null && track.id == activeAudioTrackId) {
                            currentAudioTrack = audioTrack
                        }
                    }
                }
                else if (track.type == TvTrackInfo.TYPE_SUBTITLE) {
                    val subtitleTrack = TrackBase.SubtitleTrack(track, utilsInterface)
                    if (track.encoding != "teletext-full-page") {
                        mSubtitleTracks.add(subtitleTrack)
                        if (activeSubtitleTrackId != null && track.id == activeSubtitleTrackId){
                            currentSubtitleTrack = subtitleTrack
                        }
                    }
                }
            }
        }
    }

    fun setPlayableItem(playableItem: Any) {
        activePlayableItem = playableItem
    }
}