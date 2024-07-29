package com.iwedia.cltv.platform.mal_service.text_to_speech

import android.content.Context
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type

private const val TAG = "TTSInterfaceBaseImpl"

/**
 * Represents a pause in the speech output.
 * This constant is used to introduce pauses between spoken texts in Text-to-Speech (TTS) output.
 */
private const val PAUSE = "....   "

/**
 * Implementation of the [TTSInterface] that utilizes the Android Text-to-Speech engine.
 *
 * This class manages the Text-to-Speech (TTS) functionality, allowing the setting of text
 * to be spoken. It uses a [CountDownTimer] to introduce delays between consecutive speech
 * utterances, preventing rapid speech synthesis. Additionally, it handles the queuing of speech
 * texts based on their importance level, ensuring that higher-priority texts are spoken before
 * lower-priority ones.
 *
 * @param context The application context.
 * @author Boris Tirkajla
 */
class TTSInterfaceBaseImpl(private var context: Context) : TTSInterface {

    /** The delay (in milliseconds) before text-to-speech starts speaking. */
    private var delay = 1000L

    private val delimiter = "/"

    /** The currently queued speech text awaiting synthesis. */
    private var speechText: SpeechText? = null

    /** The list of speech texts queued for synthesis, organized by importance level. */
    private val waitingList: HashMap<String, SpeechText> = HashMap()

    private val textToSpeech =
        TextToSpeech(context) {}.apply {
            setSpeechRate(1f)
            //TODO specify Locale if needed here
            // setLanguage()
        }

    /** The CountDownTimer used to introduce delays between speech utterances. */
    private val timer =
        object : CountDownTimer(delay, delay) {
            override fun onTick(p0: Long) {}

            override fun onFinish() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFinish: ${speechText!!.text}")
                textToSpeech.speak(
                    speechText!!.text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    speechText!!.importance.name + delimiter + speechText!!.id
                )
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {

                    }

                    override fun onDone(utteranceId: String?) {

                        val (importance, id) = utteranceId!!.split(delimiter)

                        if (waitingList.containsKey(importance)) {
                            val item = waitingList[importance]?.takeIf {
                                it.id.toString() == id
                            }

                            item?.let {
                                waitingList.remove(importance)
                            }
                        }

                        updateSpeakingWord()
                    }

                    override fun onError(utteranceId: String?) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onError: $utteranceId")
                    }
                })
            }
        }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        if (!isTTSEnabled()) return

        var tmpText = ""

        text.forEachIndexed { index, it ->

            if (it.isBlank()) return@forEachIndexed // Ignore empty strings to prevent unnecessary pauses in speech synthesis when adding prefixes or suffixes.

            tmpText += it

            if (index != text.lastIndex) tmpText += PAUSE
        }
        waitingList[importance.name] = SpeechText(tmpText, importance)

        updateSpeakingWord()
    }


    override fun setSpeechTextForSelectableView(
        vararg text: String,
        importance: SpeechText.Importance,
        type: Type,
        isChecked: Boolean
    ) {
        if (!isTTSEnabled()) return

        var prefixText = ""
        var suffixText = ""

        when (type) {
            Type.CHECK -> {
                prefixText = getCheckPrefixSpeechText(isChecked)
            }
            Type.SWITCH -> {
                prefixText = getSwitchPrefixSpeechText(isChecked)
            }
            Type.FAVORITE -> {
                suffixText = getFavoriteSuffix(isChecked)
            }
        }

        setSpeechText(
            prefixText, *text, suffixText, // text
            importance = importance
        )
    }

    /** Stops any ongoing speech synthesis and clears the speech text queue. */
    override fun stopSpeech() {
        waitingList.clear()
        timer.cancel()
    }

    override fun isTTSEnabled(): Boolean {
        val am: AccessibilityManager =
            (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?)!!
        return am.isEnabled
    }

    /**
     * Stops the TextToSpeech engine and releases associated resources.
     * This method should be called when the TextToSpeech functionality is no longer needed.
     */
    fun destroy() { // TODO TTS - this method should be called when talkback is turned to off
        textToSpeech.apply {
            stop()
            shutdown() // Releases the resources used by the TextToSpeech engine.
        }
    }

    /** Initiates the speech synthesis process for the currently queued speech text. */
    private fun speakText() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateSpeakingWord: ___speakText___ ")
        textToSpeech.stop()
        timer.cancel()
        timer.start()
    }

    private fun getFavoriteSuffix(isFavorite: Boolean) =
        if (isFavorite) {
            "$PAUSE Press OK to remove channel from the favorite list."
        } else {
            "$PAUSE Press OK to add channel to the favorite list."
        }

    /**
     * Constructs a prefix for speech text indicating switch status.
     *
     * @param isSelected True if the switch is enabled, false otherwise.
     * @return A prefix indicating switch status.
     */
    private fun getSwitchPrefixSpeechText(isSelected: Boolean): String {
        return if (isSelected) {
            "enabled$PAUSE"
        } else {
            "disabled$PAUSE"
        }
    }

    /**
     * Constructs a prefix for speech text indicating selection status.
     *
     * @param isSelected True if the text is selected, false otherwise.
     * @return A prefix indicating selection status.
     */
    private fun getCheckPrefixSpeechText(isSelected: Boolean): String {
        return if (isSelected) {
            "selected"
        } else {
            "unselected"
        }
    }

    /**
     * Updates the currently speaking word based on priority levels.
     * This method ensures that the highest priority speech text is spoken first.
     */
    private fun updateSpeakingWord() {
        val priorities = listOf(
            SpeechText.Importance.TOAST,
            SpeechText.Importance.HIGH,
            SpeechText.Importance.LOW
        )

        for (priority in priorities) {
            if (waitingList.containsKey(priority.name)) {
                val text = waitingList[priority.name]
                if (text!!.id != speechText?.id) {
                    speechText = text
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateSpeakingWord: ___${priority.name}___ $waitingList")
                    speakText()
                }
                return
            }
        }

        // No speech texts left to speak
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateSpeakingWord: no words left.")
    }
}