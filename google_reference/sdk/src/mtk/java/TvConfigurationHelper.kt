import android.content.Context
import android.media.tv.TvTrackInfo
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.sdk.BuildConfig
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.content_provider.ReferenceContract
import com.iwedia.cltv.sdk.entities.LanguageCodeMapper
import com.iwedia.cltv.sdk.ReferenceEvents
import com.mediatek.dm.*
import com.mediatek.twoworlds.tv.MtkTvBanner
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.MtkTvTVCallbackHandler
import com.mediatek.twoworlds.tv.MtkTvTimeshift
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase.ACFG_TSHIFT_MODE_MANUAL
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase.ACFG_TSHIFT_MODE_OFF
import utils.information_bus.Event
import utils.information_bus.InformationBus

class TvConfigurationHelper {

    interface FormattingProgressListener {
        fun reportProgress(visible: Boolean, statusText : String);
    }

    companion object {
        var primaryAudioLang: String = ""
        var secondaryAudioLang: String = ""
        var primarySubtitlesLang: String = ""
        var secondarySubtitlesLang: String = ""
        var TAG = "TvConfigurationHelper"
        var mContext : Context? = null
        var progressListener : FormattingProgressListener? = null
        const val CODEC_AUDIO_AC3 = "ac3"
        const val CODEC_AUDIO_AC3_ATSC = "ac3-atsc"
        const val CODEC_AUDIO_EAC3 = "eac3"
        const val CODEC_AUDIO_EAC3_ATSC = "eac3-atsc"
        const val CODEC_AUDIO_DTS = "dts"

        fun getPrimaryAudioLanguage(): String? {
            var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_LANGUAGE)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimaryAudioLanguage: $language")
            if(language == null) {
                language = LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")]
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimaryAudioLanguage replaced with: $language")
            }

            return language
        }

        fun getSecondaryAudioLanguage(): String? {
            var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_2ND_LANGUAGE)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSecondaryAudioLanguage: $language")
            if(language == null) {
                language = "eng"
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSecondaryAudioLanguage replaced with: $language")
            }

            return language
        }

        fun getPrimarySubtitleLanguage(): String? {
            var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage: $language")
            if(language == null) {
                language = LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")]
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage replaced with: $language")
            }

            return language
        }

        fun getSecondarySubtitleLanguage(): String? {
            var language = MtkTvConfig.getInstance().getLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG_2ND)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getSecondarySubtitleLanguage: $language")
            if(language == null) {
                language = "eng"
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPrimarySubtitleLanguage replaced with: $language")
            }
            return language
        }

        fun setPrimaryAudioLanguage(language: String) {
            MtkTvConfig.getInstance()
                .setLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_LANGUAGE, language)
            primaryAudioLang = language;
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setPrimaryAudioLanguage: $primaryAudioLang")
        }

        fun getVideoResolution(): String?{
            var resolution:String? = MtkTvBanner.getInstance().iptsRslt
            return resolution
        }

        fun setSecondaryAudioLanguage(language: String) {
            MtkTvConfig.getInstance()
                .setLanguage(MtkTvConfigType.CFG_GUI_AUD_LANG_AUD_2ND_LANGUAGE, language)
            secondaryAudioLang = language
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSecondaryAudioLanguage: $secondaryAudioLang")
        }

        fun setPrimarySubtitleLanguage(language: String) {
            MtkTvConfig.getInstance()
                .setLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG, language)
            primarySubtitlesLang = language
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setPrimarySubtitleLanguage: $primarySubtitlesLang")
        }

        fun setSecondarySubtitleLanguage(language: String) {
            MtkTvConfig.getInstance()
                .setLanguage(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_LANG_2ND, language)
            secondarySubtitlesLang = language
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setPrimarySubtitleLanguage: $secondarySubtitlesLang")
        }

        fun enableAudioDescription(enable: Boolean) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableAudioDescription: $enable")
            if (enable) {
                MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 2)
            } else {
                MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE, 0)
            }
        }

        fun getAudioDescriptionState() : Boolean {
            if(MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_AUD_AUD_TYPE) == 2) {
                return true
            }
            return false
        }

        fun enableHardOfHearing(enable: Boolean) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableHardOfHearing: $enable")
            if (enable) {
                MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR, 1)
            } else {
                MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR, 0)
            }
        }

        fun getHardOfHearingState(): Boolean {
            if(MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ATTR) == 1) {
                return true
            }
            return false
        }

        fun enableSubtitles(enable: Boolean) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableSubtitles: $enable")
            if (enable) {
                MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX, 1)
            } else {
                MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX, 0)
            }
        }

        fun getSubtitlesState(): Boolean {
            return MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_SUBTITLE_SUBTITLE_ENABLED_EX) != 0
        }

        fun updateAudioTracks() {
        }

        fun updateSubtitleTracks() {
        }

        fun isDolby(tvTrackInfo: TvTrackInfo): Boolean{
            Log.i(TAG, "isDolby: ${tvTrackInfo.extra.get("key_AudioDecodeType")}")
            if((Integer.parseInt(tvTrackInfo.extra.get("key_AudioDecodeType") as String) == 1)
                || (Integer.parseInt(tvTrackInfo.extra.get("key_AudioDecodeType") as String) == 2)
                || (Integer.parseInt(tvTrackInfo.extra.get("key_AudioDecodeType") as String) == 18)
                || (Integer.parseInt(tvTrackInfo.extra.get("key_AudioDecodeType") as String) == 23)) {
                return true
            } else {
                return false
            }
        }

        fun getDolby(tvTrackInfo: TvTrackInfo):String{
            return ""
        }
        
        fun setAudioType(position: Int) {
            if (position == 0) {
                enableAudioDescription(false)
            } else {
                enableAudioDescription(true)
            }
        }

        fun getAudioType(): Int {
            if(getAudioDescriptionState()) {
                return 1
            } else {
                return 0
            }
        }

        fun setSubtitlesEnabled(isEnabled: Boolean, updateSwitch:Boolean = true) {
            enableSubtitles(isEnabled)
        }

        fun getSubtitlesEnabled(): Boolean {
            return getSubtitlesState()
        }

        fun setSubtitlesType(position: Int, updateSwitch:Boolean = true) {
            if (position == 0) {
                enableHardOfHearing(false)
            }
            if (position == 1) {
                enableHardOfHearing(true)
            }
        }

        fun getSubtitlesType(): Int {
            if (getHardOfHearingState()) {
                return 1
            } else {
                return 0
            }
        }

        fun setTeletextDigitalLanguage(position: Int) {
            var ttxDigitalValue = 0

            if (position == 0){
                try{
                    ttxDigitalValue = LanguageCodeMapper.countryCodeToTxtDigitalLanguageMap[readInternalProviderData("current_country")]!!
                } catch (ex: Exception) {
                    Log.i(TAG, "setTeletextDecodeLanguage: Country Not Found")
                    ttxDigitalValue = 8
                }
            } else {
                try{
                    ttxDigitalValue = LanguageCodeMapper.ttxPositionToDigitalLanguageMap[position]!!
                } catch (ex: Exception) {
                    Log.i(TAG, "setTeletextDecodeLanguage: Language Not Found")
                    ttxDigitalValue = 8
                }
            }
            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DIGTL_ES_SELECT, ttxDigitalValue)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setTeletextDigitalLanguage: $ttxDigitalValue")
        }

        fun getTeletextDigitalLanguage(): Int {
            var ttxDigitalValue = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DIGTL_ES_SELECT)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTeletextDigitalLanguage: $ttxDigitalValue")
            return ttxDigitalValue
        }

        fun setTeletextDecodeLanguage(position: Int) {
            var ttxDecodeValue = 0

            if (position == 0){
                try{
                    ttxDecodeValue = LanguageCodeMapper.countryCodeToTxtDigitalLanguageMap[readInternalProviderData("current_country")]!!
                }catch (ex:Exception){
                    Log.i(TAG, "setTeletextDecodeLanguage: Country Not Found")
                    ttxDecodeValue = 0
                }
            } else {
                try{
                    ttxDecodeValue = LanguageCodeMapper.ttxPositionToDecodeLanguageMap[position]!!
                }catch (ex: Exception){
                    Log.i(TAG, "setTeletextDecodeLanguage: Language Not Found")
                    ttxDecodeValue = 0
                }
            }

            MtkTvConfig.getInstance().setConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DECODE_LANG, ttxDecodeValue)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setTeletextDigitalLanguage: $ttxDecodeValue")
        }

        fun getTeletextDecodeLanguage(): Int {
            var ttxDecodeValue = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_TTX_LANG_TTX_DECODE_LANG)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTeletextDecodeLanguage: $ttxDecodeValue")
            return ttxDecodeValue
        }

        private var timeShiftEnabled : Boolean = false
        private var usbDeviceReady = false
        private var waitingFormat = false
        private var waitingAttach = false
        private var dm: DeviceManager = DeviceManager.getInstance()
        private var mMountPointPath : String = ""

        fun registerProgressListener(listener: FormattingProgressListener) {
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
                        InformationBus.submitEvent(Event(ReferenceEvents.DISK_READY_EVENT))
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

        fun setDeafaultLanguages(){
            var defaultLanguage = LanguageCodeMapper.countryCodeToLanguageCodeMap[readInternalProviderData("current_country")]
            if(defaultLanguage != null) {
                setPrimaryAudioLanguage(defaultLanguage)
                setSecondaryAudioLanguage("eng")
                setPrimarySubtitleLanguage(defaultLanguage)
                setSecondarySubtitleLanguage("eng")
                setTeletextDigitalLanguage(0)
                setTeletextDecodeLanguage(0)
            }
        }
        
        fun attachUSB() {
            if(usbDeviceReady == false) {
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

        fun formatUSB() {
            if(usbDeviceReady == false) {
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


        var mountListener : DeviceManagerListener = DeviceManagerListener { event ->
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

        @Synchronized
        fun enableTimeshift(enable: Boolean, context: Context) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "enableTimeshift: $enable")

            mContext = context;

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

        fun setAspectRatio(index : Int) {
        }

        fun getAspectRatio() : Int {
            return 0
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean{
            return tvTrackInfo!!.isAudioDescription
        }

        fun setAntennaPower(enable: Boolean) {
        }

        fun isAntennaPowerEnabled() : Boolean {
            return false
        }

        fun isGretzkyBoard(): Boolean = false
    }
}