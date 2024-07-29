package com.iwedia.cltv.platform.t56.mtkChannelInfo

import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvChCommonBase
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase

class MtkUtils {

    companion object{
        val TAG = "MtkUtils"
        val CH_LIST_EPG_MASK = MtkTvChCommonBase.SB_VNET_ACTIVE
        val CH_LIST_EPG_VAL = MtkTvChCommonBase.SB_VNET_ACTIVE
        val CH_LIST_EPG_US_MASK = MtkTvChCommonBase.SB_VNET_EPG or MtkTvChCommonBase.SB_VNET_FAKE
        val CH_LIST_EPG_US_VAL = MtkTvChCommonBase.SB_VNET_EPG

        /**for inactive channel */
        val CH_CONFIRM_REMOVE_MASK = MtkTvChCommonBase.SB_VNET_REMOVAL_TO_CONFIRM
        val CH_CONFIRM_REMOVE_VAL = MtkTvChCommonBase.SB_VNET_REMOVAL_TO_CONFIRM
        val CH_LIST_FAV1_MASK = MtkTvChCommonBase.SB_VNET_FAVORITE1
        val CH_LIST_FAV1_VAL = MtkTvChCommonBase.SB_VNET_FAVORITE1

        val CH_SCRAMBLED_MASK = MtkTvChCommonBase.SB_VNET_SCRAMBLED
        val CH_SCRAMBLED_VAL = MtkTvChCommonBase.SB_VNET_SCRAMBLED
        private const val mCurCategories = -1
        var current3rdMId = "current_3rd_channel_mId"
        var channelType = "TYPE_PREVIEW"
        const val sourceATVSvlId = 2
        //TODO change this
        const val inputIdStart = "com.mediatek.tvinput/."
    }

    /**
     * same with getSvl() function svlID 1:air, 2:cable, 3:general sat, 4:prefer sat, 5:CAM-air,
     * 6:CAM-cable, 7:CAM-sat
     *
     * @return
     */
    fun getSvlId(): Int {
        val svlId = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID)
        return svlId
    }

    /**
     * not start with "com.mediatek.tvinput/." is 3rd
     * @return
     */
    fun is3rdTVSource(tIFChannelInfo: MtkTIFChannelInfo?): Boolean {
        return !tIFChannelInfo!!.mInputServiceName!!.startsWith(inputIdStart)
    }


    /**
     * if current source is dtv or not
     *
     * @return
     */


}