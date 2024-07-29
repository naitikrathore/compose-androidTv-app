package com.iwedia.cltv.manager

import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.guide.android.tools.GAndroidActivity
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import world.WorldHandler

abstract class ReferenceSceneManager(
    context: GAndroidActivity<*, *, *, *, *, *>?,
    worldHandler: WorldHandler?,
    id: Int
) : GAndroidSceneManager(context, worldHandler, id) {

    init {
        initConfigurableKeys()
        registerGenericEventListener(Events.TIME_CHANGED)
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (event?.type == Events.TIME_CHANGED) {
            if (event.getData(0) != null && event.getData(0) is Long) {
                ReferenceApplication.runOnUiThread(Runnable {
                    onTimeChanged(event.getData(0) as Long)
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    abstract fun initConfigurableKeys()

    abstract fun onTimeChanged(currentTime: Long)
}