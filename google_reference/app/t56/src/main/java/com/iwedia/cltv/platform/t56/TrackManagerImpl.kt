package com.iwedia.cltv.platform.t56

import android.media.tv.TvTrackInfo
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.player.TrackBase
import com.iwedia.cltv.platform.base.player.TrackBaseManager
import com.mediatek.twoworlds.tv.MtkTvAVMode
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.model.MtkTvAnalogChannelInfo
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase

class TrackManagerImpl(utilsInterface: UtilsInterface) : TrackBaseManager(utilsInterface) {

    private val analogAudioLanguage = arrayListOf(
        "Unknown",
        "Mono",
        "Stereo",
        "SAP",
        "Dual1",
        "Dual2",
        "NICAM mono",
        "NICAM stereo",
        "NICAM Dual I",
        "NICAM Dual II",
        "Dual I+II",
        "NICAM Dual I+II",
        "FM mono",
        "FM stereo"
    )

    override fun updateTracks(
        activeAudioTrackId: String?,
        activeSubtitleTrackId: String?,
        tracks: List<TvTrackInfo>
    ) {
        clear()

        synchronized(lock) {
            for (track in tracks) {
                if (track.type == TvTrackInfo.TYPE_AUDIO) {
                    if (MtkTvChannelListBase.getCurrentChannel() is MtkTvAnalogChannelInfo) {
                        var mtkAudioTracks: List<TvProviderAudioTrackBase>? = MtkTvAVMode.getInstance().audioAvailableRecord
                            mtkAudioTracks?.forEach { mtkAudioTrack ->
                                var audioLanguage = mtkAudioTrack.audioLanguage
                                if(mtkAudioTrack.audioId < analogAudioLanguage.size) {
                                    audioLanguage = analogAudioLanguage[mtkAudioTrack.audioId]
                                }
                                if (track.id == mtkAudioTrack.audioId.toString()) {
                                    val audioTrack =
                                        TrackBase.AudioTrack(track, utilsInterface, true, audioLanguage)
                                    mAudioTracks.add(audioTrack)
                                    if (activeAudioTrackId != null && track.id == activeAudioTrackId) {
                                        currentAudioTrack = audioTrack
                                    }
                                }
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
}