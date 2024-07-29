package com.iwedia.cltv.platform.base.content_provider

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class OemCustomizationProvider constructor(private var context: Context, private var jsonPath: String) {

    companion object {
        const val TAG : String = "ReferenceBootupReceiver"
        const val AUTHORITY = "com.iwedia.cltv.sdk.content_provider.ReferenceContentProvider"
        const val OEM_CUSTOMIZATION_TABLE = "oem_customization"
        val OEM_CUSTOMIZATION_URI : Uri = Uri.parse("content://${AUTHORITY}/${OEM_CUSTOMIZATION_TABLE}")
        const val LANGUAGES_TABLE = "languages"
        val LANGUAGES_URI : Uri = Uri.parse("content://${AUTHORITY}/${LANGUAGES_TABLE}")
        val SUBTITLE_COLUMN_ID = "subtitle"
        const val CONFIGURABLE_KEYS_TABLE = "configurable_keys"
        val CONFIGURABLE_KEYS_URI : Uri = Uri.parse("content://${AUTHORITY}/${CONFIGURABLE_KEYS_TABLE}")
        const val SYSTEM_INFO = "system_info"
        const val OEM = "oem"
        val SYSTEM_INFO_URI : Uri = Uri.parse("content://${AUTHORITY}/${SYSTEM_INFO}")

        const val PVR_ENABLED_COLUMN = "pvr_enabled"
        const val VIRTUAL_RCU_COLUMN = "virtual_rcu"
        const val VERTICAL_EPG_COLUMN = "vertical_epg"
        const val THIRD_PARTY_INPUT_ENABLED_COLUMN = "third_party_input_enabled"
        const val TERRESTRIAL_SCAN_ENABLED_COLUMN = "terrestrial_scan_enabled"
        const val CABLE_SCAN_ENABLED_COLUMN = "cable_scan_enabled"
        const val SATELLITE_SCAN_ENABLED_COLUMN = "satellite_scan_enabled"
        const val CHANNEL_LOGO_METADATA_ENABLED_COLUMN = "channel_logo_metadata_enabled"
        const val SCAN_TYPE = "scan_type"
        const val SWAP_CHANNEL = "swap_channel"
        const val SCAN_TYPE_SWITCH = "scan_type_switch"
        const val COUNTRY_SELECT = "country_select"
        const val SIGNAL_STATUS_MONITORING_ENABLED = "signal_status_monitoring_enabled"
        const val LICENCE_KEY = "licence_key"
        const val CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN = "clear_channels_scan_option_enabled"
        const val ASPECT_RATIO_ENABLED_COLUMN = "aspect_ratio_enabled"
        const val TELETEXT_ENABLE_COLUMN = "teletext_enable_column"
        const val DELETE_THIRD_PARTY_INPUT_COLUMN = "delete_third_party_input_enabled"
        const val TIMESHIFT_ENABLED_COLUMN = "timeshift_enabled"


        /**
         * Colors
         */
        const val COLOR_BACKGROUND_COLUMN = "color_background"
        const val COLOR_NOT_SELECTED_COLUMN = "color_not_selected"
        const val COLOR_TEXT_DESCRIPTION_COLUMN = "color_text_description"
        const val COLOR_MAIN_TEXT_COLUMN = "color_main_text"
        const val COLOR_SELECTOR_COLUMN = "color_selector"
        const val COLOR_PROGRESS_COLUMN = "color_progress"
        const val COLOR_PVR_AND_OTHER_COLUMN = "color_pvr_and_other"
        /**
         * Branding columns
         */
        const val BRANDING_CHANNEL_LOGO_COLUMN = "branding_channel_logo"
        const val BRANDING_COMPANY_NAME_COLUMN = "branding_company_name"
        const val BRANDING_WELCOME_MESSAGE_COLUMN = "branding_welcome_message"
        /**
         * Fonts
         */
        const val FONT_REGULAR = "font_regular"
        const val FONT_MEDIUM = "font_medium"
        const val FONT_BOLD = "font_bold"
        const val FONT_LIGHT = "font_light"

        /**
         * Languages
         */
        const val LANGUAGE_CODE_COLUMN = "language_code"
        const val LANGUAGE_CONTENT_COLUMN = "language_content"

        /**
         * Configurable keys
         */
        const  val CONFIGURABLE_KEY_NAME_COLUMN = "key_name"
        const  val CONFIGURABLE_KEY_ACTION_TYPE_COLUMN = "action_type"
        const  val CONFIGURABLE_KEY_DESCRIPTION_COLUMN = "description"

        /**
         * System info
         */
        const  val RF_CHANNEL_NUMBER_ENABLED_COLUMN = "rf_channel_number_enabled"
        const  val BER_ENABLED_COLUMN = "ber_enabled"
        const  val FREQUENCY_ENABLED_COLUMN = "frequency_enabled"
        const  val PROG_ENABLED_COLUMN = "prog_enabled"
        const  val UEC_ENABLED_COLUMN = "uec_enabled"
        const  val SERVICEID_ENABLED_COLUMN = "serviceId_enabled"
        const  val POSTVITERBI_ENABLED_COLUMN = "postViterbi_enabled"
        const  val TSID_ENABLED_COLUMN = "tsId_enabled"
        const  val FIVES_ENABLED_COLUMN = "fiveS_enabled"
        const  val ONID_ENABLED_COLUMN = "onId_enabled"
        const  val AGC_ENABLED_COLUMN = "agc_enabled"
        const  val NETWORKID_ENABLED_COLUMN = "networkId_enabled"
        const  val NETWORKNAME_ENABLED_COLUMN = "networkName_enabled"
        const  val BANDWIDTH_ENABLED_COLUMN = "bandwidth_enabled"

        var scanType : String ? = null
        var pvrField : Int ? = null
        var timeshiftField : Int ? = null

    }

    init {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ intent received ")
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"jsonPath in oem is $jsonPath")
        scanType = getScanType(context)
        pvrField = checkIfFieldExists(context, PVR_ENABLED_COLUMN)
        timeshiftField = checkIfFieldExists(context, TIMESHIFT_ENABLED_COLUMN)
        CoroutineHelper.runCoroutine({
            initOEMCustomization(context)
//        initLanguagesData(context)
            initConfigurableKeys(context)
            initSystemInfoData(context)
        })
    }

    @SuppressLint("Range")
    private fun getScanType(context: Context) : String {
        val cursor: Cursor? = context.contentResolver.query(OEM_CUSTOMIZATION_URI, null, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val retValue = cursor.getString(cursor.getColumnIndex("scan_type"))
                cursor.close()
                return retValue
            }
        }
        return "free"
    }

    private fun initOEMCustomization(context: Context) {
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.delete(OEM_CUSTOMIZATION_URI, null, null)

        var cv = ContentValues()
//        cv.put(COLOR_BACKGROUND_COLUMN, readFromJson(COLOR_BACKGROUND_COLUMN, OEM))
//        cv.put(COLOR_NOT_SELECTED_COLUMN, readFromJson(COLOR_NOT_SELECTED_COLUMN, OEM))
//        cv.put(COLOR_TEXT_DESCRIPTION_COLUMN, readFromJson(COLOR_TEXT_DESCRIPTION_COLUMN, OEM))
//        cv.put(COLOR_MAIN_TEXT_COLUMN, readFromJson(COLOR_MAIN_TEXT_COLUMN, OEM))
//        cv.put(COLOR_SELECTOR_COLUMN, readFromJson(COLOR_SELECTOR_COLUMN, OEM))
//        cv.put(COLOR_PROGRESS_COLUMN, readFromJson(COLOR_PROGRESS_COLUMN, OEM))
//        cv.put(COLOR_PVR_AND_OTHER_COLUMN, readFromJson(COLOR_PVR_AND_OTHER_COLUMN, OEM))
//        cv.put(FONT_REGULAR, readFromJson(FONT_REGULAR, OEM))
//        cv.put(FONT_MEDIUM, readFromJson(FONT_MEDIUM, OEM))
//        cv.put(FONT_BOLD, readFromJson(FONT_BOLD, OEM))
//        cv.put(FONT_LIGHT, readFromJson(FONT_LIGHT, OEM))
//        cv.put(BRANDING_WELCOME_MESSAGE_COLUMN, readFromJson(BRANDING_WELCOME_MESSAGE_COLUMN, OEM))
        // Custom company logo example
//        cv.put(BRANDING_CHANNEL_LOGO_COLUMN, "//upload.wikimedia.org/wikipedia/en/thumb/4/42/RT-RK_logo.svg/1200px-RT-RK_logo.svg.png".toByteArray())
        // Transparent company logo
//        cv.put(BRANDING_CHANNEL_LOGO_COLUMN, createCompanyLogo(context))
        // Custom company name
        //cv.put(BRANDING_COMPANY_NAME_COLUMN, "RT-RK")
//        cv.put(BRANDING_COMPANY_NAME_COLUMN, readFromJson(BRANDING_COMPANY_NAME_COLUMN, OEM))

//        cv.put(VERTICAL_EPG_COLUMN, readFromJson(VERTICAL_EPG_COLUMN, OEM).toInt())
//        cv.put(VIRTUAL_RCU_COLUMN, readFromJson(VIRTUAL_RCU_COLUMN, OEM).toInt())

//        cv.put(PVR_ENABLED_COLUMN, pvrField)

//        cv.put(CHANNEL_LOGO_METADATA_ENABLED_COLUMN, readFromJson(CHANNEL_LOGO_METADATA_ENABLED_COLUMN, OEM).toInt())
//        cv.put(SUBTITLE_COLUMN_ID, readFromJson(SUBTITLE_COLUMN_ID, OEM).toInt()) // 0-off, 1-normal, 2-Hearing Impaired
//        cv.put(SIGNAL_STATUS_MONITORING_ENABLED, readFromJson(SIGNAL_STATUS_MONITORING_ENABLED, OEM).toInt())
        cv.put(LICENCE_KEY, readFromJson(LICENCE_KEY, OEM))

//        cv.put(ASPECT_RATIO_ENABLED_COLUMN, readFromJson(ASPECT_RATIO_ENABLED_COLUMN, OEM).toInt())
//        cv.put(TELETEXT_ENABLE_COLUMN, readFromJson(TELETEXT_ENABLE_COLUMN, OEM).toInt())

        cv.put(THIRD_PARTY_INPUT_ENABLED_COLUMN, readFromJson(THIRD_PARTY_INPUT_ENABLED_COLUMN, OEM).toInt())
        cv.put(TERRESTRIAL_SCAN_ENABLED_COLUMN, readFromJson(TERRESTRIAL_SCAN_ENABLED_COLUMN, OEM).toInt())
        cv.put(CABLE_SCAN_ENABLED_COLUMN, readFromJson(CABLE_SCAN_ENABLED_COLUMN, OEM).toInt())
        cv.put(SATELLITE_SCAN_ENABLED_COLUMN, readFromJson(SATELLITE_SCAN_ENABLED_COLUMN, OEM).toInt())
        cv.put(SCAN_TYPE, scanType)
        cv.put(SWAP_CHANNEL, readFromJson(SWAP_CHANNEL, OEM).toInt())
        cv.put(SCAN_TYPE_SWITCH, readFromJson(SCAN_TYPE_SWITCH, OEM).toInt())
        cv.put(COUNTRY_SELECT, readFromJson(COUNTRY_SELECT, OEM))
        cv.put(CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN, readFromJson(CLEAR_CHANNELS_SCAN_OPTION_ENABLED_COLUMN, OEM).toInt())
        cv.put(DELETE_THIRD_PARTY_INPUT_COLUMN, readFromJson(DELETE_THIRD_PARTY_INPUT_COLUMN, OEM).toInt())
        contentResolver.insert(OEM_CUSTOMIZATION_URI, cv)
    }

    private fun readFromJson(name: String, table: String): String {
        val iS = context.classLoader.getResourceAsStream(jsonPath)
        val buffer = ByteArray(iS.available())
        iS.read(buffer)
        val oS = ByteArrayOutputStream()
        oS.write(buffer)
        oS.close()
        iS.close()
        val json = JSONObject(String(buffer))
        val scanArray: JSONArray = json.getJSONArray(table)
        var returnString: String = ""
        for (i in 0 until scanArray.length()) {
            val scanObject: JSONObject = scanArray.getJSONObject(i)
            returnString = scanObject.getString(name)
        }
        return returnString
    }

    @SuppressLint("Range", "Recycle")
    private fun checkIfFieldExists(context: Context, field: String): Int? {
        val cursor: Cursor? = context.contentResolver.query(OEM_CUSTOMIZATION_URI, null, null, null, null)
        try {
            if (cursor!!.count > 0) {
                cursor.moveToFirst()
                val retValue = cursor.getInt(cursor.getColumnIndex(field))
                cursor.close()
                return retValue
            }
        }catch (E : Exception){
            E.printStackTrace()
        }
        return 1
    }

    @SuppressLint("UseCompatLoadingForDrawables", "ResourceType")
    private fun createCompanyLogo(context: Context): ByteArray {
        val bitM: Bitmap = BitmapFactory.decodeStream(context.classLoader.getResourceAsStream("assets/company_logo.png"))
        var bos = ByteArrayOutputStream()
        bitM.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return bos.toByteArray()
    }


    private fun initSystemInfoData(context: Context) {
        val contentResolver: ContentResolver = context.contentResolver
        try {
            contentResolver.delete(SYSTEM_INFO_URI, null, null)
        } catch (e: Exception) {}
        var cv = ContentValues()
        cv.put(RF_CHANNEL_NUMBER_ENABLED_COLUMN, readFromJson(RF_CHANNEL_NUMBER_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(BER_ENABLED_COLUMN, readFromJson(BER_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(FREQUENCY_ENABLED_COLUMN, readFromJson(FREQUENCY_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(PROG_ENABLED_COLUMN, readFromJson(PROG_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(UEC_ENABLED_COLUMN, readFromJson(UEC_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(SERVICEID_ENABLED_COLUMN, readFromJson(SERVICEID_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(POSTVITERBI_ENABLED_COLUMN, readFromJson(POSTVITERBI_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(TSID_ENABLED_COLUMN, readFromJson(TSID_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(FIVES_ENABLED_COLUMN, readFromJson(FIVES_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(ONID_ENABLED_COLUMN, readFromJson(ONID_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(AGC_ENABLED_COLUMN, readFromJson(AGC_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(NETWORKID_ENABLED_COLUMN, readFromJson(NETWORKID_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(NETWORKNAME_ENABLED_COLUMN, readFromJson(NETWORKNAME_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        cv.put(BANDWIDTH_ENABLED_COLUMN, readFromJson(BANDWIDTH_ENABLED_COLUMN, SYSTEM_INFO).toInt())
        contentResolver.insert(SYSTEM_INFO_URI, cv)
    }

    private fun initLanguagesData(context: Context) {
        val contentResolver: ContentResolver = context.contentResolver
        try {
            contentResolver.delete(LANGUAGES_URI, null, null)
        } catch (e: Exception) {}
        val languages = LanguagesProvider(context)
        inputLanguage("Default", languages, contentResolver)
        inputLanguage("EN", languages, contentResolver)
        inputLanguage("IT", languages, contentResolver)
        inputLanguage("SR_RS", languages, contentResolver)
        inputLanguage("AF", languages, contentResolver)
        inputLanguage("AM", languages, contentResolver)
        inputLanguage("AR", languages, contentResolver)
        inputLanguage("AZ", languages, contentResolver)
        inputLanguage("BE", languages, contentResolver)
        inputLanguage("BG", languages, contentResolver)
        inputLanguage("BN", languages, contentResolver)
        inputLanguage("BS", languages, contentResolver)
        inputLanguage("CA", languages, contentResolver)
        inputLanguage("CS_CZ", languages, contentResolver)
        inputLanguage("DA", languages, contentResolver)
        inputLanguage("DE", languages, contentResolver)
        inputLanguage("EL_GR", languages, contentResolver)
        inputLanguage("EN_AU", languages, contentResolver)
        inputLanguage("EN_CA", languages, contentResolver)
        inputLanguage("EN_GB", languages, contentResolver)
        inputLanguage("EN_IN", languages, contentResolver)
        inputLanguage("EN_XC", languages, contentResolver)
        inputLanguage("ES", languages, contentResolver)
        inputLanguage("ES_US", languages, contentResolver)
        inputLanguage("ET", languages, contentResolver)
        inputLanguage("EU", languages, contentResolver)
        inputLanguage("FA", languages, contentResolver)
        inputLanguage("FI", languages, contentResolver)
        inputLanguage("FR", languages, contentResolver)
        inputLanguage("FR_CA", languages, contentResolver)
        inputLanguage("GL", languages, contentResolver)
        inputLanguage("GU", languages, contentResolver)
        inputLanguage("HI", languages, contentResolver)
        inputLanguage("HR", languages, contentResolver)
        inputLanguage("HU", languages, contentResolver)
        inputLanguage("HY", languages, contentResolver)
        inputLanguage("IN", languages, contentResolver)
        inputLanguage("IN_ID", languages, contentResolver)
        inputLanguage("IS", languages, contentResolver)
        inputLanguage("IW", languages, contentResolver)
        inputLanguage("JA", languages, contentResolver)
        inputLanguage("JA_JP", languages, contentResolver)
        inputLanguage("JP", languages, contentResolver)
        inputLanguage("KA", languages, contentResolver)
        inputLanguage("KK", languages, contentResolver)
        inputLanguage("KM", languages, contentResolver)
        inputLanguage("KN", languages, contentResolver)
        inputLanguage("KO", languages, contentResolver)
        inputLanguage("KY", languages, contentResolver)
        inputLanguage("LO", languages, contentResolver)
        inputLanguage("LT", languages, contentResolver)
        inputLanguage("LV", languages, contentResolver)
        inputLanguage("MK", languages, contentResolver)
        inputLanguage("ML", languages, contentResolver)
        inputLanguage("MN", languages, contentResolver)
        inputLanguage("MR", languages, contentResolver)
        inputLanguage("MY", languages, contentResolver)
        inputLanguage("NB", languages, contentResolver)
        inputLanguage("NE", languages, contentResolver)
        inputLanguage("NL", languages, contentResolver)
        inputLanguage("OR", languages, contentResolver)
        inputLanguage("PA", languages, contentResolver)
        inputLanguage("PL", languages, contentResolver)
        inputLanguage("PT", languages, contentResolver)
        inputLanguage("PT_PT", languages, contentResolver)
        inputLanguage("RO", languages, contentResolver)
        inputLanguage("RU", languages, contentResolver)
        inputLanguage("SI", languages, contentResolver)
        inputLanguage("SK", languages, contentResolver)
        inputLanguage("SL_SI", languages, contentResolver)
        inputLanguage("SQ", languages, contentResolver)
        inputLanguage("SV_SE", languages, contentResolver)
        inputLanguage("SW", languages, contentResolver)
        inputLanguage("TA", languages, contentResolver)
        inputLanguage("TE", languages, contentResolver)
        inputLanguage("TH", languages, contentResolver)
        inputLanguage("TL", languages, contentResolver)
        inputLanguage("TR", languages, contentResolver)
        inputLanguage("UK_UA", languages, contentResolver)
        inputLanguage("UR", languages, contentResolver)
        inputLanguage("UR_PK", languages, contentResolver)
        inputLanguage("UZ", languages, contentResolver)
        inputLanguage("VI", languages, contentResolver)
        inputLanguage("VI_VN", languages, contentResolver)
        inputLanguage("ZH_CN", languages, contentResolver)
        inputLanguage("ZH_HK", languages, contentResolver)
        inputLanguage("ZH_TW", languages, contentResolver)
        inputLanguage("ZU", languages, contentResolver)

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ languages data inserted ")
    }

    private fun inputLanguage(languageCode: String, languages: LanguagesProvider, contentResolver: ContentResolver) {
        var cv = ContentValues()
        cv.put(LANGUAGE_CODE_COLUMN, languageCode)
        cv.put(LANGUAGE_CONTENT_COLUMN, languages.getLanguageData(languageCode))
        contentResolver.insert(LANGUAGES_URI, cv)
    }

    private fun initConfigurableKeys(context: Context) {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(CONFIGURABLE_KEYS_URI, null, null, null)
        if (cursor == null || cursor.count == 0) {
            inputConfigurableKey("liveDPadUp", 0, "0", contentResolver)
            inputConfigurableKey("liveDPadDown", 0, "0", contentResolver)
            inputConfigurableKey("liveDPadRight", 0, "0", contentResolver)
            inputConfigurableKey("liveDPadLeft", 0, "0", contentResolver)
            inputConfigurableKey("liveOk", 1, "0", contentResolver)
            inputConfigurableKey("liveRed", 1, "0", contentResolver)
            inputConfigurableKey("liveGreen", 1, "0", contentResolver)
            inputConfigurableKey("liveYellow", 1, "0", contentResolver)
            inputConfigurableKey("liveBlue", 1, "0", contentResolver)
        }
        cursor?.close()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "############ configurable keys data inserted ")
    }

    private fun inputConfigurableKey(name: String, actionType: Int, description: String ,contentResolver: ContentResolver) {
        var cv = ContentValues()
        cv.put(CONFIGURABLE_KEY_NAME_COLUMN, name)
        cv.put(CONFIGURABLE_KEY_ACTION_TYPE_COLUMN, actionType)
        cv.put(CONFIGURABLE_KEY_DESCRIPTION_COLUMN, description)

        contentResolver.insert(CONFIGURABLE_KEYS_URI, cv)
    }
}