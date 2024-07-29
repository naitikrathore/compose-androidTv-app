package com.iwedia.cltv.platform.mal_service

import android.content.Context
import android.media.tv.TvInputInfo
import android.util.Log
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncListener
import com.cltv.mal.model.async.IAsyncTvChannelListener
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.PlaybackStatusInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.mal_service.player.PlaybackStatusInterfaceBaseImpl
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.channel.FilterItemType
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlayableItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TvInterfaceImpl(private val serviceImpl: IServiceAPI, val playerInterface: PlayerInterface, val networkInterface: NetworkInterface, val utilsInterface: UtilsInterface,) : TvInterface {

    override lateinit var playbackStatusInterface: PlaybackStatusInterface
    override var activeCategoryId: FilterItemType
        get() = TODO("Not yet implemented")
        set(value) {}

    init {
        playbackStatusInterface = PlaybackStatusInterfaceBaseImpl(this, playerInterface, networkInterface, utilsInterface)
        InformationBus.informationBusEventListener.registerEventListener(arrayListOf(Events.FAVORITE_LIST_UPDATED, Events.CHANNEL_LIST_UPDATED), {}, {
            clearChannelList()
        })
    }
    override fun setup() {}

    override fun setupDefaultService(channels: List<TvChannel>, applicationMode: ApplicationMode) {
        var channelList = arrayListOf<com.cltv.mal.model.entities.TvChannel>()
        channels.forEach { channel ->
            channelList.add(toServiceChannel(channel))
        }
        serviceImpl.setupDefaultService(
            channelList.toTypedArray(),
            applicationMode.ordinal
        )
    }

    override fun dispose() {}

    override fun changeChannel(
        channel: TvChannel,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        //CoroutineHelper.runCoroutine({
            serviceImpl.playChannel(toServiceChannel(channel), applicationMode.ordinal, object : IAsyncListener.Stub() {
                override fun onFailed(data: String?) {
                    callback.onFailed(Error(data))
                }

                override fun onSuccess() {
                    callback.onSuccess()
                    playChannel()
                }
            })
        //})
    }

    override fun changeChannel(
        index: Int,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        //CoroutineHelper.runCoroutine({
            serviceImpl.changeChannel(index, applicationMode.ordinal, object : IAsyncListener.Stub() {
                override fun onSuccess() {
                    callback.onSuccess()
                    playChannel()
                }

                override fun onFailed(data: String?) {
                    callback.onFailed(Error(data))
                }
            })
       // })
    }

    override fun getSelectedChannelList(
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode,
        filter: FilterItemType?,
        filterMetadata: String?
    ) {
        //CoroutineHelper.runCoroutine({
            var result = arrayListOf<TvChannel>()
            serviceImpl.getSelectedChannelList(
                applicationMode.ordinal,
                filter?.ordinal ?: 0,
                filterMetadata ?: "",
                object : IAsyncTvChannelListener.Stub() {
                    override fun onResponse(response: Array<out com.cltv.mal.model.entities.TvChannel>?) {
                        response?.forEach {
                            result.add(fromServiceChannel(it))
                        }
                        callback.onReceive(result)
                    }
                }
            )
       // })

    }

    override fun nextChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        //CoroutineHelper.runCoroutine({
            serviceImpl.nextChannel(
                applicationMode.ordinal,
                object : IAsyncListener.Stub() {
                    override fun onSuccess() {
                        callback.onSuccess()
                        playChannel()
                    }

                    override fun onFailed(error : String) {
                        callback.onFailed(Error(error))
                    }
                })
       // })
    }

    override fun previousChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        serviceImpl.previousChannel(
            applicationMode.ordinal,
            object : IAsyncListener.Stub() {
                override fun onSuccess() {
                    callback.onSuccess()
                    playChannel()
                }

                override fun onFailed(error : String) {
                    callback.onFailed(Error(error))
                }
            })
    }

    override fun getLastActiveChannel(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        serviceImpl.getLastActiveChannel(applicationMode.ordinal)
        callback.onSuccess()
    }

    override fun getChannelById(channelId: Int, applicationMode: ApplicationMode): TvChannel? {
        var tvChannel = serviceImpl.getChannelById(channelId, applicationMode.ordinal)
        return fromServiceChannel(tvChannel)
    }

    override fun findChannelPosition(tvChannel: TvChannel, applicationMode: ApplicationMode): Int {
        return serviceImpl.findChannelPosition(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    override fun playNextIndex(
        selectedChannelList: ArrayList<TvChannel>,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        //CoroutineHelper.runCoroutine({
            var list = arrayListOf<com.cltv.mal.model.entities.TvChannel>()
            selectedChannelList.forEach {
                list.add(toServiceChannel(it))
            }
            serviceImpl.playNextIndex(
                list.toTypedArray(),
                applicationMode.ordinal,
                object : IAsyncListener.Stub() {
                    override fun onSuccess() {
                        callback.onSuccess()
                        playChannel()
                    }

                    override fun onFailed(error : String) {
                        callback.onFailed(Error(error))
                    }
                })
        //})
    }

    override fun playPrevIndex(
        selectedChannelList: ArrayList<TvChannel>,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        //CoroutineHelper.runCoroutine({
            var list = arrayListOf<com.cltv.mal.model.entities.TvChannel>()
            selectedChannelList.forEach {
                list.add(toServiceChannel(it))
            }
            serviceImpl.playPrevIndex(
                list.toTypedArray(),
                applicationMode.ordinal,
                object : IAsyncListener.Stub() {
                    override fun onSuccess() {
                        callback.onSuccess()
                        playChannel()
                    }

                    override fun onFailed(error : String) {
                        callback.onFailed(Error(error))
                    }
                })
        //})

    }

    override fun getChannelByDisplayNumber(
        displayNumber: String,
        applicationMode: ApplicationMode
    ): TvChannel? {
        var channel = serviceImpl.getChannelByDisplayNumber(
            displayNumber,
            applicationMode.ordinal
        )
        return fromServiceChannel(channel)
    }

    override fun enableLcn(enableLcn: Boolean, applicationMode: ApplicationMode) {
        serviceImpl.enableLcn(enableLcn, applicationMode.ordinal)
    }

    override fun updateDesiredChannelIndex(applicationMode: ApplicationMode) {
        serviceImpl.updateDesiredChannelIndex(applicationMode.ordinal)
    }

    override fun updateLaunchOrigin(
        categoryId: Int,
        favGroupName: String,
        tifCategoryName: String,
        genreCategoryName: String,
        applicationMode: ApplicationMode
    ) {
        serviceImpl.updateLaunchOrigin(
            categoryId,
            favGroupName,
            tifCategoryName,
            genreCategoryName,
            applicationMode.ordinal
        )
    }

    @Synchronized
    override fun getActiveChannel(
        callback: IAsyncDataCallback<TvChannel>,
        applicationMode: ApplicationMode
    ) {
       // CoroutineHelper.runCoroutine({
            try{
                if (playerInterface.activePlayableItem is TvChannel) {
                    callback.onReceive(playerInterface.activePlayableItem as TvChannel)
                    return
                }
            } catch (e: Exception) {
                try {
                    var channel = serviceImpl.getActiveChannel(applicationMode.ordinal)
                    if (channel != null)
                        callback.onReceive(fromServiceChannel(channel))
                    else
                        callback.onFailed(Error("Active channel is null"))
                } catch (e: Exception) {}
            }
       // })

    }

    @Synchronized
    override fun storeActiveChannel(tvChannel: TvChannel, applicationMode: ApplicationMode) {
        serviceImpl.storeActiveChannel(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    @Synchronized
    override fun storeLastActiveChannel(channel: TvChannel, applicationMode: ApplicationMode) {
        serviceImpl.storeActiveChannel(
            toServiceChannel(channel),
            applicationMode.ordinal
        )
    }

    override fun getChannelByIndex(index: Int, applicationMode: ApplicationMode): TvChannel {
        var channel = serviceImpl.getChannelByIndex(index, applicationMode.ordinal)
        return fromServiceChannel(channel)
    }

    private var channelListFast = arrayListOf<TvChannel>()
    private var channelListBroadcast = arrayListOf<TvChannel>()
    private fun clearChannelList() {
        channelListFast.clear()
        channelListBroadcast.clear()
    }

    override fun getChannelList(applicationMode: ApplicationMode): ArrayList<TvChannel> {
        if (applicationMode == ApplicationMode.FAST_ONLY) {
            if (channelListFast.isNotEmpty()) {
                return channelListFast
            }
            channelListFast.clear()
            serviceImpl.getChannelList(applicationMode.ordinal).forEach {
                channelListFast.add(fromServiceChannel(it))
            }
            Log.d(Constants.LogTag.CLTV_TAG + "MAL", "tv interface fast list size ${channelListFast.size}")
            return channelListFast
        }
        if (applicationMode == ApplicationMode.DEFAULT) {
            if (channelListBroadcast.isNotEmpty()) {
                return channelListBroadcast
            }
            channelListBroadcast.clear()
            serviceImpl.getChannelList(applicationMode.ordinal).forEach {
                channelListBroadcast.add(fromServiceChannel(it))
            }
            Log.d(Constants.LogTag.CLTV_TAG + "MAL", "tv interface broadcast list size ${channelListBroadcast.size}")
            return channelListBroadcast
        }
        var list = arrayListOf<TvChannel>()
        serviceImpl.getChannelList(applicationMode.ordinal).forEach {
            list.add(fromServiceChannel(it))
        }
        Log.d(Constants.LogTag.CLTV_TAG + "MAL", "tv interface list size ${list.size}")
        return list
    }

    override fun getBrowsableChannelList(applicationMode: ApplicationMode): ArrayList<TvChannel> {
        val channelList = arrayListOf<TvChannel>()
        serviceImpl.getChannelList(applicationMode.ordinal).forEach { tvChannel ->
            if (tvChannel.isBrowsable) channelList.add(fromServiceChannel(tvChannel))
        }
        return channelList
    }

    override fun getChannelListAsync(
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        applicationMode: ApplicationMode
    ) {
        CoroutineHelper.runCoroutine({
            var list = arrayListOf<TvChannel>()
            serviceImpl.getChannelList(applicationMode.ordinal).forEach {
                list.add(fromServiceChannel(it))
            }
            callback.onReceive(list)
        })
    }

    override fun nextChannelByCategory(
        categoryId: Int,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        //CoroutineHelper.runCoroutine({
            serviceImpl.nextChannelByCategory(
                categoryId,
                applicationMode.ordinal,
                object : IAsyncListener.Stub() {
                    override fun onSuccess() {
                        callback.onSuccess()
                        playChannel()
                    }

                    override fun onFailed(error : String) {
                        callback.onFailed(Error(error))
                    }
                })
       // })

    }

    override fun previousChannelByCategory(
        categoryId: Int,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        //CoroutineHelper.runCoroutine({
            serviceImpl.previousChannelByCategory(
                categoryId,
                applicationMode.ordinal,
                object : IAsyncListener.Stub() {
                    override fun onSuccess() {
                        callback.onSuccess()
                        playChannel()
                    }

                    override fun onFailed(error : String) {
                        callback.onFailed(Error(error))
                    }
                })
       // })

    }

    override fun setRecentChannel(channelIndex: Int, applicationMode: ApplicationMode) {
        serviceImpl.setRecentChannel(channelIndex, applicationMode.ordinal)
    }

    override fun startInitialPlayback(callback: IAsyncCallback, applicationMode: ApplicationMode) {
        serviceImpl.startInitialPlayback(applicationMode.ordinal)
        callback.onSuccess()
        playChannel()
    }

    override fun addRecentlyWatched(playableItem: PlayableItem, applicationMode: ApplicationMode) {
        TODO("Not yet implemented")
    }

    override fun getRecentlyWatched(applicationMode: ApplicationMode): MutableList<PlayableItem>? {
        TODO("Not yet implemented")
    }

    override fun deleteChannel(tvChannel: TvChannel, applicationMode: ApplicationMode): Boolean {
        return serviceImpl.deleteChannel(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    override fun lockUnlockChannel(
        tvChannel: TvChannel,
        lockUnlock: Boolean,
        callback: IAsyncCallback,
        applicationMode: ApplicationMode
    ) {
        CoroutineHelper.runCoroutine({
            serviceImpl.lockUnlockChannel(
                toServiceChannel(tvChannel),
                lockUnlock,
                applicationMode.ordinal,
                object : IAsyncListener.Stub() {
                    override fun onFailed(error : String) {
                        callback.onFailed(Error(error))
                    }

                    override fun onSuccess() {
                        tvChannel.isLocked = lockUnlock
                        callback.onSuccess()
                    }
                })
        })

    }

    override fun isChannelLockAvailable(
        tvChannel: TvChannel,
        applicationMode: ApplicationMode
    ): Boolean {
        return serviceImpl.isChannelLockAvailable(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    override fun skipUnskipChannel(
        tvChannel: TvChannel,
        skipUnskip: Boolean,
        applicationMode: ApplicationMode
    ): Boolean {
        return serviceImpl.skipUnskipChannel(
            toServiceChannel(tvChannel),
            skipUnskip,
            applicationMode.ordinal
        )
    }

    override fun getDesiredChannelIndex(applicationMode: ApplicationMode): Int {
        return serviceImpl.getDesiredChannelIndex(applicationMode.ordinal)
    }

    override fun getTvInputList(
        receiver: IAsyncDataCallback<MutableList<TvInputInfo>>,
        applicationMode: ApplicationMode
    ) {
        var result = mutableListOf<TvInputInfo>()
        result.addAll(serviceImpl.getTvInputListTvInterface(applicationMode.ordinal))
        receiver.onReceive(result)
    }

    override fun getChannelListByCategories(
        callback: IAsyncDataCallback<ArrayList<TvChannel>>,
        entityCategory: FilterItemType?,
        applicationMode: ApplicationMode
    ) {
        CoroutineHelper.runCoroutine({
            var result = arrayListOf<TvChannel>()
            serviceImpl.getChannelListByCategories(
                entityCategory!!.ordinal,
                applicationMode.ordinal
            ).forEach {
                result.add(fromServiceChannel(it))
            }
            callback.onReceive(result)
        })
    }

    override fun isParentalEnabled(applicationMode: ApplicationMode): Boolean {
        return serviceImpl.isParentalEnabledTvInterface(applicationMode.ordinal)
    }

    override fun isTvNavigationBlocked(applicationMode: ApplicationMode): Boolean {
        return serviceImpl.isTvNavigationBlocked(applicationMode.ordinal)
    }

    override fun initSkippedChannels(applicationMode: ApplicationMode) {
        serviceImpl.initSkippedChannels(applicationMode.ordinal)
    }

    override fun isChannelLocked(channelId: Int, applicationMode: ApplicationMode): Boolean {
        return serviceImpl.isChannelLocked(channelId, applicationMode.ordinal)
    }

    override fun getTifChannelSourceLabel(
        tvChannel: TvChannel,
        applicationMode: ApplicationMode
    ): String {
        return serviceImpl.getTifChannelSourceLabel(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    override fun getChannelSourceType(
        tvChannel: TvChannel,
        applicationMode: ApplicationMode
    ): String {
        return serviceImpl.getChannelSourceType(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    override fun getAnalogTunerTypeName(
        tvChannel: TvChannel,
        applicationMode: ApplicationMode
    ): String {
        return serviceImpl.getAnalogTunerTypeName(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    override fun getAnalogServiceListID(
        tvChannel: TvChannel,
        applicationMode: ApplicationMode
    ): Int {
        return serviceImpl.getAnalogServiceListID(
            toServiceChannel(tvChannel),
            applicationMode.ordinal
        )
    }

    override fun getParentalRatingDisplayName(
        parentalRating: String?,
        applicationMode: ApplicationMode,
        tvEvent: TvEvent
    ): String {
        return serviceImpl.getParentalRatingDisplayNameTvInterface(
            parentalRating,
            applicationMode.ordinal,
            toServiceTvEvent(tvEvent)
        )
    }

    override fun isLcnEnabled(applicationMode: ApplicationMode): Boolean {
        return serviceImpl.isLcnEnabled(applicationMode.ordinal)
    }

    override fun getLockedChannelListAfterOta(applicationMode: ApplicationMode) {
        serviceImpl.getLockedChannelListAfterOta(applicationMode.ordinal)
    }

    override fun getVisuallyImpairedAudioTracks(applicationMode: ApplicationMode): List<String> {
        var result = arrayListOf<String>()
        serviceImpl.getVisuallyImpairedAudioTracks(applicationMode.ordinal).forEach {
            result.add(it)
        }
        return result
    }

    override fun isChannelSelectable(channel : TvChannel) : Boolean {
        return serviceImpl.isChannelSelectable(toServiceChannel(channel))
    }

    override fun forceChannelsRefresh(applicationMode: ApplicationMode) {
        serviceImpl.forceChannelsRefresh(applicationMode.ordinal)
    }
    override  fun addDirectTuneChannel(index: String, context: Context):TvChannel?{
        return null
    }

    override fun checkAndRunBarkerChannel(run: Boolean) {
        serviceImpl.checkAndRunBarkerChannel(run)
    }

    override fun isSignalAvailable(): Boolean {
        return playbackStatusInterface.isSignalAvailable
        //return serviceImpl.isSignalAvailable
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

    private fun playChannel() {
        /*val applicationMode = serviceImpl!!.applicationMode
        val channel = fromServiceChannel(
            serviceImpl!!.getActiveChannel(applicationMode)
        )
        println("@@@@ play channel ${channel.name}")
        CoroutineScope(Dispatchers.Main).launch {
            playerInterface.play(channel)
        }*/

    }

}