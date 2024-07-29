package com.iwedia.cltv.components.custom_card

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.anoki_fast.epg.AnimationHelper
import com.iwedia.cltv.anoki_fast.epg.Rotation
import com.iwedia.cltv.anoki_fast.epg.rotate
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.scene.channel_list.ChannelListItem
import com.iwedia.cltv.utils.LoadingPlaceholder
import com.iwedia.cltv.utils.PlaceholderName
import com.iwedia.cltv.utils.Utils.Companion.animateBackgroundColor

/**
 * @author Boris Tirkajla
 */
class CustomCardChannelList : CustomCardBase, CustomCardChannelListViews {

    override val customCardMaterialCardView: MaterialCardView
        get() = findViewById(R.id.custom_card_material_card_view)
    override val titleTextView: TextView
        get() = findViewById(R.id.title_text_view)
    override val descriptionTextView: TextView
        get() = findViewById(R.id.description_text_view)
    override val channelIndexTextView: TextView
        get() = findViewById(R.id.channel_index_text_view)
    override val lockImageView: ImageView
        get() = findViewById(R.id.lock_image_view)
    override val watchImageView: ImageView
        get() = findViewById(R.id.watch_image_view)
    override val skipImageView: ImageView
        get() = findViewById(R.id.skip_image_view)
    override val channelTypeTextView: TextView
        get() = findViewById(R.id.channel_type_text_view)
    override val arrowImageView: ImageView
        get() = findViewById(R.id.arrow_image_view)
    override val radioImageView: ImageView
        get() = findViewById(R.id.radio_image_view)

    override val scrambledImageView: ImageView
        get() = findViewById(R.id.scrambled_image_view)

    private var isDpadRightAlreadyPressed = false
    private var keyListener: KeyListener? = null
    override fun initialisation() {
        // Inflating the custom layout for the channel list
        LayoutInflater.from(context).inflate(R.layout.custom_card_channel_list, this, true)

        // IMPORTANT: The following line must be placed AFTER the layout inflater.
        // Failure to do so may result in a NullPointerException, as the super method accesses views that are initialized during inflation.
        super.initialisation()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onLongClick() {
        keepFocus() // must go before super method
        arrowImageView.rotate(Rotation.TO_END)
        super.onLongClick()
    }

    override fun onFocusChanged(hasFocus: Boolean) {
        super.onFocusChanged(hasFocus)
        if (hasFocus) {
            AnimationHelper.fadeInAnimation(arrowImageView)
            arrowImageView.rotate(Rotation.TO_START)
        } else {
            if (!shouldKeepFocus) { // rotate arrow back only if shouldKeepFocus is not set to true (this is the case when user clicked second time DPAD_RIGHT and changed focus to the buttons below the CustomDetails - arrow should stay rotated and visible)
                AnimationHelper.fadeOutAnimation(arrowImageView, duration = 10)
                arrowImageView.rotate(Rotation.TO_START)
            }
            isDpadRightAlreadyPressed = false // reset flag
        }
    }

    override fun onDpadRightPressed(): Boolean {
        super.onDpadRightPressed()

        if (isDpadRightAlreadyPressed) {
            keepFocus()
            keyListener!!.onSecondDpadRightPressed()
        } else {
            if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.CHANNEL_LIST) == false) {
                arrowImageView.rotate(Rotation.TO_END)
                isDpadRightAlreadyPressed = true // indicates that user has pressed dpad_right
                keyListener!!.onFirstDpadRightPressed()
            }
        }
        return true // key was handled successfully
    }

    override fun onDpadLeftPressed(): Boolean {
        super.onDpadLeftPressed()
        if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.CHANNEL_LIST) == false) {
            if (isDpadRightAlreadyPressed) isDpadRightAlreadyPressed =
                false // this happens if the user has pressed once DPAD_RIGHT and than DPAD_LEFT - flag isDpadRightAlreadyPressed have to be reset in this case
            arrowImageView.rotate(Rotation.TO_START)
            keyListener!!.onDpadLeftPressed()
        }
        return true // key was handled successfully
    }

    override fun onDpadDownPressed(): Boolean {
        super.onDpadDownPressed()
        if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.CHANNEL_LIST) == false) {
            keyListener!!.onDpadDownPressed()
        }
        return false // important to return false in order for scroll in the adapter to work properly
    }

    override fun onDpadUpPressed(): Boolean {
        super.onDpadUpPressed()
        if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.CHANNEL_LIST) == false) {
            keyListener!!.onDpadUpPressed()
        }
        return false // important to return false in order for scroll in the adapter to work properly
    }

    override fun onChannelDownPressed(): Boolean {
        super.onChannelDownPressed()
        keyListener!!.onDpadChannelDownPressed()
        return true // key was handled successfully
    }

    override fun onChannelUpPressed(): Boolean {
        super.onChannelUpPressed()
        keyListener!!.onDpadChannelUpPressed()
        return true // key was handled successfully
    }

    override fun onMediaRecordPressed(): Boolean {
        return true // key was handled successfully
    }

    override fun onCaptionsPressed(): Boolean {
        super.onCaptionsPressed()
        keyListener!!.onCaptionsPressed()
        return true
    }

    override fun onBackPressed() : Boolean {
        keyListener!!.onBackPressed()
        super.onBackPressed()
        return true
    }

    /**
     * Updates the data of the Custom Card with information from the provided [TvEvent].
     *
     * @param isTvEventLocked Indicates whether the TvEvent is locked or restricted by parental control.
     *                        Crucial for setting parental control restricted texts and other related values.
     */
    fun updateData(
        item: ChannelListItem,
        isTvEventLocked: Boolean,
        isParentalEnabled: Boolean,
        channelType: String?,
        isScrambled: Boolean
    ) {
        // Set the TvEvent lock status for proper handling of parental control restricted texts and values.
        super.isTvEventLocked = isTvEventLocked
        super.updateWatchImageView(isChannelCurrent = item.isCurrentChannel && !BuildConfig.FLAVOR.contains("t56"))
        super.updateTitle(title = item.channel.name)
        super.updateDescription(description = item.event?.name)
        super.handleLockedImageViewVisibility(
            isChannelLocked = item.channel.isLocked,
            isParentalEnabled = isParentalEnabled
        )

        updateChannelIndex(index = item.channel.getDisplayNumberText())
        updateChannelType(channelType = channelType)
        updateSkipImageView(isSkipped = item.channel.isSkipped)
        updateRadioChannelImageView(isRadioChannel = item.channel.isRadioChannel)
        updateScrambledImageView(isCurrentChannelScrambled = isScrambled && item.isCurrentChannel)
    }

    /**
     * Retrieves the text content intended for Text-to-Speech (TTS) when this custom view is in focus.
     *
     * The purpose of this method is to provide the text that should be read aloud using Text-to-Speech
     * functionality when the associated custom view is focused. The text is typically associated
     * with the current state or content of the view, and it can be dynamically updated as needed.
     *
     * @return The text content for Text-to-Speech when this view is focused.
     */
    fun getText() = titleTextView.text.toString()

    fun updateCardToSelectedState() {
        customCardMaterialCardView.apply {
            elevation = cardElevation
            scaleX = cardScaleX.value
            scaleY = cardScaleY.value
            animateBackgroundColor(endColor = colorFocusedCardBackground)
            strokeColor = colorStrokeKeepFocused
        }

        arrowImageView.apply {
            visibility = View.VISIBLE
            rotation = 180f
        }

    }

    fun updateUnfocusedState() {
        customCardMaterialCardView.apply {
            elevation = cardElevationNormal
            scaleX = cardScaleXNormal.value
            scaleY = cardScaleYNormal.value
            animateBackgroundColor(endColor = colorUnfocusedCardBackground)
            strokeColor = colorStrokeUnfocused
        }

        arrowImageView.apply {
            visibility = View.INVISIBLE
        }
    }

    fun setKeyListener(listener: KeyListener) {
        this.keyListener = listener
    }

    private fun updateSkipImageView(isSkipped: Boolean) {
        skipImageView.updateVisibility(isSkipped)
    }

    private fun updateRadioChannelImageView(isRadioChannel: Boolean) {
        radioImageView.updateVisibility(isRadioChannel)
    }
    private fun updateScrambledImageView(isCurrentChannelScrambled: Boolean) {
        scrambledImageView.updateVisibility(isCurrentChannelScrambled)
    }

    private fun updateChannelIndex(index: String?) {
        val isVisible = !index.isNullOrBlank()
        channelIndexTextView.updateVisibility(isVisible)
        if (isVisible) {
            channelIndexTextView.text = index
        }
    }

    private fun updateChannelType(channelType: String?) {
        val isVisible = !channelType.isNullOrBlank()
        channelTypeTextView.updateVisibility(isVisible)
        if (isVisible) {
            channelTypeTextView.text = channelType
        }
    }

    interface KeyListener {
        fun onFirstDpadRightPressed()
        fun onSecondDpadRightPressed()
        fun onDpadLeftPressed()
        fun onDpadDownPressed()
        fun onDpadUpPressed()
        fun onDpadChannelUpPressed()
        fun onDpadChannelDownPressed()
        fun onCaptionsPressed()
        fun onBackPressed()
    }

}

interface CustomCardChannelListViews {
    // ImageView for arrow that will be animated when user presses DPAD_RIGHT and DPAD_LEFT while focus is on Card or the Details.
    val arrowImageView: ImageView

    // TextView for indicating the index of the channel
    val channelIndexTextView: TextView

    // TextView for indicating the channel type (at the moment of writing the possible options are Satellite, Cable, Antenna)
    val channelTypeTextView: TextView

    // ImageView for indicating if channel is radio channel
    val radioImageView: ImageView

    //ImageView for indicating if channel is scrambled
    val scrambledImageView : ImageView
}