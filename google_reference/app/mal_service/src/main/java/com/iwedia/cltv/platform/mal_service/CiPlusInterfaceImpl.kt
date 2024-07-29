package com.iwedia.cltv.platform.mal_service

import android.os.IBinder
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.ICiPlusListener
import com.cltv.mal.model.ci_plus.CachedPinResult
import com.cltv.mal.model.ci_plus.CiPlusCamMenu
import com.cltv.mal.model.ci_plus.Enquiry
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference

class CiPlusInterfaceImpl(private val serviceImpl: IServiceAPI) : CiPlusInterface {

    override fun registerListener(listener: CiPlusInterface.RefCiHandlerListener) {
        serviceImpl.registerListener(object : ICiPlusListener.Stub() {
            override fun onEnquiryReceived(enquiryIn: Enquiry?) {
                if(enquiryIn != null) {
                    var enquiryOut = CiPlusInterface.Enquiry()
                    enquiryOut.id = enquiryIn.id
                    enquiryOut.title = enquiryIn.title
                    enquiryOut.inputText = enquiryIn.inputText
                    enquiryOut.blind = enquiryIn.blind
                    listener.onEnquiryReceived(enquiryOut)
                }
            }

            override fun onMenuReceived(menuIn: CiPlusCamMenu?) {
                if(menuIn != null) {
                    var menuOut = CiPlusInterface.CamMenu()
                    menuOut.id = menuIn.id
                    menuOut.title = menuIn.title
                    menuOut.subTitle = menuIn.subTitle
                    menuOut.bottom = menuIn.bottom
                    menuOut.menuItems = menuIn.menuItems.toMutableList()
                    listener.onMenuReceived(menuOut)
                }
            }

            override fun showCiPopup() {
                listener.showCiPopup()
            }

            override fun closePopup() {
                listener.closePopup()
            }

            override fun onPinResult(resultIn: CachedPinResult?) {
                if(resultIn != null) {
                    var resultOut = when(resultIn) {
                        CachedPinResult.CACHED_PIN_OK -> CiPlusInterface.CachedPinResult.CACHED_PIN_OK
                        CachedPinResult.CACHED_PIN_NOT_SUPPORTED -> CiPlusInterface.CachedPinResult.CACHED_PIN_NOT_SUPPORTED
                        CachedPinResult.CACHED_PIN_RETRY -> CiPlusInterface.CachedPinResult.CACHED_PIN_RETRY
                        CachedPinResult.CACHED_PIN_FAIL -> CiPlusInterface.CachedPinResult.CACHED_PIN_FAIL
                    }

                    listener.onPinResult(resultOut)
                }
            }

            override fun onStartScanReceived(profileNameIn: String?) {
                if(profileNameIn != null) {
                    listener.onStartScanReceived(profileNameIn)
                }
            }

            override fun onCICardEventInserted() {
                listener.onCICardEventInserted()
            }

            override fun onCICardEventRemoved() {
                listener.onCICardEventRemoved()
            }
        })
    }

    override fun selectMenuItem(position: Int) {
        serviceImpl.selectMenuItem(position)
    }

    override fun enquiryAnswer(abort: Boolean, answer: String) {
        serviceImpl.enquiryAnswer(abort,answer)
    }

    override fun isCamPinEnabled(): Boolean {
        return serviceImpl.isCamPinEnabled
    }

    override fun setCamPin(pin: String) {
        serviceImpl.setCamPin(pin)
    }

    override fun getMenu(): MutableList<String> {
        return mutableListOf()
    }

    override fun isCamActive(): Boolean {
        return serviceImpl.isCamActive
    }

    override fun getCiName(): String {
        return serviceImpl.ciName
    }

    override fun getMenuListID(): Int {
        return serviceImpl.menuListID
    }

    override fun getEnqId(): Int {
        return serviceImpl.enqId
    }

    override fun setMMICloseDone() {
        serviceImpl.setMMICloseDone()
    }

    override fun enterMMI() {
        serviceImpl.enterMMI()
    }

    override fun cancelCurrMenu() {
        serviceImpl.cancelCurrMenu()
    }

    override fun isChannelScrambled(): Boolean? {
        return serviceImpl.isChannelScrambled
    }

    override fun isContentClear(): Boolean {
        return false
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun startCAMScan(isTriggeredByUser: Boolean, isCanceled: Boolean) {
        serviceImpl.startCAMScan(isTriggeredByUser, isCanceled)
    }

    override fun getProfileName(): String {
        return serviceImpl.getProfileName()
    }

    override fun deleteProfile(profileName: String) {
        serviceImpl.deleteProfile(profileName)
    }

    override fun enableProfileInstallation() {
        serviceImpl.enableProfileInstallation()
    }
    override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
        var value = when(camTypePreference) {
            CamTypePreference.PCMCIA -> com.cltv.mal.model.ci_plus.CamTypePreference.PCMCIA
            CamTypePreference.USB -> com.cltv.mal.model.ci_plus.CamTypePreference.USB
        }
        serviceImpl.doCAMReconfiguration(value)
    }

    override fun platformSpecificOperations(
        operation: CiPlusInterface.PlatformSpecificOperation,
        parameter: Boolean
    ) {
        var value = when(operation) {
            CiPlusInterface.PlatformSpecificOperation.PSO_RESET_MMI_INTERFACE -> com.cltv.mal.model.ci_plus.PlatformSpecificOperation.PSO_RESET_MMI_INTERFACE
        }

        serviceImpl.platformSpecificOperations(value, parameter)
    }
}