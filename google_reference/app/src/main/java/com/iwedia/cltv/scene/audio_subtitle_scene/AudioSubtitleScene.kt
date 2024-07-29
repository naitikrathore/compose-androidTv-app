package com.iwedia.cltv.scene.audio_subtitle_scene

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.leanback.widget.VerticalGridView
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.components.CheckListAdapter
import com.iwedia.cltv.components.CheckListItem
import com.iwedia.cltv.components.FadeAdapter
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener

/**
 * A reference scene class to display Audio and Subtitle list
 * Launched on Press of Captions button from RC
 * Used for selection of Audio and Subtitles
 */
class AudioSubtitleScene(context: Context, audioSubtitleListener: AudioSubtitleListener) :
    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.AUDIO_SUBTITLE_SCENE,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.AUDIO_SUBTITLE_SCENE),
        audioSubtitleListener
    ) {
    val TAG = AudioSubtitleScene::class.simpleName as String

    private var subtitleTracks = mutableListOf<ISubtitle>()
    private var currentSelectedSubtitleTrack: ISubtitle? = null

    private var tracksTitle: TextView? = null
    private var tracksVerticalGridView: VerticalGridView? = null
    private var tracksWrapperLinearLayout: LinearLayout? = null
    private lateinit var subtitleTracksCheckListAdapter: CheckListAdapter
    private lateinit var fadingEdgeLayout: FadingEdgeLayout

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.audio_subtitle_scene_layout,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    setup()
                }
            })
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {
    }

    override fun refresh(data: Any?) {
        Log.i("$TAG", "refresh")
        super.refresh(data)

        if (data is AudioSubtitleList) {
            subtitleTracks = data.subtitleTrack
            Log.i("$TAG", "refresh subtitleTracks.size = ${subtitleTracks.size}")
            setupSubtitleListGrid()
        }

        if (data is ISubtitle) currentSelectedSubtitleTrack = data
    }

    private fun setup() {
        tracksTitle = view!!.findViewById(R.id.title)
        tracksTitle!!.typeface = TypeFaceProvider.getTypeFace(
            context!!,
            ConfigFontManager.getFont("font_medium")
        )
        tracksTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        fadingEdgeLayout = view!!.findViewById(R.id.fading_edge_layout)
        tracksVerticalGridView = view!!.findViewById(R.id.side_view_vertical_grid_view)
        tracksWrapperLinearLayout = view!!.findViewById(R.id.audio_and_subtitles_container)

        val colorStart = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_zero_per)
        )
        val colorMid = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_fifty_per)
        )
        val colorEnd = Color.parseColor(
            ConfigColorManager.getColor("color_background")
                .replace("#", ConfigColorManager.alfa_hundred_per)
        )

        val drawable1 = GradientDrawable()
        drawable1.shape = GradientDrawable.RECTANGLE
        drawable1.orientation = GradientDrawable.Orientation.LEFT_RIGHT
        drawable1.colors = intArrayOf(
            colorStart,
            colorMid,
            colorEnd
        )
        Utils.makeGradient(
            view = view!!.findViewById(R.id.audio_and_subtitles_gradient_view),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
            endColor = Color.TRANSPARENT,
            centerX = 0.8f,
            centerY = 0f
        )
        Utils.makeGradient(
            view = view!!.findViewById(R.id.audio_and_subtitles_linear_layout),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark")),
            endColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
            centerX = 0.8f,
            centerY = 0f
        )

        subtitleTracksCheckListAdapter = CheckListAdapter(
            fadingEdgeLayout = fadingEdgeLayout,
            FadeAdapter.FadeAdapterType.VERTICAL
        )

        subtitleTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {
            override fun onItemClicked(position: Int) {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                currentSelectedSubtitleTrack = subtitleTracks[position]
                (sceneListener as AudioSubtitleListener).onSubtitleTrackClicked(currentSelectedSubtitleTrack!!)
            }

            override fun onAdditionalItemClicked() {  // called when user clicks to the "Off" button in CheckListAdapter for Subtitles
                (sceneListener as AudioSubtitleListener).setSubtitles(false)
            }

            override fun onDownPressed(position: Int): Boolean {
                return false
            }

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                (sceneListener as AudioSubtitleListener).setSpeechText(text = text, importance = importance)
            }

            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                (sceneListener as AudioSubtitleListener).setSpeechTextForSelectableView(text = text, importance = importance, type = type, isChecked = isChecked)
            }

            override fun onUpPressed(position: Int): Boolean {
                return false
            }

            override fun onBackPressed(): Boolean {
                (sceneListener as AudioSubtitleListener).onBackPressed()
                return true
            }
        }
        sceneListener.onSceneInitialized()
    }

    private fun setupSubtitleListGrid() {
        if (subtitleTracks.size == 0) {
            (sceneListener as AudioSubtitleListener).showToast(ConfigStringsManager.getStringById("no_available_subtitle_tracks_msg"))

            (sceneListener as AudioSubtitleListener).onBackPressed()
            return
        }

        tracksVerticalGridView!!.setNumColumns(1)
        tracksVerticalGridView!!.adapter = subtitleTracksCheckListAdapter

        var undefinedTracks = 0
        var currentSelectedPosition = 0
        val subtitleCheckListItems = mutableListOf<CheckListItem>()
        currentSelectedSubtitleTrack = (sceneListener as AudioSubtitleListener).getCurrentSubtitleTrack()

        subtitleTracks.forEachIndexed { index, track ->
            var name = ConfigStringsManager.getStringById(track.trackName.lowercase())
            val infoIcons = mutableListOf<Int>()

            if (track.trackName.lowercase().contains("undefined"))  {
                undefinedTracks++
                name.plus(" $undefinedTracks")
            }

            if (track.isHoh){
                infoIcons.add(R.drawable.ic_hoh)
            }
            if (track.isTxtBased){
                infoIcons.add(R.drawable.ic_ttx)
            }

            val isSubtitleEnabled = (sceneListener as AudioSubtitleListener).isSubtitlesEnabled()
            if (track == currentSelectedSubtitleTrack  && isSubtitleEnabled) {
                subtitleCheckListItems.add(CheckListItem(name, true, infoIcons))
                currentSelectedPosition = index
            } else {
                subtitleCheckListItems.add(CheckListItem(name, false, infoIcons))
            }
        }

        tracksTitle!!.text = ConfigStringsManager.getStringById("subtitles")

        subtitleTracksCheckListAdapter.refreshWithAdditionalItem(
            adapterItems = subtitleCheckListItems,
            name = ConfigStringsManager.getStringById("off"),
            isChecked = !(sceneListener as AudioSubtitleListener).isSubtitlesEnabled()
        )

        if (!(sceneListener as AudioSubtitleListener).isSubtitlesEnabled()) {
            currentSelectedPosition = subtitleTracks.size
        }


        tracksWrapperLinearLayout?.postDelayed(kotlinx.coroutines.Runnable {
            tracksWrapperLinearLayout!!.visibility = View.VISIBLE
            tracksVerticalGridView?.layoutManager?.scrollToPosition(currentSelectedPosition)
            tracksVerticalGridView?.layoutManager?.findViewByPosition(currentSelectedPosition)
                ?.requestFocus()
            tracksVerticalGridView?.requestFocus()
        }, 100)
    }

}
