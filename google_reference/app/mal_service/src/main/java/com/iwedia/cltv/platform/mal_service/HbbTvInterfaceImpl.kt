package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.media.tv.TvView
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncHbbTvUnusedKeyReplyListener
import com.cltv.mal.model.hbb_tv.HbbTvUnusedKeyReply
import com.iwedia.cltv.platform.`interface`.HbbTvInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel

class HbbTvInterfaceImpl(private val serviceImpl: IServiceAPI) : HbbTvInterface {
    private val TAG = "HbbTvInterfaceMalService"
    private var mTvView: TvView? = null
    private lateinit var mTvViewParent: ViewGroup
    private var mHbbtvSurfaceView: SurfaceView? = null
    private var SCREEN_WIDTH = 1920.0f
    private var SCREEN_HEIGHT = 1080.0f

    override fun initHbbTv(context: Context, tvChannel: TvChannel) {
    }

    override fun enableHbbTv() {
        serviceImpl.enableHbbTv()
    }

    override fun disableHbbTv() {
    }

    override fun onActivityStart() {
    }

    override fun sendKeyToHbbTvEngine(
        keyCode: Int,
        event: KeyEvent?,
        type: HbbTvInterface.HbbTvKeyEventType
    ): Boolean {
        return serviceImpl.sendKeyToHbbTvEngine(
            keyCode,
            event,
            com.cltv.mal.model.hbb_tv.HbbTvKeyEventType.values()[type.ordinal]
        )
    }

    override fun isHbbTvActive(): Boolean {
        return serviceImpl.isHbbTvActive
    }

    override fun supportHbbTv(isEnabled: Boolean) {
        serviceImpl.supportHbbTv(isEnabled)
    }

    override fun disableHbbTvTracking(isEnabled: Boolean) {
        serviceImpl.disableHbbTvTracking(isEnabled)
    }

    override fun cookieSettingsHbbTv(value: HbbTvInterface.HbbTvCookieSettingsValue) {
        serviceImpl.cookieSettingsHbbTv(com.cltv.mal.model.hbb_tv.HbbTvCookieSettingsValue.values()[value.ordinal])
    }

    override fun persistentStorageHbbTv(isEnabled: Boolean) {
        serviceImpl.persistentStorageHbbTv(isEnabled)
    }

    override fun blockTrackingSitesHbbTv(isEnabled: Boolean) {
        serviceImpl.blockTrackingSitesHbbTv(isEnabled)
    }

    override fun deviceIdHbbTv(isEnabled: Boolean) {
        serviceImpl.deviceIdHbbTv(isEnabled)
    }

    override fun resetDeviceIdHbbTv() {
        serviceImpl.resetDeviceIdHbbTv()
    }

    override fun registerHbbTvUnusedKeyReply(callback: IAsyncDataCallback<HbbTvInterface.HbbTvUnusedKeyReply>) {
        serviceImpl.registerHbbTvUnusedKeyReply(object :
            IAsyncHbbTvUnusedKeyReplyListener.Stub() {
            override fun onResponse(response: HbbTvUnusedKeyReply?) {
                callback.onReceive(
                    HbbTvInterface.HbbTvUnusedKeyReply(
                        response!!.keyCode,
                        response!!.upEvent,
                        response!!.downEvent
                    )
                )
            }
        })
    }

    override fun setSurfaceView(hbbtvSurfaceView: SurfaceView, displayId: Int, sessionId: Int) {
        mHbbtvSurfaceView = hbbtvSurfaceView
        mHbbtvSurfaceView?.setZOrderMediaOverlay(true)

        if(mHbbtvSurfaceView!!.getHostToken()!= null) {
            var host = mHbbtvSurfaceView!!.getHostToken()
            if(host != null) {
                serviceImpl.setHbbTvSurfaceView(host, displayId, sessionId)
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

    override fun setTvView(tvView: TvView, tvViewParent: ViewGroup, windowWidth: Float, windowHeight: Float) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setTvView width $windowWidth height $windowHeight")
        mTvView = tvView
        mTvViewParent = tvViewParent
        SCREEN_WIDTH = windowWidth
        SCREEN_HEIGHT = windowHeight
    }

    override fun notifyHbbTvChannelLockUnlock(status: Boolean) {
    }

    override fun updateParentalControl() {
    }

    override fun getHbbtvFunctionSwitch(): Boolean {
        return serviceImpl.hbbtvFunctionSwitch
    }

    override fun getHbbtvDoNotTrack(): Boolean {
        return serviceImpl.hbbtvDoNotTrack
    }

    override fun checkSupportHbbtv(): Boolean {
        return false
    }

    override fun setHbbTVFocus(focused: Boolean, fullScreen: Boolean) {
    }

    fun setTvViewPosition(left: Double, top: Double, right: Double, bottom: Double) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setTvViewPosition ")
        if (::mTvViewParent.isInitialized) {
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
    }

    fun onVisibilityChange(visible: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onVisibilityChange $visible")
        mHbbtvSurfaceView?.handler?.post { mHbbtvSurfaceView?.setVisibility(visible) }
    }

    fun onSetSurface(view: SurfaceControlViewHost.SurfacePackage) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSetSurface ")
        mHbbtvSurfaceView?.setChildSurfacePackage(view)
        val visible = mHbbtvSurfaceView?.getVisibility() ?: View.GONE
        if (View.VISIBLE == visible) {
            mHbbtvSurfaceView?.handler?.post {
                mHbbtvSurfaceView?.visibility = View.GONE
                mHbbtvSurfaceView?.visibility = visible
            }
        }
    }

}