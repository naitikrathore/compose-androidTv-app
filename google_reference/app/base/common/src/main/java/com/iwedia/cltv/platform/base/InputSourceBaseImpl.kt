package com.iwedia.cltv.platform.base

import android.content.Context
import android.media.tv.TvView
import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.input_source.InputResolutionItem
import com.iwedia.cltv.platform.model.parental.InputSourceData

/**
 * For You Interface Base Implementation
 */
open class InputSourceBaseImpl(
    val context: Context,
    var utilsModule: UtilsInterface,
    var parentalControlSettingsInterface: ParentalControlSettingsInterface
) : InputSourceInterface {
    val GOOGLE_TV_HOME = "Google TV Home"
    val HOME = "Home"
    val TV = "TV"
    val COMPOSITE = "Composite"
    val HDMI1 = "HDMI 1"
    val HDMI2 = "HDMI 2"
    val HDMI3 = "HDMI 3"

    override var valueChanged: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    override var inputChanged: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    var mTvView: TvView? = null



    override fun setup(isFactoryMode: Boolean) {

    }

    override fun dispose() {
    }

    override fun getInputList(callback: IAsyncDataCallback<ArrayList<InputItem>>) {
    }

    override fun getAvailableInputList(callback: IAsyncDataCallback<ArrayList<Int>>) {
    }

    override fun setValueChanged(show: Boolean) {

    }

    override fun setTvView(tvView: TvView) {
        this.mTvView = tvView
    }

    override fun handleInputSource(inputSelected: String, inputURL : String) {

    }

    override fun getDefaultValue(): String {
        return TV
    }

    override fun getDefaultURLValue(): String {
        return ""
    }

    override fun isFactoryMode(): Boolean {
        return false
    }


    override fun getHardwareID(inputName: String): Int? {
        return 0
    }

    override fun unblockInput() {
    }

    override fun setInputActiveName(activeName: String) {

    }

    override fun getInputActiveName(): String {
        return ""
    }

    override fun getUserMode() {

    }

    override fun isBasicMode(): Boolean {
        return false
    }

    override fun onApplicationStop() {

    }

    override fun onApplicationStart() {

    }

    override fun isParentalEnabled(): Boolean {
        return false
    }

    override fun setResolutionDetails(hdrIndex: Int, hdrGamingIndex: Int) {
    }

    override fun isCECControlSinkActive() : Boolean {
        return false
    }

    override fun dispatchCECKeyEvent(event: KeyEvent) {

    }

    override fun getResolutionDetailsForUI(): InputResolutionItem {
        return InputResolutionItem("", "", "")
    }

    override fun handleCecTune(inputId: String) {
    }

    override fun handleCecData(hdmiData: String) {
    }

    override fun isBlock(inputName: String): Boolean {
        return false
    }

    override fun blockInput(selected: Boolean, inputName: String) {
    }

    override fun isUserSetUpComplete() : Boolean {
        return false
    }
    override fun blockInputCount(blockedInputs: MutableList<InputSourceData>): Int {
      return 0
    }

    override fun requestUnblockContent(callback: IAsyncCallback) {

    }

    override fun setLastUsedInput(inputId: String) {
    }


    override fun exitTVInput(inputId: String) {
    }


}