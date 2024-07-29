package com.iwedia.cltv.platform.gretzky

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.base.UtilsInterfaceBaseImpl
import com.iwedia.cltv.platform.gretzky.language.LanguageMapperImpl
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType

class UtilsInterfaceImpl(
    private var context: Context,
    textToSpeechModule: TTSInterface
) : UtilsInterfaceBaseImpl(context, textToSpeechModule) {
    val CODEC_AUDIO_AC3 = "ac3"
    val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
    val CODEC_AUDIO_EAC3 = "eac3"
    val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
    val CODEC_AUDIO_DTS = "dts"

    var primaryAudioLang: String = ""
    var secondaryAudioLang: String = ""
    var primarySubtitlesLang: String = ""
    var secondarySubtitlesLang: String = ""

    val AUTHORITY = "com.google.android.tv.dtvprovider"
    val LANGUAGE_SETTINGS_PATH = "tvsettings"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$LANGUAGE_SETTINGS_PATH");

    private val CBS_STRETCH = "stretch"
    private val CBS_ASPECT_RATIO_4X3_LETTER_BOX = "4x3_letter_box"
    private val CBS_ASPECT_RATIO_4X3_PAN_AND_SCAN = "4x3_pan_and_scan"
    private val CBS_ASPECT_RATIO_16X9_PILLAR_BOX = "16x9_pillar_box"
    private val CBS_ASPECT_RATIO_16X9_ZOOM = "16x9_zoom"
    private val PREF_STRETCH = 0
    private val PREF_PAN_SCAN_4_3 = 1
    private val PREF_LETTER_BOX_4_3 = 2
    private val PREF_PAN_SCAN_16_9 = 3
    private val PREF_PILLAR_BOX_16_9 = 4
    private val KEY_ASPECT_RATIO = "media.video.aspectratio"

    override var jsonPath: String = "assets/OemCustomizationJsonFolder/OemCustomizationGretzky.json"

    override var languageCodeMapper : LanguageMapperInterface = LanguageMapperImpl()

    //todo remove utilsInterface calls and replace functions from utils to player
    @RequiresApi(Build.VERSION_CODES.R)
    override fun getDolby(tvTrackInfo: TvTrackInfo): String {
        if (tvTrackInfo.encoding == CODEC_AUDIO_AC3 || tvTrackInfo.encoding == CODEC_AUDIO_AC3_ATSC)
            return "AC3"
        if (tvTrackInfo.encoding == CODEC_AUDIO_EAC3 || tvTrackInfo.encoding == CODEC_AUDIO_EAC3_ATSC)
            return "EAC3"
        if (tvTrackInfo.encoding == CODEC_AUDIO_DTS)
            return "DTS"
        return ""
    }

    override fun isGretzkyBoard(): Boolean {
        return true
    }

    override fun updateAudioTracks() {
        if (primaryAudioLang != "") {
            setPrimaryAudioLanguage(primaryAudioLang)
        }
        if (secondaryAudioLang != "") {
            setSecondaryAudioLanguage(secondaryAudioLang)
        }
    }

    override fun updateSubtitleTracks() {
        if (primarySubtitlesLang != "") {
            setPrimarySubtitleLanguage(primarySubtitlesLang)
        }
        if (secondarySubtitlesLang != "") {
            setSecondarySubtitleLanguage(secondarySubtitlesLang)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getCodecDolbySpecificAudioInfo(tvTrackInfo: TvTrackInfo): String {
        var info = ""
        var dolbyType = getDolby(tvTrackInfo)
        if(!dolbyType.isNullOrEmpty()){
            info = when (dolbyType) {
                CODEC_AUDIO_EAC3, CODEC_AUDIO_EAC3_ATSC  -> info.plus(DOLBY_AUDIO_EAC3_NAME_TYPE)
                CODEC_AUDIO_AC3, CODEC_AUDIO_AC3_ATSC -> info.plus(DOLBY_AUDIO_AC3_NAME_TYPE)
                CODEC_AUDIO_DTS -> info.plus(AUDIO_DTS_TYPE)
                else -> info
            }
        }
        return info
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        return tvTrackInfo!!.isAudioDescription
    }

    fun update(key: String?, value: String?) {
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Update " + key + " : " + value)
        try {
            val contentValues = ContentValues()
            val uri = Uri.withAppendedPath(CONTENT_URI, key)
            contentValues.put(VALUE, value)

            val count: Int = context.contentResolver.update(
                uri,
                contentValues,
                null,
                null
            )
            if (count == 0) {
                // insert this new {key, value} in table
                contentValues.put(KEY, key)
                context.contentResolver.insert(
                    CONTENT_URI,
                    contentValues
                )
            }
        } catch (e: Exception) {
            Log.w("AudioSubtitle", e.message!!)
        }
    }

    @SuppressLint("Range")
    fun read(key: String): String? {
        var value: String? = null
        val projection = arrayOf<String>(KEY, VALUE)
        val cursor: Cursor? = context.contentResolver.query(
            CONTENT_URI,
            projection,
            KEY.toString() + " = '" + key + "'",  // String selection
            null,  // String[] selectionArgs
            null
        ) // String sortOrder
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // key = cursor.getString(0);
                value = cursor.getString(1)
            }
            cursor.close()
        }
        return value
    }

    fun insert(key: String?, value: String?) {
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Insert " + key + " : " + value)
        try {
            val contentValues = ContentValues()
            contentValues.put(VALUE, value)

            // insert this new {key, value} in table
            contentValues.put(KEY, key)
            context.contentResolver.insert(
                CONTENT_URI,
                contentValues
            )
        } catch (e: Exception) {
            Log.w("AudioSubtitle", e.message!!)
        }
    }

    override fun getPrimaryAudioLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getPrimaryAudioLanguage(type)
        var primaryAudioLanguage: String?
        val preferredLanguage: String? = read(KEY_AUDIO_LANG)
        if (preferredLanguage != null) {
            if (preferredLanguage.contains(";")) {  // Suppose primary and secondary subtitle languages
                var codes = arrayOf<String>()
                codes = preferredLanguage.split(";").toTypedArray()
                primaryAudioLanguage = codes[0];
                if(primaryAudioLanguage=="null")
                    primaryAudioLanguage =
                        languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))

            } else {

                primaryAudioLanguage = preferredLanguage;

            }
        } else {
            primaryAudioLanguage =
                languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))
        }
        return primaryAudioLanguage
    }

    override fun getSecondaryAudioLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getSecondaryAudioLanguage(type)
        var secondaryAudioLanguage: String?
        val preferredLanguage: String? = read(KEY_AUDIO_LANG)
        if (preferredLanguage != null) {
            if (preferredLanguage.contains(";")) {  // Suppose primary and secondary audio languages
                var codes = arrayOf<String>()
                codes = preferredLanguage.split(";").toTypedArray()
                secondaryAudioLanguage = codes[1];
            } else {
                secondaryAudioLanguage = "eng";
            }
        } else {
            secondaryAudioLanguage = "eng";
        }
        return secondaryAudioLanguage
    }

    override fun getPrimarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getPrimarySubtitleLanguage(type)
        var primarySubtitleLanguage: String?
        val preferredLanguage: String? = read(KEY_SUBTITLE_LANG)
        if (preferredLanguage != null) {
            if (preferredLanguage.contains(";")) {  // Suppose primary and secondary subtitle languages
                var codes = arrayOf<String>()
                codes = preferredLanguage.split(";").toTypedArray()
                primarySubtitleLanguage = codes[0]
                if(primarySubtitleLanguage=="null")
                    primarySubtitleLanguage =
                        languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))

            } else {
                primarySubtitleLanguage = preferredLanguage
            }
        } else {
            primarySubtitleLanguage =
                languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))
        }
        return primarySubtitleLanguage
    }

    override fun getSecondarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getSecondarySubtitleLanguage(type)
        var secondarySubtitleLanguage: String?
        val preferredLanguage: String? = read(KEY_SUBTITLE_LANG)
        if (preferredLanguage != null) {
            if (preferredLanguage.contains(";")) {  // Suppose primary and secondary subtitle languages
                var codes = arrayOf<String>()
                codes = preferredLanguage.split(";").toTypedArray()
                secondarySubtitleLanguage = codes[1]
            } else {
                secondarySubtitleLanguage = "eng";
            }
        } else {
            secondarySubtitleLanguage = "eng";
        }
        return secondarySubtitleLanguage
    }

    private fun preferredLanguagesToDatabaseString(primary: String?, secondary: String?): String? {
        var databaseString: String?
        databaseString = "$primary;$secondary"
        return databaseString
    }

    override fun setPrimaryAudioLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setPrimaryAudioLanguage(language, type)
        var preferredAudioLanguages: String? = ""
        var secondaryLanguage = getSecondaryAudioLanguage()
        if (secondaryLanguage == null || secondaryLanguage == "") {
            preferredAudioLanguages = language
        } else {
            preferredAudioLanguages =
                preferredLanguagesToDatabaseString(
                    language,
                    secondaryLanguage
                )
        }
        insert(KEY_AUDIO_LANG, preferredAudioLanguages)
    }

    override fun setSecondaryAudioLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setSecondaryAudioLanguage(language, type)
        var preferredAudioLanguages: String? = ""
        var primaryLanguage = getPrimaryAudioLanguage()
        preferredAudioLanguages =
            preferredLanguagesToDatabaseString(primaryLanguage, language)
        insert(KEY_AUDIO_LANG, preferredAudioLanguages)
    }

    override fun setPrimarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setPrimarySubtitleLanguage(language, type)
        var preferredSubtitleLanguages: String? = ""
        var secondaryLanguage = getSecondarySubtitleLanguage()
        if (secondaryLanguage == null || secondaryLanguage == "") {
            preferredSubtitleLanguages = language
        } else {
            preferredSubtitleLanguages = preferredLanguagesToDatabaseString(
                language,
                secondaryLanguage
            )
        }
        insert(KEY_SUBTITLE_LANG, preferredSubtitleLanguages)
    }

    override fun setSecondarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setSecondarySubtitleLanguage(language, type)
        var preferredSubtitleLanguages: String? = ""
        var primaryLanguage = getPrimarySubtitleLanguage()
        preferredSubtitleLanguages =
            preferredLanguagesToDatabaseString(primaryLanguage, language)
        insert(KEY_SUBTITLE_LANG, preferredSubtitleLanguages)
        if (getSubtitlesEnabled()) {
            //(ReferenceSdk.playerHandler as ReferencePlayerHandler).updateSubtitleLanguage(
            //    language
            //)
        }
    }

    override fun setAudioType(position: Int) {
        var value: String? = ""
        if (position == 0) {
            value = "normal"
        }
        if (position == 1) {
            value = "supplementary"
        }
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Set audio type: " + value)
        insert(KEY_AUDIO_TYPE, value)
    }

    override fun getAudioType(callback: IAsyncDataCallback<Int>?) {
        var position: Int = 0
        if (read(KEY_AUDIO_TYPE) == "normal") {
            position = 0
        }
        if (read(KEY_AUDIO_TYPE) == "supplementary") {
            position = 1
        }
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Get audio type: " + position)
        callback?.onReceive(position)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setSubtitlesEnabled(isEnabled: Boolean, updateSwitch: Boolean) {
        var value: String? = ""
        if (isEnabled) {
            value = "true"
        } else {
            value = "false"
        }
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Set subtitles enabled: " + value)
        update(KEY_SUBTITLE_ENABLED, value)
        if (updateSwitch)
            setSubtitleSwitch(0)
    }


    private fun getSubtitlesEnabled(): Boolean {
        var value: Boolean = false
        value = read(KEY_SUBTITLE_ENABLED) == "true"
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Is subtitles enabled: " + value)
        return value
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
        var value: String? = ""
        if (position == 0) {
            value = "normal"
        }
        if (position == 1) {
            value = "hard_of_hearing"
        }
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Set subtitles type: " + value)
        insert(KEY_SUBTITLE_TYPE, value)
        if (updateSwitch)
            setSubtitleSwitch(position + 1)
    }

    override fun getSubtitleTypeDisplayNames(type: PrefType): MutableList<String> {
        if (type == PrefType.BASE) return super.getSubtitleTypeDisplayNames(type)
        return mutableListOf(
            getStringValue("audio_type_normal"),
            getStringValue("subtitle_type_hearing_impaired"))
    }

    override fun getSubtitlesType(): Int {
        var position: Int = 0
        if (read(KEY_SUBTITLE_TYPE) == "normal") {
            position = 0
        }
        if (read(KEY_SUBTITLE_TYPE) == "hard_of_hearing") {
            position = 1
        }
        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Get subtitles type: " + position)
        return position
    }

    override fun setDeafaultLanguages() {
        var defaultLanguages = preferredLanguagesToDatabaseString(
            languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country")),
            "eng"
        )
        insert(KEY_AUDIO_LANG, defaultLanguages)
        insert(KEY_SUBTITLE_LANG, defaultLanguages)
    }

    override fun setAspectRatio(index: Int) {
        var value = CBS_ASPECT_RATIO_16X9_PILLAR_BOX
        when(index) {
            PREF_STRETCH-> value = CBS_STRETCH
            PREF_PAN_SCAN_4_3-> value = CBS_ASPECT_RATIO_4X3_PAN_AND_SCAN
            PREF_LETTER_BOX_4_3-> value = CBS_ASPECT_RATIO_4X3_LETTER_BOX
            PREF_PAN_SCAN_16_9-> value = CBS_ASPECT_RATIO_16X9_ZOOM
            PREF_PILLAR_BOX_16_9-> value = CBS_ASPECT_RATIO_16X9_PILLAR_BOX
        }
        insert(KEY_ASPECT_RATIO, value)
    }

    override fun getAspectRatio(): Int {
        var value: String? = read(KEY_ASPECT_RATIO) ?: return PREF_PILLAR_BOX_16_9
        var prefValue = PREF_PILLAR_BOX_16_9
        when(value) {
            CBS_STRETCH -> prefValue = PREF_STRETCH
            CBS_ASPECT_RATIO_16X9_PILLAR_BOX -> prefValue = PREF_PILLAR_BOX_16_9
            CBS_ASPECT_RATIO_16X9_ZOOM -> prefValue = PREF_PAN_SCAN_16_9
            CBS_ASPECT_RATIO_4X3_LETTER_BOX -> prefValue = PREF_LETTER_BOX_4_3
            CBS_ASPECT_RATIO_4X3_PAN_AND_SCAN -> prefValue = PREF_PAN_SCAN_4_3
        }
        return prefValue
    }

    override fun setAntennaPower(enable: Boolean) {
        var value = "false"

        if(enable) {
            value = "true"
        }
        insert(KEY_ANTENNA_POWER_ENABLED, value)
    }

    override fun isAntennaPowerEnabled(): Boolean {
        var isEnabled: String? = read(KEY_ANTENNA_POWER_ENABLED) ?: return false
        if(isEnabled == "true") {
            return true
        }
        return false
    }

}