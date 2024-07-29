package com.iwedia.cltv.platform.mal_service

import android.media.PlaybackParams
import android.media.tv.TvView
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncFomattingProgressListener
import com.cltv.mal.model.async.IAsyncListener
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

class TimeshiftInterfaceImpl(
    private val serviceImpl: IServiceAPI,
    private val playerInterface: PlayerInterface,
    private val utilsInterface: UtilsInterface, override var selection: Int?
) : TimeshiftInterface {
    val TAG = javaClass.simpleName
    private var showTimeShiftInternal: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    private var liveTvView: TvView? = null
    private var startedTimeShift = false
    private var isTimeShiftPausedInternal = false
    override var isTimeShiftPaused: Boolean
        get() = isTimeShiftPausedInternal
        set(value) {
            isTimeShiftPausedInternal = value
        }
    override var isTimeShiftActive: Boolean = false

    /**
     * Time shift position callback
     */
    private var timeShiftPositionCallback: ReferenceTimeShiftPositionCallback? = null

    /**
     * Time shift start position
     */
    private var timeShiftStartPosition: Long = -1

    /**
     * Time shift position
     */
    private var timeShiftPosition = 0L

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

    /**
     * Time shift in background
     * This timer is used to calculate time when the time shift is running in the background (app is paused)
     */
    var timeShiftInBackgroundTimer: Timer? = null
    var timeShiftInBackgroundTimerTask: TimerTask? = null
    var timeShiftInBackgroundDuration = 0L

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

    override var showTimeShiftIndication: MutableLiveData<Boolean>
        get() = showTimeShiftInternal
        set(value) {
            showTimeShiftInternal.value = value.value
        }

    override fun registerFormatProgressListener(listener: UtilsInterface.FormattingProgressListener) {
        serviceImpl.registerFormatProgressListener(object : IAsyncFomattingProgressListener.Stub() {
            override fun onFinished(isSuccess: Boolean) {
                listener.onFinished(isSuccess)
            }

            override fun reportProgress(visible: Boolean, statusText: String?) {
                listener.reportProgress(visible, statusText!!)
            }
        })
    }

    override fun setLiveTvView(liveTvView: TvView) {
        this.liveTvView = liveTvView
    }

    override fun timeShiftPause(callback: IAsyncCallback) {
        if (serviceImpl.timeShiftPause()) {
            if (isTimeShiftPausedInternal) {
                liveTvView!!.timeShiftResume()
                isTimeShiftPausedInternal = false
            } else if (startedTimeShift) {
                liveTvView!!.timeShiftPause()
                isTimeShiftPausedInternal = true
            }

            if (timeShiftPositionCallback == null) {
                setTimeShiftPositionCallback()
            }
        }
        callback.onSuccess()
    }

    override fun pauseTimeShift(callback: IAsyncCallback) {

        if (liveTvView == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "live tv view is not initialized")
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

    override fun resumeTimeShift(callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "resumeTimeShift: TIMESHIFT RESUME")
        if (liveTvView == null) {
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
        } else {
            callback.onSuccess()
        }
    }

    override fun timeShiftStop(callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftStop: TIMESHIFT STOP")
        if (liveTvView == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "live tv view is not initialized")
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        isTimeShiftPaused = false
        timeShiftPositionCallback = null
        timeShiftPosition = 0
        timeShiftStartPosition = -1L
        isTimeShiftActive = false
        liveTvView!!.setTimeShiftPositionCallback(null)

        playerInterface.resume()
        callback.onSuccess()
    }

    override fun timeShiftSeekForward(timeMs: Long, callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftSeekForward: TIMESHIFT SEEK FORWARD $timeMs")
        if (liveTvView == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "live tv view is not initialized")
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }

        liveTvView!!.timeShiftSeekTo(timeShiftPosition + timeMs)
        callback.onSuccess()
    }

    override fun timeShiftSeekBackward(timeMs: Long, callback: IAsyncCallback) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "timeShiftSeekBackward: TIMESHIFT SEEK BACKWARDS $timeMs")
        if (liveTvView == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "live tv view is not initialized")
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        liveTvView!!.timeShiftSeekTo(timeShiftPosition - timeMs)
        callback.onSuccess()
    }

    override fun timeShiftSeekTo(timeMs: Long, callback: IAsyncCallback) {
        if (liveTvView == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "live tv view is not initialized")
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        liveTvView!!.timeShiftSeekTo(timeShiftStartPosition + timeMs)
        callback.onSuccess()
    }

    override fun setTimeShiftSpeed(speed: Int, callback: IAsyncCallback) {
        if (liveTvView == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "live tv view is not initialized")
            callback.onFailed(Error("live tv view is not initialized"))
            return
        }
        if (isTimeShiftActive) {
            val playbackParams = PlaybackParams()
            playbackParams.pitch = 1.0f
            playbackParams.speed = speed.toFloat()
            liveTvView!!.timeShiftSetPlaybackParams(playbackParams)
        }
        callback.onSuccess()
    }

    override fun setTimeShiftIndication(show: Boolean) {
        serviceImpl.timeShiftIndication = show
    }

    override fun setTimeShiftPositionCallback() {
        if (liveTvView == null) {
            return
        }
        timeShiftPositionCallback = ReferenceTimeShiftPositionCallback()
        liveTvView!!.setTimeShiftPositionCallback(timeShiftPositionCallback)
    }

    override fun reset(callback: IAsyncCallback) {
        if (liveTvView == null) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "live tv view is not initialized")
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

    override fun stopTimeshift() {
        liveTvView?.sendAppPrivateCommand("STOP_TIMESHIFT_PB", Bundle())
        liveTvView?.sendAppPrivateCommand("SET_SCREEN_MODE", Bundle())
        liveTvView?.sendAppPrivateCommand("SET_FILE_PATH", Bundle())
        startedTimeShift = false
        serviceImpl.stopTimeshift()
    }

    override fun realPause(callback: IAsyncCallback) {
        liveTvView?.timeShiftPause()
        isTimeShiftPaused = true
        startedTimeShift = true
        serviceImpl.realPause(object : IAsyncListener.Stub() {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onFailed(error: String) {
                callback.onFailed(Error(error))
            }
        })
    }
}