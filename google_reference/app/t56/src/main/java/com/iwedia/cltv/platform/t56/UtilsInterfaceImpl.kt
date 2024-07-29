package com.iwedia.cltv.platform.t56

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.iwedia.cltv.platform.base.UtilsInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.t56.language.LanguageMapperImpl
import com.mediatek.dm.DeviceManager
import com.mediatek.dm.DeviceManagerEvent
import com.mediatek.dm.DeviceManagerListener
import com.mediatek.dm.MountPoint
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.MtkTvTVCallbackHandler
import com.mediatek.twoworlds.tv.MtkTvTimeshift
import com.mediatek.twoworlds.tv.MtkTvTimeshiftBase
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase.ACFG_TSHIFT_MODE_MANUAL
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase.ACFG_TSHIFT_MODE_OFF
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase.CFG_VIDEO_VID_BLUE_MUTE
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_HEARING_IMPAIRED_CLEAN
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_RESERVED
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_AD
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase.AUD_MIX_TYPE_INDEPENDENT
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase.AUD_MIX_TYPE_SUPPLEMENTARY
import com.mediatek.twoworlds.tv.model.TvProviderAudioTrackBase.AUD_TYPE_VISUAL_IMPAIRED
import mediatek.sysprop.VendorProperties
import java.util.Date


class UtilsInterfaceImpl(private val context: Context, textToSpeechModule: TTSInterface): UtilsInterfaceBaseImpl(context, textToSpeechModule) {

    private var primaryAudioLang: String = ""
    private var secondaryAudioLang: String = ""
    private var primarySubtitlesLang: String = ""
    private var secondarySubtitlesLang: String = ""
    private var progressListener : UtilsInterface.FormattingProgressListener? = null

    val CODEC_AUDIO_AC3 = "ac3"
    val CODEC_AUDIO_EAC3 = "eac3"
    val CODEC_AUDIO_DTS = "dts"
    val TAG = "UtilsInterfaceImpl"
    private var timeShiftEnabled : Boolean = false
    private var usbDeviceReady = false
    private var waitingFormat = false
    private var waitingAttach = false
    private var dm: DeviceManager = DeviceManager.getInstance()
    private var mMountPointPath : String = ""
    private var defaultAudioViValue: Int = 0
    lateinit var timeInterface: TimeInterface
    override var languageCodeMapper : LanguageMapperInterface = LanguageMapperImpl()

    override var jsonPath: String = "assets/OemCustomizationJsonFolder/OemCustomizationMk5.json"

    private var isAdOn = false
    private var isHiOn = false

    override fun getLanguageMapper() : LanguageMapperInterface? {
        return languageCodeMapper
    }

    override fun isUsbConnected(): Boolean {
        return isUsbFileAvailable()
    }

    init {
        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "MTK Region: "+getRegion().name)
        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "MTK CountryCode: "+getCountryCode())
    }

    override fun getPrimaryAudioLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getPrimaryAudioLanguage(type)
        var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_LANGUAGE)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimaryAudioLanguage: $language")
        if(language == null) {
            language = languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimaryAudioLanguage replaced with: $language")
        }

        return language
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

    override fun getSecondaryAudioLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getSecondaryAudioLanguage(type)
        var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_2ND_LANGUAGE)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSecondaryAudioLanguage: $language")
        if(language == null) {
            language = "eng"
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSecondaryAudioLanguage replaced with: $language")
        }

        return language
    }

    override fun getPrimarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getPrimarySubtitleLanguage(type)
        var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage: $language")
        if(language == null) {
            language = languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage replaced with: $language")
        }
        return language
    }

    override fun getSecondarySubtitleLanguage(type: PrefType): String? {
        if (type == PrefType.BASE) return super.getSecondarySubtitleLanguage(type)
        var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG_2ND)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSecondarySubtitleLanguage: $language")
        if(language == null) {
            language = "eng"
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage replaced with: $language")
        }
        return language
    }

    override fun setPrimaryAudioLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setPrimaryAudioLanguage(language, type)
        MtkTvConfig.getInstance()
            .setLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_LANGUAGE, language)
        primaryAudioLang = language;
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setPrimaryAudioLanguage: $primaryAudioLang")
    }

    override fun setSecondaryAudioLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setSecondaryAudioLanguage(language, type)
        MtkTvConfig.getInstance()
            .setLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_2ND_LANGUAGE, language)
        secondaryAudioLang = language
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSecondaryAudioLanguage: $secondaryAudioLang")
    }

    override fun setPrimarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setPrimarySubtitleLanguage(language, type)
        MtkTvConfig.getInstance()
            .setLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG, language)
        primarySubtitlesLang = language
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setPrimarySubtitleLanguage: $primarySubtitlesLang")
    }

    override fun setSecondarySubtitleLanguage(language: String, type: PrefType) {
        if (type == PrefType.BASE) return super.setSecondarySubtitleLanguage(language, type)
        MtkTvConfig.getInstance()
            .setLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG_2ND, language)
        secondarySubtitlesLang = language
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setPrimarySubtitleLanguage: $secondarySubtitlesLang")
    }

    override fun enableAudioDescription(enable: Boolean) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableAudioDescription: $enable")
        if (enable) {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 2)
        } else {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 0)
        }
    }

    override fun getAudioDescriptionState(): Boolean {
        if(MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE) == 2) {
            return true
        }
        return false
    }

    override fun enableHardOfHearing(enable: Boolean) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableHardOfHearing: $enable")
        if (enable) {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR, 1)
        } else {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR, 0)
        }
    }

    override fun getHardOfHearingState(): Boolean {
        if(MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR) == 1) {
            return true
        }
        return false
    }

    override fun enableSubtitles(enable: Boolean, type: PrefType) {
        if (type == PrefType.BASE) return super.enableSubtitles(enable, type)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableSubtitles: $enable")
        if (enable) {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX, 1)
        } else {
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX, 0)
        }
    }

    override fun getSubtitlesState(type: PrefType): Boolean {
        if (type == PrefType.BASE) return super.getSubtitlesState(type)
        return MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX) != 0
    }

    override fun setAudioType(position: Int) {
        if (position == 0) {
            enableAudioDescription(false)
        } else {
            enableAudioDescription(true)
        }
    }

    override fun getAudioType(callback: IAsyncDataCallback<Int>?) {
        callback?.onReceive(
            if(getAudioDescriptionState()) {
                1
            } else {
                0
            }
        )
    }

    override fun getSubtitleTypeDisplayNames(type: PrefType): MutableList<String> {
        if (type == PrefType.BASE) return super.getSubtitleTypeDisplayNames(type)
        return mutableListOf(
            getStringValue("audio_type_normal"),
            getStringValue("subtitle_type_hearing_impaired"))
    }

    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
        if (position == 0) {
            enableHardOfHearing(false)
        }
        if (position == 1) {
            enableHardOfHearing(true)
        }
    }

    override fun getSubtitlesType(): Int {
        return if (getHardOfHearingState()) {
            1
        } else {
            0
        }
    }

    override fun setTeletextDigitalLanguage(position: Int) {
        var ttxDigitalValue = 0

        if (position == 0){
            try{
                ttxDigitalValue = languageCodeMapper.getTxtDigitalLanguageMapByCountryCode(readInternalProviderData("current_country"))!!
            } catch (ex: Exception) {
                Log.i(TAG, "setTeletextDecodeLanguage: Country Not Found")
                ttxDigitalValue = 8
            }
        } else {
            try{
                ttxDigitalValue = languageCodeMapper.getTxtDigitalLanguageMapByPosition(position)!!
            } catch (ex: Exception) {
                Log.i(TAG, "setTeletextDecodeLanguage: Language Not Found")
                ttxDigitalValue = 8
            }
        }
        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DIGTL_ES_SELECT, ttxDigitalValue)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setTeletextDigitalLanguage: $ttxDigitalValue")
    }

    override fun getTeletextDigitalLanguage(): Int {
        var ttxDigitalValue = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DIGTL_ES_SELECT)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTeletextDigitalLanguage: $ttxDigitalValue")
        return ttxDigitalValue
    }

    override fun setTeletextDecodeLanguage(position: Int) {
        var ttxDecodeValue = 0

        if (position == 0){
            try{
                ttxDecodeValue = languageCodeMapper.getTxtDigitalLanguageMapByCountryCode(readInternalProviderData("current_country"))!!
            }catch (ex:Exception){
                Log.i(TAG, "setTeletextDecodeLanguage: Country Not Found")
                ttxDecodeValue = 0
            }
        } else {
            try{
                ttxDecodeValue = languageCodeMapper.getTxtDigitalLanguageMapByPosition(position)!!
            }catch (ex: Exception){
                Log.i(TAG, "setTeletextDecodeLanguage: Language Not Found")
                ttxDecodeValue = 0
            }
        }

        MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DECODE_LANG, ttxDecodeValue)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setTeletextDigitalLanguage: $ttxDecodeValue")
    }

    override fun getTeletextDecodeLanguage(): Int {
        var ttxDecodeValue = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DECODE_LANG)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTeletextDecodeLanguage: $ttxDecodeValue")
        return ttxDecodeValue
    }

    override fun setDeafaultLanguages() {
        var defaultLanguage = languageCodeMapper.getLanguageCodeByCountryCode(readInternalProviderData("current_country"))
        if(defaultLanguage != null) {
            setPrimaryAudioLanguage(defaultLanguage)
            setSecondaryAudioLanguage("eng")
            setPrimarySubtitleLanguage(defaultLanguage)
            setSecondarySubtitleLanguage("eng")
            setTeletextDigitalLanguage(0)
            setTeletextDecodeLanguage(0)
        }
    }

    override fun registerFormatProgressListener(listener: UtilsInterface.FormattingProgressListener) {
        progressListener = listener
    }

    private val mTimeshiftHandler: MtkTvTVCallbackHandler =
        object : MtkTvTVCallbackHandler() {
            val TIMESHIFT_NTFY_CREATE_FILE_BEGIN = 6
            val TIMESHIFT_NTFY_CREATE_FILE_PROGRESS = 7
            val TIMESHIFT_NTFY_CREATE_FILE_OK = 8
            val TIMESHIFT_NTFY_CREATE_FILE_FAIL = 9
            val TIMESHIFT_NTFY_SPEED_TEST_BEGIN = 10
            val TIMESHIFT_NTFY_SPEED_TEST_PROGRESS = 11
            val TIMESHIFT_NTFY_SPEED_TEST_LOW = 12
            val TIMESHIFT_NTFY_SPEED_TEST_OK = 13
            val TIMESHIFT_NTFY_SPEED_TEST_FAIL = 14
            val TIMESHIFT_NTFY_FORMATING = 40

            override fun notifyTimeshiftNotification(updateType: Int, argv1: Long): Int {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "(TimeshiftCallbackHandler) notifyTimeshiftNotification type=" + updateType + "argv1=" + argv1
                );
                when (updateType) {
                    TIMESHIFT_NTFY_CREATE_FILE_BEGIN -> {
                        progressListener?.reportProgress(false,"")
                        progressListener?.reportProgress(true,"Please wait, preparing file system...")
                    }
                    TIMESHIFT_NTFY_CREATE_FILE_PROGRESS -> {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "(TimeshiftCallbackHandler) TIMESHIFT_NTFY_CREATE_FILE_PROGRESS argv1=$argv1"
                        )
                    }
                    TIMESHIFT_NTFY_CREATE_FILE_OK -> {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "(TimeshiftCallbackHandler) TIMESHIFT_NTFY_CREATE_FILE_OK argv1=$argv1"
                        )
                    }
                    TIMESHIFT_NTFY_CREATE_FILE_FAIL -> {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "(TimeshiftCallbackHandler) TIMESHIFT_NTFY_CREATE_FILE_FAIL argv1=$argv1"
                        )
                        progressListener?.reportProgress(false,"")
                        progressListener?.reportProgress(true,"Creating file system failed")
                    }
                    TIMESHIFT_NTFY_SPEED_TEST_BEGIN -> {
                        progressListener?.reportProgress(false,"")
                        progressListener?.reportProgress(true,"Please wait, testing device speed...")
                    }

                    TIMESHIFT_NTFY_SPEED_TEST_PROGRESS -> {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "(TimeshiftCallbackHandler) TIMESHIFT_NTFY_SPEED_TEST_PROGRESS argv1=$argv1"
                        )
                    }
                    TIMESHIFT_NTFY_SPEED_TEST_OK -> {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "(TimeshiftCallbackHandler) TIMESHIFT_NTFY_SPEED_TEST_OK argv1=$argv1"
                        )
                        InformationBus.informationBusEventListener?.submitEvent(Events.DISK_READY_EVENT)
                        progressListener?.reportProgress(false,"")
                        progressListener?.reportProgress(true,"Filesystem ready, device speed ${argv1/(1024*1024)}MB/s")
                    }
                    TIMESHIFT_NTFY_SPEED_TEST_FAIL -> {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "(TimeshiftCallbackHandler) TIMESHIFT_NTFY_SPEED_TEST_FAIL argv1=$argv1"
                        )
                        progressListener?.reportProgress(false,"")
                        progressListener?.reportProgress(true,"Device speed test failed")
                    }
                    TIMESHIFT_NTFY_SPEED_TEST_LOW -> {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "(TimeshiftCallbackHandler) TIMESHIFT_NTFY_SPEED_TEST_LOW argv1=$argv1"
                        )
                    }
                    else -> {}
                }
                return 0
            }
        }

    private fun attachUSB() {
        if(!usbDeviceReady) {
            waitingAttach = true
            return
        }

        var mountPath = mMountPointPath
        var path: String = mountPath + "/"
        path = path.replace("storage", "mnt/media_rw")

        var mountPoint: MountPoint = dm.getMountPoint(mountPath)

        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Attach volume label ${mountPoint.mVolumeLabel} : ${mountPoint.mMountPoint} ${mountPoint.mDeviceName} ${mountPoint.mDiskName} ${mountPoint.mDrvName}"
        )

        progressListener?.reportProgress(true, "Please wait, checking file system...")

        Thread.sleep(5000);

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "USB creating timeshift files $mountPath $mMountPointPath")
        val value = MtkTvTimeshift.getInstance().registerDevice(
            path,
            1024 * 1024 * 4 * 1024L
        )

        if(value != 0) {
            progressListener?.reportProgress(false, "")
            progressListener?.reportProgress(true, "Failed creating timeshift files")
        } else {
            progressListener?.reportProgress(false, "")
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "USB End creating timeshift files return value=$value")
    }

    private fun formatUSB() {
        if(!usbDeviceReady) {
            waitingFormat = true
            return
        }

        progressListener?.reportProgress(true,"Please wait, formatting device...")

        var mountPath = mMountPointPath
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "USB mount path $mountPath $mMountPointPath")
        var mountPoint: MountPoint = dm.getMountPoint(mountPath)

        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "Volume label ${mountPoint.mVolumeLabel} : ${mountPoint.mMountPoint} ${mountPoint.mDeviceName} ${mountPoint.mDiskName} ${mountPoint.mDrvName}"
        )

        Thread.sleep(4000);
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Unmounting device : ${mountPoint.mDeviceName}")
        dm.umountVol(mountPoint.mDeviceName);
        Thread.sleep(4000);
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Formating $mountPath")
        var resultFormat: Int = dm.formatVol(
            "fat32",
            mountPoint.mDeviceName
        );
        Thread.sleep(4000);
        if (resultFormat == 0) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "USB formated")
            progressListener?.reportProgress(false, "")
            progressListener?.reportProgress(true,"Please wait, mounting device...")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "USB mounting:${mountPoint.mDeviceName}")
            dm.mountVol(mountPoint.mDeviceName);
            Thread.sleep(4000);
            var path = ""
            dm.mountPointList.forEach() {
                if(it.mDeviceName == mountPoint.mDeviceName) {
                    path = it.mMountPoint+"/"
                }
            }
            path = path.replace("storage", "mnt/media_rw")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "USB creating timeshift files $path")

            Thread.sleep(1000);
            val value = MtkTvTimeshift.getInstance().registerDevice(
                path,
                1024 * 1024 * 4 * 1024L
            )
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "USB End creating timeshift files return value=$value")
            if(value != 0) {
                progressListener?.reportProgress(false, "")
                progressListener?.reportProgress(true, "Device format failed")
            } else {
                progressListener?.reportProgress(false, "")
            }
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Format failed $resultFormat")
            progressListener?.reportProgress(false, "")
            progressListener?.reportProgress(true,"Device format failed")
            Thread.sleep(4000);
            progressListener?.reportProgress(false,"")
        }
    }

    private var mountListener : DeviceManagerListener = DeviceManagerListener { event ->
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "device listener: device name: ${event?.inputDevName}, product name: ${event?.productName}, mount point: ${event?.mountPointPath}, device path: ${event?.devicePath} event type:${event?.type}"
        )
        if (event?.type == DeviceManagerEvent.mounted) {
            usbDeviceReady = true
            mMountPointPath = event.mountPointPath
            if(waitingFormat) {
                Thread { formatUSB() }.start()
            }
            if(waitingAttach) {
                Thread { attachUSB() }.start()
            }
            waitingFormat = false
            waitingAttach = false
        } else if(event?.type == DeviceManagerEvent.umounted) {
            usbDeviceReady = false
            waitingFormat = false
            waitingAttach = false
            mMountPointPath = ""
        } else if(event?.type == DeviceManagerEvent.disconnected) {
            usbDeviceReady = false
            waitingFormat = false
            waitingAttach = false
            mMountPointPath = ""
        }
    }

    override fun enableTimeshift(enable: Boolean) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableTimeshift: $enable")

        if(timeShiftEnabled == enable) {
            return;
        }

        timeShiftEnabled = enable

        MtkTvTimeshift.getInstance().setAutoRecord(enable)

        if (enable) {
            MtkTvConfig.getInstance().setConfigValue(
                MtkTvConfigType.CFG_RECORD_REC_TSHIFT_MODE,
                ACFG_TSHIFT_MODE_MANUAL
            );
            dm.addListener(mountListener)
        } else {
            MtkTvConfig.getInstance().setConfigValue(
                MtkTvConfigType.CFG_RECORD_REC_TSHIFT_MODE,
                ACFG_TSHIFT_MODE_OFF
            );
            dm.removeListener(mountListener)
        }
    }

    private fun isUsbFileAvailable() : Boolean {
        var deviceStatus : Int = MtkTvTimeshift.getInstance().deviceStatus
        if(MtkTvTimeshiftBase.TimeshiftDeviceStatus.values()[deviceStatus] != MtkTvTimeshiftBase.TimeshiftDeviceStatus.TIMESHIFT_DEV_STATUS_READY) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Usb file not ready, status: $deviceStatus")
            return false
        }
        return true
    }

    //todo remove utilsInterface calls and replace functions from utils to player
    override fun getDolby(tvTrackInfo: TvTrackInfo): String {
        if (tvTrackInfo.extra != null) {
            when (tvTrackInfo.extra.get("key_AudioEncodeType") as String) {
                "1" -> return CODEC_AUDIO_AC3
                "12" -> return CODEC_AUDIO_EAC3
                "26" -> return CODEC_AUDIO_DTS
            }
        }
        return ""
    }

    override fun getCodecDolbySpecificAudioInfo(tvTrackInfo: TvTrackInfo): String {
        var info = ""
        var dolbyType = getDolby(tvTrackInfo)
        if(!dolbyType.isNullOrEmpty()){
            info = when (dolbyType) {
                CODEC_AUDIO_EAC3 -> info.plus(DOLBY_AUDIO_EAC3_NAME_TYPE)
                CODEC_AUDIO_AC3 -> info.plus(DOLBY_AUDIO_AC3_NAME_TYPE)
                CODEC_AUDIO_DTS -> info.plus(AUDIO_DTS_TYPE)
                else -> info
            }
        }
        return info
    }

    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo) : Boolean {
        var hasHoHKey= false
        try {
            if (tvTrackInfo.extra.getString("key_HearingImpaired")?.toBoolean() == true) {
                hasHoHKey = true
            }
        } catch (e: java.lang.Exception) { }
        return hasHoHKey
    }

    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        var audioDescriptionEnable : Boolean = false
        if(audioIsMaskStream(tvTrackInfo) || (audIsIndependentVisualImpairedAudio(tvTrackInfo))
            || audIsMixableVisualImpairedAudio(tvTrackInfo) ){
            audioDescriptionEnable = true
        }
        Log.i(TAG, " hasAudioDescription : $audioDescriptionEnable ")
        return audioDescriptionEnable
    }

    private fun audioIsMaskStream( tvTrackInfo: TvTrackInfo) : Boolean{
        var audioDescriptionEnable = false
        var type: String =  ""
        var eClass: String = ""

        if (null != tvTrackInfo && null != tvTrackInfo.extra) {
            if (tvTrackInfo.extra.get("key_AudioType") != null) {
                type = tvTrackInfo.extra.get("key_AudioType") as String
            }
            if (tvTrackInfo.extra.get("key_AudioMixType") != null) {
                eClass = tvTrackInfo.extra.get("key_AudioEditorialClass") as String
            }

            if (!eClass.isNullOrEmpty()) {
                if (AUD_EDITORIAL_CLASS_HEARING_IMPAIRED_CLEAN == Integer.parseInt(eClass)) {
                    if (!eClass.isNullOrEmpty() && TvProviderAudioTrackBase.AUD_TYPE_HEARING_IMPAIRED == Integer.parseInt(type)) {
                        audioDescriptionEnable = true
                    }
                }
            }
        }

        return audioDescriptionEnable
    }

    private fun audIsMixableVisualImpairedAudio( tvTrackInfo: TvTrackInfo) : Boolean{
        var isMixable = false
        var type: String =  ""
        var mixtype: String = ""
        var eClass: String = ""

        if (null != tvTrackInfo && null != tvTrackInfo.extra) {
            if (tvTrackInfo.extra.get("key_AudioType") != null) {
                type = tvTrackInfo.extra.get("key_AudioType") as String
            }
            if(tvTrackInfo.extra.get("key_AudioMixType") != null){
                mixtype = tvTrackInfo.extra.get("key_AudioMixType") as String
            }
            if (tvTrackInfo.extra.get("key_AudioEditorialClass") != null) {
                eClass = tvTrackInfo.extra.get("key_AudioEditorialClass") as String
            }

            if (!mixtype.isNullOrEmpty() && !eClass.isNullOrEmpty()) {

                if (AUD_MIX_TYPE_SUPPLEMENTARY.toString() == mixtype) {
                    if (AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_AD.toString() == eClass ||
                        AUD_EDITORIAL_CLASS_RESERVED.toString() == eClass && AUD_TYPE_VISUAL_IMPAIRED.toString() == type
                    ) {
                        isMixable = true
                    }
                } else if (AUD_MIX_TYPE_INDEPENDENT.toString() != mixtype && AUD_TYPE_VISUAL_IMPAIRED.toString() == type) {
                    isMixable = true
                }
            }
        }

        return isMixable
    }

    private fun audIsIndependentVisualImpairedAudio( tvTrackInfo: TvTrackInfo) : Boolean{
        var bIsIndependent = false
        var type: String =  ""
        var mixtype: String = ""
        var eClass: String = ""

        if (null != tvTrackInfo && null != tvTrackInfo.extra) {
            if (tvTrackInfo.extra.get("key_AudioType") != null) {
                type = tvTrackInfo.extra.get("key_AudioType") as String
            }
            if (tvTrackInfo.extra.get("key_AudioMixType") != null) {
                mixtype = tvTrackInfo.extra.get("key_AudioMixType") as String
            }
            if (tvTrackInfo.extra.get("key_AudioEditorialClass") != null) {
                eClass = tvTrackInfo.extra.get("key_AudioEditorialClass") as String
            }
            if (!mixtype.isNullOrEmpty() && !eClass.isNullOrEmpty() ) {
                if (AUD_MIX_TYPE_INDEPENDENT.toString() == mixtype) {
                    if (AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_AD.toString().equals(eClass) ||
                        AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE.toString()
                            .equals(eClass) ||
                        AUD_EDITORIAL_CLASS_RESERVED.toString().equals(eClass) &&
                        AUD_TYPE_VISUAL_IMPAIRED.toString().equals(type)
                    ) {
                        bIsIndependent = true;
                    }
                }
            }
        }
        return bIsIndependent
    }


    override fun getRegion(): Region {
        return when (VendorProperties.mtk_system_marketregion().orElse("")) {
            "us" -> Region.US
            "eu" -> Region.EU
            "sa" -> Region.SA
            "cn" -> Region.CN
            else -> super.getRegion()
        }
    }

    override fun getCountryCode(): String {
        val countryISOMapping = mapOf(
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

        val countryCode = MtkTvConfig.getInstance().country
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "country code $countryCode")

        if (countryCode.length == 2) return countryCode

        return countryISOMapping[countryCode] ?: super.getCountryCode()
    }

    override fun getCountry(): String {
        return MtkTvConfig.getInstance().country
    }

    override fun getParentalPin(): String{
        return MtkTvConfig.getInstance().getConfigString(MtkTvConfigTypeBase.CFG_PWD_PASSWORD)
    }

    override fun getAudioDescriptionEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE) == 2)
    }

    override fun getHearingImpairedEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE) == 1)
    }

    override fun setAudioDescriptionEnabled(enable: Boolean) {
        isAdOn = enable
        if(enable){
            isHiOn = false
        }
        if(isAdOn){
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 2)
        }else if(!isHiOn){
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 0)
        }
    }

    override fun setHearingImpairedEnabled(enable: Boolean) {
        isHiOn = enable
        if(enable){
            isAdOn = false
        }
        if(isHiOn){
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 1)
        }else if(!isAdOn){
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 0)
        }
    }

    override fun setParentalPin(pin: String) : Boolean {
        MtkTvConfig.getInstance().setConfigString(MtkTvConfigTypeBase.CFG_PWD_PASSWORD, pin)
        context.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit().putBoolean(PREFS_KEY_IS_PARENTAL_PIN_CHANGED, true).apply()
        return true
    }

    override fun getDefFaderValue(): Int {
        val defFadeValueIndex = (MtkTvConfig.getInstance()
            .getConfigValue(MtkTvConfigType.CFG_AUD_AD_FADER_CTRL))
        return when (defFadeValueIndex) {
            0 -> 0
            25 -> 1
            50 -> 2
            75 -> 3
            100 -> 4
            else -> 0
        }
    }

    override fun getVolumeValue(): Int {
        return MtkTvConfig.getInstance()
            .getConfigValue(MtkTvConfigTypeBase.CFG_AUD_AUD_AD_VOLUME)
    }

    override fun getAudioSpeakerState(): Boolean {
        return (MtkTvConfig.getInstance()
            .getConfigValue(MtkTvConfigType.CFG_AUD_AUD_AD_SPEAKER) == 1)
    }

    override fun getAudioHeadphoneState(): Boolean {
        return (MtkTvConfig.getInstance()
            .getConfigValue(MtkTvConfigType.CFG_AUD_AUD_AD_HDPHONE) == 1)
    }

    override fun getAudioPaneState(): Boolean {
        return (MtkTvConfig.getInstance()
            .getConfigValue(MtkTvConfigType.CFG_AUD_AUD_AD_FADE_PAN) == 1)
    }

    override fun getAudioForVi() : Int {
        return defaultAudioViValue
    }

    override fun setFaderValue(newValueIndex: Int) {
        var newValue = 0
        when (newValueIndex) {
            0 -> newValue = 0
            1 -> newValue = 25
            2 -> newValue = 50
            3 -> newValue = 75
            4 -> newValue = 100
        }
        MtkTvConfig.getInstance()
            .setConfigValue(MtkTvConfigType.CFG_AUD_AD_FADER_CTRL, newValue)
    }

    override fun setVisuallyImpairedAudioValue(newValueIndex: Int) {
        MtkTvConfig.getInstance().setConfigValue(
            MtkTvConfigType.CFG_MENU_AUDIOINFO_SET_SELECT, newValueIndex)
    }

    override fun setVisuallyImpairedValues(position: Int, enabled: Boolean) {
        var i = 0
        if (enabled) {
            i = 1
        }
        if (position == 1) {
            MtkTvConfig.getInstance()
                .setConfigValue(MtkTvConfigType.CFG_AUD_AUD_AD_SPEAKER, i)
        } else if (position == 2) {
            MtkTvConfig.getInstance()
                .setConfigValue(MtkTvConfigType.CFG_AUD_AUD_AD_HDPHONE, i)
        } else {
            MtkTvConfig.getInstance()
                .setConfigValue(MtkTvConfigType.CFG_AUD_AUD_AD_FADE_PAN, i)
        }
    }

    override fun setVolumeValue(value: Int) {

        if (value >= getMin(MtkTvConfigTypeBase.CFG_AUD_AUD_AD_VOLUME) && value <= getMax(
                MtkTvConfigTypeBase.CFG_AUD_AUD_AD_VOLUME)) {
            MtkTvConfig.getInstance()
                .setConfigValue(MtkTvConfigTypeBase.CFG_AUD_AUD_AD_VOLUME, value)
        } else {
            MtkTvConfig.getInstance()
                .setConfigValue(MtkTvConfigTypeBase.CFG_AUD_AUD_AD_VOLUME, value)
        }
    }

    private fun getMin(itemID: String?): Int {
        val value: Int = MtkTvConfig.getInstance().getMinMaxConfigValue(itemID)
        return MtkTvConfig.getMinValue(value)
    }

    private fun getMax(itemID: String): Int {
        val value: Int = MtkTvConfig.getInstance().getMinMaxConfigValue(itemID)
        return MtkTvConfig.getMaxValue(value)
    }

    override fun enableBlueMute(enable: Boolean) {
        MtkTvConfig.getInstance().setConfigValue(CFG_VIDEO_VID_BLUE_MUTE, if (enable) 1 else 0)
    }

    override fun getBlueMuteState(): Boolean {
        return MtkTvConfig.getInstance().getConfigValue(CFG_VIDEO_VID_BLUE_MUTE)==1
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
        //value = 2 -> 15 minutes
        var value = 2
        if (Settings.Global.getString(context.contentResolver, "no_signal_auto_power_off") != null &&
            Settings.Global.getString(context.contentResolver, "no_signal_auto_power_off") != "") {
            //mtk database: 0 = off, 1 = 5 minutes, 2 = 10 minutes, 3 = 15 minutes, 4 = 30 minutes, 5 = 60 minutes
            //cltv database: 0 = 5 minutes, 1 = 10 minutes, 2 = 15 minutes, 3 = 30 minutes, 4 = 60 minutes
            value = Settings.Global.getInt(context.contentResolver, "no_signal_auto_power_off") - 1
            if (value == -1) {
                value = 2
            }
        }
        return value

    }

    override fun getIsPowerOffEnabled(): Boolean {
        return getPrefsValue("no_signal_auto_power_off_enable", true) as Boolean
    }

    override fun noSignalPowerOffChanged(isEnabled: Boolean){
        setPrefsValue("no_signal_auto_power_off_enable", isEnabled)
    }

    override fun getPowerOffTime(): Any {
        return getPrefsValue("no_signal_auto_power_off", "duration_15_minutes") as String
    }

    override fun setPowerOffTime(value: Int, time: String) {
        setPrefsValue("no_signal_auto_power_off", time)
    }

    override fun isUsbFormatEnabled(): Boolean {
        return false
    }

    private fun setTvSettingsTranslucentFlag(translucent: Boolean) {
        try {
            val uri = Uri.parse("content://com.mediatek.tv.setting.partnercustomizer/translucent")
            val contentValues = ContentValues()
            val translucentStr = "main_settings_translucent"
            contentValues.put(translucentStr, translucent)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "main_settings_translucent = $translucent")
            context.getContentResolver().update(uri, contentValues, null, null)
        } catch (e: java.lang.Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "setTvSettingsTranslucentFlag exception : " + e.localizedMessage)
        }
    }

    override fun setApplicationRunningInBackground(running : Boolean) {
        //MTK hack so that application is running in background when overlapped by settings screen
        setTvSettingsTranslucentFlag(running)
    }

    override fun startScanChannelsIntent() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startScanChannelsIntent: opening channel scan in settings")
        context.startActivity(
            Intent()
                .apply {
                    setClassName(Constants.ChannelConstants.COM_ANDROID_TV_SETTINGS, Constants.ChannelConstants.MEDIATEK_TV_SETTINGS_CHANNEL_ACTIVITY_T56)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
        )
    }

}