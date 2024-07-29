package com.iwedia.cltv.platform.test.data_provider

import android.content.ContentResolver
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.test.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
class EpgProviderTest {
    private val TAG: String = "EpgProviderTest"

    private lateinit var epgDataProvider: EpgDataProviderInterface
    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        if((BuildConfig.FLAVOR == "base")||(BuildConfig.FLAVOR == "gretzky")) {
            val className = "com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider" // the name of the class you want to create
            val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
            val runtimeClass = clazz.constructors.find { it.parameters.size == 1 }

            assertThat(runtimeClass, notNullValue())

            val instance = runtimeClass!!.call(appContext)
            epgDataProvider = instance as EpgDataProviderInterface
        }
    }

    @After
    fun tearUp() {

    }

    @Test
    fun verify_event_list()= runTest {
        val eventsJob = async(Dispatchers.Default){
            delay(2000)
            epgDataProvider.getEventList()
        }
        val events = eventsJob.await();
        assertThat(events.size, not(0))
        Log.i(TAG,"Number of found events " + events.count());
        events.forEach { event ->
            Log.i(TAG,"Found event " + event.name);
        }
    }

    companion object {
    }
}
