package com.iwedia.cltv.components

import android.content.Context
import android.view.KeyEvent
import android.view.View
import com.iwedia.cltv.platform.model.information_bus.events.Events
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus
import java.util.*

abstract class OverlayFeatureView {

    object Status {
        const val VISIBLE = 0 //Active and visible
        const val HIDDEN = 1 //Active and hidden
        const val INACTIVE = 2 //Inactive
    }

    var view: View? = null
    var status: Int = Status.INACTIVE

    constructor(context: Context) {
        view = createView(context)
        setup(context)
        InformationBus.registerEventListener(OverlayFeatureViewEventListener())
    }

    abstract fun setup(context: Context)

    abstract fun createView(context: Context): View

    abstract fun handleEvent(event: Event?)

    abstract fun dispatchKey(keyCode: Int, keyEvent: KeyEvent?): Boolean

    inner class OverlayFeatureViewEventListener : EventListener {
        constructor() {
            addType(Events.OVERLAY_FEATURE_VIEW_INIT)
            addType(Events.OVERLAY_FEATURE_VIEW_SHOW)
            addType(Events.OVERLAY_FEATURE_VIEW_HIDE)
            addType(Events.OVERLAY_FEATURE_VIEW_LOAD)
        }

        override fun callback(event: Event?) {
            handleEvent(event)
        }
    }

}