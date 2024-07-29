package com.iwedia.cltv.scene.home_scene.guide

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.CategoryAdapter
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.CategoryItemViewHolder
import com.iwedia.cltv.components.MultiCheckListAdapter
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.FilterItem
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.*
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.UtilsInterface.CountryPreference.*
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.scene.channel_list.ChannelListSortAdapter
import com.iwedia.cltv.scene.home_scene.HomeSceneBase
import com.iwedia.cltv.utils.LoadingPlaceholder
import com.iwedia.cltv.utils.PlaceholderName
import com.iwedia.cltv.utils.Utils
import com.iwedia.cltv.utils.Utils.Companion.checkIfDigit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


/**
 * Guide scene widget
 *
 * @author Dejan Nadj
 */
class HorizontalGuideSceneWidget(context: Context, listener: GuideSceneWidgetListener) :
    GuideSceneWidget(context, listener) {

    //Scene layout views references
    val TAG = javaClass.simpleName
    private var guideFilterGridView: HorizontalGridView? = null
    private var guideChannelListGridView: VerticalGridView? = null
    private var guideTimelineGridView: HorizontalGridView? = null
    private var guideEventsContainer: VerticalGridView? = null
    private var dateTextView: TextView? = null
    private var eventDetailsView: GuideEventDetailsView? = null
    private var eventInfoView: GuideEventListViewHolder? = null
    private var pastOverlay: RelativeLayout? = null
    private var calendarBtnIcon: ImageView? = null
    private var calendarBtnSelector: View? = null
    private var calendarFilterContainer: ConstraintLayout? = null
    private var calendarFilterGridView: VerticalGridView? = null
    private var favoritesOverlay: ConstraintLayout? = null
    private var favoritesGridView: VerticalGridView? = null
    private var selectedDayFilterOffset =0
    private var setFocusOnPlayedChannel = true
    private var setFocusOnCurrentEvent = false
    private var broadcastBackgroundView: View? = null
    private var defaultGradientBackground: GradientDrawable?=null
    private var calendarFilterTitle: TextView? = null

    private val job = mutableListOf<Job>()
    //List adapters
    private val guideTimelineAdapter = GuideTimelineAdapter()
    private val guideChannelListAdapter = GuideChannelListAdapter()
    private val guideFilterCategoryAdapter = CategoryAdapter(true)
    private val favoritesListAdapter = MultiCheckListAdapter(true)
    private var waitingForExtend = true
    private var filterItemList = ArrayList<Category>()
    //if filters updated from more info then it will update the filters
    private var filtersUpdated: Boolean = false
    private var listOfVisibleChannelsForActiveWindow = mutableListOf<TvChannel>()


    private var eventsContainerAdapter = GuideEventsContainerAdapter(object :
        GuideEventsContainerAdapter.GuideEventsContainerListener {
        override fun isAccessibilityEnabled(): Boolean {
            return listener.isAccessibilityEnabled()
        }

        override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
            callback.onReceive(activeTvChannel!!)
        }

        override fun getStartTime(): Date {
            return getTimelineFirstTime()
        }

        override fun getEndTime(): Date {
            return getTimelineLastTime()
        }
        override fun isInWatchList(event:TvEvent): Boolean {
            return event.startTime > currentTime.time && listener.isInWatchList(event)
        }
        override fun isInRecList(event:TvEvent): Boolean {
            return event.startTime > currentTime.time && listener.isInRecordingList(event)
        }

        override suspend fun getCurrentTime(tvChannel: TvChannel): Long {
            return CoroutineScope(Dispatchers.IO).async {
                return@async listener.getCurrentTime(tvChannel)
            }.await()
        }

        override fun isEventLocked(tvEvent: TvEvent?) = listener.isEventLocked(tvEvent)
    },listener.getDateTimeFormat())

    //Selected tv event displayed inside the details view
    private var selectedEvent: TvEvent? = null
    private var infoEvent: TvEvent? = null

    // Keeps track of the last event focused on while scrolling horizontally.
    private var lastHorizontallyFocusedEvent: TvEvent? = null

    //Current time
    private var currentTime: Date = Date(System.currentTimeMillis())

    //timeline position
    private var currentTimePosition = 0
    private var updateScrollPosition = false
    var eventContainerList: MutableList<MutableList<TvEvent>>? = null

    //timeline layout creation progress
    var pendingLayoutCreation = false

    private var currentCategoryPosition: Int = 0

    val focusChangeDelay = 300L

    //filterDayOffset
    val filterDayOffset = mutableListOf<Int>()

    //pagination
    var leftDayOffset = 0
    var rightDayOffset = 0

    //to prevent focus from going to the guideview for the first time
    var isOpeningFirstTime: Boolean = true

    private var TIMELINE_SIZE = 48
    private var channelList: MutableList<TvChannel>? = null
    private var lastSelectedViewHolder: GuideEventListViewHolder? = null
    private var filterList = mutableListOf<CategoryItem>()
    private var updatedFilters = mutableListOf<CategoryItem>()

    var tempEventContainerList: MutableList<MutableList<TvEvent>>? = mutableListOf()
    var tempChannelList: MutableList<TvChannel>? = mutableListOf()
    var activeFilterName: String? = null
    var activeTvChannel: TvChannel? = null
    var hideLockedServices : Boolean = false
    private var valueAnimator: ValueAnimator? = null

    //RLT
    var isRTL: Boolean =
        (context as MainActivity).window.decorView.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
    init {
        view = LayoutInflater.from(ReferenceApplication.applicationContext()).inflate(R.layout.layout_widget_guide_scene, null) as ConstraintLayout
        if (((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()) {
            broadcastBackgroundView = view!!.findViewById(R.id.broadcast_background_view)
            initGradientBackgrounds()
            val drawable: Drawable = context.resources.getDrawable(R.drawable.broadcast_epg_t56_gradient,null)
            broadcastBackgroundView?.background = drawable
        }
        LoadingPlaceholder.showLoadingPlaceholder(PlaceholderName.GUIDE, onShown = {
            view!!.visibility = View.INVISIBLE
        })
        val focusChangeHandler = Handler(Looper.myLooper()!!)
        var runnable: Runnable? = null
        guideFilterGridView = view!!.findViewById(R.id.guide_filter_list_view)
        dateTextView = view!!.findViewById(R.id.guide_date_tv)
        dateTextView!!.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor(
                        "color_text_description"
                    ), 0.8
                )
            )
        )
        dateTextView!!.visibility = View.INVISIBLE
        //Set typeface
        dateTextView!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        guideTimelineGridView = view!!.findViewById(R.id.guide_timeline_view)
        pastOverlay = view!!.findViewById(R.id.guide_past_overlay)
        Utils.makeGradient(
            pastOverlay!!, GradientDrawable.LINEAR_GRADIENT,
            GradientDrawable.Orientation.RIGHT_LEFT,
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_progress"),
                    0.2
                )
            ),
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_progress"),
                    0.1
                )
            ),
            0.0F,
            0.0F
        )
        pastOverlay?.visibility = View.GONE

        var pastOverlaySeparator: View = view!!.findViewById(R.id.past_overlay_separator)
        pastOverlaySeparator.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_progress")))
        listener.getActiveChannel(object :IAsyncDataCallback<TvChannel>{
            override fun onFailed(error: Error) {}
            override fun onReceive(data: TvChannel) {
                runBlocking {
                    currentTime = Date(listener.getCurrentTime(data))
                }
            }
         }
        )

        initTimeline()

        hideLockedServices = listener.getCountryPreferences(HIDE_LOCKED_SERVICES_IN_EPG,false) as Boolean

        //Init calendar filter
        calendarBtnIcon = view!!.findViewById(R.id.calendar_button_icon)
        calendarBtnIcon!!.setImageDrawable(
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.calendar_icon
            )
        )
        calendarBtnIcon!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        calendarBtnSelector = view!!.findViewById(R.id.calendar_button_selector)
        calendarFilterContainer = view!!.findViewById(R.id.calendar_filter_container)
        calendarFilterGridView = view!!.findViewById(R.id.calendar_filter_grid_view)
        Utils.makeGradient(
            view = view!!.findViewById(R.id.calendar_gradient_view),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
            endColor = Color.TRANSPARENT,
            centerX = 0.8f,
            centerY = 0f
        )
        Utils.makeGradient(
            view = view!!.findViewById(R.id.calendar_linear_layout),
            type = GradientDrawable.LINEAR_GRADIENT,
            orientation = GradientDrawable.Orientation.RIGHT_LEFT,
            startColor = Color.parseColor(ConfigColorManager.getColor("color_dark")),
            endColor = Color.parseColor(ConfigColorManager.getColor("color_dark").replace("#", ConfigColorManager.alfa_97)),
            centerX = 0.8f,
            centerY = 0f
        )


        calendarFilterGridView!!.itemAlignmentOffset =
            context.resources.getDimensionPixelSize(R.dimen.custom_dim_0)
        calendarFilterGridView!!.itemAlignmentOffsetPercent =
            HorizontalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
        calendarFilterGridView!!.windowAlignmentOffset =
            Utils.getDimensInPixelSize(R.dimen.custom_dim_100)
        calendarFilterGridView!!.windowAlignmentOffsetPercent =
            HorizontalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        calendarFilterGridView!!.windowAlignment = HorizontalGridView.WINDOW_ALIGN_BOTH_EDGE


        initCalendarFilter()

        //Init favorites overlay
        favoritesOverlay = view!!.findViewById(R.id.favorites_overlay)
        favoritesGridView = view!!.findViewById(R.id.favorites_overlay_grid_view)
        initFavoritesOverlay()

        guideFilterGridView!!.setNumRows(1)
        guideFilterGridView!!.adapter = guideFilterCategoryAdapter
        guideFilterGridView!!.preserveFocusAfterLayout = true
        guideChannelListGridView = view!!.findViewById(R.id.guide_channel_list)
        guideChannelListGridView!!.setNumColumns(1)

        guideChannelListAdapter.adapterListener = object : GuideChannelListAdapter.GuideChannelListAdapterListener{
            override fun changeChannelFromTalkback(tvChannel: TvChannel) {
                (listener as GuideSceneWidgetListener).onWatchButtonPressed(tvChannel)
            }

            override fun isAccessibilityEnabled(): Boolean {
                return (listener as GuideSceneWidgetListener).isAccessibilityEnabled()
            }

            override fun getChannelSourceType(tvChannel: TvChannel): String {
                return listener.getChannelSourceType(tvChannel)
            }

            override fun isScrambled(): Boolean {
                return (listener as GuideSceneWidgetListener).isScrambled()
            }

            override fun activeChannel(): TvChannel {
                return activeTvChannel!!
            }
        }
        guideChannelListGridView!!.adapter = guideChannelListAdapter
        guideChannelListGridView!!.visibility = View.INVISIBLE
        guideChannelListAdapter.isParentalEnabled(listener.isParentalEnabled())

        guideEventsContainer = view!!.findViewById(R.id.guide_events_container)
        guideEventsContainer!!.visibility = View.INVISIBLE
        guideEventsContainer!!.adapter = eventsContainerAdapter

        if((listener as GuideSceneWidgetListener).isAccessibilityEnabled()) {
            guideEventsContainer?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            guideEventsContainer?.isFocusable = false
            guideTimelineGridView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            guideTimelineGridView?.isFocusable = false
        }

        guideTimelineGridView?.windowAlignment = VerticalGridView.WINDOW_ALIGN_HIGH_EDGE
        guideTimelineGridView?.windowAlignmentOffsetPercent = 10f

        guideFilterCategoryAdapter.adapterListener =
            object : CategoryAdapter.ChannelListCategoryAdapterListener {
                override fun getAdapterPosition(position: Int) {
                    focusChangeHandler.removeCallbacksAndMessages(null)
                    runnable = Runnable {
                        if (currentCategoryPosition != position && activeFilter != position) {

                            if(waitingForExtend || isLoading){
                                focusChangeHandler.removeCallbacksAndMessages(null)
                                focusChangeHandler.postDelayed(runnable!!, focusChangeDelay)
                                return@Runnable
                            }

                            if (guideTimelineGridView?.selectedPosition!! != -1) {
                                waitingForExtend = true
                                isLoading = true
                                setVisibility(false)
                                initTimeline()
                                lastHorizontallyFocusedEvent=null
                                infoEvent=null
                                currentTimePosition = 0
                                dayOffset=selectedDayFilterOffset
                                leftDayOffset = 0
                                rightDayOffset = 0
                                if(!(listener as GuideSceneWidgetListener).isAccessibilityEnabled()) {
                                    guideEventsContainer?.setSelectedPosition(0, 0)
                                }
                                guideChannelListGridView?.setSelectedPosition(0, 0)
                                if (position != activeFilter) {
                                    guideFilterCategoryAdapter.clearFocus(activeFilter)
                                    guideFilterCategoryAdapter.requestFocus(position)
                                }
                                activeFilter = position
                                listener.onFilterSelected(position)
                            } else {
                                guideTimelineGridView?.requestFocus()
                            }
                            activeFilter = position

                        } else {
                            currentCategoryPosition = position
                        }
                        updateGuideBackgroundDrawable()

                    }
                    focusChangeHandler.postDelayed(runnable!!, focusChangeDelay)

                }

                override fun onItemSelected(position: Int) {
                }

                override fun digitPressed(digit: Int) {
                    guideTimelineGridView!!.isLayoutFrozen = true
                    onDigitPressed(digit)
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener.setSpeechText(text = text, importance = importance)
                }

                override fun onKeyLeft(currentPosition: Int): Boolean {
                    currentCategoryPosition = -1

                    runnable?.let { focusChangeHandler.removeCallbacks(it) }

                    if (currentPosition == activeFilter) {
                        val holder =
                            guideFilterGridView!!.findViewHolderForAdapterPosition(
                                currentPosition
                            ) as CategoryItemViewHolder
                        guideFilterCategoryAdapter.setActiveFilter(holder)
                    }
                    if (currentPosition == 0) {
                        calendarBtnIcon?.requestFocus()
                    }
                    return false
                }

                override fun onKeyRight(currentPosition: Int): Boolean {
                    if (currentPosition == guideFilterCategoryAdapter.itemCount - 1) return true
                    currentCategoryPosition = -1
                    if (currentPosition == activeFilter) {
                        val holder =
                            guideFilterGridView!!.findViewHolderForAdapterPosition(
                                currentPosition
                            ) as CategoryItemViewHolder
                        guideFilterCategoryAdapter.setActiveFilter(holder)
                    }
                    runnable?.let { focusChangeHandler.removeCallbacks(it) }

                    return false
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onKeyDown(currentPosition: Int): Boolean {
                    if (!isLoading) {
                        guideTimelineGridView?.isLayoutFrozen = true
                        guideFilterCategoryAdapter.clearFocus(currentCategoryPosition)
                        guideTimelineGridView?.requestFocus()
                        val holder =
                            guideFilterGridView!!.findViewHolderForAdapterPosition(
                                activeFilter
                            ) as CategoryItemViewHolder
                        guideFilterCategoryAdapter.setActiveFilter(holder)

                        activeFilterName = guideFilterCategoryAdapter.getSelectedItem()
                        currentCategoryPosition = currentPosition
                        guideTimelineGridView?.isLayoutFrozen = false
                        if (eventsContainerAdapter.itemCount > 0) {
                            if (eventsContainerAdapter.getEventListView(selectedEventListView) != null) {
                                CoroutineScope(Dispatchers.Default).launch {
                                    eventsContainerAdapter.showFocusOnPositionFrame(
                                        selectedEventListView,
                                        getFirstVisibleChildIndex(
                                            eventsContainerAdapter.getEventListView(
                                                selectedEventListView
                                            )!!
                                        ),
                                        getTimelineStartTime(),
                                        getTimelineEndTime(),
                                        object : EpgEventFocus {
                                            override fun onEventFocus(
                                                lastSelectedView: GuideEventListViewHolder,
                                                isSmallDuration: Boolean
                                            ) {
                                                onSmallItemFocus(
                                                    lastSelectedView,
                                                    isSmallDuration,
                                                    false
                                                )
                                                tuneToFocusedChannel()
                                            }
                                        }
                                    )
                                }.apply(job::add)
                            }
                        }
                        return true
                    }
                    return true
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    guideFilterCategoryAdapter.clearPreviousFocus()
                    val holder =
                        guideFilterGridView!!.findViewHolderForAdapterPosition(
                            activeFilter
                        ) as CategoryItemViewHolder
                    guideFilterCategoryAdapter.setActiveFilter(holder)
                    currentCategoryPosition = currentPosition
                    listener.requestFocusOnTopMenu()
                    return true
                }

                override fun onItemClicked(position: Int) {
                    if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
                        return
                    }
                    if (isLoading) {
                        guideFilterCategoryAdapter.requestFocus(position)
                        return
                    }
                    if (position == activeFilter && guideChannelListAdapter.itemCount != 0) {
                        guideTimelineGridView?.isLayoutFrozen = true
                        guideFilterCategoryAdapter.clearFocus(currentCategoryPosition)
                        guideTimelineGridView?.requestFocus()
                        val holder =
                            guideFilterGridView!!.findViewHolderForAdapterPosition(
                                position
                            ) as CategoryItemViewHolder
                        guideFilterCategoryAdapter.setActiveFilter(holder)
                        activeFilterName = guideFilterCategoryAdapter.getSelectedItem()

                        currentCategoryPosition = position
                        guideTimelineGridView?.isLayoutFrozen = false
                        try {
                            if (eventsContainerAdapter.itemCount > 0) {
                                if (eventsContainerAdapter.getEventListView(selectedEventListView) != null) {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        eventsContainerAdapter.showFocusOnPositionFrame(
                                            selectedEventListView,
                                            getFirstVisibleChildIndex(
                                                eventsContainerAdapter.getEventListView(
                                                    selectedEventListView
                                                )!!
                                            ),
                                            getTimelineStartTime(),
                                            getTimelineEndTime(),
                                            object : EpgEventFocus {
                                                override fun onEventFocus(
                                                    lastSelectedView: GuideEventListViewHolder,
                                                    isSmallDuration: Boolean
                                                ) {
                                                    onSmallItemFocus(
                                                        lastSelectedView,
                                                        isSmallDuration,
                                                        false
                                                    )
                                                    updateTextToSpeechForTheEvent()
                                                }
                                            }
                                        )
                                    }.apply(job::add)
                                }
                            }
                        } catch (e: Exception) { }
                    } else if (guideTimelineGridView?.selectedPosition!! != -1) {
                       //this below code is unnecessary we should remove it.
                        waitingForExtend = true
                        setVisibility(false)
                        guideTimelineGridView?.scrollTo(0, 0)
                        initTimeline()
                        lastHorizontallyFocusedEvent=null
                        infoEvent=null
                        currentTimePosition = 0
                        leftDayOffset = 0
                        rightDayOffset = 0
                        if(!(listener as GuideSceneWidgetListener).isAccessibilityEnabled()) {
                            guideEventsContainer?.setSelectedPosition(0, 0)
                        }
                        selectedEventListView = 0
                        guideChannelListGridView?.setSelectedPosition(0, 0)
                        listener.onFilterSelected(position)
                    } else {
                        guideTimelineGridView?.requestFocus()
                    }
                    activeFilter = position
                }

                override fun onBackPressed(position: Int): Boolean {
                    guideFilterCategoryAdapter.clearFocus(currentCategoryPosition)
                    val holder =
                        guideFilterGridView!!.findViewHolderForAdapterPosition(
                            position
                        ) as CategoryItemViewHolder
                    guideFilterCategoryAdapter.setActiveFilter(holder)
                    currentCategoryPosition = position
                    listener.requestFocusOnTopMenu()
                    return true
                }
            }

        // Start loading animation
        showLoading()

        // Pagination
        guideTimelineGridView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (isLoading || newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return
                }
                    checkForExtendTimeline()
            }
        })

        view?.post {
            listener.onInitialized()
        }
    }

    private fun tuneToFocusedChannel() {
        if(!((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()){
            return
        }
        updateTextToSpeechForTheEvent()
        val handler = Handler(Looper.getMainLooper())
        handler.removeCallbacksAndMessages(null)
        //putting delay of 1 sec to avoid repeatedly tuning to channel when user moves focus very fast in epg.
        handler.postDelayed({
            if (infoEvent != null) {
                (listener as GuideSceneWidgetListener).tuneToFocusedChannel(infoEvent!!.tvChannel)
                updateActiveWindow()
            }
        },1000)
    }


    fun checkForExtendTimeline() {
        val totalItemCount = guideTimelineGridView!!.adapter!!.itemCount
        if (dayOffset + leftDayOffset > DAY_OFFSET_MIN) {

            if (currentTimePosition - ITEM_OFFSET_TRIGGER <= 0) {
                showLoading()
                (listener as GuideSceneWidgetListener).getDayWithOffset(
                    dayOffset + leftDayOffset - 1,
                    true, channelList!!
                )
            }
        }

        if (dayOffset + rightDayOffset < DAY_OFFSET_MAX) {
            if (GUIDE_TIMELINE_PAGE_SIZE + currentTimePosition + ITEM_OFFSET_TRIGGER >= totalItemCount) {
                showLoading()
                (listener as GuideSceneWidgetListener).getDayWithOffset(
                    dayOffset + rightDayOffset + 1,
                    true, channelList!!
                )
            }

        }
    }

    /**
     * Init calendar filter
     */
    private fun initCalendarFilter() {
        calendarFilterTitle = view!!.findViewById<TextView>(R.id.calendar_filter_title)
        calendarFilterTitle!!.text = ConfigStringsManager.getStringById("calendar_filter_title")
        calendarFilterTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        calendarFilterTitle!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        val calendarFilterAdapter = ChannelListSortAdapter()

        val filterItems = mutableListOf<String>()
        filterDayOffset.clear()
        for (i in DAY_OFFSET_MIN..DAY_OFFSET_MAX) {
            filterItems.add(buildDayName(i))
            filterDayOffset.add(i)
        }

        calendarFilterAdapter.selectedItem = filterDayOffset.indexOf(0)
        calendarFilterAdapter.refresh(filterItems)
        calendarFilterGridView!!.setNumColumns(1)
        calendarFilterGridView!!.adapter = calendarFilterAdapter
        calendarFilterAdapter.adapterListener =
            object : ChannelListSortAdapter.ChannelListSortAdapterListener {
                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (listener as GuideSceneWidgetListener).setSpeechText(text = text, importance = importance)
                }

                override fun onItemClicked(position: Int) {
                    //this method is called to restart inactivity timer for no signal power off
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                    if (filterDayOffset[position]==dayOffset)return
                    selectCalenderFilter(position)
                }

                override fun onKeyPressed(keyEvent: KeyEvent): Boolean {
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_BACK -> {
                                if(!downActionBackKeyDone) return true
                                calendarFilterContainer?.visibility = View.GONE
                                calendarBtnIcon?.requestFocus()
                                return true
                            }
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        if (checkIfDigit(keyEvent.keyCode)){
                            calendarFilterContainer?.visibility = View.GONE
                            calendarBtnIcon?.requestFocus()
                            val digit = if (keyEvent.keyCode<KeyEvent.KEYCODE_NUMPAD_0){
                                keyEvent.keyCode - KeyEvent.KEYCODE_0
                            }else{
                                 keyEvent.keyCode - KeyEvent.KEYCODE_NUMPAD_0
                            }
                            guideTimelineGridView!!.isLayoutFrozen = true
                            onDigitPressed(digit)
                            return true
                        }
                        when (keyEvent.keyCode) {
                            if (isRTL) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT -> {
                                return true
                            }
                            KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_CHANNEL_UP -> {
                                calendarFilterContainer?.visibility = View.GONE
                                guideFilterGridView!!.requestFocus()
                                guideFilterCategoryAdapter.adapterListener!!.onKeyDown(currentCategoryPosition)
                                return true
                            }
                            KeyEvent.KEYCODE_BACK ->{
                                downActionBackKeyDone = false
                            }
                        }
                    }
                    return false
                }
            }
        calendarBtnIcon?.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    (listener as GuideSceneWidgetListener).setSpeechText(ConfigStringsManager.getStringById("calendar_filter_title"))
                    calendarBtnSelector?.background =
                        ConfigColorManager.generateButtonBackground()
                    calendarBtnIcon!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_background")))
                    calendarBtnSelector!!.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
                } else {
                calendarBtnSelector!!.setBackgroundColor(Color.TRANSPARENT)
                calendarBtnIcon!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                }
            }

        calendarBtnIcon?.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, keycode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyEvent?.action == KeyEvent.ACTION_UP) {
                    when (keycode) {
                        KeyEvent.KEYCODE_ESCAPE,
                        KeyEvent.KEYCODE_BACK -> {
                            if (!downActionBackKeyDone)return true
                            (listener as GuideSceneWidgetListener).requestFocusOnTopMenu()
                            return false
                        }
                    }
                }
                if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                    if (checkIfDigit(keyEvent.keyCode)){
                        val digit = if (keyEvent.keyCode<KeyEvent.KEYCODE_NUMPAD_0){
                            keyEvent.keyCode - KeyEvent.KEYCODE_0
                        }else{
                            keyEvent.keyCode - KeyEvent.KEYCODE_NUMPAD_0
                        }
                        guideTimelineGridView!!.isLayoutFrozen = true
                        onDigitPressed(digit)
                        return true
                    }

                    when (keyEvent.keyCode) {

                        KeyEvent.KEYCODE_BACK -> {
                            downActionBackKeyDone = true
                        }

                        if (isRTL) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            guideFilterGridView!!.requestFocus()
                            return true
                        }

                        if (isRTL) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT -> {
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            //Disable key down
                            guideTimelineGridView?.isLayoutFrozen = true
                            guideTimelineGridView?.requestFocus()
                            guideTimelineGridView?.isLayoutFrozen = false
                            if (eventsContainerAdapter.itemCount > 0) {
                                if (eventsContainerAdapter.getEventListView(selectedEventListView) != null)
                                    CoroutineScope(Dispatchers.Default).launch {
                                        eventsContainerAdapter.showFocusOnPositionFrame(
                                            selectedEventListView,
                                            getFirstVisibleChildIndex(
                                                eventsContainerAdapter.getEventListView(
                                                    selectedEventListView
                                                )!!
                                            ),
                                            getTimelineStartTime(),
                                            getTimelineEndTime(),
                                            object : EpgEventFocus {
                                                override fun onEventFocus(
                                                    lastSelectedView: GuideEventListViewHolder,
                                                    isSmallDuration: Boolean
                                                ) {
                                                    onSmallItemFocus(
                                                        lastSelectedView,
                                                        isSmallDuration,
                                                        false
                                                    )
                                                    updateTextToSpeechForTheEvent()
                                                }
                                            }
                                        )
                                    }.apply(job::add)
                            }
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            (listener as GuideSceneWidgetListener).requestFocusOnTopMenu()
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            calendarFilterGridView?.visibility = View.VISIBLE
                            calendarFilterContainer?.bringToFront()
                            calendarFilterContainer?.elevation = 10f
                            calendarFilterContainer?.visibility = View.VISIBLE
                            (listener as GuideSceneWidgetListener).setSpeechText(
                                calendarFilterTitle!!.text.toString(),
                                importance = SpeechText.Importance.HIGH
                            )
                            calendarFilterGridView?.scrollToPosition(
                                filterDayOffset.indexOf(
                                    dayOffset
                                )
                            )
                            calendarFilterGridView?.post {
                                calendarFilterGridView?.layoutManager!!.findViewByPosition(
                                    filterDayOffset.indexOf(dayOffset)
                                )?.requestFocus()
                            }
                            calendarBtnSelector?.background =
                                ContextCompat.getDrawable(
                                    view!!.context,
                                    R.drawable.reference_button_selected
                                )
                            calendarBtnSelector?.backgroundTintList = ColorStateList.valueOf(
                                Color.parseColor(
                                    ConfigColorManager.getColor("color_main_text")
                                        .replace("#", ConfigColorManager.alfa_light)
                                )
                            )
                            return true
                        }

                    }
                }
                return true
            }
        })
    }

    private fun selectCalenderFilter(position: Int) {
        setVisibility(false)
        isLoading = true
        dayOffset = filterDayOffset[position]
        selectedDayFilterOffset = dayOffset
        guideTimelineGridView?.scrollTo(0, 0)
        isOpeningFirstTime = false
        initTimeline()
        lastHorizontallyFocusedEvent=null
        infoEvent=null
        currentTimePosition = 0
        leftDayOffset = 0
        rightDayOffset = 0
        if(!(listener as GuideSceneWidgetListener).isAccessibilityEnabled()) {
            guideEventsContainer?.setSelectedPosition(0, 0)
        }
        guideChannelListGridView?.setSelectedPosition(0, 0)
        (listener as GuideSceneWidgetListener).getDayWithOffset(dayOffset, false,channelList!!)
        calendarFilterContainer?.visibility = View.GONE
        calendarFilterGridView?.visibility = View.GONE
        calendarBtnSelector!!.setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * Build display day name
     */
    private fun buildDayName(dayOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTime.time)
        calendar.add(Calendar.DATE, dayOffset)

        return when (dayOffset) {
            0 -> ConfigStringsManager.getStringById("today")
            1 -> ConfigStringsManager.getStringById("tomorrow")
            else -> {
                SimpleDateFormat((listener as GuideSceneWidgetListener).getDateTimeFormat().datePattern, Locale("en"))
                    .format(calendar.time)
            }
        }
    }

    /**
     * Update Date Text
     */
    private fun updateDateText() {
        var count = 0
            for (i in 0..currentTimePosition) {
            val position = minOf(guideTimelineAdapter.getItems().lastIndex, i)//index out of bound fix( CLTVFAST-72)
            val time = guideTimelineAdapter.getItems().get(position)
                //12:00A am for 12 hrs format and 00:00 for for 24 hr format
            if (time.contains("12:00AM") || time.contains("00:00")
            ) {
                count++
            }
        }
        dateTextView?.text = buildDayName(leftDayOffset + (count - 1) + dayOffset)
    }

    /**
     * Init favorites overlay
     */
    private fun initFavoritesOverlay() {
        val favoriteContainerTitle = view!!.findViewById<TextView>(R.id.favorites_overlay_title)
        favoriteContainerTitle!!.text = ConfigStringsManager.getStringById("add_to")
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

        val favoritesItems = arrayListOf<String>()
        (listener as GuideSceneWidgetListener).getFavoriteCategories(object: IAsyncDataCallback<ArrayList<String>> {
            override fun onFailed(error: Error) {
                favoritesListAdapter.refresh(favoritesItems)
            }

            override fun onReceive(data: ArrayList<String>) {
                favoritesItems.addAll(data)
                favoritesListAdapter.refresh(favoritesItems)
            }
        })
        favoritesGridView!!.selectedPosition = 0
        favoritesGridView!!.setNumColumns(1)
        favoritesGridView!!.preserveFocusAfterLayout = true
        favoritesGridView!!.adapter = favoritesListAdapter

        favoritesGridView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == 0) {
                    val position = favoritesListAdapter.focusedItem
                    favoritesGridView!!.layoutManager!!.findViewByPosition(
                        position
                    )?.requestFocus()
                }
            }
        })

        favoritesListAdapter.adapterListener =
            object : MultiCheckListAdapter.MultiCheckListAdapterListener {
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
                    (listener as GuideSceneWidgetListener).setSpeechText(text = text, importance = importance)
                }

                override fun onBackPressed(position: Int): Boolean {
                   saveChannelInFav()
                    favoritesGridView?.scrollToPosition(0)
                    favoritesOverlay?.visibility = View.GONE
                    eventDetailsView?.selectFavoriteButton(true)
                    eventDetailsView?.requestFocus()
                    return true
                }
            }
    }

    fun saveChannelInFav(){
        (listener as GuideSceneWidgetListener).onFavoriteButtonPressed(
            selectedEvent!!.tvChannel,
            ArrayList(favoritesListAdapter.getSelectedItems())
        )
    }

    private fun onTimeUpdate(currTime :Date){
        val lastCurrentTime: Date = currentTime
        val dateOfLastTime = LocalDate.of(lastCurrentTime.year,lastCurrentTime.month+1,lastCurrentTime.date)
        val dateOfCurrentTime = LocalDate.of(currTime.year,currTime.month+1,currTime.date)
        if (Period.between(dateOfLastTime,dateOfCurrentTime).days > 1){
            //since last time and current have a big difference so we are not updating the current time
            //else epg will break
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeUpdate: wrong time")
            return
        }
        currentTime = currTime

        // handle timeline crossing a day
        if (lastCurrentTime != null && lastCurrentTime.date != currentTime.date && currentTime.time > lastCurrentTime.time &&  (currentTime.time - lastCurrentTime.time) < TimeUnit.DAYS.toMillis(1) ){
            dayOffset -= 1
        }
        updateDateText()
        updatePastOverlay()
        //checks if focused event is now past event then it jumps the focus on next event
        if(guideTimelineGridView!!.hasFocus()){
            CoroutineScope(Dispatchers.Default).launch {
                eventsContainerAdapter.isPastEventFocused(
                    currentTime.time,
                    selectedEventListView,
                    getTimelineEndTime(),
                    currentTimePosition >= timelineEndPosition(),
                    false,
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: GuideEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                            updateTextToSpeechForTheEvent()
                        }
                    })
            }.apply(job::add)
        }
        if (!isLoading){
            //updates icons when playing event switches
            (listener as GuideSceneWidgetListener).getActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {}
                override fun onReceive(data: TvChannel) {

                    activeTvChannel = data
                    ReferenceApplication.runOnUiThread{
                        channelList?.forEachIndexed { index, tvChannel ->
                            if (tvChannel.id == data.id) {
                                CoroutineScope(Dispatchers.Default).launch {
                                    eventsContainerAdapter.refreshIndicators(
                                        index,
                                        selectedEvent!=null,
                                        object : EpgEventFocus {
                                            override fun onEventFocus(
                                                lastSelectedView: GuideEventListViewHolder,
                                                isSmallDuration: Boolean
                                            ) {
                                                onSmallItemFocus(
                                                    lastSelectedView,
                                                    isSmallDuration,
                                                    false
                                                )
                                                updateTextToSpeechForTheEvent()
                                            }
                                        })
                                }.apply(job::add)
                            }
                        }
                    }
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun refresh(data: Any) {
        super.refresh(data)
        if (data is Date) {

            (listener as GuideSceneWidgetListener).getActiveChannel(object :IAsyncDataCallback<TvChannel>{
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: Active channel not found")
                    val currTime = Date(data.time)
                    onTimeUpdate(currTime)
                }
                override fun onReceive(data: TvChannel) {
                    runBlocking {
                        val currTime = Date((listener as GuideSceneWidgetListener).getCurrentTime(data))
                        onTimeUpdate(currTime)
                    }
                }
            })
        }
        else if (data is ArrayList<*> && data.isNotEmpty()) {
            if (data.first() is TvChannel) {
                var adapterItems = mutableListOf<TvChannel>()

                data.forEach { item ->
                    if(!(hideLockedServices && (item as TvChannel).isLocked)) {
                        adapterItems.add(item as TvChannel)
                    }
                }

                channelList = adapterItems
                if (adapterItems.isNotEmpty()) {
                    guideChannelListAdapter.refresh(adapterItems)
                }
            }
            if (data.first() is Category) {

                activeFilter = (listener as GuideSceneWidgetListener).getActiveEpgFilter()
                currentCategoryPosition = (listener as GuideSceneWidgetListener).getActiveEpgFilter()
                guideFilterCategoryAdapter.focusPosition = activeFilter
                guideFilterCategoryAdapter.selectedItem = activeFilter
                val list = mutableListOf<CategoryItem>()

                data.forEach { item ->
                    if (item is Category && (item.name!! !in mutableListOf(
                            "TV Channels",
                            "Tuner(DTV)"
                        ))
                    ) {
                        filterItemList.add(item)
                        list.add(CategoryItem(item.id, item.name!!))
                    }
                }
                
                filterList = list

                if (list.isNotEmpty()) {
                    guideFilterCategoryAdapter.keepFocus = true
                    guideFilterCategoryAdapter.refresh(list)
                    guideFilterGridView!!.post {
                        guideFilterGridView!!.scrollToPosition(activeFilter)
                        guideFilterGridView!!.clearFocus()
                        guideFilterCategoryAdapter.keepFocus = false
                    }
                }
            }
        }
        else if (data is LinkedHashMap<*, *>) {
            var eventListContainer = mutableListOf<MutableList<TvEvent>>()
            for ((key, value) in data) {
                if (value == null) {
                    continue
                }
                eventListContainer.add(value as MutableList<TvEvent>)
                this.eventContainerList = eventListContainer
            }
            guideEventsContainer!!.setNumColumns(1)
            guideEventsContainer!!.setHasFixedSize(true)
            guideEventsContainer!!.itemAnimator = null
            (listener as GuideSceneWidgetListener).getActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    isLoading = false
                    waitingForExtend = false
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: Active channel not found")
                }
                override fun onReceive(data: TvChannel) {
                    runBlocking {
                        currentTime = Date((listener as GuideSceneWidgetListener).getCurrentTime(data))
                        activeTvChannel = data
                        if(setFocusOnPlayedChannel) {
                            selectedEventListView = 0
                            channelList?.forEachIndexed { index, tvChannel ->
                                if (tvChannel.id == data.id) {
                                    selectedEventListView = index
                                }
                            }
                        }
                        setFocusOnPlayedChannel = true
                        try {
                            if (eventListContainer.isNotEmpty()) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    yield()
                                    updateGuideBackgroundDrawable()
                                    if (eventDetailsView != null) {
                                        // When epg will be reloaded automatically, at that time we need to close event details view.
                                        eventDetailsView!!.clearAnimation()
                                        view!!.removeView(eventDetailsView)
                                        selectedEvent = null
                                        guideChannelListAdapter.resetSeparator()
                                        eventsContainerAdapter.resetSeparator()

                                    }
                                    eventsContainerAdapter.refresh(eventListContainer)
                                    initScrollPosition()

                                }.apply(job::add)
                            }else{
                                isLoading = false
                                waitingForExtend = false
                            }
                        }catch (e: Exception){
                            isLoading = false
                            waitingForExtend = false
                        }
                    }
                }
            })
        }
        else if (data is Boolean){
            if(data){
                LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.GUIDE, onHidden = {
                    view!!.visibility = View.VISIBLE
                    guideTimelineGridView!!.visibility = View.INVISIBLE
                    calendarBtnIcon!!.visibility = View.INVISIBLE
                })
            }
        }
    }

    override fun dispose() {
        job.forEach { it.cancel() }
        super.dispose()
    }

    //Pagination
    @RequiresApi(Build.VERSION_CODES.R)
    override fun extendTimeline(
        map: LinkedHashMap<Int, MutableList<TvEvent>>,
        additionalDayCount: Int
    ) {

        if (eventsContainerAdapter.itemCount!=map.size){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "extendTimeline: something wrong with extend")
            hideLoading()
            return
        }

        var dayCount = 0
        this.guideTimelineAdapter.getItems().forEach {
            if (it == "12:00AM" || it == "00:00") {
                dayCount++
            }
        }

        //prepare timeline lihomesst
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTime.time)
        calendar.add(Calendar.DATE, additionalDayCount)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val list = mutableListOf<String>()

        var nextHour = -1
        while (nextHour != 0) {
            repeat(2) {
                val df = SimpleDateFormat(
                    if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm"
                    else "hh:mm a", Locale.ENGLISH
                )
                var formattedDate = df.format(calendar.time)
                formattedDate = formattedDate.uppercase(Locale.getDefault())
                formattedDate = formattedDate.replace(" AM", "AM")
                formattedDate = formattedDate.replace(" PM", "PM")
                list.add(formattedDate)
                calendar.add(Calendar.MINUTE, 30)
            }
            nextHour = calendar.get(Calendar.HOUR_OF_DAY)
        }

        val extendingPreviousDay = additionalDayCount < dayOffset

        if (dayCount > 1) {
            if (extendingPreviousDay) {
                dayOffset--
                currentTimePosition = list.size
            } else {
                dayOffset++
                currentTimePosition = timelineEndPosition() - list.size
            }

        } else {
            if (extendingPreviousDay) {
                leftDayOffset--
                currentTimePosition += list.size
            } else {
                rightDayOffset++
            }
            TIMELINE_SIZE += list.size
        }

        val eventContainerList = mutableListOf<MutableList<TvEvent>>()
        for ((key, value) in map) {
            if (value == null) {
                continue
            }
            for (event in ArrayList(value)) {
                //to remove invalid events
                if (event.startTime > event.endTime) {
                    map.remove(key)
                }
            }
            eventContainerList.add(value)
        }

        if (guideTimelineGridView!!.hasFocus()) {
            eventsContainerAdapter.showFocus(
                selectedEventListView,
                Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: GuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                        updateTextToSpeechForTheEvent()
                    }
                }
            )
        }
        try {
            guideTimelineAdapter.extendTimeline(list, extendingPreviousDay, dayCount)
            guideTimelineGridView!!.scrollToPosition(currentTimePosition)
            eventsContainerAdapter.extendTimeline(
                eventContainerList,
                extendingPreviousDay,
                selectedEventListView,
                guideTimelineGridView!!.hasFocus(),
                dayCount,
                leftDayOffset + dayOffset
            )
        }catch (e:Exception){
            hideLoading()
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "extendTimeline:  ${e.message}")
            return
        }

        guideEventsContainer?.post {
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                yield()
                updatePastOverlay()
                eventsContainerAdapter.scrollToTime(getTimelineStartTime())
                if (guideTimelineGridView!!.hasFocus() && !setFocusOnCurrentEvent) {
                    eventsContainerAdapter.showFocus(
                        selectedEventListView,
                        Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                        object : EpgEventFocus {
                            override fun onEventFocus(
                                lastSelectedView: GuideEventListViewHolder,
                                isSmallDuration: Boolean
                            ) {
                                onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                                updateTextToSpeechForTheEvent()
                            }
                        }
                    )
                }
                setFocusOnCurrentEvent = false

            }.apply(job::add)

            // When timeline is not initialized yet
            if(isLoading && waitingForExtend){
                lastHorizontallyFocusedEvent=null
                infoEvent=null
                initScrollPosition()
                waitingForExtend = false
            }else{
                hideLoading()
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "extendTimeline: else ")
            }
        }
    }

    /**
     * Update event list data
     */
    @RequiresApi(Build.VERSION_CODES.R)
    override fun update(map: kotlin.collections.LinkedHashMap<Int, MutableList<TvEvent>>) {
        var eventContainerList = mutableListOf<MutableList<TvEvent>>()
        for (index in 0 until map.size) {
            if (map[index] == null) {
                continue
            }
            eventContainerList.add(map[index] as MutableList<TvEvent>)
        }
        eventsContainerAdapter.update(eventContainerList)

        if (guideEventsContainer!!.visibility == View.INVISIBLE && selectedEventListView < eventsContainerAdapter.itemCount) {
            initScrollPosition()
        }
    }

    //Timeline initialization
    private fun initTimeline() {

        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTime.time)
        calendar.add(Calendar.DATE, dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val list = mutableListOf<String>()

        var nextHour = -1
        while (nextHour != 0) {
            repeat(2) {
                val df = SimpleDateFormat(
                    if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm"
                    else "hh:mm a", Locale.ENGLISH
                )
                var formattedDate = df.format(calendar.time)
                formattedDate = formattedDate.uppercase(Locale.getDefault())
                formattedDate = formattedDate.replace(" AM", "AM")
                formattedDate = formattedDate.replace(" PM", "PM")
                list.add(formattedDate)
                calendar.add(Calendar.MINUTE, 30)
            }
            nextHour = calendar.get(Calendar.HOUR_OF_DAY)
        }

        TIMELINE_SIZE = list.size
        guideTimelineAdapter.refresh(list)
        guideTimelineGridView!!.setNumRows(1)
        guideTimelineGridView!!.adapter = guideTimelineAdapter

        guideTimelineAdapter.guideTimelineAdapterListener =
            object : GuideTimelineAdapter.GuideTimelineAdapterListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun dispatchKey(keyCode: Int, keyEvent: KeyEvent): Boolean {
                    if (isLoading)return true
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_BACK -> {
                                if (!downActionBackKeyDone)return true
                                onBackPressed()
                            }
                            KeyEvent.KEYCODE_BOOKMARK -> {
                                if (selectedEvent != null) {

                                    updatePastOverlay()
                                    updateDateText()
                                    val isAddedSuccessfully =
                                        (listener as GuideSceneWidgetListener).onWatchlistButtonPressed(
                                            selectedEvent!!
                                        )
                                    if (!isAddedSuccessfully) return true

                                    eventDetailsView!!.refreshWatchlistButton()
                                    if (eventInfoView != null && !eventInfoView!!.rootView!!.isVisible) return true
                                    CoroutineScope(Dispatchers.Default).launch {
                                        eventsContainerAdapter.refreshIndicators(
                                            selectedEventListView,
                                            selectedEvent!=null,
                                            object : EpgEventFocus {
                                                override fun onEventFocus(
                                                    lastSelectedView: GuideEventListViewHolder,
                                                    isSmallDuration: Boolean
                                                ) {
                                                    onSmallItemFocus(
                                                        lastSelectedView,
                                                        isSmallDuration,
                                                        false
                                                    )
                                                    updateTextToSpeechForTheEvent()
                                                }
                                            })
                                    }.apply(job::add)
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_INFO -> {
                                try {
                                    (listener as GuideSceneWidgetListener).onMoreInfoButtonPressed(
                                        eventsContainerAdapter.getSelectedEvent(
                                            selectedEventListView
                                        )!!
                                    )
                                } catch (E: Exception) {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "dispatchKey: ${E.printStackTrace()}")
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (selectedEvent == null) {
                                    onItemClick()
                                }
                            }

                            KeyEvent.KEYCODE_MENU -> {
                                try {
                                    (listener as GuideSceneWidgetListener).onOptionsClicked()
                                }catch (E: Exception) {

                                }
                                return true
                            }
                        }
                        return true
                    }
                    if (eventsContainerAdapter.itemCount == 0) {
                        return true
                    }
                    if (selectedEvent != null) {
                        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    if(favoritesOverlay?.visibility == View.VISIBLE) return true
                                    if (selectedEventListView == 0) return true
                                    return true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    if (eventClickAnimation) return true
                                    if(favoritesOverlay?.visibility == View.VISIBLE) return true
                                    return true
                                }
                            }
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        if (isLoading) {
                            return true
                        }
                        if (checkIfDigit(keyEvent.keyCode)){
                            val digit = if (keyEvent.keyCode<KeyEvent.KEYCODE_NUMPAD_0){
                                keyEvent.keyCode - KeyEvent.KEYCODE_0
                            }else{
                                keyEvent.keyCode - KeyEvent.KEYCODE_NUMPAD_0
                            }
                            guideTimelineGridView!!.isLayoutFrozen = true
                            onDigitPressed(digit)
                            return true
                        }
                        when (keyCode) {
                            if (isRTL) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    updateScrollPosition = keyEvent.repeatCount != 0
                                    onScrollRight()
                                }
                            }
                            if (isRTL) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    updateScrollPosition = keyEvent.repeatCount != 0
                                    onScrollLeft()
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                if (keyEvent.repeatCount % 3 == 0) {
                                    onUpPressed()
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (keyEvent.repeatCount % 3 == 0) {
                                    onDownPressed()
                                }
                            }

                            KeyEvent.KEYCODE_CHANNEL_UP -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    onChannelUpPressed()
                                }
                            }

                            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    if (selectedEvent!=null){
                                        closeEventDetails(callback = object :IAsyncCallback{
                                            override fun onFailed(error: Error) {}
                                            override fun onSuccess() {
                                                onChannelDownPressed()
                                            }
                                        })
                                    }else{
                                        onChannelDownPressed()
                                    }
                                    return true
                                }
                            }
                            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> downActionBackKeyDone = true
                        }
                        return false
                    }
                    return true
                }

                override fun isAccessibilityEnabled(): Boolean {
                    return (listener as GuideSceneWidgetListener).isAccessibilityEnabled()
                }
            }
    }

    private fun onDigitPressed(digit :Int)  {
        val epgActiveFilter =
            FilterItemType.getFilterTypeById(filterList[activeFilter].id)
        var filterMetadata: String? = null
        when (epgActiveFilter) {
            FilterItemType.FAVORITE_ID, FilterItemType.TIF_INPUT_CATEGORY, FilterItemType.GENRE_ID -> {
                filterMetadata = filterList[activeFilter].name
            }

            else -> {}
        }
        (listener as GuideSceneWidgetListener).onDigitPressed(
            digit,
            epgActiveFilter,
            filterMetadata
        )
    }

    private fun onScrollLeft() {
            //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (pendingLayoutCreation || eventClickAnimation) return

        if (pendingLayoutCreation) return
        pendingLayoutCreation = true
        guideTimelineGridView!!.postDelayed(Runnable {
            pendingLayoutCreation = false
        }, 200)

        val lastPosition = currentTimePosition
        if (lastPosition > 0) {
            var doScroll = false
            val selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
            if (selectedEvent != null) {
                //Prevent focus on past event
                val eventCurrentTime = TimeUnit.MILLISECONDS.toMinutes(currentTime.time)
                val selectedEventEndTime = TimeUnit.MILLISECONDS.toMinutes(selectedEvent.endTime )
                val selectedEventStartTime = TimeUnit.MILLISECONDS.toMinutes(selectedEvent.startTime)
                val isEventCurrentlyPlayed = eventCurrentTime in selectedEventStartTime..selectedEventEndTime
                val isEventFromThePast = selectedEventEndTime <= eventCurrentTime

                if ((isEventCurrentlyPlayed || isEventFromThePast) && getTimelineStartTime().time<currentTime.time) {
                    return
                }
                val startTime = getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(31)
                if (startTime > selectedEvent.startTime && lastPosition > 0) {
                    doScroll = true
                }
                if (doScroll) {
                    guideTimelineGridView?.layoutManager?.findViewByPosition(lastPosition)
                        ?.requestFocus()
                    currentTimePosition -= 1
                    smoothScrollTimelineToPosition(currentTimePosition)
                    eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())
                    updatePastOverlay()
                    updateDateText()

                }
            }else{
                return
            }

        } else {
            checkForExtendTimeline()

        }
        eventsContainerAdapter.findPreviousFocus(
            selectedEventListView,
            getTimelineStartTime(),
            lastPosition == 0,
            object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: GuideEventListViewHolder,
                    isSmallDuration: Boolean
                ) {
                    onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                    updateTextToSpeechForTheEvent()
                }
            })
    }

    private fun onScrollRight(isForced: Boolean = false) {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (pendingLayoutCreation || eventClickAnimation) return
        pendingLayoutCreation = true
        guideTimelineGridView!!.postDelayed({
            pendingLayoutCreation = false
        }, 200)
        val lastPosition = currentTimePosition
        var doScroll = false
        val selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
        if (selectedEvent != null) {
            val selectedEventEndTime = selectedEvent.endTime
            val endTimeline = getTimelineEndTime().time - TimeUnit.MINUTES.toMillis(30)
            if (endTimeline <= selectedEventEndTime && lastPosition <= timelineEndPosition()) {
                doScroll = true
            }
        }
        if (doScroll || isForced) {
            if (guideTimelineGridView!!.hasFocus()) {
            guideTimelineGridView?.layoutManager?.findViewByPosition(lastPosition + 1)
                ?.requestFocus()
            }
            currentTimePosition += 1
            smoothScrollTimelineToPosition(currentTimePosition)
            var startHours = (timelineEndPosition()) / 2
            if (hoursIndex(getTimelineStartTime()) <= startHours) {
                eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())
            }
            updatePastOverlay()
            updateDateText()
        }
        //scrolling can be happen when focus is not in guide so in that case double focus can appear
        //so we just need to find focus when we are inside guide
        if (guideTimelineGridView!!.hasFocus()) {
            CoroutineScope(Dispatchers.Default).launch {
                eventsContainerAdapter.findNextFocus(selectedEventListView,
                    getTimelineEndTime(),
                    currentTimePosition >= timelineEndPosition(),
                    isForced,
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: GuideEventListViewHolder, isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                            updateTextToSpeechForTheEvent()
                        }
                    })
            }.apply(job::add)
        }
    }


    interface EpgEventFocus {
        fun onEventFocus(lastSelectedView: GuideEventListViewHolder, isSmallDuration: Boolean)
    }

    /*
    * to smooth scroll timeline to position
    * */
    fun smoothScrollTimelineToPosition(position: Int) {
        val smoothScroller: RecyclerView.SmoothScroller = object : LinearSmoothScroller(context) {
            override fun getHorizontalSnapPreference(): Int {
                return SNAP_TO_START
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return (super.calculateTimeForScrolling(dx) * 2)
            }

            override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
                return PointF(-1f, 0f)
            }

            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                val extra =
                    super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference)
                return if (isRTL) extra + 103 * GUIDE_TIMELINE_MINUTE else extra
            }
        }
        smoothScroller.targetPosition = position
        guideTimelineGridView!!.stopScroll()
        guideTimelineGridView!!.layoutManager!!.startSmoothScroll(smoothScroller)
    }

    private fun updateTextToSpeechForTheEvent() {

        val tvEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)

        (listener as GuideSceneWidgetListener).setSpeechText(
            tvEvent?.name ?: ConfigStringsManager.getStringById("no_information")
        )
    }

    private fun onUpPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (eventClickAnimation||isLoading) return
        if (!channelList.isNullOrEmpty()&& (channelList!!.size > selectedEventListView) &&
            (selectedEventListView == 0 ||
                    ((listener as? GuideSceneWidgetListener)?.getChannelsOfSelectedFilter()?.firstOrNull() == channelList!![selectedEventListView])
                    )
            ) {
            if (eventInfoView != null) {
                eventInfoView!!.itemView.visibility = View.INVISIBLE
            }
            eventsContainerAdapter.clearFocus(selectedEventListView)
            guideFilterGridView!!.scrollToPosition(activeFilter)
            guideFilterGridView!!.requestFocus()
            tuneBackToActiveFastChannel()
            return
        }

        val timelineStartTime = getTimelineStartTime()
        val timelineEndTime = getTimelineEndTime()

        //index out of bound CLTVFUNAI-254
        if (!channelList.isNullOrEmpty() &&
            selectedEventListView >= 0 && selectedEventListView < 6 &&
            selectedEventListView < channelList!!.size &&
            selectedEventListView < (listener as GuideSceneWidgetListener).getChannelsOfSelectedFilter().size &&
            channelList!![selectedEventListView].id != (listener as GuideSceneWidgetListener).getChannelsOfSelectedFilter()[selectedEventListView].id) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView--

            eventsContainerAdapter.showFocusOnEventFrame(
                selectedEventListView,
                currentTime,
                lastHorizontallyFocusedEvent,
                infoEvent,
                timelineStartTime,
                timelineEndTime,
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: GuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                        tuneToFocusedChannel()
                    }
                }
            )
            var isExtend = false
            var startDayOffset = dayOffset+leftDayOffset
            var endDayOffset = dayOffset + rightDayOffset
            if (startDayOffset != endDayOffset) isExtend = true

            isLoading = true
            (listener as GuideSceneWidgetListener).loadPreviousChannels(
                channelList?.get(0)!!,

                object : IAsyncDataCallback<java.util.LinkedHashMap<Int, MutableList<TvEvent>>> {

                    override fun onReceive(data: java.util.LinkedHashMap<Int, MutableList<TvEvent>>) {
                        var index = 0
                        tempChannelList!!.clear()
                        tempEventContainerList!!.clear()
                        for (value in data.values) {
                            if(!(hideLockedServices && (value[0].tvChannel).isLocked)) {
                                if (!isChannelPresentInList(value[0].tvChannel, channelList!!)) {
                                    channelList!!.add(index, value[0].tvChannel)
                                    eventContainerList!!.add(index, value)
                                    tempChannelList!!.add(index, value[0].tvChannel)
                                    tempEventContainerList!!.add(index, value)

                                    index++

                                }
                            }
                        }
                        ReferenceApplication.runOnUiThread {
                            isLoading = false
                            guideChannelListGridView?.itemAnimator = null
                            selectedEventListView += index
                            guideChannelListAdapter.addNewChannelsToStart(tempChannelList!!)
                            eventsContainerAdapter.insertContainersToStart(tempEventContainerList!!)
                            updatePastOverlay()

                            if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                                setSelectedPosition(
                                    selectedEventListView + 2,
                                    Utils.convertDpToPixel(20.0).toInt()
                                )
                            } else {
                                setSelectedPosition(
                                    selectedEventListView,
                                    Utils.convertDpToPixel(0.0).toInt()
                                )
                            }
                        }
                    }
                    override fun onFailed(error: Error) {
                    }
                },
                startDayOffset, endDayOffset, isExtend
            )
        } else {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView--
            eventsContainerAdapter.showFocusOnEventFrame(
                selectedEventListView,
                currentTime,
                lastHorizontallyFocusedEvent,
                infoEvent,
                timelineStartTime,
                timelineEndTime,
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: GuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                        tuneToFocusedChannel()
                    }
                }
            )

            ReferenceApplication.runOnUiThread {
                if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                    setSelectedPosition(
                        selectedEventListView + 2,
                        Utils.convertDpToPixel(20.0).toInt()
                    )
                } else {
                    setSelectedPosition(
                        selectedEventListView + 2,
                        Utils.convertDpToPixel(0.0).toInt()
                    )
                }
            }

        }

    }

    private fun tuneBackToActiveFastChannel() {
        if(((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56() && ReferenceApplication.isRegionSupported){
            (listener as GuideSceneWidgetListener).getActiveFastChannel(object : IAsyncDataCallback<TvChannel>{
                override fun onFailed(error: Error) {}
                override fun onReceive(data: TvChannel) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: tuning to last active fast channel, channel name === ${data.name}")
                    (listener as GuideSceneWidgetListener).tuneToFocusedChannel(data)
                }
            })
        }
    }

    private fun updateActiveWindow() {
        val tempVisibleChannelList = guideChannelListAdapter.getVisibleChannels()
        if(!listOfVisibleChannelsForActiveWindow.containsAll(tempVisibleChannelList)){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateActiveWindow: updating active window")
            listOfVisibleChannelsForActiveWindow.clear()
            listOfVisibleChannelsForActiveWindow.addAll(tempVisibleChannelList)
            (listener as GuideSceneWidgetListener).setActiveWindow(listOfVisibleChannelsForActiveWindow, (listener as GuideSceneWidgetListener).getStartTimeForActiveWindow())
        } else{
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "updateActiveWindow: no need to update active window")
        }
    }


    private fun onDownPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (eventClickAnimation || isLoading) return
        if (selectedEventListView < eventsContainerAdapter.itemCount - 1) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView += 1
            if (eventInfoView != null) {
                view!!.removeView(eventInfoView!!.itemView)
                eventInfoView?.rootView?.visibility = View.GONE
                eventInfoView = null
            }
            guideChannelListGridView?.itemAnimator = null
            eventsContainerAdapter.showFocusOnEventFrame(
                selectedEventListView,
                currentTime,
                lastHorizontallyFocusedEvent,
                infoEvent,
                getTimelineStartTime(),
                getTimelineEndTime(),
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: GuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                        tuneToFocusedChannel()
                    }
                }
            )

            if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                setSelectedPosition(selectedEventListView + 2, Utils.convertDpToPixel(20.0).toInt())
            } else {
                setSelectedPosition(selectedEventListView + 2, Utils.convertDpToPixel(0.0).toInt())
            }

            val channelsOfSelectedFilter = (listener as GuideSceneWidgetListener).getChannelsOfSelectedFilter()

            if (selectedEventListView > channelList!!.size - 6 && selectedEventListView < channelsOfSelectedFilter.size && channelList!!.last() != channelsOfSelectedFilter
                    .last()
            ) {
                var isExtend = false

                if (eventInfoView != null) {
                    view!!.removeView(eventInfoView!!.itemView)
                    eventInfoView?.rootView?.visibility = View.GONE
                    eventInfoView = null
                }


                var startDayOffset = dayOffset + leftDayOffset
                var endDayOffset = dayOffset + rightDayOffset


                if (endDayOffset != startDayOffset) {
                    isExtend = true
                }
                isLoading = true
                (listener as GuideSceneWidgetListener).loadNextChannels(
                    channelList!!.last(),
                    object :
                        IAsyncDataCallback<java.util.LinkedHashMap<Int, MutableList<TvEvent>>> {
                        override fun onReceive(data: java.util.LinkedHashMap<Int, MutableList<TvEvent>>) {
                            tempChannelList!!.clear()
                            tempEventContainerList!!.clear()
                            for ((key, value) in data) {
                                if(!(hideLockedServices && (value[0].tvChannel).isLocked)) {
                                        if (!isChannelPresentInList(
                                                value[0].tvChannel,
                                                channelList!!
                                            )
                                        ) {
                                            channelList!!.add(value[0].tvChannel)
                                            tempChannelList!!.add(value[0].tvChannel)
                                            eventContainerList!!.add(value)
                                            tempEventContainerList!!.add(value)
                                        }
                                    }
                            }
                            ReferenceApplication.runOnUiThread {
                                isLoading = false
                                guideChannelListGridView?.itemAnimator = null
                                guideChannelListAdapter.addNewChannelsToEnd(tempChannelList!!)
                                eventsContainerAdapter.addContainersToEnd(tempEventContainerList!!)
                                updatePastOverlay()
                            }

                        }

                        override fun onFailed(error: Error) {
                        }
                    },
                    startDayOffset,
                    endDayOffset, isExtend
                )

            }
        }
    }

    private fun isChannelPresentInList(channel: TvChannel,channelList: MutableList<TvChannel> ):Boolean{
        channelList?.forEach {
                tvChannel ->  if(channel.channelId == tvChannel.channelId)return true
        }
        return false
    }

    private fun onChannelDownPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        //if closing animation is there don't do anything
        if (eventClickAnimation || isLoading) return
        if (guideTimelineGridView!!.scrollState != 0) {
            return
        }
        if (eventInfoView != null) {
            view!!.removeView(eventInfoView!!.itemView)
            eventInfoView?.rootView?.visibility = View.GONE
            eventInfoView = null
        }
        if (selectedEventListView >= channelList!!.lastIndex - 6 && channelList!!.last() != (listener as GuideSceneWidgetListener).getChannelsOfSelectedFilter()
                .last()
        ) {
            var isExtend = false
            var startDayOffset = dayOffset + leftDayOffset
            var endDayOffset = dayOffset + rightDayOffset
            if (startDayOffset != endDayOffset) isExtend = true
            isLoading = true
            (listener as GuideSceneWidgetListener).loadNextChannels(
                channelList!!.last(),
                object : IAsyncDataCallback<java.util.LinkedHashMap<Int, MutableList<TvEvent>>> {

                    override fun onReceive(data: java.util.LinkedHashMap<Int, MutableList<TvEvent>>) {
                        tempChannelList?.clear()
                        tempEventContainerList?.clear()
                        var index = 0
                        for ((key, value) in data) {
                            if(!(hideLockedServices && (value[0].tvChannel).isLocked)) {
                                if (!isChannelPresentInList(value[0].tvChannel, channelList!!)) {
                                    channelList!!.add(value[0].tvChannel)
                                    eventContainerList!!.add(value)
                                    tempChannelList?.add(value[0].tvChannel)
                                    tempEventContainerList!!.add(value)
                                    index++
                                }
                            }
                        }

                        ReferenceApplication.runOnUiThread {
                            guideChannelListAdapter.addNewChannelsToEnd(tempChannelList!!)
                            eventsContainerAdapter.addContainersToEnd(tempEventContainerList!!)

                            guideChannelListGridView!!.itemAnimator = null
                            guideEventsContainer!!.itemAnimator = null
                            updatePastOverlay()

                            eventsContainerAdapter.clearFocus(selectedEventListView)
                            selectedEventListView =
                                if (selectedEventListView + 6 <= channelList!!.lastIndex) (selectedEventListView + 6) else channelList!!.lastIndex

                            if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                                setSelectedPosition(
                                    selectedEventListView + 2,
                                    Utils.convertDpToPixel(20.0).toInt()
                                )
                            } else {
                                setSelectedPosition(
                                    selectedEventListView + 2,
                                    Utils.convertDpToPixel(0.0).toInt()
                                )
                            }
                            guideEventsContainer?.postDelayed({
                                eventsContainerAdapter.showFocusOnEventFrame(
                                    selectedEventListView,
                                    currentTime,
                                    lastHorizontallyFocusedEvent,
                                    infoEvent,
                                    getTimelineStartTime(),
                                    getTimelineEndTime(),
                                    object : EpgEventFocus {
                                        override fun onEventFocus(
                                            lastSelectedView: GuideEventListViewHolder,
                                            isSmallDuration: Boolean
                                        ) {
                                            isLoading = false
                                            onSmallItemFocus(
                                                lastSelectedView,
                                                isSmallDuration,
                                                true
                                            )
                                            updateTextToSpeechForTheEvent()
                                        }
                                    }
                                )
                            }, 0)
                        }

                    }

                    override fun onFailed(error: Error) {
                    }

                },
                startDayOffset, endDayOffset, isExtend
            )
        } else {
            var selected =
                if (selectedEventListView + 6 < eventsContainerAdapter.itemCount - 1) selectedEventListView + 6 else eventsContainerAdapter.itemCount - 1
            if (selected != selectedEventListView && selectedEventListView < eventsContainerAdapter.itemCount - 4) {
                eventsContainerAdapter.clearFocus(selectedEventListView)
                selectedEventListView = selected
                if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                    setSelectedPosition(
                        selectedEventListView + 2,
                        Utils.convertDpToPixel(20.0).toInt()
                    )
                } else {
                    setSelectedPosition(
                        selectedEventListView + 2,
                        Utils.convertDpToPixel(0.0).toInt()
                    )
                }
                guideEventsContainer!!.postDelayed(Runnable {
                    guideEventsContainer!!.visibility = View.VISIBLE
                    eventsContainerAdapter.showFocusOnEventFrame(
                        selectedEventListView,
                        currentTime,
                        lastHorizontallyFocusedEvent,
                        infoEvent,
                        getTimelineStartTime(),
                        getTimelineEndTime(),
                        object : EpgEventFocus {
                            override fun onEventFocus(
                                lastSelectedView: GuideEventListViewHolder,
                                isSmallDuration: Boolean
                            ) {
                                onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                                updateTextToSpeechForTheEvent()
                            }
                        }
                    )
                }, 200)
            } else {
                eventsContainerAdapter.clearFocus(selectedEventListView)
                selectedEventListView = selected
                eventsContainerAdapter.showFocusOnEventFrame(
                    selectedEventListView,
                    currentTime,
                    lastHorizontallyFocusedEvent,
                    infoEvent,
                    getTimelineStartTime(),
                    getTimelineEndTime(),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: GuideEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                            updateTextToSpeechForTheEvent()
                        }
                    }
                )
            }
        }
    }

    private fun onChannelUpPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        //if closing animation is there dont do anthing
        if (eventClickAnimation||isLoading) return
        //return when reached to the first index
        if (selectedEventListView == 0 && channelList!!.first() == (listener as GuideSceneWidgetListener).getChannelsOfSelectedFilter()
                .first()
        ) {
            return
        }
        if (guideTimelineGridView!!.scrollState != 0) {
            return
        }
        if (eventInfoView != null) {
            view!!.removeView(eventInfoView!!.itemView)
            eventInfoView?.rootView?.visibility = View.GONE
            eventInfoView = null
        }
        if ((selectedEventListView < 6 && channelList!!.first().id != (listener as GuideSceneWidgetListener).getChannelsOfSelectedFilter()
                .first().id)
        ) {
            var isExtend = false

            var startDayOffset = dayOffset+leftDayOffset
            var endDayOffset = dayOffset + rightDayOffset
            if (startDayOffset != endDayOffset) isExtend = true
            isLoading = true
            (listener as GuideSceneWidgetListener).loadPreviousChannels(
                channelList!!.first(),
                object : IAsyncDataCallback<java.util.LinkedHashMap<Int, MutableList<TvEvent>>> {

                    override fun onReceive(data: java.util.LinkedHashMap<Int, MutableList<TvEvent>>) {
                        var index = 0
                        tempChannelList?.clear()
                        tempEventContainerList?.clear()
                        for ((key, value) in data) {
                            if(!(hideLockedServices && (value[0].tvChannel).isLocked)) {
                                if (!isChannelPresentInList(value[0].tvChannel, channelList!!)) {
                                    channelList!!.add(index, value[0].tvChannel)
                                    eventContainerList!!.add(index, value)
                                    tempChannelList?.add(index, value[0].tvChannel)
                                    tempEventContainerList?.add(index, value)
                                    index++
                                }
                            }
                        }

                        ReferenceApplication.runOnUiThread {
                            eventsContainerAdapter.clearFocus(selectedEventListView)

                            //position after adding new items
                            selectedEventListView += (index)

                            //position for next selected itemm
                            selectedEventListView =
                                if (selectedEventListView >= 6) selectedEventListView - 6 else 0

                            guideEventsContainer!!.itemAnimator = null
                            guideChannelListGridView!!.itemAnimator = null

                            guideChannelListAdapter.addNewChannelsToStart(tempChannelList!!)
                            eventsContainerAdapter.insertContainersToStart(tempEventContainerList!!)

                            if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                                setSelectedPosition(
                                    selectedEventListView + 2,
                                    Utils.convertDpToPixel(20.0).toInt()
                                )
                            } else {
                                setSelectedPosition(
                                    selectedEventListView,
                                    Utils.convertDpToPixel(0.0).toInt()
                                )
                            }
                            guideEventsContainer?.postDelayed({
                                eventsContainerAdapter.showFocusOnEventFrame(
                                    selectedEventListView,
                                    currentTime,
                                    lastHorizontallyFocusedEvent,
                                    infoEvent,
                                    getTimelineStartTime(),
                                    getTimelineEndTime(),
                                    object : EpgEventFocus {
                                        override fun onEventFocus(
                                            lastSelectedView: GuideEventListViewHolder,
                                            isSmallDuration: Boolean
                                        ) {
                                            isLoading = false
                                            onSmallItemFocus(
                                                lastSelectedView,
                                                isSmallDuration,
                                                true
                                            )
                                            updateTextToSpeechForTheEvent()
                                        }
                                    }
                                )

                            }, 0)
                        }
                    }

                    override fun onFailed(error: Error) {
                    }

                },
                startDayOffset, endDayOffset, isExtend
            )

        } else {
            if (eventInfoView != null && eventInfoView!!.rootView!!.visibility == View.VISIBLE) {
                eventInfoView!!.rootView!!.visibility = View.GONE
            }

            var selected = if (selectedEventListView - 6 >= 0) selectedEventListView - 6 else 0
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView = selected
            if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                setSelectedPosition(selectedEventListView + 2, Utils.convertDpToPixel(20.0).toInt())
            } else {
                setSelectedPosition(selectedEventListView, Utils.convertDpToPixel(0.0).toInt())
            }
            guideEventsContainer!!.postDelayed(Runnable {
                eventsContainerAdapter.showFocusOnEventFrame(
                    selectedEventListView,
                    currentTime,
                    lastHorizontallyFocusedEvent,
                    infoEvent,
                    getTimelineStartTime(),
                    getTimelineEndTime(),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: GuideEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                            updateTextToSpeechForTheEvent()
                        }
                    }
                )
            }, 200)

        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onItemClick() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (guideTimelineGridView!!.scrollState != 0) {
            return
        }
        selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
        if (selectedEvent != null) {
            if (eventInfoView != null) {
                eventInfoView!!.itemView.visibility = View.GONE
            }
            eventClickAnimation = true
            eventsContainerAdapter.staySelected(selectedEventListView)

            eventDetailsView = GuideEventDetailsView(ReferenceApplication.applicationContext(),
                object : GuideEventDetailsView.GuideEventDetailViewListener {
                    override fun onButtonClick(buttonType: ButtonType, callback: IAsyncCallback) {
                        //this method is called to restart inactivity timer for no signal power off
                        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                        if (selectedEvent != null) {
                            when (buttonType) {
                                ButtonType.START_OVER -> {
                                    (listener as GuideSceneWidgetListener).onCatchUpButtonPressed(
                                        selectedEvent!!
                                    )
                                }
                                ButtonType.WATCH -> {
                                    (listener as GuideSceneWidgetListener).onWatchButtonPressed(
                                        selectedEvent!!.tvChannel
                                    )
                                }
                                ButtonType.RECORD, ButtonType.CANCEL_RECORDING -> {
                                    (listener as GuideSceneWidgetListener).onRecordButtonPressed(
                                        selectedEvent!!, object : IAsyncCallback {
                                            override fun onFailed(error: Error) {
                                                callback.onFailed(error)
                                            }

                                            override fun onSuccess() {
                                                refreshRecordButton()
                                                callback.onSuccess()
                                            }
                                        })
                                }
                                ButtonType.WATCHLIST, ButtonType.WATCHLIST_REMOVE -> {
                                    (listener as GuideSceneWidgetListener).onWatchlistButtonPressed(
                                        selectedEvent!!
                                    )
                                    eventDetailsView!!.refreshWatchlistButton()
                                    CoroutineScope(Dispatchers.Default).launch {
                                        eventsContainerAdapter.refreshIndicators(
                                            selectedEventListView,
                                            selectedEvent != null,
                                            object : EpgEventFocus {
                                                override fun onEventFocus(
                                                    lastSelectedView: GuideEventListViewHolder,
                                                    isSmallDuration: Boolean
                                                ) {
                                                    updateTextToSpeechForTheEvent()
                                                }
                                            })
                                    }.apply(job::add)
                                }
                                ButtonType.ADD_TO_FAVORITES, ButtonType.EDIT_FAVORITES -> {
                                    favoritesOverlay?.bringToFront()
                                    favoritesOverlay?.elevation = 10f
                                    favoritesOverlay?.visibility = View.VISIBLE
                                    (favoritesGridView?.adapter as MultiCheckListAdapter).setSelectedItems(
                                        (listener as GuideSceneWidgetListener).getFavoriteSelectedItems(
                                            selectedEvent!!.tvChannel
                                        )
                                    )

                                    eventDetailsView?.selectFavoriteButton(false)
                                    favoritesGridView?.post {
                                        favoritesGridView?.layoutManager?.findViewByPosition(0)?.requestFocus()
                                    }
                                }
                                ButtonType.MORE_INFO -> {
                                    guideTimelineGridView!!.isLayoutFrozen = true
                                    (listener as GuideSceneWidgetListener).onMoreInfoButtonPressed(
                                        selectedEvent!!
                                    )
                                }
                                else -> {

                                }
                            }
                        }
                    }
                    override fun getRecordingInProgressTvChannel(): TvChannel? {
                        return (listener as GuideSceneWidgetListener).getRecordingInProgressTvChannel()
                    }

                    override fun isInWatchList(tvEvent: TvEvent): Boolean {
                        return (listener as GuideSceneWidgetListener).isInWatchList(tvEvent)
                    }

                    override fun isInRecordingList(tvEvent: TvEvent): Boolean {
                        return (listener as GuideSceneWidgetListener).isInRecordingList(tvEvent)
                    }

                    override fun isInFavoriteList(tvChannel: TvChannel): Boolean {
                        return (listener as GuideSceneWidgetListener).isInFavoriteList(tvChannel)
                    }

                    override fun isRecordingInProgress(): Boolean {
                        return (listener as GuideSceneWidgetListener).isRecordingInProgress()
                    }

                    override fun isClosedCaptionEnabled(): Boolean {
                        return (listener as GuideSceneWidgetListener).isClosedCaptionEnabled()

                    }

                    override fun isCCTrackAvailable(): Boolean? {
                        return (listener as GuideSceneWidgetListener).isCCTrackAvailable()
                    }

                    override fun onChannelDownPressed(): Boolean {
                        closeEventDetails(callback = object :IAsyncCallback{
                            override fun onFailed(error: Error) {}
                            override fun onSuccess() {
                               this@HorizontalGuideSceneWidget.onChannelDownPressed()
                            }
                        })
                        return true
                    }

                    override fun onChannelUpPressed(): Boolean {
                        closeEventDetails(callback = object :IAsyncCallback{
                            override fun onFailed(error: Error) {}
                            override fun onSuccess() {
                                this@HorizontalGuideSceneWidget.onChannelUpPressed()
                            }
                        })
                        return true
                    }

                    override fun getIsAudioDescription(type: Int): Boolean {
                        return (listener as GuideSceneWidgetListener).getIsAudioDescription(type)

                    }

                    override fun getIsDolby(type: Int): Boolean {
                        return (listener as GuideSceneWidgetListener).getIsDolby(type)

                    }

                    override fun isHOH(type: Int): Boolean {
                        return (listener as GuideSceneWidgetListener).isHOH(type)
                    }

                    override fun isTeleText(type: Int): Boolean {
                        return (listener as GuideSceneWidgetListener).isTeleText(type)
                    }

                    override fun getAudioChannelInfo(type: Int): String {
                        return (listener as GuideSceneWidgetListener).getAudioChannelInfo(type)

                    }
                    override fun getAudioFormatInfo(): String {
                        return (listener as GuideSceneWidgetListener).getAudioFormatInfo()
                    }

                    override fun onDigitPressed(digit: Int) {
                        onDigitPress(digit)
                    }
                    override fun getVideoResolution(): String {
                        return (listener as GuideSceneWidgetListener).getVideoResolution()
                    }

                    override fun getCurrentTime(tvChannel: TvChannel): Long {
                        return runBlocking {
                            return@runBlocking CoroutineScope(Dispatchers.IO).async{
                                return@async  (listener as GuideSceneWidgetListener).getCurrentTime(tvChannel)
                            }.await()
                        }
                    }

                    override fun isCurrentEvent(tvEvent: TvEvent): Boolean {
                        return (listener as GuideSceneWidgetListener).isCurrentEvent(tvEvent)
                    }

                    override fun getCurrentAudioTrack(): IAudioTrack? {
                        return (listener as GuideSceneWidgetListener).getCurrentAudioTrack()
                    }

                    override fun getAvailableAudioTracks(): List<IAudioTrack> {
                        return (listener as GuideSceneWidgetListener).getAvailableAudioTracks()
                    }

                    override fun getAvailableSubtitleTracks(): List<ISubtitle> {
                        return (listener as GuideSceneWidgetListener).getAvailableSubtitleTracks()
                    }

                    override fun getClosedCaption(): String? {
                        return (listener as GuideSceneWidgetListener).getClosedCaption()
                    }

                    override fun onKeyUp(): Boolean {
                        //this method is called to restart inactivity timer for no signal power off
                        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                        if (eventClickAnimation) return true
                        closeEventDetails(callback = object :IAsyncCallback{
                            override fun onFailed(error: Error) {}
                            override fun onSuccess() { onUpPressed() }
                        })
                        return true
                    }

                    override fun onKeyDown(): Boolean {
                        //this method is called to restart inactivity timer for no signal power off
                        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

                        if (eventClickAnimation) {
                            return true
                        }
                        if(favoritesOverlay?.visibility == View.VISIBLE) return true
                        closeEventDetails(callback = object :IAsyncCallback{
                            override fun onFailed(error: Error) {}
                            override fun onSuccess() {
                                this@HorizontalGuideSceneWidget.onDownPressed()
                            }
                        })
                        return true
                    }

                    override fun onBackPressed() : Boolean {
                        //this method is called to restart inactivity timer for no signal power off
                        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                        if (eventClickAnimation) {
                            return true
                        }

                        if (favoritesOverlay!!.visibility != View.VISIBLE){
                            closeEventDetails()
                        }

                        return true


                    }

                    override fun isEventLocked(tvEvent: TvEvent?) = (listener as GuideSceneWidgetListener).isEventLocked(tvEvent)
                    override fun getConfigInfo(nameOfInfo: String): Boolean {
                        return (listener as GuideSceneWidgetListener).getConfigInfo(nameOfInfo)
                    }

                    override fun stopSpeech() {
                        (listener as GuideSceneWidgetListener).stopSpeech()
                    }

                    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                        (listener as GuideSceneWidgetListener).setSpeechText(text = text, importance = importance)
                    }

                    override fun getCurrentSubtitleTrack(): ISubtitle? {
                        return (listener as GuideSceneWidgetListener).getCurrentSubtitleTrack()
                    }

                    override fun isSubtitleEnabled(): Boolean {
                        return (listener as GuideSceneWidgetListener).isSubtitleEnabled()
                    }

                }, (listener as GuideSceneWidgetListener).getDateTimeFormat())
            selectedEvent?.let {
                (listener as GuideSceneWidgetListener).getParentalRatingDisplayName(selectedEvent!!.parentalRating,
                    it
                )
            }?.let {
                eventDetailsView?.updateDetails(
                    selectedEvent!!,activeTvChannel,
                    it
                )
            }


            eventDetailsView!!.y = (Utils.getDimensInPixelSize(R.dimen.custom_dim_184)
                    + ((selectedEventListView + 1) * Utils.getDimensInPixelSize(R.dimen.custom_dim_50))
                    + (selectedEventListView * Utils.getDimensInPixelSize(R.dimen.custom_dim_6))).toFloat()
            view!!.addView(eventDetailsView)
            if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount >= 3) {
                setSelectedPosition(
                    selectedEventListView,
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_39)
                )
                eventDetailsView!!.y = Utils.getDimensInPixelSize(R.dimen.custom_dim_346).toFloat()
            } else {
                setSelectedPosition(0, 0)
            }

            eventDetailsView!!.requestFocus()


            guideChannelListAdapter.showSeparator(selectedEventListView, true)
            eventClickAnimation = true
            eventsContainerAdapter.showSeparator(
                selectedEventListView,
                true,
                object : EndingTranslationAnimation {
                    override fun onEndingShowAnimation() {
                        startOpeningAlphaAnimation()
                    }

                    override fun onEndingHideAnimation() {
                        TODO("Not yet implemented")
                    }

                })


        } else {
            setSelectedPosition(0, 0)
        }
    }

    interface EndingTranslationAnimation {
        fun onEndingShowAnimation()
        fun onEndingHideAnimation()
    }

    private fun startOpeningAlphaAnimation() {
        eventDetailsView?.alpha = 0f

        eventDetailsView!!.animate().setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                eventClickAnimation = false
                updatePastOverlay()

            }

            override fun onAnimationCancel(animation: Animator) {
                updatePastOverlay()

                eventClickAnimation = false
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        }).alpha(1f).duration = 300

    }

    var handler :Handler = Handler()

    var runnable: Runnable? = null
    private fun onSmallItemFocus(
        lastSelectedView: GuideEventListViewHolder,
        isSmallDuration: Boolean,
        isVerticallyScrolled: Boolean
    ) {

        CoroutineHelper.runCoroutine({
            if (eventInfoView != null) {
                view!!.removeView(eventInfoView!!.itemView)
                eventInfoView = null
            }

            lastSelectedViewHolder = lastSelectedView

            runnable?.let {
                handler.removeCallbacks(it)
            }

//            if (guideTimelineGridView!!.scrollState != 0) {
//                return
//            }
            infoEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)

            (listener as GuideSceneWidgetListener).setSpeechText(
                infoEvent?.name ?: ConfigStringsManager.getStringById("no_information")
            )

        if (infoEvent != null) {

                if(!isVerticallyScrolled) lastHorizontallyFocusedEvent = infoEvent

                if (!isSmallDuration) return@runCoroutine

                runnable = Runnable {


                    if (!guideTimelineGridView!!.hasFocus() || selectedEvent!=null){
                        return@Runnable
                    }
                    val itemview = LayoutInflater.from(context)
                        .inflate(R.layout.guide_event_list_item, view!!, false)

                    eventInfoView = GuideEventListViewHolder(itemview)
                    eventInfoView?.timeTextView!!.setDateTimeFormat((listener as GuideSceneWidgetListener).getDateTimeFormat())
                    var layoutParams = eventInfoView?.rootView?.layoutParams
                    layoutParams?.width = 500
                    eventInfoView?.rootView?.layoutParams = layoutParams
                    eventInfoView?.rootView?.invalidate()
                    eventInfoView?.itemView?.visibility = View.VISIBLE


                    updateSmallEventInfo()
                    view!!.addView(eventInfoView?.itemView)

                    val updatePositionRunnable = Runnable {
                        val rectf = Rect()
                        lastSelectedView.itemView.getGlobalVisibleRect(rectf)
                        if (eventInfoView != null) {
                            eventInfoView!!.itemView.y = rectf.top.toFloat()
                            eventInfoView!!.itemView.x = rectf.left.toFloat()
                        }
                    }

                    updatePositionRunnable.run()

                    eventInfoView!!.indicatorContainer!!.alpha = 0f
                    eventInfoView!!.nameTextView!!.alpha = 0f
                    eventInfoView!!.timeTextView!!.alpha = 0f

                    eventInfoView!!.backgroundView!!.scaleX = 0f
                    eventInfoView!!.backgroundView!!.pivotX = 0f

                    eventInfoView!!.backgroundView!!.animate().scaleX(1f).duration = 150

                    eventInfoView!!.indicatorContainer!!.animate().alpha(1f).duration = 200
                    eventInfoView!!.nameTextView!!.animate().alpha(1f).duration = 200
                    eventInfoView!!.timeTextView!!.animate().alpha(1f).duration = 200

                    if (scrollListener != null) {
                        guideEventsContainer!!.removeOnScrollListener(scrollListener!!)
                        guideTimelineGridView!!.removeOnScrollListener(scrollListener!!)
                    }

                    scrollListener = object : RecyclerView.OnScrollListener() {

                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            updatePositionRunnable.run()
                        }

                    }

                    val target =
                        if (isVerticallyScrolled) guideEventsContainer!! else guideTimelineGridView!!
                    target.addOnScrollListener(scrollListener!!)
                    target.post(updatePositionRunnable)

                }
                handler.postDelayed(runnable!!,500)

            }
        },Dispatchers.Main)



    }

    var scrollListener: RecyclerView.OnScrollListener? = null

    /**
     * update event info on expand and close detail scene
     */

    private fun updateSmallEventInfo() {
        if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.DEFAULT.ordinal) {
            (listener as GuideSceneWidgetListener).getActiveChannel(object :
                IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {}
                override fun onReceive(data: TvChannel) {
                    if (infoEvent!!.startTime <= Date(currentTime.time).time && infoEvent!!.endTime >= Date(currentTime.time).time && data!!.channelId == infoEvent!!.tvChannel.channelId) {
                        eventInfoView!!.currentlyPlayingIcon!!.visibility = View.VISIBLE
                    } else {
                        eventInfoView!!.currentlyPlayingIcon!!.visibility = View.GONE
                    }
                }
            })
        }

        //TODO Commented due to pending refactor
        if (eventInfoView!!.recordIndicator != null)
            eventInfoView!!.recordIndicator!!.visibility =
                if (infoEvent!!.startTime > Date(currentTime.time).time && (listener as GuideSceneWidgetListener).isInRecordingList(infoEvent!!)) View.VISIBLE else View.GONE
        if (eventInfoView!!.watchlistIndicator != null)
            eventInfoView!!.watchlistIndicator!!.visibility =
                if (infoEvent!!.startTime > Date(currentTime.time).time && (listener as GuideSceneWidgetListener).isInWatchList(infoEvent!!)) View.VISIBLE else View.GONE

        if ((listener as GuideSceneWidgetListener).isEventLocked(infoEvent)) {
            eventInfoView!!.setParentalRestriction()
        } else {
            eventInfoView!!.nameTextView?.text = infoEvent!!.name
        }
        eventInfoView!!.timeTextView?.time = infoEvent!!

        eventInfoView!!.nameTextView!!.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    "color_background"
                )
            )
        )
        eventInfoView!!.timeTextView!!.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    0.8
                )
            )
        )
        eventInfoView!!.backgroundView!!.background =
            ConfigColorManager.generateGuideFocusDrawable()
        eventInfoView!!.currentlyPlayingIcon!!.setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    1.0
                )
            )
        )
        eventInfoView!!.recordIndicator!!.setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    1.0
                )
            )
        )
        eventInfoView!!.watchlistIndicator!!.setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    1.0
                )
            )
        )
        eventInfoView!!.lockImageView.setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    1.0
                )
            )
        )
    }

    var eventClickAnimation = false

    /**
     * Close event details view
     */
    private fun closeEventDetails(callback: IAsyncCallback?=null,animationDuration: Long = 300L) {
        guideTimelineGridView!!.isLayoutFrozen = true
        guideTimelineGridView?.requestFocus()
        guideTimelineGridView!!.isLayoutFrozen = false
        if (eventInfoView != null) {
            eventInfoView!!.itemView.visibility = View.VISIBLE
        }

        if (eventClickAnimation) {
            return
        }
        eventClickAnimation = true

        eventDetailsView!!.alpha = 1f
        eventDetailsView!!.animate().alpha(0f).setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                eventClickAnimation = true

            }

            override fun onAnimationEnd(animation: Animator) {
                guideChannelListAdapter.showSeparator(selectedEventListView, false)
                eventsContainerAdapter.showSeparator(
                    selectedEventListView,
                    false,
                    object : EndingTranslationAnimation {
                        override fun onEndingShowAnimation() {}

                        override fun onEndingHideAnimation() {
                            selectedEvent = null
                            eventsContainerAdapter.clearSelected(selectedEventListView)
                            updatePastOverlay()
                            eventClickAnimation = false
                            if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
                                setSelectedPosition(selectedEventListView + 2, Utils.convertDpToPixel(20.0).toInt())
                            } else {
                                setSelectedPosition(0, 0)
                            }
                            if (eventInfoView != null && eventInfoView?.itemView?.isVisible != true) {
                                onSmallItemFocus(lastSelectedViewHolder!!, isSmallDuration = true, isVerticallyScrolled = true)
                            }
                            eventDetailsView?.removeAllViews().apply {
                                eventDetailsView!!.visibility = View.GONE
                                val delayTime = if(animationDuration!=0L) 500L else 0L
                                Handler().postDelayed({
                                    if (isRefreshPending) {
                                        refreshEPGUI()
                                        isRefreshPending = false
                                    }
                                },delayTime)
                                callback?.onSuccess()
                            }
                        }
                    })
            }

            override fun onAnimationCancel(animation: Animator) {
                selectedEvent = null
                eventsContainerAdapter.clearSelected(selectedEventListView)
                updatePastOverlay()
                eventClickAnimation = false
                eventDetailsView?.removeAllViews().apply {
                    eventDetailsView!!.visibility = View.GONE
                    callback?.onSuccess()
                }
            }

            override fun onAnimationRepeat(animation: Animator) {
            }
        }).duration = animationDuration

    }

    /**
     * Refresh details favorite button
     */
    override fun refreshFavoriteButton() {
        if (selectedEvent != null) {
            eventDetailsView?.refreshFavoriteButton()
        }
    }

    override fun refreshRecordButton() {
        if (selectedEvent != null) {
            eventDetailsView?.refreshRecordButton()
            refreshIndicators()
        }
    }

    override fun refreshWatchlistButton() {
        eventDetailsView?.refreshWatchlistButton()
    }
    override fun refreshIndicators() {
        channelList?.forEachIndexed { index, _ ->
            CoroutineScope(Dispatchers.Main).launch {
                eventsContainerAdapter.refreshIndicators(
                    index,
                    selectedEvent != null,
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: GuideEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            updateTextToSpeechForTheEvent()
                        }
                    })
            }.apply(job::add)
        }
    }

    override fun setFocusToCategory() {
        if (guideFilterGridView!!.layoutManager!!.findViewByPosition(activeFilter) != null) {
            guideFilterGridView!!.layoutManager!!.findViewByPosition(activeFilter)!!.requestFocus()
        } else {
            guideFilterGridView!!.requestFocus()
        }
    }

    override fun setInitialData() {
    }

    override fun refreshGuideOnUpdateFavorite() {
        if (channelList==null || channelList!!.size<=selectedEventListView) return
        val favFilterForUpdatedChannels = (listener as GuideSceneWidgetListener).getFavoriteSelectedItems(
            channelList!![selectedEventListView]
        )

        /**
         * activefilter item contains list of fav where this channel belongs.
         * now if the current filter does not belong to the list then remove the channel
         */
        if (activeFilter >= 0 && filterList.size > activeFilter  && FilterItemType.getFilterTypeById(this.filterList[activeFilter].id) == FilterItemType.FAVORITE_ID) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "refreshGuideOnUpdateFavorite: active filter FilterItemType.FAVORITE_ID")
            //this condition means we have removed that channel from the current favorite so we need to update.
            if (!favFilterForUpdatedChannels.contains(activeFilterName)) {
                closeEventDetails(callback = object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                    }

                    override fun onSuccess() {
                        channelList!!.removeAt(selectedEventListView)
                        guideChannelListAdapter.removeChannel(selectedEventListView)
                        eventsContainerAdapter.removeChannelEvents(
                            selectedEventListView
                        )
                        updatePastOverlay()
                        val channelListSize = channelList!!.size
                        //no item remains in list
                        if (channelListSize == 0) {
                            guideFilterGridView!!.layoutManager!!.getChildAt(0)!!.requestFocus()
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(200)
                                yield()
                                //removing items other than 1st position
                                if (selectedEventListView > 0 && selectedEventListView == channelListSize) --selectedEventListView
                                if (eventsContainerAdapter.getEventListView(
                                        selectedEventListView
                                    ) != null
                                ) {
                                    eventsContainerAdapter.showFocusOnPositionFrame(
                                        selectedEventListView,
                                        getFirstVisibleChildIndex(
                                            eventsContainerAdapter.getEventListView(
                                                selectedEventListView
                                            )!!
                                        ),
                                        getTimelineStartTime(),
                                        getTimelineEndTime(),
                                        object : EpgEventFocus {
                                            override fun onEventFocus(
                                                lastSelectedView: GuideEventListViewHolder,
                                                isSmallDuration: Boolean
                                            ) {
                                                onSmallItemFocus(
                                                    lastSelectedView, isSmallDuration, false
                                                )
                                                updateTextToSpeechForTheEvent()
                                            }
                                        })
                                }
                            }.apply(job::add)

                        }

                    }

                })


            }
        }
        eventDetailsView?.selectFavoriteButton(true)
    }

    override fun getActiveCategory(): CategoryItem {
        return filterList[activeFilter]
    }

    /**
     * To get time for finding event while moving focus to different channel event
     */
    private fun getFocusSearchTime() : Date {
        val focusSearchTime = Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60))
        return if (currentTime.after(getTimelineStartTime()) && currentTime.before(getTimelineEndTime())) {
            currentTime
        } else {
            focusSearchTime
        }
    }

   var isRefreshPending = false
    override fun refreshEpg() {
        isLoading = true
        var isExtend = false
        val startDayOffset = dayOffset + leftDayOffset
        val endDayOffset = dayOffset + rightDayOffset
        if (startDayOffset != endDayOffset) isExtend = true
        (listener as GuideSceneWidgetListener).fetchEventForChannelList(object :
            IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: LinkedHashMap<Int, MutableList<TvEvent>>) {
                Handler(Looper.getMainLooper()).post {

                    eventContainerList?.clear()
                    for ((_, value) in data) {
                        eventContainerList!!.add(value)
                    }

                    //Collapse short event view if expanded while EPG refresh is called
                    if (eventInfoView != null) {
                        eventInfoView?.itemView?.visibility = View.GONE
                        eventInfoView = null
                    }

                    //if event detail view is open we will wait for it to close otherwise will refresh
                    if (selectedEvent!=null){
                        isRefreshPending = true
                    }else{
                        refreshEPGUI()
                    }
                }
            }

        }, channelList!!, startDayOffset, endDayOffset, isExtend)
    }

    /**
     * refreshing the ui of epg with the updated data
     */
    private fun refreshEPGUI(){
        isLoading =true
        // calculating time of last focused event so that focus can be regain on refresh.
        var lastFocusedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
        var lastFocusedTime= lastFocusedEvent?.startTime
        if (lastFocusedTime != null) {
            if (lastFocusedTime < getTimelineStartTime().time || lastFocusedTime < currentTime.time) {
                lastFocusedTime = getFocusSearchTime().time
            }
        }else{
            lastFocusedTime = getFocusSearchTime().time
        }


        try {
            // updating adapter
            ReferenceApplication.runOnUiThread {
                eventsContainerAdapter.refresh(eventContainerList!!)
            }
            guideEventsContainer?.doOnLayout {
                eventsContainerAdapter.scrollToTime(getTimelineStartTime())

                // Focusing event
                if (guideTimelineGridView!!.hasFocus()) {
                    eventsContainerAdapter.showFocus(
                        selectedEventListView,
                        Date(lastFocusedTime),
                        object : EpgEventFocus {
                            override fun onEventFocus(
                                lastSelectedView: GuideEventListViewHolder,
                                isSmallDuration: Boolean
                            ) {
                                onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                                updateTextToSpeechForTheEvent()
                            }
                        }
                    )
                }
            }
            isLoading = false

        }catch (e:Exception){
            isLoading = false
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "refreshEPGUI: ${e.message}")
        }
    }

    /**
     * Refresh guide filter list
     *
     * @param filterList new filter list
     */
    override fun refreshFilterList(filterList: MutableList<CategoryItem>) {
        ReferenceApplication.runOnUiThread{
            guideFilterCategoryAdapter.update(filterList)
            this.filterList.clear()
            this.filterList.addAll(filterList)
            var newActiveFilterPos = 0
            for (i in filterList) {
                if (activeFilterName == i.name) {
                    activeFilter = newActiveFilterPos
                    guideFilterCategoryAdapter.selectedItem = newActiveFilterPos
                }
                newActiveFilterPos += 1
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
        override fun zapOnGuideOnly(tvChannel: TvChannel) {
        ReferenceApplication.runOnUiThread{
            //if event is expanded first close it and then move focus else just move focus
            if(selectedEvent!=null){
                closeEventDetails(callback = object :IAsyncCallback{
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {
                        moveFocusOnZappedChannel(tvChannel)
                    }
                },0L)
            }else{
                moveFocusOnZappedChannel(tvChannel)
            }
        }

    }

    private fun moveFocusOnZappedChannel(tvChannel: TvChannel){
        if (eventInfoView != null) {
            view!!.removeView(eventInfoView!!.itemView)
            eventInfoView = null
        }
        //close the calender filter if it is oven while zapping performed
            if (calendarFilterContainer!!.visibility == View.VISIBLE){
                calendarFilterContainer!!.visibility = View.GONE
            }
        //checks if channel is present in current list if not we have to load
        var isPresentInChannelList = false
        channelList!!.forEach {
          if (it.channelId == tvChannel.channelId) {
              isPresentInChannelList = true
              return@forEach
          }
        }

        if (!isPresentInChannelList || selectedEventListView<channelList!!.size-4) {//this is to load more channels... in advance or if it is not in list
            var isExtend = false
            var startDayOffset = dayOffset + leftDayOffset
            var endDayOffset = dayOffset + rightDayOffset
            if (startDayOffset != endDayOffset) isExtend = true
            (listener as GuideSceneWidgetListener).getEventsForChannels(
                tvChannel,
                activeFilter,
                object :
                    IAsyncDataCallback<LinkedHashMap<Int, MutableList<TvEvent>>> {
                    override fun onReceive(data: LinkedHashMap<Int, MutableList<TvEvent>>) {
                        eventsContainerAdapter.clearFocus(selectedEventListView)
                        channelList!!.clear()
                        eventContainerList!!.clear()
                        var index = 0
                        for ((key, value) in data) {
                            if(!(hideLockedServices && (value[0].tvChannel).isLocked)) {
                                channelList!!.add(value[0].tvChannel)
                                eventContainerList!!.add(value)
                                if (value[0].tvChannel.channelId == tvChannel.channelId) {
                                    selectedEventListView = index
                                }
                                index++
                            }
                        }

                        ReferenceApplication.runOnUiThread {
                            if (pendingLayoutCreation) return@runOnUiThread
                            pendingLayoutCreation = true
                            guideTimelineGridView!!.postDelayed(Runnable {
                                pendingLayoutCreation = false
                            }, 500)
                            guideEventsContainer!!.itemAnimator = null
                            guideChannelListAdapter.refresh(channelList!!)
                            eventsContainerAdapter.refresh(eventContainerList!!)
                            setFocusOnZappedChannel()
                            clearFocus()
                            //when digit zapped from category, set category as active when focus moved to zapped channel.
                                val holder =
                                    guideFilterGridView!!.findViewHolderForAdapterPosition(
                                        activeFilter
                                    ) as CategoryItemViewHolder
                                guideFilterCategoryAdapter.setActiveFilter(holder)
                            //if focus on home filter grid view(broadcast button) then set it as active
                            (listener as GuideSceneWidgetListener).setGuideButtonAsActiveFilter()
                        }
                    }

                    override fun onFailed(error: Error) {
                        waitingForExtend = false
                        isLoading = false
                    }
                },
                startDayOffset, endDayOffset, isExtend
            )
        } else {
            CoroutineHelper.runCoroutineWithDelay({
                for (index in 0 until channelList!!.size) {
                    val channel = channelList!![index]
                    if (channel.id == tvChannel.id) {
                        selectedEventListView = index
                    }
                }
                guideEventsContainer!!.postDelayed(Runnable {
                    setFocusOnZappedChannel()
                    clearFocus()
                    //when digit zapped from category, set category as active when focus moved to zapped channel.
                    val holder =
                        guideFilterGridView!!.findViewHolderForAdapterPosition(
                            activeFilter
                        ) as CategoryItemViewHolder
                    guideFilterCategoryAdapter.setActiveFilter(holder)
                    //if focus on home filter grid view (broadcast button) then set it as active
                    (listener as GuideSceneWidgetListener).setGuideButtonAsActiveFilter()
                }, 0)

                //below is to fix text position while zap channel
                guideEventsContainer!!.post {
                    eventsContainerAdapter.updateTextPosition(
                        getTimelineStartTime()
                    )
                }
            }, 0, Dispatchers.Main)
        }
    }
    private fun clearFocus(){
        (listener as GuideSceneWidgetListener).clearFocusFromMainMenu()
        calendarBtnIcon?.clearFocus()
        guideChannelListGridView?.clearFocus()
    }


    private fun onBackPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (eventInfoView != null) {
            eventInfoView!!.itemView.visibility = View.GONE
            eventInfoView = null
        }
        if (selectedEvent != null) {
            if (eventClickAnimation) return
            closeEventDetails()

        } else {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            (listener as GuideSceneWidgetListener).requestFocusOnTopMenu()
            tuneBackToActiveFastChannel()
        }
    }

    /**
     * Set selected position with scroll extra inside the channel list and event list container
     *
     * @param selectedPosition selected position
     * @param scrollExtra
     */
    private fun setSelectedPosition(selectedPosition: Int, scrollExtra: Int) {
        if(!(listener as GuideSceneWidgetListener).isAccessibilityEnabled()) {
            guideEventsContainer?.scrollToPosition(0)
            guideEventsContainer?.setSelectedPosition(selectedPosition, scrollExtra)
        }
        guideChannelListGridView?.scrollToPosition(0)
        guideChannelListGridView?.setSelectedPosition(selectedPosition, scrollExtra)
    }

    /**
     * Move focus on the filter list
     *
     * @param selectFirst select first element inside the list
     * @param selectLast select last element inside the list
     */
    override fun selectedFilterList(selectFirst: Boolean, selectLast: Boolean) {
        guideFilterGridView!!.layoutManager!!.findViewByPosition(guideFilterCategoryAdapter.selectedItem)!!
            .requestFocus()

    }

    /**
     * Find first visible child index inside the list
     */
    private fun getFirstVisibleChildIndex(gridView: RecyclerView): Int {
        val layoutManager = gridView.layoutManager
        val start = layoutManager?.paddingLeft
        val childCount: Int = gridView.childCount
        for (i in 0 until childCount) {
            val childView = layoutManager!!.getChildAt(i)
            val childLeft = layoutManager.getDecoratedLeft(childView!!)
            val childRight = layoutManager.getDecoratedRight(childView)
            if ((childLeft + childRight) / 2 > start!!) {
                return gridView.getChildAdapterPosition(childView)
            }
        }
        if (gridView.adapter is GuideEventListAdapter) {
            return (gridView.adapter as GuideEventListAdapter).getSelectedIndex()
        }
        return -1
    }

    /**
     * Get the fist visible time inside the timeline
     */
    private fun getTimelineStartTime(): Date {
        var calendar = Calendar.getInstance()
        calendar.time = currentTime
        calendar.add(Calendar.DATE, leftDayOffset + dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Date(calendar.time.time + (currentTimePosition * TimeUnit.MINUTES.toMillis(30)))
    }

    /**
     * Get the last visible time inside the timeline
     */
    private fun getTimelineEndTime(): Date {
        var calendar = Calendar.getInstance()
        calendar.time = Date(currentTime.time)
        calendar.time = currentTime
        calendar.add(Calendar.DATE, leftDayOffset + dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val retDate =
            Date(
                calendar.time.time + ((currentTimePosition + GUIDE_TIMELINE_PAGE_SIZE - 1) * TimeUnit.MINUTES.toMillis(
                    30
                ))
            )
        if (currentTimePosition == timelineEndPosition() && retDate.hours == 23 && retDate.minutes == 30) {
            return Date(retDate.time + TimeUnit.MINUTES.toMillis(12))
        }
        return retDate
    }


    /**
     * Get the fist time inside the timeline
     */
    private fun getTimelineFirstTime(): Date {
        var calendar = Calendar.getInstance()
        calendar.time = Date(currentTime.time)
        calendar.time = currentTime
        calendar.add(Calendar.DATE, leftDayOffset + dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Date(calendar.time.time)
    }

    /**
     * Get the last time inside the timeline
     */
    private fun getTimelineLastTime(): Date {
        var calendar = Calendar.getInstance()
        calendar.time = Date(currentTime.time)
        calendar.time = currentTime
        calendar.add(Calendar.DATE, rightDayOffset + dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return Date(calendar.time.time)
    }

    /**
     * Update past overlay position
     */
    private fun updatePastOverlay() {
        val overlayCount = minOf(7, guideChannelListAdapter.itemCount)

        var overlayHeight =
            overlayCount * Utils.getDimensInPixelSize(R.dimen.custom_dim_56) - Utils.getDimensInPixelSize(
                R.dimen.custom_dim_6
            )

        if (selectedEvent != null) {
            overlayHeight += Utils.getDimensInPixelSize(R.dimen.custom_dim_186)
            overlayHeight += Utils.getDimensInPixelSize(R.dimen.custom_dim_6)
        }

        val startDate: Date = getTimelineStartTime()
        val endDate = Date(getTimelineEndTime().time + TimeUnit.MINUTES.toMillis(30))

        var minutes = 0

        if (currentTime.after(startDate)) {
            if (currentTime.after(endDate)) {
                minutes = 250
            } else {
                minutes = (currentTime.time - startDate.time).toInt() / 60000
            }
        }
        val pastOverlayParams = pastOverlay!!.layoutParams as ConstraintLayout.LayoutParams
        pastOverlayParams.height = overlayHeight
        if (minutes > 0) {
            val width = minutes * GUIDE_TIMELINE_MINUTE
            pastOverlayParams.width = width
        } else {
            pastOverlayParams.width = -3
        }
        pastOverlay!!.layoutParams = pastOverlayParams
    }

    /**
     * Init scroll position inside the grid
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initScrollPosition() {
        //Scroll on the current event
        val currentSlotIndex =
            ((currentTime.hours) * 2) + (if (currentTime.minutes >= 30) 1 else 0)
        val currentSlotOffset =
            if (currentTime.minutes > 30) currentTime.minutes - 30 else currentTime.minutes

        currentTimePosition = if (currentSlotOffset < 10) currentSlotIndex - 1 else currentSlotIndex
        currentTimePosition--

        if (currentTimePosition < 0) currentTimePosition = 0
        // extend timeline if it almost reached end of the day
        if(currentTimePosition > timelineEndPosition() && waitingForExtend){
            checkForExtendTimeline()
            return
        }else{
            waitingForExtend = false
        }

        if (dayOffset != 0) currentTimePosition = 0
        if (selectedEventListView >= 3 && eventsContainerAdapter.itemCount > 6) {
            setSelectedPosition(selectedEventListView + 2, Utils.convertDpToPixel(0.0).toInt())
        }
        guideEventsContainer!!.postDelayed({

            guideTimelineGridView!!.scrollToPosition(currentTimePosition)
            eventsContainerAdapter.scrollToTime(getTimelineStartTime())
            if (!isOpeningFirstTime) {
                if (eventsContainerAdapter.getEventListView(selectedEventListView) != null)
                    CoroutineScope(Dispatchers.Default).launch {
                        eventsContainerAdapter.showFocusOnPositionFrame(
                            selectedEventListView,
                            getFirstVisibleChildIndex(
                                eventsContainerAdapter.getEventListView(selectedEventListView)!!
                            ),
                            getTimelineStartTime(),
                            getTimelineEndTime(),
                            object : EpgEventFocus {
                                override fun onEventFocus(
                                    lastSelectedView: GuideEventListViewHolder,
                                    isSmallDuration: Boolean
                                ) {
                                    onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                                    updateTextToSpeechForTheEvent()
                                }
                            }
                        )
                    }.apply(job::add)
            }

            hideLoading()
            setVisibility(true)
            updatePastOverlay()

            if (!isOpeningFirstTime) {
                guideTimelineGridView!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        guideTimelineGridView!!.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                        guideTimelineGridView!!.isLayoutFrozen = true
                        guideTimelineGridView!!.requestFocus()

                        guideTimelineGridView!!.isLayoutFrozen = false

                        checkForExtendTimeline()
                    }
                })
            }
            isOpeningFirstTime = true
            updateDateText()
        }, 0)

    }

    /**
     * Set ui views visibility
     *
     * @param visible true to show, false to hide
     */
    private fun setVisibility(visible: Boolean) {
        var visibility = if (visible) View.VISIBLE else View.INVISIBLE
        dateTextView!!.visibility = visibility
        guideTimelineGridView!!.visibility = visibility
        if (!visible) {
            pastOverlay?.alpha = 0f
            guideChannelListGridView!!.alpha = 0f
            guideEventsContainer!!.alpha = 0f
        } else {
            guideChannelListGridView!!.visibility = visibility
            guideEventsContainer!!.visibility = visibility
            pastOverlay?.visibility = visibility
            pastOverlay?.alpha = 1f
            guideChannelListGridView!!.alpha = 1f
            guideEventsContainer!!.alpha = 1f
        }
    }

    /**
     * Show loading animation
     */
    private fun showLoading() {
        isLoading = true
    }

    /**
     * Hide loading animation
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun hideLoading() {
        isLoading = false
        if (HomeSceneBase.jumpGuideCurrentEvent) {
            setFocusOnEvent()
            HomeSceneBase.jumpGuideCurrentEvent = false
            (listener as GuideSceneWidgetListener).clearFocusFromMainMenu()
        }
        LoadingPlaceholder.hideLoadingPlaceholder(PlaceholderName.GUIDE, onHidden = {
            view!!.visibility = View.VISIBLE
            calendarBtnIcon!!.visibility = View.VISIBLE
        })
    }
    //set the focus on the current event of selected channel when zapped by number key on guide
    private fun setFocusOnZappedChannel(){
        setFocusOnPlayedChannel = false
        selectCalenderFilter(0)
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun setFocusOnEvent() {
        (listener as GuideSceneWidgetListener).getActiveChannel(object :
            IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: TvChannel) {
                try {
                    selectedEventListView = channelList?.indexOf(data)!!
                    if (selectedEventListView < 0) selectedEventListView = 0
                    guideTimelineGridView?.isLayoutFrozen = true
                    guideFilterCategoryAdapter.clearFocus(currentCategoryPosition)
                    guideTimelineGridView?.requestFocus()
                    guideTimelineGridView?.isLayoutFrozen = false

                    guideFilterGridView?.let {
                        if (it.findViewHolderForAdapterPosition(
                                activeFilter
                            ) != null
                        ) {
                            val holder =
                                it.findViewHolderForAdapterPosition(
                                    activeFilter
                                ) as CategoryItemViewHolder
                            guideFilterCategoryAdapter.setActiveFilter(holder)

                        }
                    }
                    if (eventsContainerAdapter.itemCount > 0) {
                        val date = Date(currentTime.time)
                        if ((date.hours == 23) && (date.minutes > 40)) date.minutes = 40
                        eventsContainerAdapter.showFocus(
                            selectedEventListView,
                            date,
                            object : EpgEventFocus {
                                override fun onEventFocus(
                                    lastSelectedView: GuideEventListViewHolder,
                                    isSmallDuration: Boolean
                                ) {
                                    onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                                    updateTextToSpeechForTheEvent()
                                }
                            }
                        )
                    } else {
                        guideFilterGridView!!.requestFocus()
                    }
                    setFocusOnCurrentEvent = true
                    (listener as GuideSceneWidgetListener).setActiveFilterOnGuide()

                }catch (E: Exception){
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${E.printStackTrace()}")
                }
            }
        })

    }

    private fun updateFavorites(){
        refreshFavoriteButton()
        refreshGuideOnUpdateFavorite()
        if (filtersUpdated) {
            refreshFilterList(updatedFilters)
            filtersUpdated = false
        }
    }

    override fun resume() {
        guideTimelineGridView!!.isLayoutFrozen = false
        updateFavorites()
        refreshRecordButton()

        //below is to fix rtl text position on resume when event is open
        guideEventsContainer!!.doOnPreDraw {
            eventsContainerAdapter.updateTextPosition(
                getTimelineStartTime()
            )
        }
        //getting focus back on guide on resume
        if (guideTimelineGridView!!.hasFocus()){
          Handler().postDelayed ({
               eventsContainerAdapter.showFocus(
                   selectedEventListView,
                   getFocusSearchTime(),
                   object : EpgEventFocus {
                       override fun onEventFocus(
                           lastSelectedView: GuideEventListViewHolder,
                           isSmallDuration: Boolean
                       ) {
                           onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                           updateTextToSpeechForTheEvent()
                       }
                   }
               )
           },0)
        }
    }

    override fun pause() {
        guideTimelineGridView!!.isLayoutFrozen = true
        //removing focus when we are on not guide and getting it back on resume.
        eventsContainerAdapter.clearFocus(selectedEventListView)
    }

    override fun channelChanged() {}

    private fun timelineEndPosition(): Int {
        return TIMELINE_SIZE - GUIDE_TIMELINE_PAGE_SIZE
    }

    private fun hoursIndex(date: Date): Int {
        val startDate = Date(date.time)
        startDate.hours = 0
        startDate.minutes = 0

        val initialHourOffset = startDate.timezoneOffset / 60

        val index = date.hours
        val hourOffset = date.timezoneOffset / 60

        return index + hourOffset - initialHourOffset
    }


    /**
     * Handles the guide background drawable based on the number of channels available.
     *
     * If channel count is 3 ,backgrokund will set transparent.
     * If channel count is > 3 ,background will set gradient.
     */
    private fun updateGuideBackgroundDrawable() {
        val channelCount = guideChannelListAdapter.itemCount
        var gradientBackground: View = view!!.findViewById(R.id.guide_fade_overlay)
        if (channelCount > 3) {
            Utils.makeGradient(
                gradientBackground,
                GradientDrawable.LINEAR_GRADIENT,
                GradientDrawable.Orientation.BOTTOM_TOP,
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ),
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ),
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_not_selected"),
                        0.0
                    )
                ),
                0.0F,
                0.0F
            )
        } else {
            gradientBackground.background = null
        }
    }

    //scroll epg to the right automatically.
    override fun scrollRight() {
        val doScroll =
            currentTime.after(Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(40)))
        if (doScroll) {
            //close favorite tab while scrolling
            if (favoritesOverlay?.visibility == View.VISIBLE) {
                favoritesListAdapter.adapterListener?.onBackPressed(0)
            }
            //closing event detail if it is open and then scrolling to the right
            if (selectedEvent != null) {
                closeEventDetails(callback = object :IAsyncCallback{
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {
                        onScrollRight(true)
                    }
                })
            } else {
                onScrollRight(true)
            }

        }
    }

    /**
     * when fetching failed should make isLoading false and waiting for extend false
     * other wise it won't load for other filters as well
     */
    override fun onFetchFailed() {
        isLoading = false
        waitingForExtend = false
    }

    override fun updateFavSelectedItem(items: ArrayList<String>,filters : ArrayList<CategoryItem>) {
        filtersUpdated = true
        updatedFilters.clear()
        updatedFilters.addAll(filters)
    }
    override fun onDigitPress(digit: Int){
        onDigitPressed(digit)
    }

    override fun refreshClosedCaption() {
        eventDetailsView?.updateCCInfo()
    }

    companion object {
        const val GUIDE_FILTER_ALL = 0
        val GUIDE_TIMELINE_ITEM_SIZE = Utils.getDimensInPixelSize(R.dimen.custom_dim_180)
        val GUIDE_TIMELINE_MINUTE = Utils.getDimensInPixelSize(R.dimen.custom_dim_6)
        const val GUIDE_TIMELINE_PAGE_SIZE = 5
        //showing data from current day
        const val DAY_OFFSET_MIN = 0
        const val DAY_OFFSET_MAX = 7
        const val ITEM_OFFSET_TRIGGER = 0
        var activeCategoryId = FilterItem.ALL_ID
        //Active filter id
        var activeFilter = GUIDE_FILTER_ALL
    }

    private fun initGradientBackgrounds() {
        defaultGradientBackground = GradientDrawable()
        defaultGradientBackground!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        defaultGradientBackground!!.orientation = GradientDrawable.Orientation.BL_TR
        val colors1 = intArrayOf( Color.parseColor("#0d0f12"), Color.parseColor("#242934"), Color.parseColor("#0d0f12"))
        defaultGradientBackground!!.colors = colors1
    }
}