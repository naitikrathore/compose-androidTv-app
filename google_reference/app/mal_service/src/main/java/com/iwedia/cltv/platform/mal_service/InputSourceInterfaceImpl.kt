package com.iwedia.cltv.platform.mal_service

import android.media.tv.TvView
import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.entities.ServiceTvView
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.input_source.InputResolutionItem
import com.iwedia.cltv.platform.model.parental.InputSourceData

class InputSourceInterfaceImpl(private val serviceImpl: IServiceAPI) : InputSourceInterface {
    override fun setup(isFactoryMode: Boolean) {
        serviceImpl.setup(isFactoryMode)
    }

    override fun dispose() {
        serviceImpl.disposeInputSource()
    }

    override var valueChanged: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    override var inputChanged: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    override fun getInputList(callback: IAsyncDataCallback<ArrayList<InputItem>>) {
        var result = arrayListOf<InputItem>()
        var list = serviceImpl.inputList
        list.forEach {
            result.add(
                InputItem(
                    it.id,
                    it.inputMainName,
                    it.inputSourceName,
                    it.isAvailable == 1,
                    it.isHidden,
                    it.hardwareId,
                    it.inputId,
                    it.tuneUrl
                )
            )
        }
        callback.onReceive(result)
    }

    override fun getAvailableInputList(callback: IAsyncDataCallback<ArrayList<Int>>) {
        var result = arrayListOf<Int>()
        serviceImpl.availableInpuList.forEach {
            result.add(it)
        }
        callback.onReceive(result)
    }

    override fun setValueChanged(show: Boolean) {
        serviceImpl.setValueChanged(show)
    }

    override fun setTvView(tvView: TvView) {
        //serviceImpl.setTvView(tvView as ServiceTvView)
    }

    override fun handleInputSource(inputSelected: String, inputURL: String) {
        serviceImpl.handleInputSource(inputSelected, inputURL)
    }

    override fun getDefaultValue(): String {
        return serviceImpl.defaultValue
    }

    override fun getDefaultURLValue(): String {
        return serviceImpl.defaultURLValue
    }

    override fun isFactoryMode(): Boolean {
        return serviceImpl.isFactoryMode
    }

    override fun getHardwareID(inputName: String): Int? {
        return serviceImpl.getHardwareID(inputName)
    }

    override fun unblockInput() {
        serviceImpl.unblockInput()
    }

    override fun setInputActiveName(activeName: String) {
        serviceImpl.inputActiveName = activeName
    }

    override fun getInputActiveName(): String {
        return serviceImpl.inputActiveName
    }

    override fun getUserMode() {
        serviceImpl.getUserMode()
    }

    override fun isBasicMode(): Boolean {
        return serviceImpl.isBasicMode
    }

    override fun onApplicationStop() {
        serviceImpl.onApplicationStop()
    }

    override fun onApplicationStart() {
        serviceImpl.onApplicationStart()
    }

    override fun isParentalEnabled(): Boolean {
        return serviceImpl.isParentalEnabled
    }

    override fun setResolutionDetails(hdrIndex: Int, hdrGamingIndex: Int) {
        serviceImpl.setResolutionDetails(hdrIndex, hdrGamingIndex)
    }

    override fun isCECControlSinkActive(): Boolean {
        return serviceImpl.isCECControlSinkActive
    }

    override fun isBlock(inputName: String): Boolean {
        return serviceImpl.isBlock(inputName)
    }

    override fun blockInput(selected: Boolean, inputName: String) {
        serviceImpl.blockInputInputSource(selected, inputName)
    }

    override fun isUserSetUpComplete(): Boolean {
        return serviceImpl.isUserSetUpComplete
    }

    override fun blockInputCount(blockedInputs: MutableList<InputSourceData>): Int {
        var inputs = mutableListOf<com.cltv.mal.model.entities.InputSourceData>()
        blockedInputs.forEach {
            inputs.add(
                com.cltv.mal.model.entities.InputSourceData(
                    it.inputSourceName,
                    it.hardwareId,
                    it.inputMainName
                )
            )
        }
        return serviceImpl.blockInputCount(inputs.toTypedArray())
    }

    override fun dispatchCECKeyEvent(event: KeyEvent) {
        serviceImpl.dispatchCECKeyEvent(event)
    }

    override fun getResolutionDetailsForUI(): InputResolutionItem {
        return InputResolutionItem(
            serviceImpl.resolutionDetailsForUI.iconValue,
            serviceImpl.resolutionDetailsForUI.pixelValue,
            serviceImpl.resolutionDetailsForUI.hdrValue
        )
    }

    override fun handleCecTune(inputId: String) {
        serviceImpl.handleCecTune(inputId)
    }

    override fun handleCecData(hdmiData: String) {
        serviceImpl.handleCecData(hdmiData)
    }

    override fun requestUnblockContent(callback: IAsyncCallback) {
        serviceImpl.requestUnblockContentInputSource()
        callback.onSuccess()
    }

    override fun setLastUsedInput(inputId: String) {
        serviceImpl.setLastUsedInput(inputId)
    }

    override fun exitTVInput(inputId: String) {
       serviceImpl.exitTVInput(inputId)
    }

}