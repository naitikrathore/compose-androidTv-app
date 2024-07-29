package com.iwedia.cltv.platform.gretzky

import android.content.Context
import android.media.AudioManager
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.base.ClosedCaptionInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.UtilsInterface

class ClosedCaptionInterfaceImpl(context: Context, utilsModule: UtilsInterface, playerInterface: PlayerInterface) : com.iwedia.cltv.platform.base.ClosedCaptionInterfaceBaseImpl(context, utilsModule, playerInterface) {

    override fun getDefaultCCValues(ccOptions: String): Int {
        return 0
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int, isOtherImput: Boolean) {
    }

    override fun resetCC() {
    }

    override fun setCCInfo() {
    }

    override fun disableCCInfo() {
    }

    override fun setCCWithMute(isEnable: Boolean, audioManager: AudioManager) {
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
        return false
    }
}