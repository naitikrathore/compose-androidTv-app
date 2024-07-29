package com.iwedia.cltv.scene.custom_recording.repeat

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import world.widget.GWidget
import world.widget.GWidgetListener


class RepeatSceneWidget (listener: RepeatSceneWidgetListener) :
    GWidget<ConstraintLayout, GWidgetListener>(0, 0, listener) {

    var daily : RadioButton?=null
    var weekly : RadioButton?=null
    var none : RadioButton?=null

    var radioGroup:RadioGroup?=null

    init {
        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_repeat, null) as ConstraintLayout

        daily = view!!.findViewById(R.id.daily)
        daily!!.setText(ConfigStringsManager.getStringById("daily"))
        weekly = view!!.findViewById(R.id.weekly)
        weekly!!.setText(ConfigStringsManager.getStringById("weekly"))
        none = view!!.findViewById(R.id.none)
        none!!.setText(ConfigStringsManager.getStringById("none"))
        radioGroup = view!!.findViewById(R.id.repeat)
        daily!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        weekly!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        none!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        radioGroup!!.setOnCheckedChangeListener { group, checkedId ->
            val checkedRadioButton = group.findViewById(checkedId) as RadioButton
            if (checkedRadioButton.isChecked) {

                daily!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                weekly!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                none!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                if (checkedRadioButton.hasFocus()) {
                    checkedRadioButton.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
                }

                checkedRadioButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.EXACTLY)
                val lp = checkedRadioButton.layoutParams
                lp.width =  checkedRadioButton.measuredWidth
                checkedRadioButton.layoutParams = lp

                if (radioGroup!!.checkedRadioButtonId == daily!!.id)
                    listener.repeatdata(1)
                else if (radioGroup!!.checkedRadioButtonId == weekly!!.id) {
                    listener.repeatdata(2)
                } else
                    listener.repeatdata(0)

            }
        }

        val focusChangeListener = object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                (v as RadioButton).setTextColor(if(hasFocus)Color.parseColor(ConfigColorManager.getColor("color_background"))else Color.parseColor(ConfigColorManager.getColor("color_main_text")) )
            }
        }

        daily!!.setOnFocusChangeListener(focusChangeListener)
        weekly!!.setOnFocusChangeListener(focusChangeListener)
        none!!.setOnFocusChangeListener(focusChangeListener)
    }

    override fun refresh(data: Any) {
        if(data is Int){
            if (data == 1){
                daily!!.isChecked = true
            }
            else if (data == 2){
                weekly!!.isChecked = true
            }
            else{
                none!!.isChecked = true
            }
        }else{
            super.refresh(data)
        }
    }
}