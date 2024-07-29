package com.iwedia.cltv.scene.home_scene.guideVertical

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

/**
 * Vertical guide events container view holder
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideEventsContainerViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    //Event list view
    var listView: RecyclerView? = null

    //Separator view
    var separatorView: View? = null

    //bind position
    var bindPosition = -1;

    init {
        //Set references
        listView = view.findViewById(R.id.guide_events_container_item)
        separatorView = view.findViewById(R.id.guide_events_container_item_separator_view)
        listView!!.focusable = View.NOT_FOCUSABLE
        listView!!.isClickable = false
    }
}