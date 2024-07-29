package com.iwedia.cltv.platform.refplus5

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.AudioManager
import android.util.Log
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.base.ClosedCaptionInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.track.ISubtitle

import android.net.Uri
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.google_reference.platform.refplus5.ccConfiguration.ClosedCaptionAnalog
import com.iwedia.google_reference.platform.refplus5.ccConfiguration.ClosedCaptionComposite
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Columns.CLOSED_CAPTION_DISPLAY
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Columns.CLOSED_CAPTION_SERVICE
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_MUTE_ON
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_OFF
import com.mediatek.dtv.tvinput.framework.tifextapi.atsc.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_ON
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC1
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_DISPLAY_MUTE
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC2
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC3
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_CC4
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT1
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT2
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT3
import com.mediatek.dtv.tvinput.framework.tifextapi.composite.settings.lib.Constants.Companion.Common.Values.CLOSED_CAPTION_SERVICE_TEXT4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Locale

class ClosedCaptionInterfaceImpl(val context: Context,utilsInterface: UtilsInterface, playerInterface: PlayerInterface) : ClosedCaptionInterfaceBaseImpl(context, utilsInterface, playerInterface) {
    private val TAG = javaClass.simpleName
    private var selectedCCTrack: String? = null

    val table = "content://" + Constants.AUTHORITY.toString() + "/" + "general"

    private val KEY_CAPTIONS_ADVANCED = "closed_caption_advance_selection"
    private val KEY_CAPTIONS_TEXTSIZE = "closed_caption_text_size"
    //Because China can't spell
    private val KEY_CAPTIONS_FONT_FAMILY = "closed_caption_front_family"
    private val KEY_CAPTIONS_TEXT_COLOR = "closed_caption_text_color"
    private val KEY_CAPTIONS_TEXT_OPACITY = "closed_caption_text_opacity"
    private val KEY_CAPTIONS_EDGE_TYPE = "closed_caption_edge_type"
    private val KEY_CAPTIONS_EDGE_COLOR = "closed_caption_edge_color"
    private val KEY_CAPTIONS_BACKGROUND_COLOR = "closed_caption_background_color"
    private val KEY_CAPTIONS_BACKGROUND_OPACITY = "closed_caption_background_opacity"
    private val KEY_CAPTIONS_LANGUAGE = "closed_caption_language"

    private val ADVANCE_SELECTION_CS1 = 1
    private val ADVANCE_SELECTION_CS2 = 2
    private val ADVANCE_SELECTION_CS3 = 3
    private val ADVANCE_SELECTION_CS4 = 4
    private val ADVANCE_SELECTION_CS5 = 5
    private val ADVANCE_SELECTION_CS6 = 6

    private val FONT_FAMILY_DEFAULT=0
    private val FONT_FAMILY_SERIF_MONO=1
    private val FONT_FAMILY_SERIF=2
    private val FONT_FAMILY_SANS_SERIF_MONO=3
    private val FONT_FAMILY_SANS_SERIF=4
    private val FONT_FAMILY_CASULA=5
    private val FONT_FAMILY_CURSIVE=6
    private val FONT_FAMILY_SMALL_CAPITALS=7

    private val MTK_FONT_FAMILY_DEFAULT=0
    private val MTK_FONT_FAMILY_SANS_SERIF=1
    private val MTK_FONT_FAMILY_SANS_SERIF_MONO=2
    private val MTK_FONT_FAMILY_SERIF=3
    private val MTK_FONT_FAMILY_SERIF_MONO=4
    private val MTK_FONT_FAMILY_CASULA=5
    private val MTK_FONT_FAMILY_CURSIVE=6
    private val MTK_FONT_FAMILY_SMALL_CAPITALS=7

    private val EDGE_TYPE_DEFAULT=0
    private val EDGE_TYPE_NONE=1
    private val EDGE_TYPE_DROP_RAISED=2
    private val EDGE_TYPE_DROP_DEPRESSED=3
    private val EDGE_TYPE_OUTLINE=4
    private val EDGE_TYPE_DROP_SHADOW=5

    private val MTK_EDGE_TYPE_DEFAULT=0
    private val MTK_EDGE_TYPE_NONE=1
    private val MTK_EDGE_TYPE_OUTLINE=2
    private val MTK_EDGE_TYPE_DROP_SHADOW=3
    private val MTK_EDGE_TYPE_DROP_RAISED=4
    private val MTK_EDGE_TYPE_DROP_DEPRESSED=5

    private fun readATSCSettingsValues(context: Context, id: String, default: Int): Int? {
        var value = default
        var cursor: Cursor?
        val URI = Uri.parse(table)
        try {
            cursor = context.contentResolver.query(URI, null, null, null, null)
            if (cursor != null && cursor.moveToNext()) {
                var columnIndex = cursor.getColumnIndex(id.toLowerCase())
                if( columnIndex > 0) {
                    value = cursor.getInt(columnIndex)
                }
                cursor.close()
            }
        } catch (ex: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "ex: Exception:" + ex.printStackTrace())
            return value
        }
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "readATSCSettingsValues: id:$id value:$value")
        return value
    }

    override fun getDefaultCCValues(ccOptions: String): Int? {
        var atscSetting = ""
        atscSetting = when (ccOptions) {
            "display_cc" -> CLOSED_CAPTION_DISPLAY
            "caption_services" -> CLOSED_CAPTION_SERVICE
            "advanced_selection" -> KEY_CAPTIONS_ADVANCED
            "text_size" -> KEY_CAPTIONS_TEXTSIZE
            "font_family" -> KEY_CAPTIONS_FONT_FAMILY
            "text_color" -> KEY_CAPTIONS_TEXT_COLOR
            "text_opacity" -> KEY_CAPTIONS_TEXT_OPACITY
            "edge_type" -> KEY_CAPTIONS_EDGE_TYPE
            "edge_color" -> KEY_CAPTIONS_EDGE_COLOR
            "background_color" -> KEY_CAPTIONS_BACKGROUND_COLOR
            "background_opacity" -> KEY_CAPTIONS_BACKGROUND_OPACITY
            else -> return 0
        }

        var value = readATSCSettingsValues(context, atscSetting, 0)
        var returnValue = value
        when (ccOptions) {
            "display_cc" -> {
                when(value) {
                    CLOSED_CAPTION_DISPLAY_OFF ->  { returnValue = 0 }
                    CLOSED_CAPTION_DISPLAY_ON -> { returnValue = 1 }
                    CLOSED_CAPTION_DISPLAY_MUTE -> { returnValue = 1 }
                }
            }
            "caption_services" -> {
                when(value) {
                    CLOSED_CAPTION_SERVICE_CC1 -> { returnValue = 0 }
                    CLOSED_CAPTION_SERVICE_CC2 -> { returnValue = 1 }
                    CLOSED_CAPTION_SERVICE_CC3 -> { returnValue = 2 }
                    CLOSED_CAPTION_SERVICE_CC4 -> { returnValue = 3 }
                    CLOSED_CAPTION_SERVICE_TEXT1 -> { returnValue = 4 }
                    CLOSED_CAPTION_SERVICE_TEXT2 -> { returnValue = 5 }
                    CLOSED_CAPTION_SERVICE_TEXT3 -> { returnValue = 6 }
                    CLOSED_CAPTION_SERVICE_TEXT4 -> { returnValue = 7 }
                }
            }
            "advanced_selection" -> {
                when(value) {
                    ADVANCE_SELECTION_CS1 -> { returnValue = 0 }
                    ADVANCE_SELECTION_CS2 -> { returnValue = 1 }
                    ADVANCE_SELECTION_CS3 -> { returnValue = 2 }
                    ADVANCE_SELECTION_CS4 -> { returnValue = 3 }
                    ADVANCE_SELECTION_CS5 -> { returnValue = 4 }
                    ADVANCE_SELECTION_CS6 -> { returnValue = 5 }
                }
            }
            "font_family" -> {
                when(value) {
                    MTK_FONT_FAMILY_DEFAULT-> { returnValue = FONT_FAMILY_DEFAULT }
                    MTK_FONT_FAMILY_SANS_SERIF -> { returnValue = FONT_FAMILY_SANS_SERIF }
                    MTK_FONT_FAMILY_SANS_SERIF_MONO -> { returnValue = FONT_FAMILY_SANS_SERIF_MONO }
                    MTK_FONT_FAMILY_SERIF -> { returnValue = FONT_FAMILY_SERIF }
                    MTK_FONT_FAMILY_SERIF_MONO -> { returnValue = FONT_FAMILY_SERIF_MONO }
                    MTK_FONT_FAMILY_CASULA -> { returnValue = FONT_FAMILY_CASULA }
                    MTK_FONT_FAMILY_CURSIVE -> { returnValue = FONT_FAMILY_CURSIVE }
                    MTK_FONT_FAMILY_SMALL_CAPITALS -> { returnValue = FONT_FAMILY_SMALL_CAPITALS }
                }
            }
            "edge_type" -> {
                when(value) {
                    MTK_EDGE_TYPE_DEFAULT -> { returnValue = EDGE_TYPE_DEFAULT }
                    MTK_EDGE_TYPE_NONE -> { returnValue = EDGE_TYPE_NONE }
                    MTK_EDGE_TYPE_OUTLINE -> { returnValue = EDGE_TYPE_OUTLINE }
                    MTK_EDGE_TYPE_DROP_SHADOW -> { returnValue = EDGE_TYPE_DROP_SHADOW }
                    MTK_EDGE_TYPE_DROP_RAISED -> { returnValue = EDGE_TYPE_DROP_RAISED }
                    MTK_EDGE_TYPE_DROP_DEPRESSED -> { returnValue = EDGE_TYPE_DROP_DEPRESSED }
                }
            }
            else -> {}
        }
        return returnValue
    }

    override fun getDefaultMuteValues(): Boolean {
        if (readATSCSettingsValues(context, CLOSED_CAPTION_DISPLAY, CLOSED_CAPTION_DISPLAY_OFF) == CLOSED_CAPTION_DISPLAY_MUTE_ON) {
            return true
        }
        return false
    }

    private fun saveATSCSettingsValue(id: String, value: String): Boolean {
        val URI = Uri.parse(table)
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "saveATSCSettingsValue id: $id value:$value")
        val values = ContentValues()
        values.put(id.toLowerCase(Locale.ROOT), value)
        try {
            var count = context.contentResolver.update(URI, values, null, null)
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "update count = $count")
        } catch (e: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "e: Exception:" + e.message)
            return false
        }
        return true
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, value: Int, isOtherImput: Boolean) {
        var atscSetting = ""
        var saveValue = 0
        when (ccOptions) {
            "display_cc" ->
            {
                selectedCCTrack = null
                atscSetting = CLOSED_CAPTION_DISPLAY
                saveValue = when (value) {
                    0 -> CLOSED_CAPTION_DISPLAY_OFF
                    1 -> CLOSED_CAPTION_DISPLAY_ON
                    2 -> CLOSED_CAPTION_DISPLAY_MUTE_ON
                    else -> return
                }
            }
            "caption_services" ->
            {
                selectedCCTrack = null
                atscSetting = CLOSED_CAPTION_SERVICE
                saveValue = when(value) {
                    0 -> CLOSED_CAPTION_SERVICE_CC1
                    1 -> CLOSED_CAPTION_SERVICE_CC2
                    2 -> CLOSED_CAPTION_SERVICE_CC3
                    3 -> CLOSED_CAPTION_SERVICE_CC4
                    4 -> CLOSED_CAPTION_SERVICE_TEXT1
                    5 -> CLOSED_CAPTION_SERVICE_TEXT2
                    6 -> CLOSED_CAPTION_SERVICE_TEXT3
                    7 -> CLOSED_CAPTION_SERVICE_TEXT4
                    else -> return
                }

            }
            "advanced_selection" -> {
                selectedCCTrack = null
                atscSetting = KEY_CAPTIONS_ADVANCED
                saveValue = when (value) {
                    0 -> ADVANCE_SELECTION_CS1
                    1 -> ADVANCE_SELECTION_CS2
                    2 -> ADVANCE_SELECTION_CS3
                    3 -> ADVANCE_SELECTION_CS4
                    4 -> ADVANCE_SELECTION_CS5
                    5 -> ADVANCE_SELECTION_CS6
                    else -> return
                }
            }
            "text_size" -> {
                atscSetting = KEY_CAPTIONS_TEXTSIZE
                saveValue = value
            }
            "font_family" -> {
                atscSetting = KEY_CAPTIONS_FONT_FAMILY
                when(value) {
                    FONT_FAMILY_DEFAULT-> { saveValue = MTK_FONT_FAMILY_DEFAULT }
                    FONT_FAMILY_SANS_SERIF -> { saveValue = MTK_FONT_FAMILY_SANS_SERIF }
                    FONT_FAMILY_SANS_SERIF_MONO -> { saveValue = MTK_FONT_FAMILY_SANS_SERIF_MONO }
                    FONT_FAMILY_SERIF -> { saveValue = MTK_FONT_FAMILY_SERIF }
                    FONT_FAMILY_SERIF_MONO -> { saveValue = MTK_FONT_FAMILY_SERIF_MONO }
                    FONT_FAMILY_CASULA -> { saveValue = MTK_FONT_FAMILY_CASULA }
                    FONT_FAMILY_CURSIVE -> { saveValue = MTK_FONT_FAMILY_CURSIVE }
                    FONT_FAMILY_SMALL_CAPITALS -> { saveValue = MTK_FONT_FAMILY_SMALL_CAPITALS }
                    else -> { saveValue = MTK_FONT_FAMILY_DEFAULT }
                }
            }
            "text_color" -> {
                atscSetting = KEY_CAPTIONS_TEXT_COLOR
                saveValue = value
            }
            "text_opacity" -> {
                atscSetting = KEY_CAPTIONS_TEXT_OPACITY
                saveValue = value
            }
            "edge_type" -> {
                atscSetting = KEY_CAPTIONS_EDGE_TYPE
                when(value) {
                    EDGE_TYPE_DEFAULT -> { saveValue = MTK_EDGE_TYPE_DEFAULT }
                    EDGE_TYPE_NONE -> { saveValue = MTK_EDGE_TYPE_NONE }
                    EDGE_TYPE_OUTLINE -> { saveValue = MTK_EDGE_TYPE_OUTLINE }
                    EDGE_TYPE_DROP_SHADOW -> { saveValue = MTK_EDGE_TYPE_DROP_SHADOW }
                    EDGE_TYPE_DROP_RAISED -> { saveValue = MTK_EDGE_TYPE_DROP_RAISED }
                    EDGE_TYPE_DROP_DEPRESSED -> { saveValue = MTK_EDGE_TYPE_DROP_DEPRESSED }
                    else -> { saveValue = MTK_EDGE_TYPE_DEFAULT}
                }
            }
            "edge_color" -> {
                atscSetting = KEY_CAPTIONS_EDGE_COLOR
                saveValue = value
            }
            "background_color" -> {
                atscSetting = KEY_CAPTIONS_BACKGROUND_COLOR 
                saveValue = value
            }
            "background_opacity" -> {
                atscSetting = KEY_CAPTIONS_BACKGROUND_OPACITY
                saveValue = value
            }
            else -> return
        }
        ClosedCaptionComposite.saveUserSelectedCCOptions(context, ccOptions ,value)
        ClosedCaptionAnalog.saveUserSelectedCCOptions(context, ccOptions ,value)
        saveATSCSettingsValue(atscSetting,saveValue.toString())
    }

    override fun resetCC() {
    }

    override fun setCCInfo() {
    }

    override fun disableCCInfo() {
        playerInterface.setCaptionEnabled(false)
    }

    override fun setCCWithMute(isEnable: Boolean, audioManager: AudioManager) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"setCCWithMute $isEnable")
        if (isEnable) {
            saveUserSelectedCCOptions("display_cc", 2)
        } else {
            saveUserSelectedCCOptions("display_cc", 1)
        }
    }

    override fun isClosedCaptionEnabled(): Boolean {
        return (getDefaultCCValues("display_cc")!! > 0)
    }

    override fun isCCTrackAvailable(): Boolean {
        val tracks = (playerInterface as PlayerInterfaceImpl).getCCSubtitleTracks()
        return !tracks.isEmpty()
    }
    override fun setClosedCaption(isOtherImput: Boolean): Int {
        val tracks = (playerInterface as PlayerInterfaceImpl).getCCSubtitleTracks()
        val currentTrack = playerInterface.getActiveSubtitle()

        if (tracks.isEmpty()) {
            return 0
        }

        val nextTrack = getNextCCTrack(currentTrack, tracks)
        selectedCCTrack = nextTrack?.languageName ?: ""

        playerInterface.selectSubtitle(nextTrack)

        return 1
    }

    override fun getClosedCaption(isOtherImput: Boolean): String? {
        var ccTrack = ""
        val tracks = (playerInterface as PlayerInterfaceImpl).getCCSubtitleTracks()
        val currentTrack = playerInterface.getActiveSubtitle()

        if (tracks.isEmpty())
            return ccTrack

        if (selectedCCTrack != null)
            return selectedCCTrack

        for (i in tracks.indices) {
            if (tracks[i].trackId == currentTrack?.trackId) {
                ccTrack = tracks[i].languageName.ifEmpty { "" }
            }
        }
        return ccTrack
    }

    override fun updateSelectedTrack() {
        selectedCCTrack = playerInterface.getActiveSubtitle()?.languageName?: ""
    }

    override fun getSubtitlesState(): Boolean {
        if (utilsInterface.getRegion() == Region.US) {
            return false
        }
        return true
    }

    private fun getNextCCTrack(
        currentTrack: ISubtitle?,
        currentTrackList: List<ISubtitle>
    ): ISubtitle? {
        if (currentTrack?.trackId == null) {
            return currentTrackList[0]
        }

        var nextTrack: ISubtitle? = null
        for (i in currentTrackList.indices) {
            if (currentTrack.trackId == currentTrackList[i].trackId) {
                nextTrack =
                    if (i + 1 < currentTrackList.size) {
                        currentTrackList[i + 1]
                    } else {
                        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "ClosedCaption", "next cc track is null")
                        null
                    }
                break
            }
        }
        return nextTrack
    }

    override fun applyClosedCaptionStyle() {
        // Note: Since The closed caption style is not applied automatically, Here we are doing it manually
        CoroutineHelper.runCoroutine({
            arrayOf(
                KEY_CAPTIONS_TEXTSIZE,
                KEY_CAPTIONS_FONT_FAMILY,
                KEY_CAPTIONS_TEXT_COLOR,
                KEY_CAPTIONS_TEXT_OPACITY,
                KEY_CAPTIONS_EDGE_TYPE,
                KEY_CAPTIONS_EDGE_COLOR,
                KEY_CAPTIONS_BACKGROUND_COLOR,
                KEY_CAPTIONS_BACKGROUND_OPACITY).forEach {key ->
                val value = readATSCSettingsValues(context,key, -1)

                if(value!=-1){
                    saveATSCSettingsValue(key,value!!.toString())
                }
            }
        }, context = Dispatchers.Default)
    }
}