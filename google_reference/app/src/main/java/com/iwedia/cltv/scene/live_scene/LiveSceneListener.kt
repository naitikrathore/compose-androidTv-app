package com.iwedia.cltv.scene.live_scene

import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.scene.ReferenceSceneListener

/**
 * Live scene listener
 *
 * @author Dejan Nadj
 */
interface LiveSceneListener : ReferenceSceneListener, TTSSetterInterface,
    ToastInterface, TTSSetterForSelectableViewInterface {
    /**
     * Channel up
     */
    fun channelUp()

    /**
     * Channel down
     */
    fun channelDown()


    /**
     * Last active channel
     */
    fun lastActiveChannel()

    /**
     * Show channel list
     */
    fun showChannelList()

    /**
     * Show home
     */
fun showHome(showHomeType: ShowHomeType = ShowHomeType.SET_FOCUS_TO_HOME_IN_TOP_MENU)
    enum class ShowHomeType{
        SET_FOCUS_TO_HOME_IN_TOP_MENU, // focus will be set to the Home Tab Item
        SET_FOCUS_TO_LIVE_OR_BROADCAST_IN_TOP_MENU // focus will be set to Broadcast or Live depending on the currently played channel
    }

    /**
     * Show info banner
     */
    fun showInfoBanner()

    /**
     * Show exit dialog
     */
    fun exitApplication()

    /**
     * Show no ethernet toast
     */
    fun showNoEthernetToast()

    /**
     * Digit pressed
     */
    fun digitPressed(digit: Int)

    /**
     * Show player
     */
    fun showPlayer(keyCode: Int)

    /**
     * Show pvr banner
     */
    fun showPvrBanner()

    fun handlePvrButton(): Boolean

    /**
     * Show Recording Type Dialog
     */
    fun showRecordingTypeDialog()

    /**Unlock button pressed*/
    fun onUnlockPressed()

    /**Check correct pin*/
    fun checkPin(pin: String, callback: IAsyncCallback)

    fun unlockChannel(callback: IAsyncCallback)

    /** returns is channel unlocked*/
    fun isChannelUnlocked():Boolean

    /**Request active channel*/
    fun requestActiveChannel(callback: IAsyncDataCallback<TvChannel>)

    /**Mute playback if locked on parental is active*/
    fun mutePlayback(isMuted: Boolean)

    /**Check if parental blocking is active*/
    fun isParentalBlockingActive(): Boolean

    /**Request active event*/
    fun requestActiveEvent(channel: TvChannel, callback: IAsyncDataCallback<TvEvent>)

    /** Destroy all other visible scene except live scene*/
    fun destroyOtherScenes()

    /** show zap banner */
    fun showZapBanner()

    /** Is recording in progress */
    fun isRecordingInProgress(): Boolean

    fun showStopRecordingDialog(channelChange: String?)

    /** Start recording by channel */
    fun startRecording(tvEvent: TvEvent)

    fun isScrambled(): Boolean

    fun isQuietTune(): Boolean

    /**
     * setIsOnLockScreen
     */
    fun setIsOnLockScreen(isOnLockScreen:Boolean)

    fun getIsOnLockScreen() : Boolean

    fun periodPressed()

    /* Is BlueMute enabled */
    fun isBlueMuteEnabled(): Boolean

    /* Is BlueMute active */
    fun isBlueMuteActive(): Boolean
    
    fun showPreferences()

    fun showTvPreferences()


    fun getEasChannel()
    fun isTuneToDetailsChannel(): Boolean
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): CharSequence?

    fun setCCInfo()

    fun launchInputSelectedScene()

    fun getCurrentTime(tvChannel: TvChannel): Long
    fun isAccessibilityEnabled(): Boolean

    fun isParentalControlEnabled() : Boolean
    fun getApplicationMode(): Int

    fun getDefaultInputValue(): String
    fun isTimeShiftInProgress(): Boolean
    fun getConfigInfo(nameOfInfo: String): Boolean
    fun resumePlayer()
    fun isNetworkAvailable() : Boolean
    fun isTTXActive() : Boolean
    fun notifyHbbTvChannelLockUnlock(status: Boolean)
    fun checkSupportHbbtv(): Boolean
    fun isSignalAvailable(): Boolean
    fun getPlatformName(): String
    fun setTTMLVisibility(isVisible: Boolean)
}