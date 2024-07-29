package com.iwedia.cltv.anoki_fast.epg

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

/**
 * Guide events container view holder
 *
 * @author Dejan Nadj
 */
class FastLiveTabEventsContainerViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    //Event list view
    var listView: RecyclerView? = null

    //Separator view
    var separatorView: View? = null

    init {
        //Set references
        listView = view.findViewById(R.id.guide_events_container_item)
        separatorView = view.findViewById(R.id.guide_events_container_item_separator_view)
        listView!!.focusable = View.NOT_FOCUSABLE
        listView!!.isClickable = false
    }
}