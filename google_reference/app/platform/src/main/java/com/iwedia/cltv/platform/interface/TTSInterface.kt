package com.iwedia.cltv.platform.`interface`

import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type

/**
 * Interface for interacting with a Text-to-Speech (TTS) engine.
 *
 * This interface defines methods for setting the text content to be spoken by a TTS engine.
 *
 * @author Boris Tirkajla
 */
interface TTSInterface : TTSSetterInterface, TTSStopperInterface, TTSSetterForSelectableViewInterface {
    fun isTTSEnabled(): Boolean
}

/** Interface for setting speech text content to be spoken by the Text-to-Speech (TTS) engine. */
interface TTSSetterInterface {
    /**
     * Sets speech text content to be spoken by the Text-to-Speech (TTS) engine.
     *
     * @param text The text content to be spoken.
     * @param importance The importance level of the speech text.
     */
    fun setSpeechText(vararg text: String, importance: SpeechText.Importance = SpeechText.Importance.LOW)
}

/**
 * Interface for setting speech text content to be spoken by the Text-to-Speech (TTS) engine
 * specifically for selectable views, such as checkboxes or radio buttons.
 */
interface TTSSetterForSelectableViewInterface {
    /**
     * Sets speech text content to be spoken by the Text-to-Speech (TTS) engine for a selectable view.
     *
     * This method allows setting speech text content with additional parameters specific to selectable views,
     * such as checkboxes or radio buttons.
     *
     * @param text The text content to be spoken.
     * @param importance The importance level of the speech text.
     * @param type The type of the selectable view.
     * @param isChecked A boolean indicating whether the selectable view is checked or not.
     */
    fun setSpeechTextForSelectableView(
        vararg text: String,
        importance: SpeechText.Importance = SpeechText.Importance.LOW,
        type: Type,
        isChecked: Boolean
    )
}

/** Interface for stopping any ongoing speech output. */
interface TTSStopperInterface {
    /** Stops any ongoing speech output. */
    fun stopSpeech()
}

/**
 * Interface for initiating Text-to-Speech (TTS) operations related to scene instructions.
 *
 * Implementations should concatenate the title and message of the scene, triggering TTS for
 * audio feedback. This method is commonly used to provide spoken instructions or information
 * when a new scene is created.
 */
interface TTSInstructionsInterface {
    /**
     * Initiates Text-to-Speech (TTS) for scene instructions.
     * This method concatenates the title and message of the scene, triggering TTS for audio feedback.
     */
    fun speakInstructions()
}

/**
 * Interface for initiating Text-to-Speech (TTS) operations when the focus on a view changes.
 *
 * Implementations should trigger TTS to speak the relevant text associated with the newly focused view.
 * This method is commonly used to provide audio feedback when the user navigates through different
 * interactive elements, ensuring that the user is informed about the currently focused view.
 */
interface TTSSpeakTextForFocusedViewInterface {
    /**
     * Initiates Text-to-Speech (TTS) for the text associated with the currently focused view.
     * This method triggers TTS to speak the relevant text associated with the newly focused view.
     */
    fun speakTextForFocusedView()
}