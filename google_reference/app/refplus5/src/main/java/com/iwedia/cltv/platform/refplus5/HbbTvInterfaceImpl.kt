package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.media.tv.TvView
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.HbbTvInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.HbbTvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.dtv.tvinput.client.hbbtv.MtkHbbTvViewSession
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUTHORITY
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_HBBTV_BLOCK_TRACKING
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_HBBTV_DEVICE_ID
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_HBBTV_PRIVACY_POLICY_COOKIES_SETTINGS
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_HBBTV_PRIVACY_POLICY_DO_NOT_TRACK
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_HBBTV_PRIVACY_POLICY_PERSISTENT_STORAGE
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_HBBTV_RESET_DEVICE_ID
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_HBBTV_SUPPORT
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.DATATYPE_GENERAL
import com.mediatek.dtv.tvinput.framework.tifextapi.common.hbbtv.Constants.Companion.KEY_HBBTV_SCREEN_MODE_OUT_BOTTOM
import com.mediatek.dtv.tvinput.framework.tifextapi.common.hbbtv.Constants.Companion.KEY_HBBTV_SCREEN_MODE_OUT_LEFT
import com.mediatek.dtv.tvinput.framework.tifextapi.common.hbbtv.Constants.Companion.KEY_HBBTV_SCREEN_MODE_OUT_RIGHT
import com.mediatek.dtv.tvinput.framework.tifextapi.common.hbbtv.Constants.Companion.KEY_HBBTV_SCREEN_MODE_OUT_TOP
import com.mediatek.tv.hbbtvclientsession.HbbtvFvpSession
import com.mediatek.tv.hbbtvclientsession.HbbtvSessionBase


class HbbTvInterfaceImpl(val context: Context?, utilsInterface : UtilsInterfaceImpl) : HbbTvInterfaceBaseImpl(
    utilsInterface
) {

    private val mHbbtv = HbbtvSessionBase("HbbtvView")
    private var mHbbtvSurfaceView: SurfaceView? = null
    private var mHbbTvViewSession: MtkHbbTvViewSession? = null
    private var mTvView: TvView? = null
    private var SCREEN_WIDTH = 1920.0f
    private var SCREEN_HEIGHT = 1080.0f
    private val hbbtvFvpSession = HbbtvFvpSession()
    private var isOKDownPressed = false
    private val HBBTV_SUPPORT_TAG = "HbbTvSupport"
    private var wasHbbtvStopped = false
    private lateinit var mTvViewParent: ViewGroup
    private val KEY_HBBTV_LIVETV_SCREEN_TYPE: String = "HBBTV_LIVETV_SCREEN_TYPE"
    private val FOCUS_UI_TYPE_NON_FULL_SCREEN: Int = 0
    private val FOCUS_UI_TYPE_FULL_SCREEN: Int = 1
    private val APP_REQUEST_FOCUS: String = "APP_REQUEST_FOCUS"
    private val APP_RLS_FOCUS: String = "APP_RLS_FOCUS"

    val TAG = "HbbTvInterfaceImpl"

    override fun supportHbbTv(isEnabled: Boolean) {
        if (context != null) {
            SaveValue.saveTISSettingsIntValue(
                context,
                COLUMN_HBBTV_SUPPORT,
                AUTHORITY,
                DATATYPE_GENERAL,
                if (isEnabled) 1 else 0
            )
        }
    }

    override fun disableHbbTvTracking(isEnabled: Boolean) {
        if (context != null) {
            SaveValue.saveTISSettingsIntValue(
                context,
                COLUMN_HBBTV_PRIVACY_POLICY_DO_NOT_TRACK,
                AUTHORITY,
                DATATYPE_GENERAL,
                if (isEnabled) 1 else 0
            )
        }
    }

    override fun cookieSettingsHbbTv(value: HbbTvInterface.HbbTvCookieSettingsValue) {
        var index: Int
        if (HbbTvInterface.HbbTvCookieSettingsValue.BLOCK_3RD_PARTY == value) index = 0
        else index = 1
        if (context != null) {
            SaveValue.saveTISSettingsIntValue(
                context,
                COLUMN_HBBTV_PRIVACY_POLICY_COOKIES_SETTINGS,
                AUTHORITY,
                DATATYPE_GENERAL,
                index
            )
        }
    }

    override fun persistentStorageHbbTv(isEnabled: Boolean) {
        if (context != null) {
            SaveValue.saveTISSettingsIntValue(
                context,
                COLUMN_HBBTV_PRIVACY_POLICY_PERSISTENT_STORAGE,
                AUTHORITY,
                DATATYPE_GENERAL,
                if (isEnabled) 1 else 0
            )
        }
    }

    override fun blockTrackingSitesHbbTv(isEnabled: Boolean) {
        if (context != null) {
            SaveValue.saveTISSettingsIntValue(
                context,
                COLUMN_HBBTV_BLOCK_TRACKING,
                AUTHORITY,
                DATATYPE_GENERAL,
                if (isEnabled) 1 else 0
            )
        }
    }

    override fun deviceIdHbbTv(isEnabled: Boolean) {
        if (context != null) {
            SaveValue.saveTISSettingsIntValue(
                context,
                COLUMN_HBBTV_DEVICE_ID,
                AUTHORITY,
                DATATYPE_GENERAL,
                if (isEnabled) 1 else 0
            )
        }
    }

    override fun resetDeviceIdHbbTv() {
        if (context != null) {
            SaveValue.saveTISSettingsIntValue(
                context,
                COLUMN_HBBTV_RESET_DEVICE_ID,
                AUTHORITY,
                DATATYPE_GENERAL,
                (System.currentTimeMillis() / 1000).toInt()
            )
        }
    }

    init {
        var isHbbSupport: Boolean
        var defaultState = utilsInterface.getCountryPreferences(UtilsInterface.CountryPreference.ENABLE_HBBTV_BY_DEFAULT, true) as Boolean

        var isHbbSupportPreference = context?.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE)?.getBoolean(HBBTV_SUPPORT_TAG, defaultState)

        if(isHbbSupportPreference != null) {
            isHbbSupport = isHbbSupportPreference
        } else {
            isHbbSupport = defaultState
        }

        supportHbbTv(isHbbSupport)
        mHbbtv.init(context)
        mHbbTvViewSession = MtkHbbTvViewSession(context!!, "com.mediatek.dtv.tvinput.dvbtuner/.DvbTvInputService/HW0")
        hbbtvFvpSession.hbbtv_fvp_init(context)
    }

    private fun setTvViewPosition(left: Double, top: Double, right: Double, bottom: Double) {
        mTvViewParent.postDelayed(Runnable {
            mTvViewParent?.let {
                var width = (SCREEN_WIDTH * (right - left)).toInt()
                var height = (SCREEN_HEIGHT * (bottom - top)).toInt()
                var params = ViewGroup.MarginLayoutParams( width, height,)

                params.leftMargin = (left * SCREEN_WIDTH).toInt()
                params.topMargin = (top * SCREEN_HEIGHT).toInt()

                it.layoutParams = RelativeLayout.LayoutParams(params).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                }

            }
        }, 100)
    }

    private var mHbbtvViewSessionListener =
        object : MtkHbbTvViewSession.MtkHbbtvViewSessionListener {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onSetSurface(view: SurfaceControlViewHost.SurfacePackage) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSurface")
                mHbbtvSurfaceView?.setChildSurfacePackage(view)
                val visible = mHbbtvSurfaceView?.getVisibility() ?: View.GONE
                if (View.VISIBLE == visible) {
                    mHbbtvSurfaceView?.handler?.post {
                        mHbbtvSurfaceView?.visibility = View.GONE
                        mHbbtvSurfaceView?.visibility = visible
                    }
                }
            }

            override fun onVisibilityChange(visible: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onVisibilityChange $visible")
                mHbbtvSurfaceView?.handler?.post { mHbbtvSurfaceView?.setVisibility(visible) }
            }

            override fun onRegionChange(data: Bundle) {
                setTvViewPosition(
                    data.getDouble(KEY_HBBTV_SCREEN_MODE_OUT_LEFT),
                    data.getDouble(KEY_HBBTV_SCREEN_MODE_OUT_TOP),
                    data.getDouble(KEY_HBBTV_SCREEN_MODE_OUT_RIGHT),
                    data.getDouble(KEY_HBBTV_SCREEN_MODE_OUT_BOTTOM))
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setSurfaceView(hbbtvSurfaceView: SurfaceView, displayId: Int, sessionId: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSurfaceView")
        mHbbtvSurfaceView = hbbtvSurfaceView
        mHbbtvSurfaceView?.setZOrderMediaOverlay(true)
        mHbbTvViewSession?.setHbbTvViewSessionListener(mHbbtvViewSessionListener)
        if(mHbbtvSurfaceView!!.getHostToken()!= null) {
            var host = mHbbtvSurfaceView!!.getHostToken()
            if(host != null) {
                mHbbTvViewSession?.setSurfaceInfo(
                    displayId.toString(),
                    sessionId.toString(),
                    host,
                    SCREEN_WIDTH.toDouble(),
                    SCREEN_HEIGHT.toDouble()
                )
                mHbbtvSurfaceView?.handler?.post {
                    mHbbtvSurfaceView?.visibility = View.VISIBLE
                }
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Host token is null")
            }
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Host token is null")
        }
    }

    override fun setTvView(tvView: TvView, tvViewParent: ViewGroup, windowWidth : Float, windowHeight : Float) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setTvView width $windowWidth height $windowHeight")
        mTvView = tvView
        mTvViewParent = tvViewParent
        SCREEN_WIDTH = windowWidth
        SCREEN_HEIGHT = windowHeight
    }

    override fun checkSupportHbbtv(): Boolean{
        return hbbtvFvpSession.checkSupportHbbtv() // todo musika - mtk issue with mw this always return true
    }

    override fun sendKeyToHbbTvEngine(keyCode: Int, event: KeyEvent?, type: HbbTvInterface.HbbTvKeyEventType): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "sendKeyToHbbTvEngine ${event!!.getKeyCode()} height $event")
        //stop HBBTV when back key is long pressed
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN && event.isLongPress){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "long back press: disabling hbbtv on current channel")
            hbbtvFvpSession.hbbtv_fvp_stopApplication(-1)
            wasHbbtvStopped = true
            return true
        }
        //handle onKeyUp for back key just after long back is pressed, otherwise it will open broadcast tab.
        if(keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && wasHbbtvStopped){
            wasHbbtvStopped = false
            return true
        }
        if(type == HbbTvInterface.HbbTvKeyEventType.KEY_UP) {
            if(event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if(!isOKDownPressed) {
                    //Coz short press of OK does not create DOWN action, why ask MTK & Google
                    var fakeEvent = KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER)
                    hbbtvFvpSession.hbbtv_fvp_sendKeyDown(event!!.getKeyCode(), fakeEvent);
                }
                isOKDownPressed = false
            }
           return hbbtvFvpSession.hbbtv_fvp_sendKeyUp(event!!.getKeyCode(), event);
        } else {
            if(event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                isOKDownPressed = true
            }
            return hbbtvFvpSession.hbbtv_fvp_sendKeyDown(event!!.getKeyCode(), event);
        }
        return false
    }

    override fun setHbbTVFocus(focused: Boolean, fullScreen: Boolean) {
        if(mTvView!= null) {
            var bundle = Bundle()
            bundle.putInt(KEY_HBBTV_LIVETV_SCREEN_TYPE, if (fullScreen) FOCUS_UI_TYPE_FULL_SCREEN else FOCUS_UI_TYPE_NON_FULL_SCREEN)
            if (focused) {
                mTvView?.sendAppPrivateCommand(APP_RLS_FOCUS, bundle)
            } else if (!focused) {
                mTvView?.sendAppPrivateCommand(APP_REQUEST_FOCUS, bundle)
            }
        }
    }
}
