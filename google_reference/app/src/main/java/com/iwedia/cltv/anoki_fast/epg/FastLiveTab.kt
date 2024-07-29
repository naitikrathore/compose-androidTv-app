package com.iwedia.cltv.anoki_fast.epg

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.FastCategoryAdapter
import com.iwedia.cltv.anoki_fast.FastCategoryItemViewHolder
import com.iwedia.cltv.anoki_fast.FastErrorInfo
import com.iwedia.cltv.anoki_fast.FastTosMoreInfo
import com.iwedia.cltv.anoki_fast.FastTosMoreInfoButtonActivity
import com.iwedia.cltv.anoki_fast.FastTosMoreInfoListener
import com.iwedia.cltv.components.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.scene.home_scene.HomeSceneBase
import com.iwedia.cltv.utils.Utils
import kotlinx.coroutines.*
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/**
 * Fast Live Tab
 *
 * @author Dejan Nadj
 */
@RequiresApi(Build.VERSION_CODES.R)
class FastLiveTab(context: Context,
    var focusCallback: (focus: Focus)-> Unit,
    var recreateCallback: ()-> Unit
): ConstraintLayout(context) {

    enum class Focus {
        REQUEST_TOP_MENU,
        CLEAR_FOCUS_FROM_MAIN_MENU,
        SELECT_ACTIVE_FILTER,
        REQUEST_FOCUS_OPTIONS
    }
    //Selected even list view
    var selectedEventListView = -1
    //Loading flag
    var isLoading = false
    //Day offset previous day, today or next day
    var dayOffset : Int = 0
    //Favorites list update in progress
    var isFavoritesUpdateInProgress = false
    private var isFirstTimeOpened = true
    var isPaused = false
    private var waitingForExtend = true
    var activeChannel : TvChannel? = null

    //Selected tv event displayed inside the details view
    private var selectedEvent: TvEvent? = null
        set(value) {
            field = value
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectedEventChanged: $value")
        }
    /**
     * [isFocusOnChannelList] is label used to mark whether user's commands should reflect on EPG events
     * or on the channels inside VerticalGridView. It's used to:
     *
     * 1) handle changing focus inside EPG - if set to true - when user clicks DPAD_UP or DOWN that
     *    will reflect to scrolling through channels, if set to false - when user clicks DPAD_UP or DOWN
     *    that will reflect to scrolling through EPG events.
     *
     * 2) handle DPAD_RIGHT and DPAD_LEFT. if focus is on the filter and user presses RIGHT focus should
     * be changed to channel list, not to events. [isFocusOnChannelList] is used to handle that behavior.
     */
    var isFocusOnChannelList = false

    //Scene layout views references
    val TAG = javaClass.simpleName
    private var channelFilterVerticalGridView: VerticalGridView? = null
    private var channelListGridView: VerticalGridView? = null
    private var channelFilterContainer: MaterialCardView?= null
    private var timelineGridView: HorizontalGridView? = null
    private var eventsContainer: VerticalGridView? = null
    private var eventInfoView: FastLiveTabEventListViewHolder? = null
    private var pastOverlay: RelativeLayout? = null
    private var selectedDayFilterOffset =0
    private var fastErrorInfo: FastErrorInfo?= null
    private var fastTosMoreInfo: FastTosMoreInfo?= null
    private var epgLoadingPlaceholder: LinearLayout?= null

    private lateinit var customDetails: CustomDetails.CustomDetailsFastEpg
    private var job = mutableListOf<Job>()
    //List adapters
    private var timelineAdapter = FastLiveTabTimelineAdapter()
    private var channelListAdapter = FastLiveTabChannelListAdapter(
        favoritesCheck = {
            FastLiveTabDataProvider.isInFavorites(it)
        }, ttsSetterForSelectableViewInterface = FastLiveTabDataProvider
    )

    private var channelFilterAdapter : FastCategoryAdapter = FastCategoryAdapter(FastLiveTabDataProvider)

    private var pastOverlayAnimator: ValueAnimator? = null

    var categoryList = ArrayList<Category>()
    private var isEventClicked = false
    private var isDetailVisible = false
    private var initialized = false

    private var channelFilterAdapterListener =  object : FastCategoryAdapter.ChannelListCategoryAdapterListener {

        override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
            FastLiveTabDataProvider.setSpeechText(text = text, importance = importance)
        }

        override fun onItemSelected(position: Int, onRefreshFinished: () -> Unit) {
            if (timelineGridView?.selectedPosition!! != -1) {
                waitingForExtend = true
                isLoading = true
                setVisibility(false)
                initTimeline()
                currentTimePosition = 0
                dayOffset=selectedDayFilterOffset
                leftDayOffset = 0
                rightDayOffset = 0
                if(!FastLiveTabDataProvider.isAccessibilityEnabled()) {
                    eventsContainer?.setSelectedPosition(0, 0)
                }
                channelListGridView?.setSelectedPosition(0, 0)
                onFilterSelected(position,onRefreshFinished)
            } else {
                timelineGridView?.isLayoutFrozen = true
                timelineGridView?.requestFocus()
                timelineGridView?.isLayoutFrozen = false
            }
            updateGuideBackgroundDrawable()
        }

        override fun onKeyLeft(currentPosition: Int): Boolean {
            return true
        }

        override fun isDataLoading(): Boolean = isLoading

        override fun onKeyRight() {
            if (isLoading) return
            requestFocusOnChannelFilter()
        }

        override fun onKeyUp(isFromFirstItem: Boolean) {
            if (isFromFirstItem) {
                focusCallback(Focus.REQUEST_TOP_MENU)
            }
        }

        override fun onItemClicked(position: Int) {
            if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
                return
            }
            if (isLoading) {
                channelFilterAdapter.holders[position]!!.customButton.keepFocus(false)
                return
            }
            if (position == channelFilterAdapter.activeItemPosition && channelListAdapter.itemCount != 0) {
                timelineGridView?.isLayoutFrozen = true
                timelineGridView?.requestFocus()
                timelineGridView?.isLayoutFrozen = false
                try {
                    if (eventsContainerAdapter.itemCount > 0) {
                        if (eventsContainerAdapter.getEventListView(selectedEventListView) != null) {
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
                                        lastSelectedView: FastLiveTabEventListViewHolder,
                                        isSmallDuration: Boolean
                                    ) {
                                        onSmallItemFocus(
                                            lastSelectedView,
                                            isSmallDuration,
                                            false
                                        )
                                        updateDataForCustomDetails()
                                        customDetails.animateVisibility(true)
                                    }
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (timelineGridView?.selectedPosition!! != -1) {
                setVisibility(false)
                timelineGridView?.scrollTo(0, 0)
                initTimeline()
                currentTimePosition = 0
                leftDayOffset = 0
                rightDayOffset = 0
                if(!FastLiveTabDataProvider.isAccessibilityEnabled()) {
                    eventsContainer?.setSelectedPosition(0, 0)
                }
                selectedEventListView = 0
                channelListGridView?.setSelectedPosition(0, 0)
                onFilterSelected(position)
            } else {
                timelineGridView?.isLayoutFrozen = true
                timelineGridView?.requestFocus()
                timelineGridView?.isLayoutFrozen = false
            }
        }

        override fun onBackPressed(position: Int) {
            customDetails.animateVisibility(false)
            focusCallback(Focus.REQUEST_TOP_MENU)
        }
    }

    private var eventsContainerAdapter = FastLiveTabEventsContainerAdapter(object :
    FastLiveTabEventsContainerAdapter.GuideEventsContainerListener {
        override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
            if (activeChannel!=null){
                callback.onReceive(activeChannel!!)
            }else{
                FastLiveTabDataProvider.getActiveChannel { tvChannel ->
                    callback.onReceive(tvChannel)
                }
            }
        }

        override fun getStartTime(): Date {
            return getTimelineFirstTime()
        }

        override fun getEndTime(): Date {
            return getTimelineLastTime()
        }

        override fun getCurrentTime(tvChannel: TvChannel): Long {
            return System.currentTimeMillis()
        }

        override fun getTimelineStartTime(): Date {
            return this@FastLiveTab.getTimelineStartTime()
        }

        override fun isEventLocked(tvEvent: TvEvent?) = FastLiveTabDataProvider.isEventLocked(tvEvent)
    }, FastLiveTabDataProvider.getDateTimeFormat())
    private var infoEvent: TvEvent? = null

    //Current time
    private var currentTime: Date? = null

    // TODO:  
    //timeline position
    private var currentTimePosition = 0
    private var updateScrollPosition = false
    var eventContainerList: MutableList<MutableList<TvEvent>>? = null


    //timeline layout creation progress
    var pendingLayoutCreation = false

    val focusChangeDelay = 300L

    //pagination
    var leftDayOffset = 0
    var rightDayOffset = 0

    //to prevent focus from going to the guideview for the first time
    var shouldFocusCurrentEvent: Boolean = false

    var timelineSize = 48
    private var mChannelList: MutableList<TvChannel> = mutableListOf()
    private var mUniqueChannelListId : ArrayList<String> = arrayListOf()
    private var lastSelectedViewHolder: FastLiveTabEventListViewHolder? = null
    var filterList = mutableListOf<CategoryItem>()

    var tempEventContainerList: MutableList<MutableList<TvEvent>>? = mutableListOf()
    var tempChannelList: MutableList<TvChannel>? = mutableListOf()
    var activeFilterName: String? = null
    //RLT
    var isRTL: Boolean =
        (context as MainActivity).window.decorView.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
    private var currentTimeLong: Long = 0
    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        LayoutInflater.from(context).inflate(R.layout.layout_widget_guide_scene_fast, this, true)
        channelFilterAdapter.init(findViewById(R.id.fading_edge_layout))
        FastLiveTabDataProvider.setup()
        initialization()
    }

    private fun initialization() {
        customDetails = findViewById(R.id.custom_details)
        customDetails.textToSpeechHandler.setupTextToSpeechTextSetterInterface(FastLiveTabDataProvider)
        customDetails.visibilityListener = object : CustomDetails.CustomDetailsFastEpg.VisibilityListener {
            override fun onVisibilityChange(isVisible: Boolean, shouldAnimate: Boolean) {
                isDetailVisible = isVisible

                // Past overlay alignment
                updatePastOverlayAlignment(shouldAnimate)

                // Grid alignment
                val offset = Utils.getDimensInPixelSize(if (isVisible) R.dimen.custom_dim_13 else R.dimen.custom_dim_0)
                channelListGridView!!.windowAlignmentOffset = offset
                eventsContainer!!.windowAlignmentOffset = offset
            }
        }

        customDetails.layoutParams.height = 0
        channelFilterVerticalGridView = findViewById(R.id.channel_filter_vertical_grid_view)
        channelFilterVerticalGridView!!.setItemViewCacheSize(10)
        channelFilterContainer = findViewById(R.id.channel_filter_vertical_grid_view_container)

        timelineGridView = findViewById(R.id.guide_timeline_view)
        timelineGridView!!.setItemViewCacheSize(6)
        pastOverlay = findViewById(R.id.guide_past_overlay)
        pastOverlay?.visibility = View.GONE

        fastErrorInfo = findViewById(R.id.fast_live_tab_no_internet)
        fastErrorInfo?.textToSpeechHandler!!.setupTextToSpeechTextSetterInterface(FastLiveTabDataProvider)
        fastErrorInfo?.visibility = View.GONE

        fastTosMoreInfo = findViewById(R.id.tos_more_info)
        fastTosMoreInfo?.textToSpeechHandler?.setupTextToSpeechTextSetterInterface(FastLiveTabDataProvider)
        fastTosMoreInfo?.visibility = View.GONE
        fastTosMoreInfo?.setListener(object : FastTosMoreInfoListener {
            override fun showTos() {
                val intent =
                    Intent(ReferenceApplication.applicationContext(), FastTosMoreInfoButtonActivity::class.java)
                ReferenceApplication.getActivity().startActivityForResult(intent, 11111)
            }

            override fun requestFocusOnRail() {
            }

            override fun requestFocusOnTopMenu() {
                focusCallback(Focus.REQUEST_TOP_MENU)
            }
        })

        epgLoadingPlaceholder = findViewById(R.id.epg_loading_placeholder)

        val pastOverlaySeparator: View = findViewById(R.id.past_overlay_separator)
        pastOverlaySeparator.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_progress")))

        initTimeline()

        channelFilterVerticalGridView!!.setNumColumns(1)
        channelFilterVerticalGridView!!.adapter = channelFilterAdapter
        channelFilterVerticalGridView!!.setItemSpacing(Utils.getDimensInPixelSize(R.dimen.custom_dim_12)) // TODO BORIS/DEJAN remove utils dependency
        channelFilterVerticalGridView!!.preserveFocusAfterLayout = true
        channelListGridView = findViewById(R.id.guide_channel_list)
        channelListGridView!!.setItemViewCacheSize(10)
        channelListGridView!!.setNumColumns(1)

        channelListGridView!!.adapter = channelListAdapter
        channelListGridView!!.visibility = View.INVISIBLE
        channelListGridView!!.setItemSpacing(Utils.getDimensInPixelSize(R.dimen.custom_dim_6))
        channelListAdapter.isParentalEnabled(FastLiveTabDataProvider.isParentalEnabled())

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            channelListGridView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            channelListGridView?.isFocusable = true
            pastOverlay?.isFocusable = false
            pastOverlay?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            timelineGridView?.isFocusable = false
            timelineGridView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            channelFilterContainer?.isFocusable = false
            channelFilterContainer?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            channelFilterVerticalGridView?.isFocusable = false
            channelFilterVerticalGridView?.importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        eventsContainer = findViewById(R.id.guide_events_container)
        eventsContainer!!.setItemViewCacheSize(10)
        eventsContainer!!.visibility = View.INVISIBLE
        eventsContainer!!.adapter = eventsContainerAdapter

        channelListGridView!!.itemAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_6)
        channelListGridView!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE

        eventsContainer!!.itemAlignmentOffset = Utils.getDimensInPixelSize(R.dimen.custom_dim_6)
        eventsContainer!!.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE

        timelineGridView?.windowAlignment = VerticalGridView.WINDOW_ALIGN_HIGH_EDGE
        timelineGridView?.windowAlignmentOffsetPercent = 10f


        channelFilterAdapter.adapterListener = channelFilterAdapterListener

//        // Start loading animation
//        showLoading()

        // Pagination
        timelineGridView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (isLoading || newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return
                }
                checkForExtendTimeline()
            }
        })

        setVisibility(false)
        initialized = false
        if (!FastLiveTabDataProvider.isRegionSupported())
            showRegionNotSupported()
        else {
            FastLiveTabDataProvider.setInternetCallback {
                if (!it) {
                    FastLiveTabDataProvider.setActiveFilter(0)
                    channelFilterVerticalGridView?.clearFocus()
                    channelFilterAdapter.activeItemPosition = 0
                    channelFilterAdapter.selectedItemPosition = 0
                    initialized = false
                }
                showInternetInfo(it)
            }
            FastLiveTabDataProvider.setAnokiServerStatusCallback {
                showAnokiServerStatusInfo(it)
            }
            showInternetInfo(FastLiveTabDataProvider.hasInternet())
            showAnokiServerStatusInfo(FastLiveTabDataProvider.isAnokiServerReachable())

            if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
                channelListGridView!!.requestFocus()
            }
        }

    }

    /**
     * add channel to the channel list and also updates the unique id
     * this function should be used if a channel needs to be added in channel list
     */
    private fun addChannelToChannelList(tvChannel: TvChannel,position: Int? = null){
        val id = tvChannel.name
        if (position==null){
            mUniqueChannelListId.add(id)
            mChannelList.add(tvChannel)
        }else{
            mUniqueChannelListId.add(position,id)
            mChannelList.add(position,tvChannel)
        }
    }

    /**
     * clears the channel list
     */
    private fun clearChannelList(){
        mUniqueChannelListId.clear()
        mChannelList.clear()
    }

    /**
     * used to check if a channel already present in the list
     */
    private fun channelListContains(tvChannel: TvChannel) : Boolean {
        val id = tvChannel.name
        return mUniqueChannelListId.contains(id)
    }
    fun checkNetworkStatus() {
        if (!FastLiveTabDataProvider.hasInternet()) {
            setVisibility(false)
            customDetails.animateVisibility(false)
            channelFilterContainer?.alpha = 0f
            fastErrorInfo?.setTextInfo(FastErrorInfo.InfoType.NO_INTERNET)
            fastErrorInfo?.visibility = VISIBLE
            epgLoadingPlaceholder?.visibility = GONE
        } else if (!isAnokiServerReachable()) {
            showAnokiServerStatusInfo(false)
        } else {
            setVisibility(true)
            channelFilterContainer?.alpha = 1f
            fastErrorInfo?.visibility = INVISIBLE
        }
    }

    private fun showInternetInfo(hasInternet: Boolean) {
        if (hasInternet) {
            if (isAnokiServerReachable()) {
                setVisibility(false)
                channelFilterContainer?.alpha = 1f
                fastErrorInfo?.visibility = GONE
                showEpgData()
            } else {
                showAnokiServerStatusInfo(false)
            }
        } else {
            setVisibility(false)
            customDetails.animateVisibility(false)
            channelFilterContainer?.alpha = 0f
            fastErrorInfo?.setTextInfo(FastErrorInfo.InfoType.NO_INTERNET)
            focusCallback.invoke(Focus.REQUEST_TOP_MENU)
            fastErrorInfo?.visibility = VISIBLE
            epgLoadingPlaceholder?.visibility = GONE
        }
    }

    fun showRegionNotSupported(){
        setVisibility(false)
        customDetails.animateVisibility(false)
        channelFilterContainer?.alpha = 0f
        fastErrorInfo?.setTextInfo(FastErrorInfo.InfoType.REGION_NOT_SUPPORTED)
        focusCallback.invoke(Focus.REQUEST_TOP_MENU)
        fastErrorInfo?.visibility = VISIBLE
        epgLoadingPlaceholder?.visibility = GONE
    }

    private fun showAnokiServerStatusInfo(serverStatus: Boolean) {
        if (serverStatus) {
            if (fastErrorInfo!!.visibility == View.VISIBLE) {
                setVisibility(false)
                customDetails.animateVisibility(false)
                channelFilterContainer?.alpha = 1f
                fastErrorInfo?.visibility = GONE
                showEpgData()
            }
        } else if (hasInternet()){
            setVisibility(false)
            customDetails.animateVisibility(false)
            channelFilterContainer?.alpha = 0f
            fastErrorInfo?.setTextInfo(FastErrorInfo.InfoType.ANOKI_SERVER_DOWN)
            fastErrorInfo?.visibility = VISIBLE
            epgLoadingPlaceholder?.visibility = GONE
        }
    }

    fun showTosMoreInfo() {
        setVisibility(false)
        customDetails.animateVisibility(false)
        channelListGridView!!.alpha = 0f
        channelFilterContainer?.alpha = 0f
        channelFilterVerticalGridView?.alpha = 0f
        channelFilterVerticalGridView?.visibility = View.INVISIBLE
        fastTosMoreInfo?.visibility = View.VISIBLE
        epgLoadingPlaceholder?.visibility = View.GONE
    }

    private fun showEpgData() {
        if (!FastLiveTabDataProvider.isTosAccepted()) {
            showTosMoreInfo()
            return
        }
        if (initialized) {
            recreateCallback.invoke()
            return
        }
        //Init active filter
        FastLiveTabDataProvider.getAvailableFilters { filters ->
            if (filters != null) {

                FastLiveTabDataProvider.getActiveCategoryIndex { index ->
                    ReferenceApplication.runOnUiThread{
                        FastLiveTabDataProvider.setActiveFilter(index)

                        FastLiveTabDataProvider.getActiveChannel { tvChannel ->
                            FastLiveTabDataProvider.getEventsForChannels(
                                tvChannel,
                                index,
                                {
                                    Handler(Looper.getMainLooper()).post {
                                        refresh(filters)
                                    }
                                    for ((_, value) in it) {
                                        addChannelToChannelList(value[0].tvChannel)
                                    }
                                    if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
                                        updateTvGuide(it, 0, false)
                                    }else{
                                        updateTvGuide(it, 0, false)
                                    }

                                    ReferenceApplication.runOnUiThread{ // Waiting for calculating selectedEventListView
                                        initialized = true
                                    }
                                }, dayOffset, 0, false)
                        }
                    }

                }
            }
        }

    }

    private fun updateTvGuide(
        channelEventsMap: LinkedHashMap<Int, MutableList<TvEvent>>,
        additionalDayCount: Int,
        isExtend: Boolean,
        onRefreshFinished: () -> Unit = {}
    ) {
            if (!isExtend) {
                ReferenceApplication.runOnUiThread {
                    //Set current date
                    refresh(Date(System.currentTimeMillis()))
                    var channelList = mutableListOf<TvChannel>()
                    channelEventsMap.entries.forEach { map ->
                        val channel = map.value[0].tvChannel
                        if (channel.isBrowsable || channel.inputId.contains("iwedia") || channel.inputId.contains(
                                "sampletvinput"
                            )
                        ) {
                            channelList.add(channel)
                        }
                    }

                    //Set channel list
                    refresh(channelList, onRefreshFinished)

                    //Set event list
                    refresh(channelEventsMap, onRefreshFinished)
                }
            } else {
                //extend timeline , merge the previous data with the upcoming data
                extendTimeline(channelEventsMap, additionalDayCount)
            }
    }


    fun checkForExtendTimeline() {
        val totalItemCount = timelineGridView!!.adapter!!.itemCount
        if (dayOffset + leftDayOffset > DAY_OFFSET_MIN) {

            if (currentTimePosition - ITEM_OFFSET_TRIGGER <= 0) {
                isLoading = true
                FastLiveTabDataProvider.getEPGEventsData(
                    mChannelList,
                    dayOffset + leftDayOffset - 1,
                    true, callback = { channelEventsMap, guideChannelList, additionalDayCount, isExtend->
                        updateTvGuide(channelEventsMap, additionalDayCount, isExtend)
                    }
                )
            }
        }

        if (dayOffset + rightDayOffset < DAY_OFFSET_MAX) {
            if (GUIDE_TIMELINE_PAGE_SIZE + currentTimePosition + ITEM_OFFSET_TRIGGER >= totalItemCount) {
                isLoading = true
                FastLiveTabDataProvider.getEPGEventsData(
                    mChannelList,
                    dayOffset + rightDayOffset + 1,
                    true, callback = { channelEventsMap, guideChannelList, additionalDayCount, isExtend->
                        updateTvGuide(channelEventsMap, additionalDayCount, isExtend)
                    }
                )
            }
        }
    }

    fun refresh(data: Any, onRefreshFinished: () -> Unit = {}) {
        if (data is Date) {
            val lastCurrentTime: Date? = currentTime
            currentTime = Date(data.time)
            // handle timeline crossing a day
            if (lastCurrentTime != null && lastCurrentTime.date != currentTime!!.date)
                dayOffset -= 1
            updatePastOverlay()


            //checks if focused event is now past event then it jumps the focus on next event
            if(timelineGridView!!.hasFocus() && customDetails.visibility == View.VISIBLE && !isFocusOnChannelList){
                eventsContainerAdapter.isPastEventFocused(
                    selectedEventListView,
                    getTimelineEndTime(),
                    currentTimePosition >= timelineEndPosition(),
                    false,
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: FastLiveTabEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                            updateDataForCustomDetails()
                        }
                    })
            }

            if (!isLoading){
                // to refresh remaining time in current events
                eventsContainerAdapter.refreshRemainingTime()

                if(eventInfoView!=null){
                    setEventTime(eventInfoView!!, infoEvent!!)
                }

                //updates icons when playing event switches
                FastLiveTabDataProvider.getActiveChannel { channel->
                    activeChannel = channel
                    currentTimeLong = System.currentTimeMillis()
                    Handler(Looper.getMainLooper()).post {
                        mChannelList.forEachIndexed { index, tvChannel ->
                            if (tvChannel.name == channel.name) {
                                eventsContainerAdapter.refreshIndicators(
                                    index,
                                    selectedEvent!=null,
                                    object : EpgEventFocus {
                                        override fun onEventFocus(
                                            lastSelectedView: FastLiveTabEventListViewHolder,
                                            isSmallDuration: Boolean
                                        ) {
                                            onSmallItemFocus(
                                                lastSelectedView,
                                                isSmallDuration,
                                                false
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        else if (data is ArrayList<*> && data.isNotEmpty()) {

            if (data.first() is TvChannel) {
                val adapterItems = mutableListOf<TvChannel>()
                data.forEach { item ->
                    val channel = item as TvChannel
                    val added = adapterItems.find { it.name == channel.name }
                    if (added == null) {
                        adapterItems.add(channel)
                    }
                }
                clearChannelList()
                adapterItems.forEach {
                    addChannelToChannelList(it)
                }
                if (adapterItems.isNotEmpty()) {
                    channelListAdapter.refresh(adapterItems, onRefreshFinished)
                }
            }
            if (data.first() is Category) {
                FastLiveTabDataProvider.getActiveCategoryIndex { index ->
                    channelFilterAdapter.selectedItemPosition = index
                    channelFilterAdapter.activeItemPosition = index
                    val list = mutableListOf<CategoryItem>()

                    data.forEach { item ->
                        if (item is Category && (item.name!! !in mutableListOf(
                                "TV Channels",
                                "Tuner(DTV)"
                            ))
                        ) {
                            categoryList.add(item)
                            list.add(CategoryItem(item.id, item.name!!))
                        }
                    }

                    filterList = list

                    if (list.isNotEmpty()) {
                        ReferenceApplication.runOnUiThread{
                            channelFilterAdapter.refresh(list)
                            channelFilterVerticalGridView!!.post {
                                channelFilterVerticalGridView!!.scrollToPosition(channelFilterAdapter.selectedItemPosition)

                            }
                        }
                    }
                }

            }
        }
        else if (data is LinkedHashMap<*, *>) {
            showLoading()
            //check to reset selectedEventListView value to 0 if channel is not present in the current filter
            var activeChInCurrentList = false
            FastLiveTabDataProvider.getActiveChannel { tvChannel ->
                val eventListContainer = mutableListOf<MutableList<TvEvent>>()
                var index =0
                for ((_, value) in data) {
                    if (value == null) {
                        continue
                    }
                    if ((value as MutableList<TvEvent>)[0].tvChannel.name == tvChannel.name) {
                        activeChInCurrentList = true
                        selectedEventListView = index
                    }
                    index++
                    eventListContainer.add(value as MutableList<TvEvent>)
                    this.eventContainerList = eventListContainer
                }
                if(!activeChInCurrentList)selectedEventListView =0
                eventsContainer!!.setNumColumns(1)
                eventsContainer!!.setHasFixedSize(true)
                eventsContainer!!.itemAnimator = null
                if (eventListContainer.isNotEmpty()) {
                    eventsContainer!!.post {
                        updateGuideBackgroundDrawable()
                        eventsContainerAdapter.refresh(eventListContainer, onRefreshFinished)
                        initScrollPosition()
                    }
                }
            }
        }
    }

    fun dispose() {
        job.forEach { it.cancel() }
    }

    //Pagination
    fun extendTimeline(
        map: LinkedHashMap<Int, MutableList<TvEvent>>,
        additionalDayCount: Int
    ) {

        var dayCount = 0
        this.timelineAdapter.getItems().forEach {
            if (it == "12:00AM" || it == "00:00") {
                dayCount++
            }
        }


        //prepare timeline lihomesst
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTimeLong)
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
            timelineSize += list.size
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
        ReferenceApplication.runOnUiThread{
            if (timelineGridView!!.hasFocus()) {
                eventsContainerAdapter.showFocus(
                    selectedEventListView,
                    Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: FastLiveTabEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                        }
                    }
                )
            }
            timelineAdapter.extendTimeline(list, extendingPreviousDay, dayCount)
            timelineGridView!!.scrollToPosition(currentTimePosition)
            eventsContainerAdapter.extendTimeline(
                eventContainerList,
                extendingPreviousDay,
                selectedEventListView,
                timelineGridView!!.hasFocus(),
                dayCount,
                leftDayOffset + dayOffset
            )
        }
        eventsContainer?.post {
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                yield()
                updatePastOverlay()
                eventsContainerAdapter.scrollToTime(getTimelineStartTime())
                if (timelineGridView!!.hasFocus()) {
                    eventsContainerAdapter.showFocus(
                        selectedEventListView,
                        Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                        object : EpgEventFocus {
                            override fun onEventFocus(
                                lastSelectedView: FastLiveTabEventListViewHolder,
                                isSmallDuration: Boolean
                            ) {
                                onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                            }
                        }
                    )
                }
            }.apply(job::add)

            // When timeline is not initialized yet
            if(isLoading && waitingForExtend){
                initScrollPosition()
                waitingForExtend = false
            }else{
                hideLoading()
            }
        }
    }

    /**
     * Update event list data
     */
    fun updateEventList() {

        if(!hasInternet()) return

        if (mChannelList.size==0) return

        var isExtend = false
        val startDayOffset = dayOffset + leftDayOffset
        val endDayOffset = dayOffset + rightDayOffset
        if (startDayOffset != endDayOffset) isExtend = true

        isLoading = true

        FastLiveTabDataProvider.fetchEventForChannelList({ data->
            Handler(Looper.getMainLooper()).post {
                eventContainerList?.clear()
                var index = 0
                for ((_, value) in data) {
                    if (channelListContains(value[0].tvChannel)) {
                        eventContainerList!!.add(value)
                        index++
                    }
                }
                // calculating time of last focused event so that focus can be regain on refresh.
                var lastFocusedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
                var lastFocusedTime= lastFocusedEvent?.startTime
                if (lastFocusedTime != null) {
                    if (lastFocusedTime < getTimelineStartTime().time || lastFocusedTime < currentTime!!.time) {
                        lastFocusedTime = getFocusSearchTime().time
                    }
                }else{
                    lastFocusedTime = getFocusSearchTime().time
                }
                hideEventInfoView()
                // updating adapter
                eventsContainerAdapter.update(eventContainerList!!)

                eventsContainer!!.doOnPreDraw {
                    eventsContainerAdapter.scrollToTime(getTimelineStartTime())
                    // Focusing event
                    if (timelineGridView!!.hasFocus() && customDetails.visibility == View.VISIBLE && !isFocusOnChannelList) {
                        eventsContainerAdapter.showFocus(
                            selectedEventListView,
                            Date(lastFocusedTime),
                            object : EpgEventFocus {
                                override fun onEventFocus(
                                    lastSelectedView: FastLiveTabEventListViewHolder,
                                    isSmallDuration: Boolean
                                ) {
                                    onSmallItemFocus(
                                        lastSelectedView,
                                        isSmallDuration,
                                        false
                                    )
                                    updateDataForCustomDetails()
                                }
                            }
                        )
                    }
                    isLoading = false
                }
            }
        }, mChannelList, startDayOffset, endDayOffset, isExtend)
    }

    /**
     * On channel list updated
     */
    fun notifyChannelListUpdated() {
        if(!hasInternet()) return

        if (mChannelList.isEmpty() || filterList.isEmpty() ||
            selectedEventListView == -1 || selectedEventListView >= mChannelList.size) {
            // When there is no filter epg initialisation wont complete, it will stuck in loading screen
            // In such case we need to initialise the epg again when channel load is completed
            // Use case: opening the epg first time before fast channels loading complete.
            showEpgData()
            return
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "notifyChannelListUpdated: ")

        var isExtend = false
        val startDayOffset = dayOffset + leftDayOffset
        val endDayOffset = dayOffset + rightDayOffset
        if (startDayOffset != endDayOffset) isExtend = true

        isLoading = true

        val tvChannel = mChannelList[selectedEventListView]

        // calculating time of last focused event so that focus can be regain on refresh.
        val lastFocusedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
        var lastFocusedTime = lastFocusedEvent?.startTime
        if (lastFocusedTime != null) {
            if (lastFocusedTime < getTimelineStartTime().time || lastFocusedTime < currentTime!!.time) {
                lastFocusedTime = getFocusSearchTime().time
            }
        } else {
            lastFocusedTime = getFocusSearchTime().time
        }

        FastLiveTabDataProvider.getAvailableFilters { filters ->
            ReferenceApplication.runOnUiThread{
                if (filters != null) {
                    // Preparing filters
                    var activeFilterIndex = 0
                    val list = mutableListOf<CategoryItem>()
                    filters.toList().forEach { item ->
                        if ((item.name!! !in mutableListOf("TV Channels", "Tuner(DTV)"))) {
                            list.add(CategoryItem(item.id, item.name!!))
                            if (item.name.equals(channelFilterAdapter.getSelectedItem())) {
                                activeFilterIndex = filters.indexOf(item)
                            }
                        }
                    }

                    //Refresh channel filter list
                    filterList = list
                    channelFilterAdapter.selectedItemPosition = activeFilterIndex
                    channelFilterAdapter.activeItemPosition = activeFilterIndex
                    channelFilterAdapter.refresh(filterList)

                    //Getting channel & event list
                    FastLiveTabDataProvider.getEventsForChannels(
                        tvChannel,
                        activeFilterIndex,
                        {
                            ReferenceApplication.runOnUiThread {
                                // preparing data
                                clearChannelList()
                                eventContainerList?.clear()

                                selectedEventListView = 0
                                for ((_, value) in it) {
                                    val channel = value[0].tvChannel
                                    val added = mChannelList.find { it.name == channel.name }
                                    if (added == null) {
                                        addChannelToChannelList(channel)
                                        eventContainerList!!.add(value)
                                        if (tvChannel.name == channel.name) selectedEventListView = mChannelList.indexOf(channel)
                                    }
                                }

                                // updating adapter
                                channelListAdapter.refresh(mChannelList)
                                eventsContainerAdapter.update(eventContainerList!!)

                                // scrolling to channel
                                setSelectedPosition(selectedEventListView)

                                // waiting for adapter finish layout
                                eventsContainer!!.doOnPreDraw {

                                    eventsContainerAdapter.scrollToTime(getTimelineStartTime())

                                    if(timelineGridView!!.hasFocus()){
                                        if (isDetailVisible) {
                                            // Focusing event
                                            eventsContainerAdapter.showFocus(
                                                selectedEventListView,
                                                Date(lastFocusedTime),
                                                object : EpgEventFocus {
                                                    override fun onEventFocus(
                                                        lastSelectedView: FastLiveTabEventListViewHolder,
                                                        isSmallDuration: Boolean
                                                    ) {
                                                        onSmallItemFocus(
                                                            lastSelectedView,
                                                            isSmallDuration,
                                                            false
                                                        )
                                                        updateDataForCustomDetails()
                                                    }
                                                }
                                            )
                                        } else {
                                            // Focusing channel
                                            channelListAdapter.setStrokeColor(selectedEventListView)
                                        }
                                    }
                                    hideLoading()
                                }
                            }
                        },
                        startDayOffset,
                        endDayOffset,
                        isExtend)
                }else{
                    hideLoading()
                }
            }
        }
    }

    /**
     * method called when focus is on one of channel categories.
     */
    private fun onFilterSelected(filterPosition: Int, onRefreshFinished: () -> Unit = {}) {
        val duration = if (customDetailsVisibleBeforeFilterChange) 500L else 0L
        customDetailsVisibleBeforeFilterChange = false
        FastLiveTabDataProvider.onFilterSelected(filterPosition, timerDuration = duration) {
            if (channelFilterAdapter.activeItemPosition == filterPosition) {
                if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
                    updateTvGuide(it, 0, false, onRefreshFinished)
                }else{
                    updateTvGuide(it, 0, false, onRefreshFinished)
                }
            }
        }
    }

    /**
     * Updates the data for the custom details view based on the provided [TvEvent].
     *
     * This method retrieves the parental rating display name for the given [TvEvent].
     * If the event is current, it checks if the active TV channel matches the [TvEvent]'s channel.
     * If they match, it also retrieves available audio and subtitle tracks.
     *
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun updateDataForCustomDetails() {
        val tvEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)

        val parentalRating: String = FastLiveTabDataProvider.getParentalRatingDisplayName(tvEvent)

        var audioTracks: List<IAudioTrack>? = null
        var subtitleTracks: List<ISubtitle>? = null

        if (FastLiveTabDataProvider.isCurrentEvent(tvEvent)) {
            FastLiveTabDataProvider.isEventOnActiveChannel(tvEvent) {
                audioTracks = FastLiveTabDataProvider.getAvailableAudioTracks()
                subtitleTracks = FastLiveTabDataProvider.getAvailableSubtitleTracks()
            }
        }

        customDetails.updateData(
            tvEvent = tvEvent,
            parentalRatingDisplayName = parentalRating,
            currentTime = System.currentTimeMillis(),
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
            dateTimeFormat = FastLiveTabDataProvider.getDateTimeFormat(),
            isEventLocked = FastLiveTabDataProvider.isEventLocked(tvEvent)
        )
    }

    //Timeline initialization
    private fun initTimeline() {

        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTimeLong)
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

        timelineSize = list.size
        timelineAdapter.refresh(list)
        timelineGridView!!.setNumRows(1)
        timelineGridView!!.adapter = timelineAdapter

        timelineAdapter.guideTimelineAdapterListener =
            object : FastLiveTabTimelineAdapter.GuideTimelineAdapterListener {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun dispatchKey(keyCode: Int, keyEvent: KeyEvent): Boolean {
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_BACK -> {
                                onBackPressed()
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (isFocusOnChannelList) {
                                    if (isFavoritesUpdateInProgress) {
                                        return true
                                    }
                                    val selectedPos = selectedEventListView
                                    isFavoritesUpdateInProgress = true
                                    if(mChannelList.size != 0) {
                                        channelListAdapter.onItemClicked(selectedPos) {
                                            var tvChannel = mChannelList[selectedPos]
                                            isFavoritesUpdateInProgress = true
                                            FastLiveTabDataProvider.updateFavorites(
                                                tvChannel,
                                                channelFilterAdapter.getSelectedItem() == "Favorites"
                                            ) {
                                                //Refresh filter list
                                                FastLiveTabDataProvider.getAvailableFilters {
                                                    if (it?.size != filterList.size) {
                                                        val addedFavoritesFilter =
                                                            filterList.size < it!!.size
                                                        val list = mutableListOf<CategoryItem>()

                                                        it.forEach { item ->
                                                            if (item is Category && (item.name!! !in mutableListOf(
                                                                    "TV Channels",
                                                                    "Tuner(DTV)"
                                                                ))
                                                            ) {
                                                                categoryList.add(item)
                                                                list.add(
                                                                    CategoryItem(
                                                                        item.id,
                                                                        item.name!!
                                                                    )
                                                                )
                                                            }
                                                        }

                                                        filterList = list
                                                        ReferenceApplication.runOnUiThread {
                                                            refreshFilterList(
                                                                filterList,
                                                                addedFavoritesFilter
                                                            )
                                                            isFavoritesUpdateInProgress = false
                                                        }
                                                    } else {
                                                        //checks if we are pressing channel item in favorite filter,
                                                        // if yes we have to remove item from fav
                                                        if (channelFilterAdapter.getSelectedItem() == "Favorites") {
                                                            ReferenceApplication.runOnUiThread {
                                                                if (!mChannelList.contains(tvChannel)){
                                                                    isFavoritesUpdateInProgress = false
                                                                    return@runOnUiThread
                                                                }
                                                                val removePosition =
                                                                    mChannelList.indexOf(tvChannel)

                                                                mChannelList.remove(tvChannel)
                                                                channelListAdapter.removeStrokeColor(
                                                                    selectedEventListView
                                                                )
                                                                channelListAdapter.removeChannel(
                                                                    removePosition
                                                                )
                                                                //Set next category as active when favorites is removed
                                                                if (categoryList.isNotEmpty()) {
                                                                    FastLiveTabDataProvider.activeGenre =
                                                                        categoryList[0].name!!
                                                                    FastLiveTabDataProvider.categoryModule?.setActiveCategory(
                                                                        categoryList[0].name!!,
                                                                        ApplicationMode.FAST_ONLY
                                                                    )
                                                                }

                                                                eventsContainerAdapter.removeEventRow(
                                                                    removePosition
                                                                )

                                                                if (selectedEventListView > 0 && selectedEventListView >= removePosition) {
                                                                    --selectedEventListView
                                                                    setSelectedPosition(
                                                                        selectedEventListView
                                                                    )
                                                                }

                                                                channelListGridView?.doOnLayout {
                                                                    requestFocusOnChannelFilter()
                                                                    isFavoritesUpdateInProgress =
                                                                        false
                                                                }
                                                                val favList =
                                                                    FastLiveTabDataProvider.getChannelsOfSelectedFilter()
                                                                val thresholdForFavChannels = 7
                                                                //when less than 7 channels are present on fav category epg screen, it will refresh the fav list and will load new fav channels if they exist.
                                                                if (favList.size > mChannelList.size && mChannelList.size < thresholdForFavChannels) {
                                                                    if (favList.first().name != mChannelList.first().name) {
                                                                        onUpPressed()
                                                                    } else if (favList.last().name != mChannelList.last().name) {
                                                                        onDownPressed()
                                                                    }
                                                                }
                                                                updatePastOverlay()
                                                            }
                                                        } else {
                                                            isFavoritesUpdateInProgress = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else{
                                        isFavoritesUpdateInProgress = false
                                        focusCallback.invoke(Focus.REQUEST_TOP_MENU)
                                    }
                                } else {
                                    if (selectedEvent == null) {
                                        onItemClicked()
                                    }
                                }
                            }
                            KeyEvent.KEYCODE_MENU -> {
                                focusCallback(Focus.REQUEST_FOCUS_OPTIONS)
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
                                    if (selectedEventListView == 0) return true
                                    return true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    if (eventClickAnimation) return true
                                    return true
                                }
                            }
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        if (isLoading) {
                            return true
                        }
                        when (keyCode) {
                            if (isRTL) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (isFavoritesUpdateInProgress)return true
                                if (keyEvent.repeatCount % 5 == 0) {
                                    if (isFocusOnChannelList) {
                                        if (eventsContainerAdapter.itemCount > 0) {
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
                                                            lastSelectedView: FastLiveTabEventListViewHolder,
                                                            isSmallDuration: Boolean
                                                        ) {
                                                            updateDataForCustomDetails()
                                                            customDetails.animateVisibility(true)

                                                            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                                                                onSmallItemFocus(
                                                                    lastSelectedView,
                                                                    isSmallDuration,
                                                                    true
                                                                )
                                                            },100)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        channelListAdapter.removeStrokeColor(selectedEventListView)
                                        isFocusOnChannelList = false
                                    } else {
                                        updateScrollPosition = keyEvent.repeatCount != 0
                                        onScrollRight()
                                    }
                                }
                            }
                            if (isRTL) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (isFavoritesUpdateInProgress)return true
                                if (keyEvent.repeatCount % 5 == 0) {
                                    updateScrollPosition = keyEvent.repeatCount != 0
                                    if (isFocusOnChannelList) {
                                        channelListAdapter.removeStrokeColor(selectedEventListView)
                                        channelFilterVerticalGridView?.layoutManager?.findViewByPosition(channelFilterAdapter.activeItemPosition)?.requestFocus()
                                        isFocusOnChannelList = false
                                    }else{
                                        onScrollLeft()
                                    }
                                    return true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                if (selectedEventListView == 0 && isFavoritesUpdateInProgress)return true
                                if (keyEvent.repeatCount % 3 == 0) {
                                    onUpPressed()
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (selectedEventListView == mChannelList.lastIndex && isFavoritesUpdateInProgress)return true
                                if (keyEvent.repeatCount % 3 == 0) {
                                    onDownPressed()
                                }
                            }

                            KeyEvent.KEYCODE_CHANNEL_UP -> {
                                if (selectedEventListView == 0 && isFavoritesUpdateInProgress)return true
                                if(selectedEventListView == 0 && channelFilterAdapter.activeItemPosition == 0)return true
                                if (keyEvent.repeatCount % 3 == 0) {
                                    onUpPressed()
                                }
                            }

                            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                if (selectedEventListView == mChannelList.lastIndex && isFavoritesUpdateInProgress)return true
                                if (keyEvent.repeatCount % 3 == 0) {
                                    onDownPressed()
                                }
                            }
                        }
                        return false
                    }
                    return true
                }
            }
    }

    /**
     * method called when user presses DPAD_LEFT while focus is on some event.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun onScrollLeft() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (pendingLayoutCreation || eventClickAnimation) return

        if (pendingLayoutCreation) return
        pendingLayoutCreation = true
        timelineGridView!!.postDelayed({
            pendingLayoutCreation = false
        }, 200)

        val lastPosition = currentTimePosition
        if (lastPosition > 0) {
            var doScroll = false
            val selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
            if (selectedEvent != null) {
                //Prevent focus on past event
                //When the current event is focused left button should move focus on filter list

                val currentTime = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis())
                val sEventStartTime = TimeUnit.MILLISECONDS.toMinutes(selectedEvent.startTime)
                val sEventEndTime = TimeUnit.MILLISECONDS.toMinutes(selectedEvent.endTime )
                val isEventCurrentlyPlayed = currentTime in sEventStartTime..sEventEndTime
                val isEventFromThePast = sEventEndTime <= currentTime
                val isLongEvent = TimeUnit.MILLISECONDS.toMinutes(selectedEvent.endTime - selectedEvent.startTime) > TimeUnit.MINUTES.toMinutes(90L)

                if (isEventCurrentlyPlayed || isEventFromThePast) {
                    if (!isFocusOnChannelList) {
                        if (isLongEvent && (sEventStartTime < TimeUnit.MILLISECONDS.toMinutes(getTimelineStartTime().time) && TimeUnit.MILLISECONDS.toMinutes(getTimelineStartTime().time) > currentTime)) {
                            timelineGridView?.layoutManager?.findViewByPosition(lastPosition)
                                ?.requestFocus()
                            currentTimePosition -= 1
                            smoothScrollTimelineToPosition(currentTimePosition)
                            eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())
                            updatePastOverlay()
                        } else {
                            // move focus to channel list
                            timelineGridView?.isLayoutFrozen = true
                            hideEventInfoView()
                            customDetails.animateVisibility(false)
                            eventsContainerAdapter.clearFocus(selectedEventListView)
                            channelListAdapter.setStrokeColor(selectedEventListView)
                            timelineGridView?.layoutManager?.findViewByPosition(lastPosition)
                                ?.requestFocus()
                            timelineGridView?.isLayoutFrozen = false
                            isFocusOnChannelList = true
                        }
                        return
                    }
                }
                val selectedEventStartTime = selectedEvent.startTime
                val startTime = getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(31)
                if (getTimelineStartTime().before(Date(currentTime))) {
                    eventsContainerAdapter.findPreviousFocus(
                        selectedEventListView,
                        Date(getTimelineStartTime().time - TimeUnit.MINUTES.toMillis(30)),
                        true,
                        object : EpgEventFocus {
                            override fun onEventFocus(
                                lastSelectedView: FastLiveTabEventListViewHolder,
                                isSmallDuration: Boolean
                            ) {
                                onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                                updateDataForCustomDetails()
                            }
                        })
                    return
                } else if (startTime > selectedEventStartTime && lastPosition > 0) {
                    doScroll = true
                }
                if (doScroll) {
                    timelineGridView?.layoutManager?.findViewByPosition(lastPosition)
                        ?.requestFocus()
                    currentTimePosition -= 1
                    smoothScrollTimelineToPosition(currentTimePosition)
                    eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())
                    updatePastOverlay()
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
                    lastSelectedView: FastLiveTabEventListViewHolder,
                    isSmallDuration: Boolean
                ) {
                    onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                    updateDataForCustomDetails()
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onScrollRight(isForced: Boolean = false) {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (pendingLayoutCreation || eventClickAnimation) return
        pendingLayoutCreation = true
        timelineGridView!!.postDelayed({
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
            //this will check on auto scroll if we need to show focus on not
            if (timelineGridView!!.hasFocus()) {
                timelineGridView?.layoutManager?.findViewByPosition(lastPosition + 1)
                    ?.requestFocus()
            }else{
                timelineGridView?.selectedPosition = lastPosition+1
            }
            currentTimePosition += 1
            smoothScrollTimelineToPosition(currentTimePosition)
            val startHours = (timelineEndPosition()) / 2
            if (hoursIndex(getTimelineStartTime()) <= startHours) {
                eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())
            }
            updatePastOverlay()
        }
        if (timelineGridView!!.hasFocus() && customDetails.visibility == View.VISIBLE) {
            eventsContainerAdapter.findNextFocus(
                selectedEventListView,
                getTimelineEndTime(),
                currentTimePosition >= timelineEndPosition(),
                isForced,
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: FastLiveTabEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                        updateDataForCustomDetails()
                    }
                })
        }
    }


    interface EpgEventFocus {
        fun onEventFocus(lastSelectedView: FastLiveTabEventListViewHolder, isSmallDuration: Boolean)
    }

    /*
    * to smooth scroll timeline to position
    * */
    private fun smoothScrollTimelineToPosition(position: Int) {
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
        timelineGridView!!.stopScroll()
        timelineGridView!!.layoutManager!!.startSmoothScroll(smoothScroller)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onUpPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (eventClickAnimation||isLoading) return
        // Check if the selected event list view is the first one.

        val isFirstItemSelected = selectedEventListView == 0
        val isFirstCategoryInChannelFilterSelected = channelFilterAdapter.activeItemPosition != 0

        if (isFirstItemSelected && isFirstCategoryInChannelFilterSelected){
            changeChannelFilter(FastCategoryAdapter.Direction.UP)
            return
        }
        if (!mChannelList.isNullOrEmpty()
            && !FastLiveTabDataProvider.getChannelsOfSelectedFilter().isNullOrEmpty()
            && (mChannelList.size > selectedEventListView)
            && (selectedEventListView == 0 || mChannelList[selectedEventListView] == FastLiveTabDataProvider.getChannelsOfSelectedFilter().first()
        )) {
            hideEventInfoView()

            customDetails.animateVisibility(false)
            eventsContainerAdapter.clearFocus(selectedEventListView)
            channelListAdapter.removeStrokeColor(selectedEventListView)
            focusCallback(Focus.REQUEST_TOP_MENU) // move focus to the Top Menu
            return
        }
        //index out of bound CLTVFUNAI-254
        if (mChannelList.isNotEmpty() && FastLiveTabDataProvider.getChannelsOfSelectedFilter().isNotEmpty() && (mChannelList.size > selectedEventListView) &&(selectedEventListView < 5 && mChannelList[selectedEventListView].name != FastLiveTabDataProvider.getChannelsOfSelectedFilter()[selectedEventListView].name)) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            if (isFocusOnChannelList) channelListAdapter.removeStrokeColor(selectedEventListView)
            selectedEventListView--

            if (isFocusOnChannelList) {
                if (isFocusOnChannelList) channelListAdapter.setStrokeColor(selectedEventListView)
            } else {
                eventsContainerAdapter.showFocus(
                    selectedEventListView,
                    getFocusSearchTime(),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: FastLiveTabEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                            updateDataForCustomDetails()
                        }
                    }
                )

            }
            var isExtend = false
            val startDayOffset = dayOffset+leftDayOffset
            val endDayOffset = dayOffset + rightDayOffset
            if (startDayOffset != endDayOffset) isExtend = true

            isLoading = true
            FastLiveTabDataProvider.loadPreviousChannels(mChannelList[0], callback = { data->
                var index = 0
                tempChannelList!!.clear()
                tempEventContainerList!!.clear()
                for ((_, value) in data) {
                    if (!channelListContains(value[0].tvChannel)) {
                        addChannelToChannelList(value[0].tvChannel,index)
                        eventContainerList!!.add(index, value)
                        tempChannelList!!.add(index, value[0].tvChannel)
                        tempEventContainerList!!.add(index, value)

                        index++

                    }
                }
                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    channelListGridView?.itemAnimator = null
                    selectedEventListView += index
                    channelListAdapter.addNewChannelsToStart(tempChannelList!!)
                    eventsContainerAdapter.insertContainersToStart(tempEventContainerList!!)
                    setSelectedPosition(selectedEventListView)
                }

            }, startDayOffset, endDayOffset, isExtend)
        } else {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            if (isFocusOnChannelList) channelListAdapter.removeStrokeColor(selectedEventListView)
            selectedEventListView--

            if (isFocusOnChannelList) {
                channelListAdapter.setStrokeColor(selectedEventListView)
            } else {
                eventsContainerAdapter.showFocus(
                    selectedEventListView,
                    getFocusSearchTime(),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: FastLiveTabEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                            updateDataForCustomDetails()
                        }
                    }
                )

            }

            ReferenceApplication.runOnUiThread {
                setSelectedPosition(selectedEventListView)
            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onDownPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (eventClickAnimation || isLoading) return

        // Check if the selected event list view is the last one.
        val isLastItemSelected = selectedEventListView == eventsContainerAdapter.itemCount - 1
        val isLastCategoryInChannelFilterSelected = channelFilterAdapter.activeItemPosition == channelFilterAdapter.itemCount - 1

        if (isLastItemSelected && isLastCategoryInChannelFilterSelected.not()){
            changeChannelFilter(FastCategoryAdapter.Direction.DOWN)
            return
        }

        if (selectedEventListView < eventsContainerAdapter.itemCount - 1) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            channelListAdapter.removeStrokeColor(selectedEventListView)
            selectedEventListView += 1
            if (eventInfoView != null) {
                removeView(eventInfoView!!.itemView)
                eventInfoView?.rootView?.visibility = View.GONE
                eventInfoView = null
            }
            channelListGridView?.itemAnimator = null

            if (isFocusOnChannelList) {
                channelListAdapter.setStrokeColor(selectedEventListView)
            } else {
                eventsContainerAdapter.showFocus(
                    selectedEventListView,
                    getFocusSearchTime(),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: FastLiveTabEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(lastSelectedView, isSmallDuration, true)
                            updateDataForCustomDetails()
                        }
                    }
                )

            }

            setSelectedPosition(selectedEventListView)

            if (selectedEventListView > mChannelList.size - 5 && FastLiveTabDataProvider.getChannelsOfSelectedFilter().isNotEmpty() &&
                mChannelList.last() != FastLiveTabDataProvider.getChannelsOfSelectedFilter()
                    .last()
            ) {
                var isExtend = false

                if (eventInfoView != null) {
                    removeView(eventInfoView!!.itemView)
                    eventInfoView?.rootView?.visibility = View.GONE
                    eventInfoView = null
                }

                val startDayOffset = dayOffset + leftDayOffset
                val endDayOffset = dayOffset + rightDayOffset
                if (endDayOffset != startDayOffset) {
                    isExtend = true
                }
                isLoading = true
                FastLiveTabDataProvider.loadNextChannels(mChannelList.last(), { data->
                    tempChannelList!!.clear()
                    tempEventContainerList!!.clear()
                    for ((_, value) in data) {
                        if (!channelListContains(value[0].tvChannel)) {
                            addChannelToChannelList(value[0].tvChannel)
                            tempChannelList!!.add(value[0].tvChannel)
                            eventContainerList!!.add(value)
                            tempEventContainerList!!.add(value)
                        }
                    }
                    Handler(Looper.getMainLooper()).post {
                        isLoading = false
                        channelListGridView?.itemAnimator = null
                        channelListAdapter.addNewChannelsToEnd(tempChannelList!!)
                        eventsContainerAdapter.addContainersToEnd(tempEventContainerList!!)
                        updatePastOverlay()
                    }
                }, startDayOffset, endDayOffset, isExtend)
            }
        }
    }

    //when custom detail is visible then we are using delay for 500ms otherwise 0L
    //  [customDetailsVisibleBeforeFilterChange] is used to set this delay value
    var customDetailsVisibleBeforeFilterChange = false
    private fun changeChannelFilter(direction: FastCategoryAdapter.Direction) {
        hideEventInfoView()
        channelListAdapter.removeStrokeColor(selectedEventListView)
        if (!FastLiveTabDataProvider.isAccessibilityEnabled()) {
            channelListGridView?.clearFocus()
        }


        val channelFilterPosition = when (direction) {
            FastCategoryAdapter.Direction.UP -> {
                channelFilterAdapter.selectedItemPosition - 1
            }
            FastCategoryAdapter.Direction.DOWN -> {
                channelFilterAdapter.selectedItemPosition + 1
            }
        }
        if (!isFocusOnChannelList) customDetailsVisibleBeforeFilterChange = true
        channelFilterVerticalGridView!!.smoothScrollToPosition(channelFilterPosition)
        customDetails.animateVisibility(false)
        channelFilterAdapter.changeCategory(
            direction = direction,
            onTimerFinished = {
                requestFocusOnChannelFilter()
            }
        )
    }

    /**
     * Requests focus on the channel filter.
     */
    private fun requestFocusOnChannelFilter() {
        if (!FastLiveTabDataProvider.isTosAccepted()) {
            channelFilterVerticalGridView?.alpha = 0f
            channelFilterVerticalGridView?.visibility = View.INVISIBLE
            fastTosMoreInfo?.requestFocus()
            return
        }
        customDetails.animateVisibility(false)
        channelListAdapter.setStrokeColor(selectedEventListView)
        timelineGridView?.isLayoutFrozen = true
        timelineGridView?.requestFocus()
        timelineGridView?.isLayoutFrozen = false
        isFocusOnChannelList = true // focus should be changed to channel items
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onItemClicked() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        if (timelineGridView!!.scrollState != 0) {
            return
        }
        selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
        if (selectedEvent != null) {
            eventsContainerAdapter.onItemClicked(
                position = selectedEventListView,
                onClick = {
                    if (FastLiveTabDataProvider.isRecordingInProgress()) {
                        FastLiveTabDataProvider.showStopRecordingDialog(object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
                                selectedEvent = null // important for enabling DPAD keys again
                            }

                            override fun onSuccess() {
                                hideEventInfoView()
                                FastLiveTabDataProvider.onTvEventClicked(tvEvent = selectedEvent!!) {
                                    initTimeline()
                                    CoroutineScope(Dispatchers.Main).launch {
                                        initScrollPosition()
                                    }
                                    FastLiveTabDataProvider.activeGenre = channelFilterAdapter.getSelectedItem()
                                }
                                selectedEvent = null // important for enabling DPAD keys again
                                isEventClicked = true
                            }
                        })
                    } else if (FastLiveTabDataProvider.isTimeShiftActive()) {
                        FastLiveTabDataProvider.showStopTimeShiftDialog(object : IAsyncCallback {
                            override fun onFailed(error: Error) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
                                selectedEvent = null // important for enabling DPAD keys again
                            }

                            override fun onSuccess() {
                                hideEventInfoView()
                                FastLiveTabDataProvider.onTvEventClicked(tvEvent = selectedEvent!!) {
                                    initTimeline()
                                    CoroutineScope(Dispatchers.Main).launch {
                                        initScrollPosition()
                                    }
                                    FastLiveTabDataProvider.activeGenre = channelFilterAdapter.getSelectedItem()
                                }
                                selectedEvent = null // important for enabling DPAD keys again
                                isEventClicked = true
                            }
                        })
                    } else {
                        hideEventInfoView()
                        FastLiveTabDataProvider.onTvEventClicked(tvEvent = selectedEvent!!) {
                            initTimeline()
                            CoroutineScope(Dispatchers.Main).launch {
                                initScrollPosition()
                            }
                            FastLiveTabDataProvider.activeGenre = channelFilterAdapter.getSelectedItem()
                        }
                        selectedEvent = null // important for enabling DPAD keys again
                        isEventClicked = true
                    }
                }
            )
        } else {
            setSelectedPosition(0)
        }
    }

    interface EndingTranslationAnimation {
        fun onEndingShowAnimation()
        fun onEndingHideAnimation()
    }

    var runnable: Runnable? = null
    private fun onSmallItemFocus(
        lastSelectedView: FastLiveTabEventListViewHolder,
        isSmallDuration: Boolean,
        isVerticallyScrolled: Boolean
    ) {

        if (eventInfoView != null) {
            removeView(eventInfoView!!.itemView)
            eventInfoView = null
        }

        lastSelectedViewHolder = lastSelectedView

        runnable?.let {
            if (handler != null) {
                handler.removeCallbacks(it)
            }
        }

        if (!isSmallDuration) return

        if (timelineGridView!!.scrollState != 0) {
            return
        }
        infoEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)

        if (infoEvent != null) {

             runnable = Runnable {
                 //do not want to expand if we changed our focus from epg to other place.
                 if (!timelineGridView!!.hasFocus() || selectedEvent!=null){
                     return@Runnable
                 }
                 val itemview = LayoutInflater.from(context).inflate(R.layout.guide_event_list_item, this, false)

                 eventInfoView = FastLiveTabEventListViewHolder(itemview)
                 eventInfoView!!.timeTextView!!.setDateTimeFormat(FastLiveTabDataProvider.getDateTimeFormat())
                 val layoutParams = eventInfoView!!.rootView?.layoutParams
                 layoutParams?.width = 500
                 eventInfoView!!.rootView?.layoutParams = layoutParams
                 eventInfoView!!.rootView?.invalidate()
                 eventInfoView!!.itemView.visibility = View.VISIBLE

                 eventInfoView?.nameTextView?.ellipsize = TextUtils.TruncateAt.MARQUEE
                 eventInfoView?.nameTextView?.marqueeRepeatLimit = -1
                 eventInfoView?.nameTextView?.isSelected = true
                 eventInfoView?.nameTextView?.setSingleLine()
                 updateSmallEventInfo()
                 addView(eventInfoView!!.itemView)

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
                     eventsContainer!!.removeOnScrollListener(scrollListener!!)
                     timelineGridView!!.removeOnScrollListener(scrollListener!!)
                 }

                 scrollListener = object : RecyclerView.OnScrollListener() {

                     override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                         super.onScrolled(recyclerView, dx, dy)
                         updatePositionRunnable.run()
                     }

                 }

                 val target =
                     if (isVerticallyScrolled) eventsContainer!! else timelineGridView!!
                 target.addOnScrollListener(scrollListener!!)
                 target.post(updatePositionRunnable)

            }
            if (handler!=null) handler.postDelayed(runnable!!,500)

        }
    }

    var scrollListener: RecyclerView.OnScrollListener? = null

    /**
     * update event info on expand and close detail scene
     */

    private fun updateSmallEventInfo() {
        if (((ReferenceApplication.worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal)) {
            FastLiveTabDataProvider.getActiveChannel { tvChannel ->
                if (infoEvent!!.startTime <= Date(currentTimeLong).time && infoEvent!!.endTime >= Date(currentTimeLong).time && tvChannel.channelId == infoEvent!!.tvChannel.channelId) {
                    eventInfoView!!.currentlyPlayingIcon!!.visibility = View.VISIBLE
                } else {
                    eventInfoView!!.currentlyPlayingIcon!!.visibility = View.GONE
                }
            }
        }

        if (FastLiveTabDataProvider.isEventLocked(infoEvent)) {
            eventInfoView!!.setParentalRestriction()
        } else {
            eventInfoView!!.nameTextView?.text = infoEvent!!.name
        }
        setEventTime(eventInfoView!!, infoEvent!!)

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

    fun refreshIndicators() {
        eventsContainerAdapter.refreshIndicators(
            selectedEventListView,
            selectedEvent != null,
            object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: FastLiveTabEventListViewHolder,
                    isSmallDuration: Boolean
                ) {}
            })
    }

    fun hasInternet(): Boolean = FastLiveTabDataProvider.hasInternet()

    fun isAnokiServerReachable(): Boolean = FastLiveTabDataProvider.isAnokiServerReachable()
    fun setFocusToCategory() {
        if (!FastLiveTabDataProvider.isTosAccepted()) {
            fastTosMoreInfo?.requestFocus()
            return
        }

// TODO BORIS - this needs to be tested, not sure when it is called
//        if (channelFilterVerticalGridView!!.layoutManager!!.findViewByPosition(activeFilter) != null) {
//            channelFilterVerticalGridView!!.layoutManager!!.findViewByPosition(activeFilter)!!.requestFocus()
//        } else {
//            channelFilterVerticalGridView!!.requestFocus()
//        }
        if (channelFilterVerticalGridView?.layoutManager?.findViewByPosition(channelFilterAdapter.selectedItemPosition) != null) {
            channelFilterVerticalGridView?.layoutManager?.findViewByPosition(channelFilterAdapter.selectedItemPosition)?.requestFocus()
        } else if (channelFilterVerticalGridView?.layoutManager?.findViewByPosition(0) != null){
            channelFilterVerticalGridView?.layoutManager?.findViewByPosition(0)?.requestFocus()
            channelFilterVerticalGridView?.requestFocus()
        } else {
            focusCallback(Focus.REQUEST_TOP_MENU)
        }
    }

    fun reset() {
        if (!FastLiveTabDataProvider.isTosAccepted()) {
            focusCallback.invoke(Focus.REQUEST_TOP_MENU)
            showTosMoreInfo()
            return
        }
        hideEventInfoView()
        if (!FastLiveTabDataProvider.isRegionSupported()) {
            showRegionNotSupported()
            return
        }
        if (!isAnokiServerReachable()) {
            showAnokiServerStatusInfo(false)
            return
        }
        FastLiveTabDataProvider.getActiveGenre { activeGenre->
            var index = 0

            channelFilterVerticalGridView!!.adapter = channelFilterAdapter
            timelineGridView!!.adapter = timelineAdapter
            // data should not load when there is no internet
            if(hasInternet()) {
                showLoading()
                waitingForExtend = true
                FastLiveTabDataProvider.getAvailableFilters { filters ->
                    ReferenceApplication.runOnUiThread{
                        if (filters != null) {
                            // Preparing filters
                            val list = mutableListOf<CategoryItem>()
                            filters.toList().forEach{item ->
                                if ((item.name!! !in mutableListOf("TV Channels", "Tuner(DTV)"))) {
                                    list.add(CategoryItem(item.id, item.name!!))
                                    if (activeGenre == item.name) {
                                        index = list.size - 1
                                    }
                                }
                            }
                            //Refresh channel filter list
                            filterList = list
                            channelFilterAdapter.selectedItemPosition = index
                            channelFilterAdapter.activeItemPosition = index
                            channelFilterAdapter.refresh(filterList)
                            channelFilterVerticalGridView!!.post {
                                channelFilterVerticalGridView!!.scrollToPosition(channelFilterAdapter.selectedItemPosition)
                                onFilterSelected(index) {
                                    if (shouldFocusCurrentEvent) {
                                        Handler().postDelayed(Runnable {
                                            focusOnGrid()
                                            timelineGridView?.requestFocus()
                                        }, 200)
                                        customDetails.animateVisibility(true,false)
                                    }
                                }
                            }
                        }
                    }
                }

                isLoading = true
                isFirstTimeOpened = true
                setVisibility(false)
                initTimeline()
                currentTimePosition = 0
                dayOffset=selectedDayFilterOffset
                leftDayOffset = 0
                rightDayOffset = 0
                if(!FastLiveTabDataProvider.isAccessibilityEnabled()) {
                    eventsContainer?.setSelectedPosition(0, 0)
                }
                channelListGridView?.setSelectedPosition(0, 0)
                customDetails.animateVisibility(shouldBeVisible = false, shouldAnimate = false)
            }
        }
    }

    fun refreshGuideOnUpdateFavorite(favoritesItems: ArrayList<String>) {

        var isFavoriteFilter = false
        activeFilterName = channelFilterAdapter.getSelectedItem()


        for (i in favoritesItems) {
            isFavoriteFilter = false
            if (i == activeFilterName) {
                isFavoriteFilter = true
                break
            }
        }

        if (isFavoriteFilter) {

            var isChannelRemovedFromFav = true
            for (filter in favoritesItems) {
                if (activeFilterName == filter) {
                    isChannelRemovedFromFav = false
                    break
                }

            }
            if (isChannelRemovedFromFav) {
                if (eventInfoView != null) {
                    onSmallItemFocus(lastSelectedViewHolder!!, true, true)
                }
                eventsContainerAdapter.showSeparator(
                    selectedEventListView,
                    false, object : EndingTranslationAnimation {
                        override fun onEndingShowAnimation() {
                        }

                        override fun onEndingHideAnimation() {
                        }

                    }
                )
                selectedEvent = null

                channelListAdapter.removeChannel(selectedEventListView)
                eventsContainerAdapter.removeChannelEvents(
                    selectedEventListView
                )
                updatePastOverlay()

                val channelListSize = channelListAdapter.itemCount

                //removing items other than 1st position
                if (selectedEventListView > 0) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(200)
                        yield()
                        if (selectedEventListView == channelListSize) {
                            selectedEventListView -= 1
                        }
                        if (eventsContainerAdapter.getEventListView(
                                selectedEventListView
                            ) != null
                        )
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
                                        lastSelectedView: FastLiveTabEventListViewHolder,
                                        isSmallDuration: Boolean
                                    ) {
                                        onSmallItemFocus(
                                            lastSelectedView,
                                            isSmallDuration,
                                            true
                                        )
                                    }
                                }
                            )
                    }.apply(job::add)
                }
                //removing the top item when size is not 0
                else if (selectedEventListView == 0 && channelListSize != 0) {

                    CoroutineScope(Dispatchers.Main).launch {
                        delay(200)
                        yield()
                        if (eventsContainerAdapter.getEventListView(
                                selectedEventListView
                            ) != null
                        )
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
                                        lastSelectedView: FastLiveTabEventListViewHolder,
                                        isSmallDuration: Boolean
                                    ) {
                                        onSmallItemFocus(
                                            lastSelectedView,
                                            isSmallDuration,
                                            true
                                        )
                                    }
                                }
                            )
                    }.apply(job::add)
                }
                //no item remains in list
                else if (channelListSize == 0) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(200)
                        yield()
                        channelFilterVerticalGridView!!.layoutManager!!.getChildAt(0)!!
                            .requestFocus()
                    }.apply(job::add)
                }
            }

        }
    }

    private fun refreshFilterAdapter(position: Int, filterList: MutableList<CategoryItem>) {
        channelFilterAdapter.selectedItemPosition = position
        channelFilterAdapter.activeItemPosition = position
        channelFilterAdapter.update(filterList)
    }

    /**
     * Refresh guide filter list
     *
     * @param filterList new filter list
     */
    fun refreshFilterList(filterList: MutableList<CategoryItem>, favoritesAdded: Boolean) {
        var selectedPosition = channelFilterAdapter.selectedItemPosition
        var newActiveFilterPos = selectedPosition
        newActiveFilterPos =
            if (favoritesAdded) newActiveFilterPos + 1 else newActiveFilterPos - 1

        if (!favoritesAdded  && channelFilterAdapter.getSelectedItem() == "Favorites") {
            // When all channels are removed from favorites and favorites filter is removed
            channelFilterAdapter.forceActiveItemRequest = false
            refreshFilterAdapter(0, filterList)
            setVisibility(false)
            timelineGridView?.scrollTo(0, 0)
            initTimeline()
            currentTimePosition = 0
            leftDayOffset = 0
            rightDayOffset = 0
            if(!FastLiveTabDataProvider.isAccessibilityEnabled()) {
                eventsContainer?.setSelectedPosition(0, 0)
            }
            selectedEventListView = 0
            channelListGridView?.setSelectedPosition(0, 0)
            onFilterSelected(0, onRefreshFinished = {requestFocusOnChannelFilter()})
            channelFilterAdapter.forceActiveItemRequest = true
        } else {
            channelFilterAdapter.forceActiveItemRequest = true
            refreshFilterAdapter(newActiveFilterPos, filterList)
        }
    }

    /**
    * Select current event inside the active channel row
    */
    fun channelChanged() {
        Handler().post {
            FastLiveTabDataProvider.getActiveChannel { tvChannel ->
                if (!channelListContains(tvChannel)) {
                    if (eventInfoView != null) {
                        removeView(eventInfoView!!.itemView)
                        eventInfoView = null
                    }
                    if (tvChannel != null) {
                        var isExtend = false

                        val startDayOffset = dayOffset+leftDayOffset
                        val endDayOffset = dayOffset + rightDayOffset
                        if (startDayOffset != endDayOffset) isExtend = true
                        FastLiveTabDataProvider.getEventsForChannels(tvChannel!!, FastLiveTabDataProvider.getActiveFilter(), { data->
                            eventsContainerAdapter.clearFocus(selectedEventListView)

                            clearChannelList()
                            eventContainerList!!.clear()
                            var index = 0
                            for ((_, value) in data) {
                                addChannelToChannelList(value[0].tvChannel)
                                eventContainerList!!.add(value)
                                if (value[0].tvChannel == tvChannel) {
                                    selectedEventListView = index
                                }
                                index++
                            }

                            Handler(Looper.getMainLooper()).post {
                                if (pendingLayoutCreation) return@post
                                pendingLayoutCreation = true
                                timelineGridView!!.postDelayed({
                                    pendingLayoutCreation = false
                                }, 500)

                                eventsContainer!!.itemAnimator = null
                                channelListAdapter.refresh(mChannelList)
                                eventsContainerAdapter.refresh(eventContainerList!!)
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(200)
                                    yield()
                                    eventsContainerAdapter.showFocus(
                                        selectedEventListView,
                                        getFocusSearchTime(),
                                        object : EpgEventFocus {
                                            override fun onEventFocus(
                                                lastSelectedView: FastLiveTabEventListViewHolder,
                                                isSmallDuration: Boolean
                                            ) {
                                                onSmallItemFocus(
                                                    lastSelectedView,
                                                    isSmallDuration,
                                                    true
                                                )
                                            }
                                        }
                                    )
                                    setSelectedPosition(selectedEventListView)
                                }.apply(job::add)
                            }
                        }, startDayOffset, endDayOffset, isExtend)
                    }
                } else {
                    for (index in 0 until mChannelList.size) {
                        val tvChannel = mChannelList[index]
                        if (tvChannel?.name == tvChannel.name) {
                            selectedEventListView = index

                        }
                    }
                    if (eventInfoView != null) {
                        removeView(eventInfoView!!.itemView)
                        eventInfoView = null
                    }
                    CoroutineHelper.runCoroutineWithDelay({
                        eventsContainerAdapter.clearFocus(selectedEventListView)
                        eventsContainer!!.visibility = View.INVISIBLE
                        setSelectedPosition(selectedEventListView)
                        eventsContainer!!.visibility = View.VISIBLE
                        eventsContainerAdapter.updateIcon()
                        if (!FastLiveTabDataProvider.isAccessibilityEnabled()) {
                            eventsContainer!!.postDelayed({
                                channelListGridView?.clearFocus()
                                focusCallback(Focus.CLEAR_FOCUS_FROM_MAIN_MENU)
                                setFocusOnEvent()
                            }, 0)
                        }

                        //below is to fix text position while zap channel
                        eventsContainer!!.post {
                            eventsContainerAdapter.updateTextPosition(
                                getTimelineStartTime()
                            )
                        }
                    }, 0, Dispatchers.Main)
                }
            }
        }
    }

    /**
     * This method checks if [eventInfoView] is not null. If it's not null, it proceeds to hide the
     * associated view by setting its visibility to GONE. Afterwards, it sets eventInfoView to null,
     * effectively releasing the reference to the view.
     */
    private fun hideEventInfoView() {
        runnable?.let {if (handler != null) handler.removeCallbacks(it) }
        if (eventInfoView == null) return
        
        eventInfoView?.nameTextView?.isSelected = false
        eventInfoView?.itemView?.visibility = View.GONE
        eventInfoView = null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onBackPressed() {
        //this method is called to restart inactivity timer for no signal power off
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()

        hideEventInfoView()
        eventsContainerAdapter.clearFocus(selectedEventListView)
        customDetails.animateVisibility(false, false)
        if (isFocusOnChannelList) {
            if (isFavoritesUpdateInProgress)return
            channelListAdapter.removeStrokeColor(selectedEventListView)
            isFocusOnChannelList = false
        }
        focusCallback(Focus.REQUEST_TOP_MENU)
    }

    /**
     * Set selected position with scroll extra inside the channel list and event list container
     *
     * @param selectedPosition selected position
     */
    private fun setSelectedPosition(selectedPosition: Int) {
        channelListGridView?.setSelectedPosition(selectedPosition+1,1)
        if(!FastLiveTabDataProvider.isAccessibilityEnabled()) {
            eventsContainer?.setSelectedPosition(selectedPosition + 1, 1)
            updatePastOverlay()
        }
    }

    /**
     * Move focus on the filter list
     *
     * @param selectFirst select first element inside the list
     * @param selectLast select last element inside the list
     */
    fun selectedFilterList(selectFirst: Boolean, selectLast: Boolean) {
        channelFilterVerticalGridView!!.layoutManager!!.findViewByPosition(channelFilterAdapter.selectedItemPosition)!!
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
        if (gridView.adapter is FastLiveTabEventListAdapter) {
            return (gridView.adapter as FastLiveTabEventListAdapter).getSelectedIndex()
        }
        return -1
    }

    /**
     * Get the fist visible time inside the timeline
     */
    private fun getTimelineStartTime(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTimeLong)
        calendar.time = currentTime!!
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
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTimeLong)
        calendar.time = currentTime!!
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
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTimeLong)
        calendar.time = currentTime!!
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
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTimeLong)
        calendar.time = currentTime!!
        calendar.add(Calendar.DATE, rightDayOffset + dayOffset)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return Date(calendar.time.time)
    }

    /**
     * Update past overlay position.
     *
     * Pastoverlay is vertical line that represents current time.
     */
    private fun updatePastOverlay() {

        updatePastOverlayAlignment(false)

        var visibleRowCount = channelListAdapter.itemCount - selectedEventListView + 2
        if(selectedEventListView == channelListAdapter.itemCount-1) visibleRowCount ++
        val overlayCount = minOf(7, channelListAdapter.itemCount, visibleRowCount)

        val overlayHeight =
            overlayCount * Utils.getDimensInPixelSize(R.dimen.custom_dim_56) - Utils.getDimensInPixelSize(
                R.dimen.custom_dim_6
            )

        val startDate: Date = getTimelineStartTime()
        val endDate = Date(getTimelineEndTime().time + TimeUnit.MINUTES.toMillis(30))

        var minutes : Int? = null

        if (currentTime!!.after(startDate)) {
            if (currentTime!!.after(endDate)) {
                minutes = 250
            } else {
                minutes = (currentTime!!.time - startDate.time).toInt() / 60000
            }
        }
        val pastOverlayParams = pastOverlay!!.layoutParams as ConstraintLayout.LayoutParams
        val width = if (minutes!=null) maxOf(minutes * GUIDE_TIMELINE_MINUTE, Utils.getDimensInPixelSize(R.dimen.custom_dim_1)) else -3
        if(overlayHeight!= pastOverlayParams.height || width !=  pastOverlayParams.width){
            pastOverlayParams.height = overlayHeight
            pastOverlayParams.width = width
            pastOverlay!!.layoutParams = pastOverlayParams
        }
    }

    /**
     * Update past overlay height alignment based on detail view visibility.
     */
    private fun updatePastOverlayAlignment(isAnimate: Boolean){
        val pastOverlaySeparator: View = findViewById(R.id.past_overlay_separator)
        val lp = pastOverlaySeparator.layoutParams as RelativeLayout.LayoutParams
        val newMargin = if(isDetailVisible && selectedEventListView > 1 && eventsContainerAdapter.itemCount > 3) Utils.getDimensInPixelSize(R.dimen.custom_dim_54) else 0
        if(lp.bottomMargin != newMargin){
            pastOverlayAnimator?.cancel()
            if(isAnimate){
                val oldMargin = lp.bottomMargin + Utils.getDimensInPixelSize(R.dimen.custom_dim_15) * (if(isDetailVisible) -1 else 1)
                pastOverlayAnimator = ValueAnimator.ofInt(oldMargin, newMargin).setDuration(500L)
                pastOverlayAnimator!!.interpolator = AccelerateDecelerateInterpolator()
                pastOverlayAnimator!!.addUpdateListener { animation ->
                    lp.bottomMargin = animation.animatedValue as Int
                    pastOverlaySeparator.requestLayout()
                }
                pastOverlayAnimator!!.start()
            }else{
                lp.bottomMargin = newMargin
                pastOverlaySeparator.requestLayout()
            }
        }
    }

    /**
     * To get time for finding event while moving focus to different channel event
     */
    private fun getFocusSearchTime() : Date {

        val focusSearchTime = Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60))

        return if (currentTime!!.after(getTimelineStartTime()) && currentTime!!.before(getTimelineEndTime())) {
            currentTime!! // To focus on current event when it is in first 60 minutes of visible timeline
        } else {
            focusSearchTime
        }
    }

    //calculate current time position
    private fun setCurrentTimePosition() : Boolean{
        //Scroll on the current event
        val currentSlotIndex =
            ((currentTime!!.hours) * 2) + (if (currentTime!!.minutes >= 30) 1 else 0)
        val currentSlotOffset =
            if (currentTime!!.minutes > 30) currentTime!!.minutes - 30 else currentTime!!.minutes

        //currentTimePosition = if (currentSlotOffset < 10) currentSlotIndex - 1 else currentSlotIndex
        currentTimePosition = currentSlotIndex
        if (currentTimePosition < 0) currentTimePosition = 0

        if (dayOffset != 0) currentTimePosition = 0
        return if(currentTimePosition >= timelineEndPosition() && waitingForExtend){
            checkForExtendTimeline()
            true
        }else{
            waitingForExtend = false
            false
        }
    }

    /**
     * Init scroll position inside the grid
     */
    private fun initScrollPosition() {
        showLoading()
        //Scroll on the current event
        var isExtending = setCurrentTimePosition()
        if (isExtending)return

        eventsContainer?.doOnPreDraw {
            setSelectedPosition(selectedEventListView)
            timelineGridView!!.scrollToPosition(currentTimePosition)
            eventsContainerAdapter.scrollToTime(getTimelineStartTime())
        }
        eventsContainer!!.post {
            if (shouldFocusCurrentEvent) {
                if (eventsContainerAdapter.getEventListView(selectedEventListView) != null)
                    eventsContainerAdapter.showFocusOnPositionFrame(
                        selectedEventListView,
                        getFirstVisibleChildIndex(
                            eventsContainerAdapter.getEventListView(selectedEventListView)!!
                        ),
                        getTimelineStartTime(),
                        getTimelineEndTime(),
                        object : EpgEventFocus {
                            override fun onEventFocus(
                                lastSelectedView: FastLiveTabEventListViewHolder,
                                isSmallDuration: Boolean
                            ) {
                                onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                            }
                        }
                    )
            }

            hideLoading(shouldFocusCurrentEvent)
            updatePastOverlay()
            if (shouldFocusCurrentEvent) {
                timelineGridView!!.viewTreeObserver.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        timelineGridView!!.viewTreeObserver
                            .removeOnGlobalLayoutListener(this)
                        timelineGridView!!.isLayoutFrozen = true
                        timelineGridView!!.requestFocus()
                        timelineGridView!!.isLayoutFrozen = false
                        focusOnCurrentEvent(true)
                        checkForExtendTimeline()
                    }
                })
            }
            shouldFocusCurrentEvent = false
        }

    }

    /**
     * Set ui views visibility
     *
     * @param visible true to show, false to hide
     */
    private fun setVisibility(visibility: Boolean) {
        var visible = visibility
        if (!FastLiveTabDataProvider.isTosAccepted()) {
            visible = false
            fastTosMoreInfo?.visibility = View.VISIBLE
        }
        val visibility = if (visible) View.VISIBLE else View.INVISIBLE
        timelineGridView!!.visibility = visibility
        if (!visible) {
            pastOverlay?.alpha = 0f
            channelListGridView!!.alpha = 0f
            eventsContainer!!.alpha = 0f
            if (FastLiveTabDataProvider.isTosAccepted()) {
                epgLoadingPlaceholder?.visibility = View.VISIBLE
            }
            if (isFirstTimeOpened || !FastLiveTabDataProvider.isTosAccepted()) {
                channelFilterVerticalGridView?.alpha = 0f
            }
        } else {
            channelFilterVerticalGridView!!.visibility = visibility
            channelListGridView!!.visibility = visibility
            eventsContainer!!.visibility = visibility
            pastOverlay?.visibility = visibility
            pastOverlay?.alpha = 1f
            channelListGridView!!.alpha = 1f
            eventsContainer!!.alpha = 1f
            channelFilterVerticalGridView?.alpha = 1f
            epgLoadingPlaceholder?.visibility = View.GONE
        }
    }

    /**
     * Show loading animation
     */
    private fun showLoading() {
        setVisibility(false)
        isLoading = true
    }

    /**
     * Hide loading animation
     */
    private fun hideLoading(updateDetails: Boolean = false) {
        Handler().postDelayed({
            isLoading = false
        },0)
        if (!FastLiveTabDataProvider.isTosAccepted()) {
            showTosMoreInfo()
            return
        }
        if (updateDetails) {
            updateDataForCustomDetails()
        }
        epgLoadingPlaceholder?.visibility = View.GONE

        setVisibility(true)
        isFirstTimeOpened = false

        if (HomeSceneBase.jumpGuideCurrentEvent && eventsContainer!!.visibility == View.VISIBLE) {
            setFocusOnEvent()
            HomeSceneBase.jumpGuideCurrentEvent = false
        }
    }

    private fun setFocusOnEvent() {
        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            return
        }
        FastLiveTabDataProvider.getActiveChannel { tvChannel ->
            try {
                selectedEventListView = mChannelList.indexOf(tvChannel)
                if (selectedEventListView < 0) selectedEventListView = 0
            } catch (e: Exception){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: ${e.printStackTrace()}")
            }
        }

        timelineGridView?.isLayoutFrozen = true
        timelineGridView?.requestFocus()

        channelFilterVerticalGridView?.let {
            if (it.findViewHolderForAdapterPosition(
                    channelFilterAdapter.activeItemPosition
                ) != null
            ) {
                // TODO BORIS LAST ONE activeFilter
                val holder =
                    it.findViewHolderForAdapterPosition(
                        channelFilterAdapter.activeItemPosition
                    ) as FastCategoryItemViewHolder
                channelFilterAdapter.setActiveFilter(holder)
                timelineGridView?.isLayoutFrozen = false

            }
        }
        if (eventsContainerAdapter.itemCount > 0) {
            val date = Date(currentTimeLong)
            if ((date.hours == 23) && (date.minutes > 40)) date.minutes = 40
            eventsContainerAdapter.showFocus(
                selectedEventListView,
                date,
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: FastLiveTabEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(lastSelectedView, isSmallDuration, false)
                    }
                }
            )
        } else {
            channelFilterVerticalGridView!!.requestFocus()
        }
        focusCallback(Focus.SELECT_ACTIVE_FILTER)
    }

    fun resume() {
        timelineGridView!!.isLayoutFrozen = false

        //below is to fix rtl text position on resume when event is open
        eventsContainer!!.doOnPreDraw {
            eventsContainerAdapter.updateTextPosition(
                getTimelineStartTime()
            )
        }
    }

    fun pause() {
        timelineGridView!!.isLayoutFrozen = true
    }

    fun timelineEndPosition(): Int {
        return timelineSize - GUIDE_TIMELINE_PAGE_SIZE
    }

    private fun isCurrentTimeVisible(): Boolean {
        return currentTime!!.after(getTimelineStartTime()) && currentTime!!.before(getTimelineEndTime())
    }

    fun focusOnCurrentEvent(selectFilter: Boolean = true) {
        if (selectFilter) {
            customDetails.animateVisibility(true,false)
            onFilterSelected(activeCategoryId) {
                Handler().postDelayed(Runnable {
                    focusOnGrid()
                    timelineGridView?.requestFocus()
                }, 200)
            }
        } else {
            customDetails.animateVisibility(true,false)
            shouldFocusCurrentEvent = true
        }
    }

    /**
     * this method will fetch events and initialise the scroll position to the start
     */
    fun reloadLiveTabToStart(){
        dayOffset = 0
        leftDayOffset = 0
        rightDayOffset = 0
        isLoading = true
        FastLiveTabDataProvider.fetchEventForChannelList({ data->
            eventContainerList?.clear()
            var index = 0
            for ((_, value) in data) {
                if (channelListContains(value[0].tvChannel)) {
                    eventContainerList!!.add(value)
                    index++
                }
            }

            // updating adapter
            eventsContainerAdapter.update(eventContainerList!!)
            eventsContainer!!.doOnPreDraw {
                var isExtending = setCurrentTimePosition()
                if (isExtending) return@doOnPreDraw
                timelineGridView!!.scrollToPosition(currentTimePosition)
                eventsContainerAdapter.scrollToTime(getTimelineStartTime())
                updatePastOverlay()
                if (timelineGridView!!.hasFocus()) {
                    eventsContainer!!.doOnPreDraw {
                        if (!isFocusOnChannelList) {
                            focusOnGrid()
                        }
                        timelineGridView?.isLayoutFrozen = true
                        timelineGridView?.requestFocus()
                        timelineGridView?.isLayoutFrozen = false
                    }
                }
                isLoading = false
            }
        }, mChannelList, 0, 0, false)
    }

    private fun focusOnGrid() {
        if (eventsContainerAdapter.itemCount > 0) {
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
                            lastSelectedView: FastLiveTabEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(
                                lastSelectedView,
                                isSmallDuration,
                                true
                            )
                            updateDataForCustomDetails()
                        }
                    }
                )
            }
            if (isFocusOnChannelList) {
                channelListAdapter.removeStrokeColor(selectedEventListView)
                isFocusOnChannelList = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun scrollRight() {
            onScrollRight(true)
    }


    fun hoursIndex(date: Date): Int {
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
     * If channel count is 3 background will set transparent.
     * If channel count is > 3 background will have gradient.
     */
    private fun updateGuideBackgroundDrawable() {
        val gradientBackground: View = findViewById(R.id.guide_fade_overlay)
        Utils.makeGradient(
            gradientBackground,
            GradientDrawable.LINEAR_GRADIENT,
            GradientDrawable.Orientation.BOTTOM_TOP,
            Color.parseColor("#293241"),
            Color.parseColor("#293241"),
            Color.parseColor("#00293241"),
            0.0F,
            0.38F
        )
    }

    /**
     * To display either the "start-end time" or "remaining time" for the current event
     *
     * @param holder target Holder
     * @param tvEvent tv event
     */
    private fun setEventTime(holder: FastLiveTabEventListViewHolder, tvEvent: TvEvent){
        if(FastLiveTabDataProvider.isCurrentEvent(tvEvent)){
            holder.timeTextView!!.text =
                FastLiveTabDataProvider.formatRemainingTime(tvEvent)
        }else{
            holder.timeTextView!!.time = tvEvent
        }
    }

    fun isInitialized(): Boolean {
        return initialized
    }

    companion object {
        val GUIDE_TIMELINE_ITEM_SIZE = Utils.getDimensInPixelSize(R.dimen.custom_dim_180)
        val GUIDE_TIMELINE_MINUTE = Utils.getDimensInPixelSize(R.dimen.custom_dim_6)
        const val GUIDE_TIMELINE_PAGE_SIZE = 4
        //showing data only one day (till yesterday) before current day
        val DAY_OFFSET_MIN = -1
        val DAY_OFFSET_MAX = 7
        val ITEM_OFFSET_TRIGGER = 0
        var activeCategoryId = Category.ALL_ID
        //Active filter id

    }
}