package com.iwedia.cltv.scene.home_scene.guideVertical

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.scene.home_scene.guideVertical.VerticalGuideEventListAdapter.EPGFocusState.*
import com.iwedia.cltv.scene.home_scene.guideVertical.VerticalGuideEventListAdapter.EPGItemPosition.*
import com.iwedia.cltv.utils.Utils
import java.util.*
import kotlin.math.roundToInt

/**
 * Vertical guide event list adapter
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideEventListAdapter(val dateTimeFormat: DateTimeFormat) : RecyclerView.Adapter<VerticalGuideEventListViewHolder>() {

    //Items
    private var items = mutableListOf<TvEvent>()

    //Selected item index
    private var selectedIndex = 0
    var initialFocusPosition = -1
    private var focusState: EPGFocusState = STATE_NONE

    //Last view for which the text position is updated
    private var lastUpdatedView: VerticalGuideEventListViewHolder? = null
    private var activeChannel: TvChannel? = null

    //start date of first visible timeline
    private var startDate: Date? = null
    private var topPosition = -1
    private var animateText = false
    private var channelIndex: Int = -1
    private var nextChannelItems = mutableListOf<TvEvent>()
    private var epgItemPosition: EPGItemPosition = NONE
    var listener: VerticalGuideSceneWidget.EpgEventFocus? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VerticalGuideEventListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_event_list_item_vertical, parent, false)

        val guideEventName: TextView = view.findViewById(R.id.guide_event_name)
        guideEventName.setText(ConfigStringsManager.getStringById("top_10"))

        val guide_event_time_info: TextView = view!!.findViewById(R.id.guide_event_time_info)
        guide_event_time_info.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        return VerticalGuideEventListViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerticalGuideEventListViewHolder, position: Int) {
        val item = items[position]

        holder.timeTextView!!.setDateTimeFormat(dateTimeFormat)

        var duration = items[position].endTime - items[position].startTime
        duration /= 60000

        val startTimeInMinutes = item.startTime / 60000.0
        val endTimeInMinutes = item.endTime / 60000.0

        val startOffset = startTimeInMinutes * VerticalGuideSceneWidget.GUIDE_TIMELINE_MINUTE
        var endOffset = endTimeInMinutes * VerticalGuideSceneWidget.GUIDE_TIMELINE_MINUTE

        if (items.lastIndex == position) endOffset += Utils.getDimens(R.dimen.custom_dim_69)


        if (position == getActiveTimelineIndex()) {
            holder.currentlyPlayingIcon!!.visibility =
                if (activeChannel!!.id == item.tvChannel.id && duration >= 10) View.VISIBLE else View.GONE
            holder.recordIndicator!!.visibility = View.GONE
            holder.watchlistIndicator!!.visibility = View.GONE
        } else {
            holder.currentlyPlayingIcon!!.visibility = View.GONE
            val isFuture = item.startTime > Date().time
            //TODO Commented due to pending refactor

            /*     holder.recordIndicator!!.visibility =
                     if (isFuture && ReferenceSdk.pvrSchedulerHandler!!.isInReclist(item) && duration >= 10) View.VISIBLE else View.GONE
                 holder.watchlistIndicator!!.visibility =
                     if (isFuture && ReferenceSdk.watchlistHandler!!.isInWatchlist(item) && duration >= 10) View.VISIBLE else View.GONE*/
        }
        val height = endOffset.toInt() - startOffset.toInt()
        if (holder.bindPosition != position || holder.channelIndex != channelIndex || height != holder.availableHeight) {

            holder.apply {

                holder.nameTextView?.text = item.name
                holder.timeTextView?.time = item
                val layoutParams = holder.rootView?.layoutParams
                layoutParams?.height = endOffset.toInt() - startOffset.toInt()
                rootView?.layoutParams = layoutParams
                rootView?.invalidate()

                updateViewsForSameProgram(this, item)

                nameTextView?.let { nameTv ->
                    nameTv.text = item.name
                    nameTv.maxLines = Int.MAX_VALUE
                    nameTv.paint?.fontMetrics?.let { lineHeightName = it.bottom - it.top }
                    requiredLineCountName = calculateRequiredLineCount(
                        nameTv, Utils.getDimensInPixelSize(R.dimen.custom_dim_132), lineHeightName
                    )
                }
                timeTextView?.let { timeTv ->
                    timeTv.time = item
                    timeTv.maxLines = Int.MAX_VALUE
                    timeTv.paint?.fontMetrics?.let {
                        lineHeightTime = it.bottom - it.top
                    }
                }



                updateViewColors(
                    this,
                    "color_main_text",
                    "color_text_description",
                    getBackgroundDrawable(false, rootView?.context)
                )
                availableHeight = height
                requiredLineCountTime = 3
                bindPosition = position
            }


            holder.channelIndex = channelIndex
        }

        startDate?.let {
            if (topPosition == position) {

                val diff = it.time - item.startTime
                val height = item.endTime - it.time
                val availableHeight =
                    ((height / 60000) * VerticalGuideSceneWidget.GUIDE_TIMELINE_MINUTE).toInt()

                updateText(availableHeight, holder)

                if (diff != 0L) {
                    holder.nameTextView?.alpha = 0f
                    ObjectAnimator.ofFloat(holder.nameTextView, "alpha", 1f).apply {
                        interpolator = AccelerateDecelerateInterpolator()
                        duration = if (animateText) 500 else 0
                        start()
                    }

                    holder.timeTextView?.alpha = 0f
                    ObjectAnimator.ofFloat(holder.timeTextView, "alpha", 1f).apply {
                        interpolator = AccelerateDecelerateInterpolator()
                        duration = if (animateText) 500 else 0
                        start()
                    }

                    holder.indicatorContainer?.alpha = 0f
                    ObjectAnimator.ofFloat(holder!!.indicatorContainer!!, "alpha", 1f)
                        .apply {
                            interpolator = AccelerateDecelerateInterpolator()
                            duration = if (animateText) 500 else 0
                            start()
                        }
                }
                animateText = false

                val diffMinutes = (diff.toFloat() / 60000)
                val y = diffMinutes * VerticalGuideSceneWidget.GUIDE_TIMELINE_MINUTE

                holder.nameTextView?.translationY = y
                holder.timeTextView?.translationY = y
                holder.indicatorContainer?.translationY = y
                lastUpdatedView = holder
            } else updateText(holder.availableHeight, holder)
        } ?: updateText(holder.availableHeight, holder)


        if (initialFocusPosition == position) {
            selectedIndex = initialFocusPosition
            initialFocusPosition = -1
        }

        val focusState = if (selectedIndex == position) focusState else STATE_NONE

        when (focusState) {
            STATE_FOCUSED, STATE_DUMMY_FOCUSED -> showFocus(holder)
            STATE_SELECTED -> staySelected(holder)
            STATE_NONE -> clearFocus(holder)
        }


    }

    private fun updateViewsForSameProgram(
        holder: VerticalGuideEventListViewHolder,
        item: TvEvent
    ) {
        if (item.isProgramSame && item.providerFlag != null) {
            val hasNextProgram = nextChannelItems.indexOfFirst {
                it.providerFlag == item.providerFlag
            }

            if (hasNextProgram >= 0) {
                holder.rootView?.setPadding(0, 0, 0, 0)
            }

            if (!item.isInitialChannel) {
                holder.nameTextView?.visibility = INVISIBLE
                holder.timeTextView?.visibility = INVISIBLE
                epgItemPosition = if (hasNextProgram >= 0) CENTER else RIGHT
            } else {
                epgItemPosition = LEFT
            }

            holder.rootView?.context?.let {
                val drawable = if (epgItemPosition == CENTER) {
                    GradientDrawable()
                } else {
                    ContextCompat.getDrawable(
                        it,
                        if (epgItemPosition == LEFT)
                            R.drawable.left_rounded_corner_background
                        else
                            R.drawable.right_rounded_corner_background
                    )?.mutate() as GradientDrawable
                }
                drawable.setColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                holder.separator?.background = drawable
            }
        }
    }

    fun updateNextChannelList(list: MutableList<TvEvent>) {
        nextChannelItems = list
    }

    private fun getBackgroundDrawable(isFocused: Boolean, context: Context? = null): Drawable {
        return when (epgItemPosition) {
            LEFT -> ConfigColorManager.generateGuideEventListItemDrawable(
                context,
                isFocused = isFocused,
                isLeft = true
            )
            CENTER -> ConfigColorManager.generateGuideEventListItemDrawable(
                context,
                isFocused = isFocused,
                isCenter = true
            )
            RIGHT -> ConfigColorManager.generateGuideEventListItemDrawable(
                context,
                isFocused = isFocused,
                isLeft = false
            )
            NONE -> {
                if (isFocused)
                    ConfigColorManager.generateGuideFocusDrawable()
                else
                    ConfigColorManager.generateGuideDrawable()
            }
        }
    }

    /**
     * To calculate the actial line count
     * @param textview view to measure
     * @param width width
     * @param singleLineHeight single line height
     * @return measured line count
     */
    private fun calculateRequiredLineCount(
        textview: TextView,
        width: Int,
        singleLineHeight: Float
    ): Int {
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        textview.measure(widthMeasureSpec, heightMeasureSpec)
        return (textview.measuredHeight / singleLineHeight).roundToInt()
    }

    /**
     * To calculate the line count for name and time
     * @param lineHeightName single line height of name
     * @param lineHeightTime single line height of time
     * @param availableHeight item position
     * @param maxLineCountNameAndTime [0] - maxLinesName count, [1] - maxLinesTime count,
     * @return [0] - calculated maxLinesName count, [1] -calculated maxLinesTime count,
     */
    private fun calculateLineCountBasedOnAvailableHeight(
        lineHeightName: Float,
        lineHeightTime: Float,
        availableHeight: Int,
        maxLineCountNameAndTime: Array<Int>
    ): Array<Int> {

        val maxLinesName = maxLineCountNameAndTime[0]
        val maxLinesTime = maxLineCountNameAndTime[1]

        return if (availableHeight > (lineHeightName * maxLinesName + lineHeightTime * maxLinesTime) || maxLinesName == 0) {
            arrayOf(maxLinesName, maxLinesTime)
        } else {
            calculateLineCountBasedOnAvailableHeight(
                lineHeightName, lineHeightTime, availableHeight, arrayOf(
                    if (maxLinesTime == 0) maxLinesName - 1 else maxLinesName,
                    if (maxLinesTime == 0) 0 else maxLinesTime - 1
                )
            )
        }
    }


    fun getActiveTimelineIndex(): Int {
        items.forEach {

            if (it.startTime <= Date().time && it.endTime >= Date().time)
                return items.indexOf(it)
        }
        return 0
    }

    /**
     * To update line count based on available height
     * @param availableHeight item position
     * @param holder holder
     */
    private fun updateText(available: Int, holder: VerticalGuideEventListViewHolder) {

        holder.nameTextView?.maxLines = Int.MAX_VALUE
        holder.timeTextView?.maxLines = Int.MAX_VALUE

        val topMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_10)
        var bottomMargin = Utils.getDimensInPixelSize(R.dimen.custom_dim_6)
        var availableHeight = available - topMargin//- bottomMargin
        var iconHeight = 30

        if (holder.currentlyPlayingIcon?.visibility == VISIBLE || holder.recordIndicator?.visibility == VISIBLE || holder.watchlistIndicator?.visibility == VISIBLE) {

            availableHeight -= iconHeight
        }
        if (availableHeight < iconHeight) {
            holder.indicatorContainer?.visibility = GONE
            holder.currentlyPlayingIcon?.visibility = GONE
            holder.recordIndicator?.visibility = GONE
            holder.watchlistIndicator?.visibility = GONE
        } else {
            holder.indicatorContainer?.visibility = VISIBLE
        }
        val maxLineCountNameAndTime: Array<Int> = with(holder) {
            calculateLineCountBasedOnAvailableHeight(
                lineHeightName,
                lineHeightTime,
                availableHeight,
                arrayOf(requiredLineCountName, requiredLineCountTime)
            )
        }

        holder.nameTextView?.maxLines = maxLineCountNameAndTime[0]
        holder.timeTextView?.maxLines = maxLineCountNameAndTime[1]
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Get selected event inside the list
     */
    fun getSelectedEvent(): TvEvent? {
        return if (selectedIndex < items.size) {
            items[selectedIndex]
        } else
            null
    }

    /**
     * Get selected item index
     */
    fun getSelectedIndex(): Int {
        return selectedIndex
    }

    /**
     * Get selected item index
     */
    fun setSelectedIndex(selectedIndex: Int) {
        this.selectedIndex = selectedIndex
    }

    /**
     * Mark as selected current focused item inside the list
     */
    fun staySelected() {
        focusState = STATE_SELECTED
        notifyItemChanged(selectedIndex, Object())
    }

    /**notify
     * Clear selected item inside the list and restore focus
     */
    fun clearSelected() {
        focusState = STATE_FOCUSED
        notifyItemChanged(selectedIndex, Object())
    }

    /**
     * Remove focus from the selected item inside the list
     */
    fun clearFocus() {
        focusState = STATE_NONE
        notifyItemChanged(selectedIndex, Object())
    }

    /**
     * Remove focus from holder
     * @param holder holder
     */
    fun clearFocus(holder: VerticalGuideEventListViewHolder?) {
        holder?.let {
            it.separator?.visibility = VISIBLE
            updateViewColors(
                it,
                "color_main_text",
                "color_text_description",
                getBackgroundDrawable(false, it.rootView?.context)
            )
        }
    }

    /**
     * Show focus on the item for the specified time and date
     *
     * @param time  specified time and date
     */
    fun showFocus(time: Date, listener: VerticalGuideSceneWidget.EpgEventFocus): Int? {
        for (i in 0 until items.size) {
            val item = items[i]
            if (item.startTime <= time.time &&
                item.endTime > time.time
            ) {

                showFocus(i, listener)
                return i
            }
        }
        return null
    }


    /**
     * Show focus on the item inside the list
     * @param position item position
     */
    fun showFocus(position: Int, listener: VerticalGuideSceneWidget.EpgEventFocus) {
        this.listener = listener
        val previous = selectedIndex
        selectedIndex = position
        notifyItemChanged(previous, Object())
        focusState = STATE_FOCUSED
        notifyItemChanged(selectedIndex, Object())
    }

    /*
     * Show focus on the item inside the list
     * @param holder holder
     */
    private fun showFocus(holder: VerticalGuideEventListViewHolder?) {

        holder?.let {

            updateViewColors(
                it,
                "color_background",
                "color_background",
                getBackgroundDrawable(true, it.rootView?.context)
            )
            holder.itemView.doOnPreDraw {
                var duration =
                    (items[holder.adapterPosition].endTime - items[holder.adapterPosition].startTime)
                duration /= 60000
                listener?.onEventFocus(holder, duration < 20)

            }
        }

    }


    /**
     * Shows an Item as focused without having focus when position
     * matches with adjacent channel Program
     */
    fun showFocusedProgram(index: Int, listener: VerticalGuideSceneWidget.EpgEventFocus) {
        if (index >= 0 && index < items.size && items[index].isProgramSame) {
            this.listener = listener
            selectedIndex = index
            focusState = STATE_DUMMY_FOCUSED
            notifyItemChanged(index, Object())
        }
    }

    private fun updateViewColors(
        holder: VerticalGuideEventListViewHolder,
        nameColor: String,
        timeColor: String,
        background: Drawable
    ) {
        holder.nameTextView?.setTextColor(Color.parseColor(ConfigColorManager.getColor(nameColor)))
        holder.timeTextView?.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(ConfigColorManager.getColor(timeColor), 0.8)
            )
        )
        holder.currentlyPlayingIcon!!.setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor(nameColor),
                    1.0
                )
            )
        )

        holder.recordIndicator!!.setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor(nameColor),
                    1.0
                )
            )
        )
        holder.watchlistIndicator!!.setColorFilter(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor(nameColor),
                    1.0
                )
            )
        )
        holder.backgroundView?.background = background


    }

    /**
     * Mark as selected current focused item inside the list
     * @param holder holder
     */
    private fun staySelected(holder: VerticalGuideEventListViewHolder?) {
        clearFocus(holder)
        holder?.separator?.visibility = INVISIBLE
        holder?.backgroundView?.background = ConfigColorManager.generateGuideSelectedDrawable()
    }

    /**
     * Select next item inside the list
     * @param endDate timeline end date
     */
    fun selectNext(endDate: Date, listener: VerticalGuideSceneWidget.EpgEventFocus) {
        val itemStartDate = Date(items[selectedIndex].startTime)
        val itemEndDate = Date(items[selectedIndex].endTime)
        val endOfTheDay =
            itemStartDate.before(endDate) && (itemEndDate == endDate || itemEndDate.after(endDate))
        if (items.size > 1 && (selectedIndex + 1) < items.size && !endOfTheDay) {
            showFocus(selectedIndex + 1, listener)
        }
    }

    /**
     * Select previous item inside the list
     */
    fun selectPrevious(listener: VerticalGuideSceneWidget.EpgEventFocus) {
        if (selectedIndex > 0 && items.size > 1) {
            showFocus(selectedIndex - 1, listener)
        }
    }

    //Refresh
    fun refresh(adapterItems: MutableList<TvEvent>) {

        val difference = adapterItems.size - items.size

        if (difference != 0) {
            //new added
            if (difference > 0) {
                notifyItemRangeInserted(items.size, Math.abs(difference))
            }
            //removed
            if (difference < 0) {
                notifyItemRangeRemoved(adapterItems.size, Math.abs(difference))
            }
        }

        if (adapterItems.size != 0) {
            //changed
            notifyItemRangeChanged(0, adapterItems.size)
        }

        this.items = adapterItems
    }

    /**
     * update current channel index
     */
    fun setChannelIndex(index: Int) {
        channelIndex = index
    }

    /**
     * Update name and time info text position inside the view
     *
     * @param startDate grid start date
     * @param animate enable animation
     */
    fun updateTextPosition(
        startDate: Date,
        animateText: Boolean,
        listener: VerticalGuideSceneWidget.EpgEventFocus
    ) {
        lastUpdatedView?.let { resetTextPosition(it) }
        lastUpdatedView = null

        this.startDate = startDate
        this.animateText = animateText

        for (i in 0 until items.size) {
            val item = items[i]
            if (item.startTime <= startDate.time && item.endTime > startDate.time) {
                topPosition = i
                this.listener = listener
                notifyItemChanged(i)
                return
            }
        }
    }

    /**
     * reset name and time info text position inside the view
     *
     * @param holder holder
     */
    private fun resetTextPosition(holder: VerticalGuideEventListViewHolder) {
        holder.nameTextView?.let {
            it.clearAnimation()
            it.translationY = 0f
            it.alpha = 1f
        }
        holder.timeTextView?.let {
            it.clearAnimation()
            it.translationY = 0f
            it.alpha = 1f
        }
        holder.indicatorContainer?.let {
            it.clearAnimation()
            it.translationY = 0f
            it.alpha = 1f
        }

        updateText(holder.availableHeight, holder)
    }


    fun refreshIndicators(position: Int, listener: VerticalGuideSceneWidget.EpgEventFocus) {
        this.listener = listener
        notifyItemChanged(position)
    }

    /**
     * Get Position by time
     *
     * @param startTime search start time
     */
    fun getPositionByTime(startTime: Long): Int {
        items.forEach { item ->
            if (item.startTime / 60000 <= startTime / 60000 && item.endTime / 60000 > startTime / 60000) {
                return items.indexOf(item)
            }
        }
        return 0
    }

    /**
     * Get tv event at the position
     *
     * @param position
     * @return tv event
     */
    fun getEvent(position: Int): TvEvent {
        return items[position]
    }

    override fun getItemViewType(position: Int) = position

    enum class EPGFocusState {
        STATE_NONE,
        STATE_FOCUSED,
        STATE_DUMMY_FOCUSED,
        STATE_SELECTED
    }

    enum class EPGItemPosition {
        LEFT,
        CENTER,
        RIGHT,
        NONE
    }
}