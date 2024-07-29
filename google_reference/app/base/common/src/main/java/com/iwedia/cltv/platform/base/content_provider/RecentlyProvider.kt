package com.iwedia.cltv.platform.base.content_provider

import android.os.CountDownTimer
import android.util.Log
import com.iwedia.cltv.platform.`interface`.ChannelDataProviderInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlayableItem

class RecentlyProvider(private val dataProvider: ChannelDataProviderInterface) {

    val RECENTLY_MAX_COUNT = 10
    var TAG = "ReferenceRecentlyHandler"

    /**
     * Recently watched list
     */
    private val recentlyWatchedItems: MutableList<PlayableItem> = mutableListOf()

    /**
     * Recently watched update timer
     */
    private var recentlyWatchedUpdateTimer: CountDownTimer? = null

    /**
     * Previously watched
     */
    private var previouslyWatched: PlayableItem? = null

    /**
     * Channel list update event listener
     */
    private var eventListener: Any? = null

    /**
     * Channel list updated flag
     */
    private var channelListUpdated = false


    private fun updateRecentlyWatchedList() {
        var channelList = dataProvider.getChannelList()
        var itemsForDelete = mutableListOf<PlayableItem>()
        recentlyWatchedItems.forEach { recentlyWatched ->
            if (recentlyWatched is TvChannel) {
                Log.d(
                    com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
                    TAG,
                    " Recently watched channel ${(recentlyWatched as TvChannel).name}"
                )
                var isExisting: Boolean = false
                channelList.forEach { channel ->
                    if ((recentlyWatched as TvChannel).id == channel.id) {
                        isExisting = true
                    }
                }
                if (!isExisting) {
                    Log.d(Constants.LogTag.CLTV_TAG +
                        TAG,
                        " Remove from recently watched ${(recentlyWatched as TvChannel).name}"
                    )
                    itemsForDelete.add(recentlyWatched)
                }
            }
        }
        itemsForDelete.forEach { item ->
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " Removed from recently watched ${(item as TvChannel).name}")
            recentlyWatchedItems.remove(item)
        }
    }

    fun setup() {
        recentlyWatchedItems.clear()
        previouslyWatched = null
        if (InformationBus.isListenerInitialized()) {
            InformationBus.informationBusEventListener?.registerEventListener(
                arrayListOf(Events.CHANNEL_LIST_UPDATED),
                callback = {
                    eventListener = it
                },
                onEventReceived = {
                    channelListUpdated = true
                })
        }
    }

    fun dispose() {
        recentlyWatchedItems.clear()
        eventListener?.let {
            InformationBus.informationBusEventListener?.unregisterEventListener(it)
        }
    }

    /**
     * Start recently watched update timer
     */
    private fun startUpdateTimer() {
        //Cancel timer if it's already started
        stopUpdateTimer()

        //Start new count down timer
        recentlyWatchedUpdateTimer = object :
            CountDownTimer(
                2000,
                1000
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                InformationBus.informationBusEventListener?.submitEvent(Events.RECENTLY_WATCHED_UPDATED)
            }
        }
        recentlyWatchedUpdateTimer!!.start()
    }

    /**
     * Stop recently watched update timer if it is already started
     */
    private fun stopUpdateTimer() {
        if (recentlyWatchedUpdateTimer != null) {
            recentlyWatchedUpdateTimer!!.cancel()
            recentlyWatchedUpdateTimer = null
        }
    }

    /**
     * Add recently watched item
     *
     * @param playableItem recently watcehd playable item
     */
    fun addRecentlyWatched(playableItem: PlayableItem) {
        if (previouslyWatched == null) {
            if (playableItem is TvChannel) {
                previouslyWatched = playableItem
            }
            return
        } else {
            var isExisting: Boolean
            isExisting = recentlyWatchedItems.contains(previouslyWatched)
            for (existingItem in recentlyWatchedItems) {
                if (existingItem is TvChannel) {
                    if (previouslyWatched is TvChannel) {
                        if (existingItem.id == (previouslyWatched as TvChannel).id) {
                            isExisting = true
                            break
                        }
                        if ((existingItem as TvChannel).displayNumber == (previouslyWatched as TvChannel).displayNumber) {
                            isExisting = true
                            break
                        }
                        if (existingItem.name == (previouslyWatched!! as TvChannel).name) {
                            isExisting = true
                            break
                        }
                    }
                }
            }

            if (!isExisting && !(previouslyWatched as TvChannel).isFastChannel()) {
                recentlyWatchedItems.add(previouslyWatched!!)
                //ReferenceSdk.activity.runOnUiThread {
                startUpdateTimer()
                //}
            }

            //Limit recently watched
            if (recentlyWatchedItems.size > RECENTLY_MAX_COUNT) {
                recentlyWatchedItems.removeAt(0)
                //ReferenceSdk.activity.runOnUiThread {
                startUpdateTimer()
                //}
            }
            if (playableItem is TvChannel) {
                previouslyWatched = playableItem
            }
        }
    }

    /**
     * Get recently watched items
     *
     * @return recently watched items
     */
    fun getRecentlyWatched(): MutableList<PlayableItem>? {
        if (channelListUpdated) {
            updateRecentlyWatchedList()
            channelListUpdated = false
        }
        return recentlyWatchedItems
    }
}