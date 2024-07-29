package com.iwedia.cltv.factorymode

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.tv.TvView
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.ModuleProvider
import com.iwedia.cltv.platform.`interface`.FactoryModeInterface
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.model.Constants

class FactoryReceiver : BroadcastReceiver() {


    private var convertedInputSourceNameForFactoryLog = ""
    private var resultValForFactoryLog = 2 // Others : NG (Not Equal zero or one)
    private var channelNumForFactoryLog = 0
    private var mActivity: FactoryModeActivity? = null
    private var factoryModule: FactoryModeInterface? = null
    private var inputSourceModule: InputSourceInterface? = null
    private var tvView: TvView? = null


    companion object {
        val TAG = "FactoryReciever"
        val FACTORY_TAG = "FactoryAtCmd[AtCommandExecutor]"
        private const val INTENT_ACTION_FM_CHGCH_INPUT_SOURCE =
            "factorytv.intent.action.fm.chgch.input.source"
        private const val INTENT_ACTION_FM_CHGCH_DIGITAL_TUNE =
            "factorytv.intent.action.fm.chgch.digital.tune"
        private const val INTENT_ACTION_REMOTE_9_CHANNEL_MAP_UPDATE =
            "factorytv.intent.action.remote.9.channel.map.update"
        private const val INTENT_ACTION_INITIALISED_TUNE = "_com.mediatek.tv.initializedtune"
        private const val INTENT_ACTION_INITIALISED_TUNE_HDMI2 =
            "_com.mediatek.tv.initializedtune_hdmi2"
        private const val INTENT_ACTION_INITIALISED_TUNE_HDMI3 =
            "_com.mediatek.tv.initializedtune_hdmi3"
        private const val INTENT_ACTION_INITIALISED_TUNE_COMPOSITE =
            "_com.mediatek.tv.initializedtune_composite"
        private const val INTENT_ACTION_CHANNEL_PRE = "_com.mediatek.tv.channelpre"
        private const val INTENT_ACTION_CHANNEL_DOWN = "_com.mediatek.tv.channelupdown"
        private const val INTENT_ACTION_CHANNEL_SELECT = "_com.mediatek.tv.selectchannel"
        private const val INTENT_ACTION_SELECT_TV = "_com.mediatek.select.TV"
        private const val INTENT_ACTION_SELECT_DTV = "_com.mediatek.select.DTV"
        private const val INTENT_ACTION_SELECT_COMPOSITE = "_com.mediatek.select.Composite"
        private const val INTENT_ACTION_SELECT_COMPONENT = "_com.mediatek.select.Component"
        private const val INTENT_ACTION_SELECT_SCART = "_com.mediatek.select.SCART"
        private const val INTENT_ACTION_SELECT_HDMI1 = "_com.mediatek.select.HDMI1"
        private const val INTENT_ACTION_SELECT_HDMI2 = "_com.mediatek.select.HDMI2"
        private const val INTENT_ACTION_SELECT_HDMI3 = "_com.mediatek.select.HDMI3"
        private const val INTENT_ACTION_SELECT_HDMI4 = "_com.mediatek.select.HDMI4"
        private const val INTENT_ACTION_SELECT_VGA = "_com.mediatek.select.VGA"

        private const val INTENT_ACTION_INITIALISED_TUNE_RESULT =
            "_com.mediatek.tv.initializedtune.result"
        private const val INTENT_ACTION_INITIALISED_TUNE_HDMI3_RESULT =
            "_com.mediatek.tv.initializedtune.resulthdmi3"
        private const val INTENT_ACTION_INITIALISED_TUNE_HDMI2_RESULT =
            "_com.mediatek.tv.initializedtune.resulthdmi2"
        private const val INTENT_ACTION_INITIALISED_TUNE_COMPOSITE_RESULT =
            "_com.mediatek.tv.initializedtune.resultcomposite"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun registerTvReceiver(
        mActivity: FactoryModeActivity,
        moduleProvider: ModuleProvider,
        tvView: TvView
    ) {
        this.mActivity = mActivity
        this.factoryModule = moduleProvider.getFactoryModule()
        this.inputSourceModule = moduleProvider.getInputSourceMoudle()
        this.tvView = tvView
        val filter = IntentFilter()
        filter.addAction(INTENT_ACTION_SELECT_TV)
        filter.addAction(INTENT_ACTION_SELECT_DTV)
        filter.addAction(INTENT_ACTION_SELECT_COMPOSITE)
        filter.addAction(INTENT_ACTION_SELECT_COMPONENT)
        filter.addAction(INTENT_ACTION_SELECT_SCART)
        filter.addAction(INTENT_ACTION_SELECT_HDMI1)
        filter.addAction(INTENT_ACTION_SELECT_HDMI2)
        filter.addAction(INTENT_ACTION_SELECT_HDMI3)
        filter.addAction(INTENT_ACTION_SELECT_HDMI4)
        filter.addAction(INTENT_ACTION_SELECT_VGA)

        filter.addAction(INTENT_ACTION_CHANNEL_SELECT)
        filter.addAction(INTENT_ACTION_CHANNEL_DOWN)
        filter.addAction(INTENT_ACTION_CHANNEL_PRE)
        filter.addAction(INTENT_ACTION_INITIALISED_TUNE)
        filter.addAction(INTENT_ACTION_INITIALISED_TUNE_HDMI2)
        filter.addAction(INTENT_ACTION_INITIALISED_TUNE_HDMI3)
        filter.addAction(INTENT_ACTION_INITIALISED_TUNE_COMPOSITE)

        filter.addAction(INTENT_ACTION_FM_CHGCH_INPUT_SOURCE)
        filter.addAction(INTENT_ACTION_FM_CHGCH_DIGITAL_TUNE)

        filter.addAction(INTENT_ACTION_REMOTE_9_CHANNEL_MAP_UPDATE)
        filter.addAction(Intent.ACTION_SCREEN_ON)

        mActivity.registerReceiver(this, filter)


    }

    fun unRegisterReceiver() {
        mActivity?.unregisterReceiver(this)
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, " FactoryRecivever intent null")
            return
        }
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, " FactoryRecivever intent " + intent.action)
        var resultVal = 1
        when (intent.action) {
            INTENT_ACTION_FM_CHGCH_INPUT_SOURCE -> {
                if (mActivity?.isTaskRoot!!) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "INTENT_ACTION_FM_CHGCH_INPUT_SOURCE")
                    var inputSourceName = ""
                    var convertedInputSourceName = ""
                    if (intent.extras != null) {
                        inputSourceName =
                            intent.extras!!.getString(FactoryUtils.FACTORY_EXTRA_NAME_CODE, "null")
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "inputSourceName= $inputSourceName")
                        convertedInputSourceName = if (inputSourceName == "Hdmi1") {
                            "HDMI 1"
                        } else if (inputSourceName == "Hdmi2") {
                            "HDMI 2"
                        } else if (inputSourceName == "Hdmi3") {
                            "HDMI 3"
                        } else {
                            // NOTE : Add items as needed
                            inputSourceName
                        }
                        convertedInputSourceNameForFactoryLog = convertedInputSourceName
                    }
                    Log.v(TAG, "handleInputSource change here $convertedInputSourceName")
                    mActivity!!.showNoSignalDialog(false)
                    mActivity!!.showInfoBanner(false, null)
                    mActivity!!.setInputPanelVisibility(false)
                    mActivity!!.handleOnClickOfInputSource(convertedInputSourceName)
                    resultVal = 0
                    resultValForFactoryLog = resultVal

                    // Do not delete! This is Factory Log.
                    Handler().postDelayed({
                        val hasSignal: Boolean = factoryModule!!.hasSignal()
                        resultValForFactoryLog = if (resultValForFactoryLog == 0 && hasSignal) {
                            0 // 0 : Switch OK (input signal detection)
                        } else if (resultValForFactoryLog == 0 && !hasSignal) {
                            1 // 1 : Switch OK (input signal undetection)
                        } else {
                            2 // Others : NG (Not Equal zero or one)
                        }
                        if (convertedInputSourceNameForFactoryLog == "HDMI 1") {
                            Log.d(Constants.LogTag.CLTV_TAG + FACTORY_TAG, "RT CHGCH 3 1 $resultValForFactoryLog")
                        } else if (convertedInputSourceNameForFactoryLog == "HDMI 2") {
                            Log.d(Constants.LogTag.CLTV_TAG + FACTORY_TAG, "RT CHGCH 3 2 $resultValForFactoryLog")
                        } else if (convertedInputSourceNameForFactoryLog == "HDMI 3") {
                            Log.d(Constants.LogTag.CLTV_TAG + FACTORY_TAG, "RT CHGCH 3 3 $resultValForFactoryLog")
                        } else {
                            // NOTE : Add items as needed
                            Log.d(Constants.LogTag.CLTV_TAG + FACTORY_TAG, "RT CHGCH 1 1 $resultValForFactoryLog")
                        }
                    }, 2000)
                }
            }

            INTENT_ACTION_FM_CHGCH_DIGITAL_TUNE -> {
                if (mActivity?.isTaskRoot!!) {
                    var channelNum = 0
                    val channelName: String
                    if (intent.extras != null) {
                        channelNum = intent.extras!!.getInt(FactoryUtils.FACTORY_EXTRA_NUM_CODE, 0)
                        channelNumForFactoryLog = channelNum
                    }
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "INTENT_ACTION_FM_CHGCH_DIGITAL_TUNE ${channelNum}")
                    channelName =
                        String.format((channelNum / 10).toString() + "-" + channelNum % 10)
                    Log.v(TAG, "channelNum= $channelNum, channelName1= $channelName")
                    mActivity!!.showNoSignalDialog(false)
                    mActivity!!.showInfoBanner(false, null)
                    mActivity!!.setInputPanelVisibility(false)
                    val uriStr = intent.getStringExtra("channelUriStr")
                    fmChgChTuneTV(channelName, uriStr)


                }

            }

            INTENT_ACTION_REMOTE_9_CHANNEL_MAP_UPDATE -> {
                val needUpdateChannel = intent.getBooleanExtra("updateChannel", true)
                Log.v(
                    TAG,
                    "INTENT_ACTION_REMOTE_9_CHANNEL_MAP_UPDATE needUpdateChannel $needUpdateChannel"
                )
                if (needUpdateChannel) {
                    factoryModule!!.tuneToActiveChannel(tvView)
                } else {
                    val am = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val tasks = am.getRunningTasks(1)
                    val cn = tasks[0].topActivity
                    if (cn!!.packageName == FactoryUtils.FACTORY_PACKAGE_NAME) {
                        //NOP
                    } else {
                        mActivity!!.startFFactory(FactoryUtils.FACTORY_START_CODE_EMPTY)
                    }
                }
            }

            INTENT_ACTION_SELECT_TV,
            INTENT_ACTION_SELECT_DTV,
            INTENT_ACTION_SELECT_COMPOSITE,
            INTENT_ACTION_SELECT_COMPONENT,
            INTENT_ACTION_SELECT_SCART,
            INTENT_ACTION_SELECT_HDMI1,
            INTENT_ACTION_SELECT_HDMI2,
            INTENT_ACTION_SELECT_HDMI3,
            INTENT_ACTION_SELECT_HDMI4,
            INTENT_ACTION_SELECT_VGA -> {
                if (1 == factoryModule!!.getVendorMtkAutoTest()) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectSourceReceiver")
                    val action = intent.action
                    val sourcename = action!!.substring(action.lastIndexOf(".") + 1)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectSourceReceiver,sourcename$sourcename")
                    mActivity!!.showNoSignalDialog(false)
                    mActivity!!.showInfoBanner(false,null)
                    mActivity!!.setInputPanelVisibility(false)
                    mActivity!!.handleOnClickOfInputSource(sourcename)
                }
            }

            INTENT_ACTION_INITIALISED_TUNE -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "INTENT_ACTION_INITIALISED_TUNE start")
                val isTuneHDMI = intent.getBooleanExtra("isTuneHDMI", true)
                if (isTuneHDMI) {
                    val inputSourceName = "HDMI 1"
                    mActivity!!.showNoSignalDialog(false)
                    mActivity!!.showInfoBanner(false,null)
                    mActivity!!.setInputPanelVisibility(false)
                    mActivity!!.handleOnClickOfInputSource(inputSourceName)
                     
                } else {
                    val isUp = intent.getBooleanExtra("upOrdown", true)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "_com.mediatek.tv.channelupdown:isUp= $isUp")
                    //TODO channel up or down
                }
                Handler().postDelayed({
                    val intent = Intent(INTENT_ACTION_INITIALISED_TUNE_RESULT)
                    intent.putExtra("isTuneHDMI", isTuneHDMI)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "HDMI result sent to factory:$isTuneHDMI")
                    context!!.sendBroadcast(intent)
                }, 2000)
            }

            INTENT_ACTION_INITIALISED_TUNE_HDMI2 -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "INTENT_ACTION_INITIALISED_TUNE_HDMI2")
                val inputSourceName = "HDMI 2"
                mActivity!!.showNoSignalDialog(false)
                mActivity!!.showInfoBanner(false,null)
                mActivity!!.setInputPanelVisibility(false)
                mActivity!!.handleOnClickOfInputSource(inputSourceName)
                 
                Handler().postDelayed({
                    val intent = Intent(INTENT_ACTION_INITIALISED_TUNE_HDMI2_RESULT)
                    // intent.putExtra("isTuneHDMI", isTuneHDMI);
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "HDMI result sent to factory: hdmi2")
                    context!!.sendBroadcast(intent)
                }, 2000)
            }

            INTENT_ACTION_INITIALISED_TUNE_HDMI3 -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "INTENT_ACTION_INITIALISED_TUNE_HDMI3 start")
                val inputSourceName = "HDMI 3"
                mActivity!!.showNoSignalDialog(false)
                mActivity!!.showInfoBanner(false,null)
                mActivity!!.setInputPanelVisibility(false)
                mActivity!!.handleOnClickOfInputSource(inputSourceName)
                 
                Handler().postDelayed({
                    val intent = Intent(INTENT_ACTION_INITIALISED_TUNE_HDMI3_RESULT)
                    // intent.putExtra("isTuneHDMI", isTuneHDMI);
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "HDMI result sent to factory: hdmi3"
                    )
                    context!!.sendBroadcast(intent)
                }, 2000)
            }

            INTENT_ACTION_INITIALISED_TUNE_COMPOSITE -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "INTENT_ACTION_INITIALISED_TUNE_COMPOSITE start")
                val inputSourceName = "Composite"
                mActivity!!.showNoSignalDialog(false)
                mActivity!!.showInfoBanner(false,null)
                mActivity!!.setInputPanelVisibility(false)
                mActivity!!.handleOnClickOfInputSource(inputSourceName)
                 
                Handler().postDelayed({
                    val intent = Intent(INTENT_ACTION_INITIALISED_TUNE_COMPOSITE_RESULT)
                    // intent.putExtra("isTuneHDMI", isTuneHDMI);
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "HDMI result sent to factory: composite"
                    )
                    context!!.sendBroadcast(intent)
                }, 2000)
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "ACTION_SCREEN_ON: ${mActivity?.defaultInputValue!!}")
                if (mActivity?.defaultInputValue!! == "TV") {
                    factoryModule?.tuneToActiveChannel(tvView)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun fmChgChTuneTV(channelName: String?, uriStr: String?) {
        var resultVal = 1
        val defaultValue = inputSourceModule!!.getDefaultValue()
        val inputSourceTv = "TV"
        if (channelName != null) {
            var returnValue = -1
            returnValue =
                factoryModule!!.tuneByChannelNameOrNum(channelName, false, tvView)
            if (returnValue == 0) {
                resultVal = 0
                resultValForFactoryLog = resultVal
                if (defaultValue != inputSourceTv) {
                    mActivity!!.handleOnClickOfInputSource(inputSourceTv)
                }
                mActivity!!.showInfoBanner(true, channelName)
            } else{
                //tune failed
                resultValForFactoryLog = 2
            }
        } else if (uriStr != null) {
            var returnValue = 0
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "began to get select data:$uriStr")
            val channelUri = Uri.parse(uriStr)
            val displayNum =
                factoryModule!!.tuneByUri(channelUri, mActivity!!.applicationContext, tvView)
            if(displayNum.isNotEmpty()){
                returnValue = 1

                if (defaultValue != inputSourceTv) {
                    mActivity!!.handleOnClickOfInputSource(inputSourceTv)
                }
               mActivity!!.showInfoBanner(true, displayNum)
            } else {
                //tune failed
                resultValForFactoryLog = 2
            }
            if (returnValue == 1) {
                resultVal = 0
                resultValForFactoryLog = resultVal
            }
        }
        // Do not delete! This is Factory Log.
        Handler().postDelayed({
            val hasSignal: Boolean = factoryModule!!.hasSignal()
            resultValForFactoryLog = if (resultValForFactoryLog == 0 && hasSignal) {
                0 // 0 : Switch OK (input signal detection)
            } else if (resultValForFactoryLog == 0 && !hasSignal) {
                1 // 1 : Switch OK (input signal undetection)
            } else {
                2 // Others : NG (Not Equal zero or one)
            }
            Log.d(Constants.LogTag.CLTV_TAG +
                FACTORY_TAG,
                "RT CHGCH 5 " + channelNumForFactoryLog.toFloat() / 10 + " " + resultValForFactoryLog
            )
        }, 5000)
    }
}