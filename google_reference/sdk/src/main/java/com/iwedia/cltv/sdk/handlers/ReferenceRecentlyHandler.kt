package com.iwedia.cltv.sdk.handlers

import android.os.CountDownTimer
import android.util.Log
import api.HandlerAPI
import com.iwedia.cltv.sdk.ReferenceEvents
import com.iwedia.cltv.sdk.ReferenceSdk
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import core_entities.PlayableItem
import core_entities.TvChannel
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus

class ReferenceRecentlyHandler : HandlerAPI {

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
    private var eventListener: ChannelListUpdateEventListener ?= null

    /**
     * Channel list updated flag
     */
    private var channelListUpdated = false

    /**
     * Channel list update event listener
     */
    private inner class ChannelListUpdateEventListener : EventListener {
        constructor() {
            addType(ReferenceEvents.CHANNEL_LIST_UPDATED)
        }

        override fun callback(event: Event?) {
            channelListUpdated = true
        }
    }

    private fun updateRecentlyWatchedList() {
        var channelList = ReferenceSdk.tvHandler!!.getChannelList()
        var itemsForDelete = mutableListOf<PlayableItem>()
        recentlyWatchedItems.forEach { recentlyWatched ->
            if (recentlyWatched!!.playableObject is TvChannel) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, " Recently watched channel ${(recentlyWatched.playableObject as ReferenceTvChannel).name}")
                var isExisting: Boolean = false
                channelList.value.forEach { channel ->
                    if ((recentlyWatched.playableObject as ReferenceTvChannel).id == channel.id) {
                        isExisting = true
                    }
                }
                if (!isExisting) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, " Remove from recently watched ${(recentlyWatched.playableObject as ReferenceTvChannel).name}")
                    itemsForDelete.add(recentlyWatched)
                }
            }
        }
        itemsForDelete.forEach { item ->
            Log.d(Constants.LogTag.CLTV_TAG + TAG, " Removed from recently watched ${(item.playableObject as ReferenceTvChannel).name}")
            recentlyWatchedItems.remove(item)
        }
    }

    override fun setup() {
        recentlyWatchedItems.clear()
        previouslyWatched = null
        eventListener = ChannelListUpdateEventListener()
        InformationBus.registerEventListener(eventListener)
    }

    override fun dispose() {
        recentlyWatchedItems.clear()
        eventListener?.let {
            InformationBus.unregisterEventListener(it)
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
                InformationBus.submitEvent(Event(ReferenceEvents.RECENTLY_WATCHED_UPDATED))
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
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "addRecentlyWatched: ######### ADD TO RECENT $playableItem")
        if (previouslyWatched == null) {
            if (playableItem.playableObject is TvChannel) {
                previouslyWatched = playableItem
            }
            return
        } else {
            var isExisting: Boolean
            isExisting = recentlyWatchedItems.contains(previouslyWatched)
            for (existingItem in recentlyWatchedItems) {
                if (existingItem.playableObject is TvChannel) {
                    if (previouslyWatched!!.playableObject is TvChannel) {
                        if ((existingItem.playableObject as ReferenceTvChannel).id == (previouslyWatched!!.playableObject as ReferenceTvChannel).id) {
                            isExisting = true
                            break
                        }
                        if ((existingItem.playableObject as ReferenceTvChannel).displayNumber == (previouslyWatched!!.playableObject as ReferenceTvChannel).displayNumber) {
                            isExisting = true
                            break
                        }
                        if ((existingItem.playableObject as ReferenceTvChannel).name == (previouslyWatched!!.playableObject as ReferenceTvChannel).name) {
                            isExisting = true
                            break
                        }
                    }
                }
            }

            if (!isExisting) {
                recentlyWatchedItems.add(previouslyWatched!!)
                ReferenceSdk.activity.runOnUiThread {
                    startUpdateTimer()
                }
            }

            //Limit recently watched
            if (recentlyWatchedItems.size > RECENTLY_MAX_COUNT) {
                recentlyWatchedItems.removeAt(0)
                ReferenceSdk.activity.runOnUiThread {
                    startUpdateTimer()
                }
            }
            if (playableItem.playableObject is TvChannel) {
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