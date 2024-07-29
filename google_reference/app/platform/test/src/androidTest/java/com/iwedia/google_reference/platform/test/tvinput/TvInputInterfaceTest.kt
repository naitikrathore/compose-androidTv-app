package com.iwedia.cltv.platform.test.tvinput

import android.app.Application
import android.media.tv.TvInputInfo
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.IAsyncDataCallback
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
class TvInputInterfaceTest {

    private val TAG: String = "TvInputInterfaceTest"
    private lateinit var tvInputInterface: TvInputInterface

    @Before
    fun setup() = runTest {
        val applicationContext = ApplicationProvider.getApplicationContext<Application>()
        val factory = ModuleFactory(applicationContext)
        withContext(Dispatchers.Main.immediate) {
            tvInputInterface = factory.createTvInputModule()
        }
    }

    @Test
    fun test_GetTvInputManager() = runTest {
        var tvInputManager = tvInputInterface.getTvInputManager()
        MatcherAssert.assertThat(tvInputManager, CoreMatchers.notNullValue())
    }

    @Test
    fun test_GetTvInputList() = runTest {
        val semaphore = Semaphore(0)
        var inputList : ArrayList<TvInputInfo>? = null
        tvInputInterface.getTvInputList(object : IAsyncDataCallback<ArrayList<TvInputInfo>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputList - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<TvInputInfo>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputList - onReceive")
                inputList = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(inputList?.size, CoreMatchers.not(0))
    }

    @Test
    fun test_GetTvInputFilteredList() = runTest {
        val semaphore = Semaphore(0)
        var inputList : ArrayList<TvInputInfo>? = null
        tvInputInterface.getTvInputFilteredList("com.google.android.videos",object : IAsyncDataCallback<ArrayList<TvInputInfo>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputFilteredList - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<TvInputInfo>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputFilteredList - onReceive")
                inputList = data
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(inputList?.size, CoreMatchers.not(0))
    }

    @Test
    fun test_GetChannelCountForInput() = runTest {
        val semaphore = Semaphore(0)
        var inputInfo : TvInputInfo? = null
        tvInputInterface.getTvInputList(object : IAsyncDataCallback<ArrayList<TvInputInfo>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputList - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: ArrayList<TvInputInfo>) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getTvInputList - onReceive")
                var inputList = data
                inputInfo = inputList.get(0)
                semaphore.release()
            }
        })
        semaphore.acquire()

        var result = false
        tvInputInterface.getChannelCountForInput(inputInfo!!, object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelCountForInput - onFailed")
                semaphore.release()
            }
            override fun onReceive(data: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelCountForInput - onReceive count is $data")
                result = true
                semaphore.release()
            }
        })
        semaphore.acquire()
        MatcherAssert.assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun test_IsParentalEnabled() = runTest {
        var isParentalOn = tvInputInterface.isParentalEnabled()
        MatcherAssert.assertThat(isParentalOn, CoreMatchers.`is`(true))
    }

}