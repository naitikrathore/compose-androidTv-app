package com.iwedia.cltv.scene.infoBanner

import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.components.custom_card.CustomCard
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface

/**
 * Info banner events view holder
 *
 * @author Aleksandar Lazic
 */
class InfoBannerEventsViewHolder(
    view: View,
    toSpeechTextSetterInterface: TTSSetterInterface
) : RecyclerView.ViewHolder(view)  {

    //Root view
    var eventCustomCard: CustomCard.CustomCardInfoBanner

    init {
        //Set references
        eventCustomCard = CustomCard.CustomCardInfoBanner(view.context)
        eventCustomCard.textToSpeechHandler.setupTextToSpeechTextSetterInterface(toSpeechTextSetterInterface)
        (view as LinearLayout).addView(eventCustomCard)
    }
}