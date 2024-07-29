package com.iwedia.cltv

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class ReferenceApplication : Application() {

    init {
        instance = this
    }


    companion object {
        const val TAG = "ReferenceApplication: "
        var guideKeyPressed = false
        var infoKeyPressed = false
        var IS_CATCH_UP_SUPPORTED = false
        var IS_TIME_SHIFT_SUPPORTED = true
        var worldHandler: ReferenceWorldHandler? = null
        private var instance: ReferenceApplication? = null
        var shouldShowStartOverButton: Boolean = false
        var shouldShowAddToFavoritesButtonInChannelList: Boolean = true
        var channelScanFromSettings = false

        const val DEFAULT_CHANNEL_ENABLE = "Default channel enable"
        const val DEFAULT_CHANNEL = "Default channel"
        const val INTERACTION_CHANNEL = "Interaction channel"
        const val MHEG_PIN_PROTECTION = "Mheg pin protection"

        const val IS_SUBTITLES_ENABLED = "IsSubtitlesEnabled"
        const val IS_CAPTION_ENABLED = "IsCaptionsEnabled"
        const val TTX_DIGITAL_LANGUAGE = "Teletext Digital Language"
        const val TTX_DECODE_LANGUAGE = "Teletext Page Decode Language"

        const val FAST_SCAN_START = "FAST_SCAN_START"
        const val FAST_SCAN_RESULT = "FAST_SCAN_RESULT"

        const val FAST_EPG_RESULT = "FAST_EPG_RESULT"

        const val FAST_CHANNEL_REORDER = "FAST_CHANNEL_REORDER"

        const val HBBTV_SUPPORT = "HbbTvSupport"
        const val HBBTV_TRACK = "HbbTvTrack"
        const val HBBTV_COOKIE_SETTINGS_SELECTED = "HbbTvCookieSelected"
        const val HBBTV_PERSISTENT_STORAGE = "HbbTvPersistentStorage"
        const val HBBTV_BLOCK_TRACKING_SITES = "HbbTvBlockTrackingSites"
        const val HBBTV_DEVICE_ID = "HbbTvDeviceId"
        var isTestingOn = false

        var isSettingsOpened = false
        var isDialogueCreated = false
        var noChannelScene = false;
        var unlockedChannel: TvChannel? = null
        var isInputOpen = false
        var isInputPaused = false
        var isTVInputBlocked = false
        var isFactoryMode = false
        var isOkClickedOnSetUp = false
        var isCecTuneFromIntro = false
        var isBackFromInput = false
        var isTosFromInput = false
        var isHiddenFromSetings = false
        var isPrefClicked: Boolean? = false
        var isRegionSupported = true
        var isScanChannelsShowed = false
        var parentalControlDeepLink = false

        /**
         *  when back key pressed first ACTION_DOWN should perform then only back should work on
         *  Action_UP otherwise when we back press from settings and press back then our app will listen
         *  back press with Action_UP and perform required operation which causes issue.
         */
        var downActionBackKeyDone :Boolean = false


        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        fun runOnUiThread(runnable: Runnable) {
            instance?.activity?.runOnUiThread(runnable)
        }

        fun setActivity(activity: Activity) {
            instance!!.activity = activity
        }

        fun getActivity(): Activity {
            return instance!!.activity!!
        }

        fun getActivityInput(): Activity? {
            return instance?.activity
        }

        fun get(): ReferenceApplication {
            return instance!!
        }

        var isInForeground = false
        var isAppPaused = true
        var isInitalized = false
        var scanPerformed = false
        var moduleProvider: ModuleProvider?= null

        /**
         * Used to check if the rating of an event is more than or equal to 18+ and if content is allowed
         */
        fun isBlockedContent(tvEvent: TvEvent ): Boolean {
            if (tvEvent.parentalRating != null) {
                val list = tvEvent.parentalRating!!.split("/")
                var rating = if (list.size > 2) list[2].split("_").last().trim() else ""
                if (rating.isNotEmpty() && (rating.toDoubleOrNull() != null) && (rating.toInt() >= 18) /*&& ReferenceSdk.tvInputHandler!!.isParentalEnabled()*/) {
                    if (unlockedChannel != null) {
                        //TODO DEJAN
                        /*if (unlockedChannel == tvEvent.tvChannel){
                            return false
                        }*/
                        return true
                    }
                    return true
                }
            }
            return false
        }
    }


    /**
     * Activity context
     */
    var context: Context? = null

    /**
     * Application activity
     */
    var activity: Activity? = null
        set(value) {
            field = value
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "################ REFRENE APPLICATION ON CREATE")

        instance = this@ReferenceApplication

        //Assign context
        context = this@ReferenceApplication

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "#################### REFERENCE APPLICATION ENTERED")
    }
}