package com.iwedia.cltv.receiver

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.media.AudioManager
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import com.iwedia.cltv.*
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.ReferenceApplication.Companion.applicationContext
import com.iwedia.cltv.ReferenceApplication.Companion.getActivity
import com.iwedia.cltv.ReferenceApplication.Companion.worldHandler
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.manager.HomeSceneManager
import com.iwedia.cltv.manager.LiveManager
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.`interface`.ClosedCaptionInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.scene.home_scene.HomeScene
import com.iwedia.cltv.scene.home_scene.HomeSceneBase
import com.iwedia.cltv.scene.home_scene.HomeSceneData
import com.iwedia.cltv.scene.live_scene.LiveScene
import com.iwedia.cltv.utils.ChannelImageData
import com.iwedia.guide.android.tools.GAndroidPrefsHandler
import data_type.GList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import org.json.JSONObject
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData
import world.SceneManager


/**
 * Global app receiver
 *
 * @author Dejan Nadj
 */
@SuppressLint("UnspecifiedRegisterReceiverFlag")
@RequiresApi(Build.VERSION_CODES.R)
class GlobalAppReceiver : BroadcastReceiver() {

    private val TAG = javaClass.simpleName
    private val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    companion object {
        const val GLOBAL_KEY_INTENT_ACTION = "com.iwedia.cltv.intent.action.GLOBAL_BUTTON"
        const val INTENT_INPUT_SOURCE =
            "com.iwedia.cltv.intent.action.INPUT_BUTTON"
        const val STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION"
        const val ANDROID_GLOBAL_BUTTON_ACTION = "android.intent.action.GLOBAL_BUTTON"
        const val GLOBAL_KEY_CODE = "keyCode"
        const val GLOBAL_KEY_ACTION = "keyAction"
        const val SCAN_COMPLETED_INTENT_ACTION = "scan_completed_sync_databases"
        const val THIRD_PARTY_CHANNEL_START_POSITON = 10001
        const val SETTINGS_OPENED_INTENT_ACTION = "com.iwedia.SETTINGS_STARTED"
        const val SETTINGS_STOPPED_INTENT_ACTION = "com.iwedia.SETTINGS_STOPPED"
        const val PVR_STATUS_CHANGED_INTENT_ACTION = "com.iwedia.PVR_STATUS_CHANGED"
        const val EXIT_APPLICATION_ON_SCAN = "exit_application_on_scan"
        const val TV_KEYCODE_INTENT_ACTION = "com.iwedia.cltv.action.tv_key_received"

        const val FACTORY_ENTRY_CLASS_NAME = "jp.funai.android.tv.factory.ui.EntryScreen"
        private const val FACTORY_PACKAGE_NAME = "jp.funai.android.tv.factory"
        private const val FFACTORY_INITIAL_CLASS_NAME = ".ui.InitialScreen"
        const val INPUT_CHANGE_ACTION = "android.intent.input_tune"


        const val SCRAMBLED_MASK = 0x8
        var activeSceneId = 0
        const val INPUT_SOURCE_BLUETOOTH = -2130705647
        var numInserted: Int? = null
        private var moduleProvider: ModuleProvider ?= null
        private var utilsModule: UtilsInterface? = null
        private var closedCaptionModule: ClosedCaptionInterface? = null

        fun setModuleProvider(moduleProvider: ModuleProvider) {
            this.moduleProvider = moduleProvider
            utilsModule = this.moduleProvider!!.getUtilsModule()
            closedCaptionModule = this.moduleProvider!!.getClosedCaptionModule()
        }
    }

    /**
     * Tv db channel list update timer
     * To detect when tv db channel list is updated
     */
    var channelListUpdateTimer: CountDownTimer? = null
    var display_number_third_party = THIRD_PARTY_CHANNEL_START_POSITON
    private var pvrStatusChanged = false

    init {
        if (!BuildConfig.FLAVOR.contains("base") && !BuildConfig.FLAVOR.contains("mal_service") && !BuildConfig.FLAVOR.contains("mk5")
            && !BuildConfig.FLAVOR.contains("refplus5") && !BuildConfig.FLAVOR.contains("rtk") && !BuildConfig.FLAVOR.contains("t56")) {
            var context = ReferenceApplication.applicationContext()
            var cursor = context.contentResolver.query(
                ContentProvider.CHANNELS_URI,
                null,
                null,
                null,
                null
            )

            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "init cursor size ${cursor?.count}")
            if (cursor!!.count == 0) {
                Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "init channels database is empty")
                initDatabase()
            }

            var channelListObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    startUpdateTimer()
                }
            }

            context.contentResolver.registerContentObserver(
                TvContract.Channels.CONTENT_URI,
                true,
                channelListObserver
            )

            val intentFilter = IntentFilter(EXIT_APPLICATION_ON_SCAN)
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (EXIT_APPLICATION_ON_SCAN == intent!!.action) {
                        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ","Received EXIT_APPLICATION_ON_SCAN")
                        InformationBus.submitEvent(Event(Events.EXIT_APPLICATION_ON_SCAN))
                    }
                }
            }, intentFilter)

        }
    }

    /**
     * Stop channel list udpate timer if it is already started
     */
    @Synchronized
    private fun stopUpdateTimer() {
        if (channelListUpdateTimer != null) {
            channelListUpdateTimer!!.cancel()
            channelListUpdateTimer = null
        }
    }

    /**
     * Start channel list update timer
     */
    @Synchronized
    private fun startUpdateTimer() {
        //Cancel timer if it's already started
        stopUpdateTimer()

        //Start new count down timer
        channelListUpdateTimer = object :
            CountDownTimer(
                2000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "update channel list")
                initDatabase()
            }
        }
        channelListUpdateTimer!!.start()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (moduleProvider == null) {
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "key received [returning hereee = ")
            if(BuildConfig.FLAVOR == "mtk") {
                if (ANDROID_GLOBAL_BUTTON_ACTION == intent!!.action) {
                    var keyCode = 0
                    var action = 0
                    var source = 0
                    val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (event != null) {
                        keyCode = event.keyCode
                        action = event.action
                        source = event.device.sources
                    }
                    if (action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_TV_INPUT -> {
                                if (ReferenceApplication.isFactoryMode) {
                                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "KEYCODE_TV_INPUT && factorymode on 1")
                                    receivedTvInputKey(ReferenceApplication.applicationContext(),event!!)
                                    return
                                }
                                if (ReferenceApplication.isInputOpen) {
                                    ReferenceApplication.isInputOpen = false
                                    try {
                                        if ((getActivity() as InputSourceActivity).pinDisplayed == false) {
                                            (getActivity() as InputSourceActivity).notifyValueChange()
                                            return
                                        } else {
                                            if ((getActivity() as InputSourceActivity).inputDisplayed == true) {
                                                (getActivity() as InputSourceActivity).notifyValueChange()
                                                return
                                            } else {
                                                (getActivity() as InputSourceActivity).showInputPanel()
                                            }

                                        }
                                    } catch (E: Exception) {
                                        println(E.message)
                                    }
                                }
                                ReferenceApplication.isInputOpen = true
                                val inputsIntent = Intent("keycode_keyinput")
                                inputsIntent.flags =
                                     Intent.FLAG_ACTIVITY_NEW_TASK
                                inputsIntent.setPackage("com.iwedia.cltv")
                                inputsIntent.putExtra("isModuleProviderNull", true)
                                inputsIntent.setClass(context, InputSourceActivity::class.java)
                                startActivity(context, inputsIntent, null)
                            }
                        }
                    }
                }
                if(INPUT_CHANGE_ACTION == intent?.action) {
                    val isInputTune = intent.extras?.getBoolean("input_tune")
                    if(isInputTune == true) {
                            var inputsIntent = Intent("android.input_onetouch")
                            inputsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_RECEIVER_FOREGROUND)
                            inputsIntent.setPackage("com.iwedia.cltv")
                            inputsIntent.setClass(applicationContext(), MainActivity::class.java)
                            val inputId = intent.extras?.getString("input_id").toString()
                            intent.putExtra("input_id",inputId)
                            startActivity(context,inputsIntent,null)

                    }
                }
                if (TV_KEYCODE_INTENT_ACTION == intent?.action) {
                    Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "tv keycode received")
                    handleTvKey(context)
                }
            }
            return
        }

        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "key received [keyCode = "+intent)

        if (TV_KEYCODE_INTENT_ACTION == intent?.action) {
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "tv keycode received")
            handleTvKey(context)
        }

        if (STREAM_MUTE_CHANGED_ACTION == intent?.action) {
            // When audio is not muted before power_off then bellow snippet will resume the audio.
            if (am.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                //if home scene is active and active widget is discover tab, do not unmute the audio
                if (worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.HOME_SCENE && ((worldHandler!!.active as HomeSceneManager).scene as HomeSceneBase).activeWidget <= 0) {
                    moduleProvider!!.getPlayerModule().mute()
                }
                else if (!HomeSceneManager.IS_VOD_ACTIVE || worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.VOD) {
                    moduleProvider!!.getPlayerModule().unmute()
                }
            }
            else moduleProvider!!.getPlayerModule().mute()
            //if isMuted is checked - on mute cc should be visible otherwise not.
            //if isMuted is unchecked - mute/unmute should not change action of CC.
            val isMuted: Boolean = closedCaptionModule?.getDefaultMuteValues()!!
            if (isMuted){
                if (am.isStreamMute(AudioManager.STREAM_MUSIC))
                    closedCaptionModule?.setCCInfo()
                else
                    closedCaptionModule?.disableCCInfo()
            }
        }

        if(INPUT_CHANGE_ACTION == intent?.action) {
            val isInputTune = intent.extras?.getBoolean("input_tune")
            if(isInputTune == true) {
                if(!isAppOnForeground(context,PACKAGE_NAME)) {
                    val inputsIntent = Intent()
                    inputsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_RECEIVER_FOREGROUND)
                    inputsIntent.setPackage("com.iwedia.cltv")
                    inputsIntent.setClass(applicationContext(), MainActivity::class.java)
                    startActivity(context,inputsIntent,null)
                }
                val inputId = intent.extras?.getString("input_id").toString()
                moduleProvider?.getInputSourceMoudle()?.handleCecTune(inputId)
            }
        }

        if (ANDROID_GLOBAL_BUTTON_ACTION == intent!!.action || GLOBAL_KEY_INTENT_ACTION == intent!!.action) {
            var keyCode = 0
            var action = 0
            var source = 0
            if (ANDROID_GLOBAL_BUTTON_ACTION == intent!!.action) {
                val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (event != null) {
                    keyCode = event.keyCode
                    action = event.action
                    source = event.device.sources
                }
                if (BuildConfig.FLAVOR == "mtk") {
                    if (action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_TV_INPUT -> {
                                if (ReferenceApplication.isFactoryMode) {
                                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "KEYCODE_TV_INPUT && factorymode on 2")
                                    receivedTvInputKey(ReferenceApplication.applicationContext(),event!!)
                                    return
                                }
                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "tv input clicked")
                                if (ReferenceWorldHandler.SceneId.INTRO != worldHandler?.active?.id) {
                                    if (getActivity().localClassName == "InputSourceActivity") {
                                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "tv input clicked dismiss")
                                        try {
                                            if ((getActivity() as InputSourceActivity).pinDisplayed == false) {
                                                (getActivity() as InputSourceActivity).notifyValueChange()
                                                return
                                            } else {
                                                if ((getActivity() as InputSourceActivity).inputDisplayed == true) {
                                                    (getActivity() as InputSourceActivity).notifyValueChange()
                                                    return
                                                } else {
                                                    (getActivity() as InputSourceActivity).showInputPanel()
                                                }

                                            }
                                        } catch (E: Exception) {
                                            println(E.message)
                                        }
                                    }
                                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "tv input clicked launch")
                                    ReferenceApplication.isInputOpen = true
                                    val inputsIntent = Intent("keycode_keyinput")
                                    inputsIntent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                    inputsIntent.setPackage("com.iwedia.cltv")
                                    inputsIntent.putExtra("isModuleProviderNull", false)
                                    inputsIntent.setClass(context, InputSourceActivity::class.java)
                                    startActivity(context, inputsIntent, null)
                                }
                            }
                        }
                    }
                }
            } else {
                keyCode = intent!!.getIntExtra(GLOBAL_KEY_CODE,0)
                action = intent!!.getIntExtra(GLOBAL_KEY_ACTION, 0)
            }

            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "key received [keyCode = $keyCode, keyAction = $action]")
            if (action == KeyEvent.ACTION_UP) {
                var pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
                val wakeLock = pm?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "ref+:wakeUpTag"
                )
                wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
                if (moduleProvider!!.getInputSourceMoudle().getDefaultValue() == "TV" && worldHandler?.active?.id != ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_INFO -> {
                            if (isAppOnForeground(
                                    context,
                                    PACKAGE_NAME
                                ) && ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.INTRO
                            ) {
                                if (ReferenceApplication.isInitalized) {
                                    ReferenceApplication.runOnUiThread {
                                        if (ReferenceApplication.worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.INFO_BANNER) {
                                            return@runOnUiThread
                                        }
                                        if (moduleProvider != null && moduleProvider!!.getTvModule()
                                                .getChannelList().isNotEmpty()
                                        ) {
                                            ReferenceApplication.worldHandler?.destroyOtherExisting(
                                                ReferenceWorldHandler.SceneId.LIVE
                                            )
                                            var sceneId =
                                                ReferenceApplication.worldHandler?.active?.id
                                            var sceneInstanceId =
                                                ReferenceApplication.worldHandler?.active?.instanceId

                                            var sceneData = SceneData(sceneId!!, sceneInstanceId!!)
                                            ReferenceApplication.worldHandler!!.triggerActionWithData(
                                                ReferenceWorldHandler.SceneId.INFO_BANNER,
                                                SceneManager.Action.SHOW_OVERLAY, sceneData
                                            )
                                        }
                                    }
                                }
                            } else {
                                launchApplication(context)
                            }
                        }
                        KeyEvent.KEYCODE_MENU -> {

                            try {
                                if (isAppOnForeground(
                                        context,
                                        PACKAGE_NAME
                                    ) && ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.INTRO
                                ) {
                                    if (ReferenceApplication.isSettingsOpened) {
                                        return
                                    }
                                    if (ReferenceApplication.isInitalized) {
                                        ReferenceApplication.runOnUiThread {
                                            if (worldHandler!!.active?.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                                                val scene = worldHandler!!.active!!
                                                val activeCategory =
                                                    (scene.scene as HomeScene).getActiveCategory()
                                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: activeCategory $activeCategory")
                                                if (activeCategory == 3 || activeCategory == -1) {
                                                    return@runOnUiThread
                                                }
                                            }

                                            val tvModule = moduleProvider?.getTvModule()

                                            if (tvModule != null && (tvModule.getBrowsableChannelList(ApplicationMode.DEFAULT).isNotEmpty() ||
                                                        tvModule.getChannelList(ApplicationMode.FAST_ONLY).isNotEmpty())) {

                                                worldHandler?.destroyOtherExisting(
                                                    ReferenceWorldHandler.SceneId.LIVE
                                                )
                                                var sceneId = worldHandler?.active?.id
                                                if (sceneId == ReferenceWorldHandler.SceneId.LIVE) {
                                                    if (((worldHandler!!.active as LiveManager).scene as LiveScene).isFastZapBannerActive()) {
                                                        ((worldHandler!!.active as LiveManager).scene as LiveScene).showFastZapBanner()
                                                    }
                                                }
                                                var sceneInstanceId = worldHandler?.active?.instanceId

                                                var position = 3

                                                var sceneData =
                                                    SceneData(
                                                        sceneId!!,
                                                        sceneInstanceId!!,
                                                        position
                                                    )

                                                worldHandler!!.triggerActionWithData(
                                                    ReferenceWorldHandler.SceneId.HOME_SCENE,
                                                    SceneManager.Action.SHOW_OVERLAY, sceneData
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    launchApplication(context)
                                }
                            } catch (E: Exception) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${E.printStackTrace()}")
                            }
                        }

                        KeyEvent.KEYCODE_GUIDE -> {
                            val setupCompeted =
                                GAndroidPrefsHandler(ReferenceApplication.applicationContext()).getValue(
                                    "setupCompleted",
                                    false
                                ) as Boolean
                            if (source != INPUT_SOURCE_BLUETOOTH && !setupCompeted) {
                                // to prevent guide key press from remote setup wizard.
                                return
                            }
                            if (worldHandler != null && (worldHandler as ReferenceWorldHandler).playbackState == ReferenceWorldHandler.PlaybackState.PVR_PLAYBACK) {
                                //Do not display Guide Scene when recordings are active
                                Toast.makeText(
                                    context,
                                    ConfigStringsManager.getStringById("no_guide_pvr_running"),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }
                            try {
                                if (isAppOnForeground(
                                        context,
                                        PACKAGE_NAME
                                    ) && ReferenceApplication.worldHandler!!.active?.id != ReferenceWorldHandler.SceneId.INTRO
                                ) {
                                    if (ReferenceApplication.parentalControlDeepLink) {
                                        return
                                    }
                                    if (ReferenceApplication.isSettingsOpened) {
                                        return
                                    }
                                    if (ReferenceApplication.isInitalized) {
                                        ReferenceApplication.runOnUiThread {

                                            //disable fast clicking guide button
                                            if (worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.HOME_SCENE) {
                                                BackFromPlayback.resetKeyPressedState()
                                                worldHandler?.triggerAction(ReferenceWorldHandler.SceneId.HOME_SCENE, SceneManager.Action.HIDE) // HIDE is used because whole Home Scene should stay in memory in order to enable it's fast accessing when pressing back from LiveScene
                                                //Use channel changed information bus event to show zap banner
                                                InformationBus.submitEvent(Event(Events.REFRESH_ZAP_BANNER))
                                                return@runOnUiThread
                                            }
                                            //Check is channel list is empty
                                            if (moduleProvider!!.getTimeshiftModule().isTimeShiftActive) {
                                                /**
                                                 * If timeShift is active ,from EPG scene, again on back
                                                 * PlayerScene is required ,no need to destroy.
                                                 */
                                            } else {
                                                val skipManagerIdList = GList<Int>()
                                                skipManagerIdList.add(ReferenceWorldHandler.SceneId.LIVE)
                                                skipManagerIdList.add(ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE)
                                                worldHandler!!.destroyOtherExistingList(
                                                    skipManagerIdList
                                                )
                                            }
                                            var sceneId = worldHandler?.active?.id
                                            var sceneInstanceId =
                                                worldHandler?.active?.instanceId

                                            var position = if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) 1 else 2
                                            if (!ReferenceApplication.isRegionSupported) {
                                                position = 1
                                            }
                                            var sceneData =
                                                HomeSceneData(
                                                    sceneId!!,
                                                    sceneInstanceId!!,
                                                    position
                                                ).also {
                                                    it.initialFilterPosition = position
                                                    it.focusToCurrentEvent = true
                                                }

                                            worldHandler!!.triggerActionWithData(
                                                ReferenceWorldHandler.SceneId.HOME_SCENE,
                                                SceneManager.Action.SHOW_OVERLAY, sceneData
                                            )
                                        }
                                    }
                                } else {
                                    launchApplication(context)
                                }
                            } catch (E: Exception) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${E.printStackTrace()}")
                            }
                        }
                    }
                }
                if(moduleProvider!!.getInputSourceMoudle().getDefaultValue().contains("Composite")) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_MENU ->
                            ReferenceApplication.runOnUiThread {
                                var sceneId = worldHandler?.active?.id
                                var sceneInstanceId =
                                    worldHandler?.active?.instanceId
                                if (sceneId != ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE) {
                                    var sceneData =
                                        SceneData(sceneId!!, sceneInstanceId!!)
                                    if (sceneId == ReferenceWorldHandler.SceneId.LIVE) {
                                        ((worldHandler!!.active as LiveManager).scene as LiveScene).inputNoSignalStatus?.visibility =
                                            View.GONE
                                    }
                                    worldHandler!!.triggerActionWithData(
                                        ReferenceWorldHandler.SceneId.INPUT_PREF_SCENE,
                                        SceneManager.Action.SHOW_OVERLAY, sceneData
                                    )
                                }
                            }
                        KeyEvent.KEYCODE_GUIDE -> {
                            return
                        }
                    }
                }

            }
        }

        if (SETTINGS_OPENED_INTENT_ACTION == intent.action) {
            if (worldHandler != null && worldHandler?.active != null) {
                activeSceneId = worldHandler!!.active!!.id
                if (!(activeSceneId == ReferenceWorldHandler.SceneId.INTRO || activeSceneId == ReferenceWorldHandler.SceneId.LIVE
                            || activeSceneId == ReferenceWorldHandler.SceneId.DIALOG_SCENE)){
                    ReferenceApplication.runOnUiThread {
                        worldHandler!!.triggerAction(activeSceneId, SceneManager.Action.HIDE)
                    }
                }
            }
            ReferenceApplication.isSettingsOpened = true
        }
        if(SETTINGS_STOPPED_INTENT_ACTION == intent.action){
            if (pvrStatusChanged) {
                onPvrStatusChanged()
            } else if (!(activeSceneId == ReferenceWorldHandler.SceneId.INTRO || activeSceneId == ReferenceWorldHandler.SceneId.LIVE
                        || activeSceneId == ReferenceWorldHandler.SceneId.DIALOG_SCENE || activeSceneId == ReferenceWorldHandler.SceneId.PLAYER_SCENE)){
                ReferenceApplication.runOnUiThread {
                    worldHandler!!.triggerAction(activeSceneId, SceneManager.Action.SHOW)
                }
            }
            ReferenceApplication.isSettingsOpened = false
        }
        if (PVR_STATUS_CHANGED_INTENT_ACTION == intent.action) {
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "PVR_STATUS_CHANGED_INTENT")
            pvrStatusChanged = true
        }

        if (SCAN_COMPLETED_INTENT_ACTION == intent!!.action) {
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "scan completed sync database intent")
            if (moduleProvider != null) {
                moduleProvider!!.getWatchlistModule().clearWatchList()
                moduleProvider!!.getSchedulerModule().clearRecordingList()
            }

            if (ReferenceApplication.isInitalized) {
                try {
                    if (ReferenceApplication.isDialogueCreated) {
                        ReferenceApplication.runOnUiThread(Runnable {
                            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        })
                    }
                } catch (e: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: Destroy other raised exception, ignoring ${e.message}")
                }
                activeSceneId = ReferenceWorldHandler.SceneId.LIVE
                ReferenceApplication.scanPerformed = true
            }
            startUpdateTimer()
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver", "started Guideupdate true")
        }
        if (intent != null && intent!!.action != null && Intent.ACTION_SCREEN_OFF == intent!!.action) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            moduleProvider!!.getPlayerModule().mute()
        }
        if(intent!=null && intent!!.action!=null && Intent.ACTION_SCREEN_ON == intent!!.action){
            if (isAppOnForeground(context, PACKAGE_NAME)) {
                Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ","Screen On")
                if (moduleProvider != null) {
                    if (moduleProvider!!.getTimeshiftModule().isTimeShiftActive) {
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        moduleProvider!!.getPlayerModule().resume()
                        moduleProvider!!.getTimeshiftModule().setTimeShiftIndication(false)
                        moduleProvider!!.getTimeshiftModule().timeShiftStop(object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                            }

                            override fun onSuccess() {
                            }
                        })
                    } else {
                        var selectedAudioLanguage = utilsModule?.getPrefsValue("AUDIO_FIRST_LANGUAGE", "") as String
                        var selectedAudioTrackId = utilsModule?.getPrefsValue("AUDIO_FIRST_TRACK_ID", "") as String
                        CoroutineHelper.runCoroutineWithDelay({
                            run exitForEach@{
                                moduleProvider!!.getPlayerModule().getAudioTracks()
                                    .forEach { track ->
                                        if (selectedAudioLanguage == track.languageName && selectedAudioTrackId == track.trackId) {
                                            moduleProvider!!.getPlayerModule()
                                                .selectAudioTrack(track)
                                            InformationBus.submitEvent(Event(Events.ACTIVE_AUDIO_TRACK_REFRESHED))
                                            return@exitForEach
                                        }
                                    }
                            }
                        }, 1000,Dispatchers.Main)
                    }
                }
            }
        }
    }

    /**
     * Checks if the application is on foreground
     *
     * @param context           context
     * @param appPackageName    application package name
     * @return true if the application is on foreground
     */
    private fun isAppOnForeground(context: Context, appPackageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName == appPackageName) {
                return true
            }
        }
        return false
    }

    /**
     * Launch TV app
     * @param context
     */
    private fun launchApplication(context: Context) {
        var launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        if (launchIntent == null) {
            launchIntent =
                context.packageManager.getLeanbackLaunchIntentForPackage(PACKAGE_NAME)
        }
        ReferenceApplication.guideKeyPressed = true
        val pendingIntent =
            PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            pendingIntent.send()
        } catch (e: CanceledException) {
            e.printStackTrace()
        }
    }

    private fun getInputIds(): ArrayList<String>? {
        val retList = ArrayList<String>()
        //Get all TV inputs
        for (input in (ReferenceApplication.applicationContext().getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager).tvInputList) {
            val inputId = input.id
            retList.add(inputId)
        }
        return retList
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.R)
    @Synchronized
    private fun clearDatabase() {
        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "clearDatabase")
        var context = ReferenceApplication.applicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        var cursor = contentResolver.query(ContentProvider.CHANNELS_URI,null, null, null)
        var channelsToDelete = arrayListOf<Int>()
        if (cursor != null && cursor.count > 0) {
            cursor.moveToFirst()
            var serviceId = 0
            var tsId = 0
            var onId = 0
            do {
                if (cursor.getString(cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN)) != null) {
                    serviceId = cursor.getInt(cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN))
                }
                if (cursor.getString(cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN)) != null) {
                    tsId = cursor.getInt(cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN))
                }
                if (cursor.getString(cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN)) != null) {
                    onId = cursor.getInt(cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN))
                }
                var selection = TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID + " = ? and " + TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID + " = ? and " + TvContract.Channels.COLUMN_SERVICE_ID + " = ?"
                var c = contentResolver.query(
                    TvContract.Channels.CONTENT_URI,
                    null,
                    selection,
                    arrayOf(onId.toString(), tsId.toString(), serviceId.toString()),
                    null,
                    null,
                )
                if (c == null || c.count == 0) {
                    var id = cursor.getInt(cursor.getColumnIndex(TvContract.BaseTvColumns._ID))
                    channelsToDelete.add(id)
                } else {
                    var delete = true
                    c.let {
                        it.moveToFirst()
                        do {
                            var frequencyTv = getFrequency(c, false)
                            var frequencyDb = getFrequency(cursor, true)
                            if (frequencyTv == frequencyDb && frequencyTv != 0) {
                                delete = false
                            }
                        } while (it.moveToNext())
                        c.close()
                    }
                    if (delete) {
                        var id = cursor.getInt(cursor.getColumnIndex(TvContract.BaseTvColumns._ID))
                        channelsToDelete.add(id)
                    }
                }
            } while (cursor.moveToNext())
            cursor.close()
        }
        channelsToDelete.forEach { id ->
            contentResolver.delete(Contract.buildChannelsUri(id.toLong()), null)
        }
        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver [clearDatabase]: ", "number of deleted channels ${channelsToDelete.size}")
    }

    inner class DisplayNumberChangeInfo(
        val displayNumber : String,
        val ordinalNumber: Int,
        val onid : Int,
        val tsid : Int,
        val serviceId : Int,
        val frequency : Int,
    )

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("Range")
    @Synchronized
    private fun initDatabase() {
        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "initDatabase")

        if (BuildConfig.FLAVOR.contains("mtk")){
            //implemented in porting layer
            return
        }

        var context = ReferenceApplication.applicationContext()
        val contentResolver: ContentResolver = context.contentResolver
        var inputList = getInputIds()
        // Clear previous database content
        try {
            clearDatabase()
        } catch (e: Exception) {

        }
        display_number_third_party = THIRD_PARTY_CHANNEL_START_POSITON
        //Get the display number for all swapped channels
        var displayNumberChangeList = ArrayList<DisplayNumberChangeInfo>()
        //Get the display number for all the existing channels
        var displayNumberList = ArrayList<String>()
        var temp_cursor = contentResolver.query(
            ContentProvider.CHANNELS_URI,
            null,null,
            null,null
        )
        if (temp_cursor!!.count > 0) {
            temp_cursor.moveToFirst()
            do {
                if (temp_cursor.getString(temp_cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_COLUMN)) != null) {
                    if(temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_CHANGED_COLUMN)) == 1) {
                        displayNumberChangeList.add(
                            DisplayNumberChangeInfo(
                                temp_cursor.getString(temp_cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.ORDINAL_NUMBER_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN)),
                                temp_cursor.getInt(temp_cursor.getColumnIndex(Contract.Channels.SERVICE_ID_COLUMN)),
                                getFrequency(temp_cursor,true))
                        )
                    }else{
                        displayNumberList.add(temp_cursor.getString(temp_cursor.getColumnIndex(Contract.Channels.DISPLAY_NUMBER_COLUMN)))
                    }
                }
            } while (temp_cursor.moveToNext())
        }
        temp_cursor?.close()

        var ordinalNumber = 1
        if (inputList!!.isNotEmpty()) {
            for (input in inputList) {
                //If third party input customization value is disabled, don't include the third party channel to database.
                if((!isThirdPartyInputEnabled()) &&
                    !(input.contains("com.google.android.tv.dtvinput") || input.contains("com.mediatek.tvinput"))) {
                    Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "Skipping 3rd party content.. $input")
                    continue
                }

                var cursor = contentResolver.query(
                    TvContract.buildChannelsUriForInput(input),
                    null,
                    null,
                    null,
                    null
                )

                if (cursor!!.count > 0) {
                    val contentValues = ArrayList<ContentValues>(cursor.count)
                    cursor.moveToFirst()
                    do {
                        var value = ContentValues()
                        if (cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID)) != null) {
                            var id = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID)).toInt()
                            value.put(Contract.Channels.ORIG_ID_COLUMN, id)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_PACKAGE_NAME)) != null) {
                            var packageName = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_PACKAGE_NAME))
                            value.put(Contract.Channels.PACKAGE_NAME_COLUMN, packageName)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID)) != null) {
                            var inputId = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INPUT_ID))
                            value.put(Contract.Channels.INPUT_ID_COLUMN, inputId)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE)) != null) {
                            var type = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE))
                            value.put(Contract.Channels.TYPE_COLUMN, type)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE)) != null) {
                            var serviceType = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE))
                            value.put(Contract.Channels.SERVICE_TYPE_COLUMN, serviceType)
                        }
                        var onid = 0
                        var tsid = 0
                        var serviceId = 0
                        var name = ""
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID)) != null) {
                            onid = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID))
                            value.put(Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN, onid)
                        }
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID)) != null) {
                            tsid = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID))
                            value.put(Contract.Channels.TRANSPORT_STREAM_ID_COLUMN, tsid)
                        }
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID)) != null) {
                            serviceId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID))
                            value.put(Contract.Channels.SERVICE_ID_COLUMN, serviceId)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_NETWORK_AFFILIATION)) != null) {
                            var networkAffiliation = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_NETWORK_AFFILIATION))
                            value.put(Contract.Channels.NETWORK_AFFILIATION_COLUMN, networkAffiliation)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION)) != null) {
                            var description = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION))
                            value.put(Contract.Channels.DESCRIPTION_COLUMN, description)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VIDEO_FORMAT)) != null) {
                            var videoFormat = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VIDEO_FORMAT))
                            value.put(Contract.Channels.VIDEO_FORMAT_COLUMN, videoFormat)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)) != null) {
                            name = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME))
                            value.put(Contract.Channels.NAME_COLUMN, name)
                        }

                        var browsable = 1
                        var oemCursor : Cursor? = contentResolver.query(
                            ContentProvider.OEM_CUSTOMIZATION_URI,
                            null,
                            null,
                            null,
                            null
                        )
                        if((oemCursor != null) && (oemCursor!!.count > 0)) {
                            var scrambled = false
                            oemCursor.moveToFirst()
                            val scanTypeIndex = oemCursor.getColumnIndex(Contract.OemCustomization.SCAN_TYPE)
                            if(scanTypeIndex != -1) {
                                val scanType = oemCursor.getString(oemCursor.getColumnIndex(Contract.OemCustomization.SCAN_TYPE))
                                if(scanType.equals("free")) {
                                    var mInternalProviderFlag1 =
                                        cursor.getInt(cursor.getColumnIndex(Contract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN))
                                    if((mInternalProviderFlag1 and SCRAMBLED_MASK) !== 0){
                                        scrambled = true
                                    }
                                }
                                if (scrambled) {
                                    browsable = 0
                                }
                            }
                            oemCursor?.close()
                        }
                        value.put(Contract.Channels.BROWSABLE_COLUMN, browsable)

                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SEARCHABLE)) != null) {
                            var searchable = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SEARCHABLE))
                            value.put(Contract.Channels.SEARCHABLE_COLUMN, searchable)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI)) != null) {
                            var iconUri = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_ICON_URI))
                            value.put(Contract.Channels.APP_LINK_ICON_URI_COLUMN, iconUri)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI)) != null) {
                            var posterArtUri = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI))
                            value.put(Contract.Channels.APP_LINK_POSTER_ART_URI_COLUMN, posterArtUri)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI)) != null) {
                            var posterArtUri = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI))
                            value.put(Contract.Channels.APP_LINK_POSTER_ART_URI_COLUMN, posterArtUri)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_TEXT)) != null) {
                            var appLinkText = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_TEXT))
                            value.put(Contract.Channels.APP_LINK_TEXT_COLUMN, appLinkText)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_COLOR)) != null) {
                            var appLinkColor = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_COLOR))
                            value.put(Contract.Channels.APP_LINK_COLOR_COLUMN, appLinkColor)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI)) != null) {
                            var appLinkIntent = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI))
                            value.put(Contract.Channels.APP_LINK_INTENT_URI_COLUMN, appLinkIntent)
                        }
                        if (cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA)) != null) {
                            var blob = cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA))
                            value.put(Contract.Channels.INTERNAL_PROVIDER_DATA_COLUMN, blob)
                            if (BuildConfig.FLAVOR.contains("mtk")){
                                if (blob != null && blob.isNotEmpty()){
                                    var providerData = String(blob, Charsets.UTF_8)
                                    //TODO
                                    /*if (!RunningDataProvider.isVisible(providerData)){
                                        browsable = 0
                                        value.put(ReferenceContract.Channels.BROWSABLE_COLUMN, browsable)
                                    }*/
                                }
                            }
                        }

                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1)) != null) {
                            var flag1 = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1))
                            value.put(Contract.Channels.INTERNAL_PROVIDER_FLAG1_COLUMN, flag1)
                            if (moduleProvider?.getUtilsModule()?.isGretzkyBoard() == true){
                                if (((flag1 and 0x1) == 0)){
                                    browsable = 0
                                    value.put(Contract.Channels.BROWSABLE_COLUMN, browsable)
                                }
                            }
                        }

                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_VERSION_NUMBER)) != null) {
                            var versionNumber = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_VERSION_NUMBER))
                            value.put(Contract.Channels.VERSION_NUMBER_COLUMN, versionNumber)
                        }
                        if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSIENT)) != null) {
                            var transient = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSIENT))
                            value.put(Contract.Channels.TRANSIENT_COLUMN, transient)
                        }
                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID)) != null) {
                            var transient = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID))
                            value.put(Contract.Channels.TRANSIENT_COLUMN, transient)
                        }
                        value.put(
                            Contract.Channels.ORDINAL_NUMBER_COLUMN,
                            ordinalNumber++
                        )

                        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)) != null) {
                            var displayNumberSet = false
                            var frequency = getFrequency(cursor,false)
                            displayNumberChangeList.forEach {
                                if ((it.onid == onid) && (it.tsid == tsid) && (it.serviceId == serviceId) && (it.frequency == frequency)) {
                                    Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "Keeping channel swap ${it.displayNumber} for $name onid: $onid tsid:  $tsid sid: $serviceId freq: $frequency")
                                    value.put(Contract.Channels.DISPLAY_NUMBER_COLUMN, it.displayNumber)
                                    value.put(Contract.Channels.ORDINAL_NUMBER_COLUMN, it.ordinalNumber)
                                    displayNumberSet = true
                                }
                            }
                            if(!displayNumberSet) {
                                setDisplayNumber(cursor, input, value, displayNumberList)
                            }
                        }

                        //Update existing channel in db
                        var selection = Contract.Channels.ORIGINAL_NETWORK_ID_COLUMN + " = ? and " + Contract.Channels.TRANSPORT_STREAM_ID_COLUMN + " = ? and " + Contract.Channels.SERVICE_ID_COLUMN + " = ?"
                        var c = contentResolver.query(
                            ContentProvider.CHANNELS_URI,
                            null,
                            selection,
                            arrayOf(onid.toString(), tsid.toString(), serviceId.toString()),
                            null
                        )
                        var updateId = -1
                        var isDeleted = 0
                        Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "check update  $name")
                        if (c != null && c.count > 0) {
                            c.moveToFirst()
                            do {
                                var frequencyTv = getFrequency(cursor, false)
                                var frequencyRef = getFrequency(c, true)
                                if (frequencyTv == frequencyRef && frequencyTv != 0) {
                                    updateId = c.getInt(c.getColumnIndex(TvContract.Channels._ID))
                                    isDeleted = c.getInt(c.getColumnIndex(Contract.Channels.DELETED_COLUMN))
                                }
                            } while (c.moveToNext())
                            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "check update  $name $updateId")
                            if (updateId != -1) {

                                if (isDeleted == 1 && !ReferenceApplication.scanPerformed){
                                    Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "Channel was manually deleted, do not update its browsable")
                                    browsable = 0
                                    value.put(Contract.Channels.BROWSABLE_COLUMN,browsable)
                                }
                                contentResolver.update(Contract.buildChannelsUri(updateId.toLong()), value, null)
                                Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "updated channel $updateId $onid $tsid $serviceId")
                                var mChannelImageData: ChannelImageData = ChannelImageData()
                                mChannelImageData.sid = serviceId
                                mChannelImageData.onid = onid
                                mChannelImageData.tsid = tsid
                                mChannelImageData.channelName = name
                            } else {
                                if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED)) != null) {
                                    var locked = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED))
                                    value.put(Contract.Channels.LOCKED_COLUMN, locked)
                                }
                                value.put(Contract.Channels.SKIP_COLUMN, 0.toInt())
                                contentValues.add(value)
                            }
                        } else {
                            if (cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED)) != null) {
                                var locked = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED))
                                value.put(Contract.Channels.LOCKED_COLUMN, locked)
                            }
                            value.put(Contract.Channels.SKIP_COLUMN, 0.toInt())
                            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "added channel $name $onid $tsid $serviceId")
                            contentValues.add(value)
                        }
                        c!!.close()
                    } while (cursor.moveToNext())
                    var cv = arrayOfNulls<ContentValues>(contentValues.size)
                    for(qwe in 0 until contentValues.size){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "initDatabase: contentValues [$qwe] = ${contentValues.get(qwe)}")
                    }
                    cv = contentValues.toArray(cv)
                    //todo notification is sent before bulkInsert is finished - this might cause a problem latter on
                    numInserted = contentResolver.bulkInsert(ContentProvider.CHANNELS_URI, cv)
                    Log.d(Constants.LogTag.CLTV_TAG + "ChannelLog", "init database number of inserted channels $numInserted")
                }
                cursor!!.close()
            }
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "initDatabase: initDatabase done")
        }
    }

    @SuppressLint("Range")
    private fun getFrequency(cursor: Cursor, fromReferenceDb : Boolean) : Int {
        var blob: ByteArray?
        var frequency = 0
        if (BuildConfig.FLAVOR == "rtk"){
            frequency = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1))
            return frequency
        }
        if (BuildConfig.FLAVOR == "base"){
            return 0
        }
        if (BuildConfig.FLAVOR.contains("mal_service")) {
            return 0
        }
        if (fromReferenceDb) {
            blob = cursor.getBlob(cursor.getColumnIndex(Contract.Channels.INTERNAL_PROVIDER_DATA_COLUMN))
        } else {
            blob = cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA))
        }
        try {
            if (blob != null && blob.isNotEmpty()) {
                var providerData = String(blob, Charsets.UTF_8)
                val obj = JSONObject(providerData)
                if(obj != null && obj.has("transport")) {
                    val jsonTransportObject = obj.getJSONObject("transport")
                    if (jsonTransportObject.has("frequency"))
                        frequency = jsonTransportObject.getInt("frequency")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(Constants.LogTag.CLTV_TAG + "GlobalAppReceiver ", "getFrequency ${e.printStackTrace()}")
            return frequency
        }
        return frequency
    }

    @SuppressLint("Range")
    private fun setDisplayNumber(cursor: Cursor, input: String, value: ContentValues, displayNumberList: ArrayList<String>) {
        if (cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER)) != null) {
            var displayNumber = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER))
            //3rd party channel position should start from 10001
            if(input.contains("com.google.android.tv.dtvinput") || input.contains("com.mediatek.tvinput") || input.contains("com.realtek.dtv")) {
                //If normal channel's display Number is 10000+, make sure no conflict
                try {
                    var int_display_number = displayNumber.toInt()
                    if(int_display_number >= THIRD_PARTY_CHANNEL_START_POSITON) {
                        if(int_display_number <= display_number_third_party) {
                            int_display_number = display_number_third_party++
                        }
                        while (displayNumberList.contains(int_display_number.toString())) {
                            int_display_number = display_number_third_party++
                        }
                        displayNumber = int_display_number.toString()
                    }
                } catch(e: NumberFormatException){
                }
            } else {
                //3rd party channels - position should not be below 10000.
                var updateDisplayNumber = false
                try {
                    if(displayNumber.toInt() < THIRD_PARTY_CHANNEL_START_POSITON) {
                        updateDisplayNumber = true
                    }
                } catch(e: NumberFormatException) {
                    // 3rd party channel is not having proper LCN, update it.
                    updateDisplayNumber = true
                }
                if(updateDisplayNumber) {
                    while(displayNumberList.contains(display_number_third_party.toString())) {
                        display_number_third_party++
                    }
                    displayNumber = display_number_third_party.toString()
                    display_number_third_party++
                }
            }
            value.put(Contract.Channels.DISPLAY_NUMBER_COLUMN, displayNumber)
            displayNumberList.add(displayNumber)
        }
    }

    /**
     * Checks if the third party input is enabled
     *
     * @return true if the third party input is enabled. Default is true
     */
    @SuppressLint("Range")
    fun isThirdPartyInputEnabled(): Boolean {
        if (BuildConfig.FLAVOR.contains("base") || BuildConfig.FLAVOR.contains("mal_service")) {
            return true
        } else {
            var context = ReferenceApplication.applicationContext()
            val contentResolver: ContentResolver = context.contentResolver
            var cursor = contentResolver.query(
                ContentProvider.OEM_CUSTOMIZATION_URI,
                null,
                null,
                null,
                null
            )
            if (cursor!!.count > 0) {
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(Contract.OemCustomization.THIRD_PARTY_INPUT_ENABLED_COLUMN)) == 0) {
                    return false
                }
            }
            return true
        }
    }

    private fun onPvrStatusChanged() {
        pvrStatusChanged = false
        if (worldHandler != null && worldHandler?.active != null) {
            if (!ReferenceApplication.isInForeground) {
                ReferenceApplication.getActivity().finishAffinity()
            } else {
                ReferenceApplication.runOnUiThread {
                    var activeSceneId = worldHandler!!.active!!.id
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        SceneManager.Action.DESTROY
                    )
                    worldHandler!!.triggerAction(activeSceneId, SceneManager.Action.DESTROY)
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                    worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.LIVE, SceneManager.Action.SHOW)
                }
            }
        }
    }
    private fun launchFactoryAppEntryScreen(
        context: Context,
        event: KeyEvent,
        packageName: String
    ) {
        val keyCode = event.keyCode
        val action = event.action
        if (action == KeyEvent.ACTION_UP) {
            val startCode = "START_CODE"
            val startCodeKeyInput = 2
            val keyCodeString = "KEY_CODE"
            var extraCode = ""
            extraCode = when (keyCode) {
                KeyEvent.KEYCODE_TV_INPUT -> "SOURCE"
                else -> return
            }
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"FactoryModeActivity"," launchFactoryAppEntryScreen $extraCode")
            val intent = Intent()
            intent.component = ComponentName(packageName, FACTORY_ENTRY_CLASS_NAME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(startCode, startCodeKeyInput)
            intent.putExtra(keyCodeString, extraCode)
            context.startActivity(intent)
        }

    }
    private fun receivedTvInputKey(context: Context, event: KeyEvent) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.getRunningTasks(1)
        val cn = tasks[0].topActivity
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "active app : package = " + cn!!.packageName + ", class = " + cn.className)
        if (cn.packageName.endsWith(FACTORY_PACKAGE_NAME)) {
            //Factory
            if (cn.className.endsWith(FFACTORY_INITIAL_CLASS_NAME)) {
                launchFactoryAppEntryScreen(context, event, cn.packageName)
            }
        }
    }

    private fun handleTvKey(context: Context) {
        if(!isAppOnForeground(context,PACKAGE_NAME)) {
            var launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            if (launchIntent == null) {
                launchIntent =
                    context.packageManager.getLeanbackLaunchIntentForPackage(PACKAGE_NAME)
            }
            val pendingIntent =
                PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                pendingIntent.send()
            } catch (e: CanceledException) {
                e.printStackTrace()
            }
        }
    }
}
