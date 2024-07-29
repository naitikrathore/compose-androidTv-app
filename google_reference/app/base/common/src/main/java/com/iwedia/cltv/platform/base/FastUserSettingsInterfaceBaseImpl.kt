package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.FastDataProviderInterface
import com.iwedia.cltv.platform.`interface`.FastUserSettingsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus

open class FastUserSettingsInterfaceBaseImpl constructor(private var fastDataProviderInterface: FastDataProviderInterface): FastUserSettingsInterface{

    open val TAG = "DoNotTrackInterfaceBaseImpl"

    override var regionSupported = true
    init {
        var eventReceiver: Any ?= null
        InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.ANOKI_REGION_NOT_SUPPORTED), {
            eventReceiver = it
        }, {
            regionSupported = false
            InformationBus.informationBusEventListener.unregisterEventListener(eventReceiver!!)
        })
    }

    override fun getDnt(callback: IAsyncDataCallback<Int>) {
        val dnt: Int = fastDataProviderInterface.getDNT()
        if (dnt != -1){
            callback.onReceive(dnt)
        }else{
            callback.onFailed(Error("DNT not found"))
        }
    }

    override fun setDnt(value: Boolean, callback: IAsyncCallback) {
        fastDataProviderInterface.updateDNT(value, callback)
    }

    override fun checkTos(callback: IAsyncDataCallback<Boolean>) {
        callback.onReceive(fastDataProviderInterface.getTosOptIn() == 1)
    }

    override fun isRegionSupported(): Boolean {
        return regionSupported
    }

    override fun deleteAllFastData(inputId: String){
        fastDataProviderInterface.deleteAllFastData(inputId)
    }

}