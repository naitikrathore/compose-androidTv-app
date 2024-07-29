package com.iwedia.cltv.sdk

import android.content.Context

import com.iwedia.cltv.sdk.R


class DisplayMode {


    companion object {

        // The values should be synced with R.arrays.display_mode_label

        /*NORMAL: A smalled playbackscreen which is not default for ref+ app*/
        val MODE_NORMAL = 0

        /*FULL: A Fullscreen playbackscreen which is default for ref+ app*/
        val MODE_FULL = 1

        /*ZOOM: A Zoomed playbackscreen*/
        val MODE_ZOOM = 2

        val SIZE_OF_RATIO_TYPES = MODE_ZOOM + 1

        val MODE_NOT_DEFINED = -1

    }
}