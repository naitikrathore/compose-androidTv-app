package com.iwedia.cltv.scene.home_scene

import android.media.tv.ContentRatingSystem
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.ReferenceWidgetPreferences
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.TTSStopperInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import listeners.AsyncDataReceiver
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType
import world.SceneListener

/**
 * Home scene listener
 *
 * @author Dejan Nadj
 */
interface HomeSceneListener : SceneListener, TTSSetterInterface, TTSStopperInterface,
    ToastInterface, TTSSetterForSelectableViewInterface {

    fun kidsModeEnabled(): Boolean

    /**
     * Get available channel filters
     *
     * @param callback  async data receiver
     */
    fun getAvailableChannelFilters(callback: IAsyncDataCallback<ArrayList<Category>>)

    /**
     * Get list of preference types
     */
    fun getAvailablePreferenceTypes(callback: IAsyncDataCallback<List<CategoryItem>>)

    /**
     * Get list of preference categories based on type
     */
    fun getAvailablePreferencesCategories(callback: IAsyncDataCallback<List<CategoryItem>>, type: PrefType)

    /**
     * Get available lists of favourite channels
     */
    fun getAvailableFavouriteCategories(callback: IAsyncDataCallback<ArrayList<String>>)

    /**
     * Get available favourite channels
     */
    fun getAvailableFavouriteChannels(callback: IAsyncDataCallback<ArrayList<TvChannel>>)

    /**
     * On favourite channel clicked (handle is in favorites)
     */
    fun onFavChannelClicked(tvChannel: TvChannel, favListIds: ArrayList<String>)


    fun changeFilterToAll(categoryName: String)

    /**
     * Get guide event for the tv channel
     *
     * @param additionalDayCount day offset
     * @param tvChannel tv channel
     * @param callback async data receiver
     */
    fun getGuideEventsForTvChannel(
        additionalDayCount: Int,
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<MutableList<TvEvent>>
    )

    /**
     * Start tv channel playback
     *
     * @param tv channel
     */
    fun playTvChannel(tvChannel: TvChannel)

    /**
     * Show details scene
     *
     * @param tvEvent tv event
     */
    fun showDetails(tvEvent: Any)

    /**
     * On recording item click
     *
     * @param recording
     */
    fun onRecordingClick(recording: Any)

    /**
     * On guide details favorite button pressed
     *
     * @param tvChannel tv channel
     * @param favListIds favorite list ids
     */
    fun onGuideFavoriteButtonPressed(tvChannel: TvChannel, favListIds: ArrayList<String>)

    /**
     * Get recordings categories
     *
     * @param sceneConfig
     */
    fun getRecordingsCategories(
        sceneConfig: SceneConfig?,
        callback: AsyncDataReceiver<MutableList<RailItem>>
    )

    /**
     * On Rename Recording
     *
     * @param recording
     * @param name
     */
    fun onRenameRecording(recording: Recording, name: String, callback: IAsyncCallback)

    /**
     * On Delete Recording
     *
     * @param recording
     */
    fun onDeleteRecording(recording: Recording, callback: IAsyncCallback)

    /**
     * On Search button clicked
     */
    fun onSearchClicked()

    /**
     * On watchlist clicked
     */
    fun onGuideWatchlistClicked(tvEvent: TvEvent): Boolean

    /**
     * On channels scan clicked
     */
    fun onChannelsScanClicked()

    /**
     * On channel settings clicked
     */

    fun onChannelsSettingsClicked()

    /**
     * On channels edit clicked
     */
    fun onChannelsEditClicked()

    /**
     * On parental control clicked
     */
    fun onParentalControlClicked()

    /**
     * On digit button pressed
     * @param digit pressed digit
     */
    fun onDigitPressed(digit: Int, epgActiveFilter: FilterItemType? = null, filterMetadata: String? = null)

    /**
     * On add favourite list clicked
     */
    fun onAddFavouriteListClicked()

    /**
     * On rename fav list clicked
     */
    fun onRenameFavouriteListClicked(currentName: String)

    /**
     * On delete fav list clicked
     */
    fun onDeleteFavouriteListClicked(categoryName: String)

    /**
     * Get ads targeting information
     */
    fun getAdsTargetingData(type: PrefType)

    /**
     * Get system information
     */
    fun getSystemInformation()

    /**
     * Get preferences setup
     */
    fun getPreferencesSetup()

    /**
     * Get preferences txt data
     */
    fun getPreferencesTxtData()
    fun getPreferencesCamData()

    /**
     * On TTX Digital Language Clicked
     */
    fun onTTXDigitalLanguageClicked(position: Int, language: String)

    /**
     * On TTX Decode Language Clicked
     */
    fun onTTXDecodeLanguageClicked(position: Int)


    /**
     * On default channel selected in preferences setup
     */
    fun onPreferencesDefaultChannelSelected(tvChannel: TvChannel)

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

    /**
     * Get audio information based on type
     *
     */
    fun getAudioInformation(type: PrefType)

    /**
     * Get first audio language
     */
    fun getAudioFirstLanguage(type: PrefType): LanguageCode?

    /**
     * On first audio language clicked
     */
    fun onAudioFirstLanguageClicked(languageCode: LanguageCode, type: PrefType)

    /**
     * Get second audio language
     * */
    fun getAudioSecondLanguage(type: PrefType): LanguageCode?

    /**
     * On second audio language clicked
     * */
    fun onAudioSecondLanguageClicked(languageCode: LanguageCode, type: PrefType)

    /**
     * On audio type option clicked
     */
    fun onAudioTypeClicked(position: Int)

    /**
     * Get subtitle information based on preference type
     *
     */
    fun getSubtitleInformation(type: PrefType)

    /**
     * Get first subtitle language
     */
    fun getSubtitleFirstLanguage(type: PrefType): LanguageCode?

    /**
     * On first subtitle language clicked
     */
    fun onSubtitleFirstLanguageClicked(languageCode: LanguageCode, type: PrefType)

    /**
     * Get second subtitle language
     */
    fun getSubtitleSecondLanguage(type: PrefType): LanguageCode?

    /**
     * On second subtitle language clicked
     * */
    fun onSubtitleSecondLanguageClicked(languageCode: LanguageCode, type: PrefType)

    /**
     * Get Subtitle State
     * */
    fun getSubtitlesState(type: PrefType): Boolean

    /**
     * On subtitles enabled/disabled clicked
     * */
    fun onSubtitlesEnabledClicked(isEnabled: Boolean, type: PrefType)

    /**
     * On subtitle type clicked
     * */
    fun onSubtitlesTypeClicked(position: Int)

    /**
     * On subtitle analog type clicked
     * */
    fun onSubtitlesAnalogTypeClicked(position: Int)

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
     * On preferences change ca pin pressed
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
     * Channel up
     */
    fun channelUp()

    /**
     * Channel down
     */
    fun channelDown()

    /**
     * Record pressed
     */
    fun onRecordButtonPressed(tvEvent: TvEvent, callback: IAsyncCallback)

    /**
     * On preferences EvaluationLicense selected
     */
    fun onEvaluationLicenseClicked()

//    /**
//     * On preferences OpenSourceLicence selected
//     */
//    fun onOpenSourceLicenceClicked()

    /**
     * On selection of add record button
     */
    fun customRecButtonClciked()

    /**
     * On preferences AspectRatio selected
     */
    fun onAspectRatioClicked(position: Int)

    fun fetchEventList(
        tvChannel: TvChannel,
        dayOffset: Int,
        callback: IAsyncDataCallback<MutableList<TvEvent>>
    )

    /**
     * loads events for channels range with respect to the anchor channel provided as reference
     * ------------   -6 <--- anchor_channel ----> +6    ---------------
     */
    fun getEventsForChannels(
        anchorChannel: TvChannel,
        filterId: Int,
        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
        dayOffset: Int,
        additionalDayOffset: Int,
        isExtend: Boolean
    )

    /**
     * loads events for next channels range with respect to the anchor channel provided as reference
     * ------------   anchor channel -> +10    ---------------
     */
    fun loadNextEpgData(
        anchorChannel: TvChannel,
        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
        dayOffset: Int,
        additionalDayOffset: Int,
        isExtend: Boolean
    )

    /**
     * loads previous events for channels range with respect to the anchor channel provided as reference
     * ------------   -10 <---anchor channel   ---------------
     */
    fun loadPreviousChannels(
        anchorChannel: TvChannel,
        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
        dayOffset: Int,
        additionalDayOffset: Int,
        isExtend: Boolean
    )

    fun getChannelsOfSelectedFilter(): MutableList<TvChannel>

    fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>, applicationMode: ApplicationMode = ApplicationMode.DEFAULT)
    fun getRecordingInProgressTvChannel(): TvChannel?
    fun getForYouRails()
    fun isInWatchList(tvEvent: TvEvent): Boolean
    fun isInRecordingList(tvEvent: TvEvent): Boolean
    fun isInFavoriteList(tvChannel: TvChannel): Boolean
    fun getFavoriteCategories(callback: IAsyncDataCallback<ArrayList<String>>)
    fun getFavoriteSelectedItems(tvChannel: TvChannel): ArrayList<String>

    fun getRailSize() : Int
    //check if is channel is locked or not
    fun isChannelLocked(channelId:Int):Boolean

    /**
     * on Interaction Channel
     */
    fun setInteractionChannel(isSelected: Boolean)
    /**
     * on EPG Language changed
     */
    fun onEpgLanguageChanged(language: String)
    fun getParentalInformation(type: PrefType)
    fun getClosedCationsInformation()
    fun getPreferencesPvrTimeshiftData()
    fun getPrefsValue(key: String, value: Any?): Any?
    fun setPrefsValue(key: String, defValue: Any?)
    fun isRecordingInProgress(): Boolean
    fun isTimeShiftActive(): Boolean
    fun lockChannel(tvChannel: TvChannel, selected: Boolean)
    fun isChannelLockAvailable(tvChannel: TvChannel): Boolean
    fun blockInput(inputSourceData: InputSourceData, blocked: Boolean)
    fun isParentalControlsEnabled(): Boolean
    fun setParentalControlsEnabled(enabled : Boolean)
    fun setContentRatingSystemEnabled(contentRatingSystem: ContentRatingSystem, enabled: Boolean)
    fun setContentRatingLevel(index: Int)
    fun setRatingBlocked(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, data: Boolean): Boolean
    fun setRelativeRatingsEnabled(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, data: Boolean)
    fun isRatingBlocked(contentRatingSystem: ContentRatingSystem,it: ContentRatingSystem.Rating): Boolean
    fun isSubRatingEnabled(contentRatingSystem: ContentRatingSystem, it: ContentRatingSystem.Rating, subRating: ContentRatingSystem.SubRating): Boolean
    fun getContentRatingLevelIndex(): Int
    fun setSubRatingBlocked(contentRatingSystem: ContentRatingSystem,rating: ContentRatingSystem.Rating,subRating: ContentRatingSystem.SubRating,data: Boolean)
    fun setRelativeRating2SubRatingEnabled(contentRatingSystem: ContentRatingSystem,data: Boolean,rating: ContentRatingSystem.Rating,subRating: ContentRatingSystem.SubRating)
    fun setBlockUnrated(blocked: Boolean)
    fun getRRT5DimInfo(index: Int): MutableList<String>
    fun getRRT5CrsInfo(regionName: String): MutableList<ContentRatingSystem>
    fun getRRT5LevelInfo(countryIndex: Int, dimIndex: Int): MutableList<String>

    fun getSelectedItemsForRRT5Level(): HashMap<Int, Int>
    fun rrt5BlockedList(regionPosition :Int, position: Int): HashMap<Int, Int>
    fun setSelectedItemsForRRT5Level(regionIndex: Int, dimIndex: Int, levelIndex: Int)
    fun resetRRT5()
    fun onSkipChannelSelected(tvChannel: TvChannel, status: Boolean)
    fun onSwapChannelSelected(firstChannel: TvChannel, secondChannel: TvChannel, previousPosition:Int,newPosition:Int)
    fun onMoveChannelSelected(moveChannelList: java.util.ArrayList<TvChannel>, previousPosition:Int, newPosition:Int, channelMap: HashMap<Int, String>)
    fun onDeleteChannelSelected(tvChannel: TvChannel, index: Int)
    fun onClearChannelSelected()
    fun saveUserSelectedCCOptions(ccOptions: String,newValue: Int)
    fun resetCC()
    fun setCCInfo()
    fun disableCCInfo()
    fun setCCWithMute(isEnabled: Boolean)
    fun lastSelectedTextOpacity(position: Int)
    fun isCCTrackAvailable(): Boolean?
    fun onChannelClicked()
    fun onFaderValueChanged(newValue: Int)
    fun onAudioViValueChanged(newValue: Int)
    fun onVisuallyImpairedValueChanged(position: Int, enabled: Boolean)
    fun onVolumeChanged(newVolumeValue: Int)
    fun onBlueMuteChanged(isEnabled: Boolean)
    fun noSignalPowerOffChanged(isEnabled: Boolean)
    fun noSignalPowerOffTimeChanged()
    fun getUtilsModule(): UtilsInterface
    fun isLcnEnabled(): Boolean
    fun enableLcn(enable: Boolean)
    fun getCiPlusInterface(): CiPlusInterface
    fun storePrefsValue(tag: String, defValue: String)
    fun getPrefsValue(tag: String, defValue: String): String
    fun isClosedCaptionEnabled(): Boolean
    fun getIsAudioDescription(type: Int): Boolean
    fun getIsDolby(type: Int): Boolean
    fun isHOH(type:Int): Boolean
    fun isTeleText(type:Int): Boolean
    fun getAudioChannelInfo(type: Int): String
    fun getVideoResolution(): String

    fun areChannelsNonBrowsable(): Boolean
    fun onPictureSettingsClicked()
    fun onScreenSettingsClicked()
    fun onSoundSettingsClicked()
    fun getSignalAvailable() : Boolean
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    fun getCurrentTime(tvChannel: TvChannel): Long
    fun getRRT5Regions(): MutableList<String>
    fun getChannelSourceType(tvChannel: TvChannel): String
    fun setPowerOffTime(value: Int, time: String)
    fun setAudioDescriptionEnabled(enable: Boolean)
    fun setHearingImpairedEnabled(enable: Boolean)
    fun onPowerClicked()
    fun isCurrentEvent(tvEvent: TvEvent) : Boolean
    fun getAvailableAudioTracks(): List<IAudioTrack>
    fun getAvailableSubtitleTracks(): List<ISubtitle>
    fun getClosedCaption(): String?
    fun getCurrentAudioTrack(): IAudioTrack?
    fun isDigitalSubtitleEnabled(type: PrefType): Boolean
    fun isAnalogSubtitleEnabled(type: PrefType): Boolean
    fun isAccessibilityEnabled(): Boolean
    fun getPromotionContent(callback: IAsyncDataCallback<ArrayList<PromotionItem>>)
    fun onFastCardClicked(tvEvent: TvEvent)
    fun onAdsTargetingChange(enable: Boolean)
    fun promotionItemActionClicked(promotionItem: PromotionItem)
    fun getActiveEpgFilter(): Int
    fun setActiveEpgFilter(filterId: Int)
    fun getActiveCategory(): String
    fun hasInternet(): Boolean
    fun onSettingsOpened()
    fun isProfileButtonEnabled(): Boolean
    fun isAnokiServerReachable(): Boolean
    fun startScan()
    fun isChannelListEmpty() : Boolean
    fun getEventsByChannelList(
        callback: IAsyncDataCallback<java.util.LinkedHashMap<Int, MutableList<TvEvent>>>,
        channelList: MutableList<TvChannel>,
        dayOffset: Int,
        additionalDayOffset: Int,
        isExtend: Boolean
    )
    fun changeChannelFromTalkback(tvChannel: TvChannel)
    fun muteAudio()
    fun unMuteAudio()
    fun getDateTimeFormat() : DateTimeFormat
    fun isLockedScreenShown(): Boolean
    fun showExitDialog()
    fun setAnokiRatingLevel(level: Int)
    fun getAnokiRatingLevel(): Int
    fun isEventLocked(tvEvent: TvEvent?): Boolean
    fun checkNetworkConnection(): Boolean
    fun isAnokiParentalControlsEnabled(): Boolean
    fun setAnokiParentalControlsEnabled(enabled : Boolean)
    fun isRegionSupported(): Boolean
    fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink?

    fun startCamScan()
    fun changeCamPin()
    fun selectMenuItem(position: Int)
    fun enterMMI()
    fun deleteProfile(profileName: String)
    fun onTimeshiftStatusChanged(enabled: Boolean)
    fun startOadScan()
    fun getConfigInfo(nameOfInfo: String): Boolean
    fun stopPlayer()
    fun resumePlayer()
    fun getCurrentSubtitleTrack() : ISubtitle?
    fun isSubtitleEnabled() : Boolean
    fun getTermsOfServiceData(type: PrefType)
    fun onClickTermsOfService()
    fun onClickPrivacyPolicy()
    fun tuneToFocusedChannel(tvChannel: TvChannel)
    fun onVodItemClicked(type: VODType, contentId: String)
    fun changeTrailer(vodItem: VODItem?)
    fun setActiveWindow(tvChannelList: MutableList<TvChannel>, startTime: Long)
    fun clearActiveWindow()
    fun getStartTimeForActiveWindow(): Long
    fun doCAMReconfiguration(camTypePreference: CamTypePreference)
    fun isFastChannelListEmpty(): Boolean
    fun getAudioFormatInfo(): String
    fun isTosAccepted(): Boolean
    fun getLockedChannelIdsFromPrefs(): MutableSet<String>
    fun backToLive()
    fun checkInternet(): Boolean
    fun isScrambled():Boolean
}
