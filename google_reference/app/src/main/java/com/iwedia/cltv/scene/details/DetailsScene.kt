package com.iwedia.cltv.scene.details

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvTrackInfo
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.components.*
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.CustomButton
import com.iwedia.cltv.components.HorizontalButtonsAdapter
import com.iwedia.cltv.components.custom_card.CustomCard
import com.iwedia.cltv.config.*
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PlatformType
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import java.util.*

/**
 * DetailsScene
 *
 * @author Aleksandar Milojevic
 */
class DetailsScene(context: Context, sceneListener: DetailsSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.DETAILS_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.DETAILS_SCENE),
    sceneListener
) {
    val TAG = javaClass.simpleName
    var buttonsHorizontalGridView: HorizontalGridView? = null
    var detailsButtonAdapter: HorizontalButtonsAdapter? = null

    var subtitleAudioContainer: ConstraintLayout? = null

    var favoriteButtonPosition: Int? = null
    var selectedButton: CustomButton? = null

    /**
    tracksGridView is VerticalGridView used for displaying Audio or Subtitle tracks.
     */
    private var tracksVerticalGridView: VerticalGridView? = null

    /**
     * sideViewWrapper is important for handling visibility of the tracksVerticalGridView (used for displaying and hiding Audio or Subtitle tracks).
     */
    var tracksWrapperLinearLayout: LinearLayout? = null

    /**
     * tracksCheckListAdapter is adapter used
     */
    private lateinit var audioTracksCheckListAdapter: CheckListAdapter
    private lateinit var subtitleTracksCheckListAdapter: CheckListAdapter

    /**
     * tracksTitle is title of the tracksGridView used to distinguish whether GridView contains Audio or Subtitle tracks
     */
    private var tracksTitle: TextView? = null

    lateinit var fadingEdgeLayout: FadingEdgeLayout

    var mData: Any? = null

    var buttonsList = mutableListOf<ButtonType>()

    private var favoritesOverlay: ConstraintLayout? = null
    private var favoritesGridView: VerticalGridView? = null
    private lateinit var favoritesListAdapter: MultiCheckListAdapter

    var audioTracks = mutableListOf<IAudioTrack>()
    var subtitleTracks = mutableListOf<ISubtitle>()

    private var currentSelectedAudioTrack: IAudioTrack? = null
    private var currentSelectedSubtitleTrack: ISubtitle? = null

    var selectedFavListItems = ArrayList<String>()
    var isCurrentEventOnCurrentChannel = false
    var moreInfoCustomCard: CustomCard.CustomCardDetailsScene? = null

    var sideViewOpen: Int = 0

    var customDetails: CustomDetails.CustomDetailsDetailsScene? = null

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(
            name,
            R.layout.layout_details_scene,
            object : GAndroidSceneFragmentListener {
                override fun onCreated() {
                    moreInfoCustomCard = view?.findViewById(R.id.custom_card)

                    val bg_cl: ConstraintLayout = view!!.findViewById(R.id.bg_cl)
                    bg_cl.setBackgroundColor(
                        Color.parseColor(
                            ConfigColorManager.getColor("color_background")
                                .replace("#", ConfigColorManager.alfa_light_bg)
                            )
                    )

                    val drawable_zap: View = view!!.findViewById(R.id.drawable_zap)

                    val drawableZap = GradientDrawable()
                    drawableZap.setShape(GradientDrawable.RECTANGLE)
                    drawableZap.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM)
                    val drawableFadingEdgeColorStart = Color.parseColor(
                        ConfigColorManager.getColor("color_background")
                            .replace("#", ConfigColorManager.alfa_zero_per)
                    )
                    val drawableFadingEdgeColorMid = Color.parseColor(
                        ConfigColorManager.getColor("color_background")
                            .replace("#", ConfigColorManager.alfa_fifty_per)
                    )
                    val drawableFadingEdgeColorEnd = Color.parseColor(
                        ConfigColorManager.getColor("color_background")
                            .replace("#", ConfigColorManager.alfa_hundred_per)
                    )
                    drawableZap.setColors(
                        intArrayOf(
                            drawableFadingEdgeColorStart,
                            drawableFadingEdgeColorMid,
                            drawableFadingEdgeColorEnd
                        )
                    )
                    drawable_zap.background = drawableZap
                    buttonsHorizontalGridView =
                        view!!.findViewById<HorizontalGridView>(R.id.buttons_horizontal_grid_view)
                    val separator = view!!.findViewById<View>(R.id.separator)
                    separator.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))


                    subtitleAudioContainer = view!!.findViewById(R.id.details_scene_left_buttons_container)

                    customDetails = view!!.findViewById(R.id.custom_details)

                    tracksWrapperLinearLayout =
                        view!!.findViewById(R.id.audio_and_subtitles_container)

                    favoritesOverlay = view!!.findViewById(R.id.favorites_overlay)
                    var drawable = GradientDrawable()
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
                    drawable.setShape(GradientDrawable.RECTANGLE)
                    drawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT)
                    drawable.setColors(
                        intArrayOf(
                            colorStart,
                            colorMid,
                            colorEnd
                        )
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

                    tracksTitle = view!!.findViewById(R.id.title)
                    tracksTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    tracksTitle!!.typeface = TypeFaceProvider.getTypeFace(
                        context!!,
                        ConfigFontManager.getFont("font_medium")
                    )

                    fadingEdgeLayout = view!!.findViewById(R.id.fading_edge_layout)

                    tracksVerticalGridView = view!!.findViewById(R.id.side_view_vertical_grid_view)

                    audioTracksCheckListAdapter = CheckListAdapter(
                        fadingEdgeLayout = fadingEdgeLayout,
                        FadeAdapter.FadeAdapterType.VERTICAL
                    )

                    audioTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {
                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onItemClicked(position: Int) {
                            //this method is called to restart inactivity timer for no signal power off
                            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                            currentSelectedAudioTrack = audioTracks[position]
                            (sceneListener as DetailsSceneListener).onAudioTrackClicked(currentSelectedAudioTrack!!)
                            if((sceneListener as DetailsSceneListener).getActiveChannel().tunerType == TunerType.ANALOG_TUNER_TYPE) {
                                customDetails?.updateAudioChannelInfo(currentSelectedAudioTrack?.trackName!!)
                            }
                        }

                        override fun onAdditionalItemClicked() {
                            // NOT IMPORTANT for Audio tracks. There is no "Off" button in Audio.
                        }

                        override fun onUpPressed(position: Int): Boolean {
                            return false
                        }

                        override fun onDownPressed(position: Int): Boolean {
                            return false
                        }

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as DetailsSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            (sceneListener as DetailsSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }

                        override fun onBackPressed(): Boolean {
                            buttonsHorizontalGridView!!.requestFocus() // move focus back to the last focused button in horizontalGridView which in this case should be AUDIO button
                            tracksWrapperLinearLayout!!.visibility = View.INVISIBLE
                            sideViewOpen = 0
                            return true
                        }
                    }

                    subtitleTracksCheckListAdapter = CheckListAdapter(
                        fadingEdgeLayout = fadingEdgeLayout,
                        FadeAdapter.FadeAdapterType.VERTICAL
                    )

                    subtitleTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {

                        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                            (sceneListener as DetailsSceneListener).setSpeechText(text = text, importance = importance)
                        }

                        override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                            (sceneListener as DetailsSceneListener).setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
                        }

                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onItemClicked(position: Int) {
                            //this method is called to restart inactivity timer for no signal power off
                            (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                            currentSelectedSubtitleTrack = subtitleTracks!![position]
                            (sceneListener as DetailsSceneListener).onSubtitleTrackClicked(currentSelectedSubtitleTrack!!)
                        }

                        override fun onAdditionalItemClicked() {  // called when user clicks to the "Off" button in CheckListAdapter for Subtitles
                            (sceneListener as DetailsSceneListener).setSubtitles(false)
                        }

                        override fun onUpPressed(position: Int): Boolean {
                            return false
                        }

                        override fun onDownPressed(position: Int): Boolean {
                            return false
                        }

                        override fun onBackPressed(): Boolean {
                            buttonsHorizontalGridView!!.requestFocus() // move focus back to the last focused button in horizontalGridView which in this case should be AUDIO button
                            tracksWrapperLinearLayout!!.visibility = View.INVISIBLE
                            sideViewOpen = 0
                            return true
                        }
                    }

                    detailsButtonAdapter =
                        HorizontalButtonsAdapter(
                            ttsSetterInterface = sceneListener as DetailsSceneListener
                        )

                    detailsButtonAdapter!!.listener =
                        object : HorizontalButtonsAdapter.HorizontalButtonsAdapterListener {
                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun itemClicked(buttonType: ButtonType, callback: IAsyncCallback) {
                                //this method is called to restart inactivity timer for no signal power off
                                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                                val platformName = (sceneListener as DetailsSceneListener).getPlatformName()
                                if(buttonType == ButtonType.CC_OR_SUBTITLE){
                                    // in MTK flavor behavior of SUBTITLE button: when clicked Closed Caption should be changed.
                                    if (platformName == PlatformType.getPlatformName(PlatformType.MTK)) {
                                        onCCButtonPressed(callback)
                                    }
                                    // in BASE flavor behavior of SUBTITLE button: show Subtitle tracks if they exist.
                                    else if (platformName == PlatformType.getPlatformName(PlatformType.BASE)) {
                                        showSubtitles()
                                        callback.onSuccess()
                                    } else if (platformName == PlatformType.getPlatformName(PlatformType.REF_PLUS_5)) {
                                        if ((sceneListener as DetailsSceneListener).getClosedCaptionSubtitlesState()) {
                                            //To show subtitles
                                            showSubtitles()
                                            callback.onSuccess()
                                        } else {
                                            callback.onSuccess()
                                            onCCPressed()
                                        }
                                    } else if (platformName == PlatformType.getPlatformName(PlatformType.RTK)) {
                                        if ((sceneListener as DetailsSceneListener).getClosedCaptionSubtitlesState()) {
                                            //To show subtitles
                                            showSubtitles()
                                            callback.onSuccess()
                                        } else {
                                            callback.onSuccess()
                                            onCCPressed()
                                        }
                                    }
                                    else{
                                        throw Exception("For this flavor: ${platformName} logic for SUBTITLE button is not implemented. Logic for SUBTITLE button has " +
                                                "been implemented only for 'mtk' and 'base' flavor for now.")
                                    }
                                }
                                if (buttonType == ButtonType.WATCHLIST) {
                                    if (mData is TvEvent) {
                                        (sceneListener as DetailsSceneListener).onWatchlistAddClicked(mData as TvEvent, object : IAsyncCallback{
                                            override fun onFailed(error: Error) {
                                                callback.onFailed(error)
                                            }

                                            override fun onSuccess() {
                                                moreInfoCustomCard!!.updateWatchlistIcon((sceneListener as DetailsSceneListener).isInWatchlist(mData as TvEvent))
                                                callback.onSuccess() // this callback is from HorizontalButtonsAdapter and it is used to set animation to finished.
                                            }
                                        })
                                    }
                                }
                                if (buttonType == ButtonType.AUDIO) {
                                    if (audioTracks.isEmpty()) {
                                        (sceneListener as DetailsSceneListener).showToast(ConfigStringsManager.getStringById("no_available_audio_tracks_msg"))
                                        callback.onSuccess() // this callback is from HorizontalButtonsAdapter and it is used to set animation to finished.
                                        return
                                    }
                                    showAudio()
                                    callback.onSuccess() // this callback is from HorizontalButtonsAdapter and it is used to set animation to finished.
                                }
                                if (buttonType == ButtonType.WATCHLIST_REMOVE) {
                                    if (mData is TvEvent) {
                                        (sceneListener as DetailsSceneListener).onWatchlistRemoveClicked(mData as TvEvent, object : IAsyncCallback{
                                            override fun onFailed(error: Error) {
                                                callback.onFailed(error)
                                            }

                                            override fun onSuccess() {
                                                moreInfoCustomCard!!.updateWatchlistIcon((sceneListener as DetailsSceneListener).isInWatchlist(mData as TvEvent))
                                                callback.onSuccess() // this callback is from HorizontalButtonsAdapter and it is used to set animation to finished.
                                            }
                                        })
                                    }
                                }
                                if (buttonType == ButtonType.RECORD || buttonType == ButtonType.CANCEL_RECORDING) {
                                    ((sceneListener as DetailsSceneListener).onRecordButtonClicked(mData as TvEvent , object : IAsyncCallback {
                                        override fun onFailed(error: Error) {
                                            callback.onFailed(error)
                                        }

                                        override fun onSuccess() {
                                            refreshRecordButton(mData as TvEvent)
                                            moreInfoCustomCard!!.updateRecordingIcon((sceneListener as DetailsSceneListener).isInRecList(mData as TvEvent))
                                            callback.onSuccess()
                                        }
                                    }))
                                }
                                (sceneListener as DetailsSceneListener).onButtonClick(
                                    buttonType,
                                    mData!!,
                                    callback
                                )
                            }

                            override fun onKeyUp(position: Int): Boolean {
                                customDetails!!.scrollUp()
                                return true
                            }

                            override fun onKeyDown(position: Int): Boolean {
                                customDetails!!.scrollDown()
                                return true
                            }

                            override fun onKeyRight(position: Int): Boolean {
                                if (position < (detailsButtonAdapter?.items?.size!! - 1)) {
                                    buttonsHorizontalGridView?.layoutManager?.findViewByPosition(
                                        position + 1
                                    )?.requestFocus()
                                }
                                return true
                            }

                            override fun onKeyLeft(position: Int): Boolean {
                                if (position > 0) {
                                    buttonsHorizontalGridView?.layoutManager?.findViewByPosition(
                                        position - 1
                                    )?.requestFocus()
                                }
                                return true
                            }

                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun onKeyBookmark() {
                                //this method is called to restart inactivity timer for no signal power off
                                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                                var button = buttonsList[0]
                                (sceneListener as DetailsSceneListener).onButtonClick(
                                    button,
                                    mData!!, object : IAsyncCallback{
                                        override fun onFailed(error: Error) {

                                        }

                                        override fun onSuccess() {

                                        }

                                    }
                                )
                            }

                            override fun onCCPressed(): Boolean {
                                if((sceneListener as DetailsSceneListener).isClosedCaptionEnabled() == true) {
                                    if (isCurrentEventOnCurrentChannel) {
                                        (sceneListener as DetailsSceneListener).setClosedCaption()
                                        updateCCInfo()
                                    }
                                } else {
                                    (sceneListener as DetailsSceneListener).saveUserSelectedCCOptions("display_cc", 1)
                                    (sceneListener as DetailsSceneListener).setClosedCaption()
                                    updateCCInfo()
                                }
                                return (isCurrentEventOnCurrentChannel && (sceneListener as DetailsSceneListener).isClosedCaptionEnabled() == true)
                            }
                            override fun onBackPressed(): Boolean {
                                return false
                            }

                            override fun onChannelDownPressed(): Boolean {
                                return false
                            }

                            override fun onChannelUpPressed(): Boolean {
                                return false
                            }

                            override fun onFocusChanged(hasFocus: Boolean) {
                            }

                            override fun onDigitPressed(digit: Int) {}

                            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                                (sceneListener as DetailsSceneListener).setSpeechText(text = text, importance = importance)
                            }
                        }
                    buttonsHorizontalGridView!!.adapter = detailsButtonAdapter
                    buttonsHorizontalGridView!!.setItemSpacing(
                        Utils.getDimensInPixelSize(R.dimen.custom_dim_20)
                    )

                    //Init favorites overlay
                    drawable = GradientDrawable()
                    drawable.shape = GradientDrawable.RECTANGLE
                    drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                    drawable.colors = intArrayOf(
                        colorStart,
                        colorMid,
                        colorEnd
                    )
                    favoritesOverlay!!.background = drawable
                    favoritesGridView = view!!.findViewById(R.id.favorites_overlay_grid_view)

                    initFavoritesOverlay()

                    sceneListener.onSceneInitialized()
                }
            })
    }

    fun setTitle(title: String) {
        customDetails?.updateTitle(title)
    }

    /**
     * Favorites overlay initialization
     */
    private fun initFavoritesOverlay() {
        val favoriteContainerTitle = view!!.findViewById<TextView>(R.id.favorites_overlay_title)
        favoriteContainerTitle.text = ConfigStringsManager.getStringById("add_to")
        favoriteContainerTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        favoriteContainerTitle.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        Utils.makeGradient(
            view = view!!.findViewById(R.id.favorites_gradient_view),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
            endColor = Color.TRANSPARENT,
            centerX = 0.8f,
            centerY = 0f
        )
        Utils.makeGradient(
            view = view!!.findViewById(R.id.favorites_linear_layout),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark")),
            endColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
            centerX = 0.8f,
            centerY = 0f
        )

        favoritesListAdapter = MultiCheckListAdapter()
        (sceneListener as DetailsSceneListener).getFavoritesCategories(object: IAsyncDataCallback<ArrayList<String>>{
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: ArrayList<String>) {
                favoritesListAdapter.refresh(data)
            }
        })

        favoritesGridView!!.selectedPosition = 0
        favoritesGridView!!.preserveFocusAfterLayout = true
        favoritesGridView!!.setNumColumns(1)
        favoritesGridView!!.adapter = favoritesListAdapter
        favoritesGridView!!.setItemSpacing(
            Utils.getDimensInPixelSize(R.dimen.custom_dim_5)
        )

        favoritesGridView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == 0) {
                    var position = favoritesListAdapter!!.focusedItem
                    var view = favoritesGridView!!.layoutManager!!.findViewByPosition(
                        position
                    )
                    if (view != null) {
                        view?.requestFocus()
                    }
                }
            }
        })

        favoritesListAdapter.adapterListener =
            object : MultiCheckListAdapter.MultiCheckListAdapterListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onItemClicked(button: String, callback: IAsyncCallback) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    callback.onSuccess()
                }

                override fun onKeyUp(position: Int): Boolean {
                    return false
                }

                override fun onKeyDown(position: Int): Boolean {
                    return false
                }

                override fun onKeyRight(position: Int): Boolean {
                    return true
                }

                override fun onKeyLeft(position: Int): Boolean {
                    return true
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as DetailsSceneListener).setSpeechText(text = text, importance = importance)
                }

                override fun onBackPressed(position: Int): Boolean {
                    hideFavouritesOverlay()
                    return true
                }

            }
    }

    /**
     * Shows favorites overlay
     */
    fun showFavoritesOverlay() {
        favoritesOverlay?.bringToFront()
        favoritesOverlay?.elevation = 10f
        favoritesOverlay?.visibility = View.VISIBLE
        if (mData != null && mData is TvEvent) {
            val event = mData as TvEvent
            var selectedItemsList = (sceneListener as DetailsSceneListener).getFavoriteItemList(event!!.tvChannel)
            (favoritesGridView?.adapter as MultiCheckListAdapter).setSelectedItems(selectedItemsList)
        }
        favoritesGridView?.postDelayed({
            val view = favoritesGridView?.layoutManager?.findViewByPosition(0)
            if (view != null) {
                view.clearFocus()
                view.requestFocus()
            }
        }, 200)
        buttonsList.forEachIndexed{index, type ->
            if (type == ButtonType.ADD_TO_FAVORITES || type == ButtonType.EDIT_FAVORITES) {
                favoriteButtonPosition = index
            }
        }
    }

    /**
     * Hides favorites overlay
     */
    private fun hideFavouritesOverlay(){
        selectedFavListItems.clear()
        selectedFavListItems.addAll(favoritesListAdapter.getSelectedItems())
        if (mData != null && mData is TvEvent) {
            val event = mData as TvEvent
            (sceneListener as DetailsSceneListener).onFavoriteButtonPressed(
                event.tvChannel,
                selectedFavListItems
            )
        }
        favoritesGridView?.scrollToPosition(0)
        favoritesOverlay?.visibility = View.GONE
        buttonsHorizontalGridView?.post {
            buttonsHorizontalGridView?.layoutManager?.findViewByPosition(favoriteButtonPosition!!)?.requestFocus()
        }
    }

    private fun showSubtitles() {
        tracksVerticalGridView!!.adapter = subtitleTracksCheckListAdapter // used to specify that adapter will be used for subtitle tracks.
        if (subtitleTracks.isEmpty()) {
            (sceneListener as DetailsSceneListener).showToast(ConfigStringsManager.getStringById("no_available_subtitle_tracks_msg"))
            (sceneListener as DetailsSceneListener).onBackPressed()
            return
        }
        //Setup subtitle side view
        sideViewOpen = 1

        currentSelectedSubtitleTrack = (sceneListener as DetailsSceneListener).getCurrentSubtitleTrack()

        var currentSelectedPosition = 0

        var subtitleCheckListItems = mutableListOf<CheckListItem>()

        var undefinedTracks = 0

        subtitleTracks.forEachIndexed {index, track ->
            val name = track.trackName
            val infoIcons = mutableListOf<Int>()

            if (track.trackName.lowercase().contains("undefined"))  {
                undefinedTracks++
                name.plus(" $undefinedTracks")
            }

            if (track.isHoh){
                infoIcons.add(R.drawable.ic_hoh)
            }

            val isSubtitleEnabled = (sceneListener as DetailsSceneListener).isSubtitlesEnabled()
            if (track == currentSelectedSubtitleTrack && isSubtitleEnabled) {
                subtitleCheckListItems.add(CheckListItem(name, true, infoIcons))
                currentSelectedPosition = index
            }
            else {
                subtitleCheckListItems.add(CheckListItem(name, false, infoIcons))
            }
        }

        tracksTitle!!.text = ConfigStringsManager.getStringById("subtitles")
        subtitleTracksCheckListAdapter.refreshWithAdditionalItem(
            adapterItems = subtitleCheckListItems,
            name = ConfigStringsManager.getStringById("off"),
            isChecked = !(sceneListener as DetailsSceneListener).isSubtitlesEnabled()
        )

        if (!(sceneListener as DetailsSceneListener).isSubtitlesEnabled()) {
            currentSelectedPosition = subtitleTracks!!.size
        }

        tracksWrapperLinearLayout?.postDelayed(Runnable {
            tracksWrapperLinearLayout!!.visibility = View.VISIBLE
            tracksVerticalGridView?.layoutManager?.scrollToPosition(currentSelectedPosition)
            tracksVerticalGridView?.layoutManager?.findViewByPosition(currentSelectedPosition)?.requestFocus()
            tracksVerticalGridView?.requestFocus()
        }, 100)
    }


    private fun showAudio() {
        tracksVerticalGridView!!.adapter = audioTracksCheckListAdapter // used to specify that adapter will be used for audio tracks.
        if (audioTracks == null || audioTracks.size == 0) {
            (sceneListener as DetailsSceneListener).showToast(ConfigStringsManager.getStringById("no_available_audio_tracks_msg"))
            (sceneListener as DetailsSceneListener).onBackPressed()
            return
        }
        sideViewOpen = 2
        //Setup audio side view

        currentSelectedAudioTrack = (sceneListener as DetailsSceneListener).getCurrentAudioTrack()

        var currentSelectedPosition = 0

        //check current track
        var audioCheckListItems = mutableListOf<CheckListItem>()

        var undefinedTracks = 0

        audioTracks.forEachIndexed {index, track ->
            Log.e("DHANYA","details scene as well? ${track.trackName}")
            val name = track.trackName

            val infoIcons = mutableListOf<Int>()

            if (track.isAd){
                infoIcons.add(R.drawable.ic_ad)
            }
            if (track.isDolby){
                infoIcons.add(R.drawable.ic_dolby)
            }

            if (track.isHohAudio) {
                infoIcons.add(R.drawable.ic_hoh)
            }

            if (track.trackName.lowercase().contains("undefined"))  {
                undefinedTracks++
                name.plus(" $undefinedTracks")
            }

            if (track == currentSelectedAudioTrack) {
                audioCheckListItems.add(CheckListItem(name, true, infoIcons))
                currentSelectedPosition = index
            }
            else {
                audioCheckListItems.add(CheckListItem(name, false, infoIcons))
            }
        }

        tracksTitle!!.text = ConfigStringsManager.getStringById("audio")
        audioTracksCheckListAdapter.refresh(audioCheckListItems)

        tracksWrapperLinearLayout!!.postDelayed(Runnable {
            tracksWrapperLinearLayout!!.visibility = View.VISIBLE
            tracksVerticalGridView!!.layoutManager!!.scrollToPosition(currentSelectedPosition)
            tracksVerticalGridView!!.requestFocus()

        }, 100)

    }


    override fun parseConfig(sceneConfig: SceneConfig?) {
        TODO("Not yet implemented")
    }
    fun refreshWatchlistButton(tvEvent: TvEvent) {
        var position = -1
        for (index in 0 until buttonsList.size) {
            var button = buttonsList[index]
            val isInWatchlist = (sceneListener as DetailsSceneListener).isInWatchlist(tvEvent)
            if (button == ButtonType.WATCHLIST || button == ButtonType.WATCHLIST_REMOVE) {
                if (isInWatchlist) {
                    buttonsList.set(index, ButtonType.WATCHLIST_REMOVE)
                } else {
                    buttonsList.set(index, ButtonType.WATCHLIST)
                }
                position = index
                break
            }
        }
        if (position != -1) {
            detailsButtonAdapter!!.updateButton(position, buttonsList.get(position))

        }
    }

    fun refreshFavoriteButton() {
        if (mData != null && (mData is TvEvent || mData is Recording)) {
            val tvEvent = if (mData is TvEvent) mData as TvEvent else (mData as Recording).tvEvent
            var favListItems = (sceneListener as DetailsSceneListener).getFavoriteItemList(tvEvent!!.tvChannel)

            var position = -1
            for (index in 0 until buttonsList.size) {
                var button = buttonsList[index]
                if (button == ButtonType.ADD_TO_FAVORITES || button == ButtonType.EDIT_FAVORITES) {
                    if (favListItems.isEmpty()){
                        buttonsList.set(index, ButtonType.ADD_TO_FAVORITES)
                    }
                    else{
                        buttonsList.set(index, ButtonType.EDIT_FAVORITES)
                    }
                    position = index
                    break
                }
            }
            if (position != -1) {
                detailsButtonAdapter!!.updateButton(position, buttonsList.get(position))
            }
        }
    }

    fun refreshRecordButton(tvEvent: TvEvent) {
        if ((sceneListener as DetailsSceneListener).isCurrentEvent(tvEvent)) {
            if ((sceneListener as DetailsSceneListener).getConfigInfo("pvr")) {
                val isRecordingInProcess =
                    (sceneListener as DetailsSceneListener).isRecordingInProgress()
                var position = -1
                for (index in 0 until buttonsList.size) {
                    val button = buttonsList[index]
                    if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                        if (isRecordingInProcess) {
                            if (TvChannel.compare(
                                    (sceneListener as DetailsSceneListener).getRecordingInProgressTvChannel(),
                                    tvEvent.tvChannel
                                )
                            ) {
                                buttonsList[index] = ButtonType.CANCEL_RECORDING
                            } else {
                                buttonsList[index] = ButtonType.RECORD
                            }
                        }
                        else {
                            buttonsList[index] = ButtonType.RECORD
                        }
                        position = index
                        break
                    }
                }
                if (position != -1) {
                    ReferenceApplication.runOnUiThread {
                        detailsButtonAdapter!!.updateButton(position, buttonsList[position])
                    }
                }
            }

        }
        else {
            //Future recording
            val isInRecList = (sceneListener as DetailsSceneListener).isInRecList(tvEvent)
            var position = -1
            for (index in 0 until buttonsList.size) {
                val button = buttonsList[index]
                if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                    if (isInRecList) {
                        buttonsList[index] = ButtonType.CANCEL_RECORDING
                    } else {
                        buttonsList[index] = ButtonType.RECORD
                    }
                    position = index
                    break
                }
            }
            if (position != -1) {
                ReferenceApplication.runOnUiThread {
                    detailsButtonAdapter!!.updateButton(position, buttonsList[position])
                }
            }
        }
    }

    private fun refreshActiveAudioTrack() {
        if (tracksWrapperLinearLayout?.visibility == View.VISIBLE && sideViewOpen == 2) {
            buttonsHorizontalGridView!!.layoutManager!!.getChildAt(buttonsList.indexOf(ButtonType.AUDIO))!!.callOnClick()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any?) {
        if (data is String) {
            customDetails!!.updateTitle(data)
        }

        if (data is TvEvent) {
            mData = data

            if ((sceneListener as DetailsSceneListener).isCurrentEvent(data)) { // only if event is in present time check whether it is on currently active channel
                isCurrentEventOnCurrentChannel =
                    TvChannel.compare(
                        (sceneListener as DetailsSceneListener).getActiveChannel(),
                        (mData as TvEvent).tvChannel
                    )
            }else{
                isCurrentEventOnCurrentChannel = false
            }

            customDetails!!.updateData(
                tvEvent = mData as TvEvent,
                isCurrentChannel = isCurrentEventOnCurrentChannel,
                parentalRatingDisplayName = (sceneListener as DetailsSceneListener).getParentalRatingDisplayName(data.parentalRating, data),
                currentTime = (sceneListener as DetailsSceneListener).getCurrentTime((mData as TvEvent).tvChannel),
                isCCTrackAvailable = (sceneListener as DetailsSceneListener).isCCTrackAvailable(),
                dateTimeFormat = (sceneListener as DetailsSceneListener).getDateTimeFormat()
            )

            customDetails!!.updateTtxImageView((sceneListener as DetailsSceneListener).getTTX(TvTrackInfo.TYPE_SUBTITLE))
            customDetails!!.updateHohImageView((sceneListener as DetailsSceneListener).isHOH(TvTrackInfo.TYPE_SUBTITLE) || (sceneListener as DetailsSceneListener).isHOH(TvTrackInfo.TYPE_AUDIO))


            moreInfoCustomCard!!.updateData(
                tvEvent = mData as TvEvent,
                isCurrentChannel = isCurrentEventOnCurrentChannel,
                isParentalEnabled = (sceneListener as DetailsSceneListener).isParentalEnabled(),
                isInWatchlist = (sceneListener as DetailsSceneListener).isInWatchlist(mData as TvEvent),
                isInRecList = (sceneListener as DetailsSceneListener).isInRecList(mData as TvEvent),
                onImageLoadingSucceed = {
                    customDetails!!.setMarginsOnInfoRow(true)
                },
                onImageLoadingFailed = {
                    customDetails!!.setMarginsOnInfoRow(false)
                },
                currentTime = (sceneListener as DetailsSceneListener).getCurrentTime((mData as TvEvent).tvChannel)
            )

            val favListItems = (sceneListener as DetailsSceneListener).getFavoriteItemList(data.tvChannel)
            favoritesListAdapter.setSelectedItems(favListItems)

            val currentTime = Date((sceneListener as DetailsSceneListener).getCurrentTime((mData as TvEvent).tvChannel))
            val eventStartTime = Date((mData as TvEvent).startTime)
            val eventEndTime = Date((mData as TvEvent).endTime)

            val isPastEvent = eventStartTime.before(currentTime) && eventEndTime.before(currentTime)
            val isCurrentEvent =
                eventStartTime.before(currentTime) && eventEndTime.after(currentTime)
            val isFutureEvent = eventStartTime.after(currentTime) && eventEndTime.after(currentTime)

            buttonsList.clear()

            when {
                isPastEvent -> {
                    if (ReferenceApplication.IS_CATCH_UP_SUPPORTED) {
                        buttonsList.add(ButtonType.WATCH)
                    }
                    if (favListItems.isEmpty()){
                        buttonsList.add(ButtonType.ADD_TO_FAVORITES)
                    }
                    else{
                        buttonsList.add(ButtonType.EDIT_FAVORITES)
                    }

                    if((sceneListener as DetailsSceneListener).isGtvMode() && !isNoInformationEvent()){
                        buttonsList.add(ButtonType.SEARCH_WITH_TEXT)
                    }


                }
                isCurrentEvent -> {
                    buttonsList.add(ButtonType.WATCH)

                    if ((sceneListener as DetailsSceneListener).getConfigInfo("pvr")) {
                        if (data.tvChannel.tunerType != TunerType.ANALOG_TUNER_TYPE) {
                            val isRecordingInProcess =
                                (sceneListener as DetailsSceneListener).isRecordingInProgress()
                            if (isRecordingInProcess) {
                                if (TvChannel.compare(
                                        (sceneListener as DetailsSceneListener).getRecordingInProgressTvChannel(),
                                        (mData as TvEvent).tvChannel
                                    )
                                ) {
                                    buttonsList.add(ButtonType.CANCEL_RECORDING)
                                } else {
                                    buttonsList.add(ButtonType.RECORD)
                                }
                            } else {
                                buttonsList.add(ButtonType.RECORD)
                            }

                            Log.d(
                                Constants.LogTag.CLTV_TAG +
                                "DetailsScene",
                                "rightButton visible for current event"
                            )
                        }
                    }

                    if (favListItems.isEmpty()) {
                        buttonsList.add(ButtonType.ADD_TO_FAVORITES)
                    }
                    else{
                        buttonsList.add(ButtonType.EDIT_FAVORITES)
                    }

                    if((sceneListener as DetailsSceneListener).isGtvMode() && !isNoInformationEvent()) {
                        buttonsList.add(ButtonType.SEARCH_WITH_TEXT)
                    }

                    customDetails!!.updateVideoQuality(
                        tvChannel = data.tvChannel,
                        videoResolution = (sceneListener as DetailsSceneListener).getVideoResolution()
                    )

                    customDetails!!.updateDolbyImageView(
                        (sceneListener as DetailsSceneListener).getIsDolby(TvTrackInfo.TYPE_AUDIO)
                    )

                    refreshAudioChannel()
                    if((sceneListener as DetailsSceneListener).getActiveChannel().tunerType == TunerType.ANALOG_TUNER_TYPE) {
                        if ((sceneListener as DetailsSceneListener).getCurrentAudioTrack()?.trackName != null) {
                            customDetails!!.updateAudioChannelInfo(
                                (sceneListener as DetailsSceneListener).getCurrentAudioTrack()?.trackName!!
                            )
                        }
                    }

                    customDetails!!.updateAdImageView(
                        (sceneListener as DetailsSceneListener).getIsAudioDescription(TvTrackInfo.TYPE_AUDIO)
                    )

                    val isTeletext = (sceneListener as DetailsSceneListener).isTeleText(TvTrackInfo.TYPE_SUBTITLE)
                    val isTeletextEnabled = (sceneListener as DetailsSceneListener).getConfigInfo("teletext_enable_column")
                    customDetails?.updateTtxImageView(isTeletext && isTeletextEnabled)

                    if (isCurrentEventOnCurrentChannel) {

                        buttonsList.add(ButtonType.AUDIO)

                        if (!(mData as TvEvent).tvChannel.isRadioChannel) {
                            buttonsList.add(ButtonType.CC_OR_SUBTITLE)
                        }

                        audioTracks = (sceneListener as DetailsSceneListener).getAvailableAudioTracks() // leave this inside if clause to avoid calling getAvailableAudioTracks() for events that are not current
                        customDetails!!.updateAudioTracks(
                            audioTracks
                        )

                        subtitleTracks =
                            (sceneListener as DetailsSceneListener).getAvailableSubtitleTracks()
                        customDetails!!.updateSubtitleTracks(subtitleTracks)

                        updateCCInfo()
                    }
                }
                isFutureEvent -> {
                    if ((sceneListener as DetailsSceneListener).isInWatchlist((mData as TvEvent))){
                        buttonsList.add(ButtonType.WATCHLIST_REMOVE)
                    }
                    else {
                        if((mData as TvEvent).isSchedulable) {
                            buttonsList.add(ButtonType.WATCHLIST)
                        }
                    }
                    if ((sceneListener as DetailsSceneListener).getConfigInfo("pvr")) {
                        if (data.tvChannel.tunerType != TunerType.ANALOG_TUNER_TYPE) {
                            val isInRecList =
                                (sceneListener as DetailsSceneListener).isInRecList((mData as TvEvent))
                            if (isInRecList) {
                                buttonsList.add(ButtonType.CANCEL_RECORDING)
                            } else {
                                if((mData as TvEvent).isSchedulable) {
                                    buttonsList.add(ButtonType.RECORD)
                                }
                            }
                        }
                    }

                    if (favListItems.isEmpty()){
                        buttonsList.add(ButtonType.ADD_TO_FAVORITES)
                    }
                    else{
                        buttonsList.add(ButtonType.EDIT_FAVORITES)
                    }

                    if((sceneListener as DetailsSceneListener).isGtvMode() && !isNoInformationEvent()) {
                        buttonsList.add(ButtonType.SEARCH_WITH_TEXT)
                    }

                }
                else -> {
                    buttonsList = mutableListOf()
                }
            }

            if (sideViewOpen != 2) buttonsHorizontalGridView!!.requestFocus()

            detailsButtonAdapter!!.refresh(buttonsList)
        }

        else if (data is Recording) {

            mData = data
            val watchedPosition =
                (sceneListener as DetailsSceneListener).getRecordingPlaybackPosition(data.id)
            val playbackPosition =
                data.recordingDate + watchedPosition

            customDetails!!.updateData(
                tvEvent = data.tvEvent, isCurrentChannel = isCurrentEventOnCurrentChannel,
                parentalRatingDisplayName = (sceneListener as DetailsSceneListener).getParentalRatingDisplayName(data.tvEvent?.parentalRating, data.tvEvent!!),
                currentTime = (sceneListener as DetailsSceneListener).getCurrentTime(data.tvEvent?.tvChannel!!),
                isCCTrackAvailable = (sceneListener as DetailsSceneListener).isCCTrackAvailable(),
                dateTimeFormat = (sceneListener as DetailsSceneListener).getDateTimeFormat()
            )

            val resolution = (sceneListener as DetailsSceneListener).getVideoResolutionForRecoding(data)
            customDetails!!.updateVideoQuality(data.tvEvent!!.tvChannel, resolution,true)
            customDetails!!.setMarginsOnInfoRow(false)
            moreInfoCustomCard!!.updateData(data)

            val favListItems = (sceneListener as DetailsSceneListener).getFavoriteItemList(data.tvChannel!!)
            favoritesListAdapter.setSelectedItems(favListItems)
            refreshFavoriteButton()
            val buttonsList = mutableListOf<ButtonType>()

            val isWatched = (sceneListener as DetailsSceneListener).getPlaybackPositionPercent(data) > 10
            if (isWatched) {
                buttonsList.add(ButtonType.CONTINUE_WATCH)
                buttonsList.add(ButtonType.START_OVER)
            }
            else {
                buttonsList.add(ButtonType.WATCH)
            }

            buttonsList.add(ButtonType.RENAME)
            buttonsList.add(ButtonType.DELETE)

            detailsButtonAdapter!!.refresh(buttonsList)

            buttonsHorizontalGridView!!.requestFocus()
        }

        if (Utils.isListDataType(data, IAudioTrack::class.java)) {
            audioTracks = data as MutableList<IAudioTrack>
        }

        if (Utils.isListDataType(data, ISubtitle::class.java)) {
            subtitleTracks = data as MutableList<ISubtitle>
        }

        if (data is IAudioTrack) {
            if (audioTracks.size > 0) {
                run exitForEach@{
                    audioTracks.forEachIndexed { index, item ->
                        if (item.trackId == data.trackId) {
                            currentSelectedAudioTrack = data
                            return@exitForEach
                        }
                    }
                }
            }
        }

        //TODO DEJAN
        if (data is ScheduledRecording) {
            mData = data
            val event = data.tvEvent!!

            buttonsList.clear()

            buttonsList.add(ButtonType.CANCEL_RECORDING)


            if (event.tvChannel.favListIds.isEmpty()){
                buttonsList.add(ButtonType.ADD_TO_FAVORITES)
            }
            else{
                buttonsList.add(ButtonType.EDIT_FAVORITES)
            }

            if((sceneListener as DetailsSceneListener).isGtvMode() && !isNoInformationEvent()) {
                buttonsList.add(ButtonType.SEARCH_WITH_TEXT)
            }


            buttonsHorizontalGridView!!.requestFocus()

            detailsButtonAdapter!!.refresh(buttonsList)

            customDetails!!.updateData(
                scheduledRecording = data,
                (sceneListener as DetailsSceneListener).getCurrentTime(data.tvChannel!!),
                (sceneListener as DetailsSceneListener).getDateTimeFormat()
            )
            moreInfoCustomCard!!.updateData(
                tvEvent = data.tvEvent!!,
                isCurrentChannel = false,
                isParentalEnabled = false,
                isInWatchlist = (sceneListener as DetailsSceneListener).isInWatchlist(data.tvEvent!!),
                isInRecList = (sceneListener as DetailsSceneListener).isInRecList(data.tvEvent!!),
                currentTime = (sceneListener as DetailsSceneListener).getCurrentTime(data.tvChannel!!),
                onImageLoadingSucceed = {
                    customDetails!!.setMarginsOnInfoRow(true)
                },
                onImageLoadingFailed = {
                    customDetails!!.setMarginsOnInfoRow(false)
                }
                )
        }

        //TODO DEJAN
        if (data is ISubtitle) {
            if (subtitleTracks.size > 0) {
                run exitForEach@{
                    subtitleTracks.forEachIndexed { index, item ->
                        if (item.trackId == data.trackId) {
                            currentSelectedSubtitleTrack = data
                            return@exitForEach
                        }
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        android.os.Handler().post {
            buttonsHorizontalGridView!!.requestFocus()
        }
        if (sideViewOpen == 2) refreshActiveAudioTrack()
    }

    override fun onPause() {
        if(favoritesOverlay!!.isVisible) hideFavouritesOverlay()
        super.onPause()
    }

    private fun onCCButtonPressed(callback: IAsyncCallback) {
        (sceneListener as DetailsSceneListener).setClosedCaption()
        if (isCurrentEventOnCurrentChannel) {
            updateCCInfo()
        } else {
            val curCaptionInfo =
                (sceneListener as DetailsSceneListener).getClosedCaption()

            val toastMessage =
                if (curCaptionInfo.isNullOrBlank())
                    ConfigStringsManager.getStringById("cc_track_turned_off")
                else ConfigStringsManager.getStringById("cc_changed")
                    .plus(" ")
                    .plus(curCaptionInfo)

            (sceneListener as DetailsSceneListener).showToast(toastMessage)
        }
        callback.onSuccess() // this callback is from HorizontalButtonsAdapter and it is used to set animation to finished.
    }

    private fun updateCCInfo() {
        if ((sceneListener as DetailsSceneListener).isClosedCaptionEnabled() != true) return
        val ccText = (sceneListener as DetailsSceneListener).getClosedCaption()
        val isCCTrackAvailable = (sceneListener as DetailsSceneListener).isCCTrackAvailable()
        customDetails!!.updateCcInfo(ccText, isCCTrackAvailable)
        InformationBus.informationBusEventListener?.submitEvent(Events.CLOSED_CAPTION_CHANGED)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyEvent as KeyEvent).action == KeyEvent.ACTION_DOWN) {
            if ((keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) ||
                (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9)) {
                tracksWrapperLinearLayout!!.visibility = View.GONE
                favoritesOverlay!!.visibility = View.GONE
            }
            if (keyCode == KeyEvent.KEYCODE_BACK){
                ReferenceApplication.downActionBackKeyDone = true
            }
        }

       if (keyCode == KeyEvent.KEYCODE_INFO) {
           return true
       }
        val dispatchResult= super.dispatchKeyEvent(keyCode, keyEvent)
        if ((keyEvent).action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK){
                ReferenceApplication.downActionBackKeyDone = false
            }
        }
        return dispatchResult
    }

    fun refreshAudioChannel() {
        customDetails?.updateAudioChannelInfo(
            (sceneListener as DetailsSceneListener).getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO)
        )
        customDetails?.updateAudioChannelFormatInfo(
            (sceneListener as DetailsSceneListener).getAudioFormatInfo()
        )

        if((sceneListener as DetailsSceneListener).getActiveChannel().tunerType == TunerType.ANALOG_TUNER_TYPE) {
            // getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO) does not always return the correct track name for fluki so we need to use direct access
            if ((sceneListener as DetailsSceneListener).getCurrentAudioTrack()?.trackName != null) {
                customDetails!!.updateAudioChannelInfo(
                    (sceneListener as DetailsSceneListener).getCurrentAudioTrack()?.trackName!!
                )
            }
        }
    }
    private fun isNoInformationEvent(): Boolean {
        return (mData as TvEvent).name == ConfigStringsManager.getStringById("no_information") ||
                (mData as TvEvent).name.startsWith(ConfigStringsManager.getStringById("tune_to"))
    }
}