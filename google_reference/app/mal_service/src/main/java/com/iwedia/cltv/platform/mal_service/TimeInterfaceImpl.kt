package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.model.TvChannel

class TimeInterfaceImpl(private val serviceImpl: IServiceAPI) : TimeInterface {
    override fun getCurrentTime(): Long {
        return serviceImpl.currentTime
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return serviceImpl.getCurrentTimeByChannel(toServiceChannel(tvChannel))
    }
}