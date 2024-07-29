package com.iwedia.cltv.platform.`interface`

import android.media.tv.TvView
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.model.IAsyncCallback

interface TimeshiftInterface {
    var isTimeShiftPaused: Boolean
    var isTimeShiftActive: Boolean
    var showTimeShiftIndication: MutableLiveData<Boolean>
    var selection: Int?
    interface FormattingProgressListener {
        fun reportProgress(visible: Boolean, statusText : String)

        fun onFinished(isSuccess: Boolean)
    }
    fun registerFormatProgressListener(listener: UtilsInterface.FormattingProgressListener)
    fun setLiveTvView(liveTvView: TvView)
    fun timeShiftPause(callback: IAsyncCallback)
    fun pauseTimeShift(callback: IAsyncCallback)
    fun resumeTimeShift(callback: IAsyncCallback)
    fun timeShiftStop(callback: IAsyncCallback)
    fun timeShiftSeekForward(timeMs: Long, callback: IAsyncCallback)
    fun timeShiftSeekBackward(timeMs: Long, callback: IAsyncCallback)
    fun timeShiftSeekTo(timeMs: Long, callback: IAsyncCallback)
    fun setTimeShiftSpeed(speed: Int, callback: IAsyncCallback)
    fun setTimeShiftIndication(show: Boolean)

    fun setTimeShiftPositionCallback()
    fun reset(callback: IAsyncCallback)
    fun stopTimeshift()
    fun realPause(callback: IAsyncCallback)
}