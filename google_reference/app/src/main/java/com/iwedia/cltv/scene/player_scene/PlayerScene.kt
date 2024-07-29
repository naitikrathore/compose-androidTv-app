package com.iwedia.cltv.scene.player_scene

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
import com.iwedia.cltv.*
import com.iwedia.cltv.components.CheckListAdapter
import com.iwedia.cltv.components.CheckListItem
import com.iwedia.cltv.components.FadeAdapter
import com.iwedia.cltv.config.*
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.VideoResolution
import com.iwedia.cltv.platform.model.player.MediaSessionControl
import com.iwedia.cltv.platform.model.player.MediaSessionControl.currentSpeedValue
import com.iwedia.cltv.platform.model.player.MediaSessionControl.getPlaybackSpeed
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.*
import com.iwedia.cltv.utils.InvalidDataTracker
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import core_entities.Error
import listeners.AsyncReceiver
import java.util.*

/**
 * PlayerScene
 *
 * @author Aleksandar Milojevic
 */
class PlayerScene(context: Context, sceneListener: PlayerSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.PLAYER_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.PLAYER_SCENE),
    sceneListener
), View.OnFocusChangeListener, View.OnClickListener {

    private val TAG = javaClass.simpleName

    var hideTimer: Timer? = null
    var hideTimerTask: TimerTask? = null
    var playPauseWrapper: CardView? = null
    var nextWrapper: CardView? = null
    var previousWrapper: CardView? = null

    var audioWrapper: CardView? = null
    var subtitlesWrapper: CardView? = null
    var selectedView: CardView? = null

    var playerTitle: TextView? = null
    var playerIndex: TextView? = null
    var playerLogo: ImageView? = null
    var playerChannelName: TextView? = null
    var parentalRating: TextView? = null
    var isUHD: ImageView? = null
    var isHD: ImageView? = null
    var isFHD: ImageView? = null
    var isSD: ImageView? = null
    var isCC: ImageView? = null
    var isDB: ImageView? = null
    var isAD: ImageView? = null
    var audioIcon: ImageView? = null
    var audioTracksContainer: LinearLayout? = null
    var subtitleIcon: ImageView? = null
    var subtitleTracksContainer: LinearLayout? = null

    var timeText: TextView? = null
    var startTimeInfo: String = "00:00"
    var endTimeInfo: String = "00:00"
    var speedText: TextView? = null
    var seekBar: SeekBar? = null
    var bufferPosition: ProgressBar? = null
    var isSeeking = false

    var isPlaying = true

    var tracksContainer: LinearLayout? = null
    var trackCount = 0

    var tvChannel: Any? = null
    var referenceTvChannel: TvChannel? = null
    var audioTracks = mutableListOf<IAudioTrack>()
    var subtitleTracks = mutableListOf<ISubtitle>()
    var existingAudioTrackCodes = mutableListOf<String>()
    var existingSubtitleTrackCodes = mutableListOf<String>()
    var currentSelectedAudioTrack: IAudioTrack? = null
    var currentSelectedSubtitleTrack: ISubtitle? = null
    private var backgroundLayout: ConstraintLayout? = null
    lateinit var isTTX: ImageView
    var focusedView: View? = null
    var isEighteenPlus = false

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
            R.layout.layout_player_scene,
            object : GAndroidSceneFragmentListener {
                @SuppressLint("ResourceType")
                override fun onCreated() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: ")
                    playPauseWrapper = view!!.findViewById<CardView>(R.id.player_scene_play_pause_wrapper)
                    speedText = view!!.findViewById(R.id.player_ff_fr_spd)
                    backgroundLayout = view!!.findViewById(R.id.background_layout)

                    val drawable_player_scene: ImageView =
                        view!!.findViewById(R.id.drawable_player_scene)
                    var drawable = GradientDrawable()
                    drawable.setShape(GradientDrawable.RECTANGLE)
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
                    drawable.setColors(
                        intArrayOf(
                            colorStart,
                            colorMid,
                            colorEnd
                        )
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        drawable_player_scene!!.setBackground(drawable)
                    } else {
                        drawable_player_scene!!.setBackgroundDrawable(drawable)
                    }

                    val player_scene_subtitles: ImageView =
                        view!!.findViewById(R.id.player_scene_subtitles)
                    val color_context =
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: Exception color_context $color_context")
                    player_scene_subtitles.setColorFilter(
                        color_context
                    )

                    val player_scene_audio: ImageView = view!!.findViewById(R.id.player_scene_audio)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: Exception color_context $color_context")
                    player_scene_audio.setColorFilter(
                        color_context
                    )

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

                    playerTitle = view!!.findViewById(R.id.player_scene_title)
                    playerTitle!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_medium")
                    )
                    playerTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    playerIndex = view!!.findViewById(R.id.player_scene_index)
                    playerIndex!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_regular")
                    )
                    playerIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    playerLogo = view!!.findViewById(R.id.player_scene_logo)
                    playerChannelName = view!!.findViewById(R.id.player_scene_channel_name)
                    playerChannelName!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_bold")
                    )
                    playerChannelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

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
                    bufferPosition!!.setProgressTintList(
                        ColorStateList.valueOf(
                            Color.parseColor(
                                ConfigColorManager.getColor("color_progress")
                                    .replace("#", ConfigColorManager.alfa_light)
                            )
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
                    parentalRating!!.typeface = TypeFaceProvider.getTypeFace(context!!, ConfigFontManager.getFont("font_regular"))
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
                            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
                        seekBar!!.progressTintList =
                            ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreated: Exception color rdb $ex")
                    }

                    seekBar!!.getThumb()
                        .setTint(Color.parseColor(ConfigColorManager.getColor("color_selector")))

                    seekBar?.progress = 0
                    seekBar!!.onFocusChangeListener = object : View.OnFocusChangeListener {
                        override fun onFocusChange(view: View?, hasFocus: Boolean) {
                            if (hasFocus) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: $hasFocus")
                                focusedView = view
                                try {
                                    seekBar!!.backgroundTintList = ColorStateList.valueOf(
                                        Color.parseColor(
                                            ConfigColorManager.getColor("color_selector")
                                        )
                                    )
                                    seekBar!!.progressTintList = ColorStateList.valueOf(
                                        Color.parseColor(
                                            ConfigColorManager.getColor("color_selector")
                                        )
                                    )
                                } catch (ex: Exception) {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
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
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                                }
                            }
                        }
                    }
                    seekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}

                        override fun onStartTrackingTouch(p0: SeekBar?) {}

                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                    seekBar!!.setOnKeyListener(object : View.OnKeyListener {
                        override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                            if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
                                if(view?.view?.visibility == View.INVISIBLE || view?.view?.visibility == View.GONE) {
                                    view?.view?.visibility = View.VISIBLE
                                    startHideTimer()
                                }
                                else{
                                    resetHideTimer()
                                }
                                return true
                            }
                            if (keyEvent!!.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                                resetHideTimer()
                                if(!TextUtils.isEmpty(speedText?.text)) {
                                    (sceneListener as PlayerSceneListener).changeSpeed()
                                }
                                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                    // maintaining repeatCount to handle key isLongPress
                                    if (keyEvent.repeatCount > 0) {
                                        (sceneListener as PlayerSceneListener).onPreviousClicked(true)
                                    }
                                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if (keyEvent.repeatCount > 0) {
                                        (sceneListener as PlayerSceneListener).onNextClicked(true)
                                    }
                                }
                                return true
                            }
                            else if (keyEvent.action == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                                resetHideTimer()
                                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                    // maintaining repeatCount to handle key isLongPress
                                    if (keyEvent.repeatCount == 0) {
                                        (sceneListener as PlayerSceneListener).onPreviousClicked(false)
                                    }
                                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                    if (keyEvent.repeatCount == 0) {
                                        (sceneListener as PlayerSceneListener).onNextClicked(false)
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    })

                    playPauseWrapper!!.onFocusChangeListener = this@PlayerScene
                    audioWrapper!!.onFocusChangeListener = this@PlayerScene
                    subtitlesWrapper!!.onFocusChangeListener = this@PlayerScene

                    playPauseWrapper!!.setOnClickListener(this@PlayerScene)
                    subtitlesWrapper!!.setOnClickListener(this@PlayerScene)
                    audioWrapper!!.setOnClickListener(this@PlayerScene)

                    fadingEdgeLayout = view!!.findViewById(R.id.fading_edge_layout)
                    tracksVerticalGridView = view!!.findViewById(R.id.side_view_vertical_grid_view)
                    subtitleTracksCheckListAdapter = CheckListAdapter(
                        fadingEdgeLayout = fadingEdgeLayout,
                        FadeAdapter.FadeAdapterType.VERTICAL
                    )
                    subtitleTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as PlayerSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            (sceneListener as PlayerSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onItemClicked(position: Int) {
                            //this method is called to restart inactivity timer for no signal power off
                            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                            //this method is called to restart inactivity timer for info banner scene
                            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                            currentSelectedSubtitleTrack = subtitleTracks!![position]
                            (sceneListener as PlayerSceneListener).onSubtitleTrackClicked(currentSelectedSubtitleTrack!!)
                        }

                        override fun onAdditionalItemClicked() {
                            (sceneListener as PlayerSceneListener).setSubtitles(false)
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
                            (sceneListener as PlayerSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            (sceneListener as PlayerSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onItemClicked(position: Int) {
                            //this method is called to restart inactivity timer for no signal power off
                            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                            //this method is called to restart inactivity timer for info banner scene
                            (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                            currentSelectedAudioTrack = audioTracks!![position]
                            (sceneListener as PlayerSceneListener).onAudioTrackClicked(currentSelectedAudioTrack!!)
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

                    playPauseWrapper!!.post {
                        playPauseWrapper!!.requestFocus()
                    }

                    tracksContainer = view!!.findViewById(R.id.tracksContainer)

                    sceneListener.onSceneInitialized()
                    if (isPlaying)
                        startHideTimer()

                }
            })
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
                            val color_context =
                                Color.parseColor(ConfigColorManager.getColor("color_background"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                            imageView.setColorFilter(
                                color_context
                            )
                           Utils.focusAnimation(view)
                        } catch (ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                        }
                    }
                    false -> {
                        try {
                            val color_context =
                                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color_context $color_context")
                            imageView.setColorFilter(
                                color_context
                            )
                            Utils.unFocusAnimation(view)

                        } catch(ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFocusChange: Exception color rdb $ex")
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ResourceType")
    override fun onClick(view: View?) {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        Utils.viewClickAnimation(view!!, object : AnimationListener {
            override fun onAnimationEnd() {
                resetHideTimer()
                if (view is CardView) {
                    when (view) {
                        playPauseWrapper -> {
                            if(getPlaybackSpeed() != MediaSessionControl.SPEED_FF_1X){
                                //when we are in speed mode, we are resetting the speed
                                (sceneListener as PlayerSceneListener).changeSpeed()
                                return
                            }
                            if (playPauseWrapper!![0] is ImageView) {
                                val imageView = playPauseWrapper!![0] as ImageView
                                if (isPlaying) {
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
                                    resetHideTimer()
                                    isPlaying = true
                                }
                            }
                        (sceneListener as PlayerSceneListener).onPlayPauseClicked()
                        return
                    }
                    previousWrapper -> {
                        (sceneListener as PlayerSceneListener).onPreviousClicked(true)
                        return
                    }
                    nextWrapper -> {
                        (sceneListener as PlayerSceneListener).onNextClicked(true)
                        return
                    }
                    audioWrapper -> {
                        tracksVerticalGridView!!.adapter = audioTracksCheckListAdapter
                        undefinedAudioTrackCount = 1
                        if (audioTracks.size == 0) {
                            (sceneListener as PlayerSceneListener).showToast(ConfigStringsManager.getStringById("no_available_audio_tracks_msg"))
                            return
                        }

                        selectedView = audioWrapper

                        var audioTrackObjects = mutableListOf<CheckListItem>()
                        audioTracksCheckListAdapter.refresh(audioTrackObjects)
                        audioTracks.forEach { item ->
                            if((sceneListener as PlayerSceneListener).getCurrentAudioTrack() == item){
                                audioTrackObjects.add(CheckListItem(item.trackName, true, null))
                                currentSelectedAudioTrack = item
                            }else{
                                audioTrackObjects.add(CheckListItem(item.trackName, false, null))
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
                            val color_light = Color.parseColor(
                                ConfigColorManager.getColor("color_main_text")
                                    .replace("#", ConfigColorManager.alfa_light)
                            )
                            audioWrapper!!.setCardBackgroundColor(
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(context, color_light)
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
                        (sceneListener as PlayerSceneListener).showToast(ConfigStringsManager.getStringById("no_available_subtitle_tracks_msg"))
                        return
                    }


                    selectedView = subtitlesWrapper

                    val subtitleCheckListItems = mutableListOf<CheckListItem>()
                    subtitleTracksCheckListAdapter.refresh(subtitleCheckListItems)
                    subtitleTracks.forEach { item ->
                        if((sceneListener as PlayerSceneListener).getCurrentSubtitleTrack() == item){
                            subtitleCheckListItems.add(CheckListItem(item.trackName, true, null))
                            currentSelectedSubtitleTrack = item
                        }else{
                            subtitleCheckListItems.add(CheckListItem(item.trackName, false, null))
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
                        val color_light = Color.parseColor(
                            ConfigColorManager.getColor("color_main_text")
                                .replace("#", ConfigColorManager.alfa_light)
                        )
                        subtitlesWrapper!!.setCardBackgroundColor(
                            ColorStateList.valueOf(
                                ContextCompat.getColor(context, color_light)
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

    /**
     * To refresh channel info on setting changes
     */
    fun updateChannelInfo(activeChannel: com.iwedia.cltv.platform.model.TvChannel?) {
        playerIndex!!.text = activeChannel?.getDisplayNumberText()
        playerChannelName!!.text = activeChannel?.name
    }

    /**
     * Set player data
     */
    fun setData(data: Any?) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setData: ############ data $data")
        if (data == null) {
            playerTitle!!.text = ConfigStringsManager.getStringById("no_information")
            playerIndex!!.text = ConfigStringsManager.getStringById("not_available")
            parentalRating!!.text = ""
        }
        else if (data is TvEvent) {
            tvChannel = data.tvChannel
            //TODO @Maksim Check later if parental here is needed and isBlockedContent() input type
            /* isEighteenPlus = ReferenceApplication.isBlockedContent(data)*/

            if(isEighteenPlus){
                playerTitle!!.text =  ConfigStringsManager.getStringById("parental_control_restriction")
                playerTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_progress")))
            }else{
                playerTitle!!.text = data.name
                playerTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            }

            parentalRating!!.text = (sceneListener as PlayerSceneListener).getParentalRatingDisplayName(data.parentalRating, data)

            playerIndex!!.text =  data.tvChannel.getDisplayNumberText()
            playerChannelName!!.text = data.tvChannel.name
            setProgress(0, 0)
            playerTitle!!.text = data.name
            setBufferProgress(0)
            if (InvalidDataTracker.hasValidData(data.tvChannel)) {
                val channelLogoPath = data.tvChannel.logoImagePath

                if (channelLogoPath != null) {
                    Utils.loadImage(
                        channelLogoPath,
                        playerLogo!!,
                        object : AsyncReceiver {
                            override fun onFailed(error: Error?) {
                                //InvalidDataTracker.setInvalidData(data.tvChannel)
                                playerLogo!!.visibility = View.INVISIBLE
                                playerChannelName!!.visibility = View.VISIBLE
                            }

                            override fun onSuccess() {
                                //InvalidDataTracker.setValidData(data.tvChannel)
                                playerLogo!!.visibility = View.VISIBLE
                                playerChannelName!!.visibility = View.GONE
                            }
                        })
                }else{
                    playerLogo!!.visibility = View.INVISIBLE
                    playerChannelName!!.visibility = View.VISIBLE
                }
            }
            else {
                //InvalidDataTracker.setInvalidData(data.tvChannel)
                playerLogo!!.visibility = View.INVISIBLE
                playerChannelName!!.visibility = View.VISIBLE
            }
        }
        else if (data is Recording) {
            tvChannel = data.tvChannel
            isEighteenPlus = ReferenceApplication.isBlockedContent(data.tvEvent!!)

            if(isEighteenPlus){
                playerTitle!!.text =  ConfigStringsManager.getStringById("parental_control_restriction")
                playerTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_progress")))
            }else{
                playerTitle!!.text = data.name
                playerTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            }
            try {
                playerIndex!!.text = String.format(Locale.ENGLISH, "%03d", data.tvChannel!!.displayNumber)
            }catch (E : Exception){
                playerIndex!!.text =data.tvChannel!!.displayNumber
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setData: ${E.printStackTrace()}")
            }
            playerChannelName!!.text = data.tvChannel!!.name
            setProgress(0, 0)
            setBufferProgress(100)
            setEndTime(Utils.getTimeStringFromSeconds(data.duration!! / 1000)!!)
            if (InvalidDataTracker.hasValidData(data.tvChannel!!)) {
                var channelLogoPath = data.tvChannel?.logoImagePath

                if (channelLogoPath != null) {
                    Utils.loadImage(
                        channelLogoPath,
                        playerLogo!!,
                        object : AsyncReceiver {

                            override fun onFailed(error: Error?) {
                                InvalidDataTracker.setInvalidData(data.tvChannel!!)
                                playerLogo!!.visibility = View.INVISIBLE
                                playerChannelName!!.visibility = View.VISIBLE
                            }

                            override fun onSuccess() {
                                InvalidDataTracker.setValidData(data.tvChannel!!)
                                playerLogo!!.visibility = View.VISIBLE
                                playerChannelName!!.visibility = View.GONE
                            }
                        })
                }else{
                    InvalidDataTracker.setInvalidData(data.tvChannel!!)
                    playerLogo!!.visibility = View.INVISIBLE
                    playerChannelName!!.visibility = View.VISIBLE
                }
            }

            else{
                InvalidDataTracker.setInvalidData(data.tvChannel!!)
                playerLogo!!.visibility = View.INVISIBLE
                playerChannelName!!.visibility = View.VISIBLE
            }
        }
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
    }

    /**
     * Set buffer progress position
     *
     * @param progress
     */
    fun setBufferProgress(progress: Int) {
        if (!isSeeking) {
            bufferPosition!!.progress = progress
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((sceneListener as PlayerSceneListener).isPlayerActiveScene()) {
            return super.dispatchKeyEvent(keyCode, keyEvent)
        }
        // Skip key handling if the close dialog is shown
        if ((sceneListener as PlayerSceneListener).isCloseDialogShown()) {
            return false
        }
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                if (view?.view?.visibility == View.INVISIBLE) {
                    (sceneListener as PlayerSceneListener).pvrPlaybackExit(null)
                    return true
                }
                view?.view?.visibility = View.INVISIBLE
                return true

            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (view?.view?.visibility == View.INVISIBLE) {
                    resetHideTimer()
                    return true
                }
            }

            if (keyCode == KeyEvent.KEYCODE_MEDIA_RECORD){
                if (view?.view?.visibility == View.INVISIBLE) {
                    resetHideTimer()
                    return true
                }
            }
        }
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {

            if (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_REWIND  ) {
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
            when (keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    (sceneListener as PlayerSceneListener).onChannelUp()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    (sceneListener as PlayerSceneListener).onChannelDown()
                    return true
                }
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

                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    (sceneListener as PlayerSceneListener).onStopClicked()
                    return true
                }
            }
            if (view?.view?.visibility == View.INVISIBLE) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
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
                    (sceneListener as PlayerSceneListener).onLeftKeyPressed()
                    return true
                }

                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_INFO) {
                    (sceneListener as PlayerSceneListener).onRightKeyPressed()
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
        return super.dispatchKeyEvent(keyCode, keyEvent)
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


    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        super.refresh(data)
        if (data is Boolean) {
            val imageView = playPauseWrapper!![0] as ImageView
            if (data == true) {
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
            var videoResolution = (sceneListener as PlayerSceneListener).getVideoResolution()
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

            if ((sceneListener as PlayerSceneListener)!!.getIsDolby(TvTrackInfo.TYPE_AUDIO)) {
                isDB!!.visibility = View.VISIBLE
            } else {
                isDB!!.visibility = View.GONE
            }
            if ((sceneListener as PlayerSceneListener)!!.getIsCC(TvTrackInfo.TYPE_AUDIO)) {
                isCC!!.visibility = View.VISIBLE
            } else {
                isCC!!.visibility = View.GONE
            }
            if ((sceneListener as PlayerSceneListener)!!.getIsAudioDescription(TvTrackInfo.TYPE_AUDIO)) {
                isAD!!.visibility = View.VISIBLE
            } else {
                isAD!!.visibility = View.GONE
            }
            if ((sceneListener as PlayerSceneListener)!!.getTeleText(TvTrackInfo.TYPE_SUBTITLE)) {
                if ((sceneListener as PlayerSceneListener).getConfigInfo("teletext_enable_column")) {
                    isTTX.visibility = View.VISIBLE
                } else {
                    isTTX.visibility = View.GONE
                }
            } else {
                isTTX.visibility = View.GONE
            }

            audioTracks = (sceneListener as PlayerSceneListener).getAvailableAudioTracks()
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


            subtitleTracks =
                (sceneListener as PlayerSceneListener).getAvailableSubtitleTracks()
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
//        setSubtitleOptionVisibility()
    }

    fun hideTrackInfo() {
        Log.i("Hide track info", "hideTrackInfo: ")
        audioWrapper!!.visibility = View.GONE
        subtitlesWrapper!!.visibility = View.GONE
    }

    fun hidePlayer(){
        backgroundLayout!!.visibility = View.GONE
        audioWrapper!!.visibility = View.GONE
        subtitlesWrapper!!.visibility = View.GONE
    }

    private fun startHideTimer() {
        if (hideTimer == null) {
            hideTimer = Timer()
        }
        view?.view?.visibility = View.VISIBLE
        PlayerSceneData.isOnlyPlayback = false
        if (focusedView != null) {
            focusedView?.requestFocus()
        }
        hideTimerTask = object : TimerTask() {
            override fun run() {
                ReferenceApplication.runOnUiThread(Runnable {
                    //InformationBus.submitEvent(Event(ReferenceEvents.FOCUS_ON_BACK_PRESS, true))
                    view?.view?.visibility = View.INVISIBLE
                    PlayerSceneData.isOnlyPlayback = true
                })
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
            PlayerSceneData.isOnlyPlayback = true
        })
    }

    fun resetHideTimer() {
        stopHideTimer()
        startHideTimer()
    }

    fun showSubtitleList() {
        subtitlesWrapper?.callOnClick()
    }

    fun showAudioList() {
        audioWrapper?.callOnClick()
    }

    override fun onPause() {
        super.onPause()
//        (sceneListener as PlayerSceneListener).onPauseClicked()
        stopHideTimer()
    }

    override fun onResume() {
        super.onResume()
        resetHideTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHideTimer()
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
            (sceneListener as PlayerSceneListener).onFastForward(speed)
        }
        if (speed < 0 ) {
            (sceneListener as PlayerSceneListener).onRewind(speed)
        }
    }

    fun updateSpeedText(speed: Int) {
        if(speed == MediaSessionControl.SPEED_FF_1X) {
            speedText?.setText("")
            currentSpeedValue = MediaSessionControl.SPEED_FF_1X
        }else{
            speedText?.visibility = View.VISIBLE
            val spd = buildString { append(speed).append("X") }
            speedText?.setText(spd)
        }
        if(isPlaying) {
            updateHideTimer(speed)
        }
    }

}