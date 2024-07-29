package com.iwedia.cltv.components

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R

class PreferencesSystemInformationViewHolder(view : View) : RecyclerView.ViewHolder(view) {

    var titleText: TextView? = null
    var contentText: TextView? = null

    init {
        titleText = view.findViewById(R.id.title)
        contentText = view.findViewById(R.id.content)
    }
}