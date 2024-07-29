package com.iwedia.cltv

import android.content.Context
import android.database.Cursor
import com.mediatek.twoworlds.tv.MtkTvBroadcast
import com.mediatek.twoworlds.tv.model.MtkTvChannelInfoBase
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.model.MtkTvDvbChannelInfo
import android.util.Log

/**
 * System Information Data provider
 *
 * @author
 */
class SystemInfoProvider {
    class SystemInfoData {
        var id = -1
        var inputId = ""
        var displayNumber = 0L
        var displayName = ""
        var providerData = ""
        var logoImagePath = ""
        var isRadioChannel = false
        var isSkipped = false
        var isLocked = false;
        var tunerType = -1
        var ordinalNumber = 0
        var frequency = 0
        var tsId = 0
        var onId = 0
        var serviceId = 0
        var bandwidth = 0
        var networkId = 0
        var networkName = ""
        var postViterbi = 0
        var attr5s = 0
        var signalQuality = 0
        var signalStrength = 0
        var signalBer = 0
        var signalAGC = 0
        var signalUEC = 0
    }

    companion object {

        fun setContext(con: Context) {
        }

        fun getSystemInfoData(onId: Int, tsId: Int, serviceId : Int) :  SystemInfoData? {

            var retVal = SystemInfoData()

            val currentChannel : MtkTvDvbChannelInfo? = MtkTvChannelListBase.getCurrentChannel() as? MtkTvDvbChannelInfo

            if(currentChannel != null) {

                /* Display Number */
                retVal.displayNumber = currentChannel.channelNumber.toLong()

                /* Signal Quality */
                retVal.signalQuality = MtkTvBroadcast.getInstance().signalQuality

                /* Signal Strength */
                retVal.signalStrength = MtkTvBroadcast.getInstance().signalLevel

                /* Network ID */
                retVal.networkId = currentChannel.nwId

                /* Network Name */
                retVal.networkName = currentChannel.nwName

                /* Post Viterbi */
                retVal.postViterbi = currentChannel.symRate

                /* 5s */
                retVal.attr5s = currentChannel.mod

                /* Service ID */
                retVal.serviceId = currentChannel.svlRecId

                /* TS ID */
                retVal.tsId = currentChannel.tsId

                /* ON ID */
                retVal.onId = currentChannel.onId

                /* Frequency */
                retVal.frequency = currentChannel.frequency

                /* Bandwidth */
                when(currentChannel.bandWidth) {
                    0 -> {
                        retVal.bandwidth = 0
                    }
                    1 -> {
                        retVal.bandwidth = 6
                    }
                    2 -> {
                        retVal.bandwidth = 7
                    }
                    3 -> {
                        retVal.bandwidth = 8
                    }
                    4 -> {
                        retVal.bandwidth = 5
                    }
                    5 -> {
                        retVal.bandwidth = 10
                    }
                    6 -> {
                        retVal.bandwidth = 0
                    }
                }

                /* Ber */
                retVal.signalBer = MtkTvBroadcast.getInstance().getConnectAttr("main", MtkTvBroadcast.BRDCST_CM_CTRL_GET_BER)

                /* AGC */
                retVal.signalAGC =  MtkTvBroadcast.getInstance().getConnectAttr("main", MtkTvBroadcast.BRDCST_CM_CTRL_GET_AGC)

                /* UEC */
                retVal.signalUEC = MtkTvBroadcast.getInstance().getConnectAttr("main", MtkTvBroadcast.BRDCST_CM_CTRL_GET_UEC)
            } else {
                return null
            }

            return retVal
        }
    }
}