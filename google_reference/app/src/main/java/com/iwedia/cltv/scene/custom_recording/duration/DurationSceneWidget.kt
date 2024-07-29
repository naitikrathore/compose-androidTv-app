package com.iwedia.cltv.scene.custom_recording.duration

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import world.widget.GWidget
import world.widget.GWidgetListener
import java.lang.reflect.Field

class DurationSceneWidget (listener: DurationSceneWidgetListener) :
    GWidget<ConstraintLayout, GWidgetListener>(0, 0, listener) {
    var textView:TextView?=null
    var timePickerHours : NumberPicker? = null
    var timePickerMinutes : NumberPicker? = null
    var numberPickerEdit : EditText? =null
    var durationhrs :Array<String>?=null
    var durationmin :Array<String>?=null
    var durationMinWithoutZero :Array<String>?=null
    var durationHrsWithoutZero :Array<String>?=null
    var hrs :Int?=null
    var min :Int?=null
    var res : Resources? =null
    var isMinValueChanged = false
    var isHrsValueChanged = false
    var valueMin : String? = ""
    var valueHrs : String? = ""

    init {

        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_duration, null) as ConstraintLayout

        Log.i("Kaustubh", "KK:Layout inflated ")
        var context = ReferenceApplication.applicationContext()
        textView = view!!.findViewById(R.id.time)
        timePickerHours = view!!.findViewById(R.id.time_picker_hours)
        timePickerMinutes = view!!.findViewById(R.id.time_picker_minutes)
        durationhrs = context.resources.getStringArray(R.array.duration_hrs)
        durationmin = context.resources.getStringArray(R.array.duration_min)
        durationMinWithoutZero = context.resources.getStringArray(R.array.duration_min_no_zero)
        durationHrsWithoutZero = context.resources.getStringArray(R.array.duration_hrs_no_zero)
        textView!!.text = ConfigStringsManager.getStringById("time")
        textView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        timePickerHours!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    hrs = if(isHrsValueChanged){
                        timePickerHours!!.value + 1
                    }else{
                        timePickerHours!!.value
                    }

                    min = if (isMinValueChanged){
                        (timePickerMinutes!!.value + 1) * 5
                    } else {
                        timePickerMinutes!!.value * 5
                    }

                    Log.i("TAG", "onKey: ${hrs}  ${min}")


                    listener.getDurationdata(hrs!!,min!!)
                }
                return false
            }
        })
        Log.i("KK", "${durationhrs!!.size}: ")

        timePickerHours!!.displayedValues = durationhrs!!
        timePickerHours!!.minValue = 0
        timePickerHours!!.maxValue = durationhrs!!.size-1
        valueHrs = durationhrs!![1]

        timePickerHours?.showDividers = LinearLayout.SHOW_DIVIDER_NONE
        setNumberPickerTextColor(timePickerHours!! , Color.TRANSPARENT)
        setDividerColor(timePickerHours!!)
        numberPickerEdit = timePickerHours!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        numberPickerEdit!!.setBackgroundResource(R.drawable.pin_rounded_layout)
        numberPickerEdit!!.height = 60
        numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        requestFocus(timePickerHours!!)



        //timePicker2
        timePickerMinutes!!.displayedValues = durationmin!!
        timePickerMinutes!!.minValue = 0
        timePickerMinutes!!.maxValue = durationmin!!.size-1
        valueMin = durationmin!![timePickerMinutes!!.value]

        timePickerMinutes?.showDividers = LinearLayout.SHOW_DIVIDER_NONE
        setNumberPickerTextColor(timePickerMinutes!! , Color.TRANSPARENT)
        setDividerColor(timePickerMinutes!!)
        numberPickerEdit = timePickerMinutes!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        numberPickerEdit!!.setBackgroundResource(R.drawable.pin_rounded_layout)
        numberPickerEdit!!.height = 60
        numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        requestFocus(timePickerMinutes!!)
    }

    fun requestFocus(timePicker :NumberPicker)
    {
        var numberPickerEdit :EditText? =null
        numberPickerEdit = timePicker!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        timePicker!!.setOnFocusChangeListener{
                v,hasFocus ->

            setNumberPickerTextColor(timePicker!! , if(hasFocus)Color.parseColor(ConfigColorManager.getColor("color_main_text")) else Color.TRANSPARENT)

            numberPickerEdit!!.setBackgroundResource(if(hasFocus)R.drawable.focus_shape else R.drawable.pin_rounded_layout)
            numberPickerEdit!!.setTextColor(if(hasFocus)Color.parseColor(ConfigColorManager.getColor("color_background")) else Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            numberPickerEdit!!.visibility = View.VISIBLE

            //preventing user selection 0 duration
            if(timePickerHours!!.hasFocus() && valueMin =="0 min"){
                val oldHrsValue = valueHrs.toString()
                timePickerHours!!.displayedValues = null
                timePickerHours!!.maxValue = durationHrsWithoutZero!!.size-1
                timePickerHours!!.displayedValues = durationHrsWithoutZero!!
                isHrsValueChanged = true
                durationHrsWithoutZero!!.forEachIndexed { index, item ->
                    if (item == oldHrsValue){
                        timePickerHours!!.value = index
                    }
                }
            }
            if(timePickerHours!!.hasFocus() && valueMin!="0 min"){
                val oldHrsValue = valueHrs.toString()
                timePickerHours!!.displayedValues = null
                timePickerHours!!.maxValue = durationhrs!!.size-1
                timePickerHours!!.displayedValues = durationhrs!!
                isHrsValueChanged = false
                durationhrs!!.forEachIndexed { index, item ->
                    if (item == oldHrsValue){
                        timePickerHours!!.value = index
                    }
                }
            }
            if(timePickerMinutes!!.hasFocus() && valueHrs=="0 hr"){
                val oldMinValue = valueMin.toString()
                timePickerMinutes!!.displayedValues = null
                timePickerMinutes!!.maxValue = durationMinWithoutZero!!.size-1
                timePickerMinutes!!.displayedValues = durationMinWithoutZero
                isMinValueChanged = true
                durationMinWithoutZero!!.forEachIndexed { index, item ->
                    if (item == oldMinValue){
                        timePickerMinutes!!.value = index
                    }
                }
            }
            if(timePickerMinutes!!.hasFocus() && valueHrs!="0 hr"){
                val oldMinValue = valueMin.toString()
                timePickerMinutes!!.displayedValues = null
                timePickerMinutes!!.maxValue = durationmin!!.size-1
                timePickerMinutes!!.displayedValues = durationmin
                isMinValueChanged = false
                durationmin!!.forEachIndexed { index, item ->
                    if (item == oldMinValue){
                        timePickerMinutes!!.value = index
                    }
                }
            }

        }


        timePicker!!.setOnValueChangedListener{ picker: NumberPicker?, oldVal: Int, newVal: Int->
            numberPickerEdit!!.setBackgroundResource(R.drawable.focus_shape )
            numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
            numberPickerEdit!!.visibility = View.VISIBLE

            if(timePicker == timePickerHours){
                valueHrs = if(isHrsValueChanged){
                    durationHrsWithoutZero!![newVal]
                }else{
                    durationhrs!![newVal]
                }
            }

            if(timePicker == timePickerMinutes){
                valueMin = if(isMinValueChanged) {
                    durationMinWithoutZero!![newVal]
                }else{
                    durationmin!![newVal]
                }
            }

        }


    }
    @SuppressLint("SoonBlockedPrivateApi")
    fun setDividerColor(numberPicker: NumberPicker)

    {
        val dividerField = NumberPicker::class.java.declaredFields.firstOrNull { it.name == "mSelectionDivider" } ?: null
        try {
            val colorDrawable = ColorDrawable(ContextCompat.getColor(ReferenceApplication.applicationContext(), R.color.transparent))
            dividerField?.isAccessible = true
            dividerField?.set(numberPicker, colorDrawable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    @SuppressLint("SoonBlockedPrivateApi")
    fun setNumberPickerTextColor(numberPicker: NumberPicker, color: Int) {
        try {
            val selectorWheelPaintField: Field = numberPicker.javaClass
                .getDeclaredField("mSelectorWheelPaint")
            selectorWheelPaintField.setAccessible(true)
            (selectorWheelPaintField.get(numberPicker) as Paint).setColor(color)
        } catch (e: NoSuchFieldException) {
            Log.w("setNumberPickerTextColor", e)
        } catch (e: IllegalAccessException) {
            Log.w("setNumberPickerTextColor", e)
        } catch (e: IllegalArgumentException) {
            Log.w("setNumberPickerTextColor", e)
        }
        val count = numberPicker.childCount
        for (i in 0 until count) {
            val child: View = numberPicker.getChildAt(i)
            if (child is EditText) (child as EditText).setTextColor(color)
        }
    }}