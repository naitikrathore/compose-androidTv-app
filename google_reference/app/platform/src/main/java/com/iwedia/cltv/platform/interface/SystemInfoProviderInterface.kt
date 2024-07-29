package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel

/**
 * Channel data provider interface
 *
 * @author Rishi Raj
 */
interface SystemInfoProviderInterface {
    fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>)
}