package com.iwedia.cltv.manager

import android.content.Intent
import android.os.Build
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.anoki_fast.vod.details.DetailsSceneData
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.search.SearchScene
import com.iwedia.cltv.scene.search.SearchSceneListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.anoki.ondemand.domain.model.VODItem
import tv.anoki.ondemand.domain.model.VODType
import tv.anoki.ondemand.presentation.listing.VodListDataUiState
import tv.anoki.ondemand.presentation.listing.VodListDataViewModel
import world.SceneData
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "SearchSceneManager"
/**
 * Search scene manager
 *
 * @author Aleksandar Lazic
 */
class SearchSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var searchModule: SearchInterface,
    var tvModule: TvInterface,
    var epgModule: EpgInterface,
    var pvrModule: PvrInterface,
    var parentalControlSettingsModule: ParentalControlSettingsInterface,
    var timeModule: TimeInterface,
    var watchlistModule: WatchlistInterface,
    private var utilsModule: UtilsInterface,
    var networkModule: NetworkInterface,
    var schedulerModule: SchedulerInterface,
    private val textToSpeechModule: TTSInterface
) :
    GAndroidSceneManager(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.SEARCH
    ), SearchSceneListener {

    val rails: MutableList<RailItem> = mutableListOf()
    private var filterJob = CoroutineScope(Dispatchers.IO).launch {  }

    private var vm: VodListDataViewModel
    init {
        context.apply {
            val viewModel by viewModels<VodListDataViewModel>()
            vm = viewModel
        }
    }
    override fun createScene() {
        scene = SearchScene(context!!, this)
    }

    override fun onSceneInitialized() {

    }

    private fun checkNetwork(): Boolean = networkModule.networkStatus.value != NetworkData.NoConnection

    override fun onSearchQuery(query: String) {
        if (query.length > 1) {
            filterRails(query)
            return
        }

        //clear previous searches
        rails.clear()

        //search channels and events
        searchModule.searchForChannels(
            query,
            object : IAsyncDataCallback<List<TvChannel>> {
                override fun onFailed(error: Error) {
                    searchEvents(query)
                }

                override fun onReceive(data: List<TvChannel>) {
                    var foundChannels = mutableListOf<TvChannel>()
                    data.forEach {
                        if (it.isFastChannel()) {
                            if (checkNetwork()) {
                                foundChannels.add(it)
                            }
                        } else {
                            foundChannels.add(it)
                        }
                    }

                    if (foundChannels.isNotEmpty()) {
                        getCurrentEvents(
                            foundChannels,
                            object : IAsyncDataCallback<MutableList<TvEvent>> {
                                override fun onFailed(error: Error) {
                                    searchEvents(query)
                                }

                                override fun onReceive(data: MutableList<TvEvent>) {
                                    if ((data).size > 0) {
                                        val events = mutableListOf<Any>()
                                        events.addAll(data)
                                        rails.add(
                                            RailItem(
                                                id = 0,
                                                railName = ConfigStringsManager.getStringById("channels"),
                                                rail = events,
                                                RailItem.RailItemType.CHANNEL
                                            )
                                        )
                                    }
                                    searchEvents(query)
                                }
                            })
                    } else {
                        searchEvents(query)
                    }
                }
            })

    }
    private fun filterRails(query: String) {
        filterJob.cancel()

        val tmpRails: MutableList<RailItem> = mutableListOf()
        rails.forEach { railItem ->
            val copiedRail = RailItem(
                id = railItem.id,
                railName = railItem.railName,
                rail = railItem.rail?.map { it }?.toMutableList(),
                type = railItem.type
            )
            tmpRails.add(copiedRail)
        }

        filterJob = CoroutineScope(Dispatchers.IO).launch {
            tmpRails.forEach { railItem ->
                val itemsToRemove = mutableListOf<Any>()
                railItem.rail?.forEach { item ->
                    if (item is TvEvent && !item.name.contains(query, ignoreCase = true)) {
                        itemsToRemove.add(item)
                    }
                    if (item is TvChannel && !item.name.contains(query, ignoreCase = true)) {
                        itemsToRemove.add(item)
                    }
                }
                railItem.rail?.removeAll(itemsToRemove)
            }
            scene!!.refresh(tmpRails)
        }
    }

    private fun searchEvents(query: String) {
        searchModule!!.searchForEvents(
            query,
            object : IAsyncDataCallback<List<TvEvent>> {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onFailed(error: Error) {
                    searchVodItems(query)
                }

                override fun onReceive(data: List<TvEvent>) {
                    val foundEvents: MutableList<TvEvent> = mutableListOf()
                    //TODO past events are removed since catch up feature is not supported
                    //var pastEvents: MutableList<ReferenceTvEvent> = mutableListOf()
                    val futureEvents: MutableList<TvEvent> = mutableListOf()


                    data.forEach { tvEvent ->
                        /*if (tvEvent.endDate.value.toLong() < currentTime.value.toLong()) {
                            pastEvents.add(tvEvent)
                        }*/
                        //future event
                        val currentTime = timeModule.getCurrentTime(tvEvent.tvChannel)
                        if (tvEvent.startTime > currentTime) {
                            if (tvEvent.tvChannel.isFastChannel()) {
                                if (checkNetwork()) {
                                    futureEvents.add(tvEvent)
                                }
                            } else {
                                futureEvents.add(tvEvent)
                            }
                        }
                        //current event
                        else if (tvEvent.startTime < currentTime && tvEvent.endTime > currentTime){
                            if (tvEvent.tvChannel.isFastChannel()) {
                                if (checkNetwork()) {
                                    foundEvents.add(tvEvent)
                                }
                            } else {
                                foundEvents.add(tvEvent)
                            }
                        }
                    }

                    /*if (pastEvents.isNotEmpty()) {
                        rails.add(
                            ForYouRailItem(
                                2,
                                ConfigStringsManager.getStringById("past_events"),
                                pastEvents,
                                ForYouRailItem.ItemType.EVENT
                            )
                        )
                    }*/
                    if (foundEvents.isNotEmpty()) {
                        val events = arrayListOf<Any>()
                        events.addAll(foundEvents)
                        rails.add(
                            RailItem(
                                id = 1,
                                railName = ConfigStringsManager.getStringById("on_now"),
                                rail = events,
                                RailItem.RailItemType.EVENT
                            )
                        )
                    }
                    if (futureEvents.isNotEmpty()) {
                        val events = arrayListOf<Any>()
                        events.addAll(futureEvents)
                        rails.add(
                            RailItem(
                                id = 3,
                                railName = ConfigStringsManager.getStringById("future_events"),
                                events,
                                RailItem.RailItemType.EVENT
                            )
                        )
                    }
                    getRecordings(
                        query,
                        object : IAsyncDataCallback<MutableList<Recording>> {
                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun onReceive(data: MutableList<Recording>) {
                                if (data.isNotEmpty()) {
                                    var recordings = mutableListOf<Any>()
                                    recordings.addAll(data)
                                    var railItem = RailItem(
                                        id = 4,
                                        railName = ConfigStringsManager.getStringById("recorded"),
                                        recordings,
                                        RailItem.RailItemType.RECORDING
                                    )
                                    rails.add(
                                        railItem
                                    )
                                }
                                searchVodItems(query)
                            }

                            @RequiresApi(Build.VERSION_CODES.R)
                            override fun onFailed(error: Error) {
                                searchVodItems(query)
                            }
                        })
                }
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun searchVodItems(query: String) {
        if (!HomeSceneManager.IS_VOD_ENABLED) {
            // VOD is not enabled - do not filter VOD items
            ReferenceApplication.runOnUiThread{
                scene!!.refresh(rails)
            }
            return
        }
        var tmpList: ArrayList<VODItem> = arrayListOf()
        vm.viewModelScope.launch(Dispatchers.Default) {
            vm.vodItemsFlow.collect{
                if (it is VodListDataUiState.Ready) {
                    it.data.forEach {vodItems ->
                        tmpList.addAll(
                            vodItems.items.filter {
                                it.title.lowercase().contains(query.lowercase())
                            }
                        )
                    }
                    rails.add(
                        RailItem(
                            id = 5,
                            railName = ConfigStringsManager.getStringById("vod"),
                            rail = tmpList.toMutableList(),
                            RailItem.RailItemType.VOD
                        )
                    )
                    withContext(Dispatchers.Main){
                        scene!!.refresh(rails)
                    }
                }
            }

        }
    }

    private fun getRecordings(
        query: String,
        callback: IAsyncDataCallback<MutableList<Recording>>
    ) {
        var recordedEvents = mutableListOf<Recording>()
        searchModule.searchForRecordings(
            query,
            object :
                IAsyncDataCallback<List<Recording>> {
                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }

                override fun onReceive(data: List<Recording>) {
                    data.forEach { item ->
                        recordedEvents.add(item as Recording)
                    }

                    callback.onReceive(recordedEvents)
                }
            })
    }

    override fun onBackPressed(): Boolean {
        worldHandler!!.triggerAction(id, Action.DESTROY)
        if (data != null) {
            worldHandler!!.triggerActionWithInstanceId(
                data!!.previousSceneId,
                data!!.previousSceneInstance,
                Action.SHOW
            )
        }
        return super.onBackPressed()

    }

    private fun getCurrentEvents(
        foundChannels: MutableList<TvChannel>,
        callback: IAsyncDataCallback<MutableList<TvEvent>>
    ) {
        val retVal: MutableList<TvEvent> = mutableListOf()
        var counter = AtomicInteger(0)

        for (i in 0 until foundChannels.size) {
            epgModule.getCurrentEvent(
                foundChannels[i]!!,
                object : IAsyncDataCallback<TvEvent> {
                    override fun onFailed(error: Error) {
                        val currentTime = timeModule.getCurrentTime(foundChannels[i])
                        val (startDate, endDate) = Utils.getStartEndDate(currentTime)
                        val add = retVal.add(
                            TvEvent(
                                -1,
                                foundChannels[i]!!,
                                foundChannels.get(i)!!.name,
                                "",
                                "",
                                "",
                                startDate.toLong(),
                                endDate.toLong(),
                                arrayListOf(),
                                1,
                                1,
                                null,
                                "",
                                false,
                                false,
                                0
                            )
                        )

                        if (counter.incrementAndGet() == foundChannels.size) {
                            callback.onReceive(retVal)
                        }
                    }

                    override fun onReceive(data: TvEvent) {
                        retVal.add(data)

                        if (counter.incrementAndGet() == foundChannels.size) {
                            callback.onReceive(retVal)
                        }
                    }
                })
        }
    }

    override fun onTvEventClicked(event: Any) {
        // TODO DEJAN ovde treba provera da li je lokovan - u tom slucaju prikazati PinScene.
        if (event is TvEvent) {
            if (event.tvChannel.isFastChannel()) {
                playFastChannel(event.tvChannel)
                //updates the active gener when channel is played from for search scene.
                if (event.tvChannel.genres.isNotEmpty()){
                    FastLiveTabDataProvider.activeGenre = event.tvChannel.genres[0].lowercase().split(' ')
                        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                }
            } else {
                // Show details
                context!!.runOnUiThread {
                    worldHandler!!.triggerAction(id, Action.HIDE)
                    val sceneData = SceneData(id, instanceId, event)
                    worldHandler!!.triggerActionWithData(
                        ReferenceWorldHandler.SceneId.DETAILS_SCENE,
                        Action.SHOW_OVERLAY, sceneData
                    )
                }
            }
        } else if (event is TvChannel) {
            if (event.isFastChannel()) {
                playFastChannel(event)
                //updates the active gener when channel is played from for search scene.
                if (event.genres.isNotEmpty()){
                    FastLiveTabDataProvider.activeGenre = event.genres[0].lowercase().split(' ')
                        .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
                }
            } else {
                playTvChannel(event)
            }
        } else if (event is VODItem) {
            worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.DETAILS_SCENE,
                Action.SHOW
            )
        }
    }

    private fun playFastChannel(tvChannel: TvChannel) {
        //Set application mode 1 for FAST ONLY mode
        utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
        ReferenceApplication.runOnUiThread(Runnable {
            val intent = Intent(FastZapBannerDataProvider.FAST_SHOW_ZAP_BANNER_INTENT)
            ReferenceApplication.applicationContext().sendBroadcast(intent)
            ReferenceApplication.worldHandler?.triggerAction(id, Action.HIDE)
            ReferenceApplication.worldHandler?.triggerAction(ReferenceWorldHandler.SceneId.HOME_SCENE, Action.HIDE)
            BackFromPlayback.resetKeyPressedState()
            BackFromPlayback.zapFromHomeOrSearch = true
        })
        tvModule.changeChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {}

            override fun onSuccess() {}
        }, ApplicationMode.FAST_ONLY)
    }

    override fun isChannelLocked(channelId: Int): Boolean {
        tvModule.getChannelList().forEach { channel ->
            if (channel.channelId == channelId.toLong()) {
                return channel.isLocked
            }
        }
        return false
    }

    override fun isParentalControlsEnabled(): Boolean {
        if ((ReferenceApplication.worldHandler as ReferenceWorldHandler).getApplicationMode() == ApplicationMode.DEFAULT.ordinal) {
            return parentalControlSettingsModule.isParentalControlsEnabled()
        } else {
            return parentalControlSettingsModule.isAnokiParentalControlsEnabled()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        val applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        return tvModule.getParentalRatingDisplayName(parentalRating, applicationMode, tvEvent)
    }

    override fun getCurrent(tvChannel: TvChannel): Long {
        return timeModule.getCurrentTime(tvChannel)
    }

    override fun isInWatchlist(tvEvent: TvEvent): Boolean {
        return watchlistModule.isInWatchlist(tvEvent)
    }

    override fun isInRecList(tvEvent: TvEvent): Boolean {
        return schedulerModule.isInReclist(tvEvent.tvChannel.id, tvEvent.startTime)
    }

    override fun getDateTimeFormat(): DateTimeFormat {
        return utilsModule.getDateTimeFormat()
    }

    override fun isEventLocked(tvEvent: TvEvent?) = parentalControlSettingsModule.isEventLocked(tvEvent)
    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    override fun stopSpeech() {
        textToSpeechModule.stopSpeech()
    }

    override fun onVodItemClicked(type: VODType, contentId: String) {
        worldHandler!!.triggerActionWithData(
            when (type) {
                VODType.SERIES -> ReferenceWorldHandler.SceneId.VOD_SERIES_DETAILS_SCENE
                else -> { ReferenceWorldHandler.SceneId.VOD_SINGLE_WORK_DETAILS_SCENE }
            },
            Action.SHOW,
            data = DetailsSceneData(
                ReferenceApplication.worldHandler!!.active!!.id,
                ReferenceApplication.worldHandler!!.active!!.instanceId,
                contentId
            )
        )
        ReferenceApplication.worldHandler!!.triggerAction(id, Action.HIDE)
    }

    private fun playTvChannel(tvChannel: TvChannel) {
        tvModule.changeChannel(tvChannel, object : IAsyncCallback {
            override fun onSuccess() {
                ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            }

            override fun onFailed(error: Error) {
                if (error.message != "t56") {
                    showToast("Failed to start ${tvChannel.name} playback")                }
            }
        })
    }
}