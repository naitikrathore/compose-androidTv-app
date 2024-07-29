package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.content.Intent
import android.media.tv.TvTrackInfo
import android.widget.Toast
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncFomattingProgressListener
import com.cltv.mal.model.async.IAsyncSpeedTestListener
import com.cltv.mal.model.async.IStringTranslationListener
import com.cltv.mal.model.entities.CountryPreference
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.mal_service.common.OemCustomizationProvider
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.recording.Recording
import kotlin.coroutines.CoroutineContext


class UtilsInterfaceImpl(private val context: Context, private val serviceImpl: IServiceAPI) :
    UtilsInterface {

    override var jsonPath: String
        get() = "assets/OemCustomizationJsonFolder/OemCustomizationBase.json"
        set(value) {}
    override var stringTranslationListener: UtilsInterface.StringTranslationListener?= null
        get() = field
        set(value) {field = value}

    init {
        serviceImpl.registerStringTranslationListener(object :
            IStringTranslationListener.Stub() {
            override fun getStringValue(p0: String?): String? {
                return stringTranslationListener?.getStringValue(p0!!)
            }
        })
    }

    override fun kidsModeEnabled(): Boolean {
        return serviceImpl.kidsModeEnabled()
    }

    override fun getStringValue(stringId: String): String {
        return serviceImpl.getStringValue(stringId)
    }

    override fun getUsbStorageTotalSpace(callback: IAsyncDataCallback<Int>) {
        val size = serviceImpl.usbStorageTotalSpace
        callback.onReceive(size)
    }

    override fun getUsbStorageFreeSize(callback: IAsyncDataCallback<Long>) {
        val size = serviceImpl.usbStorageFreeSize
        callback.onReceive(size.toLong())
    }

    override fun formatUsbStorage(callback: IAsyncCallback) {
        serviceImpl.formatUsbStorage()
        callback.onSuccess()
    }

    override fun isUsbConnected(): Boolean {
        return serviceImpl.isUsbConnected
    }

    override fun getPrimaryAudioLanguage(type: PrefType): String? {
        return serviceImpl.getPrimaryAudioLanguage(type.ordinal)
    }

    override fun getSecondaryAudioLanguage(type: PrefType): String? {
        return serviceImpl.getSecondaryAudioLanguage(type.ordinal)
    }

    override fun getPrimarySubtitleLanguage(type: PrefType): String? {
        return serviceImpl.getPrimarySubtitleLanguageUtilsInterface(type.ordinal)
    }

    override fun getSecondarySubtitleLanguage(type: PrefType): String? {
        return serviceImpl.getSecondarySubtitleLanguageUtilsInterface(type.ordinal)
    }

    override fun setPrimaryAudioLanguage(language: String, type: PrefType) {
        serviceImpl.setPrimaryAudioLanguage(language, type.ordinal)
    }

    override fun setSecondaryAudioLanguage(language: String, type: PrefType) {
        serviceImpl.setSecondaryAudioLanguage(language, type.ordinal)
    }

    override fun setPrimarySubtitleLanguage(language: String, type: PrefType) {
        serviceImpl.setPrimarySubtitleLanguageUtilsInterface(language, type.ordinal)
    }

    override fun setSecondarySubtitleLanguage(language: String, type: PrefType) {
        serviceImpl.setSecondarySubtitleLanguageUtilsInterface(language, type.ordinal)
    }

    override fun setFaderValue(newValue: Int) {
        serviceImpl.setFaderValue(newValue)
    }

    override fun setVisuallyImpairedAudioValue(newValue: Int) {
        serviceImpl.setVisuallyImpairedAudioValue(newValue)
    }

    override fun setVisuallyImpairedValues(position: Int, enabled: Boolean) {
        serviceImpl.setVisuallyImpairedValues(position, enabled)
    }

    override fun setVolumeValue(value: Int) {
        serviceImpl.volumeValue = value
    }

    override fun getVolumeValue(): Int {
        return serviceImpl.volumeValue
    }

    override fun getAudioSpeakerState(): Boolean {
        return serviceImpl.audioSpeakerState
    }

    override fun getAudioHeadphoneState(): Boolean {
        return serviceImpl.audioHeadphoneState
    }

    override fun getAudioPaneState(): Boolean {
        return serviceImpl.audioPaneState
    }

    override fun getDefFaderValue(): Int {
        return serviceImpl.defFaderValue
    }

    override fun getAudioForVi(): Int {
        return serviceImpl.audioForVi
    }

    override fun enableAudioDescription(enable: Boolean) {
        serviceImpl.enableAudioDescription(enable)
    }

    override fun getAudioDescriptionState(): Boolean {
        return serviceImpl.audioDescriptionState
    }

    override fun enableHardOfHearing(enable: Boolean) {
        serviceImpl.enableHardOfHearing(enable)
    }

    override fun getHardOfHearingState(): Boolean {
        return serviceImpl.hardOfHearingState
    }

    override fun enableSubtitles(enable: Boolean, type: PrefType) {
        serviceImpl.enableSubtitlesUtilsInterface(enable, type.ordinal)
    }

    override fun getSubtitlesState(type: PrefType): Boolean {
        return serviceImpl.getSubtitlesStateUtilsInterface(type.ordinal)
    }

    override fun updateAudioTracks() {
        serviceImpl.updateAudioTracks()
    }

    override fun updateSubtitleTracks() {
        serviceImpl.updateSubtitleTracks()
    }

    override fun getDolby(tvTrackInfo: TvTrackInfo): String {
        return serviceImpl.getDolby(tvTrackInfo)
    }

    override fun setAudioType(position: Int) {
        serviceImpl.audioType = position
    }

    override fun getAudioType(callback: IAsyncDataCallback<Int>?) {
        callback?.onReceive(serviceImpl.audioType)
    }

    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
        serviceImpl.setSubtitlesType(position, updateSwitch)
    }

    override fun getSubtitleTypeDisplayNames(type: PrefType): MutableList<String> {
        val result = mutableListOf<String>()
        serviceImpl.getSubtitleTypeDisplayNames(type.ordinal).forEach { item ->
            result.add(item)
        }
        return result
    }

    override fun getSubtitlesType(): Int {
        return serviceImpl.subtitleType
    }

    override fun setTeletextDigitalLanguage(position: Int) {
        serviceImpl.teletextDigitalLanguage = position
    }

    override fun getTeletextDigitalLanguage(): Int {
        return serviceImpl.teletextDigitalLanguage
    }

    override fun setTeletextDecodeLanguage(position: Int) {
        serviceImpl.teletextDecodeLanguage = position
    }

    override fun getTeletextDecodeLanguage(): Int {
        return serviceImpl.teletextDecodeLanguage
    }

    override fun registerFormatProgressListener(listener: UtilsInterface.FormattingProgressListener) {
        serviceImpl.registerFormatProgressListener(object :
            IAsyncFomattingProgressListener.Stub() {
            override fun onFinished(p0: Boolean) {
                listener.onFinished(p0)
            }

            override fun reportProgress(p0: Boolean, p1: String?) {
                listener.reportProgress(p0, p1!!)
            }
        })
    }

    override fun setDeafaultLanguages() {
        serviceImpl.setDeafaultLanguages()
    }

    override fun enableTimeshift(enable: Boolean) {
        serviceImpl.enableTimeshift(enable)
    }

    override fun setAspectRatio(index: Int) {
        serviceImpl.aspectRatio = index
    }

    override fun getAspectRatio(): Int {
        return serviceImpl.aspectRatio
    }

    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        return serviceImpl.hasAudioDescription(tvTrackInfo)
    }

    override fun setAntennaPower(enable: Boolean) {
        serviceImpl.setAntennaPower(enable)
    }

    override fun isAntennaPowerEnabled(): Boolean {
        return serviceImpl.isAntennaPowerEnabled
    }

    override fun isGretzkyBoard(): Boolean {
        return serviceImpl.isGretzkyBoard
    }

    override fun getLanguageMapper(): LanguageMapperInterface? {
        return fromServiceLanguageMapper(serviceImpl.languageMapper)
    }

    override fun runOemCustomization(jsonPath: String) {
        OemCustomizationProvider(context, jsonPath)
        serviceImpl.runOemCustomization(jsonPath)
    }

    override fun getPrefsValue(tag: String, defaultValue: Any?): Any? {
        return defaultValue
    }

    override fun setPrefsValue(tag: String, value: Any) {
        if (tag == UtilsInterface.APPLICATION_MODE) {
            serviceImpl.applicationMode = value as Int
        }
    }

    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
        return serviceImpl.isCurrentEvent(toServiceTvEvent(tvEvent))
    }

    override fun checkUSBSpace(): Int {
        return serviceImpl.checkUSBSpace()
    }

    override fun getCodecDolbySpecificAudioInfo(tvTrackInfo: TvTrackInfo): String {
        return serviceImpl.getCodecDolbySpecificAudioInfo(tvTrackInfo)
    }

    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo): Boolean {
        return serviceImpl.hasHardOfHearingSubtitleInfo(tvTrackInfo)
    }

    override fun isTeletextBasedSubtitle(tvTrackInfo: TvTrackInfo): Boolean {
        return false
    }

    override fun getCountryCode(): String {
        return serviceImpl.countryCode
    }

    override fun getRegion(): Region {
        return fromServiceRegion(serviceImpl.region)
    }

    override fun getCountry(): String {
        return serviceImpl.country
    }

    override fun getParentalPin(): String {
        return serviceImpl.parentalPin
    }

    override fun setParentalPin(pin: String): Boolean {
        serviceImpl.parentalPin = pin
        return true
    }

    override fun isThirdPartyChannel(tvChannel: TvChannel): Boolean {
        return serviceImpl.isThirdPartyChannel(toServiceChannel(tvChannel))
    }

    override fun enableBlueMute(enable: Boolean) {
        serviceImpl.enableBlueMute(enable)
    }

    override fun getBlueMuteState(): Boolean {
        return serviceImpl.blueMuteState
    }

    override fun setBlueMute(isEnable: Boolean) {
        serviceImpl.setBlueMute(isEnable)
    }

    override fun runCoroutine(coroutineFun: () -> Unit, context: CoroutineContext) {
        CoroutineHelper.runCoroutine(coroutineFun, context)
    }

    override fun runCoroutineWithDelay(
        coroutineFun: () -> Unit,
        delayTime: Long,
        context: CoroutineContext
    ) {
        CoroutineHelper.runCoroutineWithDelay(coroutineFun, delayTime, context)
    }

    override fun getIsPowerOffEnabled(): Boolean {
        return serviceImpl.isPowerOffEnabled
    }

    override fun noSignalPowerOffChanged(isEnabled: Boolean) {
        serviceImpl.noSignalPowerOffChanged(isEnabled)
    }

    override fun getPowerOffTime(): Any {
        return serviceImpl.powerOffTime
    }

    override fun setPowerOffTime(value: Int, time: String) {
        return serviceImpl.setPowerOffTime(value, time)
    }

    override fun getAudioDescriptionEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(serviceImpl.audioDescriptionEnabled)
    }

    override fun getHearingImpairedEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(serviceImpl.hearingImpairedEnabled)
    }

    override fun setAudioDescriptionEnabled(enable: Boolean) {
        serviceImpl.audioDescriptionEnabled = enable
    }

    override fun setHearingImpairedEnabled(enable: Boolean) {
        serviceImpl.hearingImpairedEnabled = enable
    }

    override fun isAccessibilityEnabled(): Boolean {
        return serviceImpl.isAccessibilityEnabled
    }

    override fun similarity(s1: String, s2: String): Double {
        return serviceImpl.similarity(s1, s2)
    }

    override fun noSignalPowerOffEnabledOTA(): Any? {
        return serviceImpl.noSignalPowerOffEnabledOTA()
    }

    override fun noSignalPowerOffTimeOTA(): Any? {
        return serviceImpl.noSignalPowerOffTimeOTA()
    }

    override fun dispose() {
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

    override fun isParentalPinChanged(): Boolean {
        return serviceImpl.isParentalPinChanged
    }

    override fun isUsbFormatEnabled(): Boolean {
        return serviceImpl.isUsbFormatEnabled
    }

    override fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>) {
        val systemInfoData = serviceImpl.getSystemInfoData(toServiceChannel(tvChannel))
        callback.onReceive(fromServiceSystemInfoData(systemInfoData))
    }

    override fun getDateTimeFormat(): DateTimeFormat {
        return fromServiceDateTimeFormat(serviceImpl.dateTimeFormat)
    }

    override fun startSpeedTest(realPath: String, listener: UtilsInterface.SpeedTestListener) {
        serviceImpl.startSpeedTest(
            realPath,
            object : IAsyncSpeedTestListener.Stub() {
                override fun onFinished(p0: Boolean, p1: Float) {
                    listener.onFinished(p0, p1)
                }

                override fun reportProgress(p0: Int) {
                    listener.reportProgress(p0)
                }
            })
    }

    override fun getUsbDevices(): HashMap<String, String> {
        var map = HashMap<String, String>()
        map.putAll(serviceImpl.getUsbDevices())
        return map
    }

    override fun setPvrStoragePath(path: String) {
        serviceImpl.pvrStoragePath = path
    }

    override fun getTimeshiftStoragePath(): String {
        return ""
    }

    override fun setTimeshiftStoragePath(path: String) {

    }

    override fun getPvrStoragePath(): String {
        return serviceImpl.getPvrStoragePath()
    }

    override fun isUsbFreeSpaceAvailable(): Boolean {
        return serviceImpl.isUsbFreeSpaceAvailable()
    }

    override fun setApplicationRunningInBackground(running: Boolean) {
        serviceImpl.setApplicationRunningInBackground(running)
    }

    override fun isUsbWritableReadable(): Boolean {
        return serviceImpl.isUsbWritableReadable()
    }

    override fun isCorrectUsbFormat(): Boolean {
        return serviceImpl.isCorrectUsbFormat()
    }

    override fun deleteRecordingFromUsb(recording: Recording, callback: IAsyncCallback) {
    }

    override fun getCountryPreferences(
        preference: UtilsInterface.CountryPreference,
        defaultValue: Any?
    ): Any? {
        return serviceImpl.getCountryPreferences(CountryPreference.fromInteger(preference.ordinal) ,defaultValue as Boolean)
    }

    override fun getPlatformPreferences(
        preference: UtilsInterface.PlatformPreference,
        defaultValue: Any?
    ): Any? {
        return defaultValue
    }

    override fun getEWSPostalCode(): String? {
        return serviceImpl.ewsPostalCode!!
    }

    override fun setEWSPostalCode(context: Context, postalCode: String) {
        serviceImpl.ewsPostalCode = postalCode
    }

    override fun setVideoResolutionForRecording(recordingId: Long, resolution: String) {
    }

    override fun getVideoResolutionForRecording(recording: Recording): String {
        return ""
    }
    override fun needTCServiceUpdate(context: Context): Boolean {
        return false
    }

    override fun setGoogleLauncherBehaviour(context: Context, inputId: String) {
    }

    override fun getApplicationMode(): Int {
        return serviceImpl.applicationMode
    }
    override fun checkGuideIntent(intent: Intent?): Boolean {
        return false
    }

    override fun isTeletextAvailable(): Boolean {
        return false
    }

    override fun sortListByNumber(channelList: ArrayList<TvChannel>): ArrayList<TvChannel> {
        return channelList
    }

    override fun updateLockedChannelIdsToPref(
        context: Context,
        isAdded: Boolean,
        channelId: String
    ) {
    }

    override fun getChannelIdsFromPrefs(): MutableSet<String> {
       return mutableSetOf()
    }

    override fun clearChannelidsPref() {
    }

    override fun hasHohAudio(tvTrackInfo: TvTrackInfo): Boolean {
        return false
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        Toast.makeText(context,text,duration.ordinal).show()
    }

    override fun hasSpsAudio(tvTrackInfo: TvTrackInfo): Boolean {
        return false
    }

    override fun hasAdSpsAudio(tvTrackInfo: TvTrackInfo): Boolean {
        return false
    }

    override fun getApdString(tvTrackInfo: TvTrackInfo): String {
        return  ""
    }

}