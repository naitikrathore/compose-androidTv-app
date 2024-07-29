package com.iwedia.cltv.scene.home_scene.guide

import android.animation.Animator
import android.graphics.PointF
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.view.ViewCompat
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.runOnUiThread
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.utils.Utils
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Guide events container adapter
 *
 * @author Dejan Nadj
 */
class GuideEventsContainerAdapter(val listener: GuideEventsContainerListener, private val dateTimeFormat: DateTimeFormat) : RecyclerView.Adapter<GuideEventsContainerViewHolder>() {

    private val TAG = javaClass.simpleName
    //Items
    private var items = mutableListOf<MutableList<TvEvent>>()
    //View holders map
    private var holders = HashMap<Int, GuideEventsContainerViewHolder>()
    //Scroll onEndingShowAnimation date
    private var startDate: Date?= null
    //current channel index
    private var channelIndex = -1
    //common viewPool for all events
    private val viewPool = SynchronizedRecycledViewPool()
    //RLT
    var isRTL : Boolean= false
    private var scrollPosition : Date? = null

    interface GuideEventsContainerListener{
        fun isAccessibilityEnabled(): Boolean
        fun getActiveChannel(callback : IAsyncDataCallback<TvChannel>)
        fun getStartTime(): Date
        fun getEndTime(): Date
        fun isInWatchList(event: TvEvent): Boolean
        fun isInRecList(event: TvEvent): Boolean
        suspend fun getCurrentTime(tvChannel: TvChannel): Long
        fun isEventLocked(tvEvent: TvEvent?): Boolean
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        isRTL  = (recyclerView.context as ReferenceApplication).activity!!.window.decorView.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideEventsContainerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_events_container_item, parent, false)

        if(listener.isAccessibilityEnabled()) {
            view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            view?.isFocusable = false
        }

        val holder = GuideEventsContainerViewHolder(view)

        if(listener.isAccessibilityEnabled()) {
            holder.listView!!.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            holder.listView!!.isFocusable = false
        }

        holder.listView!!.setRecycledViewPool(viewPool)

        val adapter = GuideEventListAdapter(object : GuideEventListAdapter.GuideEventListListener{
            override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>) {
                listener.getActiveChannel(callback)
            }
            override fun getStartTime():Date {
                return listener.getStartTime()
            }

            override fun getEndTime(): Date {
                return listener.getEndTime()
            }

            override fun isInWatchList(event: TvEvent): Boolean {
                return  listener.isInWatchList(event)
            }

            override fun isInRecList(event: TvEvent): Boolean {
                return  listener.isInRecList(event)
            }

            override suspend fun getCurrentTime(tvChannel: TvChannel): Long {
                return listener.getCurrentTime(tvChannel)
            }

            override fun isEventLocked(tvEvent: TvEvent?) = listener.isEventLocked(tvEvent)
        },dateTimeFormat)

        holder.listView!!.adapter = adapter
        holder.listView!!.itemAnimator = null
        holder.listView!!.layoutManager = LinearLayoutManager(holder.listView!!.context,
            LinearLayoutManager.HORIZONTAL,false)

        return holder
    }

    override fun onBindViewHolder(holder: GuideEventsContainerViewHolder, position: Int) {
        holders[position] = holder

        val adapter = (holder.listView!!.adapter as GuideEventListAdapter)
        adapter.setChannelIndex(position)
        adapter.refresh(items[position])

        if (startDate != null) {
            adapter.updateTextPosition(startDate!!, false)
        }
    }

    override fun onViewAttachedToWindow(holder: GuideEventsContainerViewHolder) {
        super.onViewAttachedToWindow(holder)
        holders[holder.adapterPosition] = holder
        val adapter = (holder.listView!!.adapter as GuideEventListAdapter)
        adapter.refresh(items[holder.adapterPosition])
        scrollToTime(holder)
    }

    fun updateIcon(){
        notifyDataSetChanged()

    }
    override fun getItemCount(): Int {
        return items.size
    }

    override fun onViewDetachedFromWindow(holder: GuideEventsContainerViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holders.remove(holder.adapterPosition)
    }

    /**
     * Show channel list separator view
     *
     * @param index item index
     * @param show show or hide separator view
     */
    fun showSeparator(index: Int, show: Boolean,listener:HorizontalGuideSceneWidget.EndingTranslationAnimation) {
        holders[index]?.listView?.isLayoutFrozen = true
        holders.values.forEach { holder ->
            holder.separatorView?.visibility = View.GONE
        }
        if (show) {
            holders[index]?.separatorView?.visibility = View.VISIBLE
            this.holders.forEach { (key, holder) ->
                if(key>index){
                    holder.itemView.translationY = -Utils.getDimens(R.dimen.custom_dim_186)
                    holder.itemView.animate().setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(p0: Animator) {
                        }

                        override fun onAnimationEnd(p0: Animator) {
                            holder.itemView.translationY = 0f
                        }

                        override fun onAnimationCancel(p0: Animator) {
                            holder.itemView.translationY = 0f
                        }

                        override fun onAnimationRepeat(p0: Animator) {
                        }
                    }).translationY(0f).duration = 200
                }

            }
            listener.onEndingShowAnimation()



        } else {
            if (index == itemCount-1) {
                listener.onEndingHideAnimation()
                holders[index]?.listView?.isLayoutFrozen = false
                return
            }
            this.holders.forEach { key, holder ->

                if(key>index){
                    holder.itemView.translationY = Utils.getDimens(R.dimen.custom_dim_186)
                    holder.itemView.animate().setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(p0: Animator) {
                        }

                        override fun onAnimationEnd(p0: Animator) {
                            holder.itemView.translationY = 0f
                            holders[index]?.separatorView?.visibility = View.GONE
                        }

                        override fun onAnimationCancel(p0: Animator) {
                            holder.itemView.translationY = 0f
                            holders[index]?.separatorView?.visibility = View.GONE
                        }

                        override fun onAnimationRepeat(p0: Animator) {
                        }
                    }).translationY(0f).duration = 200
                }

            }
            listener.onEndingHideAnimation()


        }
        holders[index]?.listView?.isLayoutFrozen = false
    }

   /**
    * Hide channel list separator view instantly
    */
    fun resetSeparator() {
        holders.values.forEach { holder ->
            holder.separatorView?.visibility = View.GONE
            holder.itemView.clearAnimation()
            holder.itemView.translationY = 0f
        }
    }

    fun setScrollPosition(scrollPosition: Date){
        this.scrollPosition = scrollPosition
    }
    //Refresh
    fun refresh(adapterItems: MutableList<MutableList<TvEvent>>) {
        holders.clear()
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    //Refresh
    fun addContainersToEnd(adapterItems: MutableList<MutableList<TvEvent>>) {
        items.addAll(adapterItems)
        runOnUiThread{notifyItemRangeInserted(itemCount,adapterItems.size) }
    }

    fun insertContainersToStart(adapterItems: MutableList<MutableList<TvEvent>>){
        items.addAll(0,adapterItems)
        val holders = HashMap<Int, GuideEventsContainerViewHolder>()

        this.holders.forEach { (key, value) ->
            holders[adapterItems.size+ key] = value
        }
        this.holders = holders
        notifyItemRangeInserted(0,adapterItems.size)
    }

    fun removeChannelEvents(position: Int){
        items.removeAt(position)
        notifyDataSetChanged()
    }

    //Pagination
    fun extendTimeline(
        eventContainerList: MutableList<MutableList<TvEvent>>,
        extendingPreviousDay: Boolean,
        selectedEventListView: Int,
        timelineGridViewHasFocus: Boolean,
        dayCount: Int,
        timelineStartOffset: Int
    )
    {
        val timelineStartTime = listener.getStartTime()
        val timelineEndTime = listener.getEndTime()

        items.forEachIndexed { channelIndex, eventList ->

            //merging the last and first events if they are same
            var additionalList = eventContainerList[channelIndex]
            val holder = holders[channelIndex]
            val isVisible = holder!=null && holder.adapterPosition!=-1

            //removing the previous day event to make events list only for two days max at a time
            if(dayCount >1 ) {
                if (extendingPreviousDay) {
                    eventList.reversed().forEach {
                        if(it.startTime > timelineEndTime.time){
                            if(isVisible){
                                val adapter = holder!!.listView!!.adapter as GuideEventListAdapter
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "extendTimeline prev: ${adapter.itemCount}")
                                adapter.notifyItemRemoved(eventList.lastIndex)
                            }
                            eventList.remove(it)
                        }
                    }
                }else{
                    val date = Date(eventList.first().endTime).date
                    eventList.toMutableList().forEach {
                        if(it.endTime < timelineStartTime.time){
                            eventList.remove(it)
                            if(isVisible){
                                val adapter = holder!!.listView!!.adapter as GuideEventListAdapter
                                adapter.notifyItemRemoved(0)
                                adapter.remove(false)
                            }
                        }
                    }
                }
            }

            var insertPosition =  if(!extendingPreviousDay) eventList.size else 0

            if (additionalList.first().id == eventList.last().id && additionalList.first().id  !=-1   && !extendingPreviousDay){
                var event = eventList.last()
                event.endTime =  additionalList.first().endTime
                eventList.removeLast()
                insertPosition-=1
                additionalList.removeFirst()
                additionalList.add(0,event)
                if(isVisible){
                    val adapter = holder!!.listView!!.adapter as GuideEventListAdapter
                    adapter.notifyItemChanged(eventList.size)
                }
            }

            else if (additionalList.last().id == eventList.first().id && additionalList.last().id  !=-1   && extendingPreviousDay){
                val removedEvent = eventList.removeFirst()
                if(isVisible){
                    val adapter = holder!!.listView!!.adapter as GuideEventListAdapter
                    if ( selectedEventListView == channelIndex  && timelineGridViewHasFocus) adapter.initialFocusPosition =
                            (additionalList.lastIndex)
                    adapter.notifyItemRemoved(0)
                }
                val targetEvent = additionalList.last()
                targetEvent.endTime = removedEvent.endTime
                notifyItemChanged(channelIndex)
            }

            else if (additionalList.last().id  == -1 && channelIndex == 0 && holder!=null){
                val adapter = holder!!.listView!!.adapter as GuideEventListAdapter
                //show focus on epg event while changing the date filter
                if ( selectedEventListView == 0  && timelineGridViewHasFocus) adapter.initialFocusPosition = additionalList.lastIndex+1
                adapter.notifyItemChanged(0)
            }

            eventList.addAll(insertPosition,additionalList)
            if(isVisible){
                val adapter = holder!!.listView!!.adapter as GuideEventListAdapter
                holder.listView!!.stopScroll()
                adapter.extend(extendingPreviousDay,additionalList.size)

                if(!extendingPreviousDay){
                    if(additionalList.size>1){
                        adapter.notifyItemRangeInserted(insertPosition+1,additionalList.size-1)
                    }
                }else{
                    adapter.notifyItemRangeInserted(insertPosition,additionalList.size)
                }
            }

        }
    }


    // Update adapter data
    fun update(adapterItems: MutableList<MutableList<TvEvent>>) {
        var position  = items.size
        this.items.addAll(adapterItems)
        notifyItemRangeInserted(position, items.size)
    }

    /**
     * Show focus on the event at the position inside the list with the passed index
     *
     * @param index list index
     * @param position event position inside the list
     */
    suspend fun showFocusOnPositionFrame(index: Int, position: Int, timelineStartTime: Date, timelineEndTime: Date,listener: HorizontalGuideSceneWidget.EpgEventFocus) {
        if (index < items.size) {
            channelIndex = index
            var pos = position
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "showFocusOnPositionFrame:position $position ")
            if ((holders[index]!!.listView!!.adapter as GuideEventListAdapter).isActiveIndexInsideFrame(
                    timelineStartTime,
                    timelineEndTime
                )
            ) {
                pos =
                    (holders[index]!!.listView!!.adapter as GuideEventListAdapter).getActiveTimelineIndex()
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "showFocusOnPositionFrame: true pos $pos")
            } else {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "showFocusOnPositionFrame: false pos $pos")
            }
            showFocusOnEventIndex(index,pos,listener)
        }
    }

    /**
     * To focus on event based on channel and event index
     */
    private fun showFocusOnEventIndex(channelIndex: Int, eventIndex: Int, listener: HorizontalGuideSceneWidget.EpgEventFocus){
        if (channelIndex < items.size && holders.containsKey(channelIndex) && holders[channelIndex]!!.listView!!.adapter != null) {
            if(!(holders[channelIndex]!!.listView!!.adapter as GuideEventListAdapter).showFocus(eventIndex,listener)){
                holders[channelIndex]!!.listView!!.getViewTreeObserver().addOnGlobalLayoutListener(object :OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if(holders[channelIndex]!=null){
                            holders[channelIndex]!!.listView!!.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                            (holders[channelIndex]!!.listView!!.adapter as GuideEventListAdapter).showFocus(eventIndex,listener)
                        }
                    }
                })
            }
        }
    }

    /**
     * Focuses on the target channel event based on the current focused event's timeframe.
     *
     * @param lastHorizontallyFocusedEvent Last focused event when scrolling towards horizontal
     * @param focusedEvent Currently focused event
     */
    fun showFocusOnEventFrame(
        channelIndex: Int,
        currentTime: Date,
        lastHorizontallyFocusedEvent: TvEvent?,
        focusedEvent: TvEvent?,
        timelineStartTime: Date,
        timelineEndTime: Date,
        listener: HorizontalGuideSceneWidget.EpgEventFocus
    ) {
        this.channelIndex = channelIndex

        val timelineStartTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(timelineStartTime.time)
        val timelineEndTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(timelineEndTime.time)
        val currentTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(currentTime.time)

        if (currentTimeInMinutes in timelineStartTimeInMinutes until timelineEndTimeInMinutes) {
            // Focus on current time if current time is visible
            showFocus(channelIndex, currentTime, listener)
        } else if (focusedEvent != null) {
            // Search focus based on previously focused event
            val currentFocusedEventStartTimeInMinutes =
                TimeUnit.MILLISECONDS.toMinutes(focusedEvent.startTime)
            val currentFocusedEventEndTimeInMinutes =
                TimeUnit.MILLISECONDS.toMinutes(focusedEvent.endTime)
            val focusSearchStartTimeInMinutes =
                TimeUnit.MILLISECONDS.toMinutes(lastHorizontallyFocusedEvent?.startTime ?: focusedEvent.startTime)
            val focusSearchEndTimeInMinutes =
                TimeUnit.MILLISECONDS.toMinutes(lastHorizontallyFocusedEvent?.endTime ?: focusedEvent.endTime)
            if (focusSearchStartTimeInMinutes <= timelineStartTimeInMinutes) {
                showFocus(channelIndex, timelineStartTime, listener)
            } else {
                val startTimeInMinutes = maxOf(
                    timelineStartTimeInMinutes,
                    focusSearchStartTimeInMinutes,
                    currentFocusedEventStartTimeInMinutes
                )
                val endTimeInMinutes = minOf(
                    timelineEndTimeInMinutes,
                    focusSearchEndTimeInMinutes,
                    currentFocusedEventEndTimeInMinutes,
                    startTimeInMinutes + 30 // To prevent bigger time window
                ) + 3 // To give weightage to right

                val minimumDurationForEdgeEvents = (endTimeInMinutes - startTimeInMinutes) / 2

                items[channelIndex].forEachIndexed { eventIndex, event ->
                    val eventStartTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(event.startTime)
                    val eventEndTimeInMinutes = TimeUnit.MILLISECONDS.toMinutes(event.endTime)

                    val isEventCoversTimeFrame = eventStartTimeInMinutes <= startTimeInMinutes
                            && eventEndTimeInMinutes >= endTimeInMinutes
                    val isEventInsideTimeFrame =
                        eventStartTimeInMinutes in startTimeInMinutes until endTimeInMinutes + 1
                                && eventEndTimeInMinutes in startTimeInMinutes until endTimeInMinutes + 1
                    val isPartiallyInsideLeft =
                        startTimeInMinutes in eventStartTimeInMinutes until eventEndTimeInMinutes + 1
                                && eventEndTimeInMinutes - startTimeInMinutes >= minimumDurationForEdgeEvents
                    val isPartiallyInsideRight =
                        endTimeInMinutes in eventStartTimeInMinutes until eventEndTimeInMinutes + 1
                                && eventStartTimeInMinutes - endTimeInMinutes >= minimumDurationForEdgeEvents

                    if (isEventCoversTimeFrame || isEventInsideTimeFrame || isPartiallyInsideLeft || isPartiallyInsideRight) {
                        showFocusOnEventIndex(channelIndex, eventIndex, listener)
                        return
                    }
                }

                // Trying to find middle event If unable to find target event
                showFocus(
                    channelIndex,
                    Date(TimeUnit.MINUTES.toMillis(startTimeInMinutes + minimumDurationForEdgeEvents)),
                    listener
                )
            }
        } else {
            showFocus(channelIndex, timelineStartTime, listener)
        }
    }

    /**
     * Show focus on the event between current time inside the list with the passed index
     *
     * @param index list index
     * @param currentTime time
     */
    fun showFocus(index: Int, currentTime: Date,listener: HorizontalGuideSceneWidget.EpgEventFocus) {
        channelIndex = index
        if (index < items.size &&
            holders.containsKey(index) &&
                holders[index]!!.listView!!.adapter != null) {
            val holder =   holders[index]!!
            if(!(holders[index]!!.listView!!.adapter as GuideEventListAdapter).showFocus(currentTime,listener)){
                holders[index]!!.listView!!.getViewTreeObserver().addOnGlobalLayoutListener(object :OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if(holders[index]!=null){
                            holder.listView!!.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                            (holders[index]!!.listView!!.adapter as GuideEventListAdapter).showFocus(currentTime,listener)
                        }
                    }
                })
            }
        }
    }

    /**
     * Get event list view for the position
     *
     * @param position event list position
     */
    fun getEventListView(position: Int): RecyclerView? {
        return holders[position]?.listView
    }

    /**
     * scroll all event lists to current time
     */
    fun scrollToTime(startDate: Date) {
        this.startDate = startDate
        holders.values.forEach { holder->
            scrollToTime(holder)
        }
    }

    /**
     * scroll all event lists inside the adapter to current time
     */
    fun scrollToTime(holder :GuideEventsContainerViewHolder) {
        if(startDate!=null){
            val eventListAdapter = holder.listView!!.adapter as GuideEventListAdapter
            val position = eventListAdapter.getPositionByTime(startDate!!.time)
            val startTime = maxOf(listener.getStartTime().time, (eventListAdapter.getEvent(position).startTime))
            val actualExtra =((startTime- startDate!!.time ) / 60000.0) * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE
            (holder.listView!!.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position,actualExtra.toInt())
            if(!eventListAdapter.updateTextPosition(startDate!!,false)){
                val index = holder.adapterPosition
                holder.listView!!.getViewTreeObserver().addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if(holders[index]!=null){
                            holder!!.listView!!.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                            val eventListAdapter = holders[index]!!.listView!!.adapter as GuideEventListAdapter
                            eventListAdapter.updateTextPosition(startDate!!,false)
                        }
                    }
                })
            }
        }
    }

    /**
     * smooth scroll all event lists inside the adapter to current time
     */
    fun smoothScrollToTime(startDate: Date) {
        this.startDate = startDate
        holders.values.forEach { holder ->
            if (holder.adapterPosition!=-1) {

                val eventListAdapter = holder.listView!!.adapter as GuideEventListAdapter
                val position = eventListAdapter.getPositionByTime(startDate.time)

                val smoothScroller: RecyclerView.SmoothScroller = object : LinearSmoothScroller(holder.listView!!.context) {
                    override fun calculateDtToFit(
                        viewStart: Int,
                        viewEnd: Int,
                        boxStart: Int,
                        boxEnd: Int,
                        snapPreference: Int
                    ): Int {

                        val startTime = maxOf(listener.getStartTime().time, (eventListAdapter.getEvent(position).startTime))
                        var actualExtra = ((startTime- startDate.time ) / 60000.0) * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE
                        if(isRTL) actualExtra = - (viewEnd - viewStart) -actualExtra+  133 * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE

                        return super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference) +actualExtra.toInt()
                    }

                    override fun getHorizontalSnapPreference(): Int {
                        return SNAP_TO_START
                    }

                    override fun calculateTimeForScrolling(dx: Int): Int {
                        return (super.calculateTimeForScrolling(dx)*2)
                    }

                    override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
                        return PointF( if(isRTL) 1f else -1f, 0f)
                    }
                }
                smoothScroller.targetPosition = position

                holder.listView!!.stopScroll()
                holder.listView!!.layoutManager!!.startSmoothScroll(smoothScroller)
                if(!eventListAdapter.updateTextPosition(startDate!!,true)){
                    holder.listView!!.post {
                        eventListAdapter.updateTextPosition(this@GuideEventsContainerAdapter.startDate!!,true)
                    }
                }
            }
        }
    }

    /**
     * Find previous focus inside the event list at the position
     *
     * @param position list position
     * @param startTime timeline onEndingShowAnimation time
     * @param reachedStart is timeline reached onEndingShowAnimation
     * @return status
     */
    fun findPreviousFocus(position: Int, startTime: Date, reachedStart: Boolean,listener: HorizontalGuideSceneWidget.EpgEventFocus) : Boolean{
        if (position < items.size) {
            holders[position]?.listView?.adapter?.let {
                var item: TvEvent? =    (it as GuideEventListAdapter).getSelectedEvent()
                var start = startTime
                if(!reachedStart) start = Date(startTime.time+ TimeUnit.MINUTES.toMillis(30))

                if (item != null &&  item.startTime > start.time) {
                    var itemStartDate = Date(item.startTime)
                    if (itemStartDate.timezoneOffset == start.timezoneOffset && itemStartDate.hours == start.hours && itemStartDate.minutes == start.minutes) {
                        return false
                    }
                    (holders[position]!!.listView!!.adapter as GuideEventListAdapter).selectPrevious(listener)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Find next focus inside the event list at the position
     *
     * @param position list position
     * @param endTime timeline end time
     * @param reachedEnd is timeline reached end
     */
    suspend fun findNextFocus(
        position: Int,
        endTime: Date,
        reachedEnd: Boolean,
        isForced: Boolean,
        listener: HorizontalGuideSceneWidget.EpgEventFocus
    ){
        try {
            if (position < items.size) {

                 holders[position]?.listView!!.adapter?.let {
                     var item: TvEvent? =  (it as GuideEventListAdapter).getSelectedEvent()

                     var end = endTime.time
                     if(!reachedEnd) end -= TimeUnit.MINUTES.toMillis(30)

                     if (item != null && item.endTime < end) {
                         (holders[position]!!.listView!!.adapter as GuideEventListAdapter).selectNext(endTime,listener,isForced)
                     }
                }

            } 
        }catch (e:Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "findNextFocus: ${e.message}")
        }
    }

    /**
     * This method checks if focus is on past event on time change,
     * if yes, then it shifts the focus on next event
     *
     * @param position list position
     * @param endTime timeline end time
     * @param reachedEnd is timeline reached end
     */
   suspend fun isPastEventFocused(
        currentTime: Long,
        position: Int,
        endTime: Date,
        reachedEnd: Boolean,
        isForced: Boolean,
        listener: HorizontalGuideSceneWidget.EpgEventFocus
    ){
        if (position < items.size) {
            holders[position]?.listView?.adapter?.let {
                val item: TvEvent? = (holders[position]!!.listView!!.adapter as GuideEventListAdapter).getSelectedEvent()
                item?.let {
                    var isPastEvent = TimeUnit.MILLISECONDS.toMinutes(item.endTime) <= TimeUnit.MILLISECONDS.toMinutes(currentTime)
                    if(isPastEvent){
                        var end = endTime.time
                        if(!reachedEnd) end -= TimeUnit.MINUTES.toMillis(30)
                        if (item.endTime < end) {
                            (holders[position]!!.listView!!.adapter as GuideEventListAdapter).selectNext(endTime,listener, isForced)
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear focus inside the list at the position
     *
     * @param position event list position
     */
    fun clearFocus(position: Int) {
        if (position >= 0 && position < items.size) {
           if(holders.containsKey(position) &&
                    holders[position]!!.listView!!.adapter != null) {
                (holders[position]!!.listView!!.adapter as GuideEventListAdapter).clearFocus()
            }
        }
    }

    /**
     * Get selected event inside the list at the position
     *
     * @param position event list position
     * @return tv event
     */
    fun getSelectedEvent(position: Int): TvEvent? {
        if (position < items.size) {
            if(holders.containsKey(position) &&
                holders[position]!!.listView!!.adapter != null) {
                return (holders[position]!!.listView!!.adapter as GuideEventListAdapter).getSelectedEvent()
            }
        }
        return null
    }

    /**
     * Save selected event inside the list at the position
     *
     * @param position event list position
     */
    fun staySelected(position: Int) {
        if (position < items.size) {
            if(holders.containsKey(position) &&
                holders[position]!!.listView!!.adapter != null) {
                (holders[position]!!.listView!!.adapter as GuideEventListAdapter).staySelected()
            }
        }
    }

    /**
     * Clear selected event inside the list at the position
     *
     * @param position event list position
     */
    fun clearSelected(position: Int) {
        if (position < items.size) {
            if(holders.containsKey(position) &&
                holders[position]!!.listView!!.adapter != null) {
                (holders[position]!!.listView!!.adapter as GuideEventListAdapter).clearSelected()
            }
        }
    }

    /**
     * Update event text position inside the all event lists
     *
     * @param startDate time line onEndingShowAnimation date
     */
    fun updateTextPosition(startDate: Date) {
        this.startDate = startDate
        holders.values.forEach { holder ->
            (holder.listView!!.adapter as GuideEventListAdapter).updateTextPosition(startDate,false)
        }
    }
    fun setStartDate(startDate: Date){
        this.startDate = startDate
    }

    /**
     * refresh indicators
     * @param position channel index
     */
    suspend fun refreshIndicators(position: Int,isOpened: Boolean,listener: HorizontalGuideSceneWidget.EpgEventFocus) {
        var index: Int?
        if(holders[position]!=null){
            val holder = holders[position]!!
            val adapter = holder.listView!!.adapter as GuideEventListAdapter
            if(adapter.getActiveEvent()==null ||(!isOpened && (adapter.getActiveEvent()!!.startTime/60000 - Date().time/60000) != 0L)){
                //avoid updating every minute, while we try to switch play icon to next event
                return
            }
            index = adapter.getSelectedIndex()
            notifyItemChanged(position)
            holders[position]?.listView?.doOnNextLayout {
                if (position == channelIndex) {
                    if (isOpened) {
                        (holders[position]?.listView?.adapter as GuideEventListAdapter).staySelected()
                    } else {
                        (holders[position]?.listView?.adapter as GuideEventListAdapter).showFocus(
                            index,
                            listener
                        )
                    }
                }
                (holders[position]?.listView?.adapter as GuideEventListAdapter).updateTextPosition(
                    startDate!!,
                    false
                )
            }
        }
    }
}