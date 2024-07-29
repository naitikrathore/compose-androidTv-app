package com.iwedia.cltv.platform.`interface`

import android.media.AudioManager
/**
 * Interface for managing Closed Caption settings and information.
 */
interface ClosedCaptionInterface {
    /**
     * Get the default Closed Caption values for the specified options.
     *
     * @param ccOptions The Closed Caption options.
     * @return The default Closed Caption value as an integer, or null if not found.
     */
    fun getDefaultCCValues(ccOptions: String): Int?

    /**
     * Save the user-selected Closed Caption options with a new value.
     *
     * @param ccOptions The Closed Caption options.
     * @param newValue The new value to be saved.
     */
    fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int, isOtherImput: Boolean = false)

    /**
     * Reset Closed Caption settings to their default values.
     */
    fun resetCC()

    /**
     * Set Closed Caption information.
     */
    fun setCCInfo()

    /**
     * Disable Closed Caption information.
     */
    fun disableCCInfo()

    /**
     * Set Closed Caption with mute control.
     *
     * @param isEnable True to enable Closed Caption with mute, false to disable.
     * @param audioManager The [AudioManager] instance for audio control.
     */
    fun setCCWithMute(isEnable: Boolean, audioManager: AudioManager)

    /**
     * Set Closed Caption with mute control information.
     */
    fun setCCWithMuteInfo()

    /**
     * Check if Closed Caption is enabled.
     *
     * @return True if Closed Caption is enabled, false otherwise.
     */
    fun isClosedCaptionEnabled(): Boolean

    /**
     * Set Closed Caption.
     *
     * @return The result code for the operation.
     */
    fun setClosedCaption(isOtherImput: Boolean = false): Int

    /**
     * Get the current Closed Caption text.
     *
     * @return The Closed Caption text as a string, or null if not available.
     */
    fun getClosedCaption(isOtherImput: Boolean = false): String?

    /**
     * Get the subtitles state.
     *
     * @return True if subtitles are enabled, false otherwise.
     */
    fun getSubtitlesState(): Boolean

    /**
     * Get the default mute values for Closed Caption.
     *
     * @return True if mute is enabled by default, false otherwise.
     */
    fun getDefaultMuteValues(): Boolean

    /**
     * Check if Closed Caption track is available.
     *
     * @return True if a Closed Caption track is available, false otherwise.
     */
    fun isCCTrackAvailable(): Boolean

    /**
     * Initialize the Closed Caption module.
     */
    fun initializeClosedCaption()

    /**
     * Dispose of the Closed Caption module.
     */
    fun disposeClosedCaption()

    /**
     * Applies the closed caption style whenever the video starts playing.
     * This function should be called when the video playback begins to ensure that
     * the closed captions are displayed with the appropriate style.
     *
     * Note: Should be called regardless of Closed Caption enable status
     */
    fun applyClosedCaptionStyle()

    /**
     * whever subtitle track will be updated, this method will update the selectedCCTrack variable
     * which is used to show the selected cc track
     */
    fun updateSelectedTrack()
}
