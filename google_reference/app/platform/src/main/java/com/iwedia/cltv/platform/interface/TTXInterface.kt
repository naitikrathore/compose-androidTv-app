package com.iwedia.cltv.platform.`interface`

import android.content.Context
import android.media.tv.TvView
import android.view.KeyEvent
import android.view.SurfaceView

interface TTXInterface {
    var decodeingPageLanguage:MutableList<String>
    fun startTTX(context: Context, liveTvView: TvView, ttxSurfaceView: SurfaceView, changeSize: Boolean = false)
    fun stopTTX()
    fun sendKeyToTtx(tvView: TvView, keycode: Int, keyEvent: KeyEvent) : Boolean
    fun isTTXAvailable() : Boolean
    fun isTTXActive(): Boolean
    fun isTTXPreviouslyActive(): Boolean
    fun updateTTXStatus(status: Boolean)
    fun addCallback(context: Context)
    fun dispose()
    fun saveDigitalTTXLanguage(value: String)
    fun saveDecodingPageLanguage(value: Int)
    fun getDigitalTTXLanguage() : String
    fun getDecodingPageLanguage() : Int
}