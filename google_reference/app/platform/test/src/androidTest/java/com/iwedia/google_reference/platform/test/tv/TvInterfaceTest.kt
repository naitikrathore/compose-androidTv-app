package com.iwedia.cltv.platform.test.tv

import android.media.tv.TvInputInfo
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.test.player.PlayerTestActivity
import com.iwedia.cltv.platform.test.player.waitFor
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore

@RunWith(AndroidJUnit4::class)
class TvInterfaceTest {
    @Rule
    @JvmField
    val mActivityRule = ActivityScenarioRule(PlayerTestActivity::class.java)

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val TAG: String = "TvInterfaceTest"
    private lateinit var tvInterface: TvInterface

    @Before
    fun setup() = runTest() {
        mActivityRule.scenario.onActivity { activity ->
            val moduleFactory = ModuleFactory(activity.application)
            tvInterface = moduleFactory.createTvModule(activity.player, moduleFactory.createNetworkModule(), moduleFactory.createTvInputModule() )
        }
        //waiting for proper initialization
        val initJob = async(Dispatchers.Default) {
            delay(3000)
        }
        initJob.await()
    }

    @After
    fun tearDown() {
        mActivityRule.scenario.close()
    }

    @Test
    fun test_ChangeChannel() = runTest {
        var isPlaybackStarted: PlayerTestActivity.EventLog? = null
        var channelList = tvInterface.getChannelList()
        val semaphore = Semaphore(0)
        mActivityRule.scenario.onActivity {
            scope.launch {
                it.switchOnChannel(0)
                Log.i(TAG,"Number of found channels " + channelList.size)
                if(channelList.size > 1) {
                    val result = async {
                        tvInterface.changeChannel(channelList.get(1),object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                assert(false)
                                semaphore.release()
                            }
                            override fun onSuccess() {
                                Log.i(TAG,"Changed channel on " + channelList.get(1).name)
                                semaphore.release()
                            }
                        })
                        semaphore.acquire()

                        delay(5000)
                        it.events
                    }
                    val events = result.await()
                    isPlaybackStarted = events.find { event ->
                        event.name == "onPlaybackStarted"
                    }
                    MatcherAssert.assertThat(isPlaybackStarted, CoreMatchers.notNullValue())
                    delay(500)
                    it.events.clear()
                }
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(5000))
    }

    @Test
    fun test_ChangeChannel_Index() = runTest {
        var isPlaybackStarted: PlayerTestActivity.EventLog? = null
        var channelList = tvInterface.getChannelList()
        mActivityRule.scenario.onActivity {
            scope.launch {
                Log.i(TAG, "Number of found channels " + channelList.size)
                if(channelList.size > 1) {
                    it.switchOnChannel(0)

                    val result = async {
                        val index = 1
                        tvInterface.changeChannel(index, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "changeChannel using Index - onFailed")
                                assert(false)
                            }

                            override fun onSuccess() {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "changeChannel using Index - onSuccess")
                                assert(true)
                            }
                        })

                        delay(5000)
                        it.events
                    }
                    val events = result.await()
                    isPlaybackStarted = events.find { event ->
                        event.name == "onPlaybackStarted"
                    }
                    MatcherAssert.assertThat(isPlaybackStarted, CoreMatchers.notNullValue())
                    delay(500)
                    it.events.clear()
                }
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(5000))
    }

    @Test
    fun test_DeleteChannel() = runTest {
        var channelList = tvInterface.getChannelList()
        var numberOfChannelBeforeDelete = channelList.size
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Number of channels are $numberOfChannelBeforeDelete")
        if(numberOfChannelBeforeDelete > 0) {
            var res = tvInterface.deleteChannel(channelList.get(0))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "deleteChannel result is $res")
            MatcherAssert.assertThat(res, CoreMatchers.`is`(true))
        }
    }

    @Test
    fun test_NextChannel() = runTest {
        mActivityRule.scenario.onActivity {
            scope.launch {
                val result = async {
                    tvInterface.nextChannel(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "nextChannel - onFailed")
                            assert(false)
                        }
                        override fun onSuccess() {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "nextChannel - onSuccess")
                            assert(true)
                        }
                    })
                    delay(5000)
                }
            }
        }
        val delay: Long = 5000.toLong()
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(delay))
    }

    @Test
    fun test_PreviousChannel() = runTest {
        mActivityRule.scenario.onActivity {
            scope.launch {
                val result = async {
                    tvInterface.previousChannel(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "previousChannel - onFailed")
                            assert(false)
                        }
                        override fun onSuccess() {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "previousChannel - onSuccess")
                            assert(true)
                        }
                    })
                    delay(5000)
                }
            }
        }
        val delay: Long = 5000.toLong()
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(delay))
    }

    @Test
    fun test_GetChannelById() = runTest {
        var channelList = tvInterface.getChannelList()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "GetChannelById: Number of channels are ${channelList.size}")
        if(channelList.size > 0) {
            var tvChannel = tvInterface.getChannelById(channelList.get(0).id)
            MatcherAssert.assertThat(tvChannel, CoreMatchers.notNullValue())
        }
    }

    @Test
    fun test_FindChannelPosition() = runTest {
        var channelList = tvInterface.getChannelList()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "findChannelPosition: Number of channels are ${channelList.size}")
        var position = -1
        if(channelList.size > 0) {
            position = tvInterface.findChannelPosition(channelList.get(0))
            MatcherAssert.assertThat(position, CoreMatchers.not(-1))
        }
    }

    @Test
    fun test_PlayNextIndex() = runTest {
        var isPlaybackStarted: PlayerTestActivity.EventLog? = null
        var channelList = tvInterface.getChannelList()
        mActivityRule.scenario.onActivity {
            scope.launch {
                val result = async {
                    tvInterface.playNextIndex(channelList, object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "playNextIndex - onFailed")
                            assert(false)
                        }
                        override fun onSuccess() {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "playNextIndex - onSuccess")
                            assert(true)
                        }
                    })
                    delay(5000)
                }
            }
        }
        val delay: Long = 5000.toLong()
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(delay))
    }

    @Test
    fun test_PlayPrevIndex() = runTest {
        var channelList = tvInterface.getChannelList()
        mActivityRule.scenario.onActivity {
            scope.launch {
                val result = async {
                    tvInterface.playPrevIndex(channelList, object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "playPrevIndex - onFailed")
                            assert(false)
                        }
                        override fun onSuccess() {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "playPrevIndex - onSuccess")
                            assert(true)
                        }
                    })
                    delay(5000)
                }
            }
        }
        val delay: Long = 5000.toLong()
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(delay))
    }

    @Test
    fun test_GetChannelByDisplayNumber() = runTest {
        var channelList = tvInterface.getChannelList()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelByDisplayNumber: Number of channels are ${channelList.size}")
        if(channelList.size > 0) {
            var tvChannel = tvInterface.getChannelByDisplayNumber(channelList.get(0).displayNumber)
            MatcherAssert.assertThat(tvChannel, CoreMatchers.notNullValue())
        }
    }

    @Test
    fun test_GetActiveChannel() = runTest {
        val semaphore = Semaphore(0)
        var channel: TvChannel? = null
        tvInterface.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getActiveChannel - onFailed")
                semaphore.release()
            }

            override fun onReceive(data: TvChannel) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getActiveChannel - onReceive")
                channel = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(channel, CoreMatchers.notNullValue())
    }

    @Test
    fun test_GetChannelByIndex() = runTest {
        var channelList = tvInterface.getChannelList()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelByIndex: Number of channels are ${channelList.size}")
        if(channelList.size > 0) {
            var tvChannel = tvInterface.getChannelByIndex(0)
            MatcherAssert.assertThat(tvChannel, CoreMatchers.notNullValue())
        }
    }

    @Test
    fun test_GetChannelList() = runTest {
        var channelList = tvInterface.getChannelList()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelList: Number of channels are ${channelList.size}")
        MatcherAssert.assertThat(channelList.size, CoreMatchers.not(0))
    }

    @Test
    fun test_GetChannelListAsync() = runTest {
        val semaphore = Semaphore(0)
        var channelList: ArrayList<TvChannel>? = null
        tvInterface.getChannelListAsync(object : IAsyncDataCallback<ArrayList<TvChannel>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelListAsync - onFailed")
                semaphore.release()
            }

            override fun onReceive(data: ArrayList<TvChannel>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelListAsync - onReceive")
                channelList = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelListAsync: Number of channels are ${channelList?.size}")
        MatcherAssert.assertThat(channelList, CoreMatchers.notNullValue())
    }

    @Test
    fun test_StartInitialPlayback() = runTest {
        val semaphore = Semaphore(0)
        mActivityRule.scenario.onActivity {
            scope.launch {
                it.switchOnChannel(0)
                tvInterface.startInitialPlayback(object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        Log.i(TAG, "startInitialPlayback - onFailed")
                        assert(false)
                        semaphore.release()
                    }

                    override fun onSuccess() {
                        Log.i(TAG, "startInitialPlayback - onSuccess")
                        assert(true)
                        semaphore.release()
                    }
                })
            }
        }
        semaphore.acquire()
    }

    @Test
    fun test_AddRecentlyWatched_GetRecentlyWatched() = runTest {
        val semaphore = Semaphore(0)
        var channelList: ArrayList<TvChannel>? = null
        tvInterface.getChannelListAsync(object : IAsyncDataCallback<ArrayList<TvChannel>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelListAsync - onFailed")
                semaphore.release()
            }

            override fun onReceive(data: ArrayList<TvChannel>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelListAsync - onReceive")
                channelList = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        var listSize = channelList?.size
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "listSize is $listSize")
        if (listSize != null) {
            if(listSize > 0) {
                var playbleItem:PlayableItem? = channelList?.get(listSize - 1)
                tvInterface.addRecentlyWatched(playbleItem!!)

                val asyncJob = async(Dispatchers.Default) {
                    delay(5000)
                }
                asyncJob.await()

                var recentList = tvInterface.getRecentlyWatched()
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "recent list size is ${recentList?.size}")
                var channelId = channelList?.get(listSize - 1)?.channelId
                var result = false
                recentList?.forEach { playableItem ->
                    if(playableItem is TvChannel) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "channelId is $channelId and recent list channelId is ${(playbleItem as TvChannel).channelId}")
                        if((playbleItem as TvChannel).channelId == channelId)
                            result = true
                    }
                }
                MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
            }
        }
    }

    @Test
    fun test_SkipUnskipChannel() = runTest {
        var channelList = tvInterface.getChannelList()
        var result = false
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "skipUnskipChannel: Number of channels are ${channelList.size}")
        if(channelList.size > 0) {
            result = tvInterface.skipUnskipChannel(channelList.get(0), true)
            MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
        }
    }

    @Test
    fun test_GetTvInputList() = runTest {
        val semaphore = Semaphore(0)
        var inputList: MutableList<TvInputInfo>? = null
        tvInterface.getTvInputList(object : IAsyncDataCallback<MutableList<TvInputInfo>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputList - onFailed")
                semaphore.release()
            }

            override fun onReceive(data: MutableList<TvInputInfo>) {
                inputList = data
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputList - onReceive. Size is: ${inputList?.size} ")
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(inputList, CoreMatchers.notNullValue())
    }

    @Test
    fun test_NextChannelByCategory() = runTest {
        var channelList = tvInterface.getChannelList()
        val semaphore = Semaphore(0)
        mActivityRule.scenario.onActivity {
            scope.launch {
                it.switchOnChannel(0)
                Log.i(TAG,"Number of found channels " + channelList.size)
                if(channelList.size > 1) {
                    val result = async {
                        tvInterface.nextChannelByCategory(0, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "nextChannelByCategory - onFailed")
                                assert(false)
                                semaphore.release()
                            }
                            override fun onSuccess() {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "nextChannelByCategory - onSuccess")
                                assert(true)
                                semaphore.release()
                            }
                        })
                    }
                    result.await()
                }
            }
        }
        semaphore.acquire()
    }

    @Test
    fun test_PreviousChannelByCategory() = runTest {
        var channelList = tvInterface.getChannelList()
        val semaphore = Semaphore(0)
        mActivityRule.scenario.onActivity {
            scope.launch {
                it.switchOnChannel(0)
                Log.i(TAG,"Number of found channels " + channelList.size)
                if(channelList.size > 1) {
                    val result = async {
                        tvInterface.previousChannelByCategory(0, object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "previousChannelByCategory - onFailed")
                                assert(false)
                                semaphore.release()
                            }
                            override fun onSuccess() {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "previousChannelByCategory - onSuccess")
                                assert(true)
                                semaphore.release()
                            }
                        })
                    }
                    result.await()
                }
            }
        }
        semaphore.acquire()
    }

    @Test
    fun test_GetChannelListByCategories() = runTest {
        val semaphore = Semaphore(0)
        var channelList: ArrayList<TvChannel>? = null
        tvInterface.getChannelListByCategories(object : IAsyncDataCallback<ArrayList<TvChannel>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelListByCategories - onFailed")
                semaphore.release()
            }

            override fun onReceive(data: ArrayList<TvChannel>) {
                channelList = data
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelListByCategories - onReceive. Size is: ${channelList?.size} ")
                semaphore.release()
            }
        }, FilterItemType.ALL_ID)
        semaphore.acquire()
        MatcherAssert.assertThat(channelList?.size, CoreMatchers.not(0))
    }

}
