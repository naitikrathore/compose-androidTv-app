package com.iwedia.cltv

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.tv.TvView
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.os.Process
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.BaseInputConnection
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.Observer
import androidx.tvprovider.media.tv.Channel
import androidx.tvprovider.media.tv.ChannelLogoUtils.storeChannelLogo
import androidx.tvprovider.media.tv.PreviewChannel
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.ReferenceWorldHandler.PlaybackState
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.anoki_fast.vod.player.VodBannerScene
import com.iwedia.cltv.anoki_fast.vod.player.VodBannerSceneManager
import com.iwedia.cltv.assistant.ContentAggregatorService
import com.iwedia.cltv.assistant.ContentAggregatorServiceRestarter
import com.iwedia.cltv.components.CiMenu
import com.iwedia.cltv.components.OverlayFeatureView
import com.iwedia.cltv.components.PreferenceCategoryItem
import com.iwedia.cltv.components.RecommendationViewHolder
import com.iwedia.cltv.components.SceneInactivityTimer
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigCompanyDetailsManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.manager.DialogSceneManager
import com.iwedia.cltv.manager.HomeSceneManager
import com.iwedia.cltv.manager.LiveManager
import com.iwedia.cltv.manager.OadPopUpManager
import com.iwedia.cltv.manager.PlayerSceneManager
import com.iwedia.cltv.manager.PvrBannerSceneManager
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.HbbTvInterface
import com.iwedia.cltv.platform.`interface`.PreferenceInterface
import com.iwedia.cltv.platform.`interface`.SchedulerInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.Constants.CONST_CHANGE_INPUT_TYPE_FOCUS_DELAY
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.fast_backend_utils.FastAnokiUidHelper
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.input_source.InputItem
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.receiver.GlobalAppReceiver
import com.iwedia.cltv.receiver.LanguageChangeReceiver
import com.iwedia.cltv.receiver.ScanStartedReceiver
import com.iwedia.cltv.receiver.UsbReceiver
import com.iwedia.cltv.scene.PIN.PinSceneData
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.home_scene.HomeSceneBase
import com.iwedia.cltv.scene.home_scene.HomeSceneBroadcast
import com.iwedia.cltv.scene.home_scene.HomeSceneData
import com.iwedia.cltv.scene.home_scene.HomeSceneFast
import com.iwedia.cltv.scene.home_scene.HomeSceneVod
import com.iwedia.cltv.scene.home_scene.guide.HorizontalGuideSceneWidget
import com.iwedia.cltv.scene.input_scene.InputSelectedSceneData
import com.iwedia.cltv.scene.live_scene.InactivityTimer
import com.iwedia.cltv.scene.recording_conflict.RecordingConflictSceneData
import com.iwedia.cltv.scene.recording_watchlist_conflict_scene.RecordingWatchlistConflictSceneData
import com.iwedia.cltv.scene.reminder_conflict_scene.ReminderSceneData
import com.iwedia.cltv.tis.helper.ScanHelper
import com.iwedia.cltv.utils.PinHelper
import com.iwedia.cltv.utils.TvViewScaler
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidActivity
import com.iwedia.guide.android.tools.GAndroidEventListener
import com.iwedia.guide.android.tools.GAndroidPrefsHandler
import com.iwedia.guide.android.tools.GAndroidScene
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import com.iwedia.guide.android.tools.debug.debugViews.DebugLayerView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import core_entities.ContentEntity
import core_entities.PlayableItem
import core_entities.PlayableItemType
import dagger.hilt.android.AndroidEntryPoint
import data_type.GList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus
import world.SceneData
import world.SceneListener
import world.SceneManager
import world.WorldHandler
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

/**
 * Main activity
 *
 * @author Veljko Ilkic
 */
@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.R)
class MainActivity : GAndroidActivity<
        SceneListener,
        GAndroidScene<GAndroidSceneFragment<GAndroidSceneFragmentListener>, SceneListener>,
        GAndroidSceneFragment<GAndroidSceneFragmentListener>,
        GAndroidSceneFragmentListener,
        WorldHandler,
        MainActivity.AndroidEventListener
        >(), ToastInterface {

    private val SCAN_ACTION = "com.iwedia.cltv.SCAN"
    private val VIEW_ACTION = "android.intent.action.VIEW"
    val CUSTOM_SCAN_MIDDLEWARE_CODE = 1000

    /**
     * Request code for runtime permissions
     */
    private val REQUEST_CODE = 123
    private val STORAGE_PERMISSION_REQUIRED_MAX_SDK = 32;
    private var appPermissions = mutableListOf(
        "android.permission.INTERNET",
        "com.android.providers.tv.permission.READ_EPG_DATA",
        "com.android.providers.tv.permission.WRITE_EPG_DATA",
    )

    /**
     * List of optional permissions that are not mandatory.
     * Users are prompted to grant these permissions in every launch if they are not granted.
     */
    //add permissions in this list that needs to be checked.
    private var appPermissionsOptional = mutableListOf(
        "android.permission.READ_TV_LISTINGS",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_FINE_LOCATION"
    )

    // Flag indicating whether the application is currently waiting for user permission dialogs to complete.
    var isWaitingForUserPermission = false

    private val TV_INPUT_SETUP_ACTIVITY_REQUEST_CODE = 999
    private val TOS_ACTIVITY_REQUEST_CODE = 777

    /**
     * Remember value of last key when slowing down key events
     */
    var lastKeyRecognized: Long = -1

    var liveTvView: TvView? = null
    var pvrPlaybackTvView: TvView? = null
    var debugLayerView: DebugLayerView? = null

    var stepTitle: TextView? = null
    var stepSubtitle: TextView? = null
    var stepContainer: ConstraintLayout? = null

    var stepChannelListTitle: TextView? = null
    var stepChannelListSubtitle: TextView? = null
    var stepChannelListContainer: ConstraintLayout? = null

    var stepInfoBannerTitle: TextView? = null
    var stepInfoBannerSubtitle: TextView? = null
    var stepInfoBannerContainer: ConstraintLayout? = null

    var stepMenuTitle: TextView? = null
    var stepMenuSubtitle: TextView? = null
    var stepMenuContainer: ConstraintLayout? = null

    var loadingProgressBar: ProgressBar? = null

    var overlayFeatureContainer: RelativeLayout? = null
    var overlayFeatureView: OverlayFeatureView? = null

    /**
     * Recording/TimeShift indication
     */
    var indicator: ConstraintLayout? = null
    var indicatorChannelTextView: TextView? = null
    var indicatorChannelImageView: ImageView? = null
    var indicatorIcon: ImageView? = null
    var testingIndication: RelativeLayout? = null
    var preloadedChannel: TvChannel? = null
    var testingTitle: TextView? = null
    var tvChannelId: Long = -1
    var channelUri: Uri? = null
    var channelUriFromPrefs: String = ""

    var usbReceiver: UsbReceiver? = null
    lateinit var scanStartedReceiver: ScanStartedReceiver
    lateinit var languageChangeReceiver: LanguageChangeReceiver

    var isIntroPaused = false
    var isTimeShiftPaused = false

    val TAG = "MainActivity: "

    private val SCAN_FINISHED_INTENT_DATA = "isScanFinished"
    private val CHANNEL_INTENT_DATA = "content://android.media.tv/channel"
    private val RECORD_INTENT_DATA = "content://android.media.tv/recorded_program"

    private val APP_ICON_URL =
        "https://firebasestorage.googleapis.com/v0/b/admin-panel-8eb67.appspot.com/o/config%2Fapp_logo.png?alt=media"

    private var channelRowTimer: Timer? = null

    private var enteredSecretCode: String = ""

    var mKeyUpEvent: KeyEvent? = null
    var mKeyDownEvent: KeyEvent? = null
    var livePlaybackView: RelativeLayout? = null

    //Teletext SurfaceView
    var ttxSurfaceView: SurfaceView? = null
    var subtitleSurfaceView: SurfaceView? = null
    var ttmlViewContainer: RelativeLayout? = null

    // dir for recommendation channel list
    var recommendationImagePath: File? = null
    val previewBuilderList: MutableList<PreviewProgram.Builder> = mutableListOf()
    var isRecommendationInProgress = false
    var hasInternet = false
    private var updateEPGAgainAtStart = true
    var lastChangeInputTime: Long? = null

    private val MAX_COUNT_LAUNCHER_RECOMMENDATION = 10

    private var conflictWatchListItems = mutableListOf<TvEvent>() /*to store watchlist events with same start time*/
    val conflictRecordingAndWatchListItems = mutableListOf<TvEvent>()
    private var isPlaybackStop = false
    var isTimeshiftDestroyed = false
    var inputSourceLayout : View ? =null

    /** Module provider */
    private lateinit var moduleProvider: ModuleProvider

    /** AudioFocusManager */
    private var mAudioManager: AudioManager? = null

    /** Event listener */
    private var proceedClicked = false
    private var channelsLoaded = false
    private var inputSelected = "TV"
    private var inputTuneURL = ""
    var inputSourceSelected = "TV"
    var isExternalInput = false

    /** No signal auto power off */
    var inactivityTimer = InactivityTimer
    private var inBackground = false
    private var exitApplicationAfterScan = false
    private var accessibilityWasOn = false

    var sceneInactivityTimer: SceneInactivityTimer? = null
    private var isFromOnNewIntent: Boolean? = false

    var settingsIconClicked = false
//    var waitingStartChannelFromLiveTab = false
    var startChannelFromLiveTab = false
    private var channelIdFromLiveTab :Int? = null
    private var isHdmiCompVideoAvailable : Boolean?= false
    private val CONFLICT_OFFSET = 1 * 60 * 1000


    /**
     *  APP_BACKGROUND_TIME_LIMIT defines the longest time app can resume normally.
     */
    private val APP_BACKGROUND_TIME_LIMIT = 60 * 60 * 1000L // 1 Hour

    /**
     * Timer to exit the application after a specified duration of being in the background.
     */
    private var backgroundExitTimer: Timer? = null

    /** BroadcastReceiver for handling fast EPG updates. */
    private var fastEPGUpdateReceiver: BroadcastReceiver? = null

    private var fastChannelReorderReceiver: BroadcastReceiver? = null

    //todo mtk needs to send event | until than this will do
    var newChannelsFoundResetApp = false

    private var startScan = false
    private var initCiFirstTime = true
    private var inCiMenu =  false
    var openedGuideFromIntent = false
    private var tosCanceled = false

    private lateinit var analytics: FirebaseAnalytics

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ResourceType", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var featureExists = false
        var systemAvailableFeatures = this.packageManager.systemAvailableFeatures
        for (saf in systemAvailableFeatures) {

            if (saf.name!=null && saf.name.contains("rialto")) {
                featureExists = true
                break
            }
        }
        analytics = Firebase.analytics
        //todo this needs to be removed for now
        if (BuildConfig.BUILD_TYPE == "release"){
            var anokiUid = this.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).getString(
                FastAnokiUidHelper.ANOKI_UID_TAG, "").toString()
            if (FirebaseCrashlytics.getInstance() != null) {
                FirebaseCrashlytics.getInstance().setUserId(anokiUid)
            }
            if (analytics != null) {
                analytics.setUserId(anokiUid)
            }
            if (!featureExists && !BuildConfig.FLAVOR.contains("refplus5") && !BuildConfig.FLAVOR.contains("rtk") && !BuildConfig.FLAVOR.contains("t56")) {
                showToast( "Cannot run application on this device", UtilsInterface.ToastDuration.LENGTH_LONG)
                Handler().postDelayed({
                    this.finishAffinity()
                }, 5000)
                return
            }
        } else {
            try {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
                analytics.setAnalyticsCollectionEnabled(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        applicationContext.getSharedPreferences(UtilsInterface.PREFS_TAG, Context.MODE_PRIVATE).edit()
            .putString("version_name", BuildConfig.VERSION_NAME).apply()
        settingsIconClicked = false



        if(android.os.Build.VERSION.SDK_INT > STORAGE_PERMISSION_REQUIRED_MAX_SDK){
            appPermissions.remove("android.permission.WRITE_EXTERNAL_STORAGE");
            appPermissions.remove("android.permission.READ_EXTERNAL_STORAGE");
        }
        ReferenceApplication.isInForeground = true
        ReferenceApplication.isAppPaused = false
        ReferenceApplication.isInputPaused = false

        sceneInactivityTimer = SceneInactivityTimer

        CoroutineHelper.runCoroutine({
            GAndroidPrefsHandler(this).storeValue("setupCompleted", true)
        })

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate: BUILD TYPE ${BuildConfig.BUILD_TYPE} ${BuildConfig.FLAVOR}")
        //Default back behaviour
        handleBackKey = false

        ReferenceApplication.setActivity(this)
        if (BuildConfig.FLAVOR.contains("mal_service")) {
            moduleProvider = ReferenceApplication.moduleProvider!!
        } else {
            moduleProvider = ModuleProvider(this.application)
        }

        GlobalAppReceiver.setModuleProvider(moduleProvider)
        (worldHandler as ReferenceWorldHandler).setModuleProvider(moduleProvider)

        com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener = object: com.iwedia.cltv.platform.model.information_bus.events.InformationBus.InformationBusEventListener {
            override fun submitEvent(eventId: Int, data: ArrayList<Any>) {
                try {
                    if(eventId == Events.CHANNELS_LOADED) {
                        channelsLoadedEvent()
                    }
                    var d = if (data.isNotEmpty()) data[0] else data
                    InformationBus.submitEvent(Event(eventId, *data.toTypedArray()))
                }catch (E: Exception){
                    println(E)
                }
            }

            override fun registerEventListener(
                eventIds: ArrayList<Int>,
                callback: (eventListener: Any) -> Unit,
                onEventReceived: (eventId: Int) -> Unit
            ) {
                class GenericEventListener: EventListener() {
                    init {
                        eventIds.forEach { id->
                            addType(id)
                        }
                    }

                    override fun callback(event: Event?) {
                        onEventReceived.invoke(event?.type!!)
                    }
                }
                var eventListener = GenericEventListener()
                callback.invoke(eventListener)
                InformationBus.registerEventListener(eventListener)
            }

            override fun unregisterEventListener(eventListener: Any) {
                InformationBus.unregisterEventListener(eventListener as EventListener)
            }
        }

        worldHandler.setup()
        ReferenceApplication.worldHandler = worldHandler as ReferenceWorldHandler
        ReferenceApplication.worldHandler!!.isEnableUserInteraction = false

        val eventListener = AndroidEventListener(this, worldHandler)
        setup(eventListener, R.layout.reference_activity_main)

        startIntro()

        observeLastInterfaceChange()

        if (BuildConfig.FLAVOR.contains("base") || moduleProvider.getPlatformOSModule().getPlatformName().contains("FAST")) {
            PreferenceInterface.ENABLE_HYBRID = false
        }

        moduleProvider.getUtilsModule().stringTranslationListener = object : UtilsInterface.StringTranslationListener {
            override fun getStringValue(stringId: String): String {
                return ConfigStringsManager.getStringById(stringId)
            }
        }
        moduleProvider.getGeneralConfigModule().setup(applicationContext.resources.openRawResource(R.raw.general_settings))
        moduleProvider.getInputSourceMoudle().setup(false)

        moduleProvider.getEasModule().initializeEas()
        moduleProvider.getClosedCaptionModule().initializeClosedCaption()
        moduleProvider.getUtilsModule().getAudioDescriptionEnabled(null)

        accessibilityWasOn = moduleProvider.getUtilsModule().isAccessibilityEnabled()

        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        accessibilityManager.addAccessibilityStateChangeListener { state ->
            if(!inBackground) {
                CoroutineHelper.runCoroutineWithDelay({
                    Utils.restartApp()
                }, 10)
            }
        }

        if (!moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("hbbtv")){
            moduleProvider.getHbbTvModule().supportHbbTv(false)
        }

        OadPopUpManager.setOatUpdateModule(moduleProvider.getOadUpdateModule())


        // Init fast zap banner data provider
        CoroutineScope(Dispatchers.IO).launch {
            FastZapBannerDataProvider.init(
                moduleProvider.getTvModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getTextToSpeechModule()
            )

            // Init fast live tab data provider
            FastLiveTabDataProvider.init(
                moduleProvider.getTvModule(),
                moduleProvider.getEpgModule(),
                moduleProvider.getUtilsModule(),
                moduleProvider.getParentalControlSettingsModule(),
                moduleProvider.getCategoryModule(),
                moduleProvider.getFastFavoriteModule(),
                moduleProvider.getNetworkModule(),
                moduleProvider.getPlayerModule(),
                moduleProvider.getTextToSpeechModule(),
                moduleProvider.getFastUserSettingsModule(),
                moduleProvider.getPvrModule(),
                moduleProvider.getTimeshiftModule(),
                moduleProvider.getForYouModule() // TODO BORIS this is not needed here - it's only used to fetch data for the Vod screen (mockuping data for the rails) - delete this argument once VOD is separated from the app
            )
        }

        (worldHandler as ReferenceWorldHandler).initHelperClasses()
        debugLayerView = findViewById(R.id.debug_layer_view);
        overlayFeatureContainer = findViewById(R.id.overlayFeatureViewContainer)

        val testingText: TextView = findViewById(R.id.testing_text)
        testingText.setText(ConfigStringsManager.getStringById("testing_in_progress"))

        if (BuildConfig.FLAVOR.contains("mtk")) {
            moduleProvider.getUtilsModule().enableTimeshift(true)
        }

        val pinActivityLoading = findViewById<RelativeLayout>(R.id.pin_activity_loading)
        PinHelper.setLoadingLayoutCallback {
            runOnUiThread {
                pinActivityLoading.visibility = if (it) View.VISIBLE else View.GONE
            }
        }

        //Setup player
        livePlaybackView = findViewById<RelativeLayout>(R.id.livePlaybackView)
        livePlaybackView?.visibility = View.VISIBLE


        liveTvView = TvView(this)
        livePlaybackView?.addView(liveTvView)
        livePlaybackView?.let {
            TvViewScaler.init(it, worldHandler as ReferenceWorldHandler)
            TvViewScaler.registerResizeListener(object : TvViewScaler.TvViewRescaleListener {
                override fun onResize(state: TvViewScaler.ScaleState) {
                   moduleProvider.getTvModule().checkAndRunBarkerChannel(state == TvViewScaler.ScaleState.SCALED_DOWN)
                }
            })
        }

        if(moduleProvider.getUtilsModule().isAccessibilityEnabled()){
            liveTvView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            livePlaybackView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            var defaultValue = moduleProvider.getInputSourceMoudle().getDefaultValue()
            moduleProvider.getInputSourceMoudle().inputChanged.observeForever {
                 defaultValue =  moduleProvider.getInputSourceMoudle().getDefaultValue()
                if(defaultValue == "TV") {
                    liveTvView!!.setOnClickListener {
                        if(worldHandler.active?.id == ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE) {
                            ReferenceApplication.worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE,
                                SceneManager.Action.DESTROY
                            )
                        }
                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                            SceneManager.Action.SHOW_OVERLAY
                        )
                    }
                } else {
                    liveTvView!!.setOnClickListener(null)
                }
            }
        }

        //Setup Teletext Surface
        ttxSurfaceView = findViewById<SurfaceView>(R.id.ttx_surface_view)
        if (ttxSurfaceView != null) {
            ttxSurfaceView!!.holder.setFormat(PixelFormat.RGBA_8888)
            if (BuildConfig.FLAVOR == "rtk") {
                ttxSurfaceView!!.setZOrderMediaOverlay(true)
                ttxSurfaceView!!.holder.setFormat(PixelFormat.TRANSLUCENT)
            }
            ttxSurfaceView!!.holder.addCallback(object: SurfaceHolder.Callback{
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Log.i(TAG, "surfaceCreated: Teletext Surface Created")
                    moduleProvider.getPlayerModule().setTeletextSurface(holder)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Log.i(TAG, "surfaceChanged: Teletext Surface Changed")
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Log.i(TAG, "surfaceDestroyed: Teletext Surface Destroyed")
                    moduleProvider.getPlayerModule().setTeletextSurface(null)
                }
            })
        }

        //Setup Subtitle Surface
        subtitleSurfaceView = findViewById<SurfaceView>(R.id.subtitle_surface_view)
        if (subtitleSurfaceView != null) {
            subtitleSurfaceView!!.setZOrderMediaOverlay(true)
            subtitleSurfaceView!!.holder.setFormat(PixelFormat.TRANSLUCENT)
            subtitleSurfaceView!!.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    Log.i(TAG, "surfaceCreated: subtitle Surface Created")
                    moduleProvider.getPlayerModule().setSubtitleSurface(holder)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    Log.i(TAG, "surfaceChanged: subtitle Surface Changed")
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    Log.i(TAG, "surfaceDestroyed: subtitle Surface Destroyed")
                    moduleProvider.getPlayerModule().setSubtitleSurface(null)
                }
            })
        }

        ttmlViewContainer = findViewById(R.id.ttmlView)
        moduleProvider.getPlayerModule().initTTML(ttmlViewContainer!!)

        //setup audio manager
        mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        //Setup player
        var pvrPlaybackView = findViewById<RelativeLayout>(R.id.pvrPlaybackView)
        pvrPlaybackView.visibility = View.VISIBLE

        pvrPlaybackTvView = TvView(this)
        pvrPlaybackView.addView(pvrPlaybackTvView)

        var intentData = intent.getData().toString()

        registerFastEPGUpdateReceiver()
        registerFastChannelReorderReceiver()
        ScanHelper.registerScanReceiver(applicationContext)

        //setup company info manager
        ConfigCompanyDetailsManager.setup(moduleProvider.getUtilsModule())
        //setup brand color manager
        ConfigColorManager.setup(moduleProvider.getUtilsModule())
        inputSelected = intent?.extras?.getString("input_main_name").toString()
        inputSourceSelected = intent?.extras?.getString("input_source_name").toString()
        inputTuneURL = intent?.extras?.getString("input_tune_url").toString()

        if(intent?.action.equals("android.input_onetouch") || intent?.action.equals("com.iwedia.cltv.inputs.assistant")) {
            ReferenceApplication.isCecTuneFromIntro = true
        }
        tuneToInputViaAssistant(intent)
        if (intent?.action.equals("keycode_keyinput") && inputSelected != null && inputSelected != "TV") {
            moduleProvider.getInputSourceMoudle().setInputActiveName(inputSourceSelected)
            var isBlocked = intent?.extras?.getBoolean("input_blocked")
                if (!isBlocked!!) {
                    isExternalInput = true
                }

        } else {
            inputSelected = moduleProvider.getInputSourceMoudle().getDefaultValue()
            inputSourceSelected = moduleProvider.getInputSourceMoudle().getInputActiveName()
            inputTuneURL = moduleProvider.getInputSourceMoudle().getDefaultURLValue()
            if (inputSelected.contains("Home")) {
                setInputToTv()
                tuneToTv()

            }
            startIntro()
        }
        var languageCode =
            if (Locale.getDefault().language.uppercase() == moduleProvider.getUtilsModule()
                    .getCountry().uppercase()
            ) {
                Locale.getDefault().language.uppercase()
            } else {
                Locale.getDefault().language.uppercase() + "_" + moduleProvider.getUtilsModule()
                    .getCountry().uppercase()
            }

        CoroutineHelper.runCoroutine({
            ConfigHandler.setup(moduleProvider, languageCode, object : AsyncReceiver {
                override fun onSuccess() {
                    runOnUiThread {
                        try {
                            val color_context =
                                Color.parseColor(ConfigColorManager.getColor("color_text_description"))
                            testingText.setTextColor(color_context)
                        } catch (ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: Exception color $ex")
                        }
                        stepTitle = findViewById(R.id.step_title)
                        stepSubtitle = findViewById(R.id.step_subtitle)
                        stepSubtitle!!.setTextColor(
                            Color.parseColor(
                                ConfigColorManager.getColor(
                                    "color_main_text"
                                )
                            )
                        )
                        stepContainer = findViewById(R.id.step_main_container)

                        stepChannelListTitle = findViewById(R.id.step_channel_list_title)
                        stepChannelListSubtitle = findViewById(R.id.step_channel_list_subtitle)
                        stepChannelListSubtitle!!.setTextColor(
                            Color.parseColor(
                                ConfigColorManager.getColor("color_main_text")
                            )
                        )
                        stepChannelListContainer =
                            findViewById(R.id.step_channel_list_main_container)

                        stepInfoBannerTitle = findViewById(R.id.step_info_banner_title)
                        stepInfoBannerSubtitle = findViewById(R.id.step_info_banner_subtitle)
                        stepInfoBannerSubtitle!!.setTextColor(
                            Color.parseColor(
                                ConfigColorManager.getColor("color_main_text")
                            )
                        )
                        stepInfoBannerContainer =
                            findViewById(R.id.step_info_banner_main_container)

                        stepChannelListTitle = findViewById(R.id.step_channel_list_title)
                        stepChannelListSubtitle = findViewById(R.id.step_channel_list_subtitle)
                        stepChannelListContainer =
                            findViewById(R.id.step_channel_list_main_container)

                        stepMenuTitle = findViewById(R.id.step_menu_title)
                        stepMenuSubtitle = findViewById(R.id.step_menu_subtitle)
                        stepMenuSubtitle!!.setTextColor(
                            Color.parseColor(
                                ConfigColorManager.getColor(
                                    "color_main_text"
                                )
                            )
                        )
                        stepMenuContainer =
                            findViewById(R.id.step_menu_main_container)

                        loadingProgressBar = findViewById(R.id.loadingProgressBar)
                        testingIndication = findViewById(R.id.testing_indicator)
                        testingIndication?.backgroundTintList = ColorStateList.valueOf(
                            Color.parseColor(
                                ConfigColorManager.getColor("color_main_text")
                                    .replace("#", ConfigColorManager.alfa_light)
                            )
                        )

                        testingTitle = findViewById(R.id.testing_text)
                    }
                }

                override fun onFailed(error: core_entities.Error?) {
                }
            })
        })

        moduleProvider.getClosedCaptionModule().setCCInfo()
        moduleProvider.getTTXModule().addCallback(this)

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate: ConfigHandler 00")

        if ((worldHandler as ReferenceWorldHandler).isFastOnly()) {
            checkRuntimePermissions()
        } else {
            startSdkInit()
        }

        //Add Broadcast receiver for USB
        //if region is not atsc (no recording and timeshift options)
        if (moduleProvider.getUtilsModule().getRegion() != Region.US) {
            if(moduleProvider.getUtilsModule().isUsbFormatEnabled()) {
                 usbReceiver = UsbReceiver()
                 usbReceiver!!.registerForUSB()
            }
        }

        //[Ref_5.0]: Display blue mute on application start if there is no signal
        if(!moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("blue_mute")){
            moduleProvider.getUtilsModule().setBlueMute(false)
        }else {
            if (moduleProvider.getUtilsModule().getBlueMuteState()) {
                moduleProvider.getUtilsModule()
                    .setBlueMute(true)
            }
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate: IS SERVICE RUNNING ${isServiceRunning(this,
            ContentAggregatorService::class.java)}")

        if (!isServiceRunning(this, ContentAggregatorService::class.java)) {
            startService(this, ContentAggregatorService::class.java)
        }

        scanStartedReceiver = ScanStartedReceiver()
        val intentFilter = IntentFilter(ScanStartedReceiver.SCAN_STARTED_INTENT_ACTION)
        registerReceiver(scanStartedReceiver, intentFilter)

        languageChangeReceiver = LanguageChangeReceiver()
        val languageChangeIntentFilter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        registerReceiver(languageChangeReceiver, languageChangeIntentFilter)

        //TODO channel logo metadata enabled usage example
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onCreate: channel logo metadata enabled ${
            moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo(
                "channel_logo_metadata_enabled"
            )
        }")

        setupIndicator()
        registerNetworkObserver()
        checkNoSignalPowerOff()
        moduleProvider.getUtilsModule().setApplicationRunningInBackground(true)
    }

    private fun startIntro() {
        if (inputSelected == "TV" && !ReferenceApplication.isCecTuneFromIntro && !ReferenceApplication.parentalControlDeepLink) {
            runOnUiThread {
                if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.INTRO) {
                    return@runOnUiThread
                }
                worldHandler.triggerAction(
                    ReferenceWorldHandler.SceneId.INTRO,
                    SceneManager.Action.SHOW
                )
            }
        }
    }

    private fun setLiveTvInput() {
        moduleProvider.getTvModule().getActiveChannel(object :
            IAsyncDataCallback<TvChannel> {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFailed(error: Error) {
                moduleProvider.getInputSourceMoudle().setLastUsedInput("")
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(data: TvChannel) {
                moduleProvider.getInputSourceMoudle().setLastUsedInput(data.inputId)
            }
        })
    }


    /**
     * Registers a BroadcastReceiver for fast EPG intermittent updates.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerFastEPGUpdateReceiver(){
        fastEPGUpdateReceiver = object : BroadcastReceiver() {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Anoki intermittent EPG update intent received")
                moduleProvider.getEpgModule().updateEpgData(ApplicationMode.FAST_ONLY)
                if(intent.getBooleanExtra("is_loading_finished",false)){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Anoki intermittent EPG update finished")
                    // Unregister the receiver once initial events loading is finished
                    unregisterReceiver(fastEPGUpdateReceiver)
                }
            }
        }
        registerReceiver(fastEPGUpdateReceiver, IntentFilter(ReferenceApplication.FAST_EPG_RESULT))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerFastChannelReorderReceiver(){
        fastChannelReorderReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: got channel reordering intent")
                InformationBus.submitEvent(Event(Events.ANOKI_CHANNEL_LIST_REORDERED))
            }

        }
        registerReceiver(fastChannelReorderReceiver, IntentFilter(ReferenceApplication.FAST_CHANNEL_REORDER),
            RECEIVER_NOT_EXPORTED)
    }

    private fun startService(context: Context, serviceClass: Class<*>) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startService: START SERVICE FROM MAIN ACTIVITY")
        val intent = Intent(context, serviceClass)
        try {
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }

    /**
     * Start sdk init
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun startSdkInit() {
        if (BuildConfig.FLAVOR.contains("mal_service")) {
            CoroutineHelper.runCoroutineWithDelay({
                moduleProvider.refresh()

            }, 1000)
            (worldHandler as ReferenceWorldHandler).setPlaybackViews()
        } else {
            runOnUiThread {
                CoroutineHelper.runCoroutine({
                    moduleProvider.refresh()

                })
                (worldHandler as ReferenceWorldHandler).setPlaybackViews()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun resetTvView(tvView: TvView) {
        liveTvView = tvView
        livePlaybackView?.removeAllViews()
        livePlaybackView?.addView(liveTvView)
        livePlaybackView?.let {
            TvViewScaler.init(it, worldHandler as ReferenceWorldHandler)
        }
        if (!BuildConfig.FLAVOR.contains("mal_service")) {
            (worldHandler as ReferenceWorldHandler).setPlaybackViews()
        }
    }

    private fun channelsLoadedEvent() {
        if(inCiMenu){
            return
        }

        if(initCiFirstTime && moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("ci")) {
            initCiFirstTime = false
            //Init CI plus communication
            initCiPlusCommunication()
        }

        ReferenceApplication.isInitalized = true
        moduleProvider.getFavoriteModule().addFavoriteInfoToChannels()

        channelsLoaded = true
        if (proceedClicked) {
            proceedClicked = false
            ReferenceApplication.runOnUiThread {
                if (moduleProvider.getTvModule().getChannelList().size == 0) {
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
                        SceneManager.Action.SHOW
                    )
                } else {
                    ReferenceApplication.worldHandler!!.destroyExisting()
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.LIVE,
                        SceneManager.Action.SHOW
                    )

                    moduleProvider.getSchedulerModule().clearRecordingList()
                    moduleProvider.getCategoryModule().setActiveEpgFilter(HorizontalGuideSceneWidget.GUIDE_FILTER_ALL)
                }
            }
        }
    }

    inner class AndroidEventListener : GAndroidEventListener<
            SceneListener,
            GAndroidScene<GAndroidSceneFragment<GAndroidSceneFragmentListener>, SceneListener>,
            GAndroidSceneFragment<GAndroidSceneFragmentListener>,
            GAndroidSceneFragmentListener,
            WorldHandler
            > {

        constructor(activity: MainActivity, androidWorldHandler: WorldHandler) : super(
            activity,
            androidWorldHandler
        ) {
            addType(Events.CHANNELS_LOADED)
            addType(Events.EVENTS_LOADED)
            addType(Events.PROCEED_CLICKED)
            addType(Events.CHANNEL_LIST_UPDATED)
            addType(Events.SCHEDULED_RECORDING_NOTIFICATION)
            addType(Events.RECORDING_SCHEDULED_TOAST)
            addType(Events.SCHEDULED_RECORDING_REMINDER_CONFLICTS_NOTIFICATION)
            addType(Events.START_TV_INPUT_SETUP_ACTIVITY)
            addType(Events.SCHEDULED_REMINDER_NOTIFICATION)
            addType(Events.WATCHLIST_SCHEDULED_TOAST)
            addType(Events.BLOCK_TV_VIEW)
            addType(Events.LAUNCH_INPUT_HOME)
            addType(Events.UNBLOCK_TV_VIEW)
            addType(Events.VIDEO_RESOLUTION_AVAILABLE)
            addType(Events.VIDEO_RESOLUTION_UNAVAILABLE)
            addType(Events.PLAYBACK_STARTED)
            addType(Events.PLAYBACK_STOPPED)
            addType(Events.NO_PLAYBACK)
            addType(Events.PLAYER_TIMEOUT)
            addType(Events.NO_CHANNELS_AVAILABLE)
            addType(Events.WAITING_FOR_CHANNEL)
            addType(Events.NO_SIGNAL_POWER_OFF_TIME_CHANGED)
            addType(Events.NO_SIGNAL_POWER_OFF_ENABLED)
            addType(Events.NO_SIGNAL_POWER_OFF_TIMER_END)
            addType(Events.EXIT_APPLICATION_ON_SCAN)
            addType(Events.RECHECK_APP_PERMISSIONS)
            addType(Events.EXIT_APPLICATION_ON_BACK_PRESS)
            addType(Events.INACTIVITY_TIMER_END)
            addType(Events.GET_INPUT_RESOLUTION_DATA)
            addType(Events.WATCHLIST_INPUT)
            addType(Events.HANDLE_INPUT_CEC_TUNE)
            addType(Events.SHOW_ANOKI_MODE_TOAST)
            addType(Events.SCHEDULED_RECORDING_CONFLICT)
            addType(Events.SHOW_STOP_TIME_SHIFT_DIALOG)
            addType(Events.USB_DEVICE_CONNECTED)
            addType(Events.USB_DEVICE_DISCONNECTED)
            addType(Events.PVR_RECORDING_INTERRUPTED)
            addType(Events.SHOW_USB_FULL_DIALOG)
            addType(Events.SHOW_NEW_CHANNELS_FOUND_POPUP)
            addType(Events.ANOKI_REGION_NOT_SUPPORTED)
            addType(Events.PLAYBACK_STATUS_MESSAGE_IS_SCRAMBLED)
            addType(Events.SHOW_CHANNEL_CHANGE_TOAST_FOR_T56)
            addType(Events.NO_AV_OR_AUDIO_ONLY_CHANNEL)
            addType(Events.SHOW_DEVICE_INFO)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun callback(event: Event?) {
            if (event?.type == 5) {
                //Show scene event
                if (event.getData(0) is ReferenceScene) {
                    var scene = event.getData(0) as ReferenceScene
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "SHOW Scene event ${scene.name}")
                    if ((worldHandler as ReferenceWorldHandler).isAlreadyShown(scene.id)) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "SHOW SCENE event, scene already visible ${scene.name}")
                        return
                    }
                }
            }
            try {
                super.callback(event)
            } catch (e: Exception) {
                Log.e(MainActivity::class.java.toString(), e.message.toString())
            }
            if (event!!.type == Events.CHANNELS_LOADED) {
                channelsLoadedEvent()
            }
            else if(event!!.type == Events.PLAYBACK_STATUS_MESSAGE_IS_SCRAMBLED || event!!.type == Events.NO_AV_OR_AUDIO_ONLY_CHANNEL){
                var displayId = display?.displayId
                if(displayId != null) {
                    moduleProvider.getHbbTvModule().setSurfaceView(ttxSurfaceView!!, displayId, 0)
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG,"Display id not found, defaulting to 0")
                    moduleProvider.getHbbTvModule()
                        .setSurfaceView(ttxSurfaceView!!, 0, 0)
                }
                val outSize = Point()
                window.windowManager.defaultDisplay.getRealSize(outSize)
                livePlaybackView?.let {
                    moduleProvider.getHbbTvModule().setTvView(
                        liveTvView!!,
                        it,
                        outSize.x.toFloat(),
                        outSize.y.toFloat()
                    )
                }
            }
            else if (event.type == Events.USB_DEVICE_CONNECTED) {
                showToast(ConfigStringsManager.getStringById("usb_connected"))
            } else if (event.type == Events.USB_DEVICE_DISCONNECTED) {
                showToast(ConfigStringsManager.getStringById("usb_disconnected"))
            }
            else if (event.type == Events.PVR_RECORDING_INTERRUPTED) {
                showToast("Recording interrupted! Recording is not saved!")
            }
            else if(event.type == Events.EXIT_APPLICATION_ON_SCAN){
                exitApplicationAfterScan = true
            }
            else if(event.type == Events.RECHECK_APP_PERMISSIONS) {
                if ((worldHandler as ReferenceWorldHandler).isFastOnly()) {
                    if (!isMandatoryPermissionsGranted()) {
                        showToast( "Permissions are not granted!")
                    }
                }
            }
            else if (event.type == Events.PROCEED_CLICKED) {
                proceedClicked = true
            }
            else if (event.type == Events.SCHEDULED_RECORDING_NOTIFICATION) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "callback here as -> Events.SCHEDULED_RECORDING_NOTIFICATION")
                loadScheduleRecordingNotification(event)
            }
            else if (event.type == Events.RECORDING_SCHEDULED_TOAST) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "callback here as -> Events.RECORDING_SCHEDULED_TOAST")

                var tvEventName = ""
                moduleProvider.getSchedulerModule().getRecList(object :
                    IAsyncDataCallback<MutableList<ScheduledRecording>> {
                    override fun onFailed(error: Error) {}

                    override fun onReceive(data: MutableList<ScheduledRecording>) {
                        //get name of the last added event in recording to display toast with event name
                        tvEventName = data[data.lastIndex].tvEvent!!.name
                    }
                })
                runOnUiThread {
                    showToast("$tvEventName ${ConfigStringsManager.getStringById("rec_scheduled_toast")}")
                }
                if (!moduleProvider.getUtilsModule().isUsbConnected()) {
                    showToast(ConfigStringsManager.getStringById("scheduled_recording_USB_warning"), UtilsInterface.ToastDuration.LENGTH_LONG)
                }
            }
            else if (event.type == Events.SCHEDULED_RECORDING_CONFLICT) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "callback here as -> SCHEDULED_RECORDING_CONFLICT")
                checkRecordingScheduleConflict(event.data?.get(0) as ScheduledRecording)
            }
            else if (event.type == Events.SCHEDULED_RECORDING_REMINDER_CONFLICTS_NOTIFICATION) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "callback here as -> SCHEDULED_RECORDING_REMINDER_CONFLICTS_NOTIFICATION")
                loadScheduledRecordingReminder(event)
            }
            else if (event.type == Events.START_TV_INPUT_SETUP_ACTIVITY) {
                try {
                    val intent = event.data?.get(0) as Intent
                    startActivityForResult(intent, TV_INPUT_SETUP_ACTIVITY_REQUEST_CODE)
                } catch (e: Exception) {
                    showToast("Please try another one", UtilsInterface.ToastDuration.LENGTH_LONG)
                }
            }
            else if(event.type == Events.SCHEDULED_REMINDER_NOTIFICATION) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "callback here as -> Events.SCHEDULED_REMINDER_NOTIFICATION")
                if (!ReferenceApplication.isInForeground)return
                if (!ReferenceApplication.isInitalized) return

                if (((inputSelected.contains("HDMI") || inputSelected.contains("Composite")) && (!moduleProvider.getInputSourceMoudle().isBlock("TV"))) ||
                    inputSelected == "TV") {
                    if (event.getData(0) != null && event.getData(0) is ScheduledReminder) {
                        val tvEventId = (event.getData(0) as ScheduledReminder).tvEventId
                        moduleProvider.getEpgModule()
                            .getEventById(tvEventId!!, object : IAsyncDataCallback<TvEvent> {
                                override fun onFailed(error: Error) {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: error == ${error.message}")
                                }

                                override fun onReceive(data: TvEvent) {
                                    if (!conflictWatchListItems.contains(data)) {
                                        conflictWatchListItems.add(data)
                                    }
                                    Log.d(Constants.LogTag.CLTV_TAG +
                                        TAG,
                                        "onReceive: conflictWatchListItems == ${conflictWatchListItems.size}"
                                    )
                                }
                            })
                    }

                     CoroutineHelper.runCoroutineWithDelay({
                        if (event.data != null && event.getData(0) is ScheduledReminder) {
                            if (conflictWatchListItems.size == 1) {
                                /*when there is only one scheduled watchlist event at a particular time*/
                                val tvEventId = (event.getData(0) as ScheduledReminder).tvEventId
                                moduleProvider.getEpgModule()
                                    .getEventById(
                                        tvEventId!!,
                                        object : IAsyncDataCallback<TvEvent> {
                                            override fun onFailed(error: Error) {
                                            }

                                            override fun onReceive(data: TvEvent) {
                                                runOnUiThread {
                                                    val currentActiveScene =
                                                        ReferenceApplication.worldHandler!!.active
                                                    val sceneData =
                                                        DialogSceneData(
                                                            currentActiveScene!!.id,
                                                            currentActiveScene.instanceId
                                                        )
                                                    sceneData.type =
                                                        DialogSceneData.DialogType.SCHEDULED_REMINDER
                                                    sceneData.title = data.name
                                                    sceneData.imageRes =
                                                        R.drawable.ic_watchlist_reminder
                                                    var message =
                                                        ConfigStringsManager.getStringById("watchlist_reminder_message")
                                                    message =
                                                        message.replace("%s", data.tvChannel.name)
                                                    sceneData.message = message
                                                    if (!moduleProvider.getPvrModule()
                                                            .isRecordingInProgress() && !moduleProvider.getTimeshiftModule().isTimeShiftActive
                                                    ) {
                                                        sceneData.subMessage =
                                                            ConfigStringsManager.getStringById("time_left_reminder_message")
                                                    }
                                                    sceneData.positiveButtonText =
                                                        ConfigStringsManager.getStringById("watch")
                                                    sceneData.negativeButtonText =
                                                        ConfigStringsManager.getStringById("cancel")

                                                    //jump to channel if nothing is clicked
                                                    var autoStartWatchlist: Job? = null
                                                    var isRecordingInProgress: Boolean
                                                    var isTimeShiftInProgress: Boolean
                                                    autoStartWatchlist =
                                                        CoroutineHelper.runCoroutineWithDelay(
                                                            {
                                                                isRecordingInProgress =
                                                                    moduleProvider.getPvrModule()
                                                                        .isRecordingInProgress()
                                                                isTimeShiftInProgress =
                                                                    moduleProvider.getTimeshiftModule().isTimeShiftActive
                                                                if (!isRecordingInProgress && !isTimeShiftInProgress) {
                                                                    worldHandler.triggerAction(
                                                                        ReferenceApplication.worldHandler!!.active!!.id,
                                                                        SceneManager.Action.DESTROY
                                                                    )
                                                                    if (inputSelected.contains("HDMI") || inputSelected.contains(
                                                                            "Composite"
                                                                        )
                                                                    ) {
                                                                        setInputToTv()
                                                                        com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                                                                            Events.VIDEO_RESOLUTION_UNAVAILABLE,
                                                                            arrayListOf(0)
                                                                        )
                                                                    }
                                                                    startWatchlistProgram(data.tvChannel)
                                                                }
                                                            }, 30000, Dispatchers.Main
                                                        )

                                                    sceneData.dialogClickListener =
                                                        object :
                                                            DialogSceneData.DialogClickListener {
                                                            override fun onNegativeButtonClicked() {
                                                                autoStartWatchlist.cancel()
                                                                if (worldHandler.active is HomeSceneManager) {
                                                                    val homescene =
                                                                        (worldHandler.active as HomeSceneManager).scene
                                                                    if (homescene is HomeSceneFast) {
                                                                        homescene.refreshForYou()
                                                                    } else if (homescene is HomeSceneBroadcast) {
                                                                        homescene.refreshForYou()
                                                                    }
                                                                    else if (homescene is HomeSceneVod) {
                                                                        homescene.refreshForYou()
                                                                    }
                                                                }
                                                            }

                                                            override fun onPositiveButtonClicked() {
                                                                if (inputSelected.contains("HDMI") || inputSelected.contains(
                                                                        "Composite"
                                                                    )
                                                                ) {
                                                                    setInputToTv()
                                                                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                                                                        Events.VIDEO_RESOLUTION_UNAVAILABLE,
                                                                        arrayListOf(0)
                                                                    )
                                                                }
                                                                worldHandler.triggerAction(
                                                                    ReferenceApplication.worldHandler!!.active!!.id,
                                                                    SceneManager.Action.DESTROY
                                                                )
                                                                if (moduleProvider.getTimeshiftModule().isTimeShiftActive) {
                                                                    ReferenceApplication.worldHandler!!.destroyOtherExisting(
                                                                        ReferenceWorldHandler.SceneId.LIVE
                                                                    )
                                                                    InformationBus.submitEvent(
                                                                        Event(
                                                                            Events.SHOW_WATCHLIST_TIME_SHIFT_CONFLICT_DIALOG,
                                                                            data.tvChannel
                                                                        )
                                                                    )
                                                                } else startWatchlistProgram(data.tvChannel)
                                                                autoStartWatchlist.cancel()
                                                            }
                                                        }

                                                    val intent =
                                                        Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                                                    ReferenceApplication.applicationContext().sendBroadcast(intent)

                                                    worldHandler.triggerActionWithData(
                                                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                        SceneManager.Action.SHOW_OVERLAY, sceneData
                                                    )
                                                }
                                                conflictWatchListItems.clear()
                                            }
                                        })
                            }
                            else if (conflictWatchListItems.size > 1) {
                                /*when there are multiple scheduled watchlist event at a particular time*/
                                runOnUiThread {
                                    if (worldHandler.active!!.id != ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE) {
                                        val currentActiveScene =
                                            ReferenceApplication.worldHandler!!.active
                                        val sceneData =
                                            ReminderSceneData(
                                                currentActiveScene!!.id,
                                                currentActiveScene.instanceId
                                            )
                                        sceneData.listOfConflictedTvEvents = conflictWatchListItems
                                        sceneData.eventSelectedClickListener =
                                            object : ReminderSceneData.EventSelectedClickListener {
                                                override fun onEventSelected() {
                                                    conflictWatchListItems.clear()
                                                    if (ReferenceApplication.worldHandler?.active?.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                                                        val homescene =
                                                            (worldHandler.active as HomeSceneManager).scene
                                                        if (homescene is HomeSceneFast) {
                                                            homescene.refreshForYou()
                                                        } else if (homescene is HomeSceneBroadcast) {
                                                            homescene.refreshForYou()
                                                        }
                                                        else if (homescene is HomeSceneVod) {
                                                            homescene.refreshForYou()
                                                        }
                                                    }
                                                }
                                            }

                                        val intent =
                                            Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                                        ReferenceApplication.applicationContext().sendBroadcast(intent)
                                        worldHandler.triggerActionWithData(
                                            ReferenceWorldHandler.SceneId.REMINDER_CONFLICT_SCENE,
                                            SceneManager.Action.SHOW_OVERLAY, sceneData
                                        )
                                    }
                                }
                            } else {
                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, " Conflict List Size is 0 ")
                            }
                        }
                    }, 2000)
                } else {
                    conflictWatchListItems.clear()
                }
            }
            else if (event.type == Events.WATCHLIST_SCHEDULED_TOAST) {
                var tvEventName = ""
                moduleProvider.getWatchlistModule().getWatchList(object :
                    IAsyncDataCallback<MutableList<ScheduledReminder>> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: MutableList<ScheduledReminder>) {
                        //get name of the last added event in watchlist to display toast with event name
                        tvEventName = data.get(data.lastIndex).tvEvent!!.name
                    }
                })
                runOnUiThread {
                    showToast("$tvEventName ${ConfigStringsManager.getStringById("added_to_watchlist")}")
                }
            }
            else if (event.type == Events.EVENTS_LOADED) {
                if (channelsLoaded) {
                    ReferenceApplication.isInitalized = true
                }
                // Fetching all the FOR YOU data once EPG data loaded.
                moduleProvider.getForYouModule()
                    .setPvrEnabled(moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("pvr"))
                moduleProvider.getSchedulerModule().loadScheduledRecording()
                moduleProvider.getWatchlistModule().loadScheduledReminders()
            }
            else if (event.type == Events.LAUNCH_INPUT_HOME) {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(intent)
            }
            else if (event.type == Events.BLOCK_TV_VIEW) {
                if(worldHandler?.active?.id != ReferenceWorldHandler.SceneId.INTRO) {
                    if (moduleProvider.getInputSourceMoudle().isParentalEnabled()) {
                        liveTvView!!.setStreamVolume(0F)
                        ReferenceApplication.isTVInputBlocked = true
                        if (event.getData(0) != null) {
                            inputSourceSelected = event.getData(0) as String
                        }
                        val sceneData = ReferenceApplication.worldHandler?.active?.id?.let { it1 ->
                            ReferenceApplication.worldHandler?.active?.instanceId?.let { it2 ->
                                PinSceneData(
                                    it1,
                                    it2,
                                    "",
                                    object : PinSceneData.PinSceneDataListener {
                                        override fun onPinSuccess() {
                                            worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                                            if (inputSelected == "TV") {
                                                tuneToTv()
                                            }
                                            moduleProvider.getInputSourceMoudle()
                                                .handleInputSource(inputSelected, inputTuneURL)
                                            liveTvView!!.visibility = View.VISIBLE
                                            liveTvView!!.setStreamVolume(1.0F)
                                            ReferenceApplication.isTVInputBlocked = false
                                            isExternalInput = true
                                        }

                                        override fun showToast(
                                            text: String,
                                            duration: UtilsInterface.ToastDuration
                                        ) {
                                            moduleProvider.getUtilsModule().showToast(text, duration)
                                        }
                                    })
                            }
                        }
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE,
                            SceneManager.Action.SHOW_OVERLAY, sceneData
                        )
                    } else {
                        liveTvView!!.setStreamVolume(0F)
                        worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        if (inputSelected == "TV") {
                            tuneToTv()
                        }
                        moduleProvider.getInputSourceMoudle()
                            .handleInputSource(inputSelected, inputTuneURL)
                        liveTvView!!.visibility = View.VISIBLE
                        liveTvView!!.setStreamVolume(1.0F)
                        isExternalInput = true
                    }
                }
            }
            else if (event.type == Events.UNBLOCK_TV_VIEW) {
                if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE) {
                    ReferenceApplication.isTVInputBlocked = false
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    liveTvView!!.setStreamVolume(1.0F)

                }
            }
            else if (event.type == Events.VIDEO_RESOLUTION_AVAILABLE || event.type == Events.VIDEO_RESOLUTION_UNAVAILABLE) {
                if(inputSelected.contains("HDMI") || inputSelected.contains("Composite")) {
                    if (event.type == Events.VIDEO_RESOLUTION_UNAVAILABLE) {
                        var value = event.getData(0) as Int
                        if (value == 1) {
                            isHdmiCompVideoAvailable = false
                            refreshNoSignalPowerOff()
                        } else {
                            isHdmiCompVideoAvailable = true
                            stopInactivityTimer()
                        }
                    } else {
                        isHdmiCompVideoAvailable = true
                        com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(Events.VIDEO_RESOLUTION_UNAVAILABLE, arrayListOf(0))
                        stopInactivityTimer()
                    }
                }
                ReferenceApplication.isCecTuneFromIntro = false
                if (isExternalInput) {
                    var delay = 0
                    if(inputSelected != "TV") {
                        delay = 300
                    }
                    CoroutineHelper.runCoroutineWithDelay({
                        if(ReferenceApplication.worldHandler?.active?.id == ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE) {
                            ReferenceApplication.worldHandler?.triggerAction(ReferenceApplication.worldHandler?.active?.id!!, SceneManager.Action.DESTROY)
                        }
                        val sceneId = worldHandler.active?.id
                        val sceneInstanceId =
                            worldHandler.active?.instanceId
                        val sceneData = InputSelectedSceneData(sceneId!!, sceneInstanceId!!)
                        sceneData.inputType = inputSourceSelected
                        var inputResolutionItem  = moduleProvider.getInputSourceMoudle().getResolutionDetailsForUI()
                        sceneData.inputIcon = inputResolutionItem.iconValue
                        sceneData.inputPixelValue = inputResolutionItem.pixelValue
                        sceneData.inputHdrValue = inputResolutionItem.hdrValue

                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE,
                            SceneManager.Action.SHOW_OVERLAY, sceneData
                        )
                    }, delay.toLong(), Dispatchers.Main)
                    isExternalInput = false
                }

                var displayId = display?.displayId
                if(displayId != null) {
                    moduleProvider.getHbbTvModule()
                        .setSurfaceView(ttxSurfaceView!!, displayId, 0)
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG,"Display id not found, defaulting to 0")
                    moduleProvider.getHbbTvModule()
                        .setSurfaceView(ttxSurfaceView!!, 0, 0)
                }
                val outSize = Point()
                window.windowManager.defaultDisplay.getRealSize(outSize)
                livePlaybackView?.let {
                    moduleProvider.getHbbTvModule().setTvView(
                        liveTvView!!,
                        it,
                        outSize.x.toFloat(),
                        outSize.y.toFloat()
                    )
                }
                moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: kotlin.Error) {}
                    override fun onReceive(data: TvChannel) {
                        moduleProvider.getHbbTvModule().initHbbTv(applicationContext, data)
                    }
                })
            }
            else if(event.type == Events.GET_INPUT_RESOLUTION_DATA) {
                var value = 0
                var value1  = 0

                if (event.getData(0) != null && event.getData(1) != null) {
                    value = event.getData(0) as Int
                    value1 = event.getData(1) as Int
                }
                moduleProvider.getInputSourceMoudle().setResolutionDetails(value, value1)

            }
            else if(event!!.type == Events.PLAYBACK_STARTED){
                var applicationMode =
                    if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
                moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }
                    override fun onReceive(data: TvChannel) {
                        //timer for no signal atsc channels
                        if (!data.isFastChannel()) {
                            ReferenceApplication.runOnUiThread {
                                if(inputSelected == "TV") {
                                    stopInactivityTimer()
                                }

                                worldHandler.getVisibles().get(WorldHandler.LayerType.OVERLAY)!!.value.forEach{ sceneManager ->
                                    if (sceneManager.id == ReferenceWorldHandler.SceneId.DIALOG_SCENE) {
                                        if ((sceneManager as DialogSceneManager).sceneData!!.type == DialogSceneData.DialogType.INACTIVITY_TIMER) {
                                            worldHandler.triggerAction(ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                SceneManager.Action.DESTROY)
                                        }
                                    }
                                }

                                TvViewScaler.restore()
                            }
                        }
                        //stop timer for fast channels
                    }
                }, applicationMode)

                if(applicationMode == ApplicationMode.DEFAULT){
                    moduleProvider.getClosedCaptionModule().applyClosedCaptionStyle()
                }
            }
            else if (event.type == Events.PLAYBACK_STOPPED) {
                //Scale up video when playback is stopped
                TvViewScaler.reset()
            }
            else if(event.type == Events.NO_PLAYBACK || event.type == Events.PLAYER_TIMEOUT
                || event.type == Events.NO_CHANNELS_AVAILABLE
                || event.type == Events.WAITING_FOR_CHANNEL) {
                if(ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.WALKTHROUGH ||
                    ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.INTRO) {
                    ReferenceApplication.runOnUiThread {
                        refreshNoSignalPowerOff()
                    }
                }
            }

            else if(event.type == Events.NO_SIGNAL_POWER_OFF_TIME_CHANGED || event.type == Events.NO_SIGNAL_POWER_OFF_ENABLED) {
                if(ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.WALKTHROUGH &&
                    ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.INTRO){
                    ReferenceApplication.runOnUiThread {
                        refreshNoSignalPowerOff()
                    }
                }
            }
            else if(event.type == Events.NO_SIGNAL_POWER_OFF_TIMER_END) {
                runOnUiThread {
                    val currentActiveScene =
                        ReferenceApplication.worldHandler!!.active
                    val sceneData =
                        DialogSceneData(
                            currentActiveScene!!.id,
                            currentActiveScene.instanceId
                        )
                    sceneData.type =
                        DialogSceneData.DialogType.INACTIVITY_TIMER

                    sceneData.title = ConfigStringsManager.getStringById("no_signal_power_off_inactivity_msg")

                    sceneData.message = ""

                    sceneData.positiveButtonText =
                        ConfigStringsManager.getStringById("cancel")

                    sceneData.dialogClickListener =
                        object : DialogSceneData.DialogClickListener {
                            override fun onNegativeButtonClicked() {
                            }
                            override fun onPositiveButtonClicked() {
                                resetTimerOnRcuClick()
                                worldHandler.triggerAction(
                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                    SceneManager.Action.DESTROY)
                            }
                        }

                    worldHandler.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.SHOW_OVERLAY, sceneData
                    )
                }
            }
            else if(event.type == Events.INACTIVITY_TIMER_END) {
                sceneInactivityTimerEnd()
            }
            else if(event.type == Events.SHOW_USB_FULL_DIALOG) {
                runOnUiThread {
                    showToast("Insufficient memory! Recording stopped and saved!", UtilsInterface.ToastDuration.LENGTH_LONG)
                }
            }
            else if (event.type == Events.EXIT_APPLICATION_ON_BACK_PRESS) {
                exitApplication()
            }
            else if(event.type == Events.WATCHLIST_INPUT) {
                setInputToTv()
                com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(Events.VIDEO_RESOLUTION_UNAVAILABLE, arrayListOf(0))
            }
            else if (event.type == Events.HANDLE_INPUT_CEC_TUNE) {
                if (event.getData(0) != null) {
                    var inputItem = event.getData(0) as InputItem
                    var inputPosition = event.getData(1) as Int
                    moduleProvider.getUtilsModule().setPrefsValue(
                        "inputSelected",
                        inputPosition
                    )
                    if(!moduleProvider.getInputSourceMoudle().isBlock(inputItem.inputMainName)) {
                        worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    }
                    inputSelected = inputItem.inputMainName
                    inputSourceSelected = inputItem.inputSourceName
                    moduleProvider.getInputSourceMoudle().setInputActiveName(inputSourceSelected)
                    if (!moduleProvider.getInputSourceMoudle().isBlock(inputItem.inputMainName)) {
                        moduleProvider.getInputSourceMoudle().handleInputSource(inputSelected)
                        isExternalInput = true
                    } else {
                        moduleProvider.getUtilsModule().setPrefsValue(
                            "inputSelectedString",
                            inputSelected
                        )
                    }
                }
            }
            else if(event.type == Events.SHOW_ANOKI_MODE_TOAST) {
                showAnokiModeToast()
            }
            else if (event.type == Events.SHOW_STOP_TIME_SHIFT_DIALOG) {
                showTimeShiftExitDialog()
            }
            else if(event.type == Events.SHOW_NEW_CHANNELS_FOUND_POPUP){
                showNewChannelsFoundPopup()
            } else if (event?.type == Events.ANOKI_REGION_NOT_SUPPORTED) {
                ReferenceApplication.isRegionSupported = false
            }
            else if(event.type == Events.SHOW_CHANNEL_CHANGE_TOAST_FOR_T56){
                //showToast("Channel zapp for t56")
                var channelId: Long = -1
                if (event.getData(0) != null && event.getData(0) is Long) {
                    channelId = event.getData(0) as Long

                    val i = Intent(Intent.ACTION_VIEW)
                    val component = ComponentName("com.mediatek.wwtv.tvcenter", "com.mediatek.wwtv.tvcenter.nav.TurnkeyUiMainActivity")
                    i.component = component
                    i.data = Uri.parse("content://android.media.tv/channel/$channelId")
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(i)
                }
            } else if(event.type == Events.SHOW_DEVICE_INFO) {
                showDeviceInfo()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startRecording(tvEvent: TvEvent) {

        if(moduleProvider.getPlayerModule().isOnLockScreen){
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            return
        }

        if (moduleProvider.getUtilsModule().getUsbDevices().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
            return
        }
        if (moduleProvider.getUtilsModule().getPvrStoragePath().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
            showDeviceInfo()
            return
        }
        if (!moduleProvider.getUtilsModule().isUsbWritableReadable()) {
            showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
            return
        }
        if (!moduleProvider.getUtilsModule().isUsbFreeSpaceAvailable()) {
            showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
            return
        }

        moduleProvider.getPvrModule().startRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {}

            override fun onSuccess() {}
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initCiPlusCommunication() {
        println("%%%%%%% INIT CI PLUS COMMUNICATION")
        var mmiOpenedInPlayer = false
        moduleProvider.getCiPlusModule()!!.registerListener(object: CiPlusInterface.RefCiHandlerListener{
            override fun onEnquiryReceived(enquiry: CiPlusInterface.Enquiry) {
                println("CI onEnquiryReceived 1")
                var activeScene = ReferenceApplication.worldHandler!!.active
                var sceneData = SceneData(activeScene!!.id, activeScene!!.instanceId, enquiry)
                runOnUiThread {
                    mmiOpenedInPlayer = worldHandler.active?.id == ReferenceWorldHandler.SceneId.PLAYER_SCENE
                    if(mmiOpenedInPlayer) {
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.CI_ENCRYPTED_SCENE,
                            SceneManager.Action.SHOW_GLOBAL, sceneData
                        )

                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.PLAYER_SCENE,
                            SceneManager.Action.HIDE
                        )
                    } else {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.CI_ENCRYPTED_SCENE,
                            SceneManager.Action.SHOW_OVERLAY, sceneData
                        )
                    }
                }
            }

            override fun onMenuReceived(menu: CiPlusInterface.CamMenu) {
                inCiMenu =  true
                if(startScan || inBackground){
                    return
                }
                //if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.MMI_MENU_SCENE) {
                println("CI T Start")
                var ciMenu: CiMenu = CiMenu

                var tempList = mutableListOf<PreferenceCategoryItem>()
                menu.menuItems.forEachIndexed { index: Int, it: String ->
                    println("CI T index $index it $it")
                    tempList.add(
                        PreferenceCategoryItem(index.toString(), it, null, false, false, false)
                    )
                }
                ciMenu.tempList = tempList
                ciMenu.menu = menu

                var activeScene = worldHandler.active
                var sceneData = SceneData(activeScene!!.id, activeScene!!.instanceId, ciMenu)

                runOnUiThread {
                    mmiOpenedInPlayer = worldHandler.active?.id == ReferenceWorldHandler.SceneId.PLAYER_SCENE
                    if(mmiOpenedInPlayer) {
                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.MMI_MENU_SCENE,
                            SceneManager.Action.SHOW_GLOBAL, sceneData
                        )

                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.PLAYER_SCENE,
                            SceneManager.Action.HIDE
                        )
                    } else {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )

                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.MMI_MENU_SCENE,
                            SceneManager.Action.SHOW_OVERLAY, sceneData
                        )
                    }
                }
            }

            override fun showCiPopup() {
                println("CI showCiPopup")
                if(worldHandler.active?.id != ReferenceWorldHandler.SceneId.MMI_MENU_SCENE){
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(
                        ReferenceWorldHandler.SceneId.LIVE
                    )
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.MMI_MENU_SCENE,
                        SceneManager.Action.SHOW_OVERLAY
                    )
                }
            }

            override fun closePopup() {
                inCiMenu =  false
                println("CI closePopup ${worldHandler!!.active!!.id}")
                if ((worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.MMI_MENU_SCENE) ||
                    (worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.CI_ENCRYPTED_SCENE)) {
                    runOnUiThread {
                        if (mmiOpenedInPlayer) {
                            ReferenceApplication.worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.MMI_MENU_SCENE,
                                SceneManager.Action.DESTROY
                            )
                            ReferenceApplication.worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.CI_ENCRYPTED_SCENE,
                                SceneManager.Action.DESTROY
                            )

                            ReferenceApplication.worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.PLAYER_SCENE,
                                SceneManager.Action.SHOW_OVERLAY
                            )
                        } else {
                                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                                    ReferenceWorldHandler.SceneId.LIVE
                                )
                        }

                        mmiOpenedInPlayer = false
                    }
                }
            }

            override fun onPinResult(result : CiPlusInterface.CachedPinResult) {
                when(result){
                    CiPlusInterface.CachedPinResult.CACHED_PIN_FAIL -> {
                        runOnUiThread {
                            showToast("Failed to enter pin")
                        }
                    }
                    CiPlusInterface.CachedPinResult.CACHED_PIN_RETRY -> {
                        runOnUiThread {showToast("Retry to enter pin")
                        }
                    }
                    CiPlusInterface.CachedPinResult.CACHED_PIN_OK -> {
                        runOnUiThread {
                            showToast("Success in entering pin")
                        }
                    }
                    else -> {}
                }
            }

            override fun onStartScanReceived(
                profileName: String,
            ) {
                Log.d(Constants.LogTag.CLTV_TAG + "onStartScanReceived", "onStartScanReceived")
                startScan = true
                inCiMenu =  true
                var currentActiveScene = ReferenceApplication.worldHandler!!.active
                var sceneData = DialogSceneData(currentActiveScene!!.id, currentActiveScene.instanceId)
                sceneData.type = DialogSceneData.DialogType.YES_NO
                sceneData.title = ConfigStringsManager.getStringById("install_dtv_operator")+" $profileName"+" ?"
                sceneData.positiveButtonText = ConfigStringsManager.getStringById("yes")
                sceneData.negativeButtonText = ConfigStringsManager.getStringById("no")

                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                    override fun onPositiveButtonClicked() {
                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            SceneManager.Action.DESTROY
                        )
                        Log.d(Constants.LogTag.CLTV_TAG + "onStartScanReceived", "onPositiveButtonClicked")
                        startScan = false
                        moduleProvider.getCiPlusModule()!!.startCAMScan(
                            isTriggeredByUser = false,
                            isCanceled = false
                        )
                    }

                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun onNegativeButtonClicked() {
                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            SceneManager.Action.DESTROY
                        )
                        Log.d(Constants.LogTag.CLTV_TAG + "onStartScanReceived", "onNegativeButtonClicked")
                        startScan = false
                        inCiMenu =  false
                        moduleProvider.getCiPlusModule()!!.startCAMScan(
                            isTriggeredByUser = false,
                            isCanceled = true
                        )
                        worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    }
                }
                ReferenceApplication.runOnUiThread {
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    if (ReferenceApplication.worldHandler?.active?.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                        TvViewScaler.scaleUpTvView()
                    }
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.SHOW_OVERLAY, sceneData
                    )
                }
            }

            override fun onCICardEventInserted() {
                ReferenceApplication.runOnUiThread {
                    Toast.makeText(applicationContext, ConfigStringsManager.getStringById("ci_card_inserted"), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCICardEventRemoved() {
                ReferenceApplication.runOnUiThread {
                    Toast.makeText(applicationContext, ConfigStringsManager.getStringById("ci_card_removed"), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setInputToTv() {
            inputSelected = "TV"
            inputSourceSelected = "TV"
            moduleProvider.getUtilsModule().setPrefsValue("inputSelectedString", inputSelected)
            moduleProvider.getInputSourceMoudle().handleInputSource(inputSelected)
            moduleProvider.getInputSourceMoudle().setInputActiveName(inputSourceSelected)
            moduleProvider.getUtilsModule().setPrefsValue(
                "inputSelected",
                1
            )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun changeChannelFromLiveTab(delay: Long = 0){
        try {
            CoroutineHelper.runCoroutineWithDelay({
                val tvChannel: TvChannel? = moduleProvider.getTvModule().getChannelById(channelIdFromLiveTab!!)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "changeChannelFromLiveTab: ${tvChannel}")
                if (tvChannel != null && !moduleProvider.getUtilsModule().isThirdPartyChannel(tvChannel)) {
                    moduleProvider.getUtilsModule().setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal)
                    moduleProvider.getTvModule().storeActiveChannel(tvChannel!!)
                    startChannelFromLiveTab = true
//                    waitingStartChannelFromLiveTab = true
                    moduleProvider.getTvModule()
                        .changeChannel(tvChannel!!, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                println(error.message)
                            }

                            override fun onSuccess() {
//                                waitingStartChannelFromLiveTab = false
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "channel changed from live tab to channel ${tvChannel.name}")
                            }
                        })
                } else {
//                    waitingStartChannelFromLiveTab = false
                    startChannelFromLiveTab = false
                }
            },delay)
        } catch (E: Exception) {
            println("Exception ${channelIdFromLiveTab} || ${E.printStackTrace()}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startWatchlistProgram(scheduledChannel: TvChannel){
        //Hide overlay
        InformationBus.submitEvent(
            Event(
                Events.PLAYBACK_STATUS_MESSAGE_NONE,
                LiveManager.LiveSceneStatus(LiveManager.LiveSceneStatus.Type.NONE)
            )
        )
        moduleProvider.getTvModule().changeChannel(
            scheduledChannel,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {
                }

                override fun onSuccess() {
                    //Set application mode 0 for DEFAULT mode
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    moduleProvider.getUtilsModule().setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal)
                    //Show only black background
                    InformationBus.submitEvent(
                        Event(
                            Events.PLAYBACK_STATUS_MESSAGE_NONE,
                            LiveManager.LiveSceneStatus(LiveManager.LiveSceneStatus.Type.IS_HIDE_OVERLAY)
                        )
                    )
                }

            })
    }

    override var worldHandler: WorldHandler = ReferenceWorldHandler(this)

    override fun handleKeys(keyCode: Int, keyEvent: KeyEvent?): Boolean {
        return worldHandler!!.dispatchKeyEvent(keyCode, keyEvent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        resetTimerOnRcuClick()

        if (worldHandler.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
            if (moduleProvider.getHbbTvModule().sendKeyToHbbTvEngine(
                    keyCode,
                    event,
                    HbbTvInterface.HbbTvKeyEventType.KEY_UP
                )
            ) {
                return true
            }
        }

        if(moduleProvider.getInputSourceMoudle().isCECControlSinkActive()) {
            moduleProvider.getInputSourceMoudle().dispatchCECKeyEvent(event)
        }


        if (!worldHandler.isEnableUserInteraction) {
            return true
        }

        if (worldHandler.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
            if (moduleProvider.getInteractiveAppModule().sendKeyToInteractiveApp(
                    keyCode = keyCode,
                    buttonDown = false
                )) {
                return true
            }
        }

        if (!ReferenceApplication.isInForeground) {
            return true
        }

        if (overlayFeatureView != null) {
            var isHandled =
                overlayFeatureView!!.dispatchKey(
                    keyCode,
                    event
                )
            if (isHandled) {
                return true
            }
        }

//        if (WalkthroughtHelper.isActive) {
//            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
//                val done = WalkthroughtHelper.onKeyDown(keyCode, event)
//                return done
//            }
//        }

        try {
            if (moduleProvider.getTTXModule().isTTXActive()) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
                    KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
                    KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
                    KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1,
                    KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4,
                    KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7,
                    KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9,
                    KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_PROG_GREEN,
                    KeyEvent.KEYCODE_PROG_YELLOW, KeyEvent.KEYCODE_PROG_BLUE,
                    KeyEvent.KEYCODE_WINDOW, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_UNKNOWN, KeyEvent.KEYCODE_CAPTIONS,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND->
                    {
                        var ttxHandled = moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event)
                        if (ttxHandled) {
                            return true
                        }
                    }
                }
            }
            if (!downActionBackKeyDone && KeyEvent.KEYCODE_BACK == keyCode){
                return true
            }
            var dispatchResult = super.onKeyUp(keyCode, event)
            if(downActionBackKeyDone && KeyEvent.KEYCODE_BACK == keyCode) downActionBackKeyDone = false
            return dispatchResult
        } catch (E: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKeyUp: Exception ${E.printStackTrace()}")
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        resetTimerOnRcuClick()

        if(moduleProvider.getInputSourceMoudle().isCECControlSinkActive()) {
            moduleProvider.getInputSourceMoudle().dispatchCECKeyEvent(event)
        }

        if (moduleProvider.getInteractiveAppModule().sendKeyToInteractiveApp(keyCode,true)) {
            return true
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKeyDown keyCode: " + keyCode)
        if (ReferenceApplication.parentalControlDeepLink && keyCode == KeyEvent.KEYCODE_BACK) {
            exitApplication()
            return true
        }

        if (!worldHandler.isEnableUserInteraction) {
            return true
        }

        if (worldHandler.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
            if (moduleProvider.getHbbTvModule().sendKeyToHbbTvEngine(
                    keyCode,
                    event,
                    HbbTvInterface.HbbTvKeyEventType.KEY_DOWN
                )
            ) {
                return true
            }
        }


        //Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKeyDown: Key not handled by HbbTvApp, handle here");

        if (overlayFeatureView != null) {
            var isHandled =
                overlayFeatureView!!.dispatchKey(
                    keyCode,
                    event
                )
            if (isHandled) {
                return true
            }
        }

        //Check if intro screen is active
        if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.INTRO) {
            return true
        }

//        if (WalkthroughtHelper.isActive) {
//            val done = WalkthroughtHelper.onKeyDown(keyCode, event)
//            return !done
//        }

        //Teletext

        if (keyCode == KeyEvent.KEYCODE_TV_TELETEXT) {
            if(moduleProvider.getTTXModule().isTTXAvailable()){
                startTtx()
            }else{
                Toast.makeText(
                    applicationContext,
                    ConfigStringsManager.getStringById("teletext_feature_not_available_for_channel"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (keyCode == KeyEvent.KEYCODE_T) {
            PinHelper.setPinResultCallback(object : PinHelper.PinCallback {
                override fun pinCorrect() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "PIN correct")
                }

                override fun pinIncorrect() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "PIN incorrect")
                }
            })
            Handler().postDelayed({
                PinHelper.startPinActivity("LiveTv", "PIN dialog")
            }, 1000)

            return true
        }

        if (worldHandler.active?.id == ReferenceWorldHandler.SceneId.LIVE) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, KeyEvent.KEYCODE_DPAD_UP, event) == true) {
                        return true
                    }
                }

                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, KeyEvent.KEYCODE_DPAD_DOWN, event) == true) {
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1,
                KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6,
                KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1,
                KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5,
                KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {

                    if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event) == true) {
                        println("TTX keyCode $keyCode | event ${event.keyCode}")
                        return true
                    }
                }

                KeyEvent.KEYCODE_MEDIA_PLAY,KeyEvent.KEYCODE_MEDIA_PAUSE,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event) == true){
                        return true
                    }
                }


                KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_PROG_GREEN, KeyEvent.KEYCODE_PROG_YELLOW, KeyEvent.KEYCODE_PROG_BLUE -> {
                    return moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event)
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, event) == true) {
                return true
            }
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, KeyEvent.KEYCODE_MEDIA_REWIND, event) == true) {
                return true
            }
        }

        if (keyCode == KeyEvent.KEYCODE_CAPTIONS) {
            if ((worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.WALKTHROUGH )) {
                return true
            }
            if(worldHandler.active!!.id == ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE || worldHandler.active!!.id == ReferenceWorldHandler.SceneId.PLAYER_SCENE){
                val sceneData = SceneData(worldHandler.active!!.id, worldHandler.active!!.instanceId)
                ReferenceApplication.runOnUiThread {
                    worldHandler.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.AUDIO_SUBTITLE_SCENE,
                        SceneManager.Action.SHOW, sceneData
                    )
                }
                return true
            }

            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, KeyEvent.KEYCODE_CAPTIONS, event) == true) {
                return true
            }

            moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(activeTvChannel: TvChannel) {

                    // If the current TV channel is a fast channel, do not handle the KeyEvent for Fast CAPTIONS at all.
                    if (activeTvChannel.isFastChannel())  return

                    if (activeTvChannel.isRadioChannel) {
                        showToast(ConfigStringsManager.getStringById("no_available_subtitle_tracks_msg"))
                    } else {
                        if (moduleProvider.getClosedCaptionModule().getSubtitlesState()) {
                            onSubtitlePressed()
                        } else {
                            if (moduleProvider.getClosedCaptionModule().isClosedCaptionEnabled()) {
                                var isNotTvInput = false
                                if (!(inputSelected == "TV")) {
                                    isNotTvInput = true
                                }
                                moduleProvider.getClosedCaptionModule()
                                    .setClosedCaption(isNotTvInput)
                                val curCaptionInfo =
                                    moduleProvider.getClosedCaptionModule()
                                        .getClosedCaption(isNotTvInput)

                                val toastMessage = if (curCaptionInfo.isNullOrBlank())
                                    ConfigStringsManager.getStringById("cc_track_turned_off")
                                else ConfigStringsManager.getStringById("cc_changed").plus(" ")
                                    .plus(curCaptionInfo)

                                showToast(toastMessage)
                            } else {
                                moduleProvider.getClosedCaptionModule()
                                    .saveUserSelectedCCOptions("display_cc", 1)
                                var isNotTvInput = false
                                if (!(inputSelected == "TV")) {
                                    isNotTvInput = true
                                }
                                moduleProvider.getClosedCaptionModule()
                                    .setClosedCaption(isNotTvInput)
                                val curCaptionInfo =
                                    moduleProvider.getClosedCaptionModule()
                                        .getClosedCaption(isNotTvInput)
                                val toastMessage =
                                    if (curCaptionInfo.isNullOrBlank())
                                        ConfigStringsManager.getStringById("cc_track_turned_off")
                                    else ConfigStringsManager.getStringById("cc_changed").plus(" ")
                                        .plus(curCaptionInfo)
                                showToast(toastMessage)
                            }
                        }
                    }
                }

            }, applicationMode = if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT)
            return false
        }

        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            if ((worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.WALKTHROUGH)) {
                return true
            }

            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event) == true) {
                return true
            }

            moduleProvider.getTvModule().getActiveChannel(
                object : IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {}

                    override fun onReceive(activeTvChannel: TvChannel) {
                        // TODO - Not sure whether favorite btn click needs to handle for fast channels or not. Temporarily not handling.
                        if (activeTvChannel.isFastChannel()) return

                        onFavoriteKeyPressed()
                    }
                },
                applicationMode = if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
            )
            return false
        }

        if (keyCode == KeyEvent.KEYCODE_GUIDE) {
            //disabling guide button on only fast case.
            when(BuildConfig.FLAVOR) {
                "base" -> return true
                else -> {
                    //need to hide fast zap banner if it is visible.
                    val intent = Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                    ReferenceApplication.applicationContext().sendBroadcast(intent)
                    if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.WALKTHROUGH) {
                        return true
                    }
                    onGuideKeyPressed()
                    return true
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                return true
            }
            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event) == true) {
                return true
            }
            if (HomeSceneManager.IS_VOD_ACTIVE) {
                return true
            }
            onMenuKeyPressed()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_INFO) {
            if (moduleProvider.getPlayerModule().isOnLockScreen){
                return true
            }
            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event) == true){
                return true
            }
            if (moduleProvider.getTimeshiftModule().isTimeShiftActive) {
                return worldHandler!!.dispatchKeyEvent(keyCode, event)
            }
            if (worldHandler!!.dispatchKeyEvent(keyCode, event)) {
                return true
            }
            onInfoKeyPressed()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_UNKNOWN){
            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event) == true){
                return true
            }
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_PROG_BLUE) {
            if (moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event) == true) {
            } else if (moduleProvider.getTimeshiftModule().isTimeShiftActive) {
                if (worldHandler?.active?.id == ReferenceWorldHandler.SceneId.PLAYER_SCENE) {
                    InformationBus.submitEvent(Event(com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_PLAYER_AUDIO_LIST))
                }
            }
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_PROG_YELLOW) {
            moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event)
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_WINDOW) {
            Log.i("iTeletext", "dispatchKeyEvent: Got key 171")
            moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, event)

            return true
        }

        when (keyCode) {
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                val digit = keyCode - KeyEvent.KEYCODE_0
                if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.HOME_SCENE) {
                    if (inputSelected == "TV") {
                        if (moduleProvider.getPvrModule().isRecordingInProgress())
                            InformationBus.submitEvent(Event(Events.SHOW_STOP_RECORDING_DIALOG, true))
                        else
                            InformationBus.submitEvent(Event(Events.RCU_DIGIT_PRESSED, digit))                    }
                }
            }
            KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {
                val digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.HOME_SCENE) {
                    if (inputSelected == "TV") {
                        if (moduleProvider.getPvrModule().isRecordingInProgress())
                            InformationBus.submitEvent(Event(Events.SHOW_STOP_RECORDING_DIALOG, true))
                        else
                            InformationBus.submitEvent(Event(Events.RCU_DIGIT_PRESSED, digit))
                    }
                }
            }
            KeyEvent.KEYCODE_PERIOD,KeyEvent.KEYCODE_SLASH,KeyEvent.KEYCODE_MINUS -> {
                InformationBus.submitEvent(Event(Events.RCU_PERIOD_PRESSED))
            }
            KeyEvent.KEYCODE_PROG_RED -> {
            }
            KeyEvent.KEYCODE_PROG_GREEN -> {
            }
        }

        if (worldHandler.isVisible(ReferenceWorldHandler.SceneId.RCU_SCENE)) {
            // Send key to the rcu scene if it is visible
            worldHandler.getVisibles()
                .get(WorldHandler.LayerType.OVERLAY)!!.value.forEach { sceneManager ->
                    if (sceneManager.id == ReferenceWorldHandler.SceneId.RCU_SCENE) {
                        return sceneManager.scene!!.dispatchKeyEvent(keyCode, event)
                    }
                }
        }
        if (keyCode == KeyEvent.KEYCODE_MUHENKAN || keyCode == KeyEvent.KEYCODE_THUMBS_DOWN) {
            if (worldHandler.active!!.id == ReferenceWorldHandler.SceneId.ZAP_BANNER || worldHandler.active!!.id == ReferenceWorldHandler.SceneId.LIVE) {
                    worldHandler.triggerAction(
                        ReferenceWorldHandler.SceneId.ZAP_BANNER,
                        SceneManager.Action.DESTROY
                    )
                val nextAudio  = moduleProvider.getPlayerModule().switchAudioTrack()
                showToast(ConfigStringsManager.getStringById("audio_switched_to $nextAudio"))
                return true
            }
            else if(worldHandler.active!!.id == ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE || worldHandler.active!!.id == ReferenceWorldHandler.SceneId.PLAYER_SCENE){
                val nextAudio  = moduleProvider.getPlayerModule().switchAudioTrack()
                showToast(ConfigStringsManager.getStringById("audio_switched_to $nextAudio"))
                return true
            }
        }

        var handled: Boolean = false
        try {
            handled = super.onKeyDown(keyCode, event)
        } catch (E: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKeyDown: Exception ${E.printStackTrace()}")
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            downActionBackKeyDone = true
            moduleProvider.getTextToSpeechModule().stopSpeech()
            //   event.startTracking();
            //Check if Evaluation screen is active
            if (Utils.isEvaluationFlavour()) {
                if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.EVALUATION_SCENE) {
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.EVALUATION_SCENE,
                        SceneManager.Action.DESTROY
                    )
                    val isFirstRun =
                        moduleProvider.getUtilsModule().getPrefsValue("isFirstRun", false) as Boolean
                    if (isFirstRun) {
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                            SceneManager.Action.SHOW
                        )
                    }
                }
            }
        }

        return handled
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onFavoriteKeyPressed() {
        runOnUiThread {
            if (worldHandler.active!!.id != ReferenceWorldHandler.SceneId.FAVOURITE_SCENE &&
                worldHandler.active!!.id == ReferenceWorldHandler.SceneId.LIVE
            ) {
                val id = worldHandler.active!!.id
                val instanceId = worldHandler.active!!.instanceId
                val sceneData = SceneData(id, instanceId)
                worldHandler.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.FAVOURITE_SCENE,
                    SceneManager.Action.SHOW_OVERLAY, sceneData
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun handleKeyInTtx(keyCode: Int){
        moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        moduleProvider.getTTXModule().sendKeyToTtx(liveTvView!!, keyCode, KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startTtx(changeSize: Boolean = false){
        moduleProvider.getTTXModule().startTTX(applicationContext, liveTvView!!, ttxSurfaceView!!, changeSize)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun globalKey(keyCode: Int){
        //todo maybe place this to utilsInterface
        val mInputConnection = BaseInputConnection(findViewById(R.id.livePlaybackView), true)
        val keyUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        mInputConnection.sendKeyEvent(keyUp)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isPvrAndTimeshiftEnabled() = (moduleProvider.getGeneralConfigModule().getGeneralSettingsInfo("pvr")
                && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.PLAYER_SCENE
                && moduleProvider.getTimeshiftModule().isTimeShiftActive)

    @RequiresApi(Build.VERSION_CODES.R)
    fun resetTimerOnRcuClick() {
        stopInactivityTimer()
        refreshNoSignalPowerOff()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onSubtitlePressed() {
        //TODO Temporary solution show InfoBanner in captions state
        runOnUiThread {
            if (worldHandler.active!!.id != ReferenceWorldHandler.SceneId.AUDIO_SUBTITLE_SCENE) {
                if (worldHandler.active!!.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                    val scene = ReferenceApplication.worldHandler!!.active!!
                    val activeCategory =
                        (scene.scene as HomeSceneBase).getActiveCategory()
                    if (activeCategory == 0) {
                        moduleProvider.getPlayerModule().unmute()
                    }
                }
                if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.PVR_PLAYBACK ||
                    ReferenceApplication.worldHandler?.active?.id == ReferenceWorldHandler.SceneId.LIVE ) {
                    // No need to destroy while pvr playback so we can go detail scene when we press back
                } else if (moduleProvider.getTimeshiftModule().isTimeShiftActive ||
                    ReferenceApplication.worldHandler?.active?.id == ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE) {
                    // we can't destroy TS scene, otherwise black screen and no subtitle issue will come.
                    worldHandler.triggerAction(
                        ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE, SceneManager.Action.HIDE
                    )
                } else if (ReferenceApplication.worldHandler?.active?.id == ReferenceWorldHandler.SceneId.ZAP_BANNER) {
                    worldHandler.triggerAction(
                        ReferenceWorldHandler.SceneId.ZAP_BANNER, SceneManager.Action.DESTROY
                    )
                } else {
                    worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                }
                val sceneData =
                    SceneData(worldHandler.active!!.id, worldHandler.active!!.instanceId)
                worldHandler.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.AUDIO_SUBTITLE_SCENE,
                    SceneManager.Action.SHOW_OVERLAY, sceneData
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStop() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onStop")
        startBackgroundExitTimer()
        if(ReferenceApplication.isTosFromInput) {
            InformationBus.submitEvent(Event(Events.APP_STOPPED))
        }
        isFromOnNewIntent = false
        moduleProvider.getInputSourceMoudle().onApplicationStop()
        moduleProvider.getUtilsModule().setApplicationRunningInBackground(false)
        moduleProvider.getHbbTvModule().disableHbbTv()
        moduleProvider.getPlayerModule().refreshTTMLStatus()
        exitTvInput()
        ReferenceApplication.isAppPaused = false
        // Check if the time shift is active
        // Keep time shift active in the background when the app is in paused state

        if (moduleProvider.getTimeshiftModule().isTimeShiftActive) {
            isTimeshiftDestroyed = true
            showToast(ConfigStringsManager.getStringById("time_shift_stopped"), UtilsInterface.ToastDuration.LENGTH_LONG)
            moduleProvider.getTimeshiftModule().timeShiftStop(object : IAsyncCallback{
                override fun onFailed(error: Error) {
                }

                    override fun onSuccess() {
                    }
                })
            stopPlayback()
            ReferenceApplication.worldHandler!!.playbackState =
                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
            ReferenceApplication.worldHandler!!.destroyOtherExisting(
                ReferenceWorldHandler.SceneId.LIVE
            )
            moduleProvider.getTimeshiftModule().setTimeShiftIndication(false)
        } else if (moduleProvider.getPvrModule().isRecordingInProgress()) {
            if (isTvInteractive(this)) {
                showToast(
                    ConfigStringsManager.getStringById("recording_stopped"),
                    UtilsInterface.ToastDuration.LENGTH_LONG
                )
                moduleProvider.getPvrModule().stopRecordingByChannel(
                    moduleProvider.getPvrModule().getRecordingInProgressTvChannel()!!,
                    object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {}
                    })
                ReferenceApplication.worldHandler!!.playbackState =
                    ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                PvrBannerSceneManager.previousProgress = 0L
                moduleProvider.getPvrModule().setRecIndication(false)
                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
                stopPlayback()
            } else moduleProvider.getPlayerModule().mute()
        } else {
            if(!HomeSceneManager.IS_VOD_ACTIVE){
                ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }

            stopPlayback()
        }
        // Saving audio status before app goes in background/power_off button clicked.
        moduleProvider.getUtilsModule().setPrefsValue(
            "PREF_KEY_IS_MUTED", mAudioManager?.isStreamMute(AudioManager.STREAM_MUSIC) as Boolean
        )
        super.onStop()
    }

    /** isTvInteractive will return whether Power ON/OFF button clicked or not.*/
    private fun isTvInteractive(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    private fun exitTvInput() {
        moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                moduleProvider.getInputSourceMoudle().exitTVInput("")
            }

            override fun onReceive(data: TvChannel) {
                moduleProvider.getInputSourceMoudle().exitTVInput(data.inputId)

            }

        })
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {

        moduleProvider.getCiPlusModule().platformSpecificOperations(CiPlusInterface.PlatformSpecificOperation.PSO_RESET_MMI_INTERFACE, true)

        stopBackgroundExitTimer()
        if (BuildConfig.FLAVOR != "rtk") {
            settingsIconClicked = false
        }
        try {
            super.onStart()
            moduleProvider.getInputSourceMoudle().onApplicationStart()
            moduleProvider.getUtilsModule().setApplicationRunningInBackground(true)
            moduleProvider.getHbbTvModule().onActivityStart()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        resumePlayback()

        val i = Intent("android.intent.action.inputreceived")
        i.component = ComponentName("com.android.tv.settings", "com.android.tv.settings.vendor.GlobalKeyReceiver")
        applicationContext.sendBroadcast(i)

    }

    private fun isAppOnForeground(context: Context, appPackageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        Handler().postDelayed({
            var task = activityManager.getRunningTasks(5)
            task.forEach {
                var packageName = it.topActivity!!.packageName
                var className = it.topActivity!!.className
                //DashboardActivity is started on settings rc key press on some devices
                if (className == "com.google.android.apps.tv.launcherx.dashboard.DashboardActivity") {
                    intent.putExtra("dashboard_activity", true)
                }
            }
        }, 1000)

        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == appPackageName) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onPause() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPause")
        downActionBackKeyDone = false
        inBackground = true
        if (isAppOnForeground(applicationContext, "com.android.tv.settings")) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPause settings opened")
            if (!ReferenceApplication.isInputOpen) {
                ReferenceApplication.runOnUiThread {
                    if (worldHandler.active != null && !settingsIconClicked) {
                        //isPaused should be true when we are pressing guide button while,
                        // we are in live tab,so that it can reset its view when we enter again in fast.
                        if (FastLiveTabDataProvider.fastLiveTab != null) {
                            FastLiveTabDataProvider.fastLiveTab!!.isPaused = true
                        }
                        ReferenceApplication.worldHandler?.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        ReferenceApplication.isHiddenFromSetings = true
                        moduleProvider.getPlayerModule().unmute()
                    } else {
                        ReferenceApplication.isHiddenFromSetings = false
                    }
                }
            } else {
                ReferenceApplication.isHiddenFromSetings = false
            }
        } else {
            ReferenceApplication.isHiddenFromSetings = false
        }
        // Check if the app is paused during the intro
        // Case when the settings button is pressed in intro scene
        isIntroPaused = worldHandler.isVisible(ReferenceWorldHandler.SceneId.INTRO)
        if (isIntroPaused) {
            super.onPause()
            return
        }

        //Check if zap banner is active
        if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.ZAP_BANNER) {
            worldHandler.triggerAction(
                ReferenceWorldHandler.SceneId.ZAP_BANNER,
                SceneManager.Action.DESTROY
            )
        }
        //Check if PVR banner is active
        if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE) {
            worldHandler.triggerAction(
                ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                SceneManager.Action.HIDE
            )
        }

        if (worldHandler.active != null && worldHandler.active!!.id == ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE) {
            worldHandler.triggerAction(
                ReferenceWorldHandler.SceneId.INPUT_SELECTED_SCENE,
                SceneManager.Action.DESTROY
            )
        }

        if (!ReferenceApplication.noChannelScene
            && !(worldHandler as ReferenceWorldHandler).isVodScene() // user is in any Vod related screen - when no internet dialog comes up and user presses Settings button dialog MUST be present in the background.
            && !HomeSceneManager.IS_VOD_ACTIVE // user is in any Vod related screen - when no internet dialog comes up and user presses Settings button dialog MUST be present in the background.
        ) {
            worldHandler.let {
                it.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    SceneManager.Action.DESTROY
                )
            }
        }

        //Check if digit zap is active
        if (worldHandler.active != null) {
            when (worldHandler.active!!.id) {
                ReferenceWorldHandler.SceneId.DIGIT_ZAP -> {
                    worldHandler.triggerAction(
                        ReferenceWorldHandler.SceneId.DIGIT_ZAP,
                        SceneManager.Action.DESTROY
                    )
                }
                ReferenceWorldHandler.SceneId.DIGIT_ZAP_CONFLICT -> {
                    worldHandler.triggerAction(
                        ReferenceWorldHandler.SceneId.DIGIT_ZAP_CONFLICT,
                        SceneManager.Action.DESTROY
                    )
                }
            }
        }

        moduleProvider.getTTXModule().stopTTX()

        //when app is in background, stop inactivity timer
        if (!ReferenceApplication.isInForeground || !ReferenceApplication.isOkClickedOnSetUp){
            stopInactivityTimer()
        }

        if(settingsIconClicked && !((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()) {
            //Hide everything when entering settings from icon so that picture, screen and sound options
            //see video running while active
            ReferenceApplication.runOnUiThread {
                //ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                ReferenceApplication.worldHandler?.triggerAction(ReferenceWorldHandler.SceneId.HOME_SCENE, SceneManager.Action.HIDE)
            }
        }
        super.onPause()

        ReferenceApplication.isInForeground = false
        ReferenceApplication.isAppPaused = true

        if (channelRowTimer != null) {
            channelRowTimer!!.cancel()
            channelRowTimer = null
        }
        //when picture/screen/sound is opened from setup donot cancle timer
        if (!ReferenceApplication.isOkClickedOnSetUp) {
            stopInactivityTimer()
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun tuneToInputViaAssistant(intent: Intent?) {
        var intentData = intent?.getData().toString()
        if (intentData.contains("HDMI")) {
            if(intentData.length > 6) {
                val s = intentData.substring(intentData.length - 6)
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " [tuneToInputViaAssistant] $s")
                moduleProvider.getInputSourceMoudle().handleCecData(s)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        moduleProvider.getUtilsModule().setApplicationRunningInBackground(true)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onNewIntent: $intent ${intent?.action} ${intent?.data} ${intent?.extras}")
        tuneToInputViaAssistant(intent)
        if (intent?.action.equals("keycode_keyinput")) {
            ReferenceApplication.isInputPaused = false
            ReferenceApplication.isPrefClicked = false
            isFromOnNewIntent = true
            var previousInput = inputSelected
            var previousTuneURL = inputTuneURL
            inputSelected = intent?.extras?.getString("input_main_name").toString()
            inputSourceSelected = intent?.extras?.getString("input_source_name").toString()
            inputTuneURL = intent?.extras?.getString("input_tune_url").toString()
            moduleProvider.getInputSourceMoudle().setInputActiveName(inputSourceSelected)
            var isBlocked = intent?.extras?.getBoolean("input_blocked")
            if (ReferenceApplication.getActivity().localClassName == "InputSourceActivity") {
                ReferenceApplication.setActivity(this)
            }
            if (worldHandler.active == null) {
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.LIVE,
                    SceneManager.Action.SHOW
                )
            }
            if (inputSelected.contains("Home")) {
                moduleProvider.getInputSourceMoudle().handleInputSource(inputSelected)
                InformationBus.submitEvent(
                    Event(Events.LAUNCH_INPUT_HOME)
                )
                return
            }
            if (isBlocked == true
            ) {
                InformationBus.submitEvent(
                    Event(Events.BLOCK_TV_VIEW)
                )
                return
            }
            worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            moduleProvider.getInputSourceMoudle().handleInputSource(inputSelected, inputTuneURL)
            if(inputSelected == "TV") {
                com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(Events.VIDEO_RESOLUTION_UNAVAILABLE, arrayListOf(0))
                if (previousInput != inputSelected) {
                    if(ReferenceApplication.isTosFromInput) {
                        startSdkInit()
                        ReferenceApplication.isTosFromInput = false
                    }
                    moduleProvider.getFastUserSettingsModule().checkTos(object : IAsyncDataCallback<Boolean>{
                        override fun onFailed(error: Error) {}

                        override fun onReceive(data: Boolean) {
                            if(data){
                                if (channelsLoaded && moduleProvider.getTvModule()
                                        .getChannelList(ApplicationMode.FAST_ONLY).size == 0
                                ) {
                                    val intent = Intent(ReferenceApplication.FAST_SCAN_START)
                                    applicationContext.sendBroadcast(intent)
                                }
                            }
                        }
                    })
                    tuneToTv()
                } else {
                    moduleProvider.getTvModule()
                        .getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                            override fun onFailed(error: kotlin.Error) {
                            }

                            override fun onReceive(data: TvChannel) {
                                InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, data))
                            }

                        })
                }
                livePlaybackView?.visibility = View.VISIBLE
            }
            if (previousInput != inputSelected) {
                if (!isBlocked!!) {
                    isExternalInput = true
                }
            }
        }
        setIntent(intent)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun tuneToTv() {
        if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE) {
            var applicationMode =
                if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
            moduleProvider.getTvModule().startInitialPlayback(object : IAsyncCallback {
                override fun onSuccess() {}

                override fun onFailed(error: Error) {}
            }, applicationMode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getLauncherOrNotificationChannel() {
        if (intent.hasExtra("Channel_ID")) {
            var channelId = intent.getIntExtra("Channel_ID", 0)
            try {
                var notifOrLauncherChannel = moduleProvider.getTvModule().getChannelById(channelId)
                if (!ReferenceApplication.isInitalized) {
                    applicationContext.getSharedPreferences("OnlinePrefsHandler", Context.MODE_PRIVATE).edit()
                        .putInt("PRELOADED_CHANNEL_ID", notifOrLauncherChannel!!.index).apply()
                } else {
                    moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onReceive(activeChannel: TvChannel) {
                            if (activeChannel == null || activeChannel!!.channelId != channelId.toLong()) {
                                //Playchannel method can be used when active channel_id doesn't match with channel_id in intent
                                moduleProvider.getTvModule().changeChannel(notifOrLauncherChannel!!, object : IAsyncCallback {
                                    override fun onSuccess() {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: playChannel resume")
                                    }

                                    override fun onFailed(error: Error) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: playChannel resume")
                                    }
                                })
                            } else if (activeChannel!!.channelId == channelId.toLong()) {
                                //play method can be used when active channel_id match with channel_id in intent
                                moduleProvider.getTvModule().changeChannel(
                                    notifOrLauncherChannel!!,
                                    object : IAsyncCallback {
                                        override fun onFailed(error: Error) {
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: play resume")
                                        }

                                        override fun onSuccess() {
                                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: play resume")
                                        }
                                    })
                            }
                        }
                    })
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getAssistantChannel() {
        var intentData = intent.getData().toString()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAssistantChannel: INTENT DATA ENTERED $intentData")
        if (intentData.contains(CHANNEL_INTENT_DATA)) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAssistantChannel: CLICK ON CHANNEL INTENT DATA SADRZI NAS INTENT ")
            // Uri "content://android.media.tv/channel/<channelId>" points to tuning
            // to channel by channel id. This is triggered by Google Assistent on the
            // one of the voice commands 'Turn to channel <id>' or 'Channel <id>'.
            // Google Assistant On-Device logic when googleAssistanOnDevice flag is set to true.
            // This will cover tuning to channel by channel number.
            var tvChannel: TvChannel? = null
            var temp = intentData.split("/", "=")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAssistantChannel: CLICK ON CHANNEL TEMP $temp")
            var channelQuery = temp[temp.size - 1]
            if (lastChangeInputTime == null || (System.currentTimeMillis() - lastChangeInputTime!! > CONST_CHANGE_INPUT_TYPE_FOCUS_DELAY)) {
                tvChannel =
                    moduleProvider.getSearchModule().getSimilarChannel(channelQuery)
            }
            if (tvChannel != null) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAssistantChannel: tvChannel ${tvChannel!!.name}")
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAssistantChannel: CLICK ON CHANNEL NASAO CHANNEL $temp")

                if (!ReferenceApplication.isInitalized) {
                    applicationContext.getSharedPreferences("OnlinePrefsHandler", Context.MODE_PRIVATE).edit()
                        .putInt("PRELOADED_CHANNEL_ID", tvChannel!!.index).apply()
                } else {
                    moduleProvider.getTvModule().getActiveChannel(object :
                        IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onReceive(activeChannel: TvChannel) {
                            if (activeChannel.channelId == tvChannel!!.channelId) {
                                resumePlayback()
                            } else {
                                moduleProvider.getTvModule().changeChannel(tvChannel!!, object : IAsyncCallback {
                                    override fun onFailed(error: Error) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: CLICK ON CHANNEL PLAY CHANNEL FAILED $temp")
                                    }

                                    override fun onSuccess() {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: CLICK ON CHANNEL PLAY CHANNEL SUCCESS $temp")
                                    }

                                })
                            }
                        }
                    })
                }
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAssistantChannel: ${R.string.no_channel_associated}")
                resumePlayback()
            }
            intent.data = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkRuntimePermissions() {
        //Check runtime permissions
        val notGrantedPermissions = ArrayList<String>()
        val appPermissionsToCheck = appPermissions.toMutableList()
        if (!(worldHandler as ReferenceWorldHandler).isFastOnly()) {
            appPermissionsToCheck.addAll(appPermissionsOptional)
        }

        for (permissionId in appPermissionsToCheck) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    permissionId
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notGrantedPermissions.add(permissionId)
            }
        }

        if (notGrantedPermissions.isEmpty()) {
            startSdkInit()
        } else {
            isWaitingForUserPermission = true
            ActivityCompat.requestPermissions(
                this,
                notGrantedPermissions.toTypedArray(),
                REQUEST_CODE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (grantResults.size > 0) {
            when (requestCode) {
                REQUEST_CODE -> {
                    if(isMandatoryPermissionsGranted()){
                        isWaitingForUserPermission = false
                        startSdkInit()
                    } else {
                        showToast( "Permissions are not granted!")
                        //Exit application if denied
                        CoroutineHelper.runCoroutine({
                            worldHandler!!.destroyExisting()
                            exitApplication()
                        }, Dispatchers.Main)
                    }
                }
                else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showAnokiModeToast() {
        if (((worldHandler) as ReferenceWorldHandler).isFastOnly() && moduleProvider.getParentalControlSettingsModule().isAnokiParentalControlsEnabled()) {
            val ratingLevel =
                moduleProvider.getParentalControlSettingsModule().getAnokiRatingLevel()
            var mode = moduleProvider.getParentalControlSettingsModule().getAnokiRatingList()
                .get(ratingLevel)
            runOnUiThread {
                showToast("$mode mode", UtilsInterface.ToastDuration.LENGTH_LONG)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun observeLastInterfaceChange() {
        moduleProvider.getInputSourceMoudle().inputChanged.observe(this) { isInputChanged ->
            if (isInputChanged) {
                lastChangeInputTime = System.currentTimeMillis()
            }
        }
    }

    /**
     * Checks whether the mandatory permissions are granted.
     *
     * @return `true` if permissions granted
     */
    fun isMandatoryPermissionsGranted(): Boolean {
        //Check runtime permissions
        val notGrantedPermissions = ArrayList<String>()

        for (permissionId in appPermissions) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    permissionId
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notGrantedPermissions.add(permissionId)
            }
        }

        return notGrantedPermissions.isEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume")
        if (intent?.action == Constants.PARENTAL_CONTROL_INTENT_ACTION) {
            super.onResume()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume parental_control_intent")
            if (!ReferenceApplication.parentalControlDeepLink) {
                showParentalControlSettings()
            }
            return
        }
        val isMuted = moduleProvider.getUtilsModule().getPrefsValue("PREF_KEY_IS_MUTED", false) as Boolean
        if (!isMuted) {
            if (moduleProvider.getPvrModule().isRecordingInProgress()) {
                moduleProvider.getPlayerModule().unmute()
            }
        } else {
            mAudioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0
            )
            moduleProvider.getPlayerModule().mute()
        }

        if(ReferenceApplication.isHiddenFromSetings){
            BackFromPlayback.resetKeyPressedState()
        }

        if(settingsIconClicked) {
            //prevent Home screen to reset to Broadcast tab after returning from settings via settings wheel button icon
            //in onPause we hide Home screen when entering settings by pressing settings wheel button icon
            BackFromPlayback.setLiveSceneFromLiveHomeState(false)
            BackFromPlayback.resetKeyPressedState()
            if (BuildConfig.FLAVOR == "rtk") {
                if (settingsIconClicked) BackFromPlayback.backKeyPressed()
            }
        }
        tosCanceled = false
        if (intent != null && intent.hasExtra("tos_canceled")) {
            tosCanceled = true
        }
        setLiveTvInput()
        settingsIconClicked = false
        startChannelFromLiveTab = false
//        waitingStartChannelFromLiveTab = false
        ReferenceApplication.isOkClickedOnSetUp = false
        if(intent != null && intent.action == "android.intent.action.VIEW" && intent.dataString != null){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Started application from live tab")
            val intentDataInformation = intent.dataString!!.split("?")
            for(dataInformation in intentDataInformation){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Started application from live tab data info string $dataInformation")
                if(dataInformation.contains("content://android.media.tv/channel/")){
                    var channelName = dataInformation.split("content://android.media.tv/channel/")[1]
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Started application from live tab channel name $channelName")
                    if(channelName.isDigitsOnly()) {
                        channelIdFromLiveTab = channelName.toInt()
                        startChannelFromLiveTab = true
//                        waitingStartChannelFromLiveTab = true
                        moduleProvider.getUtilsModule().setPrefsValue(Constants.SharedPrefsConstants.STARTED_CHANNEL_FROM_LIVE_TAB_TAG, true)
                        val tvChannel: TvChannel? = moduleProvider.getTvModule().getChannelById(channelIdFromLiveTab!!)
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "set channel from Live Tab as playable item: ${tvChannel?.name}")
                        if (tvChannel != null && !moduleProvider.getUtilsModule().isThirdPartyChannel(tvChannel)) {
                            moduleProvider.getPlayerModule().liveTabChannel = tvChannel
                        }
                    }
                    break
                }
            }
            if(inBackground && startChannelFromLiveTab){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Started application from live while app in background")
                moduleProvider.getFastUserSettingsModule().checkTos(object : IAsyncDataCallback<Boolean>{
                    override fun onFailed(error: Error) {}
                    override fun onReceive(data: Boolean) {
                        if(data){
                            changeChannelFromLiveTab()
                        }else{
                            startTosActivity()
                        }
                    }
                })
                if( (inBackground && exitApplicationAfterScan) || (accessibilityWasOn != moduleProvider.getUtilsModule().isAccessibilityEnabled())){
                    accessibilityWasOn = moduleProvider.getUtilsModule().isAccessibilityEnabled()
                    super.onResume()
                    CoroutineHelper.runCoroutineWithDelay({
                        Utils.restartApp()
                    },10)
                    return
                }
                super.onResume()
            }
        }

        if( (newChannelsFoundResetApp && moduleProvider.getTvModule().getChannelList().size != 0) || (inBackground && exitApplicationAfterScan) || (accessibilityWasOn != moduleProvider.getUtilsModule().isAccessibilityEnabled())){
            accessibilityWasOn = moduleProvider.getUtilsModule().isAccessibilityEnabled()
            newChannelsFoundResetApp = false
            super.onResume()
            CoroutineHelper.runCoroutineWithDelay({
                Utils.restartApp()
            },10)
            return
        }
        accessibilityWasOn = moduleProvider.getUtilsModule().isAccessibilityEnabled()

        exitApplicationAfterScan = false
        inBackground = false
        inputSelected = moduleProvider.getInputSourceMoudle().getDefaultValue()
        if(inputSourceSelected == null) {
            inputSourceSelected = moduleProvider.getInputSourceMoudle().getInputActiveName()
        }
        if(inputTuneURL == null) {
            inputTuneURL = moduleProvider.getInputSourceMoudle().getDefaultURLValue()
        }
        if (ReferenceApplication.isInputPaused || (inputSelected != "TV")) {
            if(!ReferenceApplication.isBackFromInput) {
                CoroutineHelper.runCoroutine({
                    handleInputChangeData()
                    isExternalInput = true
                }, Dispatchers.Main)
            }
            ReferenceApplication.isBackFromInput = false
        }

        if (inputSelected == "TV" && !startChannelFromLiveTab) {
            handleInputChangeData()
        }
        if (inputSelected.contains("Home") && !startChannelFromLiveTab) {
            setInputToTv()
            tuneToTv()
        }

        ReferenceApplication.isInForeground = true
        ReferenceApplication.isAppPaused = false
        if (ReferenceApplication.getActivity().localClassName == "InputSourceActivity") {
            (ReferenceApplication.getActivity() as InputSourceActivity).stopUpdateTimer()
            (ReferenceApplication.getActivity() as InputSourceActivity).finish()
            ReferenceApplication.setActivity(this)
        }
        try {
            super.onResume()
        } catch (e:Exception) {
            Utils.restartApp()
        }
        if (isIntroPaused) {
            return
        }
        mAudioManager?.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build())

        if(inputSelected == "TV") {
            if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE) {
                resumePlayer()
            }

            //Kill TS when application is moved to background and show lock screen based on parental flag
            if (isTimeshiftDestroyed) {
                isTimeshiftDestroyed = false
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.PLAYER_SCENE,
                    SceneManager.Action.DESTROY
                )
                resumePlayer()
            }

            if (intent != null && intent.action == "android.media.tv.action.SETUP_INPUTS") {
                try {
                    //Try triggering SCAN SCREEN from custom settings
                    var intent = Intent("android.settings.CHANNELS_SETTINGS")
                    if (BuildConfig.FLAVOR == "rtk")
                        intent = Intent("android.settings.SETTINGS")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    ReferenceApplication.applicationContext()
                        .startActivity(intent)
                } catch (e: Exception) {

                    // Stop live playback and show channels scan (FtiSelectInputScanScene)
                    stopPlayback()
                    runOnUiThread {

                        var activeScene = worldHandler.active!!.scene
                        activeScene?.let {
                            worldHandler!!.triggerAction(it.id, SceneManager.Action.HIDE)
                            val sceneData = SceneData(it.id, it.instanceId)
                            worldHandler!!.triggerActionWithData(
                                ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
                                SceneManager.Action.SHOW,
                                sceneData
                            )
                        }
                    }
                }
                return
            }

            if (intent.hasExtra("Channel_ID")) {
                if (ReferenceApplication.isInitalized) {
                    getLauncherOrNotificationChannel()
                }
                intent.removeExtra("Channel_ID")
                return
            }

            if (ReferenceApplication.scanPerformed) {
                // Go back to live if the scan is performed
                ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.LIVE,
                    SceneManager.Action.SHOW
                )
                ReferenceApplication.scanPerformed = false

                var playerState = moduleProvider.getPlayerModule().playerState
                if (playerState != PlayerState.PLAYING) {
                    moduleProvider.getTvModule()
                        .getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                            override fun onFailed(error: Error) {
                            }

                            override fun onReceive(activeChannel: TvChannel) {
                                if (activeChannel != null) {

                                    if (activeChannel.isLocked) {
                                        InformationBus.submitEvent(Event(com.iwedia.cltv.platform.model.information_bus.events.Events.ACTIVE_CHANNEL_LOCKED_EVENT))
                                    } else {
                                        InformationBus.submitEvent(Event(com.iwedia.cltv.platform.model.information_bus.events.Events.ACTIVE_CHANNEL_UNLOCKED_EVENT))

                                        var playableItem =
                                            PlayableItem(PlayableItemType.TV_CHANNEL, activeChannel)
                                        resumePlayback()
                                    }
                                }
                            }
                        })
                }
                //Reset the next/prev zap list to ALL channels
                moduleProvider.getTvModule().activeCategoryId = FilterItemType.ALL_ID
                moduleProvider.getTvModule().getRecentlyWatched()?.clear()
//            updateHomeSceneData()
                return
            } else if (moduleProvider.getTvModule().getBrowsableChannelList()
                    .size == 0 && moduleProvider.getTvModule().getChannelList(ApplicationMode.FAST_ONLY).isEmpty()
            ) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG,"Channel list empty clear prefs")
                moduleProvider.getUtilsModule().clearChannelidsPref()
                InformationBus.submitEvent(Event(com.iwedia.cltv.platform.model.information_bus.events.Events.CHANNEL_LIST_IS_EMPTY))
            }
             else if (ReferenceApplication.isSettingsOpened) {
                ReferenceApplication.isSettingsOpened = false
                return
            }
            // Check if the time shift was running in background when the app is on paused state
            if (moduleProvider.getTimeshiftModule().isTimeShiftActive) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: isTimeShiftPaused $isTimeShiftPaused")
                var intentData = intent.getData().toString()
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: INTENT DATA ENTERED $intentData")
                Log.d(Constants.LogTag.CLTV_TAG +
                    TAG,
                    "onResume: ReferenceApplication.isInitalized ${ReferenceApplication.isInitalized}"
                )
                if (intentData.contains(CHANNEL_INTENT_DATA)) {
                    if (ReferenceApplication.isInitalized) {
                        showTimeShiftExitDialog()
                    }
                    return
                } else {
                    if (isTimeShiftPaused) {
                        moduleProvider.getTimeshiftModule()
                            .resumeTimeShift(object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                }

                                override fun onSuccess() {
                                    isTimeShiftPaused = false
                                }
                            })
                    }
                }
                return
            }

            var isDirectScan: Boolean = false

            var intentData = intent.getData().toString()

            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: INTENT DATA ENTERED $intentData")
            if (intentData.contains(CHANNEL_INTENT_DATA)) {
                if (ReferenceApplication.isInitalized) {
                    getAssistantChannel()
                }
                return
            }
            /**
             * Need to check if intent is for rtk -> Guide
             * checkGuideIntent() is platform based function
             * which check if intent data Uri contains specific keys
             * */
            val ifFromGuide = moduleProvider.getUtilsModule().checkGuideIntent(intent)
            //Check is com.google.android.apps.tv.launcherx.dashboard.DashboardActivity opened
            //There is a issue after going back from this activity it sends guide intent content://android.media.tv/program
            val dashboardActivityExtra = intent.hasExtra("dashboard_activity")
            intent.removeExtra("dashboard_activity")

            if (!dashboardActivityExtra && ifFromGuide && (worldHandler.active!!.id != ReferenceWorldHandler.SceneId.INTRO)) {
                if (ReferenceApplication.parentalControlDeepLink) {
                    return
                }
                if (moduleProvider.getTTXModule().isTTXPreviouslyActive()) {
                    moduleProvider.getTTXModule().updateTTXStatus(true)
                    return
                }
                if((moduleProvider.getPvrModule().isRecordingInProgress())){
                    moduleProvider.getPvrModule().showRecIndication.value = true
                }
                if (moduleProvider.getInputSourceMoudle()
                        .getDefaultValue() == "TV" && worldHandler.active?.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE
                ) {
                    if (ReferenceApplication.isInitalized) {
                        if (worldHandler.active is HomeSceneManager) {
                            if ((worldHandler.active as HomeSceneManager).scene is HomeSceneFast) {
                                val homeScene = (worldHandler.active as HomeSceneManager).scene as HomeSceneFast
                                var applicationMode = (worldHandler as ReferenceWorldHandler).getApplicationMode()
                                if (applicationMode == ApplicationMode.DEFAULT.ordinal) {
                                    if (homeScene.checkSearchFocusOnGuideKeyPress()) {
                                        return
                                    }
                                }
                                val activeCategory = homeScene.getActiveCategoryId()
                                if ((activeCategory == 2 && applicationMode == ApplicationMode.DEFAULT.ordinal && !homeScene.isBtnFocused())
                                    || (activeCategory == 1 && applicationMode == ApplicationMode.FAST_ONLY.ordinal && !homeScene.isBtnFocused())) {
                                    onGuideKeyInHomeScene()
                                    return
                                }
                            } else if((worldHandler.active as HomeSceneManager).scene is HomeSceneBroadcast){
                                val homeScene = (worldHandler.active as HomeSceneManager).scene as HomeSceneBroadcast
                                if (homeScene.checkSearchFocusOnGuideKeyPress()) {
                                    return
                                }
                                if (homeScene.getActiveCategoryId() == 1 && !homeScene.isBtnFocused()) {
                                    onGuideKeyInHomeScene()
                                    return
                                }
                            }
                        }
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "Epg intent rec - ${moduleProvider.getNetworkModule().networkStatus.value != NetworkData.NoConnection} | ${ReferenceApplication.isRegionSupported}"
                        )
                        openedGuideFromIntent = true
                        if(moduleProvider.getNetworkModule().networkStatus.value != NetworkData.NoConnection && ReferenceApplication.isRegionSupported) {
                            moduleProvider.getFastUserSettingsModule()
                                .checkTos(object : IAsyncDataCallback<Boolean> {
                                    override fun onFailed(error: Error) {}
                                    override fun onReceive(data: Boolean) {
                                        // "moduleProvider.getNetworkModule().anokiServerStatus.value != true"
                                        // Above condition has to check because if in case user completed FTI without internet
                                        // and tried to open EPG with RCU key then we should allow user to open Broadcast tab even though TOS screen not appeared.
                                        Log.d(Constants.LogTag.CLTV_TAG +
                                            TAG,
                                            "Epg intent rec - $data | ${moduleProvider.getNetworkModule().anokiServerStatus.value}"
                                        )
                                        if (data || moduleProvider.getNetworkModule().anokiServerStatus.value != true) {
                                            runOnUiThread {
                                                worldHandler.destroyOtherExisting(
                                                    ReferenceWorldHandler.SceneId.LIVE
                                                )
                                                var sceneId =
                                                    ReferenceApplication.worldHandler?.active?.id
                                                var sceneInstanceId =
                                                    ReferenceApplication.worldHandler?.active?.instanceId
                                                if (sceneInstanceId == null || sceneId == null) {
                                                    //if scene id is null that means there is no live scene present
                                                    //so on back press it will show black blank screen.
                                                    worldHandler.triggerAction(
                                                        ReferenceWorldHandler.SceneId.LIVE,
                                                        SceneManager.Action.SHOW
                                                    )
                                                    sceneId =
                                                        ReferenceApplication.worldHandler?.active?.id
                                                    sceneInstanceId =
                                                        ReferenceApplication.worldHandler?.active?.instanceId
                                                }
                                                //if region is not supported then there will be only 2 options available
                                                //guide will be on position 1 not on 2 or 3.
                                                var position = if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) 1 else 2
                                                if (!ReferenceApplication.isRegionSupported) {
                                                    position = 1
                                                }

                                                val sceneData = sceneId?.let {
                                                    sceneInstanceId?.let { it1 ->
                                                        HomeSceneData(it, it1, position)
                                                    }
                                                }
                                                sceneData?.initialFilterPosition = position
                                                sceneData?.focusToCurrentEvent = true
                                                //isPaused should be true when we are pressing guide button while,
                                                // we are in live tab,so that it can reset its view when we enter again in fast.
                                                if (FastLiveTabDataProvider.fastLiveTab != null) {
                                                    FastLiveTabDataProvider.fastLiveTab!!.isPaused =
                                                        true
                                                }
                                                //need to hide fast zap banner if it is visible.
                                                val intent =
                                                    Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                                                ReferenceApplication.applicationContext()
                                                    .sendBroadcast(intent)
                                                worldHandler.triggerActionWithData(
                                                    ReferenceWorldHandler.SceneId.HOME_SCENE,
                                                    SceneManager.Action.SHOW_OVERLAY, sceneData
                                                )
                                            }
                                        }
                                        else {
                                            startTosActivity()
                                        }
                                    }
                                })
                        }
                        else{
                            runOnUiThread{
                                if (FastLiveTabDataProvider.fastLiveTab != null) {
                                    FastLiveTabDataProvider.fastLiveTab!!.isPaused =
                                        true
                                }
                                worldHandler.destroyOtherExisting(
                                    ReferenceWorldHandler.SceneId.LIVE
                                )
                                var position = if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) 1 else 2
                                if (!ReferenceApplication.isRegionSupported) {
                                    position = 1
                                }

                                val sceneData = HomeSceneData(1, 1, position)
                                sceneData.initialFilterPosition = position
                                sceneData.focusToCurrentEvent = true
                                val intent =
                                    Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                                ReferenceApplication.applicationContext()
                                    .sendBroadcast(intent)
                                worldHandler.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.HOME_SCENE,
                                    SceneManager.Action.SHOW_OVERLAY, sceneData
                                )

                            }
                        }
                        intent.data = null
                    }
                    else {
                        ReferenceApplication.guideKeyPressed = true
                    }
                }

                return
            }

            //Following code help to open Recorded tab with voice assistance
            if (intentData.equals(RECORD_INTENT_DATA)) {
                if (ReferenceApplication.isInitalized) {
                    runOnUiThread {
                        worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        var sceneId = ReferenceApplication.worldHandler?.active?.id
                        var sceneInstanceId = ReferenceApplication.worldHandler?.active?.instanceId
                        var position: Int = 3

                        var sceneData = SceneData(sceneId!!, sceneInstanceId!!, position)
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.HOME_SCENE,
                            SceneManager.Action.SHOW_OVERLAY, sceneData
                        )
                    }

                    intent.data = null
                }

                return
            }
            //end
            if (ReferenceApplication.guideKeyPressed && ReferenceApplication.isInitalized) {
                onGuideKeyPressed()
                ReferenceApplication.guideKeyPressed = false
            }

            if (ReferenceApplication.infoKeyPressed && ReferenceApplication.isInitalized) {
                onInfoKeyPressed()
                ReferenceApplication.infoKeyPressed = false
                return
            }

            // Check if scan is initiated from settings
            if (intent.action == SCAN_ACTION) {
                isDirectScan = true
            }

            // null on FTI
            moduleProvider.getUtilsModule().setPrefsValue(SCAN_ACTION, isDirectScan)

            // Start scan or resume LIVE
            if (ReferenceApplication.isInitalized) {
                if (isDirectScan && ReferenceApplication.worldHandler?.active != null) {
                    // Stop live playback and show channels scan (FtiSelectInputScanScene)
                    stopPlayback()
                    runOnUiThread {
                        var sceneId = ReferenceApplication.worldHandler?.active?.id
                        var sceneInstanceId = ReferenceApplication.worldHandler?.active?.instanceId
                        worldHandler!!.triggerAction(sceneId!!, SceneManager.Action.HIDE)
                        val sceneData = SceneData(sceneId!!, sceneInstanceId!!)
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.FTI_SELECT_INPUT_SCAN,
                            SceneManager.Action.SHOW,
                            sceneData
                        )
                    }
                } else if (!isDirectScan) {
                    val data = intent.data
                    if (data != null) {
                        val gson = Gson()
                        var contentEntity: ContentEntity? = null
                        try {
                            contentEntity = gson.fromJson(
                                data.toString().trim(),
                                ContentEntity::class.java
                            )

                            if (contentEntity != null) {

                                var contentEntityData = contentEntity.data

                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: CONTENT ENTITY DATA $contentEntityData")
                                var channelToPlay: TvChannel? = null

                                //Tv channel
                                if (contentEntity.contentSourceId == 1) {
                                    channelToPlay = gson.fromJson(
                                        contentEntityData.toString(),
                                        TvChannel::class.java
                                    )
                                }

                                //Tv event
                                else if (contentEntity.contentSourceId == 2) {

                                    var tvEvent = gson.fromJson(
                                        contentEntityData.toString(),
                                        TvEvent::class.java
                                    )

                                    channelToPlay = tvEvent.tvChannel
                                }
                                moduleProvider.getTvModule()
                                    .changeChannel(channelToPlay!!, object : IAsyncCallback {
                                        override fun onFailed(error: Error) {
                                        }

                                        override fun onSuccess() {
                                        }
                                    })
                            }
                        } catch (e : Exception) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"exception ${e.printStackTrace()}")
                        }
                    } else {
                        // Check internet connection
                        if (moduleProvider.getNetworkModule().networkStatus.value == NetworkData.NoConnection) {
                            if (((worldHandler) as ReferenceWorldHandler).isFastOnly()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    showNoInternetDialog()
                                }
                            } else {
                                runOnUiThread {
                                    showToast(ConfigStringsManager.getStringById("no_ethernet_message"))
                                }
                                if (ReferenceApplication.worldHandler!!.active == null && ReferenceApplication.channelScanFromSettings) {
                                    ReferenceApplication.worldHandler!!.triggerAction(
                                        ReferenceWorldHandler.SceneId.LIVE,
                                        SceneManager.Action.SHOW
                                    )
                                }
                            }
                        }else if (ReferenceApplication.worldHandler!!.active == null && ReferenceApplication.channelScanFromSettings) {
                            ReferenceApplication.worldHandler!!.triggerAction(
                                ReferenceWorldHandler.SceneId.LIVE,
                                SceneManager.Action.SHOW
                            )
                        } 
                    }
                }
            } else {
                // Check if event is triggered from assistant
                if (intent.action == VIEW_ACTION) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResume: PRELOADED CHANNEL")

                    val gson = Gson()
                    var data = intent.data

                    if (data.toString().contains("fromHomePrograms")) {
                        //Intent is from Amzn live app, extract channel to play
                        val temp = data.toString().split("/", "=")
                        val channelToPlay = temp[temp.size - 1]

                        // ReferenceSdk is not ready, store the channel which needs to be played once player is ready
                        applicationContext.getSharedPreferences(
                            "OnlinePrefsHandler",
                            Context.MODE_PRIVATE
                        ).edit()
                            .putInt("PRELOADED_CHANNEL_ID", channelToPlay.toInt()).apply()
                    } else {
                        // Check if event is triggered from assistant
                        var contentEntity: ContentEntity? = null

                        if (data.toString().contains("content:")) {
                            contentEntity = null
                        } else if(!data.toString().contains("customize:")) { //Was crashing for other scheme than content
                            contentEntity = gson.fromJson(
                                data.toString().trim(),
                                ContentEntity::class.java
                            )
                        }

                        if (contentEntity != null) {
                            var contentEntityData = contentEntity.data
                            var channelToPlay: TvChannel? = null

                            //Tv channel
                            if (contentEntity.contentSourceId == 1) {
                                channelToPlay = gson.fromJson(
                                    contentEntityData.toString(),
                                    TvChannel::class.java
                                )
                            }

                            //Tv event
                            else if (contentEntity.contentSourceId == 2) {

                                var tvEvent = gson.fromJson(
                                    contentEntityData.toString(),
                                    TvEvent::class.java
                                )

                                channelToPlay = tvEvent.tvChannel
                            }
                            applicationContext.getSharedPreferences(
                                "OnlinePrefsHandler",
                                Context.MODE_PRIVATE
                            )
                                .edit()
                                .putInt("PRELOADED_CHANNEL_ID", channelToPlay!!.index).apply()
                        }
                    }
                }
            }
        }

        if (ReferenceApplication.isInitalized) {
            CoroutineHelper.runCoroutine({
                updateRecommendationList()
            })
        }
        if (ReferenceApplication.isInForeground) {
            startLicenseExpiryValidation()
        }

        refreshNoSignalPowerOff()

        if(ReferenceApplication.isInitalized && moduleProvider.getUtilsModule().needTCServiceUpdate(applicationContext)){
            showNewChannelsFoundPopup()
        }

        if(worldHandler.active?.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
            TvViewScaler.scaleDownTvView()
        }
    }

    private fun onGuideKeyInHomeScene() {
        //Back to live playback on guide key press if home scene is visible
        BackFromPlayback.resetKeyPressedState()
        ReferenceApplication.worldHandler?.triggerAction(ReferenceWorldHandler.SceneId.HOME_SCENE, SceneManager.Action.HIDE) // HIDE is used because whole Home Scene should stay in memory in order to enable it's fast accessing when pressing back from LiveScene
        var applicationMode =
            if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT

        moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: kotlin.Error) {}
            override fun onReceive(activeChannel: TvChannel) {
                if (activeChannel.isFastChannel()) {
                    runOnUiThread {
                        //Use channel changed information bus event to show zap banner
                        InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, activeChannel))
                    }
                } else
                if (worldHandler?.active?.id != ReferenceWorldHandler.SceneId.ZAP_BANNER) {
                    if (ReferenceApplication.worldHandler!!.playbackState != ReferenceWorldHandler.PlaybackState.RECORDING) {
                        runOnUiThread {
                            ReferenceApplication.worldHandler!!.active?.let {
                                var sceneData =
                                    SceneData(it.id, it.instanceId, activeChannel)
                                worldHandler!!.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                    SceneManager.Action.SHOW_OVERLAY,
                                    sceneData
                                )
                            }
                        }
                    }
                }
            }
        }, applicationMode)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleInputChangeData() {
        if ((BuildConfig.FLAVOR == "mtk") && (!isFromOnNewIntent!!)) {
            if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.INTRO) {
                if (worldHandler.active == null) {
                    ReferenceApplication.worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.LIVE,
                        SceneManager.Action.SHOW
                    )
                }
                if (moduleProvider.getInputSourceMoudle().isBlock(inputSelected)
                ) {
                    InformationBus.submitEvent(
                        Event(Events.BLOCK_TV_VIEW)
                    )
                } else {
                    moduleProvider.getInputSourceMoudle().handleInputSource(inputSelected, inputTuneURL)
                }
            }
        }
    }

    private fun showTimeShiftExitDialog() {
        var currentActiveScene = ReferenceApplication.worldHandler!!.active
        runOnUiThread {
            var sceneData =
                DialogSceneData(currentActiveScene!!.id, currentActiveScene!!.instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")

            worldHandler.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                SceneManager.Action.SHOW, sceneData
            )

            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onNegativeButtonClicked() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onNegativeButtonClicked: isTimeShiftPaused $isTimeShiftPaused")
                    if (isTimeShiftPaused) {
                        moduleProvider.getTimeshiftModule().resumeTimeShift(object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                            }

                            override fun onSuccess() {
                                isTimeShiftPaused = false
                            }
                        })
                    }
                    intent.data = null
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onPositiveButtonClicked() {
                    moduleProvider.getTimeshiftModule().timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                        }

                        override fun onSuccess() {
                        }
                    })
                    worldHandler.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                    worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    moduleProvider.getTimeshiftModule().setTimeShiftIndication(false)
                    resumePlayback()
                }
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onDestroy() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onDestroy")
        ReferenceApplication.isInForeground = false
        ReferenceApplication.isAppPaused = false
        ReferenceApplication.isInitalized = false
        ReferenceApplication.isInputPaused = false
        ReferenceApplication.isPrefClicked = false
        ReferenceApplication.parentalControlDeepLink = false
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onDestroy: ")

        //Reset the next/prev zap list to ALL channels
        moduleProvider.getTvModule().activeCategoryId = FilterItemType.ALL_ID
        moduleProvider.getInputSourceMoudle().dispose()
        moduleProvider.getEasModule().disposeEas()
        moduleProvider.getClosedCaptionModule().disposeClosedCaption()
        moduleProvider.getUtilsModule().dispose()
        moduleProvider.getTTXModule().dispose()
        moduleProvider.getTvModule().dispose()
        exitTvInput()
        super.onDestroy()

        stopPlayback()

        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, ContentAggregatorServiceRestarter::class.java)
        this.sendBroadcast(broadcastIntent)
        unregisterReceiver(scanStartedReceiver)
        unregisterReceiver(languageChangeReceiver)
        unregisterReceiver(fastEPGUpdateReceiver)
        unregisterReceiver(fastChannelReorderReceiver)

        if (channelRowTimer != null) {
            channelRowTimer!!.cancel()
            channelRowTimer = null
        }

        mAudioManager?.abandonAudioFocusRequest(
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_LOSS).build()
        )
        stopInactivityTimer()
        stopBackgroundExitTimer()
    }

    override fun initLayoutIds() {
        layoutMainId = R.id.layout_main
        introPlaybackViewId = R.id.introPlaybackView
        livePlaybackViewId = R.id.livePlaybackView
        contentPlaybackViewId = R.id.contentPlaybackView
        pvrPlaybackViewId = R.id.pvrPlaybackView
        pvrPreviewViewId = R.id.pvrPreviewView
        uiViewId = R.id.uiView
        notificationViewId = R.id.notificationView
        globalViewId = R.id.globalView
        loadingId = R.id.loading
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TV_INPUT_SETUP_ACTIVITY_REQUEST_CODE) {
            moduleProvider.getTvInputModule()!!.triggerScanCallback(resultCode == RESULT_OK)
        } else if (requestCode == 1000) {

            ReferenceApplication.runOnUiThread {
                ReferenceApplication.worldHandler!!.destroyExisting()
                stopInactivityTimer()
            }
        } else if (requestCode == TOS_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val intent = Intent(ReferenceApplication.FAST_SCAN_START)
                applicationContext.sendBroadcast(intent)
                InformationBus.submitEvent(Event(Events.TOS_ACCEPTED))
            } else {
                finishAffinity()
            }
        } else if (requestCode == PinHelper.REQUEST_CONFIRM_PIN) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "pin activity result $resultCode")
            if (resultCode == RESULT_OK) {
                PinHelper.triggerPinResultCallback(true)
            } else {
                PinHelper.triggerPinResultCallback(false)
            }
        }
    }

    /**
     * Setup recording indicator layout
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupIndicator() {
        indicator = findViewById(R.id.indicator)
        indicator?.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor(
                ConfigColorManager.getColor("color_main_text")
                    .replace("#", ConfigColorManager.alfa_light)
            )
        )

        indicatorChannelTextView = indicator?.findViewById<TextView>(R.id.indicator_channel_name)
        indicatorChannelTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        indicatorChannelImageView = indicator?.findViewById(R.id.indicator_channel_image)
        indicatorIcon = indicator?.findViewById(R.id.indicator_icon)

        moduleProvider.getPvrModule().showRecIndication.observe(
            this
        ) { showRecIcon ->
            if (showRecIcon!!) {
                moduleProvider.getPvrModule().getRecordingInProgress(
                    object :
                        IAsyncDataCallback<com.iwedia.cltv.platform.model.recording.RecordingInProgress> {
                        override fun onFailed(error: Error) {
                            showIndicator(showRecIcon!!, null)
                        }

                        override fun onReceive(data: com.iwedia.cltv.platform.model.recording.RecordingInProgress) {
                            showIndicator(
                                showRecIcon,
                                data.tvChannel
                            )
                        }
                    })
            } else {
                showIndicator(showRecIcon!!, null)
            }
        }

        moduleProvider.getTimeshiftModule().showTimeShiftIndication.observe(
            this
        ) { showIcon ->
            if (showIcon!!) {
                moduleProvider.getTvModule().getActiveChannel(
                    object : IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                            showIndicator(showIcon!!, null)
                        }

                        override fun onReceive(tvChannel: TvChannel) {
                            showIndicator(
                                showIcon,
                                tvChannel
                            )
                        }
                    })
            } else {
                showIndicator(showIcon!!, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showIndicator(show: Boolean, tvChannel: TvChannel?) {
        if (!show) {
            indicator?.visibility = View.GONE
            return
        }
        setIndicatorRes()
        if (tvChannel != null) {
            val channelLogoPath = tvChannel.logoImagePath

            if (channelLogoPath != null) {
                Utils.loadImage(
                    channelLogoPath,
                    indicatorChannelImageView!!,
                    object : AsyncReceiver {
                        override fun onFailed(error: core_entities.Error?) {
                            indicatorChannelImageView!!.visibility = View.GONE
                            setIndicatorChannelTextSize(tvChannel!!.name)
                            indicatorChannelTextView!!.visibility = View.VISIBLE
                            indicatorChannelTextView!!.text = tvChannel!!.name
                        }

                        override fun onSuccess() {
                            indicatorChannelTextView!!.visibility = View.GONE
                            indicatorChannelImageView!!.visibility = View.VISIBLE
                        }
                    })
            } else {
                indicatorChannelImageView!!.visibility = View.GONE
                setIndicatorChannelTextSize(tvChannel!!.name)
                indicatorChannelTextView!!.visibility = View.VISIBLE
                indicatorChannelTextView!!.text = tvChannel!!.name
            }
        } else {
            indicatorChannelImageView!!.visibility = View.GONE
            indicatorChannelTextView!!.visibility = View.GONE
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun setIndicatorRes() {
        var background = ConfigColorManager.generateBackground("color_not_selected", 17f, 0.2)
        indicator?.background = background
        indicatorChannelTextView!!.setTextColor(
            Color.parseColor(ConfigColorManager.getColor("color_text_description"))
        )
        indicatorChannelTextView!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        if (moduleProvider.getTimeshiftModule().isTimeShiftActive) {
            indicator?.visibility = View.VISIBLE
            indicatorIcon?.visibility = View.VISIBLE
            indicatorChannelTextView?.visibility = View.VISIBLE
            indicatorIcon?.setImageDrawable(
                resources.getDrawable(
                    R.drawable.ic_player_icons_pause,
                    null
                )
            )
        } else {
            if (moduleProvider.getTimeshiftModule().isTimeShiftPaused || (moduleProvider.getPvrModule().isRecordingInProgress())) {
                indicator?.visibility = View.VISIBLE
                indicatorIcon?.visibility = View.VISIBLE
                indicatorChannelTextView?.visibility = View.VISIBLE
                indicatorIcon?.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.recording_indicator,
                        null
                    )
                )
                indicatorIcon?.imageTintList =
                    ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_pvr_and_other")))
            } else {
                indicator?.visibility = View.GONE
                indicatorIcon?.visibility = View.GONE
                indicatorChannelTextView?.visibility = View.GONE

            }
        }
    }

    private fun setIndicatorChannelTextSize(channelName: String) {
        if (channelName.length > 6) {
            val textSize =
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
            indicatorChannelTextView!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        } else {
            val textSize =
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_15)
            indicatorChannelTextView!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }
    }

    /**
     * Broadcast guide key pressed intent to the GlobalKeyReceiver
     */
    private fun onGuideKeyPressed() {
        val intent = Intent(GlobalAppReceiver.GLOBAL_KEY_INTENT_ACTION)
        intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_ACTION, 1)
        intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_CODE, 172)
        sendBroadcast(intent)
    }

    /**
     * Broadcast info key pressed intent to the InfoKeyReceiver
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun onInfoKeyPressed() {
        var applicationMode =
            if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(tvChannel: TvChannel) {
                if (!tvChannel.isFastChannel()) {
                    val intent = Intent(GlobalAppReceiver.GLOBAL_KEY_INTENT_ACTION)
                    intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_ACTION, 1)
                    intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_CODE, 165)
                    sendBroadcast(intent)
                }
            }
        }, applicationMode)
    }

    private fun onMenuKeyPressed() {
        val intent = Intent(GlobalAppReceiver.GLOBAL_KEY_INTENT_ACTION)
        intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_ACTION, 1)
        intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_CODE, KeyEvent.KEYCODE_MENU)
        sendBroadcast(intent)
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun insertPrograms(channelId: Long) {
        var channelList = moduleProvider.getTvModule().getChannelList()

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "insertPrograms: CHANNEL LIST SIZE $channelList.size")

        recommendationImagePath = File(filesDir, "launcher_recommendation")
        recommendationImagePath!!.mkdir()

        //old files
        val oldFiles = recommendationImagePath!!.list()

        val view = LayoutInflater.from(this)
            .inflate(R.layout.launcher_recommendation_item, previewViewContainer, false)

        var index = 0

        val channelListTemp: ArrayList<com.iwedia.cltv.platform.model.TvChannel> = ArrayList()

        channelList.forEachIndexed { index, tvChannel ->
            channelListTemp.add(tvChannel)
        }

        val listener = object : ProgramInsertListener {

            @SuppressLint("RestrictedApi")
            override fun checkNextInsert() {
                if (index < channelListTemp.size && previewBuilderList.size < MAX_COUNT_LAUNCHER_RECOMMENDATION) {
                    insertProgram(
                        view,
                        channelListTemp[index++],
                        channelId,
                        this
                    )
                } else {
                    //on finished inserting

                    //fetching old programs
                    val oldPrograms = Utils.getPreviewPrograms(
                        ReferenceApplication.applicationContext(),
                        channelId
                    )

                    // insert new programs
                    previewBuilderList.forEach {
                        var programUri1 = applicationContext.contentResolver.insert(
                            TvContractCompat.PreviewPrograms.CONTENT_URI,
                            it.build().toContentValues()
                        )
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkNextInsert: PROGRAM URI 2 $programUri1")
                    }
                    previewBuilderList.clear()

                    //remove old
                    oldPrograms.forEachIndexed { index, previewProgram ->
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkNextInsert: PROGRAMS SIZE ELSE ${oldPrograms.size} " +
                                "EVENT NAME=${previewProgram.title} INDEX=$index")
                        ReferenceApplication.applicationContext().contentResolver
                            .delete(
                                TvContractCompat.buildPreviewProgramUri(
                                    previewProgram.id
                                ), null, null
                            )
                    }

                    //delete old files
                    oldFiles?.forEach { File(recommendationImagePath, it).delete() }

                    isRecommendationInProgress = false
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkNextInsert: RECOMMENDATION COMPLETE")
                }
            }
        }

        listener.checkNextInsert()
    }

    interface ProgramInsertListener {
        fun checkNextInsert()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun insertProgram(
        view: View,
        tvChannel: com.iwedia.cltv.platform.model.TvChannel,
        channelId: Long,
        listener: ProgramInsertListener
    ) {
        moduleProvider.getEpgModule().getCurrentEvent(tvChannel, object : IAsyncDataCallback<TvEvent> {
            override fun onFailed(error: Error) {
                // we will skip adding if no current event is there
                listener.checkNextInsert()
            }

            override fun onReceive(event: TvEvent) {
                CoroutineHelper.runCoroutine({

                    val holder = RecommendationViewHolder(view)

                    buildThumbnailUri(holder, event, tvChannel, object : Listener {
                        @SuppressLint("RestrictedApi")
                        override fun onFinishLoading(uri: Uri?) {
                            var programIntent =
                                applicationContext.packageManager.getLaunchIntentForPackage(
                                    "com.iwedia.cltv"
                                )
                            if (programIntent == null) {
                                programIntent =
                                    applicationContext.packageManager.getLeanbackLaunchIntentForPackage(
                                        "com.iwedia.cltv"
                                    )
                            }
                            programIntent!!.putExtra("Channel_ID", tvChannel.id)

                            val builderProgram1 = PreviewProgram.Builder()
                            builderProgram1.setChannelId(channelId)
                                .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                                .setPosterArtUri(uri)
                                .setIntent(programIntent)
                                .setTitle(event.name)
                                .setDescription(holder.time!!.text.toString())

                            previewBuilderList.add(builderProgram1)
                            listener.checkNextInsert()
                        }
                    })
                })
            }
        })
    }


    interface Listener {
        fun onFinishLoading(uri: Uri?)
    }

    @SuppressLint("InflateParams")
    fun buildThumbnailUri(
        holder: RecommendationViewHolder,
        item: TvEvent,
        tvChannel: com.iwedia.cltv.platform.model.TvChannel,
        listener: Listener
    ) {
        loadImageChannel(tvChannel.logoImagePath,
            object : ImageLoadListenerChannel {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onResult(channelLogo: Bitmap?) {

                    ReferenceApplication.runOnUiThread(Runnable {

                        val width = Utils.getDimensInPixelSize(R.dimen.custom_dim_171_5)
                        val height = Utils.getDimensInPixelSize(R.dimen.custom_dim_96_5)

                        val view = holder.rootView!!

                        if (tvChannel.isLocked && moduleProvider.getTvInputModule()!!.isParentalEnabled()) {
                            holder.channelLockedIcon?.visibility = View.VISIBLE
                        } else {
                            holder.channelLockedIcon?.visibility = View.GONE
                        }
                        if (tvChannel.isSkipped) {
                            holder.channelSkipIcon?.visibility = View.VISIBLE
                        } else {
                            holder.channelSkipIcon?.visibility = View.GONE
                        }

                        var data = moduleProvider.getTimeModule().getCurrentTime(tvChannel)
                        loadImage(
                            item.imagePath!!,
                            holder.eventImage!!,
                            object : ImageLoadListener {
                                override fun onResult(isSuccess: Boolean) {

                                    try {
                                        holder.titlNoImage!!.setTextColor(
                                            Color.parseColor(
                                                ConfigColorManager.getColor("color_main_text")
                                            )
                                        )
                                        holder.channelName!!.setTextColor(
                                            Color.parseColor(
                                                ConfigColorManager.getColor("color_text_description")
                                            )
                                        )
                                        holder.channelLockedIcon!!.setColorFilter(
                                            Color.parseColor(
                                                ConfigColorManager.getColor("color_text_description")
                                            )
                                        )
                                        holder.channelSkipIcon!!.setColorFilter(
                                            Color.parseColor(
                                                ConfigColorManager.getColor("color_text_description")
                                            )
                                        )
                                    } catch (ex: java.lang.Exception) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResult: Exception color $ex")
                                    }



                                    holder.time!!.setDateTimeFormat(moduleProvider.getUtilsModule().getDateTimeFormat())
                                    holder.time!!.time = item as TvEvent


                                    //fonts if no images are loaded
                                    holder.titlNoImage!!.typeface =
                                        TypeFaceProvider.getTypeFace(
                                            ReferenceApplication.applicationContext(),
                                            ConfigFontManager.getFont("font_bold")
                                        )
                                    holder.channelName!!.typeface =
                                        TypeFaceProvider.getTypeFace(
                                            ReferenceApplication.applicationContext(),
                                            ConfigFontManager.getFont("font_regular")
                                        )

                                    //bind data
                                    holder.titlNoImage!!.text = item.name
                                    holder.channelName!!.text = tvChannel.name
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onResult: RECOMMENDATION CHANNEL ID: ${tvChannel.id}")
                                    if (channelLogo != null) {
                                        val bitmapRatio =
                                            channelLogo!!.width.toFloat() / channelLogo!!.height.toFloat()
                                        val maxHeight =
                                            Utils.getDimensInPixelSize(R.dimen.custom_dim_23)
                                        val calculatedWidth =
                                            (maxHeight * bitmapRatio).toInt()
                                        holder.channelLogo!!.setImageBitmap(
                                            Bitmap.createScaledBitmap(
                                                channelLogo!!,
                                                calculatedWidth,
                                                maxHeight,
                                                true
                                            )
                                        )
                                    } else {
                                        holder.channelLogo!!.setImageBitmap(null)
                                    }

                                    holder.channelName!!.text =
                                        if (channelLogo == null) tvChannel.name else ""

                                    //create bitmap
                                    view.measure(
                                        View.MeasureSpec.makeMeasureSpec(
                                            width,
                                            View.MeasureSpec.EXACTLY
                                        ),
                                        View.MeasureSpec.makeMeasureSpec(
                                            height,
                                            View.MeasureSpec.EXACTLY
                                        )
                                    )

                                    val b = Bitmap.createBitmap(
                                        view.getMeasuredWidth(),
                                        view.getMeasuredHeight(),
                                        Bitmap.Config.ARGB_8888
                                    )

                                    val c = Canvas(b)
                                    view.layout(
                                        0,
                                        0,
                                        view.getMeasuredWidth(),
                                        view.getMeasuredHeight()
                                    )
                                    view.draw(c)

                                    val imageFile = File(
                                        recommendationImagePath!!.path,
                                        "thumb_" + tvChannel.id + "_" + item.id + "_" + moduleProvider.getTimeModule().getCurrentTime(tvChannel) + ".png"
                                    )
                                    val fOut = FileOutputStream(imageFile)

                                    b.compress(Bitmap.CompressFormat.PNG, 85, fOut)
                                    fOut.flush()
                                    fOut.close()
                                    b.recycle()
                                    try {
                                        val uri: Uri = FileProvider.getUriForFile(
                                            applicationContext,
                                           "com.iwedia.cltv",
                                            imageFile
                                        )
                                        // try to get app list that have launcher screens
                                        val resInfoList: List<ResolveInfo> =
                                            applicationContext.packageManager.queryIntentActivities(
                                                Intent("android.intent.action.MAIN"),
                                                PackageManager.MATCH_DEFAULT_ONLY
                                            )

                                        for (resolveInfo in resInfoList) {
                                            resolveInfo
                                            val packageName: String =
                                                resolveInfo.activityInfo.packageName
                                            applicationContext.grantUriPermission(
                                                packageName,
                                                uri,
                                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        }

                                        listener.onFinishLoading(uri)
                                    } catch (e: Exception) {
                                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Exception  ${e.message}")
                                    }
                                }
                            })
                    })

                }
            })


    }

    interface ImageLoadListener {
        fun onResult(isSuccess: Boolean)
    }

    @SuppressLint("ResourceType")
    private fun loadImage(imagePath: String, view: ImageView, listener: ImageLoadListener) {
        CoroutineScope(Dispatchers.IO).launch {
            val logoImagePath = File(imagePath)
            if ((logoImagePath != null && logoImagePath.exists()) || imagePath.startsWith("http")) {
                val picasso = Picasso.get()
                picasso.load(imagePath).into(view, object : Callback {
                    @SuppressLint("RestrictedApi")
                    override fun onSuccess() {
                        Handler().postDelayed({
                            listener.onResult(true)
                        }, 500)

                    }

                    override fun onError(e: Exception?) {
                        e!!.printStackTrace()
                        listener.onResult(false)
                    }
                })
            } else if (imagePath.startsWith("R.raw.")) {
                view.setImageResource(R.raw.company_logo)
                listener.onResult(true)
            } else {
                val bitmap =
                    Utils.getChannelLogoBitmap(
                        ReferenceApplication.applicationContext(),
                        imagePath
                    )
                if (bitmap == null) {
                    listener.onResult(false)
                    return@launch
                }

                view.setImageBitmap(bitmap)
                listener.onResult(true)
            }
        }
    }

    interface ImageLoadListenerChannel {
        fun onResult(bitmap: Bitmap?)
    }

    @SuppressLint("ResourceType")
    private fun loadImageChannel(imagePath: String?, listener: ImageLoadListenerChannel) {
        if (imagePath == null) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "loadImageChannel: LOGOOO_PATH_IS_NULL")
            listener.onResult(null)
            return
        }
        val logoImagePath = File(imagePath)
        if ((logoImagePath != null && logoImagePath.exists()) || imagePath.startsWith("http")) {
            try {
                var bm = BitmapFactory.decodeFile(imagePath)
                listener.onResult(bm)
            } catch (e: Exception) {
                e.printStackTrace()
                listener.onResult(null)
            }
        } else if (imagePath.startsWith("R.raw.")) {
            listener.onResult(
                AppCompatResources.getDrawable(applicationContext, R.raw.company_logo)!!.toBitmap()
            )
        } else {
            val bitmap =
                Utils.getChannelLogoBitmap(
                    ReferenceApplication.applicationContext(),
                    imagePath
                )
            if (bitmap == null) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "loadImageChannel: LOGOOO_GET_BITMAP_FAILED")
                listener.onResult(null)
            } else {
                listener.onResult(bitmap)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    private fun updateRecommendationList() {
        // cancel if building recommendation already in progress
        if (isRecommendationInProgress) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateRecommendationList: RECOMMENDATION ALREADY IN PROGRESS")
            return
        }
        isRecommendationInProgress = true
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateRecommendationList: RECOMMENDATION STARTED")

        var cursor: Cursor? =null
        try {
            cursor = applicationContext.contentResolver.query(
                TvContractCompat.Channels.CONTENT_URI,
                PreviewChannel.Columns.PROJECTION,
                null,
                null,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        if (cursor != null && cursor.moveToFirst()) {
            do {
                if (cursor.getString(PreviewChannel.Columns.COL_PACKAGE_NAME).equals(packageName)) {

                    val id = cursor.getInt(PreviewChannel.Columns.COL_ID).toLong()
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateRecommendationList: RECOMMENDATION EXISTS")
                    updateRecommendationList(id)
                    cursor?.close()
                    return
                }
            } while (cursor.moveToNext())
        }
        cursor?.close()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateRecommendationList: RECOMMENDATION REMOVED")
        updateRecommendationList(null)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    private fun updateRecommendationList(channelId: Long?) {

        if (moduleProvider.getNetworkModule().networkStatus.value == NetworkData.NoConnection) return

        var channelId: Long? = channelId

        if (channelId == null) {

            val builderChannel = Channel.Builder()
            // Every channel you create must have the type TYPE_PREVIEW
            builderChannel.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(ConfigStringsManager.getStringById("app_name"))
                .setAppLinkIntentUri(null)

            channelUri = applicationContext.contentResolver.insert(
                TvContractCompat.Channels.CONTENT_URI,
                builderChannel.build().toContentValues()
            )
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateRecommendationList: CHANNEL URI $channelUri")
            channelId = ContentUris.parseId(channelUri!!)

            TvContractCompat.requestChannelBrowsable(applicationContext, channelId!!)

            val icon = AppCompatResources.getDrawable(applicationContext, R.drawable.app_logo)!!.toBitmap()

            storeChannelLogo(ReferenceApplication.applicationContext(), channelId!!, icon)
        }

        //Insert programs
        insertPrograms(channelId!!)

        startTimer()
    }

    /**
     * Start count down timer
     */
    private fun startTimer() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startTimer: START TIMER")
        //Cancel timer if it's already started
        if (channelRowTimer != null) {
            channelRowTimer!!.cancel()
            channelRowTimer = null
        }

        //Start new count down timer
        channelRowTimer = Timer("timer")

        channelRowTimer!!.schedule(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun run() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "run: ON RECOMMENDATION TIMER FINISHED")
                CoroutineHelper.runCoroutine({
                    updateRecommendationList()
                })
            }
        }, 300000)

    }

    /**
     * Show Recording Type Dialog
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun showRecordingTypeDialog() {
        if (BuildConfig.FLAVOR.contains("mtk") && !Utils.isUsbConnected()) {
            var title = ConfigStringsManager.getStringById("pvr_usb_error_msg")
            showErrorMessage(title)
            return
        }
        moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(activeChannel: TvChannel) {
                if (!moduleProvider.getPvrModule().isChannelRecordable(activeChannel)) {
                    var title = ConfigStringsManager.getStringById("pvr_msg_cannot_record_program")
                    showErrorMessage(title)
                } else {
                    val currentActiveScene = worldHandler.active
                    val sceneData = DialogSceneData(
                        currentActiveScene!!.id,
                        currentActiveScene.instanceId
                    )
                    ReferenceApplication.worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.CHOOSE_PVR_TYPE,
                        SceneManager.Action.SHOW_OVERLAY,
                        sceneData
                    )
                }
            }
        })
    }

    fun showErrorMessage(title: String) {
        CoroutineHelper.runCoroutine({
            val currentActiveScene = worldHandler.active
            val sceneData = DialogSceneData(
                currentActiveScene!!.id,
                currentActiveScene.instanceId
            )
            sceneData.type = DialogSceneData.DialogType.TEXT
            sceneData.title = title
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                }

                override fun onPositiveButtonClicked() {
                    worldHandler.destroySpecific(
                        worldHandler.active!!.id,
                        worldHandler.active!!.instanceId
                    )
                }
            }
            worldHandler.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )
        }, Dispatchers.Main)
    }

    /**
     * Checks the License expiry or not.
     */
    private fun startLicenseExpiryValidation() {
        if (Utils.isEvaluationFlavour()) {
            //start time is buildDate.
            var buildDate = Date(BuildConfig.TIMESTAMP)
            val cal = Calendar.getInstance()
            val currentDate = Date(cal.timeInMillis)

            val dateDiff = Utils.isEvaluationLicenceExpired(buildDate, currentDate)
            CoroutineHelper.runCoroutineWithDelay({
                if (dateDiff) {
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.EVAL_LICENSE_EXPIRY_SCENE,
                        SceneManager.Action.SHOW
                    )
                }
            }, 15000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleConflictClear(
        recording: ScheduledRecording,
        conflictRecordingAndWatchListItems: MutableList<TvEvent>,
        sceneData: DialogSceneData,
        currentActiveScene: SceneManager
    ) {

        val tvEventId = recording.tvEventId
        moduleProvider.getEpgModule().getEventById(tvEventId!!,
            object : IAsyncDataCallback<TvEvent> {
                override fun onFailed(error: Error) {}

                override fun onReceive(data: TvEvent) {
                    runOnUiThread {
                        sceneData.type = DialogSceneData.DialogType.SCHEDULER
                        if (data.id == -1) {//if custom recording
                            sceneData.title =
                                ConfigStringsManager.getStringById("scheduled_recording_reminder_title")
                        } else {
                            sceneData.title = data.name
                        }
                        sceneData.imageRes = R.drawable.recording_indicator
                        val message =
                            ConfigStringsManager.getStringById("scheduled_recording_reminder_message")
                        sceneData.message = message.replace("%s", data.tvChannel.name)
                        sceneData.subMessage =
                            ConfigStringsManager.getStringById("time_left_reminder_message")
                        sceneData.imageRes
                        sceneData.positiveButtonText =
                            ConfigStringsManager.getStringById("record")
                        sceneData.negativeButtonText =
                            ConfigStringsManager.getStringById("cancel")
                        sceneData.dialogClickListener =
                            object : DialogSceneData.DialogClickListener {
                                override fun onNegativeButtonClicked() {
                                    runOnUiThread {
                                        worldHandler.triggerAction(
                                            currentActiveScene.id,
                                            SceneManager.Action.SHOW
                                        )
                                    }
                                }

                                @RequiresApi(Build.VERSION_CODES.R)
                                override fun onPositiveButtonClicked() {
                                    Log.i(TAG, "onPositiveButtonClicked: ${data.tvChannel}")

                                    if (moduleProvider.getTimeshiftModule().isTimeShiftActive) {
                                        moduleProvider.getTimeshiftModule()
                                            .timeShiftStop(object : IAsyncCallback {
                                                override fun onFailed(error: Error) {}
                                                override fun onSuccess() {}
                                            })
                                        worldHandler.destroyOtherExisting(
                                            ReferenceWorldHandler.SceneId.LIVE
                                        )
                                        runOnUiThread {
                                            showToast(ConfigStringsManager.getStringById("time_shift_stop_toast"))
                                        }
                                    }
                                    data.let {
                                        moduleProvider.getTvModule()
                                            .changeChannel(it.tvChannel, object : IAsyncCallback {
                                                override fun onFailed(error: Error) {
                                                    Log.i(
                                                        TAG,
                                                        "onFailed: Failed to change channel"
                                                    )
                                                    showToast("Failed to change channel")
                                                    worldHandler.triggerActionWithData(
                                                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                        SceneManager.Action.DESTROY, sceneData
                                                    )
                                                }

                                                @RequiresApi(Build.VERSION_CODES.R)
                                                override fun onSuccess() {

                                                    Log.i(
                                                        TAG,
                                                        "onSuccess: Success changed channel"
                                                    )
                                                    runOnUiThread {
                                                        worldHandler.triggerActionWithData(
                                                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                                            SceneManager.Action.DESTROY,
                                                            sceneData
                                                        )
                                                    }
                                                    startRecording(data)
                                                }
                                            })
                                    }
                                }
                            }

                        if (!moduleProvider.getUtilsModule().isUsbConnected()) {
                            runOnUiThread {
                                showToast(ConfigStringsManager.getStringById("pvr_usb_error_msg_scheduled"), UtilsInterface.ToastDuration.LENGTH_LONG)
                            }
                        } else {
                            runOnUiThread {
                                if (currentActiveScene.id != ReferenceWorldHandler.SceneId.LIVE) {
                                    runOnUiThread {
                                        worldHandler.triggerAction(
                                            currentActiveScene.id, SceneManager.Action.HIDE
                                        )
                                    }
                                }
                                worldHandler.triggerActionWithData(
                                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                    SceneManager.Action.SHOW_OVERLAY, sceneData
                                )
                            }
                        }
                    }
                    conflictRecordingAndWatchListItems.clear()
                }
            })
    }


    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadScheduleRecordingNotification(event: Event) {
        Log.w(TAG, "loadScheduleRecordingNotification() -> isInitalized = ${ReferenceApplication.isInitalized}")
        if(!ReferenceApplication.isInitalized) return

        val recording = event.getData(0) as ScheduledRecording
        val currentTime = moduleProvider.getTimeModule().getCurrentTime(recording.tvChannel!!)
        val timeRemaining = recording.scheduledDateStart - currentTime

        if (event.getData(0) != null && event.getData(0) is ScheduledRecording) {
            val tvEventId = (event.getData(0) as ScheduledRecording).tvEventId
            moduleProvider.getEpgModule()
                .getEventById(tvEventId!!, object : IAsyncDataCallback<TvEvent> {
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: error == ${error.message}")
                    }

                    override fun onReceive(data: TvEvent) {
                        if (!conflictRecordingAndWatchListItems.contains(data)) {
                            conflictRecordingAndWatchListItems.add(data)
                        }
                        Log.d(Constants.LogTag.CLTV_TAG +
                            TAG,
                            "onReceive: conflictRecordingAndWatchListItems == ${conflictRecordingAndWatchListItems.size}"
                        )
                    }
                })
        }

        if (conflictRecordingAndWatchListItems.size > 1) {
            runOnUiThread {
                if (worldHandler.active!!.id != ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE) {
                    val currentActiveScene = ReferenceApplication.worldHandler!!.active
                    val sceneData =
                        RecordingWatchlistConflictSceneData(
                            currentActiveScene!!.id,
                            currentActiveScene.instanceId
                        )
                    sceneData.listOfConflictedTvEvents = conflictRecordingAndWatchListItems
                    sceneData.eventSelectedClickListener =
                        object : RecordingWatchlistConflictSceneData.EventSelectedClickListener {
                            override fun onEventSelected() {
                                conflictRecordingAndWatchListItems.clear()
                            }
                        }
                    worldHandler.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE,
                        SceneManager.Action.SHOW_OVERLAY, sceneData
                    )
                }
            }
            //delete Conflicted watchlist events
            for (i in 1 until conflictRecordingAndWatchListItems.size) {
                val item = conflictRecordingAndWatchListItems[i]
                val scheduleReminder = ScheduledReminder(
                    item.id, item.name, tvChannelId = item.tvChannel.id,
                    startTime = item.startTime, tvEventId = item.id
                )

                moduleProvider.getWatchlistModule().removeScheduledReminder(scheduleReminder,
                    object: IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { }
                    })
            }
        }
        else {
            conflictRecordingAndWatchListItems.clear()
            val currentActiveScene = ReferenceApplication.worldHandler!!.active
            val sceneData = DialogSceneData(
                currentActiveScene!!.id,
                currentActiveScene.instanceId,
                timeRemaining
            )
            handleConflictClear(recording, conflictRecordingAndWatchListItems, sceneData, currentActiveScene)
        }
    }

    /**
     * Responsible to handle conflict between recordings and watchList events, during 1min recording conflict PopUp.
     * Also attempt to delete all watchlist from DB
     *
     * @param event   ScheduledRecording Event
     * */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun loadScheduledRecordingReminder(event: Event){
        Log.w(TAG, "loadScheduledRecordingReminder() -> isInitalized = ${ReferenceApplication.isInitalized}")
        if(!ReferenceApplication.isInitalized) return

        val recording = event.data?.get(0) as ScheduledRecording
        val conflictRecordingAndWatchListItems = mutableListOf<TvEvent>()
        moduleProvider.getEpgModule()
            .getEventByNameAndStartTime(recording.name, recording.scheduledDateStart, recording.tvChannelId,
                object : IAsyncDataCallback<TvEvent> {
                    override fun onFailed(error: Error) {}

                    override fun onReceive(data: TvEvent) {
                        conflictRecordingAndWatchListItems.add(0, data)
                    }

            })
        moduleProvider.getWatchlistModule().getWatchList(object: IAsyncDataCallback<MutableList<ScheduledReminder>>{
            override fun onFailed(error: Error) {}

            override fun onReceive(data: MutableList<ScheduledReminder>) {
                // Start time to minutes
                val recStartTime = (recording.scheduledDateStart) / 60000
                var remStartTime: Long

                data.forEach {
                    remStartTime = (it.startTime!!) / 60000

                    if (remStartTime == recStartTime) {
                        moduleProvider.getEpgModule()
                            .getEventByNameAndStartTime(it.name, it.startTime, it.tvChannelId!!, object : IAsyncDataCallback<TvEvent> {
                                override fun onFailed(error: Error) {}

                                override fun onReceive(data: TvEvent) {
                                    conflictRecordingAndWatchListItems.add(data)
                                }
                            })
                    }

                }
            }
        })

        val currentActiveScene = ReferenceApplication.worldHandler!!.active
        runOnUiThread {
           CoroutineHelper.runCoroutineWithDelay({
               //delete Conflicted watchlist events
               for(i in 1 until conflictRecordingAndWatchListItems.size){
                   val item = conflictRecordingAndWatchListItems[i]
                   val reminder = ScheduledReminder(
                       item.id, item.name, item.tvChannel, item, item.startTime, item.tvChannel.id, item.id)

                   moduleProvider.getWatchlistModule().removeScheduledReminder(reminder,
                       object: IAsyncCallback{
                           override fun onFailed(error: Error) {
                               Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "removeScheduledReminder failed : ${error.message}")
                           }

                           override fun onSuccess() {
                               Log.i(TAG, "removeScheduledReminder success")
                           }
                       })
               }

               if (worldHandler.active!!.id != ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE) {
                   val sceneData =
                       RecordingWatchlistConflictSceneData(
                           currentActiveScene!!.id,
                           currentActiveScene.instanceId
                       )

                   sceneData.listOfConflictedTvEvents = conflictRecordingAndWatchListItems
                   sceneData.eventSelectedClickListener =
                       object : RecordingWatchlistConflictSceneData.EventSelectedClickListener {
                           override fun onEventSelected() {
                               conflictRecordingAndWatchListItems.clear()
                           }
                       }
                   worldHandler.triggerActionWithData(
                       ReferenceWorldHandler.SceneId.RECORDING_WATCHLIST_CONFLICT_SCENE,
                       SceneManager.Action.SHOW_OVERLAY, sceneData
                   )
               }
           }, 200)
        }
    }

    fun stopInactivityTimer() {
        if (inactivityTimer.timer != null){
            inactivityTimer.stopTimer()
        }
    }

    //set values for no signal power off for mtk
    @RequiresApi(Build.VERSION_CODES.R)
    fun checkNoSignalPowerOff() {
        if (!((worldHandler) as ReferenceWorldHandler).isFastOnly()) {
            val noSignalPowerOffTimeList: List<String> = listOf(
                "duration_5_minutes",
                "duration_10_minutes",
                "duration_15_minutes",
                "duration_30_minutes",
                "duration_60_minutes"
            )
            var noSignalPowerOffEnabled: Any?
            var defaultTimeNoSignalPowerOff: Any?

            //if there is nothing in shared pref regarding no signal power off -> FTI: check database values and copy them to shared pref
            var text = moduleProvider.getUtilsModule()
                .getPrefsValue("no_signal_auto_power_off", "") as String
            if (text.isEmpty()) {
                if (moduleProvider.getUtilsModule().noSignalPowerOffEnabledOTA() != null) {
                    if (moduleProvider.getUtilsModule().noSignalPowerOffEnabledOTA() is Boolean) {
                        noSignalPowerOffEnabled =
                            moduleProvider.getUtilsModule().noSignalPowerOffEnabledOTA()
                        moduleProvider.getUtilsModule()
                            .noSignalPowerOffChanged(noSignalPowerOffEnabled!! as Boolean)
                    }
                }
                if (moduleProvider.getUtilsModule().noSignalPowerOffTimeOTA() != null) {
                    if (moduleProvider.getUtilsModule().noSignalPowerOffTimeOTA() is Int) {
                        defaultTimeNoSignalPowerOff =
                            moduleProvider.getUtilsModule().noSignalPowerOffTimeOTA()
                        if((defaultTimeNoSignalPowerOff as Int) > -1) {
                            moduleProvider.getUtilsModule().setPowerOffTime(
                                defaultTimeNoSignalPowerOff,
                                noSignalPowerOffTimeList[defaultTimeNoSignalPowerOff]
                            )
                        }
                    }
                }
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun refreshNoSignalPowerOff() {
        var applicationMode =
            if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT

        val isNoSignalPowerOffEnabled = moduleProvider.getUtilsModule().getIsPowerOffEnabled()
        val preferredDuration = moduleProvider.getUtilsModule().getPowerOffTime()

        try {
            moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }
                override fun onReceive(data: TvChannel) {
                    //timer for no signal atsc channels
                    if (!data.isFastChannel()) {
                        if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.INTRO && worldHandler.active?.id != ReferenceWorldHandler.SceneId.WALKTHROUGH && (ReferenceApplication.isInForeground || ReferenceApplication.isOkClickedOnSetUp)) {
                            if (isNoSignalPowerOffEnabled) {
                                if (inputSelected == "TV") {
                                    if (!moduleProvider.getTvModule().isSignalAvailable() || !moduleProvider.getTvModule().isChannelsAvailable()) {
                                        ReferenceApplication.runOnUiThread {
                                            inactivityTimer.startTimer(preferredDuration)
                                        }
                                    }
                                } else if (inputSelected.contains("HDMI") || inputSelected.contains("Composite")) {
                                    if(!isHdmiCompVideoAvailable!!) {
                                        ReferenceApplication.runOnUiThread {
                                            inactivityTimer.startTimer(preferredDuration)
                                        }
                                    }
                                }
                            } else {
                                stopInactivityTimer()
                            }
                        }
                    }
                    //stop timer for fast channels
                    else {
//                    stopInactivityTimer()
                        if (isNoSignalPowerOffEnabled) {
                            if (moduleProvider.getNetworkModule().networkStatus.value == NetworkData.NoConnection) {
                                ReferenceApplication.runOnUiThread {
                                    inactivityTimer.startTimer(preferredDuration)
                                }
                            }
                        } else {
                            stopInactivityTimer()
                        }
                    }
                }
            }, applicationMode)
        } catch(e : Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun registerNetworkObserver() {
        moduleProvider.getNetworkModule().anokiServerStatus.observe(
            this
        ) { anokiStatus->
            if (anokiStatus) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Connection to provider OK starting fast scan $tosCanceled")
                if (tosCanceled) {
                    return@observe
                }
                moduleProvider.getFastUserSettingsModule().checkTos(object: IAsyncDataCallback<Boolean>{
                    override fun onFailed(error: Error) {}

                    override fun onReceive(data: Boolean) {
                        if(data) {
                            if (channelsLoaded && moduleProvider.getTvModule().getChannelList(ApplicationMode.FAST_ONLY).size == 0) {
                                val intent = Intent(ReferenceApplication.FAST_SCAN_START)
                                applicationContext.sendBroadcast(intent)
                            }
                        }else {
                            startTosActivity()
                        }
                    }
                })
            }
        }
        moduleProvider.getNetworkModule().networkStatus.observeForever(
            object : Observer<NetworkData> {
                override fun onChanged(networkStatusData: NetworkData) {
                    refreshNoSignalPowerOff()
                    if (ReferenceApplication.isInForeground) {

                        if(ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.VOD) {
                            // No internet state for VOD specific screens is handled internally in ReferenceSceneManager used for VOD scenes.
                            return
                        }

                        if (((worldHandler) as ReferenceWorldHandler).isFastOnly()) {
                            if (networkStatusData == NetworkData.NoConnection) {
                                showNoInternetDialog()
                            } else {
                                destroyNoInternetDialog()
                                if ((worldHandler?.isVisible(ReferenceWorldHandler.SceneId.INTRO) == false) &&
                                    (worldHandler?.isHidden(ReferenceWorldHandler.SceneId.INTRO) == false) && !ReferenceApplication.parentalControlDeepLink) {
                                    //Resume live playback
                                    moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                                        override fun onFailed(error: Error) {
                                        }

                                        override fun onReceive(data: TvChannel) {
                                            moduleProvider.getTvModule().changeChannel(data, object : IAsyncCallback {
                                                override fun onFailed(error: Error) {
                                                }

                                                override fun onSuccess() {
                                                }
                                            })
                                        }
                                    })
                                }
                            }
                        } else if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) {
                            if ((worldHandler?.isVisible(ReferenceWorldHandler.SceneId.INTRO) == false) &&
                                (worldHandler?.isHidden(ReferenceWorldHandler.SceneId.INTRO) == false) && !ReferenceApplication.parentalControlDeepLink) {
                                //Resume live playback
                                moduleProvider.getTvModule().getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                                    override fun onFailed(error: Error) {
                                    }

                                    override fun onReceive(data: TvChannel) {
                                        moduleProvider.getTvModule().changeChannel(data, object : IAsyncCallback {
                                            override fun onFailed(error: Error) {
                                            }

                                            override fun onSuccess() {
                                            }
                                        }, ApplicationMode.FAST_ONLY)
                                    }
                                }, ApplicationMode.FAST_ONLY)
                            }
                        }
                    }
                }
            })
    }

    fun destroyNoInternetDialog() {
        worldHandler.triggerAction(ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE, SceneManager.Action.DESTROY)
    }

    fun showNoInternetDialog() {

        if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE) {
            // if dialog is already shown exit the method
            return
        }

        val sceneData = DialogSceneData(-1, -1)

        sceneData.apply {
            type = DialogSceneData.DialogType.TEXT
            title = ConfigStringsManager.getStringById("no_internet_connection")
            message = ConfigStringsManager.getStringById("please_try_to_reconnect")
            //positiveButtonText = ConfigStringsManager.getStringById("Retry")
            positiveButtonEnabled = false
            imageRes = R.drawable.no_internet_icon
            isBackEnabled=false
            dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onPositiveButtonClicked() {

                }

                override fun onNegativeButtonClicked() {
                }

            }
        }

        //if vod is playing do not destroy vod banner scene, because that is the way to resume vod playback when internet is reconnected
        if(!(worldHandler as ReferenceWorldHandler).isVodScene() && !HomeSceneManager.IS_VOD_ACTIVE) {
            ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
        }
        ReferenceApplication.worldHandler?.triggerActionWithData(
            ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE,
            SceneManager.Action.SHOW_OVERLAY, sceneData
        )
    }

    fun startSceneInactivityTimer() {
        stopSceneInactivityTimer()
        val activeScene = ReferenceApplication.worldHandler!!.active
        var duration = 60000L
        //if current scene is info banner it should close in 10sec of inactivity.
        if (activeScene?.scene?.id == ReferenceWorldHandler.SceneId.INFO_BANNER){
            duration = 10000L
        }
        sceneInactivityTimer!!.startTimer(duration)
    }

    fun stopSceneInactivityTimer() {
        sceneInactivityTimer!!.stopTimer()
    }

    fun sceneInactivityTimerEnd() {
        sceneInactivityTimer!!.stopTimer()
        if (worldHandler.active!!.id == ReferenceWorldHandler.SceneId.INFO_BANNER) {
            worldHandler.triggerAction(
                ReferenceWorldHandler.SceneId.INFO_BANNER,
                SceneManager.Action.DESTROY
            )
        } else if (worldHandler.active!!.id == ReferenceWorldHandler.SceneId.CHANNEL_SCENE) {
            worldHandler.triggerAction(
                ReferenceWorldHandler.SceneId.CHANNEL_SCENE,
                SceneManager.Action.DESTROY
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun resumePlayer() {

        //if(moduleProvider.getTvModule().getChannelList().size == 0) {
        //    moduleProvider.getTvModule().forceChannelsRefresh()
        //}
        moduleProvider.getTvModule().getActiveChannel(object :
            IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                try {
                    var activePlayerItem = moduleProvider.getPlayerModule().activePlayableItem
                    if((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) {
                        if (activePlayerItem is TvChannel) {
                            if (activePlayerItem.isFastChannel()) {
                                resumePlayback()
                            }
                        }
                    } else {
                        if(moduleProvider.getTvModule().getChannelList().size == 0) {
                            CoroutineHelper.runCoroutineWithDelay({
                                val intent = Intent(GlobalAppReceiver.GLOBAL_KEY_INTENT_ACTION)
                                intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_ACTION, 1)
                                intent.putExtra(GlobalAppReceiver.GLOBAL_KEY_CODE, 172)
                                sendBroadcast(intent)
                            },500)
                        }
                    }
                } catch(e: Exception) {

                }

            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(data: TvChannel) {
                if (data.isLocked && moduleProvider.getTvInputModule().isParentalEnabled()
                    && ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                ) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: locked")
                    resumePlayback()
                    if(!moduleProvider.getPlayerModule().isChannelUnlocked){
                        InformationBus.submitEvent(Event(Events.ACTIVE_CHANNEL_LOCKED_EVENT))
                        moduleProvider.getPlayerModule().mute()
                    }
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: unlocked")
                    if (newChannelsFoundResetApp) {
                        newChannelsFoundResetApp = false
                        CoroutineHelper.runCoroutineWithDelay({
                            Utils.restartApp()
                        }, 10)
                        return
                    }
                    if (ReferenceApplication.isScanChannelsShowed) {
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            SceneManager.Action.DESTROY
                        )
                    }
                    InformationBus.submitEvent(Event(Events.ACTIVE_CHANNEL_UNLOCKED_EVENT))

                        if (worldHandler.active?.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE) {
                            try {
                                var activePlayerItem =
                                    moduleProvider.getPlayerModule().activePlayableItem

                                if (activePlayerItem is TvChannel) {
                                    var activeItemExists = false
                                    for (channel in moduleProvider.getTvModule().getChannelList()) {
                                        if (channel.id == activePlayerItem.id) {
                                            activeItemExists = true
                                        }
                                    }
                                    if (!activeItemExists && !activePlayerItem.isFastChannel() && (activePlayerItem.id != data.id)) {
                                        Log.d(Constants.LogTag.CLTV_TAG +
                                            TAG,
                                            "Invalid active channel running expect ${data.name} got ${activePlayerItem.name}, recovering"
                                        )
                                        moduleProvider.getPlayerModule().play(data)
                                    } else {
                                        resumePlayback()
                                    }
                                } else {
                                    resumePlayback()
                                }
                            } catch(e: Exception) {

                            }
                        }
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun exitApplication() {
        stopInactivityTimer()
        stopBackgroundExitTimer()
        exitTvInput()
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"exitApplication called")
        moduleProvider.getUtilsModule().clearChannelidsPref()
        stopPlayback()
        ReferenceApplication.getActivity().finish()
        moduleProvider.getInputSourceMoudle().onApplicationStop()
        moduleProvider.getUtilsModule().setApplicationRunningInBackground(false)
        Process.killProcess(Process.myPid())
        if (!isServiceRunning(this, ContentAggregatorService::class.java)) {
            startService(this, ContentAggregatorService::class.java)
        }
    }

    @SuppressLint("NewApi")
    private fun checkRecordingScheduleConflict(recording: ScheduledRecording) {

        moduleProvider.getTvModule().getActiveChannel(object: IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {}

            override fun onReceive(tvChannel: TvChannel) {
                moduleProvider.getEpgModule().getCurrentEvent(tvChannel, object : IAsyncDataCallback<TvEvent> {
                    override fun onFailed(error: Error) {
                        onReceive(TvEvent.createNoInformationEvent( tvChannel, moduleProvider.getTimeModule().getCurrentTime(tvChannel)))
                    }
                    override fun onReceive(tvEvent: TvEvent) {
                        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.RECORDING){
                            if (recording.scheduledDateStart < (tvEvent.endTime) + CONFLICT_OFFSET){
                                updateScheduleRecordingConflict(recording, tvEvent )
                            }
                        }
                        else {
                            if (!moduleProvider.getSchedulerModule().isInReclist(recording.tvEvent!!.tvChannel.id, recording.scheduledDateStart)) {
                                    val conflictedRecordings =
                                        moduleProvider.getSchedulerModule().findConflictedRecordings(recording)
                                    if (conflictedRecordings?.isNotEmpty() == true) {
                                        val currentActiveScene =
                                            ReferenceApplication.worldHandler!!.active
                                        val sceneData = RecordingConflictSceneData(
                                            currentActiveScene!!.id,
                                            currentActiveScene.instanceId
                                        )
                                        sceneData.type =
                                            RecordingConflictSceneData.RecordingConflictType.SCHEDULE_SCHEDULE
                                        sceneData.secondSchedule = recording
                                        sceneData.firstSchedule = conflictedRecordings
                                        sceneData.firstItemText = ""
                                        sceneData.secondItemText = ""
                                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                                            ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,
                                            SceneManager.Action.SHOW, sceneData
                                        )
                                        return
                                    }

                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "startRecordingScheduleEvent")
                                    moduleProvider.getSchedulerModule().scheduleRecording(recording,
                                        object :
                                            IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult> {
                                            override fun onFailed(error: Error) {
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleRecording onFailed")
                                            }

                                            @RequiresApi(Build.VERSION_CODES.R)
                                            override fun onReceive(data: SchedulerInterface.ScheduleRecordingResult) {
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: ${recording.name}")
                                            }
                                        })
                            } else  Log.d(Constants.LogTag.CLTV_TAG + TAG, "Recording already scheduled")
                        }
                    }
                })
            }
        })
    }
    @SuppressLint("NewApi")
    private fun updateScheduleRecordingConflict(
        recording: ScheduledRecording, activeRecordingEvent: TvEvent
    ) {
        val currentActiveScene = ReferenceApplication.worldHandler!!.active
        val sceneData =
            RecordingConflictSceneData(currentActiveScene!!.id, currentActiveScene.instanceId)
        sceneData.type = RecordingConflictSceneData.RecordingConflictType.RECORDING_SCHEDULE
        sceneData.title = ConfigStringsManager.getStringById("description_text")
        sceneData.firstRecording = activeRecordingEvent
        sceneData.secondSchedule = recording
        sceneData.firstItemText = getEventName(activeRecordingEvent)
        sceneData.secondItemText = getEventName(recording.tvEvent)

        worldHandler.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
        worldHandler.triggerActionWithData(
            ReferenceWorldHandler.SceneId.RECORDING_CONFLICT_SCENE,
            SceneManager.Action.SHOW, sceneData
        )
    }

    private fun getEventName(data: Any?): String {
        if (data is TvEvent?) {
            return if (data!!.name == "No Information") {
                data.tvChannel.name
            } else {
                data.tvChannel.name + " - " + data.name
            }
        } else if (data is MutableList<*>) {
            var channelNames = ""
            data.forEach {
                if (it is ScheduledRecording) {
                    val tvEvent = it.tvEvent
                    if (channelNames.isNotEmpty()) channelNames += ", "
                    channelNames += it.tvChannel?.name
                    if (tvEvent!!.name != "No Information") channelNames += (" - " + tvEvent.name)
                }
            }
            return channelNames
        }
        return ""
    }

    private fun startTosActivity(){
        //Start tos activity
        if (inputSelected == "TV" && !ReferenceApplication.isCecTuneFromIntro) {
            val intent =
                Intent(applicationContext, TermsOfServiceActivity::class.java)
            intent.putExtra("from_main_activity", true)
            startActivityForResult(intent, TOS_ACTIVITY_REQUEST_CODE)
        }
    }

    private fun showNewChannelsFoundPopup(){
        runOnUiThread{
            val currentActiveScene = worldHandler.active
            val sceneData = DialogSceneData(
                currentActiveScene!!.id,
                currentActiveScene.instanceId
            )
            sceneData.type = DialogSceneData.DialogType.TEXT
            sceneData.title = ConfigStringsManager.getStringById("channel_changed_popup")
            sceneData.message = ConfigStringsManager.getStringById("channel_changed_popup_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {}
                override fun onPositiveButtonClicked() {
                    worldHandler.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY)
                }
            }
            worldHandler.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    /**
     * Starts a background timer to exit the application after a specified duration of being in the background.
     */
    private fun startBackgroundExitTimer() {
        //Stop existing timer
        stopBackgroundExitTimer()
        //Start new timer
        backgroundExitTimer = Timer()

        backgroundExitTimer!!.schedule(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun run() {
                // Exit the App if PVR is not in progress
                if (!moduleProvider.getPvrModule().isRecordingInProgress()) {
                    exitApplication()
                }
            }
        }, APP_BACKGROUND_TIME_LIMIT)
    }

    /**
     * Stops the background timer that automatically exits the application after a specified duration
     * of being in the background.
     */
    private fun stopBackgroundExitTimer() {
        backgroundExitTimer?.cancel()
        backgroundExitTimer = null
    }


    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (::moduleProvider.isInitialized) {
                moduleProvider.getUtilsModule().showToast(text, duration)
            }
        }
    }
    //Show parental control settings in Home screen preferences menu
    private fun showParentalControlSettings() {
        moduleProvider.getPlayerModule().stop()
        runOnUiThread {
            worldHandler.destroyOtherExisting(
                ReferenceWorldHandler.SceneId.LIVE
            )
            val sceneData = HomeSceneData(1, 1, 3)
            sceneData?.initialFilterPosition = 3

            worldHandler.triggerActionWithData(
                ReferenceWorldHandler.SceneId.HOME_SCENE,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )
            ReferenceApplication.parentalControlDeepLink = true
        }
    }

    /**
     * Resume the playback if the current playback state is not VOD (Video on Demand).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun resumePlayback() {
        if (ReferenceApplication.worldHandler!!.playbackState != PlaybackState.VOD) {
            moduleProvider.getPlayerModule().resume()
        }
    }

    /**
     * Stops the playback if the current playback state is not VOD (Video on Demand).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun stopPlayback() {
        if (ReferenceApplication.worldHandler!!.playbackState != PlaybackState.VOD) {
            moduleProvider.getPlayerModule().stop()
        }
    }


    private fun showDeviceInfo() {
        val currentActiveScene = worldHandler.active
        if(currentActiveScene is HomeSceneManager) {
            ReferenceApplication.worldHandler?.triggerAction(
                ReferenceWorldHandler.SceneId.HOME_SCENE,
                SceneManager.Action.DESTROY
            )
        }
        val sceneData = HomeSceneData(currentActiveScene!!.id, currentActiveScene.instanceId)
        sceneData.initialFilterPosition = 3
        sceneData.openDeviceInfo = true

        ReferenceApplication.worldHandler!!.destroyOtherExistingList(GList<Int>().apply {
            add(ReferenceWorldHandler.SceneId.LIVE)
            add(currentActiveScene.id)
        })

        ReferenceApplication.worldHandler?.triggerActionWithData(
            ReferenceWorldHandler.SceneId.HOME_SCENE,
            SceneManager.Action.SHOW_OVERLAY,
            sceneData
        )
    }
}

