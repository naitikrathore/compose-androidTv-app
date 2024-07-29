package com.iwedia.cltv.platform.base.player

import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle

abstract class TrackManager<A: IAudioTrack, S: ISubtitle> {
    protected val mAudioTracks: MutableList<A> = ArrayList()
    var currentAudioTrack: A? = null

    protected val mSubtitleTracks: MutableList<S> = ArrayList()
    var currentSubtitleTrack: S? = null

    protected val lock = Any()

    fun clear() = synchronized(lock) {
        mAudioTracks.clear()
        mSubtitleTracks.clear()
    }

    fun getAudioTracks(): List<A> = mAudioTracks
    fun getSubtitleTracks(): List<S> = mSubtitleTracks
}