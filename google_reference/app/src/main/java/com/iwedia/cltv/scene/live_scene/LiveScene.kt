package com.iwedia.cltv.scene.live_scene

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.View.GONE
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.ReferenceApplication.Companion.getActivity
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.anoki_fast.FastAudioSubtitleList
import com.iwedia.cltv.anoki_fast.FastZapBanner
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.config.*
import com.iwedia.cltv.manager.LiveManager
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.*
import com.iwedia.cltv.platform.model.eas.EasEventInfo
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.parental_control.EnterPinAdapter
import com.iwedia.cltv.scene.parental_control.PinItem
import com.iwedia.cltv.utils.PinHelper
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import com.iwedia.guide.android.widgets.helpers.BaseLinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData
import world.SceneManager


class LiveScene(context: Context, sceneListener: LiveSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.LIVE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.LIVE),
    sceneListener
) {

    private var hideBlackOverlayJob: Job? = null
    private var backgroundLayout: ConstraintLayout? = null
    private var isEventUnlocked: Boolean? = false
    var radioChannelBg: ConstraintLayout? = null
    var scrambledChannelBg: ConstraintLayout? = null
    var statusInformationBg: ConstraintLayout? = null
    var blackOverlay: ConstraintLayout? = null
    var notRunningBg: ConstraintLayout? = null
    var inputNoSignalStatus: ConstraintLayout? = null
    var statusMessage: TextView? = null
    var blueMuteBlackOverlay: ConstraintLayout? = null
    var blueMuteOverlay: ConstraintLayout? = null

    private var doubleBackToExitPressedOnce = false
    private var isOverlayShown = false
    private var radioLogo: ImageView? = null
    private var radioTitle: TextView? = null
    private var radioIndex: TextView? = null
    private var radioEvent: TextView? = null
    private var radioText: TextView? = null

    var titleTv: TextView? = null
    var messageTv: TextView? = null
    var pressOk: TextView? = null
    var lockedIv: ImageView? = null
    var unlockBtn: ReferenceDrawableButton? = null
    var channelLogo: ImageView? = null
    var channelName: TextView? = null
    var channelIndex: TextView? = null
    var eventName : TextView? = null
    var eventRating : TextView? = null
    lateinit var pinAdapter: EnterPinAdapter
    var pinRecycler: RecyclerView? = null
    private val NUMBER_OF_PIN_ITEMS = 4
    var currentPinPosition = -1
    var lockedLayout: ConstraintLayout? = null
    var isShowingPin = false

    var isChannelLocked = false
    var isChannelParental = false

    var longPressFlag = false

    private var easLayout: RelativeLayout? = null
    private var easAlertMsg: TextView? = null
    private var easActivationInfo: TextView? = null
    private var easControl: Button? = null
    private var isEasDisplayed = false
    var radioOverlayAnimation: LottieAnimationView? = null
    var radioLoopAnimation: LottieAnimationView? = null
    var inputSelectedValue = "TV"

    var fastZapBanner: FastZapBanner?= null

    var right: TextView? = null
    var left: TextView? = null
    var up: TextView? = null
    var down: TextView? = null

    var hintMessageContainer: ConstraintLayout? = null
    var hintMessageText: TextView? = null

    var hintMessageContainer2: ConstraintLayout? = null
    var hintMessageText2: TextView? = null
    var activeChannel : TvChannel? = null
    private var isEasPlaying = false
    private var isPvrUnlockedTriggered: Boolean ? = false
    //Used to store whether pin is correct and to avoid lock screen after unlocking the channel
    private var isPinCorrect = false
    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_live, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                view?.let {
                    blackOverlay = view!!.findViewById(R.id.black_overlay)
                    blackOverlay!!.setBackgroundColor(
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                "color_background"
                            )
                        )
                    )
                    backgroundLayout = view!!.findViewById(R.id.background_layout)

                    var input_no_signal_message: TextView = view!!.findViewById(R.id.input_no_signal_message)
                    input_no_signal_message.text = ConfigStringsManager.getStringById("no_signal")
                    statusInformationBg = view!!.findViewById(R.id.status_information_container)
                    statusMessage = view!!.findViewById(R.id.status_message)
                    statusMessage!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    statusMessage!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )

                    notRunningBg = view!!.findViewById(R.id.service_not_running)

                    inputNoSignalStatus = view!!.findViewById(R.id.input_no_signal_layout)
                    //Radio channel
                    radioOverlayAnimation = view!!.findViewById(R.id.radio_intro_animation)
                    radioLoopAnimation = view!!.findViewById(R.id.radio_loop_animation)
                    radioChannelBg = view!!.findViewById(R.id.radio_layout)
                    radioChannelBg!!.setBackgroundColor(Color.TRANSPARENT)
                    radioLogo = view!!.findViewById(R.id.radio_logo)
                    radioTitle = view!!.findViewById(R.id.radio_title)
                    radioText = view!!.findViewById(R.id.txt_radio)
                    radioTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    radioTitle!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    radioIndex = view!!.findViewById(R.id.radio_index)
                    radioIndex!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    radioIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    radioEvent = view!!.findViewById(R.id.radio_event)
                    radioEvent!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    radioEvent!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    radioText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    radioText!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    //Scrambled channel
                    val scrambled_iv: ImageView = view!!.findViewById(R.id.scrambled_iv)
                    scrambled_iv.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    scrambled_iv.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    scrambled_iv.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    scrambledChannelBg = view!!.findViewById(R.id.scrambled_layout)
                    if(scrambledChannelBg != null) {
                        scrambledChannelBg!!.setBackgroundColor(
                            Color.TRANSPARENT

                        )
                    }

                    val subtitle: TextView = view!!.findViewById(R.id.subtitle)
                    subtitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    val scrambledTitle: TextView = view!!.findViewById(R.id.scrambled_title)
                    scrambledTitle.setText(ConfigStringsManager.getStringById("channels_scrambled"))
                    scrambledTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    val scrambledMessage: TextView = view!!.findViewById(R.id.scrambled_message)
                    scrambledMessage.setText(ConfigStringsManager.getStringById("channels_scrambled_message"))
                    scrambledMessage.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    blueMuteBlackOverlay = view!!.findViewById(R.id.hide_blue_mute_overlay)
                    blueMuteOverlay = view!!.findViewById(R.id.blue_mute_overlay)

                    //Locked channel
                    lockedLayout = view!!.findViewById(R.id.locked_layout) as ConstraintLayout
                    lockedLayout!!.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))

                    //setup title
                    titleTv = view!!.findViewById(R.id.title_scrambled)
                    titleTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    titleTv!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )
                    titleTv!!.text = ConfigStringsManager.getStringById("channel_locked")

                    //setup message
                    messageTv = view!!.findViewById(R.id.message)
                    messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    messageTv!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_to_unlock")

                    //setup second title
                    pressOk = view!!.findViewById(R.id.pressOkTv)
                    pressOk!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    pressOk!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    pressOk!!.text = ConfigStringsManager.getStringById("press_ok_to_confirm")
                    pressOk!!.visibility = View.INVISIBLE

                    //setup lock icon
                    lockedIv = view!!.findViewById(R.id.lockIv)
                    lockedIv!!.setImageDrawable(
                        ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
                    )
                    lockedIv!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    lockedIv!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    //setup unlock button
                    unlockBtn = view!!.findViewById(R.id.unlock)
                    unlockBtn!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface((sceneListener as LiveSceneListener))
                    unlockBtn!!.setText(ConfigStringsManager.getStringById("unlock"))
                    unlockBtn!!.getTextView().typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )


                    channelIndex = view!!.findViewById(R.id.lock_scene_index)
                    channelIndex!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    channelLogo = view!!.findViewById(R.id.lock_scene_logo)
                    channelName = view!!.findViewById(R.id.lock_scene_name)
                    channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    channelName!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )

                    eventName = view!!.findViewById(R.id.event_name)
                    eventName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    eventName!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )

                    eventRating = view!!.findViewById(R.id.event_rating)
                    eventRating!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                    eventRating!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    easLayout = view!!.findViewById(R.id.eas_layout)
                    easAlertMsg = view!!.findViewById(R.id.nav_eas_alert)
                    easActivationInfo = view!!.findViewById(R.id.nav_eas_activation)
                    easControl = view!!.findViewById(R.id.btn_eas_control)
                    easControl!!.setOnClickListener { checkEasStatus() }

                    //Fast zap banner
                    fastZapBanner = FastZapBanner(
                        context = context,
                        focusCallback = zapBannerFocusCallback,
                        fastAudioSubtitleListListener = object :
                            FastAudioSubtitleList.Listener {
                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as LiveSceneListener).setSpeechText(text = text, importance = importance)
                            }

                            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                (sceneListener as LiveSceneListener).showToast(text, duration)
                            }

                            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                                (sceneListener as LiveSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                            }
                        }
                    ).also {
                        it.visibility = GONE
                        view!!.findViewById<LinearLayout>(R.id.fast_zap_banner_container).addView(it)
                    }

                    var intentFilter = IntentFilter()
                    intentFilter.addAction(FastZapBannerDataProvider.FAST_SHOW_ZAP_BANNER_INTENT)
                    intentFilter.addAction(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                    ReferenceApplication.applicationContext().registerReceiver(receiver, intentFilter)

                    if((sceneListener as LiveSceneListener).isAccessibilityEnabled()){
                        right = view!!.findViewById(R.id.right)
                        left = view!!.findViewById(R.id.left)
                        up = view!!.findViewById(R.id.up)
                        down = view!!.findViewById(R.id.down)

                        statusMessage?.isFocusable = false
                        statusMessage?.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)

                        if ((sceneListener as LiveSceneListener).getDefaultInputValue() == "TV") {
                            right!!.isFocusable = true
                            right!!.text = ConfigStringsManager.getStringById("info_banner")
                            right!!.setOnClickListener {
                                removeFocus()
                                (sceneListener as LiveSceneListener).showInfoBanner()
                            }
                            left!!.isFocusable = true
                            left!!.text = ConfigStringsManager.getStringById("channel_list")
                            left!!.setOnClickListener {
                                removeFocus()
                                (sceneListener as LiveSceneListener).showChannelList()
                            }
                            up!!.isFocusable = true
                            up!!.requestFocus()
                            up!!.text = ConfigStringsManager.getStringById("home")
                            up!!.setOnClickListener {
                                removeFocus()
                                (sceneListener as LiveSceneListener).showHome()
                            }

                            if((sceneListener as LiveSceneListener).getConfigInfo("timeshift")) {
                                down!!.isFocusable = true
                                down!!.text = ConfigStringsManager.getStringById("player")
                                down!!.setOnClickListener {
                                    if (ReferenceApplication.worldHandler!!.playbackState != ReferenceWorldHandler.PlaybackState.RECORDING && (lockedLayout!!.visibility == View.GONE)) {
                                        removeFocus()
                                        (sceneListener as LiveSceneListener).showPlayer(KeyEvent.KEYCODE_DPAD_DOWN)
                                    }
                                }
                            }
                            up!!.isFocusable = true
                            up!!.setFocusable(true)
                            up!!.requestFocus()
                        } else {
                            right?.visibility = View.GONE
                            left?.visibility = View.GONE
                            up?.visibility = View.GONE
                            down?.visibility = View.GONE
                        }
                    }else{
                        right = view!!.findViewById(R.id.right)
                        left = view!!.findViewById(R.id.left)
                        up = view!!.findViewById(R.id.up)
                        down = view!!.findViewById(R.id.down)

                        right?.visibility = View.GONE
                        left?.visibility = View.GONE
                        up?.visibility = View.GONE
                        down?.visibility = View.GONE
                    }

                    hintMessageContainer = view!!.findViewById(R.id.hint_overlay)
                    hintMessageContainer!!.visibility = GONE

                    hintMessageContainer2 = view!!.findViewById(R.id.hint_overlay2)
                    hintMessageContainer2!!.visibility = GONE


                    hintMessageText = view!!.findViewById<TextView?>(R.id.hint_overlay_text)?.apply {
                        text = ConfigStringsManager.getStringById("long_press_ok_open_main_menu")
                        setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                        typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                    }

                    hintMessageText2 = view!!.findViewById<TextView?>(R.id.hint_overlay_text2)?.apply {
                        text = ConfigStringsManager.getStringById("press_back_exit_pin_input")
                        setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                        typeface = TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
                    }


                    hideBlueMuteBlackOverlay()
                    sceneListener.onSceneInitialized()
                }
            }
        })
    }

    fun removeFocus(){
        if((sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
            right!!.isFocusable = false
            left!!.isFocusable = false
            up!!.isFocusable = false
            down!!.isFocusable = false
            statusMessage!!.isFocusable = false
            right!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
            left!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
            up!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
            down!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
            statusMessage!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO)
        }
    }

    fun refreshAnimation() {
        Log.d(Constants.LogTag.CLTV_TAG + "LiveScene", "refreshAnimation: ")
        radioOverlayAnimation!!.clearAnimation()
        radioLoopAnimation!!.clearAnimation()
        radioLoopAnimation!!.visibility = GONE
        radioOverlayAnimation!!.visibility = VISIBLE
        radioOverlayAnimation!!.setAnimation("RadioOverlayIntro.json")
        radioOverlayAnimation!!.playAnimation()

        radioOverlayAnimation!!.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationStart(animation: Animator) {
                radioOverlayAnimation!!.alpha = 0.8F
                radioLoopAnimation!!.visibility = GONE
            }

            override fun onAnimationEnd(animation: Animator) {
                radioOverlayAnimation!!.visibility = GONE
                radioLoopAnimation!!.visibility = VISIBLE
                radioLoopAnimation!!.alpha = 0.5F
                radioLoopAnimation!!.setAnimation("RadioOverlayLoop.json")
                radioLoopAnimation!!.playAnimation()

            }

            override fun onAnimationCancel(animation: Animator) {
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        })


    }

    fun refreshEvent(currentChannel: TvChannel) {
        (sceneListener as LiveSceneListener).requestActiveEvent(currentChannel, object: IAsyncDataCallback<TvEvent>{
            override fun onFailed(error: Error) {
                runOnUiThread {
                    radioEvent!!.text = ""
                }
            }

            override fun onReceive(data: TvEvent) {
                try {
                    val curTime = (sceneListener as LiveSceneListener).getCurrentTime(data.tvChannel)
                    val endTime = data.endTime
                    val duration = endTime - curTime
                    CoroutineHelper.runCoroutineWithDelay({
                        runOnUiThread {
                            refreshEvent(currentChannel)
                        }
                    }, duration, Dispatchers.Main)
                    radioEvent!!.text = data.name
                    radioEvent!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                    )
                } catch (e: java.lang.Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + "LiveScene", "Exception - $e")
                    radioEvent!!.text = ""
                }
            }
        })
    }


    fun handlePVRPlayback(isPVRPlayback: Boolean, fromChannel: TvChannel?, recordedEvent: Recording?){
        if (isPVRPlayback){

            (sceneListener as LiveSceneListener).requestActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"LiveScene", " getActiveChannel -> onFailed()")
                }

                override fun onReceive(activeChannel: TvChannel) {
                    if (activeChannel.isRadioChannel){
                        runOnUiThread(Runnable {
                            radioChannelBg?.visibility = GONE
                        })
                    }
                }
            })

            if (recordedEvent!!.tvChannel!!.isRadioChannel){
                refreshAnimation()
                Utils.loadImage(
                    recordedEvent.tvChannel!!.logoImagePath,
                    radioLogo!!,
                    object : AsyncReceiver {
                        override fun onFailed(error: core_entities.Error?) {
                            radioLogo?.visibility = GONE
                            radioTitle?.visibility = VISIBLE
                            radioTitle!!.text = recordedEvent.tvChannel!!.name
                        }

                        override fun onSuccess() {
                            radioTitle?.visibility = GONE
                        }
                    })

                try{
                    if (recordedEvent.tvEvent!!.name == "No Information"){
                        if (recordedEvent.name == recordedEvent.tvChannel!!.name){
                            radioEvent!!.text = recordedEvent.tvEvent!!.name
                        } else {
                            radioEvent!!.text = recordedEvent.name
                        }
                    } else {
                        radioEvent!!.text = recordedEvent.tvEvent!!.name
                    }

                } catch (e: java.lang.Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + "LiveScene", "Exception - $e")
                    radioEvent!!.text = ""
                }

                val recordedDisplayNumber = recordedEvent.tvChannel!!.displayNumber
                if (recordedDisplayNumber != null) {
                    if (recordedDisplayNumber.isDigitsOnly()) {
                        var number = recordedDisplayNumber.toInt()
                        if (number < 10) {
                            radioIndex!!.text = "000$number"
                        } else if (number in 10..99) {
                            radioIndex!!.text = "00$number"
                        }else if (number in 99..999) {
                            radioIndex!!.text = "0$number"
                        } else {
                            radioIndex!!.text = number.toString()
                        }

                    } else {
                        radioIndex!!.text = recordedDisplayNumber
                    }
                }
                radioChannelBg?.visibility = VISIBLE
                refreshAnimation()
            }
        } else if (!isPVRPlayback && fromChannel!!.isRadioChannel) {
            //TODO DEJAN
            //refreshEvent(fromChannel)
            radioChannelBg?.visibility = View.VISIBLE
            refreshAnimation()
        }
    }
    /**
     * To refresh channel info on setting changes
     */
    @SuppressLint("SetTextI18n")
    fun updateChannelInfo(activeChannel: TvChannel?) {
        val displayNumber = activeChannel?.displayNumber
        if (displayNumber?.isDigitsOnly() == true) {
            val number = displayNumber.toInt()
            if (number < 10) {
                radioIndex!!.text = "000$number"
            } else if (number in 10..99) {
                radioIndex!!.text = "00$number"
            } else if (number in 99..999) {
                radioIndex!!.text = "0$number"
            } else {
                radioIndex!!.text = number.toString()
            }
        } else {
            radioIndex!!.text = displayNumber
        }
    }

    private fun setRadioDetails(tvChannel: TvChannel) {
        Utils.loadImage(
            tvChannel.logoImagePath,
            radioLogo!!,
            object : AsyncReceiver {
                override fun onFailed(error: core_entities.Error?) {
                    radioLogo?.visibility = GONE
                    radioTitle?.visibility = VISIBLE
                    radioTitle!!.text = tvChannel.name
                    radioTitle!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                    )
                }

                override fun onSuccess() {
                    radioTitle?.visibility = GONE
                }
            })
        val displayNumber = tvChannel.displayNumber
        if (displayNumber.isDigitsOnly()) {
            var number = displayNumber.toInt()
            if (number < 10) {
                radioIndex!!.text = "000$number"
            } else if (number in 10..99) {
                radioIndex!!.text = "00$number"
            }else if (number in 99..999) {
                radioIndex!!.text = "0$number"
            } else {
                radioIndex!!.text = number.toString()
            }
        } else {
            radioIndex!!.text = displayNumber
        }
        radioIndex!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        runOnUiThread {
            refreshEvent(tvChannel)
        }
    }

    fun refreshOverlays() {
        (sceneListener as LiveSceneListener).requestActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(data: TvChannel) {
                if (!data.isFastChannel()) {
                    //NO SIGNAL
                    if ((sceneListener as LiveSceneListener).isBlueMuteActive()) {
                        if((sceneListener as LiveSceneListener).isBlueMuteEnabled())
                        {
                            if(isChannelLocked && !(sceneListener as LiveSceneListener).isChannelUnlocked()) {
                                lockedLayout!!.visibility = VISIBLE
                                hintMessageContainer!!.visibility = VISIBLE
                            }
                            else {
                                lockedLayout!!.visibility = GONE
                                hintMessageContainer!!.visibility = GONE
                                blackOverlay!!.visibility = GONE
                            }
                            statusMessage!!.visibility = GONE
                        }
                        else{
                            //check if channel is locked
                            if(isChannelLocked && !(sceneListener as LiveSceneListener).isChannelUnlocked()) {
                                lockedLayout!!.visibility = VISIBLE
                                hintMessageContainer!!.visibility = VISIBLE
                                statusMessage!!.visibility = GONE
                            }
                            //if channel is not locked display no signal message
                            else {
                                lockedLayout!!.visibility = GONE
                                hintMessageContainer!!.visibility = GONE
                                blackOverlay?.visibility = VISIBLE
                                statusMessage!!.visibility = VISIBLE
                                statusMessage!!.setSelected(false)
                                statusMessage!!.setFocusable(false)
                            }

                        }
                    }
                    //HAS SIGNAL
                    else{
                        if(isChannelLocked && !(sceneListener as LiveSceneListener).isChannelUnlocked()
                            || (sceneListener as LiveSceneListener).isParentalBlockingActive()) {
                            if(lockedLayout != null && IsParentalSwitchEnable()) {
                                lockedLayout!!.visibility = VISIBLE
                                hintMessageContainer!!.visibility = VISIBLE

                                if ((worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.ZAP_BANNER)) {
                                    worldHandler!!.triggerAction(
                                        ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                        SceneManager.Action.DESTROY
                                    )
                                }
                            }

                        }
                        else {
                            if(lockedLayout != null) {
                                lockedLayout!!.visibility = GONE
                                hintMessageContainer!!.visibility = GONE
                            }

                        }
                        statusInformationBg?.visibility = GONE
                        statusMessage?.visibility = GONE
                    }
                }
            }
        })
    }
    fun IsParentalSwitchEnable():Boolean{
        return (sceneListener as LiveSceneListener).isParentalControlEnabled()
    }

    fun refreshInputNoSignal(value: Int) {
        if (value == 0) {
            inputNoSignalStatus?.visibility = GONE
        } else {
            inputNoSignalStatus?.visibility = VISIBLE

        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun refreshLiveScene( paramValue :Int) {
        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +javaClass.simpleName,"param value is $paramValue")
        if(paramValue == 4){
            if(BuildConfig.FLAVOR != "refplus5"
                && !(sceneListener as LiveSceneListener).getPlatformName().contains("RealTek")) {
                return
            }
        }
        if (paramValue == 20 || paramValue == 19 || paramValue == 4) {
            radioChannelBg!!.visibility = VISIBLE
            radioEvent?.visibility = VISIBLE
            scrambledChannelBg?.visibility = INVISIBLE
            radioEvent!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
            when (paramValue) {
                20,4 -> {
                    //show animation
                    refreshAnimation()
                    radioEvent!!.text = ConfigStringsManager.getStringById("no_video_channel")
                }
                19 -> {
                    radioOverlayAnimation?.clearAnimation()
                    radioLoopAnimation?.clearAnimation()
                    radioEvent!!.text = ConfigStringsManager.getStringById("no_audio_video_channel")
                }
            }
        }
        else {
            radioChannelBg!!.visibility = GONE
        }

    }

    private fun setLockedChannelDetails(activeChannel: TvChannel) {
        isShowingPin = false

        if(!inputSelectedValue.contains("Composite")) {
            Utils.loadImage(
                activeChannel.logoImagePath,
                channelLogo!!,
                object : AsyncReceiver {
                    override fun onFailed(error: core_entities.Error?) {
                        channelName!!.visibility = VISIBLE
                        channelLogo!!.visibility = GONE
                        setChannelName(activeChannel.name)
                        channelLogo!!.tag = "no_image"
                    }

                    override fun onSuccess() {
                        channelLogo!!.tag = "has_image"
                        channelName!!.visibility = GONE
                        channelLogo!!.visibility = VISIBLE
                    }
                })

            channelIndex!!.text = activeChannel.getDisplayNumberText()
        } else {
            channelName!!.visibility = GONE
            channelLogo!!.visibility = GONE
            channelIndex!!.text = ""
            eventName!!.visibility = VISIBLE
            eventName!!.text = ""
            eventRating!!.text = ""
        }

        if(eventName!!.visibility == VISIBLE && !inputSelectedValue.contains("Composite")){

            (sceneListener as LiveSceneListener).requestActiveEvent(activeChannel, object : IAsyncDataCallback<TvEvent>{
                override fun onFailed(error: Error) {
                }

                override fun onReceive(data: TvEvent) {
                   eventName!!.text = data.name
                   eventRating!!.text = (sceneListener as LiveSceneListener).getParentalRatingDisplayName(data.parentalRating, data)
                }

            })
        }
        channelIndex!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        unlockBtn!!.setOnClickListener {
            channelName!!.visibility = GONE
            channelIndex!!.visibility = GONE
            channelLogo!!.visibility = GONE
            hintMessageContainer?.visibility = GONE
            (sceneListener as LiveSceneListener).onUnlockPressed()
        }
        unlockBtn?.setOnLongClickListener {
            if(inputSelectedValue == "TV") {
                (sceneListener as LiveSceneListener).showHome(LiveSceneListener.ShowHomeType.SET_FOCUS_TO_LIVE_OR_BROADCAST_IN_TOP_MENU)
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener true
        }

        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        titleTv!!.text = ConfigStringsManager.getStringById("channel_locked")
        lockedIv!!.imageTintList =
            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        titleTv!!.text = if(eventName!!.visibility == VISIBLE) ConfigStringsManager.getStringById("event_locked")
                        else ConfigStringsManager.getStringById("channel_locked")
        titleTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_to_unlock")
        messageTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        pressOk!!.visibility = View.INVISIBLE
        if (pinRecycler != null) {
            pinRecycler!!.visibility = GONE
        }
        channelIndex!!.visibility = VISIBLE
        unlockBtn!!.visibility = VISIBLE
        hintMessageContainer?.visibility = VISIBLE
        hintMessageContainer2?.visibility = GONE
        if(worldHandler!!.active!!.scene == this && !isFastZapBannerActive()) {
            unlockBtn!!.requestFocus()
        }
        refreshOverlays()
        (sceneListener as LiveSceneListener).setSpeechText(
            titleTv!!.text.toString(),
            messageTv!!.text.toString(),
            importance = SpeechText.Importance.HIGH
        )
    }

    private fun setChannelName(name: String) {
        channelName!!.text = name
        channelName!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        channelName!!.post {
            if (channelName!!.lineCount > 3) {
                channelName!!.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10))
            } else {
                channelName!!.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12))
            }
        }
    }

    override fun onResume() {
        if((sceneListener as LiveSceneListener).isAccessibilityEnabled()){
            if ((sceneListener as LiveSceneListener).getDefaultInputValue() == "TV") {
                right!!.isFocusable = true
                left!!.isFocusable = true
                up!!.isFocusable = true
                right!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
                left!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
                up!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)

                if((sceneListener as LiveSceneListener).getConfigInfo("timeshift")) {
                    down!!.isFocusable = true
                    down!!.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
                }
            } else {
                right?.visibility = View.GONE
                left?.visibility = View.GONE
                up?.visibility = View.GONE
                down?.visibility = View.GONE
            }
        }

        super.onResume()
        isPvrUnlockedTriggered = false
        //used delay because MtkTvBanner.getInstance().caption is null when we turn on/off tv
        //to prevent this we are using delay.
        Handler().postDelayed({
            (sceneListener as LiveSceneListener).setCCInfo()
        },2000)
        if (isChannelParental || isChannelLocked) {
            if (pinRecycler?.visibility == VISIBLE) {
                pinRecycler?.requestFocus()
            } else if (lockedLayout?.visibility == VISIBLE && unlockBtn?.visibility == VISIBLE && !isFastZapBannerActive()) {
                unlockBtn!!.requestFocus()
            }
        }
    }

    fun focusOnHomeUp(){
        if((sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
            up?.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
            up?.isFocusable = true
            up?.requestFocus()
        }
    }

    private fun hidePin() {
        runOnUiThread{
            lockedIv!!.setImageDrawable(
                ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
            )
            lockedIv!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            titleTv!!.text = if(eventName!!.visibility == VISIBLE) ConfigStringsManager.getStringById("event_locked")
            else ConfigStringsManager.getStringById("channel_locked")
            titleTv!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
            messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_to_unlock")
            messageTv!!.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
            pressOk!!.visibility = View.INVISIBLE
            if (channelLogo!!.tag == "has_image") {
                channelLogo!!.visibility = VISIBLE
            }
            pinRecycler?.visibility = GONE
            if(channelLogo!!.visibility != VISIBLE)
                channelName!!.visibility = VISIBLE
            channelIndex!!.visibility = VISIBLE
            unlockBtn!!.visibility = VISIBLE
            hintMessageContainer?.visibility = VISIBLE
            unlockBtn!!.requestFocus()

            isShowingPin = false
            hintMessageContainer2!!.visibility = GONE
        }
    }

    private fun startPinInsertActivity() {
        hidePin()
        blackOverlay?.visibility = VISIBLE
        PinHelper.setPinResultCallback(object : PinHelper.PinCallback {
            override fun pinCorrect() {
                isPinCorrect = true
                (sceneListener as LiveSceneListener).unlockChannel(object : IAsyncCallback {
                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun onSuccess() {
                        runOnUiThread{
                            isPinCorrect = false
                            blackOverlay?.visibility = GONE
                            hintMessageContainer2!!.visibility = GONE
                            pinRecycler?.removeAllViews()
                            pinRecycler?.visibility = GONE
                            hintMessageContainer?.visibility = GONE
                            pinRecycler = null
                            //remove lock when parental and channel lock both are inactive
                            if ((sceneListener as LiveSceneListener).isChannelUnlocked() && !(sceneListener as LiveSceneListener).isParentalBlockingActive()) {
                                (sceneListener as LiveSceneListener).mutePlayback(false)

                                lockedLayout!!.visibility = GONE
                                hintMessageContainer?.visibility = GONE
                                unlockBtn!!.setOnClickListener(null)
                                unlockBtn?.visibility = GONE
                                hintMessageContainer?.visibility = GONE
                                isShowingPin = false
                                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)

                                (sceneListener as LiveSceneListener).requestActiveChannel(object: IAsyncDataCallback<TvChannel> {
                                    override fun onFailed(error: Error) {
                                    }

                                    @RequiresApi(Build.VERSION_CODES.R)
                                    override fun onReceive(data: TvChannel) {
                                        if (!data.isFastChannel()) {
                                            if ((sceneListener as LiveSceneListener).isRecordingInProgress())
                                                worldHandler!!.triggerAction(
                                                    ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                                                    SceneManager.Action.SHOW_OVERLAY
                                                )
                                            else showZapBanner(data)
                                        }
                                    }
                                })

                            }
                            if(isEventUnlocked == true) {
                                lockedLayout!!.visibility = GONE
                                unlockBtn!!.setOnClickListener(null)
                                unlockBtn?.visibility = GONE
                                hintMessageContainer?.visibility = GONE
                                isEventUnlocked = false
                                (sceneListener as LiveSceneListener).mutePlayback(false)
                                if (activeChannel != null && !activeChannel?.isFastChannel()!!) {
                                    if ((sceneListener as LiveSceneListener).isRecordingInProgress())
                                        worldHandler!!.triggerAction(
                                            ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                                            SceneManager.Action.SHOW_OVERLAY
                                        )
                                    else showZapBanner(activeChannel!!)
                                }
                            }
                        }
                    }

                    override fun onFailed(error: Error) {
                        isPinCorrect = false
                    }
                })
            }

            override fun pinIncorrect() {
                CoroutineHelper.runCoroutineWithDelay({
                    blackOverlay?.visibility = GONE
                }, 500, Dispatchers.Main)
                hintMessageContainer?.visibility = VISIBLE
                hintMessageContainer2!!.visibility = GONE
            }
        })
        var pinTitle = if(eventName!!.visibility == VISIBLE) ConfigStringsManager.getStringById("event_locked")
        else ConfigStringsManager.getStringById("unlock_channel")

        var pinDesc = ConfigStringsManager.getStringById("enter_pin_using_number")

        PinHelper.startPinActivity(pinTitle, pinDesc)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showZapBanner(tvChannel: TvChannel) {

        runOnUiThread {
            val activeScene = worldHandler!!.active
            if (activeScene != null) {
                if(worldHandler?.active?.scene?.id == id) {
                    runOnUiThread {
                        if (worldHandler!!.getVisibles().contains(ReferenceWorldHandler.SceneId.ZAP_BANNER)) {
                            worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                SceneManager.Action.DESTROY
                            )
                        }
                        val sceneData = SceneData(activeScene!!.id, activeScene.instanceId,tvChannel)
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.ZAP_BANNER,
                            SceneManager.Action.SHOW_OVERLAY, sceneData
                        )
                    }
                }
            }
        }
    }

    private fun showPin() {
        isShowingPin = true
        hintMessageContainer2!!.visibility = VISIBLE

        if (PinHelper.USE_PIN_INSERT_ACTIVITY) {
            startPinInsertActivity()
            return
        }

        currentPinPosition = 0
        if(!(sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
            if (pinRecycler != null) {
                if (pinRecycler!!.getChildAt(0) != null) {
                    pinRecycler!!.getChildAt(0).requestFocus()
                }
            }
        }

        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_unlock_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        titleTv!!.text = if(eventName!!.visibility == VISIBLE) ConfigStringsManager.getStringById("event_locked")
                        else ConfigStringsManager.getStringById("unlock_channel")
        titleTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_using_number")
        messageTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        unlockBtn!!.visibility = GONE
        hintMessageContainer?.visibility = GONE

        //setup recycler and adapter
        pinRecycler = view!!.findViewById(R.id.new_pin_items)
        pinRecycler!!.visibility = VISIBLE
        var layoutManager = LinearLayoutManager(context)
        if(!(sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
            layoutManager = BaseLinearLayoutManager(context)
        }
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        pinRecycler!!.layoutManager = layoutManager
        pinAdapter = EnterPinAdapter(getPinItems(NUMBER_OF_PIN_ITEMS)!!)
        pinRecycler!!.adapter = pinAdapter

        if((sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
            pinRecycler!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            pinRecycler!!.isFocusable = false
        }

        pinAdapter.registerListener(object : EnterPinAdapter.EnterPinListener {

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                (sceneListener as LiveSceneListener).setSpeechText(text = text, importance = importance)
            }

            override fun onPinConfirmed(pinCode: String?) {
                blackOverlay?.visibility = VISIBLE
                (sceneListener as LiveSceneListener).checkPin(pinCode!!, object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        CoroutineHelper.runCoroutineWithDelay({
                            blackOverlay?.visibility = GONE
                        }, 500, Dispatchers.Main)
                        if(!(sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
                            pinRecycler?.getChildAt(0)?.requestFocus()
                        }
                        pinAdapter.refresh(getPinItems(NUMBER_OF_PIN_ITEMS)!!)
                        hintMessageContainer2!!.visibility = VISIBLE
                    }

                    @RequiresApi(Build.VERSION_CODES.Q)
                    override fun onSuccess() {
                        CoroutineHelper.runCoroutineWithDelay({
                            hintMessageContainer2!!.visibility = GONE
                        }, 500, Dispatchers.Main)
                        runOnUiThread{
                            pinRecycler?.removeAllViews()
                            pinRecycler?.visibility = GONE
                            hintMessageContainer?.visibility = GONE
                            pinRecycler = null
                            //remove lock when parental and channel lock both are inactive
                            if ((sceneListener as LiveSceneListener).isChannelUnlocked() && !(sceneListener as LiveSceneListener).isParentalBlockingActive()) {
                                (sceneListener as LiveSceneListener).mutePlayback(false)

                                lockedLayout!!.visibility = GONE
                                hintMessageContainer?.visibility = GONE
                                unlockBtn!!.setOnClickListener(null)
                                unlockBtn?.visibility = GONE
                                hintMessageContainer?.visibility = GONE
                                isShowingPin = false
                                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)

                                (sceneListener as LiveSceneListener).requestActiveChannel(object: IAsyncDataCallback<TvChannel> {
                                    override fun onFailed(error: Error) {
                                    }

                                    @RequiresApi(Build.VERSION_CODES.R)
                                    override fun onReceive(data: TvChannel) {
                                        if (!data.isFastChannel()) {
                                            showZapBanner(data)
                                        }
                                    }
                                })

                            }
                            if(isEventUnlocked == true) {
                                lockedLayout!!.visibility = GONE
                                unlockBtn!!.setOnClickListener(null)
                                unlockBtn?.visibility = GONE
                                hintMessageContainer?.visibility = GONE
                                isEventUnlocked = false
                                //after unlocking the fast channel lock if there is no internet,
                                // then no internet message should display on screen
                                if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal)   {
                                    if (!(sceneListener as LiveSceneListener).isNetworkAvailable())
                                        refresh(Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL)
                                }
                                }

                        }
                    }
                })
            }

            override fun getAdapterPosition(position: Int) {
                currentPinPosition = position
                if (position == 3) {
                    pressOk!!.visibility = VISIBLE
                    (sceneListener as LiveSceneListener).setSpeechText(pressOk!!.text.toString(), importance = SpeechText.Importance.HIGH)
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
                if(!(sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
                    pinRecycler!!.getChildAt(currentPinPosition).requestFocus()
                }
            }

            override fun validationEnabled() {
            }

            override fun isAccessibilityEnabled(): Boolean {
                return (sceneListener as LiveSceneListener).isAccessibilityEnabled()
            }

        })

        if(!(sceneListener as LiveSceneListener).isAccessibilityEnabled()) {
            pinRecycler!!.requestFocus()
        }

        (sceneListener as LiveSceneListener).setSpeechText(
            messageTv!!.text.toString(),
            importance = SpeechText.Importance.HIGH
        )
    }

    /**
     * Get pin items
     *
     * @return Pin items
     */
    private fun getPinItems(numberOfItems: Int): MutableList<PinItem>? {
        val pinItems = mutableListOf<PinItem>()
        for (i in 0 until numberOfItems) {
            pinItems.add(PinItem(i, PinItem.TYPE_PASSWORD))
        }
        return pinItems
    }


    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun refresh(data: Any?) {
        if (data is Int) {
            when(data) {
                Events.PLAYBACK_STATUS_MESSAGE_NONE -> {
                    (sceneListener as LiveSceneListener).requestActiveChannel(object: IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                        }

                        override fun onReceive(data: TvChannel) {
                            if (!data.isRadioChannel) {
                                runOnUiThread {
                                    radioChannelBg?.visibility = GONE
                                    scrambledChannelBg?.visibility = GONE
                                    statusInformationBg?.visibility = GONE
                                    statusMessage?.text = ""
                                }
                            }
                        }
                    })
                }
                Events.PLAYBACK_STATUS_MESSAGE_IS_MESSAGE -> {
                    if (!isOverlayShown) {
                        statusInformationBg!!.visibility = GONE
                        statusMessage!!.text = ""
                    }
                }

                Events.PLAYBACK_STATUS_MESSAGE_NO_PLAYBACK -> {
                    runOnUiThread {
                        radioChannelBg!!.visibility = GONE
                        statusInformationBg!!.visibility = VISIBLE
                        statusMessage!!.text = ConfigStringsManager.getStringById("status_message_no_playback")
                        refreshOverlays()
                        statusMessage!!.typeface = TypeFaceProvider.getTypeFace(
                            ReferenceApplication.applicationContext(),
                            ConfigFontManager.getFont("font_regular")
                        )
                    }
                }

                Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL -> {
                    runOnUiThread {
                        blackOverlay?.visibility = GONE
                        radioChannelBg!!.visibility = GONE
                        scrambledChannelBg?.visibility = GONE
                        statusInformationBg!!.visibility = VISIBLE
                        //when fast channel is played and there is no internet then no internet message should display.
                        if(worldHandler!!.playbackState != ReferenceWorldHandler.PlaybackState.VOD) {
                            statusMessage!!.text =
                                if (worldHandler!!.getApplicationMode() == ApplicationMode.DEFAULT.ordinal)
                                    ConfigStringsManager
                                        .getStringById("status_message_signal_not_available")
                                else
                                    ConfigStringsManager
                                        .getStringById("no_internet_connection")
                        }
                        statusMessage?.visibility = VISIBLE
                        refreshOverlays()
                        statusMessage!!.typeface = TypeFaceProvider.getTypeFace(
                            ReferenceApplication.applicationContext(),
                            ConfigFontManager.getFont("font_regular")
                        )
                    }
                }

                Events.PLAYBACK_SHOW_BLACK_OVERLAY -> {
                    isOverlayShown = true
                    blackOverlay!!.visibility = VISIBLE
                }
                Events.PLAYBACK_HIDE_BLACK_OVERLAY -> {
                    isOverlayShown = false
                    blackOverlay!!.visibility = GONE
                }

                Events.PLAYBACK_STATUS_MESSAGE_IS_LOCKED,
                Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL -> {
                    if (data == Events.PLAYBACK_STATUS_MESSAGE_IS_LOCKED) {
                        isChannelLocked = true
                        eventRating!!.visibility = GONE
                        eventName!!.visibility = GONE
                    }

                    if (data == Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL ) {
                        if((sceneListener as LiveSceneListener).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal){
                            statusMessage?.visibility = GONE
                            statusInformationBg?.visibility = GONE
                        }
                        isChannelParental = true
                        if(!isChannelLocked || (sceneListener as LiveSceneListener).isChannelUnlocked()){
                            eventName!!.visibility = VISIBLE
                            eventRating!!.visibility = VISIBLE
                        }
                    }

                    if (isChannelLocked || isChannelParental) {
                        if (!isPinCorrect) {
                            handleLockedChannel()
                        }
                    }
                }
                Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_PARENTAL,
                Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_LOCKED -> {
                    //TODO
                    (sceneListener as LiveSceneListener).setIsOnLockScreen(false)
                    if (data == Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_LOCKED) {
                        isChannelLocked = false
                    }

                    if (data == Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_PARENTAL) {
                        isChannelParental = false
                        (sceneListener as LiveSceneListener).requestActiveChannel(object :
                            IAsyncDataCallback<TvChannel> {
                            override fun onFailed(error: Error) {
                            }

                            override fun onReceive(data: TvChannel) {
                                runOnUiThread{
                                    isChannelLocked = data.isLocked
                                    if (!data.isLocked) {
                                        removeChannelLock()
                                    } else {
                                        if (!(sceneListener as LiveSceneListener).isChannelUnlocked() && (sceneListener as LiveSceneListener).isParentalControlEnabled()) {
                                            if((!lockedLayout!!.isVisible)) {
                                                showChannelLock(data)
                                            }
                                        } else {
                                            removeChannelLock()
                                            refreshOverlays()
                                        }
                                    }
                                }
                            }
                        })
                    }

                    if (!isChannelLocked && !isChannelParental) {
                        removeChannelLock()
                    }
                    refreshOverlays()
                }

                Events.PLAYBACK_STATUS_MESSAGE_UNLOCK_PRESSED-> {
                    (sceneListener as LiveSceneListener).requestActiveChannel(object :
                        IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                        }
                        override fun onReceive(activeChannel: TvChannel) {
                            if (activeChannel.isLocked && !(sceneListener as LiveSceneListener).isParentalBlockingActive()) {
                                runOnUiThread{
                                    showPin()
                                }
                            }else if((sceneListener as LiveSceneListener).isChannelUnlocked()){
                                if ((sceneListener as LiveSceneListener).isParentalBlockingActive()){
                                    runOnUiThread{
                                        showPin()
                                    }
                                }

                            }
                        }

                    })
                    refreshOverlays()
                }
                Events.PLAYBACK_STATUS_MESSAGE_IS_SCRAMBLED -> {
                    radioChannelBg!!.visibility = GONE
                    if ((sceneListener as LiveSceneListener).isScrambled() && !(sceneListener as LiveSceneListener).isQuietTune()) {
                        runOnUiThread {
                            scrambledChannelBg?.visibility = VISIBLE
                            blackOverlay?.visibility = GONE
                            statusMessage?.visibility = GONE
                        }
                    } else {
                        runOnUiThread {
                            statusMessage?.visibility = VISIBLE
                            scrambledChannelBg?.visibility = GONE
                        }
                    }
                }
            }
        }
        if (data is LiveManager.LiveSceneStatus) {
            Log.d(Constants.LogTag.CLTV_TAG + "LiveScene", "${data.type}")
            (sceneListener as LiveSceneListener).requestActiveChannel(object: IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(activeChannel: TvChannel) {
                    if(activeChannel != null) {
                        if (data.type == LiveManager.LiveSceneStatus.Type.NONE && !activeChannel.isRadioChannel) {
                            runOnUiThread(Runnable {
                                radioChannelBg!!.visibility = GONE
                                scrambledChannelBg!!.visibility = GONE
                                statusInformationBg!!.visibility = GONE
                                statusMessage!!.text = ""
                            })
                        }
                    }
                }
            })

            if (data.type == LiveManager.LiveSceneStatus.Type.IS_NOT_RUNNING) {
                runOnUiThread {
                    notRunningBg!!.visibility = VISIBLE
                }
            }

            if (data.type == LiveManager.LiveSceneStatus.Type.IS_RUNNING) {
                runOnUiThread {
                    notRunningBg!!.visibility = GONE
                }
            }

            if (data.type == LiveManager.LiveSceneStatus.Type.IS_RADIO) {
                (sceneListener as LiveSceneListener).requestActiveChannel(object :
                    IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: TvChannel) {
                        if (data.isRadioChannel && statusInformationBg?.visibility != VISIBLE) {
                            setRadioDetails(data)
                            runOnUiThread {
                                radioChannelBg!!.visibility = VISIBLE
                                if(!(sceneListener as LiveSceneListener).checkSupportHbbtv()) {
                                    refreshAnimation()
                                }
                            }
                        }
                    }
                })
            }

            if ((sceneListener as LiveSceneListener).isScrambled() &&
                !(sceneListener as LiveSceneListener).isQuietTune()) {
                runOnUiThread {
                    scrambledChannelBg?.visibility = VISIBLE
                }
            } else {
                runOnUiThread {
                    scrambledChannelBg?.visibility = GONE
                }
            }

            if (data.type == LiveManager.LiveSceneStatus.Type.IS_MESSAGE) {

                if (data.message != "") {
                    runOnUiThread {
                        radioChannelBg!!.visibility = GONE
                        statusInformationBg!!.visibility = VISIBLE
                        statusMessage!!.text = data.message
                        refreshOverlays()
                        statusMessage!!.typeface = TypeFaceProvider.getTypeFace(
                            ReferenceApplication.applicationContext(),
                            ConfigFontManager.getFont("font_regular")
                        )
                    }
                } else {
                    if (!isOverlayShown) {
                        statusInformationBg!!.visibility = GONE
                        statusMessage!!.text = ""
                    }
                }
            }

            if (data.type == LiveManager.LiveSceneStatus.Type.IS_SHOW_OVERLAY) {
                isOverlayShown = true
                blackOverlay!!.visibility = VISIBLE
            }

            if (data.type == LiveManager.LiveSceneStatus.Type.IS_HIDE_OVERLAY) {
                isOverlayShown = false
                blackOverlay!!.visibility = GONE
                hideBlackOverlay()
            }

            if (data.type == LiveManager.LiveSceneStatus.Type.IS_LOCKED || data.type == LiveManager.LiveSceneStatus.Type.IS_PARENTAL) {
                if (data.type == LiveManager.LiveSceneStatus.Type.IS_LOCKED) {
                    isChannelLocked = true
                    eventRating!!.visibility = GONE
                    eventName!!.visibility = GONE
                }

                if (data.type == LiveManager.LiveSceneStatus.Type.IS_PARENTAL) {
                    isChannelParental = true
                    if(!isChannelLocked){
                        eventName!!.visibility = VISIBLE
                        eventRating!!.visibility = VISIBLE
                    }
                }

                if (isChannelLocked || isChannelParental) {
                    handleLockedChannel()
                }
            }

            if (data.type === LiveManager.LiveSceneStatus.Type.IS_NOT_LOCKED || data.type == LiveManager.LiveSceneStatus.Type.IS_NOT_PARENTAL) {
                (sceneListener as LiveSceneListener).setIsOnLockScreen(false)
                if (data.type === LiveManager.LiveSceneStatus.Type.IS_NOT_LOCKED) {
                    isChannelLocked = false
                }

                if (data.type === LiveManager.LiveSceneStatus.Type.IS_NOT_PARENTAL) {
                    isChannelParental = false
                    (sceneListener as LiveSceneListener).requestActiveChannel(object :
                        IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                        }

                        override fun onReceive(activeChannel: TvChannel) {
                            runOnUiThread{
                                isChannelLocked = activeChannel.isLocked
                                if (!activeChannel.isLocked) {
                                    removeChannelLock()
                                } else {
                                    if (!(sceneListener as LiveSceneListener).isChannelUnlocked()) {
                                        showChannelLock(activeChannel)
                                    } else {
                                        removeChannelLock()
                                    }
                                }
                            }
                        }
                    })
                }

                if (!isChannelLocked && !isChannelParental) {
                    removeChannelLock()
                }
            }

            if (data.type == LiveManager.LiveSceneStatus.Type.UNLOCK_PRESSED) {
                showPin()
            }
            if (data.type == LiveManager.LiveSceneStatus.Type.PVR_UNLOCK) {
                isPvrUnlockedTriggered = true
                lockedLayout!!.visibility = GONE
                hintMessageContainer?.visibility = GONE
            }
            if (data.type == LiveManager.LiveSceneStatus.Type.PVR_LOCK) {
                if (isChannelLocked || isChannelParental) {
                    handleLockedChannel()
                }
            }
        }
        if (data is Event) {
            handleEasMessage(
                data.getData(0) as Int,
                data.getData(1) as EasEventInfo
            )
        }
        if(data is String) {
            inputSelectedValue = data
        }
        if (lockedLayout?.isVisible == true){
            (sceneListener as LiveSceneListener).setIsOnLockScreen(true)
        }else{
            (sceneListener as LiveSceneListener).setIsOnLockScreen(false)
        }
        super.refresh(data)
    }

    /**
     * Added a logic that after 5s reset blue screen and black overlay because of a issue on fast channel switch
     */
    private fun hideBlackOverlay() {
        hideBlackOverlayJob?.cancel()
        hideBlackOverlayJob = CoroutineScope(Dispatchers.IO).launch {
            hideBlackOverlayDelayTimer()
        }
    }

    private suspend fun hideBlackOverlayDelayTimer() {
        delay(5000)
        if((sceneListener as LiveSceneListener).isSignalAvailable() && !(sceneListener as LiveSceneListener).isScrambled()) {
            runOnUiThread {
                blackOverlay?.visibility = GONE
                if ((sceneListener as LiveSceneListener).isBlueMuteEnabled()) {
                    blueMuteBlackOverlay?.visibility = GONE
                }
            }
        }
    }

    private fun handleLockedChannel() {

        if(inputSelectedValue.contains("Composite")) {
            var fakeChannel = TvChannel()
            showChannelLock(fakeChannel)
            return
        }

        if(isPvrUnlockedTriggered == false) {
            (sceneListener as LiveSceneListener).requestActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(data: TvChannel) {
                    runOnUiThread {
                        showChannelLock(data)
                    }
                }
            })
        }
    }

    private fun showChannelLock(channel: TvChannel) {
        (sceneListener as LiveSceneListener).mutePlayback(true)
        if(inputSelectedValue == "TV") {
            ReferenceApplication.unlockedChannel = null
            if (!lockedLayout!!.isVisible && IsParentalSwitchEnable()) {
                lockedLayout!!.visibility = VISIBLE
                unlockBtn!!.visibility = VISIBLE
                hintMessageContainer?.visibility = VISIBLE
                hintMessageContainer2?.visibility = GONE

                (sceneListener as LiveSceneListener).notifyHbbTvChannelLockUnlock(true)
                (sceneListener as LiveSceneListener).setTTMLVisibility(false)
                /* if (isChannelLocked) {
            (sceneListener as LiveSceneListener).destroyOtherScenes()
        }*/
            }
            setLockedChannelDetails(channel)
        } else if ((sceneListener as LiveSceneListener).isParentalBlockingActive() && inputSelectedValue.contains(
                "Composite"
            )
        ) {
            ReferenceApplication.unlockedChannel = null
            if (!lockedLayout!!.isVisible && IsParentalSwitchEnable()) {
                lockedLayout!!.visibility = VISIBLE
                unlockBtn!!.visibility = VISIBLE
                hintMessageContainer?.visibility = VISIBLE
            }
            setLockedChannelDetails(channel)
        }
    }

    private fun removeChannelLock() {
        (sceneListener as LiveSceneListener).requestActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(tvChannel: TvChannel) {
                activeChannel = tvChannel
                ReferenceApplication.unlockedChannel = tvChannel
            }
        })
        //while application is in on pause state, player was stoped, so need to resume again in that case.
        //otherwise black screen would come and player would be in pause state.
        (sceneListener as LiveSceneListener).resumePlayer()
        (sceneListener as LiveSceneListener).mutePlayback(false)
        (sceneListener as LiveSceneListener).notifyHbbTvChannelLockUnlock(false)
        (sceneListener as LiveSceneListener).setTTMLVisibility(true)

        if(worldHandler!!.active!!.id == id){
            if (isFastZapBannerActive()) {
                if(unlockBtn!!.isFocused){
                    // Switch focus back to fast zap banner if it is visible
                    fastZapBanner!!.requestFocusChannelImage()
                }
            }
        }

        lockedLayout?.visibility = GONE
        hintMessageContainer?.visibility = GONE
        unlockBtn?.setOnClickListener(null)
        isShowingPin = false
    }

    fun clearAll() {
        runOnUiThread {
            radioChannelBg!!.visibility = GONE
            scrambledChannelBg?.visibility = GONE
            notRunningBg!!.visibility = GONE
            statusMessage!!.text = ""
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
            if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_ESCAPE) {
                BackFromPlayback.setKeyPressedState()
            } else {
                downActionBackKeyDone = true
                if (!BackFromPlayback.zapFromHomeOrSearch) {
                    BackFromPlayback.setKeyPressedState()
                }
            }
        }
        if(inputSelectedValue == "TV") {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BUTTON_X) {
                worldHandler?.destroyOtherExisting(id)
                fastZapBanner?.switchVisibility()
                return true
            }
            if (isFastZapBannerActive()) {
                if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BUTTON_X) {
                    fastZapBanner?.switchVisibility()

                    runOnUiThread {
                        worldHandler?.destroyOtherExisting(id)
                    }
                }
                //Handle channel up/down when parental control overlay is visible
                if (lockedLayout?.visibility == VISIBLE) {
                    when(keyCode) {
                        KeyEvent.KEYCODE_CHANNEL_DOWN,
                        KeyEvent.KEYCODE_CHANNEL_UP,
                            //RCU PREVIOUS CHANNEL
                        KeyEvent.KEYCODE_11,
                        KeyEvent.KEYCODE_LAST_CHANNEL -> {
                            /**
                             * zap banner is visible but focus is not on the zap banner.
                             * passing the channel up/down key events.
                             */
                            fastZapBanner!!.startTimer()
                            fastZapBanner!!.channelChangeKeyEvent(keyCode == KeyEvent.KEYCODE_CHANNEL_UP, keyEvent)
                        }
                    }
                }
                return true
            }
            if (
                keyEvent.action == KeyEvent.ACTION_DOWN &&
                lockedLayout!!.visibility != VISIBLE &&
                keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_CHANNEL_DOWN && // IMPORTANT! This key is separately handled to avoid showing Zap Banner earlier than it should be visible (CLTVFAST-540)
                keyCode != KeyEvent.KEYCODE_CHANNEL_UP &&
                keyCode !=  KeyEvent.KEYCODE_11 &&
                keyCode !=  KeyEvent.KEYCODE_LAST_CHANNEL&&
                keyCode !=  KeyEvent.KEYCODE_PROG_RED&&
                keyCode !=  KeyEvent.KEYCODE_PROG_GREEN&&
                // IMPORTANT! This key is separately handled to avoid showing Zap Banner earlier than it should be visible (CLTVFAST-540)
                worldHandler!!.active!!.id == id &&
                (sceneListener as LiveSceneListener).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal
            ) {
                worldHandler?.destroyOtherExisting(id)
                fastZapBanner?.switchVisibility()
                return true
            }
        }

        Log.e(Constants.LogTag.CLTV_TAG +"LiveScene","active scene here:live heree in liveee "+inputSelectedValue)
        if (inputSelectedValue.contains("HDMI") || inputSelectedValue.contains("Composite")) {
            if (inputSelectedValue.contains("Composite") && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_MENU) && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                Log.e(Constants.LogTag.CLTV_TAG +"LiveScene","active scene here:live heree ")
                if(inputNoSignalStatus?.visibility == VISIBLE) {
                    inputNoSignalStatus?.visibility = GONE
                }
                (sceneListener as LiveSceneListener).showPreferences()
                return true
            } else if ((keyCode == KeyEvent.KEYCODE_INFO) && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                if(inputNoSignalStatus?.visibility == VISIBLE) {
                    inputNoSignalStatus?.visibility = GONE
                }
                (sceneListener as LiveSceneListener).launchInputSelectedScene()
                return true
            }
            else {
                Log.e(Constants.LogTag.CLTV_TAG +"LiveScene","active scene here: "+worldHandler!!.active!!.id)
                if (isShowingPin && inputSelectedValue.contains("Composite") &&
                    worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.LIVE) {
                    return super.dispatchKeyEvent(keyCode, keyEvent)
                }
                if ((worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE) && (worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE) &&
                    (worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.PIN_SCENE) && (worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE) && (worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.DIALOG_SCENE)
                ) {
                    return true
                }
            }
        }
        //Check if it's active scene
        if (worldHandler!!.active!!.id != id) {
            return false
        }

        if(lockedLayout!!.visibility == VISIBLE && !isShowingPin){
            if (keyCode == KeyEvent.KEYCODE_INFO) {
                return false
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                if(!(sceneListener as LiveSceneListener).getIsOnLockScreen()){
                    (sceneListener as LiveSceneListener).showInfoBanner()
                }
                return true
            }
            if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP){
                if((sceneListener as LiveSceneListener).getConfigInfo("timeshift")) {
                    (sceneListener as LiveSceneListener).showPlayer(keyCode)
                }
                return true
            }
            if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP){
                if(!(sceneListener as LiveSceneListener).getIsOnLockScreen()){
                    (sceneListener as LiveSceneListener).showInfoBanner()
                }
            }
        }

        if (isShowingPin) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
                    hidePin()
                    return true
                }
            } else {

                return super.dispatchKeyEvent(keyCode, keyEvent)
            }
        }

        if ((keyEvent as KeyEvent).keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
            if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN && (keyEvent as KeyEvent).isLongPress) {
                return true
            }
        }

        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    (sceneListener as LiveSceneListener).showZapBanner()
                    hideBlueMuteBlackOverlay()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    (sceneListener as LiveSceneListener).showZapBanner()
                    hideBlueMuteBlackOverlay()
                    return true
                }

                //RCU PREVIOUS CHANNEL
                KeyEvent.KEYCODE_11,
                KeyEvent.KEYCODE_LAST_CHANNEL -> {
                    (sceneListener as LiveSceneListener).showZapBanner()
                    hideBlueMuteBlackOverlay()
                    lockedLayout?.let {
                        if((sceneListener as LiveSceneListener).getApplicationMode() == ApplicationMode.DEFAULT.ordinal) {
                            if (it!!.visibility == VISIBLE) {
                                if (!isChannelLocked) {
                                    it!!.visibility = GONE
                                }
                                hintMessageContainer?.visibility = GONE
                                blackOverlay?.visibility = VISIBLE
                                CoroutineHelper.runCoroutineWithDelay({
                                    blackOverlay?.visibility = GONE
                                }, 2000, Dispatchers.Main)
                            }
                        }
                    }
                    return true
                }

                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                    val digit = keyCode - KeyEvent.KEYCODE_0
                    (sceneListener as LiveSceneListener).digitPressed(digit)
                    return true
                }
                KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {
                    val digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                    (sceneListener as LiveSceneListener).digitPressed(digit)
                    return true
                }

                KeyEvent.KEYCODE_PERIOD,KeyEvent.KEYCODE_MINUS,KeyEvent.KEYCODE_SLASH -> {
                    (sceneListener as LiveSceneListener).periodPressed()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    if ((sceneListener as LiveSceneListener).isTTXActive()){
                        return true
                    }
                    if ((lockedLayout!!.visibility == GONE)) {
                        if((sceneListener as LiveSceneListener).getConfigInfo("timeshift")) {
                            (sceneListener as LiveSceneListener).showPlayer(keyCode)
                        }
                        return true
                    } else if (worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
                        (sceneListener as LiveSceneListener).showToast(ConfigStringsManager.getStringById("recording_progress_toast"))
                    }
                }
            }
            if (lockedLayout?.visibility == VISIBLE) {
                return true
            }
            if ((sceneListener as LiveSceneListener).resolveConfigurableKey(
                    keyCode,
                    keyEvent.action
                )
            ) {
                return true
            }
        }
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            if ((sceneListener as LiveSceneListener).resolveConfigurableKey(
                    keyCode,
                    keyEvent.action
                )
            ) {
                return true
            }

            when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    easLayout?.visibility = GONE
                    (sceneListener as LiveSceneListener).channelUp()

                    lockedLayout?.let {
                        if (it!!.visibility == VISIBLE) {
                            if (!isChannelLocked) {
                                it!!.visibility = GONE
                            }
                            hintMessageContainer?.visibility = GONE
                            blackOverlay?.visibility = VISIBLE
                            CoroutineHelper.runCoroutineWithDelay({
                                blackOverlay?.visibility = GONE
                            }, 2000, Dispatchers.Main)
                        }
                    }

                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    easLayout?.visibility = GONE
                    (sceneListener as LiveSceneListener).channelDown()

                    lockedLayout?.let {
                        if (it!!.visibility == VISIBLE) {
                            if (!isChannelLocked) {
                                it!!.visibility = GONE
                            }
                            hintMessageContainer?.visibility = GONE
                            blackOverlay?.visibility = VISIBLE
                            CoroutineHelper.runCoroutineWithDelay({
                                blackOverlay?.visibility = GONE
                            }, 2000, Dispatchers.Main)
                        }
                    }

                    return true
                }
                //RCU PREVIOUS CHANNEL
                KeyEvent.KEYCODE_11 -> {
                    (sceneListener as LiveSceneListener).lastActiveChannel()
                    return true
                }

                KeyEvent.KEYCODE_FOCUS -> {
                    if(BuildConfig.FLAVOR.contains("mtk")) {
                        (sceneListener as LiveSceneListener).showTvPreferences()
                    }
                }
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_BACK -> {
                    if ((sceneListener as LiveSceneListener).isTTXActive()) {
                        return true
                    }
                    if (!downActionBackKeyDone) return true
                    if (isShowingPin) {
                        hidePin()
                        return true
                    }

                    if (worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING &&
                        worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE) {
                        InformationBus.submitEvent(Event(Events.SHOW_STOP_RECORDING_DIALOG))
                        return true
                    } else {
                        BackFromPlayback.onOkPressed(true)
                    }
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_RECORD -> {
                    if ((sceneListener as LiveSceneListener).isTTXActive()) return true
                    if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.DEFAULT.ordinal) {
                        if ((sceneListener as LiveSceneListener).getConfigInfo("pvr")) {
                            val isRecordingInProgress =
                                (sceneListener as LiveSceneListener).isRecordingInProgress()
                            if ((sceneListener as LiveSceneListener).isSignalAvailable()) {
                                if (!longPressFlag && !isRecordingInProgress) {
                                    (sceneListener as LiveSceneListener).requestActiveChannel(object :
                                        IAsyncDataCallback<TvChannel> {
                                        override fun onFailed(error: Error) {}

                                        override fun onReceive(activeChannel: TvChannel) {
                                            (sceneListener as LiveSceneListener).requestActiveEvent(
                                                activeChannel,
                                                object : IAsyncDataCallback<TvEvent> {
                                                    override fun onFailed(error: Error) {
                                                        val tvEvent =
                                                            TvEvent.createNoInformationEvent(
                                                                activeChannel,
                                                                (sceneListener as LiveSceneListener).getCurrentTime(
                                                                    activeChannel
                                                                )
                                                            )
                                                        (sceneListener as LiveSceneListener).startRecording(
                                                            tvEvent
                                                        )
                                                    }

                                                    override fun onReceive(data: TvEvent) {
                                                        if (!(sceneListener as LiveSceneListener).isTimeShiftInProgress())
                                                            (sceneListener as LiveSceneListener).startRecording(
                                                                data
                                                            )
                                                        else
                                                            com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                                                                Events.SHOW_RECORDING_TIME_SHIFT_CONFLICT_DIALOG,
                                                                arrayListOf(data)
                                                            )
                                                    }
                                                })
                                        }
                                    })
                                } else {
                                    runOnUiThread {
                                        worldHandler!!.triggerAction(
                                            ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                                            SceneManager.Action.SHOW_OVERLAY
                                        )
                                    }
                                }
                                return true
                            } else (sceneListener as LiveSceneListener).showToast(
                                ConfigStringsManager.getStringById("failed_to_start_pvr")
                            )
                        }
                    } else {
                        (sceneListener as LiveSceneListener).showToast(
                            ConfigStringsManager.getStringById("recording_not_possible")
                        )
                    }
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_STOP ->{
                    val isRecordingInProgress =
                        (sceneListener as LiveSceneListener).isRecordingInProgress()
                    if (isRecordingInProgress){
                        (sceneListener as LiveSceneListener).showStopRecordingDialog("NULL")
                    }
                    return true
                }
                KeyEvent.KEYCODE_LAST_CHANNEL ->{
                    (sceneListener as LiveSceneListener).lastActiveChannel()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    private fun handleChannelUpDown(keyCode: Int): Boolean {
        (sceneListener as LiveSceneListener).channelUp()
        when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                        Events.PVR_RECORDING_SHOULD_STOP,
                        arrayListOf({
                            (sceneListener as LiveSceneListener).channelUp()
                        })
                    )
                    return true
                }


                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                        Events.PVR_RECORDING_SHOULD_STOP,
                        arrayListOf({
                            (sceneListener as LiveSceneListener).channelDown()
                        })
                    )
                    return true
                }
            }
        return false
    }

    private fun handleEasMessage(msgType: Int, eventInfo: EasEventInfo?) {
        if (msgType == 1) {
            isEasPlaying = true
            easLayout?.visibility = VISIBLE
            isEasDisplayed = true
            Log.d(Constants.LogTag.CLTV_TAG + "LiveScene","EVENT_EAS_START eventInfo $eventInfo")
            if (eventInfo != null) {
                val channelChangeUri = eventInfo.channelChangeUri
                val channelChange = eventInfo.isChannelChange
                val alertText = eventInfo.alertText
                Log.d(Constants.LogTag.CLTV_TAG + "LiveScene","channelChangeUri :$channelChangeUri , channelChange :$channelChange , alertText :$alertText ")
                if (!alertText.isNullOrEmpty()) {
                    easAlertMsg?.visibility = VISIBLE
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    easAlertMsg?.text = alertText
                    easAlertMsg?.isSingleLine = true
                    easAlertMsg?.ellipsize = TextUtils.TruncateAt.MARQUEE
                    easAlertMsg?.isSelected = true
                    worldHandler?.isEnableUserInteraction = channelChange == true
                } else {

                    if (BuildConfig.FLAVOR.contains("refplus5")) {
                        //EAS channel with no alert text, disable user interaction if channelChangeUri is not null
                        worldHandler?.isEnableUserInteraction = channelChangeUri == null
                    }
                    easAlertMsg?.visibility = GONE
                }
                if (!TextUtils.isEmpty(eventInfo.activationText)) {
                    easActivationInfo?.visibility = VISIBLE
                    easActivationInfo?.text = eventInfo.activationText
                } else {
                    easActivationInfo?.visibility = View.INVISIBLE
                }
                if (eventInfo.isAtsc3 && (easAlertMsg?.visibility == VISIBLE
                            || easActivationInfo?.visibility == VISIBLE)
                ) {
                    easControl!!.visibility = VISIBLE
                    easControl!!.requestFocus()
                } else {
                    easControl!!.visibility = GONE
                }
            }
        } else if (msgType == 0) {
            Log.d(Constants.LogTag.CLTV_TAG + "LiveScene","EVENT_EAS_STOP")
            isEasPlaying = false
            easLayout?.visibility = GONE
            easAlertMsg?.visibility = GONE
            if (isEasDisplayed || (sceneListener as LiveSceneListener).isTuneToDetailsChannel()) {
                (sceneListener as LiveSceneListener).getEasChannel()
                isEasDisplayed = false
            }
            worldHandler?.isEnableUserInteraction = true
        }
    }

    private fun checkEasStatus() {
        if (getActivity().resources.getString(R.string.atsc30_display_eas) == easControl!!.text.toString()
        ) {
            easControl?.text = getActivity().resources.getString(R.string.atsc30_hide_eas)
            easAlertMsg?.visibility = VISIBLE
            easActivationInfo?.visibility = VISIBLE
        } else {
            easControl?.text = getActivity().resources.getString(R.string.atsc30_display_eas)
            easAlertMsg?.visibility = View.INVISIBLE
            easActivationInfo?.visibility = View.INVISIBLE
        }
    }

    fun updateEventLockInfo() {
        (sceneListener as LiveSceneListener).mutePlayback(false)
        lockedLayout?.visibility = GONE
        hintMessageContainer?.visibility = GONE
        unlockBtn?.setOnClickListener(null)
        unlockBtn?.visibility = GONE
        isShowingPin = false
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBlackOverlayJob?.cancel()
        ReferenceApplication.applicationContext().unregisterReceiver(receiver)
    }


    // --------------------- FAST ZAP BANNER RELATED CODE -------------------------------
    fun isFastZapBannerActive() = fastZapBanner?.visibility == View.VISIBLE

    fun showFastZapBanner() {
        fastZapBanner?.switchVisibility()
    }

    fun hideFastZapBanner() {
        if (isFastZapBannerActive()) fastZapBanner?.switchVisibility()
    }

    fun updateFastZapBanner() {
        fastZapBanner?.updateData()
    }

    fun clearBg() {
        runOnUiThread {
            radioChannelBg?.visibility = GONE
            scrambledChannelBg?.visibility = GONE
            notRunningBg?.visibility = GONE
            statusMessage?.text = ""
            blackOverlay?.visibility = View.GONE
            statusInformationBg?.visibility = GONE
        }
    }

    /**
     * Fast show zap banner broadcast receiver
     */
    private val receiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            if (intent != null && intent.action == FastZapBannerDataProvider.FAST_SHOW_ZAP_BANNER_INTENT) {
                Log.d(Constants.LogTag.CLTV_TAG + "LiveScene", "FAST show zap banner intent received")
                fastZapBanner?.switchVisibility()
            } else if (intent != null && intent.action == FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT) {
                Log.d(Constants.LogTag.CLTV_TAG + "LiveScene", "FAST hide zap banner intent received")
                //if fast zap banner is visible then we need to hide it but vice versa is not true.
                if (fastZapBanner != null) {
                    if (fastZapBanner!!.isVisible) {
                        fastZapBanner?.switchVisibility()
                    }
                    if (isShowingPin) {
                        hidePin()
                    }
                }
            }
        }
    }

    /**
     * whenever fast zap banner will switch the visibility it will set the focus to require position
     */
    var zapBannerFocusCallback ={
            if (unlockBtn?.visibility == VISIBLE && lockedLayout?.visibility == VISIBLE){
                unlockBtn!!.requestFocus()
            }

    }
    //if blue mute should not be visible set blueMuteOverlay background to black
    fun hideBlueMuteBlackOverlay() {
        if ((sceneListener as LiveSceneListener).isBlueMuteEnabled()){
            blueMuteBlackOverlay!!.visibility = VISIBLE
            blueMuteBlackOverlay!!.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor(
                "color_background")
                )
            )
            hideBlueMute()
        }
    }

    //if blue mute should be visible set blueMuteOverlay gone
    fun showBlueMuteBlackOverlay() {
        if ((sceneListener as LiveSceneListener).isBlueMuteEnabled()){
            blueMuteBlackOverlay!!.visibility = GONE
        }
    }

    fun showBlueMute(){
        if ((sceneListener as LiveSceneListener).isBlueMuteEnabled()) {
            blueMuteOverlay!!.visibility = VISIBLE
        }
    }

    fun hideBlueMute(){
        if ((sceneListener as LiveSceneListener).isBlueMuteEnabled()) {
            blueMuteOverlay!!.visibility = GONE
        }
    }

    fun isEventUnlocked(isEventUnlocked: Boolean) {
        this.isEventUnlocked = isEventUnlocked
    }

}