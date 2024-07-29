package com.iwedia.cltv.platform.test.player

import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.recording.Recording
import kotlinx.coroutines.*
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*

@RunWith(AndroidJUnit4::class)
class PlayerInterfaceTest {

    val TAG = javaClass.simpleName
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
    fun startPlayback_success() {
        var isPlaybackStarted: PlayerTestActivity.EventLog? = null
        mActivityRule.scenario.onActivity {
            scope.launch {
                it.switchOnChannel(0)
                val result = async {
                    delay(5000)

                    it.events
                }

                val events = result.await()
                isPlaybackStarted = events.find { event ->
                    event.name == "onPlaybackStarted"
                }
                assertThat(isPlaybackStarted, notNullValue())
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(5200))
    }

    @Test
    fun playRecordingItem_success_shouldStartPlayback() {
        val channelIndex = channels.indexOfFirst { it is Recording }
        assertThat(channelIndex, CoreMatchers.not(-1))
        mActivityRule.scenario.onActivity {
            it.channels = channels
            scope.launch {
                val result = async {
                    it.switchOnChannel(channelIndex)
                    delay(5000)
                    it.events
                }

                val events = result.await()
                var isPlaybackStarted = false
                var isTimeShiftAvailable = false
                events.forEach { event ->
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "playRecordingItem_success_shouldStartPlayback: ${event.name}")
                    when(event.name) {
                        "onPlaybackStarted" -> isPlaybackStarted = true
                        "onVideoUnAvailable", "onNoPlayback" -> isPlaybackStarted = false
                        "onTimeShiftStatusChanged" -> isTimeShiftAvailable = event.argumentsList[0] as Boolean
                    }
                }
                assertThat(isPlaybackStarted, CoreMatchers.`is`(true))
                assertThat(isTimeShiftAvailable, CoreMatchers.`is`(true))
            }
        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(8000))
    }

    @Test
    fun switchingChannels_success_verifyChannelsSwitchCorrectly() {}

    @Test
    fun switchingChannels_success_verifyPlayerStatusIsCorrectlyChanged() {}

    @Test
    fun playScrambledChannel_fail_channelShouldNotBeStreamed() {}

    @Test
    fun selectAudioTrack_success_shouldSwitchTrack() {
        mActivityRule.scenario.onActivity {
            it.channels = channels
            scope.launch {
                it.switchOnChannel(13)
                val result = async {
                    delay(4000)
                    it.events
                }

                val events = result.await()
                val tracksEvent = events.find { event ->
                    event.name == "onAudioTrackUpdated"
                }
                assertThat(tracksEvent, notNullValue())
                tracksEvent?.let { ev ->
                    val tracks = ev.argumentsList[0] as List<IAudioTrack>
                    if(tracks.size == 2) {
                        it.player.selectAudioTrack(tracks[1])
                    }
                }
                delay(1000)
            }

        }
        Espresso.onView(ViewMatchers.isRoot()).perform(waitFor(10200))
    }

    @Test
    fun selectSubtitleTrack_success_shouldDisplaySubtitles() {}

    @Test
    fun deselectSubtitles_success_shouldHideSubtitles() {}

    companion object {
        val channels = arrayListOf(
            TvChannel(10, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "My Headlines"),
            TvChannel(11, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "Current Events"),
            TvChannel(12, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "Science and Technology"),
            TvChannel(13, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "Business News"),
            TvChannel(14, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "Entertainment News"),
            TvChannel(15, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "International News"),
            TvChannel(16, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "Gaming News"),
            TvChannel(17, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "Politics"),
            TvChannel(10, inputId = "com.haystack.android/.tv.livechannel.LiveChannelInputService", name = "My Headlines"),
            TvChannel(29, inputId = "com.example.android.sampletvinput/.rich.RichTvInputService", name = "Google"),
            TvChannel(30, inputId = "com.example.android.sampletvinput/.rich.RichTvInputService", name = "Creative Commons"),
            TvChannel(31, inputId = "com.example.android.sampletvinput/.rich.RichTvInputService", name = "HLS"),
            TvChannel(32, inputId = "com.example.android.sampletvinput/.rich.RichTvInputService", name = "MPEG_DASH"),
            TvChannel(40, inputId = "com.google.android.tv.dtvinput/.DtvInputService", name = "My Custom Channel"),
            Recording(
                1,
                tvChannel = TvChannel(id = 40, inputId = "com.google.android.tv.dtvinput/.DtvInputService"),
                name = "Recording",
                duration = 299000,
                recordingDate = Date().time,
                recordingStartTime = Date().time,
                recordingEndTime = Date().time + 299000,
                shortDescription = "",
                image = "",
                videoUrl = "file:///Records/41_1680097680160/media.ts"
            )
        )
    }

}

fun waitFor(delay: Long): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = ViewMatchers.isRoot()
        override fun getDescription(): String = "wait for $delay milliseconds"
        override fun perform(uiController: UiController, v: View?) {
            uiController.loopMainThreadForAtLeast(delay)
        }
    }
}