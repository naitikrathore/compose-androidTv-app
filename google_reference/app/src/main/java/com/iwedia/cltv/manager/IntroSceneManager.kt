package com.iwedia.cltv.manager

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.iwedia.cltv.*
import com.iwedia.cltv.R
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.cltv.scene.intro_scene.IntroScene
import com.iwedia.cltv.scene.intro_scene.IntroSceneListener
import com.iwedia.cltv.tis.ui.SetupActivity
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData
import world.SceneManager

/**
 * Intro manager
 *
 * @author Veljko Ilkic
 */
class IntroSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var tvModule: TvInterface,
    var utilsModule: UtilsInterface,
    var inputSourceModule: InputSourceInterface,
    var parentalControlSettingsModule: ParentalControlSettingsInterface,
    var networkModule: NetworkInterface,
    var fastUserSettingsModule: FastUserSettingsInterface,
    var ciplusModule : CiPlusInterface
) : GAndroidSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.INTRO
), IntroSceneListener {

    private val TAG: String = this.toString()

    init {
        isScreenFlowSecured = false
    }

    override fun createScene() {
        scene = IntroScene(context!!, this)
        registerGenericEventListener(Events.NETWORK_UNAVAILABILITY)
        checkNetworkConnection()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (event?.type == Events.NETWORK_UNAVAILABILITY) {
            scene?.refresh(ConfigStringsManager.getStringById("no_internet_message"))
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onAppInitialized() {
        var isFirstRun = utilsModule.getPrefsValue("isFirstRun", false) as Boolean
        if (Utils.isEvaluationFlavour() && !isFirstRun) {
            ReferenceApplication.runOnUiThread {
                worldHandler?.triggerAction(id, Action.DESTROY)
                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.EVALUATION_SCENE,
                    Action.SHOW
                )
            }
        } else {
            if (((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()) {
                if (networkModule.networkStatus.value == null || networkModule.networkStatus.value == NetworkData.NoConnection) {
                    showNoInternetDialog()
                } else if(fastUserSettingsModule.isRegionSupported()) {
                    if (tvModule.getChannelList().size == 0) {
                        //Start tis scan
                        startTisScan()
                    } else {
                        startApplication()
                    }
                } else {
                    showNoChannelsDialog()
                }
            } else {
                if (networkModule.networkStatus.value == null || networkModule.networkStatus.value == NetworkData.NoConnection) {
                    ReferenceApplication.runOnUiThread{
                        showToast(ConfigStringsManager.getStringById("no_ethernet_message"))
                    }
                }

                if (!fastUserSettingsModule.isRegionSupported()) {
                    /* showToast(ConfigStringsManager.getStringById("region_not_supported")) */
                    fastUserSettingsModule.deleteAllFastData(SetupActivity.INPUT_ID)
                    if((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onAppInitialized: change application mode")
                        utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal)
                    }
                }

                fastUserSettingsModule.checkTos(object : IAsyncDataCallback<Boolean>{
                    override fun onFailed(error: Error) {}

                    override fun onReceive(data: Boolean) {
                        val isChannelsEmpty = tvModule.getBrowsableChannelList(ApplicationMode.DEFAULT).isEmpty()
                        val isFastChannelsEmpty = tvModule.getChannelList(ApplicationMode.FAST_ONLY).isEmpty()
                        if(data && isFastChannelsEmpty){
                            startTisScan()
                        } else if (isFastChannelsEmpty && isChannelsEmpty) {
                            showNoBroadcastChannelsDialog()
                        } else {
                            if(isChannelsEmpty){
                                // To play fast channels initially when broadcast channels are not available
                                FastLiveTabDataProvider.utilsModule!!.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
                            }
                            startApplication()
                        }
                    }
                })
            }
        }
    }

    override fun exitApplication() {
        InformationBus.submitEvent(Event(Events.EXIT_APPLICATION_ON_BACK_PRESS))
    }

    override fun isRegionSupported(): Boolean {
        return ReferenceApplication.isRegionSupported
    }

    override fun onSceneInitialized() {
        // Check if already initialized
        if (ReferenceApplication.isInitalized) {
            if (((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()) {
                if (!(ReferenceApplication.getActivity() as MainActivity).isWaitingForUserPermission) {
                    scene!!.refresh(true)
                } else if (networkModule.networkStatus.value == null || networkModule.networkStatus.value == NetworkData.NoConnection) {
                    showNoInternetDialog()
                }
            } else {
                scene!!.refresh(true)
            }
        }
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startApplication() {
        checkAndSetApplicationModeForT56()
        ReferenceApplication.runOnUiThread {

            if (!((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly() && ReferenceApplication.guideKeyPressed) {

                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.LIVE,
                    Action.SHOW
                )
                ReferenceApplication.worldHandler?.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
                // Show guide scene if the guide global key is pressed

                val sceneId = ReferenceApplication.worldHandler?.active?.id
                val sceneInstanceId =
                    ReferenceApplication.worldHandler?.active?.instanceId
                val position = 2 //broadcast tab position

                val sceneData = SceneData(sceneId!!, sceneInstanceId!!, position)
                ReferenceApplication.worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.HOME_SCENE,
                    Action.SHOW_OVERLAY, sceneData
                )
                ReferenceApplication.guideKeyPressed = false

                return@runOnUiThread
            }
            worldHandler?.triggerAction(id, Action.DESTROY)
                worldHandler?.triggerAction(
                    ReferenceWorldHandler.SceneId.LIVE,
                    Action.SHOW
                )

            if (inputSourceModule.isBlock(inputSourceModule.getDefaultValue())
            ) {
                InformationBus.submitEvent(
                    Event(
                        Events.BLOCK_TV_VIEW, inputSourceModule.getDefaultValue()
                    )
                )
                return@runOnUiThread
            }
            if (inputSourceModule.getDefaultValue() != "TV") {
                inputSourceModule.handleInputSource(
                    inputSourceModule.getDefaultValue(),
                    inputSourceModule.getDefaultURLValue()
                )
            }
            if(utilsModule.needTCServiceUpdate(context!!)){
                InformationBus.submitEvent(Event(Events.SHOW_NEW_CHANNELS_FOUND_POPUP))
            }

            ciplusModule.enableProfileInstallation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkAndSetApplicationModeForT56() {
        if((worldHandler as ReferenceWorldHandler).isT56()){
            if(fastUserSettingsModule.isRegionSupported() && tvModule.getChannelList(ApplicationMode.FAST_ONLY).isNotEmpty()){
                //set up application mode to fast again otherwise will have black screen when back is pressed from live tab. [Only for T56]
                utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
            } else{
                utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal)
            }
        }
    }

    /**
     * Fast scan result broadcast receiver
     */
    private val receiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Anoki scan result intent received")
            if (intent != null && intent.action == ReferenceApplication.FAST_SCAN_RESULT) {
                if (intent.extras != null) {
                    val scanResult = intent.extras!!.getInt(ReferenceApplication.FAST_SCAN_RESULT, 0)
                    if (((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isFastOnly()) {
                        if (scanResult == 0) {
                            showNoChannelsDialog()
                        } else {
                            ReferenceApplication.isInitalized = false
                            worldHandler?.triggerAction(id, Action.DESTROY)
                            worldHandler?.triggerAction(
                                id,
                                Action.SHOW
                            )
                        }
                    } else {
                        var isChannelsEmpty = true
                        run channelsEmptyCheck@ {
                            tvModule.getChannelList(ApplicationMode.DEFAULT).forEach {
                                if (it.isBrowsable || it.inputId.contains("iwedia") || it.inputId.contains("sampletvinput")) {
                                    isChannelsEmpty = false
                                    return@channelsEmptyCheck
                                }
                            }
                        }

                        if (scanResult == 0 && isChannelsEmpty) {
                            showNoBroadcastChannelsDialog()
                        } else {
                            if(isChannelsEmpty){
                                // To play fast channel when broadcast channels are not available
                                FastLiveTabDataProvider.utilsModule!!.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
                                CoroutineHelper.runCoroutineForSuspend({
                                    waitTillListIsFilled()
                                    startApplication()
                                },Dispatchers.IO)
                            }else{
                                startApplication()
                            }
                        }
                    }
                    ReferenceApplication.applicationContext().unregisterReceiver(this)
                }
            }
        }
    }

    /**
     * Method check every 1 sec for next 5sec if list is empty
     * if not empty it gets out and finish execution
     * */
    suspend fun waitTillListIsFilled(){
        withTimeoutOrNull(5000){
            async {
                while (tvModule.getChannelList(ApplicationMode.FAST_ONLY).size <= 0) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "waiting...")
                    delay(1000)
                }
            }.await()
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun startTisScan() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Anoki startTisScan")
        val intent = Intent(ReferenceApplication.FAST_SCAN_START)
        ReferenceApplication.applicationContext().sendBroadcast(intent)
        ReferenceApplication.applicationContext()
            .registerReceiver(receiver, IntentFilter(ReferenceApplication.FAST_SCAN_RESULT))
    }

    private fun showNoInternetDialog() {

        val sceneData = DialogSceneData(-1, -1)

        sceneData.apply {
            type = DialogSceneData.DialogType.TEXT
            title = ConfigStringsManager.getStringById("no_internet_connection")
            message = ConfigStringsManager.getStringById("please_try_to_reconnect")
            positiveButtonText = ConfigStringsManager.getStringById("Retry")
            imageRes = R.drawable.no_internet_icon
            isBackEnabled = true
            dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                }

                override fun onPositiveButtonClicked() {
                    if (networkModule.networkStatus.value != null && networkModule.networkStatus.value != NetworkData.NoConnection) {
                        ReferenceApplication.runOnUiThread {
                            worldHandler?.triggerAction(
                                ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE,
                                Action.DESTROY
                            )
                        }
                        if (tvModule.getChannelList().size == 0) {
                            startTisScan()
                        } else {
                            startApplication()
                        }
                    }
                }
            }
        }

        worldHandler?.triggerAction(id, Action.HIDE)

        ReferenceApplication.worldHandler?.triggerActionWithData(
            ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE,
            Action.SHOW_OVERLAY, sceneData
        )

    }

    private fun showNoChannelsDialog() {
        val sceneData = DialogSceneData(-1, -1)

        sceneData.apply {

            type = DialogSceneData.DialogType.TEXT
            title = ConfigStringsManager.getStringById("channel_list_not_available")
            message = ConfigStringsManager.getStringById("check_if_region_not_set_properly")
            positiveButtonText = ConfigStringsManager.getStringById("Exit")
            imageRes = R.drawable.no_channels_icon
            isBackEnabled = true
            dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                }

                override fun onPositiveButtonClicked() {
                    ReferenceApplication.get().activity?.finish()
                }
            }
        }

        ReferenceApplication.runOnUiThread {
            ReferenceApplication.worldHandler?.triggerActionWithData(
                ReferenceWorldHandler.SceneId.NO_INTERNET_DIALOG_SCENE,
                SceneManager.Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    private fun showNoBroadcastChannelsDialog() {
        val sceneData = DialogSceneData(-1, -1)
        sceneData.type = DialogSceneData.DialogType.TEXT
        (ReferenceApplication.getActivity() as MainActivity).newChannelsFoundResetApp = true
        sceneData.title = ConfigStringsManager.getStringById("no_channels_found")
        sceneData.message = ConfigStringsManager.getStringById("connect_scan_msg")
        sceneData.positiveButtonText =
            ConfigStringsManager.getStringById("scan_channels_text")
        sceneData.isBackEnabled = true
        sceneData.dialogClickListener =
            object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    ReferenceApplication.runOnUiThread {
                        worldHandler?.destroyExisting()
                        ReferenceApplication.getActivity().finish()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                }

                override fun onPositiveButtonClicked() {

                    if (!utilsModule.kidsModeEnabled()) {
                        ReferenceApplication.runOnUiThread {
                            worldHandler?.triggerAction(
                                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                Action.DESTROY
                            )
                        }
                        try {
                            utilsModule.startScanChannelsIntent()
                        } catch (e: Exception) {
                            Toast.makeText(context, "NO SCAN INTENT FOUND", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

        ReferenceApplication.runOnUiThread {
            worldHandler?.triggerAction(id, Action.DESTROY)
            worldHandler?.triggerAction(
                ReferenceWorldHandler.SceneId.LIVE,
                Action.SHOW
            )
            worldHandler?.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    private fun checkNetworkConnection() {
        val connectivityManager: ConnectivityManager = ReferenceApplication.get().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork

        val capabilities = connectivityManager.getNetworkCapabilities(network)
        var hasInternet = false
        capabilities?.let {
            hasInternet = it.hasCapability(NET_CAPABILITY_INTERNET)
        }
        if (!hasInternet) {
            ReferenceApplication.isRegionSupported = false
            InformationBus.submitEvent(Event(Events.ANOKI_REGION_NOT_SUPPORTED))
        }
    }
}