package com.iwedia.cltv.platform.rtk.eas

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.eas.EasEventInfo
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.rtk.UtilsInterfaceImpl
import com.iwedia.cltv.platform.rtk.provider.ChannelDataProvider
import com.realtek.tv.Tv
import com.realtek.tv.bean.DtvEamInfo
import com.realtek.tv.callback.EAMCallback
import java.util.ArrayList

@RequiresApi(Build.VERSION_CODES.P)
class EasControl(utilsInterface: UtilsInterface, private val channelDataProvider: ChannelDataProvider) {
    private val TAG = javaClass.simpleName
    private var easCallback: EAMCallback? = null
    private var channelId: Int = -1
    private var isTuneEnabled = false

    private val EAS_CHANNEL_TUNE = 0
    private val EAS_SHOW_TEXT = 1

    init {

        val tv = (utilsInterface as UtilsInterfaceImpl).getTvSetting()

        easCallback = object : EAMCallback() {
            override fun onStartAlertTextScrolling(mTv: Tv, eamInfo: DtvEamInfo) {
                Log.d(
                    Constants.LogTag.CLTV_TAG +
                    TAG,
                    "onStartAlertTextScrolling: eamInfo = ${eamInfo.alertText}, activation text = ${eamInfo.natureOfActivationTxt}"
                )
                sendEASNotification(EAS_SHOW_TEXT, eamInfo)
            }

            override fun onStopAlertTextScrolling(mTv: Tv) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onStopAlertTextScrolling")
                isTuneEnabled = false
                setEasChannel(-1)
                sendEASNotification(EAS_CHANNEL_TUNE, null)
            }

            @RequiresApi(Build.VERSION_CODES.P)
            override fun onTuneToDetailsChannel(mTv: Tv, uid: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTuneToDetailsChannel, uid:$uid")
                isTuneEnabled = true
                setEasChannel(uid)
                sendEASNotification(EAS_CHANNEL_TUNE, null)
            }

            @RequiresApi(Build.VERSION_CODES.P)
            override fun onReacquireOriginalChannel(mTv: Tv, uid: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReacquireOriginalChannel, uid:$uid")
                isTuneEnabled = true
                setEasChannel(uid)
                sendEASNotification(EAS_CHANNEL_TUNE, null)
            }
        }
        tv.setEAMCallback(easCallback)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun getEasChannel(): String {
        var easChannel = ""
        val channelList = channelDataProvider.getChannelList()

        channelList.forEach {
          if (it.providerFlag2 == channelId) {
                easChannel  = it.displayNumber
            }
        }
        return easChannel
    }

    fun setEasChannel(uid: Int) {
        channelId = uid
    }

    fun isTuneToDetailsChannel(): Boolean {
        return isTuneEnabled
    }

    fun removeCallback() {
        easCallback = null
    }

    private fun sendEASNotification(msgType: Int, eamInfo: DtvEamInfo?) {
        val list = ArrayList<Any>()
        list.add(msgType)
        list.add(
            EasEventInfo(
                eamInfo?.alertText,
                eamInfo?.natureOfActivationTxt,
                false
            )
        )

        InformationBus.informationBusEventListener.submitEvent(
            Events.IS_EAS_PLAYING, list
        )
    }
}