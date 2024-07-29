package com.iwedia.cltv.platform.mal_service.player

import android.media.tv.TvInputManager
import androidx.lifecycle.Observer
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.NetworkInterface
import com.iwedia.cltv.platform.`interface`.PlaybackStatusInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.network.NetworkData
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import kotlinx.coroutines.Dispatchers

open class PlaybackStatusInterfaceBaseImpl(
    private val tvModule: TvInterface,
    private var playerModule: PlayerInterface,
    private val networkModule: NetworkInterface,
    private val utilsInterface: UtilsInterface
) : PlaybackStatusInterface {

    override var isNetworkAvailable = false
    override var isSignalAvailable = true
    override var isWaitingChannel = false
    override var isChannelsAvailable = true
    override var isPlayerTimeout = false
    override var appJustStarted = false
    override var isPvrPlaybackActive = false
    override var isLockedOverlay = false

    init {
        //Register no signal events
        playerModule.playbackStatus.observeForever(
            object : Observer<PlaybackStatus> {
                override fun onChanged(status: PlaybackStatus) {
                    onPlaybackStatusChanged(status!!)
                }
            })

        networkModule.networkStatus.observeForever { networkStatusData ->
            if (networkStatusData == NetworkData.NoConnection) {
                if (isNetworkAvailable) {
                    // emit Event only first time when value is changed from true to false
                    InformationBus.informationBusEventListener.submitEvent(Events.NO_ETHERNET_EVENT)
                }
                isNetworkAvailable = false
                isPlayerTimeout = false
                checkActiveChannelPlayback()
            } else {
                isNetworkAvailable = true
                isPlayerTimeout = false
                checkActiveChannelPlayback()
                InformationBus.informationBusEventListener?.submitEvent(Events.ETHERNET_EVENT)
            }
        }
    }

    override fun onPlaybackStatusChanged(playbackStatus: PlaybackStatus) {
        if (playbackStatus == PlaybackStatus.PLAYBACK_INIT) {
            isSignalAvailable = true
            isWaitingChannel = false
            isPlayerTimeout = false
            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_NONE)
        } else if (playbackStatus == PlaybackStatus.NO_PLAYBACK) {
            isSignalAvailable = false
            isPlayerTimeout = false
            checkActiveChannelPlayback()
            InformationBus.informationBusEventListener?.submitEvent(Events.NO_PLAYBACK)
        } else if (playbackStatus == PlaybackStatus.PLAYER_TIMEOUT) {
            isPlayerTimeout = true
            checkActiveChannelPlayback()
            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYER_TIMEOUT)
        } else if (playbackStatus == PlaybackStatus.PLAYBACK_STARTED) {
            isLockedOverlay = false
            isSignalAvailable = true
            isWaitingChannel = false
            isPlayerTimeout = false
            checkActiveChannelPlayback()
            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STARTED)
        } else if (playbackStatus == PlaybackStatus.WAITING_FOR_CHANNEL) {
            isSignalAvailable = true
            isWaitingChannel = true
            isPlayerTimeout = false
            checkActiveChannelPlayback()
            InformationBus.informationBusEventListener?.submitEvent(Events.WAITING_FOR_CHANNEL)
        } else if (playbackStatus == PlaybackStatus.SCRAMBLED_CHANNEL) {
            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_SCRAMBLED)
        }else if (playbackStatus == PlaybackStatus.AUDIO_ONLY) {
            InformationBus.informationBusEventListener?.submitEvent(Events.NO_AV_OR_AUDIO_ONLY_CHANNEL,arrayListOf(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY))
        } else if (playbackStatus == PlaybackStatus.ACTIVE_CHANNEL_LOCKED_EVENT && !playerModule.isOnLockScreen) {
            playerModule.isOnLockScreen = true
            isLockedOverlay = true
            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_LOCKED)
        } else if (playbackStatus == PlaybackStatus.ACTIVE_CHANNEL_UNLOCKED_EVENT) {
            isLockedOverlay = false
            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_LOCKED)
        } else if (playbackStatus == PlaybackStatus.PARENTAL_PIN_SHOW) {
            isSignalAvailable = true
            val applicationMode = if(utilsInterface.getPrefsValue(UtilsInterface.APPLICATION_MODE, ApplicationMode.DEFAULT.ordinal) == ApplicationMode.DEFAULT.ordinal)  ApplicationMode.DEFAULT else  ApplicationMode.FAST_ONLY
            tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
                override fun onFailed(error: Error) {
                }

                override fun onReceive(activeChannel: TvChannel) {
                    println("get active channel $activeChannel || ${playerModule.isOnLockScreen} || ${activeChannel!!.isLocked}")
                    if (activeChannel == null && !(playerModule.isOnLockScreen)) {
                        playerModule.isOnLockScreen = true
                        InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
                    } else {
                        if (!activeChannel!!.isLocked && !(playerModule.isOnLockScreen)) {
                            playerModule.isOnLockScreen = true
                            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
                        } else if (playerModule.isChannelUnlocked && !(playerModule.isOnLockScreen)) {
                            playerModule.isOnLockScreen = true
                            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_PARENTAL)
                        }
                    }
                }
            },applicationMode)
        } else if (playbackStatus == PlaybackStatus.PARENTAL_PIN_HIDE) {
            playerModule.isOnLockScreen = false
            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_NOT_PARENTAL)
        }
    }

    /**
     * Check active channel playback status
     */
    private fun checkActiveChannelPlayback() {
        tvModule.getActiveChannel(object : IAsyncDataCallback<TvChannel> {
            override fun onReceive(activeChannel: TvChannel) {
                CoroutineHelper.runCoroutine({
                    if (isPvrPlaybackActive) {
                        InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_MESSAGE)
                        return@runCoroutine
                    }
                    if (!isSignalAvailable) {
                        isSignalAvailable = false
                        playerModule.isOnLockScreen = false
                        InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_NO_SIGNAL)
                        return@runCoroutine
                    } else {
                        if (isPlayerTimeout) {
                            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_NO_PLAYBACK)
                            return@runCoroutine
                        } else {
                            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_MESSAGE)
                        }
                    }
                    if (isNetworkAvailable) {
                        if (isPlayerTimeout) {
                            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_NO_PLAYBACK)
                            return@runCoroutine
                        } else {
                            InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_IS_MESSAGE)
                        }
                    }
                }, Dispatchers.Main)
            }

            override fun onFailed(error: Error) {
                InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_STATUS_MESSAGE_NO_PLAYBACK)
            }
        })
    }
}