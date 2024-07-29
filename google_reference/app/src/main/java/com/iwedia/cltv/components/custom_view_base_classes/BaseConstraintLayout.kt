package com.iwedia.cltv.components.custom_view_base_classes

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Base class for custom [ConstraintLayout] with Text-to-Speech (TTS) support.
 *
 * This class provides a common structure for custom [ConstraintLayout] that require integration with Text-to-Speech functionality.
 *
 * @author Boris Tirkajla
 */
open class BaseConstraintLayout : ConstraintLayout {

    var textToSpeechHandler: TextToSpeechViewHelper = TextToSpeechViewHelper()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(
        context,
        attrs,
        defStyleAttrs
    )

}