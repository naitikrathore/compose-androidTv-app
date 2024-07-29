package com.iwedia.cltv.scene.live_scene

import android.content.Context
import android.os.CountDownTimer
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.Events
import utils.information_bus.Event
import utils.information_bus.InformationBus
import java.lang.reflect.InvocationTargetException

object InactivityTimer {

    var millisecondsInMin: Long = 60000
    var timer: CountDownTimer? = null
    const val TAG = "InactivityTimer"

    // preference value
    const val DURATION_5_MIN = "duration_5_minutes"
    const val DURATION_10_MIN = "duration_10_minutes"
    const val DURATION_15_MIN = "duration_15_minutes"
    const val DURATION_30_MIN = "duration_30_minutes"
    const val DURATION_60_MIN = "duration_60_minutes"

    fun startTimer(preferredDuration: Any) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "startTimer")
        var time: Long = 0

        when(preferredDuration) {
            DURATION_5_MIN -> {
                time = 5 * millisecondsInMin
            }
            DURATION_10_MIN -> {
                time = 10 * millisecondsInMin
            }
            DURATION_15_MIN -> {
                time = 15 * millisecondsInMin
            }
            DURATION_30_MIN -> {
                time = 30 * millisecondsInMin
            }
            DURATION_60_MIN -> {
                time = 60 * millisecondsInMin
            }
        }

        stopTimer()

        timer = object :
            CountDownTimer(
                time,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTick")
                if(millisUntilFinished in 60000L .. 61000L) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTick show message")
                    //show power off message when there is one minute left
                    InformationBus.submitEvent(
                        Event(Events.NO_SIGNAL_POWER_OFF_TIMER_END)
                    )
                }
            }

            override fun onFinish() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFinish")
                timer!!.cancel()
                timer = null

                if(ReferenceApplication.isInForeground || ReferenceApplication.isOkClickedOnSetUp){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFinish isInForeground")
                    goToSleep()
                }
            }
        }

        timer!!.start()
    }

    fun stopTimer(){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopTimer")
        if(timer != null){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "stopTimer timer is not null")
            timer!!.cancel()
            timer = null
        }
    }

    fun goToSleep(){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "goToSleep")
        val pManager = ReferenceApplication.applicationContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "goToSleep try")
            pManager.javaClass.getMethod(
                "goToSleep",
                *arrayOf<Class<*>?>(Long::class.javaPrimitiveType)
            ).invoke(pManager, SystemClock.uptimeMillis())
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }
    }
}