package com.iwedia.cltv.scene.infoBanner

import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.components.DiffUtilCallback
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent


/**
 * Info banner events adapter
 *
 * @author Aleksandar Lazic
 */
class InfoBannerEventsAdapter(
    val dateTimeFormat: DateTimeFormat,
    private val toSpeechTextSetterInterface: TTSSetterInterface
) : RecyclerView.Adapter<InfoBannerEventsViewHolder>() {

    private val TAG = javaClass.simpleName

    //Items
    private var items: MutableList<TvEvent> = mutableListOf()

    //listener
    var adapterListener: AdapterListener? = null

    //keep focus on item when buttons are focused
    var shouldKeepFocus = false

    //check if we should use white selected bg
    var eventHasButtons = false

    var currentChannel: TvChannel? = null

    //first time focus
    var firstItemFocus = true

    // _isDpadDownEnabled is used to disable DpadDown until buttons are positioned as they should.
    // Main thing is that delay of 500 ms is added and in that time user can go down and select some
    // of the buttons and when buttons are rearranged focus can potentially be lost
    private var _isDpadDownEnabled = true

    var map = HashMap<Int, InfoBannerEventsViewHolder>()

    //DiffUtil DiffResult instance
    private var diffResult: DiffUtil.DiffResult?= null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoBannerEventsViewHolder {
        val linearLayout = LinearLayout(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        return InfoBannerEventsViewHolder(
            linearLayout,
            toSpeechTextSetterInterface
        )
    }

    private fun checkRecording(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>) {
        adapterListener?.hasScheduledRecording(tvEvent, object : IAsyncDataCallback<Boolean> {
            override fun onReceive(data: Boolean) {
                callback.onReceive(data)
            }

            override fun onFailed(error: Error) {
                callback.onFailed(error)
            }
        })
    }

    override fun onBindViewHolder(holder: InfoBannerEventsViewHolder, position: Int) {
        val item = items[position]
        map[position] = holder

        var hasReminder = false
        var hasRecording = false
        val isChannelCurrent = currentChannel?.let{
            TvChannel.compare(
                it,
                item.tvChannel
            )
        }
        holder.eventCustomCard.updateData(
            tvEvent = item,
            isParentalEnabled = adapterListener!!.isParentalControlsEnabled(),
            isCurrentChannel = isChannelCurrent,
            isInWatchlist = adapterListener!!.isInWatchlist(item),
            isInRecList = adapterListener!!.isInRecList(item),
            currentTime = adapterListener!!.getCurrentTime(currentChannel!!),
            dateTimeFormat = dateTimeFormat,
            isEventLocked = adapterListener!!.isEventLocked(item)
        )
        //TODO Boris
        /*val recordingCallback = object : IAsyncDataCallback<Boolean> {
            override fun onFailed(error: Error) {
                holder.eventCustomCard.setReminderRecordingIcons(hasReminder,hasRecording)
            }

            override fun onReceive(data: Boolean) {
                hasRecording = data
                holder.eventCustomCard.setReminderRecordingIcons(hasReminder,hasRecording)
            }
        }
        adapterListener?.hasScheduledReminder(item, object : IAsyncDataCallback<Boolean> {
            override fun onFailed(error: Error) {
                checkRecording(item, recordingCallback)
            }

            override fun onReceive(data: Boolean) {
                hasReminder = data
                checkRecording(item, recordingCallback)
            }
        })*/

        holder.eventCustomCard.setOnClick {
            //on event clicked should show details scene (more info)
            //there is no difference for current event (click on current event card should not zap to it)
//            var currentTime = System.currentTimeMillis()
            //current event
//            if (item.startTime < currentTime && item.endTime > currentTime) {
//                adapterListener!!.onEventClicked(item)
//            }
            if (items.size == 1 && item.name == ConfigStringsManager.getStringById("no_info")) {
                adapterListener!!.onEventClicked(item)
//                adapterListener!!.onEventClickedWithDpad()
            }
            else {
                adapterListener!!.onEventClicked(item)
            }
        }

        holder.eventCustomCard.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {

                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_CAPTIONS) {
                        if (isChannelCurrent == true && adapterListener!!.isCurrentEvent(item)){
                            adapterListener!!.onCCPressed()
                            return true
                        }
                    }
                }

                if (holder.eventCustomCard.isAnimationInProgress) {
                    return true
                }

                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (!_isDpadDownEnabled) {
                        return true
                    }
                    if (eventHasButtons) {

                        //Disable dpad down during list scrolling
                        if (adapterListener!!.isScrolling()) {
                            shouldKeepFocus = false
                            return true
                        }
                        holder.eventCustomCard.keepFocus()
                        adapterListener!!.onDownPressed()
                    }
                } else if (keyCode == KeyEvent.KEYCODE_BOOKMARK && event!!.action == KeyEvent.ACTION_UP) {
                    adapterListener!!.onWatchlistClicked(item)
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_PROG_RED && event!!.action == KeyEvent.ACTION_UP) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "KeyEvent red")
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_RECORD && event!!.action == KeyEvent.ACTION_UP) {
                    if(adapterListener!!.getConfigInfo("pvr")) {
                        adapterListener!!.onRecordClicked(item)
                    }
                }
                return false
            }
        })

        holder.eventCustomCard.setOnFocusChanged { hasFocus ->
            if (hasFocus) {
                _isDpadDownEnabled = false
                shouldKeepFocus = false
                triggerEventSelected(item, position)
                adapterListener!!.rearrangeScene(true) // todo REMOVE THIS WHEN DESCRIPTION CUSTOM VIEW IS CREATED in the future. Inside this method there is logic about setting view's properties connected to event's description
            }
        }
    }

    /**
     * Event selected count down timer
     * Triggers onEventSelected listener with 500ms delay
     * Added in order to avoid multiple selected item details info refreshing during fast scrolling
     */
    private var eventSelectedTimer: CountDownTimer? = null
    private fun triggerEventSelected(item: TvEvent, position: Int) {
        eventSelectedTimer?.cancel()
        eventSelectedTimer = object :
            CountDownTimer(
                500,
                500
            ) {
            override fun onTick(millisUntilFinished: Long) {}

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                adapterListener?.onEventSelected(item, position, onSelected = {
                    _isDpadDownEnabled = true
                })
            }
        }
        eventSelectedTimer!!.start()
    }

    //TODO hint: $#@1 check whether this can be deleted when Details Custom Component is created and when UI is modified
    // VERY important to check what rearrangeScene method does...
//    private fun handleEventWithImages(
//        holder: InfoBannerEventsViewHolder,
//        item: TvEvent
//    ) {
//        //show title, time... in scene (different design)
//        adapterListener!!.rearrangeScene(true)

    //TODO hint: $#@1 check whether this can be deleted when Details Custom Component is created and when UI is modified
    // VERY important to check what rearrangeScene method does...
//    private fun handleEventWithoutImages(
//        holder: InfoBannerEventsViewHolder,
//        item: TvEvent
//    ) {
//        //remove title, time... in scene (different design)
//        adapterListener!!.rearrangeScene(false)
//    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Context
    private fun getContext(): Context {
        return ReferenceApplication.applicationContext()
    }

    fun refresh(adapterItems: MutableList<TvEvent>) {
        val diffCallback = DiffUtilCallback(this.items, adapterItems)
        diffResult = DiffUtil.calculateDiff(diffCallback)
        this.items.clear()
        this.items.addAll(adapterItems)
        diffResult?.dispatchUpdatesTo(this)
    }

    fun refreshEventAtPosition(position: Int) {
        notifyItemChanged(position)
    }

    interface AdapterListener: TTSSetterInterface {
        fun onEventSelected(item: TvEvent, selectedItemPositionInAdapter: Int, onSelected: () -> Unit)
        fun getAdapterPosition(position: Int)
        fun rearrangeScene(hasImages: Boolean)
        fun onEventClicked(item: TvEvent)
        fun onWatchlistClicked(item: TvEvent)
//        fun onEventClickedWithDpad()
        fun onRecordClicked(item: TvEvent)
        fun isScrolling(): Boolean
        fun hasScheduledReminder(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>)
        fun hasScheduledRecording(tvEvent: TvEvent, callback: IAsyncDataCallback<Boolean>)
        fun onDownPressed()
        fun onCCPressed()
        fun isParentalControlsEnabled():Boolean
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun isInWatchlist(tvEvent: TvEvent): Boolean?
        fun isInRecList(tvEvent: TvEvent): Boolean
        fun isCurrentEvent(tvEvent: TvEvent) : Boolean
        fun isEventLocked(tvEvent: TvEvent): Boolean
        fun getConfigInfo(nameOfInfo: String): Boolean
    }

    fun setListener(adapterListener: AdapterListener) {
        this.adapterListener = adapterListener
    }
}