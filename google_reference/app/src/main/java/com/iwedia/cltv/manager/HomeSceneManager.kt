package com.iwedia.cltv.manager

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.tv.ContentRatingSystem
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.CaptioningManager
import androidx.annotation.RequiresApi
import com.google.android.exoplayer2.ExoPlayer
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.anoki_fast.vod.details.DetailsSceneData
import com.iwedia.cltv.anoki_fast.vod.player.VodPlayerHelper
import com.iwedia.cltv.anoki_fast.vod.player.VodTrailerHelper
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.Pref
import com.iwedia.cltv.components.PreferenceSubMenuItem
import com.iwedia.cltv.components.ReferenceWidgetPreferences
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.config.entities.ConfigRailParam
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.entities.FilterItem
import com.iwedia.cltv.entities.PreferenceAudioInformation
import com.iwedia.cltv.entities.PreferenceSetupInformation
import com.iwedia.cltv.entities.PreferenceSubtitleInformation
import com.iwedia.cltv.entities.PreferencesAdsTargetingInformation
import com.iwedia.cltv.entities.PreferencesCamInfoInformation
import com.iwedia.cltv.entities.PreferencesClosedCaptionsInformation
import com.iwedia.cltv.entities.PreferencesHbbTVInfromation
import com.iwedia.cltv.entities.PreferencesParentalControlInformation
import com.iwedia.cltv.entities.PreferencesPvrTimeshiftInformation
import com.iwedia.cltv.entities.PreferencesTeletextInformation
import com.iwedia.cltv.entities.PreferencesTermsOfServiceInformation
import com.iwedia.cltv.entities.ReferenceDeviceItem
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.ClosedCaptionInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.FastUserSettingsInterface
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.HbbTvInterface
import com.iwedia.cltv.platform.`interface`.HbbTvInterface.HbbTvCookieSettingsValue
import com.iwedia.cltv.platform.`interface`.InputSourceInterface
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.OadUpdateInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlatformOsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.PreferenceChannelsInterface
import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.`interface`.PromotionInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.SchedulerInterface
import com.iwedia.cltv.platform.`interface`.SubtitleInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TTXInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PrefMenu
import com.iwedia.cltv.platform.model.PrefSubMenu
import com.iwedia.cltv.platform.model.PrefType
import com.iwedia.cltv.platform.model.PromotionItem
import com.iwedia.cltv.platform.model.ReferenceSystemInformation
import com.iwedia.cltv.platform.model.SystemInfoData
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.ci_plus.CamInfoLanguageData
import com.iwedia.cltv.platform.model.ci_plus.CamInfoModuleInformation
import com.iwedia.cltv.platform.model.ci_plus.CamTypePreference
import com.iwedia.cltv.platform.model.fast_backend_utils.FastTosOptInHelper
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.Events.BLOCK_TV_VIEW
import com.iwedia.cltv.platform.model.information_bus.events.Events.CHANNEL_CHANGED
import com.iwedia.cltv.platform.model.information_bus.events.Events.EPG_DATA_UPDATED
import com.iwedia.cltv.platform.model.information_bus.events.Events.EVENTS_LOADED
import com.iwedia.cltv.platform.model.information_bus.events.Events.PREFERENCE_CLEAR_ALL_CHANNELS
import com.iwedia.cltv.platform.model.information_bus.events.Events.PREFERENCE_DELETE_CHANNEL
import com.iwedia.cltv.platform.model.information_bus.events.Events.REFRESH_FOR_YOU
import com.iwedia.cltv.platform.model.information_bus.events.Events.UPDATE_REC_LIST_INDICATOR
import com.iwedia.cltv.platform.model.information_bus.events.Events.UPDATE_WATCHLIST_INDICATOR
import com.iwedia.cltv.platform.model.information_bus.events.Events.WATCHLIST_RESTORED
import com.iwedia.cltv.platform.model.information_bus.events.Events.ZAP_ON_GUIDE_ONLY
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.language.LanguageCode
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.cltv.platform.model.parental.InputSourceData
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.home_scene.HomeSceneBase
import com.iwedia.cltv.scene.home_scene.HomeSceneBroadcast
import com.iwedia.cltv.scene.home_scene.HomeSceneData
import com.iwedia.cltv.scene.home_scene.HomeSceneFast
import com.iwedia.cltv.scene.home_scene.HomeSceneListener
import com.iwedia.cltv.scene.home_scene.HomeSceneVod
import com.iwedia.cltv.scene.home_scene.guide.HorizontalGuideSceneWidget
import com.iwedia.cltv.scene.home_scene.guide.HorizontalGuideSceneWidget.Companion.activeCategoryId
import com.iwedia.cltv.scene.parental_control.change_pin.ParentalPinSceneData
import com.iwedia.cltv.scene.player_scene.AudioSubtitlesHelper
import com.iwedia.cltv.scene.preferences_info_scene.PreferencesInfoSceneData
import com.iwedia.cltv.scene.preferences_status.PreferencesStatusSceneData
import com.iwedia.cltv.scene.zap_digit.DigitZapItem
import com.iwedia.cltv.utils.Utils
import kotlinx.coroutines.Dispatchers
import listeners.AsyncDataReceiver
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.timerTask
import kotlin.random.Random


/**
 * Home Scene Manager
 *
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.R)
class HomeSceneManager : ReferenceSceneManager, HomeSceneListener {

    private val TAG = javaClass.simpleName

    companion object {
        var IS_VOD_ACTIVE = false
        val IS_VOD_ENABLED = false // this flag is used to define whether VOD is enabled in the App or not. If set to True Search will filter VOD items on query and HomeSceneVod will be used instead of HomeSceneFast
    }

    private var updateSystemInformationTask: TimerTask? = null
    private var updateSystemInformationTimer: Timer? = null

    // Settings opened flag
    private var settingsOpened = false
    private var eventsLoaded = false

    var mGuideFilterList: ArrayList<Category>? = null
    val SETTINGS_PACKAGE= "com.android.tv.settings"
    val SETTINGS_CHANNEL_ACTIVITY = "com.mediatek.tv.settings.channelsetting.ChannelActivity"
    val SETTINGS_POWER_ACTIVITY = "com.mediatek.tv.settings.power.PowerActivity"

    private val MENU_CI_USER_PREFERENCE = "menu_ci_user_preference"
    private val MENU_CI_USER_PREFERENCE_DEFAULT_ID = "menu_ci_user_preference_default_id"
    private val MENU_CI_USER_PREFERENCE_AMMI_ID = "menu_ci_user_preference_ammi_id"
    private val MENU_CI_USER_PREFERENCE_BROADCAST_ID = "menu_ci_user_preference_broadcast_id"

    private val MENU_CAM_TYPE_PREFERENCE = "menu_cam_type_preference"
    private val MENU_CAM_TYPE_PREFERENCE_PCMCIA_ID = "menu_cam_type_preference_pcmcia_id"
    private val MENU_CAM_TYPE_PREFERENCE_USB_ID = "menu_cam_type_preference_usb_id"

    // Ads targeting status preference key
    private val KEY_ADS_TARGETING = "ads_targeting"

    /**
     * Default Aspect Ratio Option
     */
    private var defaultAspectRatioOption = 0

    //distinguish add and rename fav list scene
    var isAddFavouriteScene: Boolean = false
    private var mCaptioningManager: CaptioningManager? = null

    private var closedCaptionModule :ClosedCaptionInterface? = null
    //contains the map of channel events which are loaded in last call
    var mGuideChannelListForSelectedFilter = mutableListOf<TvChannel>()
    var channelList : MutableList<TvChannel>? = null
    private var RFChannelNumbers = mapOf(
        177500 to 5, 184500 to 6, 191500 to 7, 198500 to 8,
        205500 to 9, 212500 to 10, 219500 to 11, 226500 to 12,
        233500 to 13, 474000 to 21, 482000 to 22, 490000 to 23,
        498000 to 24, 506000 to 25, 514000 to 26, 522000 to 27,
        530000 to 28, 538000 to 29, 546000 to 30, 554000 to 31,
        562000 to 32, 570000 to 33, 578000 to 34, 586000 to 35,
        594000 to 36, 602000 to 37, 610000 to 38, 618000 to 39,
        626000 to 40, 634000 to 41, 642000 to 42, 650000 to 43,
        658000 to 44, 666000 to 45, 674000 to 46, 682000 to 47,
        690000 to 48, 698000 to 49, 706000 to 50, 714000 to 51,
        722000 to 52, 730000 to 53, 738000 to 54, 746000 to 55,
        754000 to 56, 762000 to 57, 770000 to 58, 778000 to 59,
        786000 to 60, 794000 to 61, 802000 to 62, 810000 to 63,
        818000 to 64, 826000 to 65, 834000 to 66, 842000 to 67,
        850000 to 68, 858000 to 69
    )

    private var pvrModule: PvrInterface
    private var hbbTvModule: HbbTvInterface
    private var favoriteModule: FavoritesInterface
    private  var tvModule: TvInterface
    private  var epgModule: EpgInterface
    private var forYouModule : ForYouInterface
    private var watchlistModule : WatchlistInterface
    private var schedulerModule: SchedulerInterface
    private var preferenceModule: PreferenceInterface
    private var preferenceChannelsModule: PreferenceChannelsInterface
    private var categoryModule: CategoryInterface
    private  var timeShiftModule: TimeshiftInterface
    private  var playerModule: PlayerInterface
    private  var tvInputModule: TvInputInterface
    private  var networkModule: NetworkInterface
    private var fastUserSettingsModule: FastUserSettingsInterface
    private  var parentalControlSettingsModule: ParentalControlSettingsInterface
    private  var utilsModule: UtilsInterface
    private  var ciPlusModule: CiPlusInterface
    private  var inputSourceModule: InputSourceInterface
    private var timeModule: TimeInterface
    private var subtitleModule: SubtitleInterface
    private var promotionModule: PromotionInterface
    private var ttxModule: TTXInterface
    private var oadModule: OadUpdateInterface
    private var generalConfigModule: GeneralConfigInterface
    private lateinit var activeChannelBroadcast: TvChannel
    private lateinit var activeChannel: TvChannel
    private lateinit var homeScene: HomeSceneBase
    private var isCurrentInputBlocked: Boolean? = false
    private var hideLockedServices : Boolean = false
    private var isForYouNeedRefresh : Boolean = false
    private val textToSpeechModule: TTSInterface
    private val platformOsInterface: PlatformOsInterface
    private var availablePreferencesCategories = mutableListOf<CategoryItem>()
    private var isTosAccepted = false

    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        pvrModule: PvrInterface,
        hbbTvModule: HbbTvInterface,
        favoriteModule: FavoritesInterface,
        forYouModule: ForYouInterface,
        tvModule: TvInterface,
        epgModule: EpgInterface,
        watchlistModule: WatchlistInterface,
        schedulerModule: SchedulerInterface,
        preferenceModule :PreferenceInterface,
        timeShiftModule: TimeshiftInterface,
        playerModule: PlayerInterface,
        tvInputModule: TvInputInterface,
        parentalControlSettingsModule: ParentalControlSettingsInterface,
        utilsModule: UtilsInterface,
        preferenceChannelsModule: PreferenceChannelsInterface,
        closedCaptionModule: ClosedCaptionInterface,
        ciPlusModule: CiPlusInterface,
        inputSourceMoudle: InputSourceInterface,
        timeModule: TimeInterface,
        subtitleModule: SubtitleInterface,
        promotionModule: PromotionInterface,
        categoryInterface: CategoryInterface,
        fastUserSettingsInterface: FastUserSettingsInterface,
        networkInterface: NetworkInterface,
        ttxModule: TTXInterface,
        oadModule: OadUpdateInterface,
        generalConfigModule: GeneralConfigInterface,
        textToSpeechModule: TTSInterface,
        platformOsInterface: PlatformOsInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.HOME_SCENE
    ) {
        isScreenFlowSecured = false
        this.pvrModule = pvrModule
        this.hbbTvModule = hbbTvModule
        this.favoriteModule = favoriteModule
        this.tvModule = tvModule
        this.epgModule = epgModule
        this.forYouModule = forYouModule
        this.watchlistModule = watchlistModule
        this.schedulerModule =  schedulerModule
        this.preferenceModule = preferenceModule
        this.timeShiftModule = timeShiftModule
        this.playerModule = playerModule
        this.tvInputModule = tvInputModule
        this.parentalControlSettingsModule = parentalControlSettingsModule
        this.utilsModule = utilsModule
        this.preferenceChannelsModule = preferenceChannelsModule
        this.closedCaptionModule = closedCaptionModule
        this.ciPlusModule = ciPlusModule
        this.inputSourceModule = inputSourceMoudle
        this.timeModule = timeModule
        this.subtitleModule = subtitleModule
        this.promotionModule = promotionModule
        this.categoryModule = categoryInterface
        this.networkModule = networkInterface
        this.fastUserSettingsModule = fastUserSettingsInterface
        this.ttxModule = ttxModule
        this.oadModule = oadModule
        this.generalConfigModule = generalConfigModule
        this.textToSpeechModule = textToSpeechModule
        this.platformOsInterface = platformOsInterface
        registerGenericEventListener(Events.SHOW_STOP_RECORDING_DIALOG)
        registerGenericEventListener(Events.TIME_CHANGED)
        registerGenericEventListener(Events.CHANNEL_CHANGED)
        registerGenericEventListener(Events.FAVORITE_LIST_CATEGORY_UPDATED)
        registerGenericEventListener(Events.RECENTLY_WATCHED_UPDATED)
        registerGenericEventListener(Events.REFRESH_FOR_YOU)
        registerGenericEventListener(Events.SCHEDULED_RECORDING_REMOVED)
        registerGenericEventListener(Events.FAVORITE_LIST_UPDATED)
        registerGenericEventListener(Events.CHANNEL_LIST_UPDATED)
        registerGenericEventListener(CHANNEL_CHANGED)
        registerGenericEventListener(PREFERENCE_DELETE_CHANNEL)
        registerGenericEventListener(PREFERENCE_CLEAR_ALL_CHANNELS)
        registerGenericEventListener(UPDATE_WATCHLIST_INDICATOR)
        registerGenericEventListener(UPDATE_REC_LIST_INDICATOR)
        registerGenericEventListener(ZAP_ON_GUIDE_ONLY)
        registerGenericEventListener(Events.FAST_FAVORITES_COLLECTED)
        registerGenericEventListener(Events.EVENTS_LOADED)
        registerGenericEventListener(Events.EPG_DATA_UPDATED)
        registerGenericEventListener(Events.WATCHLIST_RESTORED)
        registerGenericEventListener(Events.CLOSED_CAPTION_CHANGED)
        registerGenericEventListener(Events.PLAYBACK_STARTED)
        registerGenericEventListener(Events.CHANNELS_LOADED)
        registerGenericEventListener(Events.ANOKI_RATING_LEVEL_SET)
        registerGenericEventListener(Events.ANOKI_CHANNEL_LIST_REORDER_FINISHED)
        registerGenericEventListener(Events.PVR_RECORDING_RENAMED)
        registerGenericEventListener(Events.VOD_PLAY_BACK_ERROR)
        registerGenericEventListener(Events.VOD_PLAY_STATE)
        registerGenericEventListener(Events.VOD_PLAYER_STATE)
        registerGenericEventListener(Events.FOR_YOU_NOW_NEXT_UPDATED)
        registerGenericEventListener(Events.VOD_CONTENT_IS_WATCHED)
        registerGenericEventListener(Events.NO_ETHERNET_EVENT)
        registerGenericEventListener(Events.ETHERNET_EVENT)
        hideLockedServices = utilsModule.getCountryPreferences(UtilsInterface.CountryPreference.HIDE_LOCKED_SERVICES_IN_EPG,false) as Boolean

    }

    override fun isAnokiServerReachable(): Boolean {
        return networkModule.anokiServerStatus.value!!
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startScan() {
        playerModule.stop()
        InformationBus.submitEvent(Event(Events.PLAYBACK_STATUS_MESSAGE_NONE))

        if (!utilsModule.kidsModeEnabled()) {
            try {
                utilsModule.startScanChannelsIntent()
            } catch (e: Exception) {
                showToast("NO SCAN INTENT FOUND")
            }
        }
    }

    override fun isChannelListEmpty(): Boolean {
        tvModule.getChannelList(ApplicationMode.DEFAULT).forEach {
            if (it.isBrowsable || it.inputId.contains("iwedia") || it.inputId.contains("sampletvinput")) {
                return false
            }
        }
        return true
    }

    override fun isRegionSupported(): Boolean {
        return ReferenceApplication.isRegionSupported
    }

    override fun getEventsByChannelList(
        callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>,
        channelList: MutableList<TvChannel>,
        dayOffset: Int,
        additionalDayOffset: Int,
        isExtend: Boolean
    ) {
        fetchEventForChannelList(callback,channelList,dayOffset,additionalDayOffset,isExtend)
    }

    override fun muteAudio() {
        if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).isVisible(id)) {
            playerModule.mute()
        }
    }

    override fun changeChannelFromTalkback(tvChannel: TvChannel) {
        tvModule.changeChannel(tvChannel, object: IAsyncCallback{
            override fun onFailed(error: Error) {
                println(error)
            }

            override fun onSuccess() {
                println("changeChannelFromTalkback onSuccess")
            }
        },  applicationMode = if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT)
    }

    override fun unMuteAudio() {
        if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).isVisible(id)) {
            playerModule.unmute()
        }
    }

    override fun getDateTimeFormat() : DateTimeFormat {
        return utilsModule.getDateTimeFormat()
    }

    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>, applicationMode: ApplicationMode) {
        tvModule.getActiveChannel(callback, applicationMode)
    }

    override fun getRecordingInProgressTvChannel(): TvChannel? {
        return pvrModule.getRecordingInProgressTvChannel()
    }

    override fun isRecordingInProgress(): Boolean {
        return pvrModule.isRecordingInProgress()
    }

    override fun getPromotionContent(callback: IAsyncDataCallback<ArrayList<PromotionItem>>) {
        promotionModule.getPromotionList(callback)
    }

    override fun onFastCardClicked(tvEvent: TvEvent) {
        if(tvEvent.isVodContent()) {
            onVodItemClicked(if(tvEvent.subGenre!! == Constants.VodTypeConstants.SERIES) VODType.SERIES else VODType.SINGLE_WORK, tvEvent.id.toString())
        } else if (tvEvent.name == "Explore FREE TV Guide") {
            val intent = Intent(FastZapBannerDataProvider.FAST_SHOW_GUIDE_INTENT)
            context?.sendBroadcast(intent)
        } else if (tvEvent.tag != null && tvEvent.tag is String) {
            if (tvEvent.tag == "guide") {
                val intent = Intent(FastZapBannerDataProvider.FAST_SHOW_GUIDE_INTENT)
                context?.sendBroadcast(intent)
            } else  if (tvEvent.tag == "vod") {
                //TODO handle vod content
            } else {
                playFastChannel(tvEvent.tvChannel)
            }
        } else {
            playFastChannel(tvEvent.tvChannel)
        }
    }

    private fun ensureSubtitleLanguages(language: String, type: PrefType) {
        if (utilsModule.getPrimarySubtitleLanguage(type).isNullOrEmpty()) {
            utilsModule.setPrimarySubtitleLanguage(language, type)
        }
        if (utilsModule.getSecondarySubtitleLanguage(type).isNullOrEmpty()) {
            utilsModule.setSecondarySubtitleLanguage(language, type)
        }
    }
    private fun ensureAudioLanguages(language: String, type: PrefType) {
        Log.e("BHANYA","ensureAudioLanguages $language")

        if (utilsModule.getPrimaryAudioLanguage(type).isNullOrEmpty()) {
            utilsModule.setPrimaryAudioLanguage(language, type)
        }
        if (utilsModule.getSecondaryAudioLanguage(type).isNullOrEmpty()) {
            utilsModule.setSecondaryAudioLanguage(language, type)
        }
    }

    private fun playFastChannel(tvChannel: TvChannel) {
        if (pvrModule.isRecordingInProgress()) {
            val sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {}

                override fun onPositiveButtonClicked() {
                    val recordingChannel = pvrModule.getRecordingInProgressTvChannel()
                    pvrModule.stopRecordingByChannel(recordingChannel!!, object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.d(Constants.LogTag.CLTV_TAG + ReferenceApplication.TAG, "stop recording failed")
                        }

                        override fun onSuccess() {
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            PvrBannerSceneManager.previousProgress = 0L
                            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                            tuneToFastChannel(tvChannel)
                        }
                    })
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW, sceneData
            )
        } else if (timeShiftModule.isTimeShiftActive) {
            showTimeShiftChannelChangeDialog(tvChannel, true)
        } else {
            tuneToFastChannel(tvChannel)
        }
    }

    private fun tuneToFastChannel(tvChannel: TvChannel) {
        //Set application mode 1 for FAST ONLY mode
        utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
        playerModule.stop()
        ReferenceApplication.runOnUiThread {
            val intent = Intent(FastZapBannerDataProvider.FAST_SHOW_ZAP_BANNER_INTENT)
            ReferenceApplication.applicationContext().sendBroadcast(intent)
            ReferenceApplication.worldHandler?.triggerAction(
                ReferenceWorldHandler.SceneId.HOME_SCENE,
                Action.HIDE
            ) // HIDE is used because whole Home Scene should stay in memory in order to enable it's fast accessing when pressing back from LiveScene
            playerModule.unmute()
        }
        tvModule.changeChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {}
            override fun onSuccess() {}
        }, ApplicationMode.FAST_ONLY)
        BackFromPlayback.resetKeyPressedState()
    }

    private fun updateRecordings() {
        if (homeScene.recordingsWidget != null) {
            var scheduledRecordings: ArrayList<com.iwedia.cltv.platform.model.recording.ScheduledRecording> = arrayListOf()
            schedulerModule.getScheduledRecordingsList(object: IAsyncDataCallback<ArrayList<com.iwedia.cltv.platform.model.recording.ScheduledRecording>>{
                override fun onFailed(error: Error) {
                    homeScene.refreshRecordingsList(arrayListOf())
                }

                override fun onReceive(data: ArrayList<com.iwedia.cltv.platform.model.recording.ScheduledRecording>) {
                    data.forEach { item ->
                        scheduledRecordings.add(
                            com.iwedia.cltv.platform.model.recording.ScheduledRecording(
                                item.id,
                                item.name,
                                item.scheduledDateStart,
                                item.scheduledDateEnd,
                                item.tvChannelId,
                                item.tvEventId,
                                RepeatFlag.NONE,
                                item.tvChannel,
                                item.tvEvent
                            )
                        )
                    }
                    homeScene.refreshRecordingsList(scheduledRecordings)
                }
            })
        }
    }

    override fun isAccessibilityEnabled(): Boolean{
        return  utilsModule.isAccessibilityEnabled()
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

//    override fun onOpenSourceLicenceClicked() {
//        context!!.runOnUiThread {
//            worldHandler!!.triggerAction(id, Action.HIDE)
//            worldHandler!!.triggerAction(
//                ReferenceWorldHandler.SceneId.OPEN_SOURCE_LICENSE_SCENE,
//                Action.SHOW
//            )
//        }
//    }

    override fun createScene() {
        if (isRegionSupported()) {
            homeScene = if (IS_VOD_ENABLED) {
                VodTrailerHelper.initialize()
                HomeSceneVod(context!!, this)
            } else {
                HomeSceneFast(context!!, this)
            }
        } else {
            homeScene = HomeSceneBroadcast(context!!, this)
        }
        scene = homeScene
    }

    override fun onSceneInitialized() {
        FastTosOptInHelper.fetchTosOptInFromServer(ReferenceApplication.applicationContext()) {
            isTosAccepted = it == 1
        }
        favoriteModule.setup()
        getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
            }

            override fun onReceive(data: TvChannel) {
                activeChannelBroadcast = data
            }
        })

        val applicationMode = if(ReferenceApplication.worldHandler!!.getApplicationMode() ==ApplicationMode.DEFAULT.ordinal)  ApplicationMode.DEFAULT else  ApplicationMode.FAST_ONLY

        getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
            }

            override fun onReceive(data: TvChannel) {
                activeChannel = data
            }

        }, applicationMode)

        refreshTime()

        if (data != null) {
            if (data is HomeSceneData) {
                if (isRegionSupported()) {
                    HomeSceneFast.jumpLiveTabCurrentEvent = (data as HomeSceneData).focusToCurrentEvent
                } else {
                    HomeSceneBroadcast.jumpLiveTabCurrentEvent = (data as HomeSceneData).focusToCurrentEvent
                }
                scene?.refresh((data as HomeSceneData).initialFilterPosition)
            } else if (data!!.getDataByIndex(0) != null) {
                var filterViewPosition = data!!.getDataByIndex(0) as Int
                scene!!.refresh(filterViewPosition)
            }
        } else {
            scene!!.refresh(0)
        }

        //Show For You content after scene showing
        if (::homeScene.isInitialized) {
            homeScene.sceneInit()
        } else {
            Handler(Looper.myLooper()!!).postDelayed(Runnable {
                homeScene.sceneInit()
            }, 100)
        }
        mCaptioningManager =
            ReferenceApplication.getActivity()
                .getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onEventReceived: event ${event!!.type}")
        if(event.type == Events.PLAYBACK_STARTED){
            if ((scene is HomeSceneFast || scene is HomeSceneVod) && worldHandler?.active?.id == id) {
                if (scene is HomeSceneFast) (scene as HomeSceneFast).checkMutePlayback()
                else (scene as HomeSceneVod).checkMutePlayback()
            }
        }
        if (event.type == Events.FAST_FAVORITES_COLLECTED) {
            FastLiveTabDataProvider.setup()
        }
        if (event.type == Events.REFRESH_FOR_YOU) {
            if(worldHandler?.active?.id != id)
                isForYouNeedRefresh = true
            else
                homeScene.refreshForYou()
        }
        if (event.type == UPDATE_WATCHLIST_INDICATOR) {
            homeScene.guideSceneWidget?.let {
                homeScene.guideSceneWidget!!.refreshWatchlistButton()
                homeScene.guideSceneWidget!!.refreshIndicators()
            }
        }
        if (event.type == UPDATE_REC_LIST_INDICATOR) {
            homeScene.guideSceneWidget?.let {
                homeScene.guideSceneWidget!!.refreshRecordButton()
            }
        }
        if (event.type == Events.CHANNELS_LOADED) {
            homeScene.refreshEpgData()
            homeScene.refreshEpgEventsData()
        }
        if (event.type == Events.FAVORITE_LIST_UPDATED) {
            if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.HOME_SCENE)) {
                if (event.getData(1) != null) {
                    if (homeScene.getActiveCategory() == 2) {//if active category is epg
                        homeScene.refreshGuideDetailsFavButton()
                        updateGuideCategoryList()
                    }
                }
            }else{
                val list = arrayListOf<String>()
                list.addAll(event.getData(1) as ArrayList<String>)
                getAvailableChannelFilters(object : IAsyncDataCallback<ArrayList<Category>> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: ArrayList<Category>) {
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "onReceive: getAvailableChannelFilters updateGuideCategoryList data ${data.size}"
                        )
                        var filterList = arrayListOf<CategoryItem>()
                        var id = 0
                        data.forEach { filterItem ->
                            filterList.add(CategoryItem(filterItem.id, filterItem.name!!))
                            id++
                        }
                        homeScene.updateFavoritesData(list,filterList)
                    }
                })
            }
        }
        if (event.type ==Events.CHANNEL_LIST_UPDATED){
            var isBroadcastChannelUpdated = false
            if (channelList.isNullOrEmpty()){
                channelList = tvModule.getChannelList()
            }else{
                //checking whether the new list have same elements as previous or not.
                // if not then update the epg with the new list
                if (channelList!!.size!=tvModule.getChannelList().size) {
                    isBroadcastChannelUpdated = true
                    channelList = tvModule.getChannelList()
                }
                else{
                    run breaking@ {
                        tvModule.getChannelList().forEachIndexed { index, tvChannel ->
                            if (channelList!![index].getUniqueIdentifier()!= tvChannel.getUniqueIdentifier())
                            {
                                channelList = tvModule.getChannelList()
                                isBroadcastChannelUpdated = true
                                val applicationMode = if(ReferenceApplication.worldHandler!!.getApplicationMode() ==ApplicationMode.DEFAULT.ordinal)  ApplicationMode.DEFAULT else  ApplicationMode.FAST_ONLY

                                getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                                    override fun onFailed(error: Error) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
                                    }

                                    override fun onReceive(data: TvChannel) {
                                        activeChannel = data
                                    }

                                }, applicationMode)
                                return@breaking
                            }
                        }
                    }
                }
            }
            homeScene.refreshEpgChannelsData(isBroadcastChannelUpdated)
        }
        if (event.type == Events.ANOKI_RATING_LEVEL_SET) {
            homeScene.refreshEpgData()
            homeScene.refreshEpgChannelsData()
        }
        if (event.type == Events.ANOKI_CHANNEL_LIST_REORDER_FINISHED) {
            if (homeScene is HomeSceneFast) {
                (homeScene as HomeSceneFast).channelListReordered()
            }
        }
        if (event.type == Events.PVR_RECORDING_RENAMED) {
            if (homeScene.selectedPosition == 0) {
                if(worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.HOME_SCENE){
                    isForYouNeedRefresh = true
                }
            }
        }
        if (event.type == ZAP_ON_GUIDE_ONLY){
            (homeScene.zapOnGuideOnly(event.data?.get(0) as TvChannel))
        }
        if (event.type == EVENTS_LOADED) {
            if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).getPlatformName() != "RefPlus_5.0") {
                if (!eventsLoaded) {
                    homeScene.refreshForYou()
                }
            }
            eventsLoaded = true
        }
        if (event.type == WATCHLIST_RESTORED) {
            homeScene.refreshForYou()
        }
        if (event.type == EPG_DATA_UPDATED) {
            homeScene.refreshEpgData()
            homeScene.refreshEpgEventsData()
        }
        if (event.type ==CHANNEL_CHANGED){
            if (!homeScene.channelChanged()) {
                ReferenceApplication.runOnUiThread {
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(
                        ReferenceWorldHandler.SceneId.LIVE
                    )
                    var activeScene = ReferenceApplication.worldHandler!!.active
                    var sceneData = SceneData(activeScene!!.id, activeScene.instanceId)
                    ReferenceApplication.worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.ZAP_BANNER,
                        Action.SHOW_OVERLAY, sceneData
                    )
                }
            }

            if(homeScene.selectedPosition == -1){
                if (homeScene is HomeSceneFast) {
                    getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
                        }

                        override fun onReceive(data: TvChannel) {
                            if(!data.isAnalogChannel()) {
                                var contains = false //contains function didnt work correctly
                                run exitForEach@{
                                    availablePreferencesCategories.forEach {
                                        if (it.id == ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION) {
                                            contains = true
                                            return@forEach
                                        }
                                    }
                                }
                                if(!contains){
                                    availablePreferencesCategories.add(CategoryItem(
                                        ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION,
                                        ConfigStringsManager.getStringById("system_information")
                                    ))
                                    (homeScene  as HomeSceneFast).refreshCategoryItems(CategoryItem(
                                        ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION,
                                        ConfigStringsManager.getStringById("system_information")
                                    ))
                                }
                            }
                        }
                    })
                }
            }
        }

        if (event.type == PREFERENCE_DELETE_CHANNEL || event.type == PREFERENCE_CLEAR_ALL_CHANNELS  ){
            val sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title =
                ConfigStringsManager.getStringById(
                    if (event.type == PREFERENCE_DELETE_CHANNEL) "delete_channel_confirmation" else  "delete_all_channel_confirmation")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("Sure")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener =
                object : DialogSceneData.DialogClickListener {
                    override fun onNegativeButtonClicked() {
                        (event.data?.get(0)as IAsyncCallback).onFailed(java.lang.Error("Don't want to delete"))
                    }

                    override fun onPositiveButtonClicked() {
                        if (pvrModule.isRecordingInProgress()) {
                            ReferenceApplication.worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY
                            )
                            val recSceneData = DialogSceneData(id, instanceId)
                            recSceneData.title =
                                ConfigStringsManager.getStringById("recording_exit_msg")
                            recSceneData.positiveButtonText =
                                ConfigStringsManager.getStringById("ok")
                            recSceneData.negativeButtonText =
                                ConfigStringsManager.getStringById("cancel")
                            recSceneData.dialogClickListener =
                                object : DialogSceneData.DialogClickListener {
                                    override fun onNegativeButtonClicked() {
                                        (event.data?.get(0) as IAsyncCallback).onFailed(
                                            java.lang.Error(
                                                "Don't want to delete"
                                            )
                                        )
                                    }

                                    override fun onPositiveButtonClicked() {
                                        pvrModule.stopRecordingByChannel(
                                            pvrModule.getRecordingInProgressTvChannel()!!,
                                            object : IAsyncCallback {
                                                override fun onFailed(error: Error) {}
                                                override fun onSuccess() {
                                                    ReferenceApplication.worldHandler!!.playbackState =
                                                        ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                                                    PvrBannerSceneManager.previousProgress = 0L
                                                    pvrModule.setRecIndication(false)
                                                }
                                            }
                                        )
                                        ReferenceApplication.runOnUiThread {
                                            ReferenceApplication.worldHandler!!.triggerAction(
                                                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                Action.DESTROY
                                            )
                                        }
                                        (event.data?.get(0) as IAsyncCallback).onSuccess()
                                    }
                                }
                            ReferenceApplication.worldHandler!!.triggerActionWithData(
                                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                Action.SHOW_OVERLAY, recSceneData
                            )
                        }
                        else {
                            ReferenceApplication.runOnUiThread {
                                ReferenceApplication.worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                    Action.DESTROY
                                )
                            }
                            (event.data?.get(0) as IAsyncCallback).onSuccess()
                        }
                    }
                }
            ReferenceApplication.runOnUiThread{
                ReferenceApplication.worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.SHOW_OVERLAY, sceneData
                )
            }
        }
        if (event.type == Events.CLOSED_CAPTION_CHANGED) {
            homeScene.refreshClosedCaption()
        }
        if (event?.type == Events.SHOW_STOP_RECORDING_DIALOG) {
            if (event.data != null && event.getData(0) is TvChannel) {
                var tvChannel = event.getData(0) as TvChannel

                utilsModule.runCoroutine({
                    var sceneData = DialogSceneData(id, instanceId)
                    sceneData.type = DialogSceneData.DialogType.YES_NO
                    sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
                    sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                    sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
                    sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                        override fun onNegativeButtonClicked() {
                            worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY
                            )
                        }

                        override fun onPositiveButtonClicked() {
                            var recordingChannel = pvrModule.getRecordingInProgressTvChannel()
                            pvrModule.stopRecordingByChannel(recordingChannel!!, object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    Log.d(Constants.LogTag.CLTV_TAG + ReferenceApplication.TAG, "stop recording failed")
                                }

                                override fun onSuccess() {
                                    playTvChannel(tvChannel)
                                    ReferenceApplication.worldHandler!!.playbackState =
                                        ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                                    PvrBannerSceneManager.previousProgress = 0L
                                    worldHandler!!.destroyOtherExisting(
                                        ReferenceWorldHandler.SceneId.LIVE
                                    )
                                }
                            })
                        }
                    }
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW, sceneData
                    )
                }, Dispatchers.Main)
            }
        }

        if(event.type == Events.VOD_PLAY_BACK_ERROR){
            (scene as HomeSceneVod).videoReadyToPlay(playing = false)
            trailerPlaying(isPlaying = false)
        }

        if(event.type == Events.VOD_PLAY_STATE){
            val isPlaying = (event.data?.get(0) as Boolean)
            (scene as HomeSceneVod).videoReadyToPlay(playing = isPlaying)
            trailerPlaying(isPlaying = isPlaying)
        }

        if (event.type == Events.VOD_PLAYER_STATE) {
            val playbackState = (event.data?.get(0) as Int)
            if(playbackState == ExoPlayer.STATE_READY){
                if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE){
                    val isFastOnly = ((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()
                    // Todo : Boris & Vasilisa please review this condition
                    if (((homeScene.getActiveCategory() == 2 && isFastOnly)
                                || (homeScene.getActiveCategory() == 3 && !isFastOnly)) && !homeScene.settingsIv!!.hasFocus() &&
                        !homeScene.searchIv!!.hasFocus()) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG,"worldHandler?.active?.id ${worldHandler?.active?.id}")
                        if(worldHandler?.active?.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                            muteAudio()
                        }
                    }
                }
            }
        }

        if (event.type == Events.FOR_YOU_NOW_NEXT_UPDATED) {
            if (event.data != null && event.data!!.get(0) is ArrayList<*>) {
                addForYouBroadcastChannelsRow {
                    var rails = event.data!!.get(0) as ArrayList<RailItem>
                    ReferenceApplication.runOnUiThread {
                        if (homeScene.forYouWidget != null) {
                            var list = ArrayList<RailItem>()
                            list.addAll(it)
                            list.addAll(rails.distinct())
                            homeScene.forYouWidget?.update(list)
                        }
                    }
                }

            }
        }

        if (event.type == Events.VOD_CONTENT_IS_WATCHED) {
            if (event.data != null && event.data!![0] is Boolean) {
                val isContentWatched = event.data!![0] as Boolean
                ReferenceApplication.runOnUiThread {
                    if (homeScene is HomeSceneVod) {
                        (homeScene as HomeSceneVod).setVodContentIsWatchedFlag(isContentWatched)
                    }
                }
            }
        }
        when(event.type){
            Events.NO_ETHERNET_EVENT, Events.ETHERNET_EVENT -> {
                if (homeScene is HomeSceneVod && BuildConfig.FLAVOR.contains("refplus5")) {
                    (homeScene as HomeSceneVod).setInternet(event.type == Events.ETHERNET_EVENT)
                }
            }
        }
    }

    private fun trailerPlaying(isPlaying:Boolean) = if(isPlaying) unMuteAudio() else muteAudio()


    override fun onDestroy() {
        super.onDestroy()
        if (IS_VOD_ENABLED) {
            VodTrailerHelper.cancel()
        }

        if (updateSystemInformationTask != null) {
            updateSystemInformationTask!!.cancel()
            updateSystemInformationTask = null
        }

        if (updateSystemInformationTimer != null) {
            updateSystemInformationTimer?.cancel()
            updateSystemInformationTimer?.purge()
            updateSystemInformationTimer = null
        }

        if (mGuideFilterList != null && mGuideFilterList!!.isNotEmpty()) {
            mGuideFilterList?.clear()
            mGuideFilterList = null
        }
        if (mGuideChannelListForSelectedFilter.isNotEmpty()) {
            mGuideChannelListForSelectedFilter.clear()
        }

        isCurrentInputBlocked = false
        if (mCaptioningManager != null) mCaptioningManager = null

    }

    override fun initConfigurableKeys() {
    }

    override fun onTimeChanged(currentTime: Long) {
        if (TimeUnit.MILLISECONDS.toSeconds(currentTime) % 60 == 0L) {
            if (tvModule != null) {
                //can cause crash when channel list size is less than desired index
                if ((tvModule.getDesiredChannelIndex() >= 0) && tvModule.getDesiredChannelIndex()<tvModule.getChannelList().size) {
                    var activeChannel = tvModule.getChannelList()[tvModule.getDesiredChannelIndex()]
                    scene?.refresh(timeModule.getCurrentTime(activeChannel))
                }else{
                    scene?.refresh(currentTime)
                }
            } else {
                scene?.refresh(currentTime)
            }
            CoroutineHelper.cleanUp()
        }
    }

    override fun showExitDialog() {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("app_exit_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("yes")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("no")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {}

                override fun onPositiveButtonClicked() {
                    playerModule.stop()
                    InformationBus.submitEvent(Event(Events.EXIT_APPLICATION_ON_BACK_PRESS))
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
            muteAudio()
        }
    }

    override fun isEventLocked(tvEvent: TvEvent?) = parentalControlSettingsModule.isEventLocked(tvEvent)

    override fun backToLive() {
        ReferenceApplication.runOnUiThread(Runnable{

            val data = data!! as? HomeSceneData

            if (data != null ) {
                if(data.openEditChannel || data.openDeviceInfo) {
                    // To return to channel list
                    worldHandler!!.triggerActionWithInstanceId(
                        data!!.previousSceneId,
                        data!!.previousSceneInstance,
                        Action.SHOW
                    )

                    worldHandler!!.triggerAction(id, Action.DESTROY)

                    return@Runnable
                }
            }

            worldHandler!!.triggerAction(id, Action.HIDE)
            BackFromPlayback.resetKeyPressedState()
        })
    }

    override fun onBackPressed(): Boolean {
        //Move focus to Discover tab
        homeScene.homeFilterGridView?.layoutManager?.findViewByPosition(0)?.requestFocus()
        return true
    }

    /**
     * Refresh scene time info
     */
    private fun refreshTime() {
        if (this::activeChannelBroadcast.isInitialized) {
            val currentTime = timeModule.getCurrentTime(activeChannelBroadcast)
            scene?.refresh(currentTime)
        }else if (this::activeChannel.isInitialized) {
            val currentTime = timeModule.getCurrentTime(activeChannel)
            scene?.refresh(currentTime)
        }
    }

    override fun getAvailableChannelFilters(callback: IAsyncDataCallback<ArrayList<Category>>) {
        categoryModule.getAvailableFilters(object :IAsyncDataCallback<ArrayList<Category>>{
            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }

            override fun onReceive(data: ArrayList<Category>) {
                callback.onReceive(data)
            }
        })
    }

    override fun getAvailableFavouriteCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
        val favLists = arrayListOf<String>()
        favoriteModule.getAvailableCategories(object :
            IAsyncDataCallback<ArrayList<String>> {
            override fun onFailed(error: Error) {

            }

            override fun onReceive(data: ArrayList<String>) {
                data.forEach { item ->
                    favLists.add(item)
                }
                callback.onReceive(favLists)
            }

        })
    }

    override fun getAvailablePreferenceTypes(callback: IAsyncDataCallback<List<CategoryItem>>) {
        val categories = mutableListOf<CategoryItem>()
        for (type in preferenceModule.getPreferenceTypes()){
            if (isRegionSupported()) {
                if (type == PrefType.BASE)
                    categories.add(CategoryItem(type, ConfigStringsManager.getStringById("live")))
            }
            if (type == PrefType.PLATFORM)
                categories.add(CategoryItem(type, ConfigStringsManager.getStringById("broadcast")))
        }
        callback.onReceive(categories)
    }

    override fun getAvailablePreferencesCategories(callback: IAsyncDataCallback<List<CategoryItem>>, type: PrefType) {
        val menuOptionList = preferenceModule.getPreferenceMenus(type)

        //Todo remove
        availablePreferencesCategories = mutableListOf<CategoryItem>()
        for (menu in menuOptionList){
            if (menu == PrefMenu.SETUP)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP,menu, ConfigStringsManager.getStringById("setup")))
            if (menu == PrefMenu.AUDIO)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_AUDIO, ConfigStringsManager.getStringById("audio")))
            if (menu == PrefMenu.SUBTITLE)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_SUBTITLES, ConfigStringsManager.getStringById("subtitles")))
            if (menu == PrefMenu.CAMINFO && generalConfigModule.getGeneralSettingsInfo("ci"))
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_CAM_INFO, ConfigStringsManager.getStringById("cam_info")))
            if (menu == PrefMenu.PARENTAL_CONTROL)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_PARENTAL_CONTROL, ConfigStringsManager.getStringById("parental_control")))
            if (menu == PrefMenu.CLOSED_CAPTIONS)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_CLOSED_CAPTIONS, ConfigStringsManager.getStringById("closed_captions")))
            if (menu == PrefMenu.HBBTV && generalConfigModule.getGeneralSettingsInfo("hbbtv"))
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_HBBTV_SETTINGS, ConfigStringsManager.getStringById("hbbtv_settings")))
            if (menu == PrefMenu.PVR_TIMESHIFT) {
                if (generalConfigModule.getGeneralSettingsInfo("timeshift") || generalConfigModule.getGeneralSettingsInfo("pvr")) {
                    availablePreferencesCategories.add(
                        CategoryItem(
                            ReferenceWorldHandler.WidgetId.PREFERENCES_PVR_TIMESHIFT,
                            ConfigStringsManager.getStringById("preference_pvr_timeshift")
                        )
                    )
                }
            }
            if (menu == PrefMenu.TELETEXT) {
                //TODO to check this in future
                if (generalConfigModule.getGeneralSettingsInfo("teletext_enable_column"))
                    availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_TELETEXT, ConfigStringsManager.getStringById("teletext")))
            }
            if (menu == PrefMenu.SYSTEMINFO)
                if(this::activeChannelBroadcast.isInitialized) {
                    if(!activeChannelBroadcast.isAnalogChannel()) {
                        availablePreferencesCategories.add(
                            CategoryItem(
                                ReferenceWorldHandler.WidgetId.PREFERENCES_SYSTEM_INFORMATION,
                                ConfigStringsManager.getStringById("system_information")
                            )
                        )
                    }
                }
            if (menu == PrefMenu.ADS_TARGETING)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_ADS_TARGETING,menu, ConfigStringsManager.getStringById("ads_targeting")))

            if (menu == PrefMenu.OPEN_SOURCE_LICENSES)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.PREFERENCES_OPEN_SOURCE_LICENSES,menu, ConfigStringsManager.getStringById("open_source")))

            if (menu == PrefMenu.TERMS_OF_SERVICE)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.TERMS_OF_SERVICE,menu, ConfigStringsManager.getStringById("terms_of_service")))

            if (menu == PrefMenu.FEEDBACK)
                availablePreferencesCategories.add(CategoryItem(ReferenceWorldHandler.WidgetId.FEEDBACK,menu, ConfigStringsManager.getStringById("feedback")))

        }
        callback.onReceive(availablePreferencesCategories)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getPreferencesSetup() {
        val prefSetupOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.SETUP)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        prefSetupOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }

        val displayMode: HashMap<Int, String> = HashMap<Int, String>()

        displayMode[0] = ConfigStringsManager.getStringById("normal")
        displayMode[1] = ConfigStringsManager.getStringById("full")
        displayMode[2] = ConfigStringsManager.getStringById("zoom")
        val defaultDisplayMode =
            utilsModule.getPrefsValue("display_mode", 1) as Int

        val aspectRatioOptions = mutableListOf(
            ConfigStringsManager.getStringById("aspect_16_9"),
            ConfigStringsManager.getStringById("aspect_4_3"),
            ConfigStringsManager.getStringById("aspect_auto"),
            ConfigStringsManager.getStringById("aspect_original"),
        )

        var channelList = tvModule.getChannelList()
        channelList.sortBy { it.displayNumber }

        //Default value
        var defaultChannel: TvChannel? = channelList.firstOrNull()
        var defaultChannelIndex = 0

        var triplet = utilsModule.getPrefsValue(
                ReferenceApplication.DEFAULT_CHANNEL,
                ""
            ) as String

        if (triplet.isNotEmpty()) {
            var index = 0
            run exitForEach@{
                channelList.forEach { channel ->
                    var temp =
                        channel.onId.toString() + "|" + channel.tsId + "|" + channel.serviceId
                    if (temp == triplet) {
                        defaultChannel = channel
                        defaultChannelIndex = index
                        return@exitForEach
                    }
                    index++
                }
            }
        }
        defaultAspectRatioOption = utilsModule.getAspectRatio()

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getPreferencesSetup: "+channelList.size)

        val audioTracks: MutableList<LanguageCode> = mutableListOf()
        utilsModule.getLanguageMapper()!!.preferredLanguageCodes.forEach { item ->
            audioTracks.add(item)
        }

        var isInteractionChannelEnabled = utilsModule.getPrefsValue(
            ReferenceApplication.INTERACTION_CHANNEL,
            false
        ) as Boolean

        val noSignalPowerOffTimeList: List<String> = listOf(
            "duration_5_minutes",
            "duration_10_minutes",
            "duration_15_minutes",
            "duration_30_minutes",
            "duration_60_minutes"
        )

        val defaultNoSignalPowerOff = utilsModule.getPowerOffTime() as String

        val noSignalPowerOffEnabled = utilsModule.getIsPowerOffEnabled()

        var preferencesSetupInformation =
            PreferenceSetupInformation(
                subCategories,
                channelList,
                displayMode,
                defaultChannel,
                defaultChannelIndex,
                defaultDisplayMode,
                aspectRatioOptions,
                defaultAspectRatioOption!!,
                isInteractionChannelEnabled,
                audioTracks,
                //todo need to replace with default epg language
                audioTracks[0],
                noSignalPowerOffTimeList,
                defaultNoSignalPowerOff,
                noSignalPowerOffEnabled,
                utilsModule.getBlueMuteState(),
            )
        homeScene.preferencesSceneWidget?.refresh(preferencesSetupInformation)
    }

    private fun buildName(it: PrefSubMenu): String? {
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
           PrefSubMenu.OAD_UPDATE -> ConfigStringsManager.getStringById("over_air_update")
           PrefSubMenu.NO_SIGNAL_AUTO_POWER_OFF  -> ConfigStringsManager.getStringById("no_signal_power_off")
           PrefSubMenu.FIRST_LANGUAGE  -> ConfigStringsManager.getStringById(
               if(utilsModule.getRegion()==Region.US) "preferred_language" else "first_language") //for us region title is Preferred language and for other its first language
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
           PrefSubMenu.RRT5_LOCK -> ConfigStringsManager.getStringById("rrt5_lock")
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
           PrefSubMenu.DIGITAL_LANGUAGE  -> ConfigStringsManager.getStringById("digital_language")
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
            //ads targeting
           PrefSubMenu.ADS_TARGETING -> ConfigStringsManager.getStringById("ads_targeting")
            //Terms of Service
           PrefSubMenu.PRIVACY_POLICY -> ConfigStringsManager.getStringById("privacy_policy")
           PrefSubMenu.TERMS_OF_SERVICE -> ConfigStringsManager.getStringById("terms_of_service")
            //Feedback
            PrefSubMenu.FEEDBACK -> ConfigStringsManager.getStringById("feedback")

            PrefSubMenu.CAM_MENU -> ConfigStringsManager.getStringById("cam_menu")
            PrefSubMenu.USER_PREFERENCE -> ConfigStringsManager.getStringById("user_preference")
            PrefSubMenu.CAM_TYPE_PREFERENCE -> ConfigStringsManager.getStringById("cam_type_preference")
            PrefSubMenu.CAM_PIN -> ConfigStringsManager.getStringById("cam_pin")
            PrefSubMenu.CAM_SCAN -> ConfigStringsManager.getStringById("cam_scan")
            PrefSubMenu.CAM_OPERATOR -> ConfigStringsManager.getStringById("cam_op_profile")

            else -> it.name
        }
    }

    override fun onPreferencesDefaultChannelSelected(tvChannel: TvChannel) {
        var triplet = tvChannel.onId.toString() + "|" + tvChannel.tsId + "|" + tvChannel.serviceId
        utilsModule.setPrefsValue(ReferenceApplication.DEFAULT_CHANNEL,
            triplet)
    }

    override fun onDisplayModeSelected(selectedMode: Int) {
        /*if (ReferenceSdk.playerHandler != null) {
            val isAvailable: Boolean =
                (ReferenceSdk.playerHandler as ReferencePlayerHandler).isDisplayModeAvailable(
                    selectedMode
                )
            //TODO check if the selected display mode should be stored if it is not available
            utilsModule.setPrefsValue(
                ReferencePlayerHandler.PREF_DISPLAY_MODE,
                selectedMode
            )
            if (isAvailable && ReferenceSdk.playerHandler != null) {
                (ReferenceSdk.playerHandler as ReferencePlayerHandler).setDisplayMode(
                    selectedMode,
                    animate = false,
                    storeInPreference = true
                )
            }
        }*/
    }

    override fun getAvailableFavouriteChannels(callback: IAsyncDataCallback<ArrayList<TvChannel>>) {
        callback.onReceive(favoriteModule.getChannelList())
    }

    override fun onFavChannelClicked(tvChannel: TvChannel, favListIds: ArrayList<String>) {
        var favItem = FavoriteItem(
            tvChannel.id,
            FavoriteItemType.TV_CHANNEL,
            tvChannel.favListIds,
            tvChannel,
            favListIds
        )
        favoriteModule.updateFavoriteItem(favItem,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    homeScene.refreshFavouritesWidget()
                }

                override fun onSuccess() {
                    homeScene.refreshFavouritesWidget()
                    categoryModule.setActiveEpgFilter(HorizontalGuideSceneWidget.GUIDE_FILTER_ALL)
                }
            })
    }

    override fun changeFilterToAll(categoryName: String) {
        if (categoryName == categoryModule.getActiveCategory()) {
            favoriteModule.getFavoritesForCategory(categoryName, object : IAsyncDataCallback<ArrayList<FavoriteItem>> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(data: ArrayList<FavoriteItem>) {
                    if (data.size == 0 || !activeChannelBroadcast.favListIds.contains(categoryName)) {
                        categoryModule.setActiveCategory(ConfigStringsManager.getStringById("all"))
                    }
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getGuideEventsForTvChannel(
        additionalDayCount: Int,
        tvChannel: TvChannel,
        callback: IAsyncDataCallback<MutableList<TvEvent>>
    ) {
        fetchEventList(
            tvChannel,
            additionalDayCount,
            callback
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun fetchEventList(
        tvChannel: TvChannel,
        dayOffset: Int,
        callback: IAsyncDataCallback<MutableList<TvEvent>>
    ) {
       Thread {
           Log.d(Constants.LogTag.CLTV_TAG + TAG, "fetchEventList: ############# LOAD GUIDE FOR CHANNEL ${tvChannel.name} $dayOffset")
            val createFakeEvent = false
            var activeChannelIndex = tvModule.getDesiredChannelIndex(ApplicationMode.DEFAULT)
            var channelList = tvModule.getChannelList(ApplicationMode.DEFAULT)
            var isActiveChannel = false
            if (activeChannelIndex > 0 && activeChannelIndex < channelList.size) {
                var activeChannel = tvModule.getChannelList(ApplicationMode.DEFAULT)[activeChannelIndex]
                isActiveChannel = TvChannel.compare(tvChannel, activeChannel)
            }
           if(!::activeChannelBroadcast.isInitialized){
               getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                   override fun onFailed(error: Error) {}
                   override fun onReceive(data: TvChannel) {
                       activeChannelBroadcast = data
                   }
               })
           }
            //Set start time
            var calendar = Calendar.getInstance()
            calendar.time = Date(getCurrentTime(activeChannelBroadcast))
            calendar.add(Calendar.DATE, dayOffset)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            var filterStartDate = Date(calendar.time.time)

            calendar = Calendar.getInstance()
            calendar.time = Date(getCurrentTime(activeChannelBroadcast))
            calendar.add(Calendar.DATE, dayOffset)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            var filterEndDate = Date(calendar.time.time)



            Log.d(Constants.LogTag.CLTV_TAG +
                TAG,
                "getEPGGuideEventsForChannel: called filterStartDate $filterStartDate filterEndDate $filterEndDate"
            )
           epgModule.getEventListByChannelAndTime(
               tvChannel,
               filterStartDate.time,
               filterEndDate.time,
               object : IAsyncDataCallback<ArrayList<TvEvent>> {
                   override fun onReceive(eventList: ArrayList<TvEvent>) {
                       val activeChannelTime = Calendar.getInstance()
                       activeChannelTime.time.time = getCurrentTime(tvChannel)
                       activeChannelTime.set(Calendar.SECOND, 0)
                       activeChannelTime.set(Calendar.MILLISECOND, 0)

                       val channelTime = Calendar.getInstance()
                       channelTime.time.time = getCurrentTime(tvChannel)
                       channelTime.set(Calendar.SECOND, 0)
                       channelTime.set(Calendar.MILLISECOND, 0)

                     /*  if(activeChannelTime!=channelTime) {
                           eventList.clear()
                       }
                        */
                       val data= ArrayList<TvEvent>()
                       data.addAll(eventList)

                       val eventList = mutableListOf<TvEvent>()
                       val tempList = mutableListOf<TvEvent>()
                       tempList.addAll(data)
                       /*createFakeEvent =
                           tvChannel.inputId == "com.example.android.sampletvinput/.rich.RichTvInputService"*/

                       val calendar = Calendar.getInstance()
                       calendar.time = Date(getCurrentTime(tvChannel))
                       calendar.add(Calendar.DATE, dayOffset)
                       calendar.set(Calendar.HOUR_OF_DAY, 0)
                       calendar.set(Calendar.MINUTE, 0)
                       calendar.set(Calendar.SECOND, 0)
                       var startDate = Date(calendar.time.time)

                       calendar.set(Calendar.HOUR_OF_DAY, 23)
                       calendar.set(Calendar.MINUTE, 59)
                       calendar.set(Calendar.SECOND, 59)
                       calendar.set(Calendar.MILLISECOND,999)
                       var endDate = Date(calendar.time.time)

                       for (event in tempList) {
                           if ((startDate.time > event.startTime && startDate.time > event.endTime) || (endDate.time < event.startTime && endDate.time < event.endTime)) {
                               continue
                           }

                           if (Date(event.startTime).after(endDate)) {
                               continue
                           }
                           if (Date(event.endTime).before(startDate)) {
                               continue
                           }
                           eventList.add(event.clone())
                       }
                       if (eventList.isEmpty()) {

                           val calendar = Calendar.getInstance()
                           calendar.time = Date(getCurrentTime(tvChannel))
                           calendar.add(Calendar.DATE, dayOffset)
                           calendar.set(Calendar.HOUR_OF_DAY, 23)
                           calendar.set(Calendar.MINUTE, 59)
                           calendar.set(Calendar.SECOND, 59)
                           val noInformationEndDate = calendar.time.time
                           Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${startDate.time}")
                           eventList.add(
                               TvEvent.createNoInformationEvent(
                                   tvChannel,
                                   startDate.time,
                                   noInformationEndDate,
                                   getCurrentTime(tvChannel)
                               )
                           )
                                              }

                       if (createFakeEvent && eventList.isNotEmpty()) {
                           var firstEvent = eventList[0]
                           eventList.clear()
                           do {
                               var event = TvEvent(
                                   firstEvent.id,
                                   firstEvent.tvChannel,
                                   firstEvent.name,
                                   firstEvent.shortDescription,
                                   firstEvent.longDescription,
                                   firstEvent.imagePath,
                                   startDate.time,
                                   endDate.time,
                                   null,
                                   0,
                                   0,
                                   null,
                                   "",
                                   false,
                                   false,
                                   null,
                                   null,
                                   null
                               )
                               event.startTime = startDate.time
                               val duration = Random.nextInt(30, 120)
                               var d = startDate!!.time + (duration * 60000)
                               startDate = Date(d)
                               event.endTime = startDate!!.time
                               if (event.endTime> endDate.time) {
                                   event.endTime = endDate.time

                               }
                               eventList.add(event)
                           } while (event.startTime < endDate.time)
                       } else if (eventList.isNotEmpty() && !createFakeEvent) {
                           //Generate no information event at the first index
                           if (eventList[0].startTime > startDate.time) {
                               val endDate =eventList[0].startTime
                               eventList.add(
                                   0,
                                   TvEvent.createNoInformationEvent(
                                       tvChannel,
                                       startDate.time,
                                       endDate,
                                       getCurrentTime(activeChannelBroadcast)
                                   )
                               )
                           }
                           //Generate no information event at the last index
                           if (eventList[eventList.size - 1].endTime < endDate.time) {
                               eventList.add(
                                   TvEvent.createNoInformationEvent(
                                       tvChannel,
                                       eventList[eventList.size - 1].endTime,
                                       endDate.time,
                                       getCurrentTime(activeChannelBroadcast)
                                   )
                               )
                           }
                            //filling gap between two event with no information event
                            // ex: previous_event_end_time= 9.55 ,next_event_start_time = 10:00
                           // so there will be gap of 5 mins
                           var previousEvent: TvEvent? = null
                           eventList.toMutableList().forEach { event ->
                               if (previousEvent != null && event.startTime != previousEvent!!.endTime) {
                                   eventList.add(eventList.indexOf(event),
                                       TvEvent.createNoInformationEvent(
                                           tvChannel,
                                           previousEvent!!.endTime,
                                           event.startTime,
                                           getCurrentTime(activeChannelBroadcast)
                                       )
                                   )
                               }
                               previousEvent = event
                           }

                           val firstEvent = eventList.first()
                           val firstEventStartDate = Date(firstEvent.startTime)
                           val firstEventEndDate = Date(firstEvent.endTime)
                           if (firstEventStartDate.timezoneOffset == firstEventEndDate.timezoneOffset &&
                               firstEventStartDate.date == firstEventEndDate.date &&
                               firstEventStartDate.hours == firstEventEndDate.hours &&
                               firstEventStartDate.minutes == firstEventEndDate.minutes
                           ) {
                               eventList.removeFirst()
                           }

                           val lastEvent = eventList.last()
                           val lastEventStartDate = Date(lastEvent.startTime)
                           val lastEventEndDate = Date(lastEvent.endTime)
                           if (lastEventStartDate.timezoneOffset == lastEventEndDate.timezoneOffset &&
                               lastEventStartDate.date == lastEventEndDate.date &&
                               lastEventStartDate.hours == lastEventEndDate.hours &&
                               lastEventStartDate.minutes == lastEventEndDate.minutes
                           ) {
                               eventList.removeLast()
                           }
                       }

                       val firstItem = eventList.first()
                       val firstStartDate = Calendar.getInstance()
                       firstStartDate.time  = Date(getCurrentTime(activeChannelBroadcast))
                       firstStartDate.add(Calendar.DATE, dayOffset)
                       firstStartDate.set(Calendar.HOUR_OF_DAY, 0)
                       firstStartDate.set(Calendar.MINUTE, 0)
                       firstStartDate.set(Calendar.SECOND, 0)
                       firstStartDate.set(Calendar.MILLISECOND, 0)
                       if(firstItem.startTime > firstStartDate.time.time)
                           firstItem.startTime = firstStartDate.time.time

                       val lastItem = eventList.last()
                       val lastEndDate = Calendar.getInstance()
                       lastEndDate.time = Date(getCurrentTime(activeChannelBroadcast))
                       lastEndDate.add(Calendar.DATE, dayOffset)
                       lastEndDate.set(Calendar.HOUR_OF_DAY, 23)
                       lastEndDate.set(Calendar.MINUTE, 59)
                       lastEndDate.set(Calendar.SECOND, 59)
                       lastEndDate.set(Calendar.MILLISECOND, 999)
                       if(lastItem.endTime <lastEndDate.time.time)
                           lastItem.endTime = lastEndDate.time.time

                        //Add tune to channel for more information event
                        if (com.iwedia.cltv.BuildConfig.FLAVOR.contains("mtk") &&
                            !isActiveChannel && eventList.size == 1 && eventList[0].name == ConfigStringsManager.getStringById("no_information")) {
                            eventList.clear()
                            eventList.add(
                                TvEvent.createTuneToChannelForMoreInformationEvent(
                                    tvChannel,
                                    startDate.time,
                                    endDate.time,
                                    getCurrentTime(tvChannel),
                                    ConfigStringsManager.getStringById("tune_to") + " ${tvChannel.name} " +
                                            ConfigStringsManager.getStringById("channel_for_more_info")
                                )
                            )
                        }

                        callback.onReceive(eventList)
                    }

                   override fun onFailed(error: Error) {
                       callback.onFailed(error)
                   }

               })
        }.start()

    }

    override fun getChannelsOfSelectedFilter(): MutableList<TvChannel> {
        return mGuideChannelListForSelectedFilter
    }

    override fun getEventsForChannels(anchorChannel: TvChannel, filterId: Int, callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>, dayOffset: Int, additionalDayOffset: Int, isExtend:Boolean){
        categoryModule.getAvailableFilters(object :IAsyncDataCallback<ArrayList<Category>>{
            override fun onFailed(error: Error) {
                callback.onFailed(Error(error.message))
            }

            override fun onReceive(data: ArrayList<Category>) {
                mGuideFilterList = data
                mGuideChannelListForSelectedFilter.clear()
                var activeFilter = mGuideFilterList!![filterId]
                var channelsListToLoad = mutableListOf<TvChannel>()
                channelList = tvModule.getChannelList()

                categoryModule.filterChannels(
                    activeFilter,
                    object : IAsyncDataCallback<ArrayList<TvChannel>> {
                        override fun onFailed(error: Error) {
                            callback.onFailed(Error(error.message))
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onReceive(data: ArrayList<TvChannel>) {
                            val filterChannelList = utilsModule.sortListByNumber(data)
                            try {
                                if (filterChannelList.size == 0) {
                                    callback.onFailed(java.lang.Error("No channel found in the filter"))
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: channel lise size is 0")
                                    return
                                }

                                var list: MutableList<TvChannel> = mutableListOf()
                                list.addAll(filterChannelList)

                                list.forEach {
                                    var isAlreadyHave = false
                                    for (channel in mGuideChannelListForSelectedFilter) {
                                        if (channel.id == it.id) {
                                            isAlreadyHave = true
                                            break
                                        }
                                    }
                                    if (!isAlreadyHave) {
                                        if (it.isBrowsable || it.inputId.contains("iwedia") || it.inputId.contains(
                                                "sampletvinput"
                                            )
                                        ) {
                                            mGuideChannelListForSelectedFilter.add(it)
                                        }
                                    }
                                }
                                var posAnchorChannel = 0
                                for (i in 0 until mGuideChannelListForSelectedFilter.size) {
                                    if (mGuideChannelListForSelectedFilter[i].id == anchorChannel.id) {
                                        posAnchorChannel = i
                                    }
                                }
                                var counter = 0
                                var posForNextChannels = posAnchorChannel
                                if (posAnchorChannel == -1) {
                                    posAnchorChannel =
                                        if (mGuideChannelListForSelectedFilter.lastIndex >= 5) {
                                            5
                                        } else {
                                            mGuideChannelListForSelectedFilter.lastIndex
                                        }
                                    posForNextChannels = posAnchorChannel
                                }
                                if (hideLockedServices) {
                                    while (counter != 6 && posAnchorChannel >= 0) {
                                        var serviceAddedOrExists = false
                                        if (!channelsListToLoad.contains(
                                                mGuideChannelListForSelectedFilter[posAnchorChannel]
                                            )
                                        ) {
                                            if (!mGuideChannelListForSelectedFilter[posAnchorChannel].isLocked) {
                                                channelsListToLoad.add(
                                                    mGuideChannelListForSelectedFilter[posAnchorChannel]
                                                )
                                                serviceAddedOrExists = true
                                            }
                                        } else {
                                            serviceAddedOrExists = true
                                        }

                                        if (serviceAddedOrExists) {
                                            counter++
                                        }
                                        posAnchorChannel--
                                    }

                                    channelsListToLoad.reverse()
                                    counter = 0
                                    try {
                                        while (counter != 6 && posForNextChannels < mGuideChannelListForSelectedFilter.size) {
                                            var serviceAddedOrExists = false
                                            if (!channelsListToLoad.contains(
                                                    mGuideChannelListForSelectedFilter[posForNextChannels]
                                                )
                                            ) {
                                                if (!mGuideChannelListForSelectedFilter[posForNextChannels].isLocked) {
                                                    channelsListToLoad.add(
                                                        mGuideChannelListForSelectedFilter[posForNextChannels]
                                                    )
                                                    serviceAddedOrExists = true
                                                }
                                            } else {
                                                serviceAddedOrExists = true
                                            }
                                            if (serviceAddedOrExists) {
                                                counter++
                                            }
                                            posForNextChannels++
                                        }
                                    } catch (e: java.lang.Exception) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, " exception: $e ")
                                    }
                                } else {
                                    while (counter != 6 && posAnchorChannel >= 0) {
                                        if (!channelsListToLoad.contains(
                                                mGuideChannelListForSelectedFilter[posAnchorChannel]
                                            )
                                        )
                                            channelsListToLoad.add(
                                                mGuideChannelListForSelectedFilter[posAnchorChannel]
                                            )

                                        posAnchorChannel--
                                        counter++
                                    }

                                    channelsListToLoad.reverse()
                                    counter = 0
                                    try {
                                        while (counter != 6 && posForNextChannels < mGuideChannelListForSelectedFilter.size) {
                                            if (!channelsListToLoad.contains(
                                                    mGuideChannelListForSelectedFilter[posForNextChannels]
                                                )
                                            )
                                                channelsListToLoad.add(
                                                    mGuideChannelListForSelectedFilter[posForNextChannels]
                                                )
                                            posForNextChannels++
                                            counter++
                                        }
                                    } catch (e: java.lang.Exception) {

                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "  exception: $e ")
                                    }
                                }
                                getEventsByChannelList(object :
                                    IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>> {
                                    override fun onReceive(data: LinkedHashMap<Int, MutableList<TvEvent>>) {
                                        callback.onReceive(data)
                                    }

                                    override fun onFailed(error: Error) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "showEpgData onFailed: getEventsByChannelList")
                                    }
                                }, channelsListToLoad, dayOffset, additionalDayOffset, isExtend)
                            }catch (e: Exception){
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: exception $e")
                                callback.onFailed(Error(e))
                            }
                    }
                })
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun loadNextEpgData(anchorChannel: TvChannel, callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>, dayOffset: Int, additionalDayOffset: Int, isExtend:Boolean) {
        var channelsListToLoad = mutableListOf<TvChannel>()
        var posAnchorChannel = if (mGuideChannelListForSelectedFilter.indexOf(anchorChannel) == -1) 0 else mGuideChannelListForSelectedFilter.indexOf(anchorChannel)

        for (i in 0 until mGuideChannelListForSelectedFilter.size){
            if (mGuideChannelListForSelectedFilter[i].id== anchorChannel.id){
                posAnchorChannel = i
            }
        }

        var counter = 0
        while (counter!=10 && posAnchorChannel< mGuideChannelListForSelectedFilter.size){
            if (!channelsListToLoad.contains(mGuideChannelListForSelectedFilter[posAnchorChannel]))
                channelsListToLoad.add(mGuideChannelListForSelectedFilter[posAnchorChannel])

            posAnchorChannel++
            counter++
        }
        getEventsByChannelList(object : IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>> {

            override fun onFailed(error: Error) {}

            override fun onReceive(data: LinkedHashMap<Int, MutableList<TvEvent>>) {
                callback.onReceive(data)
            }
        }, channelsListToLoad, dayOffset, additionalDayOffset, isExtend)
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun loadPreviousChannels(anchorChannel: TvChannel, callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>, dayOffset: Int, additionalDayOffset: Int, isExtend:Boolean){

        var channelsListToLoad = mutableListOf<TvChannel>()
        var posAnchorChannel = if (mGuideChannelListForSelectedFilter.indexOf(anchorChannel) == -1) 0 else mGuideChannelListForSelectedFilter.indexOf(anchorChannel)

        for (i in 0 until mGuideChannelListForSelectedFilter.size){
            if (mGuideChannelListForSelectedFilter[i].id== anchorChannel.id){
                posAnchorChannel = i
            }
        }
        var counter = 0
        while (counter!=10 && posAnchorChannel>=0){
            if (!channelsListToLoad.contains(mGuideChannelListForSelectedFilter[posAnchorChannel]))
                channelsListToLoad.add(mGuideChannelListForSelectedFilter[posAnchorChannel])

            posAnchorChannel--
            counter++
        }
        channelsListToLoad.reverse()
        getEventsByChannelList(object :IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>> {
            override fun onFailed(error: Error) {
            }


            override fun onReceive(data: LinkedHashMap<Int, MutableList<TvEvent>>) {
                callback.onReceive(data)
            }

        }, channelsListToLoad, dayOffset, additionalDayOffset, isExtend)

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun fetchEventForChannelList(callback: IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>>, channelList: MutableList<TvChannel>, dayOffset: Int, additionalDayOffset: Int, isExtend: Boolean){

        var channelEventListMap = ConcurrentHashMap<Int, MutableList<TvEvent>>()
        var counter = AtomicInteger(0)

        channelList.forEach {tvChannel ->TvChannel
            fetchEventList(
                tvChannel,
                dayOffset,
                object : IAsyncDataCallback<MutableList<TvEvent>> {

                    override fun onReceive(eventList: MutableList<TvEvent>) {
                        if (isExtend) {
                            fetchEventList(
                                tvChannel,
                                additionalDayOffset,
                                object : IAsyncDataCallback<MutableList<TvEvent>> {
                                    override fun onFailed(error: Error) {

                                    }

                                    override fun onReceive(additionalEventList: MutableList<TvEvent>) {
                                        val leftEventList =
                                            if (dayOffset > additionalDayOffset) additionalEventList else eventList
                                        val rightEventList =
                                            if (dayOffset > additionalDayOffset) eventList else additionalEventList
                                        if (rightEventList.first().id == leftEventList.last().id && rightEventList.first().id != -1) {
                                            var event = leftEventList.last()
                                            event.endTime =
                                                rightEventList.first().endTime
                                            leftEventList.removeLast()
                                            rightEventList.removeFirst()
                                            rightEventList.add(0, event)
                                        }

                                        leftEventList.addAll(rightEventList)

                                        channelEventListMap[tvChannel.id] = leftEventList

                                        val vai = counter.incrementAndGet()
                                        if (vai == channelList.size) {

                                            var map = LinkedHashMap<Int, MutableList<TvEvent>>()
                                            for (index in 0 until channelList.size) {
                                                var tvChannel = channelList[index]
                                                if (channelEventListMap.keys.contains(tvChannel.id)) {
                                                    map[index] = channelEventListMap[tvChannel.id]!!
                                                }
                                            }
                                            callback.onReceive(map)
                                        }
                                    }

                                })
                        } else {
                            channelEventListMap[tvChannel.id] = eventList
                            val vai = counter.incrementAndGet()
                            if (vai == channelList.size) {
                                var map = LinkedHashMap<Int, MutableList<TvEvent>>()

                                for (index in 0 until channelList.size) {
                                    var tvChannel = channelList[index]
                                    if (channelEventListMap.keys.contains(tvChannel.id)) {
                                        map[index] = channelEventListMap[tvChannel.id]!!
                                    }
                                }
                                callback.onReceive(map)
                            }

                        }
                    }

                    override fun onFailed(error: Error) {}
                })
        }
    }

    override fun showDetails(item: Any) {
        if (item is TvEvent && item.tag == "guide") {
            val intent = Intent(FastZapBannerDataProvider.FAST_SHOW_GUIDE_INTENT)
            context?.sendBroadcast(intent)
            return
        }

        if (homeScene.getActiveCategory() == 0 && !isLockedScreenShown()) {
            unMuteAudio()
        }
        if (item is TvChannel && !isRegionSupported()) {
            playTvChannel(item as TvChannel)
            return
        }

        context!!.runOnUiThread {

            worldHandler!!.triggerAction(id, Action.HIDE)

            val sceneData = SceneData(id, instanceId, item)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DETAILS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }

        if(((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()){
            muteAudio()
        }
    }

    override fun playTvChannel(tvChannel: TvChannel) {
        //Set application mode 0 for DEFAULT mode
        if(!((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()) utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal)

        if ((homeScene.getActiveCategory() == 1 && !isRegionSupported())
                    || (homeScene.getActiveCategory() == 2 && isRegionSupported())) { // To checking current active widget is guide

            categoryModule.setActiveCategory(homeScene.getEpgActiveFilter().name)
            activeCategoryId = homeScene.getEpgActiveFilter().id
            //Reset the next/prev channel zap order to selected list.
            var favGroupName = ""
            var tifCategoryName = ""
            var genreCategoryName = ""
            if (activeCategoryId == FilterItem.FAVORITE_ID) {
                favGroupName = categoryModule.getActiveCategory()
            } else if (activeCategoryId >= FilterItem.TIF_INPUT_CATEGORY && activeCategoryId < FilterItem.FAVORITE_ID) {
                tifCategoryName = categoryModule.getActiveCategory()
            } else if (activeCategoryId == FilterItem.GENRE_CATEGORY) {
                genreCategoryName = categoryModule.getActiveCategory()
            }
            tvModule.updateLaunchOrigin(activeCategoryId, favGroupName, tifCategoryName, genreCategoryName)
        }
        if (timeShiftModule.isTimeShiftActive) showTimeShiftChannelChangeDialog(tvChannel, false)
        else changeChannel(tvChannel)
    }

    private fun changeChannel(tvChannel: TvChannel) {
        tvModule.changeChannel(tvChannel,object :IAsyncCallback{
            override fun onFailed(error: Error) {
                if (error.message != "t56") {
                    context!!.runOnUiThread {
                        showToast("Failed to start ${tvChannel.name} playback")
                    }
                }
            }

            override fun onSuccess() {
                backToLive()
                if (worldHandler?.active?.id != ReferenceWorldHandler.SceneId.ZAP_BANNER) {
                    if (ReferenceApplication.worldHandler!!.playbackState != ReferenceWorldHandler.PlaybackState.RECORDING) {
                        ReferenceApplication.runOnUiThread {
                            ReferenceApplication.worldHandler!!.active?.let {
                                var sceneData =
                                    SceneData(it.id, it.instanceId, tvChannel)
                                worldHandler!!.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                    Action.SHOW_OVERLAY,
                                    sceneData
                                )
                            }
                        }
                    }
                }
            }
        }, applicationMode = ApplicationMode.DEFAULT)
    }

    private fun showTimeShiftChannelChangeDialog(tvChannel: TvChannel, isFastChannel: Boolean) {
        context!!.runOnUiThread {
            val sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {}

                override fun onPositiveButtonClicked() {
                    timeShiftModule.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {}
                    })
                    ReferenceApplication.worldHandler!!.playbackState =
                        ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE

                    ReferenceApplication.worldHandler!!.destroyOtherExisting(
                        ReferenceWorldHandler.SceneId.LIVE
                    )
                    timeShiftModule.setTimeShiftIndication(false)
                    if (isFastChannel) tuneToFastChannel(tvChannel)
                    else changeChannel(tvChannel)
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW, sceneData
            )
        }
    }

    override fun onRecordingClick(recording: Any) {
        worldHandler!!.triggerAction(id, Action.HIDE)
        val sceneData = SceneData(id, instanceId, recording)
        worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DETAILS_SCENE,
            Action.SHOW_OVERLAY, sceneData
        )
    }

    override fun onGuideWatchlistClicked(tvEvent: TvEvent): Boolean {
        var isWatchlistUpdated = false
        watchlistModule.hasScheduledReminder(
            tvEvent,
            object : IAsyncDataCallback<Boolean> {
                override fun onFailed(error: Error) {
                    showToast(ConfigStringsManager.getStringById("recording_reminder_conflict_toast"))
                }

                override fun onReceive(data: Boolean) {
                    if (data) {
                           watchlistModule.removeScheduledReminder(
                            com.iwedia.cltv.platform.model.recording.ScheduledReminder(
                                tvEvent.id,
                                tvEvent.name,
                                tvEvent.tvChannel,
                                tvEvent,
                                tvEvent.startTime,
                                tvEvent.tvChannel.id,
                                tvEvent.id
                            ), object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                }


                                @RequiresApi(Build.VERSION_CODES.R)
                                override fun onSuccess() {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: removeScheduledReminder")
                                     Timer().schedule(timerTask {
                                         homeScene.refreshForYou()
                                       }, 200)
                                    homeScene.guideSceneWidget!!.refreshWatchlistButton()
                                    isWatchlistUpdated = true
                                }
                            })
                    }
                    else {
                        if ((TextUtils.isEmpty(tvEvent.name)
                                    || tvEvent.name.equals(ConfigStringsManager.getStringById("no_information")))
                            && (TextUtils.isEmpty(tvEvent.longDescription)
                                    || tvEvent.longDescription.equals(
                                ConfigStringsManager.getStringById(
                                    "no_information"
                                )
                            ))
                            && (TextUtils.isEmpty(tvEvent.shortDescription))
                            || tvEvent.shortDescription.equals(ConfigStringsManager.getStringById("no_information"))
                        ) {

                            Log.w(
                                "TAG",
                                "ReferenceWatchlistHandler : \n Watch List name [${tvEvent.name} ] " +
                                        " \n shortDescription  [${tvEvent.shortDescription} ]" +
                                        " \n longDescription  [${tvEvent.longDescription} ]"
                            )

                            showToast(ConfigStringsManager.getStringById("watchlist_scheduled_fail_toast"))

                        } else {
                            watchlistModule.scheduleReminder(
                                com.iwedia.cltv.platform.model.recording.ScheduledReminder(
                                    tvEvent.id,
                                    tvEvent.name,
                                    tvEvent.tvChannel,
                                    tvEvent,
                                    tvEvent.startTime,
                                    tvEvent.tvChannel.id,
                                    tvEvent.id
                                ), object : IAsyncCallback {
                                    override fun onFailed(error: Error) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: scheduleReminder")
                                        if (error.message == "alreadyPresent") {
                                            showToast(ConfigStringsManager.getStringById("favorite_list_already_exist_2"))
                                        } else if (error.message == "conflict") {
                                            showToast(ConfigStringsManager.getStringById("watchlist_recording_conflict"))
                                        } else {
                                            Log.d(Constants.LogTag.CLTV_TAG + "HomeSceneManager", "onFailed: Reminder failed")
                                        }
                                    }

                                    @RequiresApi(Build.VERSION_CODES.R)
                                    override fun onSuccess() {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: scheduleReminder")
                                        Timer().schedule(timerTask {
                                            homeScene.refreshForYou()
                                        }, 200)
                                        homeScene.guideSceneWidget!!.refreshWatchlistButton()
                                        isWatchlistUpdated = true
                                    }
                                })
                        }
                    }
                }
            })
        return isWatchlistUpdated
    }

    override fun onChannelsScanClicked() {
        settingsOpened = true

        try {
            //Try triggering SCAN SCREEN from custom settings
            var intent = Intent()
            if (BuildConfig.FLAVOR == "rtk")
                intent = Intent("android.settings.CHANNEL_SCAN_SETTINGS")
            else if (BuildConfig.FLAVOR == "refplus5" || BuildConfig.FLAVOR == "mal_service") {
                intent.setClassName(SETTINGS_PACKAGE, SETTINGS_CHANNEL_ACTIVITY)
            } else {
                intent = Intent("android.settings.CHANNELS_SETTINGS")
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChannelsScanClicked: ${e.printStackTrace()}")
        }
    }

    override fun kidsModeEnabled(): Boolean{
        return !utilsModule.kidsModeEnabled()
    }

    override fun onChannelsSettingsClicked() {
        settingsOpened = true

        try {
            //Try triggering SCAN SCREEN from custom settings
            var intent = Intent()
            if (BuildConfig.FLAVOR == "rtk") {
                intent = Intent("android.settings.CHANNEL_SCAN_SETTINGS")
            } else if (BuildConfig.FLAVOR == "refplus5" || BuildConfig.FLAVOR == "mal_service") {
                intent.setClassName(SETTINGS_PACKAGE, SETTINGS_CHANNEL_ACTIVITY)
            } else {
                intent = Intent("android.settings.CHANNELS_SETTINGS")
                (ReferenceApplication.getActivity() as MainActivity).settingsIconClicked = true
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChannelsSettingsClicked: ${e.printStackTrace()}")
        }
    }

    override fun onChannelsEditClicked() {
        settingsOpened = true

        try {
            //Try triggering CHANNEL EDIT SETTINGS from custom settings
            val intent = Intent("android.settings.CHANNEL_EDIT_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onChannelsEditClicked: ${e.printStackTrace()}")
        }
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

    private fun startUpdateSystemInformationTimerTask() {
        if (updateSystemInformationTimer == null) {
            updateSystemInformationTimer = Timer()
        }
        updateSystemInformationTask = object : TimerTask() {
            override fun run() {
                ReferenceApplication.runOnUiThread(Runnable { getSystemInformation() })
            }
        }
        updateSystemInformationTimer?.scheduleAtFixedRate(
            updateSystemInformationTask,
            0,
            (3 * 1000).toLong()
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getAdsTargetingData(type: PrefType) {

        val prefAdsTargeting = preferenceModule.getPreferenceSubMenus(PrefMenu.ADS_TARGETING, type)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        prefAdsTargeting.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }

        var adsTargetingEnabled = false

        fastUserSettingsModule.getDnt(object: IAsyncDataCallback<Int>{
                override fun onFailed(error: Error) {
        }

        override fun onReceive(data: Int) {
            when (data) {
                0 -> adsTargetingEnabled = false
                1 -> adsTargetingEnabled = true
            }
        }
    })

        val adsTargetingInformation = PreferencesAdsTargetingInformation(
            subCategories,
            adsTargetingEnabled,
            hasInternet()
        )

        homeScene.preferencesSceneWidget?.refresh(adsTargetingInformation)
    }

    override fun getTermsOfServiceData(type: PrefType) {
        val prefTermsOfService = preferenceModule.getPreferenceSubMenus(PrefMenu.TERMS_OF_SERVICE, type)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()
        prefTermsOfService.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }
        val termsOfServiceInformation = PreferencesTermsOfServiceInformation(subCategories)
        homeScene.preferencesSceneWidget?.refresh(termsOfServiceInformation)
    }

    override fun onClickTermsOfService() {
        openWebView(FastTosOptInHelper.getTosUrl())
    }

    override fun onClickPrivacyPolicy() {
        openWebView(FastTosOptInHelper.getPrivacyPolicyUrl())
    }

    override fun tuneToFocusedChannel(tvChannel: TvChannel) {
        playerModule.performBackgroundTuning(tvChannel)
    }

    override fun setActiveWindow(tvChannelList: MutableList<TvChannel>, startTime: Long) {
        epgModule.setActiveWindow(tvChannelList, startTime)
    }

    override fun clearActiveWindow() {
        epgModule.clearActiveWindow()
    }

    override fun getStartTimeForActiveWindow(): Long {
        return epgModule.getStartTimeForActiveWindow()
    }

    private fun openWebView(url: String) {
        if (checkNetworkConnection()) {
            val sceneData = SceneData(id, instanceId, url)
            ReferenceApplication.worldHandler?.triggerActionWithData(
                ReferenceWorldHandler.SceneId.CUSTOM_WEB_VIEW_SCENE,
                Action.SHOW, sceneData
            )
        }
    }

    override fun getSystemInformation() {

        var prefix = "0x"

        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onReceive(activeTvChannel: TvChannel) {
               utilsModule.getSystemInfoData(activeTvChannel, object : IAsyncDataCallback<SystemInfoData> {
                   override fun onReceive(systemData: SystemInfoData) {
                       if (systemData == null) {
                           Log.e(Constants.LogTag.CLTV_TAG +"HomeSceneManager", "Unable to get system data")
                           return
                       }

                       val signalTSID: String = prefix.plus(activeTvChannel.tsId.toString(16))
                       val signalONID: String = prefix.plus(activeTvChannel.onId.toString(16))
                       val signalServiceId: String = prefix.plus(activeTvChannel.serviceId.toString(16))
                       val signalNetworkId: String = prefix.plus(systemData.networkId.toString(16))

                       val rfKey: Int = systemData.frequency / 1000
                       val cnlRF = if (RFChannelNumbers.containsKey(rfKey)) RFChannelNumbers[rfKey].toString() else "N/A"
                       val frequency = if(activeTvChannel.tunerType == TunerType.SATELLITE_TUNER_TYPE) systemData.frequency else systemData.frequency / 1000000
                       var displayNumber = "0"
                       if(::activeChannelBroadcast.isInitialized) {
                           displayNumber = activeChannelBroadcast.displayNumber
                       } else {
                           displayNumber = activeTvChannel.displayNumber
                           activeChannelBroadcast = activeTvChannel
                       }

                       val systemInformationData = ReferenceSystemInformation(
                           cnlRF,
                           systemData.signalBer.toString(),
                           frequency.toString(),
                           displayNumber,
                           systemData.signalUEC.toString(),
                           signalServiceId,
                           systemData.postViterbi ?: "",
                           signalTSID,
                           systemData.attr5s ?: "",
                           signalONID,
                           systemData.signalAGC.toString(),
                           signalNetworkId,
                           systemData.networkName ?: "",
                           systemData.bandwidth ?: "",
                           systemData.signalStrength,
                           systemData.signalQuality
                       )
                       ReferenceApplication.runOnUiThread{
                           homeScene.preferencesSceneWidget?.refresh(systemInformationData)
                       }
                   }

                   override fun onFailed(error: Error) {
                       Log.e(Constants.LogTag.CLTV_TAG +"HomeSceneManager", "Unable to get system data")
                       return
                   }
               })
            }

            override fun onFailed(error: Error) {
            }
        })

        if (updateSystemInformationTimer == null) {
            startUpdateSystemInformationTimerTask()
        }
    }

    override fun onPause() {

        val isFastOnly = ((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()

        // ensure that timer in FastHomeData is stopped and audio is NOT muted
        // when tab is not discover or on demand
        if (this::activeChannel.isInitialized) {
            if (!activeChannel.isLocked && isCurrentInputBlocked == false
                && !(homeScene.getActiveCategory() == 0) && !playerModule.isParentalActive
                && ((homeScene.getActiveCategory() != 2 && isFastOnly) ||
                        (homeScene.getActiveCategory() != 3 && !isFastOnly))) {
                unMuteAudio()
            }
        }
        homeScene.forYouWidget?.stopRefreshTimer() // important for stopping triggering audio muting
        //---------------------------------------------

        isCurrentInputBlocked = false
        super.onPause()
        if (updateSystemInformationTask != null) {
            updateSystemInformationTask!!.cancel()
            updateSystemInformationTask = null
        }

        if (updateSystemInformationTimer != null) {
            updateSystemInformationTimer?.cancel()
            updateSystemInformationTimer?.purge()
            updateSystemInformationTimer = null
        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getAudioInformation(type: PrefType) {
        val prefAudioOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.AUDIO, type)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        prefAudioOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }
        val audioTracks: MutableList<LanguageCode> = mutableListOf()
        utilsModule.getLanguageMapper()!!.preferredLanguageCodes.forEach { item ->
            if (utilsModule.getRegion()==Region.US){
                when(item.languageCodeISO6392){
                    "eng","fra","spa"->audioTracks.add(item)
                }
            }else{
                audioTracks.add(item)
            }
        }

        var faderControlStrings = java.util.ArrayList<String>()
        faderControlStrings.add(ConfigStringsManager.getStringById("fader_main_max"))
        faderControlStrings.add(ConfigStringsManager.getStringById("fader_main_sub"))
        faderControlStrings.add(ConfigStringsManager.getStringById("fader_equal"))
        faderControlStrings.add(ConfigStringsManager.getStringById("fader_ad_sub"))
        faderControlStrings.add(ConfigStringsManager.getStringById("fader_ad_max"))

        var defFadeValueIndex = utilsModule.getDefFaderValue()

        var visuallyImpairedAudioList = tvModule.getVisuallyImpairedAudioTracks()
        var defAudioForVisuallyImpairedIndex = utilsModule.getAudioForVi()
        var audioTypeStrings = mutableListOf<String>()
        var audioFormat = mutableListOf("Stereo", "Multi Channel")//TODO need to get this from api it is temporary
        var selectedFormat = audioFormat[0] //todo this is temp

        var isHearingImpairedEnabled = false
        var isAudioDescriptionEnabled = false
        var audioTypeSelected = 0
        var viVolumeValue = utilsModule.getVolumeValue()
        var viSpeakerStatus = utilsModule.getAudioSpeakerState()
        var viHeadPhoneStatus = utilsModule.getAudioHeadphoneState()
        var viPaneFadeStatus = utilsModule.getAudioPaneState()


        audioTypeStrings.add(ConfigStringsManager.getStringById("audio_type_normal"))
        audioTypeStrings.add(ConfigStringsManager.getStringById("audio_type_visually_impaired"))

        var preferencesAudioInformation =
            PreferenceAudioInformation(
                subCategories,
                audioTracks,
                audioTypeSelected,
                isAudioDescriptionEnabled,
                isHearingImpairedEnabled,
                audioTypeStrings,
                audioFormat,
                selectedFormat,
                faderControlStrings,
                defFadeValueIndex,
                visuallyImpairedAudioList,
                defAudioForVisuallyImpairedIndex,
                viVolumeValue,
                viSpeakerStatus,
                viHeadPhoneStatus,
                viPaneFadeStatus
            )
        ensureAudioLanguages(
            language = utilsModule.getLanguageMapper()!!.getDefaultLanguageCode().languageCodeISO6392,
            type = type

        )
        homeScene.preferencesSceneWidget?.refresh(preferencesAudioInformation)

        var refreshCondition = AtomicInteger(0)
        utilsModule.getAudioType(object : IAsyncDataCallback<Int> {
            override fun onReceive(data: Int) {
                audioTypeSelected = data
                preferencesAudioInformation.audioTypeSelected = audioTypeSelected
                if (refreshCondition.incrementAndGet() >= 3) {
                    ReferenceApplication.runOnUiThread{
                        homeScene.preferencesSceneWidget?.refresh(preferencesAudioInformation)
                    }
                }
            }

            override fun onFailed(error: Error) {
                if (refreshCondition.incrementAndGet() >= 3) {
                    ReferenceApplication.runOnUiThread{
                        homeScene.preferencesSceneWidget?.refresh(preferencesAudioInformation)
                    }
                }
            }
        })

        utilsModule.getAudioDescriptionEnabled(object: IAsyncDataCallback<Boolean> {
            override fun onReceive(data: Boolean) {
                isAudioDescriptionEnabled = data
                preferencesAudioInformation.isAudioDescriptionEnabled = isAudioDescriptionEnabled
                if (refreshCondition.incrementAndGet() >= 3) {
                    ReferenceApplication.runOnUiThread{
                        homeScene.preferencesSceneWidget?.refresh(preferencesAudioInformation)
                    }
                }
            }

            override fun onFailed(error: Error) {
                if (refreshCondition.incrementAndGet() >= 3) {
                    ReferenceApplication.runOnUiThread{
                        homeScene.preferencesSceneWidget?.refresh(preferencesAudioInformation)
                    }
                }
            }
        })

        utilsModule.getHearingImpairedEnabled(object: IAsyncDataCallback<Boolean> {
            override fun onReceive(data: Boolean) {
                isHearingImpairedEnabled = data
                preferencesAudioInformation.isHearingImpairedEnabled = isHearingImpairedEnabled
                if (refreshCondition.incrementAndGet() >= 3) {
                    ReferenceApplication.runOnUiThread{
                        homeScene.preferencesSceneWidget?.refresh(preferencesAudioInformation)
                    }
                }
            }

            override fun onFailed(error: Error) {
                if (refreshCondition.incrementAndGet() >= 3) {
                    ReferenceApplication.runOnUiThread{
                        homeScene.preferencesSceneWidget?.refresh(preferencesAudioInformation)
                    }
                }
            }
        })
    }

    override fun setAudioDescriptionEnabled(enable: Boolean) {
        utilsModule.setAudioDescriptionEnabled(enable)
    }

    override fun setHearingImpairedEnabled(enable: Boolean) {
        utilsModule.setHearingImpairedEnabled(enable)
    }

    override fun getAudioFirstLanguage(type: PrefType): LanguageCode? {
        val firstLanguage = utilsModule.getPrimaryAudioLanguage(type)
        if (!firstLanguage.isNullOrEmpty()) {
            utilsModule.getLanguageMapper()!!.getLanguageCodes().forEach { item ->
                if (item.languageCodeISO6391 == firstLanguage || item.languageCodeISO6392 == firstLanguage) {
                    return LanguageCode(
                        firstLanguage,
                        "",
                        utilsModule.getLanguageMapper()!!.getLanguageName(firstLanguage)!!,
                        "",
                        ""
                    )
                }
            }

        }
        return null
    }

    override fun onAudioFirstLanguageClicked(languageCode: LanguageCode, type: PrefType) {
        utilsModule.setPrimaryAudioLanguage(languageCode.languageCodeISO6392, type)
    }

    override fun getAudioSecondLanguage(type: PrefType): LanguageCode? {
        val secondLanguage = utilsModule.getSecondaryAudioLanguage(type)
        if (!secondLanguage.isNullOrEmpty()) {

            utilsModule.getLanguageMapper()!!.getLanguageCodes().forEach { item ->
                if (item.languageCodeISO6391 == secondLanguage || item.languageCodeISO6392 == secondLanguage) {
                    return LanguageCode(
                        secondLanguage,
                        "",
                        utilsModule.getLanguageMapper()!!.getLanguageName(secondLanguage)!!,
                        "",
                        ""
                    )
                }
            }
        }
        return null
    }

    override fun onAudioSecondLanguageClicked(languageCode: LanguageCode, type: PrefType) {
        utilsModule.setSecondaryAudioLanguage(languageCode.languageCodeISO6392, type)
    }

    override fun onAudioTypeClicked(position: Int) {
        utilsModule.setAudioType(position)
    }

    override fun onFaderValueChanged(newValue: Int) {
        utilsModule.setFaderValue(newValue)
    }

    override fun onAudioViValueChanged(newValue: Int) {
        utilsModule.setVisuallyImpairedAudioValue(newValue)
    }

    override fun onVisuallyImpairedValueChanged(position: Int, enabled: Boolean) {
        utilsModule.setVisuallyImpairedValues(position, enabled)
    }

    override fun onVolumeChanged(newVolumeValue: Int) {
        utilsModule.setVolumeValue(newVolumeValue)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getSubtitleInformation(type: PrefType) {

        val prefSubtitleOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.SUBTITLE, type)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        prefSubtitleOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }
        val audioTracks: MutableList<LanguageCode> = mutableListOf()
        utilsModule.getLanguageMapper()!!.preferredLanguageCodes.forEach { item ->
            audioTracks.add(item)
        }

        val subtitleTracks: MutableList<LanguageCode> = mutableListOf()
        utilsModule.getLanguageMapper()!!.preferredLanguageCodes.forEach { item ->
            subtitleTracks.add(item)
        }

        val subtitlesType = utilsModule.getSubtitlesType()
        val subtitleTypeStrings = utilsModule.getSubtitleTypeDisplayNames(type)
        val isClosedCaptionEnabled = mCaptioningManager!!.isEnabled

        val preferencesSubtitleInformation =
            PreferenceSubtitleInformation(
                subCategories,
                subtitleTracks,
                subtitlesType,
                subtitleTypeStrings,
                isClosedCaptionEnabled
            )


        ensureSubtitleLanguages(
            language = utilsModule.getLanguageMapper()!!.getDefaultLanguageCode().languageCodeISO6392,
            type = type
        )

        homeScene.preferencesSceneWidget?.refresh(preferencesSubtitleInformation)
    }

    override fun getSubtitleFirstLanguage(type: PrefType): LanguageCode? {
        if (AudioSubtitlesHelper.getSubtitlePreferredLanguage(0, type) != null) {
            val firstLanguage = AudioSubtitlesHelper.getSubtitlePreferredLanguage(0, type)!!
            if (firstLanguage.isNotEmpty()) {
                utilsModule.getLanguageMapper()!!.getLanguageCodes().forEach { item ->
                    if (item.languageCodeISO6391 == firstLanguage || item.languageCodeISO6392 == firstLanguage) {
                        return LanguageCode(
                            firstLanguage,
                            "",
                            utilsModule.getLanguageMapper()!!.getLanguageName(firstLanguage)!!,
                            "",
                            ""
                        )
                    }
                }
            }
        }
        return null
    }

    override fun onSubtitleFirstLanguageClicked(languageCode: LanguageCode, type: PrefType) {
        AudioSubtitlesHelper.setSubtitlePreferredLanguage(languageCode.languageCodeISO6392, 0, type)
    }

    override fun getSubtitleSecondLanguage(type: PrefType): LanguageCode? {
        if (AudioSubtitlesHelper.getSubtitlePreferredLanguage(1, type) != null) {
            val secondLanguage = AudioSubtitlesHelper.getSubtitlePreferredLanguage(1, type)!!
            if (secondLanguage.isNotEmpty()) {
                utilsModule.getLanguageMapper()!!.getLanguageCodes().forEach { item ->
                    if (item.languageCodeISO6391 == secondLanguage || item.languageCodeISO6392 == secondLanguage) {
                        return LanguageCode(
                            secondLanguage,
                            "",
                            utilsModule.getLanguageMapper()!!.getLanguageName(secondLanguage)!!,
                            "",
                            ""
                        )
                    }
                }
            }
        }
        return null
    }

    override fun onSubtitleSecondLanguageClicked(languageCode: LanguageCode, type: PrefType) {
        AudioSubtitlesHelper.setSubtitlePreferredLanguage(languageCode.languageCodeISO6392, 1, type)
    }

    override fun getSubtitlesState(type: PrefType): Boolean {
        return utilsModule.getSubtitlesState(type)
    }

    override fun onSubtitlesEnabledClicked(isEnabled: Boolean, type: PrefType) {
        utilsModule.enableSubtitles(isEnabled, type)
        playerModule.setCaptionEnabled(isEnabled)
    }

    override fun onSubtitlesTypeClicked(position: Int) {
        utilsModule.setSubtitlesType(position)
        playerModule.refreshTTMLStatus()
    }

    override fun onSubtitlesAnalogTypeClicked(position: Int) {
        if(subtitleModule.getSubtitlesState()) {
            subtitleModule.setSubtitlesType(position, false)
        }else{
            subtitleModule.setSubtitlesType(position, true)
        }
    }

    override fun onClosedCaptionChanged(isCaptionEnabled: Boolean) {
        setCaptionsEnabled(isCaptionEnabled)
    }

    override fun onRenameRecording(recording: Recording, name: String, callback: IAsyncCallback) {
        pvrModule.renameRecording(recording, name, object : IAsyncCallback {
            override fun onSuccess() {
                pvrModule.reloadRecordings()
                getForYouRails()
                callback.onSuccess()
            }

            override fun onFailed(error: Error) {
                //TODO TOAST

            }
        })

    }

    override fun onDeleteRecording(recording: Recording, callback: IAsyncCallback) {
        pvrModule.removeRecording(recording, object : IAsyncCallback {
            override fun onSuccess() {
                pvrModule.reloadRecordings()
                getForYouRails()
                callback.onSuccess()
            }

            override fun onFailed(error: Error) {
                //TODO TOAST

            }
        })

    }

    override fun getRecordingsCategories(
        sceneConfig: SceneConfig?,
        callback: AsyncDataReceiver<MutableList<RailItem>>
    ) {
        val rails: MutableList<RailItem> = mutableListOf()
        var counter = AtomicInteger(0)
        var railSize = 0
        for (i in 0 until sceneConfig!!.value.size) {
            if (sceneConfig.value[i] is ConfigRailParam) {
                railSize++
            }
        }

        for (i in 0 until sceneConfig!!.value.size) {
            if (sceneConfig.value[i] is ConfigRailParam) {
                when ((sceneConfig.value[i] as ConfigRailParam).railId!!.toInt()) {
                    0 -> {
                        Log.i(TAG, "getRecordingsCategories: Recorded")
                        //Recorded
                        pvrModule.reloadRecordings()
                        pvrModule.getRecordingList(object : IAsyncDataCallback<List<Recording>> {
                            override fun onFailed(error: Error) {
                            }

                            override fun onReceive(data: List<Recording>) {
                                var recordings = mutableListOf<Any>()
                                var recList = arrayListOf<Recording>()
                                recList.addAll(data)
                                recList.reverse()
                                recList.forEach { item ->
                                    recordings.add(item)
                                }
                                rails.add(
                                    RailItem(
                                        (sceneConfig.value[i] as ConfigRailParam).railId!!.toInt(),
                                        ConfigStringsManager.getStringById("recorded"),//param.name,
                                        recordings,
                                        RailItem.RailItemType.RECORDING
                                    )
                                )

                                if (checkIfReadyForRefresh(counter, railSize)) {
                                    callback.onReceive(rails)
                                }
                            }
                        })
                    }

                    1 -> {
                        Log.i(TAG, "getRecordingsCategories: future recordings")
                        var scheduledRecordings: ArrayList<com.iwedia.cltv.platform.model.recording.ScheduledRecording> = arrayListOf()
                        schedulerModule.getScheduledRecordingsList(object: IAsyncDataCallback<ArrayList<com.iwedia.cltv.platform.model.recording.ScheduledRecording>>{
                            override fun onFailed(error: Error) {
                                if (error != null) {
                                    Log.i(
                                        TAG,
                                        "onFailed: ${error.message}  rails size  ${rails.size}"
                                    )
                                }
                                if (checkIfReadyForRefresh(counter, railSize)) {
                                    callback.onReceive(rails)
                                }
                            }

                            override fun onReceive(data: ArrayList<com.iwedia.cltv.platform.model.recording.ScheduledRecording>) {
                                data.forEach { item ->
                                    scheduledRecordings.add(
                                        com.iwedia.cltv.platform.model.recording.ScheduledRecording(
                                            item.id,
                                            item.name,
                                            item.scheduledDateStart,
                                            item.scheduledDateEnd,
                                            item.tvChannelId,
                                            item.tvEventId,
                                            RepeatFlag.NONE,
                                            item.tvChannel,
                                            item.tvEvent
                                        )
                                    )
                                }
                                var recordings = mutableListOf<Any>()
                                recordings.addAll(scheduledRecordings)
                                rails.add(
                                    RailItem(
                                        (sceneConfig.value[i] as ConfigRailParam).railId!!.toInt(),
                                        ConfigStringsManager.getStringById("scheduled"),//param.name,
                                        recordings,
                                        RailItem.RailItemType.SCHEDULED_RECORDING
                                    )
                                )

                                if (checkIfReadyForRefresh(counter, railSize)) {
                                    callback.onReceive(rails)
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    private fun checkIfReadyForRefresh(
        counter: AtomicInteger,
        counterSize: Int
    ): Boolean {
        if (counter.incrementAndGet() == counterSize) {
            return true
        }

        return false
    }

    override fun onSearchClicked() {
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.HIDE)
        val sceneData = SceneData(id, instanceId)
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.SEARCH,
            Action.SHOW,
            sceneData
        )
    }

    override fun onGuideFavoriteButtonPressed(
        tvChannel: TvChannel,
        favListIds: ArrayList<String>
    ) {

        val callback = object : IAsyncCallback {
            override fun onSuccess() {
                homeScene.refreshGuideDetailsFavButton()
            }

            override fun onFailed(error: Error) {
                homeScene.refreshGuideDetailsFavButton()

            }
        }

        run exitForEach@{
            tvModule.getChannelList().forEach { channel ->
                if (tvChannel.channelId == channel.channelId) {
                    val favoriteItem = FavoriteItem(
                        channel.id,
                        FavoriteItemType.TV_CHANNEL,
                        channel.favListIds,
                        channel,
                        favListIds
                    )
                    favoriteModule.updateFavoriteItem(
                        favoriteItem,
                        callback
                    )
                    return@exitForEach
                }
            }
        }
    }

    override fun areChannelsNonBrowsable(): Boolean{
        val channelList = tvModule.getChannelList(applicationMode = ApplicationMode.DEFAULT)
        var areChannelsBrowsable = true
        var i = 0
        for(it in channelList){
            if(it.isBrowsable || it.inputId.contains("iwedia") || it.inputId.contains("sampletvinput")){
                areChannelsBrowsable = false
                break
            }
            i++
        }
        return areChannelsBrowsable
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        val applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        return tvModule.getParentalRatingDisplayName(parentalRating, applicationMode, tvEvent)
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return timeModule.getCurrentTime(tvChannel)
    }
    override fun getChannelSourceType(tvChannel: TvChannel) : String {
        return tvModule.getChannelSourceType(tvChannel)
    }

    override fun setPowerOffTime(value: Int, time: String) {
        utilsModule.setPowerOffTime(value + 1, time)
    }

    override fun onPowerClicked() {
        try {
            var intent = Intent()
            if (BuildConfig.FLAVOR == "refplus5" || BuildConfig.FLAVOR == "mal_service") {
                intent.setClassName(SETTINGS_PACKAGE, SETTINGS_POWER_ACTIVITY)
            } else {
                intent = Intent(Settings.ACTION_SETTINGS)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPowerClicked: ${e.printStackTrace()}")
        }
    }

    override fun onResume() {
        super.onResume()
        homeScene.forYouWidget?.restartTimerIfFocusOnPromotionButton()
        // ensure that timer in FastHomeData is stopped and audio is NOT muted
        if (homeScene.selectedPosition == 0) {
            if (homeScene is HomeSceneFast) {
                if (!(homeScene as HomeSceneFast).isSettingsSearchButtonFocused()) {
                    muteAudio()
                }
            } else {
                muteAudio()
            }
        }
        // Resume live playback after going back from channels scan
        if (settingsOpened) {
            settingsOpened = false
        }

        if (updateSystemInformationTimer == null) {
            startUpdateSystemInformationTimerTask()
        }

        if (homeScene.forYouWidget != null && isForYouNeedRefresh) {
            isForYouNeedRefresh = false
            pvrModule.reloadRecordings()
            getForYouRails()
        }
        refreshTime()
    }

    private fun updateGuideCategoryList() {
        getAvailableChannelFilters(object : IAsyncDataCallback<ArrayList<Category>> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: ArrayList<Category>) {
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "onReceive: getAvailableChannelFilters updateGuideCategoryList data ${data.size}"
                )
                var filterList = mutableListOf<CategoryItem>()
                var id = 0
                data.forEach { filterItem ->
                    filterList.add(CategoryItem(filterItem.id, filterItem.name!!))
                    id++
                }
                homeScene.refreshGuideOnUpdateFavorite()
                homeScene.refreshGuideFilterList(filterList)
                homeScene.refreshFavouritesWidget()
            }
        })
    }

    override fun onDigitPressed(
        digit: Int,
        epgActiveFilter: FilterItemType?,
        filterMetadata: String?
    ) {

        ReferenceApplication.runOnUiThread {
            if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
                InformationBus.submitEvent(
                    Event(
                        Events.SHOW_STOP_RECORDING_DIALOG,
                        true
                    )
                )
                return@runOnUiThread
            }
            if (ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
                //donothing
                return@runOnUiThread
            } else {
                val sceneData = SceneData(id, instanceId, DigitZapItem(digit),epgActiveFilter, filterMetadata)
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIGIT_ZAP,
                    Action.SHOW_OVERLAY,
                    sceneData
                )
            }
        }
    }

    override fun onAddFavouriteListClicked() {
        isAddFavouriteScene = true
        context!!.runOnUiThread {
            val sceneData = SceneData(id, instanceId, object : AsyncDataReceiver<String> {
                override fun onFailed(error: core_entities.Error?) {}

                override fun onReceive(data: String) {
                    if (data.isNullOrEmpty() || data.isBlank()) {
                        homeScene.favouritesSceneWidget?.refreshFocusOnCategories()
                        CoroutineHelper.runCoroutineWithDelay({
                            val message =
                                ConfigStringsManager.getStringById("favorite_list_name_can_not_be_empty")
                            showFavoritesEditErrorMessage(message)
                        }, 500, Dispatchers.Main)
                        return
                    }
                    //data.trim() to remove leading and trailing whitespaces
                    favoriteModule.addFavoriteCategory(
                        data.trim(),
                        object : IAsyncCallback {
                            override fun onSuccess() {
                                favoriteModule.getAvailableCategories(
                                    object : IAsyncDataCallback<ArrayList<String>> {
                                        override fun onReceive(list: ArrayList<String>) {
                                            ReferenceApplication.runOnUiThread(Runnable {
                                                homeScene.favouritesSceneWidget?.refreshCategories(
                                                    list
                                                )
                                            })
                                        }

                                        override fun onFailed(error: Error) {

                                        }
                                    })
                            }

                            override fun onFailed(error: Error) {
                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"HomeSceneManager", error.message.toString())
                                if (error.message == "100") {
                                    CoroutineHelper.runCoroutineWithDelay({
                                        val message =
                                            ConfigStringsManager.getStringById("favorite_list_already_exist_1") + " ${data.trim()} " + ConfigStringsManager.getStringById(
                                                "favorite_list_already_exist_2"
                                            )
                                        //formatArgs – The format arguments that will be used for substitution.
//                                            ReferenceApplication.get().getString(R.string.favorite_list_already_exist, data);
                                        showFavoritesEditErrorMessage(message)
                                    }, 500, Dispatchers.Main)
                                }
                            }


                        })
                }
            }, isAddFavouriteScene)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.FAVOURITES_MODIFICATION_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onRenameFavouriteListClicked(currentName: String) {
        isAddFavouriteScene = false
        context!!.runOnUiThread {
            val sceneData = SceneData(id, instanceId, object : AsyncDataReceiver<String> {
                override fun onFailed(error: core_entities.Error?) {
                    homeScene.favouritesSceneWidget?.refreshFocusOnCategories()
                }

                override fun onReceive(data: String) {
                    if (data.isNullOrEmpty() || data.isBlank()) {
                        homeScene.favouritesSceneWidget?.refreshFocusOnCategories()
                        CoroutineHelper.runCoroutineWithDelay({
                            val message =
                                ConfigStringsManager.getStringById("favorite_list_name_can_not_be_empty")
                            showFavoritesEditErrorMessage(message)
                        }, 500)
                        return
                    }
                    //data.trim() to remove leading and trailing whitespaces
                    favoriteModule.renameFavoriteCategory(
                        data.trim(),
                        currentName,
                        object : IAsyncCallback {
                            override fun onSuccess() {
                                favoriteModule.getAvailableCategories(
                                    object : IAsyncDataCallback<ArrayList<String>> {
                                        override fun onReceive(list: ArrayList<String>) {
                                            ReferenceApplication.runOnUiThread(Runnable {
                                                homeScene.favouritesSceneWidget?.refreshCategories(
                                                    list
                                                )
                                            })
                                        }

                                        override fun onFailed(error: Error) {
                                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"HomeSceneManager", error?.message.toString())
                                            showToast("Failed to rename favorites category")
                                        }
                                    })
                            }

                            override fun onFailed(error: Error) {
                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"HomeSceneManager", error?.message.toString())
                                homeScene.favouritesSceneWidget?.refreshFocusOnCategories()
                                ReferenceApplication.runOnUiThread(Runnable {
                                    error.message?.let {
                                        showToast(it)
                                    }
                                })
                            }
                        })
                }
            }, isAddFavouriteScene, currentName)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.FAVOURITES_MODIFICATION_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onDeleteFavouriteListClicked(categoryName: String) {
        isAddFavouriteScene = false

        if (favoriteModule.geFavoriteCategories().size == 1) {
            var message =
                ConfigStringsManager.getStringById("favorite_list_can_not_be_deleted_1") + " ${categoryName} " + ConfigStringsManager.getStringById(
                    "favorite_list_can_not_be_deleted_2"
                )
            showFavoritesEditErrorMessage(message)
            return
        }

        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("are_you_sure_to_remove")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    homeScene.favouritesSceneWidget?.refreshFocusOnCategories()
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                }

                override fun onPositiveButtonClicked() {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    favoriteModule.removeFavoriteCategory(
                        categoryName,
                        object : IAsyncCallback {
                            override fun onSuccess() {
                                categoryModule.setActiveEpgFilter(HorizontalGuideSceneWidget.GUIDE_FILTER_ALL)
                                favoriteModule.getAvailableCategories(
                                    object : IAsyncDataCallback<ArrayList<String>> {
                                        override fun onReceive(list: ArrayList<String>) {
                                            ReferenceApplication.runOnUiThread(Runnable {
                                                homeScene.favouritesSceneWidget?.refreshCategories(
                                                    list
                                                )
                                                worldHandler!!.triggerAction(
                                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                    Action.DESTROY
                                                )
                                            })
                                        }

                                        override fun onFailed(error: Error) {
                                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"HomeSceneManager", error?.message.toString())
                                            showToast("Failed to delete favorites category")
                                        }
                                    })
                            }

                            override fun onFailed(error: Error) {
                            }
                        })
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    private fun showFavoritesEditErrorMessage(error: String) {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.TEXT
            sceneData.title = error
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    homeScene.favouritesSceneWidget?.setFocusForCategory()
                }

                override fun onPositiveButtonClicked() {
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                    homeScene.favouritesSceneWidget?.setFocusForCategory()
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getPreferencesCamInfoModuleInformation() {
        var camInfoModuleInformation = CamInfoModuleInformation(
            "SMARTDTV",
            "Cl0311 -CNX01",
            "11411505428332",
            "07.00.50.03.00.05",
            "SmartCAM 3"
        )
        homeScene.preferencesSceneWidget?.refresh(camInfoModuleInformation)
    }

    override fun onPreferencesSoftwareDownloadPressed() {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("software_download_title")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                }

                override fun onPositiveButtonClicked() {
                    // TODO start software update
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getPreferencesTxtData() {
        val prefSetupOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.TELETEXT)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()
        prefSetupOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }
        val digitalLanguages = mutableListOf<LanguageCode>()
        digitalLanguages.addAll(utilsModule.getLanguageMapper()!!.preferredLanguageCodes)

        var decodingPageLanguages = ttxModule.decodeingPageLanguage

        val preferredLanguages = mutableListOf<String>()
        preferredLanguages.add("Automatic")
        preferredLanguages.add("English")
        preferredLanguages.add("Norsk")
        preferredLanguages.add("Dansk")
        preferredLanguages.add("Svenska")
        preferredLanguages.add("Soumi")
        preferredLanguages.add("Deutch")
        preferredLanguages.add("Français")
        preferredLanguages.add("Italiano")
        preferredLanguages.add("Nederlands")
        preferredLanguages.add("Rусский")
        preferredLanguages.add("Türk")
        preferredLanguages.add("עִברִית")
        preferredLanguages.add("العربية")

        var defaultDigitalLang = ttxModule.getDigitalTTXLanguage()
        var defaultDecodingLang = ttxModule.getDecodingPageLanguage()
        var defaultPreferredLang = preferredLanguages[0]

        var preferencesTxtInformation = PreferencesTeletextInformation(
            subCategories,
            digitalLanguages,
            decodingPageLanguages,
            preferredLanguages,
            defaultDigitalLang,
            defaultDecodingLang,
            defaultPreferredLang

        )
        homeScene.preferencesSceneWidget?.refresh(preferencesTxtInformation)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getPreferencesCamData() {
        val prefSetupOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.CAMINFO)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()
        prefSetupOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }

        var camOperatorPreferenceStrings = mutableListOf<String>()
        if(ciPlusModule.getProfileName() != "") {
            subCategories.add(PreferenceSubMenuItem(PrefSubMenu.CAM_OPERATOR,buildName(PrefSubMenu.CAM_OPERATOR)!!))
            camOperatorPreferenceStrings.add(ciPlusModule.getProfileName())
        }

        var camMenuPreferenceStrings = mutableListOf<String>()
        if (ciPlusModule.getCiName() != "") {
            camMenuPreferenceStrings.add(ciPlusModule.getCiName())
        } else {
            camMenuPreferenceStrings.add(ConfigStringsManager.getStringById("no_cam"))
        }

        var userPreferenceStrings = mutableListOf<String>()
        userPreferenceStrings.add(ConfigStringsManager.getStringById("_default"))
        userPreferenceStrings.add(ConfigStringsManager.getStringById("ammi"))
        userPreferenceStrings.add(ConfigStringsManager.getStringById("broadcast"))
        var userPreferenceSelected = 0
        when(getPrefsValue(MENU_CI_USER_PREFERENCE, MENU_CI_USER_PREFERENCE_DEFAULT_ID)){
            MENU_CI_USER_PREFERENCE_DEFAULT_ID ->{
                userPreferenceSelected = 0
            }
            MENU_CI_USER_PREFERENCE_AMMI_ID ->{
                userPreferenceSelected = 1
            }
            MENU_CI_USER_PREFERENCE_BROADCAST_ID ->{
                userPreferenceSelected = 2
            }
        }

        var camTypePreferenceStrings = mutableListOf<String>()
        camTypePreferenceStrings.add(ConfigStringsManager.getStringById("pcmcia_cam"))
        camTypePreferenceStrings.add(ConfigStringsManager.getStringById("usb_cam"))
        var camTypePreferenceSelected = 0
        when(getPrefsValue(MENU_CAM_TYPE_PREFERENCE, MENU_CAM_TYPE_PREFERENCE_PCMCIA_ID)){
            MENU_CAM_TYPE_PREFERENCE_PCMCIA_ID ->{
                camTypePreferenceSelected = 0
            }
            MENU_CAM_TYPE_PREFERENCE_USB_ID ->{
                camTypePreferenceSelected = 1
            }
        }

        var preferencesCamInfoInformation = PreferencesCamInfoInformation(
            subCategories,
            camMenuPreferenceStrings,
            userPreferenceStrings,
            userPreferenceSelected,
            camTypePreferenceStrings,
            camTypePreferenceSelected,
            camOperatorPreferenceStrings
        )

        homeScene.preferencesSceneWidget?.refresh(preferencesCamInfoInformation)
    }

    override fun onTTXDigitalLanguageClicked(position: Int, language: String) {
        utilsModule.setPrefsValue(
            ReferenceApplication.TTX_DIGITAL_LANGUAGE,
            position
        )
        ttxModule.saveDigitalTTXLanguage(language)
        AudioSubtitlesHelper.setTeletextPreferredLanguage(position, 0)
    }

    override fun onTTXDecodeLanguageClicked(position: Int) {
        utilsModule.setPrefsValue(
            ReferenceApplication.TTX_DECODE_LANGUAGE,
            position
        )
        ttxModule.saveDecodingPageLanguage(position)
        AudioSubtitlesHelper.setTeletextPreferredLanguage(position, 1)
    }

    override fun onPreferencesSubscriptionStatusPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesStatusSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("subscription_status")
            //TODO dummy data
            var list = ArrayList<PreferencesStatusSceneData.StatusItem>()
            list.add(sceneData.StatusItem(true, "Name", "Start", "End", "ID"))
            list.add(sceneData.StatusItem(false, "SBB 01", "01 Feb 22", "28 Feb 22", "01800006"))
            list.add(sceneData.StatusItem(false, "SBB 01", "01 Jan 22", "31 Jan 22", "01800006"))
            list.add(sceneData.StatusItem(false, "SBB 02", "01 Feb 22", "28 Feb 22", "01000422"))
            list.add(sceneData.StatusItem(false, "SBB 02", "01 Jan 22", "31 Jan 22", "01000422"))
            sceneData.items.addAll(list)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onPreferencesEventStatusPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesStatusSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("event_status")
            //TODO dummy data
            var list = ArrayList<PreferencesStatusSceneData.StatusItem>()
            list.add(sceneData.StatusItem(true, "Name", "Start", "End", "Minutes/Credits left"))
            sceneData.items.addAll(list)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onPreferencesTokenStatusPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesStatusSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("token_status")
            //TODO dummy data
            var list = ArrayList<PreferencesStatusSceneData.StatusItem>()
            list.add(sceneData.StatusItem(true, "Purce", "Balance (Token)"))
            sceneData.items.addAll(list)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_STATUS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onPreferencesChangeCaPinPressed() {
        context!!.runOnUiThread {
            var sceneData = ParentalPinSceneData(id, instanceId)
            sceneData.sceneType = ParentalPinSceneData.CA_CHANGE_PIN_SCENE_TYPE
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PARENTAL_PIN,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onPreferencesAboutConaxCaPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesInfoSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("about_conax_ca")
            //TODO dummy data
            var list = ArrayList<PreferencesInfoSceneData.InfoData>()
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("software_version"),
                    "07.00.50.03.00.05"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("interface_version"),
                    "0x40"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("card_number"),
                    "020 9580 7967-3"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("number_of_sessions"),
                    "10"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("languages"),
                    "381"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("ca_sys_id"),
                    "0x0B00"
                )
            )
            list.add(
                sceneData.InfoData(
                    ConfigStringsManager.getStringById("chip_id"),
                    "006 2149 5407"
                )
            )
            sceneData.items = list
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_INFO_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getHbbTvInformation() {
        val prefOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.HBBTV)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        prefOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }

        var isHbbSupport = hbbTvModule.getHbbtvFunctionSwitch()

        var isTrack = hbbTvModule.getHbbtvDoNotTrack()

        var cookieSettingsSelected = utilsModule.getPrefsValue(
            ReferenceApplication.HBBTV_COOKIE_SETTINGS_SELECTED,
            0
        ) as Int

        var cookieSettingsStrings = mutableListOf<String>()
        cookieSettingsStrings.add(ConfigStringsManager.getStringById("hbbtv_blocker_3rd_party_cookies"))
        cookieSettingsStrings.add(ConfigStringsManager.getStringById("hbbtv_default"))

        var isPersistentStorage = utilsModule.getPrefsValue(
            ReferenceApplication.HBBTV_PERSISTENT_STORAGE,
            false
        ) as Boolean

        var isBlockTrackingSites = utilsModule.getPrefsValue(
            ReferenceApplication.HBBTV_BLOCK_TRACKING_SITES,
            false
        ) as Boolean

        var isDeviceId = utilsModule.getPrefsValue(
            ReferenceApplication.HBBTV_BLOCK_TRACKING_SITES,
            false
        ) as Boolean

        var preferencesHbbTvInformation =
            PreferencesHbbTVInfromation(
                subCategories,
                isHbbSupport,
                isTrack,
                cookieSettingsSelected,
                cookieSettingsStrings,
                isPersistentStorage,
                isBlockTrackingSites,
                isDeviceId
            )
        homeScene.preferencesSceneWidget?.refresh(preferencesHbbTvInformation)
    }

    override fun onHbbSupportSwitch(isEnabled: Boolean) {
        utilsModule.setPrefsValue(
            ReferenceApplication.HBBTV_SUPPORT,
            isEnabled
        )
        if (isEnabled) {
            hbbTvModule.supportHbbTv(true)
        } else {
            hbbTvModule.supportHbbTv(false)
        }
    }

    override fun onHbbTrackSwitch(isEnabled: Boolean) {
        utilsModule.setPrefsValue(
            ReferenceApplication.HBBTV_TRACK,
            isEnabled
        )

        if (isEnabled) {
            hbbTvModule.disableHbbTvTracking(true)
        } else {
            hbbTvModule.disableHbbTvTracking(false)
        }
    }

    override fun onHbbPersistentStorageSwitch(isEnabled: Boolean) {
        utilsModule.setPrefsValue(
            ReferenceApplication.HBBTV_PERSISTENT_STORAGE,
            isEnabled
        )

        if (isEnabled) {
            hbbTvModule.persistentStorageHbbTv(true)
        } else {
            hbbTvModule.persistentStorageHbbTv(false)
        }
    }

    override fun onHbbBlockTracking(isEnabled: Boolean) {
        utilsModule.setPrefsValue(
            ReferenceApplication.HBBTV_BLOCK_TRACKING_SITES,
            isEnabled
        )

        if (isEnabled) {
            hbbTvModule.blockTrackingSitesHbbTv(true)
        } else {
            hbbTvModule.blockTrackingSitesHbbTv(false)
        }
    }

    override fun onHbbTvDeviceId(isEnabled: Boolean) {
        utilsModule.setPrefsValue(
            ReferenceApplication.HBBTV_DEVICE_ID,
            isEnabled
        )

        if (isEnabled) {
            hbbTvModule.deviceIdHbbTv(true)
        } else {
            hbbTvModule.deviceIdHbbTv(false)
        }
    }

    override fun onHbbTvResetDeviceId() {
        hbbTvModule.resetDeviceIdHbbTv()
    }

    override fun onHbbCookieSettingsSelected(position: Int) {
        utilsModule.setPrefsValue(
            ReferenceApplication.HBBTV_COOKIE_SETTINGS_SELECTED,
            position
        )

        hbbTvModule.cookieSettingsHbbTv(HbbTvCookieSettingsValue.fromInt(position))
    }

    override fun onPreferencesConaxCaMessagesPressed() {
        context!!.runOnUiThread {
            var sceneData = PreferencesInfoSceneData(id, instanceId)
            sceneData.title = ConfigStringsManager.getStringById("messages")
            //TODO dummy data
            var list = ArrayList<PreferencesInfoSceneData.InfoData>()
            list.add(
                sceneData.InfoData(
                    "Message 1",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            list.add(
                sceneData.InfoData(
                    "Message 2",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            list.add(
                sceneData.InfoData(
                    "Message 3",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            list.add(
                sceneData.InfoData(
                    "Message 4",
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod\n" +
                            "tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim\n" +
                            "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea\n" +
                            "commodo consequat."
                )
            )
            sceneData.items = list
            sceneData.type = PreferencesInfoSceneData.INFO_MESSAGES_TYPE
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PREFERENCES_INFO_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun getPreferencesCamInfoMaturityRating(): String {
        // TODO dummy data
        return "G"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun getPreferencesCamInfoSettingsLanguages() {
        //TODO dummy data
        val languages = ArrayList<String>()
        languages.add("Automatic")
        languages.add("English")
        languages.add("Norsk")
        languages.add("Dansk")
        languages.add("Svenska")
        languages.add("Soumi")
        languages.add("Deutch")
        languages.add("Français")
        languages.add("Italiano")
        languages.add("Nederlands")
        languages.add("Rусский")
        var selectedLanguage: Int =
            utilsModule.getPrefsValue("CamInfoSelectedLanguage", 0) as Int
        var languageData = CamInfoLanguageData(selectedLanguage, languages)
        homeScene.preferencesSceneWidget!!.refresh(languageData)
    }

    override fun onPreferencesCamInfoSettingsLanguageSelected(position: Int) {
        utilsModule.setPrefsValue("CamInfoSelectedLanguage", position)
    }

    override fun onPreferencesCamInfoPopUpMessagesActivated(activated: Boolean) {
        utilsModule.setPrefsValue("CamInfoPopUpMessages", activated)
    }

    override fun isPreferencesCamInfoPopUpMessagesActivated(): Boolean {
        return utilsModule.getPrefsValue("CamInfoPopUpMessages", false) as Boolean
    }

    override fun channelUp() {
        if (timeShiftModule.isTimeShiftActive) {
            showTimeShiftDialogOnChannelUpDown(true)
        } else {
            tvModule.nextChannel(object : IAsyncCallback {
                override fun onSuccess() {

                }

                override fun onFailed(error: Error) {
                }
            })
        }
    }

    override fun channelDown() {
        if (timeShiftModule.isTimeShiftActive) {
            showTimeShiftDialogOnChannelUpDown(false)
        } else {
            tvModule.previousChannel(object : IAsyncCallback {
                override fun onSuccess() {

                }

                override fun onFailed(error: Error) {
                }
            })
        }
    }


    private fun showTimeShiftDialogOnChannelUpDown(channelUp: Boolean) {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("timeshift_exit_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {}

                override fun onPositiveButtonClicked() {
                    timeShiftModule.timeShiftStop(object : IAsyncCallback {
                        override fun onSuccess() {
                        }

                        override fun onFailed(error: Error) {
                        }
                    })
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                    ReferenceApplication.worldHandler!!.playbackState =
                        ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                    timeShiftModule.setTimeShiftIndication(
                        false
                    )

                    playerModule.resume()
                    if (channelUp) {
                        tvModule.nextChannel(object : IAsyncCallback {
                            override fun onFailed(error: Error) {}

                            override fun onSuccess() {}
                        })
                    } else {
                        tvModule.previousChannel(object : IAsyncCallback {
                            override fun onFailed(error: Error) {}

                            override fun onSuccess() {}
                        })
                    }
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startRecording(tvEvent: TvEvent, callback: IAsyncCallback) {
        worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY)
        if (activeChannel == tvEvent.tvChannel && playerModule.isOnLockScreen) {
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            callback.onSuccess()
            return
        }
        if (activeChannel != tvEvent.tvChannel && tvEvent.tvChannel.isLocked){
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            callback.onSuccess()
            return
        }

        if (utilsModule.getUsbDevices().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
            callback.onSuccess()
            return
        }
        if (utilsModule.getPvrStoragePath().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
            InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
            callback.onSuccess()
            return
        }
        if (!utilsModule.isUsbWritableReadable()) {
            showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
            callback.onSuccess()
            return
        }
        if (!utilsModule.isUsbFreeSpaceAvailable()) {
            showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
            callback.onSuccess()
            return
        }
        if (activeChannel.id == tvEvent.tvChannel.id) {
            startRecordingByChannel(tvEvent.tvChannel, callback)
        } else {
            tvModule.changeChannel(tvEvent.tvChannel, object : IAsyncCallback {
                override fun onFailed(error: Error) {}

                override fun onSuccess() {
                    startRecordingByChannel(tvEvent.tvChannel, callback)
                }
            }, ApplicationMode.DEFAULT)
        }
    }
    private fun startRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback) {
        pvrModule.startRecordingByChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onSuccess() {
                homeScene.guideSceneWidget!!.refreshRecordButton()
                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
                callback.onSuccess()
            }
        })
    }

    private fun stopRecording(tvEvent: TvEvent) {
        com.iwedia.cltv.platform.model.information_bus.events.InformationBus
            .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHING)
        if (tvEvent.name == ConfigStringsManager.getStringById("no_info") && pvrModule.isRecordingInProgress()
        ) {
            pvrModule.stopRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback{
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopRecording: Failed ${error.message}")
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                }

                override fun onSuccess() {
                    homeScene.guideSceneWidget!!.refreshRecordButton()
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                }
            })
        } else if (isCurrentEvent(tvEvent)) {
            //Stop current recording
            pvrModule.stopRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback{
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopRecording: Failed ${error.message}")
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                }

                override fun onSuccess() {
                    homeScene.guideSceneWidget!!.refreshRecordButton()
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRecordButtonPressed(tvEvent: TvEvent, callback: IAsyncCallback) {
        if (tvModule.isSignalAvailable()) {
            //Current time
            val currentTime = timeModule.getCurrentTime(tvEvent.tvChannel)
            if (tvEvent.startTime > currentTime) {
                //future event
                schedulerModule.hasScheduledRec(
                    tvEvent,
                    object : IAsyncDataCallback<Boolean> {
                        override fun onFailed(error: Error) {
                            callback.onFailed(error)
                        }

                        override fun onReceive(isScheduledRecording: Boolean) {
                            val obj = ScheduledRecording(
                                tvEvent.id, tvEvent.name, tvEvent.startTime, tvEvent.endTime,
                                tvEvent.tvChannel.id, tvEvent.id, RepeatFlag.NONE,
                                tvEvent.tvChannel, tvEvent
                            )
                            if (!isScheduledRecording) {
                                if ((TextUtils.isEmpty(tvEvent.name)
                                            || tvEvent.name == ConfigStringsManager.getStringById("no_information"))
                                    && (TextUtils.isEmpty(tvEvent.longDescription)
                                            || tvEvent.longDescription.equals(
                                        ConfigStringsManager.getStringById(
                                            "no_information"
                                        )
                                    ))
                                    && (TextUtils.isEmpty(tvEvent.shortDescription))
                                    || tvEvent.shortDescription.equals(
                                        ConfigStringsManager.getStringById(
                                            "no_information"
                                        )
                                    )
                                ) {

                                    showToast(ConfigStringsManager.getStringById("recording_scheduled_error"))

                                } else {
                                    if (utilsModule.getUsbDevices().isEmpty()) {
                                        showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
                                        callback.onSuccess()
                                        return
                                    }
                                    if (utilsModule.getPvrStoragePath().isEmpty()) {
                                        showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
                                        InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
                                        callback.onSuccess()
                                        return
                                    }
                                    if (!utilsModule.isUsbWritableReadable()) {
                                        showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
                                        callback.onSuccess()
                                        return
                                    }
                                    if (!utilsModule.isUsbFreeSpaceAvailable()) {
                                        showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
                                        callback.onSuccess()
                                        return
                                    }
                                    //not scheduled already
                                    schedulerModule.scheduleRecording(
                                        obj,
                                        object :
                                            IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult> {
                                            override fun onFailed(error: Error) {
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: not scheduled already")
                                                when (error.message) {
                                                    "alreadyPresent" -> {
                                                        showToast(
                                                            ConfigStringsManager.getStringById(
                                                                "favorite_list_already_exist_2"
                                                            )
                                                        )
                                                    }

                                                    "conflict" -> {
                                                        showToast(
                                                            ConfigStringsManager.getStringById(
                                                                "watchlist_recording_conflict"
                                                            )
                                                        )
                                                    }

                                                    "USB NOT CONNECTED\nConnect USB to record" -> {
                                                        showToast(
                                                            ConfigStringsManager.getStringById(
                                                                "usb_not_connected_connect_usb_to_record"
                                                            )
                                                        )
                                                        callback.onSuccess()
                                                        return
                                                    }

                                                    else -> {
                                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: Reminder failed")
                                                    }
                                                }
                                            }

                                            override fun onReceive(scheduleData: SchedulerInterface.ScheduleRecordingResult) {
                                                Log.d(Constants.LogTag.CLTV_TAG +
                                                    TAG,
                                                    "onSuccess: not scheduled already/scheduleData = ${scheduleData.name}"
                                                )
                                                InformationBus.submitEvent(
                                                    Event(
                                                        Events.REFRESH_FOR_YOU,
                                                        data
                                                    )
                                                )
                                                InformationBus.submitEvent(
                                                    Event(
                                                        UPDATE_REC_LIST_INDICATOR
                                                    )
                                                )
                                                callback.onSuccess()
                                            }
                                        })
                                }
                            } else {
                                //already scheduled, removing...
                                schedulerModule.removeScheduledRecording(
                                    obj,
                                    object : IAsyncCallback {
                                        override fun onFailed(error: Error) {
                                            callback.onFailed(error)
                                        }

                                        override fun onSuccess() {
                                            InformationBus.submitEvent(
                                                Event(
                                                    Events.REFRESH_FOR_YOU,
                                                    data
                                                )
                                            )
                                            InformationBus.submitEvent(
                                                Event(
                                                    UPDATE_REC_LIST_INDICATOR
                                                )
                                            )
                                            callback.onSuccess()
                                        }
                                    })
                            }
                        }
                    }
                )
            }
            else {
                val isActiveChannel = TvChannel.compare(tvEvent.tvChannel, activeChannel)
                if (isCurrentEvent(tvEvent)) {
                    if (!pvrModule.isRecordingInProgress()) {
                        if (!timeShiftModule.isTimeShiftActive) {
                            // below snippet is added to save last selected filter from EPG.k
                            utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, 0)
                            categoryModule.setActiveCategory(homeScene.getEpgActiveFilter().name)
                            activeCategoryId = homeScene.getEpgActiveFilter().id
                            var favGroupName = ""
                            var tifCategoryName = ""
                            var genreCategoryName = ""
                            if (activeCategoryId == FilterItem.FAVORITE_ID) {
                                favGroupName = categoryModule.getActiveCategory()
                            } else if (activeCategoryId >= FilterItem.TIF_INPUT_CATEGORY && activeCategoryId < FilterItem.FAVORITE_ID) {
                                tifCategoryName = categoryModule.getActiveCategory()
                            } else if (activeCategoryId == FilterItem.GENRE_CATEGORY) {
                                genreCategoryName = categoryModule.getActiveCategory()
                            }
                            tvModule.updateLaunchOrigin(
                                activeCategoryId,
                                favGroupName,
                                tifCategoryName,
                                genreCategoryName
                            )
                            startRecording(tvEvent, callback)
                        } else {
                            com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                                Events.SHOW_RECORDING_TIME_SHIFT_CONFLICT_DIALOG,
                                arrayListOf(tvEvent)
                            )
                        }
                    } else {
                        if (isActiveChannel) {
                            stopRecording(tvEvent)
                        } else {
                            utilsModule.runCoroutine({
                                val sceneData = DialogSceneData(id, instanceId)
                                sceneData.type = DialogSceneData.DialogType.YES_NO
                                sceneData.title =
                                    ConfigStringsManager.getStringById("recording_exit_msg")
                                sceneData.positiveButtonText =
                                    ConfigStringsManager.getStringById("ok")
                                sceneData.negativeButtonText =
                                    ConfigStringsManager.getStringById("cancel")
                                sceneData.dialogClickListener =
                                    object : DialogSceneData.DialogClickListener {
                                        override fun onNegativeButtonClicked() {
                                            worldHandler!!.triggerAction(
                                                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                Action.DESTROY
                                            )
                                        }

                                        override fun onPositiveButtonClicked() {
                                            val recordingChannel =
                                                pvrModule.getRecordingInProgressTvChannel()
                                            pvrModule.stopRecordingByChannel(
                                                recordingChannel!!,
                                                object : IAsyncCallback {
                                                    override fun onFailed(error: Error) {
                                                        Log.d(
                                                            ReferenceApplication.TAG,
                                                            "stop recording failed"
                                                        )
                                                    }

                                                    override fun onSuccess() {
                                                        stopRecording(tvEvent)
                                                        startRecording(tvEvent, callback)
                                                    }
                                                })
                                        }
                                    }
                                worldHandler!!.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                    Action.SHOW,
                                    sceneData
                                )
                            }, Dispatchers.Main)
                        }
                    }
                    callback.onSuccess()
                }
            }
        } else showToast(ConfigStringsManager.getStringById("failed_to_start_pvr"))
    }

    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
        return utilsModule.isCurrentEvent(tvEvent)
    }

    override fun getCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun getAvailableAudioTracks(): List<IAudioTrack> {
        return playerModule.getAudioTracks()
    }

    override fun getAvailableSubtitleTracks(): List<ISubtitle> {
        return playerModule.getSubtitleTracks()
    }

    override fun getClosedCaption(): String? {
        return closedCaptionModule?.getClosedCaption()
    }

    override fun isDigitalSubtitleEnabled(type: PrefType): Boolean {
        return utilsModule.getSubtitlesState(type)
    }

    override fun isAnalogSubtitleEnabled(type: PrefType): Boolean {
        return subtitleModule.getSubtitlesState()
    }

    override fun onAdsTargetingChange(enable: Boolean) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onAdsTargetingChange: enable $enable")
        fastUserSettingsModule.setDnt(enable, object :IAsyncCallback{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: update DO NOT TRACK")
            }

            override fun onSuccess() {
                utilsModule.setPrefsValue(KEY_ADS_TARGETING, enable)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: update DO NOT TRACK")
            }
        })
    }

    override fun promotionItemActionClicked(promotionItem: PromotionItem) {
        if (promotionItem.channelid != null) {
            var tvChannel = tvModule.getChannelByDisplayNumber(promotionItem.channelid!!, ApplicationMode.FAST_ONLY)
            if (tvChannel != null) {
                playFastChannel(tvChannel)
                //updates the active gener when channel is played from for you promotions.
                if (tvChannel.genres.isNotEmpty()){
                    FastLiveTabDataProvider.activeGenre = tvChannel.genres[0].lowercase().split(' ')
                        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                }
            }
        } else if(promotionItem.isVodContent()) {
            onVodItemClicked(if(promotionItem.type == Constants.VodTypeConstants.SERIES) VODType.SERIES else VODType.SINGLE_WORK , promotionItem.contentId!!)
        }
    }

    override fun getActiveEpgFilter(): Int {
        return categoryModule.getActiveEpgFilter()
    }

    override fun setActiveEpgFilter(filterId: Int) {
        categoryModule.setActiveEpgFilter(filterId)
    }

    override fun getActiveCategory(): String {
        return categoryModule.getActiveCategory()
    }

    /**
     * hasInternet() will check the internet connection and return the status.
     * */
    override fun hasInternet(): Boolean {
        return if (networkModule.networkStatus.isInitialized)
            networkModule.networkStatus.value != NetworkData.NoConnection
        else
            false
    }

    override fun onSettingsOpened() {
        settingsOpened = true
        if (!((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()) {
            (ReferenceApplication.getActivity() as MainActivity).settingsIconClicked = true
        }
    }

    override fun isProfileButtonEnabled(): Boolean {
        return false
        //return !inputSourceModule.isBasicMode()
    }

    override fun customRecButtonClciked() {
        context!!.runOnUiThread {
            worldHandler!!.triggerAction(id, Action.HIDE)
            var sceneData = SceneData(id, instanceId)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.CUSTOM_RECORDING,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onAspectRatioClicked(position: Int) {
        defaultAspectRatioOption = position
        utilsModule.setAspectRatio(position)
    }

    override fun setInteractionChannel(isSelected: Boolean) {
        utilsModule.setPrefsValue(
            ReferenceApplication.INTERACTION_CHANNEL,
            isSelected
        )
    }

    override fun onEpgLanguageChanged(language: String) {
//        TvConfigurationHelper.setEpgLanguage(language)
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
        if (channelList.size == 0) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, " Channel list size zero so clear the prefs")
            utilsModule.clearChannelidsPref()
        }
        if (type == PrefType.PLATFORM){
            //for platform channels we are getting some non browsable channels
            channelList.toList().forEach {
                if(it.isLocked){
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"fill initial locked channels to prefs ${it.name}")
                    utilsModule.updateLockedChannelIdsToPref(context!!,it.isLocked,it.getUniqueIdentifier())
                }
                if (!tvModule.isChannelLockAvailable(it)){
                    channelList.remove(it)
                }
            }
        }

        val parentalInformation = PreferencesParentalControlInformation(
            subCategories,
            utilsModule.sortListByNumber(channelList),
            blockedInputs,
            blockedInputCount,
            blockUnratedProgramsAvailability,
            parentalControlSettingsModule.getBlockUnrated()==1,
            contentRatingsData,
            contentRatingsEnabled,
            parentalControlSettingsModule.getGlobalRestrictionsArray(),
            parentalControlSettingsModule.getAnokiRatingList()
        )
        homeScene.preferencesSceneWidget?.refresh(parentalInformation)
    }

    /**
    * checkNetworkConnection() will check the internet connection and will show the toast in case of no internet.
    * */
    override fun checkNetworkConnection(): Boolean {
        return if (networkModule.networkStatus.isInitialized) {
            networkModule.networkStatus.value != NetworkData.NoConnection
        } else {
            showToast(ConfigStringsManager.getStringById("no_internet_message"))
            false
        }
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

    override fun isAnokiParentalControlsEnabled(): Boolean {
        return parentalControlSettingsModule.isAnokiParentalControlsEnabled()
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

        //why is this statically placed
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

        val defCaptionDisplay: Int? = closedCaptionModule?.getDefaultCCValues("display_cc")
        val withMute = closedCaptionModule?.getDefaultMuteValues()
        var defCaptionServices = closedCaptionModule?.getDefaultCCValues("caption_services")
        var defAdvancedSelection = closedCaptionModule?.getDefaultCCValues("advanced_selection")
        var defTextSize = closedCaptionModule?.getDefaultCCValues("text_size")
        var defFontFamily = closedCaptionModule?.getDefaultCCValues("font_family")
        var defTextColor = closedCaptionModule?.getDefaultCCValues("text_color")
        var defTextOpacity = closedCaptionModule?.getDefaultCCValues("text_opacity")
        var defEdgeType = closedCaptionModule?.getDefaultCCValues("edge_type")
        var defEdgeColor = closedCaptionModule?.getDefaultCCValues("edge_color")
        var defBgColor = closedCaptionModule?.getDefaultCCValues("background_color")
        var defBgOpacity = closedCaptionModule?.getDefaultCCValues("background_opacity")

        val lastSelectedTextOpacity = utilsModule.getPrefsValue("TEXT_OPACITY",0)as Int?
        val information = PreferencesClosedCaptionsInformation(
            subCategories,
            withMute as Boolean?, defCaptionDisplay, captionServicesList, defCaptionServices,
            advancedSelectionList, defAdvancedSelection, textSizeList, defTextSize,
            fontFamilyList, defFontFamily, captionColorList, defTextColor, captionOpacityList,
            defTextOpacity, edgeTypeList, defEdgeType, captionColorList, defEdgeColor,
            captionColorList, defBgColor, captionOpacityList, defBgOpacity, lastSelectedTextOpacity
        )
        homeScene.preferencesSceneWidget?.refresh(information)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getPreferencesPvrTimeshiftData() {
        val prefOptions = preferenceModule.getPreferenceSubMenus(PrefMenu.PVR_TIMESHIFT)
        val subCategories: MutableList<PreferenceSubMenuItem> = mutableListOf()

        prefOptions.forEach {
            subCategories.add(PreferenceSubMenuItem(it, buildName(it)!!))
        }
        val usbDevices = utilsModule.getUsbDevices()

        val timeshiftEnabled = getPrefsValue("IS_TIMESHIFT_ENABLED", false) as Boolean
        val information = PreferencesPvrTimeshiftInformation(
            subCategories,
            timeshiftEnabled,
            getReferenceDeviceItems(usbDevices)
        )
        homeScene.preferencesSceneWidget?.refresh(information)
    }

    private fun getReferenceDeviceItems(devices: HashMap<String, String>): MutableList<ReferenceDeviceItem> {
        val list = mutableListOf<ReferenceDeviceItem>()

        devices.keys.forEach {
            val statFs = StatFs(it)
            val availableBytes = ((statFs.availableBytes.toFloat() / 1024) / 1024) / 1024
            val freeBytes = statFs.freeBytes.toFloat()
            val totalBytes = ((statFs.totalBytes.toFloat() / 1024) / 1024) / 1024

            val refItem = ReferenceDeviceItem(
               devices[it],
                totalBytes,
                availableBytes,
                false, // TODO check if use set this for this storage
                false, // TODO check if use set this for this storage
                moundPoint =  it,
                false,
                null // TODO set this after checking speed
            )
            list.add(refItem)
        }

        return list
    }

    override fun getPrefsValue(key: String, value: Any?): Any? {
        return utilsModule.getPrefsValue(key, value)
    }

    override fun getPrefsValue(tag: String, defValue: String): String {
       return utilsModule.getPrefsValue(tag, defValue) as String
    }

    override fun setPrefsValue(key: String, defValue: Any?) {
        utilsModule.setPrefsValue(key, defValue!!)
    }

    override fun isTimeShiftActive(): Boolean {
        return timeShiftModule.isTimeShiftActive
    }

    override fun lockChannel(tvChannel: TvChannel, selected: Boolean) {
        var applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        tvModule.lockUnlockChannel(
            tvChannel,
            selected,
            object : IAsyncCallback{
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: lock channel failed")
                }

                override fun onSuccess() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: channel locked ${tvChannel.name}")
                }

            }, applicationMode
        )

    }

    override fun isChannelLockAvailable(tvChannel: TvChannel): Boolean {
        return tvModule.isChannelLockAvailable(tvChannel)
    }

    override fun isLockedScreenShown(): Boolean {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: TvChannel) {
                activeChannel = data
            }

        })
        return (playerModule.isParentalActive || (this::activeChannel.isInitialized && activeChannel.isLocked && !playerModule.isChannelUnlocked)) && playerModule.getIsParentalControlActive()
    }

    override fun blockInput(inputSourceData: InputSourceData, blocked: Boolean) {
        inputSourceModule.blockInput(blocked,inputSourceData.inputMainName)
        var defaultInput = utilsModule.getPrefsValue("inputSelectedString", "TV") as String
        if (defaultInput == inputSourceData.inputMainName && blocked) {
            isCurrentInputBlocked = true
            InformationBus.submitEvent(
                Event(BLOCK_TV_VIEW, inputSourceData.inputSourceName)
            )
        }
    }

    override fun isParentalControlsEnabled():Boolean {
        println("ParentalE "+"is enabled = ${parentalControlSettingsModule.isParentalControlsEnabled()}")
        return parentalControlSettingsModule.isParentalControlsEnabled()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun setParentalControlsEnabled(enabled: Boolean) {
        var res = parentalControlSettingsModule.setParentalControlsEnabled(enabled)
        if(enabled) {
            if (inputSourceModule.isBlock(inputSourceModule.getDefaultValue())) {
                InformationBus.submitEvent(
                    Event(BLOCK_TV_VIEW, inputSourceModule.getInputActiveName())
                )
                return
            }
        }
        CoroutineHelper.runCoroutineWithDelay({
            val applicationMode =
                if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
            tvModule.getActiveChannel(object :IAsyncDataCallback<TvChannel>{
                override fun onFailed(error: Error) {
                    TODO("Not yet implemented")
                }

                override fun onReceive(data: TvChannel) {
                    if (data.isLocked && !playerModule.isChannelUnlocked && tvInputModule.isParentalEnabled() ){
                        playerModule.playbackStatus.value = PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT
                    }else{
                        playerModule.playbackStatus.value = PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT
                    }
                }

            }, applicationMode)
        },1000,Dispatchers.Main)

        hbbTvModule.updateParentalControl()
        return res
    }

    override fun setContentRatingSystemEnabled(
        contentRatingSystem: ContentRatingSystem,
        enabled: Boolean
    ) {
        parentalControlSettingsModule.setContentRatingSystemEnabled(tvInputModule, contentRatingSystem, enabled)
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
        return parentalControlSettingsModule.isRatingBlocked(contentRatingSystem,it)
    }

    override fun isSubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        it: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ): Boolean {
        return parentalControlSettingsModule.isSubRatingEnabled(contentRatingSystem, it, subRating)
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
        parentalControlSettingsModule.setSubRatingBlocked(contentRatingSystem,rating,subRating,data)
    }

    override fun setRelativeRating2SubRatingEnabled(
        contentRatingSystem: ContentRatingSystem,
        data: Boolean,
        rating: ContentRatingSystem.Rating,
        subRating: ContentRatingSystem.SubRating
    ) {
        parentalControlSettingsModule.setRelativeRating2SubRatingEnabled(contentRatingSystem,data,rating,subRating)
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

    override fun rrt5BlockedList(regionPosition :Int, position: Int): HashMap<Int, Int> {
        return parentalControlSettingsModule.rrt5BlockedList(regionPosition, position)
    }

    override fun setSelectedItemsForRRT5Level(
        regionIndex: Int,
        dimIndex: Int,
        levelIndex: Int
    ) {
        parentalControlSettingsModule.setSelectedItemsForRRT5Level(regionIndex,
            dimIndex,
            levelIndex)
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
        getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {}
            override fun onReceive(data: TvChannel) {

                // Clearing the favorites of the TvChannel
                favoriteModule.updateFavoriteItem(FavoriteItem(0,FavoriteItemType.TV_CHANNEL,null,tvChannel, arrayListOf()),object :IAsyncCallback{
                    override fun onFailed(error: Error) {
                        // Ignore
                    }

                    override fun onSuccess() {
                        // Ignore
                    }
                })

                val isDeleted = tvModule.deleteChannel(tvChannel)
                if(isDeleted && data.channelId == tvChannel.channelId){
                    val allChannelList = tvModule.getBrowsableChannelList()
                    if (allChannelList.isEmpty()) {
                        onBroadcastChannelListEmpty()
                    } else {
                        //if deleted channel is active channel play next channel
                        tvModule.nextChannel(object : IAsyncCallback {
                            override fun onSuccess() {}
                            override fun onFailed(error: Error) {
                            }
                        })
                    }
                }
            }
        })
    }

    override fun getSignalAvailable() : Boolean {
        return tvModule.isSignalAvailable()
    }

    override fun onSwapChannelSelected(
        firstChannel: TvChannel,
        secondChannel: TvChannel,
        previousPosition: Int,
        newPosition: Int
    ) {
        val ret = preferenceChannelsModule.swapChannel(firstChannel, secondChannel, previousPosition, newPosition)
        if(ret){
            getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {}
                override fun onReceive(data: TvChannel) {
                    //since we are swapping channels we need to swap active channel as well if either of them is active channel
                    if(data.channelId == firstChannel.channelId){
                        tvModule.storeActiveChannel(secondChannel)
                    } else if(data.channelId == secondChannel.channelId){
                        tvModule.storeActiveChannel(firstChannel)
                    }
                    tvModule.updateDesiredChannelIndex()
                }
            })
        }
    }

    override fun onMoveChannelSelected(
        moveChannelList: ArrayList<TvChannel>,
        previousPosition: Int,
        newPosition: Int,
        channelMap: HashMap<Int, String>
    ) {
        preferenceChannelsModule.moveChannel(moveChannelList, previousPosition, newPosition, channelMap)
        //after move channels we should update the active channel,
        // as index of active channel might go to different place.
        tvModule.storeActiveChannel(activeChannel)
    }

    override fun onClearChannelSelected() {
        favoriteModule.clearFavourites()
        preferenceChannelsModule.deleteAllChannels()
        onBroadcastChannelListEmpty()
    }

    private fun onBroadcastChannelListEmpty(){
        utilsModule.clearChannelidsPref()
        playerModule.stop()

        if (getPreferenceDeepLink() != null) {
            ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            getAvailablePreferenceTypes(
                object : IAsyncDataCallback<List<CategoryItem>> {
                    override fun onFailed(error: Error) {
                    }

                    @RequiresApi(Build.VERSION_CODES.P)
                    override fun onReceive(data: List<CategoryItem>) {
                        val sceneId =
                            ReferenceApplication.worldHandler?.active?.id
                        val sceneInstanceId =
                            ReferenceApplication.worldHandler?.active?.instanceId
                        val sceneData = sceneId?.let {
                            sceneInstanceId?.let { it1 ->
                                HomeSceneData(it, it1, data.size)
                            }
                        }
                        sceneData?.initialFilterPosition = data.size
                        sceneData?.focusToCurrentEvent = true

                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                            Action.SHOW_OVERLAY, sceneData
                        )
                    }
                }
            )

        }
        InformationBus.submitEvent(Event(REFRESH_FOR_YOU))
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int) {
        closedCaptionModule?.saveUserSelectedCCOptions(ccOptions, newValue)
    }

    override fun resetCC() {
        closedCaptionModule?.resetCC()
    }

    override fun setCCInfo() {
        closedCaptionModule?.setCCInfo()
    }

    override fun setCCWithMute(isEnabled: Boolean) {
        closedCaptionModule?.setCCWithMute(isEnabled, context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
    }

    override fun lastSelectedTextOpacity(position: Int) {
        utilsModule.setPrefsValue("TEXT_OPACITY",position)
    }

    override fun isCCTrackAvailable(): Boolean? {
        return closedCaptionModule?.isCCTrackAvailable()
    }

    override fun onChannelClicked() {
        settingsOpened = true
        try {
            var intent = Intent()
            if (BuildConfig.FLAVOR == "refplus5" || BuildConfig.FLAVOR == "mal_service") {
                intent.setClassName(SETTINGS_PACKAGE, SETTINGS_CHANNEL_ACTIVITY)
            } else {
                intent = Intent("android.settings.CHANNELS_SETTINGS")
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext().startActivity(intent)
        } catch (e: Exception) {
            // Stop live playback and show channels scan (FtiSelectInputScanScene)
            playerModule.stop()
            context!!.runOnUiThread {

                worldHandler!!.triggerAction(id, Action.HIDE)
                val sceneData = SceneData(id, instanceId)
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
                    Action.SHOW,
                    sceneData
                )
            }
        }
    }

    override fun onBlueMuteChanged(isEnabled: Boolean) {
        utilsModule.enableBlueMute(isEnabled)
        InformationBus.submitEvent(Event(Events.BLUE_MUTE_ENABLED))
    }

    override fun noSignalPowerOffChanged(isEnabled: Boolean) {
        utilsModule.noSignalPowerOffChanged(isEnabled)
        InformationBus.submitEvent(Event(Events.NO_SIGNAL_POWER_OFF_ENABLED))
    }

    override fun noSignalPowerOffTimeChanged() {
        InformationBus.submitEvent(Event(Events.NO_SIGNAL_POWER_OFF_TIME_CHANGED))
    }

    override fun getUtilsModule(): UtilsInterface {
        return utilsModule
    }

    override fun isLcnEnabled(): Boolean {
        return tvModule.isLcnEnabled()
    }

    override fun enableLcn(enable: Boolean) {
        return tvModule.enableLcn(enable)
    }

    override fun getCiPlusInterface(): CiPlusInterface {
        return ciPlusModule
    }

    override fun storePrefsValue(tag: String, defValue: String) {
        utilsModule.setPrefsValue(tag,defValue)
    }

    override fun isClosedCaptionEnabled(): Boolean {
        return closedCaptionModule?.isClosedCaptionEnabled()!!
    }

    override fun getIsAudioDescription(type: Int): Boolean {
        return playerModule.getIsAudioDescription(type)
    }

    override fun getIsDolby(type: Int): Boolean {
        return playerModule.getIsDolby(type)
    }

    override fun isHOH(type: Int): Boolean {
        return playerModule.getIsCC(type)
    }

    override fun isTeleText(type: Int): Boolean {
        return playerModule.getTeleText(type)
    }

    override fun onPictureSettingsClicked() {
        settingsOpened = true

        try {
            context!!.runOnUiThread {
                worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }
            var intent = Intent()
            intent.setClassName(
                SETTINGS_PACKAGE,
                "com.android.tv.settings.partnercustomizer.picture.PictureActivity"
            )
            if (platformOsInterface.getPlatformName().contains("RealTek") || BuildConfig.FLAVOR == "refplus5" || BuildConfig.FLAVOR == "mal_service") {
                intent  = Intent("android.settings.DISPLAY_SETTINGS")
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPictureSettingsClicked: ${e.printStackTrace()}")
        }
    }

    override fun onScreenSettingsClicked() {
        settingsOpened = true

        try {
            context!!.runOnUiThread {
                worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }
            var intent = Intent()
            if (BuildConfig.FLAVOR == "rtk")
                intent = Intent("android.settings.SCREEN_SETUP")
            else if (BuildConfig.FLAVOR == "refplus5" || BuildConfig.FLAVOR == "mal_service") {
                intent  = Intent("android.settings.oem.DISPLAYMODE_SETTINGS")
            }else{
                intent.setClassName(
                    "com.android.tv.settings",
                    "com.android.tv.settings.partnercustomizer.picture.PictureActivity"
                )
            }

            val bundlePic = Bundle()
            bundlePic.putString("hot_key", "picture_format")
            intent.putExtras(bundlePic)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onScreenSettingsClicked: ${e.printStackTrace()}")
        }
    }

    override fun onSoundSettingsClicked() {
        settingsOpened = true

        try {
            var intent = Intent()
            context!!.runOnUiThread {
                worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }
            if (platformOsInterface.getPlatformName().contains("RealTek")) {
                intent = Intent("com.android.tv.action.VIEW_SOUND_SETTINGS")
            }
            else if(platformOsInterface.getPlatformName().contains("RefPlus_5.0")|| BuildConfig.FLAVOR == "mal_service"){
                intent  = Intent("com.android.tv.action.VIEW_SOUND_SETTINGS")
            }else{
                 intent.setClassName(
                     SETTINGS_PACKAGE,
                     "com.android.tv.settings.device.sound.SoundActivity"
                 )
             }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSoundSettingsClicked: ${e.printStackTrace()}")
        }
    }

    override fun disableCCInfo() {
        closedCaptionModule?.disableCCInfo()
    }

    override fun getAudioChannelInfo(type: Int): String {
        var audioChannelIdx = playerModule.getAudioChannelIndex(type)
        return if (audioChannelIdx != -1)
            Utils.getAudioChannelStringArray()[audioChannelIdx]
        else ""
    }
    override fun getAudioFormatInfo(): String {
        return playerModule.getAudioFormat()
    }
    override fun getVideoResolution(): String {
        return playerModule.getVideoResolution()
    }

    private fun addForYouBroadcastChannelsRow(callback: (ArrayList<RailItem>)-> Unit) {
        var rails = arrayListOf<RailItem>()
        if (!isRegionSupported()) {

            var channels = tvModule.getBrowsableChannelList(ApplicationMode.DEFAULT)
            if (channels.isNotEmpty()) {
                var broadcastChannels = mutableListOf<Any>()
                broadcastChannels.addAll(channels)
                var rail = RailItem(
                    -1,
                    ConfigStringsManager.getStringById("broadcast_tv_channels"),
                    broadcastChannels,
                    RailItem.RailItemType.BROADCAST_CHANNEL
                )
                rails.add(rail)

                var index = AtomicInteger(0)
                val size = channels.size
                var forYouList = mutableListOf<Any>()
                channels.shuffle()
                channels.forEach { channel ->
                    epgModule.getCurrentEvent(channel, object : IAsyncDataCallback<TvEvent> {
                        override fun onFailed(error: Error) {
                            if (index.incrementAndGet() == size) {
                                if (forYouList.isNotEmpty()) {
                                    var forYouRail = RailItem(
                                        0,
                                        ConfigStringsManager.getStringById("for_you"),
                                        forYouList,
                                        RailItem.RailItemType.EVENT
                                    )
                                    rails.add(forYouRail)
                                }
                                callback.invoke(rails)
                            }
                        }

                        override fun onReceive(data: TvEvent) {
                            if (index.get() < size) {
                                forYouList.add(data)
                            }

                            if (index.incrementAndGet() == size) {
                                var forYouRail = RailItem(
                                    0,
                                    ConfigStringsManager.getStringById("for_you"),
                                    forYouList,
                                    RailItem.RailItemType.EVENT
                                )
                                rails.add(forYouRail)
                                callback.invoke(rails)
                            }
                        }
                    })
                }
            }
        } else {
            callback.invoke(rails)
        }
    }

    override fun getForYouRails() {
        //refreshTime()
        addForYouBroadcastChannelsRow() { rails->
            getForYouData(rails)
        }
    }

    private fun getForYouData(rails: ArrayList<RailItem>) {
        forYouModule.getForYouRails(
            object : IAsyncDataCallback<ArrayList<RailItem>> {
                override fun onFailed(error: Error) {
                    ReferenceApplication.runOnUiThread {
                        if (homeScene.forYouWidget != null) {
                            var list = ArrayList<RailItem>()
                            list.addAll(rails.distinct())
                            homeScene.forYouWidget?.refresh(list)
                        }
                    }
                }

                override fun onReceive(data: ArrayList<RailItem>) {
                    ReferenceApplication.runOnUiThread {
                        if (homeScene.forYouWidget != null) {
                            data.forEach { railItem->
                                try {
                                    rails.first { railItem.railName == it.railName }
                                } catch (e: NoSuchElementException) {
                                    rails.add(railItem)
                                }
                            }
                            var list = ArrayList<RailItem>()
                            list.addAll(rails.distinct())
                            homeScene.forYouWidget?.refresh(list)
                        }
                    }
                }
            })
    }

    override fun isInWatchList(tvEvent: TvEvent): Boolean {
        return watchlistModule.isInWatchlist(tvEvent)
    }

    override fun isInRecordingList(tvEvent: TvEvent): Boolean {
        return schedulerModule.isInReclist(tvEvent.tvChannel.id, tvEvent.startTime)
    }

    override fun isInFavoriteList(tvChannel: TvChannel): Boolean {
        tvModule.getChannelList().forEach { channel ->
            if (channel.channelId == tvChannel.channelId) {
                return !channel.favListIds.isEmpty()
            }
        }
        return false
    }

    override fun getFavoriteCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
        favoriteModule.getAvailableCategories(callback)
    }

    override fun getFavoriteSelectedItems(tvChannel: TvChannel): ArrayList<String> {
        tvModule.getChannelList().forEach { channel ->
            if (channel.channelId == tvChannel.channelId) {
                return channel.favListIds
            }
        }
        return arrayListOf()
    }

    override fun getRailSize(): Int {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getRailSizeforYouModule.getAvailableRailSize() == ${forYouModule.getAvailableRailSize()} ")
        return forYouModule.getAvailableRailSize()
    }

    override fun isChannelLocked(channelId: Int): Boolean {
        return tvModule.getChannelById(channelId)?.isLocked!!
    }

    override fun dispose() {
        super.dispose()
    }

    private fun showErrorMessage(title: String) {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.TEXT
            sceneData.title = title
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                }

                override fun onPositiveButtonClicked() {
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    private fun setCaptionsEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context!!.contentResolver,
            "accessibility_captioning_enabled", if (enabled) 1 else 0
        )
    }

    override fun getPreferenceDeepLink(): ReferenceWidgetPreferences.DeepLink? {
        (data!! as? HomeSceneData)?.let{
            if (it.openEditChannel) {
                return ReferenceWidgetPreferences.DeepLink(
                    PrefType.PLATFORM,
                    ReferenceWorldHandler.WidgetId.PREFERENCES_SETUP,
                    Pref.SKIP_CHANNEL
                )
            } else if(it.openDeviceInfo) {
                return ReferenceWidgetPreferences.DeepLink(
                    PrefType.PLATFORM,
                    ReferenceWorldHandler.WidgetId.PREFERENCES_PVR_TIMESHIFT,
                    Pref.DEVICE_INFO
                )
            }
        }
        return null
    }

    override fun startCamScan() {
        if (ciPlusModule.isCamActive()) {
            if (ciPlusModule.getMenuListID() != -1 || ciPlusModule.getEnqId() != -1) {
                ciPlusModule.setMMICloseDone()
            }
            ciPlusModule.startCAMScan(
                isTriggeredByUser = true,
                isCanceled = false
            )
        }
    }

    override fun changeCamPin() {
        ReferenceApplication.runOnUiThread{
            var activeScene = ReferenceApplication.worldHandler!!.active
            var sceneData = SceneData(activeScene!!.id, activeScene!!.instanceId)
            ReferenceApplication.worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.CAM_PIN_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun selectMenuItem(position: Int) {
        ciPlusModule.selectMenuItem(position)
    }

    override fun enterMMI(){
        if (ciPlusModule.isCamActive()) {
            if (ciPlusModule.getMenuListID() != -1 || ciPlusModule.getEnqId() != -1) {
                ciPlusModule.setMMICloseDone()
            }
            ciPlusModule.enterMMI()
        }
    }

    override fun deleteProfile(profileName: String) {
        var sceneData = DialogSceneData(id, instanceId)
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title = ConfigStringsManager.getStringById("op_profile_delete")+" $profileName"
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("Sure")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener =
            object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                }

                override fun onPositiveButtonClicked() {
                    ReferenceApplication.runOnUiThread{
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        ciPlusModule.deleteProfile(profileName)
                    }

                }
            }
        ReferenceApplication.runOnUiThread{
            ReferenceApplication.worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }

    }


    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
        textToSpeechModule.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
    }

    override fun stopSpeech() {
        textToSpeechModule.stopSpeech()
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }


    override fun onTimeshiftStatusChanged(enabled: Boolean) {
        setPrefsValue("IS_TIMESHIFT_ENABLED", enabled)
    }

    override fun startOadScan() {
        oadModule.startScan()
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule.getGeneralSettingsInfo(nameOfInfo)
    }

    override fun stopPlayer() {
        //right now player should be stopped
        //when one player is used for all content, player should be muted and not stopped
        playerModule.stop()
    }
    override fun resumePlayer() {
        if(playerModule.playerState == PlayerState.STOPPED) playerModule.resume()
    }

    override fun getCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
    }

    override fun isSubtitleEnabled(): Boolean {
        return utilsModule.getSubtitlesState()
    }
    override fun changeTrailer(vodItem: VODItem?) {
        (scene as HomeSceneVod).videoReadyToPlay(playing = false)
        VodPlayerHelper.pause()
        VodTrailerHelper.changeTrailer(vodItem){
            resumeLiveContent(true)
        }
    }

    override fun onVodItemClicked(type: VODType, contentId: String) {
        VodTrailerHelper.stopVodTrailerPlayer{
            resumeLiveContent(true)
        }
        worldHandler!!.triggerActionWithData(
            when (type) {
                VODType.SERIES -> ReferenceWorldHandler . SceneId . VOD_SERIES_DETAILS_SCENE
                else -> { ReferenceWorldHandler.SceneId.VOD_SINGLE_WORK_DETAILS_SCENE }
            },
            Action.SHOW,
            data = DetailsSceneData(
                ReferenceApplication.worldHandler!!.active!!.id,
                ReferenceApplication.worldHandler!!.active!!.instanceId,
                contentId
            )
        )
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.HIDE)
    }

    override fun doCAMReconfiguration(camTypePreference: CamTypePreference){
        ciPlusModule.doCAMReconfiguration(camTypePreference)
    }

    override fun isFastChannelListEmpty(): Boolean {
        return tvModule.getChannelList(ApplicationMode.FAST_ONLY).isEmpty()
    }

    override fun isTosAccepted(): Boolean {
        return isTosAccepted
    }

    override fun getLockedChannelIdsFromPrefs(): MutableSet<String> {
        return utilsModule.getChannelIdsFromPrefs()
    }
    
    fun resumeLiveContent(mute:Boolean) {
        tvModule.getActiveChannel(
            object : IAsyncDataCallback<TvChannel> {
                override fun onReceive(data: TvChannel) {
                    changeChannelFromTalkback(data)
                    if (mute) muteAudio()
                }

                override fun onFailed(error: Error) {
                }
            },
            applicationMode = if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        )
    }

    override fun checkInternet(): Boolean {
        return if (networkModule.networkStatus.value == null || networkModule.networkStatus.value == NetworkData.NoConnection) {
            false
        } else true
    }

    override fun isScrambled(): Boolean {
        return playerModule.playbackStatus.value == PlaybackStatus.SCRAMBLED_CHANNEL
    }
}
