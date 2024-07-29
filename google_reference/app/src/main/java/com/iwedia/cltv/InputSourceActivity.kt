package com.iwedia.cltv

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.components.InputSourceAdapter
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.InputInformation
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.scene.parental_control.EnterPinAdapter
import com.iwedia.cltv.scene.parental_control.PinItem
import com.iwedia.guide.android.widgets.helpers.BaseLinearLayoutManager
import kotlinx.coroutines.Dispatchers
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.util.Locale

class InputSourceActivity : Activity(),InputSourceNotify {

    private var inputRecylcerView: RecyclerView? = null
    private var inputText: TextView? = null

    var inputAdapter: InputSourceAdapter? = null
    var inputData: ArrayList<InputItem> = ArrayList()
    val inputImgs: ArrayList<Int> = ArrayList()
    val inputFocusImgs: ArrayList<Int> = ArrayList()
    var inputSourceLayout: View? = null
    var parentalLayout: View? = null

    private lateinit var moduleProvider: ModuleProvider
    private lateinit var inputSourceModule: InputSourceInterface
    var utilsModule: UtilsInterface? = null
    var textToSpeechModule: TTSInterface? = null
    var blockedInputs = ArrayList<InputSourceData>()
    var parentalControlSettingsModule: ParentalControlSettingsInterface? = null

    var titleTv: TextView? = null
    var messageTv: TextView? = null
    var pressOk: TextView? = null
    var lockedIv: ImageView? = null
    lateinit var pinAdapter: EnterPinAdapter
    lateinit var pinRecycler: RecyclerView
    private val NUMBER_OF_PIN_ITEMS = 4
    var currentPinPosition = -1
    lateinit var blockedinputData: InputSourceData
    var isBlocked = false
    private var colorMainText : Int = 0
    private var updateTimer: CountDownTimer? = null
    private var focusedPosition: Int = 0
    private var isParentalEnabled : Boolean? = false
    private var isProviderNull : Boolean? = false
    var pinDisplayed: Boolean? = false
    var inputDisplayed: Boolean? = false
    private var isPaused: Boolean? = false


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.input_source_activity)
        ReferenceApplication.isInputPaused = true
        moduleProvider = ModuleProvider(this.application)
        inputSourceModule = moduleProvider.getInputSourceMoudle()
        utilsModule = moduleProvider.getUtilsModule()
        textToSpeechModule = moduleProvider.getTextToSpeechModule()
        if (inputSourceModule.isUserSetUpComplete()) {
            finish()
        }
        var languageCode =
            if (Locale.getDefault().language.uppercase() == moduleProvider.getUtilsModule()
                    .getCountry().uppercase()
            ) {
                Locale.getDefault().language.uppercase()
            } else {
                Locale.getDefault().language.uppercase() + "_" + moduleProvider.getUtilsModule()
                    .getCountry().uppercase()
            }
        ConfigStringsManager.setup(languageCode)
        utilsModule?.stringTranslationListener = object : UtilsInterface.StringTranslationListener {
            override fun getStringValue(stringId: String): String {
                return ConfigStringsManager.getStringById(stringId)
            }
        }
        parentalControlSettingsModule = moduleProvider.getParentalControlSettingsModule()
        inputSourceModule.getUserMode()
        inputSourceModule.setup(false)
        isParentalEnabled = inputSourceModule.isParentalEnabled()
        ReferenceApplication.setActivity(this)
        ConfigColorManager.setup(moduleProvider.getUtilsModule())
        colorMainText = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
        startInputsPanelFtv()
        setUpParentalPin()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if(isPaused == true && inputSourceLayout?.visibility == View.VISIBLE) {
            inputAdapter?.requestFocus(focusedPosition)
            startInputTimer(
                focusedPosition,
                inputData[focusedPosition],
                blockedInputs[focusedPosition].isBlocked
            )
            isPaused = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onDestroy() {
        super.onDestroy()
        inputData.clear()
        blockedInputs.clear()
        stopUpdateTimer()
        ReferenceApplication.isInputOpen = false
        isPaused = false
        inputSourceModule.setValueChanged(false)
        moduleProvider.getInputSourceMoudle().dispose()
    }

    fun setInputPanelVisibility(isVisible: Boolean) {
        if (isVisible) {
            inputDisplayed = true
            inputSourceLayout?.visibility = View.VISIBLE
        } else {
            inputDisplayed = false
            inputSourceLayout?.visibility = View.GONE
        }
    }

    fun setParentalVisibility(isVisible: Boolean) {
        if (isVisible) {
            pinDisplayed = true
            parentalLayout?.visibility = View.VISIBLE
        } else {
            pinDisplayed = false
            parentalLayout?.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startInputsPanelFtv() {
        inputSourceLayout = findViewById(R.id.input_source_id)
        setInputPanelVisibility(true)
        inputRecylcerView = inputSourceLayout!!.findViewById(R.id.input_list)
        inputText = inputSourceLayout!!.findViewById(R.id.input_text)
        inputText?.text = ConfigStringsManager.getStringById("inputs_text_string")
        var defaultValue = utilsModule?.getPrefsValue(
            "inputSelected",
            1
        ) as Int
        inputAdapter =
            InputSourceAdapter(this.application, object : InputSourceAdapter.InputSourceListener {
                override fun getAdapterPosition(position: Int) {
                }

                override fun onClicked(position: Int, inputData: InputItem, blocked: Boolean) {
                   launchMainActivity(position, inputData, blocked)
                }

                override fun adapterSet() {
                    inputRecylcerView?.smoothScrollToPosition(defaultValue)
                    inputRecylcerView?.requestFocus()
                    inputAdapter?.requestFocus(defaultValue)

                }

                override fun blockInput(
                    inputData: InputSourceData,
                    isBlock: Boolean,
                    position: Int
                ) {
                    setInputPanelVisibility(false)
                    setParentalVisibility(true)
                    blockedinputData = inputData
                    isBlocked = isBlock
                    enterParentalCheck()
                }

                override fun unfocusedItem() {
                    stopUpdateTimer()
                }

                override fun focusedItem(position: Int, inputData: InputItem, blocked: Boolean) {
                    focusedPosition = position
                    startInputTimer(position, inputData, blocked)
                }

            })
        inputRecylcerView!!.adapter = inputAdapter
        inputSourceModule.valueChanged.observeForever {
            inputSourceModule.getInputList(object : IAsyncDataCallback<ArrayList<InputItem>> {
                override fun onFailed(error: Error) {

                }

                override fun onReceive(data: ArrayList<InputItem>) {
                    inputData.clear()
                    blockedInputs.clear()
                    inputImgs.clear()
                    inputFocusImgs.clear()
                    getInputImgs()
                    data.forEach {
                        if ((!it.isHidden!!)) {
                            inputData.add(it)
                            blockedInputs.add(
                                InputSourceData(
                                    it.inputSourceName,
                                    it.hardwareId,
                                    it.inputMainName
                                )
                            )
                        } else {
                            inputImgs.remove(it.id)
                            inputFocusImgs.remove(it.id)
                        }
                    }
                    if(isParentalEnabled == true) {
                        getBlockedInputs()
                    }
//                    if(!isParentalEnabled!!) {
//                        blockedInputs = null
//                    }
                    var inputInformation =
                        InputInformation(
                            inputData,
                            inputImgs,
                            inputFocusImgs,
                            blockedInputs,
                            inputSourceModule.isFactoryMode(),
                            isParentalEnabled
                        )
                    inputAdapter!!.refresh(inputInformation)
                }
            })

        }
    }

    private fun startInputTimer(position: Int, inputData: InputItem, blocked: Boolean) {
        stopUpdateTimer()

        updateTimer = object :
            CountDownTimer(
                5000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {
            }
            override fun onFinish() {
                launchMainActivity(position,inputData,blocked)
            }
        }
        updateTimer!!.start()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchMainActivity(position: Int, inputData: InputItem, blocked: Boolean) {
        utilsModule?.setPrefsValue(
            "inputSelected",
            position
        )
        utilsModule?.setPrefsValue(
            "inputSelectedString",
            inputData.inputMainName
        )
        setInputPanelVisibility(false)
        val inputsIntent = Intent("keycode_keyinput")
        inputsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_RECEIVER_FOREGROUND)
        inputsIntent.setPackage("com.iwedia.cltv")
        inputsIntent.putExtra("input_source_name", inputData.inputSourceName)
        inputsIntent.putExtra("input_main_name", inputData.inputMainName)
        inputsIntent.putExtra("input_tune_url",inputData.tuneURL)
        if (isParentalEnabled == true) {
            inputsIntent.putExtra("input_blocked", blocked)
        } else {
            inputsIntent.putExtra("input_blocked", false)
        }

        if(inputData.inputMainName == "TV") {
            moduleProvider.getFastUserSettingsModule().checkTos(object : IAsyncDataCallback<Boolean> {
                override fun onFailed(error: Error) {}
                override fun onReceive(data: Boolean) {
                    if (!data) {
                        inputsIntent.putExtra("is_launch_tos", true)
                        inputsIntent.setClass(
                            applicationContext,
                            TermsOfServiceActivity::class.java
                        )
                        startActivity(inputsIntent)
                        finish()
                        return
                    } else {
                        inputsIntent.setClass(applicationContext, MainActivity::class.java)
                        startActivity(inputsIntent)
                        finish()
                    }
                }
            })
        } else {
            inputsIntent.setClass(applicationContext, MainActivity::class.java)
            startActivity(inputsIntent)
            finish()
        }
    }


    private fun setUpParentalPin() {
        parentalLayout = findViewById(R.id.pin_layout)
        setParentalVisibility(false)

        //setup title
        titleTv = parentalLayout!!.findViewById(R.id.title)
        titleTv!!.setTextColor(colorMainText)
        titleTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        //setup message
        messageTv = parentalLayout!!.findViewById(R.id.message)
        messageTv!!.setTextColor(colorMainText)
        messageTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        //setup second title
        pressOk = parentalLayout!!.findViewById(R.id.pressOkTv)
        pressOk!!.setTextColor(colorMainText)
        pressOk!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        pressOk!!.text = ConfigStringsManager.getStringById("press_ok_to_confirm")
        pressOk!!.visibility = View.INVISIBLE

        //setup lock icon
        lockedIv = parentalLayout!!.findViewById(R.id.lockIv)

        pinAdapter = EnterPinAdapter(getPinItems(NUMBER_OF_PIN_ITEMS)!!)

        //setup recycler and adapter
        pinRecycler = parentalLayout!!.findViewById(R.id.new_pin_items)
        pinRecycler.visibility = View.VISIBLE
        var layoutManager = LinearLayoutManager(applicationContext)
        if(!utilsModule!!.isAccessibilityEnabled()) {
            layoutManager = BaseLinearLayoutManager(applicationContext)
        }
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        pinRecycler.layoutManager = layoutManager
        pinRecycler.adapter = pinAdapter


        if(utilsModule!!.isAccessibilityEnabled()) {
            pinRecycler!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            pinRecycler!!.isFocusable = false
        }

        pinAdapter.registerListener(object : EnterPinAdapter.EnterPinListener {
            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                textToSpeechModule!!.setSpeechText(text = text, importance = importance)
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onPinConfirmed(pinCode: String?) {
                checkPin(pinCode!!)
            }

            override fun getAdapterPosition(position: Int) {
                currentPinPosition = position
                if (position == 3) {
                    pressOk!!.visibility = View.VISIBLE
                } else {
                    pressOk!!.visibility = View.INVISIBLE
                }
            }

            override fun previous() {
            }

            override fun next() {
                currentPinPosition += 1
                if (currentPinPosition > NUMBER_OF_PIN_ITEMS - 1) {
                    currentPinPosition = NUMBER_OF_PIN_ITEMS - 1
                }
                if(!utilsModule!!.isAccessibilityEnabled()) {
                    pinRecycler.getChildAt(currentPinPosition).requestFocus()
                }
            }

            override fun validationEnabled() {
            }

            override fun isAccessibilityEnabled(): Boolean {
                return utilsModule!!.isAccessibilityEnabled()
            }
        })

        if(!utilsModule!!.isAccessibilityEnabled()) {
            pinRecycler.requestFocus()
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        stopUpdateTimer()
    }

    private fun enterParentalCheck() {
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(colorMainText)
        titleTv!!.text = ConfigStringsManager.getStringById("enter_input_pin")
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_using_number")
    }

    private fun getPinItems(numberOfItems: Int): MutableList<PinItem>? {
        val pinItems = mutableListOf<PinItem>()
        for (i in 0 until numberOfItems) {
            pinItems.add(PinItem(i, PinItem.TYPE_PASSWORD))
        }
        return pinItems
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun checkPin(pin: String) {
        if (pin == utilsModule?.getParentalPin()) {
            setParentalVisibility(false)
            if (inputSourceModule.getDefaultValue() == blockedinputData.inputMainName) {
                if (ReferenceApplication.isInputPaused) {
                    if (isBlocked) {
                        InformationBus.submitEvent(
                            Event(
                                Events.BLOCK_TV_VIEW,
                                blockedinputData.inputMainName
                            )
                        )
                    } else {
                        InformationBus.submitEvent(Event(Events.UNBLOCK_TV_VIEW))
                    }
                }
            }
            inputSourceModule.blockInput(
                isBlocked,
                blockedinputData.inputMainName
            )
            pinAdapter.reset()
            startInputsPanelFtv()
        } else {
            moduleProvider.getUtilsModule().showToast(ConfigStringsManager.getStringById("wrong_pin_toast"))
            pinAdapter.reset()
        }
    }


    private fun getBlockedInputs() {
        blockedInputs.forEach {
            it.isBlocked = inputSourceModule.isBlock(it.inputMainName)
        }
    }

    private fun getInputImgs() {
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
    }

     fun stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer!!.cancel()
            updateTimer = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (pinDisplayed!!) {
                setParentalVisibility(false)
                pinAdapter.reset()
                startInputsPanelFtv()
                return true
            }
            ReferenceApplication.isInputPaused = false
            ReferenceApplication.isInputOpen = false
            ReferenceApplication.isBackFromInput = true
            stopUpdateTimer()
            finish()
        }
        return false
    }

    override fun notifyValueChange() {
        stopUpdateTimer()
        if (focusedPosition == (inputAdapter?.itemCount?.minus(1))) {
            focusedPosition = -1
        }
        CoroutineHelper.runCoroutineWithDelay({
            inputAdapter?.requestFocus(focusedPosition + 1)
            inputRecylcerView?.layoutManager?.getChildAt(focusedPosition + 1)
                ?.requestFocus()
        }, 0, Dispatchers.Main)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun showInputPanel() {
        setParentalVisibility(false)
        pinAdapter.reset()
        startInputsPanelFtv()
    }

}
