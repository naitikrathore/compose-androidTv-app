package com.iwedia.cltv.components.welcome_screen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.KeyEvent
import android.view.ViewGroup.LayoutParams
import androidx.appcompat.widget.AppCompatTextView
import com.iwedia.cltv.components.welcome_screen.CustomSpannableTextView.Type.FOCUSED_FROM_LEFT
import com.iwedia.cltv.components.welcome_screen.CustomSpannableTextView.Type.FOCUSED_FROM_RIGHT
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface

@SuppressLint("ViewConstructor")
class CustomSpannableTextView : AppCompatTextView {

    companion object {
        private const val INITIAL_POSITION = -1
    }

    private var type: Type
    private var contentText: String
    private val listOfCustomSpannableItems: List<CustomSpannableItem>
    private val indexOfFirstSelectableText: Int
    private val listener: Listener

    private var spannableString: SpannableString

    private var selectedItemIndex = INITIAL_POSITION

    private val selectedColor =
        ColorHelper.colorSelector
    private val notSelectedColor =
        ColorHelper.colorProgress

    constructor(
        context: Context,
        type: Type,
        contentText: String,
        listOfCustomSpannableItems: List<CustomSpannableItem>,
        indexOfFirstSelectableText: Int,
        listener: Listener
    ) : super(context) {
        this.type = type
        this.contentText = contentText
        this.listOfCustomSpannableItems = listOfCustomSpannableItems
        this.indexOfFirstSelectableText = indexOfFirstSelectableText
        this.listener = listener

        this.text = this.contentText
        this.spannableString = SpannableString(this.contentText)

        setupSpannableTexts() // this MUSTN'T be in the init {} - it'll execute before constructor block and fatal will occur

    }

    constructor(
        context: Context,
        type: Type,
        contentText: String,
        customSpannableItem: CustomSpannableItem,
        indexOfFirstSelectableText: Int,
        listener: Listener
    ) : this(
        context,
        type,
        contentText,
        listOf(customSpannableItem),
        indexOfFirstSelectableText,
        listener
    )

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        isFocusable = true // must be set here. Initially View is not focusable.
        isFocusedByDefault = false
        setTextColor(Color.WHITE)

        setupKeyHandling()
        setupFocusChanged()
        clearFocus()
    }

    private fun setupKeyHandling() {
        setOnKeyListener { _, keyCode, keyEvent ->
            when (keyEvent.action) {
                KeyEvent.ACTION_DOWN -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            listener.onUpPressed()
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            listener.onDownPressed()
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (selectedItemIndex == 0) {
                                listener.onLeftPressed()
                                if (type == FOCUSED_FROM_LEFT) resetInitialState()
                                return@setOnKeyListener true
                            }
                            selectedItemIndex--
                            setupSpannableTexts()
                            return@setOnKeyListener true// Return true to indicate that the event has been handled
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (selectedItemIndex == listOfCustomSpannableItems.count() - 1) {
                                listener.onRightPressed()
                                if (type == FOCUSED_FROM_RIGHT) resetInitialState()
                                return@setOnKeyListener true
                            }
                            selectedItemIndex++
                            setupSpannableTexts()
                            return@setOnKeyListener true
                        }
                    }
                    false
                }

                KeyEvent.ACTION_UP -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            listOfCustomSpannableItems[selectedItemIndex].onTextPressed.invoke()
                            return@setOnKeyListener true
                        }
                    }
                    false
                }

                else -> {
                    false
                }
            }
        }
    }

    private fun setupFocusChanged() {
        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (selectedItemIndex == -1) {
                    selectedItemIndex = indexOfFirstSelectableText
                }
                setupSpannableTexts()
            }
        }
    }

    fun resetInitialState() {
        selectedItemIndex = INITIAL_POSITION
        setupSpannableTexts()
    }


    private fun setupSpannableTexts() {
        listOfCustomSpannableItems.forEachIndexed { index, customSpannableItem ->
            val startIndex = contentText.indexOf(customSpannableItem.text)
            val endIndex = startIndex + customSpannableItem.text.length

            val colorSpan = ForegroundColorSpan(
                if (index == selectedItemIndex) {
                    selectedColor
                } else {
                    notSelectedColor
                }

            )

            // Apply the ClickableSpan to the respective ranges
            spannableString.setSpan(
                colorSpan,
                startIndex,
                endIndex,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                startIndex,
                endIndex,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (index == selectedItemIndex) {
                val underlineSpan = UnderlineSpan() // Create an UnderlineSpan
                spannableString.setSpan(
                    underlineSpan,
                    startIndex,
                    endIndex,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                listener.setSpeechText(customSpannableItem.text)

            } else {
                // If index is not selectedItemIndex, remove UnderlineSpan and BoldSpan
                val existingUnderlineSpans =
                    spannableString.getSpans(startIndex, endIndex, UnderlineSpan::class.java)
                existingUnderlineSpans.forEach {
                    spannableString.removeSpan(it)
                }
            }

            this.text = spannableString
        }
    }

    /**
     * Enum class representing the direction from which a view gained focus.
     * Used when handling DPAD_LEFT and DPAD_RIGHT navigation.
     *
     * [FOCUSED_FROM_LEFT] - When focus shifts from a view to the LEFT of [CustomSpannableTextView].
     * [FOCUSED_FROM_RIGHT] - When focus shifts from a view to the RIGHT of [CustomSpannableTextView].
     */
    enum class Type {
        FOCUSED_FROM_LEFT,
        FOCUSED_FROM_RIGHT
    }

    /**
     * Represents a custom spannable item with text content and an action to be performed when pressed.
     *
     * @property text The text content of the spannable item.
     * @property onTextPressed The action to be performed when the spannable item is pressed.
     */
    data class CustomSpannableItem(
        val text: String,
        val onTextPressed: () -> Unit
    )

    interface Listener: TTSSetterInterface {
        fun onUpPressed()
        fun onDownPressed()
        fun onLeftPressed()
        fun onRightPressed()
    }

}