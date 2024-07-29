package com.iwedia.cltv.platform.model.text_to_speech

import java.util.UUID

/**
 * Data class representing text content for Text-to-Speech (TTS) with an associated importance level.
 *
 * This class encapsulates the text to be spoken and its importance level in TTS scenarios.
 *
 * @param text The actual text content to be spoken.
 * @param importance The importance level of the speech text (default is [Importance.LOW]).
 *
 * Note: If the importance level is set to [Importance.HIGH], it indicates that this text should be
 * spoken with high priority, and any ongoing TTS should be interrupted to prioritize this speech text.
 */
data class SpeechText(
    val text: String,
    val importance: Importance = Importance.LOW,
    val id: UUID = UUID.randomUUID()
) {
    /**
     * Enumeration representing the importance levels of speech text.
     */
    enum class Importance {

        /**
         * The highest importance level for speech text. Toasts have absolute priority over other
         * text, ensuring they are spoken immediately and without interruption.
         */
        TOAST,

        /**
         * High importance level for speech text.
         */
        HIGH,

        /**
         * Low importance level for speech text (default).
         */
        LOW
    }
}

enum class Type {
    CHECK,
    SWITCH,
    FAVORITE
}
