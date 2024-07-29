package com.iwedia.cltv.platform.t56

import com.iwedia.cltv.platform.base.TimeInterfaceBaseImpl
import com.iwedia.cltv.platform.model.TvChannel
import com.mediatek.twoworlds.tv.MtkTvTime

class TimeInterfaceImpl: TimeInterfaceBaseImpl() {
    val TAG = javaClass.simpleName
    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return if ((tvChannel.inputId.lowercase().contains("iwedia"))) {
            super.getCurrentTime()
        } else
            MtkTvTime.getInstance().broadcastUtcTime.toSeconds() * 1000
    }
}