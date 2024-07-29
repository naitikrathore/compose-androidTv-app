package com.iwedia.cltv.manager

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.player.MediaSessionControl.SPEED_FACTORS_MAPPING
import com.iwedia.cltv.platform.model.player.MediaSessionControl.SPEED_FF_1X
import com.iwedia.cltv.platform.model.player.MediaSessionControl.checkRequiredLimit
import com.iwedia.cltv.platform.model.player.MediaSessionControl.getPlaybackSpeed
import com.iwedia.cltv.platform.model.player.MediaSessionControl.updatePlaybackSpeed
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.live_scene.LiveSceneListener
import com.iwedia.cltv.scene.player_scene.PlayerScene
import com.iwedia.cltv.scene.player_scene.PlayerSceneData
import com.iwedia.cltv.scene.player_scene.PlayerSceneListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneManager
import utils.information_bus.Event
import utils.information_bus.InformationBus
import utils.information_bus.events.Events
import world.SceneData
import java.util.*
import kotlin.Error
import kotlin.math.abs

/**
 * PlayerSceneManager
 *
 * @author Aleksandar Milojevic
 */
class PlayerSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var pvrModule: PvrInterface,
    var playerModule: PlayerInterface,
    var tvModule: TvInterface,
    val utilsModule: UtilsInterface,
    val generalConfigModule: GeneralConfigInterface,
    val textToSpeechModule: TTSInterface
) :
    GAndroidSceneManager(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.PLAYER_SCENE
    ), PlayerSceneListener {

    private val TAG = javaClass.simpleName

    /**
     * Time shift data
     */
    private var isSeeking = false
    private var JUMP_DUR_SECOND : Long = 5
    private var SECOND_UNIT:Int = 1000

    /**
     * Pvr playback position
     */
    private var pvrPlaybackPosition = 0L

    private var isJumpStateActive = false

    var duration : Long = 0
    var currentPosition :Int = 0

    private var pvrSpeed = 1

    /**
     * Time shift Progress Move with 5 Sec till 720 (100%) to arrive at 3600 Secs -> 1hr
     */
    private var TS_PROGRESS_PERCENT:Int = 720
    private var wasLongPress = false

    override fun createScene() {
        scene = PlayerScene(context!!, this)
        registerGenericEventListener(Events.TIME_CHANGED,
            com.iwedia.cltv.platform.model.information_bus.events.Events.AUDIO_TRACKS_SCENE_REFRESH,
            com.iwedia.cltv.platform.model.information_bus.events.Events.SUBTITLE_TRACKS_SCENE_REFRESH,
            com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_STOP_TIME_SHIFT_DIALOG,
            com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_PLAYER_BANNER_EVENT,
            com.iwedia.cltv.platform.model.information_bus.events.Events.USB_DEVICE_DISCONNECTED,
            com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_PLAYER_SUBTITLE_LIST,
            com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_PLAYER_AUDIO_LIST)
    }

    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        when (event!!.type) {
            com.iwedia.cltv.platform.model.information_bus.events.Events.SUBTITLE_TRACKS_SCENE_REFRESH -> {
                val subtitleTracks: MutableList<ISubtitle> = event.getData(0) as MutableList<ISubtitle>
                ReferenceApplication.runOnUiThread {
                    scene!!.refresh(subtitleTracks)
                }
                return
            }

            com.iwedia.cltv.platform.model.information_bus.events.Events.AUDIO_TRACKS_SCENE_REFRESH -> {
                val audioTracks: MutableList<IAudioTrack> = event.getData(0) as MutableList<IAudioTrack>
                ReferenceApplication.runOnUiThread {
                    scene!!.refresh(audioTracks)
                }
                return
            }
            com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_PLAYER_BANNER_EVENT -> {
                (scene as PlayerScene).resetHideTimer()
            }
            com.iwedia.cltv.platform.model.information_bus.events.Events.USB_DEVICE_DISCONNECTED -> {
                onBackPressed()
            }
            com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_PLAYER_SUBTITLE_LIST -> {
                (scene as PlayerScene).showSubtitleList()
            }

            com.iwedia.cltv.platform.model.information_bus.events.Events.SHOW_PLAYER_AUDIO_LIST -> {
                (scene as PlayerScene).showAudioList()
            }
        }
    }

    private var finishedCreateScene = false
    private var returnData: Recording? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onSceneInitialized() {
        finishedCreateScene = false
        if (data != null && data is PlayerSceneData) {
            var sceneData = data as PlayerSceneData
            scene!!.refresh(sceneData.isPauseClicked)
            ReferenceApplication.worldHandler!!.playbackState = ReferenceWorldHandler.PlaybackState.PVR_PLAYBACK
            if(sceneData.recordedContent is Recording) {
                Log.d(Constants.LogTag.CLTV_TAG + "PLAYTEST", "is Recording")
                returnData = sceneData.recordedContent as Recording
                if (sceneData.recordedContent != null) {
                    (scene as PlayerScene).setData(sceneData.recordedContent!!)
                }
                var duration = (sceneData.recordedContent as Recording)!!.duration
                var isWatched =
                    pvrModule.getPlaybackPosition(
                        (sceneData.recordedContent as Recording)!!.id
                    ) != 0L
                Log.d(Constants.LogTag.CLTV_TAG + "PLAYTEST", "is isWatched $isWatched")
                if (isWatched) {
                    var position =
                        pvrModule.getPlaybackPosition(
                            (sceneData.recordedContent as Recording)!!.id
                        )

                    if(position >= duration){
                        position = 0
                    }
                    pvrPlaybackPosition = position

                    CoroutineHelper.runCoroutineWithDelay({
                        pvrModule.seekPlayback(
                            position, object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                }

                                override fun onSuccess() {
                                    pvrPlaybackPosition = position
                                    resetSpeed()
                                }
                            }
                        )
                        refreshPvrPlaybackProgress(position, duration)
                    }, 1000)
                }
                pvrModule.setPvrPositionCallback()
                pvrModule.pvrPlaybackCallback =
                    object : PvrInterface.PvrPlaybackCallback {
                        override fun onPlaybackPositionChanged(position: Long) {
                            Log.d(Constants.LogTag.CLTV_TAG + "PLAYTEST", "onPlaybackPositionChanged $position ${playerModule.isParentalActive}")
                            if(playerModule.isParentalActive) {
                                playerModule.requestUnblockContent(object : IAsyncCallback {
                                    override fun onFailed(error: Error) {
                                    }

                                    override fun onSuccess() {
                                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"success of request unblock")
                                    }

                                })
                            }
                            if (wasLongPress) {
                                return
                            }

                            /*
                                -Changing logic for continue watching as this was causing issue of unlimited playback,
                                 and user were never able to go back by seeking as 'if loop' was not considering 'position'
                                 value (coming in param.)
                                -For 3-6 seconds we get initial position(0, 1500...) of video even if playing video from mid of timeline,
                                 even we get correct value after 3-6 sec as 'pvrPlaybackPosition' will be always > position
                                 'if loop' will continue forever.
                            */

                            if (pvrSpeed < 0) {
                                if ((abs(pvrSpeed) * 100) > position) { //If time step in rewind is greater than position, start playback at normal speed
                                    onFastForward(1)
                                }
                            }
                            if (isWatched && (abs(pvrPlaybackPosition - position) > 1500)) {
                                pvrPlaybackPosition += SECOND_UNIT
                                if (pvrPlaybackPosition > duration) {
                                    refreshPvrPlaybackProgress(duration, duration)
                                    pvrModule.reset()
                                    pvrPlaybackPosition = 0
                                    returnToDetails()
                                    return
                                }
                                refreshPvrPlaybackProgress(pvrPlaybackPosition, duration)
                                return
                            }
                            isWatched = false
                            pvrPlaybackPosition = position
                            var isMovementAllowed =
                                (duration - pvrPlaybackPosition) > (getPlaybackSpeed() * SECOND_UNIT)
                            //Reset playback just before last seek
                            var nextPvrSeekPosition =
                                pvrPlaybackPosition + (getPlaybackSpeed() * SECOND_UNIT)
                            if (pvrPlaybackPosition >= duration || pvrPlaybackPosition <= 0 || !isMovementAllowed) {
                                resetSpeed()
                            }

                            val diff = duration - nextPvrSeekPosition
                            if (diff < SECOND_UNIT) {
                                refreshPvrPlaybackProgress(nextPvrSeekPosition, duration)
                                pvrModule.reset()
                                pvrPlaybackPosition = 0
                                returnToDetails()
                                return
                            }
                            refreshPvrPlaybackProgress(position, duration)
                        }
                    }
            }

            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel>{
                override fun onFailed(error: Error) {}

                override fun onReceive(tvChannel: TvChannel) {
                    scene!!.refresh(tvChannel)

                    var audioTracks =  playerModule.getAudioTracks()
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

            finishedCreateScene = true
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onPlayPauseClicked() {
        pvrModule.pausePlayPlayback(object : IAsyncCallback {
            override fun onFailed(error: Error) {}

            override fun onSuccess() {
                scene!!.refresh(pvrModule.isPvrPlaybackPaused)
                resetSpeed()
            }
        })
    }

    override fun onPauseClicked() {
        pvrModule.pausePlayback()
        resetSpeed()
    }

    override fun exitRecordings() {
        returnToDetails()
    }

    override fun getVideoResolution(): String {
        return playerModule.getVideoResolution()!!
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

    override fun onPreviousClicked(isLongPress : Boolean) {
        val sceneData = data as PlayerSceneData
        var changePosition = pvrPlaybackPosition - JUMP_DUR_SECOND * SECOND_UNIT
        val duration = (sceneData.recordedContent as Recording).duration
        if (changePosition <= SECOND_UNIT) {
            changePosition = SECOND_UNIT.toLong()
        }
        if (isLongPress) {
            isJumpStateActive = true
            wasLongPress = true
            pvrModule.pausePlayback()
            refreshPvrPlaybackProgress(changePosition, duration)
        }
        else if(wasLongPress) {
            pvrModule.seek(changePosition)
            pvrModule.resumePlayback()
            wasLongPress = false
            refreshPvrPlaybackProgress(changePosition, duration)
        }
        else{
            isJumpStateActive = false
            pvrModule.seek(changePosition)
            refreshPvrPlaybackProgress(changePosition, duration)
        }
        if(!isLongPress) {
            isJumpStateActive = false
        }
        pvrPlaybackPosition = changePosition
    }

    override fun onNextClicked(isLongPress : Boolean) {
        val sceneData = data as PlayerSceneData
        var changePosition = pvrPlaybackPosition + JUMP_DUR_SECOND * SECOND_UNIT
        val duration = (sceneData.recordedContent as Recording).duration
        if (changePosition >= duration) {
            changePosition = duration - 1
        }
        if (isLongPress) {
            wasLongPress = true
            isJumpStateActive = true
            pvrModule.pausePlayback()
            refreshPvrPlaybackProgress(changePosition, duration)
        } else if (wasLongPress) {
            isJumpStateActive = true
            pvrModule.seek(changePosition)
            pvrModule.resumePlayback()
            wasLongPress = false
            refreshPvrPlaybackProgress(changePosition, duration)
        } else {
            pvrModule.seek(changePosition)
            refreshPvrPlaybackProgress(changePosition, duration)
        }
        if(!isLongPress) {
            isJumpStateActive = false
        }
        pvrPlaybackPosition = changePosition
    }

    override fun onSeek(progress: Int) {
    }

    override fun onChannelDown() {
        if(finishedCreateScene) {
            //showPvrPlaybackExitDialog(false)
        }
    }

    override fun onChannelUp() {
        if(finishedCreateScene) {
            //showPvrPlaybackExitDialog(true)
        }
    }

    override fun onStopClicked() {
        returnToDetails()
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

    }

    override fun onRightKeyPressed() {

    }

    override fun showHomeScene() {

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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent?): String {
        val applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        return tvModule.getParentalRatingDisplayName(parentalRating, applicationMode, tvEvent!!)
    }

    override fun isPlayerActiveScene() :Boolean{
        return ReferenceApplication.worldHandler!!.active!!.id != ReferenceWorldHandler.SceneId.PLAYER_SCENE
    }

    override fun requestCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
    }

    override fun requestCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun onResume() {
        super.onResume()

        // To refresh channel info on setting changes
        val desiredChannelIndex = tvModule.getDesiredChannelIndex()

        val activeTvChannel = tvModule.getChannelByIndex(desiredChannelIndex)

        (scene as PlayerScene).updateChannelInfo(activeTvChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        ReferenceApplication.worldHandler!!.playbackState =
            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
        (scene!!.sceneListener as LiveSceneListener).channelDown()
        (scene!!.sceneListener as PlayerSceneListener).onChannelDown()
    }

    override fun onBackPressed(): Boolean {
        return true
    }

    override fun pvrPlaybackExit(channelUp: Boolean?) {
        pvrModule.setPlaybackPosition(
            ((data as PlayerSceneData).recordedContent as Recording).id, pvrPlaybackPosition
        )
        pvrModule.stopPlayback(object : IAsyncCallback {
            override fun onFailed(error: Error) {}
            override fun onSuccess() {}
        })
        isJumpStateActive = false
        (scene as PlayerScene).hidePlayer()
        worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DETAILS_SCENE, Action.SHOW)
        worldHandler!!.triggerAction(id, Action.DESTROY)
    }

    override fun showPvrPlaybackExitDialog(channelUp: Boolean?) {
        context!!.runOnUiThread {
            var sceneData = DialogSceneData(id, instanceId)
            sceneData.type = DialogSceneData.DialogType.YES_NO
            sceneData.title = ConfigStringsManager.getStringById("pvr_playback_exit_msg")
            sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
            sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
            sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                override fun onNegativeButtonClicked() {
                    // Show player scene
                    (scene as PlayerScene).resetHideTimer()
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )


                    ReferenceApplication.runOnUiThread(Runnable {
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.PLAYER_SCENE,
                            Action.SHOW_OVERLAY
                        )
                    })
                }

                override fun onPositiveButtonClicked() {
                    pvrModule.setPlaybackPosition(((data as PlayerSceneData)!!.recordedContent as Recording)!!.id, pvrPlaybackPosition)
                    pvrModule.stopPlayback(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}

                        override fun onSuccess() {
                        }
                    })

                    (scene as PlayerScene).hidePlayer()
                    worldHandler!!.triggerAction(
                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                        Action.DESTROY
                    )
                    val sceneData = SceneData(id, instanceId, returnData)
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.DETAILS_SCENE,
                        Action.SHOW, sceneData
                    )
                    worldHandler!!.triggerAction(id, Action.DESTROY)
                }
            }
            // Hide player scene
            (scene as PlayerScene).stopHideTimer()
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                Action.SHOW, sceneData
            )
        }
    }

    private fun returnToDetails() {
        pvrModule.setPlaybackPosition(
            ((data as PlayerSceneData).recordedContent as Recording).id, pvrPlaybackPosition
        )
        pvrModule.stopPlayback(object : IAsyncCallback {
            override fun onFailed(error: Error) {}
            override fun onSuccess() {}
        })
        (scene as PlayerScene).hidePlayer()
        worldHandler!!.triggerAction(
            ReferenceWorldHandler.SceneId.PLAYER_SCENE,
            Action.DESTROY
        )
        pvrModule.reset()
        worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DETAILS_SCENE, Action.SHOW)
        worldHandler!!.triggerAction(id, Action.DESTROY)
        //Show lockedLayout LiveScene,
        if (tvModule != null) {
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel>{
                override fun onFailed(error: Error) {
                }

                override fun onReceive(data: TvChannel) {
                    if (data.isLocked) {
                        InformationBus.submitEvent(
                            Event(
                                com.iwedia.cltv.platform.model.information_bus.events.Events.PLAYBACK_STATUS_MESSAGE_PVR_LOCK
                            )
                        )
                    }
                }
            })
        }
    }

    /**
     * Rewind time-shifting with the given speed.
     * If speed is valid value is ranged from currentPosition to 0 i.e evaluated using program speed factor.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRewind(speed: Int) {
        val currentPosition :Long  = (pvrPlaybackPosition / SECOND_UNIT).toLong()
        val isPlaybackPlaying =  !pvrModule.isPvrPlaybackPaused
        val isValidMove = mapSpeedFactor(speed, currentPosition)

        if (isValidMove && isPlaybackPlaying) {
            updatePlaybackSpeed(speed.toFloat())
            pvrModule.setPvrSpeed(speed)

            ReferenceApplication.runOnUiThread(Runnable {
                (scene as PlayerScene).refresh(pvrModule.isPvrPlaybackPaused)
                (scene as PlayerScene).updateTimeShiftSpeedText(speed)
            })
        } else {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "PVR : Rewind not possible with speed $speed")
        }
    }

    /**
     * Fast-forwards time-shifting with the given speed.
     * If speed is valid value is ranged from currentPosition to bufferEndPosition, evaluated using program speed factor.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onFastForward(speed: Int) {
        val sceneData = data as PlayerSceneData
        val pvrRecordedEndTime = (sceneData.recordedContent as Recording).duration
        val bufferEndPosition :Long  = pvrRecordedEndTime
        val currentPosition :Int  = (pvrPlaybackPosition / SECOND_UNIT).toInt()

        val  isPlaybackPlaying =  !pvrModule.isPvrPlaybackPaused

        if ( bufferEndPosition > 0 &&   !isJumpStateActive && isPlaybackPlaying)   {
            val mappedPosition = checkRequiredLimit(bufferEndPosition, currentPosition)
            val isValidMove = mapSpeedFactor(speed, mappedPosition)

            if (isValidMove && isPlaybackPlaying) {

                updatePlaybackSpeed(speed.toFloat())
                pvrModule.setPvrSpeed(speed)

                ReferenceApplication.runOnUiThread(Runnable {
                    (scene as PlayerScene).refresh(pvrModule.isPvrPlaybackPaused)
                    (scene as PlayerScene).updateTimeShiftSpeedText(speed)
                })

            } else {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "PVR - FastForward not possible with speed $speed")
            }
        }
    }

    /**
     * Reset the playback speed to SPEED_FF_1X and update UI
     */
    private fun resetSpeed() {
        ReferenceApplication.runOnUiThread(Runnable {
            (scene as PlayerScene).updateSpeedText(SPEED_FF_1X)
        })
        updatePlaybackSpeed(SPEED_FF_1X.toFloat())
    }

    /**
     * Forced update current program speed to SPEED_FF_1X.
     * If buffer limit is not enough (currentPosition to bufferEndPosition) while current program is being updated in a certain speed
     */
    override fun changeSpeed() {
        pvrModule.setPvrSpeed(SPEED_FF_1X)
        resetSpeed()
    }

    /**
     * Check if maximum play speed is possible as per evaluated buffer (buffer limit) from currentPosition
     * When programDurationMillis the length of program under playing.
     */
    private fun mapSpeedFactor(playbackSpeed: Int, position: Long) : Boolean {
        var validMove : Boolean = false
        if(SPEED_FACTORS_MAPPING.containsKey(playbackSpeed)){
            var minBufferPos= SPEED_FACTORS_MAPPING.getValue(playbackSpeed)
            if(position > minBufferPos){
                validMove = true
            }
        }
        return validMove
    }

    private fun refreshPvrPlaybackProgress(position: Long, duration: Long) {
        var progress: Double = 0.0
        if (duration != 0L) {
            progress =
                ((position).toDouble() / duration.toDouble()) * 100
        }
        ReferenceApplication.runOnUiThread(Runnable {
            (scene as PlayerScene).setStartTime(
                Utils.getTimeStringFromSeconds(
                    (position) / SECOND_UNIT
                )!!
            )
            (scene as PlayerScene).setProgress(progress.toInt(), TS_PROGRESS_PERCENT)
        })
    }

}