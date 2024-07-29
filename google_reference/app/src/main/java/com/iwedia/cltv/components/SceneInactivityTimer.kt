package com.iwedia.cltv.components

import android.os.CountDownTimer
import com.iwedia.cltv.platform.model.information_bus.events.Events
import utils.information_bus.Event
import utils.information_bus.InformationBus

object SceneInactivityTimer {

    var timer: CountDownTimer? = null

    fun startTimer(duration : Long) {
        stopTimer()

        timer = object :
            CountDownTimer(
                duration,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                timer!!.cancel()
                timer = null
                InformationBus.submitEvent(
                    Event(Events.INACTIVITY_TIMER_END)
                )
            }
        }

        timer!!.start()
    }

    fun stopTimer() {
        if(timer != null){
            timer!!.cancel()
            timer = null
        }
    }
}