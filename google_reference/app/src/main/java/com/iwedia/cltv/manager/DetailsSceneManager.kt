package com.iwedia.cltv.manager

import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.PIN.PinSceneData
import com.iwedia.cltv.scene.details.DetailsScene
import com.iwedia.cltv.scene.details.DetailsSceneListener
import com.iwedia.cltv.scene.home_scene.guide.HorizontalGuideSceneWidget
import com.iwedia.cltv.scene.player_scene.PlayerSceneData
import com.iwedia.cltv.utils.PinHelper
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneManager
import kotlinx.coroutines.Dispatchers
import listeners.AsyncDataReceiver
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * DetailsSceneManager
 *
 * @author Aleksandar Milojevic
 */
class DetailsSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var tvModule: TvInterface,
    var playerModule: PlayerInterface,
    var pvrModule: PvrInterface,
    var favoriteModule: FavoritesInterface,
    var watchlistModule: WatchlistInterface,
    var schedulerModule: SchedulerInterface,
    var utilsModule: UtilsInterface,
    var tvInputModule: TvInputInterface,
    var closedCaptionModule: ClosedCaptionInterface,
    var timeShiftModule: TimeshiftInterface,
    var timeModule: TimeInterface,
    var categoryModule: CategoryInterface,
    var inputSourceModule: InputSourceInterface,
    var generalConfigModule: GeneralConfigInterface,
    private val textToSpeechModule: TTSInterface
) :
    GAndroidSceneManager(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.DETAILS_SCENE
    ), DetailsSceneListener {


    private val TAG = javaClass.simpleName
    var prevSceneId = 0

    private lateinit var activeChannel: TvChannel

    override fun createScene() {
        scene = DetailsScene(context!!, this)
        registerGenericEventListener(Events.AUDIO_TRACKS_SCENE_REFRESH)
        registerGenericEventListener(Events.SUBTITLE_TRACKS_SCENE_REFRESH)
        ReferenceApplication.worldHandler!!.isEnableUserInteraction = false
        registerGenericEventListener(Events.AUDIO_TRACKS_UPDATED)
    }

    override fun isGtvMode(): Boolean {
        return !inputSourceModule.isBasicMode()
    }

    override fun getDateTimeFormat(): DateTimeFormat {
        return utilsModule.getDateTimeFormat()
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule.getGeneralSettingsInfo(nameOfInfo)
    }

    override fun getVideoResolutionForRecoding(recording: Recording): String {
        return utilsModule.getVideoResolutionForRecording(recording)
    }

    override fun isHOH(type: Int): Boolean {
        return playerModule!!.getIsCC(type)
    }

    override fun getTTX(type: Int): Boolean {
        return playerModule.getTeleText(type)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (ReferenceApplication.worldHandler!!.isVisible(id)) {

            if (event!!.type == Events.SUBTITLE_TRACKS_SCENE_REFRESH) {
                var subtitleTracks: MutableList<ISubtitle> =
                    event.getData(0) as MutableList<ISubtitle>

                ReferenceApplication.runOnUiThread {
                    scene!!.refresh(subtitleTracks)
                }

            } else if (event!!.type == Events.AUDIO_TRACKS_SCENE_REFRESH) {
                var audioTracks: MutableList<IAudioTrack> =
                    event.getData(0) as MutableList<IAudioTrack>

                ReferenceApplication.runOnUiThread {
                    scene!!.refresh(audioTracks)
                }
            } else if (event!!.type == Events.AUDIO_TRACKS_UPDATED) {
                (scene as DetailsScene).refreshAudioChannel()
            }
        }
    }

    private fun isNoInformationEvent(tvEvent: TvEvent): Boolean {
        return tvEvent.name == ConfigStringsManager.getStringById("no_information") ||
                tvEvent.name.startsWith(ConfigStringsManager.getStringById("tune_to"))
    }

    override fun onWatchlistAddClicked(tvEvent: TvEvent, callback: IAsyncCallback) {
        Log.w(
            "DetailsSceneManager",
            "DetailsSceneManager : \n Watch List name [${tvEvent.name} ] " +
                    " \n shortDescription  [${tvEvent.shortDescription} ]" +
                    " \n longDescription  [${tvEvent.longDescription} ]"
        );

        if ((TextUtils.isEmpty(tvEvent.name)
                    || isNoInformationEvent(tvEvent))
            && (TextUtils.isEmpty(tvEvent.longDescription)
                    || tvEvent.longDescription.equals(ConfigStringsManager.getStringById("no_information")))
            && (TextUtils.isEmpty(tvEvent.shortDescription))
            || tvEvent.shortDescription.equals(ConfigStringsManager.getStringById("no_information"))
        ) {

            showToast(ConfigStringsManager.getStringById("watchlist_scheduled_fail_toast"))
            callback.onFailed(Error("Insufficient data for watchlist"))

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
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: scheduleReminder")
                        (scene as DetailsScene).refreshWatchlistButton(tvEvent)
                        Timer().schedule(timerTask {
                            InformationBus.submitEvent(Event(Events.REFRESH_FOR_YOU, data))
                            InformationBus.submitEvent(Event(Events.UPDATE_WATCHLIST_INDICATOR))
                        }, 200)

                        callback.onSuccess()
                    }
                })
        }
    }

    override fun onWatchlistRemoveClicked(tvEvent: TvEvent, callback: IAsyncCallback) {
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
                    callback.onFailed(error)
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onSuccess() {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: removeScheduledReminder")
                    (scene as DetailsScene).refreshWatchlistButton(tvEvent)
                    Timer().schedule(timerTask {
                        InformationBus.submitEvent(Event(Events.REFRESH_FOR_YOU, data))
                        InformationBus.submitEvent(Event(Events.UPDATE_WATCHLIST_INDICATOR))
                    }, 200)
                    callback.onSuccess()
                }
            })
        return
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRecordButtonClicked(tvEvent: TvEvent, callback: IAsyncCallback) {
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
                                    if (!utilsModule.isUsbWritableReadable()) {
                                        showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
                                        callback.onSuccess()
                                        return
                                    }
                                    if (utilsModule.getPvrStoragePath().isEmpty()) {
                                        showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
                                        InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
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
                                                InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
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
                                            InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
                                            callback.onSuccess()
                                        }
                                    })
                            }
                        }
                    }
                )
            } else {
                val isActiveChannel = TvChannel.compare(tvEvent.tvChannel, activeChannel)
                if (isCurrentEvent(tvEvent)) {
                    if (!pvrModule.isRecordingInProgress()) {
                        if (!timeShiftModule.isTimeShiftActive) {
                            startRecording(tvEvent, callback)
                        } else {
                            com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                                Events.SHOW_RECORDING_TIME_SHIFT_CONFLICT_DIALOG,
                                arrayListOf(tvEvent)
                            )
                        }
                    } else {
                        if (isActiveChannel) stopRecording(tvEvent)
                        else {
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

    override fun onButtonClick(buttonType: ButtonType, obj: Any, callback: IAsyncCallback) {
        when (buttonType) {
            ButtonType.START_OVER,
            ButtonType.CONTINUE_WATCH,
            ButtonType.WATCH -> {
                if (obj is TvEvent) {
                    playTvChannel(obj.tvChannel)
                    callback.onSuccess()
                }
                else if (obj is Recording) {
                    if ((playerModule.isParentalActive || obj.tvChannel!!.isLocked || obj.isEventLocked) && (tvInputModule.isParentalEnabled())) {
                        PinHelper.setPinResultCallback(object : PinHelper.PinCallback {
                            override fun pinCorrect() {
                                //Hide the lockedLayout LiveScene.
                                tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                                    override fun onFailed(error: Error) {
                                    }
                                    override fun onReceive(activeChannel: TvChannel) {
                                        if (activeChannel.isLocked || playerModule.isParentalActive) {
                                            //Hide the lockedLayout LiveScene.
                                            com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                                                Events.GOOGLE_PIN_CORRECT
                                            )
                                        }
                                    }
                                })

                                if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.TIME_SHIFT) {
                                    showTimeShiftExitDialog(buttonType.text, obj, true)
                                    callback.onSuccess()
                                } else {
                                    playPVR(buttonType.text, obj, true, true, callback)
                                }
                            }

                            override fun pinIncorrect() {}
                        })

                        val title =
                            ConfigStringsManager.getStringById("enter_parental_settings")
                        PinHelper.startPinActivity(title, "")

                    }
                    else {
                        //If current channel is locked ,lockedLayout (LiveScene) will impact the pvr play ,hide the layout
                        tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                            override fun onFailed(error: Error) {
                            }
                            override fun onReceive(activeChannel: TvChannel) {
                                if (activeChannel.isLocked || playerModule.isParentalActive) {
                                    //Hide the lockedLayout LiveScene.
                                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus.informationBusEventListener.submitEvent(
                                        Events.GOOGLE_PIN_CORRECT
                                    )
                                }
                            }
                        })
                        if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.TIME_SHIFT) {
                            showTimeShiftExitDialog(buttonType.text, obj, false)
                        } else {
                            playPVR(buttonType.text, obj, false, true, callback)
                        }
                    }
                }
                return
            }
            ButtonType.ADD_TO_FAVORITES,
            ButtonType.EDIT_FAVORITES -> {
                (scene as DetailsScene).showFavoritesOverlay()
                callback.onSuccess()
                return
            }
            ButtonType.DELETE -> {
                if (obj is Recording) {
                    context!!.runOnUiThread {
                        var sceneData = DialogSceneData(id, instanceId)
                        sceneData.type = DialogSceneData.DialogType.YES_NO
                        sceneData.title =
                            ConfigStringsManager.getStringById("remove_recording_dialog_msg")
                        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
                        sceneData.dialogClickListener =
                            object : DialogSceneData.DialogClickListener {
                                override fun onNegativeButtonClicked() {}

                                override fun onPositiveButtonClicked() {
                                    worldHandler!!.triggerAction(
                                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                        Action.DESTROY
                                    )
                                    removeRecording(obj,callback)
                                }
                            }
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                            Action.SHOW_OVERLAY, sceneData
                        )
                        callback.onSuccess()
                    }
                }
            }
            ButtonType.RENAME -> {
                if (obj is Recording) {
                    val sceneData = SceneData(id, instanceId, object : AsyncDataReceiver<String> {
                        override fun onFailed(error: core_entities.Error?) {
                            callback.onFailed(Error(error?.message))
                        }

                        override fun onReceive(data: String) {
                            if (!data.isNullOrEmpty()) {
                                renameRecording(obj, data)
                            }
                            callback.onSuccess()
                        }
                    }, obj.name)
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.RENAME_RECORDING_SCENE,
                        Action.SHOW_OVERLAY, sceneData
                    )
                }
            }
            //fire intent to search using the assistant by tvEvent title
            ButtonType.SEARCH_WITH_TEXT -> {
                val myIntent = Intent("android.search.action.GLOBAL_SEARCH")

                when (obj) {
                    is TvEvent -> {
                        myIntent.putExtra("query", obj.name)
                    }
                    else -> {
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onButtonClick: Mismatched obj type. May need to handle the scenario")
                        return
                    }
                }
                myIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    ReferenceApplication.applicationContext().startActivity(myIntent)
                    callback.onSuccess()
                    playerModule.stop()
                } catch (e: Throwable) {
                    if(!tvModule.playbackStatusInterface.isNetworkAvailable){
                        showToast(ConfigStringsManager.getStringById("no_internet_search_message"))
                    }
                    callback.onFailed(java.lang.Error("Unable to search"))
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onButtonClick: Unable to search")
                }
            }
            else -> {}
        }
    }

    private fun continueWatchContent(
        obj: Any,
        callback: IAsyncCallback,
        buttonType: ButtonType
    ) {
        if (obj is TvEvent) {
            playTvChannel(obj.tvChannel)
            callback.onSuccess()
        } else if (obj is Recording) {
            if ((playerModule.isParentalActive
                        || obj.tvChannel!!.isLocked) && (tvInputModule.isParentalEnabled())
            ) {
                ReferenceApplication.runOnUiThread {
                    val sceneData = PinSceneData(
                        ReferenceApplication.worldHandler!!.active!!.id,
                        ReferenceApplication.worldHandler!!.active!!.instanceId,
                        obj,
                        object : PinSceneData.PinSceneDataListener {
                            override fun onPinSuccess() {
                                //Hide the lockedLayout LiveScene.
                                InformationBus.submitEvent(
                                    Event(
                                        Events.PLAYBACK_STATUS_MESSAGE_NONE,
                                        LiveManager.LiveSceneStatus(LiveManager.LiveSceneStatus.Type.PVR_UNLOCK)
                                    )
                                )

                                if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.TIME_SHIFT) {
                                    showTimeShiftExitDialog(buttonType.text, obj, true)
                                    callback.onSuccess()
                                } else {
                                    playPVR(buttonType.text, obj, true, true, callback)
                                }
                            }

                            override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
                                utilsModule.showToast(text, duration)
                            }
                        })

                    ReferenceApplication.worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.PIN_SCENE,
                        Action.SHOW_OVERLAY, sceneData
                    )
                }
            } else {
                //If current channel is locked ,lockedLayout (LiveScene) will impact the pvr play ,hide the layout
                tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(activeChannel: TvChannel) {
                        if (activeChannel.isLocked) {
                            //Hide the lockedLayout LiveScene.
                            InformationBus.submitEvent(
                                Event(
                                    Events.PLAYBACK_STATUS_MESSAGE_NONE,
                                    LiveManager.LiveSceneStatus(LiveManager.LiveSceneStatus.Type.PVR_UNLOCK)
                                )
                            )
                        }
                    }
                })
                if (ReferenceApplication.worldHandler!!.playbackState == ReferenceWorldHandler.PlaybackState.TIME_SHIFT) {
                    showTimeShiftExitDialog(buttonType.text, obj, false)
                } else {
                    playPVR(buttonType.text, obj, false, true, callback)
                }
            }
        }
    }

    override fun getFavoritesCategories(callback: IAsyncDataCallback<ArrayList<String>>) {
        favoriteModule.getAvailableCategories(callback)
    }

    override fun isRecordingInProgress(): Boolean {
        return pvrModule.isRecordingInProgress()
    }

    override fun getRecordingInProgressTvChannel(): TvChannel {
        return pvrModule.getRecordingInProgressTvChannel()!!
    }

    override fun isSchedulerForRecording(tvEvent: TvEvent): Boolean {
        return false
    }

    override fun isInWatchlist(tvEvent: TvEvent): Boolean {
        return watchlistModule.isInWatchlist(tvEvent)
    }

    override fun isInRecList(tvEvent: TvEvent): Boolean {
        return schedulerModule.isInReclist(tvEvent.tvChannel.id, tvEvent.startTime)
    }

    override fun isDolby(type: Int): Boolean {
        return false
    }

    override fun isCC(type: Int): Boolean {
        return false
    }

    override fun isAudioDescription(type: Int): Boolean {
        return false
    }

    override fun getAvailableAudioTracks(): MutableList<IAudioTrack> {
        return playerModule.getAudioTracks() as MutableList<IAudioTrack>
    }

    override fun getAvailableSubtitleTracks(): MutableList<ISubtitle> {
        return playerModule.getSubtitleTracks() as MutableList<ISubtitle>
    }

    override fun getRecordingPlaybackPosition(id: Int): Long {
        return pvrModule.getPlaybackPosition(id)
    }

    override fun getPlaybackPositionPercent(recording: Recording): Double {
        return pvrModule.getPlaybackPositionPercent(recording)
    }

    override fun getRecordingChannelDisplayNumber(id: Int): String {
        tvModule.getChannelList().forEach { tvChannel ->
            if (tvChannel.id == id) {
                return tvChannel.displayNumber
            }
        }
        return "0"
    }

    override fun getFavoriteItemList(tvChannel: TvChannel): ArrayList<String> {
        tvModule.getChannelList().forEach {
            if (it.channelId == tvChannel.channelId) {
                return it.favListIds
            }
        }
        return arrayListOf()
    }

    override fun getPreviousSceneId(): Int {
        return prevSceneId
    }

    override fun getClosedCaptionSubtitlesState(): Boolean {
        return closedCaptionModule.getSubtitlesState()
    }

    override fun isSubtitlesEnabled(): Boolean {
        return utilsModule.getSubtitlesState()
    }

    override fun getIsCC(type: Int): Boolean {
        return playerModule!!.getIsCC(type)
    }

    override fun getIsAudioDescription(type: Int): Boolean {
        return playerModule!!.getIsAudioDescription(type)
    }

    override fun getIsDolby(type: Int): Boolean {
        return playerModule!!.getIsDolby(type)
    }

    override fun isTeleText(type: Int): Boolean {
        return playerModule!!.getTeleText(type)
    }

    override fun isClosedCaptionEnabled(): Boolean? {
        return closedCaptionModule.isClosedCaptionEnabled()
    }

    override fun setCCInfo() {
        closedCaptionModule.setCCInfo()
    }

    override fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int) {
        closedCaptionModule.saveUserSelectedCCOptions(ccOptions, newValue)
    }

    override fun getClosedCaption(): String? {
        return closedCaptionModule.getClosedCaption()
    }

    override fun getActiveChannel(): TvChannel {
        return activeChannel
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

    override fun isParentalEnabled(): Boolean {
        return tvModule.isParentalEnabled()
    }

    override fun getLanguageMapper(): LanguageMapperInterface {
        return utilsModule.getLanguageMapper()!!
    }

    override fun getVideoResolution(): String {
        return playerModule.getVideoResolution()!!
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

    override fun setSubtitles(isActive: Boolean) {
        if (!isActive) {
            playerModule.selectSubtitle(null)
        } else {
            playerModule.selectSubtitle(getCurrentSubtitleTrack())
        }
        utilsModule?.enableSubtitles(isActive)
    }

    override fun getCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun getCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
    }

    override fun setClosedCaption(): Int? {
        return closedCaptionModule.setClosedCaption()
    }

    override fun isCCTrackAvailable(): Boolean {
        return closedCaptionModule.isCCTrackAvailable()
    }

    private fun playTvChannel(tvChannel: TvChannel) {
        if (timeShiftModule.isTimeShiftActive) {
            showTimeShiftExitDialog(null, tvChannel, null)
        } else {
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: kotlin.Error) {
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onReceive(data: TvChannel) {
                    if (data.id == tvChannel.id && !((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()) {
                        //Set application mode 0 for DEFAULT mode
                        updatePostTuneValues()
                        InformationBus.submitEvent(Event(Events.CHANNEL_CHANGED, data))
                    } else {
                        tvModule.changeChannel(tvChannel, object: IAsyncCallback {
                            override fun onFailed(error: Error) {
                                if (error.message != "t56") {
                                    ReferenceApplication.runOnUiThread {
                                        showToast("Failed to start ${(tvChannel.name)} playback",)
                                    }
                                }
                            }

                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun onSuccess() {
                                //Set application mode 0 for DEFAULT mode
                                if(!((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()) updatePostTuneValues()
                            }
                        })
                    }
                }

            })
        }
    }

    private fun renameRecording(recording: Recording, name: String) {
        pvrModule.renameRecording(
            recording,
            name,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    ReferenceApplication.runOnUiThread(Runnable {
                        error.message?.let {
                            showToast(it)
                        }
                    })
                }

                override fun onSuccess() {
                    ReferenceApplication.runOnUiThread {
                        InformationBus.submitEvent(Event(Events.PVR_RECORDING_RENAMED, name, recording))
                    }
                    (scene as DetailsScene).setTitle(name)
                }
            })
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
        if (!utilsModule.isUsbWritableReadable()) {
            showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
            callback.onSuccess()
            return
        }
        if (utilsModule.getPvrStoragePath().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
            InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
            callback.onSuccess()
            return
        }
        if (!utilsModule.isUsbFreeSpaceAvailable()) {
            showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
            callback.onSuccess()
            return
        }

        if ((worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal)
            playTvChannel(tvEvent.tvChannel)
        pvrModule.startRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }

            override fun onSuccess() {
                (scene as DetailsScene).refreshRecordButton(tvEvent)
                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.PVR_BANNER_SCENE,
                    Action.SHOW_OVERLAY
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
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopRecording: Failed ${error!!.message}")
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                }

                override fun onSuccess() {
                    (scene as DetailsScene).refreshRecordButton(tvEvent)
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                    InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
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
                    (scene as DetailsScene).refreshRecordButton(tvEvent)
                    com.iwedia.cltv.platform.model.information_bus.events.InformationBus
                        .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
                    InformationBus.submitEvent(Event(Events.UPDATE_REC_LIST_INDICATOR))
                }
            })
        }
    }

    private fun removeRecording(recording: Recording, callback: IAsyncCallback) {
        pvrModule.removeRecording(
            recording,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    ReferenceApplication.runOnUiThread(Runnable {
                        error.message?.let {
                            showToast(it)
                        }
                    })
                    callback.onFailed(error)
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onSuccess() {
                    ReferenceApplication.runOnUiThread {
                        onBackPressed()
                    }
                    InformationBus.submitEvent(Event(Events.PVR_RECORDING_REMOVED))
                    callback.onSuccess()
                }
            })
    }

    override fun onFavoriteButtonPressed(
        tvChannel: TvChannel,
        favListIds: ArrayList<String>
    ) {
        val callback = object : IAsyncCallback {
            override fun onSuccess() {
                (scene as DetailsScene).refreshFavoriteButton()

                categoryModule.setActiveEpgFilter(HorizontalGuideSceneWidget.GUIDE_FILTER_ALL)
            }

            override fun onFailed(error: Error) {
            }
        }
        run exitForEach@{
            tvModule.getChannelList().forEach { channel ->
                if (tvChannel.channelId == channel.channelId) {
                    val favoriteItem = FavoriteItem(
                        channel.id,
                        FavoriteItemType.TV_CHANNEL,
                        getFavoriteItemList(channel),
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

    override fun onAudioTrackClicked(audioTrack: IAudioTrack) {
        playerModule.selectAudioTrack(audioTrack)
    }

    override fun onSubtitleTrackClicked(subtitle: ISubtitle) {
        utilsModule.enableSubtitles(true)
        playerModule.selectSubtitle(subtitle)
    }

    override fun requestCurrentSubtitleTrack(callback: AsyncDataReceiver<ISubtitle>) {
        playerModule.getActiveSubtitle()?.let { callback.onReceive(it) }
    }

    override fun onResume() {
        super.onResume()
        scene!!.refresh(data!!.getData())
        ReferenceApplication.worldHandler!!.isEnableUserInteraction = true
        //when we exit recording and go back to details scene, fast zap banner will be visible
        //underneath the details scene (when fast channel is active), if we press continue/start over
        //fast zap banner will be visible over the player scene
        val intent = Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
        ReferenceApplication.applicationContext().sendBroadcast(intent)
    }

    override fun onSceneInitialized() {
        try {
            worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.ZAP_BANNER,
                Action.DESTROY
            )
        }catch (E: Exception){
            println(E)
        }
        favoriteModule.setup()

        prevSceneId = data?.previousSceneId ?: 0

        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {

            }

            override fun onReceive(tvChannel: TvChannel) {
                this@DetailsSceneManager.activeChannel = tvChannel
                scene!!.refresh(tvChannel)
                if (data!!.getData() is TvEvent) {

                    if (tvChannel.id == (data!!.getData() as TvEvent).tvChannel.id) {
                        var tvEvent = (data!!.getData() as TvEvent)
                        if (isCurrentEvent(tvEvent)) {

                            //TODO DEJAN
                            var audioTracks =
                                playerModule.getAudioTracks()
                            if (audioTracks != null) {
                                scene!!.refresh(audioTracks)
                            }

                            var subtitleTracks =
                                playerModule.getSubtitleTracks()
                            if (subtitleTracks != null) {
                                scene!!.refresh(subtitleTracks)
                            }

                            //Refresh active tracks
                            var activeAudioTrack: IAudioTrack? = playerModule.getActiveAudioTrack()
                            if(activeAudioTrack != null){
                                scene!!.refresh(activeAudioTrack)
                            }

                            var activeSubtileTrack: ISubtitle? = playerModule.getActiveSubtitle()
                            if(activeSubtileTrack != null){
                                scene!!.refresh(activeSubtileTrack)
                            }
                        }
                    }
                }
            }
        })
        scene!!.refresh(data?.getData())
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
        textToSpeechModule.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
    }

    private fun showTimeShiftExitDialog(name: String?, obj: Any, isParental: Boolean?) {
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
                        override fun onFailed(error: Error) {
                        }

                        override fun onSuccess() {
                        }
                    })
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )

                    when (obj) {
                        is TvEvent -> {
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                            timeShiftModule.setTimeShiftIndication(
                                false
                            )

                            playerModule.resume()
                            playTvChannel(obj.tvChannel)

                        }
                        is Recording -> {
                            val playerSceneActive = false
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            timeShiftModule.setTimeShiftIndication(
                                false
                            )
                            playPVR(name, obj, isParental, playerSceneActive, null)

                        }
                        is TvChannel -> {
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                            timeShiftModule.setTimeShiftIndication(
                                false
                            )

                            playerModule.resume()
                            playTvChannel(obj)
                        }
                        else -> {
                            return
                        }
                    }
                }
            }
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    private fun mutePlayback(isMuted: Boolean) {
        if (ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE != worldHandler?.active?.id) {
            if (isMuted) {
                playerModule.mute()
            } else {
                playerModule.unmute()
            }
        }
    }

    private fun playPVR(
        name: String?,
        obj: Recording,
        isParental: Boolean?,
        playerSceneActive: Boolean?,
        callback: IAsyncCallback?
    ) {
        if (name == ConfigStringsManager.getStringById("start_over")) {
            // Reset playback position
            pvrModule.setPlaybackPosition(
                obj.id,
                0L
            )
        }
        pvrModule.startPlayback(
            obj,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    callback?.onFailed(error)
                }

                override fun onSuccess() {
                    // Go back to live
                    if (playerSceneActive == true) {
                        mutePlayback(false)
                        worldHandler!!.triggerAction(
                            id,
                            Action.HIDE
                        )
                    } else {
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.PLAYER_SCENE,
                            Action.DESTROY
                        )
                    }

                    val activeScene = worldHandler!!.active!!.scene
                    val sceneData = PlayerSceneData(activeScene!!.id, activeScene.instanceId)
                    sceneData.playerType = PlayerSceneData.PLAYER_TYPE_PVR
                    //TODO DEJAN
                    sceneData.recordedContent = obj
                    InformationBus.submitEvent(
                        Event(
                            Events.PVR_PLAYBACK_STARTED,
                            obj
                        )
                    )
                    ReferenceApplication.runOnUiThread(Runnable {
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.PLAYER_SCENE,
                            Action.SHOW_OVERLAY,
                            sceneData
                        )
                    })
                    callback?.onSuccess()
                }
            })
    }

    private fun updatePostTuneValues() {
        utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal)
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.HIDE)
        BackFromPlayback.zapFromHomeOrSearch = true
        BackFromPlayback.resetKeyPressedState()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBackPressed(): Boolean {
        if(getPreviousSceneId() == ReferenceWorldHandler.SceneId.PLAYER_SCENE || getPreviousSceneId() == ReferenceWorldHandler.SceneId.DIALOG_SCENE) {
            ReferenceApplication.worldHandler!!.triggerAction(id, Action.DESTROY)
            ReferenceApplication.worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.HOME_SCENE, Action.SHOW_OVERLAY)
        }
        else {
            ReferenceApplication.worldHandler!!.triggerAction(id, Action.DESTROY)
            ReferenceApplication.worldHandler!!.triggerAction(prevSceneId, Action.SHOW_OVERLAY)
        }
        if (getPreviousSceneId() ==  ReferenceWorldHandler.SceneId.HOME_SCENE) {
            return true
        }
        return super.onBackPressed()
    }

    override fun getPlatformName(): String {
        return (worldHandler as ReferenceWorldHandler).getPlatformName()
    }
}