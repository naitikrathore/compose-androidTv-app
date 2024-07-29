package com.iwedia.cltv.components.custom_view_base_classes

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Base class for custom [LinearLayout] with Text-to-Speech (TTS) support.
 *
 * This class provides a common structure for custom [LinearLayout] that require integration with Text-to-Speech functionality.
 *
 * @author Boris Tirkajla
 */
open class BaseLinearLayout : LinearLayout {

    var textToSpeechHandler: TextToSpeechViewHelper = TextToSpeechViewHelper()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(
        context,
        attrs,
        defStyleAttrs
    )

}