package com.iwedia.cltv.scene.home_scene.rail

import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.components.custom_card.CardType
import com.iwedia.cltv.components.custom_card.CustomCard
import com.iwedia.cltv.components.custom_card.CustomCardVod

/**
 * This is base class which defines evey rail row's adapter. Every single adapter for Channels, Events, Recordings, Watchlist MUST HAVE CustomCard which hoists corresponding data.
 * @author Boris Tirkajla
 */
sealed class RailItemViewHolder(
    private val view: View,
    private val toSpeechTextSetterInterface: TTSSetterInterface
) : RecyclerView.ViewHolder(view) {
    lateinit var channelAndEventCard: CustomCard

    fun reduceAlpha() {
        channelAndEventCard.alpha = 0.15f
    }

    fun resetAlpha() {
        channelAndEventCard.alpha = 1f
    }

    fun initialisation() {
        channelAndEventCard.textToSpeechHandler.setupTextToSpeechTextSetterInterface(toSpeechTextSetterInterface)
        (view as LinearLayout).addView(channelAndEventCard)
    }
}

class ForYouRailItemViewHolder(view: View, toSpeechTextSetterInterface: TTSSetterInterface) : RailItemViewHolder(view, toSpeechTextSetterInterface) {
    init {
        channelAndEventCard = CustomCard.CustomCardForYou(view.context)
        initialisation()
    }
}

class VodRailItemViewHolder(view: View, toSpeechTextSetterInterface: TTSSetterInterface) : RailItemViewHolder(view, toSpeechTextSetterInterface) {
    init {
        channelAndEventCard = CustomCardVod(view.context)
        (view as LinearLayout).addView(channelAndEventCard)
    }
}

class SearchRailItemViewHolder(view: View, toSpeechTextSetterInterface: TTSSetterInterface) : RailItemViewHolder(view, toSpeechTextSetterInterface) {
    init {
        channelAndEventCard = CustomCard.CustomCardSearchChannel(view.context, CardType.CHANNEL_LIST_SEARCH)
        initialisation()
    }
}

class RecordingRailItemViewHolder(view: View, toSpeechTextSetterInterface: TTSSetterInterface) : RailItemViewHolder(view, toSpeechTextSetterInterface){
    init {
        channelAndEventCard = CustomCard.CustomCardRecording(view.context)
        initialisation()
    }
}

class ScheduledRecordingRailItemViewHolder(view: View, toSpeechTextSetterInterface: TTSSetterInterface) : RailItemViewHolder(view, toSpeechTextSetterInterface){
    init {
        channelAndEventCard = CustomCard.CustomCardScheduledRecording(view.context)
        initialisation()
    }
}

class BroadcastTvChannelViewHolder(view: View, toSpeechTextSetterInterface: TTSSetterInterface) : RailItemViewHolder(view, toSpeechTextSetterInterface){
    init {
        channelAndEventCard = CustomCard.CustomCardBroadcastChannel(view.context, CardType.BROADCAST_CHANNEL)
        initialisation()
    }
}