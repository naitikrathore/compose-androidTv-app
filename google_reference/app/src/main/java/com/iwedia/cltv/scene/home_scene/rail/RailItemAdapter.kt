package com.iwedia.cltv.scene.home_scene.rail

import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.components.custom_card.CustomCard
import com.iwedia.cltv.components.DiffUtilCallback
import com.iwedia.cltv.components.custom_card.CustomCardVod
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.recording.ScheduledRecording
import com.iwedia.cltv.scene.channel_list.ChannelListItem
import tv.anoki.ondemand.domain.model.VODItem

/**
 * This adapter is used in:

 *      1) SearchScene
 *      2) ForYouWidget
 *      3) RecordingsWidget
 *
 *    and it is used for showing CustomCards for corresponding item. Item can be Event, Recording, Scheduled Recording.
 *
 *    but whatever item is inside CustomCard, behavior of Cards are the same (or with some small differences) in all of SearchScene, RecordingWidget and ForYouWidget, thus there is no need for 3 different adapters for all those scenes/widgets.

 *
 * Every single instance of this adapter handles multiple events as CustomCards inside HorizontalGridView.
 *
 * @param railItemAdapterType is used in onCreateViewHolder in order to create different subclasses of abstract class RailItemViewHolder.kt depending on from which scene or widget RailItem was created.
 * @author Boris Tirkajla
 */
class RailItemAdapter(
    private val railItemAdapterType: RailItemAdapterType,
    private val dateTimeFormat: DateTimeFormat,
    private val ttsSetterInterface: TTSSetterInterface
) :
    RecyclerView.Adapter<RailItemViewHolder>() {

    enum class RailItemAdapterType {
        EVENT,
        CHANNEL_SEARCH_SCENE,
        RECORDING,
        VOD,
        SCHEDULED_RECORDING,
        BROADCAST_CHANNEL,
    }

    //Items
    /**
     * ITEMS should be MutableList of one of the following types:
     *
     *    1) TvEvent
     *    2) Recording
     *    3) ScheduledRecording
     *
     *    idea is that refresh() method can add list of one of those types and than onBindViewHolder will handle all those situations. Main reason why this is used is because RailItem can contain different CustomCard (Event, Recording or Scheduled Recording)
     */
    private var items: MutableList<Any> = mutableListOf()

    //listener
    var adapterListener: AdapterListener? = null

    //DiffUtil DiffResult instance
    private var diffResult: DiffUtil.DiffResult? = null

    //view holders hash map
    var holders: HashMap<Int, RailItemViewHolder> = HashMap()

    //Selected item index
    private var selectedItemIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RailItemViewHolder {
        val linearLayout = LinearLayout(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        return when (railItemAdapterType) {
            RailItemAdapterType.CHANNEL_SEARCH_SCENE -> SearchRailItemViewHolder(
                linearLayout,
                ttsSetterInterface
            )

            RailItemAdapterType.EVENT -> ForYouRailItemViewHolder(linearLayout, ttsSetterInterface)
            RailItemAdapterType.RECORDING -> RecordingRailItemViewHolder(
                linearLayout,
                ttsSetterInterface
            )

            RailItemAdapterType.SCHEDULED_RECORDING -> ScheduledRecordingRailItemViewHolder(
                linearLayout, ttsSetterInterface
            )

            RailItemAdapterType.VOD -> VodRailItemViewHolder(linearLayout, ttsSetterInterface)
            RailItemAdapterType.BROADCAST_CHANNEL -> BroadcastTvChannelViewHolder(
                linearLayout,
                ttsSetterInterface
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: RailItemViewHolder, position: Int) {
        val item = items[position]
        holders[position] = holder


        var tvEvent: TvEvent? = null
        var tvChannel: TvChannel? = null
        var vodItem: VODItem? = null

        when (item) {

            is TvEvent -> { // this can be called from ForYouWidget, SearchScene and RecordingWidget (in RecordingWidget it will occur if ScheduledRecording exists)
                tvEvent = item
                tvChannel = item.tvChannel
            }

            is Recording -> { // this can be called from ForYouWidget, SearchScene and RecordingWidget.
                tvChannel = item.tvChannel!!
                tvEvent = item.tvEvent ?: TvEvent.createNoInformationEvent(
                    tvChannel,
                    adapterListener?.getCurrentTime(tvChannel)!!
                )
            }

            is ScheduledRecording -> {
                tvChannel = item.tvChannel!!
                tvEvent = item.tvEvent ?: TvEvent.createNoInformationEvent(
                    tvChannel,
                    adapterListener?.getCurrentTime(tvChannel)!!
                )
            }

            is VODItem -> {
                vodItem = item
            }

            is TvChannel -> {
                tvChannel = item
            }

            else -> {
                throw Exception("It's not allowed to have any other type except TvEvent, Recording or ScheduledRecording. If this error occurs check what was send in RailItemAdapter's refresh() method from ForYouWidget, SearchScene or RecordingWidget.")
            }
        }

        when (holder) {
            is SearchRailItemViewHolder -> {
                (holder.channelAndEventCard as CustomCard.CustomCardSearchChannel).updateData(
                    item = ChannelListItem(tvChannel!!, tvEvent),
                    isParentalEnabled = false,
                    ""
                )
            }

            is ForYouRailItemViewHolder -> {
                (holder.channelAndEventCard as CustomCard.CustomCardForYou).updateData(
                    tvEvent = tvEvent!!,
                    isParentalEnabled = adapterListener!!.isParentalControlsEnabled(),
                    isInWatchlist = adapterListener!!.isInWatchlist(tvEvent),
                    isInRecList = adapterListener!!.isInRecList(tvEvent),
                    isEventLocked = adapterListener!!.isEventLocked(tvEvent),
                    dateTimeFormat = dateTimeFormat
                )
            }

            is RecordingRailItemViewHolder -> {
                (holder.channelAndEventCard as CustomCard.CustomCardRecording).updateData(
                    recording = item as Recording,
                    isParentalEnabled = adapterListener!!.isParentalControlsEnabled()
                )
            }

            is ScheduledRecordingRailItemViewHolder -> {
                (holder.channelAndEventCard as CustomCard.CustomCardScheduledRecording).updateData(
                    scheduledRecording = item as ScheduledRecording
                )
            }

            is VodRailItemViewHolder -> {
                (holder.channelAndEventCard as CustomCardVod).updateData(
                    item as VODItem
                )
            }

            is BroadcastTvChannelViewHolder -> {
                (holder.channelAndEventCard as CustomCard.CustomCardBroadcastChannel).updateData(
                    item = item as TvChannel,
                    isParentalEnabled = adapterListener!!.isParentalControlsEnabled(),
                    ""
                )
            }
        }

        holder.channelAndEventCard.setOnClick {
            selectedItem(item, position)
        }

        holder.channelAndEventCard.setOnLongClick {
            selectedItem(item, position)
        }

        holder.channelAndEventCard.setOnCardAnimation { adapterListener!!.onCardAnimated() } // VERY IMPORTANT for setting the animation of the Rails.

        holder.channelAndEventCard.setOnFocusChanged { hasFocus ->
            if (hasFocus) {
                selectedItemIndex = position
                holder.resetAlpha()
                tvEvent?.let {
                    adapterListener!!.onEventSelected(
                        tvEvent = tvEvent,
                        eventPosition = position,
                        isStillFocused = { holder.channelAndEventCard.isFocused })
                }

                vodItem?.let {
                    adapterListener!!.onVodItemSelected(
                        vodItem = vodItem,
                        eventPosition = position,
                        isStillFocused = { holder.channelAndEventCard.isFocused })
                }

                tvChannel?.let {
                    if (tvEvent == null)
                        adapterListener!!.onBroadcastChannelSelected(tvChannel)
                }

            }
        }

        holder.channelAndEventCard.setOnKeyListener(object : View.OnKeyListener {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onKey(view: View?, p1: Int, keyEvent: KeyEvent?): Boolean {

                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
                }

                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }

                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    if (p1 == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (keyEvent.repeatCount > 0 && keyEvent.repeatCount % 5 != 0) {
                            return true
                        }
                        return adapterListener!!.onKeyDown()
                    }
                    if (p1 == KeyEvent.KEYCODE_DPAD_UP) {
                        if (keyEvent.repeatCount > 0 && keyEvent.repeatCount % 5 != 0) {
                            return true
                        }
                        return adapterListener!!.onKeyUp()
                    }
                    if (p1 == KeyEvent.KEYCODE_DPAD_LEFT || p1 == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (keyEvent.repeatCount % 5 != 0) {
                            return true
                        }
                        if (p1 == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            if (position == itemCount - 1) return true // focus is on the last item - do not change alpha value
                            if (railItemAdapterType == RailItemAdapterType.BROADCAST_CHANNEL) {
                                if (position >= 0 && position < itemCount - 6) {
                                    holders[position]?.reduceAlpha()
                                }
                            } else if (position >= 0 && position < itemCount - 4) {
                                holders[position]?.reduceAlpha()
                            }
                        }
                    }

                }
                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    if (p1 == KeyEvent.KEYCODE_BACK) {
                        adapterListener!!.onBackPressed()
                    }
                }
                return false
            }
        })

        if (position > selectedItemIndex) {
            holder.resetAlpha()
        } else {
            holder.reduceAlpha()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun refresh(adapterItems: MutableList<Any>) {
        val diffCallback = DiffUtilCallback(this.items, adapterItems)
        diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(adapterItems)
        diffResult?.dispatchUpdatesTo(this)
    }

    // todo this won't work as it should. This is copied from RecordingsEventsAdapter which is not used anymore. However, method with removing item on some position should exist and it should be used instead of removeItem which calls notifyDataChanged() which is not good.
    fun removeItemAtPosition(position: Int) {
        if (this.items.size > position) {
            this.items.removeAt(position)
        }
    }

    fun updateItemAtPosition(newRecording: Recording, clickedItemPosition: Int) {
        if (this.items.isNotEmpty()) {
            items[clickedItemPosition] = newRecording
            notifyItemChanged(clickedItemPosition)
        }
    }

    interface AdapterListener {
        fun onEventSelected(tvEvent: TvEvent, eventPosition: Int, isStillFocused: () -> Boolean)
        fun onVodItemSelected(vodItem: VODItem, eventPosition: Int, isStillFocused: () -> Boolean)
        fun getAdapterPosition(position: Int)

        /**
         * @return true if focus was changed to topMenu
         */
        fun onKeyUp(): Boolean
        fun onBackPressed()
        fun onKeyDown(): Boolean

        /**
         * @param item can be one of the following type:

         *      1) TvEvent (possible when clicking on item in ForYouWidget or SearchScene)
         *      2) Recording (possible when clicking on item in ForYouWidget, SearchScene or RecordingWidget)
         *      3) ScheduledRecording (possible when clicking on item in RecordingWidget)
         *
         * @param position used in order to be able to refresh item if any changes are made to using notifyItemChanged() if needed.
         * This method is triggered either in ForYouWidget, SearchScene or RecordingsWidget when user clicks on one of the cards.
         */
        fun onItemClicked(item: Any, position: Int)
        fun isChannelLocked(channelId: Int): Boolean
        fun isParentalControlsEnabled(): Boolean
        fun isInWatchlist(tvEvent: TvEvent): Boolean
        fun isInRecList(tvEvent: TvEvent): Boolean
        fun onCardAnimated()
        fun getCurrentTime(tvChannel: TvChannel): Long
        fun isEventLocked(tvEvent: TvEvent): Boolean
        fun onBroadcastChannelSelected(tvChannel: TvChannel)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun selectedItem(item: Any, position: Int) {
        (ReferenceApplication.getActivity() as MainActivity).resetTimerOnRcuClick()
        when (item) {
            is TvEvent -> {
                item.data = position
            }

            is Recording -> {
                item.tvEvent?.data = position
            }

            is ScheduledRecording -> {
                item.tvEvent?.data = position
            }
        }
        adapterListener!!.onItemClicked(item, position)
    }


    fun setListener(adapterListener: AdapterListener) {
        this.adapterListener = adapterListener
    }

    fun dispose() {
        adapterListener = null

        if (items.isNotEmpty()) {
            items.clear()
        }
        if (holders.isNotEmpty()) {
            holders.clear()
        }
    }
}