package com.iwedia.cltv.platform

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Rect
import android.media.tv.TvView
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.base.InputSourceBaseImpl
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.refplus5.SaveValue
import com.iwedia.cltv.platform.refplus5.screenMode.Constants.Companion.CURRENT_SOURCE
import com.iwedia.cltv.platform.refplus5.screenMode.Constants.Companion.SOURCE_URI_URI
import com.google.android.tv.inputplayer.api.LastUsedInput
import com.iwedia.cltv.platform.model.Constants

internal class InputSourceImpl(
    val applicationContext: Context,
    utilsModule: UtilsInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) :
    InputSourceBaseImpl(applicationContext, utilsModule, parentalControlSettingsInterface) {

    private val TAG = javaClass.simpleName
    private var playbackView: TvView? = null
    private val TV_MODE_AUTHORITY =
        "com.google.android.apps.tv.launcherx.coreservices.DeviceModeContentProvider"
    private val TV_MODE_QUERY = "queryDeviceMode"
    companion object{
        private const val ACTION_INPUT_SELECTED = "android.apps.tv.launcherx.INPUT_SELECTED"
        private const val EXTRA_INPUT_ID = "extra_input_id"
        private const val LAUNCHERX_PACKAGE_NAME = "com.google.android.apps.tv.launcherx"
        private const val PERMISSION_WRITE_EPG_DATA =
            "com.android.providers.tv.permission.WRITE_EPG_DATA"
        private const val DEFAULT_ATSC_INPUT_ID = "com.mediatek.dtv.tvinput.atsctuner/.AtscTunerInputService/HW0"
        private const val DEFAULT_DVB_INPUT_ID = "com.mediatek.dtv.tvinput.dvbtuner/.DvbTvInputService/HW0"
        private const val ANALOG_INPUT_SERVICE = "AnalogInputService"
        private const val US = "US"
        private const val PA = "PA"
        private const val EU = "EU"
    }
    object Source {
        const val NAME = "Source"
        const val FOCUS = "Focus"
        const val ATV = "atv"
        const val DTV = "dtv"
        const val COMPOSITE = "composite"
        const val COMPONENT = "component"
        const val VGA = "vga"
        const val HDMI1 = "hdmi1"
        const val HDMI2 = "hdmi2"
        const val HDMI3 = "hdmi3"
        const val HDMI4 = "hdmi4"
        const val MM = "mm"
        val LIST_FOR_SOURCE_ITEMS =
            listOf(ATV, DTV, COMPOSITE, COMPONENT, VGA, HDMI1, HDMI2, HDMI3, HDMI4, MM)
    }

    object SourceInfo {
        const val V_X = "positionX"
        const val V_Y = "positionY"
        const val V_WIDTH = "Width"
        const val V_HEIGHT = "Hight"
        const val V_UNIQUESTREAM_ID = "uniquestreamId"
        const val V_HDRYTPE = "hdrType"
        const val V_PICTURE_MODE = "mPictureMode"
    }






    private fun setCurrentSource(tvView: TvView?, inputId: String, source: String) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Set setCurrentSource: $inputId $source");
        SaveValue.saveWorldValue(context, CURRENT_SOURCE, inputId, true)

        var rect = Rect()
        tvView?.getGlobalVisibleRect(rect)
        val values = ContentValues()
        values.put(Source.NAME, source)
        values.put(SourceInfo.V_X, 0)
        values.put(SourceInfo.V_Y, 0 )
        values.put(SourceInfo.V_WIDTH, 1920)
        values.put(SourceInfo.V_HEIGHT, 1080)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "x,y,w,h ${rect.left}, ${rect.top}, ${rect.right - rect.left}, ${rect.bottom - rect.top}")
        try {
            context.contentResolver.update(SOURCE_URI_URI, values, null, null)
        } catch (ex: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "${ex.printStackTrace()}")
        }

        val ITEM = "item"
        val SOURCE_ID = "Source"
        val AUTHORITY = "com.mediatek.tv.agent.sounddb"
        val UPDATE_SOURCE = "update_source"
        val COMMON = "common"
        val UPDATE_SOURCE_CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + UPDATE_SOURCE)
        val soundDbValues = ContentValues()
        val SELECTION: String = ITEM + "=?"
        val SELECTIONARGS = arrayOf(SOURCE_ID)
        soundDbValues.put(COMMON, source)
        try {
            context.contentResolver.update(
                UPDATE_SOURCE_CONTENT_URI,
                soundDbValues,
                SELECTION,
                SELECTIONARGS
            )
        } catch (ex: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "setSource ${ex.printStackTrace()}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun saveLastUsedInput(inputId: String) {
        val intent = Intent(ACTION_INPUT_SELECTED)
        intent.putExtra(EXTRA_INPUT_ID, inputId)
        intent.setPackage(LAUNCHERX_PACKAGE_NAME)
        applicationContext.sendBroadcast(intent, PERMISSION_WRITE_EPG_DATA)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getDefaultInputId(): String {
        val region = utilsModule.getRegion().name
        when (region) {
            US -> return DEFAULT_ATSC_INPUT_ID
            EU, PA -> return DEFAULT_DVB_INPUT_ID
        }
        return ""
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setLastUsedInput(inputId: String) {
        var inputIdValue = inputId
        if (inputId.isEmpty() || inputId.contains(ANALOG_INPUT_SERVICE)) {
            inputIdValue = getDefaultInputId()
        }
        saveLastUsedInput(inputIdValue)
        LastUsedInput.enterInput(context, inputIdValue)
        inputChanged.value = true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun exitTVInput(inputId: String) {
        var inputIdValue = inputId
        if (inputId.isEmpty() || inputId.contains(ANALOG_INPUT_SERVICE)) {
            inputIdValue = getDefaultInputId()
        }
        LastUsedInput.exitInput(context, inputIdValue)
    }

    override fun isBasicMode(): Boolean {
        var basicMode = false
        var mCursor: Cursor? = null
        try {
            val uri = Uri.parse("content://" + TV_MODE_AUTHORITY + "/" + TV_MODE_QUERY)
            mCursor = applicationContext.contentResolver.query(
                uri,  /* projection= */
                null,  /* queryArgs= */
                null,  /* cancellationSignal= */
                null
            )
            if (mCursor != null && mCursor.moveToNext()) {
                //if user mode is 2 the device is set in basic mode
                basicMode = mCursor.getInt( /* column= */0) == 2
            }

        } catch (e: java.lang.Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "isBasic Exception ocurred : $e")

        } finally {
            mCursor?.close()
        }
        return basicMode
    }
}