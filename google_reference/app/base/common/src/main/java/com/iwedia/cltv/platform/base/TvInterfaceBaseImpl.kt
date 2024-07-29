
package com.iwedia.cltv.platform.base

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.util.Log
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.base.content_provider.RecentlyProvider
import com.iwedia.cltv.platform.base.player.PlaybackStatusInterfaceBaseImpl
import com.iwedia.cltv.platform.model.*
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import kotlinx.coroutines.Dispatchers
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

open class TvInterfaceBaseImpl constructor
    (
    val playerInterface: PlayerInterface,
    networkInterface: NetworkInterface,
    val dataProvider: ChannelDataProviderInterface,
    val tvInputInterface: TvInputInterface,
    val utilsInterface: UtilsInterface,
    val epgModule: EpgInterface,
    val context: Context,
    val timeInterface: TimeInterface,
    val parentalControlSettingsInterface: ParentalControlSettingsInterface
) : TvInterface {

    private val activeChannelTag = "FastCurrentActiveChannel"
    private val activeChannelTagId = "FastCurrentActiveChannelId"
    private val previousActiveChannelTag = "FastPreviousActiveChannel"
    private val DEFAULT_CHANNEL_ENABLE = "Fast Default channel enable"
    private val DEFAULT_CHANNEL = "Fast Default channel"
    private val PRELOADED_CHANNEL_ID = "FAST_PRELOADED_CHANNEL_ID"
    private val TAG = javaClass.simpleName
    private val prefsRecentChannels = "FastRecentChannels"
    private val recentlyProvider: RecentlyProvider = RecentlyProvider(dataProvider)
    private val SKIPPED_CHANNELS_TAG = "fast_skipped_channels"
    private var desiredChannelIndex = 0
    override var activeCategoryId = FilterItemType.ALL_ID
    private var activeFavGroupName = ""
    private var activeTifCategoryName = ""
    private var activeGenreCategoryName = ""
     override lateinit var playbackStatusInterface: PlaybackStatusInterface

    inner class ChannelLoadedEventListener(var callback: IAsyncCallback?){

        private var eventListener: Any?= null
        init {
            InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.CHANNELS_LOADED), callback = {
                eventListener = it
            }, onEventReceived = {
                callback?.onSuccess()
                InformationBus.informationBusEventListener.unregisterEventListener(eventListener!!)
            })
        }
    }

    /**
     * TV input hash map
     */
    var inputMap: HashMap<String, TvInputInfo?>? = null
    private var eventListener: Any?= null
    init {
        recentlyProvider.setup()
        playbackStatusInterface = PlaybackStatusInterfaceBaseImpl( this, playerInterface, networkInterface, utilsInterface)

        inputMap = HashMap()
        for (input in tvInputInterface.getTvInputManager()!!.tvInputList) {
            val inputId = input.id
            inputMap!![inputId] = input
        }

        InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.ANOKI_RATING_LEVEL_CHANGED,
            Events.ANOKI_CHANNEL_LIST_REORDERED), callback = {
            eventListener = it
        }, onEventReceived = {
            if (it == Events.ANOKI_CHANNEL_LIST_REORDERED) {
                //Update active channel index
                var previousChannelName = utilsInterface.getPrefsValue(previousActiveChannelTag, "")
                val applicationMode = if(utilsInterface.getPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal) == ApplicationMode.DEFAULT.ordinal)  ApplicationMode.DEFAULT else  ApplicationMode.FAST_ONLY
                var previousChannelFound = false
                var eventListener = ChannelLoadedEventListener(object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                    }

                    override fun onSuccess() {
                        val channelList = getChannelList(applicationMode)

                        channelList.forEachIndexed { index, tvChannel ->
                            if (previousChannelName == tvChannel.name) {
                                desiredChannelIndex = index
                                previousChannelFound = true
                                utilsInterface.setPrefsValue(activeChannelTag, index)
                            }
                        }
                        InformationBus.informationBusEventListener.submitEvent(Events.ANOKI_CHANNEL_LIST_REORDER_FINISHED)
                        if ((!previousChannelFound) && (applicationMode == ApplicationMode.FAST_ONLY)) {
                            changeChannel(0, object: IAsyncCallback {
                                override fun onSuccess() {
                                }

                                override fun onFailed(error: Error) {
                                }
                            },applicationMode)
                        }
                    }
                })
            } else if (it == Events.ANOKI_RATING_LEVEL_CHANGED) {
                //Update active channel index
                var previousChannelName = utilsInterface.getPrefsValue(previousActiveChannelTag, "")
                val applicationMode = if(utilsInterface.getPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal) == ApplicationMode.DEFAULT.ordinal)  ApplicationMode.DEFAULT else  ApplicationMode.FAST_ONLY
                var previousChannelFound = false
                val channelList = getChannelList(applicationMode)
                channelList.forEachIndexed { index, tvChannel ->
                    if (previousChannelName == tvChannel.name) {
                        desiredChannelIndex = index
                        previousChannelFound = true
                        utilsInterface.setPrefsValue(activeChannelTag, index)
                    }
                }
                InformationBus.informationBusEventListener.submitEvent(Events.ANOKI_RATING_LEVEL_SET)
                if ((!previousChannelFound) && (applicationMode == ApplicationMode.FAST_ONLY)) {
                    changeChannel(0, object: IAsyncCallback {
                        override fun onSuccess() {
                        }

                        override fun onFailed(error: Error) {
                        }
                    },applicationMode)
                }
            }
        })
    }

    override fun initSkippedChannels(applicationMode: ApplicationMode) {
        val skippedChannels = utilsInterface.getPrefsValue(SKIPPED_CHANNELS_TAG, "") as String
        val skippedChannelsList = skippedChannels?.split(",")
        if (skippedChannelsList != null && skippedChannelsList.isNotEmpty()) {
            skippedChannelsList.forEach { channelId->
                if (channelId.isNotEmpty()) {
                    getChannelById(channelId.toInt())?.isSkipped = true
                }
            }
        }
    }


    override fun isChannelLocked(channelId: Int, applicationMode: ApplicationMode): Boolean {
        getChannelList(applicationMode).forEach { channel ->
            if (channel.channelId == channelId.toLong()) {
                return channel.isLocked
            }
        }
        return false
    }

    override fun getParentalRatingDisplayName(
        parentalRating: String?, applicationMode: ApplicationMode, tvEvent: TvEvent
    ): String {
        if (Constants.AnokiParentalConstants.USE_ANOKI_RATING_SYSTEM && !parentalRating.isNullOrEmpty() && tvEvent.tvChannel.isFastChannel()) {
            val level = tvInputInterface.getParentalRatingDisplayName(parentalRating, tvEvent).toInt()
            //Anoki rating level starts with value 1 (level - 1)
            return if(parentalControlSettingsInterface.getAnokiRatingList().size > 0) parentalControlSettingsInterface.getAnokiRatingList()[level - 1] else ""
        }
        return tvInputInterface.getParentalRatingDisplayName(parentalRating, tvEvent)
    }

    override fun setup() {
    }

    override fun setupDefaultService(channels : List<TvChannel>, applicationMode: ApplicationMode) {

    }

    override fun dispose() {
        InformationBus.informationBusEventListener.unregisterEventListener(eventListener!!)
    }

    override fun changeChannel(channel: TvChannel, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        var index = 0
        run exitForEach@{
            getChannelList(applicationMode).forEach { tvChannel ->
                if (tvChannel.name == channel.name) {
                    changeChannel(index, callback, applicationMode)
                    return@exitForEach
                }
                index++
            }
        }
    }

    override fun changeChannel(index: Int, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        var channels = getChannelList(applicationMode)
        var index = if (index >= channels.size) 0 else index
        val playableItem = channels[index]
        desiredChannelIndex = index
        storeActiveChannel(playableItem, applicationMode)
        playPlayableItem(playableItem)
        callback.onSuccess()
    }

    override fun getSelectedChannelList(
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode,
        filter: FilterItemType?,
        filterMetadata: String?
    ){
        CoroutineHelper.runCoroutine({
            var channelList = getChannelList(applicationMode)
            callback.onReceive(channelList)
        })
    }

    override fun nextChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        initSkippedChannels()
        CoroutineHelper.runCoroutine({
            utilsInterface.setPrefsValue("AUDIO_FIRST_LANGUAGE", "")
            utilsInterface.setPrefsValue("AUDIO_FIRST_TRACK_ID", "")
            var newDesiredChannelIndex = 0
            try {
                var channelList = getChannelList(applicationMode)
                if (desiredChannelIndex >= channelList.size - 1) {
                    newDesiredChannelIndex = 0
                } else {
                    newDesiredChannelIndex = (desiredChannelIndex + 1)
                }
                var clSize = getChannelList(applicationMode).size
                var iter = 0
                while (channelList.get(newDesiredChannelIndex)!!.isSkipped || (!channelList.get(newDesiredChannelIndex).isBrowsable && !utilsInterface.isThirdPartyChannel(channelList[newDesiredChannelIndex]))){
                    newDesiredChannelIndex += 1
                    iter ++

                    if (newDesiredChannelIndex >= clSize) {
                        newDesiredChannelIndex = 1
                    }
                    if (iter >= clSize) {
                        break
                    }
                }
                if (newDesiredChannelIndex < clSize) {
                    val playableItem = channelList[newDesiredChannelIndex]
                    desiredChannelIndex = newDesiredChannelIndex
                    storeActiveChannel(playableItem, applicationMode)
                    playPlayableItem(playableItem)
                    callback.onSuccess()
                }
            } catch (E: Exception) {
                E.printStackTrace()
            }
        })
    }

    override fun previousChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        initSkippedChannels()
        CoroutineHelper.runCoroutine({
            utilsInterface.setPrefsValue("AUDIO_FIRST_LANGUAGE", "")
            utilsInterface.setPrefsValue("AUDIO_FIRST_TRACK_ID", "")
            var newDesiredChannelIndex = 0
            //ALL category
            try {
                var channelList = getChannelList(applicationMode)
                if (desiredChannelIndex <= 0) {
                    newDesiredChannelIndex = channelList.size - 1
                } else {
                    newDesiredChannelIndex = (desiredChannelIndex - 1)
                }

                var clSize = channelList.size
                var iter = 0
                while (channelList.get(newDesiredChannelIndex)!!.isSkipped || (!channelList.get(newDesiredChannelIndex).isBrowsable && !utilsInterface.isThirdPartyChannel(channelList[newDesiredChannelIndex]))){
                    newDesiredChannelIndex -= 1
                    iter ++

                    if (newDesiredChannelIndex <= 0) {
                        newDesiredChannelIndex = clSize - 1
                    }
                    if (iter >= clSize) {
                        break
                    }
                }
                if (newDesiredChannelIndex < clSize) {
                    val playableItem = channelList[newDesiredChannelIndex]
                    desiredChannelIndex = newDesiredChannelIndex
                    storeActiveChannel(playableItem, applicationMode)
                    playPlayableItem(playableItem)
                    callback.onSuccess()
                }
            } catch (E: Exception) {
                E.printStackTrace()
            }
        })
    }

    override fun getLastActiveChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
    }

    override fun getChannelById(channelId: Int, applicationMode: ApplicationMode): TvChannel? {
        val channels = dataProvider.getChannelList()
        try {
            channels.forEach { channel ->
                if (channel.channelId.toInt() == channelId) {
                    return channel
                }
            }
        }catch (E: java.lang.Exception){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getChannelById: ${E.message}")
        }
        return null
    }

    override fun findChannelPosition(tvChannel: TvChannel, applicationMode: ApplicationMode): Int {
        return tvChannel.index
    }

    override fun playNextIndex(
        selectedChannelList: ArrayList<TvChannel>,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        CoroutineHelper.runCoroutine({
            var newDesiredChannelIndex = 0
            var channel = getChannelList(applicationMode)[desiredChannelIndex]
            var index = 0
            //Get the current channel position in selectedChannelList
            selectedChannelList.forEach { item ->
                if (channel!!.id == item.id) {
                    //Update the new index for playing next channel
                    newDesiredChannelIndex =
                        if (index >= selectedChannelList.size - 1) 0 else (index + 1)
                }
                index++
            }

            if (selectedChannelList[newDesiredChannelIndex]!!.isSkipped || (!selectedChannelList[newDesiredChannelIndex]!!.isBrowsable  && !utilsInterface.isThirdPartyChannel(selectedChannelList[newDesiredChannelIndex]))) {
                var numberOfSkipped = 0
                do {
                    newDesiredChannelIndex =
                        if (newDesiredChannelIndex >= selectedChannelList.size - 1) 0 else (newDesiredChannelIndex + 1)
                    numberOfSkipped++
                    if (numberOfSkipped == selectedChannelList.size) {
                        break
                    }
                } while (selectedChannelList[newDesiredChannelIndex]!!.isSkipped || (!selectedChannelList.get(newDesiredChannelIndex).isBrowsable  && !utilsInterface.isThirdPartyChannel(selectedChannelList[newDesiredChannelIndex])))
                if (numberOfSkipped >= selectedChannelList.size) {
                    //only one channel, channel switch not required
                    callback.onFailed(Error("Single Entry in List"))
                    return@runCoroutine
                }
            }

            val playableItem = selectedChannelList[newDesiredChannelIndex]
            desiredChannelIndex = findPosition(playableItem)
            storeActiveChannel(playableItem, applicationMode)
            playPlayableItem(playableItem)
            callback.onSuccess()
        })
    }

    override fun playPrevIndex(
        selectedChannelList: ArrayList<TvChannel>,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        CoroutineHelper.runCoroutine({
            var newDesiredChannelIndex = 0
            //Get the details of currently playing channel
            var channel = getChannelList(applicationMode)[desiredChannelIndex]
            var index = 0
            //Get the current channel position in selectedChannelList
            selectedChannelList.forEach { item ->
                if (channel!!.id == item.id) {
                    //Update the new index for playing previous channel
                    newDesiredChannelIndex =
                        if (index <= 0) (selectedChannelList.size - 1) else (index - 1)
                }
                index++
            }

            if (selectedChannelList[newDesiredChannelIndex]!!.isSkipped || (!selectedChannelList[newDesiredChannelIndex]!!.isBrowsable && !utilsInterface.isThirdPartyChannel(selectedChannelList[newDesiredChannelIndex]))) {
                var numberOfSkipped = 0
                do {
                    newDesiredChannelIndex =
                        if (newDesiredChannelIndex <= 0) (selectedChannelList.size - 1) else (newDesiredChannelIndex - 1)
                    numberOfSkipped++
                    if (numberOfSkipped == selectedChannelList.size) {
                        break
                    }
                } while (selectedChannelList[newDesiredChannelIndex]!!.isSkipped || (!selectedChannelList.get(newDesiredChannelIndex).isBrowsable && !utilsInterface.isThirdPartyChannel(selectedChannelList[newDesiredChannelIndex])))
                if (numberOfSkipped >= selectedChannelList.size) {
                    //only one channel, channel switch not required
                    callback.onFailed(Error("Single Entry in List"))
                    return@runCoroutine
                }
            }
            val playableItem = selectedChannelList[newDesiredChannelIndex]
            desiredChannelIndex = findPosition(playableItem)
            storeActiveChannel(playableItem, applicationMode)
            playPlayableItem(playableItem)
            callback.onSuccess()
        })
    }

    override fun getChannelByDisplayNumber(displayNumber: String, applicationMode: ApplicationMode): TvChannel? {
        var channels = getChannelList(applicationMode)
        channels.forEach { channel ->
            if (channel.displayNumber == displayNumber) {
                return channel
            }
        }
        return null
    }

    override fun enableLcn(enableLcn: Boolean, applicationMode: ApplicationMode) {
        //TODO
    }

    override fun updateDesiredChannelIndex(applicationMode: ApplicationMode) {
        desiredChannelIndex =
            utilsInterface.getPrefsValue(activeChannelTag, 0) as Int
    }

    override fun updateLaunchOrigin(categoryId: Int, favGroupName: String, tifCategoryName: String, genreCategoryName: String, applicationMode: ApplicationMode) {
        Log.d(Constants.LogTag.CLTV_TAG +
            TAG,
            "UpdateLaunchOrigin - categoryId is $categoryId and favGroupname is $favGroupName, tifCategoryName : $tifCategoryName"
        )
        activeCategoryId = FilterItemType.getFilterTypeById(categoryId)
        if (activeCategoryId == FilterItemType.FAVORITE_ID) {
            activeFavGroupName = favGroupName
        } else if (activeCategoryId == FilterItemType.TIF_INPUT_CATEGORY) {
            activeTifCategoryName = tifCategoryName
        } else if (activeCategoryId == FilterItemType.GENRE_ID) {
            activeGenreCategoryName = genreCategoryName
        }
    }

    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>, applicationMode: ApplicationMode) {
        var activeChannelId = utilsInterface.getPrefsValue(activeChannelTagId, 0) as Int
        val channel = getChannelById(activeChannelId, applicationMode)
        if(channel != null){
            callback.onReceive(channel)
        }else{
            callback.onFailed(Error("Active channel not found."))
        }
    }

    override fun getChannelByIndex(index: Int, applicationMode: ApplicationMode): TvChannel {
        var channels = getChannelList(applicationMode)
        return channels[index]
    }

    @Synchronized
    override fun getChannelList(applicationMode: ApplicationMode): ArrayList<TvChannel> {
        var channelList = arrayListOf<TvChannel>()
        val level = parentalControlSettingsInterface.getAnokiRatingLevel()
        dataProvider.getChannelList().forEach { tvChannel ->
            if (tvChannel.isFastChannel()) {
                if (tvChannel.providerFlag3 != null && parentalControlSettingsInterface.isAnokiParentalControlsEnabled()) {
                    if (tvChannel.providerFlag3!! <= level) {
                        channelList.add(tvChannel)
                    }
                } else {
                    channelList.add(tvChannel)
                }
            }
        }
        return channelList
    }

    override fun getBrowsableChannelList(applicationMode: ApplicationMode): ArrayList<TvChannel> {
        val channelList = arrayListOf<TvChannel>()
        getChannelList(applicationMode).forEach { tvChannel ->
            if (tvChannel.isBrowsable) channelList.add(tvChannel)
        }
        return channelList
    }

    override fun getChannelListAsync(callback: IAsyncDataCallback<ArrayList<TvChannel>>, applicationMode: ApplicationMode) {
        CoroutineHelper.runCoroutine({
            var channels = getChannelList(applicationMode)
            callback.onReceive(ArrayList(channels))
        })
    }

    override fun getChannelListByCategories(callback: IAsyncDataCallback<ArrayList<TvChannel>>, entityCategory: FilterItemType?, applicationMode: ApplicationMode) {
        CoroutineHelper.runCoroutine({
            var channels: ArrayList<TvChannel> = arrayListOf()//dataProvider.getChannelList().removeAll()
            callback.onReceive(ArrayList(channels))
        })
    }

    override fun nextChannelByCategory(categoryId: Int, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        CoroutineHelper.runCoroutine({
            //not used by application
            var channels = getChannelList(applicationMode)
            var activeChannel: TvChannel = channels[0]
            var filteredChannels = ArrayList<TvChannel>()
            var indexInFilter = 0
            getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    callback.onFailed(Error("Failed to get active channel"))
                }

                override fun onReceive(data: TvChannel) {
                    activeChannel = data
                }
            })
            channels.forEach { channel ->
                channel.categoryIds.forEach { it ->
                    if (it == categoryId) {
                        filteredChannels.add(channel)
                    }
                }
            }

            var index = 0
            filteredChannels.forEach {
                if (it.channelId == activeChannel.channelId) {
                    indexInFilter = index + 1
                }
                index++
            }
            if (indexInFilter > filteredChannels.size) {
                indexInFilter = 0
            }

            val playableItem = channels[filteredChannels[indexInFilter].index]
            playPlayableItem(playableItem)
            storeActiveChannel(playableItem,applicationMode)
            callback.onSuccess()
        })
    }

    override fun previousChannelByCategory(categoryId: Int, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        CoroutineHelper.runCoroutine({
            //not used by application
            var channels = getChannelList(applicationMode)
            var activeChannel: TvChannel = channels[0]
            var filteredChannels = ArrayList<TvChannel>()
            var indexInFilter = 0
            getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    callback.onFailed(Error("Failed to get active channel"))
                }

                override fun onReceive(data: TvChannel) {
                    activeChannel = data
                }
            })
            channels.forEach { channel ->
                channel.categoryIds.forEach { it ->
                    if (it == categoryId) {
                        filteredChannels.add(channel)
                    }
                }
            }

            var index = 0
            filteredChannels.forEach {
                if (it.channelId == activeChannel.channelId) {
                    indexInFilter = index - 1
                }
                index++
            }
            if (indexInFilter < 0) {
                indexInFilter = 0
            }

            val playableItem = channels[filteredChannels[indexInFilter].index]
            playPlayableItem(playableItem)
            storeActiveChannel(playableItem, applicationMode)
            callback.onSuccess()
        })
    }

    override fun setRecentChannel(channelIndex: Int, applicationMode: ApplicationMode) {
        val recentData = LinkedHashSet<String>(
            utilsInterface.
                getPrefsValue(prefsRecentChannels, LinkedHashSet<String>())!! as Set<String>
        )
        val recentCode = "${timeInterface.getCurrentTime()}:$channelIndex"
        if (recentData!!.size > 4) {
            recentData.remove(recentData.elementAt(4))
        }
        recentData.add(recentCode)

        utilsInterface.setPrefsValue(prefsRecentChannels, recentData)
    }

    override fun startInitialPlayback(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        initSkippedChannels()
        var preloadedChannelId =
            utilsInterface.getPrefsValue(PRELOADED_CHANNEL_ID, -1) as Int
        if (preloadedChannelId != -1) {

            var tvChannel: TvChannel? = null
            try {
                tvChannel = getChannelByIndex(preloadedChannelId)
            } catch (ex: NumberFormatException) {
                getChannelList(applicationMode).forEach { existingChannel ->

                    if (similarity(
                            existingChannel.name,
                            preloadedChannelId.toString()
                        ) > 0.5f
                    ) {
                        tvChannel = existingChannel
                    }

                }
            }

            changeChannel(tvChannel!!, callback, applicationMode)
        } else {
            val isDefaultChannelEnabled =
                utilsInterface.getPrefsValue(DEFAULT_CHANNEL_ENABLE, false) as Boolean
            if (isDefaultChannelEnabled) {
                var channelToPlay: TvChannel = getChannelList(applicationMode)[0]!!

                var triplet = utilsInterface.getPrefsValue(
                    DEFAULT_CHANNEL,
                    ""
                ) as String
                if (triplet.isNotEmpty()) {
                    run exitForEach@{
                        getChannelList(applicationMode).forEach { channel ->
                            var temp =
                                channel.onId.toString() + "|" + channel.tsId + "|" + channel.serviceId
                            if (temp == triplet) {
                                channelToPlay = channel
                            }
                            return@exitForEach
                        }
                    }
                }

                changeChannel(channelToPlay, callback,applicationMode)
            } else {
                val channels = getChannelList(applicationMode)
                if(channels.size == 0) {
                    callback.onFailed(Error("Service list empty"))
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Service list is empty, sending error")
                    return
                }

                setupDefaultService(getChannelList(applicationMode))

                var activeChannel =
                    utilsInterface.getPrefsValue(activeChannelTag, -1) as Int
                var activeChannelId =
                    utilsInterface.getPrefsValue(activeChannelTagId, -1) as Int
                var badNonBrowsableChannelId = false

                if(activeChannel >= channels.size) {
                    activeChannel = 0
                }

                if(activeChannel != -1) {
                    if((channels[activeChannel].id != activeChannelId) &&
                            !channels[activeChannel].isBrowsable) {
                        badNonBrowsableChannelId = true
                        Log.d(Constants.LogTag.CLTV_TAG + TAG,"Found bad non browsable channel id $activeChannelId")
                    }

                    if(activeChannelId == -1) {
                        badNonBrowsableChannelId = true
                        Log.d(Constants.LogTag.CLTV_TAG + TAG,"Found bad active channel id")
                    }
                }


                Log.d(Constants.LogTag.CLTV_TAG + TAG,"Active channel index  $activeChannel id $activeChannelId badNonBrowsableChannelId $badNonBrowsableChannelId")

                if(activeChannel == -1 || badNonBrowsableChannelId) {
                    if(activeChannel == -1) {
                        activeChannel = 0
                    }

                    //find first browsable channel because we don't want
                    // to end up on non-browsable Analog channel by default
                    for((index,channel) in channels.withIndex()) {
                        if(channel.isBrowsable) {
                            activeChannel = index
                            Log.d(Constants.LogTag.CLTV_TAG + TAG,"Setting active channel id to $index")
                            break
                        }
                    }
                }
                changeChannel(activeChannel, callback, applicationMode)
            }
        }
    }

    override fun addRecentlyWatched(playableItem: PlayableItem, applicationMode: ApplicationMode) {
        recentlyProvider.addRecentlyWatched(playableItem)
    }

    override fun getRecentlyWatched(applicationMode: ApplicationMode): MutableList<PlayableItem>? {
        return recentlyProvider.getRecentlyWatched()
    }

    override fun deleteChannel(tvChannel: TvChannel, applicationMode: ApplicationMode): Boolean {
        return dataProvider.deleteChannel(tvChannel)
    }

    override fun lockUnlockChannel(
        tvChannel: TvChannel,
        lockUnlock: Boolean,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        var activeChannel :TvChannel? =null
        getActiveChannel(object :IAsyncDataCallback<TvChannel>{
            override fun onFailed(error: Error) {
                callback.onFailed(Error("Active Channel Not found"))
            }

            override fun onReceive(data: TvChannel) {
                activeChannel=data
                if (dataProvider.lockUnlockChannel(tvChannel, lockUnlock)) {
                    tvChannel.isLocked = lockUnlock

                    //changing eventlist's channels lock-unclock status
                    epgModule.getEventListByChannel(tvChannel,object :IAsyncDataCallback<ArrayList<TvEvent>>{
                        override fun onFailed(error: Error) {}

                        override fun onReceive(data: ArrayList<TvEvent>) {
                            data.forEach {
                                it.tvChannel.isLocked = lockUnlock
                            }
                        }
                    })

                    if(activeChannel!=null) {
                        if (activeChannel!!.id == tvChannel.id) {
                            playerInterface.playbackStatus.value = if (lockUnlock) {
                                PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT
                            } else {
                                PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT
                            }
                        }
                        callback.onSuccess()
                    }
                }
                callback.onFailed(Error("Some error while updating DB"))
            }

        },applicationMode)
    }

    override fun isChannelLockAvailable(tvChannel: TvChannel, applicationMode: ApplicationMode): Boolean {
        return dataProvider.isChannelLockAvailable(tvChannel)
    }

    override fun skipUnskipChannel(tvChannel: TvChannel, skipUnskip: Boolean, applicationMode: ApplicationMode): Boolean {
        var skippedChannels = utilsInterface.getPrefsValue(SKIPPED_CHANNELS_TAG, "") as String
        if (skipUnskip) {
            skippedChannels += "," + tvChannel.channelId
           utilsInterface.setPrefsValue(SKIPPED_CHANNELS_TAG, skippedChannels)
            tvChannel.isSkipped = true
        } else {
            var skippedList = ""
            tvChannel.isSkipped = false
            getChannelById(tvChannel.channelId.toInt())?.isSkipped = false
            getChannelList(applicationMode).forEach { channel ->
                if (channel.isSkipped) {
                    skippedList += channel.channelId.toString() + ","
                }
            }
            utilsInterface.setPrefsValue(SKIPPED_CHANNELS_TAG, skippedList)
        }
        return true
    }

    override fun getDesiredChannelIndex(applicationMode: ApplicationMode): Int {
        if (desiredChannelIndex >= 0 && desiredChannelIndex < getChannelList(applicationMode).size)
            return desiredChannelIndex
        else
            return 0
    }

    /**
     * Get tv input list
     *
     * @param receiver receiver
     */
    override fun getTvInputList(receiver: IAsyncDataCallback<MutableList<TvInputInfo>>, applicationMode: ApplicationMode) {
        val tvInputInfoList: MutableList<TvInputInfo> = mutableListOf()

        inputMap?.let {
            for (inputInfo in inputMap!!.values) {
                if (tvInputInterface.getTvInputManager().getInputState(inputInfo!!.id) == TvInputManager.INPUT_STATE_CONNECTED) {
                    tvInputInfoList.add(inputInfo)
                }
            }
        }
        receiver.onReceive(tvInputInfoList)
    }

    override fun isParentalEnabled(applicationMode: ApplicationMode) : Boolean {
        return tvInputInterface.isParentalEnabled()
    }

    override fun isTvNavigationBlocked(applicationMode: ApplicationMode): Boolean {
        return false
    }

    override fun getTifChannelSourceLabel(tvChannel: TvChannel, applicationMode: ApplicationMode): String {
        var tifSourceName = ""

        getTvInputList(object : IAsyncDataCallback<MutableList<TvInputInfo>> {
            override fun onReceive(tvInputInfos: MutableList<TvInputInfo>) {
                for (tvInputInfo in tvInputInfos) {
                    if (tvInputInfo.id.equals(tvChannel.inputId, ignoreCase = true)) {
                        tifSourceName =
                            tvInputInfo.loadLabel(context) as String
                    }
                }
            }

            override fun onFailed(error: kotlin.Error) {}
        })

        return tifSourceName
    }

    override fun getChannelSourceType(tvChannel: TvChannel, applicationMode: ApplicationMode): String {
        return ""
    }

    override fun getAnalogTunerTypeName(tvChannel: TvChannel, applicationMode: ApplicationMode): String {
        return ""
    }

    override fun getAnalogServiceListID(tvChannel: TvChannel, applicationMode: ApplicationMode): Int {
        return 1
    }

    override fun storeActiveChannel(tvChannel: TvChannel, applicationMode: ApplicationMode) {
        utilsInterface.setPrefsValue(activeChannelTag, getChannelList(applicationMode).indexOf(tvChannel))
        utilsInterface.setPrefsValue(activeChannelTagId, tvChannel.id)
        utilsInterface.setPrefsValue(previousActiveChannelTag, tvChannel.name)
    }

    override fun storeLastActiveChannel(channel: TvChannel, applicationMode: ApplicationMode) {
    }

    protected open fun playPlayableItem(item : PlayableItem) {
        CoroutineHelper.runCoroutine({
            playerInterface.play(item)
            addRecentlyWatched(item)
        }, Dispatchers.Main)
    }


    //TODO move this to Utils module
    protected fun similarity(s1: String, s2: String): Double {
        var longer = s1.lowercase()
        var shorter = s2.lowercase()
        if (s1.length < s2.length) {
            longer = s2
            shorter = s1
        }
        val longerLength = longer.length
        return if (longerLength == 0) {
            1.0 /* both strings have zero length */
        } else (longerLength - getLevenshteinDistance(
            longer,
            shorter
        )) / longerLength.toDouble()
    }

    /**
     * LevenshteinDistance
     * copied from https://commons.apache.org/proper/commons-lang/javadocs/api-2.5/src-html/org/apache/commons/lang/StringUtils.html#line.6162
     */
    private fun getLevenshteinDistance(s: String?, t: String?): Int {
        var s = s
        var t = t
        require(!(s == null || t == null)) { "Strings must not be null" }
        var n = s.length // length of s
        var m = t.length // length of t
        if (n == 0) {
            return m
        } else if (m == 0) {
            return n
        }
        if (n > m) {
            // swap the input strings to consume less memory
            val tmp: String = s
            s = t
            t = tmp
            n = m
            m = t.length
        }
        var p = IntArray(n + 1) //'previous' cost array, horizontally
        var d = IntArray(n + 1) // cost array, horizontally
        var _d: IntArray //placeholder to assist in swapping p and d

        // indexes into strings s and t
        var i: Int // iterates through s
        var j: Int // iterates through t
        var t_j: Char // jth character of t
        var cost: Int // cost
        i = 0
        while (i <= n) {
            p[i] = i
            i++
        }
        j = 1
        while (j <= m) {
            t_j = t[j - 1]
            d[0] = j
            i = 1
            while (i <= n) {
                cost = if (s[i - 1] == t_j) 0 else 1
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost)
                i++
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p
            p = d
            d = _d
            j++
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n]
    }

    private fun findPosition(tvChannel: TvChannel): Int {
        var channelList = getChannelList(ApplicationMode.FAST_ONLY)
        return channelList.indexOfFirst { it.channelId == tvChannel.channelId }
    }

    override fun isLcnEnabled(applicationMode: ApplicationMode): Boolean {
        return dataProvider.isLcnEnabled()
    }

    override fun getLockedChannelListAfterOta(applicationMode: ApplicationMode) {

    }

    override fun getVisuallyImpairedAudioTracks(applicationMode: ApplicationMode): List<String> {
        return arrayListOf()
    }

    override fun isChannelSelectable(channel : TvChannel) : Boolean {
        return true
    }

    override fun forceChannelsRefresh(applicationMode: ApplicationMode) {

    }

    override fun isSignalAvailable(): Boolean {
        return playbackStatusInterface.isSignalAvailable
    }

    override fun isChannelsAvailable(): Boolean {
        return playbackStatusInterface.isChannelsAvailable
    }

    override fun isWaitingChannel(): Boolean {
        return playbackStatusInterface.isWaitingChannel
    }

    override fun isPlayerTimeout(): Boolean {
        return playbackStatusInterface.isPlayerTimeout
    }

    override fun isNetworkAvailable(): Boolean {
        return playbackStatusInterface.isNetworkAvailable
    }

    override fun appJustStarted(): Boolean {
        return playbackStatusInterface.appJustStarted
    }
    override  fun addDirectTuneChannel(index: String, context: Context):TvChannel?{
        return null
    }

    override fun checkAndRunBarkerChannel(run : Boolean) {

    }
}