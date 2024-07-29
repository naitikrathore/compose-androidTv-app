package com.iwedia.cltv.components
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.custom_view_base_classes.BaseConstraintLayout
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSpeakTextForFocusedViewInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "CustomButton"
class CustomButton : BaseConstraintLayout, IMarqueeEffect, TTSSpeakTextForFocusedViewInterface {

    var buttonType: ButtonType = ButtonType.DEFAULT
    // COLORS
    private var colorButton = Color.parseColor(ConfigColorManager.getColor("color_button"))
    private val colorSelector = Color.parseColor(ConfigColorManager.getColor("color_selector"))
    private val colorBackground = Color.parseColor(
        ConfigColorManager.getColor("color_background")
            .replace(
                "#",
                ConfigColorManager.alfa_86
            )
    )
    private val fontRegular = TypeFaceProvider.getTypeFace(
        ReferenceApplication.applicationContext(),
        ConfigFontManager.getFont("font_regular")
    )
    private var onFocusChanged: (hasFocus: Boolean) -> Unit = {}
    private var onClick: () -> Unit = {}
    var isAnimationInProgress: Boolean = false
    private var shoudldBeClickable: Boolean = true
    private lateinit var buttonCardView: MaterialCardView
    private lateinit var buttonLabelTextView: TextView
    private lateinit var holderImageView: ImageView
    private lateinit var contentContainerLinearLayout: LinearLayout
    private lateinit var spacerView: View //spacer used to create padding between icon and text - this is not needed when buttonType is set to ICON_ONLY
    private lateinit var checkImageView: ImageView
    private var isBackgroundTransparent = false

    private var shouldKeepFocus: Boolean = false
    //used for all views that need to be added dynamically
    lateinit var layout: LinearLayout
    private var preventClip = false

    override var isMarqueeEnabled: Boolean = false


    /**
     *  Constructor used for buttons that are set dynamically
     */
    constructor(context: Context, preventClip: Boolean = false) : super(context) {
        this.preventClip = preventClip
        initialisation()
    }
    /**
     *  Constructor used for buttons that are set in xml
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        getStuffFromXML(attrs = attrs, context = context)
        initialisation()
        initialiseProperties()
    }
    /**
     *  Constructor used for buttons that are set in xml
     */
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(
        context,
        attrs,
        defStyleAttrs
    ) {
        getStuffFromXML(attrs = attrs, context = context)
        initialisation()
        initialiseProperties()
    }
    /**
     *  Constructor used for new buttons that are not supported in ButtonType enum class
     *
     *  Not recommended for usage, add the needed button design to ButtonType enum class
     *
     */
    constructor(context: Context, buttonType: ButtonType, text: String? = null, resource: Int? = null): super(context){
        this.buttonType = buttonType
        initialisation()
        initialiseProperties(text, resource)
    }
    private fun getStuffFromXML(attrs: AttributeSet?, context: Context) {
        val data = context.obtainStyledAttributes(attrs, R.styleable.CustomButton)
        buttonType = when (data.getInt(R.styleable.CustomButton_button_type, -1)) {
            0 -> ButtonType.SKIP
            1 -> ButtonType.UNSKIP
            2 -> ButtonType.ADD_TO_FAVORITES
            3 -> ButtonType.EDIT_FAVORITES
            4 -> ButtonType.LOCK
            5 -> ButtonType.UNLOCK
            6 -> ButtonType.SEARCH
            7 -> ButtonType.SEARCH_WITH_TEXT
            8 -> ButtonType.MORE_INFO
            9 -> ButtonType.WATCHLIST
            10 -> ButtonType.WATCHLIST_REMOVE
            11 -> ButtonType.AUDIO
            12 -> ButtonType.CC_OR_SUBTITLE
            13 -> ButtonType.RECORD
            14 -> ButtonType.CANCEL_RECORDING
            15 -> ButtonType.KEYBOARD
            16 -> ButtonType.RENAME
            17 -> ButtonType.FILTER
            18 -> ButtonType.START_OVER
            19 -> ButtonType.WATCH
            20 -> ButtonType.DELETE
            21 -> ButtonType.NOT_CHECKED
            22 -> ButtonType.CHECKED
            23 -> ButtonType.SKIP_OR_UNSKIP
            24 -> ButtonType.ADD_TO_FAVORITE
            25 -> ButtonType.CANCEL
            26 -> ButtonType.CUSTOM_RECORDING
            27 -> ButtonType.RETRY
            28 -> ButtonType.EXIT
            29 -> ButtonType.CUSTOM_TEXT
            30 -> ButtonType.BACKSPACE
            31 -> ButtonType.OK
            32 -> ButtonType.CHECK
            33 -> ButtonType.CONTINUE
            34 -> ButtonType.EDIT_CHANNEL
            35 -> ButtonType.RED
            36 -> ButtonType.BLUE
            37 -> ButtonType.GREEN
            38 -> ButtonType.YELLOW
            else -> ButtonType.DEFAULT
        }

        data.recycle()
    }
    /**
     *  Initialises button layout parameters, click and focus changes
     */
    private fun initialisation() {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context)
            .inflate(R.layout.custom_button, this, true)
        contentContainerLinearLayout = findViewById(R.id.content_container_linear_layout)
        layout = findViewById(R.id.layout)

        buttonCardView = findViewById<MaterialCardView?>(R.id.custom_button_card_view).apply {
            setCardBackgroundColor(colorButton)
        }
        buttonLabelTextView = findViewById<TextView?>(R.id.button_label_text_view).apply {
            setTextColor(colorSelector)
            //typeface = fontRegular
        }
        holderImageView = findViewById<ImageView?>(R.id.holder_image_view).apply {
            if(buttonType != ButtonType.RED && buttonType != ButtonType.YELLOW && buttonType != ButtonType.GREEN && buttonType != ButtonType.BLUE) {
                setColorFilter(colorSelector)
            }
        }
        checkImageView = findViewById(R.id.check_image_view)
        spacerView = findViewById(R.id.spacer_view)
        buttonCardView.setCardBackgroundColor(colorButton)
        this.setOnFocusChangeListener { view, hasFocus ->
            if(buttonType != ButtonType.RED && buttonType != ButtonType.YELLOW && buttonType != ButtonType.GREEN && buttonType != ButtonType.BLUE) {
                checkImageView.setColorFilter(if (hasFocus) colorBackground else colorSelector)
                holderImageView.setColorFilter(if (hasFocus) colorBackground else colorSelector)
            }
            if (isBackgroundTransparent){
                buttonCardView.setCardBackgroundColor(if (hasFocus) colorSelector else Color.TRANSPARENT)
            }
            else {
                buttonCardView.setCardBackgroundColor(if (hasFocus) colorSelector else colorButton)
            }
            buttonLabelTextView.setTextColor(if (hasFocus) colorBackground else colorSelector)

            updateMarqueeEffect(hasFocus)

            if (hasFocus) {

                speakTextForFocusedView()

                shouldKeepFocus = false
                CoroutineScope(Dispatchers.Main).launch {// if this Coroutine is removed animation won't work as expected when clicking DPAD_UP or DPAD_DOWN on buttons in ChannelListScene and ChannelListSearchScene
                    Utils.focusAnimation(view!!, preventClip, scale = Utils.Companion.Scale.BUTTON)
                }
            } else {
                if (!shouldKeepFocus) {
                    CoroutineScope(Dispatchers.Main).launch {// if this Coroutine is removed animation won't work as expected when clicking DPAD_UP or DPAD_DOWN on buttons in ChannelListScene and ChannelListSearchScene
                        Utils.unFocusAnimation(view!!)
                    }
                } else {
                    buttonCardView.setCardBackgroundColor(colorButton)
                }
            }

            if(layout.childCount > 0){
                layout.children.forEach {
                    if ( it is ImageView){
                        it.setColorFilter(if (hasFocus) colorBackground else colorSelector)}
                }
            }

            onFocusChanged.invoke(hasFocus) // This must go at the end of the onFocusChanged listener. It's important for TTS - firs in this file on hasFocus text should be set and than this lambda is called from the outside of this class with method that uses that string.

        }
        this.setOnClickListener {
            if (!isAnimationInProgress && shoudldBeClickable) {
                isAnimationInProgress = true
                Utils.viewClickAnimation(it, object : AnimationListener {
                    override fun onAnimationEnd() {
                        isAnimationInProgress = false
                        onClick.invoke()
                    }
                })
            }
        }
    }
    /**
     *  Initialises button properties (text and image) based on parameters
     */
    private fun initialiseProperties(text: String?, resource: Int?) {
        text?.let { _text ->
            resource?.let { _resource ->
                initialiseButtonWithIconAndText(_text, _resource)
                return
            }
        }
        text?.let { _text ->
            initialiseButtonWithText(_text)
        }

        resource?.let { _resource ->
            initialiseButtonWithIcon(_resource)
        }
    }
    /**
     *  Initialises button properties (text and drawable image) based on ButtonType
     */
    private fun initialiseProperties() {
        @SuppressLint("SetTextI18n")
        when (buttonType) {
            ButtonType.DEFAULT -> {
                buttonLabelTextView.apply {
                    visibility = VISIBLE
                    text = "THIS IS DEFAULT IMPLEMENTATION"
                }
            }
            ButtonType.CUSTOM_RECORDING -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.SKIP -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.UNSKIP -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.RECORD -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.CANCEL_RECORDING -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.ADD_TO_FAVORITES -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.EDIT_FAVORITES -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.START_OVER -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.LOCK -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.DELETE -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.SEARCH -> {
                initialiseButtonWithIcon(buttonType.resource!!)
                setTransparentBackground(true)
            }
            ButtonType.SEARCH_WITH_TEXT -> initialiseButtonWithIconAndText( buttonType.text!!, buttonType.resource!!)
            ButtonType.FILTER -> {
                initialiseButtonWithIcon(buttonType.resource!!)
                setTransparentBackground(true)
            }
            ButtonType.MORE_INFO -> initialiseButtonWithText(buttonType.text!!)
            ButtonType.WATCHLIST -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.WATCHLIST_REMOVE -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.AUDIO -> initialiseButtonWithIcon(buttonType.resource!!)
            ButtonType.CC_OR_SUBTITLE -> initialiseButtonWithIcon(buttonType.resource!!)
            ButtonType.KEYBOARD -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.WATCH -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.CONTINUE_WATCH -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.RENAME -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.CANCEL -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.RETRY -> initialiseButtonWithText(buttonType.text!!)
            ButtonType.EXIT -> initialiseButtonWithText(buttonType.text!!)
            //remove this once channel list buttons are changed
            ButtonType.ADD_TO_FAVORITE -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.SKIP_OR_UNSKIP -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)
            ButtonType.BACKSPACE -> initialiseButtonWithIcon(buttonType.resource!!)
            ButtonType.RED -> initialiseButtonWithIcon(buttonType.resource!!)
            ButtonType.YELLOW -> initialiseButtonWithIcon(buttonType.resource!!)
            ButtonType.GREEN -> initialiseButtonWithIcon(buttonType.resource!!)
            ButtonType.BLUE -> initialiseButtonWithIcon(buttonType.resource!!)
            ButtonType.OK -> initialiseButtonWithText(buttonType.text!!)
            ButtonType.CUSTOM_TEXT -> initialiseButtonWithText("")
            ButtonType.CONTINUE -> initialiseButtonWithText(buttonType.text!!)
            ButtonType.EDIT_CHANNEL -> initialiseButtonWithIconAndText(buttonType.text!!, buttonType.resource!!)

            else -> {}
        }
        refreshDrawableState()
    }
    private fun setPadding(horizontalPadding: Double, verticalPadding: Double) {
        contentContainerLinearLayout.setPadding(
            Utils.convertDpToPixel(horizontalPadding).toInt(),
            Utils.convertDpToPixel(verticalPadding).toInt(),
            Utils.convertDpToPixel(horizontalPadding).toInt(),
            Utils.convertDpToPixel(verticalPadding).toInt()
        )
    }
    private fun initialiseButtonWithIcon(imageResource: Int) {
        setPadding(horizontalPadding = 10.0, verticalPadding = 10.0)
        holderImageView.apply {
            visibility = VISIBLE
//            this.layoutParams.height = Utils.convertDpToPixel(20.0).toInt()
//            this.layoutParams.width = Utils.convertDpToPixel(20.0).toInt()
            setImageResource(imageResource)
        }
        buttonLabelTextView.apply {
            visibility = GONE
        }

    }
    private fun initialiseButtonWithText(text: String) {
        setPadding(horizontalPadding = 20.0, verticalPadding = 10.0)
        buttonLabelTextView.apply {
            this.text = ConfigStringsManager.getStringById(text)
            visibility = VISIBLE
        }
        holderImageView.visibility = GONE
        spacerView.visibility = GONE
    }
    /**
     * @param imageResource image from local resource. Example of passing this parameter is: R.drawable.search_icon
     */
    private fun initialiseButtonWithIconAndText(text: String, imageResource: Int) {
        setPadding(
            horizontalPadding = 20.0,
            verticalPadding = 10.5
        )
        buttonLabelTextView.apply {
            this.text = ConfigStringsManager.getStringById(text)
            visibility = VISIBLE
        }
        holderImageView.apply {
            visibility = VISIBLE
            setImageResource(imageResource)
        }
        spacerView.visibility = View.VISIBLE
    }
    /**
     * used to set lambda function for OnClickListener.
     *
     * Passed lambda function will be executed every time when Card is clicked.
     *
     * By default onClick variable, which is used to store lambda expression, is set to empty lambda - when Card is clicked it won't have any additional functionality beside automatic animation.
     */
    fun setOnClick(onClick: () -> Unit) {
        this.onClick = onClick
    }

    fun getTextLabel(): String {
        return (buttonLabelTextView.text.toString().ifBlank {
            buttonType.text!!
        })
    }


    fun setTextLabel(text: String) {
        this.buttonLabelTextView.text = text
    }

    /**
     * [keepFocus] is method used to set focus on button even if button losses focus.
     *
     * Example of usage: when user presses back from item but item should have state that is different
     * both from focused and unfocused.
     *
     * Similar method is [forceFocus]. If needed, read [forceFocus]'s documentation for clarity.
     */
    fun keepFocus(shouldKeepFocus: Boolean = true) {
        this.shouldKeepFocus = shouldKeepFocus
    }

    /**
     * [forceFocus] is method used to set [CustomButton]'s background to [colorForcedFocus] when
     * button should have indication that it is being selected but it is not being focused at the same time.
     *
     * [forceFocus] is different from [keepFocus] in way that [forceFocus] is method called on [CustomButton]
     * and it directly updates background color to [colorForcedFocus]. Otherwise, [keepFocus] is method
     * used to change [shouldKeepFocus] flag to true, and that will indirectly reflect to [setOnFocusChangeListener]
     * method where [CustomButton]'s background color is handled.
     */
    fun forceFocus() {
        buttonCardView.setCardBackgroundColor(colorButton)
    }

    fun releaseFocus() {
        if (isBackgroundTransparent){
            buttonCardView.setCardBackgroundColor(Color.TRANSPARENT)
        }
        else {
            buttonCardView.setCardBackgroundColor(colorButton)
        }
    }

    /**
     * used to set lambda function for OnFocusChangedListener.
     *
     * Passed lambda function will be executed every time when Card changes focus.
     *
     * By default onFocusChanged variable, which is used to store lambda expression, is set to empty lambda - when Card is focused it won't have any additional functionality beside automatic animation and border color changing.
     */
    fun setOnFocusChanged(onFocusChanged: (Boolean) -> Unit) {
        this.onFocusChanged = onFocusChanged
    }

    fun setMargins(startInDp: Double = 0.0, topInDp: Double = 0.0, endInDp: Double = 0.0, bottomInDp: Double = 0.0) {
        val param = this.layoutParams as MarginLayoutParams
        param.setMargins(
            Utils.convertDpToPixel(startInDp).toInt(),
            Utils.convertDpToPixel(topInDp).toInt(),
            Utils.convertDpToPixel(endInDp).toInt(),
            Utils.convertDpToPixel(bottomInDp).toInt())
        this.layoutParams = param
    }

    /**
     *
     * Used for updating button inside adapter with button type
     *
     */
    fun update(buttonType: ButtonType){
        this.buttonType = buttonType

        //remove all dynamically added views
        layout.removeAllViews()

        //initialize custom button with default properties
        initialiseProperties()
    }

    /**
     *
     * Used for updating button inside adapter with button type, text and resource
     *
     */
    fun update(buttonType: ButtonType, text: String? = null, resource: Int? = null) {
        this.buttonType = buttonType

        //remove all dynamically added views
        layout.removeAllViews()

        //initialize custom button with default properties
        initialiseProperties(text, resource)
    }

    /**
     * method used to switch between checked or unchecked.
     *
     * When this method is called on CustomButton it will show or hide check icon defined in xml file.
     */
    fun switchCheckedState() {
        if (checkImageView.isVisible) {
            checkImageView.visibility = GONE
        } else {
            checkImageView.visibility = VISIBLE
        }
        speakTextForFocusedView()
    }

    fun addIcons(resources: MutableList<Int>) {
        resources.forEach {resource ->
            var spacing = View(this.context)
            var imageView = ImageView(this.context)
            layout.addView(spacing)
            layout.addView(imageView)
            spacing.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
            spacing.layoutParams.width = 8 * context!!.getResources().getDisplayMetrics().density.toInt()
            imageView.layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            imageView.layoutParams.width = 15 * context!!.getResources().getDisplayMetrics().density.toInt()

            layout.children.forEach {view ->
                if (view is ImageView) {
                    view.setColorFilter(if (hasFocus()) colorBackground else colorSelector)
                }
            }

            imageView.setImageResource(resource!!)

        }
    }

    /**
     *
     * Used for setting transparent background of a button
     *
     */
    fun setTransparentBackground(isBackgroundTransparent: Boolean){
        this.isBackgroundTransparent = isBackgroundTransparent

        if (this.isBackgroundTransparent)
        {
            buttonCardView.setCardBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun setButtonClickable(value: Boolean) {
        shoudldBeClickable = value
    }

    /**
     * Sets the width of the current view to match its parent's width.
     * Also sets the width of the view with ID 'root_constraint_layout' to match its parent's width.
     * By default, a Button's width is set to wrap its content. There are use cases
     * when we need the Button to be aligned with its parent, which is achieved by
     * setting the width to match_parent.
     */
    fun setWidthToMatchParent() {
        val layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        this.layoutParams = layoutParams
        findViewById<ConstraintLayout>(R.id.root_constraint_layout).layoutParams = layoutParams
        buttonCardView.layoutParams = layoutParams
    }

    //############################## IMarqueeEffect ###########################################
    override fun enableMarqueeEffect() {
        isMarqueeEnabled = true
    }
    override fun updateMarqueeEffect(hasFocus: Boolean) {
        if (isMarqueeEnabled) {
            buttonLabelTextView.isSelected = hasFocus
        }
    }
    //############################## END OF IMarqueeEffect ####################################

    override fun speakTextForFocusedView() {
        try {
            if (buttonType == ButtonType.CHECK) {
                // when button is of type CHECK than it has prefix before the title. That's the reason why depends on buttonType we have 2 cases.
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "speakTextForFocusedView: CHECK true")
                textToSpeechHandler.setSpeechTextForSelectableView(
                    getTextLabel(),
                    type = Type.CHECK,
                    isChecked = checkImageView.isVisible
                )
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "speakTextForFocusedView: CHECK false")
                textToSpeechHandler.setSpeechText(getTextLabel())
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
            // Handle the case when textToSpeechTextSetterInterface is not implemented.
            // You can use setupTextToSpeechTextSetterInterface() method from the place where this error occurred.
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Error: textToSpeechTextSetterInterface is not implemented. Make sure to call setupTextToSpeechTextSetterInterface() to set the interface.\"")
        }
    }
}
/**
 * ButtonType is an enum class that stores all button design types that are needed
 *
 * Every button can have text, drawable image or both
 *
 * If there is a button design that is missing from this class, first add it here and then use it as CustomButton
 *
 */
enum class ButtonType(var text: String? = null, val resource: Int? = null) {
    DEFAULT("THIS IS DEFAULT IMPLEMENTATION"),
    WATCH(
        text = ConfigStringsManager.getStringById("watch"),
        resource = R.drawable.watch_icon
    ),
    CONTINUE_WATCH(
        text = ConfigStringsManager.getStringById("continue_watch"),
        resource = R.drawable.watch_icon
    ),
    SKIP(
        text = ConfigStringsManager.getStringById("skip"),
        resource = R.drawable.ic_skip
    ),
    UNSKIP(
        text = ConfigStringsManager.getStringById("unskip"),
        resource = R.drawable.ic_skip
    ),
    RECORD(
        text = ConfigStringsManager.getStringById("record"),
        resource = R.drawable.record_icon
    ),
    CANCEL_RECORDING(
        text = ConfigStringsManager.getStringById("cancel"),
        resource = R.drawable.record_icon
    ),
    ADD_TO_FAVORITES(
        text = ConfigStringsManager.getStringById("add_to_favorites"),
        resource = R.drawable.favorite_icon,
    ),
    EDIT_FAVORITES(
        text = ConfigStringsManager.getStringById("edit_favorites"),
        resource = R.drawable.favorite_icon,
    ),
    LOCK(
        text = ConfigStringsManager.getStringById("lock"),
        resource = R.drawable.ic_lock
    ),
    UNLOCK(
        text = ConfigStringsManager.getStringById("unlock"),
        resource = R.drawable.ic_lock
    ),
    DELETE(
        text = ("delete"),
        resource = R.drawable.delete
    ),
    SEARCH(
        text = ConfigStringsManager.getStringById("search"),
        resource = R.drawable.search_icon
    ),
    SEARCH_WITH_TEXT(
        text = ConfigStringsManager.getStringById("search"),
        resource = R.drawable.search_icon
    ),
    FILTER(
        text = "Filter", // TODO ADD_TEXT
        resource = R.drawable.filter_icon
    ),
    START_OVER(
        text = ConfigStringsManager.getStringById("start_over"),
        resource = R.drawable.catchup_icon
    ),
    KEYBOARD(
        text = ConfigStringsManager.getStringById("keyboard"),
        resource = R.drawable.keyboard
    ),
    AUDIO(
        text = ConfigStringsManager.getStringById("audio"),
        resource = R.drawable.audio_ic_new
    ),
    CC_OR_SUBTITLE(
        text = ConfigStringsManager.getStringById("subtitles"),
        resource = R.drawable.subtitles_ic_new
    ),
    WATCHLIST(
        text = ConfigStringsManager.getStringById("watchlist"),
        resource = R.drawable.watchlist
    ),
    WATCHLIST_REMOVE(
        text = ConfigStringsManager.getStringById("watchlist_remove"),
        resource = R.drawable.watchlist
    ),
    MORE_INFO(
        text = ConfigStringsManager.getStringById("more_info"),
    ),
    RENAME(
        text = ConfigStringsManager.getStringById("rename"),
        resource = R.drawable.rename
    ),
    CANCEL(
        text = ConfigStringsManager.getStringById("cancel"),
        resource = R.drawable.cancel
    ),
    CHECK,
    // TODO NOT_CHECKED should be deleted in the future. Instead CHECK should be used.
    NOT_CHECKED,
    // TODO CHECKED should be deleted in the future. Instead CHECK should be used.
    CHECKED(
        resource = R.drawable.ic_check
    ),
    CONTINUE(text = ConfigStringsManager.getStringById("continue")),
    EDIT_CHANNEL(text = ConfigStringsManager.getStringById("edit_channel"), resource = R.drawable.edit_icon),
    CUSTOM_RECORDING( // used in ReferenceWidgetRecordings.kt
        resource = R.drawable.ic_recording_add_unfocused,
        text = ConfigStringsManager.getStringById("custom_recordings")
    ),
    //TODO remove this once channel list widget buttons are changed
    SKIP_OR_UNSKIP(
    text = ConfigStringsManager.getStringById("skip"),
    resource = R.drawable.ic_skip
    ),
    ADD_TO_FAVORITE(
        text = ConfigStringsManager.getStringById("add_to_favorites"),
        resource = R.drawable.favorite_icon,
    ),
    RETRY(
        text = ConfigStringsManager.getStringById("retry")
    ),
    EXIT(
        text = ConfigStringsManager.getStringById("exit")
    ),
    CUSTOM_TEXT(
        text = ""
    ),
    BACKSPACE(
        text = ConfigStringsManager.getStringById("delete"),
        resource = R.drawable.backspace
    ),
    RED(
        resource = R.drawable.red_button
    ),
    BLUE(
        resource = R.drawable.blue_button
    ),
    YELLOW(
        resource = R.drawable.yellow_button
    ),
    GREEN(
        resource = R.drawable.green_button
    ),
    OK(
        text = ConfigStringsManager.getStringById("OK")
    )

}

interface IMarqueeEffect {

    /**
     * Controls whether the marquee effect is enabled for the TextView.
     *
     * When `true`, the TextView scrolls its text horizontally if it's too long to fit.
     *
     * Example:
     * ```
     * isMarqueeEnabled = true  // Enable marquee
     * isMarqueeEnabled = false // Disable marquee
     * ```
     *
     * @property isMarqueeEnabled Boolean flag to enable or disable the marquee effect.
     */
    var isMarqueeEnabled: Boolean

    /**
     * Updates the marquee effect based on the current focus status.
     * If the marquee effect is enabled, this function will set the 'isSelected' property of the
     * buttonLabelTextView to match the 'hasFocus' parameter, which determines if the view has focus.
     *
     * @param hasFocus A boolean indicating whether the view currently has focus.
     */
    fun updateMarqueeEffect(hasFocus: Boolean)

    /**
     * Enables the marquee effect for the TextView.
     * When enabled, the TextView will scroll its content if it's too long to fit in the view's width.
     * This effect is typically used for horizontally scrolling text.
     */
    fun enableMarqueeEffect()
}