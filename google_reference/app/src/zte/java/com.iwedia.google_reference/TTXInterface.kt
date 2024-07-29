package com.iwedia.cltv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.tv.TvTrackInfo
import android.os.*
import android.media.tv.TvView
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import com.iwedia.cltv.sdk.ReferenceSdk.context

class TTXInterface {

    companion object {
        val TTX_SERVICE_PACKAGE = "com.google.android.tv.teletextservice"
        private val TTX_START_FULLPAGE =
            "com.google.android.tv.dtvinput.play.mode.FULLPAGE_TELETEXT"
        private val TTX_ALPHA = 100
        private var intentTTX = Intent()
        var mService: Messenger? = null
        var bound: Boolean = false
        var mTTXHandler = Handler()
        var context: Context? = null
        private var isTTXAvailable = 0
        private val TAG = "IWTTXService"

        val mConnection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                if (TvConfigurationHelper.isGretzkyBoard()) {
                    Log.i(TAG, "Service Connect")
                    bound = true
                    mService = Messenger(service)
                    sendTTXMsg()
                }
            }

            override fun onServiceDisconnected(className: ComponentName) {
                if (TvConfigurationHelper.isGretzkyBoard()) {
                    Log.i(TAG, "Service Disconnect")
                    mService = null
                    bound = false
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun startTTXHandler(context: Context, liveTvView: TvView) {
            this.context = context
            intentTTX.component = ComponentName(
                TTX_SERVICE_PACKAGE,
                "com.google.android.tv.teletextservice.TeletextService"
            )

            context!!.bindService(intentTTX, mConnection, Context.BIND_AUTO_CREATE)

            Log.i(TAG, "startTTXHandler: Entry")
            if (mTTXHandler != null) {
                mTTXHandler.removeCallbacksAndMessages(null)
            }
            mTTXHandler.postDelayed({
                isTTXAvailable = 0
                startTTXFullPage(liveTvView)
            }, 1000)
        }

        fun disposeTTX() {
            if (context != null) {
                context!!.unbindService(TTXInterface.mConnection)
            }

            bound = false
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun startTTXFullPage(liveTvView: TvView) {
            Log.i(TAG, "startTTXFullPage: Entry")
            val tracks: List<TvTrackInfo> =
                getTracks(liveTvView, TvTrackInfo.TYPE_SUBTITLE) as List<TvTrackInfo>
            if (tracks == null) {
                return
            }
            liveTvView!!.sendAppPrivateCommand(TTX_START_FULLPAGE, null)
            for (track in tracks) {
                val trackEncoding = track.encoding
                if (trackEncoding == "teletext-full-page") {
                    isTTXAvailable = 1
                    liveTvView!!.setCaptionEnabled(true)
                    break
                }
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

        @RequiresApi(Build.VERSION_CODES.R)
        fun checkTTX(liveTvView: TvView) {
            if (isTTXAvailable == 0) {
                Log.i(TAG, "checkTTX: loop")
                startTTXFullPage(liveTvView)
            }
        }
    }


    /**
     * Empty class for gretzky
     */
    fun init() {}

    fun startTTX() {}

    fun stopTTX() {}

    fun scrollTTXdown() {}

    fun scrollTTXup() {}

    fun sendLinuxKey(keycode: Int) {}
}