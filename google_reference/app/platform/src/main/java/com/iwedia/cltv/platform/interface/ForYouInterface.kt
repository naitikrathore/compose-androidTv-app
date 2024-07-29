package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.foryou.RailItem

/**
 * For You Interface API
 *
 * @author Dejan Nadj
 */
interface ForYouInterface {
    companion object {
        var ENABLE_FAST_DATA = true
    }
    fun setup()
    fun dispose()
    fun getAvailableRailSize(): Int
    fun getForYouRails(callback: IAsyncDataCallback<ArrayList<RailItem>>)
    fun updateRailData()
    fun setPvrEnabled(pvrEnabled: Boolean)
}