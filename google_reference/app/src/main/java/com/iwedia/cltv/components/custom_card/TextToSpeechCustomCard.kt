package com.iwedia.cltv.components.custom_card

import android.content.Context
import android.util.AttributeSet
import com.iwedia.cltv.components.custom_view_base_classes.BaseConstraintLayout

private const val TAG = "ITextToSpeechCustomCard"

/**
 * A custom card component that supports text-to-speech functionality.
 *
 * The `TextToSpeechCustomCard` class extends `BaseConstraintLayout` and provides an abstract
 * base for custom card components that require text-to-speech capabilities. Subclasses must
 * implement the `textForSpeech` property to provide the text that will be announced.
 *
 */
abstract class TextToSpeechCustomCard: BaseConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * The text to be spoken by the text-to-speech engine.
     *
     * Subclasses must override this property to provide the specific text that should be
     * announced when the `speakCardText` method is called.
     *
     * Example usage:
     * ```
     * class MyCustomCard(context: Context) : TextToSpeechCustomCard(context) {
     *     override val textForSpeech: String
     *         get() = "Hello, this is a custom card."
     * }
     * ```
     *
     */
    abstract val textForSpeech: List<String>

    /**
     * Announces the text associated with this card using text-to-speech.
     *
     * This method retrieves the text to be spoken from the [textForSpeech] property and
     * uses the [textToSpeechHandler] to convert it to audible speech. The speech output
     * can be customized by modifying the [textForSpeech] value in subclasses.
     *
     */
    fun speakCardText() {
        textToSpeechHandler.setSpeechText(*textForSpeech.toTypedArray())
    }

}