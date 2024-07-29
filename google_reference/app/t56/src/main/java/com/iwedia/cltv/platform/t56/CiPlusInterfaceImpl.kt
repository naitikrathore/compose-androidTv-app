package com.iwedia.cltv.platform.t56

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.base.CiPlusInterfaceBaseImpl
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.twoworlds.tv.MtkTvCI
import com.mediatek.twoworlds.tv.MtkTvCIBase
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIEnqBase
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIMenuBase

class CiPlusInterfaceImpl(var context: Context): CiPlusInterfaceBaseImpl() {
    var ciHandler: CiHandler? = null
    var listeners = mutableListOf<CiPlusInterface.RefCiHandlerListener>()
    private var menuList = mutableListOf<String>()
    val TAG = javaClass.simpleName

    enum class CIPinCapsType {
        CI_PIN_CAPS_NONE, CI_PIN_CAPS_CAS_ONLY, CI_PIN_CAPS_CAS_AND_FTA, CI_PIN_CAPS_CAS_ONLY_CACHED, CI_PIN_CAPS_CAS_AND_FTA_CACHED
    }

    init {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "init: ######### CI + FROM MTK")
        //Setup CI handler
        val ciHandler = CiHandler(context)
        this.ciHandler = ciHandler

        ciHandler.setListener(object : CiStateChangedCallback.CIMenuUpdateListener {
            override fun showCiPopup() {
                listeners.forEach { listener ->
                    listener!!.showCiPopup()
                }
            }

            override fun closeCiPopup() {
                listeners.forEach { listener ->
                    listener!!.closePopup()
                }
            }


            override fun enqReceived(enquiry: MtkTvCIMMIEnqBase?) {
                listeners.forEach{listener ->

                    enquiry?.text?.let {
                        var lEnquiry = CiPlusInterface.Enquiry()
                        lEnquiry.inputText = it
                        listener.onEnquiryReceived(lEnquiry)
                    }
                }

                // send msg to escape may dismiss the ci info dialog
                ciHandler.mHandler.sendEmptyMessage(0XF2)
                ciHandler.mHandler.removeMessages(0xF4)
            }

            override fun menuReceived(menu: MtkTvCIMMIMenuBase?) {
                Log.d(Constants.LogTag.CLTV_TAG + "ZOLA", "ON MENU RECEIVED")
                var list = mutableListOf<String>()

                if (menu != null) {
                    if (menu.itemList == null) {
                        list.add("back")
                    }

                    for ((_, s) in menu.itemList.withIndex()) {
                        if (!TextUtils.isEmpty(s)) {
                            list.add(s)
                        }
                    }

                    var camMenu = CiPlusInterface.CamMenu()
                    camMenu.id = menu.mmiId
                    camMenu.title = menu.title
                    camMenu.subTitle = menu.subtitle
                    camMenu.bottom = menu.bottom
                    camMenu.menuItems = list

                    listeners.forEach{listener ->
                        listener.onMenuReceived(camMenu)
                    }
                }
            }

            override fun menuEnqClosed() {
                Log.d(Constants.LogTag.CLTV_TAG + "ZOLA", "MENU ENQ CLOSED")
            }

            override fun ciRemoved() {

            }

            override fun ciCamScan(message: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + "ZOLA", "CI CAM SCAN")
            }
        })

    }

    override fun dispose() {
        ciHandler = null
        listeners.clear()
    }

    override fun registerListener(listener: CiPlusInterface.RefCiHandlerListener) {
        listeners.add(listener)
    }

    override fun selectMenuItem(position: Int) {
        ciHandler!!.getmCIState()!!.selectMenuItem(position)
    }

    override fun enquiryAnswer(abort : Boolean , answer: String) {
        var goodAnswer : Int = if(abort) 0 else 1
        ciHandler!!.getmCIState()!!.answerEnquiry(goodAnswer, answer)
    }

    override fun isCamPinEnabled() : Boolean {
        var camPinCaps = MtkTvCIBase.getCamPinCaps();
        if((camPinCaps == CIPinCapsType.CI_PIN_CAPS_CAS_ONLY_CACHED.ordinal) ||
            (camPinCaps == CIPinCapsType.CI_PIN_CAPS_CAS_AND_FTA_CACHED.ordinal)) {
            return true
        }
        return false
    }

    override fun setCamPin(pin : String) {
        println("setCamPin $pin")
        MtkTvCI.getInstance(0).camPinCode = pin
    }

    override fun getMenu(): MutableList<String> {
        val menu = ciHandler!!.getmCIState()!!.getMenu()
        if (menu != null) {
            if (menu.itemList == null) {
                menuList.add("back")
            }

            for ((_, s) in menu.itemList.withIndex()) {
                if (!TextUtils.isEmpty(s)) {
                    menuList.add(s)
                }
            }

            var camMenu = CiPlusInterface.CamMenu()
            camMenu.id = menu.mmiId
            camMenu.title = menu.title
            camMenu.subTitle = menu.subtitle
            camMenu.bottom = menu.bottom
            camMenu.menuItems = menuList

            listeners.forEach{listener ->
                listener?.let {
                    it.onMenuReceived(camMenu)
                }}
        }
        return menuList
    }

    override fun isCamActive(): Boolean {
        //TODO CHECK THIS IF THIS IS OK?
        return ciHandler!!.getmCIState()!!.getCIName() != null && ciHandler!!.getmCIState()!!.getCIName() != ""
        //return ciHandler!!.getmCIState().isCamActive
    }

    override fun getCiName(): String {
        val ciName = CiStateChangedCallback(context).getCIName() ?: ""
        return ciName
    }

    override fun getMenuListID(): Int {
        return MtkTvCIBase.getMenuListID()
    }

    override fun getEnqId(): Int {
        return MtkTvCIBase.getEnqID()
    }

    override fun setMMICloseDone() {
        ciHandler!!.getmCIState()!!.getCIHandle()!!.setMMICloseDone()
    }

    override fun enterMMI() {
        //Because this is MStar SDK you need to make call to every random function
        //for all features to work properly, without this CONAX MMI does not work, why no one knows
        val msgType = MtkTvCI.getInstance(0).camScanReqTypeACON
        var caps: Int = MtkTvCI.getCamPinCaps()
        var active: Boolean = ciHandler!!.getmCIState()!!.getCIHandle()!!.slotActive
        Log.d(Constants.LogTag.CLTV_TAG + "ZOLA", "CAPS $caps Active $active MsgType $msgType")

        //In every decent SDK this call would be enough
        ciHandler!!.getmCIState()!!.getCIHandle()!!.enterMMI()
    }

    override fun cancelCurrMenu() {
        ciHandler!!.getmCIState()!!.cancelCurrMenu()
    }

    override fun isChannelScrambled(): Boolean {
        if (MtkTvChannelListBase.getCurrentChannel() == null) {
            return false
        }
        return MtkTvChannelListBase.getCurrentChannel().isScrambled
    }

    override fun isContentClear(): Boolean {
        return ciHandler!!.isClear
    }

}