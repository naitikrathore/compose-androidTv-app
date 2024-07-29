package com.iwedia.cltv.anoki_fast.epg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.manager.PvrBannerSceneManager
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.FastFavoriteInterface
import com.iwedia.cltv.platform.`interface`.FastUserSettingsInterface
import com.iwedia.cltv.platform.`interface`.ForYouInterface
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.fast_backend_utils.FastTosOptInHelper
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.home_scene.HomeSceneData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import world.SceneManager
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object FastLiveTabDataProvider: TTSSetterForSelectableViewInterface, TTSSetterInterface {
    // Page cache for improve performance
    private const val IS_EVENT_LIST_PAGE_CACHE_ENABLED = false

    val TAG = "FastLiveTabDataProvider"
    var tvModule: TvInterface?= null
    var playerModule: PlayerInterface?= null
    var epgModule: EpgInterface?= null
    lateinit var forYouModule : ForYouInterface // TODO BORIS this is not needed here - it's only used to fetch data for the Vod screen (mockuping data for the rails) - delete this argument once VOD is separated from the app
    var utilsModule: UtilsInterface?= null
    lateinit var textToSpeechModule: TTSInterface
    var parentalControlSettingsModule: ParentalControlSettingsInterface?= null
    var categoryModule: CategoryInterface?= null
    var fastFavoriteInterface: FastFavoriteInterface?= null
    var fastUserSettingsModule: FastUserSettingsInterface?= null
    var networkModule: NetworkInterface?= null
    var pvrModule: PvrInterface? = null
    var timeShiftModule: TimeshiftInterface? = null
    //contains the map of channel events which are loaded in last call
    var channelListForSelectedFilter = mutableListOf<TvChannel>()
    var filterList: ArrayList<Category>? = null
    private var filterTimer: CountDownTimer?= null

    //list of channels added for the fav
    private var favoriteList = arrayListOf<TvChannel>()
    //Fast live tab cache
    private var cache = HashMap<String, java.util.LinkedHashMap<Int, MutableList<TvEvent>>>()
    private lateinit var internetCallback: (hasInternet: Boolean)->Unit
    private lateinit var anokiServerCallback: (serverStatus: Boolean)->Unit
    var fastLiveTab: FastLiveTab? = null
    var activeGenre: String?= ""
    private var tosAccepted = false

    fun init(
        tvInterface: TvInterface,
        epgInterface: EpgInterface,
        utilsInterface: UtilsInterface,
        parentalControlSettingsModule: ParentalControlSettingsInterface,
        categoryInterface: CategoryInterface,
        fastFavoriteInterface: FastFavoriteInterface,
        networkInterface: NetworkInterface,
        playerModule: PlayerInterface,
        textToSpeechModule: TTSInterface,
        fastUserSettingsInterface: FastUserSettingsInterface,
        pvrInterface: PvrInterface,
        timeshiftInterface: TimeshiftInterface,
        forYouModule: ForYouInterface // TODO BORIS this is not needed here - it's only used to fetch data for the Vod screen (mockuping data for the rails) - delete this argument once VOD is separated from the app
    ) {
        this.forYouModule = forYouModule // TODO BORIS this is not needed here - it's only used to fetch data for the Vod screen (mockuping data for the rails) - delete this argument once VOD is separated from the app
        this.tvModule = tvInterface
        this.playerModule = playerModule
        this.epgModule = epgInterface
        this.utilsModule = utilsInterface
        this.textToSpeechModule = textToSpeechModule
        this.parentalControlSettingsModule = parentalControlSettingsModule
        this.categoryModule = categoryInterface
        this.fastFavoriteInterface = fastFavoriteInterface
        this.networkModule = networkInterface
        this.fastUserSettingsModule = fastUserSettingsInterface
        this.pvrModule = pvrInterface
        this.timeShiftModule = timeshiftInterface
        ReferenceApplication.applicationContext().registerReceiver(receiver, IntentFilter(FastZapBannerDataProvider.FAST_SHOW_GUIDE_INTENT))
        if (!(BuildConfig.FLAVOR.contains("base") && !BuildConfig.FLAVOR.contains("mal_service"))) {
            ReferenceApplication.runOnUiThread{
                this.networkModule?.networkStatus?.observeForever { networkStatusData ->
                    if (networkStatusData == NetworkData.NoConnection) {
                        if (::internetCallback.isInitialized) {
                            internetCallback.invoke(false)
                        }
                    } else {
                        if (::internetCallback.isInitialized) {
                            internetCallback.invoke(true)
                        }
                    }
                }

                this.networkModule?.anokiServerStatus?.observeForever { anokiServerStatus ->
                    if (anokiServerStatus) {
                        if (::anokiServerCallback.isInitialized) {
                            anokiServerCallback.invoke(true)
                        }
                    } else {
                        if (::anokiServerCallback.isInitialized) {
                            anokiServerCallback.invoke(false)
                        }
                    }
                }
            }
        }
        FastTosOptInHelper.fetchTosOptInFromServer(ReferenceApplication.applicationContext()) {
            tosAccepted = it == 1
        }
    }

    fun setInternetCallback(callback: (hasInternet: Boolean)-> Unit) {
        this.internetCallback = callback
    }

    fun setAnokiServerStatusCallback(callback: (anokiServerStatus: Boolean)-> Unit) {
        this.anokiServerCallback = callback
    }

    fun hasInternet():Boolean {
        if (networkModule!!.networkStatus.isInitialized)
            return if (networkModule!!.networkStatus.value == NetworkData.NoConnection) false else true
        else
            return false
    }

    fun isRegionSupported(): Boolean {
        return fastUserSettingsModule!!.isRegionSupported()
    }

    fun setup() {
        getFavorites()
        //Cache all filter data
        getAvailableFilters { filters ->
            if (filters != null) {
                getActiveChannel { tvChannel ->
                    getEventsForChannels(
                        tvChannel,
                        0,
                        {
                        }, 0, 0, false)
                }
            }
        }
    }

    fun getActiveFilter() = categoryModule!!.getActiveEpgFilter(ApplicationMode.FAST_ONLY)

    fun setActiveFilter(filter: Int) {
        categoryModule?.setActiveEpgFilter(filter, ApplicationMode.FAST_ONLY)
    }

    fun getActiveCategoryName() = categoryModule!!.getActiveCategory(ApplicationMode.FAST_ONLY)

    fun onChannelClickedFromTalkback(tvChannel: TvChannel){
        //Set application mode 1 for FAST ONLY mode
        utilsModule!!.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
        tvModule!!.changeChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {}

            override fun onSuccess() {
            }
        }, ApplicationMode.FAST_ONLY)

        CoroutineScope(Dispatchers.Main).launch {
            val intentHide = Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
            ReferenceApplication.applicationContext().sendBroadcast(intentHide)
            val intentShow = Intent(FastZapBannerDataProvider.FAST_SHOW_ZAP_BANNER_INTENT)
            ReferenceApplication.applicationContext().sendBroadcast(intentShow)
            ReferenceApplication.worldHandler?.triggerAction(ReferenceWorldHandler.SceneId.HOME_SCENE, SceneManager.Action.HIDE) // HIDE is used because whole Home Scene should stay in memory in order to enable it's fast accessing when pressing back from LiveScene
        }
        BackFromPlayback.resetKeyPressedState()
    }

    fun onTvEventClicked(tvEvent: TvEvent, zapCallback: ()->Unit) {
        val applicationMode = utilsModule!!.getApplicationMode()
        //Set application mode 1 for FAST ONLY mode
        utilsModule!!.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
        getActiveChannel {activeTvChannel ->
            if (activeTvChannel.displayNumber != tvEvent?.tvChannel?.displayNumber ||
                applicationMode != ApplicationMode.FAST_ONLY.ordinal) {
                tvModule!!.changeChannel(tvEvent.tvChannel, object : IAsyncCallback {
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {
                        zapCallback.invoke()
                    }
                }, ApplicationMode.FAST_ONLY)
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            val intentHide = Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
            ReferenceApplication.applicationContext().sendBroadcast(intentHide)
            val intentShow = Intent(FastZapBannerDataProvider.FAST_SHOW_ZAP_BANNER_INTENT)
            ReferenceApplication.applicationContext().sendBroadcast(intentShow)
            ReferenceApplication.worldHandler?.triggerAction(ReferenceWorldHandler.SceneId.HOME_SCENE, SceneManager.Action.HIDE) // HIDE is used because whole Home Scene should stay in memory in order to enable it's fast accessing when pressing back from LiveScene
        }
        BackFromPlayback.resetKeyPressedState()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getActiveCategoryIndex(callback: (index: Int)->Unit) {
        getActiveGenre { activeGenre->
            getAvailableFilters {
                var index = 0
                it?.forEachIndexed { i, category ->
                    if (FastLiveTabDataProvider.activeGenre == category.name) {
                        index = i
                    }
                }
                callback(index)
            }
        }
    }

    /**
     * Checks if the given [tvEvent] is on the active TV channel.
     *
     * @param tvEvent The [TvEvent] to check.
     * @param onMatch A lambda to execute when the condition is met.
     */
    fun isEventOnActiveChannel(tvEvent: TvEvent?, onMatch: () -> Unit) {
        getActiveChannel {activeTvChannel ->
            if (activeTvChannel.displayNumber == tvEvent?.tvChannel?.displayNumber) {
                onMatch.invoke()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun getActiveChannel(callback: (tvChannel: TvChannel)-> Unit) {
        val channelList = tvModule!!.getChannelList(ApplicationMode.FAST_ONLY)
        if (channelList.isNotEmpty()) {
            var activeChannel = tvModule!!.getChannelList(ApplicationMode.FAST_ONLY)[tvModule!!.getDesiredChannelIndex(ApplicationMode.FAST_ONLY)]
            callback.invoke(activeChannel)
        }
    }

    fun getParentalRatingDisplayName(tvEvent: TvEvent?): String {
        if (tvEvent == null) return ""
        return tvModule!!.getParentalRatingDisplayName(tvEvent.parentalRating,ApplicationMode.FAST_ONLY, tvEvent)
    }

    fun getAvailableAudioTracks(): MutableList<IAudioTrack>? {
        return playerModule?.getAudioTracks() as MutableList<IAudioTrack>?
    }

    fun getAvailableSubtitleTracks(): MutableList<ISubtitle>? {
        return playerModule?.getSubtitleTracks(ApplicationMode.FAST_ONLY) as MutableList<ISubtitle>?    }

    fun isParentalEnabled(): Boolean {
        return tvModule!!.isParentalEnabled(ApplicationMode.FAST_ONLY)
    }

    fun isAccessibilityEnabled(): Boolean{
        return utilsModule!!.isAccessibilityEnabled()
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text, importance = importance)
    }

    override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
        textToSpeechModule.setSpeechTextForSelectableView(text = text,importance = importance, type = type, isChecked = isChecked)
    }

    fun getChannelsOfSelectedFilter(): MutableList<TvChannel> {
        return channelListForSelectedFilter
    }

    fun onFilterSelected(filterId:Int,timerDuration: Long,callback: (map: java.util.LinkedHashMap<Int, MutableList<TvEvent>>)-> Unit) {
        filterTimer?.cancel()
        filterTimer = object :
            CountDownTimer(
                timerDuration,
                timerDuration
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                getActiveChannel { tvChannel ->
                    getEventsForChannels(tvChannel, filterId, callback, 0, 0, false)
                }
            }
        }
        filterTimer!!.start()
    }

    fun getAvailableFilters(callback: (filters: ArrayList<Category>?)-> Unit) {
        categoryModule?.getAvailableFilters(object :
            IAsyncDataCallback<ArrayList<Category>> {
            override fun onFailed(error: Error) {
                callback(null)
            }

            override fun onReceive(data: ArrayList<Category>) {
                // Add favorites filter
                if (favoriteList.isNotEmpty()) {
                    var category = Category(Category.FAVORITE_ID, "Favorites")
                    category.priority = 2
                    data.add(0, category)
                }
                callback(data)
            }
        }, ApplicationMode.FAST_ONLY)
    }

    //Private inner method
    private fun getEventsForChannels(anchorChannel: TvChannel, list : MutableList<TvChannel>,callback: (map: java.util.LinkedHashMap<Int, MutableList<TvEvent>>)-> Unit, dayOffset: Int, additionalDayOffset: Int, isExtend:Boolean){
        var channelsListToLoad = mutableListOf<TvChannel>()
        list.forEach {
            var isAlreadyHave = false
            for (channel in channelListForSelectedFilter.toMutableList()) {
                if (channel.name == it.name) {
                    isAlreadyHave = true
                    break
                }
            }
            if (!isAlreadyHave) {
                if(it.isBrowsable || it.inputId.contains("iwedia") || it.inputId.contains("sampletvinput")){
                    channelListForSelectedFilter.add(it)
                }
            }
        }
        var posAnchorChannel = 0
        for (i in 0 until channelListForSelectedFilter.size){
            if (channelListForSelectedFilter[i].name== anchorChannel.name){
                posAnchorChannel = i
            }
        }
        var counter = 0
        var posForNextChannels  = posAnchorChannel
        if (posAnchorChannel==-1) {
            posAnchorChannel = if (channelListForSelectedFilter.lastIndex>=5){
                6
            }else{
                channelListForSelectedFilter.lastIndex
            }
            posForNextChannels = posAnchorChannel
        }

        while (counter!=7 && posAnchorChannel>=0){
            if (channelListForSelectedFilter.size>posAnchorChannel && !channelsListToLoad.contains(channelListForSelectedFilter[posAnchorChannel]))
                channelsListToLoad.add(channelListForSelectedFilter[posAnchorChannel])

            posAnchorChannel--
            counter++
        }
        channelsListToLoad.reverse()
        counter =0
        try {
            while (counter!=7 && posForNextChannels< channelListForSelectedFilter.size){
                if (!channelsListToLoad.contains(channelListForSelectedFilter[posForNextChannels]))
                    channelsListToLoad.add(channelListForSelectedFilter[posForNextChannels])
                posForNextChannels++
                counter++
            }
        }catch (e:java.lang.Exception){
            e.printStackTrace()
        }
        fetchEventForChannelList(callback, channelsListToLoad, dayOffset, additionalDayOffset, isExtend)
    }

    fun getEventsForChannels(anchorChannel: TvChannel, filterId: Int, callback: (map: java.util.LinkedHashMap<Int, MutableList<TvEvent>>)-> Unit, dayOffset: Int, additionalDayOffset: Int, isExtend:Boolean){
        if (filterId == 0 && favoriteList.isNotEmpty()) {
            channelListForSelectedFilter.clear()
            getEventsForChannels(anchorChannel, favoriteList, callback, dayOffset, additionalDayOffset, isExtend)
        } else {
            categoryModule?.getAvailableFilters(object :IAsyncDataCallback<ArrayList<Category>>{
                override fun onFailed(error: Error) {}

                override fun onReceive(data: ArrayList<Category>) {
                    filterList = data
                    channelListForSelectedFilter.clear()
                    var filterId = filterId
                    if (favoriteList.isNotEmpty() && filterId != 0) {
                        filterId -= 1
                    }
                    val activeFilter = filterList!![filterId]

                    categoryModule?.filterChannels(activeFilter,   object : IAsyncDataCallback<ArrayList<TvChannel>> {
                        override fun onFailed(error: Error) {}
                        @RequiresApi(Build.VERSION_CODES.R)
                        override fun onReceive(data: ArrayList<TvChannel>) {

                            var list : MutableList<TvChannel> = mutableListOf()
                            list.addAll(data)
                            getEventsForChannels(anchorChannel, list, callback, dayOffset, additionalDayOffset, isExtend)
                        }
                    }, ApplicationMode.FAST_ONLY)
                }
            }, ApplicationMode.FAST_ONLY)
        }
    }

    fun loadNextChannels(anchorChannel: TvChannel, callback: (hashMap: java.util.LinkedHashMap<Int, MutableList<TvEvent>>)-> Unit, dayOffset: Int, additionalDayOffset: Int, isExtend:Boolean) {
        var channelsListToLoad = mutableListOf<TvChannel>()
        var posAnchorChannel = if (channelListForSelectedFilter.indexOf(anchorChannel) == -1) 0 else channelListForSelectedFilter.indexOf(anchorChannel)

        for (i in 0 until channelListForSelectedFilter.size){
            if (channelListForSelectedFilter[i].name == anchorChannel.name){
                posAnchorChannel = i
            }
        }

        var counter = 0
        while (counter!=10 && posAnchorChannel< channelListForSelectedFilter.size){
            if (!channelsListToLoad.contains(channelListForSelectedFilter[posAnchorChannel]))
                channelsListToLoad.add(channelListForSelectedFilter[posAnchorChannel])

            posAnchorChannel++
            counter++
        }
        fetchEventForChannelList(callback, channelsListToLoad, dayOffset, additionalDayOffset, isExtend)
    }

    fun loadPreviousChannels(anchorChannel: TvChannel, callback: (hashMap: java.util.LinkedHashMap<Int, MutableList<TvEvent>>)-> Unit, dayOffset: Int, additionalDayOffset: Int, isExtend:Boolean){

        var channelsListToLoad = mutableListOf<TvChannel>()

            //if channel is not available then it would give -1
        var posAnchorChannel = if (channelListForSelectedFilter.indexOf(anchorChannel) == -1) 0 else channelListForSelectedFilter.indexOf(anchorChannel)

        for (i in 0 until channelListForSelectedFilter.size){
            if (channelListForSelectedFilter[i].name == anchorChannel.name){
                posAnchorChannel = i
            }
        }
        var counter = 0
        while (counter!=10 && posAnchorChannel>=0){
            if (!channelsListToLoad.contains(channelListForSelectedFilter[posAnchorChannel]))
                channelsListToLoad.add(channelListForSelectedFilter[posAnchorChannel])

            posAnchorChannel--
            counter++
        }
        channelsListToLoad.reverse()
        fetchEventForChannelList(callback, channelsListToLoad, dayOffset, additionalDayOffset, isExtend)
    }

    fun fetchEventForChannelList(callback: (hashMap: java.util.LinkedHashMap<Int, MutableList<TvEvent>>)-> Unit, channelList: MutableList<TvChannel>, dayOffset: Int, additionalDayOffset: Int, isExtend: Boolean){
        val date = Date(System.currentTimeMillis()).date // to fix timeline crossing a day scenario (else we need to clear the map once day crossed)
        //Create hashcode based on fetch params
        val hashCode = "${channelList.hashCode()}_${date}_${dayOffset}_${additionalDayOffset}_${isExtend.hashCode()}"
        if (cache.isNotEmpty() && cache.containsKey(hashCode)) {
            val map = java.util.LinkedHashMap<Int, MutableList<TvEvent>>()
            cache[hashCode]!!.forEach { (index, eventList) ->
                map[index] = eventList.toMutableList()  // Duplicating to prevent updates in cache
            }
            callback.invoke(map)
            return
        }
        var channelEventListMap = ConcurrentHashMap<String, MutableList<TvEvent>>()
        var counter = AtomicInteger(0)
        var includeInCache = false

        var runnable = Runnable{
            val map = java.util.LinkedHashMap<Int, MutableList<TvEvent>>()
            val cacheMap = java.util.LinkedHashMap<Int, MutableList<TvEvent>>()

            for (index in 0 until channelList.size) {
                val tvChannel = channelList[index]
                if (channelEventListMap.keys.contains(tvChannel.name)) {
                    map[index] = channelEventListMap[tvChannel.name]!!.toMutableList() // Duplicating to prevent updates in cache
                    cacheMap[index] = channelEventListMap[tvChannel.name]!!
                }
            }
            if(includeInCache && IS_EVENT_LIST_PAGE_CACHE_ENABLED) cache[hashCode] = cacheMap
            callback.invoke(map)
        }

        channelList.forEach {tvChannel ->TvChannel
            fetchEventList(
                tvChannel,
                dayOffset) { eventList->
                if (isExtend) {
                    fetchEventList(
                        tvChannel,
                        additionalDayOffset){ additionalEventList->
                        val leftEventList =
                            if (dayOffset > additionalDayOffset) additionalEventList else eventList
                        val rightEventList =
                            if (dayOffset > additionalDayOffset) eventList else additionalEventList
                        if (rightEventList!!.first().id == leftEventList!!.last()!!.id && rightEventList!!.first().id != -1) {
                            var event = leftEventList.last()
                            event.endTime =
                                rightEventList.first().endTime
                            leftEventList.removeLast()
                            rightEventList.removeFirst()
                            rightEventList.add(0, event)
                        }

                        leftEventList.addAll(rightEventList)

                        channelEventListMap[tvChannel.name] = leftEventList

                        val vai = counter.incrementAndGet()
                        if (vai == channelList.size) {
                            runnable.run()
                        }
                    }
                } else {
                    channelEventListMap[tvChannel.name] = eventList!!
                    val vai = counter.incrementAndGet()

                    // To add into cache only when events are valid
                    if (!includeInCache) {
                        for (event in eventList) {
                            if (event.id != -1) {
                                includeInCache = true
                                break
                            }
                        }
                    }

                    if (vai == channelList.size) {
                        runnable.run()
                    }
                }
            }
        }
    }

    fun getEPGEventsData(
        guideChannelList: MutableList<TvChannel>,
        additionalDayCount: Int,
        isExtend: Boolean,
        callback: (channelEventsMap: LinkedHashMap<Int, MutableList<TvEvent>>,
                   guideChannelList: MutableList<TvChannel>,
                   additionalDayCount: Int,
                   isExtend: Boolean) -> Unit
    ) {
        val map = ConcurrentHashMap<String, MutableList<TvEvent>>()
        // Fetch epd data
        var counter = AtomicInteger(0)
        for (index in 0 until guideChannelList.size) {
            var tvChannel = guideChannelList[index]
            fetchEventList(
                tvChannel,
                additionalDayCount
            ) { eventList ->
                if (!eventList.isNullOrEmpty()) {
                    map[tvChannel.name] = eventList
                } else {
                    map[tvChannel.name] = mutableListOf(
                        TvEvent.createNoInformationEvent(
                            tvChannel,
                            System.currentTimeMillis()
                        )
                    )
                }
                if (counter.incrementAndGet() == guideChannelList.size) {
                    val channelEventMap =
                        java.util.LinkedHashMap<Int, MutableList<TvEvent>>()
                    for (index in 0 until guideChannelList.size) {
                        val tvChannel = guideChannelList[index]
                        if (map.keys.contains(tvChannel.name)) {
                            channelEventMap[index] = map[tvChannel.name]!!
                        }
                    }

                    callback(
                        channelEventMap,
                        guideChannelList, additionalDayCount, isExtend
                    )
                }
            }
        }
    }

    private fun fetchEventList(
        tvChannel: TvChannel,
        dayOffset: Int,
        callback: (eventList: MutableList<TvEvent>?) -> Unit
    ) {
        thread {
            //Set start time
            var calendar = Calendar.getInstance()
            calendar.time = Date(System.currentTimeMillis())
            calendar.add(Calendar.DATE, dayOffset)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            var filterStartDate = Date(calendar.time.time)

            calendar = Calendar.getInstance()
            calendar.time = Date(System.currentTimeMillis())
            calendar.add(Calendar.DATE, dayOffset)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            var filterEndDate = Date(calendar.time.time)

            epgModule!!.getEventListByChannelAndTime(
                tvChannel,
                filterStartDate.time,
                filterEndDate.time,
                object : IAsyncDataCallback<ArrayList<TvEvent>> {
                    override fun onReceive(eventList: ArrayList<TvEvent>) {
                        val activeChannelTime = Calendar.getInstance()
                        activeChannelTime.time.time = System.currentTimeMillis()
                        activeChannelTime.set(Calendar.SECOND, 0)
                        activeChannelTime.set(Calendar.MILLISECOND, 0)

                        val channelTime = Calendar.getInstance()
                        channelTime.time.time = System.currentTimeMillis()
                        channelTime.set(Calendar.SECOND, 0)
                        channelTime.set(Calendar.MILLISECOND, 0)

                        if(activeChannelTime!=channelTime) {
                            eventList.clear()
                        }

                        val data= ArrayList<TvEvent>()
                        data.addAll(eventList)

                        val eventList = mutableListOf<TvEvent>()
                        val tempList = mutableListOf<TvEvent>()
                        tempList.addAll(data)

                        val calendar = Calendar.getInstance()
                        calendar.time = Date(System.currentTimeMillis())
                        calendar.add(Calendar.DATE, dayOffset)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        var startDate = Date(calendar.time.time)

                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND,999)
                        var endDate = Date(calendar.time.time)

                        for (event in tempList) {
                            if ((startDate.time > event.startTime && startDate.time > event.endTime) || (endDate.time < event.startTime && endDate.time < event.endTime)) {
                                continue
                            }

                            if (Date(event.startTime).after(endDate)) {
                                continue
                            }
                            if (Date(event.endTime).before(startDate)) {
                                continue
                            }
                            eventList.add(event.clone())
                        }
                        if (eventList.isEmpty()) {
                            val calendar = Calendar.getInstance()
                            calendar.time = Date(System.currentTimeMillis())
                            calendar.add(Calendar.DATE, dayOffset)
                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                            calendar.set(Calendar.MINUTE, 59)
                            calendar.set(Calendar.SECOND, 59)
                            val noInformationEndDate = calendar.time.time
                            eventList.add(
                                TvEvent.createNoInformationEvent(
                                    tvChannel,
                                    startDate.time,
                                    noInformationEndDate,
                                    System.currentTimeMillis()
                                )
                            )
                        }

                        if (eventList.isNotEmpty()) {
                            //Generate no information event at the first index
                            if (eventList[0].startTime > startDate.time) {
                                var endDate =eventList[0].startTime
                                eventList.add(
                                    0,
                                    TvEvent.createNoInformationEvent(
                                        tvChannel,
                                        startDate.time,
                                        endDate,
                                        System.currentTimeMillis()
                                    )
                                )
                            }
                            //Generate no information event at the last index
                            if (eventList[eventList.size - 1].endTime < endDate.time) {
                                var startDate =
                                    Date(eventList[eventList.size - 1].endTime)
                                eventList.add(
                                    TvEvent.createNoInformationEvent(
                                        tvChannel,
                                        startDate.time,
                                        endDate.time,
                                        System.currentTimeMillis()
                                    )
                                )
                            }

                            var previousEvent: TvEvent? = null
                            eventList.forEach { event ->
                                if (previousEvent != null && event.startTime != previousEvent!!.endTime) {
                                    previousEvent!!.endTime = event.startTime
                                }
                                previousEvent = event
                            }

                            var firstEvent = eventList.first()
                            var firstEventStartDate = Date(firstEvent.startTime)
                            var firstEventEndDate = Date(firstEvent.endTime)
                            if (firstEventStartDate.timezoneOffset == firstEventEndDate.timezoneOffset &&
                                firstEventStartDate.date == firstEventEndDate.date &&
                                firstEventStartDate.hours == firstEventEndDate.hours &&
                                firstEventStartDate.minutes == firstEventEndDate.minutes
                            ) {
                                eventList.removeFirst()
                            }

                            var lastEvent = eventList.last()
                            var lastEventStartDate = Date(lastEvent.startTime)
                            var lastEventEndDate = Date(lastEvent.endTime)
                            if (lastEventStartDate.timezoneOffset == lastEventEndDate.timezoneOffset &&
                                lastEventStartDate.date == lastEventEndDate.date &&
                                lastEventStartDate.hours == lastEventEndDate.hours &&
                                lastEventStartDate.minutes == lastEventEndDate.minutes
                            ) {
                                eventList.removeLast()
                            }
                        }

                        val firstItem = eventList.first()
                        val firstStartDate = Calendar.getInstance()
                        firstStartDate.time  = Date(System.currentTimeMillis())
                        firstStartDate.add(Calendar.DATE, dayOffset)
                        firstStartDate.set(Calendar.HOUR_OF_DAY, 0)
                        firstStartDate.set(Calendar.MINUTE, 0)
                        firstStartDate.set(Calendar.SECOND, 0)
                        firstStartDate.set(Calendar.MILLISECOND, 0)
                        if(firstItem.startTime > firstStartDate.time.time)
                            firstItem.startTime = firstStartDate.time.time

                        val lastItem = eventList.last()
                        val lastEndDate = Calendar.getInstance()
                        lastEndDate.time = Date(System.currentTimeMillis())
                        lastEndDate.add(Calendar.DATE, dayOffset)
                        lastEndDate.set(Calendar.HOUR_OF_DAY, 23)
                        lastEndDate.set(Calendar.MINUTE, 59)
                        lastEndDate.set(Calendar.SECOND, 59)
                        lastEndDate.set(Calendar.MILLISECOND, 999)
                        if(lastItem.endTime <lastEndDate.time.time)
                            lastItem.endTime = lastEndDate.time.time

                        callback(eventList)
                    }

                    override fun onFailed(error: Error) {
                        callback(null)
                    }

                }
            )
        }
    }

    /**
     * Fast show live tab broadcast receiver
     */
    private val receiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(Constants.LogTag.CLTV_TAG + "FastLiveTabDataProvider", "FAST show live tab intent received")
            if (intent != null && intent.action == FastZapBannerDataProvider.FAST_SHOW_GUIDE_INTENT) {

                ReferenceApplication.worldHandler?.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                var sceneId = ReferenceApplication.worldHandler?.active?.id
                var sceneInstanceId = ReferenceApplication.worldHandler?.active?.instanceId
                var sceneData = HomeSceneData(sceneId!!, sceneInstanceId!!)
                sceneData.focusToCurrentEvent = true
                sceneData.initialFilterPosition = 1

                ReferenceApplication.worldHandler?.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.HOME_SCENE,
                    SceneManager.Action.SHOW_OVERLAY, sceneData
                )
            }
        }
    }

    //----------------------- Favorites -----------------------------
    private fun getFavorites(){
        favoriteList.clear()
        fastFavoriteInterface!!.getFavorites(object :IAsyncDataCallback<ArrayList<String>>{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getFavorites onFailed: ")
            }
            override fun onReceive(data: ArrayList<String>) {
                data.forEach { channelId ->
                    try{
                        tvModule?.getChannelByDisplayNumber(channelId, ApplicationMode.FAST_ONLY)?.let { favoriteList.add(it) }
                    }catch (e:Exception){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "exception while fetching fav: $e")
                    }
                }

            }
        })
    }
    fun updateFavorites(tvChannel: TvChannel,isFavoriteFilterSelected:Boolean, callack: ()->Unit) {
        val addToFav = !isInFavorites(tvChannel)
        fastFavoriteInterface!!.updateFavorites(tvChannel.displayNumber,
            addToFav, object :IAsyncCallback{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: update favorite")
                callack.invoke()
            }
            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: update favorite")
                if (addToFav) {
                    favoriteList.add(tvChannel)
                } else {
                    val channelToRemove = favoriteList.find { it.displayNumber == tvChannel.displayNumber}
                    if (channelToRemove != null) {
                        favoriteList.remove(channelToRemove)
                        if (isFavoriteFilterSelected) {
                            channelListForSelectedFilter.remove(channelToRemove)
                        }
                    }
                }
                callack.invoke()
            }
        })
    }

    fun isInFavorites(tvChannel: TvChannel) = favoriteList.find { it.displayNumber == tvChannel.displayNumber} != null

    fun getFavoriteChannels(): ArrayList<TvChannel> {
        val mFavoriteList = arrayListOf<TvChannel>()
        tvModule!!.getChannelList(ApplicationMode.FAST_ONLY).forEach { tvChannel ->
            if(isInFavorites(tvChannel)){
                mFavoriteList.add(tvChannel)
            }
        }
        return mFavoriteList
    }

    /**
     * Formats the remaining time in hours and minutes based on the provided event.
     *
     * @param tvEvent the current tv event
     * @return A formatted string representing the remaining time, e.g., "1 hour 30 minutes left."
     */
    fun formatRemainingTime(tvEvent: TvEvent): String {

        val currentTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis())
        val eventEndTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(tvEvent.endTime)

        val remainingTimeMinutes = eventEndTimeMinutes - currentTimeMinutes

        val minutes = remainingTimeMinutes % 60
        val hours = TimeUnit.MINUTES.toHours(remainingTimeMinutes)

        val hourSuffix = if (hours > 1) "hours" else "hour"
        val minuteSuffix = if (minutes > 1) "minutes" else "minute"

        val sb = StringBuilder()

        if (hours > 0) {
            sb.append(hours)
            sb.append(" ")
            sb.append(hourSuffix)
            if (minutes > 0) {
                sb.append(" ")
            }
        }
        if (minutes > 0) {
            sb.append(minutes)
            sb.append(" ")
            sb.append(minuteSuffix)
        }

        sb.append(" ")
        sb.append("left")

        return sb.toString()
    }

    /**
     * Checks if the event is current event
     *
     * @param tvEvent the event
     * @return `true` if the it is current event.
     */
    fun isCurrentEvent(tvEvent: TvEvent?): Boolean {
        if (tvEvent == null) return false
        return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) in TimeUnit.MILLISECONDS.toMinutes(tvEvent.startTime) until TimeUnit.MILLISECONDS.toMinutes(tvEvent.endTime)
    }

    /**
     * Checks if the event is past event
     *
     * @param tvEvent the event
     * @return `true` if the it is past event.
     */
    fun isPastEvent(tvEvent: TvEvent): Boolean {
        return TimeUnit.MILLISECONDS.toMinutes(tvEvent.endTime) <= TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis())
    }

    fun isAnokiServerReachable(): Boolean {
        return networkModule!!.anokiServerStatus.value!!
    }

    fun getDateTimeFormat() : DateTimeFormat {
        return utilsModule!!.getDateTimeFormat()
    }

    fun isEventLocked(tvEvent: TvEvent?) = parentalControlSettingsModule!!.isEventLocked(tvEvent)

    @RequiresApi(Build.VERSION_CODES.R)
    fun getActiveGenre(callback: (genre: String)-> Unit) {
        if (activeGenre == "") {
            getActiveChannel {
                activeGenre = it.genres[0]
                callback.invoke(activeGenre!!)
            }
        } else {
            callback.invoke(activeGenre!!)
        }
    }

    fun isRecordingInProgress(): Boolean {
        return pvrModule!!.isRecordingInProgress()
    }

    fun isTimeShiftActive(): Boolean {
        return timeShiftModule!!.isTimeShiftActive
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showStopRecordingDialog(callback: IAsyncCallback) {
        val sceneData = DialogSceneData(
            ReferenceApplication.worldHandler!!.active!!.id,
            ReferenceApplication.worldHandler!!.active!!.instanceId
        )
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {
                callback.onFailed(Error("On cancel clicked"))
            }

            override fun onPositiveButtonClicked() {
                val recordingChannel = pvrModule!!.getRecordingInProgressTvChannel()
                pvrModule!!.stopRecordingByChannel(recordingChannel!!, object : IAsyncCallback {
                    override fun onFailed(error: Error) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "stop recording failed")
                    }

                    override fun onSuccess() {
                        ReferenceApplication.worldHandler!!.playbackState =
                            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                        PvrBannerSceneManager.previousProgress = 0L
                        pvrModule!!.setRecIndication(false)
                        callback.onSuccess()
                    }
                })
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE, SceneManager.Action.DESTROY
                )
            }
        }
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE, SceneManager.Action.SHOW, sceneData
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showStopTimeShiftDialog(callback: IAsyncCallback) {
        val sceneData = DialogSceneData(
            ReferenceApplication.worldHandler!!.active!!.id,
            ReferenceApplication.worldHandler!!.active!!.instanceId
        )
        sceneData.type = DialogSceneData.DialogType.YES_NO
        sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
        sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
        sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
            override fun onNegativeButtonClicked() {
                callback.onFailed(Error("On cancel clicked"))
            }

            override fun onPositiveButtonClicked() {
                timeShiftModule?.timeShiftStop(object : IAsyncCallback {
                    override fun onFailed(error: Error) {}
                    override fun onSuccess() {}
                })
                ReferenceApplication.worldHandler!!.playbackState =
                    ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                timeShiftModule?.setTimeShiftIndication(false)
                callback.onSuccess()
                ReferenceApplication.worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE, SceneManager.Action.DESTROY
                )
            }
        }
        ReferenceApplication.worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.DIALOG_SCENE, SceneManager.Action.SHOW, sceneData
        )
    }

    fun isTosAccepted() = tosAccepted

    fun onDestroy() {
        fastLiveTab = null
        filterTimer = null
    }
}