package com.iwedia.cltv.scene.audio_subtitle_scene

import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle

/**
 * Data class to maintain Audio and Subtitle data
 * Check usage in AudioSubtitleScene
 *
 * @author Abhilash MR
 */
data class AudioSubtitleList constructor(
    val audioTrack: MutableList<IAudioTrack>,
    val subtitleTrack: MutableList<ISubtitle>
)