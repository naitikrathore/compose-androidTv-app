package com.iwedia.cltv.platform.model.information_bus.events

/**
 * InformationBus object class
 * Platform module mechanism for GuiDE library information bus event communication between interfaces and with application module
 *
 * @author Dejan Nadj
 */
object InformationBus {
    lateinit var informationBusEventListener: InformationBusEventListener
    lateinit var serviceConnectionCallback: ()->Unit
    fun isListenerInitialized(): Boolean = ::informationBusEventListener.isInitialized
    interface InformationBusEventListener {
        fun submitEvent(eventId: Int, data: ArrayList<Any> = arrayListOf())
        fun registerEventListener(eventIds: ArrayList<Int>, callback: (eventListener: Any)-> Unit, onEventReceived: (eventId: Int)->Unit)
        fun unregisterEventListener(eventListener: Any)
    }
}
