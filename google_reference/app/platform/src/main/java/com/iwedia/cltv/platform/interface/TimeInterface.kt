package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.TvChannel

interface TimeInterface {

    fun getCurrentTime(): Long

    fun getCurrentTime(tvChannel: TvChannel): Long
}