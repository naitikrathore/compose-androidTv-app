package com.iwedia.cltv.platform.rtk

import android.content.Context
import android.content.Intent
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.base.UtilsInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.rtk.PlayerInterfaceImpl.Companion.CODEC_TYPE_ARRAY
import com.iwedia.cltv.platform.rtk.PlayerInterfaceImpl.Companion.TYPE_AC4
import com.iwedia.cltv.platform.rtk.PlayerInterfaceImpl.Companion.TYPE_DOLBY_DIGITAL
import com.iwedia.cltv.platform.rtk.PlayerInterfaceImpl.Companion.TYPE_DOLBY_DIGITAL_PLUS
import com.iwedia.cltv.platform.rtk.aspectRatio.AspectRatioActioner
import com.iwedia.cltv.platform.rtk.provider.SystemInfoProvider
import com.iwedia.cltv.platform.rtk.language.LanguageMapperImpl
import com.iwedia.cltv.platform.rtk.util.GtvUtil
import com.realtek.system.RtkConfigs
import com.realtek.tv.RtkSettingConstants
import com.realtek.tv.Tv
import com.realtek.tv.hbbtv.hbbtvmanagerutils.PreferredLanguage
import java.util.*

class UtilsInterfaceImpl(private var context: Context,
                         textToSpeechModule: TTSInterface
) : UtilsInterfaceBaseImpl(context, textToSpeechModule) {
    private val TAG = javaClass.simpleName

    val CODEC_AUDIO_AC3 = "ac3"
    val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
    val CODEC_AUDIO_EAC3 = "eac3"
    val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
    val CODEC_AUDIO_DTS = "dts"

    val SUBTITLE_SETTING_TYPE_OFF = 0
    val SUBTITLE_SETTING_TYPE_BASIC = 1
    val SUBTITLE_SETTING_TYPE_HEARING_IMPAIRED = 2

    var primaryAudioLang: String = ""
    var secondaryAudioLang: String = ""
    var primarySubtitlesLang: String = ""
    var secondarySubtitlesLang: String = ""


    private val URI_SCHEME_CUSTOMIZE = "customize"
    private val URI_HOST_GUIDE_BUTTON = "guideButton"

    lateinit var timeInterface: TimeInterface
    lateinit var hbbTvInterface: HbbTvInterfaceImpl

    override var languageCodeMapper : LanguageMapperInterface = LanguageMapperImpl()

    override var jsonPath: String = "assets/OemCustomizationJsonFolder/OemCustomizationRtk.json"

    private var aspectRatioActioner: AspectRatioActioner? = null

    private val PREF_KEY_DVR_RECORD_PATH = "dvr_record_path"
    private val PREF_KEY_USB_PATHES = "usb_pathes"

    @RequiresApi(Build.VERSION_CODES.P)
    var tv: Tv? = null
    init {
        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "RTK Region: "+getRegion().name)
        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "RTK CountryCode: "+getCountryCode())
        aspectRatioActioner = AspectRatioActioner()
        //RTK has asked to call following method at initialisation of app.
        setGoogleLauncherBehaviour(context, "")
    }

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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateAudioTracks() {
        if (primaryAudioLang != "") {
            setPrimaryAudioLanguage(primaryAudioLang)
        }
        if (secondaryAudioLang != "") {
            setSecondaryAudioLanguage(secondaryAudioLang)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateSubtitleTracks() {
        if (primarySubtitlesLang != "") {
            setPrimarySubtitleLanguage(primarySubtitlesLang)
        }
        if (secondarySubtitlesLang != "") {
            setSecondarySubtitleLanguage(secondarySubtitlesLang)
        }
    }

    override fun getSubtitleTypeDisplayNames(type: PrefType): MutableList<String> {
        if (type == PrefType.BASE) return super.getSubtitleTypeDisplayNames(type)
        return mutableListOf(
            getStringValue("off"),
            "Basic",
            getStringValue("subtitle_type_hearing_impaired"))
    }

    override fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>) {
        if(systemInfoProviderInterface==null) systemInfoProviderInterface = SystemInfoProvider(context, this)
        systemInfoProviderInterface!!.getSystemInfoData(tvChannel,callback)
    }

    override fun getRegion(): Region {
        return findRegionByCountry(getCountryCode())
    }

    override fun getCountryCode(): String {
        val countryMap = mapOf(
            0 to "AD",
            1 to "AU",
            2 to "AT",
            3 to "BE",
            4 to "BW",
            5 to "BR",
            6 to "CA",
            7 to "CN",
            8 to "HR",
            9 to "CZ",
            10 to "DK",
            11 to "EE",
            12 to "FI",
            13 to "FR",
            14 to "DE",
            15 to "GR",
            16 to "HU",
            17 to "IN",
            18 to "ID",
            19 to "IE",
            20 to "IT",
            21 to "JP",
            22 to "LT",
            23 to "LU",
            24 to "MK",
            25 to "MT",
            26 to "MY",
            27 to "MX",
            28 to "NL",
            29 to "NO",
            30 to "PH",
            31 to "RO",
            32 to "PL",
            33 to "PT",
            34 to "SG",
            35 to "SK",
            36 to "SI",
            37 to "ES",
            38 to "SE",
            39 to "CH",
            40 to "TW",
            41 to "TH",
            42 to "TR",
            43 to "GB",
            44 to "US",
            45 to "VN",
            46 to "IR",
            47 to "MA",
            48 to "MM",
            49 to "NG",
            50 to "GE",
            51 to "KE",
            52 to "AO",
            53 to "MU",
            54 to "UZ",
            55 to "TZ",
            56 to "AE",
            57 to "LA",
            58 to "MN",
            59 to "KH",
            60 to "BD",
            61 to "LK",
            62 to "NP",
            63 to "BN",
            64 to "FJ",
            65 to "MV",
            66 to "IL",
            67 to "ZZ",
            68 to "AF",
            69 to "AL",
            70 to "DZ",
            71 to "AG",
            72 to "AR",
            73 to "AM",
            74 to "AW",
            75 to "BH",
            76 to "BB",
            77 to "BY",
            78 to "BZ",
            79 to "BJ",
            80 to "BO",
            81 to "BA",
            82 to "BG",
            84 to "CO",
            85 to "CG",
            86 to "CR",
            87 to "CU",
            88 to "CR",
            89 to "CY",
            90 to "CD",
            91 to "DM",
            92 to "EC",
            93 to "EG",
            94 to "ET",
            95 to "GH",
            96 to "GD",
            97 to "GT",
            98 to "GY",
            99 to "HT",
            100 to "HN",
            101 to "HK",
            102 to "IS",
            103 to "IQ",
            104 to "CI",
            105 to "JM",
            106 to "JO",
            107 to "KZ",
            108 to "XK",
            109 to "KW",
            110 to "KG",
            111 to "LV",
            112 to "LB",
            113 to "LR",
            114 to "LY",
            115 to "MD",
            116 to "ME",
            117 to "MZ",
            118 to "NC",
            119 to "NZ",
            120 to "NI",
            121 to "KP",
            122 to "OM",
            123 to "PK",
            124 to "PS",
            125 to "PA",
            126 to "PY",
            127 to "PE",
            128 to "PF",
            129 to "PR",
            130 to "QA",
            131 to "RU",
            132 to "SV",
            133 to "SA",
            134 to "RS",
            135 to "ZA",
            136 to "KR",
            137 to "KN",
            138 to "LC",
            139 to "VC",
            140 to "SD",
            141 to "SR",
            142 to "SY",
            143 to "TJ",
            144 to "TT",
            145 to "TN",
            146 to "TM",
            147 to "AE",
            148 to "UA",
            149 to "UY",
            150 to "VE",
            151 to "YE",
            152 to "ZM",
            153 to "AZ",
            154 to "NA",
            155 to "SN",
            156 to "BD",
            157 to "BF",
            158 to "BW",
            159 to "SO",
            160 to "WS",
            161 to "CR",
            162 to "ML",
            163 to "GN",
            164 to "CM",
            165 to "ZW",
            166 to "GA",
            167 to "GM",
            168 to "MG",
            169 to "MW",
            170 to "NE",
            171 to "SL",
            172 to "TG",
            173 to "UG",
            174 to "TD",
            175 to "DJ",
            176 to "MR",
        )

        Log.d(Constants.LogTag.CLTV_TAG + "FIND_COUNTRY", "RtkSettingConstants.TV_COUNTRY_INDEX: ${RtkSettingConstants.TV_COUNTRY_INDEX}")
       /* Log.d(Constants.LogTag.CLTV_TAG + "FIND_COUNTRY", "RtkSettingConstants.TV_COUNTRY_INDEX: ${RtkSettingConstants.TV_COUNTRY_INDEX}")
      //  var tvCountryIndex = "com.realtek.tv_current_country"
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.TIRAMISU) {
            tvCountryIndex = "com.android.tv_current_country"
        }*/
        val country = countryMap[Settings.Global.getInt(
            context.contentResolver,
            RtkSettingConstants.TV_COUNTRY_INDEX
        )]

        return country ?: super.getCountryCode()
    }

    override fun startScanChannelsIntent() {
        context.startActivity(Intent("android.settings.CHANNEL_SCAN_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
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


    @RequiresApi(Build.VERSION_CODES.P)
    fun getTvSetting(): Tv {
        if (tv == null) {
            tv = Tv()
            return tv!!
        }
        return tv!!
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setAudioType(position: Int) {

        when (position) {
            1 -> {
                setAudioDescriptionEnabled(true)
                setSpokenSubtitleEnabled(false)
                setHearingImpairedEnabled(false)
            }
            2 -> {
                setAudioDescriptionEnabled(false)
                setSpokenSubtitleEnabled(true)
                setHearingImpairedEnabled(false)
            }
            3 -> {
                setAudioDescriptionEnabled(false)
                setSpokenSubtitleEnabled(false)
                setHearingImpairedEnabled(true)
            }
            4 -> {
                setAudioDescriptionEnabled(true)
                setSpokenSubtitleEnabled(true)
                setHearingImpairedEnabled(false)
            }
            else -> {
                setAudioDescriptionEnabled(false)
                setSpokenSubtitleEnabled(false)
                setHearingImpairedEnabled(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getAudioType(callback: IAsyncDataCallback<Int>?) {
        val audioDescriptionEnabled = getTvSetting().dtvAudioDescriptionOnOff
        val hearingImpairedEnabled = getTvSetting().dtvSubtitleHardOfHearing
        val audioTypeIndex = if (audioDescriptionEnabled && getSpokenSubtitleEnabled())
            4
        else if (audioDescriptionEnabled)
            1
        else if (getSpokenSubtitleEnabled())
            2
        else if (hearingImpairedEnabled)
            3
        else
            0

        callback?.onReceive(audioTypeIndex)
    }

    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        var isAD = false

        if (tvTrackInfo?.extra != null) {
            isAD = (tvTrackInfo.extra?.getInt("isAD", 0)!! > 0)
        }
        return isAD
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getAudioDescriptionEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(getTvSetting().dtvAudioDescriptionOnOff)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setAudioDescriptionEnabled(enable: Boolean) {
        super.setAudioDescriptionEnabled(enable)
        getTvSetting().setDtvAudioDescriptionOnOff(enable)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getSpokenSubtitleEnabled(): Boolean {
        return getTvSetting().dtvSpokenSubtitleOnOff
    }
    @RequiresApi(Build.VERSION_CODES.P)
    private fun setSpokenSubtitleEnabled(enable: Boolean) {
        getTvSetting().dtvSpokenSubtitleOnOff = enable
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getCodecDolbySpecificAudioInfo(tvTrackInfo: TvTrackInfo): String {
        var dolbyType = ""

        try {
            val codecType = tvTrackInfo?.extra?.getInt("codecType", 0)

            if (codecType != null) {
                if (codecType >= 0 && codecType < CODEC_TYPE_ARRAY.size) {
                    val audioFormat = CODEC_TYPE_ARRAY[codecType]
                    if (audioFormat == TYPE_DOLBY_DIGITAL || audioFormat == TYPE_DOLBY_DIGITAL_PLUS
                            || audioFormat == TYPE_AC4) {
                        dolbyType = audioFormat
                    }
                }
            }
        } catch (e: Exception) {}

        return dolbyType
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getDefFaderValue(): Int {
        val mixLevel = getTvSetting().dtvAudioDescriptionMixLevel
        var newValue = 0
        when (mixLevel) {
            -15, -14, -13, -12, -11, -10, -9, -8 -> newValue = 0
            -7, -6, -5, -4, -3, -2, -1 -> newValue = 1
            0, 1, 2, 3, 4, 5 -> newValue = 2
            6, 7, 8, 9, 10 -> newValue = 3
            11, 12, 13, 14, 15 -> newValue = 4
        }
        return newValue
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setFaderValue(indexVal: Int) {
        var newValue = 0
        when (indexVal) {
            0 -> newValue = -15
            1 -> newValue = -7
            2 -> newValue = 0
            3 -> newValue = 7
            4 -> newValue = 15
        }
        getTvSetting().setDtvAudioDescriptionMixLevel(newValue)
    }

    override fun getAspectRatio(): Int {
        return aspectRatioActioner?.getAspectRatio()!!
    }

    override fun setAspectRatio(index: Int) {
        aspectRatioActioner?.setAspectRatio(index)
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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setPrimaryAudioLanguage(language: String, type: PrefType) {
        val langCode: Long = PreferredLanguage.getPreferredLanguage(language).langCode
        getTvSetting().setDtvPrimaryAudioLanguage(langCode)
        hbbTvInterface?.updateConfigurations()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getPrimaryAudioLanguage(type: PrefType): String? {
        val langCode = getTvSetting().dtvPrimaryAudioLanguage
        val language: PreferredLanguage = PreferredLanguage.getPreferredLanguage(langCode)
        return language.langCode639Part2
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getSecondaryAudioLanguage(type: PrefType): String? {
        val langCode = getTvSetting().dtvSecondaryAudioLanguage
        val language: PreferredLanguage = PreferredLanguage.getPreferredLanguage(langCode)
        return language.langCode639Part2
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setSecondaryAudioLanguage(language: String, type: PrefType) {
        val langCode: Long = PreferredLanguage.getPreferredLanguage(language).langCode
        getTvSetting().setDtvSecondaryAudioLanguage(langCode)
        hbbTvInterface?.updateConfigurations()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setPrimarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setPrimarySubtitleLanguage(language, type)

        val langCode: Long = PreferredLanguage.getPreferredLanguage(language).langCode
        getTvSetting().dtvPrimarySubtitleLanguage = langCode
        hbbTvInterface?.updateConfigurations()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setSecondarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setSecondarySubtitleLanguage(language, type)

        val langCode: Long = PreferredLanguage.getPreferredLanguage(language).langCode
        getTvSetting().dtvSecondarySubtitleLanguage = langCode
        hbbTvInterface?.updateConfigurations()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getPrimarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getPrimarySubtitleLanguage(type)

        val langCode: Long = getTvSetting().dtvPrimarySubtitleLanguage
        val language: PreferredLanguage = PreferredLanguage.getPreferredLanguage(langCode)
        return language.langCode639Part2
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getSecondarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getSecondarySubtitleLanguage(type)

        val langCode: Long = getTvSetting().dtvSecondarySubtitleLanguage
        val language: PreferredLanguage = PreferredLanguage.getPreferredLanguage(langCode)
        return language.langCode639Part2
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getHearingImpairedEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(getTvSetting().dtvSubtitleHardOfHearing)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setHearingImpairedEnabled(enable: Boolean) {
        getTvSetting().dtvSubtitleHardOfHearing = enable
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
        when (position) {
            SUBTITLE_SETTING_TYPE_BASIC -> {
                getTvSetting().dtvSubtitleOnOff = true
                getTvSetting().dtvSubtitleHardOfHearing = false
            }
            SUBTITLE_SETTING_TYPE_HEARING_IMPAIRED -> {
                getTvSetting().dtvSubtitleOnOff = true
                getTvSetting().dtvSubtitleHardOfHearing = true
            }
            else -> {
                getTvSetting().dtvSubtitleOnOff = false
                getTvSetting().dtvSubtitleHardOfHearing = false
            }
        }

        hbbTvInterface.updateConfigurations()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun enableSubtitles(enable: Boolean, type: PrefType) {
        if (type == PrefType.BASE) return super.enableSubtitles(enable, type)
        getTvSetting().dtvSubtitleOnOff = enable
        hbbTvInterface?.updateConfigurations()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getSubtitlesState(type: PrefType): Boolean {
        if (type == PrefType.BASE) return super.getSubtitlesState(type)
        return (!RtkConfigs.TvConfigs.SUPPORTED_ATSC_CLOSEDCAPTION && getTvSetting().dtvSubtitleOnOff)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getSubtitlesType(): Int {
        return when (getTvSetting().dtvSubtitleOnOff) {
            true -> {
                if (getTvSetting().dtvSubtitleHardOfHearing) SUBTITLE_SETTING_TYPE_HEARING_IMPAIRED
                else SUBTITLE_SETTING_TYPE_BASIC
            }
            else -> SUBTITLE_SETTING_TYPE_OFF
        }
    }

    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo) : Boolean {
        var hasHoH= false
        try {
            if (tvTrackInfo.extra?.getInt("hardHearing",0) == 1) {
                hasHoH = true
            }
        } catch (e: Exception) { }
        return hasHoH
    }

    override fun setGoogleLauncherBehaviour(context: Context, inputId: String) {
        GtvUtil.broadcastInputId(context = context, inputId = inputId)
    }

    override fun getCountryPreferences(
        preference: UtilsInterface.CountryPreference,
        defaultValue: Any?
    ): Any? {
        when (preference) {
            UtilsInterface.CountryPreference.SHOW_DEFAULT_PIN_TIP -> {
                return Build.VERSION.SDK_INT <= Build.VERSION_CODES.R
            }
            else -> {
                return defaultValue
            }
        }
        return defaultValue
    }
companion object {
    val mono_label = "Mono"
    val stereo_label = "Stereo"
    val dual_1 = "Dual I"
    val dual_2 = "Dual II"
    val dual_12 = "Dual I+II"
    val sap_mono_label = "SAP Mono"
    val sap_stereo_label = "SAP Stereo"
    val nicam_mono_label = "NICAM Mono"

    val mono = arrayListOf<String>(mono_label)
    val stereo = arrayListOf<String>(mono_label, stereo_label)
    val dual = arrayListOf<String>(dual_1, dual_2, dual_12)
    val nicam_dual = arrayListOf<String>(mono_label, dual_1, dual_2, dual_12)
    val sap_mono = arrayListOf<String>(mono_label, sap_mono_label)
    val sap_stereo = arrayListOf<String>(mono_label, stereo_label, sap_stereo_label)
    val nicam_mono = arrayListOf<String>(mono_label, nicam_mono_label)
}
    @RequiresApi(Build.VERSION_CODES.P)
    fun handleAnalogousAudioTracks(listType:Int): MutableList<String> {
        Log.d(Constants.LogTag.CLTV_TAG + "Analogous","handleAnalogousAudioTracks listType=$listType")
        val atvAudioTracks = mutableListOf<String>()
        when (listType) {
            1 -> atvAudioTracks.addAll(stereo)
            2 -> atvAudioTracks.addAll(dual)
            3 -> atvAudioTracks.addAll(nicam_dual)
            4 -> atvAudioTracks.addAll(sap_mono)
            5 -> atvAudioTracks.addAll(sap_stereo)
            6 -> atvAudioTracks.addAll(nicam_mono)
            0 -> atvAudioTracks.addAll(mono)
            else -> atvAudioTracks.addAll(mono)
        }
        Log.d(Constants.LogTag.CLTV_TAG + "Analogous", "handleAnalogousAudioTracks: ${atvAudioTracks.size}")
        return atvAudioTracks
    }

    override fun checkGuideIntent(intent: Intent?): Boolean {
        return uriFromGuide(parseIntentData(intent))
    }
    private fun uriFromGuide(uri: Uri?): Boolean {
        return uri?.let {URI_SCHEME_CUSTOMIZE == it.scheme && URI_HOST_GUIDE_BUTTON == it.host}?:false
    }
    private fun parseIntentData(intent: Intent?): Uri? {
        var uri: Uri? = intent?.data
        return uri
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getUsbDevices(): HashMap<String, String> {
        val map = hashMapOf<String, String>()
        var separator = ""
        var usbPaths = StringBuilder()
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

                usbPaths.append(separator).append(path)
                separator = ","

                if (getPvrStoragePath().isEmpty()) {
                    setPvrStoragePath(path)
                }

                directory?.let {
                    map[it] = item.getDescription(context)
                }
            }
            setUsbPath(usbPaths.toString())

        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return map
    }

    override fun setPvrStoragePath(path: String) {
        setPrefsValue(PVR_STORAGE_PATH, path)

        Settings.Global.putString(
            context.contentResolver, PREF_KEY_DVR_RECORD_PATH, path
        )
    }

    override fun getPvrStoragePath(): String {
//        return getPrefsValue(PVR_STORAGE_PATH, "").toString()

        var ret = Settings.Global.getString(
            context.contentResolver,
            PREF_KEY_DVR_RECORD_PATH
        )
        if (TextUtils.isEmpty(ret)) ret = ""
        return ret!!
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun isUsbFreeSpaceAvailable() : Boolean {
        var isUsbFileAccessible = true
        var isUsbFileNotTooLess = true
        val lessUsbMemory = 100L //less than 100MB free is when recording of event stops

        getUsbStorageFreeSize(object : IAsyncDataCallback<Long> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb file may not be available")
                isUsbFileAccessible = false
            }

            override fun onReceive(data: Long) {
                val freeMemory: Long = (data / 1024 / 1024)
                Log.i(Constants.LogTag.CLTV_TAG + TAG, "Usb free memory $freeMemory")
                if( ( freeMemory == 0L) || freeMemory < lessUsbMemory ){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb file may be full or very less freeMemory = $freeMemory")
                    isUsbFileNotTooLess = false
                }
            }
        })

        Log.i(Constants.LogTag.CLTV_TAG + TAG, "isUsbFreeSpaceAvailable: isUsbFileAccessible $isUsbFileAccessible, isUsbFileNotTooLess $isUsbFileNotTooLess")
        return isUsbFileAccessible && isUsbFileNotTooLess
    }

    private fun setUsbPath(path: String) {
        Settings.Global.putString(
            context.contentResolver,
            PREF_KEY_USB_PATHES,
            path
        )
    }

    private fun getUsbPath(): String? {
        var ret = Settings.Global.getString(
            context.contentResolver,
            PREF_KEY_USB_PATHES
        )
        if (TextUtils.isEmpty(ret)) ret = ""
        return ret
    }
}
