package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.parental_control.EnterPinAdapter
import com.iwedia.cltv.scene.parental_control.PinItem
import com.iwedia.guide.android.widgets.helpers.BaseLinearLayoutManager
import world.widget.GWidget
import world.widget.GWidgetListener

class CamPinWidget :
    GWidget<ConstraintLayout, CamPinWidget.CamPinWidgetListener> {

    lateinit var title: TextView
    var pressOk: TextView? = null
    lateinit var pinAdapter: EnterPinAdapter
    lateinit var pinRecycler: RecyclerView
    var currentPinPosition = -1
    private val NUMBER_OF_PIN_ITEMS = 4

    val PIN_NAME = "cam_code"
    val PIN_VALUE = "0000"

    @SuppressLint("SetTextI18n")
    constructor(context: Context, listener: CamPinWidget.CamPinWidgetListener) : super(
        ReferenceWorldHandler.WidgetId.CAM_PIN,
        ReferenceWorldHandler.WidgetId.CAM_PIN,
        listener
    ) {

        findRefs(context)
    }

    @SuppressLint("SetTextI18n")
    private fun findRefs(context: Context) {
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_cam_pin_widget, null) as ConstraintLayout

        val channel_list_bg: ConstraintLayout = view!!.findViewById(R.id.channel_list_bg)
        val bg =
            ConfigColorManager.getColor("color_background").replace("#", "#CC") //color_background

        channel_list_bg.setBackgroundColor(Color.parseColor(bg))


        title = view!!.findViewById(R.id.title)
        title.setText("Please enter cam pin code")
        title.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        title!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        //setup second title
        pressOk = view!!.findViewById(R.id.pressOkTv)
        pressOk!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        pressOk!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        pressOk!!.text = ConfigStringsManager.getStringById("press_ok_to_confirm")

        pinAdapter = EnterPinAdapter(getPinItems(NUMBER_OF_PIN_ITEMS)!!)

        //setup recycler and adapter
        pinRecycler = view!!.findViewById(R.id.new_pin_items)
        pinRecycler!!.visibility = View.VISIBLE
        val layoutManager = BaseLinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        pinRecycler.layoutManager = layoutManager
        pinRecycler.adapter = pinAdapter

        pinAdapter.registerListener(object : EnterPinAdapter.EnterPinListener {
            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener.setSpeechText(text = text, importance = importance)
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onPinConfirmed(pinCode: String?) {
                if (pinCode != null) {
                    listener.setCamPin(pinCode)
                    listener.onBackPress()
                }
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
                pinRecycler.getChildAt(currentPinPosition).requestFocus()
            }

            override fun validationEnabled() {
            }

            override fun isAccessibilityEnabled(): Boolean {
                return false
            }

        })

        pinRecycler.requestFocus()
    }

    private fun getPinItems(numberOfItems: Int): MutableList<PinItem>? {
        val pinItems = mutableListOf<PinItem>()
        for (i in 0 until numberOfItems) {
            pinItems.add(PinItem(i, PinItem.TYPE_PASSWORD))
        }
        return pinItems
    }

    fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?) {

    }

    interface CamPinWidgetListener : GWidgetListener, TTSSetterInterface {
        fun setCamPin(pin: String)
        fun onBackPress()
    }
}