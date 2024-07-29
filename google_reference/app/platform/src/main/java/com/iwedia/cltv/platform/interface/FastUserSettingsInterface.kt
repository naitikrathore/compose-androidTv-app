package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback

interface FastUserSettingsInterface {
    var regionSupported: Boolean
    fun getDnt(callback: IAsyncDataCallback<Int>)
    fun setDnt(value: Boolean, callback: IAsyncCallback)
    fun checkTos(callback: IAsyncDataCallback<Boolean>)
    fun isRegionSupported(): Boolean
    fun deleteAllFastData(inputId: String)
}