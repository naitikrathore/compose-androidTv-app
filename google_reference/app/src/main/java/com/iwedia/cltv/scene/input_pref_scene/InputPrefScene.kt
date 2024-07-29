package com.iwedia.cltv.scene.input_pref_scene

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.ContentRatingSystem
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TimeTextView
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.ReferenceWidgetPreferences
import com.iwedia.cltv.components.ReferenceWidgetPreferences.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigCompanyDetailsManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.home_scene.HomeSceneListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import core_entities.Error
import data_type.GLong
import listeners.AsyncReceiver
import world.SceneListener
import world.SceneManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Input Prefs Scene
 *
 * @author Vandana Bedhi
 */
class InputPrefScene(context: Context, sceneListener: SceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE),
    sceneListener
) {

    private val TAG = javaClass.simpleName

    var timeTv: TimeTextView? = null
    var homeContainer: ConstraintLayout? = null
    var sceneNameTv: TextView? = null
    var preferenceFirstTime = true

    //Current time
    private var date: Date? = null


    //TODO Dummy list for categories
    val list = mutableListOf<CategoryItem>()


    /**
     * Home scene widgets
     */
    var preferencesSceneWidget: ReferenceWidgetPreferences? = null
    private var enableCaching = true

    /**
     * Guide channel list
     */
    //var guideChannelList = mutableListOf<ReferenceTvChannel>()


    var mainConstraintLayout: ConstraintLayout? = null


    override fun createView() {
        super.createView()

        view = GAndroidSceneFragment(name, R.layout.layout_scene_pref, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {

                val gradientBackground: View = view!!.findViewById(R.id.gradient_background)
                Utils.makeGradient(
                    gradientBackground,
                    GradientDrawable.LINEAR_GRADIENT,
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            ConfigColorManager.getColor("color_background"),
                            0.5
                        )
                    ),
                    Color.parseColor(
                        ConfigColorManager.getColor(
                            ConfigColorManager.getColor("color_background"),
                            0.9
                        )
                    ),
                    0.0F,
                    0.0F
                )

                timeTv = view!!.findViewById(R.id.home_tv_time)
                timeTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                homeContainer = view!!.findViewById(R.id.home_container)
                sceneNameTv = view!!.findViewById(R.id.home_scene_name)
                timeTv?.typeface = TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_regular")
                )

                handleCategoryFocused()


                //parseConfig(configParam!!)
                sceneListener.onSceneInitialized()

            }

        })
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) &&
            ((homeContainer != null) && (homeContainer?.hasFocus()!!))
        ) {
            return true
        }

        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {

        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }


    private fun handleCategoryFocused() {

        //Preferences is already shown
        if (preferencesSceneWidget != null) {
            return
        }

        homeContainer?.removeAllViews()
        preferencesSceneWidget = null
        createPreferencesWidget()
        (sceneListener as InputPrefSceneListener).getAvailablePreferenceTypes(
            object : IAsyncDataCallback<List<CategoryItem>> {


                override fun onFailed(error: kotlin.Error) {
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onReceive(data: List<CategoryItem>) {
                    runOnUiThread {
                        preferencesSceneWidget!!.refresh(data)
                    }
                }

            })

        homeContainer?.addView(preferencesSceneWidget?.view)
        preferencesSceneWidget?.setFocusToPrefSub()


    }

    private fun handleCategoryClicked(position: Int) {
        //Preferences is already shown
        if ((preferencesSceneWidget != null && !(sceneListener as InputPrefSceneListener).getConfigInfo("pvr") && position == 3) ||
            (position == 4 && preferencesSceneWidget != null)
        ) {
            preferencesSceneWidget!!.setFocusToPrefSub()

            return
        }
    }


    private fun createPreferencesWidget() {
        preferencesSceneWidget = ReferenceWidgetPreferences(
            context,
            object : PreferencesWidgetListener {

                override fun stopSpeech() {
                    (sceneListener as HomeSceneListener).stopSpeech()
                }

                override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                    (sceneListener as HomeSceneListener).showToast(text, duration)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    (sceneListener as HomeSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as InputPrefSceneListener).setSpeechText(text = text, importance = importance)
                }

                override fun getAvailablePreferencesCategories(
                    callback: IAsyncDataCallback<List<CategoryItem>>,
                    type: PrefType
                ) {
                    (sceneListener as InputPrefSceneListener).getAvailablePreferencesCategories(callback, type)
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun requestFocusOnTopMenu() {
                    worldHandler!!.triggerAction(id, SceneManager.Action.DESTROY)
                }

                override fun getAudioInformationData(type: PrefType) {
                    (sceneListener as InputPrefSceneListener).getAudioInformation()
                }

                override fun getFirstAudioLanguage(type: PrefType): LanguageCode? {
                    return null
                }

                override fun onFirstAudioLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                    (sceneListener as InputPrefSceneListener).onAudioFirstLanguageClicked(languageCode)
                }

                override fun getSecondAudioLanguage(type: PrefType): LanguageCode? {
                    return null
                }

                override fun onSecondAudioLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                }


                override fun onAudioTypeClicked(position: Int) {
                    (sceneListener as InputPrefSceneListener).onAudioTypeClicked(position)
                }

                override fun onFaderValueChanged(newValue: Int) {
                    (sceneListener as InputPrefSceneListener).onFaderValueChanged(newValue)
                }

                override fun onAudioViValueChanged(newValue: Int) {
                    (sceneListener as InputPrefSceneListener).onAudioViValueChanged(newValue)
                }

                override fun onVisuallyImpairedValueChanged(position: Int, enabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).onVisuallyImpairedValueChanged(position,enabled)
                }

                override fun onVolumeChanged(newVolumeValue: Int) {
                    (sceneListener as InputPrefSceneListener).onVolumeChanged(newVolumeValue)
                }

                override fun getSubtitleInformationData(type: PrefType) {
                    (sceneListener as InputPrefSceneListener).getSubtitleInformation()
                }

                override fun getFirstSubtitleLanguage(type: PrefType): LanguageCode? {
                    return null
                }

                override fun onFirstSubtitleLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                    (sceneListener as InputPrefSceneListener).onSubtitleFirstLanguageClicked(
                        languageCode
                    )
                }

                override fun getSecondSubtitleLanguage(type: PrefType): LanguageCode? {
                    return null
                }

                override fun onSecondSubtitleLanguageSelected(languageCode: LanguageCode, type: PrefType) {
                    (sceneListener as InputPrefSceneListener).onSubtitleSecondLanguageClicked(
                        languageCode
                    )
                }


                override fun onSubtitlesEnabledClicked(isEnabled: Boolean, type: PrefType) {
                    (sceneListener as InputPrefSceneListener).onSubtitlesEnabledClicked(
                        isEnabled
                    )
                }

                override fun onSubtitlesTypeClicked(position: Int) {
                    (sceneListener as InputPrefSceneListener).onSubtitlesTypeClicked(
                        position
                    )
                }

                override fun onSubtitlesAnalogTypeClicked(position: Int) {
                }

                override fun onClosedCaptionChanged(isCaptionEnabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).onClosedCaptionChanged(
                        isCaptionEnabled
                    )
                }

                override fun getAdsTargetingData(type: PrefType) {
                    //Ignore
                }

                override fun getTermsOfServiceData(type: PrefType) {
                    //Ignore
                }

                override fun getSystemInformationData() {
                    (sceneListener as InputPrefSceneListener).getSystemInformation()
                }

                override fun getPreferencesSetupData() {
                }

                override fun onChannelsScanClicked() {
                    (sceneListener as InputPrefSceneListener).onChannelsScanClicked()
                }

                override fun onChannelsEditClicked() {
                    (sceneListener as InputPrefSceneListener).onChannelsEditClicked()
                }

                override fun onChannelsSettingsClicked() {
                }

                override fun onParentalControlClicked() {
                    (sceneListener as InputPrefSceneListener).onParentalControlClicked()
                }

                override fun getPreferencesTxtData() {
                    (sceneListener as InputPrefSceneListener).getPreferencesTxtData()
                }

                override fun getPreferencesCamData() {
                    (sceneListener as InputPrefSceneListener).getPreferencesCamData()
                }

                override fun onDefaultChannelSelected(tvChannel: TvChannel) {
                    (sceneListener as InputPrefSceneListener).onPreferencesDefaultChannelSelected(
                        tvChannel
                    )
                }

                override fun onDisplayModeSelected(selectedMode: Int) {
                    (sceneListener as InputPrefSceneListener).onDisplayModeSelected(selectedMode)
                }

                override fun getCamInfoModuleInfoData() {
                    (sceneListener as InputPrefSceneListener).getPreferencesCamInfoModuleInformation()
                }

                override fun onCamInfoSoftwareDownloadPressed() {
                    (sceneListener as InputPrefSceneListener).onPreferencesSoftwareDownloadPressed()
                }

                override fun onCamInfoSubscriptionStatusPressed() {
                    (sceneListener as InputPrefSceneListener).onPreferencesSubscriptionStatusPressed()
                }

                override fun onCamInfoEventStatusPressed() {
                    (sceneListener as InputPrefSceneListener).onPreferencesEventStatusPressed()
                }

                override fun onCamInfoTokenStatusPressed() {
                    (sceneListener as InputPrefSceneListener).onPreferencesTokenStatusPressed()
                }

                override fun onCamInfoChangeCaPinPressed() {
                    (sceneListener as InputPrefSceneListener).onPreferencesChangeCaPinPressed()
                }

                override fun getCamInfoMaturityRating(): String {
                    return (sceneListener as InputPrefSceneListener).getPreferencesCamInfoMaturityRating()
                }

                override fun onCamInfoConaxCaMessagesPressed() {
                    (sceneListener as InputPrefSceneListener).onPreferencesConaxCaMessagesPressed()
                }

                override fun onCamInfoAboutConaxCaPressed() {
                    (sceneListener as InputPrefSceneListener).onPreferencesAboutConaxCaPressed()
                }

                override fun getCamInfoSettingsLanguages() {
                    (sceneListener as InputPrefSceneListener).getPreferencesCamInfoSettingsLanguages()
                }

                override fun onCamInfoSettingsLanguageSelected(position: Int) {
                    (sceneListener as InputPrefSceneListener).onPreferencesCamInfoSettingsLanguageSelected(
                        position
                    )
                }

                override fun onCamInfoPopUpMessagesActivated(activated: Boolean) {
                    (sceneListener as InputPrefSceneListener).onPreferencesCamInfoPopUpMessagesActivated(
                        activated
                    )
                }

                override fun isCamInfoPopUpMessagesActivated(): Boolean {
                    return (sceneListener as InputPrefSceneListener).isPreferencesCamInfoPopUpMessagesActivated()
                }

                override fun getHbbTvInformation() {
                    (sceneListener as InputPrefSceneListener).getHbbTvInformation()
                }

                override fun onHbbSupportSwitch(isEnabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).onHbbSupportSwitch(isEnabled)
                }

                override fun onHbbTrackSwitch(isEnabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).onHbbTrackSwitch(isEnabled)
                }

                override fun onHbbPersistentStorageSwitch(isEnabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).onHbbPersistentStorageSwitch(isEnabled)
                }

                override fun onHbbBlockTracking(isEnabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).onHbbBlockTracking(isEnabled)
                }

                override fun onHbbTvDeviceId(isEnabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).onHbbTvDeviceId(isEnabled)
                }

                override fun onHbbCookieSettingsSelected(position: Int) {
                    (sceneListener as InputPrefSceneListener).onHbbCookieSettingsSelected(position)
                }

                override fun onHbbTvResetDeviceId() {
                    (sceneListener as InputPrefSceneListener).onHbbTvResetDeviceId()
                }

                override fun onEvaluationLicenseClicked() {
                    (sceneListener as InputPrefSceneListener).onEvaluationLicenseClicked()
                }

                override fun onTTXDigitalLanguageSelected(position: Int, language: String) {
                    (sceneListener as InputPrefSceneListener).onTTXDigitalLanguageClicked(position, language)
                }

                override fun onTTXDecodeLanguageSelected(position: Int) {
                    (sceneListener as InputPrefSceneListener).onTTXDecodeLanguageClicked(position)
                }

                override fun onAspectRatioClicked(position: Int) {
                    (sceneListener as InputPrefSceneListener).onAspectRatioClicked(position)
                }

                override fun setInteractionChannel(isSelected: Boolean) {
                    (sceneListener as InputPrefSceneListener).setInteractionChannel(isSelected)
                }

                override fun onEpgLanguageSelected(language: String) {
                    (sceneListener as InputPrefSceneListener).onEpgLanguageChanged(language)
                }

                override fun getParentalInformationData(type: PrefType) {
                    (sceneListener as InputPrefSceneListener).getParentalInformation(type)
                }

                override fun getClosedCationsInformationData() {
                    (sceneListener as InputPrefSceneListener).getClosedCationsInformation()
                }

                override fun getPreferencesPvrTimeshiftData() {
                    (sceneListener as InputPrefSceneListener).getPreferencesPvrTimeshiftData()
                }

                override fun getPrefsValue(key: String, value: Any?): Any? {
                    return (sceneListener as InputPrefSceneListener).getPrefsValue(key, value)
                }

                override fun getPrefsValue(tag: String, defValue: String): String {
                    return ""
                }

                override fun setPrefsValue(key: String, defValue: Any?) {
                    (sceneListener as InputPrefSceneListener).setPrefsValue(key, defValue)
                }


                override fun lockChannel(tvChannel: TvChannel, selected: Boolean) {
                    (sceneListener as InputPrefSceneListener).lockChannel(tvChannel, selected)
                }

                override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
                    return (sceneListener as InputPrefSceneListener).isChannelLockAvailable(tvChannel)
                }

                override fun blockInput(inputSourceData: InputSourceData, blocked: Boolean) {
                    (sceneListener as InputPrefSceneListener).blockInput(inputSourceData, blocked)
                }

                override fun isParentalControlsEnabled(): Boolean {
                    return (sceneListener as InputPrefSceneListener).isParentalControlsEnabled()
                }

                override fun setParentalControlsEnabled(enabled: Boolean) {
                    return (sceneListener as InputPrefSceneListener).setParentalControlsEnabled(
                        enabled
                    )
                }

                override fun setContentRatingSystemEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    enabled: Boolean
                ) {
                    (sceneListener as InputPrefSceneListener).setContentRatingSystemEnabled(
                        contentRatingSystem,
                        enabled
                    )
                }

                override fun setContentRatingLevel(index: Int) {
                    (sceneListener as InputPrefSceneListener).setContentRatingLevel(index)
                }

                override fun setRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    data: Boolean
                ): Boolean {
                    return (sceneListener as InputPrefSceneListener).setRatingBlocked(
                        contentRatingSystem,
                        it,
                        data
                    )
                }

                override fun setRelativeRatingsEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    data: Boolean
                ) {
                    (sceneListener as InputPrefSceneListener).setRelativeRatingsEnabled(
                        contentRatingSystem,
                        it,
                        data
                    )
                }

                override fun isRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating
                ): Boolean {
                    return (sceneListener as InputPrefSceneListener).isRatingBlocked(
                        contentRatingSystem,
                        it
                    )
                }

                override fun isSubRatingEnabled(
                    contentRatingSystem: ContentRatingSystem,
                    it: ContentRatingSystem.Rating,
                    subRating: ContentRatingSystem.SubRating
                ): Boolean {
                    return (sceneListener as InputPrefSceneListener).isSubRatingEnabled(
                        contentRatingSystem,
                        it,
                        subRating
                    )
                }

                override fun getContentRatingLevelIndex(): Int {
                    return (sceneListener as InputPrefSceneListener).getContentRatingLevelIndex()
                }

                override fun setSubRatingBlocked(
                    contentRatingSystem: ContentRatingSystem,
                    rating: ContentRatingSystem.Rating,
                    subRating: ContentRatingSystem.SubRating,
                    data: Boolean
                ) {
                    return (sceneListener as InputPrefSceneListener).setSubRatingBlocked(
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
                    return (sceneListener as InputPrefSceneListener).setRelativeRating2SubRatingEnabled(
                        contentRatingSystem,
                        data,
                        rating,
                        subRating
                    )
                }

                override fun setBlockUnrated(blocked: Boolean) {
                    (sceneListener as InputPrefSceneListener).setBlockUnrated(blocked)
                }

                override fun getRRT5DimInfo(index: Int): MutableList<String> {
                    return (sceneListener as InputPrefSceneListener).getRRT5DimInfo(index)
                }

                override fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem> {
                    return (sceneListener as InputPrefSceneListener).getRRT5CrsInfo(regionName)
                }

                override fun getRRT5LevelInfo(
                    countryIndex: Int,
                    dimIndex: Int
                ): MutableList<String> {
                    return (sceneListener as InputPrefSceneListener).getRRT5LevelInfo(
                        countryIndex,
                        dimIndex
                    )
                }

                override fun getSelectedItemsForRRT5Level(): HashMap<Int, Int> {
                    return (sceneListener as InputPrefSceneListener).getSelectedItemsForRRT5Level()
                }

                override fun rrt5BlockedList(
                    regionPosition: Int,
                    position: Int
                ): HashMap<Int, Int> {
                    return (sceneListener as InputPrefSceneListener).rrt5BlockedList(
                        regionPosition,
                        position
                    )
                }

                override fun setSelectedItemsForRRT5Level(
                    regionIndex: Int,
                    dimIndex: Int,
                    levelIndex: Int
                ) {
                    (sceneListener as InputPrefSceneListener).setSelectedItemsForRRT5Level(
                        regionIndex,
                        dimIndex,
                        levelIndex
                    )
                }

                override fun resetRRT5() {
                    (sceneListener as InputPrefSceneListener).resetRRT5()
                }

                override fun getRRT5Regions(): MutableList<String> {
                    return (sceneListener as InputPrefSceneListener).getRRT5Regions()
                }

                override fun onSkipChannelSelected(
                    tvChannel: TvChannel,
                    status: Boolean
                ) {
                    (sceneListener as InputPrefSceneListener).onSkipChannelSelected(
                        tvChannel,
                        status
                    )
                }

                override fun onSwapChannelSelected(
                    firstChannel: TvChannel,
                    secondChannel: TvChannel,
                    previousPosition: Int,
                    newPosition: Int
                ) {
                    (sceneListener as InputPrefSceneListener).onSwapChannelSelected(
                        firstChannel,
                        secondChannel,
                        previousPosition,
                        newPosition
                    )
                }

                override fun onMoveChannelSelected(
                    moveChannelList: java.util.ArrayList<TvChannel>,
                    previousPosition: Int,
                    newPosition: Int,
                    channelMap: HashMap<Int, String>
                ) {
                    (sceneListener as InputPrefSceneListener).onMoveChannelSelected(
                        moveChannelList,
                        previousPosition,
                        newPosition,
                        channelMap
                    )
                }

                override fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int) {
                    (sceneListener as InputPrefSceneListener).onDeleteChannelSelected(
                        tvChannel,
                        index
                    )
                }

                override fun onClearChannelSelected() {
                    (sceneListener as InputPrefSceneListener).onClearChannelSelected()
                }

                override fun saveUserSelectedCCOptions(
                    ccOptions: String,
                    newValue: Int
                ) {
                    (sceneListener as InputPrefSceneListener).saveUserSelectedCCOptions(
                        ccOptions,
                        newValue
                    )
                }

                override fun resetCC() {
                    (sceneListener as InputPrefSceneListener).resetCC()
                }

                override fun setCCInfo() {
                    (sceneListener as InputPrefSceneListener).setCCInfo()
                }

                override fun disableCCInfo() {
                    (sceneListener as InputPrefSceneListener).disableCCInfo()
                }

                override fun setCCWithMute(isEnabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).setCCWithMute(isEnabled)
                }

                override fun lastSelectedTextOpacity(position: Int) {
                    (sceneListener as InputPrefSceneListener).lastSelectedTextOpacity(position)
                }

                override fun onChannelClicked() {
                }

                override fun onBlueMuteChanged(isEnabled: Boolean) {
                }

                override fun noSignalPowerOffChanged(enabled: Boolean) {
                }

                override fun noSignalPowerOffTimeChanged() {
                }

                override fun getUtilsModule(): UtilsInterface {
                    return (sceneListener as InputPrefSceneListener).getUtilsModule()
                }

                override fun isLcnEnabled(): Boolean {
                    return (sceneListener as InputPrefSceneListener).isLcnEnabled()
                }

                override fun enableLcn(enable: Boolean) {
                }

                override fun getCiPlusInterface(): CiPlusInterface {
                    return (sceneListener as InputPrefSceneListener).getCiPlusInterface()
                }

                override fun isAccessibilityEnabled(): Boolean {
                    return (sceneListener as InputPrefSceneListener).isAccessibilityEnabled()
                }

                override fun changeChannelFromTalkback(tvChannel: TvChannel) {
                    return (sceneListener as InputPrefSceneListener).changeChannelFromTalkback(tvChannel)
                }

                override fun onDpadDown(btnView: ImageView?) {
                }

                override fun getAnokiRatingLevel(): Int {
                    return (sceneListener as InputPrefSceneListener).getAnokiRatingLevel()
                }

                override fun checkNetworkConnection(): Boolean {
                    return (sceneListener as InputPrefSceneListener).checkNetworkConnection()
                }

                override fun setAnokiRatingLevel(level: Int) {
                    (sceneListener as InputPrefSceneListener).setAnokiRatingLevel(level)
                }

                override fun storePrefsValue(tag: String, defValue: String) {
                }

                override fun onPictureSettingsClicked() {
                }

                override fun onScreenSettingsClicked() {
                }

                override fun onSoundSettingsClicked() {
                }

                override fun getSignalAvailable(): Boolean {
                    return (sceneListener as InputPrefSceneListener).getSignalAvailable()
                }

                override fun getChannelSourceType(tvChannel: TvChannel) : String {
                    return (sceneListener as InputPrefSceneListener).getChannelSourceType(tvChannel)
                }

                override fun setPowerOffTime(value: Int, time: String) {

                }

                override fun setAudioDescriptionEnabled(enable: Boolean) {
                    (sceneListener as InputPrefSceneListener).setAudioDescriptionEnabled(enable)
                }

                override fun setHearingImpairedEnabled(enable: Boolean) {
                    (sceneListener as InputPrefSceneListener).setHearingImpairedEnabled(enable)
                }

                override fun onPowerClicked() {
                }

                override fun isAnalogSubtitleEnabled(type: PrefType): Boolean {
                    return false
                }

                override fun isDigitalSubtitleEnabled(type: PrefType): Boolean {
                    return false
                }

                override fun onAdsTargetingChange(enable: Boolean) {
                }

                override fun dispatchKey(keyCode: Int, event: Any): Boolean {
                    return false
                }

                override fun isAnokiParentalControlsEnabled(): Boolean {
                    return (sceneListener as InputPrefSceneListener).isAnokiParentalControlsEnabled()
                }

                override fun setAnokiParentalControlsEnabled(enabled: Boolean) {
                    (sceneListener as InputPrefSceneListener).setAnokiParentalControlsEnabled(enabled)
                }

                override fun onExit() {
                }

                override fun getPreferenceDeepLink(): DeepLink? {
                    return null
                }

                override fun startOadScan() {

                }

                override fun startCamScan() {

                }

                override fun changeCamPin() {

                }

                override fun deleteProfile(profileName: String) {

                }

                override fun onClickTermsOfService() {
                    //Ignore
                }

                override fun onClickPrivacyPolicy() {
                    //Ignore
                }

                override fun doCAMReconfiguration(camTypePreference: CamTypePreference) {
                    return (sceneListener as InputPrefSceneListener).doCAMReconfiguration(camTypePreference)
                }

                override fun selectMenuItem(position: Int) {

                }

                override fun enterMMI() {

                }

                override fun onTimeshiftStatusChanged(enabled: Boolean) {
                }

                override fun getConfigInfo(nameOfInfo: String): Boolean {
                    return (sceneListener as InputPrefSceneListener).getConfigInfo(nameOfInfo)
                }

                override fun getLockedChannelIdsFromPrefs(): MutableSet<String> {
                    return (sceneListener as HomeSceneListener).getLockedChannelIdsFromPrefs()
                }
            })
        preferencesSceneWidget!!.setCategoriesMargin(Utils.getDimensInPixelSize(R.dimen.custom_dim_95_5))
    }


    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        super.refresh(data)
        if (data is Long) {
            val simpleDateFormat =
                SimpleDateFormat("HH:mm", Locale("en"))

            timeTv?.time = simpleDateFormat.format(Date(data.toLong()))
            date = Date(data.toLong())
        }
        if (data is Boolean) {
            preferencesSceneWidget!!.refresh(data)
        }
    }

}