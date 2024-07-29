package com.iwedia.cltv.manager

import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.ClosedCaptionInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.SchedulerInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.infoBanner.InfoBannerScene
import com.iwedia.cltv.scene.infoBanner.InfoBannerSceneListener
import com.iwedia.cltv.utils.Utils
import listeners.AsyncDataReceiver
import utils.information_bus.Event
import world.SceneData
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Info banner scene manager
 *
 * @author Aleksandar Lazic
 */
class InfoBannerSceneManager : ReferenceSceneManager, InfoBannerSceneListener {

    //current channel index
    private var currentIndex = 0
    val TAG = javaClass.simpleName
    //list of events for desired channel
    val eventsList = ArrayList<TvEvent>()

    //channel list
    var channelsList: ArrayList<TvChannel>? = null

    private lateinit var activeChannel: TvChannel

    var isAudioPressed = false
    var isCaptionsPressed = false
    var utilsModule: UtilsInterface
    private var tvModule: TvInterface? = null
    private var epgModule: EpgInterface? = null
    private var schedulerModule: SchedulerInterface? = null
    private var pvrModule: PvrInterface? = null
    private var playerModule: PlayerInterface? = null
    private var watchlistModule: WatchlistInterface?= null
    private var timeshiftModule: TimeshiftInterface?= null
    private var closedCaptionModule: ClosedCaptionInterface? = null
    private var parentalControlSettingsModule: ParentalControlSettingsInterface? = null
    private var timeModule: TimeInterface?= null
    private var generalConfigModule: GeneralConfigInterface? = null
    private var textToSpeechModule: TTSInterface
    constructor(
        context: MainActivity,
        worldHandler: ReferenceWorldHandler,
        tvModule: TvInterface,
        epgModule: EpgInterface,
        schedulerModule: SchedulerInterface,
        pvrModule: PvrInterface,
        playerModule: PlayerInterface,
        utilsModule: UtilsInterface,
        watchlistModule: WatchlistInterface,
        timeshiftModule: TimeshiftInterface,
        closedCaptionModule: ClosedCaptionInterface,
        parentalControlSettingsModule: ParentalControlSettingsInterface,
        timeModule: TimeInterface,
        generalConfigModule: GeneralConfigInterface,
        textToSpeechModule: TTSInterface
    ) : super(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.INFO_BANNER
    ) {
        isScreenFlowSecured = false
        this.tvModule = tvModule
        this.epgModule = epgModule
        this.schedulerModule = schedulerModule
        this.pvrModule = pvrModule
        this.playerModule = playerModule
        this.utilsModule = utilsModule
        this.watchlistModule = watchlistModule
        this.timeshiftModule = timeshiftModule
        this.closedCaptionModule = closedCaptionModule
        this.parentalControlSettingsModule = parentalControlSettingsModule
        this.timeModule = timeModule
        this.generalConfigModule = generalConfigModule
        this.textToSpeechModule = textToSpeechModule
    }

    override fun createScene() {
        scene = InfoBannerScene(context!!, this)
        registerGenericEventListener(Events.TIME_CHANGED)
        registerGenericEventListener(Events.UPDATE_INFO_BANNER)
        registerGenericEventListener(Events.ACTIVE_AUDIO_TRACK_REFRESHED)
        registerGenericEventListener(Events.AUDIO_TRACKS_UPDATED)
        registerGenericEventListener(Events.INACTIVITY_TIMER_END)
    }

    override fun getIsInReclist(tvEvent: TvEvent): Boolean {
        return schedulerModule!!.isInReclist(tvEvent.tvChannel.id, tvEvent.startTime)
    }

    override fun getIsCC(type: Int): Boolean {
        return playerModule!!.getIsCC(type)
    }

    override fun getIsAudioDescription(type: Int): Boolean {
        return playerModule!!.getIsAudioDescription(type)
    }

    override fun getTeleText(type: Int): Boolean {
        return playerModule!!.getTeleText(type)
    }

    override fun getIsDolby(type: Int): Boolean {
        return playerModule!!.getIsDolby(type)
    }

    override fun isHOH(type: Int): Boolean {
        return playerModule!!.getIsCC(type)
    }

    override fun isInWatchlist(tvEvent: TvEvent): Boolean {
        return watchlistModule!!.isInWatchlist(tvEvent)
    }

    override fun getChannelSourceType(tvChannel: TvChannel) : String {
        return tvModule!!.getChannelSourceType(tvChannel)
    }

    override fun isParentalOn(): Boolean {
        return tvModule!!.isParentalEnabled()
    }

    override fun getAudioChannelInfo(type: Int): String {
        var audioChannelIdx = playerModule!!.getAudioChannelIndex(type)
        return if (audioChannelIdx != -1)
            Utils.getAudioChannelStringArray()[audioChannelIdx]
        else ""
    }

    override fun getAudioFormatInfo(): String {
        return playerModule!!.getAudioFormat()
    }
    override fun isScrambled(): Boolean {
        return playerModule!!.playbackStatus.value == PlaybackStatus.SCRAMBLED_CHANNEL
    }
    override fun refreshData(tvChannel: TvChannel) {
        epgModule!!.getEventListByChannel(
            tvChannel,
            object : IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(data: ArrayList<TvEvent>) {
                    ReferenceApplication.runOnUiThread(Runnable {
                        eventsList.clear()
                        eventsList.addAll(data)
                        scene!!.refresh(eventsList)
                    })
                }
            }
        )
    }

    override fun getLanguageMapper(): LanguageMapperInterface {
        return utilsModule!!.getLanguageMapper()!!
    }

    override fun getVideoResolution(): String {
        return playerModule!!.getVideoResolution()!!
    }

    override fun isParentalControlsEnabled(): Boolean {
        return parentalControlSettingsModule!!.isParentalControlsEnabled()
    }

    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        return tvModule!!.getParentalRatingDisplayName(parentalRating,ApplicationMode.DEFAULT, tvEvent)
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return timeModule!!.getCurrentTime(tvChannel)
    }

    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
        return utilsModule!!.isCurrentEvent(tvEvent)
    }

    override fun getDateTimeFormat(): DateTimeFormat {
        return utilsModule!!.getDateTimeFormat()
    }

    override fun isEventLocked(tvEvent: TvEvent?) = parentalControlSettingsModule!!.isEventLocked(tvEvent)

    override fun isPvrPathSet(): Boolean {
        return utilsModule?.getPvrStoragePath()?.isNotEmpty() ?: false
    }

    override fun isUsbFreeSpaceAvailable(): Boolean {
        return utilsModule?.isUsbFreeSpaceAvailable() == true
    }

    override fun isUsbWritableReadable(): Boolean {
        return utilsModule?.isUsbWritableReadable() == true
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule!!.getGeneralSettingsInfo(nameOfInfo)
    }

    override fun getPlatformName(): String {
        return (worldHandler as ReferenceWorldHandler).getPlatformName()
    }

    override fun isUsbStorageAvailable(): Boolean {
        return utilsModule?.getUsbDevices()?.isNotEmpty() == true
    }

    override fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>) {
        pvrModule!!.getRecordingInProgress(object : IAsyncDataCallback<RecordingInProgress>{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
            }

            override fun onReceive(data: RecordingInProgress) {
                callback.onReceive(data)
            }

        })
    }

    override fun getActiveChannel(): TvChannel {
        return activeChannel
    }

    override fun getRecordingInProgressTvChannel(): TvChannel {
        return pvrModule!!.getRecordingInProgressTvChannel()!!
    }

    override fun recordingInProgress(): Boolean {
        return pvrModule!!.isRecordingInProgress()
    }

    override fun showDetailsScene(tvEvent: TvEvent) {
        context!!.runOnUiThread {
            worldHandler!!.triggerAction(id, Action.HIDE)

            tvEvent.tvChannel.isLocked = tvModule!!.isChannelLocked(tvEvent.tvChannel.id)

            val sceneData = SceneData(id, instanceId, tvEvent)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DETAILS_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun initConfigurableKeys() {
    }

    override fun onTimeChanged(currentTime: Long) {
        //Refresh time info every minute
        if (TimeUnit.MILLISECONDS.toSeconds(currentTime) % 60 == 0L) {
            if (tvModule != null) {
                var activeChannel = tvModule!!.getChannelList()[tvModule!!.getDesiredChannelIndex()]
                scene?.refresh(timeModule!!.getCurrentTime(activeChannel))
            } else {
                scene?.refresh(currentTime)
            }
        }
    }

    override fun onSceneInitialized() {
        channelsList = tvModule!!.getChannelList()
        tvModule!!.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(activeChannel: TvChannel) {
                this@InfoBannerSceneManager.activeChannel = activeChannel
                scene!!.refresh(activeChannel)
                for (i in 0 until (channelsList!!.size)) {
                    if (channelsList!!.get(i)!!.channelId == activeChannel!!.channelId) {
                        currentIndex = i
                        break
                    }
                }
                epgModule!!.getEventListByChannel(
                    activeChannel,
                    object : IAsyncDataCallback<ArrayList<TvEvent>> {
                        override fun onReceive(data: ArrayList<TvEvent>) {
                            eventsList.clear()
                            eventsList.addAll(data)
                            scene!!.refresh(eventsList)
                        }

                        override fun onFailed(error: Error) {
                            scene!!.refresh(ArrayList<TvEvent>())
                        }
                    })

                val currentTime = timeModule?.getCurrentTime(this@InfoBannerSceneManager.activeChannel)
                scene?.refresh(currentTime)
            }
        })
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (event!!.type == Events.UPDATE_INFO_BANNER) {
            var channel = event.getData(0)
            scene!!.refresh(channel)
            epgModule!!.getEventListByChannel(
                channel as TvChannel,
                object : IAsyncDataCallback<ArrayList<TvEvent>> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: ArrayList<TvEvent>) {
                        ReferenceApplication.runOnUiThread(Runnable {
                            eventsList.clear()
                            eventsList.addAll(data)
                            scene!!.refresh(eventsList)
                        })
                    }
                })
        } else if (event!!.type == Events.ACTIVE_AUDIO_TRACK_REFRESHED) {
            tvModule!!.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(channel: TvChannel) {
                    ReferenceApplication.runOnUiThread(Runnable {
                        scene!!.refresh(channel)
                    })
                    epgModule!!.getEventListByChannel(
                        channel,
                        object : IAsyncDataCallback<ArrayList<TvEvent>> {
                            override fun onFailed(error: Error) {
                            }

                            override fun onReceive(data: ArrayList<TvEvent>) {
                                ReferenceApplication.runOnUiThread(Runnable {
                                    eventsList.clear()
                                    eventsList.addAll(data)
                                    scene!!.refresh(eventsList)
                                    (scene as InfoBannerScene).refreshActiveAudioTrack()
                                })
                            }
                        })
                }
            })
        } else if (event!!.type == Events.AUDIO_TRACKS_UPDATED) {
            (scene as InfoBannerScene).refreshAudioChannel()
        }
    }

    var localIter = 0

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onChannelDownPressed() {
        if (pvrModule!!.isRecordingInProgress()) showStopRecordingDialog(false)
        else if (timeshiftModule!!.isTimeShiftActive) showStopTimeShiftDialog(false)
        else channelDown()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onChannelUpPressed() {
        if (pvrModule!!.isRecordingInProgress()) showStopRecordingDialog(true)
        else if (timeshiftModule!!.isTimeShiftActive) showStopTimeShiftDialog(true)
        else channelUp()
    }

    private fun channelUp() {
        if (currentIndex == channelsList!!.size - 1) {
            currentIndex = 0;
        } else {
            currentIndex += 1;
        }
        var currentChannel = channelsList!![currentIndex]
        while ((!currentChannel.isBrowsable && !utilsModule.isThirdPartyChannel(currentChannel)) || currentChannel.isSkipped) {
            currentIndex += 1;
            localIter++
            if (currentIndex >= channelsList!!.size) {
                currentIndex = 0
            }
            currentChannel = channelsList!![currentIndex]
            if (localIter >= channelsList!!.size) {
                break
            }
        }
        localIter = 0
        onNextPreviousChannel(currentIndex)
    }

    private fun channelDown() {
        if (currentIndex == 0) {
            currentIndex = channelsList!!.size - 1;
        } else {
            currentIndex -= 1;
        }

        var currentChannel = channelsList!![currentIndex]
        while ((!currentChannel.isBrowsable && !utilsModule.isThirdPartyChannel(currentChannel)) || currentChannel.isSkipped) {
            currentIndex -= 1;
            localIter++
            if (currentIndex < 0) {
                currentIndex = channelsList!!.size - 1;
            }
            currentChannel = channelsList!![currentIndex]
            if (localIter == channelsList!!.size) {
                break
            }
        }
        localIter = 0
        onNextPreviousChannel(currentIndex)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showStopRecordingDialog(channelUpClicked: Boolean) {
        val sceneData = DialogSceneData(id, instanceId)
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {}

            override fun onPositiveButtonClicked() {
                val recordingChannel = pvrModule!!.getRecordingInProgressTvChannel()
                pvrModule!!.stopRecordingByChannel(recordingChannel!!, object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "stop recording failed")
                    }

                    override fun onSuccess() {
                        ReferenceApplication.worldHandler!!.playbackState =
                            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                        PvrBannerSceneManager.previousProgress = 0L
                        pvrModule!!.setRecIndication(false)
                        if (channelUpClicked) channelUp()
                        else channelDown()
                    }
                })
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY
                )
            }
        }
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW, sceneData
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showStopTimeShiftDialog(channelUpClicked: Boolean) {
        val sceneData = DialogSceneData(id, instanceId)
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {}

            override fun onPositiveButtonClicked() {
                timeshiftModule?.timeShiftStop(object : IAsyncCallback {
                    override fun onFailed(error: Error) {}
                    override fun onSuccess() {
                        ReferenceApplication.worldHandler!!.playbackState =
                            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        ReferenceApplication.worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.INFO_BANNER,
                            Action.SHOW_OVERLAY
                        )
                        timeshiftModule!!.setTimeShiftIndication(false)
                        if (channelUpClicked) channelUp()
                        else channelDown()
                    }
                })
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY
                )
            }
        }
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW, sceneData
        )
    }

    private fun isActiveChannel(tvChannel: TvChannel): Boolean {
        return TvChannel.compare(activeChannel, tvChannel)
    }

    override fun isAudioSubtitleButtonPressed(): Boolean {
        return isAudioPressed || isCaptionsPressed
    }

    private fun createTuneToChannelForMoreInfoEvent(tvChannel: TvChannel): TvEvent {
        val calendar = Calendar.getInstance()
        val currentTime = getCurrentTime(tvChannel)
        calendar.time = Date(currentTime)
        calendar.add(Calendar.DATE, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        var startDate = Date(calendar.time.time)

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        var endDate = Date(calendar.time.time)
        return TvEvent.createTuneToChannelForMoreInformationEvent(
            tvChannel,
            startDate.time,
            endDate.time,
            currentTime,
            ConfigStringsManager.getStringById("tune_to") + " ${tvChannel.name} " +
                    ConfigStringsManager.getStringById("channel_for_more_info")
        )
    }

    private fun onNextPreviousChannel(channelIndex: Int) {
        val tvChannel = channelsList!!.get(channelIndex)
        val isActiveChannel = isActiveChannel(tvChannel)
        scene!!.refresh(tvChannel)
        tvModule!!.changeChannel(tvChannel, object: IAsyncCallback{
            override fun onFailed(error: Error) {}
            override fun onSuccess() {
                epgModule!!.getEventListByChannel(
                    tvChannel,
                    object : IAsyncDataCallback<ArrayList<TvEvent>> {
                        override fun onReceive(data: ArrayList<TvEvent>) {
                            eventsList.clear()
                            //Add tune to channel for more information event
                            if (BuildConfig.FLAVOR.contains("mtk") &&
                                !isActiveChannel && data.size == 1 && data[0].name == ConfigStringsManager.getStringById("no_information")) {
                                eventsList.add(createTuneToChannelForMoreInfoEvent(tvChannel))
                            } else {
                                eventsList.addAll(data)
                            }
                            scene!!.refresh(eventsList)
                        }

                        override fun onFailed(error: Error) {
                            eventsList.clear()
                            scene!!.refresh(eventsList)
                        }
                    }
                )
            }
        })
    }

    //TODO VASILISA DO WE NEED requestCurrentSubtitleTrack AND getCurrentSubtitleTrack
    override fun requestCurrentSubtitleTrack(callback: AsyncDataReceiver<ISubtitle>) {
        playerModule?.getActiveSubtitle()?.let { callback.onReceive(it) }
    }

    private fun refreshToNextChannel() {
        if (currentIndex == channelsList!!.size - 1) {
            currentIndex = 0;
        } else {
            currentIndex += 1;
        }
        refreshNextPreviousChannel(currentIndex)
    }

    private fun refreshToPreviousChannel() {
        if (currentIndex == 0) {
            currentIndex = channelsList!!.size - 1;
        } else {
            currentIndex -= 1;
        }
        refreshNextPreviousChannel(currentIndex)
    }

    private fun refreshNextPreviousChannel(channelIndex: Int) {
        val tvChannel = channelsList!!.get(channelIndex)
        scene!!.refresh(tvChannel)
        epgModule!!.getEventListByChannel(
            tvChannel!!,
            object : IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onReceive(data: ArrayList<TvEvent>) {
                    eventsList.clear()
                    eventsList.addAll(data)
                    scene!!.refresh(eventsList)
                }

                override fun onFailed(error: Error) {
                }
            })
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
        textToSpeechModule.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onKeyboardClicked() {
        ReferenceApplication.runOnUiThread {
            ReferenceApplication.worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.INFO_BANNER,
                Action.DESTROY
            )
            var sceneData = SceneData(id, instanceId)
            ReferenceApplication.worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.RCU_SCENE,
                Action.SHOW, sceneData
            )
        }

    }

    private fun startRecording(tvEvent: TvEvent, callback: IAsyncCallback) {
        worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY)
        if(getActiveChannel() == tvEvent.tvChannel && playerModule?.isOnLockScreen == true){
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            callback.onSuccess()
            return
        }
        if (getActiveChannel() != tvEvent.tvChannel && tvEvent.tvChannel.isLocked){
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            callback.onSuccess()
            return
        }
        if (!isUsbStorageAvailable()) {
            showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
            callback.onSuccess()
            return
        }

        if (!isUsbWritableReadable()) {
            showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
            callback.onSuccess()
            return
        }

        if (!isPvrPathSet()) {
            showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
            utils.information_bus.InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
            callback.onSuccess()
            return
        }
        if (!isUsbFreeSpaceAvailable()) {
            showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
            callback.onSuccess()
        }

        pvrModule!!.startRecordingByChannel(tvEvent.tvChannel, object: IAsyncCallback{
            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }

            override fun onSuccess() {
                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.INFO_BANNER,
                    Action.DESTROY
                )
                (scene as InfoBannerScene).widget!!.refreshRecordButton()
                callback.onSuccess()
            }
        })
    }

    private fun stopRecording(tvEvent: TvEvent) {
        InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHING)
        if (tvEvent.name == ConfigStringsManager.getStringById("no_info") && pvrModule!!.isRecordingInProgress()
        ) {
            pvrModule?.stopRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback{
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopRecording: Failed ${error.message}")
                    InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                }

                override fun onSuccess() {
                    (scene as InfoBannerScene).widget!!.refreshRecordButton()
                    InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                    utils.information_bus.InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
                }
            })
        } else if (isCurrentEvent(tvEvent)) {
            //Stop current recording
            pvrModule?.stopRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback{
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopRecording: Failed ${error.message}")
                    InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                }

                override fun onSuccess() {
                    (scene as InfoBannerScene).widget!!.refreshRecordButton()
                    InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                    utils.information_bus.InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
                }
            })
        }
    }

    override fun onRecordButtonClicked(tvEvent: TvEvent, callback: IAsyncCallback) {
        if (tvModule?.isSignalAvailable() == true) {
            //Current time
            val currentTime = timeModule!!.getCurrentTime(tvEvent.tvChannel)
            if (tvEvent.startTime > currentTime) {
                //future event
                schedulerModule!!.hasScheduledRec(
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
                                    if (!isUsbStorageAvailable()) {
                                        showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
                                        callback.onSuccess()
                                        return
                                    }

                                    if (!isUsbWritableReadable()) {
                                        showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
                                        callback.onSuccess()
                                        return
                                    }

                                    if (!isPvrPathSet()) {
                                        showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
                                        utils.information_bus.InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
                                        callback.onSuccess()
                                        return
                                    }
                                    if (!isUsbFreeSpaceAvailable()) {
                                        showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
                                        callback.onSuccess()
                                    }
                                    //not scheduled already
                                    schedulerModule?.scheduleRecording(
                                        obj,
                                        object :
                                            IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult> {
                                            override fun onFailed(error: Error) {
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
                                                        Log.d(Constants.LogTag.CLTV_TAG +
                                                            TAG, "onFailed: Reminder failed"
                                                        )
                                                    }
                                                }
                                            }

                                            override fun onReceive(scheduleData: SchedulerInterface.ScheduleRecordingResult) {
                                                utils.information_bus.InformationBus.submitEvent(
                                                    Event(Events.REFRESH_FOR_YOU, data)
                                                )
                                                utils.information_bus.InformationBus.submitEvent(
                                                    Event(
                                                        Events.UPDATE_REC_LIST_INDICATOR
                                                    )
                                                )
                                                callback.onSuccess()
                                            }
                                        })
                                }
                            } else {
                                //already scheduled, removing...
                                schedulerModule!!.removeScheduledRecording(
                                    obj,
                                    object : IAsyncCallback {
                                        override fun onFailed(error: Error) {
                                            callback.onFailed(error)
                                        }

                                        override fun onSuccess() {
                                            callback.onSuccess()
                                        }
                                    })
                            }
                        }
                    }
                )
            } else {
                if (isCurrentEvent(tvEvent)) {
                    if (!pvrModule!!.isRecordingInProgress()) {
                        if (!timeshiftModule!!.isTimeShiftActive) {
                            startRecording(tvEvent, callback)
                        } else {
                            InformationBus.informationBusEventListener.submitEvent(
                                Events.SHOW_RECORDING_TIME_SHIFT_CONFLICT_DIALOG,
                                arrayListOf(tvEvent)
                            )
                        }
                    } else {
                        stopRecording(tvEvent)
                    }
                    callback.onSuccess()
                }
            }
        } else showToast(ConfigStringsManager.getStringById("failed_to_start_pvr"))
    }

    override fun onAudioTrackClicked(audioTrack: IAudioTrack) {
        playerModule?.selectAudioTrack(audioTrack)
    }

    override fun onSubtitleTrackClicked(subtitleTrack: ISubtitle) {
        utilsModule.enableSubtitles(true)
        playerModule?.selectSubtitle(subtitleTrack)
    }

    private fun isNoInformationEvent(tvEvent: TvEvent): Boolean {
        return tvEvent.name == ConfigStringsManager.getStringById("no_information") ||
                tvEvent.name.startsWith(ConfigStringsManager.getStringById("tune_to"))
    }

    override fun addToWatchlist(tvEvent: TvEvent, callback: IAsyncCallback) {
        if ((TextUtils.isEmpty(tvEvent.name)
                    || isNoInformationEvent(tvEvent))
            && (TextUtils.isEmpty(tvEvent.longDescription)
                    || tvEvent.longDescription.equals(ConfigStringsManager.getStringById("no_information")))
            && (TextUtils.isEmpty(tvEvent.shortDescription))
            || tvEvent.shortDescription.equals(ConfigStringsManager.getStringById("no_information"))
        ) {

            showToast(ConfigStringsManager.getStringById("watchlist_scheduled_fail_toast"))

        } else {

            watchlistModule?.scheduleReminder(
                ScheduledReminder(
                    tvEvent.id,
                    tvEvent.name,
                    tvEvent.tvChannel,
                    tvEvent,
                    tvEvent.startTime,
                    tvEvent.tvChannel.id,
                    tvEvent.id
                ), object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        if (error!!.message == "alreadyPresent") {
                            showToast(ConfigStringsManager.getStringById("favorite_list_already_exist_2"))
                        } else if (error.message == "conflict") {
                            showToast(ConfigStringsManager.getStringById("watchlist_recording_conflict"))
                        } else {
                            Log.d(Constants.LogTag.CLTV_TAG + "DetailsSceneManager", "onFailed: Reminder failed")
                        }
                        callback.onFailed(error)
                    }

                    @RequiresApi(Build.VERSION_CODES.R)
                    override fun onSuccess() {
                        Log.d(Constants.LogTag.CLTV_TAG + "TAG", "onSuccess: scheduleReminder")
                        callback.onSuccess()
                    }
                })
        }
    }

    override fun removeFromWatchlist(tvEvent: TvEvent, callback: IAsyncCallback) {
        watchlistModule?.removeScheduledReminder(
            ScheduledReminder(
                tvEvent.id,
                tvEvent.name,
                tvEvent.tvChannel,
                tvEvent,
                tvEvent.startTime,
                tvEvent.tvChannel.id,
                tvEvent.id
            ), object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onSuccess() {
                    Log.d(Constants.LogTag.CLTV_TAG + "TAG", "onSuccess: removeScheduledReminder")
                    callback.onSuccess()
                }
            })
    }

    override fun onWatchlistClicked(tvEvent: TvEvent, callback: IAsyncCallback) {
        //TODO DEJAN
     //   callback.onSuccess()
        watchlistModule?.hasScheduledReminder(
            tvEvent,
            object : IAsyncDataCallback<Boolean> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(data: Boolean) {
                    if (!data) {
                        if ( (TextUtils.isEmpty(tvEvent.name)
                                    || isNoInformationEvent(tvEvent))
                            && ( TextUtils.isEmpty(tvEvent.longDescription)
                                    || tvEvent.longDescription.equals(ConfigStringsManager.getStringById("no_information")))
                            &&  ( TextUtils.isEmpty(tvEvent.shortDescription))
                            || tvEvent.shortDescription.equals(ConfigStringsManager.getStringById("no_information"))){

                            showToast(ConfigStringsManager.getStringById("watchlist_scheduled_fail_toast"))

                        } else {

                            watchlistModule?.scheduleReminder(
                                ScheduledReminder(
                                    tvEvent.id,
                                    tvEvent.name,
                                    tvEvent.tvChannel,
                                    tvEvent,
                                    tvEvent.startTime,
                                    tvEvent.tvChannel.id,
                                    tvEvent.id
                                ), object : IAsyncCallback {
                                    override fun onFailed(error: Error) {
                                        if (error!!.message == "alreadyPresent") {
                                            showToast(ConfigStringsManager.getStringById("favorite_list_already_exist_2"))
                                        } else if (error.message == "conflict") {
                                            showToast(ConfigStringsManager.getStringById("watchlist_recording_conflict"))
                                        } else {
                                            Log.d(Constants.LogTag.CLTV_TAG + "InfoSceneManager", "onFailed: Reminder failed")
                                        }
                                        callback.onFailed(error)
                                    }

                                    @RequiresApi(Build.VERSION_CODES.R)
                                    override fun onSuccess() {
                                        callback.onSuccess()
                                    }
                                })
                        }

                    } else {
                        watchlistModule?.removeScheduledReminder(
                            ScheduledReminder(
                                tvEvent.id,
                                tvEvent.name,
                                tvEvent.tvChannel,
                                tvEvent,
                                tvEvent.startTime,
                                tvEvent.tvChannel.id,
                                tvEvent.id
                            ),object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    callback.onFailed(error)
                                }

                                @RequiresApi(Build.VERSION_CODES.R)
                                override fun onSuccess() {
                                    callback.onSuccess()
                                }
                            })
                    }
                }
            })
    }

    override fun playCurrentEvent(tvChannel: TvChannel) {
        if (timeshiftModule?.isTimeShiftActive == true) {
            // Show time shift exit dialog before channel change
            context!!.runOnUiThread {
                var sceneData = DialogSceneData(id, instanceId)
                sceneData.type = DialogSceneData.DialogType.YES_NO
                sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
                sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                    override fun onNegativeButtonClicked() {
                    }

                    override fun onPositiveButtonClicked() {
                        timeshiftModule?.timeShiftStop(object : IAsyncCallback{
                            override fun onFailed(error: Error) {}

                            override fun onSuccess() {}

                        })
                        ReferenceApplication.worldHandler!!.playbackState =
                            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE

                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        timeshiftModule!!.setTimeShiftIndication(
                            false
                        )
                        tvModule!!.changeChannel(tvChannel, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                            }

                            override fun onSuccess() {
                            }
                        })
                    }
                }
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.SHOW, sceneData
                )
            }
        }
        else {
            tvModule!!.getActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {}

                override fun onReceive(data: TvChannel) {
                    ReferenceApplication.runOnUiThread {
                        if (tvChannel.id == data.id) {
                            onBackPressed()
                            utils.information_bus.InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, data))
                        } else {
                            tvModule!!.changeChannel(tvChannel, object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    showToast("Failed to start ${tvChannel.name} playback")
                                }

                                override fun onSuccess() {
                                    ReferenceApplication.runOnUiThread(Runnable {
                                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                                            ReferenceWorldHandler.SceneId.LIVE
                                        )
                                        var activeScene = ReferenceApplication.worldHandler!!.active
                                        var sceneData = SceneData(
                                            activeScene!!.id,
                                            activeScene.instanceId,
                                            tvChannel
                                        )
                                        ReferenceApplication.worldHandler!!.triggerActionWithData(
                                            ReferenceWorldHandler.SceneId.ZAP_BANNER,
                                            Action.SHOW_OVERLAY, sceneData
                                        )
                                    })
                                }
                            })
                        }
                    }
                }
            })
        }
    }

    override fun hasScheduledRecording(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>) {
        schedulerModule?.hasScheduledRec(tvEvent, callback)
    }
    override fun hasScheduledReminder(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>) {
        watchlistModule?.hasScheduledReminder(
            tvEvent,
            object : IAsyncDataCallback<Boolean> {
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onReceive(data: Boolean) {
                    Log.d(Constants.LogTag.CLTV_TAG + "TAG", "onReceive: future events")
                    callback.onReceive(data)
                }
            }
        )
    }

    override fun getCurrentAudioTrack(): IAudioTrack? {
        return playerModule?.getActiveAudioTrack()
    }

    override fun getCurrentSubtitleTrack(): ISubtitle? {
        return playerModule?.getActiveSubtitle()
    }

    override fun getAvailableAudioTracks(): MutableList<IAudioTrack>? {
        return playerModule?.getAudioTracks() as MutableList<IAudioTrack>?
    }

    override fun getAvailableSubtitleTracks(): MutableList<ISubtitle>? {
        return playerModule?.getSubtitleTracks() as MutableList<ISubtitle>?
    }

    override fun setSubtitles(isActive: Boolean) {
        if (!isActive) {
            playerModule?.selectSubtitle(null)
        } else {
            playerModule?.selectSubtitle(getCurrentSubtitleTrack())
        }
        utilsModule.enableSubtitles(isActive)
    }

    override fun getClosedCaptionSubtitlesState(): Boolean? {
        return closedCaptionModule?.getSubtitlesState()
    }

    override fun isSubtitlesEnabled(): Boolean {
        return utilsModule.getSubtitlesState()
    }

    override fun isClosedCaptionEnabled(): Boolean? {
        return closedCaptionModule?.isClosedCaptionEnabled()
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int) {
        closedCaptionModule?.saveUserSelectedCCOptions(ccOptions, newValue)
    }

    override fun getClosedCaption(): String? {
        return closedCaptionModule?.getClosedCaption()
    }

    override fun setClosedCaption(): Int? {
        return closedCaptionModule?.setClosedCaption()
    }

    override fun isCCTrackAvailable(): Boolean? {
        return closedCaptionModule?.isCCTrackAvailable()
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

    override fun onBackPressed(): Boolean {
        if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIALOG_SCENE)) {
            return false
        }
        if (isCaptionsPressed || isAudioPressed) {
            worldHandler!!.triggerAction(id, Action.DESTROY)
            worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.LIVE, Action.SHOW)
            return true
        }
        if ((scene as InfoBannerScene).widget!!.tracksWrapperLinearLayout!!.visibility == View.VISIBLE) {
            (scene as InfoBannerScene).widget!!.tracksWrapperLinearLayout!!.visibility = View.GONE
            return true
        }
        worldHandler!!.triggerAction(id, Action.DESTROY)
        return true
    }

    override fun defaultAudioClicked() {
        playerModule?.setDefaultAudioTrack()
    }
}