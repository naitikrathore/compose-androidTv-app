package com.iwedia.cltv.platform.test.watchlist

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import java.util.concurrent.Semaphore
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class WatchlistInterfaceTest {

    private val TAG: String = "WatchlistInterfaceTest"

    private lateinit var epgInterface: EpgInterface
    private lateinit var watchlistInterface: WatchlistInterface
    private lateinit var context: Context
    private lateinit var tvEventList: ArrayList<TvEvent>
    private lateinit var tvList: ArrayList<TvChannel>
    private lateinit var schedulerReminder_1: ScheduledReminder
    private lateinit var schedulerReminder_2: ScheduledReminder

    @Before
    fun setup() = runTest {
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        context = applicationContext.applicationContext

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
                startTime = System.currentTimeMillis() + 120000,
                endTime = System.currentTimeMillis() + 10000000,
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
                startTime = System.currentTimeMillis() + 90000,
                endTime = System.currentTimeMillis() + 20000000,
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
                startTime = System.currentTimeMillis() + 200000,
                endTime = System.currentTimeMillis() + 30000000,
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
            tvEventList.get(1)!!.id
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

        withContext(Dispatchers.Main.immediate) {
            val factory = ModuleFactory(applicationContext)
            epgInterface = factory.createEpgModule()
            watchlistInterface = factory.createWatchlistModule(epgInterface)

        }
    }

    @Test
    fun test_ScheduleReminder() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var isSuccess = schedulerReminder_2.tvEvent?.let { watchlistInterface.isInWatchlist(it) }
        MatcherAssert.assertThat(isSuccess, CoreMatchers.`is`(true))
      }

    @Test
    fun test_ClearWatchlist() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var watchlistCount = 0
        watchlistInterface.getWatchListCount(object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onFailed");
                semaphore.release()
            }
            override fun onReceive(data: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onReceive");
                watchlistCount = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(watchlistCount, CoreMatchers.`is`(1))

        watchlistInterface.clearWatchList()
        watchlistInterface.getWatchListCount(object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onFailed");
                semaphore.release()
            }
            override fun onReceive(data: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onReceive");
                watchlistCount = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(watchlistCount, CoreMatchers.`is`(0))
    }

    @Test
    fun test_IsInWatchlist() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var isSuccess = schedulerReminder_1.tvEvent?.let { watchlistInterface.isInWatchlist(it) }
        MatcherAssert.assertThat(isSuccess, CoreMatchers.`is`(true))
        isSuccess = schedulerReminder_2.tvEvent?.let { watchlistInterface.isInWatchlist(it) }
        MatcherAssert.assertThat(isSuccess, CoreMatchers.`is`(false))
    }

    @Test
    fun test_GetWatchListCount() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var watchlistCount = 0
        watchlistInterface.getWatchListCount(object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onFailed");
                semaphore.release()
            }
            override fun onReceive(data: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onReceive");
                watchlistCount = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(watchlistCount, CoreMatchers.`is`(1))
    }

    @Test
    fun test_GetWatchlist() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var channelName:String? = "Dummy"
        watchlistInterface.getWatchList(object : IAsyncDataCallback<MutableList<ScheduledReminder>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchList - onFailed");
                semaphore.release()
            }
            override fun onReceive(data: MutableList<ScheduledReminder>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchList - onReceive");
                data.forEach {
                    channelName = it.tvChannel?.name
                }
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(channelName,
            CoreMatchers.`is`("Rai1")
        )
    }

    @Test
    fun test_GetWatchlistItem() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        var channelName:String? = "Dummy"
        watchlistInterface.getWatchlistItem(0, object : IAsyncDataCallback<ScheduledReminder> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchlistItem - onFailed");
                semaphore.release()
            }
            override fun onReceive(data: ScheduledReminder) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchlistItem - onReceive");
                channelName = data.tvChannel?.name
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(channelName,
            CoreMatchers.`is`("AlJazeera")
        )
    }

    @Test
    fun test_HasScheduledReminder() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        result = false
        watchlistInterface.hasScheduledReminder(tvEventList.get(2), object : IAsyncDataCallback<Boolean> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "hasScheduledReminder - onFailed");
                semaphore.release()
            }
            override fun onReceive(data: Boolean) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "hasScheduledReminder - onReceive");
                result = data
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
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        watchlistInterface.removeScheduledReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledReminder - onFailed");
                result = false
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "removeScheduledReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_RemoveWatchlistEventsForDeletedChannels() = runTest {
        val semaphore = Semaphore(0)
        var result = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        watchlistInterface.removeWatchlistEventsForDeletedChannels()
        var watchlistCount = 0
        val countJob = async(Dispatchers.Default){
            delay(3000)
            watchlistInterface.getWatchListCount(object : IAsyncDataCallback<Int> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onFailed");
                    semaphore.release()
                }
                override fun onReceive(data: Int) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getWatchListCount - onReceive");
                    watchlistCount = data
                    semaphore.release()
                }
            })
            semaphore.acquire()
        }
        countJob.await();
        MatcherAssert.assertThat(watchlistCount, CoreMatchers.`is`(0))
    }

    @Test
    fun test_CheckReminderConflict() = runTest {
        val semaphore = Semaphore(0)
        var result: Boolean? = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_2, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        result = schedulerReminder_2.tvEvent?.startTime?.let {
            watchlistInterface.checkReminderConflict(3,
                it
            )
        }
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_LoadScheduledReminders() = runTest {
        val semaphore = Semaphore(0)
        var result: Boolean? = false
        watchlistInterface.clearWatchList()
        watchlistInterface.scheduleReminder(schedulerReminder_1, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onFailed");
                semaphore.release()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "scheduleReminder - onSuccess");
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))

        watchlistInterface.loadScheduledReminders()

        var isSuccess = schedulerReminder_1.tvEvent?.let { watchlistInterface.isInWatchlist(it) }
        MatcherAssert.assertThat(isSuccess, CoreMatchers.`is`(true))
    }
}