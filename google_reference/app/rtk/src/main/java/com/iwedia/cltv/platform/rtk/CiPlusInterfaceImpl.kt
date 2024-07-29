package com.iwedia.cltv.platform.rtk

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.os.Parcel
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.CiPlusInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.realtek.tv.Tv
import com.realtek.tv.callback.CICallback


@RequiresApi(Build.VERSION_CODES.P)
open class CiPlusInterfaceImpl(var context : Context, val utilsInterfaceImpl: UtilsInterface, val playerInterface: PlayerInterface, val tvInterface: TvInterface): CiPlusInterfaceBaseImpl() {

    var listeners = mutableListOf<CiPlusInterface.RefCiHandlerListener>()
    private val ciCallback = MyCICallback()
    private var powerManager: PowerManager? = null

    @RequiresApi(Build.VERSION_CODES.P)
    private var tv: Tv? = null

    private var listSize = 0
    private var selectedSlot = 0
    private var mmiMenuId = -1
    private var mmiLevel = 0
    private var camTypePreferenceIndex = -1
    private var mmiEnqId = -1
    private var isCamActive = false

    private val CI_MENUITEM_TITLE = 1
    private val CI_MENUITEM_SUBTITLE = 2
    private val CI_MENUITEM_ITEM = 3
    private val CI_MENUITEM_BOTTOM = 4

    init {
        if(tv == null) {
            tv = (utilsInterfaceImpl as UtilsInterfaceImpl).getTvSetting()
        }
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            tv?.registerCICallback(ciCallback)
        }
        else {
            tv?.setCICallback(ciCallback)
        }
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
    }

    companion object {
        const val TAG = "CiPlusInterfaceImpl"
    }

    override fun startCAMScan(isTriggeredByUser : Boolean, isCanceled: Boolean) {
        tv?.setCICAMOperatorRefreshRequest(!isCanceled)
    }

    override fun registerListener(listener: CiPlusInterface.RefCiHandlerListener) {
        listeners.add(listener)
    }

    override fun getCiName(): String {
        return tv?.getCIModuleName(selectedSlot) ?: ""
    }

    override fun selectMenuItem(position: Int) {
        mmiLevel++
        tv?.setCIMenuScreenResponse(selectedSlot, position + 1)
    }

    override fun enquiryAnswer(abort: Boolean, answer: String) {
        tv?.setCIEnquiryScreenResponse(selectedSlot, !abort, answer)
    }

    override fun isCamPinEnabled(): Boolean {
        return false
    }

    override fun setCamPin(pin: String) {
        val oldStatus = Settings.Global.getInt(context.contentResolver, "ci_cam_status", 0)
        var passCode = 0
        if (pin != null && pin.isNotEmpty()) {
            passCode = pin.toInt()
        }
        if (oldStatus == 0) {
            val newStatus: Boolean = tv?.setCICAMPinCode(true, passCode) ?: false
            if (newStatus) {
                Settings.Global.putInt(context.contentResolver, "ci_cam_status", 1)
            }
        }
        else if (oldStatus == 1) {
            val newStatus: Boolean = tv?.setCICAMPinCode(false, passCode) ?: false
            if (newStatus)
                Settings.Global.putInt(context.contentResolver, "ci_cam_status", 0)
        }
    }

    override fun getMenu(): MutableList<String> {
        return mutableListOf()
    }

    override fun isCamActive(): Boolean {
        return isCamActive
    }

    override fun getMenuListID(): Int {
        return -1
    }

    override fun getEnqId(): Int {
        return -1
    }

    override fun setMMICloseDone() {
        tv?.closeCIScreen(selectedSlot)
    }

    override fun enterMMI() {
        mmiLevel = 0
        tv?.enterCIMenu(selectedSlot)
    }

    override fun cancelCurrMenu() {
        if(mmiLevel > 0) {
            tv?.setCIMenuScreenResponse(selectedSlot, 0)
            mmiLevel--
        }
        else {
            mmiLevel = 0
            tv?.closeCIScreen(selectedSlot)
        }
    }

    override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
        camTypePreferenceIndex = camTypePreference.ordinal
        tv?.setCiCamDeviceType(camTypePreferenceIndex)
        powerManager?.reboot("CI CAM type changed - $camTypePreferenceIndex")
    }

    override fun isChannelScrambled(): Boolean? {
        return false
    }

    override fun isContentClear(): Boolean {
        return false
    }

    override fun dispose() {
    }

    override fun getProfileName(): String {
        return tv?.getCIOPName(selectedSlot) ?: ""
    }

    override fun deleteProfile(profileName: String) {
        tv?.deleteCIOP(selectedSlot)
    }

    inner class MyCICallback : CICallback() {

        override fun onCICardEventInserted(tv: Tv?) {
            isCamActive = true
            for (listener in listeners) {
                listener.onCICardEventInserted()
            }
        }

        override fun onCICardEventRemoved(tv: Tv?) {
            isCamActive = false
            for (listener in listeners) {
                listener.onCICardEventRemoved()
            }
        }

        override fun onCAMPowerDown(tv: Tv?, timeCount: Int) {
        }

        override fun onCIScreenEventList(tv: Tv?, slot: Int) {
            val camMenu = CiPlusInterface.CamMenu()
            selectedSlot = slot
            camMenu.menuItems = getListItems(tv, slot)
            camMenu.title = getTitleText()
            camMenu.subTitle = getSubTitleText()
            camMenu.bottom = getBottomTitleText()
            camMenu.id = mmiMenuId
            for(listener in listeners) {
                listener.onMenuReceived(camMenu)
            }
            mmiMenuId++
        }

        override fun onCIScreenEventEnquiry(tv: Tv?) {
            var title = tv?.getCIEnquiryScreenText(selectedSlot)
            var inputText = ""
            var enquiry = CiPlusInterface.Enquiry()
            enquiry.id = mmiEnqId
            if (title != null) {
                enquiry.title = title
            }
            enquiry.inputText = inputText

            for (listener in listeners) {
                try {
                    listener.onEnquiryReceived(enquiry)
                }catch (E: Exception){
                    E.printStackTrace()
                }
            }
            mmiEnqId++
        }

        override fun onCIScreenEventClose(tv: Tv?) {
            for(listener in listeners) {
                listener.closePopup()
            }
        }

        override fun onCAMUpgradeStart(tv: Tv?) {
        }

        override fun onCAMUpgradeResult(tv: Tv?, result: Int) {
        }

        override fun onCICardEventNone(tv: Tv?) {
        }

        override fun onCICardEventReady(tv: Tv?) {
        }

        override fun onCICISBypassMatch(tv: Tv?) {
        }

        override fun onCIOPRefreshRequest(tv: Tv?, parcel: Parcel) {
            for(listener in listeners) {
                listener.onStartScanReceived(getProfileName())
            }
        }

        override fun onCIReadCamFail(tv: Tv?) {
        }

        override fun onCINeedBurnCIKey(tv: Tv?) {
        }

        override fun onCIEnterOPDone(tv: Tv?, status: Int) {
        }

        override fun onCILeaveOPDone(tv: Tv?, status: Int) {
        }
    }

    private fun getListItems(tv: Tv?, slot: Int) : MutableList<String> {
        val items: MutableList<String> = ArrayList()
        if (tv != null) {
            listSize = tv.getCIItemNumbers(slot, CI_MENUITEM_ITEM)
        }
        for (j in 0 until listSize) {
            val menuStr: String = tv?.getCIItemTexts(slot, CI_MENUITEM_ITEM, j) ?: ""
            items.add(menuStr)
        }
        return items
    }

    private fun getTitleText() : String {
        val numTitle: Int = tv?.getCIItemNumbers(selectedSlot, CI_MENUITEM_TITLE) ?: 0
        if (numTitle > 0) {
            return tv?.getCIItemTexts(selectedSlot,CI_MENUITEM_TITLE, 0) ?: ""
        }
        return ""
    }

    private fun getSubTitleText() : String {
        val numTitle: Int = tv?.getCIItemNumbers(selectedSlot, CI_MENUITEM_SUBTITLE) ?: 0
        if (numTitle > 0) {
            return tv?.getCIItemTexts(selectedSlot, CI_MENUITEM_SUBTITLE, 0) ?: ""
        }
        return ""
    }

    private fun getBottomTitleText() : String {
        val numTitle: Int = tv?.getCIItemNumbers(selectedSlot, CI_MENUITEM_BOTTOM) ?: 0
        if (numTitle > 0) {
            return tv?.getCIItemTexts(selectedSlot, CI_MENUITEM_BOTTOM, 0) ?: ""
        }
        return ""
    }
}