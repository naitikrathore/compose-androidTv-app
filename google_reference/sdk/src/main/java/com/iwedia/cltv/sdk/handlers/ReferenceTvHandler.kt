package com.iwedia.cltv.sdk.handlers

import android.media.tv.TvInputInfo
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.text.isDigitsOnly
import com.iwedia.cltv.TunerManager
import com.iwedia.cltv.sdk.*
import com.iwedia.cltv.sdk.entities.ReferenceFavoriteItem
import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import com.iwedia.guide.android.tools.GAndroidSmartChannelZapper
import core_entities.Error
import core_entities.PlayableItem
import core_entities.PlayableItemType
import core_entities.TvChannel
import data_type.GList
import handlers.DataProvider
import handlers.PrefsHandler
import handlers.TvHandler
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import utils.information_bus.Event
import utils.information_bus.EventListener
import utils.information_bus.InformationBus
import utils.information_bus.events.Events
import utils.information_bus.events.PrefsStoreRequestEvent

/**
 * Reference tv handler
 *
 * @author Dejan Nadj
 */
class ReferenceTvHandler(dataProvider: DataProvider<*>) :
    TvHandler<ReferenceTvChannel>(dataProvider) {

    object FilterItem {
        //Below values should be in sync with com.iwedia.cltv.entities.FilterItem values.
        const val ALL_ID = 0
        const val TIF_INPUT_CATEGORY = 500
        const val FAVORITE_ID = 600
        const val RECENTLY_WATCHED_ID = 800
        const val RADIO_CHANNELS_ID = 900
        const val TERRESTRIAL_TUNER_TYPE_ID = 1000
        const val CABLE_TUNER_TYPE_ID = 1100
        const val SATELLITE_TUNER_TYPE_ID = 1200

        //Below value used as error code to show Toast
        const val ONLY_ONE_CHANNEL_IN_LIST = 2002
    }

    var eventListener: TvHandlerListener? = null
    var smartChannelZapper: GAndroidSmartChannelZapper<GAndroidSmartChannelZapper.GFastChannelZapListener>? =
        null
    var desiredChannelIndex = 0
    var desiredChannelDisplayNumber = 0
    var activeChannel: ReferenceTvChannel? = null
    var tunerManager: TunerManager? = null
    var activeCategoryId = FilterItem.ALL_ID
    var activeFavGroupName = ""
    private val TAG = "ReferenceTvHandler"

    init {
        eventListener = TvHandlerListener()
//        InformationBus.registerEventListener(eventListener)

        smartChannelZapper = GAndroidSmartChannelZapper()
        smartChannelZapper!!.smartChannelZapTimeout = 1000

        tunerManager = TunerManager()
    }


    override fun dispose() {
        super.dispose()
        smartChannelZapper!!.dispose()
        InformationBus.unregisterEventListener(eventListener!!)
    }

    override fun setup() {
        super.setup()
    }

    inner class TvHandlerListener : EventListener {
        constructor() {
            addType(Events.CHANNELS_LOADED)
        }

        override fun callback(event: Event?) {
            super.callback(event)
            if (event!!.type == Events.CHANNELS_LOADED) {
                getActiveChannel(object : AsyncDataReceiver<ReferenceTvChannel> {
                    override fun onFailed(error: Error?) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: INIT DESIRED CHANNEL $error")
                    }

                    override fun onReceive(data: ReferenceTvChannel) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onReceive: INIT DESIRED CHANNEL ${data.index} ${data.name}")
                        activeChannel = data
                    }

                })

            }
        }
    }

    /**
     * Find Channel Position
     *
     * @param tvChannel tv channel
     * @return position
     */
    fun findChannelPosition(tvChannel: ReferenceTvChannel): Int {
        var position = 0
        var i = 0
        getChannelList().value.forEach { item ->
            if (tvChannel.id == item.id) {
                position = i
            }

            i++
        }
        return position
    }

    override fun changeChannel(channel: ReferenceTvChannel, callback: AsyncReceiver?) {
        (ReferenceSdk.playerHandler as ReferencePlayerHandler).isChannelUnlocked = false
        forceOptimizedChannelChange(findChannelPosition(channel), false, false, callback!!)
    }

    fun changeChannel(index: Int, callback: AsyncReceiver?) {
        (ReferenceSdk.playerHandler as ReferencePlayerHandler).isChannelUnlocked = false
        forceOptimizedChannelChange(index, false, false, callback!!)
    }

    override fun nextChannel(callback: AsyncReceiver?) {
        (ReferenceSdk.playerHandler as ReferencePlayerHandler).isChannelUnlocked = false
        var newDesiredChannelIndex = 0
        if (activeCategoryId == FilterItem.FAVORITE_ID) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for FavoriteGroup $activeFavGroupName")
            //Get the favorite channel list.
            var channelList: GList<ReferenceTvChannel> = getChannelList()
            val favoriteChannelList: ArrayList<ReferenceTvChannel> = ArrayList()
            val selectedChannelList: GList<ReferenceTvChannel> = GList()
            //Get all channels for selected favorite group
            channelList.value.forEach { tvChannel ->
                if (tvChannel.favListIds.contains(activeFavGroupName)) {
                    favoriteChannelList.add(tvChannel)
                }
            }

            if (favoriteChannelList.isEmpty()) {
                //list is empty, reset the next order to all list.
                activeCategoryId = FilterItem.ALL_ID
                return
            } else if (favoriteChannelList.size == 1) {
                //only one channel, channel switch not required
                callback?.onFailed(
                    Error(
                        FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                        "Single Entry in List"
                    )
                )
                return
            }

            //Sorting data list by displayNumber so zapping on Favorite channel list should work in sequence.
            try {
                favoriteChannelList.sortBy { it.displayNumber }
            } catch (ex: Exception) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onReceive: display number missing")
            }

            favoriteChannelList.forEach { tvChannel ->
                selectedChannelList.add(tvChannel)
            }
            playNextIndex(selectedChannelList, callback)
        } else if ((activeCategoryId == FilterItem.RADIO_CHANNELS_ID) || (activeCategoryId == FilterItem.TERRESTRIAL_TUNER_TYPE_ID)
            || (activeCategoryId == FilterItem.CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItem.SATELLITE_TUNER_TYPE_ID)
            || (activeCategoryId == FilterItem.RECENTLY_WATCHED_ID)
        ) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for Category: $activeCategoryId")
            val selectedChannelList: GList<ReferenceTvChannel> = GList()

            //We are sorting the list so Channel +/- works properly
            var sortedChannelList: GList<ReferenceTvChannel> = getChannelList()
            sortedChannelList.value.sortBy { it.displayNumber }

            when (activeCategoryId) {
                FilterItem.RADIO_CHANNELS_ID -> {
                    sortedChannelList.value.forEach { tvChannel ->
                        if (tvChannel.isRadioChannel) {
                            selectedChannelList.add(tvChannel)
                        }
                    }
                }
                FilterItem.TERRESTRIAL_TUNER_TYPE_ID -> {
                    sortedChannelList.value.forEach { channel ->
                        if (channel.tunerType == ReferenceTvChannel.TERRESTRIAL_TUNER_TYPE) {
                            selectedChannelList.add(channel)
                        }
                    }
                }
                FilterItem.CABLE_TUNER_TYPE_ID -> {
                    sortedChannelList.value.forEach { channel ->
                        if (channel.tunerType == ReferenceTvChannel.CABLE_TUNER_TYPE) {
                            selectedChannelList.add(channel)
                        }
                    }
                }
                FilterItem.SATELLITE_TUNER_TYPE_ID -> {
                    sortedChannelList.value.forEach { channel ->
                        if (channel.tunerType == ReferenceTvChannel.SATELLITE_TUNER_TYPE) {
                            selectedChannelList.add(channel)
                        }
                    }
                }
                FilterItem.RECENTLY_WATCHED_ID -> {

                    var sortedPlaybleItems = ReferenceSdk.recentlyHandler!!.getRecentlyWatched()!!
                    if (sortedPlaybleItems != null && sortedPlaybleItems.size > 0)
                        sortedPlaybleItems.sortBy { (it.playableObject as ReferenceTvChannel).displayNumber }

                    for (playableItem in sortedPlaybleItems) {
                        if (playableItem.itemType == PlayableItemType.TV_CHANNEL) {
                            selectedChannelList.add(playableItem.playableObject as ReferenceTvChannel)
                        }
                    }
                }
            }

            //If list is empty, Reset the activeCategoryId to ALL
            if (selectedChannelList.size() == 0) {
                activeCategoryId = FilterItem.ALL_ID
                return
            } else if (selectedChannelList.size() == 1) {
                //only one channel, channel switch not required
                callback?.onFailed(
                    Error(
                        FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                        "Single Entry in List"
                    )
                )
                return
            }
            playNextIndex(selectedChannelList, callback)
        } else if ((activeCategoryId >= FilterItem.TIF_INPUT_CATEGORY) && (ReferenceSdk.tvInputHandler != null)) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for TIF Category: $activeCategoryId")
            ReferenceSdk.tvInputHandler!!.getTvInputList(object :
                AsyncDataReceiver<MutableList<TvInputInfo>> {
                override fun onReceive(tvInputInfos: MutableList<TvInputInfo>) {
                    val scannedInputs: MutableList<TvInputInfo> = mutableListOf()

                    for (tvInputInfo in tvInputInfos) {
                        var isInputScanned = false

                        getChannelList().value.forEach { channel ->
                            if (tvInputInfo.id.equals(channel.inputId, ignoreCase = true)) {
                                isInputScanned = true
                            }
                        }
                        if (isInputScanned) {
                            scannedInputs.add(tvInputInfo)
                        }
                    }
                    val inputIndex: Int = activeCategoryId - FilterItem.TIF_INPUT_CATEGORY
                    if (inputIndex < tvInputInfos.size && inputIndex >= 0) {
                        if (inputIndex >= 0) {
                            val tvInputInfo = scannedInputs[inputIndex]
                            val inputId = tvInputInfo.id
                            val selectedChannelList: GList<ReferenceTvChannel> = GList()

                            //We are sorting the list so Channel +/- works properly
                            var sortedChannelList: GList<ReferenceTvChannel> = getChannelList()
                            sortedChannelList.value.sortBy { it.displayNumber }
                            //todo Faisal
                            sortedChannelList.value.forEach { channelListItem ->
                                if (inputId == channelListItem!!.inputId) {
                                    selectedChannelList.add(channelListItem)
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channels are : ${channelListItem.name}")
                                }
                            }
                            if (selectedChannelList.size() > 0) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
                                if (selectedChannelList.size() == 1) {
                                    //only one channel, no action required
                                    callback?.onFailed(
                                        Error(
                                            FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                                            "Single Entry in List"
                                        )
                                    )
                                    return
                                }
                                playNextIndex(selectedChannelList, callback)
                                return
                            }
                        }
                    }
                    //Some issue, reset category to ALL
                    activeCategoryId = FilterItem.ALL_ID
                }

                override fun onFailed(error: Error?) {
                    //Failure, reset category to ALL
                    activeCategoryId = FilterItem.ALL_ID
                }
            })
        } else {
            try {
                newDesiredChannelIndex =
                    if (desiredChannelIndex >= getChannelList().size() - 1) 0 else (desiredChannelIndex + 1)

                if (getChannelList().get(newDesiredChannelIndex)!!.isSkipped) {
                    var numberOfSkipped = 0
                    do {
                        newDesiredChannelIndex =
                            if (newDesiredChannelIndex >= getChannelList().size() - 1) 0 else (newDesiredChannelIndex + 1)
                        numberOfSkipped++
                        if (numberOfSkipped == getChannelList().size()) {
                            break
                        }
                    } while (getChannelList().get(newDesiredChannelIndex)!!.isSkipped)
                    if (numberOfSkipped < getChannelList().size()) {
                        forceOptimizedChannelChange(
                            newDesiredChannelIndex,
                            false,
                            false,
                            callback!!
                        )
                    }
                } else {
                    forceOptimizedChannelChange(newDesiredChannelIndex, false, false, callback!!)
                }
            } catch (E: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "nextChannel: $E")
            }
        }
    }

    /**
     * Identify the next index to play for selected channel list and play it
     *
     * @param selectedChannelList Selected Channel list
     * @param callback callback AsyncReceiver
     */
    fun playNextIndex(selectedChannelList: GList<ReferenceTvChannel>, callback: AsyncReceiver?) {
        var newDesiredChannelIndex = 0
        //Get the details of currently playing channel
        var channel = getChannelList().get(desiredChannelIndex)
        var index = 0
        //Get the current channel position in selectedChannelList
        selectedChannelList.value.forEach { item ->
            if (channel!!.id == item.id) {
                //Update the new index for playing next channel
                newDesiredChannelIndex =
                    if (index >= selectedChannelList.size() - 1) 0 else (index + 1)
            }
            index++
        }

        if (selectedChannelList.getElement(newDesiredChannelIndex)!!.isSkipped) {
            var numberOfSkipped = 0
            do {
                newDesiredChannelIndex =
                    if (newDesiredChannelIndex >= selectedChannelList.size() - 1) 0 else (newDesiredChannelIndex + 1)
                numberOfSkipped++
                if (numberOfSkipped == selectedChannelList.size()) {
                    break
                }
            } while (selectedChannelList.getElement(newDesiredChannelIndex)!!.isSkipped)
            if (numberOfSkipped >= selectedChannelList.size()) {
                //only one channel, channel switch not required
                callback?.onFailed(
                    Error(
                        FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                        "Single Entry in List"
                    )
                )
                return
            }
        }

        //get the index for next channels from all channel list

        var playIndex = findChannelPosition(selectedChannelList.getElement(newDesiredChannelIndex)!!)
//        ReferenceSdk.sdkListener!!.runOnUiThread {
            forceOptimizedChannelChange(playIndex, false, false, callback!!)
//        }
    }

    override fun previousChannel(callback: AsyncReceiver?) {
        var newDesiredChannelIndex = 0
        if (activeCategoryId == FilterItem.FAVORITE_ID) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding previous channel for FavoriteGroup $activeFavGroupName")
            val favoriteChannelList: GList<ReferenceTvChannel> = GList()
            //Get all channels for selected favorite group
            (ReferenceSdk.favoriteHandler as ReferenceFavoriteHandler).getFavoritesForCategory(
                activeFavGroupName,
                object : AsyncDataReceiver<ArrayList<ReferenceFavoriteItem>> {
                    override fun onReceive(data: ArrayList<ReferenceFavoriteItem>) {
                        if (data.isEmpty()) {
                            //list is empty, reset the next/prev order to all list.
                            activeCategoryId = FilterItem.ALL_ID
                            return
                        } else if (data.size == 1) {
                            //only one channel, channel switch not required
                            callback?.onFailed(
                                Error(
                                    FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                                    "Single Entry in List"
                                )
                            )
                            return
                        }
                        //Sorting for Channel zapping works in sequence
                        try {
                            data.sortBy { it.tvChannel.displayNumber }
                        } catch (ex: Exception) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onReceive: display number missing")
                        }

                        data.forEach { favItem ->
                            favoriteChannelList.add(favItem.tvChannel)
                        }
                        playPrevIndex(favoriteChannelList, callback)
                    }

                    override fun onFailed(error: Error?) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "previousChannel - getFavoritesForCategory - onFailed")
                        //reset category to ALL
                        activeCategoryId = FilterItem.ALL_ID
                    }
                })
        } else if ((activeCategoryId == FilterItem.RADIO_CHANNELS_ID) || (activeCategoryId == FilterItem.TERRESTRIAL_TUNER_TYPE_ID)
            || (activeCategoryId == FilterItem.CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItem.SATELLITE_TUNER_TYPE_ID)
            || (activeCategoryId == FilterItem.RECENTLY_WATCHED_ID)
        ) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding previous channel for Category: $activeCategoryId")
            val selectedChannelList: GList<ReferenceTvChannel> = GList()

            //We are sorting the list so Channel +/- works properly
            var sortedChannelList: GList<ReferenceTvChannel> = getChannelList()
            sortedChannelList.value.sortBy { it.displayNumber }

            when (activeCategoryId) {
                FilterItem.RADIO_CHANNELS_ID -> {
                    sortedChannelList.value.forEach { tvChannel ->
                        if (tvChannel.isRadioChannel) {
                            selectedChannelList.add(tvChannel)
                        }
                    }
                }
                FilterItem.TERRESTRIAL_TUNER_TYPE_ID -> {
                    sortedChannelList.value.forEach { channel ->
                        if (channel.tunerType == ReferenceTvChannel.TERRESTRIAL_TUNER_TYPE) {
                            selectedChannelList.add(channel)
                        }
                    }
                }
                FilterItem.CABLE_TUNER_TYPE_ID -> {
                    sortedChannelList.value.forEach { channel ->
                        if (channel.tunerType == ReferenceTvChannel.CABLE_TUNER_TYPE) {
                            selectedChannelList.add(channel)
                        }
                    }
                }
                FilterItem.SATELLITE_TUNER_TYPE_ID -> {
                    sortedChannelList.value.forEach { channel ->
                        if (channel.tunerType == ReferenceTvChannel.SATELLITE_TUNER_TYPE) {
                            selectedChannelList.add(channel)
                        }
                    }
                }
                FilterItem.RECENTLY_WATCHED_ID -> {
                    var sortedPlaybleItems = ReferenceSdk.recentlyHandler!!.getRecentlyWatched()!!
                    if (sortedPlaybleItems != null && sortedPlaybleItems.size > 0)
                        sortedPlaybleItems.sortBy { (it.playableObject as ReferenceTvChannel).displayNumber }

                    for (playableItem in sortedPlaybleItems) {
                        if (playableItem.itemType == PlayableItemType.TV_CHANNEL) {
                            selectedChannelList.add(playableItem.playableObject as ReferenceTvChannel)
                        }
                    }
                }
            }
            //If list is empty, Reset the activeCategoryId to ALL
            if (selectedChannelList.size() == 0) {
                //list is empty, reset the next/prev order to all list.
                activeCategoryId = FilterItem.ALL_ID
                return
            } else if (selectedChannelList.size() == 1) {
                //only one channel, channel switch not required
                callback?.onFailed(
                    Error(
                        FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                        "Single Entry in List"
                    )
                )
                return
            }
            playPrevIndex(selectedChannelList, callback)
        } else if ((activeCategoryId >= FilterItem.TIF_INPUT_CATEGORY) && (ReferenceSdk.tvInputHandler != null)) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding previous channel for TIF Category: $activeCategoryId")
            ReferenceSdk.tvInputHandler!!.getTvInputList(object :
                AsyncDataReceiver<MutableList<TvInputInfo>> {
                override fun onReceive(tvInputInfos: MutableList<TvInputInfo>) {
                    val scannedInputs: MutableList<TvInputInfo> = mutableListOf()

                    for (tvInputInfo in tvInputInfos) {
                        var isInputScanned = false

                        getChannelList().value.forEach { channel ->
                            if (tvInputInfo.id.equals(channel.inputId, ignoreCase = true)) {
                                isInputScanned = true
                            }
                        }
                        if (isInputScanned) {
                            scannedInputs.add(tvInputInfo)
                        }
                    }
                    val inputIndex: Int = activeCategoryId - FilterItem.TIF_INPUT_CATEGORY
                    if (inputIndex < tvInputInfos.size && inputIndex >= 0) {
                        if (inputIndex >= 0) {
                            val tvInputInfo = scannedInputs[inputIndex]
                            val inputId = tvInputInfo.id
                            val selectedChannelList: GList<ReferenceTvChannel> = GList()

                            //We are sorting the list so Channel +/- works properly
                            var sortedChannelList: GList<ReferenceTvChannel> = getChannelList()
                            sortedChannelList.value.sortBy { it.displayNumber }

                            sortedChannelList.value.forEach { channelListItem ->
                                if (inputId == channelListItem!!.inputId) {
                                    selectedChannelList.add(channelListItem)
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channels are : ${channelListItem.name}")
                                }
                            }
                            if (selectedChannelList.size() > 0) {
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
                                if (selectedChannelList.size() == 1) {
                                    //only one channel, no action required
                                    callback?.onFailed(
                                        Error(
                                            FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                                            "Single Entry in List"
                                        )
                                    )
                                    return
                                }
                                playPrevIndex(selectedChannelList, callback)
                                return
                            }
                        }
                    }
                    //Some issue, reset category to ALL
                    activeCategoryId = FilterItem.ALL_ID
                }

                override fun onFailed(error: Error?) {
                    //Failure, reset category to ALL
                    activeCategoryId = FilterItem.ALL_ID
                }
            })
        } else {
            //ALL category
            try {
                newDesiredChannelIndex =
                    if (desiredChannelIndex <= 0) getChannelList().size() - 1 else desiredChannelIndex - 1
                if (getChannelList().get(newDesiredChannelIndex)!!.isSkipped) {
                    var numberOfSkipped = 0
                    do {
                        newDesiredChannelIndex =
                            if (newDesiredChannelIndex <= 0) getChannelList().size() - 1 else newDesiredChannelIndex - 1
                        numberOfSkipped++
                        if (numberOfSkipped == getChannelList().size()) {
                            break
                        }
                    } while (getChannelList().get(newDesiredChannelIndex)!!.isSkipped)
                    if (numberOfSkipped < getChannelList().size()) {
                        forceOptimizedChannelChange(
                            newDesiredChannelIndex,
                            false,
                            false,
                            callback!!
                        )
                    }
                } else {
                    forceOptimizedChannelChange(newDesiredChannelIndex, false, false, callback!!)
                }
            } catch (E: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "previousChannel: $E")
            }

        }
    }

    /**
     * Identify the previous index to play for selected channel list and play it
     *
     * @param selectedChannelList Selected Channel list
     * @param callback callback AsyncReceiver
     */
    fun playPrevIndex(selectedChannelList: GList<ReferenceTvChannel>, callback: AsyncReceiver?) {
        var newDesiredChannelIndex = 0
        //Get the details of currently playing channel
        var channel = getChannelList().get(desiredChannelIndex)
        var index = 0
        //Get the current channel position in selectedChannelList
        selectedChannelList.value.forEach { item ->
            if (channel!!.id == item.id) {
                //Update the new index for playing previous channel
                newDesiredChannelIndex =
                    if (index <= 0) (selectedChannelList.size() - 1) else (index - 1)
            }
            index++
        }

        if (selectedChannelList.getElement(newDesiredChannelIndex)!!.isSkipped) {
            var numberOfSkipped = 0
            do {
                newDesiredChannelIndex =
                    if (newDesiredChannelIndex <= 0) (selectedChannelList.size() - 1) else (newDesiredChannelIndex - 1)
                numberOfSkipped++
                if (numberOfSkipped == selectedChannelList.size()) {
                    break
                }
            } while (selectedChannelList.getElement(newDesiredChannelIndex)!!.isSkipped)
            if (numberOfSkipped >= selectedChannelList.size()) {
                //only one channel, channel switch not required
                callback?.onFailed(
                    Error(
                        FilterItem.ONLY_ONE_CHANNEL_IN_LIST,
                        "Single Entry in List"
                    )
                )
                return
            }
        }

        //get the index for next channels from all channel list
        var playIndex = findChannelPosition(selectedChannelList.getElement(newDesiredChannelIndex)!!)
//        ReferenceSdk.sdkListener!!.runOnUiThread {
            forceOptimizedChannelChange(playIndex, false, false, callback!!)
//        }

    }

    /**
     * Force optimized channel change
     *
     * @param index       index
     * @param forceUnlock force unlock
     * @param forceStop   force stop of active channel
     * @param callback    callback
     */
    fun forceOptimizedChannelChange(
        index: Int,
        forceUnlock: Boolean,
        forceStop: Boolean,
        callback: AsyncReceiver
    ) {
        smartChannelZapper!!.smartChannelZap(
            index,
            forceStop,
            object : GAndroidSmartChannelZapper.GFastChannelZapListener {
                override
                fun onChannelRequested(i: Int) {
                    desiredChannelIndex = i
                    callback.onSuccess()
                }

                override
                fun onTriggerChannelChange(i: Int) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTriggerChannelChange: ################# ZAPPER TV HANDLER CHANGE CHANNEL BY INDEX CALL $i")
                    changeChannelByIndex(i, object : AsyncReceiver {
                        override fun onFailed(error: Error?) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ################# ZAPPER TV HANDLER CHANGE CHANNEL BY INDEX CALL FAILED ${error!!.message}")
                            callback.onFailed(error)
                        }

                        override fun onSuccess() {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: ################# ZAPPER TV HANDLER CHANGE CHANNEL BY INDEX CALL SUCCESS ")
                            if (BuildConfig.FLAVOR.contains("mtk")) {
                                tunerManager!!.setTunerMode(i)
                            }

                            if (getChannelList().get(i)!!.isLocked && !(ReferenceSdk.playerHandler as ReferencePlayerHandler).isChannelUnlocked) {
                                InformationBus.submitEvent(Event(ReferenceEvents.ACTIVE_CHANNEL_LOCKED_EVENT))
                            } else {
                                InformationBus.submitEvent(Event(ReferenceEvents.ACTIVE_CHANNEL_UNLOCKED_EVENT))
                            }
                        }
                    })
                }

                override
                fun onTriggerPlayerStop() {
                    //TODO
                }
            })
    }

    override fun changeChannelByIndex(index: Int, callback: AsyncReceiver?) {

        if (index < 0 || index > getChannelList().size() - 1) {
            callback!!.onFailed(Error(500, "There is no channel with this index"))
        } else {

            var tempIndex = 0
            var tempChannel: TvChannel? = null
            getChannelList().value.forEach { tvChannel ->
                if (tempIndex == index) {
                    tempChannel = tvChannel
                }
                tempIndex++
            }

            var playableItem = PlayableItem(PlayableItemType.TV_CHANNEL, tempChannel!!)
            ReferenceSdk.playerHandler!!.play(playableItem, object : AsyncReceiver {
                override fun onSuccess() {
                    InformationBus.submitEvent(
                        PrefsStoreRequestEvent(
                            PrefsHandler.ACTIVE_CHANNEL_TAG,
                            index,
                            object : AsyncReceiver {
                                override fun onSuccess() {
                                    try {
                                        callback!!.onSuccess()

                                        tempIndex = 0
                                        var recentChannel: ReferenceTvChannel? = null

                                        getChannelList().value.forEach { tvChannel ->
                                            if (tempIndex == index) {
                                                recentChannel = tvChannel
                                            }
                                            tempIndex++
                                        }

                                        setRecentChannel(recentChannel!!.index)
                                    } catch (E: Exception) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: $E")
                                        callback!!.onFailed(Error(100, E.message.toString()))
                                    }

                                }

                                override fun onFailed(error: Error?) {
                                    callback!!.onFailed(error)
                                }
                            })
                    )
                }

                override fun onFailed(error: Error?) {
                    callback!!.onFailed(error)
                }
            })
        }
    }

    fun getChannelByDisplayNumber(displayNumber: String): ReferenceTvChannel? {
        getChannelList().value.forEach { item ->
            if(item.displayNumber.isDigitsOnly()){
                if (item.displayNumber == displayNumber) {
                    return item
                }
            }
            else {
                if (item.getDisplayNumberDigits() == displayNumber) {
                    return item
                }

            }
        }

        return null
    }

    fun getChannelListByDisplayNumber(displayNumberInput: String): MutableList<ReferenceTvChannel?> {
        val channelList = mutableListOf<ReferenceTvChannel?>()
        getChannelList().value.forEach { item ->
            if(item.displayNumber.isDigitsOnly()){
                if (item.displayNumber == displayNumberInput) {
                    channelList.add(item)
                }
            }
            else {
                val displayNumberChannel= item.getDisplayNumberDigits()

                //check digit by digit to see if channel display number is the same as input display number
                displayNumberInput.forEachIndexed { i, char ->
                    if (displayNumberChannel.length > i) {
                        if (displayNumberChannel.get(i) != char) {
                            return@forEach
                        } else {
                            if (i == displayNumberInput.length - 1) {
                                channelList.add(item)
                            }
                        }
                    }
                }
                channelList.sortBy {it!!.displayNumber}
            }
        }

        return channelList
    }

    override fun getChannelById(channelId: Int): ReferenceTvChannel? {
        getChannelList().value.forEach { channel ->
            if (channel.channelId.toInt() == channelId) {
                return channel
            }
        }
        return null
    }

    /**
     * Enable/disable lcn throughout the app
     */
    fun enableLcn(enableLcn: Boolean) {
        if (enableLcn) {
            if (checkLcnConflict()) {
                //TODO show settings lcn conflict
                return
            }
        }
        (dataProvider as TifDataProvider).lcnConfigUpdate()
    }

    fun updateDesiredChannelIndex() {
        var index = 0
        if (activeChannel != null) {
            run exitForEach@{
                getChannelList().value.forEach { channel ->
                    if (ReferenceTvChannel.compare(channel, activeChannel!!)) {
                        desiredChannelIndex = index
                        return@exitForEach
                    }
                    index++
                }
            }
        }
        ReferenceSdk.prefsHandler!!.storeValue(PrefsHandler.ACTIVE_CHANNEL_TAG, desiredChannelIndex)
    }

    /**
     * To track whether the currently playing channel is started from Favorite list/Recent list/Radio list etc.
     * It is used to identify the next/prev channel to play whenever user click next channel/prev channel keys in RCU.
     * @param categoryId currently active category list id
     * @param favGroupName currently selected favourite group name
     */
    fun updateLaunchOrigin(categoryId: Int, favGroupName: String = "") {
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "UpdateLaunchOrigin - categoryId is $categoryId and favGroupname is $favGroupName"
        )
        activeCategoryId = categoryId
        if (activeCategoryId == FilterItem.FAVORITE_ID) {
            activeFavGroupName = favGroupName
        }
    }

    private fun checkLcnConflict(): Boolean {
        var listSize = getChannelList().size()
        for (i in 0 until listSize) {
            var channel = getChannelList().get(i)
            if (channel != null) {
                getChannelList().value.forEach { item ->
                    if (!ReferenceTvChannel.compare(
                            channel,
                            item
                        ) && channel.displayNumber == item.displayNumber
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }
}


