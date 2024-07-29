package com.iwedia.cltv.platform.base

import android.content.Context
import android.media.AudioManager
import com.iwedia.cltv.platform.`interface`.ClosedCaptionInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface

open class ClosedCaptionInterfaceBaseImpl(context: Context, var utilsInterface: UtilsInterface, var playerInterface: PlayerInterface) : ClosedCaptionInterface {
    override fun getDefaultCCValues(ccOptions: String): Int? {
        return 0
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int, isOtherImput: Boolean) {
    }

    override fun resetCC() {}

    override fun setCCWithMuteInfo() {

    }
    override fun setCCInfo() {
    }

    override fun disableCCInfo() {
    }

    override fun setCCWithMute(isEnable: Boolean, audioManager: AudioManager) {
        utilsInterface.setPrefsValue("WITH_MUTE_CHECKED", isEnable)
    }

    override fun isClosedCaptionEnabled(): Boolean {
        return false
    }

    override fun setClosedCaption(isOtherImput: Boolean): Int {
        return 0
    }

    override fun getClosedCaption(isOtherImput: Boolean): String? {
        return ""
    }

    override fun getSubtitlesState(): Boolean {
        return true
    }

    override fun getDefaultMuteValues(): Boolean {
        val retVal = utilsInterface.getPrefsValue("WITH_MUTE_CHECKED", false)
        if(retVal is Boolean){
            return retVal
        }
        return false
    }

    override fun isCCTrackAvailable(): Boolean {
        return false
    }

    override fun initializeClosedCaption() {}

    override fun disposeClosedCaption() {}

    override fun applyClosedCaptionStyle() {}
    override fun updateSelectedTrack() {}
}