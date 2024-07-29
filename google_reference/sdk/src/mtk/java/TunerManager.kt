package com.iwedia.cltv

import android.util.Log
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.cltv.sdk.handlers.ReferenceTvHandler
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase
import core_entities.Error
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus

class TunerManager {

    var currentChannel: ReferenceTvChannel? = null
    var mTvCfg: MtkTvConfig? = null

    init {
        mTvCfg = MtkTvConfig.getInstance()
    }

    fun setTunerMode(i: Int){
        val tuneMode = MtkTvConfig.getInstance()
            .getConfigValue(MtkTvConfigType.CFG_BS_BS_SRC)

        currentChannel = ReferenceSdk.tvHandler!!.getChannelByIndex(i)

        when (currentChannel?.tunerType) {
            ReferenceTvChannel.TERRESTRIAL_TUNER_TYPE -> {
                if(tuneMode != MtkTvConfigType.BS_SRC_AIR) {
                    mTvCfg!!.setConfigValue(
                        MtkTvConfigType.CFG_BS_BS_SRC,
                        MtkTvConfigTypeBase.BS_SRC_AIR,
                        MtkTvConfigTypeBase.CFGF_SET_VALUE
                    )
                }
            }
            ReferenceTvChannel.CABLE_TUNER_TYPE -> {
                if(tuneMode != MtkTvConfigType.BS_SRC_CABLE) {
                    mTvCfg!!.setConfigValue(
                        MtkTvConfigType.CFG_BS_BS_SRC,
                        MtkTvConfigTypeBase.BS_SRC_CABLE,
                        MtkTvConfigTypeBase.CFGF_SET_VALUE
                    )
                }
            }
            ReferenceTvChannel.SATELLITE_TUNER_TYPE -> {
                if(tuneMode != MtkTvConfigType.ACFG_BS_SRC_SAT) {
                    mTvCfg!!.setConfigValue(
                        MtkTvConfigType.CFG_BS_BS_SRC,
                        MtkTvConfigTypeBase.ACFG_BS_SRC_SAT,
                        MtkTvConfigTypeBase.CFGF_SET_VALUE
                    )
                }
            }

            else -> mTvCfg!!.setConfigValue(
                MtkTvConfigType.CFG_BS_BS_SRC,
                MtkTvConfigTypeBase.BS_SRC_AIR,
                MtkTvConfigTypeBase.CFGF_SET_VALUE
            )
        }
    }
}