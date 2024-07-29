package com.iwedia.cltv.platform.test.epg

import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.`interface`.EpgDataProviderInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
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
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.test.BuildConfig
import com.iwedia.cltv.platform.test.player.PlayerInterfaceTest
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
class EpgInterfaceTest {
    @Rule
    @JvmField
    val mActivityRule = ActivityScenarioRule(PlayerTestActivity::class.java)

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val TAG: String = "EpgInterfaceTest"

    private lateinit var channelDataProvider: ChannelDataProviderInterface
    private lateinit var epgDataProvider: EpgDataProviderInterface
    private lateinit var epgInterface: EpgInterface
    @Before
    fun setup() {
        mActivityRule.scenario.onActivity { activity ->
            activity.channels = PlayerInterfaceTest.channels
        }

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        if(BuildConfig.FLAVOR == "base") {
            val channelClassName = "com.iwedia.cltv.platform.base.content_provider.TifChannelDataProvider" // the name of the class you want to create
            val cclazz: KClass<*> = Class.forName(channelClassName).kotlin // get the class object using reflection
            val channelRuntimeClass = cclazz.constructors.find { it.parameters.size == 1 }

            MatcherAssert.assertThat(channelRuntimeClass, CoreMatchers.notNullValue())

            val instance1 = channelRuntimeClass!!.call(appContext)
            channelDataProvider = instance1 as ChannelDataProviderInterface

            val epgProviderClassName = "com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider" // the name of the class you want to create
            val eclazz: KClass<*> = Class.forName(epgProviderClassName).kotlin // get the class object using reflection
            val epgRuntimeClass = eclazz.constructors.find { it.parameters.size == 1 }

            MatcherAssert.assertThat(epgRuntimeClass, CoreMatchers.notNullValue())

            val instance2 = epgRuntimeClass!!.call(appContext)
            epgDataProvider = instance2 as EpgDataProviderInterface

            val epgInterfaceClassName = "com.iwedia.cltv.platform.base.EpgInterfaceBaseImpl" // the name of the class you want to create
            val epclazz: KClass<*> = Class.forName(epgInterfaceClassName).kotlin // get the class object using reflection
            val runtimeClass = epclazz.constructors.find { it.parameters.size == 1 }

            MatcherAssert.assertThat(runtimeClass, CoreMatchers.notNullValue())
            MatcherAssert.assertThat(epgDataProvider, CoreMatchers.notNullValue())
            val instance3 = runtimeClass!!.call(epgDataProvider)
            epgInterface = instance3 as EpgInterface
        }
        else if (BuildConfig.FLAVOR == "gretzky") {
            val channelClassName = "com.iwedia.cltv.platform.gretzky.com.iwedia.cltv.platform.gretzky.provider.ChannelDataProvider" // the name of the class you want to create
            val cclazz: KClass<*> = Class.forName(channelClassName).kotlin // get the class object using reflection
            val channelRuntimeClass = cclazz.constructors.find { it.parameters.size == 4 }

            MatcherAssert.assertThat(channelRuntimeClass, CoreMatchers.notNullValue())

            val instance1 = channelRuntimeClass!!.call(appContext)
            channelDataProvider = instance1 as ChannelDataProviderInterface

            val epgProviderClassName = "com.iwedia.cltv.platform.base.content_provider.TifEpgDataProvider" // the name of the class you want to create
            val eclazz: KClass<*> = Class.forName(epgProviderClassName).kotlin // get the class object using reflection
            val epgRuntimeClass = eclazz.constructors.find { it.parameters.size == 1 }

            MatcherAssert.assertThat(epgRuntimeClass, CoreMatchers.notNullValue())

            val instance2 = epgRuntimeClass!!.call(appContext)
            epgDataProvider = instance2 as EpgDataProviderInterface

            val epgInterfaceClassName = "com.iwedia.cltv.platform.gretzky.com.iwedia.cltv.platform.gretzky.EpgInterfaceImpl" // the name of the class you want to create
            val epclazz: KClass<*> = Class.forName(epgInterfaceClassName).kotlin // get the class object using reflection
            val runtimeClass = epclazz.constructors.find { it.parameters.size == 1 }

            MatcherAssert.assertThat(runtimeClass, CoreMatchers.notNullValue())
            MatcherAssert.assertThat(epgDataProvider, CoreMatchers.notNullValue())
            val instance3 = runtimeClass!!.call(epgDataProvider)
            epgInterface = instance3 as EpgInterface
        }
    }

    @After
    fun tearDown() {
        mActivityRule.scenario.close()
    }

    @Test
    fun change_channel_and_check_current_event() = runTest {
        var isPlaybackStarted: PlayerTestActivity.EventLog? = null
        val channelList: ArrayList<TvChannel> = channelDataProvider.getChannelList();

        mActivityRule.scenario.onActivity {
            val moduleFactory = ModuleFactory(it.application)
            val tvInterface = moduleFactory.createTvModule(it.player, moduleFactory.createNetworkModule(), moduleFactory.createTvInputModule() )

            scope.launch {
                it.switchOnChannel(0);
                delay(2000)
                Log.i(TAG,"Number of found channels " + channelList.size);
                channelList.forEach { tvChannel ->
                    val result = async {
                        tvInterface.changeChannel(tvChannel,object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                assert(false)
                            }
                            override fun onSuccess() {
                                Log.i(TAG,"Changed channel on " + tvChannel.name);
                            }
                        })
                        epgInterface.getCurrentEvent(tvChannel, object : IAsyncDataCallback<TvEvent> {
                            override fun onFailed(error: Error) {
                                assert(false)
                            }

                            override fun onReceive(tvEvent: TvEvent) {
                                Log.i(TAG,"Current event is " + tvEvent.name);
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
        val delay: Long = ((channelList.size + 1) * 5000).toLong();
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(delay))
    }

}
