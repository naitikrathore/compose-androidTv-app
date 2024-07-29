package com.iwedia.cltv.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigHandler
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.manager.PvrBannerSceneManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import world.widget.GWidget
import world.widget.GWidgetListener


/**
 * PVR banner widget
 *
 * @author Dragan Krnjaic
 */
class RefereceWidgetPvrBanner :
        GWidget<ConstraintLayout, RefereceWidgetPvrBanner.PvrBannerWidgetListener> {
    var pvrBannerTimer: CountDownTimer? = null

    var channelLogo: ImageView? = null
    var channelName: TextView? = null
    var isCC:ImageView?=null


    var isHD: ImageView? = null
    var channelIndex: TextView? = null
    var currentEventName: TextView? = null
    var elapsedTime: TextView? = null
    var elapsedTimeInfo: String = "00:00"
    var totalTimeInfo: String = "00:00"
    var currentEventTime: TimeTextView? = null
    var progress: ProgressBar? = null
    var tracksContainer: LinearLayout? = null

    var context: Context? = null

    var configParam: SceneConfig? = null
    val TAG = javaClass.simpleName
    var existingTrackCodes = mutableListOf<String>()
    var isAudioTracksPresent = false
    var isSubtitleTracksPresent = false
    var trackCount = 0

    constructor(context: Context, listener: PvrBannerWidgetListener) : super(
            ReferenceWorldHandler.WidgetId.PVR_BANNER,
            ReferenceWorldHandler.WidgetId.PVR_BANNER,
            listener) {
        this.context = context

        view = LayoutInflater.from(context)
                .inflate(R.layout.layout_widget_pvr_banner, null) as ConstraintLayout

        val imageView: ImageView = view!!.findViewById(R.id.imageView)

        val drawableChannelLogo = GradientDrawable()
        drawableChannelLogo.setShape(GradientDrawable.RECTANGLE)
        drawableChannelLogo.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM)
        val drawableFadingEdgeColorStart = Color.parseColor(ConfigColorManager.getColor("color_background").replace("#",ConfigColorManager.alfa_zero_per))
        val drawableFadingEdgeColorMid = Color.parseColor(ConfigColorManager.getColor("color_background").replace("#",ConfigColorManager.alfa_fifty_per))
        val drawableFadingEdgeColorEnd = Color.parseColor(ConfigColorManager.getColor("color_background").replace("#",ConfigColorManager.alfa_hundred_per))

        drawableChannelLogo.setColors(
            intArrayOf(
                drawableFadingEdgeColorStart,
                drawableFadingEdgeColorMid,
                drawableFadingEdgeColorEnd
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                imageView!!.setBackground(drawableChannelLogo)
            }catch (E: Exception){}
        } else{
            imageView!!.setBackgroundDrawable(drawableChannelLogo)
        }

        channelLogo = view!!.findViewById(R.id.channel_logo)
        channelName = view!!.findViewById(R.id.channel_name)

        channelIndex = view!!.findViewById(R.id.channel_index)
        currentEventName = view!!.findViewById(R.id.current_event_name)

        elapsedTime = view!!.findViewById(R.id.recording_elapsed_time)
        elapsedTime!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        currentEventTime = view!!.findViewById(R.id.current_event_time)
        currentEventTime!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        isCC = view!!.findViewById(R.id.cc)
        isCC!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))


        progress = view!!.findViewById(R.id.progress)
        try {
            progress!!.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Constructor: Exception color rdb $ex")
        }
        progress!!.setProgressTintList(ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_pvr_and_other"))))

        tracksContainer = view!!.findViewById(R.id.tracksContainer)

        configParam = ConfigHandler.getSceneConfigParam(ReferenceWorldHandler.WidgetId.PVR_BANNER)
        isHD=view!!.findViewById(R.id.hd)
        isHD!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        val recIndication: ImageView = view!!.findViewById(R.id.recording_indicator)
        recIndication?.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_pvr_and_other")))

        channelIndex!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
        )

        channelIndex!!.text = ""

        currentEventName!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
        )

        currentEventName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        currentEventName!!.text = ""

        channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))


        currentEventTime!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
        )

        elapsedTime!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        currentEventTime!!.text = ""

        view!!.isClickable = true
        view!!.isFocusable = true
        view!!.requestFocus()
        startTimer()
    }

    /**
     * Start pvr banner scene count down timer
     */
    fun startTimer() {
        //Cancel timer if it's already started
        if (pvrBannerTimer != null) {
            pvrBannerTimer!!.cancel()
            pvrBannerTimer = null
        }

        setVisibility(View.VISIBLE)
        listener.setRecIndication(false)

        //Start new count down timer
        pvrBannerTimer = object :
            CountDownTimer(
                5000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                //Close pvr banner if it's still visible
                hideBanner()
            }
        }
        pvrBannerTimer!!.start()
    }

    fun hideBanner(){
        if (pvrBannerTimer != null) {
            pvrBannerTimer!!.cancel()
            pvrBannerTimer = null
        }
        setVisibility(View.GONE)
        listener.setRecIndication(true)
        listener.onTimerEnd()
    }

    fun isVisible(): Boolean {
        return view?.visibility == View.VISIBLE
    }

    private fun setVisibility(visibility: Int) {
        view?.visibility = visibility
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any) {
        super.refresh(data)

        if (data is RecordingInProgress){
            var currentRecordingProgress = 0

            val startTime = data.recordingStart
            val endTime = data.recordingEnd

            var totalDuration: Long? = null
            val  currentRecordedDuration = (data.currentRecordedDuration)
            if (endTime != null) {
                totalDuration = (endTime - startTime)

                val durationTimeString = Utils.getTimeStringFromSeconds(totalDuration/1000)

                if (durationTimeString != null) {
                    setTotalTime(durationTimeString)
                }

                val elapsedTimeString = Utils.getTimeStringFromSeconds(currentRecordedDuration/1000)

                if(elapsedTimeString != null) {
                    setElapsedTime(elapsedTimeString)
                }
            } else {
                totalDuration = 0
                //TODO - Infinity Recording check
                val refTvChannel = listener.getChannelById(data.tvChannel.id)

                currentEventName!!.text = refTvChannel!!.name
                currentEventTime!!.visibility = View.GONE

                var displayNumber = refTvChannel.displayNumber
                if (displayNumber.isDigitsOnly()) {
                    var number = displayNumber.toInt()
                    if (number < 10) {
                        channelIndex!!.text = "000" + number
                    } else if (number >= 10 && number < 100) {
                        channelIndex!!.text = "00" + number
                    } else if (number >= 100) {
                        channelIndex!!.text = "0" + number
                    }
                } else {
                    channelIndex!!.text = displayNumber
                }

                Utils.loadImage(
                    refTvChannel.logoImagePath!!,
                    channelLogo!!,
                    object : AsyncReceiver {
                        override fun onFailed(error: Error?) {
                            channelName!!.visibility = View.VISIBLE
                            channelName!!.text = refTvChannel.name
                        }

                        override fun onSuccess() {
                            channelName!!.visibility = View.GONE
                            channelName!!.text = ""
                        }
                    })
            }

            if (totalDuration > 0) {
                 elapsedTime!!.visibility = View.VISIBLE
                 currentRecordingProgress = ((currentRecordedDuration.toDouble()) / totalDuration.toDouble() * 100).toInt()
            } else {
                elapsedTime!!.visibility = View.GONE
                if (Utils.findStoragePercentage() != 0){
                    currentRecordingProgress = Utils.findStoragePercentage()
                } else {
                    currentRecordingProgress = 0
                }
            }
            progress!!.progress = currentRecordingProgress
            if(currentRecordingProgress == 100){
                worldHandler!!.playbackState = ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                PvrBannerSceneManager.previousProgress = 0L
            }

        }
    }

    override fun dispose() {
        super.dispose()
        if (pvrBannerTimer != null) {
            pvrBannerTimer!!.cancel()
            pvrBannerTimer = null
        }
    }

    interface PvrBannerWidgetListener : GWidgetListener {
        fun onTimerEnd()
        fun setRecIndication(boolean: Boolean)
        fun getChannelById(id: Int): TvChannel
    }

    fun setElapsedTime(time: String){
        if (worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING){
            elapsedTimeInfo = time
            "$elapsedTimeInfo / $totalTimeInfo".also { elapsedTime!!.text = it }
        }
    }

    fun setTotalTime(time: String){
        totalTimeInfo = time
        "$elapsedTimeInfo / $totalTimeInfo".also { elapsedTime!!.text = it }
    }
}