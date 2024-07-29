package com.iwedia.cltv.scene.audio_subtitle_scene

import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.ToastInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import world.SceneListener

/**
 * Listener to pass on Audio, Subtitle selections
 * Check for usages in AudioSubtitleScene
 *
 * @author Abhilash MR
 */
interface AudioSubtitleListener : SceneListener, TTSSetterInterface, ToastInterface, TTSSetterForSelectableViewInterface {
    /**
     * API to trigger Subtitle-Audio Scene
     * @param audioTrack
     * @param subtitleTrack
     * @param callback
     */
    /**
     * On audio track clicked
     */
    fun onAudioTrackClicked(audioTrack: IAudioTrack)

    /**
     * On subtitle track clicked
     */
    fun onSubtitleTrackClicked(subtitle: ISubtitle)

    fun getCurrentAudioTrack(): IAudioTrack?

    fun getCurrentSubtitleTrack(): ISubtitle?

    fun setSubtitles(isActive: Boolean)

    fun isSubtitlesEnabled(): Boolean

    fun getLanguageMapper(): LanguageMapperInterface
}