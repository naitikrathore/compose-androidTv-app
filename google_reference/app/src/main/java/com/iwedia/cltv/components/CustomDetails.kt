package com.iwedia.cltv.components

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TimeTextView
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.custom_view_base_classes.BaseConstraintLayout
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.VideoResolution
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.scene.channel_list.ChannelListItem
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.util.Locale


private const val TAG = "CustomDetails"

 val CUSTOM_DETAILS_FOR_YOU_WIDTH = R.dimen.custom_dim_680_5
 val CUSTOM_DETAILS_GUIDE = R.dimen.custom_dim_800
 val CUSTOM_DETAILS_CHANNEL_LIST_WIDTH = R.dimen.custom_dim_500
 val CUSTOM_DETAILS_ZAP_BANNER_WIDTH = R.dimen.custom_dim_715
 val CUSTOM_DETAILS_ZAP_BANNER_FAST_WIDTH = R.dimen.custom_dim_587
 val CUSTOM_DETAILS_INFO_BANNER_WIDTH = R.dimen.custom_dim_650
 val CUSTOM_DETAILS_FAST_EPG_WIDTH = R.dimen.custom_dim_607
 val CUSTOM_DETAILS_FAST_HOME_WIDTH = R.dimen.custom_dim_607
 val CUSTOM_DETAILS_DETAILS_SCENE_WIDTH = R.dimen.custom_dim_775

sealed class CustomDetails : BaseConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    protected val colorMainText = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
    protected val colorBackground = Color.parseColor(ConfigColorManager.getColor("color_background"))
    protected val colorProgress = Color.parseColor(ConfigColorManager.getColor("color_progress"))
    protected val colorDescription =
        Color.parseColor(ConfigColorManager.getColor("color_text_description"))
    private val fontRegular = TypeFaceProvider.getTypeFace(
        ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular")
    )
    private val fontBold = TypeFaceProvider.getTypeFace(
        ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_bold")
    )
    protected lateinit var detailsType: DetailsType
    protected var containerLinearLayout: ConstraintLayout

    var eventTitle: TextView
    var eventDescription: TextView
    protected var subtitleTracksLinearLayout: LinearLayout
    protected var subtitleTracksLinearLayoutContainer: LinearLayout
    protected var audioTracksLinearLayout: LinearLayout
    protected var audioTracksLinearLayoutContainer: LinearLayout
    protected var ccTracksLinearLayout: LinearLayout
    protected var ccTracksLinearLayoutContainer: LinearLayout
    protected var parentalTextView: TextView
    protected var channelIndexTextView: TextView
    protected var channelIndexTextView2: TextView
    protected var eventGenreTextView: TextView
    protected var separatorView: View
    protected var channelOrEventNameTextView: TextView
    protected var channelOrEventNameTextView2: TextView
    protected var channelLogoImageView: ImageView
    protected var channelLogoImageView2: ImageView
    protected var audioTypeLinearLayout : LinearLayout
    protected var audioFormatLinearLayout : LinearLayout
    protected var detailsScrollView : ScrollView

    protected var infoRowLinearLayout : LinearLayout
    private var infoIconsLinearLayout : LinearLayout

    protected var eventStartEndTimeRow1: TimeTextView
    protected var eventStartEndTimeRow2: TimeTextView
    protected var eventStartEndTimeRow3: TimeTextView

    var eventProgressBar: ProgressBar
    //view to show up next info in zap banner
    var upNextEventTitle: TextView

    protected var dolbyImageView: ImageView
    protected var adImageView: ImageView
    protected var ccImageView: ImageView
    protected var hdImageView: ImageView
    protected var subtitlesAndCCImageView: ImageView
    protected var uhdImageView: ImageView
    protected var sdImageView: ImageView
    protected var hohImageView: ImageView
    protected var ttxImageView: ImageView
    protected var fhdImageView: ImageView
    private var parentalRatingLinearLayoutRow1: LinearLayout // it is linear layout because TextView will be inserted in it programmatically to have consistency with Audio and Subtitle tracks
    private var parentalRatingLinearLayoutRow2: LinearLayout // it is linear layout because TextView will be inserted in it programmatically to have consistency with Audio and Subtitle tracks
    private var parentalRatingLinearLayoutRow3: LinearLayout // it is linear layout because TextView will be inserted in it programmatically to have consistency with Audio and Subtitle tracks

    /**
     * [timeAndParentalContainerLinearLayout] is used only for [CustomDetailsFastHome] at the moment when this comment is left.
     * By default visibility of this LinearLayout must be set to GONE in order to not be visible in every other scenarios.
     */
    protected var timeAndParentalContainerLinearLayout: LinearLayout

    protected var timeAndParentalContainerLinearLayout2: LinearLayout

    /**
     * isCurrentChannel is crucial variable used to manipulate icons visibility. Icons are allowed to be visible only if isCurrentChannel is set to true.
     */
    var isCurrentChannel: Boolean = false
    protected var hasSomethingInIconsRow =
        HashMap<View, Boolean>() // this property is used to dynamically change start margin of audio container - if Custom Details doesn't contain any icons, then start margin of audio container should be = 0dp

    protected var hasSomethingInAudioRow = false

    private lateinit var runnableStartEventTitleMarque : Runnable

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.custom_details, this, true)

        infoRowLinearLayout = findViewById(R.id.info_row_linear_layout)

        val firstRow = findViewById<ConstraintLayout>(R.id.first_row)
        infoIconsLinearLayout = findViewById(R.id.info_icons_linear_layout)

        (infoIconsLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
            marginEnd = Utils.getDimensInPixelSize(R.dimen.custom_dim_15) // total margin should be 25dp, but method addNewTextViewInLayout() have already added margin of 10 dp, so only 15 dp is missing.
        }

        eventTitle = firstRow.findViewById<TextView?>(R.id.event_title).apply {
            typeface = fontRegular // todo UNCOMMENT this line when appropriate font is set for fontRegular property.
            visibility = View.VISIBLE
            setTextColor(colorMainText)
        }
        eventGenreTextView = findViewById<TextView?>(R.id.event_genre).apply {
            typeface = fontRegular
            setTextColor(colorDescription)
        }
        separatorView = findViewById(R.id.view)

        containerLinearLayout = findViewById(R.id.container_linear_layout)

        eventDescription = findViewById<TextView?>(R.id.event_description).apply {
            typeface = fontRegular
            setTextColor(colorDescription)
        }
        audioTypeLinearLayout = findViewById(R.id.audio_type_linear_layout)

        (audioTypeLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
            this.marginEnd = Utils.getDimensInPixelSize(R.dimen.font_15) // total margin should be 25dp, but method addNewTextViewInLayout() have already added margin of 10 dp, so only 15 dp is missing.
        }

        audioFormatLinearLayout = findViewById(R.id.audio_format_linear_layout)
        (audioFormatLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
            this.marginEnd = Utils.getDimensInPixelSize(R.dimen.font_15) // total margin should be 25dp, but method addNewTextViewInLayout() have already added margin of 10 dp, so only 15 dp is missing.
        }
        channelOrEventNameTextView = firstRow.findViewById(R.id.channel_or_event_name_text_view)
        channelOrEventNameTextView2 = findViewById(R.id.channel_or_event_name_text_view_2)
        channelLogoImageView = firstRow.findViewById(R.id.channel_logo_image_view)
        channelLogoImageView2 = findViewById(R.id.channel_logo_image_view_2)
        channelIndexTextView = findViewById<TextView?>(R.id.channel_index_text_view).apply {
            typeface = fontRegular
        }
        channelIndexTextView2 = findViewById<TextView?>(R.id.channel_index_text_view_2).apply {
            typeface = fontRegular
        }
        subtitleTracksLinearLayout = findViewById(R.id.subtitle_tracks_linear_layout)
        subtitleTracksLinearLayoutContainer =
            findViewById(R.id.subtitle_tracks_linear_layout_container)

        detailsScrollView = findViewById(R.id.details_scroll_view)
        detailsScrollView.isFocusable = false // This is very important - if not set, ScrollView would be focusable and that will cause issue in RailAdapter because when pressing Dpad_DOWN focus would be moved to ScrollView instead on next rails
        audioTracksLinearLayout = findViewById(R.id.audio_tracks_linear_layout)

        (audioTracksLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
            this.marginEnd = Utils.getDimensInPixelSize(R.dimen.font_25)
        }

        audioTracksLinearLayoutContainer = findViewById(R.id.audio_tracks_linear_layout_container)

        ccTracksLinearLayout = findViewById(R.id.cc_tracks_linear_layout)

        (ccTracksLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
            this.marginEnd = Utils.getDimensInPixelSize(R.dimen.font_25)
        }

        ccTracksLinearLayoutContainer = findViewById(R.id.cc_tracks_linear_layout_container)

        eventStartEndTimeRow1 = firstRow.findViewById<TimeTextView?>(R.id.event_start_end_time_first_row).apply {
            typeface = fontRegular
            setTextColor(colorDescription)
        }
        eventStartEndTimeRow2 = findViewById<TimeTextView?>(R.id.event_start_end_time_row_2).apply {
            typeface = fontRegular
            setTextColor(colorDescription)
        }
        eventStartEndTimeRow3 = findViewById<TimeTextView?>(R.id.event_start_end_time_row_3).apply {
            typeface = fontRegular
            setTextColor(colorDescription)
        }
        parentalRatingLinearLayoutRow1 = firstRow.findViewById(R.id.parental_rating_linear_layout)
        parentalRatingLinearLayoutRow2 = findViewById(R.id.parental_rating_linear_layout_second_row)
        parentalRatingLinearLayoutRow3 = findViewById(R.id.parental_rating_linear_layout_third_row)
        timeAndParentalContainerLinearLayout = findViewById(R.id.time_and_parental_container_linear_layout)
        timeAndParentalContainerLinearLayout2 = findViewById(R.id.time_and_parental_container_linear_layout_2)

        eventProgressBar = findViewById<ProgressBar?>(R.id.event_progressbar).apply {
            visibility = INVISIBLE
            progressBackgroundTintList = ColorStateList.valueOf(colorDescription)
            progressTintList = ColorStateList.valueOf(colorProgress)
        }
        upNextEventTitle = findViewById<TextView?>(R.id.up_next_title).apply {
            visibility = INVISIBLE
        }

        dolbyImageView = infoIconsLinearLayout.findViewById(R.id.dolby_image_view)
        adImageView = infoIconsLinearLayout.findViewById(R.id.ad_image_view)
        hdImageView = infoIconsLinearLayout.findViewById(R.id.hd_image_view)
        subtitlesAndCCImageView = findViewById(R.id.subtitles_and_cc_image_view)
        ccImageView = infoIconsLinearLayout.findViewById(R.id.subtitles_image_view)
        uhdImageView = infoIconsLinearLayout.findViewById(R.id.uhd_image_view)
        sdImageView = infoIconsLinearLayout.findViewById(R.id.sd_image_view)
        hohImageView = infoIconsLinearLayout.findViewById(R.id.hoh_image_view)
        ttxImageView = infoIconsLinearLayout.findViewById(R.id.ttx_image_view)
        parentalTextView = findViewById(R.id.parental_text_view)
        fhdImageView = infoIconsLinearLayout.findViewById(R.id.fhd_image_view)

        infoRowLinearLayout.visibility = VISIBLE

//        refreshDrawableState()
    }

    /**
     * @param isCurrentTrack - flag serves the purpose of distinguishing the active track from others.
     * The active track is visually differentiated by using a distinct color compared to the rest.
     */
    private fun addTrackInfo(languageCode: String, trackType: TrackType, isCurrentTrack: Boolean) {

        val formattedLanguageCode = formatLanguageCode(languageCode)

        when (trackType) {
            TrackType.AUDIO -> {
                addNewTextViewInLayout(
                    label = if (languageCode == "") "UN" else formattedLanguageCode,
                    linearLayout = audioTracksLinearLayoutContainer,
                    shouldStandOut = isCurrentTrack
                )
                hasSomethingInAudioRow = true
            }
            TrackType.SUBTITLE -> {
                addNewTextViewInLayout(
                    label = if (languageCode == "") "UN" else formattedLanguageCode,
                    linearLayout = subtitleTracksLinearLayoutContainer,
                    shouldStandOut = isCurrentTrack
                )
            }
        }
    }

    /**
     * @param isSubtitleEnabled - functions as an indicator of whether subtitles are globally activated or deactivated.
     * When subtitles are disabled, no audio tracks will stand out from the others.
     * Conversely, when subtitles are enabled, the active audio track will be visually distinguished by using a distinct color.
     */
    fun updateSubtitleTracks(
        subtitleTracks: List<ISubtitle>?,
        activeSubtitleTrack: ISubtitle? = null, // if activeSubtitleTrack = null - there is no active track
        isSubtitleEnabled: Boolean = false // TODO BORIS this hardcoded "false" should be removed in the future
    ) {
        subtitleTracksLinearLayoutContainer.removeAllViews()
        if (!isCurrentChannel || subtitleTracks.isNullOrEmpty()) {
            subtitleTracksLinearLayout.visibility = GONE
            return
        }
        val listOfCurrentSubtitles = arrayListOf<String>()
        subtitleTracks.forEach { item ->
            if (!listOfCurrentSubtitles.contains(item.languageName)) {
                listOfCurrentSubtitles.add(item.languageName)
                addTrackInfo(
                    languageCode = item.languageCode,
                    trackType = TrackType.SUBTITLE,
                    isCurrentTrack = item.trackName == activeSubtitleTrack?.trackName && isSubtitleEnabled
                    //since custom details contain unique language code so if two subtiles have same name,
                    //then only one language will be visible, so indicating based on name.
                )
            }
        }
        if (subtitleTracksLinearLayoutContainer.getChildAt(0) != null) {
            subtitleTracksLinearLayout.visibility = View.VISIBLE
        }
    }

    /**
     * used to enable setting ccImageView visibility to visible outside of the CustomDetails class.
     *
     * This method was introduces since there was new requirement that CcImageView should be visible whenever CustomDetails
     * are shown for event that is current on current channel.
     */
    fun setCcImageViewVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) VISIBLE else GONE
        ccImageView.visibility = visibility
    }

    open fun updateAudioTracks(
        audioTracks: List<IAudioTrack>?,
        activeAudioTrack: IAudioTrack? = null// if activeAudioTrack = null - there is no active track
    ) {
        audioTracksLinearLayoutContainer.removeAllViews()
        if (!isCurrentChannel || audioTracks.isNullOrEmpty()) {
            audioTracksLinearLayout.visibility = GONE
            return
        }
        val listOfCurrentAudio = arrayListOf<String>()
        audioTracks.forEach { item ->
            if (!listOfCurrentAudio.contains(item.languageName) && !item.isAnalogTrack) {
                listOfCurrentAudio.add(item.languageName)
                addTrackInfo(
                    languageCode = item.languageCode,
                    trackType = TrackType.AUDIO,
                    isCurrentTrack = item.trackId == activeAudioTrack?.trackId
                )
            }
        }
        if (hasSomethingInAudioRow) {
            audioTracksLinearLayout.visibility = View.VISIBLE
            audioTracksLinearLayoutContainer.visibility = View.VISIBLE
        }
    }


    private fun addNewTextViewInLayout(label: String, linearLayout: LinearLayout, shouldStandOut: Boolean = false) {
        val trackTextView = TextView(ReferenceApplication.applicationContext())

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
        ).also {
            it.setMargins(0,0,Utils.getDimensInPixelSize(R.dimen.custom_dim_10),0)
        }

        trackTextView.layoutParams = layoutParams

        trackTextView.apply {
            gravity = Gravity.CENTER_VERTICAL
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP, 14f
            )
            setTextColor(
                if (shouldStandOut) {
                    colorMainText
                } else {
                    colorDescription
                }
            )
            text = label
            typeface = TypeFaceProvider.getTypeFace(
                Utils.getContext(),
                ConfigFontManager.getFont("font_bold")
            )
            // important: if includeFontPadding is not set to false, text would have padding and TextView wouldn't be centered in LinearLayout
            includeFontPadding = false
        }
        linearLayout.addView(trackTextView)
    }

    private fun formatLanguageCode(languageCode: String): String {
        return languageCode.uppercase(Locale.getDefault())
    }

    /**
     * used to update View which is in charge of showing Parental Rating text.
     *
     * Internally, method creates new TextView based on parental rating of passed tvEvent and inserts it in LinearLayout which is created in xml file.
     */
    fun updateParentalRating(tvEvent: TvEvent, parentalRatingDisplayName: String) {
        if (tvEvent.parentalRating.isNullOrEmpty() && parentalRatingDisplayName.isEmpty()) return //if there is nothing to show for parental exit the method.
        else {
            parentalRatingLinearLayoutRow1.removeAllViews()
            parentalRatingLinearLayoutRow2.removeAllViews()
            parentalRatingLinearLayoutRow3.removeAllViews()
            addNewTextViewInLayout(
                label = parentalRatingDisplayName,
                linearLayout = parentalRatingLinearLayoutRow1
            )
            addNewTextViewInLayout(
                label = parentalRatingDisplayName,
                linearLayout = parentalRatingLinearLayoutRow2
            )
            addNewTextViewInLayout(
                label = parentalRatingDisplayName,
                linearLayout = parentalRatingLinearLayoutRow3
            )
            updateParentalRatingViewVisibility()
        }
    }

    private fun updateParentalRatingViewVisibility() {
        when (detailsType) {
            DetailsType.FAST_HOME -> parentalRatingLinearLayoutRow2.visibility = VISIBLE
            DetailsType.DETAILS_SCENE,  DetailsType.EPG,  DetailsType.INFO_BANNER -> parentalRatingLinearLayoutRow3.visibility = VISIBLE
            else -> parentalRatingLinearLayoutRow1.visibility = VISIBLE
        }
    }

    protected fun insertImageInChannelLogoImageView(
        tvChannel: TvChannel?,
        channelOrEventNameTextView: TextView = this.channelOrEventNameTextView,
        channelLogoImageView: ImageView = this.channelLogoImageView
    ) {
        val onFailed = {
            channelOrEventNameTextView.apply {
                text = tvChannel?.name
                visibility = VISIBLE
            }
            channelLogoImageView.visibility = GONE
        }
        if (tvChannel?.logoImagePath != null) {
            Utils.loadImage(
                path = tvChannel.logoImagePath,
                view = channelLogoImageView,
                callback = object : AsyncReceiver {
                    override fun onFailed(error: Error?) {
                        onFailed.invoke()
                    }

                    override fun onSuccess() {
                        channelLogoImageView.visibility = VISIBLE
                    }
                },
                shouldCompressImage = false
            )
        } else {
            onFailed.invoke()
        }
    }

    protected fun updateTimeTextView(
        recording: Recording
    ) {
        eventStartEndTimeRow1.apply {
            visibility = VISIBLE
            text = Recording.createRecordingTimeInfo(
                recording.recordingStartTime,
                recording.recordingEndTime
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    protected fun updateTimeTextView(tvEvent: TvEvent, dateTimeFormat: DateTimeFormat) {
        when (detailsType) {
            DetailsType.FAST_HOME -> updateTimeTextView(
                eventStartEndTimeRow2,
                tvEvent,
                dateTimeFormat
            )

            DetailsType.DETAILS_SCENE, DetailsType.EPG, DetailsType.INFO_BANNER -> updateTimeTextView(
                eventStartEndTimeRow3,
                tvEvent,
                dateTimeFormat
            )

            else -> updateTimeTextView(
                eventStartEndTimeRow1,
                tvEvent,
                dateTimeFormat
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun updateTimeTextView(timeTextView: TimeTextView, tvEvent: TvEvent, dateTimeFormat: DateTimeFormat) {
        timeTextView.apply {
            setDateTimeFormat(dateTimeFormat)
            visibility = VISIBLE
            time = tvEvent
        }
    }

    protected fun updateTitleAndDescription(
        tvEvent: TvEvent,
        isDescriptionEnabled: Boolean = true,
        showLongDescription: Boolean = false
    ) {
        val description = if(showLongDescription){
            if(!tvEvent.longDescription.isNullOrBlank()) tvEvent.longDescription!!
            else if(!tvEvent.shortDescription.isNullOrBlank()) tvEvent.shortDescription!!
            else ConfigStringsManager.getStringById("no_information")
        } else {
            if(!tvEvent.shortDescription.isNullOrBlank()) tvEvent.shortDescription!!
            else ConfigStringsManager.getStringById("no_information")
        }
        updateTitleAndDescription(
            title = tvEvent.name,
            description =  description,
            isDescriptionEnabled = isDescriptionEnabled
        )
    }

    protected fun updateTitleAndDescription(
        recording: Recording,
        isDescriptionEnabled: Boolean = true
    ) {
        var title = recording.name
        if (title.trim().isEmpty()) {
            title = "${recording.tvChannel?.name} - No info"
        }
        updateTitleAndDescription(
            title = title,
            description = recording.shortDescription.ifBlank { ConfigStringsManager.getStringById("no_information") },
            isDescriptionEnabled = isDescriptionEnabled
        )
    }

    protected fun updateTitleAndDescription(
        scheduledRecording: ScheduledRecording,
        isLongDescriptionRequested: Boolean = true, // used to distinguish whether short or long description should be displayed
        isDescriptionEnabled: Boolean = true
    ) {
        var title = scheduledRecording.name
        if (title.trim().isEmpty()) {
            title = ConfigStringsManager.getStringById("no_information")
        }

        var description: String? = null

        if (isLongDescriptionRequested) {
            description = scheduledRecording.tvEvent?.longDescription
        }

        if (description.isNullOrBlank()) { // this will be true in 2 cases: 1) if isLongDescriptionRequested is false, or 2) if longDescription is null or blank
            description = scheduledRecording.tvEvent?.shortDescription
        }

        if (description.isNullOrBlank()) {
            description = ConfigStringsManager.getStringById("no_information")
        }

        updateTitleAndDescription(
            title = title,
            description =  description,
            isDescriptionEnabled = isDescriptionEnabled
        )
    }

    protected fun updateTitleAndDescription(
        title: String,
        description: String,
        isDescriptionEnabled: Boolean = true
    ) {
        eventTitle.visibility = VISIBLE
        eventDescription.visibility =
            if (isDescriptionEnabled) VISIBLE else GONE // this is important for Zap Banner - there is no description
        eventTitle.text = title
        eventDescription.text = description

    }

    protected fun updateProgressBar(recording: Recording, playbackPosition: Long) {
        // progress bar is set differently than in TvEvent because we have additional request: if recording was watched - set progress where user last time stopped watching it.
        eventProgressBar.progress = Recording.calculateCurrentProgress(
            playbackPosition,
            recording.recordingDate,
            recording.recordingEndTime
        )
        eventProgressBar.visibility = VISIBLE
    }

    protected fun updateProgressBar(tvEvent: TvEvent, currentTime: Long) {
        eventProgressBar.apply {
            progress = TvEvent.calculateCurrentProgress(
                currentTime, tvEvent
            )
            post{
                visibility = VISIBLE
            }
        }
    }

    protected fun updateEventTitleAndDescriptionWhenNoInformation(isDescriptionEnabled: Boolean = true) {
        eventTitle.text = ConfigStringsManager.getStringById("no_information")
        eventDescription.text = ConfigStringsManager.getStringById("no_information")
        eventDescription.visibility = if (isDescriptionEnabled) VISIBLE else GONE
        eventProgressBar.visibility = View.VISIBLE
    }

    /**
     * Method have 2 important roles:
     *
     *      1) resets all views inside CustomDetails
     *      2) sets proper dimensions on CustomDetails main view and sometimes on some other views (for example in CustomDetailsDetailsScene where width of description is not the same as in others CustomDetails classes)
    used to reset all views inside CustomDetails
     */
    protected open fun resetData() {
        when (detailsType) {
            DetailsType.FOR_YOU -> {
                containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_FOR_YOU_WIDTH)
            }
            DetailsType.CHANNEL_LIST -> {
                containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_CHANNEL_LIST_WIDTH)
            }
            DetailsType.ZAP_BANNER -> {
                containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_ZAP_BANNER_WIDTH)
                eventTitle.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_25)
                )
            }
            DetailsType.INFO_BANNER -> {
                containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_INFO_BANNER_WIDTH)
            }
            DetailsType.DETAILS_SCENE -> {
                containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_DETAILS_SCENE_WIDTH)
                detailsScrollView.layoutParams.width = Utils.getDimensInPixelSize(R.dimen.custom_dim_441)
                detailsScrollView.layoutParams.height = Utils.getDimensInPixelSize(R.dimen.custom_dim_325)
            }

            DetailsType.EPG -> {
                containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_GUIDE)
                eventTitle.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_22)
                )
            }

            DetailsType.FAST_HOME -> {
                containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_FAST_HOME_WIDTH)
            }

            else -> {}
        }

        if (detailsType == DetailsType.DETAILS_SCENE) {
            infoRowLinearLayout.visibility = INVISIBLE // initially it is set to invisible in order to avoid blinking while changing margins which depends on fact whether image is loaded successfully or not
            infoRowLinearLayout.orientation = LinearLayout.VERTICAL
            (infoIconsLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
                bottomMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_15)
            }
            (audioTypeLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
                bottomMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_15)
            }
            (audioFormatLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
                bottomMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_15)
            }
            (audioTracksLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
                bottomMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_15)
            }
            (ccTracksLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
                bottomMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_15)
            }
            (subtitleTracksLinearLayout.layoutParams as LinearLayout.LayoutParams).apply {
                bottomMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_15)
            }
            (infoRowLinearLayout.layoutParams as LayoutParams).apply {
                topToTop = eventProgressBar.id
                startToStart = eventProgressBar.id
            }
            (infoRowLinearLayout.layoutParams as LayoutParams).apply {
                marginStart = Utils.getDimensInPixelSize(R.dimen.custom_dim_510)
            }
        }

        eventTitle.maxLines = 1
        eventDescription.maxLines = if (detailsType == DetailsType.EPG) 2 else if (detailsType == DetailsType.FOR_YOU) 4 else 3
        audioTypeLinearLayout.removeAllViews()
        audioTypeLinearLayout.visibility = GONE
        audioFormatLinearLayout.removeAllViews()
        audioFormatLinearLayout.visibility = GONE
        ccImageView.visibility = GONE
        ccTracksLinearLayout.visibility = GONE
        subtitlesAndCCImageView.visibility = GONE
        channelOrEventNameTextView.visibility = GONE
        channelOrEventNameTextView2.visibility = GONE
        channelLogoImageView.setImageResource(android.R.color.transparent)
        channelLogoImageView.visibility = GONE
        channelLogoImageView2.setImageResource(android.R.color.transparent)
        channelLogoImageView2.visibility = GONE
        subtitleTracksLinearLayout.visibility = GONE
        audioTracksLinearLayout.visibility = GONE
        eventDescription.text = ""
        eventDescription.visibility = GONE
        audioTracksLinearLayoutContainer.removeAllViews()
        subtitleTracksLinearLayoutContainer.removeAllViews()
        parentalTextView.text = ""
        parentalTextView.visibility = GONE
        adImageView.visibility = GONE
        sdImageView.visibility = GONE
        uhdImageView.visibility = GONE
        hdImageView.visibility = GONE
        fhdImageView.visibility = GONE
        dolbyImageView.visibility = GONE
        ttxImageView.visibility = GONE
        hohImageView.visibility = GONE
        eventStartEndTimeRow1.visibility = GONE
        eventStartEndTimeRow2.visibility = GONE
        eventStartEndTimeRow3.visibility = GONE
        parentalRatingLinearLayoutRow1.visibility = GONE
        parentalRatingLinearLayoutRow2.visibility = GONE
        parentalRatingLinearLayoutRow3.visibility = GONE
        upNextEventTitle.visibility = GONE
        if (detailsType == DetailsType.FAST_HOME) {
            eventProgressBar.visibility = GONE
        }
        hasSomethingInIconsRow.forEach {
            hasSomethingInIconsRow[it.key] = false
        }
        hasSomethingInAudioRow = false
    }


    /**
     * updates imageView's visibility with additional check whether channel is current one or not.
     *
     * @param shouldBeVisible if set to true there are two possible scenarios:
     * * 1) if isCurrentChannel is set to true - corresponding ImageView passed as parameter will be VISIBLE and HasMap will be updated with true for that ImageView (this is important after for audioTracks and subtitleTracks)
     * * 2) if isCurrentChannel is set to false - no matter what is passed for shouldBeVisible parameter, corresponding ImageView won't be visible and HashMap will be updated with false for that ImageView
     */
    private fun updateImageView(imageView: ImageView, shouldBeVisible: Boolean) {
        imageView.visibility = if (shouldBeVisible && isCurrentChannel) VISIBLE else GONE
        hasSomethingInIconsRow[imageView] = if (!isCurrentChannel) false else shouldBeVisible
    }


// PRIVATE METHODS
//--------------------------------------------------------------------------------------------------
// PUBLIC METHODS

    fun updateHohImageView(isHoh: Boolean) {
        updateImageView(
            imageView = hohImageView, shouldBeVisible = isHoh
        )
    }

    fun updateCcInfo(ccText: String?, isCCTrackAvailable: Boolean?) {
        subtitlesAndCCImageView.visibility = GONE
        ccTracksLinearLayoutContainer.removeAllViews()
        if (isCurrentChannel) {
            if (isCCTrackAvailable == true) ccImageView.visibility = VISIBLE
            ccTracksLinearLayout.visibility = VISIBLE
            updateImageView(subtitlesAndCCImageView, ccText != null)
            addNewTextViewInLayout(
                label = ccText ?: "", linearLayout = ccTracksLinearLayoutContainer
            )
        }
    }

    fun updateCcImageView(isCc: Boolean) {
        updateImageView(
            imageView = ccImageView, shouldBeVisible = isCc
        )
    }

    fun updateAdImageView(isAd: Boolean) {
        updateImageView(
            imageView = adImageView, shouldBeVisible = isAd
        )
    }

    fun updateTtxImageView(isTtx: Boolean) {
        updateImageView(
            imageView = ttxImageView, shouldBeVisible = isTtx
        )
    }

    fun updateDolbyImageView(isDolby: Boolean) {
        updateImageView(
            imageView = dolbyImageView, shouldBeVisible = isDolby
        )
    }


    // for now used only in ReferenceWidgetChannelList
    fun updateVideoQuality(tvChannel: TvChannel, videoResolution: String, isFromRecording:Boolean = false) {
        uhdImageView.visibility = GONE
        hasSomethingInIconsRow[uhdImageView] = false
        hdImageView.visibility = GONE
        hasSomethingInIconsRow[hdImageView] = false
        sdImageView.visibility = GONE
        hasSomethingInIconsRow[sdImageView] = false
        fhdImageView.visibility = GONE
        hasSomethingInIconsRow[fhdImageView] = false
        if (!isCurrentChannel && !isFromRecording) return
        var resolution: VideoResolution? = null
        if (BuildConfig.FLAVOR.contains("mtk")) {
            videoResolution.let {
                val inputResolution = it.split(" ")
                if (inputResolution.size > 1) {
                    if (inputResolution[1] == "UHD")
                        resolution = VideoResolution.VIDEO_RESOLUTION_UHD
                    else if (inputResolution[1] == "HD")
                        if(inputResolution[0] == "1080p" || inputResolution[0] == "1080i"){
                            resolution = VideoResolution.VIDEO_RESOLUTION_FHD
                        }else{
                            resolution = VideoResolution.VIDEO_RESOLUTION_HD
                        }
                    else if (inputResolution[1] == "FHD")
                        resolution = VideoResolution.VIDEO_RESOLUTION_FHD
                    else if (inputResolution[1] == "ED")
                        resolution = VideoResolution.VIDEO_RESOLUTION_ED
                    else if (inputResolution[1] == "SD")
                        resolution = VideoResolution.VIDEO_RESOLUTION_SD
                }
            }

        }
        else if (videoResolution.isNotEmpty()
            && (BuildConfig.FLAVOR.contains("refplus5") || BuildConfig.FLAVOR.contains("rtk"))) {
            if (videoResolution == "UHD")
                resolution = VideoResolution.VIDEO_RESOLUTION_UHD
            else if (videoResolution == "HD") {
                if(videoResolution == "1080p" || videoResolution == "1080i"){
                    resolution = VideoResolution.VIDEO_RESOLUTION_FHD
                }else{
                    resolution = VideoResolution.VIDEO_RESOLUTION_HD
                }
            }
            else if (videoResolution == "FHD")
                resolution = VideoResolution.VIDEO_RESOLUTION_FHD
            else if (videoResolution == "ED")
                resolution = VideoResolution.VIDEO_RESOLUTION_ED
            else if (videoResolution == "SD")
                resolution = VideoResolution.VIDEO_RESOLUTION_SD
        }
        else if (tvChannel.videoQuality.isNotEmpty()) { // when not mtk flavor
            resolution = tvChannel.videoQuality[0]
        }
        when (resolution) {
            VideoResolution.VIDEO_RESOLUTION_UHD -> {
                uhdImageView.visibility = VISIBLE
                hdImageView.visibility = GONE
                sdImageView.visibility = GONE
                fhdImageView.visibility = GONE
                hasSomethingInIconsRow[uhdImageView] = true
            }

            VideoResolution.VIDEO_RESOLUTION_FHD -> {
                fhdImageView.visibility = VISIBLE
                sdImageView.visibility = GONE
                uhdImageView.visibility = GONE
                hdImageView.visibility = GONE
                hasSomethingInIconsRow[fhdImageView] = true
            }
            VideoResolution.VIDEO_RESOLUTION_HD -> {
                hdImageView.visibility = VISIBLE
                sdImageView.visibility = GONE
                uhdImageView.visibility = GONE
                fhdImageView.visibility = GONE
                hasSomethingInIconsRow[hdImageView] = true
            }

            VideoResolution.VIDEO_RESOLUTION_SD, VideoResolution.VIDEO_RESOLUTION_ED -> {
                sdImageView.visibility = VISIBLE
                hdImageView.visibility = GONE
                uhdImageView.visibility = GONE
                fhdImageView.visibility = GONE
                hasSomethingInIconsRow[sdImageView] = true
            }
            null -> {}
        }
    }

    fun updateAudioChannelInfo(audioChannel: String) {
        if (isCurrentChannel.not()) return // if not current channel audioChannelInfo can't be updated.

        if (audioChannel.isBlank()) return // if there is no text which should be set - exit this method

        audioTypeLinearLayout.removeAllViews() //removing previous audio type view while updating new one
        addNewTextViewInLayout(audioChannel, audioTypeLinearLayout)
        audioTypeLinearLayout.visibility = VISIBLE
    }

    fun updateAudioChannelFormatInfo(audioFormatChannel: String){
        if (isCurrentChannel.not()) return // if not current channel audioChannelInfo can't be updated.

        if (audioFormatChannel.isBlank()) return // if there is no text which should be set - exit this method

        audioFormatLinearLayout.removeAllViews() //removing previous audio type view while updating new one
        addNewTextViewInLayout(audioFormatChannel, audioFormatLinearLayout)
        audioFormatLinearLayout.visibility = VISIBLE
    }

    /**
     * Updates the marquee selection of the event title.
     *
     * This method controls the marquee animation of the event title,
     * allowing to start or stop the marquee effect based on the given selection state.
     *
     * @param isSelected True to start the marquee animation, false to stop it.
     */
    fun updateEventTitleMarqueeSelection(isSelected: Boolean) {

        if(!this::runnableStartEventTitleMarque.isInitialized){
            runnableStartEventTitleMarque = Runnable {
                eventTitle.isSelected = true
            }
        }

        removeCallbacks(runnableStartEventTitleMarque)

        eventTitle.isSelected = false

        if (isSelected) {
            postDelayed(runnableStartEventTitleMarque, 1500)
        }
    }

    /**
     * Performs actions based on the lock status of a [TvEvent], considering parental restrictions.
     *
     * This function checks whether a TV event is subject to parental restrictions. If the event is locked, it updates
     * the title and description to indicate parental control restrictions.
     * If the event is not locked, and it's not NULL [onNotLockedOrNull] executes, allowing for additional custom actions.
     *
     * @param tvEvent The [TvEvent] to be checked for parental restrictions.
     * @param isEventLocked Boolean flag indicating whether the event is locked.
     * @param isDescriptionEnabled Boolean flag indicating whether to consider description updates.
     * @param onNotLockedOrNull Lambda function to be executed when the event is not locked and not null.
     *                          It receives the TvEvent as a parameter for custom processing.
     *
     * Usage Example:
     * ```
     * val event: TvEvent? = // obtain a TV event
     * doIfEventIsNotLockedOrNull(
     *     tvEvent = event,
     *     isEventLocked = isLockedFunction(event),
     *     isDescriptionEnabled = true,
     *     onNotLockedOrNull = { unlockedEvent ->
     *         // Custom logic to be executed when the event is not locked
     *         // The unlocked TV event is available as 'unlockedEvent'
     *     }
     * )
     * ```
     */
    protected fun doIfEventIsNotLockedOrNull(
        tvEvent: TvEvent?,
        isEventLocked: Boolean,
        isDescriptionEnabled: Boolean = true, // this is important for Zap Banner - there is no description
        onNotLockedOrNull: (TvEvent) -> Unit
    ) {
        if (tvEvent == null) {
            updateEventTitleAndDescriptionWhenNoInformation()
            eventProgressBar.visibility = GONE
        }
        else if (isEventLocked) {
            updateTitleAndDescription(
                title = ConfigStringsManager.getStringById("parental_control_restriction"),
                description = ConfigStringsManager.getStringById("parental_control_restriction"),
                isDescriptionEnabled = isDescriptionEnabled
            )
            timeAndParentalContainerLinearLayout.visibility = GONE
            timeAndParentalContainerLinearLayout2.visibility = GONE
            eventProgressBar.visibility = GONE
        }
        else { /*event is NOT locked*/
            onNotLockedOrNull.invoke(tvEvent)
        }
    }

    class CustomDetailsChannelList : CustomDetails {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context,
            attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.CHANNEL_LIST
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            channelListItem: ChannelListItem,
            videoResolution: String,
            onDataUpdated: () -> Unit = {},
            parentalRatingDisplayName: String,
            currentTime: Long,
            isCCTrackAvailable: Boolean,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            resetData()
            this.isCurrentChannel = channelListItem.isCurrentChannel

            doIfEventIsNotLockedOrNull(
                tvEvent = channelListItem.event,
                isEventLocked = isEventLocked
            ){ tvEvent ->
                if (this.isCurrentChannel && isCCTrackAvailable) ccImageView.visibility = VISIBLE

                updateProgressBar(tvEvent, currentTime)
                updateTitleAndDescription(tvEvent)
                updateVideoQuality(tvEvent.tvChannel, videoResolution)

                // hiding or displaying row with parental restrictions
                parentalTextView.visibility = if (parentalTextView.text.isNotBlank()) VISIBLE else GONE
            }
            channelListItem.event?.let {
                updateTimeTextView(it, dateTimeFormat)
                updateParentalRating(it, parentalRatingDisplayName)
            }

            onDataUpdated.invoke()
        }
    }

    class CustomDetailsSearch : CustomDetails {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context, attrs
        ) // this constructor is important when View is used in XML files.

        private var isPromo: Boolean = false

        init {
            detailsType = DetailsType.FOR_YOU
            eventTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_16)
            )
        }

        fun updateData( // this will be used for VOD item which doesn't contain TvEvent
            title: String,
            description: String,
            isLocked: Boolean
        ) {
            resetData()

            eventProgressBar.visibility = GONE

            updateTitleAndDescription(
                title = title,
                description = description
            )
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent?,
            parentalRatingDisplayName: String,
            railItem: RailItem,
            currentTime: Long,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            this.isPromo = railItem.railName == ConfigStringsManager.getStringById("for_you_promo")

            resetData()

            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ){ tvEvent ->
                //Event type APP_LINK case
                if (isPromo) {
                    if (tvEvent.tvChannel.appLinkText.isNotBlank()) {
                        if (isEventLocked) {
                            eventTitle.text =
                                ConfigStringsManager.getStringById("parental_control_restriction")
                            eventDescription.setTextColor(colorProgress)
                            eventTitle.setTextColor(colorProgress)
                        } else {
                            eventTitle.text = tvEvent.name
                        }
                        eventTitle.visibility = VISIBLE
                        eventProgressBar.visibility = GONE
                        eventStartEndTimeRow1.visibility = GONE
                        return@doIfEventIsNotLockedOrNull // DO NOT UPDATE OTHER THINGS IF IT IS PROMO
                    }
                }

                updateTitleAndDescription(tvEvent)

                if (railItem.railName != ConfigStringsManager.getStringById("past_events")) {
                    updateProgressBar(tvEvent, currentTime)
                }
            }

            if (tvEvent == null) return

            updateTimeTextView(tvEvent, dateTimeFormat)
            updateParentalRating(tvEvent, parentalRatingDisplayName)

            when (railItem.type) {
                RailItem.RailItemType.EVENT -> {
                    channelIndexTextView.apply {
                        text = tvEvent.tvChannel.getDisplayNumberText()
                        visibility = INVISIBLE
                    }
                    insertImageInChannelLogoImageView(
                        tvChannel = tvEvent.tvChannel
                    )
                }

                RailItem.RailItemType.CHANNEL -> {
                    channelIndexTextView.text = ""
                }

                RailItem.RailItemType.RECORDING -> {
                    channelIndexTextView.apply {
                        text = tvEvent.tvChannel.getDisplayNumberText()
                        visibility = INVISIBLE
                    }
                    insertImageInChannelLogoImageView(
                        tvChannel = tvEvent.tvChannel
                    )
                }

                RailItem.RailItemType.SCHEDULED_RECORDING -> {
                    insertImageInChannelLogoImageView(
                        tvChannel = tvEvent.tvChannel
                    )
                    channelIndexTextView.apply {
                        text = tvEvent.tvChannel.getDisplayNumberText()
                        visibility = INVISIBLE
                    }
                }

                RailItem.RailItemType.VOD -> {
                    // TODO BORIS: handle this for VODItem
                }

                RailItem.RailItemType.BROADCAST_CHANNEL -> {
                    // No details for broadcast channel
                }
            }
        }

        /**
         * used to animate CustomDetail's visibility.
         *
         * Example of usage: in ForYou, Search or Recording, when focus is not on Rail - CustomDetails is not visible,
         * animation is enable which will collapse CustomDetails, but this method is important for implementing
         * fade effect which will appear at the same with CustomDetail's content height animating.
         */
        fun animateVisibility(shouldBeVisible: Boolean, duration: Long) {
            val fadeInAnimation = AlphaAnimation(
                if (shouldBeVisible) 0f else 1f,
                if (shouldBeVisible) 1f else 0f
            )
            fadeInAnimation.duration = duration
            this.startAnimation(fadeInAnimation)
        }
    }

    class CustomDetailsZapBanner : CustomDetails {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context, attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.ZAP_BANNER
            this.isCurrentChannel = true // whenever zap opens it shows info about current channel
            channelOrEventNameTextView.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
            upNextEventTitle.visibility = View.VISIBLE
            detailsScrollView.visibility = GONE // description with scroll view should never be visible in Fast zap banner, if not set to GONE additional space will be present and info icons would be separated too much from progress bar
            channelOrEventNameTextView.setTextColor(colorMainText)
            eventStartEndTimeRow1.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
        }

        public override fun resetData() {
            super.resetData()
            // TODO BORIS comment left for the future reference: this should not be done in this way.
            // In the future: consider changing logic in resetData() method - do not hide eventProgressBar in this method.
            // progressBar should be visible almost always, so the better solution is to hide it when necessary - not always...
            eventProgressBar.visibility = VISIBLE
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent,
            parentalRatingDisplayName: String,
            currentTime: Long,
            isCCTrackAvailable: Boolean?,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked,
                isDescriptionEnabled = false
            ){tvEvent ->
                updateTitleAndDescription(
                    tvEvent = tvEvent,
                    isDescriptionEnabled = false
                ) // isDescriptionEnabled must be set to false here to avoid displaying it in Zap Banner

                updateProgressBar(tvEvent, currentTime)
                if (tvEvent.id == TvEvent.DUMMY_EVENT_ID) { // this is important for FAST channels
                    updateEventTitleAndDescriptionWhenNoInformation(isDescriptionEnabled = false)
                }

                if (isCCTrackAvailable == true) ccImageView.visibility = VISIBLE
            }
            updateTimeTextView(tvEvent, dateTimeFormat)
            updateParentalRating(tvEvent, parentalRatingDisplayName)
        }

        fun updateUpNextEvent(tvEvent: TvEvent){
                ReferenceApplication.runOnUiThread{
                    upNextEventTitle.visibility = VISIBLE
                    upNextEventTitle.text = "${ConfigStringsManager.getStringById("up_next")} :  ${tvEvent.name}"
                }

        }

        fun updateCCData(isCCTrackAvailable: Boolean?) {
            if((isCCTrackAvailable != null) && isCCTrackAvailable)
            {
                ccImageView.visibility = VISIBLE
            } else {
                ccImageView.visibility = GONE
            }
        }
    }

    class CustomDetailsFastEpg : CustomDetails {

        interface VisibilityListener {
            fun onVisibilityChange(isVisible: Boolean, shouldAnimate: Boolean)
        }

        var visibilityListener: VisibilityListener? = null
        private var visibilityStatus = false
        private var visibilityAnimator : Animator? = null

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context, attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.FAST_EPG
            containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_FAST_EPG_WIDTH)
            containerLinearLayout.layoutParams.height = Utils.getDimensInPixelSize(R.dimen.custom_dim_1)
            (findViewById<ConstraintLayout>(R.id.first_row).layoutParams as LayoutParams).topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_1)
            translationY = -Utils.getDimensInPixelSize(R.dimen.custom_dim_1).toFloat()
            // here topMargin added as 1dp to fix blinking issue while closing, to balance the alignment translationY added as -1dp to its parent
            eventTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_22)
            )
        // dimension must be set to 1 instead of 0, if set to 0 as it is logical, animation for EPG
        // is not working properly, if changing this in the future pay attention to functionality of
        // that animation
        }

        /**
         * Updates the audio tracks based on the provided list and the active audio track.
         *
         * If the list of audio tracks is null or empty, no action is taken. Otherwise, the
         * audio tracks will be updated.
         *
         * Note: Audio tracks will be displayed if there are more than one available.
         *
         * @param audioTracks A list of available audio tracks.
         * @param activeAudioTrack The currently active audio track (or null if none is active).
         */
        override fun updateAudioTracks(
            audioTracks: List<IAudioTrack>?,
            activeAudioTrack: IAudioTrack?
        ) {
            if (audioTracks.isNullOrEmpty() || audioTracks.size == 1) return
            super.updateAudioTracks(audioTracks, activeAudioTrack)
        }


        /**
         * [animateVisibility] is used to animate [CustomDetailsEPG]'s visibility.
         *
         * Example of usage: in FastLiveTab (Fast EPG), [CustomDetailsEPG] is not visible initially.
         * When user navigates to an event [CustomDetailsEPG] becomes visible, but it's height is set to
         * 1 dp initially in order to be collapsed until animation is triggered.
         *
         * [animateVisibility] automatically handles:
         *    1) View.VISIBILITY property
         *    2) view's height
         * in order to have smooth animation of both those properties.
         */
        fun animateVisibility(shouldBeVisible: Boolean, shouldAnimate: Boolean = true) {

            if (visibilityStatus==shouldBeVisible) return

            visibilityStatus = shouldBeVisible
            visibilityListener!!.onVisibilityChange(shouldBeVisible, shouldAnimate)

            visibilityAnimator?.cancel()
            clearAnimation()

            if(!shouldAnimate){
                alpha = if (shouldBeVisible) 1f else 0f
                visibility = if (shouldBeVisible) VISIBLE else INVISIBLE
                containerLinearLayout.layoutParams.height = Utils.getDimensInPixelSize(if (shouldBeVisible) R.dimen.custom_dim_140 else R.dimen.custom_dim_1)
                containerLinearLayout.requestLayout()
                return
            }

            val fadeAnimation = AlphaAnimation(
                if (shouldBeVisible) 0f else 1f,
                if (shouldBeVisible) 1f else 0f
            )
            fadeAnimation.duration = 500L
            fadeAnimation.setAnimationListener(object : Animation.AnimationListener{
                override fun onAnimationStart(p0: Animation?) {
                    if(shouldBeVisible) visibility = VISIBLE
                }

                override fun onAnimationEnd(p0: Animation?) {
                    visibility = if (shouldBeVisible) VISIBLE else INVISIBLE
                }

                override fun onAnimationRepeat(p0: Animation?) {
                    TODO("Not yet implemented")
                }
            })
            this.startAnimation(fadeAnimation)

            visibilityAnimator = Utils.animateContentHeight(
                view = containerLinearLayout,
                containerLinearLayout.layoutParams.height,

                Utils.getDimensInPixelSize(
                    if (shouldBeVisible) R.dimen.custom_dim_140 else R.dimen.custom_dim_1)
                // height is set to 1 instead of 0 because animation in EPG for customDetails won't
                // work properly otherwise
                ,500L
            )
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent?,
            parentalRatingDisplayName: String,
            currentTime: Long,
            audioTracks: List<IAudioTrack>?,
            subtitleTracks: List<ISubtitle>?,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            resetData()

            textToSpeechHandler.setSpeechText(
                tvEvent?.name ?: ConfigStringsManager.getStringById("no_information"),
                tvEvent?.shortDescription ?: ConfigStringsManager.getStringById("no_information")
            )

            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ){ tvEvent ->
                containerLinearLayout.requestLayout()
                isCurrentChannel = true

                updateProgressBar(tvEvent, currentTime)
                updateTitleAndDescription(tvEvent = tvEvent)
                if (tvEvent.tvChannel.videoQuality.isNotEmpty()) {
                    updateVideoQuality(tvEvent.tvChannel.videoQuality[0])
                }
                ccImageView.visibility = GONE
                updateAudioTracks(audioTracks)
                updateSubtitleTracks(subtitleTracks)
            }

            tvEvent?.let {
                updateTimeTextView(it, dateTimeFormat)
                updateParentalRating(it, parentalRatingDisplayName)
            }
        }

        private fun updateVideoQuality(resolution: VideoResolution) {
            when (resolution) {
                VideoResolution.VIDEO_RESOLUTION_UHD -> {
                    uhdImageView.visibility = VISIBLE
                    hdImageView.visibility = GONE
                    sdImageView.visibility = GONE
                    fhdImageView.visibility = GONE
                    hasSomethingInIconsRow[uhdImageView] = true
                }

                VideoResolution.VIDEO_RESOLUTION_FHD -> {
                    fhdImageView.visibility = VISIBLE
                    sdImageView.visibility = GONE
                    uhdImageView.visibility = GONE
                    hdImageView.visibility = GONE
                    hasSomethingInIconsRow[fhdImageView] = true
                }

                VideoResolution.VIDEO_RESOLUTION_HD -> {
                    hdImageView.visibility = VISIBLE
                    sdImageView.visibility = GONE
                    uhdImageView.visibility = GONE
                    fhdImageView.visibility = GONE
                    hasSomethingInIconsRow[hdImageView] = true
                }

                VideoResolution.VIDEO_RESOLUTION_SD, VideoResolution.VIDEO_RESOLUTION_ED -> {
                    sdImageView.visibility = VISIBLE
                    hdImageView.visibility = GONE
                    uhdImageView.visibility = GONE
                    fhdImageView.visibility = GONE
                    hasSomethingInIconsRow[sdImageView] = true
                }

                null -> {}
            }
        }
    }

    class CustomDetailsInfoBanner : CustomDetails {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context, attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.INFO_BANNER
            eventTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_22)
            )
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateDataForRecording(tvEvent: TvEvent, dateTimeFormat: DateTimeFormat) {
            resetData()
            updateTimeTextView(tvEvent, dateTimeFormat)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent?,
            parentalRatingDisplayName: String,
            isCurrentChannel: Boolean,
            onPastEvent: () -> Unit,
            onFutureEvent: () -> Unit,
            onCurrentEvent: () -> Unit,
            currentTime: Long,
            isCCTrackAvailable: Boolean?,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            resetData()

            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ) { tvEvent ->

                this.isCurrentChannel = isCurrentChannel
                updateTitleAndDescription(tvEvent = tvEvent)
                updateProgressBar(tvEvent, currentTime)
                eventProgressBar.visibility = View.VISIBLE
            }

            tvEvent?.let {tvEvent ->
                updateParentalRating(tvEvent, parentalRatingDisplayName)
                updateTimeTextView(tvEvent, dateTimeFormat)
                if (!tvEvent.genre.isNullOrEmpty()) {
                    eventGenreTextView.apply {
                        text = tvEvent.genre
                        visibility = VISIBLE
                    }
                    separatorView.visibility = VISIBLE
                } else {
                    eventGenreTextView.visibility = GONE
                    separatorView.visibility = GONE
                }
                // Past event
                if (tvEvent.endTime < currentTime) {
                    onPastEvent.invoke()
                }
                // Future event
                else if (tvEvent.startTime > currentTime) {
                    onFutureEvent.invoke()
                }
                // Current event
                else if (tvEvent.startTime < currentTime && tvEvent.endTime > currentTime) {
                    onCurrentEvent.invoke()
                    if (this.isCurrentChannel && isCCTrackAvailable == true) ccImageView.visibility =
                        VISIBLE
                }

                    (timeAndParentalContainerLinearLayout2.layoutParams as LayoutParams).apply {
                        topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_0_5)
                    }
                    eventDescription.setLineSpacing(ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_5), 1.0f);

                    timeAndParentalContainerLinearLayout2.visibility = VISIBLE

            }

        }
    }

    class CustomDetailsDetailsScene : CustomDetails {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context, attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.DETAILS_SCENE
            eventTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_22)
            )
            setMarginsOnInfoRow(false)
        }

        @SuppressLint("SetTextI18n")
        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent?,
            isCurrentChannel: Boolean,
            parentalRatingDisplayName: String,
            currentTime: Long,
            isCCTrackAvailable: Boolean,
            dateTimeFormat: DateTimeFormat
        ) {
            super.resetData()
            if (tvEvent == null) {
                updateEventTitleAndDescriptionWhenNoInformation()
            } else {
                this.isCurrentChannel = isCurrentChannel
                updateTitleAndDescription(tvEvent = tvEvent, showLongDescription = true)

                if (!tvEvent.genre.isNullOrEmpty()) {
                    eventGenreTextView.apply {
                        text = tvEvent.genre
                        visibility = VISIBLE
                    }
                    separatorView.visibility = VISIBLE
                } else {
                    eventGenreTextView.visibility = GONE
                    separatorView.visibility = GONE
                }

                updateTimeTextView(tvEvent, dateTimeFormat)
                updateProgressBar(tvEvent, currentTime)
                eventProgressBar.visibility = View.VISIBLE
                if (tvEvent.id == TvEvent.DUMMY_EVENT_ID) { // this is important for FAST channels
                    updateEventTitleAndDescriptionWhenNoInformation(isDescriptionEnabled = false)
                }
                eventDescription.maxLines = Int.MAX_VALUE
                updateParentalRating(tvEvent, parentalRatingDisplayName)
                if (isCCTrackAvailable && this.isCurrentChannel && tvEvent.startTime < currentTime && tvEvent.endTime > currentTime) {
                    ccImageView.visibility = VISIBLE
                }


                updateEventTitleMarqueeSelection(true)

                    channelIndexTextView2.apply {
                        text = tvEvent.tvChannel.getDisplayNumberText()
                        visibility = VISIBLE
                    }
                    insertImageInChannelLogoImageView(
                        tvEvent.tvChannel,
                        channelOrEventNameTextView2,
                        channelLogoImageView2)

                    timeAndParentalContainerLinearLayout2.visibility = VISIBLE
            }
        }

        /**
         * method used to set margins of InfoRow depending on fact is image loaded.
         *
         * @param hasImageLoadingSucceed if:

         * * true - means that image is loaded, thus InfoRow should be moved beneath CardView which holds image.
         * * false - means that image is NOT loaded, thus InfoRow should be translated up.
         *
         * this method is called from DetailsScene as a callback which is passed in CustomCard updateData() method,
         * and this callback is moved down to the method that inserts image in CardView, and depending on 
         */
        fun setMarginsOnInfoRow(hasImageLoadingSucceed: Boolean) {
            if (hasImageLoadingSucceed) {
                (infoRowLinearLayout.layoutParams as LayoutParams).apply {
                    topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_206_5)
                }
            }else{
                (infoRowLinearLayout.layoutParams as LayoutParams).apply {
                    topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_68)
                }
            }
            infoRowLinearLayout.visibility = VISIBLE
        }


        // TODO - check why this method is used. It is called from refresh() in Details Scene when string is passed to method as parameter, but not sure why would only title be changed.
        //  this method was created to have behavior as before, but if possible this method should be removed
        fun updateTitle(
            title: String
        ){
            eventTitle.text = title
        }

        fun updateData(
            recording: Recording,
            playbackPosition: Long
        ) {
            super.resetData()
            updateTitleAndDescription(recording)
            updateTimeTextView(recording)
            updateProgressBar(recording, playbackPosition)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            scheduledRecording: ScheduledRecording,
            currentTime: Long,
            dateTimeFormat: DateTimeFormat
        ) {
            super.resetData()
                updateTimeTextView(scheduledRecording.tvEvent!!, dateTimeFormat)
                timeAndParentalContainerLinearLayout2.visibility = VISIBLE


            updateEventTitleMarqueeSelection(true)

            updateTitleAndDescription(
                scheduledRecording = scheduledRecording,
                isLongDescriptionRequested = true // if there is long description - show it because this is call from Details Scene
            )
            updateProgressBar(scheduledRecording.tvEvent!!, currentTime)
        }

        fun scrollUp() {
            detailsScrollView.smoothScrollBy(0, -25)
        }

        fun scrollDown() {
            detailsScrollView.smoothScrollBy(0, 25)
        }
    }

    class CustomDetailsEPG : CustomDetails {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context,
            attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.EPG
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent,
            isCurrentChannel: Boolean,
            parentalRatingDisplayName: String,
            isCCTrackAvailable: Boolean?,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            resetData()

            textToSpeechHandler.setSpeechText(
                tvEvent.name,
                tvEvent.shortDescription ?: ConfigStringsManager.getStringById("no_information"),
                importance = SpeechText.Importance.HIGH
            )

            this.isCurrentChannel = isCurrentChannel // crucial for displaying icons (AD, HOH..)
            if (!tvEvent.genre.isNullOrEmpty()) {
                eventGenreTextView.apply {
                    text = tvEvent.genre
                    visibility = VISIBLE
                }
                separatorView.visibility = VISIBLE
            } else {
                eventGenreTextView.visibility = GONE
                separatorView.visibility = GONE
            }
            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ) {tvEvent ->
                updateTitleAndDescription(tvEvent = tvEvent)
                if (isCCTrackAvailable == true) ccImageView.visibility = VISIBLE
            }

            timeAndParentalContainerLinearLayout2.visibility = VISIBLE
            updateEventTitleMarqueeSelection(true)


            updateParentalRating(
                tvEvent = tvEvent,
                parentalRatingDisplayName = parentalRatingDisplayName
            )
            updateTimeTextView(tvEvent, dateTimeFormat)
        }
    }

    /**
     * [CustomDetailsFastHome] is primary used in the Home Tab to collect and hold data from the chosen Card in the Rail.
     */
    class CustomDetailsFastHome : CustomDetails {
        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context,
            attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.FAST_HOME
            eventTitle.setTextColor(Color.parseColor("#f8f9fa")) // TODO BORIS this color is new, handle it somehow...
            eventTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_30)
            )
            eventDescription.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_15)
            )
            eventStartEndTimeRow2.setPadding(
                /*left*/   Utils.getDimensInPixelSize(R.dimen.custom_dim_0),
                /*top*/    eventStartEndTimeRow2.paddingTop,
                /*right*/  eventStartEndTimeRow2.paddingRight,
                /*bottom*/ eventStartEndTimeRow2.paddingBottom
            )
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent,
            railItem: RailItem,
            parentalRatingDisplayName: String,
            currentTime: Long,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            resetData()

            if (tvEvent.startTime <= currentTime && tvEvent.endTime >= currentTime) {
                eventProgressBar.visibility = VISIBLE
            }
            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ) {tvEvent ->
                // if event is not locked:
                updateTitleAndDescription(tvEvent = tvEvent)
            }

            if (!tvEvent.isGuideCardEvent()) {
                updateParentalRating(
                    tvEvent = tvEvent,
                    parentalRatingDisplayName = parentalRatingDisplayName
                )
                updateTimeTextView(tvEvent, dateTimeFormat)
                timeAndParentalContainerLinearLayout.visibility = VISIBLE
            }else{
                timeAndParentalContainerLinearLayout.visibility = GONE
            }

            if (railItem.railName == ConfigStringsManager.getStringById("on_now")) {
                updateProgressBar(tvEvent, currentTime)
                containerLinearLayout.layoutParams.height =
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_200)
            }

            if (!tvEvent.tvChannel.isFastChannel()) {
                channelIndexTextView.apply {
                    text = tvEvent.tvChannel.getDisplayNumberText()
                    visibility = VISIBLE
                }
                insertImageInChannelLogoImageView(tvChannel = tvEvent.tvChannel)
            } else channelIndexTextView.visibility = GONE
        }
    }

    class CustomDetailsZapBannerFast : CustomDetails {

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(
            context, attrs
        ) // this constructor is important when View is used in XML files.

        init {
            detailsType = DetailsType.ZAP_BANNER_FAST //important for reset() method.
            this.isCurrentChannel = true // whenever zap opens it shows info about current channel

            detailsScrollView.visibility = GONE // description with scroll view should never be visible in Fast zap banner, if not set to GONE additional space will be present and info icons would be separated too much from progress bar

            containerLinearLayout.layoutParams.width = Utils.getDimensInPixelSize(CUSTOM_DETAILS_ZAP_BANNER_FAST_WIDTH)
            eventTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_30)
            )
            eventTitle.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
            channelOrEventNameTextView.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )
            channelOrEventNameTextView.setTextColor(colorMainText)
            eventStartEndTimeRow1.typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
            eventStartEndTimeRow1.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_selector")))

            updateEventTitleMarqueeSelection(true)

            ccImageView.visibility = GONE
        }

        /**
         * Updates the audio tracks based on the provided list and the active audio track.
         *
         * If the list of audio tracks is null or empty, no action is taken. Otherwise, the
         * audio tracks will be updated.
         *
         * Note: Audio tracks will be displayed if there are more than one available.
         *
         * @param audioTracks A list of available audio tracks.
         * @param activeAudioTrack The currently active audio track (or null if none is active).
         */
        override fun updateAudioTracks(
            audioTracks: List<IAudioTrack>?,
            activeAudioTrack: IAudioTrack?
        ) {
            if (audioTracks.isNullOrEmpty() || audioTracks.size == 1) return
            super.updateAudioTracks(audioTracks, activeAudioTrack)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun updateData(
            tvEvent: TvEvent,
            parentalRatingDisplayName: String,
            currentTime: Long,
            dateTimeFormat: DateTimeFormat,
            isEventLocked: Boolean
        ) {
            super.resetData()

            doIfEventIsNotLockedOrNull(
                tvEvent = tvEvent,
                isEventLocked = isEventLocked
            ) {tvEvent ->
                updateTitleAndDescription(
                    tvEvent = tvEvent,
                    isDescriptionEnabled = false // isDescriptionEnabled must be set to false here to avoid displaying it in Zap Banner
                )
                updateProgressBar(tvEvent, currentTime)

                if (tvEvent.id == TvEvent.DUMMY_EVENT_ID) { // this is important for FAST channels
                    updateEventTitleAndDescriptionWhenNoInformation(isDescriptionEnabled = false)
                }

                textToSpeechHandler.setSpeechText(
                    tvEvent.name
                )

            }

            updateTimeTextView(tvEvent, dateTimeFormat)
            updateParentalRating(tvEvent, parentalRatingDisplayName)
        }

        fun updateVideoQuality(videoResolution: VideoResolution) {
            uhdImageView.visibility = GONE
            hasSomethingInIconsRow[uhdImageView] = false
            hdImageView.visibility = GONE
            hasSomethingInIconsRow[hdImageView] = false
            sdImageView.visibility = GONE
            hasSomethingInIconsRow[sdImageView] = false
            fhdImageView.visibility = GONE
            hasSomethingInIconsRow[fhdImageView] = false

            when (videoResolution) {
                VideoResolution.VIDEO_RESOLUTION_UHD -> {
                    uhdImageView.visibility = VISIBLE
                    hdImageView.visibility = GONE
                    sdImageView.visibility = GONE
                    fhdImageView.visibility = GONE
                    hasSomethingInIconsRow[uhdImageView] = true
                }

                VideoResolution.VIDEO_RESOLUTION_FHD -> {
                    fhdImageView.visibility = VISIBLE
                    sdImageView.visibility = GONE
                    uhdImageView.visibility = GONE
                    hdImageView.visibility = GONE
                    hasSomethingInIconsRow[fhdImageView] = true
                }
                VideoResolution.VIDEO_RESOLUTION_HD -> {
                    hdImageView.visibility = VISIBLE
                    sdImageView.visibility = GONE
                    uhdImageView.visibility = GONE
                    fhdImageView.visibility = GONE
                    hasSomethingInIconsRow[hdImageView] = true
                }

                VideoResolution.VIDEO_RESOLUTION_SD, VideoResolution.VIDEO_RESOLUTION_ED -> {
                    sdImageView.visibility = VISIBLE
                    hdImageView.visibility = GONE
                    uhdImageView.visibility = GONE
                    fhdImageView.visibility = GONE
                    hasSomethingInIconsRow[sdImageView] = true
                }
                null -> {}
            }
        }
    }

}

enum class TrackType {
    AUDIO, SUBTITLE
}

enum class DetailsType {
    FOR_YOU,
    CHANNEL_LIST,
    ZAP_BANNER,
    INFO_BANNER,
    DETAILS_SCENE,
    EPG,
    ZAP_BANNER_FAST,
    FAST_EPG,
    FAST_HOME, // used in FastHomeData for event's details
}