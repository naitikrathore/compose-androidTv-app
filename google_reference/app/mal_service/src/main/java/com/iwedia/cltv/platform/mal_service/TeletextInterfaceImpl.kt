package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.media.tv.TvTrackInfo
import android.media.tv.TvView
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceView
import android.widget.LinearLayout
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.entities.ServiceTvView
import com.iwedia.cltv.platform.`interface`.TTXInterface

class TeletextInterfaceImpl(private val serviceImpl: IServiceAPI) : TTXInterface {
    private val TAG = "TeletextInterfaceImpl"
    private val TTX_START_FULLPAGE =
        "com.google.android.tv.dtvinput.play.mode.FULLPAGE_TELETEXT"

    override var decodeingPageLanguage = mutableListOf(
        serviceImpl.getStringValue("WEST_EUR"),
        serviceImpl.getStringValue("EAST_EUR"),
        serviceImpl.getStringValue("RUSSIA"),
        serviceImpl.getStringValue("RUSSIA_2"),
        serviceImpl.getStringValue("GREEK"),
        serviceImpl.getStringValue("Turkey"),
        serviceImpl.getStringValue("Arab_Hbrw"),
        serviceImpl.getStringValue("Farsian"),
        serviceImpl.getStringValue("Arab"),
        serviceImpl.getStringValue("BYELORUSSIAN")
    )

    private fun startTTXFullPage(liveTvView: TvView) {
        Log.i(TAG, "startTTXFullPage: Entry")
        liveTvView!!.sendAppPrivateCommand(TTX_START_FULLPAGE, null)
    }

    private fun getTracks(liveTvView: TvView, type: Int): List<TvTrackInfo?>? {
        return liveTvView!!.getTracks(type)
    }

    private fun checkTeletextAvailability(liveTvView: TvView) {
        val tracks: List<TvTrackInfo> =
            getTracks(liveTvView, TvTrackInfo.TYPE_SUBTITLE) as List<TvTrackInfo>
        if (tracks == null) {
            serviceImpl.isTTXAvailable = false
            return
        }
        for (track in tracks) {
            val trackEncoding = track.encoding
            if (trackEncoding == "teletext-full-page") {
                serviceImpl.isTTXAvailable = true
                liveTvView!!.setCaptionEnabled(true)
                return
            }
        }
        serviceImpl.isTTXAvailable = false
        startTTXFullPage(liveTvView)
    }

    override fun startTTX(context: Context, liveTvView: TvView, ttxSurfaceView: SurfaceView, changeSize:Boolean) {
        serviceImpl.startTTXChangeSize(ttxSurfaceView.holder.surface, changeSize)
        if (isTTXAvailable()) {
            if(serviceImpl.isTTXActive && changeSize){
                liveTvView?.let {
                    val params = it.layoutParams
                    //aspect ratio per design
                    params.width = 700 * context!!.resources.displayMetrics.density.toInt()
                    params.height = 394 * context!!.resources.displayMetrics.density.toInt()
                    it.handler.post { it.layoutParams = params }
                }
            }
            else{
                liveTvView?.let {
                    val params = it.layoutParams
                    params.width = LinearLayout.LayoutParams.MATCH_PARENT
                    params.height = LinearLayout.LayoutParams.MATCH_PARENT //liveTvView.height
                    it.handler.post { it.layoutParams = params }
                }
            }
            liveTvView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TV_TELETEXT))
        }
    }

    override fun stopTTX() {
        serviceImpl.stopTTX()
    }

    override fun sendKeyToTtx(tvView: TvView, keycode: Int, keyEvent: KeyEvent): Boolean {
        if (serviceImpl.isTTXActive) {
            return tvView.dispatchKeyEvent(keyEvent)
        }
        return false
    }

    override fun isTTXAvailable(): Boolean {
        return serviceImpl.isTTXAvailable
    }

    override fun isTTXActive(): Boolean {
        return serviceImpl.isTTXActive
    }

    override fun isTTXPreviouslyActive(): Boolean {
        return false
    }

    override fun updateTTXStatus(status: Boolean){
        serviceImpl.isTTXAvailable = status
    }

    override fun addCallback(context: Context) {
        serviceImpl.addCallback()
    }

    override fun dispose() {
        serviceImpl.disposetTTX()
    }

    override fun saveDigitalTTXLanguage(value: String) {
        serviceImpl.saveDigitalTTXLanguage(value)
    }

    override fun saveDecodingPageLanguage(value: Int) {
        serviceImpl.saveDecodingPageLanguage(value)
    }

    override fun getDigitalTTXLanguage(): String {
        return serviceImpl.digitalTTXLanguage
    }

    override fun getDecodingPageLanguage(): Int {
        return serviceImpl.decodingPageLanguage
    }
}