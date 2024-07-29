package com.iwedia.cltv.scene.timeshift

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvTrackInfo
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.leanback.widget.VerticalGridView
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.CheckListAdapter
import com.iwedia.cltv.components.CheckListItem
import com.iwedia.cltv.components.FadeAdapter
import com.iwedia.cltv.config.*
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.VideoResolution
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.player.MediaSessionControl
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.InvalidDataTracker
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import kotlinx.coroutines.Dispatchers
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneManager
import java.util.*

/**
 * Timeshift scene
 *
 * @author Dejan Nadj
 */
class TimeshiftScene(context: Context, sceneListener: TimeshiftSceneListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE),
        sceneListener
    ), View.OnFocusChangeListener, View.OnClickListener {


    /**
     * Scene views and data
     */
    private val TAG = javaClass.simpleName
    private var hideTimer: Timer? = null
    private var hideTimerTask: TimerTask? = null
    private var playPauseWrapper: CardView? = null
    private var nextWrapper: CardView? = null
    private var previousWrapper: CardView? = null
    private var audioWrapper: CardView? = null
    private var subtitlesWrapper: CardView? = null
    private var selectedView: CardView? = null
    private var title: TextView? = null
    private var index: TextView? = null
    private var logo: ImageView? = null
    private var channelName: TextView? = null
    private var parentalRating: TextView? = null
    private var isUHD: ImageView? = null
    private var isHD: ImageView? = null
    private var isFHD: ImageView? = null
    private var isSD: ImageView? = null
    private var isCC: ImageView? = null
    private var isDB: ImageView? = null
    private var isAD: ImageView? = null
    private var audioIcon: ImageView? = null
    private var audioTracksContainer: LinearLayout? = null
    private var subtitleIcon: ImageView? = null
    private var subtitleTracksContainer: LinearLayout? = null
    private var timeText: TextView? = null
    private var startTimeInfo: String = "00:00"
    private var endTimeInfo: String = "00:00"
    private var speedText: TextView? = null
    private var seekBar: SeekBar? = null
    private var bufferPosition: ProgressBar? = null
    var isSeeking = false
    private var isPlaying = true
    private var tracksContainer: LinearLayout? = null
    private var trackCount = 0
    private var tvChannel: Any? = null
    private var referenceTvChannel: TvChannel? = null
    private var audioTracks = mutableListOf<IAudioTrack>()
    private var subtitleTracks = mutableListOf<ISubtitle>()
    private var existingAudioTrackCodes = mutableListOf<String>()
    private var existingSubtitleTrackCodes = mutableListOf<String>()
    private var currentSelectedAudioTrack: IAudioTrack? = null
    private var currentSelectedSubtitleTrack: ISubtitle? = null
    private var backgroundLayout: ConstraintLayout? = null
    private lateinit var isTTX: ImageView
    private var focusedView: View? = null
    private var undefinedAudioTrackCount: Int? = 1

    private lateinit var subtitleTracksCheckListAdapter: CheckListAdapter
    private lateinit var audioTracksCheckListAdapter: CheckListAdapter
    lateinit var fadingEdgeLayout: FadingEdgeLayout

    /**
    tracksGridView is VerticalGridView used for displaying Audio or Subtitle tracks.
     */
    private var tracksVerticalGridView: VerticalGridView? = null

    /**
     * sideViewWrapper is important for handling visibility of the tracksVerticalGridView (used for displaying and hiding Audio or Subtitle tracks).
     */
    var tracksWrapperConstraintLayout: LinearLayout? = null
    private var tracksTitle: TextView? = null

    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(
            name,
            R.layout.layout_timeshift_scene,
            object : GAndroidSceneFragmentListener {
                @SuppressLint("ResourceType")
                override fun onCreated() {
                    try {
                        playPauseWrapper = view!!.findViewById<CardView>(R.id.player_scene_play_pause_wrapper)
                    }catch (E: Exception){
                        ReferenceApplication.runOnUiThread {
                            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        }
                    }
                    speedText = view!!.findViewById(R.id.player_ff_fr_spd)
                    backgroundLayout = view!!.findViewById(R.id.background_layout)

                    val drawableImageView: ImageView =
                        view!!.findViewById(R.id.drawable_player_scene)
                    var drawable = GradientDrawable()
                    drawable.shape = GradientDrawable.RECTANGLE
                    val colorStart = Color.parseColor(
                        ConfigColorManager.getColor("color_background")
                            .replace("#", ConfigColorManager.alfa_zero_per)
                    )
                    val colorMid = Color.parseColor(
                        ConfigColorManager.getColor("color_background")
                            .replace("#", ConfigColorManager.alfa_fifty_per)
                    )
                    val colorEnd = Color.parseColor(
                        ConfigColorManager.getColor("color_background")
                            .replace("#", ConfigColorManager.alfa_hundred_per)
                    )
                    drawable.colors = intArrayOf(
                        colorStart,
                        colorMid,
                        colorEnd
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        drawableImageView.background = drawable
                    } else {
                        drawableImageView.setBackgroundDrawable(drawable)
                    }

                    val subtitlesImageView: ImageView =
                        view!!.findViewById(R.id.player_scene_subtitles)
                    val colorContext =
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    subtitlesImageView.setColorFilter(
                        colorContext
                    )

                    val audioImageView: ImageView = view!!.findViewById(R.id.player_scene_audio)
                    audioImageView.setColorFilter(
                        colorContext
                    )

                    fadingEdgeLayout = view!!.findViewById(R.id.fading_edge_layout)
                    tracksVerticalGridView = view!!.findViewById(R.id.side_view_vertical_grid_view)
                    subtitleTracksCheckListAdapter = CheckListAdapter(
                        fadingEdgeLayout = fadingEdgeLayout,
                        FadeAdapter.FadeAdapterType.VERTICAL
                    )
                    subtitleTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as TimeshiftSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            (sceneListener as TimeshiftSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onItemClicked(position: Int) {
                            //this method is called to restart inactivity timer for no signal power off
                            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                            //this method is called to restart inactivity timer for info banner scene
                            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                            currentSelectedSubtitleTrack = subtitleTracks!![position]
                            (sceneListener as TimeshiftSceneListener).onSubtitleTrackClicked(currentSelectedSubtitleTrack!!)
                        }

                        override fun onAdditionalItemClicked() {
                            (sceneListener as TimeshiftSceneListener).setSubtitles(false)
                        }

                        override fun onDownPressed(position: Int): Boolean {
                            val nextPosition = if (position <= audioTracks!!.size) position + 1 else position
                            tracksVerticalGridView?.layoutManager?.scrollToPosition(nextPosition)
                            tracksVerticalGridView?.layoutManager?.findViewByPosition(nextPosition)?.requestFocus()
                            return true
                        }

                        override fun onUpPressed(position: Int): Boolean {
                            val nextPosition = if (position >= 0) position - 1 else position
                            tracksVerticalGridView?.layoutManager?.scrollToPosition(nextPosition)
                            tracksVerticalGridView?.layoutManager?.findViewByPosition(nextPosition)?.requestFocus()
                            return false
                        }

                        override fun onBackPressed(): Boolean {
                            subtitlesWrapper!!.requestFocus()
                            tracksWrapperConstraintLayout!!.visibility = View.INVISIBLE
                            return true
                        }
                    }
                    tracksWrapperConstraintLayout = view!!.findViewById(R.id.audio_and_subtitles_container)


                    audioTracksCheckListAdapter = CheckListAdapter(
                        fadingEdgeLayout = fadingEdgeLayout,
                        FadeAdapter.FadeAdapterType.VERTICAL
                    )

                    audioTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as TimeshiftSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            (sceneListener as TimeshiftSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onItemClicked(position: Int) {
                            //this method is called to restart inactivity timer for no signal power off
                            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                            //this method is called to restart inactivity timer for info banner scene
                            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                            currentSelectedAudioTrack = audioTracks!![position]
                            (sceneListener as TimeshiftSceneListener).onAudioTrackClicked(currentSelectedAudioTrack!!)
                        }

                        override fun onAdditionalItemClicked() {
                            // NOT IMPORTANT for Audio tracks. There is no "Off" button in Audio.
                        }

                        override fun onDownPressed(position: Int): Boolean {
                            val nextPosition = if (position <= audioTracks!!.size) position + 1 else position
                            tracksVerticalGridView?.layoutManager?.scrollToPosition(nextPosition)
                            tracksVerticalGridView?.layoutManager?.findViewByPosition(nextPosition)?.requestFocus()
                            return true
                        }

                        override fun onUpPressed(position: Int): Boolean {
                            val nextPosition = if (position >= 0) position - 1 else position
                            tracksVerticalGridView?.layoutManager?.scrollToPosition(nextPosition)
                            tracksVerticalGridView?.layoutManager?.findViewByPosition(nextPosition)?.requestFocus()
                            return true
                        }

                        override fun onBackPressed(): Boolean {
                            audioWrapper!!.requestFocus()
                            tracksWrapperConstraintLayout!!.visibility = View.INVISIBLE
                            return true
                        }
                    }

                    Utils.makeGradient(
                        view = view!!.findViewById(R.id.audio_and_subtitles_gradient_view),
                        type = GradientDrawable.LINEAR_GRADIENT,
                        orientation = GradientDrawable.Orientation.RIGHT_LEFT,
                        startColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
                        endColor = Color.TRANSPARENT,
                        centerX = 0.8f,
                        centerY = 0f
                    )
                    Utils.makeGradient(
                        view = view!!.findViewById(R.id.audio_and_subtitles_linear_layout),
                        type = GradientDrawable.LINEAR_GRADIENT,
                        orientation = GradientDrawable.Orientation.RIGHT_LEFT,
                        startColor = Color.parseColor(ConfigColorManager.getColor("color_dark")),
                        endColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
                        centerX = 0.8f,
                        centerY = 0f
                    )

                    tracksTitle = view!!.findViewById(R.id.title)
                    tracksTitle!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_medium")
                    )
                    tracksTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))


                    audioWrapper = view!!.findViewById<CardView>(R.id.player_scene_audio_wrapper)
                    audioWrapper!!.setCardBackgroundColor(
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                "color_background"
                            )
                        )
                    )
                    subtitlesWrapper =
                        view!!.findViewById<CardView>(R.id.player_scene_subtitles_wrapper)
                    subtitlesWrapper!!.setCardBackgroundColor(
                        Color.parseColor(
                            ConfigColorManager.getColor(
                                "color_background"
                            )
                        )
                    )

                    title = view!!.findViewById(R.id.player_scene_title)
                    title!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_medium")
                    )
                    title!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    index = view!!.findViewById(R.id.player_scene_index)
                    index!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_regular")
                    )
                    index!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    logo = view!!.findViewById(R.id.player_scene_logo)
                    channelName = view!!.findViewById(R.id.player_scene_channel_name)
                    channelName!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_bold")
                    )
                    channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                    timeText = view!!.findViewById(R.id.player_scene_time)
                    timeText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

                    bufferPosition = view!!.findViewById(R.id.player_scene_buffer_position)
                    bufferPosition?.progress = 0

                    try {
                        bufferPosition!!.progressBackgroundTintList =
                            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: Exception color rdb $ex")
                    }
                    bufferPosition!!.progressTintList = ColorStateList.valueOf(
                        Color.parseColor(
                            ConfigColorManager.getColor("color_progress")
                                .replace("#", ConfigColorManager.alfa_light)
                        )
                    )

                    isUHD = view!!.findViewById(R.id.is_uhd)
                    isHD = view!!.findViewById(R.id.is_hd)
                    isFHD = view!!.findViewById(R.id.is_fhd)
                    isSD = view!!.findViewById(R.id.is_sd)
                    isCC = view!!.findViewById(R.id.cc)
                    isDB = view!!.findViewById(R.id.dolby)
                    isAD = view!!.findViewById(R.id.audio_description)
                    isTTX = view!!.findViewById(R.id.ttx_icon)
                    audioIcon = view!!.findViewById(R.id.audio)
                    audioTracksContainer = view!!.findViewById(R.id.audioTrackContainer)
                    parentalRating = view!!.findViewById(R.id.parental_rating)
                    parentalRating!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_regular")
                    )
                    subtitleIcon = view!!.findViewById(R.id.subtitle)
                    subtitleTracksContainer = view!!.findViewById(R.id.subtitleTrackContainer)

                    isSD!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    isHD!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    isFHD!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    isUHD!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    isCC!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    isDB!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    isAD!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    audioIcon!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    parentalRating!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    subtitleIcon!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    isTTX.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
                    seekBar = view!!.findViewById<AppCompatSeekBar>(R.id.player_scene_seek_bar)
                    try {
                        seekBar!!.backgroundTintList =
                            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_progress")))
                        seekBar!!.progressTintList =
                            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_progress")))
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: Exception color rdb $ex")
                    }

                    seekBar!!.getThumb()
                        .setTint(Color.parseColor(ConfigColorManager.getColor("color_selector")))

                    seekBar?.progress = 0
                    seekBar!!.onFocusChangeListener =
                        View.OnFocusChangeListener { view, hasFocus ->
                            if (hasFocus) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: $hasFocus")
                                focusedView = view
                                try {
                                    seekBar!!.backgroundTintList = ColorStateList.valueOf(
                                        Color.parseColor(
                                            ConfigColorManager.getColor("color_progress")
                                        )
                                    )
                                    seekBar!!.progressTintList = ColorStateList.valueOf(
                                        Color.parseColor(
                                            ConfigColorManager.getColor("color_progress")
                                        )
                                    )
                                } catch (ex: Exception) {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: Exception color rdb $ex")
                                }
                            } else {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: $hasFocus")
                                try {
                                    seekBar!!.backgroundTintList = ColorStateList.valueOf(
                                        Color.parseColor(
                                            ConfigColorManager.getColor("color_progress")
                                        )
                                    )
                                    seekBar!!.progressTintList = ColorStateList.valueOf(
                                        Color.parseColor(
                                            ConfigColorManager.getColor("color_progress")
                                        )
                                    )
                                } catch (ex: Exception) {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: Exception color rdb $ex")
                                }
                            }
                        }
                    seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            var progress = p0!!.progress
                            if (progress > bufferPosition!!.progress) {
                                progress = bufferPosition!!.progress
                                p0!!.progress = progress
                            }
                            if (isSeeking) {
                                (sceneListener as TimeshiftSceneListener).onSeek(progress)
                            }
                        }

                        override fun onStartTrackingTouch(p0: SeekBar?) {}

                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                    seekBar!!.setOnKeyListener(object : View.OnKeyListener {
                        override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                            if (keyEvent!!.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                                resetHideTimer()
                                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                    if ((sceneListener as TimeshiftSceneListener).isTimeShiftAvailable()) {
                                        // maintaining repeatCount to handle key isLongPress
                                        if (keyEvent.repeatCount > 0) {
                                            (sceneListener as TimeshiftSceneListener).onPreviousClicked(
                                                true, keyEvent.repeatCount
                                            )
                                        }
                                    }
                                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if ((sceneListener as TimeshiftSceneListener).isTimeShiftAvailable()) {
                                        if (keyEvent.repeatCount > 0) {
                                            (sceneListener as TimeshiftSceneListener).onNextClicked(
                                                true, keyEvent.repeatCount
                                            )
                                        }
                                    }
                                }
                                return true
                            }
                            else if (keyEvent.action == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                                resetHideTimer()
                                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                    if ((sceneListener as TimeshiftSceneListener).isTimeShiftAvailable()) {
                                        // maintaining repeatCount to handle key isLongPress
                                        if (keyEvent.repeatCount == 0) {
                                            (sceneListener as TimeshiftSceneListener).onPreviousClicked(
                                                false,0
                                            )
                                        }
                                    }
                                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if ((sceneListener as TimeshiftSceneListener).isTimeShiftAvailable()) {
                                        if (keyEvent.repeatCount == 0) {
                                            (sceneListener as TimeshiftSceneListener).onNextClicked(
                                                false, 0
                                            )
                                        }
                                    }
                                }
                                keyEvent!!.action == KeyEvent.ACTION_UP
                                return true
                            }
                            return false
                        }
                    })

                    playPauseWrapper!!.onFocusChangeListener = this@TimeshiftScene
                    audioWrapper!!.onFocusChangeListener = this@TimeshiftScene
                    subtitlesWrapper!!.onFocusChangeListener = this@TimeshiftScene

                    playPauseWrapper!!.setOnClickListener(this@TimeshiftScene)
                    subtitlesWrapper!!.setOnClickListener(this@TimeshiftScene)
                    audioWrapper!!.setOnClickListener(this@TimeshiftScene)

                    drawable = GradientDrawable()
                    drawable.shape = GradientDrawable.RECTANGLE
                    drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                    drawable.colors = intArrayOf(
                        colorStart,
                        colorMid,
                        colorEnd
                    )

                    playPauseWrapper!!.post {
                        playPauseWrapper!!.requestFocus()
                    }

                    tracksContainer = view!!.findViewById(R.id.tracksContainer)

                    sceneListener.onSceneInitialized()
                    if (isPlaying)
                        startHideTimer()

                }
            }
        )
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun onFocusChange(view: View?, hasFocus: Boolean) {
        if (hasFocus) {
            focusedView = view
        }
        if (view is CardView) {

            when (hasFocus) {
                true -> {
                    view.setCardBackgroundColor(
                        Color.parseColor(ConfigColorManager.getColor("color_selector"))
                    )
                    Utils.focusAnimation(view)

                }
                false -> {
                    view.setCardBackgroundColor(
                        Color.parseColor(ConfigColorManager.getColor("color_background"))
                    )
                    Utils.unFocusAnimation(view)
                }
            }

            if (view[0] is ImageView) {
                val imageView: ImageView = view[0] as ImageView
                when (hasFocus) {
                    true -> {
                        try {
                            val colorContext =
                                Color.parseColor(ConfigColorManager.getColor("color_background"))
                            imageView.setColorFilter(
                                colorContext
                            )
                            Utils.focusAnimation(view)
                        } catch (ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                        }
                    }
                    false -> {
                        try {
                            val colorContext =
                                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                            imageView.setColorFilter(
                                colorContext
                            )
                            Utils.unFocusAnimation(view)

                        } catch (ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceType")
    override fun onClick(view: View?) {
        Utils.viewClickAnimation(view!!, object : AnimationListener {
            override fun onAnimationEnd() {
                resetHideTimer()
                if (view is CardView) {
                    when (view) {
                        playPauseWrapper -> {
                            (sceneListener as TimeshiftSceneListener).onPlayPauseClicked()
                            return
                        }
                        previousWrapper -> {
                            (sceneListener as TimeshiftSceneListener).onPreviousClicked(true, 0)
                            return
                        }
                        nextWrapper -> {
                            (sceneListener as TimeshiftSceneListener).onNextClicked(true, 0)
                            return
                        }
                        audioWrapper -> {
                            tracksVerticalGridView!!.adapter = audioTracksCheckListAdapter
                            undefinedAudioTrackCount = 1
                            if (audioTracks.size == 0) {
                                (sceneListener as TimeshiftSceneListener).showToast(ConfigStringsManager.getStringById("no_available_audio_tracks_msg"))
                                return
                            }

                            selectedView = audioWrapper

                            var audioTrackObjects = mutableListOf<CheckListItem>()
                            audioTracksCheckListAdapter.refresh(audioTrackObjects)
                            audioTracks.forEach { item ->
                                val infoIcons = mutableListOf<Int>()

                                if (item.isAd) {
                                    infoIcons.add(R.drawable.ic_ad)
                                }
                                if (item.isDolby) {
                                    infoIcons.add(R.drawable.ic_dolby)
                                }
                                if((sceneListener as TimeshiftSceneListener).getCurrentAudioTrack() == item){
                                    audioTrackObjects.add(CheckListItem(item.trackName, true, infoIcons))
                                    currentSelectedAudioTrack = item
                                }else{
                                    audioTrackObjects.add(CheckListItem(item.trackName, false, infoIcons))
                                }
                            }

                            tracksTitle!!.text = ConfigStringsManager.getStringById("audio")
                            tracksWrapperConstraintLayout!!.post {
                                tracksWrapperConstraintLayout!!.visibility = View.VISIBLE
                            }

                            audioTracksCheckListAdapter.refresh(audioTrackObjects)

                            tracksWrapperConstraintLayout?.postDelayed(Runnable {
                                tracksVerticalGridView?.layoutManager?.scrollToPosition(0)
                                tracksVerticalGridView?.layoutManager?.findViewByPosition(0)?.requestFocus()
                                tracksVerticalGridView?.requestFocus()
                            }, 100)

                            try {
                                val colorLight = Color.parseColor(
                                    ConfigColorManager.getColor("color_main_text")
                                        .replace("#", ConfigColorManager.alfa_light)
                                )
                                audioWrapper!!.setCardBackgroundColor(
                                    ColorStateList.valueOf(
                                        ContextCompat.getColor(context, colorLight)
                                    )
                                )
                            } catch (E: Exception) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onAnimationEnd: $E")
                            }

                            if (hideTimer != null) {
                                hideTimer?.cancel()
                                hideTimer?.purge()
                                hideTimerTask?.cancel()
                                hideTimer = null
                            }
                            return
                        }
                        subtitlesWrapper -> {
                            tracksVerticalGridView!!.adapter = subtitleTracksCheckListAdapter
                            undefinedAudioTrackCount = 1
                            if (subtitleTracks.size == 0) {
                                (sceneListener as TimeshiftSceneListener).showToast(ConfigStringsManager.getStringById("no_available_subtitle_tracks_msg"))
                                return
                            }

                            selectedView = subtitlesWrapper

                            val subtitleCheckListItems = mutableListOf<CheckListItem>()
                            subtitleTracksCheckListAdapter.refresh(subtitleCheckListItems)
                            subtitleTracks.forEach { item ->
                                val infoIcons = mutableListOf<Int>()
                                if (item.isHoh){
                                    infoIcons.add(R.drawable.ic_hoh)
                                }
                                if(item.isTxtBased) {
                                    infoIcons.add(R.drawable.ic_ttx)
                                }
                                if((sceneListener as TimeshiftSceneListener).getCurrentSubtitleTrack() == item){
                                    subtitleCheckListItems.add(CheckListItem(item.trackName, true, infoIcons))
                                    currentSelectedSubtitleTrack = item
                                }else{
                                    subtitleCheckListItems.add(CheckListItem(item.trackName, false, infoIcons))
                                }
                            }
                            tracksTitle!!.text = ConfigStringsManager.getStringById("subtitles")

                            tracksWrapperConstraintLayout!!.post {
                                tracksWrapperConstraintLayout!!.visibility = View.VISIBLE
                            }

                            subtitleTracksCheckListAdapter.refreshWithAdditionalItem(
                                adapterItems = subtitleCheckListItems,
                                name = ConfigStringsManager.getStringById("off"),
                                isChecked = false
                            )

                            tracksWrapperConstraintLayout?.postDelayed(Runnable {
                                tracksVerticalGridView?.layoutManager?.scrollToPosition(0)
                                tracksVerticalGridView?.layoutManager?.findViewByPosition(0)?.requestFocus()
                                tracksVerticalGridView?.requestFocus()
                            }, 100)

                            try {
                                val colorLight = Color.parseColor(
                                    ConfigColorManager.getColor("color_main_text")
                                        .replace("#", ConfigColorManager.alfa_light)
                                )
                                subtitlesWrapper!!.setCardBackgroundColor(
                                    ColorStateList.valueOf(
                                        ContextCompat.getColor(context, colorLight)
                                    )
                                )
                            } catch (E: Exception) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onAnimationEnd: $E")
                            }

                            if (hideTimer != null) {
                                hideTimer?.cancel()
                                hideTimer?.purge()
                                hideTimerTask?.cancel()
                                hideTimer = null
                            }
                            return
                        }
                    }
                }
            }
        })
    }

    override fun refresh(data: Any?) {
        super.refresh(data)
        if (data is Boolean) {
            val imageView = playPauseWrapper!![0] as ImageView
            if (data) {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.play
                    )
                )
                isPlaying = false
            } else {
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.pause
                    )
                )
                isPlaying = true
            }
            startHideTimer()
        }

        if (Utils.isListDataType(data, IAudioTrack::class.java)) {
            audioTracks = data as MutableList<IAudioTrack>
        }

        if (Utils.isListDataType(data, ISubtitle::class.java)) {
            subtitleTracks = data as MutableList<ISubtitle>
        }

        if (data is IAudioTrack) {
            if (audioTracks.size > 0) {
                run exitForEach@{
                    audioTracks.forEachIndexed { index, item ->
                        if (item.trackId == data.trackId) {
                            currentSelectedAudioTrack = data
                            return@exitForEach
                        }
                    }
                }
            }
        }

        if (data is ISubtitle) {
            if (subtitleTracks.size > 0) {
                run exitForEach@{
                    subtitleTracks.forEachIndexed { index, item ->
                        if (item.trackId == data.trackId) {
                            currentSelectedSubtitleTrack = data
                            return@exitForEach
                        }
                    }
                }
            }
        }

        if (data is TvChannel) {
            referenceTvChannel = data as TvChannel?
            var resolution: VideoResolution? = null
            var videoResolution = (sceneListener as TimeshiftSceneListener).getVideoResolution()
            if (videoResolution == "UHD")
                resolution = VideoResolution.VIDEO_RESOLUTION_UHD
            else if (videoResolution == "HD") {
                if(videoResolution == "1080p" || videoResolution == "1080i"){
                    resolution = VideoResolution.VIDEO_RESOLUTION_FHD
                }else{
                    resolution = VideoResolution.VIDEO_RESOLUTION_HD
                }
            }
            else if (videoResolution == "FHD")
                resolution = VideoResolution.VIDEO_RESOLUTION_FHD
            else if (videoResolution == "ED")
                resolution = VideoResolution.VIDEO_RESOLUTION_ED
            else if (videoResolution == "SD")
                resolution = VideoResolution.VIDEO_RESOLUTION_SD


            if (resolution == VideoResolution.VIDEO_RESOLUTION_UHD) {
                isUHD!!.visibility = View.VISIBLE
            } else {
                isUHD!!.visibility = View.GONE
            }
            if (resolution == VideoResolution.VIDEO_RESOLUTION_HD) {
                isHD!!.visibility = View.VISIBLE
            } else {
                isHD!!.visibility = View.GONE
            }
            if (resolution == VideoResolution.VIDEO_RESOLUTION_FHD) {
                isFHD!!.visibility = View.VISIBLE
            } else {
                isFHD!!.visibility = View.GONE
            }
            if (resolution == VideoResolution.VIDEO_RESOLUTION_ED || resolution == VideoResolution.VIDEO_RESOLUTION_SD
            ) {
                isSD!!.visibility = View.VISIBLE
            } else {
                isSD!!.visibility = View.GONE
            }

            if ((sceneListener as TimeshiftSceneListener)!!.getIsDolby(TvTrackInfo.TYPE_AUDIO)) {
                isDB!!.visibility = View.VISIBLE
            } else {
                isDB!!.visibility = View.GONE
            }
            if ((sceneListener as TimeshiftSceneListener)!!.getIsCC(TvTrackInfo.TYPE_AUDIO)) {
                isCC!!.visibility = View.VISIBLE
            } else {
                isCC!!.visibility = View.GONE
            }
            if ((sceneListener as TimeshiftSceneListener)!!.getIsAudioDescription(TvTrackInfo.TYPE_AUDIO)) {
                isAD!!.visibility = View.VISIBLE
            } else {
                isAD!!.visibility = View.GONE
            }
            if ((sceneListener as TimeshiftSceneListener)!!.getTeleText(TvTrackInfo.TYPE_SUBTITLE)) {
                if ((sceneListener as TimeshiftSceneListener).getConfigInfo("teletext_enable_column")) {
                    isTTX.visibility = View.VISIBLE
                } else {
                    isTTX.visibility = View.GONE
                }
            } else {
                isTTX.visibility = View.GONE
            }

            audioTracks = (sceneListener as TimeshiftSceneListener).getAvailableAudioTracks()
            if (audioTracks != null) {
                if (audioTracks!!.size != 0) {
                    audioTracks!!.forEach { item ->
                        if (!existingAudioTrackCodes.contains(formatLanguageCode(item.languageName))) {
                            addTrackInfo(item.languageName, true)
                        }
                    }
                } else {
                    audioTracksContainer!!.visibility = View.GONE
                    audioIcon!!.visibility = View.GONE
                }
            } else {
                audioTracksContainer!!.visibility = View.GONE
                audioIcon!!.visibility = View.GONE
            }

            subtitleTracks = (sceneListener as TimeshiftSceneListener).getAvailableSubtitleTracks()
            if (subtitleTracks != null) {
                if (subtitleTracks!!.size != 0) {
                    subtitleTracks!!.forEach { item ->
                        if (!existingSubtitleTrackCodes.contains(formatLanguageCode(item.languageName))) {
                            addTrackInfo(item.languageName, false)
                        }
                    }
                } else {
                    subtitleTracksContainer!!.visibility = View.GONE
                    subtitleIcon!!.visibility = View.GONE
                }
            } else {
                subtitleTracksContainer!!.visibility = View.GONE
                subtitleIcon!!.visibility = View.GONE
            }
        }
        if (data is TvEvent) {
            tvChannel = data.tvChannel
            title!!.text = data.name
            title!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

            var list: List<String> = listOf()
            if (data!!.parentalRating != null)
                list = data!!.parentalRating!!.split("/")
            var rating = if (list.size > 2) list[2].split("_").last().trim() else ""
            parentalRating!!.text = if (rating == "U" || rating == "0" || rating == null || rating == "null") "" else rating

            index!!.text = data.tvChannel.getDisplayNumberText()
            channelName!!.text = data.tvChannel.name
            setProgress(0,0)
            title!!.text = data.name
            setBufferProgress(0,0)
            if (InvalidDataTracker.hasValidData(data.tvChannel)) {
                val channelLogoPath = data.tvChannel.logoImagePath

                if (channelLogoPath != null) {
                    Utils.loadImage(
                        channelLogoPath,
                        logo!!,
                        object : AsyncReceiver {
                            override fun onFailed(error: core_entities.Error?) {
                                logo!!.visibility = View.INVISIBLE
                                channelName!!.visibility = View.VISIBLE
                            }

                            override fun onSuccess() {
                                logo!!.visibility = View.VISIBLE
                                channelName!!.visibility = View.GONE
                            }
                        })
                } else {
                    logo!!.visibility = View.INVISIBLE
                    channelName!!.visibility = View.VISIBLE
                }
            } else {
                logo!!.visibility = View.INVISIBLE
                channelName!!.visibility = View.VISIBLE
            }
        }
        setSubtitleOptionVisibility()
    }

    /**
     * Set start time
     */
    fun setStartTime(time: String) {
        if (!isSeeking) {
            startTimeInfo = time
            "$startTimeInfo / $endTimeInfo".also { timeText!!.text = it }
        }
    }

    /**
     * Set end time
     */
    fun setEndTime(time: String) {
        endTimeInfo = time
        "$startTimeInfo / $endTimeInfo".also { timeText!!.text = it }
    }

    /**
     * Set progress
     */
    fun setProgress(progress: Int, maxProgress: Int) {
        if (!isSeeking) {
            seekBar!!.progress = progress
        }
        seekBar!!.max = maxProgress
    }

    /**
     * Set buffer progress position
     *
     * @param progress
     */
    fun setBufferProgress(progress: Int, maxProgress: Int) {
        if (!isSeeking) {
            bufferPosition!!.progress = progress
        }
        bufferPosition!!.max = maxProgress
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if((sceneListener as TimeshiftSceneListener).isDialogSceneOpenUsb()){
            return !(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
        }

        // Skip key handling if the close dialog is shown
        if ((sceneListener as TimeshiftSceneListener).isCloseDialogShown()) {
            return false
        }
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                (sceneListener as TimeshiftSceneListener).onStopClicked()
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                InformationBus.submitEvent(Event(Events.FOCUS_ON_BACK_PRESS, true))
                if (view?.view?.visibility == View.INVISIBLE) {
                    return sceneListener.onBackPressed()
                }
                if((sceneListener as TimeshiftSceneListener).isTimeshiftStarted()) {
                    view?.view?.visibility = View.INVISIBLE
                    (sceneListener as TimeshiftSceneListener).setIndicator(
                        true
                    )
                } else {
                    view?.view?.visibility = View.INVISIBLE
                    (sceneListener as TimeshiftSceneListener).setIndicator(
                        false
                    )
                    sceneListener.onBackPressed()
                }
                return true
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                resetHideTimer()
                return true

            }
        }
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (seekBar!!.hasFocus()) {
                        playPauseWrapper?.requestFocus()
                        return true
                    } else if (playPauseWrapper!!.hasFocus()) {
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    playPauseWrapper?.callOnClick()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    playPauseWrapper?.callOnClick()
                    return true
                }

                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    playPauseWrapper?.callOnClick()
                    return true
                }

            }

            if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                if (view?.view?.visibility == View.INVISIBLE) {
                    view?.view?.visibility = View.VISIBLE
                }
                var index = MediaSessionControl.changeSpeedArr.indexOf(MediaSessionControl.currentSpeedValue)
                index--
                if(index >= 0){
                    MediaSessionControl.currentSpeedValue =  MediaSessionControl.changeSpeedArr[index]
                    handleShiftPosition(MediaSessionControl.currentSpeedValue)
                }
                return true
            }
            if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ) {
                if (view?.view?.visibility == View.INVISIBLE) {
                    view?.view?.visibility = View.VISIBLE
                }

                var index = MediaSessionControl.changeSpeedArr.indexOf(MediaSessionControl.currentSpeedValue)
                index++
                if(index < MediaSessionControl.changeSpeedArr.size){
                    MediaSessionControl.currentSpeedValue =  MediaSessionControl.changeSpeedArr[index]
                    handleShiftPosition(MediaSessionControl.currentSpeedValue)
                }
                return true
            }


            if (view?.view?.visibility == View.INVISIBLE) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    worldHandler!!.playbackState =
                        ReferenceWorldHandler.PlaybackState.TIME_SHIFT
                    (sceneListener as TimeshiftSceneListener).setIndicator(
                        true
                    )
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE,
                        SceneManager.Action.HIDE
                    )

                    CoroutineHelper.runCoroutine({
                        (sceneListener as TimeshiftSceneListener).showHomeScene()
                    }, Dispatchers.Main)

                    return true
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    playPauseWrapper?.callOnClick()
                    return true
                }

                if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    playPauseWrapper?.callOnClick()
                    return true
                }
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    playPauseWrapper?.callOnClick()
                    return true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    (sceneListener as TimeshiftSceneListener).onLeftKeyPressed()
                    return true
                }

                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_INFO) {
                    (sceneListener as TimeshiftSceneListener).onRightKeyPressed()
                    return true
                }

                if (keyCode != KeyEvent.KEYCODE_BACK) {
                    resetHideTimer()
                    return true
                }
            }
            // Reset hide timer for all keys except back
            // Back key is handled inside the KeyEvent.ACTION_UP
            if (keyCode != KeyEvent.KEYCODE_BACK) {
                resetHideTimer()
            }
        }
        if((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    (sceneListener as TimeshiftSceneListener).onChannelUp()
                    return true
                }

                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    (sceneListener as TimeshiftSceneListener).onChannelDown()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    private fun startHideTimer() {
        if (hideTimer == null) {
            hideTimer = Timer()
        }
        view?.view?.visibility = View.VISIBLE
        (sceneListener as TimeshiftSceneListener).setIndicator(false)
        if (focusedView != null) {
            focusedView?.requestFocus()
        }
        hideTimerTask = object : TimerTask() {
            override fun run() {
                if (!(sceneListener as TimeshiftSceneListener).isTimeshiftStarted()) {
                    ReferenceApplication.runOnUiThread(Runnable {
                        sceneListener.onBackPressed()
                        (sceneListener as TimeshiftSceneListener).setIndicator(false)
                    })
                } else {
                    ReferenceApplication.runOnUiThread(Runnable {
                        InformationBus.submitEvent(Event(Events.FOCUS_ON_BACK_PRESS, true))
                        view?.view?.visibility = View.INVISIBLE
                        (sceneListener as TimeshiftSceneListener).setIndicator(true)
                    })
                }
            }
        }
        hideTimer?.schedule(hideTimerTask, 6000)
    }

    fun stopHideTimer() {
        if (hideTimer != null) {
            hideTimer?.cancel()
            hideTimer?.purge()
            hideTimerTask?.cancel()
            hideTimer = null
        }
        ReferenceApplication.runOnUiThread(Runnable {
            view?.view?.visibility = View.INVISIBLE
        })
    }

    fun resetHideTimer() {
        stopHideTimer()
        startHideTimer()
    }

    fun showSubtitleList() {
        subtitlesWrapper?.callOnClick()
    }

    private fun setSubtitleOptionVisibility() {
        if (!(sceneListener as TimeshiftSceneListener).isSubtitleEnabled())
            subtitlesWrapper?.visibility = View.GONE
        else subtitlesWrapper?.visibility = View.VISIBLE
    }

    fun showAudioList() {
        audioWrapper?.callOnClick()
    }

    override fun onPause() {
        stopHideTimer()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        resetHideTimer()
    }

    override fun onDestroy() {
        /**
         * When timeshift itself has not been started, we dont have necessity to stoptimeshift
         */
        if ((sceneListener as TimeshiftSceneListener).isTimeshiftStarted()) {
            (sceneListener as TimeshiftSceneListener).stopTimeshift()
        }
        stopHideTimer()
        super.onDestroy()
    }

    private fun formatLanguageCode(languageCode: String): String {
        if (languageCode.length > 2) {
            return languageCode.uppercase(Locale.getDefault()).substring(0, 2)
        } else {
            return languageCode.uppercase(Locale.getDefault())
        }
    }

    private fun addTrackInfo(languageCode: String, isAudioTrack: Boolean) {
        var trackView = TextView(context)

        var layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        if (trackCount > 0) {
            Utils.getDimensInPixelSize(R.dimen.custom_dim_13_5)
        }

        trackView.gravity = Gravity.CENTER_VERTICAL
        trackView.setPadding(0, 0, 0, 0)
        layoutParams.rightMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_5)
        trackView.layoutParams = layoutParams


        trackView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            14f
        )

        var formattedLanguageCode = formatLanguageCode(languageCode)
        if (languageCode == "")
            trackView.text = "UN"
        else
            trackView.text = formattedLanguageCode


        trackView.setTextColor(
            Color.parseColor(ConfigColorManager.getColor("color_text_description"))
        )
        trackView.typeface = TypeFaceProvider.getTypeFace(
            context!!,
            ConfigFontManager.getFont("font_bold")
        )

        if (isAudioTrack)
            existingAudioTrackCodes.add(formattedLanguageCode)
        else
            existingSubtitleTrackCodes.add(formattedLanguageCode)

        if (isAudioTrack) {
            audioIcon!!.visibility = View.VISIBLE
            audioTracksContainer!!.visibility = View.VISIBLE
            audioTracksContainer!!.addView(trackView)
        }
        if (!isAudioTrack) {
            subtitleIcon!!.visibility = View.VISIBLE
            subtitleTracksContainer!!.visibility = View.VISIBLE
            subtitleTracksContainer!!.addView(trackView)
        }
        trackCount++
    }

    /**
     * Updates play pause icon when time shift is automatically started when time shift buffer limit is reached
     */
    fun onTimeShiftStarted() {
        val imageView = playPauseWrapper!![0] as ImageView
        imageView.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.pause
            )
        )
        resetHideTimer()
        isPlaying = true
    }

    fun updateTimeShiftSpeedText(speed: Int) {
        if(speed == MediaSessionControl.SPEED_FF_1X) {
            speedText?.setText("")
            MediaSessionControl.currentSpeedValue = MediaSessionControl.SPEED_FF_1X
        }else{
            speedText?.visibility = View.VISIBLE
            val spd = buildString { append(speed).append("X") }
            speedText?.setText(spd)
        }
        if(isPlaying) {
            updateHideTimer(speed)
        }
    }

    private fun updateHideTimer(speed: Int) {
        if(speed == MediaSessionControl.SPEED_FF_1X) {
            if (hideTimer == null) {
                resetHideTimer()
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " HideTimer is not exist so resetHideTimer")
            }else{
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Continue with existing HideTimer")
            }
        }else{
            if (hideTimer != null) {
                hideTimer?.cancel()
                hideTimer?.purge()
                hideTimerTask?.cancel()
                hideTimer = null
            }
        }
    }

    private fun handleShiftPosition(speed: Int) {
        if (speed > 0) {
            (sceneListener as TimeshiftSceneListener).onFastForward(speed)
        }
        if (speed < 0 ) {
            (sceneListener as TimeshiftSceneListener).onRewind(speed)
        }
    }
}