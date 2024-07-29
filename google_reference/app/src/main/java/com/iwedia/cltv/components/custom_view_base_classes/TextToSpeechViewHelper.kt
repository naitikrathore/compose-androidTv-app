package com.iwedia.cltv.components.custom_view_base_classes

import android.util.Log
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.Type

private const val TAG = "TextToSpeechViewImpl"
/**
 * Implementation class for [TTSSetterInterface].
 *
 * This class provides a common structure for custom views that require integration with Text-to-Speech functionality.
 *
 * @author Boris Tirkajla
 */
class TextToSpeechViewHelper : TTSSetterInterface, TTSSetterForSelectableViewInterface {

    // Text-to-speech interface
    private var ttsSetterInterface: TTSSetterInterface? = null
    private var ttsSetterForSelectableViewInterface: TTSSetterForSelectableViewInterface? = null

    /**
     * Set up the [TTSSetterInterface] for this view.
     *
     * @param ttsSetterInterface The interface to set for handling speech text.
     */
    fun setupTextToSpeechTextSetterInterface(ttsSetterInterface: TTSSetterInterface) {
        this.ttsSetterInterface = ttsSetterInterface
    }

    /**
     * Set up the [TTSSetterForSelectableViewInterface] for this view.
     *
     * @param ttsSetterForSelectableViewInterface The interface to set for handling speech text.
     */
    fun setupTTSSetterForSelectableViewInterface(ttsSetterForSelectableViewInterface: TTSSetterForSelectableViewInterface) {
        this.ttsSetterForSelectableViewInterface = ttsSetterForSelectableViewInterface
    }

    /**
     * Implementation of [TTSSetterInterface]'s method to set speech text.
     *
     * @param text The text content to be spoken.
     * @param importance The importance level of the speech text.
     */
    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        try {
            ttsSetterInterface!!.setSpeechText(text = text, importance = importance)
        } catch (e: NullPointerException) {
            e.printStackTrace()
            // Handle the case when textToSpeechTextSetterInterface is not implemented.
            // You can use setupTextToSpeechTextSetterInterface() method from the place where this error occurred.
            Log.d(
                Constants.LogTag.CLTV_TAG +
                TAG,
                "Error: textToSpeechTextSetterInterface is not implemented. Make sure to call setupTextToSpeechTextSetterInterface() to set the interface.\""
            )
        }
    }

    override fun setSpeechTextForSelectableView(
        vararg text: String,
        importance: SpeechText.Importance,
        type: Type,
        isChecked: Boolean
    ) {
        try {
            ttsSetterForSelectableViewInterface!!.setSpeechTextForSelectableView(text = text, importance = importance, type = type, isChecked = isChecked)
        } catch (e: NullPointerException) {
            e.printStackTrace()
            // Handle the case when textToSpeechTextSetterInterface is not implemented.
            // You can use setupTextToSpeechTextSetterInterface() method from the place where this error occurred.
            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "Error: ttsSetterForSelectableViewInterface is not implemented. Make sure to call setupTextToSpeechTextSetterInterface() to set the interface.\""
            )
        }
    }

}














