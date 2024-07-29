package com.iwedia.cltv.components

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

class PreferenceMmiMenuItemViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: LinearLayout? = null

    //Category text
    var categoryText1: TextView? = null
    var arrowRight: ImageView? = null



    init {

        //Set references
        rootView = view.findViewById(R.id.channel_category_item_root_view)
        categoryText1 = view.findViewById(R.id.preference_category_title)
        arrowRight = view.findViewById(R.id.arrow_right)

        //Set root view to be focusable and clickable
        rootView!!.focusable = View.FOCUSABLE
        rootView!!.isClickable = true
    }
}