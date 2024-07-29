package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.manager.ZapDigitManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import world.widget.GWidget


/**
 * Zap banner widget
 * @author Veljko Ilic
 */
class ReferenceWidgetZapDigit :
    GWidget<ConstraintLayout, ReferenceWidgetZapDigit.GZapDigitListener> {

    var zapTimer: CountDownTimer? = null
//    var channelLogo: ImageView? = null
//    var channelName: TextView? = null

    var channelsGridView: VerticalGridView? = null

    var channelIndex: TextView? = null

    var context: Context? = null

    var activeDigit = ""

    var maxDigitNumber: ZapDigitManager.ZapDigitMaxNumber? = null

    var timerTimeout = 3000L

    var timerTimeoutLonger = 5000L

    var zapDigitChannelsAdapter: ZapDigitChannelsAdapter? = null

    var channelsList = mutableListOf<TvChannel>()

    constructor(context: Context, listener: GZapDigitListener) : super(999, 999, listener) {
        this.context = context

        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_zap_digit, null) as ConstraintLayout


        val imageView: ImageView = view!!.findViewById(R.id.imageView)

        val drawableImageView = GradientDrawable()
        drawableImageView.setShape(GradientDrawable.RECTANGLE)
        drawableImageView.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT)
        val drawableFadingEdgeColorStart = Color.parseColor(
            ConfigColorManager.getColor("color_background").replace("#",
                ConfigColorManager.alfa_hundred_per))
        val drawableFadingEdgeColorMid = Color.parseColor("#B3000000")
        val drawableFadingEdgeColorEnd = Color.parseColor(
            ConfigColorManager.getColor("color_background").replace("#",
                ConfigColorManager.alfa_zero_per))
        drawableImageView.setColors(
            intArrayOf(
                drawableFadingEdgeColorStart,
                drawableFadingEdgeColorMid,
                drawableFadingEdgeColorEnd
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imageView!!.setBackground(drawableImageView)
        } else{
            imageView!!.setBackgroundDrawable(drawableImageView)
        }

        imageView.translationZ = -10f

//        channelLogo = view!!.findViewById(R.id.channel_logo)
//        channelName = view!!.findViewById(R.id.channel_name)
        channelIndex = view!!.findViewById(R.id.channel_index)

        channelsGridView =  view!!.findViewById(R.id.widget_zap_digit_channels_grid_view)
        channelsGridView!!.setNumColumns(1)


        //fonts
//        channelName!!.typeface = TypeFaceProvider.getTypeFace(
//            ReferenceApplication.applicationContext(),
//            ConfigFontManager.getFont("font_regular")
//        )
//        channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        channelIndex!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        view!!.isClickable = true
        view!!.isFocusable = true

        view!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event!!.action == KeyEvent.ACTION_DOWN) {

                    if (ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.RCU_SCENE)) {
                        return false
                    }

                    when (event.keyCode) {
                        KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                            val digit = keyCode - KeyEvent.KEYCODE_0
                            listener.digitPressed(digit)
                            return true
                        }

                        KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {
                            val digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                            listener.digitPressed(digit)
                            return true
                        }
                        KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_SLASH,KeyEvent.KEYCODE_MINUS -> {
                            listener.onPeriodPressed()
                            return true
                        }
                    }
                }

                if (event!!.action == KeyEvent.ACTION_UP) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            listener.timerEnd()
                            return true
                        }
                    }
                }
                return false
            }

        })

        zapDigitChannelsAdapter = ZapDigitChannelsAdapter()

        zapDigitChannelsAdapter!!.zapDigitChannelsAdapterListener =
            object : ZapDigitChannelsAdapter.ZapDigitChannelsAdapterListener {
                override fun getItem(itemId: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    listener.channelClicked(itemId)
                }

                override fun onKeyUp(position: Int): Boolean {
                    return false
                }

                override fun onKeyDown(position: Int): Boolean {
                    return false
                }

                override fun onKey() {
                    if(channelsList.isNotEmpty() && channelsList.size > 1){
                        //start timer every time key is pressed to track inactivity when channel
                        //list has more than one item
                        startTimer()
                    }
                }

                override fun getChannelSourceType(item: TvChannel): String {
                    return listener.getChannelSourceType(item)
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

            }
        channelsGridView!!.adapter = zapDigitChannelsAdapter

    }

    private fun resetViews() {
//        channelName!!.text = ""
//        channelIndex!!.text = ""
//        channelLogo!!.setImageDrawable(null)
    }

    /**
     * Start zap banner scene count down timer
     */
    fun startTimer() {
        //timer for channel list with one item or for empty channel list
        if((channelsList.isNotEmpty() && channelsList.size == 1) || channelsList.isEmpty()){
            if (zapTimer != null) {
                zapTimer!!.cancel()
                zapTimer = null
                zapTimer = object :
                    CountDownTimer(
                        timerTimeout,
                        1000
                    ) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        if(channelsList.size == 1){
                            var channel = channelsList.get(0)
                            listener.timerEndZap(channel.id)
                        }
                        else if(channelsList.isEmpty()){
                            listener.timerEnd()
                        }
                    }
                }
            }else{
                //Start new count down timer
                zapTimer = object :
                    CountDownTimer(
                        timerTimeout,
                        1000
                    ) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        if(channelsList.size == 1){
                            var channel = channelsList.get(0)
                            listener.timerEndZap(channel.id)
                        }
                        else if(channelsList.isEmpty()){
                            listener.timerEnd()
                        }

                    }
                }
            }
        }
        //timer for channel list with more than one item
        else if (channelsList.isNotEmpty() && channelsList.size > 1){
            if (zapTimer != null) {
                zapTimer!!.cancel()
                zapTimer = null
                zapTimer = object :
                    CountDownTimer(
                        timerTimeoutLonger,
                        1000
                    ) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        val channel = channelsList[zapDigitChannelsAdapter!!.adapterPosition]
                        listener.timerEndZap(channel.id)
                        if(channelsList.isEmpty()){
                            listener.timerEnd()
                        }
                    }
                }
            }else{
                //Start new count down timer
                zapTimer = object :
                    CountDownTimer(
                        timerTimeoutLonger,
                        1000
                    ) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        val channel = channelsList[zapDigitChannelsAdapter!!.adapterPosition]
                        listener.timerEndZap(channel.id)
                        if(channelsList.isEmpty()){
                        listener.timerEnd()
                    }
                    }
                }
            }
        }
        zapTimer!!.start()
    }

    override fun refresh(data: Any) {
        super.refresh(data)

        if (data is ZapDigitManager.ZapDigitMaxNumber) {
            this.maxDigitNumber = data
        }

        if(data is MutableList<*>){
            channelsList.clear()
            data.forEach { tvChannel ->
                if (tvChannel is TvChannel){
                    channelsList.add(tvChannel)
                }
            }

            runOnUiThread {
                zapDigitChannelsAdapter!!.refresh(channelsList)
                startTimer()
            }

            if (!ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.RCU_SCENE)) {
                channelsGridView!!.post {
                    channelsGridView!!.scrollToPosition(0)
                    channelsGridView!!.requestFocus()
                }
            }
            //when channel list items are refreshed start timer

        }

        if (data is TvChannel) {
            val tvChannel = data

            if (tvChannel.id == -1) {
//                channelLogo!!.setImageDrawable(null)
//                channelName!!.visibility = View.INVISIBLE
//                channelName!!.text = ""
                return
            }
//            Utils.loadImage(
//                tvChannel.logoImagePath!!,
//                channelLogo!!,
//                object : AsyncReceiver {
//                    override fun onFailed(error: Error?) {
////                        channelName!!.visibility = View.VISIBLE
////                        channelName!!.text = tvChannel.name
////                        channelLogo!!.setImageDrawable(null)
////                        channelName!!.typeface = TypeFaceProvider.getTypeFace(
////                            ReferenceApplication.applicationContext(),
////                            ConfigFontManager.getFont("font_regular")
////                        )
//                    }
//
//                    override fun onSuccess() {
////                        channelName!!.visibility = View.INVISIBLE
////                        channelName!!.text = ""
//                    }
//
//                })

            channelsList.clear()
            channelsList.add(tvChannel)

            zapDigitChannelsAdapter!!.refresh(channelsList)
            if (!ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.RCU_SCENE)) {
                 channelsGridView!!.requestFocus()
            }


        }

        if (data is String) {
            channelIndex!!.text = data
            activeDigit = data
        }
    }

    fun restoreFocus() {
        channelsGridView!!.scrollToPosition(0)
        channelsGridView!!.requestFocus()
    }

    fun requestFocus() {
        channelsGridView!!.requestFocus()
    }

    override fun dispose() {
        super.dispose()

        activeDigit = ""

        if (zapTimer != null) {
            zapTimer!!.cancel()
            zapTimer = null
        }
    }

    interface GZapDigitListener: TTSSetterInterface {
        fun timerEnd()

        fun timerEndZap(itemId: Int)

        /**
         * Digit pressed
         */
        fun digitPressed(digit: Int)

        fun channelClicked(itemId: Int)

        fun onPeriodPressed()
        fun getChannelSourceType(item: TvChannel) : String
    }
}