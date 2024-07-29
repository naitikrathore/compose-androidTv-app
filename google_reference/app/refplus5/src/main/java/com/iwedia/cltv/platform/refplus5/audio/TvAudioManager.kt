package com.iwedia.cltv.platform.refplus5.audio

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.parental.Region

class TvAudioManager(context: Context) {

    private val TAG = javaClass.simpleName

    private var mAudioManager: AudioManager

    companion object {
        @JvmField val CFG_AUD_AD_SPEAKER_ENABLE = "AQ_AdSpeakerEnable"
        @JvmField val CFG_AUD_AD_HEADPHONE_ENABLE = "AQ_AdHeadphoneEnable"
        @JvmField val CFG_AUD_AD_VOLUME = "AQ_AdVolume"
        @JvmField val CFG_AUD_PAN_FADE_ENABLE = "AQ_PanFadeEnable"
        @JvmField val CFG_MS12_FADER_CONTROL = "MS12_FaderControl"
        @JvmField val ACCESSIBILITY_AUDIO_TYPE = "Accessibility_AudioType"
        @JvmField val CFG_AUD_AD_UI_ENABLE = "AQ_AdUiEnable"
        @JvmField val CFG_AUD_SPOKEN_SUBTITLE_UI_ENABLE = "AQ_SpokenSubtitleUiEnable"

        const val ITEM_ON = "On"
        const val ITEM_OFF = "Off"

        @JvmField
        val LIST_FOR_FADER_CONTROL = listOf("Main Max", "Main 3:4", "Equal", "AD 3:4", "AD Max")
    }

    init {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun set(context: Context, id: String, value: Boolean, region : Region) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "set String ${id} ${value}")
        set(context, id, if (value) ITEM_ON else ITEM_OFF,region)
    }
    fun set(context: Context, id: String, value: String, region : Region) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "set String ${id} ${value}")
        set(context, id, value, true, region)
    }
    fun set(context: Context, id: String, value: Int, updateSoundDB: Boolean, region : Region) {
        set(context, id, value.toString(), updateSoundDB, region)
    }

    fun set(context: Context, id: String, value: String, updateSoundDB: Boolean, region : Region) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "set String ${id} ${value} ${updateSoundDB}")

        if (updateSoundDB) {
            // save to provider
            Constants.setValue(context.getContentResolver(), id, value)
        }

        // For special case
        when (id) {
            ACCESSIBILITY_AUDIO_TYPE -> {
                setAudioType(context, Integer.parseInt(value), region)
                return
            }
            CFG_MS12_FADER_CONTROL -> {
                var newValue = 0
                when (value) {
                    "Main Max" -> {
                        newValue = -32
                    }
                    "Main 3:4" -> {
                        newValue = -16
                    }
                    "Equal" -> {
                        newValue = 0
                    }
                    "AD 3:4" -> {
                        newValue = 16
                    }
                    "AD Max" -> {
                        newValue = 32
                    }
                }
                mAudioManager.setParameters(id + "=" + newValue)
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                    TAG,
                    "MS12_FaderControl value=${value} newValue=${newValue} setParameters end")
                return
            }
        }

        mAudioManager.setParameters(id + "=" + value)
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "setParameters end")
    }

    fun getAudioType(context: Context, def: Int, region : Region, callback: (type: Int)-> Unit) {
        CoroutineHelper.runCoroutine({
            Log.i(TAG, "getAudioType")
            val URI: Uri?
            var id: String?
            var cursor: Cursor?
            var value = def
            when (region) {
                Region.US -> {
                    URI =
                        Uri.parse(
                            "content://" +
                                    com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.AUTHORITY +
                                    "/" +
                                    "general") // com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.TABLE
                    id =
                        com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Columns
                            .AUDIO_TYPE
                }
                else -> {
                    URI =
                        Uri.parse(
                            "content://" +
                                    com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUTHORITY +
                                    "/" +
                                    com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.DATATYPE_GENERAL)
                    id = com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_AUDIO_TYPE
                }
            }

            try {
                cursor = context.getContentResolver().query(URI, arrayOf(id), null, null, null)
                if (cursor != null && cursor.moveToNext()) {
                    val index =
                        cursor.getColumnIndex("audio_type")
                    Log.i(TAG, " gettAudioType index = ${index}")
                    value = cursor.getInt(index)
                }
                cursor?.close()
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Exception++++ ${ex.toString()}")
                callback(value)
                return@runCoroutine
            }

            Log.i(TAG, "getAudioType: ${value}")
            try {
                cursor?.close()
            } catch (ex: Exception) {}
            callback(value)
        })

    }

    fun setAudioType(context: Context, value: Int, region : Region) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "setAudioType:$value")
        val URI: Uri?
        var values = ContentValues()
        when (region) {
            Region.US -> {
                URI =
                    Uri.parse(
                        "content://" +
                                com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.AUTHORITY +
                                "/" +
                                "general") // com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.TABLE
                values.put(
                    com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Columns
                        .AUDIO_TYPE,
                    value)
            }
            else -> { // EU Region
                URI =
                    Uri.parse(
                        "content://" +
                                com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUTHORITY +
                                "/" +
                                com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.DATATYPE_GENERAL)
                values.put(
                    com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_AUDIO_TYPE, value)
            }
        }

        try {
            context.getContentResolver().update(URI, values, null, null)
        } catch (ex: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Exception++++" + Log.getStackTraceString(ex))
        }

        if (0 == value || 2 == value) {
            // 0 is mean Normal,2 is mean Hearing Impaired
            mAudioManager.setParameters(CFG_AUD_AD_UI_ENABLE + "=" + ITEM_OFF)
            mAudioManager.setParameters(CFG_AUD_SPOKEN_SUBTITLE_UI_ENABLE + "=" + ITEM_OFF)
            mAudioManager.setParameters("AQ_Ac4PrefAsocType" + "=" + value)
        } else if (1 == value) {
            // 1 is mean Audio Description
            mAudioManager.setParameters(CFG_AUD_AD_UI_ENABLE + "=" + ITEM_ON)
            mAudioManager.setParameters(CFG_AUD_SPOKEN_SUBTITLE_UI_ENABLE + "=" + ITEM_OFF)
            mAudioManager.setParameters("AQ_Ac4PrefAsocType" + "=" + value)
        } else if (3 == value) {
            // 3 is mean Spoken Subtitle
            mAudioManager.setParameters(CFG_AUD_AD_UI_ENABLE + "=" + ITEM_OFF)
            mAudioManager.setParameters(CFG_AUD_SPOKEN_SUBTITLE_UI_ENABLE + "=" + ITEM_ON)
            mAudioManager.setParameters("AQ_Ac4PrefAsocType" + "=" + "4")
        } else if (4 == value) {
            // 4 is mean Audio Description and Spoken Subtitle
            mAudioManager.setParameters(CFG_AUD_AD_UI_ENABLE + "=" + ITEM_ON)
            mAudioManager.setParameters(CFG_AUD_SPOKEN_SUBTITLE_UI_ENABLE + "=" + ITEM_ON)
            mAudioManager.setParameters("AQ_Ac4PrefAsocType" + "=" + "5")
        }
    }

    fun get(context: Context, id: String, def: Int): Int {
        val intValue = Constants.getIntValue(context.getContentResolver(), id, def)
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "get ${id} ${intValue}")
        return intValue
    }

    fun getString(context: Context, id: String): String {
        var stringValue = Constants.getStringValue(context.getContentResolver(), id)
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "get ${id} ${stringValue}")
        return stringValue
    }
    fun getBoolean(context: Context, id: String): Boolean {
        return when {
            ITEM_ON == getString(context, id) -> {
                true
            }
            ITEM_OFF == getString(context, id) -> {
                false
            }
            else -> {
                return false
            }
        }
    }

    fun setMixingLevel(context: Context, value: Int, region: Region) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "setMixingLevel:$value")
        val URI: Uri?
        var values = ContentValues()
        when (region) {
            Region.US -> {
                URI =
                    Uri.parse(
                        "content://" +
                                com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.AUTHORITY +
                                "/" +
                                "general") // com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.TABLE
                values.put(
                    com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Columns
                        .MIXING_LEVEL,
                    value)
            }
            else -> { // EU Region
                URI =
                    Uri.parse(
                        "content://" +
                                com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.AUTHORITY +
                                "/" +
                                com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.DATATYPE_GENERAL)
                values.put(
                    com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants.COLUMN_AUDIO_MIXING_LEVEL,
                    value)
            }
        }

        try {
            context.getContentResolver().update(URI, values, null, null)
        } catch (ex: Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Exception++++" + android.util.Log.getStackTraceString(ex))
        }
    }
}
