package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference

open class CiPlusInterfaceBaseImpl: CiPlusInterface {
    companion object {
        const val CASHED_CAM_PIN = "CASHED_CAM_PIN"
    }

    override fun registerListener(listener: CiPlusInterface.RefCiHandlerListener) {

    }

    override fun getCiName(): String {
        return ""
    }

    override fun selectMenuItem(position: Int) {
    }

    override fun enquiryAnswer(abort: Boolean, answer: String) {

    }

    override fun isCamPinEnabled(): Boolean {
        return false
    }

    override fun setCamPin(pin: String) {

    }

    override fun getMenu(): MutableList<String> {
        return mutableListOf()
    }

    override fun isCamActive(): Boolean {
        return false
    }

    override fun getMenuListID(): Int {
        return -1
    }

    override fun getEnqId(): Int {
        return -1
    }

    override fun setMMICloseDone() {
    }

    override fun enterMMI() {
    }

    override fun cancelCurrMenu() {
    }

    override fun isChannelScrambled(): Boolean? {
        return false
    }

    override fun isContentClear(): Boolean {
        return false
    }

    override fun dispose() {

    }

    override fun startCAMScan(isTriggeredByUser : Boolean, isCanceled: Boolean) {

    }

    override fun getProfileName(): String {
        return ""
    }

    override fun deleteProfile(profileName: String) {
    }

    override fun enableProfileInstallation() {

    }

    override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
    }

    override fun platformSpecificOperations(
        operation: CiPlusInterface.PlatformSpecificOperation,
        parameter: Boolean
    ) {

    }

    override fun isCamScanEnabled(): Boolean {
        return true
    }

}