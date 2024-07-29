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
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.LanguageCodeMapper

class TvConfigurationHelper {

    interface FormattingProgressListener {
        fun reportProgress(visible: Boolean, statusText : String);
    }

    companion object {
        var primaryAudioLang : String = ""
        var secondaryAudioLang : String = ""
        var primarySubtitlesLang : String = ""
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
                } else {
                    primaryAudioLanguage = preferredLanguage;
                }
            } else {
                primaryAudioLanguage = LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")/*System.getProperty("persist.vendor.iwedia.scan_country")*/]
                Log.d(Constants.LogTag.CLTV_TAG + "TAG","primaryLanguage: " + primaryAudioLanguage + "or" + LanguageCodeMapper.countryCodeToLanguageCodeMap["SRB"] )

            }
            return primaryAudioLanguage
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
                } else {
                    primarySubtitleLanguage = preferredLanguage
                }
            } else {
                primarySubtitleLanguage = LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")]
                Log.d(Constants.LogTag.CLTV_TAG + "TAG", "persist.vendor.iwedia.scan_country:" + System.getProperty("persist.vendor.iwedia.scan_country"))
                Log.d(Constants.LogTag.CLTV_TAG + "TAG", "countrycodetolanguagecode: " + LanguageCodeMapper.countryCodeToLanguageCodeMap["SRB"])
            }
            return primarySubtitleLanguage
        }

        fun getVideoResolution(): String?{
            return ""
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
                    val country = cursor.getString(cursor.getColumnIndex(ReferenceContract.Config.CURRENT_COUNTRY_COLUMN)).toString()
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

        }

        fun enableAudioDescription(enable: Boolean) {
        }

        fun enableHardOfHearing(enable: Boolean) {
        }

        //DRAZEN MTK?
        fun updateAudioTracks() {
            if(primaryAudioLang != "") {
                setPrimaryAudioLanguage(primaryAudioLang)
            }
            if(secondaryAudioLang != "") {
                setSecondaryAudioLanguage(secondaryAudioLang)
            }
        }

        //DRAZEN MTK?
        fun updateSubtitleTracks() {
            if(primarySubtitlesLang !=  "") {
                setPrimarySubtitleLanguage(primarySubtitlesLang)
            }
            if(secondarySubtitlesLang != "") {
                setSecondarySubtitleLanguage(secondarySubtitlesLang)
            }
        }

        fun getAudioDescriptionState() : Boolean {
            return false
        }

        fun getHardOfHearingState() : Boolean {
            return false
        }

        fun enableSubtitles(enable: Boolean) {

        }

        fun getSubtitlesState() : Boolean {
            return true
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun isDolby(tvTrackInfo: TvTrackInfo): Boolean{
            Log.i("TVConfigurationDolby", "isDolby:${tvTrackInfo.encoding} ")
            if(tvTrackInfo.encoding == CODEC_AUDIO_AC3 || tvTrackInfo.encoding == CODEC_AUDIO_AC3_ATSC||tvTrackInfo.encoding == CODEC_AUDIO_EAC3||tvTrackInfo.encoding == CODEC_AUDIO_EAC3_ATSC||tvTrackInfo.encoding == CODEC_AUDIO_DTS){
                return true
            }
            else {
                return false
            }
        }
        @RequiresApi(Build.VERSION_CODES.R)
        fun getDolby(tvTrackInfo: TvTrackInfo):String{
            if(tvTrackInfo.encoding == CODEC_AUDIO_AC3 || tvTrackInfo.encoding == CODEC_AUDIO_AC3_ATSC)
                return "AC3"
            if(tvTrackInfo.encoding == CODEC_AUDIO_EAC3||tvTrackInfo.encoding == CODEC_AUDIO_EAC3_ATSC)
                return "EAC3"
            if(tvTrackInfo.encoding == CODEC_AUDIO_DTS)
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

        fun setSubtitlesEnabled(isEnabled: Boolean) {
            var value: String? = ""
            if (isEnabled) {
                value = "true"
            } else {
                value = "false"
            }
            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Set subtitles enabled: " + value)
            insert(KEY_SUBTITLE_ENABLED, value)
        }

        fun getSubtitlesEnabled(): Boolean {
            var value: Boolean = false
            value = read(KEY_SUBTITLE_ENABLED) == "true"
            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Is subtitles enabled: " + value)
            return value
        }

        fun setSubtitlesType(position: Int) {
            var value: String? = ""
            if (position == 0) {
                value = "normal"
            }
            if (position == 1) {
                value = "hard_of_hearing"
            }
            Log.d(Constants.LogTag.CLTV_TAG + "TAG", "Set subtitles type: " + value)
            insert(KEY_SUBTITLE_TYPE, value)
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

        fun setDeafaultLanguages() {
            var defaultLanguages = preferredLanguagesToDatabaseString(LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")], "eng")
            insert(KEY_AUDIO_LANG, defaultLanguages)
            insert(KEY_SUBTITLE_LANG, defaultLanguages)
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

        fun setAspectRatio(index : Int) {
        }

        fun getAspectRatio() : Int {
            return 0
        }

        fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean{
            return tvTrackInfo!!.isAudioDescription
        }
    }
}