package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvTrackInfo
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.RecyclerView
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.components.custom_card.CustomCard
import com.iwedia.cltv.config.*
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.infoBanner.InfoBannerEventsAdapter
import com.iwedia.cltv.utils.*
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import world.widget.GWidget
import world.widget.GWidgetListener
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*


/**
 * Reference widget info banner
 *
 * @author Aleksandar Lazic
 */

open class ReferenceWidgetInfoBanner :
    GWidget<ConstraintLayout, ReferenceWidgetInfoBanner.InfoBannerWidgetListener> {

    private var isParentalOn: Boolean = false
    private val TAG = ReferenceWidgetInfoBanner::class.java.simpleName
    var context: Context? = null

    //Recycler view
    var recyclerView: HorizontalGridView? = null

    //List scrolling flag
    var isListScrolling = false

    //Adapter
    var eventsAdapter: InfoBannerEventsAdapter? = null

    //Current time
    var currentTime: TimeTextView? = null

    //Channel logo
    var channelLogo: ImageView? = null

    //Button container
    var buttonContainer: HorizontalGridView? = null

    //Channel index
    var channelIndex: TextView? = null

    //Channel type
    var channelType: TextView? = null

    var isCurrentEventOnCurrentChannel = false

    //tv channel
    var tvChannel: TvChannel? = null

    private lateinit var customDetails: CustomDetails.CustomDetailsInfoBanner

    //events
    var events: MutableList<TvEvent> = mutableListOf()

    var selectedEvent: TvEvent? = null
    var selectedEventPositionInAdapter: Int = -1
    lateinit var fadingEdgeLayout: FadingEdgeLayout

    var buttonAdapter: HorizontalButtonsAdapter? = null

    //current event position
    var currentEventPosition: Int? = -1

    var buttonsList = mutableListOf<ButtonType>()

    //channel name when no logo
    var channelName: TextView? = null
    var configParam: SceneConfig? = null
    var isLocked: ImageView? = null
    var isScrambled: ImageView? = null
    var linearlayout: LinearLayout? = null
    var trackCount = 0
    var existingAudioTrackCodes = mutableListOf<String>()
    var existingSubtitleTrackCodes = mutableListOf<String>()
    var audioTracks: MutableList<IAudioTrack>? = null
    var subtitleTracks: MutableList<ISubtitle>? = null

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
    var currentSelectedAudioTrack: IAudioTrack? = null
    var currentSelectedSubtitleTrack: ISubtitle? = null
    var sideViewOpen: Int = 0
    var rootView: ConstraintLayout? = null
    var isRef5Flavour = false

    /**
     * Constructor
     */
    @RequiresApi(Build.VERSION_CODES.R)
    constructor(
        context: Context,
        listener: InfoBannerWidgetListener
    ) : super(
        ReferenceWorldHandler.WidgetId.INFO_BANNER,
        ReferenceWorldHandler.WidgetId.INFO_BANNER,
        listener
    ) {
        this.context = context
        isRef5Flavour = ((ReferenceApplication.worldHandler) as ReferenceWorldHandler).getPlatformName().contains("RefPlus")
        setup()
    }

    //TODO record button functionality is not tested with the new adapter
    //TODO possible recording issues

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun setup() {
        view = LayoutInflater.from(context)
            .inflate(R.layout.layout_widget_infobanner, null) as ConstraintLayout
        //find refs
        customDetails = view!!.findViewById(R.id.custom_details)
        rootView = view!!.findViewById(R.id.gradient)
        //todo gradiet
        val drawable = GradientDrawable()
        drawable.setShape(GradientDrawable.RECTANGLE)
        drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM)
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
        drawable.setColors(
            intArrayOf(
                colorStart,
                colorMid,
                colorEnd,
                colorEnd
            )
        )
        rootView!!.setBackground(drawable)
        currentTime = view!!.findViewById(R.id.current_time)
        currentTime!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        currentTime?.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        recyclerView = view!!.findViewById(R.id.events_recycler)
        buttonContainer = view!!.findViewById(R.id.event_button_container)
        channelLogo = view!!.findViewById(R.id.channel_logo)
        channelName = view!!.findViewById(R.id.channel_name)
        channelIndex = view!!.findViewById(R.id.channel_index)
        channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        channelType = view!!.findViewById(R.id.channel_type)
        channelType!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
        channelType!!.typeface = TypeFaceProvider.getTypeFace(context!!, ConfigFontManager.getFont("font_regular"))

        isLocked = view!!.findViewById(R.id.is_locked)
        isLocked!!.visibility = View.GONE

        isScrambled = view!!.findViewById(R.id.is_scrambled)
        isScrambled!!.visibility = View.GONE

        fadingEdgeLayout = view!!.findViewById(R.id.fading_edge_layout)

        isParentalOn = listener.isParentalOn()

        tracksTitle = view!!.findViewById(R.id.title)
        tracksTitle!!.typeface = TypeFaceProvider.getTypeFace(
            context!!,
            ConfigFontManager.getFont("font_medium")
        )
        tracksTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        tracksVerticalGridView = view!!.findViewById(R.id.side_view_vertical_grid_view)
        tracksWrapperLinearLayout = view!!.findViewById(R.id.audio_and_subtitles_container)

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

        buttonAdapter = HorizontalButtonsAdapter(ttsSetterInterface = listener)

        buttonAdapter!!.listener =
            object : HorizontalButtonsAdapter.HorizontalButtonsAdapterListener {
                override fun itemClicked(buttonType: ButtonType, callback: IAsyncCallback) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                    //this method is called to restart inactivity timer for info banner scene
                    (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    when (buttonType) {
                        ButtonType.RECORD, ButtonType.CANCEL_RECORDING -> {
                            (listener.onRecordButtonClicked(selectedEvent!!, object : IAsyncCallback {
                                    override fun onFailed(error: Error) {
                                        callback.onFailed(error)
                                    }

                                    override fun onSuccess() {
                                        eventsAdapter!!.refreshEventAtPosition(selectedEventPositionInAdapter)
                                        var position = -1
                                        for (index in 0 until buttonsList.size) {
                                            val button = buttonsList[index]
                                            val isInRecList = listener.getIsInReclist(selectedEvent!!)
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
                                            buttonAdapter!!.updateButton(position, buttonsList[position])
                                        }
                                        callback.onSuccess()
                                    }
                                }))
                        }

                        ButtonType.WATCH -> {
                            listener.onEventClicked(selectedEvent!!)
                        }

                        ButtonType.START_OVER -> {

                        }

                        ButtonType.CC_OR_SUBTITLE -> {
                            if (BuildConfig.FLAVOR.contains("base")) {
                                callback.onSuccess()
                                showSubtitles()
                                return
                            }
                            if (BuildConfig.FLAVOR.contains("mtk")) {
                                // TODO BORIS - here logic for distinguishing whether channel is Fast or Atsc should be implemented.
                                // Here logic for mtk channels should be added
                                onCCPressed()
                                callback.onSuccess()
                                return
                            }
                            if (listener.getPlatformName().contains("RefPlus") || listener.getPlatformName().contains("RealTek")) {
                                if (listener.getClosedCaptionSubtitlesState() == true) {
                                    callback.onSuccess()
                                    showSubtitles()
                                } else {
                                    callback.onSuccess()
                                    onCCPressed()
                                }
                                return
                            }
                        }

                        ButtonType.WATCHLIST, ButtonType.WATCHLIST_REMOVE -> {
                            (listener.onWatchlistClicked(selectedEvent!!, object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    callback.onFailed(error)
                                }

                                override fun onSuccess() {
                                    eventsAdapter!!.refreshEventAtPosition(selectedEventPositionInAdapter)
                                    var position = -1
                                    for (index in 0 until buttonsList.size) {
                                        var button = buttonsList[index]
                                        var isInWatchlist = listener.isInWatchlist(selectedEvent!!)
                                        if (button == ButtonType.WATCHLIST || button == ButtonType.WATCHLIST_REMOVE) {
                                            if (isInWatchlist!!) {
                                                buttonsList.set(index, ButtonType.WATCHLIST_REMOVE)
                                            } else {
                                                buttonsList.set(index, ButtonType.WATCHLIST)
                                            }
                                            position = index
                                            break
                                        }
                                    }
                                    if (position != -1) {
                                        buttonAdapter!!.updateButton(
                                            position,
                                            buttonsList.get(position)
                                        )

                                    }
                                    callback.onSuccess()
                                }
                            }))
                        }

                        ButtonType.KEYBOARD -> {
                            listener.onKeyboardClicked()
                            callback.onSuccess()

                        }

                        ButtonType.AUDIO -> {
                            callback.onSuccess()
                            showAudio()
                        }
                        else -> {

                        }
                    }
                }

                override fun onKeyUp(position: Int): Boolean {
                    recyclerView!!.requestFocus()
                    return true
                }

                override fun onKeyDown(position: Int): Boolean {
                    return true
                }

                override fun onKeyRight(position: Int): Boolean {
                    return false
                }

                override fun onKeyLeft(position: Int): Boolean {
                    return false
                }

                override fun onKeyBookmark() {
                }

                override fun onCCPressed(): Boolean { // this method is called when user clicks to the CC/Subtitle button
                    if(listener.isClosedCaptionEnabled() == true && isCurrentEventOnCurrentChannel){
                        listener.setClosedCaption()
                        updateCCInfo()
                    } else {
                        listener.saveUserSelectedCCOptions("display_cc", 1)
                        listener.setClosedCaption()
                        updateCCInfo()
                    }
                    return (isCurrentEventOnCurrentChannel && listener.isClosedCaptionEnabled() == true)
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

                override fun onDigitPressed(digit: Int) {}

                override fun onFocusChanged(hasFocus: Boolean) {
                    if(hasFocus){
                        customDetails.updateEventTitleMarqueeSelection(false)
                    }
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }
            }
        buttonContainer!!.setItemSpacing(
            Utils.getDimensInPixelSize(R.dimen.custom_dim_20)
        )

        tracksVerticalGridView!!.setNumColumns(1)

        audioTracksCheckListAdapter = CheckListAdapter(
            fadingEdgeLayout = fadingEdgeLayout,
            FadeAdapter.FadeAdapterType.VERTICAL
        )

        audioTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {
            override fun onItemClicked(position: Int) {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                //this method is called to restart inactivity timer for info banner scene
                (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                    try {
                        if (isRef5Flavour) {
                            val size = audioTracks!!.size
                            //handle default option
                            if (position == size) {
                                Log.e("BHANYA", " default option selected show list again")
                                listener.defaultAudioClicked()
                                showAudio()
                            } else {
                                currentSelectedAudioTrack = audioTracks!![position]
                                listener.onAudioTrackClicked(currentSelectedAudioTrack!!)
                            }
                        } else {
                            currentSelectedAudioTrack = audioTracks!![position]
                            listener.onAudioTrackClicked(currentSelectedAudioTrack!!)
                        }

                    } catch (e: Exception) {
                        Log.e("BHANYA","Exception e $e ")
                        showAudio()
                    }

                if(listener.getActiveChannel().tunerType == TunerType.ANALOG_TUNER_TYPE) {
                    if (currentSelectedAudioTrack?.isAnalogTrack == true && !currentSelectedAudioTrack?.trackName.isNullOrEmpty()) {
                        customDetails.updateAudioChannelInfo(currentSelectedAudioTrack?.trackName!!)
                    }
                }
            }

            override fun onAdditionalItemClicked() {
                // NOT IMPORTANT for Audio tracks. There is no "Off" button in Audio.
            }

            override fun onDownPressed(position: Int): Boolean {
                return false
            }

            override fun onUpPressed(position: Int): Boolean {
                return false
            }

            override fun onBackPressed(): Boolean {
                buttonContainer!!.requestFocus() // move focus back to the last focused button in horizontalGridView which in this case should be AUDIO button
                tracksWrapperLinearLayout!!.visibility = View.INVISIBLE
                sideViewOpen = 0
                customDetails.updateAudioTracks(audioTracks,listener.getCurrentAudioTrack())
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

        subtitleTracksCheckListAdapter.adapterListener = object : CheckListAdapter.CheckListAdapterListener {
            override fun onItemClicked(position: Int) {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                //this method is called to restart inactivity timer for info banner scene
                (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()
                try {
                    currentSelectedSubtitleTrack = subtitleTracks!![position]
                    listener.onSubtitleTrackClicked(currentSelectedSubtitleTrack!!)
                } catch (e: Exception) {
                    showSubtitles()
                }
            }

            override fun onAdditionalItemClicked() {  // called when user clicks to the "Off" button in CheckListAdapter for Subtitles
                listener.setSubtitles(false)
            }

            override fun onDownPressed(position: Int): Boolean {
                return false
            }

            override fun onUpPressed(position: Int): Boolean {
                return false
            }

            override fun onBackPressed(): Boolean {
                buttonContainer!!.requestFocus() // move focus back to the last focused button in horizontalGridView which in this case should be AUDIO button
                tracksWrapperLinearLayout!!.visibility = View.INVISIBLE
                sideViewOpen = 0
                customDetails.updateSubtitleTracks(listener.getAvailableSubtitleTracks(),listener.getCurrentSubtitleTrack(), listener.isSubtitlesEnabled())
                return true
            }

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener.setSpeechText(text = text, importance = importance)
            }

            override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
                listener.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
            }
        }

        channelIndex!!.typeface = TypeFaceProvider.getTypeFace(
            context!!,
            ConfigFontManager.getFont("font_regular")
        )

        channelName!!.typeface = TypeFaceProvider.getTypeFace(
            context!!,
            ConfigFontManager.getFont("font_bold")
        )

        channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        buttonContainer!!.removeAllViews()

        //adapter
        setupAdapter()

        initializeLoadingPlaceholder(onStartLoading = {
            rootView!!.visibility = View.INVISIBLE

        })
        // Hide Loading Placeholder
        if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.INFO_BANNER) == true) {
            LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.INFO_BANNER,
                onHidden = { rootView!!.visibility = View.VISIBLE }
            )
        }

        //TODO implement button setup based on channel type
    }

    private fun showSubtitles() {
        tracksVerticalGridView!!.adapter = subtitleTracksCheckListAdapter // used to specify that adapter will be used for subtitle tracks.
        if (subtitleTracks == null || subtitleTracks!!.size == 0) {
            listener.showToast(ConfigStringsManager.getStringById("no_available_subtitle_tracks_msg"))
            return
        }
        //Setup subtitle side view
        sideViewOpen = 1

        currentSelectedSubtitleTrack = listener.getCurrentSubtitleTrack()

        var currentSelectedPosition = 0

        val subtitleCheckListItems = mutableListOf<CheckListItem>()

        var undefinedTracks = 0

        subtitleTracks!!.forEachIndexed {index, track ->
            val name = ConfigStringsManager.getStringById(track.trackName.lowercase())
            val infoIcons = mutableListOf<Int>()

            if (track.trackName.lowercase().contains("undefined"))  {
                undefinedTracks++
                name.plus(" $undefinedTracks")
            }

            if (track.isHoh){
                infoIcons.add(R.drawable.ic_hoh)
            }

            if(track.isTxtBased) {
                infoIcons.add(R.drawable.ic_ttx)
            }

            val isSubtitleEnabled = listener.isSubtitlesEnabled()
            if ((currentSelectedSubtitleTrack != null) && (track.trackId == currentSelectedSubtitleTrack!!.trackId)  && isSubtitleEnabled) {
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
            isChecked = !listener.isSubtitlesEnabled()
        )

        if (!listener.isSubtitlesEnabled()) {
            currentSelectedPosition = subtitleTracks!!.size
        }


        tracksWrapperLinearLayout?.postDelayed(Runnable {
            tracksWrapperLinearLayout!!.visibility = View.VISIBLE
            tracksVerticalGridView?.layoutManager?.scrollToPosition(currentSelectedPosition)
            tracksVerticalGridView?.layoutManager?.findViewByPosition(currentSelectedPosition)?.requestFocus()
            tracksVerticalGridView?.requestFocus()
        }, 100)
    }

    private fun setupAdapter() {
        eventsAdapter = InfoBannerEventsAdapter(
            listener.getDateTimeFormat(),
            toSpeechTextSetterInterface = listener)

        eventsAdapter!!.setListener(object : InfoBannerEventsAdapter.AdapterListener {

            override fun getCurrentTime(tvChannel: TvChannel): Long {
                return listener.getCurrentTime(tvChannel)
            }

            override fun isScrolling(): Boolean {
                return isListScrolling
            }

            override fun hasScheduledReminder(
                tvEvent: TvEvent,
                callback: IAsyncDataCallback<Boolean>
            ) {
                listener.hasScheduledReminder(tvEvent, callback)
            }

            override fun onDownPressed() {
                if (buttonsList.isNotEmpty()){
                    buttonContainer!!.requestFocus()
                }
            }

            override fun hasScheduledRecording(
                tvEvent: TvEvent,
                callback: IAsyncDataCallback<Boolean>
            ) {
                listener.hasScheduledRecording(tvEvent, callback)
            }
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onEventSelected(item: TvEvent, selectedItemPositionInAdapter: Int, onSelected: () -> Unit) {
                buttonsList.clear()
                buttonContainer!!.adapter = null
                selectedEvent = item
                selectedEventPositionInAdapter = selectedItemPositionInAdapter

                refreshRecordButton()
                refreshWatchlistButton()
                eventsAdapter!!.refreshEventAtPosition(selectedEventPositionInAdapter) // important if event is added or removed from watchlist from DetailsScene
                var isEighteenPlus = false
                if (selectedEvent != null) {
                    isEighteenPlus = ReferenceApplication.isBlockedContent(selectedEvent!!)
                }

                isCurrentEventOnCurrentChannel = if (listener.isCurrentEvent(item)) { // only if event is in present time check whether it is on currently active channel
                    TvChannel.compare(
                        listener.getActiveChannel(),
                        item.tvChannel
                    )
                }else{
                    false
                }
                if (item.name == ConfigStringsManager.getStringById("no_info")) {
                    customDetails.updateDataForRecording(item, listener.getDateTimeFormat())

                    eventsAdapter!!.eventHasButtons = true
                    // placeholder must be hidden here (before return) in order to hide it when
                    // event with no info is selected
                    if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.INFO_BANNER) == true) {
                        LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.INFO_BANNER,
                            onHidden = { rootView!!.visibility = View.VISIBLE }
                        )
                    }
                    onSelected()

                    if (listener.isClosedCaptionEnabled() == true) {
                        customDetails.updateCcInfo( listener.getClosedCaption(), listener.isCCTrackAvailable())
                    }

                    buttonsList.add(ButtonType.AUDIO)
                    if (!item.tvChannel.isRadioChannel) {
                        buttonsList.add(ButtonType.CC_OR_SUBTITLE)
                    }
                    eventsAdapter!!.eventHasButtons = true
                    return
                }

                refreshCustomDetails(item)

                // Hide Loading Placeholder
                if (LoadingPlaceholder.isCurrentStateShow(PlaceholderName.INFO_BANNER) == true) {
                    LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.INFO_BANNER,
                        onHidden = { rootView!!.visibility = View.VISIBLE }
                    )
                }

                buttonContainer!!.adapter = buttonAdapter
                buttonAdapter!!.refresh(buttonsList)
                buttonContainer!!.clearFocus()
                customDetails.updateEventTitleMarqueeSelection(true)

                onSelected()
            }

            override fun getAdapterPosition(position: Int) {
                TODO("Not yet implemented")
            }

            override fun rearrangeScene(hasImages: Boolean) {
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onEventClicked(event: TvEvent) {
                //this method is called to restart inactivity timer for no signal power off
                (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                //this method is called to restart inactivity timer for info banner scene
                (ReferenceApplication.getActivity() as MainActivity).startSceneInactivityTimer()

                if (event != null) {
                    //TODO open parental lock scene when event is isEighteenPlus
//                    val isEighteenPlus = ReferenceApplication.isBlockedContent(event!!)
//                    if(!isEighteenPlus)

                    listener.showDetails(event)
                }
            }

            override fun onWatchlistClicked(tvEvent: TvEvent) {
                buttonAdapter!!.listener!!.itemClicked(ButtonType.WATCHLIST, object: IAsyncCallback{
                    override fun onFailed(error: Error) {
                    }
                    override fun onSuccess() {
                    }

                })
            }

            override fun onRecordClicked(event: TvEvent) {
                buttonAdapter!!.listener!!.itemClicked(ButtonType.RECORD, object: IAsyncCallback{
                    override fun onFailed(error: Error) {
                    }
                    override fun onSuccess() {
                    }

                })
            }

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener.setSpeechText(text = text, importance = importance)
            }

            override fun isParentalControlsEnabled(): Boolean {
                return listener.isParentalControlsEnabled()
            }

            override fun onCCPressed() { //this will be called only when focus is on CURRENT EVENT card and CC is pressed
                if(listener.isClosedCaptionEnabled() == true){
                    listener.setClosedCaption()
                    updateCCInfo()
                } else {
                    listener.saveUserSelectedCCOptions("display_cc", 1)
                    listener.setClosedCaption()
                    updateCCInfo()
                }
            }

            override fun isInWatchlist(tvEvent: TvEvent): Boolean? {
                return listener.isInWatchlist(tvEvent)
            }

            override fun isInRecList(tvEvent: TvEvent): Boolean {
                return listener.getIsInReclist(tvEvent)
            }

            override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
                return listener.isCurrentEvent(tvEvent)
            }

            override fun isEventLocked(tvEvent: TvEvent) = listener.isEventLocked(tvEvent)
            override fun getConfigInfo(nameOfInfo: String): Boolean {
                return listener.getConfigInfo(nameOfInfo)
            }
        })

        recyclerView!!.setNumRows(1)
        recyclerView!!.setItemSpacing(
            Utils.getDimensInPixelSize(CustomCard.CustomCardInfoBanner.SPACE_BETWEEN_ITEMS)
        )
        recyclerView!!.adapter = eventsAdapter

        //make focus fixed on th left side of the screen
        recyclerView!!.itemAlignmentOffset =
            context!!.resources.getDimensionPixelSize(R.dimen.custom_dim_n145)
        recyclerView!!.itemAlignmentOffsetPercent =
            HorizontalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
        recyclerView!!.windowAlignmentOffset = 0
        recyclerView!!.windowAlignmentOffsetPercent =
            HorizontalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        recyclerView!!.windowAlignment = HorizontalGridView.WINDOW_ALIGN_NO_EDGE
        recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                isListScrolling = newState != 0
            }
        })
    }

   @RequiresApi(Build.VERSION_CODES.R)
   private fun refreshCustomDetails(tvEvent: TvEvent) {
        customDetails.updateData(
            tvEvent = tvEvent,
            listener.getParentalRatingDisplayName(tvEvent.parentalRating, tvEvent),
            isCurrentChannel = isCurrentEventOnCurrentChannel,
            onPastEvent = {
                if (ReferenceApplication.shouldShowStartOverButton) {
                    buttonsList.add(ButtonType.START_OVER)
                }
                if (listener.getConfigInfo("virtual_rcu")) {
                    buttonsList.add(ButtonType.KEYBOARD)
                }
            },
            onFutureEvent = {
                if (listener.getConfigInfo("pvr")) {
                    if (tvChannel!!.tunerType != TunerType.ANALOG_TUNER_TYPE) {
                        val isInRecList = listener.getIsInReclist(tvEvent)
                        if (isInRecList) {
                            buttonsList.add(ButtonType.CANCEL_RECORDING)
                        } else {
                            if(tvEvent.isSchedulable) {
                                buttonsList.add(ButtonType.RECORD)
                            }
                        }
                    }
                }

                if (listener.isInWatchlist(tvEvent)!!) {
                    buttonsList.add(ButtonType.WATCHLIST_REMOVE)
                }
                else {
                    if(tvEvent.isSchedulable) {
                        buttonsList.add(ButtonType.WATCHLIST)
                    }
                }
                if (listener.getConfigInfo("virtual_rcu")) {
                    buttonsList.add(ButtonType.KEYBOARD)
                }
                eventsAdapter!!.eventHasButtons = true
            },
            onCurrentEvent = {
                if (ReferenceApplication.shouldShowStartOverButton) {
                    buttonsList.add(ButtonType.START_OVER)
                }

                buttonsList.add(ButtonType.WATCH)
                if (listener.getConfigInfo("pvr")) {
                    if (tvChannel!!.tunerType != TunerType.ANALOG_TUNER_TYPE) {
                        val isRecordingInProcess = listener.RecordingInProgress()
                        if (isRecordingInProcess) {
                            if (TvChannel.compare(
                                    listener.getRecordingInProgressTvChannel(),
                                    tvEvent.tvChannel
                                )
                            ) {
                                buttonsList.add(ButtonType.CANCEL_RECORDING)
                            } else {
                                buttonsList.add(ButtonType.RECORD)
                            }
                        } else {
                            buttonsList.add(ButtonType.RECORD)
                        }
                    }
                }

                if (listener.getConfigInfo("virtual_rcu")) {
                    buttonsList.add(ButtonType.KEYBOARD)
                }

                if (isCurrentEventOnCurrentChannel) {
                    customDetails.updateDolbyImageView(listener.getIsDolby(TvTrackInfo.TYPE_AUDIO))
                    customDetails.updateAdImageView(listener.getIsAudioDescription(TvTrackInfo.TYPE_AUDIO))
                    audioTracks = listener.getAvailableAudioTracks()

                    customDetails.updateAudioTracks(audioTracks,listener.getCurrentAudioTrack())
                    if (tracksWrapperLinearLayout?.visibility == View.VISIBLE && sideViewOpen == 2) {
                        showAudio()
                    }

                    if (tracksWrapperLinearLayout?.visibility == View.VISIBLE && sideViewOpen == 1) {
                        showSubtitles()
                    }

                    if (listener.getClosedCaptionSubtitlesState() == true) {
                        customDetails.updateSubtitleTracks(listener.getAvailableSubtitleTracks(),listener.getCurrentSubtitleTrack(), listener.isSubtitlesEnabled())
                    }

                    customDetails.updateVideoQuality(tvEvent.tvChannel, listener.getVideoResolution())
                    customDetails.updateHohImageView(listener.isHOH(TvTrackInfo.TYPE_SUBTITLE) || listener.isHOH(TvTrackInfo.TYPE_AUDIO))
                    val isTeletext = listener.getTeleText(TvTrackInfo.TYPE_SUBTITLE)
                    val isTeletextEnabled = listener.getConfigInfo("teletext_enable_column")
                    customDetails.updateTtxImageView(isTeletext && isTeletextEnabled)

                    refreshAudioChannel()

                    if (listener.isClosedCaptionEnabled() == true) {
                        customDetails.updateCcInfo(listener.getClosedCaption(), listener.isCCTrackAvailable())
                    }

                    buttonsList.add(ButtonType.AUDIO)

                    if (!tvEvent.tvChannel.isRadioChannel) {
                        buttonsList.add(ButtonType.CC_OR_SUBTITLE)
                    }

                }
            },
            listener.getCurrentTime(tvEvent.tvChannel),
            listener.isCCTrackAvailable(),
            listener.getDateTimeFormat(),
            isEventLocked = listener.isEventLocked(tvEvent)
            )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any) {
        if (data is Long) {
            val simpleDateFormat = SimpleDateFormat("HH:mm", Locale("en"))
            var time = simpleDateFormat.format(Date(data))
            if (currentTime!!.time != time) {
                currentTime!!.time = time
            }

            if (selectedEvent != null) {
                if (selectedEvent!!.endTime < data) {
                    listener.refreshData(tvChannel!!)
                } else {
                    refreshCustomDetails(selectedEvent!!)
                }
            }
        }
        if (Utils.isListDataType(data, IAudioTrack::class.java)){
            updateCCInfo()
            customDetails.updateAudioTracks(data as List<IAudioTrack>?)
        }
        else if (data is ArrayList<*>) {
            events.clear()
            for (i in 0 until (data as ArrayList<*>).size) {
                events.add(data.get(i) as TvEvent)
            }

            ReferenceApplication.runOnUiThread {
                eventsAdapter!!.currentChannel = listener.getActiveChannel()
                eventsAdapter!!.refresh(events)
                recyclerView!!.post {
                    recyclerView!!.visibility = View.VISIBLE
                    recyclerView?.postDelayed({
                        scrollToCurrentEvent(events)
                    }, 100)
                }
            }
        }

        if (data is TvChannel) {
            if (sideViewOpen != 0) {
                tracksWrapperLinearLayout!!.visibility = View.INVISIBLE
                sideViewOpen = 0
            }

            channelLogo!!.visibility = View.INVISIBLE

            tvChannel = data as TvChannel?

            if (tvChannel!!.isLocked && isParentalOn) {
                isLocked!!.visibility = View.VISIBLE
                isLocked!!.setImageResource(R.drawable.ic_channel_lock)
                isLocked!!.imageTintList =
                    ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            } else {
                isLocked!!.visibility = View.GONE
            }
            val shouldScrambleVisible =
                listener.isScrambled() && listener.getActiveChannel()
                    .getUniqueIdentifier() == data.getUniqueIdentifier()
            if (shouldScrambleVisible) {
                isScrambled!!.visibility = View.VISIBLE
                isScrambled!!.setImageResource(R.drawable.scrambled_channel_icon)
                isScrambled!!.imageTintList =
                    ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            } else {
                isScrambled!!.visibility = View.GONE
            }
            channelName!!.visibility = View.GONE
            channelIndex!!.visibility = View.GONE
            channelType!!.visibility = View.GONE
            channelIndex!!.text = tvChannel!!.getDisplayNumberText()

            var channelTypeText = listener.getChannelSourceType(tvChannel!!)
            channelType!!.text = channelTypeText

            val channelLogoPath = data.logoImagePath

            if (channelLogoPath != null) {
                Utils.loadImage(channelLogoPath, channelLogo!!, object : AsyncReceiver {
                    override fun onFailed(error: core_entities.Error?) {
                        channelName!!.visibility = View.VISIBLE
                        channelName!!.text = tvChannel!!.name
                        channelIndex!!.visibility = View.VISIBLE
                        if (channelTypeText.isNotEmpty()) channelType!!.visibility = View.VISIBLE
                        channelName!!.post {
                            calculateChannelNameTextSize()
                        }
                    }

                    override fun onSuccess() {
                        channelName!!.visibility = View.GONE
                        channelName!!.text = ""
                        channelIndex!!.visibility = View.VISIBLE
                        channelLogo!!.visibility = View.VISIBLE
                        if (channelTypeText.isNotEmpty()) channelType!!.visibility = View.VISIBLE
                    }
                })
            } else {
                channelName!!.visibility = View.VISIBLE
                channelName!!.text = tvChannel!!.name
                channelIndex!!.visibility = View.VISIBLE
                if (channelTypeText.isNotEmpty()) channelType!!.visibility = View.VISIBLE
                channelName!!.post {
                    calculateChannelNameTextSize()
                }
            }

            if (listener.getClosedCaptionSubtitlesState() == true) {
                subtitleTracks = listener.getAvailableSubtitleTracks()
                if (subtitleTracks != null) {
                    if (subtitleTracks!!.size != 0) {
                        subtitleTracks!!.forEach { item ->
                            if (!existingSubtitleTrackCodes.contains(formatLanguageCode(item.languageName))) {
                                addSubtitleTrackInfo(item.languageName)
                            }
                        }
                    }
                }
            }

            super.refresh(data)
        }
    }

    override fun dispose() {
        (ReferenceApplication.getActivity() as MainActivity).stopSceneInactivityTimer()
        super.dispose()
    }

    private fun initializeLoadingPlaceholder(onStartLoading: () -> Unit = {}) {
        LoadingPlaceholder(
            context = ReferenceApplication.applicationContext(),
            placeholderViewId = R.layout.loading_layout_info_banner_main,
            parentConstraintLayout = view!!,
            name = PlaceholderName.INFO_BANNER
        )

        LoadingPlaceholder.showLoadingPlaceholder(
            PlaceholderName.INFO_BANNER,
            onShown = onStartLoading
        )
    }


    fun calculateChannelNameTextSize() {
        channelName!!.post {
            if (channelName!!.lineCount > 2) {
                val textSize =
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
                channelName!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

            } else {
                val textSize =
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12)
                channelName!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            }
        }
    }

    private fun formatLanguageCode(languageCode: String): String {
        if (languageCode.length > 2) {
            return languageCode.uppercase(Locale.getDefault()).substring(0, 2)
        } else {
            return languageCode.uppercase(Locale.getDefault())
        }
    }

    private fun addSubtitleTrackInfo(languageCode: String) { // TODO this have to be removed in the future when subtitle is handled inside CUSTOM DETAILS
        var trackView = TextView(context)

        var layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        if (trackCount > 0) {
            Utils.getDimensInPixelSize(R.dimen.custom_dim_13_5)
        }

        trackView.gravity = Gravity.CENTER_VERTICAL
        trackView.setPadding(0, 0, 0, 0)
        layoutParams.rightMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_5)
        layoutParams.topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_1_5)
        trackView.layoutParams = layoutParams


        trackView.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            14f
        )

        var formattedLanguageCode = formatLanguageCode(languageCode)
        if (languageCode == "")
            trackView.text = "UN"
        else
            trackView.text = formattedLanguageCode


        trackView.setTextColor(
            Color.parseColor(ConfigColorManager.getColor("color_text_description"))
        )
        trackView.typeface = TypeFaceProvider.getTypeFace(
            context!!,
            ConfigFontManager.getFont("font_bold")
        )

        existingSubtitleTrackCodes.add(formattedLanguageCode)

        trackCount++
    }


    private fun scrollToCurrentEvent(eventsList: MutableList<TvEvent>) {
        if (eventsList.isEmpty()) {
            return
        }
        findCurrentEventPosition(eventsList, listener.getCurrentTime(eventsList[0].tvChannel), object : IAsyncDataCallback<Int> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: Int) {
                currentEventPosition = data
                if (currentEventPosition != -1) {
                    recyclerView!!.clearFocus()
                    recyclerView!!.scrollToPosition(currentEventPosition!!)
                    recyclerView!!.post {
                        val view =
                            recyclerView!!.layoutManager!!.findViewByPosition(
                                currentEventPosition!!
                            )
                        view?.requestFocus()
                    }
                } else {
                    recyclerView!!.clearFocus()
                    recyclerView!!.scrollToPosition(0)
                    recyclerView!!.post {
                        recyclerView?.layoutManager?.findViewByPosition(0)?.requestFocus()
                    }
                }
            }
        })
    }

    private fun findCurrentEventPosition(
        events: MutableList<TvEvent>,
        currentTime: Long,
        callback: IAsyncDataCallback<Int>
    ) {

        var eventsItemList: MutableList<TvEvent> = mutableListOf()
        eventsItemList.addAll(events)
        var currentPosition = -1

        if (eventsItemList.size > 0) {
            run exitForEach@{
                eventsItemList.forEachIndexed() { index, item ->
                    item.let {
                        val startTime =
                            Date(it.startTime)
                        val endTime =
                            Date(it.endTime)
                        val isCurrentEvent =
                            startTime.before(Date(currentTime)) && Date(currentTime).before(
                                endTime
                            )

                        if (isCurrentEvent) {
                            currentPosition = index
                            return@exitForEach
                        }
                    }
                }
            }
        }

        callback.onReceive(currentPosition)
    }

    fun refreshActiveAudioTrack() {
        if (tracksWrapperLinearLayout?.visibility == View.VISIBLE && sideViewOpen == 2) {
            buttonContainer!!.layoutManager!!.getChildAt(buttonsList.indexOf(ButtonType.AUDIO))!!.callOnClick()
        }
    }

    fun refreshAudioChannel() {
        customDetails.updateAudioChannelInfo(listener.getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO))
        customDetails.updateAudioChannelFormatInfo(listener.getAudioFormatInfo())
        // getAudioChannelInfo(TvTrackInfo.TYPE_AUDIO) does not always return the correct track name for fluki so we need to use direct access
        if(listener.getActiveChannel().tunerType == TunerType.ANALOG_TUNER_TYPE) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "refreshAudioChannel: ${listener.getCurrentAudioTrack()?.trackName }")
            if (listener.getCurrentAudioTrack()?.trackName != null) {
                customDetails.updateAudioChannelInfo(listener.getCurrentAudioTrack()?.trackName!!)
            }
        }
        if (tracksWrapperLinearLayout?.visibility == View.VISIBLE && sideViewOpen == 2) {
            showAudio()
        }
    }

    fun showAudio() {
        tracksVerticalGridView!!.adapter = audioTracksCheckListAdapter // used to specify that adapter will be used for audio tracks.
        if (audioTracks == null || audioTracks!!.size == 0) {
            listener.showToast(ConfigStringsManager.getStringById("no_available_audio_tracks_msg"))
            return
        }
        sideViewOpen = 2
        //Setup audio side view
        currentSelectedAudioTrack = listener.getCurrentAudioTrack()
        Log.e("DHANYA","current audio track is ${currentSelectedAudioTrack?.trackName}")

        var currentSelectedPosition = 0

        //check current track
        var audioCheckListItems = mutableListOf<CheckListItem>()

        var undefinedTracks = 0

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "showAudio: ${audioTracks?.size}")
        audioTracks!!.forEachIndexed { index, track ->

            val name = buildMtsAudioString(track)

            val infoIcons = mutableListOf<Int>()

            if (track.isAd) {
                infoIcons.add(R.drawable.ic_ad)
            }
            if (track.isDolby) {
                infoIcons.add(R.drawable.ic_dolby)
            }

            if (track.isHohAudio) {
                infoIcons.add(R.drawable.ic_hoh)
            }


            if (track.isSps) {
                infoIcons.add(R.drawable.ic_sps)
            }

            if (track.isAdSps) {
                infoIcons.add(R.drawable.ic_a_s)
            }
            Log.e("BHANYA","track name ${track.trackName} ,isAd ${track.isAd} ,isDolby ${track.isDolby} , isHohAudio ${track.isHohAudio} , isSps ${track.isSps} ,isADSps ${track.isAdSps}")

            if (track.trackId == currentSelectedAudioTrack!!.trackId) {
                audioCheckListItems.add(CheckListItem(name, true, infoIcons))
                currentSelectedPosition = index
            }
            else {
                audioCheckListItems.add(CheckListItem(name, false, infoIcons))
            }
        }
        audioCheckListItems.add(CheckListItem(ConfigStringsManager.getStringById("default_audio"), false))

        tracksTitle!!.text = ConfigStringsManager.getStringById("audio")
        audioTracksCheckListAdapter.refresh(audioCheckListItems)

        tracksWrapperLinearLayout!!.postDelayed(Runnable {
            tracksWrapperLinearLayout!!.visibility = View.VISIBLE
            tracksVerticalGridView!!.layoutManager!!.scrollToPosition(currentSelectedPosition)
            tracksVerticalGridView!!.requestFocus()

        }, 100)
    }

    private fun buildMtsAudioString(track: IAudioTrack): String {
        var name = ""
        if (isRef5Flavour) {
            val trackName = ConfigStringsManager.getStringById(track.trackName.lowercase())
            var dValue = ""
            var audioTracksByTrackId = ""
            if (track.isAd) {
                dValue = ConfigStringsManager.getStringById("audio_type_visually_impaired")
            } else if (track.isHohAudio) {
                dValue = ConfigStringsManager.getStringById("subtitle_type_hearing_impaired")
            }
            val apdText = track.apdText
            Log.e("BHANYA","apdText $apdText")
            if (track.trackId == currentSelectedAudioTrack?.trackId) {
                audioTracksByTrackId = getEncodingString()
                Log.d("BHANYA", "it is current selected audio track " +
                        "${track.trackId} audioTracksByTrackId $audioTracksByTrackId")
            }
            name = "$trackName$audioTracksByTrackId$apdText $dValue"
        } else {
            name = ConfigStringsManager.getStringById(track.trackName.lowercase())
        }
        return name
    }

    private fun getEncodingString(): String {
        val audioCodec: String = listener.getAudioFormatInfo()
        Log.d("BHANYA", "audioTrack encoding string :$audioCodec")
        return if (!TextUtils.isEmpty(audioCodec)) "($audioCodec)" else audioCodec
    }

    fun blockRecordButton() {
        //TODO VASILISA not tested
        for (index in 0 until buttonsList.size) {
            var button = buttonsList[index]
            if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                buttonAdapter!!.setNotClickable(index)
            }
            break
        }
    }

    fun unblockRecordButton() {
        //TODO VASILISA not tested
        for (index in 0 until buttonsList.size) {
            var button = buttonsList[index]
            if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                buttonAdapter!!.setClickable(index)
            }
            break
        }
    }

    fun updateCCInfo() {
        if (listener.isClosedCaptionEnabled() != true) return
        val isCCTrackAvailable = listener.isCCTrackAvailable()
        customDetails.updateCcInfo(listener.getClosedCaption(), isCCTrackAvailable)
    }

    /**
     * Refresh record button
     */
    fun refreshRecordButton() {
        if (listener.getConfigInfo("pvr")) {
            if (selectedEvent != null) {
                if (listener.isCurrentEvent(selectedEvent!!)) {
                    if (listener.getConfigInfo("pvr")) {
                        val isRecordingInProcess = listener.RecordingInProgress()
                        var position = -1
                        for (index in 0 until buttonsList.size) {
                            val button = buttonsList[index]
                            if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                                if (isRecordingInProcess) {
                                    if (TvChannel.compare(
                                            listener.getRecordingInProgressTvChannel(),
                                            selectedEvent!!.tvChannel
                                        )
                                    ) {
                                        buttonsList[index] = ButtonType.CANCEL_RECORDING
                                    } else {
                                        buttonsList[index] = ButtonType.RECORD
                                    }
                                } else {
                                    buttonsList[index] = ButtonType.RECORD
                                }
                                position = index
                                break
                            }
                        }
                        if (position != -1) {
                            ReferenceApplication.runOnUiThread {
                                buttonAdapter!!.updateButton(position, buttonsList[position])
                            }
                        }
                    }

                }
                else {
                    //Future recording
                    val position = -1
                    for (index in 0 until buttonsList.size) {
                        val button = buttonsList[index]
                        if (button == ButtonType.RECORD || button == ButtonType.CANCEL_RECORDING) {
                            listener.hasScheduledRecording(
                                selectedEvent!!,
                                object : IAsyncDataCallback<Boolean> {
                                    override fun onReceive(data: Boolean) {
                                        if (data) {
                                            buttonsList[index] = ButtonType.CANCEL_RECORDING
                                        } else {
                                            buttonsList[index] = ButtonType.RECORD
                                        }
                                    }

                                    override fun onFailed(error: Error) {}
                                })
                        }
                    }
                    if (position != -1) {
                        ReferenceApplication.runOnUiThread {
                            buttonAdapter!!.updateButton(position, buttonsList[position])
                        }
                    }
                }
            }
        }
    }

    fun refreshWatchlistButton(){
        var position = -1
        for (index in 0 until buttonsList.size) {
            var button = buttonsList[index]
            if (button == ButtonType.WATCHLIST || button == ButtonType.WATCHLIST_REMOVE) {
            listener.hasScheduledReminder(
                selectedEvent!!,
                object : IAsyncDataCallback<Boolean> {
                    override fun onReceive(data: Boolean) {
                        if (data) {
                            buttonsList.set(index, ButtonType.WATCHLIST_REMOVE)
                        } else {
                            buttonsList.set(index, ButtonType.WATCHLIST)
                        }
                    }

                    override fun onFailed(error: Error) {
                    }
                })
            }
            if (position != -1) {
                buttonAdapter!!.updateButton(position, buttonsList.get(position))
            }
        }

    }

    fun showMoreInfo() {
        selectedEvent?.let { listener.showDetails(it) }
    }

    interface InfoBannerWidgetListener : GWidgetListener, TTSSetterInterface,
        ToastInterface, TTSSetterForSelectableViewInterface {
        fun getIsCC(type: Int): Boolean
        fun getIsAudioDescription(type: Int): Boolean
        fun getTeleText(type: Int): Boolean
        fun getIsDolby(type: Int): Boolean
        fun isHOH(type: Int): Boolean
        fun getIsInReclist(tvEvent: TvEvent): Boolean
        fun getRecordingInProgress(callback: IAsyncDataCallback<com.iwedia.cltv.platform.model.recording.RecordingInProgress>)
        fun getActiveChannel(): TvChannel
        fun getRecordingInProgressTvChannel(): TvChannel
        fun RecordingInProgress(): Boolean
        fun showDetails(tvEvent: TvEvent)
        fun onEventClicked(tvEvent: TvEvent)
        fun onEventLongUpPressed()
        fun onEventLongDownPressed()
        fun onKeyboardClicked()
        fun onRecordButtonClicked(tvEvent: TvEvent, callback: IAsyncCallback)
        fun onAudioTrackClicked(audioTrack: IAudioTrack)
        fun onSubtitleTrackClicked(subtitleTrack: ISubtitle)
        fun isSubtitleAudioButtonPressed(): Boolean
        //TODO BORIS onSubtitleKeyClicked and getCurrentSubtitleTrack call the same method from playerModule in manager
        fun onSubtitleKeyClicked(callback: AsyncDataReceiver<ISubtitle>)
        fun onAudioKeyClicked(): IAudioTrack
        fun addToWatchlist(tvEvent: TvEvent, callback: IAsyncCallback)
        fun removeFromWatchlist(tvEvent: TvEvent, callback: IAsyncCallback)
        fun onWatchlistClicked(tvEvent: TvEvent, callback: IAsyncCallback)
        fun hasScheduledReminder(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>)
        fun hasScheduledRecording(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>)
        fun getCurrentAudioTrack(): IAudioTrack?
        fun getAvailableAudioTracks(): MutableList<IAudioTrack>?
        fun getAvailableSubtitleTracks(): MutableList<ISubtitle>?
        fun getCurrentSubtitleTrack(): ISubtitle?
        fun setSubtitles(view: Boolean)
        fun getClosedCaptionSubtitlesState(): Boolean?
        fun isSubtitlesEnabled(): Boolean
        fun isClosedCaptionEnabled(): Boolean?
        fun saveUserSelectedCCOptions(ccOptions: String, newValue: Int)
        fun getClosedCaption(): String?
        fun setClosedCaption(): Int?
        fun isCCTrackAvailable(): Boolean?
        fun isInWatchlist(event: TvEvent): Boolean?
        fun getChannelSourceType(tvChannel: TvChannel): String
        fun isParentalOn() : Boolean
        fun getAudioChannelInfo(type : Int) : String
        fun getAudioFormatInfo():String
        fun refreshData(tvChannel: TvChannel)
        fun getLanguageMapper(): LanguageMapperInterface
        fun getVideoResolution(): String
        fun isParentalControlsEnabled(): Boolean
        fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun isCurrentEvent(tvEvent: TvEvent) : Boolean
        fun getDateTimeFormat(): DateTimeFormat
        fun isEventLocked(tvEvent: TvEvent?): Boolean
        fun isPvrPathSet(): Boolean
        fun isUsbFreeSpaceAvailable(): Boolean
        fun isUsbWritableReadable(): Boolean
        fun isUsbStorageAvailable(): Boolean
        fun getConfigInfo(nameOfInfo: String): Boolean
        fun getPlatformName(): String
        fun isScrambled(): Boolean
        fun defaultAudioClicked()

    }
}