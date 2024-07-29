import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.BuildConfig
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.ReferenceSdk.context
import com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider.Companion.OEM_CUSTOMIZATION_URI
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.content_provider.ReferenceContract.OemCustomization.Companion.SUBTITLE_COLUMN_ID
import com.iwedia.cltv.sdk.entities.LanguageCodeMapper
import com.iwedia.cltv.sdk.handlers.ReferencePlayerHandler

class TvConfigurationHelper {

    interface FormattingProgressListener {
        fun reportProgress(visible: Boolean, statusText: String);
    }

    companion object {
        var primaryAudioLang: String = ""
        var secondaryAudioLang: String = ""
        var primarySubtitlesLang: String = ""
        var secondarySubtitlesLang: String = ""

        val AUTHORITY = "com.google.android.tv.dtvprovider"
        val LANGUAGE_SETTINGS_PATH = "tvsettings"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$LANGUAGE_SETTINGS_PATH");

        const val KEY_AUDIO_TYPE = "media.audio.preferredtype"
        const val KEY_AUDIO_LANG = "media.audio.preferredlanguages"
        const val KEY_AUDIO_DESC_VOL = "media.audiodescription.volume"

        const val KEY_SUBTITLE_ENABLED = "media.subtitles.enabled"
        const val KEY_SUBTITLE_TYPE = "media.subtitles.preferredtype"
        const val KEY_SUBTITLE_LANG = "media.subtitles.preferredlanguages"
        const val CODEC_AUDIO_AC3 = "ac3"
        const val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
        const val CODEC_AUDIO_EAC3 = "eac3"
        const val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
        const val CODEC_AUDIO_DTS = "dts"
        const val KEY_ANTENNA_POWER_ENABLED = "lna.enabled"

        private const val PREF_STRETCH = 0
        private const val PREF_PAN_SCAN_4_3 = 1
        private const val PREF_LETTER_BOX_4_3 = 2
        private const val PREF_PAN_SCAN_16_9 = 3
        private const val PREF_PILLAR_BOX_16_9 = 4

        private const val CBS_STRETCH = "stretch"
        private const val CBS_ASPECT_RATIO_4X3_LETTER_BOX = "4x3_letter_box"
        private const val CBS_ASPECT_RATIO_4X3_PAN_AND_SCAN = "4x3_pan_and_scan"
        private const val CBS_ASPECT_RATIO_16X9_PILLAR_BOX = "16x9_pillar_box"
        private const val CBS_ASPECT_RATIO_16X9_ZOOM = "16x9_zoom"

        private const val KEY_ASPECT_RATIO = "media.video.aspectratio"

        /**
         * record key
         */
        const val KEY = "_id"

        /**
         * record value
         */
        const val VALUE = "value"

        fun getPrimaryAudioLanguage(): String? {
            var primaryAudioLanguage: String?
            val preferredLanguage: String? = read(KEY_AUDIO_LANG)
            if (preferredLanguage != null) {
                if (preferredLanguage.contains(";")) {  // Suppose primary and secondary subtitle languages
                    var codes = arrayOf<String>()
                    codes = preferredLanguage.split(";").toTypedArray()
                    primaryAudioLanguage = codes[0];
                    if(primaryAudioLanguage=="null")
                    primaryAudioLanguage =
                        LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")]

                } else {

                    primaryAudioLanguage = preferredLanguage;

                }
            } else {
                primaryAudioLanguage =
                    LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")/*System.getProperty("persist.vendor.iwedia.scan_country")*/]
                Log.d(Constants.LogTag.CLTV_TAG + 
                    "TAG",
                    "primaryLanguage: " + primaryAudioLanguage + "or" + LanguageCodeMapper.countryCodeToLanguageCodeMap["SRB"]
                )

            }
            return primaryAudioLanguage
        }

        fun getVideoResolution(): String?{
            return ""
        }

        fun getSecondaryAudioLanguage(): String? {
            var secondaryAudioLanguage: String?
            val preferredLanguage: String? = read(KEY_AUDIO_LANG)
            if (preferredLanguage != null) {
                if (preferredLanguage.contains(";")) {  // Suppose primary and secondary subtitle languages
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

        fun getPrimarySubtitleLanguage(): String? {
            var primarySubtitleLanguage: String?
            val preferredLanguage: String? = read(KEY_SUBTITLE_LANG)
            if (preferredLanguage != null) {
                if (preferredLanguage.contains(";")) {  // Suppose primary and secondary subtitle languages
                    var codes = arrayOf<String>()
                    codes = preferredLanguage.split(";").toTypedArray()
                    primarySubtitleLanguage = codes[0]
                    if(primarySubtitleLanguage=="null")
                        primarySubtitleLanguage =
                            LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")]

                } else {
                    primarySubtitleLanguage = preferredLanguage
                }
            } else {
                primarySubtitleLanguage =
                    LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")]
                Log.d(Constants.LogTag.CLTV_TAG + 
                    "TAG",
                    "persist.vendor.iwedia.scan_country:" + System.getProperty("persist.vendor.iwedia.scan_country")
                )
                Log.d(Constants.LogTag.CLTV_TAG + 
                    "TAG",
                    "countrycodetolanguagecode: " + LanguageCodeMapper.countryCodeToLanguageCodeMap["SRB"]
                )
            }
            return primarySubtitleLanguage
        }

        fun getSecondarySubtitleLanguage(): String? {
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

        @SuppressLint("Range")
        fun read(key: String): String? {
            var value: String? = null
            val projection = arrayOf<String>(KEY, VALUE)
            val cursor: Cursor? = ReferenceSdk.context.contentResolver.query(
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

        @SuppressLint("Range")
        fun readInternalProviderData(key: String): String? {
            val contentResolver: ContentResolver = ReferenceSdk.context.contentResolver
            var cursor = contentResolver.query(
                ReferenceContract.buildConfigUri(1),
                null,
                null,
                null,
                null
            )
            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()
                if (cursor.getString(cursor.getColumnIndex(ReferenceContract.Config.CURRENT_COUNTRY_COLUMN)) != null) {
                    val country =
                        cursor.getString(cursor.getColumnIndex(ReferenceContract.Config.CURRENT_COUNTRY_COLUMN))
                            .toString()
                    return country
                }
            }
            return null
        }

        fun update(key: String?, value: String?) {
            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Update " + key + " : " + value)
            try {
                val contentValues = ContentValues()
                val uri = Uri.withAppendedPath(CONTENT_URI, key)
                contentValues.put(VALUE, value)

                val count: Int = ReferenceSdk.context.contentResolver.update(
                    uri,
                    contentValues,
                    null,
                    null
                )
                if (count == 0) {
                    // insert this new {key, value} in table
                    contentValues.put(KEY, key)
                    ReferenceSdk.context.contentResolver.insert(
                        CONTENT_URI,
                        contentValues
                    )
                }
            } catch (e: Exception) {
                Log.w("AudioSubtitle", e.message!!)
            }
        }

        fun insert(key: String?, value: String?) {
            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Insert " + key + " : " + value)
            try {
                val contentValues = ContentValues()
                contentValues.put(VALUE, value)

                // insert this new {key, value} in table
                contentValues.put(KEY, key)
                ReferenceSdk.context.contentResolver.insert(
                    CONTENT_URI,
                    contentValues
                )
            } catch (e: Exception) {
                Log.w("AudioSubtitle", e.message!!)
            }
        }

        fun preferredLanguagesToDatabaseString(primary: String?, secondary: String?): String? {
            var databaseString: String?
            databaseString = primary + ";" + secondary
            return databaseString
        }

        fun setPrimaryAudioLanguage(language: String) {
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

        fun setSecondaryAudioLanguage(language: String) {
            var preferredAudioLanguages: String? = ""
            var primaryLanguage = getPrimaryAudioLanguage()
            preferredAudioLanguages =
                preferredLanguagesToDatabaseString(primaryLanguage, language)
            insert(KEY_AUDIO_LANG, preferredAudioLanguages)

        }

        fun setPrimarySubtitleLanguage(language: String) {
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

        fun setSecondarySubtitleLanguage(language: String) {
            var preferredSubtitleLanguages: String? = ""
            var primaryLanguage = getPrimarySubtitleLanguage()
            preferredSubtitleLanguages =
                preferredLanguagesToDatabaseString(primaryLanguage, language)
            insert(KEY_SUBTITLE_LANG, preferredSubtitleLanguages)
            if (getSubtitlesEnabled()) {
                (ReferenceSdk.playerHandler as ReferencePlayerHandler).updateSubtitleLanguage(
                    language
                )
            }
        }

        fun enableAudioDescription(enable: Boolean) {
        }

        fun enableHardOfHearing(enable: Boolean) {
        }

        //DRAZEN MTK?
        fun updateAudioTracks() {
            if (primaryAudioLang != "") {
                setPrimaryAudioLanguage(primaryAudioLang)
            }
            if (secondaryAudioLang != "") {
                setSecondaryAudioLanguage(secondaryAudioLang)
            }
        }

        //DRAZEN MTK?
        fun updateSubtitleTracks() {
            if (primarySubtitlesLang != "") {
                setPrimarySubtitleLanguage(primarySubtitlesLang)
            }
            if (secondarySubtitlesLang != "") {
                setSecondarySubtitleLanguage(secondarySubtitlesLang)
            }
        }

        fun getAudioDescriptionState(): Boolean {
            return false
        }

        fun getHardOfHearingState(): Boolean {
            return false
        }

        fun enableSubtitles(enable: Boolean) {

        }

        fun getSubtitlesState(): Boolean {
            return true
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun isDolby(tvTrackInfo: TvTrackInfo): Boolean {
            Log.i("TVConfigurationDolby", "isDolby:${tvTrackInfo.encoding} ")
            if (tvTrackInfo.encoding == CODEC_AUDIO_AC3 || tvTrackInfo.encoding == CODEC_AUDIO_AC3_ATSC || tvTrackInfo.encoding == CODEC_AUDIO_EAC3 || tvTrackInfo.encoding == CODEC_AUDIO_EAC3_ATSC || tvTrackInfo.encoding == CODEC_AUDIO_DTS) {
                return true
            } else {
                return false
            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun getDolby(tvTrackInfo: TvTrackInfo): String {
            if (tvTrackInfo.encoding == CODEC_AUDIO_AC3 || tvTrackInfo.encoding == CODEC_AUDIO_AC3_ATSC)
                return "AC3"
            if (tvTrackInfo.encoding == CODEC_AUDIO_EAC3 || tvTrackInfo.encoding == CODEC_AUDIO_EAC3_ATSC)
                return "EAC3"
            if (tvTrackInfo.encoding == CODEC_AUDIO_DTS)
                return "DTS"
            return ""
        }


        fun setAudioType(position: Int) {
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

        fun getAudioType(): Int {
            var position: Int = 0
            if (read(KEY_AUDIO_TYPE) == "normal") {
                position = 0
            }
            if (read(KEY_AUDIO_TYPE) == "supplementary") {
                position = 1
            }
            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Get audio type: " + position)
            return position
        }

        fun setSubtitlesEnabled(isEnabled: Boolean, updateSwitch:Boolean = true) {
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

        fun getSubtitlesEnabled(): Boolean {
            var value: Boolean = false
            value = read(KEY_SUBTITLE_ENABLED) == "true"
            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Is subtitles enabled: " + value)
            return value
        }

        fun setSubtitlesType(position: Int, updateSwitch: Boolean = true) {
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

        fun getSubtitlesType(): Int {
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

        @RequiresApi(Build.VERSION_CODES.R)
        private fun setSubtitleSwitch(position: Int) {
            val contentResolver: ContentResolver = context.contentResolver
            var cv = ContentValues()
            cv.put(SUBTITLE_COLUMN_ID, position)
            contentResolver.update(OEM_CUSTOMIZATION_URI, cv, null)
        }

        fun setDeafaultLanguages() {
            var defaultLanguages = preferredLanguagesToDatabaseString(
                LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")],
                "eng"
            )
            insert(KEY_AUDIO_LANG, defaultLanguages)
            insert(KEY_SUBTITLE_LANG, defaultLanguages)
        }

        fun setAntennaPower(enable: Boolean) {
            var value = "false"

            if(enable) {
                value = "true"
            }
            insert(KEY_ANTENNA_POWER_ENABLED, value)
        }

        fun isAntennaPowerEnabled() : Boolean {
            var isEnabled: String? = read(KEY_ANTENNA_POWER_ENABLED) ?: return false
            if(isEnabled == "true") {
                return true
            }
            return false
        }

        //not used for Gretzky
        fun registerProgressListener(listener: FormattingProgressListener) {
        }

        @Synchronized
        fun enableTimeshift(enable: Boolean, context: Context) {

        }

        fun attachUSB() {

        }

        fun formatUSB() {

        }

        fun setTeletextDigitalLanguage(position: Int) {

        }

        fun getTeletextDigitalLanguage(): Int {
            return -1
        }

        fun setTeletextDecodeLanguage(position: Int) {

        }

        fun getTeletextDecodeLanguage(): Int {
            return -1
        }

        fun setAspectRatio(index : Int) {
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

        fun getAspectRatio() : Int {
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
        @RequiresApi(Build.VERSION_CODES.R)
        fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean{
            return tvTrackInfo!!.isAudioDescription
        }

        fun isGretzkyBoard(): Boolean =
             BuildConfig.FLAVOR.contains("gretzky") || BuildConfig.FLAVOR.contains("zte")

    }
}