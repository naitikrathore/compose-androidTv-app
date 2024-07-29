package com.iwedia.cltv.scene.home_scene.guideVertical

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.Rect
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
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.components.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.FilterItem
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.scene.channel_list.ChannelListSortAdapter
import com.iwedia.cltv.scene.home_scene.guide.GuideSceneWidget
import com.iwedia.cltv.scene.home_scene.guide.GuideSceneWidgetListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.widgets.helpers.UtilsLib
import data_type.GList
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


/**
 * Vertical guide scene widget
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideSceneWidget(context: Context, listener: GuideSceneWidgetListener) :
    GuideSceneWidget(context, listener) {

    //Log tag
    private val TAG: String = "GuideSceneWidget"

    //Scene layout views references
    private var guideFilterGridView: HorizontalGridView? = null
    private var guideChannelListGridView: HorizontalGridView? = null
    private var guideTimelineGridView: VerticalGridView? = null
    private var guideEventsContainer: HorizontalGridView? = null
    private var dateTextView: TextView? = null
    private var eventDetailsView: VerticalGuideEventDetailsView? = null
    private var pastOverlay: RelativeLayout? = null
    private var loadingLayout: RelativeLayout? = null
    private var calendarBtnIcon: ImageView? = null
    private var calendarBtnSelector: View? = null
    private var calendarFilterContainer: ConstraintLayout? = null
    private var calendarFilterGridView: VerticalGridView? = null
    private var favoritesOverlay: ConstraintLayout? = null
    private var favoritesGridView: VerticalGridView? = null
    private var eventInfoView: VerticalGuideEventListViewHolder? = null


    //List adapters
    private val guideTimelineAdapter = VerticalGuideTimelineAdapter()
    private val guideChannelListAdapter =
        VerticalGuideChannelListAdapter()
    private val guideFilterCategoryAdapter = CategoryAdapter()
    private var eventsContainerAdapter = VerticalGuideEventsContainerAdapter(listener.getDateTimeFormat())

    //Selected tv event displayed inside the details view
    private var selectedEvent: TvEvent? = null

    //Active filter id
    private var activeFilter = GUIDE_FILTER_ALL

    //Current time
    private var currentTime: Date? = null

    //timeline position
    private var currentTimePosition = 0
    var loadingProgressBar: ProgressBar? = null

    //timeline layout creation progress
    var pendingLayoutCreation = false;

    private var currentCategoryPosition: Int = 0

    val focusChangeDelay = 500L

    //filterDayOffset
    val filterDayOffset = mutableListOf<Int>()

    //to prevent focus from going to the guideview for the first time
    var isOpeningFirstTime: Boolean = true

    //pagination
    var dayOffsetTop = 0
    var dayOffsetBottom = 0

    var channelList : MutableList<TvChannel> ? = null
    init {
        view = LayoutInflater.from(ReferenceApplication.applicationContext())
            .inflate(R.layout.layout_widget_guide_scene_vertical, null) as ConstraintLayout

        var gradientBackground: View = view!!.findViewById(R.id.guide_fade_overlay)
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
                    0.3
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

        var pastOverlaySeparator: View = view!!.findViewById(R.id.past_overlay_separator)
        pastOverlaySeparator.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_progress")))

        loadingLayout = view!!.findViewById(R.id.loading)
        initTimeline()

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
        val drawable = GradientDrawable()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            calendarFilterContainer!!.setBackground(drawable)
        } else {
            calendarFilterContainer!!.setBackgroundDrawable(drawable)
        }
        calendarFilterGridView = view!!.findViewById(R.id.calendar_filter_grid_view)

        calendarFilterGridView!!.itemAlignmentOffset =
            context!!.resources.getDimensionPixelSize(R.dimen.custom_dim_0)
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
        val drawable3 = GradientDrawable()
        drawable3.setShape(GradientDrawable.RECTANGLE)
        drawable3.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT)
        drawable3.setColors(
            intArrayOf(
                colorStart,
                colorMid,
                colorEnd
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            favoritesOverlay!!.setBackground(drawable3)
        } else {
            favoritesOverlay!!.setBackgroundDrawable(drawable3)
        }
        favoritesGridView = view!!.findViewById(R.id.favorites_overlay_grid_view)
        initFavoritesOverlay()

        guideFilterGridView!!.setNumRows(1)
        guideFilterCategoryAdapter.isCategoryFocus = true
        activeFilter = listener.getActiveEpgFilter()
        guideFilterCategoryAdapter.focusPosition = activeFilter
        guideFilterCategoryAdapter.selectedItem = activeFilter
        guideFilterGridView!!.adapter = guideFilterCategoryAdapter
        guideFilterGridView!!.preserveFocusAfterLayout = true

        guideChannelListGridView = view!!.findViewById(R.id.guide_channel_list)
        guideChannelListGridView!!.setNumRows(1)
        guideChannelListGridView!!.adapter = guideChannelListAdapter
        guideChannelListGridView!!.visibility = View.INVISIBLE

        guideEventsContainer = view!!.findViewById(R.id.guide_events_container)

        loadingProgressBar = view!!.findViewById(R.id.loadingProgressBar)
        loadingProgressBar!!.getIndeterminateDrawable().setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_selector")),
            PorterDuff.Mode.MULTIPLY
        );

        guideTimelineGridView?.windowAlignment = VerticalGridView.WINDOW_ALIGN_HIGH_EDGE
        guideTimelineGridView?.windowAlignmentOffsetPercent = 0f
        guideTimelineGridView?.windowAlignmentOffset =
            Utils.getDimensInPixelSize(R.dimen.custom_dim_70)

        guideFilterCategoryAdapter.adapterListener =
            object : CategoryAdapter.ChannelListCategoryAdapterListener {
                override fun getAdapterPosition(position: Int) {
                    runnable = Runnable {
                        if (currentCategoryPosition != position) {

                            if (guideTimelineGridView?.selectedPosition!! != -1) {
                                setVisibility(false)
                                guideTimelineGridView?.scrollTo(0, 0)
                                initTimeline()
                                currentTimePosition = 0
                                dayOffsetTop = 0
                                dayOffsetBottom = 0
                                guideEventsContainer?.setSelectedPosition(0, 0)
                                guideChannelListGridView?.setSelectedPosition(0, 0)
                                if (position != activeFilter) {
                                    guideFilterCategoryAdapter.clearFocus(activeFilter)
                                }
                                listener.onFilterSelected(position)
                            } else {
                                guideTimelineGridView?.requestFocus()
                            }
                            activeFilter = position
                        } else {
                            currentCategoryPosition = position
                        }
                    }
                    focusChangeHandler.postDelayed(runnable!!, focusChangeDelay)
                }

                override fun onItemSelected(position: Int) {
                }

                override fun digitPressed(digit: Int) {}

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
                        return true
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

                        currentCategoryPosition = currentPosition
//                    guideFilterCategoryAdapter.selectedItem = currentPosition
                        guideTimelineGridView?.isLayoutFrozen = false
                        if (eventsContainerAdapter.itemCount > 0) {
                            eventsContainerAdapter.showFocusOnPosition(
                                selectedEventListView,
                                getFirstVisibleChildIndex(
                                    eventsContainerAdapter.getEventListView(selectedEventListView)!!
                                ),
                                object : EpgEventFocus {
                                    override fun onEventFocus(
                                        lastSelectedView: VerticalGuideEventListViewHolder,
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
                        }
                        return true
                    }
                    return true
                }

                override fun onKeyUp(currentPosition: Int): Boolean {
                    guideFilterCategoryAdapter.clearFocus(currentCategoryPosition)
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
                    if (isLoading) {
                        guideFilterCategoryAdapter.requestFocus(position)
                        return
                    }
                    guideFilterCategoryAdapter.clearFocus(currentCategoryPosition)
                    val holder =
                        guideFilterGridView!!.findViewHolderForAdapterPosition(
                            position
                        ) as CategoryItemViewHolder
                    guideFilterCategoryAdapter.setActiveFilter(holder)
                    currentCategoryPosition = position

                    if (position == activeFilter && guideChannelListAdapter.itemCount != 0) {
                        guideTimelineGridView?.isLayoutFrozen = true
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onKeyDown: $currentCategoryPosition")
                        guideFilterCategoryAdapter.clearFocus(currentCategoryPosition)
                        guideTimelineGridView?.requestFocus()
                        val holder =
                            guideFilterGridView!!.findViewHolderForAdapterPosition(
                                position
                            ) as CategoryItemViewHolder
                        guideFilterCategoryAdapter.setActiveFilter(holder)

                        currentCategoryPosition = position
                        guideTimelineGridView?.isLayoutFrozen = false
                        try {
                            if (eventsContainerAdapter.itemCount > 0) {

                                eventsContainerAdapter.showFocusOnPosition(
                                    selectedEventListView,
                                    getFirstVisibleChildIndex(
                                        eventsContainerAdapter.getEventListView(
                                            selectedEventListView
                                        )!!
                                    ), object : EpgEventFocus {
                                        override fun onEventFocus(
                                            lastSelectedView: VerticalGuideEventListViewHolder,
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
                            }
                        } catch (e: Exception) {
                        }
                    } else if (guideTimelineGridView?.selectedPosition!! != -1) {
                        setVisibility(false)
                        guideTimelineGridView?.scrollTo(0, 0)
                        initTimeline()
                        currentTimePosition = 0
                        dayOffsetTop = 0
                        dayOffsetBottom = 0
                        guideEventsContainer?.setSelectedPosition(0, 0)
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
                            activeFilter
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

    }


    fun checkForExtendTimeline() {
        val totalItemCount = guideTimelineGridView!!.adapter!!.itemCount

        if (dayOffset + dayOffsetTop > DAY_OFFSET_MIN) {
            if (currentTimePosition - ITEM_OFFSET_TRIGGER <= 0) {
                showLoading()
                (listener as GuideSceneWidgetListener).getDayWithOffset(
                    dayOffset + dayOffsetTop - 1,
                    true,
                    mutableListOf<TvChannel>()
                )
            }
        }

        if (dayOffset + dayOffsetBottom < DAY_OFFSET_MAX) {
            if (GUIDE_TIMELINE_PAGE_SIZE + currentTimePosition + ITEM_OFFSET_TRIGGER >= totalItemCount) {
                showLoading()
                (listener as GuideSceneWidgetListener).getDayWithOffset(
                    dayOffset + dayOffsetBottom + 1,
                    true,
                    mutableListOf<TvChannel>()

                )
            }
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
        TODO("Not yet implemented")
    }

    override fun getActiveCategory(): CategoryItem {
        TODO("Not yet implemented")
    }

    override fun refreshEpg() {
        TODO("Not yet implemented")
    }

    override fun scrollRight() {
        TODO("Not yet implemented")
    }

    override fun onFetchFailed() {
        TODO("Not yet implemented")
    }

    override fun updateFavSelectedItem(
        favorites: ArrayList<String>,
        filterList: ArrayList<CategoryItem>
    ) {
        TODO("Not yet implemented")
    }

    override fun onDigitPress(digit: Int) {
        TODO("Not yet implemented")
    }


    override fun refreshClosedCaption() {
    }

    /**
     * Init calendar filter
     */
    private fun initCalendarFilter() {
        val calendarFilterTitle = view!!.findViewById<TextView>(R.id.calendar_filter_title)
        calendarFilterTitle.setText(ConfigStringsManager.getStringById("calendar_filter_title"))
        calendarFilterTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        calendarFilterTitle.typeface = TypeFaceProvider.getTypeFace(
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

                    isLoading = true
                    dayOffset = filterDayOffset.get(position)
                    setVisibility(false)
                    guideTimelineGridView?.scrollTo(0, 0)
                    isOpeningFirstTime = false
                    initTimeline()
                    currentTimePosition = 0
                    dayOffsetTop = 0
                    dayOffsetBottom = 0

                    guideEventsContainer?.setSelectedPosition(0, 0)
                    guideChannelListGridView?.setSelectedPosition(0, 0)

                    if (dayOffset == 0) {
                        (listener as GuideSceneWidgetListener).onFilterSelected(
                            activeFilter
                        )
                    } else {
                        (listener as GuideSceneWidgetListener).getDayWithOffset(dayOffset, false,
                            mutableListOf<TvChannel>()
                        )
                    }
                    calendarFilterContainer?.visibility = View.GONE
                    calendarBtnSelector!!.setBackgroundColor(Color.TRANSPARENT)
                }

                override fun onKeyPressed(keyEvent: KeyEvent): Boolean {
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_BACK -> {
                                calendarFilterContainer?.visibility = View.GONE
                                calendarBtnIcon?.requestFocus()
                                return true
                            }
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                return true
                            }
                        }
                    }
                    return false
                }
            }
        calendarBtnIcon?.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    calendarBtnSelector?.background =
                        ConfigColorManager.generateButtonBackground()
                    calendarBtnIcon!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_background")))
                    calendarBtnSelector!!.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
                } else {
//                    calendarBtnSelector?.background =
//                        ContextCompat.getDrawable(
//                            view!!.context,
//                            R.drawable.reference_button_non_focus_shape
//                        )
                    //calendarBtnSelector!!.setBackgroundResource(0)
                    //calendarBtnSelector = view!!.findViewById(R.id.calendar_button_selector)
                    calendarBtnSelector!!.setBackgroundColor(Color.TRANSPARENT)
//                    calendarBtnSelector!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                    //       view?.setBackgroundResource(R.drawable.calendar_icon)
                    calendarBtnIcon!!.setColorFilter(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                }
            }

        calendarBtnIcon?.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, keycode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyEvent?.action == KeyEvent.ACTION_UP) {
                    when (keycode) {
                        KeyEvent.KEYCODE_ESCAPE,
                        KeyEvent.KEYCODE_BACK -> {
                            (listener as GuideSceneWidgetListener).requestFocusOnTopMenu()
                            return true
                        }
                    }
                }
                if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            guideFilterGridView!!.requestFocus()
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            //Disable key down
                            guideTimelineGridView?.isLayoutFrozen = true
                            guideTimelineGridView?.requestFocus()
                            guideTimelineGridView?.isLayoutFrozen = false
                            if (eventsContainerAdapter.itemCount > 0) {
                                eventsContainerAdapter.showFocusOnPosition(
                                    selectedEventListView,
                                    getFirstVisibleChildIndex(
                                        eventsContainerAdapter.getEventListView(
                                            selectedEventListView
                                        )!!
                                    ), object : EpgEventFocus {
                                        override fun onEventFocus(
                                            lastSelectedView: VerticalGuideEventListViewHolder,
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
                            }
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            (listener as GuideSceneWidgetListener).requestFocusOnTopMenu()
                            return true
                        }

                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            calendarFilterContainer?.bringToFront()
                            calendarFilterContainer?.elevation = 10f
                            calendarFilterContainer?.visibility = View.VISIBLE
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

    /**
     * Build display day name
     */
    private fun buildDayName(dayOffset: Int): String {

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, dayOffset)

        return when (dayOffset) {
            -1 -> ConfigStringsManager.getStringById("yesteday")
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
        dateTextView?.text = buildDayName(dayOffsetTop + dayOffset + (currentTimePosition / 48))
    }

    /**
     * Init favorites overlay
     */
    private fun initFavoritesOverlay() {
        val favoriteContainerTitle = view!!.findViewById<TextView>(R.id.favorites_overlay_title)

        favoriteContainerTitle!!.setText(ConfigStringsManager.getStringById("add_to"))
        favoriteContainerTitle!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        favoriteContainerTitle.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        val favoritesListAdapter = ReferenceMultiCheckListAdapter()
        /*var favoritesItems =
            (ReferenceSdk.favoriteHandler as ReferenceFavoriteHandler).getCategories()*/
        var favoritesItems= mutableListOf<String>()
        favoritesListAdapter.refresh(favoritesItems)
        favoritesGridView!!.setSelectedPosition(0)
        favoritesGridView!!.preserveFocusAfterLayout = true
        favoritesGridView!!.setNumColumns(1)
        favoritesGridView!!.adapter = favoritesListAdapter

        favoritesGridView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == 0) {
                    var position = favoritesListAdapter.focusedItem
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
            object : ReferenceMultiCheckListAdapter.ReferenceMultiCheckListAdapterListener {
                override fun onItemClicked(position: Int) {
                    //TODO("Not yet implemented")
                }

                override fun onKeyPressed(keyEvent: KeyEvent): Boolean {
                    if (keyEvent.action == KeyEvent.ACTION_UP) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_BACK -> {
                                var selectedFavListItems = ArrayList<String>()
                                selectedFavListItems.addAll(favoritesListAdapter.getSelectedItems())
                                (listener as GuideSceneWidgetListener).onFavoriteButtonPressed(
                                    selectedEvent!!.tvChannel,
                                    selectedFavListItems
                                )
                                favoritesGridView?.scrollToPosition(0)
                                favoritesOverlay?.visibility = View.GONE
                                eventDetailsView?.selectFavoriteButton(true)
                                guideTimelineGridView?.isLayoutFrozen = true
                                guideTimelineGridView?.requestFocus()
                                guideTimelineGridView?.isLayoutFrozen = false
                                return true
                            }
                        }
                    }
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                return true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                var position = favoritesListAdapter.focusedItem

                                var nextPosition =
                                    if (position < favoritesListAdapter.itemCount) position + 1 else position
                                var view = favoritesGridView!!.layoutManager!!.findViewByPosition(
                                    nextPosition
                                )
                                favoritesGridView!!.setSelectedPosition(nextPosition)
                                if (view != null) {
                                    view?.requestFocus()
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                var position = favoritesListAdapter.focusedItem
                                var nextPosition = if (position > 0) position - 1 else position
                                var view = favoritesGridView!!.layoutManager!!.findViewByPosition(
                                    nextPosition
                                )
                                favoritesGridView!!.setSelectedPosition(nextPosition)
                                if (view != null) {
                                    view?.requestFocus()
                                }
                                return true
                            }
                        }
                    }
                    return false
                }
            }
    }

    override fun refresh(data: Any) {
        super.refresh(data)
        if (data is Date) {

            val lastCurrentTime: Date? = currentTime

            currentTime = Date(data.time)

            // handle timeline crossing a day
            if (lastCurrentTime != null && lastCurrentTime.date != currentTime!!.date)
                dayOffset -= 1

            updateDateText()
            updatePastOverlay()

        } else if (data is Int) {
            selectedEventListView = data as Int
        } else if (UtilsLib.isListDataType(data, TvChannel::class.java)) {
            channelList = data as MutableList<TvChannel>
            if (channelList!!.size > 0) {
                guideChannelListAdapter.refresh(channelList!!)
            } else {
                hideLoading()
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Don't load guideChannelListAdapter() ...with ReferenceTvChannel data")
            }
        } else if (data is LinkedHashMap<*, *>) {
            val eventContainerList = getProgramsList(data)
            eventsContainerAdapter = VerticalGuideEventsContainerAdapter((listener as GuideSceneWidgetListener).getDateTimeFormat())
            guideEventsContainer!!.setNumRows(1)
            guideEventsContainer!!.setHasFixedSize(true)
            guideEventsContainer!!.itemAnimator = null
            guideEventsContainer!!.adapter = eventsContainerAdapter
            guideEventsContainer!!.visibility = View.INVISIBLE
            if (eventContainerList.isNotEmpty()) {
                eventsContainerAdapter.refresh(eventContainerList, object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(
                            lastSelectedView,
                            isSmallDuration,
                            true
                        )
                    }
                })
                showLoading()
                //Wait for events to be rendered before calling scroll to current
                guideEventsContainer!!.postDelayed({
                    updatePastOverlay()
                    initScrollPosition()
                }, 100)
            } else {
                guideEventsContainer!!.visibility = View.VISIBLE
                hideLoading()
            }
        } else if (data is GList<*>) {
            if (data.value[0] is FilterItem) {
                val list = mutableListOf<CategoryItem>()

                var id = 0
                data.value.forEach { item ->

                    if (item is FilterItem && (item.name!! !in mutableListOf(
                            "TV Channels",
                            "Tuner(DTV)"
                        ))
                    ) {
                        list.add(CategoryItem(id, item.name!!))
                        id++
                    }
                }
                if (list.isNotEmpty()) {
                    guideFilterCategoryAdapter.keepFocus = true
                    guideFilterCategoryAdapter.refresh(list)
                    guideFilterGridView!!.post {
                        guideFilterGridView!!.setSelectedPositionSmooth(activeFilter)
                        guideFilterGridView!!.clearFocus()
                        guideFilterCategoryAdapter.keepFocus = false
                    }
                } else {
                    guideEventsContainer!!.visibility = View.VISIBLE
                    hideLoading()
                }
            }
        }
    }

    //Pagination
    override fun extendTimeline(
        map: LinkedHashMap<Int, MutableList<TvEvent>>,
        additionalDayCount: Int
    ) {

        hideLoading()

        val extendingPreviousDay = additionalDayCount < dayOffset

        if (extendingPreviousDay) {
            dayOffsetTop--
        } else {
            dayOffsetBottom++
        }

        if (extendingPreviousDay) currentTimePosition += 48

        val eventContainerList = getProgramsList(map)

        val list = mutableListOf<String>()

        for (hour in 0 until 24) {
            list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":00")
            list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":30")
        }

        guideTimelineAdapter.extendTimeline(list, extendingPreviousDay)
        eventsContainerAdapter.extendTimeline(
            eventContainerList,
            extendingPreviousDay,
            object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: VerticalGuideEventListViewHolder,
                    isSmallDuration: Boolean
                ) {
                    onSmallItemFocus(
                        lastSelectedView,
                        isSmallDuration,
                        true
                    )
                }
            })

        if (extendingPreviousDay) {
            guideTimelineGridView!!.scrollToPosition(currentTimePosition)
            eventsContainerAdapter.scrollToTime(getTimelineStartTime())
        } else {
            eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())
            eventsContainerAdapter.notifyTimeLineDataChanged(object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: VerticalGuideEventListViewHolder,
                    isSmallDuration: Boolean
                ) {
                    onSmallItemFocus(
                        lastSelectedView,
                        isSmallDuration,
                        true
                    )
                }
            });
        }
    }

    /**
     * Get the Programs list from Hashmap result
     */
    private fun getProgramsList(
        data: LinkedHashMap<*, *>,
        mapList: Boolean = true
    ): MutableList<MutableList<TvEvent>> {

        val list = mutableListOf<MutableList<TvEvent>>()
        val map = data as ConcurrentHashMap<Int, MutableList<TvEvent>>
        for (index in 0 until map.size) {
            val item = map[index] ?: continue
            for (event in ArrayList(item)) {
                //to remove invalid events
                if (event.startTime > event.endTime) {
                    map[index]?.remove(event)
                }
            }

            list.add(item)
        }
        return if (!(listener as GuideSceneWidgetListener).getConfigInfo("epg_cell_merge")) list
        else if (mapList) getSameProgramsMappedList(list)
        else list
    }

    /**
     * Maps the Normal List of Programs to the list of programs
     * containing the same Programs for adjacent Channels
     */
    private fun getSameProgramsMappedList(
        list: MutableList<MutableList<TvEvent>>,
        position: Int = 0
    ): MutableList<MutableList<TvEvent>> {
        if (list.size == 0) return list

        for (pIndex in position until list.size) {
            list[pIndex].forEach { pItem ->
                if (pItem.isProgramSame) return@forEach

                subLoop@ for (cIndex in pIndex + 1 until list.size) {

                    if (pItem.providerFlag == null) continue@subLoop

                    val cItemIndex = list[cIndex].indexOfFirst {
                        pItem.providerFlag == it.providerFlag
                    }
                    if (cItemIndex >= 0) {

                        if (list[cIndex][cItemIndex].isProgramSame) continue@subLoop

                        pItem.apply {
                            isProgramSame = true
                            isInitialChannel = true
                        }

                        list[cIndex][cItemIndex].isProgramSame = true
                    } else {
                        break@subLoop
                    }
                }
            }
        }
        return list
    }

    /**
     * Update event list data
     */
    override fun update(map: LinkedHashMap<Int, MutableList<TvEvent>>) {

        val list = eventsContainerAdapter.getAllItems()
        val position = list.size
        list.addAll(getProgramsList(map, false))

        if ((listener as GuideSceneWidgetListener).getConfigInfo("epg_cell_merge")) {
            val mappedList = getSameProgramsMappedList(list, position - 1)

            eventsContainerAdapter.updateItem(
                position - 1,
                mappedList[position - 1],
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(
                            lastSelectedView,
                            isSmallDuration,
                            true
                        )
                    }
                })
            eventsContainerAdapter.update(
                mappedList.subList(position, list.size),
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(
                            lastSelectedView,
                            isSmallDuration,
                            true
                        )
                    }
                })
        } else {
            eventsContainerAdapter.update(list, object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: VerticalGuideEventListViewHolder,
                    isSmallDuration: Boolean
                ) {
                    onSmallItemFocus(
                        lastSelectedView,
                        isSmallDuration,
                        true
                    )
                }
            })
        }
    }

    //Timeline initialization
    private fun initTimeline() {
        guideTimelineGridView!!.visibility = View.INVISIBLE

        val list = mutableListOf<String>()
        if (!android.text.format.DateFormat.is24HourFormat(context)) {
            for (hour in 0 until 24) {
                if (hour < 12) {
                    list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":00AM")
                    list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":30AM")
                } else if (hour == 12) {
                    list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":00PM")
                    list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":30PM")
                } else {
                    list.add(String.format(Locale.ENGLISH, "%02d", hour - 12) + ":00PM")
                    list.add(String.format(Locale.ENGLISH, "%02d", hour - 12) + ":30PM")
                }
            }
        } else {
            for (hour in 0 until 24) {
                list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":00")
                list.add(String.format(Locale.ENGLISH, "%02d", hour) + ":30")
            }
        }

        guideTimelineAdapter.refresh(list)
        guideTimelineGridView!!.setNumColumns(1)
        guideTimelineGridView!!.adapter = guideTimelineAdapter

        guideTimelineAdapter.guideTimelineAdapterListener =
            object :
                VerticalGuideTimelineAdapter.GuideTimelineAdapterListener {
                override fun dispatchKey(keyCode: Int, keyEvent: KeyEvent): Boolean {
                    if (keyEvent?.action == KeyEvent.ACTION_UP) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_ESCAPE,
                            KeyEvent.KEYCODE_BACK -> {
                                onBackPressed()
                            }
                            KeyEvent.KEYCODE_BOOKMARK -> {
                                if (selectedEvent != null) {
                                    (listener as GuideSceneWidgetListener).onWatchlistButtonPressed(
                                        selectedEvent!!
                                    )
                                    eventsContainerAdapter.refreshIndicators(
                                        selectedEventListView,
                                        object : EpgEventFocus {
                                            override fun onEventFocus(
                                                lastSelectedView: VerticalGuideEventListViewHolder,
                                                isSmallDuration: Boolean
                                            ) {
                                                onSmallItemFocus(
                                                    lastSelectedView,
                                                    isSmallDuration,
                                                    true
                                                )
                                            }
                                        })
                                    eventDetailsView!!.refreshWatchlistButton()
                                }
                                return true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (selectedEvent == null) {
                                    onItemClick()
                                } else {
                                    var selectedButtonId = eventDetailsView!!.getSelectedButtonId()
                                    when (selectedButtonId) {
                                        VerticalGuideEventDetailsView.PAST_EVENT_WATCH_BUTTON_ID -> {
                                            (listener as GuideSceneWidgetListener).onCatchUpButtonPressed(
                                                selectedEvent!!
                                            )
                                        }
                                        VerticalGuideEventDetailsView.CURRENT_EVENT_WATCH_BUTTON_ID -> {
                                            (listener as GuideSceneWidgetListener).onWatchButtonPressed(
                                                selectedEvent!!.tvChannel
                                            )


                                        }
                                        VerticalGuideEventDetailsView.START_OVER_BUTTON_ID -> {
                                            (listener as GuideSceneWidgetListener).onCatchUpButtonPressed(
                                                selectedEvent!!
                                            )
                                        }
                                        VerticalGuideEventDetailsView.RECORD_BUTTON_ID -> {
                                            (listener as GuideSceneWidgetListener).onRecordButtonPressed(
                                                selectedEvent!!, object : IAsyncCallback {
                                                    override fun onFailed(error: Error) {
//                                                        callback.onFailed(error)
                                                    }

                                                    override fun onSuccess() {
                                                        refreshRecordButton()
//                                                        callback.onSuccess()
                                                    }
                                                })
                                        }
                                        VerticalGuideEventDetailsView.WATCHLIST_BUTTON_ID -> {
                                            (listener as GuideSceneWidgetListener).onWatchlistButtonPressed(
                                                selectedEvent!!
                                            )
                                            eventDetailsView!!.refreshWatchlistButton()
                                        }
                                        VerticalGuideEventDetailsView.ADD_TO_FAVORITES_BUTTON_ID -> {
                                            favoritesOverlay?.bringToFront()
                                            favoritesOverlay?.elevation = 10f
                                            favoritesOverlay?.visibility = View.VISIBLE
                                            (favoritesGridView?.adapter as ReferenceMultiCheckListAdapter).setSelectedItems(
                                                selectedEvent!!.tvChannel.favListIds
                                            )
                                            favoritesGridView?.post {
                                                favoritesGridView?.layoutManager?.findViewByPosition(
                                                    0
                                                )?.requestFocus()
                                            }
                                            eventDetailsView?.selectFavoriteButton(false)
                                        }
                                        VerticalGuideEventDetailsView.MORE_INFO_BUTTON_ID -> {
                                            guideTimelineGridView!!.isLayoutFrozen = true
                                            (listener as GuideSceneWidgetListener).onMoreInfoButtonPressed(
                                                selectedEvent!!
                                            )
                                        }
                                    }
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
                        if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    if (keyEvent.repeatCount % 3 == 0) {
                                        closeEventDetails()
                                        if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
                                            onRightPressed()
                                        else
                                            onLeftPressed()
                                    }
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (keyEvent.repeatCount % 3 == 0) {
                                        closeEventDetails()
                                        if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
                                            onLeftPressed()
                                        else
                                            onRightPressed()
                                    }
                                }
                            }
                        }
                        if (eventDetailsView?.dispatchKey(keyCode, keyEvent)!!) {
                            return true
                        }
                    }

                    if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                        if (isLoading) {
                            return true
                        }
                        when (keyCode) {

                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    onScrollDown()
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    onScrollUp()
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (keyEvent.repeatCount % 3 == 0) {
                                    if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
                                        onRightPressed()
                                    else
                                        onLeftPressed()
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (keyEvent.repeatCount % 3 == 0) {
                                    if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
                                        onLeftPressed()
                                    else
                                        onRightPressed()
                                }
                            }

                            KeyEvent.KEYCODE_CHANNEL_UP -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
                                        onChannelDownPressed()
                                    else
                                        onChannelUpPressed()
                                }
                            }

                            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                if (keyEvent.repeatCount % 5 == 0) {
                                    if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
                                        onChannelUpPressed()
                                    else
                                        onChannelDownPressed()
                                }
                            }

                            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                                val digit = keyCode - KeyEvent.KEYCODE_0
                                guideTimelineGridView!!.isLayoutFrozen = true
                                (listener as GuideSceneWidgetListener).onDigitPressed(digit)
                                return true
                            }

                            KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9 -> {
                                val digit = keyCode - KeyEvent.KEYCODE_NUMPAD_0
                                guideTimelineGridView!!.isLayoutFrozen = true
                                (listener as GuideSceneWidgetListener).onDigitPressed(digit)
                                return true
                            }
                        }
                        return false
                    }
                    return true
                }
            }
    }

    private var lastSelectedViewHolder: VerticalGuideEventListViewHolder? = null
    private var infoEvent: TvEvent? = null
    var scrollListener: RecyclerView.OnScrollListener? = null


    private fun onSmallItemFocus(
        lastSelectedView: VerticalGuideEventListViewHolder,
        isSmallDuration: Boolean,
        isVerticallyScrolled: Boolean
    ) {

        if (eventInfoView != null) {
            view!!.removeView(eventInfoView!!.itemView)
            eventInfoView = null
        }

        lastSelectedViewHolder = lastSelectedView

        if (!isSmallDuration) return
        infoEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
        if (infoEvent != null) {
            updatePastOverlay()
            val itemview = LayoutInflater.from(context)
                .inflate(R.layout.guide_event_list_item_vertical, view!!, false)

            eventInfoView = VerticalGuideEventListViewHolder(itemview)
            eventInfoView!!.timeTextView!!.setDateTimeFormat((listener as GuideSceneWidgetListener).getDateTimeFormat())
            var layoutParams = eventInfoView!!.rootView?.layoutParams
            layoutParams?.width = 316
            layoutParams?.height = 316
            eventInfoView!!.rootView?.layoutParams = layoutParams
            eventInfoView!!.rootView?.invalidate()

            updateSmallEventInfo()
            view!!.addView(eventInfoView!!.itemView)

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

            eventInfoView!!.backgroundView!!.scaleY = 0f
            eventInfoView!!.backgroundView!!.pivotY = 0f

            eventInfoView!!.backgroundView!!.animate().scaleY(1f).duration = 150

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
                if (isVerticallyScrolled) guideTimelineGridView!! else guideEventsContainer!!
            target.addOnScrollListener(scrollListener!!)
            target.post(updatePositionRunnable)
        }


    }

    fun updateSmallEventInfo() {
        val isFuture = infoEvent!!.startTime > Date().time

        var activeChannel =
            /*(ReferenceSdk.tvHandler as ReferenceTvHandler).activeChannel*/null as TvChannel
        if (infoEvent!!.startTime<= Date().time && infoEvent!!.endTime >= Date().time && activeChannel!!.channelId == infoEvent!!.tvChannel.channelId) {
            eventInfoView!!.currentlyPlayingIcon!!.visibility = View.VISIBLE
        } else {
            eventInfoView!!.currentlyPlayingIcon!!.visibility = View.GONE

        }
        //TODO Commented due to pending refactor

/*
        if (eventInfoView!!.recordIndicator != null)
            eventInfoView!!.recordIndicator!!.visibility =
                if (isFuture && ReferenceSdk.pvrSchedulerHandler!!.isInReclist(infoEvent!!)) View.VISIBLE else View.GONE
        if (eventInfoView!!.watchlistIndicator != null)
            eventInfoView!!.watchlistIndicator!!.visibility =
                if (isFuture && ReferenceSdk.watchlistHandler!!.isInWatchlist(infoEvent!!)) View.VISIBLE else View.GONE
*/


        eventInfoView!!.nameTextView?.text = infoEvent!!.name
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
    }

    private fun onScrollUp() {
        if (pendingLayoutCreation) return
        pendingLayoutCreation = true
        guideTimelineGridView!!.postDelayed(Runnable {
            pendingLayoutCreation = false
        }, 200)

        val lastPosition = currentTimePosition

        if (lastPosition > 0) {

            var doScroll = false;
            val selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)

            if (selectedEvent != null) {
                val selectedEventStartTime = (selectedEvent.startTime)
                val startTime = getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(15)

                if (startTime > selectedEventStartTime && lastPosition > 0) {
                    doScroll = true
                }
            }

            if (doScroll) {
                guideTimelineGridView?.layoutManager?.findViewByPosition(lastPosition)
                    ?.requestFocus()

                currentTimePosition -= 1

                smoothScrollTimelineToPosition(currentTimePosition)
                eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())

                updatePastOverlay()
                updateTextPosition()
                updateDateText()
            }
        }
        val isFocusChanged = eventsContainerAdapter.findPreviousFocus(
            selectedEventListView,
            getTimelineStartTime(),
            lastPosition == 0,
            object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: VerticalGuideEventListViewHolder,
                    isSmallDuration: Boolean
                ) {
                    onSmallItemFocus(
                        lastSelectedView,
                        isSmallDuration,
                        true
                    )
                }
            })
        if (lastPosition == 0 && !isFocusChanged) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            guideFilterGridView?.requestFocus()
        }
    }

    private fun onScrollDown() {
        if (pendingLayoutCreation) return
        pendingLayoutCreation = true
        guideTimelineGridView!!.postDelayed({
            pendingLayoutCreation = false;
        }, 200)

        val lastPosition = currentTimePosition

        var doScroll = false;
        val selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)

        if (selectedEvent != null) {
            val selectedEventEndTime = (selectedEvent.endTime)
            val endTimeline = getTimelineEndTime().time - TimeUnit.MINUTES.toMillis(15)

            if (endTimeline < selectedEventEndTime && lastPosition < timelineEndPosition()) {
                doScroll = true
            }
        }

        if (doScroll) {

            guideTimelineGridView?.layoutManager?.findViewByPosition(lastPosition + 1)
                ?.requestFocus()

            currentTimePosition += 1

            smoothScrollTimelineToPosition(currentTimePosition)

            val startHours = (timelineEndPosition()) / 2
            if (getTimelineStartTime().hours <= startHours) {
                eventsContainerAdapter.smoothScrollToTime(getTimelineStartTime())
            }
            updatePastOverlay()
            updateTextPosition()
            updateDateText()
        }
        eventsContainerAdapter.findNextFocus(
            selectedEventListView,
            getTimelineEndTime(),
            lastPosition == timelineEndPosition(),
            object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: VerticalGuideEventListViewHolder,
                    isSmallDuration: Boolean
                ) {
                    onSmallItemFocus(
                        lastSelectedView,
                        isSmallDuration,
                        true
                    )
                }
            })
    }

    /*
    * to smooth scroll timeline to position
    * */
    fun smoothScrollTimelineToPosition(position: Int) {
        val smoothScroller: RecyclerView.SmoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return (super.calculateTimeForScrolling(dx) * 2.5).toInt()
            }

            override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
                return PointF(0f, -1f)
            }
        }
        smoothScroller.targetPosition = position
        guideTimelineGridView!!.layoutManager!!.startSmoothScroll(smoothScroller)
    }

    private fun onLeftPressed() {
        if (selectedEventListView > 0) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView -= 1
            eventsContainerAdapter.showFocus(
                selectedEventListView,
                Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(30)),
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
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
            if (selectedEventListView >= 2) {
                setSelectedPosition(selectedEventListView + 1, getChannelScrollExtra())
            } else {
                setSelectedPosition(selectedEventListView, 0)
            }
            updatePastOverlay();
        }
    }

    private fun onRightPressed() {
        if (selectedEventListView < eventsContainerAdapter.itemCount - 1) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView += 1
            eventsContainerAdapter.showFocus(
                selectedEventListView,
                Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(30)),
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(
                            lastSelectedView,
                            isSmallDuration,
                            true
                        )
                    }
                })

            if (selectedEventListView >= 2) {
                setSelectedPosition(selectedEventListView + 1, getChannelScrollExtra())
            } else {
                setSelectedPosition(selectedEventListView, 0)
            }
            updatePastOverlay();
        }
    }

    private fun onChannelUpPressed() {
        if (guideTimelineGridView!!.scrollState != 0) {
            return
        }
        var selected =
            if (selectedEventListView + 6 < eventsContainerAdapter.itemCount - 1) selectedEventListView + 6 else eventsContainerAdapter.itemCount - 1
        if (selected != selectedEventListView && selectedEventListView < eventsContainerAdapter.itemCount - 4) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView = selected
            guideEventsContainer!!.visibility = View.INVISIBLE
            if (selectedEventListView >= 2) {
                setSelectedPosition(selectedEventListView + 1, getChannelScrollExtra())
            } else {
                setSelectedPosition(selectedEventListView, 0)
            }
            guideEventsContainer!!.postDelayed(Runnable {
                guideEventsContainer!!.visibility = View.VISIBLE
                eventsContainerAdapter.showFocus(
                    selectedEventListView,
                    Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: VerticalGuideEventListViewHolder,
                            isSmallDuration: Boolean
                        ) {
                            onSmallItemFocus(
                                lastSelectedView,
                                isSmallDuration,
                                true
                            )
                        }
                    })
            }, 500)
        } else {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView = selected
            eventsContainerAdapter.showFocus(
                selectedEventListView,
                Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(
                            lastSelectedView,
                            isSmallDuration,
                            true
                        )
                    }
                })
        }
    }

    private fun onChannelDownPressed() {
        if (guideTimelineGridView!!.scrollState != 0) {
            return
        }
        var selected = if (selectedEventListView - 6 >= 0) selectedEventListView - 6 else 0
        if (selected != selectedEventListView && selectedEventListView >= 2) {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            guideEventsContainer!!.visibility = View.INVISIBLE
            selectedEventListView = selected
            if (selectedEventListView >= 2) {
                setSelectedPosition(selectedEventListView + 1, getChannelScrollExtra())
            } else {
                setSelectedPosition(selectedEventListView, 0)
            }
            guideEventsContainer!!.postDelayed(Runnable {
                guideEventsContainer!!.visibility = View.VISIBLE
                eventsContainerAdapter.showFocus(
                    selectedEventListView,
                    Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                    object : EpgEventFocus {
                        override fun onEventFocus(
                            lastSelectedView: VerticalGuideEventListViewHolder,
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
            }, 500)
        } else {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            selectedEventListView = selected
            eventsContainerAdapter.showFocus(
                selectedEventListView,
                Date(getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(60)),
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(
                            lastSelectedView,
                            isSmallDuration,
                            true
                        )
                    }
                })
        }
    }

    private fun onItemClick() {
        if (guideTimelineGridView!!.scrollState != 0) {
            return
        }

        selectedEvent = eventsContainerAdapter.getSelectedEvent(selectedEventListView)
        if (selectedEvent != null) {
            if (eventInfoView != null) {
                eventInfoView!!.itemView.visibility = View.INVISIBLE
            }
            eventsContainerAdapter.staySelected(selectedEventListView)
            guideChannelListAdapter.showSeparator(selectedEventListView, true)
            eventsContainerAdapter.showSeparator(selectedEventListView, true,
                object : EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: VerticalGuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        onSmallItemFocus(
                            lastSelectedView,
                            isSmallDuration,
                            true
                        )
                    }
                })

            eventDetailsView =
                VerticalGuideEventDetailsView(
                    ReferenceApplication.applicationContext()
                )

            selectedEvent?.let {
                (listener as GuideSceneWidgetListener).getParentalRatingDisplayName(selectedEvent!!.parentalRating,
                    it
                )
            }?.let {
                eventDetailsView?.setDetails(selectedEvent!!,
                    it, (listener as GuideSceneWidgetListener).getDateTimeFormat()
                )
            }
            eventDetailsView!!.x = getRTLx(
                Utils.getDimensInPixelSize(R.dimen.custom_dim_121)
                        + (
                        (selectedEventListView + 1) * (Utils.getDimensInPixelSize(R.dimen.custom_dim_158)
                            .toFloat())
                        ) - Utils.getDimensInPixelSize(R.dimen.custom_dim_6),
                (2 * Utils.getDimensInPixelSize(R.dimen.custom_dim_158)) + Utils.getDimensInPixelSize(
                    R.dimen.custom_dim_6
                )
            )
            view!!.addView(eventDetailsView)

            var sizeComparator =
                if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL) 3 else 2;

            if (selectedEventListView >= sizeComparator && eventsContainerAdapter.itemCount >= sizeComparator) {
                guideEventsContainer!!.doOnPreDraw {
                    setSelectedPosition(
                        selectedEventListView,
                        getChannelScrollExtra()
                    )
                }
                eventDetailsView!!.x = getRTLx(
                    Utils.getDimensInPixelSize(R.dimen.custom_dim_121) +
                            Utils.getDimensInPixelSize(R.dimen.custom_dim_158) +
                            Utils.getDimensInPixelSize(R.dimen.custom_dim_158) +
                            Utils.getDimensInPixelSize(R.dimen.custom_dim_158) - Utils.getDimensInPixelSize(
                        R.dimen.custom_dim_6
                    ).toFloat(),
                    (2 * Utils.getDimensInPixelSize(R.dimen.custom_dim_158)) + Utils.getDimensInPixelSize(
                        R.dimen.custom_dim_6
                    )
                )
            } else {
                setSelectedPosition(0, 0)
            }
        } else {
            setSelectedPosition(0, 0)
        }

        updatePastOverlay()
    }

    /**
     * Close event details view
     */
    private fun closeEventDetails() {
        eventsContainerAdapter.clearSelected(selectedEventListView)
        guideChannelListAdapter.showSeparator(selectedEventListView, false)
        eventsContainerAdapter.showSeparator(selectedEventListView, false, object : EpgEventFocus {
            override fun onEventFocus(
                lastSelectedView: VerticalGuideEventListViewHolder,
                isSmallDuration: Boolean
            ) {
                onSmallItemFocus(
                    lastSelectedView,
                    isSmallDuration,
                    true
                )
            }
        })
        view!!.removeView(eventDetailsView)
        selectedEvent = null
        updatePastOverlay();
    }

    /**
     * Refresh details favorite button
     */
    override fun refreshFavoriteButton() {
        if (selectedEvent != null) {
            eventDetailsView?.refreshFavoriteButton()
        }
    }

    /**
     * Refresh details record button
     */
    override fun refreshRecordButton() {
        //TODO implement
    }

    override fun refreshWatchlistButton() {
        eventDetailsView?.refreshWatchlistButton()
    }

    override fun refreshIndicators() {
        eventsContainerAdapter.refreshIndicators(
            selectedEventListView,
            object : EpgEventFocus {
                override fun onEventFocus(
                    lastSelectedView: VerticalGuideEventListViewHolder,
                    isSmallDuration: Boolean
                ) {}
            })
    }

    /**
     * Refresh guide filter list
     *
     * @param filterList new filter list
     */
    override fun refreshFilterList(filterList: MutableList<CategoryItem>) {
        guideFilterCategoryAdapter.update(filterList)
    }



    /**
     * Select current event inside the active channel row
     */
    override fun channelChanged() {
        (listener as GuideSceneWidgetListener).getActiveChannel(object: IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: TvChannel) {
                selectedEventListView =
                    channelList?.indexOf(data)!!
                if (selectedEventListView < 0) selectedEventListView = 0
            }
        })

        isLoading = true
        dayOffset = 0
        setVisibility(false)
        guideTimelineGridView?.scrollTo(0, 0)
        initTimeline()
        (listener as GuideSceneWidgetListener).onFilterSelected(
            activeFilter
        )
    }

    override fun zapOnGuideOnly(channel: TvChannel) {
        TODO("Not yet implemented")
    }

    private fun onBackPressed() {
        if (eventInfoView != null) {
            eventInfoView!!.itemView!!.visibility = View.GONE
        }
        if (selectedEvent != null) {
            closeEventDetails()
            guideEventsContainer!!.doOnPreDraw {
                if (selectedEventListView >= 2) {
                    setSelectedPosition(selectedEventListView + 1, getChannelScrollExtra())
                } else {
                    setSelectedPosition(selectedEventListView, 0)
                }
            }
        } else {
            eventsContainerAdapter.clearFocus(selectedEventListView)
            (listener as GuideSceneWidgetListener).requestFocusOnTopMenu()
        }
    }

    /**
     * Set selected position with scroll extra inside the channel list and event list container
     *
     * @param selectedPosition selected position
     * @param scrollExtra
     */
    private fun setSelectedPosition(selectedPosition: Int, scrollExtra: Int) {
        if (selectedPosition < guideChannelListAdapter.itemCount) {
            guideChannelListGridView?.setSelectedPosition(selectedPosition, scrollExtra)
            guideEventsContainer?.setSelectedPosition(selectedPosition, scrollExtra)
        } else {
            guideChannelListGridView?.setSelectedPosition(
                guideChannelListAdapter.itemCount - 1,
                scrollExtra
            )
            guideEventsContainer?.setSelectedPosition(
                guideChannelListAdapter.itemCount - 1,
                scrollExtra
            )
        }
    }

    /**
     * Move focus on the filter list
     *
     * @param selectFirst select first element inside the list
     * @param selectLast select last element inside the list
     */
    override fun selectedFilterList(selectFirst: Boolean, selectLast: Boolean) {
        val firstView = guideFilterGridView!!.layoutManager!!.findViewByPosition(0)
        val lastView =
            guideFilterGridView!!.layoutManager!!.findViewByPosition(guideFilterGridView!!.adapter!!.itemCount - 1)
        if (selectFirst && firstView != null) {
            firstView.requestFocus()
        } else if (selectLast && lastView != null) {
            lastView.requestFocus()
        } else {
            guideFilterGridView!!.requestFocus()
        }
    }

    /**
     * Find first visible child index inside the list
     */
    private fun getFirstVisibleChildIndex(gridView: RecyclerView): Int {
        val layoutManager = gridView?.layoutManager
        val top = layoutManager?.paddingTop
        val childCount: Int = gridView!!.childCount
        for (i in 0 until childCount) {
            val childView = layoutManager!!.getChildAt(i)
            val childTop = layoutManager!!.getDecoratedTop(childView!!)
            val childBottom = layoutManager!!.getDecoratedBottom(childView!!)
            if ((childTop + childBottom) / 2 > top!!) {
                return gridView!!.getChildAdapterPosition(childView)
            }
        }
        if (gridView.adapter is VerticalGuideEventListAdapter) {
            return (gridView.adapter as VerticalGuideEventListAdapter).getSelectedIndex()
        }
        return -1
    }

    /**
     * Get the fist visible time inside the timeline
     */
    private fun getTimelineStartTime(): Date {
        val calendar = Calendar.getInstance()
        calendar.time = currentTime!!
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.add(Calendar.DATE, dayOffsetTop + dayOffset)
        return Date(calendar.time.time + (currentTimePosition * TimeUnit.MINUTES.toMillis(30)))
    }

    /**
     * Get the last visible time inside the timeline
     */
    private fun getTimelineEndTime(): Date {
        return Date(
            getTimelineStartTime().time + GUIDE_TIMELINE_PAGE_SIZE * TimeUnit.MINUTES.toMillis(
                30
            )
        )
    }

    /**
     * Get scrollExtra for channel based on layout direction
     */
    private fun getChannelScrollExtra(): Int {

        if (eventsContainerAdapter.itemCount <= 4) {
            if (selectedEvent == null) {
                if (selectedEventListView < 4) {
                    return 0
                }
            }
        }

        return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
            (Utils.getDimensInPixelSize(R.dimen.custom_dim_158) - Utils.getDimensInPixelSize(R.dimen.custom_dim_89_5))
        else
            -(Utils.getDimensInPixelSize(R.dimen.custom_dim_158) - Utils.getDimensInPixelSize(R.dimen.custom_dim_89_5))
    }

    /**
     * To get inverse value x for RTL
     */
    private fun getRTLx(x: Float, itemWidth: Int): Float {
        return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_RTL)
            ReferenceApplication.applicationContext().resources.getDisplayMetrics().widthPixels - x - itemWidth
        else
            x
    }

    /**
     * Update past overlay position
     */
    private fun updatePastOverlay() {

        var overlayCount = minOf(6, guideChannelListAdapter.itemCount)

        if (selectedEvent != null) {
            if (guideChannelListAdapter.itemCount - selectedEventListView <= 1) {
                overlayCount = minOf(3, guideChannelListAdapter.itemCount)
            }
        } else {
            if (guideChannelListAdapter.itemCount - selectedEventListView <= 3) {
                overlayCount = minOf(5, guideChannelListAdapter.itemCount)
            }
        }
        var overlayWidth =
            overlayCount * Utils.getDimensInPixelSize(R.dimen.custom_dim_158) - Utils.getDimensInPixelSize(
                R.dimen.custom_dim_6
            )

        if (selectedEvent != null) {
            overlayWidth += 2 * Utils.getDimensInPixelSize(R.dimen.custom_dim_158)
        }

        var startDate: Date = getTimelineStartTime()
        var endDate = Date(getTimelineEndTime().time + TimeUnit.MINUTES.toMillis(30))

        var minutes = 0

        if (currentTime!!.after(startDate)) {
            if (currentTime!!.after(endDate)) {
                minutes = 250
            } else {
                minutes = (currentTime!!.time - startDate.time).toInt() / 60000
            }
        }

        if (minutes > 0) {
            var height = minutes * GUIDE_TIMELINE_MINUTE
            var pastOverlayParams =
                pastOverlay!!.layoutParams as ConstraintLayout.LayoutParams
            pastOverlayParams.height = height.toInt()
            pastOverlayParams.width = overlayWidth
            pastOverlay!!.layoutParams = pastOverlayParams
            pastOverlay!!.visibility = View.VISIBLE
        } else {
            pastOverlay!!.visibility = View.GONE
        }
    }

    /**
     * Update tv event name and time info text positions for all channels inside the grid
     */
    private fun updateTextPosition() {
        eventsContainerAdapter.updateTextPosition(getTimelineStartTime(), object : EpgEventFocus {
            override fun onEventFocus(
                lastSelectedView: VerticalGuideEventListViewHolder,
                isSmallDuration: Boolean
            ) {
                onSmallItemFocus(
                    lastSelectedView,
                    isSmallDuration,
                    true
                )
            }
        })
    }

    /**
     * Init scroll position inside the grid
     */
    private fun initScrollPosition() {
        //Scroll on the current event
        val currentSlotIndex =
            ((currentTime!!.hours) * 2) + (if (currentTime!!.minutes >= 30) 1 else 0)
        val currentSlotOffset =
            if (currentTime!!.minutes > 30) currentTime!!.minutes - 30 else currentTime!!.minutes

        currentTimePosition = if (currentSlotOffset < 10) currentSlotIndex - 1 else currentSlotIndex
        if (currentTimePosition < 0) currentTimePosition = 0
        if (currentTimePosition > timelineEndPosition()) currentTimePosition = timelineEndPosition()

        if (dayOffset != 0) currentTimePosition = 0

        Log.d(Constants.LogTag.CLTV_TAG + TAG, ": current time $currentTime timeline scroll position $currentTimePosition")
        setVisibility(false)

        if (selectedEventListView >= 2) {
            setSelectedPosition(selectedEventListView + 1, getChannelScrollExtra())
        } else {
            setSelectedPosition(selectedEventListView, 0)
        }
        guideEventsContainer!!.post {
            guideTimelineGridView!!.scrollToPosition(currentTimePosition)
            eventsContainerAdapter.scrollToTime(getTimelineStartTime())
            if (!isOpeningFirstTime) {
                Handler().postDelayed(Runnable{
                    ReferenceApplication.runOnUiThread(Runnable{
                        eventsContainerAdapter.showFocus(
                            selectedEventListView,
                            if (dayOffset == 0) currentTime!! else Date(
                                getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(15)
                            ),
                            object : EpgEventFocus {
                                override fun onEventFocus(
                                    lastSelectedView: VerticalGuideEventListViewHolder,
                                    isSmallDuration: Boolean
                                ) {
                                    onSmallItemFocus(
                                        lastSelectedView,
                                        isSmallDuration,
                                        true
                                    )
                                }
                            })
                    })
                }, 100)
            }
            setVisibility(true)
            hideLoading()
            updatePastOverlay()
            updateTextPosition()
            updateDateText()
            if (!isOpeningFirstTime) {
                guideTimelineGridView!!.getViewTreeObserver().addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        guideTimelineGridView!!.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this)
                        guideTimelineGridView!!.isLayoutFrozen = true
                        guideTimelineGridView!!.layoutManager!!.findViewByPosition(
                            currentTimePosition
                        )!!.requestFocus()
                        guideTimelineGridView!!.isLayoutFrozen = false
                    }
                })
            }
            isOpeningFirstTime = true
        }

        checkForExtendTimeline()
    }

    /**
     * Set ui views visibility
     *
     * @param visible true to show, false to hide
     */
    private fun setVisibility(visible: Boolean) {
        var visibility = if (visible) View.VISIBLE else View.INVISIBLE
        guideTimelineGridView!!.visibility = visibility
        guideChannelListGridView!!.visibility = visibility
        guideEventsContainer!!.visibility = visibility
        dateTextView!!.visibility = visibility
        pastOverlay!!.visibility = visibility
    }

    /**
     * Show loading animation
     */
    private fun showLoading() {
        isLoading = true
        ReferenceApplication.runOnUiThread {
            loadingLayout!!.visibility = View.VISIBLE
        }
    }

    /**
     * Hide loading animation
     */
    private fun hideLoading() {
        ReferenceApplication.runOnUiThread {
            loadingLayout!!.visibility = View.INVISIBLE
        }
        Handler().postDelayed(Runnable {
            isLoading = false
        }, 1000)
    }

    override fun resume() {
        guideTimelineGridView!!.isLayoutFrozen = true
        guideTimelineGridView!!.post {
            guideTimelineGridView!!.isLayoutFrozen = false
        }
        Handler().postDelayed(Runnable {
            ReferenceApplication.runOnUiThread(Runnable {
                if (guideFilterGridView!!.hasFocus()) {
                    guideFilterCategoryAdapter.requestFocus(currentCategoryPosition)
                }
            })
        }, 500)
        refreshFavoriteButton()
    }

    override fun pause() {
        guideTimelineGridView!!.isLayoutFrozen = true
    }

    fun timelineEndPosition(): Int {
        return ((dayOffsetBottom - dayOffsetTop + 1) * 48) - GUIDE_TIMELINE_PAGE_SIZE
    }

    companion object {
        const val GUIDE_FILTER_ALL = 0
        val GUIDE_TIMELINE_ITEM_SIZE = Utils.getDimens(R.dimen.custom_dim_144)
        val GUIDE_TIMELINE_MINUTE = Utils.getDimens(R.dimen.custom_dim_4_8)
        const val GUIDE_TIMELINE_PAGE_SIZE = 2

        val DAY_OFFSET_MIN = -7
        val DAY_OFFSET_MAX = 7
        val ITEM_OFFSET_TRIGGER = 0
    }


    interface EpgEventFocus {
        fun onEventFocus(
            lastSelectedView: VerticalGuideEventListViewHolder,
            isSmallDuration: Boolean
        )
    }
}