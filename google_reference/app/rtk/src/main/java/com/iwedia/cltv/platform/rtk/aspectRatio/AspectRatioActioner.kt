package com.iwedia.cltv.platform.rtk.aspectRatio

import android.util.Log
import com.iwedia.cltv.platform.model.Constants
import com.realtek.tv.TVMediaTypeConstants
import com.realtek.tv.VSC

class AspectRatioActioner {
    private val TAG = javaClass.simpleName
    private var mVsc: VSC? = null

    companion object {
        val RATIO_16_9 = 0
        val RATIO_4_3 = 1
        val RATIO_AUTO = 2
        val RATIO_ORIGINAL = 3
    }

    init {
        mVsc = VSC()
    }

    fun getAspectRatio(): Int {
        var index = RATIO_16_9

        when (mVsc?.getWideMode(TVMediaTypeConstants.TV_SOURCE_PATH_MAIN)) {
            TVMediaTypeConstants.TV_WIDE_MODE_FULL -> {
                index =
                    if (mVsc?.getAutoWide(getDefaultWId()) == true) {
                        RATIO_AUTO
                    } else {
                        RATIO_16_9
                    }
            }
            TVMediaTypeConstants.TV_WIDE_MODE_4_3 -> index = RATIO_4_3
            TVMediaTypeConstants.TV_WIDE_MODE_NATIVE -> index = RATIO_ORIGINAL
            else -> Log.d(
                Constants.LogTag.CLTV_TAG + TAG,
                "Error! Failed to get aspect ratio id: " + mVsc?.getWideMode(
                    TVMediaTypeConstants.TV_SOURCE_PATH_MAIN
                )
            )
        }
        return index
    }

    fun setAspectRatio(value: Int): Boolean {
            when (value) {
                RATIO_16_9 -> {
                    mVsc?.setWideMode(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        TVMediaTypeConstants.TV_WIDE_MODE_FULL
                    )
                    mVsc?.setAutoWide(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        false
                    )
                }
                RATIO_4_3 -> {
                   mVsc?.setWideMode(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        TVMediaTypeConstants.TV_WIDE_MODE_4_3
                    )
                    mVsc?.setAutoWide(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        false
                    )
                }
                RATIO_ORIGINAL -> {
                    mVsc?.setWideMode(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        TVMediaTypeConstants.TV_WIDE_MODE_NATIVE
                    )
                    mVsc?.setAutoWide(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        false
                    )
                }
                RATIO_AUTO -> {
                    mVsc?.setWideMode(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        TVMediaTypeConstants.TV_WIDE_MODE_FULL
                    )
                    mVsc?.setAutoWide(
                        TVMediaTypeConstants.TV_SOURCE_PATH_MAIN,
                        true
                    )
                }
                else -> Log.d(Constants.LogTag.CLTV_TAG + TAG, "Error! Failed to set aspect ratio id: $value")
            }
        return true
    }

    private fun getDefaultWId(): Int {
        return TVMediaTypeConstants.TV_SOURCE_PATH_MAIN
    }
}