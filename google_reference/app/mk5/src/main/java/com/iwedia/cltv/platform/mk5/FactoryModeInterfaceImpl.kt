package com.iwedia.cltv.platform.mk5

import android.content.Context
import android.media.tv.TvView
import android.net.Uri
import android.util.Log
import com.iwedia.cltv.platform.base.FactoryModeInterfaceIBasempl
import com.iwedia.cltv.platform.`interface`.FactoryModeInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.mk5.mtkChannelInfo.MtkTIFChannelManager
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.twoworlds.factory.MtkTvFApiInformation
import com.mediatek.twoworlds.factory.common.inimanager.MtkTvFApiCusdataIniManager
import com.mediatek.twoworlds.tv.MtkTvBroadcast
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import mediatek.sysprop.VendorProperties


class FactoryModeInterfaceImpl(var utilsModule: UtilsInterface) : FactoryModeInterfaceIBasempl() {


    val TAG = "FactoryModeInterfaceImpl"

    override fun hasSignal(): Boolean {
        return !MtkTvBroadcast.getInstance().isSignalLoss
    }

    override fun restoreEdidVersion() {
        val boardPath: String = MtkTvFApiCusdataIniManager.getInstance()
            .getCusIniPath(MtkTvFApiCusdataIniManager.MTKTV_FAPI_KEY_TYPE_CUSDATA_ENUM.E_MTK_FAPI_KEY_TYPE_BOARD.ordinal)
        val key = "BOARD_EDID_INFO:BOARD_EDID_INFO_HDMI_COUNT"
        val port_str = arrayOf<String>(
            MtkTvConfigType.CFG_VIDEO_SET_HDMI_EDID_PORT1,
            MtkTvConfigType.CFG_VIDEO_SET_HDMI_EDID_PORT2,
            MtkTvConfigType.CFG_VIDEO_SET_HDMI_EDID_PORT3,
            MtkTvConfigType.CFG_VIDEO_SET_HDMI_EDID_PORT4
        )
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "restoreEdidVersion:$boardPath")
        val edidInfoHdmiCount: String =
            MtkTvFApiInformation.getInstance().getIniParameter(boardPath, key)
        val hdmiEdidCount = edidInfoHdmiCount.toInt()
        val edidVer = IntArray(hdmiEdidCount)
        for (port in 0 until hdmiEdidCount) {
            edidVer[port] = MtkTvConfig.getInstance().getConfigValue(port_str[port])
            MtkTvConfig.getInstance().setConfigValue(port_str[port], 1) //0:1.4 1:2.0 2:Auto
        }
    }

    override fun tuneByChannelNameOrNum(
        channelName: String,
        isName: Boolean,
        tvView: TvView?,
    ): Int {
        return MtkTIFChannelManager.getInstance().getChannelByNumOrName(
            channelName,
            isName, tvView,
            utilsModule
        )
    }

    override fun tuneByUri(uri: Uri, context: Context, tvView: TvView?): String {
        return MtkTIFChannelManager.getInstance()
            .getTifChannelInfoByUri(uri, context, tvView, utilsModule)
    }

    override fun tuneToActiveChannel(tvView: TvView?): String {
        return MtkTIFChannelManager.getInstance().tuneToActiveChannel(tvView)
    }

    override fun getChannelDisplayName():String{
        return MtkTIFChannelManager.getInstance().getDisplayName()
    }

    override fun channelUpOrDown(isUp :Boolean , liveTvView: TvView?) :String{
        return MtkTIFChannelManager.getInstance().changeUpOrDown(isUp,liveTvView,utilsModule)
    }

    override fun loadChannels(context: Context) {
        MtkTIFChannelManager.getInstance().getAllQueriedChannels(context,utilsModule)
    }

    override fun deleteInstance(){
        MtkTIFChannelManager.getInstance().deleteInstance()
    }

    override fun getVendorMtkAutoTest(): Int {
        return VendorProperties.mtk_auto_test().orElse(0)
    }

    override fun getFirstChannelAndTune(context: Context, liveTvView: TvView?) {
        MtkTIFChannelManager.getInstance().tuneFirstChannel(context, liveTvView)
    }


}