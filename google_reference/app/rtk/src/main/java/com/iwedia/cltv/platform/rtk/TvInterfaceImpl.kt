package com.iwedia.cltv.platform.rtk

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.base.TvInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.rtk.provider.PlatformSpecificData
import com.iwedia.cltv.platform.rtk.util.GtvUtil
import com.realtek.system.RtkConfigs
import com.realtek.tv.RtkSettingConstants
import com.realtek.tv.TVMediaTypeConstants
import kotlinx.coroutines.Dispatchers


internal class TvInterfaceImpl (
    playerInterface: PlayerInterface,
    networkInterface: NetworkInterface,
    dataProvider: ChannelDataProviderInterface,
    tvInputInterface: TvInputInterface,
    utilsInterface: UtilsInterface,
    epgInterface: EpgInterface,
    context: Context,
    timeInterface: TimeInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) : TvInterfaceBaseImpl(playerInterface, networkInterface, dataProvider, tvInputInterface, utilsInterface, epgInterface, context, timeInterface, parentalControlSettingsInterface) {

    private val TAG = javaClass.simpleName
    private var isAnalogServiceStarted = false
    private var isDigitalFromAnalog = false
    private val PREFS_TAG = "LiveTVPrefs"
    private val KEY_PREFERRED_FIRST_CHANNEL_SET = "preferred_first_channel_set_from_mtk"
    private val KEY_PREFERRED_LOCKED_CHANNELS_SET = "preferred_locked_channels_set_from_mtk"

    private val activeChannelTag = "CurrentActiveChannel"
    private val activeChannelTagId = "CurrentActiveChannelId"
    private val DEFAULT_CHANNEL_ENABLE = "Default channel enable"
    private val DEFAULT_CHANNEL = "Default channel"
    private val PRELOADED_CHANNEL_ID = "PRELOADED_CHANNEL_ID"
    private val prefsRecentChannels = "RecentChannels"
    private val SKIPPED_CHANNELS_TAG = "skipped_channels"
    private var desiredChannelIndex = 0
    override var activeCategoryId = FilterItemType.ALL_ID
    private var activeFavGroupName = ""
    private var activeTifCategoryName = ""
    private var activeGenreCategoryName = ""
    override lateinit var playbackStatusInterface: PlaybackStatusInterface
    private var lastActiveChannel = mutableListOf<TvChannel>()

    init {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Initialized MTK High level TV interface")
    }

    override fun setupDefaultService(channels : List<TvChannel>, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.setupDefaultService(channels, applicationMode)
        } else {
            val firstChannelSet = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(KEY_PREFERRED_FIRST_CHANNEL_SET, false)
            if(!firstChannelSet) {
                var index = 0
                var newActiveChannel = -1

                if (newActiveChannel == -1) {
                    try {
                        index = 0
                        for (channel in channels) {
                            if(channel.isBrowsable){
                                newActiveChannel = index
                                break
                            }
                            index++
                        }
                    }catch (E: Exception){
                        println(E.message)
                    }
                }

                if (newActiveChannel != -1) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "setupDefaultService: Tag = ${newActiveChannel} - TagId - ${channels[newActiveChannel].id}")
                    utilsInterface.setPrefsValue(activeChannelTag, newActiveChannel)
                    utilsInterface.setPrefsValue(activeChannelTagId,channels[newActiveChannel].id)
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG,"Active channel activeChannel not found, outside of channel list size")
                }

                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit().putBoolean(KEY_PREFERRED_FIRST_CHANNEL_SET, true).apply()
            }
        }
    }


    override fun setup() {

    }


    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getActiveChannel(callback, applicationMode)
        } else {
            val activeChannel = utilsInterface.getPrefsValue(activeChannelTag, 0) as Int
            var channels = getChannelList()

            //When SDK removes some services from DB, activeChannel could be out of range of DB
            //fallback to data from MTK SDK
            if (activeChannel >= channels.size || activeChannel < 0) {
                if(channels.size == 0) {
                    callback.onFailed(Error("Active channel not found."))
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Active channel not found, list size iz 0")
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "getActiveChannel: tag = zero")
                    callback.onReceive(channels[0])
                    utilsInterface.setPrefsValue(activeChannelTag, 0)
                }
            } else {
                var channel = channels[activeChannel]
                callback.onReceive(channel)
            }
        }
    }

    override fun storeActiveChannel(tvChannel: TvChannel, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.storeActiveChannel(tvChannel, applicationMode)
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "storeActiveChannel: Tag = ${getChannelList(applicationMode).indexOf(tvChannel)} " +
                    "- tagId = ${tvChannel.id}")
            utilsInterface.setPrefsValue(activeChannelTag, getChannelList(applicationMode).indexOf(tvChannel))
            utilsInterface.setPrefsValue(activeChannelTagId, tvChannel.id)
        }
    }

    override fun isTvNavigationBlocked(applicationMode: ApplicationMode): Boolean {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.isTvNavigationBlocked(applicationMode)
        } else {
            return false
        }
    }

    override fun playPlayableItem(item : PlayableItem) {
        isAnalogServiceStarted = false
        isDigitalFromAnalog = false
        if(item is TvChannel && (item as TvChannel).platformSpecific == "directTuneChannel"){
            CoroutineHelper.runCoroutine({
                playerInterface.play(item)
            }, Dispatchers.Main)
        }else {
            super.playPlayableItem(item)
        }
    }

    override fun getChannelSourceType(tvChannel: TvChannel, applicationMode: ApplicationMode): String {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelSourceType(tvChannel, applicationMode)
        } else {
            return when (tvChannel.tunerType) {
                TunerType.TERRESTRIAL_TUNER_TYPE -> utilsInterface.getStringValue("antenna_type")
                TunerType.CABLE_TUNER_TYPE -> utilsInterface.getStringValue("cable")
                TunerType.SATELLITE_TUNER_TYPE -> utilsInterface.getStringValue("satellite")
                TunerType.ANALOG_TUNER_TYPE -> getAnalogTunerTypeName(tvChannel)
                TunerType.DEFAULT -> {
                    if(tvChannel.platformSpecific?.toString() != "directTuneChannel")
                        getTifChannelSourceLabel(tvChannel).substring(0,2)
                    else
                        ""
                }
                else -> ""
            }
        }
    }

    private fun getSignalType(ctx: Context): Int {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getSignalType")
        return Settings.Global.getInt(
            ctx.contentResolver,
            RtkSettingConstants.TV_SIGNAL_TYPE, TVMediaTypeConstants.TV_SIGNAL_TYPE_ANTENNA
        )
    }

    override fun getAnalogTunerTypeName(tvChannel: TvChannel, applicationMode: ApplicationMode) : String {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getAnalogTunerTypeName(tvChannel, applicationMode)
        } else {
            return when (getAnalogServiceListID(tvChannel)) {
                TunerType.TYPE_ANALOG_ANTENNA -> utilsInterface.getStringValue("analog_antenna")
                TunerType.TYPE_ANALOG_CABLE -> utilsInterface.getStringValue("analog_cable")
                else -> ""
            }
        }
    }

    override fun changeChannel(channel: TvChannel, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.changeChannel(channel, callback, applicationMode)
        } else {
            if((channel.platformSpecific?:"").toString() == "directTuneChannel"){
                playPlayableItem(channel)
            }else {
                var index = 0
                run exitForEach@{
                    getChannelList().forEach { tvChannel ->
                        if (tvChannel.channelId == channel.channelId) {
                            changeChannel(index, callback)
                            return@exitForEach
                        }
                        index++
                    }
                }
            }
        }
    }

    override fun changeChannel(index: Int, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.changeChannel(index, callback, applicationMode)
        } else {
            var channels = getChannelList(applicationMode)
            var index = if (index >= channels.size) 0 else index
            val playableItem = channels[index]
            playPlayableItem(playableItem)
            desiredChannelIndex = index
            storeActiveChannel(playableItem)
            storeLastActiveChannel(playableItem, applicationMode)
            callback.onSuccess()
        }
    }

    override fun getChannelList(applicationMode: ApplicationMode): ArrayList<TvChannel> {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelList(applicationMode)
        } else {
            var channelList = arrayListOf<TvChannel>()
            dataProvider.getChannelList().forEach {
                if (!it.isFastChannel() && it.platformSpecific is PlatformSpecificData
                    && (it.platformSpecific as PlatformSpecificData).isSiBrowsable) {
                    channelList.add(it)
                }
            }
            return channelList
        }
    }

    override fun getBrowsableChannelList(applicationMode: ApplicationMode): ArrayList<TvChannel> {
        val channelList = arrayListOf<TvChannel>()
        getChannelList(applicationMode).forEach { tvChannel ->
            if (tvChannel.isBrowsable) channelList.add(tvChannel)
        }
        return channelList
    }

    override fun getAnalogServiceListID(tvChannel: TvChannel, applicationMode: ApplicationMode): Int {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getAnalogServiceListID(tvChannel, applicationMode)
        } else {
            val curSignalType = getSignalType(context)
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getAnalogServiceListID: curSignalType = $curSignalType")
            var inputType = -1
            if (curSignalType == TVMediaTypeConstants.TV_SIGNAL_TYPE_CABLE) {
                inputType = TunerType.TYPE_ANALOG_CABLE
            } else if (curSignalType == TVMediaTypeConstants.TV_SIGNAL_TYPE_ANTENNA) {
                inputType = TunerType.TYPE_ANALOG_ANTENNA
            }
            return inputType
        }
    }

    override fun getLockedChannelListAfterOta(applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getLockedChannelListAfterOta(applicationMode)
        } else {
            val lockedChannelsSet = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(KEY_PREFERRED_LOCKED_CHANNELS_SET, false)
            //TODO
                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit().putBoolean(KEY_PREFERRED_LOCKED_CHANNELS_SET, true).apply()
            }
    }

    override fun getVisuallyImpairedAudioTracks(applicationMode: ApplicationMode): List<String> {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getVisuallyImpairedAudioTracks(applicationMode)
        } else {
            val soundtrackItem = ArrayList<String>()
            //TODO
            return soundtrackItem
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun addDirectTuneChannel(index: String, context: Context):TvChannel? {
        return if(index.toIntOrNull() != null && (2..69).contains(index.toInt()) && !isDVB()) {
            TvChannel(
                id = -2,
                displayNumber = index,
                name = "",
                inputId = GtvUtil.getCurrentInputID(context),
                platformSpecific = "directTuneChannel",
                tunerType = TunerType.DEFAULT
            )
        }else
            null

    }

    override fun initSkippedChannels(applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.initSkippedChannels(applicationMode)
        } else {
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
    }

    override fun isChannelLocked(channelId: Int, applicationMode: ApplicationMode): Boolean {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.isChannelLocked(channelId, applicationMode)
        } else {
            getChannelList().forEach { channel ->
                if (channel.channelId == channelId.toLong()) {
                    return channel.isLocked
                }
            }
            return false
        }
    }

    override fun getSelectedChannelList(
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode,
        filter: FilterItemType?,
        filterMetadata: String?
    ){
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getSelectedChannelList(callback, applicationMode, filter,filterMetadata)
        } else {
            CoroutineHelper.runCoroutine({
                var channelList = getChannelList()
                val favoriteChannelList: ArrayList<TvChannel> = ArrayList()
                val selectedChannelList: ArrayList<TvChannel> = ArrayList()

                /**
                 * @param filter is not null then we are using the passed filter to get channel list
                 * otherwise we would use default one. Same case for other variable also.
                 * @param activeFavGroupName
                 * @param activeTifCategoryName
                 * @param activeGenreCategoryName
                 */

                var activeCategoryId = filter ?: this.activeCategoryId
                var activeFavGroupName = this.activeFavGroupName
                var activeTifCategoryName =  this.activeTifCategoryName
                var activeGenreCategoryName = this.activeGenreCategoryName
                val isItEpgCategoryId = filter!=null

                if (filter!=null && filterMetadata!=null){
                    if (activeCategoryId == FilterItemType.FAVORITE_ID) {
                        activeFavGroupName = filterMetadata
                    } else if (activeCategoryId == FilterItemType.TIF_INPUT_CATEGORY) {
                        activeTifCategoryName = filterMetadata
                    } else if (activeCategoryId == FilterItemType.GENRE_ID) {
                        activeGenreCategoryName = filterMetadata
                    }
                }

                if (activeCategoryId == FilterItemType.FAVORITE_ID) {
                    //Get all channels for selected favorite group
                    channelList.forEach { tvChannel ->
                        if (tvChannel.favListIds.contains(activeFavGroupName)) {
                            favoriteChannelList.add(tvChannel)
                        }
                    }

                    if (favoriteChannelList.isEmpty()) {
                        //list is empty, reset the next order to all list.
                        this.activeCategoryId = FilterItemType.ALL_ID
                        callback.onFailed(Error("List is empty"))
                        return@runCoroutine
                    }
                    //Sorting data list by displayNumber so zapping on Favorite channel list should work in sequence.
                    dataProvider.getSortedChannelList(favoriteChannelList)

                    favoriteChannelList.forEach { tvChannel ->
                        selectedChannelList.add(tvChannel)
                    }

                    callback.onReceive(selectedChannelList)
                }
                else if ((activeCategoryId == FilterItemType.RADIO_CHANNELS_ID) || (activeCategoryId == FilterItemType.TERRESTRIAL_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.SATELLITE_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.RECENTLY_WATCHED_ID) || (activeCategoryId == FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID)
                ) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for Category: $activeCategoryId")
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()

                    //We are sorting the list so Channel +/- works properly
                    var sortedChannelList: ArrayList<TvChannel> = getChannelList()
                    dataProvider.getSortedChannelList(sortedChannelList)

                    when (activeCategoryId) {
                        FilterItemType.RADIO_CHANNELS_ID -> {
                            sortedChannelList.forEach { tvChannel ->
                                if (tvChannel.isRadioChannel) {
                                    selectedChannelList.add(tvChannel)
                                }
                            }
                        }
                        FilterItemType.TERRESTRIAL_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.CABLE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.CABLE_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.SATELLITE_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_ANTENNA) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_CABLE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }

                        FilterItemType.RECENTLY_WATCHED_ID -> {

                            var sortedPlaybleItems = getRecentlyWatched()
                            if (sortedPlaybleItems != null && sortedPlaybleItems.size > 0)
                                sortedPlaybleItems.sortBy { (it as TvChannel).displayNumber }

                            if (sortedPlaybleItems != null) {
                                for (playableItem in sortedPlaybleItems) {
                                    if (playableItem is TvChannel) {
                                        selectedChannelList.add(playableItem as TvChannel)
                                    }
                                }
                            }
                        }
                        else -> {

                        }
                    }

                    //If list is empty, Reset the activeCategoryId to ALL
                    if (selectedChannelList.size == 0) {
                        this.activeCategoryId = FilterItemType.ALL_ID
                        return@runCoroutine
                    }
                    callback.onReceive(selectedChannelList)
                }
                else if (activeCategoryId >= FilterItemType.TIF_INPUT_CATEGORY) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for TIF Category: $activeCategoryId, activeTifCategoryName: $activeTifCategoryName")

                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()

                    for (tvInputInfo in tvInputInterface.getTvInputManager().tvInputList) {
                        getChannelList().forEach { channel ->
                            if (tvInputInfo.id.equals(channel.inputId, ignoreCase = true)
                                && (tvInputInfo.loadLabel(context) as String).contentEquals(activeTifCategoryName)) {
                                selectedChannelList.add(channel)
                            }
                        }
                    }

                    if (selectedChannelList.size > 0) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
                        dataProvider.getSortedChannelList(selectedChannelList)
                        callback.onReceive(selectedChannelList)
                        return@runCoroutine
                    }

                    //Some issue, reset category to ALL
                    this.activeCategoryId = FilterItemType.ALL_ID
                }
                else if (activeCategoryId == FilterItemType.GENRE_ID) {
                    var selectedChannelList = java.util.ArrayList<TvChannel>()
                    getChannelList().forEach { channel ->
                        if (channel.genres.contains(activeGenreCategoryName)) {
                            selectedChannelList.add(channel)
                        }
                    }
                    if (selectedChannelList.size > 0) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
                        dataProvider.getSortedChannelList(selectedChannelList)
                        callback.onReceive(selectedChannelList)
                        return@runCoroutine
                    }

                    //Some issue, reset category to ALL
                    this.activeCategoryId = FilterItemType.ALL_ID
                }
                else if (activeCategoryId == FilterItemType.ALL_ID){
                    //if calling from epg, it should only suggest browsable channels
                    if (isItEpgCategoryId) {
                        val selectedChannelList: ArrayList<TvChannel> = ArrayList()
                        channelList.forEach {channel->
                            if (channel.isBrowsable)selectedChannelList.add(channel)
                        }
                        callback.onReceive(selectedChannelList)
                    } else
                        callback.onReceive(channelList)
                }
            })
        }
    }

    override fun nextChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.nextChannel(callback, applicationMode)
        } else {
            initSkippedChannels()
            CoroutineHelper.runCoroutine({
                utilsInterface.setPrefsValue("AUDIO_FIRST_LANGUAGE", "")
                utilsInterface.setPrefsValue("AUDIO_FIRST_TRACK_ID", "")
                var newDesiredChannelIndex = 0
                if (activeCategoryId == FilterItemType.FAVORITE_ID) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for FavoriteGroup $activeFavGroupName")
                    //Get the favorite channel list.
                    var channelList = getChannelList()
                    val favoriteChannelList: ArrayList<TvChannel> = ArrayList()
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()
                    //Get all channels for selected favorite group
                    channelList.forEach { tvChannel ->
                        if (tvChannel.favListIds.contains(activeFavGroupName)) {
                            favoriteChannelList.add(tvChannel)
                        }
                    }

                    if (favoriteChannelList.isEmpty()) {
                        //list is empty, reset the next order to all list.
                        activeCategoryId = FilterItemType.ALL_ID
                        return@runCoroutine
                    } else if (favoriteChannelList.size == 1) {
                        //only one channel, channel switch not required
                        callback.onFailed(Error("Single Entry in List"))
                        return@runCoroutine
                    }

//                //Sorting data list by displayNumber so zapping on Favorite channel list should work in sequence.
                    dataProvider.getSortedChannelList(favoriteChannelList)

                    favoriteChannelList.forEach { tvChannel ->
                        selectedChannelList.add(tvChannel)
                    }
                    playNextIndex(selectedChannelList, callback)
                } else if ((activeCategoryId == FilterItemType.RADIO_CHANNELS_ID) || (activeCategoryId == FilterItemType.TERRESTRIAL_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.SATELLITE_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.RECENTLY_WATCHED_ID) || (activeCategoryId == FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID)
                ) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for Category: $activeCategoryId")
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()

                    //We are sorting the list so Channel +/- works properly
                    var sortedChannelList: ArrayList<TvChannel> = getChannelList()
                    dataProvider.getSortedChannelList(sortedChannelList)

                    when (activeCategoryId) {
                        FilterItemType.RADIO_CHANNELS_ID -> {
                            sortedChannelList.forEach { tvChannel ->
                                if (tvChannel.isRadioChannel) {
                                    selectedChannelList.add(tvChannel)
                                }
                            }
                        }
                        FilterItemType.TERRESTRIAL_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.CABLE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.CABLE_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.SATELLITE_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_ANTENNA) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_CABLE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }

                        FilterItemType.RECENTLY_WATCHED_ID -> {

                            var sortedPlaybleItems = getRecentlyWatched()

                            if (sortedPlaybleItems != null && sortedPlaybleItems.size > 0)
                                sortedPlaybleItems.sortBy { (it as TvChannel).displayNumber }

                            if (sortedPlaybleItems != null) {
                                for (playableItem in sortedPlaybleItems) {
                                    if (playableItem is TvChannel && playableItem.isBroadcastChannel()) {
                                        selectedChannelList.add(playableItem as TvChannel)
                                    }
                                }
                            }
                        }
                        else -> {

                        }
                    }

                    //If list is empty, Reset the activeCategoryId to ALL
                    if (selectedChannelList.size == 0) {
                        activeCategoryId = FilterItemType.ALL_ID
                        return@runCoroutine
                    } else if (selectedChannelList.size == 1) {
                        //only one channel, channel switch not required
                        callback.onFailed(
                            Error("Single Entry in List")
                        )
                        return@runCoroutine
                    }
                    playNextIndex(selectedChannelList, callback)
                } else if (activeCategoryId >= FilterItemType.TIF_INPUT_CATEGORY) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for TIF Category: $activeCategoryId, activeTifCategoryName: $activeTifCategoryName")

                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()

                    for (tvInputInfo in tvInputInterface.getTvInputManager().tvInputList) {
                        getChannelList().forEach { channel ->
                            if (tvInputInfo.id.equals(channel.inputId, ignoreCase = true)
                                && (tvInputInfo.loadLabel(context) as String).contentEquals(activeTifCategoryName)) {
                                selectedChannelList.add(channel)
                            }
                        }
                    }

                    if (selectedChannelList.size > 0) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
                        if (selectedChannelList.size == 1) {
                            //only one channel, no action required
                            callback.onFailed(Error("Single Entry in List"))
                            return@runCoroutine
                        }
                        dataProvider.getSortedChannelList(selectedChannelList)
                        playNextIndex(selectedChannelList, callback)
                        return@runCoroutine
                    }

                    //Some issue, reset category to ALL
                    activeCategoryId = FilterItemType.ALL_ID
                } else if (activeCategoryId == FilterItemType.GENRE_ID) {
                    var selectedChannelList = java.util.ArrayList<TvChannel>()
                    getChannelList().forEach { channel ->
                        if (channel.genres.contains(activeGenreCategoryName)) {
                            selectedChannelList.add(channel)
                        }
                    }
                    if (selectedChannelList.size > 0) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
                        if (selectedChannelList.size == 1) {
                            //only one channel, no action required
                            callback.onFailed(Error("Single Entry in List"))
                            return@runCoroutine
                        }
                        dataProvider.getSortedChannelList(selectedChannelList)
                        playNextIndex(selectedChannelList, callback)
                        return@runCoroutine
                    }

                    //Some issue, reset category to ALL
                    activeCategoryId = FilterItemType.ALL_ID
                } else {
                    try {
                        var channelList = getChannelList()
                        if (desiredChannelIndex >= channelList.size - 1) {
                            newDesiredChannelIndex = 0
                        } else {
                            newDesiredChannelIndex = (desiredChannelIndex + 1)
                        }
                        var clSize = getChannelList().size
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
                            playPlayableItem(playableItem)
                            desiredChannelIndex = newDesiredChannelIndex
                            storeActiveChannel(playableItem)
                            storeLastActiveChannel(playableItem, applicationMode)
                            callback.onSuccess()
                        }
                    } catch (E: Exception) {
                        E.printStackTrace()
                    }
                }
            })
        }
    }

    override fun previousChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.previousChannel(callback, applicationMode)
        } else {
            initSkippedChannels()
            CoroutineHelper.runCoroutine({
                utilsInterface.setPrefsValue("AUDIO_FIRST_LANGUAGE", "")
                utilsInterface.setPrefsValue("AUDIO_FIRST_TRACK_ID", "")
                var newDesiredChannelIndex = 0
                if (activeCategoryId == FilterItemType.FAVORITE_ID) {

                    //Get all channels for selected favorite group
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for FavoriteGroup $activeFavGroupName")
                    //Get the favorite channel list.
                    var channelList = getChannelList()
                    val favoriteChannelList: ArrayList<TvChannel> = ArrayList()
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()
                    //Get all channels for selected favorite group
                    channelList.forEach { tvChannel ->
                        if (tvChannel.favListIds.contains(activeFavGroupName)) {
                            favoriteChannelList.add(tvChannel)
                        }
                    }

                    if (favoriteChannelList.isEmpty()) {
                        //list is empty, reset the next order to all list.
                        activeCategoryId = FilterItemType.ALL_ID
                        return@runCoroutine
                    } else if (favoriteChannelList.size == 1) {
                        //only one channel, channel switch not required
                        callback.onFailed(Error("Single Entry in List"))
                        return@runCoroutine
                    }

                    //Sorting data list by displayNumber so zapping on Favorite channel list should work in sequence.
                    dataProvider.getSortedChannelList(favoriteChannelList)

                    favoriteChannelList.forEach { tvChannel ->
                        selectedChannelList.add(tvChannel)
                    }
                    playPrevIndex(favoriteChannelList, callback)
                } else if ((activeCategoryId == FilterItemType.RADIO_CHANNELS_ID) || (activeCategoryId == FilterItemType.TERRESTRIAL_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.SATELLITE_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.RECENTLY_WATCHED_ID) || (activeCategoryId == FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID)
                ) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding previous channel for Category: $activeCategoryId")
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()

                    //We are sorting the list so Channel +/- works properly
                    var sortedChannelList: ArrayList<TvChannel> = getChannelList()
                    dataProvider.getSortedChannelList(sortedChannelList)

                    when (activeCategoryId) {
                        FilterItemType.RADIO_CHANNELS_ID -> {
                            sortedChannelList.forEach { tvChannel ->
                                if (tvChannel.isRadioChannel) {
                                    selectedChannelList.add(tvChannel)
                                }
                            }
                        }
                        FilterItemType.TERRESTRIAL_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.CABLE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.CABLE_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.SATELLITE_TUNER_TYPE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_ANTENNA) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }
                        FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG_CABLE) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }

                        FilterItemType.RECENTLY_WATCHED_ID -> {
                            var sortedPlaybleItems = getRecentlyWatched()
                            if (sortedPlaybleItems != null && sortedPlaybleItems.size > 0)
                                sortedPlaybleItems.sortBy { (it as TvChannel).displayNumber }

                            if (sortedPlaybleItems != null) {
                                for (playableItem in sortedPlaybleItems) {
                                    if (playableItem is TvChannel && playableItem.isBroadcastChannel()) {
                                        selectedChannelList.add(playableItem)
                                    }
                                }
                            }
                        }
                        else -> {

                        }
                    }

                    //If list is empty, Reset the activeCategoryId to ALL
                    if (selectedChannelList.size == 0) {
                        //list is empty, reset the next/prev order to all list.
                        activeCategoryId = FilterItemType.ALL_ID
                        return@runCoroutine
                    } else if (selectedChannelList.size == 1) {
                        //only one channel, channel switch not required
                        callback.onFailed(Error("Single Entry in List"))
                        return@runCoroutine
                    }
                    playPrevIndex(selectedChannelList, callback)
                } else if (activeCategoryId >= FilterItemType.TIF_INPUT_CATEGORY) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for TIF Category: $activeCategoryId")
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()

                    for (tvInputInfo in tvInputInterface.getTvInputManager().tvInputList) {
                        getChannelList().forEach { channel ->
                            if (tvInputInfo.id.equals(channel.inputId, ignoreCase = true)
                                && (tvInputInfo.loadLabel(context) as String).contentEquals(activeTifCategoryName)) {
                                selectedChannelList.add(channel)
                            }
                        }
                    }

                    if (selectedChannelList.size > 0) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
                        if (selectedChannelList.size == 1) {
                            //only one channel, no action required
                            callback.onFailed(Error("Single Entry in List"))
                            return@runCoroutine
                        }
                        dataProvider.getSortedChannelList(selectedChannelList)
                        playPrevIndex(selectedChannelList, callback)
                        return@runCoroutine
                    }
                    //Some issue, reset category to ALL
                    activeCategoryId = FilterItemType.ALL_ID
                } else if (activeCategoryId == FilterItemType.GENRE_ID) {
                    var selectedChannelList = java.util.ArrayList<TvChannel>()
                    getChannelList().forEach { channel ->
                        if (channel.genres.contains(activeGenreCategoryName)) {
                            selectedChannelList.add(channel)
                        }
                    }
                    if (selectedChannelList.size > 0) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Channel previous switch for $activeCategoryId")
                        if (selectedChannelList.size == 1) {
                            //only one channel, no action required
                            callback.onFailed(Error("Single Entry in List"))
                            return@runCoroutine
                        }
                        dataProvider.getSortedChannelList(selectedChannelList)
                        playPrevIndex(selectedChannelList, callback)
                        return@runCoroutine
                    }

                    //Some issue, reset category to ALL
                    activeCategoryId = FilterItemType.ALL_ID
                } else {
                    //ALL category
                    try {
                        var channelList = getChannelList()
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
                            playPlayableItem(playableItem)
                            desiredChannelIndex = newDesiredChannelIndex
                            storeActiveChannel(playableItem)
                            storeLastActiveChannel(playableItem, applicationMode)
                            callback.onSuccess()
                        }
                    } catch (E: Exception) {
                        E.printStackTrace()
                    }
                }
            })
        }
    }

    override fun getLastActiveChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getLastActiveChannel(callback, applicationMode)
        }
        else {
            val lastChannel = lastActiveChannel.get(0)
            changeChannel(lastChannel, callback, applicationMode)

        }
    }

    override fun getChannelById(channelId: Int, applicationMode: ApplicationMode): TvChannel? {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelById(channelId, applicationMode)
        } else {
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
    }

    override fun findChannelPosition(tvChannel: TvChannel, applicationMode: ApplicationMode): Int {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.findChannelPosition(tvChannel, applicationMode)
        } else
            return tvChannel.index
    }

    override fun playNextIndex(
        selectedChannelList: ArrayList<TvChannel>,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.playNextIndex(selectedChannelList, callback, applicationMode)
        } else {
            CoroutineHelper.runCoroutine({
                var newDesiredChannelIndex = 0
                var channel = getChannelList()[desiredChannelIndex]
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
                playPlayableItem(playableItem)
                storeActiveChannel(playableItem)
                storeLastActiveChannel(playableItem, applicationMode)
                callback.onSuccess()
            })
        }
    }

    override fun playPrevIndex(
        selectedChannelList: ArrayList<TvChannel>,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.playPrevIndex(selectedChannelList, callback, applicationMode)
        } else {
            CoroutineHelper.runCoroutine({
                var newDesiredChannelIndex = 0
                //Get the details of currently playing channel
                var channel = getChannelList()[desiredChannelIndex]
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
                playPlayableItem(playableItem)
                storeActiveChannel(playableItem)
                storeLastActiveChannel(playableItem, applicationMode)
                callback.onSuccess()
            })
        }
    }
    override fun getChannelByDisplayNumber(displayNumber: String, applicationMode: ApplicationMode): TvChannel? {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelByDisplayNumber(displayNumber, applicationMode)
        } else {
            var channels = dataProvider.getChannelList()
            channels.forEach { channel ->
                if (channel.displayNumber == displayNumber) {
                    return channel
                }
            }
            return null
        }
    }

    override fun updateDesiredChannelIndex(applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.updateDesiredChannelIndex(applicationMode)
        } else {
            desiredChannelIndex =
                utilsInterface.getPrefsValue(activeChannelTag, 0) as Int
        }
    }

    override fun updateLaunchOrigin(categoryId: Int, favGroupName: String, tifCategoryName: String, genreCategoryName: String, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.updateLaunchOrigin(categoryId, favGroupName, tifCategoryName, genreCategoryName, applicationMode)
        } else {
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
    }

    override fun getChannelByIndex(index: Int, applicationMode: ApplicationMode): TvChannel {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelByIndex(index, applicationMode)
        } else {
            var channels = getChannelList()
            return channels[index]
        }
    }

    override fun getChannelListAsync(callback: IAsyncDataCallback<ArrayList<TvChannel>>, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getChannelListAsync(callback, applicationMode)
        } else {
            CoroutineHelper.runCoroutine({
                callback.onReceive(getChannelList())
            })
        }
    }

    override fun getChannelListByCategories(callback: IAsyncDataCallback<ArrayList<TvChannel>>, entityCategory: FilterItemType?, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getChannelListByCategories(callback, entityCategory, applicationMode)
        } else {
            CoroutineHelper.runCoroutine({
                var channels: ArrayList<TvChannel> = arrayListOf()//dataProvider.getChannelList().removeAll()
                callback.onReceive(ArrayList(channels))
            })
        }
    }

    override fun nextChannelByCategory(categoryId: Int, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.nextChannelByCategory(categoryId, callback, applicationMode)
        } else {
            CoroutineHelper.runCoroutine({
                //not used by application
                var channels = getChannelList()
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
                storeActiveChannel(playableItem)
                storeLastActiveChannel(playableItem, applicationMode)
                callback.onSuccess()
            })
        }
    }

    override fun previousChannelByCategory(categoryId: Int, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.previousChannelByCategory(categoryId, callback, applicationMode)
        } else {
            CoroutineHelper.runCoroutine({
                //not used by application
                var channels = getChannelList()
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
                storeActiveChannel(playableItem)
                storeLastActiveChannel(playableItem, applicationMode)
                callback.onSuccess()
            })
        }
    }

    override fun setRecentChannel(channelIndex: Int, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.setRecentChannel(channelIndex, applicationMode)
        } else {
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
    }

    override fun storeLastActiveChannel(channel: TvChannel, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.storeLastActiveChannel(channel, applicationMode)
        } else {
            //lastActiveChannel is a list containing only two elements: current active channel and last active channel
            //it behaves like fifo stack- when active channel is added, first element at index 0 is removed
            lastActiveChannel.add(channel)

            //remove first item if size is more than 2
            if (lastActiveChannel.size > 2) {
                lastActiveChannel.removeAt(0)
            }
        }
    }


    override fun startInitialPlayback(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.startInitialPlayback(callback, applicationMode)
        } else {
            initSkippedChannels()
            var preloadedChannelId =
                utilsInterface.getPrefsValue(PRELOADED_CHANNEL_ID, -1) as Int
            if (preloadedChannelId != -1) {

                var tvChannel: TvChannel? = null
                try {
                    tvChannel = getChannelByIndex(preloadedChannelId)
                } catch (ex: NumberFormatException) {
                    getChannelList().forEach { existingChannel ->

                        if (similarity(
                                existingChannel.name,
                                preloadedChannelId.toString()
                            ) > 0.5f
                        ) {
                            tvChannel = existingChannel
                        }

                    }
                }

                changeChannel(tvChannel!!, callback)
            } else {
                val isDefaultChannelEnabled =
                    utilsInterface.getPrefsValue(DEFAULT_CHANNEL_ENABLE, false) as Boolean
                if (isDefaultChannelEnabled) {
                    var channelToPlay: TvChannel = getChannelList()[0]!!

                    var triplet = utilsInterface.getPrefsValue(
                        DEFAULT_CHANNEL,
                        ""
                    ) as String
                    if (triplet.isNotEmpty()) {
                        run exitForEach@{
                            getChannelList().forEach { channel ->
                                var temp =
                                    channel.onId.toString() + "|" + channel.tsId + "|" + channel.serviceId
                                if (temp == triplet) {
                                    channelToPlay = channel
                                }
                                return@exitForEach
                            }
                        }
                    }

                    changeChannel(channelToPlay, callback)
                } else {
                    val channels = getChannelList()
                    if(channels.size == 0) {
                        callback.onFailed(Error("Service list empty"))
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Service list is empty, sending error")
                        return
                    }

                    setupDefaultService(getChannelList())

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
                    changeChannel(activeChannel, callback)
                }
            }
        }
    }

    override fun lockUnlockChannel(
        tvChannel: TvChannel,
        lockUnlock: Boolean,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.lockUnlockChannel(tvChannel, lockUnlock, callback, applicationMode)
        } else {
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
                                playerInterface.isChannelUnlocked = false
                                playerInterface.playbackStatus.value = if (lockUnlock) {
                                    PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT
                                } else {
                                    PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT
                                }
                            }
                            callback.onSuccess()
                        }
                    } else {
                        callback.onFailed(Error("Some error while updating DB"))
                    }
                }

            })
        }

    }

    override fun skipUnskipChannel(tvChannel: TvChannel, skipUnskip: Boolean, applicationMode: ApplicationMode): Boolean {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.skipUnskipChannel(tvChannel, skipUnskip, applicationMode)
        } else {
            var skippedChannels = utilsInterface.getPrefsValue(SKIPPED_CHANNELS_TAG, "") as String
            if (skipUnskip) {
                skippedChannels += "," + tvChannel.channelId
                utilsInterface.setPrefsValue(SKIPPED_CHANNELS_TAG, skippedChannels)
                tvChannel.isSkipped = true
            } else {
                var skippedList = ""
                tvChannel.isSkipped = false
                getChannelById(tvChannel.channelId.toInt())?.isSkipped = false
                getChannelList().forEach { channel ->
                    if (channel.isSkipped) {
                        skippedList += channel.channelId.toString() + ","
                    }
                }
                utilsInterface.setPrefsValue(SKIPPED_CHANNELS_TAG, skippedList)
            }
            return true
        }
    }

    override fun getDesiredChannelIndex(applicationMode: ApplicationMode): Int {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getDesiredChannelIndex(applicationMode)
        } else
            return desiredChannelIndex
    }

    private fun findPosition(tvChannel: TvChannel): Int {
        var channelList = getChannelList()
        return channelList.indexOfFirst { it.channelId == tvChannel.channelId }
    }

    override fun getParentalRatingDisplayName(parentalRating: String?, applicationMode: ApplicationMode, tvEvent: TvEvent): String {
        var baseParentalRating = ""
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            baseParentalRating =
                super.getParentalRatingDisplayName(parentalRating, applicationMode, tvEvent)
        } else {
            getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {}

                override fun onReceive(data: TvChannel) {
                    if (data.id == tvEvent.tvChannel.id && utilsInterface.isCurrentEvent(tvEvent)) {
                        //TODO
                    }
                }
            }, applicationMode)
        }
        if (baseParentalRating.isEmpty())
            baseParentalRating =
                super.getParentalRatingDisplayName(parentalRating, applicationMode, tvEvent)
        return baseParentalRating
    }

    fun isDVB(): Boolean {
        return RtkConfigs.TvConfigs.TV_SYSTEM == RtkConfigs.TvConfigs.TvSystemConstants.DVB
    }
}