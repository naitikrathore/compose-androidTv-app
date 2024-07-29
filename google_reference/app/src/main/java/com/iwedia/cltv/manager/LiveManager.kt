package com.iwedia.cltv.manager

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.isInForeground
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.ConfigurableKeysManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.entities.FilterItem
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.*
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.Events.PLAYBACK_STATUS_MESSAGE_IS_LOCKED
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type

import com.iwedia.cltv.receiver.GlobalAppReceiver
import com.iwedia.cltv.scene.channel_list.ChannelListScene
import com.iwedia.cltv.scene.home_scene.HomeSceneBase
import com.iwedia.cltv.scene.home_scene.HomeSceneData
import com.iwedia.cltv.scene.input_scene.InputSelectedSceneData
import com.iwedia.cltv.scene.live_scene.*
import com.iwedia.cltv.scene.player_scene.PlayerSceneData
import com.iwedia.cltv.scene.timeshift.TimeshiftSceneData
import com.iwedia.cltv.scene.zap_digit.DigitZapItem
import com.iwedia.cltv.utils.TvViewScaler
import com.iwedia.cltv.utils.Utils
import core_entities.ScheduledRecording
import kotlinx.coroutines.Dispatchers
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData
import world.WorldHandler

class LiveManager : ReferenceSceneManager, LiveSceneListener {

    private val TAG = javaClass.simpleName
    var shownNoEthMessage = false
    var showNoSignalInputMsg = false

    lateinit var liveDPadUp: LiveSceneKeyDpadUpAction
    lateinit var liveDPadDown: LiveSceneKeyDpadDownAction
    lateinit var liveDPadRight: LiveSceneKeyDpadRightAction
    lateinit var liveDPadLeft: LiveSceneKeyDpadLeftAction
    lateinit var liveOk: LiveSceneKeyOkAction
    lateinit var liveRed: LiveSceneKeyRedAction
    lateinit var liveGreen: LiveSceneKeyGreenAction
    lateinit var liveYellow: LiveSceneKeyYellowAction
    lateinit var liveBlue: LiveSceneKeyBlueAction

    private val SETTINGS_PACKAGE= "com.android.tv.settings"
    private val SETTINGS_SOUND_ACTIVITY = "com.mediatek.tv.settings.displayandsound.sound.SoundActivity"
    private val SETTINGS_SCREEN_MODE_ACTIVITY = "com.mediatek.tv.settings.displayandsound.screen.ScreenModeActivity"

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showBanner(tvChannel: TvChannel) {
        val currentActiveScene = ReferenceApplication.worldHandler!!.active
        val isHomeSceneVisible =
            ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.HOME_SCENE)
        val isSearchSceneVisible =
            ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.SEARCH)
        val isChannelListVisible =
            ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.CHANNEL_SCENE)
        val isVodBannerVisible =
            ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.VOD_BANNER_SCENE)
        val isVodPlaying =
            ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.VOD
        val isVodActive = HomeSceneManager.IS_VOD_ACTIVE

        var isFastZapBannerActive = (scene as LiveScene).isFastZapBannerActive()

        if (isFastZapBannerActive && getApplicationMode() == ApplicationMode.DEFAULT.ordinal) {
            (scene as LiveScene).hideFastZapBanner()
            isFastZapBannerActive = false
        }

        if (BuildConfig.FLAVOR.contains("mal_service") && isFastZapBannerActive) {
            (scene as LiveScene).updateFastZapBanner()
            return
        }

        /*When timeshift is active and user goes to home and comeback, timeshift banner needs to be shown instead
        * of zapbanner*/
        if(timeshiftModule.isTimeShiftActive) {
            showPlayer()
            return
        }
        // Do not show banner if fast zap banner is active or home or search scene or channel list is visible
        if (isFastZapBannerActive || isHomeSceneVisible || isSearchSceneVisible || isChannelListVisible || isVodBannerVisible || isVodPlaying || isVodActive) {
            return
        }
        if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) {
            (scene as LiveScene).showFastZapBanner()
            return
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "showBanner: ########### KEY --- SHOW ZAP BANNER PASS CHECK")
        if (currentActiveScene!!.id != ReferenceWorldHandler.SceneId.ZAP_BANNER &&
            !isHomeSceneVisible
        ) {
            if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE)
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.LIVE,
                    Action.SHOW
                )
            } else if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.MMI_MENU_SCENE) {
                return
            } else if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE) {
                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            } else if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.CHANNEL_SCENE) {
                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            } else if (!BackFromPlayback.zapFromHomeOrSearch) {
                //if details scene was previous scene, do not destroy home scene (it saves focused event)
                if (data != null && data!!.previousSceneId != ReferenceWorldHandler.SceneId.DETAILS_SCENE) {
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(
                        ReferenceWorldHandler.SceneId.LIVE
                    )
                }
            }
            runOnUiThread {
                var activeScene = ReferenceApplication.worldHandler!!.active
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "showBanner: ########### KEY --- SHOW ZAP BANNER TRIGGER ACTION ${activeScene?.id} ${activeScene?.scene} ${activeScene?.instanceId}"
                )
                if (activeScene != null && !(ReferenceApplication.getActivity() as MainActivity).openedGuideFromIntent) {
                    if (ReferenceApplication.worldHandler?.active?.scene?.id == id) {
                        runOnUiThread {
                            if (ReferenceApplication.worldHandler!!.getVisibles()
                                    .contains(ReferenceWorldHandler.SceneId.ZAP_BANNER)
                            ) {
                                ReferenceApplication.worldHandler!!.triggerAction(
                                    ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                    Action.DESTROY
                                )
                            }
                            var sceneData =
                                SceneData(activeScene!!.id, activeScene.instanceId, tvChannel)
                            ReferenceApplication.worldHandler!!.triggerActionWithData(
                                ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                Action.SHOW_OVERLAY, sceneData
                            )
                        }
                    }
                }
                (ReferenceApplication.getActivity() as MainActivity).openedGuideFromIntent = false
            }
        }
    }

    /**
     * Platform modules
     */
    private var tvModule: TvInterface
    private var textToSpeechModule: TTSInterface
    private val playerModule: PlayerInterface
    private val epgModule: EpgInterface
    private val pvrModule: PvrInterface
    private val schedulerModule: SchedulerInterface
    private val watchlistModule: WatchlistInterface
    private val timeshiftModule: TimeshiftInterface
    private val utilsModule: UtilsInterface
    private val preferenceModule: PreferenceInterface
    private var isInitialized = false
    private val inputSourceModule: InputSourceInterface
    private val timeModule: TimeInterface
    private val closedCaptionModule: ClosedCaptionInterface
    private val parentalControlSettingsModule: ParentalControlSettingsInterface
    private var categoryModule: CategoryInterface
    private var generalConfigModule: GeneralConfigInterface
    private val ttxModule: TTXInterface
    private val hbbTvModule: HbbTvInterface
    private val platformOSModule: PlatformOsInterface


    //Is scene paused flag used to show home scene after pressing home key and returning to the app
    private var appStopped = false
    private var isAppStop = false
    private var noSignal = false

    private var channelNameLiveTab = ""

    @RequiresApi(Build.VERSION_CODES.R)
    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        tvModule: TvInterface,
        playerModule: PlayerInterface,
        epgModule: EpgInterface,
        pvrModule: PvrInterface,
        schedulerModule: SchedulerInterface,
        watchlistModule: WatchlistInterface,
        utilsModule: UtilsInterface,
        timeshiftModule: TimeshiftInterface,
        preferenceModule: PreferenceInterface,
        inputSourceModule: InputSourceInterface,
        timeModule: TimeInterface,
        closedCaptionModule: ClosedCaptionInterface,
        parentalControlSettingsModule: ParentalControlSettingsInterface,
        categoryModule: CategoryInterface,
        generalConfigModule: GeneralConfigInterface,
        ttxModule: TTXInterface,
        hbbTvModule: HbbTvInterface,
        textToSpeechModule: TTSInterface,
        platformOSModule: PlatformOsInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.LIVE
    ) {
        this.tvModule = tvModule
        this.playerModule = playerModule
        this.epgModule = epgModule
        this.pvrModule = pvrModule
        this.schedulerModule = schedulerModule
        this.watchlistModule = watchlistModule
        this.timeshiftModule = timeshiftModule
        this.utilsModule = utilsModule
        this.preferenceModule = preferenceModule
        this.inputSourceModule = inputSourceModule
        this.timeModule = timeModule
        this.closedCaptionModule = closedCaptionModule
        this.parentalControlSettingsModule = parentalControlSettingsModule
        this.categoryModule = categoryModule
        this.generalConfigModule = generalConfigModule
        this.ttxModule = ttxModule
        this.hbbTvModule = hbbTvModule
        this.textToSpeechModule = textToSpeechModule
        this.platformOSModule = platformOSModule
        isScreenFlowSecured = false


        registerGenericEventListener(Events.PVR_RECORDING_STARTED)
        registerGenericEventListener(Events.PVR_RECORDING_STARTING)
        registerGenericEventListener(Events.PVR_RECORDING_FINISHING)
        registerGenericEventListener(Events.PVR_RECORDING_SHOULD_STOP)
        registerGenericEventListener(Events.SHOW_PVR_BANNER)
        registerGenericEventListener(Events.SHOW_STOP_RECORDING_DIALOG)
        registerGenericEventListener(Events.CHANNEL_CHANGED)
        registerGenericEventListener(Events.NO_ETHERNET_EVENT)
        registerGenericEventListener(Events.PLAYBACK_SHOW_BLACK_OVERLAY)
        registerGenericEventListener(Events.PLAYBACK_HIDE_BLACK_OVERLAY)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_NONE)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_IS_SCRAMBLED)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_IS_LOCKED)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_LOCKED)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_PARENTAL)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_IS_MESSAGE)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL)
        registerGenericEventListener(Events.PLAYBACK_STATUS_MESSAGE_NO_PLAYBACK)
        registerGenericEventListener(Events.IS_EAS_PLAYING)
        registerGenericEventListener(Events.PLAYBACK_STARTED)
        registerGenericEventListener(Events.NO_PLAYBACK)
        registerGenericEventListener(Events.PLAYER_TIMEOUT)
        registerGenericEventListener(Events.NO_CHANNELS_AVAILABLE)
        registerGenericEventListener(Events.WAITING_FOR_CHANNEL)
        registerGenericEventListener(Events.VIDEO_RESOLUTION_UNAVAILABLE)
        registerGenericEventListener(Events.ETHERNET_EVENT)
        registerGenericEventListener(Events.PVR_RECORDING_FINISHED)
        registerGenericEventListener(Events.RCU_DIGIT_PRESSED)
        registerGenericEventListener(Events.ACTIVE_CHANNEL_DELETED_EVENT)
        registerGenericEventListener(Events.CHANNEL_LIST_IS_EMPTY)
        registerGenericEventListener(Events.ACTIVE_SERVICE_NOT_RUNNING)
        registerGenericEventListener(Events.ACTIVE_SERVICE_REPLACEABLE)
        registerGenericEventListener(Events.ACTIVE_SERVICE_RUNNING)
        registerGenericEventListener(Events.DISK_READY_EVENT)
        registerGenericEventListener(Events.CHANNEL_LIST_UPDATED)
        registerGenericEventListener(Events.UPDATED_CONFIG_KEYS)
        registerGenericEventListener(Events.SHOW_PLAYER)
        registerGenericEventListener(Events.PVR_PLAYBACK_STARTED)
        registerGenericEventListener(Events.FOCUS_ON_BACK_PRESS)
        registerGenericEventListener(Events.BLUE_MUTE_ENABLED)
        registerGenericEventListener(Events.RCU_PERIOD_PRESSED)
        registerGenericEventListener(Events.NO_AV_OR_AUDIO_ONLY_CHANNEL)
        registerGenericEventListener(Events.APP_STOPPED)
        registerGenericEventListener(Events.TALKBACK_CLOSE_ZAP_BANNER)
        registerGenericEventListener(Events.SUBTITLE_TRACK_SELECTED)
        registerGenericEventListener(Events.SHOW_RECORDING_TIME_SHIFT_CONFLICT_DIALOG)
        registerGenericEventListener(Events.SHOW_WATCHLIST_TIME_SHIFT_CONFLICT_DIALOG)
        registerGenericEventListener(Events.CHANNEL_LIST_SWITCHED)
        registerGenericEventListener(Events.DIRECT_TUNE_FORCE_PLAYBACK)
        registerGenericEventListener(Events.GOOGLE_PIN_CORRECT)
        registerGenericEventListener(Events.VOD_PLAYBACK_STARTED)

        playerModule.playbackStatus.observeForever { playbackStatus->
            if (inputSourceModule.getDefaultValue() == "TV") {
                runOnUiThread {
                    if (playbackStatus == PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT) {
                        scene?.refresh(PLAYBACK_STATUS_MESSAGE_IS_LOCKED)
                    } else if (playbackStatus == PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT) {
                        scene?.refresh(Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_LOCKED)
                        //on unLocking channel lock need to check if parental lock is there, if it is there show parental lock
                        if (isParentalBlockingActive()) scene?.refresh(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
                    }
                }
            }
        }
    }

    private fun getInputValues() {
        inputSourceModule.inputChanged.observeForever {
            var defaultValue = inputSourceModule.getDefaultValue()
            scene!!.refresh(defaultValue)
        }
    }

    override fun getDefaultInputValue(): String {
        return inputSourceModule.getDefaultValue()
    }

    override fun isScrambled(): Boolean {
        return playerModule.wasScramble
    }

    override fun isQuietTune(): Boolean {
        return playerModule.getQuietTuneEnabled()
    }

    override fun setIsOnLockScreen(isOnLockScreen: Boolean) {
        playerModule.isOnLockScreen = isOnLockScreen
    }

    override fun getIsOnLockScreen() : Boolean {
        return playerModule.isOnLockScreen
    }

    override fun onTimeChanged(currentTime: Long) {}

    override fun initConfigurableKeys() {

        ConfigurableKeysManager.setup()

        var livedUpData = ConfigurableKeysManager.getConfigurableKey("liveDPadUp")
        if (livedUpData == null) {
            ConfigurableKeysManager.setup()
            livedUpData = ConfigurableKeysManager.getConfigurableKey("liveDPadUp")
        }

        if (livedUpData != null) {
            liveDPadUp = LiveSceneKeyDpadUpAction(
                this,
                livedUpData.description.toInt(),
                livedUpData.keyActionType
            )
        }

        var livedDownData = ConfigurableKeysManager.getConfigurableKey("liveDPadDown")
        if (livedDownData != null) {
            liveDPadDown = LiveSceneKeyDpadDownAction(
                this,
                livedDownData.description.toInt(),
                livedDownData.keyActionType
            )
        }

        var livedRightData = ConfigurableKeysManager.getConfigurableKey("liveDPadRight")
        if (livedRightData != null) {
            liveDPadRight = LiveSceneKeyDpadRightAction(
                this,
                livedRightData.description.toInt(),
                livedRightData.keyActionType
            )
        }

        var livedLeftData = ConfigurableKeysManager.getConfigurableKey("liveDPadLeft")

        if (livedLeftData != null) {
            liveDPadLeft = LiveSceneKeyDpadLeftAction(
                this, livedLeftData.description.toInt(),
                livedLeftData.keyActionType
            )
        }

        var livedOkData = ConfigurableKeysManager.getConfigurableKey("liveOk")
        if (livedOkData != null) {
            liveOk = LiveSceneKeyOkAction(
                this, livedOkData.description.toInt(),
                livedOkData.keyActionType
            )
        }

        var livedRedData = ConfigurableKeysManager.getConfigurableKey("liveRed")
        if (livedRedData != null) {
            liveRed = LiveSceneKeyRedAction(
                this, livedRedData.description.toInt(),
                livedRedData.keyActionType
            )
        }

        var livedGreenData = ConfigurableKeysManager.getConfigurableKey("liveGreen")
        if (livedGreenData != null) {
            liveGreen = LiveSceneKeyGreenAction(
                this, livedGreenData.description.toInt(),
                livedGreenData.keyActionType
            )
        }

        var livedYellowData = ConfigurableKeysManager.getConfigurableKey("liveYellow")
        if (livedYellowData != null) {
            liveYellow = LiveSceneKeyYellowAction(
                this, livedYellowData.description.toInt(),
                livedYellowData.keyActionType
            )
        }

        var livedBlueData = ConfigurableKeysManager.getConfigurableKey("liveBlue")
        if (livedBlueData != null) {
            liveBlue = LiveSceneKeyBlueAction(
                this, livedBlueData.description.toInt(),
                livedBlueData.keyActionType
            )
        }


    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun resolveConfigurableKey(keyCode: Int, action: Int): Boolean {
        //app crashed when we call different scenes before proper initialization of live scene.
        if (!isInitialized) return true
        if (inputSourceModule.getDefaultValue().contains("TV")) {
            if ((scene as LiveScene).isFastZapBannerActive()) {
                return true
            }
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isRecordingInProgress()) {
                    showPvrBanner()
                } else {
                    //Show zap banner
                    tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: kotlin.Error) {}
                        override fun onReceive(activeChannel: TvChannel) {
                            if (worldHandler?.active?.id != ReferenceWorldHandler.SceneId.ZAP_BANNER) {
                                if (ReferenceApplication.worldHandler!!.playbackState != ReferenceWorldHandler.PlaybackState.RECORDING) {
                                    runOnUiThread {
                                        ReferenceApplication.worldHandler!!.active?.let {
                                            var sceneData =
                                                SceneData(it.id, it.instanceId, activeChannel)
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
                    })
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (liveDPadRight.handleKey(action)) {
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_12,
            KeyEvent.KEYCODE_YEN-> {
                if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) {
                    (scene as LiveScene).showFastZapBanner()
                } else  {
                    showInfoBanner()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) {
                    BackFromPlayback.onOkPressed(true)
                } else  {
                    showChannelList()
                }
                return true
            }

            KeyEvent.KEYCODE_PROG_RED -> {
                if (liveRed.handleKey(action)) {
                    return true
                }
            }

            KeyEvent.KEYCODE_PROG_GREEN -> {
                if (liveGreen.handleKey(action)) {
                    return true
                }
            }

            KeyEvent.KEYCODE_PROG_YELLOW -> {
                if (liveYellow.handleKey(action)) {
                    return true
                }
            }

            KeyEvent.KEYCODE_PROG_BLUE -> {
                if (liveBlue.handleKey(action)) {
                    return true
                }
            }

            KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK -> {
                onSoundSettingsClicked()
                return true
            }

            KeyEvent.KEYCODE_TV_ZOOM_MODE -> {
                onScreenSettingsClicked()
                return true
            }
        }

        return false
    }

    override fun createScene() {
        scene = LiveScene(context!!, this)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onSceneInitialized() {
        tvModule.getLockedChannelListAfterOta()
        //Get channel list and start initial playback
        if (inputSourceModule.getDefaultValue() == "TV") {
            var applicationMode =
                if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
            if (tvModule.getChannelList(applicationMode).size > 0) {
                if (!inputSourceModule.isBlock(inputSourceModule.getDefaultValue())) {
                    if (ForYouInterface.ENABLE_FAST_DATA) {
                        var startChannelFromLiveTabPref = utilsModule.getPrefsValue(Constants.SharedPrefsConstants.STARTED_CHANNEL_FROM_LIVE_TAB_TAG, false) as Boolean
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSceneInitialized get STARTED_CHANNEL_FROM_LIVE_TAB_TAG $startChannelFromLiveTabPref")
                        if (!startChannelFromLiveTabPref) {
                            if (ReferenceApplication.isRegionSupported || ((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()) {
                                showHome()
                            }
                            BackFromPlayback.setLiveSceneFromLiveHomeState(false)
                        }else{
                            BackFromPlayback.setLiveSceneFromLiveHomeState(true)
                            utilsModule.setPrefsValue(Constants.SharedPrefsConstants.STARTED_CHANNEL_FROM_LIVE_TAB_TAG, false)
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSceneInitialized set STARTED_CHANNEL_FROM_LIVE_TAB_TAG false")

                        }
                    }
                    if(inputSourceModule.getDefaultValue() == "TV") {
                        if (worldHandler?.active?.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE && !ReferenceApplication.isCecTuneFromIntro) {
                            tvModule.startInitialPlayback(object : IAsyncCallback {
                                override fun onSuccess() {
                                    TvViewScaler.restore()
                                }

                                override fun onFailed(error: Error) {}
                            }, applicationMode)
                        }
                    }
                }
                //fixed crash
                Handler().post {
                    isInitialized = true
                }
            }
        } else {
            if(worldHandler?.active?.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE) {
                inputSourceModule.handleInputSource(
                    inputSourceModule.getDefaultValue(),
                    inputSourceModule.getDefaultURLValue()
                )
            }
            //fixed crash
            Handler().post {
                isInitialized = true
            }
        }
        getInputValues()
        categoryModule.getAvailableFilters(object : IAsyncDataCallback<ArrayList<Category>> {
            override fun onFailed(error: Error) {}
            override fun onReceive(data: ArrayList<Category>) {
                for(category in data){
                    if(category.name == categoryModule.getActiveCategory()){
                        val activeCategoryId = category.id
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
                        break
                    }
                }
            }
        })
    }

    override fun showZapBanner() {
        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
            InformationBus.submitEvent(
                Event(
                    Events.SHOW_STOP_RECORDING_DIALOG,
                    true
                )
            )
        } else {
            //TODO DEJAN check is this needed
            if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
                tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(tvChannel: TvChannel) {
                        //showBanner(tvChannel)
                    }
                })
            }
        }
    }

    override fun isRecordingInProgress(): Boolean {
        return pvrModule.isRecordingInProgress()
    }

    override fun isTimeShiftInProgress(): Boolean {
        return timeshiftModule.isTimeShiftActive
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule.getGeneralSettingsInfo(nameOfInfo)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startRecording(tvEvent: TvEvent) {

        if(playerModule.isOnLockScreen){
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            return
        }

        if (utilsModule.getUsbDevices().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
            return
        }
        if (utilsModule.getPvrStoragePath().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
            InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
            return
        }
        if (!utilsModule.isUsbWritableReadable()) {
            showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
            return
        }
        if (!utilsModule.isUsbFreeSpaceAvailable()) {
            showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
            return
        }

        pvrModule.startRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {}

            override fun onSuccess() {}
        })
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun setSpeechTextForSelectableView(
        vararg text: String,
        importance: SpeechText.Importance,
        type: Type,
        isChecked: Boolean
    ) {
        textToSpeechModule.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun channelUp() {
        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
            InformationBus.submitEvent(
                Event(
                    Events.SHOW_STOP_RECORDING_DIALOG,
                    true
                )
            )
        } else {
            if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
                var applicationMode =
                    if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
                if (applicationMode == ApplicationMode.FAST_ONLY) {
                    FastZapBannerDataProvider.channelUp {  }
                } else {
                    tvModule.nextChannel(object : IAsyncCallback {
                        override fun onSuccess() {}

                        override fun onFailed(error: Error) {
                        }
                    }, applicationMode)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun lastActiveChannel() {
        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
            InformationBus.submitEvent(
                Event(
                    Events.SHOW_STOP_RECORDING_DIALOG,
                    true
                )
            )
        } else {
            if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
                var applicationMode =
                    if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT

                categoryModule.setActiveCategory("All")
                categoryModule.setActiveEpgFilter(0)
                tvModule.updateLaunchOrigin(0, "", "", "")

                tvModule.getLastActiveChannel(object : IAsyncCallback {
                    override fun onSuccess() {
                    }

                    override fun onFailed(error: Error) {
                    }
                }, applicationMode)


            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun channelDown() {
        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
            InformationBus.submitEvent(
                Event(
                    Events.SHOW_STOP_RECORDING_DIALOG,
                    false
                )
            )
        } else {
            if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
                var applicationMode =
                    if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
                if (applicationMode == ApplicationMode.FAST_ONLY) {
                    FastZapBannerDataProvider.channelDown {  }
                } else {
                    tvModule.previousChannel(object : IAsyncCallback {
                        override fun onSuccess() {
                        }

                        override fun onFailed(error: Error) {
                        }
                    }, applicationMode)
                }
            }
        }
    }

    @Volatile
    private var requestedChannelList = false
    @RequiresApi(Build.VERSION_CODES.R)
    override fun showChannelList() {
        if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) {
            return
        }
        if (ReferenceApplication.worldHandler!!.active?.id == id && !requestedChannelList && !requestedInfoBanner) {
            requestedChannelList = true
            runOnUiThread {
                val sceneData = SceneData(id, instanceId)
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.CHANNEL_SCENE, Action.SHOW_OVERLAY, sceneData
                )
                requestedChannelList = false
            }
        }
    }

    @Volatile
    private var requestedInfoBanner = false
    @RequiresApi(Build.VERSION_CODES.R)
    override fun showInfoBanner() {

        if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) {
            return
        }
        if (ReferenceApplication.worldHandler!!.active?.id == id && !requestedInfoBanner &&
            ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.INFO_BANNER && !requestedChannelList
        ) {
            requestedInfoBanner = true
            runOnUiThread {
                var sceneData = SceneData(id, instanceId)
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.INFO_BANNER,
                    Action.SHOW_OVERLAY, sceneData
                )

                requestedInfoBanner = false
            }
        }
    }

    @Volatile
    private var requestedHome = false
    private var requestedPreference = false

    /**
    * Displays the home scene with an optional specified focus position.
    *
    * @param focusPosition The position within the home scene to initially focus on.
    *                      If null, the default focus position will be used.
    */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun showHome(showHomeType: LiveSceneListener.ShowHomeType) {
        if (!ReferenceApplication.isOkClickedOnSetUp && ReferenceApplication.worldHandler!!.active?.id == id && !requestedHome && !ReferenceApplication.isCecTuneFromIntro) {
            requestedHome = true
            runOnUiThread {

                when (showHomeType) {
                    LiveSceneListener.ShowHomeType.SET_FOCUS_TO_HOME_IN_TOP_MENU -> {
                        var sceneData = SceneData(id, instanceId)
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                            Action.SHOW_OVERLAY, sceneData
                        )
                    }
                    LiveSceneListener.ShowHomeType.SET_FOCUS_TO_LIVE_OR_BROADCAST_IN_TOP_MENU -> {
                        BackFromPlayback.onOkPressed()
                    }
                }
                requestedHome = false
            }
        }

    }

    override fun isAccessibilityEnabled(): Boolean {
        return utilsModule.isAccessibilityEnabled()
    }

    override fun showPreferences() {
        if (ReferenceApplication.worldHandler!!.active?.id == id && !requestedPreference) {
            runOnUiThread {
                var sceneId = ReferenceApplication.worldHandler?.active?.id
                var sceneInstanceId =
                    ReferenceApplication.worldHandler?.active?.instanceId

                if (sceneId != ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE) {
                    requestedPreference = true
                    var sceneData =
                        SceneData(sceneId!!, sceneInstanceId!!)
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE,
                        Action.SHOW_OVERLAY, sceneData
                    )
                    requestedPreference = false
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun launchInputSelectedScene() {
        if (ReferenceApplication.worldHandler?.active?.id == ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE) {
            ReferenceApplication.worldHandler?.triggerAction(
                ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE,
                Action.DESTROY
            )
        }
        val sceneId = ReferenceApplication.worldHandler?.active?.id
        val sceneInstanceId =
            ReferenceApplication.worldHandler?.active?.instanceId
        val sceneData = InputSelectedSceneData(
            sceneId!!,
            sceneInstanceId!!
        )
        sceneData.inputType = inputSourceModule.getInputActiveName()
        var inputResolutionItem = inputSourceModule.getResolutionDetailsForUI()
        sceneData.inputIcon = inputResolutionItem.iconValue
        sceneData.inputPixelValue = inputResolutionItem.pixelValue
        sceneData.inputHdrValue = inputResolutionItem.hdrValue
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE,
            Action.SHOW_OVERLAY, sceneData
        )
    }


    override fun showTvPreferences() {
        if (ReferenceApplication.worldHandler!!.active?.id == id && !requestedHome) {
            requestedHome = true
            runOnUiThread {
                var position = 3
                ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                var sceneData =
                    SceneData(
                        id,
                        instanceId,
                        position
                    )

                ReferenceApplication.worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.HOME_SCENE,
                    Action.SHOW_OVERLAY, sceneData
                )
                requestedHome = false
            }
        }
    }

    override fun showNoEthernetToast() {
        if (isInForeground) {
            showToast(ConfigStringsManager.getStringById("no_ethernet_message"))
        }
    }

    override fun exitApplication() {
        InformationBus.submitEvent(Event(Events.EXIT_APPLICATION_ON_BACK_PRESS))
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun digitPressed(digit: Int) {

        if(ttxModule.isTTXActive()){
            return
        }

        // Allow digit zap only after unlock/temporary unlock if input source is blocked
        if (ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE == worldHandler?.active?.id) {
            return
        }
        if (inputSourceModule.getDefaultValue()
                .contains("HDMI") || inputSourceModule.getDefaultValue().contains("Composite")
        ) {
            return
        }

        //digit zap should be disabled for FAST channels
        requestActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: TvChannel) {
                if (!data.isFastChannel()) {
                    // Send digit to the digit zap scene if it is visible
                    if (ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
                        ReferenceApplication.worldHandler!!.getVisibles()
                            .get(WorldHandler.LayerType.OVERLAY)!!.value.forEach { sceneManager ->
                                if (sceneManager.id == ReferenceWorldHandler.SceneId.DIGIT_ZAP) {
                                    (sceneManager as ZapDigitManager).onDigitPressed(digit)
                                }
                            }
                    } else {
                        runOnUiThread {
                            val sceneData = SceneData(id, instanceId, DigitZapItem(digit))
                            worldHandler!!.triggerActionWithData(
                                ReferenceWorldHandler.SceneId.DIGIT_ZAP,
                                Action.SHOW_OVERLAY,
                                sceneData
                            )
                        }
                    }
                }
            }
        })

    }

    override fun periodPressed() {
        if (ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIGIT_ZAP)) {
            ReferenceApplication.worldHandler!!.getVisibles()
                .get(WorldHandler.LayerType.OVERLAY)!!.value.forEach { sceneManager ->
                    if (sceneManager.id == ReferenceWorldHandler.SceneId.DIGIT_ZAP) {
                        (sceneManager as ZapDigitManager).onPeriodPressed()
                    }
                }
        }
    }

    override fun showPvrBanner() {
        runOnUiThread(Runnable {
            var sceneData = SceneData(id, instanceId)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        })
    }

    override fun handlePvrButton(): Boolean {
        if (generalConfigModule.getGeneralSettingsInfo("pvr")) {
            if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING)
                showPvrBanner()
            else
                showRecordingTypeDialog()

            return true
        }
        return false
    }

    override fun showRecordingTypeDialog() {
        if (!Utils.isUsbConnected()) {
            var title = ConfigStringsManager.getStringById("pvr_usb_error_msg")
            showErrorMessage(title)
            return
        }

        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(channel: TvChannel) {
                val isRecordable = pvrModule.isChannelRecordable(channel)
                if (!isRecordable) {
                    var title = ConfigStringsManager.getStringById("pvr_msg_cannot_record_program")
                    showErrorMessage(title)
                    return
                }
                val sceneData = SceneData(id, instanceId)
                ReferenceApplication.worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.CHOOSE_PVR_TYPE,
                    Action.SHOW_OVERLAY,
                    sceneData
                )
            }
        })
    }

    override fun onUnlockPressed() {
        scene!!.refresh(LiveSceneStatus(LiveSceneStatus.Type.UNLOCK_PRESSED))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun unlockChannel(callback: IAsyncCallback) {
        if(inputSourceModule.getDefaultValue().contains("Composite")) {
            ReferenceApplication.worldHandler!!.isEnableUserInteraction = false
            inputSourceModule.requestUnblockContent(
                object :
                    IAsyncCallback {
                    override fun onFailed(error: Error) {
                        ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                            true
                        callback.onSuccess()
                    }

                    override fun onSuccess() {
                        runOnUiThread(Runnable {
                            callback.onSuccess()
                            ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                                true
                            mutePlayback(false)
                        })
                    }
                })
            (scene as LiveScene).isEventUnlocked(true)

            if (isParentalBlockingActive()) {
                scene?.refresh(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
            }
            callback.onSuccess()
            return
        }

        requestActiveChannel(object :
            IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(activeChannel: TvChannel) {
                //activeChannel.isLocked giving wrong value of locked Channels so using prefs value to check if channel is locked or not
                if (!isChannelUnlocked() && activeChannel.isLocked) {
                    checkLockedChannel(activeChannel)
                    callback.onSuccess()
                } else {
                    ReferenceApplication.worldHandler!!.isEnableUserInteraction = false
                    playerModule.requestUnblockContent(
                        object :
                            IAsyncCallback {
                            override fun onFailed(error: Error) {
                                ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                                    true
                                callback.onSuccess()
                            }

                            override fun onSuccess() {
                                runOnUiThread(Runnable {
                                    callback.onSuccess()
                                    ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                                        true
                                    mutePlayback(false)
                                })
                            }
                        })
                    (scene as LiveScene).isEventUnlocked(true)
                }

                if (isParentalBlockingActive()) {
                    scene?.refresh(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
                }
                return
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun checkPin(pin: String, callback: IAsyncCallback) {
        if (pin == utilsModule.getParentalPin()) {
            if(inputSourceModule.getDefaultValue().contains("Composite")) {
                ReferenceApplication.worldHandler!!.isEnableUserInteraction = false
                inputSourceModule.requestUnblockContent(
                    object :
                        IAsyncCallback {
                        override fun onFailed(error: Error) {
                            ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                                true
                            callback.onSuccess()
                        }

                        override fun onSuccess() {
                            runOnUiThread(Runnable {
                                callback.onSuccess()
                                ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                                    true
                                mutePlayback(false)
                            })
                        }
                    })
                (scene as LiveScene).isEventUnlocked(true)

                if (isParentalBlockingActive()) {
                    scene?.refresh(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
                }
                callback.onSuccess()
                return
            }

            requestActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(activeChannel: TvChannel) {
                    //activeChannel.isLocked giving wrong value of locked Channels so using prefs value to check if channel is locked or not
                    if (!isChannelUnlocked() && activeChannel.isLocked) {
                        checkLockedChannel(activeChannel)
                    } else {
                        ReferenceApplication.worldHandler!!.isEnableUserInteraction = false
                        playerModule.requestUnblockContent(
                            object :
                                IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                                        true
                                    callback.onSuccess()
                                }

                                override fun onSuccess() {
                                    runOnUiThread(Runnable {
                                        callback.onSuccess()
                                        ReferenceApplication.worldHandler!!.isEnableUserInteraction =
                                            true
                                        mutePlayback(false)
                                    })
                                }
                            })
                        (scene as LiveScene).isEventUnlocked(true)
                    }

                    if (isParentalBlockingActive()) {
                        scene?.refresh(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
                    }
                    callback.onSuccess()
                    return
                }
            })
        } else {
            showToast(ConfigStringsManager.getStringById("wrong_pin_toast"))

            callback.onFailed(Error("Wrong pin."))
        }
    }

    override fun isChannelUnlocked(): Boolean {
        return playerModule.isChannelUnlocked
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkLockedChannel(tvChannel: TvChannel) {
        playerModule.unlockChannel()
        if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).playbackState != ReferenceWorldHandler.PlaybackState.RECORDING)
            playerModule.play(tvChannel)

        playerModule.isChannelUnlocked = true
        mutePlayback(false)
        //Show no signal screen if signal is not available
        if (!tvModule.isSignalAvailable()) {
            var status =
                LiveSceneStatus(LiveSceneStatus.Type.IS_MESSAGE)
            status.message =
                ConfigStringsManager.getStringById("status_message_signal_not_available")
            scene?.refresh(status)
        }

    }

    override fun resumePlayer() {
        if (!isParentalBlockingActive() && playerModule.playerState == PlayerState.STOPPED) playerModule.resume()
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun requestActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
        if (inputSourceModule.getDefaultValue() == "TV") {
            var applicationMode =
                if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
            tvModule.getActiveChannel(callback, applicationMode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun mutePlayback(isMuted: Boolean) {
        if (ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE != worldHandler?.active?.id) {
            if (isMuted) {
                playerModule.mute()
            } else {
                val isAudioMuted = utilsModule.getPrefsValue("PREF_KEY_IS_MUTED", false) as Boolean
                // Added condition to prevent unmuting when Discover tab is open, no need to unmute it
                // as it will give background sound which should not come in discover tab.
                if((worldHandler?.active?.scene as? HomeSceneBase)?.selectedPosition != 0){
                     if (!HomeSceneManager.IS_VOD_ACTIVE && !isAudioMuted) playerModule.unmute()
                }

            }
        }
    }

    override fun isParentalBlockingActive(): Boolean {
        return playerModule.isParentalActive
    }

    override fun requestActiveEvent(channel: TvChannel, callback: IAsyncDataCallback<TvEvent>) {
        epgModule.getCurrentEvent(channel, callback)
    }

    override fun destroyOtherScenes() {
        worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        InformationBus.submitEvent(Event(Events.RECHECK_APP_PERMISSIONS))
        if (showNoSignalInputMsg && (inputSourceModule.getDefaultValue().contains("Composite") || inputSourceModule.getDefaultValue().contains("HDMI"))) {
            (scene as LiveScene).refreshInputNoSignal(1)
        } else {
            (scene as LiveScene).refreshInputNoSignal(0)
        }
        // To refresh channel info on setting changes
        if (inputSourceModule.getDefaultValue() == "TV") {
            if (ForYouInterface.ENABLE_FAST_DATA && appStopped) {
                worldHandler?.destroyOtherExisting(id)
                if ((scene as LiveScene).isFastZapBannerActive()) {
                    (scene as LiveScene).showFastZapBanner()
                }
                var startChannelFromLiveTabPref = utilsModule.getPrefsValue(Constants.SharedPrefsConstants.STARTED_CHANNEL_FROM_LIVE_TAB_TAG, false) as Boolean
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "get STARTED_CHANNEL_FROM_LIVE_TAB_TAG $startChannelFromLiveTabPref")

                if (!startChannelFromLiveTabPref) {
                    showHome()
                    BackFromPlayback.setLiveSceneFromLiveHomeState(false)
                }else{
                    BackFromPlayback.setLiveSceneFromLiveHomeState(true)
                    (ReferenceApplication.getActivity() as MainActivity).startChannelFromLiveTab = false
                    utilsModule.setPrefsValue(Constants.SharedPrefsConstants.STARTED_CHANNEL_FROM_LIVE_TAB_TAG, false)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "set STARTED_CHANNEL_FROM_LIVE_TAB_TAG false")

                }
            }
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(activeTvChannel: TvChannel) {
                    if (activeTvChannel != null && activeTvChannel.isRadioChannel) {
                        runOnUiThread {
                            (scene as LiveScene).updateChannelInfo(activeTvChannel)
                        }
                    }
                }
            })
        } else {
            if(ReferenceApplication.isPrefClicked == false) {
                (scene as LiveScene).clearBg()
                (scene as LiveScene).updateEventLockInfo()
                hideBlackOverlay()
            }
        }
        appStopped = false
    }

    private fun showBlackOverlay() {
        val status = LiveSceneStatus(LiveSceneStatus.Type.IS_SHOW_OVERLAY)
        runOnUiThread {
            scene!!.refresh(status)
        }
    }
    private fun hideBlackOverlay() {
        (scene as LiveScene).hideBlueMute()
        val status = LiveSceneStatus(LiveSceneStatus.Type.IS_HIDE_OVERLAY)
        runOnUiThread {
            if (scene != null) {
                scene!!.refresh(status)
            }
        }
    }

    private fun hideChannelLock() {
        val status = LiveSceneStatus(LiveSceneStatus.Type.PVR_UNLOCK)
        runOnUiThread {
            if (scene != null) {
                scene!!.refresh(status)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onEventReceived(event: Event?) {
        if(event!!.type != Events.TIME_CHANGED)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onEventReceived: event ${event!!.type}")

        if(event?.type == Events.DIRECT_TUNE_FORCE_PLAYBACK){
            val channelId = (event.data?.get(0) as Long)
            val playbleItem = tvModule.getChannelById(channelId?.toInt()?:0)
            playbleItem?.let {
                tvModule.changeChannel(it, object : IAsyncCallback{
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: forceplayback")
                    }

                    override fun onSuccess() {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: onSuccess")
                    }

                }, ApplicationMode.DEFAULT)
            }
        }

        if (event!!.type == Events.APP_STOPPED) {
            isAppStop = true
        }
        if (event?.type == Events.CHANNEL_CHANGED) {
            if (!inputSourceModule.isBlock(getDefaultInputValue())) {
                if (event.data != null && event.getData(0) is TvChannel) {
                    var tvChannel = event.getData(0) as TvChannel
                    if(inputSourceModule.getDefaultValue() == "TV") {
                        (scene as LiveScene).refreshInputNoSignal(0)
                    }
                    if(!isAppStop && !playerModule.getQuietTuneEnabled()
                        && !(ReferenceApplication.getActivity() as MainActivity).settingsIconClicked) {
                        showBanner(tvChannel)
                    }
                    isAppStop = false
                    if (tvChannel.isRadioChannel) {
                        var status = LiveSceneStatus(LiveSceneStatus.Type.IS_RADIO)
                        scene!!.refresh(status)
                    } else if (tvChannel.isFastChannel()) {
                        //if internet is not available then no internet scene should display on live scene.
                        if (isNetworkAvailable() || isParentalBlockingActive()) {
                            var status = LiveSceneStatus(LiveSceneStatus.Type.NONE)
                            scene!!.refresh(status)
                        }else{
                            scene!!.refresh(Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL)
                        }
                    }
                }
            }

            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()



        } else if (event?.type == Events.PLAYBACK_SHOW_BLACK_OVERLAY) {
            showBlackOverlay()
        } else if (event?.type == Events.PLAYBACK_HIDE_BLACK_OVERLAY) {
            runOnUiThread {
                hideBlackOverlay()
            }
        } else if (event?.type == Events.GOOGLE_PIN_CORRECT) {
            hideChannelLock()
        } else if (event?.type == Events.PLAYBACK_STATUS_MESSAGE_NONE ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_SCRAMBLED ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_LOCKED ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_NO_PLAYBACK ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_MESSAGE ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_LOCKED ||
            event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_PARENTAL
        ) {
            if(event?.type == Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL){
                noSignal = true
                (scene as LiveScene).showBlueMute()
            }
            if(event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_SCRAMBLED &&
                playerModule.getQuietTuneEnabled()) {
                return
            }

            if (inputSourceModule.getDefaultValue().contains("TV")) {
                if (((scene as LiveScene).isFastZapBannerActive() || (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal)) && (
                            event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_MESSAGE ||
                                    event?.type == Events.PLAYBACK_STATUS_MESSAGE_NO_PLAYBACK ||
                                    event?.type == Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL)
                ) {
                    return
                }
            }
            if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).playbackState != ReferenceWorldHandler.PlaybackState.PVR_PLAYBACK) {
                runOnUiThread {
                    scene?.refresh(event.type)
                }
            }
        } else if (event.type == Events.NO_ETHERNET_EVENT) {
            if (!shownNoEthMessage && !tvModule.appJustStarted()) {
                runOnUiThread {
                    showNoEthernetToast()
                }
                shownNoEthMessage = true
                if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal){
                    scene?.refresh(Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL)
                }
            }
        } else if (event?.type == Events.IS_EAS_PLAYING) {
            scene?.refresh(event)
        } else if (event!!.type == Events.PLAYBACK_STARTED) {
            noSignal = false
            runOnUiThread {
                (scene as LiveScene).showBlueMuteBlackOverlay()
            }
        }else if (event.type == Events.SUBTITLE_TRACK_SELECTED) {
            // For fast channels subtitles are not visible by default only in FUNAI image
            // So here we are selecting it manually once channel tuned and subtitles are available
            if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal && utilsModule.getSubtitlesState(PrefType.BASE)) {
                val track = playerModule.getActiveSubtitle()
                if (track != null) {
                    playerModule.selectSubtitle(track)
                }
            }else{
                closedCaptionModule.updateSelectedTrack()
            }
        }

        else if (event.type == Events.NO_PLAYBACK || event.type == Events.PLAYER_TIMEOUT
            || event.type == Events.NO_CHANNELS_AVAILABLE
        //WAITING_FOR_CHANNEL happens when event is locked and blue mute should not be visible in that case
        //in mtk app WAITING_FOR_CHANNEL shows blue mute
//            || event.type == Events.WAITING_FOR_CHANNEL
        ) {
            (scene as LiveScene).refreshOverlays()
            (scene as LiveScene).showBlueMuteBlackOverlay()
        } else if (event.type == Events.PVR_PLAYBACK_STARTED) {
            ReferenceApplication.worldHandler!!.playbackState = ReferenceWorldHandler.PlaybackState.PVR_PLAYBACK
            val recordedEvent = event.data?.get(0) as Recording
            val recordedChannel = recordedEvent.tvChannel
            (scene as LiveScene).handlePVRPlayback(true, recordedChannel, recordedEvent)
        }
        if (event.type == Events.VOD_PLAYBACK_STARTED) {
            (scene as LiveScene).clearAll()
        }

        if (event!!.type == Events.PVR_RECORDING_FINISHED) {
            ReferenceApplication.worldHandler!!.playbackState =
                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
//            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)

            hideRecordingOverlay()
        }

        if (event!!.type == Events.ACTIVE_SERVICE_RUNNING) {

            //Check if locked or parental is active
            tvModule.getActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {

                }

                override fun onReceive(data: TvChannel) {
                    if (data.isRadioChannel) {
                        var status = LiveSceneStatus(LiveSceneStatus.Type.IS_RADIO)
                        scene!!.refresh(status)
                    }
                    if (!data.isLocked) {
                        mutePlayback(false)
                    }
                }
            })

            val status = LiveSceneStatus(LiveSceneStatus.Type.IS_RUNNING)
            runOnUiThread {
                scene!!.refresh(status)
            }
        }

        // Cases for running status and replacement service (Banners only)
        if (event!!.type == Events.ACTIVE_SERVICE_NOT_RUNNING) {
            Log.i("RunningDataProvider", "onEventReceived: NOT RUNNING")
            mutePlayback(true)
            val status = LiveSceneStatus(LiveSceneStatus.Type.IS_NOT_RUNNING)
            runOnUiThread {
                scene!!.refresh(status)
            }
        } else if (event!!.type == Events.ACTIVE_SERVICE_REPLACEABLE) {
            Log.i("RunningDataProvider", "onEventReceived: REPLACEMENT")
            playerModule.mute()
            val status = LiveSceneStatus(LiveSceneStatus.Type.IS_NOT_RUNNING)
            runOnUiThread {
                scene!!.refresh(status)
            }
        }

        if (event?.type == Events.PLAYBACK_STATUS_MESSAGE_IS_RADIO) {
            ReferenceApplication.getActivity().runOnUiThread {
                scene!!.refresh(LiveSceneStatus(LiveSceneStatus.Type.IS_RADIO))
            }
        } else
            if (event.type == Events.NO_ETHERNET_EVENT) {
                if (!shownNoEthMessage && !tvModule.appJustStarted()) {
                    runOnUiThread {
                        showNoEthernetToast()
                    }
                    shownNoEthMessage = true
                }
            } else if (event.type == Events.ETHERNET_EVENT) {
                runOnUiThread {
                    if (worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.DIALOG_SCENE) {
                        worldHandler!!.triggerAction(worldHandler!!.active!!.id, Action.DESTROY)
                    }
                    shownNoEthMessage = false
                    tvModule.playbackStatusInterface.appJustStarted = false
                }
            } else if (event.type == Events.USB_DRIVE_FULL) {
                Log.d(Constants.LogTag.CLTV_TAG + "Live MAnager", "onEventReceived: No space in pendrive or pendrive not found")

            } else if (event!!.type == Events.SCHEDULED_RECORDING_NOTIFICATION) {
                if (event.data != null && event.getData(0) is ScheduledRecording<*, *>) {
                    Log.v("RECORDING", "SCHEDULED RECORDING ADDED")

                }
            }else if (event.type == Events.RCU_DIGIT_PRESSED) {
                if (event.getData(0) is Int) {
                    Log.d(Constants.LogTag.CLTV_TAG + "RCU-LiveManager", "onEventReceived: ${event.getData(0)}")

                    if (worldHandler?.active?.id == ReferenceWorldHandler.SceneId.INFO_BANNER ||
                        worldHandler?.active?.id == ReferenceWorldHandler.SceneId.ZAP_BANNER ||
                        worldHandler?.active?.id == ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE
                    ) {

                        runOnUiThread {
                            worldHandler?.active?.scene?.id?.let {
                                worldHandler?.triggerAction(it,Action.DESTROY)
                            }
                        }
                    }
                    else if (worldHandler?.active?.id == ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE)
                    {
                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE,
                            Action.HIDE
                        )
                    }
                    digitPressed(event.getData(0) as Int)
                }
            } else if (event.type == Events.RCU_PERIOD_PRESSED) {
                periodPressed()
            } else if (event.type == Events.ACTIVE_CHANNEL_DELETED_EVENT) {
                handleCurrentChannel()
            } else if (event.type == Events.CHANNEL_LIST_IS_EMPTY) {
                showChannelScanDialog()
            } else if (event.type == Events.DISK_READY_EVENT) {
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.DESTROY
                )
            }
        if (event.type == Events.CHANNEL_LIST_UPDATED) {
            if (inputSourceModule.getDefaultValue() == "TV") {
                //since noSignal might come or not, but we need to tune. As discussed it works for ATSC
                //but in case of dvb it does not work so we are tuning it always.
                if (noSignal) {
                    val channelList = tvModule.getChannelList()
                    tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                            println(error)
                        }

                        override fun onReceive(activeChannel: TvChannel) {
                            if ((activeChannel.name == "") && (activeChannel.displayNumber == "0")) {
                                playFirstBrowsableChannel(channelList)
                            }else{
                                val activeChannelId = utilsModule.getPrefsValue("CurrentActiveChannelId", -1)
                                if (activeChannelId != -1) {
                                    var isNewListContainsChannel = false
                                    for (ch in channelList) {
                                        if (ch.id == activeChannelId) {
                                            isNewListContainsChannel = true
                                            break
                                        }
                                    }
                                    //if updated list does not contain the active channel then play the first browsable channel.
                                    //else if it contains than no need to do anything.
                                    if (!isNewListContainsChannel) {
                                        playFirstBrowsableChannel(channelList)
                                    }
                                }else{
                                    playFirstBrowsableChannel(channelList)
                                }
                            }

                        }
                    })
                }else{
                    val channelList = tvModule.getChannelList()
                    val activeChannelId = utilsModule.getPrefsValue("CurrentActiveChannelId", -1)
                    if (activeChannelId != -1) {
                        var isNewListContainsChannel = false
                        for (ch in channelList) {
                            if (ch.id == activeChannelId) {
                                isNewListContainsChannel = true
                                break
                            }
                        }
                        //if updated list does not contain the active channel then play the first browsable channel.
                        //else if it contains than no need to do anything.
                        if (!isNewListContainsChannel) {
                            playFirstBrowsableChannel(channelList)
                        }
                    }else{
                        playFirstBrowsableChannel(channelList)
                    }
               }
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "onEventReceived: channels list size ${
                        tvModule.getChannelList().size
                    }"
                )
                if (tvModule.getBrowsableChannelList().size > 0) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "started Guideupdate false")
                    watchlistModule.removeWatchlistEventsForDeletedChannels()
                    schedulerModule.removeScheduledRecordingForDeletedChannels()
                }
            }
        }

        if(event.type == Events.CHANNEL_LIST_SWITCHED) {
            val channelList = tvModule.getChannelList()
            run exitForEach@{
                channelList.forEach {
                    if (it.isBrowsable && !it.inputId.contains("iwedia")) {
                        tvModule.changeChannel(it, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                println(error)
                            }

                            override fun onSuccess() {
                            }
                        })
                        return@exitForEach
                    }
                }
            }
        }

        if (event.type == Events.UPDATED_CONFIG_KEYS) {
            initConfigurableKeys()
        }

        if (event.type == Events.FOCUS_ON_BACK_PRESS) {
            scene!!.onResume()
        }

        if (event.type == Events.SHOW_PLAYER) {
            if (generalConfigModule.getGeneralSettingsInfo("timeshift")) {
                showPlayer()
            }

        }

        if (event.type == Events.BLUE_MUTE_ENABLED) {
            (scene as LiveScene).refreshOverlays()
        }

        if (event.type == Events.WAITING_FOR_CHANNEL) {
            (scene as LiveScene).hideBlueMuteBlackOverlay()
            (scene as LiveScene).refreshOverlays()
        }
        if (event.type == Events.VIDEO_RESOLUTION_UNAVAILABLE) {
            if (BuildConfig.FLAVOR == "mtk" && (inputSourceModule.getDefaultValue().contains("Composite") || inputSourceModule.getDefaultValue().contains("HDMI"))) {
                if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).playbackState != ReferenceWorldHandler.PlaybackState.PVR_PLAYBACK) {
                    if (ReferenceApplication.isTVInputBlocked && inputSourceModule.isBlock(
                            inputSourceModule.getDefaultValue()
                        )
                    ) {
                        mutePlayback(true)
                    }
                    runOnUiThread {
                        var value = event.getData(0) as Int
                        showNoSignalInputMsg = value == 1
                        (scene as LiveScene).refreshInputNoSignal(value)
                    }
                }
            }
        }
        if (event.type == Events.NO_AV_OR_AUDIO_ONLY_CHANNEL
        ) {
            if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).playbackState != ReferenceWorldHandler.PlaybackState.PVR_PLAYBACK && !HomeSceneManager.IS_VOD_ACTIVE) {
                runOnUiThread {
                    if(event.getData(0) != null){
                        val value = event.getData(0) as Int
                        (scene as LiveScene).refreshLiveScene(value)
                    }
                }
            }
        }
        if(event.type == Events.TALKBACK_CLOSE_ZAP_BANNER){
            (scene as LiveScene).focusOnHomeUp()
        }
        if (event.type == Events.PVR_RECORDING_STARTED) {
            hideRecordingOverlay()
        }
        if (event.type == Events.PVR_RECORDING_STARTING) {
            showStartRecordingOverlay()
        }
        if (event.type == Events.PVR_RECORDING_FINISHING) {
            showStopRecordingOverlay()
        }
        if (event.type == Events.SHOW_PVR_BANNER) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, Log.getStackTraceString(java.lang.Exception()))
            CoroutineHelper.runCoroutine({
                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                    Action.SHOW_OVERLAY
                )
            }, Dispatchers.Main)
        }
        if (event.type == Events.PVR_RECORDING_SHOULD_STOP) {
            event.data?.first()?.let {
                val recordingInProgress = pvrModule.isRecordingInProgress()
                val continuation = (it as? () -> Unit)
                if (!recordingInProgress) {
                    continuation?.invoke()
                }
            }
        }
        if(event.type == Events.SHOW_RECORDING_TIME_SHIFT_CONFLICT_DIALOG) {
            recordingTimeShiftConflictDialog(event.getData(0) as TvEvent)
        }
        if (event.type == Events.SHOW_WATCHLIST_TIME_SHIFT_CONFLICT_DIALOG) {
            watchlistTimeShiftConflictDialog(event.getData(0) as TvChannel)
        }

        super.onEventReceived(event)
    }

    override fun checkSupportHbbtv(): Boolean{
        return hbbTvModule.checkSupportHbbtv()
    }

    override fun isSignalAvailable(): Boolean {
        return tvModule.isSignalAvailable()
    }
    override fun setTTMLVisibility(isVisible: Boolean) {
        playerModule.setTTMLVisibility(isVisible)
    }

    private fun showRecordingOverlay(text: String?) {
        val sceneData = DialogSceneData(id, instanceId)
        sceneData.type = DialogSceneData.DialogType.TEXT
        sceneData.message = text
        sceneData.isBackEnabled = false
        sceneData.positiveButtonEnabled = false
        sceneData.imageRes = R.drawable.record_icon

        worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
            Action.SHOW_NOTIFICATION, sceneData
        )
    }

    private fun hideRecordingOverlay() {
        worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
            Action.DESTROY
        )
    }

    private fun showStartRecordingOverlay() {
        showRecordingOverlay(ConfigStringsManager.getStringById("recording_is_starting_please_wait"))
    }

    private fun showStopRecordingOverlay() {
        showRecordingOverlay(ConfigStringsManager.getStringById("recording_is_stopping_please_wait"))
    }

    private fun handleCurrentChannel() {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onReceive(data: TvChannel) {
                if (data.isRadioChannel) {
                    var status = LiveSceneStatus(LiveSceneStatus.Type.IS_RADIO)
                    scene!!.refresh(status)
                }
            }

            override fun onFailed(error: Error) {
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showChannelScanDialog() {
        playerModule.stop()
        var sceneData = DialogSceneData(id, instanceId)
        sceneData.type = DialogSceneData.DialogType.TEXT
        sceneData.title = ConfigStringsManager.getStringById("no_channels_found")
        sceneData.message = ConfigStringsManager.getStringById("connect_scan_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("scan_channels_text")
        sceneData.isBackEnabled = true
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {
                exitApplication()
            }

            override fun onPositiveButtonClicked() {
                // resetPlaybackStatus is called like this since ChannelHelper is not initialized 
                InformationBus.submitEvent(
                    Event(
                        Events.PLAYBACK_STATUS_MESSAGE_NONE))

                if (!utilsModule.kidsModeEnabled()) {
                    try {
                        utilsModule.startScanChannelsIntent()
                    } catch (e: Exception) {
                        showToast("NO SCAN INTENT FOUND")
                    }
                }
            }
        }
        runOnUiThread(Runnable {
            GlobalAppReceiver.activeSceneId = 0
            if (!ReferenceApplication.isDialogueCreated) {
                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.LIVE, Action.SHOW)
                ReferenceApplication.noChannelScene = true
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.SHOW_OVERLAY, sceneData
                )
            }
        })
    }
    @RequiresApi(Build.VERSION_CODES.R)
    private fun playFirstBrowsableChannel(channelList : ArrayList<TvChannel>){
        for (channel in channelList) {
            if (channel.isBrowsable && !channel.inputId.contains("iwedia")) {
                //for t56 there is no need to play first broadcast channel, it will play only in case of fast channel.
                if(!((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56() || channel.isFastChannel()){
                    val applicationMode = if(channel.isFastChannel()) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
                    utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, applicationMode.ordinal)
                    tvModule.changeChannel(channel, object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            println(error)
                        }

                        override fun onSuccess() {
                            Log.d(Constants.LogTag.CLTV_TAG +
                                TAG,
                                "onSuccess: new channel Played from the updated list"
                            )
                        }
                    },applicationMode)
                    break
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun showPlayer(keyCode: Int) {
        //check if fast channel is active, do not show the player
        if (getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) return

        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING) {
            InformationBus.submitEvent(Event(Events.REFRESH_PVR_BANNER))
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN
            ) {
                showPvrBanner()
                if (keyCode != KeyEvent.KEYCODE_DPAD_DOWN) {
                    showToast(ConfigStringsManager.getStringById("recording_progress_toast"))
                }
            }
            return
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
        ) {
            //TODO
            /*if ((ReferenceSdk.playerHandler as ReferencePlayerHandler).isActiveChannelScrambled()) {
                var title =
                    ConfigStringsManager.getStringById("time_shift_scrambled_channel_msg")
                showErrorMessage(title)
                return
            }*/
            if (utilsModule.getUsbDevices().size == 0) {
                showErrorMessage(ConfigStringsManager.getStringById("time_shift_usb_error_msg"))
                return
            }
                showPlayer(
                    playerType = PlayerSceneData.PLAYER_TYPE_TIME_SHIFT,
                    true
                )
        } else {
            var desiredChannelIndex = tvModule.getDesiredChannelIndex()

            var activeTvChannel = tvModule.getChannelByIndex(desiredChannelIndex)

            epgModule.getCurrentEvent(
                activeTvChannel!!,
                object : IAsyncDataCallback<TvEvent> {
                    override fun onReceive(data: TvEvent) {
                        var sceneData = TimeshiftSceneData(id, instanceId)
                        sceneData.tvEvent = data

                        if (pvrModule.isRecordingInProgress())
                            showPvrBanner()
                        else {
                            if (utilsModule.getUsbDevices().size == 0) {
                                showErrorMessage(ConfigStringsManager.getStringById("time_shift_usb_error_msg"))
                                return
                            }
                            showPlayer(
                                playerType = PlayerSceneData.PLAYER_TYPE_TIME_SHIFT,
                                false
                            )

                        }
                    }

                    override fun onFailed(error: kotlin.Error) {
                        var sceneData = TimeshiftSceneData(id, instanceId)
                        sceneData.tvEvent = TvEvent.createNoInformationEvent(
                            activeTvChannel,
                            getCurrentTime(activeTvChannel)
                        )

                        if (pvrModule.isRecordingInProgress())
                            showPvrBanner()
                        else {
                            if (utilsModule.getUsbDevices().size == 0) {
                                showErrorMessage(ConfigStringsManager.getStringById("time_shift_usb_error_msg"))
                                return
                            }
                            showPlayer(
                                playerType = PlayerSceneData.PLAYER_TYPE_TIME_SHIFT,
                                false
                            )
                        }
                    }
                })
        }
    }



    private fun stopRecordOnActiveTvChannel() {

        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
            }

            override fun onReceive(activeTvChannel: TvChannel) {
                pvrModule.stopRecordingByChannel(activeTvChannel, object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                    }

                    override fun onSuccess() {
                        com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                        utilsModule.runCoroutine({
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            PvrBannerSceneManager.previousProgress = 0L
                        })
                    }
                })
            }
        })

    }

    override fun showStopRecordingDialog(channelChange: String?) {
        utilsModule.runCoroutine({
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    worldHandler!!.triggerAction(id, Action.SHOW_OVERLAY)
                }

                override fun onPositiveButtonClicked() {
                    Log.w(ReferenceApplication.TAG, "onPositiveButtonClicked() here")
                    stopRecordOnActiveTvChannel()
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHING)
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW, sceneData
            )
        }, Dispatchers.Main)
    }

    private fun recordingTimeShiftConflictDialog(tvEvent: TvEvent) {
        val worldHandler = ReferenceApplication.worldHandler!!
        val currentActiveScene = worldHandler.active
        val sceneData = DialogSceneData(
            currentActiveScene!!.id,
            currentActiveScene.instanceId
        )
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title = ConfigStringsManager.getStringById("time_shift_start_record_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onPositiveButtonClicked() {
                timeshiftModule.timeShiftStop(object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                    }

                    override fun onSuccess() {
                    }
                })
                worldHandler.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.DESTROY
                )
                worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                timeshiftModule.setTimeShiftIndication(false)
                playerModule.resume()
                startRecording(tvEvent)
            }
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "recordingTimeShiftConflictDialog: ")
        worldHandler.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
            Action.SHOW, sceneData
        )
        return
    }

    private fun watchlistTimeShiftConflictDialog(tvChannel: TvChannel) {
        val worldHandler = ReferenceApplication.worldHandler!!
        val currentActiveScene = worldHandler.active
        val sceneData = DialogSceneData(
            currentActiveScene!!.id,
            currentActiveScene.instanceId
        )
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {}

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onPositiveButtonClicked() {
                timeshiftModule.timeShiftStop(object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                    }

                    override fun onSuccess() {
                    }
                })
                worldHandler.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.DESTROY
                )
                worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                timeshiftModule.setTimeShiftIndication(false)
                playerModule.resume()
                tvModule.changeChannel(tvChannel, object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        InformationBus.submitEvent(Event(utils.information_bus.events.Events.HIDE_SCENE_LOADING))
                    }

                    override fun onSuccess() {
                        utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, 0)
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        InformationBus.submitEvent(Event(utils.information_bus.events.Events.HIDE_SCENE_LOADING))
                        InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, tvChannel))
                        Log.i("ReminderConflictSceneManager", " HandlePlayChannel here")

                        if (tvChannel.isRadioChannel) {
                            val status =
                                LiveSceneStatus(LiveSceneStatus.Type.IS_RADIO)
                            InformationBus.submitEvent(
                                Event(
                                    Events.PLAYBACK_STATUS_MESSAGE_NONE,
                                    status
                                )
                            )
                        }
                    }
                })
            }
        }
        worldHandler.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
            Action.SHOW, sceneData
        )
        return
    }

    private fun showPlayer(
        playerType: Int = PlayerSceneData.PLAYER_TYPE_DEFAULT,
        isPauseClicked: Boolean = false
    ) {
        if(utilsModule.getPrefsValue("IS_TIMESHIFT_ENABLED", false) == false && utilsModule.getTimeshiftStoragePath().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("set_usb"))
            InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
            return
        }

        if(utilsModule.getPrefsValue("IS_TIMESHIFT_ENABLED", false) == false && utilsModule.getTimeshiftStoragePath().isNotEmpty()) {
            showToast(ConfigStringsManager.getStringById("enable_timeshift"))
            return
        }

        if(utilsModule.getPrefsValue("IS_TIMESHIFT_ENABLED", false) == true && utilsModule.getTimeshiftStoragePath().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("set_usb"))
            InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
            return
        }

        if(playerModule.isOnLockScreen){
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_timeshift"))
            return
        }
        var desiredChannelIndex = tvModule.getDesiredChannelIndex()
        var activeTvChannel = tvModule.getChannelByIndex(desiredChannelIndex)
        epgModule.getCurrentEvent(
            activeTvChannel!!,
            object : IAsyncDataCallback<TvEvent> {
                override fun onReceive(data: TvEvent) {
                    var sceneData = TimeshiftSceneData(id, instanceId)
                    sceneData.tvEvent = data
                    sceneData.isPauseClicked = isPauseClicked
                    runOnUiThread(Runnable {
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE,
                            Action.SHOW_OVERLAY,
                            sceneData
                        )
                    })
                }

                override fun onFailed(error: Error) {
                    var sceneData = TimeshiftSceneData(id, instanceId)
                    sceneData.tvEvent =
                        TvEvent.createNoInformationEvent(
                            activeTvChannel,
                            getCurrentTime(activeTvChannel)
                        )
                    sceneData.isPauseClicked = isPauseClicked
                    runOnUiThread(Runnable {
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE,
                            Action.SHOW_OVERLAY,
                            sceneData
                        )
                    })
                }
            })
    }


    private fun showErrorMessage(title: String) {
        CoroutineHelper.runCoroutine({
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
        }, Dispatchers.Main)
    }

    class LiveSceneStatus {

        enum class Type {
            IS_RADIO,
            IS_SCRAMBLED,
            IS_LOCKED,
            IS_NOT_LOCKED,
            IS_PARENTAL,
            IS_NOT_PARENTAL,
            IS_MESSAGE,
            IS_SHOW_OVERLAY,
            IS_HIDE_OVERLAY,
            IS_RUNNING,
            IS_NOT_RUNNING,
            IS_REPLACEABLE,
            UNLOCK_PRESSED,
            PVR_UNLOCK,
            PVR_LOCK,
            NONE
        }

        var type: Type? = null

        constructor(type: Type) {
            this.type = type
        }

        var flag: Boolean = false
        var message: String = ""
    }

    override fun onPause() {
    }

    override fun isBlueMuteEnabled(): Boolean {
        return utilsModule.getBlueMuteState()
    }

    override fun isBlueMuteActive(): Boolean {
        return !tvModule.isSignalAvailable() ||
                !tvModule.isChannelsAvailable() ||
//                tvModule.playbackStatusInterface.isWaitingChannel ||
                tvModule.isPlayerTimeout()
        return false
    }

    override fun getEasChannel() {
        val channelNumber = preferenceModule.getEasChannel()
        tvModule.getSelectedChannelList(object : IAsyncDataCallback<ArrayList<TvChannel>> {
            override fun onFailed(error: Error) {}

            override fun onReceive(data: ArrayList<TvChannel>) {
                data.forEach { item ->
                    if (item.displayNumber == channelNumber) {
                        tvModule.changeChannel(item, object : IAsyncCallback {
                            override fun onFailed(error: Error) {}

                            override fun onSuccess() {}
                        })
                    }
                }
            }
        })
    }

    override fun isTuneToDetailsChannel(): Boolean {
        return preferenceModule.isTuneToDetailsChannel()
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

    override fun setCCInfo() {
        if (closedCaptionModule.isClosedCaptionEnabled())
            closedCaptionModule.setCCInfo()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun isParentalControlEnabled(): Boolean {
        if (getApplicationMode() == ApplicationMode.DEFAULT.ordinal) {
            return parentalControlSettingsModule.isParentalControlsEnabled()
        } else {
            return parentalControlSettingsModule.isAnokiParentalControlsEnabled()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getApplicationMode(): Int {
        return (worldHandler as ReferenceWorldHandler).getApplicationMode()
    }

    override fun isNetworkAvailable() : Boolean {
        return tvModule.isNetworkAvailable()
    }

    override fun isTTXActive () : Boolean{
        return ttxModule.isTTXActive()
    }

    override fun notifyHbbTvChannelLockUnlock(status: Boolean) {
        hbbTvModule.notifyHbbTvChannelLockUnlock(status)
    }

    override fun getPlatformName(): String {
        return platformOSModule.getPlatformName()
    }

    private fun onSoundSettingsClicked() {
        try {
            var intent = Intent()
            context!!.runOnUiThread {
                worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }
            when (BuildConfig.FLAVOR) {
                "rtk" -> intent = Intent("android.settings.SOUND_SETUP")
                "refplus5" -> {
                    intent  = Intent("com.android.tv.action.VIEW_SOUND_SETTINGS")
                }

                else -> {
                    intent.setClassName(
                        SETTINGS_PACKAGE,
                        "com.android.tv.settings.device.sound.SoundActivity"
                    )
                }
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ReferenceApplication.applicationContext()
                .startActivity(intent)
        } catch (e: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSoundSettingsClicked: ${e.printStackTrace()}")
        }
    }

    private fun onScreenSettingsClicked() {
        try {
            context!!.runOnUiThread {
                worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }
            var intent = Intent()
            when (BuildConfig.FLAVOR) {
                "rtk" -> intent = Intent("android.settings.SCREEN_SETUP")
                "refplus5" -> {
                    intent.setClassName(SETTINGS_PACKAGE, SETTINGS_SCREEN_MODE_ACTIVITY)
                }

                else -> {
                    intent.setClassName(
                        "com.android.tv.settings",
                        "com.android.tv.settings.partnercustomizer.picture.PictureActivity"
                    )
                }
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
}