package com.iwedia.cltv.scene.custom_recording

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.leanback.widget.VerticalGridView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.TAG
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.custom_recording.channel.CustomRecordingsChannelSceneWidget
import com.iwedia.cltv.scene.custom_recording.channel.ChannelSceneWidgetListener
import com.iwedia.cltv.scene.custom_recording.duration.DurationSceneWidget
import com.iwedia.cltv.scene.custom_recording.duration.DurationSceneWidgetListener
import com.iwedia.cltv.scene.custom_recording.repeat.RepeatSceneWidget
import com.iwedia.cltv.scene.custom_recording.repeat.RepeatSceneWidgetListener
import com.iwedia.cltv.scene.custom_recording.start.StartSceneWidget
import com.iwedia.cltv.scene.custom_recording.start.StartSceneWidgetListener
import com.iwedia.cltv.utils.InvalidDataTracker
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import listeners.AsyncReceiver
import world.SceneManager
import java.util.*


/**
 * Custom Recording Scene
 *
 * @author Kaustubh Kadam
 */
class CustomRecordingScene(context: Context, sceneListener: CustomRecordingSceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.CUSTOM_RECORDING,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.CUSTOM_RECORDING),
        sceneListener
    ) {
    private var crLeftOptionsGridView: VerticalGridView? = null
    private var crLeftOptionsAdapter: CategoryAdapter? = null
    var scheduleContainer: ConstraintLayout? = null
    var channelSceneWidget: CustomRecordingsChannelSceneWidget? = null
    var startSceneWidget: StartSceneWidget? = null
    var durationSceneWidget: DurationSceneWidget? = null
    var repeatSceneWidget: RepeatSceneWidget? = null
    var submitBtn: TextView? = null

    var tvChannel: TvChannel? = null
    var channelList = mutableListOf<TvChannel>()
    var timehrs: Int? = null
    var timemin: Int? = null
    var dateday: Int? = null
    var datemon: Int? = null
    var dateyear: Int? = null
    var durationhrs: Int? = null
    var durationmin: Int? = null
    var repeatSelected: Int? = null
    var textview1: TextView? = null
    var channelImage: ImageView? = null
    var textview2: TextView? = null
    var textview3: TextView? = null
    var textview4: TextView? = null
    var textview5: TextView? = null
    var defaultendtimeoffset = 30
    var useDefaultValues: Boolean? = null//signals whether to use default values
    var channelname: TextView? = null
    var focusPosition = 0

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(
            name,
            R.layout.layout_custom_recording,
            object : GAndroidSceneFragmentListener {

                override fun onCreated() {
                    (sceneListener as CustomRecordingSceneListener).getActiveChannel(object :IAsyncDataCallback<TvChannel>{
                        override fun onFailed(error: kotlin.Error) {
                            Log.i(TAG, " createView() -> getActiveChannel() -> onFailed() ")
                        }

                        override fun onReceive(channel: TvChannel) {
                            tvChannel = channel
                            val data = (sceneListener as CustomRecordingSceneListener).getChannelList()
                            channelList.clear()
                            data.forEach {item ->
                                channelList.add(item)
                                if(item.id == channel!!.id){
                                    focusPosition = channelList.indexOf(item)
                                }
                            }
                        }
                    })

                    var gradientBackground: View = view!!.findViewById(R.id.gradient_background)

                    val channelHeading: TextView = view!!.findViewById(R.id.channel_heading)
                    channelHeading.setText(ConfigStringsManager.getStringById("custom_recording"))
                    channelHeading.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    durationhrs = 1
                    durationmin = 0
                    repeatSelected = 0
                    useDefaultValues = true
                    var calendar = Calendar.getInstance()
                    calendar.timeInMillis = (sceneListener as CustomRecordingSceneListener).getCurrentTime(tvChannel!!) + 1800000

                    dateyear = calendar.get(Calendar.YEAR)
                    datemon = calendar.get(Calendar.MONTH) + 1
                    dateday = calendar.get(Calendar.DAY_OF_MONTH)
                    timehrs = calendar.get(Calendar.HOUR_OF_DAY)
                    timemin = calendar.get(Calendar.MINUTE)
                    useDefaultValues = true
                    Utils.makeGradient(
                        gradientBackground,
                        GradientDrawable.LINEAR_GRADIENT,
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                ConfigColorManager.getColor("color_background"),
                                0.5
                            )
                        ),
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                ConfigColorManager.getColor("color_background"),
                                0.9
                            )
                        ),
                        0.0F,
                        0.0F
                    )
                    scheduleContainer = view!!.findViewById(R.id.schedule_container)
                    crLeftOptionsGridView = view!!.findViewById(R.id.custom_recording_left_options)
                    submitBtn = view!!.findViewById(R.id.submit_btn)
                    submitBtn!!.setText(ConfigStringsManager.getStringById("schedule_recording"))
                    submitBtn!!.typeface =
                        TypeFaceProvider.getTypeFace(
                            ReferenceApplication.applicationContext(),
                            ConfigFontManager.getFont("font_regular")
                        )
                    submitBtn!!.setOnFocusChangeListener { view, hasFocus ->
                        var selectorColor = ConfigColorManager.getColor("color_selector")
                        var selectorDrawable = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.focus_shape
                        )

                        DrawableCompat.setTint(selectorDrawable!!, Color.parseColor(selectorColor))
                        if (hasFocus) {
                            submitBtn!!.background = selectorDrawable

                        } else {
                            Handler().post{
                                scheduleContainer!!.removeAllViews()
                                onShowRepeatOptions()
                            }
                            submitBtn!!.setBackgroundResource(R.drawable.bg_search_bar_rounded)

                        }


                        submitBtn!!.setTextColor(
                            if (hasFocus) Color.parseColor(
                                ConfigColorManager.getColor(
                                    "color_background"
                                )
                            ) else Color.parseColor(ConfigColorManager.getColor("color_text_description"))
                        )
                    }
                    submitBtn!!.setOnClickListener {
                        Log.i(
                            TAG,
                            "onCreated: startTime default value ${(sceneListener as CustomRecordingSceneListener).getCurrentTime(tvChannel!!) + (600000)}"
                        )
                        var startTime: Long?
                        var endTime: Long?

                        Log.i(TAG, "onCreated: {Submit button clicked}")
                        var calander: Calendar = Calendar.getInstance()
                        calander.set(dateyear!!, datemon!! -1, dateday!!, timehrs!!, timemin!!)
                        calander.set(Calendar.SECOND,0)
                        calander.set(Calendar.MILLISECOND,0)

                        startTime = calander.timeInMillis
                        Log.i(TAG, "onCreated: start time changed ${startTime}")
                        Log.i("TIMEMIN", "onCreated: ${datemon}")
                        endTime = startTime + (durationhrs!! * 3600000) + (durationmin!! * 60000)
                        Log.i(TAG, "onCreated:endtime ${endTime}")

                        val eventStartTime = startTime!!
                        val timeBeforeStartProgramme = (eventStartTime - (sceneListener as CustomRecordingSceneListener).getCurrentTime(tvChannel!!))

                        if(timeBeforeStartProgramme>0){
                            if(tvChannel == null){
                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, " Current TvChannel Not Available ")
                            }else{
                                (sceneListener as CustomRecordingSceneListener).scheduleCustomRecording(tvChannel!!
                                    ,startTime ,endTime,repeatSelected!!)
                                (sceneListener as CustomRecordingSceneListener).onBackPressed()
                                ReferenceApplication.worldHandler!!.triggerAction(id, SceneManager.Action.DESTROY)
                            }
                        }else{
                            (sceneListener as CustomRecordingSceneListener).showToast(ConfigStringsManager.getStringById("custom_recording_toast_validation_start_is_past"))
                        }

                    }
                    submitBtn!!.setOnKeyListener(object : View.OnKeyListener {
                        override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                return true
                            }
                            if(keyCode == KeyEvent.KEYCODE_DPAD_UP){
                                Handler().post {
                                    scheduleContainer!!.removeAllViews()
                                    onShowRepeatOptions()
                                }

                            }
                            return false
                        }
                    })
                    crLeftOptionsAdapter = CategoryAdapter()
                    val list = mutableListOf<CategoryItem>()
                    list.add(CategoryItem(0, ConfigStringsManager.getStringById("channel")))
                    list.add(CategoryItem(1, ConfigStringsManager.getStringById("start")))
                    list.add(CategoryItem(2, ConfigStringsManager.getStringById("duration")))
                    list.add(CategoryItem(3, ConfigStringsManager.getStringById("repeat")))


                    crLeftOptionsAdapter?.refresh(list)
                    crLeftOptionsGridView!!.setNumColumns(1)
                    crLeftOptionsGridView!!.adapter = crLeftOptionsAdapter
                    //crLeftOptionsAdapter!!.selectedItem = 1
                    crLeftOptionsGridView!!.preserveFocusAfterLayout = true



                    crLeftOptionsAdapter!!.adapterListener =
                        object : CategoryAdapter.ChannelListCategoryAdapterListener {

                            override fun getAdapterPosition(position: Int) {

                            }


                            override fun onItemSelected(position: Int) {

                            }

                            override fun digitPressed(digit: Int) {}

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as CustomRecordingSceneListener).setSpeechText(text = text, importance = importance)
                            }

                            override fun onKeyLeft(currentPosition: Int): Boolean {

                                return false
                            }

                            @RequiresApi(Build.VERSION_CODES.Q)
                            override fun onKeyRight(currentPosition: Int): Boolean {
                                if (currentPosition == 0) {

                                    channelSceneWidget!!.channelGridView?.layoutManager?.findViewByPosition(
                                        channelSceneWidget!!.focusedPosition
                                    )?.requestFocus()
                                }

                                if (currentPosition == 1) {
                                    Handler().post {

                                        startSceneWidget!!.startHours!!.requestFocus()
                                    }
                                } else if (currentPosition == 2) {
                                    Handler().post {
                                        try {
                                            durationSceneWidget!!.timePickerHours!!.requestFocus()
                                        }catch (E: Exception){
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKeyRight: ${E.printStackTrace()}")
                                        }
                                    }
                                } else if (currentPosition == 3) {
                                    Handler().post {

                                        repeatSceneWidget!!.daily!!.requestFocus()
                                    }
                                }

                                return false
                            }

                            @RequiresApi(Build.VERSION_CODES.Q)
                            override fun onKeyUp(currentPosition: Int): Boolean {

                                if (currentPosition == 0) {

                                    return true
                                } else {
                                    crLeftOptionsAdapter!!.clearPreviousFocus()
                                    crLeftOptionsAdapter!!.selectedItem = currentPosition - 1
                                    onItemClicked(currentPosition - 1)
                                }

                                return false
                            }

                            @RequiresApi(Build.VERSION_CODES.Q)
                            override fun onKeyDown(currentPosition: Int): Boolean {
                                crLeftOptionsAdapter!!.clearPreviousFocus()
                                crLeftOptionsAdapter!!.focusPosition = currentPosition
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKeyDown: ${list.size - 1}     $currentPosition")
                                if (currentPosition == list.size - 1) {
                                    Log.i(TAG, "onKeyDown: equalKK")
                                    submitBtn!!.requestFocus()
                                    scheduleContainer!!.removeAllViews()
                                    scheduleContainer!!.addView(
                                        LayoutInflater.from(ReferenceApplication.applicationContext())
                                            .inflate(
                                                R.layout.layout_schedule_recording_final_values,
                                                null
                                            ) as ConstraintLayout
                                    )
                                    Log.i(TAG, "onKeyDownk: $dateday    $datemon    $dateyear")
                                    if (dateday == 0 && datemon == 0 && dateyear == 0) {
                                        var calendar = Calendar.getInstance()
                                        calendar.timeInMillis = (sceneListener as CustomRecordingSceneListener).getCurrentTime(tvChannel!!)
                                        Log.i(
                                            TAG,
                                            "onKeyDownkkkkkkkkkkkkkkkkk: ${calendar.get(Calendar.MONTH)}"
                                        )
                                        dateyear = calendar.get(Calendar.YEAR)
                                        datemon = calendar.get(Calendar.MONTH)
                                        dateday = calendar.get(Calendar.DAY_OF_MONTH)
                                        timehrs = calendar.get(Calendar.HOUR_OF_DAY)
                                        timemin =
                                            calendar.get(Calendar.MINUTE) + defaultendtimeoffset
                                        useDefaultValues = true
                                    }

                                    Log.i(TAG, "onCreated kkkkkk: ${Calendar.DATE}")
                                    Log.i(TAG, "onCreated kkkkkk: ${Calendar.HOUR_OF_DAY}")
                                    textview1 = scheduleContainer!!.findViewById(R.id.channel_num)
                                    channelImage =
                                        scheduleContainer!!.findViewById(R.id.channel_image)
                                    channelname = scheduleContainer!!.findViewById(R.id.channelname)
                                    channelname!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    if(tvChannel != null){
                                        handleChannelNumDisplay(tvChannel!!)
                                        handleTvChannelData(tvChannel!!)
                                    }

                                    textview1 = null
                                    textview2 = null
                                    textview1 = scheduleContainer!!.findViewById(R.id.time_h1)
                                    textview1!!.setText(ConfigStringsManager.getStringById("time"))
                                    textview1!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )

                                    textview2 = scheduleContainer!!.findViewById(R.id.time_h2)
                                    textview2!!.setText(ConfigStringsManager.getStringById("time"))
                                    textview2!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    textview3 = scheduleContainer!!.findViewById(R.id.date_h1)
                                    textview3!!.setText(ConfigStringsManager.getStringById("date"))
                                    textview3!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    textview2!!.setText(ConfigStringsManager.getStringById("time"))
                                    textview1!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_light")
                                    )
                                    textview2!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_light")
                                    )
                                    textview3!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_light")
                                    )
                                    textview1 = null
                                    textview2 = null
                                    textview3 = null

                                    textview1 = scheduleContainer!!.findViewById(R.id.hour)
                                    textview2 = scheduleContainer!!.findViewById(R.id.min)
                                    textview3 = scheduleContainer!!.findViewById(R.id.day)

                                    textview4 = scheduleContainer!!.findViewById(R.id.month)
                                    textview5 = scheduleContainer!!.findViewById(R.id.year)
                                    textview1!!.text =
                                        String.format(Locale.ENGLISH, "%02d", timehrs)
                                    textview1!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    textview2!!.text =
                                        String.format(Locale.ENGLISH, "%02d", timemin)
                                    textview2!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    textview3!!.text =
                                        String.format(Locale.ENGLISH, "%02d", dateday)
                                    textview3!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    textview4!!.text =
                                        String.format(Locale.ENGLISH, "%02d", datemon)
                                    textview4!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    textview5!!.text =
                                        String.format(Locale.ENGLISH, "%04d", dateyear)
                                    textview5!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_main_text"
                                            )
                                        )
                                    )
                                    textview1!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_regular")
                                    )
                                    textview2!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_regular")
                                    )
                                    textview3!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_regular")
                                    )
                                    textview4!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_regular")
                                    )
                                    textview5!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_regular")
                                    )
                                    textview1!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )
                                    textview2!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )
                                    textview3!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )
                                    textview4!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )
                                    textview5!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )

                                    textview1 = null
                                    textview2 = null
                                    textview3 = null
                                    textview4 = null
                                    textview5 = null

                                    textview1 = scheduleContainer!!.findViewById(R.id.durationhr)
                                    textview2 = scheduleContainer!!.findViewById(R.id.durationmin)
                                    textview1!!.text = String.format(
                                        Locale.ENGLISH,
                                        "%02d",
                                        durationhrs
                                    ) + ConfigStringsManager.getStringById("hr")
                                    textview2!!.text = String.format(
                                        Locale.ENGLISH,
                                        "%02d",
                                        durationmin
                                    ) + ConfigStringsManager.getStringById("min")
                                    textview1!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )
                                    textview2!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )
                                    textview1!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_light")
                                    )
                                    textview2!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_light")
                                    )
                                    textview1 = null
                                    textview2 = null
                                    textview1 = scheduleContainer!!.findViewById(R.id.repeat)
                                    textview1!!.setText(ConfigStringsManager.getStringById("time"))
                                    textview1!!.typeface = TypeFaceProvider.getTypeFace(
                                        ReferenceApplication.applicationContext(),
                                        ConfigFontManager.getFont("work_sans_light")
                                    )
                                    if (repeatSelected == 1)
                                        textview1!!.text= ConfigStringsManager.getStringById("daily")
                                    else if (repeatSelected == 2)
                                        textview1!!.text= ConfigStringsManager.getStringById("weekly")
                                    else
                                        textview1!!.text= ConfigStringsManager.getStringById("none")
                                    textview1!!.setTextColor(
                                        Color.parseColor(
                                            ConfigColorManager.getColor(
                                                "color_text_description"
                                            )
                                        )
                                    )
                                    textview1 = null


                                    return true
                                } else {
                                    crLeftOptionsAdapter!!.selectedItem = currentPosition + 1
                                    onItemClicked(currentPosition + 1)

                                }
                                Log.d(Constants.LogTag.CLTV_TAG + "KK", "onKeyDown: kkk" + currentPosition)

                                return false
                            }

                            @RequiresApi(Build.VERSION_CODES.Q)
                            override fun onItemClicked(position: Int) {
                                Log.i(TAG, "onItemClicked: KK $position")
                                crLeftOptionsAdapter!!.clearPreviousFocus()
                                crLeftOptionsAdapter!!.focusPosition = position
                                scheduleContainer?.removeAllViews()
                                if(position != 1){
                                    crLeftOptionsAdapter!!.clearFocus(1)
                                }
                                when (position) {
                                    0 -> {
                                        crLeftOptionsAdapter!!.clearPreviousFocus()
                                        crLeftOptionsAdapter!!.keepFocus = false
                                        onShowChannels()
                                        crLeftOptionsGridView!!.post {
                                            crLeftOptionsAdapter!!.requestFocus(0)
                                        }
                                        try {
                                            channelSceneWidget!!.focusedPosition = focusPosition
                                            channelSceneWidget!!.channelAdapter!!.selectedItem =
                                                focusPosition

                                        } catch (E: Exception) {
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onItemClicked: ${E.printStackTrace()}")
                                        }

                                        return
                                    }
                                    1 -> {
                                        Log.i(TAG, "onItemClicked: START clicked")
                                        crLeftOptionsAdapter!!.clearPreviousFocus()
                                        onShowStartOptions()
                                        crLeftOptionsGridView!!.post {
                                            crLeftOptionsAdapter!!.requestFocus(1)
                                        }
                                        try{
                                            startSceneWidget!!.startHours!!.setValue(timehrs!!)
                                            startSceneWidget!!.startMinutes!!.setValue(timemin!!)
                                            startSceneWidget!!.datePicker1!!.setValue(dateday!!)
                                            startSceneWidget!!.datePicker2!!.setValue(datemon!!)
                                            startSceneWidget!!.datePicker3!!.setValue(dateyear!!)
                                        }
                                        catch (E: Exception){
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onItemClicked: ${E.printStackTrace()}")
                                        }



                                        return
                                    }
                                    2 -> {
                                        crLeftOptionsAdapter!!.clearPreviousFocus()
                                        onShowDurationOptions()
                                        crLeftOptionsGridView!!.post {
                                            crLeftOptionsGridView!!.layoutManager?.findViewByPosition(
                                                2
                                            )?.requestFocus()
                                        }
                                        try {
                                            durationSceneWidget!!.timePickerHours!!.setValue(durationhrs!!)
                                            durationSceneWidget!!.timePickerMinutes!!.setValue(durationmin!!/5)
                                        }catch (E: Exception){
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onItemClicked: ${E.printStackTrace()}")
                                        }
                                    }
                                    3 -> {
                                        Log.i(TAG, "onItemClicked: fourth clicked")
                                        crLeftOptionsAdapter!!.clearPreviousFocus()
                                        onShowRepeatOptions()
                                        crLeftOptionsGridView!!.post {
                                            crLeftOptionsGridView!!.layoutManager?.findViewByPosition(
                                                3
                                            )?.requestFocus()
                                        }
                                        try{
                                            if (repeatSelected == 1){
                                                repeatSceneWidget!!.daily!!.requestFocus()
                                            }
                                            else if (repeatSelected == 2){
                                                repeatSceneWidget!!.weekly!!.requestFocus()
                                            }
                                            else{
                                                repeatSceneWidget!!.none!!.requestFocus()
                                            }
                                        }catch (E: Exception){
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onItemClicked: ${E.printStackTrace()}")
                                        }
                                        return
                                    }
                                }

                            }

                            override fun onBackPressed(position: Int): Boolean {
                                return false
                            }
                        }
                    crLeftOptionsGridView?.requestFocus()
                    // ReferenceApplication.worldHandler?.isEnableUserInteraction = true

                    onShowChannels()
                    sceneListener.onSceneInitialized()
                    crLeftOptionsGridView!!.post {
                        crLeftOptionsGridView!!.layoutManager?.findViewByPosition(0)?.requestFocus()
                    }

                }

            })

    }

    private fun handleChannelNumDisplay(tvChannel: TvChannel) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, " handleChannelNumDisplay() : channel number = ${tvChannel?.displayNumber} ")
        textview1!!.text = tvChannel.getDisplayNumberText()
        textview1!!.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    "color_main_text"
                )
            )
        )
        textview1?.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("work_sans_regular")
        )
    }

    private fun handleTvChannelData(tvChannel: TvChannel) {
        if (InvalidDataTracker.hasValidData(tvChannel!!)) {
            Utils.loadImage(
                tvChannel.logoImagePath!!,
                channelImage!!,
                object : AsyncReceiver {
                    override fun onFailed(error: core_entities.Error?) {
                        InvalidDataTracker.setInvalidData(tvChannel!!)
                        channelname!!.visibility = View.VISIBLE
                        channelname!!.text = tvChannel!!.name
                    }

                    override fun onSuccess() {
                        channelname!!.visibility = View.GONE
                        channelname!!.text = ""
                    }
                })
        } else {
            channelImage!!.visibility = View.GONE
            channelname!!.visibility = View.VISIBLE
            channelname!!.text = tvChannel!!.name
            channelname!!.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor("color_main_text")
                )
            )
        }
    }

    fun onShowChannels() {
        channelSceneWidget =
            CustomRecordingsChannelSceneWidget(object : ChannelSceneWidgetListener {

                override fun setFocusPosition(position: Int) {
                    focusPosition = position
                    Log.i(TAG, "getFocusPosition: CR scene $focusPosition")
                }

                override fun getFocusPosition(): Int {
                    Log.i(TAG, "setFocusPosition: returning focusPosition${focusPosition}")
                    return focusPosition
                }

                override fun onChannelItemClicked(position: Int) {
                     tvChannel = channelList[position]
                }

                override fun onLeftClicked() {
                    crLeftOptionsGridView!!.getChildAt(0).requestFocus()
                }


                override fun dispatchKey(
                    keyCode: Int,
                    event: Any
                ): Boolean {
                    return false
                }


            })

        scheduleContainer?.addView(channelSceneWidget?.view)
        channelSceneWidget?.refresh(channelList)

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onShowStartOptions() {
        Log.i(TAG, "startclicked: ItemClick Received")
        scheduleContainer?.removeAllViews()
        startSceneWidget = StartSceneWidget(object : StartSceneWidgetListener {

            override fun getCurrentTime(): Long {
                return (sceneListener as CustomRecordingSceneListener).getCurrentTime(tvChannel!!)
            }

            override fun gettimedata(hr: Int, min: Int, day: Int, month: Int, year: Int) {
                Log.i(TAG, "gettimedata: ${hr},${min},${day},${month},${year}}")
                timehrs = hr
                timemin = min
                dateday = day
                datemon = month
                dateyear = year

            }

            override fun dispatchKey(
                keyCode: Int,
                event: Any
            ): Boolean {
                Log.i(TAG, "onShowStartOptions -> dispatchKey")
                return false
            }
        })
        scheduleContainer?.addView(startSceneWidget?.view)
    }

    fun onShowDurationOptions() {
        durationSceneWidget = DurationSceneWidget(object : DurationSceneWidgetListener {
            override fun getDurationdata(hours: Int, minutes: Int) {
                durationhrs = hours
                durationmin = minutes
            }

            override fun dispatchKey(
                keyCode: Int,
                event: Any
            ): Boolean {
                Log.i(TAG, "onShowDurationOptions -> dispatchKey")
                return false
            }
        })
        scheduleContainer?.addView(durationSceneWidget?.view)

    }

    fun onShowRepeatOptions() {
        repeatSceneWidget = RepeatSceneWidget(object : RepeatSceneWidgetListener {
            override fun repeatdata(repeat: Int) {
                repeatSelected = repeat
            }

            override fun dispatchKey(
                keyCode: Int,
                event: Any
            ): Boolean {
                Log.i(TAG, "onShowRepeatOptions -> dispatchKey")
                return false
            }
        })
        repeatSceneWidget!!.refresh(repeatSelected!!)
        scheduleContainer?.addView(repeatSceneWidget?.view)

    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && scheduleContainer!!.hasFocus()) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {

                crLeftOptionsGridView?.requestFocus()
                return true
            }
            return true
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }
}