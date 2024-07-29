package com.iwedia.cltv.platform.refplus5

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvTrackInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.iwedia.cltv.platform.*
import com.iwedia.cltv.platform.base.TvInterfaceBaseImpl
import com.iwedia.cltv.platform.base.content_provider.getInputIds
import com.iwedia.cltv.platform.`interface`.*
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.parental.Region
import com.iwedia.cltv.platform.model.player.PlayableItem
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.refplus5.audio.TvProviderAudioTrackBase
import com.iwedia.cltv.platform.refplus5.provider.ChannelDataProvider
import com.iwedia.cltv.platform.refplus5.provider.PlatformSpecificData
import com.mediatek.dtv.tvinput.client.channeltuned.ChannelTunedManager
import com.mediatek.dtv.tvinput.client.servicedatabase.ServiceListEditManager
import com.mediatek.dtv.tvinput.framework.tifextapi.common.tunerinfo.Constants
import kotlinx.coroutines.Dispatchers


internal class TvInterfaceImpl (
    playerInterface: PlayerInterface,
    networkInterface: NetworkInterface,
    dataProvider: ChannelDataProviderInterface,
    tvInputInterface: TvInputInterface,
    utilsInterface: UtilsInterface,
    epgModule: EpgInterface,
    context: Context,
    timeInterface: TimeInterface,
    parentalControlSettingsInterface: ParentalControlSettingsInterface
) : TvInterfaceBaseImpl(playerInterface, networkInterface, dataProvider,  tvInputInterface, utilsInterface,epgModule, context, timeInterface, parentalControlSettingsInterface) {

    private val TAG = javaClass.simpleName
    companion object {
        public val activeChannelTag = "CurrentActiveChannel"
    }
    private val activeChannelTagId = "CurrentActiveChannelId"
    private var desiredChannelIndex = 0
    override var activeCategoryId = FilterItemType.ALL_ID
    private var activeFavGroupName = ""
    private var activeTifCategoryName = ""
    private var activeGenreCategoryName = ""
    private val prefsRecentChannels = "RecentChannels"
    private val SKIPPED_CHANNELS_TAG = "skipped_channels"

    private val GLOBAL_VALUE_KEY = "key"
    private val GLOBAL_VALUE_VALUE = "value"
    private val GLOBAL_VALUE_STORED = "stored"
    private val AUTHORITY = "com.mediatek.tv.internal.data"
    private val GLOBAL_PROVIDER_ID = "global_value"
    private val GLOBAL_PROVIDER_URI_URI = Uri.parse("content://$AUTHORITY/$GLOBAL_PROVIDER_ID")
    private val CFG_GRP_BS_PREFIX = "g_bs__"
    private val CFG_BS_BS_SRC: String = CFG_GRP_BS_PREFIX + "bs_src"
    private val CFG_BS_BS_USER_SRC = CFG_GRP_BS_PREFIX + "bs_user_src"
    private val BS_SRC_AIR = 0
    private val BS_SRC_CABLE = 1
    private val BS_SRC_SAT = 2
    private var directTuneInProgress = false
    private var directTuneListener : Any? = null
    private val BROADCAST_MEDIUM_TERRESTRIAL = 1
    private val BROADCAST_MEDIUM_CABLE = 2
    private var currentActiveChannel: TvChannel? = null
    private var lastActiveChannel: TvChannel? = null
    private var isBarkerRunning = false
    private val preferredSatellite = "PREFERRED_SATELLITE"
    private val generalSatellite = "GENERAL_SATELLITE"

    private var mChannelTunedManager = ChannelTunedManager(context, CiPlusInterfaceImpl.inputid)
    private var reSortBundle = Bundle()
    private var mTempServiceListId = ""
    val DVBC_INPUT_ID0: String = "com.mediatek.dtv.tvinput.dvbtuner" + "/.DvbTvInputService/HW0"
    val SERVICE_EDIT_OPT_UPDATE_NUMBER = 2

    private val mChannelTunedListener =
        object : ChannelTunedManager.ChannelTunedListener {
            override fun onChannelTuned(sessionToken: String?, params: Bundle?) {
                params?.let {
                    val status = it.getInt(Constants.KEY_TUNED_STATUS, -1)
                    when(status) {
                        Constants.TUNED_STATUS_BEGIN -> {
                            val mId = it.getLong(Constants.KEY_TUNED_CHANNEL_ID, -1L)
                            val mFlag = it.getInt(Constants.KEY_TUNED_QUIETLY_FLAG, -1)
                            when (mFlag) {
                                Constants.QUIETLY_TUNE -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_BEGIN for QUIETLY_TUNE, not handled")
                                }

                                Constants.NORMAL_WITH_NO_UI_DISPLAY_TUNE -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_BEGIN for NORMAL_WITH_NO_UI_DISPLAY_TUNE, id $mId, handled")
                                    for(channel in getChannelList()) {
                                        if(mId != -1L) {
                                            if (channel.id == mId.toInt()) {
                                                storeActiveChannel(channel)
                                                break
                                            }
                                        }
                                    }
                                }
                                Constants.SERVICE_REPLACEMENT_TUNE -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_BEGIN for SERVICE_REPLACEMENT_TUNE, not handled")
                                }

                                Constants.NORMAL_TUNE -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_BEGIN for NORMAL_TUNE, not handled")
                                }
                                else -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_BEGIN for unknown status $mFlag, not handled")
                                }
                            }
                        }
                        Constants.TUNED_STATUS_SUCCESS -> {
                            val mId = it.getLong(Constants.KEY_TUNED_CHANNEL_ID, -1L)
                            val mFlag = it.getInt(Constants.KEY_TUNED_QUIETLY_FLAG, -1)
                            when (mFlag) {
                                Constants.QUIETLY_TUNE -> {
                                    // reset silence tune
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_SUCCESS for QUIETLY_TUNE, not handled")
                                }
                                Constants.NORMAL_WITH_NO_UI_DISPLAY_TUNE -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_SUCCESS for NORMAL_WITH_NO_UI_DISPLAY_TUNE, not handled")
                                }
                                Constants.NORMAL_TUNE -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_SUCCESS for NORMAL_TUNE id:$mId, handled")
                                    for(channel in getChannelList()) {
                                        if(mId != -1L) {
                                            if (channel.id == mId.toInt()) {
                                                storeActiveChannel(channel)
                                                InformationBus.informationBusEventListener.submitEvent(
                                                    Events.CHANNEL_CHANGED,
                                                    arrayListOf(channel)
                                                )
                                                break
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received TUNED_STATUS_SUCCESS for unknown status $mFlag, not handled")
                                }
                            } // when
                        }
                        else -> {
                            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Received unknown status $status, not handled")
                        }
                    }
                }
            }
        }

    init {
        mChannelTunedManager.addChannelTunedListener(mChannelTunedListener)
    }

    override fun getChannelSourceType(tvChannel: TvChannel, applicationMode: ApplicationMode): String {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getChannelSourceType(tvChannel, applicationMode)
        } else {
            return when (tvChannel.tunerType) {
                TunerType.TERRESTRIAL_TUNER_TYPE -> utilsInterface.getStringValue("antenna_type")
                TunerType.CABLE_TUNER_TYPE -> utilsInterface.getStringValue("cable")
                TunerType.SATELLITE_TUNER_TYPE -> getTunerTypeForSatellite(tvChannel)
                TunerType.ANALOG_TUNER_TYPE -> getAnalogTunerTypeName(tvChannel)
                TunerType.DEFAULT -> getTifChannelSourceLabel(tvChannel)
                else -> ""
            }
        }
    }

    private fun getTunerTypeForSatellite(tvChannel: TvChannel): String {
        if (tvChannel.internalProviderId != null) {
            if (tvChannel.internalProviderId?.contains("PREFERRED_SATELLITE")!!) {
                return utilsInterface.getStringValue("preferred_satellite")
            }
        }
        return utilsInterface.getStringValue("satellite")
    }

    override fun getAnalogTunerTypeName(tvChannel: TvChannel, applicationMode: ApplicationMode) : String {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getAnalogTunerTypeName(tvChannel, applicationMode)
        } else {
            return when (getAnalogServiceListID(tvChannel)) {
                TunerType.TYPE_ANALOG_ANTENNA -> utilsInterface.getStringValue("analog_antenna")
                TunerType.TYPE_ANALOG_CABLE -> utilsInterface.getStringValue("analog_cable")
                TunerType.TYPE_ANALOG -> utilsInterface.getStringValue("analog")
                else -> ""
            }
        }
    }


    override fun getAnalogServiceListID(tvChannel: TvChannel, applicationMode: ApplicationMode): Int {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getAnalogServiceListID(tvChannel, applicationMode)
        }
        else {
            if (utilsInterface.getRegion() == Region.PA) {
                return TunerType.TYPE_ANALOG
            }
            else if(utilsInterface.getRegion() == Region.EU){
                if ((tvChannel.platformSpecific as PlatformSpecificData).channelListId == "MEDIATEK_ATV_CHANNEL_LIST_CABLE") {
                    return TunerType.TYPE_ANALOG_CABLE
                }
                else if ((tvChannel.platformSpecific as PlatformSpecificData).channelListId == "MEDIATEK_ATV_CHANNEL_LIST_TERRESTRIAL") {
                    return TunerType.TYPE_ANALOG
                }
            }else {
                if ((tvChannel.platformSpecific as PlatformSpecificData).channelListId == "MEDIATEK_ATV_CHANNEL_LIST_CABLE") {
                    return TunerType.TYPE_ANALOG_CABLE
                }
                else if ((tvChannel.platformSpecific as PlatformSpecificData).channelListId == "MEDIATEK_ATV_CHANNEL_LIST_TERRESTRIAL") {
                    return TunerType.TYPE_ANALOG_ANTENNA
                }
            }
            return TunerType.TYPE_ANALOG
        }
    }

    override fun getVisuallyImpairedAudioTracks(applicationMode: ApplicationMode): List<String> {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.getVisuallyImpairedAudioTracks(applicationMode)
        } else {
            val tracks = playerInterface.getPlaybackTracks(TvTrackInfo.TYPE_AUDIO)
            val viTracks = mutableListOf<String>()
            for (track in tracks) {
                val eType = track.extra?.getInt("KEY_TRACK_INT_AUDIO_TYPE") ?: 0
                val mixType = track.extra?.getInt("KEY_TRACK_INT_AUDIO_MIX_TYPE") ?: 0
                val eClass = track.extra?.getInt("KEY_TRACK_INT_AUDIO_CLASS") ?: 0
                val visualImpairedAd = (eType == TvProviderAudioTrackBase.AUD_TYPE_VISUAL_IMPAIRED
                        && eClass != TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE)
                        || eClass == TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_AD
                val visualImpairedSps =
                    eClass == TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE
                val visualImpairedAdSps =
                    eClass == TvProviderAudioTrackBase.AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE
                val isViTrack = visualImpairedAd || visualImpairedSps || visualImpairedAdSps
                if (isViTrack) {
                    viTracks.add(track.language.toString())
                }
            }
            return viTracks
        }

    }

    @Synchronized
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

    override fun getActiveChannel(callback: IAsyncDataCallback<TvChannel>, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getActiveChannel(callback, applicationMode)
        } else {
            val activeChannel = utilsInterface.getPrefsValue(activeChannelTag, 0) as Int
            val activeChannelId = utilsInterface.getPrefsValue(activeChannelTagId, 0) as Int
            var channels = getChannelList()

            //When SDK removes some services from DB, activeChannel could be out of range of DB
            //fallback to data from MTK SDK
            if (activeChannel >= channels.size || activeChannel < 0) {
                if(channels.size == 0) {
                    callback.onFailed(Error("Active channel not found."))
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG, "Active channel not found, list size iz 0")
                } else {
                    callback.onReceive(channels[0])
                    utilsInterface.setPrefsValue(activeChannelTag, 0)
                    utilsInterface.setPrefsValue(activeChannelTagId, channels[0].id)
                }
            } else {
                var channel = channels[activeChannel]

                //Because MTK SDK can inject Analog services in front of rest of services our id and
                //index can get out of sync so lets resync them
                if(channel.id != activeChannelId) {
                    Log.e(TAG,"Active channel ${channel.name} has wrong Id expected $activeChannelId got ${channel.id} ")
                    channels.forEachIndexed { index, availableChannel ->
                        if(activeChannelId == availableChannel.id) {
                            Log.d(TAG,"Found Active channel id on position $index, resynchronizing")
                            utilsInterface.setPrefsValue(activeChannelTag, index)
                            channel = availableChannel
                            return@forEachIndexed
                        }
                    }
                }
                //if still didn't synchronized just overwrite id
                if(channel.id != activeChannelId) {
                    Log.d(TAG,"Did not found service with proper id, overwriting active id with ${channel.id}")
                    utilsInterface.setPrefsValue(activeChannelTagId, channel.id)
                }

                callback.onReceive(channel)
            }
        }
    }
    override fun storeActiveChannel(tvChannel: TvChannel, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.storeActiveChannel(tvChannel, applicationMode)
        } else {
            utilsInterface.setPrefsValue(activeChannelTag, getChannelList(applicationMode).indexOf(tvChannel))
            utilsInterface.setPrefsValue(activeChannelTagId, tvChannel.id)
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

    override fun getLastActiveChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getLastActiveChannel(callback, applicationMode)
        } else {
            if (lastActiveChannel == null){
                return
            }
            val channelToUse = lastActiveChannel ?: currentActiveChannel
            channelToUse?.let { changeChannel(it, callback, applicationMode) }
        }
    }

    private fun getDirectTuneBundle(item : PlayableItem) : Bundle {
        val bundle = Bundle()
        when (item) {
            is TvChannel -> {
                bundle.putInt("KEY_FREQUENCY", (item.platformSpecific as PlatformSpecificData).frequency)
                if ((item.platformSpecific as PlatformSpecificData).channelListId == "MEDIATEK_ATV_CHANNEL_LIST_CABLE") {
                    bundle.putInt("KEY_BROADCAST_MEDIUM", BROADCAST_MEDIUM_CABLE);
                } else {
                    bundle.putInt("KEY_BROADCAST_MEDIUM", BROADCAST_MEDIUM_TERRESTRIAL);
                }
                bundle.putString("KEY_MODULATION", (item.platformSpecific as PlatformSpecificData).modulation);
                bundle.putBoolean("com.mediatek.dtv.tvinput.dvbtuner.MMI_OVERLAY_SUPPORT", false);
            }
        }
        return bundle
    }

    private fun saveMtkInternalGlobalValue(context: Context, id: String?, value: String?, isStored: Boolean): Boolean {
        val values = ContentValues()
        values.put(GLOBAL_VALUE_KEY, id)
        values.put(GLOBAL_VALUE_VALUE, value)
        values.put(GLOBAL_VALUE_STORED, java.lang.Boolean.valueOf(isStored))
        try {
            context.contentResolver.insert(GLOBAL_PROVIDER_URI_URI, values)
        } catch (ex: Exception) {
            return false
        }
        return true
    }

    inner class ChannelLoadedEventListener(var callback: IAsyncCallback?, val destinationFrequency : Int, val destinationType : String){
        private var eventListener: Any?= null
        init {
            InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.CHANNELS_LOADED), callback = {
                eventListener = it
                directTuneListener = eventListener
            }, onEventReceived = {
                var serviceFound = false
                for(destinationChannel in getChannelList(ApplicationMode.DEFAULT)) {
                    if((((destinationChannel.platformSpecific) as PlatformSpecificData).frequency == destinationFrequency)
                        && (destinationType == destinationChannel.type) &&
                        destinationChannel.isBrowsable) {
                        serviceFound = true
                        callback?.onSuccess()
                        directTuneListener = null
                        InformationBus.informationBusEventListener.unregisterEventListener(eventListener!!)
                        storeActiveChannel(destinationChannel, ApplicationMode.DEFAULT)
                        (playerInterface as PlayerInterfaceImpl).play(destinationChannel, Bundle())
                        break
                    }
                }
                if(!serviceFound) {
                    InformationBus.informationBusEventListener.unregisterEventListener(eventListener!!)
                    callback?.onFailed(Error())
                }
            })
        }
    }


    private fun digitalDirectTune(lastChannel : TvChannel, item : TvChannel, isRetry : Boolean) : TvChannel {
        var playItem = item
        var type = TvContract.Channels.TYPE_ATSC_T
        var mtkType = BS_SRC_AIR
        if ((item.platformSpecific as PlatformSpecificData).channelListId == "MEDIATEK_ATV_CHANNEL_LIST_CABLE") {
            type = TvContract.Channels.TYPE_ATSC_C
            mtkType = BS_SRC_CABLE
        }

        for(channel in getChannelList(ApplicationMode.DEFAULT)) {
            var channelDirectTuneNumber = (channel.platformSpecific as PlatformSpecificData).directTuneNumber
            if((channel.type == type) && (item.displayNumber.toInt() == channelDirectTuneNumber)) {
                playItem = channel
                CoroutineHelper.runCoroutine({
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Starting direct tune for : $playItem ${getDirectTuneBundle(playItem)} $mtkType")
                    saveMtkInternalGlobalValue(context, CFG_BS_BS_SRC, mtkType.toString(),true);
                    saveMtkInternalGlobalValue(context, CFG_BS_BS_USER_SRC , mtkType.toString(),true);
                    directTuneInProgress = true
                    (playerInterface as PlayerInterfaceImpl).play(playItem,getDirectTuneBundle(playItem))
                    if(directTuneListener != null) {
                        InformationBus.informationBusEventListener.unregisterEventListener(directTuneListener!!)
                        directTuneListener = null
                    }
                    ChannelLoadedEventListener(object : IAsyncCallback {
                        override fun onFailed(error: Error) {
                            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"Digital direct tune failed for ${playItem.type} Frequency : ${((playItem.platformSpecific) as PlatformSpecificData).frequency}")
                            if(!isRetry) {
                                /*thread {
                                    Thread.sleep(2000)
                                    analogDirectTune(lastChannel, item, true)
                                }*/
                            }
                        }

                        override fun onSuccess() {
                            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Digital direct tune good for ${playItem.type} Frequency : ${((playItem.platformSpecific) as PlatformSpecificData).frequency}")
                            directTuneInProgress = false

                        }
                    },((playItem.platformSpecific) as PlatformSpecificData).frequency, playItem.type!!)

                    addRecentlyWatched(playItem)
                }, Dispatchers.Main)
            }
        }
        return playItem
    }

    private fun analogDirectTune(lastChannel : TvChannel, item : TvChannel, isRetry : Boolean) : TvChannel {
        var playItem = item
        var mtkType = BS_SRC_AIR
        if ((item.platformSpecific as PlatformSpecificData).channelListId == "MEDIATEK_ATV_CHANNEL_LIST_CABLE") {
            mtkType = BS_SRC_CABLE
        }
        CoroutineHelper.runCoroutine({
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Starting direct tune for : $playItem ${getDirectTuneBundle(playItem)} $mtkType")
            saveMtkInternalGlobalValue(context, CFG_BS_BS_SRC, mtkType.toString(),true);
            saveMtkInternalGlobalValue(context, CFG_BS_BS_USER_SRC , mtkType.toString(),true);
            directTuneInProgress = true
            (playerInterface as PlayerInterfaceImpl).play(playItem,getDirectTuneBundle(playItem))
            if(directTuneListener != null) {
                InformationBus.informationBusEventListener.unregisterEventListener(directTuneListener!!)
                directTuneListener = null
            }
            ChannelLoadedEventListener(object : IAsyncCallback {
                override fun onFailed(error: Error) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"Analog direct tune failed for ${(playItem as TvChannel).type} Frequency : ${(((playItem as TvChannel).platformSpecific) as PlatformSpecificData).frequency}")
                    if(!isRetry) {
                        /*thread {
                            Thread.sleep(2000)
                            digitalDirectTune(lastChannel, item, true)
                        }*/
                    }
                }

                override fun onSuccess() {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"Analog direct tune good for ${(playItem as TvChannel).type} Frequency : ${(((playItem as TvChannel).platformSpecific) as PlatformSpecificData).frequency}")
                    directTuneInProgress = false

                }
            },(((playItem as TvChannel).platformSpecific) as PlatformSpecificData).frequency, playItem.type!!)

            addRecentlyWatched(playItem)
        }, Dispatchers.Main)
        return playItem
    }

    override fun playPlayableItem(item : PlayableItem) {
        var playItem = item
        when (item) {
            is TvChannel -> {
                if((item.tunerType == TunerType.ANALOG_TUNER_TYPE) && (!item.isBrowsable)) {
                    if(lastActiveChannel != null) {
                        if (lastActiveChannel?.tunerType == TunerType.ANALOG_TUNER_TYPE) {
                            playItem = analogDirectTune(lastActiveChannel!!, item, false)
                        } else {
                            playItem = digitalDirectTune(lastActiveChannel!!, item, false)
                        }
                    }
                    return
                }
            }
        }

        CoroutineHelper.runCoroutine({
            if(directTuneInProgress) {
                directTuneInProgress = false
                if(directTuneListener != null) {
                    InformationBus.informationBusEventListener.unregisterEventListener(directTuneListener!!)
                    directTuneListener = null
                }
                (playerInterface as PlayerInterfaceImpl).play(playItem, Bundle())
            } else {
                if(isBarkerRunning) {
                    (epgModule as EpgInterfaceImpl).cancelBarkerChannel()
                    isBarkerRunning = false
                }
                playerInterface.play(playItem)
            }
            addRecentlyWatched(playItem)
        }, Dispatchers.Main)
    }

    override fun changeChannel(index: Int, callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.changeChannel(index, callback, applicationMode)
        } else {
            var channels = getChannelList(applicationMode)
            var index = if (index >= channels.size) 0 else index
            val playableItem = channels[index]
            desiredChannelIndex = index
            storeActiveChannel(playableItem)
            storeLastActiveChannel(playableItem)
            playPlayableItem(playableItem)
            callback.onSuccess()
        }
    }

    override fun storeLastActiveChannel(channel: TvChannel, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.storeLastActiveChannel(channel, applicationMode)
        } else {
            lastActiveChannel = currentActiveChannel
            currentActiveChannel = channel
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
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "getChannelById: ${E.message}")
            }
            return null
        }
    }

    override fun getSelectedChannelList(
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode,
        filter: FilterItemType?,
        filterMetadata: String?
    ) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.getSelectedChannelList(callback, applicationMode, filter, filterMetadata)
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
                    || (activeCategoryId == FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.ANALOG_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.PREFERRED_SATELLITE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.GENERAL_SATELLITE_TUNER_TYPE_ID)
                ) {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding channel for Category: $activeCategoryId")
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
                        FilterItemType.ANALOG_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG) {
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

                        FilterItemType.PREFERRED_SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.internalProviderId != null && channel.internalProviderId?.contains(preferredSatellite)!!) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }

                        FilterItemType.GENERAL_SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.internalProviderId != null && channel.internalProviderId?.contains(generalSatellite)!!) {
                                    selectedChannelList.add(channel)
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
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding channel for TIF Category: $activeCategoryId, activeTifCategoryName: $activeTifCategoryName")

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
                        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Channel switch for $activeCategoryId")
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
                        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Channel switch for $activeCategoryId")
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
                        getActiveChannel(object :
                            IAsyncDataCallback<TvChannel> {
                            override fun onFailed(error: Error) {
                                callback.onReceive(channelList)
                            }

                            override fun onReceive(data: TvChannel) {
                                //Because Ref 5.0 SDK can't direct tune to non-current tuner
                                //so no reason to show direct tune services in dial list
                                val selectedChannelList: ArrayList<TvChannel> = ArrayList()
                                channelList.forEach { channel->
                                    var channelListId = getAnalogServiceListID(channel,ApplicationMode.DEFAULT)
                                    var isAtsc = (data.type == TvContract.Channels.TYPE_NTSC) || (data.type == TvContract.Channels.TYPE_ATSC_C) || (data.type == TvContract.Channels.TYPE_ATSC_T)

                                    if(data.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE) {
                                        if (channel.isBrowsable || (channelListId != TunerType.TYPE_ANALOG_CABLE  && (channel.displayNumber != "0" || !isAtsc)))
                                        {
                                            selectedChannelList.add(channel)
                                        }
                                    }
                                    else if(data.tunerType == TunerType.CABLE_TUNER_TYPE) {
                                        if (channel.isBrowsable || (channelListId != TunerType.TYPE_ANALOG_ANTENNA && (channel.displayNumber != "0" || !isAtsc)) )
                                        {
                                            selectedChannelList.add(channel)
                                        }
                                    } else {
                                        selectedChannelList.add(channel)
                                    }
                                }
                                callback.onReceive(selectedChannelList)
                            }
                        })
                }
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun isChannelSelectable(channel : TvChannel) : Boolean {
        if(channel.isFastChannel()) {
            return true
        }

        if(utilsInterface.getCountryPreferences(UtilsInterface.CountryPreference.USE_HIDDEN_SERVICE_FLAG,false) == true) {
            var provider: ChannelDataProvider = dataProvider as ChannelDataProvider
            var providerFlag4 = channel.providerFlag4 ?: 0

            if (((providerFlag4 and 0x1) > 0) || provider.isAnalogService(channel)) {
                return true
            }
            return false
        }
        return true
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
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for FavoriteGroup $activeFavGroupName")
                    //Get the favorite channel list.
                    var channelList = getChannelList()
                    val favoriteChannelList: ArrayList<TvChannel> = ArrayList()
                    val favoriteChannelListBrowsable: ArrayList<TvChannel> = ArrayList()
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()
                    //Get all channels for selected favorite group
                    channelList.forEach { tvChannel ->
                        if (tvChannel.favListIds.contains(activeFavGroupName)) {
                            favoriteChannelList.add(tvChannel)
                            if(tvChannel.isBrowsable) favoriteChannelListBrowsable.add(tvChannel)
                        }
                    }

                    if (favoriteChannelListBrowsable.isEmpty()) {
                        //list is empty, reset the next order to all list.
                        activeCategoryId = FilterItemType.ALL_ID
                        nextChannel(callback, applicationMode)
                        return@runCoroutine
                    } else if (favoriteChannelListBrowsable.size == 1 && currentActiveChannel?.isBrowsable == true) {
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
                }
                else if ((activeCategoryId == FilterItemType.RADIO_CHANNELS_ID) || (activeCategoryId == FilterItemType.TERRESTRIAL_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.SATELLITE_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.RECENTLY_WATCHED_ID) || (activeCategoryId == FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.ANALOG_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.PREFERRED_SATELLITE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.GENERAL_SATELLITE_TUNER_TYPE_ID)
                ) {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for Category: $activeCategoryId")
                    val selectedChannelList: ArrayList<TvChannel> = ArrayList()
                    val selectedChannelListBrowsable: ArrayList<TvChannel> = ArrayList()

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
                        FilterItemType.ANALOG_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG) {
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

                        FilterItemType.PREFERRED_SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.internalProviderId != null && channel.internalProviderId?.contains(preferredSatellite)!!) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }

                        FilterItemType.GENERAL_SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.internalProviderId != null && channel.internalProviderId?.contains(generalSatellite)!!) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }

                        else -> {

                        }
                    }

                    selectedChannelList.forEach {
                        if(it.isBrowsable) selectedChannelListBrowsable.add(it)
                    }

                    //If list is empty, Reset the activeCategoryId to ALL
                    if (selectedChannelListBrowsable.size == 0) {
                        activeCategoryId = FilterItemType.ALL_ID
                        nextChannel(callback, applicationMode)
                        return@runCoroutine
                    } else if (selectedChannelListBrowsable.size == 1 && currentActiveChannel?.isBrowsable == true) {
                        //only one channel, channel switch not required
                        callback.onFailed(
                            Error("Single Entry in List")
                        )
                        return@runCoroutine
                    }
                    playNextIndex(selectedChannelList, callback)
                }
                else if (activeCategoryId >= FilterItemType.TIF_INPUT_CATEGORY) {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding next channel for TIF Category: $activeCategoryId, activeTifCategoryName: $activeTifCategoryName")

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
                        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
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
                }
                else if (activeCategoryId == FilterItemType.GENRE_ID) {
                    var selectedChannelList = java.util.ArrayList<TvChannel>()
                    getChannelList().forEach { channel ->
                        if (channel.genres.contains(activeGenreCategoryName)) {
                            selectedChannelList.add(channel)
                        }
                    }
                    if (selectedChannelList.size > 0) {
                        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Channel next switch for $activeCategoryId")
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
                }
                else {
                    try {
                        val channelList = getChannelList()
                        val channelListBrowsable : ArrayList<TvChannel> = ArrayList()
                        var isAllSkippedChannels = true
                        channelList.forEach{tvChannel ->
                            if(tvChannel.isBrowsable && !tvChannel.isSkipped){
                                isAllSkippedChannels = false
                            }
                            if(tvChannel.isBrowsable) channelListBrowsable.add(tvChannel)
                        }
                        newDesiredChannelIndex = if (desiredChannelIndex >= channelList.size - 1) {
                            0
                        } else {
                            (desiredChannelIndex + 1)
                        }
                        val includeSkippedChannels = isAllSkippedChannels && currentActiveChannel?.isBrowsable == false
                        val clSize = channelList.size
                        var iter = 0
                        var tvChannel = channelList[newDesiredChannelIndex]
                        while ((tvChannel.isSkipped && !includeSkippedChannels) || (!tvChannel.isBrowsable && !utilsInterface.isThirdPartyChannel(tvChannel))){
                            newDesiredChannelIndex += 1
                            iter ++

                            if (newDesiredChannelIndex >= clSize) {
                                newDesiredChannelIndex = 0
                            }
                            tvChannel = channelList[newDesiredChannelIndex]
                            if (iter >= clSize) {
                                if(isAllSkippedChannels){
                                    storeActiveChannel(channelList[desiredChannelIndex])
                                    storeLastActiveChannel(channelList[desiredChannelIndex])
                                    playPlayableItem(channelList[desiredChannelIndex])
                                    callback.onFailed(Error("All channels skipped"))
                                    return@runCoroutine
                                }
                                break
                            }
                        }
                        if (newDesiredChannelIndex < clSize) {
                            val playableItem = channelList[newDesiredChannelIndex]
                            desiredChannelIndex = newDesiredChannelIndex
                            storeActiveChannel(playableItem)
                            storeLastActiveChannel(playableItem)
                            playPlayableItem(playableItem)
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
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding previous channel for FavoriteGroup $activeFavGroupName")
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
                }
                else if ((activeCategoryId == FilterItemType.RADIO_CHANNELS_ID) || (activeCategoryId == FilterItemType.TERRESTRIAL_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.CABLE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.SATELLITE_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.RECENTLY_WATCHED_ID) || (activeCategoryId == FilterItemType.ANALOG_ANTENNA_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.ANALOG_CABLE_TUNER_TYPE_ID)|| (activeCategoryId == FilterItemType.ANALOG_TUNER_TYPE_ID)
                    || (activeCategoryId == FilterItemType.PREFERRED_SATELLITE_TUNER_TYPE_ID) || (activeCategoryId == FilterItemType.GENERAL_SATELLITE_TUNER_TYPE_ID)
                ) {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding previous channel for Category: $activeCategoryId")
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
                        FilterItemType.ANALOG_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.tunerType == TunerType.ANALOG_TUNER_TYPE
                                    && getAnalogServiceListID(channel) == TunerType.TYPE_ANALOG) {
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

                        FilterItemType.PREFERRED_SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.internalProviderId != null && channel.internalProviderId?.contains(preferredSatellite)!!) {
                                    selectedChannelList.add(channel)
                                }
                            }
                        }

                        FilterItemType.GENERAL_SATELLITE_TUNER_TYPE_ID -> {
                            sortedChannelList.forEach { channel ->
                                if (channel.internalProviderId != null && channel.internalProviderId?.contains(generalSatellite)!!) {
                                    selectedChannelList.add(channel)
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
                }
                else if (activeCategoryId >= FilterItemType.TIF_INPUT_CATEGORY) {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Finding previous channel for TIF Category: $activeCategoryId")
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
                        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Channel previous switch for $activeCategoryId")
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
                }
                else if (activeCategoryId == FilterItemType.GENRE_ID) {
                    var selectedChannelList = java.util.ArrayList<TvChannel>()
                    getChannelList().forEach { channel ->
                        if (channel.genres.contains(activeGenreCategoryName)) {
                            selectedChannelList.add(channel)
                        }
                    }
                    if (selectedChannelList.size > 0) {
                        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Channel previous switch for $activeCategoryId")
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
                }
                else {
                    //ALL category
                    try {
                        val channelList = getChannelList()
                        var isAllSkippedChannels = true
                        channelList.forEach{tvChannel ->
                            if(tvChannel.isBrowsable && !tvChannel.isSkipped){
                                isAllSkippedChannels = false
                            }
                        }

                        newDesiredChannelIndex = if (desiredChannelIndex <= 0) {
                            channelList.size - 1
                        } else {
                            (desiredChannelIndex - 1)
                        }

                        val clSize = channelList.size
                        var iter = 0
                        var tvChannel = channelList[newDesiredChannelIndex]
                        while (tvChannel.isSkipped || (!tvChannel.isBrowsable && !utilsInterface.isThirdPartyChannel(tvChannel))){
                            newDesiredChannelIndex -= 1
                            iter ++

                            if (newDesiredChannelIndex < 0) {
                                newDesiredChannelIndex = clSize - 1
                            }
                            tvChannel = channelList[newDesiredChannelIndex]
                            if (iter >= clSize) {
                                if(isAllSkippedChannels){
                                    storeActiveChannel(channelList[desiredChannelIndex])
                                    storeLastActiveChannel(channelList[desiredChannelIndex])
                                    playPlayableItem(channelList[desiredChannelIndex])
                                    callback.onFailed(Error("All channels skipped"))
                                    return@runCoroutine
                                }
                                break
                            }
                        }
                        if (newDesiredChannelIndex < clSize) {
                            val playableItem = channelList[newDesiredChannelIndex]
                            desiredChannelIndex = newDesiredChannelIndex
                            storeActiveChannel(playableItem)
                            storeLastActiveChannel(playableItem)
                            playPlayableItem(playableItem)
                            callback.onSuccess()
                        }
                    } catch (E: Exception) {
                        E.printStackTrace()
                    }
                }
            })
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
                var isAllSkippedChannels = true
                //Get the current channel position in selectedChannelList
                selectedChannelList.forEach { item ->
                    if(item.isBrowsable && !item.isSkipped){
                        isAllSkippedChannels = false
                    }

                    if (channel!!.id == item.id) {
                        //Update the new index for playing next channel
                        newDesiredChannelIndex =
                            if (index >= selectedChannelList.size - 1) 0 else (index + 1)
                    }
                    index++
                }

                val includeSkippedChannels = isAllSkippedChannels && currentActiveChannel?.isBrowsable == false

                if ((selectedChannelList[newDesiredChannelIndex]!!.isSkipped && !includeSkippedChannels) || (!selectedChannelList[newDesiredChannelIndex]!!.isBrowsable  && !utilsInterface.isThirdPartyChannel(selectedChannelList[newDesiredChannelIndex]))) {
                    var numberOfSkipped = 0
                    do {
                        newDesiredChannelIndex =
                            if (newDesiredChannelIndex >= selectedChannelList.size - 1) 0 else (newDesiredChannelIndex + 1)
                        numberOfSkipped++
                        if (numberOfSkipped == selectedChannelList.size) {
                            break
                        }
                    } while ((selectedChannelList[newDesiredChannelIndex]!!.isSkipped && !includeSkippedChannels) || (!selectedChannelList.get(newDesiredChannelIndex).isBrowsable  && !utilsInterface.isThirdPartyChannel(selectedChannelList[newDesiredChannelIndex])))
                    if (numberOfSkipped >= selectedChannelList.size) {
                        //only one channel, channel switch not required
                        callback.onFailed(Error("Single Entry in List"))
                        return@runCoroutine
                    }
                }

                val playableItem = selectedChannelList[newDesiredChannelIndex]
                desiredChannelIndex = findPosition(playableItem)
                storeActiveChannel(playableItem)
                storeLastActiveChannel(playableItem)
                playPlayableItem(playableItem)
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
                storeActiveChannel(playableItem)
                storeLastActiveChannel(playableItem)
                playPlayableItem(playableItem)
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
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +
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
                storeLastActiveChannel(playableItem)
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
                storeLastActiveChannel(playableItem)
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

    override fun startInitialPlayback(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.startInitialPlayback(callback, applicationMode)
        } else {
            getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onReceive(tvChannel: TvChannel) {
                    //since channel list contains some non browsable channels and active channel is 0 when we restart app
                    //and if first channel is non browsable it would show black as non browsable channel will be zapped.
                    if (!tvChannel.isBrowsable){
                        val channelList = getChannelList()
                        var found = false
                        run breaking@{
                            channelList.forEach {
                                if (it.isBrowsable){
                                    found = true
                                    changeChannel(it,callback)
                                    return@breaking
                                }
                            }
                        }
                        if (!found){
                            changeChannel(tvChannel, callback)
                        }

                    }else{
                        changeChannel(tvChannel, callback)
                    }
                    
                }

                override fun onFailed(error: Error) {
                    callback.onFailed(error)
                }
            })
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
                    activeChannel = data
                    if (dataProvider.lockUnlockChannel(tvChannel, lockUnlock)) {
                        tvChannel.isLocked = lockUnlock
                        if(isParentalEnabled(applicationMode)){
                            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"parental enabled so add locked channels to prefs ${tvChannel.name}")
                            utilsInterface.updateLockedChannelIdsToPref(context,lockUnlock,tvChannel.getUniqueIdentifier())
                        }
                        val position = findPosition(tvChannel)
                        val channelList = getChannelList()
                        if (channelList.isNotEmpty() && position >= 0 && position < channelList.size) {
                            channelList.get(position).isLocked = lockUnlock
                        }
                        activeChannel = data
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
                                data.isLocked = lockUnlock
                                playerInterface.isChannelUnlocked = false
                                playerInterface.playbackStatus.value = if (lockUnlock) {
                                    playerInterface.play(activeChannel!!)
                                    PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT
                                } else {
                                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"active channel is unlocked so again re-tune to avoid black screen")
                                    playerInterface.play(activeChannel!!)
                                    PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT
                                }
                            }
                            callback.onSuccess()
                        }
                    }
                    callback.onFailed(Error("Some error while updating DB"))
                }

            })
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun skipUnskipChannel(tvChannel: TvChannel, skipUnskip: Boolean, applicationMode: ApplicationMode): Boolean {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            return super.skipUnskipChannel(tvChannel, skipUnskip, applicationMode)
        } else {
            var skippedChannelsData = utilsInterface.getPrefsValue(SKIPPED_CHANNELS_TAG, "") as String
            // Checked isNotEmpty due to mal format data in previous version
            val skippedChannelIds = skippedChannelsData.split(",").filter { it.isNotEmpty() }.toMutableList()
            // Retain only IDs that are present in the channel list
            skippedChannelIds.retainAll(getChannelList().map { it.channelId.toString() })
            if (skipUnskip) {
                skippedChannelIds.add(tvChannel.channelId.toString())
            } else {
                skippedChannelIds.remove(tvChannel.channelId.toString())
            }
            tvChannel.isSkipped = skipUnskip
            getChannelById(tvChannel.channelId.toInt())?.isSkipped = skipUnskip
            skippedChannelsData = skippedChannelIds.joinToString(",")
            utilsInterface.setPrefsValue(SKIPPED_CHANNELS_TAG, skippedChannelsData)
            return true
        }
    }



    @RequiresApi(Build.VERSION_CODES.S)
    fun updateChannels(mTIFChannelInfos: List<TvChannel>) {
        //all of this bellow is mtk code
        val valuesList = arrayListOf<ContentValues>()
        val chListForDb = getAllChannelListForDb()
        if(chListForDb == null || chListForDb.size == 0){
            return
        }
        var editType = 0

        for (info in mTIFChannelInfos) {
            val originalChannel = chListForDb.get(info.id.toLong())
            if (originalChannel == null) continue
            var updateChannelLocal = false

            val values = ContentValues()

            if (info.displayNumber != originalChannel.displayNumber) {
                editType = 2
                values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, info.displayNumber)
                originalChannel.displayNumber = info.displayNumber
                updateChannelLocal = true
            }

            if (info.name != originalChannel.name) {
                editType = 1
                values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, info.name)
                originalChannel.name = info.name
                updateChannelLocal = true
            }

            if (info.isBrowsable != originalChannel.isBrowsable) {
                values.put(TvContract.Channels.COLUMN_BROWSABLE, if (info.isBrowsable) 1 else 0)
                originalChannel.isBrowsable = info.isBrowsable
                updateChannelLocal = true
            }

            if (info.providerFlag1 != originalChannel.providerFlag1) {
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, info.providerFlag1)
                originalChannel.providerFlag1 = info.providerFlag1
                updateChannelLocal = true
            }

            if (info.providerFlag2 != originalChannel.providerFlag2) {
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG2, info.providerFlag2)
                originalChannel.providerFlag2 = info.providerFlag2
                updateChannelLocal = true
            }

            if (info.providerFlag3 != originalChannel.providerFlag3) {
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG3, info.providerFlag3)
                originalChannel.providerFlag3 = info.providerFlag3
                updateChannelLocal = true
            }

            if (info.providerFlag4 != originalChannel.providerFlag4) {
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG4, info.providerFlag4)
                originalChannel.providerFlag4 = info.providerFlag4
                updateChannelLocal = true
            }

            if (updateChannelLocal) {
                values.put(TvContract.Channels._ID, info.id)
                valuesList.add(values)
            }
        }

        try {
            updateTis(mTIFChannelInfos, valuesList.size > 0, valuesList, editType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("Range")
    @RequiresApi(Build.VERSION_CODES.S)
    fun getAllChannelListForDb(): HashMap<Long, TvChannel>? {
        Log.d(TAG, "getAllChannelListForDb() start")
        val result = HashMap<Long, TvChannel>()
        var cursor: Cursor? = null
        val mContentResolver: ContentResolver = context.contentResolver
        var inputList = getInputIds(context)
        var lChannelList = arrayListOf<TvChannel>()
        lChannelList.clear()

        try {
            if (inputList!!.isNotEmpty()) {
                for (input in inputList) {
                    if (input.contains("com.google.android.videos") || input.contains("anoki", ignoreCase = true)) {
                        continue
                    }
                    var cursor = mContentResolver.query(
                        TvContract.buildChannelsUriForInput(input),
                        null,
                        null,
                        null,
                        null
                    )
                    if (cursor!!.count > 0) {
                        cursor.moveToFirst()
                        do {
                            try {
                                var tvChannel =
                                    com.iwedia.cltv.platform.base.content_provider.createChannelFromCursor(
                                        context,
                                        cursor,
                                        lChannelList.size
                                    )

                                result.put(tvChannel!!.channelId, tvChannel)
                            } catch (e: Exception) {
                                Log.e(TAG, "loadChannels: failed to load channel ${e.message}")
                            }
                        } while (cursor.moveToNext())
                    }
                    cursor!!.close()
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        Log.d(TAG, "getAllChannelListForDb() end")
        return result
    }


    @RequiresApi(Build.VERSION_CODES.S)
    fun updateTis(allChList: List<TvChannel>, updatechannel: Boolean, valuesList: ArrayList<ContentValues>, editType: Int) {
        initSyncSvl(allChList)
        if(updatechannel){
            updateChannelValue(valuesList, editType)
        }
        forceChannelsRefresh()
    }

    fun initSyncSvl(allChList: List<TvChannel>) {
        if (allChList.isEmpty()) {
            return;
        }
        var recordIdList: ArrayList<String> = arrayListOf()
        allChList.forEachIndexed { index, info ->
            var mInternalProviderId: String = info.internalProviderId.toString()
            if (TextUtils.isEmpty(mTempServiceListId)) {
                var mChannelListId: String = info.channelId.toString()
                mTempServiceListId = mChannelListId.replace("MEDIATEK_CHANNEL_", "SERVICE_")
            }
            if (TextUtils.isEmpty(mInternalProviderId)) {
                return@forEachIndexed
            }

            var newInternalProviderId :String = mInternalProviderId.replace("channels://MEDIATEK_CHANNEL_", "service://SERVICE_")
            if (!TextUtils.isEmpty(newInternalProviderId)) {
                recordIdList.add(newInternalProviderId)
            }
        }
        reSortBundle = Bundle()
        reSortBundle.putStringArrayList("editChannel", recordIdList)
    }

    fun updateChannelValue(valuesList: ArrayList<ContentValues>, editType: Int){
        try {
            val opUpdateValues = ArrayList<ContentProviderOperation>()
            for (values in valuesList) {
                val idValue = values[TvContract.Channels._ID]
                if (idValue != null) {
                    var id: Long = 0
                    if (idValue is Int){
                        id = idValue.toLong()
                    }else if(idValue is Long){
                        id = idValue
                    }else if(idValue is Boolean){
                        if(idValue){
                            id = 1
                        }
                    }else{
                        return
                    }
                    val selection: String = buildSelectionForId(TvContract.Channels._ID, id)
                    values.remove(TvContract.Channels._ID)
                    opUpdateValues.add(
                        ContentProviderOperation.newUpdate(TvContract.Channels.CONTENT_URI)
                            .withValues(values)
                            .withSelection(selection, null)
                            .build()
                    )
                }
            }
            val mContentProviderResult: Array<ContentProviderResult> = context.contentResolver.applyBatch(
                TvContract.AUTHORITY, opUpdateValues
            )
        } catch (e: OperationApplicationException) {
            e.printStackTrace()
        } catch (e: RemoteException) {
            e.printStackTrace()
        } finally {
            updateChannelEvent(reSortBundle, mTempServiceListId, editType)
        }
    }

    fun updateChannelEvent(bundle: Bundle, serviceListId: String, editType: Int) {
        do {
            if (editType == -1) { //close dialog
                break
            }
            val manager = ServiceListEditManager(context, DVBC_INPUT_ID0)
            val listener: ServiceListEditManager.ServiceListEditListener = object :
                ServiceListEditManager.ServiceListEditListener {
                override fun onCompleted(requestId: Int, result: Bundle) {
                    val closeResult = manager.close()!!
                }
            }
            val openResult = manager.open(listener)!!
            if (openResult >= 0) { //open success
                val result = manager.putRecordIdList(serviceListId, bundle, editType)!!
                if (result >= 0) { //putRecordIdList success
                    val commitResult = manager.commit()!!
                    if (commitResult < 0) { //commit fail
                        val closeResult = manager.close()!!
                        break
                    }
                } else { //putRecordIdList fail
                    val closeResult = manager.close()!!
                    break
                }
            }
        } while (false)
    }


    private fun buildSelectionForId(idName: String, mId: Long): String {
        val sb = StringBuilder()
        sb.append(idName).append(" == ").append(mId)
        return sb.toString()
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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun forceChannelsRefresh(applicationMode: ApplicationMode) {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            super.forceChannelsRefresh(applicationMode)
        } else {
            var provider: ChannelDataProvider = dataProvider as ChannelDataProvider
            provider.loadChannelsSync()
        }
    }

    override fun checkAndRunBarkerChannel(run : Boolean) {

        var currentEpgModule = epgModule as EpgInterfaceImpl
        getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onFailed(error: Error) {
            }

            override fun onReceive(data: TvChannel) {
                if (run) {
                    if (currentEpgModule.checkIfBarkerChannel(data)) {
                        if(!isBarkerRunning) {
                            CoroutineHelper.runCoroutine({
                                playerInterface.stop()
                            }, Dispatchers.Main)
                            currentEpgModule.tuneToBarkerChannel(data)
                            isBarkerRunning = true
                        }
                    }
                } else {
                    if (currentEpgModule.checkIfBarkerChannel(data)) {
                        if(isBarkerRunning) {
                            currentEpgModule.cancelBarkerChannel()
                            CoroutineHelper.runCoroutine({
                                playerInterface.play(data)
                            }, Dispatchers.Main)
                            isBarkerRunning = false
                        }
                    }
                }
            }
        })
    }

    override fun dispose() {
        super.dispose()
        AudioInterfaceImpl.dispose()
    }
}
