package com.iwedia.cltv.anoki_fast.epg

import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.isBlockedContent
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.utils.Utils
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Guide event list adapter
 *
 * @author Dejan Nadj
 */
class FastLiveTabEventListAdapter(
    val listener: GuideEventListListener,
    private val dateTimeFormat: DateTimeFormat
) : RecyclerView.Adapter<FastLiveTabEventListViewHolder>() {

    //Items
    private var items = mutableListOf<TvEvent>()

    //Selected view holder
    private var lastSelectedView: FastLiveTabEventListViewHolder? = null

    //Selected item index
    private var selectedIndex = 0
    var initialFocusPosition = -1

    //Holders map
    private var holders = mutableMapOf<Int, FastLiveTabEventListViewHolder>()

    //Last view for which the text position is updated
    private var lastUpdatedView: FastLiveTabEventListViewHolder? = null
    private var lastUpdatedEvent: TvEvent? = null

    //Number of char length available in the last updated timeTextView
    private var numberOfCharsTime = 0

    //Last view for where remaining time is updated
    private var lastCurrentEventView: FastLiveTabEventListViewHolder? = null

    private var activeChannel: TvChannel?=null

    //start date of first visible timeline
    private var channelIndex: Int = -1
        private set
    //RLT
    var isRTL : Boolean= false

    var isWaitingForLayout : Boolean= true

    interface GuideEventListListener{
        fun getActiveChannel(callback : IAsyncDataCallback<TvChannel>)
        fun getTimelineStartTime(): Date
        fun getStartTime(): Date
        fun getEndTime(): Date
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun activeEventViewHolder(viewHolder: FastLiveTabEventListViewHolder)
        fun isEventLocked(tvEvent: TvEvent?): Boolean
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FastLiveTabEventListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_event_list_item, parent, false)

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            view?.isFocusable = false
            view?.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        }

        return FastLiveTabEventListViewHolder(view)
    }

    override fun onBindViewHolder(holder: FastLiveTabEventListViewHolder, pos: Int) {
        val position = pos

        holder.timeTextView!!.setDateTimeFormat(dateTimeFormat)

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            holder.itemView?.isFocusable = false
            holder.itemView?.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            holder.backgroundView?.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            holder.backgroundView?.isFocusable = false
            holder.rootView?.isFocusable = false
            holder.rootView?.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            holder.nameTextView?.isFocusable = false
            holder.nameTextView?.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            holder.timeTextView?.isFocusable = false
            holder.timeTextView?.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        }

        holders[position] = holder
        val item = items[position]
        listener.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {}
            override fun onReceive(data: TvChannel) {

                activeChannel = data
                var duration = items[position].endTime - items[position].startTime
                duration/=60000

                if (position==getActiveTimelineIndex()) {
                    if (((ReferenceApplication.worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.FAST_ONLY.ordinal) &&
                        (activeChannel!!.name == item.tvChannel.name && duration>=10 && getActiveEvent() == item)) {
                        holder.currentlyPlayingIcon!!.visibility = View.VISIBLE
                        listener.activeEventViewHolder(holder)
                    }else{
                        holder.currentlyPlayingIcon!!.visibility = View.GONE
                    }

                    holder.recordIndicator!!.visibility = View.GONE
                    holder.watchlistIndicator!!.visibility = View.GONE
                }else {
                    holder.currentlyPlayingIcon!!.visibility = View.GONE
                }

                /*
                * More event will cause more delay in initial loading.
                * Here we avoiding more event initially for quick loading,
                * Then after delay we are loading additional events
                * */
                val runnableUpdateWidth = Runnable {
                    val event = items[holder.bindingAdapterPosition]

                    val layoutParams = holder.rootView?.layoutParams

                    val startTime = maxOf(listener.getStartTime().time, event.startTime)
                    val endTime = minOf(listener.getEndTime().time, event.endTime)

                    val startTimeInMinutes = startTime/60000.0
                    val endTimeInMinutes = endTime/60000.0

                    val startOffset = startTimeInMinutes * FastLiveTab.GUIDE_TIMELINE_MINUTE
                    val endOffset = endTimeInMinutes * FastLiveTab.GUIDE_TIMELINE_MINUTE


                    var width = endOffset.toInt() - startOffset.toInt()

                    //if the start time and end time have less than 1 min diff then UI does not create event
                    //so setting it to the minimum length of 1 so that it could be visible.
                    if (width==0)width=1


                    layoutParams?.width = width
                    holder.rootView?.layoutParams = layoutParams
                    holder.rootView?.invalidate()
                }

                val time =
                    if (activeChannel!!.name == item.tvChannel.name || position ==0)
                        System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2)
                    else
                        listener.getTimelineStartTime().time + TimeUnit.MINUTES.toMillis(10)

                if(listener.getTimelineStartTime().time > System.currentTimeMillis()){
                    isWaitingForLayout = false
                }

                if(!isWaitingForLayout || time > item.startTime){
                    runnableUpdateWidth.run()
                    holder.itemView.post{
                        isWaitingForLayout = false
                    }
                }else{
                    holder.itemView.doOnPreDraw {
                        runnableUpdateWidth.run()
                        holder.itemView.post{
                            isWaitingForLayout = false
                        }
                    }
                }
            }
        })

        if (listener.isEventLocked(item)){
            holder.setParentalRestriction()
        }else{
            holder.lockImageView?.visibility = View.GONE
            holder.nameTextView?.text = item.name
            holder.nameTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        }

        setEventTime(holder, item)
        holder.timeTextView!!.setTextColor(
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_text_description"),
                    0.8
                )
            )
        )
        holder.backgroundView!!.background = ConfigColorManager.generateGuideDrawable()
        holder.currentlyPlayingIcon!!.clearColorFilter()
        holder.lockImageView.clearColorFilter()
        holder.recordIndicator!!.clearColorFilter()
        holder.watchlistIndicator!!.clearColorFilter()

        if (initialFocusPosition == position ) {
            showFocus(initialFocusPosition,
                object : FastLiveTab.EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: FastLiveTabEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        //we can ignore as it will be already open
                    }
                })
        }
        initialFocusPosition = -1

    }

    override fun onViewRecycled(holder: FastLiveTabEventListViewHolder) {
        if(lastCurrentEventView == holder) lastCurrentEventView = null
        holders.remove(holder.adapterPosition)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Show focus on the item inside the list
     * @param position item position
     */
    fun showFocus(position: Int,listener: FastLiveTab.EpgEventFocus) : Boolean {
        //Clear previously selected item
        clearFocus()
        lastSelectedView = holders[position]
        selectedIndex = position
        if (lastSelectedView != null) {
            lastSelectedView!!.nameTextView!!.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        "color_background"
                    )
                )
            )
            lastSelectedView!!.timeTextView!!.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        0.8
                    )
                )
            )
            lastSelectedView!!.backgroundView!!.background =
                ConfigColorManager.generateGuideFocusDrawable()
            lastSelectedView!!.currentlyPlayingIcon!!.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))
            lastSelectedView!!.lockImageView.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))
            lastSelectedView!!.recordIndicator!!.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))
            lastSelectedView!!.watchlistIndicator!!.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))

            var duration = (Date(items[position].endTime).time - Date(items[position].startTime).time)
            duration/=60000

            listener.onEventFocus(lastSelectedView!!,duration<20)
            return true
        }
        return false
    }

    /**
     * Show focus on the item for the specified time and date
     *
     * @param time  specifiedfixla time and date
     */
    fun showFocus(time: Date,listener: FastLiveTab.EpgEventFocus) : Boolean {
        val refTime = TimeUnit.MILLISECONDS.toMinutes(time.time)
        for (i in 0 until items.size) {
            val item = items[i]
            val startTime = TimeUnit.MILLISECONDS.toMinutes(item.startTime)
            val endTime = TimeUnit.MILLISECONDS.toMinutes(item.endTime)
            if (refTime in startTime until endTime) {
                return showFocus(i,listener)
            }
        }
        return false
    }

    /**
     * Get selected event inside the list
     */
    fun getSelectedEvent(): TvEvent? {
        return if (selectedIndex < items.size && selectedIndex>=0) {
            items[selectedIndex]
        } else
            null
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

    /**
     * Get selected item index
     */
    fun getSelectedIndex(): Int {
        return selectedIndex
    }

    /**
     * Clear selected item inside the list and restore focus
     */
    fun clearSelected() {
        clearFocus()
        if (lastSelectedView != null) {
            lastSelectedView!!.separator?.visibility = View.VISIBLE
            lastSelectedView!!.nameTextView!!.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        "color_background"
                    )
                )
            )
            lastSelectedView!!.timeTextView!!.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        0.8
                    )
                )
            )
            lastSelectedView!!.backgroundView!!.background =
                ConfigColorManager.generateGuideFocusDrawable()

            lastSelectedView!!.currentlyPlayingIcon!!.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))
            lastSelectedView!!.lockImageView.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))
            lastSelectedView!!.recordIndicator!!.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))
            lastSelectedView!!.watchlistIndicator!!.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))

        }
    }

    /**
     * Remove focus from the selected item inside the list
     */
    fun clearFocus() {
        if (lastSelectedView != null && getSelectedEvent()!=null) {
            lastSelectedView!!.nameTextView!!.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        "color_main_text"
                    )
                )
            )

            lastSelectedView!!.timeTextView!!.setTextColor(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_text_description"),
                        0.8
                    )
                )
            )

            lastSelectedView!!.backgroundView!!.background =
                ConfigColorManager.generateGuideDrawable()
            lastSelectedView!!.currentlyPlayingIcon!!.clearColorFilter()
            lastSelectedView!!.lockImageView.clearColorFilter()
            lastSelectedView!!.recordIndicator!!.clearColorFilter()
            lastSelectedView!!.watchlistIndicator!!.clearColorFilter()

        }
    }

    /**
     * Select next item inside the list
     * @param endDate timeline end date
     */
    fun selectNext(endDate: Date,listener: FastLiveTab.EpgEventFocus, forced: Boolean) {
        val itemStartDate = Date(items[selectedIndex].startTime)
        val itemEndDate = Date(items[selectedIndex].endTime)
        val endOfTheDay = itemStartDate.before(endDate) && (itemEndDate == endDate || itemEndDate.after(endDate))
        if (items.size > 1 && (selectedIndex+1) < items.size && !endOfTheDay) {
            var nextIndex = 0
            if (forced) {
                while (items[nextIndex].endTime < System.currentTimeMillis()) {
                    nextIndex++
                    // To avoid NullPointerException
                    if (nextIndex > items.lastIndex) {
                        nextIndex = selectedIndex
                        break
                    }
                }
                if (nextIndex == 0) nextIndex = selectedIndex
            } else {
                nextIndex = selectedIndex + 1
            }

            showFocus(nextIndex, listener)
        }

    }

    /**
     * Select previous item inside the list
     */
    fun selectPrevious(listener: FastLiveTab.EpgEventFocus) : Boolean {
        if (selectedIndex > 0 && items.size > 1) {
            return showFocus(selectedIndex-1,listener)
        }
        return false
    }

    //Refresh
    fun refresh(adapterItems: MutableList<TvEvent>) {
        holders.clear()
        var difference =  adapterItems.size - items.size

        if(difference!=0){
            //new added
            if(difference>0){
                notifyItemRangeInserted(items.size,Math.abs(difference))
            }
            //removed
            if(difference<0){
                notifyItemRangeRemoved(adapterItems.size,Math.abs(difference))
            }
        }

        if(adapterItems.size!=0) {
            //changed
            notifyItemRangeChanged(0, adapterItems.size)
        }

        this.items = adapterItems
    }

    //Refresh
    fun getActiveTimelineIndex() : Int {
        items.forEach {
            if( it.startTime/60000 <= Date(listener.getCurrentTime(items[0].tvChannel)).time/60000 &&  it.endTime/60000 > Date(listener.getCurrentTime(items[0].tvChannel)).time/60000)
                return  items.indexOf(it)
        }
        return 0
    }

    //Refresh
    fun getActiveEvent() : TvEvent? {
        items.forEach {
            if( it.startTime/60000 <= Date(listener.getCurrentTime(items[0].tvChannel)).time/60000 &&  it.endTime/60000 > Date(listener.getCurrentTime(items[0].tvChannel)).time/60000)
                return  it
        }
        return null
    }

    //Refresh
    fun isActiveIndexInsideFrame(timelineStartTime: Date, timelineEndTime: Date): Boolean {
        val item = items.get(getActiveTimelineIndex())

        return (item.startTime >= timelineStartTime.time && item.startTime <= timelineEndTime.time)
                || (item.endTime <= timelineEndTime.time && item.endTime >= timelineStartTime.time)
                || (item.startTime <= timelineStartTime.time && item.endTime >= timelineEndTime.time)
    }

    /**
     * update current channel index
     */
    fun setChannelIndex(index : Int){
        channelIndex = index
    }

    /**
     * Update name and time info text position inside the view
     *
     * @param startDate grid start date
     * @param animate enable animation
     * @return success status
     */
    fun updateTextPosition(startDate: Date, animateText: Boolean) : Boolean {

        var isAnimated = false
        if (lastUpdatedView != null ) {
            if (listener.isEventLocked(lastUpdatedEvent!!)) {
                lastUpdatedView!!.setParentalRestriction()
            }else{
                lastUpdatedView!!.nameTextView!!.text = lastUpdatedEvent!!.name
            }

            setEventTime(lastUpdatedView!!, lastUpdatedEvent!!)
            ObjectAnimator.ofFloat(lastUpdatedView!!.nameTextView!!, "translationX", 0f)
                .apply {
                    duration = 0
                    start()
                }
            ObjectAnimator.ofFloat(lastUpdatedView!!.timeTextView!!, "translationX", 0f)
                .apply {
                    duration = 0
                    start()
                }
            ObjectAnimator.ofFloat(lastUpdatedView!!.indicatorContainer!!, "translationX", 0f)
                .apply {
                    duration = 0
                    start()
                }
            lastUpdatedView = null
        }

        val timelineStartTime = listener.getStartTime()
        for (i in 0 until items.size) {
            val item = items[i]
            val holder : FastLiveTabEventListViewHolder?= holders[i]
            if(holder==null || holder.adapterPosition == -1) continue

            if ((item.startTime <= startDate.time && item.endTime > startDate.time)) {
                val startTime = maxOf(timelineStartTime.time, item.startTime)
                var diff = startDate.time - startTime
                if(isRTL) diff *= -1
                val width = item.endTime - startDate.time


                val charWidth = holder!!.nameTextView!!.paint.measureText("A")
                val charWidthTime = holder!!.timeTextView!!.paint.measureText("A")

                val numberOfChars =
                    ((((width / 60000.0) * FastLiveTab.GUIDE_TIMELINE_MINUTE) -
                            Utils.getDimensInPixelSize(R.dimen.custom_dim_15)) / charWidth).toInt()
                numberOfCharsTime =
                    ((((width / 60000.0) * FastLiveTab.GUIDE_TIMELINE_MINUTE) -
                            Utils.getDimensInPixelSize(R.dimen.custom_dim_15)) / charWidthTime).toInt()

                holder.indicatorContainer!!.alpha = 1f

                val name = if (listener.isEventLocked(item)) {
                    ConfigStringsManager.getStringById("parental_control_restriction")
                } else {
                    item.name
                }

                setEventTime(holder, item)
                val time = holder.timeTextView!!.text.toString()

                if (numberOfChars < 6) {
                    if (numberOfChars < 3) {
                        holder.indicatorContainer!!.alpha = 0f
                        holder.nameTextView!!.text = ""
                    } else {
                        holder.nameTextView!!.text = ". . ."
                    }
                    holder.timeTextView!!.text = ""
                } else{

                    holder.nameTextView?.text = if (numberOfChars < name.length) {
                        name.subSequence(0, numberOfChars - 3).toString() + "..."
                    } else {
                        name
                    }

                    holder.timeTextView!!.text = if (numberOfCharsTime < time.length) {
                        time.subSequence(0, numberOfCharsTime - 3).toString() + "..."
                    } else {
                        time
                    }
                }

                if (diff != 0L) {
                    holder!!.nameTextView!!.alpha = 0f
                    ObjectAnimator.ofFloat(holder!!.nameTextView!!, "alpha", 1f).apply {
                        interpolator = AccelerateDecelerateInterpolator()
                        duration = if(animateText) 1000 else 0
                        start()
                    }

                    holder!!.timeTextView!!.alpha = 0f
                    ObjectAnimator.ofFloat(holder!!.timeTextView!!, "alpha", 1f).apply {
                        interpolator = AccelerateDecelerateInterpolator()
                        duration = if(animateText) 1000 else 0
                        start()
                    }
                    holder!!.indicatorContainer!!.alpha = 0f
                    ObjectAnimator.ofFloat(holder!!.indicatorContainer!!, "alpha", 1f).apply {
                        interpolator = AccelerateDecelerateInterpolator()
                        duration = if(animateText) 1000 else 0
                        start()
                    }
                }

                ObjectAnimator.ofFloat(
                    holder!!.nameTextView!!,
                    "translationX",
                    ((diff / 60000.0) * FastLiveTab.GUIDE_TIMELINE_MINUTE).toFloat()
                ).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    duration = 0
                    start()
                }

                ObjectAnimator.ofFloat(
                    holder!!.timeTextView!!,
                    "translationX",
                    ((diff / 60000.0) * FastLiveTab.GUIDE_TIMELINE_MINUTE).toFloat()
                ).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    duration = 0
                    start()
                }

                ObjectAnimator.ofFloat(
                    holder!!.indicatorContainer!!,
                    "translationX",
                    ((diff / 60000.0) * FastLiveTab.GUIDE_TIMELINE_MINUTE).toFloat()
                ).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    duration = 0
                    start()
                }

                lastUpdatedView = holder
                lastUpdatedEvent = item

                // To fix rtl text visibility when scroll up down
                if(isRTL && !animateText){
                    holder.nameTextView!!.requestLayout()
                    holder.timeTextView!!.requestLayout()
                }
                isAnimated  =  true
            }
        }
        return isAnimated
    }

    /**
     * To update remaining time in current event every minute
     */
    fun refreshRemainingTime() {

        var isLastUpdatedViewAffected = false

        var tvEvent : TvEvent? = null
        var holder: FastLiveTabEventListViewHolder? = null

        for (i in 0 until items.size) {
            if (holders[i] == null || holders[i]!!.adapterPosition == -1) continue
            if(FastLiveTabDataProvider.isCurrentEvent(items[i])){
                tvEvent = items[i]
                holder = holders[i]
            }
        }

        // to reset the last current event
        lastCurrentEventView?.let { holder ->
            if (holder.timeTextView!!.text.toString() != "") {
                items.getOrNull(holder.bindingAdapterPosition)?.let { tvEvent ->
                    setEventTime(holder, tvEvent)
                    isLastUpdatedViewAffected = lastUpdatedView == holder
                }
            }
        }

        // to set remaining time for current event
        if (holder!=null && holder.timeTextView!!.text.toString() != "") {
            setEventTime(holder, tvEvent!!)
            isLastUpdatedViewAffected = lastUpdatedView == holder
        }

        // to fix text length if its a first item
        if (isLastUpdatedViewAffected) {

            lastUpdatedView?.timeTextView?.let{
                val text = it.text.toString()
                it.text = if (numberOfCharsTime < text.length) {
                    text.subSequence(0, numberOfCharsTime - 3).toString() + "..."
                } else {
                    text
                }
            }
        }
    }

    /**
     * Displays either the "start-end time" or "remaining time" for the event
     * Sets alpha to 0.2f if it's a past event
     *
     * @param holder target Holder
     * @param tvEvent tv event
     */
    private fun setEventTime(holder: FastLiveTabEventListViewHolder, tvEvent: TvEvent){

        if(FastLiveTabDataProvider.isCurrentEvent(tvEvent)){
            lastCurrentEventView = holder
        } else if(lastCurrentEventView == holder){
            lastCurrentEventView = null
        }

        if(FastLiveTabDataProvider.isCurrentEvent(tvEvent)){
            holder.timeTextView!!.text =
                FastLiveTabDataProvider.formatRemainingTime(tvEvent)
        }else{
            holder.timeTextView!!.time = tvEvent
        }

        holder.rootView!!.alpha = if(FastLiveTabDataProvider.isPastEvent(tvEvent)) 0.2f else 1f // used to be able to distinguish all events from the past which are no more focusable.
    }

    /**
     * Get Position by time
     *
     * @param startTime search start time
     */
    fun getPositionByTime(startTime : Long): Int {
        items.forEach { item ->
            if (item.startTime/60000 <= startTime/60000 && item.endTime/60000 > startTime/60000) {
                return items.indexOf(item)
            }
        }
        return 0
    }

    //Pagination
    fun extend(extendingPreviousDay: Boolean, size: Int) {
        if(extendingPreviousDay){
            selectedIndex += size
            val holders = mutableMapOf<Int, FastLiveTabEventListViewHolder>()
            this.holders.forEach { (key, value) ->
                val newIndex = size+ key
                if(newIndex<items.size)
                    holders[newIndex] = value
            }
            this.holders = holders
        }
    }

    fun remove(extendingPreviousDay: Boolean) {
        if(!extendingPreviousDay){
            selectedIndex --

            val holders = mutableMapOf<Int, FastLiveTabEventListViewHolder>()
            this.holders.forEach { (key, value) ->
                val newIndex = key-1
                if(newIndex>=0)
                    holders[newIndex] = value
            }
            this.holders = holders
        }
    }

    fun onItemClicked(onClick: () -> Unit) {
        // Invoke the provided onClick function.
        onClick.invoke()

        // if animation of the item is needed this is place where to add it.
        // EXAMPLE for animation:
        /*lastSelectedView?.rootView?.let {
            it.animate()
                .scaleY(0.95f)
                .scaleX(0.95f)
                .setDuration(300)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                    }

                    override fun onAnimationEnd(p0: Animator) {
                        onClick.invoke()
                    }

                    override fun onAnimationCancel(p0: Animator) {
                    }

                    override fun onAnimationRepeat(p0: Animator) {
                    }

                }).start()
        }*/
    }

}