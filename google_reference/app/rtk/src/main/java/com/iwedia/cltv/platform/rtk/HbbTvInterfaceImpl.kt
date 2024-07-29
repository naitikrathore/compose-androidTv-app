package com.iwedia.cltv.platform.rtk

import android.app.Instrumentation
import android.content.Context
import android.media.tv.TvView
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.HbbTvInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.HbbTvInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.TvChannel
import com.realtek.system.RtkConfigs
import com.realtek.system.RtkProjectConfigs
import com.realtek.tv.RtkSettingConstants
import com.realtek.tv.hbbtv.HbbtvManagerBase
import kotlinx.coroutines.Dispatchers
import java.lang.reflect.InvocationTargetException

class HbbTvInterfaceImpl(val context: Context?, private val utilsInterface: UtilsInterfaceImpl) : HbbTvInterfaceBaseImpl(
    utilsInterface
) {
    private val TAG = javaClass.simpleName
    private var currentInputId: String? = null
    private var currentTvChannel: TvChannel? = null
    private var liveTvView: TvView? = null
    private var hbbtvSurfaceView: SurfaceView? = null
    private var hbbtvManager: HbbtvManagerBase? = null

    private val HBBTV_FUNCTION_SWITCH = "hbbtv_function_switch"
    private val HBBTV_FUNCTION_SWITCH_OFF = 0
    private val HBBTV_FUNCTION_SWITCH_ON = 1
    private val HBBTV_SUPPORT = "hbbtv_support"
    private val HBBTV_DO_NOT_TRACK = "hbbtv_do_not_track"
    private val PREF_PATH_GLOBAL_SHARED_CURRENT_INPUT = "global_shared_current_input"
    private val PREF_KEY_CURRENT_INPUT = "current_input"
    private val PREF_KEY_CURRENT_INPUT_TYPE = "current_input_type"
    private val EXCUTE_APP_CMD_IS_STREAMING = "is_streaming"
    private val SERVICE_NAME_DTV = "DTVTvInputService"
    private var hbbtvManagerListener: Any? = null

    init {
        try {
            hbbtvManagerListener = object : HbbtvManagerBase.OnListener {
                override fun onTuneToPendingChannel() {}
                override fun setInputConnectionProxyFactory() {}

            }
        } catch (ex: ClassNotFoundException) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Hbbtv not supported, ex --> ${ex.message}")
        } catch (ex: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Hbbtv not supported, ex --> ${ex.message}")
        }
    }

    override fun setSurfaceView(hbbtvSurfaceView: SurfaceView, displayId: Int, sessionId: Int) {
        this.hbbtvSurfaceView = hbbtvSurfaceView
        hbbtvSurfaceView?.setZOrderMediaOverlay(true)
    }

    override fun setTvView(
        tvView: TvView,
        tvViewParent: ViewGroup,
        windowWidth: Float,
        windowHeight: Float
    ) {
        this.liveTvView = tvView
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun initHbbTv(pContext: Context, tvChannel: TvChannel) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "initHbbTv: ")
        this.currentTvChannel = tvChannel
        val isHbbtvEnableConfig: Boolean = RtkProjectConfigs.getInstance().isSupportHbbtv
        val tvSupportHbbtv = utilsInterface.getTvSetting().isHbbtvSupported

        if (isHbbtvEnableConfig && tvSupportHbbtv && liveTvView != null) {
            try {
                if (hbbtvManager == null) {
                    val clazz = Class.forName("com.realtek.tv.hbbtv.hbbtvmanager.HbbtvManager")
                    val cons = clazz.getConstructor(
                        Context::class.java,
                        HbbtvManagerBase.OnListener::class.java, Any::class.java, Any::class.java
                    )

                    hbbtvManager = cons.newInstance(
                        pContext, hbbtvManagerListener as HbbtvManagerBase.OnListener,
                        utilsInterface.getTvSetting(), hbbtvSurfaceView
                    ) as HbbtvManagerBase

                    if (hbbtvManager?.checkIfEmpty() == true) {
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "hbbtvManager is empty")
                        hbbtvManager = null
                    } else {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "hbbtvManager is not empty")
                        enableHbbTv()
                    }
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "initHbbTv: HbbTvManager already created")
                }
            } catch (e: ClassNotFoundException) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "there is no HbbtvManager", e)
                hbbtvManager = null
            } catch (e: NoSuchMethodException) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "there is no HbbtvManager", e)
                hbbtvManager = null
            } catch (e: InstantiationException) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "there is no HbbtvManager", e)
                hbbtvManager = null
            } catch (e: IllegalAccessException) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "there is no HbbtvManager", e)
                hbbtvManager = null
            } catch (e: InvocationTargetException) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "there is no HbbtvManager", e)
                e.targetException
                hbbtvManager = null
            }
        }
    }

    override fun enableHbbTv() {
        try {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableHbbTv: hbbtvManager = $hbbtvManager")
            if (hbbtvManager != null) {
                hbbtvManager?.changeSource()
                setCurrentInputIdAndType(currentTvChannel)
                hbbtvManager?.onChannelChanged()
                hbbtvManager?.addInViewGroup(liveTvView?.parent!! as ViewGroup)
                hbbtvManager?.onStart()
                hbbtvManager?.onTune()
                hbbtvManager?.enableHbbtv()
            }
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableHbbTv: Exception --> ${ex.message}")
        }
    }

    override fun disableHbbTv() {
        if (hbbtvManager != null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "disableHbbTv: ")
            hbbtvManager?.disableHbbtv()
            hbbtvManager?.onStop()
        }
    }

    override fun onActivityStart() {
        //This method is required to enable HbbTv while resuming from background. In this case hbbtvManager is already initialized.
        //When starting CLTV for first time, this method will not have any effect as hbbtvManager will not be initialized so soon.
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onActivityStart: ")
        if (hbbtvManager != null) {
            enableHbbTv()
        }
    }

    override fun sendKeyToHbbTvEngine(
        keyCode: Int,
        event: KeyEvent?,
        type: HbbTvInterface.HbbTvKeyEventType
    ): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (hbbtvManager == null || hbbtvManager?.isApplicationStarted == false) {
                simulateKey(event!!.action, KeyEvent.KEYCODE_HOME)
                return true
            }
        }
        if (type == HbbTvInterface.HbbTvKeyEventType.KEY_UP) {
            if (event?.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                //Because RC_OK click does not create ACTION_DOWN but, RTK needs ACTION_DOWN
                var fakeEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)
                return (hbbtvManager?.dispatchKeyEvent(fakeEvent) == true)
            }
        }
        return (hbbtvManager?.dispatchKeyEvent(event) == true)
    }

    override fun supportHbbTv(isEnabled :Boolean) {
        try {
            if (isEnabled) {
                Settings.Global.putInt(
                    context?.contentResolver,
                    HBBTV_FUNCTION_SWITCH, HBBTV_FUNCTION_SWITCH_ON
                )
                enableHbbTv()
                updateConfigurations()
            } else {
                Settings.Global.putInt(
                    context?.contentResolver,
                    HBBTV_FUNCTION_SWITCH, HBBTV_FUNCTION_SWITCH_OFF
                )
                disableHbbTv()
            }

            val isSuspended = !isEnabled
            hbbtvManager?.setHbbtvSuspended(isSuspended)
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "supportHbbTv: Exception --> ${ex.message}")
        }
    }

    override fun disableHbbTvTracking(isEnabled: Boolean) {
        try {
            if (isEnabled) {
                Settings.Global.putInt(
                    context?.contentResolver,
                    HBBTV_DO_NOT_TRACK, 1
                )

                updateConfigurations()
                hbbtvManager?.updateDoNotTrackPref()
            } else {
                Settings.Global.putInt(
                    context?.contentResolver,
                    HBBTV_DO_NOT_TRACK, 0
                )
            }
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "disableHbbTvTracking: Exception --> ${ex.message}")
        }
    }

    override fun getHbbtvFunctionSwitch(): Boolean {
        var switchStatus = 0
        try {
            switchStatus = Settings.Global.getInt(
                context?.contentResolver,
                HBBTV_FUNCTION_SWITCH, 1
            )
        } catch (ex: Exception) {}

        return (switchStatus > 0)
    }

    override fun getHbbtvDoNotTrack(): Boolean {
        var switchStatus = 0
        try {
            switchStatus =  Settings.Global.getInt(
                context?.contentResolver,
                HBBTV_DO_NOT_TRACK, 0
            )
        } catch (ex: Exception) {}

        return (switchStatus > 0)
    }
    override fun isHbbTvActive(): Boolean {
        var isHbbtvAppRunning = false

        if ((hbbtvManager != null) && (hbbtvManager?.isApplicationStarted == true)) {
            isHbbtvAppRunning = true
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isHbbtvAppRunning = $isHbbtvAppRunning")
        return isHbbtvAppRunning
    }

    override fun notifyHbbTvChannelLockUnlock(status: Boolean) {
        if (hbbtvManager != null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "notifyHbbTvChannelLockUnlock = $status")
            if (status) {
                hbbtvManager?.notifyChannelLocked(true)
            } else {
                hbbtvManager?.notifyChannelLocked(false)
                hbbtvManager?.unblockContent()
            }
        }
    }

    override fun updateParentalControl() {
        if (hbbtvManager != null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateParentalControl")
            hbbtvManager?.updateParentalControl()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun isHbbtvStreaming(): Boolean {
        return if (isHbbTvActive()) {
            utilsInterface.getTvSetting().hbbtvExcuteAppCmd(EXCUTE_APP_CMD_IS_STREAMING)
        } else false
    }

    fun onContentAllowed() {
        if (hbbtvManager != null) {
            hbbtvManager?.onContentAllowed()
        }
    }

    fun onContentBlocked() {
        if (hbbtvManager != null) {
            hbbtvManager?.onContentBlocked()
        }
    }

    fun updateConfigurations() {
        if (hbbtvManager != null) {
            hbbtvManager?.updateOiifConfiguration()
            hbbtvManager?.updateMediaComponents()
        }
    }

    private fun setCurrentInputIdAndType(tvChannel: TvChannel?) {
        this.currentInputId = tvChannel?.inputId

        CoroutineHelper.runCoroutine({
            val preferences = context!!.getSharedPreferences(
                PREF_PATH_GLOBAL_SHARED_CURRENT_INPUT,
                Context.MODE_PRIVATE
            )
            preferences.edit().putString(PREF_KEY_CURRENT_INPUT, tvChannel?.inputId).apply()
            preferences.edit().putInt(PREF_KEY_CURRENT_INPUT_TYPE, tvChannel?.tunerType!!.ordinal).apply()

            Settings.Global.putString(
                context.contentResolver,
                RtkSettingConstants.TV_CURRENT_INPUT_ID,
                currentInputId
            )
        }, Dispatchers.Default)
    }

    private fun canRunHbbtvApp(currentInputId: String?): Boolean {
        return isDVB() && isDtv(currentInputId)
    }

    private fun isDVB(): Boolean {
        return RtkConfigs.TvConfigs.TV_SYSTEM == RtkConfigs.TvConfigs.TvSystemConstants.DVB
    }

    private fun isDtv(inputId: String?): Boolean {
        return (!inputId.isNullOrEmpty() && inputId.contains(SERVICE_NAME_DTV))
    }

    private fun simulateKey(keyAction: Int, keyCode: Int) {
        CoroutineHelper.runCoroutine({
            val inst = Instrumentation()
            inst.sendKeySync(KeyEvent(keyAction, keyCode))
        }, Dispatchers.Default)
    }
}