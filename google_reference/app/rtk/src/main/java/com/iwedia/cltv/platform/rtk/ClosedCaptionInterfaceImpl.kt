package com.iwedia.cltv.platform.rtk

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.realtek.system.RtkConfigs
import com.realtek.tv.closedcaption.CustomCaptionManager

class ClosedCaptionInterfaceImpl(private val context: Context, private val utilsModule: UtilsInterface, playerInterface: PlayerInterface) : com.iwedia.cltv.platform.base.ClosedCaptionInterfaceBaseImpl(context, utilsModule, playerInterface) {
    private val TAG = javaClass.simpleName
    private var captionManager: CustomCaptionManager? = null

    var currentCCIndex = 0

    val ENABLED_STATE_OFF = 0
    val ENABLED_STATE_ON = 1
    val ENABLED_STATE_ON_WHEN_MUTING = 2

    val ANALOG_SELECTION_NONE = 0
    val ANALOG_SELECTION_CC1 = 1
    val ANALOG_SELECTION_CC2 = 2
    val ANALOG_SELECTION_CC3 = 3
    val ANALOG_SELECTION_CC4 = 4
    val ANALOG_SELECTION_T1 = 5
    val ANALOG_SELECTION_T2 = 6
    val ANALOG_SELECTION_T3 = 7
    val ANALOG_SELECTION_T4 = 8

    val DIGITAL_SELECTION_SERVICE1 = 1
    val DIGITAL_SELECTION_SERVICE2 = 2
    val DIGITAL_SELECTION_SERVICE3 = 3
    val DIGITAL_SELECTION_SERVICE4 = 4
    val DIGITAL_SELECTION_SERVICE5 = 5
    val DIGITAL_SELECTION_SERVICE6 = 6

    val ANALOG_CC_COUNT = 8
    val DIGITAL_CC_COUNT = 6

    val ANALOG_CC_START_IDX = 0
    val ANALOG_CC_END_IDX = 7
    val DIGITAL_CC_START_IDX = 8
    val DIGITAL_CC_END_IDX = 13

    val UPDATE_ANALOG_CC_IDX = 0
    val UPDATE_DIGITAL_CC_IDX = 1

    val ccTrackList = arrayListOf(
        "CC_1", "CC_2", "CC_3", "CC_4", "T-1", "T-2", "T-3", "T-4", "CS-1", "CS-2", "CS-3", "CS-4", "CS-5", "CS-6"
    )

    init {
        captionManager = CustomCaptionManager(context)
        val activeCC = captionManager?.activeCC!!
        if (activeCC > 0) currentCCIndex = activeCC-1
    }

    override fun getDefaultCCValues(ccOptions: String): Int? {
        var returnValue = -1

        when (ccOptions) {
            "display_cc" -> {
                when(captionManager?.enabledState!!) {
                    ENABLED_STATE_OFF ->  { returnValue = 0 }
                    ENABLED_STATE_ON,
                    ENABLED_STATE_ON_WHEN_MUTING -> { returnValue = 1 }
                }
            }

            "caption_services" -> {
                when(captionManager?.analogSelection!!) {
                    ANALOG_SELECTION_CC1 -> returnValue = 0
                    ANALOG_SELECTION_CC2 -> returnValue = 1
                    ANALOG_SELECTION_CC3 -> returnValue = 2
                    ANALOG_SELECTION_CC4 -> returnValue = 3
                    ANALOG_SELECTION_T1 -> returnValue = 4
                    ANALOG_SELECTION_T2 -> returnValue = 5
                    ANALOG_SELECTION_T3 -> returnValue = 6
                    ANALOG_SELECTION_T4 -> returnValue = 7
                }
            }

            "advanced_selection" -> {
                when(captionManager?.digitalSelection!!) {
                    DIGITAL_SELECTION_SERVICE1 -> returnValue = 0
                    DIGITAL_SELECTION_SERVICE2 -> returnValue = 1
                    DIGITAL_SELECTION_SERVICE3 -> returnValue = 2
                    DIGITAL_SELECTION_SERVICE4 -> returnValue = 3
                    DIGITAL_SELECTION_SERVICE5 -> returnValue = 4
                    DIGITAL_SELECTION_SERVICE6 -> returnValue = 5
                }
            }

            "text_size" -> returnValue = captionManager?.textSizeIndex!!

            "font_family" -> returnValue = captionManager?.fontFamilyIndex!!

            "text_color" -> returnValue = captionManager?.textColorIndex!!

            "text_opacity" -> returnValue = captionManager?.textOpacityIndex!!

            "edge_type" -> returnValue = captionManager?.edgeTypeIndex!!

            "edge_color" -> returnValue = captionManager?.edgeColorIndex!!

            "background_color" -> returnValue = captionManager?.backgroundColorIndex!!

            "background_opacity" ->returnValue = captionManager?.backgroundOpacityIndex!!

            else -> return 0

        }
        return returnValue
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int, isOtherImput: Boolean) {
        var value = 0

        when (ccOptions) {
            "display_cc" -> {
                value = when (newValue) {
                    0 -> ENABLED_STATE_OFF
                    1 -> ENABLED_STATE_ON
                    2 -> ENABLED_STATE_ON_WHEN_MUTING
                    else -> return
                }
                captionManager?.enabledState = value
            }

            "caption_services" -> {
                when(newValue) {
                    0 -> value = ANALOG_SELECTION_CC1
                    1 -> value = ANALOG_SELECTION_CC2
                    2 -> value = ANALOG_SELECTION_CC3
                    3 -> value = ANALOG_SELECTION_CC4
                    4 -> value = ANALOG_SELECTION_T1
                    5 -> value = ANALOG_SELECTION_T2
                    6 -> value = ANALOG_SELECTION_T3
                    7 -> value = ANALOG_SELECTION_T4
                }
                captionManager?.analogSelection = value
                updateCurrentCCIndex(UPDATE_ANALOG_CC_IDX)
            }

            "advanced_selection" -> {
                when(newValue) {
                    0 -> value = DIGITAL_SELECTION_SERVICE1
                    1 -> value = DIGITAL_SELECTION_SERVICE2
                    2 -> value = DIGITAL_SELECTION_SERVICE3
                    3 -> value = DIGITAL_SELECTION_SERVICE4
                    4 -> value = DIGITAL_SELECTION_SERVICE5
                    5 -> value = DIGITAL_SELECTION_SERVICE6
                }
                captionManager?.digitalSelection  = value
                updateCurrentCCIndex(UPDATE_DIGITAL_CC_IDX)
            }

            "text_size" -> captionManager?.textSizeIndex = newValue

            "font_family" -> captionManager?.fontFamilyIndex = newValue

            "text_color" -> captionManager?.textColorIndex = newValue

            "text_opacity" -> captionManager?.textOpacityIndex = newValue

            "edge_type" -> captionManager?.edgeTypeIndex = newValue

            "edge_color" -> captionManager?.edgeColorIndex = newValue

            "background_color" -> captionManager?.backgroundColorIndex = newValue

            "background_opacity" -> captionManager?.backgroundOpacityIndex = newValue
        }
    }

    override fun resetCC() {
    }

    override fun setCCInfo() {
    }

    override fun disableCCInfo() {
    }

    override fun setCCWithMute(isEnable: Boolean, audioManager: AudioManager) {
        if (isEnable) {
            saveUserSelectedCCOptions("display_cc", 2)
        } else {
            saveUserSelectedCCOptions("display_cc", 1)
        }
    }

    override fun isClosedCaptionEnabled(): Boolean {
        return (RtkConfigs.TvConfigs.SUPPORTED_ATSC_CLOSEDCAPTION && getDefaultCCValues("display_cc")!! > 0)
    }

    override fun setClosedCaption(isOtherImput: Boolean): Int {
        val currentTrack = getClosedCaption(false)

        for (track in ccTrackList) {
            if (track == currentTrack) {
                currentCCIndex++
            }
        }

        if (currentCCIndex >= ccTrackList.size) {
            currentCCIndex = 0
        }

        if (ccTrackList[currentCCIndex].contains("cc", ignoreCase = true)
            || ccTrackList[currentCCIndex].contains("t", ignoreCase = true)) {
                saveUserSelectedCCOptions("caption_services", currentCCIndex, false)
        }
        else if (ccTrackList[currentCCIndex].contains("cs", ignoreCase = true)) {
            saveUserSelectedCCOptions("advanced_selection", (currentCCIndex-ANALOG_CC_COUNT), false)
        }
        return currentCCIndex
    }

    override fun getClosedCaption(isOtherImput: Boolean): String? {
        return ccTrackList[currentCCIndex]
    }

    override fun getDefaultMuteValues(): Boolean {
        if (captionManager?.enabledState == ENABLED_STATE_ON_WHEN_MUTING) return true
        return false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getSubtitlesState(): Boolean {
        return !RtkConfigs.TvConfigs.SUPPORTED_ATSC_CLOSEDCAPTION
    }

    override fun isCCTrackAvailable(): Boolean {
        return isClosedCaptionEnabled()
    }

    private fun updateCurrentCCIndex(updateType: Int) {
        when (updateType) {
            UPDATE_ANALOG_CC_IDX -> { currentCCIndex = getDefaultCCValues("caption_services")!! }
            UPDATE_DIGITAL_CC_IDX -> {
                currentCCIndex = ANALOG_CC_COUNT + getDefaultCCValues("advanced_selection")!!
            }
        }
    }
}