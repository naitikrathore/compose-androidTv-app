package com.iwedia.cltv.scene.home_scene.guide

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Guide event list adapter
 *
 * @author Dejan Nadj
 */
class GuideEventListAdapter(val listener : GuideEventListListener, private val dateTimeFormat: DateTimeFormat) : RecyclerView.Adapter<GuideEventListViewHolder>() {

    //Items
    private var items = mutableListOf<TvEvent>()

    //Selected view holder
    private var lastSelectedView: GuideEventListViewHolder? = null

    //Selected item index
    private var selectedIndex = 0
    var initialFocusPosition = -1

    //Holders map
    private var holders = mutableMapOf<Int, GuideEventListViewHolder>()

    //Last view for which the text position is updated
    private var lastUpdatedView: GuideEventListViewHolder? = null
    private var lastUpdatedEvent: TvEvent? = null
    private var activeChannel: TvChannel?=null

    //start date of first visible timeline
    private var channelIndex: Int = -1

    //RLT
    var isRTL : Boolean= false

    interface GuideEventListListener{
        fun getActiveChannel(callback : IAsyncDataCallback<TvChannel>)
        fun getStartTime(): Date
        fun getEndTime(): Date
        fun isInWatchList(event: TvEvent):Boolean
        fun isInRecList(event: TvEvent):Boolean
        suspend fun getCurrentTime(tvChannel: TvChannel): Long
        fun isEventLocked(tvEvent: TvEvent?): Boolean
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        isRTL  = (recyclerView.context as ReferenceApplication).activity!!.window.decorView.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideEventListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_event_list_item, parent, false)

        return GuideEventListViewHolder(view)
    }

    override fun onViewRecycled(holder: GuideEventListViewHolder) {
        holders.remove(holder.adapterPosition)
        super.onViewRecycled(holder)
    }
    override fun onBindViewHolder(holder: GuideEventListViewHolder, pos: Int) {
        val position = pos
        holders[position] = holder
        val item = items[position]
        holder.timeTextView!!.setDateTimeFormat(dateTimeFormat)
        listener.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {}
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(data: TvChannel) {
                CoroutineHelper.runCoroutineForSuspend({
                    activeChannel = data
                    var duration = items[position].endTime - items[position].startTime
                    duration/=60000

                    if (((ReferenceApplication.worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.DEFAULT.ordinal) &&
                        (position==getActiveTimelineIndex())) {
                        holder.currentlyPlayingIcon!!.visibility =if (activeChannel!!.id == item.tvChannel.id && duration>=10 && getActiveEvent() == item && !((ReferenceApplication.worldHandler) as ReferenceWorldHandler).isT56()) View.VISIBLE else  View.GONE
                        holder.recordIndicator!!.visibility = View.GONE
                        holder.watchlistIndicator!!.visibility = View.GONE
                    }else {
                        holder.currentlyPlayingIcon!!.visibility = View.GONE
                        holder.recordIndicator!!.visibility = if(listener.isInRecList(item) && duration>=10 ) View.VISIBLE else View.GONE
                        holder.watchlistIndicator!!.visibility = if(listener.isInWatchList(item) && duration>=10 ) View.VISIBLE else View.GONE
                    }
                },Dispatchers.Main)
            }
        })


        var layoutParams = holder.rootView?.layoutParams

        val startTime = maxOf(listener.getStartTime().time, item.startTime)
        val endTime = minOf(listener.getEndTime().time, item.endTime)

        val startTimeInMinutes = startTime/60000.0
        val endTimeInMinutes = endTime/60000.0

        val startOffset = startTimeInMinutes * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE
        var endOffset = endTimeInMinutes * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE


        var width = endOffset.toInt() - startOffset.toInt()

        layoutParams?.width = width
        if (width==0) width=1
        holder.rootView?.layoutParams = layoutParams
        holder.rootView?.invalidate()

        if (listener.isEventLocked(item)){
            holder.setParentalRestriction()
        }else{
            holder.nameTextView?.text = item.name
            holder.nameTextView!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        }

        holder.timeTextView?.time = item
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
                object : HorizontalGuideSceneWidget.EpgEventFocus {
                    override fun onEventFocus(
                        lastSelectedView: GuideEventListViewHolder,
                        isSmallDuration: Boolean
                    ) {
                        //we can ignore as it will be already open
                    }
                })
        }
        initialFocusPosition = -1

    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Show focus on the item inside the list
     * @param position item position
     */
    fun showFocus(position: Int,listener: HorizontalGuideSceneWidget.EpgEventFocus) : Boolean {
        //Clear previously selected item
        clearFocus()
        lastSelectedView?.nameTextView?.isSelected = false
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

            lastSelectedView!!.lockImageView.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))

            lastSelectedView!!.currentlyPlayingIcon!!.setColorFilter(
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

            CoroutineHelper.runCoroutine({
                lastSelectedView?.nameTextView?.ellipsize = TextUtils.TruncateAt.MARQUEE
                lastSelectedView?.nameTextView?.isSelected = true
                lastSelectedView?.nameTextView?.marqueeRepeatLimit = -1
                lastSelectedView?.nameTextView?.setSingleLine()
            },Dispatchers.Main)
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
    fun showFocus(time: Date,listener: HorizontalGuideSceneWidget.EpgEventFocus) : Boolean {
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
     * Get selected item index
     */
    fun setSelectedIndex(selectedIndex : Int){
        this.selectedIndex = selectedIndex
    }

    /**
     * Mark as selected current focused item inside the list
     */
    fun staySelected() {
        clearFocus()
        if (lastSelectedView != null) {
            lastSelectedView!!.separator?.visibility = View.INVISIBLE
            lastSelectedView!!.backgroundView!!.background =
                ConfigColorManager.generateGuideSelectedDrawable()
            lastSelectedView!!.currentlyPlayingIcon!!.clearColorFilter()
            lastSelectedView!!.lockImageView.clearColorFilter()
            lastSelectedView!!.recordIndicator!!.clearColorFilter()
            lastSelectedView!!.watchlistIndicator!!.clearColorFilter()

        }
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

            lastSelectedView!!.lockImageView.setColorFilter(
                Color.parseColor(
                    ConfigColorManager.getColor(
                        ConfigColorManager.getColor("color_background"),
                        1.0
                    )
                ))

            lastSelectedView!!.currentlyPlayingIcon!!.setColorFilter(
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
            lastSelectedView!!.nameTextView!!.isSelected=false
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
     * @param forced is used when we explicitly wants to focus on current time after scrolling
     */
    suspend fun selectNext(endDate: Date,listener: HorizontalGuideSceneWidget.EpgEventFocus, forced: Boolean) {
        val itemStartDate = Date(items[selectedIndex].startTime)
        val itemEndDate = Date(items[selectedIndex].endTime)
        val endOfTheDay = itemStartDate.before(endDate) && (itemEndDate == endDate || itemEndDate.after(endDate))
        if (items.size > 1 && (selectedIndex+1) < items.size && !endOfTheDay) {
            var nextIndex = 0
            if (forced) {
                while (items[nextIndex].endTime < this.listener.getCurrentTime(items[selectedIndex].tvChannel)) {
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        CoroutineHelper.cleanUp()
    }

    /**
     * Select previous item inside the list
     */
    fun selectPrevious(listener: HorizontalGuideSceneWidget.EpgEventFocus) : Boolean {
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
    suspend fun getActiveTimelineIndex() : Int {
        val currentTime =
            CoroutineScope(Dispatchers.IO).async { listener.getCurrentTime(items[0].tvChannel) }
                .await()
        items.forEach {
            if( it.startTime/60000 <= Date(currentTime).time/60000 &&  it.endTime/60000 > Date(currentTime).time/60000)
                return  items.indexOf(it)
        }
        return 0
    }

    //Refresh
   suspend fun getActiveEvent() : TvEvent? {
        val currentTime =
            CoroutineScope(Dispatchers.IO).async{ listener.getCurrentTime(items[0].tvChannel) }
                .await()
        items.forEach {
            if( it.startTime/60000 <= Date(currentTime).time/60000 &&  it.endTime/60000 > Date(currentTime).time/60000)
                return  it
        }
        return null
    }

    //Refresh
    suspend fun isActiveIndexInsideFrame(timelineStartTime: Date, timelineEndTime: Date): Boolean {
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

            lastUpdatedView!!.timeTextView!!.time = lastUpdatedEvent!!
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
            val holder :GuideEventListViewHolder?= holders[i]
            if(holder==null || holder.adapterPosition == -1) continue

            if ((item.startTime <= startDate.time && item.endTime > startDate.time)) {
                val startTime = maxOf(timelineStartTime.time, item.startTime)
                var diff = startDate.time - startTime
                if(isRTL) diff *= -1
                val width = item.endTime - startDate.time


                val charWidth = holder!!.nameTextView!!.paint.measureText("A")
                val charWidthTime = holder!!.timeTextView!!.paint.measureText("A")

                val numberOfChars =
                    ((((width / 60000.0) * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE) -
                            Utils.getDimensInPixelSize(R.dimen.custom_dim_15)) / charWidth).toInt()
                val numberOfCharsTime =
                    ((((width / 60000.0) * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE) -
                            Utils.getDimensInPixelSize(R.dimen.custom_dim_15)) / charWidthTime).toInt()

                holder.indicatorContainer!!.alpha = 1f

                val name = if (listener.isEventLocked(item)) {
                    ConfigStringsManager.getStringById("parental_control_restriction")
                } else {
                    item.name
                }

                holder.timeTextView!!.time = item
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
                    ((diff / 60000.0) * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE).toFloat()
                ).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    duration = 0
                    start()
                }

                ObjectAnimator.ofFloat(
                    holder!!.timeTextView!!,
                    "translationX",
                    ((diff / 60000.0) * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE).toFloat()
                ).apply {
                    interpolator = AccelerateDecelerateInterpolator()
                    duration = 0
                    start()
                }

                ObjectAnimator.ofFloat(
                    holder!!.indicatorContainer!!,
                    "translationX",
                    ((diff / 60000.0) * HorizontalGuideSceneWidget.GUIDE_TIMELINE_MINUTE).toFloat()
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
            val holders = mutableMapOf<Int, GuideEventListViewHolder>()
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

            val holders = mutableMapOf<Int, GuideEventListViewHolder>()
            this.holders.forEach { (key, value) ->
                val newIndex = key-1
                if(newIndex>=0)
                    holders[newIndex] = value
            }
            this.holders = holders
        }
    }

}