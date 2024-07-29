/**
 * Interface for interacting with HbbTV features on a TV platform.
 * HbbTV (Hybrid Broadcast Broadband TV) is a standard that combines broadcast and broadband delivery
 * for a more interactive TV experience.
 */
package com.iwedia.cltv.platform.`interface`

import android.content.Context
import android.media.tv.TvView
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.ViewGroup
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel


/**
 * Defines the possible values for enabling or disabling HbbTV.
 */
interface HbbTvInterface {
    /**
     * Enum representing the HbbTV settings value.
     */

    enum class HbbTvSettingsValue {
        OFF, // HbbTV is turned off
        ON   // HbbTV is turned on
    }

    /**
     * Enum representing the HbbTV settings value with a default option.
     */
    enum class HbbTvSettingsWithDefaultValue {
        OFF,      // HbbTV is turned off
        ON,       // HbbTV is turned on
        DEFAULT   // Use the default HbbTV setting
    }

    /**
     * Enum representing HbbTV cookie settings with associated integer values.
     */
    enum class HbbTvCookieSettingsValue(val value: Int) {
        BLOCK_ALL(0),         // Block all HbbTV cookies
        BLOCK_3RD_PARTY(1),   // Block 3rd-party HbbTV cookies
        DEFAULT(2);           // Use the default HbbTV cookie settings

        companion object {
            /**
             * Converts an integer value to its corresponding [HbbTvCookieSettingsValue].
             * @param value The integer value to convert.
             * @return The corresponding [HbbTvCookieSettingsValue].
             */
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

    /**
     * Enum representing the types of key events for HbbTV interaction.
     */
    enum class HbbTvKeyEventType {
        KEY_DOWN,  // Key down event
        KEY_UP     // Key up event
    }

    /**
     * Data class representing an unused key reply for HbbTV.
     * @property keyCode The key code for the unused key.
     * @property upEvent The key event for key release (may be null).
     * @property downEvent The key event for key press (may be null).
     */
    class HbbTvUnusedKeyReply(val keyCode : Int, val upEvent:KeyEvent?, val downEvent:KeyEvent?)

    /**
     * Initialization of HbbTV functionality.
     */
    fun initHbbTv(context: Context, tvChannel: TvChannel)

    /**
     * Enables HbbTV functionality.
     */
    fun enableHbbTv()

    /**
     * Disables HbbTV functionality.
     */
    fun disableHbbTv()

    /**
     * To enable HbbTV functionality when activity starts
     */
    fun onActivityStart()

    /**
     * Sends a key event to the HbbTV engine for interaction.
     * @param keyCode The key code for the event.
     * @param event The key event (may be null).
     * @param type The type of key event (KEY_DOWN or KEY_UP).
     * @return `true` if the key event was successfully sent, `false` otherwise.
     */
    fun sendKeyToHbbTvEngine(keyCode: Int, event: KeyEvent?, type: HbbTvKeyEventType): Boolean

    /**
     * Checks if HbbTV is currently active.
     * @return `true` if HbbTV is active, `false` otherwise.
     */
    fun isHbbTvActive(): Boolean

    /**
     * Enables or disables HbbTV support.
     * @param isEnabled `true` to enable HbbTV support, `false` to disable it.
     */
    fun supportHbbTv(isEnabled :Boolean)

    /**
     * Disables HbbTV tracking.
     * @param isEnabled `true` to disable HbbTV tracking, `false` to enable it.
     */
    fun disableHbbTvTracking(isEnabled :Boolean)

    /**
     * Sets the cookie settings for HbbTV.
     * @param value The desired [HbbTvCookieSettingsValue].
     */
    fun cookieSettingsHbbTv(value: HbbTvCookieSettingsValue)

    /**
     * Enables or disables persistent storage for HbbTV.
     * @param isEnabled `true` to enable persistent storage, `false` to disable it.
     */
    fun persistentStorageHbbTv(isEnabled :Boolean)

    /**
     * Blocks or unblocks tracking sites for HbbTV.
     * @param isEnabled `true` to block tracking sites, `false` to unblock them.
     */
    fun blockTrackingSitesHbbTv(isEnabled :Boolean)

    /**
     * Enables or disables device ID retrieval for HbbTV.
     * @param isEnabled `true` to enable device ID retrieval, `false` to disable it.
     */
    fun deviceIdHbbTv(isEnabled :Boolean)

    /**
     * Resets the device ID for HbbTV.
     */
    fun resetDeviceIdHbbTv()

    /**
     * Registers a callback to receive unused key replies for HbbTV.
     * @param callback The callback to register for receiving unused key replies.
     */
    fun registerHbbTvUnusedKeyReply(callback: IAsyncDataCallback<HbbTvUnusedKeyReply>)

    fun setSurfaceView(hbbtvSurfaceView: SurfaceView, displayId: Int, sessionId: Int)

    fun setTvView(tvView: TvView, tvViewParent: ViewGroup, windowWidth : Float, windowHeight : Float)

    /**
     * To notify MW that channel is locked/unlocked or parental locked/unlocked
     */
    fun notifyHbbTvChannelLockUnlock(status: Boolean)

    /**
     * To notify MW when parental setting is changed
     */
    fun updateParentalControl()

    /**
     * To get HbbTv switch status in preferences
     */
    fun getHbbtvFunctionSwitch(): Boolean

    /**
     * To get HbbTv Track status in preferences
     */
    fun getHbbtvDoNotTrack(): Boolean

    /**
     * Check support of HBB on channel
     */
    fun checkSupportHbbtv(): Boolean

    fun setHbbTVFocus(focused : Boolean, fullScreen : Boolean)

}