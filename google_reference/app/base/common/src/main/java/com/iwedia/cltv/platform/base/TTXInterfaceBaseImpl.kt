package com.iwedia.cltv.platform.base

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.tv.TvTrackInfo
import android.media.tv.TvView
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceView
import com.iwedia.cltv.platform.`interface`.TTXInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface

open class TTXInterfaceBaseImpl(private val utilsInterface: UtilsInterface) : TTXInterface {

    val TTX_SERVICE_PACKAGE = "com.google.android.tv.teletextservice"
    private val TTX_START_FULLPAGE =
        "com.google.android.tv.dtvinput.play.mode.FULLPAGE_TELETEXT"
    private val TTX_ALPHA = 100
    private var intentTTX = Intent()
    var mService: Messenger? = null
    var bound: Boolean = false
    var mTTXHandler : Handler? = null
    private var context: Context? = null
    private var isTTXAvailable = 0
    private val TAG = "IWTTXService"

    override var decodeingPageLanguage = mutableListOf<String>()

   private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.i(TAG, "Service Connect")
            bound = true
            mService = Messenger(service)
            sendTTXMsg()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.i(TAG, "Service Disconnect")
            mService = null
            bound = false
        }
    }

    private fun getTracks(liveTvView: TvView, type: Int): List<TvTrackInfo?>? {
        return liveTvView!!.getTracks(type)
    }

    private fun sendTTXMsg() {
        Log.i(TAG, "sendTTXMsg: Entry")
        if (!bound) {
            return
        }

        var msg: Message = Message.obtain(null, 1, TTX_ALPHA, isTTXAvailable)

        try {
            mService!!.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun startTTXFullPage(liveTvView: TvView) {
        Log.i(TAG, "startTTXFullPage: Entry")
        liveTvView!!.sendAppPrivateCommand(TTX_START_FULLPAGE, null)
    }

    private fun checkTeletextAvailability(liveTvView: TvView){
        val tracks: List<TvTrackInfo> =
            getTracks(liveTvView, TvTrackInfo.TYPE_SUBTITLE) as List<TvTrackInfo>
        if (tracks == null) {
            isTTXAvailable = 0
            return
        }
        for (track in tracks) {
            val trackEncoding = track.encoding
            if (trackEncoding == "teletext-full-page") {
                isTTXAvailable = 1
                liveTvView!!.setCaptionEnabled(true)
                return
            }
        }
        isTTXAvailable = 0
        startTTXFullPage(liveTvView)
    }

    override fun startTTX(context: Context, liveTvView: TvView, ttxSurfaceView: SurfaceView, changeSize: Boolean) {
        this.context = context

        checkTeletextAvailability(liveTvView!!)

        intentTTX.component = ComponentName(
            TTX_SERVICE_PACKAGE,
            "com.google.android.tv.teletextservice.TeletextService"
        )

        intentTTX.putExtra("Surface", ttxSurfaceView.holder.surface)

        context!!.bindService(intentTTX, mConnection, Context.BIND_AUTO_CREATE)

        Log.i(TAG, "startTTXHandler: Entry")

        if(mTTXHandler == null) {
            mTTXHandler = Handler()
        }

        mTTXHandler?.postDelayed({
            checkTeletextAvailability(liveTvView)
            startTTXFullPage(liveTvView)
        }, 1000)
    }

    override fun stopTTX() {
        if(bound) {
            if (mTTXHandler != null) {
                mTTXHandler?.removeCallbacksAndMessages(null)
            }

            if (context != null && bound) {
                context!!.unbindService(mConnection)
            }
        }

        bound = false
    }

    override fun sendKeyToTtx(tvView: TvView, keycode: Int, keyEvent: KeyEvent): Boolean {
        return false
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

    override fun updateTTXStatus(status: Boolean) {}


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