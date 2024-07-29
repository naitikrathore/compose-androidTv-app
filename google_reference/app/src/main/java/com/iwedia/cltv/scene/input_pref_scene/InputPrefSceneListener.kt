package com.iwedia.cltv.scene.input_pref_scene

import android.media.tv.ContentRatingSystem
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.parental.InputSourceData
import world.SceneListener

interface InputPrefSceneListener : SceneListener, TTSSetterInterface, ToastInterface {
    /**
     * Get list of preference types
     */
    fun getAvailablePreferenceTypes(callback: IAsyncDataCallback<List<CategoryItem>>)

    /**
     * Get list of preference categories based on type
     */
    fun getAvailablePreferencesCategories(callback: IAsyncDataCallback<List<CategoryItem>>, type: PrefType)

    /* Get audio information
     *
     */
    fun getAudioInformation()

    /**
     * On first audio language clicked
     */
    fun onAudioFirstLanguageClicked(languageCode: LanguageCode)

    /**
     * On second audio language clicked
     * */
    fun onAudioSecondLanguageClicked(languageCode: LanguageCode)

    /**
     * On audio type option clicked
     */
    fun onAudioTypeClicked(position: Int)

    /**
     * Get subtitle information
     *
     */
    fun getSubtitleInformation()

    /**
     * Get system information
     */
    fun getSystemInformation()

    /**
     * On first subtitle language clicked
     */
    fun onSubtitleFirstLanguageClicked(languageCode: LanguageCode)

    /**
     * On second subtitle language clicked
     * */
    fun onSubtitleSecondLanguageClicked(languageCode: LanguageCode)

    /**
     * On subtitles enabled/disabled clicked
     * */
    fun onSubtitlesEnabledClicked(isEnabled: Boolean)

    /**
     * On subtitle type clicked
     * */
    fun onSubtitlesTypeClicked(position: Int)


    /**
     * On channels scan clicked
     */
    fun onChannelsScanClicked()


    /**
     * On channels edit clicked
     */
    fun onChannelsEditClicked()

    /* Get preferences txt data
    */
    fun getPreferencesTxtData()

    /**
     * On TTX Digital Language Clicked
     */
    fun onTTXDigitalLanguageClicked(position: Int, language: String)

    /**
     * On TTX Decode Language Clicked
     */
    fun onTTXDecodeLanguageClicked(position: Int)

    /**
     * on display mode selected in preference setup
     */
    fun onDisplayModeSelected(selectedMode: Int)

    /**
     * Get preferences cam info module information
     */
    fun getPreferencesCamInfoModuleInformation()

    /**
     * On preferences software download pressed
     */
    fun onPreferencesSoftwareDownloadPressed()

    /* On preferences change ca pin pressed
    */
    fun onPreferencesChangeCaPinPressed()

    /**
     * On preferences conax ca messages pressed
     */
    fun onPreferencesConaxCaMessagesPressed()

    /**
     * On preferences about conax ca pressed
     */
    fun onPreferencesAboutConaxCaPressed()

    /**
     * Get preferences cam info maturity rating
     */
    fun getPreferencesCamInfoMaturityRating(): String

    /**
     * Get preferences cam info settings languages
     */
    fun getPreferencesCamInfoSettingsLanguages()

    /**
     * On preferences cam info settings language selected
     *
     * @param position language position in list
     */
    fun onPreferencesCamInfoSettingsLanguageSelected(position: Int)

    /**
     * On preferences cam info pop up messages activated
     *
     * @param activated is pop up messages activated
     */
    fun onPreferencesCamInfoPopUpMessagesActivated(activated: Boolean)

    /**
     * Is preferences cam info pop up messages activated
     *
     * @return is pop up messages activated
     */
    fun isPreferencesCamInfoPopUpMessagesActivated(): Boolean

    /**
     * Get hbb tv preferences information
     */
    fun getHbbTvInformation()
    fun getPreferencesCamData()

    fun onHbbSupportSwitch(isEnabled: Boolean)
    fun onHbbTrackSwitch(isEnabled: Boolean)
    fun onHbbPersistentStorageSwitch(isEnabled: Boolean)
    fun onHbbBlockTracking(isEnabled: Boolean)
    fun onHbbTvDeviceId(isEnabled: Boolean)
    fun onHbbTvResetDeviceId()

    /**
     * On hbb cookie settings selected
     */
    fun onHbbCookieSettingsSelected(position: Int)

    /**
     * On preferences AspectRatio selected
     */
    fun onAspectRatioClicked(position: Int)

    /**
     * On parental control clicked
     */
    fun onParentalControlClicked()


    /**
     * On default channel selected in preferences setup
     */
    fun onPreferencesDefaultChannelSelected(tvChannel: TvChannel)


    /**
     * On closed caption changed
     * */
    fun onClosedCaptionChanged(isCaptionEnabled: Boolean)

    /**
     * On preferences subscription status pressed
     */
    fun onPreferencesSubscriptionStatusPressed()

    /**
     * On preferences event status pressed
     */
    fun onPreferencesEventStatusPressed()

    /**
     * On preferences token status pressed
     */
    fun onPreferencesTokenStatusPressed()


    /**
     * On preferences EvaluationLicense selected
     */
    fun onEvaluationLicenseClicked()

    /**
     * On preferences OpenSourceLicence selected
     */
    fun onOpenSourceLicenceClicked()


    fun getChannelsOfSelectedFilter(): MutableList<TvChannel>

    /**
     * on EPG Language changed
     */
    fun onEpgLanguageChanged(language: String)

    fun setInteractionChannel(isSelected: Boolean)

    fun doCAMReconfiguration(camTypePreference: CamTypePreference)

    fun getParentalInformation(type: PrefType)
    fun getClosedCationsInformation()
    fun getPreferencesPvrTimeshiftData()
    fun getPrefsValue(key: String, value: Any?): Any?
    fun setPrefsValue(key: String, defValue: Any?)
    fun lockChannel(tvChannel: TvChannel, selected: Boolean)
    fun isChannelLockAvailable(tvChannel: TvChannel): Boolean
    fun blockInput(inputSourceData: InputSourceData, blocked: Boolean)
    fun isParentalControlsEnabled(): Boolean
    fun setParentalControlsEnabled(enabled: Boolean)
    fun setContentRatingSystemEnabled(contentRatingSystem: ContentRatingSystem, enabled: Boolean)
    fun setContentRatingLevel(index: Int)
    fun setRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating,
        data: Boolean
    ): Boolean

    fun setRelativeRatingsEnabled(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating,
        data: Boolean
    )

    fun isRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating
    ): Boolean

    fun isSubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ): Boolean

    fun getContentRatingLevelIndex(): Int
    fun setSubRatingBlocked(
        contentRatingSystem: ContentRatingSystem,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating,
        data: Boolean
    )

    fun setRelativeRating2SubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        data: Boolean,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    )

    fun setBlockUnrated(blocked: Boolean)
    fun getRRT5DimInfo(index: Int): MutableList<String>
    fun getRRT5LevelInfo(countryIndex: Int, dimIndex: Int): MutableList<String>

    fun getSelectedItemsForRRT5Level(): HashMap<Int, Int>
    fun rrt5BlockedList(regionPosition: Int, position: Int): HashMap<Int, Int>
    fun setSelectedItemsForRRT5Level(regionIndex: Int, dimIndex: Int, levelIndex: Int)
    fun resetRRT5()
    fun getRRT5Regions(): MutableList<String>

    fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem>

    fun onSkipChannelSelected(tvChannel: TvChannel, status: Boolean)
    fun onSwapChannelSelected(
        firstChannel: TvChannel,
        secondChannel: TvChannel,
        previousPosition: Int,
        newPosition: Int
    )

    fun onMoveChannelSelected(moveChannelList: java.util.ArrayList<TvChannel>, previousPosition: Int, newPosition: Int, channelMap: HashMap<Int, String>)
    fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int)
    fun onClearChannelSelected()
    fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int)
    fun resetCC()
    fun setCCInfo()
    fun disableCCInfo()
    fun setCCWithMute(isEnabled: Boolean)
    fun lastSelectedTextOpacity(position: Int)
    fun getUtilsModule(): UtilsInterface
    fun isLcnEnabled(): Boolean
    fun getCiPlusInterface(): CiPlusInterface
    fun getSignalAvailable() : Boolean
    fun getChannelSourceType(tvChannel: TvChannel): String
    fun onFaderValueChanged(newValue: Int)
    fun onAudioViValueChanged(newValue: Int)
    fun onVisuallyImpairedValueChanged(position: Int, enabled: Boolean)
    fun onVolumeChanged(newVolumeValue: Int)
    fun setAudioDescriptionEnabled(enable: Boolean)
    fun setHearingImpairedEnabled(enable: Boolean)
    fun isAccessibilityEnabled(): Boolean
    fun changeChannelFromTalkback(tvChannel: TvChannel)
    fun setAnokiRatingLevel(level: Int)
    fun getAnokiRatingLevel(): Int
    fun checkNetworkConnection(): Boolean
    fun isAnokiParentalControlsEnabled(): Boolean
    fun setAnokiParentalControlsEnabled(enabled : Boolean)
    fun getConfigInfo(nameOfInfo: String): Boolean
}