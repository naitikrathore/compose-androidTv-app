package com.iwedia.cltv.platform.rtk

import com.iwedia.cltv.platform.base.TimeInterfaceBaseImpl
import com.iwedia.cltv.platform.model.TvChannel

class TimeInterfaceImpl: TimeInterfaceBaseImpl() {
    private val TAG = javaClass.simpleName
    private var lastKnownStreamTime = 0L
    private var streamTimeOffset = 0L
    private var lastKnownStreamTimeOffset = 0
    private var lastKnowStreamTimeChangeTime = 0L
    private var lastKnowStreamTimeNextOffset = 0

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return if ((tvChannel.inputId.lowercase().contains("iwedia"))) {
            super.getCurrentTime()
        } else {
            System.currentTimeMillis() - streamTimeOffset
        }
    }

    fun setStreamTime(time: Long) {
        lastKnownStreamTime = time
        streamTimeOffset = System.currentTimeMillis() - time
        if(streamTimeOffset < 0) {
            streamTimeOffset = 0
        }
    }

    fun setStreamTimeOffset(offset: Int, changeTime : Long, nextOffset : Int) {
        lastKnownStreamTimeOffset = offset
        lastKnowStreamTimeChangeTime = changeTime
        lastKnowStreamTimeNextOffset = nextOffset
    }
}