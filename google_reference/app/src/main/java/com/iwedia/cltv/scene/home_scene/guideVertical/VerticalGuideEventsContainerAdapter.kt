package com.iwedia.cltv.scene.home_scene.guideVertical

import android.graphics.PointF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvEvent
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Vertical guide events container adapter
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideEventsContainerAdapter(val dateTimeFormat: DateTimeFormat) : RecyclerView.Adapter<VerticalGuideEventsContainerViewHolder>() {

    //Items
    private var items = mutableListOf<MutableList<TvEvent>>()
    //View holders map
    private var holders = HashMap<Int, VerticalGuideEventsContainerViewHolder>()
    //Scroll start date
    private var startDate: Date?= null
    //separator showing status
    private var showSeparator = false;
    //current channel index
    private var channelIndex = -1;
    private var focusedChannelsList = HashMap<Int, Int>()
    //common viewPool for all events
    private val viewPool = RecyclerView.RecycledViewPool()
    private val epgMergeStatus = true//GeneralConfigManager.getGeneralSettingsInfo("epg_cell_merge")
    var listener: VerticalGuideSceneWidget.EpgEventFocus?=null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalGuideEventsContainerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_events_container_item_vertical, parent, false)

        val holder = VerticalGuideEventsContainerViewHolder(view)

        if (!epgMergeStatus) holder.listView?.setRecycledViewPool(viewPool)

        holder.listView?.itemAnimator = null
        holder.listView?.layoutManager = LinearLayoutManager(holder.listView!!.context,LinearLayoutManager.VERTICAL,false)

        return holder
    }

    override fun onBindViewHolder(holder: VerticalGuideEventsContainerViewHolder, position: Int) {

        if(holder.bindPosition != position){
            holders[position] = holder

            val adapter = VerticalGuideEventListAdapter(dateTimeFormat)
            holder.listView?.adapter = adapter
            adapter.setChannelIndex(position)
            if (epgMergeStatus && position < items.size - 1) {
                adapter.updateNextChannelList(items[position + 1])
            }
            adapter.refresh(items[position])
            startDate?.let { adapter.updateTextPosition(it, false,listener!!) }

            holder.bindPosition = position
        }else{
            holder.separatorView?.visibility = if(showSeparator && channelIndex == position) View.VISIBLE else View.GONE
        }


    }

    override fun onViewAttachedToWindow(holder: VerticalGuideEventsContainerViewHolder) {
        super.onViewAttachedToWindow(holder)
        scrollToTime(holder)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Show channel list separator view
     *
     * @param index item index
     * @param show show or hide separator view
     */
    fun showSeparator(index: Int, show: Boolean,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        showSeparator = show
        this.listener =listener
        notifyItemChanged(index)
    }

    //Refresh
    fun refresh(adapterItems: MutableList<MutableList<TvEvent>>,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        this.items.clear()
        this.items.addAll(adapterItems)
        this.listener = listener
        notifyItemRangeChanged(0, adapterItems.size)
    }

    //Pagination
    fun extendTimeline(
        eventContainerList: MutableList<MutableList<TvEvent>>,
        extendingPreviousDay: Boolean,listener: VerticalGuideSceneWidget.EpgEventFocus
    ) {
        eventContainerList.forEachIndexed { channelIndex, eventListTemp ->
            this.listener =listener
            val holder = holders[channelIndex]
            val isVisible = holder!=null && holder.adapterPosition!=-1
            val eventList = items[channelIndex]

            val insertPosition =  if(!extendingPreviousDay) eventList.size else 0

            if(isVisible){
                val adapter = holder!!.listView!!.adapter as VerticalGuideEventListAdapter
                if(!extendingPreviousDay) adapter.notifyItemChanged(insertPosition-1)
                if(extendingPreviousDay&& this.channelIndex == channelIndex) adapter.showFocus(
                    adapter.getSelectedIndex() + eventListTemp.size,listener
                )
                adapter.notifyItemRangeInserted(insertPosition,eventListTemp.size)
            }
            eventList.addAll(insertPosition,eventListTemp)
        }
    }

    fun updateItem(position: Int, item: MutableList<TvEvent>,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        items.set(position, item)
        this.listener = listener
        notifyItemChanged(position)
    }


    // Update adapter data
    fun update(adapterItems: MutableList<MutableList<TvEvent>>,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        val position  = items.size
        this.listener =listener
        this.items.addAll(adapterItems)
        notifyItemRangeInserted(position, items.size)
    }

    /**
     * Show focus on the event at the position inside the list with the passed index
     *
     * @param index list index
     * @param position event position inside the list
     */
    fun showFocusOnPosition(index: Int, position: Int,listener:VerticalGuideSceneWidget.EpgEventFocus) {
        channelIndex = index
        if (index < items.size) {
            getListAdapter(index)?.showFocus(position,listener)
            if (epgMergeStatus) showSameProgramsFocused(index, position,listener)
        }
    }

    /**
     * Show focus on the event between current time inside the list with the passed index
     *
     * @param index list index
     * @param currentTime time
     */
    fun showFocus(index: Int, currentTime: Date,listener:VerticalGuideSceneWidget.EpgEventFocus) {
        channelIndex = index
        var programPosition: Int? = null
        if (index < items.size &&
            holders.containsKey(index) && holders[index]?.listView?.adapter != null
        ) {
            if (epgMergeStatus) clearFocusedPrograms()
            programPosition = getListAdapter(index)?.showFocus(currentTime,listener)
        }
        if (epgMergeStatus) programPosition?.let { showSameProgramsFocused(index, it,listener) }
    }


    /**
     * Makes the Adjacent Channel Programs as focused without having focus
     * when the two channels have the same program data for the specified time
     */
    private fun showSameProgramsFocused(index: Int, programPosition: Int,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        if (index >= 1 && index < items.size - 1) {
            val providerFlag = items[index][programPosition].providerFlag ?: return

            for (i in index - 1 downTo 0) {
                val prevPosition = items[i].indexOfFirst { it.providerFlag == providerFlag }

                if (prevPosition >= 0) {
                    focusedChannelsList[i] = prevPosition
                    continue
                } else {
                    break
                }
            }

            for (i in index + 1 until items.size) {
                val nextPosition = items[i].indexOfFirst { it.providerFlag == providerFlag }

                if (nextPosition >= 0) {
                    focusedChannelsList[i] = nextPosition
                    continue
                } else {
                    break
                }
            }

            focusedChannelsList.keys.forEach { key ->
                focusedChannelsList[key]?.let {
                    getListAdapter(key)?.showFocus(it,listener)
                }
            }
        }
    }

    /**
     * Clears the Dummy Focused Background of the Programs
     */
    private fun clearFocusedPrograms() {
        if (focusedChannelsList.isNotEmpty()) {
            focusedChannelsList.keys.forEach { key ->
                getListAdapter(key)?.clearFocus()
            }
            focusedChannelsList.clear()
        }
    }

    /**
     * Move the Dummy Focus to the views vertically when scrolled vertical
     * by maintaining focus on the Program having same content with adjacent channel
     */

    private fun showVerticalFocusedPrograms(index: Int, isPrevious: Boolean,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        if (focusedChannelsList.isNotEmpty()) {
            focusedChannelsList.keys.forEach { key ->
                focusedChannelsList[key]?.let {
                    val adapter = getListAdapter(key)
                    adapter?.clearFocus()

                    val position = if (isPrevious) it - 1 else it + 1
                    focusedChannelsList[key] = position

                    adapter?.showFocusedProgram(position,listener)
                }
            }
        } else {
            val adapter = getListAdapter(index)
            val item = adapter?.getSelectedEvent()
            if (item?.isProgramSame == true) {
                showSameProgramsFocused(index, adapter.getSelectedIndex(),listener)
            }
        }
    }

    private fun getListAdapter(position: Int) : VerticalGuideEventListAdapter? =
        holders[position]?.listView?.adapter?.let {
            (it as VerticalGuideEventListAdapter)
        }

    /**
     * Get event list view for the position        val index = (holders[position]!!.listView!!.adapter as GuideEventListAdapter).getSelectedIndex()

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
    fun scrollToTime(holder :VerticalGuideEventsContainerViewHolder) {
        if (holder.adapterPosition != -1 && startDate != null) {
            val eventListAdapter = holder.listView!!.adapter as VerticalGuideEventListAdapter
            holder.listView?.stopScroll()
            val position = eventListAdapter.getPositionByTime(startDate!!.time)
            val actualExtra =((eventListAdapter.getEvent(position).startTime - startDate!!.time ) / 60000.0) * VerticalGuideSceneWidget.GUIDE_TIMELINE_MINUTE
            (holder.listView?.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position,actualExtra.toInt())
        }
    }

    /**
     * smooth scroll all event lists inside the adapter to current time
     */
    fun smoothScrollToTime(startDate: Date) {
        this.startDate = startDate
        holders.values.forEach { holder ->
            if (holder.adapterPosition!=-1) {

                val eventListAdapter = holder.listView!!.adapter as VerticalGuideEventListAdapter
                val position = eventListAdapter.getPositionByTime(startDate.time)

                val actualExtra =
                    ((eventListAdapter.getEvent(position).startTime - startDate.time ) / 60000.0) * VerticalGuideSceneWidget.GUIDE_TIMELINE_MINUTE

                val smoothScroller: RecyclerView.SmoothScroller = object : LinearSmoothScroller(holder.listView!!.context) {
                    override fun calculateDtToFit(
                        viewStart: Int,
                        viewEnd: Int,
                        boxStart: Int,
                        boxEnd: Int,
                        snapPreference: Int
                    ): Int {
                        return super.calculateDtToFit(viewStart, viewEnd, boxStart, boxEnd, snapPreference)+actualExtra.toInt()
                    }
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }

                    override fun calculateTimeForScrolling(dx: Int): Int {
                        return (super.calculateTimeForScrolling(dx)*2.5).toInt()
                    }

                    override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
                        return PointF(0f, -1f)
                    }
                }
                smoothScroller.targetPosition = position

                holder.listView!!.layoutManager!!.startSmoothScroll(smoothScroller)
            }
        }
    }

    fun notifyTimeLineDataChanged(listener: VerticalGuideSceneWidget.EpgEventFocus) {
        holders.values.forEach { holder ->
            if (holder.adapterPosition!=-1) {
                val eventListAdapter = holder.listView!!.adapter as VerticalGuideEventListAdapter
                this.listener = listener
                eventListAdapter.notifyDataSetChanged()
            }
        }
    }

    /**
     * Find previous focus inside the event list at the position
     *
     * @param position list position
     * @param startTime timeline start time
     * @param reachedStart is timeline reached start
     * @return status
     */
    fun findPreviousFocus(position: Int, startTime: Date, reachedStart: Boolean,listener: VerticalGuideSceneWidget.EpgEventFocus): Boolean {

        if (position < items.size) {

            var start = startTime
            if(!reachedStart) start = Date(startTime.time+TimeUnit.MINUTES.toMillis(15))

            val item: TvEvent? = getListAdapter(position)?.getSelectedEvent()
            if (item != null && item.startTime > start.time) {
                val itemStartDate = Date(item.startTime)
                if (itemStartDate.hours == start.hours && itemStartDate.minutes == start.minutes) {
                    return false
                }
                getListAdapter(position)?.selectPrevious(listener)
                if (epgMergeStatus) showVerticalFocusedPrograms(position, isPrevious = true,listener)
                return true
            }
        }
        return false
    }

    /**        val index = (holders[position]!!.listView!!.adapter as GuideEventListAdapter).getSelectedIndex()

     * Find next focus inside the event list at the position
     *
     * @param position list position
     * @param endTime timeline end time
     * @param reachedEnd is timeline reached end
     */
    fun findNextFocus(position: Int, endTime: Date, reachedEnd: Boolean,listener: VerticalGuideSceneWidget.EpgEventFocus) {

        if (position < items.size) {

            var end = endTime.time
            if(!reachedEnd) end -= TimeUnit.MINUTES.toMillis(15)

            val item = getListAdapter(position)?.getSelectedEvent()
            if (item != null && item.endTime < end) {
                getListAdapter(position)?.selectNext(endTime,listener)
                if (epgMergeStatus) showVerticalFocusedPrograms(position, isPrevious = false,listener)
            }
        }
    }

    /**
     * Clear focus inside the list at the position
     *
     * @param position event list position
     */
    fun clearFocus(position: Int) {
        if (position < items.size) {
            if (epgMergeStatus) clearFocusedPrograms()
            getListAdapter(position)?.clearFocus()
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
            return getListAdapter(position)?.getSelectedEvent()
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
            getListAdapter(position)?.staySelected()
        }
    }

    /**
     * Clear selected event inside the list at the position
     *
     * @param position event list position
     */
    fun clearSelected(position: Int) {
        if (position < items.size) {
            getListAdapter(position)?.clearSelected()
        }
    }

    /**
     * Update event text position inside the all event lists
     *
     * @param startDate time line start date
     */
    fun updateTextPosition(startDate: Date,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        this.startDate = startDate
        holders.values.forEach { holder ->
            (holder.listView?.adapter as VerticalGuideEventListAdapter).updateTextPosition(startDate, true,listener)
        }
    }

    fun getAllItems() = items

    /**
     * refresh indicators
     * @param position channel index
     */
    fun refreshIndicators(position: Int,listener: VerticalGuideSceneWidget.EpgEventFocus) {
        val adapter = VerticalGuideEventListAdapter(dateTimeFormat)
        adapter.refreshIndicators(position,listener)
    }
}