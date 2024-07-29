package com.iwedia.cltv.platform.mal_service

import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.FastUserSettingsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback

class FastUserSettingsInterfaceImpl(private val serviceImpl: IServiceAPI) :
    FastUserSettingsInterface {
    override var regionSupported: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun getDnt(callback: IAsyncDataCallback<Int>) {
        callback.onReceive(serviceImpl.getDnt())
    }

    override fun setDnt(value: Boolean, callback: IAsyncCallback) {
        serviceImpl.setDnt(value)
        callback.onSuccess()
    }

    override fun checkTos(callback: IAsyncDataCallback<Boolean>) {
        callback.onReceive(serviceImpl.checkTos())
    }

    override fun isRegionSupported(): Boolean {
        return serviceImpl.isRegionSupported
    }

    override fun deleteAllFastData(inputId: String) {
        serviceImpl.deleteAllFastData(inputId)
    }

}