package com.iwedia.cltv.components.custom_card

import android.content.Context
import android.util.AttributeSet
import android.view.View
import tv.anoki.ondemand.domain.model.VODItem

class CustomCardVod: CustomCard {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        cardType = CardType.VOD
        setCardSize()
    }

    fun updateData(
        vodItem: VODItem,
    ) {
        resetData()

        insertImageInChannelOrEventImageView(  // Set thumbnail image
            imagePath = vodItem.thumbnail,
            onFailed = {
                channelOrEventNameTextView.apply {// if image fails to load - display Vod title instead
                    text = vodItem.title
                    visibility = View.VISIBLE
                }
            }
        )
    }

    override val textForSpeech: List<String>
        get() = listOf() // TODO This needs to be implemented in the future
}