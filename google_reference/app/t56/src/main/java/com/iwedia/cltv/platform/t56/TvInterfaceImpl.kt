package com.iwedia.cltv.platform.t56

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
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
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.t56.provider.PlatformSpecificData
import com.mediatek.twoworlds.tv.MtkTvBanner
import com.mediatek.twoworlds.tv.MtkTvChannelList
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.MtkTvConfig
import com.mediatek.twoworlds.tv.MtkTvHighLevel
import com.mediatek.twoworlds.tv.common.MtkTvChCommonBase
import com.mediatek.twoworlds.tv.common.MtkTvConfigType
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase
import com.mediatek.twoworlds.tv.common.MtkTvNativeAppId
import com.mediatek.twoworlds.tv.common.MtkTvSvctxNotifyCode
import com.mediatek.twoworlds.tv.model.MtkTvChannelInfoBase
import com.mediatek.wwtv.tvcenter.util.TVAsyncExecutor
import kotlinx.coroutines.Dispatchers
import kotlin.concurrent.thread


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
    private var rrt5Rating: MtkTvBanner? = null

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

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                //if due to dial scan service list is updated we need to change to first
                //digital channel in TS
                com.iwedia.cltv.platform.t56.TvCallbackConst.MSG_CB_CHANNEL_LIST_UPDATE,
                com.iwedia.cltv.platform.t56.TvCallbackConst.MSG_CB_NFY_UPDATE_TV_PROVIDER_LIST-> {
                     Log.d(Constants.LogTag.CLTV_TAG + TAG, "Received service list update notification")
                     thread(start = true) {
                         Thread.sleep(5000)

                         CoroutineHelper.runCoroutine({
                             isDigitalFromAnalog = true
                             //only to refresh active channel, because on time of scan notification application service list
                             //is not yet update, it will be 10 seconds later
                             getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                                 override fun onFailed(error: Error) {
                                 }

                                 override fun onReceive(data: TvChannel) {
                                     Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "New active channel ${data.name}")
                                 }
                             })
                         }, Dispatchers.Main)
                     }
                 }
                com.iwedia.cltv.platform.t56.TvCallbackConst.MSG_CB_SVCTX_NOTIFY-> {
                    val currentMtkService =  MtkTvChannelListBase.getCurrentChannel()
                    if((msg.obj as com.iwedia.cltv.platform.t56.TvCallbackData).param1 == MtkTvSvctxNotifyCode.SVCTX_NTFY_CODE_SERVICE_CHANGING) {
                        if(currentMtkService != null) {
                            if(currentMtkService.isAnalogService) {
                                isAnalogServiceStarted = true
                            } else {
                                if(isAnalogServiceStarted) {
                                    isDigitalFromAnalog = true

                                    CoroutineHelper.runCoroutine({
                                        //only to refresh active channel when switched from Analog channel to Digital service
                                        getActiveChannel(object: IAsyncDataCallback<TvChannel> {
                                            override fun onFailed(error: Error) {
                                            }

                                            override fun onReceive(data: TvChannel) {
                                                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "New active channel ${data.name}")
                                            }
                                        })
                                    }, Dispatchers.Main)
                                }
                            }
                        }
                    } else if ((msg.obj as com.iwedia.cltv.platform.t56.TvCallbackData).param1 == MtkTvSvctxNotifyCode.SVCTX_NTFY_CODE_NO_AUDIO_VIDEO_SVC ||
                        (msg.obj as com.iwedia.cltv.platform.t56.TvCallbackData).param1 == MtkTvSvctxNotifyCode.SVCTX_NTFY_CODE_AUDIO_ONLY_SVC
                    ) {
                        InformationBus.informationBusEventListener?.submitEvent(
                            Events.NO_AV_OR_AUDIO_ONLY_CHANNEL,
                            arrayListOf((msg.obj as com.iwedia.cltv.platform.t56.TvCallbackData).param1)
                        )
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "MSG_CB_SVCTX_NOTIFY is ${(msg.obj as com.iwedia.cltv.platform.t56.TvCallbackData).param1} ")
                    }
                }
            }
        }
    }

    init {
        com.iwedia.cltv.platform.t56.TvCallbackHandler.getInstance()
            .addCallBackListener(com.iwedia.cltv.platform.t56.TvCallbackConst.MSG_CB_CHANNEL_LIST_UPDATE, mHandler)

        com.iwedia.cltv.platform.t56.TvCallbackHandler.getInstance()
            .addCallBackListener(com.iwedia.cltv.platform.t56.TvCallbackConst.MSG_CB_NFY_UPDATE_TV_PROVIDER_LIST, mHandler)

        com.iwedia.cltv.platform.t56.TvCallbackHandler.getInstance()
            .addCallBackListener(com.iwedia.cltv.platform.t56.TvCallbackConst.MSG_CB_SVCTX_NOTIFY, mHandler)

        com.iwedia.cltv.platform.t56.TvCallbackHandler.getInstance().addCallBackListener(com.iwedia.cltv.platform.t56.TvCallbackConst.MSG_CB_SVCTX_NOTIFY,mHandler)

        //MTK Voodoo magic, without this CC and digit tune do not work properly after OOBE(FTI)
        val mHighLevel = MtkTvHighLevel()
        mHighLevel.launchInternalApp(MtkTvNativeAppId.MTKTV_NATIVE_APP_ID_NAV)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "Initialized MTK High level TV interface")
        rrt5Rating = MtkTvBanner.getInstance()
    }

    override fun setupDefaultService(channels : List<TvChannel>, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.setupDefaultService(channels, applicationMode)
        } else {
            val firstChannelSet = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(KEY_PREFERRED_FIRST_CHANNEL_SET, false)
            if(!firstChannelSet) {
                val currentMtkService = MtkTvChannelListBase.getCurrentChannel()
                var index = 0
                var newActiveChannel = -1
                if (currentMtkService != null) {
                    for (channel in channels) {
                        try {
                            if ((channel.platformSpecific as PlatformSpecificData).internalServiceListID == currentMtkService.svlId &&
                                (channel.platformSpecific as PlatformSpecificData).internalServiceIndex == currentMtkService.svlRecId
                            ) {
                                newActiveChannel = index
                                Log.i(TAG , "Found fallback active service at $index")
                                break
                            }
                        } catch (E: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, E.toString())
                        } finally {
                            index++
                        }
                    }
                }else{
                    val currentMtkChannelId = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_NAV_AIR_CRNT_CH)
                    for (channel in channels) {
                        try {
                            if ((channel.platformSpecific as PlatformSpecificData).internalServiceChannelId == currentMtkChannelId) {
                                newActiveChannel = index
                                Log.i(TAG, "Found fallback active service with currentMtkChannelId is at $index name $channel")
                                break
                            }
                        } catch (e: Exception) {
                            Log.i(TAG, e.toString())
                        } finally {
                            index++
                        }
                    }
                }

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
                    utilsInterface.setPrefsValue("CurrentActiveChannel", newActiveChannel)
                    utilsInterface.setPrefsValue("CurrentActiveChannelId",channels[newActiveChannel].id)
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
            val activeChannel = utilsInterface.getPrefsValue("CurrentActiveChannel", 0) as Int
            var channels = getChannelList()

            //When SDK removes some services from DB, activeChannel could be out of range of DB
            //fallback to data from MTK SDK
            if (activeChannel >= channels.size) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Active channel over channel size $activeChannel ${channels.size}")
                var newActiveChannel = -1
                val currentMtkService =  MtkTvChannelListBase.getCurrentChannel()
                var index = 0
                if(currentMtkService != null) {
                    for (channel in channels) {
                        try {
                            if ((channel.platformSpecific as PlatformSpecificData).internalServiceListID == currentMtkService.svlId &&
                                (channel.platformSpecific as PlatformSpecificData).internalServiceIndex == currentMtkService.svlRecId
                            ) {
                                newActiveChannel = index
                                Log.d(Constants.LogTag.CLTV_TAG + TAG, "Found fallback active service at $index")
                                break
                            }
                        } catch (E: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, E.toString())
                        } finally {
                            index++
                        }
                    }
                }

                if(newActiveChannel != -1) {
                    utilsInterface.setPrefsValue("CurrentActiveChannel", newActiveChannel)
                    utilsInterface.setPrefsValue("CurrentActiveChannelId",channels[newActiveChannel].id)
                    callback.onReceive(channels[newActiveChannel])
                } else {
                    callback.onFailed(Error("Active channel not found."))
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Active channel $activeChannel not found, outside of channel list size")
                }
            }
            else {
                if(activeChannel < 0) {
                    if(channels.size == 0) {
                        callback.onFailed(Error("Active channel not found."))
                        Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Active channel not found, list size iz 0")
                    } else {
                        callback.onReceive(channels[0])
                    }
                }
                else {
                    var channel = channels[activeChannel]
                    val mtkChannel =  MtkTvChannelListBase.getCurrentChannel()

                    //Handle case when user changes to Analog channel number and middleware starts Digital service from Analog channel
                    if((channel != null) && (mtkChannel != null)) {
                        if (isDigitalFromAnalog) {
                            var channelIndex = (channel.platformSpecific as PlatformSpecificData).internalServiceIndex
                            var channelListIndex = (channel.platformSpecific as PlatformSpecificData).internalServiceListID

                            if((channelIndex != mtkChannel.svlRecId) || (channelListIndex != mtkChannel.svlId)) {
                                for ((index, service) in channels.withIndex()) {
                                    var serviceIndex = ((service.platformSpecific) as PlatformSpecificData).internalServiceIndex
                                    var listIndex = ((service.platformSpecific) as PlatformSpecificData).internalServiceListID

                                    if ((serviceIndex == mtkChannel.svlRecId) && (listIndex == mtkChannel.svlId)) {
                                        Log.d(Constants.LogTag.CLTV_TAG + TAG,"Updating active service to digital service at $index")
                                        utilsInterface.setPrefsValue("CurrentActiveChannel", index)
                                        utilsInterface.setPrefsValue("CurrentActiveChannelId",service.id)
                                        updateDesiredChannelIndex()
                                        callback.onReceive(service)
                                        return
                                    }
                                }
                            }
                        }
                    }
                    callback.onReceive(channel)
                }
            }
        }
    }

    override fun storeActiveChannel(tvChannel: TvChannel, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.storeActiveChannel(tvChannel, applicationMode)
        } else {
            if(tvChannel.isFastChannel()){
                utilsInterface.setPrefsValue(activeChannelTag, getChannelList(applicationMode).indexOf(tvChannel))
                utilsInterface.setPrefsValue(activeChannelTagId, tvChannel.id)
            }
            try {
                val activeMtkChannel = MtkTvChannelList.getInstance().getChannelInfoBySvlRecId(((tvChannel.platformSpecific) as PlatformSpecificData).internalServiceListID, ((tvChannel.platformSpecific) as PlatformSpecificData).internalServiceIndex)
                MtkTvChannelListBase.setCurrentChannel(activeMtkChannel)
            }catch (e: Exception){
                println(e.printStackTrace())
            }
        }
    }

    override fun isTvNavigationBlocked(applicationMode: ApplicationMode): Boolean {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.isTvNavigationBlocked(applicationMode)
        } else {
            val status = MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_NAV_BLOCKED_STATUS)
            val mask: Int = status and 0xff
            return mask == 3
        }
    }

    override fun playPlayableItem(item : PlayableItem) {
        if(!(item as TvChannel).isFastChannel()){
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "playPlayableItem: show channel change toast for t56")
            InformationBus.informationBusEventListener.submitEvent(Events.SHOW_CHANNEL_CHANGE_TOAST_FOR_T56,
                arrayListOf(item.channelId))
        } else {
            isAnalogServiceStarted = false
            isDigitalFromAnalog = false
            super.playPlayableItem(item)
        }
    }

    override fun getChannelSourceType(tvChannel: TvChannel, applicationMode: ApplicationMode): String {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelSourceType(tvChannel, applicationMode)
        } else {
            return when (tvChannel.tunerType) {
                TunerType.TERRESTRIAL_TUNER_TYPE -> utilsInterface.getStringValue("antenna_type").substring(0,2)
                TunerType.CABLE_TUNER_TYPE -> utilsInterface.getStringValue("cable").substring(0,2)
                TunerType.SATELLITE_TUNER_TYPE -> utilsInterface.getStringValue("satellite").substring(0,2)
                TunerType.ANALOG_TUNER_TYPE -> getAnalogTunerTypeName(tvChannel).substring(0,2)
                TunerType.DEFAULT -> getTifChannelSourceLabel(tvChannel).substring(0,2)
                else -> ""
            }
        }
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
            if (playableItem.isFastChannel()) {
                callback.onSuccess()
            } else {
                callback.onFailed(Error("t56"))
            }

        }
    }

    override fun getChannelList(applicationMode: ApplicationMode): ArrayList<TvChannel> {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelList(applicationMode)
        } else {
            var channelList = arrayListOf<TvChannel>()
            dataProvider.getChannelList().forEach {
                if (!it.isFastChannel()) {
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
        } else
            return (tvChannel.platformSpecific as PlatformSpecificData).internalServiceListID
    }

    override fun getLockedChannelListAfterOta(applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getLockedChannelListAfterOta(applicationMode)
        } else {
            val lockedChannelsSet = context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).getBoolean(KEY_PREFERRED_LOCKED_CHANNELS_SET, false)
            if(!lockedChannelsSet) {
                val mtkTvChList: MtkTvChannelList = MtkTvChannelList.getInstance()
                val svlID: Int =
                    MtkTvConfig.getInstance().getConfigValue(MtkTvConfigTypeBase.CFG_BS_SVL_ID)
                val filter: Int = MtkTvChCommonBase.SB_VNET_ALL
                val fullServiceList: List<MtkTvChannelInfoBase> =
                    mtkTvChList.getChannelListByFilter(svlID, filter, 0, 0, 0xFFFF)
                val tvChannels: ArrayList<TvChannel> = getChannelList()
                val mtkModificationList: ArrayList<MtkTvChannelInfoBase> = ArrayList()
                val tifModificationList: ArrayList<TvChannel> = ArrayList()
                var foundBlockedService = false
                fullServiceList.forEach { mtkTvChannelInfoBase ->
                    kotlin.run data@{
                        tvChannels.forEach { channel ->
                            if ((channel.platformSpecific as PlatformSpecificData).internalServiceIndex == mtkTvChannelInfoBase.svlRecId &&
                                (channel.platformSpecific as PlatformSpecificData).internalServiceListID == mtkTvChannelInfoBase.svlId) {
                                channel.isLocked = mtkTvChannelInfoBase.isBlock
                                if(mtkTvChannelInfoBase.isBlock) {
                                    tifModificationList.add(channel)
                                    mtkTvChannelInfoBase.isBlock = false
                                    mtkModificationList.add(mtkTvChannelInfoBase)
                                    foundBlockedService = true
                                }
                                return@data
                            }
                        }
                    }
                }
                if(foundBlockedService) {
                    mtkTvChList.setChannelList(MtkTvChannelList.CHLST_OPERATOR_MOD, mtkModificationList);
                    thread {
                        //because this is MTK SDK and it will finish updating TIF DB at random time
                        //so we need to wait for it for a little bit to unlock services
                        var retryCount = 0
                        while(true) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG,"getLockedChannelListAfterOta waiting")
                            Thread.sleep(2000)
                            var lockedFound = false
                            for(channel in getChannelList())
                            {
                                if(channel.isLocked) {
                                    Log.d(Constants.LogTag.CLTV_TAG + TAG,"Locked service ${channel.name} found continuing")
                                    lockedFound = true
                                }
                            }
                            retryCount++
                            Log.d(Constants.LogTag.CLTV_TAG + TAG,"getLockedChannelListAfterOta lockedFound $lockedFound $retryCount")
                            //Did SDK unlocked everything in TIF ?
                            if(lockedFound && (retryCount <= 10)) {
                                continue
                            }
                            Log.d(Constants.LogTag.CLTV_TAG + TAG,"getLockedChannelListAfterOta locking")
                            //now lock everything that needs to be locked
                            for (channel in tifModificationList) {
                                dataProvider.lockUnlockChannel(channel, channel.isLocked)
                            }
                            break
                        }
                    }
                }

                context.getSharedPreferences(PREFS_TAG, Context.MODE_PRIVATE).edit().putBoolean(KEY_PREFERRED_LOCKED_CHANNELS_SET, true).apply()
            }
        }
    }

    override fun getVisuallyImpairedAudioTracks(applicationMode: ApplicationMode): List<String> {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getVisuallyImpairedAudioTracks(applicationMode)
        } else {
            val soundtrackItem = ArrayList<String>()
            TVAsyncExecutor.getInstance().execute {
                MtkTvConfig.getInstance().setConfigValue(
                    MtkTvConfigTypeBase.CFG_MENU_AUDIOINFO_SET_INIT,
                    0
                )
            }
            val soundListsize: Int =
                MtkTvConfig.getInstance()
                    .getConfigValue(MtkTvConfigTypeBase.CFG_MENU_AUDIOINFO_GET_TOTAL)
            val viIndex: Int = MtkTvConfig.getInstance()
                .getConfigValue(MtkTvConfigTypeBase.CFG_MENU_AUDIOINFO_GET_CURRENT)
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "sound list size===$soundListsize")
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "get details of audio indedx====$viIndex")

            for (i in 0 until soundListsize) {
                val itemName = "audioinfogetstring_$i"
                val soundString: String = MtkTvConfig.getInstance().getConfigString(itemName)
                val itemValueStrings = arrayOfNulls<String>(3)
                Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "get details of sound string index====$soundString")
                if (soundString != null) {
                    itemValueStrings[0] = soundString
                    itemValueStrings[1] = ""
                    itemValueStrings[2] = ""
                } else {
                    itemValueStrings[0] = ""
                    itemValueStrings[1] = ""
                    itemValueStrings[2] = ""
                }
                soundtrackItem.add(soundString)

            }
            return soundtrackItem
        }
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
                                    if (playableItem is TvChannel) {
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
            if(channel.isFastChannel()){
                //lastActiveChannel is a list containing only two elements: current active channel and last active channel
                //it behaves like fifo stack- when active channel is added, first element at index 0 is removed
                lastActiveChannel.add(channel)

                //remove first item if size is more than 2
                if (lastActiveChannel.size > 2) {
                    lastActiveChannel.removeAt(0)
                }
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
                if (tvChannel!!.isFastChannel()) {
                    changeChannel(tvChannel!!, callback)
                }
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
                    if (channelToPlay!!.isFastChannel()) {
                        changeChannel(channelToPlay, callback)
                    }
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
                    var tempList = getChannelList(applicationMode)
                    var tempIndex = if (activeChannel >= tempList.size) 0 else activeChannel
                    val playableItem = channels[tempIndex]
                    if (playableItem.isFastChannel()) {
                        changeChannel(activeChannel, callback)
                    }
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
                        if (!rrt5Rating?.rating.isNullOrEmpty())
                            baseParentalRating = rrt5Rating?.rating.toString()
                    }
                }
            }, applicationMode)
        }
        if (baseParentalRating.isEmpty())
            baseParentalRating =
                super.getParentalRatingDisplayName(parentalRating, applicationMode, tvEvent)
        return baseParentalRating
    }
}