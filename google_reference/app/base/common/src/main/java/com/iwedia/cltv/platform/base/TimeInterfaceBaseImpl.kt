package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class TimeInterfaceBaseImpl: TimeInterface {

    init {
        //Time update
        try {
            Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate({
                    var currentTime = getCurrentTime()
                    if (InformationBus.isListenerInitialized()) {
                        InformationBus.informationBusEventListener.submitEvent(Events.TIME_CHANGED, arrayListOf(currentTime))
                    }
                }, 0, 1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCurrentTime(): Long {
        return System.currentTimeMillis()
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return getCurrentTime()
    }
}