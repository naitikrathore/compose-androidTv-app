package com.iwedia.cltv.platform.`interface`

import android.media.tv.TvInputInfo
import android.media.tv.TvView
import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.input_source.InputResolutionItem
import com.iwedia.cltv.platform.model.parental.InputSourceData


interface InputSourceInterface {
    fun setup(isFactoryMode: Boolean)
    fun dispose()
    var valueChanged: MutableLiveData<Boolean>

    var inputChanged: MutableLiveData<Boolean>

    fun getInputList(callback: IAsyncDataCallback<ArrayList<InputItem>>)
    fun getAvailableInputList(callback: IAsyncDataCallback<ArrayList<Int>>)
    fun setValueChanged(show: Boolean)

    fun setTvView(tvView: TvView)

    fun handleInputSource( inputSelected: String, inputURL : String = "")

    fun getDefaultValue() : String


    fun getDefaultURLValue(): String

    fun isFactoryMode() : Boolean

    fun getHardwareID(inputName : String) : Int?

    fun unblockInput()

    fun setInputActiveName(activeName: String)

    fun getInputActiveName(): String

    fun getUserMode()

    fun isBasicMode(): Boolean

    fun onApplicationStop()

    fun onApplicationStart()

    fun isParentalEnabled() : Boolean

    fun setResolutionDetails(hdrIndex: Int, hdrGamingIndex: Int)

    fun isCECControlSinkActive() : Boolean

    fun isBlock(inputName: String) : Boolean

    fun blockInput(selected: Boolean, inputName: String)

    fun isUserSetUpComplete() : Boolean

    fun blockInputCount(blockedInputs: MutableList<InputSourceData>): Int

    fun dispatchCECKeyEvent(event : KeyEvent)

    fun getResolutionDetailsForUI() : InputResolutionItem

    fun handleCecTune(inputId : String)

    fun handleCecData(hdmiData: String)

    fun requestUnblockContent(callback: IAsyncCallback)
    fun setLastUsedInput(inputId: String)
    fun exitTVInput(inputId: String)
}