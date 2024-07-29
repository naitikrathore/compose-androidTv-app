package com.iwedia.cltv.platform.test.player

import android.media.tv.TvContentRating
import android.media.tv.TvView
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.platform.ModuleFactory
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.test.test.R
import kotlinx.coroutines.Job
import java.util.*

class PlayerTestActivity : AppCompatActivity(R.layout.activity_player_test), PlayerInterface.PlayerListener {
    private var currentChannel = 0
    private var currentAudioTrackIndex = -1
    private var currentSubtitleTrackIndex = -1
    private lateinit var channelNumView: TextView
    private lateinit var channelName: TextView
    private lateinit var progressTimeView: TextView
    private lateinit var progressBar: AppCompatSeekBar
    lateinit var liveTvView: TvView
    private lateinit var blackOverlayView: View
    private lateinit var trackRecyclerView: RecyclerView
    private lateinit var ttxSurface: SurfaceView
    private val trackAdapter = TrackAdapter()

    private var isPlayerStarted = false

    private var isTimeShiftAvailable = false
    private var timeShiftStartPosition = 0L
    private var timeShiftCurrentTimePosition = 0L
    private var streamDuration = "00:00:00"

//    private val displayModes = arrayListOf(DisplayMode.FULL, DisplayMode.NORMAL, DisplayMode.ZOOM)
    private var displayModeIndex = 0

    private lateinit var seekJob: Job
    private var isSeekInProgress = false

    /* Testing data */
    private var eventOrder: Int = 0
    data class EventLog(
        val name: String,
        val argumentsList: List<Any>,
        val eventOrderNum: Int
    )
    val events: MutableList<EventLog> = mutableListOf()
    lateinit var channels: ArrayList<PlayableItem>

    val factory by lazy {
        ModuleFactory(application)
    }

    val util by lazy {
        factory.createUtilsModule()
    }

    val epg by lazy {
        factory.createEpgModule()
    }

    val player by lazy {
        factory.createPlayerModule(util,epg)
    }

    val tv by lazy {
        factory.createTvModule(player,factory.createNetworkModule(), factory.createTvInputModule() )
    }

    val ttx by lazy {
        factory.createTTXModule()
    }

    val timeShiftInterface by lazy {
        factory.createTimeshiftModule(player)
    }

    private val trackSelectListener = object: TrackAdapter.TrackAdapterListener {
        override fun onItemSelected(itemPosition: Int) {
            player.selectAudioTrack(player.getAudioTracks()[itemPosition])
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelNumView = findViewById(R.id.channel_number)
        channelNumView.text = currentChannel.toString()

        channelName = findViewById(R.id.channel_name)

        progressTimeView = findViewById(R.id.content_progress_time)
        progressTimeView.visibility = View.INVISIBLE
        progressBar = findViewById(R.id.content_progress_seek_bar)
        progressBar.setOnClickListener {
            if(player.playerState == PlayerState.PLAYING) {
                player.pause()
            }
            else {
                player.resume()
            }
        }
        /*progressBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    if(this@PlayerTestActivity::seekJob.isInitialized && seekJob.isActive) {
                        seekJob.cancel()
                    }

                    isSeekInProgress = true
                    seekJob = lifecycleScope.launch {
                        delay(700)
                        player.seek(timeShiftCurrentTimePosition, false)
                        isSeekInProgress = false
                    }
                    val m = channels[currentChannel].durationMS.toDouble() * progress / 100
                    timeShiftCurrentTimePosition = timeShiftStartPosition + m.roundToLong()

                    progressTimeView.text = getString(
                        R.string.start_end,
                        formatTime(timeShiftCurrentTimePosition-timeShiftStartPosition),
                        streamDuration
                    )
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {println("onStopTrackingTouch")}
        })*/

        ttxSurface = findViewById(R.id.ttx_surface)
        trackRecyclerView = findViewById(R.id.trackRecyclerView)
        trackRecyclerView.layoutManager =
            LinearLayoutManager(baseContext, LinearLayoutManager.VERTICAL, false)
        trackRecyclerView.adapter = trackAdapter
        trackAdapter.registerListener(trackSelectListener)

        findViewById<ImageButton>(R.id.audio_btn).setOnClickListener {
            if(trackRecyclerView.isVisible){
                trackRecyclerView.visibility = View.GONE
            }
            else {
                trackAdapter.onTracksDataUpdate(player.getAudioTracks(), currentAudioTrackIndex)
                trackRecyclerView.visibility = View.VISIBLE
            }
        }

        blackOverlayView = findViewById(R.id.black_overlay)
        liveTvView = findViewById(R.id.tvView)

        player.setPlaybackView(liveTvView)
        timeShiftInterface.setLiveTvView(liveTvView)
    }

    override fun onStart() {
        super.onStart()
        player.registerListener(this)
        channels = tv.getChannelList() as ArrayList<PlayableItem>
//        playChannel()
    }

    override fun onPause() {
        super.onPause()
        player.unregisterListener(this)
        if(isPlayerStarted) {
            isPlayerStarted = false
            player.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackAdapter.unregisterListeners(trackSelectListener)
    }

    private fun nextChannel() {
        currentChannel = (currentChannel + 1) % channels.size
        playChannel()
    }

    private fun previousChannel() {
        currentChannel = ( if(currentChannel == 0) channels.size else currentChannel ) - 1
        playChannel()
    }

    fun switchOnChannel(num: Int) {
        if(num in 0 until channels.size) {
            currentChannel = num
            playChannel()
        }
    }

    private fun playChannel() {
        if(trackRecyclerView.isVisible) { trackRecyclerView.visibility = View.GONE }
        channelNumView.text = currentChannel.toString()
        channelName.text = getChannelName(channels[currentChannel])

        isTimeShiftAvailable = false
        progressTimeView.visibility = View.INVISIBLE
        progressBar.focusable = View.NOT_FOCUSABLE
        progressBar.progress = 100
        player.play(channels[currentChannel])
        isPlayerStarted = true
    }

    fun startTtx() {
        ttxSurface.visibility = View.VISIBLE
        ttx.startTTX(baseContext, liveTvView, ttxSurface)
    }

    override fun onBackPressed() {}

    override fun onNoPlayback() {
        log(this::onNoPlayback.name)
    }

    override fun onPlaybackStarted() {
        log(this::onPlaybackStarted.name)
    }

    override fun onAudioTrackUpdated(audioTracks: List<IAudioTrack>) {
        log(this::onAudioTrackUpdated.name, listOf(audioTracks))
        if(audioTracks.isNotEmpty()) {
            currentAudioTrackIndex = 0
            player.selectAudioTrack(audioTracks[currentAudioTrackIndex])
        }
    }

    override fun onSubtitleTrackUpdated(subtitleTracks: List<ISubtitle>) {
        log(this::onSubtitleTrackUpdated.name, listOf(subtitleTracks))
    }

    override fun onVideoAvailable() {
        log(this::onVideoAvailable.name)
        blackOverlayView.visibility = View.GONE
    }

    override fun onVideoUnAvailable(reason: Int) {
        log(this::onVideoUnAvailable.name)
        blackOverlayView.visibility = View.VISIBLE
    }

    override fun onContentAvailable() {
        log(this::onContentAvailable.name)
        blackOverlayView.visibility = View.GONE
    }

    override fun onContentBlocked(rating: TvContentRating) {
        log(this::onContentBlocked.name)
        blackOverlayView.visibility = View.VISIBLE
    }

    override fun onTimeShiftStatusChanged(inputId: String, status: Boolean) {
        log(this::onTimeShiftStatusChanged.name, listOf(status))
        isTimeShiftAvailable = status
        if(isTimeShiftAvailable) {
            progressBar.focusable = View.FOCUSABLE
        }
        else {
            progressBar.focusable = View.NOT_FOCUSABLE
        }
//        streamDuration = formatTime(channels[currentChannel].durationMS)
    }

//    override fun onTimeShiftStartPositionChanged(inputId: String, timeMs: Long) {
//        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeShiftStartPositionChanged: $inputId | ${formatTime(timeMs)} | $isTimeShiftAvailable")
//        timeShiftStartPosition = timeMs
//    }
//
//    override fun onTimeShiftCurrentPositionChanged(inputId: String, timeMs: Long) {
//        if(!isSeekInProgress) {
//            val progress = ((timeMs - timeShiftStartPosition).toDouble() / channels[currentChannel].durationMS) * 100
//            progressBar.setProgress(progress.roundToInt(), true)
//
//            timeShiftCurrentTimePosition = timeMs
//            val currentStreamPosition = formatTime(timeMs-timeShiftStartPosition)
//
//            Log.i(TAG, "onTimeShiftCurrentPositionChanged:$inputId | $currentStreamPosition")
//
//            progressTimeView.text = getString(
//                R.string.start_end,
//                currentStreamPosition,
//                streamDuration
//            )
//            if(!progressTimeView.isVisible) {
//                progressTimeView.visibility = View.VISIBLE
//            }
//        }
//    }

    private fun log(methodName: String, argumentsList: List<Any> = emptyList()) {
        events.add(EventLog(methodName, argumentsList, ++eventOrder))
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "log: $methodName :: $argumentsList")
    }

    private fun getChannelName(channel: PlayableItem): String {
        return when (channel) {
            is TvChannel -> channel.name
            is Recording -> channel.name
            else -> ""
        }
    }

    companion object Channels {

        fun formatTime(milliseconds: Long): String {
            val totalSeconds = milliseconds / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }

        const val TAG = "PlayerTestApp"
}

}
