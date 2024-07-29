package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference

interface CiPlusInterface {

    class CamMenu {
        var id  = 0
        var title  = ""
        var subTitle = ""
        var bottom = ""
        var menuItems = mutableListOf<String>()
    }

    class Enquiry {
        var id = 0
        var title = ""
        var inputText = ""
        var blind = false
    }

    enum class CachedPinResult {
        CACHED_PIN_OK,
        CACHED_PIN_NOT_SUPPORTED,
        CACHED_PIN_RETRY,
        CACHED_PIN_FAIL
    }

    enum class PlatformSpecificOperation {
        PSO_RESET_MMI_INTERFACE
    }

    interface RefCiHandlerListener {
        fun onEnquiryReceived(enquiry : Enquiry)

        fun onMenuReceived(menu: CamMenu)

        fun showCiPopup()

        fun closePopup()

        fun onPinResult(result : CachedPinResult)

        fun onStartScanReceived(profileName: String)

        fun onCICardEventInserted()

        fun onCICardEventRemoved()
    }
    fun registerListener(listener: CiPlusInterface.RefCiHandlerListener)
    fun selectMenuItem(position: Int)
    fun enquiryAnswer(abort : Boolean , answer: String)
    fun isCamPinEnabled() : Boolean
    fun setCamPin(pin : String)

    fun getMenu(): MutableList<String>

    fun isCamActive(): Boolean

    fun getCiName(): String

    fun getMenuListID(): Int

    fun getEnqId(): Int

    fun setMMICloseDone()

    fun enterMMI()

    fun cancelCurrMenu()

    fun isChannelScrambled(): Boolean?

    fun isContentClear(): Boolean

    fun dispose()

    fun startCAMScan(isTriggeredByUser : Boolean, isCanceled: Boolean)

    fun getProfileName()  : String

    fun deleteProfile(profileName: String)

    fun enableProfileInstallation()

    fun doCAMReconfiguration(camTypePreference: CamTypePreference)

    fun platformSpecificOperations(operation : PlatformSpecificOperation, parameter : Boolean)

    fun isCamScanEnabled() : Boolean
}