package com.iwedia.cltv.platform.rtk

import android.content.Context
import android.media.tv.TvView
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.TTXInterfaceBaseImpl
import com.realtek.system.RtkConfigs
import com.realtek.tv.closedcaption.CustomCaptionManager
import com.realtek.tv.hbbtv.hbbtvmanagerutils.PreferredLanguage
import com.realtek.tv.utils.TvSource

class TTXInterfaceImpl(val utilsInterface: UtilsInterface) :TTXInterfaceBaseImpl(utilsInterface) {
    private val TAG = javaClass.simpleName
    private val ACTIVE_TV_SOURCE_PATH = 0
    private val TTX_DISPLAY_NORMAL = 1
    private val ENABLED_STATE_OFF = 0
    private val ENABLED_STATE_ON = 1
    private val ACCESSIBILITY_CUSTOM_CAPTIONING_ENABLED = "accessibility_custom_captioning_enabled"

    private var context: Context? = null

    override var decodeingPageLanguage = mutableListOf(
        (utilsInterface as UtilsInterfaceImpl).getStringValue("WEST_EUR"),
        utilsInterface.getStringValue("EAST_EUR"),
        utilsInterface.getStringValue("russian"),
        utilsInterface.getStringValue("Arabic"),
        utilsInterface.getStringValue("Farsian"),
        utilsInterface.getStringValue("Hebrew"),
        utilsInterface.getStringValue("Greek"),
        utilsInterface.getStringValue("Turkey")
    )

    @RequiresApi(Build.VERSION_CODES.P)
    override fun startTTX(
        context: Context,
        liveTvView: TvView,
        ttxSurfaceView: SurfaceView,
        changeSize: Boolean
    ) {
        this.context = context
        liveTvView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TV_TELETEXT))

        Settings.Secure.putInt(
            context.contentResolver,
            ACCESSIBILITY_CUSTOM_CAPTIONING_ENABLED, ENABLED_STATE_ON
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun stopTTX() {
        try {
            if (isTTXActive()) {
                Settings.Secure.putInt(
                    context?.contentResolver,
                    ACCESSIBILITY_CUSTOM_CAPTIONING_ENABLED, ENABLED_STATE_OFF
                )
            }
        } catch (ex: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun isTTXAvailable() : Boolean {
        return RtkConfigs.TvConfigs.SUPPORTED_TELTEX
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun isTTXActive(): Boolean {
        val tvSource: TvSource = (utilsInterface as UtilsInterfaceImpl).getTvSetting()
            .getActivatedTvSource(ACTIVE_TV_SOURCE_PATH) ?: return false
        val ttxDisplayType: Int = utilsInterface.getTvSetting().getTtxDispType(tvSource.src, tvSource.port)
        return ttxDisplayType == TTX_DISPLAY_NORMAL
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun sendKeyToTtx(liveTvView: TvView, keycode: Int, keyEvent: KeyEvent): Boolean {
        if (isTTXActive()) {
            return liveTvView.dispatchKeyEvent(keyEvent)
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun saveDigitalTTXLanguage(value: String) {
        (utilsInterface as UtilsInterfaceImpl).getTvSetting().dtvTeletextLanguage =
            PreferredLanguage.getPreferredLanguage(value).langCode
    }

    override fun saveDecodingPageLanguage(value: Int) {
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getDigitalTTXLanguage(): String {
        val ttxLanguage = (utilsInterface as UtilsInterfaceImpl).getTvSetting().dtvTeletextLanguage
        return PreferredLanguage.getPreferredLanguage(ttxLanguage).langCode639Part2
    }

    override fun getDecodingPageLanguage(): Int {
        return 0
    }
}