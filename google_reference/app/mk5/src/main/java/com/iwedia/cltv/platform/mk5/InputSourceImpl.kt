package com.iwedia.cltv.platform.mk5

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Handler
import android.provider.*
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.View
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.base.InputSourceBaseImpl
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants.AnokiParentalConstants.USE_ANOKI_RATING_SYSTEM
import com.iwedia.cltv.platform.model.Constants.SharedPrefsConstants.PARENTAL_CONTROLS_ENABLED_TAG
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.input_source.InputResolutionItem
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.mediatek.twoworlds.tv.MtkTvAppTVBase
import com.mediatek.twoworlds.tv.MtkTvBanner
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.wwtv.tvcenter.util.Constants
import com.mediatek.wwtv.tvcenter.util.SaveValue
import com.mediatek.wwtv.tvcenter.util.SystemsApi
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import kotlin.concurrent.thread

internal class InputSourceImpl(
    var applicationContext: Context,
    utilsModule: UtilsInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) :
    InputSourceBaseImpl(
        applicationContext,
        utilsModule,
        parentalControlSettingsInterface
    ) {

    private val TAG = javaClass.simpleName
    private var mTvInputManager: TvInputManager? = null
    var availableList: ArrayList<Int> = ArrayList()
    var tvInputList: List<TvInputInfo>? = null
    val mHandler = Handler()
    val path = "main"

    private val mUriMain = Uri.parse("content://main")
    private val TvInputId = "com.mediatek.tvinput/.tuner.TunerInputService/HW8"
    private val CompositeInputId =
        "com.mediatek.tvinput/.composite.CompositeInputService/HW1"

    //    private val ComponentInputId =
//        "com.mediatek.tvinput/.component.ComponentInputService/HW2"
//    private val VgaInputId = "com.mediatek.tvinput/.vga.VGAInputService/HW3"
    private val HDMI1InputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW4"
    private val HDMI2InputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW5";
    private val HDMI3InputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW6"

    val MAIN_SOURCE_NAME = "multi_view_main_source_name"
    val MAIN_INPUT_TYPE = "MAIN_INPUT_TYPE"
    val MAIN_INPUT_TYPE_FOR_HOME_CHANNEL = "MAIN_INPUT_TYPE_FOR_HOME_CHANNEL"
    private val MAIN_SOURCE_HARDWARE_ID = "multi_view_main_source_hardware_id"


    var hardwareId = 0
    var CompositeHardwareId = 65536
    var ComponentHardwareId = 131072
    var VgaHardwareId = 196608
    var HDMI1HardwareId = 262144
    var HDMI2HardwareId = 327680
    var HDMI3HardwareId = 393216
    var mInputId = "com.mediatek.tvinput/.tuner.TunerInputService/HW8"
    var isInputSourceSelected = false

    var isFactoryMode: Boolean? = false
    private val mHDRVideoInfo = arrayListOf(
        "",
        "HDR10",
        "HLG",
        "",
        "TECHNT",
        "HDR10+",
        "HDR VIVID"
    )
    private val homeId = 0
    private val tvId = 1
    private val compositeId = 2
    private val hdmi1Id = 3
    private val hdmi2Id = 4
    private val hdmi3Id = 5
    var inputList: ArrayList<InputItem> = ArrayList()
    var inputAllList: ArrayList<TvInputInfo> = ArrayList()
    var hardwareIDs: HashMap<String, Int> = HashMap()
    private var tvAppTvBase: MtkTvAppTVBase? = null

    private val TV_MODE_AUTHORITY =
        "com.google.android.apps.tv.launcherx.coreservices.DeviceModeContentProvider"
    private val TV_MODE_QUERY = "queryDeviceMode"
    private var userMode : Int ?= 2
    var inputSourceControl: com.iwedia.cltv.platform.mk5.InputSourceControl? = null
    var hdrIndex: Int? = 0
    var hdrGamingIndex: Int? = 0
    val hdrGaming: String = "HDR10+ Gaming"
    private var settingCheckThread: Thread? = null
    private var runSettingCheck = false

    private var isOKKeyDownSend = false

    private var cecCheckThread: Thread? = null
    private var runCecCheckThread = false
    private val RESET_APPLICATION_ON_START = "exit_application_on_scan"
    var inputBlockedIds: HashMap<String, Boolean> = HashMap()

    override fun setup(isFactoryMode: Boolean) {
        mTvInputManager =
            applicationContext.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager?
        mTvInputManager!!.registerCallback(mTvInputListner, mHandler)
        tvAppTvBase = MtkTvAppTVBase()
        this.isFactoryMode = isFactoryMode
        tvInputList = mTvInputManager!!.tvInputList
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "tv list size===" + tvInputList!!.size)
        valueChanged.value = false
        inputChanged.value = false
        inputSourceControl = InputSourceControl()
        getConnectedList()
        getOtherConnectionDetails()
        setHardwareIds()

    }

    private fun setInputBlockedValues() {
        val pSharedPref: SharedPreferences = context.getSharedPreferences(
            "inputsBlockedList",
            Context.MODE_PRIVATE
        )
        try {
            if (pSharedPref != null) {
                val jsonString = pSharedPref.getString("inputBlockList", "")
                if (jsonString != null && jsonString.isEmpty()) {
                    getBlockedValuesFromMtkApi(pSharedPref)
                }
                val jsonObject = JSONObject(jsonString)
                val keysItr = jsonObject.keys()
                while (keysItr.hasNext()) {
                    val key = keysItr.next()
                    inputBlockedIds[key] = jsonObject[key] as Boolean
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getBlockedValuesFromMtkApi(pSharedPref: SharedPreferences) {
        for (inputItem in inputList) {
            if (!inputItem.isHidden!!) {
                var isBlocked = parentalControlSettingsInterface.isBlockSource(inputItem.hardwareId)
                inputBlockedIds[inputItem.inputMainName] = isBlocked
                if (isBlocked) {
                    parentalControlSettingsInterface.blockInput(
                        false,
                        InputSourceData(
                            inputItem.inputSourceName,
                            inputItem.hardwareId,
                            inputItem.inputMainName
                        )
                    )
                }
            }
        }
        val jsonObject = (inputBlockedIds as Map<*, *>?)?.let { JSONObject(it) }
        val jsonString: String = jsonObject.toString()
        val editor = pSharedPref.edit()
        editor.remove("inputBlockList").apply()
        editor.putString("inputBlockList", jsonString)
        editor.commit()
    }

    override fun isBlock(inputName: String): Boolean {
        if(isParentalEnabled()) {
            val pSharedPref: SharedPreferences = context.getSharedPreferences(
                "inputsBlockedList",
                Context.MODE_PRIVATE
            )
            return try {
                val jsonString = pSharedPref.getString("inputBlockList", "")
                val jsonObject = JSONObject(jsonString)
                val keysItr = jsonObject.keys()
                while (keysItr.hasNext()) {
                    val key = keysItr.next()
                    inputBlockedIds[key] = jsonObject[key] as Boolean
                }
                inputBlockedIds[inputName] == true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    override fun blockInput(selected: Boolean, inputName: String) {
        inputBlockedIds[inputName] = selected
        val pSharedPref: SharedPreferences = context.getSharedPreferences(
            "inputsBlockedList",
            Context.MODE_PRIVATE
        )
        if (pSharedPref != null) {
            val jsonObject = (inputBlockedIds as Map<*, *>?)?.let { JSONObject(it) }
            val jsonString: String = jsonObject.toString()
            val editor = pSharedPref.edit()
            editor.remove("inputBlockList").apply()
            editor.putString("inputBlockList", jsonString)
            editor.commit()
        }

    }

    private fun setHardwareIds() {
        hardwareIDs.clear()
        hardwareIDs[TV] = 0
        hardwareIDs[COMPOSITE] = CompositeHardwareId
        hardwareIDs[HDMI1] = HDMI1HardwareId
        hardwareIDs[HDMI2] = HDMI2HardwareId
        hardwareIDs[HDMI3] = HDMI3HardwareId

    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun getOtherConnectionDetails() {
        val secure = Settings.Secure().javaClass
        val inputHidden = secure.getDeclaredField("TV_INPUT_HIDDEN_INPUTS").get(null) as String
        context.contentResolver.registerContentObserver(
            Settings.Secure.CONTENT_URI.buildUpon()
                .appendPath(inputHidden).build(),
            true,
            object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    CoroutineHelper.runCoroutine({
                        getConnectedList()
                    }, Dispatchers.Main)
                }
            })
    }

    private fun getConnectedList() {
        inputAllList.clear()
        inputList.clear()
        inputBlockedIds.clear()
        if (!isFactoryMode!!) {
            if (userMode == 2) {
                inputList.add(InputItem(homeId, HOME, HOME, true, false, -1, "", ""))
            } else {
                inputList.add(
                    InputItem(
                        homeId,
                        GOOGLE_TV_HOME,
                        GOOGLE_TV_HOME,
                        true,
                        false,
                        -1,
                        "",
                        ""
                    )
                )
            }
        }
        inputList.add(InputItem(tvId, TV, TV, true, false, 0, "",""))
        if (isFactoryMode == true) {
            inputList[0].isHidden = true
        }
        for (input in tvInputList!!) {
            //Skip all the inputs which are not passthrough input.
            if (!input.isPassthroughInput) continue
            inputAllList.add(input)
        }

        getList()
        valueChanged.value = true
    }

    private fun getList() {
        for (i in (inputAllList.size - 1) downTo 0) {
            var tvInputInfo = inputAllList[i]
            var name = getSourceNameForUI(tvInputInfo)

            when (tvInputInfo.id) {
                CompositeInputId -> {
                    inputList.add(
                        InputItem(
                            compositeId,
                            COMPOSITE,
                            name,
                            (mTvInputManager!!.getInputState(tvInputInfo.id) == TvInputManager.INPUT_STATE_CONNECTED),
                            tvInputInfo.isHidden(context), CompositeHardwareId, tvInputInfo.id,CompositeInputId
                        )
                    )
                }

                HDMI1InputId -> {
                    var deviceCount = getInsertedDeviceCount(tvInputInfo)
                    if((deviceCount > 1) && !isFactoryMode()) {
                        var devicesInfo = getInsertedDevicesInfo(tvInputInfo)
                        for(info in devicesInfo) {
                            inputList.add(
                                InputItem(
                                    hdmi1Id,
                                    HDMI1,
                                    tvInputInfo.loadLabel(context).toString() +" : "+ info[0],
                                    (mTvInputManager!!.getInputState(tvInputInfo.id) == TvInputManager.INPUT_STATE_CONNECTED),
                                    tvInputInfo.isHidden(context), HDMI1HardwareId, tvInputInfo.id, info[1]
                                )
                            )
                        }
                    } else {
                        inputList.add(
                            InputItem(
                                hdmi1Id,
                                HDMI1,
                                name,
                                (mTvInputManager!!.getInputState(tvInputInfo.id) == TvInputManager.INPUT_STATE_CONNECTED),
                                tvInputInfo.isHidden(context), HDMI1HardwareId, tvInputInfo.id,HDMI1InputId
                            )
                        )
                    }
                }

                HDMI2InputId -> {
                    var deviceCount = getInsertedDeviceCount(tvInputInfo)
                    if((deviceCount > 1) && !isFactoryMode()) {
                        var devicesInfo = getInsertedDevicesInfo(tvInputInfo)
                        for(info in devicesInfo) {
                            inputList.add(
                                InputItem(
                                    hdmi2Id,
                                    HDMI2,
                                    tvInputInfo.loadLabel(context).toString() +" : "+ info[0],
                                    (mTvInputManager!!.getInputState(tvInputInfo.id) == TvInputManager.INPUT_STATE_CONNECTED),
                                    tvInputInfo.isHidden(context), HDMI2HardwareId, tvInputInfo.id ,info[1]
                                )
                            )
                        }
                    } else {
                        var deviceInfo = tvInputInfo.javaClass.getMethod("getHdmiDeviceInfo").invoke(tvInputInfo)
                        inputList.add(
                            InputItem(
                                hdmi2Id,
                                HDMI2,
                                name,
                                (mTvInputManager!!.getInputState(tvInputInfo.id) == TvInputManager.INPUT_STATE_CONNECTED),
                                tvInputInfo.isHidden(context), HDMI2HardwareId, tvInputInfo.id , HDMI2InputId
                            )
                        )
                    }
                }

                HDMI3InputId -> {
                    var deviceCount = getInsertedDeviceCount(tvInputInfo)
                    if((deviceCount > 1) && !isFactoryMode()) {
                        var devicesInfo = getInsertedDevicesInfo(tvInputInfo)
                        for(info in devicesInfo) {
                            inputList.add(
                                InputItem(
                                    hdmi3Id,
                                    HDMI3,
                                    tvInputInfo.loadLabel(context).toString() +" : "+ info[0],
                                    (mTvInputManager!!.getInputState(tvInputInfo.id) == TvInputManager.INPUT_STATE_CONNECTED),
                                    tvInputInfo.isHidden(context), HDMI3HardwareId, tvInputInfo.id , info[1]
                                )
                            )
                        }
                    } else {
                        inputList.add(
                            InputItem(
                                hdmi3Id,
                                HDMI3,
                                name,
                                (mTvInputManager!!.getInputState(tvInputInfo.id) == TvInputManager.INPUT_STATE_CONNECTED),
                                tvInputInfo.isHidden(context), HDMI3HardwareId, tvInputInfo.id, HDMI3InputId
                            )
                        )
                    }
                }
            }
        }
        setInputBlockedValues()
    }

    private fun getSourceNameForUI(tvInputInfo: TvInputInfo): String {
        val customLabel: String = getCustomSourceName(tvInputInfo)
        return if (TextUtils.isEmpty(customLabel) ||
            TextUtils.equals(customLabel, "null")
        ) {
            getSourceName(tvInputInfo)
        } else {
            customLabel
        }
    }

    private fun getInsertedDeviceName(tvInputInfo: TvInputInfo): String {
        if(isFactoryMode()) {
            return ""
        }
        for (input in inputAllList) {
            var deviceInfo = input.javaClass.getMethod("getHdmiDeviceInfo").invoke(input)
            if (deviceInfo != null) {
                if (input.parentId == tvInputInfo.id) {
                    var name = deviceInfo.javaClass.getMethod("getDisplayName").invoke(deviceInfo)
                    if ((name as String).isEmpty()) {
                        name = "HDMI service"
                    }
                    return " : $name"
                }
            }
        }
        return ""
    }

    private fun getInsertedDeviceCount(tvInputInfo: TvInputInfo): Int {
        var deviceCount = 0

        for (input in inputAllList) {
            var deviceInfo = input.javaClass.getMethod("getHdmiDeviceInfo").invoke(input)
            if (deviceInfo != null) {
                if (input.parentId == tvInputInfo.id) {
                    deviceCount++
                }
            }
        }
        return deviceCount
    }

    private fun getInsertedDevicesInfo(tvInputInfo: TvInputInfo): ArrayList<ArrayList<String>> {
        var retVal = arrayListOf<ArrayList<String>>()
        for (input in inputAllList) {
            var deviceInfo = input.javaClass.getMethod("getHdmiDeviceInfo").invoke(input)

            if (deviceInfo != null) {
                if (input.parentId == tvInputInfo.id) {
                    var name = deviceInfo.javaClass.getMethod("getDisplayName").invoke(deviceInfo)
                    if ((name as String).isEmpty()) {
                        name = "HDMI service"
                    }
                    retVal.add(arrayListOf(name,input.id))
                }
            }
        }
        return retVal
    }

    private fun getSourceName(mTvInputInfo: TvInputInfo): String {
        return if (mTvInputInfo != null) {
            mTvInputInfo.loadLabel(context).toString() + getInsertedDeviceName(mTvInputInfo)
        } else ""
    }

    private fun getCustomSourceName(mTvInputInfo: TvInputInfo): String {
        return if (mTvInputInfo != null) {
            if (mTvInputInfo.loadCustomLabel(context) == null) {
                return ""
            } else {
                return mTvInputInfo.loadCustomLabel(context).toString()
            }
        } else ""
    }


    override fun getInputList(callback: IAsyncDataCallback<ArrayList<InputItem>>) {
        if (inputList.isNotEmpty()) {
            callback.onReceive(inputList)
        } else {
            callback.onFailed(Error("list empty"))
        }
    }

    override fun getAvailableInputList(callback: IAsyncDataCallback<ArrayList<Int>>) {
        if (availableList.isNotEmpty()) {
            callback.onReceive(availableList)
        } else {
            callback.onFailed(Error("list empty"))
        }
    }

    override fun unblockInput() {
        tvAppTvBase!!.unlockService("main")
    }

    private fun writeMtkInputSettingsValues(
        id: String?,
        value: String?,
        isStored: Boolean
    ): Boolean {
        val values = ContentValues()
        values.put(SaveValue.GLOBAL_VALUE_KEY, id)
        values.put(SaveValue.GLOBAL_VALUE_VALUE, value)
        values.put(SaveValue.GLOBAL_VALUE_STORED, isStored)
        try {
            context.contentResolver.insert(
                SaveValue.GLOBAL_PROVIDER_URI_URI,
                values
            )
        } catch (ex: Exception) {
            return false
        }
        return true
    }

    private fun configureMtkInputSettings(inputSelected: String) {
        var inputType = Constants.INPUT_TYPE_OTHER

        when (inputSelected) {
            "TV" -> {
                hardwareId = 0
                mInputId = TvInputId
                inputType = Constants.INPUT_TYPE_TV
            }

            "Composite" -> {
                hardwareId = CompositeHardwareId
                mInputId = CompositeInputId
                inputType = Constants.INPUT_TYPE_COMPOSITE
            }

            "HDMI 1" -> {
                hardwareId = HDMI1HardwareId
                mInputId = HDMI1InputId
                inputType = Constants.INPUT_TYPE_HDMI
            }

            "HDMI 2" -> {
                hardwareId = HDMI2HardwareId
                mInputId = HDMI2InputId
                inputType = Constants.INPUT_TYPE_HDMI
            }

            "HDMI 3" -> {
                hardwareId = HDMI3HardwareId
                mInputId = HDMI3InputId
                inputType = Constants.INPUT_TYPE_HDMI

            }
        }

        //This is used so that MTK part of settings know what is current input and enable
        //HDMI EDID Version functionality if needed.
        writeMtkInputSettingsValues(MAIN_INPUT_TYPE, inputType.toString(), false)
        writeMtkInputSettingsValues(MAIN_INPUT_TYPE_FOR_HOME_CHANNEL, inputType.toString(), true)
        writeMtkInputSettingsValues(MAIN_SOURCE_NAME, inputSelected, true)
    }

    override fun handleInputSource(inputSelected: String, inputURL : String) {
        if (isFactoryMode!!) {
            utilsModule.setPrefsValue(
                "factoryInputSelected",
                inputSelected
            )
        } else {
            utilsModule.setPrefsValue(
                "inputSelectedString",
                inputSelected
            )
        }

        utilsModule.setPrefsValue(
            "inputURLString",
            inputURL
        )

        configureMtkInputSettings(inputSelected)

        if (!inputSelected.contains("Home")) {
            changeSource(inputSelected, inputURL )
        }
    }

    override fun handleCecTune(inputId: String) {
        if (isUserSetUpComplete()) {
            return
        }
        for (tvInputInfo in tvInputList!!) {
            if (tvInputInfo.type == TvInputInfo.TYPE_HDMI && tvInputInfo.id == inputId) {
                if (getCurrentInputInfo()?.id == tvInputInfo.parentId) {
                    return
                } else {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"handle cec tune input parent id: "+tvInputInfo.parentId)
                    when (tvInputInfo.parentId) {
                        HDMI1InputId -> {
                            handleCecData(HDMI1)
                        }

                        HDMI2InputId -> {
                            handleCecData(HDMI2)
                        }

                        HDMI3InputId -> {
                            handleCecData(HDMI3)

                        }
                    }
                }
            }

        }
    }

    override fun handleCecData(hdmiData: String) {
        for (inputPosition in inputList.indices) {
            val inputData = inputList[inputPosition]
            if (inputData.inputMainName == hdmiData) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"handleCecData input:  "+inputData.inputMainName)
                if(isBlock(hdmiData)) {
                    InformationBus.informationBusEventListener.submitEvent(
                        Events.BLOCK_TV_VIEW,
                        arrayListOf(inputData.inputSourceName)
                    )
                }
                InformationBus.informationBusEventListener.submitEvent(
                    Events.HANDLE_INPUT_CEC_TUNE,
                    arrayListOf(inputData,inputPosition)
                )
            }
        }
    }

    override fun getHardwareID(inputName: String): Int? {
        return hardwareIDs[inputName]
    }

    private fun isAppOnForeground(context: Context, appPackageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == appPackageName) {
                return true
            }
        }
        return false
    }

    private fun applicationStopInputConfiguration() {
        writeMtkInputSettingsValues(MAIN_INPUT_TYPE, Constants.INPUT_TYPE_OTHER.toString(), false)
        writeMtkInputSettingsValues(MAIN_SOURCE_NAME, "Null", true)
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MISC_AV_COND_MMP_MODE, 1)
    }

    override fun onApplicationStop() {
        //cancel previous thread that checks if settings is in focus
        if (runSettingCheck) {
            runSettingCheck = false
            settingCheckThread?.interrupt()
            settingCheckThread?.join()
            settingCheckThread = null
        }

        if (runCecCheckThread) {
            runCecCheckThread = false
            cecCheckThread?.interrupt()
            cecCheckThread?.join()
            cecCheckThread = null
        }

        //if application was not stopped by entering settings just configure MTK Input to other
        if (!isAppOnForeground(applicationContext, "com.android.tv.settings")) {
            applicationStopInputConfiguration()
        } else {
            //If application was stopped by entering settings create thread that will configure MTK Input to other
            //when settings is exited
            runSettingCheck = true
            settingCheckThread = thread {
                while (runSettingCheck) {
                    try {
                        Thread.sleep(2000)
                        if (!isAppOnForeground(applicationContext, "com.android.tv.settings") &&
                            !isAppOnForeground(applicationContext, "com.iwedia.cltv")
                        ) {
                            applicationStopInputConfiguration()
                            runSettingCheck = false
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Settings has been exited, setting MTK configuration")
                        }
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "Settings check thread has been interrupted, all good")
                    }
                }
            }
        }

        //HACK Workaround for enabling CEC when application is in stop state
        //TODO rework ASAP
        if (!isCecControlActive()) {
            runCecCheckThread = true
            cecCheckThread = thread {
                while (runCecCheckThread) {
                    try {
                        Thread.sleep(2000)
                        if (isCecControlActive()) {
                            val exitAppIntent = Intent(RESET_APPLICATION_ON_START)
                            exitAppIntent.setPackage("com.iwedia.cltv")
                            applicationContext.sendBroadcast(exitAppIntent)
                            runCecCheckThread = false
                        }
                    } catch (e: InterruptedException) {
                        Log.d(TAG, "CEC check thread has been interrupted, all good")
                    }
                }
            }
        }
    }

    override fun onApplicationStart() {
        //cancel previous thread that checks if settings is in focus
        if (runSettingCheck) {
            runSettingCheck = false
            settingCheckThread?.interrupt()
            settingCheckThread?.join()
            settingCheckThread = null
        }

        if (runCecCheckThread) {
            runCecCheckThread = false
            cecCheckThread?.interrupt()
            cecCheckThread?.join()
            cecCheckThread = null
        }

        configureMtkInputSettings(getDefaultValue())
        if ((hardwareId == HDMI1HardwareId)
            || (hardwareId == HDMI2HardwareId)
            || (hardwareId == HDMI3HardwareId)
        ) {
            mTvView?.reset()
        }
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MISC_AV_COND_MMP_MODE, 0)
    }

    private fun changeSource(inputSelected: String, inputURL : String) {
        utilsModule.setPrefsValue(
            "isInputSourceSelected",
            false
        )
        isInputSourceSelected = utilsModule.getPrefsValue(
            "isInputSourceSelected",
            false
        ) as Boolean
        if (mTvView?.visibility != View.VISIBLE) {
            mTvView?.visibility = View.VISIBLE
        }

        if (inputSelected == "TV") {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "change sourceee")
        } else {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "change sourceee in hdmiii")

            if(inputSelected.contains("HDMI")) {
                if(inputURL.isNotEmpty()) {
                    mTvView?.tune(inputURL, mUriMain)
                } else {
                    mTvView?.tune(mInputId, mUriMain)
                }
            } else {
                mTvView?.tune(mInputId, mUriMain)
            }
        }
        valueChanged.value = true
        inputChanged.value = true
    }

    /**
     * TvInputCallback is used to observe the TvInput events
     */
    private val mTvInputListner: TvInputManager.TvInputCallback =
        object : TvInputManager.TvInputCallback() {
            override fun onInputStateChanged(inputId: String, state: Int) {
                getConnectedList()
            }

            override fun onInputAdded(inputId: String) {
                getConnectedList()
            }

            override fun onInputRemoved(inputId: String) {
                getConnectedList()

            }

            override fun onInputUpdated(inputId: String?) {
                getConnectedList()
            }

            override fun onTvInputInfoUpdated(inputInfo: TvInputInfo?) {
                getConnectedList()
            }

        }


    private fun getCurrentInputInfo(): TvInputInfo? {
        when(getDefaultValue()) {
            TV -> {
                return mTvInputManager!!.getTvInputInfo(TvInputId)
            }
            COMPOSITE -> {
                return mTvInputManager!!.getTvInputInfo(CompositeInputId)
            }

            HDMI1 -> {
                return mTvInputManager!!.getTvInputInfo(HDMI1InputId)
            }

            HDMI2 -> {
                return mTvInputManager!!.getTvInputInfo(HDMI2InputId)
            }

            HDMI3 -> {
                return mTvInputManager!!.getTvInputInfo(HDMI3InputId)
            }

        }
        return null
    }


    override fun setValueChanged(show: Boolean) {
        valueChanged.value = show
    }

    override fun getDefaultValue(): String {
        return if (isFactoryMode!!) {
            utilsModule.getPrefsValue("factoryInputSelected", "TV") as String
        } else {
            utilsModule.getPrefsValue("inputSelectedString", "TV") as String
        }

    }

    override fun getDefaultURLValue(): String {
        return utilsModule.getPrefsValue("inputURLString", "") as String
    }

    override fun setInputActiveName(activeName: String) {
        utilsModule.setPrefsValue("activeInput", activeName)
    }

    override fun getInputActiveName(): String {
        return utilsModule.getPrefsValue("activeInput", "TV") as String
    }

    override fun getUserMode() {
        val uri =
            Uri.parse("content://" + TV_MODE_AUTHORITY + "/" + TV_MODE_QUERY)
        val mCursor: Cursor? = applicationContext.contentResolver.query(
            uri,  /* projection= */
            null,  /* queryArgs= */
            null,  /* cancellationSignal= */
            null
        )
        if (mCursor != null && mCursor.moveToNext()) {
            userMode = mCursor.getInt( /* column= */0)
        }
        try {
            mCursor?.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun isBasicMode(): Boolean {
        var basicMode = false
        val uri = Uri.parse("content://" + TV_MODE_AUTHORITY + "/" + TV_MODE_QUERY)
        val mCursor: Cursor? = applicationContext.contentResolver.query(
            uri,  /* projection= */
            null,  /* queryArgs= */
            null,  /* cancellationSignal= */
            null
        )
        if (mCursor != null && mCursor.moveToNext()) {
            //if user mode is 2 the device is set in basic mode
            basicMode = mCursor.getInt( /* column= */0) == 2
        }
        try {
            mCursor?.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        return basicMode
    }

    override fun isFactoryMode(): Boolean {
        return isFactoryMode == true
    }

    override fun isParentalEnabled(): Boolean {
        return parentalControlSettingsInterface.isParentalControlsEnabled()
    }

    override fun setResolutionDetails(hdrIndex: Int, hdrGamingIndex: Int) {
        this.hdrIndex = hdrIndex
        this.hdrGamingIndex = hdrGamingIndex
    }

    fun getResolutionDetails(): String {
        var resDetails = ""
        if (getDefaultValue() != "TV") {
            var hdrValue =
                getHDRVideoInfoByIndex(
                    hdrIndex!!
                )
            if (getDefaultValue().contains("HDMI")) {
                if (mHDRVideoInfo[5] == hdrValue) {
                    hdrValue =
                        if (hdrGamingIndex == 1) hdrGaming else getHDRVideoInfoByIndex(
                            hdrIndex!!
                        )
                }
            }
            resDetails = getSafeString(
                hdrValue,
                " ",
                getInputResolution()
            )
            return resDetails.uppercase()
        }
        return ""
    }

    override fun getResolutionDetailsForUI(): InputResolutionItem {
        var itemValue: InputResolutionItem? = InputResolutionItem("","","")
        var inputResolutionData = getResolutionDetails()
        var iconValue = ""
        var pixelValue = ""
        var hdrValue = ""

        inputResolutionData.let {
            val inputResolution = it.split(" ")
            if(inputResolution.size == 2) {
                pixelValue = inputResolution[0]
                iconValue = inputResolution[1]
            } else if(inputResolution.size > 2) {
                hdrValue = inputResolution[0]
                pixelValue = inputResolution[1]
                iconValue = inputResolution[2]
            }
            if (inputResolution.size > 1) {
                if (iconValue == "UHD")
                    itemValue?.iconValue = "1"
                else if (iconValue == "HD")
                    if (pixelValue == "1080p" || pixelValue == "1080i") {
                        //fhd value
                        itemValue?.iconValue = "2"
                    } else {
                        //hd value
                        itemValue?.iconValue = "0"
                    }
                else if (iconValue == "FHD")
                    itemValue?.iconValue = "2"
                else if (iconValue == "ED")
                //ed value
                    itemValue?.iconValue = "4"
                else if (iconValue == "SD")
                //sd value
                    itemValue?.iconValue = "3"
            }
            itemValue?.pixelValue = pixelValue
            if(inputResolution.size > 2) {
                itemValue?.hdrValue = hdrValue
            }
        }
        return itemValue!!
    }

    private fun getInputResolution(): String? {
        return MtkTvBanner.getInstance().iptsRslt
    }

    private fun getHDRVideoInfoByIndex(hdrValue: Int): String {
        return if (0 <= hdrValue && hdrValue < mHDRVideoInfo.size) {
            mHDRVideoInfo[hdrValue]
        } else {
            mHDRVideoInfo[0]
        }
    }

    private fun getSafeString(vararg args: String?): String {
        val sb = StringBuilder()
        for (string in args) {
            if (!TextUtils.isEmpty(string)) {
                sb.append(string)
            }
        }
        return sb.toString().trim { it <= ' ' }
    }

    override fun isUserSetUpComplete(): Boolean {
        if (!SystemsApi.isUserSetupComplete(context)) {
            return true
        }
        return false
    }

    override fun blockInputCount(blockedInputs: MutableList<InputSourceData>): Int {
        var blockedInputCount = 0
        blockedInputs.forEach {
            if (inputBlockedIds[it.inputMainName] == true) {

                blockedInputCount++
            }
        }
        return blockedInputCount

    }

    override fun dispose() {
        mTvInputManager!!.unregisterCallback(mTvInputListner)
        mHandler.removeCallbacksAndMessages(null)
        inputSourceControl?.removeCallback()
        inputChanged.value = false
    }

    override fun isCECControlSinkActive(): Boolean {
        if ((mInputId == HDMI1InputId) || (mInputId == HDMI2InputId) || (mInputId == HDMI3InputId)) {
            tvInputList = mTvInputManager!!.tvInputList
            for (input in tvInputList!!) {
                var deviceInfo = input.javaClass.getMethod("getHdmiDeviceInfo").invoke(input)
                if (deviceInfo != null) {
                    if (input.parentId == mInputId) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isCecControlActive(): Boolean {
        for (input in mTvInputManager!!.tvInputList) {
            var deviceInfo = input.javaClass.getMethod("getHdmiDeviceInfo").invoke(input)
            if (deviceInfo != null) {
                if ((input.parentId == HDMI1InputId) ||
                    (input.parentId == HDMI2InputId) ||
                    (input.parentId == HDMI3InputId)
                ) {
                    return true
                }
            }
        }

        return false
    }

    override fun dispatchCECKeyEvent(event: KeyEvent) {
        if (event.keyCode == KEYCODE_DPAD_CENTER) {
            if (event.action == ACTION_UP) {
                if (!isOKKeyDownSend) {
                    //simulate ok key down coz MTK SDK
                    mTvView?.dispatchKeyEvent(
                        KeyEvent(
                            ACTION_DOWN,
                            KEYCODE_DPAD_CENTER
                        )
                    )
                }
                isOKKeyDownSend = false
            }
            if (event.action == ACTION_DOWN) {
                isOKKeyDownSend = true
            }
        }
        mTvView?.dispatchKeyEvent(event)
    }

    override fun requestUnblockContent(callback: IAsyncCallback) {
        CoroutineHelper.runCoroutine({
            MtkTvAppTVBase().unblockSvc("main", true);
            callback.onSuccess()
        })
    }
}