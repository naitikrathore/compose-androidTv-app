package com.iwedia.cltv.platform.mal_service

import android.media.tv.TvContentRating
import android.media.tv.TvTrackInfo
import android.view.SurfaceHolder
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.lifecycle.MutableLiveData
import com.cltv.mal.IServiceAPI
import com.cltv.mal.model.async.IAsyncListener
import com.cltv.mal.model.entities.ServiceTvView
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.player.PlaybackStatus
import com.iwedia.cltv.platform.model.player.PlayerState
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import kotlin.reflect.KCallable

class PlayerInterfaceImpl(
    private val serviceImpl: IServiceAPI,
    private val utilsInterface: UtilsInterface
) : PlayerInterface {

    override var liveTabChannel: TvChannel?
        get() = null
        set(value) {}
    override var activePlayableItem: Any
        get() = fromServiceChannel(serviceImpl.getActiveChannel(ApplicationMode.DEFAULT.ordinal))
        set(value) {}
    override var playerState: PlayerState
        get() = PlayerState.values()[serviceImpl.playerState]
        set(value) {}
    override var isTimeShiftAvailable: Boolean
        get() = serviceImpl.isTimeShiftAvailable
        set(value) {}
    override var wasScramble: Boolean
        get() = serviceImpl.wasScramble()
        set(value) {}
    override var playbackStatus: MutableLiveData<PlaybackStatus>
        get() = if (serviceImpl != null) MutableLiveData(PlaybackStatus.values()[serviceImpl.getPlaybackStatus()]) else MutableLiveData(
            PlaybackStatus.PLAYBACK_STARTED
        )
        set(value) {}
    override var isParentalActive: Boolean
        get() = serviceImpl.isParentalActive
        set(value) {}
    override var blockedRating: TvContentRating?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var isChannelUnlocked: Boolean
        get() = serviceImpl.isChannelUnlocked
        set(value) {}
    override var isOnLockScreen: Boolean
        get() = serviceImpl.isOnLockScreen
        set(value) {}
    override var mServiceRef: KCallable<*>?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun setCaptionEnabled(enabled: Boolean) {
        serviceImpl.setCaptionEnabled(enabled)
    }

    override fun getIsParentalControlActive(): Boolean {
        return false
    }

    override fun selectSubtitle(subtitle: ISubtitle?) {
        if (subtitle == null) {
            serviceImpl.selectSubtitle(null, false)
        } else {
            serviceImpl.selectSubtitle(toServiceSubtitleTrack(subtitle), false)
        }
    }

    override fun selectAudioTrack(audioTrack: IAudioTrack) {
        serviceImpl.selectAudioTrack(toServiceAudioTrack(audioTrack))
    }

    override fun getSubtitleTracks(applicationMode: ApplicationMode): List<ISubtitle> {
        var list = arrayListOf<ISubtitle>()
        serviceImpl.subtitleTracks.forEach {
            fromServiceSubtitleTrack(it, utilsInterface).let { track ->
                list.add(track!!)
            }
        }
        return list
    }

    override fun getAudioTracks(): List<IAudioTrack> {
        var list = arrayListOf<IAudioTrack>()
        serviceImpl.audioTracks.forEach {
            var track = fromServiceAudioTrack(it, utilsInterface)
            if (track != null)
                list.add(track)
        }
        return list
    }

    override fun getActiveSubtitle(): ISubtitle? {
        return fromServiceSubtitleTrack(serviceImpl.activeSubtitle, utilsInterface)
    }

    override fun getActiveAudioTrack(): IAudioTrack? {
        if (serviceImpl.activeAudioTrack != null)
            return fromServiceAudioTrack(serviceImpl.activeAudioTrack, utilsInterface)
        else
            return null
    }

    override fun setPlaybackView(playbackView: ViewGroup) {
        serviceImpl?.setPlaybackView(playbackView as ServiceTvView)
    }

    override fun pause() {
        serviceImpl.pause()
    }

    override fun play(playableItem: Any) {
        serviceImpl.play(toServiceChannel(playableItem as TvChannel))
    }

    override fun reset() {
        serviceImpl.reset()
    }

    override fun resume() {
        serviceImpl.resume()
    }

    override fun stop() {
        serviceImpl.stop()
    }

    override fun mute() {
        serviceImpl.mute()
    }

    override fun unmute() {
        serviceImpl.unmute()
    }

    override fun seek(positionMs: Long, isRelative: Boolean) {
        serviceImpl.seek(positionMs, isRelative)
    }

    override fun setSpeed(speed: Int) {
        serviceImpl.speed = speed
    }

    override fun getSpeed() {
        serviceImpl.speed
    }

    override fun slowDown() {
        serviceImpl.slowDown()
    }

    override fun speedUp() {
        serviceImpl.speedUp()
    }

    override fun getDuration(): Long {
        return serviceImpl.duration
    }

    override fun getPosition(): Long {
        return serviceImpl.position
    }

    override fun registerListener(listener: PlayerInterface.PlayerListener) {
        TODO("Not yet implemented")
    }

    override fun unregisterListener(listener: PlayerInterface.PlayerListener) {
        TODO("Not yet implemented")
    }

    override fun requestUnblockContent(callback: IAsyncCallback) {
        /*serviceImpl.requestUnblockContent(object : IAsyncListener.Stub() {
            override fun onFailed(error: String) {
                callback.onFailed(Error(error))
            }

            override fun onSuccess() {
                callback.onSuccess()
            }
        })*/
    }

    override fun getPlaybackTracks(type: Int): List<TvTrackInfo> {
        var list = arrayListOf<TvTrackInfo>()
        list.addAll(serviceImpl.getPlaybackTracks(type))
        return list
    }

    override fun getIsDolby(type: Int): Boolean {
        return serviceImpl.getIsDolby(type)
    }

    override fun getDolbyType(type: Int, trackId: String): String {
        return serviceImpl.getDolbyType(type, trackId)
    }

    override fun getIsCC(type: Int): Boolean {
        return serviceImpl.getIsCC(type)
    }

    override fun getIsAudioDescription(type: Int): Boolean {
        return serviceImpl.getIsAudioDescription(type)
    }

    override fun hasAudioDescription(tvTrackInfo: TvTrackInfo): Boolean {
        return serviceImpl.hasAudioDescription(tvTrackInfo)
    }

    override fun getTeleText(type: Int): Boolean {
        return serviceImpl.getTeletext(type)
    }

    override fun getVideoResolution(): String {
        return serviceImpl.videoResolution
    }

    override fun getAudioChannelIndex(type: Int): Int {
        return serviceImpl.getAudioChannelIndex(type)
    }

    override fun getAudioFormat(): String {
        return ""
    }

    override fun unlockChannel(): Boolean {
        return serviceImpl.unlockChannel()
    }

    override fun switchAudioTrack(): String? {
        return ""
    }

    override fun setSubtitleSurface(holder: SurfaceHolder?) {
    }

    override fun setQuietTuneEnabled(enabled: Boolean) {
        serviceImpl.setQuietTuneEnabled(enabled)
    }

    override fun getQuietTuneEnabled(): Boolean {
        return serviceImpl.getQuietTuneEnabled()
    }

    override fun setTeletextSurface(holder: SurfaceHolder?) {}
    override fun performBackgroundTuning(tvChannel: TvChannel) {}
    override fun initTTML(ttmlViewContainer: RelativeLayout) {}

    override fun refreshTTMLStatus() {}

    override fun setTTMLVisibility(isVisible: Boolean) {}
}