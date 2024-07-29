package com.iwedia.cltv.anoki_fast

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.VerticalGridView
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.components.CheckListAdapter
import com.iwedia.cltv.components.CheckListItem
import com.iwedia.cltv.components.FadeAdapter
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.Type

/**
 * Fast audio subtitle list Constraint Layout
 *
 * @author Dejan Nadj
 */
class FastAudioSubtitleList(
    private val context: Context,
    private val dataProvider: FastZapBannerDataProvider,
    private val listener: Listener,
    backCallback: (buttonId: Int) -> Unit
) : ConstraintLayout(context) {
    /**
    tracksGridView is VerticalGridView used for displaying Audio or Subtitle tracks.
     */
    private var tracksVerticalGridView: VerticalGridView

    /**
     * sideViewWrapper is important for handling visibility of the tracksVerticalGridView (used for displaying and hiding Audio or Subtitle tracks).
     */
    var tracksWrapperLinearLayout: LinearLayout? = null

    /**
     * tracksCheckListAdapter is adapter used
     */
    private var audioTracksCheckListAdapter: CheckListAdapter
    private var subtitleTracksCheckListAdapter: CheckListAdapter

    /**
     * tracksTitle is title of the tracksGridView used to distinguish whether GridView contains Audio or Subtitle tracks
     */
    private var tracksTitle: TextView? = null
    var currentSelectedAudioTrack: IAudioTrack? = null
    var currentSelectedSubtitleTrack: ISubtitle? = null
    private var fadingEdgeLayout: FadingEdgeLayout
    private var audioTracks: MutableList<IAudioTrack>? = null
    private var subtitleTracks: MutableList<ISubtitle>? = null

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.fast_audio_subtitles_list_layout, this, true)
        tracksTitle = findViewById(R.id.title)

        tracksVerticalGridView = findViewById(R.id.side_view_vertical_grid_view)
        tracksWrapperLinearLayout = findViewById(R.id.audio_and_subtitles_container)

        Utils.makeGradient(
            view = tracksWrapperLinearLayout!!,
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            listOfColor = intArrayOf(
                Color.parseColor("#1a202b"),
                Color.parseColor("#22272f"),
                Color.parseColor("#00000000")
            )
        )

        tracksVerticalGridView.setNumColumns(1)
        fadingEdgeLayout = findViewById(R.id.fading_edge_layout)
        audioTracksCheckListAdapter = CheckListAdapter(
            fadingEdgeLayout = fadingEdgeLayout,
            FadeAdapter.FadeAdapterType.VERTICAL
        )

        audioTracksCheckListAdapter.adapterListener =
            object : CheckListAdapter.CheckListAdapterListener {
                override fun onItemClicked(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                    currentSelectedAudioTrack = audioTracks!![position]
                    dataProvider.audioTrackSelected(currentSelectedAudioTrack!!)
                }

                override fun onAdditionalItemClicked() {
                    // NOT IMPORTANT for Audio tracks. There is no "Off" button in Audio.
                }

                override fun onUpPressed(position: Int): Boolean {
                    val nextPosition = if (position > 0) position - 1 else position
                    tracksVerticalGridView.layoutManager?.findViewByPosition(nextPosition)
                        ?.requestFocus()
                    return true
                }

                override fun onDownPressed(position: Int): Boolean {
                    val nextPosition = if (position < audioTracks!!.size) position + 1 else position
                    tracksVerticalGridView.layoutManager?.findViewByPosition(nextPosition)
                        ?.requestFocus()
                    return true
                }

                override fun onBackPressed(): Boolean {
                    tracksWrapperLinearLayout!!.visibility = View.GONE
                    backCallback(1)
                    return true
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

            }

        subtitleTracksCheckListAdapter = CheckListAdapter(
            fadingEdgeLayout = fadingEdgeLayout,
            FadeAdapter.FadeAdapterType.VERTICAL
        )

        subtitleTracksCheckListAdapter.adapterListener =
            object : CheckListAdapter.CheckListAdapterListener {
                override fun onItemClicked(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                    if(subtitleTracks!!.size > 0){
                        currentSelectedSubtitleTrack = subtitleTracks!![position]
                        dataProvider.subtitleTrackSelected(currentSelectedSubtitleTrack!!)
                    }
                }

                override fun onAdditionalItemClicked() {  // called when user clicks to the "Off" button in CheckListAdapter for Subtitles
                    dataProvider.enableSubtitles(false)
                }

                override fun onUpPressed(position: Int): Boolean {
                    val nextPosition = if (position > 0) position - 1 else position
                    tracksVerticalGridView.layoutManager?.findViewByPosition(nextPosition)
                        ?.requestFocus()
                    return true
                }

                override fun onDownPressed(position: Int): Boolean {
                    val nextPosition =
                        if (position < subtitleTracks!!.size) position + 1 else position
                    tracksVerticalGridView.layoutManager?.findViewByPosition(nextPosition)
                        ?.requestFocus()
                    return true
                }

                override fun onBackPressed(): Boolean {
                    visibility = View.GONE
                    tracksWrapperLinearLayout!!.visibility = View.GONE
                    backCallback(0)
                    return true
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                    listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                }

            }
    }

    /**
     * Sets up subtitle tracks for display and interaction.
     * Displays a Toast message if there are no available subtitle tracks.
     *
     * @return `true` if subtitle tracks are available, `false` otherwise.
     */
    private fun setupSubtitleTracks(): Boolean {
        subtitleTracks = dataProvider.getAvailableSubtitleTracks()
        tracksVerticalGridView.adapter =
            subtitleTracksCheckListAdapter // used to specify that adapter will be used for subtitle tracks.

        if (subtitleTracks.isNullOrEmpty()) {
            listener.showToast(ConfigStringsManager.getStringById("no_available_subtitle_tracks_msg"))
            return false //tracks failed with loading
        }

        return true //tracks are loaded successfully
    }

    /**
     * Show available subtitle track list
     * Active subtitle track is checked if subtitle option is enabled
     * Off option is checked if subtitles are disabled
     */
    fun showSubtitles() {
        val areSubtitleTracksSuccessfullyLoaded = setupSubtitleTracks()
        if (areSubtitleTracksSuccessfullyLoaded.not()) return

        //----------------------------------------------------------------------------------------\\
        //--------------- Execute code only if there are available SUBTITLE tracks. --------------\\
        //----------------------------------------------------------------------------------------\\
        visibility = View.VISIBLE

        currentSelectedSubtitleTrack = dataProvider.getCurrentSubtitleTrack()

        var currentSelectedPosition = 0

        val subtitleCheckListItems = mutableListOf<CheckListItem>()

        var similarNameCountMap = mutableMapOf<String, Pair<Int, Int>>()

        subtitleTracks!!.forEachIndexed { index, track ->
            var name = track.trackName
            if (similarNameCountMap[name] == null) {
                similarNameCountMap[name] = Pair(1, 0)
            } else {
                var pair = similarNameCountMap[name]
                similarNameCountMap[name] = Pair(pair!!.first + 1, 0)
            }
        }
        subtitleTracks!!.forEachIndexed { index, track ->
            val infoIcons = mutableListOf<Int>()
            var key = track.trackName
            var trackName = track.trackName
            var countIndexPair = similarNameCountMap[key]!!
            //if single entry of same name then no need to add index
            if (countIndexPair.first == 1 && countIndexPair.second == 0) {
                trackName = track.trackName
            } else {
                if (countIndexPair.first >= 1) {
                    similarNameCountMap[key] = Pair(countIndexPair.first - 1, countIndexPair.second + 1)
                    trackName += " ${countIndexPair.second + 1}"
                }
            }
            val isSubtitleEnabled = dataProvider.isSubtitlesEnabled()
            if (track.trackId == currentSelectedSubtitleTrack?.trackId && isSubtitleEnabled) {
                subtitleCheckListItems.add(CheckListItem(trackName, true, infoIcons))
                currentSelectedPosition = index
            } else {
                subtitleCheckListItems.add(CheckListItem(trackName, false, infoIcons))
            }
        }

        tracksTitle!!.text = ConfigStringsManager.getStringById("subtitles")
        subtitleTracksCheckListAdapter.refreshWithAdditionalItem(
            adapterItems = subtitleCheckListItems,
            name = ConfigStringsManager.getStringById("off"),
            isChecked = !dataProvider.isSubtitlesEnabled()
        )

        if (!dataProvider.isSubtitlesEnabled()) {
            currentSelectedPosition = subtitleTracks!!.size
        }


        tracksWrapperLinearLayout?.postDelayed({
            tracksWrapperLinearLayout!!.visibility = View.VISIBLE
            tracksVerticalGridView.layoutManager?.scrollToPosition(currentSelectedPosition)
            tracksVerticalGridView.layoutManager?.findViewByPosition(currentSelectedPosition)
                ?.requestFocus()
            tracksVerticalGridView.requestFocus()
        }, 100)
    }

    /**
     * Sets up audio tracks for display and interaction.
     *
     * @return `true` if the setup is successful and audio tracks are available,
     *         `false` if there are no available audio tracks or only a single track is available.
     */
    private fun setupAudioTracks(): Boolean {
        audioTracks = dataProvider.getAvailableAudioTracks()
        tracksVerticalGridView.adapter =
            audioTracksCheckListAdapter // used to specify that adapter will be used for audio tracks.

        if (audioTracks.isNullOrEmpty()) {
            listener.showToast(ConfigStringsManager.getStringById("no_available_audio_tracks_msg"))
            return false //tracks failed with loading
        }
        else if(audioTracks!!.size == 1){ // no need to show audio tracks when only when track is available.
            listener.showToast(ConfigStringsManager.getStringById("single_audio_track_is_available"))
            return false //only one track - do not show list of audio tracks
        }

        return true //tracks are loaded successfully
    }

    /**
     * Show available audio track list
     * Active audio track is checked
     */
    fun showAudio() {
        val areAudioTracksSuccessfullyLoaded  = setupAudioTracks()
        if (areAudioTracksSuccessfullyLoaded.not()) return

        //----------------------------------------------------------------------------------------\\
        //----------------- Execute code only if there are available AUDIO tracks. ---------------\\
        //----------------------------------------------------------------------------------------\\
        visibility = View.VISIBLE

        currentSelectedAudioTrack = dataProvider.getCurrentAudioTrack()

        var currentSelectedPosition = 0
        var similarNameCountMap = mutableMapOf<String, Pair<Int, Int>>()

        audioTracks!!.forEachIndexed { index, track ->
            var name = track.trackName
            if (similarNameCountMap[name] == null) {
                similarNameCountMap[name] = Pair(1, 0)
            } else {
                var pair = similarNameCountMap[name]
                similarNameCountMap[name] = Pair(pair!!.first + 1, 0)
            }
        }

        //check current track
        val audioCheckListItems = mutableListOf<CheckListItem>()

        audioTracks!!.forEachIndexed { index, track ->
            val infoIcons = mutableListOf<Int>()

            if (track.isAd) {
                infoIcons.add(R.drawable.ic_ad)
            }
            if (track.isDolby) {
                infoIcons.add(R.drawable.ic_dolby)
            }
            var key = track.trackName
            var trackName = track.trackName
            var countIndexPair = similarNameCountMap[key]!!
            //if single entry of same name then no need to add index
            if (countIndexPair.first == 1 && countIndexPair.second == 0) {
                trackName = track.trackName
            } else {
                if (countIndexPair.first >= 1) {
                    similarNameCountMap[key] = Pair(countIndexPair.first - 1, countIndexPair.second + 1)
                    trackName += " ${countIndexPair.second + 1}"
                }
            }
            if (track == currentSelectedAudioTrack) {
                audioCheckListItems.add(CheckListItem(trackName, true, infoIcons))
                currentSelectedPosition = index
            } else {
                audioCheckListItems.add(CheckListItem(trackName, false, infoIcons))
            }
        }

        tracksTitle!!.text = ConfigStringsManager.getStringById("audio")
        audioTracksCheckListAdapter.refresh(audioCheckListItems)

        tracksWrapperLinearLayout!!.postDelayed({
            tracksWrapperLinearLayout!!.visibility = View.VISIBLE
            tracksVerticalGridView.layoutManager!!.scrollToPosition(currentSelectedPosition)
            tracksVerticalGridView.requestFocus()

        }, 100)
    }

    interface Listener:  TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
    }

}