package com.iwedia.cltv.anoki_fast

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvTrackInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.anoki_fast.epg.AnimationHelper
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.components.CustomButton
import com.iwedia.cltv.components.CustomDetails
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import utils.information_bus.Event
import utils.information_bus.InformationBus

const val STROKE_WIDTH_DP = 2f // Desired stroke width in dp

const val COLOR_SELECTOR = "#e8f0fe"
const val COLOR_SELECTOR_40 = "#66e8f0fe"
const val COLOR_BG = "#db202124"
const val COLOR_GRADIENT_BOTTOM = "#fc000000"
const val COLOR_GRADIENT_CENTER = "#bf040405"
const val COLOR_GRADIENT_TOP = "#002b2b2d"

/**
 * @author Boris Tirkajla
 */
@SuppressLint("ViewConstructor")
class FastZapBanner(
    private val context: Context,
    private val focusCallback: () -> Unit,
    private val fastAudioSubtitleListListener: FastAudioSubtitleList.Listener
) : ConstraintLayout(context) {

    private val animationDuration = 100L
    private val density = resources.displayMetrics.density
    private val strokeWidthPx = (STROKE_WIDTH_DP * density).toInt()

    private var customDetails: CustomDetails.CustomDetailsZapBannerFast
    private var audioCustomButton: CustomButton
    private var subtitleCustomButton: CustomButton
    private var channelImageCardView: MaterialCardView
    private var tvGuideCardView: MaterialCardView
    private var tvGuideImageView: ImageView
    private var tvGuideTextView: TextView
    private var channelImageView: ImageView
    private var arrowUpImageView: ImageView

    private var arrowDownImageView: ImageView
    private var isChannelImageLoaded: Boolean = false
    private var audioSubtitleList: FastAudioSubtitleList
    private var trackUpdateTimer: CountDownTimer?= null
    private var fadeOutTimer: CountDownTimer? = null
    private lateinit var activeChannel: TvChannel

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.fast_zap_banner, this, true)

        // initialise all Views
        customDetails = findViewById(R.id.custom_details)
        customDetails.textToSpeechHandler.setupTextToSpeechTextSetterInterface(FastZapBannerDataProvider)
        audioCustomButton = findViewById(R.id.audio_custom_button)
        audioCustomButton.textToSpeechHandler.setupTextToSpeechTextSetterInterface(FastZapBannerDataProvider)
        subtitleCustomButton = findViewById(R.id.subtitle_custom_button)
        subtitleCustomButton.textToSpeechHandler.setupTextToSpeechTextSetterInterface(FastZapBannerDataProvider)
        channelImageCardView = findViewById(R.id.channel_image_card_view)
        channelImageCardView.setCardBackgroundColor(Color.TRANSPARENT)
        tvGuideCardView = findViewById(R.id.tv_guide_card_view)
        tvGuideImageView = findViewById(R.id.tv_guide_image_view)
        tvGuideTextView = findViewById(R.id.tv_guide_text_view)
        channelImageView = findViewById(R.id.channel_image_view)
        arrowUpImageView = findViewById(R.id.arrow_up_image_view)
        arrowDownImageView = findViewById(R.id.arrow_down_image_view)
        audioSubtitleList = FastAudioSubtitleList(
            context = context,
            dataProvider = FastZapBannerDataProvider,
            listener = fastAudioSubtitleListListener
        ) {
            if (it == 0) {
                subtitleCustomButton.requestFocus()
            } else {
                audioCustomButton.requestFocus()
            }
        }.also {
            it.visibility = GONE
            findViewById<LinearLayout>(R.id.audio_subtitle_container).addView(it)
        }

        // set gradient of the background
        val gradientDrawable = GradientDrawable()
        gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        gradientDrawable.orientation = GradientDrawable.Orientation.BOTTOM_TOP
        gradientDrawable.colors = intArrayOf(
            Color.parseColor(COLOR_GRADIENT_BOTTOM),
            Color.parseColor(COLOR_GRADIENT_CENTER),
            Color.parseColor(COLOR_GRADIENT_TOP)
        )
        findViewById<ConstraintLayout>(R.id.background_constraint_layout).background =
            gradientDrawable


        tvGuideCardView.setOnFocusChangeListener { _, hasFocus ->

            tvGuideCardView.setCardBackgroundColor(
                if (hasFocus) {
                    Color.parseColor(COLOR_SELECTOR)
                } else {
                    Color.TRANSPARENT
                }
            )

            tvGuideImageView.setColorFilter(
                if (hasFocus) {
                    Color.parseColor(COLOR_BG)
                } else {
                    Color.parseColor(COLOR_SELECTOR)
                }
            )

            tvGuideTextView.setTextColor(
                if (hasFocus) {
                    Color.parseColor(COLOR_BG)
                } else {
                    Color.parseColor(COLOR_SELECTOR)
                }
            )

            if (hasFocus) {
                fastAudioSubtitleListListener.setSpeechText(
                    tvGuideTextView.text.toString()
                )
            }
        }

        channelImageCardView.setOnFocusChangeListener { _, hasFocus ->
            channelImageCardView.strokeWidth = if (hasFocus) strokeWidthPx else 0
            if (hasFocus) {
                if (::activeChannel.isInitialized) {
                    fastAudioSubtitleListListener.setSpeechText(
                        activeChannel.name
                    )
                }

                // It will remain transparent until an image loading failure occurs, at which point it will be set to a solid color in onFailure.
                channelImageCardView.setCardBackgroundColor(Color.TRANSPARENT)
            }
        }


        tvGuideCardView.setOnKeyListener { _, keyCode, keyEvent ->
            startTimer()
            //CHECK BACK KEY
            if (checkBackKeyEvent(keyEvent)) {
                return@setOnKeyListener true
            }
            //CHANNEL CHANGE KEY
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                channelChangeKeyEvent(true, keyEvent)
                return@setOnKeyListener true
            }
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                channelChangeKeyEvent(false, keyEvent)
                return@setOnKeyListener true
            }
            // ACTION DOWN -------------------------------------------------------------------------
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    channelImageCardView.requestFocus()
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    animateClick(
                        clickState = ClickState.PRESSED, view = tvGuideCardView
                    )
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return@setOnKeyListener true
                }
            }
            // ACTION UP ---------------------------------------------------------------------------
            if (keyEvent.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    // animate pressing of the view
                    animateClick(
                        clickState = ClickState.RELEASED, view = tvGuideCardView
                    )
                    // trigger action
                    onTvGuideButtonClicked()
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }


        channelImageCardView.setOnKeyListener { _, keyCode, keyEvent ->
            startTimer()
            //CHECK BACK KEY
            if (checkBackKeyEvent(keyEvent)) {
                return@setOnKeyListener true
            }
            //CHANNEL CHANGE KEY
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                channelChangeKeyEvent(true, keyEvent)
                return@setOnKeyListener true
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                channelChangeKeyEvent(false, keyEvent)
                return@setOnKeyListener true
            }
            // ACTION DOWN--------------------------------------------------------------------------
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (subtitleCustomButton.visibility == VISIBLE)
                        subtitleCustomButton.requestFocus()
                    else if (audioCustomButton.visibility == VISIBLE)
                        audioCustomButton.requestFocus()
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    tvGuideCardView.requestFocus()
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }

        subtitleCustomButton.setOnClick {
            onSubtitleButtonPressed()
        }

        subtitleCustomButton.setOnKeyListener { _, keyCode, keyEvent ->
            startTimer()
            //CHECK BACK KEY
            if (checkBackKeyEvent(keyEvent)) {
                return@setOnKeyListener true
            }
            //CHANNEL CHANGE KEY
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                channelChangeKeyEvent(true, keyEvent)
                return@setOnKeyListener true
            }
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                channelChangeKeyEvent(false, keyEvent)
                return@setOnKeyListener true
            }
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (audioCustomButton.isVisible)
                        audioCustomButton.requestFocus()
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    channelImageCardView.requestFocus()
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }

        audioCustomButton.setOnClick {
            onAudioButtonPressed()
        }

        audioCustomButton.setOnKeyListener { _, keyCode, keyEvent ->
            startTimer()
            //CHECK BACK KEY
            if (checkBackKeyEvent(keyEvent)) {
                return@setOnKeyListener true
            }
            //CHANNEL CHANGE KEY
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                channelChangeKeyEvent(true, keyEvent)
                return@setOnKeyListener true
            }
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                channelChangeKeyEvent(false, keyEvent)
                return@setOnKeyListener true
            }
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (subtitleCustomButton.visibility == VISIBLE)
                        subtitleCustomButton.requestFocus()
                    else
                        channelImageCardView.requestFocus()
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }
    }

    fun channelChangeKeyEvent(keyUp: Boolean, keyEvent: KeyEvent) {
        // ACTION DOWN--------------------------------------------------------------------------
        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
            if (keyUp) {
                animateClick(ClickState.PRESSED, arrowUpImageView)
                channelUp()
            } else {
                animateClick(ClickState.PRESSED, arrowDownImageView)
                channelDown()
            }
        }
        // ACTION UP----------------------------------------------------------------------------
        if (keyEvent.action == KeyEvent.ACTION_UP) {
            if (keyUp) {
                animateClick(ClickState.RELEASED, arrowUpImageView)
            } else {
                animateClick(ClickState.RELEASED, arrowDownImageView)
            }
            BackFromPlayback.setKeyPressedState()
        }
    }

    private fun checkBackKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                return true
            }
        }
        if (keyEvent.action == KeyEvent.ACTION_UP) {
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                BackFromPlayback.setKeyPressedState()
                switchVisibility()
                return true
            }
        }
        return false
    }

    private fun onTvGuideButtonClicked() {
        switchVisibility()
        FastZapBannerDataProvider.guideClick(context)
    }

    private fun channelDown() {
        FastZapBannerDataProvider.channelDown{ success->
            updateData()
        }
    }

    private fun channelUp() {
       FastZapBannerDataProvider.channelUp{ success->
           updateData()
       }
    }

    private fun onAudioButtonPressed() {
        audioSubtitleList.showAudio()
    }

    private fun onSubtitleButtonPressed() {
        audioSubtitleList.showSubtitles()
    }

    /**
     * method used for handling FastZapBanner's visibility.
     *
     * If FastZapBanner was visible, calling this method will set it's visibility to GONE. Otherwise,
     * when FastZapBanner was not visible, it's visibility will be set to VISIBLE and [updateData]
     * will be called automatically.
     */
    fun switchVisibility() {
        if (isVisible) {
            AnimationHelper.fadeOutAnimation(view = this, duration = animationDuration).apply {
                focusCallback.invoke()
            }
            cancelTimer()
        } else {
            audioSubtitleList.visibility =
                GONE //hiding audio/subtitle scene before showing new zap banner.
            updateData()
            AnimationHelper.fadeInAnimation(view = this, duration = animationDuration)
            channelImageCardView.requestFocus() // The initial focus inside the banner is on channels
            startTimer()
        }
    }

    fun requestFocusChannelImage() {
        channelImageCardView.requestFocus()
    }

    fun updateData() {
        FastZapBannerDataProvider.getCurrentChannel { activeChannel->
            if (activeChannel != null) {
                var updateTrackData = true
                if (::activeChannel.isInitialized) {
                    updateTrackData = this.activeChannel.displayNumber != activeChannel.displayNumber
                }
                // Update active channel data
                ReferenceApplication.runOnUiThread {
                    this.activeChannel = activeChannel

                    // set background to TRANSPARENT and in onFailed set it to solid if image loading fails.
                    channelImageCardView.setCardBackgroundColor(Color.TRANSPARENT)

                    // Set channel logo image
                    loadWithGlide(imageView = channelImageView,
                        imagePath = activeChannel.logoImagePath,
                        onSuccess = {
                            isChannelImageLoaded = true
                        },
                        onFailure = {
                            channelImageCardView.setCardBackgroundColor(
                                Color.parseColor(
                                    COLOR_SELECTOR_40
                                )
                            )
                            isChannelImageLoaded = false
                        }
                    )
                }

                // Update current event info
                FastZapBannerDataProvider.getCurrentEvent(activeChannel) { event->
                    ReferenceApplication.runOnUiThread {
                        var tvEvent = event
                        if (event == null) {
                            tvEvent = TvEvent.createNoInformationEvent(activeChannel, System.currentTimeMillis())
                        }
                        // The null check is not necessary cause it should not happen
                        if (tvEvent != null) {
                            //Refresh ui elements inside the UI thread
                            customDetails.updateData(
                                tvEvent = tvEvent,
                                parentalRatingDisplayName = FastZapBannerDataProvider.getParentalRatingDisplayName(
                                    tvEvent.parentalRating, tvEvent
                                ),
                                currentTime = System.currentTimeMillis(),
                                dateTimeFormat = FastZapBannerDataProvider.getDateTimeFromat(),
                                isEventLocked = FastZapBannerDataProvider.isEventLocked(tvEvent)
                            )

                            // UPDATE VIDEO QUALITY INFORMATION
                            if (!tvEvent.tvChannel.videoQuality.isNullOrEmpty()) {
                                customDetails.updateVideoQuality(tvEvent.tvChannel.videoQuality[0])
                            }

                            // UPDATE AUDIO AND SUBTITLE TRACKS INFORMATION
                            if (updateTrackData)
                                updateTrackData()
                            else {
                                refreshTrackData()
                                //if tracks are visible then making the buttons visible
                                if(FastZapBannerDataProvider.getAvailableSubtitleTracks()?.size!!>0)
                                    subtitleCustomButton.visibility = VISIBLE

                                if(FastZapBannerDataProvider.getAvailableAudioTracks()?.size!!>0)
                                    audioCustomButton.visibility = VISIBLE
                            }
                        } else {
                            // TODO show error
                        }
                    }
                }
            } else {
                // TODO show error
            }
        }
    }

    private fun clearTrackData() {
        //Clear old track data before update
        customDetails.updateSubtitleTracks(
            subtitleTracks = arrayListOf(),
            activeSubtitleTrack = null,
            isSubtitleEnabled = FastZapBannerDataProvider.isSubtitlesEnabled()
        )
        customDetails.updateAudioTracks(
            arrayListOf(), null
        )
        customDetails.updateDolbyImageView(false)
    }

    private fun updateTrackData() {
        clearTrackData()
        //making subtitle and audio button invisible so that they only present when tracks are updated.
        subtitleCustomButton.visibility = INVISIBLE
        audioCustomButton.visibility = INVISIBLE
        //requesting focus as buttons might have focus and they are not present
        channelImageCardView.requestFocus()

        //Update track data after 5 seconds, this is time needed for tracks information to be refreshed by the mw
        trackUpdateTimer?.cancel()
        trackUpdateTimer = object :
            CountDownTimer(
                5000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {
                //each second checking if tracks are available or not if available the showing them and cancelling the timer
                Handler(Looper.getMainLooper()).post {
                    if(FastZapBannerDataProvider.getAvailableSubtitleTracks()?.size!!>0) {
                        clearTrackData()
                        refreshTrackData()
                        subtitleCustomButton.visibility = VISIBLE
                        trackUpdateTimer?.cancel()
                    }
                    if(FastZapBannerDataProvider.getAvailableAudioTracks()?.size!!>0) {
                        clearTrackData()
                        refreshTrackData()
                        audioCustomButton.visibility = VISIBLE
                        trackUpdateTimer?.cancel()
                    }
                }
            }
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                Handler(Looper.getMainLooper()).post {
                    clearTrackData()
                    refreshTrackData()
                    if(FastZapBannerDataProvider.getAvailableSubtitleTracks()?.size!!>0)
                        subtitleCustomButton.visibility = VISIBLE

                    if(FastZapBannerDataProvider.getAvailableAudioTracks()?.size!!>0)
                        audioCustomButton.visibility = VISIBLE
                    }

            }
        }
        trackUpdateTimer?.start()
    }

    private fun refreshTrackData() {
        // UPDATE SUBTITLE TRACKS
        customDetails.updateSubtitleTracks(
            subtitleTracks = FastZapBannerDataProvider.getAvailableSubtitleTracks(),
            activeSubtitleTrack = FastZapBannerDataProvider.getCurrentSubtitleTrack(),
            isSubtitleEnabled = FastZapBannerDataProvider.isSubtitlesEnabled()
        )

        customDetails.updateAudioTracks(
            audioTracks = FastZapBannerDataProvider.getAvailableAudioTracks(),
            activeAudioTrack = FastZapBannerDataProvider.getCurrentAudioTrack()
        )

        // UPDATE DOLBY ICON
        customDetails.updateDolbyImageView(FastZapBannerDataProvider.isDolby(TvTrackInfo.TYPE_AUDIO))
    }

    enum class ClickState {
        PRESSED, RELEASED
    }

    private fun animateClick(clickState: ClickState, view: View) {
        when (clickState) {
            ClickState.PRESSED -> {
                view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(300).start()
            }

            ClickState.RELEASED -> {
                view.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
            }
        }
    }

    private fun loadWithGlide(
        imageView: ImageView,
        imagePath: String?,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        Glide.with(context).load(imagePath).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .centerInside().transition(DrawableTransitionOptions.withCrossFade(150))
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    onFailure()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    onSuccess()
                    return false
                }
            }).error(Glide.with(context).load(imagePath)).into(imageView)
    }

    private fun cancelTimer() {
        //Cancel timer if it's already started
        if (fadeOutTimer != null) {
            fadeOutTimer!!.cancel()
            fadeOutTimer = null
        }
    }

    /**
     * Start zap banner fade out timer
     */
    fun startTimer() {
        cancelTimer()

        //Start new count down timer
        fadeOutTimer = object :
            CountDownTimer(
                5000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                //Close zap banner if it's still visible
                if (audioSubtitleList.visibility == View.GONE || audioSubtitleList.tracksWrapperLinearLayout?.visibility==View.GONE) {
                    InformationBus.submitEvent(Event(Events.TALKBACK_CLOSE_ZAP_BANNER))
                    AnimationHelper.fadeOutAnimation(view = this@FastZapBanner, duration = animationDuration).apply {
                        focusCallback.invoke()
                    }
                } else {
                    startTimer()
                }
            }
        }
        fadeOutTimer!!.start()
    }
}