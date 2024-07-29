package com.iwedia.cltv

import android.media.tv.TvView

object InputSourceHelper {

    var isInputSourceSelected = false
    const val ANTENNA_INPUT_TYPE =
        "content://android.media.tv/passthrough/com.mediatek.tvinput%2F.tuner.TunerInputService%2FHW0-20001"

    fun isExternalInputSource(intentData: String): Boolean {
        return false
    }

    fun setTvView(tvView: TvView) {
        return
    }


    fun handleHdmiInputSource(intentData: String) {
      return

    }

    private fun changeSource() {
       return
    }


}
