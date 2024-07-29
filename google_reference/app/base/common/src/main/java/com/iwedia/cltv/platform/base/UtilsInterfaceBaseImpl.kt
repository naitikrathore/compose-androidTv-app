package com.iwedia.cltv.platform.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.usb.UsbManager
import android.location.Geocoder
import android.media.tv.TvTrackInfo
import android.os.Build
import android.os.Looper
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.format.DateFormat
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.os.ConfigurationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.iwedia.cltv.platform.base.content_provider.OemCustomizationProvider
import com.iwedia.cltv.platform.base.content_provider.TifSystemInfoProvider
import com.iwedia.cltv.platform.base.content_provider.isNumeric
import com.iwedia.cltv.platform.base.language.LanguageMapperBaseImpl
import com.iwedia.cltv.platform.base.player.TrackBase
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.SystemInfoProviderInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface.Companion.PREFS_TAG
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.recording.Recording
import java.io.File
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

open class UtilsInterfaceBaseImpl(
    private var context: Context,
    private val textToSpeechModule: TTSInterface
) : UtilsInterface {

    private var toast: Toast? = null
    override var stringTranslationListener: UtilsInterface.StringTranslationListener? = null
    val KEY_AUDIO_LANG = "media.audio.preferredlanguages"
    val KEY_SUBTITLE_LANG = "media.subtitles.preferredlanguages"
    val KEY_AUDIO_TYPE = "media.audio.preferredtype"
    val KEY_SUBTITLE_ENABLED = "media.subtitles.enabled"
    val KEY_SUBTITLE_TYPE = "media.subtitles.preferredtype"
    val KEY = "_id"
    val VALUE = "value"
    val KEY_PREFERRED_AUDIO_LANGUAGE = "preferred_audio_language"
    val KEY_PREFERRED_SUBTITLE_LANGUAGE = "preferred_subtitle_language"
    val KEY_PREFERRED_SECOND_AUDIO_LANGUAGE = "preferred_second_audio_language"
    val KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE = "preferred_second_subtitle_language"


    val KEY_ANTENNA_POWER_ENABLED = "lna.enabled"
    val DOLBY_AUDIO_EAC3_NAME_TYPE = " - Dolby D+"
    val DOLBY_AUDIO_AC3_NAME_TYPE = " - Dolby D"
    val AUDIO_DTS_TYPE = " [DTS]"
    private val PREFS_KEY_CURRENT_COUNTRY = "CURRENT_COUNTRY"
    private val PREFS_KEY_CURRENT_COUNTRY_ALPHA3 = "CURRENT_COUNTRY_ALPHA3"
    protected val PREFS_KEY_IS_PARENTAL_PIN_CHANGED = "IS_PARENTAL_PIN_CHANGED"

    private val DEFAULT_PIN = "0000"

    // PVR/TimeShift
    val PVR_STORAGE_PATH = "PVR_STORAGE_PATH"
    val TIMESHIFT_STORAGE_PATH = "TIMESHIFT_STORAGE_PATH"

    private val GUIDE_INTENT_DATA = "content://android.media.tv/program"

    protected open var languageCodeMapper : LanguageMapperInterface = LanguageMapperBaseImpl()

    override var jsonPath: String = "assets/OemCustomizationJsonFolder/OemCustomizationBase.json"

    //current country code
    private var currentCountry : String
    //current region
    private var currentRegion : Region

    private var userProfileNumber = "0"

    protected var systemInfoProviderInterface : SystemInfoProviderInterface? = null

    init {
        val prefs  = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE)
        CoroutineHelper.runCoroutine({
            userProfileNumber = context.filesDir.path.split("/")[3]
        })

        currentCountry = prefs.getString(
            PREFS_KEY_CURRENT_COUNTRY,
            ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)!!.country //default
        )!!

        currentRegion = findRegionByCountry(currentCountry)
    }

    override fun kidsModeEnabled(): Boolean{
        return userProfileNumber != "0"
    }

    @SuppressLint("MissingPermission")
    open fun prepareRegion() {
        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "init")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "permission not available")
            currentCountry = "US"
            currentRegion = findRegionByCountry(currentCountry)
            var alpha3CountryCode = convertAlpha2ToAlpha3(currentCountry)
            val prefs  = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE)
            prefs.edit().putString(PREFS_KEY_CURRENT_COUNTRY, "").apply()
            prefs.edit().putString(PREFS_KEY_CURRENT_COUNTRY_ALPHA3, "").apply()
        } else {
            val locationRequest = com.google.android.gms.location.LocationRequest()
            locationRequest.setFastestInterval(5000)
            locationRequest.setInterval(10000)
            locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)

            Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "trying to get latlong")
            val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

            val mLocationCallback =object : LocationCallback(){
                override fun onLocationResult(result: LocationResult) {

                    fusedLocationClient.removeLocationUpdates(this)
                    super.onLocationResult(result)
                    val location = result.lastLocation
                    if(location!=null){
                        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "lat long is ${location.latitude},${location.latitude}")
                        val geocoder = Geocoder(context)

                        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "calling geocoder")

                        try {
                            val addressList =
                                geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            if(!addressList.isNullOrEmpty()){
                                currentCountry = addressList!!.get(0).countryCode
                                // store country code on success
                                val prefs  = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE)
                                prefs.edit().putString(PREFS_KEY_CURRENT_COUNTRY, currentCountry).apply()
                                var alpha3CountryCode = convertAlpha2ToAlpha3(currentCountry)
                                prefs.edit().putString(PREFS_KEY_CURRENT_COUNTRY_ALPHA3, alpha3CountryCode).apply()
                                Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "Device location country code is: $currentCountry $alpha3CountryCode")
                                currentRegion = findRegionByCountry(currentCountry)
                            }else{
                                Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "address list is empty")
                            }
                        } catch (e : Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "Failed to get address list")
                        }
                    }else{
                        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "Unable to find country based on lat long")
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    super.onLocationAvailability(availability)
                    Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "location availability : $availability")
                }
            }

            fusedLocationClient.requestLocationUpdates(com.google.android.gms.location.LocationRequest(),
                mLocationCallback,
                Looper.getMainLooper())
        }
    }

    private fun convertAlpha2ToAlpha3(alpha2Code: String): String? {
        return alpha2ToAlpha3Map[alpha2Code]
    }

    protected fun findRegionByCountry(countryCode : String): Region {
        Region.getRegionList().forEach { region ->
            Region.getCountryList(region).forEach {country->
                if(countryCode == country){
                    Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "Region is: ${region.name}")
                    return region
                }
            }
        }
        Log.d(Constants.LogTag.CLTV_TAG + "FIND_REGION", "Unable to find region for current country")
        return Region.EU // default
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getUsbStorageTotalSpace(callback: IAsyncDataCallback<Int>) {
        val storageManager: StorageManager = context?.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumes: MutableList<StorageVolume> = storageManager.storageVolumes
        var usbStorageFound = false
        for (storageVolume in storageVolumes){
            var volumePath = storageVolume.directory
            if (volumePath != null){
                if (!(storageVolume.directory.toString().contains("emulated"))) {
                    usbStorageFound = true
                    val statFs = StatFs(volumePath.toString())
                    val availableBytes = statFs.availableBytes
                    callback.onReceive(availableBytes.toInt())
                }
            }
        }
        if (!usbStorageFound) callback.onFailed(Error("USB storage not found"))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getUsbStorageFreeSize(callback: IAsyncDataCallback<Long>) {
        val storageManager: StorageManager = context?.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageVolumes: MutableList<StorageVolume> = storageManager.storageVolumes
        var usbStorageFound = false
        for (storageVolume in storageVolumes){
            var volumePath = storageVolume.directory
            if (volumePath != null){
                if (!(storageVolume.directory.toString().contains("emulated"))) {
                    usbStorageFound = true
                    val statFs = StatFs(volumePath.toString())
                    val availableBytes = statFs.availableBytes
                    callback.onReceive(availableBytes)
                }
            }
        }
        if (!usbStorageFound) callback.onFailed(Error("USB storage not found"))
    }

    override fun formatUsbStorage(callback: IAsyncCallback) {}

    override fun isUsbConnected(): Boolean {
        var usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        return usbManager.deviceList.size > 0
    }

    override fun getPrimaryAudioLanguage(type: PrefType): String? {
        return getPrefsValue(KEY_PREFERRED_AUDIO_LANGUAGE, "") as String
    }

    override fun getSecondaryAudioLanguage(type: PrefType): String? {
        return getPrefsValue(KEY_PREFERRED_SECOND_AUDIO_LANGUAGE, "") as String
    }

    override fun getPrimarySubtitleLanguage(type: PrefType): String? {
        return getPrefsValue(KEY_PREFERRED_SUBTITLE_LANGUAGE, "") as String
    }

    override fun getSecondarySubtitleLanguage(type: PrefType): String? {
        return getPrefsValue(KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE, "") as String
    }

    override fun setPrimaryAudioLanguage(language: String, type: PrefType) {
        setPrefsValue(KEY_PREFERRED_AUDIO_LANGUAGE, language)
    }

    override fun setSecondaryAudioLanguage(language: String, type: PrefType) {
        setPrefsValue(KEY_PREFERRED_SECOND_AUDIO_LANGUAGE, language)
    }

    override fun setPrimarySubtitleLanguage(language: String, type: PrefType) {
        setPrefsValue(KEY_PREFERRED_SUBTITLE_LANGUAGE, language)
    }

    override fun setSecondarySubtitleLanguage(language: String, type: PrefType) {
        setPrefsValue(KEY_PREFERRED_SECOND_SUBTITLE_LANGUAGE, language)
    }

    override fun setFaderValue(newValue: Int) {
    }

    override fun setVisuallyImpairedAudioValue(newValue: Int) {
    }

    override fun setVisuallyImpairedValues(position: Int, enabled: Boolean) {
    }

    override fun setVolumeValue(value: Int) {
    }

    override fun getVolumeValue(): Int {
        return 0
    }

    override fun getAudioSpeakerState(): Boolean {
        return false
    }

    override fun getAudioHeadphoneState(): Boolean {
        return false
    }

    override fun getAudioPaneState(): Boolean {
        return false
    }

    override fun getDefFaderValue(): Int {
        return 0
    }

    override fun getAudioForVi(): Int {
        return 0
    }

    @SuppressLint("Range")
    fun readInternalProviderData(key: String): String? {
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(
            Contract.buildConfigUri(1),
            null,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            if (cursor.getString(cursor.getColumnIndex(Contract.Config.CURRENT_COUNTRY_COLUMN)) != null) {
                val country = cursor.getString(cursor.getColumnIndex(Contract.Config.CURRENT_COUNTRY_COLUMN)).toString()
                return country
            }
        }
        return null
    }

    override fun enableAudioDescription(enable: Boolean) {
    }

    override fun getAudioDescriptionState(): Boolean {
        return false
    }

    override fun enableHardOfHearing(enable: Boolean) {
    }

    override fun getHardOfHearingState(): Boolean {
        return false
    }

    override fun enableSubtitles(enable: Boolean, type: PrefType) {
        setPrefsValue(KEY_SUBTITLE_ENABLED, enable)
    }

    override fun getSubtitlesState(type: PrefType): Boolean {
        return getPrefsValue(KEY_SUBTITLE_ENABLED, false) as Boolean
    }

    override fun updateAudioTracks() {
    }

    override fun updateSubtitleTracks() {
    }

    override fun getDolby(tvTrackInfo: TvTrackInfo): String {
        return ""
    }

    override fun setAudioType(position: Int) {
    }

    override fun getAudioType(callback: IAsyncDataCallback<Int>?) {
        callback?.onReceive(0)
    }

    override fun setSubtitlesType(position: Int, updateSwitch: Boolean) {
    }

    override fun getSubtitleTypeDisplayNames(type: PrefType): MutableList<String> {
        return mutableListOf()
    }

    override fun getSubtitlesType(): Int {
        return 0
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun setSubtitleSwitch(position: Int) {
        val contentResolver: ContentResolver = context.contentResolver
        var cv = ContentValues()
        cv.put(Contract.OemCustomization.SUBTITLE_COLUMN_ID, position)
        contentResolver.update(ContentProvider.OEM_CUSTOMIZATION_URI, cv, null)
    }

    override fun setTeletextDigitalLanguage(position: Int) {
    }

    override fun getTeletextDigitalLanguage(): Int {
        return -1
    }

    override fun setTeletextDecodeLanguage(position: Int) {

    }

    override fun getTeletextDecodeLanguage(): Int {
        return -1
    }

    override fun registerFormatProgressListener(listener: UtilsInterface.FormattingProgressListener) {
    }

    override fun setDeafaultLanguages() {
    }


    override fun enableTimeshift(enable: Boolean) {
    }

    override fun setAspectRatio(index: Int) {
    }

    override fun getAspectRatio(): Int {
        return 0
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        return false
    }

    override fun setAntennaPower(enable: Boolean) {
    }

    override fun isAntennaPowerEnabled(): Boolean {
        return false
    }


    override fun isGretzkyBoard(): Boolean {
        return false
    }

    override fun getLanguageMapper(): LanguageMapperInterface? {
        return languageCodeMapper
    }

    override fun setPrefsValue(tag: String, value: Any) {
        if (value != null) {
            if (value is Set<*>) {
                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putStringSet(tag, value as Set<String>).apply()
            } else if (value is Int) {
                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putInt(tag, value).apply()
            } else if (value is Boolean) {
                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putBoolean(tag, value).apply()
            } else if (value is Long) {
                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putLong(tag, value).apply()
            } else if (value is Float) {
                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putFloat(tag, value).apply()
            } else if (value is String) {
                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit()
                    .putString(tag, value).apply()
            }
        }
    }

    override fun getPrefsValue(tag: String, defaultValue: Any?): Any? {
        if (defaultValue != null) {
            if (defaultValue is String) {
                return  context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getString(tag, defaultValue)
            } else if (defaultValue is Int) {
                return  context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getInt(tag, defaultValue)
            } else if (defaultValue is Boolean) {
                return  context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(tag, defaultValue)
            } else if (defaultValue is Long) {
                return  context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getLong(tag, defaultValue)
            } else if (defaultValue is Float) {
                return  context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getFloat(tag, defaultValue)
            } else if (defaultValue is Set<*>) {
                return  context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getStringSet(tag, defaultValue as Set<String>)
            }
        }
        return defaultValue
    }

    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
        var currentTime = Date()
        var eventStartTime = Date(tvEvent.startTime)
        var eventEndTime = Date(tvEvent.endTime)
        if (eventStartTime.before(currentTime) && eventEndTime.after(currentTime)) {
            return true
        }
        return false
    }

    override fun checkUSBSpace(): Int {
        var mntMedia: File? = null
        val USB_ROOT_PATH = "/mnt/media_rw"
        if (File(USB_ROOT_PATH).exists()) {
            mntMedia = File(USB_ROOT_PATH)
        }
        if (mntMedia != null) {
            val usbList = mntMedia.listFiles()
            if (usbList != null) {
                for (usb in usbList) {
                    if (!usb.name.contains("emulated") && !usb.name.contains("self")) {
                        return (usb.freeSpace * 100 / usb.totalSpace).toInt()
                    }
                }
            }
        }
        return -1
    }


    override fun runOemCustomization(jsonPath: String){
        OemCustomizationProvider(context, jsonPath)
    }
    override fun getCodecDolbySpecificAudioInfo(tvTrackInfo: TvTrackInfo): String {
        return ""
    }

    override fun hasHardOfHearingSubtitleInfo(tvTrackInfo: TvTrackInfo) : Boolean {
        return false
    }

    override fun getCountryCode(): String {
        return currentCountry
    }

    override fun getRegion(): Region {
        return currentRegion
    }

    override fun getCountry(): String {
        return Locale.getDefault().country
    }

    @SuppressLint("Range")
    override fun getParentalPin(): String {
        val pin = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getString("parental_pin", DEFAULT_PIN)
        if (pin == null){
            setParentalPin(DEFAULT_PIN)
            return DEFAULT_PIN
        }
        return pin
    }

    override fun setParentalPin(pin: String) : Boolean {
        context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit().putString("parental_pin", pin).apply()
        context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit().putBoolean(PREFS_KEY_IS_PARENTAL_PIN_CHANGED, true).apply()
        return true
    }

    override fun isThirdPartyChannel(tvChannel: TvChannel): Boolean {
        if ((tvChannel.inputId.lowercase().contains("iwedia"))) {
            return true
        }
        return false
    }

    override fun enableBlueMute(enable: Boolean) {
    }

    override fun getBlueMuteState(): Boolean {
        return false
    }

    override fun setBlueMute(isEnable: Boolean) {
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
        return false
    }

    override fun noSignalPowerOffChanged(isEnabled: Boolean){
    }

    override fun getPowerOffTime(): Any {
        return ""
    }

    override fun setPowerOffTime(value: Int, time: String) {
        setPrefsValue("no_signal_auto_power_off", time)
    }

    override fun getAudioDescriptionEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(getPrefsValue("IS_AD_ON", false) as Boolean)
    }

    override fun getHearingImpairedEnabled(callback: IAsyncDataCallback<Boolean>?) {
        callback?.onReceive(getPrefsValue("IS_HI_ON", false) as Boolean)
    }

    override fun setAudioDescriptionEnabled(enable: Boolean) {
        setPrefsValue("IS_AD_ON", enable)
    }

    override fun setHearingImpairedEnabled(enable: Boolean) {
        setPrefsValue("IS_HI_ON", enable)
    }

    override fun isAccessibilityEnabled(): Boolean {
        val am: AccessibilityManager =
            (context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager?)!!
        return am.isEnabled
    }

    override fun dispose() {
    }

    override fun getStringValue(stringId: String): String {
        stringTranslationListener?.let {
            return it.getStringValue(stringId)
        }
        return ""
    }

    override fun similarity(s1: String, s2: String): Double {
            var longer = s1.lowercase()
            var shorter = s2.lowercase()
            if (s1.length < s2.length) {
                longer = s2
                shorter = s1
            }
            val longerLength = longer.length
            return if (longerLength == 0) {
                1.0 /* both strings have zero length */
            } else (longerLength - getLevenshteinDistance(
                longer,
                shorter
            )) / longerLength.toDouble()
        }

    override fun noSignalPowerOffEnabledOTA(): Any? {
        return null
    }

    override fun noSignalPowerOffTimeOTA(): Any? {
        return null
    }

    /**
     * LevenshteinDistance
     * copied from https://commons.apache.org/proper/commons-lang/javadocs/api-2.5/src-html/org/apache/commons/lang/StringUtils.html#line.6162
     */
    private fun getLevenshteinDistance(s: String?, t: String?): Int {

        var s = s
        var t = t
        require(!(s == null || t == null)) { "Strings must not be null" }
        var n = s.length // length of s
        var m = t.length // length of t
        if (n == 0) {
            return m
        } else if (m == 0) {
            return n
        }
        if (n > m) {
            // swap the input strings to consume less memory
            val tmp: String = s
            s = t
            t = tmp
            n = m
            m = t.length
        }
        var p = IntArray(n + 1) //'previous' cost array, horizontally
        var d = IntArray(n + 1) // cost array, horizontally
        var _d: IntArray //placeholder to assist in swapping p and d

        // indexes into strings s and t
        var i: Int // iterates through s
        var j: Int // iterates through t
        var t_j: Char // jth character of t
        var cost: Int // cost
        i = 0
        while (i <= n) {
            p[i] = i
            i++
        }
        j = 1
        while (j <= m) {
            t_j = t[j - 1]
            d[0] = j
            i = 1
            while (i <= n) {
                cost = if (s[i - 1] == t_j) 0 else 1
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost)
                i++
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p
            p = d
            d = _d
            j++
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n]
    }

    override fun startScanChannelsIntent() {
        context.startActivity(Intent(Constants.ChannelConstants.ANDROID_SETTINGS_CHANNEL_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
    
    override fun isParentalPinChanged(): Boolean {
        return context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(PREFS_KEY_IS_PARENTAL_PIN_CHANGED, false)
    }

    override fun isUsbFormatEnabled(): Boolean {
        return true
    }

    override fun getSystemInfoData(tvChannel: TvChannel, callback: IAsyncDataCallback<SystemInfoData>) {
        if(systemInfoProviderInterface==null) systemInfoProviderInterface = TifSystemInfoProvider()
        systemInfoProviderInterface!!.getSystemInfoData(tvChannel, callback)
    }

    override fun startSpeedTest(realPath: String, listener: UtilsInterface.SpeedTestListener) {}

    override fun getUsbDevices() = hashMapOf<String, String>()

    /**
     * @param path for USB storage used for PVR/TimeShift
     */
    override fun setPvrStoragePath(path: String) {}
    override fun setTimeshiftStoragePath(path: String) {}
    override fun getTimeshiftStoragePath(): String {
        return ""
    }

    override fun getPvrStoragePath() = ""

    override fun isUsbFreeSpaceAvailable() = false

    private val alpha2ToAlpha3Map = mapOf(
        "AF" to "AFG", "AX" to "ALA", "AL" to "ALB", "DZ" to "DZA", "AS" to "ASM",
        "AD" to "AND", "AO" to "AGO", "AI" to "AIA", "AQ" to "ATA", "AG" to "ATG",
        "AR" to "ARG", "AM" to "ARM", "AW" to "ABW", "AU" to "AUS", "AT" to "AUT",
        "AZ" to "AZE", "BS" to "BHS", "BH" to "BHR", "BD" to "BGD", "BB" to "BRB",
        "BY" to "BLR", "BE" to "BEL", "BZ" to "BLZ", "BJ" to "BEN", "BM" to "BMU",
        "BT" to "BTN", "BO" to "BOL", "BQ" to "BES", "BA" to "BIH", "BW" to "BWA",
        "BV" to "BVT", "BR" to "BRA", "IO" to "IOT", "BN" to "BRN", "BG" to "BGR",
        "BF" to "BFA", "BI" to "BDI", "CV" to "CPV", "KH" to "KHM", "CM" to "CMR",
        "CA" to "CAN", "KY" to "CYM", "CF" to "CAF", "TD" to "TCD", "CL" to "CHL",
        "CN" to "CHN", "CX" to "CXR", "CC" to "CCK", "CO" to "COL", "KM" to "COM",
        "CG" to "COG", "CD" to "COD", "CK" to "COK", "CR" to "CRI", "HR" to "HRV",
        "CU" to "CUB", "CW" to "CUW", "CY" to "CYP", "CZ" to "CZE", "DK" to "DNK",
        "DJ" to "DJI", "DM" to "DMA", "DO" to "DOM", "EC" to "ECU", "EG" to "EGY",
        "SV" to "SLV", "GQ" to "GNQ", "ER" to "ERI", "EE" to "EST", "SZ" to "SWZ",
        "ET" to "ETH", "FK" to "FLK", "FO" to "FRO", "FJ" to "FJI", "FI" to "FIN",
        "FR" to "FRA", "GF" to "GUF", "PF" to "PYF", "TF" to "ATF", "GA" to "GAB",
        "GM" to "GMB", "GE" to "GEO", "DE" to "DEU", "GH" to "GHA", "GI" to "GIB",
        "GR" to "GRC", "GL" to "GRL", "GD" to "GRD", "GP" to "GLP", "GU" to "GUM",
        "GT" to "GTM", "GG" to "GGY", "GN" to "GIN", "GW" to "GNB", "GY" to "GUY",
        "HT" to "HTI", "HM" to "HMD", "VA" to "VAT", "HN" to "HND", "HK" to "HKG",
        "HU" to "HUN", "IS" to "ISL", "IN" to "IND", "ID" to "IDN", "IR" to "IRN",
        "IQ" to "IRQ", "IE" to "IRL", "IM" to "IMN", "IL" to "ISR", "IT" to "ITA",
        "JM" to "JAM", "JP" to "JPN", "JE" to "JEY", "JO" to "JOR", "KZ" to "KAZ",
        "KE" to "KEN", "KI" to "KIR", "KP" to "PRK", "KR" to "KOR", "KW" to "KWT",
        "KG" to "KGZ", "LA" to "LAO", "LV" to "LVA", "LB" to "LBN", "LS" to "LSO",
        "LR" to "LBR", "LY" to "LBY", "LI" to "LIE", "LT" to "LTU", "LU" to "LUX",
        "MO" to "MAC", "MK" to "MKD", "MG" to "MDG", "MW" to "MWI", "MY" to "MYS",
        "MV" to "MDV", "ML" to "MLI", "MT" to "MLT", "MH" to "MHL", "MQ" to "MTQ",
        "MR" to "MRT", "MU" to "MUS", "YT" to "MYT", "MX" to "MEX", "FM" to "FSM",
        "MD" to "MDA", "MC" to "MCO", "MN" to "MNG", "ME" to "MNE", "MS" to "MSR",
        "MA" to "MAR", "MZ" to "MOZ", "MM" to "MMR", "NA" to "NAM", "NR" to "NRU",
        "NP" to "NPL", "NL" to "NLD", "NC" to "NCL", "NZ" to "NZL", "NI" to "NIC",
        "NE" to "NER", "NG" to "NGA", "NU" to "NIU", "NF" to "NFK", "MP" to "MNP",
        "NO" to "NOR", "OM" to "OMN", "PK" to "PAK", "PW" to "PLW", "PS" to "PSE",
        "PA" to "PAN", "PG" to "PNG", "PY" to "PRY", "PE" to "PER", "PH" to "PHL",
        "PN" to "PCN", "PL" to "POL", "PT" to "PRT", "PR" to "PRI", "QA" to "QAT",
        "RE" to "REU", "RO" to "ROU", "RU" to "RUS", "RW" to "RWA", "BL" to "BLM",
        "SH" to "SHN", "KN" to "KNA", "LC" to "LCA", "MF" to "MAF", "PM" to "SPM",
        "VC" to "VCT", "WS" to "WSM", "SM" to "SMR", "ST" to "STP", "SA" to "SAU",
        "SN" to "SEN", "RS" to "SRB", "SC" to "SYC", "SL" to "SLE", "SG" to "SGP",
        "SX" to "SXM", "SK" to "SVK", "SI" to "SVN", "SB" to "SLB", "SO" to "SOM",
        "ZA" to "ZAF", "GS" to "SGS", "SS" to "SSD", "ES" to "ESP", "LK" to "LKA",
        "SD" to "SDN", "SR" to "SUR", "SJ" to "SJM", "SE" to "SWE", "CH" to "CHE",
        "SY" to "SYR", "TW" to "TWN", "TJ" to "TJK", "TZ" to "TZA", "TH" to "THA",
        "TL" to "TLS", "TG" to "TGO", "TK" to "TKL", "TO" to "TON", "TT" to "TTO",
        "TN" to "TUN", "TR" to "TUR", "TM" to "TKM", "TC" to "TCA", "TV" to "TUV",
        "UG" to "UGA", "UA" to "UKR", "AE" to "ARE", "GB" to "GBR", "US" to "USA",
        "UM" to "UMI", "UY" to "URY", "UZ" to "UZB", "VU" to "VUT", "VE" to "VEN",
        "VN" to "VNM", "VG" to "VGB", "VI" to "VIR", "WF" to "WLF", "EH" to "ESH",
        "YE" to "YEM", "ZM" to "ZMB", "ZW" to "ZWE"
    )

    /**
     * Retrieves the appropriate date and time format based on the region and time settings.
     * @return DateTimeFormat object containing the datePattern, timePattern & dateTimePattern.
     */
    override fun getDateTimeFormat() : DateTimeFormat {
        val isUS = getRegion()==Region.US
        val timeFormat = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mma"

        return DateTimeFormat(
            datePattern = if(isUS) "MM.dd.yyyy" else "dd.MM.yyyy",
            timePattern = timeFormat,
            dateTimePattern = "${if(isUS) "MM/dd" else "dd/MM"} $timeFormat"
        )
    }

    override fun setApplicationRunningInBackground(running : Boolean) {

    }

    override fun isUsbWritableReadable() = true


    override fun isCorrectUsbFormat(): Boolean {
        return true
    }

    override fun getCountryPreferences(
        preference: UtilsInterface.CountryPreference,
        defaultValue: Any?
    ): Any? {
        return defaultValue
    }

    override fun getPlatformPreferences(
        preference: UtilsInterface.PlatformPreference,
        defaultValue: Any?
    ): Any? {
        return defaultValue
    }

    override fun deleteRecordingFromUsb(recording: Recording, callback: IAsyncCallback) {

    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        toast?.cancel()
        // Make toast
        toast = Toast.makeText(
            context,
            text,
            Toast.LENGTH_LONG
        )
        toast?.show()
        textToSpeechModule.setSpeechText(text, importance = SpeechText.Importance.TOAST)
    }
    override fun getEWSPostalCode(): String? {
        return null
    }

    override fun setEWSPostalCode(context: Context, postalCode: String) {}
    override fun setVideoResolutionForRecording(recordingId: Long, resolution: String) {}
    override fun getVideoResolutionForRecording(recording: Recording): String {
        return ""
    }
    override fun needTCServiceUpdate(context: Context): Boolean {
        return false
    }

    override fun setGoogleLauncherBehaviour(context: Context, inputId: String) {

    }

    override fun getApplicationMode(): Int {
        return getPrefsValue("application_mode", 0) as Int
    }

    override fun checkGuideIntent(intent: Intent?): Boolean {
       return (intent?.data?.toString() == GUIDE_INTENT_DATA)
    }

    override fun isTeletextBasedSubtitle(tvTrackInfo: TvTrackInfo): Boolean {
        return false
    }
    override fun isTeletextAvailable() :Boolean{
        return false
    }

    override fun sortListByNumber(channelList: ArrayList<TvChannel>): ArrayList<TvChannel> {
        try {
            val isNumeric = isNumeric(ArrayList(channelList))
            if(channelList[0].isFastChannel()){
                return ArrayList(channelList.sortedWith(compareBy<TvChannel> {it.ordinalNumber}))
            }else if(isNumeric){
                return ArrayList(channelList.sortedWith(compareBy<TvChannel> {it.displayNumber.toInt()}))
            }else{
                return ArrayList(channelList.sortedWith(compareBy<TvChannel> (
                    { it.displayNumber.split("-")[0].toInt() }, { it.displayNumber.split("-").getOrElse(1) { "0" }.toInt() })))
            }
        }catch (E: Exception){
            return channelList
        }
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
    override fun clearChannelidsPref(){
    }

    override fun hasHohAudio(tvTrackInfo: TvTrackInfo): Boolean {

         return false
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