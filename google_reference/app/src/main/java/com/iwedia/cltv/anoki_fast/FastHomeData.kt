package com.iwedia.cltv.anoki_fast

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.anoki_fast.epg.AnimationHelper
import com.iwedia.cltv.components.CustomButton
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.utils.Utils

/**
 * Fast home screen data
 *
 * @author Dejan Nadj
 */
@SuppressLint("ViewConstructor")
class FastHomeData : ConstraintLayout {

    private lateinit var promotionButton: CustomButton
    private lateinit var promotionLogoImageView: ImageView
    private lateinit var slider: LinearLayout

    /**
     * [backgroundImageView] is a [ImageView] that must be set from outside this class. It occupies the full screen size and hoists the Images of the promotion items.
     */
    private lateinit var backgroundImageView: ImageView

    /**
     * [backgroundView] is a [View] that must be set from outside this class. It occupies the full screen size and creates a solid background when promotions are shown.
     */
    private lateinit var backgroundView: View
    private var refreshTimer: CountDownTimer? = null
    private var dots: ArrayList<ImageView> = arrayListOf()
    private var listener: FastHomeDataListener? = null
    private var promotionList: ArrayList<PromotionItem> = arrayListOf()
    private var currentPromotionIndex = 0
    private var dotIndex = 0
    private var dotsCount = 4
    private var firstTimeShown = true

    /**
     * A ValueAnimator used to control the fade-in and fade-out animations.
     * This variable is nullable and is initially set to null. It is used to track and manage the state
     * of the animation to ensure that any ongoing animation is properly cancelled before starting a new one.
     *
     * @see AnimationHelper.fadeOutFadeIn
     * @see AnimationHelper.fadeOutAnimation
     */
    private var valueAnimator: ValueAnimator? = null

    /**
     * [globalValueAnimator] is used to manage the animation of [FastHomeData]'s visibility.
     * Its significance lies in the [hide] and [show] methods, where it plays a crucial role:
     * prior to revealing or concealing [FastHomeData], any ongoing animation must be stopped.
     */
    private var globalValueAnimator: ValueAnimator? = null

    private var isAnimationAllowed = true

    /**
     *  Constructor used for home data that is set in xml
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialisation()
    }

    /**
     *  Constructor used for home data that is set in xml
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(
        context,
        attrs,
        defStyleAttrs
    ) {
        initialisation()
    }

    /**
     * View initialisation
     */
    private fun initialisation() {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.fast_home_data, this, true)

        // initialise all Views
        promotionButton = findViewById(R.id.promotion_custom_button)
        promotionLogoImageView = findViewById(R.id.promotion_logo_image_view)
        slider = findViewById(R.id.promotion_slider)
        promotionButton.alpha =
            0f // This is important when the user enters FastHomeData for the first time. It ensures that the button will be animated, not just shown.
        slider.alpha =
            0f // This is important when the user enters FastHomeData for the first time. It ensures that the slider will be animated, not just shown.
        dots.add(findViewById(R.id.dot0))
        dots.add(findViewById(R.id.dot1))
        dots.add(findViewById(R.id.dot2))
        dots.add(findViewById(R.id.dot3))
        scaleAnimation(dots[0], true)
        firstTimeShown = true

        //Promotion button click and key listener
        promotionButton.setOnClick { listener?.onButtonClicked(promotionList[currentPromotionIndex]) }

        promotionButton.setOnFocusChanged { hasFocus ->
            if (hasFocus) {
                isAnimationAllowed = true
            }
        }

        promotionButton.setOnKeyListener(object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        listener?.onDown()
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        listener?.onUp()
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (isAnimationInProgress()) return true // Disable user input of DPAD_RIGHT during animation.
                        loadNextPromotion()
                        restartTimer()
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && promotionList.size > 1) {
                        if (isAnimationInProgress()) return true // Disable user input of DPAD_LEFT during animation.
                        loadPreviousPromotion()
                        restartTimer()
                        return true
                    }
                }
                return false
            }
        })
    }

    /**
     * Checks if any of the specified views are currently undergoing animation.
     *
     * This method evaluates the alpha values of a list of views to determine if any of them are currently in the process of
     * animation. It returns true if at least one view's alpha value falls within the range [0, 1], indicating an ongoing
     * animation.
     *
     * @return `true` if any of the views are currently undergoing animation, `false` otherwise.
     */
    fun isAnimationInProgress(): Boolean {
        val viewsToCheck = listOf(
            promotionLogoImageView,
            backgroundImageView,
            slider,
            promotionButton
        )
        return viewsToCheck.any { it.alpha > 0f && it.alpha < 1f }
    }

    // Do not add muteAudio() in below function as it causes to mute audio at multiple places like
    // clicking on watch button from on Now rail, removing watchlist event from discovery and again adding it from EPG etc.
    fun show(onShow: () -> Unit, shouldSetFocusToButton: Boolean) {
        // TODO BORIS think about behavior when list is empty.
        if (promotionList.isEmpty()) {
            listener?.onUp()
            return
        }

        onShow.invoke()
        globalValueAnimator?.cancel()
        globalValueAnimator = AnimationHelper.fadeInAnimation(
            listOf(this, backgroundImageView, backgroundView),
            onAnimationEnded = {
                AnimationHelper.fadeInAnimation(listOf(promotionButton, slider))
                loadPromotion()
                restartTimer()
            })

        if (shouldSetFocusToButton) promotionButton.requestFocus()
    }

    private fun loadNextPromotion() {
        if (promotionList.size > 1) {
            if (currentPromotionIndex < promotionList.size - 1) {
                currentPromotionIndex++
            } else {
                currentPromotionIndex = 0
            }
            dotIndex = if (dotIndex >= dotsCount - 1) 0 else dotIndex + 1
            loadPromotion(animate = true)
            animateSlider()
        }
    }

    private fun loadPreviousPromotion() {
        if (currentPromotionIndex > 0) {
            currentPromotionIndex--
        } else {
            currentPromotionIndex = promotionList.size - 1
        }
        dotIndex = if (dotIndex > 0) dotIndex - 1 else dotsCount - 1
        loadPromotion(animate = true)
        animateSlider()
    }

    //Loads current banner logo and background images
    private fun loadPromotion(animate: Boolean = false) {
        if (promotionList.isNotEmpty()) {

            if(currentPromotionIndex >= promotionList.size) currentPromotionIndex = 0

            val promotionItem = promotionList[currentPromotionIndex]
            promotionButton.setTextLabel(promotionItem.callToAction!!)
            if (animate) {

                valueAnimator = AnimationHelper.fadeOutFadeIn(
                    listOfViews = listOf(
                        backgroundImageView,
                        promotionLogoImageView
                    ),
                    onBetweenAnimation = {
                        Utils.loadImage(promotionItem.logo!!, promotionLogoImageView, shouldCompressImage = false)
                        Utils.loadImage(promotionItem.banner!!, backgroundImageView, shouldCompressImage = false)
                    },
                    toAlpha = 0.1f
                )
                return
            }
            // The code below this comment will only be executed if 'animate' is false
            Utils.loadImage(promotionItem.logo!!, promotionLogoImageView, shouldCompressImage = false)
            Utils.loadImage(promotionItem.banner!!, backgroundImageView, shouldCompressImage = false)
        }
    }

    //Slider dots scale animation
    private fun animateSlider() {
        for (i in 0 until dotsCount) {
            val index = if (i >= dots.size) dots.size - 1 else i
            if (index == dotIndex) {
                scaleAnimation(dots[index], true)
            } else {
                scaleAnimation(dots[index], false)
            }
        }
    }

    private fun scaleAnimation(dot: ImageView, scaleUp: Boolean) {
        if (scaleUp) {
            dot.scaleX = 1.2f
            dot.scaleY = 1.2f
            dot.backgroundTintList = resources.getColorStateList(R.color.white, null)
        } else {
            dot.scaleX = 1.0f
            dot.scaleY = 1.0f
            dot.backgroundTintList =
                resources.getColorStateList(R.color.color_text_description, null)
        }
    }


    fun removeFocus() {
        promotionButton.clearFocus()
        firstTimeShown = true
    }

    fun isPromotionListEmpty() = promotionList.isEmpty()

    /**
     * Set promotion banner item list
     */
    fun setPromotionList(promotionList: ArrayList<PromotionItem>) {
        this.promotionList.clear()
        this.promotionList.addAll(promotionList)

        if (promotionList.isEmpty()) {
            hide(onAnimationEnded = {
                listener!!.onUp()
            })
            return
        }

        updateDotsVisibility(promotionList.size)
    }
    private fun updateDotsVisibility(count: Int) {
        if (count == 0 || count == 1) {
            slider.visibility = View.GONE
            dotsCount = 0
            dots.forEach { dot ->
                dot.visibility = View.GONE
            }
        } else {
            slider.visibility = View.VISIBLE
            dotsCount = count
            dots.forEachIndexed { index, dot ->
                dot.visibility = if (index < count) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * @see [backgroundImageView]'s documentation to understand what this method does.
     */
    fun setBackgroundImageView(view: ImageView) {
        this.backgroundImageView = view
    }

    /**
     * @see [backgroundView]'s documentation to understand what this method does.
     */
    fun setBackgroundView(view: View) {
        this.backgroundView = view
    }

    /**
     * Set fast home data listener
     */
    fun setListener(fastHomeDataListener: FastHomeDataListener) {
        promotionButton.textToSpeechHandler.setupTextToSpeechTextSetterInterface(fastHomeDataListener)
        this.listener = fastHomeDataListener
    }

    /**
     * Stop refresh update timer if it is already started
     */
    fun stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer!!.cancel()
            refreshTimer = null
        }
    }

    /**
     * Restart refresh timer
     */
    private fun restartTimer() {
        stopRefreshTimer()
        //Start new refresh count down timer
        refreshTimer = object :
            CountDownTimer(
                5000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                loadNextPromotion()
                restartTimer()
            }
        }
        refreshTimer!!.start()
    }

    fun restartTimerIfFocusOnPromotionButton() {
        if (promotionButton.isFocused) {
            restartTimer()
        }
    }

    fun restartTimerIfListNotEmpty() {
        if (promotionList.isNotEmpty()) {
            restartTimer()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRefreshTimer()
    }

    /**
     * [hide] is main method for displaying the [FastHomeData] instance in app. It controls all
     * animations and has callbacks implemented for triggering some code on [hide]'s start and on
     * [hide]'s end.
     */
    fun hide(onAnimationEnded: () -> Unit = {}, onAnimationStarted: () -> Unit = {}) {
        globalValueAnimator?.cancel() // important to stop previous animation if it was launched.
        globalValueAnimator = AnimationHelper.fadeOutAnimation(
            listOfViews = listOf(this, backgroundImageView, backgroundView),
            onAnimationStarted = {
                onAnimationStarted.invoke()
                stopRefreshTimer()
                valueAnimator?.cancel() // Cancel any ongoing animation to ensure it's stopped before starting the fade-out animation.
            },
            onAnimationEnded = {
                onAnimationEnded.invoke()
            }
        )
    }

    interface FastHomeDataListener: TTSSetterInterface {
        /**
         * Promotion button click
         */
        fun onButtonClicked(promotionItem: PromotionItem)

        /**
         * Up from promotion banner
         */
        fun onUp()

        /**
         * Down from promotion banner
         */
        fun onDown()
        /**
         * Mute audio.
         */
        fun mute()
    }
}