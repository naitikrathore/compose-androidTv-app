package com.iwedia.cltv.platform.t56

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.KeyEvent
import com.iwedia.cltv.platform.base.HbbTvInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.HbbTvInterface.HbbTvCookieSettingsValue
import com.iwedia.cltv.platform.`interface`.HbbTvInterface.HbbTvKeyEventType
import com.iwedia.cltv.platform.`interface`.HbbTvInterface.HbbTvSettingsValue
import com.iwedia.cltv.platform.`interface`.HbbTvInterface.HbbTvUnusedKeyReply
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.MtkTvHBBTVBase
import com.mediatek.twoworlds.tv.MtkTvKeyEvent
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.wwtv.tvcenter.util.Commands
import com.mediatek.wwtv.tvcenter.util.DataSeparaterUtil
import com.mediatek.wwtv.tvcenter.util.KeyMap
import com.mediatek.wwtv.tvcenter.util.MtkLog

class HbbTvInterfaceImpl(val context: Context?, val timeInterface: TimeInterface, val utilsInterfaceImpl: UtilsInterfaceImpl) : HbbTvInterfaceBaseImpl(
    utilsInterfaceImpl
) {
    private val prefsTag = "OnlinePrefsHandler"
    private val HBBTV_SUPPORT = "HbbTvSupport"
    private val HBBTV_TRACK = "HbbTvTrack"
    private val HBBTV_COOKIE_SETTINGS_SELECTED = "HbbTvCookieSelected"
    private val HBBTV_PERSISTENT_STORAGE = "HbbTvPersistentStorage"
    private val HBBTV_BLOCK_TRACKING_SITES = "HbbTvBlockTrackingSites"
    private val HBBTV_DEVICE_ID = "HbbTvDeviceId"
    private val TAG = "HbbTvInterfaceImpl"
    private val hbbTvSDKCallback = MtkTvCallback.getInstance()

    private var mKey: MtkTvKeyEvent? = null
    private var mPassedAndroidKey = -1
    private var mPassedAndroidScanKey = -1
    private var mIsLongpressed = false
    private var isFromNative = false
    private var NATIVE_COMP_HBBTV = false
    private var HBBTV_STREAMING = false
    private var hbbtvBase: MtkTvHBBTVBase? = null
    private var unusedKeyCallback : IAsyncDataCallback<HbbTvUnusedKeyReply>? = null
    private var mKeyUpEvent: KeyEvent? = null
    private var mKeyDownEvent: KeyEvent? = null


    private fun getSharedPreferences() : SharedPreferences? {
        return context?.getSharedPreferences(prefsTag, Context.MODE_PRIVATE)
    }

    private fun setup() {
        //Initial setup
        val isHbbSupport = getSharedPreferences()!!.getBoolean(HBBTV_SUPPORT,false)
        if (isHbbSupport) {
            supportHbbTv(true)
        } else {
            supportHbbTv(false)
        }

        val isTrack = getSharedPreferences()!!.getBoolean(HBBTV_TRACK, false)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setup isTrack=$isTrack")
        if (isTrack) {
            disableHbbTvTracking(true)
        } else {
            disableHbbTvTracking(false)
        }

        val cookieSettingsSelected = getSharedPreferences()!!.getInt(HBBTV_COOKIE_SETTINGS_SELECTED, 0)
        cookieSettingsHbbTv(HbbTvCookieSettingsValue.fromInt(cookieSettingsSelected) )

        val isPersistentStorage = getSharedPreferences()!!.getBoolean(HBBTV_PERSISTENT_STORAGE, false)
        if (isPersistentStorage) {
            persistentStorageHbbTv(true)
        } else {
            persistentStorageHbbTv(false)
        }

        val isBlockTrackingSites = getSharedPreferences()!!.getBoolean(HBBTV_BLOCK_TRACKING_SITES, false)
        if (isBlockTrackingSites) {
            blockTrackingSitesHbbTv(true)
        } else {
            blockTrackingSitesHbbTv(false)
        }

        val isDeviceId = getSharedPreferences()!!.getBoolean(HBBTV_DEVICE_ID, false)
        if (isDeviceId) {
            deviceIdHbbTv(true)
        } else {
            deviceIdHbbTv(false)
        }
    }

    init {
        hbbTvSDKCallback.registerHbbTvHandleInterface(object : MtkTvCallback.HbbTvHandleInterface {
            override fun handleHbbTvMessage(type: Int?, message: Int?) {
                handleHbbTvMessages(type,message)
            }

            override fun notifyNoUsedKeyMessage(
                updateType: Int,
                argv1: Int,
                argv2: Int,
                argv3: Long
            ) {
                isFromNative = true
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "MSG_CB_NO_USED_KEY_MSG, param1=$updateType param2=$argv1")
                if (argv1 != 0) { // just handle keydown event//?????????????????????/
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "MSG_CB_NO_USED_KEY_MSG, data.param2=$argv1")
                    return
                }

                val keyCode: Int = mPassedAndroidKey
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "MSG_CB_NO_USED_KEY_MSG, keyCode:$keyCode")
                if (mKey!!.androidKeyToDFBkey(keyCode) !== updateType) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "key not map, data.param2=$updateType")
                    return
                }
                CoroutineHelper.runCoroutine ({
                    unusedKeyCallback?.onReceive(
                        HbbTvUnusedKeyReply(
                            keyCode,
                            mKeyUpEvent,
                            mKeyDownEvent
                        )
                    )
                })
            }
        })

        hbbtvBase = MtkTvHBBTVBase()
        setup()
        enableHbbTv()
        try {
            mKey = MtkTvKeyEvent.getInstance()
        } catch (ex: Exception) {
        }

    }

    private fun handleHbbTvMessages(type: Int?, message: Int?) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "handlerHbbtvMessage, type=$type, message=$message")
        when (type) {
            1, 3 -> {
                NATIVE_COMP_HBBTV = true
            }
            2, 4 -> {
                NATIVE_COMP_HBBTV = false
                HBBTV_STREAMING = false
            }
            5 -> {
                HBBTV_STREAMING = true
            }
            6 -> {
                HBBTV_STREAMING = false
            }
            10000 -> {
                NATIVE_COMP_HBBTV = false
                HBBTV_STREAMING = false
            }
            else -> {
            }
        }
    }

    override fun enableHbbTv() {
        val result = hbbtvBase!!.enableHbbtvByLiveTV()
        if (DataSeparaterUtil.getInstance() != null && DataSeparaterUtil.getInstance().isSupportHbbtv) {
            Commands.startCmd()
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "hbbtv not supported")
        }
    }

    private fun getScanCode(keyCode: Int, event: KeyEvent?):Int {
        if (keyCode == KeyMap.KEYCODE_BACK) {
            val value =
                if (event == null) -1 else event.getScanCode() + KeyMap.KEYCODE_SCANCODE_OFFSET
            if (value == KeyMap.getCustomKey(KeyMap.KEYCODE_MTKIR_EPG)) {
                return value
            } else if (value == KeyMap.getCustomKey(KeyMap.KEYCODE_MTKBT_EPG)) {
                return value
            }
        }
        return -1
    }

    private fun isKeyValid(keyCode: Int): Boolean {
        when (keyCode) {
            KeyMap.KEYCODE_MENU, KeyMap.KEYCODE_MTKIR_ANGLE ->             // case KeyMap.KEYCODE_MTKIR_MTSAUDIO:
                return false
            KeyMap.KEYCODE_MTKIR_REPEAT -> {
                if (isHbbTvActive()) {
                    return true
                }
                return false
            }
            KeyMap.KEYCODE_MTKIR_MUTE -> return false
            KeyMap.KEYCODE_MTKIR_PRECH, KeyMap.KEYCODE_MTKIR_CHDN, KeyMap.KEYCODE_MTKIR_CHUP -> {
                return false
            }
            KeyMap.KEYCODE_MTKIR_SUBTITLE, KeyMap.KEYCODE_MTKIR_MTKIR_CC, KeyMap.KEYCODE_MTKIR_MTSAUDIO -> if (isHbbTvActive() == true) {
                if (HBBTV_STREAMING) {
                    MtkLog.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        "hbbTvSettings.getStreamBoolean()"
                    )
                    return false
                }
            }
            KeyMap.KEYCODE_MTKIR_GUIDE -> {
                return true
            }
            KeyMap.KEYCODE_DPAD_CENTER -> {
                return true
            }
        }
        return true
    }

    override fun sendKeyToHbbTvEngine(keyCode: Int, event: KeyEvent?, type: HbbTvKeyEventType): Boolean {

        when(type) {
            HbbTvKeyEventType.KEY_UP ->
                mKeyUpEvent = event
            HbbTvKeyEventType.KEY_DOWN ->
                mKeyDownEvent = event
        }

        if(!isHbbTvActive()) {
            return false
        }

        if ((type == HbbTvKeyEventType.KEY_DOWN) &&
                (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK)) {
            return true
        }

        if ((type == HbbTvKeyEventType.KEY_UP) &&
            (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) && (keyCode != KeyEvent.KEYCODE_ENTER) && (keyCode != KeyEvent.KEYCODE_BACK)) {
            return false
        }

        if(isFromNative) {
            isFromNative = false
            return false
        }

        var dfbkeycode = -1

        synchronized(HbbTvInterfaceImpl::class.java) {
            //record
            mPassedAndroidKey = keyCode
            mPassedAndroidScanKey = getScanCode(mPassedAndroidKey, event)
            if (mPassedAndroidScanKey != -1) {
                mPassedAndroidKey = mPassedAndroidScanKey
            }
        }
        if (event == null) {
            dfbkeycode = mKey!!.androidKeyToDFBkey(mPassedAndroidKey)
            if (dfbkeycode != -1 && mKey!!.sendKeyClick(dfbkeycode) == 0) {
                return true
            }
        } else {
            if (isKeyValid(mPassedAndroidKey)) {
                dfbkeycode = mKey!!.androidKeyToDFBkey(mPassedAndroidKey, event)
                if (dfbkeycode == -1) {
                    return false
                }
                if (keyCode == KeyEvent.KEYCODE_SHIFT_LEFT ||
                    keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT
                ) {
                    if (event.action == KeyEvent.ACTION_UP) {
                        return mKey!!.sendKey(1, dfbkeycode) == 0
                    } else if (event.action == KeyEvent.ACTION_DOWN) {
                        return mKey!!.sendKey(0, dfbkeycode) == 0
                    }
                }
                if (event.repeatCount > 0) {
                    if (!mIsLongpressed) {
                        mIsLongpressed = true
                        MtkLog.v(
                            TAG,
                            "repeat, mIsLongpressed = $mIsLongpressed"
                        )
                        return mKey!!.sendKey(0, dfbkeycode) == 0
                    } else {
                        /* need not send key again*/
                        return true
                    }
                } else {
                    return mKey!!.sendKeyClick(dfbkeycode) == 0
                } // repeat
            } // isKeyValid
        } // event
        return false
    }

    override fun isHbbTvActive(): Boolean {
        return false
    }

    override fun supportHbbTv(isEnabled :Boolean) {
        val value = if(isEnabled) HbbTvSettingsValue.ON else HbbTvSettingsValue.OFF
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MENU_HBBTV, value.ordinal)
    }

    override fun disableHbbTvTracking(isEnabled :Boolean) {
        val value = if(isEnabled) HbbTvSettingsValue.ON else HbbTvSettingsValue.OFF
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MENU_DO_NOT_TRACK, value.ordinal)
    }

    override fun cookieSettingsHbbTv(value: HbbTvCookieSettingsValue) {
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MENU_ALLOW_3RD_COOKIES, value.ordinal)
    }

    override fun persistentStorageHbbTv(isEnabled :Boolean) {
        val value = if(isEnabled) HbbTvSettingsValue.ON else HbbTvSettingsValue.OFF
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MENU_PERSISTENT_STORAGE, value.ordinal)
    }

    override fun blockTrackingSitesHbbTv(isEnabled :Boolean) {
        val value = if(isEnabled) HbbTvSettingsValue.ON else HbbTvSettingsValue.OFF
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MENU_BLOCK_TRACKING_SITES, value.ordinal)
    }

    override fun deviceIdHbbTv(isEnabled :Boolean) {
        val value = if(isEnabled) HbbTvSettingsValue.ON else HbbTvSettingsValue.OFF
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_MENU_DEV_ID, value.ordinal)
    }

    override fun resetDeviceIdHbbTv() {
        MtkTvConfig.getInstance().setConfigValue(
            MtkTvConfigType.CFG_MENU_DHBBTV_DEV_ID_TIMESTAMP,
            (timeInterface.getCurrentTime() / 1000).toInt()
        )
    }

    override fun registerHbbTvUnusedKeyReply(callback: IAsyncDataCallback<HbbTvUnusedKeyReply>) {
        unusedKeyCallback = callback
    }
}