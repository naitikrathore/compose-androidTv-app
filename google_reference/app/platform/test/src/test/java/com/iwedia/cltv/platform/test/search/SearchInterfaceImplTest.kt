package com.iwedia.cltv.platform.test.search

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.RepeatFlag
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.test.BuildConfig
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SearchInterfaceImplTest {
    private lateinit var searchInterface : SearchInterface
    private lateinit var mockChannelProvider : ChannelDataProviderInterface
    private lateinit var mockEpgDataProvider: EpgDataProviderInterface
    private lateinit var fakePvrInterface : PvrInterface
    private lateinit var fakeScheduledInterface : ScheduledInterface
    val TAG = javaClass.simpleName
    @Before
    fun setup(){
        mockChannelProvider = mock(ChannelDataProviderInterface::class.java)
        mockEpgDataProvider = mock(EpgDataProviderInterface::class.java)
        fakePvrInterface = PvrFakeInterfaceImpl(MutableLiveData(false))
        fakeScheduledInterface = ScheduledFakeInterfaceImpl()

        if(BuildConfig.FLAVOR == "base") {
            val className = "com.iwedia.cltv.platform.base.SearchInterfaceBaseImpl" // the name of the class you want to create
            val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
            val runtimeClass = clazz.constructors.find { it.parameters.size == 4 }

            assertThat(runtimeClass, notNullValue())
            assertThat(mockEpgDataProvider, notNullValue())

            val instance = runtimeClass!!.call(mockChannelProvider, mockEpgDataProvider, fakePvrInterface, fakeScheduledInterface)
            searchInterface = instance as SearchInterface
        }
        else if (BuildConfig.FLAVOR == "gretzky") {
            val className = "com.iwedia.cltv.platform.gretzky.SearchInterfaceImpl" // the name of the class you want to create
            val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
            val runtimeClass = clazz.constructors.find { it.parameters.size == 4 }

            assertThat(runtimeClass, notNullValue())
            assertThat(mockEpgDataProvider, notNullValue())

            val instance = runtimeClass!!.call(mockChannelProvider, mockEpgDataProvider, fakePvrInterface, fakeScheduledInterface)
            searchInterface = instance as SearchInterface
        }
    }

    @Test
    fun verify_channels_search() = runBlocking {
        val tvList: ArrayList<TvChannel> = arrayListOf(
            TvChannel(name = "Rai4", lcn = 41),
            TvChannel(name = "Rai2", lcn = 4),
            TvChannel(name = "Rai3", displayNumber = "5"),
            TvChannel(name = "Rai5", displayNumber = "4")
        )
        var searchedTvList : List<TvChannel>? = null

        val semaphore = Semaphore(1,1)
        `when`(mockChannelProvider.getChannelList()).thenReturn(tvList)
        searchInterface.searchForChannels("4", object : IAsyncDataCallback<List<TvChannel>> {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
                semaphore.release()
            }
            override fun onReceive(data: List<TvChannel>) {
                searchedTvList = data
                semaphore.release()
            }
        })

        assertThat(withTimeoutOrNull(1000) { semaphore.acquire() }, notNullValue())

        verify(mockChannelProvider).getChannelList()
        assertThat(searchedTvList?.size, `is`(3))
        assertThat(searchedTvList?.get(0)?.name, `is`("Rai4"))
        assertThat(searchedTvList?.get(1)?.name, `is`("Rai2"))
        assertThat(searchedTvList?.get(2)?.name, `is`("Rai5"))
    }

    @Test
    fun verify_event_search() = runBlocking {
        val tvEvent: ArrayList<TvEvent> = arrayListOf(
            TvEvent(-1, TvChannel(name = "Rai4"), "tvEvent1", "", "", "",
                0, 0, null, 0, 0, null, "",
                false, false, null, null, null),
            TvEvent(-1, TvChannel(name = "Rai5"), "tvEvent4", "", "", "",
                0, 0, null, 0, 0, null, "",
                false, false, null, null, null),
            TvEvent(-1, TvChannel(name = "Rai2"), "tvEvent2", "", "", "",
                5, 0, null, 0, 0, null, "",
                false, false, null, null, null),
            TvEvent(-1, TvChannel(name = "Rai5"), "tvEvent5", "", "", "",
                0, 0, null, 0, 0, null, "",
                false, false, null, null, null))

        var searchedTvEventList : List<TvEvent>? = null

        val semaphore = Semaphore(1,1)
        `when`(mockEpgDataProvider.getEventList()).thenReturn(tvEvent)

        searchInterface.searchForEvents("4", object : IAsyncDataCallback<List<TvEvent>>{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: $error")
                semaphore.release()
            }
            override fun onReceive(data: List<TvEvent>) {
                searchedTvEventList = data
                semaphore.release()
            }
        })

        assertThat(withTimeoutOrNull(1000) { semaphore.acquire() }, notNullValue())

        assertThat(searchedTvEventList?.size, `is`(3))
        verify(mockEpgDataProvider).getEventList()
        assertThat(searchedTvEventList?.get(0)?.name, `is`("tvEvent1"))
        assertThat(searchedTvEventList?.get(1)?.name, `is`("tvEvent4"))
        assertThat(searchedTvEventList?.get(2)?.name, `is`("tvEvent2"))
    }

    @Test
    fun verify_recording_search() = runBlocking {
        val recordings: ArrayList<Recording> = arrayListOf(
            Recording(-1, "rec1", 0, 0, "", "url1", TvChannel(name = "Rai4"),
                null, 0, 0, "",""),
            Recording(-1, "rec4", 0, 0, "", "url2", TvChannel(name = "Rai2"),
                null, 0, 0, "",""),
            Recording(-1, "rec3", 0, 0, "", "url4", TvChannel(name = "Rai3"),
                null, 0, 0, "",""),
            Recording(-1, "rec3", 5, 0, "", "url5", TvChannel(name = "Rai5"),
                null, 0, 0, "","")
        )

        var searchedRecordingsList : List<Recording>? = null

        val semaphore = Semaphore(1,1)
        val fakeInterfaceImpl = fakePvrInterface as PvrFakeInterfaceImpl
        fakeInterfaceImpl.recordingQueryList = recordings

        searchInterface.searchForRecordings("4", object : IAsyncDataCallback<List<Recording>> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: List<Recording>) {
                searchedRecordingsList = data
                semaphore.release()
            }
        })

        assertThat(withTimeoutOrNull(1000) { semaphore.acquire() }, notNullValue())

        assertThat(searchedRecordingsList?.size, `is`(3))
        assertThat(searchedRecordingsList?.get(0)?.name, `is`("rec1"))
        assertThat(searchedRecordingsList?.get(1)?.name, `is`("rec4"))
        assertThat(searchedRecordingsList?.get(2)?.name, `is`("rec3"))
    }

    @Test
    fun verify_scheduled_recording_search() = runBlocking {
        val scheduledRecordings: ArrayList<ScheduledRecording> = arrayListOf(
            ScheduledRecording(-1, "sch1", 6, 0, 0, null, RepeatFlag.NONE, TvChannel(name = "Rai4"),
                null),
            ScheduledRecording(-1, "sch4", 7, 0, 0, null, RepeatFlag.NONE, TvChannel(name = "Rai2"),
                null),
            ScheduledRecording(-1, "sch3", 1, 0,0, null, RepeatFlag.NONE,  TvChannel(name = "Rai3"),
                null),
            ScheduledRecording(-1, "sch5", 5, 0, 0, null, RepeatFlag.NONE, TvChannel(name = "Rai5"),
                null)
        )

        var scheduledRecordingsList : List<ScheduledRecording>? = null

        val semaphore = Semaphore(1,1)
        val fakeInterfaceImpl = fakeScheduledInterface as ScheduledFakeInterfaceImpl
        fakeInterfaceImpl.scheduledRecordingsQueryList = scheduledRecordings

        searchInterface.searchForScheduledRecordings("4", object : IAsyncDataCallback<List<ScheduledRecording>> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: List<ScheduledRecording>) {
                scheduledRecordingsList = data
                semaphore.release()
            }
        })

        assertThat(withTimeoutOrNull(1000) { semaphore.acquire() }, notNullValue())

        assertThat(scheduledRecordingsList?.size, `is`(3))
        assertThat(scheduledRecordingsList?.get(0)?.name, `is`("sch1"))
        assertThat(scheduledRecordingsList?.get(1)?.name, `is`("sch4"))
        assertThat(scheduledRecordingsList?.get(2)?.name, `is`("sch3"))
    }

    @Test
    fun verify_scheduled_reminder_search() = runBlocking {
        val scheduledReminders: ArrayList<ScheduledReminder> = arrayListOf(
            ScheduledReminder(-1, "sch1", TvChannel(name = "Rai4"),
                TvEvent(-1, TvChannel(name = "Rai4"), "tvEvent1", "", "", "",
                    0, 0, null, 0, 0, null, "",
                    false, false, null, null, null)),
            ScheduledReminder(-1, "sch4", TvChannel(name = "Rai2"),
                TvEvent(-1, TvChannel(name = "Rai5"), "tvEvent4", "", "", "",
                    0, 0, null, 0, 0, null, "",
                    false, false, null, null, null)),
            ScheduledReminder(-1, "sch3", TvChannel(name = "Rai4"),
                TvEvent(-1, TvChannel(name = "Rai2"), "tvEvent3", "", "", "",
                    5, 0, null, 0, 0, null, "",
                    false, false, null, null, null),),
            ScheduledReminder(-1, "sch5",  TvChannel(name = "Rai5"),
                TvEvent(-1, TvChannel(name = "Rai5"), "tvEvent5", "", "", "",
                    0, 0, null, 0, 0, null, "",
                    false, false, null, null, null))
        )

        var scheduledRemindersList : List<ScheduledReminder>? = null

        val semaphore = Semaphore(1,1)
        val fakeInterfaceImpl = fakeScheduledInterface as ScheduledFakeInterfaceImpl
        fakeInterfaceImpl.scheduledReminderQueryList = scheduledReminders

        searchInterface.searchForScheduledReminders("4", object : IAsyncDataCallback<List<ScheduledReminder>> {
            override fun onFailed(error: Error) {
                semaphore.release()
            }

            override fun onReceive(data: List<ScheduledReminder>) {
                scheduledRemindersList = data
                semaphore.release()
            }
        })

        assertThat(withTimeoutOrNull(1000) { semaphore.acquire() }, notNullValue())

        assertThat(scheduledRemindersList?.size, `is`(3))
        assertThat(scheduledRemindersList?.get(0)?.name, `is`("sch1"))
        assertThat(scheduledRemindersList?.get(1)?.name, `is`("sch4"))
        assertThat(scheduledRemindersList?.get(2)?.name, `is`("sch3"))
    }
}
