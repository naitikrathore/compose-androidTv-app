package com.iwedia.cltv.manager

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.player.MediaSessionControl
import com.iwedia.cltv.platform.model.player.MediaSessionControl.SPEED_FF_1X
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.timeshift.TimeshiftScene
import com.iwedia.cltv.scene.timeshift.TimeshiftSceneData
import com.iwedia.cltv.scene.timeshift.TimeshiftSceneListener
import com.iwedia.cltv.utils.Utils
import utils.information_bus.Event
import world.SceneData
import java.lang.Math.ceil
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Timeshift scene manager
 *
 * @author Dejan Nadj
 */
class TimeshiftSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var playerModule: PlayerInterface,
    var tvModule: TvInterface,
    val timeshiftModule: TimeshiftInterface,
    val utilsModule: UtilsInterface,
    val generalConfigModule: GeneralConfigInterface,
    val textToSpeechModule: TTSInterface
) : ReferenceSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE
), TimeshiftSceneListener {

    private val TAG = javaClass.simpleName

    /*
    * Time shift buffer size in minutes - 90 minutes
    */
    private var TIME_SHIFT_BUFFER_SIZE: Long = 90

    private var startTimeShift = false
    //startTimeShift

    /**
     * Time shift data
     */
    private var initialTime: Long? = null
    private var timeShiftPosition = 0L
    private var seekPosition = 0L
    private var timeShiftEndTime = 0L
    private var isSeeking = false
    private var timeShiftBufferReachedShowed = false
    private var currentTime = 0L
    private var isStarted = false

    private var nextTimeShiftBufferReached = false
    private var isJumpStateActive = false
    private var bufferTime  = 0L
    private var nextBufferCounter = 0
    private var nextTsStartPoint = 0L
    private var nextTsEndPoint = 0L
    private var wasLongPress = false
    private var repeatCountGlobal = 0
    private var dialogOpen = false

    private var SECOND_UNIT:Int = 1000
    private var endTime: Long = 0

    /**
     * Time shift Progress Move with 5 Sec
     */
    private var TS_PROGRESS_PERCENT:Int = 100
    private var TS_ADDITIONAL_PROGRESS_PERCENT: Double = 110.0
    private var JUMP_DUR_SECOND : Long = 5

    //Default callback
    private val timeShiftCallback = object : IAsyncCallback {
        override fun onFailed(error: Error) {
            println("timeShiftCallback onFailed error: ${error.message}")
            if(error.message == "No usb device detected"){
                ReferenceApplication.runOnUiThread {
                    error.message?.let {
                        showToast(it, UtilsInterface.ToastDuration.LENGTH_LONG)
                    }
                }
            }else if(error.message == "Please select storage path for Recording Timeshift in Preferences"){
                ReferenceApplication.runOnUiThread {
                    error.message?.let {
                        showToast(it, UtilsInterface.ToastDuration.LENGTH_LONG)
                    }
                }
            }
            scene!!.refresh(timeshiftModule.isTimeShiftPaused)
        }

        override fun onSuccess() {
            scene!!.refresh(timeshiftModule.isTimeShiftPaused)
        }
    }

    init {
        registerGenericEventListener(Events.AUDIO_TRACKS_SCENE_REFRESH)
        registerGenericEventListener(Events.SUBTITLE_TRACKS_SCENE_REFRESH)
        registerGenericEventListener(Events.SHOW_PLAYER_BANNER_EVENT)
        registerGenericEventListener(Events.USB_DEVICE_DISCONNECTED)
        registerGenericEventListener(Events.SHOW_PLAYER_SUBTITLE_LIST)
        registerGenericEventListener(Events.SHOW_PLAYER_AUDIO_LIST)
        registerGenericEventListener(Events.TIME_CHANGED_IN_TIMESHIFT)
        registerGenericEventListener(Events.ACTIVE_AUDIO_TRACK_REFRESHED)
        registerGenericEventListener(Events.SET_FILE_TO_USB)
        registerGenericEventListener(Events.STOP_TIMESHIFT)
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (event?.type == Events.AUDIO_TRACKS_SCENE_REFRESH) {
            val audioTracks: MutableList<IAudioTrack> = event.getData(0) as MutableList<IAudioTrack>
            ReferenceApplication.runOnUiThread {
                scene!!.refresh(audioTracks)
            }
        }
        else if (event?.type == Events.SUBTITLE_TRACKS_SCENE_REFRESH) {
            val subtitleTracks: MutableList<ISubtitle> = event.getData(0) as MutableList<ISubtitle>
            ReferenceApplication.runOnUiThread {
                scene!!.refresh(subtitleTracks)
            }
        } else if (event?.type == Events.SHOW_PLAYER_BANNER_EVENT) {
            (scene as TimeshiftScene).resetHideTimer()
        } else if (event?.type == Events.USB_DEVICE_DISCONNECTED) {
            if (timeshiftModule.isTimeShiftActive) {
                timeshiftModule.timeShiftStop(timeShiftCallback)
                ReferenceApplication.worldHandler!!.playbackState =
                    ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE

                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
                tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: TvChannel) {
                        tvModule.changeChannel(data, timeShiftCallback)
                    }
                })
                showTimeShiftNotAvailable(ConfigStringsManager.getStringById("timeshift_usb_removed_msg"))
            }
        } else if (event?.type == Events.SHOW_PLAYER_SUBTITLE_LIST) {
            (scene as TimeshiftScene).showSubtitleList()
        } else if (event?.type == Events.SHOW_PLAYER_AUDIO_LIST) {
            (scene as TimeshiftScene).showAudioList()
        }

        //this event is called when timeshift is started
        else if (event?.type == Events.TIME_CHANGED_IN_TIMESHIFT) {
            startTimeShift = true
            timeshiftModule.realPause(timeShiftCallback)
        }
        //this event is when usb needs to be set - no ts file on usb device
        else if (event?.type == Events.SET_FILE_TO_USB) {
            var currentActiveScene = ReferenceApplication.worldHandler!!.active
            var sceneData = DialogSceneData(currentActiveScene!!.id, currentActiveScene.instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("timeshift_file")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("yes")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("no")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onPositiveButtonClicked() {
                    timeshiftModule.registerFormatProgressListener(object : UtilsInterface.FormattingProgressListener{
                        override fun reportProgress(visible: Boolean, statusText: String) {}

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onFinished(isSuccess: Boolean) {
                            dialogOpen = false
                            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                        }
                    })
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onNegativeButtonClicked() {
                    dialogOpen = false
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                }
            }
            ReferenceApplication.runOnUiThread {
                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                dialogOpen = true
                ReferenceApplication.worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.SHOW_OVERLAY, sceneData
                )
            }
        }
        //this event track how much time has passed once ts is turned on - most code is taken from mtk branch since ts works well there
        else if (event?.type == Events.TIME_CHANGED) {
            if(!startTimeShift){
                return
            }
            val time = (event.data?.get(0) as Long)
            currentTime = time
            if (initialTime == null) {
                initialTime = time
                timeShiftEndTime = TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)
            }
            //Set time shift end time
            endTime = (time - initialTime!!) / SECOND_UNIT
            if ( !timeShiftBufferReachedShowed && endTime >= TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)) {
                //Show time shift buffer limit reached message
                ReferenceApplication.runOnUiThread {
                    showToast(ConfigStringsManager.getStringById("time_shift_buffer_size_reached_msg"))
                }
                if (timeShiftPosition < TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE))
                    timeShiftPosition += MediaSessionControl.getPlaybackSpeed()
                timeShiftBufferReachedShowed = true
            }
            if (endTime > TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)) {
                endTime =  TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)
                //Update End Position of Buffer
                if (timeShiftBufferReachedShowed) {
                    // Before buffer full, encountered Playback State PAUSE => nextBufferCounter > 0 ||  nextBufferCounter == 60
                    if (nextTsStartPoint > 0 && nextBufferCounter != TimeUnit.MINUTES.toSeconds(
                            TIME_SHIFT_BUFFER_SIZE
                        ).toInt()
                    ) {
                        nextBufferCounter++
                        // Reset End Point of Buffer, based on captured nextTsStartPoint
                        endTime =
                            (timeShiftEndTime - nextTsStartPoint) + TimeUnit.MINUTES.toSeconds(
                                TIME_SHIFT_BUFFER_SIZE
                            )
                    } else {
                        // Buffer is full, but not encountered Playback State PAUSE, hence endTime will be TIME_SHIFT_BUFFER_SIZE
                        endTime = TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)
                    }
                    nextTsEndPoint = endTime
                }

                if (!timeShiftBufferReachedShowed) {
                    resetSpeed()
                    //Show time shift buffer limit reached message

                    ReferenceApplication.runOnUiThread {
                        showToast(ConfigStringsManager.getStringById("time_shift_buffer_size_reached_msg"))

                    }
                    timeShiftBufferReachedShowed = true
                }

                if (nextBufferCounter == TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)
                        .toInt()
                ) {
                    if (timeshiftModule.isTimeShiftPaused) {
                        timeshiftModule.timeShiftPause(timeShiftCallback)
                        ReferenceApplication.runOnUiThread(Runnable {
                            (scene as TimeshiftScene).onTimeShiftStarted()
                        })
                        if (!nextTimeShiftBufferReached)
                            nextTimeShiftBufferReached = true
                    }
                }

                // Handler FF/FR limit Set
                if (timeShiftBufferReachedShowed) {
                    //Reset the Speed at the new start position of buffer
                    if (MediaSessionControl.getPlaybackSpeed() < 1 && nextTsStartPoint > timeShiftPosition) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, " Arrived at New Start Point of new buffer ")
                        resetSpeed()
                        return
                    }
                    // Update the Speed to control Auto set of FF / FR
                    val mappedPosition = MediaSessionControl.checkRequiredLimit(endTime, timeShiftPosition.toInt())
                    var startPosition: Int = -1
                    startPosition = if (!nextTimeShiftBufferReached) {
                        0
                    } else {
                        nextTsStartPoint.toInt()
                    }
                    if (timeShiftPosition > startPosition && MediaSessionControl.getPlaybackSpeed() != 1 && !mapSpeedFactor(
                            MediaSessionControl.getPlaybackSpeed(),
                            mappedPosition
                        )
                    ) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, " Reset speed, if new buffer  limit is not enough")
                        changeSpeed()
                    }
                }

                refreshTimeShiftTimeInfo(endTime)

                return
            }

            //Truncate time shift as timeShiftEndTime if exceeds
            if ((timeShiftPosition > timeShiftEndTime) && timeShiftEndTime != 0L) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Reached Buffer end point")
                resetSpeed()
                timeShiftPosition = timeShiftEndTime
                return
            }

            ReferenceApplication.runOnUiThread {
                (scene as TimeshiftScene).setEndTime(Utils.getTimeStringFromSeconds(endTime)!!)
                (scene as TimeshiftScene).setStartTime(
                    Utils.getTimeStringFromSeconds(
                        timeShiftPosition
                    )!!
                )
            }

            timeShiftEndTime = TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)

            //Reset the Speed at the beginning/ start position
            if (MediaSessionControl.getPlaybackSpeed() in timeShiftPosition..0 && initialTime != null) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Arrived at starting point")
                resetSpeed()
                return
            }

            // Update the Speed to control Auto set of FF / FR
            var mappedPosition = MediaSessionControl.checkRequiredLimit(endTime, timeShiftPosition.toInt())
            // var mappedPosition = checkRequiredLimit(timeShiftEndTime.toInt(), timeShiftPosition.toInt())
            if (timeShiftPosition > 0 && MediaSessionControl.getPlaybackSpeed() != 1 && !mapSpeedFactor(
                    MediaSessionControl.getPlaybackSpeed(), mappedPosition
                )
            ) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Reset speed, if buffer limit is not enough")
                changeSpeed()
            }
            bufferTime = endTime
            refreshTimeShiftTimeInfo(endTime)
            var progress: Double = 0.0
            if (timeShiftEndTime != 0L) {
                progress =
                    (endTime.toDouble() / timeShiftEndTime.toDouble()) * TS_PROGRESS_PERCENT
            }
            progress += 1
            ReferenceApplication.runOnUiThread(Runnable {
                (scene as TimeshiftScene).setBufferProgress(progress.toInt(), TS_PROGRESS_PERCENT)
            })

        }
        else if (event?.type == Events.STOP_TIMESHIFT) {
            stopTimeshift()
        }

    }

    override fun isTimeShiftAvailable(): Boolean {
        return playerModule.isTimeShiftAvailable
    }

    //stop ts mode - this only needed for mtk ref 5 tvs
    override fun stopTimeshift() {
        timeshiftModule.timeShiftStop(timeShiftCallback)
        timeshiftModule.stopTimeshift()
        timeshiftModule.setTimeShiftIndication(false)
        worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
    }

    override fun isDialogSceneOpenUsb(): Boolean {
        return dialogOpen
    }

    override fun getVideoResolution(): String {
        return playerModule.getVideoResolution()!!
    }

    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel>{
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: TvChannel) {
                callback.onReceive(data)
            }

        })
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule.getGeneralSettingsInfo(nameOfInfo)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
        textToSpeechModule.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    /**
     * Check if maximum play speed is possible as per evaluated buffer (buffer limit) from currentPosition
     * When programDurationMillis the length of program under playing.
     */
    private fun mapSpeedFactor(playbackSpeed: Int, position: Long) : Boolean {
        var validMove : Boolean = false
//        if(SPEED_FACTORS_MAPPING.containsKey(playbackSpeed)){
//            var minBufferPos= SPEED_FACTORS_MAPPING.getValue(playbackSpeed)
//            if(position > minBufferPos){
//                validMove = true
//            }
//        }
        return true//validMove
    }

    /**
     * Reset the playback speed to SPEED_FF_1X and update UI
     */
    private fun resetSpeed() {
        ReferenceApplication.runOnUiThread(Runnable {
            (scene as TimeshiftScene).updateTimeShiftSpeedText(SPEED_FF_1X)
        })
        MediaSessionControl.updatePlaybackSpeed(SPEED_FF_1X.toFloat())
    }

    override fun initConfigurableKeys() {
    }

    override fun onTimeChanged(currentTime: Long) {}

    override fun createScene() {
        scene = TimeshiftScene(context!!, this)
        startTimeShift = false
    }

    override fun onDestroy() {
        isStarted = false
        startTimeShift = false
        super.onDestroy()
    }

    override fun onPlayPauseClicked() {
        if (generalConfigModule.getGeneralSettingsInfo("timeshift")) {
            if (!Utils.isUsbConnected()) {
                showTimeShiftNotAvailable(ConfigStringsManager.getStringById("time_shift_usb_error_msg"))
            } else if (true){//(playerModule.isTimeShiftAvailable) { //todo - true
                resetSpeed()
                timeshiftModule.timeShiftPause(timeShiftCallback)
                isStarted = true
            } else {
                showTimeShiftNotAvailable(ConfigStringsManager.getStringById("time_shift_not_available_error_msg"))
            }
        }
    }

    //this function track how long on seek bar user holds left dpad
    override fun onPreviousClicked(isLongPress: Boolean, repeatCount: Int) {
        isJumpStateActive = true

        //track until when you can go back in seek bar
        var changePosition = timeShiftPosition - JUMP_DUR_SECOND
        var jumpDur = JUMP_DUR_SECOND
        if (changePosition < -1) {
            changePosition = 0
            jumpDur = 0
        }

        if (isLongPress) {
            if (!wasLongPress) {
//                timeshiftModule.pauseTimeShift(timeShiftCallback)
            }
            repeatCountGlobal = repeatCount
            // wasLongPress is added to keep track of how long we pressed KEYCODE_DPAD_LEFT and when it is released.
            wasLongPress = true
            // To smooth the seek bar below line is added.
            refreshTimeShiftProgress(timeShiftEndTime)
        }
        else if (wasLongPress) {
            // In below condition seeking till O second was an issue, so for now we are keeping it till 1 second.
            if (jumpDur == 0L) {
                timeshiftModule.timeShiftSeekBackward(
                    TimeUnit.SECONDS.toMillis(
                        SECOND_UNIT.toLong()
                    ),timeShiftCallback
                )
            } else {
                timeshiftModule.timeShiftSeekBackward(
                    TimeUnit.SECONDS.toMillis(
                        jumpDur * repeatCountGlobal
                    ),timeShiftCallback
                )
            }
            wasLongPress = false
            repeatCountGlobal = 0
//            timeshiftModule.resumeTimeShift(timeShiftCallback)
        }
        else {
            timeshiftModule.timeShiftSeekBackward(
                TimeUnit.SECONDS.toMillis(
                    jumpDur
                ),timeShiftCallback
            )
        }
        //always update ts position
        updateTimeShiftCurrentPosition(changePosition)
        isJumpStateActive = false
    }

    //this function track how long on seek bar user holds right dpad
    override fun onNextClicked(isLongPress: Boolean, repeatCount: Int) {
        isJumpStateActive = true

        //track until when you can go forward in seek bar
        var changePosition = timeShiftPosition + JUMP_DUR_SECOND
        var jumpDur = JUMP_DUR_SECOND
        val bufferEndPosition: Int = if (nextTimeShiftBufferReached) {
            nextTsEndPoint.toInt()
        } else {
            bufferTime.toInt()
        }

        if (changePosition >= bufferEndPosition) {
            jumpDur = changePosition - bufferEndPosition
            changePosition = bufferEndPosition.toLong()
        }
        if(changePosition >= endTime){
            changePosition = endTime
            jumpDur = endTime
        }

        if (isLongPress) {
            if (!wasLongPress) {
//                timeshiftModule.pauseTimeShift(timeShiftCallback)
            }
            repeatCountGlobal = repeatCount
            // wasLongPress is added to keep track of how long we pressed KEYCODE_DPAD_RIGHT and when it is released.
            wasLongPress = true
            refreshTimeShiftProgress(timeShiftEndTime)
        } else if (wasLongPress) {
            timeshiftModule.timeShiftSeekForward(
                TimeUnit.SECONDS.toMillis(
                    jumpDur * repeatCountGlobal
                ), timeShiftCallback
            )
            wasLongPress = false
            repeatCountGlobal = 0
//            timeshiftModule.resumeTimeShift(timeShiftCallback)
        }else {
//            timeshiftModule.pauseTimeShift(timeShiftCallback)
            timeshiftModule.timeShiftSeekForward(
                TimeUnit.SECONDS.toMillis(
                    jumpDur
                ),timeShiftCallback
            )
//            timeshiftModule.resumeTimeShift(timeShiftCallback)
        }

        updateTimeShiftCurrentPosition(changePosition)
        isJumpStateActive = false
    }

    private fun updateTimeShiftCurrentPosition(changePosition: Long) {
        val bufferEndPosition: Int = if (nextTimeShiftBufferReached) {
            nextTsEndPoint.toInt()
        } else {
            timeShiftEndTime.toInt()
        }

        timeShiftPosition = if (changePosition >= bufferEndPosition) {
            // Limit time shift position
            bufferEndPosition.toLong()
        } else if (changePosition < -1) {
            0
        } else {
            changePosition
        }
        setTimeShiftStartTime(timeShiftPosition)
    }

    private fun refreshTimeShiftProgress(endTime: Long) {
        if (!timeshiftModule.isTimeShiftPaused) {
            timeShiftPosition += MediaSessionControl.getPlaybackSpeed()
        }

        if (timeShiftPosition >= endTime) {
            timeShiftPosition = endTime
        }

        var progress =
            if (timeShiftPosition == TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)) TS_ADDITIONAL_PROGRESS_PERCENT
            else (timeShiftPosition.toDouble() / TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE).toDouble()) * 100.0


        ReferenceApplication.runOnUiThread {
            (scene as TimeshiftScene).setProgress(progress.toInt(), TS_PROGRESS_PERCENT)
        }
    }

    private fun setTimeShiftStartTime(timeShiftPosition: Long) {
        ReferenceApplication.runOnUiThread {
            (scene as TimeshiftScene).setStartTime(
                Utils.getTimeStringFromSeconds(
                    timeShiftPosition
                )!!
            )
        }
    }

    override fun onSeek(progress: Int) {
        val position = timeShiftEndTime.toDouble() * (progress.toDouble() / TS_PROGRESS_PERCENT)
        seekPosition = ceil(position).toLong()

        timeShiftPosition = seekPosition
        isSeeking = true
        if (!nextTimeShiftBufferReached) {
            timeShiftPosition = seekPosition
            isSeeking = true
            timeshiftModule.timeShiftSeekTo(
                TimeUnit.SECONDS.toMillis(
                    seekPosition
                ), timeShiftCallback
            )
        }

        CoroutineHelper.runCoroutineWithDelay({
            isSeeking = false
        }, 1000)
    }

    override fun onChannelUp() {
        if(startTimeShift) {
            showTimeShiftExitDialog(true)
        }else{
            tvModule.nextChannel(object : IAsyncCallback {
                override fun onFailed(error: Error) {
                }

                override fun onSuccess() {
                }
            })
        }
    }

    override fun onChannelDown() {
        if(startTimeShift) {
            showTimeShiftExitDialog(false)
        }else{
            tvModule.previousChannel(object : IAsyncCallback {
                override fun onFailed(error: Error) {
                }

                override fun onSuccess() {
                }
            })
        }
    }

    override fun onStopClicked() {
        if (startTimeShift) showTimeShiftExitDialog(null)
    }

    override fun isCloseDialogShown(): Boolean {
        return ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIALOG_SCENE) ||
                ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.CHANNEL_SCENE) ||
                ReferenceApplication.worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.INFO_BANNER)
    }

    override fun onAudioTrackClicked(audioTrack: IAudioTrack) {
        playerModule.selectAudioTrack(audioTrack)
    }

    override fun onSubtitleTrackClicked(subtitle: ISubtitle) {
        playerModule.selectSubtitle(subtitle)
    }

    override fun onLeftKeyPressed() {
        // Show channel list
        context!!.runOnUiThread {
            var sceneData = SceneData(id, instanceId)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.CHANNEL_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun onRightKeyPressed() {
        // Show info banner
        context!!.runOnUiThread {
            var sceneData = SceneData(id, instanceId)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.INFO_BANNER,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    override fun isActiveScene(): Boolean {
        return ReferenceApplication.worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE
    }

    override fun onFastForward(speed: Int) {
        var bufferEndPosition :Long  = 0
        val currentPosition :Int  = timeShiftPosition.toInt()

        bufferEndPosition = if (nextTimeShiftBufferReached){
            nextTsEndPoint
        }else{
            bufferTime
        }

        if (bufferEndPosition > 0 && !isJumpStateActive)   {
            val mappedPosition = MediaSessionControl.checkRequiredLimit(bufferEndPosition, currentPosition)
            val isValidMove = mapSpeedFactor(speed, mappedPosition)

            if (isValidMove) {
                if(timeshiftModule.isTimeShiftPaused){
                    timeshiftModule.timeShiftPause(object: IAsyncCallback{
                        override fun onFailed(error: Error) {
                            println(error.message)
                            return
                        }

                        override fun onSuccess() {
                            scene!!.refresh(timeshiftModule.isTimeShiftPaused)
                        }
                    })
                }
                MediaSessionControl.updatePlaybackSpeed(speed.toFloat())
                timeshiftModule.setTimeShiftSpeed(speed, object: IAsyncCallback{
                    override fun onFailed(error: Error) {
                        println(error.message)
                    }

                    override fun onSuccess() {
                        ReferenceApplication.runOnUiThread(Runnable {
                            (scene as TimeshiftScene).updateTimeShiftSpeedText(speed)
                        })
                    }
                })
            } else {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "FastForward not possible with speed $speed")
            }
        }
        else{
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "FastForward not possible ....")
        }
    }

    override fun onRewind(speed: Int) {
        val currentPosition :Long  = timeShiftPosition
        var startPosition :Int = 0

        if (nextTimeShiftBufferReached) {
            startPosition = nextTsStartPoint.toInt()
        }
        else {
            startPosition = 0
        }

        if (currentPosition > startPosition && !isJumpStateActive)   {
            val isValidMove = mapSpeedFactor(speed, currentPosition)
            if (isValidMove) {
                if(timeshiftModule.isTimeShiftPaused){
                    timeshiftModule.timeShiftPause(object: IAsyncCallback{
                        override fun onFailed(error: Error) {
                            println(error.message)
                            return
                        }

                        override fun onSuccess() {
                            scene!!.refresh(timeshiftModule.isTimeShiftPaused)
                        }
                    })
                }
                MediaSessionControl.updatePlaybackSpeed(speed.toFloat())
                timeshiftModule.setTimeShiftSpeed(speed, object : IAsyncCallback{
                    override fun onFailed(error: Error) {
                        println(error.message)
                    }

                    override fun onSuccess() {
                        ReferenceApplication.runOnUiThread(Runnable {
                            (scene as TimeshiftScene).updateTimeShiftSpeedText(speed)
                        })
                    }
                })
            } else {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Rewind not possible with speed $speed")
            }
        }else {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Rewind not possible ...")
        }
    }

    override fun changeSpeed() {
        timeshiftModule.setTimeShiftSpeed(SPEED_FF_1X, object : IAsyncCallback{
            override fun onFailed(error: Error) {
                println(error.message)
            }

            override fun onSuccess() {
                resetSpeed()
            }
        })
    }

    override fun requestCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
    }

    override fun requestCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun showHomeScene() {
        // Show HomeScene
        var sceneData = SceneData(id, instanceId)
        worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.HOME_SCENE,
            Action.SHOW_OVERLAY, sceneData
        )
    }

    override fun getAvailableSubtitleTracks(): MutableList<ISubtitle> {
        return playerModule.getSubtitleTracks() as MutableList<ISubtitle>
    }

    override fun getAvailableAudioTracks(): MutableList<IAudioTrack> {
        return playerModule.getAudioTracks() as MutableList<IAudioTrack>
    }

    override fun setSubtitles(isActive: Boolean) {
        if (!isActive) {
            playerModule?.selectSubtitle(null)
        } else {
            playerModule?.selectSubtitle(getCurrentSubtitleTrack())
        }
    }

    override fun getCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun getCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
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

    override fun getLanguageMapper(): LanguageMapperInterface {
        return utilsModule.getLanguageMapper()!!
    }

    override fun setIndicator(show: Boolean) {
        timeshiftModule.setTimeShiftIndication(show)
    }

    override fun isTimeshiftStarted(): Boolean {
        return isStarted
    }

    override fun isSubtitleEnabled(): Boolean {
        return utilsModule.getSubtitlesState()
    }

    override fun isSeeking(): Boolean {
        return isSeeking
    }

    override fun onSceneInitialized() {
        if (data != null && data is TimeshiftSceneData) {
            var sceneData = data as TimeshiftSceneData
            scene!!.refresh(sceneData.isPauseClicked)
            isStarted = sceneData.isPauseClicked
            if(isStarted){
                timeshiftModule.timeShiftPause(object : IAsyncCallback {
                    override fun onFailed(error: Error) {}
                    override fun onSuccess() {}
                })
            }
            if (sceneData.tvEvent != null) {
                (scene as TimeshiftScene).refresh(sceneData.tvEvent!!)
                (scene as TimeshiftScene).refresh(sceneData.tvEvent!!.tvChannel)
            } else {
                (scene as TimeshiftScene).refresh(null)
            }
            (scene as TimeshiftScene).setProgress(0, TS_PROGRESS_PERCENT)
            (scene as TimeshiftScene).setBufferProgress(0, TS_PROGRESS_PERCENT)
        }


        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: kotlin.Error) {}

            override fun onReceive(tvChannel: TvChannel) {
                scene!!.refresh(tvChannel)

                var audioTracks = playerModule.getAudioTracks()
                if (audioTracks != null) {
                    scene!!.refresh(audioTracks)
                }

                var subtitleTracks = playerModule.getSubtitleTracks()
                if (subtitleTracks != null) {
                    scene!!.refresh(subtitleTracks)
                }

                //Refresh active tracks
                scene!!.refresh(playerModule!!.getActiveAudioTrack())
                scene!!.refresh(playerModule!!.getSubtitleTracks())
            }
        })
    }

    override fun onBackPressed(): Boolean {
        /**
         * if timeshift is not started no need to keep scene hidden
         */
        if (!startTimeShift) {
            worldHandler!!.triggerAction(
                id,
                Action.DESTROY
            )
            timeshiftModule.setTimeShiftIndication(false)
        } else {
            showTimeShiftExitDialog(null)
            timeshiftModule.setTimeShiftIndication(false)
        }

        return true
    }

    private fun showTimeShiftExitDialog(channelUp: Boolean?) {
        var sceneData = DialogSceneData(id, instanceId)
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title =
            if (channelUp == null)
                ConfigStringsManager.getStringById("timeshift_exit_msg")
            else
                ConfigStringsManager.getStringById("timeshift_channel_change_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {
            }

            override fun onPositiveButtonClicked() {
                timeshiftModule.timeShiftStop(timeShiftCallback)

                ReferenceApplication.runOnUiThread {
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                    worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                }

                if (channelUp == null) {
                    //for some reason some time it does not want to re-tune on the same channel that timeshift was on
                    //playerModule.play((data as TimeshiftSceneData).tvEvent!!.tvChannel)

                    tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                        override fun onFailed(error: Error) {
                            tvModule.nextChannel(object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                }

                                override fun onSuccess() {
                                    timeshiftModule.setTimeShiftIndication(false)
                                }
                            })
                        }

                        override fun onReceive(data: TvChannel) {
                            tvModule.changeChannel(data, object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                }

                                override fun onSuccess() {
                                    timeshiftModule.setTimeShiftIndication(false)
                                }
                            })
                        }
                    })
                } else if (channelUp) {
                    tvModule.nextChannel(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                        }

                        override fun onSuccess() {
                            timeshiftModule.setTimeShiftIndication(false)
                        }
                    })
                } else {
                    tvModule.previousChannel(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                        }

                        override fun onSuccess() {
                            timeshiftModule.setTimeShiftIndication(false)
                        }
                    })
                }
            }
        }

        context!!.runOnUiThread {
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW, sceneData
            )
        }
    }

    private fun showTimeShiftNotAvailable(title: String) {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.TEXT
            sceneData.isBackEnabled = false
            sceneData.title = title
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                }

                override fun onPositiveButtonClicked() {
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                }
            }
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            timeshiftModule.setTimeShiftIndication(false)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW, sceneData
            )
        }
    }

    private fun refreshTimeShiftTimeInfo(endTime: Long) {
        if (!timeshiftModule.isTimeShiftPaused && isStarted) {
            timeShiftPosition += MediaSessionControl.getPlaybackSpeed()
        }
        if (timeShiftPosition >= endTime) {
            timeShiftPosition = endTime
        }

        //Case when buffer size is reached and time shift is paused
        if (timeShiftBufferReachedShowed && timeShiftPosition <= 0L) {
            if (timeshiftModule.isTimeShiftPaused) {
                timeshiftModule.timeShiftPause(timeShiftCallback)
                ReferenceApplication.runOnUiThread {
                    (scene as TimeshiftScene).onTimeShiftStarted()
                }
            }
        }
        timeShiftEndTime = TimeUnit.MILLISECONDS.toSeconds(currentTime - initialTime!!)
        timeShiftEndTime = minOf(timeShiftEndTime,TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE))
        val start =
            if (timeShiftBufferReachedShowed) timeShiftEndTime - endTime + timeShiftPosition else timeShiftPosition
        ReferenceApplication.runOnUiThread(Runnable {
            if (TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE) >= timeShiftEndTime) {
                (scene as TimeshiftScene).setEndTime(
                    Utils.getTimeStringFromSeconds(endTime)!!
                )
            } else {
                (scene as TimeshiftScene).setEndTime(
                    Utils.getTimeStringFromSeconds(timeShiftEndTime)!!
                )
            }
            (scene as TimeshiftScene).setStartTime(
                Utils.getTimeStringFromSeconds(start)!!
            )
        })

        // Below snippet will pause timeshift once buffer limit will reached and will show
        // scene to notify user that timeshift buffer limit is reached and ask whether user wants to
        // watch timeshift or go back to live playback.
        val tsProgress =
            (timeShiftPosition.toDouble() / TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)
                .toDouble()) * 100.0
        if (tsProgress > (TS_PROGRESS_PERCENT - 1) && !timeshiftModule.isTimeShiftPaused) {
            if (worldHandler?.active?.scene?.id == id
                || worldHandler?.active?.scene?.id == ReferenceWorldHandler.SceneId.TIMESHIFT_SCENE
            ) {
                ReferenceApplication.runOnUiThread {
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.TIMESHIFT_BUFFER_LIMIT_SCENE,
                        Action.SHOW_OVERLAY
                    )
                }
            }
            resetSpeed()
            timeshiftModule.timeShiftPause(timeShiftCallback)
            timeshiftModule.isTimeShiftPaused = true
            (scene as TimeshiftScene).setProgress(0, TS_PROGRESS_PERCENT)
        }

        var progress: Double = 0.0
        if (!timeShiftBufferReachedShowed) {
            progress =
                if (timeShiftPosition == TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)) 110.0
                else (timeShiftPosition.toDouble() / TimeUnit.MINUTES.toSeconds(
                    TIME_SHIFT_BUFFER_SIZE
                ).toDouble()) * 100.0
            if (!(scene as TimeshiftScene).isSeeking) {
                ReferenceApplication.runOnUiThread(Runnable {
                    (scene as TimeshiftScene).setProgress(progress.toInt(), TS_PROGRESS_PERCENT)
                })
            }
        } else {
            if ((timeShiftEndTime - initialTime!!) > TimeUnit.MINUTES.toSeconds(
                    TIME_SHIFT_BUFFER_SIZE
                )
            ) {
                if (!(scene as TimeshiftScene).isSeeking) {
                    ReferenceApplication.runOnUiThread(Runnable {
                        (scene as TimeshiftScene).setProgress(110, TS_PROGRESS_PERCENT)
                    })
                }
            } else {
                progress =
                    if (timeShiftPosition == TimeUnit.MINUTES.toSeconds(TIME_SHIFT_BUFFER_SIZE)) 110.0
                    else (timeShiftPosition.toDouble() / TimeUnit.MINUTES.toSeconds(
                        TIME_SHIFT_BUFFER_SIZE
                    ).toDouble()) * 100.0
                if (!(scene as TimeshiftScene).isSeeking) {
                    ReferenceApplication.runOnUiThread(Runnable {
                        (scene as TimeshiftScene).setProgress(progress.toInt(), TS_PROGRESS_PERCENT)
                    })
                }
            }
        }
    }
}