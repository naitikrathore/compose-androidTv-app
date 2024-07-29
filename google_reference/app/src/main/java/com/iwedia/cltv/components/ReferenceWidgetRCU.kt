package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import world.widget.GWidget
import world.widget.GWidgetListener


/**
 * RCU widget
 * @author Nishant Bansal
 */
class ReferenceWidgetRCU : GWidget<ConstraintLayout, ReferenceWidgetRCU.RCUWidgetListener> {

    var context: Context? = null
    var configParam: SceneConfig? = null
    var activeButton: CustomButton? = null

    //number buttons
    var button0: CustomButton? = null
    var button1: CustomButton? = null
    var button2: CustomButton? = null
    var button3: CustomButton? = null
    var button4: CustomButton? = null
    var button5: CustomButton? = null
    var button6: CustomButton? = null
    var button7: CustomButton? = null
    var button8: CustomButton? = null
    var button9: CustomButton? = null
    var button_: CustomButton? = null
    var buttonBackspace: CustomButton? = null
    var buttonOk: CustomButton? = null
    var buttonTTx: CustomButton? = null
    var buttonRed: CustomButton? = null
    var buttonBlue: CustomButton? = null
    var buttonYellow: CustomButton? = null
    var buttonGreen: CustomButton? = null

    var timer: CountDownTimer? = null
    var timerTimeout = 10000L

    /**
     * Constructor
     */
    @RequiresApi(Build.VERSION_CODES.R)
    constructor(
        context: Context,
        listener: RCUWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.MIN_RCU,
        ReferenceWorldHandler.WidgetId.MIN_RCU,
        listener
    ) {
        this.context = context
        setup()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("InflateParams")
    private fun setup(){
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_min_rcu, null) as ConstraintLayout
        
        button0 = view!!.findViewById<CustomButton?>(R.id.button0).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button1 = view!!.findViewById<CustomButton?>(R.id.button1).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button2 = view!!.findViewById<CustomButton?>(R.id.button2).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button3 = view!!.findViewById<CustomButton?>(R.id.button3).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button4 = view!!.findViewById<CustomButton?>(R.id.button4).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button5 = view!!.findViewById<CustomButton?>(R.id.button5).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button6 = view!!.findViewById<CustomButton?>(R.id.button6).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button7 = view!!.findViewById<CustomButton?>(R.id.button7).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button8 = view!!.findViewById<CustomButton?>(R.id.button8).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button9 = view!!.findViewById<CustomButton?>(R.id.button9).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        button_ = view!!.findViewById<CustomButton?>(R.id.button_).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        buttonBackspace = view!!.findViewById<CustomButton?>(R.id.button_backspace).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }
        buttonOk = view!!.findViewById<CustomButton?>(R.id.button_ok).also {
            it.textToSpeechHandler.setupTextToSpeechTextSetterInterface(listener)
        }

        // TODO TTS: those buttons must be created as the previous buttons, make sure that TTS functionality works here.
        button0 = view!!.findViewById(R.id.button0)
        button1 = view!!.findViewById(R.id.button1)
        button2 = view!!.findViewById(R.id.button2)
        button3 = view!!.findViewById(R.id.button3)
        button4 = view!!.findViewById(R.id.button4)
        button5 = view!!.findViewById(R.id.button5)
        button6 = view!!.findViewById(R.id.button6)
        button7 = view!!.findViewById(R.id.button7)
        button8 = view!!.findViewById(R.id.button8)
        button9 = view!!.findViewById(R.id.button9)
        button_ = view!!.findViewById(R.id.button_)
        buttonBackspace = view!!.findViewById(R.id.button_backspace)
        buttonOk = view!!.findViewById(R.id.button_ok)
        buttonTTx = view!!.findViewById(R.id.button_ttx)
        buttonRed = view!!.findViewById(R.id.button_red)
        buttonYellow = view!!.findViewById(R.id.button_yellow)
        buttonGreen = view!!.findViewById(R.id.button_green)
        buttonBlue = view!!.findViewById(R.id.button_blue)

        button0!!.setTextLabel("0")
        button1!!.setTextLabel("1")
        button2!!.setTextLabel("2")
        button3!!.setTextLabel("3")
        button4!!.setTextLabel("4")
        button5!!.setTextLabel("5")
        button6!!.setTextLabel("6")
        button7!!.setTextLabel("7")
        button8!!.setTextLabel("8")
        button9!!.setTextLabel("9")
        button_!!.setTextLabel(".")
        buttonTTx!!.setTextLabel("TTX")

        button1?.requestFocus()

        button1!!.setOnClick { onClick(button1!!) }
        button2!!.setOnClick { onClick(button2!!) }
        button3!!.setOnClick { onClick(button3!!) }
        button4!!.setOnClick { onClick(button4!!) }
        button5!!.setOnClick { onClick(button5!!) }
        button6!!.setOnClick { onClick(button6!!) }
        button7!!.setOnClick { onClick(button7!!) }
        button8!!.setOnClick { onClick(button8!!) }
        button9!!.setOnClick { onClick(button9!!) }
        button0!!.setOnClick { onClick(button0!!) }
        button_!!.setOnClick { onClick(button_!!) }
        buttonBackspace!!.setOnClick { onClick(buttonBackspace!!) }
        buttonOk!!.setOnClick { onClick(buttonOk!!)}
        buttonTTx!!.setOnClick { onClick(buttonTTx!!)}
        buttonGreen!!.setOnClick { onClick(buttonGreen!!)}
        buttonYellow!!.setOnClick { onClick(buttonYellow!!)}
        buttonRed!!.setOnClick { onClick(buttonRed!!)}
        buttonBlue!!.setOnClick { onClick(buttonBlue!!)}

        val keyListener: View.OnKeyListener = object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                listener.onKey()
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (button1!!.hasFocus() || button4!!.hasFocus() || button7!!.hasFocus()
                            || button0!!.hasFocus() || buttonRed!!.hasFocus() || buttonYellow!!.hasFocus() || buttonTTx!!.hasFocus())
                        {
                            return true
                        }
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (button3!!.hasFocus() || button6!!.hasFocus() || button9!!.hasFocus()
                            || buttonBackspace!!.hasFocus() || buttonOk!!.hasFocus() || buttonGreen!!.hasFocus() || buttonBlue!!.hasFocus())
                        {
                            return true
                        }
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (buttonRed!!.hasFocus() || buttonGreen!!.hasFocus()) {
                            return true
                        }
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (buttonOk!!.hasFocus() || buttonTTx!!.hasFocus()) {
                            return true
                        }
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        if (buttonOk!!.hasFocus()) {
                            return false
                        }

                    }
                    if(keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9){
                        return true
                    }else if(keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
                        return true
                    }else if(keyCode >= KeyEvent.KEYCODE_PROG_RED && keyCode <= KeyEvent.KEYCODE_PROG_BLUE){
                        return true
                    }
                }
                else if(event!!.action == KeyEvent.ACTION_UP){
                    if( (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) ){
                        listener.digitPressed(keyCode - KeyEvent.KEYCODE_0)
                        return true
                    }else if((keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) ) {
                        listener.digitPressed(keyCode - KeyEvent. KEYCODE_NUMPAD_0)
                        return true
                    }else if(keyCode >= KeyEvent.KEYCODE_PROG_RED && keyCode <= KeyEvent.KEYCODE_PROG_BLUE){
                        listener.digitPressed(keyCode)
                        return true
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return true
                    }
                }
                return false
            }
        }

        button1!!.setOnKeyListener(keyListener)
        button2!!.setOnKeyListener(keyListener)
        button3!!.setOnKeyListener(keyListener)
        button4!!.setOnKeyListener(keyListener)
        button5!!.setOnKeyListener(keyListener)
        button6!!.setOnKeyListener(keyListener)
        button7!!.setOnKeyListener(keyListener)
        button8!!.setOnKeyListener(keyListener)
        button9!!.setOnKeyListener(keyListener)
        button0!!.setOnKeyListener(keyListener)
        button_!!.setOnKeyListener(keyListener)
        buttonBackspace!!.setOnKeyListener(keyListener)
        buttonOk!!.setOnKeyListener(keyListener)
        buttonTTx!!.setOnKeyListener(keyListener)
        buttonRed!!.setOnKeyListener(keyListener)
        buttonGreen!!.setOnKeyListener(keyListener)
        buttonYellow!!.setOnKeyListener(keyListener)
        buttonBlue!!.setOnKeyListener(keyListener)
    }

    fun onPause() {
        activeButton = view!!.findFocus() as CustomButton
    }

    fun onResume() {
    }

    override fun dispose() {
        stopTimer()
    }

    private var clickTime = 0L

    @RequiresApi(Build.VERSION_CODES.R)
    fun onClick(button: CustomButton){
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (listener.getCurrentTime() - clickTime < 500) {
            return
        }

        when(button.id) {
            R.id.button0 ->{
                listener.digitPressed(0)
            }
            R.id.button1 ->{
                listener.digitPressed(1)
            }
            R.id.button2 ->{
                listener.digitPressed(2)
            }
            R.id.button3 ->{
                listener.digitPressed(3)
            }
            R.id.button4 ->{
                listener.digitPressed(4)
            }
            R.id.button5 ->{
                listener.digitPressed(5)
            }
            R.id.button6 ->{
                listener.digitPressed(6)
            }
            R.id.button7 ->{
                listener.digitPressed(7)
            }
            R.id.button8 ->{
                listener.digitPressed(8)
            }
            R.id.button9 ->{
                listener.digitPressed(9)
            }
            R.id.button_ ->{
                listener.digitPressed(10)
            }
            R.id.button_ok ->{
                listener.okPressed()
            }
            R.id.button_ttx ->{
                listener.ttxPressed()
            }
            R.id.button_red ->{
                listener.redPressed()
            }
            R.id.button_blue ->{
                listener.bluePressed()
            }
            R.id.button_green ->{
                listener.greenPressed()
            }
            R.id.button_yellow ->{
                listener.yellowPressed()
            }
            R.id.button_backspace ->{
                listener.backPressed()
            }
        }
    }

    fun startTimer() {
        stopTimer()
        if(timer == null) {
            timer = object :
                CountDownTimer(
                    timerTimeout,
                    1000
                ) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    listener.timerEnd()
                }
            }
        }
        timer!!.start()
    }

    fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    interface RCUWidgetListener : GWidgetListener, TTSSetterInterface {
        fun digitPressed(digit: Int)
        fun onKey()
        fun getCurrentTime(): Long
        fun timerEnd()
        fun okPressed()
        fun ttxPressed()
        fun redPressed()
        fun bluePressed()
        fun greenPressed()
        fun yellowPressed()
        fun backPressed()
        fun ttxOn(): Boolean
    }
}