package com.iwedia.cltv.components.custom_card


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TimeTextView
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.IMarqueeEffect
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.scene.channel_list.ChannelListItem
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.cltv.utils.Utils.Companion.viewClickAnimation
import core_entities.Error
import kotlinx.coroutines.Dispatchers
import listeners.AsyncReceiver

 val CARD_BORDER_WIDTH = R.dimen.custom_dim_1_5

 val CUSTOM_CARD_SEARCH_CHANNEL_HEIGHT = R.dimen.custom_dim_97
 val CUSTOM_CARD_SEARCH_CHANNEL_WIDTH = R.dimen.custom_dim_152_5

 val CUSTOM_CARD_INFO_BANNER_HEIGHT = R.dimen.custom_dim_112_5
 val CUSTOM_CARD_INFO_BANNER_WIDTH = R.dimen.custom_dim_177_5

 val CUSTOM_CARD_BROADCAST_CHANNEL_HEIGHT = R.dimen.custom_dim_80
 val CUSTOM_CARD_BROADCAST_CHANNEL_WIDTH = R.dimen.custom_dim_120

 val CUSTOM_CARD_DETAIL_SCENE_HEIGHT = R.dimen.custom_dim_170
 val CUSTOM_CARD_DETAIL_SCENE_WIDTH = R.dimen.custom_dim_255

 val ICON_CUSTOM_CARD_FAVORITE_HEART_EMPTY = R.drawable.heart_full_icon
 val ICON_CUSTOM_CARD_FAVORITE_HEART_FULL = R.drawable.heart_empty_icon

private const val TAG = "CustomCard"

/**
 * CustomCard is class with all common features that all subclasses of this class used in project should have as:
 *
 * OnFocusChangeListener:
 *
 * * when card is focused/unfocused it is automatically being animated
 *
 * * when card is focused it has border
 *
 * * when card's property named shouldKeepFocus is set to true and Card is currently not focused - card will be expanded and it will have border (as when it is focused, but border's color is different)
 *
 * OnClickListener:
 *
 * * method setOnClickListener() is implemented with basic logic as click animation
 *
 * OnLongClickListener:
 *
 * * method setOnLongClickListener() is implemented with basic logic as click animation
 *
 * OnClickListener:
 *
 * * method setOnClickListener() is implemented with basic logic as click animation
 *
 * ALSO: properties which are common for all subclasses of class CustomCard are defined here.
 *
 *
 * @author Boris Tirkajla
 */
abstract class CustomCard : TextToSpeechCustomCard, IMarqueeEffect{
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private val colorBackground = Color.parseColor(ConfigColorManager.getColor("color_background"))
    private val colorButton = Color.parseColor(ConfigColorManager.getColor("color_button"))
    private val descriptionText =
        Color.parseColor(ConfigColorManager.getColor("color_text_description"))
    private val colorMainText = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
    private val colorSelector = Color.parseColor(ConfigColorManager.getColor("color_selector"))
    private val fontRegular = TypeFaceProvider.getTypeFace(
        ReferenceApplication.applicationContext(),
        ConfigFontManager.getFont("font_regular")
    )

    // common Views both for ChannelListScreen and InfoBannerScene

    protected lateinit var cardType: CardType
    protected lateinit var favoriteImageView: ImageView
    protected lateinit var channelIndexTextView: TextView
    protected lateinit var channelTypeTextView: TextView
    protected lateinit var skipImageView: ImageView
    protected lateinit var radioImageView: ImageView
    protected lateinit var channelOrEventNameTextView: TextView
    protected lateinit var durationTextView: TextView
    protected lateinit var channelOrEventCardView: MaterialCardView
    protected lateinit var bottomGradientBackground: View
    protected lateinit var contentContainerLinearLayout: LinearLayout
    protected lateinit var contentContainerConstraintLayout: ConstraintLayout
    protected lateinit var iconsContainerLinearLayout: LinearLayout
    private lateinit var mainConstraintLayout: ConstraintLayout
    protected lateinit var channelOrEventImageView: ImageView
    protected lateinit var watchImageView: ImageView
    protected lateinit var catchupImageView: ImageView
    protected lateinit var lockedImageView: ImageView
    protected lateinit var watchlistImageView: ImageView
    protected lateinit var blindEyeImageView: ImageView
    protected lateinit var recordingImageView: ImageView
    protected lateinit var timeTextView: TimeTextView
    private var channelListUpdateTimer: CountDownTimer? = null

    private var isLongPressEnabled: Boolean = true
    private var isAnimated: Boolean = true
    private var shouldKeepFocus: Boolean = false
    var isAnimationInProgress: Boolean = false

    /**
     * The lambda expressions are initialized to an empty lambdas that takes no action when the property is accessed. If needed they can be updated using methods: setOnFocusChanged, setOnClick and setOnLongPress.
     */
    private var onFocusChanged: (hasFocus: Boolean) -> Unit = {}
    private var onClick: () -> Unit = {}
    private var onLongClick: () -> Unit = {}
    private var onCardAnimation: () -> Unit = {}

    protected var tvEvent: TvEvent? = null
    protected var tvChannel: TvChannel? = null
    protected var scheduledRecording: ScheduledRecording? = null
    protected var recording: Recording? = null

    override var isMarqueeEnabled: Boolean = false

    override fun updateMarqueeEffect(hasFocus: Boolean) {
        if (isMarqueeEnabled) {
            channelOrEventNameTextView.isSelected = hasFocus
        }
    }

    override fun enableMarqueeEffect() {
        isMarqueeEnabled = true
    }

    init {
        initialization()
    }

    /**
    animate Custom Card only in user stays on it for at least 250 ms, this is used to prevent animating every Card while scrolling
     */
    private fun startUpdateTimer(view: View) {
        onCardAnimation.invoke()
        //Cancel timer if it's already started
        stopUpdateTimer()

        //Start new count down timer
        channelListUpdateTimer = object :
            CountDownTimer(
                500,
                250
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                CoroutineHelper.runCoroutine(
                    context = Dispatchers.Main,
                    coroutineFun = {
                        Utils.focusAnimation(view, true, scale = Utils.Companion.Scale.EVENT_ITEM)
                        isAnimated = true
                    }
                )
            }
        }
        channelListUpdateTimer!!.start()
    }

    private fun stopUpdateTimer() {
        if (channelListUpdateTimer != null) {
            channelListUpdateTimer!!.cancel()
            channelListUpdateTimer = null
        }
    }

    /**
     * callback used when Card is being animated. Example: in ForYou, when Card is focused automatically Description of Event should be animated.
     */
    fun setOnCardAnimation(onCardAnimation: () -> Unit) {
        this.onCardAnimation = onCardAnimation
    }

    /**
     * Method used to handle lockedImageView
     *
     * lockedImageView will be visible only when both parameters are set to true.
     *
     * @param isChannelLocked value from TvChannel (TvChannel.isLocked) which tells if channel is being locked
     * @param isParentalEnabled value which tells whether Parental Control is, generally, enabled in app
     */
    protected fun handleLockedImageViewVisibility(
        isEventLocked: Boolean,
        isChannelLocked: Boolean, // this is data from TvChannel - if it is locked or not
        isParentalEnabled: Boolean // this is data about general Parental Control in our app, whether it is enabled or not
    ) {
        lockedImageView.visibility = if((isChannelLocked || isEventLocked) && isParentalEnabled) VISIBLE else GONE
    }

    protected fun setCardSize() {
        val params = channelOrEventCardView.layoutParams as LayoutParams

        channelOrEventCardView.layoutParams = params

        when (cardType) {
            CardType.FAVORITE, CardType.SEARCH_CHANNEL -> {
                params.height =
                    Utils.getDimensInPixelSize(CUSTOM_CARD_SEARCH_CHANNEL_HEIGHT)
                params.width = Utils.getDimensInPixelSize(CUSTOM_CARD_SEARCH_CHANNEL_WIDTH)
            }

            CardType.INFO_BANNER,
            CardType.FOR_YOU,
            CardType.VOD,
            CardType.CHANNEL_LIST_SEARCH,
            CardType.SCHEDULED_RECORDING,
            CardType.RECORDING -> {
                params.height = Utils.getDimensInPixelSize(CUSTOM_CARD_INFO_BANNER_HEIGHT)
                params.width = Utils.getDimensInPixelSize(CUSTOM_CARD_INFO_BANNER_WIDTH)
            }
            CardType.DETAILS_SCENE -> {
                params.height = Utils.getDimensInPixelSize(CUSTOM_CARD_DETAIL_SCENE_HEIGHT)
                params.width = Utils.getDimensInPixelSize(CUSTOM_CARD_DETAIL_SCENE_WIDTH)
            }
            CardType.BROADCAST_CHANNEL -> {
                params.height = Utils.getDimensInPixelSize(CUSTOM_CARD_BROADCAST_CHANNEL_HEIGHT)
                params.width = Utils.getDimensInPixelSize(CUSTOM_CARD_BROADCAST_CHANNEL_WIDTH)
            }
        }
        channelOrEventCardView.layoutParams = params
    }
    protected fun resetData() {
        clearAnimation()
        if (!isFocused) {
            isAnimated = false
            scaleX = 1F
            scaleY = 1F
        }
        bottomGradientBackground.visibility = GONE
        channelOrEventNameTextView.text = ""
        channelOrEventNameTextView.visibility = GONE
        favoriteImageView.visibility = GONE
        channelIndexTextView.text = ""
        channelTypeTextView.text = ""
        lockedImageView.visibility = GONE
        skipImageView.visibility = GONE
        radioImageView.visibility = GONE
        watchImageView.visibility = GONE
        timeTextView.visibility = GONE
        timeTextView.text = ""
        // set ImageView's alpha for Event's image
        channelOrEventImageView.setImageResource(0)
        channelOrEventImageView.setImageDrawable(null)
    }

    private fun initialization() {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.custom_card, this, true)

        durationTextView = findViewById<TextView?>(R.id.duration_text_view).apply {
            setTextColor(descriptionText)
            typeface = fontRegular
        }
        watchImageView = findViewById(R.id.watch_image_view)
        catchupImageView = findViewById(R.id.catchup_image_view)
        lockedImageView = findViewById(R.id.lock_image_view)
        watchlistImageView = findViewById(R.id.watchlist_image_view)
        blindEyeImageView = findViewById(R.id.blind_eye_image_view)
        recordingImageView = findViewById(R.id.record_image_view)

        channelOrEventNameTextView =
            findViewById<TextView?>(R.id.channel_or_event_name_text_view).apply {
                typeface = fontRegular
                setTextColor(colorMainText)
            }
        bottomGradientBackground = findViewById(R.id.bottom_gradient_background)
        contentContainerLinearLayout = findViewById(R.id.content_container_linear_layout)
        channelOrEventCardView =
            findViewById<MaterialCardView?>(R.id.custom_button_card_view).apply {
                strokeWidth = Utils.getDimensInPixelSize(CARD_BORDER_WIDTH)
                setCardBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_not_selected")))
            }
        contentContainerConstraintLayout = findViewById(R.id.content_container_constraint_layout)
        iconsContainerLinearLayout = findViewById(R.id.icons_container_linear_layout)
        mainConstraintLayout = findViewById(R.id.main_constraint_layout)
        channelOrEventImageView = findViewById(R.id.channel_or_event_image_view)
        favoriteImageView = findViewById<ImageView?>(R.id.favorite_image_view)
            .apply { visibility = VISIBLE }
        channelIndexTextView = findViewById<TextView?>(R.id.channel_index_text_view).apply {
            setTextColor(colorMainText) // todo CHANGE "colorMainText" to "silver"
            setTextColor(Color.parseColor("#bdc1c6")) // todo REMOVE THIS LINE when previous one is applied
            typeface = fontRegular
        }
        channelTypeTextView = findViewById<TextView?>(R.id.channel_type_text_view).apply {
            setTextColor(colorMainText) // todo CHANGE "colorMainText" to "silver"
            setTextColor(Color.parseColor("#bdc1c6")) // todo REMOVE THIS LINE when previous one is applied
            typeface = fontRegular
        }
        skipImageView = findViewById(R.id.skip_image_view)

        radioImageView = findViewById(R.id.radio_image_view)
        timeTextView = findViewById<TimeTextView?>(R.id.time_text_view).apply {
            setTextColor(descriptionText)
        }

        this.setOnFocusChangeListener { view, hasFocus ->
            updateMarqueeEffect(hasFocus)
            if (hasFocus) {
                speakCardText()
                channelOrEventCardView.strokeColor =
                    colorSelector
                shouldKeepFocus = false
                startUpdateTimer(view!!)
            } else {
                stopUpdateTimer() // when Card doesn't have focus reset timer (it was started when Card gained focus) to avoid animating the Card
                if (!shouldKeepFocus) {
                    channelOrEventCardView.strokeColor = Color.TRANSPARENT
                    CoroutineHelper.runCoroutine(
                        context = Dispatchers.Main, // if this Coroutine is removed animation won't work as expected when clicking DPAD_UP or DPAD_DOWN on buttons in ChannelListScene and ChannelListSearchScene
                        coroutineFun = {
                            if (isAnimated) {
                                Utils.unFocusAnimation(view!!)
                                isAnimated = false
                            }
                        }
                    )
                } else {
                    channelOrEventCardView.strokeColor =
                        Color.parseColor("#4de8f0fe") // TODO read color using OEM
                }
            }
            onFocusChanged.invoke(hasFocus)
        }

        this.setOnClickListener {
            if (!isAnimationInProgress) {
                isAnimationInProgress = true
                viewClickAnimation(it, object : AnimationListener {
                    override fun onAnimationEnd() {
                        isAnimationInProgress = false
                        onClick.invoke()
                    }
                })
            }
        }

        this.setOnLongClickListener {
            if (isLongPressEnabled && !isAnimationInProgress) {
                isAnimationInProgress = true
                viewClickAnimation(it, object : AnimationListener {
                    override fun onAnimationEnd() {
                        isAnimationInProgress = false
                        onLongClick.invoke()
                    }
                })
            }
            return@setOnLongClickListener true
        }
    }

    fun updateWatchlistIcon(isInWatchlist: Boolean) {
        CoroutineHelper.runCoroutine(context = Dispatchers.Main, coroutineFun = {
            watchlistImageView.visibility = if (isInWatchlist) VISIBLE else GONE // this is necessary because in DetailsScene when Event is removed from Watchlist that happens on thread different than thread different from Main
        })
    }

    fun updateRecordingIcon(isInRecList: Boolean) {
        CoroutineHelper.runCoroutine(context = Dispatchers.Main, coroutineFun = {
            recordingImageView.visibility = if (isInRecList) VISIBLE else GONE // this is necessary because in DetailsScene when Event is removed from Recording list that happens on thread different than thread different from Main
        })
    }

    /**
     * Method used to disable long click on CustomCard.
     *
     * By default, long click is enabled - call this method on CustomCard object if it shouldn't be.
     */
    fun disableLongClick() {
        isLongPressEnabled = false
    }

    /**
     * called when Card should be animated and have border even if not focused at the moment.
     */
    fun keepFocus() {
        this.shouldKeepFocus = true
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

    /**
     * used to set lambda function for OnLongClickListener.
     *
     * Passed lambda function will be executed every time when Card is long pressed.
     *
     * By default onLongClick variable, which is used to store lambda expression, is set to empty lambda - when Card is being long pressed it won't have any additional functionality beside automatic animation.
     *
     * Also, it is possible to disable Long Press functionality by calling disableLongClick() on Card's object. If that is case, passed lambda won't be executed.
     */
    fun setOnLongClick(onLongClick: () -> Unit) {
        this.onLongClick = onLongClick
    }

    fun doIfEventIsNotLockedOrNull(
        tvEvent: TvEvent?,
        isEventLocked: Boolean,
        onLocked: () -> Unit = {},
        onNotLockedOrNull: (TvEvent) -> Unit
    ) {
        if (tvEvent == null) {

        }
        else if (isEventLocked) {
            blindEyeImageView.visibility = View.VISIBLE
            Utils.loadImage(
                view = blindEyeImageView,
                resource = R.drawable.blind_eye_icon
            )
            lockedImageView.visibility = View.VISIBLE
            onLocked.invoke()
        }
        else { /*event is NOT locked*/
            blindEyeImageView.visibility = View.GONE
            onNotLockedOrNull.invoke(tvEvent)
        }
    }

    @Deprecated(
        "Do NOT handle focus change using method setOnFocusChangeListener(). Instead, method setOnFocusChange() on CustomEventCard's object should be used in the same way as setOnFocusChangeListener() was used before.",
        ReplaceWith("setOnFocusChange{ }")
    )
    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        super.setOnFocusChangeListener(l)
    }

    @Deprecated(
        "Do NOT handle on click using method setOnClickListener(). Instead, method setOnClick() on CustomEventCard's object should be used in the same way as setOnClickListener() was used before.",
        ReplaceWith("setOnClick{ }")
    )
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }

    @Deprecated(
        "Do NOT handle on long click using method setOnLongClickListener(). Instead, method setOnLongClick() on CustomEventCard's object should be used in the same way as setOnLongClickListener() was used before.",
        ReplaceWith("setOnLongClick{ }")
    )
    override fun setOnLongClickListener(l: OnLongClickListener?) {
        super.setOnLongClickListener(l)
    }

    protected fun insertImageInChannelOrEventImageView(
        imagePath: String?,
        onFailed: () -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        if (imagePath != null) {
            Utils.loadImage(
                path = imagePath,
                view = channelOrEventImageView,
                callback = object : AsyncReceiver {
                    override fun onFailed(error: Error?) {
                        bottomGradientBackground.visibility = VISIBLE
                        onFailed.invoke()
                    }

                    override fun onSuccess() {
                        channelOrEventNameTextView.visibility = GONE
                        bottomGradientBackground.visibility =
                            GONE // bottomGradientBackground will not be set to visible for all CustomCard's child classes - whenever image has been loaded bottomGradient should not be present.
                        onSuccess.invoke()
                    }
                }
            )
        } else {
            onFailed.invoke()
        }
    }

    /**
     * @param cardType used to enable having different size of the cards of type SearchChannel. CustomCardChannelList is used in ChannelList Widget and in Search Scene. Size of the cards used in those 2 places have to be different but all other functionalities are the same, so this property is used to change size of the card in resetData().
     */
    @SuppressLint("ViewConstructor")
    class CustomCardSearchChannel(context: Context, cardType: CardType) :
        CustomCard(context) {
        init {
            this.cardType = cardType
            setCardSize()
        }

        fun updateData(
            item: ChannelListItem,
            isParentalEnabled: Boolean,
            channelType: String
        ) {

            this.tvEvent = item.event

            resetData()
            insertImageInChannelOrEventImageView(
                imagePath = item.channel.logoImagePath,
                onFailed = {
                    channelOrEventNameTextView.visibility = VISIBLE
                }
            )

            handleLockedImageViewVisibility(
                isEventLocked = false,
                isChannelLocked = item.channel.isLocked,
                isParentalEnabled = isParentalEnabled
            )
            channelTypeTextView.apply {
                text = channelType
                visibility = VISIBLE
            }
            channelIndexTextView.apply {
                text = item.channel.getDisplayNumberText()
                visibility = INVISIBLE
            }
            channelOrEventNameTextView.apply {
                text = item.channel.name
            }

            lockedImageView.layoutParams.height = 28
            lockedImageView.setPadding(0,0,5,0)
            skipImageView.layoutParams.height = 25
            skipImageView.setPadding(0,0,5,0)
            radioImageView.layoutParams.height = 25
            radioImageView.setPadding(0,0,5,0)
            watchImageView.layoutParams.height = 25
            watchImageView.setPadding(0,0,5,0)
            skipImageView.visibility = if (item.channel.isSkipped) View.VISIBLE else View.GONE
            radioImageView.visibility = if (item.channel.isRadioChannel) View.VISIBLE else View.GONE
            watchImageView.visibility = if (item.isCurrentChannel) View.VISIBLE else View.GONE
        }

        override val textForSpeech: List<String>
            get() = listOf(
                tvEvent?.name ?: ConfigStringsManager.getStringById("no_information"),
                tvEvent?.shortDescription ?: ConfigStringsManager.getStringById("no_information")
            )
    }

    /**
     * @param cardType used to enable having different size of the cards of type SearchChannel. CustomCardChannelList is used in ChannelList Widget and in Search Scene. Size of the cards used in those 2 places have to be different but all other functionalities are the same, so this property is used to change size of the card in resetData().
     */
    @SuppressLint("ViewConstructor")
    class CustomCardBroadcastChannel(context: Context, cardType: CardType) : CustomCard(context) {
        init {
            this.cardType = cardType
            setCardSize()
            enableMarqueeEffect()

            setOnFocusChanged {
                updateMarqueeEffect(it)
            }
        }

        fun updateData(
            item: TvChannel,
            isParentalEnabled: Boolean,
            channelType: String
        ) {

            this.tvChannel = item

            resetData()
            insertImageInChannelOrEventImageView(
                imagePath = item.logoImagePath,
                onFailed = {
                    channelOrEventNameTextView.visibility = VISIBLE
                }
            )

            handleLockedImageViewVisibility(
                isEventLocked = false,
                isChannelLocked = item.isLocked,
                isParentalEnabled = isParentalEnabled
            )
            channelTypeTextView.apply {
                text = channelType
                visibility = VISIBLE
            }

            (timeTextView.layoutParams as LinearLayout.LayoutParams).apply {
                topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_5)
            }
            timeTextView.apply {
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    channelIndexTextView.context.resources.getDimensionPixelSize(R.dimen.font_10).toFloat()
                )
                text = item.getDisplayNumberText()
                visibility = View.VISIBLE
            }
            channelOrEventNameTextView.apply {
                visibility = VISIBLE
                text = item.name
            }
            lockedImageView.layoutParams.height = 28
            lockedImageView.setPadding(0,0,5,0)
            skipImageView.layoutParams.height = 25
            skipImageView.setPadding(0,0,5,0)
            radioImageView.layoutParams.height = 25
            radioImageView.setPadding(0,0,5,0)
            watchImageView.layoutParams.height = 25
            watchImageView.setPadding(0,0,5,0)
            skipImageView.visibility = if (item.isSkipped) View.VISIBLE else View.GONE
            radioImageView.visibility = if (item.isRadioChannel) View.VISIBLE else View.GONE
            watchImageView.visibility =  View.GONE
        }

        override val textForSpeech: List<String>
            get() = listOf(
                tvChannel?.name ?: ConfigStringsManager.getStringById("no_information")
            )
    }

    @Deprecated("This card is not used anymore. ReferenceWidgetFavourites is removed as feature, thus this card is not valid anymore.")
    class CustomCardFavourite(context: Context) : CustomCard(context) {
        init {
            cardType =
                CardType.FAVORITE // this is important in base CustomCard class when changing size of the Card depending on which type it is
            setCardSize()
        }

        fun updateData(item: TvChannel, selectedCategory: String) {
            resetData()
            channelTypeTextView.apply {
                text = ""
                visibility = VISIBLE
            }
            channelIndexTextView.apply {
                text = item.getDisplayNumberText()
                visibility = VISIBLE
            }
            channelOrEventNameTextView.apply {
                text = item.name
            }

            insertImageInChannelOrEventImageView(
                imagePath = item.logoImagePath,
                onFailed = {
                    channelOrEventNameTextView.visibility = VISIBLE
                }
            )
            // read values from item and after them when modifying views
            val isItemFavorite = item.favListIds.contains(selectedCategory)
            val isItemRadioChannel: Boolean = item.isRadioChannel

            favoriteImageView
                .apply {
                    setImageResource(
                        if (isItemFavorite) ICON_CUSTOM_CARD_FAVORITE_HEART_EMPTY
                        else ICON_CUSTOM_CARD_FAVORITE_HEART_FULL
                    )
                    visibility = VISIBLE
                }

            radioImageView.apply {
                if (isItemRadioChannel) {
                    visibility = VISIBLE
                }
            }
        }

        override val textForSpeech: List<String>
            get() = TODO() // Since CustomCardFavorite is deprecated this is not handled
    }


    class CustomCardInfoBanner(context: Context) : CustomCard(context) {

        companion object {
             val SPACE_BETWEEN_ITEMS = R.dimen.custom_dim_15
        }

        init {
            cardType = CardType.INFO_BANNER
            setCardSize()
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent,
            isParentalEnabled: Boolean,
            isCurrentChannel: Boolean?,
            currentTime: Long,
            isInWatchlist: Boolean?,
            isInRecList:Boolean,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            this.tvEvent = tvEvent
            resetData()

            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ){
                // TODO what to do with following lock when channel is locked? Will this be covered with isEventLocked also?
                handleLockedImageViewVisibility(
                    isEventLocked = false,
                    isChannelLocked = tvEvent.tvChannel.isLocked,
                    isParentalEnabled = isParentalEnabled
                )

                insertImageInChannelOrEventImageView(
                    imagePath = it.imagePath,
                    onFailed = {
                        timeTextView.visibility = VISIBLE
                        channelOrEventNameTextView.apply {
                            visibility = VISIBLE
                            if (!ReferenceApplication.isBlockedContent(tvEvent)) {
                                text = tvEvent.name
                            }
                        }
                    }
                )
            }

            timeTextView.setDateTimeFormat(dateTimeFormat)
            timeTextView.time = tvEvent

            updateWatchlistIcon(isInWatchlist = isInWatchlist == true)
            updateRecordingIcon(isInRecList = isInRecList)
            watchImageView.visibility =
                if ((tvEvent.startTime < currentTime && tvEvent.endTime > currentTime) && isCurrentChannel == true) View.VISIBLE else View.GONE

        }

        override val textForSpeech: List<String>
            get() = listOf(
                tvEvent?.name ?: ConfigStringsManager.getStringById("no_information"),
                tvEvent?.shortDescription ?: ConfigStringsManager.getStringById("no_information")
            )
    }

    class CustomCardDetailsScene : CustomCard {
        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

        init {
            cardType =
                CardType.DETAILS_SCENE // this is important in base CustomCard class when changing size of the Card depending on which type it is
            setCardSize()
            channelOrEventCardView.visibility = GONE // initially Card is set to be invisible in order to avoid blinking when DetailsScene is opened and image fails to load.
        }

        /**
         * method used to handle inserting image in ChannelOrEventImageView.
         *
         * If image is not loaded successfully or there is no image, that should be handled differently than in other cards and onFailed is important here.
         */
        private fun insertImage(
            imagePath: String?,
            onImageLoadingSucceed: () -> Unit = {},
            onImageLoadingFailed: () -> Unit = {}) {
            insertImageInChannelOrEventImageView(
                imagePath = imagePath,
                onSuccess = {
                    onImageLoadingSucceed.invoke()
                    channelOrEventCardView.visibility = VISIBLE
                },
                onFailed = {
                    // #1 - idea of onFailed is to hide all card's elements (except row with icons) and set card's background to transparent in order to have only icons row visible.
                    contentContainerLinearLayout.visibility = View.GONE
                    contentContainerConstraintLayout.layoutParams.height =
                        ViewGroup.LayoutParams.WRAP_CONTENT // set to wrap content because in xml it's height is set to MATCH_PARENT
                    channelOrEventCardView.layoutParams.height =
                        ViewGroup.LayoutParams.WRAP_CONTENT // set to wrap content because in xml it's height is set to fixed value
                    bottomGradientBackground.visibility = GONE
                    channelOrEventCardView.visibility = VISIBLE // even if image is not loaded it is important to set Card's visibility to visible - otherwise icons as Watch, Skip... won't be visible
                    channelOrEventCardView.setBackgroundColor(Color.TRANSPARENT)
                    iconsContainerLinearLayout.apply {
                        setPadding(
                            0, // without left padding set to 0 first icon wouldn't be aligned with icons bellow it.
                            paddingTop, // the same padding as it was
                            paddingRight, // the same padding as it was
                            paddingBottom // the same padding as it was
                        )
                    }
                    onImageLoadingFailed.invoke()
                }
            )
        }

        /**
         * used from DetailsScene when passing Recording as parameter
         */
        fun updateData(recording: Recording) {
            resetData()
            insertImage(recording.image)
        }

        /**
         * used from DetailsScene when passing Event as parameter
         */
        fun updateData(
            tvEvent: TvEvent,
            isCurrentChannel: Boolean,
            isParentalEnabled: Boolean,
            isInWatchlist: Boolean?,
            isInRecList: Boolean,
            currentTime: Long,
            onImageLoadingSucceed: () -> Unit,
            onImageLoadingFailed: () -> Unit
        ) {

            this.tvEvent = tvEvent

            resetData()
            handleLockedImageViewVisibility(
                isEventLocked = false,
                isChannelLocked = tvEvent.tvChannel.isLocked,
                isParentalEnabled = isParentalEnabled
            )
            updateWatchlistIcon(isInWatchlist = isInWatchlist == true)
            updateRecordingIcon(isInRecList = isInRecList)
            watchImageView.visibility =
                if ((tvEvent.startTime < currentTime && tvEvent.endTime > currentTime) && isCurrentChannel) View.VISIBLE else View.GONE
            radioImageView.visibility =
                if (tvEvent.tvChannel.isRadioChannel) View.VISIBLE else View.GONE
            bottomGradientBackground.visibility = GONE

            insertImage(
                imagePath = tvEvent.imagePath,
                onImageLoadingSucceed = onImageLoadingSucceed,
                onImageLoadingFailed = onImageLoadingFailed
            )
        }

        override val textForSpeech: List<String>
            get() = listOf(
                tvEvent?.name ?: ConfigStringsManager.getStringById("no_information"),
                tvEvent?.shortDescription ?: ConfigStringsManager.getStringById("no_information")
            )

    }

    class CustomCardForYou(context: Context) : CustomCard(context) {

        init {
            cardType = CardType.FOR_YOU
            setCardSize()
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent,
            isParentalEnabled: Boolean,
            isInWatchlist: Boolean,
            isInRecList: Boolean,
            isEventLocked: Boolean,
            dateTimeFormat: DateTimeFormat
        ) {
            resetData()
            timeTextView.setDateTimeFormat(dateTimeFormat)

            this.tvEvent = tvEvent

            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ){tvEvent ->
                insertImageInChannelOrEventImageView(
                    imagePath = tvEvent.imagePath,
                    onFailed = {
                        channelOrEventNameTextView.text =
                            if (tvEvent.name.isBlank().not()) tvEvent.name
                            else tvEvent.tvChannel.name
                        channelOrEventNameTextView.visibility = VISIBLE
                        timeTextView.time = tvEvent.tvChannel
                        timeTextView.visibility = VISIBLE
                    }
                )
                // TODO what to do with following lock when channel is locked? Will this be covered with isEventLocked also?
                handleLockedImageViewVisibility(
                    isEventLocked = false,
                    isChannelLocked = tvEvent.tvChannel.isLocked,
                    isParentalEnabled = isParentalEnabled
                )
            }

            updateWatchlistIcon(isInWatchlist)
            updateRecordingIcon(isInRecList)
            // initially Channel Name will be displayed, BUT it should be hidden if there is Channel logo which is successfully shown
            channelOrEventNameTextView.apply {
                if (!ReferenceApplication.isBlockedContent(tvEvent)) {
                    text = tvEvent.name
                }
            }

            radioImageView.apply {
                if (tvEvent.tvChannel.isRadioChannel) {
                    visibility = VISIBLE
                }
            }

            timeTextView.apply {
                time = tvEvent.tvChannel
            }
        }

        override val textForSpeech: List<String>
            get() = listOf(
                tvEvent?.name ?: ConfigStringsManager.getStringById("no_information"),
                tvEvent?.shortDescription ?: ConfigStringsManager.getStringById("no_information")
            )
    }

    class CustomCardRecording(context: Context) : CustomCard(context) {

        init {
            cardType = CardType.RECORDING
            setCardSize()
        }

        fun updateData(
            recording: Recording,
            isParentalEnabled: Boolean,
        ) {
            resetData()

            this.recording = recording

            lockedImageView.visibility =
                if (ReferenceApplication.isBlockedContent(recording.tvEvent!!)) View.VISIBLE else View.GONE

            insertImageInChannelOrEventImageView(
                imagePath = recording.image,
                onSuccess = {
                    durationTextView.visibility = GONE
                },
                onFailed = {
                    channelOrEventNameTextView.text =
                        if (recording.name.isBlank().not()) recording.name
                        else recording.tvEvent!!.name
                    channelOrEventNameTextView.visibility = VISIBLE
                    durationTextView.text = Recording.createRecordingTimeInfo(
                        recordingDate = recording.recordingStartTime,
                        recordingEndTime = recording.recordingEndTime
                    )
                    durationTextView.visibility = VISIBLE
                }
            )
            // TODO what to do with following lock when channel is locked? Will this be covered with isEventLocked also?
            handleLockedImageViewVisibility(
                isEventLocked = recording.isEventLocked,
                isChannelLocked = recording.tvChannel!!.isLocked,
                isParentalEnabled = isParentalEnabled
            )
        }

        override val textForSpeech: List<String>
            get() = listOf(
                recording?.name ?: ConfigStringsManager.getStringById("no_information")
            )
    }

    class CustomCardScheduledRecording(context: Context) : CustomCard(context) {

        init {
            cardType = CardType.SCHEDULED_RECORDING
            setCardSize()
        }

        fun updateData(
            scheduledRecording: ScheduledRecording,
        ) {

            this.scheduledRecording = scheduledRecording

            resetData()

            lockedImageView.visibility =
                if (ReferenceApplication.isBlockedContent(scheduledRecording.tvEvent!!)) View.VISIBLE else View.GONE

            insertImageInChannelOrEventImageView(
                imagePath = scheduledRecording.tvEvent!!.imagePath,
                onSuccess = {
                    durationTextView.visibility = GONE
                },
                onFailed = {
                    channelOrEventNameTextView.text =
                        if (scheduledRecording.name.isBlank().not()) scheduledRecording.name
                        else scheduledRecording.tvEvent!!.name
                    channelOrEventNameTextView.visibility = VISIBLE
                    durationTextView.text = Recording.createRecordingTimeInfo(
                        recordingDate = scheduledRecording.scheduledDateStart,
                        recordingEndTime = scheduledRecording.scheduledDateEnd
                    )
                    durationTextView.visibility = VISIBLE
                }
            )
        }

        override val textForSpeech: List<String>
            get() = listOf(
                scheduledRecording?.name ?: ConfigStringsManager.getStringById("no_information")
            )
    }
}

enum class CardType {
    FAVORITE,
    FOR_YOU,
    VOD,
    SEARCH_CHANNEL,
    SCHEDULED_RECORDING,
    RECORDING, // this one is used in ForYouEventsViewHolder and it is displayed in SearchScene or ForYouWidget if there is Recording rail available
    CHANNEL_LIST_SEARCH, // this one is used from ForYouEventsViewHolder and it is used in SearchScene in case that rail contains channels - main point of it is to set size of the card to be same as in InfoBanner and ForYou but to have card with all data needed to be shown for channels.
    INFO_BANNER,
    DETAILS_SCENE,
    BROADCAST_CHANNEL,
}