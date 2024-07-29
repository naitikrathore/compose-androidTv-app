package com.iwedia.cltv.platform.gretzky

import android.media.tv.TvContentRating
import android.os.Bundle
import com.iwedia.cltv.platform.`interface`.ParentalControlSettingsInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.base.player.PlayerBaseImpl
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import kotlinx.coroutines.Dispatchers

internal class PlayerInterfaceImpl(utilsInterface: UtilsInterface,parentalControlSettingsModule: ParentalControlSettingsInterface) : PlayerBaseImpl(utilsInterface,parentalControlSettingsModule) {

    override var playerState: PlayerState = PlayerState.IDLE
    override var isTimeShiftAvailable = false

    init {
        super.mListeners.add(object : PlayerInterface.PlayerListener {
            override fun onNoPlayback() {
            }

            override fun onPlaybackStarted() {
            }

            override fun onAudioTrackUpdated(audioTracks: List<IAudioTrack>) {
            }

            override fun onSubtitleTrackUpdated(subtitleTracks: List<ISubtitle>) {
            }

            override fun onVideoAvailable(inputId: String) {
                wasScramble = false
                if (isParentalActive)
                    playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
                else
                    playbackStatus.value = PlaybackStatus.PLAYBACK_INIT

                //Hide black overlay after 1sec
                CoroutineHelper.runCoroutineWithDelay({
                    if (!isMuted) {
                        unmute()
                    }
                    InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_HIDE_BLACK_OVERLAY)
                }, 1000)
            }

            override fun onVideoUnAvailable(reason: Int, inputId: String) {
                if (reason == 256 || reason == 18) {
                    wasScramble = true
                    playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
                } else if (reason == 1) {
                    playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                } else if (reason == 2) {
                    playbackStatus.value = PlaybackStatus.NO_PLAYBACK
                } else {
                    playbackStatus.value = PlaybackStatus.WAITING_FOR_CHANNEL
                }
            }

            override fun onContentAvailable() {
                playbackStatus.value = PlaybackStatus.PARENTAL_PIN_HIDE
                isParentalActive = false
                if (!wasScramble) {
                    playbackStatus.value = PlaybackStatus.PLAYBACK_STARTED
                } else {
                    playbackStatus.value = PlaybackStatus.SCRAMBLED_CHANNEL
                }
            }

            override fun onContentBlocked(rating: TvContentRating) {
                blockedRating = rating
                isParentalActive = true
                playbackStatus.value = PlaybackStatus.PARENTAL_PIN_SHOW
            }

            override fun onTimeShiftStatusChanged(inputId: String, status: Boolean) {}

            override fun onEvent(inputId: String, eventType: String, eventArgs: Bundle) {}

            override fun onTrackSelected(inputId: String, type: Int, trackId: String?) {
            }
        })
    }

    override fun play(playableItem: Any) {
        super.play(playableItem)
        if(playableItem is TvChannel){
            // Show black overlay during channel switching
            if (!(playableItem as TvChannel).isLocked && !wasScramble) {
                CoroutineHelper.runCoroutine({
                    InformationBus.informationBusEventListener?.submitEvent(Events.PLAYBACK_SHOW_BLACK_OVERLAY)
                    mute()
                }, Dispatchers.Main)
            }
        }
    }

}