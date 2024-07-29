package com.iwedia.cltv.platform.refplus5

import android.content.Context
import android.media.tv.TvView
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.base.TTXInterfaceBaseImpl
import com.mediatek.dtv.tvinput.client.dataservicesignalinfo.DataServiceSignalInfo
import com.mediatek.dtv.tvinput.client.dataservicesignalinfo.DataServiceSignalInfo.ISignalInfoListener
import com.mediatek.dtv.tvinput.framework.tifextapi.common.dataservicesignalinfo.Constants

class TTXInterfaceImpl(val utilsInterface: UtilsInterface) :TTXInterfaceBaseImpl(utilsInterface) {
    private val TAG = javaClass.simpleName
    private var context: Context? = null
    private var isTTXRunning: Boolean = false
    private var isTTXPreviuoslyRunning: Boolean = false
    private var ttxCallback: ISignalInfoListener? = null
    private var dataServiceSignalInfo: DataServiceSignalInfo? = null

    override var decodeingPageLanguage = mutableListOf(
        (utilsInterface as UtilsInterfaceImpl).getStringValue("WEST_EUR"),
        utilsInterface.getStringValue("EAST_EUR"),
        utilsInterface.getStringValue("RUSSIA"),
        utilsInterface.getStringValue("RUSSIA_2"),
        utilsInterface.getStringValue("GREEK"),
        utilsInterface.getStringValue("Turkey"),
        utilsInterface.getStringValue("Arab_Hbrw"),
        utilsInterface.getStringValue("Farsian"),
        utilsInterface.getStringValue("Arab"),
        utilsInterface.getStringValue("BYELORUSSIAN")
    )

    override fun startTTX(context: Context, liveTvView: TvView, ttxSurfaceView: SurfaceView, changeSize: Boolean) {
        if (isTTXAvailable()) {
            isTTXRunning = !isTTXRunning
            isTTXPreviuoslyRunning = isTTXRunning
            if(isTTXRunning && changeSize){
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
        isTTXRunning = false
    }

    override fun sendKeyToTtx(liveTvView: TvView, keycode: Int, keyEvent: KeyEvent): Boolean {
        if (isTTXRunning) {
            return liveTvView.dispatchKeyEvent(keyEvent)
        }
        return false
    }

    override fun isTTXAvailable() : Boolean {
        return (utilsInterface as UtilsInterfaceImpl).isTeletextAvailable()
    }

    override fun isTTXActive(): Boolean {
        return isTTXRunning
    }

    override fun isTTXPreviouslyActive(): Boolean {
        return isTTXPreviuoslyRunning
    }

    override fun updateTTXStatus(status: Boolean) {
        isTTXRunning = status
    }
    override fun dispose() {
        removeCallback()
    }

    override fun addCallback(context: Context) {
        this.context = context

        ttxCallback = object : ISignalInfoListener {
            override fun onSignalInfoChanged(sessionToken: String?, changedSignalInfo: Bundle) {
                if (changedSignalInfo.containsKey(KEY_TIS_IS_TELETEXT_RUNNING)) {
                    isTTXRunning =
                        getTeletextSignalInfo(context, (utilsInterface as UtilsInterfaceImpl).getTvInputId())
                            .getDataServiceSignalInfo(sessionToken!!)!!
                            .getBoolean(KEY_TIS_IS_TELETEXT_RUNNING)
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "onSignalInfoChanged: isTTXRunning = $isTTXRunning")
                }
            }
        }

        getTeletextSignalInfo(context, (utilsInterface as UtilsInterfaceImpl).getTvInputId()).addDataServiceSignalInfoListener("TTXClientToken", ttxCallback)
    }

    override fun saveDigitalTTXLanguage(value:String) {
        try {
            SaveValue.saveTISSettingsStringValue(
                context!!,
                COLUMN_TELETEXT_DIGITAL_LANGUAGE,
                AUTHORITY,
                DATATYPE_GENERAL,
                value
            )
        } catch (ex: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "saveDigitalTTXLanguage: exception : ${ex.message}")
        }
    }

    override fun saveDecodingPageLanguage(value: Int) {
        try {
            SaveValue.saveTISSettingsIntValue(
                context!!,
                COLUMN_TELETEXT_DECODING_LANGUAGE,
                AUTHORITY,
                DATATYPE_GENERAL, value.toInt()
            )
        } catch (ex: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "saveDecodingPageLanguage: exception : ${ex.message}")
        }
    }

    override fun getDigitalTTXLanguage(): String {
        var digitalTTXLanguage = ""

        try {
            digitalTTXLanguage = SaveValue.readTISSettingsStringValues(
                context!!,
                COLUMN_TELETEXT_DIGITAL_LANGUAGE,
                AUTHORITY,
                DATATYPE_GENERAL,
                ""
            )
        } catch (ex: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "readDigitalTTXLanguage: exception : ${ex.message}")
        }
        return digitalTTXLanguage
    }

    override fun getDecodingPageLanguage(): Int {
        var decodingPageLanguage = 0
        try {
            decodingPageLanguage = SaveValue.readTISSettingsIntValues(
                context!!,
                COLUMN_TELETEXT_DECODING_LANGUAGE,
                AUTHORITY,
                DATATYPE_GENERAL,
                0
            )
        } catch (ex: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "readDecodingPageLanguage: exception : ${ex.message}")
        }
        return decodingPageLanguage
    }

    private fun removeCallback() {
        getTeletextSignalInfo(context!!, (utilsInterface as UtilsInterfaceImpl).getTvInputId()).removeDataServiceSignalInfoListener("TTXClientToken", ttxCallback)
    }

    private fun getTeletextSignalInfo(context: Context, inputId: String): DataServiceSignalInfo {
        if (dataServiceSignalInfo == null) {
            dataServiceSignalInfo = DataServiceSignalInfo(context, inputId)
        }
        return dataServiceSignalInfo!!
    }

    companion object {
        const val EVENT_TYPE_TTX_AVAILABLE = "TTX_STATUS_NOTIFY"
        const val KEY_TIS_TTX_AVAILABLE = "TTX_AVAILABLE"
        const val KEY_TIS_IS_TELETEXT_RUNNING: String = Constants.KEY_IS_TELETEXT_RUNNING
        const val COLUMN_TELETEXT_DIGITAL_LANGUAGE = com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_TELETEXT_DIGITAL_LANGUAGE
        const val AUTHORITY = com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUTHORITY
        const val DATATYPE_GENERAL = com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.DATATYPE_GENERAL
        const val COLUMN_TELETEXT_DECODING_LANGUAGE = com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_TELETEXT_DECORDING_LANGUAGE
    }
}