package com.iwedia.cltv.platform.test.scheduler

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.SchedulerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.concurrent.Semaphore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class SchedulerInterfaceTest {

    private val TAG: String = "SchedulerInterfaceTest"

    private lateinit var epgInterface: EpgInterface
    private lateinit var schedulerInterface: SchedulerInterface
    private lateinit var utilsInterface: UtilsInterface
    private lateinit var watchlistInterface: WatchlistInterface
    private lateinit var context: Context
    private lateinit var tvEventList: ArrayList<TvEvent>
    private lateinit var tvList: ArrayList<TvChannel>
    private lateinit var schedulerReminder_1: ScheduledReminder
    private lateinit var schedulerReminder_2: ScheduledReminder
    private lateinit var scheduledRecording: ScheduledRecording

    @Before
    fun setup() = runTest {
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        context = applicationContext.applicationContext
        val time = System.currentTimeMillis()

        tvList = arrayListOf(
            TvChannel(id = 1, name = "Rai1", lcn = 41, tunerType = TunerType.TERRESTRIAL_TUNER_TYPE),
            TvChannel(id = 2, name = "BBC", lcn = 4, tunerType = TunerType.TERRESTRIAL_TUNER_TYPE),
            TvChannel(id = 3, name = "AlJazeera", displayNumber = "5", tunerType = TunerType.TERRESTRIAL_TUNER_TYPE),
            TvChannel(id = 4, name = "CNN", displayNumber = "4", tunerType = TunerType.TERRESTRIAL_TUNER_TYPE)
        )

        tvEventList = arrayListOf(
            TvEvent(id = 1,
                tvChannel = tvList.get(0),
                name = "Event1",
                shortDescription = "Event1 Short Des",
                longDescription = "Event1 short des",
                imagePath = null,
                startTime = time + 350000,
                endTime = time + 10000000,
                categories = null,
                parentalRate = 18,
                rating = 1,
                tag = null,
                parentalRating = null,
                isProgramSame = false,
                isInitialChannel = true,
                providerFlag = null
            ),
            TvEvent(id = 2,
                tvChannel = tvList.get(1),
                name = "Event2",
                shortDescription = "Event2 Short Des",
                longDescription = "Event2 short des",
                imagePath = null,
                startTime = time + 600000,
                endTime = time + 20000000,
                categories = null,
                parentalRate = 18,
                rating = 1,
                tag = null,
                parentalRating = null,
                isProgramSame = false,
                isInitialChannel = true,
                providerFlag = null
            ),
            TvEvent(id = 3,
                tvChannel = tvList.get(2),
                name = "Event3",
                shortDescription = "Event3 Short Des",
                longDescription = "Event3 short des",
                imagePath = null,
                startTime = time + 25000000,
                endTime = time + 40000000,
                categories = null,
                parentalRate = 18,
                rating = 1,
                tag = null,
                parentalRating = null,
                isProgramSame = false,
                isInitialChannel = true,
                providerFlag = null
            )
        )

        schedulerReminder_1 = ScheduledReminder(
            333,
            "Cricket Live",
            tvList.get(0),
            tvEventList.get(0),
            tvEventList.get(0)!!.startTime,
            tvList.get(0)!!.id,
            tvEventList.get(0)!!.id
        )

        schedulerReminder_2 = ScheduledReminder(
            444,
            "Breaking News",
            tvList.get(2),
            tvEventList.get(2),
            tvEventList.get(2)!!.startTime,
            tvList.get(2)!!.id,
            tvEventList.get(2)!!.id
        )

        scheduledRecording = ScheduledRecording(
            666,
            tvList.get(0).name,
            tvEventList.get(0)!!.startTime,
            tvEventList.get(0)!!.endTime,
            tvList.get(0).id,
            tvEventList.get(0).id,
            RepeatFlag.NONE,
            tvList.get(0),
            tvEventList.get(0)
        )

        val factory = ModuleFactory(applicationContext)
        utilsInterface = factory.createUtilsModule()
        epgInterface = factory.createEpgModule()
        watchlistInterface = factory.createWatchlistModule(epgInterface)
        schedulerInterface = factory.createSchedulerModule(utilsInterface,epgInterface,watchlistInterface)
    }

    @Test
    fun test_StoreScheduledReminder() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearReminderList()
        schedulerInterface.storeScheduledReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
      }

    @Test
    fun test_RemoveScheduledReminder() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearReminderList()
        schedulerInterface.storeScheduledReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.removeScheduledReminder(schedulerReminder_2,  object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledReminder - onFailed")
                result = false
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_GetScheduledRemindersData() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearReminderList()
        schedulerInterface.storeScheduledReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var scheduledReminderData : ArrayList<ScheduledReminder>? = null
        schedulerInterface.getScheduledRemindersData(object : IAsyncDataCallback<ArrayList<ScheduledReminder>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<ScheduledReminder>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData - onReceive")
                scheduledReminderData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledReminderData?.get(0)?.name, CoreMatchers.`is`("Breaking News"))
    }

    @Test
    fun test_ClearReminderList() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.storeScheduledReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var scheduledReminderData : ArrayList<ScheduledReminder>? = null
        schedulerInterface.clearReminderList()
        schedulerInterface.getScheduledRemindersData(object : IAsyncDataCallback<ArrayList<ScheduledReminder>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<ScheduledReminder>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData - onReceive")
                scheduledReminderData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledReminderData?.size, CoreMatchers.`is`(0))
    }

    @Test
    fun test_StoreScheduledRecording() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_RemoveScheduledRecording() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.removeScheduledRecording(scheduledRecording,  object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledRecording - onFailed")
                result = false
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_GetScheduledRecordingData() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
        schedulerInterface.getScheduledRecordingData(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<ScheduledRecording>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                scheduledRecordingData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledRecordingData?.get(0)?.name, CoreMatchers.`is`("Rai1"))
    }

    @Test
    fun test_ClearRecordingList() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
        schedulerInterface.clearRecordingList()
        schedulerInterface.getScheduledRecordingData(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<ScheduledRecording>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                scheduledRecordingData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(0))
    }

    @Test
    fun test_ClearRecordingListPvr() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.getScheduledRecordingData(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<ScheduledRecording>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                scheduledRecordingData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(0))
    }

    @Test
    fun test_GetRecodingId() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.getRecodingId(scheduledRecording,object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getRecodingId - onFailed")
                result = false
                semaphore.release()
            }
            override fun onReceive(data: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getRecodingId - onReceive")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_RemoveDeletedChannelsFromScheduledTables() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearReminderList()
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                result = false
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        val deletedChannels = arrayListOf<Int>(1,3)
        schedulerInterface.removeDeletedChannelsFromScheduledTables(deletedChannels)

        var scheduledReminderData : ArrayList<ScheduledReminder>? = null
        schedulerInterface.getScheduledRemindersData(object : IAsyncDataCallback<ArrayList<ScheduledReminder>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<ScheduledReminder>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRemindersData - onReceive")
                scheduledReminderData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledReminderData?.size, CoreMatchers.`is`(0))

        var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
        schedulerInterface.getScheduledRecordingData(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<ScheduledRecording>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                scheduledRecordingData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(0))
    }

    @Test
    fun test_RemoveScheduledRecordingForDeletedChannels() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.removeScheduledRecordingForDeletedChannels()
        val checkingJob = async(Dispatchers.Default) {
            delay(3000)

            var isInReclist = schedulerInterface.isInReclist(tvEventList.get(0))
            MatcherAssert.assertThat(isInReclist, CoreMatchers.`is`(false))
        }
        checkingJob.await()
    }

    @Test
    fun test_Schedule() = runTest {
        val semaphore = Semaphore(0)
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.schedule(350000,scheduledRecording)

        val scheduleJob = async(Dispatchers.Default) {
            delay(3000)
            var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
            schedulerInterface.getScheduledRecordingData(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                    semaphore.release()
                }
                override fun onReceive(data: ArrayList<ScheduledRecording>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                    scheduledRecordingData = data
                    semaphore.release()
                }
            })
            semaphore.acquire()
            MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(1))
        }
        scheduleJob.await()
    }

    @Test
    fun test_FindConflictedRecordings() = runTest {
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.schedule(350000,scheduledRecording)

        val userScheduledRecording = ScheduledRecording(
            666,
            tvList.get(2).name,
            tvEventList.get(2)!!.startTime,
            tvEventList.get(2)!!.endTime,
            tvList.get(2).id,
            tvEventList.get(2).id,
            RepeatFlag.NONE,
            tvList.get(2),
            tvEventList.get(2)
        )
        val checkConflictJob = async(Dispatchers.Default) {
            delay(3000)
            var conflictRecording = schedulerInterface.findConflictedRecordings(userScheduledRecording)
            MatcherAssert.assertThat(conflictRecording?.size, CoreMatchers.`is`(0))
        }
        checkConflictJob.await()
    }

    @Test
    fun test_FindConflictedRecordings_startTime_endTime() = runTest {
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.schedule(350000,scheduledRecording)

        val checkConflictJob = async(Dispatchers.Default) {
            delay(3000)
            var conflictRecording = schedulerInterface.findConflictedRecordings(
                System.currentTimeMillis() + 750000,
                System.currentTimeMillis() + 2000000
            )
            MatcherAssert.assertThat(conflictRecording?.size, CoreMatchers.`is`(1))
        }
        checkConflictJob.await()
    }

    @Test
    fun test_ScheduleWithDailyRepeat() = runTest {
        val semaphore = Semaphore(0)
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.scheduleWithDailyRepeat(350000,scheduledRecording)

        val scheduleJob = async(Dispatchers.Default) {
            delay(3000)
            var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
            schedulerInterface.getScheduledRecordingData(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                    semaphore.release()
                }
                override fun onReceive(data: ArrayList<ScheduledRecording>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                    scheduledRecordingData = data
                    semaphore.release()
                }
            })
            semaphore.acquire()
            MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(1))
        }
        scheduleJob.await()
    }

    @Test
    fun test_ScheduleWithWeeklyRepeat() = runTest {
        val semaphore = Semaphore(0)
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.scheduleWithWeeklyRepeat(350000,scheduledRecording)

        val scheduleJob = async(Dispatchers.Default) {
            delay(3000)
            var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
            schedulerInterface.getScheduledRecordingData(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                    semaphore.release()
                }
                override fun onReceive(data: ArrayList<ScheduledRecording>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                    scheduledRecordingData = data
                    semaphore.release()
                }
            })
            semaphore.acquire()
            MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(1))
        }
        scheduleJob.await()
    }

    @Test
    fun test_CheckRecordingConflict() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.reload()
        val checkConflictJob = async(Dispatchers.Default) {
            delay(3000)

            Log.d(Constants.LogTag.CLTV_TAG + TAG, "checkRecordingConflict - time: ${scheduledRecording.scheduledDateStart}")
            var isConflict = schedulerInterface.checkRecordingConflict(scheduledRecording.scheduledDateStart)
            MatcherAssert.assertThat(isConflict, CoreMatchers.`is`(true))
        }
        checkConflictJob.await()
    }

    @Test
    fun test_GetId() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.reload()
        val getIdJob = async(Dispatchers.Default) {
            delay(3000)

            var id = schedulerInterface.getId(scheduledRecording)
            MatcherAssert.assertThat(id, CoreMatchers.not(-1))
        }
        getIdJob.await()
    }

    @Test
    fun test_IsInConflictedList() = runTest {
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.updateConflictRecordings(scheduledRecording, true)
        result = schedulerInterface.isInConflictedList(scheduledRecording)
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_UpdateConflictRecordings() = runTest {
        var result = false
        schedulerInterface.clearRecordingListPvr()
        result = schedulerInterface.isInConflictedList(scheduledRecording)
        MatcherAssert.assertThat(result, CoreMatchers.`is`(false))
        schedulerInterface.updateConflictRecordings(scheduledRecording, true)
        result = schedulerInterface.isInConflictedList(scheduledRecording)
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_IsInReclist() = runTest {
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.schedule(250000,scheduledRecording)

        val checkJob = async(Dispatchers.Default) {
            delay(3000)
            var isInReclist = schedulerInterface.isInReclist(tvEventList.get(0))
            MatcherAssert.assertThat(isInReclist, CoreMatchers.`is`(true))
        }
        checkJob.await()
    }

    @Test
    fun test_GetRecList() = runTest {
        val semaphore = Semaphore(0)
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.schedule(350000,scheduledRecording)

        val checkJob = async(Dispatchers.Default) {
            delay(3000)
            var scheduledRecordingData : MutableList<ScheduledRecording>? = null
            schedulerInterface.getRecList(object : IAsyncDataCallback<MutableList<ScheduledRecording>> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getRecList - onFailed")
                    semaphore.release()
                }
                override fun onReceive(data: MutableList<ScheduledRecording>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getRecList - onReceive")
                    scheduledRecordingData = data
                    semaphore.release()
                }
            })
            semaphore.acquire()
            MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(1))
        }
        checkJob.await()
    }

    @Test
    fun test_Reload() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
        var id = schedulerInterface.getId(scheduledRecording)
        MatcherAssert.assertThat(id, CoreMatchers.equalTo(-1))

        schedulerInterface.reload()
        val getIdJob = async(Dispatchers.Default) {
            delay(3000)

            id = schedulerInterface.getId(scheduledRecording)
            MatcherAssert.assertThat(id, CoreMatchers.not(-1))
        }
        getIdJob.await()
    }

    @Test
    fun test_GetScheduledRecordingsList() = runTest {
        val semaphore = Semaphore(0)
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.schedule(350000,scheduledRecording)

        val scheduleJob = async(Dispatchers.Default) {
            delay(3000)
            var scheduledRecordingData : ArrayList<ScheduledRecording>? = null
            var isInReclist = schedulerInterface.getScheduledRecordingsList(object : IAsyncDataCallback<ArrayList<ScheduledRecording>> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingsList - onFailed")
                    semaphore.release()
                }
                override fun onReceive(data: ArrayList<ScheduledRecording>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingsList - onReceive")
                    scheduledRecordingData = data
                    semaphore.release()
                }
            })
            semaphore.acquire()
            MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(1))
        }
        scheduleJob.await()
    }

    @Test
    fun test_GetEventId() = runTest {
        val semaphore = Semaphore(0)
        var event: TvEvent? = null
        schedulerInterface.getEventId(tvList.get(2),-1,object : IAsyncDataCallback<TvEvent> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventId - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: TvEvent) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getEventId - onReceive")
                event = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(event, CoreMatchers.notNullValue())
    }

    @Test
    fun test_HasScheduledRec() = runTest {
        val semaphore = Semaphore(0)
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.schedule(350000,scheduledRecording)

        val checkRecjob = async(Dispatchers.Default) {
            delay(3000)
            var result = false
            var isInReclist = schedulerInterface.hasScheduledRec(tvEventList.get(0),object : IAsyncDataCallback<Boolean> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "hasScheduledRec - onFailed")
                    semaphore.release()
                }
                override fun onReceive(data: Boolean) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "hasScheduledRec - onReceive")
                    result = data
                    semaphore.release()
                }
            })
            semaphore.acquire()
            MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
        }
        checkRecjob.await()
    }

    @Test
    fun test_ScheduleRecording() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()

        var scheduledRecordingData : SchedulerInterface.ScheduleRecordingResult? = null
        schedulerInterface.scheduleRecording(scheduledRecording, object : IAsyncDataCallback<SchedulerInterface.ScheduleRecordingResult> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleRecording - onFailed - ${error.toString()}")
                semaphore.release()
            }
            override fun onReceive(data: SchedulerInterface.ScheduleRecordingResult) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleRecording - onReceive")
                scheduledRecordingData = data
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
    }

    @Test
    fun test_RemoveAllScheduledRecording() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        schedulerInterface.clearRecordingListPvr()
        schedulerInterface.storeScheduledRecording(scheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onFailed")
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledReminder - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        val userScheduledRecording = ScheduledRecording(
            666,
            tvList.get(2).name,
            tvEventList.get(2)!!.startTime,
            tvEventList.get(2)!!.endTime,
            tvList.get(2).id,
            tvEventList.get(2).id,
            RepeatFlag.NONE,
            tvList.get(2),
            tvEventList.get(2)
        )
        schedulerInterface.storeScheduledRecording(userScheduledRecording, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onFailed")
                result = false
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeScheduledRecording - onSuccess")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        schedulerInterface.reload()

        val removeJob = async(Dispatchers.Default) {
            delay(3000)
            schedulerInterface.removeAllScheduledRecording(
                scheduledRecording,
                object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeAllScheduledRecording - onFailed")
                        semaphore.release()
                    }

                    override fun onSuccess() {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeAllScheduledRecording - onSuccess")
                        semaphore.release()
                    }
                })
            semaphore.acquire()
        }
        removeJob.await()

        var scheduledRecordingData: ArrayList<ScheduledRecording>? = null
        schedulerInterface.getScheduledRecordingData(object :
            IAsyncDataCallback<ArrayList<ScheduledRecording>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onFailed")
                semaphore.release()
            }

            override fun onReceive(data: ArrayList<ScheduledRecording>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getScheduledRecordingData - onReceive")
                scheduledRecordingData = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(scheduledRecordingData?.size, CoreMatchers.`is`(1))
    }

}