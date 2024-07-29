package com.iwedia.cltv.platform.model.player

enum class PlaybackStatus {
    PLAYBACK_INIT,
    NO_PLAYBACK,
    PLAYER_TIMEOUT,
    PLAYBACK_STARTED,
    WAITING_FOR_CHANNEL,
    SCRAMBLED_CHANNEL,
    ACTIVE_CHANNEL_LOCKED_EVENT,
    ACTIVE_CHANNEL_UNLOCKED_EVENT,
    PARENTAL_PIN_SHOW,
    PARENTAL_PIN_HIDE,
    AUDIO_ONLY
}