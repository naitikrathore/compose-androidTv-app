package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvTrackInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.scene.zap_banner_scene.CustomGzapBannerListener
import com.iwedia.cltv.utils.Utils
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.widget.custom.zap_banner.GZapBanner
import world.widget.custom.zap_banner.GZapBannerListener


/**
 * Zap banner widget
 * @author Veljko Ilic
 */
class ReferenceWidgetZapBanner :
    GZapBanner<ConstraintLayout, GZapBannerListener> {

    var zapTimer: CountDownTimer? = null
    var channelLogo: ImageView? = null
    var channelName: TextView? = null

    //Channel type
    var channelType: TextView? = null

    var customDetails: CustomDetails.CustomDetailsZapBanner
    var channelIndex: TextView? = null
    var tracksContainer: LinearLayout? = null
    var scrambledImageView : ImageView? = null
//    var parentalrating: TextView? = null // todo parental should be handled inside CustomDetailsZapBanner
    private var longPressFlag = false
    var context: Context? = null

    var configParam: SceneConfig? = null

    var lastKeyPressedTime = 0L
    var lastKeyUpPressedTime = 0L

    val TAG = javaClass.simpleName

    @RequiresApi(Build.VERSION_CODES.R)
    constructor(context: Context, listener: GZapBannerListener) : super(listener) {
        this.context = context
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_zap_banner, null) as ConstraintLayout
        val imageView: ImageView = view!!.findViewById(R.id.imageView)

        customDetails = view!!.findViewById(R.id.custom_details)

        val drawableImageView = GradientDrawable()
        drawableImageView.setShape(GradientDrawable.RECTANGLE)
        drawableImageView.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM)
        val drawableFadingEdgeColorStart = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_zero_per)
        )
        val drawableFadingEdgeColorMid = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_sixty_per)
        )
        val drawableFadingEdgeColorEnd = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_hundred_per)
        )
        drawableImageView.setColors(
            intArrayOf(
                drawableFadingEdgeColorStart,
                drawableFadingEdgeColorMid,
                drawableFadingEdgeColorEnd
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imageView!!.setBackground(drawableImageView)
        } else {
            imageView!!.setBackgroundDrawable(drawableImageView)
        }

        channelLogo = view!!.findViewById(R.id.channel_logo)
        channelName = view!!.findViewById(R.id.channel_name)

        channelType = view!!.findViewById(R.id.channel_type)
        channelType!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        channelType!!.typeface = TypeFaceProvider.getTypeFace(context!!, ConfigFontManager.getFont("font_regular"))

        channelIndex = view!!.findViewById(R.id.channel_index)
        scrambledImageView = view!!.findViewById(R.id.zap_is_scrambled)

        tracksContainer = view!!.findViewById(R.id.tracksContainer)
//        parentalrating = view!!.findViewById(R.id.parental_rating) // todo parental should be handled inside CustomDetailsZapBanner

        configParam = ConfigHandler.getSceneConfigParam(ReferenceWorldHandler.WidgetId.ZAP_BANNER)

        //fonts
        channelName!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_bold")
        )

        channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        channelIndex!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        view!!.isClickable = true
        view!!.isFocusable = true
        view!!.requestFocus()

        startTimer()

    }

    fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        var event = keyEvent as KeyEvent
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                if (worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.PLAYER_SCENE) {
                    if(event.isLongPress || longPressFlag) {
                        if (event.eventTime - lastKeyPressedTime > 200) {
                            lastKeyPressedTime = event.eventTime
                            longPressFlag = true
                            (listener as CustomGzapBannerListener).onNextChannelInfoBannerHold()
                            resetViews()
                            return true
                        }
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                if (worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.PLAYER_SCENE) {
                    if (event.isLongPress || longPressFlag) {
                        if (event.eventTime - lastKeyPressedTime > 200) {
                            lastKeyPressedTime = event.eventTime
                            longPressFlag = true
                            (listener as CustomGzapBannerListener).onPreviousChannelInfoBannerHold()
                            resetViews()
                            return true
                        }
                    }
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                resetViews()
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                resetViews()
            }
        }
        else if(event.action == KeyEvent.ACTION_UP){
            if (worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.PLAYER_SCENE) {
                lastKeyPressedTime = 0L
                if (longPressFlag) {
                    longPressFlag = false
                    if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                        resetViews()
                        (listener as CustomGzapBannerListener).onNextChannelZapHold()
                        return true
                    } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                        resetViews()
                        (listener as CustomGzapBannerListener).onPreviousZapHold()
                        return true
                    }
                } else {
                    if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                        // Reason to add followed condition "worldHandler?.isEnableUserInteraction == true" is to prevent
                        // user interaction while EAS message is showing, which was happening on global key pressed.
                        if (event.eventTime - lastKeyUpPressedTime > 500 && worldHandler?.isEnableUserInteraction == true) {
                            lastKeyUpPressedTime = event.eventTime
                            resetViews()
                            listener.onNextChannelClicked()
                        }
                        return true
                    } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                        // Reason to add followed condition "worldHandler?.isEnableUserInteraction == true" is to prevent
                        // user interaction while EAS message is showing, which was happening on global key pressed.
                        if (event.eventTime - lastKeyUpPressedTime > 500 && worldHandler?.isEnableUserInteraction == true) {
                            lastKeyUpPressedTime = event.eventTime
                            resetViews()
                            listener.onPreviousChannelClicked()
                        }
                        return true
                    }
                    //RCU PREVIOUS CHANNEL
                    else if (keyCode == KeyEvent.KEYCODE_11) {
                        resetViews()
                        (listener as CustomGzapBannerListener).onLastActiveChannelClicked()
                        return true
                    }else if (keyCode == KeyEvent.KEYCODE_LAST_CHANNEL) {
                        resetViews()
                        (listener as CustomGzapBannerListener).onLastActiveChannelClicked()
                        return true
                    }
                }
                if (worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.ZAP_BANNER) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT ->
                            (listener as CustomGzapBannerListener).showInfoBanner()

                        KeyEvent.KEYCODE_DPAD_LEFT ->
                            (listener as CustomGzapBannerListener).showInfoBanner()

                        KeyEvent.KEYCODE_BACK -> {
                            listener.onTimerEnd()
                            return true
                        }
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            (listener as CustomGzapBannerListener).showChannelList()
                        }
                    }
                }
            }
        }
        return false
    }

    private fun resetViews() {
        channelName!!.text = ""
        channelIndex!!.text = ""
        channelLogo!!.setImageDrawable(null)
        scrambledImageView!!.visibility =View.GONE

        val textSize =
            ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12)
        channelName!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }

    /**
     * Start zap banner scene count down timer
     */
    private fun startTimer() {

        //Cancel timer if it's already started
        if (zapTimer != null) {
            zapTimer!!.cancel()
            zapTimer = null
        }

        //Start new count down timer
        zapTimer = object :
            CountDownTimer(
                6000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                //Close zap banner if it's still visible
                listener.onTimerEnd()
                InformationBus.submitEvent(Event(Events.TALKBACK_CLOSE_ZAP_BANNER))
            }
        }
        zapTimer!!.start()
    }

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any) {
        super.refresh(data)
        if (data is com.iwedia.cltv.platform.model.TvChannel) {
            resetViews()
            customDetails.resetData()
            val constraintLayout: ConstraintLayout = view!!
                val constraintSet = ConstraintSet()
                constraintSet.clone(constraintLayout)
                constraintSet.connect(
                    R.id.zap_is_scrambled,
                    ConstraintSet.START,
                    R.id.channel_logo,
                    ConstraintSet.START,
                    0
                )
                constraintSet.connect(
                    R.id.zap_is_scrambled,
                    ConstraintSet.END,
                    R.id.channel_logo,
                    ConstraintSet.END,
                    0
                )
            val tvChannel = data
            var channelLogoPath = tvChannel.logoImagePath

            Utils.loadImage(
                channelLogoPath,
                channelLogo!!,
                object : AsyncReceiver {
                    override fun onFailed(error: core_entities.Error?) {
                        channelName!!.visibility = View.VISIBLE
                        channelName!!.text = tvChannel.name
                        channelName!!.post {
                            calculateChannelNameTextSize()
                        }
                        channelName!!.typeface = TypeFaceProvider.getTypeFace(
                            ReferenceApplication.applicationContext(),
                            ConfigFontManager.getFont("font_bold")
                        )

                        constraintSet.connect(
                            R.id.zap_is_scrambled,
                            ConstraintSet.BOTTOM,
                            R.id.channel_name,
                            ConstraintSet.TOP,
                            0
                        )
                        constraintSet.applyTo(constraintLayout)
                    }

                    override fun onSuccess() {
                        constraintSet.connect(
                            R.id.zap_is_scrambled,
                            ConstraintSet.BOTTOM,
                            R.id.channel_logo,
                            ConstraintSet.TOP,
                            0
                        )
                        constraintSet.applyTo(constraintLayout)
                        channelName!!.visibility = View.INVISIBLE
                        channelName!!.text = ""
                    }
                })

            var displayNumber = tvChannel.displayNumber
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

            channelType!!.text = (listener as CustomGzapBannerListener).getChannelSourceType(
                tvChannel
            )
        }
        else if (data is com.iwedia.cltv.platform.model.TvEvent) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "refresh: Mablog eventNAme ${data.name}")
            startTimer()
            //whenever the event info updates, update the next event info as well.
            (listener as CustomGzapBannerListener).getNextEvent(data.tvChannel,object :IAsyncDataCallback<TvEvent>{
                override fun onFailed(error: kotlin.Error) {}

                override fun onReceive(nextEvent: TvEvent) {
                    //if some callback comes late due to fast zapping
                    // then it should can show wrong channel data

                    if(nextEvent.tvChannel.id == data.tvChannel.id) {
                        customDetails.updateUpNextEvent(nextEvent)
                    }
                }
            })

            customDetails.updateData(
                data,
                (listener as CustomGzapBannerListener).getParentalRatingDisplayName(data.parentalRating, data),
                (listener as CustomGzapBannerListener).getCurrentTime(data.tvChannel),
                (listener as CustomGzapBannerListener).isCCTrackAvailable(),
                (listener as CustomGzapBannerListener).getDateTimeFormat(),
                (listener as CustomGzapBannerListener).isEventLocked(data),

            )
            //delay is added because it take some time to playbackstatus take some time to update the status to scramble.
            Handler().postDelayed({
                val isScrambled = (listener as CustomGzapBannerListener).isScrambled()
                scrambledImageView!!.visibility = if(isScrambled) View.VISIBLE else View.INVISIBLE
            }, 2000)


            Handler().postDelayed({
                customDetails.updateCCData((listener as CustomGzapBannerListener).isCCTrackAvailable())
                customDetails.updateVideoQuality(data.tvChannel, (listener as CustomGzapBannerListener).getVideoResolution())
            }, 5000)
            customDetails.updateAdImageView((listener as CustomGzapBannerListener).getIsAudioDescription(TvTrackInfo.TYPE_AUDIO))
            customDetails.updateDolbyImageView((listener as CustomGzapBannerListener).getIsDolby(TvTrackInfo.TYPE_AUDIO))

            customDetails.updateAudioChannelInfo((listener as CustomGzapBannerListener).getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO))
            customDetails.updateAudioChannelFormatInfo((listener as CustomGzapBannerListener).getAudioFormatInfo())
            // getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO) does not always return the correct track name for fluki so we need to use direct access
            if (data.tvChannel.tunerType == TunerType.ANALOG_TUNER_TYPE) {
                Handler().postDelayed({
                    if ((listener as CustomGzapBannerListener).getCurrentAudioTrack()?.trackName != null) {
                        customDetails.updateAudioChannelInfo((listener as CustomGzapBannerListener).getCurrentAudioTrack()?.trackName!!)
                    }
                }, 700)
            }

            customDetails.updateAudioTracks((listener as CustomGzapBannerListener).getAvailableAudioTracks(),(listener as CustomGzapBannerListener).getCurrentAudioTrack())

            customDetails.updateSubtitleTracks((listener as CustomGzapBannerListener).getAvailableSubtitleTracks(),(listener as CustomGzapBannerListener).getCurrentSubtitleTrack(),(listener as CustomGzapBannerListener).isSubtitleEnabled())

            updateCCInfo()

            (listener as CustomGzapBannerListener).setSpeechText(
                data.name.ifBlank {ConfigStringsManager.getStringById("no_information")},
                data.shortDescription ?: ConfigStringsManager.getStringById("no_information")
            )
         // todo parental should be handled inside CustomDetailsZapBanner

//            var list: List<String> = listOf()
//            if (data.parentalRating != null)
//                list = data.parentalRating!!.split("/")
//            var rating = if (list.size > 2) list[2].split("_").last().trim() else ""
//            parentalrating!!.text = if (rating == "U" || rating == "0") "" else rating
//            parentalrating!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        } else if (Utils.isListDataType(data, IAudioTrack::class.java)){
            val audioTracks: List<IAudioTrack> = data as List<IAudioTrack>
            customDetails.updateAdImageView((listener as CustomGzapBannerListener).getIsAudioDescription(TvTrackInfo.TYPE_AUDIO))
            customDetails.updateDolbyImageView((listener as CustomGzapBannerListener).getIsDolby(TvTrackInfo.TYPE_AUDIO))

            customDetails.updateAudioChannelInfo((listener as CustomGzapBannerListener).getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO))
            customDetails.updateAudioChannelFormatInfo((listener as CustomGzapBannerListener).getAudioFormatInfo())
            customDetails.updateAudioTracks(audioTracks, (listener as CustomGzapBannerListener).getCurrentAudioTrack()  )
            // getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO) does not always return the correct track name for fluki so we need to use direct access
            if((listener as CustomGzapBannerListener).getCurrentAudioTrack()?.trackName != null){
                //HACK ME BABY ONE MORE TIME, Musikic to fix, here you need to figure out if current channel is Analog
                //how, I do not know, good luck and god speed
                if((listener as CustomGzapBannerListener).getCurrentAudioTrack()?.trackName == "SAP") {
                    customDetails.updateAudioChannelInfo("SAP")
                }else if(((listener as CustomGzapBannerListener).getCurrentAudioTrack()?.isAnalogTrack)?:false && (listener as CustomGzapBannerListener).getCurrentAudioTrack()?.analogName != null){
                    Handler().postDelayed({
                        customDetails.updateAudioChannelInfo((listener as CustomGzapBannerListener).getCurrentAudioTrack()?.analogName?:"")
                    }, 700)
                   }
            }
            updateCCInfo()

        }
        customDetails.updateHohImageView((listener as CustomGzapBannerListener).isHOH(TvTrackInfo.TYPE_SUBTITLE)|| (listener as CustomGzapBannerListener).isHOH(TvTrackInfo.TYPE_AUDIO))
        customDetails.updateTtxImageView((listener as CustomGzapBannerListener).getTeleText(TvTrackInfo.TYPE_SUBTITLE))
    }

    fun calculateChannelNameTextSize() {
        channelName!!.post {
            if (channelName!!.lineCount > 2) {
                val textSize =
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
                channelName!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

            } else {
                val textSize =
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12)
                channelName!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            }
        }
    }

    // Returns max length of the word inside the string list
    private fun getTextWordMaxLength(text: List<String>): Int {
        var length = 0
        for (word in text) {
            if (word.length > length) {
                length = word.length
            }
        }
        return length
    }

    private fun updateCCInfo() {
        if((listener as CustomGzapBannerListener).isClosedCaptionEnabled() != true)     return
        val ccText = (listener as CustomGzapBannerListener).getClosedCaption()
        val isCCTrackAvailable = (listener as CustomGzapBannerListener).isCCTrackAvailable()
        customDetails.updateCcInfo(ccText, isCCTrackAvailable)
    }

    // Sets channel name text size depending on the text length and channel number
    private fun setChannelNameTextSize(maxLength: Int, textView: TextView, channelNumber: Int) {
        if (maxLength in 11..12) {
            val textSize = if (channelNumber > 10000)
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
            else
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12)
            textView!!.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                textSize
            )
        } else {
            val textSize = if (channelNumber > 10000)
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_8)
            else
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
            textView!!.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                textSize
            )
        }
    }

    override fun dispose() {
        super.dispose()
        resetViews()

        if (zapTimer != null) {
            zapTimer!!.cancel()
            zapTimer = null
        }

        lastKeyUpPressedTime = 0L
    }

    fun updateResolution(tvChannel: com.iwedia.cltv.platform.model.TvChannel,resolution: String) {
        customDetails.updateVideoQuality(tvChannel, resolution)
    }

}

