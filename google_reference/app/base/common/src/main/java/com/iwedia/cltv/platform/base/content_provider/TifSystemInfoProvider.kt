package com.iwedia.cltv.platform.base.content_provider

import com.iwedia.cltv.platform.`interface`.SystemInfoProviderInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel

open class TifSystemInfoProvider : SystemInfoProviderInterface {
    override fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>) {
        callback.onReceive(SystemInfoData())
    }
}