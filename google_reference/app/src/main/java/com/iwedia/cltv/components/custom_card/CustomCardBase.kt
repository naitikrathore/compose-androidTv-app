package com.iwedia.cltv.components.custom_card

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.cltv.utils.Utils.Companion.animateBackgroundColor

/**
 * The [CustomCardBase] class serves as the foundation for various card types within the application.
 *
 * ***Purpose of [CustomCardBase]:***
 * This class defines and implements common behaviors shared among these cards, including:
 * 1) Animations
 * 2) Click listeners
 * 3) Key handling
 * 4) Focus handling
 * 5) Long-press handling
 *
 * **Guidelines for Modification:**
 * When enhancing this class, exercise caution to ensure that additions are universally applicable
 * to all cards extending this class. The primary goal is to preserve only common functionalities here.
 *
 * Two approaches for modifying the card's behavior outside of [CustomCardBase] are supported:
 * 1) **Using open functions:** - **modification from the child classes.**
 * For instance, the [onFocusChanged] function is implemented in this class
 * ([CustomCardBase]) specifically for animations and card colors. Subsequently, it can be overridden in
 * child classes, like [CustomCardChannelList.onFocusChanged]. This allows child classes to inherit the
 * general behavior while specifying any card-specific actions. Similar flexibility is provided for
 * methods such as [onDpadRightPressed], [onDpadUpPressed], [onDpadDownPressed], [onDpadLeftPressed],
 * [onBackPressed], [onChannelDownPressed], [onChannelUpPressed], [onMediaRecordPressed], and
 * [onCaptionsPressed]. Even if some of these methods remain generic in [CustomCardBase], this approach
 * ensures adaptability and streamlined maintenance for future requirements. If new requirements arise
 * that are common to some of the [CustomCardBase]'s child classes, they can be added in a single
 * location here, rather than implementing them individually in all child classes of [CustomCardBase].
 *
 * 2) **Using listeners:** - **modification from the classes that instantiate [CustomCardBase]'s child classes.**
 * In order to enable adding some specific behavior to the specific card (child of the [CustomCardBase]),
 * but out of that child class, Listener Design Pattern is used. If there is some behavior needed to
 * be set for the specific card out of it, use Listeners. Examples that can be used for reference
 * are [LongClickListener], [ClickListener], [FocusListener]. This approach is combined with the previous
 * one (with the open classes) and enables adding functionalities outside of the class. Please refer to
 * the following example, which explains how to implement those listeners (class [CustomCardChannelList]
 * will be used in this example, but the same stands for all the child of the [CustomCardBase] class):
 *
 * ```kotlin
 * // BASE CLASS
 *
 * // In this BASE CLASS, create an Interface that will represent the listener as follows:
 * interface FocusListener {
 *     fun onFocusChanged(hasFocus: Boolean)
 * }
 *
 * // Also, in the base class, create a method that will enable setting the listener:
 * fun setFocusListener(listener: FocusListener) {
 *     customFocusListener = listener
 * }
 *
 * // ----------------------------------------------------------------------------------------------
 * // CLASS WHERE CHILD OF THE BASE CLASS IS BEING INSTANTIATED:
 *
 * // Create an instance of CustomCardChannelList (or inflate it from the XML file)
 * val customCardChannelList = CustomCardChannelList(context)
 *
 * // Implement the desired listener; in this example, FocusListener will be implemented
 * customCardChannelList.setFocusListener(object : FocusListener {
 *                 override fun onFocusChanged(hasFocus: Boolean) {
 *                     if (hasFocus) {
 *                         triggerItemSelected(position) // Example of the code that will be triggered
 *                         once the object gains focus
 *                     }
 *                 }
 *             })
 * ```
 *
 * ***Notes for Improvement*** - If you identify areas for improvement or need to refactor existing code, feel free to add comments
 * or suggestions for enhancement.
 *
 * ***Usage Guidelines*** - Extend this class for creating custom card types.
 * - Implement interfaces like [FocusListener], [LongClickListener], and [ClickListener] for handling specific events.
 * - Customize behaviors as needed for individual card types.
 * - Exercise caution to maintain consistency with the core functionalities of this base class.
 *
 * ***Best Practices:***
 * - Document methods, variables, and classes to ensure code readability.
 * - Provide examples of usage for clarity on extending and implementing this base class.
 *
 * @author Boris Tirkajla
 *
 */

abstract class CustomCardBase : ConstraintLayout, CustomCardBaseViews {

    protected val cardElevation = Utils.getDimens(R.dimen.custom_dim_8)
    protected val cardElevationNormal = Utils.getDimens(R.dimen.custom_dim_0)
    protected val cardScaleX = Utils.Companion.Scale.CUSTOM_CARD
    protected val cardScaleY = Utils.Companion.Scale.CUSTOM_CARD
    protected val cardScaleXNormal = Utils.Companion.Scale.NORMAL
    protected val cardScaleYNormal = Utils.Companion.Scale.NORMAL
    private val colorStrokeFocused = Color.parseColor(ConfigColorManager.getColor("color_selector"))
    protected val colorStrokeUnfocused = Color.TRANSPARENT
    protected val colorStrokeKeepFocused = Color.parseColor("#4de8f0fe")
    protected val colorFocusedCardBackground = Color.parseColor("#FF808080")
    protected val colorUnfocusedCardBackground = Color.parseColor("#CC808080")
    private val fontRegular = TypeFaceProvider.getTypeFace(
        context,
        ConfigFontManager.getFont("font_regular")
    )

    /**
     * Listener for handling long-click events on the card.
     * If set, this listener triggers when a long-click event occurs on the card.
     */
    private var customLongClickListener: LongClickListener? = null

    /**
     * Listener for handling click events on the card.
     * If set, this listener triggers when a click event occurs on the card.
     */
    private var customClickListener: ClickListener? = null

    /**
     * Listener for receiving notifications about focus changes on the card.
     * If set, this listener triggers when the focus state of the card changes.
     */
    private var customFocusListener: FocusListener? = null

    /**
     * Animator for handling background color changes during focus transitions.
     * Used to smoothly animate the card's background color when gaining or losing focus.
     */
    private var valueAnimator: ValueAnimator? = null

    /**
     * Timer to introduce a delay before animating the card, allowing for smoother transitions
     */
    private var channelListUpdateTimer: CountDownTimer? = null

    /**
     * Flag indicating whether an animation is currently in progress for the Custom Card.
     * When set to `true`, it prevents triggering additional animations until the ongoing one completes.
     * This helps avoid unintended behavior when handling multiple user interactions simultaneously.
     */
    private var isAnimationInProgress: Boolean = false

    /**
     * Flag determining whether the Custom Card should maintain focus even when it is not actively selected.
     * When set to `true`, the card retains a focused appearance, providing a visual cue to the user.
     * This is useful for indicating special conditions or states related to the card.
     */
    protected var shouldKeepFocus: Boolean = false

    /**
     * Flag indicating whether the TV event associated with the Custom Card is locked or restricted.
     * When set to `true`, it implies that the TV event has some form of parental control or restriction applied.
     * This flag helps customize the card's behavior and appearance based on the locked TV event status.
     */
    protected var isTvEventLocked: Boolean = false

    constructor(context: Context) : super(context) {
        initialisation()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialisation()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialisation()
    }

    init {
        setupOnClickListener()
        setupOnKeyListener()
        setupOnFocusChangeListener()
        setupOnLongClickListener()
    }

    /**
     * Performs the initial setup for a custom view, setting the typeface of title and description text views.
     * Subclasses MUST override this method to ensure proper initialization.
     *
     * IMPORTANT: It's crucial to override this method in the child class.
     * Please refer to [CustomCardChannelList.initialisation] for an example implementation.
     *
     * @see [CustomCardChannelList.initialisation]
     */
    protected open fun initialisation() {
        // Set the typeface for the title text view
        titleTextView.typeface = fontRegular

        // Set the typeface for the description text view
        descriptionTextView.typeface = fontRegular
    }

    /**
     * Handles focus changes for a custom card view, updating its appearance and triggering animations.
     *
     * @param hasFocus True if the card has gained focus, false otherwise.
     */
    protected open fun onFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            // Update appearance for focused state
            customCardMaterialCardView.strokeColor = colorStrokeFocused
            valueAnimator?.cancel()
            valueAnimator = customCardMaterialCardView.animateBackgroundColor(
                endColor = colorFocusedCardBackground
            )
            shouldKeepFocus = false
            startUpdateTimer()
        } else {
            // Update appearance for unfocused state
            stopUpdateTimer() // when Card doesn't have focus reset timer (it was started when Card gained focus) to avoid animating the Card
            if (!shouldKeepFocus) {
                customCardMaterialCardView.strokeColor = colorStrokeUnfocused
                valueAnimator?.cancel()
                valueAnimator = customCardMaterialCardView.animateBackgroundColor(
                    endColor = colorUnfocusedCardBackground
                )
                Utils.focusAnimation(
                    customCardMaterialCardView,
                    elevation = cardElevationNormal,
                    scaleX = cardScaleXNormal,
                    scaleY = cardScaleYNormal
                )
                customCardMaterialCardView.cardElevation = 0f
            } else {
                customCardMaterialCardView.strokeColor = colorStrokeKeepFocused
            }
        }
        customFocusListener?.onFocusChanged(hasFocus) // the listener is optional, indicated by the safe call operator `?`.
    }

    /**
     * Handles the visibility of the locked image view.
     *
     * The locked image view (lockedImageView) will be visible only when both parameters are set to true.
     *
     * @param isChannelLocked Value indicating whether the channel is locked (TvChannel.isLocked).
     * @param isParentalEnabled Value indicating whether Parental Control is generally enabled in the app.
     */
    protected fun handleLockedImageViewVisibility(
        isChannelLocked: Boolean, // this is data from TvChannel - if it is locked or not
        isParentalEnabled: Boolean // this is data about general Parental Control in our app, whether it is enabled or not
    ) {
        lockImageView.updateVisibility(isChannelLocked && isParentalEnabled)
    }

    protected open fun onClick() {
        customClickListener?.onClick()
    }

    protected open fun onLongClick() {
        customLongClickListener?.onLongClick()
    }

    protected open fun onDpadRightPressed(): Boolean {
        return false
    }

    protected open fun onDpadLeftPressed(): Boolean {
        return false
    }

    protected open fun onDpadDownPressed(): Boolean {
        return false
    }

    protected open fun onDpadUpPressed(): Boolean {
        return false
    }

    protected open fun onChannelDownPressed(): Boolean {
        return false
    }

    protected open fun onChannelUpPressed(): Boolean {
        return false
    }

    protected open fun onMediaRecordPressed(): Boolean {
        return false
    }

    protected open fun onCaptionsPressed(): Boolean {
        return false
    }

    protected open fun onBackPressed() : Boolean {
        return false
    }

    /**
     * Updates the title displayed in the titleTextView based on the provided title parameter.
     * If the TV event is locked (isTvEventLocked is true), the titleTextView will be set to
     * the string representing parental control restrictions. Otherwise, it sets the titleTextView
     * to the provided title or a default "no information" string if the title is null.
     *
     * @param title The title to be displayed in the titleTextView. Can be null.
     */
    protected fun updateTitle(title: String?) {
        if (isTvEventLocked) {
            titleTextView.text = ConfigStringsManager.getStringById("parental_control_restriction")
            return
        }
        titleTextView.text = title ?: ConfigStringsManager.getStringById("no_information")
    }

    protected fun updateWatchImageView(isChannelCurrent: Boolean) {
        watchImageView.updateVisibility(isChannelCurrent)
    }

    /**
     * Updates the description displayed in the descriptionTextView based on the provided description parameter.
     * If the TV event is locked (isTvEventLocked is true), the descriptionTextView will be set to
     * the string representing parental control restrictions. Otherwise, it sets the descriptionTextView
     * to the provided description or a default "no information" string if the description is null.
     *
     * @param description The description to be displayed in the descriptionTextView. Can be null.
     */
    protected fun updateDescription(description: String?) {
        if (isTvEventLocked) {
            descriptionTextView.text =
                ConfigStringsManager.getStringById("parental_control_restriction")
            return
        }
        descriptionTextView.text =
            description ?: ConfigStringsManager.getStringById("no_information")
    }

    /**
     * called when Card should be animated and have border even if not focused at the moment.
     */
    fun keepFocus() {
        this.shouldKeepFocus = true
    }
    fun setFocusListener(listener: FocusListener) {
        customFocusListener = listener
    }

    fun setLongClickListener(listener: LongClickListener) {
        this.customLongClickListener = listener
    }

    fun setClickListener(listener: ClickListener) {
        this.customClickListener = listener
    }

    private fun setupOnFocusChangeListener() {
        this.setOnFocusChangeListener { _, hasFocus ->
            onFocusChanged(hasFocus)
        }
    }

    private fun setupOnLongClickListener() {
        this.setOnLongClickListener {
            isAnimationInProgress = true
            Utils.viewClickAnimation(it, object : AnimationListener {
                override fun onAnimationEnd() {
                    isAnimationInProgress = false
                    onLongClick()
                }
            })
            return@setOnLongClickListener true
        }
    }

    /**
     * Sets up the key listener for handling key events on the Custom Card.
     * Supported key events include directional navigation (DPAD), media controls, and other custom actions.
     */
    private fun setupOnKeyListener() {
        setOnKeyListener { _, keyCode, keyEvent ->
            when (keyEvent.action) {
                KeyEvent.ACTION_DOWN -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            return@setOnKeyListener onDpadRightPressed()
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            return@setOnKeyListener onDpadLeftPressed()
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            return@setOnKeyListener onDpadDownPressed()
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            return@setOnKeyListener onDpadUpPressed()
                        }

                        KeyEvent.KEYCODE_MEDIA_RECORD -> {
                            return@setOnKeyListener onMediaRecordPressed()
                        }

                        KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            return@setOnKeyListener onChannelDownPressed()
                        }

                        KeyEvent.KEYCODE_CHANNEL_UP -> {
                            return@setOnKeyListener onChannelUpPressed()
                        }

                        KeyEvent.KEYCODE_CAPTIONS -> {
                            return@setOnKeyListener onCaptionsPressed()
                        }

                        KeyEvent.KEYCODE_BACK -> {
                            downActionBackKeyDone = true
                        }
                    }
                }

                KeyEvent.ACTION_UP -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK,
                        KeyEvent.KEYCODE_ESCAPE -> {
                            if (!downActionBackKeyDone) return@setOnKeyListener true
                            return@setOnKeyListener onBackPressed()
                        }
                    }
                }
            }
            return@setOnKeyListener false
        }
    }

    private fun setupOnClickListener() {
        this.setOnClickListener {
            if (!isAnimationInProgress) {
                isAnimationInProgress = true
                Utils.viewClickAnimation(it, object : AnimationListener {
                    override fun onAnimationEnd() {
                        isAnimationInProgress = false
                        onClick()
                    }
                })
            }
        }
    }

    /**
     * Initiates a countdown timer to trigger a focus animation on the Custom Card, provided the user
     * stays on it for at least 250 milliseconds. This prevents unnecessary animations during card scrolling.
     */
    private fun startUpdateTimer() {
        // Cancel timer if it's already started
        stopUpdateTimer()

        // Start new count down timer
        channelListUpdateTimer = object :
            CountDownTimer(
                250,
                250
            ) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                // Perform focus animation on the Custom Card
                Utils.focusAnimation(
                    view = customCardMaterialCardView,
                    elevation = cardElevation,
                    scaleX = cardScaleX,
                    scaleY = cardScaleY
                )
            }
        }
        channelListUpdateTimer!!.start()
    }

    /**
     * Stops the update timer if it's running.
     */
    private fun stopUpdateTimer() {
        if (channelListUpdateTimer != null) {
            channelListUpdateTimer!!.cancel()
            channelListUpdateTimer = null
        }
    }
}

/**
 * Interface defining mandatory views for classes that inherit from [CustomCardBase].
 * In the future, if some views are not utilized by child classes, consider moving them out of this interface.
 * @see [CustomCardChannelListViews]
 */
interface CustomCardBaseViews {
    // MaterialCardView representing the card container
    val customCardMaterialCardView: MaterialCardView

    // TextView displaying the title of the card
    val titleTextView: TextView

    // TextView providing a description or additional information
    val descriptionTextView: TextView

    // ImageView for indicating a locked state
    val lockImageView: ImageView

    // ImageView for indicating if channel is currently being watched
    val watchImageView: ImageView

    // ImageView for indicating a skip or next action
    val skipImageView: ImageView
}

/**
 * Interface for receiving notifications about focus changes.
 */
interface FocusListener {
    /**
     * Called when the focus state changes.
     *
     * @param hasFocus `true` if the view has gained focus, `false` otherwise.
     */
    fun onFocusChanged(hasFocus: Boolean)
}

/**
 * Interface for handling long-click events.
 */
interface LongClickListener {
    /**
     * Called when a long-click event occurs.
     */
    fun onLongClick()
}

/**
 * Interface for handling click events.
 */
interface ClickListener {
    /**
     * Called when a click event occurs.
     */
    fun onClick()
}

fun View.updateVisibility(isVisible: Boolean) {
    this.visibility = if (isVisible) View.VISIBLE else View.GONE
}