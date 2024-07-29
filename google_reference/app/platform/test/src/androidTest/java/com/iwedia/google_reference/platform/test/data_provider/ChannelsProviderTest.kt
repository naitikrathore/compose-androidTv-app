package com.iwedia.cltv.platform.test.data_provider

import android.content.ContentResolver
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
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
class ChannelsProviderTest {
    private val TAG: String = "ChannelsProviderTest"

    private lateinit var contentResolver: ContentResolver
    private lateinit var channelDataProvider: ChannelDataProviderInterface
    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        if(BuildConfig.FLAVOR == "base") {
            val className = "com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider" // the name of the class you want to create
            val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
            val runtimeClass = clazz.constructors.find { it.parameters.size == 1 }

            assertThat(runtimeClass, notNullValue())

            val instance = runtimeClass!!.call(appContext)
            channelDataProvider = instance as ChannelDataProviderInterface
        }
       else if (BuildConfig.FLAVOR == "gretzky") {
            val className = "com.iwedia.cltv.platform.gretzky.com.iwedia.cltv.platform.gretzky.provider.ChannelDataProvider" // the name of the class you want to create
            val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
            val runtimeClass = clazz.constructors.find { it.parameters.size == 4 }

            assertThat(runtimeClass, notNullValue())

            val instance = runtimeClass!!.call(appContext)
            channelDataProvider = instance as ChannelDataProviderInterface
        }
    }

    @After
    fun tearUp() {

    }


    @Test
    fun verify_channel_list()= runTest {
        val channelsJob = async(Dispatchers.Default){
            delay(2000)
            channelDataProvider.getChannelList()
        }

        val channels = channelsJob.await();
        assertThat(channels.size, not(0))
        Log.i(TAG,"Number of found channels " + channels.size);
        channels.forEach { tvChannel ->
            Log.i(TAG,"Found channel " + tvChannel.name);
        }
    }

    companion object {
    }
}
