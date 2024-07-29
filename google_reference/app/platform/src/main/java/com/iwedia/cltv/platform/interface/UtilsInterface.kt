package com.iwedia.cltv.platform.`interface`

import android.content.Context
import android.content.Intent
import android.media.tv.TvTrackInfo
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.recording.Recording
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Channel data provider interface
 *
 * @author Dejan Nadj
 */
interface UtilsInterface: ToastInterface {
    companion object {
        val PREFS_TAG = "LiveTVPrefs"
        val PREFS_TAG_LOCKED_CHANNEL_IDS = "LockedChannelIds"
        val APPLICATION_MODE = "application_mode"
        const val STORAGE_PATH = "com.mediatek.dtv.tvinput.dvbtuner.STORAGE_ID"
    }

    var jsonPath : String
    var stringTranslationListener: StringTranslationListener?
    interface FormattingProgressListener {
        fun reportProgress(visible: Boolean, statusText : String)

        fun onFinished(isSuccess: Boolean)
    }

    interface SpeedTestListener {
        fun reportProgress(progress: Int)

        fun onFinished(isSuccess: Boolean, speedRate: Float)
    }
    interface StringTranslationListener {
        fun getStringValue(stringId: String): String
    }

    /**
     * Enum class defining the duration options for a toast message.
     */
    enum class ToastDuration {
        /**
         * Short duration for the toast message.
         */
        LENGTH_SHORT,

        /**
         * Long duration for the toast message.
         */
        LENGTH_LONG
    }


    enum class CountryPreference {
        HIDE_LOCKED_SERVICES_IN_EPG,
        DISABLE_ZERO_PIN,
        HIDE_LOCKED_RECORDINGS,
        ENABLE_HBBTV_BY_DEFAULT,
        USE_HIDDEN_SERVICE_FLAG,
        SHOW_DEFAULT_PIN_TIP,
        DISABLE_OAD_NEWEST_VERSION_NOTIFICATION,
        DISABLE_ZERO_DIAL
    }

    enum class PlatformPreference {
        //MTK uses fallback mechanism when recording service disappeared
        //to hold channel name and number in thumbnail and poster art uri
        USE_POSTER_THUMB_RECORDING_FALLBACK
    }


    fun kidsModeEnabled(): Boolean
    fun getStringValue(stringId: String): String
    fun getUsbStorageTotalSpace(callback: IAsyncDataCallback<Int>)
    fun getUsbStorageFreeSize(callback: IAsyncDataCallback<Long>)
    fun formatUsbStorage(callback: IAsyncCallback)
    fun isUsbConnected(): Boolean
    fun getPrimaryAudioLanguage(type: PrefType = PrefType.PLATFORM): String?
    fun getSecondaryAudioLanguage(type: PrefType = PrefType.PLATFORM): String?
    fun getPrimarySubtitleLanguage(type: PrefType = PrefType.PLATFORM): String?
    fun getSecondarySubtitleLanguage(type: PrefType = PrefType.PLATFORM): String?
    fun setPrimaryAudioLanguage(language: String, type: PrefType = PrefType.PLATFORM)
    fun setSecondaryAudioLanguage(language: String, type: PrefType = PrefType.PLATFORM)
    fun setPrimarySubtitleLanguage(language: String, type: PrefType = PrefType.PLATFORM)
    fun setSecondarySubtitleLanguage(language: String, type: PrefType = PrefType.PLATFORM)
    fun setFaderValue(newValue: Int)
    fun setVisuallyImpairedAudioValue(newValue: Int)
    fun setVisuallyImpairedValues(position: Int, enabled: Boolean)
    fun setVolumeValue(value: Int)
    fun getVolumeValue() : Int
    fun getAudioSpeakerState() : Boolean
    fun getAudioHeadphoneState() : Boolean
    fun getAudioPaneState() : Boolean
    fun getDefFaderValue() : Int
    fun getAudioForVi() : Int
    fun enableAudioDescription(enable: Boolean)
    fun getAudioDescriptionState() : Boolean
    fun enableHardOfHearing(enable: Boolean)
    fun getHardOfHearingState(): Boolean
    fun enableSubtitles(enable: Boolean, type: PrefType = PrefType.PLATFORM)
    fun getSubtitlesState(type: PrefType = PrefType.PLATFORM): Boolean
    fun updateAudioTracks()
    fun updateSubtitleTracks()
    fun getDolby(tvTrackInfo: TvTrackInfo):String
    fun setAudioType(position: Int)
    fun getAudioType(callback: IAsyncDataCallback<Int>?)
    fun setSubtitlesType(position: Int, updateSwitch:Boolean = true)
    fun getSubtitleTypeDisplayNames(type: PrefType = PrefType.PLATFORM): MutableList<String>
    fun getSubtitlesType(): Int
    fun setTeletextDigitalLanguage(position: Int)
    fun getTeletextDigitalLanguage(): Int
    fun setTeletextDecodeLanguage(position: Int)
    fun getTeletextDecodeLanguage(): Int
    fun registerFormatProgressListener(listener: FormattingProgressListener)
    fun setDeafaultLanguages()
    fun enableTimeshift(enable: Boolean)
    fun setAspectRatio(index : Int)
    fun getAspectRatio() : Int
    fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean
    fun setAntennaPower(enable: Boolean)
    fun isAntennaPowerEnabled() : Boolean
    fun isGretzkyBoard(): Boolean
    fun getLanguageMapper() : LanguageMapperInterface?
    fun runOemCustomization(jsonPath: String)
    fun getPrefsValue(tag: String, defaultValue: Any?): Any?
    fun setPrefsValue(tag: String, value: Any)
    fun isCurrentEvent(tvEvent: TvEvent): Boolean
    fun checkUSBSpace(): Int
    fun getCodecDolbySpecificAudioInfo(tvTrackInfo: TvTrackInfo): String
    fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo) : Boolean
    fun isTeletextBasedSubtitle(tvTrackInfo: TvTrackInfo) : Boolean
    fun getCountryCode() : String
    fun getRegion() : Region
    fun getCountry() : String
    fun getParentalPin() : String
    fun setParentalPin(pin: String) : Boolean
    fun isThirdPartyChannel(tvChannel: TvChannel) : Boolean
    fun enableBlueMute(enable: Boolean)
    fun getBlueMuteState(): Boolean
    fun setBlueMute(isEnable: Boolean)
    fun runCoroutine(coroutineFun: () -> Unit, context: CoroutineContext = Dispatchers.Default)
    fun runCoroutineWithDelay(coroutineFun: () -> Unit, delayTime: Long = 5000, context: CoroutineContext = Dispatchers.Default)
    fun getIsPowerOffEnabled(): Boolean
    fun noSignalPowerOffChanged(isEnabled: Boolean)
    fun getPowerOffTime() : Any
    fun setPowerOffTime(value: Int, time: String)
    fun getAudioDescriptionEnabled(callback: IAsyncDataCallback<Boolean>?)
    fun getHearingImpairedEnabled(callback: IAsyncDataCallback<Boolean>?)
    fun setAudioDescriptionEnabled(enable: Boolean)
    fun setHearingImpairedEnabled(enable: Boolean)
    fun isAccessibilityEnabled(): Boolean
    fun similarity(s1:String, s2:String) :Double
    fun noSignalPowerOffEnabledOTA(): Any?
    fun noSignalPowerOffTimeOTA(): Any?
    fun dispose()
    fun startScanChannelsIntent()

    /**
     * Checks whether the user has set a custom PIN, or if it remains at its default value.
     *
     * @return `true` if the PIN has been changed from its default value, `false` otherwise.
     */
    fun isParentalPinChanged(): Boolean

    fun isUsbFormatEnabled() : Boolean
    fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>)
    fun getDateTimeFormat() : DateTimeFormat

    fun startSpeedTest(realPath: String, listener: SpeedTestListener)

    /**
     * @return Returns a Map, String path of storage, String storage description
     */
    fun getUsbDevices(): HashMap<String, String>

    fun setPvrStoragePath(path: String)

    fun setTimeshiftStoragePath(path: String)
    fun getTimeshiftStoragePath(): String

    fun getPvrStoragePath(): String

    fun isUsbFreeSpaceAvailable(): Boolean

    fun setApplicationRunningInBackground(running : Boolean)
    fun isUsbWritableReadable(): Boolean
    fun isCorrectUsbFormat(): Boolean
    fun deleteRecordingFromUsb(recording: Recording, callback: IAsyncCallback)
    fun getCountryPreferences(preference: CountryPreference, defaultValue: Any?): Any?
    fun getPlatformPreferences(preference: PlatformPreference, defaultValue: Any?): Any?
    fun getEWSPostalCode(): String?
    fun setEWSPostalCode(context: Context, postalCode: String)
    fun setVideoResolutionForRecording(recordingId: Long, resolution: String)
    fun getVideoResolutionForRecording(recording: Recording): String
    fun needTCServiceUpdate(context: Context): Boolean
    fun setGoogleLauncherBehaviour(context: Context, inputId:String)

    fun getApplicationMode():Int
    fun checkGuideIntent(intent: Intent?):Boolean
    fun isTeletextAvailable() :Boolean
    fun sortListByNumber(channelList: ArrayList<TvChannel>): ArrayList<TvChannel>

    fun updateLockedChannelIdsToPref(context: Context, isAdded: Boolean, channelId: String)

    fun getChannelIdsFromPrefs(): MutableSet<String>

    fun clearChannelidsPref()

    fun hasHohAudio(tvTrackInfo: TvTrackInfo): Boolean

    fun hasSpsAudio(tvTrackInfo: TvTrackInfo): Boolean

    fun hasAdSpsAudio(tvTrackInfo: TvTrackInfo): Boolean

    fun getApdString(tvTrackInfo: TvTrackInfo) : String


}

interface ToastInterface {
    fun showToast(text: String, duration: UtilsInterface.ToastDuration = UtilsInterface.ToastDuration.LENGTH_SHORT)
}