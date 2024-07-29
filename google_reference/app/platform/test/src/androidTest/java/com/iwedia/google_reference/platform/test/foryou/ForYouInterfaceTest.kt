package com.iwedia.cltv.platform.test.foryou

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.foryou.ForYouItem
import kotlin.collections.ArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore

@RunWith(AndroidJUnit4::class)
class ForYouInterfaceTest {

    private val TAG: String = "ForYouInterfaceTest"
    private lateinit var forYouInterface: ForYouInterface

    @Before
    fun setup() = runTest {
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        val factory = ModuleFactory(applicationContext)
        withContext(Dispatchers.Main.immediate) {
            var epgInterface = factory.createEpgModule()
            var watchlistInterface = factory.createWatchlistModule(epgInterface)
            var utilsInterface = factory.createUtilsModule()
            var playerInterface = factory.createPlayerModule(utilsInterface, epgInterface)
            var tvInterface = factory.createTvModule(playerInterface, factory.createNetworkModule(), factory.createTvInputModule())
            var pvrInterface = factory.createPvrModule(epgInterface,playerInterface,tvInterface,utilsInterface)
            forYouInterface = factory.createForYouModule(epgInterface,watchlistInterface,pvrInterface)
        }
    }

    @Test
    fun test_GetAvailableRailSize() = runTest {
        forYouInterface.updateRailData()
        val updateJob = async(Dispatchers.Default) {
            delay(5000)
            var railSize = forYouInterface.getAvailableRailSize()
            MatcherAssert.assertThat(railSize, CoreMatchers.not(0))
        }
        updateJob.await()
    }

    @Test
    fun test_UpdateRailData() = runTest {
        var railSize = forYouInterface.getAvailableRailSize()
        MatcherAssert.assertThat(railSize, CoreMatchers.`is`(0))
        forYouInterface.updateRailData()
        val updateJob = async(Dispatchers.Default) {
            delay(5000)
            railSize = forYouInterface.getAvailableRailSize()
            MatcherAssert.assertThat(railSize, CoreMatchers.not(0))
        }
        updateJob.await()
    }

    @Test
    fun test_GetForYouRails() = runTest {
        val semaphore = Semaphore(0)
        var count = 0
        forYouInterface.setPvrEnabled(true)
        forYouInterface.updateRailData()
        val updateJob = async(Dispatchers.Default) {
            delay(5000)
            forYouInterface.getForYouRails(object : IAsyncDataCallback<ArrayList<ForYouItem>> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouRails - onFailed")
                    semaphore.release()
                }
                override fun onReceive(data: ArrayList<ForYouItem>) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getForYouRails - onReceive - size: ${data.size}")
                    count = data.size
                    semaphore.release()
                }
            })
            semaphore.acquire()
            MatcherAssert.assertThat(count, CoreMatchers.not(0))
        }
        updateJob.await()
    }
    
}