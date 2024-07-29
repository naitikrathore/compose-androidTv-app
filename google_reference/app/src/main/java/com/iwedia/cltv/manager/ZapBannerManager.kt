package com.iwedia.cltv.manager

import android.os.Build
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import com.iwedia.cltv.MainActivity
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.anoki_fast.epg.BackFromPlayback
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.ConfigurableKeysManager
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.*
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.zap_banner_scene.ZapBannerKeyDpadLeftAction
import com.iwedia.cltv.scene.zap_banner_scene.ZapBannerKeyDpadRightAction
import com.iwedia.cltv.scene.zap_banner_scene.ZapBannerScene
import com.iwedia.cltv.scene.zap_banner_scene.ZapBannerSceneListener
import com.iwedia.cltv.utils.Utils
import kotlinx.coroutines.Dispatchers
import utils.information_bus.Event
import utils.information_bus.InformationBus
import world.SceneData


/**
 * Zap banner manager
 *
 * @author Veljko Ilkic
 */
class ZapBannerManager(
    context: MainActivity,
    worldHandler: ReferenceWorldHandler,
    var tvModule: TvInterface,
    var epgModule: EpgInterface,
    var playerModule: PlayerInterface,
    var utilsModule: UtilsInterface,
    var parentalControlSettingsModule: ParentalControlSettingsInterface,
    private var closedCaptionModule: ClosedCaptionInterface?,
    private var timeModule: TimeInterface,
    private var categoryModule: CategoryInterface,
    private val generalConfigModule: GeneralConfigInterface,
    private var textToSpeechModule: TTSInterface
) : ReferenceSceneManager(
    context,
    worldHandler, ReferenceWorldHandler.SceneId.ZAP_BANNER
), ZapBannerSceneListener {

    lateinit var zapDPadRight: ZapBannerKeyDpadRightAction
    lateinit var zapDPadLeft: ZapBannerKeyDpadLeftAction

    private var selectedChannelList: ArrayList<TvChannel> = arrayListOf()
    private var activeChannel: TvChannel? = null

    private var iter: Int = 0 // iteration tracker for channel change on long press
    private var tvChannelOnFocus: TvChannel? = null // active channel
    private var activeChannelIndex = 0 // index of active channel
    private var zapBanner = true // used for stopping multiple channel changes before info is updated
    private var keyboardShown = false
    var tvEvent : TvEvent? = null

    override fun initConfigurableKeys() {
        var zapRightData = ConfigurableKeysManager.getConfigurableKey("liveDPadRight")
        zapDPadRight = ZapBannerKeyDpadRightAction(
            this,
            zapRightData!!.description.toInt(),
            zapRightData.keyActionType
        )

        var zapLeftData = ConfigurableKeysManager.getConfigurableKey("liveDPadLeft")
        zapDPadLeft = ZapBannerKeyDpadLeftAction(
            this, zapLeftData!!.description.toInt(),
            zapLeftData.keyActionType
        )
    }

    override fun onTimeChanged(currentTime: Long) {}

    override fun resolveConfigurableKey(keyCode: Int, action: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (zapDPadRight.handleKey(action)) {
                    return true
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (zapDPadLeft.handleKey(action)) {
                    return true
                }
            }
        }

        return false
    }

    override fun createScene() {
        scene = ZapBannerScene(context!!, this)
        keyboardShown = false
        registerGenericEventListener(Events.CHANNEL_CHANGED)
        registerGenericEventListener(Events.AUDIO_TRACKS_UPDATED)
        registerGenericEventListener(Events.SUBTITLE_TRACKS_SCENE_REFRESH)
        registerGenericEventListener(Events.AUDIO_TRACKS_SCENE_REFRESH)
        registerGenericEventListener(Events.REFRESH_ZAP_BANNER)
        registerGenericEventListener(Events.UPDATE_INFO_BANNER)
        registerGenericEventListener(Events.VIDEO_RESOLUTION_AVAILABLE)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onEventReceived(event: Event?) {
        super.onEventReceived(event)
        if (event!!.type == Events.REFRESH_ZAP_BANNER || event!!.type == Events.UPDATE_INFO_BANNER) {
            onSceneInitialized()
        } else if (event?.type == Events.CHANNEL_CHANGED) {
            refreshSceneData(event.getData(0) as TvChannel)
        } else if (event!!.type == Events.AUDIO_TRACKS_UPDATED) {
            var audioTracks = playerModule.getAudioTracks()
            if (audioTracks.isNotEmpty()) {
                scene!!.refresh(audioTracks)
            }

            var subtitleTracks = playerModule.getSubtitleTracks()
            if (subtitleTracks.isNotEmpty()) {
                scene!!.refresh(subtitleTracks)
            }
        }else if (event.type == Events.VIDEO_RESOLUTION_AVAILABLE){
            if (scene != null && activeChannel != null) {
                (scene as ZapBannerScene).updateResolution(activeChannel!!, playerModule.getVideoResolution())
                (scene as ZapBannerScene).refresh(tvEvent)
            }
        }
    }

    override fun channelUp() {
        tvModule.nextChannel(object: IAsyncCallback {
            override fun onSuccess() {
                tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: TvChannel) {
                        tvChannelOnFocus = data
                        run exitForEach@{
                            selectedChannelList.forEachIndexed { index, tvChannel ->
                                if (TvChannel.compare(tvChannel, tvChannelOnFocus!!)) {
                                    activeChannelIndex = index
                                    return@exitForEach
                                }
                            }
                        }
                    }
                })
            }

            override fun onFailed(error: Error) {}
        })
    }

    override fun channelUpAfterLongPress() {
        tvModule.changeChannel(tvChannelOnFocus!!, object : IAsyncCallback {
            override fun onFailed(error: Error) {
            }

            override fun onSuccess() {
                iter = 0
                tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: TvChannel) {
                        tvChannelOnFocus = data
                        run exitForEach@{
                            selectedChannelList.forEachIndexed { index, tvChannel ->
                                if (TvChannel.compare(tvChannel, tvChannelOnFocus!!)) {
                                    activeChannelIndex = index
                                    return@exitForEach
                                }
                            }
                        }
                    }
                })
            }
        })
    }
    override fun channelUpZapBanner() {
        if(zapBanner) {
            zapBanner = false
            CoroutineHelper.runCoroutine({
                var tvChannelsSize = selectedChannelList.size
                if (activeChannel == null || tvChannelsSize == 0) {
                    zapBanner = true
                    return@runCoroutine
                }

                var desiredChannelIndex = activeChannelIndex + iter
                if (desiredChannelIndex >= tvChannelsSize) {
                    iter = 0
                    desiredChannelIndex = 0
                    activeChannelIndex = 0
                }
                tvChannelOnFocus = selectedChannelList[desiredChannelIndex]
                refreshSceneData(tvChannelOnFocus!!)
                iter += 1

                zapBanner = true
            })
        }
    }

    override fun lastActiveChannel() {
        categoryModule.setActiveCategory("All")
        categoryModule.setActiveEpgFilter(0)
        tvModule.updateLaunchOrigin(0, "", "", "")

        tvModule.getLastActiveChannel(object : IAsyncCallback {
            override fun onSuccess() {
            }

            override fun onFailed(error: Error) {
            }
        })
    }

    override fun collectData(callback: IDataCallback) {
        tvModule.getSelectedChannelList(object : IAsyncDataCallback<ArrayList<TvChannel>>{
            override fun onFailed(error: Error) {
                callback.onDataCollected()
            }

            override fun onReceive(data: ArrayList<TvChannel>) {
                for (it in data){
                    if(!it.isSkipped && (it.isBrowsable || utilsModule.isThirdPartyChannel(it))){
                        selectedChannelList.add(it)
                    }
                }
                callback.onDataCollected()
            }
        })
    }

    override fun channelDownZapBanner() {
        if(zapBanner) {
            zapBanner = false
            CoroutineHelper.runCoroutine({
                var tvChannelsSize = selectedChannelList.size
                if (activeChannel == null || tvChannelsSize == 0) {
                    zapBanner = true
                    return@runCoroutine
                }

                var desiredChannelIndex = activeChannelIndex - iter
                if (desiredChannelIndex <= -1) {
                    activeChannelIndex = tvChannelsSize - 1
                    iter = 0
                    desiredChannelIndex = tvChannelsSize - 1
                }
                tvChannelOnFocus = selectedChannelList[desiredChannelIndex]
                refreshSceneData(tvChannelOnFocus!!)
                iter += 1
                zapBanner = true
            })
        }
    }

    override fun getIsCC(type: Int): Boolean {
        return playerModule!!.getIsCC(type)
    }

    override fun getIsAudioDescription(type: Int): Boolean {
        return playerModule!!.getIsAudioDescription(type)
    }

    override fun getTeleText(type: Int): Boolean {
        return playerModule!!.getTeleText(type)
    }

    override fun getIsDolby(type: Int): Boolean {
        return playerModule!!.getIsDolby(type)
    }
    override fun isHOH(type: Int): Boolean {
        return playerModule!!.getIsCC(type) //in old arch hoh was labeled as cc - i don't know why
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
    override fun isChannelUnlocked(): Boolean {
        return playerModule.isChannelUnlocked
    }

    override fun isScrambled(): Boolean {
        return playerModule.playbackStatus.value == PlaybackStatus.SCRAMBLED_CHANNEL
    }

    override fun getVideoResolution(): String {
        return if (playerModule != null)  playerModule.getVideoResolution()!! else ""
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun getParentalRatingDisplayName(parentalRating: String?, tvEvent: TvEvent): String {
        val applicationMode =
            if (((worldHandler as ReferenceWorldHandler).getApplicationMode()) == ApplicationMode.FAST_ONLY.ordinal) ApplicationMode.FAST_ONLY else ApplicationMode.DEFAULT
        return tvModule.getParentalRatingDisplayName(parentalRating, applicationMode, tvEvent)
    }

    override fun isParentalEnabled() : Boolean {
        return tvModule.isParentalEnabled()
    }

    override fun getCurrentTime(tvChannel: TvChannel): Long {
        return timeModule.getCurrentTime(tvChannel)
    }

    override fun showVirtualKeyboard() {
        ReferenceApplication.runOnUiThread {
            if (!keyboardShown) {
                keyboardShown = true
                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.ZAP_BANNER, Action.DESTROY
                )
                worldHandler!!.triggerAction(
                    ReferenceWorldHandler.SceneId.RCU_SCENE,
                    Action.SHOW_OVERLAY
                )
            }
        }
    }

    override fun getCurrentSubtitleTrack(): ISubtitle? {
        return playerModule.getActiveSubtitle()
    }

    override fun isSubtitleEnabled(): Boolean {
        return utilsModule.getSubtitlesState()
    }

    override fun getNextEvent(tvChannel: TvChannel, callback: IAsyncDataCallback<TvEvent>) {
        epgModule.getNextEventByChannel(tvChannel,object :IAsyncDataCallback<TvEvent>{
            override fun onFailed(error: Error) {
                val dummyEvent = TvEvent(
                    TvEvent.DUMMY_EVENT_ID,
                    tvChannel,
                    ConfigStringsManager.getStringById("no_information"),
                    "",
                    "",
                    "",
                    -1L,
                    -1L,
                    arrayListOf(),
                    0,
                    0,
                    null,
                    "",
                    isProgramSame = false,
                    isInitialChannel = false,
                    providerFlag = 0
                )
                callback.onReceive(dummyEvent)
            }

            override fun onReceive(data: TvEvent) {
                callback.onReceive(data)
            }

        })
    }
    override fun getCurrentAudioTrack(): IAudioTrack? {
        return playerModule.getActiveAudioTrack()
    }

    override fun getAvailableAudioTracks(): List<IAudioTrack> {
        return playerModule.getAudioTracks()
    }

    override fun getDateTimeFormat():DateTimeFormat {
        return utilsModule.getDateTimeFormat()
    }

    override fun isEventLocked(tvEvent: TvEvent?) = parentalControlSettingsModule.isEventLocked(tvEvent)

    override fun getAvailableSubtitleTracks(): List<ISubtitle> {
        return playerModule.getSubtitleTracks()
    }

    override fun channelDown() {
        tvModule.previousChannel(object : IAsyncCallback {
            override fun onFailed(error: Error) {
            }

            override fun onSuccess() {
                tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: TvChannel) {
                        tvChannelOnFocus = data
                        run exitForEach@{
                            selectedChannelList.forEachIndexed { index, tvChannel ->
                                if (TvChannel.compare(tvChannel, tvChannelOnFocus!!)) {
                                    activeChannelIndex = index
                                    return@exitForEach
                                }
                            }
                        }
                    }
                })
            }
        })
    }

    override fun channelDownAfterLongPress() {
        tvModule.changeChannel(tvChannelOnFocus!!, object : IAsyncCallback {
            override fun onFailed(error: Error) {
            }

            override fun onSuccess() {
                iter = 0
                tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                    override fun onFailed(error: Error) {
                    }

                    override fun onReceive(data: TvChannel) {
                        tvChannelOnFocus = data
                        run exitForEach@{
                            selectedChannelList.forEachIndexed { index, tvChannel ->
                                if (TvChannel.compare(tvChannel, tvChannelOnFocus!!)) {
                                    activeChannelIndex = index
                                    return@exitForEach
                                }
                            }
                        }
                    }
                })
            }
        })
    }

    override fun showChannelList() {
        ReferenceApplication.runOnUiThread {
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
            var active = worldHandler!!.active
            var sceneData = SceneData(active!!.id, active!!.instanceId)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.CHANNEL_SCENE,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }


    override fun showInfoBanner() {
        if (ReferenceApplication.worldHandler!!.active!!.id != id) {
            return
        }

        ReferenceApplication.runOnUiThread {
            worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)

            var active = worldHandler!!.active
            var sceneData = SceneData(active!!.id, active!!.instanceId)
            worldHandler!!.triggerActionWithData(
                ReferenceWorldHandler.SceneId.INFO_BANNER,
                Action.SHOW_OVERLAY, sceneData
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun showHomeScene() {
        ReferenceApplication.runOnUiThread {
            BackFromPlayback.onOkPressed()
        }
    }


    override fun onSceneInitialized() {
        keyboardShown = false
        tvModule.getActiveChannel(object: IAsyncDataCallback<TvChannel> {
            override fun onReceive(data: TvChannel) {
                if (this@ZapBannerManager.data != null && this@ZapBannerManager.data!!.getDataList() != null && this@ZapBannerManager.data!!.getDataByIndex(0) is TvChannel) {
                    val tvChannel = this@ZapBannerManager.data!!.getDataByIndex(0) as TvChannel
                    if(tvChannel.isBrowsable) refreshSceneData(tvChannel) else refreshSceneData(data)
                }
            }
            override fun onFailed(error: Error) {}
        })
    }

    override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
        textToSpeechModule.setSpeechText(text = text,importance = importance)
    }

    override fun onBackPressed(): Boolean {
        if (worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.ZAP_BANNER) {
            worldHandler!!.triggerAction(id, Action.DESTROY)
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBackClicked() {
        ReferenceApplication.runOnUiThread {
            ReferenceApplication.worldHandler!!.triggerAction(
                ReferenceWorldHandler.SceneId.ZAP_BANNER,
                Action.DESTROY
            )
        }
        if (generalConfigModule.getGeneralSettingsInfo("timeshift")) {
            InformationBus.submitEvent(Event(Events.SHOW_PLAYER))
        }
    }

    override fun isClosedCaptionEnabled(): Boolean? {
        return closedCaptionModule?.isClosedCaptionEnabled()
    }

    override fun getClosedCaption(): String? {
        return closedCaptionModule?.getClosedCaption()
    }

    override fun isCCTrackAvailable(): Boolean? {
        return closedCaptionModule?.isCCTrackAvailable()
    }

    override fun getChannelSourceType(tvChannel: TvChannel) : String {
        return tvModule.getChannelSourceType(tvChannel)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onOKPressed() {
        if (worldHandler!!.active!!.id == ReferenceWorldHandler.SceneId.ZAP_BANNER) {
            worldHandler!!.triggerAction(id, Action.DESTROY)
        }
        showChannelList()
    }

    override fun showPlayer() {
        onBackPressed()
        InformationBus.submitEvent(Event(Events.SHOW_PLAYER))
    }

    private fun refreshSceneData(tvChannel: TvChannel) {
        activeChannel = tvChannel
        tvChannelOnFocus = tvChannel
        run exitForEach@{
            selectedChannelList.toList().forEachIndexed { index, tvChannel ->
                if (TvChannel.compare(tvChannel, tvChannelOnFocus!!)) {
                    activeChannelIndex = index
                    return@exitForEach
                }
            }
        }
        //Refresh channel info
        scene.let {
            CoroutineHelper.runCoroutine({
                it?.refresh(tvChannel)
            },Dispatchers.Main)
            //Refresh epg data info
            epgModule.getCurrentEvent(tvChannel, object: IAsyncDataCallback<TvEvent> {
                override fun onFailed(error: Error) {
                    ReferenceApplication.runOnUiThread {
                        var dummyEvent = TvEvent(
                            TvEvent.DUMMY_EVENT_ID,
                            tvChannel,
                            "",
                            "",
                            "",
                            "",
                            -1L,
                            -1L,
                            arrayListOf(),
                            0,
                            0,
                            null,
                            "",
                            isProgramSame = false,
                            isInitialChannel = false,
                            providerFlag = 0,
                            rrt5Rating = ""
                        )
                        scene!!.refresh(dummyEvent)
                        this@ZapBannerManager.tvEvent = dummyEvent
                    }
                }

                override fun onReceive(tvEvent: TvEvent) {
                    this@ZapBannerManager.tvEvent = tvEvent
                    CoroutineHelper.runCoroutine({
                        it?.refresh(tvEvent)
                    },Dispatchers.Main)
                }
            })
        }
    }
}