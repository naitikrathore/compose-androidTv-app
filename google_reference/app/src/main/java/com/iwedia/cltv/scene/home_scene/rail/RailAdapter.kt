package com.iwedia.cltv.scene.home_scene.rail

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.components.DiffUtilCallback
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.foryou.RailItem.RailItemType.*
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import tv.anoki.ondemand.domain.model.VODItem
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * RailsAdapter is used as adapter which will handle multiple rails in Search Scene or ForYou Widget.
 *
 * In SearchScene this adapter can contain Channels Rail, Events Rail and Recording Rail.
 *
 * In ForYouWidget this adapter can contain Events Rail and Recording Rail.
 *
 * @author Boris Tirkajla
 */

class RailAdapter(private val dateTimeFormat: DateTimeFormat) :
    RecyclerView.Adapter<RailsViewHolder>() {

    val onNowTitleColor = "#55d3ed"
    val upNextTitleColor = "#c524ff"
    val broadcastTvChannelsTitleColor = "#55d3ed"
    val watchlistTitleColor = "#00b57c"
    val recordedTitleColor = "#8f7eff"
    val scheduledRecordingTitleColor = "#99ae59"
    val forYouTitleColor = "#f9a230"

    //view holders hash map
    var holders: HashMap<Int, RailsViewHolder> = HashMap()

    //listener
    var adapterListener: AdapterListener? = null

    var items: ArrayList<RailItem> = ArrayList()

    var selectedItemPosition: Int = -1

    var clickedItemPosition: Int = -1

    /**
     * this flag is used to disable default details showing on event selection
     */
    var isRailDetailsEnabled: Boolean = true

    //adapter
    private var railItemAdapter: RailItemAdapter? = null

    private var sortedEvents: MutableList<TvEvent> =
        mutableListOf() // this is used both in ForYou and Search Scene - both can contain Rail with Events (with On Now and Future events)

    private var sortedRecordings: MutableList<Recording> =
        mutableListOf() // this is used both in ForYou and Search Scene - both can contain Rail with Recordings

    private var sortedScheduledRecording: MutableList<ScheduledRecording> =
        mutableListOf() // this is used in Recordings

    private var sortedVodItems: MutableList<VODItem> =
        mutableListOf() // this is used in Recordings

    private var broadcastChannelsItems: MutableList<TvChannel> =
        mutableListOf() // this is used in Recordings

    private var detailsRailTimer: Timer? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RailsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.rail_item, parent, false)
        return RailsViewHolder(view)
    }

    override fun onViewRecycled(holder: RailsViewHolder) {
        holder.eventRecycler!!.selectedPosition = 0
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(
        holder: RailsViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        val start = System.currentTimeMillis()
        val item = items[position]
        holders[position] = holder

//---------------------------------------- ITEM IS CHANNEL -----------------------------------------
//--------------------------------------------------------------------------------------------------
        when (item.type) {
            EVENT -> {
                val events = item.rail as MutableList<TvEvent>

                if (events.isEmpty()) return

                val selectedPosition = holder.eventRecycler!!.selectedPosition
                railItemAdapter =
                    RailItemAdapter(
                        railItemAdapterType = RailItemAdapter.RailItemAdapterType.EVENT,
                        dateTimeFormat = dateTimeFormat,
                        ttsSetterInterface = adapterListener!!
                    )
                holder.eventRecycler!!.adapter = railItemAdapter
                holder.eventRecycler!!.selectedPosition = selectedPosition

                //Fast events should be sorted by its channel ordinal number
                sortedEvents = if (events[0].tvChannel.isFastChannel())
                    events?.sortedWith(compareBy { it.tvChannel.ordinalNumber })!!.toMutableList()
                else events?.sortedWith(compareBy { it.startTime })!!.toMutableList()

                item.rail!!.clear()
                item.rail!!.addAll(sortedEvents)
                railItemAdapter!!.refresh(sortedEvents.toMutableList()) // this will refresh onNow and FutureEvents - this adapter is only for events, not for channels.
            }

            CHANNEL -> {
                val events = item.rail as MutableList<TvEvent>

                if (events.isEmpty()) return // if there is no event DON'T initialise adapter - it's not needed, but events shouldn't be empty

                //Fast channels should be sorted by ordinal number
                sortedEvents = if (events[0].tvChannel.isFastChannel())
                    events.sortedWith(compareBy { it.tvChannel.ordinalNumber }).toMutableList()
                else events
                    .sortedWith(compareBy { it.tvChannel.index })
                    .toMutableList()

                item.rail!!.clear()
                item.rail!!.addAll(sortedEvents)

                val selectedPosition = holder.eventRecycler!!.selectedPosition
                railItemAdapter =
                    RailItemAdapter(
                        railItemAdapterType = RailItemAdapter.RailItemAdapterType.CHANNEL_SEARCH_SCENE,
                        dateTimeFormat = dateTimeFormat,
                        ttsSetterInterface = adapterListener!!
                    )
                holder.eventRecycler!!.adapter = railItemAdapter
                holder.eventRecycler!!.selectedPosition = selectedPosition

                railItemAdapter!!.refresh(sortedEvents.toMutableList())
            }

            RECORDING -> {
                val recordings = item.rail as MutableList<Recording>

                if (recordings.isEmpty()) return

                val selectedPosition = holder.eventRecycler!!.selectedPosition
                railItemAdapter =
                    RailItemAdapter(
                        railItemAdapterType = RailItemAdapter.RailItemAdapterType.RECORDING,
                        dateTimeFormat = dateTimeFormat,
                        ttsSetterInterface = adapterListener!!
                    )
                holder.eventRecycler!!.adapter = railItemAdapter
                holder.eventRecycler!!.selectedPosition = selectedPosition

                sortedRecordings =
                    recordings.sortedWith(compareBy { it.recordedEvent?.startTime.toString() })
                        .toMutableList()
                item.rail!!.clear()
                item.rail!!.addAll(sortedRecordings)
                railItemAdapter!!.refresh(sortedRecordings.toMutableList())
            }

            SCHEDULED_RECORDING -> {
                val scheduledRecordings = item.rail as MutableList<ScheduledRecording>

                if (scheduledRecordings.isEmpty()) return

                val selectedPosition = holder.eventRecycler!!.selectedPosition
                railItemAdapter =
                    RailItemAdapter(
                        railItemAdapterType = RailItemAdapter.RailItemAdapterType.SCHEDULED_RECORDING,
                        dateTimeFormat = dateTimeFormat,
                        ttsSetterInterface = adapterListener!!
                    )
                holder.eventRecycler!!.adapter = railItemAdapter
                holder.eventRecycler!!.selectedPosition = selectedPosition

                sortedScheduledRecording =
                    scheduledRecordings.sortedWith(compareBy { it.scheduledDateStart.toString() })
                        .toMutableList()
                item.rail!!.clear()
                item.rail!!.addAll(sortedScheduledRecording)
                railItemAdapter!!.refresh(sortedScheduledRecording.toMutableList())
            }

            VOD -> {
                if (item.rail.isNullOrEmpty()) return
                val vodItems = item.rail as MutableList<VODItem>
                val selectedPosition = holder.eventRecycler!!.selectedPosition
                railItemAdapter =
                    RailItemAdapter(
                        railItemAdapterType = RailItemAdapter.RailItemAdapterType.VOD,
                        dateTimeFormat = dateTimeFormat,
                        ttsSetterInterface = adapterListener!!
                    )
                holder.eventRecycler!!.adapter = railItemAdapter
                holder.eventRecycler!!.selectedPosition = selectedPosition

                sortedVodItems =
                    vodItems.sortedWith(compareBy { it.title })
                        .toMutableList()

                item.rail!!.clear()
                item.rail!!.addAll(sortedVodItems)
                railItemAdapter!!.refresh(sortedVodItems.toMutableList())
            }

            BROADCAST_CHANNEL -> {
                broadcastChannelsItems = item.rail as MutableList<TvChannel>

                if (broadcastChannelsItems.isEmpty()) return // if there is no event DON'T initialise adapter - it's not needed, but events shouldn't be empty

                val selectedPosition = holder.eventRecycler!!.selectedPosition
                railItemAdapter =
                    RailItemAdapter(
                        railItemAdapterType = RailItemAdapter.RailItemAdapterType.BROADCAST_CHANNEL,
                        dateTimeFormat = dateTimeFormat,
                        ttsSetterInterface = adapterListener!!
                    )
                holder.eventRecycler!!.adapter = railItemAdapter
                holder.eventRecycler!!.selectedPosition = selectedPosition

                railItemAdapter!!.refresh(broadcastChannelsItems.toMutableList())
            }
        }

//---------------------------- code common no matter which type is item ----------------------------
//--------------------------------------------------------------------------------------------------
        holder.railLabelTextView!!.text = item.railName
        if (!adapterListener!!.isRegionSupported()) {
            holder.railLabelTextView!!.setTextColor(
                Color.parseColor(
                    getRailTextLabelColor(
                        item.railName,
                        ConfigColorManager.getColor("color_text_description")
                    )
                )
            )
        }

        // TODO BORIS here should go another else if for Recording #1 - do not delete this comment until this is implemented

        initialiseRailItemAdapter(
            position = position,
            item = item
        ) // listener is the same for all types of items. If needed (in future) to have some specific logic for some of item.type, move this in previous if clause and create specific logic.


        // focus first element in the first rail
        if (position == 0) { // if rail is the first focus it's first item
            if (sortedEvents.isNotEmpty()) {
                val tvEvent = sortedEvents[0]
                onEventSelected(
                    holder = holder,
                    tvEvent = tvEvent,
                    railItem = item,
                    isStillFocused = { true }
                )
            } else if (sortedRecordings.isNotEmpty()) {
                val tvEvent = sortedRecordings[0].tvEvent ?: TvEvent.createNoInformationEvent(
                    sortedRecordings[0].tvChannel!!,
                    adapterListener!!.getCurrentTime(sortedRecordings[0].tvChannel!!)
                )
                onEventSelected(
                    holder = holder,
                    tvEvent = tvEvent,
                    railItem = item,
                    isStillFocused = { true }
                )
            } else if (sortedScheduledRecording.isNotEmpty()) {
                val tvEvent =
                    sortedScheduledRecording[0].tvEvent ?: TvEvent.createNoInformationEvent(
                        sortedRecordings[0].tvChannel!!,
                        adapterListener!!.getCurrentTime(sortedRecordings[0].tvChannel!!)
                    )
                onEventSelected(
                    holder = holder,
                    tvEvent = tvEvent,
                    railItem = item,
                    isStillFocused = { true }
                )
            } else if (broadcastChannelsItems.isNotEmpty()) {
                val tvChannel = broadcastChannelsItems[0]
                adapterListener!!.onBroadcastChannelSelected(tvChannel, item)
            }
        }
    }

    private fun initialiseRailItemAdapter(position: Int, item: RailItem) {
        railItemAdapter?.setListener(object : RailItemAdapter.AdapterListener {
            override fun onEventSelected(
                tvEvent: TvEvent,
                eventPosition: Int,
                isStillFocused: () -> Boolean
            ) {
                onEventSelected(
                    holder = holders[position]!!,
                    tvEvent = tvEvent,
                    railItem = item,
                    isStillFocused = isStillFocused
                )
                selectedItemPosition =
                    position // important for Recording or Scheduled Recording when changing name or deleting them from list in order to be able to refresh adapter.
                clickedItemPosition = eventPosition
            }

            override fun onBroadcastChannelSelected(tvChannel: TvChannel) {
                adapterListener?.onBroadcastChannelSelected(tvChannel, item)
            }

            override fun onVodItemSelected(
                vodItem: VODItem,
                eventPosition: Int,
                isStillFocused: () -> Boolean
            ) {
                onEventSelected(
                    holder = holders[position]!!,
                    vodItem = vodItem,
                    railItem = item,
                    isStillFocused = isStillFocused
                )
                selectedItemPosition =
                    position // important for Recording or Scheduled Recording when changing name or deleting them from list in order to be able to refresh adapter.
                clickedItemPosition = eventPosition
            }

            override fun getAdapterPosition(position: Int) {
            }

            override fun onKeyDown(): Boolean {
                if (position != holders.size - 1) {
                    holders[position]!!.collapseDetailsContainer(
                        getRailTextLabelColor(
                            items.get(
                                position
                            ).railName, ConfigColorManager.getColor("color_text_description")
                        )
                    )
                    adapterListener?.onScrollDown(position + 1)
                    return true
                }
                return false
            }

            override fun onKeyUp(): Boolean {
                if (position < items.size) {
                    val isFirstRail = position == 0
                    if (isFirstRail) { // if rail is the first one
                        adapterListener!!.onKeyUp {
                            holders[position]?.collapseDetailsContainer(
                                getRailTextLabelColor(
                                    items.get(
                                        position
                                    ).railName, ConfigColorManager.getColor("color_text_description")
                                )
                            )
                        }
                    } else {
                        holders[position]?.collapseDetailsContainer(
                            getRailTextLabelColor(
                                items.get(
                                    position
                                ).railName, ConfigColorManager.getColor("color_text_description")
                            )
                        )
                        adapterListener?.onScrollUp(position - 1)
                    }
                }
                return true
            }

            override fun onBackPressed() {
                holders[position]!!.collapseDetailsContainer(
                    getRailTextLabelColor(
                        items.get(
                            position
                        ).railName, ConfigColorManager.getColor("color_text_description")
                    )
                )
            }

            override fun onItemClicked(item: Any, position: Int) {
                adapterListener!!.onItemClicked(item)
            }

            override fun isChannelLocked(channelId: Int): Boolean {
                return adapterListener!!.isChannelLocked(channelId)
            }

            override fun isParentalControlsEnabled(): Boolean {
                return adapterListener!!.isParentalControlsEnabled()
            }

            /**
             * Callback function triggered when a card animation is completed.
             * Depending on the application state, this function either expands the details container
             * and animates the text view scale or only animates the text view scale.
             * If `enableRailDetails` is true, it expands the details container with text animation. Otherwise, it animates the text view to a larger size.
             */
            override fun onCardAnimated() {
                if (position < holders.size) {
                    holders[position]!!.expandDetailsContainer(
                        isRailDetailsEnabled,
                        getRailTextLabelColor(
                            items.get(position).railName,
                            ConfigColorManager.getColor("color_main_text")
                        )
                    )
                }
            }

            override fun getCurrentTime(tvChannel: TvChannel): Long {
                return adapterListener!!.getCurrentTime(tvChannel)
            }

            override fun isInWatchlist(tvEvent: TvEvent): Boolean {
                return adapterListener!!.isInWatchlist(tvEvent)
            }

            override fun isEventLocked(tvEvent: TvEvent) = adapterListener!!.isEventLocked(tvEvent)

            override fun isInRecList(tvEvent: TvEvent): Boolean {
                return adapterListener!!.isInRecList(tvEvent)
            }
        })
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun onEventSelected(
        holder: RailsViewHolder,
        tvEvent: TvEvent? = null,
        vodItem: VODItem? = null,
        railItem: RailItem,
        isStillFocused: () -> Boolean
    ) {
        if (isRailDetailsEnabled) {
            if (detailsRailTimer != null) {
                detailsRailTimer!!.cancel()
                detailsRailTimer = null
            }

            detailsRailTimer = Timer("timer")

            //Timer to update custom details only when user focus the event for sometime in For you scene.
            //Changes made for fast scrolling use case to make the right/left navigation faster.
            detailsRailTimer!!.schedule(object : TimerTask() {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun run() {
                    ReferenceApplication.runOnUiThread(Runnable {
                        // adapterListener becoming null need to check
                        adapterListener?.let {

                            tvEvent?.let { tvEvent ->
                                holder.customDetails.updateData(
                                    tvEvent = tvEvent,
                                    parentalRatingDisplayName = it.getParentalRatingDisplayName(
                                        tvEvent.parentalRating, tvEvent
                                    ),
                                    railItem = railItem,
                                    it.getCurrentTime(tvEvent.tvChannel),
                                    dateTimeFormat = dateTimeFormat,
                                    isEventLocked = adapterListener!!.isEventLocked(tvEvent)
                                )
                            }

                            vodItem?.let {
                                holder.customDetails.updateData(
                                    title = vodItem.title,
                                    description = vodItem.description,
                                    isLocked = false
                                )
                            }
                        }
                    })
                }
            }, 600)
        } else {
            tvEvent?.let {
                adapterListener!!.onEventSelected(
                    tvEvent = tvEvent,
                    parentalRatingDisplayName = adapterListener!!.getParentalRatingDisplayName(
                        it.parentalRating,
                        it
                    ),
                    railItem = railItem,
                    currentTime = adapterListener!!.getCurrentTime(it.tvChannel),
                    isStillFocused = isStillFocused
                )
            }

            vodItem?.let {
                adapterListener!!.onEventSelected(
                    vodItem = it,
                    parentalRatingDisplayName = "", // TODO add parental for Vod content.
                    railItem = railItem,
                    currentTime = System.currentTimeMillis(),
                    isStillFocused = isStillFocused
                )
            }
        }

        //TODO this code below is for recording, it is copied and it should be implemented and tested. It is for ProgressBar when item is Recording, from documentation:
        // If recorded item has already been watched, progress bar shall be shown corresponding to the percentage of watched content.
        // it is not implemented and tested yet because there are no real Recorded items which can be used for testing.
        /*var data = System.currentTimeMillis()
        var startTime =  if (item is Recording) item.recordingDate
        else if (item is ReferenceRecording) item.recordingDate
        else if (item is ScheduledRecording) item.scheduledDateStart
        else if (item is ReferenceScheduledRecording) item.scheduledDateStart
        else if (item is TvEvent) item.startTime
        else (item as ReferenceTvEvent).startDate
        var endTime =  if (item is Recording) item.recordingEndTime
        else if (item is ScheduledRecording) item.scheduledDateEnd
        else if (item is ReferenceTvEvent) item.endDate
        else if (item is ReferenceRecording) item.recordingEndTime
        else if (item is ReferenceScheduledRecording) item.scheduledDateEnd
        else (item as TvEvent).endTime

        var currentTime = if (item is Recording) {
            var watchedPosition = (ReferenceSdk.pvrHandler as ReferencePVRHandler).getPlaybackPosition(item.id)
            GLong((startTime.value.toLong() + watchedPosition).toString())
        } else data
        if (!railItem.railName.equals(ConfigStringsManager.getStringById("past_events"))) {

            if (item is Recording) {

            }
            var p = Recording.calculateCurrentProgress(
                currentTime,
                startTime!!,
                endTime!!)

            holder.eventProgress!!.visibility = View.VISIBLE
            holder.eventProgress!!.progress = p
        }

        holder.eventTime!!.text = Recording.createRecordingTimeInfo(startTime!!,
            endTime!!)
        holder.eventTime!!.visibility = View.VISIBLE*/
    }

    /**
     * Method specific only for Rail that contains Recordings. Usage is to rename clicked Recording.
     */
    fun onRecordingRenamed(name: String) {
        if (this.items.size > 0) {
            if (this.items[selectedItemPosition].rail?.size!! > clickedItemPosition) {
                val rec =
                    this.items[selectedItemPosition].rail?.get(clickedItemPosition) as Recording
                rec.let { recording ->
                    val newRecording = Recording(
                        recording.id,
                        name,
                        recording.duration,
                        recording.recordingDate,
                        recording.image,
                        recording.videoUrl,
                        recording.tvChannel,
                        recording.tvEvent,
                        recording.recordingStartTime,
                        recording.recordingEndTime,
                        recording.shortDescription,
                        recording.longDescription,
                        recording.contentRating
                    )
                    this.items[selectedItemPosition].rail?.set(clickedItemPosition, newRecording)
                    (this.holders[selectedItemPosition]!!.eventRecycler!!.adapter!! as RailItemAdapter).updateItemAtPosition(
                        newRecording,
                        clickedItemPosition
                    )
                }
            }
        }
    }

    fun onRecordingDeleted(callback: IAsyncCallback) {
        if (clickedItemPosition > -1 && this.items.isNotEmpty() && this.items.size > selectedItemPosition && this.items[selectedItemPosition].rail!!.size > clickedItemPosition) {
            Log.d(
                Constants.LogTag.CLTV_TAG +
                "RailAdapter",
                "onRecordingDeleted ${this.items[selectedItemPosition].rail!!.size}"
            )
            this.items[selectedItemPosition].rail!!.removeAt(clickedItemPosition)
            Log.d(Constants.LogTag.CLTV_TAG +
                "RailAdapter",
                "onRecordingDeleted ${this.items[selectedItemPosition].rail!!.size}"
            )
            (this.holders[selectedItemPosition]!!.eventRecycler!!.adapter!! as RailItemAdapter).refresh(
                this.items[selectedItemPosition].rail!!
            )
            callback.onSuccess()
        }
    }

    private fun getEventRecyclerSize() =
        this.holders[selectedItemPosition]!!.eventRecycler?.adapter?.itemCount ?: 0

    private fun focusNextRecordedItem(
        size: Int,
        focusItem: Int
    ) {
        if (size > 0) {
            this.holders[selectedItemPosition]!!.eventRecycler!!.postDelayed({
                this.holders[selectedItemPosition]!!.eventRecycler!!.layoutManager!!.findViewByPosition(
                    focusItem
                )!!.requestFocus()
            }, 100)
        }
    }

    private fun calculateNextFocusedItem(size: Int, focusItem: Int): Int {
        var focusItemTemp = focusItem
        when (clickedItemPosition) {
            0 -> {
                // Do nothing and focus first item
            }

            size - 1 -> {
                // Its last item
                focusItemTemp -= 1
            }

            else -> {
                focusItemTemp -= 1
            }
        }
        return focusItemTemp
    }


    fun refresh(adapterItems: ArrayList<RailItem>) {

        val diffCallback = DiffUtilCallback(items, adapterItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.items.clear()
        this.items.addAll(adapterItems)

        diffResult.dispatchUpdatesTo(this)
    }

    fun update(adapterItems: ArrayList<RailItem>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyItemRangeChanged(0,  adapterItems.size)
    }

    fun setListener(adapterListener: AdapterListener) {
        this.adapterListener = adapterListener
    }

    fun dispose() {
        railItemAdapter?.dispose()
        adapterListener = null
        railItemAdapter = null

        detailsRailTimer?.cancel()
        detailsRailTimer = null

        if (items.isNotEmpty()) {
            items.clear()
        }
        if (sortedEvents.isNotEmpty()) {
            sortedEvents.clear()
        }
        if (sortedRecordings.isNotEmpty()) {
            sortedRecordings.clear()
        }
        if (sortedScheduledRecording.isNotEmpty()) {
            sortedScheduledRecording.clear()
        }
        if (sortedVodItems.isNotEmpty()) {
            sortedVodItems.clear()
        }
    }

    private fun getRailTextLabelColor(railName: String, defaultColor: String): String {
        if (!adapterListener!!.isRegionSupported()) {
            if (railName == ConfigStringsManager.getStringById("on_now")) {
                return onNowTitleColor
            } else if (railName == ConfigStringsManager.getStringById("up_next")) {
                return upNextTitleColor
            } else if (railName == ConfigStringsManager.getStringById("watchlist")) {
                return watchlistTitleColor
            } else if (railName == ConfigStringsManager.getStringById("recorded")) {
                return recordedTitleColor
            } else if (railName == ConfigStringsManager.getStringById("scheduled")) {
                return scheduledRecordingTitleColor
            } else if (railName == ConfigStringsManager.getStringById("for_you")) {
                return forYouTitleColor
            } else if (railName == ConfigStringsManager.getStringById("broadcast_tv_channels")) {
                return defaultColor
            } else {
                return defaultColor
            }
        } else {
            return defaultColor
        }
    }
}

interface AdapterListener : TTSSetterInterface {
    fun onScrollDown(position: Int)
    fun onScrollUp(position: Int)

    /**
     * Handles the key up event and executing the provided [onKeyUpFinished] lambda, which is crucial for collapsing the card.
     *
     * @param onKeyUpFinished A lambda function to be executed after the key up event is processed.
     */
    fun onKeyUp(onKeyUpFinished: () -> Unit) // called if DPAD_UP is called from the first rail
    fun onItemClicked(item: Any) // called when card (item/event) is clicked
    fun isChannelLocked(channelId: Int): Boolean
    fun isParentalControlsEnabled(): Boolean // returns true if Parental Control is enabled, otherwise false.
    fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String
    fun getCurrentTime(tvChannel: TvChannel): Long
    fun isInWatchlist(tvEvent: TvEvent): Boolean
    fun isInRecList(tvEvent: TvEvent): Boolean
    fun isRegionSupported(): Boolean

    fun onEventSelected(
        tvEvent: TvEvent? = null,
        vodItem: VODItem? = null,
        parentalRatingDisplayName: String,
        railItem: RailItem,
        currentTime: Long,
        isStillFocused: () -> Boolean
    )

    fun onBroadcastChannelSelected(
        tvChannel: TvChannel?,
        railItem: RailItem
    )

    fun isEventLocked(tvEvent: TvEvent?): Boolean
}