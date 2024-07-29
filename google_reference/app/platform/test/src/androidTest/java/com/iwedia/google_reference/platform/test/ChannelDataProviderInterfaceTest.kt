package com.iwedia.cltv.platform.test

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.model.TvChannel
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.*
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
class ChannelDataProviderInterfaceTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var channelDataProvider: ChannelDataProviderInterface

    private var tvChannelList: ArrayList<TvChannel> = arrayListOf(
        TvChannel(name = "Rai4", lcn = 41),
        TvChannel(name = "Rai2", lcn = 4),
        TvChannel(name = "Rai3", displayNumber = "5"),
        TvChannel(name = "Rai5", displayNumber = "4")
    )
    private var tvChannel: TvChannel = tvChannelList[0]

    @Before
    fun setup() {
        if(BuildConfig.FLAVOR == "base") {
            val className =
                "com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider" // the name of the class you want to create
            val clazz: KClass<*> = Class.forName(className).kotlin // get the class object using reflection
            val runtimeClass = clazz.constructors.find { it.parameters.size == 1 }

            assertThat(runtimeClass, notNullValue())

            val instance = runtimeClass!!.call(context)
            channelDataProvider = instance as ChannelDataProviderInterface
        }
    }

    @Test
    fun getChannelListTest() = runTest {
        val list = channelDataProvider.getChannelList()
        assertThat(list, notNullValue())
    }

    /*@Test
    fun lockUnlockChannelTest() = runTest {
        val result = channelDataProvider.lockUnlockChannel(tvChannel, true)
        assertThat(result, `is`(true))
    }

    @Test
    fun skipUnSkipChannelTest() = runTest {
        val result = channelDataProvider.skipUnskipChannel(tvChannel, skip = true)
        assertThat(result, `is`(true))
    }

    @Test
    fun enableLcnTest() = runTest {
        assertThat(channelDataProvider.enableLcn(true), notNullValue())
    }

    @Test
    fun isLcnEnabledTest() = runTest {
        val result = channelDataProvider.isLcnEnabled()
        assertThat(result, `is`(true))
    }

    @Test
    fun deleteChannelTest() = runTest {
        val result = channelDataProvider.deleteChannel(tvChannel)
        assertThat(result, `is`(true))
    }*/
}