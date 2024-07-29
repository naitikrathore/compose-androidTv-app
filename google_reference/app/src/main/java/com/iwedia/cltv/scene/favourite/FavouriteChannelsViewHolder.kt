package com.iwedia.cltv.scene.favourite

import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.components.custom_card.CustomCard

/**
 * Favourite channels view holder
 *
 * @author Aleksandar Lazic
 */
class FavouriteChannelsViewHolder(view: LinearLayout) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: CustomCard.CustomCardFavourite

    init {
        //Set references
        rootView = CustomCard.CustomCardFavourite(view.context)
        view.addView(rootView)
    }
}