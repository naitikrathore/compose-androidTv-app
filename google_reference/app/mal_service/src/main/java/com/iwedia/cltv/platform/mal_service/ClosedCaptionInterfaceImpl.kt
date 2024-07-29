package com.iwedia.cltv.platform.mal_service

import android.media.AudioManager
import android.util.Log
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.ClosedCaptionInterface
import com.iwedia.cltv.platform.mal_service.player.PlayerInterfaceImpl
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.player.track.ISubtitle

internal class ClosedCaptionInterfaceImpl(private val serviceImpl: IServiceAPI,
                                 private val playerInterface: PlayerInterfaceImpl
) : ClosedCaptionInterface{
    private var selectedCCTrack: String? = null

    override fun getDefaultCCValues(ccOptions: String): Int? {
        return serviceImpl.getDefaultCCValues(ccOptions)
    }

    override fun saveUserSelectedCCOptions(
        ccOptions: String,
        newValue: Int,
        isOtherImput: Boolean
    ) {
        serviceImpl.saveUserSelectedCCOptions(
            ccOptions,
            newValue,
            isOtherImput
        )
    }

    override fun resetCC() {
        serviceImpl.resetCC()
    }

    override fun setCCInfo() {
        serviceImpl.setCCInfo()
    }

    override fun disableCCInfo() {
        playerInterface.setCaptionEnabled(false)
    }

    override fun setCCWithMute(isEnable: Boolean, audioManager: AudioManager) {
        serviceImpl.setCCWithMute(isEnable)
    }

    override fun setCCWithMuteInfo() {
        serviceImpl.setCCWithMuteInfo()
    }

    override fun isClosedCaptionEnabled() =
        serviceImpl.isClosedCaptionEnabled()

    override fun setClosedCaption(isOtherImput: Boolean): Int {
        val tracks = playerInterface.getCCSubtitleTracks()
        val currentTrack = playerInterface.getActiveSubtitle()

        if (tracks.isEmpty()) {
            return 0
        }

        val nextTrack = getNextCCTrack(currentTrack, tracks.toList())
        selectedCCTrack = nextTrack?.languageName ?: ""

        playerInterface.selectSubtitle(nextTrack)

        return 1
    }

    override fun getClosedCaption(isOtherImput: Boolean) : String?{
        var ccTrack = ""
        val tracks = playerInterface.getCCSubtitleTracks()
        val currentTrack = (playerInterface ).getActiveSubtitle()
                if (tracks.isEmpty())
                    return ccTrack

                if (selectedCCTrack != null)
                    return selectedCCTrack

                for (i in tracks.indices) {
                    if (tracks[i].trackId == currentTrack?.trackId) {
                        ccTrack = tracks[i].languageName.ifEmpty { "" }
                    }
                }
        return ccTrack
    }

    override fun getSubtitlesState() : Boolean{
        return serviceImpl.getSubtitlesStateClosedCaption()
    }

    override fun getDefaultMuteValues() = serviceImpl.getDefaultMuteValues()

    override fun isCCTrackAvailable():Boolean{
         val tracks = playerInterface.getCCSubtitleTracks()
        return tracks.isNotEmpty()
    }

    override fun initializeClosedCaption() {
        //serviceImpl.initializeClosedCaption()
    }

    override fun disposeClosedCaption() {
        serviceImpl.disposeClosedCaption()
    }

    override fun applyClosedCaptionStyle() {
        serviceImpl.applyClosedCaptionStyle()
    }

    override fun updateSelectedTrack() {
        selectedCCTrack = playerInterface.getActiveSubtitle()?.languageName?: ""
    }
}

private fun getNextCCTrack(
    currentTrack: ISubtitle?,
    currentTrackList: List<ISubtitle>
): ISubtitle? {
    if (currentTrack?.trackId == null) {
        return currentTrackList[0]
    }

    var nextTrack: ISubtitle? = null
    for (i in currentTrackList.indices) {
        if (currentTrack.trackId == currentTrackList[i].trackId) {
            nextTrack =
                if (i + 1 < currentTrackList.size) {
                    currentTrackList[i + 1]
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + "ClosedCaption", "next cc track is null")
                    null
                }
            break
        }
    }
    return nextTrack
}