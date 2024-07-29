package com.iwedia.cltv.platform.t56

import android.content.Context
import android.media.tv.TvView
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceView
import com.iwedia.cltv.platform.`interface`.TTXInterface
import com.mediatek.twoworlds.tv.MtkTvKeyEvent
import com.mediatek.twoworlds.tv.MtkTvTeletext

class TTXInterfaceImpl : TTXInterface {

    enum class TTXState {
        TTX_AVAILABLE, TTX_UNAVAILABLE, TTX_INACTIVE, TTX_ACTIVE, NO_TTX, TTX_X_RATED, TTX_PAGE_SET, TTX_RECEIVE_DATA
    }

    private var iKey: MtkTvKeyEvent? = null
    private var iTeletext: MtkTvTeletext? = null
    private val TAG = "iTeletext"
    private val ttxSDKCallback = MtkTvCallback.getInstance()
    private var ttxState : TTXState = TTXState.TTX_INACTIVE

    /**
     * Init both classes MtkTvKeyEvent and MtkTvTeletext
     */
    fun init()
    {
        iKey = MtkTvKeyEvent.getInstance()
        iTeletext = MtkTvTeletext.getInstance()
        Log.i(TAG, "init: Teletext and KeyEvent init")
        ttxSDKCallback.registerTTXHandleInterface( object : MtkTvCallback.TtxTvHandleInterface {
            override fun ttxStateChanged(newState: TTXState) {
                ttxState = newState
            }
        })
    }

    override var decodeingPageLanguage = mutableListOf<String>()

    override fun startTTX(context: Context, liveTvView: TvView, ttxSurfaceView: SurfaceView, changeSize: Boolean) {
        Log.i(TAG, "startTTX: startTTX")
        if((ttxState == TTXState.TTX_INACTIVE) || (ttxState == TTXState.NO_TTX)) {
            Log.i(TAG, "startTTX: startTTX starting teletext")
            sendKeyToTtx(KeyEvent.KEYCODE_TV_TELETEXT,false)
        }
        if(ttxState == TTXState.TTX_ACTIVE) {
            Log.i(TAG, "startTTX: startTTX stopping teletext")
            sendKeyToTtx(KeyEvent.KEYCODE_BACK,false)
        }
        Log.i(TAG, "startTTX: Teletext Start")
    }
    override fun stopTTX() {
        Log.i(TAG, "startTTX: stopTTX doing nothing")
    }

    private fun sendKeyToTtx(keycode: Int, checkActive : Boolean) : Boolean {
        if (iKey == null)
        {
            init()
        }
        else
        {
            Log.i(TAG, "sendLinuxKey: init already done")
        }

        if(checkActive &&
            (ttxState != TTXState.TTX_ACTIVE)) {
            return false
        }

        var dfbkey: Int = iKey!!.androidKeyToDFBkey(keycode)
        Log.i(TAG, "sendLinuxKey: key $keycode converted to linux world key $dfbkey")
        if (iKey!!.sendKeyClick(dfbkey) == 0) {
            Log.i(TAG, "sendLinuxKey: sent key $dfbkey to linux world")
        }
        else
        {
            Log.i(TAG, "sendLinuxKey: Linux key send failed")
        }
        return true
    }

    override fun sendKeyToTtx(tvView: TvView, keycode: Int, keyEvent: KeyEvent): Boolean {
        return sendKeyToTtx(keycode,true)
    }

    override fun isTTXAvailable(): Boolean {
        return false
    }

    override fun addCallback(context: Context) {
    }

    override fun dispose() {
    }

    override fun isTTXActive(): Boolean {
        return false
    }

    override fun isTTXPreviouslyActive(): Boolean {
        return false
    }

    override fun updateTTXStatus(status: Boolean) {
    }

    override fun saveDigitalTTXLanguage(value: String) {
    }

    override fun saveDecodingPageLanguage(value: Int) {
    }

    override fun getDigitalTTXLanguage(): String {
        return ""
    }

    override fun getDecodingPageLanguage(): Int {
        return 0
    }
}
