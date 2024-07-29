package com.iwedia.cltv.platform.base.player

import android.media.tv.TvTrackInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.player.TrackBase.AudioTrack
import com.iwedia.cltv.platform.base.player.TrackBase.SubtitleTrack

open class TrackBaseManager(val utilsInterface: UtilsInterface): TrackManager<AudioTrack, SubtitleTrack>() {
    @RequiresApi(Build.VERSION_CODES.R)
    open fun updateTracks(
        activeAudioTrackId: String?,
        activeSubtitleTrackId: String?,
        tracks: List<TvTrackInfo>
    ) {
        clear()

        synchronized(lock) {
            for (track in tracks) {
                if (track.type == TvTrackInfo.TYPE_AUDIO) {
                    val audioTrack = AudioTrack(track, utilsInterface, false, "")
                    mAudioTracks.add(audioTrack)
                    if (activeAudioTrackId != null && track.id == activeAudioTrackId) {
                        currentAudioTrack = audioTrack
                    }
                }
                else if (track.type == TvTrackInfo.TYPE_SUBTITLE) {
                    val subtitleTrack = SubtitleTrack(track, utilsInterface)
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

    fun selectAudioTrack(trackId: String?) {
        currentAudioTrack =
            if (trackId == null) {
                null
            }
            else {
                synchronized(lock) {
                    mAudioTracks.find { it.trackInfo.id == trackId }
                }
            }
    }

    fun selectSubtitleTrack(trackId: String?) {
        currentSubtitleTrack =
            if (trackId == null) {
                null
            }
            else {
                synchronized(lock) {
                    mSubtitleTracks.find { it.trackInfo.id == trackId }
                }
            }
    }
}