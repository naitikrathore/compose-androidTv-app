package com.iwedia.cltv.platform.rtk

import android.media.tv.TvTrackInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.player.PlayerBaseImpl
import com.iwedia.cltv.platform.base.player.TrackBase
import com.iwedia.cltv.platform.base.player.TrackBase.AudioTrack
import com.iwedia.cltv.platform.base.player.TrackBase.SubtitleTrack
import com.iwedia.cltv.platform.base.player.TrackBaseManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.realtek.tv.TVMediaTypeConstants
import com.realtek.tv.Tv

open class TrackManagerImpl(utilsInterface: UtilsInterface): TrackBaseManager(utilsInterface) {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun updateTracks(
        activeAudioTrackId: String?,
        activeSubtitleTrackId: String?,
        tracks: List<TvTrackInfo>
    ) {
        clear()
        synchronized(lock) {
            val tv:Tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
            var selectedAudioTrackId = "a" + tv.getCurChAudioIndex(
                    TVMediaTypeConstants.TV_SOURCE_DTV).toString()

            var selectedSubtitleTrackId = "s" + tv.getDtvCurChSubtitleIndex(
                    TVMediaTypeConstants.TV_SOURCE_DTV).toString()

            val tunerType = getTunerSource()
            if(tunerType == TVMediaTypeConstants.TV_SOURCE_ATV){
                mAudioTracks.addAll(getAudioTracksForATV(tv))
                val listIndex: Int = tv.getAtvMTSAudioListIndex()
                Log.d(Constants.LogTag.CLTV_TAG + "Analogous", "updateTracks: ${listIndex}")
                if(listIndex >= 0 && listIndex < mAudioTracks.size){
                    currentAudioTrack = mAudioTracks[listIndex]
                    Log.d(Constants.LogTag.CLTV_TAG + "Analogous", "updateTracks: $listIndex ${mAudioTracks[listIndex]} $currentAudioTrack")
                }
            }

            for (track in tracks) {
                if (track.type == TvTrackInfo.TYPE_AUDIO && tunerType != TVMediaTypeConstants.TV_SOURCE_ATV) {
                    val audioTrack = AudioTrack(track, utilsInterface, false, "")
                    mAudioTracks.add(audioTrack)
                    if (activeAudioTrackId != null && track.id == activeAudioTrackId) {
                        currentAudioTrack = audioTrack
                    } else if (track.id == selectedAudioTrackId) {
                        currentAudioTrack = audioTrack
                    }
                }
                else if (track.type == TvTrackInfo.TYPE_SUBTITLE) {
                    val subtitleTrack = SubtitleTrack(track, utilsInterface)
                    if (track.encoding != "teletext-full-page") {
                        mSubtitleTracks.add(subtitleTrack)
                        if (activeSubtitleTrackId != null && track.id == activeSubtitleTrackId){
                            currentSubtitleTrack = subtitleTrack
                        } else if (track.id == selectedSubtitleTrackId) {
                            currentSubtitleTrack = subtitleTrack
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getTunerSource(): Int {
        val tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
        val tvSrc = tv.getActivatedTvSource(0)
        return tvSrc.src
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getAudioTracksForATV(tv: Tv): List<TrackBase.AudioTrack> {
        val tunerType = getTunerSource()
        val audioTracks = mutableListOf<TrackBase.AudioTrack>()
        if(tunerType == TVMediaTypeConstants.TV_SOURCE_ATV){
            val listType:Int = tv.getAtvMTSAudioListType()
            Log.d(Constants.LogTag.CLTV_TAG + "Analogous", "updateTracks: getAudioTracksForATV lt $listType")
            val atvTracksNames = (utilsInterface as UtilsInterfaceImpl).handleAnalogousAudioTracks(listType)
            atvTracksNames.forEachIndexed { index, analogName ->
                val builder = TvTrackInfo.Builder(
                    TvTrackInfo.TYPE_AUDIO,
                    index.toString()
                )
                val args = Bundle()
                args.putInt("hardHearing", 0)
                args.putInt("index", index)
                builder.setExtra(args)
                Log.d(Constants.LogTag.CLTV_TAG + "Analogous", "updateTracks: getAudioTracksForATV an: $analogName")

                audioTracks.add(TrackBase.AudioTrack(builder.build(), utilsInterface, true, analogName))
            }
            return audioTracks
        }
        return super.getAudioTracks()
    }

}