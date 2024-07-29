package com.iwedia.cltv.factorymode

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.database.ContentObserver
import android.media.tv.TvContract
import android.media.tv.TvView
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.ModuleProvider
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.components.InputSourceAdapter
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.InputInformation
import com.iwedia.cltv.factorymode.FactoryUtils.Companion.FACTORY_ENTRY_CLASS_NAME
import com.iwedia.cltv.factorymode.FactoryUtils.Companion.FACTORY_EXTRA_SELECT_CODE
import com.iwedia.cltv.factorymode.FactoryUtils.Companion.FACTORY_PACKAGE_NAME
import com.iwedia.cltv.factorymode.FactoryUtils.Companion.FACTORY_REQUEST_CODE
import com.iwedia.cltv.factorymode.FactoryUtils.Companion.FACTORY_START_CODE
import com.iwedia.cltv.factorymode.FactoryUtils.Companion.FACTORY_START_CODE_EMPTY
import com.iwedia.cltv.factorymode.FactoryUtils.Companion.RESULT_FINISH
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.`interface`.FactoryModeInterface
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.parental.InputSourceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FactoryModeActivity : AppCompatActivity() {

    private var hdmiControlAutoDeviceOffEnabled: String? = null
    private var hdmiControlEnabled: String? = null
    private var hdmiControlAutoWakeUpEnabled: String? = null
    private lateinit var inputSourceMoudle: InputSourceInterface
    private lateinit var factoryModule: FactoryModeInterface
    private var mIsBeforeInitialized: Boolean = true
    private var fromSelectInput: Boolean = false
    private var isStartFactory: Boolean = false
    private var isFactoryInitialScreenVisible : Boolean = false
    private var isVideoAvailable: Boolean = false
    private val TAG = "FactoryModeActivity"
    private var mStartIntent: Int = 0
    private lateinit var moduleProvider: ModuleProvider
    private var tvView: TvView? = null
    private var factoryModeText: TextView? = null
    private var exitFtvText: TextView? = null
    private var noSignalBannerText: TextView? = null

    private var inputRecylcerView: RecyclerView? = null
    var inputAdapter: InputSourceAdapter? = null
    var inputData: ArrayList<InputItem> = ArrayList()
    val inputImgs: ArrayList<Int> = ArrayList()
    val inputFocusImgs: ArrayList<Int> = ArrayList()
    var defaultInputValue = "TV"
    var inputSourceLayout: View? = null
    var tvViewLayout: RelativeLayout? = null
    var zapBanner: RelativeLayout? = null
    var noSignalDialog: RelativeLayout? = null
    private var zapBannerChannelName: TextView? = null
    private var zapBannerChannelNum: TextView? = null
    private var zapprogramName: TextView? = null
    var zapTimer: CountDownTimer? = null
    var noSignalTimer: CountDownTimer? = null
    var selectedSourceViewTimer: CountDownTimer? = null

    private val mKeyQueue = ArrayList<Int>()
    private val KEYQUEUE_SIZE = 4
    private val FINISH_KEYCODES =
        KeyEvent.KEYCODE_0.toString() + KeyEvent.KEYCODE_0.toString() + KeyEvent.KEYCODE_0.toString() + KeyEvent.KEYCODE_INFO.toString()

    var inputSourceSelectionLayout: View? = null
    private var updateTimer: CountDownTimer? = null
    var inputSelectionText: TextView? = null
    var inputPixelText: TextView? = null
    var inputResHDIcon: ImageView? = null
    var inputResUHDIcon: ImageView? = null
    var inputResSDIcon: ImageView? = null
    var inputResFHDIcon: ImageView? = null
    var inputHdrValue: TextView? = null
    var main_text: TextView? = null
    private var channelLoadTimer: CountDownTimer? = null
    private val CHANNEL_UPDATE_TIMEOUT = 3000L


    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SoonBlockedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_factory_mode)
        moduleProvider = ModuleProvider(this.application)
        supportActionBar!!.hide()
        tvViewLayout = findViewById(R.id.tv_view)
        zapBanner = findViewById(R.id.zapBanner)
        zapBannerChannelName = findViewById(R.id.zapChannel_name)
        main_text = findViewById(R.id.main_text)
        main_text?.text = ConfigStringsManager.getStringById("no_signal")
        zapBannerChannelNum = findViewById(R.id.zapChannel_num)
        zapprogramName = findViewById(R.id.zapprogram_name)
        noSignalDialog = findViewById(R.id.noSignalBanner)
        exitFtvText = findViewById(R.id.input_text2)
        noSignalBannerText = findViewById(R.id.text_description)
        tvView = TvView(this)
        tvViewLayout?.addView(tvView)
        tvView!!.setCallback(tvViewCallback)
        moduleProvider.getInputSourceMoudle().setTvView(tvView!!)
        factoryModule = moduleProvider.getFactoryModule()
        ConfigColorManager.setup(moduleProvider.getUtilsModule())
        inputSourceMoudle = moduleProvider.getInputSourceMoudle()
        moduleProvider.getInputSourceMoudle().setup(true)
        isStartFactory = FactoryUtils().isExistFactory(applicationContext)
        factoryModeText = findViewById(R.id.factory_text)
        ReferenceApplication.isFactoryMode = true
        defaultInputValue = inputSourceMoudle.getDefaultValue()
        mStartIntent = intent.getIntExtra(
            FACTORY_START_CODE,
            FACTORY_START_CODE_EMPTY
        )
        startInputsPanelFtv()
        val cls = Settings.Global().javaClass
        hdmiControlAutoDeviceOffEnabled =
            cls.getDeclaredField("HDMI_CONTROL_AUTO_DEVICE_OFF_ENABLED").get(null) as String
        hdmiControlEnabled = cls.getDeclaredField("HDMI_CONTROL_ENABLED").get(null) as String
        hdmiControlAutoWakeUpEnabled =
            cls.getDeclaredField("HDMI_CONTROL_AUTO_WAKEUP_ENABLED").get(null) as String

        mIsBeforeInitialized = FactoryUtils().getInitialToVirginFlag()
        if (mIsBeforeInitialized) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " inside mIsBeforeInitialized $mIsBeforeInitialized")
            //Restore it when it is finished without restoreing the previous setting value
            if (FactoryUtils().isNeedRestoreHdmiSettings(applicationContext)) {
                //restore settings
                FactoryUtils().writeCecOption(
                    applicationContext,
                    hdmiControlEnabled!!,
                    FactoryUtils().isHdmiControlSettings(applicationContext)
                )
                FactoryUtils().writeCecOption(
                    applicationContext,
                    hdmiControlAutoDeviceOffEnabled!!,
                    FactoryUtils().isDevicePowerOffSettings(applicationContext)
                )
                FactoryUtils().writeCecOption(
                    applicationContext,
                    hdmiControlAutoWakeUpEnabled!!,
                    FactoryUtils().isTvPowerOffSettings(applicationContext)
                )
            }
            //remember settings
            FactoryUtils().setHdmiControlSettings(
                applicationContext, FactoryUtils().readCecOption(
                    applicationContext,
                    hdmiControlEnabled as String
                )
            )
            FactoryUtils().setHdmiControlSettings(
                applicationContext,
                FactoryUtils().readCecOption(
                    applicationContext,
                    hdmiControlAutoDeviceOffEnabled as String
                )
            )
            FactoryUtils().setHdmiControlSettings(
                applicationContext,
                FactoryUtils().readCecOption(
                    applicationContext,
                    hdmiControlAutoWakeUpEnabled as String
                )
            )
        }
        factoryModule.restoreEdidVersion()
        loadChannels()
        // Recieve broadcast for Factory TV
        applicationContext.contentResolver.registerContentObserver(
            TvContract.Channels.CONTENT_URI,
            true,
            channelsObserver)
        FactoryReceiver().registerTvReceiver(this, moduleProvider, tvView!!)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, " onCreate() end:mStartInt= $mStartIntent")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadChannels(){
        lifecycleScope.launch {
            withContext(Dispatchers.Default){
                factoryModule.loadChannels(applicationContext)
                withContext(Dispatchers.Main){
                    tuneToInput()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun tuneToInput() {
        if (defaultInputValue == "TV") {
            factoryModule.tuneToActiveChannel(tvView)
        } else {
            handleOnClickOfInputSource(defaultInputValue)
        }
    }



    private val tvViewCallback: TvView.TvInputCallback = object : TvView.TvInputCallback() {

        override fun onVideoUnavailable(inputId: String?, reason: Int) {
            super.onVideoUnavailable(inputId, reason)
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onVideoUnavailable isFactoryInitialScreenVisible " + isFactoryInitialScreenVisible + " reason :" + reason)
            isVideoAvailable = true
            if (!isFactoryInitialScreenVisible && !inputSourceLayout!!.isVisible) {
                showNoSignalDialog(true)
            }
        }

        override fun onVideoAvailable(inputId: String?) {
            super.onVideoAvailable(inputId)
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onVideoAvailable")
            showNoSignalDialog(false)
            isVideoAvailable = false
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume():mStartInt= $mStartIntent, isStartFactory= $isStartFactory")
        if (mIsBeforeInitialized) {
            FactoryUtils().writeCecOption(
                applicationContext,
                hdmiControlEnabled!!,
                false
            )
            FactoryUtils().writeCecOption(
                applicationContext,
                hdmiControlAutoDeviceOffEnabled!!,
                false
            )
            FactoryUtils().writeCecOption(
                applicationContext,
                hdmiControlAutoWakeUpEnabled!!,
                false
            )
            FactoryUtils().setFactoryTvFlag(applicationContext, true)
            val factoryTvFlag = FactoryUtils().getFactoryTvFlag(applicationContext)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "factoryTvFlag " + factoryTvFlag)
        }
        factoryModeText!!.visibility = View.VISIBLE
        if (isStartFactory) {
            startFFactory(mStartIntent)
        }
    }

    private fun startZapTimer() {
        cancelZapTimer()
        //Start new count down timer
        zapTimer = object :
            CountDownTimer(
                5000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                //Close zap banner if it's still visible
                showInfoBanner(false, null)
            }
        }
        zapTimer!!.start()
    }

    private fun cancelZapTimer() {
        if (zapTimer != null) {
            zapTimer!!.cancel()
            zapTimer = null
        }
    }


    private fun cancelNoSignalTimer() {
        if (noSignalTimer != null) {
            noSignalTimer!!.cancel()
            noSignalTimer = null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        mStartIntent = intent!!.getIntExtra(
            FACTORY_START_CODE,
            FACTORY_START_CODE_EMPTY
        )
        if (fromSelectInput) {
            isStartFactory = false
        } else {
            fromSelectInput = intent.getBooleanExtra(
                FACTORY_EXTRA_SELECT_CODE,
                false
            )
            isStartFactory = !fromSelectInput
        }
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "onNewIntent():mStartInt= $mStartIntent, isStartFactory= $isStartFactory ,fromSelectInput $fromSelectInput "
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onActivityResult, requestCode=$requestCode,resultCode=$resultCode $mIsBeforeInitialized")
        if (requestCode == FACTORY_REQUEST_CODE) {
            if (resultCode == RESULT_FINISH) {
                finish()
            } else if (mIsBeforeInitialized) {
                FactoryUtils().setFactoryTvFlag(applicationContext, true)
            }
            //back from cc or FM LIVE
            if (resultCode == 0) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"back from cc or FM LIVE $isVideoAvailable")
                isFactoryInitialScreenVisible = false
                setInputSourceSelection()
                showNoSignalDialog(isVideoAvailable)

            }
        }
    }

    /*
      Display FFactory in foreground
     */
    fun startFFactory(startInt: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startFactoryApp():startInt= $startInt")
        isStartFactory = false
        isFactoryInitialScreenVisible = true
        showInfoBanner(false,null)
        showNoSignalDialog(false)
        FactoryUtils().setIsExistFactory(applicationContext, true)
        val intentFactory = Intent()
        factoryModeText!!.visibility = View.GONE
        intentFactory.component =
            ComponentName(FACTORY_PACKAGE_NAME, FACTORY_ENTRY_CLASS_NAME)
        intentFactory.putExtra(FACTORY_START_CODE, startInt)
        startActivityForResult(intentFactory, FACTORY_REQUEST_CODE)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    private fun showZapBanner() {
        val displayNum = factoryModule.tuneToActiveChannel(tvView)
        if (displayNum.isNotEmpty()) {
            showInfoBanner(true, displayNum)
        }

    }

    private fun handleTuneToChannel(isUp: Boolean) {
        if (defaultInputValue == "TV" && !inputSourceLayout!!.isVisible) {
            val displayNum = factoryModule.channelUpOrDown(isUp, tvView)
            if (displayNum.isNotEmpty()) {
                showInfoBanner(true, displayNum)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "startFactoryApp on press of back key:$keyCode")
                if (inputSourceLayout!!.isVisible) {
                    setInputPanelVisibility(false)
                    //get active channel and tune to it
                    if (defaultInputValue == "TV") {
                        showZapBanner()
                    } else {
                        setInputSourceSelection()
                        showNoSignalDialog(isVideoAvailable)
                    }
                } else {
                    showInfoBanner(false, null)
                    showNoSignalDialog(false)
                    startFFactory(FACTORY_START_CODE_EMPTY)
                }
                return true
            }

            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_DPAD_UP -> {
                handleTuneToChannel(true)
            }

            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_DPAD_DOWN-> {
                handleTuneToChannel(false)

            }

            else -> {
                inputSourceSelectionLayout?.visibility = View.GONE
                showInfoBanner(false, null)
                showNoSignalDialog(false)
                setInputPanelVisibility(true)
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (inputSourceLayout!!.isVisible) {
            val keyCode = event!!.keyCode
            Log.v(TAG, "dispatchKeyEvent():keyCode= $keyCode")
            if (event.action == KeyEvent.ACTION_DOWN) {
                mKeyQueue.add(keyCode)
                if (keyCode != KeyEvent.KEYCODE_0 && keyCode != KeyEvent.KEYCODE_INFO) {
                    mKeyQueue.clear()
                }
                if (mKeyQueue.size == KEYQUEUE_SIZE) {
                    val keystr: String = intArrayListToString(mKeyQueue)
                    if (keystr == FINISH_KEYCODES) {
                        mKeyQueue.clear()
                        this.finish()
                        Log.v(TAG, "FactoryTV finish!!!")
                        return true
                    } else {
                        mKeyQueue.removeAt(0)
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun intArrayListToString(al: ArrayList<Int>): String {
        var str = ""
        for (i in al.indices) {
            str += al[i].toString()
        }
        return str
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onDestroy() {
        super.onDestroy()
        Log.d(Constants.LogTag.CLTV_TAG + TAG," onDestroy called ")
        //Send broadcast to FFactory when factory mode is closed
        val intent = Intent()
        intent.action = "cltv.intent.action.FINISH_FACTORY_MODE"
        sendBroadcast(intent)
        FactoryUtils().setFactoryTvFlag(applicationContext, false)
        FactoryUtils().setIsExistFactory(applicationContext, true)
        tvView?.reset()
        moduleProvider.getInputSourceMoudle().dispose()
        FactoryReceiver().unRegisterReceiver()
        factoryModule.deleteInstance()
         ReferenceApplication.isFactoryMode = false
    }


    public fun setInputPanelVisibility(isVisible: Boolean) {
        if (isVisible) {
            showInfoBanner(false,null)
            inputSourceLayout?.visibility = View.VISIBLE
            requestFocusToInputPanel()
        } else {
            inputSourceLayout?.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startInputsPanelFtv() {
        inputSourceLayout = findViewById<View>(R.id.input_source_id)
        inputSourceSelectionLayout = findViewById<View>(R.id.input_source_selection_id)
        inputRecylcerView = inputSourceLayout!!.findViewById(R.id.input_list)
        inputSelectionText = inputSourceSelectionLayout!!.findViewById(R.id.input_selected_text)
        inputPixelText = inputSourceSelectionLayout!!.findViewById(R.id.input_pixel)
        inputHdrValue = inputSourceSelectionLayout!!.findViewById(R.id.input_hdr)
        inputResHDIcon = inputSourceSelectionLayout!!.findViewById(R.id.input_hd_icon)
        inputResUHDIcon = inputSourceSelectionLayout!!.findViewById(R.id.input_uhd_icon)
        inputResFHDIcon = inputSourceSelectionLayout!!.findViewById(R.id.input_fhd_icon)
        inputResSDIcon = inputSourceSelectionLayout!!.findViewById(R.id.input_sd_icon)


        exitFtvText!!.visibility = View.VISIBLE
        inputAdapter =
            InputSourceAdapter(applicationContext, object : InputSourceAdapter.InputSourceListener {
                override fun getAdapterPosition(position: Int) {
                }

                override fun onClicked(position: Int, inputData: InputItem, blocked: Boolean) {
                    handleOnClickOfInputSource(inputData.inputMainName)
                }

                override fun adapterSet() {
                    requestFocusToInputPanel()
                }

                override fun blockInput(
                    inputData: InputSourceData,
                    isBlock: Boolean,
                    position: Int
                ) {
                }

                override fun unfocusedItem() {
                }

                override fun focusedItem(position: Int, inputData: InputItem, blocked: Boolean) {
                }


            })
        inputRecylcerView!!.adapter = inputAdapter
        inputRecylcerView!!.requestFocus()

        inputImgs.add(R.drawable.input_home)
        inputImgs.add(R.drawable.input_tv)
        inputImgs.add(R.drawable.input_composite)
        inputImgs.add(R.drawable.input_hdmi)
        inputImgs.add(R.drawable.input_hdmi)
        inputImgs.add(R.drawable.input_hdmi)

        inputFocusImgs.add(R.drawable.input_home_focus)
        inputFocusImgs.add(R.drawable.input_tv_focus)
        inputFocusImgs.add(R.drawable.input_composite_focus)
        inputFocusImgs.add(R.drawable.input_hdmi_focus)
        inputFocusImgs.add(R.drawable.input_hdmi_focus)
        inputFocusImgs.add(R.drawable.input_hdmi_focus)
        moduleProvider.getInputSourceMoudle().valueChanged.observeForever {
            moduleProvider.getInputSourceMoudle().getInputList(object :
                IAsyncDataCallback<ArrayList<InputItem>> {
                override fun onFailed(error: Error) {

                }

                override fun onReceive(data: ArrayList<InputItem>) {
                    inputData.clear()
                    inputData.addAll(data)
                    var inputInformation =
                        InputInformation(
                            inputData,inputImgs, inputFocusImgs,null,
                            true,false
                        )
                    inputAdapter!!.refresh(inputInformation)
                }
            })
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    public fun handleOnClickOfInputSource(inputMainName: String) {
        //component is not there in our list
        if (inputMainName == "Component") {
            return
        }
        defaultInputValue = inputMainName
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "[handleOnClickOfInputSource]" + defaultInputValue)
        moduleProvider.getInputSourceMoudle().inputChanged.value = true
        moduleProvider.getInputSourceMoudle().handleInputSource(inputMainName)
        inputSourceLayout?.visibility = View.GONE
        moduleProvider.getInputSourceMoudle().setValueChanged(false)
        selectedSourceViewTimer?.cancel()
        selectedSourceViewTimer = object :
            CountDownTimer(
                1000,
                500
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (isFactoryInitialScreenVisible) {
                    return
                }
                setInputSourceSelection()
            }
        }
        selectedSourceViewTimer!!.start()

    }

    fun showInfoBanner(isVisible: Boolean, channelNum: String?) {
        if (isVisible) {
            zapBannerChannelName!!.text = factoryModule.getChannelDisplayName()
            startZapTimer()
            noSignalDialog!!.visibility = View.GONE
            zapBannerChannelNum!!.text = channelNum
            zapBanner!!.visibility = View.VISIBLE
        } else {
            cancelZapTimer()
            zapBanner!!.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setInputSourceSelection() {
        inputSourceSelectionLayout?.visibility = View.VISIBLE
        inputSelectionText?.text = defaultInputValue
        var inputResolutionItem  = moduleProvider.getInputSourceMoudle().getResolutionDetailsForUI()
        if (inputResolutionItem.iconValue.isNotEmpty()) {
            when (inputResolutionItem.iconValue) {
                "0" -> {
                    inputResHDIcon?.visibility = View.VISIBLE
                    inputResHDIcon?.visibility = View.GONE
                    inputResHDIcon?.visibility = View.GONE
                    inputResHDIcon?.visibility = View.GONE

                }

                "1" -> {
                    inputResHDIcon?.visibility = View.GONE
                    inputResUHDIcon?.visibility = View.VISIBLE
                    inputResFHDIcon?.visibility = View.GONE
                    inputResSDIcon?.visibility = View.GONE

                }

                "2" -> {
                    inputResHDIcon?.visibility = View.GONE
                    inputResUHDIcon?.visibility = View.GONE
                    inputResFHDIcon?.visibility = View.VISIBLE
                    inputResSDIcon?.visibility = View.GONE

                }

                "3", "4" -> {
                    inputResHDIcon?.visibility = View.GONE
                    inputResUHDIcon?.visibility = View.GONE
                    inputResFHDIcon?.visibility = View.GONE
                    inputResSDIcon?.visibility = View.VISIBLE

                }
            }
        } else {
            inputResHDIcon?.visibility = View.GONE
            inputResUHDIcon?.visibility = View.GONE
            inputResFHDIcon?.visibility = View.GONE
            inputResSDIcon?.visibility = View.GONE

        }
        if (inputResolutionItem.pixelValue.isNotEmpty()) {
            inputPixelText?.visibility = View.VISIBLE
            inputPixelText?.text = inputResolutionItem.pixelValue
        } else {
            inputPixelText?.visibility = View.GONE
        }
        if (inputResolutionItem.hdrValue.isNotEmpty()) {
            inputHdrValue?.visibility = View.VISIBLE
            inputHdrValue?.text = inputResolutionItem.hdrValue
        } else {
            inputHdrValue?.visibility = View.GONE
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"pixelValue : ${inputResolutionItem.pixelValue} hdrValue : ${inputResolutionItem.hdrValue}")
        stopInputSourceSelectionTimer()
        updateTimer = object :
            CountDownTimer(
                3000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                inputSourceSelectionLayout?.visibility = View.GONE
                if (defaultInputValue == "TV") {
                    showZapBanner()
                }
            }
        }
        updateTimer!!.start()
    }

    private fun stopInputSourceSelectionTimer() {
        if (updateTimer != null) {
            updateTimer!!.cancel()
            updateTimer = null
        }
    }

    fun showNoSignalDialog(isVisible: Boolean) {
        if (isVisible) {
            cancelNoSignalTimer()
            //Start new count down timer
            noSignalTimer = object :
                CountDownTimer(
                    5000,
                    1000
                ) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    noSignalDialog!!.visibility = View.VISIBLE
                    if (defaultInputValue == "TV") {
                        noSignalBannerText!!.text =
                            application.resources.getString(R.string.factory_no_signal_tv)
                    } else {
                        noSignalBannerText!!.text =
                            application.resources.getString(R.string.factory_no_signal_inputs)
                    }
                }
            }
            noSignalTimer!!.start()

        } else {
            cancelNoSignalTimer()
            noSignalDialog!!.visibility = View.GONE
        }

    }

    private fun requestFocusToInputPanel() {
        var pos = 0
        when (defaultInputValue) {
            "TV" -> pos = 0
            "Composite" -> pos = 1
            "HDMI 1" -> pos = 2
            "HDMI 2" -> pos = 3
            "HDMI 3" -> pos = 4
        }
        inputAdapter?.requestFocus(pos)
        inputRecylcerView?.smoothScrollToPosition(pos)

    }

    private val channelsObserver = object : ContentObserver(Handler(Looper.getMainLooper())){
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            startChannelLoadTimer()
        }
    }

    private fun startChannelLoadTimer() {
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"startChannelLoadTimer")
        stopChannelLoadTimer()
        channelLoadTimer = object :CountDownTimer(CHANNEL_UPDATE_TIMEOUT,
            1000){
            override fun onTick(millisUntilFinished: Long) {
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"finish called")
                loadChannels()
            }

        }
        channelLoadTimer?.start()
    }

    private fun stopChannelLoadTimer() {
        if (channelLoadTimer != null) {
            channelLoadTimer?.cancel()
            channelLoadTimer = null
        }
    }

}
