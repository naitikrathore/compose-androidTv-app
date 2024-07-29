package com.iwedia.cltv.scene.custom_recording.start

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import world.widget.GWidget
import world.widget.GWidgetListener
import java.lang.reflect.Field

@RequiresApi(Build.VERSION_CODES.Q)
class StartSceneWidget (listener: StartSceneWidgetListener) :
    GWidget<ConstraintLayout, GWidgetListener>(0, 0, listener) {
    var textView:TextView?=null
    var startHours : NumberPicker? = null
    var startMinutes : NumberPicker? = null
    var datePicker1 : NumberPicker? = null
    var datePicker2 : NumberPicker? = null
    var datePicker3 : NumberPicker? = null
    var numberPickerEdit : EditText? =null

    var selectedCalendar = Calendar.getInstance()


    init {
        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_start, null) as ConstraintLayout
        Log.i("Kaustubh", "KK:Layout inflated ")
        textView = view!!.findViewById(R.id.time)
        textView!!.setText(ConfigStringsManager.getStringById("time"))
        textView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        textView = view!!.findViewById(R.id.date)
        textView!!.setText(ConfigStringsManager.getStringById("date"))
        textView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        startHours = view!!.findViewById(R.id.time_picker_hours)
        startMinutes = view!!.findViewById(R.id.time_picker_minutes)
        datePicker1 = view!!.findViewById(R.id.date_picker1)
        datePicker2 = view!!.findViewById(R.id.date_picker2)
        datePicker3 = view!!.findViewById(R.id.date_picker3)
        var calendar = Calendar.getInstance()
        calendar.timeInMillis=listener.getCurrentTime()+1800000

        Log.i("Kaustubh", ": day ${calendar.get(Calendar.DAY_OF_MONTH)}")
        startHours!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    val timehrs= selectedCalendar.get(Calendar.HOUR_OF_DAY)
                    val timemin =  selectedCalendar.get(Calendar.MINUTE)
                    val dateday= selectedCalendar.get(Calendar.DAY_OF_MONTH)
                    val datemon = selectedCalendar.get(Calendar.MONTH)+1
                    val dateyear = selectedCalendar.get(Calendar.YEAR)

                    Log.i("TAG", "onKey: ${timehrs}   ${timemin}   ${dateday} ${datemon}  ${dateyear}")
                    listener .gettimedata(timehrs,timemin,dateday,datemon,dateyear)
                }
                return false
            }
        })


        startHours?.setMinValue(0)
        startHours?.setMaxValue(23)
        startHours?.value= selectedCalendar.get(Calendar.HOUR_OF_DAY)
        startHours!!.setFormatter( { i -> String.format("%02d", i) })
        startHours?.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE)
        setNumberPickerTextColor(startHours!! , Color.TRANSPARENT)
        setDividerColor(startHours!!)
        numberPickerEdit = startHours!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        numberPickerEdit!!.setBackgroundResource(R.drawable.pin_rounded_layout)
        numberPickerEdit!!.setHeight(60)
        numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))


        requestFocus(startHours!!)


        //timePicker2
        startMinutes?.setMinValue(0)
        startMinutes?.setMaxValue(59)
        startMinutes?.value= selectedCalendar.get(Calendar.MINUTE)

        startMinutes!!.setFormatter( { i -> String.format("%02d", i) })

        startMinutes?.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE)
        setNumberPickerTextColor(startMinutes!! , Color.TRANSPARENT)
        setDividerColor(startMinutes!!)
        numberPickerEdit = startMinutes!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        numberPickerEdit!!.setBackgroundResource(R.drawable.pin_rounded_layout)
        numberPickerEdit!!.setHeight(60)
        numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        requestFocus(startMinutes!!)




        //datePicker1
        datePicker1?.setMinValue(1)
        datePicker1!!.setMaxValue(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        datePicker1!!.value=selectedCalendar.get(Calendar.DAY_OF_MONTH)
        Log.i("Kaustubh", "dp${datePicker1?.value}: ")
        datePicker1!!.setFormatter( { i -> String.format("%02d", i) })

        datePicker1?.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE)
        setNumberPickerTextColor(datePicker1!! , Color.TRANSPARENT)
        setDividerColor(datePicker1!!)
        numberPickerEdit = datePicker1!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        numberPickerEdit!!.setBackgroundResource(R.drawable.pin_rounded_layout)
        numberPickerEdit!!.setHeight(60)
        numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        requestFocus(datePicker1!!)

        //datePicker2
        datePicker2?.setMinValue(1)
        datePicker2?.setMaxValue(12)
        datePicker2!!.value = selectedCalendar.get(Calendar.MONTH)+1

        datePicker2!!.setFormatter( { i -> String.format("%02d", i) })

        datePicker2?.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE)
        setNumberPickerTextColor(datePicker2!! , Color.TRANSPARENT)
        setDividerColor(datePicker2!!)
        numberPickerEdit = datePicker2!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        numberPickerEdit!!.setBackgroundResource(R.drawable.pin_rounded_layout)
        numberPickerEdit!!.setHeight(60)
        numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        requestFocus(datePicker2!!)

        //datePicker3
        datePicker3!!.setMinValue(calendar.get(Calendar.YEAR))
        datePicker3!!.setMaxValue(calendar.get(Calendar.YEAR)+3)
        datePicker3!!.value = selectedCalendar.get(Calendar.YEAR)

        datePicker3?.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE)
        setNumberPickerTextColor(datePicker3!! , Color.TRANSPARENT)
        setDividerColor(datePicker3!!)
        numberPickerEdit = datePicker3!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        numberPickerEdit!!.setBackgroundResource(R.drawable.pin_rounded_layout)
        numberPickerEdit!!.setHeight(60)
        numberPickerEdit!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        requestFocus(datePicker3!!)
    }
    fun requestFocus(timePicker :NumberPicker)
    {
        Log.i("Kaustubh", "requestfocus: FocusRequest")

        var numberPickerEdit :EditText? =null
        numberPickerEdit = timePicker!!.findViewById(Resources.getSystem().getIdentifier("numberpicker_input", "id", "android"))
        timePicker!!.setOnFocusChangeListener{
                v,hasFocus ->

            setNumberPickerTextColor(timePicker!! , if(hasFocus)Color.parseColor(ConfigColorManager.getColor("color_main_text")) else Color.TRANSPARENT)

            numberPickerEdit!!.setBackgroundResource(if(hasFocus)R.drawable.focus_shape else R.drawable.pin_rounded_layout)
            numberPickerEdit!!.setTextColor(if(hasFocus)Color.parseColor(ConfigColorManager.getColor("color_background")) else Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            numberPickerEdit!!.visibility = View.VISIBLE

        }

        timePicker.setOnValueChangedListener{picker: NumberPicker?, oldVal: Int, newVal: Int->

            numberPickerEdit!!.setBackgroundResource(R.drawable.focus_shape )
            numberPickerEdit!!.setTextColor( Color.parseColor(ConfigColorManager.getColor("color_background")))
            numberPickerEdit!!.visibility = View.VISIBLE

            when(picker!!.id ){
                R.id.time_picker_hours -> selectedCalendar.set(Calendar.HOUR_OF_DAY,newVal)
                R.id.time_picker_minutes -> selectedCalendar.set(Calendar.MINUTE,newVal)
                R.id.date_picker1 -> selectedCalendar.set(Calendar.DAY_OF_MONTH,newVal)
                R.id.date_picker2 -> {
                    selectedCalendar.set(Calendar.MONTH,newVal-1)
                    datePicker1!!.setMaxValue(selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    selectedCalendar.set(Calendar.DAY_OF_MONTH,datePicker1!!.value)
                }
                R.id.date_picker3 -> selectedCalendar.set(Calendar.YEAR,newVal)
            }

            val timehrs= selectedCalendar.get(Calendar.HOUR_OF_DAY)
            val timemin =  selectedCalendar.get(Calendar.MINUTE)
            val dateday= selectedCalendar.get(Calendar.DAY_OF_MONTH)
            val datemon = selectedCalendar.get(Calendar.MONTH)+1
            val dateyear = selectedCalendar.get(Calendar.YEAR)
            Log.i("TAG", "onKey: ${timehrs} : ${timemin} /  ${dateday} -${datemon} - ${dateyear}")
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
        numberPicker.invalidate()
    }
}