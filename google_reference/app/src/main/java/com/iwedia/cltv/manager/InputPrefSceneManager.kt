package com.iwedia.cltv.manager

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.tv.ContentRatingSystem
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.*
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.PreferenceSubMenuItem
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.*
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.`interface`.HbbTvInterface.*
import com.iwedia.cltv.platform.model.*
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.input_pref_scene.InputPrefScene
import com.iwedia.cltv.scene.input_pref_scene.InputPrefSceneListener
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus
import utils.information_bus.events.Events
import world.SceneManager
import java.util.*


class InputPrefSceneManager : GAndroidSceneManager, InputPrefSceneListener {
    private val TAG = javaClass.simpleName
    private var hbbTvModule: HbbTvInterface
    private var tvModule: TvInterface

    /**
     * Update time timer
     */
    private var updateTimeTask: TimerTask? = null
    private var updateTimeTimer: Timer? = null
    private var tvInputModule: TvInputInterface
    private var parentalControlSettingsModule: ParentalControlSettingsInterface
    private var utilsModule: UtilsInterface
    private  var ciPlusModule: CiPlusInterface
    private var preferenceModule: PreferenceInterface
    private var textToSpeechModule: TTSInterface
    private var closedCaptionModule: ClosedCaptionInterface
    private var inputSourceModule: InputSourceInterface
    private  var networkModule: NetworkInterface
    private var timeModule: TimeInterface
    private var defaultInputValue = "Composite"
    private lateinit var activeChannel: TvChannel
    private lateinit var generalConfigModule: GeneralConfigInterface


    // Settings opened flag
    private var settingsOpened = false

    /**
     * Default Aspect Ratio Option
     */
    private var defaultAspectRatioOption: Int? = null
    private var preferenceChannelsModule: PreferenceChannelsInterface


    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        hbbTvModule: HbbTvInterface,
        tvModule1: TvInterface,
        preferenceModule: PreferenceInterface,
        tvInputModule: TvInputInterface,
        parentalControlSettingsModule: ParentalControlSettingsInterface,
        utilsModule: UtilsInterface,
        ciPlusModule: CiPlusInterface,
        closedCaptionModule: ClosedCaptionInterface,
        preferenceChannelsModule: PreferenceChannelsInterface,
        inputSourceModule: InputSourceInterface,
        timeModule: TimeInterface,
        networkModule: NetworkInterface,
        generalConfigModule: GeneralConfigInterface,
        textToSpeechModule: TTSInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE
    ) {
        isScreenFlowSecured = false
        this.hbbTvModule = hbbTvModule
        this.preferenceModule = preferenceModule
        this.textToSpeechModule = textToSpeechModule
        this.tvInputModule = tvInputModule
        this.tvModule = tvModule1
        this.parentalControlSettingsModule = parentalControlSettingsModule
        this.utilsModule = utilsModule
        this.ciPlusModule = ciPlusModule
        this.closedCaptionModule = closedCaptionModule
        this.preferenceChannelsModule = preferenceChannelsModule
        this.inputSourceModule = inputSourceModule
        this.timeModule = timeModule
        this.networkModule = networkModule
        this.generalConfigModule = generalConfigModule
        ReferenceApplication.isPrefClicked = true
        registerGenericEventListener(Events.CHANNEL_CHANGED)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun getAvailablePreferenceTypes(callback: IAsyncDataCallback<List<CategoryItem>>) {
        val categories = mutableListOf<CategoryItem>()
        categories.add(CategoryItem(PrefType.PLATFORM, ""))
        callback.onReceive(categories)
    }

    override fun getAvailablePreferencesCategories(callback: IAsyncDataCallback<List<CategoryItem>>, type: PrefType) {
        val menuOptionList = preferenceModule.getPreferenceMenus(type)

        //Todo remove
        val categories = mutableListOf<CategoryItem>()
        for (menu in menuOptionList) {
            if (menu == PrefMenu.PARENTAL_CONTROL)
                categories.add(
                    CategoryItem(
                        ReferenceWorldHandler.WidgetId.PREFERENCES_PARENTAL_CONTROL,
                        ConfigStringsManager.getStringById("parental_control")
                    )
                )
            if (defaultInputValue.contains("Composite")) {
                if (menu == PrefMenu.CLOSED_CAPTIONS)
                    categories.add(
                        CategoryItem(
                            ReferenceWorldHandler.WidgetId.PREFERENCES_CLOSED_CAPTIONS,
                            ConfigStringsManager.getStringById("closed_captions")
                        )
                    )
            }
        }
        callback.onReceive(categories)
    }

    override fun getAudioInformation() {
    }

    override fun onAudioFirstLanguageClicked(languageCode: LanguageCode) {
        utilsModule.setPrimaryAudioLanguage(languageCode.languageCodeISO6392)
    }

    override fun onAudioSecondLanguageClicked(languageCode: LanguageCode) {
        utilsModule.setPrimaryAudioLanguage(languageCode.languageCodeISO6392)
    }

    override fun onAudioTypeClicked(position: Int) {
        utilsModule.setAudioType(position)
    }

    override fun getSubtitleInformation() {
    }

    override fun getSystemInformation() {
    }

    override fun onSubtitleFirstLanguageClicked(languageCode: LanguageCode) {
    }

    override fun onSubtitleSecondLanguageClicked(languageCode: LanguageCode) {
    }

    override fun onSubtitlesEnabledClicked(isEnabled: Boolean) {
    }

    override fun onSubtitlesTypeClicked(position: Int) {
    }

    override fun onChannelsScanClicked() {
    }

    override fun onChannelsEditClicked() {
    }

    override fun getPreferencesTxtData() {
    }

    override fun onTTXDigitalLanguageClicked(position: Int, language: String) {
    }

    override fun onTTXDecodeLanguageClicked(position: Int) {
    }

    override fun onDisplayModeSelected(selectedMode: Int) {
    }

    override fun getPreferencesCamInfoModuleInformation() {
    }

    override fun onPreferencesSoftwareDownloadPressed() {
    }

    override fun onPreferencesChangeCaPinPressed() {
    }

    override fun onPreferencesConaxCaMessagesPressed() {
    }

    override fun onPreferencesAboutConaxCaPressed() {
    }

    override fun getPreferencesCamInfoMaturityRating(): String {
        return ""
    }

    override fun getPreferencesCamInfoSettingsLanguages() {
    }

    override fun onPreferencesCamInfoSettingsLanguageSelected(position: Int) {
    }

    override fun onPreferencesCamInfoPopUpMessagesActivated(activated: Boolean) {
    }

    override fun isPreferencesCamInfoPopUpMessagesActivated(): Boolean {
        return false
    }

    override fun getHbbTvInformation() {
    }

    override fun getPreferencesCamData() {

    }

    override fun onHbbSupportSwitch(isEnabled: Boolean) {
    }

    override fun onHbbTrackSwitch(isEnabled: Boolean) {
    }

    override fun onHbbPersistentStorageSwitch(isEnabled: Boolean) {
    }

    override fun onHbbBlockTracking(isEnabled: Boolean) {
    }

    override fun onHbbTvDeviceId(isEnabled: Boolean) {
    }

    override fun onHbbTvResetDeviceId() {
    }

    override fun onHbbCookieSettingsSelected(position: Int) {
    }

    override fun onAspectRatioClicked(position: Int) {
    }

    override fun onParentalControlClicked() {
        settingsOpened = true

        try {
            //Try triggering PARENTAL SCREEN from custom settings
            val intent = Intent("android.settings.PARENTAL_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("fromApp", true)
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
        }
    }

    private fun buildName(it: PrefSubMenu): String? {
        if (defaultInputValue.contains("Composite")) {
            return when(it){
                PrefSubMenu.CHANNELS -> ConfigStringsManager.getStringById("channels")
                PrefSubMenu.CHANNELS_SETTING -> ConfigStringsManager.getStringById("channels")
                PrefSubMenu.CHANNEL_SCAN -> ConfigStringsManager.getStringById("channel_scan")
                PrefSubMenu.POSTAL_CODE -> ConfigStringsManager.getStringById("postal_code")
                PrefSubMenu.CHANNEL_EDIT -> ConfigStringsManager.getStringById("channel_edit")
                PrefSubMenu.DEFAULT_CHANNEL -> ConfigStringsManager.getStringById("default_channel")
                PrefSubMenu.PICTURE -> ConfigStringsManager.getStringById("picture")
                PrefSubMenu.SOUND -> ConfigStringsManager.getStringById("sound")
                PrefSubMenu.SCREEN -> ConfigStringsManager.getStringById("screen")
                PrefSubMenu.POWER -> ConfigStringsManager.getStringById("power")
                PrefSubMenu.LCN -> ConfigStringsManager.getStringById("lcn")
                PrefSubMenu.ANTENNA -> ConfigStringsManager.getStringById("antenna")
                PrefSubMenu.PARENTAL_CONTROL -> ConfigStringsManager.getStringById("parental_control")
                PrefSubMenu.INTERACTION_CHANNEL -> ConfigStringsManager.getStringById("interaction_channel")
                PrefSubMenu.MHEG_PIN -> ConfigStringsManager.getStringById("mpeg_pin")
                PrefSubMenu.EVALUATION_LICENCE -> ConfigStringsManager.getStringById("evaluation_license_pref")
                PrefSubMenu.DISPLAY_MODE -> ConfigStringsManager.getStringById("display_mode")
                PrefSubMenu.ASPECT_RATIO -> ConfigStringsManager.getStringById("aspect_ratio")
                PrefSubMenu.PREFERRED_EPG_LANGUAGE -> ConfigStringsManager.getStringById("preferred_epg_language")
                PrefSubMenu.BLUE_MUTE -> ConfigStringsManager.getStringById("blue_mute")
                PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF  -> ConfigStringsManager.getStringById("no_signal_power_off")
                PrefSubMenu.FIRST_LANGUAGE  -> ConfigStringsManager.getStringById(
                    if(utilsModule.getRegion()== Region.US) "preferred_language" else "first_language") //for us region title is Preferred language and for other its first language
                PrefSubMenu.SECOND_LANGUAGE  -> ConfigStringsManager.getStringById("second_language")
                PrefSubMenu.AUDIO_DESCRIPTION  -> ConfigStringsManager.getStringById("audio_description")
                PrefSubMenu.AUTO_SERVICE_UPDATE -> ConfigStringsManager.getStringById("auto_service_update")
                PrefSubMenu.AUDIO_TYPE -> ConfigStringsManager.getStringById("audio_type")
                PrefSubMenu.GENERAL  -> ConfigStringsManager.getStringById("general")
                PrefSubMenu.CLOSED_CAPTIONS  -> ConfigStringsManager.getStringById("closed_caption")
                //parental control
                PrefSubMenu.CHANNEL_BLOCK  -> ConfigStringsManager.getStringById("channel_block")
                PrefSubMenu.INPUT_BLOCK  -> ConfigStringsManager.getStringById("Input_block")
                PrefSubMenu.RATING_SYSTEMS  -> ConfigStringsManager.getStringById("rating_systems")
                PrefSubMenu.RATING_LOCK  -> ConfigStringsManager.getStringById("Rating_lock")
                PrefSubMenu.ANOKI_PARENTAL_RATING  -> "Anoki Parental Rating"
                PrefSubMenu.RRT5_LOCK  -> ConfigStringsManager.getStringById("rrt5_lock")
                PrefSubMenu.CHANGE_PIN  -> ConfigStringsManager.getStringById("change_pin")
                PrefSubMenu.ANALOG_SUBTITLE -> ConfigStringsManager.getStringById("analog_subtitle")
                PrefSubMenu.DIGITAL_SUBTITLE -> ConfigStringsManager.getStringById("digital_subtitle")
                PrefSubMenu.SUBTITLE_TYPE  -> ConfigStringsManager.getStringById("subtitle_type")
                PrefSubMenu.AUDIO_DESCRIPTION_KONKA  -> ConfigStringsManager.getStringById("audio_description")
                PrefSubMenu.ENABLE_SUBTITLES  -> ConfigStringsManager.getStringById("enable_subtitles")
                PrefSubMenu.AUDIO_FORMAT  -> ConfigStringsManager.getStringById("audio_format")
                PrefSubMenu.HEARING_IMPAIRED  -> ConfigStringsManager.getStringById("subtitle_type_hearing_impaired")
                PrefSubMenu.VISUALLY_IMPAIRED, PrefSubMenu.VISUALLY_IMPAIRED_MINIMAL  -> ConfigStringsManager.getStringById("audio_type_visually_impaired")
                PrefSubMenu.PREFERED_TELETEXT_LANGUAGE  -> ConfigStringsManager.getStringById("preferred_language")
                PrefSubMenu.DIGITAL_LANGUAGE  -> ConfigStringsManager.getStringById("Digital language")
                PrefSubMenu.DECODING_PAGE_LANGUAGE  -> ConfigStringsManager.getStringById("decoding_page_language")
                PrefSubMenu.DEVICE_INFO  -> ConfigStringsManager.getStringById("device_info")
                PrefSubMenu.TIMESHIFT_MODE  -> ConfigStringsManager.getStringById("timeshift_mode")
                //hbbtv
                PrefSubMenu.HBBTV_SUPPORT ->  ConfigStringsManager.getStringById("hbbtv_support")
                PrefSubMenu.COOKIE_SETTING ->  ConfigStringsManager.getStringById("hbbtv_cookie_settings")
                PrefSubMenu.TRACK ->  ConfigStringsManager.getStringById("hbbtv_track")
                PrefSubMenu.PERSISTENT_STORAGE ->  ConfigStringsManager.getStringById("hbbtv_persisent_storage")
                PrefSubMenu.BLOCK_TRACKING_SITES ->  ConfigStringsManager.getStringById("hbbtv_block_tracking_sites")
                PrefSubMenu.DEVICE_ID ->  ConfigStringsManager.getStringById("hbbtv_device_id")
                PrefSubMenu.RESET_DEVICE_ID ->  ConfigStringsManager.getStringById("hbbtv_reset_device_id")
                PrefSubMenu.CAPTION_SERVICES ->  ConfigStringsManager.getStringById("caption_services")
                //cc
                PrefSubMenu.DISPLAY_CC ->  ConfigStringsManager.getStringById("display_cc")
                PrefSubMenu.ADVANCED_SELECTION ->  ConfigStringsManager.getStringById("advanced_selection")
                PrefSubMenu.TEXT_SIZE ->  ConfigStringsManager.getStringById("text_size")
                PrefSubMenu.FONT_FAMILY ->  ConfigStringsManager.getStringById("font_family")
                PrefSubMenu.TEXT_COLOR ->  ConfigStringsManager.getStringById("text_color")
                PrefSubMenu.TEXT_OPACITY ->  ConfigStringsManager.getStringById("text_opacity")
                PrefSubMenu.EDGE_TYPE ->  ConfigStringsManager.getStringById("edge_type")
                PrefSubMenu.EDGE_COLOR ->  ConfigStringsManager.getStringById("edge_color")
                PrefSubMenu.BACKGROUND_COLOR ->  ConfigStringsManager.getStringById("background_color")
                PrefSubMenu.BACKGROUND_OPACITY ->  ConfigStringsManager.getStringById("background_opacity")
                //channel edit konka
                PrefSubMenu.CHANNEL_EDIT_MENUS -> ConfigStringsManager.getStringById("channel_edit")
                else -> it.name
            }
        }
        else if (defaultInputValue.contains("HDMI")) {
            return when (it) {
                PrefSubMenu.INPUT_BLOCK -> ConfigStringsManager.getStringById("Input_block")

                else -> it.name

            }
        }

        return ""
    }

    override fun onPreferencesDefaultChannelSelected(tvChannel: TvChannel) {
        var triplet =
            tvChannel.onId.toString() + "|" + tvChannel.tsId + "|" + tvChannel.serviceId
        utilsModule.setPrefsValue(
            ReferenceApplication.DEFAULT_CHANNEL,
            triplet
        )
    }

    override fun onClosedCaptionChanged(isCaptionEnabled: Boolean) {
        setCaptionsEnabled(isCaptionEnabled)
    }

    private fun setCaptionsEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context!!.contentResolver,
            "accessibility_captioning_enabled", if (enabled) 1 else 0
        )
    }

    override fun onPreferencesSubscriptionStatusPressed() {
    }

    override fun onPreferencesEventStatusPressed() {
    }

    override fun onPreferencesTokenStatusPressed() {
    }


    override fun onEvaluationLicenseClicked() {
        context!!.runOnUiThread {
            worldHandler!!.triggerAction(id, Action.HIDE)
            worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.EVALUATION_SCENE,
                Action.SHOW
            )
        }
    }

    override fun onOpenSourceLicenceClicked() {
        context!!.runOnUiThread {
            worldHandler!!.triggerAction(id, Action.HIDE)
            worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.OPEN_SOURCE_LICENSE_SCENE,
                Action.SHOW
            )
        }
    }

    override fun getChannelsOfSelectedFilter(): MutableList<TvChannel> {
        return mutableListOf()
    }

    override fun onEpgLanguageChanged(language: String) {
    }

    override fun setInteractionChannel(isSelected: Boolean) {
        utilsModule.setPrefsValue(
            ReferenceApplication.INTERACTION_CHANNEL,
            isSelected
        )
    }

    override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getParentalInformation(type: PrefType) {
        val prefParentalOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.PARENTAL_CONTROL, type)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        var blockUnratedProgramsAvailability = false

        prefParentalOptions.forEach {
            if(it ==PrefSubMenu.BLOCK_UNRATED_PROGRAMS){
                blockUnratedProgramsAvailability = true
            }else{
                subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
            }
        }

        var blockedInputCount = 0
        val blockedInputs = mutableListOf<InputSourceData>()



        inputSourceModule.getInputList(object : IAsyncDataCallback<ArrayList<InputItem>> {
            override fun onFailed(error: Error) {

            }

            override fun onReceive(data: ArrayList<InputItem>) {
                data.forEach {
                    if (!it.isHidden!!) {
                        if (!it.inputMainName.contains("Home")) {
                            blockedInputs.add(
                                InputSourceData(
                                    it.inputSourceName,
                                    it.hardwareId,
                                    it.inputMainName
                                )
                            )
                        }
                    }
                }
            }

        })

        blockedInputs.forEach {
            it.isBlocked = inputSourceModule.isBlock(it.inputMainName)
        }

        blockedInputCount = inputSourceModule.blockInputCount(blockedInputs)
        val contentRatingsData = getContentRatingsData()

        val contentRatingsEnabled = mutableListOf<ContentRatingSystem>()

        contentRatingsData.forEach {
            if(parentalControlSettingsModule.isContentRatingSystemEnabled(it)) contentRatingsEnabled.add(it)
        }
        var mode = if (type==PrefType.BASE) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT

        //if preftype is base then we are fetching only fast channels else broadcast channels
        var channelList = tvModule.getChannelList(mode)
        if (type == PrefType.PLATFORM){
            //for platform channels we are getting some non browsable channels
            channelList.toList().forEach {
                if (!tvModule.isChannelLockAvailable(it)){
                    channelList.remove(it)
                }
            }
        }

        val parentalInformation = PreferencesParentalControlInformation(
            subCategories,
            channelList,
            blockedInputs,
            blockedInputCount,
            blockUnratedProgramsAvailability,
            parentalControlSettingsModule.getBlockUnrated()==1,
            contentRatingsData,
            contentRatingsEnabled,
            parentalControlSettingsModule.getGlobalRestrictionsArray(),
            parentalControlSettingsModule.getAnokiRatingList()
        )
        (scene as InputPrefScene).preferencesSceneWidget?.refresh(parentalInformation)
    }

    private fun getContentRatingsData(): MutableList<ContentRatingSystem> {
        val ratingSystemList = tvInputModule.getContentRatingSystemsList()
        return ratingSystemList
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getClosedCationsInformation() {
        val prefOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.CLOSED_CAPTIONS)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        prefOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }
        val captionServicesList: ArrayList<String> = arrayListOf()
        val advancedSelectionList: ArrayList<String> = arrayListOf()
        val textSizeList: ArrayList<String> = arrayListOf()
        val fontFamilyList: ArrayList<String> = arrayListOf()
        val captionColorList: ArrayList<String> = arrayListOf()
        val captionOpacityList: ArrayList<String> = arrayListOf()
        val edgeTypeList: ArrayList<String> = arrayListOf()

        captionServicesList.apply {
            add(ConfigStringsManager.getStringById("cc_1"))
            add(ConfigStringsManager.getStringById("cc_2"))
            add(ConfigStringsManager.getStringById("cc_3"))
            add(ConfigStringsManager.getStringById("cc_4"))
            add(ConfigStringsManager.getStringById("t_1"))
            add(ConfigStringsManager.getStringById("t_2"))
            add(ConfigStringsManager.getStringById("t_3"))
            add(ConfigStringsManager.getStringById("t_4"))
        }
        advancedSelectionList.apply {
            add(ConfigStringsManager.getStringById("cs_1"))
            add(ConfigStringsManager.getStringById("cs_2"))
            add(ConfigStringsManager.getStringById("cs_3"))
            add(ConfigStringsManager.getStringById("cs_4"))
            add(ConfigStringsManager.getStringById("cs_5"))
            add(ConfigStringsManager.getStringById("cs_6"))
        }
        textSizeList.apply {
            add(ConfigStringsManager.getStringById("_default"))
            add(ConfigStringsManager.getStringById("small"))
            add(ConfigStringsManager.getStringById("audio_type_normal"))
            add(ConfigStringsManager.getStringById("large"))
        }
        fontFamilyList.apply {
            add(ConfigStringsManager.getStringById("_default"))
            add(ConfigStringsManager.getStringById("serif_monospace"))
            add(ConfigStringsManager.getStringById("serif"))
            add(ConfigStringsManager.getStringById("sans_serif_monospace"))
            add(ConfigStringsManager.getStringById("sans_serif"))
            add(ConfigStringsManager.getStringById("casual"))
            add(ConfigStringsManager.getStringById("cursive"))
            add(ConfigStringsManager.getStringById("small_capitals"))
        }
        captionColorList.apply {
            add(ConfigStringsManager.getStringById("_default"))
            add(ConfigStringsManager.getStringById("black"))
            add(ConfigStringsManager.getStringById("white"))
            add(ConfigStringsManager.getStringById("green"))
            add(ConfigStringsManager.getStringById("blue"))
            add(ConfigStringsManager.getStringById("red"))
            add(ConfigStringsManager.getStringById("cyan"))
            add(ConfigStringsManager.getStringById("yellow"))
            add(ConfigStringsManager.getStringById("magenta"))
        }
        captionOpacityList.apply {
            add(ConfigStringsManager.getStringById("_default"))
            add(ConfigStringsManager.getStringById("solid"))
            add(ConfigStringsManager.getStringById("translucent"))
            add(ConfigStringsManager.getStringById("transparent"))
            add(ConfigStringsManager.getStringById("flashing"))
        }
        edgeTypeList.apply {
            add(ConfigStringsManager.getStringById("_default"))
            add(ConfigStringsManager.getStringById("none"))
            add(ConfigStringsManager.getStringById("raised"))
            add(ConfigStringsManager.getStringById("depressed"))
            add(ConfigStringsManager.getStringById("outline"))
            add(ConfigStringsManager.getStringById("drop_shadow"))
        }

        val defCaptionDisplay: Int? = closedCaptionModule.getDefaultCCValues("display_cc")
        val withMute = closedCaptionModule.getDefaultMuteValues()
        var defCaptionServices = closedCaptionModule.getDefaultCCValues("caption_services")
        var defAdvancedSelection = closedCaptionModule.getDefaultCCValues("advanced_selection")
        var defTextSize = closedCaptionModule.getDefaultCCValues("text_size")
        var defFontFamily = closedCaptionModule.getDefaultCCValues("font_family")
        var defTextColor = closedCaptionModule.getDefaultCCValues("text_color")
        var defTextOpacity = closedCaptionModule.getDefaultCCValues("text_opacity")
        var defEdgeType = closedCaptionModule.getDefaultCCValues("edge_type")
        var defEdgeColor = closedCaptionModule.getDefaultCCValues("edge_color")
        var defBgColor = closedCaptionModule.getDefaultCCValues("background_color")
        var defBgOpacity = closedCaptionModule.getDefaultCCValues("background_opacity")
        
        val lastSelectedTextOpacity = utilsModule.getPrefsValue("TEXT_OPACITY",0)as Int?
        val information = PreferencesClosedCaptionsInformation(
            subCategories,
            withMute as Boolean?, defCaptionDisplay, captionServicesList, defCaptionServices,
            advancedSelectionList, defAdvancedSelection, textSizeList, defTextSize,
            fontFamilyList, defFontFamily, captionColorList, defTextColor, captionOpacityList,
            defTextOpacity, edgeTypeList, defEdgeType, captionColorList, defEdgeColor,
            captionColorList, defBgColor, captionOpacityList, defBgOpacity, lastSelectedTextOpacity
        )
        (scene as InputPrefScene).preferencesSceneWidget?.refresh(information)
    }

    override fun getPreferencesPvrTimeshiftData() {
    }

    override fun getPrefsValue(key: String, value: Any?): Any? {
        return utilsModule.getPrefsValue(key, value)
    }

    override fun setPrefsValue(key: String, defValue: Any?) {
        utilsModule.setPrefsValue(key, defValue!!)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun lockChannel(tvChannel: TvChannel, selected: Boolean) {
        var applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        tvModule.lockUnlockChannel(
            tvChannel,
            selected,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {
                }

                override fun onSuccess() {
                }

            }, applicationMode
        )
    }

    override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
        return tvModule.isChannelLockAvailable(tvChannel)
    }

    override fun blockInput(inputSourceData: InputSourceData, blocked: Boolean) {
        inputSourceModule.blockInput(blocked, inputSourceData.inputMainName)
        var defaultInput = utilsModule.getPrefsValue("inputSelectedString", "TV") as String
        if (defaultInput == inputSourceData.inputMainName && blocked) {
            InformationBus.submitEvent(
                Event(
                    com.iwedia.cltv.platform.model.information_bus.events.Events.BLOCK_TV_VIEW,
                    inputSourceData.inputSourceName
                )
            )
        }
    }

    override fun isParentalControlsEnabled(): Boolean {
        return parentalControlSettingsModule.isParentalControlsEnabled()
    }

    override fun setParentalControlsEnabled(enabled: Boolean) {
        return parentalControlSettingsModule.setParentalControlsEnabled(enabled)
    }

    override fun setContentRatingSystemEnabled(
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean
    ) {
        parentalControlSettingsModule.setContentRatingSystemEnabled(
            tvInputModule,
            contentRatingSystem,
            enabled
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setContentRatingLevel(index: Int) {
        parentalControlSettingsModule.setContentRatingLevel(tvInputModule, index)
    }

    override fun setRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating,
        data: Boolean
    ): Boolean {
        return parentalControlSettingsModule.setRatingBlocked(contentRatingSystem, it, data)
    }

    override fun setRelativeRatingsEnabled(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating,
        data: Boolean
    ) {
        parentalControlSettingsModule.setRelativeRatingsEnabled(contentRatingSystem, it, data)
    }

    override fun isRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating
    ): Boolean {
        return parentalControlSettingsModule.isRatingBlocked(contentRatingSystem, it)
    }

    override fun isSubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ): Boolean {
        return parentalControlSettingsModule.isSubRatingEnabled(
            contentRatingSystem,
            it,
            subRating
        )
    }

    override fun getContentRatingLevelIndex(): Int {
        return parentalControlSettingsModule.getContentRatingLevelIndex()
    }

    override fun setSubRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating,
        data: Boolean
    ) {
        parentalControlSettingsModule.setSubRatingBlocked(
            contentRatingSystem,
            rating,
            subRating,
            data
        )
    }

    override fun setRelativeRating2SubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        data: Boolean,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ) {
        parentalControlSettingsModule.setRelativeRating2SubRatingEnabled(
            contentRatingSystem,
            data,
            rating,
            subRating
        )
    }

    override fun setBlockUnrated(blocked: Boolean) {
        parentalControlSettingsModule.setBlockUnrated(blocked)
    }

    override fun getRRT5DimInfo(index: Int): MutableList<String> {
        return parentalControlSettingsModule.getRRT5Dim(index)
    }

    override fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem> {
        return parentalControlSettingsModule.getRRT5CrsInfo(regionName)
    }

    override fun getRRT5LevelInfo(
        countryIndex: Int,
        dimIndex: Int
    ): MutableList<String> {
        return parentalControlSettingsModule.getRRT5Level(countryIndex, dimIndex)
    }

    override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
        return parentalControlSettingsModule.getSelectedItemsForRRT5Level()
    }

    override fun rrt5BlockedList(regionPosition: Int, position: Int): HashMap<Int, Int> {
        return parentalControlSettingsModule.rrt5BlockedList(regionPosition, position)
    }

    override fun setSelectedItemsForRRT5Level(
        regionIndex: Int,
        dimIndex: Int,
        levelIndex: Int
    ) {
        parentalControlSettingsModule.setSelectedItemsForRRT5Level(
            regionIndex,
            dimIndex,
            levelIndex
        )
    }

    override fun resetRRT5() {
        parentalControlSettingsModule.resetRRT5()
    }

    override fun getRRT5Regions(): MutableList<String> {
        return parentalControlSettingsModule.getRRT5Regions()
    }

    override fun onSkipChannelSelected(tvChannel: TvChannel, status: Boolean) {
        tvModule.skipUnskipChannel(tvChannel, status)
    }

    override fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int) {
        tvModule.deleteChannel(tvChannel)
    }

    override fun onSwapChannelSelected(
        firstChannel: TvChannel,
        secondChannel: TvChannel,
        previousPosition: Int,
        newPosition: Int
    ) {
        preferenceChannelsModule.swapChannel(
            firstChannel,
            secondChannel,
            previousPosition,
            newPosition
        )
    }

    override fun onMoveChannelSelected(
        moveChannelList: ArrayList<TvChannel>,
        previousPosition: Int,
        newPosition: Int,
        channelMap: HashMap<Int, String>
    ) {
        preferenceChannelsModule.moveChannel(moveChannelList, previousPosition, newPosition, channelMap)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onClearChannelSelected() {
        var ret = preferenceChannelsModule.deleteAllChannels()
        InformationBus.submitEvent(
            Event(com.iwedia.cltv.platform.model.information_bus.events.Events.CHANNEL_LIST_IS_EMPTY)
        )
        ReferenceApplication.worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE,
            SceneManager.Action.DESTROY
        )
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int) {
        closedCaptionModule.saveUserSelectedCCOptions(ccOptions, newValue, true)
    }

    override fun resetCC() {
        closedCaptionModule.resetCC()
    }

    override fun setCCInfo() {
        closedCaptionModule.setCCInfo()
    }

    override fun setCCWithMute(isEnabled: Boolean) {
        closedCaptionModule.setCCWithMute(
            isEnabled,
            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        )
    }

    override fun lastSelectedTextOpacity(position: Int) {
        utilsModule.setPrefsValue("TEXT_OPACITY", position)
    }

    override fun getUtilsModule(): UtilsInterface {
        return utilsModule
    }

    override fun isLcnEnabled(): Boolean {
        return tvModule.isLcnEnabled()
    }

    override fun getCiPlusInterface(): CiPlusInterface {
        return ciPlusModule
    }

    override fun isAccessibilityEnabled(): Boolean {
        return utilsModule.isAccessibilityEnabled()
    }

    override fun changeChannelFromTalkback(tvChannel: TvChannel) {
        tvModule.changeChannel(tvChannel, object: IAsyncCallback{
            override fun onFailed(error: Error) {
                println(error)
            }

            override fun onSuccess() {
                println("changeChannelFromTalkback onSuccess")
            }
        })
    }

    override fun checkNetworkConnection(): Boolean {
        if (networkModule.networkStatus.value == NetworkData.NoConnection) {
            showToast(ConfigStringsManager.getStringById("no_internet_message"), UtilsInterface.ToastDuration.LENGTH_LONG)
            return false
        }
        return true
    }

    override fun setAnokiRatingLevel(level: Int) {
        //Show dialog when the last rating item is selected
        if (parentalControlSettingsModule.getAnokiRatingList().size - 1 == level) {
            context!!.runOnUiThread {
                var sceneData = DialogSceneData(id, instanceId)
                sceneData.type = DialogSceneData.DialogType.YES_NO
                val ratingName = parentalControlSettingsModule.getAnokiRatingList().get(parentalControlSettingsModule.getAnokiRatingList().size - 1)
                //TODO dialog title text should be translated
                sceneData.title = "Do you want to set $ratingName as permanent rating level?"/*ConfigStringsManager.getStringById("app_exit_msg")*/
                sceneData.positiveButtonText = ConfigStringsManager.getStringById("yes")
                sceneData.negativeButtonText = ConfigStringsManager.getStringById("no")
                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                    override fun onNegativeButtonClicked() {
                        parentalControlSettingsModule.setAnokiRatingLevel(level, temporary = true)
                    }

                    override fun onPositiveButtonClicked() {
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            Action.DESTROY, sceneData
                        )
                        parentalControlSettingsModule.setAnokiRatingLevel(level, temporary = false)
                    }
                }
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.SHOW_OVERLAY, sceneData
                )
            }
        } else {
            parentalControlSettingsModule.setAnokiRatingLevel(level)
        }
    }

    override fun getAnokiRatingLevel(): Int {
        return parentalControlSettingsModule.getAnokiRatingLevel()
    }

    override fun setAnokiParentalControlsEnabled(enabled: Boolean) {
        parentalControlSettingsModule.setAnokiParentalControlsEnabled(enabled)
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule.getGeneralSettingsInfo(nameOfInfo)
    }

    override fun isAnokiParentalControlsEnabled(): Boolean {
        return parentalControlSettingsModule.isAnokiParentalControlsEnabled()
    }

    override fun getSignalAvailable(): Boolean {
        return tvModule.playbackStatusInterface.isSignalAvailable
    }

    override fun onVolumeChanged(newVolumeValue: Int) {
        utilsModule.setVolumeValue(newVolumeValue)
    }

    override fun onVisuallyImpairedValueChanged(position: Int, enabled: Boolean) {
        utilsModule.setVisuallyImpairedValues(position, enabled)
    }

    override fun getChannelSourceType(tvChannel: TvChannel): String {
        return tvModule.getChannelSourceType(tvChannel)
    }


    override fun setAudioDescriptionEnabled(enable: Boolean) {
        utilsModule.setAudioDescriptionEnabled(enable)
    }

    override fun setHearingImpairedEnabled(enable: Boolean) {
        utilsModule.setHearingImpairedEnabled(enable)
    }

    override fun onFaderValueChanged(newValue: Int) {
        utilsModule.setFaderValue(newValue)
    }

    override fun onAudioViValueChanged(newValue: Int) {
        utilsModule.setVisuallyImpairedAudioValue(newValue)
    }

    override fun disableCCInfo() {
        closedCaptionModule.disableCCInfo()
    }

    override fun createScene() {
        scene = InputPrefScene(context!!, this)
    }

    override fun onSceneInitialized() {
        scene!!.refresh(true)
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
            }

            override fun onReceive(data: TvChannel) {
                activeChannel = data
            }
        })
        refreshTime()
        startUpdateTimeTimerTask()
        defaultInputValue = inputSourceModule.getDefaultValue()
    }


    override fun onResume() {
        super.onResume()
        if (timeModule.getCurrentTime() - updateTimeTask?.scheduledExecutionTime()!! >=
            60000
        ) {
            startUpdateTimeTimerTask()
        } else {
            refreshTime()
        }
    }
    private fun refreshTime() {
        if (this::activeChannel.isInitialized) {
            val currentTime = timeModule.getCurrentTime(activeChannel)
            scene?.refresh(currentTime)
        }
    }

    private fun startUpdateTimeTimerTask() {
        if (updateTimeTimer == null) {
            updateTimeTimer = Timer()
        }
        updateTimeTask = object : TimerTask() {
            override fun run() {
                ReferenceApplication.runOnUiThread(Runnable { refreshTime() })
            }
        }
        updateTimeTimer?.scheduleAtFixedRate(updateTimeTask, 0, (60 * 1000).toLong())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (updateTimeTask != null) {
            updateTimeTask!!.cancel()
            updateTimeTask = null
        }
        if (updateTimeTimer != null) {
            updateTimeTimer?.cancel()
            updateTimeTimer?.purge()
            updateTimeTimer = null
        }
    }

}