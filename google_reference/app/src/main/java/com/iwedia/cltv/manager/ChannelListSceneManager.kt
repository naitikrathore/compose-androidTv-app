package com.iwedia.cltv.manager

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.BuildConfig
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.FastZapBannerDataProvider
import com.iwedia.cltv.anoki_fast.epg.FastLiveTabDataProvider
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.entities.DialogSceneData
import com.iwedia.cltv.entities.FilterItem
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.CategoryInterface
import com.iwedia.cltv.platform.`interface`.ClosedCaptionInterface
import com.iwedia.cltv.platform.`interface`.EpgInterface
import com.iwedia.cltv.platform.`interface`.FavoritesInterface
import com.iwedia.cltv.platform.`interface`.GeneralConfigInterface
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.PvrInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TimeInterface
import com.iwedia.cltv.platform.`interface`.TimeshiftInterface
import com.iwedia.cltv.platform.`interface`.TvInputInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.`interface`.WatchlistInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.category.Category
import com.iwedia.cltv.platform.model.favorite.FavoriteItem
import com.iwedia.cltv.platform.model.favorite.FavoriteItemType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.recording.RecordingInProgress
import com.iwedia.cltv.platform.model.recording.ScheduledReminder
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.scene.channel_list.ChannelListItem
import com.iwedia.cltv.scene.channel_list.ChannelListScene
import com.iwedia.cltv.scene.channel_list.ChannelListSceneListener
import com.iwedia.cltv.scene.home_scene.HomeSceneData
import com.iwedia.cltv.scene.zap_digit.DigitZapItem
import com.iwedia.cltv.utils.Utils
import data_type.GList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import utils.information_bus.Event
import world.SceneData
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.concurrent.TimeUnit


class ChannelListSceneManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var tvModule: TvInterface,
    var epgModule: EpgInterface,
    var pvrModule: PvrInterface,
    var favoritesModule: FavoritesInterface,
    val playerModule: PlayerInterface,
    val timeshiftModule: TimeshiftInterface,
    val utilsModule : UtilsInterface,
    val parentalControlSettingsModule: ParentalControlSettingsInterface,
    val tvInputModule: TvInputInterface,
    val watchlistModule: WatchlistInterface,
    val closedCaptionModule: ClosedCaptionInterface,
    val timeModule: TimeInterface,
    val categoryModule: CategoryInterface,
    val generalConfigModule: GeneralConfigInterface,
    private val textToSpeechModule: TTSInterface
) :
    ReferenceSceneManager(
        context,
        worldHandler, ReferenceWorldHandler.SceneId.CHANNEL_SCENE
    ), ChannelListSceneListener {

    init {
        isScreenFlowSecured = false
    }

    var deletedChannelList: MutableList<TvChannel> = mutableListOf()

    private val TAG = javaClass.simpleName

    //Filter item list
    var filterItemList = ArrayList<Category>()

    //Currently selected list category
    var activeCategoryId = FilterItem.ALL_ID

    //Tv event list
    val sceneChannelList = Collections.synchronizedList(mutableListOf<ChannelListItem>())

    var currentEventDetailsUpdater: CurrentEventDetailsUpdater? = null

    var activeTvChannel: TvChannel ?= null
    var openEditChannel = false

    override fun createScene() {
        scene = ChannelListScene(context!!, this)
        registerGenericEventListener(Events.CHANNEL_CHANGED)
        registerGenericEventListener(Events.FAVORITE_LIST_UPDATED)
        registerGenericEventListener(Events.CHANNELS_LOADED)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onTimeChanged(currentTime: Long) {
        //Refresh current event details every minute
        if (TimeUnit.MILLISECONDS.toSeconds(currentTime) % 60 == 0L) {
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeChanged onFailed: ${error.message}")
                }

                override fun onReceive(data: TvChannel) {
                    activeTvChannel = data
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onTimeChanged onReceive: ${Date(getCurrentTime(data))}")
                }

            })
            activeTvChannel?.let { getCurrentTime(it) }
                ?.let { (scene as ChannelListScene).refreshCurrentEvent(it) }
        }
    }

    override fun collectData(callback: IDataCallback) {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                callback.onDataCanceled()
            }

            override fun onReceive(data: TvChannel) {
                this@ChannelListSceneManager.activeTvChannel = data
            }
        })

        if(filterItemList.isNotEmpty())  { // Data collected already
            callback.onDataCollected()
            return
        }

        categoryModule.getAvailableFilters(object :
            IAsyncDataCallback<ArrayList<Category>> {
            override fun onFailed(error: Error) {
                callback.onDataCanceled()
            }

            override fun onReceive(data: ArrayList<Category>) {
                filterItemList.clear()
                filterItemList.addAll(data)
                ReferenceApplication.runOnUiThread(Runnable{
                    callback.onDataCollected()
                })

            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onSceneInitialized() {
        favoritesModule.setup()
        ReferenceApplication.runOnUiThread {
            scene!!.refresh(filterItemList)

            if (categoryModule.getActiveCategory().isEmpty()) {
                resetChannelList()
            } else {
                var item = filterItemList.find { it.name.equals(categoryModule.getActiveCategory()) }
                var index = filterItemList.indexOf(item)
                if (index != -1) {
                    (scene as ChannelListScene).setActiveCategory(index)
                }else{
                    resetChannelList()
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun resetChannelList(){
        (scene as ChannelListScene).setActiveCategory(0)
        onCategoryChannelClicked(0)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onEventReceived: event ${event!!.type}")
        if (event?.type == Events.CHANNEL_CHANGED) {
            activeTvChannel = event.getData(0) as TvChannel
            (scene as ChannelListScene).widget?.onActiveChannelChanged(activeTvChannel!!)
        } else if (event?.type == Events.FAVORITE_LIST_UPDATED) {
            refreshCategoryList()
            (scene as ChannelListScene).refreshFavButton(tvModule.getChannelList().toMutableList())
        }else if(event?.type == Events.CHANNELS_LOADED){
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                    Log.d(TAG, "onTimeChanged onFailed: ${error.message}")
                }

                override fun onReceive(data: TvChannel) {
                    activeTvChannel = data
                    (scene as ChannelListScene).widget?.onActiveChannelChanged(activeTvChannel!!)
                }

            })
            if(openEditChannel) {
                refreshCategoryList()
            }
        }
    }

    override fun getRecordingInProgress(callback: IAsyncDataCallback<RecordingInProgress>) {
        pvrModule!!.getRecordingInProgress(object : IAsyncDataCallback<RecordingInProgress>{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
            }

            override fun onReceive(data: RecordingInProgress) {
                callback.onReceive(data)
            }

        })
    }

    override fun getActiveCategory(): String {
        return categoryModule.getActiveCategory()
    }

    override fun getDateTimeFormat(): DateTimeFormat {
        return utilsModule.getDateTimeFormat()
    }

    override fun isPvrPathSet(): Boolean {
        return utilsModule.getPvrStoragePath().isNotEmpty()
    }

    override fun isUsbFreeSpaceAvailable(): Boolean {
        return utilsModule.isUsbFreeSpaceAvailable()
    }

    override fun isUsbStorageAvailable(): Boolean {
        return utilsModule.getUsbDevices().isNotEmpty()
    }
    override fun isUsbWritableReadable(): Boolean {
        return utilsModule.isUsbWritableReadable()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onClickEditChannel() {

        val sceneData = HomeSceneData(id, instanceId)
        sceneData.initialFilterPosition = 3
        sceneData.openEditChannel = true
        openEditChannel = true

        ReferenceApplication.worldHandler!!.destroyOtherExistingList(GList<Int>().apply {
            add(ReferenceWorldHandler.SceneId.LIVE)
            add(id)
        })

        (scene as ChannelListScene).view?.view?.alpha = 0F

        ReferenceApplication.worldHandler?.triggerActionWithData(
            ReferenceWorldHandler.SceneId.HOME_SCENE,
            Action.SHOW_OVERLAY,
            sceneData
        )
    }

    override fun getConfigInfo(nameOfInfo: String): Boolean {
        return generalConfigModule.getGeneralSettingsInfo(nameOfInfo)
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun setSpeechTextForSelectableView(vararg text: String, importance: SpeechText.Importance, type: Type, isChecked: Boolean) {
        textToSpeechModule.setSpeechTextForSelectableView(*text, importance = importance, type = type, isChecked = isChecked)
    }

    override fun showToast(text: String, duration: UtilsInterface.ToastDuration) {
        utilsModule.showToast(text, duration)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        /**
         * This as well is needed, as if we dont do anything and come back we wont get channel_loaded callback and channel list doesnt appear
         */
        if((scene as ChannelListScene).widget!!.isPerformingChannelEdit) {
            (scene as ChannelListScene).view?.view?.alpha = 1F
            refreshCategoryList()
        }
        openEditChannel = false
    }

    override fun isEventLocked(tvEvent: TvEvent?) = parentalControlSettingsModule.isEventLocked(tvEvent)

    override fun onCategoryChannelClicked(position: Int) {
        try {
            activeCategoryId = filterItemList[position]!!.id
            categoryModule.filterChannels(
                filterItemList[position]!!,
                object : IAsyncDataCallback<ArrayList<TvChannel>> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: ArrayList<TvChannel>) {
                        getChannelList(data, filterItemList[position])
                    }
                }
            )
        }catch (e: Exception){
            println(e.message)
        }
    }

    fun isNumericItem(channelList: ArrayList<ChannelListItem>): Boolean {
        val regex = "-?[0-9]+(\\.[0-9]+)?".toRegex()
        channelList.toList().forEach { it ->
            var isNum = it.channel.displayNumber.matches(regex)
            if(!isNum){
                return false
            }
        }
        return true
    }

    override fun sortChannelList(channelItemList: MutableList<ChannelListItem>): MutableList<ChannelListItem> {
        var platformName = (worldHandler as ReferenceWorldHandler).getPlatformName()
        if (platformName.contains(("RefPlus"))) {
            val sortedChannels = channelItemList.sortedWith(
                compareBy(
                    { it.channel.displayNumber.split("-")[0].toInt() },
                    { it.channel.displayNumber.split("-").getOrElse(1) { "0" }.toInt() })
            ) as MutableList<ChannelListItem>
            return sortedChannels
        } else if (platformName.contains("MediaTeK")) {
            val newChannelList = mutableListOf<ChannelListItem>()
            channelItemList.forEach { ChannelItem ->
                if(ChannelItem.channel.displayNumber.length == 4){
                    ChannelItem.channel.displayNumber = ChannelItem.channel.displayNumber.replace("-",".0")
                    newChannelList.add(ChannelItem)
                }else{
                    ChannelItem.channel.displayNumber = ChannelItem.channel.displayNumber.replace("-",".")
                    newChannelList.add(ChannelItem)
                }
            }
            newChannelList.sortBy{ it.channel.displayNumber.replace("-",".").toDouble() }
            val sortedChannelList = mutableListOf<ChannelListItem>()
            newChannelList.forEach { ChannelItem ->
                ChannelItem.channel.displayNumber = ChannelItem.channel.displayNumber.replace(".0","-")
                ChannelItem.channel.displayNumber = ChannelItem.channel.displayNumber.replace(".","-")
                sortedChannelList.add(ChannelItem)
            }
            return newChannelList
        } else if (BuildConfig.FLAVOR.equals("rtk")) {
            val broadcastChannelList = mutableListOf<ChannelListItem>()
            channelItemList.forEach { listItem ->
                if (listItem.channel.inputId.contains("realtek", ignoreCase = true)) {
                    broadcastChannelList.add(listItem)
                }
            }
            if (isNumericItem(ArrayList(broadcastChannelList))) {
                broadcastChannelList.sortBy { it.channel.displayNumber.toDouble() }
            } else {
                broadcastChannelList.sortBy { it.channel.displayNumber }
            }
            return broadcastChannelList
        } else {
            val fastChannelsList = mutableListOf<ChannelListItem>()
            val broadcastChannelList = mutableListOf<ChannelListItem>()
            channelItemList.forEach { listItem ->
                if(listItem.channel.inputId.contains("anoki", ignoreCase = true)) {
                    fastChannelsList.add(listItem)
                }else{
                    broadcastChannelList.add(listItem)
                }
            }
            fastChannelsList.sortBy { it.channel.ordinalNumber }
            broadcastChannelList.sortBy { it.channel.displayNumber }
            if(isNumericItem(ArrayList(broadcastChannelList))) {
                broadcastChannelList.sortedWith(compareBy { it.channel.displayNumber.toInt() })
            }else{
                broadcastChannelList.sortedWith(compareBy { it.channel.displayNumber })
                broadcastChannelList.sortedWith(compareBy (
                    { it.channel.displayNumber.split("-")[0].toInt() }, { it.channel.displayNumber.split("-").getOrElse(1) { "0" }.toInt() }))
            }
            channelItemList.clear()
            channelItemList.addAll(broadcastChannelList)
            channelItemList.addAll(fastChannelsList)
            return channelItemList
        }

    }

    override fun onSearchClicked() {
        var sceneData = SceneData(id, instanceId)
        worldHandler!!.triggerAction(
            id, Action.HIDE
        )
        worldHandler!!.triggerActionWithData(
            ReferenceWorldHandler.SceneId.SEARCH, Action.SHOW_OVERLAY, sceneData
        )
    }

    override fun onAddFavoritesClicked(tvChannel: TvChannel, favListIds: ArrayList<String>) {
        var favItem = FavoriteItem(1, FavoriteItemType.TV_CHANNEL, tvChannel, tvChannel, favListIds)
        favoritesModule.updateFavoriteItem(favItem,
            object : IAsyncCallback {
                override fun onFailed(error: Error) {}

                @RequiresApi(Build.VERSION_CODES.R)
                override fun onSuccess() {}
            })
    }

    override fun getFavoritesCategory(callback: IAsyncDataCallback<ArrayList<String>>) {
        favoritesModule.getAvailableCategories(callback)
    }

    private fun refreshCategoryList() {
        categoryModule.getAvailableFilters(object : IAsyncDataCallback<ArrayList<Category>> {
            override fun onFailed(error: Error) {
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onReceive(data: ArrayList<Category>) {
                ReferenceApplication.runOnUiThread {
                    filterItemList.clear()
                    filterItemList.addAll(data)
                    (scene as ChannelListScene).refreshCategoryList(filterItemList)
                }
            }
        })
    }

    override fun digitPressed(digit: Int) {
        // Disable digits when the PinScene is visible
        if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.PIN_SCENE)) {
            return
        }
        ReferenceApplication.runOnUiThread {
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            val sceneData = SceneData(id, instanceId, DigitZapItem(digit))
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.DIGIT_ZAP,
                Action.SHOW_OVERLAY,
                sceneData
            )
        }
    }

    @SuppressLint("NewApi")
    override fun onChannelItemClicked(tvChannel: TvChannel) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "  onChannelItemClicked() : timeshiftModule.isTimeShiftActive: [ ${timeshiftModule.isTimeShiftActive} ]")
        categoryModule.setActiveCategory((scene as ChannelListScene).getActiveCategoryName())
        //Reset the next/prev channel zap order to selected list.
        var favGroupName = ""
        var tifCategoryName = ""
        var genreCategoryName = ""
        if (activeCategoryId == FilterItem.FAVORITE_ID) {
            favGroupName = categoryModule.getActiveCategory()
        }
        else if (activeCategoryId >= FilterItem.TIF_INPUT_CATEGORY && activeCategoryId < FilterItem.FAVORITE_ID) {
            tifCategoryName = categoryModule.getActiveCategory()
        } else if (activeCategoryId == FilterItem.GENRE_CATEGORY) {
            genreCategoryName = categoryModule.getActiveCategory()
        }
        tvModule.updateLaunchOrigin(activeCategoryId, favGroupName, tifCategoryName, genreCategoryName)
        if (timeshiftModule.isTimeShiftActive) {
            // Show time shift exit dialog before channel change
            context!!.runOnUiThread {
                var sceneData = DialogSceneData(id, instanceId)
                sceneData.type = DialogSceneData.DialogType.YES_NO
                sceneData.title = ConfigStringsManager.getStringById("timeshift_channel_change_msg")
                sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                    override fun onNegativeButtonClicked() {
                    }

                    override fun onPositiveButtonClicked() {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "  timeshiftModule!!.timeShiftStop() -> onSuccess() ")
                        ReferenceApplication.worldHandler!!.playbackState =
                            ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                        //destroying all scenes other than live scene will also destroy timeShiftScene and it will stop time shift from onDestroy of timeShiftScene.
                        ReferenceApplication.worldHandler!!.destroyOtherExisting(
                            ReferenceWorldHandler.SceneId.LIVE
                        )
                        timeshiftModule.setTimeShiftIndication(false)
                        changeChannel(tvChannel)
                    }
                }
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                    Action.SHOW, sceneData
                )
            }
        }
        else {
            changeChannel(tvChannel)
            ReferenceApplication.worldHandler!!.destroyOtherExisting(
                ReferenceWorldHandler.SceneId.LIVE)
        }
    }

    private fun changeChannel(tvChannel: TvChannel) {
        worldHandler!!.destroySpecific(id,instanceId)
        tvModule.changeChannel(tvChannel, object: IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "  tvModule!!.changeChannel() -> Failed : [ ${error.message} ]")
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onSuccess() {}
        })
    }

    override fun getChannelSourceType(tvChannel: TvChannel): String{
        return tvModule.getChannelSourceType(tvChannel)
    }

    override fun getCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun isCCTrackAvailable(): Boolean {
        return closedCaptionModule.isCCTrackAvailable()
    }
    
    override fun getInstalledRegion(): Region {
        return utilsModule.getRegion()
    }

    override fun onBackPressed(): Boolean {
        if (worldHandler!!.isVisible(ReferenceWorldHandler.SceneId.DIALOG_SCENE)) {
            return false
        }
        ReferenceApplication.runOnUiThread(Runnable {
            worldHandler!!.triggerAction(id, Action.DESTROY)
        })
        return true
    }

    /*
           Tv event should not be null, if there is no data we need pass no info data so that recordings can be started
            */
    private fun getChannelList(data: ArrayList<TvChannel>,category: Category) {

        currentEventDetailsUpdater?.destroy()

        sceneChannelList.clear()

        if (activeTvChannel != null) {
            var foundMtkInput = false
            run exitForEach@{
                data.forEach { channel ->
                    val channelItem = ChannelListItem(channel, TvEvent.createNoInformationEvent(channel,timeModule.getCurrentTime(channel)))
                    channelItem.isCurrentChannel =
                        activeTvChannel!!.channelId == channel.channelId

                    if (!(channel.packageName.lowercase().contains("com.mediatek.tvinput"))
                        && !(channel.packageName.lowercase().contains("com.realtek.dtv"))
                        && !(channel.packageName.lowercase().contains("com.mediatek.dtv.tvinput"))
                        && !(channel.packageName.lowercase().contains("com.iwedia.tvinput"))
                        && !(channel.packageName.lowercase().contains("com.mediatek.tis"))
                    ) {
                        if (deletedChannelList.contains(channel)) return@exitForEach
                        sceneChannelList.add(channelItem)
                    } else {
                        if (channel.isBrowsable) {
                            if (deletedChannelList.contains(channel)) return@exitForEach
                            sceneChannelList.add(channelItem)
                        }
                    }

                    if (channel.packageName.lowercase().contains("com.mediatek.tvinput") ||
                        channel.packageName.lowercase().contains("com.mediatek.dtv.tvinput") ||
                        channel.packageName.lowercase().contains("com.mediatek.tis")
                    ) {
                        foundMtkInput = true
                    }
                }
            }
            //simulate behaviour of MTK application, put current analog service to ALL service list
            if ((category.id == FilterItem.ALL_ID) && foundMtkInput && activeTvChannel?.type?.contains("DVB") != true) {
                if(!(activeTvChannel!!.isBrowsable)) {
                    var channelListItem = ChannelListItem(activeTvChannel!!, TvEvent.createNoInformationEvent(activeTvChannel!!,timeModule.getCurrentTime(activeTvChannel!!)))
                    channelListItem.isCurrentChannel = true
                    sceneChannelList.add(channelListItem)
                }
            }        }

        var focusChannelIndex = 0

        run exitForEach@{
            sceneChannelList.forEach { channelItem ->
                if (channelItem.isCurrentChannel) {
                    focusChannelIndex = sceneChannelList.indexOf(channelItem)
                    return@exitForEach
                }
            }
        }

        currentEventDetailsUpdater = CurrentEventDetailsUpdater(focusChannelIndex,
            object : CurrentEventDetailsUpdaterListener {
                override fun onUpdateSelectedEvent() {
                    //update
                    ReferenceApplication.runOnUiThread {
                        scene!!.refresh(sceneChannelList)
                    }
                }
            }
        )
    }

    interface CurrentEventDetailsUpdaterListener {
        fun onUpdateSelectedEvent()
    }

    /*
    * To Update Current Event Details for channels list starting from selected channel
    * */
    inner class CurrentEventDetailsUpdater(
        index: Int,
        val listener: CurrentEventDetailsUpdaterListener
    ) {

        var isDestroyed = false

        init {
            utilsModule.runCoroutine({
                updateCurrentEventDetails(null, index)
            })
        }

        fun createTuneToChannelForMoreInfoEvent(tvChannel: TvChannel): TvEvent {
            val calendar = Calendar.getInstance()
            val currentTime = getCurrentTime(tvChannel)
            calendar.time = Date(currentTime)
            calendar.add(Calendar.DATE, 0)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            var startDate = Date(calendar.time.time)

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            var endDate = Date(calendar.time.time)
            return TvEvent.createTuneToChannelForMoreInformationEvent(
                tvChannel,
                startDate.time,
                endDate.time,
                currentTime,
                ConfigStringsManager.getStringById("tune_to") + " ${tvChannel.name} " +
                        ConfigStringsManager.getStringById("channel_for_more_info")
            )
        }

        fun updateCurrentEventDetails(isPositiveDirection: Boolean?, position: Int) {
            if (!(position >= 0 && position <= sceneChannelList.lastIndex)) return

            val channel = sceneChannelList.get(position).channel

           epgModule!!.getCurrentEvent(
                channel,
                object : IAsyncDataCallback<TvEvent> {
                    override fun onFailed(error: Error) {
                        if (isDestroyed) return
                        continueNext()
                    }

                    override fun onReceive(data: TvEvent) {
                        if (isDestroyed) return
                        var tvEvent = data
                        val isActiveChannel = TvChannel.compare(activeTvChannel!!, channel)
                        //Add tune to channel for more information event
                        if (BuildConfig.FLAVOR.contains("mtk") &&
                            !isActiveChannel && data.name == ConfigStringsManager.getStringById("no_information")) {
                            tvEvent = createTuneToChannelForMoreInfoEvent(channel)
                        }
                        val channelItem = ChannelListItem(channel, tvEvent)
                        channelItem.isCurrentChannel = sceneChannelList[position].isCurrentChannel
                        sceneChannelList[position] = channelItem
                        //update
                        ReferenceApplication.runOnUiThread {
                            if(scene != null) {
                                scene!!.refresh(channelItem)
                            }
                        }
                        continueNext();
                    }

                    private fun continueNext() {
                        Log.d(Constants.LogTag.CLTV_TAG + "pos_pos", "" + position)
                        if (isPositiveDirection == null) {
                            listener.onUpdateSelectedEvent()
                            updateCurrentEventDetails(true, position + 1)
                            updateCurrentEventDetails(false, position - 1)
                        } else {
                            updateCurrentEventDetails(
                                isPositiveDirection,
                                if (isPositiveDirection) position + 1 else position - 1
                            )
                        }
                    }
                }
            )
        }

        fun destroy() {
            isDestroyed = true
        }
    }


    override fun saveSelectedSortListPosition(position: Int) {
        utilsModule.setPrefsValue("sorted_channel_list", position)
    }

    override fun getSelectedSortListPosition(): Int {
        return utilsModule.getPrefsValue("sorted_channel_list", 0) as Int
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startRecording(tvEvent: TvEvent) {
        worldHandler!!.triggerAction(ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY)
        if (getActiveChannel() == tvEvent.tvChannel && playerModule.isOnLockScreen) {
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            return
        }
        if (getActiveChannel() != tvEvent.tvChannel && tvEvent.tvChannel.isLocked) {
            showToast(ConfigStringsManager.getStringById("unlock_channel_to_start_recording"))
            return
        }

        if (utilsModule.getUsbDevices().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("usb_not_connected_connect_usb_to_record"))
            return
        }
        if (utilsModule.getPvrStoragePath().isEmpty()) {
            showToast(ConfigStringsManager.getStringById("please_select_storage_path_for_recording_pvr"))
            utils.information_bus.InformationBus.submitEvent(Event(Events.SHOW_DEVICE_INFO))
            return
        }
        if (!utilsModule.isUsbWritableReadable()) {
            showToast(ConfigStringsManager.getStringById("usb_storage_corrupted"))
            return
        }
        if (!utilsModule.isUsbFreeSpaceAvailable()) {
            showToast(ConfigStringsManager.getStringById("insufficient_disk_space"))
            return
        }
        if (getActiveChannel().id == tvEvent.tvChannel.id) {
            startRecordingByChannel(tvEvent.tvChannel)
        } else {
            tvModule.changeChannel(tvEvent.tvChannel, object : IAsyncCallback {
                override fun onFailed(error: Error) {}

                override fun onSuccess() {
                    startRecordingByChannel(tvEvent.tvChannel)
                }
            }, ApplicationMode.DEFAULT)
        }
    }

    private fun startRecordingByChannel(tvChannel: TvChannel) {
        pvrModule.startRecordingByChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {}

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onSuccess() {
                (scene as ChannelListScene).widget?.refreshRecordButton()
                ReferenceApplication.worldHandler!!.destroyOtherExisting(
                    ReferenceWorldHandler.SceneId.LIVE
                )
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun stopRecording(tvEvent: TvEvent) {
        InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHING)
        pvrModule.stopRecordingByChannel(tvEvent.tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(TAG, "stopRecording: Failed ${error.message}")
                InformationBus
                    .informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
            }

            override fun onSuccess() {
                worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                (scene as ChannelListScene).widget?.refreshRecordButton()
                InformationBus.informationBusEventListener.submitEvent(Events.PVR_RECORDING_FINISHED)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRecordButtonPressed(tvEvent: TvEvent) {
        if (tvModule.isSignalAvailable()) {
            if (!pvrModule.isRecordingInProgress()) {
                if (!timeshiftModule.isTimeShiftActive) {
                    // below snippet is added to save last selected filter from EPG.k
                    utilsModule.setPrefsValue(UtilsInterface.APPLICATION_MODE, 0)
                    categoryModule.setActiveCategory((scene as ChannelListScene).getActiveCategoryName())
                    var favGroupName = ""
                    var tifCategoryName = ""
                    var genreCategoryName = ""
                    if (activeCategoryId == FilterItem.FAVORITE_ID) {
                        favGroupName = categoryModule.getActiveCategory()
                    } else if (activeCategoryId >= FilterItem.TIF_INPUT_CATEGORY && activeCategoryId < FilterItem.FAVORITE_ID) {
                        tifCategoryName = categoryModule.getActiveCategory()
                    } else if (activeCategoryId == FilterItem.GENRE_CATEGORY) {
                        genreCategoryName = categoryModule.getActiveCategory()
                    }
                    tvModule.updateLaunchOrigin(
                        activeCategoryId, favGroupName, tifCategoryName, genreCategoryName
                    )
                    startRecording(tvEvent)
                } else {
                    InformationBus.informationBusEventListener.submitEvent(
                        Events.SHOW_RECORDING_TIME_SHIFT_CONFLICT_DIALOG,
                        arrayListOf(tvEvent)
                    )
                }
            } else {
                val isActiveChannel = TvChannel.compare(tvEvent.tvChannel, getActiveChannel())
                if (isActiveChannel) stopRecording(tvEvent)
                else {
                    utilsModule.runCoroutine({
                        val sceneData = DialogSceneData(id, instanceId)
                        sceneData.type = DialogSceneData.DialogType.YES_NO
                        sceneData.title =
                            ConfigStringsManager.getStringById("recording_exit_msg")
                        sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                        sceneData.negativeButtonText =
                            ConfigStringsManager.getStringById("cancel")
                        sceneData.dialogClickListener =
                            object : DialogSceneData.DialogClickListener {
                                override fun onNegativeButtonClicked() {
                                    worldHandler!!.triggerAction(
                                        ReferenceWorldHandler.SceneId.DIALOG_SCENE,
                                        Action.DESTROY
                                    )
                                }

                                override fun onPositiveButtonClicked() {
                                    val recordingChannel =
                                        pvrModule.getRecordingInProgressTvChannel()
                                    pvrModule.stopRecordingByChannel(
                                        recordingChannel!!,
                                        object : IAsyncCallback {
                                            override fun onFailed(error: Error) {
                                                Log.d(
                                                    ReferenceApplication.TAG,
                                                    "stop recording failed"
                                                )
                                            }

                                            override fun onSuccess() {
                                                stopRecording(tvEvent)
                                                startRecording(tvEvent)
                                            }
                                        })
                                }
                            }
                        worldHandler!!.triggerActionWithData(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW, sceneData
                        )
                    }, Dispatchers.Main)
                }
            }
        } else showToast(ConfigStringsManager.getStringById("failed_to_start_pvr"))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onChannelListEmpty() {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
                // Stop live playback and show channels scan dialog
                utils.information_bus.InformationBus.submitEvent(Event(Events.CHANNEL_LIST_IS_EMPTY))
            }

            override fun onReceive(data: TvChannel) {
                //Set application mode 1 for FAST ONLY mode
                FastLiveTabDataProvider.utilsModule!!.setPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.FAST_ONLY.ordinal)
                FastLiveTabDataProvider.tvModule!!.changeChannel(data, object : IAsyncCallback {
                    override fun onFailed(error: Error) {}

                    override fun onSuccess() {}
                }, ApplicationMode.FAST_ONLY)

                CoroutineScope(Dispatchers.Main).launch {
                    val intentHide = Intent(FastZapBannerDataProvider.FAST_HIDE_ZAP_BANNER_INTENT)
                    ReferenceApplication.applicationContext().sendBroadcast(intentHide)
                    val intentShow = Intent(FastZapBannerDataProvider.FAST_SHOW_ZAP_BANNER_INTENT)
                    ReferenceApplication.applicationContext().sendBroadcast(intentShow)
                    ReferenceApplication.worldHandler?.triggerAction(ReferenceWorldHandler.SceneId.CHANNEL_SCENE, Action.DESTROY)
                }
            }
        },ApplicationMode.FAST_ONLY)
    }

    private fun getChannelSearchIntent(): Intent {
        var intent = Intent()
        if (BuildConfig.FLAVOR == "rtk")
            intent = Intent("android.settings.SETTINGS")
        else if (BuildConfig.FLAVOR == "refplus5") {
            intent.setClassName(
                "com.android.tv.settings",
                "com.mediatek.tv.settings.channelsetting.ChannelActivity"
            )
        }
        return intent
    }

    override fun getAvailableAudioTracks(): List<IAudioTrack> {
        return playerModule.getAudioTracks()
    }

    override fun getAvailableSubtitleTracks(): List<ISubtitle> {
        return playerModule.getSubtitleTracks()
    }

    override fun addDeletedChannel(tvChannel: TvChannel) {
        deletedChannelList.add(tvChannel)
    }

    override fun deleteChannel(tvChannel: TvChannel): Boolean {

        // Clearing the favorites of the TvChannel
        favoritesModule.updateFavoriteItem(FavoriteItem(0,FavoriteItemType.TV_CHANNEL,null,tvChannel, arrayListOf()),object :IAsyncCallback{
            override fun onFailed(error: Error) {
                // Ignore
            }

            override fun onSuccess() {
                // Ignore
            }
        })

        return tvModule.deleteChannel(tvChannel)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun lockUnlockChannel(
        tvChannel: TvChannel,
        lockUnlock: Boolean,
        callback: IAsyncCallback
    ) {
        var applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        tvModule.lockUnlockChannel(tvChannel, lockUnlock, callback, applicationMode)
    }

    override fun skipUnskipChannel(tvChannel: TvChannel, skipUnskip: Boolean): Boolean {
        return tvModule.skipUnskipChannel(tvChannel, skipUnskip
        )
    }

    override fun getActiveChannel(): TvChannel {
        return activeTvChannel!!
    }

    override fun onDestroy() {
        if (deletedChannelList.isNotEmpty()) {
            deletedChannelList.clear()
        }
        if (filterItemList.isNotEmpty()) {
            filterItemList.clear()
        }
        if (sceneChannelList.isNotEmpty()) {
            sceneChannelList.clear()
        }
        currentEventDetailsUpdater?.destroy()
        if (currentEventDetailsUpdater != null) {
            currentEventDetailsUpdater = null
        }
        activeTvChannel = null
        super.onDestroy()
    }

    override fun initConfigurableKeys() {
    }

    override fun getIsAudioDescription(type: Int): Boolean {
        return playerModule!!.getIsAudioDescription(type)
    }

    override fun getIsDolby(type: Int): Boolean {
        return playerModule!!.getIsDolby(type)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getIsHOH(type: Int): Boolean {
        return playerModule!!.getIsCC(type) //in old arch hoh was labeled as cc - i don't know why
    }

    override fun getDolbyType(type: Int, trackId: String): String {
        return playerModule!!.getDolbyType(type, trackId)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getTeleText(type: Int): Boolean {
        return playerModule!!.getTeleText(type)
    }

    override fun isParentalEnabled(): Boolean {
        return tvInputModule.isParentalEnabled()
    }

    override fun getChannelList(): ArrayList<TvChannel> {
        return tvModule.getBrowsableChannelList()
    }

    override fun getWatchlist(): MutableList<ScheduledReminder>? {
        var watchlist: MutableList<ScheduledReminder>? = null
        watchlistModule.getWatchList(object: IAsyncDataCallback<MutableList<ScheduledReminder>>{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
            }

            override fun onReceive(data: MutableList<ScheduledReminder>) {
                watchlist = data
            }
        })
        return watchlist
    }

    override fun removeScheduledReminder(reminder: ScheduledReminder) {
        watchlistModule.removeScheduledReminder(reminder, object: IAsyncCallback{
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
            }

            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: Remove Scheduled Reminder Success")
            }
        })
    }

    override fun onActiveChannelDeleted() {
        tvModule.nextChannel(object: IAsyncCallback{
            override fun onFailed(error: Error) {
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "onFailed: $error", )
            }

            @RequiresApi(Build.VERSION_CODES.R)
            override fun onSuccess() {

            }

        })
    }

    override fun showRecordingStopPopUp(callback: IAsyncCallback) {
            utilsModule.runCoroutine({
                var sceneData = DialogSceneData(id, instanceId)
                sceneData.type = DialogSceneData.DialogType.YES_NO
                sceneData.title = ConfigStringsManager.getStringById("recording_exit_msg")
                sceneData.positiveButtonText = ConfigStringsManager.getStringById("ok")
                sceneData.negativeButtonText = ConfigStringsManager.getStringById("cancel")
                sceneData.dialogClickListener = object : DialogSceneData.DialogClickListener {
                    override fun onNegativeButtonClicked() {
                        worldHandler!!.triggerAction(
                            ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.DESTROY
                        )
                    }

                    override fun onPositiveButtonClicked() {
                        var tvChannel = pvrModule.getRecordingInProgressTvChannel()
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onPositiveButtonClicked: ${tvChannel?.name}")
                        try {
                            pvrModule.stopRecordingByChannel(tvChannel!!, object : IAsyncCallback {
                                override fun onFailed(error: Error) {
                                    Log.d(Constants.LogTag.CLTV_TAG + ReferenceApplication.TAG, "stop recording failed")
                                }

                                override fun onSuccess() {
                                    callback.onSuccess()
                                    ReferenceApplication.worldHandler!!.playbackState =
                                        ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                                    PvrBannerSceneManager.previousProgress = 0L
                                    worldHandler!!.destroyOtherExisting(
                                        ReferenceWorldHandler.SceneId.LIVE
                                    )
                                }
                            })
                        }catch (E: Exception){
                            E.printStackTrace()
                            ReferenceApplication.worldHandler!!.playbackState =
                                ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                            PvrBannerSceneManager.previousProgress = 0L
                            worldHandler!!.destroyOtherExisting(
                                ReferenceWorldHandler.SceneId.LIVE
                            )
                        }
                    }
                }
                worldHandler!!.triggerActionWithData(
                    ReferenceWorldHandler.SceneId.DIALOG_SCENE, Action.SHOW, sceneData
                )
            }, Dispatchers.Main)

    }
    override fun isClosedCaptionEnabled(): Boolean? {
        return closedCaptionModule?.isClosedCaptionEnabled()
    }

    override fun getClosedCaption(): String? {
        return closedCaptionModule?.getClosedCaption()
    }

    override fun setClosedCaption(): Int? {
        return closedCaptionModule?.setClosedCaption()
    }

    override fun getAudioChannelInfo(type: Int): String {
        val audioChannelIdx = playerModule.getAudioChannelIndex(type)
        return if (audioChannelIdx != -1)
            Utils.getAudioChannelStringArray()[audioChannelIdx]
        else ""
    }

    override fun getAudioFormatInfo(): String {
        return playerModule.getAudioFormat()
    }

    override fun getVideoResolution(): String {
        return playerModule.getVideoResolution()!!
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        val applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        return tvModule.getParentalRatingDisplayName(parentalRating,applicationMode, tvEvent)
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return timeModule.getCurrentTime(tvChannel)
    }

    override fun getCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
    }

    override fun isSubtitlesEnabled(): Boolean {
        return utilsModule.getSubtitlesState()
    }

    override fun stopRecordingByChannel(tvChannel: TvChannel, callback: IAsyncCallback) {
        pvrModule.stopRecordingByChannel(tvChannel, object : IAsyncCallback {
            override fun onFailed(error: Error) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: ${error.message}")
            }

            override fun onSuccess() {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onSuccess: Stop recording success")
                callback.onSuccess()
                ReferenceApplication.worldHandler!!.playbackState =
                    ReferenceWorldHandler.PlaybackState.PLAYBACK_LIVE
                PvrBannerSceneManager.previousProgress = 0L
                pvrModule.setRecIndication(false)
            }
        })
    }

    override fun isScrambled(): Boolean {
        return playerModule.playbackStatus.value == PlaybackStatus.SCRAMBLED_CHANNEL
    }
}
