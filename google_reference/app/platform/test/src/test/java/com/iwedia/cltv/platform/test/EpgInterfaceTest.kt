package com.iwedia.cltv.platform.test

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.TunerType
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.*
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Semaphore
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class EpgInterfaceTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockEpgDataProvider: EpgDataProviderInterface
    private lateinit var mockChannelDataProvider: ChannelDataProviderInterface
    private lateinit var epgInterface: EpgInterface
    private lateinit var tvChannel: TvChannel
    private lateinit var tvEventList: ArrayList<TvEvent>
    private lateinit var tvList: ArrayList<TvChannel>
    private var events = HashMap<Int, ArrayList<TvEvent>>()

    @Before
    fun setUp() {
        tvList = arrayListOf(
            TvChannel(name = "Rai4", lcn = 41, tunerType = TunerType.TERRESTRIAL_TUNER_TYPE),
            TvChannel(name = "Rai2", lcn = 4, tunerType = TunerType.TERRESTRIAL_TUNER_TYPE),
            TvChannel(name = "Rai3", displayNumber = "5", tunerType = TunerType.TERRESTRIAL_TUNER_TYPE),
            TvChannel(name = "Rai5", displayNumber = "4", tunerType = TunerType.TERRESTRIAL_TUNER_TYPE)
        )
        tvChannel = TvChannel(id = 1, name = "Rai1", lcn = 52, tunerType = TunerType.TERRESTRIAL_TUNER_TYPE)
        tvEventList = arrayListOf(
            TvEvent(id = 1,
                tvChannel = tvChannel,
                name = "Event1",
                shortDescription = "Event1 Short Des",
                longDescription = "Event1 short des",
                imagePath = null,
                startTime = System.currentTimeMillis() - 100,
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
                tvChannel = tvChannel,
                name = "Event2",
                shortDescription = "Event2 Short Des",
                longDescription = "Event2 short des",
                imagePath = null,
                startTime = System.currentTimeMillis() - 200,
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
                tvChannel = tvChannel,
                name = "Event3",
                shortDescription = "Event3 Short Des",
                longDescription = "Event3 short des",
                imagePath = null,
                startTime = System.currentTimeMillis() - 300,
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
        events[tvChannel.id] = tvEventList

        mockEpgDataProvider = mock(EpgDataProviderInterface::class.java)
        `when`(mockEpgDataProvider.getEventList()).thenReturn(tvEventList)

        mockChannelDataProvider = mock(ChannelDataProviderInterface::class.java)
        `when`(mockChannelDataProvider.getChannelList()).thenReturn(tvList)

        val context = ApplicationProvider.getApplicationContext<Application>()

        if(BuildConfig.FLAVOR == "base") {
            val className = "com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl" // the name of the class you want to create
            val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
            val runtimeClass = clazz.constructors.find { it.parameters.size == 1 }

            MatcherAssert.assertThat(runtimeClass, CoreMatchers.notNullValue())
            MatcherAssert.assertThat(mockEpgDataProvider, CoreMatchers.notNullValue())
            val instance = runtimeClass!!.call(mockEpgDataProvider)
            epgInterface = instance as EpgInterface
        }
        else if (BuildConfig.FLAVOR == "mtk") {
            val className = "com.iwedia.cltv.platform.mk5.EpgInterfaceImpl"
            val clazz: KClass<*> = Class.forName(className).kotlin
            val runtimeClass = clazz.constructors.find { it.parameters.size == 3 }

            MatcherAssert.assertThat(runtimeClass, CoreMatchers.notNullValue())
            MatcherAssert.assertThat(mockEpgDataProvider, CoreMatchers.notNullValue())
            val instance = runtimeClass!!.call(context, mockEpgDataProvider, mockChannelDataProvider)
            epgInterface = instance as EpgInterface
        }
    }

    @Test
    fun getCurrentEventTest() = runTest {
        var tvEvent: TvEvent? = null
        val semaphore = Semaphore(0)

        epgInterface.getCurrentEvent(tvChannel, object : IAsyncDataCallback<TvEvent> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: TvEvent) {
                tvEvent = data
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.tvChannel, CoreMatchers.`is`(tvChannel))
        MatcherAssert.assertThat(tvEvent?.isInitialChannel, CoreMatchers.`is`(true))
    }

    @Test
    fun getAllCurrentEventTest() = runTest{
        `when`(mockEpgDataProvider.getEventList()).thenReturn(tvEventList)
        var tvEvent: ArrayList<TvEvent>? = null
        val semaphore = Semaphore(0)

        epgInterface.getAllCurrentEvent(object : IAsyncDataCallback<ArrayList<TvEvent>> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: ArrayList<TvEvent>) {
                tvEvent = data
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.notNullValue())
    }

    @Test
    fun getEventTest() = runTest {
        `when`(mockEpgDataProvider.getEventList()).thenReturn(tvEventList)
        var tvEvent: TvEvent? = null
        val semaphore = Semaphore(0)

        val index = 0
        epgInterface.getEvent(index, object : IAsyncDataCallback<TvEvent> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: TvEvent) {
                tvEvent = data
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.tvChannel, CoreMatchers.`is`(tvChannel))
        MatcherAssert.assertThat(tvEvent?.isInitialChannel, CoreMatchers.`is`(true))
    }

    @Test
    fun getEventByIdTest() = runTest {
        `when`(mockEpgDataProvider.getEventList()).thenReturn(tvEventList)
        var tvEvent: TvEvent? = null
        val semaphore = Semaphore(0)

        epgInterface.getEventById(1, object : IAsyncDataCallback<TvEvent> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: TvEvent) {
                tvEvent = data
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.id, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(tvEvent?.tvChannel, CoreMatchers.`is`(tvChannel))
        MatcherAssert.assertThat(tvEvent?.isInitialChannel, CoreMatchers.`is`(true))
    }

    @Test
    fun getEventListTest() = runTest{
        `when`(mockEpgDataProvider.getEventList()).thenReturn(tvEventList)
        var tvEvent: ArrayList<TvEvent>? = null
        val semaphore = Semaphore(0)

        epgInterface.getEventList(object : IAsyncDataCallback<ArrayList<TvEvent>> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: ArrayList<TvEvent>) {
                tvEvent = data
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.notNullValue())
    }

    @Test
    fun getEventListByChannelTEst() = runTest {
        `when`(mockEpgDataProvider.getEventChannelMap()).thenReturn(events)
        var tvEvent: ArrayList<TvEvent>? = null
        val semaphore = Semaphore(0)

        epgInterface.getEventListByChannel(
            tvChannel,
            object : IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onFailed(error: Error) {
                    semaphore.release()
                }

                override fun onReceive(data: ArrayList<TvEvent>) {
                    tvEvent = data
                    semaphore.release()
                }
            })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.get(0)?.id, CoreMatchers.`is`(1))
    }


    @Test
    fun getEventListByChannelAndTimeTest() = runTest {
        `when`(mockEpgDataProvider.getEventChannelMap()).thenReturn(events)
        var tvEvent: ArrayList<TvEvent>? = null
        val semaphore = Semaphore(0)

        epgInterface.getEventListByChannelAndTime(tvChannel,
            System.currentTimeMillis() - 50000,
            System.currentTimeMillis() + 500000000,
            object : IAsyncDataCallback<ArrayList<TvEvent>> {
                override fun onFailed(error: Error) {
                    semaphore.release()
                }

                override fun onReceive(data: ArrayList<TvEvent>) {
                    tvEvent = data
                    semaphore.release()
                }
            })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.get(0)?.id, CoreMatchers.`is`(1))
    }


    @Test
    fun getAllNextEventsTest() = runTest {
        tvChannel = TvChannel(name = "Rai1", lcn = 52)
        tvEventList = arrayListOf(
            TvEvent(
                id = 1,
                tvChannel = tvChannel,
                name = "Event1",
                shortDescription = "Event1 Short Des",
                longDescription = "Event1 short des",
                imagePath = null,
                startTime = System.currentTimeMillis() + 100000,
                endTime = System.currentTimeMillis() + 10000000,
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
        `when`(mockEpgDataProvider.getEventList()).thenReturn(tvEventList)

        var tvEvent: ArrayList<TvEvent>? = null
        val semaphore = Semaphore(0)

        epgInterface.getAllNextEvents(object : IAsyncDataCallback<ArrayList<TvEvent>> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: ArrayList<TvEvent>) {
                tvEvent = data
                semaphore.release()
            }
        })

        semaphore.acquire()
        MatcherAssert.assertThat(tvEvent, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(tvEvent?.size, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(tvEvent?.get(0)?.id, CoreMatchers.`is`(1))
    }
}