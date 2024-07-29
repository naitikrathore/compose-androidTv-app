package com.iwedia.cltv.platform.refplus5

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.tv.settings.partnercustomizer.tvsettingservice.TVSettingConfig
import com.iwedia.cltv.platform.base.UtilsInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.DISABLE_ZERO_PIN
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.ENABLE_HBBTV_BY_DEFAULT
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.HIDE_LOCKED_RECORDINGS
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.HIDE_LOCKED_SERVICES_IN_EPG
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.USE_HIDDEN_SERVICE_FLAG
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.SHOW_DEFAULT_PIN_TIP
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.DISABLE_OAD_NEWEST_VERSION_NOTIFICATION
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.DISABLE_ZERO_DIAL
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.refplus5.audio.TvAudioLanguageForATSC
import com.iwedia.cltv.platform.refplus5.audio.TvAudioLanguageForDVB
import com.iwedia.cltv.platform.refplus5.audio.TvAudioManager
import com.iwedia.cltv.platform.refplus5.audio.TvProviderAudioTrackBase
import com.iwedia.cltv.platform.refplus5.eas.EasControl
import com.iwedia.cltv.platform.refplus5.language.LanguageMapperImpl
import com.iwedia.cltv.platform.refplus5.provider.SystemInfoProvider
import com.iwedia.cltv.platform.refplus5.screenMode.Constants.Companion.CFG_BS_BS_SRC
import com.iwedia.cltv.platform.refplus5.subtitle.TvSubtitleLanguage
import com.iwedia.cltv.platform.refplus5.util.BlueMuteServiceBinder
import com.mediatek.dm.MountPoint
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.settings.lib.Constants
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigDecimal
import java.util.Date


class UtilsInterfaceImpl(
    private val context: Context,
    textToSpeechInterface: TTSInterface
) :
    UtilsInterfaceBaseImpl(context, textToSpeechInterface) {
    val TAG = javaClass.simpleName

    val CODEC_AUDIO_AC3 = "ac3"
    val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
    val CODEC_AUDIO_EAC3 = "eac3"
    val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
    val CODEC_AUDIO_DTS = TvProviderAudioTrackBase.DTS

    var primaryAudioLang: String = ""
    var secondaryAudioLang: String = ""
    var primarySubtitlesLang: String = ""
    var secondarySubtitlesLang: String = ""

    val AUTHORITY = "com.mediatek.tv.internal.data"
    val GLOBAL_PROVIDER_ID = "global_value"
    val GLOBAL_PROVIDER_URI_URI = Uri.parse("content://$AUTHORITY/$GLOBAL_PROVIDER_ID")
    val CFG_PWD_PASSWORD = "CFG_PWD_PASSWORD"
    private val GLOBAL_VALUE_KEY = "key"
    private val GLOBAL_VALUE_VALUE = "value"
    private val GLOBAL_VALUE_STORED = "stored"
    private val KEY_CFG_BLUE_MUTE = "CFG_BLUE_MUTE"

    private var renderBinder: BlueMuteServiceBinder? = null

    private var general = "general"

    override var languageCodeMapper : LanguageMapperInterface = LanguageMapperImpl(getCountryCode())

    override var jsonPath: String = "assets/OemCustomizationJsonFolder/OemCustomizationRef5_0.json"

    private val tvAudioManager = TvAudioManager(context)

    private val mTVSettingConfig = TVSettingConfig.getInstance(context)

    private var isTeletextAvailable: Boolean = false
    private var tvInputId: String = ""

    lateinit var timeInterface: TimeInterface
    private var easControl: EasControl? = null

    private var channelUnlocked = false
    private var updatedStoragePath = ""

    override fun getLanguageMapper() : LanguageMapperInterface {
        return languageCodeMapper
    }

    init {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "FIND_REGION", "MTK Region: "+getRegion().name)
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "FIND_REGION", "MTK CountryCode: "+getCountryCode())
        easControl = EasControl(context)
    }

    override fun getAudioDescriptionEnabled(callback: IAsyncDataCallback<Boolean>?) {
        tvAudioManager.getAudioType(context,0, getRegion(), callback = object : (Int)-> Unit {
            override fun invoke(type: Int) {
                callback?.onReceive(type == getAudioTypeValue(1))
            }
        })
    }

    override fun setAudioDescriptionEnabled(enable: Boolean) {
        tvAudioManager.set(
            context,
            TvAudioManager.ACCESSIBILITY_AUDIO_TYPE,
            if (enable) getAudioTypeValue(1) else getAudioTypeValue(0),
            true,
            getRegion()
        )
    }

    override fun setHearingImpairedEnabled(enable: Boolean) {
        tvAudioManager.set(
            context,
            TvAudioManager.ACCESSIBILITY_AUDIO_TYPE,
            if (enable) getAudioTypeValue(3) else getAudioTypeValue(0),
            true,
            getRegion()
        )
    }

    override fun getHearingImpairedEnabled(callback: IAsyncDataCallback<Boolean>?) {
        tvAudioManager.getAudioType(context,0, getRegion(), callback = object : (Int)-> Unit {
            override fun invoke(type: Int) {
                callback?.onReceive(type == getAudioTypeValue(3))
            }

        })
    }

    private fun getAudioTypeValue(type: Int): Int {
        /**
         * 0 : AUDIO_TYPE_NORMAL 1 : AUDIO_TYPE_AUDIO_DESCRIPTION 2 : AUDIO_TYPE_SPOKEN_SUBTITLE 3 :
         * AUDIO_TYPE_FOR_HARD_OF_HEARING 4 : Audio Description and spoken subtitle
         */
        return if (getRegion()==Region.US) {
            TvAudioLanguageForATSC.audioType.indexOf(type)
        } else {
            TvAudioLanguageForDVB.audioType.indexOf(type)
        }
    }

    override fun setVisuallyImpairedValues(position: Int, enabled: Boolean) {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "setVisuallyImpairedValues: $position, $enabled")
        if (position == 1) { // SPEAKER
            tvAudioManager.set(context, TvAudioManager.CFG_AUD_AD_SPEAKER_ENABLE, enabled, getRegion())
        } else if (position == 2) {// HEADPHONE
            tvAudioManager.set(context, TvAudioManager.CFG_AUD_AD_HEADPHONE_ENABLE, enabled, getRegion())
        } else {// FADE_PAN
            tvAudioManager.set(context, TvAudioManager.CFG_AUD_PAN_FADE_ENABLE, enabled, getRegion())
        }
    }

    override fun getAudioSpeakerState(): Boolean {
        val bool = tvAudioManager.getBoolean(
            context,TvAudioManager.CFG_AUD_AD_SPEAKER_ENABLE
        )
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getAudioSpeakerState: $bool")
        return tvAudioManager.getBoolean(
            context,TvAudioManager.CFG_AUD_AD_SPEAKER_ENABLE
        )
    }

    override fun getAudioHeadphoneState(): Boolean {
        val bool = tvAudioManager.getBoolean(
            context,TvAudioManager.CFG_AUD_AD_HEADPHONE_ENABLE
        )
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getAudioHeadphoneState: $bool")
        return tvAudioManager.getBoolean(
            context,TvAudioManager.CFG_AUD_AD_HEADPHONE_ENABLE
        )
    }

    override fun getAudioPaneState(): Boolean {
        val bool = tvAudioManager.getBoolean(
            context,TvAudioManager.CFG_AUD_PAN_FADE_ENABLE
        )
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getAudioPaneState: $bool")
        return tvAudioManager.getBoolean(
            context,TvAudioManager.CFG_AUD_PAN_FADE_ENABLE
        )
    }

    override fun setVolumeValue(value: Int) {
        tvAudioManager.set(context, TvAudioManager.CFG_AUD_AD_VOLUME, value, true, getRegion())
    }

    override fun getVolumeValue(): Int {
        return tvAudioManager.get(context, TvAudioManager.CFG_AUD_AD_VOLUME, 0)
    }

    override fun setFaderValue(newValueIndex: Int) {
        val newValue = TvAudioManager.LIST_FOR_FADER_CONTROL[newValueIndex]
        tvAudioManager.set(context, TvAudioManager.CFG_MS12_FADER_CONTROL, newValue,true,getRegion())
        val mixingLevelValue = when (newValueIndex) {
            0 -> -32
            1 -> -16
            2 -> 0
            3 -> 16
            4 -> 32
            else -> 0
        }
        tvAudioManager.setMixingLevel(context, mixingLevelValue, getRegion())
    }

    override fun getDefFaderValue(): Int {
        val defFadeValueIndex = tvAudioManager.getString(
            context,
            TvAudioManager.CFG_MS12_FADER_CONTROL
        )
        return TvAudioManager.LIST_FOR_FADER_CONTROL.indexOf(defFadeValueIndex)
    }

    override fun setVisuallyImpairedAudioValue(newValue: Int) {
        mTVSettingConfig.setConifg(TVSettingConfig.CFG_MENU_AUDIOINFO_SET_SELECT, newValue)
    }

    override fun getAudioForVi(): Int {
        return mTVSettingConfig.getConfigValueInt(TVSettingConfig.CFG_MENU_AUDIOINFO_GET_CURRENT)
    }

    override fun setPrimaryAudioLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setPrimaryAudioLanguage(language, type)

        val audioPrimaryLanguage : String
        val authority : String
        setPrefsValue("key_first_audio_lang", language)
        if (getRegion() == Region.US) {
            audioPrimaryLanguage = TvAudioLanguageForATSC.audio_primary_lang
            authority = TvAudioLanguageForATSC.authority
        }else{
            audioPrimaryLanguage = TvAudioLanguageForDVB.audio_primary_lang
            authority = TvAudioLanguageForDVB.authority
        }

        SaveValue.saveTISSettingsStringValue(
            context,
            audioPrimaryLanguage,
            authority,
            "general",
            language)
    }

    override fun setSecondaryAudioLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setSecondaryAudioLanguage(language, type)

        val audioSecondaryLanguage : String
        val authority : String
        setPrefsValue("key_second_audio_lang", language)
        if (getRegion() == Region.US) {
            audioSecondaryLanguage = TvAudioLanguageForATSC.audio_second_lang
            authority = TvAudioLanguageForATSC.authority
        }else{
            audioSecondaryLanguage = TvAudioLanguageForDVB.audio_second_lang
            authority = TvAudioLanguageForDVB.authority
        }

        SaveValue.saveTISSettingsStringValue(
            context,
            audioSecondaryLanguage,
            authority,
            "general",
            language)
    }

    override fun getPrimaryAudioLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getPrimaryAudioLanguage(type)

        val audioPrimaryLanguage : String
        val authority : String

        if (getRegion() == Region.US) {
            audioPrimaryLanguage = TvAudioLanguageForATSC.audio_primary_lang
            authority = TvAudioLanguageForATSC.authority
        }else{
            audioPrimaryLanguage = TvAudioLanguageForDVB.audio_primary_lang
            authority = TvAudioLanguageForDVB.authority
        }

        return SaveValue.readTISSettingsStringValues(context, audioPrimaryLanguage, authority, "general", "")
    }

    override fun getSecondaryAudioLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getSecondaryAudioLanguage(type)

        val audioSecondaryLanguage : String
        val authority : String

        if (getRegion() == Region.US) {
            audioSecondaryLanguage = TvAudioLanguageForATSC.audio_second_lang
            authority = TvAudioLanguageForATSC.authority
        }else{
            audioSecondaryLanguage = TvAudioLanguageForDVB.audio_second_lang
            authority = TvAudioLanguageForDVB.authority
        }

        return SaveValue.readTISSettingsStringValues(context, audioSecondaryLanguage, authority, "general", "")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean{
        return tvTrackInfo.isAudioDescription
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getDolby(tvTrackInfo: TvTrackInfo): String {
        if (tvTrackInfo.encoding == CODEC_AUDIO_AC3 || tvTrackInfo.encoding == CODEC_AUDIO_AC3_ATSC)
            return "AC3"
        if (tvTrackInfo.encoding == CODEC_AUDIO_EAC3 || tvTrackInfo.encoding == CODEC_AUDIO_EAC3_ATSC)
            return "EAC3"
        if (tvTrackInfo.encoding == CODEC_AUDIO_DTS)
            return TvProviderAudioTrackBase.DTS
        return ""
    }

    override fun isGretzkyBoard(): Boolean {
        return false
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

    override fun getRegion(): Region {
        return findRegionByCountry(getCountryCode())
    }

    override fun getAudioType(callback: IAsyncDataCallback<Int>?) {
        tvAudioManager.getAudioType(context,0, getRegion(), callback = object : (Int)-> Unit {
            override fun invoke(type: Int) {
                callback?.onReceive(type)
            }

        })
    }

    override fun setAudioType(position: Int) {
        tvAudioManager.setAudioType(context, position, getRegion())
    }

    override fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>) {
        val tunerMode = readMtkInternalGlobalIntValue(context, CFG_BS_BS_SRC,-1)
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getSystemInfoData: tunerMode= $tunerMode")
        if(systemInfoProviderInterface==null) systemInfoProviderInterface = SystemInfoProvider(context, tunerMode)
        systemInfoProviderInterface!!.getSystemInfoData(tvChannel, callback)
    }

    override fun getCountryCode(): String {
        val countryMap = mapOf(
            "AFG" to "AF",
            "ALA" to "AX",
            "ALB" to "AL",
            "DZA" to "DZ",
            "ASM" to "AS",
            "AND" to "AD",
            "AGO" to "AO",
            "AIA" to "AI",
            "ATA" to "AQ",
            "ATG" to "AG",
            "ARG" to "AR",
            "ARM" to "AM",
            "ABW" to "AW",
            "AUS" to "AU",
            "AUT" to "AT",
            "AZE" to "AZ",
            "BHS" to "BS",
            "BHR" to "BH",
            "BGD" to "BD",
            "BRB" to "BB",
            "BLR" to "BY",
            "BEL" to "BE",
            "BLZ" to "BZ",
            "BEN" to "BJ",
            "BMU" to "BM",
            "BTN" to "BT",
            "BOL" to "BO",
            "BES" to "BQ",
            "BIH" to "BA",
            "BWA" to "BW",
            "BVT" to "BV",
            "BRA" to "BR",
            "VGB" to "VG",
            "IOT" to "IO",
            "BRN" to "BN",
            "BGR" to "BG",
            "BFA" to "BF",
            "BDI" to "BI",
            "KHM" to "KH",
            "CMR" to "CM",
            "CAN" to "CA",
            "CPV" to "CV",
            "CYM" to "KY",
            "CAF" to "CF",
            "TCD" to "TD",
            "CHL" to "CL",
            "CHN" to "CN",
            "HKG" to "HK",
            "MAC" to "MO",
            "CXR" to "CX",
            "CCK" to "CC",
            "COL" to "CO",
            "COM" to "KM",
            "COG" to "CG",
            "COD" to "CD",
            "COK" to "CK",
            "CRI" to "CR",
            "CIV" to "CI",
            "HRV" to "HR",
            "CUB" to "CU",
            "CUW" to "CW",
            "CYP" to "CY",
            "CZE" to "CZ",
            "DNK" to "DK",
            "DJI" to "DJ",
            "DMA" to "DM",
            "DOM" to "DO",
            "ECU" to "EC",
            "EGY" to "EG",
            "SLV" to "SV",
            "GNQ" to "GQ",
            "ERI" to "ER",
            "EST" to "EE",
            "ETH" to "ET",
            "FLK" to "FK",
            "FRO" to "FO",
            "FJI" to "FJ",
            "FIN" to "FI",
            "FRA" to "FR",
            "GUF" to "GF",
            "PYF" to "PF",
            "ATF" to "TF",
            "GAB" to "GA",
            "GMB" to "GM",
            "GEO" to "GE",
            "DEU" to "DE",
            "GHA" to "GH",
            "GIB" to "GI",
            "GRC" to "GR",
            "GRL" to "GL",
            "GRD" to "GD",
            "GLP" to "GP",
            "GUM" to "GU",
            "GTM" to "GT",
            "GGY" to "GG",
            "GIN" to "GN",
            "GNB" to "GW",
            "GUY" to "GY",
            "HTI" to "HT",
            "HMD" to "HM",
            "VAT" to "VA",
            "HND" to "HN",
            "HUN" to "HU",
            "ISL" to "IS",
            "IND" to "IN",
            "IDN" to "ID",
            "IRN" to "IR",
            "IRQ" to "IQ",
            "IRL" to "IE",
            "IMN" to "IM",
            "ISR" to "IL",
            "ITA" to "IT",
            "JAM" to "JM",
            "JPN" to "JP",
            "JEY" to "JE",
            "JOR" to "JO",
            "KAZ" to "KZ",
            "KEN" to "KE",
            "KIR" to "KI",
            "PRK" to "KP",
            "KOR" to "KR",
            "KWT" to "KW",
            "KGZ" to "KG",
            "LAO" to "LA",
            "LVA" to "LV",
            "LBN" to "LB",
            "LSO" to "LS",
            "LBR" to "LR",
            "LBY" to "LY",
            "LIE" to "LI",
            "LTU" to "LT",
            "LUX" to "LU",
            "MKD" to "MK",
            "MDG" to "MG",
            "MWI" to "MW",
            "MYS" to "MY",
            "MDV" to "MV",
            "MLI" to "ML",
            "MLT" to "MT",
            "MHL" to "MH",
            "MTQ" to "MQ",
            "MRT" to "MR",
            "MUS" to "MU",
            "MYT" to "YT",
            "MEX" to "MX",
            "FSM" to "FM",
            "MDA" to "MD",
            "MCO" to "MC",
            "MNG" to "MN",
            "MNE" to "ME",
            "MSR" to "MS",
            "MAR" to "MA",
            "MOZ" to "MZ",
            "MMR" to "MM",
            "NAM" to "NA",
            "NRU" to "NR",
            "NPL" to "NP",
            "NLD" to "NL",
            "ANT" to "AN",
            "NCL" to "NC",
            "NZL" to "NZ",
            "NIC" to "NI",
            "NER" to "NE",
            "NGA" to "NG",
            "NIU" to "NU",
            "NFK" to "NF",
            "MNP" to "MP",
            "NOR" to "NO",
            "OMN" to "OM",
            "PAK" to "PK",
            "PLW" to "PW",
            "PSE" to "PS",
            "PAN" to "PA",
            "PNG" to "PG",
            "PRY" to "PY",
            "PER" to "PE",
            "PHL" to "PH",
            "PCN" to "PN",
            "POL" to "PL",
            "PRT" to "PT",
            "PRI" to "PR",
            "QAT" to "QA",
            "REU" to "RE",
            "ROU" to "RO",
            "RUS" to "RU",
            "RWA" to "RW",
            "BLM" to "BL",
            "SHN" to "SH",
            "KNA" to "KN",
            "LCA" to "LC",
            "MAF" to "MF",
            "SPM" to "PM",
            "VCT" to "VC",
            "WSM" to "WS",
            "SMR" to "SM",
            "STP" to "ST",
            "SAU" to "SA",
            "SEN" to "SN",
            "SRB" to "RS",
            "SYC" to "SC",
            "SLE" to "SL",
            "SGP" to "SG",
            "SXM" to "SX",
            "SVK" to "SK",
            "SVN" to "SI",
            "SLB" to "SB",
            "SOM" to "SO",
            "ZAF" to "ZA",
            "SGS" to "GS",
            "SSD" to "SS",
            "ESP" to "ES",
            "LKA" to "LK",
            "SDN" to "SD",
            "SUR" to "SR",
            "SJM" to "SJ",
            "SWZ" to "SZ",
            "SWE" to "SE",
            "CHE" to "CH",
            "SYR" to "SY",
            "TWN" to "TW",
            "TJK" to "TJ",
            "TZA" to "TZ",
            "THA" to "TH",
            "TLS" to "TL",
            "TGO" to "TG",
            "TKL" to "TK",
            "TON" to "TO",
            "TTO" to "TT",
            "TUN" to "TN",
            "TUR" to "TR",
            "TKM" to "TM",
            "TCA" to "TC",
            "TUV" to "TV",
            "UGA" to "UG",
            "UKR" to "UA",
            "ARE" to "AE",
            "GBR" to "GB",
            "USA" to "US",
            "UMI" to "UM",
            "URY" to "UY",
            "UZB" to "UZ",
            "VUT" to "VU",
            "VEN" to "VE",
            "VNM" to "VN",
            "VIR" to "VI",
            "WLF" to "WF",
            "ESH" to "EH",
            "YEM" to "YE",
            "ZMB" to "ZM",
            "ZWE" to "ZW",
            "XKX" to "XK"
        )

        val countryCode = Settings.Global.getString(context.contentResolver,
            "M_CURRENT_COUNTRY_REGION"
        )
        if (countryCode.length == 2) return countryCode

        return countryMap[countryCode] ?: super.getCountryCode()
    }

    fun saveMtkInternalGlobalValue(context: Context, id: String?, value: String?, isStored: Boolean): Boolean {
        val values = ContentValues()
        values.put(GLOBAL_VALUE_KEY, id)
        values.put(GLOBAL_VALUE_VALUE, value)
        values.put(GLOBAL_VALUE_STORED, java.lang.Boolean.valueOf(isStored))
        try {
            context.contentResolver.insert(GLOBAL_PROVIDER_URI_URI, values)
        } catch (ex: Exception) {
            return false
        }
        return true
    }

    fun readMtkInternalGlobalIntValue(context: Context, name : String, default : Int) : Int {
        try {
            val value = readMtkInternalGlobalValue(context, name)
            return Integer.parseInt(value!!)
        } catch(e : Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"readMtkInternalGlobalIntValue: Int conversion failed ${e.message}")
        }
        return default
    }

    fun readMtkInternalGlobalValue(context: Context, id: String?): String? {
        var cursor: Cursor? = null
        var value = ""
        val uri = Uri.withAppendedPath(GLOBAL_PROVIDER_URI_URI, id)
        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor == null) {
                return value
            }
            if (cursor.moveToNext()) {
                value = cursor.getString(1)
            }
        } catch (ex: java.lang.Exception) {
        }
        try {
            cursor!!.close()
        } catch (ex: Exception) {
            return value
        }
        return value
    }

    fun savePreference(name : String, value : Int) {
        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().putInt(name, value).apply()
    }

    fun savePreference(name : String, value : String) {
        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().putString(name, value).apply()
    }

    @SuppressLint("Range")
    override fun getParentalPin(): String {
        val pin = readMtkInternalGlobalValue(context,CFG_PWD_PASSWORD)
        if (pin.isNullOrEmpty()){
            setParentalPin("1234")
            return "1234"
        }
        return pin
    }

    override fun setParentalPin(pin: String) : Boolean {
        if((getCountryPreferences(DISABLE_ZERO_PIN,false) as Boolean) && (pin == "0000")) {
            return false
        }

        saveMtkInternalGlobalValue(context, CFG_PWD_PASSWORD, pin, true)
        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().putBoolean(PREFS_KEY_IS_PARENTAL_PIN_CHANGED, true).apply()
        return true
    }

    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
        var currentTime = Date(timeInterface.getCurrentTime(tvEvent.tvChannel))
        var eventStartTime = Date(tvEvent.startTime)
        var eventEndTime = Date(tvEvent.endTime)
        if (eventStartTime.before(currentTime) && eventEndTime.after(currentTime)) {
            return true
        }
        return false
    }

    override fun getBlueMuteState(): Boolean {
        return Settings.Global.getInt(context.contentResolver, KEY_CFG_BLUE_MUTE, 0) == 1
    }

    override fun enableBlueMute(enable: Boolean) {
        var value = if (enable) 1 else 0
        try {
            Settings.Global.putInt(context.contentResolver, KEY_CFG_BLUE_MUTE, value)
        } catch (e: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "e: Exception:" + e.message)
        }

        setBlueMute(enable)
    }

    override fun setBlueMute(isEnable: Boolean) {
        try {
            val intent = Intent()
            intent.action = "RenderService.remote"
            intent.setPackage("com.mediatek.extservice")

            renderBinder = BlueMuteServiceBinder(context, intent)
            renderBinder?.bindToService(isEnable)
        } catch (ex: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "init RenderBinder e = $ex")
        }
    }

    override fun dispose() {
        try {
            renderBinder?.unbind()
        } catch (ex: Exception) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "unbind RenderBinder e = $ex")
        }
    }

    override fun noSignalPowerOffEnabledOTA(): Boolean {
        var isEnabled = true
        if (Settings.Global.getString(context.contentResolver, "no_signal_auto_power_off") != null &&
            Settings.Global.getString(context.contentResolver, "no_signal_auto_power_off") != "") {
            isEnabled = Settings.Global.getInt(context.contentResolver, "no_signal_auto_power_off") != 0
        }
        return isEnabled
    }

    override fun noSignalPowerOffTimeOTA(): Int {
        //mtk database: 0 = off, 1 = 5 minutes, 2 = 10 minutes, 3 = 15 minutes, 4 = 30 minutes, 5 = 60 minutes
        //cltv database: 0 = 5 minutes, 1 = 10 minutes, 2 = 15 minutes, 3 = 30 minutes, 4 = 60 minutes
        var value = 2

        if (Settings.Global.getString(
                context.contentResolver,
                "no_signal_auto_power_off"
            ) != null &&
            Settings.Global.getString(context.contentResolver, "no_signal_auto_power_off") != ""
        ) {
            value = Settings.Global.getInt(context.contentResolver, "no_signal_auto_power_off") - 1
        }
        return value
    }

    override fun getIsPowerOffEnabled(): Boolean {
        return getPrefsValue("no_signal_auto_power_off_enable", true) as Boolean
    }

    override fun noSignalPowerOffChanged(isEnabled: Boolean) {
        setPrefsValue("no_signal_auto_power_off_enable", isEnabled)
    }

    override fun getPowerOffTime(): Any {
        return getPrefsValue("no_signal_auto_power_off", "duration_15_minutes") as String
    }

    override fun setPowerOffTime(value: Int, time: String) {
        setPrefsValue("no_signal_auto_power_off", time)
    }

    fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {
        when (eventType) {
            TTXInterfaceImpl.EVENT_TYPE_TTX_AVAILABLE -> {
                if (eventArgs.containsKey(TTXInterfaceImpl.KEY_TIS_TTX_AVAILABLE)) {
                    var status = eventArgs.getBoolean(TTXInterfaceImpl.KEY_TIS_TTX_AVAILABLE) ?: false
                    setTeletextAvailable(status)
                }
            }
            EasControl.EVENT_EAS_START,
            EasControl.EVENT_EAS_STOP -> {
                easControl?.onEvent(inputId, eventType, eventArgs)
            }
        }
    }

    fun setTvInputId(tvInputId: String) {
        this.tvInputId = tvInputId
    }

    fun getTvInputId(): String {
        return tvInputId
    }

    private fun setTeletextAvailable(isAvailable: Boolean) {
        isTeletextAvailable = isAvailable
    }

    override fun isTeletextAvailable() : Boolean {
        return isTeletextAvailable
    }

    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
        if (position == 1) {
            enableSubtitles(false)
        } else {
            enableSubtitles(true)
        }

        saveTISSettingsIntValue(context, TvSubtitleLanguage.digital_subtitle, TvSubtitleLanguage.authority, general, position)
    }

    override fun enableSubtitles(enable: Boolean, type: PrefType) {
        if (type == PrefType.BASE) return super.enableSubtitles(enable, type)
        var value = if (enable) {
            1
        } else {
            0
        }
        saveTISSettingsIntValue(context, TvSubtitleLanguage.digital_subtitle, TvSubtitleLanguage.authority, general, value)
    }

    override fun getSubtitlesState(type: PrefType): Boolean {
        if (type == PrefType.BASE) return super.getSubtitlesState(type)
        var subtitleValue = readTISSettingsIntValues(context, TvSubtitleLanguage.digital_subtitle, TvSubtitleLanguage.authority, general, -1)

        if (subtitleValue > 0) {
            return true
        }
        return false
    }

    override fun getSubtitleTypeDisplayNames(type: PrefType): MutableList<String> {
        if (type == PrefType.BASE) return super.getSubtitleTypeDisplayNames(type)
        return mutableListOf(
            getStringValue("off"),
            getStringValue("on"),
            getStringValue("subtitle_type_hearing_impaired"))
    }

    override fun getSubtitlesType(): Int {
        var subtitlesType = readTISSettingsIntValues(context, TvSubtitleLanguage.digital_subtitle, TvSubtitleLanguage.authority, general, -1)
        if (subtitlesType == -1) subtitlesType = 0
        return subtitlesType
    }

    fun isSubtitleRegion() : Boolean {
        if (getRegion() != Region.US) {
            return true
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo) : Boolean {
        var hasHoHKey= false
        try {
            if (tvTrackInfo.isHardOfHearing) {
                hasHoHKey = true
            }
        } catch (e: java.lang.Exception) { }
        return hasHoHKey
    }

    override fun setPrimarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setPrimarySubtitleLanguage(language, type)
        saveTISSettingsStringValue(
            context, TvSubtitleLanguage.subtitle_primary_langauge,
            TvSubtitleLanguage.authority, general,
            language)
    }

    override fun setSecondarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setSecondarySubtitleLanguage(language, type)
        saveTISSettingsStringValue(
            context, TvSubtitleLanguage.subtitle_second_langauge,
            TvSubtitleLanguage.authority, general,
            language)
    }

    override fun getPrimarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getPrimarySubtitleLanguage(type)
        return readTISSettingsStringValues(context, TvSubtitleLanguage.subtitle_primary_langauge, TvSubtitleLanguage.authority, general, "")
    }

    override fun getSecondarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getSecondarySubtitleLanguage(type)
        return readTISSettingsStringValues(context, TvSubtitleLanguage.subtitle_second_langauge, TvSubtitleLanguage.authority, general, "")
    }
    private fun saveTISSettingsStringValue(
        context: Context, id: String?, AUTHORITY: String?, general: String?, value: String?): Boolean {
        val uri = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))

        try {
            val values = ContentValues()
            values.put(id, value)
            context.contentResolver.update(uri, values, null, null)
        } catch (ex: java.lang.Exception) {
            return false
        }
        return true
    }

    @SuppressLint("Range")
    private fun readTISSettingsStringValues(
        context: Context, id: String, AUTHORITY: String?, general: String?, def: String): String? {
        var def = def
        var cursor: Cursor? = null
        val uri = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))

        try {
            cursor = context.contentResolver.query(uri, arrayOf(id), null, null, null)
            if (cursor == null) {
                return def
            }
            if (cursor.moveToNext()) {
                def = cursor.getString(cursor.getColumnIndex(id))
            }
        } catch (ex: java.lang.Exception) {
        }
        try {
            cursor!!.close()
        } catch (ex: java.lang.Exception) {
            return def
        }
        return def
    }

    private fun saveTISSettingsIntValue(context: Context, id: String?, AUTHORITY: String?, general: String?, value: Int): Boolean {
        val uri = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))

        try {
            val values = ContentValues()
            values.put(id, value)
            context.contentResolver.update(uri, values, null, null)
        } catch (ex: java.lang.Exception) {
            return false
        }
        return true
    }

    @SuppressLint("Range")
    private fun readTISSettingsIntValues(context: Context, id: String, AUTHORITY: String?, general: String?, def: Int): Int {
        var def = def
        var cursor: Cursor? = null
        val uri = Uri.parse(String.format("content://%s/%s", AUTHORITY, general))

        try {
            cursor = context.contentResolver.query(uri, arrayOf(id), null, null, null)
            if (cursor == null) {
                return def
            }
            if (cursor.moveToNext()) {
                def = cursor.getInt(cursor.getColumnIndex(id))
            }
        } catch (ex: java.lang.Exception) {
        }
        try {
            cursor!!.close()
        } catch (ex: java.lang.Exception) {
            return def
        }
        return def
    }

    override fun startScanChannelsIntent() {
        context.startActivity(
            Intent()
            .apply {
                setClassName(
                    com.iwedia.cltv.platform.model.Constants.ChannelConstants.COM_ANDROID_TV_SETTINGS,
                    com.iwedia.cltv.platform.model.Constants.ChannelConstants.MEDIATEK_TV_SETTINGS_CHANNEL_ACTIVITY
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    override fun formatUsbStorage(callback: IAsyncCallback) {
        try {
            val pvrPath = getPvrStoragePath()
            val currentDir = File("/storage/$pvrPath/")
            val deleteCommand = "rm -rf " + currentDir.absolutePath

            val runtime = Runtime.getRuntime()
            val process = runtime.exec(deleteCommand)
            process.waitFor()
            callback.onSuccess()

        } catch (e: Exception) {
            e.printStackTrace()
            callback.onFailed(Error())
        }
    }

    fun getUSBDriveFormat(devicePath: String): String {
        var usbDriveFormat = "Unknown"

        try {
            val command = "blkid -o value -s TYPE $devicePath"
            val process = Runtime.getRuntime().exec(command)
            val inputStream = process.inputStream
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            bufferedReader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        usbDriveFormat = line.trim()
                        return@forEach
                    }
                }
            }

            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return usbDriveFormat
    }


    override fun isCorrectUsbFormat(): Boolean {
        val pvrStoragePath = getPvrStoragePath()

        val fs = checkUSBFormat("/storage/$pvrStoragePath")
        Log.i(TAG, "isCorrectUsbFormat: $fs")
        return true
    }

    fun checkUSBFormat(path: String) {
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageVolumes = storageManager.storageVolumes

            val format = inferFormatFromPath(path)
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "USB Path: $path, Format: $format")
        } else {
            // For older versions of Android
            val path = Environment.getExternalStorageDirectory().path
            val format = inferFormatFromPath(path)
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "USB Path: $path, Format: $format")
        }
    }

    private fun inferFormatFromPath(path: String): String {
        return when {
            path.contains("fat16", ignoreCase = true) -> "FAT16"
            path.contains("fat32", ignoreCase = true) -> "FAT32"
            path.contains("ntfs", ignoreCase = true) -> "NTFS"
            path.contains("ext2", ignoreCase = true) -> "EXT2"
            path.contains("ext3", ignoreCase = true) -> "EXT3"
            path.contains("ext4", ignoreCase = true) -> "EXT4"
            else -> "UNKNOWN"
        }
    }

    override fun startSpeedTest(realPath: String, listener: UtilsInterface.SpeedTestListener) {
        val mSize = 0L

        if (realPath.isEmpty()) {
            listener.onFinished(false, -1f)
            return
        }
        var fis: FileOutputStream?
        var speedRate = 0f

        if (mSize == 0L) { // TODO @maksim dynamically pass this value
            val path = "$realPath/speedTest0.txt"
            val testFile = File(path)
            var maxSpeed = 0.0f
            var minTime = Long.MAX_VALUE
            if (testFile.exists()) {
                testFile.delete()
            }
            try {
                testFile.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                listener.onFinished(false, -1f)
                return
            }
            var bufferSize = 1024 * 100 // 7.7
            val defaultCount = 500f
            val defTestTimes = 3
            var counts = defaultCount
            val writeByte = ByteArray(bufferSize)
            var startTime = 0L
            val endTime: Long
            try {
                fis = FileOutputStream(testFile, false)
            } catch (e1: FileNotFoundException) {
                e1.printStackTrace()
                listener.onFinished(false, -1f)
                return
            }
            startTime = SystemClock.uptimeMillis()
            var startTime1 = 0L
            var startTime2 = 0L
            var progress = 0
            try {
                while (counts > 0) {
                    startTime1 = SystemClock.uptimeMillis()
                    fis?.write(writeByte)
                    startTime2 = SystemClock.uptimeMillis()
                    if (startTime2 - startTime1 in 1 until minTime
                    ) {
                        minTime = startTime2 - startTime1
                    }
                    counts--
                    if (defaultCount != 0f) {
                        progress = ((defaultCount - counts) / defaultCount * 100).toInt()
                    }

                    if (progress != 100) {
                        listener.reportProgress(progress)
                    }
                }
                fis.close()
                fis = null
            } catch (e: IOException) {
                e.printStackTrace()
                bufferSize = 0
                listener.onFinished(false, -1f)
            } finally {
                testFile.delete()
                try {
                    if (null != fis) {
                        fis.close()
                        fis = null
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    listener.onFinished(false, -1f)
                }
            }
            endTime = SystemClock.uptimeMillis()
            maxSpeed = bufferSize * 1000f / minTime / 1024 / 1024
            val speedMax = BigDecimal(maxSpeed.toString()).setScale(
                1,
                BigDecimal.ROUND_HALF_UP
            ).toString()
            maxSpeed = speedMax.toFloat()
            val average =
                bufferSize * defaultCount * 1000L / (endTime - startTime) / 1024 / 1024
            if (defTestTimes != 0) {
                speedRate = average / defTestTimes
                listener.onFinished(true, speedRate)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getUsbDevices(): HashMap<String, String> {
        val map = hashMapOf<String, String>()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        try {
            val volumes = storageManager.storageVolumes
            for (item in volumes) {
                val path = item.uuid
                val state = item.state
                var directory = item?.directory?.toString()


                if (state == null || path == null || state != Environment.MEDIA_MOUNTED) {
                    continue
                }

                directory?.let {
                    map[it] = item.getDescription(context)
                }
                updatedStoragePath = path
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return map
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun isUsbWritableReadable(): Boolean {
        var isWritableAndReadable = false
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        try {
            val volumes = storageManager.storageVolumes
            for (item in volumes) {
                val path = item.uuid
                val state = item.state
                var directory = item?.directory?.toString()


                if (state == null || path == null || state != Environment.MEDIA_MOUNTED) {
                    continue
                }

                val stat = StatFs(directory)
                val bytesAvailable = stat.availableBytes
                val bytesTotal = stat.totalBytes
                val megaBytesAvailable = bytesAvailable / (1024 * 1024)
                val megaBytesTotal = bytesTotal / (1024 * 1024)
                val mountpoint = MountPoint("","","")
                mountpoint.mMountPoint = directory
                mountpoint.mVolumeLabel = item.getDescription(context)
                mountpoint.mTotalSize = bytesTotal
                mountpoint.mFreeSize = bytesAvailable

                isWritableAndReadable = true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            isWritableAndReadable = false
        }
        return isWritableAndReadable
    }

    override fun setPvrStoragePath(path: String) {
        if (path == updatedStoragePath) setPrefsValue(PVR_STORAGE_PATH, path)
        else setPrefsValue(PVR_STORAGE_PATH, "")
    }

    override fun setTimeshiftStoragePath(path: String) {
        setPrefsValue(TIMESHIFT_STORAGE_PATH, path)
    }

    override fun getTimeshiftStoragePath(): String {
        return getPrefsValue(TIMESHIFT_STORAGE_PATH, "").toString()
    }

    override fun getPvrStoragePath(): String {
        val path = getPrefsValue(PVR_STORAGE_PATH, "").toString()
        return if (updatedStoragePath == path) path else ""
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun getUsbStorageFreeSize(callback: IAsyncDataCallback<Long>){
        Log.i(TAG, "getMountPointList")
        val list = ArrayList<MountPoint>()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val stats = context.getSystemService(
            StorageStatsManager::class.java
        )
        try {
            val volumes = storageManager.storageVolumes
            if (volumes == null || 0 == volumes.size) {
                callback.onFailed(Error("No device found"))
                return
            }
            else {
                Log.i(TAG, "volumes.length:" + volumes.size.toString())
                for (item in volumes) {
                    val path = item.uuid
                    val state = item.state
                    var directory: String? = null
                    if (item.directory != null) {
                        directory = item.directory.toString()
                    }
                    if (state == null || path == null || state != Environment.MEDIA_MOUNTED) {
                        continue
                    }
                    val stat = StatFs(directory)
                    val bytesAvailable = stat.availableBytes
                    val bytesTotal = stat.totalBytes
                    val mountpoint = MountPoint("","","")
                    mountpoint.mMountPoint = directory
                    mountpoint.mVolumeLabel = item.getDescription(context)
                    mountpoint.mTotalSize = bytesTotal
                    mountpoint.mFreeSize = bytesAvailable
                    //mountpoint.mStatus = state;
                    Log.i(TAG, "**************************")
                    Log.i(TAG, "StorageVolume path state:$path  $state")
                    Log.i(TAG, "StorageVolume total free:$bytesTotal/$bytesAvailable")
                    Log.i(TAG, "StorageVolume getUuid:" + item.uuid)
                    Log.i(TAG, "StorageVolume getDescription:" + item.getDescription(context))
                    Log.i(TAG, "StorageVolume getDirectory():$directory")
                    Log.i(
                        TAG,
                        "StorageVolume getMediaStoreVolumeName():" + item.mediaStoreVolumeName
                    )
                    Log.i(TAG, "StorageVolume getState():" + item.state)
                    Log.i(TAG, "**************************")
                    //DvrLog.i(TAG, "StorageVolume getStorageUuid():" + item.getStorageUuid());
                    list.add(mountpoint)
                }
            }
        } catch (e: SecurityException) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "permission.PACKAGE_USAGE_STATS")
            callback.onFailed(Error("No permission"))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Exception $e")
            callback.onFailed(Error("Exception"))
        }
        Log.i(TAG, "list size = " + list.size)
        if (list.isNotEmpty()) callback.onReceive(list[0].mFreeSize)
        else callback.onFailed(Error("No device found"))

        return
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun isUsbFreeSpaceAvailable() : Boolean {
        var isUsbFileAccessible = true
        var isUsbFileNotTooLess = true
        val lessUsbMemory = 100L //less than 100MB free is when recording of event stops

        getUsbStorageFreeSize(object : IAsyncDataCallback<Long> {
            override fun onFailed(error: Error) {
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Usb file may not be available")
                isUsbFileAccessible = false
            }

            override fun onReceive(data: Long) {
                val freeMemory: Long = (data / 1024 / 1024)
                Log.i(TAG, "Usb free memory $freeMemory")
                if( ( freeMemory == 0L) || freeMemory < lessUsbMemory ){
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Usb file may be full or very less freeMemory = $freeMemory")
                    isUsbFileNotTooLess = false
                }
            }
        })

        Log.i(TAG, "isUsbFreeSpaceAvailable: isUsbFileAccessible $isUsbFileAccessible")
        Log.i(TAG, "isUsbFreeSpaceAvailable: isUsbFileNotTooLess $isUsbFileNotTooLess")
        return isUsbFileAccessible && isUsbFileNotTooLess
    }

    override fun getCountryPreferences(
        preference: UtilsInterface.CountryPreference,
        defaultValue: Any?
    ): Any? {
        var countryCode = getCountryCode().uppercase()
        when (preference) {
            HIDE_LOCKED_SERVICES_IN_EPG -> {
                if(countryCode == "FR") {
                    return true
                }
            }
            DISABLE_ZERO_PIN -> {
                if(countryCode == "FR") {
                    return true
                }
            }
            HIDE_LOCKED_RECORDINGS -> {
                if(countryCode == "FR") {
                    return true
                }
            }
            ENABLE_HBBTV_BY_DEFAULT -> {
                if(countryCode == "MY") {
                    return true
                }
            }
            USE_HIDDEN_SERVICE_FLAG -> {
                if(countryCode == "MY" || countryCode == "ID" || countryCode == "TH") {
                    return true
                }

                if (getRegion()==Region.EU) {
                    return true
                }
            }
            SHOW_DEFAULT_PIN_TIP -> {
                //todo Kindly handle it as required(currently it will work for fast only)
                return defaultValue
            }
            DISABLE_OAD_NEWEST_VERSION_NOTIFICATION -> {
                if(countryCode == "MY") {
                     return true
                }
            }
            DISABLE_ZERO_DIAL -> {
                if (getRegion()==Region.US) {
                    return true
                }
            }
            else -> { return defaultValue }
        }

        return defaultValue
    }

    override fun getPlatformPreferences(
        preference: UtilsInterface.PlatformPreference,
        defaultValue: Any?
    ): Any? {
        when(preference) {
            UtilsInterface.PlatformPreference.USE_POSTER_THUMB_RECORDING_FALLBACK -> { return true }
        }
        return defaultValue
    }

    override fun deleteRecordingFromUsb(recording: Recording, callback: IAsyncCallback) {
        try {
            val recordingUri = recording.videoUrl
            val currentDir = File(recordingUri)
            CoroutineHelper.runCoroutine({
                deleteRecursive(currentDir)
                callback.onSuccess()
            })
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onFailed(Error())
        }
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) for (child in fileOrDirectory.listFiles()!!) deleteRecursive(child)
        fileOrDirectory.delete()
    }

    //Used for HIDE_LOCKED_RECORDINGS parental requirement to not show PVR recording of locked service
    //show it only if service was unlocked
    fun setChannelUnlocked(unlocked : Boolean) {
        channelUnlocked = unlocked
    }
    fun isChannelUnlocked() : Boolean {
        return channelUnlocked
    }

    override fun getEWSPostalCode(): String {
        return SaveValue.readTISSettingsStringValues(
            context, Constants.COLUMN_INDONESIA_EWS_LOCATION_CODE,
            Constants.AUTHORITY, "general", ""
        )
    }

    override fun setEWSPostalCode(context: Context, postalCode: String) {
        SaveValue.saveTISSettingsStringValue(
            context, Constants.COLUMN_INDONESIA_EWS_LOCATION_CODE, Constants.AUTHORITY,
            Constants.DATATYPE_GENERAL, postalCode
        )
    }

    override fun setVideoResolutionForRecording(recordingId: Long, resolution: String) {
        val contentResolver: ContentResolver = context.contentResolver
        val where = TvContract.RecordedPrograms._ID + " =?"
        val args = arrayOf(recordingId.toString())
        val contentValues = ContentValues()
        contentValues.put(TvContract.RecordedPrograms.COLUMN_INTERNAL_PROVIDER_DATA, resolution)
        contentResolver.update(
            TvContract.RecordedPrograms.CONTENT_URI,
            contentValues,
            where,
            args
        )
    }

    @SuppressLint("Range")
    override fun getVideoResolutionForRecording(recording: Recording): String {
        var resolution = ""
        val contentResolver: ContentResolver = context.contentResolver
        val where = TvContract.RecordedPrograms._ID + " =?"
        val args = arrayOf(recording.id.toString())
        val cursor = contentResolver.query(
            TvContract.RecordedPrograms.CONTENT_URI,
            null,
            where,
            args,
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            if(cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_INTERNAL_PROVIDER_DATA)) != null){
                resolution = cursor.getString(cursor.getColumnIndex(TvContract.RecordedPrograms.COLUMN_INTERNAL_PROVIDER_DATA))
            }
        }
        cursor?.close()

        return resolution
    }

    override fun needTCServiceUpdate(context: Context): Boolean {
        val value = com.mediatek.wwtv.tvcenter.util.SaveValue.readWorldBooleanValue(context, "CFG_MENU_CH_UPDATE_MSG")
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "needSServiceUpdate: value == $value")
        if(value){
            val number = SaveValue.readTISSettingsIntValues(
                context,
                Constants.COLUMN_CHANNEL_NUM_ADDED,
                Constants.AUTHORITY,
                Constants.DATATYPE_GENERAL,
                0)
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "needSServiceUpdate: number == $number")
            if(number > 0){
                resetTCServiceUpdate(context)
            }
            return number > 0
        }
        return false
    }

    private fun resetTCServiceUpdate(context: Context) {
        SaveValue.saveTISSettingsIntValue(
            context,
            Constants.COLUMN_CHANNEL_NUM_ADDED,
            Constants.AUTHORITY,
            Constants.DATATYPE_GENERAL,
            0)
    }

    override fun setGoogleLauncherBehaviour(context: Context, inputId: String) {
    }

    override fun isTeletextBasedSubtitle(tvTrackInfo: TvTrackInfo): Boolean {
        if((tvTrackInfo.encoding != null) && (tvTrackInfo.encoding!!.contains("dvbttx"))) {
            return true
        }
        return false
    }

    override fun updateLockedChannelIdsToPref(
        context: Context,
        isAdded: Boolean,
        channelId: String
    ) {
        if (isAdded) {
            addChannelIds(context, channelId)
        } else {
            removeChannelIds(context, channelId)
        }
    }

    override fun getChannelIdsFromPrefs(): MutableSet<String> {
        val sharedPreferences =
            context.getSharedPreferences(UtilsInterface.PREFS_TAG_LOCKED_CHANNEL_IDS, Context.MODE_PRIVATE)
        val channelIds = sharedPreferences?.getString(
            com.iwedia.cltv.platform.model.Constants.SharedPrefsConstants.PREFS_KEY_LOCKED_CHANNEL_IDS,
            ""
        ) ?: ""
        return channelIds.split(",").filter { it.isNotEmpty() }.toMutableSet()
    }

    private fun saveChannelIdsToSharedPref(context: Context, ids: Set<String>) {
        var sharedPreferences =
            context.getSharedPreferences(UtilsInterface.PREFS_TAG_LOCKED_CHANNEL_IDS, Context.MODE_PRIVATE)
        with(sharedPreferences?.edit()) {
            this!!.putString(
                com.iwedia.cltv.platform.model.Constants.SharedPrefsConstants.PREFS_KEY_LOCKED_CHANNEL_IDS,
                ids.joinToString(",")
            )
            apply()
        }
    }


    private fun addChannelIds(context: Context, uniqueId: String) {
        val ids = getChannelIdsFromPrefs()
        ids.add(uniqueId)
        saveChannelIdsToSharedPref(context, ids)
    }

    private fun removeChannelIds(context: Context, uniqueId: String) {
        val ids = getChannelIdsFromPrefs()
        ids.remove(uniqueId)
        saveChannelIdsToSharedPref(context, ids)
    }

    override fun clearChannelidsPref(){
        var sharedPreferences =
            context.getSharedPreferences(UtilsInterface.PREFS_TAG_LOCKED_CHANNEL_IDS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        for (key in sharedPreferences.all.keys) {
            if (key.startsWith(com.iwedia.cltv.platform.model.Constants.SharedPrefsConstants.PREFS_KEY_LOCKED_CHANNEL_IDS)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun hasHohAudio(tvTrackInfo: TvTrackInfo): Boolean {
        var hasHoHKey = false
        try {
            if (tvTrackInfo.isHardOfHearing) {
                hasHoHKey = true
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "[hasHohAudio] Exception : ${e}")
        }
        return hasHoHKey
    }

    fun getMtkChannelRating(tvChannel: TvChannel): String {
        var mtkChannelRating = ""
        val contentResolver: ContentResolver = context.contentResolver
        val where = TvContract.Channels._ID + " =?"
        val args = arrayOf(tvChannel.id.toString())
        val cursor = contentResolver.query(
            TvContract.buildChannelUri(tvChannel.channelId),
            null, where, args, null
        )

        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            val indexOfMtkChannelRating = cursor.getColumnIndex("mtk_channel_rating")
            if (indexOfMtkChannelRating != -1 && indexOfMtkChannelRating.let { cursor.getString(it) } != null) {
                mtkChannelRating = cursor.getString(indexOfMtkChannelRating)
            }
        }
        cursor?.close()
        return mtkChannelRating
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getCodecDolbySpecificAudioInfo(tvTrackInfo: TvTrackInfo): String {
        var dolbyType = ""
        try {
            var encoding = tvTrackInfo?.encoding
            Log.e("UTILSREF", "[getCodecDolbySpecificAudioInfo] encoding $encoding ")
            if (encoding.isNullOrEmpty()) {
                return dolbyType
            }
            val split = encoding.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            Log.e("UTILSREF", "[getCodecDolbySpecificAudioInfo] split $split ")
            val encodingTxt = split[1]
            if ("ac3" == encodingTxt || "eac3" == encodingTxt || "ac4" == encodingTxt) {
                dolbyType = encodingTxt
            }
        } catch (e: Exception) {
            Log.e("UTILSREF", "[getCodecDolbySpecificAudioInfo] Exception $e ")
        }
        return dolbyType
    }


    override fun hasAdSpsAudio(tvTrackInfo: TvTrackInfo): Boolean {
        return isAdSpsAudio(tvTrackInfo)
    }

    override fun hasSpsAudio(tvTrackInfo: TvTrackInfo): Boolean {
        return isSpsAudio(tvTrackInfo)
    }

    private fun isSpsAudio(tAudioMpeg: TvTrackInfo): Boolean {
        Log.d("UTILSREF", "aAudIsSpsAudio-----track id: " + tAudioMpeg.id)
        var bIsSps = false
        var mixtype = -1
        var eClass = -1
        if (null != tAudioMpeg.extra) {
            mixtype = tAudioMpeg.extra.getInt("KEY_TRACK_INT_AUDIO_MIX_TYPE")
            eClass = tAudioMpeg.extra.getInt("KEY_TRACK_INT_AUDIO_CLASS")
            Log.d("UTILSREF", "aAudIsSpsAudio eClass:$eClass mixtype:$mixtype")
            // Mix_type = 1 && e_class = 4
            if (TvProviderAudioTrackBase.AUD_MIX_TYPE_SUPPLEMENTARY == mixtype
                && TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE == eClass
            ) {
                bIsSps = true
            }
        }
        Log.d("UTILSREF", "aAudIsSpsAudio-----result: $bIsSps")
        return bIsSps
    }

    private fun isAdSpsAudio(tAudioMpeg: TvTrackInfo): Boolean {
        Log.d("UTILSREF", "aAudIsAdSpsAudio-----track id: " + tAudioMpeg.id)
        var isAdSps = false
        var eType = -1
        var mixtype = -1
        var eClass = -1
        if (null != tAudioMpeg.extra) {
            eType = tAudioMpeg.extra.getInt("KEY_TRACK_INT_AUDIO_TYPE")
            mixtype = tAudioMpeg.extra.getInt("KEY_TRACK_INT_AUDIO_MIX_TYPE")
            eClass = tAudioMpeg.extra.getInt("KEY_TRACK_INT_AUDIO_CLASS")
            Log.d("UTILSREF", "aAudIsAdSpsAudio eClass:$eClass mixtype:$mixtype,eType:$eType")
            if (TvProviderAudioTrackBase.AUD_MIX_TYPE_SUPPLEMENTARY == mixtype &&
                TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_RESERVED == eClass
                && TvProviderAudioTrackBase.AUD_TYPE_VISUAL_IMPAIRED == eType) {
                isAdSps = true
            }
        }
        Log.d("UTILSREF","aAudIsAdSpsAudio-----result: $isAdSps")
        return isAdSps
    }


    override fun getApdString(tvTrackInfo: TvTrackInfo): String {
        var apdText = ""
        if (tvTrackInfo.extra != null) {
            apdText = tvTrackInfo.extra.getString("KEY_TRACK_AC4_APD") ?: apdText
        }
        return apdText
    }

}