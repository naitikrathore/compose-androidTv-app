package com.iwedia.cltv.platform.base

import android.content.Context
import android.media.PlaybackParams
import android.media.tv.TvView
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import java.util.Date
import java.util.Timer
import java.util.TimerTask

open class TimeshiftInterfaceBaseImpl(private var playerInterface: PlayerInterface, private var  utilsInterface: UtilsInterface, private var context: Context): TimeshiftInterface {

    private val TAG = javaClass.simpleName
    private var liveTvView: TvView? = null

    override var isTimeShiftActive: Boolean = false
    override var showTimeShiftIndication: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    override var isTimeShiftPaused = false
    override var selection: Int? = null

    protected var timeShiftStartPosition: Long = -1
    protected var timeShiftPosition = 0L

    /**
     * Time shift in background
     * This timer is used to calculate time when the time shift is running in the background (app is paused)
     */
    var timeShiftPositionCallback: ReferenceTimeShiftPositionCallback? = null
    var timeShiftInBackgroundTimer: Timer? = null
    var timeShiftInBackgroundTimerTask: TimerTask? = null
    var timeShiftInBackgroundDuration = 0L

    override fun registerFormatProgressListener(listener: UtilsInterface.FormattingProgressListener) {}

    override fun setTimeShiftIndication(show: Boolean) {
        showTimeShiftIndication.value = show
    }

    fun getLiveTvView(): TvView?{
        return liveTvView
    }

    override fun setLiveTvView(tvView: TvView){
        liveTvView = tvView
    }

    override fun setTimeShiftPositionCallback(){
        if(liveTvView == null){
              return
        }
        timeShiftPositionCallback = ReferenceTimeShiftPositionCallback()
        liveTvView!!.setTimeShiftPositionCallback(timeShiftPositionCallback)
    }

    /**
     * Time shift pause/resume
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun timeShiftPause(callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftPause: TIMESHIFT PUASE IN PLAYERHANDLER $isTimeShiftPaused")
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        if (isTimeShiftPaused) {
            liveTvView!!.timeShiftResume()
            isTimeShiftPaused = false
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftPause: LIVETVVIEW TIMESHIFT PAUSE CALLED")
            liveTvView!!.timeShiftPause()
            if (timeShiftPositionCallback == null) {
                setTimeShiftPositionCallback()
            }
            isTimeShiftPaused = true
        }
        isTimeShiftActive = true
        callback.onSuccess()
    }

    /**
     * Time shift in background
     * This timer is used to calculate time when the time shift is running in the background (app is paused)
     */
    private fun startTimeShiftInBackgroundTimer() {
        if (timeShiftInBackgroundTimer == null) {
            timeShiftInBackgroundTimer = Timer()
        }
        timeShiftInBackgroundDuration = 0L
        timeShiftInBackgroundTimerTask = object : TimerTask() {
            override fun run() {
                timeShiftInBackgroundDuration += 1000
            }
        }
        timeShiftInBackgroundTimer?.scheduleAtFixedRate(timeShiftInBackgroundTimerTask, 1000, 1000)
    }

    private fun stopTimeShiftInBackgroundTimer() {
        if (timeShiftInBackgroundTimer != null) {
            timeShiftInBackgroundTimer?.cancel()
            timeShiftInBackgroundTimer?.purge()
            timeShiftInBackgroundTimerTask?.cancel()
            timeShiftInBackgroundTimer = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun pauseTimeShift(callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "pauseTimeShift: PAUSE TIME IN TIMESHIFT")
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        if (isTimeShiftActive) {
            if (!isTimeShiftPaused) {
                startTimeShiftInBackgroundTimer()
            }
            liveTvView!!.timeShiftPause()
        }
        callback.onSuccess()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun resumeTimeShift(callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "resumeTimeShift: TIMESHIFT RESUME")
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        stopTimeShiftInBackgroundTimer()
        if (isTimeShiftActive) {
            liveTvView!!.timeShiftResume()
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                liveTvView!!.timeShiftSeekTo(timeShiftPosition + timeShiftInBackgroundDuration)
                InformationBus.informationBusEventListener?.submitEvent(Events.SHOW_PLAYER_BANNER_EVENT)
                timeShiftInBackgroundDuration = 0L
                if (isTimeShiftPaused) {
                    liveTvView!!.timeShiftPause()
                } else {
                    liveTvView!!.timeShiftResume()
                }
                callback.onSuccess()
            }, 1000)
        }else{
            callback.onSuccess()
        }
    }

    /**
     * Time shift stop
     * Stop time shift and resume live playback
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun timeShiftStop(callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftStop: TIMESHIFT STOP")
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        isTimeShiftPaused = false
        timeShiftPositionCallback = null
        timeShiftPosition = 0
        timeShiftStartPosition = -1L
        isTimeShiftActive = false
        liveTvView!!.setTimeShiftPositionCallback(null)

        //need to stop playback when exiting timeShift, otherwise playback will have black screen on exit.
        playerInterface.stop()
        if (playerInterface.activePlayableItem != null) {
            playerInterface.play(playerInterface.activePlayableItem!!)
        }
        callback.onSuccess()
    }

    /**
     * Time shift seek forward for a specified time in ms
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun timeShiftSeekForward(timeMs: Long, callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftSeekForward: TIMESHIFT SEEK FORWARD $timeMs")
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }

        liveTvView!!.timeShiftSeekTo(timeShiftPosition + timeMs)

        callback.onSuccess()
    }

    /**
     * Time shift seek backward for a specified time in ms
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun timeShiftSeekBackward(timeMs: Long, callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftSeekBackward: TIMESHIFT SEEK BACKWARDS $timeMs")
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        liveTvView!!.timeShiftSeekTo(timeShiftPosition - timeMs)
        callback.onSuccess()
    }


    /**
     * Time shift seek to a specified time in ms
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun timeShiftSeekTo(timeMs: Long, callback: IAsyncCallback) {
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        liveTvView!!.timeShiftSeekTo(timeShiftStartPosition + timeMs)
        callback.onSuccess()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun setTimeShiftSpeed(speed: Int, callback: IAsyncCallback) {
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        if (isTimeShiftActive) {
            val playbackParams = PlaybackParams()
            playbackParams.pitch = 1.0f
            playbackParams.speed = speed.toFloat()
            liveTvView!!.timeShiftSetPlaybackParams(playbackParams)
            callback.onSuccess()
        }
    }

    /**
     * Reference time shift position callback
     */
    @RequiresApi(Build.VERSION_CODES.M)
    inner class ReferenceTimeShiftPositionCallback : TvView.TimeShiftPositionCallback() {
        //Duration in seconds time shift position - start position
        private var duration: Long = 0

        override fun onTimeShiftCurrentPositionChanged(inputId: String?, timeMs: Long) {
            if (timeShiftStartPosition == -1L) {
                timeShiftStartPosition = timeMs
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Init Time shift start position ${Date(timeShiftStartPosition)}")
            }
            timeShiftPosition = timeMs
            duration = (timeShiftPosition - timeShiftStartPosition) / 1000
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Time shift current position ${Date(timeMs)}")
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Time shift duration ${duration}")
        }

        override fun onTimeShiftStartPositionChanged(inputId: String?, timeMs: Long) {
            super.onTimeShiftStartPositionChanged(inputId, timeMs)
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Time shift start position ${Date(timeMs)}")
        }
    }

    override fun reset(callback: IAsyncCallback){
        if(liveTvView == null){
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }

        liveTvView?.let {
            it.reset()
            it.setTimeShiftPositionCallback(null)
        }
        timeShiftPosition = 0
        timeShiftPositionCallback = null
        callback.onSuccess()

    }

    override fun stopTimeshift() {}

    override fun realPause(callback: IAsyncCallback) {}

}