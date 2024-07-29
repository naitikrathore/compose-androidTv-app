package com.iwedia.cltv.platform.test.ttx

import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.TTXInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.test.player.PlayerInterfaceTest
import com.iwedia.cltv.platform.test.player.PlayerTestActivity
import com.iwedia.cltv.platform.test.player.waitFor
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TTXInterfaceTest {

    @Rule
    @JvmField
    val mActivityRule = ActivityScenarioRule(PlayerTestActivity::class.java)
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    @Before
    fun setup() {
        mActivityRule.scenario.onActivity { activity ->
            activity.channels = channels
        }
    }

    @After
    fun tearDown() {
        mActivityRule.scenario.close()
    }

    @Test
    fun startTTXTest() = runTest {
        mActivityRule.scenario.onActivity {
            scope.launch {
                it.switchOnChannel(0)
                val result = async {
                    delay(3000)
                    it.events
                }

                val events = result.await()
                val isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                assertThat(isPlaybackStarted, notNullValue())

                it.startTtx()
                delay(13000)
            }
        }

        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(8000))
    }

    companion object {
        val channels = arrayListOf<PlayableItem>(
            TvChannel(52, inputId = "com.google.android.tv.dtvinput/.DtvInputService", name = "Rai 1 HD")
        )
    }

}
