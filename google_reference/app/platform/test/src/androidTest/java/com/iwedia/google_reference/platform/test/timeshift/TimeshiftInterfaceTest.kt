package com.iwedia.cltv.platform.test.timeshift

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.test.player.PlayerTestActivity
import com.iwedia.cltv.platform.test.player.waitFor
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class TimeshiftInterfaceTest {

    companion object {
        val channels = arrayListOf<PlayableItem>(
            TvChannel(id = 233, inputId = "com.mediatek.tvinput/.tuner.TunerInputService/HW0", name = "Rai 1", channelId = 233)
        )
    }

    @Rule
    @JvmField
    val mActivityRule = ActivityScenarioRule(PlayerTestActivity::class.java)
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    @Before
    fun setup() {
        mActivityRule.scenario.onActivity { activity ->
            activity.channels = channels
        }
    }

    @After
    fun tearDown() {
        mActivityRule.scenario.close()
    }

    @Test
    fun timeShiftPause_test() = runTest {

        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())

                //Pause timeshift
                val status = async {
                    var status = false
                    it.timeShiftInterface.timeShiftPause(object: IAsyncCallback{
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeShiftPaused = status.await()
                MatcherAssert.assertThat(isTimeShiftPaused, `is`(true))
                delay(3000)


                //Stop timeshift
                val res = async {
                    var status = false
                    it.timeShiftInterface.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }
                
                val isTimeshiftStopped = res.await()
                MatcherAssert.assertThat(isTimeshiftStopped, `is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(11000))
    }

    @Test
    fun resumeTimeShift_test() = runTest {

        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())


                //Pause timeshift
                val status = async {
                    var status = false
                    it.timeShiftInterface.timeShiftPause(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {
                            status = true
                        }
                    })
                    delay(1000)
                    status
                }

                val isTimeShiftPaused = status.await()
                MatcherAssert.assertThat(isTimeShiftPaused, `is`(true))
                delay(3000)


                //resume timeshift
                val value = async {
                    var status = false
                    it.timeShiftInterface.resumeTimeShift(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {
                            status = true
                        }
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftResumed = value.await()
                MatcherAssert.assertThat(isTimeshiftResumed, `is`(true))
                delay(2000)

                //Stop timeshift
                val res = async {
                    var status = false
                    it.timeShiftInterface.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftStopped = res.await()
                MatcherAssert.assertThat(isTimeshiftStopped, `is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(14000))
    }

    @Test
    fun timeShiftStop_test() = runTest {

        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())

                //Pause timeshift
                val status = async {
                    var status = false
                    it.timeShiftInterface.timeShiftPause(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {
                            status = true
                        }
                    })
                    delay(1000)
                    status
                }

                val isTimeShiftPaused = status.await()
                MatcherAssert.assertThat(isTimeShiftPaused, `is`(true))
                delay(3000)

                //Stop timeshift
                val res = async {
                    var status = false
                    it.timeShiftInterface.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftStopped = res.await()
                MatcherAssert.assertThat(isTimeshiftStopped, `is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(11000))
    }

    @Test
    fun timeShiftSeekForward_test() = runTest {

        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())


                //Pause timeshift
                val status = async {
                    var status = false
                    it.timeShiftInterface.timeShiftPause(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {
                            status = true
                        }
                    })
                    delay(1000)
                    status
                }

                val isTimeShiftPaused = status.await()
                MatcherAssert.assertThat(isTimeShiftPaused, `is`(true))
                delay(3000)

                //resume timeshift
                val value = async {
                    var status = false
                    it.timeShiftInterface.resumeTimeShift(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftResumed = value.await()
                MatcherAssert.assertThat(isTimeshiftResumed, `is`(true))
                delay(2000)

                //Timeshift seek forward
                val seek = async {
                    var status = false
                    it.timeShiftInterface.timeShiftSeekForward(2000, object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftseekForward = seek.await()
                MatcherAssert.assertThat(isTimeshiftseekForward, `is`(true))
                delay(2000)

                //Stop timeshift
                val res = async {
                    var status = false
                    it.timeShiftInterface.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftStopped = res.await()
                MatcherAssert.assertThat(isTimeshiftStopped, `is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(17000))
    }

    @Test
    fun timeShiftSeekBackward_test() = runTest {

        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())


                //Pause timeshift
                val status = async {
                    var status = false
                    it.timeShiftInterface.timeShiftPause(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {
                            status = true
                        }
                    })
                    delay(1000)
                    status
                }

                val isTimeShiftPaused = status.await()
                MatcherAssert.assertThat(isTimeShiftPaused, `is`(true))
                delay(3000)

                //resume timeshift
                val value = async {
                    var status = false
                    it.timeShiftInterface.resumeTimeShift(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftResumed = value.await()
                MatcherAssert.assertThat(isTimeshiftResumed, `is`(true))
                delay(2000)

                //Timeshift seek forward
                val seek = async {
                    var status = false
                    it.timeShiftInterface.timeShiftSeekForward(2000, object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftseekForward = seek.await()
                MatcherAssert.assertThat(isTimeshiftseekForward, `is`(true))
                delay(2000)

                //Timeshift seek backward
                val seekBackward = async {
                    var status = false
                    it.timeShiftInterface.timeShiftSeekBackward(2000, object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftseekBackward = seekBackward.await()
                MatcherAssert.assertThat(isTimeshiftseekBackward, `is`(true))
                delay(2000)

                //Stop timeshift
                val res = async {
                    var status = false
                    it.timeShiftInterface.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftStopped = res.await()
                MatcherAssert.assertThat(isTimeshiftStopped, `is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(20000))
    }

    @Test
    fun timeShiftSeekTo_test() = runTest {
        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())


                //Pause timeshift
                val status = async {
                    var status = false
                    it.timeShiftInterface.timeShiftPause(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {
                            status = true
                        }
                    })
                    delay(1000)
                    status
                }

                val isTimeShiftPaused = status.await()
                MatcherAssert.assertThat(isTimeShiftPaused, `is`(true))
                delay(3000)

                //resume timeshift
                val value = async {
                    var status = false
                    it.timeShiftInterface.resumeTimeShift(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftResumed = value.await()
                MatcherAssert.assertThat(isTimeshiftResumed, `is`(true))
                delay(1000)

                //Timeshift seek to position
                val seek = async {
                    var status = false
                    it.timeShiftInterface.timeShiftSeekTo(2000, object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftseekTo = seek.await()
                MatcherAssert.assertThat(isTimeshiftseekTo, `is`(true))
                delay(2000)

                //Stop timeshift
                val res = async {
                    var status = false
                    it.timeShiftInterface.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftStopped = res.await()
                MatcherAssert.assertThat(isTimeshiftStopped, `is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(16000))
    }

    @Test
    fun setTimeShiftSpeed_test() = runTest {

        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())


                //Pause timeshift
                val status = async {
                    var status = false
                    it.timeShiftInterface.timeShiftPause(object: IAsyncCallback{
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeShiftPaused = status.await()
                MatcherAssert.assertThat(isTimeShiftPaused, `is`(true))
                delay(3000)


                //resume timeshift
                val value = async {
                    var status = false
                    it.timeShiftInterface.resumeTimeShift(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftResumed = value.await()
                MatcherAssert.assertThat(isTimeshiftResumed, `is`(true))
                delay(1000)


                //Timeshift seek forward
                val seek = async {
                    var status = false
                    it.timeShiftInterface.timeShiftSeekForward(2000, object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftseekForward = seek.await()
                MatcherAssert.assertThat(isTimeshiftseekForward, `is`(true))
                delay(1000)

                //Timeshift set speed
                val speed = async {
                    var status = false
                    it.timeShiftInterface.setTimeShiftSpeed(2, it.liveTvView, object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() { status = true }
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftSpeedSet = speed.await()
                MatcherAssert.assertThat(isTimeshiftSpeedSet, `is`(true))
                delay(4000)

                //Stop timeshift
                val res = async {
                    var status = false
                    it.timeShiftInterface.timeShiftStop(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftStopped = res.await()
                MatcherAssert.assertThat(isTimeshiftStopped, `is`(true))
            }
        }

        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(20000))
    }

    @Test
    fun reset_test() = runTest {
        mActivityRule.scenario.onActivity {
            it.switchOnChannel(0)
            scope.launch {
                val result = async {
                    delay(5000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                MatcherAssert.assertThat(isPlaybackStarted, notNullValue())

                //reset liveTvView
                val res = async {
                    var status = false
                    it.timeShiftInterface.reset(object : IAsyncCallback {
                        override fun onFailed(error: Error) {}
                        override fun onSuccess() {status = true}
                    })
                    delay(1000)
                    status
                }

                val isTimeshiftViewReset = res.await()
                MatcherAssert.assertThat(isTimeshiftViewReset, `is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(7000))
    }
}